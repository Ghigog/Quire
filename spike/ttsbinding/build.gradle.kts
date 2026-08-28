plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

android {
    // arm64 only. The reference device is arm64 and the other ABIs in the AAR would
    // triple the APK for platforms we do not target.
    defaultConfig { ndk { abiFilters += "arm64-v8a" } }

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

    packaging { jniLibs { useLegacyPackaging = false } }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Fetched by tools/fetch-sherpa-aar.sh, not committed: 38 MB of native ONNX Runtime.
    implementation(files("libs/sherpa-onnx.aar"))

    // Pure-Java bzip2 and tar, to unpack the model archives on device. Spike-only.
    implementation("org.apache.commons:commons-compress:1.28.0")
}
