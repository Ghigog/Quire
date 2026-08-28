// Root build. Android application modules (app:companion, app:ttsservice) are added by
// QUI-001 once an Android SDK is available; the core modules below are pure Kotlin/JVM
// on purpose, so attribution and matching can be tested on a desktop in seconds rather
// than on a device in minutes (docs/architecture.md §1).
plugins {
    kotlin("jvm") version "2.2.20" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

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
