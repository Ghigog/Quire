plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

android {
    defaultConfig { ndk { abiFilters += "arm64-v8a" } }

    namespace = "quire.app.ttsservice"
    compileSdk = 35

    defaultConfig {
        applicationId = "quire.app.ttsservice"
        minSdk = 26 // rangeStart() for highlighting arrived in Android O
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
    // Empty but wired (CLAUDE.md §2.3): the real TextToSpeechService — matcher, ONNX
    // synthesis, ring buffer — belongs to QUI-010 and QUI-024, not this scaffold.
    implementation("quire:tts")
    implementation("quire:index")
    implementation("quire:model")
}
