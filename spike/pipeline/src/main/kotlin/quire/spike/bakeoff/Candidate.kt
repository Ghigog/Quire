package quire.spike.bakeoff

import quire.spike.ParagraphUnit

/**
 * One thing being measured: Tier 1, an attribution encoder, BookNLP, or the 1B SLM (QUI-028).
 *
 * Every candidate sees the whole novel and answers the same questions, so the comparison is
 * of models rather than of scoring code. A candidate that cannot answer returns `null` —
 * declining is a legitimate answer and is scored as coverage lost rather than as a mistake,
 * because a line nobody claimed is read by the narrator and a line claimed wrongly is heard
 * (PRD §3.1).
 */
interface Candidate {

    /** Short, stable, and used as a column heading. */
    val id: String

    /** What exactly was run, including switches. Printed above the table. */
    val description: String

    /** Answers by [Question.id]. A missing key counts the same as a null answer. */
    fun answer(paragraphs: List<ParagraphUnit>, questions: List<Question>): Map<String, Answer>
}

/**
 * Who speaks the text at these offsets?
 *
 * [start] and [end] are character offsets **within the paragraph**, resolved from PDNC's byte
 * spans by [quire.spike.Pdnc.locateIn], so a candidate never has to reason about the file.
 */
data class Question(
    val id: String,
    val paragraph: Int,
    val start: Int,
    val end: Int,
    /** `Explicit`, `Implicit`, `Anaphoric`, or `Unspecified` for PDNC's 14 blank rows. */
    val type: String,
    val gold: String,
    /** What PDNC recorded, for diagnostics only. Answering by matching it would be cheating. */
    val text: String = "",
)

/** A candidate's answer: a speaker as the candidate would name them, and why. */
data class Answer(val speaker: String?, val evidence: String = "")
