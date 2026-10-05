package quire.desktop

import java.io.File
import quire.attribution.Conversation
import quire.attribution.Heuristic
import quire.attribution.scan.BookScan
import quire.epub.EpubText
import quire.model.AttributionResult
import quire.model.Paragraph
import quire.model.characters.CharacterManifest
import quire.voice.foundry.QualityList
import quire.voice.foundry.SpeakerProfile

/**
 * QUI-046: the whole import, on the desktop, in the order the app runs it.
 *
 * This is the same sequence `app:companion`'s `ImportService` drives — cast first, then
 * Tier 1, then turn-taking — and it is deliberately the same code rather than a copy of it:
 * `EpubText`, `BookScan`, `Heuristic` and `Conversation` are the real modules.
 *
 * **No SLM.** `BookScan(null)` and no `SceneAttributor`, exactly as `ImportService` ships
 * today (`slmRuntime()` returns null, because QUI-031 has not picked a runtime). So an
 * unresolved line stays unresolved and is voiced by the narrator. That is the product's
 * current, documented behaviour, not a shortcut taken here — and `cast` prints the coverage
 * so the size of that gap is visible rather than assumed.
 */
class BookReader {

    data class Read(
        val manifest: CharacterManifest,
        val paragraphs: List<Paragraph>,
        val segments: List<AttributionResult>,
    )

    fun read(epub: File, bookId: String = epub.nameWithoutExtension): Read {
        require(epub.isFile) { "no such file: ${epub.path}" }
        val paragraphs = EpubText.paragraphs(epub)
        require(paragraphs.isNotEmpty()) { "no text found in ${epub.name} — is it an EPUB?" }

        val manifest = BookScan(slm = null).scan(
            paragraphs = paragraphs,
            bookId = bookId,
            generatedAt = System.currentTimeMillis(),
        )
        val tier1 = Heuristic(manifest).attributeAll(paragraphs.map { it.locator to it.text })
        val withTurns = Conversation.resolve(tier1, cast = manifest.characters.map { it.id })

        return Read(manifest, paragraphs, withTurns)
    }

    companion object {

        /**
         * [read]'s segments for one chapter, or all of them.
         *
         * Segment locators are `spine#p12#s1`; the paragraph half of that is what carries
         * the chapter, so this strips the sentence suffix and looks the paragraph up.
         */
        fun segmentsFor(read: Read, chapter: Int?): List<AttributionResult> {
            if (chapter == null) return read.segments
            val chapterOf = read.paragraphs.associate { it.locator to it.chapterIndex }
            return read.segments.filter { chapterOf[paragraphOf(it.locator)] == chapter }
        }

        private fun paragraphOf(segmentLocator: String) = segmentLocator.substringBeforeLast("#s")

        /** Chapters in reading order, for `--list` and for validating a `--chapter` value. */
        fun chapters(read: Read): List<Int> =
            read.paragraphs.map { it.chapterIndex }.distinct().sorted()
    }
}
