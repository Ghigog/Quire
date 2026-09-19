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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<Test> { useJUnitPlatform() }

dependencies {
    // QUI-024: the multi-voice synthesis orchestration in `synthesis/` — matcher, casting
    // and ONNX synthesis wired into one continuous utterance.
    implementation("quire:tts")
    implementation("quire:index")
    implementation("quire:model")

    // JVM unit tests for `synthesis/`: CLAUDE.md §9's "put the logic where it can be
    // tested" applies here too — everything but CallbackAdapter's few lines of glue is
    // plain Kotlin and runs on the desktop without a device or an emulator.
    testImplementation(kotlin("test"))
}
