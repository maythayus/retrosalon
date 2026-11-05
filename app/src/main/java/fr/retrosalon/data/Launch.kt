package fr.retrosalon.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import fr.retrosalon.R
import fr.retrosalon.userMessage
import java.io.File

object Launch {

    fun isInstalled(context: Context, pkg: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(pkg, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun isInstalled(context: Context, launcher: Launcher) = launcher.components.any { isInstalled(context, it.first) }

    /** L'émulateur choisi pour la console s'il est installé, sinon le premier installé de la liste. */
    fun pick(context: Context, store: Store, system: GameSystem): Launcher? {
        val chosen = system.launchers.firstOrNull { it.id == store.launcherFor(system.id) }
        if (chosen != null && isInstalled(context, chosen)) return chosen
        return system.launchers.firstOrNull { isInstalled(context, it) }
    }

    /** Lance le jeu. Renvoie un message d'erreur, ou null si l'émulateur a démarré. */
    fun start(context: Context, launcher: Launcher, prepared: Prepared): String? {
        val candidates = launcher.components.filter { isInstalled(context, it.first) }
        if (candidates.isEmpty()) return context.getString(R.string.launch_not_installed, launcher.label)
        // MAME reçoit les dossiers entre apostrophes : une apostrophe dans un nom casserait la commande.
        val usesRomPath = launcher.extras.any { it.second == Arg.MameRomPath }
        if (usesRomPath && listOf(prepared.file.parent.orEmpty(), prepared.systemDir.absolutePath).any { '\'' in it }) {
            return context.getString(R.string.launch_apostrophe_in_folder, launcher.label)
        }
        for ((pkg, cls) in candidates) {
            val intent = buildIntent(context, launcher, pkg, cls, prepared)
            try {
                context.startActivity(intent)
                return null
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (e: SecurityException) {
                return context.getString(R.string.launch_refused, launcher.label, e.userMessage(context))
            }
        }
        return context.getString(R.string.launch_open_failed, launcher.label)
    }

    private fun buildIntent(context: Context, launcher: Launcher, pkg: String, cls: String, prepared: Prepared): Intent {
        val file = prepared.file
        val userId = Process.myUid() / 100_000
        val romUri: Uri = when (launcher.uriMode) {
            UriMode.PATH -> Uri.fromFile(file)
            UriMode.SAF -> safUri(file, prepared.systemDir) ?: providerUri(context, file)
            UriMode.PROVIDER -> providerUri(context, file)
        }
        val romString = if (launcher.uriMode == UriMode.PATH) file.absolutePath else romUri.toString()

        val intent = Intent()
        intent.component = ComponentName(pkg, cls)
        launcher.action?.let { intent.action = it }
        launcher.category?.let { intent.addCategory(it) }
        if (launcher.dataRom) intent.data = romUri
        for ((key, arg) in launcher.extras) {
            when (arg) {
                Arg.Rom -> intent.putExtra(key, romString)
                Arg.RomPath -> intent.putExtra(key, file.absolutePath)
                is Arg.Str -> intent.putExtra(key, arg.value)
                is Arg.Bool -> intent.putExtra(key, arg.value)
                is Arg.RaCore -> intent.putExtra(key, "/data/user/$userId/$pkg/cores/${arg.core}_libretro_android.so")
                Arg.RaConfig -> intent.putExtra(key, "/storage/emulated/$userId/Android/data/$pkg/files/retroarch.cfg")
                Arg.MameRomPath -> intent.putExtra(key, "-rompath '${file.parent};${prepared.systemDir.absolutePath}'")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (romUri.authority == "${context.packageName}.files") {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            intent.addFlags(flags)
            intent.clipData = ClipData.newRawUri("jeu", romUri)
            context.grantUriPermission(pkg, romUri, flags)
        }
        return intent
    }

    private fun providerUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** URI SAF « arbre du dossier de la console + document du jeu », comme ES-DE. */
    private fun safUri(file: File, systemDir: File): Uri? {
        val treeId = Saf.docId(systemDir.absolutePath) ?: return null
        val docId = Saf.docId(file.absolutePath) ?: return null
        val tree = DocumentsContract.buildTreeDocumentUri(Saf.AUTHORITY, treeId)
        return DocumentsContract.buildDocumentUriUsingTree(tree, docId)
    }
}
