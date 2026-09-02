#!/usr/bin/env bash
# Fetch the Project Dialogism Novel Corpus for the QUI-028 bake-off.
#
# PDNC is 28 annotated novels — 37,131 quotations labelled with speaker, addressee and
# quotation type. It is the benchmark the attribution literature reports against, which is
# the whole reason we score on it: our numbers become comparable to published ones.
#
# It is NOT committed and must not be. Checked 2026-08-28 and again 2026-09-02: the
# repository carries no LICENSE file and its ReadMe states no terms, only a link to the
# paper. Treat it as evaluation-only — clone it, cite the paper, never redistribute it.
# Cite: Vishnubhotla, Hammond & Hirst, "The Project Dialogism Novel Corpus: A Dataset for
# Quotation Attribution in Literary Texts", LREC 2022. https://arxiv.org/abs/2204.05836
set -euo pipefail

REPO="https://github.com/Priya22/project-dialogism-novel-corpus"
DEST="${1:-${PDNC_HOME:-$HOME/.cache/quire/pdnc}}"

if [ -d "$DEST/.git" ]; then
  echo "already present: $DEST"
  git -C "$DEST" fetch --depth 1 origin master >/dev/null 2>&1 || true
  git -C "$DEST" reset --hard origin/master >/dev/null 2>&1 || true
else
  mkdir -p "$(dirname "$DEST")"
  echo "cloning PDNC into $DEST (~40 MB)"
  # Shallow: we want the current revision of the data, never its history.
  git clone --depth 1 "$REPO" "$DEST"
fi

novels=$(find "$DEST/data" -maxdepth 1 -mindepth 1 -type d | wc -l | tr -d ' ')
echo "PDNC ready: $DEST ($novels novels)"
echo
echo "Score Tier 1 against it:"
echo "  cd spike/attribution-bakeoff && gradle run --args=\"score --corpus $DEST\""
