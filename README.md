# Quire

**The tactile, multi-voice e-reader.**

Quire turns a standard EPUB into a multi-character audio experience. A small language
model runs on-device to work out who is speaking each line, an ONNX text-to-speech
engine gives each character their own voice, and the active sentence is highlighted as
it is read — all offline, and tuned for e-ink Android hardware.

## Status

The pipeline runs end to end on a laptop. `:desktop` turns an EPUB into a multi-voice WAV —
import, cast discovery, Tier 1 attribution, turn-taking, casting and synthesis — with one
command and no device:

```sh
tools/fetch-sherpa-jvm.sh && tools/fetch-tts-model.sh
./gradlew :desktop:installDist
desktop/build/install/quire/bin/quire cast mybook.epub
desktop/build/install/quire/bin/quire read mybook.epub --chapter 0 --out ~/Desktop
```

See [`desktop/README.md`](desktop/README.md). It is the read-aloud half of architecture.md
§8's **M2 — MVP**; the Android `TextToSpeechService` is still Todo, and the desktop app is
why that no longer blocks anyone from hearing the product.

The root Gradle build holds seven JVM modules — `core:model`, `core:index`,
`core:attribution`, `core:epub`, `core:voice`, `core:tts` and `desktop`, plus the
`spike:indexer` and `spike:slice` spikes — with unit tests that run on any JVM
(`./gradlew test`, seconds, no device or SDK needed). Between them they cover a SQLite-backed
book index, a heuristic speaker attributor scored against the labelled TSV fixtures in
[`fixtures/attribution/`](fixtures/attribution), the casting and span-clipping the reader
uses, and the desktop renderer above.

Alongside them are harnesses that answer questions cheaply:
[`spike/hostbench`](spike/hostbench) screens TTS candidates on the build machine in
Python before one costs a device cycle, and [`spike/pipeline`](spike/pipeline) scores
attribution quality on the desktop. [`spike/ttsbinding`](spike/ttsbinding) is an Android
probe that builds and speaks on the reference device.

The Android applications live in their own build tree under `app/` (`app:companion` for
import, `app:ttsservice` for the system TTS engine) and are reached through `includeBuild`,
so the JVM-only root stays free of the Android SDK.


## Documentation

| Document | What it is for |
| --- | --- |
| [`docs/PRD.md`](docs/PRD.md) | The product requirement document — what we are building and the SLAs it must hit |
| [`tickets.md`](tickets.md) | The backlog and the state of all work in flight |
| [`CLAUDE.md`](CLAUDE.md) | How agents and humans work in this repo: branching, parallel work, definition of done |

## Design targets

| Metric | Target |
| --- | --- |
| Peak RAM | ≤ 1.2 GB |
| App footprint | ≤ 450 MB incl. quantized models |
| Real-time factor | ≤ 0.15 |
| Battery | < 8% per hour of playback |
| Time to first sound | < 800 ms |

Everything stays on the device. Book text and generated audio are never sent anywhere.
