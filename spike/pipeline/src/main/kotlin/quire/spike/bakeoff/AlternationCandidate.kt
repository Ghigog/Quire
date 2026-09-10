package quire.spike.bakeoff

import quire.attribution.scenes.SceneSegmenter
import quire.spike.ParagraphUnit

/**
 * Tier 1, plus the dialogue conventions prose is actually typeset by (QUI-028).
 *
 * **This exists because the bake-off was comparing the wrong two things.** Every candidate so
 * far was either a model or `core:attribution`'s [quire.attribution.Heuristic], and that
 * heuristic has exactly three rules — speech tag, pronoun tag, action beat — after which it
 * returns `"no tag"` and declines. So "rules cannot reach untagged dialogue" was never measured;
 * what was measured is that *tag-reading* rules cannot, which is true by construction.
 *
 * Untagged dialogue is not evidence-free. Fiction is set to a published convention, and it is
 * strong enough that a reader applies it without noticing:
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
 * ## The conventions, and where they are written down
 *
 * There is no governing body — this is editorial tradition, codified by *The Chicago Manual of
 * Style* (North American book publishing) and *New Hart's Rules* (British), and underwritten by
 * conversation analysis, from which the turn-taking half descends (Sacks, Schegloff & Jefferson
 * on adjacency pairs). Four of those conventions are mechanically checkable and all four are
 * implemented here:
 *
 * 1. **One speaker per paragraph.** A paragraph break is the turn boundary, so a paragraph is a
 *    turn and everything quoted inside it is one voice. This is why the loop below groups questions by
 *    paragraph before anything else: `"Oh, come on," said John, pulling at her coat. "We must go
 *    and see Thunderclap."` is two quotations and one speaker, and a chain that stepped per
 *    quotation would flip the alternation inside a paragraph — wrong exactly where prose is most
 *    explicit.
 * 2. **Turn-taking between two speakers.** Once two speakers are established, tags are dropped
 *    and the paragraph breaks carry the alternation. The floor passes to the other party on each
 *    turn.
 * 3. **Establishment, and re-establishment.** The convention only licenses the omission *after*
 *    tags have named the pair, and it lapses when a third speaker arrives or a delay intervenes;
 *    the author is then obliged to name a speaker again. Both halves are preconditions of the
 *    rule, not tuning — see [requirePair] and [requireAdjacent].
 * 4. **Continued speech.** One speaker running across a paragraph break takes an opening quote on
 *    each paragraph and a closing quote only at the end. It is the one place a paragraph break
 *    does *not* mean a new speaker, so it is the one thing that would silently invert
 *    everything after it. See [continuedSpeech].
 *
 * The convention this deliberately does not implement is the fifth: attribution by voice,
 * idiolect and knowledge asymmetry — who would say this, who could know it. That needs a model
 * of the characters, which is precisely QUI-009's job and not a rule's.
 *
 * ## What it does when the prose does not conform
 *
 * It declines. `wrong voice = coverage x (1 - precision)` (ADR-0005) makes a confident wrong
 * answer the expensive failure: the listener hears the wrong voice whether the book broke the
 * convention or we misread it. And conformance is not an excuse bolted on afterwards — the
 * convention itself says when it applies (tags establish the pair; a third speaker or a delay
 * ends it), so detecting conformance *is* the rule.
 *
 * **Scene boundaries are load-bearing.** Alternation across a scene break is a guess about two
 * different conversations, so [SceneSegmenter] (QUI-038) resets the chain — the same segmentation
 * the SLM candidate is prompted with, so the two are answering with the same notion of a scene.
 */
