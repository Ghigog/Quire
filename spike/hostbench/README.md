# Host-side relative screen for TTS candidates

Part of QUI-017. Answers one question cheaply: **is this candidate worth a device cycle?**

Every number the bake-off actually cares about — RTF against PRD §5, TTFS, sustained power —
has to come off the reference device. This runs the same `sherpa-onnx` runtime on whatever
x86 box is to hand, so it can compare two candidates against each other without waiting for
a build, an install and a listen.

```sh
./fetch-models.sh                                   # ~330 MB, not committed
python3 -m pip install sherpa-onnx
python3 bench.py                                    # matrix over every fetched model
python3 bench.py --paired vctk libritts_r           # interleaved A/B for close calls
```

## What it can and cannot tell you

**It cannot give you an SLA number.** A desktop core with wide vectors is not a Cortex-A77.
Absolute RTF here is meaningless for the device and must never be quoted as if it were.

**Ratios within a tier and sample rate transfer well.** Two medium 22.05 kHz VITS models of
similar size differ on the device roughly as they differ here.

**Ratios across tiers demonstrably do not.** Measured, and worth keeping in view:

| Comparison | Host (x86, 2 threads) | Device (750G) |
| --- | --- | --- |
| vctk-medium ÷ libritts_r-medium | **0.995** | not yet run |
| alan-low ÷ alan-medium | **0.76** | — |
| alan-low ÷ libritts_r-medium | **0.72** | **0.37** |

The last row is the warning. The host says a low-tier model buys about 1.4×; the device
measured 2.7× for the same pair. Something on the 750G punishes medium-tier 22.05 kHz
synthesis roughly twice as hard as this machine does — plausibly memory bandwidth around
the decoder, since `alan-low` and `alan-medium` are the same size on disk (63 MB) and differ
mainly in output rate. Untested either way, and it is why cross-tier screening here is a
hint and not a result.

## Method notes

- Median of N interleaved runs after a discarded warm-up. Run-to-run spread on a shared VM
  is around 8%, which is wider than most differences worth arguing about — hence `--paired`,
  which alternates candidates so noise hits both equally.
- `length_scale` is forced to 1.0. VCTK ships 1.4, and a model that speaks more slowly
  produces more audio per unit of compute; leaving it alone compares voices, not engines.
- Speaker id is the midpoint of the model's range, not 0. Adjacent ids in `libritts_r` are
  neighbouring readers from one corpus and sound alike — the mistake the first device test
  made.
