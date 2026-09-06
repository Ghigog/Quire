#!/usr/bin/env python3
"""
The encoder candidate for QUI-028: SpanBERT literary speaker attribution.

Reads what `quire-pipeline bakeoff dump` wrote, predicts a speaker per quotation, and
writes `<novel>.answers.tsv` back beside it. The scoring stays in Kotlin for every
candidate alike — see `ExternalCandidate` for why.

    python3 -m pip install grimbert
    tools/fetch-attribution-models.sh
    cd spike/pipeline && gradle run --args="dump --out build/bakeoff --novels AHandfulOfDust"
    python3 predictors/grimbert_predict.py build/bakeoff --novels AHandfulOfDust

**What this model actually is, and it is not what the ticket assumed.** It does not name a
speaker. It scores (quotation, candidate mention) pairs and returns the character whose
mentions score highest, declining below 0.5. So it *ranks a cast it is given* — it presumes
the cast and the mentions already exist. In Quire that is Roster's job (QUI-007), which
means the encoder composes with our cast discovery rather than replacing it, and its
accuracy is bounded by ours: a character Roster never found cannot be predicted here.

**Mentions come from PDNC's alias lists, matched in the text.** PDNC ships `Main Name` and
`Aliases` per character but no mention offsets, so they are found by string matching, which
is also how they would be found on device. The alternative — hand-annotated gold mentions —
would flatter the model with information the product will not have.
"""
import argparse
import ast
import csv
import json
import os
import re
import sys

# Renard's settings for this checkpoint (renard/pipeline/speaker_attribution.py:24-25).
QUOTE_CTX_LEN = 512
SPEAKER_REPR_NB = 4
MODEL_ID = "compnet-renard/spanbert-base-cased-literary-speaker-attribution"

TOKEN = re.compile(r"\w+|[^\w\s]")

# Hugging Face's Xet transfer protocol resolves to cas-server.xethub.hf.co and
# transfer.xethub.hf.co, neither of which a session container can reach; the classic CDN
# (us.aws.cdn.hf.co) it can. Without this the download does not fail, it hangs — which is
# a slow thing to diagnose, so it is set here rather than left to the caller's shell.
os.environ.setdefault("HF_HUB_DISABLE_XET", "1")


def tokenise(text):
    """Tokens and their character spans, so quotation offsets can be mapped to indices."""
    toks, spans = [], []
    for m in TOKEN.finditer(text):
        toks.append(m.group())
        spans.append((m.start(), m.end()))
    return toks, spans


def read_jsonl(path):
    with open(path, encoding="utf-8") as fh:
        return [json.loads(line) for line in fh if line.strip()]


