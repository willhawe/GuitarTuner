#!/usr/bin/env bash
set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "adb must be available on PATH." >&2
    exit 1
fi

./gradlew installDebug
adb shell am start -n com.whawe.guitartuner/.MainActivity
