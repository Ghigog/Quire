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
| QUI-020 | TTS service registration and NeoReader binding | Spike | In progress | quire-setup-docs | — |
| QUI-017 | TTS engine bake-off on target hardware | Spike | Done | session-visibility-check | — |
| QUI-028 | Encoder vs SLM for quotation attribution | Spike | Todo | — | — |
| QUI-018 | Headless pipeline spike | Spike | In progress | — | — |
| QUI-019 | Vertical slice: NeoReader Read Aloud in three voices | Spike | In review | — | QUI-020, QUI-021, QUI-022, QUI-024 |
| QUI-001 | Project scaffold, build and CI | Foundations | In progress | session-visibility-check | — |
| QUI-021 | Dialogue index schema and store | Index | In review | quire-setup-docs | QUI-001 |
| QUI-022 | Text normalisation and cursor matcher | Index | In review | quire-setup-docs | QUI-021 |
| QUI-023 | Book identification by fingerprint | Index | In review | — | QUI-021, QUI-022 |
| QUI-027 | Normalised-to-raw offset map | Index | Done | session-visibility-check | QUI-021, QUI-022 |
| QUI-005 | `characters.json` schema and manifest store | Attribution | In review | — | QUI-001 |
| QUI-006 | On-device SLM runtime | Attribution | Todo | — | QUI-001, QUI-017 |
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
| QUI-016 | Performance and SLA harness | Quality | Todo | — | QUI-010 |
| QUI-029 | Unindexed books and non-EPUB formats | Companion | **Deferred → later phase** | — | QUI-025 |
| QUI-002 | EPUB import and Readium reader shell | Reader | **Deferred → V3.0** | — | — |
| QUI-004 | Reading position and progress tracking | Reader | **Deferred → V3.0** | — | — |
| QUI-013 | Playback controls | Reader | **Deferred → V3.0** | — | — |
| QUI-014 | Sentence-level highlighting | Reader | **Deferred → V3.0** | — | QUI-024 covers the host-side part |
| QUI-015 | Character & voice drawer | UI | **Deferred → V2.0** | — | — |
| QUI-031 | SLM runtime bake-off and co-residency | Spike | Todo | — | QUI-006 |
| QUI-032 | Voice descriptor in `characters.json` | Attribution | In review | — | QUI-005 |
| QUI-033 | Accent: listening test and per-character variants | Spike | Todo | — | QUI-032 |
| QUI-034 | Cast discovery precision on real books | Spike | In review | session-visibility-check | QUI-008 |
| QUI-035 | Gender coverage for the inferred cast | Spike | Todo | — | QUI-034 |
| QUI-036 | Voice foundry: generate a voice, don't pick one | Spike | In review | session-visibility-check | — |

Next free ID: **QUI-037**

**Milestones** (see [`docs/architecture.md`](docs/architecture.md) §8):
**M0a prove interception** — QUI-020 · **M0b prove the stack** — QUI-017, QUI-018 ·
**M1 vertical slice** — QUI-019 · **M2 MVP** — the rest. Work down this table, not down
the epics.

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

**Status:** Todo · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-001
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
- _(empty)_

---

## QUI-007 — Upfront book scan → character manifest

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
are **known to be optimistic**: QUI-028 measured explicit tags at 68.6% precision on PDNC
against the declared 0.95. Moving them without a measurement would swap one fiction for
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
For each line Tier 1 left unattributed, the SLM is asked who is speaking, given a 5-line
sliding context window. Below 0.65 confidence the system falls back to inferring from the
speakers active in the scene; still below 0.40, the line is read by the Narrator.

### Requirements (how)
- Owns: `core/attribution/slm/attribution/`, `core/attribution/scene/`
- Context window: the 5 preceding lines with their resolved speakers, plus the candidate
  cast for the current scene.
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

**Status:** Todo · **Owner:** — · **Epic:** Quality · **Depends on:** QUI-010
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
- Owns: `benchmarks/`, `docs/performance.md`
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
- _(empty)_

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

> **Partially done, unclaimed.** The Tier 1 baseline exists and is scored (see Worklog);
> the bake-off this ticket is actually for has not started. `Todo` rather than
> `In progress` because nobody is working it — the claim is free to take.

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** —
**PRD:** §2 Phase 1, §4 · **Timebox:** 3 days

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
- Owns: `spike/attribution-bakeoff/`, `docs/adr/0005-attribution-model.md`
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
None of it is measured. QUI-028 has since shown Tier 1 resolves far less than hoped
(58.5% precision, and roughly one line in nine on untagged material), so the SLM carries
more of the load than the architecture assumed, not less.

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
- _(empty)_
**2026-08-31 — session-visibility-check**

Reproduce, measure, fix, measure again. All numbers from the 28 PDNC novels, whole corpus:

| | found | real | precision | recall | gender acc. |
| --- | --- | --- | --- | --- | --- |
| before | 2342 | 962 | 41.1% | 88.1% | 88.2% |
| + quoted text blanked | 1469 | 850 | 57.9% | 84.6% | 89.3% |
| + `ADJACENCY_MIN` 2 → 8 | **768** | **651** | **84.8%** | **82.3%** | **90.9%** |

Invented characters fell from 1380 to 117 — **49 per novel to 4.2**. Recall over major and
intermediate characters cost 5.8 points; those are found by their speech tags almost
without exception, and adjacency was mostly admitting people who are talked *about*.

**Defect 1 — the context around a quotation contains the neighbouring quotation.**
`Segment.before` and `.after` are the whole rest of the paragraph, so on a line of
back-and-forth the adjacency scan reads the quote beside it, whose first word is capitalised
because the speech starts there. `"Absolutely." "And another thing."` reported a character
called Absolutely. `Names.withoutQuotedText` blanks quoted spans first.

