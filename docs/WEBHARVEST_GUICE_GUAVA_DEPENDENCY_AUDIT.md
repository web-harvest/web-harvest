# Guava and Guice Runtime Dependency Audit

**Status:** Bounded repository source/dependency audit; not an approval to remove dependencies or implement a replacement.
**Inspected source revision:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`.
**Related design:** [Plugin Registry and Generation Design](PLUGIN_REGISTRY_GENERATION_DESIGN.md), [Variable, Event and Payload Call-Site Audit](WEBHARVEST_VARIABLE_EVENT_PAYLOAD_CALLSITE_AUDIT.md), [Resource Ownership and Runtime Integration Audit](WEBHARVEST_RESOURCE_OWNERSHIP_INTEGRATION_AUDIT.md).

## 1. Executive findings

1. **Guice is a production Core dependency, not merely an optional host adapter.** Core imports Guice annotations, Injector, Provider, scopes, type listeners, AOP interception and static injection. The runtime plugin construction path consults a static Guice Injector.
2. **The Core public/runtime boundary exposes Guava types.** EventBus appears in public constructors/accessors and is used by both the legacy Harvester event path and newer session/plugin lifecycle path. Guava Monitor is also used for runtime status coordination.
3. **The current no-IoC fallback is partial.** `DefaultPluginBodyExecutor` falls back to reflective no-argument construction when `InjectorHelper.getInjector()` is null, but that does not make the Core module free of Guice linkage or remove Guice annotations and Guice-specific runtime composition.
4. **The existing plugin DI package is a separate reflection-based mechanism.** `PluginDependencyInjector` / `DefaultPluginDependencyInjector` use the project-owned `@InjectDependency` annotation and `DependencyProvider`; the inspected runtime composition does not show it replacing the Guice path.
5. **The POM makes both libraries transitive compile dependencies of Core.** This is an architectural constraint for any host that wants to use Core without Guice or Guava.

## 2. Inspection scope and limits

The recursive source tree at the inspected SHA contained 2,123 entries and 768 Java files: 358 production Java files and 410 test Java files. A text scan was run across all 358 production and all 410 test Java files for Guava/Guice package references and related symbols (including EventBus, Injector, Guice, Provider, TypeLiteral, scopes, Monitor and injection annotations).

All ten repository POMs were inspected: the root POM and the POMs for Core, CLI, IDE, Database, FTP, Mail, WebBrowser, ZIP and Docs.

This is a repository-wide Java/POM scan, but not yet a complete dependency audit. The tree also contains 85 XML/property resource files; their contents and scripts/generated sources have not been scanned for dependency/service metadata. All 15 workflow files were enumerated and the dependency-inventory, reactor/SBOM and OSV workflows were inspected, but no effective-POM/dependency-tree artifact was produced for this docs-only PR: the dedicated dependency-inventory workflow is triggered by POM changes or manual dispatch. The PR OSV workflow generates separate production/test SBOMs, and the reactor workflow generates distribution provenance; their latest artifacts still need inspection. Downstream consumers remain outside repository scope. The scan is text-based and should be followed by symbol-level call-site and binary/API checks before removal decisions.

## 3. Maven dependency facts

### 3.1 Version management versus dependency declaration

The root POM's `dependencyManagement` manages:
- `com.google.guava:guava:33.8.0-jre`
- `com.google.inject:guice:5.1.0`

Dependency management supplies versions; by itself it does not add either library to every module.

### 3.2 Direct dependency declarations

| Module | Guava | Guice | Observed declaration |
|---|---:|---:|---|
| `webharvest-core` | Direct | Direct | Compile scope (no narrower scope declared) |
| `webharvest-cli` | Direct | Direct | Compile scope; also receives both transitively from Core |
| `webharvest-ide` | No direct declaration | No direct declaration | Depends on Core, therefore receives both transitively |
| `webharvest-database` | No direct declaration | No direct declaration | Depends on Core, therefore receives both transitively |
| `webharvest-ftp` | No direct declaration | No direct declaration | Depends on Core, therefore receives both transitively |
| `webharvest-mail` | No direct declaration | No direct declaration | Depends on Core, therefore receives both transitively |
| `webharvest-webbrowser` | No direct declaration | No direct declaration | Depends on Core, therefore receives both transitively |
| `webharvest-zip` | No direct declaration | No direct declaration | Depends on Core, therefore receives both transitively |
| `docs` | No direct declaration | No direct declaration | No direct declaration found |

The CLI's direct declarations appear redundant with its Core dependency, but that should be verified against its packaged/shaded output and intended standalone dependency contract before removal. The root POM also contains `unitils-spring` in test scope; this is not evidence of a production Spring adapter. No second production IoC adapter was identified in the inspected source/POM paths, but alternate injection APIs still need a separate scan.

Because Core declares both libraries at compile scope, consumers of the published Core artifact inherit them as transitive dependencies. Marking them optional without first removing Core's direct type references would not create a working no-Guice/no-Guava runtime.

## 4. Guice production call-site map

### 4.1 Runtime composition and container integration

The principal Guice integration is concentrated in:

- `webharvest-core/src/main/java/org/webharvest/ioc/ScraperModule.java` — bindings for Core services, scopes, EventBus, factories, configuration parser, status holder, type listeners, interceptors and static injection.
- `webharvest-core/src/main/java/org/webharvest/ioc/InjectorHelper.java` — static `Injector` field populated through `requestStaticInjection`; exposes a process/classloader-global accessor.
- `webharvest-core/src/main/java/org/webharvest/ioc/AttributeHolderScope.java`, `ScrapingScope.java`, `ScrapingInterceptor.java`, `TypeMatchers.java`, `EventBusTypeListener.java`, `ScrapingAwareTypeListener.java`, `PostConstructListener.java` — Guice-specific scope, AOP, type-listener and lifecycle integration.
- `webharvest-core/src/main/java/org/webharvest/plugin/DefaultPluginBodyExecutor.java` — `createChildPlugin()` asks the static Injector for the plugin instance and otherwise uses reflective no-argument construction.
- `webharvest-core/src/main/java/org/webharvest/definition/DefaultConfigFactory.java` — injects Guice `Provider<ConfigParser>`.
- `webharvest-core/src/main/java/org/webharvest/definition/XMLConfig.java` and `StreamingXmlParser.java` — use Guice `@Inject` annotations.
- `webharvest-core/src/main/java/org/webharvest/runtime/DefaultHarvest.java`, `DefaultHarvesterFactory.java`, `ScrapingHarvester.java`, `Scraper.java`, `ScraperContext.java`, `BaseTemplater.java` and Core plugin classes — Guice injection/Provider or static-injection call sites.
- `webharvest-database/src/main/java/org/webharvest/ioc/DatabaseModule.java` — optional Database module contributes Guice bindings.
- `webharvest-cli/src/main/java/org/webharvest/cli/WebHarvestCLI.java` and `webharvest-ide/src/main/java/org/webharvest/server/api/ConfigServlet.java`, `webharvest-ide/src/main/java/org/webharvest/server/manager/ExecutionManager.java` — host entry points create Guice Injectors.

### 4.2 Architectural consequences

The static `InjectorHelper` is the strongest container-coupling point: plugin construction is resolved through a globally installed Guice Injector rather than through an explicit runtime-owned factory/provider boundary. This also creates an ambiguity for multiple independent runtimes or host containers in one process.

There are additional static-injection/shared-state seams in the same composition path:
- `ScraperModule.SCRAPER_SCOPE` is a static singleton scope shared by all `ScraperModule` instances.
- `ScraperModule` requests static injection into both `InjectorHelper` and `BaseTemplater`.
- `BaseTemplater.scriptEngineFactory` is a public static injected field used by static template evaluation methods. Tests explicitly construct a Guice Injector to populate it.

These are not merely annotation dependencies. They are runtime state/ownership questions that must be resolved for two independent Core runtimes to coexist without sharing or overwriting each other's script-engine factory, injector or scope state. The separate `CorePluginsRegistrar` / singleton `CorePluginRegistry` initialization path is covered by the registry-generation audit and must be unified with the same runtime-owned model.

The reflection fallback only covers plugin construction when the static Injector is absent. It does not provide equivalent injection for plugin dependencies, configuration factories, HTTP services, scripting services, scopes, lifecycle hooks or the other Guice-integrated paths.

The tests make this limitation observable: `IncludePluginTest.shouldFailClearlyWhenConfigFactoryWasNotInjected()` explicitly expects an Include plugin without Guice injection to fail; `HttpPluginWithDependencyInjectionTest` constructs the plugin through a Guice Injector; `BaseTemplaterStaticInjectionTest` asserts Guice static injection into `BaseTemplater.scriptEngineFactory`; and `DefaultPluginBodyExecutorTest` initializes its execution context through `ScraperModule`. These tests do not establish the required plain-Java end-to-end execution path.

Guice annotations in Core source also mean that merely extracting `ScraperModule` into an adapter module would not remove the Core compile dependency. The annotations and types themselves must move behind a neutral construction/configuration contract or be removed from Core.

## 5. Guava production call-site map

### 5.1 EventBus families

The scan confirms two distinct event families:

- **Legacy Harvester-scoped notifications:** `org.webharvest.events.DefaultHandlerHolder` and `HarvesterEventSink`, with Harvester-to-EventBus association and Guice subscriber registration in `EventBusTypeListener`.
- **Session/plugin lifecycle notifications:** `AbstractPlugin`, `ScraperSessionImpl`, `WebHarvestServiceImpl`, `Scraper`, `ScraperContext`, `DynamicScopeContext`, `DefaultHarvesterFactory` and `ScrapingHarvester`.

Resource subscribers include:
- `webharvest-database/.../ConnectionPlugin.java` and `StandaloneConnectionPool.java`;
- `webharvest-webbrowser/.../WebBrowserPlugin.java`;
- Core `ExitPlugin.java`.

EventBus is present in public/runtime method signatures, not just private implementation fields. Examples include constructors/accessors on `Scraper`, `ScraperSessionImpl`, `WebHarvestServiceImpl`, `ScraperContext` and `DynamicScopeContext`. Replacing Guava therefore requires a deliberate API migration and compatibility adapter.

The two event families must not be collapsed into one merely because they share Guava EventBus. The newer session path is synchronous in-process notification and currently participates in resource cleanup; the event/lifecycle ordering hazards are documented in the Resource Ownership audit.

### 5.2 Other Guava APIs

`com.google.common.util.concurrent.Monitor` is used by `ScraperModule`, `EventBasedStatusHolder` and `RunningStatusGuard` for runtime status coordination. This is separate from EventBus and should be assessed independently; replacing EventBus alone will not remove the Guava dependency.

## 6. A second, project-owned plugin DI abstraction

The Core package `org.webharvest.plugin.di` contains:
- `PluginDependencyInjector`;
- `DefaultPluginDependencyInjector`;
- `DependencyProvider<T>`;
- `@InjectDependency`.

The default implementation uses reflection over fields annotated with `@InjectDependency`, with separately registered singleton instances and providers. It does not use Guice types in its API.

This is not equivalent to the current Guice composition:
- it injects fields on an already-created Plugin instance;
- it does not replace the Guice-backed runtime factories/scopes/interceptors or static Injector access;
- no registration/use of this injector was found in the inspected `ScraperModule` / `DefaultPluginBodyExecutor` composition path.

Its actual production usage and intended status (active SPI, legacy experiment, or unused subsystem) need a dedicated reference/call-site check before deciding to retain, merge or remove it. Do not introduce a third competing DI abstraction.

## 7. Direction for the target architecture

The target should be **container-neutral, not container-less**:

1. Core defines explicit construction/provider, service-access, scope and lifecycle contracts using only JDK and WebHarvest-owned types.
2. A Guice adapter module implements those contracts for Guice hosts; separate adapters may support Spring, another IoC container or no container.
3. Runtime/plugin construction receives a runtime-owned provider/factory explicitly. Remove the static global Injector lookup, static-injected `BaseTemplater` dependency and static scope ownership; avoid resolving host services through a global service locator.
4. Core plugin classes must not require Guice annotations for construction or field injection. Prefer explicit constructor/factory inputs; optional host adapters may translate container bindings into the neutral provider contract.
5. The host owns its general-purpose IoC container unless ownership is explicitly transferred. Runtime/generation shutdown must not close a shared host container.
6. Guava EventBus replacement is a separate migration: define neutral event publication/subscription contracts and preserve synchronous/asynchronous behavior, exception policy, subscriber registration ownership, session scoping and cleanup ordering. A compatibility adapter can preserve old integrations during migration.
7. Replace or isolate Guava Monitor independently, with concurrency/timeout/interrupt behavior characterized.

This direction supports Guice, other IoC containers and no-IoC deployments without making any one container a Core requirement.

## 8. Required follow-up before implementation

- [ ] Complete symbol-level references for every Guice/Guava production and test call site; classify public API, implementation, test-only and documentation-only references.
- [ ] Scan the 85 XML/property resources, scripts, generated sources and service metadata; inspect Maven profiles and shaded/fat-JAR configuration.
- [ ] Obtain the dedicated dependency-inventory workflow artifact (effective POMs and resolved dependency trees) by manual dispatch or a POM-scoped verification run.
- [ ] Inspect the production/test SBOMs and CLI/IDE distribution provenance artifacts from the PR workflows.
- [ ] Check JSR-330/JSR-250 and other alternate injection annotations/APIs separately from Guice-specific imports.
- [ ] Run effective POM/dependency-tree analysis for every module and record direct versus transitive runtime dependencies.
- [ ] Inspect generated SBOMs and packaged CLI/IDE distributions to confirm actual dependency closure and service metadata.
- [ ] Verify whether the project-owned `PluginDependencyInjector` has any production registration or call sites.
- [ ] Add a host-neutral sample runtime and external plugin proof using: Guice, a second IoC container and no IoC.
- [ ] Add two-runtime isolation tests proving no static Injector or global provider leakage.
- [ ] Add compatibility tests for old public APIs that expose Guava EventBus.
- [ ] Only after the above, decide whether Guice and/or Guava should become optional adapter dependencies or be removed from Core.

## 9. Review conclusion

The current Core is **not yet IoC-neutral**. The design can become IoC-neutral, but it requires a real separation of Core contracts from Guice-specific composition and annotations; moving only the Guice module is insufficient. Guava is an independent concern because EventBus and Monitor are also used directly by Core runtime APIs.

No production code or dependency declarations were changed by this audit. This document records bounded source/POM evidence and does not authorize implementation.
