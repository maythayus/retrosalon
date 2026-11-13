package fr.retrosalon.data

/**
 * Catalogue des consoles et des émulateurs.
 *
 * Les intents de lancement reprennent la configuration Android publique d'ES-DE
 * (resources/systems/android/es_systems.xml et es_find_rules.xml, licence MIT).
 */

/** Comment le chemin du jeu est transmis à l'émulateur. */
enum class UriMode {
    /** Chemin absolu (/storage/...). RetroArch, ePSXe. */
    PATH,

    /**
     * URI SAF (content://com.android.externalstorage.documents/tree/.../document/...).
     * L'émulateur doit avoir reçu l'accès au dossier de la console dans ses propres réglages.
     */
    SAF,

    /** URI FileProvider de RetroSalon : l'accès est accordé automatiquement au lancement. */
    PROVIDER,
}

sealed interface Arg {
    /** Le jeu, au format défini par [Launcher.uriMode]. */
    data object Rom : Arg

    /** Le chemin absolu du jeu. */
    data object RomPath : Arg
    data class Str(val value: String) : Arg
    data class Bool(val value: Boolean) : Arg

    /** Chemin du cœur libretro dans les données de RetroArch. */
    data class RaCore(val core: String) : Arg

    /** Fichier retroarch.cfg de RetroArch. */
    data object RaConfig : Arg

    /** Paramètres de ligne de commande de MAME4droid (dossiers des ROM). */
    data object MameRomPath : Arg
}

data class Launcher(
    val id: String,
    val label: String,
    /** Paquet → activité, par ordre de préférence. */
    val components: List<Pair<String, String>>,
    val action: String? = null,
    val category: String? = null,
    val dataRom: Boolean = false,
    val extras: List<Pair<String, Arg>> = emptyList(),
    val uriMode: UriMode = UriMode.SAF,
    /** Version d'Android minimale de l'émulateur, quand elle dépasse celle de RetroSalon (API 26). */
    val minSdk: Int = 26,
)

data class GameSystem(
    val id: String,
    val name: String,
    /** Noms de dossier reconnus (en minuscules), en plus de [id]. */
    val aliases: Set<String>,
    val extensions: Set<String>,
    val launchers: List<Launcher>,
    /** Console sur disque : les .bin/.img des .cue/.gdi sont masqués dans la liste. */
    val disc: Boolean = false,
)

private const val VIEW = "android.intent.action.VIEW"
private const val MAIN = "android.intent.action.MAIN"

/** RetroArch intégré à RetroSalon d'abord (version perso), puis les applis RetroArch installées. */
private val RETROARCH = listOf(EmbeddedRetroArch.SELF, "com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32")
    .map { it to EmbeddedRetroArch.ACTIVITY }

private fun ra(core: String, label: String) = Launcher(
    id = "ra:$core",
    label = "RetroArch · $label",
    components = RETROARCH,
    action = MAIN,
    extras = listOf(
        "ROM" to Arg.RomPath,
        "LIBRETRO" to Arg.RaCore(core),
        "CONFIGFILE" to Arg.RaConfig,
    ),
    uriMode = UriMode.PATH,
)

