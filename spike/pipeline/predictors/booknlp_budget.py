#!/usr/bin/env python3
"""
What the encoder costs at import time (QUI-041).

PRD §5 gives the app 450 MB of footprint and 1.2 GB of working set, and attribution spends
both **at import**, not during playback: `BookImport` runs once per book and has seconds,
where the TTS path has milliseconds. So the numbers that decide are the checkpoint on disk,
the peak resident set while it runs, and the wall clock for one novel's worth of quotations.

    tools/fetch-attribution-models.sh
    cd spike/pipeline && gradle run --args="dump --out build/bakeoff"
    python3 predictors/booknlp_budget.py build/bakeoff --flavour booknlp-plus

**These are host numbers and none of them is an SLA** (CLAUDE.md §1.6). The build machine has
four x86 cores and no Hexagon DSP; the Note Air5 C has two A77s, six A55s and no i8mm. What
transfers is the disk footprint, which is a property of the file, and the *ratio* between the
checkpoints. What does not transfer is the wall clock, which is why it is labelled below.

**Each phase runs in its own process**, which is the only way the peak RSS means anything: a
figure measured after torch, the ONNX exporter and the fp16 converter have all been in memory
describes the build machine's toolchain, not what a reader's device would hold. The first
measurement here was 4,923 MB for exactly that reason. Inference alone is what matters.

fp16 is exported for the footprint and for the device, where the GPU and DSP consume it
natively. onnxruntime on x86 CPU has no fp16 kernels and inserts casts around every op, so
the fp16 wall clock here is slower than fp32 and means nothing about the device. It is
reported anyway, because leaving it out would invite someone to measure it and be surprised.
"""
import argparse
import json
import os
import subprocess
import sys
import time

os.environ.setdefault("HF_HUB_DISABLE_XET", "1")

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)


def peak_rss_mb():
    """VmHWM — the high-water mark, which is the number the budget is about."""
    with open("/proc/self/status") as fh:
        for line in fh:
            if line.startswith("VmHWM:"):
                return int(line.split()[1]) / 1024
    return float("nan")


def phase_export(args):
    """Build the scorer, export it, and cache the batches the timing phase will replay."""
    import numpy as np
    import onnx
    import torch
    from onnxconverter_common import float16

    from booknlp_predict import FLAVOURS, bootstrap_cast, build, loader

    spec = FLAVOURS[args.flavour]
    path = os.path.join(args.models, spec["file"])
    t0 = time.perf_counter()
    qa = loader(path, args.flavour)
    torch_load_s = time.perf_counter() - t0
    model = qa.model

    cast = bootstrap_cast(args.dump_dir, args.novel)
    tokens, quotes, entities, _, asked = build(args.dump_dir, args.novel, cast)
    texts, metas, _, _, _ = qa.get_representation(
        quotes, entities, tokens, doLowerCase=spec["lower"])
    x_batches, m_batches, _, _ = model.get_batches(texts, metas, doLowerCase=spec["lower"])

    class Scorer(torch.nn.Module):
        """The whole scorer, not just the encoder.

        The joint head — mean-pool the quotation, mean-pool each candidate, concatenate, two
        linear layers — is three ops and a rounding error in size. Exporting the encoder
        alone would mean reimplementing the head in Kotlin on the device, with no way to tell
        a port bug from a model difference. One graph, one thing to be wrong.
        """

        def __init__(self, inner):
            super().__init__()
            self.inner = inner

        def forward(self, toks, mask, cands, quote):
            return self.inner.forward({"toks": toks, "mask": mask},
                                      {"cands": cands, "quote": quote})

    fp32 = os.path.join(args.out, f"{args.flavour}.onnx")
    xb, mb = x_batches[0], m_batches[0]
    torch.onnx.export(
        Scorer(model), (xb["toks"], xb["mask"], mb["cands"], mb["quote"]), fp32,
        input_names=["toks", "mask", "cands", "quote"], output_names=["scores"],
        dynamic_axes={n: {0: "batch", 1: "seq"} for n in ("toks", "mask")} |
                     {n: {0: "batch", 2: "seq"} for n in ("cands", "quote")} |
                     {"scores": {0: "batch"}},
        opset_version=17, dynamo=False)

    fp16 = os.path.join(args.out, f"{args.flavour}-fp16.onnx")
    onnx.save(float16.convert_float_to_float16(onnx.load(fp32), keep_io_types=True), fp16)

    batches = os.path.join(args.out, f"{args.flavour}-batches.npz")
    flat = {}
    for i, (x, m) in enumerate(zip(x_batches, m_batches)):
        flat[f"toks{i}"] = x["toks"].numpy().astype(np.int64)
        flat[f"mask{i}"] = x["mask"].numpy().astype(np.int64)
        flat[f"cands{i}"] = m["cands"].numpy().astype(np.float32)
        flat[f"quote{i}"] = m["quote"].numpy().astype(np.float32)
    np.savez(batches, **flat)

    print(json.dumps({
        "params": sum(p.numel() for p in model.parameters()),
        "checkpoint_mb": os.path.getsize(path) / 1e6,
        "fp32_mb": os.path.getsize(fp32) / 1e6,
        "fp16_mb": os.path.getsize(fp16) / 1e6,
        "torch_load_s": torch_load_s,
        "batches": len(x_batches),
        "windows": len(texts),
        "quotations": len(asked),
        "export_peak_rss_mb": peak_rss_mb(),
    }))


