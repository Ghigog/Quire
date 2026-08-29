package quire.index

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

/**
 * QUI-027 — a chunk gets the spans it actually covered, in its own coordinates.
 *
 * Matching works in normalised text; spans and `rangeStart` are raw. While a chunk covers
 * whole entries the two agree at the boundaries, and stop agreeing the moment a chunk ends
 * mid-entry — which ADR-0004 measured as the common case, 42 of 73 chunks ending on a
 * comma.
 */
class MatcherClipTest {

    //                        0        9                   21
    private val line = "\"I know,\" said Sarah."
    private val dialogue = VoiceSpan(0, 9, Kind.DIALOGUE, "Sarah", 1.0)
    private val narration = VoiceSpan(9, line.length, Kind.NARRATION, null, 1.0)

    private fun matcher() = Matcher(InMemoryBookIndex("t", listOf(
        IndexEntry(0, line, Normalizer.normalize(line), listOf(dialogue, narration)),
        IndexEntry(1, "Thomas did not look up.",
            Normalizer.normalize("Thomas did not look up."),
            listOf(VoiceSpan(0, 23, Kind.NARRATION, null, 1.0))),
    )))

    @Test
    fun `a speech tag split at its comma reports only the speaking half`() {
        val result = matcher().match("\"I know,\"")
        assertEquals(listOf("Sarah"), result.spans.map { it.speakerId })
        assertEquals(listOf(Kind.DIALOGUE), result.spans.map { it.kind })
    }

    @Test
    fun `the narration half reports only narration`() {
        val matcher = matcher()
        matcher.match("\"I know,\"")
        val result = matcher.match(" said Sarah.")
        assertEquals(listOf<String?>(null), result.spans.map { it.speakerId })
        assertEquals(listOf(Kind.NARRATION), result.spans.map { it.kind })
    }

    @Test
    fun `offsets address the host's own string`() {
        val chunk = "\"I know,\" said Sarah."
        val result = matcher().match(chunk)
        assertEquals(2, result.spans.size)
        // Applying a returned span to the chunk selects that span's text exactly.
        assertEquals("\"I know,\"", chunk.substring(result.spans[0].start, result.spans[0].end))
        assertEquals(" said Sarah.", chunk.substring(result.spans[1].start, result.spans[1].end))
    }

    @Test
    fun `every span of every chunk in the captured trace is inside that chunk`() {
        // The real shape, from the QUI-020 capture: clause-level splits, leading spaces on
        // continuations, a heading and a page number the index does not contain.
        val dir = File("../../fixtures/host-traces")
        val labels = File(dir, "neoreader-epub-shape.labels.tsv").readLines()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { it.split('\t', limit = 2) }
        val entries = labels.mapIndexed { seq, (label, text) ->
            val speaker = label.takeIf { it != "NARRATION" }
            IndexEntry(seq, text, Normalizer.normalize(text), spansOf(text, speaker))
        }
        val matcher = Matcher(InMemoryBookIndex("scene", entries))

        val chunks = File(dir, "neoreader-epub-shape.tsv").readLines().drop(1)
            .filter { it.isNotBlank() }.map { it.substringAfterLast('\t') }

        var voiced = 0
        for (chunk in chunks) {
            for (span in matcher.match(chunk).spans) {
                assertTrue(
                    span.start >= 0 && span.end <= chunk.length && span.start < span.end,
                    "span ${span.start}..${span.end} escapes chunk '$chunk' (${chunk.length})",
                )
                voiced++
            }
        }
        assertTrue(voiced > 0, "the trace produced no spans at all")
    }

    /** Quote marks are readable offline, on a whole sentence, which is the writer's job. */
    private fun spansOf(text: String, speaker: String?): List<VoiceSpan> {
        if (speaker == null) return listOf(VoiceSpan(0, text.length, Kind.NARRATION, null, 1.0))
        val out = mutableListOf<VoiceSpan>()
        var start = 0
        var inQuote = false
        text.forEachIndexed { i, c ->
            if (c == '"') {
                if (inQuote) { out += VoiceSpan(start, i + 1, Kind.DIALOGUE, speaker, 1.0); start = i + 1 }
                else { if (i > start) out += VoiceSpan(start, i, Kind.NARRATION, null, 1.0); start = i }
                inQuote = !inQuote
            }
        }
        if (start < text.length) {
            out += VoiceSpan(start, text.length,
                if (inQuote) Kind.DIALOGUE else Kind.NARRATION, if (inQuote) speaker else null, 1.0)
        }
        return out
    }
}
