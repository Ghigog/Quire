package quire.spike.bakeoff

import quire.attribution.scenes.SceneSegmenter
import quire.spike.ParagraphUnit

/**
 * Tier 1, plus the turn-taking rule nobody had built (QUI-028).
 *
 * **This exists because the bake-off was comparing the wrong two things.** Every candidate so
 * far was either a model or `core:attribution`'s [quire.attribution.Heuristic], and that
 * heuristic has exactly three rules — speech tag, pronoun tag, action beat — after which it
 * returns `"no tag"` and declines. So "rules cannot reach untagged dialogue" was never measured;
 * what was measured is that *tag-reading* rules cannot, which is true by construction.
 *
 * Untagged dialogue is not evidence-free. Prose has a convention, and it is strong enough that
 * a reader applies it without noticing:
 *
 * ```
 * Geralt whispered:
 * "You need to run that way."                       <- Geralt, by the beat before it
 * "I can't, he'll see me!"                          <- the other one, by alternation
 * "And I can hear you too!" growled the griffon.    <- the griffon, by its own tag
 * ```
 *
 * A new speaker arriving mid-exchange is named *when they arrive*, which is why alternation and
 * tag-reading compose instead of fighting: the tag wins wherever there is one, and alternation
 * only fills the gaps between tags. ADR-0006 calls this "QUI-009's turn-taking fallback" and
 * QUI-009 has not been written.
 *
 * **The rule, deliberately the weakest form that could work.** Within one scene, over dialogue
 * paragraphs in order: if the previous paragraph's speaker is known, and the one before that has
 * a *different* known speaker, then this paragraph is the speaker from two turns back. That is
 * A-B-A continued, and it needs no cast size, no gender, and no model.
 *
 * Everything else declines, on purpose. `wrong voice = coverage x (1 - precision)` (ADR-0005)
 * makes a confident wrong answer the expensive failure, so this reaches only for the pattern it
 * can actually see and leaves the rest to the narrator.
 *
 * **Scene boundaries are load-bearing.** Alternation across a scene break is a guess about two
 * different conversations, so [SceneSegmenter] (QUI-038) resets the chain — the same segmentation
 * the SLM candidate is prompted with, so the two are answering with the same notion of a scene.
 */
class AlternationCandidate(
    private val base: Candidate = Tier1Candidate(),
    /**
     * Fire only where exactly two speakers have been established in this scene.
     *
     * The convention alternation imitates is a *two-person* one. In a scene where five people
     * have spoken, "the speaker from two turns back" is not a reading rule, it is a guess, and
     * ADR-0005 prices a confident guess at the wrong-voice rate. This switch is how the
     * bake-off says what the rule is worth where it genuinely applies, against what it is worth
     * sprayed everywhere — the same reason `pronouns` and `actionBeats` are switchable on
     * [Tier1Candidate].
     */
    private val pairsOnly: Boolean = false,
    /**
     * Alternate only between paragraphs that actually adjoin.
     *
     * A scene here averages ~97 paragraphs (QUI-038 measured a median of 30 scenes over ~2,900),
     * which is not one conversation — it is many, with narration between them. The convention
     * alternation imitates is a *rapid exchange*: consecutive paragraphs, nothing in between.
     * Once narration intervenes the floor is open again and the next line need not be the other
     * speaker's, so a chain that spans the gap is guessing across a boundary the prose drew.
     */
    private val adjacentOnly: Boolean = false,
) : Candidate {

    override val id = "${base.id}+alternation" +
        (if (pairsOnly) "-pairs" else "") + (if (adjacentOnly) "-adjacent" else "")

    override val description =
        "${base.description}; untagged turns filled by A-B-A alternation within a scene" +
            (if (pairsOnly) ", only where exactly two speakers are established" else "") +
            (if (adjacentOnly) ", only between adjoining paragraphs" else "")

    override fun answer(paragraphs: List<ParagraphUnit>, questions: List<Question>): Map<String, Answer> {
        val answers = base.answer(paragraphs, questions).toMutableMap()

        // The same scene signal the dump gives the SLM: PDNC carries no chapter index, so it is
        // reconstructed from headings first. See SceneReport for why that stand-in is needed.
        val prepared = SceneReport.reconstructChapters(paragraphs)
        val sceneOf = HashMap<Int, Int>(prepared.size)
        SceneSegmenter.segment(prepared).forEachIndexed { scene, range ->
            for (p in range.start until range.endExclusive) sceneOf[p] = scene
        }

        // **A turn is a paragraph, not a quotation.** `"Oh, come on," said John, pulling at
        // her coat. "We must go and see Thunderclap."` is two quotations and one speaker, so a
        // chain that stepped per quotation would flip the alternation inside a paragraph and be
        // wrong exactly where prose is most explicit. Grouping first is what makes the rule
        // match the convention it is imitating.
        val turns = questions.groupBy { it.paragraph }.toSortedMap()

        // `previous` and `beforeThat` hold the last two *resolved* turns. An unresolved turn does
        // not advance them: a gap in the middle of an exchange is still the same exchange.
        var scene = -1
        var previous: String? = null
        var beforeThat: String? = null
        var established = mutableSetOf<String>()
        // The paragraph indices of the last two resolved turns, for the adjacency test.
        var previousAt = -99
        var beforeThatAt = -99

        for ((paragraph, inThisTurn) in turns) {
            val here = sceneOf[paragraph] ?: -1
            if (here != scene) {
                scene = here
                previous = null
                beforeThat = null
                established = mutableSetOf()
                previousAt = -99
                beforeThatAt = -99
            }

            // The turn's speaker as the base candidate sees it — any quotation in the paragraph
            // that carries a tag speaks for the whole paragraph.
            val known = inThisTurn.firstNotNullOfOrNull { answers[it.id]?.speaker }
            if (known != null) {
                // Fill the rest of the paragraph from its own tag before moving on, so a split
                // quotation does not read half in one voice and half in the narrator's.
                for (q in inThisTurn) {
                    if (answers[q.id]?.speaker == null) {
                        answers[q.id] = Answer(known, "same paragraph as tag")
                    }
                }
                established += known
                if (known != previous) {
                    beforeThat = previous; beforeThatAt = previousAt
                    previous = known; previousAt = paragraph
                }
                continue
            }

            // A-B-?: the two turns behind this one are known and different, so continue the
            // alternation. Requiring them to differ is what keeps a monologue read as an
            // exchange, and declining otherwise is deliberate — ADR-0005 makes a confident wrong
            // answer the expensive failure.
            if (pairsOnly && established.size != 2) continue
            // A-B-? has to be an unbroken run: B adjoins A, and this turn adjoins B.
            if (adjacentOnly && (paragraph - previousAt != 1 || previousAt - beforeThatAt != 1)) {
                continue
            }
            val filled = beforeThat?.takeIf { previous != null && it != previous } ?: continue
            for (q in inThisTurn) answers[q.id] = Answer(filled, "alternation")
            beforeThat = previous; beforeThatAt = previousAt
            previous = filled; previousAt = paragraph
        }
        return answers
    }
}
