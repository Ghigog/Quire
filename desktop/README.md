# :desktop — a book, read aloud, on this machine

QUI-046. The whole pipeline in one JVM program: an EPUB goes in, a multi-voice `.wav` comes
out. No emulator, no APK, no device.

This is the read-aloud half of `docs/architecture.md` §8's **M2 — MVP** (*"Any book, any
Tier 1 reader … Import, read aloud, no setup"*). The import half is `app:companion`; the
Android `TextToSpeechService` (`app:ttsservice`, QUI-010) is still Todo, and this module is
why that no longer blocks anyone from hearing the product.

## First run

Needs a JDK 21 on the path. On macOS with Homebrew:

```sh
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # the `java` on PATH may be much older
```

Then:

```sh
tools/fetch-sherpa-jvm.sh     # sherpa-onnx JVM API + natives for this OS, ~8 MB
tools/fetch-tts-model.sh      # Piper libritts_r medium, 82 MB, into ~/.cache/quire/models
./gradlew :desktop:installDist
```

Nothing large is committed (CLAUDE.md §9); both scripts are re-runnable and skip what is
already there.

## Use

```sh
Q=desktop/build/install/quire/bin/quire

$Q cast mybook.epub                       # who is in it, their voices, how much resolved
$Q read mybook.epub --list                # which chapters exist
$Q read mybook.epub --chapter 0 --out ~/Desktop
$Q read mybook.epub --out ~/Desktop       # the whole book
```

`read` writes a mono 16-bit WAV named after the book, at the engine's own rate (22.05 kHz
for `libritts_r`). `--dialogue-only` renders just the spoken lines, which is the quickest
way to hear whether the casting works.

## What it does, in order

`EpubText` → `BookScan` → `Heuristic` (Tier 1) → `Conversation` (turn-taking) → `Voices`
(casting) → `RenderPlan` (order and fallback) → sherpa-onnx → `WavFile`.

Every step but the last two is a `core:*` module, unchanged and un-copied (CLAUDE.md §9).
`Voices` and `RenderPlan` live here because they are the two decisions no existing module
owned: which of the engine's 904 speakers reads a character, and what order the audio goes in.

## Known limits

- **No SLM is bound**, exactly as `app:companion` ships today (`ImportService.slmRuntime()`
  returns null). A line Tier 1 and turn-taking both decline is read by the narrator. `cast`
  prints that fraction, so the size of the gap is visible rather than assumed.
- **Voices are picked, not yet interpolated.** `Foundry.plan` chooses two speakers and a
  fraction between them; realising the fraction means writing the model's `emb_g.weight`,
  which no sherpa-onnx binding exposes. The plan degrades to the nearer of its two measured
  parents, so a character's voice is real and its own — just not the designed midpoint yet.
  That is QUI-010's.
- Absolute speed here says nothing about the reference device (CLAUDE.md §5). A comparison
  between two models often does; `spike/hostbench/README.md` records where that transfers.
