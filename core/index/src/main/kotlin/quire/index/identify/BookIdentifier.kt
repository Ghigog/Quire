package quire.index.identify

import quire.index.BookIndex
import quire.index.BookRecord
import quire.index.MatchResult
import quire.index.Matcher

/**
 * Works out which indexed book the host is reading, from the text alone.
 *
 * The Android TTS API tells an engine nothing about what is being read — no title, no
 * file, no identity of any kind. Asking the reader to pick the book first would break the
 * one promise the product makes, that nothing changes about how you read, and would give
 * wrong voices silently on the day they forget. So identity is inferred from the prose.
 *
 * **Everything is narrated until a book has agreed [lockAfter] times running.** Guessing
 * early is worse than waiting: a wrong lock reads a whole chapter in another book's cast,
 * and three utterances of narrator at the start of a session is a cost nobody notices.
 *
 * One identifier per reading session, driven from the binder thread. Not thread-safe.
 */
class BookIdentifier private constructor(
    candidates: List<Pair<BookRecord, BookIndex>>,
    private val lockAfter: Int,
    private val forgetAfter: Int,
    lockTo: String?,
) {

    private class Candidate(val record: BookRecord, index: BookIndex) {
        val matcher = Matcher(index)
        /** Consecutive chunks this book has placed. Reset by any miss. */
        var streak = 0
    }

    private val candidates = candidates.map { (record, index) -> Candidate(record, index) }
    private var locked: Candidate? = null
    private var misses = 0

    /** True when the lock was taken while more than one book agreed. */
    var ambiguous = false
        private set

    /** The book being read, or null while still identifying. */
    val bookId: String? get() = locked?.record?.bookId

    /** The matcher for the locked book, so the caller can seek it. Null while searching. */
    val matcher: Matcher? get() = locked?.matcher

    init {
        lockTo?.let { wanted ->
            locked = this.candidates.firstOrNull { it.record.bookId == wanted }
        }
    }

    /**
     * Place one incoming chunk.
     *
     * Returns [MatchResult.none] while still identifying, which the caller reads in the
     * narrator's voice — the same path an unindexed book takes, so there is one behaviour
     * for "we do not know who is speaking" rather than two.
     */
    fun accept(chunk: String): MatchResult {
        locked?.let { book ->
            val result = book.matcher.match(chunk)
            misses = if (result.matched) 0 else misses + 1
            // A long unmatched passage is not a new book: an epigraph, a quoted letter, a
            // footnote the host reads out. Only a sustained run of misses means the reader
            // has actually moved to a different book.
            if (misses >= forgetAfter) unlock()
            return result
        }
        return search(chunk)
    }

    private fun search(chunk: String): MatchResult {
        var agreeing = 0
        for (candidate in candidates) {
            if (candidate.matcher.match(chunk).matched) {
                candidate.streak++
                agreeing++
            } else {
                candidate.streak = 0
            }
        }

        val ready = candidates.filter { it.streak >= lockAfter }
        if (ready.isNotEmpty()) {
            // Two editions of one novel agree on most of their text, and will often both
            // reach the threshold. The newest import is the better guess — it is the one
            // the reader most likely just added — and the ambiguity is recorded so the
            // companion app can offer the override rather than leaving them puzzled.
            locked = ready.maxByOrNull { it.record.indexedAt }
            ambiguous = ready.size > 1
            misses = 0
        }

        // Narrator for this chunk either way: locking on the chunk that completes the
        // streak would voice it before the caller has been told which book won.
        return MatchResult.none
    }

    private fun unlock() {
        locked = null
        misses = 0
        ambiguous = false
        candidates.forEach { it.streak = 0 }
    }

    companion object {
        /** Consecutive agreeing chunks before a book is trusted. */
        const val LOCK_AFTER = 3

        /** Consecutive misses before a locked book is given up on. */
        const val FORGET_AFTER = 10

        fun over(
            candidates: List<Pair<BookRecord, BookIndex>>,
            lockAfter: Int = LOCK_AFTER,
            forgetAfter: Int = FORGET_AFTER,
        ) = BookIdentifier(candidates, lockAfter, forgetAfter, lockTo = null)

        /**
         * An identifier already locked to [bookId], for the companion app's override.
         *
         * Fingerprinting is skipped entirely rather than merely biased: the reader has
         * told us the answer, and a heuristic that could still overrule them would be a
         * worse setting than none.
         */
        fun forBook(
            bookId: String,
            candidates: List<Pair<BookRecord, BookIndex>>,
            forgetAfter: Int = FORGET_AFTER,
        ) = BookIdentifier(candidates, LOCK_AFTER, forgetAfter, lockTo = bookId)
    }
}
