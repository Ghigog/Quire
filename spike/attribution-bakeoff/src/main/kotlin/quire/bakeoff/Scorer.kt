package quire.bakeoff

/**
 * Turns answers into the three numbers the decision needs, split by quotation type.
 *
 * **Coverage, precision and accuracy are reported separately** because they trade against
 * each other and the tradeoff is the actual decision. Coverage is the share of quotations a
 * candidate answered at all; precision, of those, how many named the right speaker;
 * accuracy, correct answers over every quotation asked. A candidate that answers a tenth of
 * the book perfectly and a candidate that answers all of it half-right have the same
 * accuracy and are not remotely the same product.
 *
 * The denominator is every scorable gold quotation, including the ones the candidate never
 * saw. That is the change from QUI-018's pass, which scored only quotations it could match
 * by text and so reported 58.5% precision over 2,846 of 37,131.
 */
object Scorer {

    data class Tally(var scored: Int = 0, var attributed: Int = 0, var correct: Int = 0) {
        val coverage get() = if (scored == 0) 0.0 else attributed * 100.0 / scored
        val precision get() = if (attributed == 0) 0.0 else correct * 100.0 / attributed
        val accuracy get() = if (scored == 0) 0.0 else correct * 100.0 / scored

        operator fun plusAssign(other: Tally) {
            scored += other.scored; attributed += other.attributed; correct += other.correct
        }
    }

    /** One candidate on one novel: tallies by quotation type, plus what went wrong. */
    data class NovelScore(
        val meta: Corpus.NovelMeta,
        val byType: Map<String, Tally>,
        val byEvidence: Map<String, Tally>,
        val mistakes: List<Mistake>,
    ) {
        val total: Tally get() = Tally().also { t -> byType.values.forEach { t += it } }
    }

    data class Mistake(val questionId: String, val type: String, val gold: String, val predicted: String, val quote: String)

    fun score(novel: Corpus.Novel, candidate: Candidate, keepMistakes: Int = 12): NovelScore {
        val answers = candidate.answer(novel)
        val byType = linkedMapOf<String, Tally>()
        val byEvidence = linkedMapOf<String, Tally>()
        val mistakes = mutableListOf<Mistake>()

        for (question in novel.questions) {
            val answer = answers[question.id] ?: Answer(null, "unanswered")
            val type = byType.getOrPut(question.type) { Tally() }
            val evidence = byEvidence.getOrPut(answer.evidence.ifEmpty { "none" }) { Tally() }
            type.scored++; evidence.scored++
            val predicted = answer.speaker ?: continue
            type.attributed++; evidence.attributed++
            if (sameSpeaker(predicted, question.gold)) {
                type.correct++; evidence.correct++
            } else if (mistakes.size < keepMistakes) {
                mistakes += Mistake(question.id, question.type, question.gold, predicted, question.text.take(60))
            }
        }
        return NovelScore(novel.meta, byType, byEvidence, mistakes)
    }

    /**
     * PDNC names a character canonically ("Mr Elton", "The General"); a candidate returns
     * whatever the book called them at that moment ("Elton"). Count a match when one name's
     * words contain the other's.
     *
     * This is deliberately generous, and it flatters every candidate equally, which is what
     * matters for a comparison. Alias resolution proper is QUI-007's job, and until it
     * exists a stricter rule would measure our lack of it rather than the model.
     */
    fun sameSpeaker(predicted: String, gold: String): Boolean {
        val p = words(predicted)
        val g = words(gold)
        if (p.isEmpty() || g.isEmpty()) return false
        return p == g || p.containsAll(g) || g.containsAll(p)
    }

    /**
     * Honorifics are kept on purpose. Folding them away would make "Mr Bennet" and
     * "Mrs Bennet" the same person, and Austen is a fifth of this corpus; subset matching
     * already lets "Elton" match "Mr Elton" without that cost.
     */
    private fun words(name: String) = name.lowercase()
        .replace(Regex("[^a-z ]"), " ")
        .split(' ')
        .filter { it.isNotBlank() }
        .toSet()
}
