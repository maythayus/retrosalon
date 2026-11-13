package fr.retrosalon.data

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mserref.NtStatus
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.transport.TransportException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.auth.NtlmAuthenticator
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import fr.retrosalon.R
import fr.retrosalon.UiException
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Accès en lecture à un partage réseau SMB 2/3 (NAS, PC Windows, Samba). */
class SmbFs(private val source: Source.Smb) : SourceFs {
    private val client = SMBClient(
        SmbConfig.builder()
            // Réponse à une demande (connexion, liste d'un dossier, ouverture d'un fichier) : 30 s.
            .withTimeout(30, TimeUnit.SECONDS)
            // Lecture d'un morceau de jeu (1 Mo) : jusqu'à 60 s, le temps qu'un disque en veille redémarre
            // ou qu'un Wi-Fi faible laisse passer les données.
            .withReadTimeout(60, TimeUnit.SECONDS)
            // Plus long que la lecture : sinon le socket couperait la connexion avant qu'elle abandonne.
            .withSoTimeout(90, TimeUnit.SECONDS)
            // NTLM uniquement : Kerberos (JGSS) n'existe pas sur Android.
            .withAuthenticators(NtlmAuthenticator.Factory())
            .build(),
    )
    private val connection: Connection
    private val session: Session
    private val share: DiskShare

    init {
        try {
            val (host, port) = parseHost(source.host)
            connection = client.connect(pickAddress(host, port).hostAddress, port)
            val auth = if (source.user.isBlank()) {
                AuthenticationContext.guest()
            } else {
                AuthenticationContext(source.user.trim(), source.password.toCharArray(), source.domain.trim().ifBlank { null })
            }
            session = connection.authenticate(auth)
            share = session.connectShare(source.share.trim().trim('/', '\\')) as DiskShare
        } catch (e: Throwable) {
            // Connexion à moitié établie (serveur qui ne répond plus en cours de route) : rien ne doit rester ouvert.
            abort()
            throw e
        }
    }

    /**
     * Coupe la connexion sans attendre le serveur. Après une coupure ou un délai dépassé, les adieux
     * habituels (fermer le fichier, le partage, la session) attendraient chacun 30 s une réponse qui ne
     * viendra pas : la connexion est coupée tout de suite, et ces adieux échouent aussitôt.
     */
    private fun abort() {
        runCatching { client.close() }
    }

    /** Exécute [block] ; après une coupure ou un délai dépassé, coupe aussi la connexion. */
    private inline fun <T> guard(block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            when {
                isNetworkGlitch(e) -> {
                    abort()
                    throw e
                }
                // Connexion tombée alors qu'aucune demande n'était en cours (Wi-Fi coupé entre deux fichiers) :
                // smbj ne le dit que par « DiskShare has already been closed », sans cause. C'est une coupure.
                e !is SMBApiException && !connected() -> {
                    abort()
                    throw TransportException(e)
                }
                else -> throw e
            }
        }

    private fun connected(): Boolean = runCatching { connection.isConnected && share.isConnected }.getOrDefault(false)

    private fun smbPath(rel: String): String =
        listOf(source.path.trim('/', '\\'), rel.trim('/'))
            .filter { it.isNotEmpty() }
            .joinToString("/")
            .replace('/', '\\')

