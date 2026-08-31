package quire.epub

import org.jsoup.Jsoup
import quire.model.Paragraph
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

/**
 * Pulls the prose out of an EPUB, once, at import.
 *
 * **Not a reader.** Quire never renders a book — the reader's own app does that, and
 * QUI-002's reader shell is deferred. This opens the file, extracts paragraphs in reading
 * order so the cast can be worked out and an index built, and is finished with it. What
 * the app keeps afterwards is the note it wrote itself, not the book.
 *
 * Deliberately not Readium: encryption, fallback chains, media overlays and CSS injection
 * all matter to something that displays a book and none of them matter to something that
 * reads its words once. If an encrypted book ever needs importing, that is its own ticket.
 *
 * Locators are `spineHref#p{index}`. The shape matters more than the format — everything
 * downstream keys off it.
 */
object EpubText {

    fun paragraphs(epub: File): List<Paragraph> {
        ZipFile(epub).use { zip ->
            val opfPath = opfPath(zip)
            val opfDir = opfPath.substringBeforeLast('/', "")
            val opf = Jsoup.parse(
                zip.text(opfPath) ?: error("EPUB names a package at $opfPath that is not in the archive"),
                "", Parser.xmlParser(),
            )

            val hrefById = opf.select("manifest > item").associate {
                it.attr("id") to it.attr("href")
            }
            val spine = opf.select("spine > itemref").mapNotNull { hrefById[it.attr("idref")] }

            var index = 0
            return spine.flatMapIndexed { chapterIndex, href ->
                val path = resolve(opfDir, href)
                val doc = Jsoup.parse(zip.text(path) ?: return@flatMapIndexed emptyList(), "")
                doc.select("p, h1, h2, h3, h4, blockquote").mapNotNull { el ->
                    val text = el.text().trim()
                    if (text.isEmpty()) null
                    else Paragraph("$href#p${index}", text, chapterIndex, index++)
                }
            }
        }
    }

    private fun opfPath(zip: ZipFile): String {
        val container = zip.text("META-INF/container.xml")
            ?: error("not an EPUB: no META-INF/container.xml")
        return Jsoup.parse(container, "", Parser.xmlParser())
            .selectFirst("rootfile")?.attr("full-path")
            ?: error("not an EPUB: container.xml names no rootfile")
    }

    private fun resolve(dir: String, href: String) = if (dir.isEmpty()) href else "$dir/$href"

    private fun ZipFile.text(path: String): String? =
        getEntry(path)?.let { getInputStream(it).use { s -> s.readBytes().toString(Charsets.UTF_8) } }
}
