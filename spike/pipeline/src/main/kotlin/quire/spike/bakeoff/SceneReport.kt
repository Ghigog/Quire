package quire.spike.bakeoff

import java.io.File
import kotlin.math.ceil
import quire.attribution.scenes.Scene
import quire.attribution.scenes.SceneSegmenter
import quire.attribution.scenes.SceneSplitter
import quire.spike.Pdnc
import quire.spike.ParagraphUnit

/**
 * The first real count of what a novel contains (QUI-038): scenes per novel, quotations
 * per scene, and the share of scenes too long for a 2,048-token prompt. ADR-0006 guessed
 * 60-120 scenes per novel from arithmetic alone; nobody had counted until this.
 *
 * Reuses `bakeoff dump`'s own machinery — [Pdnc.locate] for paragraphs and
 * [Bakeoff.questions] for where each gold quotation lands — so this is a report over data
 * the corpus loader already produces, not a new harness.
 *
 * **PDNC's loader carries no chapter boundaries.** [Pdnc.locate] gives every paragraph
 * `chapterIndex = 0` (see its `Located` construction) because the corpus is one flat
 * `novel_text.txt` per novel with no structural marker the loader parses into a chapter
 * number — unlike a real import, where [quire.epub.EpubText] sets `chapterIndex` from the
 * EPUB spine. Feeding [SceneSegmenter] that as-is undercounts badly: most of these novels
 * carry no scene-break markup inside a chapter at all, so without a chapter signal several
 * of them come back as 2-3 "scenes" for the whole book. [reconstructChapters] recovers a
 * stand-in signal by recognising a paragraph that is nothing but a heading — `CHAPTER I`,
 * `Book II`, a bare roman numeral — the same way a reader's eye does, and treating it as the
 * start of a new chapter. **This is report-side data preparation, not part of the shipped
 * segmenter**: [SceneSegmenter] itself only ever reads the `chapterIndex` it is given, and a
 * real import never needs this reconstruction because the EPUB spine already carries it.
 * The regex is not exhaustive, so treat the counts below as closer to real than the naive
 * ones, not as exact.
 */
object SceneReport {

    /** ~4 characters per token for English prose. There is no on-device tokenizer to
     * measure against in this environment — the SLM runtime is Android-only (CLAUDE.md
     * §9) — so this is the standard rule-of-thumb approximation, not a measurement. */
    fun estimateTokens(text: String): Int = ceil(text.length / 4.0).toInt()

    data class NovelScenes(
        val folder: String,
        val paragraphs: Int,
        val scenes: List<Scene>,
        val quotationsPerScene: List<Int>,
        val tokensPerScene: List<Int>,
        /** Pieces after splitting every over-budget scene, and how many open mid-exchange. */
        val pieces: Int = 0,
        val midExchange: Int = 0,
    ) {
        val overBudget get() = tokensPerScene.count { it > BUDGET }
    }

    const val BUDGET = 2048

    /**
     * A paragraph that opens a chapter/part/book heading — `CHAPTER I`, `Book II.`, a bare
     * roman or arabic numeral on its own. See the class doc: this stands in for the
     * `chapterIndex` a real EPUB import already carries.
     *
     * No length cap on the heading-word case: Dickens-style headings run the number and the
     * descriptive subtitle into one paragraph ("CHAPTER I. TREATS OF THE PLACE WHERE...")
     * when the source has no blank line between them, and capping the length missed every
     * one of those.
     */
    private val HEADING_WORD = Regex("^(chapter|book|part|volume)\\b", RegexOption.IGNORE_CASE)
    private val BARE_NUMERAL = Regex("^([ivxlcdm]+|\\d+)\\.?$", RegexOption.IGNORE_CASE)

    fun looksLikeChapterHeading(text: String): Boolean {
        val t = text.trim()
        return t.isNotEmpty() && (HEADING_WORD.containsMatchIn(t) || BARE_NUMERAL.matches(t))
    }

