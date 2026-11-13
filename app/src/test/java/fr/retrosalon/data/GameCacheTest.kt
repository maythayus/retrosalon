package fr.retrosalon.data

import com.hierynomus.mserref.NtStatus
import com.hierynomus.mssmb2.SMB2MessageCommandCode
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.transport.TransportException
import com.hierynomus.smbj.common.SMBRuntimeException
import fr.retrosalon.R
import fr.retrosalon.UiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.SocketException
import java.nio.file.Files
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

/**
 * Serveur en mémoire : chemin relatif à la racine du partage → contenu du fichier.
 * [failOpen] / [failSize] : erreur levée en ouvrant un fichier ou en demandant sa taille (coupure, annulation).
 */
private class FakeServer(
    private val files: Map<String, ByteArray>,
    private val failOpen: Map<String, Throwable> = emptyMap(),
    private val failSize: Map<String, Throwable> = emptyMap(),
) : SourceFs {
    /** Fichiers téléchargés, dans l'ordre. */
    val opened = mutableListOf<String>()

    override fun list(rel: String) = emptyList<Entry>()
    override fun open(rel: String): InputStream {
        opened += rel
        failOpen[rel]?.let { throw it }
        return ByteArrayInputStream(files[rel] ?: throw FileNotFoundException(rel))
    }
    override fun size(rel: String): Long {
        failSize[rel]?.let { throw it }
        return files[rel]?.size?.toLong() ?: throw FileNotFoundException(rel)
    }
}

/** Ce que smbj lève quand le serveur ne répond pas à temps (message vu sur la TV). */
private fun smbTimeout() = TransportException(ExecutionException(SMBRuntimeException(TimeoutException("Timeout expired"))))

/**
 * Serveur qui cesse de répondre après [cutAfter] octets d'un fichier, aux [stalls] premières lectures,
 * en levant [error]. Les lectures suivantes vont jusqu'au bout. [offsets] : point de départ de chaque lecture.
 */
private class StallingServer(
    private val files: Map<String, ByteArray>,
    private val cutAfter: Int,
    private var stalls: Int,
    private val error: () -> Throwable = ::smbTimeout,
) : SourceFs {
    val offsets = mutableListOf<Long>()

    override fun list(rel: String) = emptyList<Entry>()
    override fun size(rel: String) = files[rel]?.size?.toLong() ?: throw FileNotFoundException(rel)
    override fun open(rel: String): InputStream = open(rel, 0)
    override fun open(rel: String, offset: Long): InputStream {
        offsets += offset
        val data = files[rel] ?: throw FileNotFoundException(rel)
        val stall = stalls-- > 0
        return object : InputStream() {
            var pos = offset.toInt()
            override fun read(): Int {
                val one = ByteArray(1)
                return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (stall && pos >= cutAfter) throw error()
                val end = if (stall) minOf(cutAfter, data.size) else data.size
                if (pos >= end) return -1
                val n = minOf(len, end - pos)
                System.arraycopy(data, pos, b, off, n)
                pos += n
                return n
            }
        }
    }
}

class GameCacheTest {
    private val root: File = Files.createTempDirectory("retrosalon-cache").toFile()
    private val psx = File(root, "psx")
    private val source = Source.Smb(id = "nas", label = "NAS", host = "nas.local", share = "jeux")

    /** Nombre de connexions au serveur demandées par le cache. */
    private var connections = 0

    /** Nombre d'appels à onConnect (affichage « Copie depuis … »). */
    private var connectNotices = 0

    /** Cache dont le serveur est éteint : toute connexion échoue avec [down]. */
    private val down = IOException("serveur éteint")
    private fun offlineCache() = GameCache({ root.path }, { 32 }, retryDelayMs = 0) { connections++; throw down }
    private fun onlineCache(server: SourceFs) = GameCache({ root.path }, { 32 }, retryDelayMs = 0) { connections++; server }

    /** Nouveaux essais annoncés : numéro de l'essai, nombre d'essais en tout. */
    private val retries = mutableListOf<Pair<Int, Int>>()

    private fun game(rel: String, size: Long) = Game("nas", "psx", "ps1", rel, size)

    private suspend fun GameCache.launch(game: Game) =
        prepare(source, game, { connectNotices++ }, { attempt, attempts -> retries += attempt to attempts }) { _, _ -> }

