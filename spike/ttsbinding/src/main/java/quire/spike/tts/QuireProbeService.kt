package quire.spike.tts

import android.content.ContentValues
import android.media.AudioFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * QUI-020: a system TTS engine that answers questions rather than reading books.
 *
 * It speaks a tone, not words. The deliverable is the observation log: what a host
 * actually sends us, how big it is, how often, and with what parameters. Everything in
 * `core:index`'s matcher is designed around assumptions this service exists to confirm.
 *
 * Throwaway. Nothing here ships.
 */
class QuireProbeService : TextToSpeechService() {

    private val supported = setOf("eng")
    private var currentLanguage = Triple("eng", "USA", "")

    @Volatile private var stopped = false
    private var lastCallAt = 0L
    private var utterance = 0

    private val header = listOf(
        "wall", "utterance", "gapMs", "chars", "rate", "pitch",
        "locale", "voice", "callerUid", "text",
    ).joinToString("\t") + "\n"

    /** Private copy, always written. Reachable only over adb. */
    private val logFile: File by lazy {
        File(getExternalFilesDir(null), "quire-probe.tsv").also { f ->
            if (!f.exists()) f.appendText(header)
        }
    }

    /**
     * Public copy in the device's Downloads folder, so the log can be collected from the
     * device itself — opened, shared or emailed — without a cable and without adb. Best
     * effort: every failure here is swallowed, because losing the convenient copy must
     * never cost us the run.
     *
     * Rewritten whole on every row rather than appended. `openOutputStream(uri, "wa")`
     * does not reliably append through MediaStore — measured on the Note Air5 C, 97 rows
     * each landed at offset 0 without truncating, leaving one row and the tail of a longer
     * earlier one. Holding the rows in memory and truncating on each write is O(n²) in
     * bytes and completely fine at a few thousand rows.
     */
    private var downloadsUri: Uri? = null
    private val rows = StringBuilder()

