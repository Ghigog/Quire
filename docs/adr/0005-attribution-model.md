# ADR-0005 — A published attribution encoder does not replace Tier 1

**Status:** Accepted, 2026-09-07 — for the encoder question only. The SLM half of QUI-028
is not answered here and is not answered anywhere yet.
**Date:** 2026-09-07
**Ticket:** QUI-028
**Deciders:** dylangrowcoot, local-model-voice-accents

## Context

QUI-028 exists because of a number in `docs/prior-art.md` §3: an encoder built for quotation
attribution reporting **94.5% on PDNC**, against BookNLP's ~63%, at a fraction of an LLM's
cost. If that transferred, it would remove this project's worst constraint — `device-profile.md`
§2 puts a quantized 1B SLM on a Snapdragon 750G in the *hours* for a novel, against QUI-007's
30-minute budget.

Two candidates were measurable once `huggingface.co` reached the build container:

| | parameters | on disk |
| --- | ---: | ---: |
| `compnet-renard/spanbert-base-cased-literary-speaker-attribution` | 107.7M | 431 MB fp32 |
| BookNLP `speaker_google_bert_uncased_L-8_H-256_A-4` (`small`) | 14M | 57 MB |

Both were scored by `spike/pipeline`'s bake-off harness — **one scorer for every candidate**,
so what is compared is models and not two scoring codebases disagreeing about what counts as
a match. Mentions were found by matching PDNC's alias lists against the text rather than
using gold offsets, because a device will not have gold offsets either.

## The metric this decision turns on

Not accuracy. **PRD §3.1 is explicit that a missing voice is flat and a wrong voice is
heard**, so the figure that decides is `coverage × (1 − precision)`: the share of *all*
quotations read in somebody else's voice. A candidate can double accuracy and still be worse
for a reader, and both candidates here do exactly that.

## Decision

**Neither encoder replaces Tier 1.** Both are rejected for the untagged bulk of dialogue,
which is what this ticket was for.

**BookNLP `small` is adopted for the Explicit slice**, where it beats Tier 1 outright, at
57 MB. This is a real but narrow win and it is optional — it can be deferred without
blocking anything.

**The untagged three quarters goes to a scene-level generative model** (ADR-0006, QUI-009),
which is now the only remaining candidate rather than one of two.

## Evidence

### 1. Both encoders lose on the metric that decides

Eight novels, 12,521 quotations — four headline, four of PDNC's own out-of-domain holdouts:

| PDNC headline, 7,656 quotations | coverage | precision | accuracy | **wrong voice** |
| --- | ---: | ---: | ---: | ---: |
| Tier 1 | 30.5% | 89.2% | 27.2% | **3.3%** |
| BookNLP `small` | 93.5% | 57.2% | 53.4% | 40.0% |

The 431 MB SpanBERT encoder was measured on one novel before being ruled out on size and
speed: 88.0% coverage at 52.9% precision, a 41.4% wrong-voice rate. Same shape.

> **Note, 2026-09-10 (QUI-028).** Every precision figure in this ADR is measured **low**, on
> both sides. The scorer compared speaker strings and never read PDNC's alias table, so a
> candidate naming Charlotte Lucas "Miss Lucas" — the novel's own name for her — was counted
> wrong. Folding aliases is worth about 8 points of precision to Tier 1 (89.2% → 93-ish on
> this split; 93.2% over the whole corpus). BookNLP was not re-scored, and it predicts its
> own names, so it would gain too. **The decision stands**: a 12-point gap in wrong voice
> does not close on 8 points of precision applied to both, and the shape of the tradeoff is
> unchanged. Read the columns as ordering the candidates, not as absolute numbers.

**Doubling accuracy costs a twelvefold increase in lines spoken by the wrong character.**
For a reader that is very likely worse than the flat narrator it replaces.

### 2. No confidence threshold rescues it

The obvious repair is to answer only when confident. Swept over BookNLP `small`'s own
softmax scores, one novel:

| threshold | coverage | precision |
| ---: | ---: | ---: |
| 0.00 | 98.5% | 44.0% |
| 0.80 | 67.5% | 50.0% |
| 0.95 | 44.4% | 55.7% |
| 0.99 | 15.8% | 76.2% |

