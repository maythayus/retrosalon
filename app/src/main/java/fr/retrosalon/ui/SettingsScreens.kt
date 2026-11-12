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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.retrosalon.AppModel
import fr.retrosalon.Gamepads
import fr.retrosalon.R
import fr.retrosalon.StorageAccess
import fr.retrosalon.TvArch
import fr.retrosalon.formatBytes
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
    val backLabel = stringResource(R.string.settings_back)
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ActionButton(backLabel, onClick = onBack, modifier = Modifier.focusRequester(focus))
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
    onAllowBluetooth: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val context = LocalContext.current
    val volumes = remember { Volumes.list(context) }
    LaunchedEffect(Unit) { model.refreshCacheUsage() }

    Screen(stringResource(R.string.settings_title), onBack) {
        item {
            val pads by model.gamepads.pads.collectAsState()
            val bluetoothGranted by model.gamepads.bluetoothGranted.collectAsState()
            val bluetoothDenied by model.gamepads.bluetoothDenied.collectAsState()
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_controller_title))
                if (pads.isEmpty()) {
                    Badge(stringResource(R.string.settings_controller_none), Palette.warn)
                } else {
                    pads.forEach {
                        PadBadge(
                            if (Gamepads.isXbox(it.name)) {
                                stringResource(R.string.settings_controller_connected, it.name)
                            } else {
                                stringResource(R.string.settings_controller_other, it.name)
                            },
                            it,
                        )
                    }
                }
                // Batterie des manettes Bluetooth comme la Xbox : il faut « Appareils à proximité » (Android 12+).
                // Pas avant la première lecture : une DualShock 4 qui arrive n'en a pas besoin. Même règle
                // que la lecture Bluetooth : une manette sans pourcentage.
                if (Build.VERSION.SDK_INT >= 31 && !bluetoothGranted && pads.any { it.battery?.percent == null && !it.batteryPending }) {
                    // Un seul bouton, dont le texte change après un refus : il garde le focus de la télécommande.
                    Muted(
                        stringResource(
                            if (bluetoothDenied) R.string.settings_controller_battery_denied else R.string.settings_controller_battery_help,
                        ),
                    )
                    ActionButton(
                        stringResource(
                            if (bluetoothDenied) R.string.settings_controller_battery_open else R.string.settings_controller_battery_allow,
                        ),
                        onClick = if (bluetoothDenied) onOpenAppSettings else onAllowBluetooth,
                    )
                }
                Muted(stringResource(R.string.settings_controller_step1))
                Muted(stringResource(R.string.settings_controller_step2))
                Muted(stringResource(R.string.settings_controller_step3))
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_storage_title))
                if (storageOk) {
                    Badge(stringResource(R.string.settings_storage_allowed), Palette.accent)
                } else {
                    Badge(stringResource(R.string.settings_storage_denied), Palette.warn)
                    Muted(stringResource(R.string.settings_storage_adb_help))
                    Mono(StorageAccess.adbCommand(context), Palette.warn)
                    ActionButton(stringResource(R.string.settings_storage_allow), onClick = onRequestStorage, primary = true)
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_sources_title))
                if (model.sources.isEmpty()) Muted(stringResource(R.string.settings_sources_empty))
                model.sources.forEach { s ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.titleMedium)
                            Muted(
                                when (s) {
                                    is Source.Local -> stringResource(R.string.settings_source_local, s.path)
                                    is Source.Smb -> stringResource(
                                        R.string.settings_source_smb,
                                        "\\\\${s.host}\\${s.share}${if (s.path.isNotBlank()) "\\" + s.path else ""}",
                                    )
                                },
                            )
                        }
                        ActionButton(stringResource(R.string.settings_source_remove), onClick = { model.removeSource(s.id) })
                    }
                }
                ButtonRow {
                    ActionButton(stringResource(R.string.settings_add_local), onClick = onAddLocal)
                    ActionButton(stringResource(R.string.settings_add_smb), onClick = onAddSmb)
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_cache_title))
                Muted(stringResource(R.string.settings_cache_help))
                Mono(model.cacheRoot)
                Text(
                    stringResource(R.string.settings_cache_used, formatBytes(context, model.cacheUsed), model.cacheLimitGb),
                    style = MaterialTheme.typography.titleSmall,
                )
                ButtonRow {
                    ActionButton(stringResource(R.string.settings_cache_less), onClick = { model.changeCacheLimit(-8) })
                    ActionButton(stringResource(R.string.settings_cache_more), onClick = { model.changeCacheLimit(8) })
                    ActionButton(stringResource(R.string.settings_cache_clear), onClick = { model.clearCache() })
                }
                if (volumes.size > 1) {
                    Muted(stringResource(R.string.settings_cache_location))
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
                val arch = remember { TvArch.current() }
                SectionTitle(stringResource(R.string.settings_emulators_title))
                Muted(stringResource(R.string.settings_emulators_help))
                Text(tvArchSummary(arch), style = MaterialTheme.typography.titleSmall)
                TvArchAdvice(arch)
                ActionButton(stringResource(R.string.settings_emulators_choose), onClick = onEmulators)
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

    Screen(stringResource(R.string.emulators_title), onBack) {
        item {
            Muted(stringResource(R.string.emulators_help))
        }
        items(systems, key = { it.id }) { system ->
            val chosen = remember(tick) { Launch.pick(context, model.store, system) }
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(system.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val count = counts[system.id] ?: 0
                    Muted(pluralStringResource(R.plurals.emulators_game_count, count, count))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(system.launchers, key = { it.id }) { l ->
                        val installed = remember(tick) { Launch.isInstalled(context, l) }
                        Tile(onClick = { model.setLauncher(system.id, l.id) }, selected = chosen?.id == l.id, modifier = Modifier.width(260.dp)) {
                            Text(l.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    Build.VERSION.SDK_INT < l.minSdk -> stringResource(R.string.emulators_status_requires, androidName(l.minSdk))
                                    !installed -> stringResource(R.string.emulators_status_not_installed)
                                    chosen?.id == l.id -> stringResource(R.string.emulators_status_in_use)
                                    else -> stringResource(R.string.emulators_status_installed)
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                chosen?.let { l ->
                    when (l.uriMode) {
                        UriMode.SAF -> {
                            Badge(stringResource(R.string.emulators_saf_folder, l.label), Palette.warn)
                            Mono(File(model.cacheRoot, system.id).absolutePath)
                        }
                        UriMode.PATH -> if (l.id.startsWith("ra:")) {
                            Muted(stringResource(R.string.emulators_retroarch_core, l.label.substringAfter("· ")))
                        }
                        UriMode.PROVIDER -> Muted(stringResource(R.string.emulators_provider_auto))
                    }
                } ?: Badge(stringResource(R.string.emulators_none_installed), Palette.bad)
            }
        }
    }
}

@Composable
fun AddLocalScreen(model: AppModel, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val defaultLocalLabel = stringResource(R.string.addlocal_default_label)
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

    Screen(stringResource(R.string.addlocal_title), onBack, listState) {
        if (path == null) {
            item { Muted(stringResource(R.string.addlocal_pick_storage)) }
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
                        Badge(stringResource(R.string.addlocal_recognized, recognized.joinToString(", ")), Palette.accent)
                    } else if (Systems.byFolder(File(current).name) != null) {
                        Badge(stringResource(R.string.addlocal_console_folder, Systems.byFolder(File(current).name)!!.name), Palette.accent)
                    } else {
                        Muted(stringResource(R.string.addlocal_pick_root_hint))
                    }
                    ButtonRow {
                        ActionButton(stringResource(R.string.addlocal_choose), primary = true, modifier = Modifier.focusRequester(chooseFocus), onClick = {
                            val f = File(current)
                            model.addSource(
                                Source.Local(
                                    label = f.name.ifBlank { defaultLocalLabel },
                                    path = f.absolutePath,
                                ),
                            )
                            onDone()
                        })
                        ActionButton(stringResource(R.string.addlocal_parent), onClick = {
                            val parent = File(current).parentFile
                            path = if (parent == null || volumes.any { it.path == current }) null else parent.absolutePath
                        })
                    }
                }
            }
            if (dirs == null) {
                item { Muted(stringResource(R.string.addlocal_unreadable)) }
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
    val defaultLabel = stringResource(R.string.addsmb_default_label)
    val missingFields = stringResource(R.string.addsmb_error_missing_fields)
    var label by rememberSaveable { mutableStateOf(defaultLabel) }
    var host by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf("") }
    var folder by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var domain by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun current() = Source.Smb(label = label.ifBlank { host }, host = host, share = share, path = folder, user = user, password = password, domain = domain)

    Screen(stringResource(R.string.addsmb_title), onBack) {
        item {
            Muted(stringResource(R.string.addsmb_help))
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field(stringResource(R.string.addsmb_field_label), label, { label = it }, Modifier.weight(1f))
                    Field(stringResource(R.string.addsmb_field_host), host, { host = it }, Modifier.weight(1f), hint = "192.168.1.20", keyboardType = KeyboardType.Uri)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field(stringResource(R.string.addsmb_field_share), share, { share = it }, Modifier.weight(1f), hint = stringResource(R.string.addsmb_field_share_hint))
                    Field(stringResource(R.string.addsmb_field_folder), folder, { folder = it }, Modifier.weight(1f), hint = "roms")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field(stringResource(R.string.addsmb_field_user), user, { user = it }, Modifier.weight(1f))
                    Field(stringResource(R.string.addsmb_field_password), password, { password = it }, Modifier.weight(1f), password = true)
                    Field(stringResource(R.string.addsmb_field_domain), domain, { domain = it }, Modifier.weight(1f))
                }
                result?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = if (failed) Palette.bad else Palette.accent)
                }
                ButtonRow {
                    ActionButton(stringResource(if (testing) R.string.addsmb_testing else R.string.addsmb_test), onClick = {
                        if (host.isBlank() || share.isBlank()) {
                            result = missingFields
                            failed = true
                        } else if (!testing) {
                            testing = true
                            scope.launch {
                                val (ok, message) = model.testSmb(current())
                                failed = !ok
                                result = message
                                testing = false
                            }
                        }
                    })
                    ActionButton(stringResource(R.string.addsmb_save), primary = true, onClick = {
                        if (host.isBlank() || share.isBlank()) {
                            result = missingFields
                            failed = true
                        } else {
                            model.addSource(current())
                            onDone()
                        }
                    })
                }
                Muted(stringResource(R.string.addsmb_password_note))
            }
        }
    }
}
