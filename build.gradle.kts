// Root build. Android application modules (app:companion, app:ttsservice) are added by
// QUI-001 once an Android SDK is available; the core modules below are pure Kotlin/JVM
// on purpose, so attribution and matching can be tested on a desktop in seconds rather
// than on a device in minutes (docs/architecture.md §1).
plugins {
    kotlin("jvm") version "2.2.20" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // Named so the standalone spike builds can substitute these modules by coordinate
    // through includeBuild — the Android probe has to run the *real* matcher, not a copy
    // of it, or the spike stops proving anything (QUI-019).
    group = "quire"

    repositories { mavenCentral() }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    dependencies { add("testImplementation", kotlin("test")) }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging { showStandardStreams = true }
    }
}
