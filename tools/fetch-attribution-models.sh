#!/usr/bin/env bash
# Fetch the attribution candidates QUI-028 measures. Not committed: ~0.9 GB of weights.
# CLAUDE.md §6 — model files are fetched by a documented script, never stored in git.
#
# Needs huggingface.co, which was added to the environment's allowed-domain list on
# 2026-09-06 (QUI-037). Before that neither of these was reachable from a session
# container, and BookNLP's own host still is not — see the note below.
set -euo pipefail

DEST="${QUIRE_MODELS:-$HOME/.cache/quire/models}"
mkdir -p "$DEST"

# 1. The encoder this ticket exists to test. SpanBERT-base, ~431 MB fp32, ~108M parameters.
#    Loaded by `grimbert`, not by stock transformers: its head is a custom class. Downloaded
#    into the HF cache by predictors/grimbert_predict.py on first use, so nothing to do here
#    beyond warming it, which is worth doing separately from a timed run.
python3 - <<'PY'
from grimbert.model import SpeakerAttributionModel
from transformers import AutoTokenizer
m = "compnet-renard/spanbert-base-cased-literary-speaker-attribution"
SpeakerAttributionModel.from_pretrained(m)
AutoTokenizer.from_pretrained("bert-base-cased")
print("warmed", m)
PY

# 2. BookNLP, the ~63% the field reports against.
#
#    Its own host (people.ischool.berkeley.edu) 403s from a session container and was not
#    on the allowlist request, so this pulls the community mirror instead. That mirror is
#    one 1.1 GB zip of every BookNLP model; only the two speaker checkpoints are wanted:
#      speaker_google_bert_uncased_L-12_H-768_A-12-v1.0.1.model   ~438 MB, accuracy ceiling
#      speaker_google_bert_uncased_L-8_H-256_A-4-v1.0.1.model      ~57 MB, could ship
#    Commented out rather than deleted: 1.1 GB for two files is a bad trade, and asking for
#    the Berkeley host on the allowlist is the cheaper fix. Uncomment if that is refused.
#
# curl -fL --max-time 1800 -o "$DEST/booknlp_models.zip" \
#   https://huggingface.co/superdrew100/booknlp_models/resolve/main/booknlp_models.zip
# unzip -j "$DEST/booknlp_models.zip" '*speaker_google_bert*' -d "$DEST"

du -sh "$DEST" 2>/dev/null || true
echo "HF cache: ${HF_HOME:-$HOME/.cache/huggingface}"
