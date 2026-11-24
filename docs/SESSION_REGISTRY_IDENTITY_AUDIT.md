# Session registry identity and stale-removal audit

**Date:** 2026-11-24  
**Repository baseline:** `main` at `1729bdf4620a66686d02605083e27e27d1ab3944`  
**Scope:** source and test inspection only. No runtime race, build or test was executed.

## Finding

The current `SessionRegistry` contract treats a session ID as a replaceable map key, but does not bind removal to the identity of the session instance that was originally registered.

Evidence:

- `SessionRegistry.register(ScraperSession)` explicitly documents that a session with the same ID is replaced. (`webharvest-core/src/main/java/org/webharvest/runtime/session/SessionRegistry.java`, blob `aa08f07031c623b545a74e0447143336dfa24812`)
- `SessionRegistryImpl.register()` uses `sessions.put(session.getSessionId(), session)`; `unregister(String)` uses `sessions.remove(sessionId)`. There is no compare-and-remove operation tied to the expected session object or an immutable registration token. (`webharvest-core/src/main/java/org/webharvest/runtime/session/SessionRegistryImpl.java`, blob `8813c995b6f2c7e4b4adb3a4a12d57cbb3c6c124`)
- `SessionRegistryTest.testRegister_ReplaceExisting()` explicitly codifies replacement of the first session by a second object with the same ID. The tests do not cover stale cleanup of the first object after replacement. (`webharvest-core/src/test/java/org/webharvest/runtime/session/SessionRegistryTest.java`, blob `aea82998ecf4ac81a8d02765af3f36465c3f8083`)

This creates a deterministic API-level hazard: after A is registered and B replaces A under the same ID, a delayed cleanup operation holding only A's ID can remove B. The current built-in `WebHarvestServiceImpl` creates UUID IDs and does not itself call `unregister`, so this is not asserted as an already-reproduced service race. It is a flaw in the general registry contract and becomes material for cleanup, retries, custom callers and future session retention policies.

The same key-only model also leaves registration replacement semantically ambiguous: callers cannot distinguish “same logical session updated” from “different execution reused an ID”. The interface describes IDs as unique while simultaneously defining replacement as normal behavior.

## Required contract

1. Session registration must reject duplicate active identities by default; replacement must be an explicit, separately named operation with documented semantics.
2. Removal must be identity-safe: `unregister(expectedSession)` or `unregister(sessionId, expectedRegistrationToken)` must compare the currently registered identity and leave a newer registration untouched.
3. Every admitted execution should carry an immutable registration/execution identity, not just a reusable display/session ID.
4. Bulk clear/removal must be coordinated with admission and task lifecycle; removing a map entry must never be interpreted as cancellation, worker exit, quiescence or resource retirement.
5. Define retention separately from active execution tracking: completed-session history may use a bounded/history store, while the active registry remains authoritative for one live execution identity.
6. Add deterministic tests:
   - A registered, B attempts same-ID registration: default policy rejects without replacing A.
   - If explicit replacement is supported, stale removal for A cannot remove B.
   - Concurrent replace/remove uses compare-and-remove semantics.
   - Reusing a public session ID cannot make an old cancellation/cleanup request target a new execution.
   - Clear/retention operations do not imply worker cancellation or quiescence.

## Dependency and decision boundary

This belongs to #354 (authoritative session/execution identity and lifecycle tracking). It must be resolved before resource leases (#355), subscriber disposal (#356) and generation retirement (#347/#349) depend on registry membership as an ownership signal.

Documentation/audit only. No runtime/API/test/POM change, compatibility deletion, merge or release is authorized by this finding. #346 remains unaccepted; runtime implementation remains STOP.
