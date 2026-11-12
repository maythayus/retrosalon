package fr.retrosalon.data

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Une source de jeux : un dossier de la TV (ou d'un disque USB), ou un partage réseau SMB. */
sealed class Source {
    abstract val id: String
    abstract val label: String

    data class Local(
        override val id: String = UUID.randomUUID().toString(),
        override val label: String,
        val path: String,
    ) : Source()

    data class Smb(
        override val id: String = UUID.randomUUID().toString(),
        override val label: String,
        val host: String,
        val share: String,
        val path: String = "",
        val user: String = "",
        val password: String = "",
        val domain: String = "",
    ) : Source()

    fun toJson(): JSONObject = when (this) {
        is Local -> JSONObject().put("type", "local").put("id", id).put("label", label).put("path", path)
        is Smb -> JSONObject().put("type", "smb").put("id", id).put("label", label)
            .put("host", host).put("share", share).put("path", path)
            .put("user", user).put("password", password).put("domain", domain)
    }

    companion object {
        fun fromJson(o: JSONObject): Source? = when (o.optString("type")) {
            "local" -> Local(o.getString("id"), o.getString("label"), o.getString("path"))
            "smb" -> Smb(
                o.getString("id"), o.getString("label"), o.getString("host"), o.getString("share"),
                o.optString("path"), o.optString("user"), o.optString("password"), o.optString("domain"),
            )
            else -> null
        }
    }
}

/**
 * Un jeu trouvé dans une source.
 * [folder] est le chemin du dossier de la console dans la source (ex. "ps1"),
 * [rel] le chemin du fichier à l'intérieur de ce dossier (ex. "Final Fantasy VII/FF7 (Disc 1).chd").
 */
data class Game(
    val sourceId: String,
    val systemId: String,
    val folder: String,
    val rel: String,
    val size: Long,
) {
    val key: String get() = "$sourceId|$folder|$rel"
    val fileName: String get() = rel.substringAfterLast('/')
    val title: String get() = cleanTitle(fileName.substringBeforeLast('.'))
    val tags: String get() = Regex("""[(\[]([^)\]]+)[)\]]""").findAll(fileName.substringBeforeLast('.'))
        .map { it.groupValues[1] }.joinToString(" · ")

    fun toJson(): JSONObject = JSONObject().put("s", sourceId).put("y", systemId)
        .put("f", folder).put("r", rel).put("z", size)

    companion object {
        fun fromJson(o: JSONObject) = Game(o.getString("s"), o.getString("y"), o.getString("f"), o.getString("r"), o.optLong("z"))

        fun cleanTitle(name: String): String {
            val cut = name.indexOfFirst { it == '(' || it == '[' }
            return (if (cut > 0) name.substring(0, cut) else name).replace('_', ' ').trim()
        }
    }
}

/** Réglages et bibliothèque, enregistrés dans le stockage privé de l'app. */
class Store(private val context: Context) {
    private val prefs = context.getSharedPreferences("retrosalon", Context.MODE_PRIVATE)
    private val libraryFile = File(context.filesDir, "bibliotheque.json")

    var sources: List<Source>
        get() = runCatching {
            val a = JSONArray(prefs.getString("sources", "[]"))
            (0 until a.length()).mapNotNull { Source.fromJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())
        set(value) = prefs.edit().putString("sources", JSONArray(value.map { it.toJson() }).toString()).apply()

    /** Dossier racine du cache des jeux copiés depuis le réseau. */
    var cacheRoot: String
        get() = prefs.getString("cacheRoot", null)
            ?: File(Environment.getExternalStorageDirectory(), "RetroSalon/jeux").absolutePath
        set(value) = prefs.edit().putString("cacheRoot", value).apply()

    /** Taille maximale du cache, en Go. */
    var cacheLimitGb: Int
        get() = prefs.getInt("cacheLimitGb", 32)
        set(value) = prefs.edit().putInt("cacheLimitGb", value.coerceIn(2, 2000)).apply()

    /** Vrai une fois l'écran d'accueil (demande d'autorisation) passé au premier lancement. */
    var welcomeDone: Boolean
        get() = prefs.getBoolean("welcomeDone", false)
        set(value) = prefs.edit().putBoolean("welcomeDone", value).apply()

    /** Vrai une fois l'écran « Votre TV » (Android 32 ou 64 bits) vu, au premier lancement ou après une mise à jour. */
    var tvInfoShown: Boolean
        get() = prefs.getBoolean("tvInfoShown", false)
        set(value) = prefs.edit().putBoolean("tvInfoShown", value).apply()

    fun launcherFor(systemId: String): String? = prefs.getString("emu:$systemId", null)
    fun setLauncherFor(systemId: String, launcherId: String) =
        prefs.edit().putString("emu:$systemId", launcherId).apply()

    fun loadLibrary(): List<Game> = runCatching {
        val a = JSONArray(libraryFile.readText())
        (0 until a.length()).map { Game.fromJson(a.getJSONObject(it)) }.distinctBy { it.key }
    }.getOrDefault(emptyList())

    fun saveLibrary(games: List<Game>) {
        val tmp = File(libraryFile.path + ".tmp")
        tmp.writeText(JSONArray(games.map { it.toJson() }).toString())
        tmp.renameTo(libraryFile)
    }
}
