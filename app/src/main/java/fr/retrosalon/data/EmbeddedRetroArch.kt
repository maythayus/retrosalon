package fr.retrosalon.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.system.Os
import android.view.InputDevice
import android.util.Log
import com.retroarch.browser.preferences.util.ConfigFile
import com.retroarch.browser.preferences.util.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * RetroArch intégré : le moteur de RetroArch et ses cœurs sont dans l'APK de RetroSalon.
 *
 * - Les cœurs sont des bibliothèques de l'APK (lib<cœur>_libretro_android.so), qu'Android installe dans
 *   le dossier des bibliothèques de l'appli. RetroArch les trouve sous leur nom habituel par des liens
 *   symboliques, dans data/retroarch-cores, refaits avant chaque lancement (le dossier des bibliothèques
 *   change à chaque mise à jour). C'est aussi ce que fait la version Play Store de RetroArch.
 *   Pas dans data/cores : au démarrage, RetroArch y efface tous les liens symboliques.
 * - Les fichiers de RetroArch (menus, polices, profils de manettes, fiches des cœurs) sont dans
 *   assets/retroarch/ra-assets.zip, décompressé par RetroSalon dans data/ après chaque mise à jour.
 * - RetroArch tourne dans le processus « :retroarch » (voir le manifeste du module retroarch).
 */
object EmbeddedRetroArch {
    /** « Paquet » du RetroArch intégré dans le catalogue : RetroSalon lui-même. */
    const val SELF = "@retrosalon"
    const val ACTIVITY = "com.retroarch.browser.retroactivity.RetroActivityFuture"
    const val VERSION = "1.22.2"

    private const val TAG = "RetroSalon"
    private const val SUFFIX = "_libretro_android.so"
    private val ASSET_DIRS = listOf("assets", "info", "autoconfig", "filters")

    private val lock = Any()

    @Volatile
    private var preparedStamp: String? = null

    private fun libDir(context: Context) = File(context.applicationInfo.nativeLibraryDir)
    private fun dataDir(context: Context) = File(context.applicationInfo.dataDir)
    private fun coresDir(context: Context) = File(dataDir(context), "retroarch-cores")

    /** Les cœurs livrés dans cet APK (ceux de l'architecture de la TV). */
    fun cores(context: Context): List<String> =
        libDir(context).list().orEmpty()
            .filter { it.startsWith("lib") && it.endsWith(SUFFIX) }
            .map { it.removePrefix("lib").removeSuffix(SUFFIX) }
            .sorted()

    fun hasCore(context: Context, core: String) = File(libDir(context), "lib$core$SUFFIX").isFile

    fun corePath(context: Context, core: String) = File(coresDir(context), "$core$SUFFIX").absolutePath

    fun configPath(context: Context): String = UserPreferences.getDefaultConfigPath(context)

    /** Dossier des BIOS : le même que celui de l'appli RetroArch officielle. */
    fun systemDir(): File = File(Environment.getExternalStorageDirectory(), "RetroArch/system")

    /**
     * Prépare RetroArch avant un lancement : fichiers, cœurs et réglages.
     * Lit et écrit le disque : à appeler hors du fil principal. Presque instantané une fois fait.
     */
    fun prepare(context: Context) {
        synchronized(lock) {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            // lastUpdateTime change aussi quand on réinstalle la même version.
            val stamp = "$code/${info.lastUpdateTime}"
            if (preparedStamp != stamp) {
                extractAssets(context, stamp)
                writeConfig(context)
                preparedStamp = stamp
            }
            // À chaque fois : peu coûteux, et rien ne doit manquer au lancement
            // (l'accès au stockage peut aussi avoir été accordé depuis).
            linkCores(context)
            installSystemFiles(context)
            reservePads(context)
        }
    }

