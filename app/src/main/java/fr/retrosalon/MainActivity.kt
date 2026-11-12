package fr.retrosalon

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.retrosalon.ui.AddLocalScreen
import fr.retrosalon.ui.AddSmbScreen
import fr.retrosalon.ui.EmulatorsScreen
import fr.retrosalon.ui.HomeScreen
import fr.retrosalon.ui.LaunchOverlay
import fr.retrosalon.ui.RetroSalonTheme
import fr.retrosalon.ui.SettingsScreen
import fr.retrosalon.ui.TvInfoScreen
import fr.retrosalon.ui.WelcomeScreen

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()
    private var storageOk by mutableStateOf(false)

    /** Écran d'accueil du premier lancement, qui demande l'accès aux fichiers. */
    private var showWelcome by mutableStateOf(false)

    /** Écran « Votre TV » (Android 32 ou 64 bits), montré une fois après l'accueil, y compris après une mise à jour. */
    private var showTvInfo by mutableStateOf(false)

    private val bluetoothRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.gamepads.permissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageOk = StorageAccess.granted(this)
        showWelcome = !storageOk && !model.store.welcomeDone
        showTvInfo = !model.store.tvInfoShown

        setContent {
            RetroSalonTheme {
                var screen by rememberSaveable { mutableStateOf("home") }
                val requestStorage = {
                    if (!StorageAccess.request(this)) {
                        Toast.makeText(this, getString(R.string.main_storage_screen_unavailable), Toast.LENGTH_LONG).show()
                        screen = "settings"
                    }
                }

                val finishWelcome = {
                    model.store.welcomeDone = true
                    showWelcome = false
                }
                val finishTvInfo = {
                    model.store.tvInfoShown = true
                    showTvInfo = false
                }

                LaunchedEffect(Unit) {
                    if (model.sources.isNotEmpty()) model.rescan()
                }

                BackHandler(enabled = showWelcome || showTvInfo || screen != "home" || model.launch != null) {
                    when {
                        showWelcome -> finishWelcome()
                        showTvInfo -> finishTvInfo()
                        model.launch != null -> model.dismissLaunch()
                        screen == "home" -> Unit
                        screen == "settings" -> screen = "home"
                        else -> screen = "settings"
                    }
                }

                if (showWelcome) {
                    WelcomeScreen(
                        systemDialog = Build.VERSION.SDK_INT < 30,
                        onAllow = {
                            finishWelcome()
                            requestStorage()
                        },
                        onLater = finishWelcome,
                    )
                } else if (showTvInfo) {
                    TvInfoScreen(remember { TvArch.current() }, onDone = finishTvInfo)
                } else Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        "home" -> HomeScreen(
                            model, storageOk, requestStorage,
                            onSettings = { screen = "settings" },
                            onAddLocal = { screen = "addLocal" },
                            onAddSmb = { screen = "addSmb" },
                        )
                        "settings" -> SettingsScreen(
                            model, storageOk, requestStorage,
                            onBack = { screen = "home" },
                            onAddLocal = { screen = "addLocal" },
                            onAddSmb = { screen = "addSmb" },
                            onEmulators = { screen = "emulators" },
                            onAllowBluetooth = { bluetoothRequest.launch(model.gamepads.bluetoothPermission) },
                            onOpenAppSettings = ::openAppSettings,
                        )
                        "emulators" -> EmulatorsScreen(model, onBack = { screen = "settings" })
                        "addLocal" -> AddLocalScreen(model, onBack = { screen = "settings" }, onDone = { screen = "home" })
                        "addSmb" -> AddSmbScreen(model, onBack = { screen = "settings" }, onDone = { screen = "home" })
                    }
                    model.launch?.let { LaunchOverlay(it, onDismiss = { model.dismissLaunch() }) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        model.gamepads.start()
    }

    override fun onStop() {
        // Libère le Bluetooth de la manette pendant le jeu. Une simple recréation de l'écran (langue,
        // affichage) ne coupe rien.
        if (!isChangingConfigurations) model.gamepads.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        updateStorage()
        model.refreshInstalled()
        // L'autorisation Bluetooth a pu être accordée dans les paramètres de la TV.
        model.gamepads.permissionChanged()
    }

    /** Écran des paramètres de RetroSalon (autorisations), quand la TV le propose. */
    private fun openAppSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }.onFailure {
            Toast.makeText(this, getString(R.string.settings_controller_battery_no_screen), Toast.LENGTH_LONG).show()
        }
    }

    /** Relit l'autorisation ; si elle vient d'être accordée, relance l'analyse des sources. */
    private fun updateStorage() {
        val before = storageOk
        storageOk = StorageAccess.granted(this)
        if (storageOk) showWelcome = false
        if (!before && storageOk && model.sources.isNotEmpty()) model.rescan()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStorage()
    }
}
