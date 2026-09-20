#!/usr/bin/env python3
"""
Render a `synthesize` script to one continuous wav file (QUI-018).

Same split CLAUDE.md §9 asks for and QUI-039's `spike/hostbench/listen.py` already uses:
which words go in which voice is decided in the JVM (`SynthesisScript`), where it has
tests; this only turns the result into sound.

    python3 -m pip install sherpa-onnx numpy
    ../hostbench/fetch-models.sh vits-piper-en_US-libritts_r-medium
    ./gradlew installDist
    build/install/quire-pipeline-spike/bin/quire-pipeline-spike synthesize book.epub 3
    python3 render.py build/synthesize/script.json
"""
import argparse
import json
import os
import wave

import numpy as np
import sherpa_onnx

MODEL = "vits-piper-en_US-libritts_r-medium"
MODELS_ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "hostbench", "models")

# Piper libritts_r ships 904 speakers. Voice *design* is QUI-011/032/037's question; this
# crude, deterministic spread only needs to keep two characters from sounding alike, the
# same reasoning spike/hostbench/listen.py uses for the same reason.
NARRATOR = 40
CAST_RANGE = (100, 880)

# Prose runs on without pauses if you simply concatenate; a beat between pieces is what
# makes a turn audible as a turn.
GAP_SECONDS = 0.25


def write_wav(path, samples, rate):
    """Write mono 16-bit PCM. sherpa-onnx hands back float32 in [-1, 1]."""
    parent = os.path.dirname(path)
    if parent:
        os.makedirs(parent, exist_ok=True)
    pcm = (np.clip(np.asarray(samples, dtype=np.float32), -1.0, 1.0) * 32767.0)
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(int(rate))
        out.writeframes(pcm.astype("<i2").tobytes())


def engine(threads):
    directory = os.path.join(MODELS_ROOT, MODEL)
    if not os.path.isdir(directory):
        raise SystemExit(
            "model missing: %s\n  run ../hostbench/fetch-models.sh %s" % (directory, MODEL)
        )
    onnx = [f for f in os.listdir(directory) if f.endswith(".onnx")][0]
    return sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            vits=sherpa_onnx.OfflineTtsVitsModelConfig(
                model=os.path.join(directory, onnx),
                tokens=os.path.join(directory, "tokens.txt"),
                data_dir=os.path.join(directory, "espeak-ng-data"),
            ),
            provider="cpu", num_threads=threads),
        max_num_sentences=1))


def cast_voices(cast):
    """Name -> speaker id, spread across the range, stable for a given cast list."""
    low, high = CAST_RANGE
    if not cast:
        return {}
    step = (high - low) // max(len(cast), 1)
    return {name: low + i * step for i, name in enumerate(cast)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("script", help="the JSON written by `quire-pipeline-spike synthesize`")
    parser.add_argument("--out", default=None, help="defaults to <book>-ch<chapter>.wav")
    parser.add_argument("--threads", type=int, default=4)
    args = parser.parse_args()

    script = json.load(open(args.script, encoding="utf-8"))
    voices = cast_voices(script["cast"])
    tts = engine(args.threads)

    print("Chapter synthesis — QUI-018")
    print("book:  %s, chapter %d" % (script["book"], script["chapter"]))
    print("cast:  %s" % ", ".join("%s=%d" % (n, v) for n, v in sorted(voices.items())))
    print("       narrator=%d\n" % NARRATOR)

    out = []
    rate = 22050
    for piece in script["pieces"]:
        speaker = voices.get(piece["speaker"]) if piece["speaker"] else NARRATOR
        audio = tts.generate(piece["text"], sid=speaker, speed=1.0)
        rate = audio.sample_rate
        out.append(np.asarray(audio.samples))
        out.append(np.zeros(int(GAP_SECONDS * rate), dtype=np.float32))

    samples = np.concatenate(out) if out else np.zeros(0, dtype=np.float32)
    path = args.out or "%s-ch%d.wav" % (script["book"], script["chapter"])
    write_wav(path, samples, rate)
    print("%.1f s -> %s" % (len(samples) / rate, path))


if __name__ == "__main__":
    main()
