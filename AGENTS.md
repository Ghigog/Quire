# AGENTS.md — how we work on Quire

The operating manual for every agent in this repository, whatever tool it runs. Claude Code
reads it through [`CLAUDE.md`](CLAUDE.md), which is one line — `@AGENTS.md`. Edit this file,
never that one.

**Quire** is a tactile, multi-voice e-reader: EPUBs turned into multi-character audio by
on-device AI, on e-ink Android hardware. [`docs/PRD.md`](docs/PRD.md) is the source of truth
for *what* we build and the SLAs it must hit; [`docs/architecture.md`](docs/architecture.md)
for *how* the pipeline fits together. Read the document before the code it describes.

Section numbers are stable: source comments and ADRs cite `CLAUDE.md §9`, or *§2.3 fan-out
seams*, and mean the sections below.

---

## 1. First principles

1. **Documentation before code** — a change no ticket describes doesn't get written. Claim or
   write the ticket (§4), then implement.
2. **Small, complete units** — one ticket = one branch = one PR. Three unrelated concerns are
   three PRs.
3. **Report honestly** — paste failing output, name anything skipped and say why. Never call
   partial work done.
4. **Do the asked-for scope** — don't widen or narrow it silently. If you think it is wrong,
   say so in a sentence or two, then deliver under a stated assumption.
5. **The device is the constraint** — the PRD §5 SLAs (≤1.2 GB RAM, ≤450 MB footprint,
   RTF ≤0.15, <8%/hr) are only ever true on the reference device, an Onyx Boox Note Air5 C
   ([`docs/device-profile.md`](docs/device-profile.md)). Android only; iOS is out of scope.
6. **Screen on the host, decide on the device** — a *comparison* between two candidates
   usually transfers ([`spike/hostbench`](spike/hostbench) records where it does not); an
   *absolute* host number never does. Never quote one at an SLA.

## 2. Working with multiple agents at once

### 2.1 One agent, one ticket, one branch

- Branch: `claude/<ticket-id>-<kebab-slug>`. A branch handed to you by the web or desktop app
  keeps its name and is not duplicated — note the ticket in your first commit instead.
- **Claim first, on the board *and* in [`tickets.md`](tickets.md):** `Status: In progress`,
  `Owner: <your branch slug>`, pushed before any code. The board is authoritative for a
  ticket's status and owner — a ticket that has already moved or closed there is not yours to
  take, whatever `tickets.md` still says — and `tickets.md` mirrors it beside the ticket body
  and its Worklog. Never a model name — every session runs the same model, so it cannot tell
  two owners apart.
- On finish set `Done` / `In review` / `Blocked` / `Todo` and clear `Owner` to `—` **on the
  board first**, then mirror it in `tickets.md`.
  **`In review` is the honest state** when the deliverable is done but something outside the
  repo must confirm it: a device measurement, a listen, a human read.

### 2.2 Avoiding collisions

- Check the board before starting — its status wins over `tickets.md` — and never edit a file
  outside your ticket's declared file list. Need a change in someone else's area? Open a
  ticket and link it as a dependency.
- `tickets.md`, `AGENTS.md`, dependency manifests and DI/wiring are shared and high-contention:
  append at the end, don't reflow or reorder.
- Rebase before pushing (`git fetch origin main && git rebase origin/main`); never force-push a
  branch you did not create.

### 2.3 Parallelise along interface seams, not files

Land the interface (data class, protocol, JSON schema) in a small fast PR, then build against
it in parallel. The frozen seams are
[`docs/schema/characters.schema.json`](docs/schema/characters.schema.json),
`AttributionResult` (`core/model`), `TtsChunk` + `Boundary` (`core/tts`) and `RollingBuffer`
(`core/tts`). Never parallelise two agents on one pipeline stage, or any stage whose upstream
interface is still moving.

### 2.4 Handoff notes

End every session with a dated entry in the ticket's **Worklog**: what landed, what's left,
what surprised you, and the exact command that reproduces your test run. Assume the next agent
has zero memory of your session.

## 3. Where things live

