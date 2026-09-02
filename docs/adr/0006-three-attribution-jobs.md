# ADR-0006 — Attribution is three jobs, and the model is asked once per scene

**Status:** Accepted, 2026-09-02 — the scene-batching arithmetic is projected, not measured
**Date:** 2026-09-02
**Ticket:** QUI-009, QUI-011, QUI-032
**Deciders:** dylangrowcoot, local-model-voice-accents

## Context

QUI-028 killed the hope that heuristics carry the book. Tier 1 scores **58.5% precision
over 2,846 PDNC quotations**, and on quotations with no explicit speech tag it is right
about **one time in nine** — guessing and losing, not declining to guess. Whatever fills
that gap is a model, and `device-profile.md` §2 puts a quantized 1B SLM on a Snapdragon
750G in the *hours* for a novel against QUI-007's **30-minute** budget.

That is the pressure this ADR is written under. It is easy, under it, to talk about "the
model" as one thing that does "the judging". Three different jobs hide inside that phrase,
and they have different inputs, different costs and different failure modes.

## Decision

### 1. Name the three jobs and schedule them separately

| | Job | Input | Output | Ticket |
| --- | --- | --- | --- | --- |
| **A** | **Cast discovery** — who is in this book | the whole text, skimmed | names, aliases, gender, age band | QUI-007 |
| **B** | **Speaker attribution** — who says quotation *n* | one scene at a time | a speaker id per quotation | QUI-009 |
| **C** | **Voice design** — what each character sounds like | a handful of that character's lines | a voice descriptor (ADR-0007) | QUI-011 |

They are not stages of one pipeline that must run in order. A and C are per-*character* and
cost is bounded by the size of the cast. B is per-*quotation* and is the only one whose
cost scales with the length of the book. Conflating them makes the cheap jobs look as
expensive as the dear one.

### 2. C does not wait for B

The intuitive dependency — you cannot describe Geralt's voice until you know which lines
are Geralt's — is real but far weaker than it looks. To describe a voice you do not need
*all* of a character's lines. You need *some lines you are confident about*, and explicit
speech tags supply them: PDNC is **30.1% `Explicit`**, and that explicit slice is precisely
where Tier 1 is strong rather than weak. Ten confidently-attributed lines is plenty to
judge how somebody speaks.

So voice design runs in the upfront scan, immediately after cast discovery, on the Tier 1
explicit set alone — before B has resolved a single ambiguous line. The cast, and how they
sound, are known before the hard half of attribution starts.

This matters beyond scheduling: it means a book can be *cast and playable* while B is still
improving, and it means a better attribution model later does not invalidate the voices.

### 3. B is asked once per scene, not once per line

Take a 100,000-word novel. Roughly 6,000–7,000 quotations, of which Tier 1 leaves on the
order of **3,000** unresolved. QUI-007 allows **30 minutes** for the entire scan — EPUB
parse, cast discovery, voice design, attribution and index write.

Attribution therefore has to sustain something above **1.7 decisions per second** even if
it were given the whole budget, which it is not. That is the arithmetic that kills
per-line prompting: each call re-reads its context, and the prompt dwarfs the single token
of answer. QUI-031 will measure what the runtime actually sustains; it does not need to be
measured to see that per-line is the wrong shape.

A scene-sized call inverts the ratio. The same novel holds perhaps 60–120 scenes. One call
per scene, with the cast in context, returns a *list* of speakers — the whole scene's
quotations resolved by one pass over one prompt.

**This is not a speed compromise. It is the better answer.** Turn-taking is a property of
the scene, not of the line: who spoke last, who was addressed, who has been silent since
they entered. A model shown one quotation in isolation has strictly less information than
a human reader has, and we would be paying more for it. Per-line prompting would be both
slower and worse.

## Consequences

- **QUI-009's Tier 2/3 prompt changes shape** from one quotation to one scene, and its
  output from a name to a list aligned to quotation ids.
- **A scene segmentation step is now required** and does not exist. Chapter boundaries,
  scene-break markup and blank-line runs are the cheap signals; this is new work.
- **Alignment must fail closed.** A model that returns 11 speakers for 12 quotations must
  drop to QUI-009's turn-taking fallback for the whole scene rather than shifting everyone
  by one. A misaligned list is worse than no list, because it is confidently wrong for a
  whole scene at once.
- **Blast radius grows.** One bad call now costs a scene rather than a line. The
  confidence gates QUI-028 showed to be uncalibrated (`EXPLICIT_TAG` claims 0.95 against a
  measured 68.6%) become more important, not less.
- **QUI-031 must measure scene-sized prompts.** Throughput measured on single-line prompts
  would answer a question we are no longer asking, and would flatter the prefill cost that
  scene batching exists to amortise.
- **Long scenes may exceed the context window.** Splitting a scene is allowed; splitting it
  at a turn boundary and carrying the last speaker across is the obvious approach and is
  untested.

## What is unverified

The 3,000-unresolved figure is projected from QUI-028's PDNC rates, not counted on a real
novel. The 60–120 scene count is an estimate. Neither changes the shape of the decision —
per-line loses by more than an order of magnitude, not by a margin an estimate could
close — but both should be counted properly when QUI-031 runs.
