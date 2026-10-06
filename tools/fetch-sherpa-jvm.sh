#!/usr/bin/env bash
# Fetch the sherpa-onnx JVM API and the native libraries for this machine's OS/arch.
#
# This is the desktop counterpart to tools/fetch-sherpa-aar.sh: the same runtime, the same
# version, but built for the JVM rather than for Android. Not committed (CLAUDE.md §9) — the
# jars are ~8 MB and are reproducible from this script.
#
# The API jar carries the Java classes only; the native jar carries the shared libraries.
# Both go on the classpath and sherpa's own loader extracts and binds the .dylib/.so at
# first use, so no java.library.path juggling is needed.
#
# Usage: tools/fetch-sherpa-jvm.sh [version]
set -euo pipefail

VERSION="${1:-1.12.15}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/desktop/libs"

# Match the native jar to the host, so the same command works on a Mac and in CI.
case "$(uname -s)" in
    Darwin) OS="osx" ;;
    Linux)  OS="linux" ;;
    *) echo "unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac
case "$(uname -m)" in
    arm64|aarch64) ARCH="aarch64" ;;
    x86_64|amd64)  ARCH="x64" ;;
    *) echo "unsupported arch: $(uname -m)" >&2; exit 1 ;;
esac

BASE="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${VERSION}"
API_JAR="sherpa-onnx-v${VERSION}.jar"
NATIVE_JAR="sherpa-onnx-native-lib-${OS}-${ARCH}-v${VERSION}.jar"

mkdir -p "$DEST"
echo "fetching sherpa-onnx ${VERSION} for ${OS}-${ARCH}"
curl -fSL --retry 3 -o "$DEST/$API_JAR" "$BASE/$API_JAR"
curl -fSL --retry 3 -o "$DEST/$NATIVE_JAR" "$BASE/$NATIVE_JAR"
ls -la "$DEST"
