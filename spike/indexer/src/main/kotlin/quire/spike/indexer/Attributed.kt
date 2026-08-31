package quire.spike.indexer

import java.io.File
import quire.index.Normalizer
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

/**
 * Turns a real book's attribution into index entries — the last link between "upload an
 * EPUB" and "hear it in voices".
 *
 * The pipeline emits segments: runs of a paragraph that are narration or one span of
 * speech. The index stores *sentences*, because that is what hosts send. So each paragraph
 * is split into sentences and the segments covering each are clipped onto it.
 *
 * Segment text is trimmed, which is why the export writes every paragraph's exact text
 * alongside its segments: rebuilding a paragraph by joining segments would drift from what
 * the reader's app sends, and a paragraph off by one space matches nothing.
 */
object Attributed {

    private data class Row(val kind: String, val speaker: String?, val confidence: Double, val text: String)

    fun entries(tsv: File): List<IndexEntry> {
        val out = mutableListOf<IndexEntry>()
        var paragraph: String? = null
        var segments = mutableListOf<Row>()

        fun flush() {
            val text = paragraph ?: return
            out += sentencesOf(text, segments, out.size)
            segments = mutableListOf()
        }

        for (line in tsv.readLines().drop(1)) {
            if (line.isBlank()) continue
            val parts = line.split('\t')
            if (parts.size < 6) continue
            val row = Row(parts[1], parts[2].ifBlank { null }, parts[3].toDoubleOrNull() ?: 0.0, parts[5])
            if (row.kind == "PARAGRAPH") {
                flush()
                paragraph = row.text
            } else {
                segments += row
            }
        }
        flush()
        return out
    }

    /**
     * Split one paragraph into index entries, carrying its segments' speakers onto them.
     *
     * Segments are located by walking the paragraph forward, because a short line of speech
     * can occur twice in one paragraph and only the first unconsumed occurrence is the one
     * this segment means.
     */
    private fun sentencesOf(paragraph: String, segments: List<Row>, firstSeq: Int): List<IndexEntry> {
        // Where each segment sits in the paragraph.
        val placed = mutableListOf<Triple<Int, Int, Row>>()
        var cursor = 0
        for (segment in segments) {
            val needle = segment.text.trim()
            if (needle.isEmpty()) continue
            val at = paragraph.indexOf(needle, cursor)
            if (at < 0) continue // the exporter trimmed something we cannot place; skip it
            placed += Triple(at, at + needle.length, segment)
            cursor = at + needle.length
        }

        val out = mutableListOf<IndexEntry>()
        var offset = 0
        for (sentence in Normalizer.sentences(paragraph)) {
            val start = paragraph.indexOf(sentence, offset).takeIf { it >= 0 } ?: offset
            val end = start + sentence.length
            offset = end

            val spans = placed.mapNotNull { (from, to, row) ->
                val lo = maxOf(from, start)
                val hi = minOf(to, end)
                if (lo >= hi) return@mapNotNull null
                VoiceSpan(
                    start = lo - start,
                    end = hi - start,
                    kind = if (row.kind == "DIALOGUE") Kind.DIALOGUE else Kind.NARRATION,
                    speakerId = row.speaker,
                    confidence = row.confidence,
                )
            }.ifEmpty {
                // A sentence no segment covered — a heading, a stray numeral. Narration is
                // the right answer and the narrator is the right voice.
                listOf(VoiceSpan(0, sentence.length, Kind.NARRATION, null, 0.0))
            }

            out += IndexEntry(
                seq = firstSeq + out.size,
                text = sentence,
                normalized = Normalizer.normalize(sentence),
                spans = spans,
            )
        }
        return out
    }
}
