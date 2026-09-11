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

TOKEN = re.compile(r"\w+|[^\w\s]")

# The two checkpoints this runs, and what each was trained against. Getting a field here
# wrong does not crash: it feeds the model text shaped differently from its training and
# quietly costs accuracy, which is why each value below was read off the checkpoint or the
# package source rather than assumed. See `verify` in Loader.
FLAVOURS = {
    # QUI-028's shippable candidate. 14M parameters, 57 MB, uncased, four added tokens.
    "booknlp": dict(
        file="speaker_google_bert_uncased_L-8_H-256_A-4-v1.0.1.model",
        base="google/bert_uncased_L-8_H-256_A-4",
        added=["[QUOTE]", "[ALTQUOTE]", "[PAR]", "[CAP]"],
        lower=True,
        vocab=30526,
    ),
    # QUI-041's baseline (2026-09-11 directive). `bodyanats/booknlp-plus-speaker-attribution`
    # fold 2, the fold its own card names best: 72.5% on unseen novels against a 60.5% mean.
    #
    # **It is cased and it predates `[CAP]`.** Its embedding matrix is 28,999 rows, which is
    # bert-base-cased's 28,996 plus *three* added tokens — and booknlp added only three until
    # 1.0.5, when `[CAP]` and the lowercasing walk arrived together. So the current package
    # would build a 29,000-row model (a strict load then fails, which is the cheap failure)
    # and, forced past that, would feed a cased model lowercased text carrying a token it has
    # never seen (the expensive one). Hence `lower=False` and three tokens.
    "booknlp-plus": dict(
        file="booknlp_plus_split_2.model",
        base="bert-base-cased",
        added=["[QUOTE]", "[ALTQUOTE]", "[PAR]"],
        lower=False,
        vocab=28999,
    ),
}


class Tok:
    """What BookNLP's representation builder reads off a token: text and paragraph."""

    __slots__ = ("text", "paragraph_id")

    def __init__(self, text, paragraph_id):
        self.text = text
        self.paragraph_id = paragraph_id


def read_jsonl(path):
    with open(path, encoding="utf-8") as fh:
        return [json.loads(line) for line in fh if line.strip()]


def cast_of(novel_dir):
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


def build(dump_dir, novel, corpus):
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

    cast = cast_of(os.path.join(corpus, "data", novel))
    lowered = [t.text.lower() for t in tokens]
    entities, owner, taken = [], [], set()
    for cid, (_, names) in cast.items():
        for name in names:
            parts = [p.lower() for p in TOKEN.findall(name)]
            if not parts:
                continue
            for i in range(len(lowered) - len(parts) + 1):
                if lowered[i:i + len(parts)] == parts and not (taken & set(range(i, i + len(parts)))):
                    entities.append((i, i + len(parts) - 1, "PROP_PER", name))
                    owner.append(cid)
                    taken |= set(range(i, i + len(parts)))
    order = sorted(range(len(entities)), key=lambda k: entities[k][0])
    return tokens, quotes, [entities[k] for k in order], [owner[k] for k in order], asked, cast


def scores_path(dump_dir, novel, flavour):
    """Cached scores are per flavour; answers are not, because the harness names that file."""
    return os.path.join(dump_dir, f"{novel}.{flavour}.scores.tsv")


def write_answers(dump_dir, novel, threshold, flavour):
    """Apply a confidence threshold to cached scores. No model needed.

    Below the threshold the answer is blank, which the Kotlin scorer reads as *declined* and
    charges to coverage rather than to precision. That is the abstention QUI-041 requires: on
    the device a blank routes the line to the narrator, which is flat rather than wrong.
    """
    kept = 0
    with open(scores_path(dump_dir, novel, flavour), encoding="utf-8") as src, \
            open(os.path.join(dump_dir, f"{novel}.answers.tsv"), "w", encoding="utf-8") as dst:
        dst.write(f"# {flavour} {FLAVOURS[flavour]['file']}, threshold {threshold}\n")
        for line in src:
            qid, name, score = line.rstrip("\n").split("\t")
            if name and float(score) >= threshold:
                kept += 1
            else:
                name = ""
            dst.write(f"{qid}\t{name}\t{flavour}\n")
    return kept