    private fun cached(rel: String, text: String): File =
        File(psx, rel).apply { parentFile!!.mkdirs(); writeText(text) }

    private val cue = "FILE \"Nights (Track 1).bin\" BINARY\n  TRACK 01 MODE1/2352\nFILE \"Nights (Track 2).bin\" BINARY\n  TRACK 02 AUDIO\n"

    @Test
    fun completeGameStartsWithoutContactingTheServer() = runBlocking {
        val main = cached("Nights.cue", cue)
        cached("Nights (Track 1).bin", "data")
        cached("Nights (Track 2).bin", "audio")

        val prepared = offlineCache().launch(game("Nights.cue", main.length()))

        assertEquals(main.canonicalPath, prepared.file.canonicalPath)
        assertEquals(psx.canonicalPath, prepared.systemDir.canonicalPath)
        assertEquals(0, connections)
        assertEquals(0, connectNotices)
    }

    @Test
    fun startingFromTheCacheMarksEveryFileAsRecentlyPlayed() = runBlocking {
        val files = listOf(cached("Nights.cue", cue), cached("Nights (Track 1).bin", "data"), cached("Nights (Track 2).bin", "audio"))
        files.forEach { it.setLastModified(1_000_000_000_000L) }
        val before = System.currentTimeMillis() - 2_000

        offlineCache().launch(game("Nights.cue", files[0].length()))

        files.forEach { assertTrue(it.name, it.lastModified() >= before) }
    }

    @Test
    fun missingTrackWithServerOffShowsTheServerError() = runBlocking {
        val main = cached("Nights.cue", cue)
        cached("Nights (Track 1).bin", "data")
        val cache = offlineCache()

        val e = assertThrows(IOException::class.java) { runBlocking { cache.launch(game("Nights.cue", main.length())) } }
        assertSame(down, e)
        assertEquals(1, connections)
        assertEquals(1, connectNotices)
    }

    @Test
    fun gameChangedOnTheServerIsCopiedAgainWhenTheServerIsOn() = runBlocking {
        cached("Crash.chd", "old dump")
        val server = FakeServer(mapOf("ps1/Crash.chd" to "new dump!!".toByteArray()))
        onlineCache(server).launch(game("Crash.chd", 10))
        assertEquals(listOf("ps1/Crash.chd"), server.opened)
        assertEquals("new dump!!", File(psx, "Crash.chd").readText())
    }

    @Test
    fun serverOffPlaysTheCachedVersionEvenIfTheGameChangedSinceTheScan() = runBlocking {
        // La bibliothèque a vu une autre version sur le serveur (autre taille), mais il est éteint :
        // la version entière du cache se lance.
        val old = cached("Crash.chd", "old dump")
        val prepared = offlineCache().launch(game("Crash.chd", 1234))
        assertEquals(old.canonicalPath, prepared.file.canonicalPath)
        assertEquals(1, connections)
    }

    @Test
    fun copiedOnceThenStartsWhileTheServerIsOff() = runBlocking {
        val server = FakeServer(
            mapOf(
                "ps1/Nights.cue" to cue.toByteArray(),
                "ps1/Nights (Track 1).bin" to "data".toByteArray(),
                "ps1/Nights (Track 2).bin" to "audio".toByteArray(),
            ),
        )
        val game = game("Nights.cue", cue.toByteArray().size.toLong())

        val first = onlineCache(server).launch(game)
        assertEquals(1, connections)
        assertArrayEquals("audio".toByteArray(), File(psx, "Nights (Track 2).bin").readBytes())
        assertEquals(emptyList<String>(), leftovers())

        val second = offlineCache().launch(game)
        assertEquals(first.file.canonicalPath, second.file.canonicalPath)
        assertEquals(1, connections)
    }

    /** Le jeu a été remplacé sur le serveur : même nom de pistes, autre .cue, autres pistes. */
    private val newCue = cue.replace("MODE1/2352", "MODE2/2352") + "  REM nouvelle version\n"
    private val updatedServer = mapOf(
        "ps1/Nights.cue" to newCue.toByteArray(),
        "ps1/Nights (Track 1).bin" to "new data".toByteArray(),
        "ps1/Nights (Track 2).bin" to "new audio".toByteArray(),
    )

