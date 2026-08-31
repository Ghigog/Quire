package quire.attribution

import quire.model.AttributionResult
import quire.model.Kind
import quire.model.Tier

/**
 * Resolves untagged dialogue by who spoke last — the second pass, and the one that makes a
 * real book sound like a book.
 *
 * Most dialogue carries no speech tag. An author writes `"You will miss it," said Ellen`
 * once and then trusts the reader to follow the alternation for a dozen lines. Tier 1 reads
 * three of those fifteen lines; the other twelve are exactly the passage a listener most
 * wants voiced, and they are the ones that come out flat.
 *
 * **This is not @Voice's alternation.** That switches voices whenever it detects a change,
 * with no idea who anybody is, so it cannot survive a third speaker or a scene break and
 * cannot give a character the same voice in chapter nine as in chapter one. This anchors
 * alternation to *named* speakers Tier 1 established, and refuses wherever that anchor is
 * missing:
 *
 * - only inside a two-hander — three active speakers means alternation is a coin toss
 * - only while the exchange is unbroken — a scene break ends it
 * - never over an attribution Tier 1 already made
 *
 * Its confidence sits below every Tier 1 rule, so QUI-009's model can overrule it.
 */
object Conversation {

    /**
     * Paragraphs of narration that end an exchange.
     *
     * One or two is a beat inside a scene — a porter passing, a whistle down the platform —
     * and the alternation carries straight over it. More is a scene change, where whoever
     * spoke last is no longer a guide to who speaks next.
     */
    const val MAX_GAP_PARAGRAPHS = 2

    /**
     * A quotation continuing its own paragraph. Higher than alternation because the
     * convention is stronger: a change of speaker gets a new paragraph.
     */
    const val CONTINUATION = 0.80

    /** How many recent attributed lines decide whether this is a two-hander. */
    private const val WINDOW = 6

    fun resolve(
        results: List<AttributionResult>,
        /**
         * The book's speaking cast.
         *
         * Needed because an author tags an exchange *once* — `"You will miss it," said
         * Ellen` — and leaves the rest to the reader. After that single tag the pass knows
         * who just spoke but has never seen the other party named, so it has nobody to
         * alternate to. When the cast holds exactly two people, the other one is not a
         * guess: it is the only remaining possibility.
         */
        cast: List<String> = emptyList(),
        maxGap: Int = MAX_GAP_PARAGRAPHS,
    ): List<AttributionResult> {
        val out = results.toMutableList()
        val recent = ArrayDeque<String>()
        var last: String? = null
        var other: String? = null
        var lastDialogueParagraph: String? = null
        var previousDialogueParagraph: String? = null
        var countedParagraph: String? = null
        var gap = 0

        fun saw(speaker: String) {
            if (speaker != last) {
                other = last
                last = speaker
            }
            recent.addLast(speaker)
            while (recent.size > WINDOW) recent.removeFirst()
        }

        for ((i, result) in results.withIndex()) {
            val paragraph = result.locator.substringBeforeLast("#s")

            if (result.kind != Kind.DIALOGUE) {
                // Narration in a paragraph that holds no speech at all is a beat between
                // exchanges; narration beside a quotation is its speech tag.
                if (paragraph != lastDialogueParagraph && paragraph != countedParagraph) {
                    countedParagraph = paragraph
                    gap++
                    if (gap > maxGap) { last = null; other = null; recent.clear() }
                }
                continue
            }

            gap = 0
            countedParagraph = null
            previousDialogueParagraph = lastDialogueParagraph
            lastDialogueParagraph = paragraph


            val known = result.speakerId
            if (known != null) {
                saw(known)
                continue
            }

            // A second quotation in the *same* paragraph is the same person still
            // talking: `"It is not the letter," he said. "It is the answer to it."` is one
            // speaker across two spans. Prose signals a change of speaker with a new
            // paragraph, so alternating inside one is reading the convention backwards —
            // it put Sarah in the middle of Thomas's line before this existed.
            if (paragraph == previousDialogueParagraph && last != null) {
                out[i] = result.copy(
                    speakerId = last,
                    confidence = CONTINUATION,
                    tier = Tier.SCENE,
                    evidence = "same paragraph as $last",
                )
                continue
            }

            // Two speakers and no more: with a third in the room, whoever did not speak
            // last is a guess rather than an inference.
            val seen = recent.distinct()
            val partner = when {
                seen.size == 2 -> other
                // Only one has been named so far, but the cast is a two-hander.
                seen.size == 1 && cast.size == 2 -> cast.firstOrNull { it != last }
                else -> null
            }
            if (partner != null && partner != last) {
                out[i] = result.copy(
                    speakerId = partner,
                    confidence = ALTERNATION,
                    tier = Tier.SCENE,
                    evidence = "alternation after $last",
                )
                saw(partner)
            }
        }
        return out
    }

    /**
     * Below every Tier 1 rule and above nothing.
     *
     * It is an inference from structure rather than evidence in the text, so PRD §2's gates
     * should let the model overrule it wherever the model has an opinion.
     */
    const val ALTERNATION = 0.70
}