Giving up 54 points of coverage buys 12 of precision, and **at 0.99 Tier 1 beats it on both
axes at once** (18.4% coverage, 88.6% precision). There is no operating point on this curve
where the model is preferable.

That is a sharper result than a low score would have been: the model is not right-but-unsure
on what it misses, it is **confidently wrong**, so no gate can recover it.

### 3. The model degrades four times as hard on unfamiliar prose

| Held out, 4,865 quotations | coverage | precision | accuracy |
| --- | ---: | ---: | ---: |
| Tier 1 | 24.2% | **95.5%** | 23.1% |
| BookNLP `small` | 84.6% | 41.9% | 35.5% |

Tier 1 loses 4.1 accuracy points out of domain; BookNLP loses 17.9. And the precision columns
move in **opposite directions**: the heuristic gets *more* precise on unfamiliar prose, the
model less.

The mechanism is not mysterious. A rule that fires on `said Geralt` is right wherever that
construction appears and declines everywhere else. A model trained on pre-1934 literary
fiction is confidently wrong on prose it has not seen. **PDNC contains no contemporary genre
fiction at any price** — it stops in 1934 — so the real degradation on a reader's library is
worse than −17.9 and remains unmeasured.

This is the finding with the longest reach. It says something about *approach*, not about
these two checkpoints.

### 4. The 431 MB encoder also fails on size and speed, independently

| | measured | budget |
| --- | --- | --- |
| whole-novel time | ~77 min on a desktop core (0.50 quotations/s) | 30 min, on device |
| on disk | 431 MB fp32 | 450 MB for the whole app, TTS voices included |

Host absolutes do not transfer to the device (`spike/hostbench/README.md`) and here they do
not need to: being 2.5× over the whole scan budget on hardware several times faster than a
750G settles it, and ADR-0002 §8 measured that SoC punishing this class of work about twice
as hard again.

BookNLP `small` at 14M parameters is roughly eight times cheaper and was never ruled out on
these grounds — it is rejected on §1 to §3 alone.

## The one win, and it is worth taking

| Explicit quotations only, 2,520 of them | coverage | precision | accuracy |
| --- | ---: | ---: | ---: |
| Tier 1 | 84.8% | 91.8% | 77.9% |
| BookNLP `small` | 98.8% | 90.0% | **88.9%** |

**Eleven accuracy points at the same precision**, confirmed across four novels. BookNLP finds
speech tags Tier 1's regex misses and matches it for correctness on the ones it does find.

A hybrid — BookNLP on the Explicit slice, silent elsewhere — scores about 29.3% accuracy at
roughly 90% precision and ~3% wrong voice: better than Tier 1 on every axis, for 57 MB. That
is the whole of what this ticket buys, and it does nothing for the untagged three quarters.

## Consequences

- **QUI-009 is no longer a choice between two approaches.** The scene-level generative model
  is the only remaining candidate for untagged dialogue, which raises the stakes on ADR-0006
  and on QUI-038's segmentation.
- **QUI-006's runtime moves onto the critical path** rather than beside it.
- **A 57 MB optional tier is available** for the Explicit slice. Its own ticket, when someone
  wants the eleven points; nothing blocks on it.
- **Out-of-domain degradation becomes a standing acceptance criterion**, not a footnote. Any
  future attribution candidate is scored on held-out books and on the gap, because §3 shows
  the gap can be four times larger for a model than for a rule.
- **The wrong-voice rate replaces accuracy as this project's headline attribution metric.**
  Every table above ranks the candidates differently depending on which column is read, and
  PRD §3.1 already said which one matters.

## What is not established

- **One scorer, one mention-extraction strategy.** Mentions come from alias matching, not
  gold offsets. Nobody has run either model with gold mentions to price how much of the gap
  is ours rather than the model's. It would be an afternoon and it would sharpen §1, though
  it cannot plausibly close a twelvefold wrong-voice gap.
- **The SpanBERT encoder's numbers are one novel.** It was ruled out on size and speed before
  a wider run was worth the hours.
