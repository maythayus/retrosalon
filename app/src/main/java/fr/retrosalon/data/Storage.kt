package fr.retrosalon.data

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import fr.retrosalon.R
import fr.retrosalon.UiException
import java.io.File
import java.io.InputStream

/** Une entrée de dossier, commune au stockage local et au SMB. */
data class Entry(val name: String, val isDir: Boolean, val size: Long)

/** Accès en lecture à une source (chemins relatifs à la racine de la source, séparateur "/"). */
interface SourceFs : AutoCloseable {
    fun list(rel: String): List<Entry>
    fun open(rel: String): InputStream
    fun size(rel: String): Long
    override fun close() {}

    /** Empreinte de la version d'un fichier : elle change quand le fichier est remplacé. */
    fun stamp(rel: String): String = size(rel).toString()

    /** Ouvre [rel] à partir de l'octet [offset] : reprise d'une copie interrompue. */
    fun open(rel: String, offset: Long): InputStream {
        val input = open(rel)
        try {
            var left = offset
            while (left > 0) {
                val skipped = input.skip(left)
                when {
                    skipped > 0 -> left -= skipped
                    input.read() >= 0 -> left--
                    // Plus court que la partie déjà copiée : le fichier a changé sur le serveur.
                    else -> throw UiException(R.string.err_copy_changed, rel)
                }
            }
        } catch (e: Throwable) {
            runCatching { input.close() }
            throw e
        }
        return input
    }
}

class LocalFs(private val root: String) : SourceFs {
    fun file(rel: String) = if (rel.isEmpty()) File(root) else File(root, rel)

    override fun list(rel: String): List<Entry> {
        val dir = file(rel)
        val files = dir.listFiles() ?: throw UiException(R.string.err_folder_unreadable, dir.path)
        return files.filter { !it.isHidden }.map { Entry(it.name, it.isDirectory, if (it.isDirectory) 0 else it.length()) }
    }

    override fun open(rel: String): InputStream = file(rel).inputStream()
    override fun size(rel: String): Long = file(rel).length()
}

/** Un volume de stockage de la TV (interne ou USB). */
data class Volume(val label: String, val path: String)

object Volumes {
    fun list(context: Context): List<Volume> {
        val sm = context.getSystemService(StorageManager::class.java)
        val result = mutableListOf<Volume>()
        val internal = Environment.getExternalStorageDirectory()
        result += Volume(context.getString(R.string.storage_internal), internal.absolutePath)
        File("/storage").listFiles()
            ?.filter { it.isDirectory && it.name != "self" && it.name != "emulated" }
            ?.forEach { dir ->
                val desc = runCatching { sm.getStorageVolume(dir)?.getDescription(context) }.getOrNull()
                result += Volume(desc ?: context.getString(R.string.storage_disk, dir.name), dir.absolutePath)
            }
        return result
    }
}

/** Conversion d'un chemin /storage/... en identifiant de document SAF (ExternalStorageProvider). */
object Saf {
    const val AUTHORITY = "com.android.externalstorage.documents"
    private val emulated = Regex("""^/storage/emulated/\d+(/.*)?$""")
    private val volume = Regex("""^/storage/([^/]+)(/.*)?$""")

    fun docId(path: String): String? {
        val clean = path.trimEnd('/')
        emulated.matchEntire(clean)?.let { return "primary:" + (it.groupValues[1].trimStart('/')) }
        if (clean.startsWith("/sdcard")) return "primary:" + clean.removePrefix("/sdcard").trimStart('/')
        volume.matchEntire(clean)?.let { return it.groupValues[1] + ":" + it.groupValues[2].trimStart('/') }
        return null
    }
}