    private fun oldCopyInCache() {
        cached("Nights.cue", cue)
        cached("Nights (Track 1).bin", "data")
        cached("Nights (Track 2).bin", "audio")
    }

    /** Copies en cours abandonnées : ne doivent jamais rester. */
    private fun leftovers() = root.walkTopDown().filter { it.isFile && it.name.endsWith(".part") }.map { it.name }.toList()

    /** Fichiers téléchargés en entier mais pas encore mis en place (servent à reprendre la copie). */
    private fun provisionals() = root.walkTopDown().filter { it.isFile && it.name.endsWith(".new") }.map { it.name }.sorted().toList()

    @Test
    fun updateCutHalfwayLeavesTheCachedVersionUntouched() = runBlocking {
        oldCopyInCache()
        val cut = IOException("coupure Wi-Fi")
        val game = game("Nights.cue", newCue.length.toLong())
        val flaky = FakeServer(updatedServer, failOpen = mapOf("ps1/Nights (Track 2).bin" to cut))

        val e = assertThrows(IOException::class.java) { runBlocking { onlineCache(flaky).launch(game) } }
        assertSame(cut, e)
        // Rien n'a bougé dans le cache : ancien .cue et anciennes pistes, piste 1 comprise…
        assertEquals(cue, File(psx, "Nights.cue").readText())
        assertEquals("data", File(psx, "Nights (Track 1).bin").readText())
        assertEquals("audio", File(psx, "Nights (Track 2).bin").readText())
        assertEquals(emptyList<String>(), leftovers())
        assertEquals(listOf("Nights (Track 1).bin.new", "Nights.cue.new"), provisionals())
        // … donc, serveur éteint, c'est l'ancienne version, entière et cohérente, qui se lance.
        assertEquals("Nights.cue", offlineCache().launch(game).file.name)
        assertEquals(cue, File(psx, "Nights.cue").readText())
        assertEquals("data", File(psx, "Nights (Track 1).bin").readText())

        // Serveur revenu : la copie reprend là où elle s'était arrêtée, puis tout est mis en place.
        val back = FakeServer(updatedServer)
        onlineCache(back).launch(game)
        assertEquals(listOf("ps1/Nights (Track 2).bin"), back.opened)
        assertEquals(newCue, File(psx, "Nights.cue").readText())
        assertEquals("new data", File(psx, "Nights (Track 1).bin").readText())
        assertEquals("new audio", File(psx, "Nights (Track 2).bin").readText())
        assertEquals(emptyList<String>(), provisionals())
    }

    @Test
    fun cancellingAnUpdateKeepsTheOldCopyConsistent() {
        oldCopyInCache()
        val game = game("Nights.cue", newCue.length.toLong())
        val cancelled = FakeServer(updatedServer, failOpen = mapOf("ps1/Nights (Track 2).bin" to CancellationException("Annuler")))

        assertThrows(CancellationException::class.java) { runBlocking { onlineCache(cancelled).launch(game) } }
        assertEquals(cue, File(psx, "Nights.cue").readText())
        assertEquals("data", File(psx, "Nights (Track 1).bin").readText())
        assertNull(cachedFiles(psx, "Nights.cue", newCue.length.toLong(), lenient = true))
        assertEquals(emptyList<String>(), leftovers())
    }

