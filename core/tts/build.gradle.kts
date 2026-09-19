// QUI-011: automatic voice casting. Pure Kotlin, no I/O beyond the cast file, no Android.
// Depends on core:model alone, which is the only module the boundary check lets a core
// module reach (QUI-001).
plugins { kotlin("plugin.serialization") }

dependencies {
    api(project(":core:model"))
}
