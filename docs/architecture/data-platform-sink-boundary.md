# Data-platform sink boundary and publication lifecycle

**Status:** Design proposal only; not a stable public API or implementation authorization.
**Related issue:** [#358 — interruption, deadlines and external-effect uncertainty](https://github.com/web-harvest/web-harvest/issues/358).
**Architecture constraint:** WebHarvest remains independently usable. A future data platform is an optional destination behind an adapter, never a Core dependency.

## 1. Purpose

WebHarvest extracts and transforms data. A future portable data platform may store, version, index and replicate selected outputs from WebHarvest, but the two projects must remain independently deployable and independently evolvable.

This note defines the compatibility seam to preserve while stabilizing runtime ownership, cancellation and plugin contracts. It does not add a platform client, transport, database, replication protocol or dependency to WebHarvest.

## 2. Four distinct lifecycles

Do not collapse these state machines:

1. **Execution/resource lifecycle** — admission, execution, cancellation request, worker exit, resource quiescence, drain, quarantine and retirement.
2. **Extraction/output lifecycle** — source observed, extraction performed, output produced, output validated and artifact staged.
3. **Sink/publication lifecycle** — publication requested, accepted, transferred, integrity checked, durably committed, rejected or outcome unknown.
4. **Source/domain lifecycle** — meaning assigned by the source system (for example a transaction may be pending, posted, reversed or refunded).

One lifecycle's success does not prove another's success. Worker exit is not durable publication. A successful sink write is not proof that the upstream source transaction is settled. A transport acknowledgement is not proof that the destination committed the artifact.

## 3. Stable artifact identity and provenance

A future output contract should be able to describe an artifact independently of its in-memory Java object or workflow variable name.

Candidate metadata:

- stable execution/run ID and operation ID;
- stable artifact ID and revision ID;
- source URI or source-system reference, subject to secret/PII redaction;
- acquisition/extraction timestamps, preserving source event time separately from processing time;
- workflow/configuration identity and exact version/digest;
- WebHarvest version, plugin IDs/versions and relevant provider identity;
- output media type, schema/format version, byte length and content digest;
- parent/source artifact references and transformation lineage;
- validation status and known warnings/uncertainty;
- publication attempts and destination receipt references.

The original source evidence and produced output should remain distinguishable. Normalization or enrichment must not silently overwrite source material. Do not put credentials, session tokens or sensitive payload fields into manifests, metrics or logs.

## 4. Publication is not execution completion

The execution result should report what happened to the workflow. Publication is a separate outcome reported by the selected sink/adapter.

Candidate sink outcome vocabulary:

| Outcome | Meaning |
|---|---|
| NOT_STARTED | No external publication side effect was initiated |
| IN_PROGRESS | A publication attempt is active; destination outcome is not yet known |
| COMMITTED | Destination confirmed the required durable commit boundary |
| FAILED | Evidence establishes that publication failed without an ambiguous commit |
| UNKNOWN | A side effect may have occurred, but available evidence cannot establish commit or failure |

A richer adapter may expose intermediate transport states, integrity verification and authorization decisions. Those must not be collapsed into COMMITTED.

UNKNOWN is a first-class outcome. A timeout after sending a request, lost acknowledgement or process crash may leave the destination committed even though WebHarvest did not receive confirmation. Do not automatically retry a non-idempotent operation in that state.

## 5. Idempotency, retries and bounded hand-off

Where a destination supports idempotent writes, the adapter should use a stable operation/artifact identity and exact revision/digest. The destination must define its deduplication scope and retention. A client-generated ID alone does not make an operation idempotent.

Where idempotency is unavailable:
- preserve the attempt identity and destination response/transport evidence;
- expose UNKNOWN when commit cannot be determined;
- require reconciliation or an explicitly safe operator decision before retry;
- never convert uncertainty into FAILED merely because a local worker exited.

A future hand-off must use bounded queues and explicit backpressure. In-memory callback acceptance is not durable storage. If output is staged locally, the staging durability boundary and recovery behavior must be explicit. A full queue must fail, defer or apply an explicitly configured policy; it must not silently drop output or claim publication success.

## 6. Adapter boundary

A future sink belongs outside Core, for example as an optional plugin or integration adapter after the public Plugin SPI and its trust/compatibility gates are stable.

The adapter boundary should accept a versioned artifact description and payload stream (or equivalent portable representation), and return an explicit publication disposition/receipt. It should define:
- destination identity and scope;
- accepted artifact and schema versions;
- maximum payload/chunk sizes and streaming behavior;
- authorization and data-class policy;
- digest/integrity verification;
- timeout, cancellation and abort capabilities;
- durable commit semantics;
- idempotency scope and reconciliation lookup;
- partial/batch result semantics;
- privacy-safe diagnostics.

Core must not import platform-specific classes, credentials, SDKs or wire protocols. The platform must not assume WebHarvest-specific execution internals.

## 7. Relationship to the current runtime work

The current runtime lifecycle work remains the prerequisite for safe plugin/adapter execution, not a reason to add persistence or replication into the runtime ledger.

- PR #390 provides the merged owner-bound resource-ledger foundation.
- PR #391 provides merged failure-safe scraping-scope teardown.
- PR #403 is the open integration of runtime EventBus ownership and owner-bound resource lifecycle. Its review conditions must be resolved on that PR; this document does not approve or replace them.
- Issue #358 specifies interruption/deadline propagation and external-effect disposition. Its source audit correctly states that local interruption does not establish whether HTTP/FTP/SMTP/file effects were committed.

The resource ledger owns the lifecycle of runtime resources. It must not become a general data outbox or a substitute for destination receipts. A future sink adapter may itself own bounded resources/leases, but publication state must remain separately observable.

## 8. Required conformance scenarios before a sink API is approved

- Workflow fails before any publication call: execution fails; sink remains NOT_STARTED.
- Destination commits and response is received: sink reports COMMITTED with destination receipt.
- Destination commits but response is lost: sink reports UNKNOWN unless a reconciliation query proves commit.
- Destination rejects before commit: sink reports FAILED with reason.
- Cancellation occurs while request is in flight: worker and sink outcomes are reported separately.
- Worker exits while abort callback remains active: resource retirement follows the existing owner/lease/quiescence contract.
- Same idempotent artifact operation is retried: destination returns the same committed result without duplicate data.
- Non-idempotent UNKNOWN operation is retried: automatic retry is blocked.
- Payload is corrupted in transit: destination does not report COMMITTED.
- Queue is full or destination unavailable: bounded backpressure/defer/failure is visible; output is not silently discarded.
- Two workflow executions produce similar content: distinct provenance is retained unless an explicit content-dedup policy proves exact identity.
- Sensitive source metadata is redacted from logs and broadly visible routing metadata.

These are design acceptance scenarios, not tests claimed to exist or pass.

## 9. Decision gates

**GO**
- Preserve stable execution and artifact identity.
- Keep output publication separate from worker/resource completion.
- Preserve NOT_STARTED / IN_PROGRESS / COMMITTED / FAILED / UNKNOWN semantics at external-effect boundaries.
- Keep future sinks optional, versioned and outside Core.
- Retain provenance, integrity and bounded backpressure requirements.

**HOLD**
- Public sink SPI, Java interfaces, artifact manifest schema and receipt schema.
- Durable local outbox/staging implementation.
- Platform-specific SDK or transport integration.
- Any runtime code changes specifically for the future platform.

**STOP**
- Do not add a dependency from Core to the data platform.
- Do not equate worker exit, resource retirement, callback acceptance or transport receipt with durable destination commit.
- Do not blindly retry ambiguous non-idempotent external effects.
- Do not fold domain transaction status into execution or sink state.
- Do not claim a data-platform integration exists until an independently tested adapter and destination contract exist.

## 10. Evidence and scope

This document records an architectural compatibility proposal only. No code, tests, public API or runtime behavior is changed by this PR. Existing issue #358 and PR #403 remain authoritative for their respective implementation scopes and evidence.
