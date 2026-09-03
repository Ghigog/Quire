#!/usr/bin/env python3
"""
Probe the two voice axes that live outside the speaker table (QUI-033).

Casting treats a voice as a speaker id, but `libritts_r`'s 904 speakers are all readers of
an American corpus. Two things a character description asks for — how fast they speak and
what accent they have — are not in that table at all:

  * **rate** is `length_scale`, a runtime argument;
  * **accent** is the espeak-ng variant used to phonemise the text, and Piper bakes that
    into the ONNX metadata as `voice`, alongside `n_speakers` and `sample_rate`.

sherpa-onnx reads the metadata, not the sidecar `.json`, so editing the JSON changes
nothing. This patches the metadata field in a scratch copy of the model and synthesises
through it.

**Read the control before believing any result.** Piper samples durations from `noise_w`
on every call, so two identical runs differ by more than most accents do — measured here at
0.30 s of spread against a 0.66 s Caribbean shift. Comparing one run to one run proves
nothing at all.

So the probe pins `noise_scale` and `noise_scale_w` to zero, which makes synthesis
deterministic, and then compares waveforms sample for sample. That turns a weak question
("is this difference bigger than the noise?") into an exact one: **identical output means
the variant did nothing; any difference at all means the phoneme stream changed.** The
stochastic control still runs first, because its spread is the reason determinism is
needed.

    python3 -m pip install sherpa-onnx onnx numpy
    ./fetch-models.sh
    python3 voiceprobe.py --mode accent      # espeak variants, against the noise floor
    python3 voiceprobe.py --mode rate        # length_scale, against the same floor
    python3 voiceprobe.py --list             # what variants the model actually ships
    python3 voiceprobe.py --mode accent --wav-dir out    # ...and keep the audio

This is a host-side screen and the caveats in README.md apply: it says whether a knob
*reaches the model*, never how the result sounds. Only a listen on the device does that,
which is what `--wav-dir` is for: it writes the waveform each row was measured from, so
the file a tester plays is the same audio the number describes. Note what that audio is —
the deterministic render, noise pinned off. That is what makes two variants comparable at
all, and it is not how the app will render; judge pronunciation from it, not naturalness.
"""
import argparse
import glob
import os
import shutil
import tempfile

import numpy as np
import onnx
import sherpa_onnx

from voiceprofile import median_f0
from wavout import write_wav

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")

# Deliberately full of the vowels and rhotic /r/ that English accents disagree about.
PROBE = "I heard the water start, and my father asked whether the bath was warm."

# The variants espeak-ng ships for English, by the identifier it resolves. The middle
# field is the file-name label: a tester picking a track out of a flat list on the device
# has the name and nothing else, and `en-gb-x-gbcwmd` alone tells them nothing.
ENGLISH_VARIANTS = [
    ("en-us", "general-american", "General American — what the model was trained on"),
    ("en-us-nyc", "new-york-city", "New York City"),
    ("en-gb", "england-default", "England, RP-adjacent default"),
    ("en-gb-x-rp", "received-pronunciation", "Received Pronunciation"),
    ("en-gb-scotland", "scots", "Scots"),
    ("en-gb-x-gbclan", "lancashire", "Lancashire"),
    ("en-gb-x-gbcwmd", "west-midlands", "West Midlands"),
    ("en-029", "caribbean", "Caribbean"),
]


def model_dir(name):
    return os.path.join(ROOT, name)


def onnx_path(directory):
    return [f for f in glob.glob(directory + "/*.onnx") if not f.endswith(".json")][0]


def read_metadata(directory):
    model = onnx.load(onnx_path(directory), load_external_data=False)
    return {p.key: p.value for p in model.metadata_props}


def variants_shipped(directory):
    """Variant identifiers espeak-ng-data actually holds, by filename under lang/."""
    lang = os.path.join(directory, "espeak-ng-data", "lang")
    found = []
    for family in sorted(os.listdir(lang)):
        path = os.path.join(lang, family)
        if os.path.isdir(path):
            found += [f for f in sorted(os.listdir(path)) if f.startswith("en")]
    return found


def patched_model(directory, variant, scratch):
    """A copy of the model whose espeak `voice` metadata is `variant`.

    Copied rather than edited in place: the fetched models are shared with bench.py and
    voiceprofile.py, and a probe that silently repointed the incumbent's phonemiser would
    be a very hard bug to find later.
    """
    dest = os.path.join(scratch, variant)
    os.makedirs(dest, exist_ok=True)
    for name in ("tokens.txt",):
        shutil.copy(os.path.join(directory, name), dest)
    espeak = os.path.join(dest, "espeak-ng-data")
    if not os.path.exists(espeak):
        os.symlink(os.path.join(directory, "espeak-ng-data"), espeak)

    model = onnx.load(onnx_path(directory))
    for prop in model.metadata_props:
        if prop.key == "voice":
            prop.value = variant
    out = os.path.join(dest, os.path.basename(onnx_path(directory)))
    onnx.save(model, out)
    return dest, out


