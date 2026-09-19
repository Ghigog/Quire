package quire.attribution.slm.attribution

import quire.attribution.slm.StructuredCompletion
import quire.attribution.slm.StructuredResult
import quire.model.AttributionResult
import quire.model.Thresholds
import quire.model.Tier

/**
 * QUI-009: resolves what Tier 1 declines, one SLM call per scene rather than per line —
 * ADR-0006's amendment, because a per-line budget cannot fit QUI-007's 30-minute import
 * window and a model shown one quotation in isolation knows less than the reader does.
 *
 * **Takes a scene's already-attributed lines, not attribution internals.** The caller
 * groups paragraphs into scenes (`core:attribution`'s own [quire.attribution.scenes]) and
 * runs Tier 1 first; this class only sees the resulting [AttributionResult]s for one scene.
 *
 * **Never blocks playback.** [cached] is a pure lookup that returns null for anything not
 * yet resolved — the caller reads it on the playback path and falls back to the Narrator
 * on a miss. [attribute] is the only method that calls the model, meant to run ahead of
 * playback, and it fills [cached] as it goes so a re-read never re-infers.
 */
class SceneAttributor(
    private val slm: StructuredCompletion,
    private val knownCharacterIds: Set<String>,
) {
    private val cache = mutableMapOf<String, AttributionResult>()

    /** The cached result for [locator], or null if it has never been attributed. */
    fun cached(locator: String): AttributionResult? = cache[locator]

    /**
     * Attributes every line in [sceneLines] that Tier 1 left as [Tier.NONE] and is not
     * already cached, and returns the scene with every line resolved — from cache, from
     * this call, or, failing both, Tier 1's own answer unchanged.
     */
    fun attribute(sceneLines: List<AttributionResult>, maxTokens: Int = MAX_TOKENS): List<AttributionResult> {
        val current = sceneLines.map { cache[it.locator] ?: it }
        val known = current.withIndex().mapNotNull { (i, r) -> r.speakerId?.let { i to it } }
        val toResolve = current.withIndex().filter { (_, r) -> r.tier == Tier.NONE }
        if (toResolve.isEmpty()) return current

        val activeSpeakers = known.map { it.second }.toSet()
        val answers = ask(current, toResolve.map { it.value }, maxTokens)

        for ((slot, indexed) in toResolve.withIndex()) {
            val (index, line) = indexed
            cache[line.locator] = resolve(line, answers?.getOrNull(slot), activeSpeakers, known, index)
        }
        return current.map { cache[it.locator] ?: it }
    }

    /**
     * @return the model's aligned answers, or null if the reply failed or — per the
     *   Requirements — named a different number of speakers than there are quotations.
     *   Either way the whole scene drops to Tier 3 rather than shifting one line's answer
     *   onto another's quotation.
     */
    private fun ask(sceneLines: List<AttributionResult>, unresolved: List<AttributionResult>, maxTokens: Int): List<SceneAnswer>? =
        when (val result = slm.complete(prompt(sceneLines, unresolved), SceneAnswerShape, maxTokens)) {
            is StructuredResult.Success -> result.value.takeIf { it.size == unresolved.size }
            is StructuredResult.Failure -> null
        }

    private fun resolve(
        line: AttributionResult,
        answer: SceneAnswer?,
        activeSpeakers: Set<String>,
        known: List<Pair<Int, String>>,
        index: Int,
    ): AttributionResult {
        // The model's own "nobody speaks this" is stronger evidence than a Tier 3 guess.
        if (answer != null && answer.speakerId == null) {
            return line.copy(speakerId = null, confidence = 0.0, tier = Tier.NARRATOR, evidence = "scene SLM: no speaker")
        }
        if (answer != null && answer.confidence >= Thresholds.SLM_MIN && answer.speakerId in knownCharacterIds) {
            return line.copy(speakerId = answer.speakerId, confidence = answer.confidence, tier = Tier.SLM, evidence = "scene SLM")
        }
        // Below SLM_MIN but not hopeless, or the whole scene's answer was unusable
        // (answer == null): one attempt at Tier 3 before giving up to the Narrator.
        val worthTrying = answer == null || answer.confidence >= Thresholds.NARRATOR_FLOOR
        if (worthTrying) {
            SceneInference.speakerFor(index, known, activeSpeakers)?.let { speaker ->
                return line.copy(
                    speakerId = speaker, confidence = answer?.confidence ?: Thresholds.NARRATOR_FLOOR,
                    tier = Tier.SCENE, evidence = "scene inference, two-party",
                )
            }
        }
        return line.copy(speakerId = null, confidence = 0.0, tier = Tier.NARRATOR, evidence = "below confidence floor")
    }

    private fun prompt(sceneLines: List<AttributionResult>, unresolved: List<AttributionResult>) = buildString {
        appendLine("This scene's lines, in order. The cast: ${knownCharacterIds.sorted().joinToString(", ")}.")
        appendLine("For each numbered line, name its speaker from the cast, or null if it is")
        appendLine("narration or spoken by nobody in the cast. Reply with a JSON array of")
        appendLine("exactly ${unresolved.size} objects, in the same order as the numbered lines:")
        appendLine("""[{"speaker": name-or-null, "confidence": 0-1}, ...]""")
        appendLine()
        var n = 0
        for (line in sceneLines) {
            if (line.tier == Tier.NONE) {
                n++
                appendLine("[$n] ${line.text}")
            } else {
                appendLine(line.text)
            }
        }
    }

    companion object {
        /** A scene rarely needs more than this to answer every unresolved line at once. */
        const val MAX_TOKENS = 800
    }
}
