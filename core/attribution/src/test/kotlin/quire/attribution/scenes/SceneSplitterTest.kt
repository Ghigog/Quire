package quire.attribution.scenes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.model.Paragraph

class SceneSplitterTest {

    private val texts = listOf(
        "\"You will miss it,\" said Ellen.",
        "\"There is another at nine.\"",
        "\"There is not,\" said Robert.",
        "The guard's whistle went somewhere down the platform.",
        "The train pulled away and the platform emptied.",
        "Rain began against the high windows of the station roof.",
        "\"Are you coming?\" said Dana.",
    )
    private val paragraphs = texts.mapIndexed { i, t -> Paragraph("b#p$i", t, 0, i) }
    private val speakers = mapOf(0 to "Ellen", 2 to "Robert", 6 to "Dana")

    @Test
    fun `an over-long scene is split at a turn boundary`() {
        val scene = Scene(0, paragraphs.size)
        val pieces = SceneSplitter.split(
            scene, paragraphs, budget = 5,
            length = { 1 },
            speakerOf = { speakers[it.index] },
        )

        assertEquals(2, pieces.size)
        assertEquals(Scene(0, 5), pieces[0].range)
        assertEquals(Scene(5, 7), pieces[1].range)

        // Each piece within budget.
        for (piece in pieces) assertTrue(piece.range.size <= 5, "piece $piece over budget")

        // No piece begins mid-exchange: the second piece opens on the narration paragraph
        // reached only after the exchange above it had already gone quiet.
        val secondPieceStart = paragraphs[pieces[1].range.start]
        assertTrue(secondPieceStart.text.contains("Rain"))

        // The second piece carries the last speaker the first piece resolved — Robert,
        // from before the gap, not Ellen from earlier still.
        assertEquals(null, pieces[0].carriedSpeaker)
        assertEquals("Robert", pieces[1].carriedSpeaker)
    }

    @Test
    fun `a scene within budget is not split`() {
        val scene = Scene(0, paragraphs.size)
        val pieces = SceneSplitter.split(scene, paragraphs, budget = 100, length = { 1 })
        assertEquals(listOf(Scene(0, paragraphs.size)), pieces.map { it.range })
    }

    @Test
    fun `splitting covers every paragraph in the scene exactly once`() {
        val scene = Scene(0, paragraphs.size)
        val pieces = SceneSplitter.split(scene, paragraphs, budget = 3, length = { 1 })
        val covered = pieces.flatMap { it.range.start until it.range.endExclusive }
        assertEquals((0 until paragraphs.size).toList(), covered)
    }
}
