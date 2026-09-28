#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"; "$ROOT/scripts/build.sh"
TMP="${TMPDIR:-/tmp}/hhuop_foodmart"
mkdir -p "$TMP"
curl -L --fail -o "$TMP/foodmart.txt" "https://www.philippe-fournier-viger.com/spmf/publicdatasets/quantitative/foodmart.txt"
curl -L --fail -o "$TMP/foodmartf1profit.txt" "https://www.philippe-fournier-viger.com/spmf/publicdatasets/quantitative/foodmartf1profit.txt"
java -cp build/hhuop-fair2026.jar org.hhuop.experiment.SPMFQuantitativeConverterMain \
  --dbIn "$TMP/foodmart.txt" --profitIn "$TMP/foodmartf1profit.txt" --outDir datasets/foodmart
printf 'Foodmart prepared under datasets/foodmart (source: SPMF public quantitative datasets).\n'
