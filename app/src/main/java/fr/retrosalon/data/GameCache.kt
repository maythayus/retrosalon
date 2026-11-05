package fr.retrosalon.data

import fr.retrosalon.R
import fr.retrosalon.UiException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** Fichier prêt à être lancé, et le dossier de sa console (utile pour les URI SAF et MAME). */
data class Prepared(val file: File, val systemDir: File)

/**
 * Fichier [rel] à l'intérieur de [base]. Les noms viennent du serveur (listes de fichiers,
 * lignes des .cue/.gdi/.m3u) : un nom absolu ou contenant « .. » qui sortirait de [base]
 * est refusé, pour qu'un serveur ne puisse pas faire écrire ailleurs dans le stockage.
 */
internal fun childInside(base: File, rel: String): File {
    val clean = rel.replace('\\', '/')
    val file = File(base, clean)
    val escapes = clean.isBlank() || clean.startsWith("/") || clean.split('/').any { it == ".." } ||
        !file.canonicalPath.startsWith(base.canonicalPath + File.separator)
    if (escapes) throw UiException(R.string.err_file_name_refused, rel)
    return file
}

/**
 * Mode hybride : un jeu local est lancé là où il est ; un jeu du serveur est d'abord copié
 * dans le cache de la TV (`<cache>/<console>/...`), puis lancé depuis ce cache.
 * Le cache garde les jeux récents et efface les plus anciens au-delà de la limite.
 */
class GameCache(private val store: Store) {

    fun systemDirFor(source: Source, game: Game): File = when (source) {
        is Source.Local -> File(source.path, game.folder)
        is Source.Smb -> File(store.cacheRoot, game.systemId)
    }

    fun isCached(source: Source, game: Game): Boolean = when (source) {
        is Source.Local -> true
        is Source.Smb -> File(systemDirFor(source, game), game.rel).let { it.exists() && it.length() == game.size }
    }

    /**
     * @param onProgress octets copiés, octets à copier.
     */
    suspend fun prepare(source: Source, game: Game, onProgress: (Long, Long) -> Unit): Prepared {
        val systemDir = systemDirFor(source, game)
        if (source is Source.Local) {
            val file = File(systemDir, game.rel)
            if (!file.exists()) throw UiException(R.string.err_file_not_found, file.path)
            return Prepared(file, systemDir)
        }
        source as Source.Smb
        SmbFs(source).use { fs ->
            val main = Scanner.join(game.folder, game.rel)
            val dest = childInside(systemDir, game.rel)
            val needed = mutableListOf(main to dest)

            // Les fichiers descripteurs (.cue, .gdi, .m3u, .ccd) sont petits : on les copie d'abord
            // pour connaître les pistes et disques qu'ils référencent.
            val pending = ArrayDeque(listOf(main to dest))
            while (pending.isNotEmpty()) {
                val (rel, file) = pending.removeFirst()
                val kind = Scanner.ext(rel)
                if (kind !in setOf("cue", "gdi", "m3u", "ccd")) continue
                val size = if (rel == main) fs.size(rel) else runCatching { fs.size(rel) }.getOrNull() ?: continue
                copy(fs, rel, file, size, 0, 0) { _, _ -> }
                val dirRel = rel.substringBeforeLast('/', "")
                for (name in companions(file)) {
                    val childRel = Scanner.join(dirRel, name)
                    // Reste dans le dossier du descripteur, donc dans celui de la console.
                    val childFile = childInside(file.parentFile!!, name)
                    if (needed.none { it.first == childRel }) {
                        needed += childRel to childFile
                        pending += childRel to childFile
                    }
                }
            }

            // Un fichier compagnon absent du serveur (ex. le .sub facultatif d'un .ccd) est ignoré :
            // l'émulateur signalera lui-même s'il lui manque vraiment quelque chose.
            val present = needed.mapNotNull { (rel, file) ->
                val size = if (rel == main) game.size else runCatching { fs.size(rel) }.getOrNull()
                size?.let { Triple(rel, file, it) }
            }
            val total = present.sumOf { it.third }
            var done = 0L
            for ((rel, file, size) in present) {
                done = copy(fs, rel, file, size, done, total, onProgress)
            }
            val now = System.currentTimeMillis()
            present.forEach { it.second.setLastModified(now) }
            evict(protect = present.map { it.second.absolutePath }.toSet())
            return Prepared(dest, systemDir)
        }
    }

    /** Copie un fichier s'il manque ou si sa taille diffère. Renvoie le cumul d'octets traités. */
    private suspend fun copy(
        fs: SourceFs, rel: String, dest: File, size: Long, doneBefore: Long, total: Long,
        onProgress: (Long, Long) -> Unit,
    ): Long {
        if (dest.exists() && dest.length() == size) {
            onProgress(doneBefore + size, total)
            return doneBefore + size
        }
        dest.parentFile?.mkdirs()
        val part = File(dest.path + ".part")
        try {
            fs.open(rel).use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(1 shl 20)
                    var copied = 0L
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        copied += n
                        if (copied - lastReport > 2_000_000) {
                            lastReport = copied
                            onProgress(doneBefore + copied, total)
                        }
                    }
                }
            }
            if (dest.exists()) dest.delete()
            if (!part.renameTo(dest)) throw UiException(R.string.err_cannot_write, dest.path)
        } catch (e: Throwable) {
            part.delete()
            throw e
        }
        onProgress(doneBefore + size, total)
        return doneBefore + size
    }

    /** Fichiers référencés par un .cue, .gdi, .m3u ou .ccd (noms relatifs au même dossier). */
    private fun companions(file: File): List<String> {
        val lines = runCatching { file.readLines() }.getOrDefault(emptyList())
        return when (Scanner.ext(file.name)) {
            "cue" -> lines.mapNotNull { Regex("""^\s*FILE\s+"?(.+?)"?\s+\S+\s*$""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
            "gdi" -> lines.drop(1).mapNotNull { line ->
                Regex("""^\s*\d+\s+\d+\s+\d+\s+\d+\s+("[^"]+"|\S+)""").find(line)?.groupValues?.get(1)?.trim('"')
            }
            "m3u" -> lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            "ccd" -> {
                val base = file.name.substringBeforeLast('.')
                listOf("$base.img", "$base.sub")
            }
            else -> emptyList()
        }.map { it.replace('\\', '/') }
    }

    /** Efface les jeux les moins récemment lancés quand le cache dépasse la limite. */
    fun evict(protect: Set<String> = emptySet()) {
        val root = File(store.cacheRoot)
        val limit = store.cacheLimitGb.toLong() * 1_000_000_000L
        val files = root.walkTopDown().filter { it.isFile }.toList()
        var used = files.sumOf { it.length() }
        if (used <= limit) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (used <= limit) break
            if (f.absolutePath in protect) continue
            val len = f.length()
            if (f.delete()) used -= len
        }
        root.walkBottomUp().filter { it.isDirectory && it != root && (it.list()?.isEmpty() == true) }.forEach { it.delete() }
    }

    fun usedBytes(): Long = File(store.cacheRoot).walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clear() {
        File(store.cacheRoot).listFiles()?.forEach { it.deleteRecursively() }
    }
}
