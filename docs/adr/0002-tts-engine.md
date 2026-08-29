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

## Measured — Piper `libritts_r` medium, Note Air5 C, 2 threads

```
load        2524 ms
synthesis   4595 ms for 12979 ms of audio
RTF         0.354   FAIL (> 0.15)
peak RSS    314 MB
on disk     92 MB
voices      904 at 22050 Hz
```

Four things fall out of this, and the RTF failure is not the most important.

### 1. RTF fails the SLA, but the SLA may be the wrong instrument

0.354 is 2.4× over PRD §5's 0.15. It is also **2.8× faster than real time**: 13 seconds of
audio in 4.6 seconds. With the ring buffer, playback never starves at this rate; the host
gives us a whole page at once and then goes quiet for twenty seconds (ADR-0004), so there
is far more slack than the budget assumes.

RTF was never the thing we cared about. It is a proxy for two things we do care about —
never starving playback, and battery — and it is a poor proxy for the second. The decisive
measurement is sustained power draw against ≈1.14 W (`device-profile.md` §4), which is
QUI-016 and has not been run.

**This is not a licence to ignore the number.** It fails as written, and either the SLA is
re-derived from a battery measurement or the engine has to get faster. What it should not
do is kill Piper on a proxy metric before the real one has been taken.

### 2. Load time alone blows the TTFS budget

2,524 ms to load, against 800 ms for time-to-first-sound. A cold start cannot meet the SLA
however fast synthesis is. The engine must be **loaded before the first utterance arrives**
and kept warm across a reading session — a foreground service, or loading on the first
`onIsLanguageAvailable` rather than the first `onSynthesizeText`. QUI-010 and QUI-012.

### 3. 904 voices, free

`libritts_r` carries 904 speakers in one 92 MB model. Casting is an integer, memory cost
does not grow with the size of the cast, and there is no reason to ever run two models.
This is the strongest argument for Piper and it makes QUI-011 much simpler than planned.

The probe reported "no multi-speaker" only because it picked speaker ids 0 and 1, which are
adjacent voices in the same corpus and sound alike. Fixed by spreading them.

### 4. Peak RSS 314 MB — comfortable

Against a 1.2 GB ceiling, with the SLM in a different process entirely.

### 7. The SoC is not the ceiling — the model is

```
Piper alan LOW (1 voice, 16 kHz)
load        3190 ms
synthesis   2236 ms for 16960 ms of audio
RTF         0.132   PASS (<= 0.15)
peak RSS    304 MB
on disk     77 MB
```

**0.132 against libritts_r's 0.354, on the same chip.** The 750G can hit the SLA; medium
quality at 22.05 kHz cannot. Two variables move together — Piper's `low` tier is a smaller
network *and* runs at 16 kHz, roughly 1.38x less audio to generate — so the 2.7x gap is both.

That reframes the decision. Not "is the hardware fast enough" but **"what do we spend the
speed on"**:

| | Voices | Quality | RTF |
| --- | --- | --- | --- |
| Piper `alan` low, 16 kHz | 1 | acceptable | **0.132** |
| Piper `libritts_r` medium, 22.05 kHz | 904 | "almost perfect" | 0.354 |

Piper publishes no multi-speaker model at the `low` tier — every multi-speaker option
(`libritts_r`, `vctk`, `arctic`, `l2arctic`) is `medium`. The two things we want are not
available in one file.

Three ways out, in the order to try them:

1. **Another medium multi-speaker model may be lighter.** `en_GB-vctk-medium`, 76 MB,
   ~109 speakers, different corpus, same tier. One benchmark says whether 0.35 is inherent
   to medium multi-speaker Piper or specific to `libritts_r`. Added to the probe.
2. **Re-derive the budget from power.** 0.354 is still 2.8x faster than real time and the
   host hands us a page at a time, so nothing starves. If sustained draw at 0.354 fits
   ~1.14 W, then 0.15 was simply the wrong number. QUI-016, and the measurement that
   actually decides.
3. **Differentiate one fast voice** with per-character pitch and rate offsets. Cheap,
   always affordable, audibly worse than real distinct speakers. The fallback if 1 and 2
   both fail.

Casting quality is the product. Reaching for option 3 first would be optimising the metric
and losing the thing the metric was for.

## Decision

**The engine is sherpa-onnx running a Piper/VITS model** — accepted. Kitten is ruled out on
quality, Kokoro on speed, and the runtime has proved itself on device.

**The voice model is not yet decided.** `libritts_r` medium gives the casting the product
needs but misses the RTF budget; `alan` low meets the budget with one voice. Settled by the
VCTK-medium benchmark and, failing that, by the power measurement in QUI-016.

## Consequences if confirmed

- QUI-010 targets sherpa-onnx with a VITS/Piper model.
- QUI-011's casting becomes an index into one model's speakers rather than a choice between
  models — a significant simplification, and it makes cast size free in memory terms.
- The footprint budget stops being the binding constraint on the attribution model, which
  matters for QUI-028.
- **Word alignments still do not exist.** `OfflineTts.generate()` returns samples only, so
  PRD §4.2's boundary timestamps must be estimated from span positions or bought with a
  forced aligner. Independent of which engine wins. See ADR-0004.

### 5. Threads do not help — measured

| Threads | RTF |
| --- | --- |
| 2 | 0.354 |
| 4 | **0.370** |

Four threads is *slightly worse* than two. The 750G's two performance cores are the whole
budget; scheduling onto the efficiency cores costs more in coordination than it returns.
Thread tuning is a closed avenue, and RTF ≈ 0.35 is what this model does on this SoC.

The open question is therefore whether 0.35 is the *SoC's* limit or this *model's*. A
single-speaker Piper "low" model has been added to the probe purely to answer it: if it
lands near 0.1, the hardware is fine and we need a smaller multi-speaker model; if it lands
near 0.3, the hardware is the ceiling and the RTF budget has to be re-derived from power.

### 6. Quote-mark inference cannot survive chunking — and that is the architecture's point

The probe switched voices on the chunk carrying the opening quote and then reverted to the
narrator for the rest of the line. The cause: it decided narration-versus-speech *per
chunk*, in isolation, so a continuation clause carrying no opening quote read as narration.

Patched in the probe by carrying quote state across utterances, but the lesson is the
architectural one. Any approach that infers who is speaking from the text of a single chunk
is defeated by clause-level chunking, which is exactly what @Voice's "alternating voices"
does. Quire does not infer: it looks the speaker up by position in the index, which is
state that survives chunking by construction.

## Still needed

1. ~~**Thread count.**~~ Measured: no help. See §5.
2. **Sustained power draw** against ≈1.14 W (QUI-016). This is what RTF was standing in
   for, and it decides whether 0.354 is actually a problem.
3. **Kokoro int8 (140 MB)** for completeness. Only the 304 MB fp32 build was tried, and it
   was slow throughout rather than slow to start, so int8 is unlikely to close a 2× gap —
   but the number is cheap to take.
4. **TTFS** end to end with the engine preloaded, against 800 ms.
