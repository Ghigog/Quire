package quire.app.ttsservice.synthesis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.index.How
import quire.index.MatchResult
import quire.model.Kind
import quire.model.VoiceSpan
import quire.tts.casting.AssignmentSource
import quire.tts.casting.Cast
import quire.tts.casting.VoiceAssignment
import quire.tts.engine.RawAudio
import quire.tts.engine.RawSynthesizer
import quire.tts.engine.TtsEngine

private const val SAMPLE_RATE = 16_000

/** Deterministic stand-in for the ONNX engine: duration is proportional to text length. */
private class FakeRawSynthesizer(private val msPerChar: Long = 50) : RawSynthesizer {
    val requestedVoices = mutableListOf<Int>()

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        if (cancelled()) return null
        requestedVoices += voiceId
        val durationMs = text.length.coerceAtLeast(1) * msPerChar
        val samples = (durationMs * SAMPLE_RATE / 1000).toInt().coerceAtLeast(1)
        return RawAudio(FloatArray(samples) { 0.5f }, SAMPLE_RATE)
    }

    override fun release() = Unit
}

private val cast = Cast(
    bookId = "book-1",
    narratorVoiceId = 0,
    voices = mapOf(
        "sarah" to VoiceAssignment(voiceId = 1, source = AssignmentSource.AUTO),
        "thomas" to VoiceAssignment(voiceId = 2, rate = 1.2, source = AssignmentSource.AUTO),
    ),
)

class UtteranceSynthesizerTest {

    private val chunk = "Ann walked in. Sarah said hello there. Thomas said hi now."
    private val sarahStart = chunk.indexOf("Sarah")
    private val thomasStart = chunk.indexOf("Thomas")

    private val threeVoiceMatch = MatchResult(
        how = How.FORWARD,
        entries = emptyList(),
        spans = listOf(
            VoiceSpan(0, sarahStart, Kind.NARRATION, speakerId = null, confidence = 1.0),
            VoiceSpan(sarahStart, thomasStart, Kind.DIALOGUE, speakerId = "sarah", confidence = 0.9),
            VoiceSpan(thomasStart, chunk.length, Kind.DIALOGUE, speakerId = "thomas", confidence = 0.9),
        ),
    )

    @Test
    fun `three voices in one utterance`() {
        val synth = FakeRawSynthesizer()
        val events = UtteranceSynthesizer(TtsEngine(synth))
            .synthesize(chunk, threeVoiceMatch, cast, hostRate = 1.0) { false }
            .toList()

        assertEquals(1, events.count { it is AudioEvent.Started })
        assertEquals(1, events.count { it is AudioEvent.Done })
        assertTrue(events.first() is AudioEvent.Started)
        assertTrue(events.last() is AudioEvent.Done)
        // The host sees one continuous utterance built from exactly the three voices, in order.
        assertEquals(listOf(0, 1, 2), synth.requestedVoices)
    }

    @Test
    fun `the host's speech rate is honoured, with the character's offset applied on top`() {
        val synth = FakeRawSynthesizer(msPerChar = 100)
        val engine = TtsEngine(synth)

        fun audioBytes(hostRate: Double) = UtteranceSynthesizer(engine)
            .synthesize("hello world", MatchResult.none, cast, hostRate) { false }
            .filterIsInstance<AudioEvent.Audio>()
            .sumOf { it.pcm.size }

        val normal = audioBytes(1.0)
        val fast = audioBytes(1.5)

        // 1.5x speed should produce roughly 1/1.5 the audio bytes; allow for the rounding
        // that comes from resampling and slicing per word.
        val ratio = fast.toDouble() / normal
        assertTrue(ratio in 0.60..0.72, "expected ~1/1.5 audio length, got ratio $ratio")
    }

    @Test
    fun `character rate stacks on top of the host rate rather than replacing it`() {
        val synth = FakeRawSynthesizer(msPerChar = 100)
        val engine = TtsEngine(synth)
        // "thomas" carries rate 1.2 in the cast fixture above.
        val thomasOnly = MatchResult(
            how = How.FORWARD,
            entries = emptyList(),
            spans = listOf(VoiceSpan(0, 11, Kind.DIALOGUE, speakerId = "thomas", confidence = 0.9)),
        )

        fun audioBytes(hostRate: Double) = UtteranceSynthesizer(engine)
            .synthesize("hello world", thomasOnly, cast, hostRate) { false }
            .filterIsInstance<AudioEvent.Audio>()
            .sumOf { it.pcm.size }

        val hostOnly = audioBytes(1.0) // combined factor 1.2
        val hostAndCharacter = audioBytes(1.5) // combined factor 1.8

        val ratio = hostAndCharacter.toDouble() / hostOnly
        assertTrue(ratio in 0.60..0.72, "expected ~1.2/1.8 audio length, got ratio $ratio")
    }

    @Test
    fun `ranges point into the original string`() {
        val synth = FakeRawSynthesizer()
        val ranges = UtteranceSynthesizer(TtsEngine(synth))
            .synthesize(chunk, threeVoiceMatch, cast, hostRate = 1.0) { false }
            .filterIsInstance<AudioEvent.Range>()
            .toList()

        assertTrue(ranges.isNotEmpty())
        for (range in ranges) {
            assertTrue(range.charStart in 0..chunk.length)
            assertTrue(range.charEnd in range.charStart..chunk.length)
            assertTrue(chunk.substring(range.charStart, range.charEnd).isNotBlank())
        }
        // No range straddles a voice change: each one selects a word wholly inside the
        // segment it was synthesised for, never across a boundary into the next speaker's.
        assertTrue(ranges.none { it.charStart < sarahStart && it.charEnd > sarahStart })
        assertTrue(ranges.none { it.charStart < thomasStart && it.charEnd > thomasStart })
    }

    @Test
    fun `stopping is immediate`() {
        var stopped = false
        val synth = FakeRawSynthesizer()

        val events = UtteranceSynthesizer(TtsEngine(synth))
            .synthesize(chunk, threeVoiceMatch, cast, hostRate = 1.0) { stopped }
            .onEach { if (it is AudioEvent.Range && it.charStart >= sarahStart) stopped = true }
            .toList()

        assertTrue(events.none { it is AudioEvent.Done })
        assertTrue(events.none { it is AudioEvent.Range && it.charStart >= thomasStart })
        // Thomas's voice is never even requested once we stopped inside Sarah's segment.
        assertTrue(2 !in synth.requestedVoices)
    }

    @Test
    fun `an unmatched chunk still speaks, in the narrator voice`() {
        val synth = FakeRawSynthesizer()
        val events = UtteranceSynthesizer(TtsEngine(synth))
            .synthesize(chunk, MatchResult.none, cast, hostRate = 1.0) { false }
            .toList()

        assertEquals(listOf(cast.narratorVoiceId), synth.requestedVoices)
        assertTrue(events.any { it is AudioEvent.Started })
        assertTrue(events.any { it is AudioEvent.Done })
        val ranges = events.filterIsInstance<AudioEvent.Range>()
        assertEquals(0, ranges.minOf { it.charStart })
        assertEquals(chunk.length, ranges.maxOf { it.charEnd })
    }
}
