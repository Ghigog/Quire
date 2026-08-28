package quire.index

import java.text.Normalizer as JNormalizer

/**
 * Sentence splitting and text normalisation.
 *
 * This is deliberately the *only* implementation: the companion app uses it to build the
 * index and the TTS service uses it to match against the index. If the two ever drifted,
 * nothing would match and the failure would look like a matcher bug rather than a
 * normalisation bug (QUI-021).
 */
object Normalizer {

    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "st", "sr", "jr", "capt", "col", "gen",
        "lt", "sgt", "rev", "hon", "esq", "vs", "etc",
    )

    /** Characters stripped outright: quotes, footnote daggers, soft hyphens, marks. */
    private val STRIPPED = setOf(
        '"', '“', '”', '‘', '’', '«', '»',
        '­', '​', '﻿', '*', '†', '‡',
    )

    /**
     * Split text into sentences on terminal punctuation.
     *
     * Mirrors what the host does. A fragment with no terminal punctuation — a chapter
     * heading — is returned as its own sentence here, even though NeoReader will glue it
     * to whatever follows; the matcher handles that by matching a *run* of entries.
     */
    fun sentences(text: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for ((i, c) in text.withIndex()) {
            sb.append(c)
            if (c != '.' && c != '!' && c != '?') continue
            val lastWord = sb.toString().trimEnd('.', '!', '?', '"', '”')
                .takeLastWhile { !it.isWhitespace() }.lowercase()
            if (c == '.' && lastWord in ABBREVIATIONS) continue
            // Let a closing quote or bracket ride along with the sentence it ends.
            val next = text.getOrNull(i + 1)
            if (next != null && next !in " \n\r\t") continue
            out += sb.toString().trim()
            sb.clear()
        }
        if (sb.isNotBlank()) out += sb.toString().trim()
        return out.filter { it.isNotBlank() }
    }

    /**
     * Reduce a sentence to the form both sides compare on: NFKC, lowercase, quotes and
     * footnote markers gone, punctuation gone, whitespace collapsed.
     *
     * Punctuation is dropped entirely because hosts are inconsistent about which of it
     * they pass through, and because it carries no information we need once the sentence
     * has already been split on it.
     */
    fun normalize(text: String): String {
        val nfkc = JNormalizer.normalize(text, JNormalizer.Form.NFKC)
        val sb = StringBuilder(nfkc.length)
        for (c in nfkc) {
            when {
                c in STRIPPED -> Unit
                c.isLetterOrDigit() -> sb.append(c.lowercaseChar())
                c == '\'' -> sb.append(c) // don't turn "don't" into "don t"
                c.isWhitespace() || c == '-' || c == '—' -> sb.append(' ')
                else -> Unit // all other punctuation dropped
            }
        }
        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }

    /** Split then normalise, dropping anything that normalises to nothing. */
    fun normalizedSentences(text: String): List<String> =
        sentences(text).map(::normalize).filter { it.isNotEmpty() }

    /**
     * How many leading words of an entry are indexed for relocation lookups.
     *
     * A whole-chunk hash cannot be the key: hosts split and glue entries, so a chunk is
     * rarely equal to any one entry. Its opening words, though, always identify the entry
     * it starts on. Writer and matcher agree on this constant.
     */
    const val HEAD_WORDS = 6

    /**
     * FNV-1a, 64-bit. Used to key prefix lookups.
     *
     * Storing prefixes as text costs more than the entries themselves: six overlapping,
     * cumulative strings per sentence. Hashing them removes that. Its own implementation
     * rather than [String.hashCode] because this value goes on disk, so it must be
     * identical on every platform and stable across releases forever — 32 bits is also
     * uncomfortably narrow at ~50,000 keys per book.
     *
     * Collisions are harmless: a lookup only ever *proposes* candidate positions, and the
     * matcher then verifies the text.
     */
    fun hash(text: String): Long {
        var h = -0x340d631b7bdddcdbL // FNV offset basis
        for (c in text) {
            h = h xor c.code.toLong()
            h *= 0x100000001b3L // FNV prime
        }
        return h
    }
}