def predict_novel(qa, dump_dir, novel, corpus, threshold, flavour):
    import torch

    lower = FLAVOURS[flavour]["lower"]
    tokens, quotes, entities, owner, asked, cast = build(dump_dir, novel, corpus)
    if not quotes or not entities:
        print(f"  {novel}: nothing to attribute ({len(quotes)} quotes, {len(entities)} mentions)")
        return 0, 0

    # `doLowerCase` has to be passed at both ends or the window is built one way and batched
    # the other. It defaults to True in the package because the shipped checkpoints are
    # uncased; BookNLP+ is not. See FLAVOURS.
    texts, metas, positions, global_positions, quote_indexes = qa.get_representation(
        quotes, entities, tokens, doLowerCase=lower)
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
    x_batches, m_batches, _, _ = qa.model.get_batches(texts, metas, doLowerCase=lower)
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
    with open(scores_path(dump_dir, novel, flavour), "w", encoding="utf-8") as fh:
        for qid in asked:
            name, score = answers.get(qid, ("", 0.0))
            fh.write(f"{qid}\t{name}\t{score:.4f}\n")
    kept = write_answers(dump_dir, novel, threshold, flavour)
    print(f"  {novel}: {len(asked)} quotations, {len(answers)} predicted, {kept} kept "
          f"at >= {threshold}")
    return len(asked), len(answers)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump_dir")
    ap.add_argument("--corpus", default=os.path.expanduser("~/.cache/quire/pdnc"))
    ap.add_argument("--models", default=os.path.expanduser("~/.cache/quire/models"))
    ap.add_argument("--novels", default="")
    ap.add_argument("--threshold", type=float, default=0.0,
                    help="0 keeps every answer; raise it to trade coverage for precision")
    ap.add_argument("--rethreshold", action="store_true",
                    help="re-apply --threshold to cached scores without running the model")
    ap.add_argument("--flavour", default="booknlp", choices=sorted(FLAVOURS),
                    help="which checkpoint to run (default: booknlp)")
    args = ap.parse_args()

    import torch
    from booknlp.english.bert_qa import QuotationAttribution
    from booknlp.english.speaker_attribution import BERTSpeakerID

    from transformers import BertConfig, BertModel, BertTokenizer

    class Loader(QuotationAttribution):
        """QuotationAttribution's constructor, built from a FLAVOURS entry rather than a name.

        The package derives everything from the checkpoint's *filename*: it regex-matches
        `-<layers>_H-<dim>_A-` out of it for the layer sizes and hands the rest to
        `from_pretrained`. That works for the two checkpoints BookNLP ships and for nothing
        else — BookNLP+'s base was a local Kaggle directory, so there is no name that both
        parses and resolves. Building the parts here keeps the filename out of it.

        Two checkpoint-age fixes ride along:

        - `position_ids` was a registered buffer on BERT's embeddings when these were saved
          and is computed now, so a strict load rejects the key. Dropping it is exact — it
          held `arange(max_position_embeddings)` and nothing learned.
        - The BERT weights are never downloaded, only its config. `from_pretrained` would
          fetch ~436 MB of parameters that `load_state_dict` overwrites on the next line.

        The load is strict, which is the point: a wrong vocab, a wrong depth or a wrong
        hidden size fails here rather than 20 minutes later as a plausible-looking number.
        """

        def __init__(self, path, flavour):
            spec = FLAVOURS[flavour]
            self.model = BERTSpeakerID.__new__(BERTSpeakerID)
            torch.nn.Module.__init__(self.model)

            self.model.tokenizer = BertTokenizer.from_pretrained(
                spec["base"], do_lower_case=False, do_basic_tokenize=False)
            self.model.tokenizer.add_tokens(spec["added"], special_tokens=True)

            config = BertConfig.from_pretrained(spec["base"])
            self.model.bert = BertModel(config)
            self.model.bert.resize_token_embeddings(len(self.model.tokenizer))
            self.model.num_layers = min(4, config.num_hidden_layers)
            self.model.tanh = torch.nn.Tanh()
            self.model.fc = torch.nn.Linear(2 * config.hidden_size, 100)
            self.model.fc2 = torch.nn.Linear(100, 1)

            got = len(self.model.tokenizer)
            if got != spec["vocab"]:
                sys.exit(f"{flavour}: tokenizer is {got} tokens, checkpoint wants "
                         f"{spec['vocab']} — the base model or the added tokens are wrong")

            state = torch.load(path, map_location="cpu", weights_only=True)
            state.pop("bert.embeddings.position_ids", None)
            self.model.load_state_dict(state)
            self.model.eval()

            # `get_batches(doLowerCase=False)` honours the flag when it builds token ids but
            # calls `get_wp_position_for_all_tokens(xb[j])` without it, so the word-piece
            # *position map* is still built the lowercased way — with a `[CAP]` inserted
            # before every capitalised word. The ids are then shorter than the positions
            # indexing them, and a cased run dies on `IndexError: index 173 is out of bounds
            # for axis 0 with size 137`. Binding the flavour's casing as that method's
            # default makes the one caller agree with the rest; it is a no-op for the uncased
            # flavour, whose value is the package default anyway.
            unbound = type(self.model).get_wp_position_for_all_tokens
            self.model.get_wp_position_for_all_tokens = (
                lambda words, doLowerCase=spec["lower"]: unbound(self.model, words, doLowerCase))

    wanted = [n.strip() for n in args.novels.split(",") if n.strip()] or sorted(
        f[: -len(".questions.jsonl")]
        for f in os.listdir(args.dump_dir) if f.endswith(".questions.jsonl"))

    if args.rethreshold:
        for novel in wanted:
            kept = write_answers(args.dump_dir, novel, args.threshold, args.flavour)
            print(f"  {novel}: {kept} kept at >= {args.threshold}")
        return

    model_file = FLAVOURS[args.flavour]["file"]
    path = os.path.join(args.models, model_file)
    if not os.path.exists(path):
        sys.exit(f"missing {path} — run tools/fetch-attribution-models.sh")
    print(f"loading {model_file} ({os.path.getsize(path) / 1e6:.0f} MB) as {args.flavour}")
    qa = Loader(path, args.flavour)

    total = answered = 0
    for novel in wanted:
        a, b = predict_novel(qa, args.dump_dir, novel, args.corpus, args.threshold, args.flavour)
        total += a
        answered += b
    print(f"\n{answered}/{total} answered over {len(wanted)} novels. Score with:")
    print(f'  gradle run --args="bakeoff --candidate {args.flavour} '
          f'--answers {args.dump_dir}"')


if __name__ == "__main__":
    sys.exit(main())
