package fr.retrosalon

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.retrosalon.data.Game
import fr.retrosalon.data.GameCache
import fr.retrosalon.data.Launch
import fr.retrosalon.data.LocalFs
import fr.retrosalon.data.Scanner
import fr.retrosalon.data.SmbFs
import fr.retrosalon.data.Source
import fr.retrosalon.data.Store
import fr.retrosalon.data.Systems
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LaunchState(
    val title: String,
    val step: String,
    val progress: Float? = null,
    val error: String? = null,
)

class AppModel(app: Application) : AndroidViewModel(app) {
    val store = Store(app)
    val cache = GameCache(store)

    var sources by mutableStateOf(store.sources)
        private set
    var games by mutableStateOf(store.loadLibrary())
        private set
    var scanning by mutableStateOf(false)
        private set
    var scanReport by mutableStateOf<String?>(null)
        private set
    var launch by mutableStateOf<LaunchState?>(null)
        private set
    var cacheRoot by mutableStateOf(store.cacheRoot)
        private set
    var cacheLimitGb by mutableStateOf(store.cacheLimitGb)
        private set
    var cacheUsed by mutableStateOf(0L)
        private set

    /** Incrémenté pour forcer le recalcul des émulateurs installés (retour sur l'app). */
    var installTick by mutableStateOf(0)
        private set

    private var launchJob: Job? = null

    fun refreshInstalled() {
        installTick++
        refreshCacheUsage()
    }

    fun refreshCacheUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val used = runCatching { cache.usedBytes() }.getOrDefault(0L)
            withContext(Dispatchers.Main) { cacheUsed = used }
        }
    }

    fun rescan() {
        if (scanning) return
        scanning = true
        scanReport = "Analyse des sources…"
        viewModelScope.launch(Dispatchers.IO) {
            val found = mutableListOf<Game>()
            val problems = mutableListOf<String>()
            val unreachable = mutableSetOf<String>()
            for (source in sources) {
                try {
                    val (fs, rootName) = when (source) {
                        is Source.Local -> LocalFs(source.path) to File(source.path).name
                        is Source.Smb -> SmbFs(source) to source.path.trim('/', '\\').substringAfterLast('/').substringAfterLast('\\')
                    }
                    fs.use { found += Scanner.scan(it, source.id, rootName) }
                } catch (e: Exception) {
                    problems += "${source.label} : ${e.message ?: e.javaClass.simpleName}"
                    unreachable += source.id
                }
            }
            // Si une source est injoignable, on garde ses jeux de la liste précédente
            // (repérée par son identifiant : deux sources peuvent porter le même nom).
            val merged = (found + games.filter { it.sourceId in unreachable })
                .distinctBy { it.key }
                .sortedBy { it.title.lowercase() }
            store.saveLibrary(merged)
            withContext(Dispatchers.Main) {
                games = merged
                scanning = false
                scanReport = if (problems.isEmpty()) null else "Problème : " + problems.joinToString(" ; ")
            }
        }
    }

    fun play(game: Game) {
        if (launchJob?.isActive == true) return
        val app = getApplication<Application>()
        val system = Systems.byId(game.systemId) ?: return
        val source = sources.firstOrNull { it.id == game.sourceId }
        if (source == null) {
            launch = LaunchState(game.title, "", error = "La source de ce jeu a été supprimée. Lancez « Actualiser ».")
            return
        }
        val launcher = Launch.pick(app, store, system)
        if (launcher == null) {
            val names = system.launchers.map { it.label.substringBefore(" ·") }.distinct().joinToString(" ou ")
            launch = LaunchState(game.title, "", error = "Aucun émulateur ${system.name} n'est installé. Installez $names.")
            return
        }
        launch = LaunchState(
            game.title,
            if (source is Source.Smb && !cache.isCached(source, game)) "Copie depuis ${source.label}…" else "Lancement avec ${launcher.label}…",
            progress = null,
        )
        launchJob = viewModelScope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) {
                    cache.prepare(source, game) { done, total ->
                        if (total > 0) {
                            val p = (done.toDouble() / total).toFloat().coerceIn(0f, 1f)
                            viewModelScope.launch(Dispatchers.Main) {
                                launch = launch?.copy(progress = p, step = "Copie : ${formatBytes(done)} / ${formatBytes(total)}")
                            }
                        }
                    }
                }
                launch = launch?.copy(step = "Lancement avec ${launcher.label}…", progress = 1f)
                val error = Launch.start(app, launcher, prepared)
                launch = if (error != null) launch?.copy(error = error) else null
                refreshCacheUsage()
            } catch (e: CancellationException) {
                launch = null
                throw e
            } catch (e: Exception) {
                launch = launch?.copy(error = "Impossible de préparer le jeu : ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun dismissLaunch() {
        launchJob?.cancel()
        launch = null
    }

    fun addSource(source: Source) {
        sources = sources + source
        store.sources = sources
        rescan()
    }

    fun removeSource(id: String) {
        sources = sources.filterNot { it.id == id }
        store.sources = sources
        games = games.filterNot { it.sourceId == id }
        store.saveLibrary(games)
    }

    /** Teste la connexion SMB et renvoie un compte rendu lisible. */
    suspend fun testSmb(source: Source.Smb): String = withContext(Dispatchers.IO) {
        try {
            SmbFs(source).use { fs ->
                val entries = fs.list("")
                val systems = entries.filter { it.isDir }.mapNotNull { Systems.byFolder(it.name)?.name }
                if (systems.isEmpty()) {
                    "Connexion réussie. ${entries.size} éléments, mais aucun dossier de console reconnu (ex. snes, psx, gc)."
                } else {
                    "Connexion réussie. Consoles trouvées : ${systems.joinToString(", ")}."
                }
            }
        } catch (e: Exception) {
            "Échec : ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun setLauncher(systemId: String, launcherId: String) {
        store.setLauncherFor(systemId, launcherId)
        installTick++
    }

    fun moveCache(path: String) {
        store.cacheRoot = path
        cacheRoot = store.cacheRoot
        refreshCacheUsage()
    }

    fun changeCacheLimit(deltaGb: Int) {
        store.cacheLimitGb = store.cacheLimitGb + deltaGb
        cacheLimitGb = store.cacheLimitGb
        viewModelScope.launch(Dispatchers.IO) {
            cache.evict()
            refreshCacheUsage()
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            cache.clear()
            refreshCacheUsage()
        }
    }

    companion object {
        fun formatBytes(b: Long): String = when {
            b >= 1_000_000_000L -> "%.1f Go".format(b / 1e9)
            b >= 1_000_000L -> "%.0f Mo".format(b / 1e6)
            b >= 1_000L -> "%.0f Ko".format(b / 1e3)
            else -> "$b o"
        }
    }
}