    @Test
    fun interruptedUpdateOfOneDiscNeverMixesVersionsUnderAnUnchangedPlaylist() = runBlocking {
        // Le .m3u ne change pas, seuls les disques sont remplacés sur le serveur. Dans le cache, la piste
        // du disque 1 avait été effacée pour faire de la place.
        val m3u = "Disc 1.cue\nDisc 2.cue\n"
        val oldDisc = { n: Int -> "FILE \"Disc $n.bin\" BINARY\n  TRACK 01 MODE1/2352\n" }
        val newDisc = { n: Int -> "FILE \"Disc $n.bin\" BINARY\n  TRACK 01 MODE2/2352\n  REM v2\n" }
        cached("FF7/FF7.m3u", m3u)
        cached("FF7/Disc 1.cue", oldDisc(1))
        cached("FF7/Disc 2.cue", oldDisc(2))
        cached("FF7/Disc 2.bin", "old 2")
        val server = mapOf(
            "ps1/FF7/FF7.m3u" to m3u.toByteArray(),
            "ps1/FF7/Disc 1.cue" to newDisc(1).toByteArray(),
            "ps1/FF7/Disc 2.cue" to newDisc(2).toByteArray(),
            "ps1/FF7/Disc 1.bin" to "new disc 1".toByteArray(),
            "ps1/FF7/Disc 2.bin" to "new disc 2".toByteArray(),
        )
        val game = game("FF7/FF7.m3u", m3u.length.toLong())
        val cut = IOException("coupure")

        assertThrows(IOException::class.java) {
            runBlocking { onlineCache(FakeServer(server, failOpen = mapOf("ps1/FF7/Disc 2.bin" to cut))).launch(game) }
        }
        // Ni l'ancien disque 1 avec la nouvelle piste, ni l'inverse : le jeu reste incomplet.
        assertEquals(oldDisc(1), File(psx, "FF7/Disc 1.cue").readText())
        assertFalse(File(psx, "FF7/Disc 1.bin").exists())
        assertNull(cachedFiles(psx, "FF7/FF7.m3u", m3u.length.toLong()))
        assertNull(cachedFiles(psx, "FF7/FF7.m3u", m3u.length.toLong(), lenient = true))

        onlineCache(FakeServer(server)).launch(game)
        assertEquals(newDisc(1), File(psx, "FF7/Disc 1.cue").readText())
        assertEquals("new disc 2", File(psx, "FF7/Disc 2.bin").readText())
        assertEquals(5, cachedFiles(psx, "FF7/FF7.m3u", m3u.length.toLong())!!.size)
    }

    @Test
    fun networkErrorWhileCheckingTracksStopsTheCopy() {
        // Avant : une erreur en demandant la taille d'une piste la faisait passer pour absente du serveur.
        val game = game("Nights.cue", newCue.length.toLong())
        val cut = IOException("coupure")
        val flaky = FakeServer(updatedServer, failSize = mapOf("ps1/Nights (Track 2).bin" to cut))

        val e = assertThrows(IOException::class.java) { runBlocking { onlineCache(flaky).launch(game) } }
        assertSame(cut, e)
        assertFalse(File(psx, "Nights.cue").exists())
        assertEquals(emptyList<String>(), leftovers())
    }

    @Test
    fun fileChangedOnTheServerDuringTheCopyIsNotKept() {
        // Le serveur annonce 100 octets mais n'en envoie que 5 (fichier réécrit pendant la copie).
        val changing = object : SourceFs {
            override fun list(rel: String) = emptyList<Entry>()
            override fun size(rel: String) = 100L
            override fun open(rel: String): InputStream = ByteArrayInputStream("short".toByteArray())
        }
        val cache = GameCache({ root.path }, { 32 }) { changing }
        val e = assertThrows(UiException::class.java) { runBlocking { cache.launch(game("Crash.chd", 100)) } }
        assertEquals(R.string.err_copy_changed, e.res)
        assertFalse(File(psx, "Crash.chd").exists())
        assertEquals(emptyList<String>(), leftovers())
        assertEquals(emptyList<String>(), provisionals())
    }

    @Test
    fun mainFileRemovedFromTheServerSaysSo() {
        val e = assertThrows(UiException::class.java) {
            runBlocking { onlineCache(FakeServer(emptyMap())).launch(game("Nights.cue", 99)) }
        }
        assertEquals(R.string.err_file_not_found, e.res)
    }

    @Test
    fun refusedNameInAnOldCachedDescriptorIsFixedByTheServer() = runBlocking {
        // Un .cue copié par la 0.2.0, avec un nom refusé : il ne doit pas bloquer le jeu pour toujours.
        val m3u = cached("Game/Game.m3u", "Disc.cue\n")
        cached("Game/Disc.cue", "FILE \"../../Download/x.apk\" BINARY\n")
        val fixed = "FILE \"Disc.bin\" BINARY\n"
        val server = FakeServer(
            mapOf(
                "ps1/Game/Game.m3u" to m3u.readBytes(),
                "ps1/Game/Disc.cue" to fixed.toByteArray(),
                "ps1/Game/Disc.bin" to "disc".toByteArray(),
            ),
        )
        val game = game("Game/Game.m3u", m3u.length())

        // Serveur éteint : c'est l'erreur du serveur qu'on montre, puisque c'est lui qui peut réparer.
        val e = assertThrows(IOException::class.java) { runBlocking { offlineCache().launch(game) } }
        assertSame(down, e)

        onlineCache(server).launch(game)
        assertEquals(fixed, File(psx, "Game/Disc.cue").readText())
        assertNotNull(cachedFiles(psx, "Game/Game.m3u", m3u.length()))
    }

