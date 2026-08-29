#!/usr/bin/env python3
"""
Desktop TTS bench (QUI-017).

Runs the same models the Android probe runs, on this machine, through the same
sherpa-onnx runtime. The absolute RTF here means nothing — this is an x86 container, not a
Snapdragon 750G. The **ratio between models** is what transfers, and it is what most of
our device round-trips have actually been asking.

It also catches, in seconds and without a crash, the class of bug that cost a device round
trip: a model whose files are not where the loader expected them.
"""
import argparse, glob, os, statistics, sys, time, wave

FIXTURE = (
    "The rain had not let up since morning, and the windows of the reading room were grey "
    "with it. Sarah crossed to the window and stood there a while without saying anything "
    "at all. Thomas did not turn from the desk, though he had stopped writing some minutes "
    "before, and the clock in the hall struck four."
)


def find(model_dir):
    """Resolve a model directory the way TtsEngine.load does, and say what it found."""
    def opt(*names):
        for n in names:
            p = os.path.join(model_dir, n)
            if os.path.exists(p):
                return p
        return None

    onnx = sorted(glob.glob(os.path.join(model_dir, "*.onnx")), key=len)
    return {
        "onnx": onnx[0] if onnx else None,
        "tokens": opt("tokens.txt"),
        "espeak": opt("espeak-ng-data"),
        "voices": opt("voices.bin"),
        "lexicon": opt("lexicon-us-en.txt", "lexicon.txt"),
        "dict": opt("dict"),
    }


def kind_of(model_dir):
    name = os.path.basename(model_dir).lower()
    if "kokoro" in name:
        return "kokoro"
    if "kitten" in name:
        return "kitten"
    return "vits"


def build(model_dir, threads):
    import sherpa_onnx
    files = find(model_dir)
    required = ["onnx", "tokens"] + (["voices"] if kind_of(model_dir) != "vits" else [])
    missing = [k for k in required if files[k] is None]
    if missing:
        raise SystemExit(f"{os.path.basename(model_dir)}: missing {missing}")
    kind = kind_of(model_dir)
    if kind == "kokoro":
        inner = dict(kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
            model=files["onnx"], voices=files["voices"], tokens=files["tokens"],
            data_dir=files["espeak"] or "", lexicon=files["lexicon"] or "",
            dict_dir=files["dict"] or "",
        ))
    elif kind == "kitten":
        inner = dict(kitten=sherpa_onnx.OfflineTtsKittenModelConfig(
            model=files["onnx"], voices=files["voices"], tokens=files["tokens"],
            data_dir=files["espeak"] or "",
        ))
    else:
        # Not every VITS model phonemises with espeak: the Coqui-derived ones ship a
        # lexicon instead, and sherpa-onnx *aborts the process* rather than raising when
        # given neither. The Android loader needs the same both-ways handling.
        inner = dict(vits=sherpa_onnx.OfflineTtsVitsModelConfig(
            model=files["onnx"], tokens=files["tokens"],
            data_dir=files["espeak"] or "", lexicon=files["lexicon"] or "",
        ))
    cfg = sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(num_threads=threads, provider="cpu", **inner),
    )
    return sherpa_onnx.OfflineTts(cfg), files


def bench(model_dir, threads, repeats, sid, out_wav=None):
    name = os.path.basename(model_dir)
    t0 = time.perf_counter()
    tts, files = build(model_dir, threads)
    load_ms = (time.perf_counter() - t0) * 1000

    tts.generate("Ready.", sid=sid, speed=1.0)  # warm

    times, audio_ms = [], 0
    audio = None
    for _ in range(repeats):
        t = time.perf_counter()
        result = tts.generate(FIXTURE, sid=sid, speed=1.0)
        times.append((time.perf_counter() - t) * 1000)
        audio_ms = len(result.samples) / result.sample_rate * 1000
        audio = result

    if out_wav and audio is not None:
        write_wav(out_wav, audio.samples, audio.sample_rate)

    synth = statistics.median(times)
    return {
        "name": name,
        "load_ms": round(load_ms),
        "synth_ms": round(synth),
        "audio_ms": round(audio_ms),
        "rtf": synth / audio_ms if audio_ms else 0.0,
        "sample_rate": audio.sample_rate if audio else 0,
        "voices": tts.num_speakers,
        "disk_mb": round(sum(
            os.path.getsize(os.path.join(r, f))
            for r, _, fs in os.walk(model_dir) for f in fs
        ) / 1024 / 1024),
    }


def write_wav(path, samples, rate):
    import struct
    with wave.open(path, "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate)
        w.writeframes(b"".join(
            struct.pack("<h", max(-32768, min(32767, int(s * 32767)))) for s in samples
        ))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("models", nargs="+")
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--repeats", type=int, default=3)
    ap.add_argument("--sid", type=int, default=0)
    ap.add_argument("--wav-dir")
    ap.add_argument("--child", action="store_true", help=argparse.SUPPRESS)
    args = ap.parse_args()

    # Each model runs in its own process: a bad config makes sherpa-onnx abort, and one
    # unusable candidate must not take the whole sweep with it.
    if args.child:
        d = args.models[0]
        wav = os.path.join(args.wav_dir, os.path.basename(d) + ".wav") if args.wav_dir else None
        if wav:
            os.makedirs(args.wav_dir, exist_ok=True)
        import json
        print("RESULT " + json.dumps(bench(d, args.threads, args.repeats, args.sid, wav)))
        return

    import json, subprocess
    rows = []
    for d in args.models:
        cmd = [sys.executable, __file__, d, "--child", "--threads", str(args.threads),
               "--repeats", str(args.repeats), "--sid", str(args.sid)]
        if args.wav_dir:
            cmd += ["--wav-dir", args.wav_dir]
        proc = subprocess.run(cmd, capture_output=True, text=True)
        line = next((l for l in proc.stdout.splitlines() if l.startswith("RESULT ")), None)
        if line:
            rows.append(json.loads(line[len("RESULT "):]))
        else:
            reason = (proc.stderr.strip().splitlines() or ["no output"])[-1]
            print(f"FAILED {os.path.basename(d)}: {reason}", file=sys.stderr)
    if False:
      for d in []:
        wav = os.path.join(args.wav_dir, os.path.basename(d) + ".wav") if args.wav_dir else None
        if wav:
            os.makedirs(args.wav_dir, exist_ok=True)
        try:
            rows.append(bench(d, args.threads, args.repeats, args.sid, wav))
        except Exception as e:
            print(f"FAILED {os.path.basename(d)}: {e}", file=sys.stderr)

    if not rows:
        return
    base = min(r["rtf"] for r in rows)
    print(f"\n{'model':<38}{'load':>7}{'synth':>8}{'audio':>8}{'RTF':>8}{'xfastest':>10}{'kHz':>7}{'voices':>8}{'MB':>5}")
    for r in sorted(rows, key=lambda r: r["rtf"]):
        print(f"{r['name']:<38}{r['load_ms']:>6}ms{r['synth_ms']:>7}ms{r['audio_ms']:>7}ms"
              f"{r['rtf']:>8.3f}{r['rtf']/base:>9.2f}x{r['sample_rate']/1000:>7.1f}"
              f"{r['voices']:>8}{r['disk_mb']:>5}")
    print("\nAbsolute RTF is meaningless off-device. The xfastest column is the number that "
          "transfers.")


if __name__ == "__main__":
    main()
