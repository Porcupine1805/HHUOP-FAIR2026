# Reviewer experiment protocol

All runs use a fresh JVM (`-Xms256m -Xmx6g`), one algorithm per process, and the same sensitive-set seed for every algorithm in a block. Sensitive patterns are a seeded uniform sample of the mined HUOP set. `peakRSS_MB` is the peak Windows working set of that process. `peakMemoryMB` remains the JVM heap. The paired block for every test is the sensitive-set seed.

## E1. Main comparison

Manuscript thresholds on the public SPMF files, fractions 5%, 10%, and 25%, seeds 2026–2035, algorithms `SMAU`, `SMIU`, `SMSE`, `DEL`, `HUI`, `GRED`.

| Dataset | alpha | beta | HUOPs at this realization |
|---|---:|---:|---:|
| Retail | 0.005 | 0.30 | 16 |
| Mushroom | 0.20 | 0.65 | 21 |
| Foodmart | 0.003 | 0.30 | 153 |
| Chess | 0.62 | 0.40 | 855 |

Chess at the manuscript beta has many more HUOPs than the manuscript table because this file is the SPMF utility realization, not the unpublished generated-utility file. The run is kept so the manuscript threshold is reported directly. E2 adds the nearby betas at which the HUOP count is 52 and 9.

## E2. Threshold sweep

Fraction 10%, seeds 2026–2035, same six algorithms.

| Dataset | alpha | beta | HUOPs |
|---|---:|---:|---:|
| Chess | 0.62 | 0.44 | 52 |
| Chess | 0.62 | 0.46 | 9 |
| Mushroom | 0.20 | 0.60 | 133 |
| Retail | 0.005 | 0.25 | 32 |
| Foodmart | 0.003 | 0.35 | 71 |
| Foodmart | 0.003 | 0.40 | 25 |

## E3. SMSE weight sensitivity

Fraction 10%, algorithm `SMSE` only. One weight moves at a time. The others stay at `wM=1`, `wA=1`, `wD=0.1`, `wS=0.5`.

Levels: `wM` and `wA` in {0, 0.5, 1, 2, 5}; `wD` in {0, 0.1, 0.5, 1}; `wS` in {0, 0.5, 1, 2}. The default row uses seeds 2026–2035. Each changed level uses seeds 2026–2030. Inference uses the five seeds shared by every level.

## E4. Utility realizations

Chess, Mushroom, and Retail keep their transaction membership. Each utility seed `701`–`705` draws quantity and unit profit independently from `{1,…,10}`. Foodmart is not redrawn. Fraction 10%, sensitive seeds 2026–2030, same thresholds as E1, same six algorithms.

## Statistics

For each dataset, threshold, and fraction, blocks are seeds. The report gives the mean, sample standard deviation, and 95% Student t interval; the Friedman test with Kendall's W; and paired Wilcoxon tests with the exact sign distribution when there are at most 16 nonzero pairs and no tied ranks. Holm adjustment is applied inside that dataset-threshold-fraction-metric family. Paired mean differences also have a 95% bootstrap percentile interval from 10,000 resamples.
