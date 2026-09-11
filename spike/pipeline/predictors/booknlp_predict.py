#!/usr/bin/env python3
"""
The BookNLP candidate for QUI-028: the ~63% the field reports against, at 57 MB.

Same contract as `grimbert_predict.py` — reads what `bakeoff dump` wrote, writes
`<novel>.answers.tsv`, and the scoring stays in Kotlin so the two candidates are compared
by one scorer. The mentions are built the same way for the same reason: matching PDNC's
alias lists, not gold offsets, because the device will not have gold offsets either.

    tools/fetch-attribution-models.sh
    cd spike/pipeline && gradle run --args="dump --out build/bakeoff --novels AHandfulOfDust"
    python3 predictors/booknlp_predict.py build/bakeoff --novels AHandfulOfDust

**Why `small` and not `big`.** `L-8_H-256_A-4` is 14M parameters and 57 MB; the `L-12_H-768`
model is 108M and 438 MB. PRD §5 allows 450 MB for the whole app including TTS voices, so
only this one could ever ship. The 431 MB SpanBERT encoder measured before it failed on
size, on speed and on wrong-voice rate together; this is the same family an order of
magnitude smaller.

**How BookNLP frames the problem**, which is worth knowing before reading the numbers: the
target quotation is replaced by a single `[QUOTE]` token in a ±50-word window — the model
never sees the words inside the quotation it is attributing — and it ranks up to ten nearby
person mentions by distance. So it cannot invent a speaker and it cannot reach one more than
about fifty words away. Both are the right shape for a reader and both cap its recall.
"""
import argparse
import ast
import csv
import json
import os
import re
import sys

os.environ.setdefault("HF_HUB_DISABLE_XET", "1")        # see grimbert_predict.py

MODELS = {
    # QUI-028's candidate: 14M parameters, 57 MB, the only one that fits PRD §5 as it stands.
    "small": "speaker_google_bert_uncased_L-8_H-256_A-4-v1.0.1.model",
    # QUI-041's: 110M parameters, 438 MB fp32 — the BERT-base encoder class the research
    # answer points at. Too big to ship untouched; the point is to find out what the extra
    # 96M parameters actually buy before deciding whether to pay for them.
    "big": "speaker_google_bert_uncased_L-12_H-768_A-12-v1.0.1.model",
}
TOKEN = re.compile(r"\w+|[^\w\s]")


class Tok:
    """What BookNLP's representation builder reads off a token: text and paragraph."""

    __slots__ = ("text", "paragraph_id")

    def __init__(self, text, paragraph_id):
        self.text = text
        self.paragraph_id = paragraph_id


def read_jsonl(path):
    with open(path, encoding="utf-8") as fh:
        return [json.loads(line) for line in fh if line.strip()]


