# Groovy runtime migration notes

## Production baseline

The production runtime uses Apache Groovy **5.1.3** on the existing Java 17 baseline.

The Core runtime resolves two explicit runtime artifacts:

- `org.apache.groovy:groovy:5.1.3`
- `org.apache.groovy:groovy-jsr223:5.1.3`

Do not replace these with the `groovy-all` aggregator. The Groovy 6 compatibility experiment established that the aggregator can introduce `groovy-test-junit6`, changing Maven Failsafe provider selection and preventing this project's TestNG integration suite from being discovered. The production dependency graph must keep the TestNG provider and non-zero Failsafe execution.

## Compatibility contract

The migration preserves the existing Java 17 baseline and the WebHarvest JSR-223 integration:

- the `groovy` engine alias is discoverable through `ScriptEngineManager`;
- `ScriptPlugin` executes Groovy through the injected JSR-223 factory;
- Java interop remains available;
- the final script expression is returned through the existing WebHarvest variable adapter;
- packaged CLI and IDE distributions must each contain and discover the Groovy provider and report `GroovySystem.getVersion() == 5.1.3`.

The Core and packaged-launcher compatibility gates passed before the production migration was authorized. The production migration PR must still pass the exact-head full reactor, test, static-analysis, dependency, security and packaging checks against the production dependency profile.

## Downstream workflow owners

Groovy 5.1.3 is a major-version migration from the historical Groovy 2.0.4 runtime. Repository tests cannot establish compatibility for every customer-authored workflow, embedded application or downstream extension. Owners should validate their own scripts and integrations before upgrading. In particular, review scripts that depend on old Groovy APIs, metaprogramming details, compiler behavior, GDK methods or implicit coercions.

This migration does not promise source compatibility for every arbitrary Groovy 2.0.4 script and does not introduce a compatibility shim for undocumented language behavior.

## Security boundary

Groovy scripts execute inside the WebHarvest JVM with the operating-system privileges of the WebHarvest process. They are not sandboxed. Workflow XML, script bodies and nested includes must be treated as executable code. Run only trusted workflow definitions and apply OS-level least privilege, filesystem permissions, network restrictions and process/container isolation as appropriate.

Changing the Groovy runtime does not make untrusted scripts safe and is not a sandboxing feature. See [Workflow Security Model](SECURITY.md).

## Scope separation

Commons Collections 3.2.1 remains a separate migration. This document does not authorize a Java baseline change, unrelated dependency upgrades, release version change, GitHub Release, tag or Maven publication.
