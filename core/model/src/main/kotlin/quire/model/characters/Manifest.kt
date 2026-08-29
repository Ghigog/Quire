package quire.model.characters

import kotlinx.serialization.json.JsonObject

/**
 * The cast of one book, as detected by the upfront scan (QUI-007).
 *
 * This is the project's highest-fanout interface: the scan writes it, attribution reads it,
 * casting turns it into voices, and the drawer shows it. It is frozen deliberately early so
 * those four can be built in parallel against something that will not move under them.
 *
 * **Unknown fields survive a round trip.** A companion app written against a later schema
 * must be able to hand a manifest to an older TTS service and get it back intact — dropping
 * fields we do not recognise would silently destroy a newer app's data. They are kept in
 * [extras] and merged back on write.
 */
data class CharacterManifest(
    val schemaVersion: Int,
    val bookId: String,
    val generatedAt: Long,
    /** The voice for everything nobody says. Never absent: a book is always narrated. */
    val narrator: Character,
    val characters: List<Character>,
    /** Fields this build does not know about, preserved verbatim. */
    val extras: JsonObject = JsonObject(emptyMap()),
) {
    companion object {
        /** Bumped only by a ticket that changes the shape. Readers reject what they cannot parse. */
        const val VERSION = 1
    }
}

data class Character(
    val id: String,
    val displayName: String,
    val aliases: List<String> = emptyList(),
    val gender: Gender = Gender.UNKNOWN,
    val ageBand: AgeBand = AgeBand.UNKNOWN,
    val traits: List<String> = emptyList(),
    /** How sure the scan is that this is a distinct person. 0–1, inclusive. */
    val confidence: Double = 0.0,
    /** Where they first speak or are named, as a `chapter#p3#s1` locator. */
    val firstSeenLocator: String? = null,
    val lineCount: Int = 0,
    val extras: JsonObject = JsonObject(emptyMap()),
)

/**
 * Enums degrade rather than fail.
 *
 * A value this build has never heard of means a newer scan wrote the file, and refusing to
 * read the book would be a far worse outcome than casting one character from a default.
 * Both enums therefore carry `UNKNOWN`, and it is a real value the caster must handle —
 * not a null to be dereferenced.
 */
enum class Gender {
    MALE, FEMALE, NEUTRAL, UNKNOWN;

    companion object {
        fun from(raw: String?) =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class AgeBand {
    CHILD, TEEN, ADULT, ELDER, UNKNOWN;

    companion object {
        fun from(raw: String?) =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

/** Thrown when a manifest cannot be trusted. Names the field path, never a line number. */
class ManifestException(val path: String, message: String) :
    IllegalArgumentException("$path: $message")
