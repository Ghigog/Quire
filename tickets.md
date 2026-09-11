# Quire — Tickets

The single source of truth for work in flight. See [`CLAUDE.md`](CLAUDE.md) §2 for how
multiple agents share this file, and §4 for the ticket rules.

**Before you start work:** find your ticket below, set `Status: In progress` and
`Owner: <your agent label>`, and push that change *first*. Never take a ticket that is
already `In progress`.

---

## Board

| ID | Title | Epic | Status | Owner | Depends on |
| --- | --- | --- | --- | --- | --- |
| QUI-020 | TTS service registration and NeoReader binding | Spike | Todo | — | — |
| QUI-017 | TTS engine bake-off on target hardware | Spike | Done | session-visibility-check | — |
| QUI-028 | Encoder vs SLM for quotation attribution | Spike | Todo | — | — |
| QUI-018 | Headless pipeline spike | Spike | Todo | — | — |
| QUI-019 | Vertical slice: NeoReader Read Aloud in three voices | Spike | In review | — | QUI-020, QUI-021, QUI-022, QUI-024 |
| QUI-001 | Project scaffold, build and CI | Foundations | Todo | — | — |
| QUI-021 | Dialogue index schema and store | Index | In review | — | QUI-001 |
| QUI-022 | Text normalisation and cursor matcher | Index | In review | — | QUI-021 |
| QUI-023 | Book identification by fingerprint | Index | In review | — | QUI-021, QUI-022 |
| QUI-027 | Normalised-to-raw offset map | Index | Done | session-visibility-check | QUI-021, QUI-022 |
| QUI-005 | `characters.json` schema and manifest store | Attribution | In review | — | QUI-001 |
| QUI-006 | On-device SLM runtime | Attribution | In review | — | QUI-001, QUI-017 |
| QUI-007 | Upfront book scan → character manifest | Attribution | Todo | — | QUI-005, QUI-006 |
| QUI-008 | Tier 1 heuristic dialogue attribution | Attribution | In review | — | QUI-005, QUI-018 |
| QUI-009 | Tier 2/3 SLM attribution with confidence fallback | Attribution | Todo | — | QUI-006, QUI-008 |
| QUI-010 | ONNX TTS engine with boundary timestamps | Audio | Todo | — | QUI-001, QUI-017 |
| QUI-011 | Automatic voice casting | Audio | Todo | — | QUI-007, QUI-010 |
| QUI-012 | Rolling ring buffer keyed by segment | Audio | Todo | — | QUI-010, QUI-022 |
| QUI-024 | Multi-voice utterance and `rangeStart` callbacks | Audio | Todo | — | QUI-010, QUI-022 |
| QUI-030 | Whole-sentence synthesis with fragment serving | Audio | Todo | — | QUI-012, QUI-027 |
| QUI-025 | Companion app import and indexing flow | Companion | Todo | — | QUI-007, QUI-021 |
| QUI-003 | E-ink display mode and hardware keys | Companion | Todo (reduced) | — | QUI-025 |
| QUI-026 | E-reader compatibility matrix verification | Quality | Todo | — | QUI-019 |
| QUI-016 | Performance and SLA harness | Quality | In review | — | QUI-010 |
| QUI-029 | Unindexed books and non-EPUB formats | Companion | **Deferred → later phase** | — | QUI-025 |
| QUI-002 | EPUB import and Readium reader shell | Reader | **Deferred → V3.0** | — | — |
| QUI-004 | Reading position and progress tracking | Reader | **Deferred → V3.0** | — | — |
| QUI-013 | Playback controls | Reader | **Deferred → V3.0** | — | — |
| QUI-014 | Sentence-level highlighting | Reader | **Deferred → V3.0** | — | QUI-024 covers the host-side part |
| QUI-015 | Character & voice drawer | UI | **Deferred → V2.0** | — | — |
| QUI-031 | SLM runtime bake-off and co-residency | Spike | Todo | — | QUI-006 |
| QUI-032 | Voice descriptor in `characters.json` | Attribution | In review | — | QUI-005 |
| QUI-033 | Accent: listening test and per-character variants | Spike | Done | — | QUI-032 |
| QUI-034 | Cast discovery precision on real books | Spike | In review | session-visibility-check | QUI-008 |
| QUI-035 | Gender coverage for the inferred cast | Spike | In review | — | QUI-034 |
| QUI-036 | Voice foundry: generate a voice, don't pick one | Spike | Done | — | — |
| QUI-037 | Voice foundry: descriptor → generated voice | Audio | In review | voice-generation-foundry | QUI-032, QUI-036 |
| QUI-038 | Scene segmentation for scene-level attribution | Attribution | Done | — | QUI-021 |
| QUI-039 | Listening test: what the wrong-voice rate sounds like | Spike | In review | — | QUI-028, QUI-037 |
| QUI-040 | TTS on the GPU or the DSP, not the CPU | Spike | Todo | — | QUI-017 |
| QUI-041 | Encoder attribution: the 110M joint-scoring model | Attribution | Todo | — | QUI-028 |
| QUI-042 | Bring-your-own-key cloud voices | Audio | Todo | — | QUI-010 |

Next free ID: **QUI-043**

**Milestones** (see [`docs/architecture.md`](docs/architecture.md) §8):
**M0a prove interception** — QUI-020 · **M0b prove the stack** — QUI-017, QUI-018 ·
**M1 vertical slice** — QUI-019 · **M2 MVP** — the rest. Work down this table, not down
the epics.

**Claims released 2026-09-06.** QUI-001, QUI-018 and QUI-020 were `In progress` under
owners whose sessions had ended, and QUI-021 and QUI-022 carried the same stale owner while
already `In review`. Per CLAUDE.md §2.1 an abandoned claim is released by clearing the owner
and setting the status back, so those three are `Todo` again. **That is a release, not a
re-judgement of the work** — each ticket's Worklog says what actually landed, and whoever
picks one up should read it before assuming nothing was done.

**Deferred tickets stay in this file** with their original text and a banner saying why.
They were written against PRD v1.1 and are still broadly right for the version that
revives them; rewriting them now would be guessing.

---

## Ticket template

Copy this block verbatim for every new ticket. All five sections are mandatory.

````markdown
## QUI-XXX — <Title>

**Status:** Todo · **Owner:** — · **Epic:** <epic> · **Depends on:** <ids or —>
**PRD:** §<section>

### User story
As a <role>, I want <capability>, so that <benefit>.

### Context (why)
Why this matters now, what is broken or missing without it, and what it unblocks.

### Description (what)
What is observably different once this ships, in plain prose.

### Requirements (how)
- Files / modules this ticket owns (nothing outside this list may be edited)
- Libraries, data shapes, algorithms
- Performance budget this change is held to
- Explicitly out of scope

### Acceptance criteria (Gherkin)
```gherkin
Scenario: <name>
  Given <precondition>
  When <action>
  Then <observable, checkable outcome>
```

### Worklog
- _(append dated entries; see CLAUDE.md §2.4)_
````

---

# Epic: Foundations

## QUI-001 — Project scaffold, build and CI

> **Partially landed out of order (2026-08-27).** QUI-022 needed somewhere real to live, so
> the pure-Kotlin half exists: root `settings.gradle.kts`/`build.gradle.kts`, `core:model`
> and `core:index`, building and testing on a desktop JVM. Still owed by this ticket: the
> Android application modules (`app:companion`, `app:ttsservice`), `.github/workflows/ci.yml`,
> and the module-boundary checks. Those need an Android SDK, which this environment cannot
> reach — `dl.google.com` is denied by the network egress policy.

**Status:** In progress · **Owner:** session-visibility-check · **Epic:** Foundations · **Depends on:** —
**PRD:** §2

### User story
As a developer, I want a buildable project skeleton with CI, so that every later ticket
starts from a green baseline instead of inventing its own structure.

### Context (why)
The repository currently holds documentation only. Until there is a build, no ticket can
prove it works, and parallel agents will each invent a different module layout that then
has to be reconciled. This is the interface seam every other ticket sits on, so it lands
first and lands small.

### Description (what)
A minimal Android application module that builds, installs, and shows an empty screen,
plus the shared module boundaries the pipeline will fill in, plus a CI workflow that
builds and runs unit tests on every push.

### Requirements (how)
- Owns: `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, `app/`, `core/`,
  `.github/workflows/ci.yml`, `.gitignore`, `docs/architecture.md`
- Modules: `app` (UI shell), `core:reader`, `core:attribution`, `core:tts`,
  `core:model` (shared data types). Empty but wired, with dependency direction
  `app → core:*` and no `core → core` cycles.
  **Rule corrected during implementation, 2026-08-29:** "no core module depends on another"
  is false as written — `core:index` depends on `core:model`, deliberately, and this same
  requirement calls `core:model` the shared data types. The rule enforced by
  `checkModuleBoundaries` is the one it meant: `core:model` is the single permitted shared
  leaf, it may depend on nothing itself, and no core module may reach sideways, upward into
  `app`, or down into a spike.
- Minimum Android SDK 26; target the current stable SDK.
- `.gitignore` must exclude model weights (`*.gguf`, `*.onnx`), audio caches, and
  `.epub` fixtures over 1 MB.
- CI: build + unit test on push and PR. No emulator tests yet.
- Out of scope: any reader, AI, or audio functionality; iOS targets.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Clean checkout builds
  Given a fresh clone of the repository
  When I run "./gradlew assembleDebug"
  Then the build succeeds with no manual setup beyond a standard Android SDK

Scenario: CI runs on a pull request
  Given a pull request against main
  When CI runs
  Then it builds the debug variant and executes unit tests
  And the check appears as required on the pull request

Scenario: Module boundaries hold
  Given the module graph
  When I inspect the dependency declarations
  Then no core module depends on another core module
  And no core module depends on "app"

Scenario: Model weights cannot be committed
  Given a file "model.onnx" placed in the working tree
  When I run "git status --porcelain"
  Then the file is not listed as untracked content to add
```

### Worklog

**2026-08-29 — session-visibility-check.** Reproduce with
`./gradlew test checkModuleBoundaries`: 61 tests, 0 failures, boundaries clean.

*Gradle wrapper.* The acceptance criteria say `./gradlew` and there was none — every
session so far has used whatever `gradle` happened to be installed. `./gradlew` now
bootstraps 8.14.3, so a clean checkout builds with a pinned Gradle.

*Module boundaries* are enforced by a `checkModuleBoundaries` task wired into `check`, with
the rule corrected as above. **Verified by breaking it:** adding
`core:model → core:index` fails the build with
`:core:model must depend on nothing: found :core:index`, and removing it passes again. A
check that has never failed is not known to work — the same lesson QUI-023's benchmark
taught an hour earlier.

*`.gitignore`* covers weights (`*.gguf`, `*.onnx`, `*.bin`, `*.aar`), generated audio and
caches, books, and built indexes. The ticket asked to exclude `.epub` **over 1 MB**; a size
rule cannot be expressed in `.gitignore`, and §8 forbids committing book files at any size,
so the stricter rule is the correct one — the slice's book is generated from a labelled TSV
rather than stored. Verified by dropping `model.onnx`, `book.epub`, `voice.wav` and
`index.db` in the tree: none appears in `git status --porcelain`, and no already-tracked
file is caught by the new rules.

*CI does something this ticket did not anticipate.* GitHub's runners ship the Android SDK,
which the dev containers cannot reach (CLAUDE.md §9). So the workflow has a second job that
fetches the AAR, builds the slice index, assembles the probe, and **uploads the APK and the
matching EPUB as artefacts**. That is a way to get a build onto the device without an agent
being able to compile Android at all, and it is the fastest route to unblocking QUI-019.

**CI run #1 is green** — [run 33261024333](https://github.com/Ghigog/Quire/actions/runs/33261024333),
both jobs, first attempt. `quire-tts-probe-debug` (11.6 MB) and `slice-book` are attached to
it, and artefacts expire on 2026-11-27.

The consequence is larger than a green tick. **The `Assemble` step compiled the Android
sources that were handed over unbuilt on this branch** — `AndroidSql`, `SliceIndex` and the
rewritten `QuireProbeService.speak()`. QUI-019's worklog warned to expect compiler errors
first; there were none. More usefully, the constraint in CLAUDE.md §9 is now routed around
rather than merely documented: an agent that cannot resolve the Android Gradle Plugin can
still get a signed-by-nobody debug APK onto the device by pushing.

*What is left before this is Done:*

1. **The Android application modules** (`app:companion`, `app:ttsservice`) — still owed.
   They cannot be built in a dev container, but CI compiles Android now, so they no longer
   have to be written blind.
2. **"The check appears as required on the pull request"** is a branch-protection setting.
   It needs a human in GitHub settings; nothing in this repository can assert it.

---

## QUI-002 — EPUB import and Readium reader shell

> **Deferred to V3.0 by PRD v1.2 (2026-08-27).** PRD v1.2 puts Quire beneath the reader the user already has. A built-in Readium
> reader returns in V3.0 as the standalone fallback and the route to guaranteed
> word-level highlighting.
>
> Text below is the original v1.1 ticket, unedited.

**Status:** Deferred · **Owner:** — · **Epic:** Foundations · **Depends on:** QUI-001
**PRD:** §2, §4.1

### User story
As a reader, I want to open an EPUB from my device and page through it, so that Quire is
usable as a plain e-reader before any audio exists.

### Context (why)
Everything downstream consumes the parsed document: attribution reads its text nodes,
highlighting addresses its DOM ranges. Adopting Readium now, rather than a bespoke
parser, buys pagination, CSS injection and locator handling that we would otherwise
rebuild badly.

### Description (what)
The user picks an `.epub` file, it appears in a library list, and opening it shows the
book paginated with working forward/back navigation, a chapter list, and controls for
font face, font size, line height and margins.

### Requirements (how)
- Owns: `core/reader/`, `app/.../library/`, `app/.../reader/`
- Uses the Readium Kotlin toolkit for parsing, pagination and locators. Publication
  state is exposed through a single `ReaderSession` type — downstream tickets consume
  that, never Readium types directly.
- Text is exposed to consumers as an ordered stream of paragraph units carrying a stable
  Readium locator, so that attribution and highlighting can address the same unit.
- Import copies the file into app storage; the source file is never mutated.
- Typography settings persist per book.
- Out of scope: audio, highlighting, e-ink specific rendering (QUI-003), cloud sync.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Import a valid EPUB
  Given I am on the library screen
  When I import a valid EPUB 3 file
  Then the book appears in the library with its title and author
  And opening it renders the first page of content

Scenario: Reject an unreadable file
  Given I am on the library screen
  When I import a file that is not a valid EPUB
  Then I see an error naming the file
  And the library is unchanged

Scenario: Paginate through a chapter
  Given a book is open
  When I turn the page forward and then back
  Then I return to the exact page I started on

Scenario: Typography settings apply and persist
  Given a book is open
  When I set the font size to its largest value and reopen the book
  Then the text renders at that size

Scenario: Paragraph stream is addressable
  Given a parsed publication
  When a consumer requests the paragraph stream
  Then each unit carries a locator that resolves back to the same text range
```

### Worklog
- _(empty)_

---

## QUI-003 — E-ink display mode and hardware keys

**Status:** Todo · **Owner:** — · **Epic:** Foundations · **Depends on:** QUI-002
**PRD:** §2

### User story
As an Onyx Boox reader, I want a monochrome, animation-free mode with working hardware
page-turn keys, so that Quire feels native on e-ink instead of ghosting and flickering.

### Context (why)
E-ink panels punish everything a normal Android UI does: animation smears, greys dither,
and per-frame updates leave ghosts. Retrofitting this after the UI is built is far more
expensive than establishing it as a mode from the start.

### Description (what)
A display mode setting with Light, Dark and E-ink options. In E-ink mode the UI is pure
black and white, every animation and transition is disabled, view updates are batched
into a single refresh, and hardware page-turn and volume keys turn pages.

### Requirements (how)
- Owns: `app/.../theme/`, `app/.../input/`, e-ink overrides in `app/.../reader/`
- Monochrome palette is `#000000` and `#FFFFFF` only — no intermediate greys anywhere in
  the mode, including disabled states, dividers and icons.
- All animator durations set to zero in the mode; no cross-fades, ripples, or shimmer
  placeholders.
- Reader view mutations coalesced so one page turn produces one refresh.
- Key handling: volume up/down and page-turn keycodes map to previous/next page while
  reading; they retain default behaviour elsewhere.
- Out of scope: vendor-specific refresh SDKs (Onyx/Meebook) — track separately if the
  generic path proves insufficient.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Monochrome palette contains no greys
  Given E-ink mode is active
  When the reader and its settings screens are rendered
  Then every colour resolved from the theme is #000000 or #FFFFFF

Scenario: Animations are disabled
  Given E-ink mode is active
  When I navigate between any two screens
  Then no transition animation is played

Scenario: One page turn, one refresh
  Given E-ink mode is active and a book is open
  When I turn the page
  Then the reader view is invalidated exactly once

Scenario: Hardware keys turn pages
  Given a book is open
  When I press the volume-down key
  Then the reader advances one page and the device volume is unchanged

Scenario: Keys behave normally outside the reader
  Given I am on the library screen
  When I press the volume-down key
  Then the device volume decreases
```

### Worklog
- _(empty)_

---

## QUI-004 — Reading position and progress tracking

> **Deferred to V3.0 by PRD v1.2 (2026-08-27).** The host reader owns position under v1.2 — Quire cannot see it and does not need it.
> `core:index`'s cursor (QUI-022) replaces this for matching purposes.
>
> Text below is the original v1.1 ticket, unedited.

**Status:** Deferred · **Owner:** — · **Epic:** Foundations · **Depends on:** QUI-002
**PRD:** §4.1

### User story
As a reader, I want Quire to remember exactly where I stopped, so that I can close the
app mid-sentence and resume there.

### Context (why)
Position is shared state between reading and listening: pausing playback must leave the
text at the same place, and vice versa. Getting one durable representation of "where I
am" now avoids two drifting ones later.

### Description (what)
The current locator is persisted continuously, restored on reopen, and shown as a
percentage through the book and through the current chapter. Chapter navigation jumps
update it. The store is local and works entirely offline.

### Requirements (how)
- Owns: `core/reader/progress/`, its persistence schema and migrations
- Position is a Readium locator plus a timestamp; store the last position per book and
  keep the five most recent distinct positions for recovery.
- Writes are debounced (≥2 s) and must not block the render path.
- Survives process death, not just backgrounding.
- Offline-only. "Offline position syncing" in the PRD means durable local sync between
  reader and player, not a network service.
- Out of scope: cross-device sync, bookmarks and annotations (separate tickets).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Resume after process death
  Given I am reading at 43% of a book
  When the app process is killed and I reopen the book
  Then the reader opens at the same locator

Scenario: Progress is reported at both scopes
  Given a book is open mid-chapter
  When I read the progress indicator
  Then it shows progress through the book and through the current chapter

Scenario: Chapter jump updates position
  Given a book is open
  When I navigate to a later chapter and reopen the book
  Then the reader opens in that chapter

Scenario: Persistence does not stall rendering
  Given a book is open
  When I turn twenty pages in rapid succession
  Then no page turn is delayed by a position write
```

### Worklog
- _(empty)_

---

# Epic: Attribution

## QUI-005 — `characters.json` schema and manifest store

**Status:** In review · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-001, QUI-017
**PRD:** §3.1

### User story
As a developer, I want a frozen character manifest schema, so that the scan, the
attribution engine, the voice caster and the drawer can all be built in parallel against
it.

### Context (why)
This is the highest-value interface seam in the project: four downstream tickets consume
it. It lands early, small, and independently of any model, so that agents can fan out
(CLAUDE.md §2.3).

### Description (what)
A versioned JSON schema describing the characters detected in a book, a typed model of
it, a reader/writer with validation, and per-book storage. Ships with a hand-written
example manifest that serves as the fixture for every downstream test.

### Requirements (how)
- Owns: `core/model/characters/`, `docs/schema/characters.schema.json`,
  `core/model/src/test/resources/characters.example.json`
- Manifest fields: `schemaVersion`, `bookId`, `generatedAt`, `narrator`, and
  `characters[]` with `id`, `displayName`, `aliases[]`, `gender`
  (`male|female|neutral|unknown`), `ageBand` (`child|teen|adult|elder|unknown`),
  `traits[]`, `confidence` (0–1), `firstSeenLocator`, `lineCount`.
- Unknown fields are preserved on round-trip; unknown enum values degrade to `unknown`
  rather than failing the load.
- A manifest failing validation is rejected with a message naming the offending field
  path — never partially loaded.
- Out of scope: generating the manifest (QUI-007), voice assignment (QUI-011).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Round-trip preserves content
  Given the example manifest
  When it is parsed and re-serialised
  Then the result is semantically identical, including unknown fields

Scenario: Invalid manifest is rejected clearly
  Given a manifest whose "confidence" is 1.4
  When it is loaded
  Then loading fails with an error naming "characters[n].confidence"
  And no partial manifest is exposed

Scenario: Forward compatibility
  Given a manifest with an unrecognised gender value
  When it is loaded
  Then that character's gender is "unknown" and the load succeeds

Scenario: Storage is per book
  Given manifests written for two different books
  When each is loaded by book id
  Then each returns its own characters
```

### Worklog

**2026-08-29 — session-visibility-check.** `core/model/characters/`,
`docs/schema/characters.schema.json`, and the example manifest that downstream tickets test
against. Reproduce with `gradle :core:model:test`; 7 tests, one per Gherkin scenario plus
two the scenarios implied. Whole repository: 61 tests, 0 failures.

*Dependency, justified as CLAUDE.md §6 requires.* `kotlinx-serialization-json`, roughly
900 KB against the 450 MB footprint. This is the seam four tickets consume, so its parser
has to be right rather than clever, and escapes, unicode and number handling are exactly
where a hand-rolled reader goes quietly wrong. The mapping is still hand-written rather
than `@Serializable`, because annotations give neither of the two things the ticket asks
for: unknown fields *kept* rather than ignored, and a rejection that names a field path.

*Two rules that shape the design.* Unknown fields survive a round trip, because a newer
companion app handing a manifest to an older service must get its data back rather than a
silently trimmed one; they live in `extras` and are re-emitted last, so a future field can
never shadow one we own. And loading is all or nothing — a half-read cast is worse than
none, since the reader would hear some characters correctly and the rest as the narrator
with no way to tell which.

*Two checks the ticket did not ask for but the type system cannot make.* Duplicate
character ids are rejected: casting keys on the id, and a duplicate shows up only as one
character occasionally speaking in another's voice. And `ManifestStore` percent-encodes the
book id when naming files — ids come from a fingerprint and *should* be hex, but nothing
says so, and a `/` would write outside the store. Both have tests.

*Writes are atomic*, via a temp file and rename. A manifest half-written when the process
died would fail validation on next read and lose the book's cast.

*What is left before this is Done:* nothing in this ticket's own scope. It is `In review`
rather than `Done` because its declared dependency QUI-001 does not exist, so the schema
has never been exercised by a real consumer — the first of QUI-007, QUI-008 or QUI-011 to
land is what will actually prove the shape is right. It is deliberately frozen now anyway,
which is the point of a seam (CLAUDE.md §2.3).

---

## QUI-006 — On-device SLM runtime

**Status:** In review · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-001
**PRD:** §3.1

### User story
As a developer, I want a wrapper around a quantized 1B language model running on-device,
so that scanning and attribution can prompt it without knowing anything about the
runtime.

### Context (why)
Both the book scan and Tier 2 attribution need inference, and both must stay inside the
RAM budget shared with TTS and the UI. Isolating the runtime behind one interface also
lets us swap Llama 3.2 1B for Qwen 2.5 1.5B, or `llama.cpp` for ExecuTorch, without
touching callers.

### Description (what)
An `SlmRuntime` interface offering "complete this prompt" and "complete into this JSON
shape" with a token budget and a cancel signal, backed by a quantized model loaded from
app storage. Model files are fetched by a documented script, never committed.

### Requirements (how)
- Owns: `core/attribution/slm/`, `tools/fetch-models.sh`, `docs/adr/0001-slm-runtime.md`
- Backend: `llama.cpp` via JNI with a Q4_K_M quantization, or ExecuTorch — whichever the
  ADR selects on measured RSS and tokens/s; the ADR must record both measurements.
- The runtime is loaded lazily and released under memory pressure; it must never be
  resident at the same time as an idle TTS session if the combined RSS would exceed the
  1.2 GB budget (PRD §5).
- Structured output is validated against the caller's expected shape; a malformed
  generation is retried once, then reported as failure. Callers never see raw text they
  have to parse defensively.
- All inference is cancellable and runs off the main thread.
- Out of scope: prompts for scanning or attribution (QUI-007, QUI-009).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Structured completion returns typed output
  Given a loaded runtime
  When I request a completion constrained to a known JSON shape
  Then I receive a parsed object of that shape

Scenario: Malformed generation is retried then reported
  Given a runtime that produces invalid JSON twice
  When a structured completion is requested
  Then exactly one retry occurs
  And the caller receives an explicit failure, not raw text

Scenario: Memory budget is respected
  Given the runtime is loaded on a mid-tier ARM device
  When peak resident memory is measured during a completion
  Then it stays within the budget recorded in the ADR

Scenario: Inference is cancellable
  Given a long-running completion
  When it is cancelled
  Then it stops within 500 ms and releases its working memory
```

### Worklog

**2026-09-06 — `slm-runtime-interface`.** Built the parts that don't need the Boox, per
this session's brief. Landed `docs/adr/0001-slm-runtime.md`, `core/attribution/slm/`
(`SlmRuntime`, `CancellationSignal`, `JsonShape`, `StructuredCompletion`,
`BackgroundSlmExecutor`), and `tools/fetch-models.sh`.

*What is done and tested (`gradle :core:attribution:test`, all green, no device needed):*
- `SlmRuntime`: a one-method `fun interface` — `complete(prompt, maxTokens, cancellation)` —
  pure JVM, no Android, so QUI-007/QUI-009 can build and test against it today with a fake.
- `StructuredCompletion`: validates against a caller-supplied `JsonShape<T>`, retries
  exactly once, returns `StructuredResult.Success`/`Failure` — never raw text. Covers the
  ticket's first two Gherkin scenarios directly: `StructuredCompletionTest` asserts a valid
  first reply needs no retry, one bad reply retries once and then succeeds, and two bad
  replies report `Failure` with the runtime called exactly twice, never three times.
- `CancellationSignal` + `BackgroundSlmExecutor`: a polled flag rather than
  `kotlinx.coroutines.Job`, because no JVM thread interrupt reaches into a native
  `llama.cpp` loop uninvited — a backend has to poll regardless. `BackgroundSlmExecutorTest`
  demonstrates the contract end to end on a fake backend that ticks every 20 ms: cancelling
  mid-generation stops the call well inside the 500 ms acceptance criterion. This proves the
  *mechanism*, not the device number — see below.
- `tools/fetch-models.sh`: fetches Llama 3.2 1B Instruct, Q4_K_M GGUF
  (`bartowski/Llama-3.2-1B-Instruct-GGUF`), ~770 MiB. **Ran it in this session** — it works.
  The one thing worth flagging for whoever touches it next: this repo's files resolve
  through HF's Xet backend (confirmed by hand — the `resolve/main/...` URL redirects to
  `us.aws.cdn.hf.co/xet-bridge-us/...`, not a plain CDN), so it uses `huggingface_hub` with
  `HF_HUB_DISABLE_XET=1` rather than a bare `curl`, which would hang against a host outside
  the environment's allowlist rather than fail — matches the note this session was given.

*What is NOT done, and is not a small gap:* **no backend is chosen, and no backend is
implemented.** ADR-0001 is deliberately `Proposed`, not `Accepted`, because the ticket's
other two Gherkin scenarios — the memory-budget scenario and the *real* 500 ms cancellation
scenario against genuine token generation — need an actual `llama.cpp` or ExecuTorch
implementation running on the Note Air5 C, and this session has no device. **I have not
fabricated an RSS or tokens/s number anywhere** — ADR-0001's "What is still needed" section
says exactly what QUI-031 has to measure and why (prompt-eval tokens/s, not generation,
per `device-profile.md` §2 — a scene-sized prompt spends most of its cost on the prompt).
QUI-031 already depends on QUI-006 and is the right ticket to carry that measurement; this
one should not block on it, which is why it's `In review` rather than `Blocked`.

**Reproduce:**
```bash
gradle :core:attribution:test
./tools/fetch-models.sh   # ~770 MiB to $QUIRE_MODELS or ~/.cache/quire/models
```

*Surprise worth naming:* `fun interface` abstract methods can't carry default parameter
values in Kotlin — `SlmRuntime.complete`'s `cancellation` parameter has no default, unlike
`StructuredCompletion.complete`'s, which is a regular class method and can. Caught by the
compiler immediately, not worth a design change, but easy to trip over again if the
interface grows a second method later (it would then need to stop being `fun interface`
regardless).

---

## QUI-007 — Upfront book scan → character manifest

> **Note from QUI-037, 2026-09-06.** ADR-0006's job C (voice design) now has a
> pure-Kotlin implementation to call: `quire.voice.design.VoiceDesigner` in `core/voice`,
> taking a character and its confidently-attributed explicit-tag lines and returning a
> `Voice?` — null below three lines, per the handoff's proposed fallback. It does not write
> `description` via the SLM; that half waits on QUI-006 and is a drop-in replacement when it
> lands, since `source` already stays `AUTO`. This ticket's scan still needs to call it once
> Tier 1's explicit set is available per character.

**Status:** Todo · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-005, QUI-006
**PRD:** §3.1

### User story
As a reader, I want Quire to work out who is in my book when I import it, so that voices
are already cast before I press Play.

### Context (why)
Voice casting needs a roster, and deriving it lazily during playback would blow the
800 ms TTFS budget. Doing it once at import trades a one-off wait for instant playback
forever after.

### Description (what)
On import, a background job scans the book with the SLM, extracts the cast — names,
aliases, estimated gender and age band, personality traits — and writes a
`characters.json` manifest. Progress is visible, the job survives backgrounding, and the
book is readable (though not yet voiced) while it runs.

### Requirements (how)
- Owns: `core/attribution/scan/`, the import-progress UI in `app/.../library/`
- Chunk the book to fit the model's context; merge per-chunk results by name and alias,
  collapsing variants ("Sarah", "Miss Bennet") into one character with a confidence
  weighted by mention count.
- Discard candidates appearing fewer than 3 times unless they carry attributed dialogue.
- Writes exactly one manifest per book, atomically (temp file then rename).
- Resumable: a scan interrupted at chunk *n* restarts at chunk *n*, not at zero.
- Budget: a 100k-word novel scans in **under 30 minutes** on the reference device; record
  the measured time in the Worklog. (Relaxed from 10 minutes on 2026-08-27: PRD v1.2 moves
  indexing into the companion app, off any real-time path. See QUI-025.)
- Out of scope: line-by-line attribution (QUI-008/009), voice assignment (QUI-011).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Import produces a manifest
  Given I import a novel with several named characters
  When the scan completes
  Then a valid characters.json exists for that book
  And each detected character has a name, gender estimate, age band and traits

Scenario: Aliases collapse to one character
  Given a book referring to the same person as "Sarah" and "Miss Bennet"
  When the scan completes
  Then one character exists carrying both as aliases

Scenario: Walk-ons are discarded
  Given a name mentioned twice with no dialogue
  When the scan completes
  Then that name is not in the manifest

Scenario: Scan resumes after interruption
  Given a scan interrupted at 60%
  When the app restarts
  Then the scan resumes from that point and completes

Scenario: The book is readable during the scan
  Given a scan is running
  When I open the book
  Then I can read and page through it normally
```

### Worklog
- _(empty)_

---

## QUI-008 — Tier 1 heuristic dialogue attribution

> **Measured 2026-08-29, and it points at the cheapest win in the project.** Tier 1 over the
> slice's own book attributed 3 of 9 dialogue spans (33%, in line with PDNC's 39.4%), at 3/3
> precision. Three of the six misses carry a pronoun tag — `" she said.`, `" he said.` — and
> a pronoun plus the cast's gender from `characters.json` resolves them outright. That is
> 33% → 67% on this chapter with no model loaded. See `docs/architecture.md` §5.

**Status:** In review · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-005
**PRD:** §3.1

### User story
As a reader, I want explicitly tagged dialogue attributed instantly, so that most lines
get the right voice without waiting on a language model.

### Context (why)
The majority of dialogue in most prose carries an explicit speech tag. Handling those
with regex is effectively free, and every line resolved here is a line the SLM never has
to see — which is what keeps us inside the RTF and battery budgets.

### Description (what)
A pass over the paragraph stream that segments text into narration and quoted speech and
resolves the speaker where a speech tag makes it explicit, emitting a result with a
confidence score and the tier that produced it.

### Requirements (how)
- Owns: `core/attribution/heuristic/`, the shared `AttributionResult` type in
  `core/model/`
- `AttributionResult`: `locator`, `text`, `kind` (`narration|dialogue`), `speakerId?`,
  `confidence`, `tier` (`heuristic|slm|scene|narrator`).
- Handle: trailing tags (`"I know," said Sarah`), leading tags (`Sarah said, "I know"`),
  inverted order, action beats (`Sarah set down the cup. "I know."`), and both straight
  and typographic quotes, plus em-dash dialogue.
- Speaker names resolve against the manifest's names and aliases; an unmatched name
  yields no attribution rather than a new character.
- Tier 1 output confidence is 0.95 for a direct tag, 0.75 for an action beat.
- Budget: a 100k-word book is fully processed in under 2 seconds.
- Out of scope: unattributed dialogue (QUI-009).

### Acceptance criteria (Gherkin)
```gherkin
Scenario Outline: Explicit speech tags resolve
  Given a manifest containing "Sarah"
  When the line <line> is attributed
  Then the speaker is "Sarah" with tier "heuristic" and confidence >= 0.9

  Examples:
    | line                          |
    | "I know," said Sarah.         |
    | Sarah said, "I know."         |
    | "I know," Sarah said quietly. |

