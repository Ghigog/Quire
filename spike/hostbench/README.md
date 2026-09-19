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
python3 voiceprobe.py --mode accent                 # espeak variant, deterministic (QUI-033)
python3 voiceprobe.py --mode accent --wav-dir out   # ...and keep the audio to listen to
python3 mos.py path/to/wavs/                        # quality screen beside the speed one (QUI-044)
```


## listen.py — the QUI-039 listening test

Renders the script written by `quire-pipeline-spike listen`: the same passage twice, differing
only in which character each line is given to. It decides nothing — passages, both
attributions and the narrator fallback are settled in the JVM where they have tests.

```bash
./fetch-models.sh vits-piper-en_US-libritts_r-medium
../pipeline/build/install/quire-pipeline-spike/bin/quire-pipeline-spike listen
python3 listen.py ../pipeline/build/listen/script.json --wav-dir ../../build/listen
```

Unlike `voiceprobe.py` this one does **not** pin the noise terms. The probes need determinism
so two variants can be compared sample for sample; this one is judged by ear, and the flat
render determinism buys would be the wrong thing to listen to. Files come out as `A` and `B`
with the mapping in `key.txt`, so the listen can be blind.


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
against `libritts_r-medium`, the incumbent. MOS is UTMOSv2, 10 repetitions, narration-length
audio — see `mos.py` below; a row without one has not been screened for quality yet.

| Model | Voices | Rate | Host RTF | Relative | MOS |
| --- | --- | --- | --- | --- | --- |
| `vits-piper-en_GB-alan-low` | 1 | 16 kHz | 0.047 | 0.73× | — |
| `vits-piper-en_GB-alan-medium` | 1 | 22.05 kHz | 0.060 | 0.93× | — |
| `vits-piper-en_GB-vctk-medium` | 109 | 22.05 kHz | 0.065 | 1.00× | — |
| `vits-piper-en_US-libritts_r-medium` | 904 | 22.05 kHz | 0.065 | 1.00× | **2.93** FAIL |
| `vits-vctk` (non-Piper VITS) | 109 | 22.05 kHz | 0.369 | **5.7×** | — |
| `kokoro-multi-lang-v1_1` (fp32) | 103 | 24 kHz | 0.607 | **9.4×** | — |
| `kokoro-int8-multi-lang-v1_1` | 103 | 24 kHz | 1.493 | **23×** | — |

`kitten-nano-en-v0_1-fp16` is not a row here — ADR-0002 ruled it out on quality before this
table's speed screen existed — but it is the calibration pair for `mos.py` below: **2.11**,
lower than `libritts_r`'s 2.93, the same order ADR-0002's ear gave them.

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

## mos.py — the QUI-044 quality screen

`bench.py` answers whether a candidate is worth a device cycle on speed. It says nothing
about whether it is worth a human ear, and that gap is why ADR-0002 accepted an engine that
QUI-039's listen later called **"not audiobook quality"** — the RTF screen had nothing to
say about it either way. `mos.py` scores rendered audio with **UTMOSv2**
(`sarulab-speech/UTMOSv2`, MIT, reference-free MOS prediction), so a candidate can fail a
quality filter before anyone's ears are spent on it.

```bash
python3 -m pip install "git+https://github.com/sarulab-speech/UTMOSv2.git"
HF_HUB_DISABLE_XET=1 python3 mos.py ../../build/listen   # score a QUI-039 listening set
HF_HUB_DISABLE_XET=1 python3 mos.py path/to/wavs/        # any directory of .wav files
```

`HF_HUB_DISABLE_XET=1` matters here the same way it does in `tools/fetch-models.sh`: without
it, `transformers`' `AutoModel.from_pretrained` hangs against the Xet CDN hosts CLAUDE.md §9
documents as refused, rather than falling back to the plain HTTP path that works. First run
also pulls a ~800 MB fold checkpoint and the `timm`/`transformers` backbones UTMOSv2 is built
from — none of it committed (CLAUDE.md §6) — to `~/.cache/utmosv2`.

**Calibrated against a known judgement.** Piper `libritts_r-medium` — the engine QUI-039
judged by ear as not audiobook quality — scores **2.93** on an isolated narration passage,
well under the 3.5 bar. The QUI-039 listening-test renders themselves (Daisy Miller, natural
and disagreements tracks, 206–543 s each, same engine) score **3.18–3.43**, mean 3.30 — also
under bar, but a third of a point higher than the isolated passage. That gap is bigger than
the run-to-run noise `--repetitions` exists to average out, and it is recorded rather than
smoothed over, because it is a second finding in its own right — see "What this cannot tell
you" below.

**It ranks, not just rates.** ADR-0002 (2026-08-28) is the one place two engines were ranked
by ear before this tool existed: Piper `libritts_r` "almost perfect", Kitten nano's quality
"judged unacceptable" and ruled out for the narrator. Scored on matched narration-length
clips, UTMOSv2 puts them in the same order — Piper **2.93**, Kitten nano **2.11** — the one
pair in this repository where the metric can be checked against a verdict rather than just
compared against a fixed bar.

**What this cannot tell you.** Two things worth being plain about, because both were found
by running the tool rather than by reading about it.

1. **A "long-form" score is not a long-form listen.** UTMOSv2 crops every forward pass to a
   3-second SSL window plus two 1.4-second spectrogram frames, so a 542-second render and a
   32-second one cost the model the same single glance. `--repetitions` (default 10, this
   script's choice — UTMOSv2's own default is 1) buys more independent crops of the file,
   not a longer one; it is the reason two back-to-back single-pass scores of the *same* 32 s
   clip landed 3.12 and 3.01, and why the number above is a mean of ten, not one.
2. **Piper's own four QUI-039 tracks spread 0.25 points (3.18 to 3.43) at ten repetitions
   each, despite being the same engine end to end** — only who is cast to which line differs
   between the `A` and `B` file in a pair, and between the natural and disagreements tracks.
   That is a wider spread than ten repetitions collapsed the single-clip noise to in the
   calibration above. Whether it is the metric responding to casting rather than to the
   engine, or plain residual noise at ten repetitions rather than a hundred, is not answered
   here; it is recorded so nobody quotes one track's number as the engine's.

**A MOS score filters candidates. It never settles a choice.** It is trained on VoiceMOS
Challenge corpora — short, single-speaker, crowd-rated clips — not on minutes of a fictional
cast, and QUI-036/QUI-033 both found a cheap host-side proxy can look clean on paper and miss
the thing a person actually hears. A low score disqualifies; a high score means only "worth
a listen" — CLAUDE.md §1's rule that the host screens and an ear decides. Neither stands in
for QUI-039's harness, which is what a shipping decision still needs.

## `voicelab.py` — inventing a voice instead of picking one (QUI-036)

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
python3 voicelab.py blend --wav-dir out    # ...and keep the ramp to listen to
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
they differ in vowel *quality* and rhoticity rather than phoneme count. That was this probe's blind
spot, and `voiceprobe.py` below has since closed it: with both noise terms pinned to zero
synthesis is exactly reproducible, and every one of the six variants changes the waveform,
RP and Lancashire included. What still takes an ear on the device is whether any of them
*sounds* like the accent.

## voiceprobe.py — the axes outside the speaker table

`bench.py` asks how fast a model is. `voiceprobe.py` asks what a voice can be *made to do*:
speaking rate (`length_scale`) and accent (the espeak-ng variant Piper bakes into the ONNX
metadata as `voice`). See [ADR-0007](../../docs/adr/0007-voice-is-a-description.md).

Two things it does that are worth copying into any future audio probe here.

**It pins `noise_scale` and `noise_scale_w` to zero.** Piper samples durations per call, so
three identical runs spread 0.38 s — wider than most effects worth testing. With sampling
off, synthesis is exactly reproducible and waveforms can be compared sample for sample, so
"did this knob reach the model" becomes a yes/no rather than a significance test.

**It re-saves the model through the same path for the control.** The `en-us` row is the
model's own variant patched back onto itself; it returns bit-identical output, which is
what makes a difference on any other row attributable to the variant rather than to the
patching.

It says whether a knob reaches the model. It cannot say how the result sounds — the caveat
at the top of this file, in its strongest form.

## Getting the audio off this machine

`--wav-dir` is how the caveat gets answered rather than just restated. Both probes take it,
both write mono 16-bit PCM through `wavout.py`, and both write the waveform the row above
was *measured* from — no second synthesis, no normalisation, so the file and the number
describe the same take.

`voiceprobe.py --mode accent` writes one file per espeak variant, same speaker and same
sentence throughout, so what changes between two files is pronunciation and nothing else.
`voicelab.py blend` writes the interpolation ramp with both parent speakers either side of
it, numbered `00` and `06` around `01`–`05`, because "is this a third person or an average
of two?" is a question about the sequence rather than about any one file in it.

**The accent files are the deterministic render**, noise pinned off. That is what makes two
variants comparable at all, and it is not how the app will render: judge pronunciation from
them, not naturalness.

Names are long on purpose. A tester scrolling a flat list on the device has the file name
and nothing else — no table, no console log, no this file — so each name carries its
position in the listening order, the condition, and the measurement:

```
accent-05-en-gb-scotland-scots-spk447.wav
blend-03-t050-invented-156hz.wav
```

CI renders both probes on every push and attaches the directory as **`voice-probe-wavs`**,
next to `quire-probe-apk`. The probes' console output is in there too, as `accent-probe.txt`
and `blend-probe.txt`, since that is the only place the numbers behind each file are
written down. Nothing needs building locally to get a listen: download the artifact.
