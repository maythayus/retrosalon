package fr.retrosalon

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * Accès « tous les fichiers » : nécessaire pour lire les jeux sur la TV ou un disque USB
 * et écrire le cache. Certaines TV n'ont pas l'écran de réglage correspondant :
 * dans ce cas l'app affiche la commande adb à lancer depuis un PC.
 */
object StorageAccess {
    fun granted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

    /** Ouvre l'écran système. Renvoie false si la TV n'en a pas. */
    fun request(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < 30) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1,
            )
            return true
        }
        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${activity.packageName}")),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
        for (intent in intents) {
            try {
                activity.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }

    fun adbCommand(context: Context) =
        "adb shell appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow"
}
