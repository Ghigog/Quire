#!/usr/bin/env bash
# Install the Android SDK a dev container needs to build spike/ttsbinding.
#
# Not part of the image: the SDK is ~460 MB and the container is ephemeral, so this runs
# once per session. It needs `dl.google.com` on the environment's allowed-domain list —
# without it neither the SDK nor the Android Gradle Plugin resolves, and the build dies
# before reaching any of our code (CLAUDE.md §9).
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"
PLATFORM="platforms;android-35"     # matches compileSdk in spike/ttsbinding
BUILD_TOOLS="build-tools;35.0.0"

if ! curl -fsS -o /dev/null --max-time 20 -r 0-100 \
    "https://dl.google.com/android/repository/repository2-3.xml"; then
  echo "dl.google.com is not reachable from this container." >&2
  echo "Add it to the environment's allowed domains, or push and let CI build the APK." >&2
  exit 1
fi

mkdir -p "$SDK/cmdline-tools"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/$TOOLS_ZIP" "https://dl.google.com/android/repository/$TOOLS_ZIP"
  unzip -q -o "$tmp/$TOOLS_ZIP" -d "$SDK/cmdline-tools"
  mv -f "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses > /dev/null 2>&1 || true
"$SDKMANAGER" --sdk_root="$SDK" "platform-tools" "$PLATFORM" "$BUILD_TOOLS" > /dev/null

# local.properties is gitignored: it points at this container's SDK and nobody else's.
echo "sdk.dir=$SDK" > "$(cd "$(dirname "$0")/.." && pwd)/spike/ttsbinding/local.properties"

echo "Android SDK ready at $SDK ($(du -sh "$SDK" | cut -f1))"
echo
echo "Build the probe with:"
echo "  ./tools/build-slice-index.sh"
echo "  cd spike/ttsbinding && ../../gradlew assembleDebug"
