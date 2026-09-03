#!/usr/bin/env python3
"""
Write probe waveforms to disk so they can be listened to (QUI-033, QUI-036).

The host probes answer whether a knob *reaches the model*; they cannot answer whether the
result sounds like anything. That question needs ears on the reference device, which means
the audio has to leave this machine — see README.md.

Two rules the file names follow, because a tester picking a track out of a flat list on the
device has no table, no console log and no screen context to read alongside it:

  * every name starts with a two-digit index, so the directory sorts into listening order;
  * every name carries the whole condition — what was varied, to what value, and the
    measurement the probe printed for it — so a file is identifiable on its own.
"""
import os
import wave

import numpy as np


def write_wav(directory, name, samples, rate):
    """Write mono 16-bit PCM. Returns the path, for printing next to the measurement.

    sherpa-onnx hands back float32 in [-1, 1]; nothing here is normalised or gained,
    because a level difference between two takes is a finding, not a defect to hide.
    """
    os.makedirs(directory, exist_ok=True)
    path = os.path.join(directory, name + ".wav")
    pcm = (np.clip(np.asarray(samples, dtype=np.float32), -1.0, 1.0) * 32767.0)
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(int(rate))
        out.writeframes(pcm.astype("<i2").tobytes())
    return path
