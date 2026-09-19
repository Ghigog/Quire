plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

android {
    defaultConfig { ndk { abiFilters += "arm64-v8a" } }

    namespace = "quire.app.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "quire.app.companion"
        minSdk = 26 // matches app:ttsservice's rangeStart() floor (Android O)
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    // Shared debug key so every builder (dev container, CI, a laptop) produces a
    // mutually installable APK — see app/debug.keystore and spike/ttsbinding's
    // build.gradle.kts for the full rationale (QUI-001 worklog, 2026-08-29).
    signingConfigs {
        getByName("debug") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes { debug { isMinifyEnabled = false } }

    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Empty but wired (CLAUDE.md §2.3): the companion app's real job — import, the SLM
    // scan, writing the index — belongs to QUI-025 and QUI-007. This ticket only owes the
    // dependency edges architecture.md §1 describes.
    implementation("quire:epub")
    implementation("quire:attribution")
    implementation("quire:index")
    implementation("quire:model")
}
