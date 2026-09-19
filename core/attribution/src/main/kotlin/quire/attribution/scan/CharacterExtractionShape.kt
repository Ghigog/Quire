package quire.attribution.scan

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import quire.attribution.slm.JsonShape
import quire.attribution.slm.ShapeMismatch
import quire.model.characters.AgeBand
import quire.model.characters.Gender

/**
 * Parses one scene's character extraction: a JSON array of `{name, aliases, gender,
 * ageBand, traits}`.
 *
 * Defensive rather than trusting, same as [quire.model.characters.ManifestCodec] — a model
 * reply is untrusted input, not a value this build already validated. An entry with no
 * name, or a reply that is not an array at all, fails the whole scene's extraction so
 * [quire.attribution.slm.StructuredCompletion]'s one retry gets a chance at a clean reply
 * rather than this class guessing around a malformed one.
 */
object CharacterExtractionShape : JsonShape<List<ScannedCharacter>> {

    override fun parse(raw: String): List<ScannedCharacter> {
        val array = runCatching { Json.parseToJsonElement(raw.trim()) }.getOrNull() as? JsonArray
            ?: throw ShapeMismatch("expected a JSON array of characters", raw)

        return array.mapIndexed { i, element ->
            val obj = element as? JsonObject
                ?: throw ShapeMismatch("characters[$i] is not an object", raw)
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw ShapeMismatch("characters[$i] has no name", raw)

            ScannedCharacter(
                name = name,
                aliases = strings(obj, "aliases"),
                gender = Gender.from(obj["gender"]?.jsonPrimitive?.contentOrNull),
                ageBand = AgeBand.from(obj["ageBand"]?.jsonPrimitive?.contentOrNull),
                traits = strings(obj, "traits"),
            )
        }
    }

    private fun strings(obj: JsonObject, key: String): List<String> =
        obj[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
}
