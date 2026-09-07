package quire.attribution.scenes

/**
 * A scene: a contiguous run of a book's paragraphs, as a half-open range over
 * [quire.model.Paragraph.index].
 *
 * A range rather than a paragraph list because scenes partition the whole book — every
 * paragraph belongs to exactly one — and a range says that without copying anything.
 */
data class Scene(val start: Int, val endExclusive: Int) {
    init {
        require(start < endExclusive) { "empty scene [$start, $endExclusive)" }
    }

    val size: Int get() = endExclusive - start

    operator fun contains(paragraphIndex: Int) = paragraphIndex in start until endExclusive
}
