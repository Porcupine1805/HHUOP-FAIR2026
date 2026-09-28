# HHUOP FAIR 2026 Java Project

A dependency-free Java research prototype for hiding sensitive **High-Utility Occupancy Patterns (HUOPs)** in quantitative transaction databases.

Implemented heuristics:

- **HHUOP-SMAU** — selects the maximum-utility item of the current sensitive HUOP in the selected victim transaction.
- **HHUOP-SMIU** — selects the minimum-utility item.
- **HHUOP-SMSE** — evaluates candidate victim-item actions using HUOP-specific missing-risk, artificial-risk, utility distortion and sensitive-pattern coverage.
- **HHUOP-DEL** — deletion-only baseline. Same victim and maximum-utility item as SMAU, but the item is always removed.
- **HHUOP-HUI** — HHUIF-style baseline. The quantity step freezes transaction utility, then the outer loop re-mines HUOPs.
- **HHUOP-GRED** — unit greedy baseline. Each step removes one unit of the item that most lowers occupancy of the sensitive HUOP.

SMAU, SMIU, and SMSE share: (i) the exact occupancy-aware quantity-reduction formula, (ii) an explicit support-hiding branch when a keep-support edit is infeasible, and (iii) final exact HUOP re-mining in a verify-and-repair loop. Pass `--algorithms SMAU,SMIU,SMSE,DEL,HUI,GRED` to `ExperimentMain` to include the baselines. The default remains `SMAU,SMIU,SMSE`.

## Requirements

- JDK 17 or newer.
- No Maven/Gradle or external Java dependency is required.

## Quick start

Linux/macOS:

```bash
./scripts/run_example.sh
```

Windows:

```bat
scripts\run_example.bat
```

The running example uses `alpha=0.4`, `beta=0.78`, and the sensitive HUOP `{a,c}`. The self-test checks that the original HUOP set is exactly `{a,c}` and `{a,c,d}` and that all three sanitizers achieve `HF=0`.

Run the complete regression suite, including 200 deterministic randomized databases checked against a brute-force oracle:

```bat
scripts\test.bat
```

On Linux/macOS use `./scripts/test.sh`.

## Compile manually

```bash
./scripts/build.sh
```

The build produces `build/hhuop-fair2026.jar`.

## Mine HUOPs

```bash
java -cp build/hhuop-fair2026.jar org.hhuop.experiment.MineMain \
  --db datasets/example/database.txt --profits datasets/example/profits.txt \
  --alpha 0.4 --beta 0.78
```

## Run the three hiding algorithms

```bash
java -cp build/hhuop-fair2026.jar org.hhuop.experiment.ExperimentMain \
  --db datasets/example/database.txt --profits datasets/example/profits.txt \
  --alpha 0.4 --beta 0.78 --sensitiveFile datasets/example/sensitive.txt \
  --out results/example_results.csv --actionDir results/example_actions
```

Instead of `--sensitiveFile`, use `--sensitiveFraction 0.10 --seed 2026` to select a reproducible random subset from the original HUOP set. The **same selected set** is then reused by SMAU, SMIU and SMSE.

## Reproduce the pilot benchmarks

```bash
./scripts/generate_benchmarks.sh
./scripts/run_benchmarks.sh
```

The safe pilot script runs sparse, medium and bounded dense synthetic instances. Raw CSV files and per-action logs are saved under `results/bench/`. A concise interpretation is in `results/PILOT_SUMMARY.md`.

For a broader publication-oriented parameter sweep:

```bash
./scripts/run_full_grid.sh
```

The full grid is intentionally separated because the exact self-contained miner can be expensive on dense/low-threshold instances.

For the real-transaction Mushroom and Retail utility benchmarks, including download, conversion, safe parameter grids, aggregation and chart instructions, see [`docs/REAL_DATA_BENCHMARK_GUIDE.md`](docs/REAL_DATA_BENCHMARK_GUIDE.md).

## Input format

Profit file:

```text
a 4
b 3
c 6
```

Database file:

```text
T0 a:10 b:1 c:3
T1 b:2 c:1
```

Sensitive file (one itemset per line):

```text
a,c
```

All unit profits must be positive in this implementation, matching the problem definition used in the draft paper.

## Core algorithmic details

