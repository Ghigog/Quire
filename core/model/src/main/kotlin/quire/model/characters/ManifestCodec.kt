package quire.model.characters

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Reads and writes `characters.json`.
 *
 * Hand-mapped from the JSON tree rather than driven by `@Serializable`, for two reasons the
 * ticket asks for and annotations do not give: unknown fields have to be *kept* rather than
 * ignored, and a rejection has to name the offending field path so a broken manifest is
 * diagnosable from the message alone.
 *
 * **Loading is all or nothing.** A manifest that fails validation throws; no partial object
 * is ever handed back. A half-read cast is worse than none, because the reader would hear
 * some characters in the right voice and the rest in the narrator's and have no way to tell
 * which.
 */
object ManifestCodec {

    private val json = Json { prettyPrint = true; prettyPrintIndent = "  " }

    /** Keys this build owns. Anything else is somebody's future field and is preserved. */
    private val MANIFEST_KEYS =
        setOf("schemaVersion", "bookId", "generatedAt", "narrator", "characters")
    private val CHARACTER_KEYS = setOf(
        "id", "displayName", "aliases", "gender", "ageBand",
        "traits", "confidence", "firstSeenLocator", "lineCount", "voice",
    )
    private val VOICE_KEYS = setOf(
        "speakerId", "espeakVoice", "lengthScale", "targetF0Hz", "description", "source",
    )

    fun decode(text: String): CharacterManifest {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw ManifestException("$", "not a JSON object") }

        val version = root.int("schemaVersion", "schemaVersion")
        if (version > CharacterManifest.VERSION) {
            throw ManifestException(
                "schemaVersion",
                "manifest is version $version, this build reads ${CharacterManifest.VERSION}",
            )
        }

        val characters = root["characters"]?.let { element ->
            val array = element as? JsonArray
                ?: throw ManifestException("characters", "expected an array")
            array.mapIndexed { i, item ->
                val obj = item as? JsonObject
                    ?: throw ManifestException("characters[$i]", "expected an object")
                character(obj, "characters[$i]")
            }
        }.orEmpty()

        val narrator = root["narrator"]?.let {
            character(it as? JsonObject ?: throw ManifestException("narrator", "expected an object"), "narrator")
        } ?: throw ManifestException("narrator", "missing")

        val ids = characters.map { it.id }
        ids.groupingBy { it }.eachCount().forEach { (id, n) ->
            // Duplicate ids would make casting non-deterministic in a way that only shows
            // up as one character occasionally speaking in another's voice.
            if (n > 1) throw ManifestException("characters[].id", "duplicate id '$id'")
        }

