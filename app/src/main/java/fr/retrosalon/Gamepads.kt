package fr.retrosalon

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToInt

/** Alimentation d'une manette. */
enum class PadPower { CHARGING, PLUGGED, DISCHARGING, UNKNOWN }

/** Batterie d'une manette. [percent] vaut null quand seul l'état de charge est connu. */
data class PadBattery(val percent: Int?, val power: PadPower) {
    /** Batterie faible, manette pas en charge. */
    val low: Boolean
        get() = percent != null && percent <= LOW_BATTERY_PERCENT &&
            power != PadPower.CHARGING && power != PadPower.PLUGGED

    companion object {
        const val LOW_BATTERY_PERCENT = 20
    }
}

/**
 * Une manette connectée (la Xbox Nano apparaît en « Xbox Wireless Controller »). [battery] : null si
 * inconnue. [batteryPending] : la batterie n'a pas encore été cherchée (manette qui vient d'arriver).
 */
data class Pad(
    val name: String,
    val descriptor: String,
    val battery: PadBattery? = null,
    val batteryPending: Boolean = false,
)

/** Règles sans dépendance à Android, testées dans GamepadsTest. */
object Gamepads {
    fun isXbox(name: String) = name.contains("xbox", ignoreCase = true)

    // Valeurs de android.hardware.BatteryState (API 31), recopiées pour rester utilisables partout.
    internal const val STATUS_CHARGING = 2
    internal const val STATUS_DISCHARGING = 3
    internal const val STATUS_NOT_CHARGING = 4
    internal const val STATUS_FULL = 5

    /**
     * Manettes Xbox en Bluetooth « classique » (ancien firmware) : le niveau que le noyau croit lire
     * est un champ de bits mal interprété, il ne faut pas l'afficher.
     */
    private val XBOX_CLASSIC_BLUETOOTH = setOf(0x02FD, 0x0B05)

    fun kernelBatteryTrusted(vendorId: Int, productId: Int) =
        !(vendorId == 0x045E && productId in XBOX_CLASSIC_BLUETOOTH)

    /**
     * Batterie donnée par InputDevice.getBatteryState() (Android 12+). Null si la manette n'en
     * indique pas. Une capacité NaN (Android 12-13) ou négative (-0,01 sur Android 14+) veut dire
     * « niveau illisible ».
     */
    fun fromBatteryState(present: Boolean, capacity: Float, status: Int): PadBattery? {
        if (!present) return null
        val percent = if (capacity.isFinite() && capacity >= 0f) (capacity * 100).roundToInt().coerceIn(0, 100) else null
        val power = when (status) {
            STATUS_CHARGING -> PadPower.CHARGING
            STATUS_FULL, STATUS_NOT_CHARGING -> PadPower.PLUGGED
            STATUS_DISCHARGING -> PadPower.DISCHARGING
            else -> PadPower.UNKNOWN
        }
        return if (percent == null && power == PadPower.UNKNOWN) null else PadBattery(percent, power)
    }

    /** Caractéristique Bluetooth « Battery Level » (0x2A19) : un octet, de 0 à 100 %. L'état de charge n'y figure pas. */
    fun fromBatteryLevel(value: ByteArray?): PadBattery? {
        val level = value?.firstOrNull()?.toInt()?.and(0xFF) ?: return null
        return if (level <= 100) PadBattery(level, PadPower.UNKNOWN) else null
    }

    /**
     * Batterie à afficher : celle d'Android quand elle donne un pourcentage ; sinon le niveau lu en
     * Bluetooth, avec l'état de charge d'Android s'il est connu.
     */
    fun merge(kernel: PadBattery?, bluetooth: PadBattery?): PadBattery? = when {
        kernel?.percent != null || bluetooth == null -> kernel
        kernel == null || kernel.power == PadPower.UNKNOWN -> bluetooth
        else -> PadBattery(bluetooth.percent, kernel.power)
    }

    /**
     * Identifiant qu'Android donne à un périphérique d'entrée (InputDevice.getDescriptor(), calculé
     * par EventHub) : SHA-1 de « :vid:pid:uniqueId:… », suivi de « nonce:… » s'il y en a un.
     */
    internal fun inputDescriptor(vendorId: Int, productId: Int, uniqueId: String, nonce: Int = 0): String {
        var raw = String.format(Locale.ROOT, ":%04x:%04x:uniqueId:%s", vendorId, productId, uniqueId)
        if (nonce != 0) raw += String.format(Locale.ROOT, "nonce:%04x", nonce)
        return MessageDigest.getInstance("SHA-1").digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { String.format(Locale.ROOT, "%02x", it) }
    }

    /**
     * Identifiants Android possibles d'une manette Bluetooth d'adresse [address] (« AA:BB:… », comme
     * BluetoothDevice.getAddress()), sur la version d'Android [sdk]. L'uniqueId est l'adresse en
     * minuscules depuis Android 9 ; Android 8 l'écrit en majuscules et à l'envers. Android 15+ ajoute
     * un « nonce » quand un autre périphérique porte déjà le même identifiant (ancienne entrée de la
     * même manette encore présente).
     */
    fun bluetoothDescriptors(vendorId: Int, productId: Int, address: String, sdk: Int): List<String> {
        val uniqueId = if (sdk >= 28) {
            address.lowercase(Locale.ROOT)
        } else {
            address.uppercase(Locale.ROOT).split(':').reversed().joinToString(":")
        }
        return (0..3).map { inputDescriptor(vendorId, productId, uniqueId, it) }
    }

    /** Liaison Bluetooth d'un appareil appairé (BluetoothDevice.getType()). */
    enum class Transport { LE, DUAL, CLASSIC, UNKNOWN }

    /** Appareil Bluetooth appairé. */
    data class Bonded(val address: String, val name: String?, val transport: Transport)

    /**
     * Appareil appairé qui correspond à une manette. [sure] : trouvé par l'identifiant Android, et
     * uniquement Bluetooth LE ; RetroSalon peut alors réessayer après un échec.
     */
    data class Match(val bonded: Bonded, val sure: Boolean)

    /**
     * L'appareil Bluetooth appairé qui correspond à une manette : celui dont l'adresse donne le même
     * identifiant Android ; à défaut, le seul appareil appairé qui porte le même nom. Null si
     * l'appareil n'a que le Bluetooth classique (pas de service Batterie accessible), ou si le choix
     * est impossible : plusieurs appareils du même nom, ou [nameIsUnique] faux (une autre manette
     * connectée porte ce nom).
     */
    fun matchBonded(
        name: String,
        descriptor: String,
        vendorId: Int,
        productId: Int,
        bonded: List<Bonded>,
        sdk: Int,
        nameIsUnique: Boolean = true,
    ): Match? {
        bonded.firstOrNull { descriptor in bluetoothDescriptors(vendorId, productId, it.address, sdk) }?.let {
            return if (it.transport == Transport.CLASSIC) null else Match(it, sure = it.transport == Transport.LE)
        }
        if (!nameIsUnique) return null
        // Au nom, seulement un appareil uniquement LE : un appareil double ou inconnu pourrait être
        // relié en Bluetooth classique, et RetroSalon lancerait alors une nouvelle connexion LE.
        return bonded.filter { it.name == name }.singleOrNull()
            ?.takeIf { it.transport == Transport.LE }
            ?.let { Match(it, sure = false) }
    }
}
