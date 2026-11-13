package fr.retrosalon

import android.app.Application
import android.os.Build
import fr.retrosalon.data.EmbeddedRetroArch
import java.io.File

/** L'appli, dans chacun de ses processus : celui de RetroSalon et celui du RetroArch intégré. */
class RetroSalonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Avant que RetroArch démarre dans ce processus.
        if (processName().endsWith(":retroarch")) EmbeddedRetroArch.setUpProcess(this)
    }

    private fun processName(): String =
        if (Build.VERSION.SDK_INT >= 28) getProcessName()
        else runCatching { File("/proc/self/cmdline").readText().substringBefore('\u0000') }.getOrDefault("")
}
