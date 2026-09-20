# ADR-0010 — Bring-your-own-key cloud voices

**Status:** Accepted, 2026-09-20
**Date:** 2026-09-20
**Ticket:** QUI-042
**Deciders:** dylangrowcoot (product authorisation, 2026-09-10 memo), next-ticket-yuhurr

## Context

CLAUDE.md §8 forbids adding cloud services without a ticket and an ADR, and PRD §6 puts
cloud TTS in V2. Product Leadership's 2026-09-10 memo, directive B, reverses that for V1:
**cloud voices for dialogue, the local engine for narration.** Dialogue is ~25% of a book's
word count and carries ~90% of its emotional weight, so the spend goes where it is heard,
and metering a quarter of the book costs a quarter of metering all of it.

ADR-0002 (Piper `libritts_r`, RTF 0.354) and ADR-0009 (generated voices) both stand — this
does not replace the local engine, it gives dialogue a second, optional source of audio.

## What leaves the device, and what does not

**Only the text of the line currently being spoken, plus the reader's chosen voice id,
reach the configured endpoint.** Indexing, attribution, casting and the character manifest
all run on-device already (`BookImport`) and stay there. No book id, no chapter locator, no
reader identifier and no cast data is ever included in a cloud request — the request body
is `{"text": "<line>", "voice_id": "<configured id>"}` and nothing else. There is no
telemetry of what was sent; a reader who wants to audit it can put a proxy in front of their
own endpoint, because the endpoint is theirs.

**The offline tier remains the default and the only tier that needs no configuration.**
Cloud synthesis is off until a reader supplies an endpoint and a key. A reader who never
opens the settings screen this ticket adds sees byte-for-byte the same behaviour as before
it existed.

## What happens to a reader's key

The key is a secret, stored the way Android stores secrets: an `EncryptedSharedPreferences`
file backed by a key the Android Keystore holds, never in a plain preferences file, never in
a log line, a crash report, or anything `adb logcat` can surface. `CloudSynthesizer` (core:tts)
never logs — it has no logging dependency at all — and the one place the key is read into
memory (`CloudVoiceSettingsStore`) passes it straight into an `Authorization` header.

## The provider-shaped contract

One shape — endpoint, key, voice id, audio format — not one integration per vendor. The
memo names ElevenLabs, OpenAI and Sesame CSM; all three, and a reader's own endpoint, are
expected to sit behind a `POST` that accepts `{"text", "voice_id"}` and returns a WAV body.
A vendor whose API differs needs a thin adapter in front of it, not a second synthesizer
inside Quire — `CloudSynthesizer` (`core/tts/.../engine/CloudSynthesizer.kt`) takes the
network call through an injected `HttpTransport`, so the seam stays one class regardless of
how many providers a reader points it at.

This ticket ships WAV/PCM16 responses only. A provider that only speaks MP3 or Opus needs a
decoder this repository does not carry yet — adding one is its own dependency-cost decision,
not bundled into this ADR.

## What the network losing mid-chapter does

`FallbackSynthesizer` wraps the cloud `RawSynthesizer` and the local one behind the same
interface `TtsEngine` already consumes. A cloud call that throws or times out drops to the
local engine for that line and every line after it until `resetFallbackNotice()` is called
at the next chapter boundary; the reader is told once per chapter, not once per line,
because pausing the book is worse than the voice changing under them.

## The acoustic requirements the approval attached

- **Loudness.** `LoudnessNormalizer` brings every buffer, local or cloud, to a configurable
  target (default −17 dBFS RMS) before it reaches the ring buffer, so the cast never sounds
  louder than the narrator. This is an RMS-based approximation of the −16 to −18 LUFS
  mobile-audiobook target, not a full ITU-R BS.1770 loudness meter — K-weighting and gating
  are not implemented. Revisit if a listen finds the approximation audible as a mismatch.
- **Seam cross-fade.** `SeamCrossfader` fades the last `durationMs` of one buffer into the
  first `durationMs` of the next with low-level comfort noise, applied only at a
  local/cloud boundary, not as a continuous floor under the whole book.

## Cost visibility

`CloudCostEstimator` turns a character count and a reader-entered price-per-1,000-characters
into an estimate, shown before the reader enables the tier for a book. Quire does not know
real provider pricing — it has no account with any of them — so the price is what the
reader types in from their own provider's rate card, not a number Quire fetches.

## Alternatives considered

- **Quire-hosted inference or a Quire-supplied key.** Rejected outright by the memo: this is
  bring-your-own-key, not a Quire cloud product. Out of scope, unchanged by this ADR.
- **One integration class per vendor.** Rejected — three near-identical HTTP clients to
  maintain for a feature whose whole value is "point it at your own endpoint."
- **Sending the whole chapter ahead of time for lower per-call overhead.** Rejected: it
  would mean chapter text, not just the current line, leaving the device, which is a
  materially larger privacy surface than the memo authorised.

## Consequences

- A second `RawSynthesizer` exists beside the sherpa-onnx one from ADR-0002, wired through
  the same `TtsEngine` seam QUI-010 landed — no change to that class was needed.
- The settings screen and the encrypted key store are companion-app-only for now; wiring the
  configured backend into `app:ttsservice`'s live session is a small follow-up once QUI-020
  lands the service itself (it does not exist on `main` yet).
- Per-line network latency against the 800 ms TTFS budget, and real-provider behaviour
  (ElevenLabs, OpenAI, Sesame CSM specifically), are unmeasured — this repository holds no
  API key for any of them. See this ticket's Worklog for what a reader must verify by hand
  before relying on the cloud tier for a whole book.
