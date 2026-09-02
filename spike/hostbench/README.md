# Host-side relative screen for TTS candidates

Part of QUI-017. Answers one question cheaply: **is this candidate worth a device cycle?**

Every number the bake-off actually cares about — RTF against PRD §5, TTFS, sustained power —
has to come off the reference device. This runs the same `sherpa-onnx` runtime on whatever
x86 box is to hand, so it can compare two candidates against each other without waiting for
a build, an install and a listen.

```sh
./fetch-models.sh                                   # ~330 MB, not committed
python3 -m pip install sherpa-onnx
python3 bench.py                                    # matrix over every fetched model
python3 bench.py --paired vctk libritts_r           # interleaved A/B for close calls
```

## What it can and cannot tell you

**It cannot give you an SLA number.** A desktop core with wide vectors is not a Cortex-A77.
Absolute RTF here is meaningless for the device and must never be quoted as if it were.

**Ratios within a tier and sample rate transfer well.** Two medium 22.05 kHz VITS models of
similar size differ on the device roughly as they differ here.

**Ratios across tiers demonstrably do not.** Measured, and worth keeping in view:

| Comparison | Host (x86, 2 threads) | Device (750G) |
| --- | --- | --- |
| vctk-medium ÷ libritts_r-medium | **0.995** | not yet run |
| alan-low ÷ alan-medium | **0.76** | — |
| alan-low ÷ libritts_r-medium | **0.72** | **0.37** |

The last row is the warning. The host says a low-tier model buys about 1.4×; the device
measured 2.7× for the same pair. Something on the 750G punishes medium-tier 22.05 kHz
synthesis roughly twice as hard as this machine does — plausibly memory bandwidth around
the decoder, since `alan-low` and `alan-medium` are the same size on disk (63 MB) and differ
mainly in output rate. Untested either way, and it is why cross-tier screening here is a
hint and not a result.

## Screened so far

Median of 5–7 interleaved runs, 2 threads, `length_scale` equalised. Relative column is
against `libritts_r-medium`, the incumbent.

| Model | Voices | Rate | Host RTF | Relative |
| --- | --- | --- | --- | --- |
| `vits-piper-en_GB-alan-low` | 1 | 16 kHz | 0.047 | 0.73× |
| `vits-piper-en_GB-alan-medium` | 1 | 22.05 kHz | 0.060 | 0.93× |
| `vits-piper-en_GB-vctk-medium` | 109 | 22.05 kHz | 0.065 | 1.00× |
| `vits-piper-en_US-libritts_r-medium` | 904 | 22.05 kHz | 0.065 | 1.00× |
| `vits-vctk` (non-Piper VITS) | 109 | 22.05 kHz | 0.369 | **5.7×** |
| `kokoro-multi-lang-v1_1` (fp32) | 103 | 24 kHz | 0.607 | **9.4×** |
| `kokoro-int8-multi-lang-v1_1` | 103 | 24 kHz | 1.493 | **23×** |

Two things worth taking away.

**Piper is not narrowly ahead, it is an order of magnitude ahead.** Every other
multi-speaker engine in the zoo is 6–23× its cost on the same runtime and machine. There is
no faster multi-speaker model to go and find.

**int8 made Kokoro 2.46× slower, not faster.** Paired against the identical unquantized
model: 1.493 against 0.607. Quantized weights are smaller on disk but the graph pays
dequantize/quantize conversions around operators with no fused int8 kernel, and here that
costs more than the arithmetic saves. It is a reminder that "int8" is a size decision that
*may* be a speed decision, and never automatically is — and on a chip without i8mm the
odds are worse, not better.

`vits-vctk` has no curly quotes in its lexicon and drops them with an OOV warning. Quote
marks are not spoken, so the timing stands, but the phoneme stream is not byte-identical to
the Piper runs.

## Method notes

- Median of N interleaved runs after a discarded warm-up. Run-to-run spread on a shared VM
  is around 8%, which is wider than most differences worth arguing about — hence `--paired`,
  which alternates candidates so noise hits both equally.
- `length_scale` is forced to 1.0. VCTK ships 1.4, and a model that speaks more slowly
  produces more audio per unit of compute; leaving it alone compares voices, not engines.
- Speaker id is the midpoint of the model's range, not 0. Adjacent ids in `libritts_r` are
  neighbouring readers from one corpus and sound alike — the mistake the first device test
  made.

## `voicelab.py` — inventing a voice instead of picking one (QUI-034)

`bench.py` asks which engine to ship. `voicelab.py` asks a different question: whether the
engine we shipped can produce a voice that is not one of its 904 speakers.

Both levers turned out to be editable fields inside the model file, needing no new runtime:

- **Timbre** — `emb_g.weight` is a `[904, 512]` initializer in the ONNX graph. A voice is
  512 floats, 2 KB. Writing a row that was never trained produces a working voice.
- **Accent** — the espeak-ng variant lives in the ONNX `metadata_props["voice"]`. The
  model's `.onnx.json` is **not read by sherpa-onnx**; patching it changes nothing, which
  is worth knowing before you spend an afternoon on it as this file's author did.

```bash
./fetch-models.sh
python3 voicelab.py blend     # interpolate two speakers, measure the pitch of the result
python3 voicelab.py accent    # swap the phonemiser, measure whether it reaches the model
```

### The measurement trap in this one

Piper is stochastic — `noise_scale` and `noise_w` are both 0.333 — so two *identical* calls
differ by rms ~0.15 and by ±10k samples. The first version of this probe compared one
waveform per accent and duly found that every variant "differed"; the control found that
en-US differs from itself by the same amount. Nothing here is single-shot: every comparison
is repeated and quoted against its own spread, and `blend` prints the control before the
result.

Duration also only sees accents that change how many phonemes a word has. Scots (+1.4 s,
3.9 sd) and Caribbean (+1.2 s, 2.9 sd) clear the noise; RP and Lancashire do not, because
they differ in vowel *quality* and rhoticity rather than phoneme count. That is this
probe's blind spot, not a null result — it takes an ear on the device to settle.
