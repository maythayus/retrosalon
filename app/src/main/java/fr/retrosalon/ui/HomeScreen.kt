package fr.retrosalon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.retrosalon.AppModel
import fr.retrosalon.rememberGamepads
import fr.retrosalon.data.Game
import fr.retrosalon.data.Source
import fr.retrosalon.data.Systems

@Composable
fun HomeScreen(
    model: AppModel,
    storageOk: Boolean,
    onRequestStorage: () -> Unit,
    onSettings: () -> Unit,
    onAddLocal: () -> Unit,
    onAddSmb: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val games = model.games
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) { Systems.all.filter { (counts[it.id] ?: 0) > 0 } }
    val shown = remember(games, selected) { if (selected == null) games else games.filter { it.systemId == selected } }
    val sourceKind = remember(model.sources) { model.sources.associate { it.id to (it is Source.Smb) } }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val sidebar = minOf(280.dp, maxWidth * 0.32f)
    Row(Modifier.fillMaxSize()) {
        // Colonne des consoles
        Column(
            Modifier
                .width(sidebar)
                .fillMaxHeight()
                .background(Palette.panel)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("RetroSalon", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Palette.accent)
            Muted("${games.size} jeux · ${model.sources.size} source(s)")
            val pads = rememberGamepads()
            if (pads.isEmpty()) {
                Badge("Aucune manette · appairez la Xbox Nano", Palette.warn)
            } else {
                Badge("Manette : ${pads.first()}", Palette.accent)
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SystemRow("Tous les jeux", games.size, selected == null, Modifier.focusRequester(firstFocus)) { selected = null }
                }
                items(systems, key = { it.id }) { s ->
                    SystemRow(s.name, counts[s.id] ?: 0, selected == s.id) { selected = s.id }
                }
            }
            ActionButton(if (model.scanning) "Analyse en cours…" else "Actualiser", onClick = { model.rescan() }, modifier = Modifier.fillMaxWidth())
            ActionButton("Réglages", onClick = onSettings, modifier = Modifier.fillMaxWidth())
        }

        // Liste des jeux
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 32.dp, end = 32.dp, top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!storageOk) {
                Panel(Modifier.fillMaxWidth()) {
                    Text("RetroSalon a besoin de l'accès aux fichiers", style = MaterialTheme.typography.titleMedium, color = Palette.warn)
                    Muted("Sans lui, l'app ne peut ni lire les jeux de la TV et des disques USB, ni copier les jeux du serveur.")
                    ActionButton("Autoriser l'accès", onClick = onRequestStorage, primary = true)
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    selected?.let { Systems.byId(it)?.name } ?: "Tous les jeux",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Palette.text,
                )
                Muted("${shown.size} jeux")
            }
            model.scanReport?.let { Muted(it) }

            if (model.sources.isEmpty()) {
                EmptyState(onAddLocal, onAddSmb)
            } else if (games.isEmpty() && !model.scanning) {
                Panel(Modifier.fillMaxWidth()) {
                    Text("Aucun jeu trouvé", style = MaterialTheme.typography.titleMedium)
                    Muted("Rangez les jeux dans un dossier par console à la racine de la source : snes, megadrive, psx, ps2, gc, n64, gba, dreamcast, psp…")
                    ActionButton("Réglages des sources", onClick = onSettings)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(230.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 4.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(shown, key = { it.key }) { game ->
                        GameCard(game, network = sourceKind[game.sourceId] == true, showSystem = selected == null) { model.play(game) }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SystemRow(name: String, count: Int, selected: Boolean, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    Tile(
        onClick = onSelect,
        selected = selected,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onSelect() },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("$count", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun GameCard(game: Game, network: Boolean, showSystem: Boolean, onPlay: () -> Unit) {
    Tile(onClick = onPlay, modifier = Modifier.height(132.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (game.tags.isNotEmpty()) {
                    Text(game.tags, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showSystem) Text(Systems.byId(game.systemId)?.name ?: game.systemId, style = MaterialTheme.typography.labelSmall)
                Text(if (network) "Serveur" else "Local", style = MaterialTheme.typography.labelSmall)
                Text(AppModel.formatBytes(game.size), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EmptyState(onAddLocal: () -> Unit, onAddSmb: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Panel(Modifier.fillMaxWidth(0.8f)) {
            Text("Ajoutez vos jeux", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Muted(
                "Deux types de sources. Un dossier de la TV ou d'un disque USB : les jeux sont lancés sur place. " +
                    "Un serveur de la maison (NAS ou PC, partage SMB) : le jeu est copié sur la TV au lancement, puis gardé en cache.",
            )
            ButtonRow {
                ActionButton("Dossier de la TV ou USB", onClick = onAddLocal, primary = true)
                ActionButton("Serveur réseau (SMB)", onClick = onAddSmb)
            }
        }
    }
}
