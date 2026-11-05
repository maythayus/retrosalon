package fr.retrosalon.data

/**
 * Parcourt une source et liste les jeux.
 *
 * Organisation attendue (celle d'ES-DE / RetroArch) : un dossier par console à la racine de la source,
 * par exemple `snes/`, `psx/` ou `ps1/`, `gc/` ou `gamecube/`. Les sous-dossiers sont parcourus sur 3 niveaux.
 * Si la source pointe directement sur un dossier de console, il est reconnu aussi.
 */
object Scanner {
    private const val MAX_DEPTH = 3
    private val ignoredDirs = setOf("media", "images", "videos", "manuals", "bios", "saves", "states", "screenshots", "downloaded_media")
    private val descriptors = setOf("cue", "ccd", "gdi", "toc")
    private val discData = setOf("bin", "img", "sub", "raw")

    fun scan(fs: SourceFs, sourceId: String, rootName: String): List<Game> {
        Systems.byFolder(rootName)?.let { return collect(fs, sourceId, it, folder = "") }
        return fs.list("")
            .filter { it.isDir }
            .flatMap { dir ->
                val system = Systems.byFolder(dir.name) ?: return@flatMap emptyList()
                collect(fs, sourceId, system, folder = dir.name)
            }
    }

    private fun collect(fs: SourceFs, sourceId: String, system: GameSystem, folder: String, sub: String = "", depth: Int = 0): List<Game> {
        val entries = runCatching { fs.list(join(folder, sub)) }.getOrElse { return emptyList() }
        val files = entries.filter { !it.isDir && ext(it.name) in system.extensions }
        val hidden = hiddenCompanions(system, files)
        val games = files.filter { it.name !in hidden }.map {
            Game(sourceId, system.id, folder, join(sub, it.name), it.size)
        }
        if (depth >= MAX_DEPTH) return games
        val children = entries
            .filter { it.isDir && it.name.lowercase() !in ignoredDirs && !it.name.startsWith(".") }
            .flatMap { collect(fs, sourceId, system, folder, join(sub, it.name), depth + 1) }
        return games + children
    }

    /** Masque les pistes .bin d'un .cue et les disques déjà regroupés dans un .m3u. */
    private fun hiddenCompanions(system: GameSystem, files: List<Entry>): Set<String> {
        if (!system.disc) return emptySet()
        val hidden = mutableSetOf<String>()
        if (files.any { ext(it.name) in descriptors }) {
            files.filter { ext(it.name) in discData }.forEach { hidden += it.name }
        }
        val playlists = files.filter { ext(it.name) == "m3u" }.map { Game.cleanTitle(it.name.substringBeforeLast('.')) }.toSet()
        if (playlists.isNotEmpty()) {
            files.filter { ext(it.name) != "m3u" && Game.cleanTitle(it.name.substringBeforeLast('.')) in playlists }
                .forEach { hidden += it.name }
        }
        return hidden
    }

    fun ext(name: String) = name.substringAfterLast('.', "").lowercase()
    fun join(a: String, b: String) = listOf(a, b).filter { it.isNotEmpty() }.joinToString("/")
}