- **BookNLP `big` was never run.** 438 MB fp32 cannot ship at any quantisation story we have,
  so it would only ever have been an accuracy ceiling to admire.
- **The published 94.5% was never reproduced or explained.** `arxiv.org` is refused at the
  proxy, so the paper behind it could not be read, and no checkpoint matching that claim was
  found on Hugging Face. It remains a number in `prior-art.md` with no artefact behind it,
  and this ADR does not contradict it — it reports what two *available* encoders do.

---

## Amendment, 2026-09-12 — QUI-041: the whole corpus, three checkpoints, and the composite

This ADR was decided on eight novels and two checkpoints, with a scorer that did not fold
aliases. QUI-041 ran **all 36,970 PDNC quotations** against three checkpoints, with alias
folding, both with PDNC's gold character list and with a roster bootstrapped from the prose,
and composed the best of them with Tier 1 as a single measured candidate.

**The decision stands, and it is now decided on the right number.** Three of this ADR's
reasons were wrong and the verdict does not depend on them.

### What was measured

Whole corpus, alias-folded, discovered cast, wrong voice = coverage × (1 − precision):

| no abstention | coverage | precision | accuracy | **wrong voice** |
| --- | ---: | ---: | ---: | ---: |
| Tier 1 | 26.4% | 94.2% | 24.9% | **1.5%** |
| BookNLP `small` (14M) | 93.4% | 53.7% | 50.2% | 43.2% |
| BookNLP `big` (110M) | 94.0% | 54.3% | 51.1% | 43.0% |
| BookNLP+ (110M, `split_2`) | 93.4% | 66.8% | 62.4% | 31.0% |

### Reason 2 was wrong: the threshold is a working dial

"No confidence threshold rescues it" is false of all three checkpoints. Precision rises
monotonically with the cut, whole corpus:

| at P ≥ | BookNLP+ cover / prec / wrong | BookNLP `big` cover / prec / wrong |
| ---: | --- | --- |
| 0.50 | 85.8% / 69.5% / 26.2% | 50.0% / 72.6% / 13.7% |
| 0.75 | 69.6% / 74.6% / 17.7% | 40.0% / 81.2% / 7.5% |
| 0.90 | 60.8% / 77.2% / 13.9% | 37.4% / 83.3% / 6.2% |
| 0.99 | 48.6% / 80.8% / 9.3% | 33.0% / 85.3% / 4.9% |

The earlier sweep was one novel on the 14M checkpoint, where the curve is shallow. On the
110M checkpoints it is not. **Abstention makes an encoder as safe as you ask it to be** — and
it does so by discarding answers, not by correcting them. That distinction is the verdict.

### Reason 4's size argument is answered, not inherited

`booknlp_budget.py` exports the whole scorer — encoder and joint head in one graph — and
measures each phase in its own process:

| BookNLP+, 108.5M parameters | |
| --- | ---: |
| torch checkpoint, ONNX fp32 | 433.9 MB, 431.7 MB |
| **ONNX fp16** | **216.1 MB** |
| peak RSS, fp16 inference only | 931 MB |
| 101k-word novel, 4 x86 threads | 97.6 s (73 ms/window) |

fp16 halves it to 216 MB, which *fits* PRD §5's 450 MB beside a 92 MB voice pack. So "438 MB
cannot ship at any quantisation story we have" is no longer true: fp16 needs no calibration
set and no int8, which is exactly what QUI-040 found to be the trap on this SoC. **Size is
not what rejects the encoder now.** The wall clock and the peak RSS are host numbers and
neither is an SLA (CLAUDE.md §1.6); the footprint is a property of the file and transfers.

### Three things this ADR said were unestablished, now established

- **BookNLP `big` was never run.** It has been. Eight times the parameters of `small`, the
  same training and the same preprocessing, buys **0.9 accuracy points** (50.2% → 51.1%).
  Capacity is not the constraint. BookNLP+ reaches 62.4% at the same 110M, so the whole of
  that gain is training data and method. What capacity does buy is *calibration*: at P ≥ 0.75
  the 110M answers 40.0% at 81.2%, the 14M answers 62.0% at 63.9%.
