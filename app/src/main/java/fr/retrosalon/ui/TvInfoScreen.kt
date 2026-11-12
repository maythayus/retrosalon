package fr.retrosalon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.retrosalon.R
import fr.retrosalon.TvArch

/** « Android 10 · 32 bits · processeur ARM ». */
@Composable
fun tvArchSummary(arch: TvArch): String {
    val bits = stringResource(if (arch.is64) R.string.tvinfo_bits_64 else R.string.tvinfo_bits_32)
    val cpu = when (arch.family) {
        TvArch.Family.ARM -> stringResource(R.string.tvinfo_cpu_arm)
        TvArch.Family.X86 -> stringResource(R.string.tvinfo_cpu_x86)
        TvArch.Family.OTHER -> stringResource(R.string.tvinfo_cpu_other, arch.primaryAbi.ifEmpty { "?" })
    }
    return stringResource(R.string.tvinfo_summary, arch.android.ifEmpty { "?" }, bits, cpu)
}

/** Ce que l'architecture change pour les émulateurs. */
@Composable
fun TvArchAdvice(arch: TvArch) {
    if (arch.is64) {
        // Un x86_64 sans traduction ARM 64 bits n'installe pas les émulateurs faits seulement pour ARM.
        Muted(stringResource(if (arch.arm64) R.string.tvinfo_body_64 else R.string.tvinfo_body_64_no_arm))
    } else {
        Muted(stringResource(R.string.tvinfo_body_32))
        Muted(stringResource(R.string.tvinfo_tips_32))
    }
    if (arch.family == TvArch.Family.X86) Muted(stringResource(R.string.tvinfo_x86))
}

/** Montré une fois, y compris après une mise à jour : 32 ou 64 bits, et ce que cela change. */
@Composable
fun TvInfoScreen(arch: TvArch, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        // Juste après « Plus tard » de l'accueil, Compose donne le focus au bouton dès son apparition,
        // avant que son surlignage n'écoute : le bouton a le focus mais reste éteint. Une image plus tard,
        // on le lui reprend puis le lui redonne pour qu'il s'éclaire.
        withFrameNanos { }
        focusManager.clearFocus(force = true)
        runCatching { focus.requestFocus() }
    }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Panel(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.tvinfo_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Palette.accent)
            Text(tvArchSummary(arch), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TvArchAdvice(arch)
            Muted(stringResource(R.string.tvinfo_later))
            ButtonRow {
                ActionButton(stringResource(R.string.tvinfo_ok), onClick = onDone, modifier = Modifier.focusRequester(focus), primary = true)
            }
        }
    }
}
