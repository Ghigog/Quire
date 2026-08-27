package quire.spike

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpubTest {

    private fun minimalEpub(dir: File): File {
        val f = File(dir, "test.epub")
        ZipOutputStream(f.outputStream()).use { zip ->
            fun put(name: String, body: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(body.toByteArray()); zip.closeEntry()
            }
            put("mimetype", "application/epub+zip")
            put("META-INF/container.xml", """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles><rootfile full-path="OEBPS/content.opf"
                    media-type="application/oebps-package+xml"/></rootfiles>
                </container>""".trimIndent())
            put("OEBPS/content.opf", """
                <?xml version="1.0"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                  <manifest>
                    <item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine><itemref idref="c1"/><itemref idref="c2"/></spine>
                </package>""".trimIndent())
            put("OEBPS/ch1.xhtml", """
                <html><body><h1>One</h1>
                <p>"I know," said Sarah.</p><p></p><p>The rain kept on.</p></body></html>""".trimIndent())
            put("OEBPS/ch2.xhtml", """
                <html><body><p>Thomas said, "It is late."</p></body></html>""".trimIndent())
        }
        return f
    }

    @Test
    fun `reads spine order, skips empties, and produces addressable locators`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "quire-epub-test").apply { mkdirs() }
        val paragraphs = Epub.paragraphs(minimalEpub(dir))

        assertEquals(listOf("One", "\"I know,\" said Sarah.", "The rain kept on.", "Thomas said, \"It is late.\""),
            paragraphs.map { it.text })
        assertEquals(listOf(0, 0, 0, 1), paragraphs.map { it.chapterIndex })
        assertTrue(paragraphs.map { it.locator }.toSet().size == paragraphs.size, "locators must be unique")
        assertEquals("ch2.xhtml#p3", paragraphs.last().locator)

        // The stream feeds attribution unchanged — that is the seam this test protects.
        val roster = Tier1.bootstrapRoster(paragraphs)
        assertEquals(setOf("Sarah", "Thomas"), roster.fromTags.keys)
    }
}
