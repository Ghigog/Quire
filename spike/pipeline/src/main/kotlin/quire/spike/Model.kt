package quire.spike

/**
 * What is left of the spike's own types after QUI-008 moved the rest into `core:model`
 * and `core:attribution`. The aliases keep the spike's call sites reading naturally while
 * pointing at the real types.
 */

typealias Kind = quire.model.Kind
typealias Tier = quire.model.Tier
typealias AttributionResult = quire.model.AttributionResult
typealias Segment = quire.attribution.Segment

typealias ParagraphUnit = quire.model.Paragraph

/** Confidence gates from PRD §3.1. Changing these requires a ticket. */
object Thresholds {
    const val SLM_MIN = 0.65
    const val NARRATOR_FLOOR = 0.40

    /** Tier 1 confidences, from QUI-008. */
    const val EXPLICIT_TAG = 0.95
    const val ACTION_BEAT = 0.75
}
