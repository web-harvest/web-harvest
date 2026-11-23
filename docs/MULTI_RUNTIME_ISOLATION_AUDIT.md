# Multi-runtime isolation and static IoC state audit

**Status:** Source-backed blocker analysis; no runtime implementation or test execution.
**Source baseline:** `main` at `1729bdf4620a66686d02605083e27e27d1ab3944` (2026-11-23).
**Purpose:** Establish whether independent embedded WebHarvest runtimes can safely coexist in one JVM while each host retains control of its own IoC container.

## Findings

### 1. The Guice scraping scope is shared process-wide

`ScraperModule` declares one `private static final ScraperScope SCRAPER_SCOPE`. Every module instance binds its `ScrapingScope` annotation to that same object and binds `ScraperScope.class` to the same instance. `ScraperScope` extends `AttributeHolderScope`, whose backing store is an instance `ThreadLocal<Stack<AHT>>`. `ScrapingHarvester.execute()` enters this scope and exits it in `finally`.

This gives thread-local nesting, not runtime isolation. Two injectors do not own distinct scope instances. On one thread, nested runtime A/runtime B calls share the same stack; while B is on top, a provider obtained from A can resolve against B's current holder if it is invoked in that dynamic extent. The stack does not encode or validate runtime identity. Concurrent calls on different threads have separate stacks, but that does not repair same-thread cross-runtime re-entrancy.

`AttributeHolderScope.exit()` pops the holder but does not call `ThreadLocal.remove()` when the stack becomes empty. This leaves an empty Stack attached to pooled threads; it does not by itself prove retention of the popped holder, but is avoidable thread-local residue.

### 2. Static injector injection is last-writer-wins across containers

`ScraperModule.configure()` calls `requestStaticInjection(InjectorHelper.class)`. `InjectorHelper` stores the injected Guice `Injector` in a single static field and exposes it through `getInjector()`. Every injector configured with this module writes the same field. There is no per-runtime key, restoration stack, ownership token, or clearing/close path in the inspected class.

Consequently, constructing a second independent WebHarvest Guice injector overwrites the static injector reference used process-wide. Any code that consults `InjectorHelper.getInjector()` can observe whichever injector most recently performed static injection, not necessarily the injector/runtime that owns the caller. The audit confirms the global mutable seam; it does not claim every active call path currently consults it.

### 3. Script engine injection is also process-global

`ScraperModule` binds `ScriptEngineFactory` as a Guice singleton within each injector, then calls `requestStaticInjection(BaseTemplater.class)`. `BaseTemplater.scriptEngineFactory` is a public static injected field used by static template-evaluation methods.

Each injector therefore writes its own factory into the same process-wide static field. With multiple embedded runtimes, template evaluation can use the last injected runtime's factory, independently of the `DynamicScopeContext` passed to the call. This is a concrete cross-runtime service-selection hazard, not merely a theoretical scope concern.

### 4. These findings compound the existing Guice dependency boundary

Core currently owns Guice-facing runtime classes and module wiring. The above static seams mean that simply giving each host a separate Guice injector is insufficient: the current implementation still shares scope and static service references across those injectors. A host-owned Guice adapter must not create a hidden global injector, and WebHarvest must not close or replace the host's injector.

## Required target

- Each WebHarvest runtime owns an independent composition root, runtime identity, execution-scope coordinator and service/provider registry.
- Core runtime code receives explicit runtime-scoped services/context; no static mutable injected fields or process-global injector lookup.
- Guice integration is an optional adapter that binds into a host-owned injector without owning, replacing, or closing it.
- Runtime close stops admissions, drains active executions/generation leases, and closes only WebHarvest-owned resources. Host-owned services and injector remain host-owned.
- Nested execution and all nested plugin-body executor paths retain the same runtime identity, registry generation, policy, cancellation/deadline and budget.

## Required conformance tests (not run)

1. Create two runtimes with distinct script-engine factories; alternate and nest executions on the same thread and prove each uses its own factory.
2. Repeat with two host-owned Guice injectors; prove no static injector/service reference crosses runtime boundaries.
3. Nest runtime B execution inside runtime A execution; resolve scoped services from both sides and prove each provider sees its own runtime/session holder.
4. Run both runtimes concurrently on separate threads and then on a reused thread pool; prove isolation and no stale scope state.
5. Close/fail runtime A while runtime B is active; prove B's providers, sessions, browser state and host injector remain usable.
6. Verify scope exit on success, exception, interruption and cancellation, including nested scopes and empty-stack ThreadLocal cleanup.

## Evidence (source blobs on the stated `main` baseline)

- `ScraperModule.java`: `d31a5e30b0949c752743123cbcfb4219ba60713e`
- `ScraperScope.java`: `877b943c38994566cc0128629d26de6a3618bcd9`
- `AttributeHolderScope.java`: `11a39c99e517ae9ae1ba09cf69a885b636465b64`
- `ScrapingHarvester.java`: `2af32ca67c235e075c8cc613118814d54e555338`
- `InjectorHelper.java`: `24c114b24a3abc079a9d447b59155e3598f5abe7`
- `BaseTemplater.java`: `dacefb8faec186176148ab2a0cbc5a538941d4c5`

This is source inspection only. No tests were run, no runtime code or POM was changed, and no claim is made that the proposed target contract is already implemented.


## Additional active consumers and listener-retention findings

A follow-up read of the actual Guice type listeners and interceptor confirms that the global injector seam is actively consumed, not just exposed as a utility:

- `ScrapingInterceptor.invoke()` calls `InjectorHelper.getInjector()` and uses that injector to resolve `ScraperScope` and `ScrapingAwareHelper` before entering the scope. Therefore a method intercepted by runtime A can resolve scope/listener services from the most recently injected runtime B.
- `EventBusTypeListener.afterInjection()` calls `InjectorHelper.getInjector().getInstance(EventBus.class).register(i)`. An object created by one injector can consequently be registered with the EventBus obtained from another runtime after the static injector reference has been overwritten.
- `ScraperModule` installs both listeners for each injector; their callback code does not capture the owning injector as an instance field. This makes the global lookup especially consequential for embedded multi-injector use.
- `ScrapingAwareTypeListener` adds detected instances to the injector-scoped `ScrapingAwareHelper`. That helper stores listeners in a mutable `ArrayList`; registration is explicitly marked not thread-safe, and there is no unregister operation. The source documentation itself notes the missing reverse process. Runtime disposal therefore needs listener deregistration and subscriber unregistration, not just injector garbage collection.
- `EventBusTypeListener` registers subscribers but the inspected listener has no corresponding unregister/disposal callback. EventBus ownership and subscriber lifetime need an explicit runtime lifecycle contract.

This expands the required two-runtime tests: invoke an AOP-intercepted Harvester from each runtime after constructing both injectors; create a subscriber through A and verify it registers only with A's EventBus; close A and prove its subscribers/listeners are detached while B remains operational.

Additional source blobs:
- `ScrapingInterceptor.java`: `8de38b972d5c6eb41cc00e9c38ebe0f584e8c9e2`
- `ScrapingAwareTypeListener.java`: `4ed97073d64878be10cd4798c47b56ecd169f1cf`
- `EventBusTypeListener.java`: `3b368eb75ce842f3db6a67b1e9424f644b341824`
- `PostConstructListener.java`: `7d3a23788e96f4507d51f88c6dc5b21f1dc4e49b`
- `DefaultHarvesterFactory.java`: `58bae4e22a82d89dac1c6e63e28ce5f3587e687e`
