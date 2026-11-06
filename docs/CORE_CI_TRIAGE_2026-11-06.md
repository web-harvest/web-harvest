# Core CI triage — 2026-11-06

This note accompanies a CI-trigger branch based on the final PR #17 candidate plus the CallParam test-harness correction.

## Exact evidence before this follow-up

Candidate SHA: `eaf7852caa965892eae2461888fb4d05c4ad5a8d`  
Workflow run: `42233308478`

- Surefire: 2,891 tests, 2 failures. Only `CallParamPluginTest.testCallParamPluginExecute` and `CallParamPluginTest.testCallParamPluginWithIdAttribute` remain; the prior failures in FinalPushTo70, HtmlToXml and plugin metadata tests are gone.
- Failsafe: 242 tests, 8 failures. Five are in `WhilePluginIntegrationTest`; three are HTTP tests returning empty responses.
- The earlier three SetVar integration failures are absent.

## Follow-up test-harness correction

`CallParamPluginTest` now uses a real Guice-initialized `ScraperContext` instead of a mocked `DynamicScopeContext`. This follows the setup already used by `WhilePluginIntegrationTest`: Guice `ScraperModule`, a test `Config`, BeanShell scripting language and UTF-8 charset.

This is test-only. No production behavior was changed.

## Validation status

This branch is under `recovery/**` so the existing Core CI push trigger can execute against the corrected tree. The follow-up is **not validated until the resulting workflow run completes**.
