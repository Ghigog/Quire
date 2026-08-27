package quire.spike

import java.io.File

/** A labelled fixture paragraph: the text, plus the correct speaker of each quoted span. */
data class LabelledParagraph(val unit: ParagraphUnit, val gold: List<String>)

object Fixture {
    const val NARRATION_LABEL = "NARRATION"
    const val UNKNOWN_LABEL = "UNKNOWN"

    fun load(file: File): List<LabelledParagraph> =
        file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapIndexed { i, line ->
                val (label, text) = line.split('\t', limit = 2).let {
                    require(it.size == 2) { "${file.name}:${i + 1} has no tab separator" }
                    it[0].trim() to it[1].trim()
                }
                LabelledParagraph(
                    unit = ParagraphUnit(
                        locator = "${file.nameWithoutExtension}#p$i",
                        text = text,
                        chapterIndex = 0,
                        index = i,
                    ),
                    gold = if (label == NARRATION_LABEL) emptyList() else label.split('|').map { it.trim() },
                )
            }
}

/** Tier 1's score on a fixture set. Coverage and precision are reported separately
 *  because they trade against each other, and the tradeoff is the actual decision. */
data class Score(
    var scored: Int = 0,
    var attributed: Int = 0,
    var correct: Int = 0,
    val byEvidence: MutableMap<String, IntArray> = linkedMapOf(), // evidence -> [attributed, correct]
    val mistakes: MutableList<String> = mutableListOf(),
) {
    val coverage get() = if (scored == 0) 0.0 else attributed.toDouble() / scored
    val precision get() = if (attributed == 0) 0.0 else correct.toDouble() / attributed
    val accuracy get() = if (scored == 0) 0.0 else correct.toDouble() / scored
}

object Scorer {

    fun score(labelled: List<LabelledParagraph>): Score {
        val paragraphs = labelled.map { it.unit }
        val roster = Tier1.bootstrapRoster(paragraphs).names
        val score = Score()

        for (lp in labelled) {
            val predictions = Tier1.attribute(listOf(lp.unit), roster)
                .filter { it.kind == Kind.DIALOGUE }

            predictions.forEachIndexed { i, pred ->
                val gold = lp.gold.getOrNull(i) ?: return@forEachIndexed
                if (gold == Fixture.UNKNOWN_LABEL) return@forEachIndexed
                score.scored++
                if (pred.speakerId == null) return@forEachIndexed
                score.attributed++
                val bucket = score.byEvidence.getOrPut(pred.evidence) { IntArray(2) }
                bucket[0]++
                if (matches(pred.speakerId, gold)) {
                    score.correct++
                    bucket[1]++
                } else {
                    score.mistakes += "${pred.locator}: predicted ${pred.speakerId}, gold $gold " +
                        "(${pred.evidence}) — \"${pred.text.take(48)}\""
                }
            }
        }
        return score
    }

    /**
     * Gold labels are written the way a reader would name the character; the roster holds
     * whatever form appeared in the tag. Treat one as matching if either contains the
     * other as a whole word, so "Mr Ashcombe" matches "Ashcombe". Proper alias merging is
     * QUI-007's job, not Tier 1's.
     */
    private fun matches(predicted: String, gold: String): Boolean {
        val p = predicted.lowercase().split(' ').toSet()
        val g = gold.lowercase().split(' ').toSet()
        return p == g || p.containsAll(g) || g.containsAll(p)
    }
}
