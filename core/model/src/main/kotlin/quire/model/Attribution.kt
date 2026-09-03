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

    /**
     * This pass declined, and a later one should try.
     *
     * Distinct from [NARRATOR] on purpose: a line nobody resolved still needs the model,
     * while a line decided to be narration does not. Both read in the narrator's voice
     * today, so folding them together looks harmless right up until QUI-009 needs to know
     * which lines to spend the SLM on.
     */
    NONE,
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
     * **These are not yet calibrated.** QUI-028 scored Tier 1 against PDNC: an explicit tag
     * claims 0.95 against a measured **89.9%** precision, and an action beat claims 0.75
     * against 61.3%. PRD §2's gates in [Thresholds] assume calibrated numbers, so QUI-009
     * must either recalibrate these or stop treating them as probabilities.
     *
     * The 68.6% this comment carried until 2026-09-02 came from a scorer that matched gold
     * quotations by text and so measured 7.7% of the corpus. The gap is real but small.
     */
    val confidence: Double,
    val tier: Tier,
    /** Why this was decided, for the transcript. Never shown to a reader. */
    val evidence: String = "",
)
