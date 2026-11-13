# Phase 3 — Build / test baseline

**Toolchain (verified):** Java **11.0.28** (Homebrew OpenJDK 11), Maven **3.9.11**  
**Module under test:** `webharvest-core` with `-am`  
**Surefire/Failsafe separation:** preserved (unit vs integration; HTTP Surefire exclusion policy unchanged)

## Commands executed this continuation

| Step | Command | Exit | Evidence |
| --- | --- | ---: | --- |
| Focused residual batch | `mvn … -pl webharvest-core -am test -Dtest=GateCStabilizationResidualBatch2ContractTest,DefaultPluginFactoryMetadataCtorBranchContractTest,Stage1AndEqualsResidualBranchContractTest,DefaultPluginRegistryUnregisterResidualBranchContractTest` | **0** | `evidence/11-batch2-focused-ok.log` (29/0) |
| Full Core verify | `mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify` | **0** | `evidence/12-verify-after-batch2.log` |

## Full verify results (authoritative)

| Gate | Result |
| --- | --- |
| Surefire | **3552** tests, 0 failures, 0 errors, 0 skipped |
| Failsafe | **255** tests, 0 failures, 0 errors, 0 skipped |
| Checkstyle | **0** violations |
| SpotBugs | **0** BugInstance |
| JaCoCo instructions | **91.29%** |
| JaCoCo branches | **80.01%** (3758 / 4697) |
| `jacoco:check` 80% | **PASS** |
| Overall | **BUILD SUCCESS** |

## Compatibility contracts inspected / preserved

- SetVar registration and XML compatibility paths exercised via residual SetVar tests and existing stack
- Plugin factory metadata-constructor discovery covered without changing production constructors
- Registry unregister contracts extended without altering public API
- No runtime behavior changes were introduced solely to satisfy coverage

## Not run in this continuation

- Full multi-module reactor `verify` across IDE/CLI/plugins (Core-focused campaign)
- GitHub Actions runner execution (account spending limit; local evidence authoritative)
