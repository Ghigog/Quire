rootProject.name = "quire-pipeline-spike"

// QUI-008: Tier 1 lives in core:attribution now. Composite build rather than a copy —
// two implementations of the same rules is how the measured one and the shipped one
// quietly stop agreeing.
includeBuild("../..")
