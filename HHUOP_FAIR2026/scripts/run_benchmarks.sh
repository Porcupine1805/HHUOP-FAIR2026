#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"; "$ROOT/scripts/build.sh"; mkdir -p results/bench
run_one() {
  local D="$1" A="$2" B="$3" F="$4" S="$5" TAG="$6"
  echo "=== $TAG: dataset=$D alpha=$A beta=$B sensitiveFraction=$F seed=$S ==="
  java -cp build/hhuop-fair2026.jar org.hhuop.experiment.ExperimentMain \
    --db "datasets/$D/database.txt" --profits "datasets/$D/profits.txt" \
    --alpha "$A" --beta "$B" --sensitiveFraction "$F" --seed "$S" \
    --out "results/bench/${TAG}.csv" --actionDir "results/bench/${TAG}_actions"
}
# Small/medium pilot points used in the draft paper.
run_one sparse      0.05 0.35 0.10 2026 sparse_b035
run_one sparse      0.05 0.45 0.10 2026 sparse_b045
run_one medium      0.05 0.35 0.10 2026 medium_b035
run_one medium      0.05 0.45 0.10 2026 medium_b045
run_one dense_small 0.05 0.55 0.01 2026 dense_small_b055
