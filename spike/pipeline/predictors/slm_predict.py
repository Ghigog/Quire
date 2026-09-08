#!/usr/bin/env python3
"""
The scene-level SLM candidate for QUI-028's second half — the one ADR-0005 leaves open.

ADR-0005 closed the encoder question: neither published encoder replaces Tier 1, because
both take the wrong-voice rate from ~3% to ~40%. ADR-0006 says the remaining candidate is a
generative model prompted **once per scene**, resolving turn-taking in context the way a
reader does. Nothing had run that prompt against anything.

This does, on a quantized 1B model on CPU, scored by the same PDNC harness that measured
Tier 1, BookNLP and the SpanBERT encoder.

    python3 -m pip install llama-cpp-python huggingface_hub
    cd spike/pipeline && gradle run --args="dump --out build/bakeoff --novels AHandfulOfDust"
    python3 predictors/slm_predict.py build/bakeoff --novels AHandfulOfDust

**What transfers off this host and what does not.** Accuracy does — it is a property of the
model and the prompt, which is why PDNC scoring has been valid all along. Speed does not:
`spike/hostbench/README.md` is emphatic that an x86 core is not a Cortex-A77, and QUI-031
owns the on-device timing. So this answers "does scene-level attribution beat Tier 1 on
wrong-voice rate", and says nothing about whether it fits 30 minutes.

**Scenes come from the dump, not from here.** `bakeoff dump` runs QUI-038's real segmenter
and splitter and writes the pieces; reimplementing that in Python would score the model
against context it never saw (CLAUDE.md §9).
"""
import argparse
import ast
import csv
import json
import os
import re
import sys
import time

os.environ.setdefault("HF_HUB_DISABLE_XET", "1")        # see grimbert_predict.py

REPO = "bartowski/Llama-3.2-1B-Instruct-GGUF"
FILENAME = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"

SYSTEM = (
    "You attribute dialogue in novels. You are given a scene with its quotations numbered "
    "Q1, Q2, and so on, and the list of characters who appear in this book. For each "
    "quotation you name the character who speaks it. You answer only with a JSON array of "
    "strings, one per quotation, in order. Use a name exactly as it appears in the cast "
    "list, or \"?\" when the scene does not tell you."
)


def read_jsonl(path):
    with open(path, encoding="utf-8") as fh:
        return [json.loads(line) for line in fh if line.strip()]


def normalise(text):
    """Lowercase, and drop the punctuation that separates `Mrs. Beaver` from `Mrs Beaver`."""
    return re.sub(r"[^\w\s]", "", text.lower())


