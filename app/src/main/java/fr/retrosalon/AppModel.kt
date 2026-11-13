package fr.retrosalon

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import fr.retrosalon.data.EmbeddedRetroArch
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

    /** Manettes connectées et leur batterie (suivies tant que l'app est visible). */
    val gamepads = PadMonitor(app, viewModelScope)

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
    private var launchGen = 0

    init {
        // Prépare le RetroArch intégré dès l'ouverture (décompression après une mise à jour) :
        // le premier jeu démarre ensuite sans attendre.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { EmbeddedRetroArch.prepare(app) }.onFailure { Log.w("RetroSalon", "RetroArch intégré", it) }
        }
    }

    /** Texte traduit dans la langue du système. */
    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        getApplication<Application>().resources.getQuantityString(id, count, *args)

    /** Assemble une liste avec une ressource à deux paramètres (« %1$s ou %2$s »…). */
    private fun List<String>.joinWith(@StringRes id: Int): String =
        reduceOrNull { acc, s -> str(id, acc, s) }.orEmpty()

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
        scanReport = str(R.string.model_scan_running)
        val app = getApplication<Application>()
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
                    problems += str(R.string.model_scan_problem_item, source.label, SmbFs.explain(e, source.label).userMessage(app))
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
                scanReport = if (problems.isEmpty()) null
                else str(R.string.model_scan_problems, problems.joinWith(R.string.model_scan_problem_join))
            }
        }
    }

    fun play(game: Game) {
        if (launchJob?.isActive == true) return
        // Numéro de ce lancement : un lancement annulé, encore en train de se terminer, ne doit pas
        // modifier l'écran d'un lancement plus récent (ni ses messages d'erreur).
        val gen = ++launchGen
        val app = getApplication<Application>()
        val system = Systems.byId(game.systemId) ?: return
        val source = sources.firstOrNull { it.id == game.sourceId }
        if (source == null) {
            launch = LaunchState(game.title, "", error = str(R.string.model_launch_source_removed))
            return
        }
        val launcher = Launch.pick(app, store, system)
        if (launcher == null) {
            val names = system.launchers.map { it.label.substringBefore(" ·") }.distinct().joinWith(R.string.model_launch_or)
            launch = LaunchState(game.title, "", error = str(R.string.model_launch_no_emulator, system.name, names))
            return
        }
        // « Copie depuis … » s'affiche seulement si le cache doit contacter le serveur : c'est
        // prepare(), hors du fil principal, qui le sait (il lit le disque, parfois une clé USB lente).
        launch = LaunchState(game.title, str(R.string.model_launch_starting, launcher.label), progress = null)
        launchJob = viewModelScope.launch {
            // N'écrit dans l'écran de lancement que si ce lancement est toujours le courant.
            fun update(change: (LaunchState?) -> LaunchState?) {
                if (launchGen == gen) launch = change(launch)
            }
            fun post(change: (LaunchState) -> LaunchState) {
                viewModelScope.launch(Dispatchers.Main) { update { it?.let(change) } }
            }
            try {
                val prepared = withContext(Dispatchers.IO) {
                    cache.prepare(
                        source,
                        game,
                        // Une copie annulée encore en cours est attendue par le cache lui-même.
                        onConnect = { post { it.copy(step = str(R.string.model_launch_copying_from, source.label)) } },
                        onRetry = { attempt, attempts ->
                            post { it.copy(step = str(R.string.model_launch_retrying, source.label, attempt, attempts)) }
                        },
                    ) { done, total ->
                        if (total > 0) {
                            val p = (done.toDouble() / total).toFloat().coerceIn(0f, 1f)
                            post {
                                it.copy(
                                    progress = p,
                                    step = str(R.string.model_launch_copy_progress, formatBytes(app, done), formatBytes(app, total)),
                                )
                            }
                        }
                    }
                }
                update { it?.copy(step = str(R.string.model_launch_starting, launcher.label), progress = 1f) }
                if (Launch.usesEmbedded(app, launcher)) withContext(Dispatchers.IO) { EmbeddedRetroArch.prepare(app) }
                val error = Launch.start(app, launcher, prepared)
                update { if (error != null) it?.copy(error = error) else null }
                refreshCacheUsage()
            } catch (e: CancellationException) {
                update { null }
                throw e
            } catch (e: Exception) {
                update { it?.copy(error = str(R.string.model_launch_prepare_failed, e.userMessage(app))) }
            }
        }
    }

    /** Ouvre le menu du RetroArch intégré, sans jeu : réglages, manettes, shaders… */
    fun openRetroArch() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { EmbeddedRetroArch.prepare(app) }
                EmbeddedRetroArch.start(app, EmbeddedRetroArch.menuIntent(app))
            } catch (e: Exception) {
                launch = LaunchState("RetroArch", "", error = str(R.string.settings_retroarch_open_failed, e.userMessage(app)))
            }
        }
    }

    fun dismissLaunch() {
        launchGen++
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

    /** Teste la connexion SMB : réussite ou échec, et un compte rendu lisible. */
    suspend fun testSmb(source: Source.Smb): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            SmbFs(source).use { fs ->
                val entries = fs.list("")
                val systems = entries.filter { it.isDir }.mapNotNull { Systems.byFolder(it.name)?.name }
                true to if (systems.isEmpty()) {
                    plural(R.plurals.model_smb_ok_no_console, entries.size, entries.size)
                } else {
                    str(R.string.model_smb_ok_consoles, systems.joinToString(", "))
                }
            }
        } catch (e: Exception) {
            false to str(R.string.model_smb_failed, SmbFs.explain(e, source.label).userMessage(getApplication<Application>()))
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

    override fun onCleared() {
        gamepads.stop()
    }
}
