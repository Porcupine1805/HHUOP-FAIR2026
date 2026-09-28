# Real-data benchmark guide: Mushroom and Retail

## Dataset interpretation

The source files are the official SPMF `mushroom_utility` and `retail_utility` datasets. Their transaction membership is real, while SPMF labels their internal utilities as synthetic values generated in `[1,10]`. The converter represents every internal utility as a quantity and sets every external unit profit to 1. Thus item utility, transaction utility and utility occupancy are preserved exactly.

Sources:

- https://www.philippe-fournier-viger.com/spmf/v2/datasets.php/
- https://www.philippe-fournier-viger.com/spmf/publicdatasets/mushroom_utility_spmf.txt
- https://www.philippe-fournier-viger.com/spmf/publicdatasets/retail_utility_spmf.txt

Converted statistics:

| Dataset | Transactions | Items | Nonzero cells | Average length |
|---|---:|---:|---:|---:|
| Mushroom | 8,124 | 119 | 186,852 | 23.0000 |
| Retail | 88,162 | 16,470 | 908,576 | 10.3058 |

## Reproduce from scratch on Windows

Run commands from the project root.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\fetch_real_utility_datasets.ps1
scripts\test.bat
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run_real_pilot.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\summarize_results.ps1
```

Linux/macOS equivalents are `fetch_real_utility_datasets.sh`, `test.sh`, and `run_real_pilot.sh`.

## Run one configuration

```powershell
java -Xms512m -Xmx6g -cp build\hhuop-fair2026.jar org.hhuop.experiment.ExperimentMain `
  --db datasets/mushroom/database.txt --profits datasets/mushroom/profits.txt `
  --alpha 0.20 --beta 0.55 --sensitiveFraction 0.001 --seed 2026 `
  --algorithms SMAU,SMIU,SMSE `
  --out results/my_run.csv --actionDir results/my_run_actions
```

Parameters:

- `alpha`: minimum support ratio; larger values reduce the search space.
- `beta`: minimum average utility occupancy.
- `sensitiveFraction`: fraction of initial HUOPs selected as sensitive.
- `seed`: deterministic sensitive-pattern selection seed. Use matched seeds for all algorithms.
- `epsilon`: safety margin below beta, default `0.001`.
- `borderEta`: width of the near-border set used by SMSE, default `0.05`; reducing it saves memory but monitors fewer artificial-pattern candidates.
- `maxActions`: hard action limit, default `100000`.
- `algorithms`: exact comma-separated set from `SMAU,SMIU,SMSE`.

## Recommended controlled sweeps

Change one factor at a time and keep the same seeds (`2026` through `2030`).

| Experiment | Mushroom | Retail | Fixed values |
|---|---|---|---|
| Alpha | 0.15, 0.175, 0.20 | 0.0025, 0.005, 0.01 | beta 0.55 / 0.20 |
| Beta | 0.50, 0.55, 0.60 | 0.15, 0.20, 0.30 | alpha 0.20 / 0.005 |
| Sensitive fraction | 0.001, 0.005, 0.01 | 0.01, 0.05, 0.10 | baseline alpha/beta |

Always probe a new threshold using `MineMain` first. A low threshold on dense Mushroom may produce an exponential pattern space.

```powershell
java -Xmx6g -cp build\hhuop-fair2026.jar org.hhuop.experiment.MineMain `
  --db datasets/mushroom/database.txt --profits datasets/mushroom/profits.txt `
  --alpha 0.20 --beta 0.55
```

## Charts

Use `aggregate.csv` for raw matched runs and `summary.csv` for mean/standard-deviation charts.

Recommended charts:

1. Clustered columns: algorithm on X, `MC_mean`, `AC_mean`, and `HF_mean` on Y.
2. Clustered columns: algorithm on X, `utilityLoss_mean`, `OD_mean`, `TDR_mean`, and `QDR_mean` on Y.
3. Columns or log-scale columns: `endToEndSanitizationMs_mean` and `peakMemoryMB_mean`.
4. Line chart for alpha sweep: alpha on X and one metric on Y, one series per algorithm.
5. Line chart for beta sweep: beta on X and one series per algorithm.
6. Line chart for sensitive-fraction scalability: fraction on X; runtime, memory, and actions on Y in separate charts.

Use standard-deviation columns as error bars. Do not mix datasets in one unnormalized runtime line without clearly grouping or faceting by dataset.
