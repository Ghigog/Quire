# Handoff — where attribution actually stands

**Date:** 2026-09-08 · **Supersedes for attribution:** the open questions in
[2026-09-02](2026-09-02-voice-and-attribution.md), most of which are now answered.
**Reading order:** this file, then [ADR-0005](../adr/0005-attribution-model.md), then
QUI-028's Worklog.

Written for an agent with no memory of the last two days. It is a thinking document: what
is known, what is only asserted, and what the next hour should go on.

---

## 1. The one-paragraph state

**Voice generation is solved.** A character's voice is 512 writable floats in the TTS
model, generated rather than picked, and confirmed by ear on the device
([ADR-0009](../adr/0009-voices-are-generated.md), QUI-036/037). Accent was tried and
dropped ([ADR-0007](../adr/0007-voice-is-a-description.md), QUI-033).

**Attribution is not solved, and it is the whole remaining risk.** Rules answer the fifth
of dialogue that carries a speech tag. Nothing yet answers the rest without making the
product worse.

---

## 2. The metric, because it inverts the obvious reading

**`wrong voice = coverage × (1 − precision)`** — the share of all quotations read in
somebody else's voice. PRD §3.1: a missing voice is flat, a wrong voice is *heard*.
ADR-0005 makes this the headline attribution metric, above accuracy.

Every candidate so far ranks differently depending on which column you read, and accuracy
is the misleading one.

| One novel, `A Handful of Dust`, 2,337 quotations | coverage | precision | accuracy | **wrong voice** |
| --- | ---: | ---: | ---: | ---: |
| Tier 1 rules | 18.4% | 88.6% | 16.3% | **2.1%** |
| SpanBERT encoder, 431 MB | 88.0% | 52.9% | 46.6% | 41.4% |
| BookNLP `small`, 57 MB | 98.5% | 44.0% | 43.3% | 55.1% |
| Llama 3.2 1B, per scene | 9.4% | 32.4% | 3.0% | 6.4% |

On eight novels (12,521 quotations) Tier 1 and BookNLP hold the same ordering, and the
**out-of-domain gap is four times larger for the model**: Tier 1 loses 4.1 accuracy points
on held-out books and gets *more* precise; BookNLP loses 17.9 and gets less. PDNC stops in
1934, so contemporary genre fiction is not in any of these numbers at all.

---

## 3. What is settled

- **Encoders do not replace Tier 1** (ADR-0005). Both published candidates roughly double
  accuracy and take wrong-voice from ~3% to ~40%. No confidence threshold recovers them:
  at 0.99 Tier 1 wins on coverage *and* precision simultaneously, so the model is
  confidently wrong rather than unsure.
- **BookNLP `small` wins the Explicit slice** — 88.9% accuracy against Tier 1's 77.9% at
  the same precision, over four novels, for 57 MB. A real, optional, narrow win.
- **The model is prompted once per scene, not once per line** (ADR-0006), and scenes are
  real: `core/attribution/scenes` segments them, `SceneSplitter` splits the 75% that
  exceed a 2,048-token budget. Measured: **median 30 scenes/novel**, not the 60–120 the
  ADR guessed; median 31 quotations per scene.
- **The runtime seam exists** (QUI-006): `SlmRuntime`, structured output with one retry,
  cancellation. ADR-0001 is `Proposed`, blocked only on device numbers.

---

## 4. The open question, and it is the only one that matters

**Can anything take the untagged three quarters of dialogue without wrecking the
wrong-voice rate?**

Encoders: answered, no. A scene-level generative model is the last candidate, and its
number is **not yet trustworthy**. Read QUI-028's 2026-09-08 Worklog entry before
believing anything about it.

### Do this first, before any other measurement

**Fix precision on Explicit quotations.** It reads **51.5%** against Tier 1's 91.8%. Those
are the quotations whose speech tag names the speaker in the same sentence — a model that
can read English should be near-perfect. Until that number is high, nothing else the SLM
produces means anything.

