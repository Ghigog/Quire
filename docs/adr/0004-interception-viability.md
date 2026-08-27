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

## What is still unverified

This ADR is provisional because four of QUI-020's six questions remain open. They need the
logging spike, or careful observation on device:

1. **Chunk size and alignment** — sentence, paragraph or page? `architecture.md` §9.1 and
   the matcher's forward window (QUI-022) are designed around an assumption here.
2. **Text cleanliness** — do chapter headings, page numbers, footnote markers or
   hyphenation arrive in the stream? Decides how aggressive normalisation must be
   (QUI-021).
3. **`rangeStart` consumption** — does NeoReader highlight in response? Decides whether
   read-along survives in V1 or waits for V3.0.
4. **`onStop` frequency and page-turn behaviour** — decides how cheap cancellation has to
   be, and whether the cursor survives a page turn.

Revisit this ADR if any of the four turns out to break the design, or if a firmware update
changes NeoReader's TTS routing.
