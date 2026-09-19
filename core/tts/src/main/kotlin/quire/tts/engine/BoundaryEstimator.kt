package quire.tts.engine

/**
 * Word and sentence boundary timestamps, estimated from each span's position in the
 * synthesised text rather than measured during synthesis.
 *
 * There is nothing else to estimate them from. ADR-0004 ("Word timestamps are not available
 * from sherpa-onnx") found that `OfflineTts.generate()` returns samples and a sample rate,
 * full stop — reconfirmed here against sherpa-onnx 1.13.8's Python binding, which mirrors
 * the Kotlin API `spike/ttsbinding` calls: `GeneratedAudio` carries no alignment. A forced
 * aligner would buy real per-word timing at the cost of a second model in a memory and RTF
 * budget ADR-0002 already exceeds. ADR-0004 flagged position-based estimation as "fine at
 * sentence granularity, poor at word granularity" and left the choice to this ticket; it is
 * accepted here for both, because QUI-014's highlighting needs a word to arrive close enough
 * in sequence to track the sentence being read, not a frame-accurate one.
 *
 * The estimate: each sentence gets a share of the total duration proportional to its share
 * of the text's non-whitespace characters, and each word within a sentence gets the same
 * split of its sentence's share. A trailing sentence or word absorbs any rounding remainder
 * so the last boundary always ends exactly at [totalDurationMs].
 */
object BoundaryEstimator {

    fun estimate(text: String, totalDurationMs: Long): List<Boundary> {
        if (totalDurationMs <= 0) return emptyList()
        val sentences = sentenceSpans(text)
        if (sentences.isEmpty()) return emptyList()

        val boundaries = mutableListOf<Boundary>()
        val sentenceWeights = sentences.map { weightOf(text, it) }
        val totalWeight = sentenceWeights.sum().coerceAtLeast(1)

        var elapsedMs = 0L
        var elapsedWeight = 0
        for ((index, sentence) in sentences.withIndex()) {
            val weight = sentenceWeights[index]
            elapsedWeight += weight
            val sentenceEndMs = if (index == sentences.lastIndex) {
                totalDurationMs
            } else {
                elapsedWeight * totalDurationMs / totalWeight
            }
            val sentenceStartMs = elapsedMs

            boundaries += wordBoundaries(text, sentence, sentenceStartMs, sentenceEndMs)
            boundaries += Boundary(
                sentenceStartMs,
                sentenceEndMs,
                sentence.first,
                sentence.last + 1,
                BoundaryKind.SENTENCE,
            )

            elapsedMs = sentenceEndMs
        }
        return boundaries
    }

    private fun wordBoundaries(
        text: String,
        sentence: IntRange,
        startMs: Long,
        endMs: Long,
    ): List<Boundary> {
        val words = wordSpans(text, sentence)
        if (words.isEmpty()) return emptyList()

        val durationMs = endMs - startMs
        val wordWeights = words.map { weightOf(text, it) }
        val totalWeight = wordWeights.sum().coerceAtLeast(1)

        val result = mutableListOf<Boundary>()
        var elapsedMs = startMs
        var elapsedWeight = 0
        for ((index, word) in words.withIndex()) {
            elapsedWeight += wordWeights[index]
            val wordEndMs = if (index == words.lastIndex) {
                endMs
            } else {
                startMs + elapsedWeight * durationMs / totalWeight
            }
            result += Boundary(elapsedMs, wordEndMs, word.first, word.last + 1, BoundaryKind.WORD)
            elapsedMs = wordEndMs
        }
        return result
    }

    /** Non-whitespace characters in [span] — the proxy for how long a listener spends on it. */
    private fun weightOf(text: String, span: IntRange): Int =
        span.count { !text[it].isWhitespace() }.coerceAtLeast(1)

    /** Whitespace-delimited spans within [range], end-inclusive, empty runs skipped. */
    private fun wordSpans(text: String, range: IntRange): List<IntRange> {
        val spans = mutableListOf<IntRange>()
        var start = -1
        for (i in range) {
            if (text[i].isWhitespace()) {
                if (start >= 0) spans += start..(i - 1)
                start = -1
            } else if (start < 0) {
                start = i
            }
        }
        if (start >= 0) spans += start..range.last
        return spans
    }

    private val SENTENCE_END = setOf('.', '!', '?')

    /**
     * Sentence spans, trimmed of surrounding whitespace, end-inclusive. A run of sentence-
     * ending punctuation (and any closing quote or bracket immediately after it) closes a
     * sentence; text with no such punctuation at all is one sentence.
     */
    private fun sentenceSpans(text: String): List<IntRange> {
        val spans = mutableListOf<IntRange>()
        var start = firstNonWhitespace(text, 0)
        var i = start
        while (start >= 0 && i in text.indices) {
            if (text[i] in SENTENCE_END) {
                var end = i
                while (end + 1 < text.length && text[end + 1] in SENTENCE_END) end++
                while (end + 1 < text.length && text[end + 1] in CLOSING) end++
                spans += start..end
                start = firstNonWhitespace(text, end + 1)
                i = start
            } else {
                i++
            }
        }
        if (start >= 0) {
            val end = lastNonWhitespace(text, text.length - 1)
            if (end >= start) spans += start..end
        }
        return spans
    }

    private val CLOSING = setOf('"', '\'', '”', '’', ')', ']')

    private fun firstNonWhitespace(text: String, from: Int): Int {
        var i = from
        while (i < text.length && text[i].isWhitespace()) i++
        return if (i < text.length) i else -1
    }

    private fun lastNonWhitespace(text: String, from: Int): Int {
        var i = from
        while (i >= 0 && text[i].isWhitespace()) i--
        return i
    }
}