def gold_cast(novel_dir):
    """PDNC's own character list, aliases and all. Not a setting we can ship — see below."""
    out = {}
    with open(os.path.join(novel_dir, "character_info.csv"), encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            names = {row["Main Name"]}
            try:
                names |= set(ast.literal_eval(row["Aliases"]))
            except (ValueError, SyntaxError):
                pass
            out[row["Character ID"]] = (row["Main Name"], sorted(names, key=len, reverse=True))
    return out


def bootstrap_cast(dump_dir, novel):
    """The cast our own roster finds in the prose, with no gold anything (QUI-041).

    The research answer is that the published 94.5% assumes gold candidate lists, and that
    end to end with cast discovery in front of it the figure is 82-85%. This is the way to
    see that gap rather than take it on trust: the same model, the same questions, the only
    difference being where the list of possible speakers came from. Each discovered name is
    its own entity — the roster does not claim `Elizabeth` and `Miss Bennet` are one person,
    and pretending otherwise here would quietly hand back the gold knowledge.
    """
    names = [row["name"] for row in read_jsonl(os.path.join(dump_dir, f"{novel}.cast.jsonl"))]
    return {name: (name, [name]) for name in names}


def build(dump_dir, novel, cast):
    """Tokens, quote spans, PER entities, and the question id behind each quote."""
    paragraphs = read_jsonl(os.path.join(dump_dir, f"{novel}.paragraphs.jsonl"))
    questions = read_jsonl(os.path.join(dump_dir, f"{novel}.questions.jsonl"))

    tokens, spans, base = [], [], {}
    for p in paragraphs:
        base[p["n"]] = len(tokens)
        for m in TOKEN.finditer(p["text"]):
            tokens.append(Tok(m.group(), p["n"]))
            spans.append((p["n"], m.start(), m.end()))

    # Character offsets within a paragraph -> inclusive token indices in the document.
    by_paragraph = {}
    for i, (n, s, e) in enumerate(spans):
        by_paragraph.setdefault(n, []).append((i, s, e))

    quotes, asked = [], []
    for q in questions:
        toks = by_paragraph.get(q["paragraph"], [])
        inside = [i for i, s, e in toks if e > q["start"] and s < q["end"]]
        if not inside:
            continue                                    # unanswered, and scored as such
        quotes.append((inside[0], inside[-1]))
        asked.append(q["id"])

    lowered = [t.text.lower() for t in tokens]
    # Longest name first, across the whole cast and not just within one character. `Lady
    # Catherine` and `Catherine` are separate entries in a discovered roster, and whichever
    # is tried first wins the tokens: scanning in name order let `Catherine` eat the second
    # half of every `Lady Catherine`, so the longer name matched nowhere and the model was
    # offered a candidate list that never contained her.
    claims = sorted(
        ((cid, name, [p.lower() for p in TOKEN.findall(name)])
         for cid, (_, names) in cast.items() for name in names),
        key=lambda c: -len(c[2]))
    entities, owner, taken = [], [], set()
    for cid, name, parts in claims:
        if not parts:
            continue
        for i in range(len(lowered) - len(parts) + 1):
            if lowered[i:i + len(parts)] == parts and not (taken & set(range(i, i + len(parts)))):
                entities.append((i, i + len(parts) - 1, "PROP_PER", name))
                owner.append(cid)
                taken |= set(range(i, i + len(parts)))
    order = sorted(range(len(entities)), key=lambda k: entities[k][0])
    return tokens, quotes, [entities[k] for k in order], [owner[k] for k in order], asked


def write_answers(out_dir, novel, threshold, note=""):
    """Apply a confidence threshold to cached scores. No model needed."""
    kept = 0
    with open(os.path.join(out_dir, f"{novel}.scores.tsv"), encoding="utf-8") as src, \
            open(os.path.join(out_dir, f"{novel}.answers.tsv"), "w", encoding="utf-8") as dst:
        dst.write(f"# booknlp {note}, threshold {threshold}\n")
        for line in src:
            qid, name, score = line.rstrip("\n").split("\t")
            if name and float(score) >= threshold:
                kept += 1
            else:
                name = ""
            dst.write(f"{qid}\t{name}\tbooknlp\n")
    return kept


def predict_novel(qa, dump_dir, out_dir, novel, cast, threshold, note=""):
    import torch

    tokens, quotes, entities, owner, asked = build(dump_dir, novel, cast)
    if not quotes or not entities:
        print(f"  {novel}: nothing to attribute ({len(quotes)} quotes, {len(entities)} mentions)")
        return 0, 0

    texts, metas, positions, global_positions, quote_indexes = qa.get_representation(
        quotes, entities, tokens)
    # Keyed the way BookNLP reports positions: entity end is exclusive there.
    where = {(s, e + 1): i for i, (s, e, _, _) in enumerate(entities)}

    def base_of(start, end, chain, seen_keys=()):
        """Follow a quote-to-quote attribution back to the entity it ultimately names.

        BookNLP lets the winning candidate be *another quotation* rather than a mention —
        which is how it handles turn-taking, and it is most of its answer on untagged
        dialogue. Resolving the chain is therefore not an optimisation: without it every
        such prediction is thrown away, which cost 66% of coverage before this was added.
        A cycle would loop forever, so keys already visited end the walk.
        """
        while (start, end) in chain and (start, end) not in seen_keys:
            seen_keys = seen_keys + ((start, end),)
            start, end = chain[(start, end)]
        return start, end

    answers, chain = {}, {}
    x_batches, m_batches, _, _ = qa.model.get_batches(texts, metas)
    seen = 0
    with torch.no_grad():
        for xb, mb in zip(x_batches, m_batches):
            preds = qa.model.forward(xb, mb)             # (batch, 10, 1)
            for row in preds:
                # BookNLP's own `tag` writes results at the prediction index rather than the
                # quote index, which drifts whenever a quotation has no candidates. Indexing
                # through quote_indexes instead keeps answers on the right quotation.
                q_id = quote_indexes[seen]
                valid = len(metas[seen][1])
                scores = torch.softmax(row[:valid, 0], dim=0)
                best = int(torch.argmax(scores))
                kind, start, end, _ = positions[seen][best]
                if kind == "QUOTE":
                    start, end = base_of(start, end, chain)
                idx = where.get((start, end))
                if idx is not None:
                    chain[quotes[q_id]] = (start, end)
                    if float(scores[best]) >= threshold:
                        answers[asked[q_id]] = (cast[owner[idx]][0], float(scores[best]))
                seen += 1

    # Every prediction with its confidence, so a threshold sweep costs a file read rather
    # than another pass of the model. Inference is the expensive part and the threshold is
    # the parameter most worth moving — see the ticket.
    with open(os.path.join(out_dir, f"{novel}.scores.tsv"), "w", encoding="utf-8") as fh:
        for qid in asked:
            name, score = answers.get(qid, ("", 0.0))
            fh.write(f"{qid}\t{name}\t{score:.4f}\n")
    kept = write_answers(out_dir, novel, threshold, note)
    print(f"  {novel}: {len(asked)} quotations, {len(answers)} predicted, {kept} kept "
          f"at >= {threshold}")
    return len(asked), len(answers)


def loader(path):
    """QuotationAttribution's constructor, with one checkpoint-age fix.

    The checkpoint was saved when `position_ids` was a registered buffer on BERT's
    embeddings; current transformers computes it instead, so a strict load rejects the key.
    Dropping it is exact — it held `arange(max_position_embeddings)` and nothing learned —
    and it is done here rather than by patching the installed package.

    Returns a `QuotationAttribution`, whose `get_representation` builds the ±50-word window
    around each quotation. That window is the shape QUI-041 wanted and BookNLP already has
    it: the target quotation becomes one `[QUOTE]` token and the prose either side of it —
    where the speech tag lives — is what the model reads.
    """
    import torch
    from booknlp.english.bert_qa import QuotationAttribution
    from booknlp.english.speaker_attribution import BERTSpeakerID

    base = re.sub("google_bert", "google/bert", os.path.basename(path))
    qa = QuotationAttribution.__new__(QuotationAttribution)
    qa.model = BERTSpeakerID(base_model=re.sub(r"\.model$", "", base))
    state = torch.load(path, map_location="cpu", weights_only=True)
    state.pop("bert.embeddings.position_ids", None)
    qa.model.load_state_dict(state)
    qa.model.eval()
    return qa


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump_dir")
    ap.add_argument("--corpus", default=os.path.expanduser("~/.cache/quire/pdnc"))
    ap.add_argument("--models", default=os.path.expanduser("~/.cache/quire/models"))
    ap.add_argument("--novels", default="")
    ap.add_argument("--out", default="",
                    help="where answers and scores go; defaults to the dump directory. Give "
                         "each configuration its own, or the next run overwrites the last")
    ap.add_argument("--size", choices=sorted(MODELS), default="small",
                    help="small = QUI-028's 14M checkpoint; big = QUI-041's 110M one")
    ap.add_argument("--cast", choices=("gold", "bootstrap"), default="gold",
                    help="gold reads PDNC's character list; bootstrap reads the roster our "
                         "own indexer found, which is the only one a device will have")
    ap.add_argument("--threshold", type=float, default=0.0,
                    help="0 keeps every answer; raise it to trade coverage for precision")
    ap.add_argument("--rethreshold", action="store_true",
                    help="re-apply --threshold to cached scores without running the model")
    args = ap.parse_args()
    out_dir = args.out or args.dump_dir
    os.makedirs(out_dir, exist_ok=True)

    wanted = [n.strip() for n in args.novels.split(",") if n.strip()] or sorted(
        f[: -len(".questions.jsonl")]
        for f in os.listdir(args.dump_dir) if f.endswith(".questions.jsonl"))

    note = f"{args.size} {args.cast}-cast"

    if args.rethreshold:
        for novel in wanted:
            kept = write_answers(out_dir, novel, args.threshold, note)
            print(f"  {novel}: {kept} kept at >= {args.threshold}")
        return

    model = MODELS[args.size]
    path = os.path.join(args.models, model)
    if not os.path.exists(path):
        sys.exit(f"missing {path} — run tools/fetch-attribution-models.sh")
    print(f"loading {model} ({os.path.getsize(path) / 1e6:.0f} MB), {args.cast} cast")
    qa = loader(path)

    total = answered = 0
    for novel in wanted:
        cast = (bootstrap_cast(args.dump_dir, novel) if args.cast == "bootstrap"
                else gold_cast(os.path.join(args.corpus, "data", novel)))
        a, b = predict_novel(qa, args.dump_dir, out_dir, novel, cast, args.threshold, note)
        total += a
        answered += b
    print(f"\n{answered}/{total} answered over {len(wanted)} novels. Score with:")
    print(f'  gradle run --args="bakeoff --candidate booknlp --answers {out_dir}"')


if __name__ == "__main__":
    sys.exit(main())
