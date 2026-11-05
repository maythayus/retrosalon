package fr.retrosalon

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.StringRes
import java.io.IOException

/**
 * Erreur destinée à l'utilisateur. Son texte est une ressource traduite (langue du système :
 * français ou anglais) et n'est résolu qu'au moment de l'affichage, avec [userMessage].
 * Hérite d'IOException pour rester compatible avec les `catch (e: IOException)` existants.
 */
class UiException(
    @StringRes val res: Int,
    vararg val args: Any,
    cause: Throwable? = null,
) : IOException(cause) {
    override val message: String
        get() = "UiException(res=$res, args=${args.toList()})"
}

/** Message lisible par l'utilisateur, dans la langue de l'appareil. */
fun Throwable.userMessage(context: Context): String =
    if (this is UiException) context.getString(res, *args) else localizedMessage ?: javaClass.simpleName

/** Taille lisible selon la langue du système (« 1,2 Go » en français, « 1.2 GB » en anglais). */
fun formatBytes(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)
