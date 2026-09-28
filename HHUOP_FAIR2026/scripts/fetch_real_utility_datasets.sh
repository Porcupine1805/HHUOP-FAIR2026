#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; DOWNLOAD="$ROOT/build/downloads"; mkdir -p "$DOWNLOAD"; "$ROOT/scripts/build.sh"
for NAME in mushroom retail; do
  curl -L --fail -o "$DOWNLOAD/${NAME}_utility_spmf.txt" "https://www.philippe-fournier-viger.com/spmf/publicdatasets/${NAME}_utility_spmf.txt"
  java -cp "$ROOT/build/hhuop-fair2026.jar" org.hhuop.experiment.SPMFUtilityConverterMain --input "$DOWNLOAD/${NAME}_utility_spmf.txt" --outDir "$ROOT/datasets/$NAME"
done
