#!/usr/bin/env python3
"""
Can Quire *invent* a voice, rather than pick one? (QUI-034)

Two levers, both of which turn out to be editable fields inside the Piper model rather
than anything needing a new runtime:

  timbre   `emb_g.weight`, a [904, 512] speaker table. A voice is 512 floats. Write a row
           that was never trained and the model speaks in it.
  accent   ONNX `metadata_props["voice"]`, the espeak-ng phonemiser variant. The model
           config .json is *not* read by sherpa-onnx — patching it does nothing, which
           cost an afternoon to notice.

    python3 voicelab.py blend    # interpolate two speakers, measure the pitch of the result
    python3 voicelab.py accent   # swap the phonemiser, measure whether it reaches the model
    python3 voicelab.py blend --wav-dir out    # ...and keep the audio to listen to

F0 says an invented voice sits between its parents. It cannot say whether the voice is a
*person* — interpolating two speaker embeddings could as easily land on a smeared average
of two readers as on a third one. `--wav-dir` writes the blend ramp with both parents
either side of it, which is the arrangement that makes that audible: play them in order
and hear whether the middle belongs in the sequence.

A WARNING ABOUT MEASUREMENT, because it invalidated the first run of this file. Piper is
stochastic: `noise_scale` and `noise_w` are 0.333, so two identical calls differ by rms
~0.15 and by ±10k samples. A single-shot A/B on the waveform therefore "proves" a
difference between any two things, including a thing and itself. Every comparison here is
repeated and reported against its own spread. `blend` is exempt: median F0 over a whole
utterance is stable enough to read directly, and the control below shows it.
"""
import argparse, os, shutil, statistics, sys

import numpy as np
import onnx
from onnx import numpy_helper

import bench
from voiceprofile import median_f0
from wavout import write_wav

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")
BASE = os.path.join(ROOT, "vits-piper-en_US-libritts_r-medium")
WORK = os.path.join(ROOT, "voicelab")

# Two real speakers either side of the F0 trough, from fixtures/voices/libritts_r-f0.tsv.
MALE, FEMALE = 659, 192          # measured at 111.9 Hz and 188.5 Hz
TEXT = ("The water in the garden by the tower was rather dark, "
        "and father heard a bird there.")


def _copy(tag):
    """A scratch copy of the model to patch. The base is left alone."""
    dst = os.path.join(WORK, tag)
    if not os.path.isdir(dst):
        os.makedirs(WORK, exist_ok=True)
        shutil.copytree(BASE, dst)
    return dst, os.path.join(dst, "en_US-libritts_r-medium.onnx")


def _say(tts, sid, n=1):
    """Synthesise `n` times; return every duration and the last waveform."""
    durations, samples, rate = [], None, None
    for _ in range(n):
        a = tts.generate(TEXT, sid=sid, speed=1.0)
        samples = np.asarray(a.samples, dtype=np.float32)
        rate = a.sample_rate
        durations.append(len(samples) / rate)
    return durations, samples, rate


def _keep(wav_dir, name, samples, rate):
    """Write one take of the ramp, when `--wav-dir` asked for it."""
    if wav_dir:
        print(f"  {'':28} wrote {write_wav(wav_dir, name, samples, rate)}")


