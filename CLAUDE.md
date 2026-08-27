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

---

## 2. Working with multiple agents at once

Several agents will often be running against this repo in parallel. The rules below
exist so that two agents never fight over the same bytes.

### 2.1 One agent, one ticket, one branch

| Rule | Detail |
| --- | --- |
| Branch naming | `claude/<ticket-id>-<kebab-slug>` e.g. `claude/QUI-014-ring-buffer` |
| Ownership | An agent owns the ticket it claimed and *only* the files listed in that ticket's **Requirements** |
| Claiming | Set the ticket's `Status` to `In progress` and `Owner` to your agent label in `tickets.md`, and push that change **first**, before writing code |
| Releasing | On finish (or abandonment) set `Status` back to `Done` / `Blocked` / `Todo` and push |

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
/docs/architecture.md ← how the pipeline actually fits together (kept in sync with code)
/docs/device-profile.md ← the reference device and what it forces on the design
/fixtures/            ← labelled test data shared across tickets (attribution golds)
/spike/               ← timeboxed throwaway harnesses; never shipped, never depended on
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