**Defect 2 — two sightings is not corroboration in a novel.** A threshold tuned on
four-paragraph fixtures is meaningless at 200,000 words. Swept 2→20: the knee is at 8, and
past it only recall moves. It is a count rather than a rate, so a very short book is
stricter than intended; nothing in the corpus made that worth fixing.

**One rule was tried and rejected on the evidence.** Requiring a candidate to appear
mid-sentence somewhere in the book — the position where English does not force a capital —
looked like the principled discriminator between `Geralt` and `Suddenly`. On top of the
retuned threshold it *lost*: 84.5% precision and 80.7% recall against 84.8% and 82.3%. It is
not in the diff.

**The finding that matters more than the fix.** Gender is inferred for only **58.7%** of the
real cast; the rest are `UNKNOWN`, and `Casting` then falls back to spreading raw speaker
ids, which picks a voice of arbitrary sex. That is exactly the report from the device — a
book of men read in women's voices — and it is not a casting bug. When a gender *is*
inferred it is right 90.9% of the time, so the problem is coverage, not accuracy. Needs its
own ticket.

Reproduce:

```bash
git clone --depth 1 https://github.com/Priya22/project-dialogism-novel-corpus.git /tmp/pdnc
cd spike/pipeline && gradle installDist
./build/install/quire-pipeline-spike/bin/quire-pipeline-spike cast /tmp/pdnc/data/* 
```

`gradle test` at the root: 30 tests, all green. Not measured against a PRD §5 SLA — this
changes what import reports, not what it costs. `In review` rather than `Done`: the number
that prompted it came off a device, and only a re-import of the same book closes it.

---

## QUI-035 — Gender coverage for the inferred cast

**Status:** Todo · **Epic:** Spike · **Owner:** — · **Depends on:** QUI-034

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

**2026-09-02 — session-visibility-check**

**Yes, and it is cheaper than expected.** Both levers are editable fields inside the model
file. No second model, no new runtime, no cloud, nothing added to the 450 MB footprint.

**Timbre.** `emb_g.weight` is a `[904, 512]` float initializer in the ONNX graph — the
speaker lookup table, 1.8 MB of the 92 MB model. **A voice is 512 floats: 2 KB.** Writing
an untrained row and addressing it by `sid` produces working speech:

```
control — spk659 repeated 5x: F0 116.5 Hz, sd 2.16 Hz

real spk659 (male)      116.7        blend t=0.00 (invented)   111.4
real spk192 (female)    195.1        blend t=0.25 (invented)   125.3
                                     blend t=0.50 (invented)   154.2
                                     blend t=0.75 (invented)   173.6
                                     blend t=1.00 (invented)   200.5
```

Endpoints land on the parents; the invented middle moves monotonically in steps of 14–29 Hz
against a 2.16 Hz noise floor. Linear interpolation only reaches the line between two
speakers — the space is 512-dimensional with 904 anchors, so this is the crudest possible
use of it.

**Accent lives in the phonemiser, not the speaker vector.** The model is `en-US` and the
espeak-ng variant is read from the ONNX `metadata_props["voice"]`. Note the trap: the
model's own `.onnx.json` carries an `espeak.voice` field that **sherpa-onnx does not read**
— patching it changes nothing and looks like the whole idea failing. The bundled
`espeak-ng-data` (19 MB, already shipping) contains `en-GB-x-rp`, `en-GB-scotland`,
`en-GB-x-gbclan` (Lancashire), `en-GB-x-gbcwmd` (West Midlands), `en-029` (Caribbean) and
`en-US-nyc`. Patched, they reach the model:

| espeak voice | mean s | sd | vs en-US |
| --- | --- | --- | --- |
| en-US | 4.28 | 0.25 | — |
| en-GB-x-rp | 4.32 | 0.25 | +0.03s (0.1 sd) |
| **en-GB-scotland** | 5.67 | 0.44 | **+1.39s (3.9 sd)** |
| en-GB-x-gbclan | 4.17 | 0.24 | −0.11s (0.4 sd) |
| en-GB-x-gbcwmd | 4.40 | 0.33 | +0.12s (0.4 sd) |
| **en-029** | 5.45 | 0.51 | **+1.17s (2.9 sd)** |
| en-US-nyc | 4.10 | 0.18 | −0.18s (0.8 sd) |

**A wrong result, recorded because it is the instructive part.** The first version compared
one waveform per accent and found every variant "differed" at rms ~0.14. The control found
en-US differs *from itself* by rms 0.149 — `noise_scale` and `noise_w` are both 0.333, so
Piper's duration and waveform are stochastic per call. The single-shot A/B could not have
returned anything else. Everything above is repeated 10× and quoted against its own spread.

**What is not established.** Nothing here was listened to. F0 and duration prove the audio
is well-formed and that the phoneme stream genuinely changed; they cannot hear whether a
blended embedding sounds like a person or like mush, nor whether `en-GB-scotland` phonemes
through an `en-US`-trained model sound Scottish or merely wrong — that combination is
out-of-distribution for the model and is the likeliest place for this to fall down. Duration
is also blind to RP and Lancashire, which differ in vowel quality rather than phoneme count;
their null rows above mean "this probe cannot see it", not "nothing happened".

**This is why the status is `In review` and not `Done`.** It needs an ear on the reference
device. Two follow-ups it justifies, neither started: a `VoiceSpec` in the character
manifest so the analysis records *what a character should sound like* rather than a speaker
integer, and a foundry that realises a spec — today by nearest-neighbour plus blending plus
`length_scale` for pace, later by better use of the 512 dimensions.

Reproduce:

```bash
cd spike/hostbench && ./fetch-models.sh
python3 voicelab.py blend
python3 voicelab.py accent
```
