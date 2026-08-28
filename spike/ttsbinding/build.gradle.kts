plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

android {
    namespace = "quire.spike.tts"
    compileSdk = 35

    defaultConfig {
        applicationId = "quire.spike.tts"
        minSdk = 26 // rangeStart() for highlighting arrived in O
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-probe"
    }

    buildTypes { debug { isMinifyEnabled = false } }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
