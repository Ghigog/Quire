# Target device profile — Onyx Boox Note Air5 C

The single reference device for all Quire measurements. Every SLA number in
[PRD §5](PRD.md#5-non-functional-requirements) is measured here unless a ticket says
otherwise, and every benchmark result records this device by name.

## Hardware

| | |
| --- | --- |
| SoC | Qualcomm Snapdragon 750G, octa-core (2× Kryo 570 Gold ≈ Cortex-A77 @ 2.2–2.4 GHz, 6× Silver ≈ Cortex-A55), 8 nm |
| RAM | 6 GB |
| Storage | 64 GB + microSD |
| Display | 10.3" E Ink Kaleido 3 — 2480×1860 (300 ppi) monochrome, 1240×930 (150 ppi) colour, 4096 colours |
| OS | Android 15 (API 35), with Google Play |
| Battery | 3,700 mAh |
| Input | EMR stylus (Pen3), touch, hardware buttons |

## What this means for Quire

### 1. RAM is not the wall we assumed — but the budget stays

6 GB is far more headroom than the 2–3 GB typical of cheaper e-ink hardware. The 1.2 GB
ceiling in PRD §5 is therefore a **self-imposed budget for good citizenship**, not a
hardware limit: Android 15 plus the Boox launcher and its stylus services take a
meaningful slice, and Boox firmware is aggressive about killing background processes
that grow. Staying near 1.2 GB is what keeps Quire alive in the background between
reading sessions.

Consequence: co-residency of the SLM and the TTS engine is probably viable here, which
softens `architecture.md` §4 considerably. It does not settle it — QUI-017 still
measures, because a design that only works on a 6 GB device narrows the product.

### 2. Prompt throughput, not generation throughput, is the bottleneck

The 750G has two performance cores with ARMv8.2 dot-product but **no i8mm** (that is
ARMv8.6). Quantized matmul falls back to the slower path. Expect single-digit to low
double-digit tokens/s generation for a 1B Q4 model.

That number breaks the naive design. A 100k-word novel has on the order of 3,000
dialogue lines; at ~300 prompt tokens of context per line, attributing them one at a
time re-processes ~900k tokens and lands in the **hours**, against QUI-007's 10-minute
budget.

The fix is structural, and it is why QUI-017 must measure the right thing:

- **Reuse the KV cache across a chapter.** Feed the chapter once as a rolling context and
  query speakers as you go, rather than rebuilding a 5-line window per line. This turns
  ~900k tokens of prompt work into ~130k for the whole book.
- **Constrain generation to a single token** — an index into the candidate speaker list,
  not free text. Generation then costs ~1 token per line instead of ~20.
- **Make Tier 1 carry what it can.** Every line the regex resolves is a line the model
  never sees, so coverage is a performance feature and not only an accuracy one.
  **Do not over-rate it, though:** measured on PDNC, only 30.1% of quotations across 28
  novels carry an explicit speech tag, and on some books it is 12%. Tier 1 shaves the
  workload; it cannot avoid the model (`docs/prior-art.md` §3).

So QUI-017 measures **prompt-eval tokens/s and the KV-cache reuse factor** as first-class
numbers, not just generation speed.

### 3. The panel is colour, and the reading surface still is not

Kaleido 3 puts a colour filter array over the panel. Colour costs half the resolution
(150 ppi vs 300 ppi) and cuts contrast even when displaying black and white. The
monochrome rule in CLAUDE.md §7 therefore holds for anything you read: body text,
highlighting, the reader chrome. Colour is permitted only away from the reading
surface — and never as the sole carrier of meaning.

### 4. The battery budget in watts

3,700 mAh at ~3.85 V is ≈14.2 Wh. PRD §5's "< 8% per hour" is therefore a sustained
budget of **≈1.14 W for the entire device** during playback — panel refreshes, audio
output, and synthesis combined. On an 8 nm SoC that is real but not generous: it rules
out keeping the SLM warm during playback, independent of the RAM question.

## Test device discipline

- Benchmarks record device model, Android build number and battery level at start.
- Measurements taken below 30% battery are discarded — thermal and DVFS behaviour
  changes.
- The device is not the only target, just the reference one. Anything that only works
  with 6 GB and a 750G is noted as such in its ADR.
