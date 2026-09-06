package quire.voice.foundry

import quire.model.characters.Gender

/**
 * What each of the engine's speakers measures as, by pitch.
 *
 * Ported from `spike/slice/VoiceProfile.kt` rather than depended on: spike code is never a
 * dependency of `core/` (CLAUDE.md §3), and this is small enough that porting costs less
 * than the coupling would. The TSV shape — `speaker`, `f0_hz`, `voice` — is
 * `spike/hostbench/voiceprofile.py`'s output and is shared between the two call sites.
 */
class SpeakerProfile(voices: List<Voice>) {

    data class Voice(val id: Int, val f0: Double, val gender: Gender)

    /** Ids per gender, ordered by pitch, so "nearest to a target" has a meaning. */
    private val pools: Map<Gender, List<Voice>> =
        voices.groupBy { it.gender }.mapValues { (_, group) -> group.sortedBy { it.f0 } }

    private val f0ById: Map<Int, Double> = voices.associate { it.id to it.f0 }

    fun pool(gender: Gender): List<Voice> = pools[gender].orEmpty()

    fun f0Of(id: Int): Double? = f0ById[id]

    /** Median measured pitch for a gender's pool, or null if it is empty. */
    fun medianF0(gender: Gender): Double? {
        val f0s = pool(gender).map { it.f0 }.sorted()
        if (f0s.isEmpty()) return null
        val n = f0s.size
        return if (n % 2 == 1) f0s[n / 2] else (f0s[n / 2 - 1] + f0s[n / 2]) / 2
    }

    companion object {
        fun parse(lines: Sequence<String>): SpeakerProfile {
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
            return SpeakerProfile(voices)
        }
    }
}

/**
 * Which speakers a listen has flagged unfit to be chosen at all, ADR-0009's quality
 * signal — a list of listened exceptions, not a formula. QUI-037's Worklog records why two
 * computed proxies were tried and rejected; the mechanism here is the one that survived.
 */
class QualityList(private val poor: Set<Int>) {

    fun isPoor(id: Int): Boolean = id in poor

    companion object {
        val EMPTY = QualityList(emptySet())

        fun parse(lines: Sequence<String>): QualityList {
            val poor = lines
                .filterNot { it.isBlank() || it.startsWith("#") || it.startsWith("speaker\t") }
                .mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size < 2) return@mapNotNull null
                    val id = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                    if (parts[1].trim() != "poor") return@mapNotNull null
                    id
                }
                .toSet()
            return QualityList(poor)
        }
    }
}