def engine(model_file, directory, threads, deterministic=True):
    """`deterministic` zeroes both noise terms, so repeated calls are sample-identical."""
    return sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            vits=sherpa_onnx.OfflineTtsVitsModelConfig(
                model=model_file,
                tokens=os.path.join(directory, "tokens.txt"),
                data_dir=os.path.join(directory, "espeak-ng-data"),
                length_scale=1.0,
                noise_scale=0.0 if deterministic else 0.667,
                noise_scale_w=0.0 if deterministic else 0.8),
            provider="cpu", num_threads=threads),
        max_num_sentences=1))


def measure(tts, speaker, repeats, speed=1.0):
    """Duration, median F0, the last waveform and its rate, over `repeats` calls."""
    seconds, pitches, samples, rate = [], [], None, 0
    for _ in range(repeats):
        audio = tts.generate(PROBE, sid=speaker, speed=speed)
        samples = np.asarray(audio.samples)
        rate = audio.sample_rate
        seconds.append(len(samples) / audio.sample_rate)
        pitches.append(median_f0(samples, audio.sample_rate))
    return np.array(seconds), np.array(pitches), samples, rate


def compare(reference, samples):
    """How far this waveform is from the reference one, as a verdict."""
    if reference is None:
        return ""
    if len(reference) != len(samples):
        return "  different phonemes"
    if np.array_equal(reference, samples):
        return "  IDENTICAL — the variant did nothing"
    return "  same length, rms %.4f" % float(np.sqrt(np.mean((reference - samples) ** 2)))


def export(directory, name, samples, rate):
    """Write the waveform a row was measured from, when `--wav-dir` asked for it."""
    if not directory:
        return
    print(" " * 16 + "  wrote " + write_wav(directory, name, samples, rate))


def summarise(label, seconds, pitches, verdict=""):
    print("%-16s %6.2f s  (spread %.2f)   F0 %6.1f Hz%s"
          % (label, np.median(seconds), seconds.max() - seconds.min(),
             np.median(pitches), verdict))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="vits-piper-en_US-libritts_r-medium")
    parser.add_argument("--mode", choices=["accent", "rate"], default="accent")
    parser.add_argument("--speaker", type=int, default=447, help="mid-range libritts_r id")
    parser.add_argument("--repeats", type=int, default=5)
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--list", action="store_true", help="print metadata and variants, then exit")
    parser.add_argument("--wav-dir", help="also write the measured waveform of each row here")
    args = parser.parse_args()

    directory = model_dir(args.model)
    if args.list:
        for key, value in read_metadata(directory).items():
            if len(value) < 200:
                print("%-12s %s" % (key, value))
        print("variants     " + " ".join(variants_shipped(directory)))
        return

    print("# %s, speaker %d, %d repeats of one sentence" % (args.model, args.speaker, args.repeats))
    print("# baseline is the model's own espeak voice: %s" % read_metadata(directory)["voice"])

    print("\n## control — identical calls, sampling left on")
    stochastic = engine(onnx_path(directory), directory, args.threads, deterministic=False)
    seconds, pitches, _, _ = measure(stochastic, args.speaker, args.repeats)
    print("# noise floor %.2f s of spread over %d identical calls — wider than most"
          % (seconds.max() - seconds.min(), args.repeats))
    summarise("sampled", seconds, pitches)

    baseline = engine(onnx_path(directory), directory, args.threads)
    seconds, pitches, reference, _ = measure(baseline, args.speaker, args.repeats)
    print("\n## control — identical calls, sampling pinned off")
    summarise("deterministic", seconds, pitches, compare(reference, reference))
    print("# everything below is compared against this waveform, sample for sample")

    if args.mode == "rate":
        print("\n## rate — length_scale, the runtime argument")
        for index, speed in enumerate((0.8, 0.9, 1.0, 1.1, 1.25), start=1):
            seconds, pitches, samples, rate = measure(baseline, args.speaker, args.repeats, speed=speed)
            summarise("speed %.2f" % speed, seconds, pitches, compare(reference, samples))
            export(args.wav_dir, "rate-%02d-speed%03d-spk%d" % (index, speed * 100, args.speaker),
                   samples, rate)
        return

    shipped = variants_shipped(directory)
    print("\n## accent — espeak variant, patched into the ONNX metadata")
    with tempfile.TemporaryDirectory() as scratch:
        for index, (variant, label, note) in enumerate(ENGLISH_VARIANTS, start=1):
            if variant not in [v.lower() for v in shipped] and variant != "en-gb":
                print("%-16s not shipped by this model's espeak-ng-data" % variant)
                continue
            copy_dir, model_file = patched_model(directory, variant, scratch)
            try:
                tts = engine(model_file, copy_dir, args.threads)
                seconds, pitches, samples, rate = measure(tts, args.speaker, args.repeats)
            except Exception as exc:                     # noqa: BLE001 — report, don't stop
                print("%-16s failed: %s" % (variant, str(exc).splitlines()[0]))
                continue
            summarise(variant, seconds, pitches, compare(reference, samples))
            print(" " * 16 + "  " + note)
            # The index is the variant's position in the table, not the run's, so a variant
            # the model does not ship leaves a gap rather than renumbering the rest.
            export(args.wav_dir, "accent-%02d-%s-%s-spk%d" % (index, variant, label, args.speaker),
                   samples, rate)


if __name__ == "__main__":
    main()
