# Plugin inventory — evidence baseline

**Status:** Preliminary inventory baseline; not an approved Core membership list.
**Source snapshot:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4` (2026-11-27).
**Refresh note:** Targeted refresh against the 2026-11-27 `main` snapshot covers the plugin SPI, metadata/schema contracts, Core and optional-module registration, package/class scanners, metadata catalog/factory, module POMs, and WebBrowser session lifecycle. This is still not a complete per-plugin or external-embedding consumer audit. Stage 1 descriptor projection and structural validation remain separate from the runtime registration and lifecycle contracts described here.
**Tracking:** [Issue #93 — inventory and approve the built-in plugin set](https://github.com/web-harvest/web-harvest/issues/93).
**Purpose:** Reconcile runtime registration, metadata discovery and module ownership before making any plugin moves.

> **Important:** “Registered by the current Core registrar”, “found by metadata scanning”, “annotated with `@CorePlugin`” and “belongs in the future Core product” are four different claims. This document records the first three only where evidence exists. It does not make the fourth decision.

## 1. Current Core runtime element mappings

[CorePluginsRegistrar.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/definition/CorePluginsRegistrar.java) manually registers 44 element-name mappings in [CorePluginRegistry.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/definition/CorePluginRegistry.java).

| XML element | Implementation class | Current registration evidence |
|---|---|---|
| `call` | `CallPlugin` | CorePluginsRegistrar |
| `call-param` | `CallParamPlugin` | CorePluginsRegistrar |
| `case` | `CasePlugin` | CorePluginsRegistrar |
| `catch` | `CatchPlugin` | CorePluginsRegistrar |
| `constant` | `ConstantPlugin` | CorePluginsRegistrar |
| `def` | `DefPlugin` | CorePluginsRegistrar |
| `else` | `ElsePlugin` | CorePluginsRegistrar |
| `empty` | `EmptyPlugin` | CorePluginsRegistrar |
| `exit` | `ExitPlugin` | CorePluginsRegistrar |
| `file` | `FilePlugin` | CorePluginsRegistrar |
| `function` | `FunctionPlugin` | CorePluginsRegistrar |
| `get` | `GetVarPlugin` | CorePluginsRegistrar |
| `html-to-xml` | `HtmlToXmlPlugin` | CorePluginsRegistrar |
| `http` | `HttpPlugin` | CorePluginsRegistrar |
| `http-header` | `HttpHeaderPlugin` | CorePluginsRegistrar |
| `http-param` | `HttpParamPlugin` | CorePluginsRegistrar |
| `if` | `IfPlugin` | CorePluginsRegistrar |
| `include` | `IncludePlugin` | CorePluginsRegistrar |
| `json-to-xml` | `JsonToXmlPlugin` | CorePluginsRegistrar |
| `loop` | `LoopPlugin` | CorePluginsRegistrar |
| `regexp` | `RegexpPlugin` | CorePluginsRegistrar |
| `regexp-pattern` | `RegexpPatternPlugin` | CorePluginsRegistrar |
| `regexp-result` | `RegexpResultPlugin` | CorePluginsRegistrar |
| `regexp-source` | `RegexpSourcePlugin` | CorePluginsRegistrar |
| `return` | `ReturnPlugin` | CorePluginsRegistrar |
| `script` | `ScriptPlugin` | CorePluginsRegistrar |
| `list` | `ListPlugin` | CorePluginsRegistrar |
| `log` | `LogPlugin` | CorePluginsRegistrar |
| `value-of` | `ValueOfPlugin` | CorePluginsRegistrar |
| `xml` | `XMLPlugin` | CorePluginsRegistrar |
| `set-var` | `SetVarPlugin` | Explicit legacy compatibility mapping |
| `sleep` | `SleepPlugin` | CorePluginsRegistrar |
| `stylesheet` | `StylesheetPlugin` | CorePluginsRegistrar |
| `template` | `TemplatePlugin` | CorePluginsRegistrar |
| `text` | `TextPlugin` | CorePluginsRegistrar |
| `tokenize` | `TokenizePlugin` | CorePluginsRegistrar |
| `try` | `TryPlugin` | CorePluginsRegistrar |
| `while` | `WhilePlugin` | CorePluginsRegistrar |
| `xml-to-json` | `XmlToJsonPlugin` | CorePluginsRegistrar |
| `xpath` | `XPathPlugin` | CorePluginsRegistrar |
| `xquery` | `XQueryPlugin` | CorePluginsRegistrar |
| `xq-expression` | `XQExpressionPlugin` | CorePluginsRegistrar |
| `xq-param` | `XQParamPlugin` | CorePluginsRegistrar |
| `xslt` | `XsltPlugin` | CorePluginsRegistrar |

This is the current manual XML element mapping set, not yet the approved minimal Core product set. For example, HTTP, file I/O, scripting and transformation capabilities need explicit dependency/semantics review before final classification.

The registrar source comments that `VarPlugin` was removed in favor of `DefPlugin`. The separate `set-var` alias is deliberately retained for compatibility.

## 2. Optional Maven modules — source-path baseline

The current source tree contains these plugin-named implementation classes outside `webharvest-core`:

| Maven module | Plugin implementation classes visible in source tree | Preliminary product status |
|---|---|---|
| `webharvest-ftp` | `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin`, `FtpListPlugin` | Outside Core; optional integration (review direction 2026-11-27); retain container/child compatibility audit |
| `webharvest-database` | `DatabasePlugin`, `ConnectionPlugin` | Outside default Core; optional-integration candidate; inspect connection/runtime support classes |
| `webharvest-mail` | `MailPlugin` | Outside Core; optional integration (review direction 2026-11-27) |
| `webharvest-webbrowser` | `WebBrowserPlugin` | Optional browser-rendering integration; HtmlUnit remains outside Core |
| `webharvest-zip` | `ZipPlugin`, `ZipEntryPlugin` | Optional extension; PR #187 adds an explicit `ZipPluginRegistrar`, scoped execution-context stream and nested-archive runtime evidence; remains outside default Core |

The database module also contains the following runtime/support classes, which are part of the module boundary audit even though they are not plugin entry points by name:

- `org.webharvest.ioc.DatabaseModule`
- `org.webharvest.runtime.database.ConnectionFactory`
- `org.webharvest.runtime.database.ConnectionProxy`
- `org.webharvest.runtime.database.DefaultDriverManager`
- `org.webharvest.runtime.database.DriverManager`
- `org.webharvest.runtime.database.JNDIConnectionFactory`
- `org.webharvest.runtime.database.StandaloneConnectionPool`

Exception classes are not counted as user-facing plugin implementations.

**Agreed membership direction (2026-11-27):** FTP, Mail and WebBrowser are outside Core and must be delivered as opt-in integrations. Their current `@CorePlugin` annotations are historical/discovery metadata, not product-boundary decisions or permission for default activation. WebBrowser remains an optional HtmlUnit-backed integration because browser automation is specialized, has a substantial engine dependency and a distinct resource lifecycle; ordinary HTTP acquisition remains available without it. Do not introduce a generic Core browser capability prematurely. Revisit that neutral contract only if a second browser provider or a demonstrated Core-wide use case requires it.

### Current descriptor and dependency-contract findings (2026-11-27)

- `@CorePlugin` exposes one `elementName` and one namespace. `PluginMetadata` also exposes one namespace/element-name pair. Neither is sufficient to describe multiple XML aliases for one executable plugin as an explicit first-class binding set.
- `PluginMetadata` has a `String[] getDependencies()`, but this is not a module dependency manifest. The inspected FTP, Mail and WebBrowser metadata implementations all return an empty array while reporting `isCorePlugin() == true`; their Maven POMs independently declare third-party dependencies.
- `PluginMetadataCatalog.collectDependencies()` combines metadata dependencies with `PluginConfigurationSchema.getRequiredParents()` and semantic required-parent types. This projects XML nesting constraints into the same `dependencies` list, conflating grammar constraints with dependency resolution.
- `PluginClassScanner` filters on `@CorePlugin` and its fallback class list includes optional modules. `PluginPackageScanner` uses the same annotation as its opt-in discovery filter. Neither scanner represents product membership or an explicit host activation decision.
- `PluginMetadataCatalog` loads discovered classes with `Class.forName(className)` and constructs plugin instances to read metadata. It is a metadata projection with reflective class initialization/instantiation, not a side-effect-free module manifest or runtime generation.
- `DefaultPluginFactory` uses a mutable legacy `PluginRegistry` as its class lookup path, then reflectively creates a fresh instance. Its fallback metadata is inferred from the Java class name and supplies no declared module/capability requirements.

These findings support a clean separation between module requirements, runtime capability requirements, plugin-composition dependencies, XML content constraints, and Maven/package dependencies. Do not repair the ambiguity by merely renaming the existing `dependencies` field.

## 3. Discovery and metadata discrepancies

[PluginClassScanner.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginClassScanner.java) declares six scan roots: Core, FTP, mail, database, ZIP and webbrowser. Its fallback list contains 53 class names: 43 Core-package names and 10 optional-module names.

This differs from the 44 mappings in `CorePluginsRegistrar`:
- the fallback list omits the `SetVarPlugin` compatibility alias;
- the fallback list contains the 10 optional module plugin classes;
- the scanner is a class/metadata discovery path, not the same registry as `CorePluginRegistry`.

The Core package also contains plugin-related classes not present in the 44-entry manual mapping (for example `BodyProcessorPlugin`, `ConfigPlugin` and `VarPlugin`). Their abstract/support/legacy/unused status must be confirmed before they are counted as workflow processors.

The `@CorePlugin` annotation and `PluginMetadata.isCorePlugin()` are not reliable product-boundary signals by themselves. A source spot-check found FTP classes annotated with `@CorePlugin` and reporting `isCorePlugin() == true` while they live in `webharvest-ftp`; ZIP plugin metadata reports `false`. The exact intended semantics of these flags must be audited and reconciled.

## 4. Concrete investigation items

- [ ] Reconcile the 44 runtime mappings, 53 fallback class names, actual annotation scan results, metadata catalog and generated XML/schema/documentation outputs.
- [ ] Establish which mechanism is authoritative for runtime execution versus metadata discovery and semantic tooling.
- [ ] Inspect every plugin implementation and support class in all current modules.
- [ ] Record each element name, alias, namespace, metadata ID, `isCorePlugin` flag and descriptor identity.
- [ ] Compare declared configuration/schema and execution/variable semantics with implementation and runtime tests.
- [ ] Map tests by plugin: unit, deterministic runtime integration and network/service dependent.
- [ ] Verify compatibility obligations, especially `set-var`.
- [x] Replace the ZIP parent-instance lookup with a scoped `DynamicScopeContext` stream handoff; verify nested ZIP scope restoration and orphan-entry rejection in PR #187.
- [ ] Produce a reviewed Core membership and extension migration table.

## 5. Evidence and limitations

This baseline was produced from the Git tree and targeted source reads on the stated `main` commit. It does not yet include a complete per-plugin read of configuration schemas, runtime call sites, dependencies, semantic descriptors and tests. No class has been moved or removed, and no Core membership decision is implied.

The authoritative work item is [Issue #93](https://github.com/web-harvest/web-harvest/issues/93). Update this inventory as each implementation is inspected and record source/test evidence rather than relying on historical plugin counts.


## 6. Runtime registration is not the same as metadata discovery

A deeper call-path inspection found a more important architectural gap than the initial count mismatch:

- `ScraperModule.configure()` calls `CorePluginsRegistrar.registerCorePlugins()`. That registrar populates `CorePluginRegistry` with the 44 built-in element mappings.
- `DefaultPluginBodyExecutor` resolves child execution by calling `CorePluginRegistry.getPluginClass(childConfig.getElementName())`. If the mapping is absent, it logs that the plugin class was not found and does not execute that child.
- `PluginPackageScanner` can scan arbitrary packages and register `@CorePlugin` classes into that same runtime registry. Its API is covered by dedicated tests, but the default `ScraperModule` wiring does not instantiate or invoke it. The former per-module `CorePluginsPostProcessor` wiring is commented out as removed.
- Separately, `PluginClassScanner` scans six module package roots and feeds `PluginMetadataCatalog`. The catalog creates descriptors for discovered classes; this does not register them in `CorePluginRegistry` and does not make XML execution available.
- `StreamingXmlParser` currently sets `elementInfo = null`, explicitly skips the old resolver lookup, and creates `PluginConfigurationTree` nodes with a null `pluginClass`. Its comments label this as a POC fallback and say direct `PluginRegistry` lookup is future work. Actual child execution later relies on `CorePluginRegistry` name lookup.

**Consequence:** metadata visibility in the IDE/catalog is not proof that a plugin can execute from XML. A plugin may be discoverable by `PluginClassScanner` and still be absent from the runtime registry. The current package scanner is a usable building block, but the default application does not yet provide the automatic, opt-in extension registration experience described by the target architecture.

Evidence:
- [ScraperModule.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/ioc/ScraperModule.java)
- [CorePluginsRegistrar.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/definition/CorePluginsRegistrar.java)
- [DefaultPluginBodyExecutor.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/plugin/DefaultPluginBodyExecutor.java)
- [PluginPackageScanner.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/plugin/scanner/PluginPackageScanner.java)
- [PluginClassScanner.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginClassScanner.java)
- [PluginMetadataCatalog.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginMetadataCatalog.java)
- [StreamingXmlParser.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/definition/StreamingXmlParser.java)

## 7. Optional-module implementation and test evidence

The following module boundaries are confirmed by each module POM: every optional module depends on `webharvest-core`; Core does not declare these optional modules as dependencies. The modules currently add their integration library as follows:

| Module | Plugin XML elements | Direct integration dependency | Test evidence in source tree | Preliminary assessment |
|---|---|---|---|---|
| `webharvest-ftp` | `ftp`, `ftp-get`, `ftp-put`, `ftp-list` | Apache Commons Net | `FtpPluginTest` includes tests for parent and child operations; separate live/integration test exists | Strong optional-integration candidate |
| `webharvest-database` | `database`, `connection` | Commons DBCP | Unit tests for both plugin classes and driver manager; XML example resource | Strong optional-integration candidate |
| `webharvest-mail` | `mail` | Commons Email | Unit tests, exception tests, bug regression and integration test | Strong optional-integration candidate |
| `webharvest-webbrowser` | `web-browser` | HtmlUnit | Unit, metadata, regression and integration tests | Strong optional-integration candidate |
| `webharvest-zip` | `zip`, `zip-entry` | JDK ZIP APIs only | One Failsafe integration test inspects outer/nested archive bytes; one unit test rejects an orphan entry; path-filtered optional-module CI | Optional extension; behavior is now tested, but it remains opt-in and outside default Core |

The FTP child classes do not have separate test source files, but `FtpPluginTest` contains explicit `testFtpGetPlugin`, `testFtpPutPlugin` and `testFtpListPlugin` methods. Do not infer missing coverage solely from file-name matching.

### ZIP execution contract repaired in PR #187

The former resource-exists placeholder has been replaced with a real XML-loader/Harvester integration test. It inspects the outer archive entries and a nested archive's contents, proving that the active stream is restored after the inner ZIP completes. A separate unit test verifies that an orphan `<zip-entry>` is rejected.

`ZipPlugin` now owns its stream as a local variable and exposes it only through a scoped `DynamicScopeContext` value while its body executes. `ZipEntryPlugin` resolves that active stream from the context rather than looking up a parent plugin instance. Nested ZIP processors create nested scopes; the outer stream is restored when the inner scope exits. Try-with-resources closes the stream on success or failure. ZIP remains an optional extension and is not added to the 44 default Core mappings.

## 8. Core implementation inventory and test evidence

The current source tree contains **47** Java classes named `*Plugin.java` in `webharvest-core/.../plugin/core`. The manual runtime registrar exposes **44 XML element mappings**. The 47-class count is not a product count.

A source-level comparison of the 47 class paths with the Core test tree found a matching test filename for every class, but this is only a navigation aid, not proof of meaningful coverage or passing tests. Several important distinctions are now source-confirmed:

| Class | Annotation / metadata evidence | Runtime registrar | Test evidence | Preliminary status |
|---|---|---|---|---|
| `BodyProcessorPlugin` | `@CorePlugin(elementName="body-processor")`; class comment describes an internal body execution processor; metadata tests exist | Not registered | Dedicated unit test; a legacy XML example also contains `<body-processor>` | Internal helper by documented intent, but the example is not evidence of supported execution; verify or remove/repair the stale example and check internal call sites |
| `ConfigPlugin` | `@CorePlugin(elementName="config")`; legacy root-container implementation with metadata and direct unit tests | Not registered | Unit, XML and integration tests exist in tree | Legacy root implementation; current runtime handles `<config>` through `RootConfigurationExecutor`, not plugin registration |
| `VarPlugin` | `@CorePlugin(elementName="var")`; explicitly deprecated in favor of `<def>` | Not registered | Legacy unit tests still exist | Deprecated compatibility candidate; not currently enabled by default runtime |
| `SetVarPlugin` | `@CorePlugin(elementName="set-var")`; deprecated for new configurations, retained for old XML | Registered as `set-var` | Dedicated tests and an explicit registrar contract test | Public legacy compatibility element; preserve |
| Remaining registrar classes | Manual element mapping is authoritative for current default runtime | Registered | Per-class test files exist in the test tree | Current Core runtime candidates; final product membership still requires dependency/security/semantic review |

### A key inconsistency in the metadata audit

`AllPluginsMetadataAuditTest` builds its own explicit list of Core classes and explicitly excludes `VarPlugin` and `SetVarPlugin`, describing both as removed in favor of `DefPlugin`. That test's list also excludes `BodyProcessorPlugin` and `ConfigPlugin`. However, the runtime registrar still registers `SetVarPlugin`, and `SetVarCompatibilityRegistrationContractTest` explicitly asserts that `set-var` remains mapped. The test audit therefore describes a *new-configuration metadata cohort*, not the actual enabled runtime set. Its comment/list must not be used as the authoritative runtime inventory.

The `@CorePlugin` annotation Javadoc says annotated plugins are always available Core functionality. That statement is too broad for current behavior: `BodyProcessorPlugin` and deprecated `VarPlugin` are annotated but absent from the default registrar; `ConfigPlugin` is also annotated but the root `<config>` element is handled by `RootConfigurationExecutor`, not ordinary child-plugin dispatch. A test-resource XML example nevertheless includes `<body-processor>`, so that example must be checked before treating the element as either supported public XML or dead documentation. FTP implementations also carry the annotation while living in an optional module. The annotation currently mixes at least three concepts: implementation lineage, IDE metadata/palette classification, and runtime/product membership. These must become separate fields or descriptor decisions in the future SPI.

### Core dependency and risk classification still needed

Even for the 44 current runtime mappings, membership in a minimal engine is not yet approved. In particular:
- Network and extraction plugins (`HttpPlugin`, `HtmlToXmlPlugin`, JSON/XML transforms, XPath/XQuery, XSLT) need a dependency/license/security review before declaring them indispensable Core.
- `ScriptPlugin` needs an explicit policy for script engines, language support and execution sandboxing.
- `FilePlugin` needs a documented filesystem access/security boundary.
- `ConfigPlugin` and `BodyProcessorPlugin` need internal-vs-public contracts resolved.
- Legacy `set-var` must remain available for compatibility even if it is not recommended for new configurations.
- `VarPlugin` should remain out of the default runtime unless a deliberate compatibility requirement is established; its deprecated class and tests alone do not imply support.

## 9. Core Maven dependency footprint — current baseline

The current `webharvest-core/pom.xml` declares the following non-test dependencies. This is a declared module footprint, not yet a per-plugin reachability/dependency map; transitive dependencies are not expanded here.

| Dependency | Scope | Initial review question |
|---|---|---|
| SLF4J API | compile | Core logging contract; likely foundational |
| Guice | compile | Is DI a permanent Core runtime requirement or an implementation detail? |
| CGLIB | compile | Identify the exact Core features requiring class generation |
| JSON (`org.json`) and Gson | compile | Which JSON/XML conversion and metadata paths require each library? |
| TagSoup and Jsoup | compile | HTML parsing/repair; consider whether HTML extraction is a separately selectable capability |
| Apache Tika Core | compile | Identify the actual MIME/type-detection call sites and whether optional |
| Commons Lang, Commons IO, Commons Collections | compile | Trace shared utility use and determine whether any can be removed/replaced with JDK APIs |
| Reflections | compile | Dynamic package scanning is not the authoritative runtime registration path today; evaluate whether it belongs in minimal Core |
| Saxon (base artifact plus `dom` classifier) | compile | Verify both artifacts are required and map them to XSLT/XQuery call sites |
| BeanShell | compile | Script engine is part of the default runtime path; define scripting capability and security boundary |
| Groovy | runtime | Optional script language; should not necessarily burden every Core consumer |
| Rhino | runtime | JavaScript scripting engine; define supported runtime capability and lifecycle |
| Guava | compile | Trace remaining usage; assess whether JDK replacements are practical |
| Apache HttpClient 5 | compile | HTTP is a major Core capability; review dependency and transport boundary |
| `javax.annotation-api` | compile | Verify required annotations and Java baseline |

### Dependency review implications

- The current Core is not a tiny execution kernel: it bundles HTTP, HTML parsing, XSLT/XQuery, scripting engines and dynamic classpath scanning dependencies alongside execution primitives.
- Do not remove these dependencies based on POM inspection alone. First map each dependency to concrete classes, call sites, tests and public XML behavior.
- The eventual minimal Core may need capability modules (for example HTTP, HTML extraction, transformations, scripting) that remain first-party but are independently selectable. This is a design candidate, not an approved module split.
- Saxon is declared as both the base artifact and a `dom` classifier variant; this is not an exact duplicate. Verify which classes need each artifact before changing the dependency declarations.
- The optional-module extraction decision should be based on the combined public XML contract, dependency graph, licensing/security, and runtime integration tests—not module names alone.

## 10. Required follow-up before calling the plugin platform complete

1. Define one authoritative runtime registration API and remove the split-brain between metadata-only scanning and execution registry registration.
2. Make extension registration opt-in and module/classloader aware; never require Core to depend on optional integrations.
3. Ensure the same discovered descriptor drives XML element resolution, metadata catalog, schema/validation and semantic resolution.
4. Add duplicate ID/element/namespace conflict checks and deterministic ordering.
5. Add integration tests proving: plugin jar on classpath -> discovered -> registered -> XML parsed -> instantiated with DI -> executed -> lifecycle/resources closed.
6. Add an out-of-tree test plugin Maven project before stabilizing the SPI or extracting the company extensions repository.
7. Repair ZIP parent resolution and replace the current resource-existence test with an actual execution/output assertion.


## 11. Follow-up verification — BodyProcessor execution path and ZIP defect

Source snapshot: `main` at `c4357d8fa531d5ba536b5d217e149de8e1bd8262` (2026-11-16).

### BodyProcessorPlugin is not the framework's current body executor

A follow-up call-path check narrows the earlier uncertainty:

- `AbstractPlugin.executeBody(context)` directly constructs `DefaultPluginBodyExecutor` and invokes `executeBody(configuration, context)` when child configuration exists.
- No call from this execution path constructs or invokes `BodyProcessorPlugin`.
- `CorePluginsRegistrar` does not map `body-processor` into `CorePluginRegistry`.
- `BodyProcessorPluginTest` tests metadata, schema and initialization; it does not exercise `doExecute` or prove an execution call path.
- The only matching XML example is `src/test/resources/integration-tests/new_plugins/42_bodyprocessor_plugin_example.xml`; no dedicated test class was found that loads and executes that fixture.

Current classification: legacy/internal metadata artifact, not a current default-runtime XML capability. The fixture should be repaired into a genuine integration test or removed as stale. Do not count this class as an active Core element on the basis of its annotation or fixture alone.

### ZIP parent resolution is a confirmed blocker

PR #187 addresses the defect tracked in [Issue #97 — ZIP entry plugin cannot resolve its parent ZIP plugin](https://github.com/web-harvest/web-harvest/issues/97). The active archive is now passed through a scoped execution context, the example is executable, the integration test inspects real archive contents (including nested ZIP), and the orphan-entry path has a direct negative test. The optional ZIP workflow runs `mvn -B -ntp -pl webharvest-zip -am verify` on relevant path changes. ZIP remains optional; this is not a Core membership change.

Evidence links:
- [AbstractPlugin.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/plugin/AbstractPlugin.java)
- [BodyProcessorPlugin.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/main/java/org/webharvest/plugin/core/BodyProcessorPlugin.java)
- [BodyProcessorPluginTest.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/test/java/org/webharvest/plugin/core/BodyProcessorPluginTest.java)
- [BodyProcessor XML fixture](https://github.com/web-harvest/web-harvest/blob/main/webharvest-core/src/test/resources/integration-tests/new_plugins/42_bodyprocessor_plugin_example.xml)
- [ZipEntryPlugin.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-zip/src/main/java/org/webharvest/plugin/zip/ZipEntryPlugin.java)
- [ZipPluginIntegrationTest.java](https://github.com/web-harvest/web-harvest/blob/main/webharvest-zip/src/test/java/org/webharvest/plugin/integration/ZipPluginIntegrationTest.java)

These findings refine the inventory only. No runtime code, module boundaries or dependency versions were changed.

## 12. Runtime registry contract status (refreshed 2026-11-17)

The exact registration inventory is now protected by
[CorePluginsRegistrarContractTest.java](../webharvest-core/src/test/java/org/webharvest/definition/CorePluginsRegistrarContractTest.java),
merged in PR #173. The test verifies the exact 44 XML element names, each expected
implementation class, the legacy `set-var` alias, and the absence of unexpected
mappings. It does not claim that all 44 mapped classes are independently executed
plugin capabilities.

The following registry design gaps remain source-confirmed on the current
`main` snapshot:

- `CorePluginRegistry.registerMapping(...)` uses `ConcurrentHashMap.put`, so
  duplicate element names silently overwrite an earlier mapping. No explicit
  duplicate/override policy is enforced.
- The `processorClass` argument to `registerMapping(Class processorClass,
  Class<? extends Plugin> pluginClass, String elementName)` is ignored; only
  element name to plugin class is stored. The signature suggests a relationship
  the registry does not preserve.
- `CorePluginsRegistrar` keeps a static `initialized` flag separately from the
  singleton registry. Calling `CorePluginRegistry.clear()` without
  `CorePluginsRegistrar.reset()` can leave the registry empty while a later
  `registerCorePlugins()` call returns early.

These are design requirements for Issue #94, not permission to patch the registry
in isolation. The target architecture needs deterministic duplicate handling,
accurate descriptor identity, and lifecycle-safe registry ownership/isolation.

### Registration inventory versus executable behavior

PR #173 establishes the 44-name registration contract. The source audit identifies three declarative child nodes and one dual-use XML mapping. See the [44-row Core XML Runtime Evidence Matrix](CORE_PLUGIN_RUNTIME_EVIDENCE.md) for mapping-by-mapping test evidence.

| XML name | Current source behavior | Evidence classification |
|---|---|---|
| `xml` | `XMLPlugin.doExecute()` executes child body or reads literal `_textContent`, returning a `NodeVariable`. The direct Failsafe test loads CDATA markup through the XML loader, stores it, then consumes it through XPath. Within `<xslt>`, the parent reads child `_textContent` directly. | Dual-use: standalone executable wrapper plus declarative XSLT input node; PR #183 merged and its runtime test/main CI are green. |
| `stylesheet` | `XsltPlugin` reads child `_textContent` directly. `StylesheetPlugin.doExecute()` currently returns `EmptyVariable.INSTANCE`. | Declarative XSLT configuration node; no independent executable capability demonstrated. |
| `xq-expression` | `XQueryPlugin` reads child `_textContent` directly. | Declarative XQuery expression node. |
| `xq-param` | `XQueryPlugin` parses the child name/type/text itself; nested value-producing children are executed by the parent. | Declarative XQuery parameter node. |

Evidence: [XsltPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/XsltPlugin.java),
[XQueryPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/XQueryPlugin.java),
[StylesheetPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/StylesheetPlugin.java),
and [XMLPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/XMLPlugin.java).
The XSLT/XQuery integration tests prove parent transformation and parameter semantics. PR #183 now also proves the standalone `xml` wrapper path: literal CDATA becomes a runtime value consumable by downstream XPath.

**Compatibility rule:** preserve the existing XML names and forms. The future
descriptor/registry contract should distinguish executable plugin identities from
declarative configuration-node identities. Do not remove or rename these nodes
as part of the reliability work.

## 13. Core XML integration-test evidence quality (refreshed 2026-11-17)

The six principal deterministic XML integration suites were re-read from current
`main` after PR #183 at `4a4dcc609f302decdb3fd9f1e3da49790f01fc18`. The earlier placeholder
counts in this document are historical and no longer describe the current files.

| Test class | Current test methods | `assertTrue(true)` | Broad catch of Exception / RuntimeException / Throwable | Observable behavior examples |
|---|---:|---:|---:|---|
| `CorePluginsIntegrationTest` | 9 | 0 | 0 | Exact text, definition, legacy set-var, get, file output, list contents and value-of results; Sleep duration lower bound. |
| `ControlFlowPluginsIntegrationTest` | 7 | 0 | 0 | If/Else selected output; Loop/While ordering and counts; Try/Catch branch state and continuation; Case output. |
| `AdvancedPluginsIntegrationTest` | 3 | 0 | 0 | Exact variable/body values and rendered template output. |
| `HttpPluginsIntegrationTest` | 2 | 0 | 0 | Deterministic stub response URL/origin and stored response values. |
| `XmlTransformationPluginsIntegrationTest` | 8 | 0 | 0 | XPath selection, HTML-to-XML, JSON-to-XML, XML-to-JSON, XSLT, XQuery output, empty-sequence behavior and standalone XML wrapper/CDATA. |
| `TextProcessingPluginsIntegrationTest` | 3 | 0 | 0 | Regexp match/capture list, ordered tokenization, JavaScript result. |
| **Total** | **32** | **0** | **0** | Source-level assertion review; not a claim of exhaustive 44-class coverage. |

The six source files were inspected individually. The counts above are static
source observations, not test execution results. In particular, zero placeholder
assertions and zero broad catch-and-pass blocks do not prove every edge case or
every registered implementation is covered. Two additional XML-loader/Harvester
suites cover XQuery parameters: `XQueryParameterTypesTest` (8 methods) and
`XQueryComplexTypesIntegrationTest` (3 methods), including typed scalars,
integer/double sequences and `node()*` parameters with arithmetic/output assertions.

The XML integration base uses a Guice-backed `Harvest` / `Harvester` execution
path and deterministic HTTP stubs. These suites therefore provide meaningful
runtime evidence for the behaviors named in the table; live-network behavior
remains separately classified and is not inferred from stubbed tests.

### Additional runtime contracts merged

- **Log** — PR #170: inline body, stdout/stderr routing and return behavior.
- **Exit** — PR #171: true/false conditions and propagation through Loop, While,
  Call and Try.
- **Empty** — PR #172: nested side effects, observable empty result and parent
  continuation.
- **44-name registrar** — PR #173: exact mapping names and implementation
  classes, including legacy `set-var`.
- **Include** — PR #174: relative include through XML loading and the standard
  Harvester path; included state is visible and parent execution continues.
- **XQuery empty sequence** — PR #181: valid `()` expression returns an empty `ListVariable`; the parent configuration continues.
- **XML wrapper** — PR #183: literal CDATA is returned through the XML loader and consumed by downstream XPath; raw nested tags are explicitly documented as child plugin configurations.

Each of PRs #170–#174 passed its exact-head three-job CI run, including the full
Maven verify job, Checkstyle and JaCoCo report validation. Main push run #537
([Actions run 44158373034](https://github.com/web-harvest/web-harvest/actions/runs/44158373034))
on `d9a1466953a82afc8b6f68b86b4ddb04bde5fe37` passed all three jobs after PR #181.
PR #183 exact-head run [44160973377](https://github.com/web-harvest/web-harvest/actions/runs/44160973377)
and post-merge main push run [44161596727](https://github.com/web-harvest/web-harvest/actions/runs/44161596727)
on `4a4dcc609f302decdb3fd9f1e3da49790f01fc18` also passed all three jobs. PRs #182
and #184 changed documentation only. Root JaCoCo minimums remain 0.80 instruction
coverage and 0.80 branch coverage.

### Unit-test false-green cleanup — PR #176 merged

A source scan of the 43 conventional `*PluginTest.java` paths that resolved for
the 44 Core registrar mappings (the expected `ScriptPluginTest` path did not
resolve) found 35 unconditional `assertTrue(true)` placeholders in 15 files.
The other 28 inspected files contained none.

PR #176 was merged as
[`5611642`](https://github.com/web-harvest/web-harvest/commit/5611642ddd76daae5e562bea5680fb87c973e6ca).
It removed all 35 placeholders:
- initialization tests now assert the plugin is initialized;
- valid-configuration tests call validation directly, so an unexpected
  exception fails the test naturally;
- Case and Text schema tests assert that undeclared attributes are absent;
- 11 non-executing/context-only or explicitly unimplemented Def, Template and
  XQuery test methods were removed rather than counted as behavioral evidence.

This is a test-honesty correction, not a claim that 35 new behaviors are covered.
XQuery typed parameter binding and arithmetic are covered by `XQueryParameterTypesTest`
(8 XML-loader/Harvester integration methods) and `XQueryComplexTypesIntegrationTest`
(3 methods covering typed scalar, sequence and node parameters); PR #181 covers
empty-sequence behavior. These suites sit outside the six principal suite count.
The per-plugin matrix continues to separate metadata/schema smoke tests from
observable runtime behavior.

## 14. Optional-module execution and dependency evidence (2026-11-17)

A targeted source/POM/test audit covered Database, FTP, Mail, WebBrowser and ZIP. This is
evidence for the module boundary review in Issue #93, not an approved Core
membership decision.

| Module | Direct runtime dependency | Source-confirmed side effects / lifecycle | Current test evidence |
|---|---|---|---|
| `webharvest-ftp` | `commons-net:commons-net` | `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin` and `FtpListPlugin` connect to remote FTP servers, authenticate, and read/write/list remote files. The connection-oriented implementations disconnect in `finally`. Requires external network, credentials and remote filesystem access. | `FtpPluginIntegrationTest` only checks that the example XML resource exists. `FtpPluginTest` mostly covers metadata/schema/initialization; its execute and invalid-action tests accept a broad `Exception` without asserting the exact failure. No deterministic FTP server execution contract was found in the inspected tests. |
| `webharvest-mail` | `org.apache.commons:commons-email` | `MailPlugin` creates SimpleEmail/HtmlEmail and calls `email.send()`; this is an external SMTP/network side effect that can deliver real email. | `MailPluginIntegrationTest` only checks that the example XML resource exists. No deterministic SMTP execution contract was found in the inspected integration test. |
| `webharvest-webbrowser` | `org.htmlunit:htmlunit:5.8.0` | `WebBrowserPlugin` loads remote URLs and executes JavaScript/AJAX. Current code keeps pages in a static map keyed by scraper session ID, with per-session `BrowserSessionState`; it registers for session completed/failed/cancelled events and closes clients on those terminal events. A weak-key context fallback exists when no `ScraperSession` is attached, and `closeAllPages()` remains a process-wide shutdown hook. This is better isolated than a single global default page, but the static bookkeeping and fallback lifecycle still need concurrency/cleanup tests. | Unit tests mainly cover metadata, initialization and unsupported actions; the XML integration test asserts only a non-null execution context, not rendered page content, JavaScript results or cleanup. |
| `webharvest-zip` | No additional direct runtime library beyond Core/JDK | `ZipPlugin` owns a local `ZipOutputStream` and exposes it only in a scoped `DynamicScopeContext` value; `ZipEntryPlugin` reads it from that scope. Nested ZIP scopes restore the outer stream and try-with-resources closes it. | `ZipPluginIntegrationTest` executes the XML example and inspects outer and nested archive contents; `ZipEntryPluginTest` rejects an orphan entry. Optional ZIP CI runs Core+ZIP verify. See PR #187 / Issue #97. |
| `webharvest-database` | `commons-dbcp:commons-dbcp`; no JDBC driver declared by the module | `DatabasePlugin` executes SQL via `PreparedStatement`, supports inline driver/url and a named `connection` path; `ConnectionPlugin` stores named connection factories in context. Named mode is currently blocked by unconditional driver/url schema requirements. Metadata/annotation names and namespaces also disagree. | 33 unit tests (12 DatabasePlugin, 10 ConnectionPlugin, 7 exception, 4 driver-manager); no successful SQL result assertion or executed XML integration fixture. Core CI excludes this optional module. Follow-up: Issue #185. |

The inspected Database, FTP, Mail, WebBrowser and ZIP classes carry `@CorePlugin`
annotations despite being in optional Maven modules. Database/FTP unit tests also
assert `metadata.isCorePlugin() == true`. Therefore the annotation and
`PluginMetadata.isCorePlugin()` flag are not reliable product-boundary signals.

**WebBrowser re-check (2026-11-27):** the earlier static-global-state concern is too broad for the current source. Browser pages are grouped by scraper session ID and the state subscribes to terminal session events for cleanup; only the no-session compatibility path uses a weak-key context map. The remaining risks are whether every session reliably emits a terminal event, whether the no-session fallback closes resources deterministically, and whether the static maps are safe across multiple runtime instances with colliding session IDs. The module POM currently depends on HtmlUnit 5.8.0. The CLI POM directly includes this and all other optional modules in its shaded distribution, so module-level optionality does not currently mean the CLI distribution is minimal.

**Agreed architecture direction (2026-11-27):** keep WebBrowser outside Core as an opt-in browser-rendering integration. JavaScript-capable browser execution is a specialized acquisition capability with a substantial engine dependency and a distinct resource lifecycle; ordinary HTTP acquisition remains available without it. Do not introduce a generic browser/rendering capability into Core now. Reconsider a neutral contract only if a second browser provider or a demonstrated baseline requirement justifies it. Do not promote the current HtmlUnit-backed implementation itself into Core merely because it is annotated `@CorePlugin` or reports `isCorePlugin() == true`.

### Database extension: concrete support gaps (Issue #185)

- `DatabasePlugin` is annotated with `elementName="database"`, but its metadata
  reports `getElementName() == "db"`; its schema unconditionally requires
  `driver` and `url`, even though `doExecute()` supports `connection="..."`.
  The documented named-connection form therefore fails validation before SQL
  execution.
- `ConnectionPlugin` metadata declares the database namespace, while the checked-in
  `database_plugin_example.xml` places `<connection>` under the Core namespace.
- `DatabasePluginTest` has 12 methods and only two calls to `plugin.execute()`;
  those exercise error paths, not successful SQL. `ConnectionPluginTest` has 10
  methods and no `plugin.execute()` call. The remaining seven exception tests and
  four driver-manager tests do not prove SQL behavior.
- The `MockDBDriver.jar` fixture is used only by `DefaultDriverManagerTest` to verify
  driver registration/loading; that test never calls `connect()` or executes SQL.
  The module POM declares no H2 or other JDBC driver, and the XML example is not
  executed by a Java test.
- The README claims 29 tests; the four test classes contain 33 `@Test` methods.
- Standard Semantic Core CI uses `-pl webharvest-core -am`, so it does not build
  or test this optional module.

Keep the database module optional. Before claiming it supported, align the XML
name/namespace/schema, make named-vs-inline validation conditional, add a
successful deterministic create/insert/select integration test, verify pool
lifecycle, and add an optional-module CI gate. See [Issue #185](https://github.com/web-harvest/web-harvest/issues/185).
Keep source module ownership, Maven dependency direction, runtime registration,
metadata discovery and future product classification as separate inventory
columns.

Evidence links:
- [FTP module POM](../webharvest-ftp/pom.xml)
- [FtpPlugin.java](../webharvest-ftp/src/main/java/org/webharvest/plugin/ftp/FtpPlugin.java)
- [FtpGetPlugin.java](../webharvest-ftp/src/main/java/org/webharvest/plugin/ftp/FtpGetPlugin.java)
- [FtpPutPlugin.java](../webharvest-ftp/src/main/java/org/webharvest/plugin/ftp/FtpPutPlugin.java)
- [FtpListPlugin.java](../webharvest-ftp/src/main/java/org/webharvest/plugin/ftp/FtpListPlugin.java)
- [Mail module POM](../webharvest-mail/pom.xml)
- [MailPlugin.java](../webharvest-mail/src/main/java/org/webharvest/plugin/mail/MailPlugin.java)
- [WebBrowser module POM](../webharvest-webbrowser/pom.xml)
- [WebBrowserPlugin.java](../webharvest-webbrowser/src/main/java/org/webharvest/plugin/webbrowser/WebBrowserPlugin.java)
- [ZIP module POM](../webharvest-zip/pom.xml)
- [ZipPlugin.java](../webharvest-zip/src/main/java/org/webharvest/plugin/zip/ZipPlugin.java)
- [ZipEntryPlugin.java](../webharvest-zip/src/main/java/org/webharvest/plugin/zip/ZipEntryPlugin.java)
- [ZIP integration test](../webharvest-zip/src/test/java/org/webharvest/plugin/integration/ZipPluginIntegrationTest.java)
- [ZIP orphan-entry unit test](../webharvest-zip/src/test/java/org/webharvest/plugin/zip/ZipEntryPluginTest.java)
- [ZIP module README](../webharvest-zip/README.md)
- [Optional ZIP CI workflow](../.github/workflows/optional-zip-ci.yml)
- [Database module POM](../webharvest-database/pom.xml)
- [DatabasePlugin.java](../webharvest-database/src/main/java/org/webharvest/plugin/db/DatabasePlugin.java)
- [ConnectionPlugin.java](../webharvest-database/src/main/java/org/webharvest/plugin/db/ConnectionPlugin.java)
- [DatabaseModule.java](../webharvest-database/src/main/java/org/webharvest/ioc/DatabaseModule.java)
- [DatabasePluginTest.java](../webharvest-database/src/test/java/org/webharvest/plugin/db/DatabasePluginTest.java)
- [ConnectionPluginTest.java](../webharvest-database/src/test/java/org/webharvest/plugin/db/ConnectionPluginTest.java)
- [DefaultDriverManagerTest.java](../webharvest-database/src/test/java/org/webharvest/runtime/database/DefaultDriverManagerTest.java)
- [Database module README](../webharvest-database/README.md)

### Remaining acceptance work

1. **Complete:** the 44-row matrix matches `CorePluginsRegistrar` exactly (44/44;
   no missing, extra or duplicate names). Registration and execution evidence are
   separated in `CORE_PLUGIN_RUNTIME_EVIDENCE.md`.
2. **Classified:** `xml` is dual-use; `stylesheet`, `xq-expression` and `xq-param`
   are parent-consumed declarative nodes. Descriptor/registry semantics remain a
   product/architecture decision under Issues #93 and #94, with XML compatibility
   preserved.
3. **Complete:** no remaining direct runtime evidence gap was identified among
   the 44 default mappings. Parent-consumed configuration nodes are not claimed
   to be independent execution capabilities.
4. **Complete:** exact post-merge main push run #44161596727 on
   `4a4dcc609f302decdb3fd9f1e3da49790f01fc18` passed all three jobs.

Evidence links:
- [CorePluginsRegistrarContractTest.java](../webharvest-core/src/test/java/org/webharvest/definition/CorePluginsRegistrarContractTest.java)
- [EmptyPluginRuntimeContractIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/EmptyPluginRuntimeContractIntegrationTest.java)
- [IncludePluginRuntimeContractIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/IncludePluginRuntimeContractIntegrationTest.java)
- [XmlBasedIntegrationTestBase.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/XmlBasedIntegrationTestBase.java)
- [CorePluginsIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/CorePluginsIntegrationTest.java)
- [ControlFlowPluginsIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/ControlFlowPluginsIntegrationTest.java)
- [AdvancedPluginsIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/AdvancedPluginsIntegrationTest.java)
- [HttpPluginsIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/HttpPluginsIntegrationTest.java)
- [XmlTransformationPluginsIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/XmlTransformationPluginsIntegrationTest.java)
- [XQueryParameterTypesTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/core/XQueryParameterTypesTest.java)
- [XQueryComplexTypesIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/examples/XQueryComplexTypesIntegrationTest.java)
- [XMLPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/XMLPlugin.java)
- [TextProcessingPluginsIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/TextProcessingPluginsIntegrationTest.java)


## 15. Optional-module contribution contract audit (2026-11-28)

The detailed source-backed matrix is maintained in [Plugin Registry Generation Design, section 42](PLUGIN_REGISTRY_GENERATION_DESIGN.md#42-optional-module-contribution-inventory--annotations-xml-identities-and-build-dependencies-2026-11-28).

Key results:
- The five optional modules expose ten principal XML plugin names: Database (2), FTP (4), Mail (1), WebBrowser (1), ZIP (2).
- The ten names do not collide with the 44 default Core local-name registrations at the audited base commit. This does not make local-name-only dispatch safe; namespace identity and explicit collision validation remain required.
- FTP, Mail and WebBrowser metadata incorrectly reports `isCorePlugin() == true` even though their Maven modules are optional. This flag currently also participates in legacy package-scanner eligibility and cannot be removed safely without a compatibility migration.
- Database and ZIP use explicit registrars that mutate the global Core registry; FTP/Mail/WebBrowser rely on opt-in package scanning. There is no common module contribution contract.
- `getDependencies()` and required-parent metadata do not provide a trustworthy module dependency graph. ZIP's `zip-entry` → `zip` relation and FTP child → `ftp` relation are XML containment constraints, not Maven module dependencies.
- Optional POMs depend directly on Core plus Commons DBCP, Commons Net, Commons Email, HtmlUnit, or no additional production library (ZIP), respectively. These implementation libraries must not be promoted automatically to Core runtime Capabilities.
- All five modules have test sources and XML fixtures, but activation/dispatch/resource lifecycle coverage must be verified separately from file presence.

The audit is bounded to source and test paths at base commit `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`; external embedding usage and later main changes remain outside this evidence.


## 16. Optional-module test evidence quality (2026-11-28)

The follow-up source inspection distinguishes module test presence from real activation/dispatch evidence; details are in [Plugin Registry Generation Design, section 43](PLUGIN_REGISTRY_GENERATION_DESIGN.md#43-optional-module-integration-tests-do-not-provide-uniform-activation-or-execution-evidence-2026-11-28).

- FTP and Mail classes named `*IntegrationTest` only locate their XML fixture and assert file existence. They construct a Core-only `ScraperModule`; they do not activate the optional package, parse the fixture or execute a plugin.
- ZIP has meaningful explicit registrar, opt-in scanner and real archive-output tests, but all paths mutate the process-global registry.
- WebBrowser has an XML execution test and lifecycle tests, but the inspected setup uses only Core `ScraperModule`; the source does not establish a deterministic, isolated activation path. Test-order/global-state contamination must be ruled out.
- Database has plugin/connection/driver unit tests, but the inspected test inventory does not establish end-to-end optional XML activation and dispatch.

Do not count test files or test names as proof of optional-module runtime support. The acceptance suite must independently verify discovery, explicit selection, generation publication, QName binding, factory resolution, execution, failure behavior and resource cleanup for each module.


### CI execution qualification

The root POM excludes `*IntegrationTest` from Surefire and runs those classes through Failsafe during `verify`. The dedicated Database, FTP, WebBrowser and ZIP workflows invoke `verify`; no dedicated Mail workflow was found in the inspected workflow inventory. Consequently FTP/Mail module CI can execute and pass the current fixture-presence tests without proving XML dispatch or plugin behavior. See section 43.1 of the registry design for exact workflow commands and boundaries.


## 17. Optional plugin descriptor and lifecycle completeness (2026-11-28)

See [Plugin Registry Generation Design, section 44](PLUGIN_REGISTRY_GENERATION_DESIGN.md#44-optional-plugin-class-contracts--xml-grammar-descriptor-completeness-and-ownership-2026-11-28) for the full class-by-class matrix.

Key additional findings:
- All ten principal XML contributions were inspected, not only the classes previously listed in the module summary.
- Nine of ten return an empty `AttributeDefinition[]`, even where required/optional attribute name arrays and imperative validators exist. ZIP `zip-entry` is the only inspected class with populated attribute definitions. The current metadata therefore cannot be treated as a complete typed schema.
- XML QName, plugin metadata ID and Maven module ID are demonstrably different identity spaces (e.g. XML `database` vs plugin ID `DatabasePlugin`). The target registry must model each independently.
- FTP child parent constraints are expressed by `getRequiredParents()`; ZIP entry duplicates its `zip` containment relation in both `getDependencies()` and `getRequiredParents()`. These are grammar constraints, not module or Capability dependencies.
- WebBrowser retains static session/context maps; Database stores connection-pool handles inside the Variable hierarchy; FTP/Mail/ZIP have operation-level resource paths requiring explicit failure/cancellation cleanup characterization.
- Descriptor completeness must be explicit. Legacy incomplete metadata may be adapted, but must not be presented as a fully typed/validated descriptor.


### Conditional configuration rules and defaults

Section 45 of the registry design records behavior that is not visible in the current attribute-name metadata: Database named-vs-inline JDBC modes are mutually exclusive; FTP and WebBrowser actions have action-specific path/URL requirements; FTP defaults to port 21/passive mode; Mail defaults to port 25/plain text; WebBrowser defaults to 1000 ms wait and JavaScript enabled (malformed wait silently falls back); ZIP entry defaults charset to UTF-8. These constraints need typed conditional schemas and validator parity, not a flat list of required/optional strings.


## 18. Existing public API and external SPI readiness (2026-11-28)

See [Plugin Registry Generation Design, section 46](PLUGIN_REGISTRY_GENERATION_DESIGN.md#46-existing-public-plugin-api-is-not-yet-a-stable-independent-extension-spi-2026-11-28).

The current public `Plugin` interface is coupled to `DynamicScopeContext` and `Variable`, combines execution with mutable parent/body hierarchy, and has no explicit close-result or cancellation/drain contract. `PluginMetadata` mixes plugin/module membership, XML identity, display/UI fields and semantic roles. `PluginConfigurationSchema` cannot represent secrets, conditional variants or cross-field constraints. `DefaultPluginConfiguration` has mutable child assignment.

A concrete projection issue: `PluginMetadataCatalog.collectDependencies()` merges declared dependencies, required XML parents and semantic required-parent types into one `dependencies` list. Do not use this metadata output as a runtime dependency graph.

The existing API should be retained behind a compatibility adapter while a smaller host-independent SPI is designed and proven with a third-party sample and two independent runtime instances.


### Metadata discovery is not runtime activation

Section 47 of the registry design records the current scanner/catalog split: the static IDE metadata catalog scans Core and known optional packages, loads classes reflectively and invokes no-argument constructors; the default runtime holder scans Core only; the opt-in package scanner mutates the global Core registry. Metadata can therefore be visible while the plugin is unavailable in the default runtime. The catalog descriptor also lacks first-class module/provider/QName identity and must remain a projection, not the activation authority.


### External consumer boundary

Section 48 of the registry design records a bounded public GitHub code search: no publicly indexed third-party imports of the current v2 plugin package or matching v2 XML namespace were surfaced. This is not evidence that no consumers exist. Private repositories, binary artifacts, Maven dependents, customer XML, downstream forks and embedding applications remain unknown. Preserve compatibility through adapters until those sources are inventoried.


## 19. Core registry bootstrap consistency defect (2026-11-28)

The direct source audit of `CorePluginsRegistrar` and `CorePluginRegistry` found a concrete state-desynchronization path: the registrar's static `initialized` guard is independent of the public registry `clear()` method. Calling `clear()` without `CorePluginsRegistrar.reset()` empties the map but leaves the guard true, so the next Core registration call is a no-op and the default 44 mappings remain absent. The registration batch also consists of individually visible map writes, and duplicate local names silently overwrite earlier classes. See [Plugin Registry Generation Design, section 49](PLUGIN_REGISTRY_GENERATION_DESIGN.md#49-core-registration-bootstrap-is-mutable-non-transactional-and-can-become-desynchronized-2026-11-28).


## 20. XML execution seam, error policy and lifecycle findings (2026-11-28)

The parser's `PluginConfigurationTree` is not a resolved execution plan: `StreamingXmlParser` leaves `elementInfo` null, so each tree node's `pluginClass` is null; the runtime body executor instead traverses mutable `PluginConfiguration.children` and resolves classes from the static `CorePluginRegistry`. Ordinary child bodies also use historical best-effort semantics (log most failures, continue siblings and return only successful results), while Try uses fail-fast/Catch semantics and validation/Include failures propagate. Finally, `AbstractPlugin.execute()` calls `doCleanup()` per invocation, and public `cleanup()` calls the same hook again for instance disposal. These are separate parser/binding, error-policy and lifecycle contracts that the target design must model explicitly. The SAX parser source also lacks explicit secure-processing/external-entity hardening; exploitability is not yet reproduced, so a deterministic XXE characterization test is required before claiming safety. See [Plugin Registry Generation Design, section 50](PLUGIN_REGISTRY_GENERATION_DESIGN.md#50-xml-to-execution-seam-error-policy-and-plugin-lifecycle-are-not-one-coherent-contract-2026-11-28).


## 21. Namespace dispatch and validation admission gaps (2026-11-28)

The XML parser stores a normalized namespace URI on configuration, but `DefaultPluginBodyExecutor` resolves the implementation by local element name only through `CorePluginRegistry`; unrelated namespaces can therefore collide with or resolve to Core mappings. Unknown elements are accepted into the parse tree and fail later during execution. Separately, `AbstractPlugin.initialize()` does not invoke `validateConfiguration()`, and the executor calls only `initialize()`. Validation is opt-in inside each plugin's `doInitialize()`: Database, Mail and File call it, while Http and Def do not and reject some missing inputs only during execution. The target binder must resolve expanded QNames and centrally validate the entire statically checkable plan before side effects. See [Plugin Registry Generation Design, section 51](PLUGIN_REGISTRY_GENERATION_DESIGN.md#51-namespace-dispatch-and-configuration-validation-are-not-enforced-at-the-execution-boundary-2026-11-28).
