package quire.app.companion.pipeline

import quire.attribution.Heuristic
import quire.model.AttributionResult
import quire.model.Tier
import quire.model.characters.CharacterManifest
import quire.voice.design.VoiceDesigner
import quire.voice.foundry.SpeakerProfile

/**
 * Assigns each character a [quire.model.characters.Voice] descriptor before the manifest is
 * written, so the companion's "ready" screen shows the cast with its voices rather than
 * blank slots the drawer would have to fill in later (QUI-015, V2.0 — not this ticket).
 *
 * Deliberately stops at a descriptor. Turning it into actual audio means reading and writing
 * a loaded model's speaker embedding — QUI-011's job, and it needs a real TTS session
 * (QUI-010) this import pipeline never opens. [Foundry.plan]/[Foundry.blend] realise a
 * descriptor at synthesis time; nothing here calls them.
 *
 * Lines are "explicit" the same way ADR-0006 defines the phrase for [VoiceDesigner]: a Tier 1
 * speech tag, [Heuristic.DIRECT_TAG] confidence, never a pronoun or action-beat guess.
 */
class VoiceCasting(profile: SpeakerProfile = SpeakerProfile(emptyList())) {
    private val designer = VoiceDesigner(profile)

    fun assign(manifest: CharacterManifest, attributed: List<AttributionResult>): CharacterManifest {
        val explicitLines = attributed
            .filter { it.tier == Tier.HEURISTIC && it.confidence >= Heuristic.DIRECT_TAG && it.speakerId != null }
            .groupBy({ it.speakerId!! }, { it.text })

        val cast = manifest.characters.map { character ->
            if (character.voice != null) character
            else designer.design(character, explicitLines[character.id].orEmpty())
                ?.let { character.copy(voice = it) }
                ?: character
        }
        return manifest.copy(characters = cast)
    }
}
