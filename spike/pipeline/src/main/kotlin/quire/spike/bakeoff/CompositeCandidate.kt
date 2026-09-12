package quire.spike.bakeoff

import quire.spike.ParagraphUnit

/**
 * Two candidates in the order a reader would want them: the precise one first, the broad one
 * on whatever it declines (QUI-041).
 *
 * This is not a convenience. Every figure anyone has quoted for "Tier 1 plus an encoder" so
 * far has been **arithmetic over per-type rows** — coverage and precision added up by hand
 * from a table split on PDNC's `quoteType`. Two things are wrong with that. `quoteType` is a
 * gold label a device will never have, so a composite routed on it cannot be built; and
 * adding rows assumes the two candidates disagree only where the table says they do, which
 * nothing has checked. Composing the candidates and scoring the result through [Bakeoff]
 * makes the number measured rather than derived, and it routes on the only thing that is
 * available at run time: whether the first candidate answered.
 *
 * Evidence labels pass through untouched, so the report's "By evidence" table says which half
 * answered each quotation and at what precision. That is the whole decision: if the fallback
 * is spending more wrong voices than it buys right ones, it shows up there as its own row.
 */
class CompositeCandidate(
    private val first: Candidate,
    private val second: Candidate,
) : Candidate {

    override val id = "${first.id}+${second.id}"

    override val description: String
        get() = "${first.id} first (${first.description}); " +
            "${second.id} on what it declines (${second.description})"

    override fun answer(
        paragraphs: List<ParagraphUnit>,
        questions: List<Question>,
    ): Map<String, Answer> {
        val primary = first.answer(paragraphs, questions)
        val fallback = second.answer(paragraphs, questions)
        return buildMap {
            for (question in questions) {
                // A null speaker is a decline, not an answer, and has to fall through — the
                // map can hold one either way, and treating a present key as an answer would
                // silently give the fallback nothing to do.
                val answer = primary[question.id]?.takeIf { it.speaker != null }
                    ?: fallback[question.id]
                    ?: continue
                put(question.id, answer)
            }
        }
    }
}
