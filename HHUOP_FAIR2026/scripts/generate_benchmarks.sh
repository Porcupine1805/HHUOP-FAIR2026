#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"; "$ROOT/scripts/build.sh"
# Reproducible synthetic datasets used by the pilot validation in the paper.
java -cp build/hhuop-fair2026.jar org.hhuop.experiment.SyntheticGeneratorMain --transactions 400 --items 24 --avgLen 4 --seed 101 --outDir datasets/sparse
java -cp build/hhuop-fair2026.jar org.hhuop.experiment.SyntheticGeneratorMain --transactions 400 --items 24 --avgLen 7 --seed 102 --outDir datasets/medium
# A bounded dense instance that finishes quickly with the exact self-contained miner.
java -cp build/hhuop-fair2026.jar org.hhuop.experiment.SyntheticGeneratorMain --transactions 250 --items 16 --avgLen 8 --seed 103 --outDir datasets/dense_small
printf 'Generated datasets/sparse, datasets/medium and datasets/dense_small\n'
