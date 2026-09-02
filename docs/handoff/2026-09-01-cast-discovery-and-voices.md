# Handoff — the state of the thinking

**Living document.** Rewrite it, don't append to it. The durable record is
[`tickets.md`](../tickets.md) and [`docs/adr/`](adr/); this is the part that does not fit in
either — the frame the decisions were made in, and the questions worth arguing about next.

**Last updated:** 2026-09-02 by `session-visibility-check`.

---

## 1. What Quire is, in the reader's words

> "I send a file to the app. The app reads it, and generates a note for itself; the
> characters, how they speak, and therefore which voice to use for them. Once that's done,
> the app tells me so. I then go to the EPUB and activate the TTS and it works."

**Quire is not an EPUB reader and never displays a book.** It is a parser plus a system TTS
engine. The reader keeps reading in NeoReader (or whatever they use) and presses that app's
Read Aloud; Quire is what speaks. `core:epub` is a *parser*, used once at import and then the
file is deleted. If a plan starts drifting toward a reading UI, it is the wrong plan.

The product thesis in one line: **speaker is resolved by position in a pre-built index, never
inferred from punctuation in the chunk the host hands us.** ADR-0004 §6 records why — the
host chunks by clause, so a chunk's quote marks are unbalanced in both directions and quote
inference is defeated. Everything hard about this project follows from that one commitment.

## 2. The three jobs

The single most useful frame from the 2026-09-02 session. Most confusion in earlier sessions
came from collapsing these into "attribution".

| | Job | Size per novel | Where it stands |
| --- | --- | --- | --- |
| **A** | Who is in this book? | one pass | Heuristics, 84.8% precision (QUI-034) |
| **B** | Who says line 4,412? | ~3,000 unresolved quotations | Heuristics + turn-taking; the real gap |
| **C** | What does each character sound like? | ~10 judgements | Not built — ADR-0009 |

Two things fall out of this and both matter:

- **C does not depend on B.** To design Geralt's voice you need *some* lines you are
  confident are his, not all of them. Explicit speech tags supply those (PDNC: 30.1% of
  quotations, and the slice the regex is actually good at). The apparent circularity —
  "you need attribution to sample a character, and a character to do attribution" — is not
  real.
- **Only B is expensive**, which is what makes on-device viable at all (ADR-0008).

## 3. What is decided

| Decision | Where | One-line reason |
| --- | --- | --- |
| Piper `libritts_r` medium is the engine | ADR-0002 | 6–23× ahead of every alternative screened; 904 speakers in 92 MB |
| Intercepting Read Aloud works | ADR-0004 | Verified on device 2026-08-27 |
| All book analysis runs on-device | ADR-0008 | CLAUDE.md §8 is a hard rule; cloud was costed, not hand-waved |
| Voices are generated, not assigned | ADR-0009 | A voice is 512 editable floats; assignment throws away what the analysis knew |

## 4. What is measured (so don't re-derive it)

- **Cast discovery:** 84.8% precision, 82.3% recall over PDNC's 28 novels. Was 41.1%.
  Gender inferred for only **58.7%** of the cast — this is why a book of men read in
  women's voices, and it is a coverage problem, not an accuracy one (90.9% when it commits).
  QUI-034, QUI-035.
- **Tier 1 attribution:** 58.5% precision on PDNC. Explicit tags are only 30.1% of
  quotations, anaphoric 24.8%, implicit 45.0%. QUI-028.
- **Voice generation:** an untrained interpolated speaker embedding synthesises cleanly, F0
  moving 14–29 Hz per step against a 2.16 Hz noise floor. Accent variants reach the model
  (Scots +1.39 s, 3.9 sd). QUI-036.
- **Engine:** RTF 0.354 against a PRD budget of 0.15, accepted as a recorded deviation
  because nothing faster is multi-speaker. Peak RSS 314 MB. ADR-0002.
- **Budget that constrains everything next:** QUI-007's **30 minutes for a 100k-word novel**.

## 5. Open questions worth theorising about

Roughly in order of how much they would change the plan.

