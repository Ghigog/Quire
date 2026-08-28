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
            if (!dir.isDirectory) {
                Log.e(TAG, "${candidate.id}: not installed")
                return null
            }

            // Discover the files rather than guessing their names. Piper names its weights
            // after the voice — en_GB-alan-low.onnx, not model.onnx — and an earlier build
            // hardcoded a list, missed, and handed native code an empty path, which is a
            // SIGSEGV rather than a catchable exception. Never pass a path we have not
            // confirmed exists.
            val onnx = dir.walkTopDown()
                .filter { it.isFile && it.extension == "onnx" }
                .sortedBy { it.name.length } // "model.onnx" over "model.int8.onnx"
                .firstOrNull()
            val tokens = File(dir, "tokens.txt").takeIf { it.exists() }
            val espeak = File(dir, "espeak-ng-data").takeIf { it.isDirectory }
            val voices = File(dir, "voices.bin").takeIf { it.exists() }
            val dict = File(dir, "dict").takeIf { it.isDirectory }
            val lexicon = listOf("lexicon-us-en.txt", "lexicon.txt")
                .map { File(dir, it) }.firstOrNull { it.exists() }

            Log.i(
                TAG,
                "${candidate.id}: onnx=${onnx?.name} tokens=${tokens != null} " +
                    "espeak=${espeak != null} voices=${voices != null} lexicon=${lexicon?.name}",
            )

            if (onnx == null || tokens == null) {
                Log.e(TAG, "${candidate.id}: missing model or tokens; refusing to load")
                return null
            }
            if (candidate.kind != Candidate.Kind.VITS && voices == null) {
                Log.e(TAG, "${candidate.id}: ${candidate.kind} needs voices.bin; refusing to load")
                return null
            }

            fun p(f: File?) = f?.absolutePath.orEmpty()

            val model = when (candidate.kind) {
                Candidate.Kind.KOKORO -> OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = p(onnx), voices = p(voices), tokens = p(tokens),
                        dataDir = p(espeak), lexicon = p(lexicon), dictDir = p(dict),
                    ),
                    numThreads = threads,
                )
                Candidate.Kind.VITS -> OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = p(onnx), tokens = p(tokens), dataDir = p(espeak),
                    ),
                    numThreads = threads,
                )
                Candidate.Kind.KITTEN -> OfflineTtsModelConfig(
                    kitten = OfflineTtsKittenModelConfig(
                        model = p(onnx), voices = p(voices), tokens = p(tokens),
                        dataDir = p(espeak),
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
