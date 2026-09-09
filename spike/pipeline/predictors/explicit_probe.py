#!/usr/bin/env python3
"""
Why is the SLM's precision on Explicit quotations 51.5% when Tier 1's is 91.8% (QUI-028)?

An Explicit quotation is one whose speech tag names the speaker in the same sentence, so a
model that can read English should be near-perfect and the sub-score should be boring. It is
not, and the handoff of 2026-09-08 makes fixing it the gate on every other SLM number. Four
harness faults have already been caught by this one sub-score; the candidate list is ruled
out as the main cause (94.4% of gold Explicit speakers are offered).

**This is a diagnostic, not a candidate.** It reads `quotation_info.csv` to pick Explicit
quotations and to score them immediately, which is exactly what `slm_predict.py` is forbidden
to do — a predictor that can see the answer can score itself by accident (see
`ExternalCandidate`'s doc). Keeping the two in separate files is what keeps that guarantee
legible: nothing here is ever wired into a `bakeoff --candidate` run.

**What it separates.** The two live hypotheses both predict 51.5% and imply opposite fixes:

  A. the batch protocol is the fault — a piece asks for 32 answers in one array where only 2
     carry a tag, and turn-tracking drift across the untagged 30 pulls the tagged ones down
     with it. Fix: the prompt.
  B. a 1B model cannot read a speech tag reliably at all. Fix: a bigger model, and if only a
     3B clears it that is a device question rather than an approach question.

So the same sampled quotations are asked three ways, changing one thing at a time:

  batch  one call per scene piece for the whole array — what `slm_predict.py` does today
  scene  one call, the same scene text and the same cast, asking about one marked quotation
  para   one call, only the paragraph the quotation sits in, one marked quotation
  plain  the same paragraph with **no `[Qn: ...]` marker at all**, the quotation quoted back
         in the question instead
  scene-plain  the scene text, unmarked, the quotation quoted back — the fourth cell of the
         window x marking square, without which neither effect can be told from the other

`scene` isolates the batch array from the context: same text, same candidates, one answer.
`para` then isolates the long context from the task. `plain` is the last harness suspect the
2026-09-08 entry names and does not test: the markers themselves. If a 1B model reads
`"Yes, I'll go," said Jock.` correctly but `[Q1: "Yes, I'll go,"] said Jock.` wrongly, the
marking is the fault and it is ours, not the model's. **The cast is the scene's cast in all
three**, deliberately — give `para` a two-name cast and it wins for a reason that has nothing
to do with what is being measured.

    python3 predictors/explicit_probe.py build/bakeoff --novels AHandfulOfDust --sample 30

Results are appended to `<dump>/<novel>.explicit-probe.tsv` as they are produced, and a rerun
skips what is already there. That is not tidiness: this container is reclaimed every 20-40
minutes (CLAUDE.md §9, handoff §6) and a probe that only prints at the end would lose the run.
"""
import argparse
import csv
import os
import random
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import slm_predict as sp

CONDITIONS = ("batch", "scene", "scene-plain", "para", "plain")
# `raw` is the model's literal answer, kept beside the resolved one because they fail
# differently and the difference is the next item on this ticket's list: an empty `raw` is a
# piece dropped on alignment, `"?"` is the free out the prompt offers and the model takes ~90%
# of the time, and a name that resolved to nothing would be a `resolve` bug.
FIELDS = ("quoteID", "condition", "gold", "raw", "predicted", "offered", "context")


def words(name):
    """`Pdnc.words`, ported. Both sides folded the same way — see its doc for why."""
    return {w for w in re.sub(r"[^a-z ]", " ", name.lower()).split(" ") if w}


def matches(predicted, gold):
    """`Pdnc.matches`, ported: count a match when either name's words contain the other's.

    **This has to be the harness's rule, not a stricter one.** Scoring `Reggie` against gold
    `Reggie St Cloud` as a miss — which exact-match-after-normalising does — understates this
    probe against the very 51.5% it exists to explain, and an understated diagnostic sends the
    next session after the wrong bug. Porting a scorer is normally the thing this repository
    refuses (`ExternalCandidate`: what is compared is models, not two scoring codebases); the
    exception is narrow and deliberate, because the alternative here is two rules rather than
    one. Anything that decides a candidate still goes through `Bakeoff`.
    """
    p, g = words(predicted), words(gold)
    if not p or not g:
        return False
    return p == g or p >= g or g >= p


