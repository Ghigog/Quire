# Quire

**The tactile, multi-voice e-reader.**

Quire turns a standard EPUB into a multi-character audio experience. A small language
model runs on-device to work out who is speaking each line, an ONNX text-to-speech
engine gives each character their own voice, and the active sentence is highlighted as
it is read — all offline, and tuned for e-ink Android hardware.

## Status

The libraries and the measurement harnesses exist; the Android app does not yet.

The root Gradle build holds six Kotlin modules — `core:model`, `core:index`,
`core:attribution`, `core:epub`, `spike:indexer` and `spike:slice` — with unit tests
that run on any JVM (`./gradlew test`, seconds, no device or SDK needed). Between them
they cover a SQLite-backed book index, a heuristic speaker attributor scored against the
labelled TSV fixtures in [`fixtures/attribution/`](fixtures/attribution), and the casting
and span-clipping the reader will use.

Alongside them are two harnesses that answer questions cheaply:
[`spike/hostbench`](spike/hostbench) screens TTS candidates on the build machine in
Python before one costs a device cycle, and [`spike/pipeline`](spike/pipeline) scores
attribution quality on the desktop. [`spike/ttsbinding`](spike/ttsbinding) is an Android
probe that builds and speaks on the reference device.

What is still to come is the reading application itself. There is no app module in
`settings.gradle.kts` yet, so there is nothing to install and read a book with; the
pieces it will be assembled from are the ones above.

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
