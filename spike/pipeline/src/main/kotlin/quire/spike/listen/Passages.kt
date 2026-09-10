package quire.spike.listen

import kotlin.random.Random

/**
 * Choosing what the listener hears (QUI-039).
 *
 * **The chooser cannot see who is right, because the type it is given has no room for it.**
 * [Spot] says where a quotation is and nothing about who said it. That is not a convention to
 * be remembered — a selector that quietly preferred passages where one candidate loses would
 * produce a rigged listen and would not look rigged, so the blindness is enforced by leaving
 * gold out of the shape rather than by leaving it out of the code.
 *
 * Two tracks, because a listen answers two different questions and one passage cannot do both:
 *
 * * [natural] is prose taken without regard to what either candidate said. It is the everyday
 *   experience and the honest test of whether the difference is noticeable at all.
 * * [aroundDisagreements] is only the places the two candidates part company, each kept in the
 *   context of the turns around it. About one quotation in thirteen separates the two settings
 *   (QUI-028: 26.8% coverage against 34.3%), so an unselected passage holds a handful; this
 *   track is how you hear what one *sounds* like without listening to a whole chapter.
 */
object Passages {

    /** Where a quotation sits, and deliberately nothing else. */
    data class Spot(val id: String, val paragraph: Int, val start: Int, val end: Int)

    /** A half-open run of paragraphs. */
    data class Passage(val from: Int, val toExclusive: Int) {
        val paragraphs get() = from until toExclusive
    }

    /**
     * [count] continuous runs of prose, each carrying [quotations] quotations inside at most
     * [maxParagraphs] paragraphs, drawn at random from every run that qualifies.
     *
     * **Density is a structural filter, not a peek at the answers.** An unconstrained window
     * that gathers sixty quotations out of Austen drags in five thousand words of narration
     * and renders to thirty-seven minutes — measured, not guessed — which is not a listen
     * anybody will sit through twice. Requiring the quotations to arrive close together buys
     * a passage of actual conversation, and conversation is where attribution is load-bearing
     * anyway. What the filter never reads is who said any of it.
     *
     * **[count] passages rather than one**, because the thing being sampled is rare. About one
     * quotation in thirteen separates the two candidates, so a single short window swings
     * between zero and seven of them purely on the seed, and a listener handed the zero would
     * conclude the settings are identical. Several independent windows keep the sample honest
     * without lengthening any one of them past patience. Seeds are derived from [seed] in
     * order, so the set is reproducible and none of it is chosen after the fact.
     */
    fun natural(
        spots: List<Spot>,
        quotations: Int,
        maxParagraphs: Int,
        count: Int,
        seed: Long,
    ): List<Passage> {
        if (spots.isEmpty()) return emptyList()
        val ordered = spots.sortedBy { it.paragraph }
        val starts = ordered.map { it.paragraph }.distinct()
        val qualifying = starts.mapNotNull { start ->
            val window = ordered.filter { it.paragraph >= start }.take(quotations)
            if (window.size < quotations) return@mapNotNull null
            val last = window.last().paragraph
            if (last - start + 1 > maxParagraphs) null else Passage(start, last + 1)
        }
        if (qualifying.isEmpty()) return emptyList()

        val picked = mutableListOf<Passage>()
        for (i in 0 until count) {
            val rng = Random(seed + i)
            // Overlapping picks would play the listener the same conversation twice, which
            // costs their patience without adding a sample.
            val free = qualifying.filterNot { candidate ->
                picked.any { it.from < candidate.toExclusive && candidate.from < it.toExclusive }
            }
            if (free.isEmpty()) break
            picked += free[rng.nextInt(free.size)]
        }
        return picked.sortedBy { it.from }
    }

    /**
     * [limit] disagreements, sampled so that no two land near each other, each with [context]
     * paragraphs of run-up and run-out.
     *
     * **Spacing them out is what keeps the sample honest, and it was not obvious.** The first
     * version took a random sample and merged overlapping windows, which quietly over-weighted
     * exactly the wrong thing: consecutive disagreements are mostly one broken chain, because an
     * alternation that inverts stays inverted until the next tag re-seats it. Merging turned one
     * mistake into five and made the rule look far worse than it measures — on Daisy Miller it
     * drew a sample the conventions got 3 of 8 right, against 80.0% for the same rule over the
     * whole novel. Requiring a gap draws independent errors instead of one error five times.
     *
     * Context is not decoration either. Alternation is a claim about a *sequence* of turns, so a
     * disagreement played alone is unjudgeable — the listener needs the turns that established
     * the exchange to hear which reading of it was right.
     */
    fun aroundDisagreements(
        disagreeAt: Set<Int>,
        context: Int,
        limit: Int,
        seed: Long,
    ): List<Passage> {
        if (disagreeAt.isEmpty()) return emptyList()
        val all = disagreeAt.sorted()
        // Stratified: the novel is cut into `limit` equal stretches and one disagreement is
        // drawn from each that has any. Spacing alone was not enough — in this corpus
        // disagreements arrive in long runs, so two picks four paragraphs apart still land in
        // the same broken chain. Stratifying spreads the sample over the book by construction.
        val first = all.first()
        val span = (all.last() - first + 1).coerceAtLeast(1)
        val picked = LinkedHashSet<Int>()
        for (stratum in 0 until limit) {
            val from = first + (span.toLong() * stratum / limit).toInt()
            val to = first + (span.toLong() * (stratum + 1) / limit).toInt()
            val inStratum = all.filter { it in from until to && it !in picked }
            if (inStratum.isEmpty()) continue
            picked += inStratum[Random(seed + stratum).nextInt(inStratum.size)]
        }
        // Strata the disagreements never reach leave slots empty — two long runs at opposite
        // ends of a book fill two strata out of four. Top up from whatever is left, each time
        // taking the paragraph furthest from everything already chosen, so the shortfall is
        // spread rather than dropped.
        while (picked.size < limit) {
            val remaining = all.filterNot { it in picked }
            if (remaining.isEmpty()) break
            picked += remaining.maxByOrNull { candidate ->
                picked.minOfOrNull { kotlin.math.abs(it - candidate) } ?: 0
            }!!
        }
        return picked.sorted().map { Passage(it - context, it + context + 1) }
    }
}