Scenario: Action beats attribute with lower confidence
  Given a manifest containing "Sarah"
  When attributing 'Sarah set down the cup. "I know."'
  Then the speaker is "Sarah" with confidence below 0.9 and at or above 0.65

Scenario: Narration is not treated as dialogue
  Given a paragraph containing no quoted speech
  When it is attributed
  Then its kind is "narration"

Scenario: Unknown names do not invent characters
  Given a manifest without "Gregor"
  When attributing '"I know," said Gregor.'
  Then no speaker is assigned and the manifest is unchanged

Scenario: Throughput
  Given a 100,000 word book
  When Tier 1 attribution runs over it
  Then it completes in under 2 seconds
```

### Worklog

**2026-08-29 — session-visibility-check.** `core/attribution/` and `AttributionResult` in
`core/model/`. Reproduce with `./gradlew :core:attribution:test`; 13 tests, one per Gherkin
scenario plus the pronoun rule and the scoring. Whole repository: 80 tests, 0 failures,
boundaries clean.

*Measured, on the labelled fixtures rather than on examples chosen to flatter it:*

| fixture | gold | attributed | correct | coverage | precision |
| --- | --- | --- | --- | --- | --- |
| tagged | 10 | 9 | 8 | 90.0% | 88.9% |
| untagged | 15 | 3 | 3 | 20.0% | 100.0% |
| beats | 9 | 6 | 5 | 66.7% | 83.3% |
| **all** | **34** | **18** | **16** | **52.9%** | **88.9%** |

*The pronoun rule, scored against its own absence on identical text* — the only fair way to
say what it bought, and the same flag trick QUI-028 used for action beats:

```
pronoun rule OFF: coverage 44.1%, precision 86.7%
pronoun rule ON:  coverage 52.9%, precision 88.9%
lift: +8.8 points of coverage, +3 lines
```

All three added lines were right, so precision *rose*. **That is less than the 33% → 67% I
projected from the slice's own chapter**: nine spans was an anecdote and the chapter's mix
of tags happened to favour the rule. The fixture number is the one to quote.

*The rule declines more than it resolves, deliberately.* `she said` attributes only when the
cast holds exactly one woman. With two, a pronoun narrows the field without choosing, and
choosing anyway would be a guess wearing a confidence score — that is QUI-009's job. A
speech verb is also required, so `She crossed to the window. "I know."` stays an action beat
rather than being promoted to a tag. Both have tests, as does whole-word matching, because
`he` inside `the` was an obvious way to get this quietly wrong.

*Throughput:* 125,000 words in **183 ms** on the host, against the ticket's 2 s for 100k. A
desktop number kept as a regression guard rather than an SLA claim (CLAUDE.md §1.6).

*Confidences are left as the ticket specifies* — 0.95 direct, 0.75 beat, 0.85 pronoun — and
are **known to be optimistic**: QUI-028 measured explicit tags at 89.9% precision on PDNC
against the declared 0.95 (68.6% before its 2026-09-02 re-measurement). Moving them without a measurement would swap one fiction for
another, so the class documents the discrepancy and recalibration stays QUI-009's
prerequisite.

**2026-08-29 (later) — the two implementations are one.** `spike/pipeline` now runs
`core:attribution` through a 55-line adapter; its 199 lines of duplicated rules are gone,
and its own 12 tests pass against the core implementation unchanged. Root build 99 tests,
spike 12, all green, boundaries clean.

*Porting beat rewriting, measurably.* Swapping my `contains`-based tag matching for the
spike's regexes — which had been scored against PDNC — took precision from **88.9% to
100%** on the same fixtures and raised the pronoun rule's lift from +8.8 to **+11.8 points**
(41.2% → 52.9%, four lines, all correct). The lesson is worth keeping: a fresh
implementation is an unmeasured one wearing the measured one's numbers.

*Two things the spike's tests caught that my port had quietly dropped*, both in `evidence`:
a tag naming somebody outside the cast used to record the name, and a pronoun tag that
cannot be pinned down used to say so. The second matters beyond tidiness — `pronoun speech
tag` and `no tag` are very different prospects for the model, and QUI-009 chooses its
targets by exactly that. `Tier.NONE` came back for the same reason: "Tier 1 declined, spend
the model here" is not "this is narration", and folding them together looks harmless right
up until it isn't.

*`Roster` moved into core too*, because a heuristic that needs a manifest is useless on a
book nobody has scanned. It discovers the cast from speech tags and adjacency, **and infers
gender from the pronoun that stands in for each name** — without which an uploaded book has
no genders, the pronoun rule cannot fire, and casting falls back to arbitrary speaker ids.
That is the chain that put a male Sarah on the device.

Two bugs found writing it, both from testing on real prose rather than on examples: the
anaphora cursor was reset at every paragraph break, where anaphora routinely crosses them;
and pronouns *inside* quotations were being counted, though `"You have been standing
there," she said` has "you" inside and "she" outside, and only the second says anything
about the speaker.

*Measured end to end.* `export` over the slice's own book went **33.3% → 44.4%** — the
`he said` lines now resolve to Thomas through the real pipeline, which was impossible while
the improvement lived only in a module nothing called. Sarah's `she said` still does not:
gender inference needs two pronoun sightings and this chapter gives her one. Conservative
by design, and a novel supplies hundreds.

*What is left before this is Done.*

1. **PDNC scoring.** These fixtures are 34 lines of prose written for this repository; PDNC
   is 2,846 quotations from five novels. The scorer exists in `spike/pipeline` but points at
   that module's own Tier 1, so pointing it here means editing QUI-018's files — not this
   ticket's to touch (CLAUDE.md §2.2). Needs a small follow-up or QUI-018's owner.
2. ~~**Two implementations now exist.**~~ Done, same day: the spike delegates and its copy
   is deleted.
3. **Em-dash dialogue** is segmented but never scored — no fixture uses it.

---

## QUI-009 — Tier 2/3 SLM attribution with confidence fallback

**Status:** Todo · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-006, QUI-008
**PRD:** §3.1

### User story
As a reader, I want back-and-forth dialogue with no speech tags to still land on the
right voices, so that conversations don't collapse into the narrator.

### Context (why)
Long exchanges routinely drop speech tags for pages at a time — exactly the passages
where multi-voice reading is most valuable. Tier 1 cannot resolve these. The PRD sets
explicit confidence gates so that the system degrades to something safe rather than
guessing loudly.

### Description (what)
For each **scene** holding lines Tier 1 left unattributed, the SLM is asked who speaks each
one, given the whole scene and its cast, and answers with a list. Below 0.65 confidence a
line falls back to inferring from the speakers active in the scene; still below 0.40, it is
read by the Narrator.

> **Amended 2026-09-07.** This ticket originally said "a 5-line sliding context window", one
> call per line. [ADR-0006](docs/adr/0006-three-attribution-jobs.md) superseded that on both
> cost and quality: per-line cannot fit QUI-007's 30-minute budget by an order of magnitude,
> and a model shown one quotation in isolation knows strictly less than the reader does,
> because turn-taking is a property of the scene. The confidence gates below are unchanged —
> they come from the PRD, not from the window size.

### Requirements (how)
- Owns: `core/attribution/slm/attribution/`, `core/attribution/scene/`
- **Prompt unit: one scene** (`core/attribution/scenes`, QUI-038), carrying the scene's
  text, the cast, and any speaker Tier 1 already resolved inside it. A scene too long for
  the window is split by `SceneSplitter`, which carries the last speaker across.
- **The answer is a list aligned to quotation ids, and alignment fails closed.** A reply
  naming 11 speakers for 12 quotations drops the whole scene to Tier 3 rather than shifting
  everyone by one: a misaligned list is confidently wrong for a scene at a time, which
  ADR-0005 makes the most expensive failure this system has.
- A piece whose `atTurnBoundary` is false opens mid-exchange, so its carried speaker is a
  weaker guide; weight it below one carried across a real turn boundary (QUI-038).
- Thresholds are named constants, exactly as the PRD states: `SLM_MIN = 0.65`,
  `NARRATOR_FLOOR = 0.40`. Changing them requires a ticket.
- Tier 3 scene inference: alternate between the two most recently active speakers for a
  two-party exchange; otherwise Narrator.
- Results are cached per locator so a re-read never re-infers.
- Runs ahead of playback, never in its critical path — a line whose attribution is not
  ready yet is voiced by the Narrator rather than stalling audio.
- Accuracy target: ≥85% correct speaker on the labelled fixture set, measured and
  recorded in the Worklog.
- Out of scope: emotion tags (V2, PRD §6).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Untagged dialogue resolves from context
  Given an exchange where Tier 1 attributed the two preceding lines
  When an untagged line between them is attributed
  Then the SLM assigns a speaker with tier "slm"

Scenario: Low confidence falls back to scene inference
  Given the SLM returns confidence 0.50 for a line
  When attribution completes
  Then the speaker comes from active scene speakers with tier "scene"

Scenario: Very low confidence falls back to the narrator
  Given the SLM returns confidence 0.30 and no clear active speaker
  When attribution completes
  Then the line is attributed to the Narrator with tier "narrator"

Scenario: Attribution never blocks playback
  Given playback reaches a line whose attribution is still pending
  Then the line is voiced by the Narrator without a pause in audio

Scenario: Results are cached
  Given a line already attributed by the SLM
  When the same line is attributed again
  Then no inference is performed

Scenario: Accuracy on the fixture set
  Given the labelled dialogue fixture set
  When attribution runs end to end
  Then at least 85% of lines match the labelled speaker
```

### Worklog
- _(empty)_

---

# Epic: Audio

## QUI-010 — ONNX TTS engine with boundary timestamps

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-001, QUI-017
**PRD:** §3.2, §4.2

### User story
As a reader, I want text synthesised into speech on-device, with timings for each word,
so that I can hear my book and see the line I am hearing.

### Context (why)
This is the other big interface seam: the ring buffer, voice casting and highlighting all
sit on top of it. The word-boundary timestamps have to be emitted by synthesis itself —
reconstructing them afterwards from audio is both inaccurate and expensive.

### Description (what)
A `TtsEngine` that takes text plus a voice id and returns PCM audio together with word
and sentence boundary timestamps, running an ONNX model locally with no network access.

### Requirements (how)
- Owns: `core/tts/`, `docs/adr/0002-tts-engine.md`
- Engine: Kokoro-TTS (82M ONNX) or Piper C++; the ADR records measured RTF, RSS and
  on-disk size for both and states the choice.
- Output type `TtsChunk`: PCM buffer, sample rate, `voiceId`, and
  `boundaries[] { startMs, endMs, charStart, charEnd, kind: word|sentence }`.
- Synthesis is cancellable mid-utterance and releases its buffers on cancel.
- Budget: RTF ≤ 0.15 on a mid-tier ARM SoC (10 s of audio in under 1.5 s), measured on
  the standard fixture and recorded in the Worklog.
- No network calls of any kind. Cloud synthesis is V2 (PRD §6).
- Out of scope: buffering strategy (QUI-012), voice assignment (QUI-011).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Synthesis produces audio and boundaries
  Given the engine is loaded
  When I synthesise a two-sentence paragraph
  Then I receive PCM audio
  And boundary timestamps covering every word, in ascending order

Scenario: Boundaries map back to the source text
  Given a synthesised chunk
  When I take the boundary for the fifth word
  Then its character range selects that same word in the input text

Scenario: Real-time factor meets the SLA
  Given the standard 10 second fixture on a mid-tier ARM SoC
  When it is synthesised
  Then synthesis completes in under 1.5 seconds

Scenario: Cancellation releases resources
  Given a synthesis in progress
  When it is cancelled
  Then it stops promptly and its buffers are released

Scenario: Fully offline
  Given the device is in airplane mode
  When I synthesise a paragraph
  Then synthesis succeeds
```

### Worklog
- _(empty)_

---

## QUI-011 — Automatic voice casting

> **Note from QUI-036, 2026-09-06.** Casting picks parents by F0, and F0 says nothing about
> whether a reader is any good. The listening test found spk659 — a real trained voice, well
> inside the usable pitch range — sounds mushy, and the voices *generated* from it sound
> better than it does. Before this ticket chooses parents it needs a quality signal
> alongside the pitch one; `fixtures/voices/libritts_r-f0.tsv` does not carry one.

> **Note from QUI-037, 2026-09-06.** The quality signal exists now:
> `fixtures/voices/libritts_r-quality.tsv`, human-curated from listening — two computed
> proxies were tried and both failed to flag spk659 as bad; see QUI-037's Worklog before
> re-deriving either. `core/voice/foundry`'s `Foundry.plan()` does quality-aware parent
> selection and returns a `BlendPlan`; `spike/slice/Casting.kt` calls it for any character
> carrying a descriptor. Still this ticket's to do: a real Android caster against a real
> manifest, and the piece nothing has built yet — reading `emb_g.weight` out of a loaded
> sherpa-onnx session and writing an interpolated row back in, which needs QUI-010 first.

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-007, QUI-010
**PRD:** §4.2

### User story
As a reader, I want each character to get a fitting, distinct voice automatically, so
that I can press Play without configuring anything.

### Context (why)
The product promise is zero setup. Casting is also where "distinct" matters more than
"perfect": two characters in the same scene sharing a voice is far more damaging to
comprehension than a slightly-off age match.

### Description (what)
A deterministic caster that maps each character in the manifest to a local TTS voice
variant using gender, age band and traits, guaranteeing that characters who appear
together are audibly distinct, and reserving a stable Narrator voice.

### Requirements (how)
- Owns: `core/tts/casting/`, per-book cast persistence
- Deterministic: the same manifest always yields the same cast, so a re-import doesn't
  reshuffle the reader's voices.
- Distinctness: characters sharing a scene never share a voice; where the voice pool is
  exhausted, differentiate by pitch/rate offsets before ever reusing a voice.
- Priority by line count — the most-spoken characters get the most distinct voices.
- The cast is persisted and user overrides (QUI-015) always win over auto-assignment,
  including after a rescan.
- Out of scope: the override UI (QUI-015), voice cloning (V2).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Every character is cast without user input
  Given a book with a completed character manifest
  When casting runs
  Then every character and the narrator has an assigned voice

Scenario: Casting is deterministic
  Given the same manifest
  When casting runs twice
  Then both runs produce identical assignments

Scenario: Co-present characters are distinct
  Given two characters with dialogue in the same scene
  When casting runs
  Then they are not assigned the same voice and pitch combination

Scenario: Gender and age inform the choice
  Given a character estimated as an elderly man
  When casting runs
  Then the assigned voice comes from the matching gender and age group where one is available

Scenario: User overrides survive a rescan
  Given I have manually assigned a voice to a character
  When the book is rescanned and cast again
  Then my assignment is preserved
```

### Worklog
- _(empty)_

---

## QUI-012 — Rolling dynamic ring buffer

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-010
**PRD:** §3.2

### User story
As a reader, I want playback to start almost immediately and never stutter, so that
listening feels like a normal audiobook rather than a device thinking out loud.

### Context (why)
Synthesising the whole book up front costs storage and a long wait; synthesising line by
line on demand stutters. The PRD's answer is a rolling buffer of the current paragraph
plus three ahead — enough to absorb synthesis jitter, small enough to stay cheap.

### Description (what)
A buffer that keeps the current paragraph plus the next three synthesised and ready,
refilling as playback advances, discarding chunks once played, and re-seeding itself on
a seek.

### Requirements (how)
- Owns: `core/tts/buffer/`
- Depth: current + 3 paragraphs ahead, adaptive downward under memory pressure but never
  below current + 1.
- Chunks live as temporary `.wav` cache files, deleted immediately after playback and on
  session end; a crash must not leave orphans — clean the cache directory on startup.
- On seek, in-flight synthesis is cancelled and the buffer re-seeds from the new
  position.
- TTFS: under 800 ms from pressing Play, measured from input event to first audio frame,
  recorded in the Worklog.
- Out of scope: transport controls (QUI-013).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Time to first sound
  Given a book is open and audio has not started
  When I press Play
  Then the first audio frame is emitted within 800 ms

Scenario: Buffer stays ahead
  Given playback is running
  When I sample the buffer at any point
  Then it holds the current paragraph and up to three ahead

Scenario: Playback does not stutter across a paragraph boundary
  Given playback is running
  When it crosses into the next paragraph
  Then there is no audible gap

Scenario: Chunks are discarded after playback
  Given a paragraph has finished playing
  When I inspect the cache directory
  Then its wav file is gone

Scenario: Seeking re-seeds the buffer
  Given playback is running
  When I seek to a different chapter
  Then in-flight synthesis is cancelled and playback resumes from the new position within 800 ms

Scenario: Crash leaves no orphaned cache
  Given the process was killed mid-playback
  When the app restarts
  Then the audio cache directory is empty
```

### Worklog
- _(empty)_

---

## QUI-013 — Playback controls

> **Deferred to V3.0 by PRD v1.2 (2026-08-27).** The host reader owns the transport under v1.2. Rate and pitch arrive in the
> `SynthesisRequest` and Quire honours them (QUI-024); it does not present controls.
>
> Text below is the original v1.1 ticket, unedited.

**Status:** Deferred · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-012
**PRD:** §4.2

### User story
As a reader, I want ordinary audiobook controls, so that I can pause, skip back over a
sentence I missed, and set a comfortable speed.

### Context (why)
Nobody listens without these. They also define how playback and reading position stay in
step — pausing must leave the text where the audio stopped, and skipping must move both.

### Description (what)
A transport with play/pause, ±10 s skip, a 0.8×–2.0× speed slider, and a "Narrator Only"
toggle that ignores character casting and reads everything in the narrator voice.

### Requirements (how)
- Owns: `core/tts/playback/`, `app/.../player/`
- Speed change applies to already-buffered audio without a re-synthesis stall, and
  persists across sessions.
- ±10 s skip snaps to the nearest sentence boundary rather than mid-word.
- "Narrator Only" takes effect within one paragraph and does not discard the character
  cast.
- Playback and reading position stay in sync in both directions (QUI-004).
- Media session integration: lock-screen and headset controls work; audio focus is
  respected.
- Out of scope: sleep timer, bookmarks.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Pause leaves the text in place
  Given playback is running mid-paragraph
  When I pause
  Then the reader position matches where the audio stopped

Scenario: Skip snaps to a sentence boundary
  Given playback is running
  When I skip back 10 seconds
  Then playback resumes at the start of the nearest sentence

Scenario Outline: Speed range
  Given playback is running
  When I set the speed to <speed>
  Then audio plays at that rate without a re-synthesis pause

  Examples:
    | speed |
    | 0.8   |
    | 1.5   |
    | 2.0   |

Scenario: Narrator Only mode
  Given a book with several cast characters
  When I enable Narrator Only
  Then all subsequent dialogue is read in the narrator voice within one paragraph
  And disabling it restores the character voices

Scenario: External controls work
  Given playback is running and the screen is locked
  When I press pause on a headset
  Then playback pauses
```

### Worklog
- _(empty)_

---

## QUI-014 — Sentence-level highlighting

> **Deferred to V3.0 by PRD v1.2 (2026-08-27).** Under v1.2 the host renders the text. Quire's half of highlighting — emitting
> `rangeStart` from TTS boundary timestamps — moves to QUI-024. Guaranteed word-level
> highlighting needs Quire's own reader, which is V3.0.
>
> Text below is the original v1.1 ticket, unedited.

**Status:** Deferred · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-010, QUI-013
**PRD:** §4.2

### User story
As a reader, I want the sentence being spoken to be highlighted, so that I can follow
along on the page and pick up reading wherever the audio is.

### Context (why)
Read-along is the feature that ties the audio and reading halves of Quire together. On
e-ink it is also the most refresh-expensive thing we do, so it must be sentence-level
rather than word-level and must batch its updates.

### Description (what)
As audio plays, the current sentence is highlighted in the reader, the view auto-scrolls
or turns the page to keep it visible, and tapping any sentence jumps playback there.

### Requirements (how)
- Owns: `core/reader/highlight/`, the highlight layer in `app/.../reader/`
- Driven by the boundary timestamps from `TtsChunk` (QUI-010), mapped through paragraph
  locators (QUI-002).
- Sentence granularity in all modes; word-level is explicitly excluded on e-ink.
- E-ink: one view invalidation per sentence change, using inversion rather than colour
  (CLAUDE.md §7).
- Drift tolerance: highlight stays within 150 ms of the audio across a full chapter.
- Page turns triggered by highlighting must not fight a manual page turn — manual input
  wins for 5 seconds.
- Out of scope: karaoke word highlighting, note-taking.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: The spoken sentence is highlighted
  Given playback is running
  When a new sentence begins
  Then that sentence is highlighted and the previous one is not

Scenario: Highlight stays in sync
  Given a chapter plays to its end
  When the highlight is compared with audio position
  Then it stays within 150 ms of the audio throughout

Scenario: The page follows the audio
  Given the spoken sentence moves past the bottom of the page
  When it changes
  Then the reader turns to the page containing it

Scenario: Manual reading wins briefly
  Given playback is running
  When I manually turn the page
  Then the view does not jump back for at least 5 seconds

Scenario: Tap to seek
  Given a book is open with playback running
  When I tap a sentence on the page
  Then playback jumps to that sentence

Scenario: E-ink refresh cost
  Given E-ink mode is active and playback is running
  When one sentence transition occurs
  Then exactly one view invalidation is issued
```

### Worklog
- _(empty)_

---

# Epic: UI

## QUI-015 — Character & voice drawer

> **Deferred to V2.0 by PRD v1.2 (2026-08-27).** Explicitly scheduled for V2.0 by PRD v1.2 §5. Automatic casting (QUI-011) must be
> good enough to ship without an override; the override is the V2 escape hatch.
>
> Text below is the original v1.1 ticket, unedited.

**Status:** Deferred · **Owner:** — · **Epic:** UI · **Depends on:** QUI-011
**PRD:** §4.3

### User story
As a reader, I want to see who Quire thinks is in my book and change a voice I dislike,
so that I am not stuck with an assignment that breaks the story for me.

### Context (why)
Auto-casting will sometimes be wrong or simply not to taste, and being unable to fix it
sours the whole book. This is the escape hatch — deliberately optional and minimal, so
it never becomes required setup.

### Description (what)
A slide-out panel listing detected characters with their assigned voice, letting the user
preview and change a voice, adjust pitch and tone, and reset to the automatic choice.
Changes apply to upcoming audio without restarting playback.

### Requirements (how)
- Owns: `app/.../drawer/`, override persistence in `core/tts/casting/`
- Lists characters by line count, showing name, aliases, and current voice.
- Preview plays a short sample in the selected voice; it must not disturb book playback.
- Changes take effect within one paragraph — the buffer re-synthesises affected upcoming
  chunks only, and never re-synthesises what is already played.
- Per-character "Reset to automatic".
- Renders correctly in monochrome e-ink mode: no greys, no animated drawer transition.
- Out of scope: renaming or merging characters, adding characters by hand.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: The drawer lists the cast
  Given a book with a completed cast
  When I open the drawer
  Then I see each character with their assigned voice, most-spoken first

Scenario: Preview a voice
  Given the drawer is open and playback is paused
  When I preview a voice
  Then a short sample plays in that voice

Scenario: Override applies quickly
  Given playback is running
  When I change a character's voice
  Then their next line uses the new voice, within one paragraph

Scenario: Reset restores the automatic choice
  Given I have overridden a character's voice
  When I choose "Reset to automatic"
  Then the originally cast voice is restored

Scenario: Drawer is usable on e-ink
  Given E-ink mode is active
  When I open the drawer
  Then it renders in pure black and white with no transition animation
```

### Worklog
- _(empty)_

---

# Epic: Quality

## QUI-016 — Performance and SLA harness

**Status:** In review · **Owner:** — · **Epic:** Quality · **Depends on:** QUI-010
**PRD:** §5

### User story
As a developer, I want the PRD's performance SLAs measured automatically, so that a
regression is caught by a number rather than by a reader noticing the app got worse.

### Context (why)
Every SLA in PRD §5 is currently an aspiration checked by hand, per ticket, if at all.
Four separate tickets are each meant to record a measurement; without one harness they
will each invent their own method and the numbers will not be comparable.

### Description (what)
A repeatable benchmark suite that measures peak RAM, app footprint, real-time factor,
time to first sound, and battery drain per hour, against fixed fixtures, and reports the
numbers with a pass/fail against the SLA table.

### Requirements (how)
- Owns: `benchmarks/`, `docs/performance.md`, and (the sustained-power harness specifically)
  `spike/ttsbinding/src/main/java/quire/spike/tts/SustainedRun.kt` plus its wiring in
  `MainActivity.kt` and `AndroidManifest.xml` — it lives next to `Benchmark.kt` because it
  needs the same loaded `TtsEngine`, not a second copy of it.
- Fixtures: one standard novel EPUB and one standard 10 s synthesis text, both committed
  (or fetched by script if over the size limit) so runs are comparable over time.
- Measures, from PRD §5: peak RSS ≤ 1.2 GB across a full read-plus-playback session;
  installed footprint ≤ 450 MB; RTF ≤ 0.15; TTFS < 800 ms; battery drain < 8%/hour.
- Runs on a physical mid-tier ARM device; the device model is recorded with every result.
- Output is a committed markdown report in `docs/performance.md` with date, device,
  commit and each measurement against its target.
- Battery measurement may be a documented manual procedure if automation is impractical
  — but the procedure must be written down and repeatable.
- Out of scope: gating CI on these numbers (needs a device lab; revisit later).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: The suite runs and reports
  Given a connected mid-tier ARM device
  When I run the benchmark suite
  Then it reports peak RAM, footprint, RTF and TTFS with pass or fail against each SLA

Scenario: Results are comparable across runs
  Given two runs on the same device and commit
  When I compare their reports
  Then the measurements agree within 10%

Scenario: A regression fails loudly
  Given a build whose RTF is 0.25
  When the suite runs
  Then the RTF check reports a failure naming the target of 0.15

Scenario: Battery procedure is documented
  Given docs/performance.md
  When I read the battery section
  Then it gives a step-by-step procedure another person can repeat
```

### Worklog

**2026-09-06 — qui-016-sustained-power-harness.** Built the sustained-power *harness*,
not the measurement — that still needs the reference device, which this session doesn't
have. Scope was narrowed to that on purpose; the rest of this ticket (peak RSS, footprint,
`benchmarks/`, `docs/performance.md`) is still open and untouched.

