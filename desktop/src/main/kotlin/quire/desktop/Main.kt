package quire.desktop

import java.io.File
import kotlin.system.exitProcess
import quire.tts.buffer.WavFile

/**
 * QUI-046: the desktop app.
 *
 * ```sh
 * tools/fetch-sherpa-jvm.sh && tools/fetch-tts-model.sh
 * ./gradlew :desktop:installDist
 * build/install/quire/bin/quire cast mybook.epub
 * build/install/quire/bin/quire read mybook.epub --chapter 1 --out ~/Desktop
 * ```
 *
 * Two commands, because they answer the two questions a person actually has: *who is in
 * this book, and did it work* (`cast`), and *let me hear it* (`read`). No UI — CLAUDE.md §5
 * puts the reader in the host app and PRD §6 defers a drawer to V2.0, so a command that
 * writes a `.wav` is the honest shape for v1.
 */
fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] in setOf("help", "-h", "--help")) {
        usage()
        return
    }
    try {
        when (val command = args[0]) {
            "read" -> read(Flags.parse(args.drop(1)))
            "cast" -> cast(Flags.parse(args.drop(1)))
            else -> {
                System.err.println("unknown command: $command")
                usage()
                exitProcess(2)
            }
        }
    } catch (failure: Exception) {
        System.err.println("error: ${failure.message}")
        exitProcess(1)
    }
}

private fun usage() {
    println(
        """
        quire — multi-voice read-aloud, on this machine.

        Usage:
          quire cast <book.epub>      Show the cast, their voices, and how much was resolved
          quire read <book.epub>      Render the book to a .wav, one voice per character

        Options (read):
          --chapter N        Only chapter N (0-based; --list shows them)
          --out DIR          Where the .wav goes (default: build/audio)
          --speed F          Speaking rate; 1.0 is the model's own pace
          --dialogue-only    Skip narration, to audition the voices alone
          --threads N        Decoder threads (default 2)

        Options (both):
          --model DIR        TTS model directory (default: ~/.cache/quire/models/${SherpaEngine.DEFAULT_MODEL})
          --voices-dir DIR   Where libritts_r-f0.tsv and -quality.tsv live
          --list             Print the chapters and stop

        First run:
          tools/fetch-sherpa-jvm.sh && tools/fetch-tts-model.sh
        """.trimIndent(),
    )
}

private fun read(flags: Flags) {
    val epub = flags.book()
    val modelDir = flags.get("model")?.let(::File)
        ?: File(System.getProperty("user.home"), ".cache/quire/models/${SherpaEngine.DEFAULT_MODEL}")
    val voicesDir = flags.get("voices-dir")?.let(::File) ?: fixturesDir()
    val outDir = flags.get("out")?.let(::File) ?: File("build/audio")

    println("Reading ${epub.name}…")
    val book = BookReader().read(epub)

    if (flags.has("list")) {
        val chapters = BookReader.chapters(book)
        println("${chapters.size} chapters: ${chapters.joinToString(", ")}")
        return
    }

    val chapter = flags.int("chapter")
    chapter?.let {
        val known = BookReader.chapters(book)
        require(it in known) { "chapter $it is not in this book; chapters are ${known.joinToString(", ")}" }
    }

    val voices = Voices(profilesIn(voicesDir), qualityIn(voicesDir)).assign(book.manifest)
    val narrator = voices.getValue(Voices.NARRATOR)
    announce(book, voices, narrator)

    val segments = BookReader.segmentsFor(book, chapter)
    val coverage = RenderPlan.coverage(segments)
    val plan = RenderPlan.build(
        segments = segments,
        voices = voices,
        narratorVoiceId = narrator,
        dialogueOnly = flags.has("dialogue-only"),
    )
    require(plan.isNotEmpty()) { "nothing to render for chapter ${chapter ?: "(all)"}" }
    println(
        "Rendering ${plan.size} segments — dialogue resolved ${coverage.resolved}/${coverage.dialogue} " +
            "(${"%.0f".format(coverage.fraction * 100)}%), the rest narrated.",
    )

    println("Loading the engine…")
    val engine = SherpaEngine.load(
        modelDir = modelDir,
        speed = flags.double("speed")?.toFloat() ?: 1.0f,
        threads = flags.int("threads") ?: 2,
    )
    val samples = try {
        render(engine, plan)
    } finally {
        engine.release()
    }

    val out = File(outDir, book.manifest.bookId + (chapter?.let { "-ch$it" } ?: "") + ".wav")
    WavFile.write(out, samples, engine.sampleRate)
    val seconds = samples.size.toLong() / engine.sampleRate
    println("Wrote ${out.path} — ${plan.size} segments, ${seconds / 60}m ${seconds % 60}s")
}

/**
 * Every segment in order, with a short beat between them.
 *
 * Chunks are collected and copied once rather than grown element by element: a chapter is
 * tens of millions of samples, and appending them to one `FloatArray` at a time costs more
 * than the synthesis does.
 */
private fun render(engine: SherpaEngine, plan: List<PlannedLine>): FloatArray {
    val gap = FloatArray(engine.sampleRate * GAP_MS / 1000)
    val chunks = ArrayList<FloatArray>(plan.size * 2)
    var total = 0
    var previous: String? = null

    plan.forEachIndexed { i, line ->
        // A beat *between* paragraphs, never inside one. `"...," he said.` is two spans of a
        // single spoken line — the quotation and the tag that follows it — and a pause on
        // either side of the tag reads as three separate utterances in two voices.
        if (previous != null && line.paragraph != previous) {
            chunks += gap
            total += gap.size
        }
        engine.synthesize(line.text, line.voiceId) { false }?.let { audio ->
            chunks += audio.pcm
            total += audio.pcm.size
        }
        previous = line.paragraph
        println("  [${i + 1}/${plan.size}] ${(line.speakerId ?: "narrator").padEnd(16)} ${line.text.take(64)}")
    }

    return FloatArray(total).also { all ->
        var at = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, all, at, chunk.size)
            at += chunk.size
        }
    }
}

private fun cast(flags: Flags) {
    val epub = flags.book()
    val voicesDir = flags.get("voices-dir")?.let(::File) ?: fixturesDir()

    println("Reading ${epub.name}…")
    val book = BookReader().read(epub)
    val voices = Voices(profilesIn(voicesDir), qualityIn(voicesDir)).assign(book.manifest)
    val narrator = voices.getValue(Voices.NARRATOR)

    println()
    announce(book, voices, narrator)
    println()

    val coverage = RenderPlan.coverage(book.segments)
    println("Dialogue resolved: ${coverage.resolved}/${coverage.dialogue} (${"%.0f".format(coverage.fraction * 100)}%)")
    val byTier = book.segments.groupingBy { it.tier }.eachCount().toSortedMap(compareBy { it.ordinal })
    for ((tier, count) in byTier) println("  ${tier.name.lowercase().padEnd(10)} $count segments")
    println()
    println("Chapters: ${BookReader.chapters(book).size} — render one with --chapter N")
}

private fun announce(book: BookReader.Read, voices: Map<String, Int>, narrator: Int) {
    println("Cast of ${book.manifest.bookId}:")
    println("  ${"narrator".padEnd(18)} speaker $narrator")
    for (character in book.manifest.characters) {
        val description = character.voice?.description ?: "(no descriptor)"
        println(
            "  ${character.displayName.padEnd(18)} speaker ${voices[character.id]}  " +
                "${character.gender.name.lowercase().padEnd(8)} $description",
        )
    }
}
