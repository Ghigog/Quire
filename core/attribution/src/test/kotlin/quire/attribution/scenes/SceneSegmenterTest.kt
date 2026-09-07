package quire.attribution.scenes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.model.Paragraph

class SceneSegmenterTest {

    private fun paragraphs(vararg rows: Pair<Int, String>): List<Paragraph> =
        rows.mapIndexed { i, (chapter, text) -> Paragraph("b#p$i", text, chapter, i) }

    @Test
    fun `a chapter with no break markup is one scene`() {
        val paras = paragraphs(
            0 to "Sarah walked into the room.",
            0 to "\"Hello,\" she said.",
            0 to "Thomas looked up.",
        )
        val scenes = SceneSegmenter.segment(paras)
        assertEquals(listOf(Scene(0, 3)), scenes)
    }

    @Test
    fun `chapters are never merged`() {
        val paras = paragraphs(
            0 to "The end of the chapter.",
            1 to "The start of the next one, with no break marker at all.",
        )
        val scenes = SceneSegmenter.segment(paras)
        assertEquals(listOf(Scene(0, 1), Scene(1, 2)), scenes)
        for (scene in scenes) {
            val chapters = paras.filter { it.index in scene }.map { it.chapterIndex }.distinct()
            assertEquals(1, chapters.size, "scene $scene spans chapters $chapters")
        }
    }

    @Test
    fun `an explicit scene break starts a new scene`() {
        val paras = paragraphs(
            0 to "\"You will miss it,\" said Ellen.",
            0 to "*        *        *        *        *",
            0 to "The following morning was fine.",
        )
        val scenes = SceneSegmenter.segment(paras)
        assertEquals(listOf(Scene(0, 2), Scene(2, 3)), scenes)
    }

    @Test
    fun `a run of several break paragraphs is one boundary, not several`() {
        val paras = paragraphs(
            0 to "\"You will miss it,\" said Ellen.",
            0 to "* * *",
            0 to "---",
            0 to "The following morning was fine.",
        )
        val scenes = SceneSegmenter.segment(paras)
        assertEquals(listOf(Scene(0, 3), Scene(3, 4)), scenes)
    }

    @Test
    fun `a whitespace-only paragraph is a break too`() {
        val paras = paragraphs(
            0 to "\"You will miss it,\" said Ellen.",
            0 to "   ",
            0 to "The following morning was fine.",
        )
        val scenes = SceneSegmenter.segment(paras)
        assertEquals(listOf(Scene(0, 2), Scene(2, 3)), scenes)
    }

    @Test
    fun `an over-long chapter is cut by the backstop`() {
        val paras = paragraphs(*Array(10) { 0 to "Narration continues, paragraph $it." })
        val scenes = SceneSegmenter.segment(paras, maxParagraphs = 4)
        assertEquals(listOf(Scene(0, 4), Scene(4, 8), Scene(8, 10)), scenes)
    }

    @Test
    fun `segmentation is deterministic`() {
        val paras = paragraphs(
            0 to "Sarah walked into the room.",
            0 to "\"Hello,\" she said.",
            0 to "* * *",
            0 to "Later that day.",
            1 to "A new chapter begins.",
        )
        assertEquals(SceneSegmenter.segment(paras), SceneSegmenter.segment(paras))
    }

    @Test
    fun `every paragraph belongs to exactly one scene`() {
        val paras = paragraphs(
            0 to "One.", 0 to "* * *", 0 to "Two.", 1 to "Three.", 1 to "Four.",
        )
        val scenes = SceneSegmenter.segment(paras)
        val covered = scenes.flatMap { it.start until it.endExclusive }
        assertEquals(paras.map { it.index }, covered)
        assertTrue(scenes.zipWithNext().all { (a, b) -> a.endExclusive == b.start })
    }

    @Test
    fun `an empty book has no scenes`() {
        assertEquals(emptyList(), SceneSegmenter.segment(emptyList()))
    }
}
