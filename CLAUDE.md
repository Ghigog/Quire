# CLAUDE.md — How we work on Quire

This file is the operating manual for any Claude agent working in this repository.
Read it before doing anything else. If a request conflicts with this file, say so and ask.

**Quire** is a tactile, multi-voice e-reader: EPUBs turned into multi-character audio
using on-device AI, optimised for e-ink Android hardware. The product spec lives in
[`docs/PRD.md`](docs/PRD.md) — it is the source of truth for *what* we are building.

---

## 1. First principles

1. **Documentation before code.** A change that isn't described in a ticket doesn't get
   written. If the ticket is missing, write the ticket first (see §4), then implement.
2. **Small, complete units of work.** One ticket = one branch = one PR. A PR that
   touches three unrelated concerns is three PRs.
3. **Report honestly.** If tests fail, say so and paste the output. If a requirement was
   skipped, name it and say why. Never mark a ticket done on partial work.
4. **Do the asked-for scope.** Don't silently widen (refactoring the world) or narrow
   (shipping half and calling it done). If you think the scope is wrong, say it in one
   or two sentences, then deliver under a stated assumption.
5. **The device is the constraint.** Every decision is weighed against the SLAs in
   §5 of the PRD: ≤1.2 GB RAM, ≤450 MB app footprint, RTF ≤0.15, <8%/hr battery.
   "It works on my laptop" is not evidence. The reference device is an Onyx Boox
   Note Air5 C — `docs/device-profile.md` says what that implies. Android only;
   iOS is out of scope.
6. **The device decides, but it does not have to be asked first.** An SLA number is only
   ever true on the reference device. A *comparison* between two candidates usually is
   not: `spike/hostbench` screens TTS models on the build machine in seconds, and killed
   two candidates that would each have cost a build, an install and a listen. Its README
   records where that transfers and where it demonstrably does not. Screen on the host,
   decide on the device, and never quote a host number at an SLA.

---

## 2. Working with multiple agents at once

Several agents will often be running against this repo in parallel. The rules below
exist so that two agents never fight over the same bytes.

### 2.1 One agent, one ticket, one branch

| Rule | Detail |
| --- | --- |
| Branch naming | `claude/<ticket-id>-<kebab-slug>` e.g. `claude/QUI-014-ring-buffer`. A session handed a branch it did not choose keeps that branch — see below |
| Ownership | An agent owns the ticket it claimed and *only* the files listed in that ticket's **Requirements** |
| Claiming | Set the ticket's `Status` to `In progress` and `Owner` to **your branch slug** in `tickets.md`, and push that change **first**, before writing code |
| Releasing | On finish (or abandonment) set `Status` back to `Done` / `In review` / `Blocked` / `Todo`, clear `Owner` to `—`, and push |

**Owner is the branch slug, never the model name.** Every session runs on the same model,
so signing the board `claude-opus-5` makes two concurrent agents indistinguishable and
defeats the whole point of claiming — a real collision, found on 2026-08-29 when four
tickets shared one owner across three sessions. Use the distinctive part of your branch:
owner `session-visibility-check` for `claude/session-visibility-check-62dpup`. Model names
do not belong in committed artefacts anyway.

**Sessions started from the web or the desktop app are given a generated branch** named
after the request, not after a ticket. Do not rename it and do not open a second branch to
satisfy the naming rule — note the ticket in the first commit instead. The rule above is
for branches you create yourself.

**`In review` means the deliverable is done and something outside this repo has to
confirm it** — a device measurement, a listen, a human read. It is a legitimate release
state, and more honest than `Done` for work whose acceptance criteria need hardware.

### 2.2 Avoiding collisions

- **Check `tickets.md` before starting.** If a ticket is `In progress` and owned by
  another agent, pick a different one. Do not "help" with someone else's ticket.
- **Never edit a file outside your ticket's declared file list.** If you need a change
  in someone else's area, open a new ticket describing it and link it as a dependency;
  don't reach across.
- **Shared, high-contention files** — `tickets.md`, `CLAUDE.md`, dependency manifests
  (`build.gradle*`, `Package.swift`, lockfiles), and DI/wiring modules — are
  *append-only where possible*. Add your row/entry at the end; don't reflow or reorder
  the file, because that turns every concurrent edit into a conflict.
- **Rebase before you push**, always: `git fetch origin main && git rebase origin/main`.
  If the rebase conflicts in a file you don't own, take the other side (`--theirs` for
  their change) and re-apply your own edit on top; never overwrite another agent's work.
- **Never force-push a branch you did not create.** On your own branch, prefer a merge
  commit over history rewriting once anything is pushed.

### 2.3 Parallelisation strategy

When splitting work across agents, split along **interface seams**, not along files:

1. Agent A defines and lands the *interface* (data class, protocol, JSON schema) in a
   small, fast PR.
