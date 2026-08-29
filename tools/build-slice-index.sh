#!/usr/bin/env bash
# Build the dialogue index the QUI-019 vertical slice ships inside its APK.
#
# Not committed: a derived artefact. The fixture it derives from is what is under version
# control, so regenerating is always cheaper than storing (CLAUDE.md §6).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FIXTURE="${1:-$ROOT/fixtures/slice/chapter-one.labels.tsv}"
ASSETS="$ROOT/spike/ttsbinding/src/main/assets"
OUT="$ASSETS/slice-index.db"
BOOK="$ROOT/build/slice/chapter-one.epub"

cd "$ROOT"
"$ROOT/gradlew" --quiet :spike:indexer:installDist
mkdir -p "$ASSETS"
mkdir -p "$(dirname "$BOOK")"
"$ROOT/spike/indexer/build/install/indexer/bin/indexer" \
    build "$FIXTURE" "$OUT" --epub "$BOOK"

# The measured voice table travels with the index: the service cannot cast by gender
# without knowing what the model's speakers sound like.
cp "$ROOT/fixtures/voices/libritts_r-f0.tsv" "$ASSETS/voices.tsv"

echo
echo "asset written: ${OUT#$ROOT/}"
echo "cast written:  ${ASSETS#$ROOT/}/characters.json"
echo "voices:        ${ASSETS#$ROOT/}/voices.tsv"
echo "book written:  ${BOOK#$ROOT/}"
echo
echo "Rebuild the probe to pick up the asset, and side-load the EPUB into the reader."
echo "The book and the index come from the same fixture, so they cannot drift."
