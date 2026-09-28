#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/build/classes"
: > "$ROOT/build/sources.txt"
find "$ROOT/src/main/java" -name '*.java' | sort | while IFS= read -r file; do printf '"%s"\n' "$file"; done > "$ROOT/build/sources.txt"
javac --release 17 -encoding UTF-8 -d "$ROOT/build/classes" @"$ROOT/build/sources.txt"
jar --create --file "$ROOT/build/hhuop-fair2026.jar" -C "$ROOT/build/classes" .
echo "Built $ROOT/build/hhuop-fair2026.jar"
