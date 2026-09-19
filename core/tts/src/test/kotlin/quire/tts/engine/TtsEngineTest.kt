package quire.tts.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSynthesizer(
    private val samplesPerCall: Int = 4_800,
    private val sampleRate: Int = 24_000,
) : RawSynthesizer {
    var releaseCalls = 0
        private set
    var lastVoiceId: Int? = null

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        lastVoiceId = voiceId
        if (cancelled()) return null
        return RawAudio(FloatArray(samplesPerCall), sampleRate)
    }

    override fun release() {
        releaseCalls++
    }
}

class TtsEngineTest {

    @Test
    fun `synthesis produces audio and boundaries covering every word`() {
        val engine = TtsEngine(FakeSynthesizer(samplesPerCall = 4_800, sampleRate = 24_000))

        val chunk = engine.synthesize("Two sentences here. Read them aloud.", voiceId = 3)

        requireNotNull(chunk)
        assertEquals(4_800, chunk.pcm.size)
        assertEquals(24_000, chunk.sampleRate)
        assertEquals(3, chunk.voiceId)
        assertTrue(chunk.boundaries.isNotEmpty())
        assertEquals(2, chunk.boundaries.count { it.kind == BoundaryKind.SENTENCE })
        assertEquals(0L, chunk.boundaries.minOf { it.startMs })
        assertEquals(200L, chunk.boundaries.maxOf { it.endMs }) // 4800 samples / 24000 Hz = 200ms
    }

    @Test
    fun `voice id is passed through to the raw synthesizer`() {
        val synth = FakeSynthesizer()
        TtsEngine(synth).synthesize("hello", voiceId = 42)

        assertEquals(42, synth.lastVoiceId)
    }

    @Test
    fun `cancellation before synthesis returns null without calling the raw synthesizer`() {
        var synthesizeCalled = false
        val synth = object : RawSynthesizer {
            override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
                synthesizeCalled = true
                return RawAudio(FloatArray(10), 24_000)
            }

            override fun release() = Unit
        }

        val chunk = TtsEngine(synth).synthesize("hello", voiceId = 0, cancelled = { true })

        assertNull(chunk)
        assertTrue(!synthesizeCalled)
    }

    @Test
    fun `cancellation mid-synthesis discards the produced audio`() {
        val synth = object : RawSynthesizer {
            override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio {
                // A real engine polls `cancelled` during generation; here synthesis "completes"
                // but the cancellation flag flipped while it ran.
                return RawAudio(FloatArray(10), 24_000)
            }

            override fun release() = Unit
        }
        var cancelled = false

        val chunk = TtsEngine(synth).synthesize("hello", voiceId = 0, cancelled = { cancelled })
        cancelled = true
        val second = TtsEngine(synth).synthesize("hello", voiceId = 0, cancelled = { cancelled })

        assertTrue(chunk != null)
        assertNull(second)
    }

    @Test
    fun `release delegates to the raw synthesizer`() {
        val synth = FakeSynthesizer()
        val engine = TtsEngine(synth)

        engine.release()

        assertEquals(1, synth.releaseCalls)
    }
}
