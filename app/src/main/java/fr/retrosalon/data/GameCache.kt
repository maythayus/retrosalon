package fr.retrosalon.data

import fr.retrosalon.R
import fr.retrosalon.UiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileNotFoundException

/** Fichier prêt à être lancé, et le dossier de sa console (utile pour les URI SAF et MAME). */
data class Prepared(val file: File, val systemDir: File)

/**
 * Une seule copie, ou un seul ménage du cache, à la fois dans toute l'app : une copie annulée
 * peut rester bloquée un moment (lecture réseau, écriture sur une clé USB lente) et ne doit pas
 * croiser la suivante sur les mêmes fichiers. Le verrou n'est rendu qu'une fois son code vraiment
 * terminé ; un lancement qui attend reste annulable.
 */
private val cacheLock = Mutex()

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

/** Fichier référencé par un .cue, .gdi, .m3u ou .ccd. [optional] : il peut ne pas exister (le .sub d'un .ccd). */
internal data class Companion(val name: String, val optional: Boolean = false)

private val DESCRIPTORS = setOf("cue", "gdi", "m3u", "ccd")

/**
 * Fichiers référencés par le descripteur [file] (.cue, .gdi, .m3u ou .ccd), relatifs à son dossier.
 * [name] est son nom définitif, s'il est encore enregistré sous un nom provisoire.
 * Un autre fichier n'est pas ouvert : ce peut être une image disque de plusieurs gigaoctets.
 */
internal fun companions(file: File, name: String = file.name): List<Companion> {
    val kind = Scanner.ext(name)
    if (kind !in DESCRIPTORS) return emptyList()
    if (kind == "ccd") {
        val base = name.substringBeforeLast('.')
        return listOf(Companion("$base.img"), Companion("$base.sub", optional = true))
    }
    // Un fichier enregistré « UTF-8 avec BOM » (ancien Bloc-notes de Windows) commence par U+FEFF :
    // sans le retirer, le premier disque ou la première piste ne serait pas reconnu.
    val lines = runCatching { file.readLines() }.getOrDefault(emptyList())
        .mapIndexed { i, line -> if (i == 0) line.removePrefix("\uFEFF") else line }
    return when (kind) {
        "cue" -> lines.mapNotNull { Regex("""^\s*FILE\s+"?(.+?)"?\s+\S+\s*$""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
        "gdi" -> lines.drop(1).mapNotNull { line ->
            Regex("""^\s*\d+\s+\d+\s+\d+\s+\d+\s+("[^"]+"|\S+)""").find(line)?.groupValues?.get(1)?.trim('"')
        }
        else -> lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") } // m3u
    }.map { Companion(it.replace('\\', '/')) }
}

/**
 * Fichiers d'un jeu du serveur déjà présents dans le cache [systemDir] : le fichier principal [rel],
 * à la taille [size] relevée au dernier scan (null : toute taille), puis toutes les pistes et tous les disques que ses
 * descripteurs référencent (un .m3u renvoie à des .cue, qui renvoient à des .bin…). Le fichier
 * principal vient en premier. Null s'il en manque un : il faut alors passer par le serveur.
 * [lenient] tolère l'absence des fichiers facultatifs, qui peuvent ne pas exister sur le serveur.
 *
 * Une copie ne met ses fichiers en place qu'une fois tous téléchargés en entier (voir
 * GameCache.copyFromServer) : un descripteur présent va donc avec les pistes présentes.
 */
internal fun cachedFiles(systemDir: File, rel: String, size: Long?, lenient: Boolean = false): List<File>? {
    val main = childInside(systemDir, rel)
    if (!main.isFile || (size != null && main.length() != size)) return null
    val found = mutableListOf(main)
    val seen = mutableSetOf(main.canonicalPath)
    val pending = ArrayDeque(listOf(main))
    while (pending.isNotEmpty()) {
        val file = pending.removeFirst()
        for (ref in companions(file)) {
            // Reste dans le dossier du descripteur, donc dans celui de la console.
            val child = childInside(file.parentFile!!, ref.name)
            if (!seen.add(child.canonicalPath)) continue
            when {
                child.isFile -> {
                    found += child
                    pending += child
                }
                ref.optional && lenient -> Unit
                else -> return null
            }
        }
    }
    return found
}

/**
 * Mode hybride : un jeu local est lancé là où il est ; un jeu du serveur est d'abord copié
 * dans le cache de la TV (`<cache>/<console>/...`), puis lancé depuis ce cache.
 * Un jeu déjà entier dans le cache se lance sans le serveur, même éteint.
 * Le cache garde les jeux récents et efface les plus anciens au-delà de la limite.
 */