def blend(repeats, wav_dir=None):
    """Write interpolated rows into the speaker table and see what comes out."""
    dst, path = _copy("blend")
    slots = [0, 1, 2, 3, 4]
    ts = [0.0, 0.25, 0.5, 0.75, 1.0]
    if not os.path.exists(os.path.join(dst, ".patched")):
        m = onnx.load(path)
        table = next(i for i in m.graph.initializer if i.name == "emb_g.weight")
        emb = numpy_helper.to_array(table).copy()
        a, b = emb[MALE], emb[FEMALE]
        for slot, t in zip(slots, ts):
            emb[slot] = (1 - t) * a + t * b
        table.CopyFrom(numpy_helper.from_array(emb, name="emb_g.weight"))
        onnx.save(m, path)
        open(os.path.join(dst, ".patched"), "w").close()
        print(f"patched emb_g.weight: slots {slots} = blend(spk{MALE}, spk{FEMALE})\n")

    tts, _ = bench.load(dst, length_scale=1.0)

    # Control first: how much does F0 wander between identical calls? Everything below
    # has to clear this bar to mean anything.
    f0s = [median_f0(_say(tts, MALE)[1], 22050) for _ in range(repeats)]
    print(f"  control — spk{MALE} repeated {repeats}x: "
          f"F0 {statistics.mean(f0s):.1f} Hz, sd {statistics.stdev(f0s):.2f} Hz\n")

    # File names put the parents at either end of the ramp — 00 and 06 around 01..05 —
    # so the directory sorts into the order the comparison has to be heard in, and each
    # name carries its own F0 for a tester holding nothing but a file list.
    print(f"  {'voice':28} {'F0 Hz':>7}")
    for index, (sid, sex, label) in enumerate(
            ((MALE, "male", f"real spk{MALE} (male)"),
             (FEMALE, "female", f"real spk{FEMALE} (female)"))):
        _, s, rate = _say(tts, sid)
        f0 = median_f0(s, rate)
        print(f"  {label:28} {f0:>7.1f}")
        side = "a" if index == 0 else "b"
        _keep(wav_dir, f"blend-{index * 6:02d}-parent-{side}-real-spk{sid}-{sex}-{f0:.0f}hz",
              s, rate)
    print()
    for index, (slot, t) in enumerate(zip(slots, ts), start=1):
        _, s, rate = _say(tts, slot)
        f0 = median_f0(s, rate)
        print(f"  {f'blend t={t:.2f} (invented)':28} {f0:>7.1f}")
        _keep(wav_dir, f"blend-{index:02d}-t{t * 100:03.0f}-invented-{f0:.0f}hz", s, rate)


def accent(repeats):
    """Swap the espeak variant and check the change actually reaches the model.

    Duration is a crude probe: it sees a variant that changes how many phonemes a word
    has (Scots, Caribbean) and is blind to one that only changes vowel *quality* or
    rhoticity (RP). A null result here is 'this probe cannot see it', not 'nothing
    happened' — that distinction needs an ear, on the device.
    """
    voices = ["en-US", "en-GB-x-rp", "en-GB-scotland", "en-GB-x-gbclan",
              "en-GB-x-gbcwmd", "en-029", "en-US-nyc"]
    print(f"  {'espeak voice':16} {'mean s':>7} {'sd':>6}   {repeats} runs, speaker {MALE}")
    base = None
    for voice in voices:
        dst, path = _copy(f"accent-{voice}")
        if not os.path.exists(os.path.join(dst, ".patched")):
            m = onnx.load(path)
            for prop in m.metadata_props:
                if prop.key == "voice":
                    prop.value = voice
            onnx.save(m, path)
            open(os.path.join(dst, ".patched"), "w").close()
        try:
            tts, _ = bench.load(dst, length_scale=1.0)
            durations, _, _ = _say(tts, MALE, repeats)
        except Exception as exc:                      # an absent espeak variant lands here
            print(f"  {voice:16} FAILED: {str(exc).splitlines()[0][:60]}")
            continue
        mean, sd = statistics.mean(durations), statistics.stdev(durations)
        if base is None:
            base, tag = (mean, sd), "—"
        else:
            pooled = ((sd ** 2 + base[1] ** 2) / 2) ** 0.5
            tag = f"{mean - base[0]:+.2f}s vs en-US ({abs(mean - base[0]) / pooled:.1f} sd)"
        print(f"  {voice:16} {mean:>7.2f} {sd:>6.2f}   {tag}")


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("probe", choices=["blend", "accent"])
    p.add_argument("--repeats", type=int, default=10,
                   help="runs per condition; the spread is what makes a difference readable")
    p.add_argument("--wav-dir", help="write the blend ramp and both parents here (blend only; "
                                     "voiceprobe.py --mode accent exports the accent audio)")
    args = p.parse_args()
    if not os.path.isdir(BASE):
        sys.exit(f"{BASE} not found — run ./fetch-models.sh first")
    if args.probe == "blend":
        blend(args.repeats, args.wav_dir)
    else:
        accent(args.repeats)
