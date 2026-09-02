# ADR-0007 — A voice is a stored description, not a speaker id

**Status:** Accepted, 2026-09-02 — axis 3 reaches the model but has not been heard
**Date:** 2026-09-02
**Ticket:** QUI-032, QUI-033
**Deciders:** dylangrowcoot, local-model-voice-accents
**Superseded in part by:** [ADR-0009](0009-voices-are-generated.md), which shows the speaker table is writable, so a
voice need not be one of the 904 at all. Read them together.

## Context

ADR-0002 settled the engine: one 92 MB Piper `libritts_r` model carrying 904 speakers, so
casting is an integer lookup rather than a second model in memory. QUI-011 takes that
literally — a character gets a speaker id, and that is the whole of what "voice" means.

The upfront scan knows far more than an integer. It has read the book. It knows Geralt is
a laconic adult man who speaks in short declaratives, and that the innkeeper is a nervous
elder who repeats himself. Casting to an id throws all of that away at the moment it is
cheapest to keep, and leaves the character drawer (QUI-015) offering the reader a number to
edit.

Two things prompted revisiting it: ADR-0006 makes voice design a job in its own right, and
a probe (`spike/hostbench/voiceprobe.py`) found an axis we had written off.

## What a voice actually consists of

Four axes, and it is worth separating them because they live in completely different places
and three of the four already work.

| | Axis | Where it lives | Status |
| --- | --- | --- | --- |
| 1 | **What they say** — dialect, verbal tics, formality, register | the book's own text | Free. The author already wrote it, and we read it aloud verbatim. |
| 2 | **Delivery** — pace, pitch range | `length_scale` at runtime, plus speaker choice | Works today |
| 3 | **Pronunciation** — rhoticity, vowel qualities, i.e. accent | the espeak-ng variant, in the ONNX metadata | **Reaches the model — measured below. Never listened to.** |
| 4 | **Vocal identity** — timbre, who this sounds like | the 512-dim speaker embedding, addressed by id | Works today |

Axis 3 was previously assumed unreachable, on the grounds that 904 speakers of an American
corpus can only sound American. That reasoning was wrong about *where accent lives*. It is
not in the speaker embedding at all. Piper phonemises through espeak-ng before the model
sees anything, and which regional variant it uses is a field called `voice` in the ONNX
metadata — an editable field sitting beside `n_speakers` and `sample_rate`.

## Evidence — the variant reaches the model

`spike/hostbench/voiceprobe.py`, `libritts_r-medium`, speaker 447, one fixed sentence.

The first attempt at this measured durations and compared them, and it was worthless.
Piper samples durations from `noise_w` on every call, so **three identical calls spread
0.38 s** — wider than most of the differences being tested. The probe therefore pins
`noise_scale` and `noise_scale_w` to zero, which makes synthesis exactly reproducible, and
compares waveforms sample for sample instead.

```
control, sampling on      3.27 s   spread 0.38   ← the reason determinism is needed
control, sampling off     2.83 s   spread 0.00

en-us          2.83 s   IDENTICAL to the control, sample for sample
en-us-nyc      2.77 s   different
en-GB-x-rp     3.07 s   different
en-GB-scotland 3.80 s   different      (+34% on the same sentence)
en-GB-x-gbclan 2.84 s   different      Lancashire
en-GB-x-gbcwmd 2.90 s   different      West Midlands
en-029         3.32 s   different      (+17%) Caribbean
en-gb          failed — "Failed to set eSpeak-ng voice"
```

The `en-us` row is the control that makes the rest mean anything: it goes through the same
patch-and-re-save path as every other row and comes back **bit-identical**, so a difference
elsewhere is the variant and not the patching. Every variant the bundled `espeak-ng-data`
ships changes the phoneme stream, including the four (NYC, RP, Lancashire, West Midlands)
that a duration comparison had dismissed as noise.

`en-gb` fails as an identifier even though the data ships an `en` file claiming
`language en-gb`; the resolvable names are the directory names under `lang/gmw/`. Six
usable English accents ship inside the model we already have, at zero extra footprint.

## Decision

**1. `characters.json` carries a voice descriptor, not a speaker id.** The manifest is a
frozen fan-out seam (CLAUDE.md §2.3), so this lands early and small as QUI-032. Shape:

```json
"voice": {
  "speakerId": 447,
  "espeakVoice": "en-GB-scotland",
  "lengthScale": 1.05,
  "targetF0Hz": 118,
  "description": "Laconic, low, unhurried. Rarely raises his voice.",
  "source": "auto"
}
```

Every field is optional and the existing `additionalProperties: true` round-trip rule
already covers readers that predate it, so no `schemaVersion` bump is needed.

`description` is prose because it is the field a human reads in the drawer and the field a
model writes. `source` is `auto` or `user`, so QUI-011's rule that user overrides survive a
rescan has something to key on.

**2. Casting consumes the descriptor rather than producing the id.** QUI-011's caster
becomes a *resolver*: given a description and the co-presence constraint, pick the speaker
whose measured F0 (`fixtures/voices/libritts_r-f0.tsv`, QUI-011's existing profile) is
nearest the target, then set the rate and the variant. Determinism and distinctness are
unchanged, and the choice becomes explainable — which is what makes the drawer editable.

**3. Accent is designed for, but not shipped, until it has been heard.** QUI-033.

## The caveat that could sink axis 3

`libritts_r` was trained on en-US phonemes. Feeding it Scots phonemes is out-of-distribution
input, and nothing measured here can tell the difference between *sounds Scottish* and
*sounds like an American reader failing at Scottish*. Durations and F0 confirm the phoneme
stream changed; they say nothing about whether the result is listenable.

That is a listening test, on the reference device, and it is the single highest-value one
available. It is cheap — the probe already produces the audio — and it is binary. If it
passes, six accents ship free. If it fails, axes 1, 2 and 4 still stand and the descriptor
simply never sets `espeakVoice`.

**Nothing depends on the answer.** The descriptor is worth storing either way, which is why
QUI-032 is not blocked on QUI-033.

## Consequences

- QUI-011's caster is rewritten as a resolver over the descriptor. Its Gherkin on
  determinism, distinctness and override survival is unaffected.
- QUI-015's drawer shows a sentence and a set of controls rather than a voice number.
- QUI-007's scan gains job C from ADR-0006, which writes these descriptors.
- The engine must be able to switch espeak variant per character. `libritts_r` holds one
  `voice` value in its metadata, and sherpa-onnx exposes no runtime override — its VITS
  config offers `lexicon`, `data_dir` and `dict_dir`, and nothing that names a variant. So
  a per-character accent means one loaded engine per variant, and ADR-0002 measured load
  at **2,524 ms** with **314 MB** peak RSS. Six accents that way fit neither the 1.2 GB
  ceiling nor any latency budget. **This is unsolved and QUI-033 must answer it before any
  accent ships.** Nothing else in this ADR depends on it.
- No new dependency and no footprint cost: `espeak-ng-data` is already inside the model
  directory. `onnx` is added to the *host* probe only, never to the app.
