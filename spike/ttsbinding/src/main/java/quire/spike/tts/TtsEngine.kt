package quire.spike.tts

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File

/**
 * Loads a downloaded candidate as a sherpa-onnx engine (QUI-017).
 *
 * One instance, serialised inference. Concurrency buys nothing at RTF 0.15 and doubles
 * peak memory; the prior art hit native concurrency bugs doing otherwise.
 */
class TtsEngine private constructor(
    val candidate: Candidate,
    private val tts: OfflineTts,
) {
    val sampleRate: Int get() = tts.sampleRate()
    val voiceCount: Int get() = tts.numSpeakers()

    /** Synthesise, returning float samples in [-1, 1]. [speakerId] selects a voice. */
    @Synchronized
    fun synthesise(text: String, speakerId: Int, speed: Float): FloatArray =
        tts.generate(text, speakerId, speed).samples

    @Synchronized
    fun release() = tts.release()

    companion object {
        private const val TAG = "QuireProbe"

        fun load(context: Context, candidate: Candidate, threads: Int = 2): TtsEngine? {
            val dir = ModelStore.dirFor(context, candidate)
            if (!dir.isDirectory) return null

            fun path(vararg names: String): String =
                names.map { File(dir, it) }.firstOrNull { it.exists() }?.absolutePath ?: ""

            val model = when (candidate.kind) {
                Candidate.Kind.KOKORO -> OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = path("model.onnx", "model.int8.onnx"),
                        voices = path("voices.bin"),
                        tokens = path("tokens.txt"),
                        dataDir = File(dir, "espeak-ng-data").absolutePath,
                        lexicon = path("lexicon-us-en.txt", "lexicon.txt"),
                        dictDir = File(dir, "dict").takeIf { it.isDirectory }?.absolutePath ?: "",
                    ),
                    numThreads = threads,
                )
                Candidate.Kind.VITS -> OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = path("en_US-libritts_r-medium.onnx", "model.onnx"),
                        tokens = path("tokens.txt"),
                        dataDir = File(dir, "espeak-ng-data").absolutePath,
                    ),
                    numThreads = threads,
                )
                Candidate.Kind.KITTEN -> OfflineTtsModelConfig(
                    kitten = OfflineTtsKittenModelConfig(
                        model = path("model.fp16.onnx", "model.onnx"),
                        voices = path("voices.bin"),
                        tokens = path("tokens.txt"),
                        dataDir = File(dir, "espeak-ng-data").absolutePath,
                    ),
                    numThreads = threads,
                )
            }

            return runCatching {
                TtsEngine(candidate, OfflineTts(config = OfflineTtsConfig(model = model)))
            }.onFailure {
                Log.e(TAG, "could not load ${candidate.id}: ${it.message}", it)
            }.getOrNull()
        }
    }
}
