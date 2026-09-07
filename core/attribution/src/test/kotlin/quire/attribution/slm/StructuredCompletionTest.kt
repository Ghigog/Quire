package quire.attribution.slm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private data class Greeting(val name: String)

private val greetingShape = JsonShape<Greeting> { raw ->
    val match = Regex(""""name"\s*:\s*"([^"]+)"""").find(raw)
        ?: throw ShapeMismatch("no \"name\" field found", raw)
    Greeting(match.groupValues[1])
}

class StructuredCompletionTest {

    @Test
    fun `valid first reply is parsed without a retry`() {
        val runtime = FakeSlmRuntime(listOf("""{"name":"Geralt"}"""))
        val result = StructuredCompletion(runtime).complete("prompt", greetingShape, maxTokens = 20)

        assertEquals(StructuredResult.Success(Greeting("Geralt")), result)
        assertEquals(1, runtime.callCount)
    }

    @Test
    fun `one malformed reply is retried once and then succeeds`() {
        val runtime = FakeSlmRuntime(listOf("not json", """{"name":"Yennefer"}"""))
        val result = StructuredCompletion(runtime).complete("prompt", greetingShape, maxTokens = 20)

        assertEquals(StructuredResult.Success(Greeting("Yennefer")), result)
        assertEquals(2, runtime.callCount)
    }

    @Test
    fun `two malformed replies report failure, not raw text, after exactly one retry`() {
        val runtime = FakeSlmRuntime(listOf("not json", "still not json"))
        val result = StructuredCompletion(runtime).complete("prompt", greetingShape, maxTokens = 20)

        assertIs<StructuredResult.Failure>(result)
        assertEquals(2, runtime.callCount)
    }
}
