package quire.attribution.scenes

import quire.model.Paragraph

/**
 * Partitions a book's paragraphs into scenes (QUI-038, ADR-0006).
 *
 * QUI-009's model is prompted once per scene rather than once per line, so a scene has to
 * be a thing this codebase can name before that prompt can exist. Every signal here is
 * structural — no model reads the prose for meaning:
 *
 * - a chapter boundary, from [Paragraph.chapterIndex]
 * - an explicit scene-break paragraph: a line of asterisks, dashes or nothing at all
 * - a hard paragraph-count backstop, for a chapter with no break markup at all
 *
 * A re-import must not reshuffle attribution, so this is a pure function of the paragraph
 * list: same input, same scenes, every time.
 */
object SceneSegmenter {

    /**
     * A run of paragraphs this long with no other signal is cut anyway.
     *
     * A backstop, not a target — ADR-0006 guesses 60-120 scenes per novel, which on a
     * 100k-word book is on the order of a few hundred paragraphs per scene. This is set well
     * above that so it only fires on the chapter that genuinely has no break markup at all.
     */
    const val DEFAULT_MAX_PARAGRAPHS = 400

    /**
     * A line of nothing but asterisks, dashes (of any width) or whitespace — the
     * typographic conventions for "time has passed, the scene has changed" that carry no
     * words a segmenter could get wrong by misreading them.
     */
    private val BREAK = Regex("^[\\s*·•#~=_.,\\-‐‑‒–—―]+$")

    /** True for a paragraph that marks a scene break rather than holding any prose. */
    fun isBreak(text: String): Boolean {
        val t = text.trim()
        return t.isEmpty() || BREAK.matches(t)
    }

    fun segment(paragraphs: List<Paragraph>, maxParagraphs: Int = DEFAULT_MAX_PARAGRAPHS): List<Scene> {
        if (paragraphs.isEmpty()) return emptyList()

        val scenes = mutableListOf<Scene>()
        var start = paragraphs.first().index
        var count = 0

        for (i in paragraphs.indices) {
            val paragraph = paragraphs[i]
            val next = paragraphs.getOrNull(i + 1)
            count++
            if (next == null) continue

            val chapterChange = next.chapterIndex != paragraph.chapterIndex
            // A run of several break paragraphs in a row — two lines of asterisks, or a
            // stack of empty ones — is one boundary, not one per marker: only cut after the
            // last of the run, so the run itself never becomes a scene of its own.
            val breakHere = isBreak(paragraph.text) && !isBreak(next.text)
            val overLong = count >= maxParagraphs

            if (chapterChange || breakHere || overLong) {
                scenes += Scene(start, next.index)
                start = next.index
                count = 0
            }
        }
        scenes += Scene(start, paragraphs.last().index + 1)
        return scenes
    }
}
