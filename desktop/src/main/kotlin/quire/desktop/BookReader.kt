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
        val withTurns = turnTakingByChapter(
            tier1 = tier1,
            chapterOfParagraph = paragraphs.associate { it.locator to it.chapterIndex },
            cast = manifest.characters.map { it.id },
        )

        return Read(manifest, paragraphs, withTurns)
    }

    companion object {

        /**
         * Turn-taking runs **within each chapter**, never across the whole book.
         *
         * ADR-0006 makes turn-taking a property of the scene, not of the line — "who spoke
         * last, who was addressed, who has been silent since they entered". A chapter is the
         * coarsest scene boundary a book gives us, and the rule's own reset is weaker than a
         * chapter: [Conversation.MAX_GAP_PARAGRAPHS] is two paragraphs of narration, so a
         * chapter opening with a heading and one line of prose carries straight over.
         *
         * That is not hypothetical. In *The Sign of the Four*, Chapter II opens
         * `"I have come to you, Mr. Holmes," she said`, and Holmes was the last speaker
         * tagged in Chapter I — so the whole-book pass gave one of Miss Morstan's lines to
         * Holmes. A wrong voice is the failure PRD §3.1 prices highest, and a chapter
         * boundary is the cheapest place to stop making it.
         *
         * The cast still comes from the whole book, and Tier 1 still runs over all of it:
         * a name is only stable if attribution has seen every chapter a character appears
         * in. Only the *exchange state* — the floor, the pair, the pending revert — resets.
         */
        internal fun turnTakingByChapter(
            tier1: List<AttributionResult>,
            chapterOfParagraph: Map<String, Int>,
            cast: List<String>,
        ): List<AttributionResult> = tier1
            .groupBy { chapterOfParagraph[paragraphOf(it.locator)] ?: -1 }
            .values
            .flatMap { Conversation.resolve(it, cast = cast) }

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
