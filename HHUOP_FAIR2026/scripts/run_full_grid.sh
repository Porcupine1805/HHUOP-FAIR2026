#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"; "$ROOT/scripts/build.sh"; mkdir -p results/full_grid
# Publication-oriented grid. This can be computationally expensive with the bundled exact miner.
for D in sparse medium dense_small; do
  for A in 0.02 0.05 0.10; do
    for B in 0.35 0.45 0.55; do
      for F in 0.01 0.05 0.10; do
        for S in 2026 2027 2028 2029 2030; do
          TAG="${D}_a${A}_b${B}_f${F}_s${S}"
          java -cp build/hhuop-fair2026.jar org.hhuop.experiment.ExperimentMain \
            --db "datasets/$D/database.txt" --profits "datasets/$D/profits.txt" \
            --alpha "$A" --beta "$B" --sensitiveFraction "$F" --seed "$S" \
            --out "results/full_grid/${TAG}.csv" --actionDir "results/full_grid/${TAG}_actions" || \
            echo "SKIP $TAG (no HUOP / resource limit)"
        done
      done
    done
  done
done
