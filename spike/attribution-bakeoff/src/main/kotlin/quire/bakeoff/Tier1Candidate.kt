package quire.bakeoff

import quire.attribution.Heuristic
import quire.attribution.Roster
import quire.attribution.Segmenter
import quire.model.Kind

/**
 * The baseline: `core:attribution`'s Tier 1, exactly as it ships (QUI-008).
 *
 * It is the number every other candidate has to beat, and it is also the floor — Tier 1
 * runs before any model whatever the bake-off decides, so a candidate is really being
 * asked what it adds to this.
 *
 * Both rules that trade precision for coverage are switchable, because the only fair way
 * to say what a rule bought is to score it against its own absence on identical text.
 */
class Tier1Candidate(
    private val pronouns: Boolean = true,
    private val actionBeats: Boolean = true,
) : Candidate {

    override val id = "tier1" +
        (if (!pronouns) "-nopronouns" else "") +
        (if (!actionBeats) "-nobeats" else "")

    override val description =
        "core:attribution Heuristic, roster bootstrapped from the book itself, " +
            "pronouns=${on(pronouns)}, action beats=${on(actionBeats)}"

    private fun on(b: Boolean) = if (b) "on" else "off"

    override fun answer(novel: Corpus.Novel): Map<String, Answer> {
        val paragraphs = novel.paragraphs.map { it.locator to it.text }
        val cast = Roster.scan(paragraphs)
        val manifest = Roster.manifest(cast, bookId = novel.meta.folder, generatedAt = 0L)
        val heuristic = Heuristic(manifest, pronouns = pronouns, actionBeats = actionBeats)

        // Attribute the book once, then index the dialogue spans by paragraph so a question
        // is answered by a lookup rather than by re-running the rules per quotation.
        val byParagraph = HashMap<Int, List<Span>>(novel.paragraphs.size)
        for (paragraph in novel.paragraphs) {
            val segments = Segmenter.segment(paragraph.locator, paragraph.text)
            if (segments.isEmpty()) continue
            val results = heuristic.attribute(paragraph.locator, paragraph.text)
            // Segmenter trims before splitting, so a segment's offsets are relative to the
            // trimmed paragraph; add the leading whitespace back to land in the real one.
            val lead = paragraph.text.length - paragraph.text.trimStart().length
            val trimmed = paragraph.text.trim()
            byParagraph[paragraph.index] = segments.zip(results) { segment, result ->
                Span(
                    start = lead + segment.before.length,
                    end = lead + trimmed.length - segment.after.length,
                    dialogue = segment.kind == Kind.DIALOGUE,
                    speaker = result.speakerId,
                    evidence = result.evidence,
                )
            }
        }

        return novel.questions.associate { question ->
            question.id to (byParagraph[question.paragraph]
                ?.filter { it.dialogue }
                ?.maxByOrNull { it.overlap(question.start, question.end) }
                ?.takeIf { it.overlap(question.start, question.end) > 0 }
                ?.let { Answer(it.speaker, it.evidence) }
            // No quoted span covers the gold quotation: our segmenter did not see speech
            // here, so on device this line reads in the narrator's voice. That is a miss,
            // not a gap in the harness, and it is counted as one.
                ?: Answer(null, "no dialogue segment"))
        }
    }

    private data class Span(
        val start: Int,
        val end: Int,
        val dialogue: Boolean,
        val speaker: String?,
        val evidence: String,
    ) {
        fun overlap(from: Int, to: Int) = (minOf(end, to) - maxOf(start, from)).coerceAtLeast(0)
    }
}
