#!/usr/bin/env bash
# Fetch the sherpa-onnx Android AAR for the QUI-017 bake-off.
#
# Not committed: a 38 MB binary carrying native ONNX Runtime builds for four ABIs.
# CLAUDE.md §6 — binaries of this size are fetched by a documented script, not stored in git.
set -euo pipefail

VERSION="${1:-1.12.15}"
DEST="$(cd "$(dirname "$0")/.." && pwd)/spike/ttsbinding/libs"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${VERSION}/sherpa-onnx-${VERSION}.aar"

mkdir -p "$DEST"
echo "fetching sherpa-onnx ${VERSION}"
curl -fSL -o "$DEST/sherpa-onnx.aar" "$URL"
ls -la "$DEST/sherpa-onnx.aar"
