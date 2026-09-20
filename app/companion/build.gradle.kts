plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.2.20"
    // ImportCheckpoint (QUI-025): the resumption format is companion-owned, so it is the
    // only thing here that needs this plugin — see core:model's build file for why the
    // shared characters.json format is hand-mapped instead.
    kotlin("plugin.serialization") version "2.2.20"
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<Test> { useJUnitPlatform() }

dependencies {
    // QUI-025: the import pipeline in `pipeline/` — scan, Tier 1/2/3 attribution, casting
    // and index assembly wired into one resumable run.
    implementation("quire:epub")
    implementation("quire:attribution")
    implementation("quire:index")
    implementation("quire:model")
    implementation("quire:voice")
    // kotlinx-serialization-json itself comes transitively from quire:model (an `api`
    // dependency there) — ImportCheckpoint's own @Serializable just needs the plugin above.

    // QUI-042: CloudCostEstimator and the RawSynthesizer seam a cloud backend plugs into.
    implementation("quire:tts")
    // EncryptedSharedPreferences is where a reader's cloud-voice API key is stored
    // (ADR-0010: "a key is a secret and the logs are not") — ~150 KB against the 450 MB
    // footprint, and the only dependency this ticket adds.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // JVM unit tests for `pipeline/`: plain Kotlin, no Android and no device needed
    // (CLAUDE.md §9) — everything but the Service and MainActivity glue runs here.
    testImplementation(kotlin("test"))
    testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
}
