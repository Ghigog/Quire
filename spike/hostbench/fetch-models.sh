#!/usr/bin/env bash
# Fetch Piper/VITS models for the host-side screen (QUI-017).
#
# Not committed: ~1.1 GB of weights. CLAUDE.md §6 — model files are fetched by a
# documented script, never stored in git.
set -euo pipefail

DEST="$(cd "$(dirname "$0")" && pwd)/models"
BASE="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
MODELS=(
  vits-piper-en_GB-alan-low        # single voice, 16 kHz — the speed floor
  vits-piper-en_GB-alan-medium     # same voice at 22.05 kHz — isolates the tier
  vits-piper-en_GB-vctk-medium     # 109 voices
  vits-piper-en_US-libritts_r-medium  # 904 voices, the incumbent
  vits-vctk                        # 109 voices, the original non-Piper VITS
  kokoro-int8-multi-lang-v1_1      # the "realistic Kokoro" of ADR-0002
  kokoro-multi-lang-v1_1           # same model unquantized — isolates int8 (365 MB)
)

# Named models override the list, so a caller that needs one of them does not pay for
# 1.1 GB of the others. CI fetches only libritts_r this way.
if [ "$#" -gt 0 ]; then MODELS=("$@"); fi

mkdir -p "$DEST"
for m in "${MODELS[@]}"; do
  if [ -d "$DEST/$m" ]; then echo "have $m"; continue; fi
  echo "fetching $m"
  curl -fsSL --max-time 600 "$BASE/$m.tar.bz2" | tar xj -C "$DEST"
done
du -sh "$DEST"/*/
