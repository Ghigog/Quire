package quire.app.ttsservice.synthesis

import quire.index.MatchResult
import quire.tts.casting.Cast

/**
 * A run of the host's chunk that gets one voice.
 *
 * [offset] is where [text] begins within the chunk the host supplied — needed to rebase a
 * segment's own boundary timestamps (0-based, from [quire.tts.engine.TtsEngine.synthesize])
 * onto the coordinates a `rangeStart` callback must use.
 */
data class PlanSegment(
    val text: String,
    val offset: Int,
    val speakerId: String?,
    val voiceId: Int,
    val rate: Double,
)

/**
 * Turns one incoming chunk into the ordered segments to synthesise.
 *
 * Mirrors `spike/slice`'s `ChunkPlan` (QUI-018/QUI-020's prototype of this exact mechanic),
 * but resolves voices from the production [Cast] directly rather than a spike-only
 * `Casting` wrapper, and carries a character's rate offset so [UtteranceSynthesizer] can
 * apply it on top of the host's requested speech rate.
 */
object Segmenter {

    fun plan(chunk: String, match: MatchResult, cast: Cast): List<PlanSegment> {
        val narration = listOf(PlanSegment(chunk, 0, null, cast.narratorVoiceId, 1.0))
        if (!match.matched || match.spans.isEmpty()) return narration

        val spans = match.spans
            .filter { it.start < it.end }
            .sortedBy { it.start }
            .ifEmpty { return narration }

        val out = mutableListOf<PlanSegment>()
        var cursor = 0
        for ((i, span) in spans.withIndex()) {
            // Anything between spans — the space a host leaves on a continuation, a
            // character the index did not cover — joins the segment that follows it, so
            // every character of the chunk is spoken exactly once and in order.
            val start = if (i == 0) 0 else cursor
            val end = if (i == spans.lastIndex) chunk.length else span.end.coerceIn(start, chunk.length)
            if (end <= start) continue

            val assignment = span.speakerId?.let { cast.voices[it] }
            val voiceId = assignment?.voiceId ?: cast.narratorVoiceId
            val rate = assignment?.rate ?: 1.0
            out += PlanSegment(chunk.substring(start, end), start, span.speakerId, voiceId, rate)
            cursor = end
        }
        return out.ifEmpty { narration }
    }
}
