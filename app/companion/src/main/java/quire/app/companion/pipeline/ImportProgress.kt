package quire.app.companion.pipeline

/**
 * QUI-025: which phase an import is in, and how far through it, so the app can say
 * something honest instead of showing a spinner.
 *
 * Phases take wildly different amounts of time — scanning a chapter is instant without an
 * SLM and slow with one — so [fraction] is only ever compared within the same [stage], never
 * across stages. The UI shows the stage name plus that fraction, never a single whole-import
 * percentage a smooth bar would imply.
 */
data class ImportProgress(val stage: ImportStage, val fraction: Double) {
    init {
        require(fraction in 0.0..1.0) { "fraction $fraction out of range" }
    }
}

enum class ImportStage {
    COPYING,
    PARSING,
    SCANNING_CAST,
    ATTRIBUTING_DIALOGUE,
    CASTING_VOICES,
    WRITING_INDEX,
    DONE,
}
