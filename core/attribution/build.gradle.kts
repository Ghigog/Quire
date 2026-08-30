// Tier 1 attribution: pure Kotlin, no I/O, no Android. Depends on core:model alone, which
// is the only module the boundary check lets a core module reach (QUI-001).
dependencies {
    api(project(":core:model"))
}
