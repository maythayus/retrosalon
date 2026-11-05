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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

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
            Text("Bienvenue dans RetroSalon", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Palette.accent)
            Muted("Une seule app pour lancer les jeux de toutes vos consoles, à la manette Xbox Nano ou à la télécommande.")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Une autorisation est nécessaire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Muted(
                    "Accès aux fichiers : pour lire vos jeux sur la TV ou un disque USB, et copier ceux du serveur " +
                        "dans le cache. RetroSalon ne parcourt que les dossiers que vous ajoutez comme source.",
                )
                Muted(
                    if (systemDialog) {
                        "Android va vous demander de confirmer : choisissez « Autoriser »."
                    } else {
                        "Sur l'écran suivant, activez RetroSalon, puis revenez avec la touche Retour."
                    },
                )
            }
            ButtonRow {
                ActionButton("Autoriser l'accès", onClick = onAllow, modifier = Modifier.focusRequester(focus), primary = true)
                ActionButton("Plus tard", onClick = onLater)
            }
        }
    }
}
