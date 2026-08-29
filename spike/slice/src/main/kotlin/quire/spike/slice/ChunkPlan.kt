package quire.spike.slice

import quire.index.MatchResult
import quire.index.Normalizer
import quire.model.Kind
import quire.model.VoiceSpan

/** A run of the host's chunk that gets one voice. */
data class Segment(val text: String, val speakerId: String?, val voice: Int)

/**
 * Turns one incoming chunk into the ordered segments to synthesise.
 *
 * This is the whole product in one function: the host hands over a clause with no evidence
 * of who is speaking, and the index says. Nothing here inspects quote marks.
 *
 * The work is clipping. `MatchResult.spans` covers the *entries* a chunk touched, not the
 * part of them the chunk actually contains, and most chunks stop inside a sentence. So a
 * chunk carrying only the tail of `"...the letter," she said.` arrives with both a
 * dialogue span and a narration span, and voicing it by the first span would read `she
 * said.` in the character's voice. [OffsetMap] locates the chunk inside the matched text
 * so the spans can be cut to it.
 */
object ChunkPlan {

    fun of(chunk: String, match: MatchResult, casting: Casting): List<Segment> {
        val narration = listOf(Segment(chunk, null, casting.narrator))
        if (!match.matched || match.entries.isEmpty()) return narration

        // The same concatenation the matcher rebased spans onto: entries joined by the
        // space a host puts between sentences.
        val joined = match.entries.joinToString(" ") { it.text }
        val map = OffsetMap(joined)
        val wanted = Normalizer.normalize(chunk)
        if (wanted.isEmpty()) return narration

        val at = map.normalized.indexOf(wanted)
        if (at < 0) return narration // matcher and normaliser disagree; read it plainly
        val from = map.rawAt(at)
        val to = map.rawAt(at + wanted.length)

        val cut = clip(match.spans, from, to)
        if (cut.isEmpty()) return narration

        // Offsets are into `joined`; the host's chunk may differ from that slice in
        // punctuation and spacing, so segment the host's own string proportionally to the
        // clipped spans rather than substringing `joined` and speaking that.
        return proportional(chunk, cut, from, to, casting)
    }

    /** The spans overlapping the raw range [from, to), clipped to it. */
    private fun clip(spans: List<VoiceSpan>, from: Int, to: Int): List<VoiceSpan> =
        spans.mapNotNull { span ->
            val start = maxOf(span.start, from)
            val end = minOf(span.end, to)
            if (start < end) span.copy(start = start, end = end) else null
        }

    /**
     * Cut the host's chunk at the same proportions as the clipped spans.
     *
     * The chunk and the indexed text say the same words but not the same characters — the
     * host strips quote marks, adds a leading space, and normalises punctuation its own
     * way. Proportional cutting keeps the voice changes in the right places without
     * needing the two strings to be identical, and the error is bounded by the difference
     * in their lengths, which is a few characters.
     */
    private fun proportional(
        chunk: String,
        spans: List<VoiceSpan>,
        from: Int,
        to: Int,
        casting: Casting,
    ): List<Segment> {
        if (spans.size == 1) {
            val only = spans.single()
            return listOf(Segment(chunk, only.speakerId, casting.voiceFor(only.speakerId)))
        }
        val width = (to - from).toDouble()
        val out = mutableListOf<Segment>()
        var cursor = 0
        for ((i, span) in spans.withIndex()) {
            val end =
                if (i == spans.lastIndex) chunk.length
                else wordBoundary(chunk, ((span.end - from) / width * chunk.length).toInt())
            if (end <= cursor) continue
            val text = chunk.substring(cursor, end)
            if (text.isNotBlank()) {
                out += Segment(text, span.speakerId, casting.voiceFor(span.speakerId))
            }
            cursor = end
        }
        return out.ifEmpty { listOf(Segment(chunk, null, casting.narrator)) }
    }

    /** Nearest space at or after [at], so a cut never lands mid-word. */
    private fun wordBoundary(text: String, at: Int): Int {
        val start = at.coerceIn(0, text.length)
        val forward = text.indexOf(' ', start)
        return if (forward < 0) text.length else forward
    }

    /** Convenience for logging: does this chunk change voice inside itself? */
    fun switchesVoice(segments: List<Segment>): Boolean =
        segments.map { it.voice }.distinct().size > 1

    /** Whether any dialogue is present, for the probe's log line. */
    fun hasDialogue(spans: List<VoiceSpan>): Boolean =
        spans.any { it.kind == Kind.DIALOGUE }
}
