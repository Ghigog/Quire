package quire.model

/** Which pass decided a speaker. Ordered cheapest first. */
enum class Tier {
    /** Regex over speech tags, pronouns and action beats. Free, and covers a third. */
    HEURISTIC,

    /** The language model, for lines the heuristics decline. */
    SLM,

    /** The scene's most active speaker, when even the model is unsure. */
    SCENE,

    /** Nobody was identified; read it in the narrator's voice. */
    NARRATOR,
}

/**
 * One segment of prose and who says it.
 *
 * The unit is a *segment* rather than a sentence: `"I know," said Sarah.` is one sentence
 * carrying two of these, because the quoted half and the tag get different voices. QUI-021's
 * index stores them as [VoiceSpan]s within a sentence entry.
 */
data class AttributionResult(
    /** Where in the book, as `spine#p3#s1`. */
    val locator: String,
    val text: String,
    val kind: Kind,
    /** A character id from the manifest, or null when nobody was identified. */
    val speakerId: String?,
    /**
     * How much to trust [speakerId], 0–1.
     *
     * **These are not yet calibrated.** QUI-028 scored Tier 1 against PDNC and found the
     * declared values are fiction: an explicit tag claims 0.95 against a measured 68.6%
     * precision. PRD §2's gates in [Thresholds] assume calibrated numbers, so QUI-009 must
     * either recalibrate these or stop treating them as probabilities.
     */
    val confidence: Double,
    val tier: Tier,
    /** Why this was decided, for the transcript. Never shown to a reader. */
    val evidence: String = "",
)