    /** [ParagraphUnit.chapterIndex] is 0 for every PDNC paragraph; this rebuilds a stand-in. */
    fun reconstructChapters(paragraphs: List<ParagraphUnit>): List<ParagraphUnit> {
        var headingsSeen = 0
        return paragraphs.map { p ->
            if (looksLikeChapterHeading(p.text)) headingsSeen++
            // The heading itself belongs to the chapter it opens, not the one before it —
            // front matter ahead of the first heading is chapter 0 along with that chapter.
            p.copy(chapterIndex = maxOf(0, headingsSeen - 1))
        }
    }

    fun run(root: File, only: Set<String>) {
        val index = Pdnc.index(root).filter { only.isEmpty() || it.folder in only }
        println("Scene segmentation over PDNC — QUI-038\n")
        println("%-30s %10s %8s %14s %14s".format(
            "novel", "paragraphs", "scenes", "quotes/scene", "over %d tok".format(BUDGET)))

        val results = mutableListOf<NovelScenes>()
        for (meta in index) {
            val dir = File(File(root, "data"), meta.folder)
            val (located, gold) = Pdnc.locate(dir)
            val paragraphs = reconstructChapters(located.map { it.unit })
            val scenes = SceneSegmenter.segment(paragraphs)
            val (questions, _) = Bakeoff.questions(located, gold)

            val quotationsPerScene = scenes.map { scene -> questions.count { it.paragraph in scene } }
            val tokensByParagraph = paragraphs.associate { it.index to estimateTokens(it.text) }
            val tokensPerScene = scenes.map { scene ->
                paragraphs.filter { it.index in scene }.sumOf { tokensByParagraph.getValue(it.index) }
            }

            // QUI-038 asks that no piece begin in the middle of an exchange. The splitter
            // falls back to cutting where the budget ran out when the scene holds no turn
            // boundary in range, so the criterion is only met as often as that fallback is
            // avoided. Nothing had run the splitter over the corpus to find out.
            var pieces = 0
            var midExchange = 0
            for ((i, scene) in scenes.withIndex()) {
                if (tokensPerScene[i] <= BUDGET) continue
                val split = SceneSplitter.split(
                    scene, paragraphs, BUDGET, { tokensByParagraph.getValue(it.index) })
                pieces += split.size
                midExchange += split.count { !it.atTurnBoundary }
            }

            val novel = NovelScenes(meta.folder, paragraphs.size, scenes, quotationsPerScene, tokensPerScene, pieces, midExchange)
            results += novel
            println("%-30s %10d %8d %14.1f %13.1f%%".format(
                meta.folder.take(30), novel.paragraphs, scenes.size,
                quotationsPerScene.average(), novel.overBudget * 100.0 / scenes.size))
        }

        val perNovelSceneCounts = results.map { it.scenes.size }
        val allQuotationsPerScene = results.flatMap { it.quotationsPerScene }
        val allScenes = results.sumOf { it.scenes.size }
        val allOverBudget = results.sumOf { it.overBudget }

        println()
        println("scenes per novel   min %d, median %.0f, max %d, mean %.1f".format(
            perNovelSceneCounts.min(), median(perNovelSceneCounts), perNovelSceneCounts.max(),
            perNovelSceneCounts.average()))
        println("quotations/scene   min %d, median %.0f, max %d, mean %.1f".format(
            allQuotationsPerScene.min(), median(allQuotationsPerScene), allQuotationsPerScene.max(),
            allQuotationsPerScene.average()))
        val pieces = results.sumOf { it.pieces }
        val mid = results.sumOf { it.midExchange }
        println("split pieces       %d, of which %d (%.1f%%) open mid-exchange".format(
            pieces, mid, if (pieces == 0) 0.0 else 100.0 * mid / pieces))
        println("scenes over %d tokens   %d / %d (%.1f%%)".format(
            BUDGET, allOverBudget, allScenes, allOverBudget * 100.0 / allScenes))
        println()
        println("ADR-0006 guessed 60-120 scenes per novel from arithmetic, unmeasured. See this")
        println("ticket's Worklog for whether that held. Chapter boundaries above are")
        println("reconstructed from heading-shaped paragraphs (see this file's class doc) since")
        println("PDNC's loader does not carry them; real EPUB chapterIndex needs no such guess.")
    }

    private fun median(values: List<Int>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid].toDouble()
    }
}