    @Test
    fun optionalFileMissingOnTheServerIsSkipped() = runBlocking {
        val ccd = "[CloneCD]\n"
        val server = FakeServer(mapOf("ps1/Vib.ccd" to ccd.toByteArray(), "ps1/Vib.img" to "image".toByteArray()))
        val prepared = onlineCache(server).launch(game("Vib.ccd", ccd.length.toLong()))
        assertEquals("Vib.ccd", prepared.file.name)
        assertTrue(File(psx, "Vib.img").isFile)
        assertFalse(File(psx, "Vib.sub").exists())
    }

    @Test
    fun multiDiscPlaylistNeedsEveryDiscAndTrack() = runBlocking {
        val m3u = cached("FF7/FF7.m3u", "# disques\nFF7 (Disc 1).cue\r\nFF7 (Disc 2).cue\n")
        cached("FF7/FF7 (Disc 1).cue", "FILE \"FF7 (Disc 1).bin\" BINARY\n")
        cached("FF7/FF7 (Disc 2).cue", "FILE \"FF7 (Disc 2).bin\" BINARY\n")
        cached("FF7/FF7 (Disc 1).bin", "1")
        val disc2 = cached("FF7/FF7 (Disc 2).bin", "2")

        val all = cachedFiles(psx, "FF7/FF7.m3u", m3u.length())
        assertEquals(
            listOf("FF7.m3u", "FF7 (Disc 1).cue", "FF7 (Disc 2).cue", "FF7 (Disc 1).bin", "FF7 (Disc 2).bin"),
            all!!.map { it.name },
        )

        disc2.delete()
        assertNull(cachedFiles(psx, "FF7/FF7.m3u", m3u.length()))
        assertNull(cachedFiles(psx, "FF7/FF7.m3u", m3u.length(), lenient = true))
    }

    @Test
    fun multiDiscPlaylistIsCopiedWithEveryDisc() = runBlocking {
        val m3u = "FF7 (Disc 1).cue\nFF7 (Disc 2).cue\n"
        val server = FakeServer(
            mapOf(
                "ps1/FF7/FF7.m3u" to m3u.toByteArray(),
                "ps1/FF7/FF7 (Disc 1).cue" to "FILE \"FF7 (Disc 1).bin\" BINARY\n".toByteArray(),
                "ps1/FF7/FF7 (Disc 2).cue" to "FILE \"FF7 (Disc 2).bin\" BINARY\n".toByteArray(),
                "ps1/FF7/FF7 (Disc 1).bin" to "1".toByteArray(),
                "ps1/FF7/FF7 (Disc 2).bin" to "2".toByteArray(),
            ),
        )
        val game = game("FF7/FF7.m3u", m3u.length.toLong())
        onlineCache(server).launch(game)
        assertEquals(5, cachedFiles(psx, "FF7/FF7.m3u", m3u.length.toLong())!!.size)
        assertEquals(emptyList<String>(), leftovers())
        offlineCache().launch(game)
        assertEquals(1, connections)
    }

    @Test
    fun cloneCdWithoutSubFileStartsWhenTheServerIsOff() = runBlocking {
        val ccd = cached("Vib.ccd", "[CloneCD]\n")
        cached("Vib.img", "image")
        val game = game("Vib.ccd", ccd.length())

        // Le .sub est facultatif : sans lui, le jeu n'est pas « entier », mais se lance quand même
        // si le serveur ne répond pas.
        assertNull(cachedFiles(psx, "Vib.ccd", ccd.length()))
        assertNotNull(cachedFiles(psx, "Vib.ccd", ccd.length(), lenient = true))
        val prepared = offlineCache().launch(game)
        assertEquals(ccd.canonicalPath, prepared.file.canonicalPath)
        assertEquals(1, connections)
    }

    @Test
    fun cloneCdWithoutImageNeverStartsFromTheCache() {
        val ccd = cached("Vib.ccd", "[CloneCD]\n")
        cached("Vib.sub", "sub")
        assertNull(cachedFiles(psx, "Vib.ccd", ccd.length(), lenient = true))
    }

