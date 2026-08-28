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
| QUI-020 | TTS service registration and NeoReader binding | Spike | In progress | claude-opus-5 | — |
| QUI-017 | Model bake-off on target hardware | Spike | Todo | — | — |
| QUI-028 | Encoder vs SLM for quotation attribution | Spike | Todo | — | — |
| QUI-018 | Headless pipeline spike | Spike | In progress | claude-opus-5 | — |
| QUI-019 | Vertical slice: NeoReader Read Aloud in three voices | Spike | Todo | — | QUI-020, QUI-021, QUI-022, QUI-024 |
| QUI-001 | Project scaffold, build and CI | Foundations | Todo | — | — |
| QUI-021 | Dialogue index schema and store | Index | In review | claude-opus-5 | QUI-001 |
| QUI-022 | Text normalisation and cursor matcher | Index | In review | claude-opus-5 | QUI-021 |
| QUI-023 | Book identification by fingerprint | Index | Todo | — | QUI-021, QUI-022 |
| QUI-027 | Normalised-to-raw offset map | Index | Todo | — | QUI-021, QUI-022 |
| QUI-005 | `characters.json` schema and manifest store | Attribution | Todo | — | QUI-001 |
| QUI-006 | On-device SLM runtime | Attribution | Todo | — | QUI-001, QUI-017 |
| QUI-007 | Upfront book scan → character manifest | Attribution | Todo | — | QUI-005, QUI-006 |
| QUI-008 | Tier 1 heuristic dialogue attribution | Attribution | Todo | — | QUI-005, QUI-018 |
| QUI-009 | Tier 2/3 SLM attribution with confidence fallback | Attribution | Todo | — | QUI-006, QUI-008 |
| QUI-010 | ONNX TTS engine with boundary timestamps | Audio | Todo | — | QUI-001, QUI-017 |
| QUI-011 | Automatic voice casting | Audio | Todo | — | QUI-007, QUI-010 |
| QUI-012 | Rolling ring buffer keyed by segment | Audio | Todo | — | QUI-010, QUI-022 |
| QUI-024 | Multi-voice utterance and `rangeStart` callbacks | Audio | Todo | — | QUI-010, QUI-022 |
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

Next free ID: **QUI-030**

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

**Status:** Todo · **Owner:** — · **Epic:** Foundations · **Depends on:** —
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
- _(empty)_

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

**Status:** Todo · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-001, QUI-017
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
- _(empty)_

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

**Status:** Todo · **Owner:** — · **Epic:** Attribution · **Depends on:** QUI-005
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
- _(empty)_

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

## QUI-017 — Model bake-off on target hardware

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** —
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
- Owns: `spike/bakeoff/`, `docs/adr/0001-slm-runtime.md`, `docs/adr/0002-tts-engine.md`,
  `docs/adr/0003-memory-arbitration.md`
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
- Out of scope: any production code, any UI, cloud engines.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Every candidate is measured
  Given the bake-off harness on a mid-tier e-ink device
  When it runs to completion
  Then it reports load time, peak RSS and throughput for each SLM candidate
  And RTF, peak RSS, disk size and voice count for each TTS candidate

Scenario: Boundary timestamps are proven, not assumed
  Given the chosen TTS candidate
  When a 10 second text is synthesised
  Then word boundary timestamps are emitted by synthesis itself
  And a named word's timestamp matches its position in the audio within 50 ms

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

Scenario: Decisions are recorded
  Given the bake-off has run
  When I read the three ADRs
  Then each names its alternatives, its measurements, the decision, and its revisit trigger

Scenario: A candidate that fails is reported, not worked around
  Given a candidate that misses its SLA on the target device
  When results are written up
  Then the ADR states the miss plainly rather than proposing a heavier device
```

### Worklog
- _(empty)_

---

## QUI-018 — Headless end-to-end pipeline spike

**Status:** In progress · **Owner:** claude-opus-5 · **Epic:** Spike · **Depends on:** QUI-017 (partial)
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

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-020, QUI-021, QUI-022, QUI-024
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
- _(empty)_

---

# Epic: Index

> The seam between the two processes of PRD v1.2. The companion app writes; the TTS
> service reads. Freeze this before fanning out — it is to v1.2 what `characters.json`
> was to v1.1 (`docs/architecture.md` §1).

## QUI-021 — Dialogue index schema and store

**Status:** In review · **Owner:** claude-opus-5 · **Epic:** Index · **Depends on:** QUI-001
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

**Status:** In review · **Owner:** claude-opus-5 · **Epic:** Index · **Depends on:** QUI-021
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

**Status:** Todo · **Owner:** — · **Epic:** Index · **Depends on:** QUI-021, QUI-022
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
- _(empty)_

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

**Status:** In progress · **Owner:** — · **Epic:** Spike · **Depends on:** —
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

**Status:** Todo · **Owner:** — · **Epic:** Index · **Depends on:** QUI-021, QUI-022
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
- Store the map compactly — a per-word pair of offsets is enough, since normalisation only
  ever deletes characters and collapses runs of whitespace; it never reorders or inserts.
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
- _(empty)_

---

## QUI-028 — Encoder vs SLM for quotation attribution

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
- _(empty)_

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