| Where | What is there |
| --- | --- |
| [`tickets.md`](tickets.md) | The backlog and all work in flight |
| [`docs/PRD.md`](docs/PRD.md), [`docs/architecture.md`](docs/architecture.md) | What we build and its SLAs; how the pipeline fits together |
| [`docs/device-profile.md`](docs/device-profile.md) | The reference device and what it forces on the design |
| [`docs/adr/`](docs/adr), [`docs/handoff/`](docs/handoff), [`docs/schema/`](docs/schema) | Decisions and their alternatives; dated handovers; frozen data contracts |
| [`docs/claude-global-memory.md`](docs/claude-global-memory.md) | The portable version of §10 |
| `core/model` | `quire.model`: `Tier`, `AttributionResult` (`Attribution.kt`); `IndexEntry`, `VoiceSpan`, `Thresholds` (`Index.kt`); the character manifest (`characters/`) |
| `core/index` | `quire.index`: `IndexWriter`/`SqliteBookIndex` over the `BookIndex` and `Sql` ports; `Matcher`, `Normalizer`, `OffsetMap`, `identify/BookIdentifier` |
| `core/attribution` | `quire.attribution`: `Heuristic` (Tier 1), `Roster`, `Conversation`; `scenes/SceneSegmenter`, `scan/BookScan`; `slm/` — the `SlmRuntime` port, `StructuredCompletion`, `SceneAttributor` |
| `core/epub` | `quire.epub`: `EpubText` — EPUB in, paragraphs out |
| `core/voice` | `quire.voice`: `design/VoiceDesigner`, `foundry/Foundry` + `SpeakerProfile` |
| `core/tts` | `quire.tts`: `casting/Caster` + `CastStore`; `engine/TtsEngine`, `TtsChunk`, `RawSynthesizer` and its cloud/fallback wrappers; `buffer/RollingBuffer`; `sentence/SentenceCache` + `sliceChunk` |
| `desktop` | `quire.desktop`: `Cli`/`main`, `BookReader` (import → cast → attribute → read), `RenderPlan`, `SherpaEngine`, `Voices` — see [`desktop/README.md`](desktop/README.md) |
| `app/` | Shipped Android, in **its own build tree** (`includeBuild("..")`): `app:companion` (`pipeline/ImportPipeline`, `IndexPublisher`) and `app:ttsservice` (`synthesis/UtteranceSynthesizer`) |
| `spike/` | Harnesses, never shipped and never depended on: `indexer/` (a labelled fixture → a matching EPUB and index), `slice/`, `pipeline/`, `hostbench/`, `ttsbinding/` |
| `fixtures/` | Labelled test data; `attribution/*.tsv` are the attribution golds |
| `tools/` | Fetch and build scripts for artefacts git does not hold |

The ticket that adds a top-level directory adds its line to this table in the same PR.

## 4. Tickets

Every ticket, without exception, has these five sections in this order: **User story**
(`As a <role>, I want <capability>, so that <benefit>.`), **Context (why)**, **Description
(what)**, **Requirements (how)** — files and modules to touch, data shapes, budgets,
out-of-scope notes — and **Acceptance criteria (Gherkin)**, each scenario mechanically
checkable by a test, a measurement, or an explicit manual procedure.

IDs are `QUI-###`, allocated sequentially and never reused. Statuses are `Todo` →
`In progress` → `In review` → `Done`, plus `Blocked`, which must name the blocking ticket.

A ticket's **status and owner live on the board**, which is authoritative for both; the
`tickets.md` header mirrors them so the bodies and their history read correctly on their own.
Where the two disagree, a ticket is at the board's status and that is the one you act on —
never start or continue one that has moved or closed on the board. The ticket's story,
requirements, acceptance criteria and Worklog stay in `tickets.md`.

## 5. Definition of done

A ticket is `Done` only when: every Gherkin scenario passes, with an automated test where one
is possible; the relevant PRD §5 SLA was **measured**, not assumed, and the number is in the
Worklog; the docs moved in the same PR ([`docs/architecture.md`](docs/architecture.md) for a
structural change, an ADR for a decision with alternatives, this file for a process change);
no new files outside the ticket's declared list; and the PR body links the ticket ID and lists
what was verified, and how.

## 6. Code conventions

