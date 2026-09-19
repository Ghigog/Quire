pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "quire-app"

include(":companion", ":ttsservice")

// QUI-001: a separate build tree, on purpose. If app:* were subprojects of the root
// build, `./gradlew test` would need the Android SDK to even configure — exactly the
// dependency CLAUDE.md §9 keeps the JVM-only root free of. Composite build rather than
// copied sources, same as spike/ttsbinding, so the app reaches the *real* core:* modules.
includeBuild("..")