2. Agents B, C, D then build against that frozen interface in parallel.

Concretely for Quire, these seams are already good fan-out points and should be landed
early: the `characters.json` schema, the `AttributionResult` type, the `TtsChunk` +
timestamp type, and the audio `RingBuffer` interface.

Do **not** parallelise: two agents on the same pipeline stage, or any agent on a stage
whose upstream interface is still in flux.

### 2.4 Handoff notes

Every agent ends its session by appending a dated entry to the ticket's **Worklog**
section — what landed, what's left, what surprised you, and the exact command to
reproduce your test run. Assume the next agent has zero memory of your session,
because it does.

---

## 3. Repository layout

```
/CLAUDE.md            ← you are here: how we work
/tickets.md           ← the backlog and the single source of truth for work in flight
/docs/PRD.md          ← the product requirement document
/docs/adr/            ← architecture decision records (one file per decision)
/docs/schema/         ← frozen data contracts (characters.json), the fan-out seams of §2.3
/docs/architecture.md ← how the pipeline actually fits together (kept in sync with code)
/docs/device-profile.md ← the reference device and what it forces on the design
/docs/handoff/       ← dated notes handing a live design thread to the next session
/core/                ← pure-Kotlin/JVM modules: model, index (testable without a device)
/fixtures/            ← labelled test data shared across tickets (attribution golds)
/spike/               ← timeboxed throwaway harnesses; never shipped, never depended on
/spike/hostbench/     ← screens TTS candidates on the build machine (Python + sherpa-onnx)
/spike/indexer/       ← builds a dialogue index, and the EPUB that matches it
/spike/slice/         ← pure-Kotlin casting and span clipping for the vertical slice
/spike/pipeline/      ← Tier 1 attribution and scoring against PDNC
/spike/ttsbinding/    ← the Android TTS probe; the only module needing an SDK
/tools/               ← documented fetch and build scripts for artefacts git does not hold
```

Application code directories are created by the tickets that introduce them; the ticket
that adds a top-level directory must also add a line to this table in the same PR.

---

## 4. Tickets

`tickets.md` is the work tracker. **Every** ticket, without exception, has these five
sections in this order:

1. **User story** — `As a <role>, I want <capability>, so that <benefit>.`
2. **Context (why)** — the background, the problem, links to the PRD section and to
   related tickets. Why this matters *now*.
3. **Description (what)** — the change in plain prose. What a user or another system
   observes as different afterwards.
4. **Requirements (how)** — the implementation constraints: files/modules to touch,
   libraries, data shapes, performance budgets, out-of-scope notes.
5. **Acceptance criteria (Gherkin)** — `Given / When / Then` scenarios. Each scenario
   must be mechanically checkable — by a test, a measurement, or an explicit manual
   procedure written into the ticket.

Ticket IDs are `QUI-###`, allocated sequentially, never reused. Statuses are
`Todo` → `In progress` → `In review` → `Done`, plus `Blocked` (which must name the
blocking ticket).

---

## 5. Definition of done

A ticket is `Done` only when all of the following are true:

- [ ] Every Gherkin scenario in the ticket passes, with an automated test where one is
      possible.
- [ ] The relevant SLA from PRD §5 was **measured**, not assumed, and the number is in
      the ticket's Worklog.
- [ ] Docs updated in the same PR: `docs/architecture.md` for structural change, an ADR
      for a decision with alternatives, this file for a process change.
- [ ] No new files outside the ticket's declared file list.
- [ ] The PR body links the ticket ID and lists what was verified and how.

---

## 6. Code conventions

- **Style:** match the file you are in. Don't reformat lines you didn't change.
- **Comments:** explain *why*, not *what*. Match the surrounding density.
- **Commits:** `QUI-###: imperative summary under 72 chars`, body explains why.
  Group logically; don't dump one giant commit per ticket.
- **Tests:** a bug fix lands with a regression test that fails before the fix.
- **Dependencies:** every new dependency needs a line in the ticket justifying its
  size cost against the 450 MB footprint budget. Model files are never committed to
  git — they are fetched by a documented script.
- **No dead code.** Don't leave commented-out blocks or speculative abstractions
  "for later".

---

## 7. E-ink rules of thumb

These bite often enough to be written down:

- Pure `#000000` / `#FFFFFF` assets only in monochrome mode; no greys that dither.
- The reference device's panel *is* colour (Kaleido 3), but colour halves resolution to
  150 ppi and cuts contrast. Nothing on the reading surface uses it, and colour is never
  the sole carrier of meaning. See `docs/device-profile.md`.
- No animation, no cross-fades, no shimmer loaders. State changes are instant.
- Batch DOM/view mutations into a single frame to avoid partial-refresh ghosting.
- Hardware page-turn and volume keys are first-class inputs, not accessibility extras.
- Any UI added must be checked in monochrome mode before the ticket is `Done`.

