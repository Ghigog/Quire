#!/usr/bin/env python3
"""
Measure what each of a model's voices actually sounds like (QUI-011).

`libritts_r` ships 904 speakers as bare integers. Nothing in the model says which are men
and which are women, so casting by id alone is a coin toss per character — measured on
device 2026-08-29, where the narrator came out a woman, Sarah a man and Thomas a woman.

Rather than hunt for LibriSpeech's speaker metadata, this synthesises one sentence per
speaker and measures its median fundamental frequency. That works for any model in the zoo,
including ones with no metadata at all, and it measures the voice as this engine actually
renders it rather than as a corpus once described it.

    python3 -m pip install sherpa-onnx numpy
    ./fetch-models.sh
    python3 voiceprofile.py > ../../fixtures/voices/libritts_r-f0.tsv
"""
import argparse
import glob
import os
import sys

import numpy as np
import sherpa_onnx

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")

# Long enough to hold several voiced vowels, short enough that 904 of them finish quickly.
PROBE = "The rain had not let up since morning, and the room was grey with it."

# Below this a voice reads as male, above as female.
#
# 145 Hz is measured, not conventional. Profiling all 904 libritts_r speakers gives a
# clearly bimodal distribution with modes near 115 and 195 Hz and its trough in the
# 140-150 bin, so that is where the split belongs. The textbook 165 Hz would sit on the
# rising female side and throw away ~70 usable voices.
#
# Voices within MARGIN of the line are marked `unclear` and dropped by the caster rather
# than forced: with hundreds to choose from, an unused voice costs nothing and a wrongly
# sexed one is heard immediately.
BOUNDARY_HZ = 145.0
MARGIN_HZ = 10.0


def median_f0(samples, rate, fmin=60.0, fmax=400.0):
    """Median F0 over voiced frames, by autocorrelation.

    Autocorrelation rather than anything cleverer because the signal is clean synthetic
    speech with no noise and no competing speakers — the case where the simple method is
    not meaningfully worse than the sophisticated one.
    """
    frame, hop = int(0.04 * rate), int(0.02 * rate)
    lo, hi = int(rate / fmax), int(rate / fmin)
    out = []
    for start in range(0, max(0, len(samples) - frame), hop):
        window = samples[start:start + frame].astype(np.float64)
        if np.sqrt(np.mean(window ** 2)) < 0.02:
            continue                                    # silence between words
        window -= window.mean()
        corr = np.correlate(window, window, "full")[frame - 1:]
        if corr[0] <= 0 or hi >= len(corr):
            continue
        lag = lo + int(np.argmax(corr[lo:hi]))
        # A weak peak means the frame is unvoiced — a fricative, a stop burst — and its
        # "pitch" would be noise. Dropping those matters more than any other detail here.
        if corr[lag] / corr[0] < 0.3:
            continue
        out.append(rate / lag)
    return float(np.median(out)) if out else 0.0


def classify(f0):
    if f0 <= 0:
        return "unknown"
    if f0 < BOUNDARY_HZ - MARGIN_HZ:
        return "male"
    if f0 > BOUNDARY_HZ + MARGIN_HZ:
        return "female"
    return "unclear"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="vits-piper-en_US-libritts_r-medium")
    parser.add_argument("--limit", type=int, default=0, help="profile only the first N voices")
    parser.add_argument("--threads", type=int, default=4)
    args = parser.parse_args()

    directory = os.path.join(ROOT, args.model)
    onnx = [f for f in glob.glob(directory + "/*.onnx") if not f.endswith(".json")][0]
    tts = sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            vits=sherpa_onnx.OfflineTtsVitsModelConfig(
                model=onnx, tokens=directory + "/tokens.txt",
                data_dir=directory + "/espeak-ng-data"),
            provider="cpu", num_threads=args.threads),
        max_num_sentences=1))

    total = tts.num_speakers if not args.limit else min(args.limit, tts.num_speakers)
    print("# voice profile for %s, %d speakers" % (args.model, total))
    print("# median F0 in Hz over voiced frames of one fixed sentence; see voiceprofile.py")
    print("speaker\tf0_hz\tvoice")
    for sid in range(total):
        audio = tts.generate(PROBE, sid=sid, speed=1.0)
        f0 = median_f0(np.asarray(audio.samples), audio.sample_rate)
        print("%d\t%.1f\t%s" % (sid, f0, classify(f0)))
        sys.stdout.flush()


if __name__ == "__main__":
    main()
