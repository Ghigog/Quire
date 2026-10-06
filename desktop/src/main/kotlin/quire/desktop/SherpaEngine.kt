package quire.desktop

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import quire.tts.engine.RawAudio
import quire.tts.engine.RawSynthesizer

/**
 * QUI-046: [RawSynthesizer] over sherpa-onnx, on the JVM.
 *
 * This is the desktop twin of `spike/ttsbinding`'s `TtsEngine`. Same runtime, same version,
 * same model — the difference is the binding. The Android module binds the Kotlin API that
 * ships in the AAR; the JVM jar ships the Java API, which configures through builders
 * (`OfflineTtsVitsModelConfig.builder()`) rather than named constructor arguments. The
 * synthesis call and the shape it returns are the same either way.
 *
 * **Why this is the piece that was missing.** `core:tts`'s own [RawSynthesizer] doc says the
 * engine "lives in `app:ttsservice` rather than here… and there is no desktop build of
 * sherpa-onnx's Kotlin binding to test against." There is now a desktop build of its *Java*
 * binding, which is what makes a book audible on a laptop without an Android build.
 *
 * File discovery is by walk rather than by name, for the reason the Android loader gives:
 * Piper names its weights after the voice, and a wrong path handed to native code is a
 * SIGSEGV rather than an exception.
 */
class SherpaEngine private constructor(
    private val tts: OfflineTts,
    private val speed: Float,
    val sampleRate: Int,
    val speakerCount: Int,
) : RawSynthesizer {

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        if (text.isBlank() || cancelled()) return null
        val speaker = voiceId.coerceIn(0, (speakerCount - 1).coerceAtLeast(0))

        // sherpa's callback is polled as samples come off the decoder and a non-zero return
        // stops it, so a cancelled utterance stops producing rather than being discarded
        // after the fact — the contract [RawSynthesizer] documents.
        val audio = tts.generateWithCallback(text, speaker, speed) { _ -> if (cancelled()) 1 else 0 }
        if (cancelled() || audio.samples.isEmpty()) return null
        return RawAudio(audio.samples, audio.sampleRate)
    }

    override fun release() = tts.release()

    companion object {

        /** ADR-0002's model, and the one [SpeakerProfile]'s fixture was measured from. */
        const val DEFAULT_MODEL = "vits-piper-en_US-libritts_r-medium"

        fun load(modelDir: File, speed: Float = 1.0f, threads: Int = 2): SherpaEngine {
            require(modelDir.isDirectory) { "no model directory at ${modelDir.path}" }

            val onnx = modelDir.walkTopDown()
                .filter { it.isFile && it.extension == "onnx" }
                .minByOrNull { it.name.length } // "model.onnx" over "model.int8.onnx"
                ?: throw IllegalStateException("no .onnx file in ${modelDir.path}")
            val tokens = File(modelDir, "tokens.txt").takeIf { it.isFile }
                ?: throw IllegalStateException("no tokens.txt in ${modelDir.path}")
            val espeak = File(modelDir, "espeak-ng-data").takeIf { it.isDirectory }

            val vits = OfflineTtsVitsModelConfig.builder()
                .setModel(onnx.absolutePath)
                .setTokens(tokens.absolutePath)
                .apply { espeak?.let { setDataDir(it.absolutePath) } }
                .build()

            val model = OfflineTtsModelConfig.builder()
                .setVits(vits)
                .setNumThreads(threads)
                .build()

            val tts = OfflineTts(OfflineTtsConfig.builder().setModel(model).build())
            return SherpaEngine(tts, speed, tts.sampleRate, speakerCountOf(modelDir))
        }

        /**
         * The Java API exposes no `numSpeakers()`, though the Kotlin one does. The model's
         * own config carries it, so read it there rather than pinning 904 — a different
         * Piper voice has a different cast and would silently clamp every character to its
         * last speaker.
         */
        private fun speakerCountOf(modelDir: File): Int {
            val config = modelDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.endsWith(".onnx.json") } ?: return 1
            val found = Regex("\"num_speakers\"\\s*:\\s*(\\d+)").find(config.readText())
                ?: return 1
            return found.groupValues[1].toInt().coerceAtLeast(1)
        }
    }
}
