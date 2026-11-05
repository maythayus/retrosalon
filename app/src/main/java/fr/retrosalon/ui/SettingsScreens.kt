package fr.retrosalon.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.retrosalon.AppModel
import fr.retrosalon.Gamepads
import fr.retrosalon.StorageAccess
import fr.retrosalon.rememberGamepads
import fr.retrosalon.data.Launch
import fr.retrosalon.data.Source
import fr.retrosalon.data.Systems
import fr.retrosalon.data.UriMode
import fr.retrosalon.data.Volumes
import kotlinx.coroutines.launch
import java.io.File

@Composable
private fun Screen(
    title: String,
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ActionButton("← Retour", onClick = onBack, modifier = Modifier.focusRequester(focus))
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        content()
    }
}

@Composable
fun SettingsScreen(
    model: AppModel,
    storageOk: Boolean,
    onRequestStorage: () -> Unit,
    onBack: () -> Unit,
    onAddLocal: () -> Unit,
    onAddSmb: () -> Unit,
    onEmulators: () -> Unit,
) {
    val context = LocalContext.current
    val volumes = remember { Volumes.list(context) }
    LaunchedEffect(Unit) { model.refreshCacheUsage() }

    Screen("Réglages", onBack) {
        item {
            val pads = rememberGamepads()
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle("Manette Xbox Nano")
                if (pads.isEmpty()) {
                    Badge("Aucune manette connectée", Palette.warn)
                } else {
                    pads.forEach { Badge(if (Gamepads.isXbox(it)) "Connectée : $it" else "Manette : $it", Palette.accent) }
                }
                Muted("1. Sur la TV : Paramètres → Télécommandes et accessoires → Associer un accessoire.")
                Muted("2. Allumez la manette (bouton Xbox), puis maintenez le bouton d'appairage jusqu'à ce que le logo clignote vite.")
                Muted("3. Choisissez « Xbox Wireless Controller ». RetroArch, PPSSPP, Dolphin et la plupart des émulateurs l'assignent seuls.")
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle("Accès aux fichiers")
                if (storageOk) {
                    Badge("Autorisé", Palette.accent)
                } else {
                    Badge("Non autorisé", Palette.warn)
                    Muted("Si le bouton n'ouvre rien, votre TV n'a pas cet écran de réglage. Depuis un PC relié à la TV par adb, lancez :")
                    Mono(StorageAccess.adbCommand(context), Palette.warn)
                    ActionButton("Autoriser l'accès", onClick = onRequestStorage, primary = true)
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle("Sources de jeux")
                if (model.sources.isEmpty()) Muted("Aucune source pour l'instant.")
                model.sources.forEach { s ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.titleMedium)
                            Muted(
                                when (s) {
                                    is Source.Local -> "Dossier local · ${s.path}"
                                    is Source.Smb -> "Serveur SMB · \\\\${s.host}\\${s.share}${if (s.path.isNotBlank()) "\\" + s.path else ""}"
                                },
                            )
                        }
                        ActionButton("Supprimer", onClick = { model.removeSource(s.id) })
                    }
                }
                ButtonRow {
                    ActionButton("Ajouter un dossier (TV ou USB)", onClick = onAddLocal)
                    ActionButton("Ajouter un serveur (SMB)", onClick = onAddSmb)
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle("Cache des jeux du serveur")
                Muted("Les jeux du serveur sont copiés ici avant d'être lancés. Les plus anciens sont effacés quand la limite est atteinte. Vos sauvegardes restent dans les émulateurs.")
                Mono(model.cacheRoot)
                Text(
                    "Utilisé : ${AppModel.formatBytes(model.cacheUsed)} sur ${model.cacheLimitGb} Go",
                    style = MaterialTheme.typography.titleSmall,
                )
                ButtonRow {
                    ActionButton("− 8 Go", onClick = { model.changeCacheLimit(-8) })
                    ActionButton("+ 8 Go", onClick = { model.changeCacheLimit(8) })
                    ActionButton("Vider le cache", onClick = { model.clearCache() })
                }
                if (volumes.size > 1) {
                    Muted("Placer le cache sur :")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(volumes) { v ->
                            val target = File(v.path, "RetroSalon/jeux").absolutePath
                            Tile(onClick = { model.moveCache(target) }, selected = model.cacheRoot == target) {
                                Text(v.label, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle("Émulateurs")
                Muted("Choisissez l'émulateur utilisé pour chaque console et voyez ce qu'il reste à configurer.")
                ActionButton("Choisir les émulateurs", onClick = onEmulators)
            }
        }
    }
}

private fun androidName(sdk: Int) = when (sdk) {
    26 -> "Android 8.0"
    28 -> "Android 9"
    29 -> "Android 10"
    30 -> "Android 11"
    31 -> "Android 12"
    32 -> "Android 12L"
    33 -> "Android 13"
    34 -> "Android 14"
    else -> "API $sdk"
}

@Composable
fun EmulatorsScreen(model: AppModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val counts = remember(model.games) { model.games.groupingBy { it.systemId }.eachCount() }
    val tick = model.installTick
    val systems = remember(counts) { Systems.all.sortedByDescending { counts[it.id] ?: 0 } }

    Screen("Émulateurs par console", onBack) {
        item {
            Muted(
                "Les émulateurs marqués « dossier à autoriser » doivent avoir accès au dossier de la console : " +
                    "dans leurs réglages, ajoutez le dossier indiqué (cache du serveur) et, pour vos jeux locaux, le dossier de la console sur la TV ou le disque USB.",
            )
        }
        items(systems, key = { it.id }) { system ->
            val chosen = remember(tick) { Launch.pick(context, model.store, system) }
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(system.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Muted("${counts[system.id] ?: 0} jeux")
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(system.launchers, key = { it.id }) { l ->
                        val installed = remember(tick) { Launch.isInstalled(context, l) }
                        Tile(onClick = { model.setLauncher(system.id, l.id) }, selected = chosen?.id == l.id, modifier = Modifier.width(260.dp)) {
                            Text(l.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    Build.VERSION.SDK_INT < l.minSdk -> "Demande ${androidName(l.minSdk)}"
                                    !installed -> "Pas installé"
                                    chosen?.id == l.id -> "Utilisé"
                                    else -> "Installé"
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                chosen?.let { l ->
                    when (l.uriMode) {
                        UriMode.SAF -> {
                            Badge("Dossier à autoriser dans ${l.label}", Palette.warn)
                            Mono(File(model.cacheRoot, system.id).absolutePath)
                        }
                        UriMode.PATH -> if (l.id.startsWith("ra:")) {
                            Muted("Dans RetroArch : Menu principal → Mise à jour en ligne → Télécharger des cœurs → « ${l.label.substringAfter("· ")} ».")
                        }
                        UriMode.PROVIDER -> Muted("Accès accordé automatiquement au lancement.")
                    }
                } ?: Badge("Aucun émulateur installé pour cette console", Palette.bad)
            }
        }
    }
}

@Composable
fun AddLocalScreen(model: AppModel, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val volumes = remember { Volumes.list(context) }
    var path by rememberSaveable { mutableStateOf<String?>(null) }
    val dirs = remember(path) {
        path?.let { p -> File(p).listFiles()?.filter { it.isDirectory && !it.isHidden }?.sortedBy { it.name.lowercase() } }
    }
    val recognized = remember(dirs) { dirs?.mapNotNull { Systems.byFolder(it.name)?.name } ?: emptyList() }
    val listState = rememberLazyListState()
    val chooseFocus = remember { FocusRequester() }
    // À chaque changement de dossier : retour en haut et focus sur « Choisir ce dossier ».
    LaunchedEffect(path) {
        if (path != null) {
            listState.scrollToItem(0)
            withFrameNanos { }
            runCatching { chooseFocus.requestFocus() }
        }
    }

    Screen("Ajouter un dossier", onBack, listState) {
        if (path == null) {
            item { Muted("Choisissez le stockage où se trouvent vos jeux.") }
            items(volumes) { v ->
                Tile(onClick = { path = v.path }, modifier = Modifier.fillMaxWidth()) {
                    Text(v.label, style = MaterialTheme.typography.titleMedium)
                    Text(v.path, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            val current = path!!
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Mono(current)
                    if (recognized.isNotEmpty()) {
                        Badge("Consoles reconnues : ${recognized.joinToString(", ")}", Palette.accent)
                    } else if (Systems.byFolder(File(current).name) != null) {
                        Badge("Dossier de console : ${Systems.byFolder(File(current).name)!!.name}", Palette.accent)
                    } else {
                        Muted("Choisissez le dossier qui contient vos dossiers de consoles (snes, psx, gc…).")
                    }
                    ButtonRow {
                        ActionButton("Choisir ce dossier", primary = true, modifier = Modifier.focusRequester(chooseFocus), onClick = {
                            val f = File(current)
                            model.addSource(Source.Local(label = f.name.ifBlank { "Dossier" }, path = f.absolutePath))
                            onDone()
                        })
                        ActionButton("Dossier parent", onClick = {
                            val parent = File(current).parentFile
                            path = if (parent == null || volumes.any { it.path == current }) null else parent.absolutePath
                        })
                    }
                }
            }
            if (dirs == null) {
                item { Muted("Ce dossier est illisible. Vérifiez l'accès aux fichiers dans les réglages.") }
            } else {
                items(dirs, key = { it.absolutePath }) { d ->
                    Tile(onClick = { path = d.absolutePath }, modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(d.name, style = MaterialTheme.typography.titleMedium)
                            Systems.byFolder(d.name)?.let { Text("· ${it.name}", style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSmbScreen(model: AppModel, onBack: () -> Unit, onDone: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("Serveur") }
    var host by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf("") }
    var folder by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var domain by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun current() = Source.Smb(label = label.ifBlank { host }, host = host, share = share, path = folder, user = user, password = password, domain = domain)

    Screen("Ajouter un serveur (SMB)", onBack) {
        item {
            Muted("NAS (Synology, QNAP…), PC Windows avec un dossier partagé, ou serveur Samba. Les jeux doivent être rangés dans un dossier par console.")
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field("Nom affiché", label, { label = it }, Modifier.weight(1f))
                    Field("Adresse du serveur", host, { host = it }, Modifier.weight(1f), hint = "192.168.1.20", keyboardType = KeyboardType.Uri)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field("Partage", share, { share = it }, Modifier.weight(1f), hint = "jeux")
                    Field("Sous-dossier (facultatif)", folder, { folder = it }, Modifier.weight(1f), hint = "roms")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field("Utilisateur (vide = invité)", user, { user = it }, Modifier.weight(1f))
                    Field("Mot de passe", password, { password = it }, Modifier.weight(1f), password = true)
                    Field("Domaine (facultatif)", domain, { domain = it }, Modifier.weight(1f))
                }
                result?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = if (it.startsWith("Échec")) Palette.bad else Palette.accent)
                }
                ButtonRow {
                    ActionButton(if (testing) "Test en cours…" else "Tester la connexion", onClick = {
                        if (host.isBlank() || share.isBlank()) {
                            result = "Échec : renseignez l'adresse et le partage."
                        } else if (!testing) {
                            testing = true
                            scope.launch {
                                result = model.testSmb(current())
                                testing = false
                            }
                        }
                    })
                    ActionButton("Enregistrer", primary = true, onClick = {
                        if (host.isBlank() || share.isBlank()) {
                            result = "Échec : renseignez l'adresse et le partage."
                        } else {
                            model.addSource(current())
                            onDone()
                        }
                    })
                }
                Muted("Le mot de passe est enregistré dans le stockage privé de l'app, sur cette TV uniquement.")
            }
        }
    }
}
