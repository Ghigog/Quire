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