---

## 8. Guardrails

- Never commit model weights, audio caches, book files, or anything under a
  reader's personal library.
- Book content and generated audio stay on-device. No telemetry containing text from a
  user's books, ever — this is a hard product rule, not a preference.
- Don't add cloud services. Cloud TTS is explicitly V2 scope (PRD §6) and needs its own
  ticket and an ADR.
- Ask before: deleting files you didn't create, changing the branch protection or CI
  config, or bumping a major dependency version.

---

## 9. What the build environment actually has

Sessions run in an ephemeral cloud container, not on the machine with the device attached.
Knowing its edges saves a lot of wasted work.

**Available:** JDK 21 and Gradle 8.14 (`gradle test` at the root runs the whole JVM suite
in seconds), Python 3.11 with pip, and outbound HTTPS to GitHub release assets and PyPI.

**You can build the APK, but the SDK is not in the image.** Run
`./tools/install-android-sdk.sh` once per session — roughly 460 MB and a couple of minutes —
then `./tools/build-slice-index.sh` and `cd spike/ttsbinding && ../../gradlew assembleDebug`.

This depends on **`dl.google.com` being on the environment's allowed-domain list**, added
2026-08-29. It is the single point of failure and it is not obvious when it fails: Gradle's
`google()` repository *is* `dl.google.com/dl/android/maven2`, so without it the Android
Gradle Plugin cannot resolve and the build dies before reaching any of our code.
`maven.google.com` only redirects there, and AGP is published nowhere else — Maven Central
and the plugin portal both 404. The install script checks the host first and says so.

Test it directly rather than inferring it, and use a **real path**: `https://dl.google.com/`
alone returns nothing useful because it redirects to `www.google.com`, which is blocked
independently.

```bash
curl -sS -o /dev/null -w '%{http_code}\n' -r 0-100 \
  https://dl.google.com/android/repository/repository2-3.xml   # 206 means available
```

**If the host is ever blocked again, push instead of giving up.** GitHub's runners ship the
SDK, and `.github/workflows/ci.yml` assembles the probe and attaches the APK and its
matching EPUB to the run. That path stays available whatever the container can reach, and
it is also what proves the Android sources compile on a machine that is not this one.

Either way, **a green `gradle test` says nothing about whether the Android code compiles**:
the root build does not include `spike/ttsbinding` at all.

Also blocked: Hugging Face and Project Gutenberg. GitHub release assets are reachable,
which is where the TTS models and the sherpa AAR come from.

**Two habits follow from this, and both are worth keeping even once an SDK exists.**

1. **Put the logic where it can be tested.** Casting, matching, normalisation and span
   clipping live in pure-Kotlin modules with tests, not in Android classes. `spike/slice`
   exists for exactly this reason, and its tests caught two real bugs — a per-character
   normalisation walk that silently dropped every space, and a chunk-clipping rule that
   read a speech tag in the character's voice. Neither would have been obvious by ear.
   Android classes stay thin glue: a driver, an asset copy, a service callback.
2. **Never copy `core:` sources into a spike.** The Android and desktop spikes reach the
   real modules through `includeBuild("../..")`. If a spike normalised text even slightly
   differently from the writer that built the index, nothing would match and it would look
   like a matcher bug for a day.

**Nothing large is committed.** Model weights, the sherpa AAR, and generated indexes are
produced by scripts in `/tools/`, and the fixtures they derive from are what version
control holds. Where a spike needs a book, generate it — `spike/indexer` emits an EPUB and
its index from one labelled fixture, so the two cannot drift and no real book ever lands in
the repository (§8).

---

## 10. Working economically

Every tool call re-sends the whole conversation, so a token added early is paid for again on
every turn after it. This repository makes that easy to get wrong: `CLAUDE.md`, `tickets.md`
and the ADRs are all long, and reading one in full to change three lines is a bill that
recurs for the rest of the session.

**Before doing something materially more expensive than the alternative, say so in one
sentence and name the substitute.** Then do whatever is chosen. Advise, don't refuse, and
don't give the same advice twice in one session.

The moves that cost most here, and what to do instead:

| Instead of | Do |
| --- | --- |
| Pasting a previous session's transcript | Pointing at its handoff note in `/docs/handoff/` |
| Reading `tickets.md` or a long ADR whole | `grep -n` for the heading, then `sed -n` for its range |
| A subagent for work this session can do inline | Doing it inline — a subagent starts cold and re-derives what is already loaded |
| Carrying one session across several tickets | One session per ticket, per §2.1, then `/clear` |
| A reasoning model for mechanical edits | Switching down for that stretch, back up for design |

The portable version of this, for `~/.claude/CLAUDE.md` and for new projects, is
[`docs/claude-global-memory.md`](docs/claude-global-memory.md).
