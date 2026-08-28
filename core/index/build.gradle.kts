dependencies {
    api(project(":core:model"))
}

dependencies {
    // Test-only: the desktop JVM needs a SQLite driver to exercise the same SQL that
    // Android runs against its own bundled SQLite. Never shipped, so it costs nothing
    // against the 450 MB app footprint (CLAUDE.md §6).
    testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
}
