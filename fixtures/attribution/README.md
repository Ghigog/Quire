# Attribution fixtures

Labelled dialogue used by QUI-008, QUI-009 and QUI-018's `score` command.

**Format.** One paragraph per line: `GOLD<TAB>text`. `GOLD` lists the correct speaker of
each quoted span in that paragraph, in order, separated by `|`. A paragraph with no
speech is labelled `NARRATION`. `UNKNOWN` marks a span a careful human reader cannot
attribute either — those are excluded from scoring rather than counted as failures.
Lines starting with `#` are comments.

**Provenance.** These three passages were written for this repository rather than taken
from real books: the container this was built in has no network access to Project
Gutenberg. They are shaped to cover the cases the tiers must handle, which makes them a
decent regression suite and a *weak* accuracy estimate — hand-written prose is tidier
than the real thing.

**Before trusting an accuracy number from these, add real chapters.** Three chapters from
different public-domain novels, hand-labelled, is what QUI-018's acceptance criteria
actually ask for. Until then, treat the scores as a floor on difficulty, not a measure of
production accuracy.