class GameCache(
    private val cacheRoot: () -> String,
    private val cacheLimitGb: () -> Int,
    private val openSmb: (Source.Smb) -> SourceFs = ::SmbFs,
) {
    constructor(store: Store) : this({ store.cacheRoot }, { store.cacheLimitGb })

    fun systemDirFor(source: Source, game: Game): File = when (source) {
        is Source.Local -> File(source.path, game.folder)
        is Source.Smb -> File(cacheRoot(), game.systemId)
    }

    /**
     * Fichier à lancer : le jeu local là où il est, ou le jeu du serveur pris dans le cache,
     * après copie si besoin. Lit le disque et le réseau : à appeler hors du fil principal.
     * @param onConnect appelé juste avant de contacter le serveur, donc quand une copie peut être nécessaire.
     * @param onProgress octets copiés, octets à copier.
     */
    suspend fun prepare(
        source: Source,
        game: Game,
        onConnect: suspend () -> Unit = {},
        onProgress: (Long, Long) -> Unit,
    ): Prepared {
        val systemDir = systemDirFor(source, game)
        if (source is Source.Local) {
            val file = File(systemDir, game.rel)
            if (!file.exists()) throw UiException(R.string.err_file_not_found, file.path)
            return Prepared(file, systemDir)
        }
        source as Source.Smb
        // Déjà entier dans le cache : lancé tout de suite, sans attendre ni même joindre le serveur.
        // Un cache illisible (nom refusé dans un descripteur copié par une ancienne version) n'empêche
        // pas de repasser par le serveur, qui fournit peut-être un descripteur corrigé.
        runCatching { cachedFiles(systemDir, game.rel, game.size) }.getOrNull()?.let { return fromCache(it, systemDir) }
        currentCoroutineContext().ensureActive()
        return try {
            onConnect()
            cacheLock.withLock {
                val fs = try {
                    openSmb(source)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Serveur éteint ou injoignable : on joue la version entière du cache, même si le jeu a
                    // changé sur le serveur depuis (taille différente du dernier scan). Une copie ne met ses
                    // fichiers en place que tous ensemble : cette version est cohérente.
                    val files = runCatching { cachedFiles(systemDir, game.rel, size = null, lenient = true) }.getOrNull() ?: throw e
                    return@withLock fromCache(files, systemDir)
                }
                fs.use { copyFromServer(it, game, systemDir, onProgress) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Copie interrompue (coupure, disque plein…) : le jeu se lance quand même s'il est entier dans
            // le cache dans la version du dernier scan (sauf fichiers facultatifs absents). Sinon, l'erreur
            // est la bonne explication pour l'utilisateur.
            val files = runCatching { cachedFiles(systemDir, game.rel, game.size, lenient = true) }.getOrNull() ?: throw e
            fromCache(files, systemDir)
        }
    }

    /** Marque les fichiers comme récemment joués, pour que le cache les garde le plus longtemps. */
    private fun fromCache(files: List<File>, systemDir: File): Prepared {
        val now = System.currentTimeMillis()
        files.forEach { it.setLastModified(now) }
        return Prepared(files.first(), systemDir)
    }

    /**
     * Copie le jeu depuis le serveur, sans retélécharger les fichiers déjà dans le cache à la bonne taille.
     *
     * Les descripteurs (.cue, .gdi, .m3u, .ccd) sont petits : on les télécharge d'abord pour connaître
     * les pistes et disques qu'ils référencent. Chaque fichier téléchargé l'est sous un nom provisoire
     * (« .new »), et tous ne prennent leur vrai nom qu'à la fin, ensemble. Une copie interrompue
     * (annulation, coupure) ne touche donc pas au jeu déjà dans le cache : jamais un mélange de deux
     * versions. Les fichiers provisoires complets servent à reprendre la copie la fois suivante.
     */
    private suspend fun copyFromServer(
        fs: SourceFs, game: Game, systemDir: File, onProgress: (Long, Long) -> Unit,
    ): Prepared {
        val main = Scanner.join(game.folder, game.rel)
        val dest = childInside(systemDir, game.rel)
        val mainSize = try {
            fs.size(main)
        } catch (e: FileNotFoundException) {
            // Supprimé ou renommé sur le serveur depuis le dernier scan.
            throw UiException(R.string.err_file_not_found, main, cause = e)
        }
        val needed = mutableListOf(main to dest)
        val newDescriptors = mutableListOf<Pair<File, File>>() // fichier provisoire → nom définitif

        val pending = ArrayDeque(listOf(main to dest))
        while (pending.isNotEmpty()) {
            val (rel, file) = pending.removeFirst()
            if (Scanner.ext(rel) !in DESCRIPTORS) continue
            val size = if (rel == main) mainSize else sizeIfPresent(fs, rel) ?: continue
            val read = if (file.isFile && file.length() == size) file else {
                val next = provisional(file)
                copy(fs, rel, next, size, 0, 0) { _, _ -> }
                newDescriptors += next to file
                next
            }
            val dirRel = rel.substringBeforeLast('/', "")
            for ((name) in companions(read, file.name)) {
                val childRel = Scanner.join(dirRel, name)
                // Reste dans le dossier du descripteur, donc dans celui de la console.
                val childFile = childInside(file.parentFile!!, name)
                if (needed.none { it.first == childRel }) {
                    needed += childRel to childFile
                    pending += childRel to childFile
                }
            }
        }

        // Un fichier absent du serveur (ex. le .sub facultatif d'un .ccd) est ignoré : l'émulateur
        // signalera lui-même s'il lui manque vraiment quelque chose. Une coupure réseau, elle,
        // arrête la copie.
        val present = needed.mapNotNull { (rel, file) ->
            val size = if (rel == main) mainSize else sizeIfPresent(fs, rel)
            size?.let { Triple(rel, file, it) }
        }
        val toCopy = present.filter { Scanner.ext(it.first) !in DESCRIPTORS }
        val total = toCopy.sumOf { it.third }
        var done = 0L
        val newFiles = mutableListOf<Pair<File, File>>()
        for ((rel, file, size) in toCopy) {
            if (file.isFile && file.length() == size) {
                done += size
                onProgress(done, total)
                continue
            }
            val next = provisional(file)
            done = copy(fs, rel, next, size, done, total, onProgress)
            newFiles += next to file
        }

        // Tout est téléchargé : mise en place. Les descripteurs remplacés sont retirés d'abord : si
        // un renommage échoue, le jeu est incomplet (il faudra le serveur), pas un mélange de versions.
        // Puis les pistes et disques, puis les descripteurs, les plus profonds d'abord (les .cue avant
        // le .m3u qui les cite), le fichier principal en dernier.
        currentCoroutineContext().ensureActive()
        newDescriptors.forEach { (_, file) -> file.delete() }
        for ((next, file) in newFiles + newDescriptors.asReversed()) {
            if (file.exists()) file.delete()
            if (!next.renameTo(file)) throw UiException(R.string.err_cannot_write, file.path)
        }

        val now = System.currentTimeMillis()
        present.forEach { it.second.setLastModified(now) }
        evictUnlocked(protect = present.map { it.second.absolutePath }.toSet())
        return Prepared(dest, systemDir)
    }

    /** Nom provisoire d'un fichier téléchargé, avant sa mise en place. */
    private fun provisional(file: File) = File(file.path + ".new")

    /** Taille d'un fichier du serveur, ou null s'il n'y existe pas. Une autre erreur (réseau) remonte. */
    private fun sizeIfPresent(fs: SourceFs, rel: String): Long? =
        try {
            fs.size(rel)
        } catch (_: FileNotFoundException) {
            null
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
                    // Fichier modifié sur le serveur pendant la copie : ne jamais garder un fichier de
                    // la mauvaise taille, le cache le croirait complet.
                    if (copied != size) throw UiException(R.string.err_copy_changed, rel)
                    // Sur le disque avant de prendre son vrai nom : un fichier présent dans le cache
                    // doit être complet, même après une coupure de courant. Au mieux : certains
                    // systèmes de fichiers (clés USB) peuvent refuser.
                    runCatching { out.fd.sync() }
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

    /** Efface les jeux les moins récemment lancés quand le cache dépasse la limite. */
    suspend fun evict() {
        cacheLock.withLock { evictUnlocked() }
    }

    private fun evictUnlocked(protect: Set<String> = emptySet()) {
        val root = File(cacheRoot())
        val limit = cacheLimitGb().toLong() * 1_000_000_000L
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

    fun usedBytes(): Long = File(cacheRoot()).walkTopDown().filter { it.isFile }.sumOf { it.length() }

    /** Vide le cache, après la copie en cours s'il y en a une. */
    suspend fun clear() {
        cacheLock.withLock { File(cacheRoot()).listFiles()?.forEach { it.deleteRecursively() } }
    }
}