    @Test
    fun cancellingTheCopyDoesNotStartTheGameFromTheCache() {
        val ccd = cached("Vib.ccd", "[CloneCD]\n")
        cached("Vib.img", "image")
        val cache = GameCache({ root.path }, { 32 }) { throw CancellationException("annulé") }
        assertThrows(CancellationException::class.java) {
            runBlocking { cache.launch(game("Vib.ccd", ccd.length())) }
        }
    }

    @Test
    fun cachedDescriptorCannotPointOutsideTheConsoleFolder() {
        val main = cached("Evil.cue", "FILE \"../../Download/x.apk\" BINARY\n")
        assertThrows(IOException::class.java) { cachedFiles(psx, "Evil.cue", main.length()) }
    }

    @Test
    fun playlistThatNamesItselfDoesNotLoop() {
        val m3u = cached("Loop.m3u", "Loop.m3u\n./Loop.m3u\nGame.chd\n")
        cached("Game.chd", "x")
        assertEquals(listOf("Loop.m3u", "Game.chd"), cachedFiles(psx, "Loop.m3u", m3u.length())!!.map { it.name })
    }

    @Test
    fun byteOrderMarkFromWindowsNotepadIsIgnored() {
        val m3u = cached("Saved with BOM.m3u", "\uFEFFDisc 1.cue\nDisc 2.cue\n")
        val cue = cached("Track list.cue", "\uFEFFFILE \"Track 1.bin\" BINARY\nFILE \"Track 2.bin\" BINARY\n")
        assertEquals(listOf("Disc 1.cue", "Disc 2.cue"), companions(m3u).map { it.name })
        assertEquals(listOf("Track 1.bin", "Track 2.bin"), companions(cue).map { it.name })
    }

    @Test
    fun descriptorUnderAProvisionalNameIsReadAsItsRealType() {
        val staged = cached("Vib.ccd.new", "[CloneCD]\n")
        assertEquals(listOf("Vib.img", "Vib.sub"), companions(staged, "Vib.ccd").map { it.name })
        assertEquals(emptyList<Companion>(), companions(staged))
    }

    @Test
    fun largeDiscImagesAreNeverOpenedToLookForTracks() {
        // Un .chd ou un .iso n'est pas un descripteur : companions() ne doit pas le lire.
        val iso = File(psx, "Big.iso").apply { parentFile!!.mkdirs(); writeText("FILE \"ghost.bin\" BINARY\n") }
        assertEquals(emptyList<Companion>(), companions(iso))
        assertEquals(listOf(iso.name), cachedFiles(psx, "Big.iso", iso.length())!!.map { it.name })
    }

    /** Une ROM de N64 comme Mortal Kombat 4, en plus petit. */
    private val rom = ByteArray(10_000) { (it * 7).toByte() }

    @Test
    fun serverThatStopsAnsweringMidCopyIsRetriedAndTheCopyResumes() = runBlocking {
        val server = StallingServer(mapOf("ps1/MK4.z64" to rom), cutAfter = 4_000, stalls = 1)

        val prepared = onlineCache(server).launch(game("MK4.z64", rom.size.toLong()))

        assertArrayEquals(rom, prepared.file.readBytes())
        // Deuxième connexion, qui reprend après les 4 000 octets déjà copiés.
        assertEquals(2, connections)
        assertEquals(listOf(0L, 4_000L), server.offsets)
        assertEquals(listOf(2 to GameCache.ATTEMPTS), retries)
        assertEquals(emptyList<String>(), leftovers())
        assertEquals(emptyList<String>(), provisionals())
    }

    @Test
    fun serverThatNeverAnswersEndsWithAClearMessage() {
        val server = StallingServer(mapOf("ps1/MK4.z64" to rom), cutAfter = 3_000, stalls = 99)

        val e = assertThrows(UiException::class.java) {
            runBlocking { onlineCache(server).launch(game("MK4.z64", rom.size.toLong())) }
        }
        assertEquals(R.string.err_server_timeout, e.res)
        assertEquals(listOf("NAS"), e.args.toList())
        assertEquals(GameCache.ATTEMPTS, connections)
        assertEquals(listOf(2 to 3, 3 to 3), retries)
        // Chaque essai repart de là où le précédent s'est arrêté (ici, toujours au même endroit).
        assertEquals(listOf(0L, 3_000L, 3_000L), server.offsets)
        assertFalse(File(psx, "MK4.z64").exists())
        assertEquals(emptyList<String>(), leftovers())
    }

