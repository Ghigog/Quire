package quire.voice.foundry

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import quire.model.characters.Gender
import quire.model.characters.Voice

class FoundryTest {

    private val profile: SpeakerProfile =
        File("../../fixtures/voices/libritts_r-f0.tsv").useLines { SpeakerProfile.parse(it) }

    @Test
    fun `a target between two measured speakers brackets it`() {
        // 115 Hz sits mid-range for the measured male pool (84.5-134.5 Hz), so it should
        // bracket rather than degrade to a nearest single candidate.
        val plan = Foundry.plan(Voice(targetF0Hz = 115.0), Gender.MALE, profile)

        val f0a = profile.f0Of(plan.parentA)!!
        val f0b = profile.f0Of(plan.parentB)!!
        assertTrue(profile.pool(Gender.MALE).any { it.id == plan.parentA })
        assertTrue(profile.pool(Gender.MALE).any { it.id == plan.parentB })
        assertTrue(minOf(f0a, f0b) <= 115.0 && 115.0 <= maxOf(f0a, f0b), "115 not bracketed by $f0a/$f0b")
    }

    @Test
    fun `a speaker flagged poor is never returned as a parent`() {
        // spk659 is a real male speaker at 112.5 Hz (QUI-036); target right on top of it so
        // it would otherwise be the clear nearest match.
        val quality = QualityList.parse("659\tpoor".lineSequence())
        val plan = Foundry.plan(Voice(targetF0Hz = 112.5), Gender.MALE, profile, quality)

        assertFalse(plan.parentA == 659 || plan.parentB == 659, "spk659 was chosen despite being flagged poor")
    }

    @Test
    fun `a target outside the measured range degrades to the nearest single candidate`() {
        val plan = Foundry.plan(Voice(targetF0Hz = 10.0), Gender.MALE, profile)
        assertEquals(plan.parentA, plan.parentB, "should not blend past the measured range")
        assertEquals(0.0, plan.fraction)
    }

    @Test
    fun `blending two vectors produces a point between them`() {
        val a = FloatArray(512) { 0.0f }
        val b = FloatArray(512) { 10.0f }
        val result = Foundry.blend(a, b, 0.3)

        for (i in result.indices) {
            assertTrue(result[i] in a[i]..b[i], "element $i = ${result[i]} not between ${a[i]} and ${b[i]}")
        }
        assertEquals(3.0f, result[0], absoluteTolerance = 1e-4f)
    }

    @Test
    fun `planning is deterministic`() {
        val quality = QualityList.parse("659\tpoor".lineSequence())
        val target = Voice(targetF0Hz = 175.0, lengthScale = 1.1)
        val first = Foundry.plan(target, Gender.FEMALE, profile, quality)
        val second = Foundry.plan(target, Gender.FEMALE, profile, quality)
        assertEquals(first, second)
    }

    @Test
    fun `no target pitch degrades to the descriptor's fallback speaker id`() {
        val plan = Foundry.plan(Voice(speakerId = 447), Gender.MALE, profile)
        assertEquals(447, plan.parentA)
        assertEquals(447, plan.parentB)
        assertEquals(0.0, plan.fraction)
    }

    private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "expected $expected, got $actual")
    }
}
