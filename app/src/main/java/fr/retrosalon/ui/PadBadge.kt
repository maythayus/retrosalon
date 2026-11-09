package fr.retrosalon.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.retrosalon.Pad
import fr.retrosalon.PadBattery
import fr.retrosalon.PadPower
import fr.retrosalon.R

/**
 * « 80 % · en charge » : batterie d'une manette, dans la langue de l'app (le format du pourcentage
 * vient des textes, pas de la langue du système). Vide si elle est inconnue.
 */
@Composable
fun padBatteryText(battery: PadBattery?): String {
    if (battery == null) return ""
    val power = when (battery.power) {
        PadPower.CHARGING -> stringResource(R.string.pad_power_charging)
        PadPower.PLUGGED -> stringResource(R.string.pad_power_plugged)
        else -> null
    }
    val percent = battery.percent?.let { stringResource(R.string.pad_percent, it.toString()) }
    return listOfNotNull(percent, power).joinToString(" · ")
}

/** Badge d'une manette : [label] (son nom), puis sa batterie ; orange quand la batterie est faible. */
@Composable
fun PadBadge(label: String, pad: Pad) {
    val battery = padBatteryText(pad.battery)
    Badge(
        if (battery.isEmpty()) label else "$label · $battery",
        if (pad.battery?.low == true) Palette.warn else Palette.accent,
    )
}
