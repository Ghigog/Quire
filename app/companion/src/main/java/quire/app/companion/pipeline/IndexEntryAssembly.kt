package quire.app.companion.pipeline

import quire.index.Normalizer
import quire.model.AttributionResult
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.Paragraph
import quire.model.VoiceSpan

/**
 * Folds attributed segments onto the sentences `dialogue_index.db` stores.
 *
 * Segments are runs within a paragraph; sentences are what a reader's app sends (QUI-021).
 * Each segment is located by walking the paragraph forward, because a short line of speech
 * can occur twice in one paragraph and only the first unconsumed one is this segment. Ported
 * from the equivalent private function in `spike/ttsbinding`'s `BookImport` — that copy is a
 * throwaway harness and never a dependency (CLAUDE.md §3); this is the one production code
 * calls.
 *
 * [seq] on every returned entry is local to this call (0-based within [paragraphs]) — the
 * caller renumbers wholesale once every scene's entries are collected, because
 * [ImportPipeline] calls this once per scene and a book-wide sequence isn't known until then.
 */
object IndexEntryAssembly {

    fun assemble(paragraphs: List<Paragraph>, attributed: List<AttributionResult>): List<IndexEntry> {
        val byParagraph = attributed.groupBy { it.locator.substringBeforeLast("#s") }
        val out = mutableListOf<IndexEntry>()
        for (paragraph in paragraphs) {
            val segments = byParagraph[paragraph.locator].orEmpty()
            var cursor = 0
            val placed = segments.mapNotNull { segment ->
                val needle = segment.text.trim()
                if (needle.isEmpty()) return@mapNotNull null
                val at = paragraph.text.indexOf(needle, cursor)
                if (at < 0) return@mapNotNull null
                cursor = at + needle.length
                Triple(at, at + needle.length, segment)
            }

            var offset = 0
            for (sentence in Normalizer.sentences(paragraph.text)) {
                val start = paragraph.text.indexOf(sentence, offset).takeIf { it >= 0 } ?: offset
                val end = start + sentence.length
                offset = end
                val spans = placed.mapNotNull { (from, to, segment) ->
                    val lo = maxOf(from, start)
                    val hi = minOf(to, end)
                    if (lo >= hi) null
                    else VoiceSpan(lo - start, hi - start, segment.kind, segment.speakerId, segment.confidence)
                }.ifEmpty { listOf(VoiceSpan(0, sentence.length, Kind.NARRATION, null, 0.0)) }

                out += IndexEntry(out.size, sentence, Normalizer.normalize(sentence), spans, paragraph.chapterIndex)
            }
        }
        return out
    }
}
