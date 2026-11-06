plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "fr.retrosalon"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "fr.retrosalon"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.2.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signé avec la clé de debug pour pouvoir l'installer directement (sideload) sur la TV.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // Déclare les langues de l'app (anglais par défaut, français) : l'interface suit la langue
        // du système, et Android 13+ permet aussi de choisir la langue de RetroSalon seule.
        generateLocaleConfig = true
        // Garde seulement ces langues dans l'APK, y compris pour les textes des bibliothèques :
        // pas d'écran à moitié en allemand quand l'app, elle, retombe sur l'anglais.
        localeFilters += setOf("en", "fr")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
    }
}

androidComponents {
    onVariants { variant ->
        // Les licences tierces (legal/THIRD_PARTY_NOTICES.md) sont livrées dans l'APK, sous assets/.
        variant.sources.assets?.addStaticSourceDirectory(rootProject.file("legal").absolutePath)
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.hierynomus:smbj:0.15.0")

    testImplementation("junit:junit:4.13.2")
}
