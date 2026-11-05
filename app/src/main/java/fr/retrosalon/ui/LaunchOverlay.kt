package fr.retrosalon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.retrosalon.LaunchState

@Composable
fun LaunchOverlay(state: LaunchState, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.error != null) { runCatching { focus.requestFocus() } }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Panel(Modifier.width(620.dp)) {
            Text(state.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (state.error != null) {
                Text(state.error, style = MaterialTheme.typography.bodyLarge, color = Palette.bad)
                ActionButton("Fermer", onClick = onDismiss, modifier = Modifier.focusRequester(focus), primary = true)
            } else {
                Muted(state.step)
                ProgressBar(state.progress, Modifier.fillMaxWidth())
                ActionButton("Annuler", onClick = onDismiss, modifier = Modifier.focusRequester(focus))
            }
        }
    }
}
