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
| QUI-017 | Model bake-off on target hardware | Spike | Todo | — | — |
| QUI-018 | Headless end-to-end pipeline spike | Spike | Todo | — | QUI-017 |
| QUI-019 | Vertical slice: one chapter on device | Spike | Todo | — | QUI-018, QUI-002, QUI-010, QUI-012 |
| QUI-001 | Project scaffold, build and CI | Foundations | Todo | — | — |
| QUI-002 | EPUB import and Readium reader shell | Foundations | Todo | — | QUI-001 |
| QUI-003 | E-ink display mode and hardware keys | Foundations | Todo | — | QUI-002 |
| QUI-004 | Reading position and progress tracking | Foundations | Todo | — | QUI-002 |
| QUI-005 | `characters.json` schema and manifest store | Attribution | Todo | — | QUI-001 |
| QUI-006 | On-device SLM runtime | Attribution | Todo | — | QUI-001, QUI-017 |
| QUI-007 | Upfront book scan → character manifest | Attribution | Todo | — | QUI-005, QUI-006 |
| QUI-008 | Tier 1 heuristic dialogue attribution | Attribution | Todo | — | QUI-005 |
| QUI-009 | Tier 2/3 SLM attribution with confidence fallback | Attribution | Todo | — | QUI-006, QUI-008 |
| QUI-010 | ONNX TTS engine with boundary timestamps | Audio | Todo | — | QUI-001, QUI-017 |
| QUI-011 | Automatic voice casting | Audio | Todo | — | QUI-007, QUI-010 |
| QUI-012 | Rolling dynamic ring buffer | Audio | Todo | — | QUI-010 |
| QUI-013 | Playback controls | Audio | Todo | — | QUI-012 |
| QUI-014 | Sentence-level highlighting | Audio | Todo | — | QUI-010, QUI-013 |
| QUI-015 | Character & voice drawer | UI | Todo | — | QUI-011 |
| QUI-016 | Performance and SLA harness | Quality | Todo | — | QUI-010 |

Next free ID: **QUI-020**

**Milestones** (see [`docs/architecture.md`](docs/architecture.md) §8): **M0 prove the
stack** — QUI-017, QUI-018 · **M1 vertical slice** — QUI-019 · **M2 prototype** —
everything else. Start at the top of this table, not the top of the epics.

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

**Status:** Todo · **Owner:** — · **Epic:** Foundations · **Depends on:** QUI-001
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

**Status:** Todo · **Owner:** — · **Epic:** Foundations · **Depends on:** QUI-002
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
- Budget: a 100k-word novel scans in under 10 minutes on a mid-tier ARM SoC; record the
  measured time in the Worklog.
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

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-012
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

**Status:** Todo · **Owner:** — · **Epic:** Audio · **Depends on:** QUI-010, QUI-013
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

**Status:** Todo · **Owner:** — · **Epic:** UI · **Depends on:** QUI-011
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
- SLM candidates: Llama 3.2 1B and Qwen 2.5 1.5B, Q4_K_M, via `llama.cpp` JNI and via
  ExecuTorch. Measure: load time, peak RSS, prompt-eval and generation tokens/s on a
  fixed 5-line attribution prompt.
- TTS candidates: Kokoro-TTS (82M ONNX) and Piper C++. Measure: RTF on a fixed 10 s
  text, peak RSS, on-disk size, number of usable voice variants, and whether word
  boundary timestamps are obtainable **without** post-hoc alignment.
- Co-residency: load an SLM and a TTS engine together and record combined peak RSS
  against the 1.2 GB ceiling. This number decides ADR-0003.
- Run on at least one true e-ink device (Onyx Boox or Meebook); record exact model, SoC
  and RAM with every result.
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
  When peak resident memory is measured
  Then the number is recorded against the 1.2 GB ceiling
  And ADR-0003 selects whole-book, chapter-ahead or co-resident attribution on that basis

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

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-017
**PRD:** §3 · **Timebox:** 4 days

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
  speaker per line, including one heavy untagged back-and-forth exchange.
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
- _(empty)_

---

## QUI-019 — Vertical slice: one chapter, three voices, on device

**Status:** Todo · **Owner:** — · **Epic:** Spike · **Depends on:** QUI-018, QUI-002, QUI-010, QUI-012
**PRD:** §3, §4.2 · **Timebox:** 1 week

### User story
As a stakeholder, I want to hold an e-ink device, open a book, press Play, and hear a
chapter performed with the text tracking along, so that we can feel whether Quire is
actually good before committing to the full build.

### Context (why)
M0 proves the parts work. This proves the *experience* works — the thing no benchmark
can tell us. It is also the first time the memory decision from ADR-0003 meets reality
with a real UI attached. Deliberately narrow: one hardcoded book, one chapter, no
library, no settings, no recovery.

### Description (what)
An installable debug build that opens a single bundled EPUB at a single chapter,
plays it with the narrator plus two character voices using attribution precomputed by
QUI-018, and highlights the sentence being spoken. Play/pause only.

### Requirements (how)
- Owns: `app/src/debug/` slice sources; consumes `core:reader`, `core:tts` unchanged
- Attribution is loaded from a precomputed `attribution.jsonl` shipped with the build —
  no SLM runs on device in this ticket. That isolates the audio and UI experience from
  model performance.
- Must run on a physical e-ink device in monochrome mode, and record measured TTFS and
  peak RSS in the Worklog against the SLAs.
- Hardcoding is expected and fine. Anything discovered here that must survive becomes a
  ticket, not a quiet addition to this one.
- Delete or fold into production code once M2 begins; this build is not shipped.
- Out of scope: import, library, settings, drawer, seek, speed, resumability.

### Acceptance criteria (Gherkin)
```gherkin
Scenario: Press play, hear a chapter
  Given the slice build on a physical e-ink device
  When I press Play
  Then the chapter is read aloud with the narrator and two distinct character voices

Scenario: The text tracks the audio
  Given playback is running
  When a new sentence begins
  Then that sentence is highlighted and the page follows it

Scenario: It starts fast enough to feel instant
  Given the slice build on the target device
  When I press Play
  Then the first audio frame is emitted within 800 ms
  And the measured value is recorded in the Worklog

Scenario: It stays inside the memory ceiling
  Given a full chapter plays to its end
  When peak resident memory is measured
  Then it is recorded against the 1.2 GB ceiling

Scenario: Pause is honest
  Given playback is running
  When I pause
  Then audio stops and the highlighted sentence is the one that was being spoken
```

### Worklog
- _(empty)_
