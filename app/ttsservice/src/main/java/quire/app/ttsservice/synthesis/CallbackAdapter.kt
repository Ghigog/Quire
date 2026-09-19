package quire.app.ttsservice.synthesis

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.TextToSpeech

/**
 * Plays an [UtteranceSynthesizer] event stream into a real Android [SynthesisCallback].
 *
 * The only class in this package that touches `android.speech.tts` — everything else is
 * plain Kotlin so it runs as a JVM unit test with no device or Robolectric shadow
 * (CLAUDE.md §9's "Android classes stay thin glue").
 */
object CallbackAdapter {

    /** Frame value `rangeStart`'s third parameter expects when an engine has none to report. */
    private const val NO_FRAME = 0

    fun play(events: Sequence<AudioEvent>, callback: SynthesisCallback, cancelled: () -> Boolean) {
        for (event in events) {
            if (cancelled()) return
            when (event) {
                is AudioEvent.Started -> callback.start(event.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
                is AudioEvent.Range -> callback.rangeStart(event.charStart, event.charEnd, NO_FRAME)
                is AudioEvent.Audio -> if (!write(event.pcm, callback, cancelled)) return
                AudioEvent.Done -> callback.done()
            }
        }
    }

    private fun write(pcm: ByteArray, callback: SynthesisCallback, cancelled: () -> Boolean): Boolean {
        var offset = 0
        val max = callback.maxBufferSize
        while (offset < pcm.size) {
            if (cancelled()) return false
            val len = minOf(max, pcm.size - offset)
            if (callback.audioAvailable(pcm, offset, len) != TextToSpeech.SUCCESS) return false
            offset += len
        }
        return true
    }
}
