package quire.spike.slice

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.index.InMemoryBookIndex
import quire.index.Matcher
import quire.index.Normalizer
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

/**
 * The slice's book, read the way a host reads it.
 *
 * `fixtures/slice/chapter-one.labels.tsv` generates both the EPUB the reader opens and the
 * index the service reads, so this test asks the only question left: when the chapter
 * arrives clause by clause, with no quote marks in most chunks, does every line come out
 * in the right voice?
 */
class ChapterOneTest {

    private val fixture = File("../../fixtures/slice/chapter-one.labels.tsv")

    /**
     * Split a sentence the way NeoReader does — at commas, with the separator staying on
     * the front of the continuation, which is where the leading spaces in the captured
     * trace come from (`fixtures/host-traces/neoreader-epub-shape.tsv`).
     */
    private fun hostChunks(text: String): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        text.forEachIndexed { i, c ->
            if (c == ',' && i + 1 < text.length) {
                out += text.substring(start, i + 1)
                start = i + 1
            }
        }
        out += text.substring(start)
        return out.filter { it.isNotBlank() }
    }

    private fun rows() = fixture.readLines()
        .filterNot { it.isBlank() || it.startsWith("#") }
        .map { it.split('\t', limit = 2) }
        .map { (label, text) -> (if (label == "NARRATION") null else label) to text }

    private fun book(): List<IndexEntry> =
        rows().mapIndexed { seq, (speaker, text) ->
            IndexEntry(seq, text, Normalizer.normalize(text), spansOf(text, speaker))
        }

    /** Same rule the indexer applies: quote marks are readable here, offline, in full. */
    private fun spansOf(text: String, speaker: String?): List<VoiceSpan> {
        if (speaker == null) return listOf(VoiceSpan(0, text.length, Kind.NARRATION, null, 1.0))
        val out = mutableListOf<VoiceSpan>()
        var runStart = 0
        var inQuote = false
        text.forEachIndexed { i, c ->
            if (c == '"') {
                if (inQuote) {
                    out += VoiceSpan(runStart, i + 1, Kind.DIALOGUE, speaker, 1.0)
                    runStart = i + 1
                } else {
                    if (i > runStart) out += VoiceSpan(runStart, i, Kind.NARRATION, null, 1.0)
                    runStart = i
                }
                inQuote = !inQuote
            }
        }
        if (runStart < text.length) {
            out += VoiceSpan(runStart, text.length, if (inQuote) Kind.DIALOGUE else Kind.NARRATION,
                if (inQuote) speaker else null, 1.0)
        }
        return out
    }

    private fun read(): List<Pair<String, List<Segment>>> {
        val entries = book()
        val casting = Casting(
            entries.flatMap { e -> e.spans.mapNotNull { it.speakerId } },
            voiceCount = 904,
        )
        val matcher = Matcher(InMemoryBookIndex("chapter-one", entries))
        return entries.flatMap { hostChunks(it.text) }
            .map { chunk -> chunk to ChunkPlan.of(chunk, matcher.match(chunk), casting) }
    }

    @Test
    fun `the whole chapter places in the index`() {
        val unplaced = read().filter { (_, segments) -> segments.isEmpty() }
        assertTrue(unplaced.isEmpty(), "unplaced: ${unplaced.map { it.first }}")
    }

    @Test
    fun `three distinct voices are used, and the narrator is not one of the characters`() {
        val voices = read().flatMap { it.second }
        val speakers = voices.mapNotNull { it.speakerId }.distinct().sorted()
        assertEquals(listOf("Sarah", "Thomas"), speakers)

        val narratorVoice = voices.first { it.speakerId == null }.voice
        val characterVoices = voices.filter { it.speakerId != null }.map { it.voice }.distinct()
        assertEquals(2, characterVoices.size)
        assertTrue(narratorVoice !in characterVoices)
    }

    @Test
    fun `dialogue clauses carrying no quote mark still get their speaker`() {
        val quoteless = read()
            .filter { (chunk, _) -> '"' !in chunk }
            .filter { (_, segments) -> segments.any { it.speakerId != null } }

        // These are the chunks that defeat quote inference entirely: a clause in the
        // middle of a line of speech, with nothing in it to say it is speech at all.
        assertTrue(quoteless.isNotEmpty(), "expected quoteless dialogue clauses in the chapter")
        for ((chunk, segments) in quoteless) {
            assertTrue(
                segments.any { it.speakerId != null },
                "lost the speaker on: '$chunk'",
            )
        }
    }

    @Test
    fun `a line with speech and a tag switches voice inside one chunk`() {
        // "I have been thinking," Thomas replied, without turning from the desk.
        val line = read().first { (chunk, _) -> chunk.startsWith("\"I have been thinking,") }
        assertEquals("Thomas", line.second.first().speakerId)
    }
}
