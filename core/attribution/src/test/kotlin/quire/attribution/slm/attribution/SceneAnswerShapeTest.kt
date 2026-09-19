package quire.attribution.slm.attribution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import quire.attribution.slm.ShapeMismatch

class SceneAnswerShapeTest {

    @Test
    fun `parses a named speaker`() {
        val parsed = SceneAnswerShape.parse("""[{"speaker":"Sarah","confidence":0.9}]""")
        assertEquals(listOf(SceneAnswer("Sarah", 0.9)), parsed)
    }

    @Test
    fun `a null speaker parses as narration, not the literal word null`() {
        val parsed = SceneAnswerShape.parse("""[{"speaker":null,"confidence":0.8}]""")
        assertEquals(listOf(SceneAnswer(null, 0.8)), parsed)
    }

    @Test
    fun `not an array is rejected`() {
        assertFailsWith<ShapeMismatch> { SceneAnswerShape.parse("""{"speaker":"Sarah","confidence":0.9}""") }
    }

    @Test
    fun `a missing confidence is rejected`() {
        assertFailsWith<ShapeMismatch> { SceneAnswerShape.parse("""[{"speaker":"Sarah"}]""") }
    }
}