    @Test
    fun connectionDroppingEveryTimeSaysSo() {
        val reset = { TransportException(SocketException("Connection reset")) }
        val server = StallingServer(mapOf("ps1/MK4.z64" to rom), cutAfter = 5_000, stalls = 99, error = reset)

        val e = assertThrows(UiException::class.java) {
            runBlocking { onlineCache(server).launch(game("MK4.z64", rom.size.toLong())) }
        }
        assertEquals(R.string.err_server_connection_lost, e.res)
        assertEquals(GameCache.ATTEMPTS, connections)
    }

    @Test
    fun serverTooSlowToAcceptTheConnectionIsRetried() = runBlocking {
        val server = FakeServer(mapOf("ps1/MK4.z64" to rom))
        var calls = 0
        val cache = GameCache({ root.path }, { 32 }, retryDelayMs = 0) {
            if (++calls == 1) throw smbTimeout()
            server
        }

        val prepared = cache.launch(game("MK4.z64", rom.size.toLong()))

        assertArrayEquals(rom, prepared.file.readBytes())
        assertEquals(2, calls)
        assertEquals(listOf(2 to 3), retries)
    }

    @Test
    fun serverOffIsNotRetried() {
        // Injoignable (éteint) : pas d'attente inutile, l'erreur arrive tout de suite.
        assertThrows(IOException::class.java) { runBlocking { offlineCache().launch(game("MK4.z64", 10)) } }
        assertEquals(1, connections)
        assertEquals(emptyList<Pair<Int, Int>>(), retries)
    }

    @Test
    fun aCopyLeftHalfDoneByAnEarlierLaunchStartsOver() = runBlocking {
        // Reste d'une copie d'un lancement précédent (appli tuée) : jamais recollé à la nouvelle copie.
        File(psx, "MK4.z64.new.part").apply { parentFile!!.mkdirs(); writeText("vieux morceau") }
        val server = StallingServer(mapOf("ps1/MK4.z64" to rom), cutAfter = 0, stalls = 0)

        val prepared = onlineCache(server).launch(game("MK4.z64", rom.size.toLong()))

        assertArrayEquals(rom, prepared.file.readBytes())
        assertEquals(listOf(0L), server.offsets)
        assertEquals(emptyList<String>(), leftovers())
    }

    @Test
    fun timeoutsAreRecognisedButServerAnswersAreNot() {
        assertTrue(SmbFs.isNetworkGlitch(smbTimeout()))
        assertTrue(SmbFs.isTimeout(smbTimeout()))
        assertTrue(SmbFs.isNetworkGlitch(TransportException(SocketException("Connection reset"))))
        assertFalse(SmbFs.isTimeout(TransportException(SocketException("Connection reset"))))
        // Réponses du serveur, erreurs de RetroSalon ou du disque de la TV : réessayer n'y changerait rien.
        assertFalse(SmbFs.isNetworkGlitch(IOException("No space left on device")))
        assertFalse(SmbFs.isNetworkGlitch(FileNotFoundException("MK4.z64")))
        assertFalse(SmbFs.isNetworkGlitch(UiException(R.string.err_copy_changed, "MK4.z64", cause = smbTimeout())))
    }

    @Test
    fun aCopyLeftByAnEarlierLaunchIsNotResumedEvenAfterARetry() = runBlocking {
        // Morceau d'une ancienne version laissé par un lancement interrompu (TV mise en veille) ; puis le premier
        // essai échoue avant d'atteindre ce fichier. Le deuxième ne doit pas le recoller à la nouvelle copie.
        File(psx, "MK4.z64.new.part").apply { parentFile!!.mkdirs(); writeBytes(ByteArray(4_000) { 0x55 }) }
        val server = FakeServer(mapOf("ps1/MK4.z64" to rom))
        var calls = 0
        val cache = GameCache({ root.path }, { 32 }, retryDelayMs = 0) {
            if (++calls == 1) throw smbTimeout()
            server
        }

        val prepared = cache.launch(game("MK4.z64", rom.size.toLong()))

        assertArrayEquals(rom, prepared.file.readBytes())
        assertEquals(2, calls)
    }

