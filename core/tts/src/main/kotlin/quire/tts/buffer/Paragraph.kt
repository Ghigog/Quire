package quire.tts.buffer

/** One paragraph of the open book, addressed by its position in reading order. */
data class Paragraph(val index: Int, val text: String)

/** Where the book's paragraphs come from, keyed by reading-order index. */
fun interface ParagraphSource {
    /** The paragraph at [index], or `null` once the book is exhausted. */
    fun at(index: Int): Paragraph?
}
