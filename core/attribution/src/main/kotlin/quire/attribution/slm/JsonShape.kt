package quire.attribution.slm

/**
 * Parses one raw completion into [T], throwing [ShapeMismatch] when it doesn't match.
 *
 * A functional interface rather than a dependency on a particular JSON library: the shape
 * QUI-007 wants (a character manifest) and the shape QUI-009 wants (a per-scene speaker
 * list, per ADR-0006 §3) are that ticket's business, not this runtime's. Whatever they use
 * to parse — kotlinx.serialization, hand-rolled — plugs in here unchanged.
 */
fun interface JsonShape<T> {
    fun parse(raw: String): T
}

/** Thrown by a [JsonShape] when [raw] does not match the expected structure. */
class ShapeMismatch(message: String, val raw: String) : Exception(message)
