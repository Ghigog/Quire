package quire.attribution.slm.attribution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneInferenceTest {

    @Test
    fun `alternates onto the other speaker when one precedes`() {
        val known = listOf(0 to "Sarah", 2 to "Thomas")
        assertEquals("Thomas", SceneInference.speakerFor(1, known, setOf("Sarah", "Thomas")))
    }

    @Test
    fun `anchors on the following line when none precedes`() {
        val known = listOf(2 to "Thomas")
        assertEquals("Sarah", SceneInference.speakerFor(0, known, setOf("Sarah", "Thomas")))
    }

    @Test
    fun `declines when the scene is not two distinct speakers`() {
        assertNull(SceneInference.speakerFor(1, listOf(0 to "Sarah"), setOf("Sarah")))
        assertNull(
            SceneInference.speakerFor(
                1, listOf(0 to "Sarah", 2 to "Thomas", 4 to "Elizabeth"),
                setOf("Sarah", "Thomas", "Elizabeth"),
            ),
        )
    }

    @Test
    fun `declines with nothing resolved anywhere in the scene`() {
        assertNull(SceneInference.speakerFor(0, emptyList(), setOf("Sarah", "Thomas")))
    }
}