class AlternationCandidate(
    private val base: Candidate = Tier1Candidate(),
    /**
     * Require the pair to have been established, and drop it when a third speaker arrives.
     *
     * The convention alternation imitates is a *two-person* one, licensed by tags that named
     * the pair first. In a scene where five people have spoken, "the other one" is not a reading
     * rule, it is a guess, and ADR-0005 prices a confident guess at the wrong-voice rate. Off,
     * the pair is just the last two distinct speakers and a third simply slides the window —
     * which is what the rule is worth sprayed everywhere, against what it is worth where the
     * convention says it holds.
     */
    private val requirePair: Boolean = true,
    /**
     * Require a turn to adjoin the previous one: narration between them re-opens the floor.
     *
     * A scene here averages ~97 paragraphs (QUI-038 measured a median of 30 scenes over ~2,900),
     * which is not one conversation — it is many, with narration between them. The convention is
     * for a *rapid exchange*: consecutive paragraphs, nothing in between. Once narration
     * intervenes the author owes the reader a fresh tag, so a chain that spans the gap is
     * guessing across a boundary the prose drew.
     */
    private val requireAdjacent: Boolean = true,
    /**
     * Read the continued-speech convention: an unclosed quotation carries its speaker into the
     * next paragraph.
     *
     * Rare but poisonous. It is the only paragraph break that is not a turn boundary, so missing
     * one does not cost a single line — it inverts every inferred line after it until the next
     * tag. 0.83% of PDNC paragraphs containing a quote mark leave it open (`conformance`), which
     * is small, and real EPUBs of long speeches are where it earns its keep.
     */
    private val continuedSpeech: Boolean = true,
    /**
     * Seat the floor only on a speech tag, never on the weaker rules below it.
     *
     * The convention says tags establish the pattern, and the style guides count an action
     * beat as one of them. Whether that holds *for us* is a different question, because our
     * beat rule is not the convention, it is an approximation of it: `speech tag` is 99.0%
     * precise on PDNC where `action beat` is 66.0% and `same paragraph as tag` 50.8%. A seat
     * taken on a coin-flip is alternated on confidently for every turn after it.
     */
    private val strongSeatsOnly: Boolean = false,
    /**
     * Stop after this many inferred turns in a row, or 0 for no limit.
     *
     * Precision decays with distance from the last tag — the rule is re-deriving the floor
     * from its own previous guess — so this asks how far the convention can be run before it
     * is guessing. It is a cap on the chain, not on the exchange: a tag resets it.
     */
    private val maxChain: Int = 0,
) : Candidate {

    override val id = "${base.id}+alternation" +
        (if (!requirePair) "-anyspeakers" else "") +
        (if (!requireAdjacent) "-anygap" else "") +
        (if (!continuedSpeech) "-nocontinued" else "") +
        (if (strongSeatsOnly) "-strongseats" else "") +
        (if (maxChain > 0) "-chain$maxChain" else "")

    override val description = buildString {
        append(base.description)
        append("; untagged turns filled by two-speaker alternation within a scene")
        append(if (requirePair) ", pair established by tags and dropped on a third speaker" else ", pair is whoever spoke last")
        append(if (requireAdjacent) ", exchange broken by intervening narration" else ", exchange spans narration")
        if (continuedSpeech) append(", unclosed quotations continue the same speaker")
        if (strongSeatsOnly) append(", floor seated only by explicit speech tags")
        if (maxChain > 0) append(", at most $maxChain inferred turns after a tag")
    }

    override fun answer(paragraphs: List<ParagraphUnit>, questions: List<Question>): Map<String, Answer> {
        val answers = base.answer(paragraphs, questions).toMutableMap()

        // The same scene signal the dump gives the SLM: PDNC carries no chapter index, so it is
        // reconstructed from headings first. See SceneReport for why that stand-in is needed.
        val prepared = SceneReport.reconstructChapters(paragraphs)
        val sceneOf = HashMap<Int, Int>(prepared.size)
        SceneSegmenter.segment(prepared).forEachIndexed { scene, range ->
            for (p in range.start until range.endExclusive) sceneOf[p] = scene
        }
        val textOf = paragraphs.associate { it.index to it.text }

        val turns = questions.groupBy { it.paragraph }.toSortedMap()
        val floor = Floor(requirePair, unbrokenRun = requireAdjacent)
        var chain = 0
        var scene = -1
        var lastTurnAt = NEVER

        for ((paragraph, inThisTurn) in turns) {
            val here = sceneOf[paragraph] ?: -1
            if (here != scene) {
                scene = here
                floor.reset()
                lastTurnAt = NEVER
            }
            // The turn before this one, whether or not anybody could attribute it: a turn nobody
            // claimed still happened, and pretending it did not is how a chain skips a speaker.
            val previousTurnAt = lastTurnAt
            lastTurnAt = paragraph

            // Convention 4, checked before anything else because it is the one that says this
            // paragraph break is *not* a turn boundary: the previous paragraph left a quotation
            // open and this one opens another. Everything downstream would otherwise flip.
            val continues = continuedSpeech &&
                previousTurnAt == paragraph - 1 &&
                textOf[previousTurnAt]?.let(::leavesQuotationOpen) == true &&
                textOf[paragraph]?.let(::opensQuotation) == true

            // Convention 1: any quotation in the paragraph that carries a tag speaks for all of
            // them, so the turn's speaker is whatever the base candidate could read anywhere in it.
            val answered = inThisTurn.firstOrNull { answers[it.id]?.speaker != null }?.let { answers.getValue(it.id) }
            val tagged = answered?.speaker

            if (tagged != null) {
                for (q in inThisTurn) {
                    if (answers[q.id]?.speaker == null) answers[q.id] = Answer(tagged, "same paragraph as tag")
                }
                // A tag is self-sufficient and it also repairs the floor: this is how a third
                // speaker is noticed, and how one tag after a delay re-establishes the exchange.
                chain = 0
                if (strongSeatsOnly && answered?.evidence != "speech tag") {
                    // Attributed, but not by evidence this rule is willing to alternate on.
                    floor.lost()
                    continue
                }
                // [Floor.took] leaves the other seat alone when the same speaker speaks again,
                // which is also what a continuation needs: the floor never passed.
                floor.took(tagged, paragraph)
                continue
            }

            if (continues) {
                val speaking = floor.speaking
                if (speaking == null) { floor.lost(); continue }
                for (q in inThisTurn) answers[q.id] = Answer(speaking, "continued speech")
                continue
            }

            // Convention 3, the delay clause: an exchange is consecutive paragraphs. A turn that
            // does not adjoin the last one is past the point where the author owed a fresh tag.
            if (requireAdjacent && paragraph - previousTurnAt != 1) {
                floor.lost()
                continue
            }

            if (maxChain > 0 && chain >= maxChain) {
                floor.lost()
                continue
            }

            // Convention 2: the floor passes to the other party.
            val other = floor.pass(paragraph)
            if (other == null) {
                // **Every decline has to forget who was speaking.** This turn belonged to
                // somebody; carrying the previous speaker past it would have the next tag
                // establish a pair with a speaker who is one turn stale, and then alternate
                // confidently on it. Found by the continued-speech rule scoring 11% (Worklog).
                floor.lost()
                continue
            }
            chain++
            // Labelled by how far the chain has run from the last tag, because the two things
            // that distance does are both worth seeing in the report: precision decays, and it
            // alternates — the even steps name the speaker two turns back, which survives a
            // wrong guess about who the partner is, and the odd steps do not.
            val evidence = when {
                chain == 1 -> "alternation"
                chain >= 4 -> "alternation x4+"
                else -> "alternation x$chain"
            }
            for (q in inThisTurn) answers[q.id] = Answer(other, evidence)
        }
        return answers
    }

    /**
     * Who is speaking, who they are speaking with, where each last spoke, and whether we still
     * know. Two seats and a floor, rather than "the speaker from two turns back" — the
     * difference is a delay. After narration re-opens the floor, one tag is all the convention
     * asks for to resume, because the pair is still the pair; "two turns back" would need two.
     */
    private class Floor(private val requirePair: Boolean, private val unbrokenRun: Boolean) {

        /** A party to the exchange and the paragraph they last spoke in. */
        private data class Seat(val who: String, val at: Int)

        /** Most recent first, at most two: the floor holder and the other party. */
        private var seats = listOf<Seat>()

        /** False once a turn went unattributed: somebody spoke and we do not know who. */
        private var floorKnown = false

        /** Who spoke the last turn, or null when a decline left it unknown. */
        val speaking: String? get() = seats.firstOrNull()?.who.takeIf { floorKnown }

        fun reset() {
            seats = emptyList()
            floorKnown = false
        }

        /** This turn is [who]'s at paragraph [at], on evidence of its own. */
        fun took(who: String, at: Int) {
            val seat = Seat(who, at)
            seats = when {
                // The same speaker again: the floor did not pass, so the other seat stands.
                seats.firstOrNull()?.who == who -> listOf(seat) + seats.drop(1)
                // A third speaker: the convention lapses and the pair must be named again.
                requirePair && seats.size == 2 && seats.none { it.who == who } -> listOf(seat)
                else -> listOf(seat) + seats.filter { it.who != who }.take(1)
            }
            floorKnown = true
        }

        /** Nobody could attribute this turn, so who holds the floor is no longer known. */
        fun lost() {
            floorKnown = false
        }

        /**
         * The other party takes the floor at paragraph [at], or null when the convention does
         * not license it: no pair established, the last turn unattributed, or — under
         * [unbrokenRun] — the two seats not themselves adjoining.
         */
        fun pass(at: Int): String? {
            if (!floorKnown || seats.size != 2) return null
            val (holder, other) = seats
            if (unbrokenRun && holder.at - other.at != 1) return null
            took(other.who, at)
            return other.who
        }
    }

    internal companion object {
        /** Further from any real paragraph than any adjacency test will accept. */
        const val NEVER = -99

        /**
         * Quotation marks this reads. British single-quote setting (New Hart's Rules) is not
         * here: `'` is an apostrophe far more often than a quotation mark, so counting it would
         * report every contraction as an open quotation. PDNC is double-quoted throughout.
         */
        val OPENERS = setOf('"', '“')

        /** Does this paragraph open a quotation it never closes? */
        fun leavesQuotationOpen(text: String): Boolean =
            text.count { it == '"' } % 2 == 1 || text.count { it == '“' } > text.count { it == '”' }

        /** Does it start with a quotation mark, as the convention requires of a continuation? */
        fun opensQuotation(text: String): Boolean = text.trimStart().firstOrNull() in OPENERS
    }
}
