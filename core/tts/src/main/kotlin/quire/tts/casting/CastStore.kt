package quire.tts.casting

import java.io.File
import kotlinx.serialization.json.Json

/**
 * Per-book cast storage. Same atomic-write shape as QUI-005's `ManifestStore` and for the
 * same reason: a cast half-written when the process dies must not surface as a reshuffled
 * set of voices on the next launch, silently breaking the "a re-import doesn't reshuffle
 * the reader's voices" promise this ticket makes.
 *
 * Plain `@Serializable` rather than [quire.model.characters.ManifestCodec]'s hand-mapped,
 * unknown-fields-preserved style: unlike `characters.json`, nothing outside this module
 * writes or reads a cast file, so there is no other writer's future field to protect.
 */
class CastStore(private val root: File) {

    private val json = Json { prettyPrint = true }

    fun write(cast: Cast) {
        root.mkdirs()
        val target = fileFor(cast.bookId)
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(json.encodeToString(Cast.serializer(), cast))
        check(temp.renameTo(target)) { "could not replace ${target.path}" }
    }

    /** The cast for [bookId], or null if it has never been cast. */
    fun read(bookId: String): Cast? {
        val file = fileFor(bookId)
        return if (file.exists()) json.decodeFromString(Cast.serializer(), file.readText()) else null
    }

    private fun fileFor(bookId: String) = File(root, "${encodeName(bookId)}$SUFFIX")

    /**
     * Book ids come from a fingerprint and should be hex, but nothing says so, and a `/`
     * in one would write outside [root] — same guard `ManifestStore` applies.
     */
    private fun encodeName(bookId: String) = buildString {
        for (c in bookId) {
            if (c.isLetterOrDigit() || c == '-' || c == '_') append(c)
            else append('%').append("%02X".format(c.code))
        }
    }

    private companion object {
        const val SUFFIX = ".cast.json"
    }
}