def cast_of(novel_dir):
    """{main name: [every name this character is called]}, from PDNC's own list.

    **The aliases matter, and leaving them out silently broke three runs.** PDNC's
    `Main Name` for a character can be `Mrs. Beaver` while the novel writes `Mrs Beaver`,
    so matching main names alone against the text misses the speaker in scenes that name
    her outright — the correct answer was simply not among the candidates offered, and the
    model could only answer "?" or wrongly. It scored 45.6% on Explicit quotations, where a
    speech tag names the speaker in the sentence, which is what gave it away. The other
    predictors in this directory read the alias set; this one did not.
    """
    out = {}
    with open(os.path.join(novel_dir, "character_info.csv"), encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            names = {row["Main Name"]}
            try:
                names |= set(ast.literal_eval(row["Aliases"]))
            except (ValueError, SyntaxError):
                pass
            out[row["Main Name"]] = sorted(names, key=len, reverse=True)
    return out


def mark_quotations(paragraphs, questions, piece):
    """The piece's text with each gold quotation replaced by a numbered marker.

    Marking rather than merely numbering: the model has to answer about *these* spans and
    no others, and a marker it can see is far more reliable than an instruction to count
    quotation marks itself.
    """
    inside = sorted(
        (q for q in questions if piece["start"] <= q["paragraph"] < piece["endExclusive"]),
        key=lambda q: (q["paragraph"], q["start"]),
    )
    if not inside:
        return None, []

    by_paragraph = {}
    for n, q in enumerate(inside, start=1):
        by_paragraph.setdefault(q["paragraph"], []).append((n, q))

    lines = []
    for p in paragraphs:
        if not (piece["start"] <= p["n"] < piece["endExclusive"]):
            continue
        text = p["text"]
        for n, q in sorted(by_paragraph.get(p["n"], []), key=lambda it: it[1]["start"], reverse=True):
            text = text[: q["start"]] + f"[Q{n}: {text[q['start']:q['end']]}]" + text[q["end"]:]
        lines.append(text)
    return "\n\n".join(lines), inside


def grammar_for(cast, count):
    """A GBNF grammar admitting exactly `count` names, each one from `cast` or "?".

    **Asking a 1B model for JSON and hoping is not a measurement.** Unconstrained, this model
    ignores "reply with a JSON array" and returns an object mapping names to quotation text —
    104 of 112 pieces were unparseable on the first run, which says nothing whatever about
    whether it can attribute dialogue. Constrained decoding removes the format from the
    experiment so what is left is the judgement, and it is also what QUI-006's structured
    output contract specifies, so it is what would ship.

    Restricting names to the cast matters as much as the array shape: it turns "write a name"
    into "choose a character", which is the task, and it makes an unresolvable hallucination
    impossible rather than merely unlikely.
    """
    def literal(text):
        return '"\\"' + text.replace("\\", "\\\\").replace('"', '\\"') + '\\""'

    alternatives = " | ".join(literal(name) for name in cast + ["?"])
    return (
        'root ::= "[" ' + " \",\" ".join(["item"] * count) + ' "]"\n'
        "item ::= " + alternatives + "\n"
    )


def parse_answer(raw, expected):
    """A JSON array of `expected` names, or None.

    **Alignment fails closed** (ADR-0006): a reply with the wrong number of entries drops the
    whole piece rather than shifting every speaker by one. A misaligned list is confidently
    wrong for a scene at a time, which ADR-0005 makes the most expensive failure here.
    """
    match = re.search(r"\[.*?\]", raw, re.S)
    if not match:
        return None
    for loader in (json.loads, ast.literal_eval):
        try:
            value = loader(match.group())
        except (ValueError, SyntaxError):
            continue
        if isinstance(value, list) and len(value) == expected:
            return [str(v).strip() for v in value]
        return None
    return None


def resolve(name, cast):
    """Map a generated name onto the cast, or None. Case- and prefix-tolerant, nothing more."""
    if not name or name == "?":
        return None
    lowered = name.lower()
    for c in cast:
        if c.lower() == lowered:
            return c
    for c in cast:
        if lowered in c.lower() or c.lower() in lowered:
            return c
    return None


def scene_cast(text, cast, recent):
    """The characters this scene could plausibly be about.

    **Offering the whole book's cast is not the same task.** `A Handful of Dust` names 104
    characters; asking a 1B model to pick one of 104 for every quotation is a far harder
    problem than the one the encoders were scored on, because BookNLP and grimbert only ever
    rank mentions *present in the window*. A reader has the same advantage: they are choosing
    between the people in the room, not the people in the book.

    So a scene offers the names that appear in its own text, plus whoever spoke most recently
    before it — which is how a speaker survives a paragraph that does not name them.
    """
    lowered = normalise(text)
    present = [main for main, names in cast.items()
               if any(normalise(n) in lowered for n in names)]
    for name in recent:
        if name not in present:
            present.append(name)
    return present or list(cast)


def predict_novel(llm, dump_dir, novel, corpus, max_tokens):
    from llama_cpp import LlamaGrammar
    paragraphs = read_jsonl(os.path.join(dump_dir, f"{novel}.paragraphs.jsonl"))
    questions = read_jsonl(os.path.join(dump_dir, f"{novel}.questions.jsonl"))
    pieces = read_jsonl(os.path.join(dump_dir, f"{novel}.scenes.jsonl"))
    cast = cast_of(os.path.join(corpus, "data", novel))

    answers, misaligned, asked, recent = {}, 0, 0, []
    started = time.time()
    for done, piece in enumerate(pieces, start=1):
        text, inside = mark_quotations(paragraphs, questions, piece)
        if not text:
            continue
        asked += len(inside)
        here = scene_cast(text, cast, recent[-4:])
        prompt = (
            f"Cast: {', '.join(here)}\n\n"
            f"Scene:\n{text}\n\n"
            f"Who speaks each of the {len(inside)} marked quotations? "
            f"Answer with a JSON array of {len(inside)} names."
        )
        try:
            grammar = LlamaGrammar.from_string(grammar_for(here, len(inside)), verbose=False)
        except Exception:                                # noqa: BLE001 — a piece, not the run
            grammar = None
        out = llm.create_chat_completion(
            messages=[{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}],
            max_tokens=max_tokens, temperature=0.0, grammar=grammar,
        )["choices"][0]["message"]["content"]

        named = parse_answer(out, len(inside))
        if named is None:
            misaligned += 1

        # A whole-novel run is tens of minutes of silence otherwise, and the useful
        # diagnostics — is it answering at all, is it dropping every piece on alignment —
        # are visible from the first few pieces.
        rate = (time.time() - started) / done
        print(f"    {novel} piece {done}/{len(pieces)}  {len(inside):3d} quotes  "
              f"{'dropped' if named is None else 'ok':>7}  {rate:.1f}s/piece", flush=True)
        if named is None:
            continue                                    # fails closed: the whole piece drops
        for question, name in zip(inside, named):
            resolved = resolve(name, here)
            if resolved:
                answers[question["id"]] = resolved
                if resolved in recent:
                    recent.remove(resolved)
                recent.append(resolved)

    out_path = os.path.join(dump_dir, f"{novel}.answers.tsv")
    with open(out_path, "w", encoding="utf-8") as fh:
        fh.write(f"# slm {FILENAME}, one call per scene piece\n")
        for q in questions:
            fh.write("%s\t%s\tslm\n" % (q["id"], answers.get(q["id"], "")))
    print(f"  {novel}: {len(pieces)} pieces, {asked} quotations asked, "
          f"{len(answers)} answered, {misaligned} pieces dropped on alignment")
    return asked, len(answers), misaligned


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump_dir")
    ap.add_argument("--corpus", default=os.path.expanduser("~/.cache/quire/pdnc"))
    ap.add_argument("--novels", default="")
    ap.add_argument("--ctx", type=int, default=4096)
    ap.add_argument("--max-tokens", type=int, default=512)
    ap.add_argument("--threads", type=int, default=os.cpu_count() or 4)
    args = ap.parse_args()

    from huggingface_hub import hf_hub_download
    from llama_cpp import Llama

    wanted = [n.strip() for n in args.novels.split(",") if n.strip()] or sorted(
        f[: -len(".questions.jsonl")]
        for f in os.listdir(args.dump_dir) if f.endswith(".questions.jsonl"))

    path = hf_hub_download(REPO, FILENAME)
    print(f"loading {FILENAME} ({os.path.getsize(path) / 1e6:.0f} MB)")
    llm = Llama(model_path=path, n_ctx=args.ctx, n_threads=args.threads, verbose=False)

    asked = answered = dropped = 0
    for novel in wanted:
        a, b, c = predict_novel(llm, args.dump_dir, novel, args.corpus, args.max_tokens)
        asked += a; answered += b; dropped += c
    print(f"\n{answered}/{asked} answered, {dropped} pieces dropped on alignment. Score with:")
    print(f'  gradle run --args="bakeoff --candidate slm --answers {args.dump_dir}"')


if __name__ == "__main__":
    sys.exit(main())
