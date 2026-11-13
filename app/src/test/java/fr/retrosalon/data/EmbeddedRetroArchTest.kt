package fr.retrosalon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmbeddedRetroArchTest {
    private val nano = "045e:0b13 Xbox Wireless Controller"
    private val other = "054c:09cc Wireless Controller"

    @Test
    fun aControllerIsWrittenAsRetroArchReadsIt() {
        assertEquals(nano, EmbeddedRetroArch.reservedDevice(0x045e, 0x0b13, "Xbox Wireless Controller"))
    }

    @Test
    fun aControllerWithoutUsbIdentifiersIsNotReserved() {
        assertNull(EmbeddedRetroArch.reservedDevice(0, 0, "Virtual"))
    }

    @Test
    fun theConnectedControllersBecomePlayersOneTwoAndSoOn() {
        val plan = EmbeddedRetroArch.planReservations(listOf("", "", "", ""), emptyList(), listOf(nano, other))
        assertEquals(mapOf(0 to nano, 1 to other, 2 to "", 3 to ""), plan)
    }

    @Test
    fun twoIdenticalControllersAreBothReserved() {
        val plan = EmbeddedRetroArch.planReservations(emptyList(), emptyList(), listOf(nano, nano))
        assertEquals(nano, plan[0])
        assertEquals(nano, plan[1])
    }

    @Test
    fun aDisconnectedControllerFreesItsPlayer() {
        val plan = EmbeddedRetroArch.planReservations(listOf(nano, other, "", ""), listOf(nano, other, "", ""), listOf(other))
        assertEquals(mapOf(0 to other, 1 to "", 2 to "", 3 to ""), plan)
    }

    @Test
    fun aPlayerSetUpInRetroArchIsLeftAlone() {
        // Le joueur 2 a été réservé à la main dans RetroArch : RetroSalon n'y touche plus.
        val plan = EmbeddedRetroArch.planReservations(listOf(nano, other, "", ""), listOf(nano, "", "", ""), listOf(nano))
        assertEquals(mapOf(0 to nano, 2 to "", 3 to ""), plan)
    }
}
