# ADR-0009 — Character voices are generated, not assigned

**Status:** Accepted, 2026-09-02 — the mechanism is proven, the quality is unheard
**Date:** 2026-09-02
**Ticket:** QUI-036
**Deciders:** dylangrowcoot, session-visibility-check

## Context

QUI-011 and `spike/slice/Casting.kt` assign each character one of the engine's 904 speakers,
spread across a pitch-sorted pool. ADR-0002 treated "904 voices, free" as the happy outcome,
and architecture.md §9 recorded the voice-pool question as answered on that basis.

That is not what the product is for. The intent, stated 2026-09-02: the app reads a book,
forms an idea of what each character *sounds like*, and then **makes** that voice. Assignment
caps the cast at 904 fixed strangers, none of them chosen for the character, and it reduces
everything the analysis understood to an opaque integer.

Before designing around "generate", QUI-036 asked whether the engine we already committed to
can do it at all. A "no" would have reopened ADR-0002.

## Decision

**A character's voice is generated from a description of how that character should sound,
not selected from a fixed set.** The analysis records a *voice description*; a separate
foundry step realises it. The two are kept apart deliberately: the foundry can improve
without re-running the analysis, and the description survives a change of engine.

```
analysis  →  "older man, low, slow, rough"    ← what the model decides
foundry   →  512 floats + a speaking rate     ← how we realise it
```

## Evidence

Both levers turned out to be editable fields inside the Piper model. No second model, no new
runtime, nothing added to the 450 MB footprint. Full numbers in QUI-036's Worklog.

**Timbre.** `emb_g.weight` is a `[904, 512]` float initializer in the ONNX graph — the
speaker lookup table, 1.8 MB of the 92 MB model. **A voice is 512 floats: 2 KB.** An
untrained row written as an interpolation of two speakers synthesises cleanly, and its
median F0 moves monotonically between the parents in 14–29 Hz steps against a measured
2.16 Hz noise floor.

**Accent lives in the phonemiser, not the speaker vector.** The espeak-ng variant is read
from the ONNX `metadata_props["voice"]`. The bundled `espeak-ng-data` (19 MB, already
shipping) carries RP, Scots, Lancashire, West Midlands, Caribbean and NYC. Patched, they
reach the model: Scots runs +1.39 s on a fixed sentence (3.9 sd), Caribbean +1.17 s (2.9 sd).

## Alternatives considered

- **Assignment from the 904 (status quo).** Rejected as the product's ceiling, retained as
  the fallback the foundry degrades to.
- **Voice cloning from a reference sample** (XTTS, OpenVoice and similar). Rejected on
  concept before cost: a fictional character has no recording to clone. Wrong tool.
- **Description-to-voice models** (Parler-TTS, VoiceLDM), which take an English description
  directly. This is the most literal reading of the intent and the one that cannot run
  here — ~880M parameters against a 1.2 GB budget that already holds Piper and will hold an
  SLM (ADR-0008), with hopeless throughput on a Snapdragon 750G. Revisit only if the device
  changes.
- **Pure DSP** — pitch/formant shifting one base voice. Cheap and available as a top-up, but
  it moves a voice rather than making one, and heavy shifting sounds processed. Kept as a
  supplement to embedding synthesis, not a replacement.

## Relationship to ADR-0007

[ADR-0007](0007-voice-is-a-description.md) was written the same day from the other end of
the same problem, and the two agree more than the numbering suggests. ADR-0007 specifies
*what is stored* — the four axes a voice has, and the `voice` object QUI-032 has since
landed in `characters.json`. This ADR specifies *how the description is realised*, and goes
further: the target need not be one of the 904, because `emb_g.weight` can be written.

Where they differ, take this one. ADR-0007 keeps `speakerId` as the realisation; here it is
the **fallback** the foundry degrades to when no generated vector is available. The stored
schema is right either way, which is why QUI-032 was not blocked on this.

One correction runs the other way. This ADR records that duration cannot see RP or
Lancashire and calls that a blind spot. ADR-0007 closed it: pinning `noise_scale` and
`noise_scale_w` to zero makes synthesis exactly reproducible, and compared sample for
sample **all six variants change the waveform**, RP and Lancashire included. The
`emb_g` blend results below were measured against a stochastic noise floor and would be
sharper re-run deterministically.

## Consequences

- **`characters.json` gains a voice description** and stops storing a speaker integer. It is
  one of the frozen fan-out seams (CLAUDE.md §2.3), so it wants landing early and small.
  **Landed 2026-09-02 as QUI-032**, keeping `speakerId` as the fallback above.
- **QUI-011 / `Casting.kt` changes shape.** Pool selection becomes spec realisation. The
  25 Hz narrator guard band and the "spread across the pitch range" rule survive as
  constraints on the foundry's output, not as the mechanism.
- **`fixtures/voices/libritts_r-f0.tsv` becomes more valuable, and insufficient.** F0 for all
  904 speakers is the first axis of a searchable space; realising a description needs more
  measured axes than pitch.
- **Gender may stop being the right abstraction.** QUI-035 exists to raise gender coverage
  from 58.7% because `Casting` uses gender to pick a pool. If a voice is generated from a
  description, gender is one input to a pitch and timbre target rather than a pool selector,
  and QUI-035's framing should be revisited before it is worked.
- **Pace comes free.** The model takes `length_scale`, so speaking rate is per-character at
  no cost.

## What is not established

**Nothing in QUI-036 was listened to.** F0 and duration prove the audio is well-formed and
that the phoneme stream genuinely changed. They cannot hear whether a blended embedding
sounds like a person or like mush, nor whether Scots phonemes through an `en-US`-trained
model sound Scottish or merely broken — that combination is out-of-distribution for the
model and is the likeliest place for this to fall down.

Duration is also blind to RP and Lancashire, which differ in vowel quality and rhoticity
rather than phoneme count; their null rows in QUI-036 mean "the probe cannot see it", not
"nothing happened".

This is why the status says the quality is unheard. The decision to *build toward* generated
voices is made; the decision that generated voices are *good* needs an ear on the reference
device, and that listening test is the highest-value thing available on the next build.