def gold_of(novel_dir):
    """{quoteID: (speaker, quoteType)} — the answers, which is why this file is a diagnostic."""
    with open(os.path.join(novel_dir, "quotation_info.csv"), encoding="utf-8") as fh:
        return {r["quoteID"]: (r["speaker"], r["quoteType"]) for r in csv.DictReader(fh)}


def tag_excerpt(paragraphs, question, width=70):
    """The text just after the quotation closes — where an Explicit tag sits.

    Printed with every row so a failure can be read rather than guessed at. Guessing is what
    the 2026-09-08 entry asks the next session to stop doing.
    """
    for p in paragraphs:
        if p["n"] == question["paragraph"]:
            return " ".join(p["text"][question["end"]:question["end"] + width].split())
    return ""


def ask(llm, cast, text, count, max_tokens, quote=None):
    """One grammar-constrained call. Returns the parsed list of `count` names, or None.

    `quote` switches to the unmarked form: the passage as the novel wrote it and the words
    quoted back in the question. Everything else — system prompt, grammar, temperature — is
    held identical, so the only difference measured is the marking.
    """
    from llama_cpp import LlamaGrammar
    if quote is not None:
        prompt = (
            f"Cast: {', '.join(cast)}\n\n"
            f"Passage:\n{text}\n\n"
            f"Who speaks the words \u201c{quote}\u201d in this passage? "
            f"Answer with a JSON array of 1 name."
        )
    else:
        prompt = (
            f"Cast: {', '.join(cast)}\n\n"
            f"Scene:\n{text}\n\n"
            f"Who speaks each of the {count} marked quotations? "
            f"Answer with a JSON array of {count} names."
        )
    try:
        grammar = LlamaGrammar.from_string(sp.grammar_for(cast, count), verbose=False)
    except Exception:                                    # noqa: BLE001 — a call, not the run
        grammar = None
    out = llm.create_chat_completion(
        messages=[{"role": "system", "content": sp.SYSTEM}, {"role": "user", "content": prompt}],
        max_tokens=max_tokens, temperature=0.0, grammar=grammar,
    )["choices"][0]["message"]["content"]
    return sp.parse_answer(out, count)


def one_marked(paragraphs, questions, piece, target):
    """`piece`'s text with only `target` marked, as `[Q1: ...]`, plus that paragraph alone.

    Both conditions mark exactly one quotation. Leaving the other markers in place would ask
    the model to answer about Q7 of 32 while showing it 32 markers, which is neither of the
    two things being compared.
    """
    scene_lines, para_line = [], None
    for p in paragraphs:
        if not (piece["start"] <= p["n"] < piece["endExclusive"]):
            continue
        text = p["text"]
        if p["n"] == target["paragraph"]:
            text = (text[: target["start"]]
                    + f"[Q1: {text[target['start']:target['end']]}]"
                    + text[target["end"]:])
            para_line = text
        scene_lines.append(text)
    return ("\n\n".join(scene_lines) if para_line else None), para_line


def one_unmarked(paragraphs, piece):
    """The piece's text exactly as the novel wrote it — no markers of any kind."""
    return "\n\n".join(p["text"] for p in paragraphs
                        if piece["start"] <= p["n"] < piece["endExclusive"]) or None


def load_done(path):
    if not os.path.exists(path):
        return set()
    with open(path, encoding="utf-8") as fh:
        return {(r["quoteID"], r["condition"]) for r in csv.DictReader(fh, delimiter="\t")}


