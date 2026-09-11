# Review — the 2026-09-11 Engineering Handoff Specification

**Date:** 2026-09-11 · **Reviews:** the researchers' *Engineering Handoff Specification:
Quire Platform*, 11 September 2026.
**Reading order:** this file, then [ADR-0005](../adr/0005-attribution-model.md), then
[2026-09-08](2026-09-08-attribution-state-of-play.md).

This is a response, not a rejection. The spec gets the hardware right, gets our int8 finding
right, and its four sprints map almost exactly onto tickets already on the board. What
follows is the delta: where it agrees with measured work, where it restates an unverified
number as settled, and the five questions that have to come back before Sprint 1 can start.

---

## 1. The headline

**The 94.5% encoder figure the spec is built on is not reproduced by the only public
PDNC-trained encoder that reports held-out-novel numbers, and the gap has an obvious
mechanism.**

`bodyanats/booknlp-plus-speaker-attribution` (Apache-2.0, BERT-base, 413 MB fp32 — so the
spec's "110M ≈ 220 MB fp16" sizing is right) publishes a five-fold leave-novels-out
evaluation. Its own `evaluation_results.json`:

| Fold | dev accuracy (novels seen in training) | test accuracy (unseen novels) | gap |
| ---: | ---: | ---: | ---: |
| 0 | 83.9% | 62.3% | +21.6 |
| 1 | 87.1% | 54.3% | +32.8 |
| 2 | 84.1% | 72.5% | +11.6 |
| 3 | 85.8% | 56.9% | +28.9 |
| 4 | 86.6% | 56.8% | +29.8 |
| **mean** | **85.5%** | **60.5%** | **+24.9** |

Same model, same corpus, same code. The only variable is whether the test novels were in
training. That is a ~25-point in-domain premium, consistent across all five folds, and it is
the most likely explanation for a published 94.5%.

At ~full coverage, 60.5% accuracy is **~40% wrong voice** under the metric PRD §3.1 forces on
us. That lands on top of the two encoders we already measured ourselves — SpanBERT 41.4%,
BookNLP `small` 55.1% — against Tier 1's 2.1%. Three independent BERT-class encoders, three
versions of the same answer.

**This does not close QUI-041.** It does mean the ticket's premise is currently unsupported,
and the sprint plan should not assume the wall comes down.

### Why we cannot check the paper ourselves

`arxiv.org` is refused at this environment's proxy (`CONNECT` 403), and
`huggingface.co/api/papers/2608.02359` 404s. A previous session recorded the same dead end in
QUI-028 and in ADR-0005's *What is not established*. We are not able to read the paper, find
the checkpoint, or explain the difference in setup. **See question 1.**

---

## 2. Where the spec is right, and we should say so

- **The int8/Kryo 570 reading is exactly ours.** 2.46× slower than fp32, cause is the missing
  `i8mm` and the dequantize tax per operator pass, and that argues *for* the Hexagon rather
  than against it. QUI-040 already says this in the same words.
- **Attribution at import, never at playback.** Settled here too (ADR-0006, ADR-0008).
- **NNAPI is a sunset API on Android 15; target QNN.** QUI-040's requirement, independently
  reached.
- **Auto-regressive SLMs at 1–3B do not carry this.** QUI-028 measured 37.5% / 41.0%
  precision and 1.9 GB for the 3B. Agreed and closed.
- **An automatic quality screen before a device listen is worth having.** Filed as **QUI-044**
  by the session that landed PR #13, with the same two corrections (§4).

---

## 3. Where the spec would undo measured work

### 3.1 Gender from coreference would cost us coverage, not gain it

The spec says pronominal binding on entity clusters yields >95% gender accuracy,
"eliminating heuristic name-scanning". QUI-035 measured the thing being eliminated,
two days ago, and the result runs the other way:

| | coverage | accuracy | effective |
| --- | ---: | ---: | ---: |
| pronoun evidence only | 52.6% | 91.1% | 71.6% |
| **+ titles decide sex** | **83.7%** | **91.4%** | **84.7%** |

**The title is what fixed it.** `Mr`, `Mrs`, `Sir`, `Lady` settle sex outright and outrank the
pronoun vote — and they settle precisely the case a pronoun graph cannot: `Mr Bennet` and
`Mrs Bennet` draw the same pronouns in the same paragraphs. Removing name-scanning in favour
of coreference would give back 31 points of coverage to chase 4 points of accuracy we have no
measurement for.

A coreference graph is still worth having for *attribution*. It is not a replacement for the
title rule, and the spec should not be read as authorising one.

### 3.2 The import SLA moved by a factor of 30–60 without saying so

The spec states "Max ~30–60s" for the import phase. QUI-007's budget is **30 minutes** for a
100k-word novel on the reference device (relaxed from 10 minutes on 2026-08-27 because
indexing left the real-time path). **See question 3.**

The supporting arithmetic does not hold either: "~200 tokens/sec, completing a 100k-word novel
in under 15 seconds". A 100k-word novel is ~130k tokens; at 200 tokens/sec that is ~11
minutes, not 15 seconds. If the intended unit was quotations per second, ~3,000 quotations at
200/sec is 15 seconds and the claim works — but then it is not a token rate and does not
bound the coreference pass, which reads the whole book.

### 3.3 The QNN guardrail does not guard

```cpp
std::vector<std::string> providers = Ort::GetAvailableProviders();
```

`GetAvailableProviders()` reports the providers **compiled into the ONNX Runtime build**, not
the ones that initialised, and not where the graph's nodes were actually placed. It returns
`QnnExecutionProvider` on a build that has QNN compiled in even when `libQnnHtp.so` failed to
load and every node silently fell back to CPU. That is the precise failure QUI-040 names as
"the most likely wrong answer this ticket can produce", so the assertion as written would let
it through.

What does work: append the EP to the session options and let session construction throw, and
verify node placement from the session's profiling output or ORT logging at
`ORT_LOGGING_LEVEL_VERBOSE`. QUI-040 already requires the resolved provider to be logged per
session and recorded beside every number; that requirement stands and this snippet does not
satisfy it.

### 3.4 There is no Vulkan execution provider in ONNX Runtime

The spec assigns the vocoder to "FP16 execution on the Adreno 619 GPU via Vulkan / OpenCL
Execution Provider". ONNX Runtime does not ship a Vulkan EP, and the OpenCL EP was never
mainlined. The real Adreno paths are QNN's **GPU backend** (`libQnnGpu.so`, OpenCL underneath)
or the **WebGPU EP** over Dawn, which does run on Vulkan on Android. These have different
operator coverage and different fp16 stories, and neither is a drop-in.

We have not confirmed this on the device and QUI-040 exists to do exactly that, so treat it as
a flag rather than a finding — but Sprint 2 should not be scheduled as if turning on a Vulkan
EP is a configuration step. **See question 4.**

---

## 4. The quality screen: adopt, with two corrections

Worth doing, and it is the one directive in the spec that this build container can run today.
Filed as **QUI-044** (see §9 — PR #13 allocated the id).

1. **The model id does not exist.** `sarulab-speech/UTMOS-sinc56-utmos-strong` returns 401
   from the Hub API. What is published: `sarulab-speech/UTMOSv2`, and community mirrors of
   the UTMOS22 strong learner (`vokra/utmos22-strong`, `Blinorot/UTMOS-PyTorch`).
2. **A screen, not a gate.** UTMOS is trained on MOS listening data for short synthesis
   comparison clips. A 3.5 absolute threshold on long-form narration is a host number quoted
   at a product bar, which CLAUDE.md §1.6 forbids and `spike/hostbench/README.md` §43 already
   qualifies: ratios within a tier and sample rate transfer, absolutes do not. Rank candidates
   with it and use it to kill the obviously bad ones before they cost a device hour. Do not
   let it approve anything — QUI-039 exists because the listen found what the numbers could
   not.

---

## 5. The structural point about the hybrid architecture

The cloud-dialogue / local-prose split is a reasonable answer to *synthesis quality*. It is
not an answer to anything this project is currently blocked on, and the spec reads as though
it were.

**Attribution is upstream of both tiers.** If the wrong character is assigned, the cloud tier
renders the wrong voice at studio quality and per-character cost. Sprint 3 does not reduce the
risk Sprint 1 carries; it raises the price of getting Sprint 1 wrong.

Two smaller things, both needing a product decision rather than an engineering one:

- **-24 LUFS** is the ATSC A/85 broadcast target. Audiobook practice is -18 to -23
  (ACX/Audible), streaming is -14 to -16. On a tablet speaker in a room, -24 will read as too
  quiet. **See question 5.**
- **A continuous room-tone bed** is deliberately the opposite of the ≤ -60 dB noise floor
  audiobook distribution asks for. It may still be the right call for masking cloud/local
  transitions, but it is an ADR, not a mixer setting.

QUI-042 already carries the BYOK work and states the constraint the spec leaves out: the
offline tier stays the default and the only tier needing no configuration. The spec's diagram
makes cloud the V1 dialogue path outright. We are treating QUI-042's framing as the one that
holds unless told otherwise.

---

## 6. Questions back to the researchers

Ordered by how much they block.

1. **The checkpoint behind 94.5%.** The Hugging Face repo id or download URL for the
   joint-scoring encoder, plus its parameter count and whether an ONNX export exists. Failing
   that, the PDF — we cannot reach arXiv. **Without this QUI-041 cannot start on its stated
   premise**, and the one public PDNC encoder we can reach says 60.5% on unseen novels.
2. **Was 94.5% measured on held-out novels or a within-novel split?** If within-novel, the
   comparable number for our use case is the ~85.5% dev column above, and the honest
   expectation for a reader's own library is ~60%. If held-out, what differs about the setup?
3. **Is the 30–60s import SLA a product requirement or an estimate?** QUI-007 says 30 minutes.
   If 60s is now binding it rules out the coreference pass as specified, and we would want
   that stated as a change to QUI-007 rather than inherited from a diagram.
4. **On which ONNX Runtime build has QNN + Adreno been run?** Specifically: the ORT version,
   the QNN SDK version, and whether the vocoder ran on `libQnnGpu.so` or something else. If
   this is a design proposal rather than a measurement, say so and QUI-040 will measure it.
5. **Is -24 LUFS deliberate?** If it came from a broadcast reference we would rather target
   -20 LUFS and bring it to product as an ADR with the room-tone decision.

---

## 7. Answered — the researchers' reply, same day

All five came back. Recorded here because the answers change tickets, not just this note.

| # | Question | Answer |
| --- | --- | --- |
| 1 | The checkpoint behind 94.5% | **There isn't one.** No public ONNX checkpoint exists; `deberta-v3-small` was an architecture proposal, not a link. Directed not to search further, and to use BookNLP+ as the baseline encoder |
| 2 | Held-out or within-novel | **Our reading accepted.** 60.5% is what a reader's library gets. **Abstention is now mandatory**: below a confidence threshold the quotation routes to the narrator |
| 3 | Import SLA | **30 minutes confirmed** as binding; the 30–60s figure is withdrawn. Run the encoder over dialogue windows only, not prose blocks |
| 4 | QNN and Adreno | **Our reading accepted.** QNN HTP for the DSP, QNN GPU for Adreno. Verify placement by parsing `ORT_LOGGING_LEVEL_VERBOSE` node assignments or `GetProfilingOutput()`, not `GetAvailableProviders()` |
| 5 | Loudness | **−16 to −18 LUFS.** The room-tone bed is reframed as an active cross-fade at local/cloud transitions, not a continuous noise floor |

The gender note was accepted without argument: titles **and** the coreference graph, not coreference alone. QUI-035's title rule stands.

The framing note was accepted and is now the board's ordering principle:

> **Sprint 1 (abstention-first attribution) is the risk-mitigation gate for Sprint 3 (cloud
> rendering).** A confident wrong voice rendered in the cloud is a paid, highly audible
> disaster, so attribution has to be trustworthy before a paid backend is wired to it.

### The one thing to carry into QUI-041

A fixed softmax threshold is the mechanism being asked for, and **we have already run that
experiment twice with a negative result.** ADR-0005: no confidence threshold recovered
SpanBERT or BookNLP `small` — at 0.99 Tier 1 still won on coverage *and* precision
simultaneously, because the model was confidently wrong rather than unsure.

That is not a reason to skip it here. BookNLP+ is a different checkpoint — cased, BERT-base
rather than `L-8_H-256_A-4`, trained leave-novels-out — and the harness caches per-quotation
scores, so a sweep costs a file read rather than another pass of the model. It *is* a reason
to sweep the whole curve and report it, rather than to measure 0.75 alone and call it done.
If the curve fails the same way a third time, that is the finding, and §8 of the 2026-09-08
handoff says what follows from it.

---

## 8. What this session changed

- This note.
- **QUI-041** — the BookNLP+ measurement and threshold sweep, in its Worklog, plus the two
  Requirements the measurement earned. The ticket's own re-scoping came from PR #13; see §9.
- `spike/pipeline/predictors/booknlp_predict.py` — a `booknlp-plus` flavour, a strict loader,
  and the casing fix the checkpoint needs.
- `tools/fetch-attribution-models.sh` — fetches fold 2 as one 414 MB file.

## 9. Collision with PR #13, and how it was resolved

**Two sessions answered this specification in parallel and both wrote to the board.** PR #13
(`quire-dialogue-attribution`) merged at 14:13 on 2026-09-11 while this branch was open. It is
the same research reply, read independently, and its ticket text is now main's.

Resolved per CLAUDE.md §2.2 — take the other side, re-apply your own on top:

- **`tickets.md` was reset to main's version wholesale.** Every QUI-040/041/042 amendment in
  this branch was dropped in favour of PR #13's, which say the same things and landed first.
- **The QUI-043 in this branch was deleted.** PR #13 had already allocated QUI-043 to a
  modern-prose test set and QUI-044 to MOS screening. Two tickets briefly shared an id; main's
  wins, ids are never reused, and **QUI-044 is the better version** — it adds calibrating the
  metric against Piper `libritts_r`, the one engine a person has actually judged.
- **Only the measurement was re-applied**, as a QUI-041 Worklog entry.

**PR #13 caught something this branch got wrong.** Its ⚠️ on QUI-041 notes that "run inference
strictly on extracted dialogue quotes" must not be implemented literally: the speech tag lives
in the prose *either side* of the quotation, so stripping prose would discard the 99.0% rule.
This branch had written that directive into QUI-041's Requirements as "dialogue windows only,
never prose blocks" — the dangerous reading. Their framing is correct and is what stands.

The board rule that would have prevented this is §2.1: claim the ticket and push the claim
before writing. Both sessions were working from the same inbound document rather than from the
board, so neither claim was visible to the other when the work started.
