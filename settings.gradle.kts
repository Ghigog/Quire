rootProject.name = "quire"

include(":core:model", ":core:index")

// QUI-019: builds the pre-built index the vertical slice ships.
include(":spike:indexer")
include(":spike:slice")

// QUI-008: Tier 1 heuristic attribution.
include(":core:attribution")

// QUI-025: reads an imported book so the app can write itself a note about it.
include(":core:epub")

// QUI-037: descriptor -> generated voice, and the job C descriptor writer.
include(":core:voice")

// QUI-011: automatic voice casting.
include(":core:tts")

// QUI-046: the desktop app — the whole pipeline end to end on a laptop, so a book can be
// read aloud in voices without an Android build, an install or a device.
include(":desktop")