def phase_time(args):
    """Load one ONNX graph, replay the cached batches, and report this process's own peak."""
    import numpy as np
    import onnxruntime as ort

    data = np.load(os.path.join(args.out, f"{args.flavour}-batches.npz"))
    n = len({k for k in data.files if k.startswith("toks")})

    opts = ort.SessionOptions()
    opts.intra_op_num_threads = args.threads
    t0 = time.perf_counter()
    sess = ort.InferenceSession(args.graph, opts, providers=["CPUExecutionProvider"])
    load_s = time.perf_counter() - t0

    t0 = time.perf_counter()
    for i in range(n):
        sess.run(None, {k: data[f"{k}{i}"] for k in ("toks", "mask", "cands", "quote")})
    run_s = time.perf_counter() - t0
    print(json.dumps({"load_s": load_s, "run_s": run_s, "peak_rss_mb": peak_rss_mb()}))


def child(args, extra):
    """Run one phase in a clean interpreter and hand back the JSON line it printed."""
    cmd = [sys.executable, os.path.abspath(__file__), args.dump_dir,
           "--flavour", args.flavour, "--novel", args.novel, "--models", args.models,
           "--out", args.out, "--threads", str(args.threads)] + extra
    out = subprocess.run(cmd, capture_output=True, text=True)
    line = next((l for l in reversed(out.stdout.splitlines()) if l.startswith("{")), None)
    if line is None:
        sys.exit(f"phase {extra} produced no result:\n{out.stdout[-2000:]}\n{out.stderr[-4000:]}")
    return json.loads(line)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump_dir")
    ap.add_argument("--flavour", default="booknlp-plus")
    ap.add_argument("--novel", default="TheAgeOfInnocence",
                    help="101,261 words and 1,393 quotations — the PDNC novel nearest the "
                         "100k-word book the ticket asks about")
    ap.add_argument("--models", default=os.path.expanduser("~/.cache/quire/models"))
    ap.add_argument("--out", default=os.path.expanduser("~/.cache/quire/onnx"))
    ap.add_argument("--threads", type=int, default=os.cpu_count())
    ap.add_argument("--phase", choices=("export", "time"))
    ap.add_argument("--graph", default="")
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    if args.phase == "export":
        return phase_export(args)
    if args.phase == "time":
        return phase_time(args)

    e = child(args, ["--phase", "export"])
    print(f"{args.flavour} — {args.novel}: {e['windows']} windows over "
          f"{e['quotations']} quotations, {e['params'] / 1e6:.1f}M parameters\n")
    print("footprint — a property of the file, so this part does transfer")
    print(f"  torch checkpoint (fp32)          {e['checkpoint_mb']:8.1f} MB")
    print(f"  ONNX fp32                        {e['fp32_mb']:8.1f} MB")
    print(f"  ONNX fp16                        {e['fp16_mb']:8.1f} MB   <- PRD §5 budget: 450 MB")

    print(f"\nhost timings on {args.threads} x86 threads — NOT an SLA (CLAUDE.md §1.6)")
    print(f"  torch checkpoint load            {e['torch_load_s']:8.1f} s")
    for label, suffix in (("fp32", ".onnx"), ("fp16", "-fp16.onnx")):
        r = child(args, ["--phase", "time", "--graph",
                         os.path.join(args.out, f"{args.flavour}{suffix}")])
        print(f"  onnxruntime {label}: session load   {r['load_s']:8.1f} s, "
              f"whole novel {r['run_s']:7.1f} s ({r['run_s'] / e['windows'] * 1000:.0f} ms/window)")
        print(f"  onnxruntime {label}: peak RSS       {r['peak_rss_mb']:8.0f} MB   "
              f"<- PRD §5 working set: 1,200 MB")
    print(f"\n  (the export phase alone peaked at {e['export_peak_rss_mb']:.0f} MB. That is torch,")
    print("  the exporter and the fp16 converter on a build machine; no device runs it.)")
    print("\nAttribution runs at import (BookImport), once per book, so the wall clock competes")
    print("with EPUB parsing and indexing rather than with the 800 ms time-to-first-sound.")


if __name__ == "__main__":
    sys.exit(main())
