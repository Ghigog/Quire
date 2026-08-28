# ADR-0004 — Intercepting Read Aloud via a system `TextToSpeechService` is viable

**Status:** Accepted (provisional — see *What is still unverified*)
**Date:** 2026-08-27
**Ticket:** QUI-020
**Deciders:** dylangrowcoot (manual verification on device), claude-opus-5

## Context

PRD v1.2 rests entirely on one assumption: that a third-party Android
`TextToSpeechService` can be selected on the reference device, and that Onyx NeoReader
will route its native Read Aloud text through it. If that assumption fails, Quire has no
V1 — the standalone reader currently scheduled for V3.0 becomes the only path, and the
roadmap inverts.

This was the cheapest experiment that could invalidate the architecture, so it ran before
the model bake-off (QUI-017).

## Decision

**Proceed with PRD v1.2 as specified.** Interception works on the reference device.

## Evidence

Manual verification on an Onyx Boox Note Air5 C:

1. The system text-to-speech settings expose a preferred-engine picker. It initially
   listed only "Speech Recognition and Synthesis from Google", because that was the only
   engine installed — not because third-party engines are excluded.
2. Installing a second engine (eSpeak NG, chosen because its output is unmistakably
   synthetic) made it appear in that picker and selectable.
3. Opening a book in NeoReader, tapping the centre of the screen and using the headphone
   icon started Read Aloud.
4. **Switching the system engine changed the voice NeoReader produced.** Both eSpeak NG
   and Google TTS were heard through NeoReader's own Read Aloud control.

Point 4 is the whole finding. NeoReader does not bypass the Android TTS API with a
proprietary engine, so a service Quire registers will receive the book's text.

## Alternatives considered

- **Accessibility-service screen scraping.** Works against readers that do not use system
  TTS, but needs intrusive permissions, is fragile against layout changes, and carries
  Play Store policy risk. Retained only as the Tier 2 approach for walled gardens
  (PRD §3), not as the primary mechanism.
- **A standalone reader (the v1.1 design).** Total control, but requires users to abandon
  the reader they already use — the product problem v1.2 exists to solve. Retained as
  V3.0.
- **A NeoReader plugin.** No public plugin API exists.

## Consequences

- QUI-021 through QUI-026 are unblocked and the v1.2 module split stands.
- `docs/architecture.md` §3's matcher design is worth building: text will arrive.
- Prior art is confirmed relevant. `mateogon/boox-supertonic-tts` ships a third-party TTS
  engine for NeoReader on sherpa-onnx, reports `rangeStart` reaching NeoReader's
  highlighting, and documents an engine-rebinding quirk (close and reopen the reader's TTS
  session after switching engines). Read before QUI-020's logging spike; check its licence
  before borrowing anything.

## Observed behaviour (manual, 2026-08-27)

Four of the six questions answered by ear and eye on the device:

| Question | Answer |
| --- | --- |
| Does NeoReader consume `rangeStart`? | **Yes — it underlines the spoken word**, with both eSpeak NG and Google TTS. Word-granularity read-along survives in V1. |
| Is the text clean? | Chapter headings are spoken. Acceptable: a narrator reading a heading is correct behaviour. |
| Page turns | NeoReader turns the page and continues. No re-read of the last sentence was observed. |
| Rate and pitch | Pass through from NeoReader's own controls. |

### The chunking finding

The most consequential observation: **NeoReader segments by terminal punctuation, not by
document structure.** A heading with no full stop runs straight into the paragraph beneath
it and is spoken as a single unit. Otherwise it reads all text on screen, pausing at
punctuation.

Three design consequences, all of which change tickets:

1. **The match unit is the sentence, not the paragraph** (QUI-021). Chunks arrive
   punctuation-delimited, so the index must be addressable at that granularity or nothing
   will line up.
2. **A chunk may merge across structural boundaries.** A heading and the first sentence of
   the following paragraph can arrive as one string. The matcher (QUI-022) must therefore
   match a chunk against a *contiguous run* of index entries, not against one entry, and a
   run may cross a boundary the EPUB considered structural.
3. **The voice unit stays finer than the match unit.** `"I know," said Sarah.` is one
   sentence containing a dialogue span and a narration span, needing two voices. So an
   index entry is a sentence carrying an ordered list of voiced spans — sentences for
   matching, spans for voicing.

## What is still unverified

This ADR is provisional because four of QUI-020's six questions remain open. They need the
logging spike, or careful observation on device:

1. **Exact chunk sizes in characters**, and whether NeoReader ever splits mid-sentence at
   the 4000-character API limit. Inferred to be punctuation-delimited from listening;
   needs the logging spike to confirm and to bound.
2. **`onStop` frequency** — whether it fires per page turn or only on stop. Decides how
   cheap cancellation has to be.
3. **Footnote markers, page numbers and hyphenation** in the stream. Headings are known to
   arrive; the rest was not specifically listened for.

Revisit this ADR if any of these breaks the design, or if a firmware update changes
NeoReader's TTS routing.
