// RetroArch intégré à RetroSalon (depuis 0.3.0).
// Le code Java, le moteur natif, ses fichiers et les cœurs sont récupérés par fetch.py dans vendor/
// (non versionné) : lancez « python -I retroarch/fetch.py » avant le premier build.
plugins {
    id("com.android.library")
}

val vendor = file("vendor")
check(vendor.resolve("java").isDirectory && vendor.resolve("jniLibs").isDirectory) {
    "RetroArch n'est pas encore récupéré : lancez « python -I retroarch/fetch.py »."
}

android {
    // Le code de RetroArch attend com.retroarch.BuildConfig.
    namespace = "com.retroarch"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        minSdk = 26
        // Version « hors Play Store » : pas de téléchargement de cœurs par le Play Store.
        buildConfigField("boolean", "PLAY_STORE_BUILD", "false")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Code de RetroArch repris tel quel : ses avertissements ne bloquent pas le build.
        abortOnError = false
        checkReleaseBuilds = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.java?.addStaticSourceDirectory(vendor.resolve("java").absolutePath)
        variant.sources.jniLibs?.addStaticSourceDirectory(vendor.resolve("jniLibs").absolutePath)
        variant.sources.assets?.addStaticSourceDirectory(vendor.resolve("assets").absolutePath)
    }
}
