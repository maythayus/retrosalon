package fr.retrosalon.data

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.auth.NtlmAuthenticator
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.EnumSet
import java.util.concurrent.TimeUnit

/** Accès en lecture à un partage réseau SMB 2/3 (NAS, PC Windows, Samba). */
class SmbFs(private val source: Source.Smb) : SourceFs {
    private val client = SMBClient(
        SmbConfig.builder()
            .withTimeout(20, TimeUnit.SECONDS)
            .withSoTimeout(60, TimeUnit.SECONDS)
            // NTLM uniquement : Kerberos (JGSS) n'existe pas sur Android.
            .withAuthenticators(NtlmAuthenticator.Factory())
            .build(),
    )
    private val connection: Connection
    private val session: Session
    private val share: DiskShare

    init {
        val (host, port) = parseHost(source.host)
        connection = client.connect(pickAddress(host, port).hostAddress, port)
        val auth = if (source.user.isBlank()) {
            AuthenticationContext.guest()
        } else {
            AuthenticationContext(source.user.trim(), source.password.toCharArray(), source.domain.trim().ifBlank { null })
        }
        session = connection.authenticate(auth)
        share = session.connectShare(source.share.trim().trim('/', '\\')) as DiskShare
    }

    private fun smbPath(rel: String): String =
        listOf(source.path.trim('/', '\\'), rel.trim('/'))
            .filter { it.isNotEmpty() }
            .joinToString("/")
            .replace('/', '\\')

    override fun list(rel: String): List<Entry> =
        share.list(smbPath(rel))
            .filter { it.fileName != "." && it.fileName != ".." && !it.fileName.startsWith(".") }
            // Un nom de fichier ne contient jamais de séparateur ; sinon le serveur triche.
            .filter { '/' !in it.fileName && '\\' !in it.fileName }
            .map {
                val dir = (it.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                Entry(it.fileName, dir, if (dir) 0 else it.endOfFile)
            }

    override fun size(rel: String): Long =
        share.getFileInformation(smbPath(rel)).standardInformation.endOfFile

    override fun open(rel: String): InputStream {
        val file = share.openFile(
            smbPath(rel),
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null,
        )
        return object : FilterInputStream(file.inputStream) {
            override fun close() {
                try { super.close() } finally { file.close() }
            }
        }
    }

    companion object {
        const val SMB_PORT = 445
        private const val PROBE_TIMEOUT_MS = 3_000

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
                throw IOException("Serveur introuvable : « $host ». Essayez son adresse IP (ex. 192.168.1.20).", e)
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
            throw IOException("Serveur injoignable sur le port $port (essayé : ${tried.joinToString(", ")}).")
        }
    }

    override fun close() {
        runCatching { share.close() }
        runCatching { session.close() }
        runCatching { connection.close() }
        runCatching { client.close() }
    }
}
