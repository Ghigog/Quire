package quire.model.characters

import java.io.File

/**
 * Per-book manifest storage.
 *
 * One file per book under [root], named by book id, because the companion app writes each
 * book independently and a single combined file would make two concurrent imports fight
 * over the same bytes.
 *
 * Writes go to a temporary file and are renamed into place. A manifest half-written when
 * the process died would fail validation on the next read and lose the book's cast; the
 * rename is atomic on every filesystem Android uses, so a reader sees either the old
 * manifest or the new one.
 */
class ManifestStore(private val root: File) {

    fun write(manifest: CharacterManifest) {
        root.mkdirs()
        val target = fileFor(manifest.bookId)
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(ManifestCodec.encode(manifest))
        check(temp.renameTo(target)) { "could not replace ${target.path}" }
    }

    /** The manifest for [bookId], or null if this book has never been scanned. */
    fun read(bookId: String): CharacterManifest? {
        val file = fileFor(bookId)
        return if (file.exists()) ManifestCodec.decode(file.readText()) else null
    }

    fun bookIds(): List<String> =
        root.listFiles { f: File -> f.isFile && f.name.endsWith(SUFFIX) }
            ?.map { it.name.removeSuffix(SUFFIX) }
            ?.map(::decodeName)
            ?.sorted()
            .orEmpty()

    private fun fileFor(bookId: String) = File(root, encodeName(bookId) + SUFFIX)

    /**
     * Book ids come from a fingerprint and should be hex, but nothing in the type system
     * says so, and a `/` in one would write outside [root]. Percent-encode anything that
     * is not plainly safe rather than trusting the caller.
     */
    private fun encodeName(bookId: String) = buildString {
        for (c in bookId) {
            if (c.isLetterOrDigit() || c == '-' || c == '_') append(c)
            else append('%').append("%02X".format(c.code))
        }
    }

    private fun decodeName(name: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < name.length) {
            if (name[i] == '%' && i + 2 < name.length) {
                out.append(name.substring(i + 1, i + 3).toInt(16).toChar())
                i += 3
            } else {
                out.append(name[i])
                i++
            }
        }
        return out.toString()
    }

    private companion object {
        const val SUFFIX = ".characters.json"
    }
}
