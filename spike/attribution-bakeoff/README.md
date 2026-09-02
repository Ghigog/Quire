# QUI-028 — attribution bake-off harness

Scores a quotation-attribution candidate against **PDNC**, splits the result by quotation
type, and reports three out-of-domain holdouts apart from the headline figure.

Host-side only, and deliberately so: comparing two candidates is a question a build machine
can answer in seconds, while what each costs in RAM, watts and wall-clock is a question only
the Note Air5 C can answer. Screen here, decide there, and never quote a host number at an
SLA (CLAUDE.md §1).

## Run it

```bash
../../tools/fetch-pdnc.sh                 # clones PDNC to ~/.cache/quire/pdnc, ~40 MB
gradle installDist
B=build/install/quire-attribution-bakeoff/bin/quire-attribution-bakeoff

$B score                                  # Tier 1: headline, holdouts, the gap
$B score --candidate tier1-tags-only      # the same with the softer rules switched off
$B score --per-novel --mistakes           # per book, and a sample of wrong answers
$B holdouts                               # the split and why each novel is in it
$B novels                                 # what PDNC holds, from its own index
gradle test                               # the harness's own tests; no corpus needed
```

`--corpus DIR` overrides the location; so does `$PDNC_HOME`.

## What is here

| File | Does |
| --- | --- |
| `Corpus.kt` | Loads PDNC: novel metadata, paragraphs, and one question per gold quotation |
| `Candidate.kt` | The seam every candidate implements — see the whole novel, answer the questions |
| `Tier1Candidate.kt` | The baseline: `core:attribution` exactly as it ships |
| `Scorer.kt` | Coverage, precision, accuracy, by quotation type |
| `Holdouts.kt` | The out-of-domain split, and what it is honestly evidence of |
| `Report.kt` | The tables the ADR quotes |

**PDNC is not committed and must not be.** It carries no licence — no `LICENSE` file, no
terms in its ReadMe, only a link to its paper (checked 2026-09-02). Clone it, cite the
paper, never redistribute it.

## Three things worth knowing before reading the numbers

**Quotations are located by byte span, not by matching their text.** PDNC gives exact byte
offsets into `novel_text.txt`, and the harness uses them. The first scoring pass (QUI-018)
keyed gold quotations against predicted segments by normalised text, which matched 2,846 of
37,131 and silently dropped the rest — so its 58.5% precision was measured on 7.7% of the
corpus, self-selected for being easy to key. Here the denominator is every scorable
quotation, and one the candidate never saw counts against it. **The two numbers are not
comparable, and this one is the honest one.**

**Coverage, precision and accuracy are three numbers, not one.** A candidate answering a
tenth of the book perfectly and one answering all of it half-right score the same accuracy
and are not the same product. A confidently wrong voice is heard; a missing one is merely
flat (PRD §3.1), so precision is the property to protect and coverage is what a model is
bought to raise.

**The holdouts are proxies and the gap they measure is a lower bound.** The ticket asks for
books unlike PDNC; these are books unlike the *rest of PDNC*, picked off its own index — one
translation, three genre novels, seven first-person narratives. The corpus stops in 1934, so
nothing here says anything about contemporary prose. `Holdouts.External` is the slot that
would, and it is empty: a book we could commit is a book CLAUDE.md §8 forbids.

## Adding a candidate

Implement `Candidate`, register it in `Main.kt`. A candidate is handed the whole novel and a
list of questions — each a paragraph index and a character range — and answers with a
speaker name or `null`. Declining is a real answer and is scored as coverage lost rather
than as a mistake.

The two candidates the ticket still wants are not here. Neither is blocked on this harness:
both need a model file, and this container can reach neither Hugging Face nor the Berkeley
host BookNLP downloads from. The ticket's worklog names the exact files.
