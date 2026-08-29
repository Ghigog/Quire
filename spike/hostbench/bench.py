#!/usr/bin/env python3
"""
Host-side relative screen for TTS candidates (QUI-017).

This does **not** measure the SLA. RTF on a desktop x86 core says nothing about RTF on a
Snapdragon 750G, and the numbers here must never be quoted as if it did. What it measures
is the *ratio* between two candidates, which is what decides whether a model is worth
spending a device cycle on.

Ratios transfer only within a tier and sample rate — see README.md, where a measured
counter-example is recorded.

    ./fetch-models.sh && python3 bench.py            # matrix over every fetched model
    python3 bench.py --paired vctk libritts_r        # interleaved A/B, for close calls
"""
import argparse, glob, json, os, statistics, time

import sherpa_onnx

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")

# Narration and dialogue, close to the excerpt used on device so the shapes are comparable.
TEXT = (
    "The lamps along the terrace had not yet been lit, and the sea below was the colour "
    "of slate. “You were not at dinner,” she said, without turning round. "
    "“I was walking,” he answered. “It is a long way to the point and back, "
    "and I lost the light.” She considered this for a moment, and said nothing at all."
)


def load(model_dir, length_scale):
    onnx = [f for f in glob.glob(model_dir + "/*.onnx") if not f.endswith(".json")][0]
    cfg = sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            vits=sherpa_onnx.OfflineTtsVitsModelConfig(
                model=onnx,
                tokens=model_dir + "/tokens.txt",
                data_dir=model_dir + "/espeak-ng-data",
                length_scale=length_scale,
            ),
            provider="cpu",
            num_threads=int(os.environ.get("THREADS", "2")),
        ),
        max_num_sentences=1,
    )
    return sherpa_onnx.OfflineTts(cfg), onnx


def run(tts, repeats):
    """Median RTF over `repeats`, after a discarded warm-up."""
    sid = tts.num_speakers // 2 if tts.num_speakers > 1 else 0
    tts.generate(TEXT, sid=sid, speed=1.0)
    rtfs = []
    for _ in range(repeats):
        t0 = time.perf_counter()
        audio = tts.generate(TEXT, sid=sid, speed=1.0)
        rtfs.append((time.perf_counter() - t0) / (len(audio.samples) / audio.sample_rate))
    return rtfs, audio.sample_rate


def resolve(fragment):
    hits = [d for d in sorted(glob.glob(ROOT + "/vits-piper-*")) if fragment in d]
    if len(hits) != 1:
        raise SystemExit(f"{fragment!r} matched {len(hits)} models; be more specific")
    return hits[0]


def matrix(repeats):
    out = []
    for d in sorted(glob.glob(ROOT + "/vits-piper-*")):
        t0 = time.perf_counter()
        tts, onnx = load(d, 1.0)
        load_ms = round((time.perf_counter() - t0) * 1000)
        rtfs, rate = run(tts, repeats)
        out.append(dict(
            name=os.path.basename(d), rate=rate, speakers=tts.num_speakers,
            onnx_mb=round(os.path.getsize(onnx) / 1e6, 1),
            load_ms=load_ms, rtf=round(statistics.median(rtfs), 4),
        ))
    print(json.dumps(out, indent=1))


def paired(a, b, repeats):
    """Interleaved A/B. Close calls need this: run-to-run spread on a shared VM is ~8%,
    which is wider than the difference we are usually trying to resolve."""
    # length_scale forced equal, so neither candidate is flattered by speaking slower.
    cases = {os.path.basename(resolve(x)): load(resolve(x), 1.0)[0] for x in (a, b)}
    for tts in cases.values():
        tts.generate(TEXT, sid=tts.num_speakers // 2, speed=1.0)

    res = {k: [] for k in cases}
    for _ in range(repeats):
        for name, tts in cases.items():
            sid = tts.num_speakers // 2 if tts.num_speakers > 1 else 0
            t0 = time.perf_counter()
            audio = tts.generate(TEXT, sid=sid, speed=1.0)
            res[name].append(
                (time.perf_counter() - t0) / (len(audio.samples) / audio.sample_rate))

    for name, v in res.items():
        print(f"{name:38s} median {statistics.median(v):.4f}  "
              f"min {min(v):.4f}  max {max(v):.4f}")
    m = [statistics.median(v) for v in res.values()]
    print(f"\nratio {' / '.join(res)} = {m[0] / m[1]:.3f}")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--paired", nargs=2, metavar=("A", "B"))
    p.add_argument("--repeats", type=int, default=7)
    args = p.parse_args()
    if args.paired:
        paired(*args.paired, repeats=args.repeats)
    else:
        matrix(args.repeats)
