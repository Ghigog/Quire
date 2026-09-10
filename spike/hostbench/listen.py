#!/usr/bin/env python3
"""
Render a listening test: the same prose twice, differing only in who speaks (QUI-039).

QUI-028 left a decision that a table cannot make. Tier 1 alone gives 330 lines of a novel
the right character's voice and 24 the wrong one; Tier 1 plus the dialogue conventions gives
396 right and 57 wrong. ADR-0005 prices a wrong voice as the expensive failure and PRD §2
Phase 2 encodes the same instinct in its confidence gates — but nobody has heard either rate,
and how much worse a wrong voice is than a narrator-read line is not derivable from a
percentage.

    python3 -m pip install sherpa-onnx onnx numpy
    ./fetch-models.sh vits-piper-en_US-libritts_r-medium
    ../pipeline/build/install/quire-pipeline-spike/bin/quire-pipeline-spike listen
    python3 listen.py ../pipeline/build/listen/script.json --wav-dir ../../build/listen

**What this script decides: nothing.** The passages, both attributions and the narrator
fallback are settled in the JVM (`spike/pipeline/.../listen/`), where they have tests. This
reads the result and synthesises it. That division is CLAUDE.md §9's, and it is why a bug
here can make the audio bad but cannot make the comparison unfair.

**Casting is deliberately crude and deliberately shared.** Characters are spread across the
speaker range in sorted order and the narrator is pinned. Voice *design* is QUI-011, QUI-032
and QUI-037's question; if the two renderings cast differently, the listener would be judging
two variables at once, so this holds it fixed and cheap. What matters is only that two
characters do not sound alike and that Daisy is the same Daisy in A and in B.

**Blind by construction.** The files are `A` and `B`; which candidate is which is written to
`key.txt` in the same directory, so the person listening can avoid reading it until they have
a verdict. Nothing else in the output names a candidate.
"""
import argparse
import json
import os
import sys

import numpy as np
import sherpa_onnx

from wavout import write_wav

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")
MODEL = "vits-piper-en_US-libritts_r-medium"

# libritts_r ships 904 speakers. These are spread far enough apart to be plainly different
# people rather than neighbouring readers of the same corpus, which is the mistake the first
# device test made (see spike/slice/VoiceProfile). The narrator sits outside the range the
# cast draws from so it never collides with a character.
NARRATOR = 40
CAST_RANGE = (100, 880)

# Prose runs on without pauses if you simply concatenate; a beat between pieces is what makes
# a turn audible as a turn. Long enough to hear, short enough not to pad the listen.
GAP_SECONDS = 0.25


def engine(threads):
    directory = os.path.join(ROOT, MODEL)
    if not os.path.isdir(directory):
        sys.exit("model missing: %s\n  run ./fetch-models.sh %s" % (directory, MODEL))
    onnx = [f for f in os.listdir(directory) if f.endswith(".onnx")][0]
    return sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            vits=sherpa_onnx.OfflineTtsVitsModelConfig(
                model=os.path.join(directory, onnx),
                tokens=os.path.join(directory, "tokens.txt"),
                data_dir=os.path.join(directory, "espeak-ng-data"),
                length_scale=1.0,
                # Not deterministic, unlike the probes: this one is judged by ear, and pinning
                # the noise terms to zero is what makes two variants *comparable* at the cost
                # of sounding flat. Here the two renderings are compared by their speakers,
                # not sample for sample, so the natural setting is the honest one.
                noise_scale=0.667,
                noise_scale_w=0.8),
            provider="cpu", num_threads=threads),
        max_num_sentences=1))


def cast_voices(cast):
    """Name -> speaker id, spread across the range, stable for a given cast list."""
    low, high = CAST_RANGE
    if not cast:
        return {}
    step = (high - low) // max(len(cast), 1)
    return {name: low + i * step for i, name in enumerate(cast)}


def render(tts, pieces, voices, which, rate_hint=None):
    """One track in one candidate's attribution. `which` is 'a' or 'b'."""
    out = []
    rate = rate_hint
    for piece in pieces:
        speaker = voices.get(piece[which]) if piece["quotation"] else None
        audio = tts.generate(piece["text"], sid=speaker if speaker is not None else NARRATOR, speed=1.0)
        rate = audio.sample_rate
        out.append(np.asarray(audio.samples))
        out.append(np.zeros(int(GAP_SECONDS * rate), dtype=np.float32))
    if not out:
        return np.zeros(0, dtype=np.float32), rate or 22050
    return np.concatenate(out), rate


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("script", help="the JSON written by `quire-pipeline-spike listen`")
    parser.add_argument("--wav-dir", default="../../build/listen")
    parser.add_argument("--threads", type=int, default=4)
    args = parser.parse_args()

    script = json.load(open(args.script, encoding="utf-8"))
    voices = cast_voices(script["cast"])
    tts = engine(args.threads)

    print("Listening test — QUI-039")
    print("novel: %s" % script["novel"])
    print("cast:  %s" % ", ".join("%s=%d" % (n, v) for n, v in sorted(voices.items())))
    print("       narrator=%d\n" % NARRATOR)

    written = []
    for track in script["tracks"]:
        pieces = track["pieces"]
        spoken = sum(1 for p in pieces if p["quotation"])
        differ = sum(1 for p in pieces if p["a"] != p["b"])
        print("%s — %s" % (track["name"], track["why"]))
        print("  %d pieces, %d spoken, %d where the two renderings differ" % (len(pieces), spoken, differ))
        for label, which in (("A", "a"), ("B", "b")):
            samples, rate = render(tts, pieces, voices, which)
            name = "%s-%s" % (track["name"], label)
            path = write_wav(args.wav_dir, name, samples, rate)
            print("  %s  %5.1f s  %s" % (label, len(samples) / rate, path))
            written.append(path)
        print()

    # The key, written last and separately, so a listener can hold off reading it.
    key = os.path.join(args.wav_dir, "key.txt")
    with open(key, "w", encoding="utf-8") as out:
        out.write("QUI-039 listening test — which rendering is which\n\n")
        out.write("Do not read this until you have a verdict.\n\n")
        out.write("A = %s\n" % script["candidateA"])
        out.write("B = %s\n" % script["candidateB"])
        out.write("novel: %s\n" % script["novel"])
    print("key written to %s — do not read it before listening" % key)


if __name__ == "__main__":
    main()
