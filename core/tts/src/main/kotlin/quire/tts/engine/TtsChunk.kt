package quire.tts.engine

/** A word both starts and ends speech; a sentence only ever ends it, never starts fresh. */
enum class BoundaryKind { WORD, SENTENCE }

/**
 * Where one word or sentence falls in a [TtsChunk]'s audio and in the text that produced it.
 *
 * [charStart]/[charEnd] index into the text passed to [TtsEngine.synthesize], end-exclusive
 * (`text.substring(charStart, charEnd)` recovers the span), so a matcher can hand these
 * straight to a host's `rangeStart` callback without another offset translation.
 */
data class Boundary(
    val startMs: Long,
    val endMs: Long,
    val charStart: Int,
    val charEnd: Int,
    val kind: BoundaryKind,
)

/**
 * One utterance's audio, ready for the ring buffer (QUI-012) and the highlighter (QUI-014).
 *
 * [pcm] is mono float samples in `[-1, 1]` at [sampleRate] — the shape sherpa-onnx's
 * `GeneratedAudio` returns (confirmed against the 1.13.8 Python binding, which mirrors the
 * Kotlin API `spike/ttsbinding` calls; there is no other shape to normalise from).
 * [boundaries] is sorted ascending by [Boundary.startMs] and covers the whole input text.
 */
data class TtsChunk(
    val pcm: FloatArray,
    val sampleRate: Int,
    val voiceId: Int,
    val boundaries: List<Boundary>,
)
