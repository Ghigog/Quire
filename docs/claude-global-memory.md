# Working economically — global Claude Code memory

Copy this into `~/.claude/CLAUDE.md` on your own machine and it applies to every project,
not just Quire:

```sh
mkdir -p ~/.claude && cat /path/to/Quire/docs/claude-global-memory.md >> ~/.claude/CLAUDE.md
```

(Or open it with `/memory` in Claude Code and paste the sections below.)

---

## The cost model, in one line

**Every tool call re-sends the whole conversation.** A token added early is paid for again
on every turn after it. Long sessions are therefore quadratic, not linear, and the
expensive thing is almost never the answer — it is the context the answer was computed
from.

## Advise the cheap path before taking the dear one

When about to do something materially more expensive than the alternative, **say so in one
sentence and name the substitute**, then do whatever is chosen:

> "Pasting that log puts it in every request from here on. I advise saving it to a file and
> giving me the path instead — I'll read the part I need, once."

Advise, don't refuse, and don't repeat the same advice twice in a session.

| About to | Advise instead |
| --- | --- |
| Paste a transcript, log or large output into the prompt | Save it to a file, give the path. It is read once rather than carried on every turn. |
| Spawn a subagent for work the current session can do inline | Each subagent starts cold and re-derives context this session already has. Do it inline unless the work is genuinely parallel and self-contained. |
| Carry a long session into unrelated work | `/clear`, or a new session. Otherwise every turn re-bills the finished task's history. |
| Do mechanical work — file edits, running scripts, writing up a decision already made | `/model sonnet` for that stretch; `/model opus` when the thinking resumes. |
| Read a large file to change a small part of it | `grep`/`sed` for the slice. A file read in full is paid for on every subsequent turn. |
| Search the codebase broadly with no anchor | Ask for the directory or the symbol. |
| Re-read a file that was just edited to check it landed | Don't. The edit tool errors if it fails. |

## Habits on the agent's side

- Batch independent tool calls into one turn. Ten sequential calls re-send the context ten
  times; one turn with ten calls sends it once.
- Read the slice, not the file. `sed -n '200,260p'` beats reading 900 lines.
- Long background work (downloads, builds, test suites) is nearly free in tokens — poll it
  rarely and do other work meanwhile, rather than waiting in a loop.
- State a finding once, in the place it belongs. Restating a measurement in three documents
  costs three times and drifts.

## When writing a `CLAUDE.md` for a new project

Carry these rules forward. Include a short **Working economically** section that states the
cost model in one line, names the two or three expensive moves most likely in *that*
project, and gives the substitute for each. Keep it under a screen — a rule nobody reads
costs tokens without saving any.
