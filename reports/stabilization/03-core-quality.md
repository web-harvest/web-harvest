# Phase 3 continued — Core quality (JaCoCo / Checkstyle / SpotBugs)

**SHA under test:** working tree on `stabilization/comprehensive-cleanup-20261113` after Gate C residual batch 2 (pre-commit; see closeout for final commit SHAs)  
**Command:** `mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify`  
**Log:** `evidence/12-verify-after-batch2.log`  
**Summary:** `evidence/12-verify-after-batch2.summary.txt`  
**JaCoCo XML parse:** `evidence/13-jacoco-final.summary.txt`  
**Exit code:** **0** (`BUILD SUCCESS`)

## Executed suite counts (final verify)

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Surefire | **3552** | 0 | 0 | 0 |
| Failsafe | **255** | 0 | 0 | 0 |
| Checkstyle | **0** violations | | | |
| SpotBugs | **0** `BugInstance` findings (`spotbugsXml.xml`) | | | |

## JaCoCo (from `webharvest-core/target/jacoco-report/jacoco.xml`)

| Counter | Covered | Missed | Total | Ratio | Gate 80% |
| --- | ---: | ---: | ---: | ---: | --- |
| INSTRUCTION | 47327 | 4513 | 51840 | **91.29%** | met |
| **BRANCH** | **3758** | **939** | **4697** | **80.01%** | **met** |
| LINE | 10615 | 1120 | 11735 | 90.46% | n/a |
| METHOD | 4170 | 237 | 4407 | 94.62% | n/a |
| CLASS | 461 | 11 | 472 | 97.67% | n/a |

Branches required for 80% at fixed total 4697: **3758**. Measured covered: **3758** (ratio **0.800085**).

### Campaign delta (this continuation)

| Point | Branch covered | Ratio |
| --- | ---: | ---: |
| Prior campaign tip (`08-verify-final`) | 3717 | 79.14% |
| After Gate C residual batch 2 (`12-verify`) | 3758 | 80.01% |
| Measured gain | **+41 branches** | **+0.87 pp** |

Threshold **not lowered**. Unqualified Core `verify` now passes locally.

## Test batches added this session (behavior-asserting)

1. `GateCStabilizationResidualBatch2ContractTest` — If condition matrix (`==` / `!=` / literals), JsonToXml array/scalar/invalid JSON, XPath CSS/empty expression routing, While `empty` + literal-true safety break, Template language conversion, Call unknown-function fallback, SetVar empty/value expression  
2. `DefaultPluginFactoryMetadataCtorBranchContractTest` — PluginMetadata-only constructor instantiation  
3. `DefaultPluginRegistryUnregisterResidualBranchContractTest` — null/blank/missing unregister + sparse metadata cleanup  
4. `Stage1AndEqualsResidualBranchContractTest` — OptionalPluginProfile/LinkedService/KeyboardShortcut equals + Stage1 schema projection  

Focused pre-verify run: **29 tests / 0 failures** (`evidence/11-batch2-focused-ok.log`).

## Largest remaining branch gaps (class) after gate pass

| Class | Missed branches |
| --- | ---: |
| `PluginMetadataCatalog` | 55 |
| `CallPlugin` | 36 |
| `CommonUtil` | 20 |
| `HttpPlugin` | 20 |
| `SemanticXmlAnalyzer$Handler` | 20 |

These residuals remain meaningful follow-up under Issue #75 but no longer block the 80% Maven gate.