def aliases(novel_dir):
    """{character id: (main name, [alias, ...])} from PDNC's own character list."""
    out = {}
    with open(os.path.join(novel_dir, "character_info.csv"), encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            names = {row["Main Name"]}
            try:
                names |= set(ast.literal_eval(row["Aliases"]))
            except (ValueError, SyntaxError):
                pass                                    # a malformed cell costs one alias
            out[row["Character ID"]] = (row["Main Name"], sorted(names, key=len, reverse=True))
    return out


def find_mentions(tokens, cast):
    """Every place a character's alias appears, as a token span.

    Longest alias first so "Mrs Beaver" is not shadowed by "Beaver"; a token already claimed
    by a longer alias of the same character is not claimed again.
    """
    lowered = [t.lower() for t in tokens]
    mentions, taken = [], set()
    for cid, (_, names) in cast.items():
        for name in names:
            parts = [p.lower() for p in TOKEN.findall(name)]
            if not parts:
                continue
            for i in range(len(lowered) - len(parts) + 1):
                if lowered[i:i + len(parts)] == parts and not (taken & set(range(i, i + len(parts)))):
                    mentions.append((cid, i, i + len(parts)))
                    taken |= set(range(i, i + len(parts)))
    return mentions


def span_to_tokens(spans, start, end):
    """The token indices overlapping a character span. Empty if the span hits nothing."""
    lo = next((i for i, (s, e) in enumerate(spans) if e > start), None)
    hi = next((i for i in range(len(spans) - 1, -1, -1) if spans[i][0] < end), None)
    return (lo, hi + 1) if lo is not None and hi is not None and hi >= lo else None


def predict_novel(dump_dir, novel, corpus, model, tokenizer, batch_size, device):
    from grimbert.datas import (
        SpeakerAttributionDataset,
        SpeakerAttributionDocument,
        SpeakerAttributionQuote,
        SpeakerAttributionMention,
    )
    from grimbert.predict import predict_speaker

    paragraphs = read_jsonl(os.path.join(dump_dir, f"{novel}.paragraphs.jsonl"))
    questions = read_jsonl(os.path.join(dump_dir, f"{novel}.questions.jsonl"))
    if not questions:
        return 0

    # One document, rebuilt from the dump so the offsets stay the harness's, not ours.
    offset, texts, base = 0, [], {}
    for p in paragraphs:
        base[p["n"]] = offset
        texts.append(p["text"])
        offset += len(p["text"]) + 2                    # the "\n\n" joined below
    document = "\n\n".join(texts)
    tokens, spans = tokenise(document)

    cast = aliases(os.path.join(corpus, "data", novel))
    mentions = find_mentions(tokens, cast)
    if not mentions:
        print(f"  {novel}: no mentions matched — every quotation will be declined")
        return 0

    quotes, asked = [], []
    for q in questions:
        at = span_to_tokens(spans, base[q["paragraph"]] + q["start"], base[q["paragraph"]] + q["end"])
        if at is None:
            continue                                    # scored as unanswered, which it is
        quotes.append(SpeakerAttributionQuote(tokens[at[0]:at[1]], at[0], at[1], None))
        asked.append(q["id"])

    doc = SpeakerAttributionDocument(
        tokens,
        quotes,
        [SpeakerAttributionMention(tokens[s:e], s, e, cid) for cid, s, e in mentions],
    )
    dataset = SpeakerAttributionDataset([doc], QUOTE_CTX_LEN, SPEAKER_REPR_NB, tokenizer)
    predictions = predict_speaker(dataset, model, tokenizer, batch_size, device=device, quiet=True)[0]

    out = os.path.join(dump_dir, f"{novel}.answers.tsv")
    with open(out, "w", encoding="utf-8") as fh:
        fh.write("# grimbert %s, threshold 0.5\n" % MODEL_ID)
        for qid, pred in zip(asked, predictions):
            # Below the threshold the model is declining, and declining is a real answer:
            # a quotation nobody claims is read by the narrator, which is the safe failure.
            name = cast[pred.predicted_speaker][0] if pred.score > 0.5 else ""
            fh.write("%s\t%s\tgrimbert\n" % (qid, name))
    answered = sum(1 for p in predictions if p.score > 0.5)
    print(f"  {novel}: {len(quotes)} quotations, {answered} answered -> {out}")
    return len(quotes)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump_dir", help="what `bakeoff dump --out` wrote")
    ap.add_argument("--corpus", default=os.path.expanduser("~/.cache/quire/pdnc"))
    ap.add_argument("--novels", default="", help="comma-separated folders; default all in the dump")
    ap.add_argument("--batch-size", type=int, default=16)
    ap.add_argument("--device", default="auto", choices=["auto", "cpu", "cuda"])
    args = ap.parse_args()

    from grimbert.model import SpeakerAttributionModel
    from transformers import AutoTokenizer

    wanted = [n.strip() for n in args.novels.split(",") if n.strip()] or sorted(
        f[: -len(".questions.jsonl")]
        for f in os.listdir(args.dump_dir) if f.endswith(".questions.jsonl")
    )
    print(f"loading {MODEL_ID}")
    model = SpeakerAttributionModel.from_pretrained(MODEL_ID)
    tokenizer = AutoTokenizer.from_pretrained("bert-base-cased")

    total = 0
    for novel in wanted:
        total += predict_novel(args.dump_dir, novel, args.corpus, model, tokenizer,
                               args.batch_size, args.device)
    print(f"\n{total} quotations over {len(wanted)} novels. Score them with:")
    print(f"  gradle run --args=\"bakeoff --candidate grimbert --answers {args.dump_dir}\"")


if __name__ == "__main__":
    sys.exit(main())
