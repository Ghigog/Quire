package quire.model

/** Narration or quoted speech. */
enum class Kind { NARRATION, DIALOGUE }

/**
 * A run of characters within a sentence that gets one voice.
 *
 * The voice unit is finer than the match unit: `"I know," said Sarah.` is a single
 * sentence — and arrives from the host as a single chunk — but needs two voices.
 * Offsets are into [IndexEntry.text].
 */
data class VoiceSpan(
    val start: Int,
    val end: Int,
    val kind: Kind,
    val speakerId: String?,
    val confidence: Double,
)

/**
 * One sentence of a book, as stored in `dialogue_index.db`.
 *
 * Sentences are the unit because that is what hosts send. NeoReader segments by terminal
 * punctuation rather than by document structure — a heading with no full stop runs into
 * the paragraph beneath it and arrives as one chunk — so an index addressable only by
 * paragraph would never line up. See docs/adr/0004-interception-viability.md.
 */
data class IndexEntry(
    /** Dense, 0-based, in reading order. The spine of v1.2. */
    val seq: Int,
    val text: String,
    val normalized: String,
    val spans: List<VoiceSpan>,
    val chapter: Int = 0,
)

/** Confidence gates from PRD §2 Phase 2. Changing these requires a ticket. */
object Thresholds {
    const val SLM_MIN = 0.65
    const val NARRATOR_FLOOR = 0.40
}

/**
 * One paragraph of an imported book, in reading order.
 *
 * The unit the importer works in: attribution needs the sentences either side of a
 * quotation, and those live in the same paragraph. Sentences come later, when the index is
 * built — they are what the reader's app sends us, not what a book is made of.
 */
data class Paragraph(
    /** `spineHref#p{index}`. */
    val locator: String,
    val text: String,
    val chapterIndex: Int,
    val index: Int,
)
