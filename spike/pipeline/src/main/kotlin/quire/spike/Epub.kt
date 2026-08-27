package quire.spike

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

/**
 * Minimal EPUB reader for the spike (QUI-018).
 *
 * This is *not* what ships: QUI-002 uses the Readium toolkit, which handles the
 * encryption, fallback chains, media overlays and CSS injection deliberately ignored
 * here. This exists so the attribution work can be driven from real books on a desktop
 * JVM without pulling in an Android dependency.
 *
 * Locators are `spineHref#p{index}`, a stand-in for a real Readium locator. The shape
 * matters more than the format: everything downstream keys off it.
 */
object Epub {

    fun paragraphs(epub: File): List<ParagraphUnit> {
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
                    else ParagraphUnit("$href#p${index}", text, chapterIndex, index++)
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