object Emulators {
    val PPSSPP = Launcher(
        "ppsspp", "PPSSPP",
        listOf(
            "org.ppsspp.ppsspp" to "org.ppsspp.ppsspp.PpssppActivity",
            "org.ppsspp.ppssppgold" to "org.ppsspp.ppsspp.PpssppActivity",
        ),
        action = VIEW, category = "android.intent.category.DEFAULT", dataRom = true,
    )
    val DOLPHIN = Launcher(
        "dolphin", "Dolphin",
        listOf("org.dolphinemu.dolphinemu" to "org.dolphinemu.dolphinemu.ui.main.TvMainActivity"),
        action = MAIN, category = "android.intent.category.LEANBACK_LAUNCHER",
        extras = listOf("AutoStartFile" to Arg.Rom),
    )
    val DUCKSTATION = Launcher(
        "duckstation", "DuckStation",
        listOf("com.github.stenzek.duckstation" to "com.github.stenzek.duckstation.EmulationActivity"),
        extras = listOf("resumeState" to Arg.Bool(false), "bootPath" to Arg.Rom),
    )
    val EPSXE = Launcher(
        "epsxe", "ePSXe",
        listOf("com.epsxe.ePSXe" to "com.epsxe.ePSXe.ePSXe"),
        action = MAIN, extras = listOf("com.epsxe.ePSXe.isoName" to Arg.RomPath),
        uriMode = UriMode.PATH,
    )
    val FLYCAST = Launcher(
        "flycast", "Flycast",
        listOf("com.flycast.emulator" to "com.flycast.emulator.MainActivity"),
        action = VIEW, dataRom = true,
    )
    val REDREAM = Launcher(
        "redream", "Redream",
        listOf("io.recompiled.redream" to "io.recompiled.redream.MainActivity"),
        action = VIEW, dataRom = true,
    )
    val M64PLUS = Launcher(
        "m64plus", "M64Plus FZ",
        listOf("org.mupen64plusae.v3.fzurita.pro", "org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.fzurita.amazon")
            .map { it to "paulscode.android.mupen64plusae.SplashActivity" },
        action = VIEW, dataRom = true,
    )
    val MELONDS = Launcher(
        "melonds", "melonDS",
        listOf("me.magnum.melonds", "me.magnum.melonds.nightly")
            .map { it to "me.magnum.melonds.ui.emulator.EmulatorActivity" },
        action = "me.magnum.melonds.LAUNCH_ROM", extras = listOf("uri" to Arg.Rom),
    )
    val DRASTIC = Launcher(
        "drastic", "DraStic",
        listOf("com.dsemu.drastic" to "com.dsemu.drastic.DraSticActivity"),
        dataRom = true,
    )
    val AZAHAR = Launcher(
        "azahar", "Azahar",
        listOf("org.azahar_emu.azahar" to "org.citra.citra_emu.activities.EmulationActivity"),
        dataRom = true,
        minSdk = 29,
    )
    val YABA = Launcher(
        "yaba", "Yaba Sanshiro 2",
        listOf("org.devmiyax.yabasanshioro2.pro", "org.devmiyax.yabasanshioro2")
            .map { it to "org.uoyabause.android.Yabause" },
        action = VIEW, extras = listOf("org.uoyabause.android.FileNameUri" to Arg.Rom),
    )
    val NETHERSX2 = Launcher(
        "nethersx2", "NetherSX2",
        listOf("xyz.aethersx2.android" to "xyz.aethersx2.android.EmulationActivity"),
        action = MAIN, extras = listOf("bootPath" to Arg.Rom),
    )
    val ARMSX2 = Launcher(
        "armsx2", "ARMSX2",
        listOf("com.armsx2", "come.nanodata.armsx2", "com.armsx2.nightly").map { it to "com.armsx2.MainActivity" },
        action = VIEW, dataRom = true,
    )
    val MAME4DROID = Launcher(
        "mame4droid", "MAME4droid",
        listOf("com.seleuco.mame4d2024", "com.seleuco.mame4droid").map { it to "com.seleuco.mame4droid.MAME4droid" },
        action = VIEW, dataRom = true, extras = listOf("cli_params" to Arg.MameRomPath),
        uriMode = UriMode.PROVIDER,
    )
    val EDEN = Launcher(
        "eden", "Eden",
        listOf("dev.eden.eden_emulator" to "org.yuzu.yuzu_emu.activities.EmulationActivity"),
        action = "android.nfc.action.TECH_DISCOVERED", dataRom = true,
        uriMode = UriMode.PROVIDER,
        // APK « legacy » : Android 10 ; APK principal : Android 13.
        minSdk = 29,
    )

    /** Tous les paquets connus (doit rester aligné avec <queries> du manifeste). */
    val allPackages: Set<String> by lazy {
        Systems.all.flatMap { s -> s.launchers.flatMap { l -> l.components.map { it.first } } }.toSet() - EmbeddedRetroArch.SELF
    }
}

object Systems {
    private val cartridge = setOf("zip", "7z")

