package quire.attribution

import quire.model.Kind
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender

/**
 * Reads a book once and works out who is in it — the model-free half of QUI-007.
 *
 * Two sources for the cast. A name in speech-tag position (`said X`) is a speaking
 * character by construction, so one sighting is enough. A name merely standing beside a
 * quotation is weaker — it could be the person being addressed, or someone who happens to
 * be in the room — so it takes [ADJACENCY_MIN] sightings. The second source exists because
 * modern prose runs whole scenes on action beats with no speech tag at all.
 *
 * **Gender is inferred, not asked for.** A cast without it cannot be voiced: casting falls
 * back to spreading arbitrary speaker ids, which is what put a male Sarah on the device on
 * 2026-08-29. The evidence is the pronoun that stands in for a name nearby — `Sarah … she`
 * — counted across the whole book so one odd sentence cannot decide it.
 */
object Roster {

    /**
     * A name must sit beside this many quotes before adjacency alone admits it.
     *
     * It was 2, and a real novel imported on 2026-08-31 reported 157 characters. Measured
     * across PDNC's 28 novels (`spike/pipeline cast`), raising it to 8 takes precision from
     * 57.9% to 84.8% and costs 2.3 points of recall on the characters PDNC calls major or
     * intermediate — those are nearly all found by their speech tags anyway, and adjacency
     * mostly admits people who are talked *about*. Past 8 the curve flattens and only
     * recall moves. It is a count rather than a rate, so a very short book is stricter than
     * this number was tuned for; nothing in the corpus made that worth the complexity.
     */
    const val ADJACENCY_MIN = 8

    /** How many pronoun sightings must agree before a gender is claimed. */
    const val GENDER_MIN = 2

    /** How much the winning pronoun must lead by, as a share of that name's sightings. */
    const val GENDER_MAJORITY = 0.6

    data class Cast(
        val fromTags: Map<String, Int>,
        val fromAdjacency: Map<String, Int>,
        val genders: Map<String, Gender>,
    ) {
        val names: Set<String> =
            fromTags.keys + fromAdjacency.filterValues { it >= ADJACENCY_MIN }.keys
    }

    /**
     * Scan the book.
     *
     * [paragraphs] is `locator to text`, in reading order.
     */
    fun scan(paragraphs: List<Pair<String, String>>): Cast {
        val tags = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, Int>()
        val pronouns = mutableMapOf<String, MutableMap<Gender, Int>>()
        // The last name mentioned with nothing ambiguous after it, carried *across*
        // paragraphs. Anaphora routinely spans them — a paragraph names Sarah, the next
        // says "she said" — and resetting at every break was why this found nothing at
        // all on real prose.
        var pending: String? = null

        for ((locator, text) in paragraphs) {
            for (segment in Segmenter.segment(locator, text)) {
                if (segment.kind != Kind.DIALOGUE) continue
                val tagged = Names.tagName(segment.before, segment.after)
                if (tagged != null) {
                    tags.merge(tagged, 1, Int::plus)
                } else {
                    // The context either side is the *whole* rest of the paragraph, so on a
                    // back-and-forth line it carries the neighbouring quotations. Their
                    // first word is capitalised because speech starts there.
                    val nearby = listOfNotNull(
                        Names.sentences(Names.withoutQuotedText(segment.before)).lastOrNull(),
                        Names.sentences(Names.withoutQuotedText(segment.after)).firstOrNull(),
                    ).flatMap(Names::namesIn)
                    for (name in nearby) adjacency.merge(name, 1, Int::plus)
                }
            }
            // Narration only. A pronoun inside a quotation refers to whoever is being
            // spoken to — `"You have been standing there," she said` has "you" inside and
            // "she" outside, and only the second says anything about the speaker.
            val narration = Segmenter.segment(locator, text)
                .filter { it.kind == Kind.NARRATION }
                .joinToString(" ") { it.text }
            pending = countPronouns(narration, pronouns, pending)
        }

        val genders = pronouns.mapNotNull { (name, votes) ->
            val total = votes.values.sum()
            val (best, count) = votes.maxByOrNull { it.value } ?: return@mapNotNull null
            // Both thresholds matter. A single sighting is noise, and a name that draws
            // "he" and "she" about equally is usually two people sharing a surname.
            if (count >= GENDER_MIN && count.toDouble() / total >= GENDER_MAJORITY) {
                name to best
            } else {
                null
            }
        }.toMap()

        return Cast(tags, adjacency - tags.keys, genders)
    }

    /**
     * Count `Sarah … she` within a paragraph.
     *
     * The pronoun is only counted when it *follows* the name in the same paragraph and no
     * other name intervenes — the ordinary anaphoric pattern. Looking further afield finds
     * more evidence and much worse evidence.
     */
    private fun countPronouns(
        text: String,
        into: MutableMap<String, MutableMap<Gender, Int>>,
        carried: String?,
    ): String? {
        var pending = carried
        for (sentence in Names.sentences(text)) {
            val names = Names.namesIn(sentence)
            val gender = pronounGender(sentence)
            when {
                names.size == 1 && gender != null -> {
                    // Name and pronoun in one sentence: "Sarah put down her cup."
                    into.getOrPut(names.first()) { mutableMapOf() }.merge(gender, 1, Int::plus)
                    pending = names.first()
                }
                names.size == 1 -> pending = names.first()
                names.size > 1 -> pending = null // ambiguous; wait for a cleaner sentence
                gender != null && pending != null ->
                    into.getOrPut(pending!!) { mutableMapOf() }.merge(gender, 1, Int::plus)
            }
        }
        return pending
    }

    private val FEMALE = Regex("\\b(she|her|hers|herself)\\b", RegexOption.IGNORE_CASE)
    private val MALE = Regex("\\b(he|him|his|himself)\\b", RegexOption.IGNORE_CASE)

    private fun pronounGender(sentence: String): Gender? {
        val female = FEMALE.containsMatchIn(sentence)
        val male = MALE.containsMatchIn(sentence)
        return when {
            female && !male -> Gender.FEMALE
            male && !female -> Gender.MALE
            else -> null // both or neither says nothing
        }
    }

    /**
     * The cast as a manifest, in QUI-005's frozen shape, ready for casting and the index.
     *
     * This is what the companion app hands the service: the scan's whole output, in the
     * form four other tickets already build against.
     */
    fun manifest(cast: Cast, bookId: String, generatedAt: Long): CharacterManifest =
        CharacterManifest(
            schemaVersion = CharacterManifest.VERSION,
            bookId = bookId,
            generatedAt = generatedAt,
            narrator = Character(
                id = "narrator", displayName = "Narrator",
                gender = Gender.NEUTRAL, confidence = 1.0,
            ),
            characters = cast.names.sorted().map { name ->
                Character(
                    id = name,
                    displayName = name,
                    gender = cast.genders[name] ?: Gender.UNKNOWN,
                    // A name seen in tag position is certain; one admitted on adjacency
                    // alone is a guess that the SLM pass may yet overturn.
                    confidence = if (name in cast.fromTags) 1.0 else 0.6,
                    lineCount = cast.fromTags[name] ?: cast.fromAdjacency[name] ?: 0,
                )
            },
        )
}
