package quire.tts.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class ScriptedSynthesizer(private val behavior: () -> RawAudio?) : RawSynthesizer {
    var calls = 0
        private set
    var releaseCalls = 0
        private set

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        calls++
        return behavior()
    }

    override fun release() {
        releaseCalls++
    }
}

private fun failing() = ScriptedSynthesizer { error("network down") }
private fun succeeding(sampleRate: Int = 24_000) = ScriptedSynthesizer { RawAudio(FloatArray(10), sampleRate) }

class FallbackSynthesizerTest {

    @Test
    fun `a healthy primary is used and local is never touched`() {
        val primary = succeeding(sampleRate = 11_025)
        val local = succeeding(sampleRate = 24_000)
        val fallback = FallbackSynthesizer(primary, local)

        val audio = fallback.synthesize("hello", voiceId = 0, cancelled = { false })

        assertEquals(11_025, audio?.sampleRate)
        assertEquals(0, local.calls)
    }

    @Test
    fun `a failing primary falls back to local and announces once`() {
        val local = succeeding()
        var announcements = 0
        val fallback = FallbackSynthesizer(failing(), local) { announcements++ }

        fallback.synthesize("line one", voiceId = 0, cancelled = { false })
        fallback.synthesize("line two", voiceId = 0, cancelled = { false })
        fallback.synthesize("line three", voiceId = 0, cancelled = { false })

        assertEquals(1, announcements)
        assertEquals(3, local.calls)
    }

    @Test
    fun `resetting the notice re-announces on the next failure`() {
        val local = succeeding()
        var announcements = 0
        val fallback = FallbackSynthesizer(failing(), local) { announcements++ }

        fallback.synthesize("chapter 1 line", voiceId = 0, cancelled = { false })
        fallback.resetFallbackNotice()
        fallback.synthesize("chapter 2 line", voiceId = 0, cancelled = { false })

        assertEquals(2, announcements)
    }

    @Test
    fun `a clean cancellation returns null without falling back or announcing`() {
        val local = succeeding()
        var announcements = 0
        val cancelledPrimary = ScriptedSynthesizer { null }
        val fallback = FallbackSynthesizer(cancelledPrimary, local) { announcements++ }

        val audio = fallback.synthesize("hello", voiceId = 0, cancelled = { true })

        assertNull(audio)
        assertEquals(0, local.calls)
        assertEquals(0, announcements)
    }

    @Test
    fun `release releases both synthesizers`() {
        val primary = succeeding()
        val local = succeeding()
        val fallback = FallbackSynthesizer(primary, local)

        fallback.release()

        assertEquals(1, primary.releaseCalls)
        assertEquals(1, local.releaseCalls)
    }
}
