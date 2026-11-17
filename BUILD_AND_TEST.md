# Build and Test Guide

This document describes the repository's current Maven validation paths. GitHub Actions is the authoritative CI gate; a local focused test run is not equivalent to the complete Core or reactor checks.

For branch, review, CI evidence and network-test policy, see the [Contributing Guide](CONTRIBUTING.md).

## Prerequisites

- JDK 11 for the current Core CI baseline.
- Maven 3.6 or newer.
- Node.js and npm only when working on IDE frontend assets.

Run Maven commands from the repository root unless a command explicitly changes directory.

## Core validation

Run the focused Core unit-test path and required upstream modules:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am test
```

Run the full Core lifecycle, including Surefire, Failsafe and configured quality gates:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

The full Core verification is the required gate for Core changes. It includes the configured Checkstyle and JaCoCo checks; do not replace it with a focused test and describe the result as a complete pass.

## Full reactor validation

For changes that may affect multiple modules, run:

```bash
mvn --batch-mode --no-transfer-progress verify
```

A full reactor run is broader and more expensive than the Core gate. Select it when the changed modules or dependency boundaries require it.

## Network-tagged Core tests

The default Core CI gate is deterministic and excludes network-tagged tests. Run the opt-in network suites separately when the change requires them and network access is available.

Network-tagged unit tests:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am -Pnetwork-tests test-compile surefire:test
```

Network-tagged integration tests:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am -Pnetwork-tests test-compile failsafe:integration-test failsafe:verify
```

These commands are diagnostics for explicitly network-dependent tests. They do not replace the deterministic CI gate and should not be combined into a single invocation that could hide later diagnostics after an earlier failure.

## Reporting validation evidence

For a pull request, record:

- the exact commit SHA tested;
- the exact Maven command or GitHub Actions workflow/run;
- Surefire and Failsafe test counts, failures, errors and skipped tests;
- Checkstyle and JaCoCo results when applicable;
- whether validation was focused, Core-wide or full-reactor;
- network dependencies, environment limitations and untested behavior.

Do not weaken a gate to obtain a green result. Do not delete tests, add broad exclusions, use `continue-on-error`, or lower coverage thresholds to conceal a regression.

## IDE frontend work

The IDE frontend has its own Node.js/npm toolchain. Do not rely on the historical commands, paths, generated bundle names or manual Jetty startup instructions that previously appeared in this file. Verify the current frontend package scripts and Maven module lifecycle before running or documenting an IDE-specific build.

For current contributor workflow and review requirements, return to the [Contributing Guide](CONTRIBUTING.md).