    private fun downloadsFile(): Uri? {
        downloadsUri?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "quire-probe-${startedAt}.tsv")
                put(MediaStore.Downloads.MIME_TYPE, "text/tab-separated-values")
            }
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        }.getOrNull()?.also { downloadsUri = it }
    }

    private val startedAt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    /** Write one row to both copies. Neither failure stops the probe. */
    @Synchronized
    private fun append(line: String) {
        if (rows.isEmpty()) rows.append(header)
        rows.append(line)
        runCatching { logFile.appendText(line) }
            .onFailure { Log.w(TAG, "private log: ${it.message}") }
        runCatching {
            downloadsFile()?.let { uri ->
                // "wt" truncates: we write the whole log, not a delta.
                contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(rows.toString().toByteArray())
                }
            }
        }.onFailure { Log.w(TAG, "downloads log: ${it.message}") }
    }

    // ---- Language plumbing. A host will not use an engine that claims nothing. ----

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int =
        when {
            lang !in supported -> TextToSpeech.LANG_NOT_SUPPORTED
            country.isNullOrEmpty() -> TextToSpeech.LANG_AVAILABLE
            variant.isNullOrEmpty() -> TextToSpeech.LANG_COUNTRY_AVAILABLE
            else -> TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val result = onIsLanguageAvailable(lang, country, variant)
        if (result != TextToSpeech.LANG_NOT_SUPPORTED) {
            currentLanguage = Triple(lang.orEmpty(), country.orEmpty(), variant.orEmpty())
        }
        return result
    }

    override fun onGetLanguage(): Array<String> =
        arrayOf(currentLanguage.first, currentLanguage.second, currentLanguage.third)

    override fun onStop() {
        stopped = true
        record("onStop")
    }

    // ---- The call we actually care about ----

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        stopped = false

        val text = (request.charSequenceText ?: "").toString()
        val now = System.currentTimeMillis()
        val gap = if (lastCallAt == 0L) 0 else now - lastCallAt
        lastCallAt = now
        utterance++

        val callerUid = runCatching { request.callerUid.toString() }.getOrDefault("?")
        val line = listOf(
            stamp(now), utterance.toString(), gap.toString(), text.length.toString(),
            request.speechRate.toString(), request.pitch.toString(),
            "${request.language}-${request.country}", request.voiceName ?: "-",
            callerUid,
            // Tabs and newlines would break the TSV; the shape of the text is what matters.
            text.replace("\t", "\\t").replace("\n", "\\n"),
        ).joinToString("\t")

        Log.i(TAG, "chars=${text.length} gap=${gap}ms rate=${request.speechRate} :: $text")
        append(line + "\n")

        val engine = engine()
        if (engine == null) speakTone(text, callback) else speak(engine, text, request, callback)
    }

    // ---- Real synthesis (QUI-017) ----

    private var loaded: TtsEngine? = null

    @Synchronized
    private fun engine(): TtsEngine? {
        val wanted = Prefs.engineId(this)?.let(Candidate::byId)
        if (wanted == null) { loaded?.release(); loaded = null; return null }
        loaded?.let { if (it.candidate.id == wanted.id) return it else it.release() }
        loaded = TtsEngine.load(this, wanted, Prefs.threads(this))
        Log.i(TAG, "engine ${wanted.id} loaded=${loaded != null}")
        return loaded
    }

    /**
     * Speak one chunk, switching voice between narration and quoted speech.
     *
     * The voice switching is a **demo, not attribution**: it splits on quote marks and
     * alternates two speaker ids, which is what @Voice already does and what Quire exists
     * to improve on. It is here because it exercises the mechanic QUI-024 needs — several
     * voices inside a single `onSynthesizeText` call, presented to the host as one
     * continuous utterance — and because hearing it is the point of this spike.
     */
    private fun speak(
        engine: TtsEngine,
        text: String,
        request: SynthesisRequest,
        callback: SynthesisCallback,
    ) {
        // The host sends rate and pitch as integer percentages, 100 being normal.
        val speed = (request.speechRate.coerceIn(20, 400)) / 100f
        callback.start(engine.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        var spokenChars = 0
        val started = System.nanoTime()
        var audioMs = 0L

        for ((span, isDialogue) in splitOnQuotes(text)) {
            if (stopped) break
            if (span.isBlank()) { spokenChars += span.length; continue }

            val voice = if (isDialogue) dialogueVoice(engine) else NARRATOR_VOICE
            val samples = runCatching { engine.synthesise(span, voice, speed) }
                .onFailure { Log.w(TAG, "synthesis failed: ${it.message}") }
                .getOrNull() ?: continue
            audioMs += samples.size * 1000L / engine.sampleRate

            // No word alignments come out of the engine, so ranges are estimated from the
            // span's position in the chunk. Sentence-level highlighting is fine on this;
            // word-level is not, and that is a finding for ADR-0002.
            runCatching { callback.rangeStart(spokenChars, spokenChars + span.length, 0) }
            spokenChars += span.length

            if (!writePcm(samples, callback)) return
        }

        val elapsed = (System.nanoTime() - started) / 1_000_000
        if (audioMs > 0) {
            Log.i(TAG, "RTF %.3f (%d ms synth for %d ms audio)".format(elapsed.toDouble() / audioMs, elapsed, audioMs))
        }
        callback.done()
    }

    /** Convert float samples to 16-bit PCM and hand them over in callback-sized pieces. */
    private fun writePcm(samples: FloatArray, callback: SynthesisCallback): Boolean {
        val pcm = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = (samples[i].coerceIn(-1f, 1f) * 32767).toInt()
            pcm[i * 2] = (v and 0xFF).toByte()
            pcm[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        var offset = 0
        val max = callback.maxBufferSize
        while (offset < pcm.size) {
            if (stopped) return false
            val len = minOf(max, pcm.size - offset)
            if (callback.audioAvailable(pcm, offset, len) != TextToSpeech.SUCCESS) return false
            offset += len
        }
        return true
    }

    /**
     * A second voice, chosen far from the narrator's.
     *
     * Speaker 1 sounds like speaker 0 — adjacent ids in `libritts_r` are neighbouring
     * readers from the same corpus, which is why the first device test heard no voice
     * change at all on a model carrying 904 of them. Halfway down the list is reliably a
     * different person.
     */
    private fun dialogueVoice(engine: TtsEngine) =
        if (engine.voiceCount > 1) engine.voiceCount / 2 else NARRATOR_VOICE

    /** Split into runs of narration and quoted speech, preserving every character. */
    private fun splitOnQuotes(text: String): List<Pair<String, Boolean>> {
        val out = mutableListOf<Pair<String, Boolean>>()
        val sb = StringBuilder()
        var inQuote = false
        for (c in text) {
            val isQuote = c == '"' || c == '\u201C' || c == '\u201D'
            if (isQuote) {
                sb.append(c)
                if (inQuote) { out += sb.toString() to true; sb.clear(); inQuote = false }
                else {
                    val head = sb.dropLast(1).toString()
                    if (head.isNotEmpty()) out += head to false
                    sb.clear(); sb.append(c); inQuote = true
                }
                continue
            }
            sb.append(c)
        }
        if (sb.isNotEmpty()) out += sb.toString() to inQuote
        return out
    }

    /**
     * Emit a tone of roughly the right duration, reporting word ranges as it goes.
     *
     * The audio is deliberately not speech: the question is whether the host feeds us and
     * consumes our callbacks, and a tone makes it obvious which engine is playing. The
     * `rangeStart` calls are the real payload — if the host highlights in response, we
     * know read-along survives in V1.
     */
    private fun speakTone(text: String, callback: SynthesisCallback) {
        val sampleRate = 22_050
        callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        // Roughly natural pacing, so chunk-to-chunk timing in the log stays meaningful.
        val words = wordRanges(text)
        val perWordMs = 300L

        for ((index, range) in words.withIndex()) {
            if (stopped) break
            // API 26+. Tells the host which characters we are speaking right now.
            runCatching { callback.rangeStart(range.first, range.last + 1, 0) }

            val samples = (sampleRate * perWordMs / 1000).toInt()
            val pcm = tone(samples, if (index % 2 == 0) 440.0 else 494.0, sampleRate)
            var offset = 0
            val max = callback.maxBufferSize
            while (offset < pcm.size && !stopped) {
                val len = minOf(max, pcm.size - offset)
                if (callback.audioAvailable(pcm, offset, len) != TextToSpeech.SUCCESS) return
                offset += len
            }
        }
        callback.done()
    }

    private fun tone(samples: Int, hz: Double, sampleRate: Int): ByteArray {
        val out = ByteArray(samples * 2)
        for (i in 0 until samples) {
            // Fade the edges so a sequence of tones does not click between words.
            val fade = minOf(1.0, minOf(i, samples - i) / (sampleRate * 0.01))
            val v = (sin(2.0 * PI * hz * i / sampleRate) * 8000 * fade).toInt()
            out[i * 2] = (v and 0xFF).toByte()
            out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }

    /** Character ranges of each word, so rangeStart points at something real. */
    private fun wordRanges(text: String): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (i in text.indices) {
            val word = !text[i].isWhitespace()
            if (word && start < 0) start = i
            if (!word && start >= 0) { out += start..(i - 1); start = -1 }
        }
        if (start >= 0) out += start..(text.length - 1)
        return out.ifEmpty { listOf(0..0) }
    }

    private fun record(event: String) {
        Log.i(TAG, event)
        append("${stamp(System.currentTimeMillis())}\t$event\n")
    }

    private fun stamp(millis: Long) =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

    private companion object {
        const val TAG = "QuireProbe"

        /** Speaker id used for narration; models expose voices as an index. */
        const val NARRATOR_VOICE = 0
    }
}
