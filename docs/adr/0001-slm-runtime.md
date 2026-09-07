# ADR-0001 — On-device SLM runtime

**Status:** Proposed — blocked on device measurement, not yet Accepted
**Date:** 2026-09-06
**Ticket:** QUI-006
**Deciders:** dylangrowcoot, slm-runtime-interface

## Context

ADR-0006 settled *that* a generative model is needed for the three quarters of dialogue
Tier 1 never answers, and made it the only candidate left: QUI-028 measured both published
attribution encoders against Tier 1 on one novel and both lose on wrong-voice rate at every
threshold (`docs/handoff/2026-09-02-voice-and-attribution.md` §1). This ADR is therefore not
asking *whether* to run a local model — that is decided — only *how*: which inference
backend, and what interface sits in front of it so callers (QUI-007's scan, QUI-009's Tier
2/3 attribution) never see the choice.

`device-profile.md` §2 already narrows the field before any measurement: the 750G has no
i8mm, so quantized matmul takes the slow path, and prompt-eval throughput — not generation
throughput — is the number that decides whether scene-sized prompts (ADR-0006 §3) fit
QUI-007's 30-minute budget.

## Candidates

PRD §3.1 and §6 name two: `llama.cpp` via JNI, and ExecuTorch. Both run a GGUF- or
`.pte`-format quantized model resident in the app process.

| | `llama.cpp` (JNI) | ExecuTorch |
| --- | --- | --- |
| Quantization | Q4_K_M GGUF, widely published | needs its own export pipeline per model |
| Integration | JNI bindings around a C library | Android AAR, native PyTorch runtime |
| Model availability | most community 1B/1.5B checkpoints ship a GGUF already | fewer prequantized `.pte` files; usually exported by hand |
| KV-cache reuse across a chapter (ADR-0006 §3) | supported (`llama_kv_cache` API) | supported, less documented for this use case |

Neither has been run on the Note Air5 C. Nothing below decides between them — that is
exactly the RSS and tokens/s measurement this ADR is missing.

## Decision — the interface, decided now

The choice of backend does not need to be made before the interface it sits behind does,
and QUI-007 and QUI-009 can be built against that interface today (CLAUDE.md §2.3: land the
seam, then fan out). So this ADR fixes the shape now and defers the backend pick to the
measurement in "What is still needed" below.

### `SlmRuntime` — one method, pure JVM

```kotlin
fun interface SlmRuntime {
    fun complete(prompt: String, maxTokens: Int, cancellation: CancellationSignal): String
}
```

`core/attribution/slm/SlmRuntime.kt`. One method because that is all a backend has to
supply — a token budget (`maxTokens`) and a cancellation signal, per the ticket. Loading,
lifetime, and staying clear of an idle TTS session's memory (PRD §5) are the caller's
concern once an Android service exists to own them; this module knows nothing about
Android, by the same rule that keeps `core:attribution` and `core:voice` testable on a
desktop JVM (CLAUDE.md §9).

### Cancellation is a polled flag, not a coroutine `Job`

`CancellationSignal` is an `AtomicBoolean` wrapper, not `kotlinx.coroutines.Job`. The
reason is JNI, not taste: there is no JVM thread interrupt that reaches into a native
`llama.cpp` generation loop uninvited. A backend has to poll a flag between generated
tokens regardless of what concurrency primitive the JVM side prefers, so the interface asks
for exactly that and nothing more. `BackgroundSlmExecutor` runs a completion on a dedicated
worker thread and gives a `Handle` whose `cancel()` sets the flag without blocking —
demonstrating the contract on a fake backend that ticks and checks the signal every 20 ms,
and stopping within it (`BackgroundSlmExecutorTest`, well inside the 500 ms acceptance
criterion, on a fake — see "What is still needed").

### Structured output: validate, retry once, fail explicitly

```kotlin
class StructuredCompletion(private val runtime: SlmRuntime) {
    fun <T> complete(prompt: String, shape: JsonShape<T>, maxTokens: Int, cancellation: CancellationSignal): StructuredResult<T>
}
```

`JsonShape<T>` is a caller-supplied parser (`fun interface { fun parse(raw: String): T }`),
not a dependency on a particular JSON library. QUI-007's manifest shape and QUI-009's
per-scene speaker list (ADR-0006 §3) are different shapes owned by different tickets; this
module only owns the retry-and-report policy around whichever parser they bring.
`StructuredResult<T>` is `Success(T)` or `Failure(reason)` — a caller never receives raw
text to parse defensively, per the ticket. Exactly one retry: the first malformed reply is
shown back to the model with what was wrong, per ADR-0006's warning that blast radius grows
with the size of what one call resolves — a scene, not a line — so a call that fails
outright rather than silently guessing matters more here than it would per-line.

Tested against a fake `SlmRuntime` (`StructuredCompletionTest`): a valid first reply is
parsed without a retry; one malformed reply retries once and then succeeds; two malformed
replies report `Failure` and the runtime was called exactly twice, never three times. This
is the same "put the logic where it can be tested" move as `spike/slice` (CLAUDE.md §9) —
the retry policy is real business logic and needs no device or model to verify.

### What deliberately is not decided here

- **Which backend.** See above.
- **The prompt shapes.** Out of scope per the ticket — QUI-007 and QUI-009 own theirs.
- **Loading, release under memory pressure, and co-residency with TTS.** These need a real
  backend and a real device; QUI-031 measures co-residency specifically.

## What is still needed before this ADR can be Accepted

**RSS and tokens/s, both backends, on the Note Air5 C.** Neither has been measured — this
session has no device. Per `device-profile.md` §2, the number that actually decides
QUI-007's budget is **prompt-eval tokens/s**, not generation tokens/s: a scene-sized prompt
(ADR-0006 §3) spends most of its cost on the prompt, and generation is constrained to a
short structured reply. Measuring generation speed alone would flatter the wrong half of
the cost, the same mistake ADR-0006 §3 already flagged for per-line attribution.

Concretely, QUI-031 (already on the board, depends on QUI-006) needs to run, on-device, for
each backend:
1. Load time and peak RSS with a scene-sized prompt (ADR-0006's ~60–120 scenes/novel,
   projected not measured — handoff §2) resident alongside an idle Piper session
   (ADR-0002: 314 MB peak RSS), against the combined 1.2 GB budget.
2. Prompt-eval tokens/s on a real scene-length prompt, and the KV-cache reuse factor across
   a chapter (`device-profile.md` §2).
3. Generation tokens/s constrained to a single-token-class answer (a speaker index), which
   should cost close to nothing once (1) and (2) are paid.

Until those four numbers exist for both candidates, this ADR names candidates and commits
to an interface; it does not commit to a backend. The **Status** line reflects that: this
is `Proposed`, not `Accepted`, and stays that way until QUI-031 records the numbers here.

## Consequences

- QUI-007 and QUI-009 can be built now against `SlmRuntime`/`StructuredCompletion`, in
  parallel with QUI-031's bake-off, using a fake `SlmRuntime` in their own tests exactly as
  this ticket's tests do — no real backend is needed to build or test either caller.
- `tools/fetch-models.sh` fetches one concrete Q4_K_M GGUF candidate (Llama 3.2 1B
  Instruct) so QUI-031 has something to load without a separate step. It does not fetch an
  ExecuTorch candidate — that needs its own export pipeline and is not built here.
- Whichever backend QUI-031 measures faster/lighter gets its JNI or ExecuTorch adapter
  written as an `SlmRuntime` implementation in an Android module; no caller code changes,
  because both would implement the same one-method interface.