- **Gold mentions were never priced.** Near enough now, and the answer is the opposite of the
  expected one. A roster discovered in the prose beats PDNC's gold character list on every
  checkpoint — 62.4% against 61.7%, 51.1% against 49.9%, 50.2% against 49.2% — because gold
  alias lists contain names the novel never uses for that character, while a discovered
  roster contains only strings that are in the text. **None of the gap is ours.**
- **The published 94.5% is still not reproduced, and cast discovery is not the explanation.**
  The research reply of 2026-09-11 attributed the drop to gold candidate lists and put the
  real figure at 82–85%. Gold candidate lists are worth −0.7 points, not twelve, and the best
  available encoder reaches **62.4%**. Twenty points below the revised estimate and thirty-two
  below the published one, and nothing measured here accounts for either.

### The composite, measured rather than derived

Every "Tier 1 plus an encoder" figure quoted so far — including this ADR's hybrid, and
QUI-041's earlier per-type arithmetic — was added up by hand from a table split on PDNC's
`quoteType`. That label is gold and a device never has it, so the composite it describes
cannot be built. `CompositeCandidate` routes on the only signal available at run time —
whether Tier 1 answered — and is scored by the same scorer as its halves:

| `tier1+booknlp-plus` | coverage | precision | **wrong voice** | encoder's own precision on the declines |
| ---: | ---: | ---: | ---: | ---: |
| Tier 1 alone | 26.4% | 94.2% | **1.5%** | — |
| at P ≥ 0.75 | 70.5% | 73.7% | 18.5% | 61.4% |
| at P ≥ 0.90 | 62.2% | 76.2% | 14.8% | 62.9% |
| at P ≥ 0.99 | 51.3% | 79.7% | **10.4%** | **64.3%** |

**This is the number that decides, and it is the one nobody had.** On the quotations Tier 1
declines, the best available encoder at its most conservative setting is **64.3% precise**.
Its headline precision at that threshold is 80.8%; the difference is that Tier 1 has already
taken every quotation with a speech tag in it. So the composite buys 25 points of coverage
for a **sevenfold** rise in wrong voice, from 1.5% to 10.4%. One line in ten read by the
wrong character, against one in sixty-seven.

**The one thing that cuts the other way, and it is not small.** QUI-039's verdict (its Worklog,
2026-09-11) is that the *lower*-coverage rendering was judged to contain slightly **more**
mistakes, on the novel where the two settings diverge most. Its reading of that is the sharper
one: a narrator line is cheap only where the listener cannot tell who is speaking, so declining
is not free, merely cheaper. The weight this ADR puts on `coverage × (1 − precision)` has
therefore never been confirmed by ears, and the composite above is rejected on a metric whose
exchange rate is still assumed. What survives that caveat is the mechanism rather than the
arithmetic: at 64.3% precision on the declines, a third of the lines the encoder newly voices
go to someone the page names as somebody else — which is the case point 2 of that verdict
endorsed the narrator for. QUI-044's perceptual screen and a second listen at these operating
points are what would settle the rate.

### What this changes

- **The encoder is not adopted, at any threshold, for untagged dialogue.** Unchanged verdict,
  now measured end to end on the product's own routing rather than derived from a gold label.
- **`small` is no longer the Explicit-slice candidate; BookNLP+ at P ≥ 0.90 is.** Whole
  corpus, Explicit only: Tier 1 is 79.9% coverage at 97.7%; BookNLP+ is 93.2% at 97.4%. That
  is 13 points of coverage for 0.3 of precision, and it raises Explicit wrong voice from 1.8%
  to 2.4%. It is a real win and it is 216 MB for it — its own ticket, and the trade is the
  footprint, not the accuracy.
- **Implicit and Anaphoric remain open and remain 70% of all dialogue.** BookNLP+ is 55.8%
  and 48.3% precise on them with no abstention. Raising the cut removes those answers; it
  does not fix them. ADR-0006's scene-level model still has no measured competitor.
- **Two of this ADR's stated reasons should not be cited again**: the threshold argument
  (reason 2) and the size argument (reason 4). Reasons 1 and 3 carry the decision, and the
  composite table above carries it more directly than either.
