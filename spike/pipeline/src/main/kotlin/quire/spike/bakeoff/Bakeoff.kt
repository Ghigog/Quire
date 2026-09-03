package quire.spike.bakeoff

import java.io.File
import quire.spike.Pdnc

/**
 * The one place a candidate is scored against PDNC (QUI-028).
 *
 * **Coverage, precision and accuracy are three numbers, not one**, because they trade against
 * each other and the tradeoff is the actual decision. Coverage is the share of quotations a
 * candidate answered at all; precision, of those, how many named the right speaker; accuracy,
 * correct answers over every quotation asked. A candidate answering a tenth of the book
 * perfectly and one answering all of it half-right score the same accuracy and are not
 * remotely the same product.
 *
 * The denominator is every scorable gold quotation, including the ones the candidate never
 * saw — a quotation it cannot answer counts against it instead of vanishing.
 */
object Bakeoff {

    data class Mistake(
        val questionId: String,
        val type: String,
        val gold: String,
        val predicted: String,
        val quote: String,
    )

    data class NovelScore(
        val meta: Pdnc.NovelMeta,
        val byType: Map<String, Pdnc.Tally>,
        val byEvidence: Map<String, Pdnc.Tally>,
        val mistakes: List<Mistake>,
        /** Quotations whose speaker is a PDNC pseudo-entity (`_group`, `_unknowable`). */
        val unscorable: Int,
        /** Quotations whose byte span fell outside every paragraph. Should be 0. */
        val unlocatable: Int,
    ) {
        val total: Pdnc.Tally get() = Pdnc.Tally().also { t -> byType.values.forEach { t += it } }
    }

    /** Turn a novel's gold quotations into the questions every candidate is asked. */
    fun questions(paragraphs: List<Pdnc.Located>, gold: List<Pdnc.Gold>): Pair<List<Question>, Int> {
        val out = mutableListOf<Question>()
        var unlocatable = 0
        for (g in gold) {
            if (!g.scorable) continue
            val at = Pdnc.locateIn(paragraphs, g)
            if (at == null) { unlocatable++; continue }
            val (paragraph, start, end) = at
            out += Question(g.id, paragraph, start, end, g.type, g.speaker, g.text)
        }
        return out to unlocatable
    }

    fun score(
        novelDir: File,
        candidate: Candidate,
        meta: Pdnc.NovelMeta = bare(novelDir),
        keepMistakes: Int = 12,
    ): NovelScore {
        val (paragraphs, gold) = Pdnc.locate(novelDir)
        val (questions, unlocatable) = questions(paragraphs, gold)
        val answers = candidate.answer(paragraphs.map { it.unit }, questions)
        return tally(questions, answers, keepMistakes)
            .copy(meta = meta, unscorable = gold.count { !it.scorable }, unlocatable = unlocatable)
    }

    /** The tallying, apart from the corpus, so it can be tested without one. */
    fun tally(questions: List<Question>, answers: Map<String, Answer>, keepMistakes: Int = 12): NovelScore {
        val byType = linkedMapOf<String, Pdnc.Tally>()
        val byEvidence = linkedMapOf<String, Pdnc.Tally>()
        val mistakes = mutableListOf<Mistake>()

        for (question in questions) {
            val answer = answers[question.id] ?: Answer(null, "unanswered")
            val type = byType.getOrPut(question.type) { Pdnc.Tally() }
            val evidence = byEvidence.getOrPut(answer.evidence.ifEmpty { "none" }) { Pdnc.Tally() }
            type.scored++; evidence.scored++
            val predicted = answer.speaker ?: continue
            type.attributed++; evidence.attributed++
            if (Pdnc.matches(predicted, question.gold)) {
                type.correct++; evidence.correct++
            } else if (mistakes.size < keepMistakes) {
                mistakes += Mistake(question.id, question.type, question.gold, predicted, question.text.take(60))
            }
        }
        return NovelScore(bare(File(".")), byType, byEvidence, mistakes, 0, 0)
    }

    /** For callers holding a directory but no index row — the `pdnc` command's path. */
    private fun bare(dir: File) = Pdnc.NovelMeta(dir.name, dir.name, 3, false, "unknown", 0)
}
