# ADR-0008 — Book analysis runs on-device

**Status:** Accepted, 2026-09-02
**Date:** 2026-09-02
**Ticket:** QUI-034 (what prompted it), QUI-031 (what it commits us to)
**Deciders:** dylangrowcoot, session-visibility-check

## Context

Deterministic attribution has a ceiling and we have now measured where it is. QUI-034 scored
cast discovery against PDNC's 28 novels: after fixes, 84.8% precision and 82.3% recall on
the cast, but gender inferred for only **58.7%** of it. QUI-028 put Tier 1 attribution at
58.5% precision, and PDNC says only 30.1% of quotations carry a named speech tag at all.

> **Correction, 2026-09-02 (QUI-028).** Both measurements above shared a bug in
> `Pdnc.matches`, which stripped punctuation from the gold name but not from ours, so
> `Mr. Woodhouse` never matched `Mr. Woodhouse`. Re-measured: cast discovery is **96.6%
> precision and 88.4% recall** over the 28 novels, with gender inferred for **52.6%** of the
> cast — that share fell rather than rose because the real-character count went from 651 to
> 742 of the same 768 found, and the 91 characters the fix recovers mostly have no gender. Tier 1 attribution is **84.9% precision at 26.8%
> coverage**. **This ADR's decision is unchanged**: the ceiling is still a ceiling, gender
> is still missing for half the cast, and Tier 1 still leaves three quarters of dialogue
> unanswered. The errors are still not regex bugs.

The remaining errors are not regex bugs. They need something that understands context —
which is a language model. The question this ADR settles is *where that model runs*, because
CLAUDE.md §8 states a hard product rule: **book content and generated audio stay on-device,
no telemetry containing text from a user's books, ever.** Sending a book to an API breaks
it, so the alternative had to be considered explicitly rather than drifted into.

## Alternatives considered

**1. Cloud LLM at import.** Costed honestly, because "too expensive" would have been a lazy
rejection. A 100k-word novel is roughly 130k tokens and current models take a 1M-token
context, so the *entire book* fits in a single call. Cast discovery lands around $0.26–1.00
per book depending on model; full per-line attribution, chunked by chapter with the cast
cached in the prompt, around $1–3. Once per book, forever. Quality would be the best
available by a wide margin.

Rejected on the §8 rule, and on two things that outlast the prototype: it needs network at
import, and a shipped version means either every reader supplies an API key (friction) or
Quire operates a service that sees people's libraries and must keep a promise about them.

**2. A free cloud tier.** Rejected, and worth writing down because it is the intuitive
escape hatch. Free tiers are generally free *because* inputs become training data. Measured
against §8 that is strictly worse than paying, not better — it converts "we sent your book
to a vendor" into "we contributed your book to a training corpus". It also makes the product
depend on someone else's free tier continuing to exist.

**3. Heuristics only.** The status quo. Rejected as a ceiling, not as a component — it
remains the offline fallback and the bootstrap for option 4 (see Consequences).

**4. An on-device small model.** Chosen.

## Decision

**All analysis of a reader's book runs on the device.** No book text leaves it, at import or
at any other time. This is the §8 rule restated as an architectural commitment rather than a
guardrail, and it makes QUI-031's SLM runtime bake-off load-bearing rather than exploratory.

## What made this affordable, and it is a reframing rather than a concession

The first framing of option 4 was "run a small model over the book", which is why it looked
prohibitive — `device-profile.md` warns of hours. That framing was wrong. The work splits
into three jobs with very different sizes:

| | Job | Volume per novel |
| --- | --- | --- |
| **A** | Who is in this book? | one pass |
| **B** | Who says line 4,412? | ~3,000 unresolved quotations |
| **C** | What does each character sound like? | ~10 judgements |

Only **B** is large. **A** and **C** are small enough to be uncontroversial on-device: for C
you do not need every one of a character's lines, only a handful you are confident about,
and explicit speech tags — the 30.1% the regex is genuinely good at — supply them. **C does
not depend on B being solved**, which is what makes the whole thing tractable.

For **B**, the budget is QUI-007's 30 minutes for a 100k-word novel. One model call per line
is ~2 decisions/second on a Snapdragon 750G and is not going to happen. **One call per
scene**, with the cast in context and a list of speakers returned, amortises nearly all of
the prefill — and it is also the better answer for quality, because a model that sees a
whole scene resolves turn-taking from context instead of guessing line by line. Per-line
calls would be both slower and worse. This is QUI-031's real target and ADR-0003's input.

## Consequences

- **QUI-031 is now on the critical path.** Which SLM, how fast on a 750G, and whether it can
  be resident alongside Piper inside the 1.2 GB budget.
- **Heuristics stay.** They are the offline fallback, and they are what feeds the model:
  they find the cast and the confidently-tagged lines, the model judges. `core:attribution`
  is not throwaway.
- **Import gets slower and stays honest about it.** The product already promises "the app
  tells you when it is done"; import is a background job, not an interactive one, which is
  what makes a multi-minute scan acceptable where it would not be mid-page.
- **No network permission is needed for analysis.** Model downloads already have one; book
  content never uses it.
- **If the device cannot do it, we say so.** The failure mode is to fall back to heuristics
  and narrator voice (QUI-029's shape), not to quietly ship the book to a server.
