package quire.spike.slice

import quire.index.MatchResult
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
 * Since QUI-027 the clipping is the matcher's job — `MatchResult.spans` already covers only
 * the part of the entry this chunk contains, in the chunk's own coordinates — so all that
 * is left here is cutting the string and assigning voices.
 */
object ChunkPlan {

    fun of(chunk: String, match: MatchResult, casting: Casting): List<Segment> {
        val narration = listOf(Segment(chunk, null, casting.narrator))
        if (!match.matched || match.spans.isEmpty()) return narration

        val spans = match.spans
            .filter { it.start < it.end }
            .sortedBy { it.start }
            .ifEmpty { return narration }

        val out = mutableListOf<Segment>()
        var cursor = 0
        for ((i, span) in spans.withIndex()) {
            // Anything between spans — the space a host leaves on a continuation, a
            // character the index did not cover — joins the segment that follows it, so
            // every character of the chunk is spoken exactly once and in order.
            val start = if (i == 0) 0 else cursor
            val end = if (i == spans.lastIndex) chunk.length else span.end.coerceIn(start, chunk.length)
            if (end <= start) continue
            out += Segment(chunk.substring(start, end), span.speakerId, casting.voiceFor(span.speakerId))
            cursor = end
        }
        return out.ifEmpty { narration }
    }

    /** Convenience for logging: does this chunk change voice inside itself? */
    fun switchesVoice(segments: List<Segment>): Boolean =
        segments.map { it.voice }.distinct().size > 1

    /** Whether any dialogue is present, for the probe's log line. */
    fun hasDialogue(spans: List<VoiceSpan>): Boolean =
        spans.any { it.kind == Kind.DIALOGUE }
}
