# QUI-020 — TTS binding probe

A system TTS engine that answers questions instead of reading books. It speaks a tone, not
words, and logs everything a host sends it.

> **Not compiled.** This was written in an environment with no Android SDK
> (`dl.google.com` is blocked by the network egress policy), so it has never been built.
> Expect a round of small fixes. The manifest registration, the `TextToSpeechService`
> overrides and the callback sequence are the parts worth reviewing closely.

## Build and install

```bash
cd spike/ttsbinding
gradle assembleDebug
adb install -r build/outputs/apk/debug/quire-tts-probe-debug.apk
```

Then on the device: Settings → Text-to-speech → preferred engine → **Quire Probe (spike)**.
If NeoReader already had a TTS session open, close and reopen it — Android only rebinds
the engine on a fresh session.

## Run

Open a book in NeoReader, tap the centre of the screen, tap the headphone icon. You should
hear alternating tones, one per word, instead of speech.

## Collect

```bash
adb logcat -s QuireProbe
adb pull /sdcard/Android/data/quire.spike.tts/files/quire-probe.tsv
```

The TSV has one row per `onSynthesizeText` call: wall clock, utterance number, gap since
the previous call, character count, speech rate, pitch, locale, voice, caller UID, and the
exact text.

## The questions it exists to answer

1. **Chunk size** — the `chars` column. What is typical, and what is the maximum? Does it
   ever approach the 4000-character API limit?
2. **Chunk alignment** — read the `text` column. Sentences? Paragraphs? Whole pages? Does a
   heading with no full stop arrive glued to the paragraph beneath it, as listening
   suggested?
3. **Text cleanliness** — do page numbers, footnote markers or hyphenation appear?
4. **`onStop` frequency** — count `onStop` rows against page turns. Per page, or only on
   stop?
5. **Highlighting** — does NeoReader underline as the tones play? That confirms it consumes
   `rangeStart` from an arbitrary engine, not just from Google's.
6. **Rate and pitch** — change the speed in NeoReader and check the `rate` column moves.

Write the answers into `docs/adr/0004-interception-viability.md` and delete the guesses.
