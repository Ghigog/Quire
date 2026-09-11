#!/usr/bin/env python3
"""
What the encoder costs at import time (QUI-041).

PRD §5 gives the app 450 MB of footprint and 1.2 GB of working set, and attribution spends
both **at import**, not during playback: `BookImport` runs once per book and has seconds,
where the TTS path has milliseconds. So the three numbers that decide are the checkpoint on
disk, the peak resident set while it runs, and the wall clock for one novel's worth of
quotations.

    tools/fetch-attribution-models.sh
    cd spike/pipeline && gradle run --args="dump --out build/bakeoff"
    python3 predictors/booknlp_budget.py build/bakeoff --flavour booknlp-plus

**These are host numbers and none of them is an SLA** (CLAUDE.md §1.6). The build machine has
four x86 cores and no Hexagon DSP; the Note Air5 C has two A77s, six A55s and no i8mm. What
transfers is the disk footprint, which is a property of the file, and the *ratio* between the
checkpoints. What does not transfer is the wall clock, which is why it is labelled below.

fp16 is exported for the footprint and for the device, where the GPU and DSP consume it
natively. onnxruntime on x86 CPU has no fp16 kernels and inserts casts around every op, so
the fp16 wall clock here is slower than fp32 and means nothing about the device. It is
reported anyway, because leaving it out would invite someone to measure it and be surprised.
"""
import argparse
import json
import os
import re
import sys
import time

os.environ.setdefault("HF_HUB_DISABLE_XET", "1")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from booknlp_predict import FLAVOURS, bootstrap_cast, build, loader  # noqa: E402


def peak_rss_mb():
    """VmHWM — the high-water mark, which is the number the budget is about."""
    with open("/proc/self/status") as fh:
        for line in fh:
            if line.startswith("VmHWM:"):
                return int(line.split()[1]) / 1024
    return float("nan")


def export(model, sample_x, sample_m, out):
    """The whole scorer, not just the encoder.

    The joint head — mean-pool the quotation, mean-pool each candidate, concatenate, two
    linear layers — is three ops and a rounding error in size, but exporting the encoder
    alone would mean reimplementing the head on the device in Kotlin and having no way to
    tell a port bug from a model difference. One graph, one thing to be wrong.
    """
    import torch

    class Scorer(torch.nn.Module):
        def __init__(self, inner):
            super().__init__()
            self.inner = inner

        def forward(self, toks, mask, cands, quote):
            return self.inner.forward({"toks": toks, "mask": mask},
                                      {"cands": cands, "quote": quote})

    torch.onnx.export(
        Scorer(model),
        (sample_x["toks"], sample_x["mask"], sample_m["cands"], sample_m["quote"]),
        out,
        input_names=["toks", "mask", "cands", "quote"],
        output_names=["scores"],
        dynamic_axes={n: {0: "batch", 1: "seq"} for n in ("toks", "mask")} |
                     {n: {0: "batch", 2: "seq"} for n in ("cands", "quote")} |
                     {"scores": {0: "batch"}},
        opset_version=17,
        dynamo=False,
    )
    return out


def to_fp16(src, dst):
    import onnx
    from onnxconverter_common import float16

    onnx.save(float16.convert_float_to_float16(onnx.load(src), keep_io_types=True),
              dst, save_as_external_data=False)
    return dst


def time_session(path, batches, threads):
    import numpy as np
    import onnxruntime as ort

    opts = ort.SessionOptions()
    opts.intra_op_num_threads = threads
    t0 = time.perf_counter()
    sess = ort.InferenceSession(path, opts, providers=["CPUExecutionProvider"])
    load_s = time.perf_counter() - t0

    t0 = time.perf_counter()
    for xb, mb in batches:
        sess.run(None, {"toks": xb["toks"].numpy().astype(np.int64),
                        "mask": xb["mask"].numpy().astype(np.int64),
                        "cands": mb["cands"].numpy().astype(np.float32),
                        "quote": mb["quote"].numpy().astype(np.float32)})
    return load_s, time.perf_counter() - t0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump_dir")
    ap.add_argument("--flavour", choices=sorted(FLAVOURS), default="booknlp-plus")
    ap.add_argument("--novel", default="TheAgeOfInnocence",
                    help="101,261 words and 1,393 quotations — the PDNC novel nearest the "
                         "100k-word book the ticket asks about")
    ap.add_argument("--models", default=os.path.expanduser("~/.cache/quire/models"))
    ap.add_argument("--out", default=os.path.expanduser("~/.cache/quire/onnx"))
    ap.add_argument("--threads", type=int, default=os.cpu_count())
    args = ap.parse_args()

    import gc

    os.makedirs(args.out, exist_ok=True)
    spec = FLAVOURS[args.flavour]
    path = os.path.join(args.models, spec["file"])
    checkpoint_mb = os.path.getsize(path) / 1e6
    print(f'{args.flavour} — {spec["file"]}')
    print(f"  checkpoint on disk (torch fp32)   {checkpoint_mb:8.1f} MB")

    t0 = time.perf_counter()
    qa = loader(path, args.flavour)
    torch_load_s = time.perf_counter() - t0
    model = qa.model
    params = sum(p.numel() for p in model.parameters())

    cast = bootstrap_cast(args.dump_dir, args.novel)
    tokens, quotes, entities, _, asked = build(args.dump_dir, args.novel, cast)
    texts, metas, _, _, _ = qa.get_representation(
        quotes, entities, tokens, doLowerCase=spec["lower"])
    x_batches, m_batches, _, _ = model.get_batches(texts, metas, doLowerCase=spec["lower"])
    windows = len(texts)
    print(f"  parameters                       {params/1e6:8.1f} M")
    print(f"  {args.novel}: {windows} windows over {len(asked)} quotations")

    fp32 = export(model, x_batches[0], m_batches[0], os.path.join(args.out, f"{args.flavour}.onnx"))
    fp16 = to_fp16(fp32, os.path.join(args.out, f"{args.flavour}-fp16.onnx"))
    print(f"  ONNX fp32                        {os.path.getsize(fp32)/1e6:8.1f} MB")
    print(f"  ONNX fp16                        {os.path.getsize(fp16)/1e6:8.1f} MB")

    batches = list(zip(x_batches, m_batches))
    del model, qa
    gc.collect()

    print(f"\nhost timings on {args.threads} x86 threads — NOT an SLA (CLAUDE.md §1.6)")
    print(f"  torch checkpoint load            {torch_load_s:8.1f} s")
    for label, p in (("fp32", fp32), ("fp16", fp16)):
        load_s, run_s = time_session(p, batches, args.threads)
        print(f"  onnxruntime {label} session load    {load_s:8.1f} s")
        print(f"  onnxruntime {label} whole novel     {run_s:8.1f} s "
              f"({run_s/windows*1000:.0f} ms/window)")
    print(f"  peak RSS for all of the above    {peak_rss_mb():8.0f} MB")
    print("\nPRD §5: 450 MB app footprint, 1.2 GB working set. Attribution runs at import")
    print("(BookImport), once per book, so the wall clock competes with EPUB parsing and")
    print("indexing rather than with the 800 ms time-to-first-sound.")


if __name__ == "__main__":
    sys.exit(main())
