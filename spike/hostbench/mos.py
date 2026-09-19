#!/usr/bin/env python3
"""
Score rendered audio for quality, host-side (QUI-044).

`bench.py` answers "is this candidate worth a device cycle" on speed alone. Speed screening
is cheap, so every candidate that turned up got measured on it; quality screening was one
person listening once, and the engine that passed the speed screen (ADR-0002) turned out on
a real listen (QUI-039, 2026-09-11) not to be audiobook grade. This closes that asymmetry a
little: **UTMOSv2** (`sarulab-speech/UTMOSv2`, MIT), a reference-free MOS predictor, scores
audio the same way the speed screen times it, so a candidate can be filtered before anyone's
ears are spent on it.

    python3 -m pip install "git+https://github.com/sarulab-speech/UTMOSv2.git"
    python3 mos.py path/to/wavs/                 # every .wav in a directory
    python3 mos.py path/to/one.wav                # a single file
    python3 mos.py ../../build/listen             # score a QUI-039 listening set

First run downloads pretrained weights to `~/.cache/utmosv2` — a fold checkpoint (~800 MB)
plus the SSL and image-backbone weights the model is built from (`transformers` and `timm`,
both `pretrained=True`). None of this is committed; see CLAUDE.md §6. Point `UTMOSV2_CHACHE`
elsewhere to change where it lands.

**A MOS number filters candidates. It never settles a choice.** UTMOSv2 is trained on VoiceMOS
Challenge corpora — short single-speaker utterances rated by crowdworkers — not on minutes of
narration read by a fictional cast, and QUI-036/QUI-033 both found that a signal-processing
proxy can look clean on paper and miss exactly the thing a person hears. Treat a low score as
disqualifying and a high one as merely "worth a listen" — CLAUDE.md §1's rule that the host
screens and the device (or, here, an ear) decides, never as a substitute for QUI-039's
harness.

**Score narration-length audio, not one sentence.** The bar (3.5 MOS) is long-form listening,
and a metric validated on short VoiceMOS-style clips may not carry to minutes of continuous
speech. This script does not render audio itself — CLAUDE.md §9's rule about keeping logic
testable does not really apply to a thin scoring wrapper, but reuse is the bigger reason: it
scores whatever `bench.py`'s renders or `listen.py`'s passages already produced, so the same
audio measured for RTF is the audio judged for quality, and the length of what was scored is
printed so a two-second clip can never pass silently as a two-minute one.

**Handing it a long file does not make it listen to all of it.** `fusion_stage3`'s config
crops each forward pass to a 3-second SSL window plus two 1.4-second spectrogram frames,
chosen at random — read from `utmosv2/config/fusion_stage3.py`, not benchmarked separately.
A 542-second natural-speech render and a 32-second one cost the same single pass. What
"long-form" buys is not a longer listen per pass; it is more of the file for
`num_repetitions` to land a fresh random crop in, which is why that argument matters more
than file length does and why the default here is 10, not UTMOSv2's own default of 1.
"""
import argparse
import glob
import json
import os
import sys
import wave

BAR = 3.5


def _seconds(path):
    with wave.open(path, "rb") as w:
        return w.getnframes() / w.getframerate()


def score(target, repetitions):
    """Run UTMOSv2 over one file or every .wav in a directory. Returns a list of
    {file_path, predicted_mos, seconds}, sorted by file name.

    UTMOSv2 crops long audio to fixed-length windows at random, so one pass over the same
    file is noisy: two back-to-back single-file runs on the same 32s clip landed 3.12 and
    3.01, and scoring it a third time alongside a second candidate swung it to 2.64 — a
    bigger gap than some of the differences this screen exists to catch. `num_repetitions`
    is the library's own fix, averaging several forward passes over fresh crops rather than
    trusting one; the default here (10) costs proportionally more wall-clock but is what
    makes the mean worth reading as a number rather than a coin flip.
    """
    import utmosv2

    # `predict()` defaults to `device="cuda:0"` independently of `create_model`'s own
    # auto-detect, so a host with no GPU has to be told explicitly.
    device = "cpu"
    model = utmosv2.create_model(pretrained=True, device=device)
    if os.path.isdir(target):
        rows = model.predict(input_dir=target, device=device, num_repetitions=repetitions)
    else:
        rows = [{"file_path": target,
                  "predicted_mos": model.predict(
                      input_path=target, device=device, num_repetitions=repetitions)}]
    for row in rows:
        row["seconds"] = _seconds(row["file_path"])
    rows.sort(key=lambda r: r["file_path"])
    return rows


def main():
    p = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    p.add_argument("target", help="a .wav file, or a directory of .wav files")
    p.add_argument("--repetitions", type=int, default=10,
                    help="forward passes averaged per file (default 10; see module docstring)")
    p.add_argument("--json", action="store_true", help="machine-readable output")
    args = p.parse_args()

    if not os.path.exists(args.target):
        sys.exit(f"not found: {args.target}")
    if os.path.isdir(args.target) and not glob.glob(os.path.join(args.target, "*.wav")):
        sys.exit(f"no .wav files in {args.target}")

    rows = score(args.target, args.repetitions)

    if args.json:
        print(json.dumps(rows, indent=1))
        return

    for row in rows:
        mark = "OK  " if row["predicted_mos"] >= BAR else "FAIL"
        print(f"{mark}  {row['predicted_mos']:.2f}  {row['seconds']:6.1f}s  "
              f"{os.path.basename(row['file_path'])}")
    mean = sum(r["predicted_mos"] for r in rows) / len(rows)
    total_s = sum(r["seconds"] for r in rows)
    print(f"\nmean {mean:.2f} over {len(rows)} file(s), {total_s:.1f}s scored, bar {BAR}")
    print("MOS filters candidates for a listen; it does not replace one (QUI-039).")


if __name__ == "__main__":
    main()
