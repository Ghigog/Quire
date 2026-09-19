package quire.attribution.slm.attribution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import quire.attribution.slm.FakeSlmRuntime
import quire.attribution.slm.StructuredCompletion
import quire.model.AttributionResult
import quire.model.Kind
import quire.model.Tier

/**
 * QUI-009's Gherkin scenarios, minus "accuracy on the fixture set" — that needs a real
 * model (QUI-031 is still Todo) and would only be measuring `FakeSlmRuntime`'s canned
 * replies against itself. Everything else is behaviour this class owns outright.
 */
class SceneAttributorTest {

    private fun line(locator: String, text: String, speaker: String? = null, tier: Tier = Tier.NONE) =
        AttributionResult(locator, text, Kind.DIALOGUE, speaker, if (speaker != null) 0.95 else 0.0, tier)

    /** Sarah (tagged), an untagged line, Thomas (tagged) — the standard two-party scene. */
    private fun twoPartyScene() = listOf(
        line("p1#s1", "\"I know,\" said Sarah.", "Sarah", Tier.HEURISTIC),
        line("p1#s2", "\"Are you certain?\""),
        line("p1#s3", "\"Yes,\" said Thomas.", "Thomas", Tier.HEURISTIC),
    )

    @Test
    fun `untagged dialogue resolves from context via the SLM tier`() {
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":"Sarah","confidence":0.9}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        val result = attributor.attribute(twoPartyScene())

        val resolved = result.single { it.locator == "p1#s2" }
        assertEquals("Sarah", resolved.speakerId)
        assertEquals(Tier.SLM, resolved.tier)
    }

    @Test
    fun `confidence between the floor and SLM_MIN falls back to scene inference`() {
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":"Sarah","confidence":0.50}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        val resolved = attributor.attribute(twoPartyScene()).single { it.locator == "p1#s2" }

        // Two-party alternation: Sarah precedes, Thomas follows, so the line between them
        // alternates onto whichever the SLM's own answer did not confirm.
        assertEquals("Thomas", resolved.speakerId)
        assertEquals(Tier.SCENE, resolved.tier)
    }

    @Test
    fun `confidence below the narrator floor goes straight to the narrator`() {
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":"Sarah","confidence":0.30}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        val resolved = attributor.attribute(twoPartyScene()).single { it.locator == "p1#s2" }

        assertNull(resolved.speakerId)
        assertEquals(Tier.NARRATOR, resolved.tier)
    }

    @Test
    fun `the model naming nobody is trusted over a scene guess`() {
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":null,"confidence":0.9}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        val resolved = attributor.attribute(twoPartyScene()).single { it.locator == "p1#s2" }

        assertNull(resolved.speakerId)
        assertEquals(Tier.NARRATOR, resolved.tier)
    }

    @Test
    fun `a speaker outside the cast is declined, not invented`() {
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":"Bob","confidence":0.9}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        val resolved = attributor.attribute(twoPartyScene()).single { it.locator == "p1#s2" }

        assertEquals(Tier.SCENE, resolved.tier)
        assertEquals("Thomas", resolved.speakerId) // falls through to two-party inference
    }

    @Test
    fun `a misaligned reply drops the whole scene to Tier 3, not Tier 1's neighbour's answer`() {
        val scene = listOf(
            line("p1#s1", "\"I know,\" said Sarah.", "Sarah", Tier.HEURISTIC),
            line("p1#s2", "\"Are you certain?\""),
            line("p1#s3", "\"Yes,\" said Thomas.", "Thomas", Tier.HEURISTIC),
            line("p1#s4", "\"Then let us go.\""),
        )
        // Two quotations need an answer; the model returns only one.
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":"Sarah","confidence":0.9}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        val result = attributor.attribute(scene)

        assertEquals(Tier.SCENE, result.single { it.locator == "p1#s2" }.tier)
        assertEquals(Tier.SCENE, result.single { it.locator == "p1#s4" }.tier)
    }

    @Test
    fun `a line not yet attributed reads as narrator without calling the model`() {
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":"Sarah","confidence":0.9}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        assertNull(attributor.cached("p1#s2"))
        assertEquals(0, runtime.callCount)
    }

    @Test
    fun `results are cached, so a second read of the same scene performs no inference`() {
        val runtime = FakeSlmRuntime(listOf("""[{"speaker":"Sarah","confidence":0.9}]"""))
        val attributor = SceneAttributor(StructuredCompletion(runtime), setOf("Sarah", "Thomas"))

        attributor.attribute(twoPartyScene())
        val callsAfterFirst = runtime.callCount
        attributor.attribute(twoPartyScene())

        assertEquals(callsAfterFirst, runtime.callCount)
        assertEquals("Sarah", attributor.cached("p1#s2")?.speakerId)
    }
}
