# Handoff — voice design and where the local model gets used

**Date:** 2026-09-02 · **Branch:** `claude/local-model-voice-accents-q7m6da`
**Reading order:** this file, then [ADR-0006](../adr/0006-three-attribution-jobs.md), then
[ADR-0007](../adr/0007-voice-is-a-description.md).

Written for the next agent picking up this thread with no memory of the conversation. It is
a thinking document, not a status report: the point is to hand over *what is actually
known*, *what is only asserted*, and *which questions are worth the next hour*.

---

## 1. Where the conversation had got to

The reader wants characters to sound like themselves — not just different timbres, but
accent and speech pattern. The starting question was whether a local model is needed at all
or whether attribution can be done deterministically. QUI-028 answered that: Tier 1
heuristics score 58.5% precision on PDNC and are right about **one line in nine** on
untagged material. A model is required. That is settled and is not worth relitigating.

From there, two things got worked out and both are now ADRs.

**A voice is a description, not a number.** Casting to a speaker id throws away everything
the scan learned at the moment it is cheapest to keep. ADR-0007 separates the four axes a
voice actually has and specifies a `voice` object for `characters.json`. QUI-032 lands it.

**"The model does the judging" was hiding three different jobs.** ADR-0006 names them —
cast discovery, per-quotation attribution, voice design — and makes two calls that matter:
voice design does **not** wait for attribution, and attribution is asked **once per scene**
rather than once per line.

---

## 2. What is measured, what is projected, what is assumed

Keep these apart. Most of the wasted effort in this repo's history came from treating a
projection as a measurement.

### Measured, on the reference device
- Piper `libritts_r` medium: RTF 0.354, load 2,524 ms, peak RSS 314 MB, 904 voices, 92 MB.
  4 threads is *worse* than 2. (ADR-0002)
- Tier 1 attribution: 58.5% precision over 2,846 PDNC quotations; ~1 in 9 on untagged
  lines; confidence values uncalibrated (`EXPLICIT_TAG` claims 0.95, measures 68.6%).
  (QUI-028 worklog)

### Measured, on the build host only — ratios transfer, absolutes never do
- Piper is 6–23× faster than every other multi-speaker engine in the zoo. int8 made Kokoro
  2.46× *slower*. (`spike/hostbench/README.md`)
- **All six bundled English espeak variants change the phoneme stream.** Deterministic
  waveform comparison, `en-us` control comes back bit-identical through the same patch
  path. Scots is +34% on the same sentence, Caribbean +17%. (ADR-0007, and reproduce it
  with the command in §5)

### Projected — reasonable, uncounted
- ~3,000 quotations per 100k-word novel survive Tier 1; 60–120 scenes per novel. Both are
  extrapolations from PDNC rates, and both should be counted on a real book when QUI-031
  runs. The scene-batching decision does not hinge on them — per-line loses by an order of
  magnitude, not by a margin — but the numbers themselves are soft.

### Assumed, unverified, and load-bearing
- **That a Scots phoneme stream through an American-trained model sounds Scottish.** It may
  sound like an American reader failing. Nothing measurable distinguishes those. This is
  QUI-033 and it is one listen away from being known either way.
- That the SLM can be resident beside a 314 MB TTS engine inside 1.2 GB. QUI-031.

---

## 3. The open questions, in the order I would take them

**1. Does the accent actually sound like the accent?** (QUI-033)
Cheapest decisive experiment available. The probe already generates the audio; it needs a
build, a copy and a listen. Binary outcome: six free accents, or axis 3 gets dropped and
ADR-0007 stands on its other three axes unchanged.

**2. Can one loaded engine switch espeak variant between utterances?**
This is the one that could make accents useless even if they sound perfect. sherpa-onnx's
VITS config exposes `lexicon`, `data_dir`, `dict_dir` — and nothing that names a variant.
The variant is ONNX metadata, read at load. So today, per-character accent means one engine
per accent: 2,524 ms and 314 MB each. That fits nothing.

Three things to try before concluding, roughly in cost order: whether `dict_dir` or
`lexicon` can override phonemisation without touching metadata; whether two `OfflineTts`
instances over identical weights share pages; and what a metadata patch plus reload
actually costs on device. If none works, accent becomes a *per-book* property (the narrator
and everyone in a Scottish novel) rather than per-character — still worth having, much
less interesting.

**3. What does a scene-sized prompt actually cost?** (QUI-031)
ADR-0006 changed the shape of the question this bake-off must answer. Measuring
single-line throughput would flatter the prefill cost that scene batching exists to
amortise. Measure the shape we will ship.

**4. Where does the descriptor come from?**
ADR-0006 says job C reads a handful of confidently-attributed lines and writes a prose
description. Nobody has written that prompt, or decided what happens to a character with
three lines and no explicit tags. Probably: no descriptor, fall back to pure F0-and-
distinctness casting. Worth thinking about before QUI-007 is built.

---

## 4. Things already ruled out — do not spend a session rediscovering these

- **A faster multi-speaker TTS model.** The search is exhausted (ADR-0002 §8, §9). Piper
  publishes no multi-speaker `low` tier; every alternative engine is 6–23× the cost.
- **Quote-mark inference for who is speaking.** Two mirror-image failure modes, no third
  setting of the flag, because the information is not in the quote marks (ADR-0002 §6).
- **Thread tuning on the 750G.** 4 threads is worse than 2 (ADR-0002 §5).
- **Editing the Piper sidecar `.json` to change the espeak voice.** sherpa-onnx reads the
  ONNX metadata and ignores the JSON entirely. Output is byte-identical.
- **Comparing single synthesis runs.** Piper samples durations from `noise_w`; three
  identical calls spread 0.38 s, wider than most real effects. Pin `noise_scale` and
  `noise_scale_w` to zero, or the measurement is worthless.
- **`en-gb` as an espeak identifier.** It fails to resolve even though the data ships an
  `en` file claiming `language en-gb`. Use the directory names under
  `espeak-ng-data/lang/gmw/`.

---

## 5. Reproducing the voice work

```sh
cd spike/hostbench
python3 -m pip install sherpa-onnx onnx numpy
./fetch-models.sh                       # ~1.1 GB, not committed
python3 voiceprobe.py --list            # metadata and the variants this model ships
python3 voiceprobe.py --mode accent     # the ADR-0007 table
python3 voiceprobe.py --mode rate       # length_scale against the same control
```

`voiceprobe.py --mode accent` takes a few minutes: it re-saves the 92 MB model once per
variant. Read the two control rows before believing anything below them.

Note: `spike/hostbench/models` was committed as an absolute symlink into a previous
session's `/tmp` scratchpad, so `fetch-models.sh` wrote into a dead path on any fresh
checkout. It is now untracked and `.gitignore` covers it. If you see it reappear in
`git status`, do not commit it.

---

## 6. The thread the reader is actually pulling on

Worth keeping in view, because it is easy to optimise the wrong thing. The ask is not
"more voices". It is that **a character should be recognisable before you are told who is
speaking** — the thing a good audiobook narrator does. Three of the four axes in ADR-0007
already deliver some of that, and one of them (what the character says) is free because the
author wrote it.

The open design question underneath all of §3 is how much of a *performance* we are willing
to synthesise versus how much we simply read faithfully. Accent sits right on that line:
it is a property of the character, but we would be inventing it from a description rather
than reading it off the page. Nobody has decided how confident the scan has to be before it
gives someone an accent, or whether a reader should be asked. That is the conversation this
handoff exists to continue.
