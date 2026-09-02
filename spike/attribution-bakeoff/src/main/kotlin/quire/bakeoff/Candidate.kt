package quire.bakeoff

/**
 * One thing being measured: Tier 1, an attribution encoder, BookNLP, or the 1B SLM.
 *
 * Every candidate sees the whole novel and answers the same questions, so the comparison
 * is of models rather than of harnesses. A candidate that cannot answer a question returns
 * `null` — declining is a legitimate answer and the scorer reports it as coverage lost
 * rather than as a mistake, because a line nobody claimed is read by the narrator and a
 * line claimed wrongly is heard (PRD §3.1).
 */
interface Candidate {

    /** Short, stable, and used as a column heading. */
    val id: String

    /** What exactly was run, including switches. Printed above the table. */
    val description: String

    /** Answers by [Corpus.Question.id]. A missing key counts the same as a null answer. */
    fun answer(novel: Corpus.Novel): Map<String, Answer>
}

/** A candidate's answer: a speaker name as the candidate would call them, and why. */
data class Answer(val speaker: String?, val evidence: String = "")
