package fr.retrosalon

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import fr.retrosalon.ui.WelcomeScreen

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()
    private var storageOk by mutableStateOf(false)

    /** Écran d'accueil du premier lancement, qui demande l'accès aux fichiers. */
    private var showWelcome by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageOk = StorageAccess.granted(this)
        showWelcome = !storageOk && !model.store.welcomeDone

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

                LaunchedEffect(Unit) {
                    if (model.sources.isNotEmpty()) model.rescan()
                }

                BackHandler(enabled = showWelcome || screen != "home" || model.launch != null) {
                    when {
                        showWelcome -> finishWelcome()
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

    override fun onResume() {
        super.onResume()
        updateStorage()
        model.refreshInstalled()
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
