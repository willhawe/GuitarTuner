#!/usr/bin/env bash
set -euo pipefail

./gradlew testDebugUnitTest lintDebug assembleDebug
