package quire.spike.indexer

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes the slice's book as an EPUB, from the same labelled rows the index is built from.
 *
 * **Why generate it rather than index an existing book.** The vertical slice has to be
 * heard on a real reader, which means the reader needs a file to open — and the index only
 * works if its text is exactly the text the host reads out. Generating both from one
 * fixture makes drift impossible, so anything that goes wrong on device is the matcher's
 * fault and not the fixture's. It also keeps CLAUDE.md §8 satisfied: no real book goes
 * near this repository.
 *
 * EPUB 2 layout — OPF plus NCX, no nav document. Boox's reader takes either, and EPUB 2 is
 * the more conservative choice for a file whose only job is to be opened once.
 */
object Epub {

    fun write(rows: List<Pair<String?, String>>, out: File, title: String) {
        val paragraphs = rows.joinToString("\n") { (_, text) ->
            if (text.startsWith("CHAPTER")) "<h1>${escape(text)}</h1>"
            else "<p>${escape(text)}</p>"
        }
        out.parentFile?.mkdirs()
        ZipOutputStream(out.outputStream()).use { zip ->
            // The mimetype entry must be first and stored uncompressed, or readers that
            // sniff the file rather than parse it will reject the book.
            stored(zip, "mimetype", "application/epub+zip")
            deflated(zip, "META-INF/container.xml", CONTAINER)
            deflated(zip, "OEBPS/content.opf", opf(title))
            deflated(zip, "OEBPS/toc.ncx", ncx(title))
            deflated(zip, "OEBPS/chapter1.xhtml", chapter(title, paragraphs))
        }
    }

    private fun stored(zip: ZipOutputStream, name: String, body: String) {
        val bytes = body.toByteArray()
        zip.putNextEntry(ZipEntry(name).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            crc = CRC32().apply { update(bytes) }.value
        })
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun deflated(zip: ZipOutputStream, name: String, body: String) {
        zip.putNextEntry(ZipEntry(name).apply { method = ZipEntry.DEFLATED })
        zip.write(body.toByteArray())
        zip.closeEntry()
    }

    private fun escape(s: String) =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private const val CONTAINER = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""

    private fun opf(title: String) = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="bookid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>$title</dc:title>
    <dc:creator>Quire fixtures</dc:creator>
    <dc:language>en</dc:language>
    <dc:identifier id="bookid">quire-slice-chapter-one</dc:identifier>
  </metadata>
  <manifest>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
    <item id="ch1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine toc="ncx"><itemref idref="ch1"/></spine>
</package>"""

    private fun ncx(title: String) = """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head><meta name="dtb:uid" content="quire-slice-chapter-one"/></head>
  <docTitle><text>$title</text></docTitle>
  <navMap>
    <navPoint id="ch1" playOrder="1">
      <navLabel><text>Chapter One</text></navLabel>
      <content src="chapter1.xhtml"/>
    </navPoint>
  </navMap>
</ncx>"""

    private fun chapter(title: String, body: String) = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"><head>
  <title>$title</title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
</head><body>
$body
</body></html>"""
}
