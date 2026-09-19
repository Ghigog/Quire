package quire.tts.casting

import kotlinx.serialization.Serializable

/** Who chose a character's voice. A [USER] entry is never touched by [Caster.cast] again. */
@Serializable
enum class AssignmentSource { AUTO, USER }

/**
 * One character's voice: an id from the engine's speaker range, plus a small rate offset
 * used only when [Caster] had to reuse an id among characters who share a scene — see its
 * KDoc. Two assignments are "the same voice" to a listener only when both fields match.
 */
@Serializable
data class VoiceAssignment(
    val voiceId: Int,
    val rate: Double = 1.0,
    val source: AssignmentSource = AssignmentSource.AUTO,
)

/** A book's whole cast: the narrator plus every character [Caster] or a reader has voiced. */
@Serializable
data class Cast(
    val bookId: String,
    val narratorVoiceId: Int,
    val voices: Map<String, VoiceAssignment>,
)
