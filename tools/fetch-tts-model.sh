#!/usr/bin/env bash
# Fetch the production TTS model — Piper `libritts_r` medium (ADR-0002), 904 speakers.
#
# Same source tools/fetch-sherpa-aar.sh pulls from, and not committed for the same reason:
# ~82 MB of ONNX weights (CLAUDE.md §9). Extracted into the cache the desktop app reads by
# default, so `quire read book.epub` finds it with no flags.
#
# The F0 profile and quality list this project casts against are committed under
# fixtures/voices/ and were measured from exactly this model — see spike/hostbench.
#
# Usage: tools/fetch-tts-model.sh [model-name]
set -euo pipefail

MODEL="${1:-vits-piper-en_US-libritts_r-medium}"
DEST="${QUIRE_MODELS:-$HOME/.cache/quire/models}"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/${MODEL}.tar.bz2"

mkdir -p "$DEST"
if [ -d "$DEST/$MODEL" ]; then
    echo "$DEST/$MODEL already exists; nothing to do"
    exit 0
fi

echo "fetching $MODEL"
curl -fSL --retry 3 -o "$DEST/${MODEL}.tar.bz2" "$URL"
tar -xf "$DEST/${MODEL}.tar.bz2" -C "$DEST"
rm -f "$DEST/${MODEL}.tar.bz2"
ls -la "$DEST/$MODEL"