    val all: List<GameSystem> = listOf(
        GameSystem("nes", "Nintendo NES", setOf("famicom", "fc"), setOf("nes", "fds", "unf", "unif") + cartridge,
            listOf(ra("mesen", "Mesen"), ra("fceumm", "FCEUmm"))),
        GameSystem("snes", "Super Nintendo", setOf("sfc", "superfamicom", "supernintendo"),
            setOf("sfc", "smc", "fig", "swc", "bs", "st") + cartridge, listOf(ra("snes9x", "Snes9x"))),
        GameSystem("n64", "Nintendo 64", setOf("nintendo64"), setOf("n64", "z64", "v64") + cartridge,
            listOf(Emulators.M64PLUS, ra("mupen64plus_next_gles3", "Mupen64Plus-Next"))),
        GameSystem("gb", "Game Boy", setOf("gameboy"), setOf("gb", "dmg") + cartridge,
            listOf(ra("gambatte", "Gambatte"), ra("mgba", "mGBA"))),
        GameSystem("gbc", "Game Boy Color", setOf("gameboycolor"), setOf("gbc", "cgb") + cartridge,
            listOf(ra("gambatte", "Gambatte"), ra("mgba", "mGBA"))),
        GameSystem("gba", "Game Boy Advance", setOf("gameboyadvance"), setOf("gba", "agb") + cartridge,
            listOf(ra("mgba", "mGBA"))),
        GameSystem("nds", "Nintendo DS", setOf("ds"), setOf("nds", "dsi") + cartridge,
            listOf(Emulators.MELONDS, Emulators.DRASTIC, ra("melondsds", "melonDS DS"))),
        GameSystem("n3ds", "Nintendo 3DS", setOf("3ds"), setOf("3ds", "cci", "cxi", "3dsx", "app", "zcci", "zcxi", "z3dsx"),
            listOf(Emulators.AZAHAR)),
        GameSystem("gc", "GameCube", setOf("gamecube", "ngc"), setOf("iso", "gcm", "gcz", "rvz", "ciso", "wbfs", "wia", "dol", "elf", "tgc", "m3u"),
            listOf(Emulators.DOLPHIN), disc = true),
        GameSystem("wii", "Wii", emptySet(), setOf("iso", "rvz", "wbfs", "wia", "gcz", "ciso", "wad", "dol", "elf"),
            listOf(Emulators.DOLPHIN), disc = true),
        GameSystem("switch", "Nintendo Switch", setOf("nsw"), setOf("nsp", "xci", "nca", "nro", "nso"),
            listOf(Emulators.EDEN)),
        GameSystem("mastersystem", "Master System", setOf("sms"), setOf("sms") + cartridge,
            listOf(ra("genesis_plus_gx", "Genesis Plus GX"))),
        GameSystem("megadrive", "Mega Drive", setOf("genesis", "md"), setOf("md", "gen", "smd", "bin") + cartridge,
            listOf(ra("genesis_plus_gx", "Genesis Plus GX"))),
        GameSystem("gamegear", "Game Gear", setOf("gg"), setOf("gg") + cartridge,
            listOf(ra("genesis_plus_gx", "Genesis Plus GX"))),
        GameSystem("segacd", "Mega-CD", setOf("megacd"), setOf("chd", "cue", "iso", "m3u"),
            listOf(ra("genesis_plus_gx", "Genesis Plus GX")), disc = true),
        GameSystem("saturn", "Saturn", emptySet(), setOf("chd", "cue", "iso", "ccd", "mds", "m3u"),
            listOf(Emulators.YABA, ra("yabasanshiro", "YabaSanshiro")), disc = true),
        GameSystem("dreamcast", "Dreamcast", setOf("dc"), setOf("chd", "cdi", "gdi", "cue", "m3u"),
            listOf(Emulators.FLYCAST, Emulators.REDREAM, ra("flycast", "Flycast")), disc = true),
        GameSystem("psx", "PlayStation", setOf("ps1", "playstation"), setOf("chd", "cue", "pbp", "m3u", "iso", "img", "ccd", "ecm"),
            // PCSX ReARMed d'abord : il tourne sans BIOS (BIOS de remplacement intégré) et reste léger en 32 bits.
            listOf(Emulators.DUCKSTATION, ra("pcsx_rearmed", "PCSX ReARMed"), ra("swanstation", "SwanStation"), Emulators.EPSXE), disc = true),
        GameSystem("ps2", "PlayStation 2", setOf("playstation2"), setOf("iso", "chd", "cso", "gz", "m3u"),
            listOf(Emulators.NETHERSX2, Emulators.ARMSX2), disc = true),
        GameSystem("psp", "PSP", emptySet(), setOf("iso", "cso", "pbp", "chd", "elf"),
            listOf(Emulators.PPSSPP, ra("ppsspp", "PPSSPP"))),
        GameSystem("pcengine", "PC Engine", setOf("tg16", "turbografx16", "pce"), setOf("pce", "sgx", "cue", "chd", "ccd", "m3u") + cartridge,
            listOf(ra("mednafen_pce_fast", "Beetle PCE Fast")), disc = true),
        GameSystem("neogeo", "Neo Geo", emptySet(), cartridge,
            listOf(ra("fbneo", "FinalBurn Neo"), Emulators.MAME4DROID)),
        GameSystem("arcade", "Arcade", setOf("mame", "fbneo", "fba", "cps1", "cps2", "cps3"), cartridge,
            listOf(ra("fbneo", "FinalBurn Neo"), Emulators.MAME4DROID)),
        GameSystem("atari2600", "Atari 2600", setOf("2600"), setOf("a26", "bin") + cartridge,
            listOf(ra("stella", "Stella"))),
    )

    private val byFolderName: Map<String, GameSystem> =
        all.flatMap { s -> (s.aliases + s.id).map { it to s } }.toMap()

    fun byId(id: String): GameSystem? = all.firstOrNull { it.id == id }

    /** Reconnaît un dossier de console d'après son nom (insensible à la casse, espaces et tirets ignorés). */
    fun byFolder(name: String): GameSystem? =
        byFolderName[name.lowercase().replace(" ", "").replace("-", "").replace("_", "")]
}