def report(path):
    """Coverage, precision and accuracy per condition — the three numbers, never one.

    **Precision is correct over *attributed*, not over asked.** [Bakeoff]'s doc is explicit
    that these three trade against each other and that the tradeoff is the decision, and the
    51.5% this probe exists to explain is a precision. Dividing by everything asked would
    print an accuracy under a precision's name and make every condition look like the same
    kind of failure, when declining and answering wrongly are opposite problems with opposite
    fixes — the whole point of the `"?"` and `dropped` columns beside them.

    `offered` repeats precision over just the quotations whose gold speaker was on the
    candidate list. The candidate-list gap caps precision at 94.4% and is a known, separate
    issue, so a prompt fix shows up in that column first.
    """
    with open(path, encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh, delimiter="\t"))
    def pct(n, of):
        return "%6.1f%%" % (100.0 * n / of) if of else "     — "

    print("\n%-11s %6s %6s %6s %7s %7s %7s  %7s %5s %7s" % (
        "", "asked", "answ.", "corr.", "cover", "prec.", "acc.", "prec.@off", '"?"', "dropped"))
    for condition in CONDITIONS:
        here = [r for r in rows if r["condition"] == condition]
        if not here:
            continue
        answered = [r for r in here if r["predicted"]]
        ok = [r for r in answered if matches(r["predicted"], r["gold"])]
        off = [r for r in answered if r["offered"] == "yes"]
        ok_off = [r for r in off if r in ok]
        print("%-11s %6d %6d %6d %7s %7s %7s  %7s %5d %7d" % (
            condition, len(here), len(answered), len(ok),
            pct(len(answered), len(here)),               # coverage
            pct(len(ok), len(answered)),                 # precision — over attributed
            pct(len(ok), len(here)),                     # accuracy
            pct(len(ok_off), len(off)),
            sum(1 for r in here if r["raw"] == "?"),
            sum(1 for r in here if not r["raw"])))
    # The per-condition table above pools every row in the file, and `--sample` can be raised
    # between runs, so two conditions there need not rest on the same quotations. A difference
    # between conditions is only a difference if it is measured on the same questions, so it
    # gets its own table over the quotations both conditions actually answered.
    by_condition = {c: {r["quoteID"]: r for r in rows if r["condition"] == c} for c in CONDITIONS}
    pairs = [(a, b) for i, a in enumerate(CONDITIONS) for b in CONDITIONS[i + 1:]
             if by_condition[a] and by_condition[b]]
    if pairs:
        print("\nPaired on the same quotations")
        for a, b in pairs:
            shared = sorted(set(by_condition[a]) & set(by_condition[b]))
            answered = [q for q in shared
                        if by_condition[a][q]["predicted"] and by_condition[b][q]["predicted"]]
            if not answered:
                continue
            ok_a = sum(1 for q in answered if matches(by_condition[a][q]["predicted"], by_condition[a][q]["gold"]))
            ok_b = sum(1 for q in answered if matches(by_condition[b][q]["predicted"], by_condition[b][q]["gold"]))
            print("  %-11s %6.1f%%  vs  %-11s %6.1f%%  on %d both answered (%d shared)" % (
                a, 100.0 * ok_a / len(answered), b, 100.0 * ok_b / len(answered),
                len(answered), len(shared)))

    print("\nWrong answers, %s:" % os.path.basename(path))
    for r in rows:
        if not r["predicted"] or not matches(r["predicted"], r["gold"]):
            said = r["predicted"] or ("?" if r["raw"] == "?" else "(piece dropped)")
            print("  %-6s %-5s gold %-18s said %-18s | %s" % (
                r["quoteID"], r["condition"], r["gold"][:18], said[:18], r["context"][:60]))