That single sub-score caught all four harness faults found so far. Each produced a
plausible headline first:

| Fault | Symptom |
| --- | --- |
| Unconstrained output | Model returned a JSON *object*, not an array. 104/112 pieces unparseable |
| `head -8` in the run command | Killed a run partway, left a complete-looking 639-answer file |
| All 104 book characters offered per quotation | Encoders only ever rank mentions *in the window*; a reader does too |
| Candidates from `Main Name`, not aliases | `Mrs. Beaver` never matched `Mrs Beaver`; **the right answer was often not on the list** |

**The lesson is the method, and it is worth carrying:** check a sub-score whose correct
value you can predict from first principles, not the headline. The headline always looks
plausible. This is the same failure that made QUI-028 retract its original 58.5%.

### Then, in order

1. **A prompt that does not offer `"?"` as an easy out.** The model takes it ~90% of the
   time, which is why coverage is 9.4%.
2. **A larger model.** Qwen 2.5 1.5B, or a 3B, to find where capability starts. If a 3B
   clears it and a 1B cannot, that is a device question rather than an approach question.
3. **A wider corpus,** only once a single novel looks right.

---

## 5. Reproducing the attribution work

```sh
tools/fetch-pdnc.sh
python3 -m pip install llama-cpp-python huggingface_hub grimbert
cd spike/pipeline
gradle run --args="dump --out build/bakeoff --novels AHandfulOfDust"   # paragraphs, questions, scenes
python3 predictors/slm_predict.py build/bakeoff --novels AHandfulOfDust   # ~20 min
gradle run --args="bakeoff --candidate slm --answers build/bakeoff --novels AHandfulOfDust"
```

`gradle run --args="scenes"` reports segmentation over all 28 novels. Candidates
`tier1`, `tier1-nobeats`, `tier1-tags-only` need no model.

**Everything is scored by one scorer in Kotlin**, whatever language the predictor is in.
That is deliberate: what is compared is models, not two scoring codebases disagreeing
about what counts as a match. The dump carries **no gold speakers**, so a predictor cannot
score itself by accident.

---

## 6. Environment traps that cost real time

- **`HF_HUB_DISABLE_XET=1` or large Hugging Face downloads hang** rather than failing.
  Half an hour lost to this. Now in CLAUDE.md §9.
- **Berkeley's BookNLP host is https-only**; BookNLP's own code uses `http` URLs, refused.
- **The container is reclaimed roughly every 20–40 minutes** under load — three restarts
  in one session. Single-novel runs are the right unit here; a 28-novel run will not
  survive. Never pipe a long run through `head`.
- Root `gradle test` does not include `spike/ttsbinding` or `spike/pipeline`. Run
  `spike/pipeline`'s suite separately.

---

## 7. What needs the device, and what does not

**Nothing is worth a device hour right now**, and QUI-016 says so on the ticket with its
reasoning. The power measurement would be taken against a TTS-only configuration that
never ships, and ADR-0002 §9 already exhausted the search for a faster engine, so a
failing number would unblock nothing.

The right device sitting is **after** an SLM backend exists: QUI-031's co-residency and
throughput, plus power, in the configuration that actually ships, measured once. A
ten-minute power run is the one cheap exception, and only to check the order of magnitude.

---

## 8. If the answer turns out to be no

Worth deciding in advance, because it is a real possibility and it changes the PRD rather
than a ticket.

If no available model takes untagged dialogue at an acceptable wrong-voice rate, the
honest product is **multi-voice on tagged dialogue, narrator elsewhere**. That is still
worth shipping: it is the passage where a reader most notices voices, BookNLP `small`
already buys 11 accuracy points on exactly that slice for 57 MB, and the failure mode is
flat rather than wrong.

Do not let that outcome arrive by attrition. If two more prompt variations and one larger
model do not move the Explicit sub-score, say so plainly and put the question to the
product, rather than continuing to tune.