**Why a burst can't answer this.** `Benchmark.kt` synthesises one ~10 s fixture — useful
for RTF, useless for battery. ADR-0002 §1 already names the reason: at RTF 0.354 the
engine synthesises a page in a third of the time it takes to play, then goes quiet until
the next page is due (ADR-0004's paging). A 10 s burst measures the *busy* fraction of
that cycle; PRD §5 wants drain over an *hour of continuous playback*, idle stretches
included. So the harness reproduces the duty cycle for real — synthesise a page, sleep
for what's left of that page's audio, repeat — for up to 60 minutes, with the engine
loaded once and never reloaded.

**What landed**, all in `spike/ttsbinding`:
- `SustainedRun.kt` — loads the selected engine once, then loops
  `Benchmark.FIXTURE` through it for up to 60 minutes on that duty cycle. Refuses to
  start if the device is on charge, aborts mid-run if a charger is connected (both make
  the reading meaningless), holds a `PARTIAL_WAKE_LOCK` so a screen timeout doesn't let
  the CPU sleep under the measurement, and samples battery percentage once a minute via
  the sticky `ACTION_BATTERY_CHANGED` broadcast (works from API 1; `BatteryManager`'s
  newer property API needs API 28, above this probe's minSdk 26). Writes a per-minute
  TSV (`quire-sustained-<timestamp>.tsv`) to both the app's private directory and, best
  effort, the device's Downloads folder — same collection story as `QuireProbeService`'s
  existing log, so there's one place to look for both.
- `MainActivity.kt` — two buttons under the existing benchmark row: "Run sustained
  synthesis (60 min)" (runs against whichever engine is currently selected) and "Stop"
  (winds down at the next chunk boundary rather than killing the run mid-chunk).
- `AndroidManifest.xml` — added `WAKE_LOCK`, the only new permission needed.
- Verified by building: fetched the SDK and the sherpa AAR in this session
  (`dl.google.com` was reachable) and ran `cd spike/ttsbinding && ../../gradlew
  assembleDebug` — `BUILD SUCCESSFUL`. That confirms the Kotlin compiles and packages;
  it says nothing about runtime behaviour, which needs the device.

**The device procedure** — read this before running it, and follow it exactly or the
number isn't comparable to anyone else's:

1. **Charge state.** Unplug the device. Start at 90–100% if you can — a 60-minute run
   at an 8%/hour SLA only drains ~8 points, and `device-profile.md`'s own rule discards
   any measurement taken below 30%, so starting high leaves margin if the run overshoots
   or needs a retry. Confirm in Settings → Battery that "Charging" is not shown.
2. **What to disable, before opening the probe:**
   - Wi-Fi and Bluetooth off (radios are not what this measures, and Wi-Fi's periodic
     scans are a confound).
   - Sync / background app refresh off, or airplane-mode-then-Wi-Fi-off if the launcher
     makes that easier — no account sync waking the CPU mid-run.
   - Do Not Disturb on, so a notification doesn't wake the screen or pull a network hit.
   - Auto-rotate and adaptive brightness off — irrelevant to synthesis power, but a
     panel-driven confound worth removing since it's free to.
   - Leave the probe as the foreground app for the duration; don't lock the screen (the
     wake lock keeps the CPU alive either way, but a screen-off timeout partway through
     is one more variable to control for rather than rely on).
3. **How to start it.** Build and install the probe (this session's build command
   above), pick **Piper libritts_r (medium, 904 voices)** — that's the accepted engine,
   ADR-0002 — as the selected engine (Download, then "Use this" if not already
   installed), then tap **"Run sustained synthesis (60 min)"**. The on-screen log prints
   one line per minute (`min=N battery=NN% chunks=N rtf=0.NNN`); if it stops printing or
   shows a `battery=` value that isn't dropping over 10+ minutes, something's gone wrong
   — re-check step 2 rather than trusting the run to the end.
4. **How long.** 60 minutes, matching PRD §5's "per hour". Let it run to completion
   rather than stopping early — the summary flags anything under 55 minutes as an
   extrapolation, not a measurement, and `device-profile.md`'s "discard below 30%"
   rule means starting over costs less than arguing about a partial number later.
5. **How to read the drain.** The probe computes it for you at the end: a summary line
   giving start/end battery %, points drained, and `%/hour` against the 8%/hour SLA
   (PASS or FAIL), plus the path to the full per-minute TSV in Downloads for anyone who
   wants the trace rather than the summary. Cross-check the two start/end percentages
   against Settings → Battery's own history as a sanity check — they should agree to
   within a point or two; if they don't, something (a background sync, a radio waking)
   moved the number and the run should be discarded and retried after re-checking step 2.
6. Record the device model, Android build, starting battery %, and the resulting
   `%/hour` number back into this Worklog (or `docs/performance.md` once QUI-016's other
   half exists), same as every other measurement in this file. This is ADR-0002's
   revisit trigger (§1, §7, "Revisit trigger"): if drain at RTF 0.354 fits inside
   `device-profile.md` §4's ≈1.14 W budget, PRD §5's RTF SLA gets re-derived from this
   number and the ADR stands as written; if it doesn't, the product changes instead —
   fewer voices, or synthesis paced further ahead of playback — because §8–§9 already
   closed off every faster engine.

**What's left:** the actual hour on the reference device, and folding this into the rest
of QUI-016 (peak RSS, footprint, `benchmarks/`, `docs/performance.md`) rather than leaving
it a one-off harness. Marking `In review`, not `Done` — the harness is finished, but its
whole purpose is a number only the device can produce.
- _(empty)_

**2026-09-07 — do not spend a device hour on this yet.** The harness above is ready and the
procedure is written, and running it now would still be close to wasted. Recorded here so
the next session does not hand the same hour to the one person who can spend it.

**The number would be superseded.** What the probe measures today is TTS-only draw. What
ships has an SLM co-resident for attribution, and on a 750G without i8mm that model is
likely the larger consumer, not Piper. Measuring a configuration that never ships means
measuring twice.

**And it cannot change a decision today.** ADR-0002 §9 exhausted the search for a faster
multi-speaker engine, so the ADR's own answer to a power failure is that the engine stays
and the *product* changes — fewer distinct voices, or synthesis further ahead of playback.
Nothing is currently built against that, so a failing number unblocks nothing and a passing
one confirms nothing anyone is waiting on.

**Take it when the SLM is co-resident** (QUI-031), which is the configuration whose power
actually matters, and take it once.

*One exception, and it is cheap.* A **ten-minute** run answers a different and useful
question: is Piper alone anywhere near ≈1.14 W? If the engine on its own already blows the
budget then adding an SLM is hopeless, and that is worth knowing before more is built on
the assumption. Ten minutes is an order-of-magnitude read, not an SLA number — the summary
correctly flags anything under 55 minutes as an extrapolation, and that is the right label
for it.

---

# Epic: Spike

> Spike tickets are timeboxed and exist to produce a **decision**, not a feature. Their
> code may be throwaway; their measurements and ADRs are not. See
> [`docs/architecture.md`](docs/architecture.md) §8.

## QUI-017 — TTS engine bake-off on target hardware

> **Narrowed to TTS, 2026-08-29.** This ticket was written to bake off the SLM *and* the TTS
> engine together, because co-residency was thought to couple them. The TTS half is
> finished and ADR-0002 is accepted; the SLM half has not started and is blocked on QUI-006
> having a runtime at all. Splitting rather than holding a settled decision hostage — the
> SLM scenarios moved verbatim to **QUI-031**.

**Status:** Done · **Owner:** session-visibility-check · **Epic:** Spike · **Depends on:** —
**PRD:** §3.1, §3.2, §5 · **Timebox:** 3 days

### User story
As a team, I want measured numbers for the candidate SLM and TTS models on a real e-ink
device, so that we choose a stack on evidence instead of picking one and discovering in
month two that it cannot hit the SLAs.

### Context (why)
Every performance target in PRD §5 currently rests on an assumption, and two of the
biggest architectural questions — which runtime, and whether the SLM and TTS can be
co-resident (`docs/architecture.md` §4) — cannot be answered by reading documentation.
This is the cheapest possible experiment that can invalidate the product, so it runs
first and everything else waits on it. Nothing built before this measurement is safe.

### Description (what)
A throwaway harness, installed on a real mid-tier e-ink Android device, that loads each
candidate model, runs a fixed workload, and reports latency, throughput and peak
resident memory. The output is three ADRs and a table of numbers.

### Requirements (how)
- Owns: `spike/bakeoff/`, `docs/adr/0001-slm-runtime.md`, `docs/adr/0002-tts-engine.md`
- Target device is the Onyx Boox Note Air5 C (`docs/device-profile.md`).
- SLM candidates: Llama 3.2 1B and Qwen 2.5 1.5B, Q4_K_M, via `llama.cpp` JNI and via
  ExecuTorch. Measure: load time, peak RSS, and **prompt-eval tokens/s separately from
  generation tokens/s** — on this SoC prompt eval dominates, so one blended number hides
  the answer.
- Measure the **KV-cache reuse factor**: time to attribute 50 consecutive dialogue lines
  with a fresh context window each, versus one rolling chapter context. That ratio decides
  whether QUI-007's 30-minute scan budget is reachable at all
  (`docs/architecture.md` §5).
- Measure **sustained power draw** in mW for each configuration, not only memory. The
  device budget is ≈1.14 W total during playback.
- TTS candidates: Kokoro-TTS (82M ONNX) and Piper C++. Measure: RTF on a fixed 10 s
  text, peak RSS, on-disk size, number of usable voice variants, and whether word
  boundary timestamps are obtainable **without** post-hoc alignment.
- Co-residency: measure each model's peak RSS separately. Under PRD v1.2 the SLM lives in
  the companion app and the TTS engine in the service process, so they are never resident
  together and ADR-0003 shrinks to "does each process fit its own budget". Record the
  combined figure anyway — V3.0's built-in reader would put them back in one process.
- Run on the Note Air5 C; record Android build and starting battery level with every
  result, and discard runs started below 30% battery.
- Each ADR states the alternatives, the measurements, the choice, and what would make us
  revisit it.
- **Never hand a native API a path that has not been confirmed to exist.** Model archives
  name their weights after the voice (`en_GB-alan-low.onnx`), not predictably, and an
  unfound file becomes an empty string, which sherpa-onnx turns into a SIGSEGV that no
  `runCatching` can catch. Discover files by scanning; validate before loading; log what
  was resolved.
- Out of scope: any production code, any UI, cloud engines.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Every TTS candidate is measured
  Given the bake-off harness on a mid-tier e-ink device
  When it runs to completion
  Then it reports RTF, peak RSS, disk size and voice count for each TTS candidate

Scenario: Boundary timestamps are proven, not assumed
  Given the chosen TTS candidate
  When a 10 second text is synthesised
  Then word boundary timestamps are emitted by synthesis itself
  And a named word's timestamp matches its position in the audio within 50 ms

Scenario: Decisions are recorded
  Given the bake-off has run
  When I read ADR-0002
  Then it names its alternatives, its measurements, the decision, and its revisit trigger

Scenario: A candidate that fails is reported, not worked around
  Given a candidate that misses its SLA on the target device
  When results are written up
  Then the ADR states the miss plainly rather than proposing a heavier device
```

### Worklog

**2026-08-28 — claude-opus-5.** Candidates measured on the reference device (Onyx Boox
Note Air5 C, Snapdragon 750G) through the probe APK in `spike/ttsbinding`. Reproduce:
build and install the probe, pick a model, tap Download, then Benchmark; the numbers below
are what it prints.

*Measured — Piper `libritts_r` medium, 2 threads:*

```
load        2524 ms
synthesis   4595 ms for 12979 ms of audio
RTF         0.354   FAIL (> 0.15)
peak RSS    314 MB
on disk     92 MB
voices      904 at 22050 Hz
```

| Threads | RTF |
| --- | --- |
| 2 | 0.354 |
| 4 | 0.370 |

Four threads is slightly *worse*. The 750G has two performance cores and scheduling onto
the six efficiency cores costs more in coordination than it returns, so thread tuning is a
closed avenue and RTF ≈ 0.35 is what this model does on this chip.

Qualitatively: Kitten nano fast but unacceptable; `libritts_r` "almost perfect" and the
leading candidate; Kokoro too slow on this SoC. Kokoro int8 was dropped by decision rather
than measured — reasoning in ADR-0002.

*What is left before this is Done:*

1. **The alan LOW number**, which is the only thing still separating two very different
   worlds: near 0.10 means the hardware is fine and `libritts_r` medium is merely heavy, so
   a smaller multi-speaker model (VCTK, 109 voices) is worth chasing; near 0.30 means the
   SoC is the ceiling and PRD §5's 0.15 has to be re-derived from a power measurement
   instead of defended. Both models are already in the probe's list.
2. **TTFS end to end with the engine preloaded**, against 800 ms. Cold load alone is
   2,524 ms, so the engine must be warm before the first utterance either way — that is
   QUI-010 and QUI-012's problem, but the number belongs here.
3. **Sustained power draw** (QUI-016), which is what RTF was ever standing in for.

ADR-0002 stays `Proposed` until 1 and 2 exist.

**2026-08-29 — claude-opus-5.** Device: `alan-low` measures **0.132 PASS**, against 0.354
for `libritts_r` medium. The 750G is not the ceiling; medium quality at 22.05 kHz is what
is expensive. Piper publishes no multi-speaker model at the low tier, so the two things we
want are not in one file.

Added `spike/hostbench/`, which runs the same `sherpa-onnx` runtime on the build host.
Reproduce with `spike/hostbench/fetch-models.sh` then
`python3 spike/hostbench/bench.py --paired vctk libritts_r`.

*Measured (host, x86, 2 threads, interleaved, seven runs each):* `vctk-medium` 0.0566
against `libritts_r-medium` 0.0569 — **ratio 0.995, ranges overlapping**. A lighter medium
multi-speaker model is not available; `vctk` is the same engine on a different corpus and
would trade 904 voices for 109 to no purpose. Written up in ADR-0002 §7.

The host screen has a stated limit, recorded because it will be tempting to over-trust it:
same-tier ratios transfer, cross-tier ratios do not. The host puts `alan-low` at 0.72 of
`libritts_r-medium`; the device measured 0.37. Whatever the 750G does to medium-tier
22.05 kHz synthesis, this machine does not do it.

**2026-08-29 (later) — claude-opus-5.** Screened the last two candidates on the host.
Reproduce with `spike/hostbench/fetch-models.sh` then `python3 spike/hostbench/bench.py`.

A low-tier VCTK does not exist: `en_GB-vctk-low`, `en_US-vctk-low` and `libritts-low` all
404 in the model zoo. Screened the nearest substitutes instead.

*Measured (host, 2 threads, relative to `libritts_r-medium` at 0.065):* `vits-vctk` 0.369
(5.7x), Kokoro fp32 0.607 (9.4x), **Kokoro int8 1.493 (23x)**. Kokoro int8 is 2.46x slower
than the identical unquantized model — quantization made it worse, not better, which is a
stronger reason to drop it than the one recorded on 2026-08-28.

Piper is 6–23x ahead of every other multi-speaker engine in the zoo. **The search for a
faster multi-speaker model is exhausted** — there is no model left to find, and §7's
finding means the alternatives get worse on device, not better. Written up in ADR-0002 §8.

**2026-08-29 (close) — claude-opus-5.** ADR-0002 accepted: Piper `libritts_r` medium on
`sherpa-onnx`, 904 voices in 92 MB. Accepted **with a recorded deviation, not a pass** —
RTF 0.354 misses PRD §5's 0.15 by 2.4×, and we proceed because §8 exhausted the search for a
faster multi-speaker engine, so blocking on the number would block the product without
improving it. QUI-016's power measurement is the revisit trigger; if it fails, the engine
stays and the product changes, because there is nothing faster to move to.

Ticket narrowed to TTS and closed. Two things it was carrying go elsewhere rather than
being quietly dropped: **TTFS** with a preloaded engine is already a QUI-019 acceptance
criterion and is measured there; **sustained power** is QUI-016. The SLM half of the
original bake-off is now QUI-031.

---

## QUI-018 — Headless end-to-end pipeline spike

**Status:** In progress · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-017 (partial)
**PRD:** §3 · **Timebox:** 4 days

> Started ahead of QUI-017 on the Tier 1 half only, which needs no model and therefore no
> device. The Tier 2/3 half stays blocked on ADR-0001.

### User story
As a team, I want a command-line run that turns an EPUB into a multi-voice wav file, so
that we can judge whether the attribution is actually any good before spending weeks on
an app around it.

### Context (why)
The hardest question in Quire is not "does audio play" — it is "does the system put the
right voice on the right line often enough that a reader isn't jarred". That is a
quality question, and quality questions need iteration loops measured in seconds. A
desktop JVM harness gives us that; an Android build does not. It also produces the
labelled fixture set that QUI-008, QUI-009 and QUI-016 all need.

### Description (what)
A JVM command-line tool that takes an EPUB and a chapter number, runs parse → scan →
Tier 1 → Tier 2/3 → casting → synthesis, and writes a wav file plus a human-readable
transcript of who was assigned to each line and why. Plus a small labelled fixture set of
chapters with hand-annotated speakers, and a scoring command that reports attribution
accuracy against it.

### Requirements (how)
- Owns: `spike/pipeline/`, `fixtures/attribution/`
- Runs on desktop JVM against public-domain EPUBs; no Android, no device required.
- Transcript output per line: text, assigned speaker, confidence, **tier**, and for
  Tier 2 the context window that was used. Debuggability is the point of this ticket.
- Fixture set: at least 3 chapters from different books, hand-labelled with the correct
  speaker per line, including one heavy untagged back-and-forth exchange. **Prefer PDNC**
  (35,978 annotated quotations across 22 novels) over hand-labelling — it is the benchmark
  the field reports against, so our numbers become comparable to published ones. Check its
  licence first. See `docs/prior-art.md` §3.
- `score` command reports overall accuracy plus a breakdown by tier, so we can see
  whether errors come from heuristics or from the model.
- Code here is explicitly throwaway; where a component is obviously reusable, extract it
  into `core:*` under its own ticket rather than growing this one.
- Out of scope: UI, real-time buffering, on-device execution.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: A chapter becomes audio
  Given a public-domain EPUB
  When I run the pipeline for chapter 3
  Then a wav file is produced in which distinct characters have distinct voices

Scenario: Every decision is explainable
  Given a completed run
  When I read the transcript
  Then each line shows its speaker, confidence and tier
  And each Tier 2 line shows the context window used

Scenario: Accuracy is scored, not eyeballed
  Given the labelled fixture set
  When I run the score command
  Then it reports overall speaker accuracy and a per-tier breakdown

Scenario: The untagged exchange is the honest test
  Given the fixture chapter with a long untagged back-and-forth
  When it is scored
  Then its accuracy is reported separately from the tagged fixtures

Scenario: Fixtures are reusable downstream
  Given the fixture set
  When QUI-008 and QUI-009 run their tests
  Then they consume these same files without modification
```

### Worklog

**2026-08-27 — claude-opus-5.** Landed the Tier 1 half; Tier 2/3 remain blocked on
ADR-0001. Reproduce with:

```bash
cd spike/pipeline && gradle installDist test
build/install/quire-pipeline-spike/bin/quire-pipeline-spike score ../../fixtures/attribution/*.tsv
```

*Measured, on the hand-written fixtures (36 dialogue spans):*

| | coverage | precision | accuracy |
| --- | --- | --- | --- |
| `tagged.tsv` (classic speech tags) | 83.3% | 100% | 83.3% |
| `beats.tsv` (action beats) | 55.6% | 100% | 55.6% |
| `untagged.tsv` (bare exchange) | 6.7% | 100% | 6.7% |
| **total** | **44.4%** | **100%** | **44.4%** |

**QUI-008 throughput SLA: 99,960 words in 693 ms**, against a 2 s budget — but measured
on an Intel Xeon 2.10 GHz container, *not* the Boox. The reference device is several times
slower single-threaded, so this passes with far less margin than it looks and must be
re-measured on device before QUI-008 can be called done.

*What surprised me.* The roster is the bottleneck, not the patterns. Tier 1 originally
bootstrapped its roster only from explicit speech tags, which meant that in
`beats.tsv` — modern prose that never writes "said Dana" — Dana never entered the roster
and every one of her lines was unattributable. Admitting names that stand beside a quote
at least twice took total coverage 38.9% → 44.4% with precision unchanged. This is direct
evidence for QUI-007: the SLM scan's real job is supplying a roster Tier 1 cannot infer,
more than supplying traits.

*Precision held at 100% throughout.* Every rule declines rather than guesses, and
unresolved spans record *why* (`pronoun speech tag`, `tag names unknown 'Gregor'`), which
is a better input to Tier 2 than a blank. Worth keeping as a rule: a confidently wrong
voice is worse for a listener than the narrator taking a line.

*A structural finding for QUI-010/QUI-011.* A paragraph routinely contains narration plus
two speakers, so the unit that gets a voice must be finer than the paragraph. The spike
introduces `Segment` with locators of the form `chapter#p3#s1`. `docs/architecture.md` §2
still holds for scheduling and synthesis; it now names the exception.

**2026-08-28 — scored against PDNC, and the hand-written numbers do not survive.**
Reproduce: clone PDNC, then
`quire-pipeline-spike pdnc <pdnc>/data/Emma <pdnc>/data/TheSignOfTheFour …`

> **Superseded 2026-09-02 by QUI-028.** The scorer behind this table matched gold
> quotations to predicted segments by text, and so measured 2,846 of 37,131 — 7.7% of PDNC,
> self-selected for being short and cleanly punctuated. Re-scored against PDNC's byte spans
> over all 28 novels, Tier 1 is **26.8% coverage at 84.9% precision**. The table below is
> kept as the record of what was believed; do not quote it.

Measured over 2,846 matched quotations from five novels:

| Quote type | Coverage | Precision | Accuracy |
| --- | --- | --- | --- |
| Explicit | 91.7% | 68.6% | 62.9% |
| Anaphoric | 21.5% | 11.9% | 2.6% |
| Implicit | 4.8% | 10.7% | 0.5% |
| **All** | **39.4%** | **58.5%** | **23.0%** |

**The 100% precision I reported on 2026-08-27 was an artefact of writing my own fixtures.**
On real novels Tier 1 is right about 59% of the time when it commits to a speaker — and on
quotations that carry no explicit tag it is right about one time in nine. It is not
declining to guess; it is guessing and losing.

Disabling the action-beat rule (`--no-beats`) trades accuracy for precision: 23.0% → 15.0%
accuracy, 58.5% → 64.8% precision. So beats earn their keep on explicitly tagged lines and
do damage everywhere else. Keeping them, for now, but see below.

**The finding that matters: our confidence numbers are fiction.** `EXPLICIT_TAG = 0.95`
against a measured 68.6% precision, and `ACTION_BEAT = 0.75` against roughly 11% on
untagged material. QUI-009's gates (`SLM_MIN = 0.65`, `NARRATOR_FLOOR = 0.40`) assume
calibrated confidences and will misbehave on these. Calibrating them against PDNC is now
part of QUI-008.

*Caveat.* Only 2,846 of roughly 10,000 quotations in these five novels matched our
segmentation by text, so this is a sample rather than a census, and alias mismatches
between PDNC's canonical names and our roster's tag-derived names may understate Explicit
precision. Both are fixable; neither changes the direction.

*What is left.* Tier 2/3 (blocked on ADR-0001). No wav yet; that needs ADR-0002. Confidence
calibration. The ticket stays `In progress`.

---

## QUI-019 — Vertical slice: NeoReader Read Aloud in three voices

**Status:** In review · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-020, QUI-021, QUI-022, QUI-024
**PRD:** §1, §2 · **Timebox:** 1 week

> Rewritten for PRD v1.2. The v1.1 version of this ticket was a standalone player on a
> bundled book; under v1.2 the whole point is that the audio comes out of somebody else's
> reader.

### User story
As a stakeholder, I want to open a book in NeoReader, press its own Read Aloud button, and
hear it performed in three voices, so that we can judge the actual product experience
before committing to the full build.

### Context (why)
QUI-020 proves the pipe exists and QUI-017 proves the models are fast enough. This is the
first time they meet, and it is the first moment anyone can tell whether the illusion holds
— whether multi-voice audio arriving through a reader you did not write feels like a
feature of that reader or like a hack bolted underneath it. Deliberately narrow: one
pre-indexed book, no companion UI, no fingerprinting.

### Description (what)
An installable debug build registering the real TTS service, shipping a pre-built index for
one book, that plays that book in NeoReader with a narrator and two character voices, with
the sentence highlighted if NeoReader honours `rangeStart`.

### Requirements (how)
- Owns: `app/ttsservice/src/debug/` slice sources; consumes `core:index`, `core:tts`
- The index is **pre-built and shipped with the build** — no SLM runs on device in this
  ticket, and the book is hardcoded rather than fingerprinted. That isolates the listening
  experience from indexing performance.
- Must run on the physical Note Air5 C in monochrome mode. Record measured TTFS and peak
  RSS of the service process in the Worklog, against the 800 ms and 1.2 GB budgets.
- Hardcoding is expected. Anything discovered here that must survive becomes a ticket, not
  a quiet addition to this one.
- Out of scope: the companion app, book identification, indexing, voice overrides.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: A chapter performs in NeoReader
  Given the slice build installed and selected as the TTS engine
  When I open the indexed book in NeoReader and press Read Aloud
  Then the chapter is read with a narrator and two distinct character voices

Scenario: Mixed paragraphs switch voice mid-chunk
  Given a paragraph containing narration and two speakers
  When it is read aloud
  Then the voice changes within that paragraph, in the right order

Scenario: It starts fast enough to feel instant
  Given the slice build on the reference device
  When I press Read Aloud
  Then the first audio frame is emitted within 800 ms
  And the measured value is recorded in the Worklog

Scenario: It stays inside the memory budget
  Given a full chapter plays to its end
  When peak resident memory of the service process is measured
  Then it is recorded against the 1.2 GB ceiling

Scenario: Page turns do not break it
  Given playback is running
  When I turn the page in NeoReader
  Then audio continues from the right place in the right voices
```

### Worklog

**2026-08-29 — claude-opus-5.** Started with the index rather than the audio, on the
reasoning that tuning voice switching is wasted while the speaker data underneath it is
absent. Reproduce with `gradle :spike:indexer:test` from the repository root.

Added `spike/indexer`, which plays the companion app's role: it builds a real
`dialogue_index.db` with `core:index`'s own `IndexWriter`, so what ships to the device is
what the service reads rather than a stand-in. It has a `replay` command that feeds a
captured host trace through the matcher and prints the voice each chunk resolves to.

Added `fixtures/host-traces/neoreader-epub-shape.labels.tsv`: gold speaker labels for the
same scene as the existing trace, as whole sentences. The pairing is what makes the slice
checkable without a device — the trace is what the host *sends*, the labels are what the
book *says*, and the index is the thing that has to connect them.

*Measured:* replaying the 15-chunk trace against the 8-entry index resolves **14 of 15**.
The miss is `"1"`, a page number the host emitted between the heading and the prose; it is
not in the book, so falling through to the narrator is the correct answer. One RELOCATED to
find the opening heading, then the cursor carries the remaining chunks forward.

The result that matters:

```
FORWARD   " avoiding the letter,"    Sarah
FORWARD   " she said."               Sarah
```

Neither clause contains a quote mark. Both are attributed to Sarah, because the speaker
comes from position in the index rather than from evidence in the chunk. That is the
failure recorded in ADR-0002 §6 — observed on device, twice, in both directions — not
patched but structurally absent. Three tests in `SceneReplayTest` hold it there.

*Known gap, and it lands on this ticket.* `MatchResult.partial` is true for most chunks,
because hosts stop inside a sentence. Spans then cover the whole entry rather than the part
spoken, so a partial chunk can be given the *right* voice but cannot yet be split into
several voices at the right character offsets. That needs QUI-027's normalised-to-raw
offset map. For the slice, a partial chunk takes its first span's voice — audibly right for
the case in this scene, and wrong for a chunk that straddles a speaker change mid-clause.

**2026-08-29 (later) — claude-opus-5.** All three pieces written. Reproduce with
`gradle test` from the repository root: **43 tests, 0 failures**.

*Casting and clipping went into `spike/slice`, deliberately not into the Android module.*
That is where the bugs live, and there they are testable on a desktop in seconds rather
than on a device by ear. `Casting` is QUI-011 in miniature — a deterministic speaker-id to
voice-index map that spreads voices apart, because adjacent ids in `libritts_r` are
neighbouring readers and sound alike. `ChunkPlan` cuts a chunk into voiced segments.

*`OffsetMap` is QUI-027, prototyped.* Clipping needs normalised-to-raw offsets, which the
index does not store, so the slice reconstructs them locally. It is one small class on
purpose: QUI-027 lifts it into `core:index` and deletes this one. Its own test asserts the
walk reproduces `Normalizer.normalize` exactly, and that assertion **caught a real bug** —
`normalize(" ")` returns empty because the function trims, so a naive per-character walk
drops every space in the book.

*The case that decides whether any of this works* is a chunk like `" she said."` arriving
after a line of speech. Its match carries both spans, so voicing it by the first span reads
the speech tag in the character's voice. Clipping gets it right, and
`ChunkPlanTest` pins it.

*Measured, on the slice's own book* (`indexer read fixtures/slice/chapter-one.labels.tsv`):
36 host chunks, cast `{Sarah=452, Thomas=903}` against narrator 0. **Five chunks contain no
quote mark and are still correctly attributed** — `"quite still,"`, `"for a quarter of an
hour,"`, `"on and off,"`, `"since breakfast,"`, `"in this house,"`. Those five are exactly
what quote-mark inference reads in the wrong voice. One chunk,
`" he said. "It is the answer to it.""`, changes voice *within itself*, which is the
QUI-024 mechanic.

*The book is generated, not sourced.* `fixtures/slice/chapter-one.labels.tsv` produces both
the EPUB the reader opens and the index the service reads, so the two cannot drift and any
wrong voice on device is the matcher's fault rather than the fixture's. It also keeps
CLAUDE.md §8 satisfied — no real book goes near this repository.

**2026-08-29 — device result, dylangrowcoot.** Piper `libritts_r` on the Note Air5 C,
build `0.3-20260829-592073f`, reading `chapter-one.epub` through NeoReader's Read Aloud.

**Three distinct voices, driven by the index.** That is the architecture working on hardware
for the first time: the speaker comes from position in a pre-built index, not from
punctuation in the chunk, and it survives the host's clause-level chunking.

**The casting is arbitrary, and audibly so.** The narrator is a woman, Sarah is a man,
Thomas is a woman. Not a bug — `Casting` spreads speaker ids across the model's range to
make voices *distinct*, and has no idea what any of them sound like. QUI-005's manifest
carries `gender` precisely for this and nothing consumes it yet; that is QUI-011.

**2026-08-29 — both confirmed on device, dylangrowcoot.** The character's voice **holds
across a comma** (`quite still,`, `for a quarter of an hour,`), and the speech tag
`" she said.` **drops back to the narrator**.

That is the whole architecture demonstrated on hardware. Both failures ADR-0002 §6 records —
per-chunk inference breaking *within* a line, carried state breaking *across* paragraphs —
are absent, not because a flag was set correctly but because the speaker is looked up by
position in an index and never inferred from the text of a chunk. A reader hears three
voices in a book whose cast the engine was told about, through a reader nobody wrote, with
the host chunking at commas.

Casting was also re-heard after QUI-011's measured voices landed: narrator and Sarah are
both women, 188.5 and 217.2 Hz, and Thomas a man at 111.9 Hz.

**Page turns do not break it** — confirmed on device the same session. The cursor survives
the host re-entering mid-book, which was the scenario most likely to expose the matcher's
relocation path in anger.

*Three of five acceptance scenarios are therefore confirmed on hardware.* The two left are
measurements rather than behaviours: **TTFS against 800 ms** and **peak RSS against
1.2 GB**. The probe now records both — time from `onSynthesizeText` entry to the first
frame handed back, and `VmHWM` from `/proc/self/status`, which is the kernel's own
high-water mark and so survives the collector having already given memory back. They land
in the TSV the probe drops in Downloads, two columns before `text` so existing replay
tooling still reads the last column as the chunk. Nobody has run a session with that build
yet; the numbers cost one reading, not one ticket.

*Two installation findings, the second expensive.* The screen is a `ScrollView` and the
build stamp sits above the fold, which is easy to miss. And the first container-built APK
was signed with a freshly generated per-machine debug key, so it could not install over the
previous build — the uninstall wiped app storage and took every downloaded model with it.
Fixed by committing a shared debug key; a build from any machine now installs as an update.

*Not verified: everything Android.* `AndroidSql`, `SliceIndex` and the rewritten
`QuireProbeService.speak()` are **written but never compiled** — this container has no
Android SDK. Writing them already surfaced one error a compiler would have caught in a
second (`NARRATOR_VOICE` and `TAG` live in a *private* companion object and were referenced
from another file), so expect more of that shape on the first build.

*What is left:* build the APK, side-load `build/slice/chapter-one.epub`, and listen. Then
QUI-019's remaining criteria, none of which can be taken here: TTFS against 800 ms, peak RSS
against 1.2 GB, and page turns.

**Released `In review`, unowned, 2026-08-29.** "Review" here means device verification, not
code review — the desktop half is tested and the Android half has never been compiled.
Whoever picks this up should expect compiler errors in `AndroidSql`, `SliceIndex` and
`QuireProbeService` first, and should not read a clean `gradle test` as evidence that the
probe builds: no module in the root build compiles against the Android SDK.

---

# Epic: Index

> The seam between the two processes of PRD v1.2. The companion app writes; the TTS
> service reads. Freeze this before fanning out — it is to v1.2 what `characters.json`
> was to v1.1 (`docs/architecture.md` §1).

## QUI-021 — Dialogue index schema and store

**Status:** In review · **Owner:** quire-setup-docs · **Epic:** Index · **Depends on:** QUI-001
**PRD:** §2 Phase 1

### User story
As a developer, I want a frozen on-disk index format, so that the companion app and the
TTS service can be built in parallel by different agents without either guessing at the
other's format.

### Context (why)
v1.2 splits Quire into two programs that never run together and communicate only through
files. The index is the whole contract. Four downstream tickets (QUI-022, QUI-023,
QUI-025, QUI-012) consume it, so it lands early and small, exactly as `characters.json`
did under v1.1.

### Description (what)
A SQLite schema holding, per book, the ordered sequence of segments with their normalised
text, hash, attributed speaker and confidence; a writer used by the companion app; and a
read-only reader used by the TTS service. Ships with a committed example database and the
normalisation function both sides share.

### Requirements (how)
- Owns: `core/index/`, `docs/schema/dialogue-index.md`
- Granularity is the **sentence** — hosts segment on terminal punctuation (ADR-0004).
- Table `entries`: `book_id`, `seq` (dense, 0-based, reading order), `text`, `normalized`,
  `head`, `chapter`. Indexes on `(book_id, seq)` and `(book_id, head)`.
- Table `spans`: `book_id`, `seq`, `start`, `end`, `kind` (`narration|dialogue`),
  `speaker_id`, `confidence` — the voiced runs within an entry, because one sentence can
  need two voices.
- Table `prefixes`: `book_id`, `words` (1..`Normalizer.HEAD_WORDS`), `prefix`, `seq` —
  one row per entry per prefix length, indexed on `(book_id, prefix)`. This is the
  relocation key and the matcher probes it with exact equality only. A *single* fixed-width
  head column does not work: a short entry has fewer words than the key, and a short chunk
  cannot produce the key of the longer sentence it starts. Measured cost is roughly six
  rows per entry; verify the 5 MB budget still holds.
- Implement `quire.index.BookIndex` (QUI-022 defines it); `InMemoryBookIndex` is the
  reference behaviour and the tests to match.
- Table `books`: `book_id`, `title`, `author`, `segment_count`, `indexed_at`,
  `schema_version`, `source_digest` (SHA-256 of the EPUB, so a re-import is detectable).
- **Normalisation lives here**, used by writer and matcher alike, so the two can never
  drift: NFKC, lowercase, strip quote marks and footnote markers, collapse whitespace,
  drop soft hyphens. Changing it bumps `schema_version` and invalidates indexes.
- The service opens the database **read-only**. One writer, one reader, no locking.
- Size budget: under 5 MB for a 100k-word novel. Record the measured size.
- Out of scope: matching (QUI-022), building the content (QUI-025).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Round-trip through the store
  Given a sequence of attributed segments
  When they are written and read back
  Then the sequence, speakers and confidences are identical and in order

Scenario: Writer and matcher normalise identically
  Given a paragraph containing typographic quotes, a soft hyphen and a footnote marker
  When it is normalised by the writer and by the matcher
  Then both produce the same string

Scenario: The service cannot corrupt the index
  Given the TTS service has the database open
  When it attempts a write
  Then the write fails and the service continues serving audio

Scenario: A re-imported book is detected
  Given a book indexed from one EPUB file
  When a different EPUB of the same title is imported
  Then the differing source digest is reported rather than silently merged

Scenario: Size budget
  Given a 100,000 word novel
  When it is indexed
  Then the database is under 5 MB
```

### Worklog

**2026-08-28 — claude-opus-5.** Implemented in `core/index/`. Reproduce with
`gradle :core:index:test` from the repository root. 30 tests pass across the module.

*Measured:* a 100,000-word novel (8,888 sentence entries) indexes to **2,412 KiB**, against
the 5 MB budget.

It did not start there. The first honest measurement was **7,588 KiB — over budget** — and
the scenario failing is what found it. Three fixes, in order of how much they bought:

1. **`WITHOUT ROWID` on every table, and delete the redundant index.** Each table has a
   natural composite key, so the default layout stored the data once in the table and again
   in the primary-key index — and I had additionally created an index on
   `(book_id, prefix_hash)`, which the primary key already begins with, storing the prefix
   data a *third* time. 5,668 → 2,412 KiB.
2. **Hash the prefixes.** Six overlapping cumulative strings per sentence cost more than
   the sentences themselves. FNV-1a 64-bit, written by hand rather than `String.hashCode`
   because the value goes on disk and must be identical on every platform forever.
   Collisions are harmless: a lookup only proposes candidates that the matcher then
   verifies against the text. 7,588 → 5,668 KiB.
3. **Stop storing normalised text**, recomputing it on read. Cheap beside a disk hit, and
   it makes `Normalizer` part of the on-disk contract — which is what `Schema.VERSION` is
   for.

*Design note.* `core:index` stays pure Kotlin, so the SQL sits behind a small `Sql`/`Row`
port: Android supplies its own implementation over the platform's SQLite, and the tests
supply one over JDBC. The schema and every statement live on this side of the port, so both
platforms run the same SQL rather than reimplementing it. The JDBC driver is
`testImplementation` only and never ships, so it costs nothing against the 450 MB footprint.

*The test that matters most* runs the same chunk sequence through the matcher twice — once
over `InMemoryBookIndex`, once over SQLite — and asserts identical results, cursor and
offset. That is what makes the in-memory stand-in trustworthy as the reference behaviour.

*What is left before this is Done:* an Android `Sql` implementation, which arrives with the
Android modules QUI-001 still owes; and the size figure re-measured with real prose rather
than four repeating sentences, which compress better than a real novel will.

---

## QUI-022 — Text normalisation and cursor matcher

**Status:** In review · **Owner:** quire-setup-docs · **Epic:** Index · **Depends on:** QUI-021
**PRD:** §2 Phase 2

### User story
As a listener, I want the right character's voice on each line even when the same words
are spoken by different people, so that a rapid exchange does not dissolve into one voice.

### Context (why)
`onSynthesizeText` hands over a bare string with no book, chapter or position. The PRD
answers this with `hash(text) -> speaker`, but text → speaker is many-to-many: `"Well,"`
appears twice with two different speakers in a twenty-line fixture, and novels are dense
with bare `"Yes."` and `"I know."`. A hash lookup answers confidently and wrongly on
exactly the back-and-forth this product exists to voice. Reading is sequential, so a
cursor resolves those collisions for free; the hash becomes the recovery path after a
seek. Decided 2026-08-27; see `docs/architecture.md` §3.

### Description (what)
A matcher that takes an incoming text chunk and returns the ordered index entries it
covers. It tracks a cursor through the book, advances it on each match, tolerates skipped
headings, and relocates by hash when the reader jumps.

### Requirements (how)
- Owns: `core/index/match/`
- State machine: `Unlocated → Locating → Locked ⇄ Relocating`
  (`docs/architecture.md` §3).
- Forward match tries `cursor+1 … cursor+5` before falling back to hash lookup; a hash hit
  with several candidates picks the one nearest the cursor.
- **A chunk may cover several consecutive segments.** The return type is a list, in order,
  so a paragraph containing narration and two speakers can be voiced correctly.
- **A chunk may be a fragment.** Hosts split at the 4000-character API limit; a prefix or
  suffix match against a segment counts, with the offset returned.
- A miss returns narrator immediately and never blocks. Matching must add under 10 ms to
  an utterance on the reference device — measure it.
- Out of scope: identifying which book (QUI-023), synthesis (QUI-024).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Sequential reading advances the cursor
  Given the matcher is locked to a book at segment 40
  When the text of segment 41 arrives
  Then it matches segment 41 and the cursor advances

Scenario: Identical text with different speakers resolves by position
  Given segments 12 and 87 both normalise to "well"
  And the cursor is at segment 11
  When "Well," arrives
  Then segment 12 is returned, not segment 87

Scenario: A skipped heading does not lose the cursor
  Given the cursor is at segment 40 and the host skips the chapter heading at 41
  When the text of segment 42 arrives
  Then it matches segment 42 and the cursor advances to it

Scenario: A jump relocates by hash
  Given the cursor is at segment 40
  When text belonging to segment 900 arrives
  Then the matcher relocates the cursor to 900

Scenario: One chunk covering three segments
  Given a paragraph of narration, dialogue and narration indexed as segments 10 to 12
  When the whole paragraph arrives as one chunk
  Then the matcher returns segments 10, 11 and 12 in order

Scenario: A fragment matches with an offset
  Given a long segment split by the host at the character limit
  When the first half arrives
  Then it matches that segment and reports the offset covered

Scenario: An unmatched chunk falls back at once
  Given text that appears nowhere in the index
  When it arrives
  Then the matcher returns a narrator result without blocking

Scenario: Matching is fast enough to be invisible
  Given a 100,000 word index on the reference device
  When 500 chunks are matched
  Then the mean added latency per chunk is under 10 ms
```

### Worklog

**2026-08-27 — claude-opus-5.** Implemented in `core/index/`. Reproduce with
`gradle :core:index:test` from the repository root. 18 tests, all passing.

*Measured:* 44 µs mean per chunk over 500 chunks against a 14,284-entry index, against a
10 ms budget — but on an Intel Xeon container, **not** the Note Air5 C. Three orders of
magnitude of headroom means the device will not be the problem, but the number is not the
device's number and the scenario is not yet satisfied as written.

*The design changed because of ADR-0004.* The ticket was written assuming chunks arrive
aligned to something structural. They do not: NeoReader segments on terminal punctuation,
so a chapter heading with no full stop arrives glued to the sentence beneath it. Two
consequences, both now in the code:

1. The index is addressed by **sentence**, not paragraph, or nothing lines up.
2. Matching **consumes by concatenation** rather than comparing sentence lists. My first
   implementation split the incoming chunk into sentences and compared them pairwise; it
   failed the glued-heading test, because splitting the chunk cannot recover a boundary
   the host has already destroyed. Walking entries and eating their normalised text off
   the front of the chunk handles gluing, whole-paragraph chunks and mid-sentence
   fragments with one loop.

*Second thing that surprised me.* Relocation keyed on a fixed six-word head silently
failed for every entry shorter than six words — which is most dialogue. Fixed by probing
progressively shorter prefixes; an implementation still needs only exact-equality lookups,
so QUI-021 can index a stored `head` column.

**2026-08-28 — rebuilt against the real host trace (ADR-0004).** 22 tests pass;
`gradle :core:index:test`. Matching now costs 52 µs per chunk against a 14,284-entry index
(still x86, not the device).

The trace broke two assumptions:

1. **Chunks are interior fragments of a sentence, not whole ones.** The host splits at
   commas, so 42 of 73 chunks ended mid-sentence. The matcher gained an intra-entry
   `offset` beside the cursor. Without it the second clause of every sentence would have
   missed and fallen to the narrator — most of a book.
2. **Relocation cannot key on a fixed-width head.** A failing test showed a two-word chunk
   can never produce the six-word key of the sentence it starts. Entries are now indexed
   under every one-to-six-word prefix, which keeps lookups exact-equality at about six
   rows per entry.

A pleasant accident worth keeping in mind for QUI-011: comma splitting frequently lands
exactly on the dialogue/narration seam, so `"I know," said Sarah.` arrives as `"I know,"`
then ` said Sarah.` — the two voices already separated by the host.

One deliberate limitation: a lost cursor cannot anchor on a mid-sentence fragment, because
it shares its prefix with nothing. It falls to the narrator and re-locks on the next
sentence start — a sentence or two, not a page. Tested.

*What is left before this is Done:* the latency scenario re-measured on device, and
QUI-027, without which `spans` on a partial match covers the whole entry rather than the
part actually spoken. **The SQLite backing landed with QUI-021 on 2026-08-28**, and a test
now runs the same chunks through both `InMemoryBookIndex` and `SqliteBookIndex` asserting
identical results, cursor and offset.

---

## QUI-023 — Book identification by fingerprint

**Status:** In review · **Owner:** — · **Epic:** Index · **Depends on:** QUI-021, QUI-022
**PRD:** §2 Phase 2

### User story
As a reader, I want Quire to work out which book I have opened by itself, so that I can
just press Read Aloud in NeoReader without telling Quire anything first.

### Context (why)
The product promise is that nothing changes about how you read. Requiring the user to open
the companion app and pick a book before every session breaks it, and silently gives wrong
voices when they forget. The Android TTS API offers no book identity, so it has to be
inferred from the text itself. Decided 2026-08-27: fingerprint automatically, with a
manual override for when it fails.

### Description (what)
On the first chunks of a session, Quire matches incoming text against every indexed book
and locks on once one book agrees for several consecutive segments. Until then everything
is read by the narrator. A companion-app override forces a specific book.

### Requirements (how)
- Owns: `core/index/identify/`, the override setting in `app/companion/`
- Lock after **3 consecutive** agreeing segments in a single book; below that, narrator.
- Ambiguity (two indexed editions both agreeing) resolves to the most recently imported,
  and records that it was ambiguous so the override can be surfaced.
- Identification must not re-run mid-session unless matching fails for 10 consecutive
  chunks — a long unmatched passage is not a new book.
- Budget: locking on adds no more than 3 utterances of narrator voice, and identification
  across 50 indexed books completes in under 50 ms per chunk. Measure both.
- Out of scope: automatic import of an unknown book.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Locking on without user input
  Given three indexed books and Read Aloud started in one of them
  When the fourth chunk arrives
  Then the matcher is locked to the correct book

Scenario: Narrator until confident
  Given a session that has just started
  When the first chunk arrives
  Then it is read by the narrator rather than a guessed character voice

Scenario: An unindexed book never locks on
  Given a book that has not been imported
  When a chapter is read aloud
  Then every chunk is read by the narrator and no book is locked

Scenario: A long unmatched passage does not trigger re-identification
  Given the matcher is locked and the reader enters a nine-chunk quoted letter
  When those chunks fail to match
  Then the lock is retained

Scenario: The user can override
  Given identification picked the wrong edition
  When the user selects the correct book in the companion app
  Then the next session uses it without fingerprinting
```

### Worklog

**2026-08-29 — session-visibility-check.** `core/index/identify/BookIdentifier.kt`.
Reproduce with `gradle :core:index:test`; 7 tests, one per Gherkin scenario plus the
budget. Whole module: 36 tests, 0 failures.

*Shape.* One identifier per reading session, holding a `Matcher` per candidate book. While
searching it runs every candidate and counts consecutive agreements; at three it locks.
`accept()` returns `MatchResult.none` until then, so "still identifying" and "book not
indexed" take the **same** path to the narrator rather than two — the caller needs no new
branch, and QUI-029's failure shape is inherited for free.

*Why narrate the chunk that completes the streak.* Locking is decided after all candidates
have seen the chunk, so voicing it would mean the caller acting on a book it has not been
told about yet. Costs one utterance and keeps the rule "no more than 3" exactly true.

*Ambiguity.* Two editions of one novel agree on nearly all their text and will both reach
the threshold. Resolved to the newest `indexedAt` — most likely the import the reader just
made — and `ambiguous` is set so the companion app can offer the override instead of
leaving them puzzled by a wrong cast.

*Override.* `BookIdentifier.forBook()` starts locked and never fingerprints. Deliberately
not a bias: a heuristic that could still overrule an explicit choice is a worse setting
than no setting.

*Measured (host, not an SLA):* **1.30 ms per chunk** identifying across 50 books of 200
entries, against the ticket's 50 ms. That is a desktop x86 number and PRD budgets are only
true on the reference device (CLAUDE.md §1.6) — it is in the suite to catch a linear scan
going quadratic, not to claim a pass. The device measurement is still owed.

The first version of that benchmark **measured nothing**: it timed a *locked* identifier,
which runs one matcher rather than fifty, and reported 0.00 ms. It now builds a fresh
identifier per iteration so the scan is what is timed. Worth remembering — a performance
test that exercises the cheap path passes loudly and tells you nothing.

*What is left before this is Done:* the per-chunk cost re-measured on the Note Air5 C, and
the companion-app half of the override (`app/companion/`), which needs QUI-025. The
`core:index` half is complete and tested.

---

# Epic: Companion

## QUI-025 — Companion app import and indexing flow

**Status:** Todo · **Owner:** — · **Epic:** Companion · **Depends on:** QUI-007, QUI-021
**PRD:** §2 Phase 1, §5 V1.0

### User story
As a reader, I want to add a book to Quire once and see it get ready, so that I know when
I can go and listen to it in my e-reader.

### Description (what)
The companion app: pick an EPUB, watch it parse, scan and index with visible progress, and
end with a book listed as ready, showing its detected cast. Indexing survives
backgrounding and resumes after a kill.

### Context (why)
This is the only UI Quire has in V1.0 — everything else happens invisibly beneath another
app. It is also where the one expensive operation lives, so how honestly it reports
progress is most of the product's felt quality.

### Requirements (how)
- Owns: `app/companion/`
- Pipeline: EPUB → segments → Tier 1 → Tier 2/3 → `characters.json` → casting →
  `dialogue_index.db`, written atomically (temp then rename) so a partial index is never
  visible to the service.
- **Budget: a 100k-word novel indexes in ≤30 minutes** in the foreground on the reference
  device (decided 2026-08-27, relaxed from 10 minutes because indexing left the real-time
  path). Progress is per stage, not a spinner.
- Resumable at chapter granularity after process death.
- Renders correctly in monochrome e-ink mode, no animated progress (CLAUDE.md §7).
- Out of scope: the voice drawer (QUI-015, V2.0), reading the book.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Import a book end to end
  Given the companion app
  When I import a 100,000 word EPUB
  Then it completes in 30 minutes or less and is listed as ready
  And its detected characters are shown with their assigned voices

Scenario: Progress is honest
  Given an import is running
  When I watch the progress display
  Then it names the current stage and advances monotonically

Scenario: Interrupted indexing resumes
  Given indexing was killed at 60%
  When the app reopens
  Then it resumes from that chapter rather than restarting

Scenario: A partial index is never visible to the service
  Given indexing is in progress
  When the TTS service looks for that book
  Then the book is absent until indexing completes

Scenario: Usable on e-ink
  Given the device is in monochrome mode
  When I run an import
  Then every element renders in pure black and white with no animation
```

### Worklog
- _(empty)_

---

# Epic: Audio (v1.2 additions)

## QUI-024 — Multi-voice utterance and `rangeStart` callbacks

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-010, QUI-022
**PRD:** §2 Phase 2, §3

### User story
As a listener, I want a paragraph that mixes narration and two characters to be read in
three voices, so that the multi-voice promise survives a reader that hands us the whole
paragraph at once.

### Context (why)
The Android TTS contract is one call, one utterance, one voice — but hosts routinely send
a whole paragraph, and QUI-018 measured that paragraphs regularly contain narration plus
two speakers. Voicing a mixed chunk in a single voice would silently reduce the product to
a normal TTS engine on exactly the passages that matter most. Synthesising several voices
inside one `onSynthesizeText` call is the mechanic that makes multi-voice work through a
single-voice API.

### Description (what)
Given the ordered segments a chunk covers, synthesise each in its assigned voice and write
them to the callback as one continuous stream, honouring the host's rate and pitch and
emitting `rangeStart` so hosts that highlight can follow along.

### Requirements (how)
- Owns: `app/ttsservice/synthesis/`
- One `callback.start()`, several synthesised segments, one `callback.done()` — the host
  must see a single continuous utterance.
- Character voices apply pitch/timbre offsets **on top of** the host's requested rate and
  pitch, never instead of them.
- `callback.rangeStart(start, end, frame)` emitted per word from the boundary timestamps
  in `TtsChunk` (QUI-010), with offsets relative to the original incoming string.
- `onStop()` cancels in-flight synthesis promptly and leaves no partial audio queued.
- One ONNX session, serialised inference; concurrency buys nothing at RTF 0.15 and doubles
  peak memory.
- Out of scope: which voice a character gets (QUI-011), buffering (QUI-012).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Three voices in one utterance
  Given a chunk covering narration, a line by Sarah and a line by Thomas
  When it is synthesised
  Then the audio uses three distinct voices in that order
  And the host receives one continuous utterance

Scenario: The host's speech rate is honoured
  Given the host requests a rate of 1.5
  When a character line is synthesised
  Then it plays at 1.5 times, with the character's offset applied on top

Scenario: Ranges point into the original string
  Given a chunk is synthesised
  When rangeStart fires for a word
  Then its offsets select that word in the string the host supplied

Scenario: Stopping is immediate
  Given synthesis is in progress
  When the host calls onStop
  Then synthesis stops promptly and no further audio is written

Scenario: An unmatched chunk still speaks
  Given a chunk the matcher could not place
  When it is synthesised
  Then it is read in the narrator voice with no added delay
```

### Worklog
- _(empty)_

---

# Epic: Quality (v1.2 additions)

## QUI-026 — E-reader compatibility matrix verification

**Status:** Todo · **Owner:** — · **Epic:** Quality · **Depends on:** QUI-019
**PRD:** §3

### User story
As a developer, I want each Tier 1 reader actually tested, so that the compatibility
matrix in the PRD is a record of what we ran rather than a list of what we hope.

### Context (why)
PRD §3 names five Tier 1 apps on the assumption they all use the system TTS engine the
same way. They will not: chunk sizes, whether `rangeStart` is consumed, whether the engine
can be selected at all, and how often `onStop` fires will differ per app. Each difference
is a matcher bug we would otherwise find from a user.

### Description (what)
A written, repeatable manual procedure run against each Tier 1 reader on the reference
device, recording chunk sizes, highlighting behaviour, transport behaviour and any
misbehaviour, and a matrix in the docs stating what was verified and when.

### Requirements (how)
- Owns: `docs/compatibility.md`
- Apps: Onyx NeoReader, Moon+ Reader, Librera, Google Play Books, eReader Prestigio.
- Record per app: can Quire be selected as the engine; typical and maximum chunk size;
  whether chunks align to sentences or paragraphs; whether `rangeStart` drives
  highlighting; `onStop` frequency; whether rate and pitch are passed through.
- Any app that cannot select a third-party engine is **demoted out of Tier 1 in the PRD**
  in the same PR — the matrix and the PRD must not disagree.
- Out of scope: Tier 2 accessibility scraping, Tier 3 KOReader plugin.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Every Tier 1 app is tested and recorded
  Given the five Tier 1 apps installed on the reference device
  When the procedure is run against each
  Then docs/compatibility.md records the result per app with the date and app version

Scenario: The procedure is repeatable by someone else
  Given docs/compatibility.md
  When a second person follows it
  Then they can reproduce the recorded observations without asking questions

Scenario: A failing app is demoted, not hidden
  Given an app that cannot select a third-party TTS engine
  When the matrix is written
  Then the PRD's Tier 1 list is corrected in the same change
```

### Worklog
- _(empty)_

---

# Epic: Spike (v1.2 additions)

## QUI-020 — TTS service registration and NeoReader binding

**Status:** In progress · **Owner:** quire-setup-docs · **Epic:** Spike · **Depends on:** —
**PRD:** §1, §2 Phase 2, §3 · **Timebox:** 2 days

### User story
As a team, I want proof that NeoReader will route its Read Aloud text through an engine we
wrote, so that we find out in two days rather than two months whether PRD v1.2 is buildable
at all.

### Context (why)
Every line of v1.2 rests on one unverified assumption: that a third-party
`TextToSpeechService` can be selected on the reference device and that NeoReader will hand
it the book's text. If that fails, the product has no V1 and the roadmap inverts — the
standalone reader currently at V3.0 becomes the only path. This is the cheapest experiment
that can invalidate the architecture, so it runs **before** the model bake-off, which is
now the second-biggest risk rather than the first.

Evidence it should work: `mateogon/boox-supertonic-tts` is an unofficial offline TTS engine
for BOOX NeoReader built on sherpa-onnx. It reports `rangeStart` reaching NeoReader's
highlighting, notes that NeoReader needs its TTS session closed and reopened to pick up a
new engine, and serialises inference to avoid native concurrency bugs. Read it before
starting; do not copy from it without checking its licence.

### Description (what)
A throwaway Android app registering a `TextToSpeechService` that speaks a fixed phrase and
logs every string, parameter and lifecycle call it receives. Installed on the Note Air5 C,
selected as the engine, and driven from each Tier 1 reader. The deliverable is a written
observation log, not a feature.

### Requirements (how)
- Owns: `spike/ttsbinding/`, `docs/adr/0004-interception-viability.md`
- Register the service (`android.intent.action.TTS_SERVICE`, engine metadata) and implement
  `onIsLanguageAvailable`, `onLoadLanguage`, `onGetLanguage`, `onGetVoices`,
  `onSynthesizeText`, `onStop`. Return a recognisable tone or a canned clip; audio quality
  is irrelevant here.
- Log for every call: the exact string, its length, the requested rate/pitch/locale/voice,
  the calling package, and the wall-clock gap since the previous call.
- Drive it from **NeoReader first**, then Moon+ Reader and Librera if time allows.
- Answer explicitly, in the ADR:
  1. Can Quire be selected as the engine on the Note Air5 C, and through which settings path?
  2. What are the typical and maximum chunk sizes, and do chunks align to sentences,
     paragraphs or pages?
  3. Is the text clean, or does it carry headers, page numbers, footnote markers or
     hyphenation?
  4. Does `rangeStart` drive NeoReader's highlighting?
  5. How often is `onStop` called — per page turn, or only on stop?
  6. Are rate and pitch passed through from the reader's own controls?
- Question 2 settles `docs/architecture.md` §9.1, which the matcher (QUI-022) is designed
  around; question 3 settles how aggressive normalisation has to be.
- Out of scope: real synthesis, matching, indexing, any production code.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: The engine can be selected
  Given the spike app installed on the Note Air5 C
  When I open the device text-to-speech settings
  Then Quire appears as a selectable engine
  And the settings path taken is written down

Scenario: NeoReader routes text to it
  Given Quire is the selected engine
  When I open a book in NeoReader and press Read Aloud
  Then the spike receives the book's text and logs it

Scenario: Chunking is characterised, not guessed
  Given a chapter read aloud in NeoReader
  When the log is reviewed
  Then typical and maximum chunk sizes are recorded
  And whether chunks align to sentences, paragraphs or pages is stated

Scenario: The highlighting question is answered
  Given the spike emits rangeStart callbacks
  When a chapter is read aloud in NeoReader
  Then whether NeoReader highlights in response is recorded either way

Scenario: A negative result is reported plainly
  Given NeoReader cannot use a third-party engine
  When the ADR is written
  Then it says so, and states what that means for the V1 roadmap
  And no work proceeds on QUI-021 through QUI-026 until the roadmap is re-decided
```

### Worklog

**2026-08-27 — manual verification by dylangrowcoot, written up by claude-opus-5.**
Question 1 answered without writing any code, by using an existing engine as the probe.

Reproduce: install eSpeak NG from the Play Store (chosen because its output is
unmistakably synthetic, so which engine is speaking is never ambiguous). Settings →
Text-to-speech → preferred engine → eSpeak NG. Open a book in NeoReader, tap the centre of
the screen, tap the headphone icon.

*Result:* the engine picker lists third-party engines once installed, and **switching it
changes the voice NeoReader produces** — both eSpeak NG and Google TTS were heard through
NeoReader's own Read Aloud. NeoReader uses the Android TTS API rather than a proprietary
Boox engine, so Quire can sit in that path. Recorded as `docs/adr/0004-interception-viability.md`.

*What surprised us:* the engine list initially showing only Google looked like a blocker
and was not one — it lists installed engines, and only one was installed. The picker
existing at all was the signal.

**2026-08-27 (later) — observation round, then the probe written.**

Four more questions answered by ear and eye, recorded in ADR-0004: NeoReader **underlines
the spoken word** (so it consumes `rangeStart` — read-along survives V1), it speaks chapter
headings, it turns the page and continues, and its own speed control passes through.

The consequential one was unprompted: **it does not treat a line break as a boundary.** A
heading with no full stop is spoken as one unit with the paragraph beneath it. So the host
segments on terminal punctuation, not on document structure. That forced two changes in
QUI-021 and QUI-022 — index by sentence, and match by concatenation rather than by
comparing sentence lists.

`spike/ttsbinding/` now holds the probe service that answers the rest: a system TTS engine
that plays one tone per word and logs every `onSynthesizeText` call to a TSV. Its README
lists the six questions and how to collect the answers.

**2026-08-28 — builds.** `dl.google.com` was added to the environment's network allowlist,
so the Android SDK installed here and the APK now compiles (AGP 8.7.3, compileSdk 35,
minSdk 26 for `rangeStart`). One real bug found by compiling: the engine metadata declared
`android:languages` on `<tts-engine>`, which is not an attribute — resource linking failed
outright. Supported locales are reported at runtime through `onIsLanguageAvailable()` and
`onGetVoices()`, never in XML; `android:settingsActivity` is the element's only attribute
and it is optional. `res/xml/tts_languages.xml` deleted.

Verified in the built APK: the service declares the `TTS_SERVICE` action, the `DEFAULT`
category, the metadata resource, and `exported=true`. Runtime behaviour on the device is
still unverified.

**2026-08-28 — the probe runs on the device.** Installed, selected as the system engine,
and driven from NeoReader: beeps play in place of speech, so the engine registers, NeoReader
binds to it, and the `start` → `audioAvailable` → `done` callback sequence works.

**The text still underlined while the beeps played.** NeoReader honours `rangeStart` from an
arbitrary third-party engine, not only from Google's, so word-level read-along ships in V1
instead of waiting for V3.0. Recorded in ADR-0004.

**2026-08-28 — captured, and the log answers the rest.** Two bugs on the way. v1 wrote the
log only to the app's private directory, which scoped storage puts out of reach without
adb. v2 added a Downloads copy but appended with `openOutputStream(uri, "wa")`, which does
not append through MediaStore — 97 rows each landed at offset 0 and one survived. v3 holds
the rows in memory and rewrites the file whole with `"wt"`.

The clean capture is 73 utterances of a novel in NeoReader. Full measurements are in
ADR-0004; the headline is that **the host chunks by clause, not by sentence** — 42 of 73
chunks ended on a comma against 27 on a full stop, median length 27 characters, nothing
within two orders of magnitude of the 4000-character limit. That reshaped QUI-022.

Also measured: the host submits a whole page of utterances in ~1.6 s then goes quiet for
the ~20 s it takes to speak them, so the ring buffer has far more lead time than the PRD
assumed; `rate` and `pitch` arrive as integer percentages; semicolons and em-dashes are not
split points; headings arrive as their own chunks with trailing spaces.

**A correction I had to make.** The earlier "headings glue to the following paragraph"
finding came from reading a PDF, and I generalised it to EPUB without saying so. In EPUB,
structure is respected. The mid-word split seen in the PDF capture was likewise a PDF
text-extraction artefact. The matcher keeps its concatenation path — it costs nothing and
covers PDFs and other hosts — but it should not have been the thing driving the design.

*Not committed:* the raw capture. It is verbatim prose from a copyrighted novel and
CLAUDE.md §8 forbids committing book text. `fixtures/host-traces/` holds a synthetic trace
of the same shape instead, with the measurements in the ADR.

*Still open:* `onStop` frequency (the probe logs it, but this capture caught none), and
whether footnote markers or page numbers appear — none were in these two chapters. Both
want a longer capture across a chapter boundary. Ticket stays `In progress` for those.

---

## QUI-027 — Normalised-to-raw offset map

> **Prototyped in the spike, 2026-08-29 (QUI-019).** `spike/slice/OffsetMap.kt` is a working
> implementation, written because the vertical slice cannot voice a partial chunk without
> one. Lift it into `core:index` and delete the spike copy. Two things it already knows:
> the walk must handle whitespace itself, because `Normalizer.normalize(" ")` trims to
> empty and a naive per-character walk drops every space; and asserting the walk reproduces
> `normalize()` exactly is what caught that.

**Status:** Done · **Owner:** session-visibility-check · **Epic:** Index · **Depends on:** QUI-021, QUI-022
**PRD:** §2 Phase 2

### User story
As a listener, I want the right voice on a clause even when the host hands over half a
sentence, so that a speech tag split at its comma does not drag the character's voice onto
the narration that follows it.

### Context (why)
QUI-022 matches on normalised text — punctuation stripped, whitespace collapsed, lowercased
— while voiced spans and `rangeStart` callbacks are addressed in the *raw* text the host
sent. While a chunk covers whole entries the two agree at the boundaries. They stop
agreeing the moment a chunk stops mid-entry, which the trace in ADR-0004 shows is the
common case: 42 of 73 chunks ended on a comma.

Today `MatchResult.spans` on a partial match returns the spans of the whole entry, so a
chunk containing only the dialogue half of `"I know," said Sarah.` still reports the
narration span too. QUI-024 cannot emit correct `rangeStart` offsets without this either.

### Description (what)
The index stores, per entry, a mapping between normalised character offsets and raw ones.
The matcher uses it to clip the spans it returns to the part of the entry a chunk actually
covered, and to report raw offsets rather than normalised ones.

### Requirements (how)
- Owns: the offset-map column in `core/index/` schema, `MatchResult` span clipping
- ~~Store the map compactly — a per-word pair of offsets.~~ **Changed during
  implementation, 2026-08-29:** the map is *computed* from the entry's raw text, not
  stored. It costs a linear walk of one sentence, needs no schema bump and no migration,
  adds nothing to the 5 MB index budget QUI-021 fought for, and — deciding it — a stored
  map could disagree with `Normalizer` after a normalisation change, which is the exact
  bug that module exists as the single implementation to prevent. Cache per entry if
  profiling ever demands it; do not persist.
- `MatchResult.spans` must be clipped to the covered range on partial matches, and its
  offsets must be raw, relative to the chunk the host supplied.
- Verify against `fixtures/host-traces/neoreader-epub-shape.tsv`, whose speech-tag rows are
  exactly this case.
- Out of scope: emitting the callbacks themselves (QUI-024).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: A speech tag split at its comma reports only the speaking half
  Given the entry "I know," said Sarah. indexed with a dialogue span and a narration span
  When the chunk "I know," is matched
  Then the returned spans cover only the dialogue span

Scenario: The narration half reports only narration
  Given the same entry with the cursor after the dialogue span
  When the chunk " said Sarah." is matched
  Then the returned spans cover only the narration span

Scenario: Offsets address the host's own string
  Given any matched chunk
  When a returned span is applied to the chunk the host supplied
  Then it selects that span's text exactly

Scenario: Whole-entry matches are unchanged
  Given a chunk covering an entire entry
  When it is matched
  Then the spans returned are identical to those before this ticket
```

### Worklog

**2026-08-29 — session-visibility-check.** `OffsetMap` lifted from `spike/slice` into
`core:index`, and `Matcher.rebase` replaced by `Matcher.clip`. Reproduce with
`gradle test`: 54 tests, 0 failures across the repository, 4 of them the scenarios here.

*Deviation from Requirements, stated rather than slipped in:* the map is computed, not
stored. Reasoning on the ticket above and in the class doc.

*Two off-by-one bugs, both caught by the new tests and both worth remembering.*

1. **The resume offset is not the content end.** `runAt` steps the offset over the space
   between clauses so the next chunk resumes on a word boundary. Clipping with that value
   handed every partial chunk one character of the clause *after* it, which was enough to
   drag a whole narration span into a dialogue chunk. `Run` now carries `contentEnd`
   separately from `endOffset`.
2. **Normalisation drops the opening quote**, so the first normalised character of
   `"I know,"` maps to the `I`, and the chunk's own opening quote fell to whoever held the
   previous span. `clip` now reaches back over characters that normalise to nothing,
   stopping at whitespace so it can never cross into the clause before.

*A test that encoded the bug.* `SceneReplayTest` asserted that `" she said."` carries
Sarah, and passed, because spans covered the whole entry and the speech tag inherited the
dialogue voice. That is precisely the defect this ticket exists to fix, written down as an
expectation. Corrected, with the reason in the test. Worth the reminder that a test can be
as confidently wrong as the code.

*Verified against the capture* the ticket asks for: every span of every chunk in
`fixtures/host-traces/neoreader-epub-shape.tsv` now falls inside the chunk it belongs to.

*Downstream simplification.* `spike/slice/ChunkPlan` loses its proportional-cutting
approximation entirely — the matcher returns exact, chunk-relative spans, so the spike just
cuts the string. That was the one remaining place where a chunk straddling a speaker change
mid-clause would have been voiced wrongly.

---

## QUI-028 — Encoder vs SLM for quotation attribution

> **Released 2026-09-10 by `quire-dialogue-attribution`** (PR #8, merged). Nobody owns this;
> what it still needs is listed under *What is left* below. Took over from
> `quire-explicit-subscore` (PR #7, merged). The encoder half is closed (ADR-0005). The
> Explicit sub-score was answered and was a harness fault: marking a quotation `[Qn: ...]`
> in the text costs 50 points of Explicit precision, and unmarked the same 1B model reaches
> 90.6% among offered speakers (2026-09-09 Worklog). **No SLM headline follows yet** — the
> fix needs an addressing scheme for N targets in one call that is not in-text marking.
>
> **Every PDNC precision figure in this ticket dated before 2026-09-10 is ~8 points low.**
> Fault 6: scoring compared speaker *strings* and never read PDNC's own alias table, so
> naming Charlotte Lucas "Miss Lucas" — which the novel does — counted as a wrong voice.
> Tier 1's explicit-tag precision is **99.0%**, not 89.9%. See the 2026-09-10 Worklog.

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** —
**PRD:** §2 Phase 1, §4 · **Timebox:** 3 days

### What is left (2026-09-10)

The encoder half is closed by ADR-0005 and the rule half is measured. Three things in the
Requirements above are still unanswered, and none of them belongs to a session that only has
a build container:

- **No SLM headline.** The batched prompt needs an addressing scheme for N targets in one
  call that is not in-text marking; `51.5%` / `3.0%` remain the marked-prompt numbers.
- **No device measurements at all.** Wall-clock for a 100k-word novel, peak RSS, on-disk
  size and sustained power are five of this ticket's acceptance criteria and every one of
  them needs the Note Air5 C. Nothing measured here is a device number.
- **`Holdouts.External` is empty.** The out-of-domain split is PDNC novels unlike the rest
  of PDNC; the corpus stops in 1934, so no figure here speaks to contemporary prose.
  Filling it needs a decision rather than a script, because CLAUDE.md §8 forbids committing
  book text.

### User story
As a team, I want to know whether a small purpose-built encoder attributes dialogue better
and faster than the 1B SLM we planned on, so that we are not spending our worst constraint
on a job a much cheaper model does better.

### Context (why)
The architecture puts a quantized 1B SLM on Tier 2 attribution, and `device-profile.md` §2
works out that on a Snapdragon 750G without i8mm this lands in the *hours* for a novel.
Everything in `architecture.md` §5 — KV-cache reuse, single-token generation, Tier 1
coverage as a performance feature — exists to fight that.

The literature suggests the fight may be unnecessary (`docs/prior-art.md` §3). Encoder
models built for quotation attribution report **94.5% on PDNC at 20× the speed of standard
methods and over 1000× the speed of LLM approaches**, against BookNLP's ~63%. A BERT-class
encoder is roughly 110M parameters — an order of magnitude below the SLM, and squarely in
what ONNX Runtime Mobile already runs well.

If that transfers to our hardware it removes our worst constraint. If it does not, we have
lost three days and know the SLM plan is right.

### Description (what)
A comparison on the same fixtures, on the reference device: a small attribution encoder
against the planned 1B SLM, measured on accuracy, wall-clock time for a whole novel, peak
RSS and power. The output is a decision recorded as an ADR, and — if the encoder wins — a
rewrite of QUI-006 and QUI-009.

### Requirements (how)
- Owns: `docs/adr/0005-attribution-model.md`, `tools/fetch-pdnc.sh`, and the bake-off half
  of `spike/pipeline/` — `bakeoff/`, plus the corpus layer in `Pdnc.kt` it is built on.
  The harness lives beside the PDNC loader rather than in a module of its own: two loaders
  is how the measured Tier 1 and the shipped one quietly stop agreeing, and `Pdnc.kt` is
  already shared with QUI-032's cast scoring.
- Candidates: an encoder-based attribution model exported to ONNX, BookNLP as the baseline
  the field reports against, and the 1B SLM prompt from QUI-009.
- Evaluate on **PDNC** — 37,131 quotations across 28 novels in the current revision — so
  numbers are comparable to published results. **It carries no licence**: no `LICENSE`
  file, no terms in its ReadMe, only a link to its paper (checked 2026-08-28). Clone it
  with a fetch script, cite the paper, never commit or redistribute it.
- Report accuracy **split by PDNC's `quoteType`**. The corpus is 30.1% `Explicit`, 45.0%
  `Implicit`, 24.8% `Anaphoric`, so a headline number hides which tier does the work —
  Tier 1 can never exceed the explicit share.
- Measure per candidate: accuracy on PDNC, wall-clock to attribute a 100k-word novel on the
  Note Air5 C, peak RSS, on-disk size, sustained power draw.
- Report Tier 1 coverage separately, so we can see how much of the book each model is even
  asked about (QUI-018 measured 44.4% coverage at 100% precision on hand-written fixtures).
- **Measure out-of-domain accuracy, not just PDNC.** PDNC is 22 English novels weighted
  towards literary fiction; a reader's library is not. Hold out at least three books
  unlike it — a translated novel, contemporary genre fiction heavy on action beats rather
  than speech tags, and a first-person narrative — and report their accuracy separately
  from the headline figure. The published 94.5% is a ceiling on that corpus's home turf,
  not a promise about a real library, and a model that only works on the benchmark is not
  a model that ships.
- The ADR must state explicitly what happens to the SLM. Character-manifest generation
  (names, aliases, gender, age band, traits) is a *different* task an attribution encoder
  does not do, so a win here narrows the SLM's job rather than removing it.
- Out of scope: production integration; that is QUI-006 and QUI-009 rewritten afterwards.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Both candidates measured on the same data
  Given PDNC and the reference device
  When the bake-off runs
  Then accuracy, wall-clock time for a novel, peak RSS, disk size and power are recorded for each candidate

Scenario: The indexing budget is answered
  Given the winning candidate
  When a 100,000 word novel is attributed end to end on the device
  Then the elapsed time is stated against QUI-007's 30 minute budget

Scenario: Accuracy is comparable to published work
  Given results on PDNC
  When they are written up
  Then they are stated alongside the published BookNLP and state-of-the-art figures

Scenario: Out-of-domain accuracy is reported separately
  Given held-out books unlike PDNC — a translation, action-beat-heavy genre fiction, first person
  When each candidate is evaluated on them
  Then their accuracy is reported apart from the PDNC figure
  And the gap between the two is stated as the expected real-library degradation

Scenario: The decision names the consequences
  Given the ADR
  When I read it
  Then it says whether QUI-006 and QUI-009 are rewritten, and what job the SLM keeps

Scenario: A negative result is reported plainly
  Given the encoder underperforms the SLM on our hardware
  When the ADR is written
  Then it says so and the SLM plan stands unchanged
```

### Worklog

**2026-08-28 — claude-opus-5.** Tier 1 was scored against PDNC and the results are written
up in **QUI-018's worklog**, not here — the scoring landed as part of the pipeline spike.
Recorded in this ticket so the board does not read as though no work has happened.

Headline: 58.5% precision over 2,846 matched quotations from five novels, against the 100%
that hand-written fixtures had suggested. On quotations carrying no explicit tag, Tier 1 is
right about one time in nine — it is not declining to guess, it is guessing and losing. The
consequential finding is that the confidence values are fiction (EXPLICIT_TAG 0.95 against
a measured 68.6%), which makes calibration a prerequisite for QUI-009's gates.

*What is left:* the actual bake-off this ticket is for — an encoder candidate measured
against the 1B SLM on device — has not started. Only the Tier 1 baseline it will be
compared against exists.

**2026-09-02 — `qui-028-scoring-harness`.** Host-side half of the bake-off. The corpus
fetch, the harness, the Tier 1 baseline and the out-of-domain split all landed; neither
model candidate did, because neither model file is reachable from a session container.

*Landed:* `tools/fetch-pdnc.sh`; the corpus layer in `spike/pipeline`'s `Pdnc.kt`; the
harness in `spike/pipeline/src/main/kotlin/quire/spike/bakeoff/`; 18 tests, which are the
first `Pdnc.kt` has ever had.

**Reproduce:**

```bash
tools/fetch-pdnc.sh
cd spike/pipeline && gradle installDist && gradle test
build/install/quire-pipeline-spike/bin/quire-pipeline-spike bakeoff --per-novel
```

#### A bug in the name matcher was deflating every PDNC number in the repo

`Pdnc.matches` stripped punctuation from the gold name and left it on ours, so predicted
`Mr. Woodhouse` did not match gold `Mr. Woodhouse` — the words compared were `mr.` and `mr`.
Every honorific carrying a full stop scored as a miss. Austen is a fifth of this corpus.

Fixed by folding both sides the same way, with a regression test. **This corrects numbers
outside this ticket:** QUI-032's cast scoring shares the same function, and on Emma, The
Gambler and The Sign of the Four its precision goes **77.4% → 96.2%** — most of the "junk"
characters it reported were real characters that failed to match on a full stop. Its recall
is unchanged at 80.8%. `gknown` moves 58.5% → 47.1%, which is arithmetic rather than a
regression: the count of real characters rose from 41 to 51, and the extra ones have no
gender. **QUI-032's worklog numbers are understated and should be re-measured.**

#### The baseline, re-measured — and the August number was wrong

Tier 1 (`core:attribution`, pronouns and action beats on) over **36,970 scorable quotations
across all 28 novels**. PDNC holds 37,131; 160 name a pseudo-entity (`_group`,
`_unknowable`, `_narr`) that nobody could be right about, and one span fell outside every
paragraph.

| Quotation type | Quotes | Coverage | Precision | Accuracy |
| --- | ---: | ---: | ---: | ---: |
| Explicit | 11,172 | 79.9% | 88.9% | 71.0% |
| Implicit | 16,645 | 2.3% | 61.8% | 1.4% |
| Anaphoric | 9,125 | 6.5% | 40.9% | 2.7% |
| **All** | **36,970** | **26.8%** | **84.9%** | **22.8%** |

**This supersedes the 58.5% precision in the entry above, which was measured wrongly.**
That pass matched gold quotations to predicted segments by normalised text and scored only
the ones that keyed — 2,846 of 37,131, or 7.7% of the corpus, self-selected for being short
and cleanly punctuated. The harness now uses PDNC's own byte spans, so the denominator is
every scorable quotation and a quotation the candidate never saw counts against it. Same
code under test, same corpus, a denominator thirteen times larger.

**The confidence values are better calibrated than we told ourselves.** Scored by evidence,
the explicit speech-tag rule answered 8,357 quotations at **89.9% precision**, against a
declared `EXPLICIT_TAG = 0.95`. The 68.6% recorded in the entry above — and copied into the
KDoc on `AttributionResult.confidence` in `core/model` — came from the same broken sample.
Calibration is still needed for QUI-009's gates, but the error is about five points, not
twenty-seven. *That KDoc is QUI-008's file, so it has been left alone rather than reached
across (CLAUDE.md §2.2); it needs a one-line correction from whoever owns it next.*

**What each rule bought,** the same corpus with rules switched off:

| Tier 1 configuration | Coverage | Precision | Accuracy |
| --- | ---: | ---: | ---: |
| explicit tags only | 22.6% | 89.9% | 20.3% |
| tags + pronoun rule | 23.0% | 88.8% | 20.5% |
| tags + pronouns + action beats | 26.8% | 84.9% | 22.8% |

The action-beat rule buys **3.8 points of coverage for 3.9 points of precision** — it
answered 1,402 quotations at 61.3%. That is a much better trade than it looked before
QUI-032's roster junk filter landed, which cut the rule's answers from 1,733 to 1,402 and
lifted its precision from 48.9% to 61.3%: most of what it used to get wrong, it now declines
to answer. Left on. Given that a confidently wrong voice is heard and a missing one is
merely flat (PRD §3.1), it is still the first switch to try if the winning model answers
those lines well — `--candidate tier1-nobeats`.

#### The out-of-domain holdouts

Three axes, selected from PDNC's own novel index (`Narrative Person`, `Translator Code`,
`Genre`), and excluded from the headline figure:

| Axis | Novels | Quotes | Coverage | Precision | Accuracy |
| --- | --- | ---: | ---: | ---: | ---: |
| translation | The Gambler | 767 | 7.7% | 64.4% | 5.0% |
| action-beats | The Invisible Man, The Mysterious Affair at Styles, The Sign of the Four | 3,405 | 17.2% | 89.9% | 15.5% |
| first-person | Daisy Miller, The Gambler, Styles, Sign of the Four, The Sun Also Rises, Where Angels Fear to Tread, Winnie-the-Pooh | 8,268 | 21.9% | 87.0% | 19.0% |
| **all holdouts** | 8 novels | **9,172** | **22.9%** | **88.2%** | **20.2%** |

Headline (the remaining 20 novels, 27,798 quotations): coverage 28.1%, precision 84.1%,
**accuracy 23.7%**. The gap is **−3.5 accuracy points**, and it is a **lower bound**, for
two reasons worth stating plainly:

1. **These are still PDNC novels.** The ticket asks for books unlike PDNC; these are books
   unlike the rest of PDNC, one axis each. The corpus stops in 1934, so *contemporary genre
   fiction is not in it at any price* — the action-beat axis is served by Doyle, Christie
   and Wells, and it is the holdout Tier 1 handles best, at 89.9% precision.
2. **The translation axis is one novel**, and it is the worst book in the corpus for us:
   5.0% accuracy at 64.4% precision, against 23.7% headline. If translated prose really
   costs that much, one novel is far too thin a basis for the number, and a second
   translation would be the single most valuable addition to the split.

`Holdouts.External` is the empty slot for real out-of-corpus books, with the file format
documented. **Nothing was written to fill it, on purpose.** Hand-authored passages would
produce a number built from the cases we thought of, and that bias is precisely what cost
QUI-018 a day: its hand-written fixtures said 100% precision where PDNC says 84.9%.
Gutenberg is unreachable from the container and CLAUDE.md §8 forbids committing book text,
so filling this slot needs a decision, not a script.

#### Blocked: the model files, and exactly what to host

Hugging Face is refused at the proxy (`connect_rejected`, `huggingface.co:443`), and so is
`people.ischool.berkeley.edu`, where BookNLP fetches its weights (HTTP 403 on both model
URLs). Neither model candidate can run here until the files are hosted somewhere reachable.

**GitHub release assets are reachable** — verified with a ranged GET against the sherpa
asset, HTTP 206, and `raw.githubusercontent.com` returns 200. Attaching a file to a release
on any public GitHub repo is enough; no allowlist change is needed. Adding `huggingface.co`
to the environment's allowed-domain list would also work and would unblock every future
model at once, the way `dl.google.com` did for the Android SDK (CLAUDE.md §9).

**1. BookNLP speaker attribution model** — the ticket's required baseline, the ~63% the
field reports against. File names and URLs read out of `booknlp/english/english_booknlp.py`
(lines 56–95) at repository HEAD, so they are exact:

| | `big` (accuracy reference) | `small` (the one that could ship) |
| --- | --- | --- |
| File | `speaker_google_bert_uncased_L-12_H-768_A-12-v1.0.1.model` | `speaker_google_bert_uncased_L-8_H-256_A-4-v1.0.1.model` |
| URL | `http://people.ischool.berkeley.edu/~dbamman/booknlp_models/speaker_google_bert_uncased_L-12_H-768_A-12-v1.0.1.model` | `http://people.ischool.berkeley.edu/~dbamman/booknlp_models/speaker_google_bert_uncased_L-8_H-256_A-4-v1.0.1.model` |
| Size | **≈ 438 MB** | **≈ 57 MB** |

**The sizes are derived, not measured** — the host 403s from here, so nothing verified them.
They come from the architecture the filename declares: BERT-base `L-12_H-768_A-12` is 109.5M
parameters, plus BookNLP's scoring head (`Linear(2×768, 100)` then `Linear(100, 1)`,
`speaker_attribution.py:44–46`), as an fp32 state dict — 109.7M × 4 bytes. The small model is
`L-8_H-256_A-4`, 14.3M parameters, same arithmetic. Expect the real files within a few
percent; if `big` arrives at half that, it was saved in fp16 and that is worth knowing.

Note which one matters. PRD §5 allows 450 MB for the *whole app*, TTS voices included, so a
438 MB fp32 BERT-base cannot ship at any quantisation story we have — `big` is the accuracy
ceiling to measure against, and `small` is the only BookNLP candidate that could become a
product.

**Also needed, and also on Hugging Face:** BookNLP derives the base model id from the
checkpoint filename (`bert_qa.py:16`) and loads it through `transformers`, so running it also
pulls **`google/bert_uncased_L-12_H-768_A-12`** (or `L-8_H-256_A-4` for small) for the
tokenizer — `vocab.txt` (~232 KB) and `config.json` are the parts actually needed, since the
checkpoint overwrites the weights, but stock `transformers` will fetch `pytorch_model.bin`
(≈438 MB) as well unless it is pointed at a local directory.

**2. The 94.5% joint-scoring encoder** — the model this ticket exists for. **I cannot name
its file.** `docs/prior-art.md` §3 cites it as arXiv 2608.02359, `arxiv.org` is refused at
the proxy (`CONNECT` 403), and guessing a checkpoint URL for a paper I cannot read would be
inventing evidence. What is needed from the paper or its repository: the checkpoint or
Hugging Face repo id, its parameter count, and whether an ONNX export exists or has to be
made. With that, the candidate is an afternoon on top of this harness.

*What is left:* both model candidates and the 1B SLM prompt from QUI-009; every on-device
measurement (wall-clock for a 100k-word novel, peak RSS, disk, power) — none of which this
container can take; and `docs/adr/0005-attribution-model.md`, which cannot honestly be
written until something has been measured against the baseline above.

*Note for the next session:* `Tier1Test` has two failures on `origin/main` that are not this
ticket's. `roster bootstrap separates strong and weak evidence` expects `Mary` and gets only
`Sarah` — it is red on `origin/main` itself, and arrived with QUI-032's roster junk filter.
`attributes a hundred thousand words in under two seconds` is a wall-clock SLA that fails
under container load and passes on a quiet rerun.

**2026-09-06 — `local-model-voice-accents`.** The encoder candidate is built and wired.
It does not have weights, and the reason is exact and fixable.

*Landed:* `spike/pipeline/predictors/grimbert_predict.py`, `ExternalCandidate` and the
`bakeoff dump` command that feeds it, `tools/fetch-attribution-models.sh`, and six tests.
Root and pipeline suites green.

**Reproduce:**

```bash
tools/fetch-pdnc.sh
python3 -m pip install grimbert
cd spike/pipeline
gradle run --args="dump --out build/bakeoff --novels AHandfulOfDust"
python3 predictors/grimbert_predict.py build/bakeoff --novels AHandfulOfDust
gradle run --args="bakeoff --candidate grimbert --answers build/bakeoff"
```

#### Still blocked, and it is one hostname

`huggingface.co` was allowlisted (QUI-037) and that is genuinely not enough. The API and
small files serve from `huggingface.co` itself — `config.json` fetches, `api/models`
returns 200 — but **every large file 307s to a separate CDN host, and the proxy refuses the
tunnel with 403**:

```
model.safetensors -> https://us.aws.cdn.hf.co/xet-bridge-us/...
curl: (56) CONNECT tunnel failed, response 403
```

Also refused: `cdn-lfs.huggingface.co`, `cdn-lfs-us-1.hf.co`, `cas-bridge.xethub.hf.co`,
`transfer.xethub.hf.co`. The failure is silent from Python's side — `from_pretrained` hangs
rather than erroring, which cost half an hour before the redirect was inspected directly.

**What to add: `us.aws.cdn.hf.co`, and `cas-bridge.xethub.hf.co` for other regions.** Every
other blocker on this ticket is gone.

#### The encoder is not the model this ticket assumed

Worth knowing before anyone budgets against 94.5%. `compnet-renard/spanbert-base-cased-literary-speaker-attribution`
is SpanBERT-base, 431 MB fp32, ~108M parameters, loaded by the `grimbert` package because
its head is a custom class rather than a stock transformers one.

**It ranks a cast it is handed; it does not name a speaker.** It scores (quotation,
candidate mention) pairs and returns the character whose mentions win, declining below 0.5.
So it presumes NER and character unification have already run — which in Quire is Roster
(QUI-007, QUI-034). Two consequences:

1. **It composes with our cast discovery rather than replacing it**, and its accuracy is
   bounded by ours: a character Roster never found cannot be predicted. QUI-034's 96.6%
   cast precision is therefore an input to this ticket's answer, not a separate result.
2. **The published figure is not directly comparable** until we know what mentions it was
   given. The predictor matches PDNC's own alias lists against the text, because PDNC ships
   no mention offsets and because gold mentions would flatter the model with information
   the device will not have. If the published number used gold mentions, the gap between
   that and ours is a real cost of shipping, not a bug.

#### Sizing, unchanged by any of this

431 MB fp32 against PRD §5's 450 MB for the whole app, TTS voices included. Like BookNLP
`big`, this is an accuracy ceiling to measure against rather than a thing that ships;
int8 would be ~108 MB and might, but ADR-0002 §9 is a standing warning that int8 is a size
decision that is not automatically a speed one.

*What is left:* the run itself, then BookNLP as the published baseline, then the QUI-009 SLM
prompt, then every on-device measurement. `docs/adr/0005-attribution-model.md` still cannot
honestly be written — nothing has been measured against the baseline yet.

**2026-09-06 — first encoder numbers, one novel.** `A Handful of Dust`, 2,337 quotations.
The download works with `HF_HUB_DISABLE_XET=1`; the model is 107.7M parameters, 412 MB.

| | coverage | precision | accuracy |
| --- | ---: | ---: | ---: |
| Tier 1 | 18.4% | 88.6% | 16.3% |
| grimbert encoder | 88.0% | 52.9% | **46.6%** |

Accuracy nearly triples. **Read the precision column before celebrating.**

#### The two are complementary, by quotation type

Accuracy, so the columns are comparable:

| | quotes | Tier 1 | grimbert |
| --- | ---: | ---: | ---: |
| Explicit | 409 | **82.2%** | 70.4% |
| Implicit | 1,727 | 2.4% | **39.2%** |
| Anaphoric | 201 | 2.0% | **61.2%** |

**Tier 1 wins outright where there is a speech tag** — 91.6% precision against 74.8% — and
loses everywhere else by an order of magnitude. That is the shape ADR-0006 assumed and it
is now measured rather than asserted. A hybrid taking Tier 1's answer when it has one and
the encoder's otherwise scores about 48.6% on this novel, two points above the encoder
alone and with far better precision on the tagged slice.

#### The number that decides it is the wrong-voice rate, not accuracy

PRD §3.1: a missing voice is flat, a wrong voice is *heard*. So the figure to compare is
coverage × (1 − precision) — the share of all quotations read in somebody else's voice:

| | wrong voice, as a share of all quotations |
| --- | ---: |
| Tier 1 | **2.1%** |
| grimbert encoder | **41.4%** |

Tripling accuracy costs a twentyfold increase in lines read by the wrong character. On this
product that is very likely a worse outcome than the flat narrator it replaces, and it is
the opposite of what the accuracy column alone would tell you.

**This does not kill the encoder; it names the next experiment.** The predictor already
thresholds at 0.5 on a score the model exposes, chosen because that is what Renard uses.
Nothing has swept it. Trading coverage for precision is one parameter, and the interesting
question is whether there is a threshold where the encoder keeps most of its Implicit and
Anaphoric wins at a precision the ear can live with.

#### Why 52.9% and not the published 94.5%

Three reasons, and the first is the one to fix:

1. **Mentions are found by matching PDNC's alias lists against the text**, because PDNC
   ships no mention offsets and gold mentions would flatter the model with information the
   device will not have. The published figure was very likely computed with gold mentions.
   This is the largest suspected gap and it is measurable: run it once with gold mentions
   and the difference is the true cost of shipping.
2. **One novel, and not a held-out one.** No claim here generalises yet.
3. The cast is PDNC's, not Roster's. On device it would be Roster's, which QUI-034 measures
   at 96.6% precision — another bound on top of this one.

*What is left:* the whole-corpus run (started, ~15 minutes a novel on this container's CPU,
so roughly overnight for 28), a threshold sweep, BookNLP as the published baseline, and
every on-device measurement. ADR-0005 still cannot be written honestly — but the shape of
what it will say is now visible, and it is not "the encoder replaces Tier 1".

**2026-09-06 — throughput, and it is the third disqualifier.** Measured off the corpus run
on this container's x86 CPU: `ARoomWithAView`, 1,634 quotations, 54.2 minutes wall clock —
**0.50 quotations per second**. The neighbouring novel gives the same rate within a few
percent. The machine was shared with Gradle runs, so treat it as a floor rather than a
clean figure.

Absolute host numbers do not transfer to the device (`spike/hostbench/README.md`), and this
one does not need to. QUI-007 allows **30 minutes for the entire scan** of a 100k-word
novel — parse, cast, voice design, attribution, index write. Such a novel carries roughly
2,300 quotations, which this encoder answers in about **77 minutes on a desktop core**.
It is already 2.5× over the whole budget on hardware several times faster than a
Snapdragon 750G, and §8 of ADR-0002 measured that SoC punishing this class of work about
twice as hard again. There is no version of this arithmetic that fits.

**So the 431 MB encoder is out on three independent counts**, any one of which would be
enough:

| | measured | budget |
| --- | --- | --- |
| wrong-voice rate | 41.4% of all quotations | ~2% today, from Tier 1 |
| whole-novel time | ~77 min, desktop | 30 min, on device |
| on-disk size | 431 MB fp32 | 450 MB for the whole app |

**What survives is the finding underneath it, and it is the valuable part.** The encoder is
strong exactly where Tier 1 is blind — Implicit 39.2% against 2.4%, Anaphoric 61.2% against
2.0% — and worse than Tier 1 where a speech tag exists. That shape is a property of the
approach, not of this checkpoint, and it is what ADR-0006's tiering assumed.

The next candidate is therefore not a bigger corpus run on this model. It is **BookNLP
`small`** — `L-8_H-256_A-4`, ~14M parameters against 107.7M, ~57 MB against 431 MB — which
is the only published attribution model in this family that could fit any of the three
budgets. Plus a threshold sweep on whatever wins, since 0.5 is Renard's default and nobody
has moved it.

*Corpus run:* left running; at this rate the full 36,970 quotations is roughly 20 hours, so
it will not finish inside a session container. Not worth restarting on this checkpoint.

**2026-09-06 — BookNLP `small`, and the threshold sweep that settles it.** One novel,
`A Handful of Dust`, 2,337 quotations. 57 MB, 14M parameters, fetched over **https** —
`people.ischool.berkeley.edu` is on the allowlist now but the proxy tunnels TLS only, and
BookNLP's own code uses `http` URLs, which still 403.

Two fixes were needed to run it at all, both recorded in `predictors/booknlp_predict.py`:
the checkpoint carries `bert.embeddings.position_ids`, a buffer current transformers no
longer registers, so it is dropped on load; and BookNLP's winning candidate is often
*another quotation* rather than a mention — its turn-taking mechanism — so the chain has to
be resolved. Without the chain, coverage was 33.9% instead of 98.5%.

#### All three candidates, same novel, same scorer

| | coverage | precision | accuracy | **wrong voice** |
| --- | ---: | ---: | ---: | ---: |
| Tier 1 | 18.4% | 88.6% | 16.3% | **2.1%** |
| SpanBERT encoder, 431 MB | 88.0% | 52.9% | 46.6% | 41.4% |
| BookNLP `small`, 57 MB | 98.5% | 44.0% | 43.3% | 55.1% |

"Wrong voice" is coverage × (1 − precision): the share of all quotations read in somebody
else's voice, which PRD §3.1 says is what a reader actually notices.

**BookNLP wins where a speech tag exists** — 90.7% accuracy on Explicit against Tier 1's
82.2% and the big encoder's 70.4% — and that is the one genuine win on the board. It is
also the easy fifth of the book.

#### The confidence score is not a precision dial

The obvious rescue is to answer only when confident. It does not work. Sweeping the
softmax threshold over the cached scores:

| threshold | coverage | precision | accuracy |
| ---: | ---: | ---: | ---: |
| 0.00 | 98.5% | 44.0% | 43.3% |
| 0.50 | 80.9% | 47.8% | 38.7% |
| 0.80 | 67.5% | 50.0% | 33.7% |
| 0.90 | 56.8% | 51.7% | 29.4% |
| 0.95 | 44.4% | 55.7% | 24.7% |
| 0.99 | 15.8% | 76.2% | 12.0% |

Giving up 54 points of coverage buys 12 points of precision. **At 0.99 — 15.8% coverage,
76.2% precision — Tier 1 is better on both axes at once**, 18.4% and 88.6%. There is no
operating point on this curve where BookNLP `small` beats the heuristic we already have.

That is a sharper finding than a bad score would have been. The model is not right-but-
unsure on the lines it misses; it is confidently wrong, so no gate can recover it.

#### What this means for the architecture

Neither published attribution model, at any operating point, beats Tier 1 on the metric
that decides. **The untagged three-quarters of dialogue is not solved by an encoder.**

That is not a failure of the ticket, it is its answer, and it lands on ADR-0006's side: the
remaining candidate is a generative model reading a *whole scene* and resolving turn-taking
in context, which is QUI-009 and QUI-031. The encoders were the cheap hope; they are spent.

Two things keep the encoder family alive in a narrow sense, and both are for later:
BookNLP `small` could serve the Explicit slice at 57 MB and buy ~8 accuracy points there,
and nobody has yet run it with **gold mentions** to price how much of the gap is our
alias matching rather than the model.

*Caveat, and it is not small:* one novel, not held out. Every number above is a single
book. The direction is consistent across three candidates and a seven-point sweep, but the
magnitudes are not corpus figures.

*What is left:* a corpus run on BookNLP `small` (about 10 minutes a novel, so feasible
overnight unlike the 431 MB encoder), the gold-mention comparison, and the QUI-009 SLM
prompt. ADR-0005 can now be written for the encoder half, and it says no.

**2026-09-07 — eight novels, and the out-of-domain gap is the finding.** 12,521 quotations:
four headline books and four of PDNC's own out-of-domain holdouts. Same scorer, same
alias-matched mentions, BookNLP `small` at threshold 0.

| PDNC headline, 7,656 quotations | coverage | precision | accuracy | **wrong voice** |
| --- | ---: | ---: | ---: | ---: |
| Tier 1 | 30.5% | 89.2% | 27.2% | **3.3%** |
| BookNLP `small` | 93.5% | 57.2% | 53.4% | 40.0% |

| Held out, 4,865 quotations | coverage | precision | accuracy | **wrong voice** |
| --- | ---: | ---: | ---: | ---: |
| Tier 1 | 24.2% | **95.5%** | 23.1% | **1.1%** |
| BookNLP `small` | 84.6% | 41.9% | 35.5% | 49.2% |

**The model degrades four times as hard as the heuristic.** Out of domain Tier 1 loses 4.1
accuracy points (27.2 → 23.1); BookNLP loses 17.9 (53.4 → 35.5). And the direction of the
precision column is opposite: **Tier 1 gets *more* precise out of domain, 89.2% → 95.5%,
while BookNLP falls 57.2% → 41.9%.**

That is not a curiosity, it is the whole risk. A rule that fires on `said Geralt` is right
wherever that construction appears, and declines everywhere else; a model trained on 19th
century literary fiction is confidently wrong on prose it has not seen. PDNC stops in 1934,
so *contemporary genre fiction is not in this table at any price* — the real degradation on
a reader's library is worse than −17.9 and remains unmeasured.

**The single-novel numbers were pessimistic about the model, and it does not matter.**
`A Handful of Dust` alone gave BookNLP 43.3%; over four headline novels it is 53.4%. The
ordering that decides is unchanged: the heuristic reads 3.3% of quotations in the wrong
voice and the model reads 40%.

#### The one robust win, now confirmed across four novels

| Explicit quotations only | coverage | precision | accuracy |
| --- | ---: | ---: | ---: |
| Tier 1 | 84.8% | 91.8% | 77.9% |
| BookNLP `small` | 98.8% | 90.0% | **88.9%** |

**Eleven accuracy points, at the same precision**, on the quotations that carry a speech
tag. BookNLP finds tags Tier 1's regex misses and matches it for correctness on the ones
it does find. A hybrid — BookNLP on the Explicit slice, silence elsewhere — scores about
29.3% accuracy at roughly 90% precision and ~3% wrong voice: better than Tier 1 on every
axis, for 57 MB. That is a real, shippable improvement and it is the only one on offer here.

**It does not touch the untagged three quarters**, which is what the ticket was for.

*What is left:* the QUI-009 SLM prompt on a whole scene (QUI-038 must land first), the
gold-mention comparison that would price our alias matching, and every on-device number.
ADR-0005 can be written now for the encoder half, and its answer is no.

**2026-09-07 — encoder half closed; [ADR-0005](docs/adr/0005-attribution-model.md) written.**
The decision this ticket existed to make is recorded: **neither published encoder replaces
Tier 1.** Both roughly double accuracy and both take the wrong-voice rate from ~3% to ~40%,
no confidence threshold recovers either, and the model degrades four times as hard as the
heuristic on prose unlike PDNC's.

BookNLP `small` is adopted for the Explicit slice alone — 88.9% accuracy against Tier 1's
77.9% at the same precision, for 57 MB — as an optional tier that blocks nothing.

**Status `In review` rather than `Done`,** because the ticket's title names two things and
only one is answered. The SLM half needs QUI-009's prompt and QUI-006's runtime, and the
on-device measurements need hardware. What this ticket set out to decide about *encoders*
is decided; whoever closes it should either split the SLM half into its own ticket or
re-open this one against QUI-009's results.

**2026-09-08 — the scene-level SLM harness works; the 1B model does not, and I am not
certain which of those is the finding.** Llama 3.2 1B Instruct Q4_K_M, one call per scene
piece, `A Handful of Dust`, scored by the same harness as every other candidate.

| | coverage | precision | accuracy | wrong voice |
| --- | ---: | ---: | ---: | ---: |
| Tier 1 | 18.4% | 88.6% | 16.3% | **2.1%** |
| BookNLP `small` | 98.5% | 44.0% | 43.3% | 55.1% |
| Llama 3.2 1B, per scene | 9.4% | 32.4% | 3.0% | 6.4% |

**Read this as a lower bound on the approach, not a verdict on it.** Four harness faults
were found and fixed on the way here, each of which produced a plausible-looking number
first:

1. **Unconstrained output.** Asked for a JSON array, the model returned a JSON object
   mapping names to quotation text. 104 of 112 pieces unparseable. Fixed with a GBNF
   grammar — **0 pieces dropped on alignment since.**
2. **A `head -8` in the run command** closed the pipe and killed a run partway, leaving a
   639-answer file that looked complete.
3. **The whole book's cast offered as candidates** — 104 names per quotation, where
   BookNLP and grimbert only ever rank mentions inside the window. Now scene-restricted.
4. **Candidates built from PDNC's `Main Name` only**, not its aliases, so `Mrs. Beaver`
   never matched `Mrs Beaver` in the text and **the correct speaker was frequently not on
   the list at all.**

Each was caught by a *sub-score* rather than the headline: precision on Explicit
quotations, where the speech tag names the speaker in the same sentence, so anything that
can read should be near-perfect. It is now **51.5%**, against Tier 1's 91.8%. That is much
better than the 36.8% of fault 4, and still wrong enough that I do not believe the harness
is clean.

**What is established.** The pipeline runs end to end: QUI-038's scenes → a marked prompt →
grammar-constrained output → the shared scorer. Constrained decoding is necessary and
sufficient for format. Timing on this host is **4–11 s per scene piece**, roughly flat in
the number of quotations, which is the amortisation ADR-0006 predicted.

**What is not.** Whether a 1B model can do this at all. The Explicit sub-score says
something is still wrong, and the obvious suspects are untested: the `[Qn: ...]` markers may
be confusing it, the system prompt invites `"?"` and it takes that offer 90% of the time,
and no larger model has been tried. **Do not quote 3.0% as the SLM's number.** It is the
number for this prompt, this model, and a harness with a known-suspicious sub-score.

*Next, in order:* fix the Explicit sub-score before anything else — a scene whose tag says
`said Mrs Beaver` must resolve to Mrs Beaver essentially always, and until it does no other
number here means anything. Then a prompt without `"?"` as an easy out, then Qwen 2.5 1.5B
or a 3B, then a wider corpus. `predictors/slm_predict.py --rethreshold` does not exist for
this candidate; scores are not cached, so each variation costs a full run (~20 min/novel).

**2026-09-08 — the leading suspect for the Explicit sub-score is ruled out; the model
itself cannot be run this session, and that is a new, separate blocker.** Two independent
findings, `A Handful of Dust`, no model run required for either.

#### The candidate-list gap is real but small — at most 5.6 of the 40.3 points

The obvious next suspect, unchecked until now: does `scene_cast()` even *offer* the gold
speaker as a choice? The grammar in `grammar_for` restricts every answer to the scene's
candidate list plus `"?"`, so if the correct name is not in that list the model cannot
answer correctly no matter how well it reads.

Measured directly against `build/bakeoff` (no model needed — this only exercises
`cast_of`, `mark_quotations` and `scene_cast` from `predictors/slm_predict.py`):

| | Explicit quotations | gold speaker offered | ceiling on precision |
| --- | ---: | ---: | ---: |
| current `cast_of` (PDNC's own aliases) | 409 | 386 (94.4%) | 94.4% |

So candidate-list omission caps at most **5.6** of the **40.3**-point gap between 51.5% and
Tier 1's 91.8%. Most of the gap is the model choosing wrong among names it *was* offered,
which this check cannot see without running it.

The misses are genuine, and one is worth naming because it is not PDNC noise: `John Andrew`
(a child) is voiced only as `"[Q...] said John,"` in the text; PDNC's alias list for him is
`{Johnny-Boy, John Last, John Andrew}` — no bare `John` — while `John Beaver` (an adult, the
novel's other lead) is explicitly aliased to bare `John`. The scene offers `John Beaver` and
never offers the right answer at all. A human reader disambiguates two Johns by context;
that is exactly the judgement the SLM is supposed to supply, and the harness cannot hand it
a candidate that was never on the list.

**Tried the obvious repair and it does not pay for itself.** Adding each character's own
unclaimed name-tokens as extra aliases (only when no other character already claims that
token, so `John` stays with `John Beaver` and is not duplicated) recovers 3 more
quotations — 95.1% — at the cost of **nearly doubling the average scene cast, 6.8 → 12.4
names, max 24**. That is the shape of the harness fault QUI-028 already found and fixed
once (fault 3, 2026-09-08 entry above: "the whole book's cast offered as candidates"). Not
committed. `scene_cast`'s current candidate lists are tight (avg 6.8, max 17) and that is
not, on this evidence, where the fix lives.

#### What the marked text looks like, for anyone about to guess again

Read three real pieces before proposing another harness fix (transcript kept out of this
entry; reproduce with the script below). One thing worth knowing: **Explicit tags sit
sparsely inside long runs of untagged back-and-forth.** A representative piece asks the
model for 32 quotations in one JSON array; only 2 carry a tag, the other 30 are Tony and
Brenda alternating with no tag at all. The prompt does not distinguish "answer this one
from the tag beside it" from "answer this one by tracking twenty turns of alternation" —
both are the same instruction, same array, same call. If a 1B model's turn-tracking drifts
across the untagged majority, that drift is exactly the kind of thing that could also pull
down an adjacent tagged answer, and nothing here rules that in or out — it wants the actual
model, run scene by scene, with output inspected next to the piece it was asked about.

*Verified clean, not the bug:* the `[Qn: ...]` marker numbers are local to each piece and
are correctly independent of PDNC's own `quoteID` — the scorer joins on `quoteID`
(`question["id"]`), never on the marker number, so this was checked and is not a
mislabelling risk.

#### Blocked: `huggingface.co` and `people.ischool.berkeley.edu`, both organization-policy denials, not transient

CLAUDE.md §9 says Hugging Face has been reachable since 2026-09-06. It is not, in this
session, and the failure is not the CDN-redirect trap that note describes — the top-level
host itself is refused:

```
$ curl -sS -o /dev/null -w '%{http_code}\n' https://huggingface.co/api/models/bartowski/Llama-3.2-1B-Instruct-GGUF
curl: (56) CONNECT tunnel failed, response 403
$ curl -sS http://127.0.0.1:$PORT/__agentproxy/status | grep -A3 huggingface
  { "kind": "connect_rejected", "detail": "gateway answered 403 to CONNECT (policy denial or upstream failure)", "host": "huggingface.co:443" }
```

Retried three times over ten seconds, same result each time — the proxy's own guidance
(`/root/.ccr/README.md`) is explicit that a 403 from the egress proxy is an organization
policy denial and should be reported, not retried or routed around. `people.ischool.berkeley.edu`
(BookNLP's host) is refused the same way. `github.com`, `raw.githubusercontent.com` and
`dl.google.com` are all still reachable, so this is not a general outage — it is these two
hosts specifically, and it blocks both remaining model candidates (the Llama GGUF here and
BookNLP's checkpoint) exactly as it did before 2026-08-28. `HF_HUB_DISABLE_XET=1` and the
Xet-CDN trap are moot: the run never gets past the first request.

**Nothing in this ticket's remaining work can proceed without either a model file already
on disk or this host restored.** Releasing the claim rather than sitting on it blocked.

**Reproduce the candidate-list measurement (no model, no PDNC-license risk beyond the
existing fetch, a few seconds):**

```sh
tools/fetch-pdnc.sh
cd spike/pipeline && gradle installDist
build/install/quire-pipeline-spike/bin/quire-pipeline-spike dump --out build/bakeoff --novels AHandfulOfDust
python3 - <<'PY'
import sys, os, csv
sys.path.insert(0, "predictors")
import slm_predict as sp

dump, novel = "build/bakeoff", "AHandfulOfDust"
corpus = os.path.expanduser("~/.cache/quire/pdnc")
paragraphs = sp.read_jsonl(f"{dump}/{novel}.paragraphs.jsonl")
questions = sp.read_jsonl(f"{dump}/{novel}.questions.jsonl")
pieces = sp.read_jsonl(f"{dump}/{novel}.scenes.jsonl")
cast = sp.cast_of(f"{corpus}/data/{novel}")
gold = {r["quoteID"]: (r["speaker"], r["quoteType"])
        for r in csv.DictReader(open(f"{corpus}/data/{novel}/quotation_info.csv", encoding="utf-8"))}

total = present = 0
for piece in pieces:
    text, inside = sp.mark_quotations(paragraphs, questions, piece)
    if not text: continue
    here = sp.scene_cast(text, cast, [])
    for q in inside:
        speaker, qtype = gold.get(q["id"], ("", ""))
        if qtype != "Explicit": continue
        total += 1
        present += speaker in here
print(f"{present}/{total} ({100*present/total:.1f}%) gold Explicit speakers offered as candidates")
PY
```

*What is left, unchanged from the entry above except that the model files needed to attempt
any of it are, right now, unreachable:* a prompt without `"?"` as an easy out; Qwen 2.5 1.5B
or a 3B to find where capability starts; a wider corpus once one novel looks right; and
inspecting real model output beside the marked scene text once a run is possible, to see
whether the untagged-majority-drift hypothesis above holds up.

**2026-09-09 — the Explicit sub-score is a harness fault after all, and it is the in-text
marker. Fault 5.** `A Handful of Dust`, Llama 3.2 1B Q4_K_M, one novel, the model that
produced the 51.5%. `predictors/explicit_probe.py` asks the same Explicit quotations several
ways and changes one thing at a time, which is what the entry above asks for instead of more
guessing.

The square that matters — same 40 quotations, same scene cast offered in every cell, one
question per call, grammar and temperature identical:

| window given to the model | quotation marked `[Qn: ...]` in the text | unmarked, target words quoted back in the question |
| --- | ---: | ---: |
| the whole scene piece | **32.5%** | **82.5%** |
| its paragraph alone | 80.0% | 87.5% |

**Marking the quotation inside the text costs 50 points of Explicit precision at scene
length.** Window length costs ~5. On the larger paired sample the marker effect holds:
199 quotations, `para` 83.9% against `plain` 89.4%, and 84.9% against **90.6%** among the
quotations whose gold speaker was offered at all — which is Tier 1's 91.8% to within noise.
So a 1B model can read a speech tag. It could not read one *through our marking*.

**I got this wrong once on the way, and the wrong version is the instructive one.** With only
the two marked cells measured, `scene` 32.5% against `para` 80.0% reads as "scene-length
context destroys tag reading", which would have put a hole in ADR-0006 §3 — the claim that a
scene-sized call "is not a speed compromise, it is the better answer". Both of those cells are
*marked*; the comparison conflated the window with how much marked text surrounds the target.
The fourth cell reverses the attribution and ADR-0006 §3 survives intact. **Never read a 2x2
from two cells** — and the missing cell was cheap, 40 calls and five minutes.

**What the failures looked like before the fix**, so nobody has to take this on trust:

```
"Aren't you coming home with us?" said Babs.     -> Jock Grant-Menzies
"He wants a man up," said Ben.                   -> John Beaver
"Yes, I'll go," said Jock.                       -> Tony Last
"You are one for making people learn things,"
                              said Beaver.       -> Tony Last
```

Eleven of eleven are tags a reader answers without pausing, and in every one the right name
was on the candidate list. That is what 32.5% looks like from the inside, and it is why the
sub-score was worth trusting over the headline for a fourth time.

#### The confound I cannot separate, and it decides the fix

The two unmarked cells change two things at once: the marker leaves the text, **and** the
target stops being addressed by marker number and starts being addressed by quoting its words
back. Those co-vary in every cell above, so "the marker is the fault" is really "the marking
scheme — in-text marker plus addressing by number — is the fault". Which half carries the 50
points is untested and it is not academic:

- if the *addressing* is what helps, the batch fix lists each target's opening words in the
  question over unmarked text;
- if the *marker's presence* is what hurts, numbers can stay in the question and merely leave
  the text.

**And the fix is not free either way, because both unmarked cells ask about one quotation per
call.** The shipped form asks for all N in one array, and an array needs the targets addressed
somehow — which is why the markers exist. So this does not reduce to deleting them: it needs an
addressing scheme that survives N targets in one call while keeping ADR-0006 §3's shape. That
is the next piece of work and it is a prompt design question, not a model question.

#### The `"?"` free out is confirmed, separately, and it is the coverage story

In the batch condition the model answered `"?"` for **35 of 40** Explicit quotations — 87.5% —
and dropped no piece on alignment. Forced to one quotation per call it never declined once.
So the 9.4% coverage is the free out being taken, as the 2026-09-08 entry suspected, and it is
a *batch* phenomenon rather than a property of the model.

**One thing this changes about the order of the remaining work.** Removing `"?"` on its own
would have made wrong-voice worse, not better: it raises coverage against whatever precision
the prompt actually sustains, and marked-and-batched that was 32.5%. Fix the marking first,
then remove the out. `wrong voice = coverage x (1 - precision)` is unforgiving of doing those
two in the other order.

#### Numbers, and what they are not

| condition | asked | coverage | precision | prec. among offered |
| --- | ---: | ---: | ---: | ---: |
| `batch` — one call per piece, whole array, as shipped | 40 | 12.5% | 80.0% (n=5) | 80.0% |
| `scene` — one question, scene text, marked | 40 | 100% | 32.5% | 32.4% |
| `scene-plain` — one question, scene text, unmarked | 40 | 100% | 82.5% | 82.5% |
| `para` — one question, paragraph, marked | 199 | 100% | 83.9% | 84.9% |
| `plain` — one question, paragraph, unmarked | 199 | 100% | 89.4% | 90.6% |

Explicit quotations only, one novel, 409 of them in it. `batch`'s precision rests on the five
it did not decline and means nothing on its own; it is in the table for its coverage column.
The window comparison rests on 40, the marker comparison on 199. **No headline SLM number
follows from any of this** — every cell asks one quotation per call, which is not what would
ship, and the untagged three quarters of dialogue are untouched here. `3.0%` accuracy and
`51.5%` Explicit precision remain the numbers for the marked, batched prompt, and both are now
known to be measuring our marking as much as the model.

#### Two faults found in the probe itself while writing it

Worth recording because they are the same species as the five in the harness, and the second
one would have understated the very gap being explained.

1. **Precision printed as correct over *asked*.** That is an accuracy. Declining and answering
   wrongly are opposite problems with opposite fixes, and one denominator hides which is
   happening. Three columns now, as `Bakeoff`'s own doc requires.
2. **A stricter matcher than the harness's.** It scored `Reggie` against gold `Reggie St Cloud`
   as a miss; `Pdnc.matches` counts either name's words containing the other's as a match.
   `Pdnc.matches` and `Pdnc.words` are now ported verbatim, with the reason the usual refusal
   to duplicate a scorer is suspended for a diagnostic.

The probe reads gold, so it lives apart from `slm_predict.py`, which stays gold-blind. It is
never wired into a `bakeoff --candidate` run.

#### The addressing scheme was tried, and it does not rescue the array

Measured rather than assumed, which is the whole habit here. `batch-plain` is the shipped
shape with the marking taken out: one call per piece, unmarked text, every target addressed by
its opening words in the question (lengthened until unique, or two identical `"Yes,"` openers
would ask the same question twice and misalign the array).

| | coverage | precision | `"?"` |
| --- | ---: | ---: | ---: |
| `batch` — marked, addressed by marker number | 12.5% | 80.0% (n=5) | 35/40 |
| `batch-plain` — unmarked, addressed by opening words | 22.5% | 66.7% (n=9) | 31/40 |
| `scene-plain` — same unmarked text, **one question per call** | 100% | 82.5% | 0/40 |

**The array is the fault, not only the marking.** Asked one at a time over exactly the same
unmarked scene text the model declines *never*; asked for the whole array it declines 31 times
in 40, and better addressing moved that by four quotations. So the 50-point marking effect is
real but it is only reachable once the array stops collapsing into `"?"`, and an addressing
scheme alone does not get there.

That makes **removing `"?"` from the grammar the next experiment rather than a later one** —
the ordering argument in the entry above still holds and is now satisfied: the marking question
is answered, so the free out is what is left. With `"?"` gone the model must name somebody, and
the question becomes whether array-form precision holds near `scene-plain`'s 82.5% or collapses
under the drift the 2026-09-08 entry suspected. That is one cheap run and it is the gate.

`batch-plain`'s 66.7% rests on the nine it answered and is not a precision worth quoting; its
coverage column, on all 40, is the finding.

#### The instrument reads true, so here is the product question it was blocking

Explicit quotations are **17.5%** of this novel. `Implicit` — no tag names the speaker anywhere,
so it is answered by tracking turns — is **73.9%**, and it is the only reason a generative model
is in this design at all. It had never been measured. `--quote-type Implicit`, 40 quotations,
the same probe:

| | coverage | precision | prec. among offered |
| --- | ---: | ---: | ---: |
| `scene-plain` — scene context, unmarked, forced to answer | 100% | **37.5%** | 44.1% |
| `plain` — the paragraph alone | 100% | 12.5% | 14.7% |

**ADR-0006 §3 is vindicated a second time.** Scene context tripled precision on the same 40
quotations. Turn-taking is a property of the scene, exactly as the ADR argues from first
principles, and the paragraph window that wins on Explicit collapses here — which is the
sanity check that says this measurement is wired up right.

**And 37.5% is not a product.** `wrong voice = coverage x (1 - precision)` puts this at **62.5%**
on the untagged three quarters, against Tier 1's 2.1% over the whole novel. That is the band
ADR-0005 already rejected the encoders for (41-55%), and for the same reason: confidently wrong
is the failure PRD §3.1 says a reader hears.

**This is the ceiling, not a starting point.** One question per call — which cannot ship, on
ADR-0006 §3's own throughput arithmetic — marking removed, `"?"` unavailable, full scene context,
every affordance the probe can give it. A 1B model does not do this job, and the gap to usable is
roughly fifty points rather than five.

So the honest reading of today: **the sub-score is fixed, the instrument reads true, and its
first trustworthy reading is negative.** Everything measured before this entry was on the 17.5%
Tier 1 already answers at 91.8%, where the best configuration merely draws level with rules that
cost nothing.

One question stands between here and the §8 conversation in
`docs/handoff/2026-09-08-attribution-state-of-play.md`: does the capability exist at *any* size.
Taking Qwen 2.5 **3B** before the 1.5B the handoff lists first, because going straight to the
larger model settles it in one run — and note a 3B Q4 is ~2 GB against PRD §5's 1.2 GB budget, so
even a success there is a device question (ADR-0001, QUI-031), not a candidate. If a 3B also
lands near 40%, §8's fallback is the answer and it has arrived on evidence rather than by
attrition, which is what §8 asked for.

#### A 3x larger model buys 3.5 points, and that settles it

Qwen 2.5 3B Instruct Q4_K_M, the same 40 `Implicit` quotations, the same best-case
configuration — scene context, unmarked, one question per call, `"?"` unavailable in practice:

| model | size | coverage | precision | prec. among offered | correct |
| --- | ---: | ---: | ---: | ---: | ---: |
| Llama 3.2 1B | 808 MB | 100% | 37.5% | 44.1% | 15/40 |
| Qwen 2.5 3B | 1,930 MB | 97.5% | **41.0%** | 47.1% | 16/40 |

**Three times the parameters bought one more correct answer.** At n=40 that is inside the
noise, and it is the *slope* that matters rather than either number: reaching a wrong-voice
rate this product could ship needs roughly fifty more points, and 1B to 3B delivered three and
a half. Nothing in that slope suggests a 7B closes it, and a 7B does not fit the device either —
the 3B is already 1.9 GB against PRD §5's **1.2 GB** RAM budget, so even the success case here
was never a candidate. It is diagnostic only, exactly as it was taken.

Wrong voice at 41.0% precision and full coverage is **59%**, against Tier 1's 2.1% over the
whole novel.

**So the stopping rule in `docs/handoff/2026-09-08-attribution-state-of-play.md` §8 is met, and
met on evidence rather than by attrition.** Its words: *"If no available model takes untagged
dialogue at an acceptable wrong-voice rate, the honest product is multi-voice on tagged
dialogue, narrator elsewhere."* That is now the recommendation, and it is a PRD question rather
than a ticket one — §8 says so, and this entry does not presume the answer.

Three things worth carrying into that conversation:

1. **The fallback is stronger than it sounds.** Tier 1 holds 88.6% precision at 2.1% wrong
   voice, BookNLP `small` buys 11 accuracy points on the Explicit slice for 57 MB (ADR-0005),
   and today's marking fix means a small model *can* read a tag at 90.6% if one is ever wanted
   there. The failure mode is flat, not wrong, which is the trade PRD §3.1 asks for.
2. **ADR-0006 §3 was right twice over** and should not be reverted on the way: scene context
   tripled Implicit precision over a paragraph window, and the fourth cell of the marking square
   cleared it of the crime the first reading charged it with.
3. **Nothing here is a measurement of the design's ceiling with a bigger model on a bigger
   machine** — only of what fits this device. If cloud TTS ever gets its V2 ticket (PRD §6),
   cloud attribution is the same conversation and these numbers do not speak to it.

**Limits, stated plainly.** One novel, 40 quotations per cell, so a standard error near 8
points; one 3B rather than every 3B; `Anaphoric` (8.6%) never measured separately. None of that
rescues a fifty-point gap, which is why this is written as settled rather than provisional.

#### Correction: "rules cannot reach untagged dialogue" was never measured, and it is wrong

Raised in review, and right. Every candidate in this ticket was either a model or
`core:attribution`'s `Heuristic`, and that heuristic has exactly three rules — speech tag,
pronoun tag, action beat — after which it returns `"no tag"` and declines. So what the entries
above establish is that *tag-reading* rules cannot reach untagged dialogue, which is true by
construction and not the same claim at all.

Untagged dialogue is not evidence-free. Prose alternates, and a reader applies the convention
without noticing:

```
Geralt whispered:
"You need to run that way."                       <- Geralt, by the beat before it
"I can't, he'll see me!"                          <- the other one, by alternation
"And I can hear you too!" growled the griffon.    <- the griffon, by its own tag
```

A speaker arriving mid-exchange is named *when they arrive*, which is why alternation and tag
reading compose rather than fight. ADR-0006 calls this "QUI-009's turn-taking fallback"; QUI-009
has not been written, so it had never been scored.

`AlternationCandidate` is it, in the weakest form that could work: within a scene, if the
previous turn's speaker is known and the turn before that is a *different* known speaker, this
turn is the speaker from two back. No cast size, no gender, no model. **Whole corpus, 28 novels,
36,970 quotations:**

| candidate | coverage | precision | the alternation rule alone | wrong voice |
| --- | ---: | ---: | ---: | ---: |
| `tier1` | 26.8% | 84.9% | — | **4.0%** |
| `alternation-adjacent-pairs` | 30.2% | 81.9% | 65.8% (n=701) | **5.5%** |
| `alternation-adjacent` | 36.5% | 77.1% | 56.9% (n=3,043) | **8.4%** |
| `alternation` (anywhere in a scene) | 66.2%* | 54.5%* | 41.1%* | — |
| Qwen 2.5 3B, best case, `Implicit` | ~100% | ~41% | — | ~59% |

\* one novel; the unrestricted variant was not worth a corpus run once the restricted ones were in.

**Two preconditions carry almost all of it, and both are the convention rather than tuning.**

- **Adjoining paragraphs.** A scene here averages ~97 paragraphs, so it is many conversations
  with narration between them, and a chain spanning that gap guesses across a boundary the prose
  drew. Requiring an unbroken run took the rule from 41.1% to 49.7% on one novel.
- **Exactly two established speakers.** "Two turns back" is a two-person convention; in a scene
  where five people have spoken it is a guess, and ADR-0005 prices a guess at the wrong-voice
  rate. Both together reached 86.7% on one novel — **but that was n=30 and did not survive the
  corpus, where it is 65.8%.** Recorded because quoting the 86.7% would be exactly the mistake
  this ticket has made five times.

**What this changes, and what it does not.**

- **It beats the models, decisively.** 65.8% against the 3B's ~41%, in about twenty lines of
  Kotlin, with no model file and no RAM. A 1.9 GB model performing worse than a stated rule is
  the clearest argument in this ticket against the SLM, and it strengthens rather than softens
  the conclusion above.
- **It does not rescue the untagged majority.** At its strictest it fires on 1.9% of quotations
  and the loose variant on 8.2%; `Implicit` coverage goes 2.3% → 6.4%. Most untagged dialogue
  stays out of reach.
- **It widens the shippable slice.** Overall coverage 26.8% → 30.2%, for 1.5 points of
  wrong-voice (4.0% → 5.5%). Whether that trade is worth taking is PRD §3.1's call, and the two
  variants are a dial rather than a switch.

**The headroom is in the preconditions, not the rule.** "Two established speakers" is established
*by tags*, so a crowd scene where only two are tagged reads as a duet; and a speech split across
two paragraphs breaks the alternation with no tag to recover it. Detecting presence in a scene
rather than taggedness, and merging paragraph-split speeches, are both mechanical and both
measurable against this table — a far better bet on this evidence than more model work. That is
QUI-009's proper scope and wants its own ticket.

*Reproduce (no model, seconds per novel):* `bakeoff --candidate alternation-adjacent-pairs`,
also `alternation`, `alternation-pairs`, `alternation-adjacent`, `alternation-tags-only`.

*Next, in order.* An addressing scheme for N targets in one call that is not in-text marking,
and the confound above resolved on the way — that is the fix, and until it lands no SLM
headline is worth quoting — and on the evidence above that fix is removing `"?"`, not a
better addressing scheme, which was tried. Then Qwen 2.5
1.5B and a 3B against the *fixed* prompt, which is a different and much fairer question than
the one yesterday's list assumed. Then the untagged classes, which is where scene context
earns its place and where none of today's numbers reach. The stopping rule in
`docs/handoff/2026-09-08-attribution-state-of-play.md` §8 is unchanged and its clock has not
started: today moved a harness fault, not a model.

**Environment, corrected.** `huggingface.co` and `people.ischool.berkeley.edu` were still 403
at the start of this session, as the entry above records; they began answering 200 mid-session
after the domains were added to the environment's allow-list. Maven Central 429s
intermittently and `gradle installDist` needs a retry loop. The 1B GGUF is 808 MB and
`slm_predict.py --model` now takes a path, so a reclaimed container does not re-download it.

**Reproduce** (needs the model; ~15 min for the marker square, ~25 for all five conditions):

```sh
tools/fetch-pdnc.sh
python3 -m pip install llama-cpp-python huggingface_hub
cd spike/pipeline && gradle installDist
build/install/quire-pipeline-spike/bin/quire-pipeline-spike dump --out build/bakeoff --novels AHandfulOfDust
curl -L -o ~/llama-1b.gguf https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf
python3 predictors/explicit_probe.py build/bakeoff --novels AHandfulOfDust \
    --model ~/llama-1b.gguf --sample 40                       # all five conditions
python3 predictors/explicit_probe.py build/bakeoff --novels AHandfulOfDust \
    --model ~/llama-1b.gguf --sample 200 --conditions para,plain   # the marker effect
python3 predictors/explicit_probe.py build/bakeoff --report-only   # re-read, run nothing
```

Rows are appended and flushed as they are produced and a rerun skips what is there, because
this container is reclaimed every 20-40 minutes. `--sample` may be raised freely; the sample
is a prefix of one seeded shuffle, so a larger run keeps every quotation the smaller one chose.


**2026-09-10 — `quire-dialogue-attribution`.** The published dialogue conventions, all of
them, implemented and measured. And a sixth harness fault, which moves every precision
number this repository has printed.

**Fault 6: quotations were scored by string, not by character.** PDNC ships
`character_info.csv`, an alias table naming everything each novel calls each person, and the
quotation scoring never opened it — only QUI-032's cast scoring did. So predicting `Miss
Lucas` where gold said `Charlotte Lucas` scored as a wrong voice, though both names are
Austen's own for the same woman and a listener would have heard the right one. Folding the
aliases is worth about **8 points of precision to every candidate**:

| | before | after |
| --- | --- | --- |
| Tier 1, whole corpus | 84.9% | **93.2%** |
| Tier 1, `speech tag` rule | 89.9% | **99.0%** |
| Tier 1, `action beat` rule | 61.3% | **66.0%** |

An alias claimed by two characters is dropped rather than trusted — PDNC's sets are
hand-made and leak, and Charlotte Lucas's includes `Lady Lucas`, who is a separate character
in the same file. `Pdnc.Identity`, tested both ways in `PdncTest`.

Read every earlier figure in this ticket and in QUI-018's as ~8 points low on precision.
The *comparisons* between candidates mostly survive, because the fault hit all of them.

**The conventions, and where they come from.** Four are mechanically checkable and all four
are now in `AlternationCandidate`: one speaker per paragraph; two-speaker turn-taking; the
establishment and re-establishment clause (a third speaker or a delay ends the licence to
drop tags); and continued speech, where one speaker runs across a paragraph break taking an
opening quote on each and a closing quote only at the end. The fifth — attribution by voice,
idiolect and knowledge asymmetry — needs a model of the characters and is QUI-009's, not a
rule's.

**First, whether the corpus obeys them.** A rule cannot score above the conformance of the
text it reads, so `bakeoff conformance` measures each convention against gold speakers alone,
with nothing attributed. Whole corpus:

```
one speaker per paragraph                       1230 / 1497    82.2%
a paragraph break means the speaker changed    25914 / 27132   95.5%
A-B-? continues as A                           17705 / 21133   83.8%
an unclosed quotation continues the speaker      180 / 186     96.8%
```

So the prose conforms, and **83.8% is the ceiling on turn-taking**, measured with the two
previous speakers known for certain. That number settles the framing this work started from:
where the rule is wrong, it is overwhelmingly us, not the novel.

**The rule itself, whole corpus, 36,970 quotations.** `wrong voice = coverage x (1 -
precision)` (ADR-0005) is the column that decides.

```
                          coverage  precision  accuracy  wrong voice
tier1-tags-only              22.6%      99.0%     22.4%       0.23%
tier1                        26.8%      93.2%     25.0%       1.82%
alternation                  34.3%      87.5%     30.0%       4.29%
alternation-chain2           31.9%      89.1%     28.4%       3.48%
alternation-strongseats      32.6%      89.1%     29.0%       3.55%
alternation-anyspeakers      37.9%      84.5%     32.0%       5.87%
alternation-anygap           64.0%      65.1%     41.6%      22.34%
alternation-loose            77.7%      60.2%     46.8%      30.92%
```

Headline/holdout for `alternation`: 31.1% against 26.7%, a gap of 4.4 points, next to Tier
1's 26.0% / 22.0% and 4.0. The conventions do not travel worse than tag-reading does.

**Read the last two rows as the answer to "why not just apply the rule everywhere".**
Dropping the delay clause alone (`-anygap`) buys 12 points of accuracy and costs 18 points of
wrong voice. The preconditions are not conservatism bolted onto the rule; they are the rule.

**Two bugs found by measurement, both worth the sentence they cost.**

*The floor survived a decline.* When no rule could name a turn, the machine kept the previous
speaker as the floor holder, so the next tag established a pair with a speaker one turn stale
and then alternated confidently on it. It showed up as the continued-speech rule scoring
**11.0% on 91 fires** — worse than chance, which is what a systematically inverted phase
looks like. Every decline now forgets the floor: 10 fires at **90.0%**. Regression test in
`AlternationCandidateTest`.

*The exchange was only half-checked.* The rule required this turn to adjoin the last, but not
the last to adjoin the one before it, so a pair could be seated across a paragraph of
narration. Requiring the whole run cut alternation from 4,048 fires to 2,215 and moved the
candidate from 39.3% coverage at 75.2% precision to 34.3% at 79.1% — both measured before the
alias fold, so comparable only with each other.

**A structural finding, left open.** Alternation precision alternates with chain depth, and
the odd steps are consistently ~10 points worse than the even ones:

```
alternation (x1)   984   71.6%
alternation x2     456   80.3%
alternation x3     265   64.5%
alternation x4+    510   64.1%
```

The even steps name *the speaker two turns back*, which is right whenever the exchange
returns to them — it survives being wrong about who the partner is. The odd steps have to
name the partner, and in a scene with three people present but only two tagged, that is where
the rule breaks. The `requirePair` clause catches a third speaker who is *tagged*; one who
never is, it cannot see. That is a knowledge-asymmetry problem, which is the fifth convention,
which is QUI-009's.

**What QUI-009 should take.** Not `alternation` as its default. 4.29% wrong voice against
Tier 1's 1.82% is a poor trade at 87.5% precision when the same signal is worth far more as a
*prior*: the conventions say what the speaker probably is at 34.3% of quotations, with a
measured reliability per rule and per chain depth, and a model that is given that alongside
the scene will do better than one asked cold. Ship `tier1` as the confident tier; hand the
alternation answer to the SLM as evidence, not as an answer.

**Not done, and why.** The trailing action beat — `"We don't have enough time." Marcus set
his mug down.` — is still unread, because detecting it needs name detection, which lives in
`core:attribution`'s internals, and CLAUDE.md §9 forbids copying that into a spike. It is
QUI-009's to add where `Names` is already on the classpath. British single-quote setting is
also unread: `'` is an apostrophe far more often than a quotation mark, and PDNC is
double-quoted throughout, so there was nothing here to test a detector against.

Reproduce:

```bash
tools/fetch-pdnc.sh
cd spike/pipeline && gradle installDist && gradle test
B=build/install/quire-pipeline-spike/bin/quire-pipeline-spike
$B conformance                              # the ceilings, from gold speakers alone
$B bakeoff --candidate alternation          # the conventions as the style guides state them
$B bakeoff --candidate alternation-anygap   # and what dropping the delay clause costs
```

Whole-corpus run: about 7 seconds per candidate on the build container. No SLA from PRD §5
is measured here — this is scoring quality on the build machine, and nothing in it is a
device number.

---

## QUI-039 — Listening test: what the wrong-voice rate sounds like

**Status:** In review · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-028, QUI-037
**PRD:** §2 Phase 2 step 3 · **Timebox:** 1 day

### User story
As the product owner, I want to hear the two attribution settings played against each other
on real prose, so that I can set the confidence gates in PRD §2 Phase 2 on something I have
listened to rather than on a percentage I have read.

### Context (why)
QUI-028 finished with a number and a decision it cannot make. Whole corpus, 36,970
quotations:

```
                coverage  precision  accuracy  wrong voice
tier1              26.8%      93.2%     25.0%       1.82%
alternation        34.3%      87.5%     30.0%       4.29%
```

Per novel — PDNC averages 1,320 quotations — that is **330 against 396 lines given the right
character's voice, and 24 against 57 given the wrong one.** ADR-0005 prices a wrong voice as
the expensive failure, and PRD §2 Phase 2 already encodes the same instinct: below 0.65
confidence a line goes to "the most active speaker in the current scene", and below 0.40 to
the narrator. The conventions are a concrete implementation of that middle band, and whether
the band should exist at all is an audio question.

**Nobody has heard either rate.** Every judgement in this repository about wrong voice has
been made from a table. A line the narrator reads is a normal audiobook convention; a line
read by the wrong character is a different kind of wrong, and the whole tradeoff turns on how
much worse it is. That is not knowable from a percentage.

### Description (what)
Two renderings of the same prose, differing only in attribution, produced as audio a person
can play back to back.

The listener hears **A** and **B** without being told which is which, and answers one
question: which would you rather listen to. Nothing is counted by ear.

Two tracks, because they answer different questions:

- **Natural** — a continuous passage taken without regard to what either candidate says.
  This is the everyday experience, and the honest test of "would I even notice".
- **Disagreements** — only the paragraphs where the two candidates differ, each kept in the
  context of the turns around it. This is what the difference sounds like when it happens,
  and it is where the ~1 in 13 quotations that separate the two settings actually live.

Casting is held constant. The same character gets the same voice in both renderings, so the
only variable is who is speaking.

### Requirements (how)
- Owns: `spike/pipeline/src/main/kotlin/quire/spike/listen/` (new package),
  `spike/hostbench/listen.py`, one dispatch line in `spike/pipeline/.../Main.kt`, and a
  section each in `spike/pipeline/README.md` and `spike/hostbench/README.md`.
- **The Kotlin half emits a script, not audio.** Passage selection, both candidates'
  attributions and the narrator fallback are decided in the JVM where they can be tested;
  the Python half only synthesises. This is CLAUDE.md §9's rule about putting logic where a
  test can reach it, and it is why the passage chooser gets unit tests and the renderer does
  not.
- **The passage chooser must not be able to see who is right.** It selects on structure —
  density of dialogue, number of speakers, contiguity — and on a seed, never on gold or on
  agreement with either candidate. A selector that preferred passages where the conventions
  lose would produce a rigged listen, and it would not look rigged.
- **Casting is shared between the two renderings and fixed by name.** Voice assignment is a
  separate question (QUI-011, QUI-032, QUI-037) and varying it here would confound the only
  thing being measured.
- Render with `vits-piper-en_US-libritts_r-medium`, the incumbent from ADR-0002, through
  `spike/hostbench`'s existing sherpa-onnx setup. No new dependency: `sherpa-onnx`, `onnx`
  and `numpy` are already what the voice probes install.
- **Nothing rendered is committed.** The passages come from PDNC and the audio derives from
  them, so §8 applies: audio and scripts are build output, handed to the listener directly.
- Out of scope: rendering on the device, any change to CI, and any change to shipped
  behaviour. This ticket produces evidence, not a setting. The setting is PRD's and the
  rewrite that follows is QUI-009's.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: The two renderings differ only in attribution
  Given one passage and the two candidates
  When both renderings are produced
  Then the text, the cast and the voice of each character are identical between them
  And only the speaker assigned to each line differs

Scenario: Passages are chosen without reference to the answer
  Given the passage chooser and a seed
  When it selects passages
  Then it has read neither the gold speakers nor either candidate's answers
  And the same seed selects the same passages

Scenario: Both tracks are produced
  Given a novel
  When the listening set is built
  Then it contains a continuous unselected passage and a disagreements-only passage
  And each is rendered once per candidate

Scenario: The listener is not told which is which
  Given the rendered set
  When it is handed over
  Then the files are labelled A and B
  And the mapping from A and B to candidate is written down somewhere the listener does not read first

Scenario: A verdict is recorded either way
  Given the listen has happened
  When the result is written into this ticket's Worklog
  Then it says which rendering was preferred, or that no difference was audible
  And it says what that implies for the confidence gates in PRD §2 Phase 2
```

### Worklog

**2026-09-10 — `quire-dialogue-attribution`.** Harness built, audio rendered, and handed to the
product owner. `In review` rather than `Done`: the deliverable is finished and the thing that
closes the ticket is a listen, which is outside this repository.

**What was produced.** Two tracks, each rendered twice, from *Daisy Miller*:

```
natural         61 pieces, 30 spoken,  4 lines differ    9:00 per rendering
disagreements   46 pieces, 20 spoken, 10 lines differ    3:30 per rendering
```

**Daisy Miller was chosen because it is where the two settings diverge most** — coverage 43.3%
against 66.2%, precision 96.2% against 89.8% — so the listen is an **upper bound** on the
difference and not an average. Choosing on divergence is not choosing on who is right, which
is the line the acceptance criteria draw. For contrast, Pride and Prejudice was the first
candidate and had to be abandoned: a random forty-line stretch of it contained **zero**
differences, because Austen tags almost everything and 28 of those 40 lines went to the
narrator under both settings. That is itself worth recording — the conventions are close to a
no-op in heavily tagged prose.

**Four things the output taught me that reasoning had not.** Each was found by reading the
rendered script rather than by thinking about the code, which is the argument for the script
being a file a person can read at all.

1. **The disagreement sampler was quietly rigged against the conventions.** Sampling
   disagreements and merging overlapping windows over-weights *runs*, and a run of consecutive
   disagreements is mostly one inverted alternation rather than several independent mistakes —
   the chain stays out of phase until the next tag re-seats it. The first sample drew a set the
   conventions got **3 of 8** right, against **80.0%** for that same rule across the whole
   novel. Stratifying across the book brought it to **6 of 10**. Had this shipped, the listener
   would have heard a rule perform at half its measured rate and nothing would have looked
   wrong.
2. **Aliases had to be folded before casting.** Tier 1 names a speaker by whatever the tag
   said, so one woman arrived as `Daisy Miller`, `Miss Daisy` and `Miss Miller` and was cast as
   three different voices. It also inflated the disagreement count, because two names for one
   person is not a disagreement. Folded through QUI-028's `Pdnc.Identity`, the cast went from
   9 characters to 5.
3. **PDNC's spans stop inside the quotation marks.** A naive cut left the opening `"` trailing
   the narration and the closing one leading the next piece, and both were read aloud.
4. **Length is the binding constraint on a listen, not fidelity.** The first build was
   honest and useless: 60 quotations of Austen drags in 5,578 words of narration and renders to
   **37 minutes** per rendering, 74 minutes for the pair. Requiring the quotations to arrive
   close together — a structural filter, not a peek at the answers — bought a passage of actual
   conversation at a length a person will sit through twice.

**Not measured.** No PRD §5 SLA. This is host-side rendering for a human to judge; nothing in
it is a device number, and `spike/hostbench/README.md`'s caveat applies — the audio says what
the attribution sounds like, not what the device sounds like.

**What closes this ticket.** A verdict in this Worklog: which rendering was preferred, or that
no difference was audible, and what that implies for the confidence gates in PRD §2 Phase 2.
Until then the answer to "should the conventions ship" remains the one QUI-028 left: not as a
default, and worth far more as a prior handed to QUI-009's model.

Reproduce:

```bash
tools/fetch-pdnc.sh
cd spike/pipeline && gradle installDist && gradle test
build/install/quire-pipeline-spike/bin/quire-pipeline-spike listen
cd ../hostbench && ./fetch-models.sh vits-piper-en_US-libritts_r-medium
python3 listen.py ../pipeline/build/listen/script.json --wav-dir ../../build/listen
```

---

## QUI-040 — TTS on the GPU or the DSP, not the CPU

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-017
**PRD:** §5 · **Timebox:** 3 days

### User story
As a listener, I want synthesis to sound like an audiobook rather than like a speech engine, so
that I finish the book instead of giving up on it.

### Context (why)
Product Leadership's 2026-09-10 memo, directive 1. ADR-0002 accepted Piper `libritts_r` with a
recorded deviation: RTF 0.354 against a 0.15 budget, load 2,524 ms against 800 ms. The listen on
2026-09-10 added the finding the numbers could not: **it is not audiobook quality**, which is the
product's non-negotiable bar. Every engine we screened that might be better is 6–23× Piper's cost
(`spike/hostbench/README.md`), so on the CPU there is nothing left to find.

Every one of those measurements is CPU-only, and every candidate came out of `sherpa-onnx`'s
model zoo. The Snapdragon 750G also has an **Adreno 619 GPU and a Hexagon 694 DSP**, and neither
has ever been asked to do anything.

> **Do not cite our int8 result against this.** Kokoro int8 measuring 2.46× *slower* than fp32
> was a CPU result and the cause was the missing `i8mm`: with no fused int8 kernel the graph pays
> dequantize/requantize around every operator. On the Hexagon HTP int8 is the native path and
> that overhead is the thing that disappears. The finding argues *for* this ticket.

### Description (what)
A measurement of what the GPU and the DSP can actually do on the reference device, and a verdict
on whether any engine better than Piper gets inside the RTF and TTFS budgets on one of them.

### Requirements (how)
- Owns: `spike/ttsbinding/` (the probe and its engine loader), `docs/adr/0002-tts-engine.md`
  (an amendment, not a rewrite), and a new `spike/hostbench` row per candidate screened.
- **Put QNN ahead of NNAPI and SNPE.** NNAPI is deprecated as of Android 15, which is what the
  reference device runs, so targeting it is building on a sunset API. ONNX Runtime's **QNN
  execution provider** targets Hexagon directly and is the live path for new Qualcomm work;
  SNPE is the older SDK. Confirm this before building — it is the kind of fact that moves — and
  if QNN will not load, say so with the error rather than falling back silently to CPU.
- **Report what provider actually ran.** An execution provider that fails to initialise falls
  back to CPU and reports nothing; a 0.354 RTF "on the GPU" that is really the CPU again is the
  most likely wrong answer this ticket can produce. Log the resolved provider per session and
  put it in the Worklog beside every number.
- Candidates, under GPU/DSP only: Piper `libritts_r` first, as the control with a known CPU
  number; then Matcha-TTS and StyleTTS 2, both flow-matching architectures the memo names.
- Measure per candidate per provider: RTF, load time, peak RSS, on-disk size, and **whether it
  sounds better than Piper** — judged by ear on the device, because that is the bar and no
  number stands in for it.
- Out of scope: cloud synthesis (QUI-042), and any change to what ships before this reports.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: The provider that ran is named
  Given a benchmark row
  When it is recorded
  Then it names the execution provider that actually initialised, not the one requested

Scenario: The control is measured first
  Given Piper libritts_r, whose CPU numbers are known
  When it runs on the GPU or the DSP
  Then its RTF and load time are stated against the 0.354 and 2,524 ms already recorded

Scenario: A candidate is judged by ear, not only by RTF
  Given a candidate that meets RTF 0.15 on some provider
  When it is assessed
  Then the Worklog says whether it sounds better than Piper on the reference device

Scenario: A negative result closes the question
  Given no provider and no candidate reaches audiobook quality inside the budgets
  When the ADR amendment is written
  Then it says so plainly, and says what that leaves for QUI-042
```

---

## QUI-041 — Encoder attribution: the 110M joint-scoring model

**Status:** Todo · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-028
**PRD:** §2 Phase 1 · **Timebox:** 4 days

### User story
As a listener, I want the three quarters of dialogue that carries no speech tag to reach the
right character, so that a conversation sounds like a conversation rather than like a narrator
occasionally interrupted.

### Context (why)
Product Leadership's 2026-09-10 memo, directive 2. QUI-028 closed the generative route on
measurement: 37.5% precision at 1B, 41.0% at 3B, and the 3B is 1.9 GB against a 1.2 GB working
set. Rules reach 26.8% of quotations at 93.2% and 56.5% of quotations get no answer from any
rule at all.

The literature reports a BERT-class encoder with joint scoring at **94.5% on PDNC**, 20× faster
than standard methods and over 1000× faster than LLM approaches
([arXiv:2608.02359](https://arxiv.org/abs/2608.02359)). A bidirectional encoder reads the whole
window at once instead of generating token by token, so ~110M parameters at fp16 is roughly
220 MB and milliseconds of work — and it needs no int8, which on this SoC is the trap
(QUI-040). **If it transfers, the attribution wall is gone.**

### Description (what)
The encoder, scored on the same 36,970 PDNC quotations every other candidate was scored on, and
then on prose PDNC does not contain, with a verdict on whether it runs inside the import budget.

### Requirements (how)
- Owns: `spike/pipeline/predictors/` (a new predictor), and `docs/adr/0005-attribution-model.md`
  (an amendment recording what this does to the decision).
- **Score it through the existing harness and nothing else.** `bakeoff --candidate <name>
  --answers DIR` already takes any outside predictor's TSV, and `dump` already writes paragraph
  text, quotation offsets and scenes with **no gold speakers**, so a predictor cannot score
  itself. Use both. A second scoring path is how the measured Tier 1 and the shipped one quietly
  stop agreeing.
- Report coverage, precision and **wrong voice** = coverage × (1 − precision), split by PDNC's
  `quoteType`, against the table in QUI-028's Worklog. Note that those figures are
  alias-folded; a predictor naming a character by an alias the novel uses is not wrong.
- **Measure out-of-domain separately and treat it as the real number.** PDNC stops at 1934 and
  is weighted to literary fiction. `Holdouts.External` is the empty slot for books unlike it;
  filling it needs a decision, because CLAUDE.md §8 forbids committing book text.
- Then the budget: export to ONNX fp16, and measure load time, peak RSS and wall-clock for a
  100k-word novel **at import**, not during playback — import is where attribution runs
  (`BookImport`), and it has seconds to spend rather than milliseconds.
- Out of scope: replacing Tier 1. Tier 1 is 93.2% precise and free; this fills what it declines.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Scored on the same questions as everything else
  Given the encoder's answers
  When they are scored
  Then they go through bakeoff --answers over all 36,970 quotations
  And coverage, precision and wrong voice are stated beside QUI-028's table

Scenario: The published number is either reproduced or not
  Given the paper reports 94.5% on PDNC
  When our measurement is written up
  Then it says what we got and, if it differs, what we think differs about the setup

Scenario: Out-of-domain degradation is quantified
  Given books unlike PDNC
  When the encoder is scored on them
  Then that figure is reported apart from the PDNC one and called the expected real-library number

Scenario: It fits the import budget, or does not
  Given the ONNX fp16 export
  Then load time, peak RSS and wall-clock for a 100k-word novel are recorded against QUI-007's budget

Scenario: The decision is recorded either way
  Given the result
  When ADR-0005 is amended
  Then it says whether the SLM keeps any attribution job at all
```

---

## QUI-042 — Bring-your-own-key cloud voices

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-010
**PRD:** §6 (V2 scope, brought forward by the 2026-09-10 memo) · **Timebox:** 5 days

### User story
As a reader who cares more about voice quality than about staying offline, I want to point Quire
at my own speech API key, so that I get studio-grade narration without waiting for a 2020 SoC to
become capable of it.

### Context (why)
Product Leadership's 2026-09-10 memo, directive B. This reverses a standing rule, so it is worth
stating exactly what changes and what does not.

**CLAUDE.md §8 forbids adding cloud services without a ticket and an ADR, and PRD §6 puts cloud
TTS in V2.** The memo is the authorisation; this ticket and its ADR are what §8 asks for. The
privacy mandate is not being dropped — the offline tier remains the default and the only tier
that needs no configuration.

What makes this tractable now is that **indexing already happens locally and stays local**
(`BookImport`). The cloud tier is a synthesis backend, so what would leave the device is the
text of the line being spoken — not the book, not the index, not the cast. That distinction is
the whole design and the ADR has to be explicit about it.

### Description (what)
A second synthesis backend, off by default, configured with a user's own endpoint and key, that
the player can use in place of the local engine. Plus the ADR recording the reversal.

### Requirements (how)
- Owns: `docs/adr/0010-cloud-voices.md` (new), a synthesis-backend seam in
  `spike/ttsbinding/TtsEngine.kt`, and the settings screen in `MainActivity.kt`.
- **Write the ADR first and land it separately.** It must say: what leaves the device and what
  never does; that the offline tier stays the default; what happens to a reader's key; and what
  the failure mode is when the network is gone mid-chapter.
- **One provider-shaped contract, not one integration per vendor.** Endpoint, key, voice
  identifier, and an audio format. The memo names ElevenLabs, OpenAI and Sesame CSM; a custom
  endpoint wrapper has to be as well supported as any of them, or this becomes three
  integrations to maintain.
- **A key is a secret and the logs are not.** Never log it, never put it in a crash report, and
  keep it out of anything `adb logcat` can see. Store it where Android stores secrets.
- **Falling back must be audible, not silent.** A dead network mid-chapter drops to the local
  engine; the reader hears the voice change and that is correct, because pausing the book is
  worse. Say so in the UI once, not per line.
- **Latency is the real risk, not quality.** A per-line round trip against an 800 ms TTFS budget
  needs the ring buffer reading ahead, and a cost per book the reader can see before they start.
  Measure both.
- Out of scope: Quire-hosted inference, any key we supply, and sending anything but the current
  line.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: The offline tier is untouched
  Given no key configured
  When a book is read aloud
  Then behaviour is byte-for-byte what it was before this ticket

Scenario: What leaves the device is only the line being spoken
  Given a configured cloud backend
  When a chapter is synthesised
  Then the request carries the line's text and nothing identifying the book, the reader or the cast

Scenario: A key never reaches a log
  Given a configured key
  When the app logs, crashes, or is inspected with adb logcat
  Then the key does not appear

Scenario: Losing the network does not stop the book
  Given playback on the cloud tier
  When the network goes away
  Then synthesis continues on the local engine and the reader is told once

Scenario: The reader knows what it costs before starting
  Given a book and a configured provider
  When the reader enables the cloud tier for it
  Then an estimated character count and cost are shown first

Scenario: The reversal is recorded
  Given ADR-0010
  When I read it
  Then it states what CLAUDE.md §8 and PRD §6 said, who authorised the change, and what stayed
```
---

## QUI-029 — Unindexed books and non-EPUB formats

> **Deferred by decision, 2026-08-28.** Not a V1 blocker: the failure shape is already
> correct, just silent. Revisit once the main EPUB path works well. Written down now so it
> is tracked rather than remembered.

**Status:** Deferred · **Owner:** — · **Epic:** Companion · **Depends on:** QUI-025
**PRD:** §2 Phase 1

### User story
As a reader, I want to know why a book is being read in a single voice, so that I do not
assume Quire is broken when it is simply working on a book I never imported.

### Context (why)
The companion app imports EPUBs. Everything else — a PDF, a `.mobi`, an EPUB the reader
never imported — produces no index match, so every chunk falls to the narrator and Quire
behaves as an ordinary TTS engine. That is the right failure: the reader is no worse off
than before installing Quire.

It is silent, though. Nothing distinguishes "this book has no dialogue" from "you never
imported this book". PDFs in particular are heavily read on the reference device, and PDF
text extraction is its own problem — the QUI-020 capture showed words split across line
breaks and doubled spaces that EPUB never produces (ADR-0004).

### Description (what)
Some way for the reader to find out that the book they are listening to is not indexed, and
a decision on whether non-EPUB formats are ever indexed at all.

### Requirements (how)
- Owns: `app/companion/` unindexed-book surfacing; any format support that follows
- Decide between: a notification on first unmatched session; a companion-app list of
  "books heard recently that are not indexed"; or accepting the silence and documenting it.
- If PDF indexing is ever in scope it needs its own normalisation rules — hyphenation
  across line breaks, running headers and footers, doubled spaces — and its own ticket.
- Must not nag: a reader who deliberately listens to unindexed material should not be
  interrupted repeatedly.
- Out of scope until this ticket is picked up: everything above.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: An unindexed book still reads aloud
  Given a book that was never imported
  When it is read aloud
  Then every chunk is spoken in the narrator voice with no error and no silence

Scenario: The reader can find out why
  Given a session on an unindexed book
  When the reader looks in the companion app
  Then that book is identifiable as unindexed

Scenario: Quire does not nag
  Given a reader who listens to unindexed material repeatedly
  When they do so
  Then they are not interrupted on every session
```

### Worklog
- _(empty)_

---

## QUI-030 — Whole-sentence synthesis with fragment serving

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-012, QUI-027
**PRD:** §3.2

### User story
As a listener, I want a sentence to sound like a sentence, so that a comma does not land
like a full stop and the clause after it does not restart oddly.

### Context (why)
Measured on device (ADR-0002): the host splits at commas, so each clause arrives as its own
`onSynthesizeText` call, and **every engine tested synthesises it as a standalone
utterance** — sentence-final intonation on a fragment, and a strange re-entry on the clause
that follows. Kitten, Piper and Kokoro all did it. This is not an engine defect; it is what
interception does to prosody by construction, and it is the single most audible flaw in the
prototype.

The fix is available to Quire and to nothing else on the market. A reader app has no index;
a plain TTS engine has no idea what comes next. We have both.

### Description (what)
When the first clause of a sentence arrives, synthesise the *whole* sentence from the index
entry, cache the audio, and return only the portion the arriving clause covers. Subsequent
clauses of that sentence are served from the cache. The listener hears one continuously
intoned sentence, delivered in pieces.

### Requirements (how)
- Owns: `core/tts/sentence/`, the fragment-serving path in `app/ttsservice/`
- Keyed by index `seq`; evicted once the cursor has passed the entry.
- Needs QUI-027's normalised-to-raw offset map to know where in the audio a clause begins
  and ends. Without it the cut points are guesses.
- A cache miss must never stall: an unmatched or unlocated chunk falls back to synthesising
  the fragment alone, which is today's behaviour and today's prosody.
- Interacts with multi-voice (QUI-024): a sentence containing both narration and dialogue
  must still be synthesised per voice span, so the unit cached is the span, not always the
  whole sentence.
- Measure the memory cost: one sentence of audio at 22,050 Hz mono 16-bit is roughly 40 KB
  per second, and the buffer already holds several.
- Out of scope: cross-sentence prosody.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: A sentence split at its commas sounds continuous
  Given an indexed sentence of three comma-separated clauses
  When the host requests each clause in turn
  Then the audio returned is the corresponding part of one synthesis of the whole sentence
  And no clause carries sentence-final intonation except the last

Scenario: Later clauses cost nothing
  Given the first clause of a sentence has been served
  When the second clause arrives
  Then it is served from cache without a further synthesis call

Scenario: A miss degrades rather than stalls
  Given a chunk the matcher could not place
  When it is synthesised
  Then the fragment is spoken on its own with no added delay

Scenario: Mixed sentences keep their voices
  Given a sentence containing narration and one character's speech
  When it is served clause by clause
  Then each clause is spoken in the voice of the span it belongs to

Scenario: The cache does not grow without bound
  Given a chapter played to its end
  When cache occupancy is sampled
  Then entries behind the cursor have been evicted
```

### Worklog
- _(empty)_

---

## QUI-031 — SLM runtime bake-off and co-residency

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-006
**PRD:** §3, §5 · **Timebox:** 3 days

> Split out of QUI-017 on 2026-08-29. QUI-017 bundled the SLM and TTS bake-offs because
> co-residency was thought to couple them. The TTS half finished first and ADR-0002 is
> accepted; this is the half that has not started, and it cannot start until QUI-006 gives
> us a runtime to measure.

### User story
As a team, I want the attribution model measured on the reference device beside the TTS
engine, so that we know whether a book can be scanned in the time and power we have, and
whether the two models can be resident at once.

### Context (why)
`device-profile.md` §2 works out that a quantized 1B SLM on a Snapdragon 750G without i8mm
lands in the *hours* for a novel, and everything in `architecture.md` §5 — KV-cache reuse,
single-token generation, Tier 1 coverage as a performance feature — exists to fight that.
None of it is measured. QUI-028 has since shown Tier 1 resolves far less than hoped —
**26.8% of dialogue, at 84.9% precision** — so the SLM carries more of the load than the
architecture assumed, not less. (Re-measured 2026-09-02; the earlier 58.5% and "one line
in nine" were artefacts of a scorer that saw 7.7% of the corpus.)

ADR-0002 also leaves this ticket a harder budget than it expected. The TTS engine is
accepted at RTF 0.354 with peak RSS 314 MB, so the SLM's share of the 1.2 GB ceiling is
what remains after that, and its share of ≈1.14 W competes with an engine already spending
more of it than planned.

### Description (what)
The SLM half of the original bake-off: each candidate runtime measured on the device for
load time, peak RSS and throughput, the two models measured together, and ADR-0003 written
to choose whole-book, chapter-ahead or co-resident attribution on the numbers.

### Requirements (how)
- Owns: `spike/slmbench/`; writes `docs/adr/0003-attribution-timing.md`
- Measure on the physical Note Air5 C, not an emulator and not the host. `spike/hostbench`
  exists for TTS and its README records why host numbers do not transfer to this SoC —
  the same caveat applies here, and more strongly, because quantized inference is exactly
  where the missing i8mm bites.
- Peak RSS is measured with the TTS engine loaded, since that is the configuration that
  has to fit.
- Out of scope: which model wins on accuracy (QUI-028), and the attribution logic itself.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Every SLM candidate is measured
  Given the bake-off harness on a mid-tier e-ink device
  When it runs to completion
  Then it reports load time, peak RSS and throughput for each SLM candidate

Scenario: The co-residency question is answered
  Given an SLM and a TTS engine loaded simultaneously
  When peak resident memory and sustained power draw are measured
  Then both are recorded against the 1.2 GB and 1.14 W budgets
  And ADR-0003 selects whole-book, chapter-ahead or co-resident attribution on that basis

Scenario: KV-cache reuse is quantified
  Given 50 consecutive dialogue lines from one chapter
  When they are attributed with a fresh context window each, and again with one rolling context
  Then both wall-clock times are recorded
  And the ratio is stated as a projected whole-book scan time against the 30 minute budget

Scenario: A candidate that fails is reported, not worked around
  Given a candidate that misses its SLA on the target device
  When results are written up
  Then ADR-0003 states the miss plainly rather than proposing a heavier device
```

### Worklog


---

## QUI-032 — Voice descriptor in `characters.json`

**Status:** In review · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-005
**PRD:** §4.2 · **ADR:** [0007](docs/adr/0007-voice-is-a-description.md)

### User story
As a reader, I want each character's voice to be *described* rather than numbered, so that
the drawer can tell me why Geralt sounds the way he does and let me change it in words I
understand.

### Context (why)
ADR-0002 makes casting a speaker id into one 904-voice model, and QUI-011 currently treats
that id as the entirety of a voice. The upfront scan has read the book and knows far more —
ADR-0006 makes voice design (job C) a first-class job that runs in the scan, and ADR-0007
records that a voice has four axes of which the id is one.

`characters.json` is a frozen fan-out seam (CLAUDE.md §2.3), so the field lands early,
small, and before QUI-007 and QUI-011 are built against the old shape.

### Description (what)
The character manifest gains an optional `voice` object on every character and on the
narrator, carrying the speaker id, the espeak variant, a rate, a target F0 and a prose
description, plus whether a human or the scan chose it. Nothing consumes it yet; this
ticket freezes the shape.

### Requirements (how)
- Owns: `docs/schema/characters.schema.json`, and the manifest model and round-trip test in
  `core/` that QUI-005 landed.
- Shape exactly as ADR-0007 §Decision 1. Every field optional.
- No `schemaVersion` bump: the schema's `additionalProperties: true` round-trip rule
  already covers readers that predate the field, and a bump would force QUI-005's store to
  migrate for a purely additive change.
- `source` is `auto` or `user`. An unrecognised value degrades to `auto`, matching how
  `gender` degrades to `unknown`.
- `espeakVoice` is stored as written and **not validated against a list**. The resolvable
  identifiers come from the model's bundled `espeak-ng-data`, which is a property of the
  model file, not of the schema.
- Out of scope: writing descriptors (QUI-007 job C), consuming them (QUI-011), whether
  accents are shippable (QUI-033).

### Acceptance criteria (Gherkin)
```gherkin
Scenario: A manifest with voice descriptors round-trips
  Given a manifest whose characters carry a voice object
  When it is written and read back by the manifest store
  Then every voice field survives unchanged

Scenario: A manifest without voice descriptors still loads
  Given a manifest written before this ticket
  When it is loaded
  Then it loads without error and every character's voice is absent

Scenario: An older reader preserves the field
  Given a manifest carrying a voice object
  When it is loaded by a reader that does not know the field and written back out
  Then the voice object is still present in the output

Scenario: An unrecognised source degrades
  Given a character whose voice source is "imported"
  When the manifest is loaded
  Then the source reads as auto and the load does not fail
**2026-09-02 — session-visibility-check (not claimed; context only)**

[ADR-0008](docs/adr/0008-analysis-runs-on-device.md) puts this ticket on the critical path:
cloud analysis was costed and rejected, so on-device is the only route to the accuracy the
product needs, and this bake-off decides whether that route exists.

One thing to carry into the design, because it moves the target by an order of magnitude:
**do not budget for one model call per line.** ~3,000 unresolved quotations per novel against
QUI-007's 30-minute budget is ~2 decisions/second on a 750G, which is not reachable. One call
**per scene** — cast in context, list of speakers returned — amortises nearly all the
prefill, and is also the better answer for quality, since a model that sees the whole scene
resolves turn-taking from context rather than guessing line by line. Measure per-scene
throughput, not per-line.


---

## QUI-034 — Cast discovery precision on real books

**Status:** In review · **Epic:** Spike · **Owner:** session-visibility-check · **Depends on:** QUI-008

### User story

As a reader who has just imported a novel, I want the cast Quire shows me to be the people
in the book, so that I trust it enough to press Read Aloud.

### Context (why)

Importing a real novel (*The Witcher*, 2026-08-31) produced a cast of **157 characters**.
Almost none of them were people. That is the first thing a reader sees after an import, and
it is the screen the whole product hangs off — a roster that is visibly nonsense makes the
voices that follow unbelievable before a word is spoken.

Attribution accuracy hid this completely. QUI-028 scored Tier 1 at 58.5% precision on PDNC
and said nothing about the roster, because invented names are never in speech-tag position:
they cost nothing on the attribution metric and everything on the screen. The cast needed
its own number.

Fixtures could not provide it. `fixtures/attribution/*.tsv` is hand-written to exercise the
rules, so it contains no adverb opening a sentence, no interjection opening a line of
speech, and none of the density of capitalised nouns real prose carries. PDNC ships
`character_info.csv` — every character in 28 novels, with aliases and gender — which is
exactly the ground truth this needs (`docs/prior-art.md` §3; evaluation only, never
committed).

### Description (what)

`spike/pipeline` gains a `cast` command that scores `Roster.scan` against PDNC's character
lists: how many of the cast it finds, how much of what it reports is invented, and whether
the genders it infers are right. Two defects the measurement exposed are fixed in
`core:attribution`, so a book imported on device reports a cast of plausible size.

### Requirements (how)

- `spike/pipeline`: `Pdnc.cast`, `Pdnc.characters`, and a `cast` subcommand in `Main.kt`.
  Recall is scored against PDNC's `major` and `intermediate` characters only — a one-line
  footman does not need his own voice. Name matching reuses the existing word-subset rule.
- `core/attribution/Names.kt`: `withoutQuotedText`, blanking quoted spans.
- `core/attribution/Roster.kt`: use it on the adjacency context; retune `ADJACENCY_MIN`.
- Out of scope: alias merging (`Elizabeth` and `Miss Bennet` still count as two), raising
  gender coverage, and anything model-based — see QUI-006/QUI-031.
- No new dependency. PDNC is cloned by the person running the command and never committed.

### Acceptance criteria (Gherkin)

```gherkin
Scenario: The cast is scored against real novels rather than fixtures
  Given a local clone of PDNC
  When `quire-pipeline-spike cast pdnc/data/*` is run
  Then it reports found, real, precision, recall and gender accuracy per novel and overall

Scenario: A word that opens a line of speech is not admitted as a character
  Given a paragraph of back-and-forth dialogue with no speech tags
  When the roster is scanned
  Then the first word of each quotation is absent from the cast

Scenario: Precision on real prose is measured and improved
  Given the 28 PDNC novels
  When the cast is scored before and after this change
  Then precision rises and both numbers are recorded in the Worklog

Scenario: Recall on the characters that matter is not sacrificed for it
  Given the same 28 novels
  When recall over major and intermediate characters is scored
  Then it stays within 6 points of the pre-change number, and the exact cost is recorded
```

### Worklog

**2026-09-02 — fix-tickets-ownership.** Landed the shape exactly as ADR-0007 §Decision 1:
`speakerId`, `espeakVoice`, `lengthScale`, `targetF0Hz`, `description`, `source`, all
optional, on `docs/schema/characters.schema.json` and on `Character.voice` in
`core/model/src/main/kotlin/quire/model/characters/Manifest.kt`. `ManifestCodec` reads and
writes the object; an absent `voice` is omitted entirely rather than written as an empty
object, so a manifest nobody has designed voices for is byte-for-byte what it was before
this ticket.

`espeakVoice` is stored as written and never validated, per the ticket's own requirement —
the resolvable identifiers live in the model's bundled `espeak-ng-data`, not in this schema.
`source` degrades an unrecognised value to `auto`, the same pattern `gender` already uses.
`Voice` carries its own `extras`, so a field a *future* ticket adds inside `voice` survives
a round trip through this reader too, not just fields beside it — the schema's
`additionalProperties: true` promise applies one level deeper now.

No `schemaVersion` bump, as the ticket requires: purely additive, and the round-trip rule
already covers it.

Four new tests in `ManifestTest.kt`, one per Gherkin scenario. Reproduce with:

```bash
./gradlew :core:model:test
```

All 11 tests in `ManifestTest.kt` pass. Also ran the full JVM suite (`./gradlew test`) to
confirm nothing downstream broke — `Character`'s only other consumer,
`core/attribution/Roster.kt`, uses named arguments and was untouched.

Status set to `In review`: this is a schema/model change with no consumer yet
(QUI-007 writes it, QUI-011 reads it), so nothing exists to exercise it end-to-end.

---

## QUI-033 — Accent: listening test and per-character variants

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-032
**PRD:** §4.2 · **ADR:** [0007](docs/adr/0007-voice-is-a-description.md) · **Timebox:** 2 days

### User story
As a reader, I want a character who is written as Scottish to sound Scottish, so that the
cast is distinguishable by more than timbre.

### Context (why)
ADR-0007 establishes by measurement that the espeak-ng variant baked into the Piper ONNX
metadata reaches the model: six English accents ship inside `libritts_r` already, at zero
footprint, and each one demonstrably changes the phoneme stream. `spike/hostbench/voiceprobe.py`
proves it deterministically — the `en-us` control comes back bit-identical through the same
patch path, so the differences are the variant and not the patching.

What the probe cannot say is whether it *sounds* like an accent. `libritts_r` was trained on
en-US phonemes; Scots phonemes are out-of-distribution input, and the result may be a
Scottish reader or an American one falling over. Durations and F0 cannot tell those apart.
Only ears can, and this is the cheapest high-value listen available.

There is also a blocking engineering question ADR-0007 could not answer: sherpa-onnx exposes
no runtime override for the variant, so a per-character accent currently implies one loaded
engine per accent — 2,524 ms load and 314 MB RSS each (ADR-0002), which fits nothing.

### Description (what)
Synthesise the same lines through each shipped variant, listen to them on the reference
device, and record which are usable. Separately, determine whether one loaded engine can
switch variant between utterances; if it cannot, say what accents would actually cost.

### Requirements (how)
- Owns: `spike/hostbench/voiceprobe.py` (already landed), any WAV export added to it, and
  the device-side listening procedure recorded in this ticket's worklog.
- Use the same speaker id across variants, so what changes is pronunciation and not timbre.
- Listen on the Note Air5 C through its own speaker, not on desktop headphones. A phoneme
  artefact that is obvious on studio monitors may be inaudible on an e-reader, and the
  reverse.
- Judge each variant on one axis only: **would a reader accept this as that accent, or does
  it sound broken?** Not "is it a good accent".
- For the runtime question, exhaust the cheap options before concluding: sherpa-onnx's VITS
  config (`lexicon`, `data_dir`, `dict_dir`), whether two `OfflineTts` instances over the
  same weights share memory, and what a metadata patch plus reload actually costs on device.
- Out of scope: non-English variants; training or fine-tuning anything; shipping accents,
  which needs its own ticket once this reports.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Every shipped variant is heard
  Given the six English espeak variants bundled with libritts_r
  When the same lines are synthesised through each and played on the reference device
  Then the worklog records, per variant, whether it is usable or sounds broken

Scenario: The out-of-distribution risk is answered plainly
  Given the listening results
  When they are written up
  Then ADR-0007 is amended to say whether accent is a shippable axis or is dropped
  And a negative result is recorded as a negative result, not worked around

Scenario: The per-character cost is known
  Given an attempt to switch espeak variant on a loaded engine
  When the cheap options are exhausted
  Then the worklog states the achievable mechanism and its cost in load time and RSS
  And if none is affordable, ADR-0007 records that accent is per-book rather than per-character
```

### Worklog

**2026-09-06 — listened on the reference device. Negative result; the axis is dropped.**

Six espeak variants, same speaker, same sentence, played through the Boox's own speaker.

| Variant | Verdict |
| --- | --- |
| General American | Perfect. The model's own training distribution. |
| New York City | Clean, but not recognisably the accent. |
| RP | Not accurate. |
| West Midlands | Roughly acceptable. |
| Lancashire | Roughly acceptable. |
| Scots | Audible stall of about a second on /a/. Broken. |
| Caribbean | Vowels sound skipped. Broken. |

Two separate problems, and either alone would be enough. **Artefacts:** Scots stalls on a
vowel and Caribbean drops them, which is what feeding out-of-distribution phonemes to an
`en-US`-trained model looks like. **Fidelity:** even the variants with no artefacts are not
convincingly the accent they claim, so there is nothing to salvage by fixing the artefacts.

This is the outcome ADR-0007 pre-registered as possible: the phoneme stream demonstrably
changed, and the change does not sound like an accent. The measurement was right and the
inference from it would have been wrong. Only the listen could tell them apart.

**Closed as Done with a negative result, per this ticket's own third scenario.** The
per-character variant question is moot — there is nothing worth making per-character — so
it was not investigated further. ADR-0007 is amended.

*If accent is ever revisited* it needs a model trained on the target accent, not a
phonemiser swap on this one. That is a different engine and a different ADR.

---

## QUI-035 — Gender coverage for the inferred cast

**Status:** In review · **Epic:** Spike · **Owner:** — · **Depends on:** QUI-034

### User story

As a reader of a book with a mostly male cast, I want the men to sound like men, so that the
voices carry information instead of contradicting the page.

### Context (why)

QUI-034 measured gender inference across PDNC's 28 novels: when the scan claims a gender it
is right **90.9%** of the time, but it claims one for only **58.7%** of the real cast. The
other 41.3% arrive at `Casting` as `Gender.UNKNOWN`, where `candidatesFor` finds no pool and
`spreadOverIds` picks a voice of arbitrary sex — distinct, but as likely wrong as right.

This is the device report from 2026-08-31 explained: *The Witcher*'s men read in women's
voices. It is not a casting bug — QUI-011's pools work — it is that most of the cast never
reaches them. Accuracy is already good enough to ship; coverage is what is missing.

### Description (what)

More of the cast arrives with a gender. Afterwards the same PDNC measurement reports
materially higher coverage without giving back accuracy, and a book whose characters are
all one sex is not voiced as a mix.

### Requirements (how)

- `core/attribution/Roster.kt` — the evidence currently counted is one pattern: a pronoun
  standing in for a name later in the same paragraph, gated by `GENDER_MIN` and
  `GENDER_MAJORITY`. Candidate sources of more: titles already in `Names.TITLES`
  (`Mr`/`Mrs`/`Miss`/`Lady`/`Sir` decide it outright), possessives (`Geralt's sword … his`),
  and pronouns in the paragraph *after* the one that names the character.
- Retune `GENDER_MIN` and `GENDER_MAJORITY` against the corpus rather than by argument;
  they were chosen before there was anything to tune them on.
- `spike/pipeline cast` already reports the `gknown` column. Both numbers move together or
  the change is not an improvement.
- Out of scope: what to do with a character who genuinely has no gender in the text, and
  any model-based inference (QUI-006/QUI-031).

### Acceptance criteria (Gherkin)

```gherkin
Scenario: Coverage rises without costing accuracy
  Given the 28 PDNC novels
  When `quire-pipeline-spike cast pdnc/data/*` is run
  Then gender coverage is materially above 58.7%
  And gender accuracy is not below 90.9%

Scenario: A title decides a gender on its own
  Given a character the book only ever calls "Mrs. Bennet"
  When the roster is scanned
  Then she is FEMALE without needing a pronoun

Scenario: A single-sex cast is voiced as one
  Given a book whose named characters are all men
  When the cast is voiced
  Then no character is cast from the female pool
```

### Worklog

**2026-09-02 — session-visibility-check (not claimed; read before starting)**

**This ticket's framing may be obsolete.** It exists because `Casting` uses gender to select
a *pool* of speakers, so a character with no gender gets a voice of arbitrary sex. Under
[ADR-0009](docs/adr/0009-voices-are-generated.md) there are no pools: a voice is generated
from a description, and gender becomes one input to a pitch and timbre target rather than a
selector.

The underlying problem is real either way — 58.7% coverage means most of the cast reaches
casting with nothing said about how they sound. But "infer a binary gender for more of the
cast" may be the wrong shape of fix, and "infer a pitch and timbre target for more of the
cast" the right one, which would subsume this ticket. Settle that before writing code.

### Worklog

**2026-09-10 — `quire-dialogue-attribution`.** Two changes to `Roster.scan`, measured over
PDNC's 28 novels and 742 real characters:

```
                      coverage  accuracy   right sex heard
before                   52.6%     91.1%             71.6%
+ titles decide sex      78.8%     91.6%             82.8%
+ GENDER_MIN 2 -> 1      83.7%     91.4%             84.7%
```

The third column is the one a listener hears: a character with no gender is voiced
arbitrarily and so is wrong about half the time, which makes it
`coverage x accuracy + (1 - coverage) x 0.5`. **It goes from 71.6% to 84.7%** — roughly a
46% cut in characters heard in the wrong sex. Accuracy is flat to +0.3, so the acceptance
criterion (coverage rises without costing accuracy) is met on both numbers.

**1. A title settles the sex outright and outranks the pronoun vote.** `Mr`, `Sir`, `Lord`,
`Uncle`, `Father`; `Mrs`, `Ms`, `Miss`, `Lady`, `Aunt`, `Mother`. These were already in
`Names.TITLES` and already kept on the stored name — nothing read them. This is not evidence
to be weighed: the book prints it beside her name on every appearance, and counting it as one
vote among many left exactly the characters the text is clearest about unvoiced. It is also
what two people sharing a surname need — `Mr Bennet` and `Mrs Bennet` draw the same pronouns
around the same sentences, the case `GENDER_MAJORITY` was written to refuse, and refusing it
is right on pronouns alone and needless once the title is read.

**The ranks are deliberately excluded.** `Dr`, `Prof`, `St`, `Captain`, `Colonel`, `Major`
are worn by women in fiction, and guessing male from them would trade the accuracy this
change is not allowed to cost. Tested both ways.

**2. `GENDER_MIN` drops from 2 to 1, and the reason is the first change.** The threshold
existed to stop a single stray pronoun deciding a character, and the case it was really
protecting against was the shared surname — which titles now settle definitively. Swept
against the corpus rather than argued:

```
MIN  MAJORITY   accuracy  coverage
  1      0.60      91.4%     83.7%   <- taken
  2      0.60      91.6%     78.8%
  3      0.60      91.7%     74.7%
  2      0.55      89.8%     82.5%
  1      0.75      92.9%     71.6%
```

`0.55` is the only row that actually costs accuracy, and it is rejected. `0.75` buys 1.5
points of accuracy for 12 of coverage, which loses 4 points on the column that matters.

`RosterTest`'s `one sighting is not enough to claim a gender` asserted the behaviour this
overturns, on the reasoning that "the cup's owner is not necessarily Sarah". The corpus
disagrees, so the test now asserts the rule that holds and carries the numbers that changed
it, plus two new cases: a title settling what pronouns cannot, and a rank settling nothing.

**What is left.** 16.3% of characters still arrive with no gender — no title, and no clean
pronoun sighting anywhere in the book. The ticket's other two candidate signals are untried:
possessives (`Geralt's sword ... his`) and pronouns in the paragraph after the naming one.
Both are mechanical and measurable against the table above.

**Not measured.** No device number. This is a host-side corpus measurement; what it predicts
is that fewer characters come out in the wrong sex, and confirming that needs a listen on the
reference device with a book whose cast is mostly one sex.

Reproduce:

```bash
tools/fetch-pdnc.sh
gradle test
cd spike/pipeline && gradle installDist
build/install/quire-pipeline-spike/bin/quire-pipeline-spike cast ~/.cache/quire/pdnc/data/*/
```

**2026-09-11 — `quire-dialogue-attribution`.** The co-occurrence half, per Product Leadership's
2026-09-10 memo (directive 3). **It works and it is weak**, and the memo's prediction —
">90% of characters with high statistical confidence" — does not hold with this method.

The memo's diagnosis was that "real-time sliding window rules fail". The scan is neither
real-time nor a sliding window: `Roster.scan` already walks the whole book at import and
carries anaphora across paragraph breaks. What it actually did was **throw away every
sentence naming two people** (`names.size > 1 -> pending = null`), and in dialogue that is
most sentences. That is the real gap and it is what this closes.

Sentences naming several people now credit the pronoun to the nearest name preceding it, into
a **separate channel** that is consulted only where the strong rule declined and can never
overturn it. Kept apart because it is wrong often — "Elizabeth told Darcy that she would not"
credits Darcy — and usable only in volume.

```
setting                 coverage  accuracy   right sex heard
none (as shipped)          83.7%     91.4%             84.7%
MIN=4 MAJ=0.80             84.5%     91.5%             85.1%
MIN=3 MAJ=0.75  <- taken   85.3%     91.1%             85.1%
MIN=3 MAJ=0.65             86.3%     91.0%             85.4%
MIN=2 MAJ=0.70             86.9%     90.7%             85.4%
MIN=2 MAJ=0.60             88.5%     90.5%             85.8%
MIN=1 MAJ=0.70             89.8%     90.0%             85.9%
```

**Every row improves, and none of them improves much** — 1.2 points of right-sex-heard across
the whole range. `MIN=3 MAJ=0.75` is taken because it is the loosest setting that costs no
accuracy, which this ticket's acceptance criteria require. The looser rows are a product
choice rather than an engineering one: `MIN=1 MAJ=0.70` reaches the memo's 90% coverage
target, but at 90.0% accuracy rather than at high confidence, so it buys 0.8 points of
right-sex-heard by making 1.4 points more of the cast confidently wrong.

**What this says about the directive.** Nearest-preceding-name is the cheap approximation of a
co-occurrence graph and it is too noisy to reach the memo's bar. Getting past ~86% needs real
coreference — pronoun resolution that knows "she" refers to the subject rather than the last
noun — which is a model, not a rule, and belongs with the encoder work rather than here.

Reproduce: `cd spike/pipeline && build/install/quire-pipeline-spike/bin/quire-pipeline-spike cast ~/.cache/quire/pdnc/data/*/`

---

## QUI-036 — Voice foundry: generate a voice, don't pick one

**Status:** In review · **Epic:** Spike · **Owner:** session-visibility-check · **Depends on:** —

### User story

As a reader, I want each character to have a voice built for *them* — their pitch, their
pace, ideally their accent — so that the cast sounds like people rather than like a list of
strangers reading in turn.

### Context (why)

The product has been assuming a character gets *assigned* one of the engine's 904 speakers
(QUI-011, `spike/slice/Casting.kt`). That caps the cast at 904 fixed voices, none of them
chosen for the character, and it makes the analysis output an opaque integer.

The intent stated on 2026-09-02 is different and stronger: the app should read the book,
form an idea of what each character *sounds like*, and then **make** that voice. This
ticket asks whether the engine ADR-0002 already chose can do that at all, before anything
is designed around the assumption.

Answering it first matters because a "no" would reopen ADR-0002.

### Description (what)

A host probe that patches the shipped Piper model and reports what comes out, plus the
finding written down either way. Nothing ships from this ticket; it decides whether the
foundry is a real design or a dead end.

### Requirements (how)

- `spike/hostbench/voicelab.py`, alongside `bench.py`, reusing its `load()` and
  `voiceprofile.median_f0` so the numbers are comparable with the F0 fixture.
- Two probes: `blend` (speaker-embedding interpolation) and `accent` (espeak variant).
- Every comparison repeated and reported against its own spread — see the Worklog.
- No new dependency beyond `onnx`, host-only, never shipped. Patched models are written
  under `spike/hostbench/models/`, which is gitignored.
- Out of scope: choosing what a character should sound like (that is the analysis, QUI-006
  / QUI-035), the manifest schema change, and anything on the device.

### Acceptance criteria (Gherkin)

```gherkin
Scenario: A voice that was never trained can be synthesised
  Given the shipped libritts_r model
  When a row of the speaker table is replaced with an interpolation of two speakers
  Then the model produces well-formed speech at that row
  And its median F0 lies between the two parents by more than the run-to-run spread

Scenario: The stochastic baseline is established before any difference is claimed
  Given the same speaker and the same sentence
  When synthesis is repeated
  Then the spread between identical runs is measured and printed
  And no difference smaller than that spread is reported as a finding

Scenario: Whether accent is reachable is answered with evidence
  Given the phonemiser variant patched in the model metadata
  When the same speaker is synthesised across English variants
  Then any variant whose phoneme stream differs is identified against the noise floor
  And the limits of the probe are stated rather than implied
```

### Worklog

**2026-09-06 — listened on the reference device. Confirmed: generated voices work.**

The blend ramp between spk659 (male, 112 Hz) and spk192 (female, 189 Hz), played through
the Boox's own speaker. Every invented voice — t=0.25, 0.50, 0.75 — sounded like a person.

**The two that sounded mushy were `t000` and `parent-a`, which are the same voice.**
`ts` starts at 0.0, so `blend-01-t000-invented` is spk659's own vector written into an
untrained slot, and `blend-00-parent-a-real-spk659` is spk659 played natively. They sound
alike and they sound alike in the same way.

That is two findings, and both favour the decision:

1. **The write path is faithful.** A known vector written into a slot that was never
   trained reproduces the speaker it was copied from. This is the control the probe needed
   and did not have, and it passed by accident of the ramp including t=0.
2. **The mushiness is spk659's, not the blend's.** It is a real trained libritts_r reader,
   and the invented voices between the parents sound *better* than one of their parents.
   Interpolation is not degrading anything.

So ADR-0009 stands, confirmed by ear rather than by F0.

**What it exposes instead is a casting problem, not a foundry problem.** Some of the 904
readers are poor, and picking parents by F0 alone will sometimes pick one. `fixtures/voices/
libritts_r-f0.tsv` ranks voices by pitch and says nothing about quality. QUI-011 needs a
quality signal as well as a pitch one before it chooses parents — a note has been left on
that ticket.

---

## QUI-037 — Voice foundry: descriptor → generated voice

**Status:** In review · **Owner:** voice-generation-foundry · **Epic:** Audio
**Depends on:** QUI-032, QUI-036 · **PRD:** §4.2

### User story

As a reader, I want the app to actually build each character's voice from what the scan
learned about them, so that Play produces a generated cast rather than 904 fixed strangers.

### Context (why)

QUI-036 answered whether the engine can generate a voice at all: yes, confirmed by ear
2026-09-06 (ADR-0009). Nothing consumes that finding yet. Three gaps stand between "the
engine can do it" and "the app does it", all named in ADR-0009's consequences and the
2026-09-02 handoff's open questions:

1. Nothing turns a `Voice` descriptor (QUI-032's schema) into the two floats-per-axis a
   foundry needs — which two speakers to blend and by how much.
2. Nothing writes a descriptor during the scan. ADR-0006 names this job C and specifies its
   input (a character's confidently-attributed explicit-tag lines) but "nobody has written
   that prompt, or decided what happens to a character with three lines and no explicit
   tags" (handoff, §3.4).
3. QUI-011's caster still picks a single id by pitch alone. QUI-036's listening test found a
   real, well-inside-range speaker (spk659) that sounds mushy, and flagged that pitch-only
   parent selection will sometimes choose it. QUI-011 cannot proceed without a quality
   signal alongside the pitch one.

This ticket closes all three, in pure Kotlin, ahead of QUI-007 and QUI-011's full builds —
the same interface-first pattern QUI-032 used for the schema (CLAUDE.md §2.3).

### Description (what)

A new `core/voice` module that (a) plans and performs the embedding blend ADR-0009
describes, given a descriptor and the measured speaker fixtures, and (b) derives a
descriptor from a character's confidently-attributed lines during the scan. QUI-011's spike
caster (`spike/slice/Casting.kt`) is rewritten to consume both: a character with a
descriptor is resolved to a blend plan; a character without one still gets today's
gender/pitch pool behaviour unchanged.

**What this ticket does not do**, and why: it does not read or write `emb_g.weight` on a
loaded model. That is a sherpa-onnx / Android binding concern — thin glue over a pure
decision, per CLAUDE.md §9 — and has no device or SDK to be built against yet (QUI-010 is
still Todo). This ticket produces the *plan* (which two speakers, what fraction, what rate)
and the *arithmetic* (interpolating two known vectors), both of which are pure functions of
data already committed as fixtures. Writing the result into a running model is follow-on
work once QUI-010 exists.

### Requirements (how)

- Owns: `core/voice/`, `fixtures/voices/libritts_r-quality.tsv` (new),
  `spike/slice/src/main/kotlin/quire/spike/slice/Casting.kt`,
  `spike/slice/src/test/kotlin/quire/spike/slice/CastingTest.kt`,
  `spike/slice/build.gradle.kts`, `settings.gradle.kts`, `docs/architecture.md` §9 item 8.
- `core/voice` depends only on `core:model` (module boundary rule, QUI-001). No dependency
  on `core:attribution`: job C takes pre-filtered line text, not attribution internals, so
  the two stay decoupled.
- **Foundry** (`quire.voice.foundry`):
  - `SpeakerProfile` parses the existing `libritts_r-f0.tsv` shape (speaker, f0, gender) —
    ported from `spike/slice/VoiceProfile.kt` rather than depended on, since spike code is
    never a dependency of core (CLAUDE.md §3).
  - `QualityList` parses `libritts_r-quality.tsv`: `speaker`, `quality` (`ok` implied for
    every id not listed; `poor` entries are excluded from parent selection). This file is
    **human-curated from listening, not computed** — see the Worklog for why two computed
    proxies were tried and rejected.
  - `Foundry.plan(target: Voice, gender: Gender, profile, quality): BlendPlan` picks the two
    candidates in `gender`'s pool nearest `targetF0Hz` on either side, skipping any flagged
    `poor`, and returns their ids plus the interpolation fraction. A target outside the
    pool's range, or a pool with fewer than two usable candidates, degrades to the single
    nearest candidate rather than failing — "a near miss beats no voice" (existing
    `Casting.kt` philosophy, kept).
  - `Foundry.blend(a: FloatArray, b: FloatArray, fraction: Double): FloatArray` — the actual
    512-float interpolation, exactly the arithmetic QUI-036's probe validated by ear.
  - Deterministic: same descriptor and fixtures in, same plan out.
- **Voice design / job C** (`quire.voice.design`):
  - `VoiceDesigner.design(character, explicitLines): Voice?` — null when `explicitLines`
    has fewer than 3 entries (the same walk-on threshold QUI-007 already uses), matching the
    handoff's proposed fallback: too little evidence means no descriptor, and casting falls
    back to plain gender/pitch.
  - `targetF0Hz` from gender's **measured** median in `libritts_r-f0.tsv` (112 Hz male,
    188.5 Hz female — recomputed in the Worklog, not assumed), offset by age band. The age
    offsets themselves are **assumed, not measured** — no labelled data exists — and are
    called out as such in the Worklog and in a code comment, matching how the handoff keeps
    those two categories apart.
  - `lengthScale` from the mean words-per-line of `explicitLines` against a fixed reference,
    clamped to a narrow, safe band (0.85–1.2) — pace is a weak signal from so few lines, so
    the range stays close to the model's default rather than risking a caricature.
  - `description` is a template sentence composed from age band, gender and up to two
    traits — **not** SLM-authored. QUI-006 (the SLM runtime) does not exist yet, and
    prompting it is explicitly out of scope here. `source` stays `AUTO` either way, so a
    future SLM-backed writer is a drop-in replacement with no schema change.
- Out of scope: reading/writing `emb_g.weight` on a real model (needs QUI-010); the SLM
  prompt for `description` (needs QUI-006); wiring this into the real scan pipeline
  (QUI-007) or the real Android caster (QUI-011) — both remain Todo and get a handoff note
  instead of being claimed by this ticket.

### Acceptance criteria (Gherkin)

```gherkin
Scenario: A descriptor resolves to two real parents bracketing its target pitch
  Given a voice descriptor with a target F0 between two measured speakers of one gender
  When the foundry plans a blend
  Then both chosen speakers are of the requested gender
  And the target lies between their measured pitches

Scenario: A speaker flagged poor is never chosen as a parent
  Given a quality list flagging one speaker as poor
  And that speaker would otherwise be the nearest match to a target pitch
  When the foundry plans a blend
  Then the flagged speaker is not returned as either parent

Scenario: Blending two vectors produces a point between them
  Given two 512-float parent vectors and a fraction strictly between 0 and 1
  When they are blended
  Then every element of the result lies between the corresponding parent elements

Scenario: A character with too few confidently-attributed lines gets no descriptor
  Given a character with fewer than three confidently-attributed lines
  When voice design runs
  Then no voice descriptor is produced for that character

Scenario: A character with enough lines gets a descriptor grounded in measured pitch
  Given a character with at least three confidently-attributed lines and a known gender
  When voice design runs
  Then the descriptor's targetF0Hz is within the measured range for that gender

Scenario: Casting still works for a character with no descriptor
  Given a character manifest where one character carries no voice descriptor
  When casting resolves the cast
  Then that character still receives a voice, unchanged from today's gender/pitch behaviour

Scenario: Planning is deterministic
  Given the same descriptor, gender, profile and quality list
  When a blend is planned twice
  Then both plans are identical
```

### Worklog

**2026-09-06 — voice-generation-foundry.**

Two candidate host-computable quality signals were measured against the one ground truth
this project has — spk659, confirmed mushy by ear in QUI-036 — and both failed to flag it,
so neither ships:

- **Embedding-centroid distance.** `emb_g.weight`'s row for spk659 sits at the
  **50th percentile** of distance-from-centroid across all 904 speakers (rank 450 of 904).
  Unremarkable. Fetched the real model (`vits-piper-en_US-libritts_r-medium`, GitHub release
  assets, ~94 MB, reachable per CLAUDE.md §9) and read the initializer directly with `onnx`
  — no synthesis needed, seconds to run.
- **Autocorrelation peak clarity**, the by-product `voiceprofile.py`'s F0 pass already
  discards. Raw, it correlates with pitch band far more than with quality (high-F0 female
  voices score ~0.85, low-F0 male voices ~0.60-0.72, regardless of how they sound), so it
  was re-scored as a z-score against each speaker's 40 nearest-F0 neighbours to remove that
  confound. spk659 still scores **+0.54** — above its neighbourhood's average, 66th
  percentile from the bottom. Measured across all 904 speakers, ~153 s on this host.

Both are recorded so the next agent does not re-spend a session on either. The honest
conclusion: "sounds mushy" is not something a cheap signal-processing proxy on one fixed
sentence predicts — same shape of result as QUI-033's accent listen, where a measurement
(phoneme stream changed) did not imply the thing anyone actually cared about (sounds like
the accent). `libritts_r-quality.tsv` is therefore seeded from listening, not a formula:
one row, `659	poor`, and it is expected to grow the same way `voiceprobe.py`'s accent table
did — by someone's ear on the reference device, not by a script.

`targetF0Hz` gender centers recomputed directly from the fixture rather than reused from
memory: **male median 111.9 Hz** (n=426, range 84.5–134.5), **female median 188.5 Hz**
(n=426, range 155.3–245.0) — matches the values already used as examples in
`VoiceProfile.kt`'s doc comments, which is a second, independent confirmation.

Age-band offsets (elder −10 Hz, teen +15 Hz, child +40 Hz, adult/unknown +0) are **assumed**
— no labelled data exists for how age shifts F0 in this cast, and the Worklog says so
plainly rather than dressing a guess as a measurement.

Build: `./gradlew test checkModuleBoundaries` — all green, root build included
(`spike:indexer` and `spike:slice` both compile unchanged against `Casting`'s new,
backward-compatible constructor — the two new parameters are trailing and defaulted, so
`spike/indexer/Main.kt` and `spike/ttsbinding/SliceIndex.kt`, both outside this ticket's
file list, needed no changes). `checkModuleBoundaries`: 5 core modules, all clean —
`core:voice` depends only on `core:model`.

Filed a follow-up suggestion rather than doing it here (out of this ticket's declared file
list): `docs/architecture.md` §9 items 7 and the duplicate-numbered accent item are still
written as open questions, though both were answered by ear on 2026-09-06 (QUI-036, QUI-033)
before this ticket started. Only item 8, the one this ticket actually touches, is corrected
here.

**Handed to QUI-011 and QUI-007** as notes on those tickets rather than claimed here: the
resolver and the descriptor writer both exist now in `core/voice`, but wiring them into the
real scan and the real Android caster is those tickets' work, not this one's.

**What's left, for whoever picks up QUI-010/011 next:** reading `emb_g.weight` out of a
loaded sherpa-onnx session and writing an interpolated row back in — the one piece ADR-0009
and this ticket both stop short of, for lack of an SDK-backed device path to build it
against.

---

## QUI-038 — Scene segmentation for scene-level attribution

**Status:** Done · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-021
**PRD:** §3.1, §4 · **ADR:** [0006](docs/adr/0006-three-attribution-jobs.md)

### User story
As a reader, I want the model deciding who speaks to see the whole scene, so that
turn-taking resolves the way it does for me when I read it myself.

### Context (why)
[ADR-0006](docs/adr/0006-three-attribution-jobs.md) makes the **scene** the unit the model
is prompted with, for two reasons: one call per line cannot fit QUI-007's 30-minute budget,
and a model shown one quotation in isolation knows strictly less than the reader does.
`architecture.md` §9 item 4 records segmentation as being on the critical path because of
it. Nothing implements it.

QUI-028 has since made this the *only* path. Measured on one novel, both published
attribution encoders lose to the Tier 1 heuristic on wrong-voice rate at every operating
point, and a threshold sweep showed the confidence score cannot buy precision back. The
untagged three-quarters of dialogue is not solved by an encoder, which leaves a generative
model reading a whole scene — and that model cannot be prompted until a scene is a thing
this codebase can name.

This is also work that can be done and tested without the device (CLAUDE.md §9), which
almost nothing else on the critical path currently can.

### Description (what)
A pure-Kotlin segmenter that partitions a book's paragraphs into scenes, and a splitter for
scenes too long for a model's context window that cuts at a turn boundary rather than
mid-exchange and records the last known speaker so the next piece can carry it.

Also the first real count of what a novel contains: ADR-0006's "60–120 scenes" is an
estimate nobody has checked.

### Requirements (how)
- Owns: `core/attribution/scenes/` and its tests. Nothing else.
- Signals, cheapest first, and all of them structural rather than semantic:
  chapter boundaries from the index; an explicit scene-break paragraph (asterisks, dashes,
  or whitespace only); a run of blank paragraphs; and a hard maximum length as a backstop.
- **A scene never spans a chapter boundary.** That is the one rule with no exceptions.
- Output is ranges over paragraph indices and is deterministic: the same book always yields
  the same scenes, because a re-import must not reshuffle attribution.
- A scene longer than a caller-supplied budget is split at a paragraph boundary that is not
  mid-exchange, and the split carries the last speaker forward, per ADR-0006's note that
  splitting at a turn boundary is the obvious approach and is untested.
- **Measured on PDNC, and the ticket is not `Done` without the numbers:** scenes per novel,
  the distribution of quotations per scene, and the share of scenes that exceed a 2,048-token
  budget. `spike/pipeline`'s corpus loader already reads PDNC and `bakeoff dump` already
  emits paragraphs, so this is a report, not a new harness.
- Pure Kotlin in `core`, no Android classes (CLAUDE.md §9): this is exactly the kind of
  logic that must be testable without a build-install-listen cycle.
- Out of scope: the attribution prompt itself (QUI-009), the runtime that runs it
  (QUI-006), and any semantic scene detection — no model decides where a scene begins.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Chapters are never merged
  Given a book whose chapter ends without any scene-break markup
  When it is segmented
  Then no scene contains paragraphs from two chapters

Scenario: An explicit scene break starts a new scene
  Given a chapter containing a paragraph of only asterisks
  When it is segmented
  Then the paragraphs before and after it are in different scenes

Scenario: Segmentation is deterministic
  Given the same book
  When it is segmented twice
  Then both runs produce identical scene ranges

Scenario: An over-long scene is split at a turn boundary
  Given a scene exceeding the caller's token budget
  When it is split
  Then each piece is within budget
  And no piece begins in the middle of an exchange
  And each piece after the first records the last speaker of the piece before it

Scenario: The corpus is counted, not estimated
  Given PDNC's 28 novels
  When the segmenter reports over them
  Then scenes per novel, quotations per scene and the share over a 2,048-token budget are
    recorded in this ticket's Worklog
  And ADR-0006's estimate of 60-120 scenes per novel is confirmed or corrected
```

### Worklog
- **2026-09-07 (scene-segmentation-qui-038):** Landed `core/attribution/scenes/` — `Scene`
  (a half-open paragraph-index range), `SceneSegmenter` (chapter boundary, explicit
  scene-break paragraph, paragraph-count backstop; 12 tests) and `SceneSplitter` (splits an
  over-long scene at the same turn-boundary reset `Conversation` already uses — a run of
  narration longer than `Conversation.MAX_GAP_PARAGRAPHS`, so it never divides one exchange
  between two prompts — and carries the last known speaker into the next piece; 3 tests).
  All Gherkin scenarios pass as JUnit tests; `./gradlew :core:attribution:test` to reproduce.
  *(Corrected 2026-09-07, see the entry below: the "no piece begins in the middle of an
  exchange" scenario passed on its fixture but failed on 35.5% of real pieces, because
  nothing had run the splitter over the corpus. Fixed, and now measured.)*
  Pure Kotlin, no Android, no new dependency (`core:attribution` already depends only on
  `core:model`).
  Added a `scenes` command to the pipeline spike (`spike/pipeline/.../bakeoff/SceneReport.kt`,
  wired through `BakeoffCli`/`Main`) that runs the segmenter over all 28 PDNC novels, reusing
  `Pdnc.locate` and `Bakeoff.questions` exactly as `bakeoff dump` does — no new harness.
  **Measured** (`cd spike/pipeline && ../../gradlew run --args="scenes"`, after
  `tools/fetch-pdnc.sh`): **median 30 scenes per novel, mean 32.9, range 2–96**; **median 31
  quotations per scene, mean 40.1**; **75% of scenes exceed a 2,048-token budget**
  (`~4 chars/token`, the standard estimate — there is no on-device tokenizer to measure
  against in this JVM environment, CLAUDE.md §9).
  **ADR-0006's 60-120 guess did not hold — amended it and `docs/architecture.md` §9 item 4.**
  The real number is lower because PDNC's novels carry almost no scene-break markup inside a
  chapter: a "scene" in this corpus is overwhelmingly a whole chapter. One real wrinkle:
  PDNC's flat-text loader (`Pdnc.locate`) sets `chapterIndex = 0` for every paragraph — no
  chapter signal exists in the raw corpus at all — so `SceneReport.reconstructChapters`
  rebuilds a stand-in by recognising heading-shaped paragraphs (`CHAPTER I`, `Book II`, a
  bare numeral) before calling the segmenter. That reconstruction is report-side-only
  (`spike/pipeline`, not `core/attribution/scenes`): a real EPUB import needs none of it,
  because `EpubText` already sets `chapterIndex` from the spine. Sanity-checked by hand
  against known chapter counts (OliverTwist: 53 detected vs 53 real; TheSignOfTheFour: 12 vs
  12; TheAwakening: 39 vs 39) — good enough to trust the corpus-wide median, not exact for
  every novel (the regex is documented as non-exhaustive in `SceneReport`'s class doc).
  **What's left for QUI-009:** the splitter's `speakerOf` callback is a plain function the
  caller supplies — QUI-009's prompt loop is what will actually thread Tier 1's resolved
  speakers through it; nothing here assumes how that wiring looks. The 75%-over-budget
  finding should feed QUI-031's throughput measurement directly: scene batching's win was
  sized on rare splits, not routine ones.

- **2026-09-07 (local-model-voice-accents) — the splitter's criterion, measured then met.**
  This ticket asks that "no piece begins in the middle of an exchange". Nothing had run
  `SceneSplitter` over the corpus, so it was known only against a fixture.

  `spike/pipeline scenes` now splits every over-budget scene and counts. As merged, the
  splitter looked for a **turn boundary** — a narration run past
  `Conversation.MAX_GAP_PARAGRAPHS` — and cut wherever the budget ran out when it found
  none. Over PDNC that fallback was not rare: **759 of 2,140 pieces, 35.5%, opened
  mid-exchange.**

  The cause is the shape of the data this ticket itself measured. A scene only needs
  splitting because it is long and dialogue-dense, and dialogue-dense is exactly where long
  narration runs do not exist, so the criterion failed hardest on the scenes it mattered
  for.

  **Fixed with one more tier rather than by amending the criterion.** Between "a full
  narration run" and "cut anywhere" sits an obvious third option: any paragraph holding no
  dialogue at all. Weaker evidence that an exchange has ended, but it still never puts the
  cut between two adjacent turns, which is what mid-exchange means.

  | | pieces | open mid-exchange |
  | --- | ---: | ---: |
  | turn boundary only | 2,140 | 759 (**35.5%**) |
  | plus any narration paragraph | 2,235 | 70 (**3.1%**) |

  The remaining 3.1% is an unbroken run of dialogue longer than the budget, where nothing
  safe exists to cut at. Those pieces are flagged rather than hidden: `Piece.atTurnBoundary`
  is false, and QUI-009 should weight `carriedSpeaker` lower there, because the turns that
  explain the opening lines are in the piece before.

  Two regression tests cover both paths — a dense scene that must cut at narration, and an
  unbroken run that cannot. Root and `spike/pipeline` suites green.
  Reproduce: `cd spike/pipeline && gradle run --args="scenes"`.
