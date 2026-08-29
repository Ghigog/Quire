pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "quire-tts-probe"

// QUI-019: the probe voices chunks from the dialogue index, so it needs core:index's
// matcher and normaliser. Composite build rather than copied sources — if the service
// normalised text even slightly differently from the writer, nothing would match and it
// would look like a matcher bug.
includeBuild("../..")
