rootProject.name = "quire"

include(":core:model", ":core:index")

// QUI-019: builds the pre-built index the vertical slice ships.
include(":spike:indexer")
include(":spike:slice")

// QUI-008: Tier 1 heuristic attribution.
include(":core:attribution")

// QUI-025: reads an imported book so the app can write itself a note about it.
include(":core:epub")
