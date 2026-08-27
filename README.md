# Quire

**The tactile, multi-voice e-reader.**

Quire turns a standard EPUB into a multi-character audio experience. A small language
model runs on-device to work out who is speaking each line, an ONNX text-to-speech
engine gives each character their own voice, and the active sentence is highlighted as
it is read — all offline, and tuned for e-ink Android hardware.

## Status

Early setup. No application code yet — the project currently consists of its
specification and process documentation.

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