- **Style:** match the file you are in; don't reformat lines you didn't change.
- **Comments:** explain *why*, not *what*; match the surrounding density.
- **Commits:** `QUI-###: imperative summary under 72 chars`, body explains why.
- **Tests:** a bug fix lands with a regression test that fails before the fix.
- **Dependencies:** each new one needs a line in the ticket justifying its size against the
  450 MB budget. Model files and binaries are fetched by a script in [`tools/`](tools), never
  committed.
- **No dead code:** no commented-out blocks, no speculative abstractions "for later".

## 7. E-ink rules of thumb

- Pure `#000000` / `#FFFFFF` only where monochrome mode applies; no greys that dither.
- The panel is colour (Kaleido 3), but colour halves resolution to 150 ppi and cuts contrast.
  Nothing on the reading surface uses it, and colour never carries meaning alone.
- No animation, cross-fades or shimmer loaders; state changes are instant. Batch view
  mutations into one frame to avoid partial-refresh ghosting.
- Hardware page-turn and volume keys are first-class inputs, not accessibility extras.
- Any UI a ticket adds is checked in monochrome mode before that ticket is `Done`.

## 8. Guardrails

- Never commit model weights, audio caches, book files, or anything from a reader's library.
- Book text and generated audio stay on-device. No telemetry containing book text, ever — a
  hard product rule, not a preference.
- No cloud services without their own ticket and ADR; cloud TTS is V2 scope (PRD §6), and
  ADR-0010 governs what exists today.
- Ask before deleting files you didn't create, changing branch protection or CI config, or
  bumping a major dependency version.

## 9. What the build environment actually has

An ephemeral container, not the machine with the device attached.

- JDK 21, Gradle 8.14, Python 3.11 with pip, HTTPS to GitHub release assets and PyPI.
- **`./gradlew test` is the fast check** — the whole JVM suite in seconds, no device or SDK —
  and CI also runs `./gradlew checkModuleBoundaries`. Neither compiles the Android code:
  `app/` and `spike/ttsbinding` are separate build trees, so green there says nothing about
  them.
- **The SDK is not in the image.** [`tools/install-android-sdk.sh`](tools/install-android-sdk.sh)
  fetches it, then `cd spike/ttsbinding && ../../gradlew assembleDebug`. That needs
  `dl.google.com` reachable — the Android Gradle Plugin is published nowhere else, and the
  build dies before reaching our code. Test the host with a real path, not the bare domain:
  `curl -sS -o /dev/null -w '%{http_code}\n' -r 0-100 https://dl.google.com/android/repository/repository2-3.xml`
  (206 means available). If it is blocked, push instead of giving up:
  [`.github/workflows/ci.yml`](.github/workflows/ci.yml) builds the probe and the app on a
  runner and attaches their APKs to the run.
- Hugging Face needs `HF_HUB_DISABLE_XET=1` — without it large files hang rather than fail.
  BookNLP's host is https-only. Project Gutenberg is blocked; the TTS models and the sherpa
  AAR come from GitHub release assets.
- **Put the logic where it can be tested** — casting, matching, normalisation and span
  clipping are pure Kotlin with tests; Android classes stay thin glue: a driver, an asset
  copy, a service callback.
- **Never copy `core:` sources into a spike** — the spikes reach the real modules through
  `includeBuild("../..")`; a slightly different normalisation matches nothing and reads as a
  matcher bug for a day.
- **Nothing large is committed** — weights, the AAR and generated indexes come from
  [`tools/`](tools), and a book for a spike is generated by `spike/indexer` from a labelled
  fixture rather than committed.

## 10. Working economically

Every tool call re-sends the whole conversation, so context added early is paid for on every
later turn — and `tickets.md`, this file and the ADRs are all long. Before doing something
materially more expensive than the alternative, say so in one sentence, name the substitute,
then do whatever is chosen: advise, don't refuse, and don't repeat the advice. Read the slice,
not the file (`grep -n` for the heading, then `sed -n` for its range); batch independent tool
calls into one turn; prefer inline work over a subagent that starts cold.

The portable version, for `~/.claude/CLAUDE.md` and new projects, is
[`docs/claude-global-memory.md`](docs/claude-global-memory.md).
