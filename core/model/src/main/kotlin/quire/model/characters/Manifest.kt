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
    /** ADR-0007: how this character sounds, or null if nobody has designed it yet. */
    val voice: Voice? = null,
    val extras: JsonObject = JsonObject(emptyMap()),
)

/**
 * ADR-0007 — a voice is a stored description, not a speaker id. Every field is optional:
 * this is the shape the upfront scan (job C) and the drawer write into, not a value that
 * has to exist for casting to fall back on the id alone.
 */
data class Voice(
    val speakerId: Int? = null,
    /** An espeak-ng variant name from the model's bundled data. Stored as written, never validated. */
    val espeakVoice: String? = null,
    val lengthScale: Double? = null,
    val targetF0Hz: Double? = null,
    val description: String? = null,
    val source: VoiceSource = VoiceSource.AUTO,
    val extras: JsonObject = JsonObject(emptyMap()),
)

enum class VoiceSource {
    AUTO, USER;

    companion object {
        fun from(raw: String?) =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AUTO
    }
}

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