def probe_novel(llm, dump_dir, novel, corpus, sample, max_tokens, seed, only=CONDITIONS):
    paragraphs = sp.read_jsonl(os.path.join(dump_dir, f"{novel}.paragraphs.jsonl"))
    questions = sp.read_jsonl(os.path.join(dump_dir, f"{novel}.questions.jsonl"))
    pieces = sp.read_jsonl(os.path.join(dump_dir, f"{novel}.scenes.jsonl"))
    cast = sp.cast_of(os.path.join(corpus, "data", novel))
    gold = gold_of(os.path.join(corpus, "data", novel))
    by_id = {q["id"]: q for q in questions}

    explicit = [q["id"] for q in questions if gold.get(q["id"], ("", ""))[1] == "Explicit"]
    # Shuffle once and take a prefix, rather than `sample`, so raising --sample keeps every
    # quotation the earlier run chose. Otherwise the conditions stop being paired on the same
    # quotations and the comparison between them quietly stops meaning anything.
    order = list(explicit)
    random.Random(seed).shuffle(order)
    chosen = set(order[:min(sample, len(order))])
    print(f"{novel}: {len(explicit)} Explicit quotations, probing {len(chosen)}")

    out_path = os.path.join(dump_dir, f"{novel}.explicit-probe.tsv")
    done = load_done(out_path)
    fresh = not os.path.exists(out_path)
    fh = open(out_path, "a", encoding="utf-8", newline="")
    writer = csv.DictWriter(fh, FIELDS, delimiter="\t")
    if fresh:
        writer.writeheader()

    def record(qid, condition, raw, predicted, offered, context):
        writer.writerow({"quoteID": qid, "condition": condition, "gold": gold[qid][0],
                         "raw": raw or "", "predicted": predicted or "",
                         "offered": "yes" if offered else "no", "context": context})
        fh.flush()                                       # the container may not see the end

    started, calls = time.time(), 0
    for n, piece in enumerate(pieces, start=1):
        text, inside = sp.mark_quotations(paragraphs, questions, piece)
        if not text:
            continue
        here = [q for q in inside if q["id"] in chosen]
        if not here:
            continue
        # The scene's cast, held constant across all three conditions — see the module doc.
        scene_names = sp.scene_cast(text, cast, [])

        if "batch" in only and any((q["id"], "batch") not in done for q in here):
            named = ask(llm, scene_names, text, len(inside), max_tokens)
            calls += 1
            for q, name in zip(inside, named or [None] * len(inside)):
                if q["id"] in chosen and (q["id"], "batch") not in done:
                    record(q["id"], "batch", name, sp.resolve(name, scene_names) if name else None,
                           gold[q["id"]][0] in scene_names, tag_excerpt(paragraphs, q))

        for q in here:
            scene_text, para_text = one_marked(paragraphs, questions, piece, q)
            unmarked = next((p["text"] for p in paragraphs if p["n"] == q["paragraph"]), None)
            quote = unmarked[q["start"]:q["end"]] if unmarked else None
            scene_unmarked = one_unmarked(paragraphs, piece)
            plans = (("scene", scene_text, None), ("scene-plain", scene_unmarked, quote),
                     ("para", para_text, None), ("plain", unmarked, quote))
            for condition, body, quoted in plans:
                if condition not in only or (q["id"], condition) in done or not body:
                    continue
                named = ask(llm, scene_names, body, 1, max_tokens, quote=quoted)
                calls += 1
                record(q["id"], condition, named[0] if named else None,
                       sp.resolve(named[0], scene_names) if named else None,
                       gold[q["id"]][0] in scene_names, tag_excerpt(paragraphs, q))

        print(f"    piece {n}/{len(pieces)}  {len(here)} probed  "
              f"{calls} calls  {(time.time() - started) / max(1, calls):.1f}s/call", flush=True)
    fh.close()
    return out_path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump_dir")
    ap.add_argument("--corpus", default=os.path.expanduser("~/.cache/quire/pdnc"))
    ap.add_argument("--novels", default="")
    ap.add_argument("--model", default="", help="a local GGUF; overrides the HF download")
    ap.add_argument("--sample", type=int, default=30)
    # A `scene` call costs several times a `para` one, so firming up one comparison on a
    # larger sample should not mean paying for the conditions already settled.
    ap.add_argument("--conditions", default=",".join(CONDITIONS),
                    help="comma-separated subset of " + ",".join(CONDITIONS))
    ap.add_argument("--seed", type=int, default=0)
    ap.add_argument("--ctx", type=int, default=4096)
    ap.add_argument("--max-tokens", type=int, default=512)
    ap.add_argument("--threads", type=int, default=os.cpu_count() or 4)
    ap.add_argument("--report-only", action="store_true", help="re-read the TSV, run nothing")
    args = ap.parse_args()

    wanted = [n.strip() for n in args.novels.split(",") if n.strip()] or sorted(
        f[: -len(".questions.jsonl")]
        for f in os.listdir(args.dump_dir) if f.endswith(".questions.jsonl"))

    if args.report_only:
        for novel in wanted:
            report(os.path.join(args.dump_dir, f"{novel}.explicit-probe.tsv"))
        return 0

    llm = sp.load_model(args.model, args.ctx, args.threads)
    for novel in wanted:
        report(probe_novel(llm, args.dump_dir, novel, args.corpus, args.sample,
                           args.max_tokens, args.seed,
                           only=tuple(c.strip() for c in args.conditions.split(",") if c.strip())))
    return 0


if __name__ == "__main__":
    sys.exit(main())
