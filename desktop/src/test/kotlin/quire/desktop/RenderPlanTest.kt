package quire.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import quire.model.AttributionResult
import quire.model.Kind
import quire.model.Tier

class RenderPlanTest {

    private fun segment(text: String, kind: Kind, speaker: String? = null) = AttributionResult(
        locator = "c#p1#s0",
        text = text,
        kind = kind,
        speakerId = speaker,
        confidence = if (speaker == null) 0.0 else 0.95,
        tier = if (speaker == null) Tier.NONE else Tier.HEURISTIC,
    )

    @Test
    fun `renders every segment in reading order, narration included`() {
        val plan = RenderPlan.build(
            segments = listOf(
                segment("The room was cold.", Kind.NARRATION),
                segment("\"Shut it,\" said Sarah.", Kind.DIALOGUE, "Sarah"),
                segment("\"After you.\"", Kind.DIALOGUE),
            ),
            voices = mapOf("Sarah" to 7),
            narratorVoiceId = 42,
        )

        assertEquals(listOf(42, 7, 42), plan.map { it.voiceId })
        assertEquals(listOf("The room was cold.", "\"Shut it,\" said Sarah.", "\"After you.\""), plan.map { it.text })
    }

    @Test
    fun `an unresolved line is narrated but keeps the fact that nobody resolved it`() {
        val plan = RenderPlan.build(
            segments = listOf(segment("\"After you.\"", Kind.DIALOGUE)),
            voices = emptyMap(),
            narratorVoiceId = 3,
        )

        assertEquals(3, plan.single().voiceId)
        assertEquals(null, plan.single().speakerId, "coverage reporting depends on this staying null")
    }

    @Test
    fun `dialogue only drops narration and leaves the voices`() {
        val plan = RenderPlan.build(
            segments = listOf(
                segment("The room was cold.", Kind.NARRATION),
                segment("\"Shut it,\" said Sarah.", Kind.DIALOGUE, "Sarah"),
            ),
            voices = mapOf("Sarah" to 7),
            narratorVoiceId = 42,
            dialogueOnly = true,
        )

        assertEquals(listOf(7), plan.map { it.voiceId })
    }

    @Test
    fun `an empty segment is not worth a synthesis call`() {
        val plan = RenderPlan.build(
            segments = listOf(segment("   ", Kind.NARRATION), segment("Real.", Kind.NARRATION)),
            voices = emptyMap(),
            narratorVoiceId = 1,
        )

        assertEquals(listOf("Real."), plan.map { it.text })
    }

    @Test
    fun `segments of one paragraph share a key, so no pause lands inside a spoken line`() {
        fun at(locator: String, text: String, kind: Kind, speaker: String?) = AttributionResult(
            locator = locator,
            text = text,
            kind = kind,
            speakerId = speaker,
            confidence = if (speaker == null) 0.0 else 0.95,
            tier = if (speaker == null) Tier.NARRATOR else Tier.HEURISTIC,
        )

        val plan = RenderPlan.build(
            segments = listOf(
                at("c#p1#s0", "\"It is not the letter,\"", Kind.DIALOGUE, "Thomas"),
                at("c#p1#s1", "he said.", Kind.NARRATION, null),
                at("c#p1#s2", "\"It is the answer to it.\"", Kind.DIALOGUE, "Thomas"),
                at("c#p2#s0", "The clock struck four.", Kind.NARRATION, null),
            ),
            voices = mapOf("Thomas" to 5),
            narratorVoiceId = 9,
        )

        assertEquals(listOf("c#p1", "c#p1", "c#p1", "c#p2"), plan.map { it.paragraph })
    }

    @Test
    fun `coverage counts dialogue, that a speaker was named`() {
        val coverage = RenderPlan.coverage(
            listOf(
                segment("narration", Kind.NARRATION),
                segment("a", Kind.DIALOGUE, "Sarah"),
                segment("b", Kind.DIALOGUE),
                segment("c", Kind.DIALOGUE, "Thomas"),
            ),
        )

        assertEquals(3, coverage.dialogue)
        assertEquals(2, coverage.resolved)
    }

    @Test
    fun `coverage of a book with no dialogue is not a divide by zero`() {
        assertEquals(0.0, RenderPlan.coverage(listOf(segment("narration", Kind.NARRATION))).fraction)
    }
}