    /**
     * Les manettes branchées deviennent les joueurs 1, 2… de RetroArch (sa « réservation d'appareil »,
     * en mode « préféré »).
     *
     * Sans cela, RetroArch donne le joueur 1 au premier appareil qui envoie une touche : si c'est la
     * télécommande (Retour pour ouvrir le menu…), la manette devient le joueur 2 et ne contrôle plus le
     * jeu. Les joueurs dont la réservation a été changée dans RetroArch ne sont plus touchés.
     */
    private fun reservePads(context: Context) {
        val path = configPath(context)
        val config = ConfigFile(path)
        val marker = File(dataDir(context), "retroarch-pads.txt")
        val previous = marker.takeIf { it.isFile }?.readLines().orEmpty()
        val current = (1..MAX_PLAYERS).map { player ->
            if (config.getString("input_player${player}_device_reservation_type") in setOf(null, "", "0")) ""
            else config.getString("input_player${player}_reserved_device").orEmpty()
        }
        val plan = planReservations(current, previous, connectedPads())
        if (plan.all { (index, device) -> current[index] == device }) return
        for ((index, device) in plan) {
            val player = index + 1
            config.setString("input_player${player}_device_reservation_type", if (device.isEmpty()) "0" else "1")
            config.setString("input_player${player}_reserved_device", device)
        }
        config.write(path)
        marker.writeText((0 until MAX_PLAYERS).joinToString("\n") { plan[it] ?: previous.getOrElse(it) { "" } })
    }

