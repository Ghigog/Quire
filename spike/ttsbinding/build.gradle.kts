import java.time.LocalDate

plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

/**
 * A build stamp, so a device holding four probe APKs can tell which is which.
 *
 * The commit is the part that matters: version names get forgotten and dates repeat within
 * a day, but a short SHA points at exactly the tree a build came from. Falls back to
 * "nogit" rather than failing, because the build must not depend on being inside a
 * checkout.
 */
val gitSha: String = providers.exec {
    commandLine("git", "rev-parse", "--short=7", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }.orElse("").get().ifEmpty { "nogit" }

/**
 * A trailing `+` means the tree had uncommitted changes when this was built, so the SHA
 * names the commit it was *based on* rather than the code inside it. Without the marker a
 * work-in-progress build is indistinguishable from the commit it claims to be, which is
 * exactly the confusion the stamp exists to prevent.
 */
val dirty: String = providers.exec {
    commandLine("git", "status", "--porcelain")
    isIgnoreExitValue = true
}.standardOutput.asText.map { if (it.isBlank()) "" else "+" }.orElse("").get()

val buildDate: String = LocalDate.now().toString().replace("-", "")
val probeVersion = "0.3"
val buildStamp = "$probeVersion-$buildDate-$gitSha$dirty"

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
        versionCode = 3
        versionName = buildStamp

        // Surfaced in the app as well as the filename: the filename is gone the moment the
        // APK is installed, and that is exactly when you need to know which build you have.
        buildConfigField("String", "BUILD_STAMP", "\"$buildStamp\"")
    }

    buildTypes { debug { isMinifyEnabled = false } }

    buildFeatures { buildConfig = true }

    packaging { jniLibs { useLegacyPackaging = false } }

    // Produces quire-probe-0.3-20260829-5b0eec5-debug.apk rather than four files with the
    // same name in one Downloads folder.
    base { archivesName.set("quire-probe-$buildStamp") }
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

    // The dialogue index and the slice's casting and clipping logic. Both pure Kotlin,
    // both tested on the desktop — see spike/slice.
    implementation("quire:index")
    implementation("quire:slice")
}
