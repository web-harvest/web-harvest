# CLI and headless execution: architecture decision

**Status:** Maintainer-approved direction; implementation and compatibility assessment in progress (2026-11-22).  
**Decision record:** [Issue #327](https://github.com/web-harvest/web-harvest/issues/327).  
**Scope:** Product role, integration boundary and deployment model of `webharvest-cli`. This document does not authorize a breaking change.

## 1. Decision

WebHarvest is primarily a reusable execution runtime. The **Core Java API is the primary integration surface**. A command-line process may remain as an optional, thin headless adapter for one-shot and batch execution where a separate process is useful.

A first-party Docker image is **not** a product requirement and will not be developed merely to justify the CLI. Docker is a packaging/isolation option to revisit only for a named consumer and a defined operational contract.

This separates three concerns:

1. **Integration boundary:** embedding Core in a host JVM versus invoking a separate process.
2. **Execution lifecycle:** one-shot/batch execution versus a long-running service.
3. **Packaging/orchestration:** executable JAR, container, Kubernetes Job/CronJob or another host-managed process.

A container does not imply a daemon, REST server, scheduler, persistent job manager or workflow control plane.

## 2. Why this direction fits the product

The project direction defines Core as the owner of workflow semantics, execution contracts and plugin contracts. CLI and IDE are clients; they must not become alternate semantic authorities. Keeping Java API first:

- lets larger Java products embed the runtime without process and deployment overhead;
- keeps runtime semantics independent of Docker, Kubernetes and a command-line syntax;
- makes the same Core execution behavior reusable by IDE, future MCP adapters and other host applications;
- avoids duplicating configuration, lifecycle, plugin resolution or execution logic in a process wrapper.

A separate launcher still has legitimate uses: process isolation, non-Java consumers, scheduled batch jobs, CI tasks and environments where operators need a simple executable boundary. Those are reasons to retain the *option* of a launcher, not evidence that a broad standalone distribution or server is required.

Kubernetes already provides Jobs and CronJobs for run-to-completion and scheduled processes. The scheduler/retry/lifecycle policy can remain with the deployment environment rather than becoming WebHarvest runtime responsibilities. See [Kubernetes Jobs](https://kubernetes.io/docs/concepts/workloads/controllers/job/) and [CronJobs](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/). Apache Camel is a useful precedent for an embeddable integration runtime with multiple deployment forms; see [What is Camel?](https://camel.apache.org/what-is-apache-camel/) and [Camel JBang running](https://camel.apache.org/manual/camel-jbang-running.html).

## 3. Current implementation facts (main, inspected 2026-11-22)

- `webharvest-cli` builds a shaded executable JAR with `org.webharvest.cli.WebHarvestCLI` as its main class.
- Its POM directly depends on Core and Database, FTP, Mail, WebBrowser and ZIP modules. This is a broad capability bundle, not a Core-only launcher.
- `WebHarvestCLI.main` creates a Guice `ScraperModule`, loads a configuration from a local path or HTTP(S) URL, applies working-directory/logging/debug/database-driver settings, initializes variables and invokes `Harvester.execute`.
- It is a batch process, not an HTTP server.
- The source contains a separate `CommandLineParser` with Commons CLI support and legacy `option=value` parsing. The inspected `WebHarvestCLI.main` still performs its own argument parsing; whether the newer parser is wired into the production entry point must be resolved before any CLI refactor.
- Existing tests cover parser behavior, logging configuration and configuration/runtime fragments, but the current suite has not yet been demonstrated to prove the end-to-end executable contract, process exit behavior or cancellation/shutdown semantics.
- The old `docs/web/cli.html` page contains stale promotional claims (including parallel batch processing, production readiness and old SourceForge canonical URLs). It must not be treated as authoritative current behavior until rewritten.
- Repository inspection found no current root Dockerfile or Compose definition and no GitHub Releases. These observations do not prove that no historical container experiment, Maven distribution or external consumer ever existed.

## 4. Provisional target shape

```text
                    WebHarvest Core
           execution + workflow/plugin contracts
                         |
        +----------------+----------------+
        |                |                |
    Java API        Headless launcher   IDE / future adapters
    embedded        one-shot process    clients of Core
        |                |
  Host application   host-managed process
                     JAR / optional container
```

The launcher, if retained after the compatibility audit, should delegate to the same public Core API and contain only process-boundary concerns:

- argument/configuration-source handling;
- working directory and environment handoff;
- logging configuration;
- clear success/failure exit status;
- cancellation, shutdown and resource cleanup;
- concise help and diagnostics.

It should not implement workflow semantics, plugin resolution, scheduling, retries, persistence or a service API independently of Core/host infrastructure.

Optional integrations should not be bundled indiscriminately. Their packaging/discovery should follow the approved plugin contract and compatibility model; no exclusions or module moves are authorized by this decision alone.

## 5. Compatibility and implementation gates

Before changing the current module or distribution, complete and record:

1. Historical repository/tag/Maven publication and distribution evidence; identify any known consumers or documented invocation contract.
2. Full CLI argument inventory, including legacy syntax, settings-file precedence, proxy, logging, debug, plugin and database-driver options.
3. Trace the production entry point and parser discrepancy; determine which parser and options are actually effective.
4. Execution failure propagation, process exit codes, interruption/cancellation and shutdown behavior.
5. Test inventory; add focused contract tests for the actual executable boundary before refactoring.
6. Dependency/capability graph: what Core requires, what each optional module contributes, and which dependencies are currently bundled transitively.
7. Migration and deprecation plan for any argument, artifact or module change.

Only after those gates pass should a narrow implementation PR be proposed. Preserve existing artifact coordinates and legacy invocation until compatibility evidence and an explicit migration decision support changing them.

## 6. Non-goals

This decision does not authorize:

- deleting `webharvest-cli` or removing its Maven artifact;
- breaking or silently changing legacy command-line arguments;
- excluding optional modules or changing plugin distribution;
- creating a daemon, REST/HTTP server, scheduler, queue, job database or control plane;
- building or publishing a Docker image;
- release, signing, publishing or deployment work.

The current artifact must continue to be represented accurately in SBOM, license and provenance records while it is built and distributed.

## 7. Documentation policy

This decision record is the authoritative architecture rationale. `README.md`, `docs/MONOREPO.md` and `docs/PROJECT_DIRECTION.md` link here for the concise product direction. The historical `docs/web/cli.html` page must be revised separately to distinguish verified current behavior from planned/unsupported claims; generated Javadoc must be updated alongside any source/API changes, not used as a substitute for this product decision.
