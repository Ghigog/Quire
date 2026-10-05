package quire.desktop

import quire.model.AttributionResult
import quire.model.Kind

/**
 * One span of the book and the engine speaker that reads it.
 *
 * [speakerId] is the character's manifest id, or null when nobody was identified — which is
 * not the same as "narrator" upstream ([quire.model.Tier.NONE]) even though both end up in
 * the narrator's voice here. Keeping the distinction in the output is what lets `cast`
 * report how much of a book Tier 1 actually resolved.
 *
 * [paragraph] is the paragraph this span came from. It is carried so a beat can be inserted
 * *between* paragraphs and not inside one: a segmenter splits `"...," he said.` into two
 * spans that share a speaker-tag sentence, and a pause between them chops a single spoken
 * line into three ill-fitting pieces.
 */
data class PlannedLine(
    val text: String,
    val kind: Kind,
    val speakerId: String?,
    val voiceId: Int,
    val paragraph: String,
)

/**
 * QUI-046: turns attributed segments into a rendering order.
 *
 * Pure on purpose (CLAUDE.md §9) — no engine, no disk — so the ordering and the fallback
 * rule can be tested in milliseconds while the synthesis they feed takes minutes.
 */
object RenderPlan {

    /**
     * Every segment in reading order, each pointed at a voice.
     *
     * Narration is rendered rather than skipped: the point of a read-aloud book is the whole
     * book, and a passage of pure narration is where a listener notices a gap most. A
     * dialogue span nobody resolved goes to the narrator too — ADR-0005's documented
     * fallback, and PRD §3.1's rule that a missing voice is flat while a wrong voice is
     * heard.
     */
    fun build(
        segments: List<AttributionResult>,
        voices: Map<String, Int>,
        narratorVoiceId: Int,
        dialogueOnly: Boolean = false,
    ): List<PlannedLine> = segments
        .filter { it.text.isNotBlank() }
        .filter { !dialogueOnly || it.kind == Kind.DIALOGUE }
        .map { segment ->
            val voice = segment.speakerId?.let { voices[it] } ?: narratorVoiceId
            PlannedLine(segment.text, segment.kind, segment.speakerId, voice, paragraphOf(segment.locator))
        }

    /** `href#p12#s1` is the third span of the paragraph `href#p12`. */
    private fun paragraphOf(segmentLocator: String) = segmentLocator.substringBeforeLast("#s")

    /** How much of a book's dialogue a pass actually resolved, for the `cast` report. */
    data class Coverage(val dialogue: Int, val resolved: Int) {
        val fraction: Double get() = if (dialogue == 0) 0.0 else resolved.toDouble() / dialogue
    }

    fun coverage(segments: List<AttributionResult>): Coverage {
        val dialogue = segments.count { it.kind == Kind.DIALOGUE }
        val resolved = segments.count { it.kind == Kind.DIALOGUE && it.speakerId != null }
        return Coverage(dialogue, resolved)
    }
}
