package quire.app.companion.pipeline

import quire.attribution.slm.CancellationSignal
import quire.attribution.slm.SlmRuntime

/**
 * A minimal [SlmRuntime] for exercising [ImportPipeline]'s resumability without a real
 * model: it tells [BookScan][quire.attribution.scan.BookScan]'s character-extraction prompt
 * apart from [SceneAttributor][quire.attribution.slm.attribution.SceneAttributor]'s
 * scene-attribution prompt by their fixed opening lines, and answers each with the cheapest
 * valid reply — no characters found, every unresolved line read by the Narrator — so what a
 * test measures is which scenes were ever asked, not what the model decided.
 */
class FakeSlmRuntime : SlmRuntime {
    var callCount = 0
        private set

    override fun complete(prompt: String, maxTokens: Int, cancellation: CancellationSignal): String {
        callCount++
        return when {
            prompt.startsWith("List every named character") -> "[]"
            else -> {
                val unresolved = LINE_NUMBER.findAll(prompt).count()
                (1..unresolved).joinToString(",", prefix = "[", postfix = "]") {
                    """{"speaker":null,"confidence":0.0}"""
                }
            }
        }
    }

    companion object {
        private val LINE_NUMBER = Regex("""^\[\d+]""", RegexOption.MULTILINE)
    }
}
