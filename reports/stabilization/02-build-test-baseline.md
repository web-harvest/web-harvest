# Phase 3 — Build and test baseline

**Branch:** `stabilization/comprehensive-cleanup-20261113`  
**Base tip before campaign commits:** `6eac8c5fd3ff27b79715500005abb736b114ed27`  
**Toolchain (verified):** OpenJDK 11.0.28, Apache Maven 3.9.11  
**Compiler source/target:** 11 (root `pom.xml`)

## Commands executed

```bash
export JAVA_HOME=…/openjdk@11/11.0.28/…
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am validate
mvn --batch-mode --no-transfer-progress -Dcheckstyle.skip=true -Dspotbugs.skip=true -pl webharvest-core -am test
mvn --batch-mode --no-transfer-progress -Dcheckstyle.skip=true -Dspotbugs.skip=true -pl webharvest-core -am test-compile failsafe:integration-test failsafe:verify
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

Logs: `reports/stabilization/evidence/01-validate.log` … `04-verify.log`, `06-verify-after-tests.log`.

## Results on tip + campaign changes (pre-commit measurement in `04-verify.log`)

| Gate | Result | Exit |
| --- | --- | --- |
| `validate` | SUCCESS | **0** |
| Surefire (unit) | **3489** tests, 0 failures, 0 errors, 0 skipped | **0** |
| Failsafe (IT) | **255** tests, 0 failures, 0 errors, 0 skipped | **0** |
| Checkstyle | **0** violations | (during verify) |
| SpotBugs | Analysis completed (`failOnError` remains false in POM) | (during verify) |
| JaCoCo instruction | **90.68%** (47011/51840) — threshold 80% met | |
| JaCoCo branch | **78.79%** (3701/4697) — threshold **80% not met** | |
| Full `verify` | **FAILURE** solely on JaCoCo branch gate | **1** |

Surefire/Failsafe separation is preserved (`*Test` / `*Tests` vs `*IT` / `*IntegrationTest`; `*IntegrationTest` excluded from Surefire).

## Compatibility contracts inspected (preserved)

- Legacy `set-var` registration: `SetVarCompatibilityRegistrationContractTest`
- Plugin registration / metadata / schema paths remain on the Core stack tip
- Gitleaks Stripe-shaped fixture in `HttpRedirectIntegrationBug36Test` aligned with merged #81 content

## Notes

- Default branch `2.2.x` has **no** `.github/workflows` files; CI YAML exists only on development tips.
- Prior cleanup inventory claimed Surefire 3517; this run reports **3489** suite tests (TestNG suite aggregation). Prefer the executed counts above.
