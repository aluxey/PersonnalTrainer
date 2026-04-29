#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
APK_PATH="${1:-$ROOT_DIR/android-exporter/app/build/outputs/apk/debug/app-debug.apk}"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb introuvable. Installe Android Platform Tools ou utilise l'installation manuelle depuis le telephone." >&2
  exit 1
fi

if [ ! -f "$APK_PATH" ]; then
  echo "APK introuvable: $APK_PATH" >&2
  echo "Construis d'abord avec: cd android-exporter && ./gradlew :app:assembleDebug" >&2
  exit 1
fi

adb devices
adb install -r "$APK_PATH"
