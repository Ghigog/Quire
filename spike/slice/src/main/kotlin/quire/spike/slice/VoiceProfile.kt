package quire.spike.slice

import quire.model.characters.Gender

/**
 * What each of a model's voices actually sounds like.
 *
 * `libritts_r` exposes 904 speakers as bare integers with no indication of who they are, so
 * casting by id is a coin toss per character — heard on device, where the narrator came out
 * a woman, Sarah a man and Thomas a woman.
 *
 * The table is measured, not looked up: `spike/hostbench/voiceprofile.py` synthesises one
 * sentence per speaker and takes the median fundamental frequency. That works for any model
 * in the zoo rather than only ones with published metadata, and it describes the voice as
 * this engine renders it rather than as a corpus once described it.
 */
class VoiceProfile(voices: List<Voice>) {

    data class Voice(val id: Int, val f0: Double, val gender: Gender)

    /** Ids per gender, ordered by pitch, so "spread these apart" has a meaning. */
    private val pools: Map<Gender, List<Int>> =
        voices.groupBy { it.gender }
            .mapValues { (_, group) -> group.sortedBy { it.f0 }.map { it.id } }

    private val f0ById: Map<Int, Double> = voices.associate { it.id to it.f0 }

    fun pool(gender: Gender): List<Int> = pools[gender].orEmpty()

    /** Measured pitch of one voice, for judging whether two of them are far enough apart. */
    fun f0Of(id: Int): Double? = f0ById[id]

    val size: Int get() = pools.values.sumOf { it.size }

    companion object {
        /**
         * Parse the TSV `voiceprofile.py` emits: `speaker`, `f0_hz`, `voice`.
         *
         * Anything the measurement could not place — an unvoiced result, or a pitch too
         * close to the boundary to call — is dropped rather than guessed. A voice left out
         * of the pools costs nothing when there are hundreds; a wrongly sexed one is heard
         * immediately.
         */
        fun parse(lines: Sequence<String>): VoiceProfile {
            val voices = lines
                .filterNot { it.isBlank() || it.startsWith("#") || it.startsWith("speaker\t") }
                .mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size < 3) return@mapNotNull null
                    val id = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                    val f0 = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
                    val gender = when (parts[2].trim()) {
                        "male" -> Gender.MALE
                        "female" -> Gender.FEMALE
                        else -> return@mapNotNull null
                    }
                    Voice(id, f0, gender)
                }
                .toList()
            return VoiceProfile(voices)
        }
    }
}
