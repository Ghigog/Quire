# QUI-018 — pipeline spike

A desktop JVM harness for judging **attribution quality** without an Android build or a
device. Code here is explicitly throwaway; the measurements and the fixture set are not.

## Run it

```bash
gradle installDist
B=build/install/quire-pipeline-spike/bin/quire-pipeline-spike

$B score      ../../fixtures/attribution/*.tsv   # coverage / precision / accuracy
$B transcript ../../fixtures/attribution/beats.tsv  # per-span decisions and why
$B roster     ../../fixtures/attribution/beats.tsv  # the model-free roster
$B epub       some-book.epub                     # the same, over a real book
gradle test                                      # includes the QUI-008 throughput SLA
```

## The bake-off (QUI-028)

The same module scores an attribution *candidate* against PDNC, which is what the encoder
versus SLM decision needs. PDNC is not committed and carries no licence — no `LICENSE` file,
no terms in its ReadMe, only a link to its paper. Clone it, cite the paper, never
redistribute it.

```bash
../../tools/fetch-pdnc.sh                 # clones PDNC to ~/.cache/quire/pdnc, ~40 MB
$B bakeoff                                # headline, out-of-domain holdouts, and the gap
$B bakeoff --candidate tier1-tags-only    # the same with the softer rules switched off
$B bakeoff --per-novel --mistakes         # per book, and a sample of wrong answers
$B holdouts                               # the split, and why each novel is in it
$B novels                                 # what PDNC holds, from its own index
```

`Pdnc` loads the corpus and folds names; `bakeoff/Bakeoff` scores; `bakeoff/Candidate` is
the seam a new candidate implements — it sees the whole novel and answers the same questions
every other candidate is asked, so the comparison is of models rather than of scoring code.

**Quotations are located by byte span, not by matching their text.** PDNC gives exact offsets
into `novel_text.txt` and the harness uses them. Until 2026-09-02 the scoring here keyed gold
quotations against predicted segments by normalised text and counted only the ones that keyed
— 2,846 of 37,131, self-selected for being short and cleanly punctuated. Offsets mean the
denominator is the whole corpus and a quotation a candidate cannot answer counts against it.

**The holdouts are proxies and the gap they measure is a lower bound.** QUI-028 asks for books
unlike PDNC; these are books unlike the *rest of* PDNC, picked off its own index — one
translation, three genre novels, seven first-person narratives. The corpus stops in 1934, so
nothing here says anything about contemporary prose. `Holdouts.External` is the empty slot
that would, and filling it needs a decision rather than a script: CLAUDE.md §8 forbids
committing book text.

## What is implemented

Tier 1 only (QUI-008): quote segmentation, a model-free roster bootstrap, explicit speech
tags, action beats, and an addressee guard. Tier 2 and Tier 3 need the runtime that
ADR-0001 has not chosen yet, so they are absent rather than stubbed.

## Two things worth knowing before reading the numbers

**Segments, not paragraphs.** A paragraph regularly holds narration and two speakers, so
the unit that gets *a voice* is finer than the unit that gets *scheduled*. The spike
attributes `Segment`s and the locator gains a `#s{n}` suffix. `docs/architecture.md` §2
says the paragraph is the atom; that is true for scheduling and synthesis, and this is the
exception.

**Coverage, precision and accuracy are three numbers, not one.** A candidate answering a
tenth of the book perfectly and one answering all of it half-right score the same accuracy
and are not the same product.

**Precision is the property to protect.** A confidently wrong voice is worse for a
listener than the narrator taking a line, so every rule here declines rather than guesses,
and unresolved spans carry the reason they were unresolved — `pronoun speech tag` is a
much better input to Tier 2 than a blank.
