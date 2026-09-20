package quire.tts.sentence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan
import quire.tts.casting.Cast
import quire.tts.casting.VoiceAssignment
import quire.tts.engine.RawAudio
import quire.tts.engine.RawSynthesizer
import quire.tts.engine.TtsEngine

private const val SAMPLE_RATE = 16_000

/** Deterministic stand-in for the ONNX engine: duration is proportional to text length. */
private class FakeRawSynthesizer(private val msPerChar: Long = 50) : RawSynthesizer {
    var calls = 0
        private set
    val requestedTexts = mutableListOf<String>()

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        if (cancelled()) return null
        calls++
        requestedTexts += text
        val durationMs = text.length.coerceAtLeast(1) * msPerChar
        val samples = (durationMs * SAMPLE_RATE / 1000).toInt().coerceAtLeast(1)
        return RawAudio(FloatArray(samples) { 0.5f }, SAMPLE_RATE)
    }

    override fun release() = Unit
}

private val cast = Cast(
    bookId = "book-1",
    narratorVoiceId = 0,
    voices = mapOf("sarah" to VoiceAssignment(voiceId = 1)),
)

private fun entry(seq: Int, text: String, spans: List<VoiceSpan>) =
    IndexEntry(seq = seq, text = text, normalized = text.lowercase(), spans = spans)

class SentenceCacheTest {

    private val line = "\"I know,\" said Sarah, \"we should leave.\""
    private val dialogueEnd = line.indexOf("said")
    private val entrySpans = listOf(
        VoiceSpan(0, dialogueEnd, Kind.DIALOGUE, "sarah", 0.9),
        VoiceSpan(dialogueEnd, line.length, Kind.NARRATION, null, 1.0),
    )

    @Test
    fun `a sentence is synthesised once even when asked for twice`() {
        val synth = FakeRawSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val e = entry(0, line, entrySpans)

        cache.sentence(e, cast, cancelled = { false })
        cache.sentence(e, cast, cancelled = { false })

        assertEquals(entrySpans.size, synth.calls)
    }

    @Test
    fun `later clauses are served from cache without a further synthesis call`() {
        val synth = FakeRawSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val e = entry(0, line, entrySpans)

        val sentence = cache.sentence(e, cast, cancelled = { false })
        val callsAfterBuild = synth.calls

        val first = sentence.fragment(0, dialogueEnd)
        val second = sentence.fragment(dialogueEnd, line.length)

        assertEquals(callsAfterBuild, synth.calls)
        assertTrue(first != null && first.isNotEmpty())
        assertTrue(second != null && second.isNotEmpty())
    }

    @Test
    fun `mixed sentences keep their voices`() {
        val synth = FakeRawSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val e = entry(0, line, entrySpans)
        val sentence = cache.sentence(e, cast, cancelled = { false })

        val fragments = requireNotNull(sentence.fragment(0, line.length))

        assertEquals(listOf("sarah", null), fragments.map { it.speakerId })
        assertEquals(listOf(1, 0), fragments.map { it.voiceId })
    }

    @Test
    fun `a fragment's boundaries are rebased to start at zero`() {
        val synth = FakeRawSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val e = entry(0, line, entrySpans)
        val sentence = cache.sentence(e, cast, cancelled = { false })

        val narration = requireNotNull(sentence.fragment(dialogueEnd, line.length)).single()

        assertEquals(0, narration.chunk.boundaries.minOf { it.charStart })
        for (b in narration.chunk.boundaries) {
            assertTrue(b.charEnd <= line.length - dialogueEnd)
        }
    }

    @Test
    fun `a span the engine failed on is reported as a miss, not served as a hole`() {
        val failing = object : RawSynthesizer {
            override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean) = null
            override fun release() = Unit
        }
        val cache = SentenceCache(TtsEngine(failing))
        val e = entry(0, line, entrySpans)
        val sentence = cache.sentence(e, cast, cancelled = { false })

        assertNull(sentence.fragment(0, line.length))
    }

    @Test
    fun `entries behind the cursor are evicted`() {
        val synth = FakeRawSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val narrationOnly = listOf(VoiceSpan(0, 3, Kind.NARRATION, null, 1.0))

        for (seq in 0..4) cache.sentence(entry(seq, "Hi.", narrationOnly), cast) { false }
        assertEquals((0..4).toSet(), cache.cachedSeqs)

        cache.evictBefore(3)

        assertEquals(setOf(3, 4), cache.cachedSeqs)
    }

    @Test
    fun `evicting a seq the cache never held is harmless`() {
        val cache = SentenceCache(TtsEngine(FakeRawSynthesizer()))
        cache.evictBefore(10)
        assertTrue(cache.cachedSeqs.isEmpty())
    }
}