    /** Manettes branchées, dans l'ordre de connexion, celles à sticks d'abord (une télécommande n'en a pas). */
    private fun connectedPads(): List<String> =
        InputDevice.getDeviceIds().sorted()
            .mapNotNull { InputDevice.getDevice(it) }
            .filter { !it.isVirtual && (it.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD }
            .sortedBy { (it.sources and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK }
            .mapNotNull { reservedDevice(it.vendorId, it.productId, it.name) }

    /** Une manette telle que RetroArch la réserve (« vvvv:pppp nom »), ou null sans identifiants USB. */
    internal fun reservedDevice(vendorId: Int, productId: Int, name: String): String? =
        if (vendorId == 0 && productId == 0) null else String.format(Locale.ROOT, "%04x:%04x %s", vendorId, productId, name)

    /**
     * Réservation voulue pour chaque joueur (index → manette, "" pour aucune) : la n-ième manette branchée
     * pour le joueur n. Un joueur dont la réservation actuelle n'est plus celle écrite par RetroSalon la
     * dernière fois ([previous]) a été réglé dans RetroArch : il n'apparaît pas.
     */
    internal fun planReservations(current: List<String>, previous: List<String>, pads: List<String>): Map<Int, String> =
        (0 until MAX_PLAYERS)
            .filter { current.getOrElse(it) { "" } == previous.getOrElse(it) { "" } }
            .associateWith { pads.getOrElse(it) { "" } }

    /** Ajoute à l'intent de RetroArch ce que l'appli RetroArch y met elle-même (MainMenuActivity). */
    fun fillIntent(context: Context, intent: Intent) {
        intent.component = ComponentName(context.packageName, ACTIVITY)
        intent.putExtra("IME", Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
        intent.putExtra("DATADIR", context.applicationInfo.dataDir)
        intent.putExtra("APK", context.applicationInfo.sourceDir)
        intent.putExtra("SDCARD", Environment.getExternalStorageDirectory().absolutePath)
        (context.getExternalFilesDir(null)?.absolutePath)?.let { intent.putExtra("EXTERNAL", it) }
        // RetroArch se ferme dès qu'il quitte l'écran (bouton Accueil) : le prochain jeu repart d'un
        // RetroArch neuf. Option de RetroArch lui-même (RetroActivityFuture.onStop).
        intent.putExtra("QUITFOCUS", true)
    }

    /** Ouvre le menu de RetroArch, sans jeu (réglages, manettes, shaders…). */
    fun menuIntent(context: Context): Intent = Intent(Intent.ACTION_MAIN).apply {
        fillIntent(context, this)
        putExtra("LIBRETRO", coresDir(context).absolutePath + "/")
        putExtra("CONFIGFILE", configPath(context))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Ouvre RetroArch avec [intent] (un jeu, ou son menu), toujours dans un processus neuf.
     *
     * RetroArch ne sait démarrer que dans un processus neuf : resté ouvert en arrière-plan, il affichait un
     * écran noir au lancement suivant. Il se ferme de lui-même quand on appuie sur Accueil ([fillIntent]) ;
     * s'il tourne encore malgré tout (en train de se fermer…), on le ferme avant de lancer. Il écrit les
     * sauvegardes du jeu toutes les 10 secondes (réglage par défaut sur Android) : au plus quelques
     * secondes perdues.
     */
    suspend fun start(context: Context, intent: Intent) {
        withContext(Dispatchers.IO) {
            val pid = runningPid(context) ?: return@withContext
            Log.i(TAG, "RetroArch encore ouvert : fermé avant le lancement")
            Process.killProcess(pid)
            // Le lancement ne doit pas être confié au processus en train de s'arrêter.
            withTimeoutOrNull(3_000) { while (runningPid(context) == pid) delay(50) }
        }
        // Une tâche neuve : sinon Android rouvrirait l'ancien jeu.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    /**
     * À appeler au démarrage du processus de RetroArch, avant RetroArch lui-même (RetroSalonApp).
     *
     * RetroArch lit le modèle de l'appareil avec la commande « getprop ». Quand ce nom contient « Play »
     * (« Freebox Player POP »), il croit tourner sur une console XPERIA Play : toutes les manettes
     * reçoivent le profil de cette console, où le bouton A des manettes ne fait rien, et elles sont
     * toutes regroupées sur le joueur 1. Ce processus trouve d'abord notre propre « getprop »
     * (libretrosalon-getprop.so), qui donne le même nom avec « play » en minuscules.
     */
    fun setUpProcess(context: Context) {
        val target = File(libDir(context), GETPROP)
        if (!target.isFile) return
        try {
            val bin = File(dataDir(context), "retroarch-bin")
            val link = File(bin, "getprop")
            bin.mkdirs()
            // Le dossier des bibliothèques change à chaque mise à jour.
            if (runCatching { Os.readlink(link.path) }.getOrNull() != target.path) {
                link.delete()
                Os.symlink(target.path, link.path)
            }
            Os.setenv("PATH", bin.path + ":" + (System.getenv("PATH") ?: "/system/bin"), true)
        } catch (e: Exception) {
            Log.w(TAG, "getprop de RetroSalon non installé", e)
        }
    }

    private const val GETPROP = "libretrosalon-getprop.so"

    /** Processus de RetroArch (« :retroarch » dans le manifeste du module), s'il tourne encore. */
    private fun runningPid(context: Context): Int? =
        context.getSystemService(ActivityManager::class.java)?.runningAppProcesses
            ?.firstOrNull { it.processName == context.packageName + ":retroarch" }?.pid

    private fun extractAssets(context: Context, stamp: String) {
        val data = dataDir(context)
        val marker = File(data, "retroarch-assets.txt")
        if (marker.isFile && marker.readText() == stamp) return
        for (dir in ASSET_DIRS) File(data, dir).deleteRecursively()
        context.assets.open("retroarch/ra-assets.zip").use { unzip(it, data) }
        marker.writeText(stamp)
    }

    private fun linkCores(context: Context) {
        // RetroArch parcourt data/cores à chaque démarrage et plante si le dossier manque.
        File(dataDir(context), "cores").mkdirs()
        val dir = coresDir(context)
        dir.mkdirs()
        // Les liens d'une version précédente pointent vers un dossier qui n'existe plus.
        dir.listFiles()?.filter { it.name.endsWith(SUFFIX) }?.forEach { it.delete() }
        for (core in cores(context)) {
            try {
                Os.symlink(File(libDir(context), "lib$core$SUFFIX").absolutePath, corePath(context, core))
            } catch (e: Exception) {
                Log.w(TAG, "Lien du cœur $core impossible", e)
            }
        }
    }

    private fun writeConfig(context: Context) {
        // Ce que l'appli RetroArch règle à chaque démarrage : audio de la TV (et un dossier des cœurs, remplacé ici).
        UserPreferences.updateConfigFile(context)
        val path = configPath(context)
        val config = ConfigFile(path)
        config.setString("libretro_directory", coresDir(context).absolutePath + "/")
        // RetroSalon décompresse lui-même les fichiers de RetroArch (voir extractAssets).
        config.setBoolean("bundle_assets_extract_enable", false)
        // Les cœurs sont dans l'APK. Un cœur téléchargé ne serait pas gardé : linkCores refait le dossier
        // des cœurs avant chaque lancement, et le catalogue ne connaît que les cœurs de l'APK (hasCore).
        config.setBoolean("menu_show_core_updater", false)
        // Réglages pour la TV, appliqués une seule fois (RetroArch réécrit toutes ses clés dans le fichier) :
        // ensuite, ce qu'on change dans RetroArch est gardé.
        val marker = File(dataDir(context), "retroarch-defaults.txt")
        val applied = marker.takeIf { it.isFile }?.readText()?.trim()?.toIntOrNull() ?: 0
        // Chaque groupe n'est appliqué qu'une fois, même après une mise à jour de RetroSalon.
        if (applied < 1) {
            config.setString("menu_driver", "ozone") // menu pensé pour la manette (le menu tactile est le défaut sur Android)
            config.setString("quit_on_close_content", "1") // « Fermer le contenu » quitte RetroArch : retour à RetroSalon
            config.setString("input_menu_toggle_gamepad_combo", "4") // Start + Select ouvre le menu de RetroArch
            // Retour (code Android 4) ouvre le menu : RetroArch ne le fait que pour les télécommandes
            // qu'il connaît, et celle de la Freebox n'en fait pas partie.
            config.setString("input_menu_toggle_btn", "4")
        }
        if (applied < 2) {
            // Le stick gauche sert aussi de croix dans les jeux qui n'utilisent pas le stick (consoles
            // sans stick, PlayStation en manette simple…). Les jeux à stick (Nintendo 64, PSP…) le
            // gardent : RetroArch coupe ce réglage dès que le cœur lit le stick.
            for (player in 1..MAX_PLAYERS) config.setString("input_player${player}_analog_dpad_mode", "1")
            // Toutes les manettes pilotent le menu de RetroArch, pas seulement celle du joueur 1
            // (la télécommande peut avoir pris la place du joueur 1).
            config.setBoolean("all_users_control_menu", true)
        }
        config.write(path)
        marker.writeText(DEFAULTS_VERSION.toString())
    }

    /** À augmenter, avec un nouveau groupe dans writeConfig, quand on ajoute un réglage par défaut. */
    private const val DEFAULTS_VERSION = 2
    private const val MAX_PLAYERS = 4

    /** Fichiers système de PPSSPP (polices des jeux PSP), dans le dossier des BIOS s'il est accessible. */
    private fun installSystemFiles(context: Context) {
        val system = systemDir()
        if (File(system, "PPSSPP/ppge_atlas.zim").isFile) return
        try {
            if (!system.isDirectory && !system.mkdirs()) return
            context.assets.open("retroarch/ppsspp-system.zip").use { unzip(it, system) }
        } catch (e: Exception) {
            // Pas encore d'accès au stockage : réessayé au prochain lancement.
            Log.w(TAG, "Fichiers système de PPSSPP non installés", e)
        }
    }

    private fun unzip(input: InputStream, target: File) {
        val root = target.canonicalPath + File.separator
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val out = File(target, entry.name)
                if (!out.canonicalPath.startsWith(root)) continue
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                }
            }
        }
    }
}