    override fun list(rel: String): List<Entry> =
        guard { share.list(smbPath(rel)) }
            .filter { it.fileName != "." && it.fileName != ".." && !it.fileName.startsWith(".") }
            // Un nom de fichier ne contient jamais de séparateur ; sinon le serveur triche.
            .filter { '/' !in it.fileName && '\\' !in it.fileName }
            .map {
                val dir = (it.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                Entry(it.fileName, dir, if (dir) 0 else it.endOfFile)
            }

    /** Taille d'un fichier ; FileNotFoundException s'il n'existe pas (à distinguer d'une coupure réseau). */
    override fun size(rel: String): Long = info(rel).standardInformation.endOfFile

    /** Taille et date de modification : un fichier remplacé par un autre de même taille change d'empreinte. */
    override fun stamp(rel: String): String =
        info(rel).let { "${it.standardInformation.endOfFile}/${it.basicInformation.lastWriteTime.toEpochMillis()}" }

    private fun info(rel: String) = try {
        guard { share.getFileInformation(smbPath(rel)) }
    } catch (e: SMBApiException) {
        if (e.status in NOT_FOUND) throw FileNotFoundException(rel).apply { initCause(e) }
        throw e
    }

    override fun open(rel: String): InputStream {
        val file = guard {
            share.openFile(
                smbPath(rel),
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null,
            )
        }
        return object : FilterInputStream(file.inputStream) {
            override fun read(): Int = guard { super.read() }
            override fun read(b: ByteArray, off: Int, len: Int): Int = guard { super.read(b, off, len) }
            override fun close() {
                // Lecture seule : un fichier mal fermé ne gâche rien de ce qui a été lu (la taille est vérifiée).
                try { super.close() } finally { runCatching { guard { file.close() } } }
            }
        }
    }

    companion object {
        const val SMB_PORT = 445

        /** Statuts SMB d'un fichier ou d'un dossier absent du partage (ou dont le nom ne peut pas y exister). */
        private val NOT_FOUND = setOf(
            NtStatus.STATUS_NO_SUCH_FILE,
            NtStatus.STATUS_OBJECT_NAME_NOT_FOUND,
            NtStatus.STATUS_OBJECT_PATH_NOT_FOUND,
            NtStatus.STATUS_OBJECT_NAME_INVALID,
            NtStatus.STATUS_NOT_FOUND,
        )
        private const val PROBE_TIMEOUT_MS = 3_000

        /**
         * Coupure ou lenteur du réseau ou du serveur, qui peut passer en réessayant : délai dépassé,
         * connexion perdue. Ni une réponse du serveur (fichier absent, accès refusé), ni une erreur
         * de RetroSalon ou du stockage de la TV.
         */
        fun isNetworkGlitch(e: Throwable): Boolean =
            e !is UiException && e !is SMBApiException && causes(e).any {
                it is TimeoutException || it is SocketTimeoutException || it is SocketException || it is TransportException
            }

        /** Délai dépassé : le serveur n'a pas répondu à temps. */
        fun isTimeout(e: Throwable): Boolean =
            isNetworkGlitch(e) && causes(e).any { it is TimeoutException || it is SocketTimeoutException }

        /** Message clair quand le serveur ne répond plus ou que la connexion tombe, plutôt que l'erreur de smbj. */
        fun explain(e: Exception, label: String): Exception = when {
            isTimeout(e) -> UiException(R.string.err_server_timeout, label, cause = e)
            isNetworkGlitch(e) -> UiException(R.string.err_server_connection_lost, label, cause = e)
            else -> e
        }

        private fun causes(e: Throwable): Sequence<Throwable> =
            generateSequence(e) { it.cause?.takeIf { cause -> cause !== it } }.take(16)

        /**
         * Sépare l'hôte et le port. Formats acceptés : `nas.local`, `192.168.1.20`, `192.168.1.20:4450`,
         * `fd00::20` (IPv6 sans port), `[fd00::20]:4450`.
         */
        fun parseHost(raw: String): Pair<String, Int> {
            var h = raw.trim()
            if (h.startsWith("smb://", ignoreCase = true)) h = h.substring("smb://".length)
            // Seul le serveur compte : « smb://nas/jeux » ou « \\NAS\jeux » donnent « nas » / « NAS »
            // (le partage a son propre champ).
            h = h.trimStart('\\', '/').substringBefore('/').substringBefore('\\')
            if (h.startsWith("[")) {
                val host = h.substringAfter('[').substringBefore(']')
                val port = h.substringAfter("]:", "").toIntOrNull() ?: SMB_PORT
                return host to port
            }
            if (h.count { it == ':' } > 1) return h to SMB_PORT // IPv6 littérale sans port
            val port = h.substringAfter(':', "").toIntOrNull() ?: SMB_PORT
            return h.substringBefore(':') to port
        }

        /**
         * Résout l'hôte et teste les adresses : IPv6 d'abord, puis IPv4 si l'IPv6 ne répond pas.
         * Chaque essai est limité à 3 secondes pour ne pas bloquer l'écran.
         */
        fun pickAddress(host: String, port: Int): InetAddress {
            val all = try {
                InetAddress.getAllByName(host).toList()
            } catch (e: UnknownHostException) {
                throw UiException(R.string.err_server_not_found, host, cause = e)
            }
            val ordered = all.filterIsInstance<Inet6Address>() + all.filterIsInstance<Inet4Address>()
            val tried = mutableListOf<String>()
            for (address in ordered) {
                try {
                    Socket().use { it.connect(InetSocketAddress(address, port), PROBE_TIMEOUT_MS) }
                    return address
                } catch (_: IOException) {
                    tried += if (address is Inet6Address) "IPv6 ${address.hostAddress}" else "IPv4 ${address.hostAddress}"
                }
            }
            throw UiException(R.string.err_server_unreachable, port, tried.joinToString(", "))
        }
    }

    /**
     * Ferme la connexion, sans les adieux au serveur (fermer le partage, la session) : un serveur qui ne
     * répond plus ferait attendre 30 s chacun. Le serveur libère tout dès que la connexion se ferme.
     */
    override fun close() = abort()
}