    @Test
    fun serverStillUnreachableRightAfterADropIsTriedAgain() = runBlocking {
        // Le Wi-Fi tombe en pleine copie ; au deuxième essai la TV n'a pas encore retrouvé le réseau.
        val reset = { TransportException(SocketException("Connection reset")) }
        val server = StallingServer(mapOf("ps1/MK4.z64" to rom), cutAfter = 4_000, stalls = 1, error = reset)
        var calls = 0
        val cache = GameCache({ root.path }, { 32 }, retryDelayMs = 0) {
            if (++calls == 2) throw UiException(R.string.err_server_unreachable, 445, "IPv4 192.168.1.20")
            server
        }

        val prepared = cache.launch(game("MK4.z64", rom.size.toLong()))

        assertArrayEquals(rom, prepared.file.readBytes())
        assertEquals(3, calls)
        assertEquals(listOf(2 to 3, 3 to 3), retries)
        // Le troisième essai reprend la partie copiée au premier.
        assertEquals(listOf(0L, 4_000L), server.offsets)
    }

    @Test
    fun aGameReplacedOnTheServerBetweenTwoTriesIsCopiedAgainFromTheStart() = runBlocking {
        // Entre les deux essais, le jeu est remplacé sur le serveur par une autre version de même taille.
        val newer = ByteArray(rom.size) { (it * 3 + 1).toByte() }
        val before = StallingServer(mapOf("ps1/MK4.z64" to rom), cutAfter = 4_000, stalls = 1)
        val replaced = StallingServer(mapOf("ps1/MK4.z64" to newer), cutAfter = 0, stalls = 0)
        val after = object : SourceFs by replaced {
            override fun stamp(rel: String) = "${size(rel)}/nouvelle date"
        }
        var calls = 0
        val cache = GameCache({ root.path }, { 32 }, retryDelayMs = 0) { if (++calls == 1) before else after }

        val prepared = cache.launch(game("MK4.z64", rom.size.toLong()))

        // Pas le début de l'ancienne version collé à la fin de la nouvelle.
        assertArrayEquals(newer, prepared.file.readBytes())
        assertEquals(listOf(0L), replaced.offsets)
    }

    @Test
    fun aPasswordRefusedAfterADropIsNotTriedAgain() {
        // Des échecs de connexion répétés peuvent bloquer le compte sur un PC Windows.
        val server = StallingServer(mapOf("ps1/MK4.z64" to rom), cutAfter = 4_000, stalls = 1)
        val refused = SMBApiException(NtStatus.STATUS_LOGON_FAILURE.value, SMB2MessageCommandCode.SMB2_SESSION_SETUP, null)
        var calls = 0
        val cache = GameCache({ root.path }, { 32 }, retryDelayMs = 0) {
            if (++calls >= 2) throw refused
            server
        }

        val e = assertThrows(SMBApiException::class.java) {
            runBlocking { cache.launch(game("MK4.z64", rom.size.toLong())) }
        }
        assertSame(refused, e)
        assertEquals(2, calls)
        assertEquals(listOf(2 to 3), retries)
    }

    /** Source avec deux consoles ; la liste du dossier n64 échoue avec [n64Error]. */
    private fun twoConsoles(n64Error: () -> Throwable) = object : SourceFs {
        override fun list(rel: String): List<Entry> = when (rel) {
            "" -> listOf(Entry("n64", true, 0), Entry("snes", true, 0))
            "n64" -> throw n64Error()
            else -> listOf(Entry("Zelda.sfc", false, 10))
        }
        override fun open(rel: String): InputStream = throw FileNotFoundException(rel)
        override fun size(rel: String): Long = throw FileNotFoundException(rel)
    }

    @Test
    fun scanStopsOnATimeoutSoTheLibraryKeepsItsGames() {
        // Avant : le dossier lent était sauté sans rien dire, et ses jeux disparaissaient de la bibliothèque.
        // Maintenant le scan de la source échoue, et la bibliothèque garde ses jeux d'avant.
        assertThrows(TransportException::class.java) { Scanner.scan(twoConsoles(::smbTimeout), "nas", "jeux") }
        // Un dossier illisible pour une autre raison est toujours simplement sauté.
        val games = Scanner.scan(twoConsoles { IOException("accès refusé") }, "nas", "jeux")
        assertEquals(listOf("Zelda.sfc"), games.map { it.rel })
    }
}
