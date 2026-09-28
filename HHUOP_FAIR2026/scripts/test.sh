#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT/scripts/build.sh"
java -cp "$ROOT/build/hhuop-fair2026.jar" org.hhuop.experiment.SelfTestMain "$ROOT"
java -cp "$ROOT/build/hhuop-fair2026.jar" org.hhuop.experiment.OraclePropertyTestMain 200
