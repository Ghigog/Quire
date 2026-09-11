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
# Xet resolves to hosts a session container cannot reach, and hangs rather than failing.
export HF_HUB_DISABLE_XET=1

python3 - <<'PY'
from grimbert.model import SpeakerAttributionModel
from transformers import AutoTokenizer
m = "compnet-renard/spanbert-base-cased-literary-speaker-attribution"
SpeakerAttributionModel.from_pretrained(m)
AutoTokenizer.from_pretrained("bert-base-cased")
print("warmed", m)
PY

# 2. BookNLP, the ~63% the field reports against. Two checkpoints of one architecture:
#      speaker_google_bert_uncased_L-8_H-256_A-4-v1.0.1.model     ~57 MB,  14M params (QUI-028)
#      speaker_google_bert_uncased_L-12_H-768_A-12-v1.0.1.model  ~438 MB, 110M params (QUI-041)
#
#    Berkeley's own host serves both, and is reachable over **https**. BookNLP's own code
#    builds `http` URLs, which the session proxy refuses; that is what made this look blocked,
#    and it is one character. The 1.1 GB community mirror this used to point at is no longer
#    needed and the commented-out block that fetched it is gone.
BOOKNLP=https://people.ischool.berkeley.edu/~dbamman/booknlp_models
for m in speaker_google_bert_uncased_L-8_H-256_A-4-v1.0.1.model \
         speaker_google_bert_uncased_L-12_H-768_A-12-v1.0.1.model; do
  [ -s "$DEST/$m" ] || curl -fL --max-time 1800 -o "$DEST/$m" "$BOOKNLP/$m"
done

# Their tokenizers and encoder configs come from the Hub on first use. Warm them here, so a
# timed run measures the model rather than a download.
python3 - <<'WARM'
from transformers import BertTokenizer, BertModel
for m in ("google/bert_uncased_L-8_H-256_A-4", "google/bert_uncased_L-12_H-768_A-12",
          "bert-base-cased"):
    BertTokenizer.from_pretrained(m, do_lower_case=False, do_basic_tokenize=False)
    BertModel.from_pretrained(m)
    print("warmed", m)
WARM

# 3. BookNLP+, QUI-041's baseline encoder (2026-09-11 directive). One 414 MB checkpoint:
#    `bodyanats/booknlp-plus-speaker-attribution` publishes five
#    leave-novels-out folds and we take fold 2, the one its own card names best. Apache-2.0.
#
#    Its base is bert-base-cased and it predates BookNLP's `[CAP]` token — predictors/
#    booknlp_predict.py carries the detail and verifies it on load, so do not "fix" the
#    filename to look like BookNLP's own.
PLUS="$DEST/booknlp_plus_split_2.model"
if [ -s "$PLUS" ]; then
  echo "already present: $PLUS"
else
  curl -fL --max-time 1800 -o "$PLUS" \
    https://huggingface.co/bodyanats/booknlp-plus-speaker-attribution/resolve/main/leave-x-out/split_2/best_model.model
fi

du -sh "$DEST" 2>/dev/null || true
echo "HF cache: ${HF_HOME:-$HOME/.cache/huggingface}"
