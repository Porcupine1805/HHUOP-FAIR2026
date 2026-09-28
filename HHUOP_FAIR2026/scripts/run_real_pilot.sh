#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; OUT="$ROOT/results/real_pilot"; mkdir -p "$OUT"; "$ROOT/scripts/build.sh"
run(){ local d="$1" a="$2" b="$3" f="$4" seed="$5" tag="${d}_a${a}_b${b}_f${f}_s${seed}"; java -Xms512m -Xmx6g -cp "$ROOT/build/hhuop-fair2026.jar" org.hhuop.experiment.ExperimentMain --db "$ROOT/datasets/$d/database.txt" --profits "$ROOT/datasets/$d/profits.txt" --alpha "$a" --beta "$b" --sensitiveFraction "$f" --seed "$seed" --out "$OUT/$tag.csv" --actionDir "$OUT/${tag}_actions"; }
for seed in 2026 2027 2028;do run mushroom .20 .55 .001 "$seed";run mushroom .20 .60 .001 "$seed";run retail .005 .20 .05 "$seed";run retail .005 .30 .05 "$seed";done
java -cp "$ROOT/build/hhuop-fair2026.jar" org.hhuop.experiment.AggregateResultsMain --inputDir "$OUT" --out "$OUT/aggregate.csv"
