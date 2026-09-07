package quire.attribution.scenes

import quire.attribution.Conversation
import quire.attribution.Segmenter
import quire.model.Kind
import quire.model.Paragraph

/**
 * Splits a scene too long for the model's context window (ADR-0006: "Long scenes may
 * exceed the context window... splitting it at a turn boundary and carrying the last
 * speaker across is the obvious approach and is untested").
 *
 * A **turn boundary** is the same reset [Conversation] already uses to know an exchange has
 * ended: once a stretch of narration has run longer than [Conversation.MAX_GAP_PARAGRAPHS]
 * paragraphs with no dialogue in it, whoever spoke last is no longer a guide to who speaks
 * next, so cutting there never divides one exchange between two prompts. Structural, like
 * [SceneSegmenter] — this reads the same paragraphs Tier 1 already segmented, not the
 * meaning of the words.
 */
object SceneSplitter {

    /**
     * One piece of a split scene, and the speaker the piece before it left off on.
     *
     * [atTurnBoundary] is false only when this piece begins where the budget ran out rather
     * than at a turn boundary or a paragraph of pure narration — the last-resort cut. It matters downstream: a piece starting
     * mid-exchange has had its opening lines separated from the turns that explain them, so
     * [carriedSpeaker] is a weaker guide there than usual, and QUI-009 may want to weight it
     * accordingly. The first piece of a scene is always true.
     */
    data class Piece(
        val range: Scene,
        val carriedSpeaker: String? = null,
        val atTurnBoundary: Boolean = true,
    )

    /**
     * @param length a piece budget's cost for one paragraph — tokens, in practice.
     * @param speakerOf the last speaker resolved for a paragraph by whatever attribution has
     *   already run (Tier 1 today; QUI-009's model, later). Null wherever nobody is known.
     */
    fun split(
        scene: Scene,
        paragraphs: List<Paragraph>,
        budget: Int,
        length: (Paragraph) -> Int,
        speakerOf: (Paragraph) -> String? = { null },
        maxGap: Int = Conversation.MAX_GAP_PARAGRAPHS,
    ): List<Piece> {
        val inScene = paragraphs.filter { it.index in scene }.sortedBy { it.index }
        if (inScene.isEmpty()) return emptyList()
        if (inScene.size == 1) return listOf(Piece(scene))

        val boundaries = turnBoundaries(inScene, maxGap)
        val narration = narrationStarts(inScene)
        val costs = inScene.map(length)
        val prefix = IntArray(inScene.size + 1)
        for (i in inScene.indices) prefix[i + 1] = prefix[i] + costs[i]
        fun sum(fromPos: Int, toPosExclusive: Int) = prefix[toPosExclusive] - prefix[fromPos]

        val pieces = mutableListOf<Piece>()
        var pieceStart = 0
        var carried: String? = null
        var atBoundary = true

        while (pieceStart < inScene.size) {
            var end = pieceStart
            while (end < inScene.size && sum(pieceStart, end + 1) <= budget) end++
            // A single paragraph over budget on its own: take it anyway rather than emit
            // an empty piece.
            if (end == pieceStart) end = pieceStart + 1

            if (end >= inScene.size) {
                pieces += Piece(Scene(inScene[pieceStart].index, scene.endExclusive), carried, atBoundary)
                break
            }

            // Three places to cut, best first, all at or before where the budget ran out:
            //
            // 1. a turn boundary — narration has run long enough that Conversation itself
            //    has given up on who spoke last, so nothing is being separated;
            // 2. any paragraph holding no dialogue — weaker, but it still never puts the
            //    cut between two adjacent turns, which is what "mid-exchange" means;
            // 3. wherever the budget ran out.
            //
            // Measured over PDNC (`spike/pipeline scenes`): with only (1) and (3), 35.5% of
            // pieces opened mid-exchange. Adding (2) is what makes the acceptance criterion
            // mostly true rather than mostly false — see QUI-038's Worklog.
            val range = end downTo pieceStart + 1
            val clean = range.firstOrNull { inScene[it].index in boundaries }
                ?: range.firstOrNull { inScene[it].index in narration }
            val cut = clean ?: end

            pieces += Piece(Scene(inScene[pieceStart].index, inScene[cut].index), carried, atBoundary)
            carried = lastSpeaker(inScene, pieceStart, cut, speakerOf)
            atBoundary = clean != null
            pieceStart = cut
        }
        return pieces
    }

    /** Paragraph indices holding no dialogue at all — always a safe place to open a piece. */
    private fun narrationStarts(inScene: List<Paragraph>): Set<Int> =
        inScene.filterNot { hasDialogue(it) }.map { it.index }.toSet()

    private fun hasDialogue(paragraph: Paragraph) =
        Segmenter.segment(paragraph.locator, paragraph.text).any { it.kind == Kind.DIALOGUE }

    /**
     * Positions in [inScene] that may open a new piece: the scene's own first paragraph,
     * and any paragraph reached only after a run of more than [maxGap] consecutive
     * paragraphs holding no dialogue — the point at which [Conversation] itself gives up
     * on who spoke last.
     */
    private fun turnBoundaries(inScene: List<Paragraph>, maxGap: Int): Set<Int> {
        val out = mutableSetOf(inScene.first().index)
        var gap = 0
        for (paragraph in inScene) {
            if (hasDialogue(paragraph)) {
                gap = 0
            } else {
                gap++
                if (gap > maxGap) out += paragraph.index
            }
        }
        return out
    }

    private fun lastSpeaker(
        inScene: List<Paragraph>,
        fromPos: Int,
        toPosExclusive: Int,
        speakerOf: (Paragraph) -> String?,
    ): String? {
        for (pos in (toPosExclusive - 1) downTo fromPos) {
            speakerOf(inScene[pos])?.let { return it }
        }
        return null
    }
}
