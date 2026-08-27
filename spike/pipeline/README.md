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

**Precision is the property to protect.** A confidently wrong voice is worse for a
listener than the narrator taking a line, so every rule here declines rather than guesses,
and unresolved spans carry the reason they were unresolved — `pronoun speech tag` is a
much better input to Tier 2 than a blank.
