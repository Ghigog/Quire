package quire.spike.bakeoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import quire.spike.ParagraphUnit

class SceneReportTest {

    @Test
    fun `recognises the heading shapes PDNC's novels actually use`() {
        assertTrue(SceneReport.looksLikeChapterHeading("CHAPTER I"))
        assertTrue(SceneReport.looksLikeChapterHeading("CHAPTER I."))
        assertTrue(SceneReport.looksLikeChapterHeading("Chapter 1"))
        // Dickens-style: number and subtitle run into one paragraph with no blank line
        // between them in the source (OliverTwist), so the match cannot require the whole
        // paragraph to be short.
        assertTrue(SceneReport.looksLikeChapterHeading(
            "CHAPTER I. TREATS OF THE PLACE WHERE OLIVER TWIST WAS BORN"))
        assertTrue(SceneReport.looksLikeChapterHeading("VOLUME I"))
        assertTrue(SceneReport.looksLikeChapterHeading("BOOK THE FIRST"))
        assertTrue(SceneReport.looksLikeChapterHeading("PART ONE"))
        assertTrue(SceneReport.looksLikeChapterHeading("IV"))
        assertTrue(SceneReport.looksLikeChapterHeading("12"))
    }

    @Test
    fun `does not fire on ordinary prose`() {
        assertFalse(SceneReport.looksLikeChapterHeading("\"You will miss it,\" said Ellen."))
        assertFalse(SceneReport.looksLikeChapterHeading("She had read every chapter of the book twice."))
        assertFalse(SceneReport.looksLikeChapterHeading(""))
    }

    @Test
    fun `reconstructs chapters that increment on each heading paragraph`() {
        val paragraphs = listOf(
            ParagraphUnit("n#p0", "CHAPTER I", 0, 0),
            ParagraphUnit("n#p1", "Sarah walked in.", 0, 1),
            ParagraphUnit("n#p2", "CHAPTER II", 0, 2),
            ParagraphUnit("n#p3", "Thomas looked up.", 0, 3),
        )
        val chapters = SceneReport.reconstructChapters(paragraphs).map { it.chapterIndex }
        assertEquals(listOf(0, 0, 1, 1), chapters)
    }
}
