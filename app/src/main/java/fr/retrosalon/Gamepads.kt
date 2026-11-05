package fr.retrosalon

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Manettes de jeu branchées ou appairées (la Xbox Nano apparaît en « Xbox Wireless Controller »). */
object Gamepads {
    fun connected(): List<String> =
        InputDevice.getDeviceIds().toList()
            .mapNotNull { InputDevice.getDevice(it) }
            .filter { !it.isVirtual && (it.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD }
            .map { it.name }
            .distinct()

    fun isXbox(name: String) = name.contains("xbox", ignoreCase = true)
}

/** Liste des manettes, mise à jour quand une manette se connecte ou se déconnecte. */
@Composable
fun rememberGamepads(): List<String> {
    val context = LocalContext.current
    var pads by remember { mutableStateOf(Gamepads.connected()) }
    DisposableEffect(Unit) {
        val im = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { pads = Gamepads.connected() }
            override fun onInputDeviceRemoved(deviceId: Int) { pads = Gamepads.connected() }
            override fun onInputDeviceChanged(deviceId: Int) { pads = Gamepads.connected() }
        }
        im.registerInputDeviceListener(listener, null)
        onDispose { im.unregisterInputDeviceListener(listener) }
    }
    return pads
}
