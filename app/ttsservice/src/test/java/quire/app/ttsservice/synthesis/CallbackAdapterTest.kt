package quire.app.ttsservice.synthesis

import android.speech.tts.SynthesisCallback
import android.speech.tts.TextToSpeech
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Records every call it receives, exactly like the real callback's contract expects them. */
private class FakeCallback(private val bufferSize: Int = 8) : SynthesisCallback {
    val calls = mutableListOf<String>()
    val audio = mutableListOf<Byte>()

    override fun getMaxBufferSize(): Int = bufferSize

    override fun start(sampleRateInHz: Int, audioFormat: Int, channelCount: Int): Int {
        calls += "start($sampleRateInHz)"
        return TextToSpeech.SUCCESS
    }

    override fun audioAvailable(buffer: ByteArray, offset: Int, length: Int): Int {
        calls += "audio($length)"
        for (i in offset until offset + length) audio += buffer[i]
        return TextToSpeech.SUCCESS
    }

    override fun done(): Int {
        calls += "done"
        return TextToSpeech.SUCCESS
    }

    override fun error() {
        calls += "error"
    }

    override fun error(errorCode: Int) {
        calls += "error($errorCode)"
    }

    override fun hasStarted(): Boolean = calls.any { it.startsWith("start") }
    override fun hasFinished(): Boolean = calls.contains("done")

    override fun rangeStart(start: Int, end: Int, frame: Int) {
        calls += "range($start,$end)"
    }
}

class CallbackAdapterTest {

    @Test
    fun `plays start, ranges, audio and done in order, chunked to the callback's buffer`() {
        val callback = FakeCallback(bufferSize = 8)
        val events = sequenceOf(
            AudioEvent.Started(16_000),
            AudioEvent.Range(0, 5),
            AudioEvent.Audio(ByteArray(10) { (it + 1).toByte() }),
            AudioEvent.Done,
        )

        CallbackAdapter.play(events, callback) { false }

        assertEquals(listOf("start(16000)", "range(0,5)", "audio(8)", "audio(2)", "done"), callback.calls)
        assertEquals((1..10).map { it.toByte() }, callback.audio)
    }

    @Test
    fun `stops writing once cancelled, and never calls done`() {
        val callback = FakeCallback()
        var stopped = false
        val events = sequence {
            yield(AudioEvent.Started(16_000))
            yield(AudioEvent.Audio(byteArrayOf(1, 2, 3, 4)))
            stopped = true
            yield(AudioEvent.Audio(byteArrayOf(5, 6, 7, 8)))
            yield(AudioEvent.Done)
        }

        CallbackAdapter.play(events, callback) { stopped }

        assertTrue(callback.calls.none { it == "done" })
        assertEquals(listOf<Byte>(1, 2, 3, 4), callback.audio)
    }
}
