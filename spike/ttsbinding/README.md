# QUI-020 — TTS binding probe

A system TTS engine that answers questions instead of reading books. It speaks a tone, not
words, and logs everything a host sends it.

> **Builds clean** as of 2026-08-28 (AGP 8.7.3, compileSdk 35, minSdk 26). It has not yet
> been run on a device, so the runtime behaviour — whether NeoReader binds to it and what
> the audio callbacks do — is still unverified.

## Build and install

```bash
cd spike/ttsbinding
echo "sdk.dir=$ANDROID_HOME" > local.properties   # if not already set
gradle assembleDebug
adb install -r build/outputs/apk/debug/quire-tts-probe-debug.apk
```

Sideloading the APK directly works too; the device will ask for permission to install from
an unknown source. The app has no launcher icon on purpose — it is a service, and it shows
up in the text-to-speech engine list rather than the app drawer.

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
