// Root build. Android application modules (app:companion, app:ttsservice) are added by
// QUI-001 once an Android SDK is available; the core modules below are pure Kotlin/JVM
// on purpose, so attribution and matching can be tested on a desktop in seconds rather
// than on a device in minutes (docs/architecture.md §1).
plugins {
    kotlin("jvm") version "2.2.20" apply false
    // Applied only by core:model, for characters.json (QUI-005).
    kotlin("plugin.serialization") version "2.2.20" apply false
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

/**
 * QUI-001 — the module graph cannot rot quietly.
 *
 * The ticket asked for "no core module depends on another core module". Taken literally
 * that fails on the graph it was written for: `core:index` depends on `core:model`, and the
 * same ticket calls `core:model` the shared data types. The rule it meant is encoded here —
 * `core:model` is the one permitted shared leaf, and nothing else may reach sideways.
 *
 * Run by `check`, so CI enforces it without anyone remembering to.
 */
val checkModuleBoundaries by tasks.registering {
    group = "verification"
    description = "Fails if a core module reaches sideways, upward, or into a spike."

    doLast {
        val violations = mutableListOf<String>()

        fun dependenciesOf(project: Project): List<String> =
            project.configurations
                .filter { it.name in setOf("api", "implementation", "compileOnly", "runtimeOnly") }
                .flatMap { configuration ->
                    configuration.dependencies.filterIsInstance<ProjectDependency>().map { it.path }
                }
                .distinct()

        for (project in subprojects) {
            val from = project.path
            if (!from.startsWith(":core:")) continue
            for (to in dependenciesOf(project)) {
                when {
                    to == ":core:model" -> Unit // the permitted shared leaf
                    from == ":core:model" ->
                        violations += "$from must depend on nothing: found $to"
                    to.startsWith(":core:") ->
                        violations += "$from reaches sideways to $to (only :core:model is shared)"
                    to.startsWith(":app") ->
                        violations += "$from depends upward on $to"
                    to.startsWith(":spike:") ->
                        violations += "$from depends on the throwaway module $to"
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "module boundaries violated:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
        logger.lifecycle("module boundaries: ${subprojects.count { it.path.startsWith(":core:") }} core modules, all clean")
    }
}

tasks.register("check") { dependsOn(checkModuleBoundaries) }