1. **Does a generated voice sound like a person?** The whole of ADR-0009 rests on this and
   nothing has been listened to. A blended embedding could be a plausible new speaker or an
   averaged mush; F0 cannot tell you. **Highest-value test available on the next device
   build**, and it is five minutes of listening.
2. **Do Scots phonemes through an `en-US`-trained model sound Scottish, or broken?**
   Out-of-distribution by construction. If broken, accent collapses back to "what the author
   wrote in the dialogue" and ADR-0009's accent claim needs striking.
3. **How do you get from "gravelly older man" to 512 floats?** The genuinely unsolved design
   problem. Sketches, none tried: (a) nearest-neighbour in a space of *measured* speaker
   attributes — we have F0 for all 904, so what else is cheap to measure? (b) learned
   directions, found by contrasting groups of speakers, then moved along; (c) give the model
   a described menu of anchor speakers and let it pick and blend. (a) is the cheapest and
   (b) is the one that would actually feel like a foundry.
4. **Can an SLM hit B's throughput?** ~3,000 decisions in 30 minutes. Per-line calls are
   hopeless; **per-scene** calls amortise the prefill and are better for quality anyway,
   since a model that sees the scene resolves turn-taking from context. QUI-031 and ADR-0003.
5. **What belongs in the voice description?** The useful set is the intersection of what is
   *inferable from prose* and what is *realisable in the engine*. Age, sex, pitch, pace and
   roughness are plausibly both. Accent is inferable but only conditionally realisable (see
   2). Anything outside that intersection is decoration.
6. **Is gender still the right abstraction?** QUI-035 wants to raise gender coverage because
   `Casting` uses gender to pick a pool. Under ADR-0009 gender is one input to a pitch and
   timbre target, not a selector. **Revisit QUI-035's framing before working it** — it may be
   solving a problem that no longer exists in that shape.
7. **How much of "speech patterns" is already free?** Dialect, verbal tics and formality are
   in the words the author wrote; TTS only controls delivery. Possibly the cheapest quality
   win available, and entirely unexplored.

## 6. Traps that have already cost time

- **`sherpa-onnx` does not read the model's `.onnx.json`.** Config comes from the ONNX
  `metadata_props`. Patching the JSON changes nothing and looks exactly like the idea failing.
- **Piper is stochastic.** `noise_scale` and `noise_w` are both 0.333, so two *identical*
  calls differ by rms ~0.15 and ±10k samples. Any single-shot A/B on the waveform "proves" a
  difference between a thing and itself. Always run the control.
- **Fixture-tuned constants do not survive real books.** `ADJACENCY_MIN` was 2, tuned on
  four-paragraph fixtures, and produced 157 characters on a real novel. It is 8 now, measured.
- **Attribution accuracy is blind to roster junk**, because invented names are never in
  speech-tag position. Measure the cast separately: `spike/pipeline cast`.
- **Never copy `core:` sources into a spike** — composite builds (`includeBuild("../..")`).
  Two copies of the normalisation rules is a day lost to a phantom matcher bug.
- **Owner on the board is the branch slug, not the model name.** Every session is the same
  model; signing `claude-opus-5` makes concurrent agents indistinguishable.
- **A green `gradle test` says nothing about the Android code** — the root build excludes
  `spike/ttsbinding`. CI builds the APK.

## 7. If you are picking this up

Read in this order: `CLAUDE.md` → this file → ADR-0008 and ADR-0009 → the Worklogs of
QUI-034 and QUI-036. Then:

```bash
gradle test                                    # whole JVM suite, seconds
cd spike/pipeline && gradle installDist        # then: cast <pdnc>/data/*   (see QUI-034)
cd spike/hostbench && python3 voicelab.py blend    # needs ./fetch-models.sh first
```

PDNC is not committed (no licence — `docs/prior-art.md` §3); clone it fresh:
`git clone --depth 1 https://github.com/Priya22/project-dialogism-novel-corpus.git`

**The next thing that actually needs doing is a device build and a listen** — questions 1
and 2 above. Everything else is theory until those are answered.
