package quire.attribution.slm.attribution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import quire.attribution.slm.JsonShape
import quire.attribution.slm.ShapeMismatch

/**
 * One quotation's answer from a scene-level SLM call: who speaks it, or null for narration,
 * and how sure the model is. Aligned by position to the scene's unresolved quotations — see
 * [SceneAttributor], which drops the whole scene to Tier 3 rather than trust a misaligned list.
 */
data class SceneAnswer(val speakerId: String?, val confidence: Double)

/** Parses a scene reply: a JSON array of `{"speaker": name-or-null, "confidence": 0-1}`. */
object SceneAnswerShape : JsonShape<List<SceneAnswer>> {

    override fun parse(raw: String): List<SceneAnswer> {
        val array = runCatching { Json.parseToJsonElement(raw.trim()) }.getOrNull() as? JsonArray
            ?: throw ShapeMismatch("expected a JSON array of answers", raw)

        return array.mapIndexed { i, element ->
            val obj = element as? JsonObject ?: throw ShapeMismatch("answers[$i] is not an object", raw)
            val speakerElement = obj["speaker"]
            val speaker = if (speakerElement == null || speakerElement is JsonNull) {
                null
            } else {
                speakerElement.jsonPrimitive.content
            }
            val confidence = obj["confidence"]?.jsonPrimitive?.doubleOrNull
                ?: throw ShapeMismatch("answers[$i] has no numeric confidence", raw)
            SceneAnswer(speaker, confidence)
        }
    }
}
