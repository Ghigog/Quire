# ADR-0002 — TTS engine

**Status:** Proposed — qualitative results in, numbers pending
**Date:** 2026-08-28
**Ticket:** QUI-017
**Deciders:** dylangrowcoot (device testing), claude-opus-5

## Context

PRD §3.2 names Kokoro-TTS (82M ONNX) or Piper C++ and sets RTF ≤ 0.15 and TTFS < 800 ms,
inside a 450 MB app footprint that also has to hold an attribution model. The engine is
loaded by `sherpa-onnx`, which packages all the candidates behind one Kotlin API and ships
prebuilt arm64 libraries (`spike/ttsbinding`).

Reference device: Onyx Boox Note Air5 C, Snapdragon 750G, no i8mm.

## Candidates and download sizes

| Candidate | Download | Note |
| --- | --- | --- |
| Kitten nano (fp16) | 25 MB | Smallest by far |
| Piper `libritts_r` medium | 78 MB | Multi-speaker; one of the PRD's two named engines |
| Kokoro multi-lang v1.1 (int8) | 140 MB | The realistic Kokoro |
| Kokoro en v0.19 (fp32) | 304 MB | Two thirds of the entire footprint budget |

## Findings so far (device testing, 2026-08-28)

Qualitative, from listening on the reference device. **The measured RTF figures are not yet
recorded and this ADR cannot be accepted without them.**

- **Kitten nano** — fast, and the quality was judged unacceptable. Ruled out for the
  narrator on quality, but worth remembering as a fallback if a future constraint makes
  25 MB the only affordable size.
- **Piper `libritts_r` medium** — "almost perfect". The leading candidate. It is also the
  most convenient outcome: 78 MB leaves well over 300 MB of the footprint budget for the
  attribution model, and `libritts_r` is multi-speaker, so voice casting is a speaker-id
  lookup against one resident model rather than several models in memory (QUI-011).
- **Kokoro** — too slow on this SoC. That the 750G cannot drive Kokoro at acceptable speed
  is consistent with `device-profile.md` §2: no i8mm, so quantized matmul takes the slow
  path.

## Decision

**Provisionally Piper `libritts_r` medium**, pending the measured RTF, TTFS and peak RSS
that QUI-017's benchmark reports.

## Consequences if confirmed

- QUI-010 targets sherpa-onnx with a VITS/Piper model.
- QUI-011's casting becomes an index into one model's speakers rather than a choice between
  models — a significant simplification, and it makes cast size free in memory terms.
- The footprint budget stops being the binding constraint on the attribution model, which
  matters for QUI-028.
- **Word alignments still do not exist.** `OfflineTts.generate()` returns samples only, so
  PRD §4.2's boundary timestamps must be estimated from span positions or bought with a
  forced aligner. Independent of which engine wins. See ADR-0004.

## Still needed

1. Measured **RTF** for Piper and Kokoro against the 0.15 budget — the number this ADR
   turns on.
2. **TTFS** from pressing Read Aloud to first audio, against 800 ms.
3. **Peak RSS** for the service process, against the 1.2 GB ceiling.
4. What "almost" meant for Piper: pronunciation, pacing, or something else. A defect that
   shows up on every page is different from one that shows up on proper nouns.
