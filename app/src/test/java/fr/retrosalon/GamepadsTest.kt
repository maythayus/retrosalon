package fr.retrosalon

import fr.retrosalon.Gamepads.Bonded
import fr.retrosalon.Gamepads.Match
import fr.retrosalon.Gamepads.Transport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadsTest {
    // Valeurs de android.hardware.BatteryState.
    private val unknown = 1
    private val charging = 2
    private val discharging = 3
    private val notCharging = 4
    private val full = 5

    @Test
    fun controllerWithoutBatteryShowsNothing() {
        assertNull(Gamepads.fromBatteryState(present = false, capacity = Float.NaN, status = unknown))
        // Même si des valeurs traînent, une manette sans batterie n'en affiche pas.
        assertNull(Gamepads.fromBatteryState(present = false, capacity = 0.5f, status = discharging))
    }

    @Test
    fun levelIsShownAsAPercentage() {
        assertEquals(PadBattery(45, PadPower.DISCHARGING), Gamepads.fromBatteryState(true, 0.45f, discharging))
        assertEquals(PadBattery(100, PadPower.PLUGGED), Gamepads.fromBatteryState(true, 1f, full))
        assertEquals(PadBattery(55, PadPower.CHARGING), Gamepads.fromBatteryState(true, 0.55f, charging))
        assertEquals(PadBattery(70, PadPower.PLUGGED), Gamepads.fromBatteryState(true, 0.7f, notCharging))
        assertEquals(PadBattery(30, PadPower.UNKNOWN), Gamepads.fromBatteryState(true, 0.3f, unknown))
    }

    @Test
    fun unreadableLevelIsNeverShownAsZero() {
        // Android 12-13 renvoie NaN, Android 14+ renvoie -0,01 quand le niveau est illisible.
        assertEquals(PadBattery(null, PadPower.CHARGING), Gamepads.fromBatteryState(true, Float.NaN, charging))
        assertEquals(PadBattery(null, PadPower.DISCHARGING), Gamepads.fromBatteryState(true, -0.01f, discharging))
        assertNull(Gamepads.fromBatteryState(true, Float.NaN, unknown))
        assertNull(Gamepads.fromBatteryState(true, -0.01f, unknown))
        assertNull(Gamepads.fromBatteryState(true, Float.POSITIVE_INFINITY, unknown))
    }

    @Test
    fun levelAboveFullIsCappedAt100() {
        assertEquals(PadBattery(100, PadPower.DISCHARGING), Gamepads.fromBatteryState(true, 1.2f, discharging))
    }

    @Test
    fun bluetoothBatteryLevelIsOneByteFrom0To100() {
        assertEquals(PadBattery(50, PadPower.UNKNOWN), Gamepads.fromBatteryLevel(byteArrayOf(0x32)))
        assertEquals(PadBattery(0, PadPower.UNKNOWN), Gamepads.fromBatteryLevel(byteArrayOf(0)))
        assertEquals(PadBattery(100, PadPower.UNKNOWN), Gamepads.fromBatteryLevel(byteArrayOf(100, 7)))
        assertNull(Gamepads.fromBatteryLevel(byteArrayOf(101)))
        assertNull(Gamepads.fromBatteryLevel(byteArrayOf(0xFF.toByte())))
        assertNull(Gamepads.fromBatteryLevel(byteArrayOf()))
        assertNull(Gamepads.fromBatteryLevel(null))
    }

    @Test
    fun lowBatteryOnlyWhenNotCharging() {
        assertTrue(PadBattery(20, PadPower.DISCHARGING).low)
        assertTrue(PadBattery(5, PadPower.UNKNOWN).low)
        assertFalse(PadBattery(21, PadPower.DISCHARGING).low)
        assertFalse(PadBattery(10, PadPower.CHARGING).low)
        assertFalse(PadBattery(10, PadPower.PLUGGED).low)
        assertFalse(PadBattery(null, PadPower.DISCHARGING).low)
    }

    @Test
    fun oldXboxBluetoothFirmwareLevelIsIgnored() {
        assertFalse(Gamepads.kernelBatteryTrusted(0x045E, 0x02FD))
        assertFalse(Gamepads.kernelBatteryTrusted(0x045E, 0x0B05))
        assertTrue(Gamepads.kernelBatteryTrusted(0x045E, 0x0B13))
        assertTrue(Gamepads.kernelBatteryTrusted(0x054C, 0x05C4))
    }

    @Test
    fun androidLevelWinsOverBluetooth() {
        val android = PadBattery(60, PadPower.CHARGING)
        val bt = PadBattery(55, PadPower.UNKNOWN)
        assertEquals(android, Gamepads.merge(android, bt))
        assertEquals(bt, Gamepads.merge(null, bt))
        assertEquals(android, Gamepads.merge(android, null))
        assertNull(Gamepads.merge(null, null))
    }

    @Test
    fun bluetoothLevelKeepsTheChargingStateFromAndroid() {
        // Android sait que la manette charge mais pas son niveau : on prend le niveau Bluetooth.
        assertEquals(
            PadBattery(55, PadPower.CHARGING),
            Gamepads.merge(PadBattery(null, PadPower.CHARGING), PadBattery(55, PadPower.UNKNOWN)),
        )
        assertEquals(PadBattery(null, PadPower.PLUGGED), Gamepads.merge(PadBattery(null, PadPower.PLUGGED), null))
    }

    @Test
    fun androidDescriptorOfABluetoothController() {
        // SHA-1 de ":045e:0b13:uniqueId:aa:bb:cc:dd:ee:ff", calculé à part.
        val expected = "abf02bc7310a51e27c96d162aeac1b838890aa8b"
        assertEquals(expected, Gamepads.bluetoothDescriptors(0x045E, 0x0B13, "aa:bb:cc:dd:ee:ff", sdk = 34).first())
        // BluetoothDevice.getAddress() est en majuscules, Android 9+ les met en minuscules.
        assertEquals(expected, Gamepads.bluetoothDescriptors(0x045E, 0x0B13, "AA:BB:CC:DD:EE:FF", sdk = 28).first())
        assertEquals(
            "d9aa956eeae0209a1a9f74df81cdd70129083b0c",
            Gamepads.bluetoothDescriptors(0x054C, 0x09CC, "01:23:45:67:89:AB", sdk = 34).first(),
        )
    }

    @Test
    fun android8WritesTheAddressUppercaseAndReversed() {
        // SHA-1 de ":045e:0b13:uniqueId:FF:EE:DD:CC:BB:AA", calculé à part.
        assertEquals(
            "7a89512ab9288c89fbedd171955ab21de8f1c13c",
            Gamepads.bluetoothDescriptors(0x045E, 0x0B13, "AA:BB:CC:DD:EE:FF", sdk = 27).first(),
        )
    }

    @Test
    fun descriptorWithANonceStillMatches() {
        // SHA-1 de ":045e:0b13:uniqueId:aa:bb:cc:dd:ee:ffnonce:0001", calculé à part.
        val withNonce = "c4d6429e56ea82874185d07a004918c38d9b26d4"
        assertTrue(withNonce in Gamepads.bluetoothDescriptors(0x045E, 0x0B13, "AA:BB:CC:DD:EE:FF", sdk = 35))
        val pad = padA.copy(address = "AA:BB:CC:DD:EE:FF")
        assertEquals(Match(pad, sure = true), Gamepads.matchBonded(xbox, withNonce, 0x045E, 0x0B13, listOf(pad), 35))
    }

    private val xbox = "Xbox Wireless Controller"
    private val padA = Bonded("AA:BB:CC:DD:EE:01", xbox, Transport.LE)
    private val padB = Bonded("AA:BB:CC:DD:EE:02", xbox, Transport.LE)
    private val headset = Bonded("11:22:33:44:55:66", "Casque", Transport.CLASSIC)

    private fun descriptorOf(b: Bonded, sdk: Int = 34) = Gamepads.bluetoothDescriptors(0x045E, 0x0B13, b.address, sdk).first()

    @Test
    fun controllerIsMatchedByItsAndroidDescriptor() {
        // Deux manettes identiques : le nom ne suffit pas, l'identifiant Android départage.
        assertEquals(Match(padB, sure = true), Gamepads.matchBonded(xbox, descriptorOf(padB), 0x045E, 0x0B13, listOf(headset, padA, padB), 34))
        // Android 8 : adresse à l'envers.
        assertEquals(Match(padB, sure = true), Gamepads.matchBonded(xbox, descriptorOf(padB, 27), 0x045E, 0x0B13, listOf(padA, padB), 27))
    }

    @Test
    fun controllerIsMatchedByNameWhenTheNameIsUnique() {
        assertEquals(Match(padA, sure = false), Gamepads.matchBonded(xbox, "autre-identifiant", 0x045E, 0x0B13, listOf(headset, padA), 34))
    }

    @Test
    fun twoControllersWithTheSameNameAreNeverGuessed() {
        assertNull(Gamepads.matchBonded(xbox, "autre-identifiant", 0x045E, 0x0B13, listOf(padA, padB), 34))
    }

    @Test
    fun classicBluetoothDevicesAreNotQueried() {
        val classic = padA.copy(transport = Transport.CLASSIC)
        assertNull(Gamepads.matchBonded(xbox, descriptorOf(classic), 0x045E, 0x0B13, listOf(classic), 34))
        assertNull(Gamepads.matchBonded("Casque", "x", 0, 0, listOf(headset), 34))
    }

    @Test
    fun anotherControllerWithTheSameNameIsNeverPicked() {
        // Xbox One S en Bluetooth classique (ancien firmware) à côté d'une Xbox Nano en LE : même nom.
        val oneS = Bonded("AA:BB:CC:DD:EE:09", xbox, Transport.CLASSIC)
        assertNull(Gamepads.matchBonded(xbox, "autre-identifiant", 0x045E, 0x02FD, listOf(padA, oneS), 34))
        // Reconnue par son identifiant, elle reste sans service Batterie LE : jamais la Nano à sa place.
        val oneSDescriptor = Gamepads.bluetoothDescriptors(0x045E, 0x02FD, oneS.address, 34).first()
        assertNull(Gamepads.matchBonded(xbox, oneSDescriptor, 0x045E, 0x02FD, listOf(padA, oneS), 34))
        // Une autre manette connectée porte le même nom (USB, par exemple) : pas de choix au nom.
        assertNull(Gamepads.matchBonded(xbox, "autre-identifiant", 0x045E, 0x0B13, listOf(padA), 34, nameIsUnique = false))
        // L'identifiant reste valable dans ce cas.
        assertEquals(Match(padA, sure = true), Gamepads.matchBonded(xbox, descriptorOf(padA), 0x045E, 0x0B13, listOf(padA), 34, nameIsUnique = false))
    }

    @Test
    fun dualModeDeviceOnlyByItsDescriptorAndNeverRetried() {
        val dual = padA.copy(transport = Transport.DUAL)
        val unknown = padA.copy(transport = Transport.UNKNOWN)
        assertEquals(Match(dual, sure = false), Gamepads.matchBonded(xbox, descriptorOf(dual), 0x045E, 0x0B13, listOf(dual), 34))
        assertEquals(Match(unknown, sure = false), Gamepads.matchBonded(xbox, descriptorOf(unknown), 0x045E, 0x0B13, listOf(unknown), 34))
        // Au nom seul, un appareil double pourrait être relié en Bluetooth classique.
        assertNull(Gamepads.matchBonded(xbox, "autre-identifiant", 0x045E, 0x0B13, listOf(dual), 34))
        assertNull(Gamepads.matchBonded(xbox, "autre-identifiant", 0x045E, 0x0B13, listOf(unknown), 34))
    }
}
