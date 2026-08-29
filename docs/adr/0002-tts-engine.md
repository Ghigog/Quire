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

**Second listen, same day: carrying the state fixed the comma case and broke the paragraph
case.** Narration on the *same line* as a quotation returns to the narrator correctly;
narration on a *new line* stays in the character's voice.

The cause is ordinary typography. A speech continuing across paragraphs opens a quote on
every paragraph and closes only at the end of the last one, so the probe sees openers
without closers and the flag latches true. Sources that use `“` at both ends of a
quotation latch it the same way.

The two failures are mirror images: per-chunk state breaks *within* a line, carried state
breaks *across* paragraphs, and there is no third setting of the flag, because the
information is not in the quote marks at all. Quote-mark inference is therefore not a weak
signal to be strengthened — it is the wrong signal. The probe is left as it is; it has
answered its question.

### 7. VCTK is not the way out — screened on the host, 2026-08-29

The open question after §5 was whether RTF ≈ 0.35 is inherent to medium-tier multi-speaker
Piper or specific to `libritts_r`. It is not specific to `libritts_r`.

`spike/hostbench` runs the same `sherpa-onnx` runtime on an x86 host. It cannot produce an
SLA number — a desktop core is not a Cortex-A77 — but it compares candidates against each
other without a build-install-listen cycle. Interleaved, seven runs each, `length_scale`
forced equal so neither model is flattered by speaking slower:

| Model | Voices | Host RTF (median) | Range |
| --- | --- | --- | --- |
| `en_GB-vctk-medium` | 109 | 0.0566 | 0.0549–0.0600 |
| `en_US-libritts_r-medium` | 904 | 0.0569 | 0.0549–0.0593 |

**Ratio 0.995, with the ranges fully overlapping.** The two are the same engine wearing a
different corpus. Expect `vctk` to land within noise of 0.354 on device, and note that it
would buy 109 voices in place of 904 for nothing. Option 1 is closed.

A second host result bounds what the low tier is actually buying. `alan-low` and
`alan-medium` are the same voice, the same corpus, and the same 63 MB on disk, differing
mainly in output rate:

| Comparison | Host | Device |
| --- | --- | --- |
| `alan-low` ÷ `alan-medium` | 0.76 | — |
| `alan-low` ÷ `libritts_r-medium` | 0.72 | **0.37** |

The host says the low tier buys about 1.4×, most of which is the 1.38× fewer samples at
16 kHz rather than a smaller network. The device measured 2.7× for the same pair. **The
750G punishes medium-tier 22.05 kHz synthesis about twice as hard as the host does**, which
is a property of the device worth knowing and is not explained by anything in the model
files. Most likely memory bandwidth around the decoder — `device-profile.md` §2 already
says this SoC is bandwidth-poor — but that is a hypothesis, not a finding.

Thread scaling shows the same divergence from the opposite direction. On the host
`libritts_r` goes 0.092 → 0.057 → 0.045 across 1, 2 and 4 threads; on device 4 threads was
*worse* than 2. The scheduling result in §5 is therefore not measurement noise: it is
big.LITTLE behaving unlike a homogeneous machine, and it confirms that nothing about this
device's performance can be inferred from a desktop.

**Consequence for the decision.** Of the three ways out, option 1 is closed and option 3
(pitch and rate offsets on one fast voice) trades away the thing the product is for. That
leaves option 2: re-derive the budget from sustained power (QUI-016). 0.354 is still 2.8×
faster than real time, and ADR-0004 establishes that the host hands us a page at a time,
so nothing starves. Either power says multi-voice at medium quality is affordable, or it
says the engine must get faster and we are choosing between 109 voices and none.

## Still needed

1. ~~**Thread count.**~~ Measured: no help. See §5.
1b. ~~**A lighter medium multi-speaker model.**~~ Screened on the host: `vctk` is within
   0.5% of `libritts_r`. See §7.
2. **Sustained power draw** against ≈1.14 W (QUI-016). This is what RTF was standing in
   for, and it decides whether 0.354 is actually a problem.
3. ~~**Kokoro int8 (140 MB).**~~ Not taken, by decision (dylangrowcoot, 2026-08-28). The
   304 MB fp32 build was slow *throughout* rather than slow to start, which makes it a
   compute problem and not a size one; quantizing cannot close a 2.4× gap on an SoC with
   no i8mm (`device-profile.md` §2). Reopen only if Kokoro becomes the only multi-speaker
   option left.
4. **TTFS** end to end with the engine preloaded, against 800 ms.
