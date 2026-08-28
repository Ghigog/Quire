# Host traces

What e-reader apps actually send a system TTS engine, captured by the QUI-020 probe.

**No book text is committed here.** CLAUDE.md §8 forbids committing book files or anything
from a reader's personal library, and a trace is verbatim prose from whatever was being
read. So captures are reduced to two things: the measurements, written up in
[`docs/adr/0004-interception-viability.md`](../../docs/adr/0004-interception-viability.md),
and a **synthetic trace of the same shape** using prose written for this repository.

`neoreader-epub-shape.tsv` reproduces, with substitute text: clause-level splitting at
commas, leading spaces on continuation chunks, headings as their own chunks with trailing
spaces, whole-page batches separated by multi-second gaps, and the `rate`/`pitch` integer
percentages.

If you capture a new trace, add its measurements to the ADR and update the synthetic file
if the *shape* changed. Never commit the raw capture.
