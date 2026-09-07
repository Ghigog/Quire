#!/usr/bin/env bash
# Fetch a quantized 1B SLM candidate for QUI-006's runtime — Llama 3.2 1B Instruct,
# Q4_K_M GGUF, for a llama.cpp/JNI backend. Not committed: ~0.8 GB (CLAUDE.md §6).
#
# This gets one candidate onto disk so its RSS and tokens/s can be measured on the
# reference device (docs/adr/0001-slm-runtime.md). It does not pick the backend — an
# ExecuTorch candidate needs its own export pipeline and is future work, tracked there.
#
# huggingface.co is on the environment's allowed-domain list (QUI-037); its Xet transfer
# hosts are not. This repo's files resolve through xet-bridge — a plain `curl` follow of
# the redirect hangs against a blocked host instead of failing. HF_HUB_DISABLE_XET=1 makes
# huggingface_hub fall back to the plain HTTP path, which does resolve.
set -euo pipefail

python3 -m pip install --quiet "huggingface_hub>=0.34"

export HF_HUB_DISABLE_XET=1

DEST="${QUIRE_MODELS:-$HOME/.cache/quire/models}"
REPO="${1:-bartowski/Llama-3.2-1B-Instruct-GGUF}"
FILE="${2:-Llama-3.2-1B-Instruct-Q4_K_M.gguf}"
mkdir -p "$DEST"

python3 - "$REPO" "$FILE" "$DEST" <<'PY'
import sys
from huggingface_hub import hf_hub_download

repo_id, filename, dest = sys.argv[1:4]
path = hf_hub_download(repo_id=repo_id, filename=filename, local_dir=dest)
print("fetched", path)
PY

ls -la "$DEST"