        return CharacterManifest(
            schemaVersion = version,
            bookId = root.string("bookId", "bookId"),
            generatedAt = root["generatedAt"]?.jsonPrimitive?.longOrNull
                ?: throw ManifestException("generatedAt", "missing or not a number"),
            narrator = narrator,
            characters = characters,
            extras = root.extrasOutside(MANIFEST_KEYS),
        )
    }

    fun encode(manifest: CharacterManifest): String = json.encodeToString(
        JsonObject.serializer(),
        buildMap {
            put("schemaVersion", JsonPrimitive(manifest.schemaVersion))
            put("bookId", JsonPrimitive(manifest.bookId))
            put("generatedAt", JsonPrimitive(manifest.generatedAt))
            put("narrator", encode(manifest.narrator))
            put("characters", JsonArray(manifest.characters.map(::encode)))
            // Unknown keys last so a future field can never shadow one we own.
            putAll(manifest.extras)
        }.let(::JsonObject),
    )

    private fun encode(character: Character) = JsonObject(
        buildMap {
            put("id", JsonPrimitive(character.id))
            put("displayName", JsonPrimitive(character.displayName))
            put("aliases", JsonArray(character.aliases.map(::JsonPrimitive)))
            put("gender", JsonPrimitive(character.gender.name.lowercase()))
            put("ageBand", JsonPrimitive(character.ageBand.name.lowercase()))
            put("traits", JsonArray(character.traits.map(::JsonPrimitive)))
            put("confidence", JsonPrimitive(character.confidence))
            character.firstSeenLocator?.let { put("firstSeenLocator", JsonPrimitive(it)) }
            put("lineCount", JsonPrimitive(character.lineCount))
            character.voice?.let { put("voice", encode(it)) }
            putAll(character.extras)
        },
    )

    private fun encode(voice: Voice) = JsonObject(
        buildMap {
            voice.speakerId?.let { put("speakerId", JsonPrimitive(it)) }
            voice.espeakVoice?.let { put("espeakVoice", JsonPrimitive(it)) }
            voice.lengthScale?.let { put("lengthScale", JsonPrimitive(it)) }
            voice.targetF0Hz?.let { put("targetF0Hz", JsonPrimitive(it)) }
            voice.description?.let { put("description", JsonPrimitive(it)) }
            put("source", JsonPrimitive(voice.source.name.lowercase()))
            putAll(voice.extras)
        },
    )

    private fun character(obj: JsonObject, path: String): Character {
        val confidence = obj["confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        if (confidence !in 0.0..1.0) {
            throw ManifestException("$path.confidence", "must be between 0 and 1, was $confidence")
        }
        val lineCount = obj["lineCount"]?.jsonPrimitive?.intOrNull ?: 0
        if (lineCount < 0) throw ManifestException("$path.lineCount", "must not be negative")

        return Character(
            id = obj.string("id", "$path.id"),
            displayName = obj.string("displayName", "$path.displayName"),
            aliases = obj.strings("aliases", "$path.aliases"),
            // An unrecognised value is a newer scan's, not a corruption: degrade quietly.
            gender = Gender.from(obj["gender"]?.jsonPrimitive?.contentOrNullSafe()),
            ageBand = AgeBand.from(obj["ageBand"]?.jsonPrimitive?.contentOrNullSafe()),
            traits = obj.strings("traits", "$path.traits"),
            confidence = confidence,
            firstSeenLocator = obj["firstSeenLocator"]?.jsonPrimitive?.contentOrNullSafe(),
            lineCount = lineCount,
            voice = obj["voice"]?.let {
                voice(it as? JsonObject ?: throw ManifestException("$path.voice", "expected an object"))
            },
            extras = obj.extrasOutside(CHARACTER_KEYS),
        )
    }

    private fun voice(obj: JsonObject): Voice = Voice(
        speakerId = obj["speakerId"]?.jsonPrimitive?.intOrNull,
        espeakVoice = obj["espeakVoice"]?.jsonPrimitive?.contentOrNullSafe(),
        lengthScale = obj["lengthScale"]?.jsonPrimitive?.doubleOrNull,
        targetF0Hz = obj["targetF0Hz"]?.jsonPrimitive?.doubleOrNull,
        description = obj["description"]?.jsonPrimitive?.contentOrNullSafe(),
        // An unrecognised value is a newer writer's, not a corruption: degrade quietly.
        source = VoiceSource.from(obj["source"]?.jsonPrimitive?.contentOrNullSafe()),
        extras = obj.extrasOutside(VOICE_KEYS),
    )

    private fun JsonObject.extrasOutside(known: Set<String>) =
        JsonObject(filterKeys { it !in known })

    private fun JsonObject.string(key: String, path: String): String =
        this[key]?.jsonPrimitive?.contentOrNullSafe()
            ?: throw ManifestException(path, "missing or not a string")

    private fun JsonObject.int(key: String, path: String): Int =
        this[key]?.jsonPrimitive?.intOrNull
            ?: throw ManifestException(path, "missing or not an integer")

    private fun JsonObject.strings(key: String, path: String): List<String> {
        val element = this[key] ?: return emptyList()
        val array = element as? JsonArray ?: throw ManifestException(path, "expected an array")
        return array.mapIndexed { i, item ->
            (item as? JsonPrimitive)?.contentOrNullSafe()
                ?: throw ManifestException("$path[$i]", "expected a string")
        }
    }

    /** The string content, treating an unquoted `null` as absent rather than as "null". */
    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (!isString && content == "null") null else content
}
