# Phase 3 continued — Core quality (JaCoCo / Checkstyle / SpotBugs)

**SHA under test:** working tree on `stabilization/comprehensive-cleanup-20261113` (pre-push; see closeout for final commit SHAs)  
**Command:** `mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify`  
**Log summary:** `evidence/08-verify-final.summary.txt`  
**Exit code:** **1** (JaCoCo branch gate only)

## Executed suite counts (final verify)

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Surefire | **3523** | 0 | 0 | 0 |
| Failsafe | **255** | 0 | 0 | 0 |
| Checkstyle | **0** violations | | | |
| SpotBugs | Analysis completed (`failOnError=false` unchanged) | | | |

## JaCoCo (from `webharvest-core/target/jacoco-report/jacoco.xml`)

| Counter | Covered | Missed | Total | Ratio | Gate 80% |
| --- | ---: | ---: | ---: | ---: | --- |
| INSTRUCTION | 47087 | 4753 | 51840 | **90.83%** | met |
| **BRANCH** | **3717** | **980** | **4697** | **79.14%** | **NOT met** |
| LINE | 10567 | 1168 | 11735 | 90.05% | n/a |
| METHOD | 4165 | 242 | 4407 | 94.51% | n/a |
| CLASS | 461 | 11 | 472 | 97.67% | n/a |

Branches still required for 80% (fixed total): **~41**.

### Campaign delta

| Point | Branch covered | Ratio |
| --- | ---: | ---: |
| Tip baseline this session (`04-verify`) | 3701 | 78.79% |
| After residual test batches (`08-verify-final`) | 3717 | 79.14% |
| Measured gain | **+16 branches** | **+0.35 pp** |

Threshold **not lowered**.

## Test batches added (behavior-asserting)

1. `StabilizationResidualBranchContractTest` — Script/Include/GetVar/XML/Call/While/JsonGenerator path validation and return contracts  
2. `StabilizationCallWhileResidualBranchContractTest` — Call blank-name validation, Include stub execute, While false condition, multi-param call  
3. `CommonUtilIoAndUrlResidualBranchContractTest` — file IO + query `fullUrl` + `createVariable` edges  

## Largest remaining branch gaps (class)

| Class | Missed branches | Approx % |
| --- | ---: | ---: |
| `PluginMetadataCatalog` | 55 | 82.8% |
| `CallPlugin` | 36 | 74.6% |
| `CommonUtil` | 20 | 92.3% |
| `HttpPlugin` | 20 | 83.1% |
| `SemanticXmlAnalyzer$Handler` | 20 | 71.4% |
| `PluginMetadataJsonGenerator` | 17 | 67.3% |
| `WhilePlugin` | 16 | 72.4% |

### Why 80% remains unmet

Remaining gaps concentrate in deep metadata catalog schema projection, CallPlugin execution lambda edge combinations (return-flag / explicit-return fallbacks), HTTP networking branches, and semantic XML handler edges. Closing ~41 branches requires another focused Gate C iteration under Issue #75 — not achievable honestly by assertion-free line poking.
