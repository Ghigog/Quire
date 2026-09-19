package quire.app.ttsservice.synthesis

import kotlin.test.Test
import kotlin.test.assertEquals
import quire.index.How
import quire.index.MatchResult
import quire.model.Kind
import quire.model.VoiceSpan
import quire.tts.casting.Cast
import quire.tts.casting.VoiceAssignment

class SegmenterTest {

    private val cast = Cast(
        bookId = "book-1",
        narratorVoiceId = 9,
        voices = mapOf(
            "sarah" to VoiceAssignment(voiceId = 1),
            "thomas" to VoiceAssignment(voiceId = 2, rate = 1.3),
        ),
    )

    @Test
    fun `unmatched chunk is all narration`() {
        val segments = Segmenter.plan("Hello there.", MatchResult.none, cast)

        assertEquals(listOf(PlanSegment("Hello there.", 0, null, 9, 1.0)), segments)
    }

    @Test
    fun `splits a chunk into voiced segments in order`() {
        val chunk = "Ann walked in. Sarah said hi. Thomas replied."
        val sarahStart = chunk.indexOf("Sarah")
        val thomasStart = chunk.indexOf("Thomas")
        val match = MatchResult(
            how = How.FORWARD,
            entries = emptyList(),
            spans = listOf(
                VoiceSpan(0, sarahStart, Kind.NARRATION, null, 1.0),
                VoiceSpan(sarahStart, thomasStart, Kind.DIALOGUE, "sarah", 0.9),
                VoiceSpan(thomasStart, chunk.length, Kind.DIALOGUE, "thomas", 0.9),
            ),
        )

        val segments = Segmenter.plan(chunk, match, cast)

        assertEquals(3, segments.size)

        assertEquals(chunk.substring(0, sarahStart), segments[0].text)
        assertEquals(0, segments[0].offset)
        assertEquals(9, segments[0].voiceId)
        assertEquals(1.0, segments[0].rate)

        assertEquals(chunk.substring(sarahStart, thomasStart), segments[1].text)
        assertEquals(sarahStart, segments[1].offset)
        assertEquals(1, segments[1].voiceId)
        assertEquals(1.0, segments[1].rate)

        assertEquals(chunk.substring(thomasStart), segments[2].text)
        assertEquals(thomasStart, segments[2].offset)
        assertEquals(2, segments[2].voiceId)
        assertEquals(1.3, segments[2].rate)

        // Every character of the chunk is spoken exactly once, in order.
        assertEquals(chunk, segments.joinToString("") { it.text })
    }

    @Test
    fun `a speaker id with no cast entry falls back to the narrator`() {
        val chunk = "Whoever said that."
        val match = MatchResult(
            how = How.FORWARD,
            entries = emptyList(),
            spans = listOf(VoiceSpan(0, chunk.length, Kind.DIALOGUE, "ghost", 0.5)),
        )

        val segments = Segmenter.plan(chunk, match, cast)

        assertEquals(1, segments.size)
        assertEquals(9, segments[0].voiceId)
        assertEquals(1.0, segments[0].rate)
    }
}
