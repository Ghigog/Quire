package quire.spike

/**
 * Types mirroring what `core:model` will hold (QUI-005, QUI-008). Kept deliberately
 * plain so they can be lifted into the real module rather than rewritten.
 */

/** A paragraph of the publication, addressed by locator. QUI-002's scheduling atom. */
data class ParagraphUnit(
    val locator: String,
    val text: String,
    val chapterIndex: Int,
    val index: Int,
)

/**
 * A run of text inside a paragraph that is either narration or one span of quoted
 * speech.
 *
 * The PRD treats the paragraph as the unit of work, but real prose puts narration and
 * two speakers inside one paragraph, so the unit that gets *a voice* has to be finer
 * than the unit that gets scheduled. Segments are that finer unit.
 */
data class Segment(
    val locator: String,
    val text: String,
    val kind: Kind,
    /** Narration immediately before this segment within the same paragraph. */
    val before: String,
    /** Narration immediately after this segment within the same paragraph. */
    val after: String,
)

enum class Kind { NARRATION, DIALOGUE }

/** Which stage resolved a speaker. Mirrors PRD §3.1's tiers. */
enum class Tier {
    HEURISTIC, // Tier 1: an explicit speech tag
    SLM,       // Tier 2: model inference over a context window
    SCENE,     // Tier 3: guess from active speakers
    NARRATOR,  // Tier 3 floor: below NARRATOR_FLOOR confidence
    NONE,      // nothing resolved it — Tier 1 leaving work for Tier 2
}

data class AttributionResult(
    val locator: String,
    val text: String,
    val kind: Kind,
    val speakerId: String?,
    val confidence: Double,
    val tier: Tier,
    /** Why this was decided. Spike-only: the point of QUI-018 is explainability. */
    val evidence: String,
)

/** Confidence gates from PRD §3.1. Changing these requires a ticket. */
object Thresholds {
    const val SLM_MIN = 0.65
    const val NARRATOR_FLOOR = 0.40

    /** Tier 1 confidences, from QUI-008. */
    const val EXPLICIT_TAG = 0.95
    const val ACTION_BEAT = 0.75
}
