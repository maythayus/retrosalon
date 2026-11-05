package fr.retrosalon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.retrosalon.R

/**
 * Premier lancement : explique l'autorisation demandée, puis ouvre la demande du système.
 * [systemDialog] : vrai avant Android 11, où Android affiche une simple boîte de dialogue
 * au lieu de l'écran « Accès à tous les fichiers ».
 */
@Composable
fun WelcomeScreen(systemDialog: Boolean, onAllow: () -> Unit, onLater: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Panel(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Palette.accent)
            Muted(stringResource(R.string.welcome_intro))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.welcome_permission_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Muted(stringResource(R.string.welcome_permission_body))
                Muted(
                    stringResource(
                        if (systemDialog) R.string.welcome_hint_system_dialog else R.string.welcome_hint_settings_screen,
                    ),
                )
            }
            ButtonRow {
                ActionButton(stringResource(R.string.welcome_allow_access), onClick = onAllow, modifier = Modifier.focusRequester(focus), primary = true)
                ActionButton(stringResource(R.string.welcome_later), onClick = onLater)
            }
        }
    }
}
