package fr.retrosalon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/** Système de fichiers en mémoire : chemin relatif → contenu du dossier. */
private class FakeFs(private val tree: Map<String, List<Entry>>) : SourceFs {
    override fun list(rel: String) = tree[rel] ?: emptyList()
    override fun open(rel: String): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun size(rel: String) = 0L
}

private fun f(name: String, size: Long = 1) = Entry(name, false, size)
private fun d(name: String) = Entry(name, true, 0)

class ScannerTest {
    @Test
    fun recognisesConsoleFoldersAndAliases() {
        val fs = FakeFs(
            mapOf(
                "" to listOf(d("SNES"), d("ps1"), d("GameCube"), d("photos"), f("readme.txt")),
                "SNES" to listOf(f("Super Metroid (Europe).sfc"), f("notes.txt")),
                "ps1" to listOf(d("Final Fantasy VII"), f("Crash Bandicoot (Europe).chd")),
                "ps1/Final Fantasy VII" to listOf(
                    f("Final Fantasy VII (Europe).m3u"),
                    f("Final Fantasy VII (Europe) (Disc 1).chd"),
                    f("Final Fantasy VII (Europe) (Disc 2).chd"),
                ),
                "GameCube" to listOf(f("Zelda Wind Waker.rvz")),
                "photos" to listOf(f("img.iso")),
            ),
        )
        val games = Scanner.scan(fs, "src", rootName = "roms")
        val names = games.map { "${it.systemId}:${it.rel}" }.toSet()
        assertEquals(
            setOf(
                "snes:Super Metroid (Europe).sfc",
                "psx:Crash Bandicoot (Europe).chd",
                "psx:Final Fantasy VII/Final Fantasy VII (Europe).m3u",
                "gc:Zelda Wind Waker.rvz",
            ),
            names,
        )
        assertEquals("ps1", games.first { it.systemId == "psx" }.folder)
    }

    @Test
    fun hidesCueTracks() {
        val fs = FakeFs(
            mapOf(
                "" to listOf(d("saturn")),
                "saturn" to listOf(f("Nights (Europe).cue"), f("Nights (Europe) (Track 1).bin"), f("Nights (Europe) (Track 2).bin")),
            ),
        )
        assertEquals(listOf("Nights (Europe).cue"), Scanner.scan(fs, "s", "roms").map { it.rel })
    }

    @Test
    fun hidesTrackFilesTheConsoleWouldOtherwiseList() {
        // .img est une extension PlayStation : sans le masquage, la piste serait listée comme un jeu.
        val fs = FakeFs(
            mapOf(
                "" to listOf(d("psx")),
                "psx" to listOf(f("Vagrant Story (Europe).ccd"), f("Vagrant Story (Europe).img"), f("Vagrant Story (Europe).sub")),
            ),
        )
        assertEquals(listOf("Vagrant Story (Europe).ccd"), Scanner.scan(fs, "s", "roms").map { it.rel })
    }

    @Test
    fun sourcePointingAtConsoleFolder() {
        val fs = FakeFs(mapOf("" to listOf(f("Sonic.md"), f("cover.png"))))
        val games = Scanner.scan(fs, "s", rootName = "megadrive")
        assertEquals(listOf("megadrive:Sonic.md"), games.map { "${it.systemId}:${it.rel}" })
        assertEquals("", games.single().folder)
    }

    @Test
    fun cleansTitles() {
        val g = Game("s", "psx", "psx", "dir/Metal Gear Solid (Europe) (Disc 1) [!].chd", 1)
        assertEquals("Metal Gear Solid", g.title)
        assertEquals("Europe · Disc 1 · !", g.tags)
    }
}

class SafTest {
    @Test
    fun internalStorage() {
        assertEquals("primary:RetroSalon/jeux/psx/a.chd", Saf.docId("/storage/emulated/0/RetroSalon/jeux/psx/a.chd"))
        assertEquals("primary:", Saf.docId("/storage/emulated/0"))
    }

    @Test
    fun usbDrive() {
        assertEquals("1A2B-3C4D:roms/gc", Saf.docId("/storage/1A2B-3C4D/roms/gc/"))
    }

    @Test
    fun outsideStorage() {
        assertNull(Saf.docId("/data/user/0/fr.retrosalon/files/x"))
    }
}

class HostTest {
    @Test
    fun ipv4AndNames() {
        assertEquals("192.168.1.20" to 445, SmbFs.parseHost("192.168.1.20"))
        assertEquals("192.168.1.20" to 4450, SmbFs.parseHost(" 192.168.1.20:4450 "))
        assertEquals("nas.local" to 445, SmbFs.parseHost("smb://nas.local/"))
        assertEquals("NAS" to 445, SmbFs.parseHost("\\\\NAS"))
    }

    @Test
    fun fullShareAddressesKeepOnlyTheServer() {
        assertEquals("nas.local" to 445, SmbFs.parseHost("SMB://nas.local/jeux/retro"))
        assertEquals("NAS" to 445, SmbFs.parseHost("\\\\NAS\\jeux"))
        assertEquals("fd00::20" to 4450, SmbFs.parseHost("smb://[fd00::20]:4450/jeux"))
    }

    @Test
    fun ipv6() {
        assertEquals("fd00::20" to 445, SmbFs.parseHost("fd00::20"))
        assertEquals("fd00::20" to 4450, SmbFs.parseHost("[fd00::20]:4450"))
        assertEquals("::1" to 445, SmbFs.parseHost("[::1]"))
    }

    @Test
    fun fallsBackToIpv4WhenIpv6Fails() {
        // localhost résout en ::1 et 127.0.0.1 ; un serveur n'écoute qu'en IPv4.
        java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1")).use { server ->
            val chosen = SmbFs.pickAddress("localhost", server.localPort)
            assertTrue("attendu IPv4, obtenu $chosen", chosen is java.net.Inet4Address)
        }
    }

    @Test
    fun prefersIpv6WhenItWorks() {
        val v6 = runCatching { java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("::1")) }.getOrNull() ?: return
        v6.use { server ->
            java.net.ServerSocket(server.localPort, 1, java.net.InetAddress.getByName("127.0.0.1")).use {
                val chosen = SmbFs.pickAddress("localhost", server.localPort)
                assertTrue("attendu IPv6, obtenu $chosen", chosen is java.net.Inet6Address)
            }
        }
    }
}

class PathSafetyTest {
    private val base = java.nio.file.Files.createTempDirectory("retrosalon").toFile()

    @Test
    fun acceptsFilesInsideTheConsoleFolder() {
        assertEquals(java.io.File(base, "FF7/disc1.cue").canonicalPath, childInside(base, "FF7/disc1.cue").canonicalPath)
        assertEquals(java.io.File(base, "discs/a.bin").canonicalPath, childInside(base, "discs\\a.bin").canonicalPath)
    }

    @Test
    fun rejectsServerNamesThatLeaveTheFolder() {
        for (bad in listOf("../x.apk", "a/../../x", "..\\..\\Download\\x.apk", "/etc/passwd", "", "..")) {
            org.junit.Assert.assertThrows(bad, java.io.IOException::class.java) { childInside(base, bad) }
        }
    }
}
