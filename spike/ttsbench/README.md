# Desktop TTS bench (QUI-017)

Runs the same models the Android probe runs, through the same sherpa-onnx runtime, on
whatever machine you are sitting at.

```bash
pip install sherpa-onnx
curl -L -o m.tar.bz2 https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/<model>.tar.bz2
mkdir -p tts && tar -xjf m.tar.bz2 -C tts
python3 bench.py tts/* --threads 2 --wav-dir /tmp/wavs
```

## What it is and is not for

**Absolute RTF here is meaningless.** This is not a Snapdragon 750G, and a desktop will
flatter every model. The `xfastest` column — each model relative to the fastest — is the
number that transfers, along with sample rate, voice count and disk size.

It exists because device round-trips are slow and most of the questions we were sending to
the device did not need one. Use it to **eliminate candidates before anyone touches the
Boox**, then take the survivor's absolute numbers on hardware.

It also catches, in seconds, the class of failure that costs an APK cycle: a model whose
files are not where the loader expects. sherpa-onnx does not raise on a bad config — it
**aborts the process** — so each model is benched in its own subprocess and a fatal
candidate is reported rather than taking the sweep with it.

## Measured 2026-08-28, x86_64, 2 threads

| Model | RTF | ×fastest | kHz | Voices | MB |
| --- | --- | --- | --- | --- | --- |
| Piper `alan` low | 0.042 | 1.00× | 16.0 | 1 | 77 |
| Piper `vctk` medium | 0.059 | 1.40× | 22.1 | 109 | 91 |
| Piper `libritts_r` medium | 0.066 | 1.57× | 22.1 | 904 | 92 |
| Kitten nano fp16 | 0.229 | 5.47× | 24.0 | 8 | 40 |
| VITS VCTK (Coqui) | 0.344 | 8.21× | 22.1 | 109 | 189 |
| Kokoro int8 multi-lang | **1.040** | 24.83× | 24.0 | 103 | 205 |

`bench.py` also takes `--wav-dir` to write the audio out, which is how you compare quality
without a device.
