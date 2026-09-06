package quire.voice.foundry

import quire.model.characters.Gender
import quire.model.characters.Voice

/**
 * ADR-0009's foundry: turns a stored [Voice] descriptor into a plan for realising it, and
 * performs the interpolation the plan describes.
 *
 * **What this deliberately does not do.** It never reads or writes `emb_g.weight` on a
 * loaded model — that is a sherpa-onnx / Android binding concern, thin glue over the pure
 * decision made here (CLAUDE.md §9), and there is no device or SDK path to build it against
 * yet (QUI-010 is still Todo). [plan] decides *which* two speakers and *how much* of each;
 * [blend] does the arithmetic once a caller has read those two rows out of a real model.
 */
object Foundry {

    /**
     * Which two speakers to interpolate, and by how much, to realise [target].
     *
     * "A near miss beats no voice" — `spike/slice/Casting.kt`'s existing philosophy, kept
     * here: a target with no usable pool degrades to a single fallback id rather than
     * failing, and a target outside the measured range degrades to the nearest single
     * candidate rather than extrapolating past what was ever measured.
     */
    fun plan(
        target: Voice,
        gender: Gender,
        profile: SpeakerProfile,
        quality: QualityList = QualityList.EMPTY,
    ): BlendPlan {
        val lengthScale = target.lengthScale ?: 1.0
        val fallback = target.speakerId ?: profile.pool(gender).firstOrNull()?.id ?: 0
        val targetF0 = target.targetF0Hz ?: return BlendPlan(fallback, fallback, 0.0, lengthScale)

        val candidates = profile.pool(gender).filterNot { quality.isPoor(it.id) }
            .ifEmpty { profile.pool(gender) }
            .ifEmpty { return BlendPlan(fallback, fallback, 0.0, lengthScale) }

        if (candidates.size == 1 || targetF0 <= candidates.first().f0 || targetF0 >= candidates.last().f0) {
            val nearest = candidates.minBy { kotlin.math.abs(it.f0 - targetF0) }.id
            return BlendPlan(nearest, nearest, 0.0, lengthScale)
        }

        // candidates is sorted ascending by f0 (SpeakerProfile.pool's contract); find the
        // consecutive pair bracketing the target.
        val hiIndex = candidates.indexOfFirst { it.f0 >= targetF0 }
        val lo = candidates[hiIndex - 1]
        val hi = candidates[hiIndex]
        val fraction = ((targetF0 - lo.f0) / (hi.f0 - lo.f0)).coerceIn(0.0, 1.0)
        return BlendPlan(lo.id, hi.id, fraction, lengthScale)
    }

    /**
     * The interpolation QUI-036's probe validated by ear: a linear blend between two
     * measured speaker embeddings, monotonic in [fraction].
     */
    fun blend(a: FloatArray, b: FloatArray, fraction: Double): FloatArray {
        require(a.size == b.size) { "parent embeddings differ in size: ${a.size} vs ${b.size}" }
        val f = fraction.toFloat()
        return FloatArray(a.size) { i -> a[i] * (1 - f) + b[i] * f }
    }
}

/**
 * A realisation plan for one voice: interpolate [fraction] of the way from [parentA]'s
 * embedding to [parentB]'s, at [lengthScale]. `parentA == parentB` (and `fraction == 0.0`)
 * means no blend was possible or needed — the plan degrades to a single real speaker.
 */
data class BlendPlan(
    val parentA: Int,
    val parentB: Int,
    val fraction: Double,
    val lengthScale: Double,
)
