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

The log is written twice. **The Downloads copy needs no cable**: open the device's
Downloads folder and you will find `quire-probe-<timestamp>.tsv`, one file per service
start. Open it, share it, or email it off the device.

The private copy under the app's own directory is written regardless, as a fallback if
the Downloads write fails, and needs adb:

```bash
adb logcat -s QuireProbe
adb pull /sdcard/Android/data/quire.spike.tts/files/quire-probe.tsv
```

The TSV has one row per `onSynthesizeText` call: wall clock, utterance number, gap since
the previous call, character count, speech rate, pitch, locale, voice, caller UID, and the
exact text.

## Sustained-power harness (QUI-016)

Two buttons under "Benchmark everything installed" run and stop a second mode, next to
the QUI-017 bake-off console this app already carries. Where `Benchmark` measures one
~10 s burst, this is for PRD §5's battery SLA — "< 8% drain per hour of continuous
playback" — which a burst cannot answer: ADR-0002 §1 notes that at RTF 0.354 the engine
synthesises a page in a third of the time it plays, then goes quiet until the next page
is due. **Run sustained synthesis (60 min)** reproduces that duty cycle for real, against
whichever engine is currently selected, loaded once and kept warm throughout; **Stop**
winds it down at the next chunk boundary.

It refuses to start, or aborts, if the device is on charge — a drain reading while
plugged in measures nothing — and holds a wake lock so a screen timeout doesn't let the
CPU sleep mid-measurement. It samples battery percentage once a minute and, at the end,
prints a pass/fail summary against the 8%/hour SLA. The full per-minute trace is written
to `quire-sustained-<timestamp>.tsv`, collected the same way as the log above (Downloads
copy first, private-directory fallback over adb).

This is the harness, not the measurement — running it for the hour, on the reference
device, unplugged, with the exact preconditions (what to disable, how long, how to read
the result) is a manual procedure written into `tickets.md`'s QUI-016 Worklog, not
automated here.

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
