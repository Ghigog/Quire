// Shared types only: no I/O, no Android, no behaviour beyond data.

// QUI-005: characters.json is a frozen interface seam that four tickets consume, so its
// parser has to be right rather than clever. kotlinx-serialization costs roughly 900 KB
// against the 450 MB footprint (CLAUDE.md §6) and buys correct handling of escapes,
// unicode and numbers — the places a hand-rolled JSON reader goes subtly wrong.
plugins { kotlin("plugin.serialization") }

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