For a sensitive HUOP `X` supported by transaction `T`, let `A=u(X,T)`, `B=TU(T)`, `s=sup(X)`, `r=A/B`, `R=s*uo(X)`, and `lambda=beta-epsilon`. Keeping all other supporting transactions unchanged, the target local occupancy is

```text
tau = s*lambda - (R-r)
```

and the exact utility reduction needed to make the edited local occupancy at most `tau` is

```text
delta* = (A - tau*B)/(1-tau),  k* = ceil(delta*/p(i)).
```

If `k* <= q(i,T)-1`, the edit preserves support. Otherwise the implementation enters the support-hiding branch by removing the selected victim item from that transaction and then recomputes HUOP status.

The victim transaction minimizes the number of original non-sensitive HUOPs it supports (NSHC), with deterministic TID tie-breaking.

`HHUOP-SMSE` temporarily simulates each candidate item action and computes:

```text
PSE = (wM*MR + wA*AR + wD*Dist) / (1 + wS*SHC)
```

with defaults `wM=1`, `wA=1`, `wD=0.1`, `wS=0.5`. `MR` is the number of affected original non-sensitive HUOPs that would be lost, `AR` is the number of monitored near-border patterns that would become HUOPs, `Dist` is normalized utility loss, and `SHC` is active sensitive-pattern coverage. This is a greedy/local heuristic, not a global optimum guarantee.

## Metrics

The runner reports:

- `HF = |S intersect H(D')| / |S|` (Hiding Failure).
- `MC = |(H(D)\S) \ H(D')| / |H(D)\S|` (Missing Cost).
- `AC = |H(D') \ H(D)| / |H(D')|` (Artificial Cost).
- `DUS = U(D') / U(D)` (Database Utility Similarity).
- `utilityLoss = 1-DUS`.
- `OD = changed item-transaction cells / original nonzero cells`.
- `TDR = changed transactions / |D|`.
- `QDR = (Q(D)-Q(D'))/Q(D)`.
- action count, initial mining, setup, action selection, verification, end-to-end sanitization and total wall-clock time.
- sampled peak JVM heap usage across initial mining and the complete sanitization phase.

`endToEndSanitizationMs` is the authoritative sanitizer wall-clock measurement. `actionSelectionMs` is a narrower diagnostic measurement and is not total runtime.

`HF=0` is treated as the hard privacy constraint; the remaining metrics characterize utility, side effects and efficiency.

## Project architecture

- `model/` — quantitative database, transactions, itemsets and HUOP records.
- `io/` — parser for the research input format.
- `mining/` — `HUOPMiner` interface, exact vertical UO-list-style miner, and direct pattern evaluator.
- `sanitize/` — common verify-and-repair framework plus SMAU/SMIU/SMSE.
- `experiment/` — CLI, sensitive-set selection, synthetic generator and self-test.
- `util/` — memory sampler.
- `datasets/` — running example and generated pilot instances.
- `results/` — raw metrics and action logs.

The included `VerticalHUOPMiner` is exact and makes the project fully runnable without external libraries. It uses primitive vertical arrays, sorted-TID merge joins and transaction-local suffix remaining utilities. Sanitizers cache transaction/database utility and update monitored pattern statistics incrementally after each edit. For extremely large or dense final experiments, an established optimized HUOPM/HUOMIL implementation can still replace it behind the `HUOPMiner` interface without changing the sanitizers.

## Optional real quantitative benchmark: Foodmart

SPMF publishes a quantitative Foodmart version with separate purchase quantities and unit-profit files. If `curl` is available:

```bash
./scripts/fetch_foodmart.sh
```

The script downloads the two public SPMF files and converts them to `datasets/foodmart/database.txt` and `profits.txt`. The converter can also be called directly as `org.hhuop.experiment.SPMFQuantitativeConverterMain` for other FHUQI/VHUQI-format datasets. Data are not redistributed in this ZIP; they are fetched from the original SPMF source.

## Publication protocol

For defensible final results, run all algorithms on the exact same database/sensitive set, use multiple seeds, vary `alpha`, `beta` and sensitive-set size, and report both side-effect and resource metrics. Recommended reporting is median/IQR plus mean/std; use paired non-parametric tests across matched configurations (e.g., Friedman followed by Holm-corrected pairwise comparisons). Keep raw CSVs and action logs as the reproducibility record.

Pilot runtime/memory values bundled with this project are **development sanity checks only**. Re-run timing and memory experiments on the machine whose hardware/JVM details will be reported in the final paper.
