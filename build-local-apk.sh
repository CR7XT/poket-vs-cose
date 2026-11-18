#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
echo "=== PocketCode Local ZIP-to-APK Builder (macOS / Linux) ==="

if [ ! -f "debug.keystore" ]; then
  if [ -f "debug.keystore.base64" ]; then
    echo "Decoding debug.keystore from debug.keystore.base64..."
    if base64 -d debug.keystore.base64 > debug.keystore 2>/dev/null; then
      :
    elif base64 -D -i debug.keystore.base64 -o debug.keystore 2>/dev/null; then
      :
    else
      python3 -c "import base64; open('debug.keystore','wb').write(base64.b64decode(open('debug.keystore.base64').read().strip()))"
    fi
  else
    echo "Generating fresh debug.keystore with keytool..."
    keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  fi
fi

GRADLE_CMD=""
if [ -x "./gradlew" ]; then
  GRADLE_CMD="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD="gradle"
else
  FOUND_GRADLE=$(find "${HOME}/.gradle/wrapper/dists" -maxdepth 4 -type f -name gradle -path "*/bin/gradle" 2>/dev/null | head -n 1 || true)
  if [ -n "${FOUND_GRADLE}" ]; then
    GRADLE_CMD="${FOUND_GRADLE}"
  fi
fi

if [ -z "${GRADLE_CMD}" ]; then
  echo ""
  echo "Gradle was not found in PATH."
  echo "Open this unzipped folder in Android Studio and click:"
  echo "  Build > Build Bundle(s) / APK(s) > Build APK(s)"
  echo "(debug.keystore has already been prepared for you!)"
  exit 1
fi

echo "Building debug APK with ${GRADLE_CMD}..."
"${GRADLE_CMD}" :app:assembleDebug

echo ""
echo "SUCCESS! Your APK is ready at:"
echo "  $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
echo "Copy this file to your Android phone and tap it to install."
