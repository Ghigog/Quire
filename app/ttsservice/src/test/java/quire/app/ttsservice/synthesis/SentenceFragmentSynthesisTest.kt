package quire.app.ttsservice.synthesis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.index.How
import quire.index.MatchResult
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan
import quire.tts.casting.Cast
import quire.tts.casting.VoiceAssignment
import quire.tts.engine.RawAudio
import quire.tts.engine.RawSynthesizer
import quire.tts.engine.TtsEngine
import quire.tts.sentence.SentenceCache

private const val SAMPLE_RATE = 16_000

/** Deterministic stand-in for the ONNX engine: duration is proportional to text length. */
private class SentenceCacheFakeSynthesizer(private val msPerChar: Long = 50) : RawSynthesizer {
    val requestedTexts = mutableListOf<String>()

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        if (cancelled()) return null
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

/**
 * QUI-030's fragment-serving path, wired through [UtteranceSynthesizer]: a host that asks
 * for one sentence in two clauses gets both from a single whole-sentence synthesis, not
 * one standalone utterance per clause.
 */
class SentenceFragmentSynthesisTest {

    private val sentence = "\"I know,\" said Sarah, \"we should leave.\""
    private val narrationStart = sentence.indexOf("said")
    private val secondQuoteStart = sentence.indexOf("\"we")
    private val entry = IndexEntry(
        seq = 5,
        text = sentence,
        normalized = sentence.lowercase(),
        spans = listOf(
            VoiceSpan(0, narrationStart, Kind.DIALOGUE, "sarah", 0.9),
            VoiceSpan(narrationStart, secondQuoteStart, Kind.NARRATION, null, 1.0),
            VoiceSpan(secondQuoteStart, sentence.length, Kind.DIALOGUE, "sarah", 0.9),
        ),
    )

    private fun matchFor(clause: String) = MatchResult(
        how = How.FORWARD,
        entries = listOf(entry),
        spans = emptyList(), // unused by the cache path; only match.entries is consulted
        partial = true,
    )

    @Test
    fun `a sentence split at its comma sounds continuous - one synthesis, not one per clause`() {
        val synth = SentenceCacheFakeSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val synthesizer = UtteranceSynthesizer(TtsEngine(synth), cache)

        val firstClause = sentence.substring(0, narrationStart)
        synthesizer.synthesize(firstClause, matchFor(firstClause), cast, hostRate = 1.0) { false }.toList()

        // Building the cache synthesises every span of the whole sentence, not just the
        // clause that arrived — the fix itself: the comma's intonation depends on what
        // follows it, which a lone clause cannot supply.
        assertEquals(entry.spans.map { sentence.substring(it.start, it.end) }, synth.requestedTexts)
    }

    @Test
    fun `later clauses cost nothing - no further synthesis call`() {
        val synth = SentenceCacheFakeSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val synthesizer = UtteranceSynthesizer(TtsEngine(synth), cache)

        val firstClause = sentence.substring(0, narrationStart)
        val secondClause = sentence.substring(narrationStart, secondQuoteStart)
        val thirdClause = sentence.substring(secondQuoteStart)

        synthesizer.synthesize(firstClause, matchFor(firstClause), cast, hostRate = 1.0) { false }.toList()
        val callsAfterFirst = synth.requestedTexts.size

        val secondEvents =
            synthesizer.synthesize(secondClause, matchFor(secondClause), cast, hostRate = 1.0) { false }.toList()
        val thirdEvents =
            synthesizer.synthesize(thirdClause, matchFor(thirdClause), cast, hostRate = 1.0) { false }.toList()

        assertEquals(callsAfterFirst, synth.requestedTexts.size)
        assertTrue(secondEvents.any { it is AudioEvent.Audio })
        assertTrue(thirdEvents.any { it is AudioEvent.Audio })
    }

    @Test
    fun `mixed sentences keep their voices when served from cache`() {
        val synth = SentenceCacheFakeSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val synthesizer = UtteranceSynthesizer(TtsEngine(synth), cache)

        val secondClause = sentence.substring(narrationStart, secondQuoteStart)
        cache.sentence(entry, cast) { false } // pre-warm, as the first clause would have

        val ranges = synthesizer.synthesize(secondClause, matchFor(secondClause), cast, hostRate = 1.0) { false }
            .filterIsInstance<AudioEvent.Range>()
            .toList()

        assertTrue(ranges.isNotEmpty())
        for (range in ranges) {
            assertTrue(range.charStart in 0..secondClause.length)
            assertTrue(range.charEnd in range.charStart..secondClause.length)
        }
    }

    @Test
    fun `a chunk the matcher glued across entries is not served from cache`() {
        val synth = SentenceCacheFakeSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val synthesizer = UtteranceSynthesizer(TtsEngine(synth), cache)

        val glued = MatchResult(
            how = How.FORWARD,
            entries = listOf(entry, entry.copy(seq = 6)),
            spans = emptyList(),
        )

        synthesizer.synthesize("Heading. " + sentence, glued, cast, hostRate = 1.0) { false }.toList()

        // Fell back to fresh synthesis of the chunk as narration (no matcher spans given),
        // not a whole-sentence cache build.
        assertEquals(listOf("Heading. " + sentence), synth.requestedTexts)
    }

    @Test
    fun `entries behind the cursor are evicted as later chunks arrive`() {
        val synth = SentenceCacheFakeSynthesizer()
        val cache = SentenceCache(TtsEngine(synth))
        val synthesizer = UtteranceSynthesizer(TtsEngine(synth), cache)

        val next = entry.copy(seq = 6, text = "Next.", normalized = "next.", spans = listOf(
            VoiceSpan(0, 5, Kind.NARRATION, null, 1.0),
        ))

        synthesizer.synthesize(sentence, matchFor(sentence), cast, hostRate = 1.0) { false }.toList()
        assertTrue(5 in cache.cachedSeqs)

        val nextMatch = MatchResult(how = How.FORWARD, entries = listOf(next), spans = emptyList())
        synthesizer.synthesize("Next.", nextMatch, cast, hostRate = 1.0) { false }.toList()

        assertTrue(5 !in cache.cachedSeqs)
        assertTrue(6 in cache.cachedSeqs)
    }
}
