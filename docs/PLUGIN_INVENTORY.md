# Plugin inventory — evidence baseline

**Status:** Preliminary inventory baseline; not an approved Core membership list.
**Baseline snapshot:** `main` at `c4357d8fa531d5ba536b5d217e149de8e1bd8262` (2026-11-16).
**Latest source audit:** `main` at `1729bdf4620a66686d02605083e27e27d1ab3944` (2026-11-23); the exact source/test blob IDs and discovery/semantic call-path findings are recorded in section 15. The optional-module side-effect review in section 15.6 is a source-level audit; tests were inspected but not executed.
**Status:** This is still a preliminary inventory and decision package, not an approved Core membership list or a complete per-implementation acceptance matrix. Section 15 is the newer source-backed audit; earlier sections retain their historical snapshot context. Stage 1 descriptor projection and structural validation merged in PR #90 but do not change the manual Core mappings or parser/registry lifecycle described here.
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
| `webharvest-ftp` | `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin`, `FtpListPlugin` | Optional-integration candidate; verify container/child semantics |
| `webharvest-database` | `DatabasePlugin`, `ConnectionPlugin` | Optional-integration candidate; inspect connection/runtime support classes |
| `webharvest-mail` | `MailPlugin` | Optional-integration candidate |
| `webharvest-webbrowser` | `WebBrowserPlugin` | Optional-integration candidate |
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

> **Historical snapshot warning (refreshed 2026-11-23):** This section preserves the
> earlier 2026-11-17 evidence and must not be read as the latest runtime/test
> status. Its FTP, Mail and WebBrowser coverage/lifecycle statements were made
> against that earlier source snapshot. Section 15.6 is the later targeted
> source-level review of current `main` for side effects and policy gaps. Where
> the two sections differ, use the newer source audit and re-inspect exact
> current source/test files before implementation. In particular, the earlier
> claim that FTP integration coverage only checks resource existence is
> superseded by the later inspection of MockFtpServer operation coverage; the
> later review still does not establish all transport, timeout, size or
> cancellation policy controls. Likewise, do not treat the earlier WebBrowser
> static-client lifecycle description as a current finding without rechecking
> current source. The earlier DatabasePlugin named-connection validation defect
> is also superseded: current validation conditionally accepts either named
> connection mode or inline JDBC mode; ConnectionPlugin itself still requires
> its own id/driver/url configuration.
>
> This section remains useful as a dated dependency/module-boundary baseline,
> not as a current test coverage statement.


A targeted source/POM/test audit covered Database, FTP, Mail, WebBrowser and ZIP. This is
evidence for the module boundary review in Issue #93, not an approved Core
membership decision.

| Module | Direct runtime dependency | Source-confirmed side effects / lifecycle | Current test evidence |
|---|---|---|---|
| `webharvest-ftp` | `commons-net:commons-net` | `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin` and `FtpListPlugin` connect to remote FTP servers, authenticate, and read/write/list remote files. The connection-oriented implementations disconnect in `finally`. Requires external network, credentials and remote filesystem access. | `FtpPluginIntegrationTest` only checks that the example XML resource exists. `FtpPluginTest` mostly covers metadata/schema/initialization; its execute and invalid-action tests accept a broad `Exception` without asserting the exact failure. No deterministic FTP server execution contract was found in the inspected tests. |
| `webharvest-mail` | `org.apache.commons:commons-email` | `MailPlugin` creates SimpleEmail/HtmlEmail and calls `email.send()`; this is an external SMTP/network side effect that can deliver real email. | `MailPluginIntegrationTest` only checks that the example XML resource exists. No deterministic SMTP execution contract was found in the inspected integration test. |
| `webharvest-webbrowser` | `net.sourceforge.htmlunit:htmlunit:2.70.0` | `WebBrowserPlugin` loads remote URLs, executes JavaScript/AJAX and stores WebClients in static `namedPages` / `defaultPage` state. The explicit `close` action closes clients, but the inspected class has no automatic Harvester-run cleanup hook; the static default client also raises cross-run isolation/concurrency questions. | The integration test executes XML but asserts only a non-null context; it does not assert loaded page content or script results. |
| `webharvest-zip` | No additional direct runtime library beyond Core/JDK | `ZipPlugin` owns a local `ZipOutputStream` and exposes it only in a scoped `DynamicScopeContext` value; `ZipEntryPlugin` reads it from that scope. Nested ZIP scopes restore the outer stream and try-with-resources closes it. | `ZipPluginIntegrationTest` executes the XML example and inspects outer and nested archive contents; `ZipEntryPluginTest` rejects an orphan entry. Optional ZIP CI runs Core+ZIP verify. See PR #187 / Issue #97. |
| `webharvest-database` | `commons-dbcp:commons-dbcp`; no JDBC driver declared by the module | `DatabasePlugin` executes SQL via `PreparedStatement`, supports inline driver/url and a named `connection` path; `ConnectionPlugin` stores named connection factories in context. Named mode is currently blocked by unconditional driver/url schema requirements. Metadata/annotation names and namespaces also disagree. | 33 unit tests (12 DatabasePlugin, 10 ConnectionPlugin, 7 exception, 4 driver-manager); no successful SQL result assertion or executed XML integration fixture. Core CI excludes this optional module. Follow-up: Issue #185. |

The inspected Database, FTP, Mail, WebBrowser and ZIP classes carry `@CorePlugin`
annotations despite being in optional Maven modules. Database/FTP unit tests also
assert `metadata.isCorePlugin() == true`. Therefore the annotation and
`PluginMetadata.isCorePlugin()` flag are not reliable product-boundary signals.

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


## 15. Plugin Platform release-gate audit — 2026-11-23

**Status:** Release-blocking architecture gap. This section records current-main source behavior; it is not an implementation claim or an approved SPI.

**Related work:** [#93 inventory/decision](https://github.com/web-harvest/web-harvest/issues/93), [#94 unified plugin platform](https://github.com/web-harvest/web-harvest/issues/94), [#346 SPI/descriptors](https://github.com/web-harvest/web-harvest/issues/346), [#347 discovery/registry](https://github.com/web-harvest/web-harvest/issues/347), [#348 semantic projection](https://github.com/web-harvest/web-harvest/issues/348), [#349 policy/lifecycle](https://github.com/web-harvest/web-harvest/issues/349), [#350 conformance suite](https://github.com/web-harvest/web-harvest/issues/350), [release gate #88](https://github.com/web-harvest/web-harvest/issues/88).

### 15.1 Current implementation facts

The following files were read from current `main`; blob IDs are included to make the audit reproducible:

| Source | Blob | Confirmed behavior |
|---|---|---|
| `CorePluginsRegistrar.java` | `7ddc5b26a33c5143ed893a513263718990fa515e` | 44 manual XML mappings, including legacy `set-var`; process-global static initialized flag and singleton registry |
| `PluginClassScanner.java` | `dadd28a434000816253ac66bff736e2c5fff0494` | Scans six hard-coded package roots; annotation-based class discovery; caches successful class-name list; fallback on scan failure |
| `PluginMetadataCatalog.java` | `5ac82d7fb6436b458b29647e4c929878557b1c6e` | Builds a static catalog from scanner results; loads each class and invokes its no-argument constructor, then calls `getMetadata()`; duplicate descriptor IDs are inserted with `Map.put` |
| `PluginDescriptor.java` | `c3d3f8a70f4072895fed6c0133c8caf0bc556e79` | Carries ID, element name, display/category/description, origin, class name, semantics, UI hints, dependencies and extensions; not yet a versioned provider SPI |
| `PluginSemanticsResolver.java` | `5590d9642e1dc2b1890a24b6caf1c2d19b07346d` | Derives semantics from implementation class plus `PluginMetadata` / schema / visual metadata |
| `SemanticModel.java` | `abfb1af10be445084149682462143d4676eb018c` | Stage 1 lazily creates a descriptor index from the metadata catalog |
| `PluginDescriptorIndex.java` | `017a6011783c30d24a6b74d22628f3e010e9094b` | Uses namespace-qualified descriptor lookup, but runtime availability is a separate local-name-only probe against `CorePluginRegistry` |
| `OptionalPluginProfile.java` | `6c83ad1474553d9657ca8f44547e8808fd1f934a` | Hard-coded five-entry optional-extension profile with class-name presence probes |
| `CorePlugin.java` | `073f25677e8a647a4a9bbd6de7ec456df521ae40` | Annotation Javadoc says Core plugins are always available, while actual annotation use spans optional modules and non-default/internal/legacy classes |

### 15.2 Release-critical consequences

1. **No general external provider discovery contract:** package roots are hard-coded. A third-party artifact in an unrelated package is not discovered by this scanner merely because it is on the classpath.
2. **Discovery is not side-effect-free:** catalog initialization constructs each discovered implementation. A disabled provider could therefore execute constructor code before host activation policy; providers requiring host DI or a non-default constructor cannot participate cleanly.
3. **Catalog collision handling is unsafe for correctness:** duplicate IDs can overwrite prior descriptors rather than fail bootstrap with a collision diagnostic.
4. **Semantic/runtime truth is split:** Stage 1 semantic indexing consumes the metadata catalog, while runtime availability is checked separately against the manual Core registry. The optional profile is another hard-coded list. These are not one immutable effective registry snapshot.
5. **Current descriptors are not the public SPI:** explicit provider/API compatibility, lifecycle/DI factory, artifact provenance, permissions/resource policies and stable dependency/alias resolution are not yet represented as a complete extension contract.
6. **Annotation is not activation or trust:** `@CorePlugin` cannot determine product membership, enablement or authorization.

These are source-backed architecture gaps. They do not establish a confirmed exploit or claim that the current Stage 1 XML inspector is defective within its narrower documented contract.

### 15.3 Existing test coverage inspected (not executed in this audit)

- `PluginClassScannerTest` blob `acb1d90457c8912f23f0078a9e0f73092178c221`: covers sorting, unique names, cache, retry/fallback, optional-package absence and required-Core scan failure. It does not prove third-party artifact/provider discovery or registry parity.
- `PluginMetadataCatalogTest` blob `40efed43975105fc214357ba526c78d4615d9693`: checks descriptor completeness and selected semantic/UI/schema projections. It does not assert constructor-free discovery, no-arg-free DI provider construction, duplicate-ID failure or host activation isolation.
- `PluginDescriptorIndexTest` blob `7e80f3e375eceaa1618ffcf03b40152b54171751`: covers namespace resolution, ambiguity, optional availability and runtime probe failure. It does not bind the index to an authoritative runtime registry snapshot.
- `SemanticModelTest` blob `769f65937d116a93a99b881e9cfb0eb93529a670`: covers XML fidelity, secure parsing and limits; it does not test external plugin capability discovery.
- `CorePluginsRegistrarContractTest` blob `bda7938a517bccc78150932711355035c5b4d1eb`: asserts exactly 44 default mappings including `set-var`.

No tests were run as part of this read-only audit.

### 15.4 Mandatory target behavior and tests

The target pipeline is:

`artifact/classpath discovery → descriptor parsing → compatibility/dependency/collision validation → host activation and permission policy → provider construction through host DI → immutable effective registry snapshot → semantic/metadata/schema projections + runtime dispatch`

Discovery and descriptor projection must not instantiate implementations. A provider is constructed only after activation and authorization. The same snapshot ID must be used by validation and execution.

The conformance suite (#350) must prove:

- External JAR outside the current six package roots is discovered through the supported provider mechanism without Core source edits.
- Disabled provider constructor sentinel remains untouched; descriptor/catalog/semantic projection works without implementation construction.
- A DI-only provider (no public no-arg constructor) is constructed only after activation.
- Duplicate IDs, XML QNames/namespaces and aliases fail deterministically; no silent overwrite.
- Malformed, incompatible, missing-dependency and linkage-failing providers produce explicit fail-closed diagnostics.
- Runtime dispatch, metadata, semantic index and structural/semantic validation expose identical enabled IDs/QNames/aliases for one registry snapshot.
- Core-only runtime works without optional modules; XML compatibility including `set-var` remains intact.
- Lifecycle initialization rollback, cancellation, concurrency, classloader close, resource cleanup and secret/log redaction are verified.
- Classpath/JAR ordering and repeated bootstrap do not change the effective registry output.

**Release rule:** no release GO until this suite passes against the exact candidate Core artifact, the independent fixture report and artifact hashes are attached to #94/#88, and the maintainer records explicit sign-off.


### 15.5 Autonomous dynamic platform clarification (2026-11-23)

The concise pipeline above is superseded by the more complete target contract in
[Autonomous Dynamic Plugin Platform](AUTONOMOUS_DYNAMIC_PLUGIN_PLATFORM.md)
(being introduced in documentation PR #352). “Artifact/classpath discovery” must
not be interpreted as a one-time scan of the JVM's immutable startup classpath.

The release target is a **runtime-managed extension source** with autonomous
reconciliation of live artifact add, update and removal while the host remains
running. The host owns artifact fingerprints, provider discovery, dependency and
collision validation, classloader/module lifecycle, and publication of immutable
registry generations. A generic provider contract is used for built-in and
external artifacts; Core must not maintain package roots or per-plugin class
lists.

Discovery, descriptor parsing and semantic/catalog/schema projection are
side-effect-free and do not instantiate provider implementations. Discovery
makes a provider known; host policy separately determines compatibility,
availability, enablement, authorization and execution. Only an authorized
provider may be constructed through the host's DI/provider factory.

Runtime dispatch, metadata/catalog, schema, inspection, Semantic Model and
validation are projections of the **same** registry generation. Each execution
pins one generation. New sessions use the newest published generation; existing
sessions remain consistent on their pinned generation. Removal prevents new
admissions, drains or cancels existing work according to a defined policy, and
releases provider resources and classloaders only after all references retire.
Failed reconciliation preserves the last-known-good generation and reports
actionable artifact/provider diagnostics.

A normal JVM classpath is fixed after process startup. Merely copying a JAR into
a directory does not make it visible to that classloader. Therefore, the
implementation must own a managed extension source and explicit classloader or
module lifecycle; it must not promise live loading based only on classpath
mutation. ServiceLoader may enumerate providers inside an already managed
artifact/classloader, but it is not the whole discovery or lifecycle mechanism.

**Security boundary:** in-process Java permissions and host policy are not a
sandbox against malicious provider code. A provider loaded into the host JVM
runs with the process's effective privileges. Only trusted/approved providers
may run in-process; genuinely untrusted extensions require a separate process
or stronger OS/container isolation boundary. This distinction must be reflected
in the SPI, threat model and release documentation.

Mandatory #350 evidence must exercise a separately built external provider
through live add → discover → inspect/semantic/schema visibility → authorize →
execute → update → generation-pinned concurrency → remove/drain/cleanup →
re-add, without restarting the host or editing Core source. Startup-only
scanner tests do not satisfy this release gate.


### 15.6 Optional extension side-effect and policy audit

This is a source-level review of the current optional module implementations. It
records observed behavior and missing host-policy contracts; it is not a claim
that the modules have passed security testing. Existing test sources were
inspected but not executed for this audit.

| Module / entry points | Observed execution and lifecycle behavior | Policy / verification gaps before safe activation |
|---|---|---|
| Database — `DatabasePlugin`, `ConnectionPlugin` | Supports named and inline connection configuration. Inline configuration includes JDBC driver, URL, username, password and SQL. SQL reaches `prepareStatement(sql)`; the inspected plugin schema does not expose a parameter-binding contract. SELECT handling is based on a `startsWith("select")` check. ResultSet/Statement cleanup occurs in finally; connection cleanup differs between standalone and EventBus/session-managed paths. No password logging was observed in the inspected code. | Driver and JDBC URL allowlists; credential/secret provider; query timeout; transaction and cancellation semantics; result-row/byte limits; safe connection-pool ownership; parameterized query contract. |
| FTP — `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin`, `FtpListPlugin` | Uses Apache Commons Net FTP. Inline configuration carries host, port, user, password and paths. Logs include host, username and path (not password). Logout/disconnect cleanup is present. Get buffers the full remote object in a `ByteArrayOutputStream` and returns a String-backed value; put materializes the body as a byte array. Direct plugin variants do not show explicit timeout configuration. | Explicit secure transport mode (FTP vs FTPS/SFTP; no SFTP evidence found); destination/host allowlist and local path sandbox; secret source; connection/data timeout; bounded transfer size; cancellation and partial-transfer cleanup. |
| Mail — `MailPlugin` | Uses Commons Email; host, port, from, to, subject and body are accepted; SMTP defaults to port 25. Debug logging includes host/from/to/subject. The inspected implementation does not visibly configure SMTP username/password authentication despite credentials appearing in class documentation. Sending is an irreversible external side effect. | Explicit send authorization; recipient/domain policy; TLS/authentication requirements; secret source; log redaction; idempotency/retry semantics and audit trail. |
| Web browser — `WebBrowserPlugin` | Supports load, execute-script, get-content and close. JavaScript is enabled by default. URL validation checks HTTP(S) scheme, while HtmlUnit follows URLs and executes JavaScript. Logs include requested URL and a prefix of submitted JavaScript; result content is debug-logged. Session cleanup is present on completion/failure/cancel/close paths. | Scheme-only checks are not an SSRF boundary: private/link-local/local destinations, redirects and DNS rebinding need explicit policy. Add JavaScript permission, destination/redirect policy, navigation and execution time/resource limits, output limits and concurrency/terminal-race tests. |
| ZIP — `ZipPlugin`, `ZipEntryPlugin` | Builds archives in memory. `ZipEntryPlugin` passes the supplied entry name to `new ZipEntry(name)`; only non-empty validation was observed. Scoped execution-context stream ownership and nested archive restoration are covered by the ZIP integration path; streams use try-with-resources. | Normalize/reject absolute and traversal-like entry names; define duplicate-entry policy; cap entry count, total uncompressed bytes and in-memory archive size; define cancellation and failure cleanup. This is a producer-side path/name policy gap, not evidence of an extraction vulnerability. |

### 15.6.1 Optional plugin entry-point reconciliation

The module-level table above is not sufficient for the #93 per-implementation
inventory. The ten concrete entry points currently resolve as follows (source
files were re-read from `main` on 2026-11-23):

| Implementation / XML name | Role and relationship | Source-confirmed behavior | Required policy / unresolved contract |
|---|---|---|---|
| `DatabasePlugin` / `database` | Executable SQL operation; accepts either a named `connection` or inline JDBC configuration | Evaluates SQL templates/body, obtains a JDBC connection, calls `prepareStatement(sql)`; classifies queries using `sql.trim().toLowerCase().startsWith("select")`; materializes all result rows as XML text; closes ResultSet/Statement in finally. | JDBC driver and URL allowlists; secrets; parameter binding; query timeout, cancellation, transactions and result limits. The prefix-based SELECT classification is not a general SQL statement classifier. |
| `ConnectionPlugin` / `connection` | Executable declaration that stores a named connection factory in the execution context for later `database` use | Requires `id`, `driver`, `url`; optional username/password/poolSize; registers its factory for session cleanup when EventBus is present. Metadata reports `isCorePlugin=false`; DatabasePlugin consumes the named connection. | Treat credentials as secrets; JDBC URL/driver allowlist; deterministic session ownership/cleanup; avoid exposing connection details through variable/log output. |
| `FtpPlugin` / `ftp` | Parent/container and standalone action entry point | Accepts actions `get`, `put`, `list`, `mkdir`, `rmdir`, `del`; creates an Apache Commons Net `FTPClient`, logs in, executes remote operation and disconnects in finally. GET/PUT materialize payloads in memory. | Network destination and credential policy, remote path policy, bounded payloads/timeouts/cancellation, explicit secure transport. **Important:** source uses plain `FTPClient`; class Javadoc's statement that port 990 enables implicit FTPS is not evidence of TLS. |
| `FtpGetPlugin` / `ftp-get` | Child operation whose schema requires parent `ftp`; nevertheless the implementation reads host/user/password itself and creates its own FTP connection | Downloads to `ByteArrayOutputStream`, then materializes bytes; closes/logout/disconnects its own client. | Resolve the mismatch between advertised shared parent connection and per-child connection setup; remote host/path policy, size bounds, timeouts, secure transport and cancellation. |
| `FtpPutPlugin` / `ftp-put` | Child operation requiring parent `ftp`; implementation independently connects using host/user/password | Executes child body, converts it to a byte array, uploads via FTPClient, then closes its own connection. | Same parent-vs-own-connection mismatch; outbound destination, remote write authorization, body size, timeouts, secure transport and cancellation. |
| `FtpListPlugin` / `ftp-list` | Child operation requiring parent `ftp`; implementation independently connects using host/user/password | Lists remote directory and materializes names/results; own connection lifecycle. | Same parent-vs-own-connection mismatch; destination/path policy, maximum entries/output size, timeout, secure transport and cancellation. |
| `MailPlugin` / `mail` | Executable irreversible SMTP send | Creates SimpleEmail/HtmlEmail and calls `send()`; logs SMTP host, sender, recipient and subject. Current schema requires smtp-host/from/to/subject; optional port/type/from-name. | **Source/Javadoc mismatch:** Javadoc describes username/password authentication and TLS examples, but current schema has no username/password or TLS fields and execution does not configure authentication/TLS. Do not advertise authenticated/TLS delivery until implemented and tested. Add send authorization, recipient policy, secret support, transport security and log redaction. |
| `WebBrowserPlugin` / `web-browser` | Executable browser session with load/execute-script/get-content/close actions | Browser clients are held in session-keyed state (or weak context fallback); session completion/failure/cancellation events close clients. JavaScript defaults to enabled; URL check accepts HTTP(S) scheme only. | SSRF destination/redirect/DNS policy, explicit JavaScript permission, execution/navigation/time/memory/output limits, concurrent terminal-state tests. Session scoping is a positive lifecycle improvement, not a security boundary. |
| `ZipPlugin` / `zip` | Executable archive scope/provider | Creates an in-memory `ByteArrayOutputStream` archive and exposes the active stream through scoped execution context; nested scope restoration and try-with-resources are covered by the ZIP integration contract. | Bounded archive/entry count/uncompressed size and cancellation; deterministic close/rollback on failure. |
| `ZipEntryPlugin` / `zip-entry` | Declarative child operation requiring parent `zip`; writes one named entry into the scoped archive | Requires non-empty `name`, optional charset, rejects root use; passes the name directly to `new ZipEntry(name)`. | Normalize/reject absolute and traversal-like names; duplicate policy; per-entry and aggregate size limits; charset/error policy. |

This reconciliation exposes two important mismatches that must not be hidden by
the generic module labels: (1) FTP child schemas require an `ftp` parent, while
the child implementations create their own network clients rather than using
the advertised shared parent connection; (2) Mail Javadoc advertises
authentication/TLS configuration that the current executable schema and
`doExecute` path do not implement. The FTP source also documents FTPS on port
990 while the factory constructs plain `FTPClient`, not an `FTPSClient`.
These are source observations, not runtime test results.

Source blob IDs:
- `DatabasePlugin.java`: `b3934d753f17f133bd582b50206db701af843728`
- `ConnectionPlugin.java`: `edb6ed93584cb03d967c830d120848c7e891f481`
- `FtpPlugin.java`: `d7098611ea7e0569b7cf9af01346be7b951da799`
- `FtpGetPlugin.java`: `fd4ee5da4556bf91bdae7940072cf7a5e106f6d5`
- `FtpPutPlugin.java`: `34061eb4fbc3a70f95d8d90a3a2faef611659f82`
- `FtpListPlugin.java`: `c5fcfa6c5f90f643e6df8e85f6ad6a0b6e1074fa`
- `MailPlugin.java`: `083297481e7e218916a77f0dea2308c63bbac7e7`
- `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`
- `ZipPlugin.java`: `e62046292bca8689637e5eefbd7ce25ed460d118`
- `ZipEntryPlugin.java`: `ea90361ca90b31d4c3903688ca78204121af0743`
- `FtpClientFactory.java`: `acfb17feb4361527bf86db21642f77a96c2ddece`

The existing tests cover meaningful happy/error paths (including FTP mock-server
operations, database plugin/driver behavior, mail configuration and SMTP
integration, browser regression/integration behavior, and ZIP nested archive
bytes/orphan-entry rejection). They do **not**, based on the inspected sources,
establish the full host-policy controls above. In particular, tests for TLS/auth,
SSRF/redirect policy, bounded transfer/archive resources, secret/log redaction,
and cancellation/resource exhaustion are not demonstrated by this inventory.
Do not infer a passing test result from the presence of test source files.

These requirements must be represented in provider descriptors as requested
permissions/resource declarations and enforced by the embedding host. Descriptor
declarations alone are not enforcement. They belong in #349 policy/lifecycle
and #350 conformance acceptance, and must not be treated as a reason to activate
an extension automatically when it is discovered.


### 15.7 Core mapping role and host-effect crosswalk (proposal, not approval)

This crosswalk turns the 44-name runtime evidence matrix into descriptor/SPI
inputs. It does **not** decide which capabilities belong in the minimal Core
product. “No special host permission identified” means only that the operation
is internal to the execution model; it does not remove the need for CPU, memory,
recursion, concurrency and execution-time budgets.

| XML name | Runtime role | Descriptor kind | Host effect / policy domain to declare |
|---|---|---|---|
| `call` | Invoke a named function | Executable | Internal execution; recursion/depth and execution budget |
| `call-param` | Supply a function argument | Declarative child | Inherits parent call/function context; no independent activation |
| `case` | Select a control-flow branch | Executable | Internal execution budget |
| `catch` | Handle a parent try failure | Declarative/control-flow child | Inherits parent try scope; no independent activation |
| `constant` | Produce a constant value | Executable | Memory/output-size budget |
| `def` | Define a variable | Executable | Context mutation and memory budget |
| `else` | Alternative branch | Declarative/control-flow child | Inherits parent if scope |
| `empty` | Execute nested work and return empty | Executable | Inherits permissions of nested operations |
| `exit` | Terminate current execution path | Executable | Session cancellation/termination semantics |
| `file` | Read/write a local file | Executable | Filesystem roots, read/write mode, symlink/path policy, byte limits |
| `function` | Define a callable function | Executable/declaration | Function namespace, recursion/depth and memory budget |
| `get` | Read a context variable | Executable | Context read; secret-value handling if host marks values sensitive |
| `html-to-xml` | Parse/convert HTML | Executable | CPU, memory, input/output size and parser limits |
| `http` | Perform an HTTP request | Executable | Outbound network destinations, schemes, redirects, headers, credentials, timeout and response limits |
| `http-header` | Configure an HTTP header | Declarative child | Inherits HTTP policy; sensitive-header/credential redaction |
| `http-param` | Configure HTTP request parameters | Declarative child | Inherits HTTP policy; URL/body encoding and size limits |
| `if` | Conditional execution | Executable | Internal execution budget |
| `include` | Load another configuration file | Executable | Filesystem read roots, canonical path/symlink policy, include depth/cycle limits |
| `json-to-xml` | Parse/convert JSON | Executable | CPU, memory, input/output size and parser limits |
| `loop` | Iterate a collection | Executable | Iteration, recursion, CPU and wall-clock budgets |
| `regexp` | Apply regular-expression processing | Executable | CPU/time/input-size limits; regex denial-of-service controls |
| `regexp-pattern` | Configure a regexp pattern | Declarative child | Inherits regexp execution policy |
| `regexp-result` | Configure regexp result/capture template | Declarative child | Inherits regexp execution policy; output-size limit |
| `regexp-source` | Configure regexp input | Declarative child | Inherits regexp execution policy; input-size limit |
| `return` | Return from a function | Executable/control-flow | Function/session lifecycle semantics |
| `script` | Evaluate a script | Executable | Script-engine allowlist, code-execution trust, exposed host capabilities, CPU/time/memory limits |
| `list` | Construct a list | Executable | Memory and collection-size budget |
| `log` | Emit a log message | Executable | Logging sink/routing policy, redaction and message-size limit |
| `value-of` | Resolve/extract a value | Executable | Context/data access; output-size budget |
| `xml` | Produce an XML value or supply parent XML content | Dual-use | Parser/serializer limits; declarative use inherits parent capability |
| `set-var` | Legacy variable assignment | Executable/compatibility alias | Context mutation; preserve compatibility |
| `sleep` | Delay execution | Executable | Maximum per-call and cumulative wait budget; cancellation responsiveness |
| `stylesheet` | Supply XSLT stylesheet text | Declarative child | Inherits XSLT policy; stylesheet/parser resource limits |
| `template` | Render a template | Executable | Template evaluation limits; filesystem/network only if separately exposed by the template engine |
| `text` | Produce text content | Executable | Memory/output-size budget |
| `tokenize` | Split text into tokens | Executable | CPU, input-size and collection-size limits |
| `try` | Establish an exception boundary | Executable | Cancellation/fatal-error propagation; bounded nesting |
| `while` | Conditional iteration | Executable | Iteration, recursion, CPU and wall-clock budgets |
| `xml-to-json` | Convert XML to JSON | Executable | CPU, memory, input/output size and parser limits |
| `xpath` | Query XML using XPath | Executable | CPU, expression complexity and result-size limits |
| `xquery` | Evaluate XQuery | Executable | Query engine/function allowlist, CPU/time/memory and result-size limits |
| `xq-expression` | Supply XQuery expression text | Declarative child | Inherits XQuery policy |
| `xq-param` | Supply XQuery parameter | Declarative child | Inherits XQuery policy; nested value providers retain their own permissions |
| `xslt` | Execute XSLT transformation | Executable | External document/stylesheet access policy, extension functions, CPU/time/memory and output limits |

#### Descriptor and enforcement implications

1. Descriptors should distinguish **executable providers** from **declarative
   configuration nodes**. The latter retain XML identity and schema/parent-child
   rules but do not acquire an independently activatable provider merely because
   they have a class or registrar mapping.
2. Permission declarations should be capability-scoped and structured (for
   example filesystem read vs write, outbound network destination classes,
   script engine/function access), not a single broad “plugin permission”.
3. A child node inherits the relevant parent execution policy; it must not
   silently grant itself broader permissions. Nested executable providers still
   require their own permission checks.
4. Host enforcement must occur at the actual I/O/engine boundary. Descriptor
   declarations are requests, not enforcement. Legacy providers that bypass
   policy-aware host services must be adapted or remain unavailable under a
   restricted host profile.
5. Resource budgets apply even to internal Core operations. CPU/memory/recursion
   exhaustion can affect the embedding process even when no filesystem or network
   permission is requested.
6. The same policy model must be usable for built-in and external providers;
   product membership and trust level remain separate decisions.

This table is a design aid for #346/#349/#350. It is not a statement that all
listed hooks exist today, nor a maintainer-approved Core membership decision.



### 15.8 Core artifact dependency footprint and boundary implications (2026-11-23)

This is a **module-level direct dependency audit**, not a class-to-library attribution claim. The current `webharvest-core/pom.xml` at main baseline `1729bdf4620a66686d02605083e27e27d1ab3944` declares the following production dependencies (test-scoped entries are excluded):

| Dependency family | Direct Core POM declarations | Boundary implication to verify at call-site level |
|---|---|---|
| Logging / utility / collections | SLF4J API, Commons Lang 3, Commons IO, Commons Collections, Guava | Likely shared runtime infrastructure; retain only where actual Core call sites require them. |
| JSON / data conversion | `org.json:json`, Gson | Determine whether conversion providers can be separated without breaking shared value/config contracts. |
| HTML / parsing | TagSoup, Jsoup | HTML parsing/conversion is a candidate adapter capability; confirm shared parser use before moving. |
| HTTP client | Apache HttpComponents 5 `httpclient5` | Outbound network capability should sit behind a host-policy-aware service; HTTP XML compatibility must remain. |
| XML query / transformation | Saxon (base and DOM classifier) | XPath/XQuery/XSLT call sites and shared XML value contracts must be mapped before module separation. |
| Script engines | BeanShell; Rhino Engine (runtime); Groovy + Groovy JSR-223 (runtime profile) | Script execution is a high-risk host capability; engine discovery and host object exposure need explicit policy. |
| Discovery / DI / annotations | Reflections, Guice, `javax.annotation-api` | Reflections is not an acceptable substitute for the managed-artifact discovery contract; Guice/API dependencies may be shared Core infrastructure but provider construction must be host-controlled. |
| Bytecode/test support | CGLIB `cglib-nodep` (production scope in current POM) | Confirm production call sites; avoid retaining test-era infrastructure in the minimal Core runtime without evidence. |

The POM also declares test-only SLF4J Simple, WireMock and Mockito; these are not production runtime dependencies. The Groovy production profile is active unless `groovy.compatibility` is set, and declares the Groovy runtime and JSR-223 artifacts separately. The compatibility profile substitutes the configured Groovy compatibility version. These profile semantics must be preserved when evaluating any dependency split.

**What this establishes:** Core currently carries direct runtime dependencies spanning HTTP, HTML parsing, XML transformation/query, scripting, reflection-based scanning and DI, in addition to execution primitives. Therefore, “move the optional plugin classes” is not a sufficient modularization plan. A boundary proposal must map (a) each implementation's direct imports/call sites, (b) shared Core callers and public value contracts, and (c) transitive dependency reachability before deciding whether a dependency belongs in Core, an adapter, or a host-provided service.

**What this does not establish:** the table does not prove that each listed dependency is used by a particular plugin, that every dependency can be removed independently, or that any Core/extension membership decision has been approved. The next audit step is per-class import/call-site attribution for the dependency-heavy capabilities, followed by test coverage and compatibility mapping. No source or POM changes are authorized by this inventory.

Evidence: `webharvest-core/pom.xml` fetched from the current `main` branch at the baseline above; the production Groovy dependencies are declared in the `groovy-production-runtime` profile. Test dependencies are separately scoped as `test`.


### 15.9 Targeted Core implementation dependency/call-site samples (2026-11-23)

The following source files were inspected directly to begin the per-class attribution requested by #93. The evidence below is intentionally limited to imports and visible integration seams; it is not yet a complete method-level call graph or proof that a host policy is enforced.

| XML capability / class | Current source blob | Direct implementation dependencies or seams observed | Boundary question |
|---|---|---|---|
| `http` / `HttpPlugin` | `c6045d6c7638f3aa86c510240814633597b108ad` | Injects/uses the Core `HttpService` abstraction; imports Guice `@Inject`, response and service exception types, runtime context and templating. | Strong candidate for a policy-aware host service seam. Inspect the service interface, concrete binding and redirect/credential/timeout enforcement before claiming the abstraction is sufficient. |
| `file` / `FilePlugin` | `8d78e797975ab236a47420d18f28600beaae04bf` | Imports injected `WorkingDir`, Guice, file/plugin exceptions, runtime variables and `FileListIterator`. | Determine whether WorkingDir constrains canonical paths and symlinks or merely supplies a base directory; enumerate all read/write/list operations and their limits. |
| `include` / `IncludePlugin` | `ab06127f7f5402978937160a42455b4c0107dfab` | Uses `ConfigFactory`, `ConfigSource`, `Config`, `PluginConfigurationTree` and `DefaultPluginBodyExecutor`; no external library is visible in the import list. | Include is a configuration-loading/execution composition boundary. Confirm path resolution, include cycle/depth limits and whether it reuses the active registry generation. |
| `script` / `ScriptPlugin` | `0bd1992d0739f43c9d550b302d4fa0a815e63995` | Delegates through Core `ScriptEngineFactory`, `ScriptSource` and `ScriptingLanguage`; imports Guice and runtime context. | Separate script-engine selection from host-object exposure and execution policy. Engine availability alone must not grant script permission. |
| `xslt` / `XsltPlugin` | `c94b8e26dba566cb30bc12f6fc811bb1fcdc59b1` | Uses JAXP `TransformerFactory`, `Transformer`, SAX parser APIs and XML constants directly. | Confirm secure-processing and external document/stylesheet access restrictions at factory/resolver boundaries; a JAXP API dependency is not itself a sandbox. |
| `xpath` / `XPathPlugin` | `e519eb21a2e6faeef43a9d23c219502ac7f7f2a6` | Imports Saxon query/configuration types, Jsoup DOM types and Core `XmlUtil`. | Confirm whether this class currently combines XPath and HTML selection concerns; split only after runtime behavior and XML compatibility are mapped. |
| `xquery` / `XQueryPlugin` | `b1f0ee00c16c9a37144e7e27cb46bfd57de6e9f1` | Uses Saxon `Configuration`, `DynamicQueryContext`, `StaticQueryContext`, `XQueryExpression`, Core `XmlUtil` and runtime values. | Define query function/module/external-resource policy and bounded execution/result semantics before classifying Saxon as a Core invariant or adapter dependency. |
| `html-to-xml` / `HtmlToXmlPlugin` | `ad3322118e453239fcf8d16646490070b2fc8ba2` | Uses Jsoup parser/document/element/safelist APIs directly. | HTML parsing is a clear dependency-bearing conversion capability; preserve its XML output contract if implementation is later moved behind a provider boundary. |

This sample reveals a useful distinction: some effects already have an injectable Core service seam (`HttpService`), while others directly use JAXP/third-party APIs or a narrow context object (`WorkingDir`, `ScriptEngineFactory`). It is premature to conclude that these seams are policy-complete. The next pass must inspect the corresponding service interfaces and Guice bindings, enumerate all call sites, and map the tests that assert path/network/script/query behavior.

The import-level sample is a starting slice, not completion of the #93 per-plugin dependency/call-site matrix. No code, test or POM behavior changed.


### 15.10 Verification of three apparent host-service seams (2026-11-23)

A follow-up inspected the actual service interfaces, default Guice bindings and focused tests. This refines section 15.9: the existence of an injectable interface is not equivalent to a policy-enforcing boundary.

| Seam | Exact source evidence | Current conclusion |
|---|---|---|
| HTTP | `HttpService.java` blob `7ef8e5f318f8dcbaf23d05f360474c6a41f27ed2`; `HttpServiceImpl.java` blob `9e3dab1a40250f4d059ea812c74012d994873f0d`; `ScraperModule.java` blob `d31a5e30b0949c752743123cbcfb4219ba60713e` | The interface accepts method, URL, body and headers only; it has no policy/context parameter. The default implementation enables redirects by default, reads the entire response into a byte array, and exposes no explicit connect/response timeout or response-size limit in the inspected implementation. ScraperModule binds one singleton `HttpServiceImpl`. Thus this is a useful abstraction seam, but current binding is not a per-host/per-session policy boundary. |
| Filesystem | `WorkingDir.java` blob `5f763cf8e8c43dda4add3e198304eda599aed499`; `FilePlugin.java` blob `8d78e797975ab236a47420d18f28600beaae04bf` | `WorkingDir` is a Guice binding annotation for a working-directory string (Javadoc describes temporary-file location); it is not itself a filesystem service or sandbox. No canonical-path, symlink, root allowlist or byte-budget guarantee follows from this annotation. The actual FilePlugin path operations still need method-level review. |
| Scripting | `ScriptEngineFactory.java` blob `3aa0acd84369b6167db810200d39fc6a6c0c7765`; `ScraperModule.java` blob `d31a5e30b0949c752743123cbcfb4219ba60713e` | Core exposes an engine-factory abstraction and binds `JSRScriptEngineFactory` as a singleton. The interface returns an engine for a ScriptSource; it does not carry a host permission context, allowed host-object set, execution budget or cancellation contract. Engine selection abstraction is not script isolation. |

The existing `HttpServiceRedirectContractTest` (blob `f21c8c62ccca31e4b01891970244b1d9439b1b8f`) verifies deterministic loopback redirect enabled/disabled behavior and configured header propagation. It does not establish destination allowlisting, private/link-local address blocking, redirect target revalidation, DNS rebinding resistance, timeout enforcement or bounded response consumption. Tests were inspected, not run in this audit.

**Architecture consequence:** the next policy design must be expressed in host-owned service contracts, not merely descriptor permission strings. HTTP needs destination/redirect validation, timeout and bounded-body policy; filesystem needs a canonicalized root-aware service; scripting needs an explicit capability context and execution lifecycle. Existing interfaces can inform compatibility adapters, but should not be assumed to satisfy the new contracts unchanged.

No runtime code, tests, POMs or behavior changed.


### 15.11 Method-level review: filesystem, include, script and XSLT (2026-11-23)

This pass follows the concrete execution methods rather than relying on class names or metadata. Source blob IDs are pinned below; the observations describe current source behavior, not a future policy contract.

| Capability | Source evidence | Method-level behavior and boundary finding |
|---|---|---|
| `file` / `FilePlugin` | `FilePlugin.java` blob `8d78e797975ab236a47420d18f28600beaae04bf` | `doExecute` evaluates the path and dispatches read/write/append/list. Read/write/list handlers construct `new File(workingDir, path)`; Java absolute child paths can escape the working directory, and no canonical-path, symlink, root-containment or allowlist check is visible in these handlers. Write creates parent directories and opens `FileOutputStream`; read materializes file content into a variable; recursive listing traverses descendants and accumulates names in a list. No byte/count/depth budget is visible. The class Javadoc explicitly documents absolute paths and full-file in-memory reads. This is unrestricted host filesystem access under the current default binding, not a sandbox. |
| `include` / `IncludePlugin` | `IncludePlugin.java` blob `ab06127f7f5402978937160a42455b4c0107dfab` | `doExecute` evaluates the configured path, obtains the active `ConfigSource`, resolves an included source through `parentConfig.getConfigSource().include(location)`, creates/reloads a new `Config`, swaps `context.config`, executes included children using a new `DefaultPluginBodyExecutor`, then restores the parent config in `finally`. No explicit include depth/cycle guard or filesystem-root policy appears in this method. The underlying `ConfigSource.include` implementation and executor registry-generation behavior remain to be traced before concluding whether another layer supplies those guarantees. |
| `script` / `ScriptPlugin` | `ScriptPlugin.java` blob `0bd1992d0739f43c9d550b302d4fa0a815e63995` | `doExecute` evaluates the body into source text, selects a language from configuration/context, and calls `scriptEngineFactory.getEngine(...).evaluate(context, scriptSource)`; if injection is absent it falls back to `BaseTemplater.evaluateToVariable`. The execution API receives the full `DynamicScopeContext`; no permission context, host-object allowlist, CPU/wall-time budget, memory budget or cancellation token is passed at this call site. Treat this as trusted-code execution in the host process unless a separate process boundary is introduced; language selection is not isolation. |
| `xslt` / `XsltPlugin` | `XsltPlugin.java` blob `c94b8e26dba566cb30bc12f6fc811bb1fcdc59b1` | `doExecute` creates a `TransformerFactory`, enables JAXP secure processing, rejects external stylesheet/document resolution via `URIResolver`, and parses XML through a SAX reader with DOCTYPE and external entities disabled. This is positive explicit XXE/external-resource hardening in the inspected path. However, transformation input/output is held in strings/StringWriter and no explicit input/output-size, CPU, wall-time, recursion or cancellation budget is visible. Preserve the existing external-resource denial while adding bounded execution policy; do not label this provider wholly unprotected. |

**Cross-cutting implications**

1. A working-directory string is not a filesystem capability. Replace direct `File` operations with a host-owned filesystem service that canonicalizes paths, rejects traversal/symlink escapes, applies read/write/list permissions and enforces byte, entry and recursion budgets.
2. Include resolution is a configuration-source capability. Its resolver must apply the same filesystem/network policy as the host, enforce cycle/depth and aggregate-size limits, and bind included execution to the same pinned registry generation as its parent session. This generation property is not established by the current method-level evidence.
3. Script execution must be an explicit host policy decision. Descriptor discovery and semantic inspection must never instantiate/evaluate scripts. In-process scripting is trusted code, not a sandbox; resource limits that cannot be reliably enforced in-process require process isolation.
4. XSLT currently has a meaningful secure-processing and external-resource denial path. Keep these guarantees as regression requirements, then add bounded input/output and execution controls at the transformer/runtime boundary.
5. The findings above are not test results. Existing tests were not run in this audit; targeted positive and negative tests must be mapped before any implementation.

**Evidence boundary:** the source blobs above were fetched from the current `main` tree. No runtime source, tests, POMs or behavior changed in this pass.


### 15.12 Include source-resolution follow-up (2026-11-23)

The underlying configuration-source layer was traced one level further:

- `AbstractConfigSource.include(Location)` creates an `IncludeVisitor` from the location string and delegates to the concrete source's `visit` method (blob `61a59ff096c62e37a7d4c6445e212da868a49bff`). It contains no visible cycle/depth, root-containment or destination-policy enforcement itself.
- `FileConfigSource.FileLocation.getReader()` opens the configured file with `FileInputStream` and UTF-8 decoding (blob `60313fb8f21db7630fa66dacf7a2dcfd4871eb98`). No canonicalization or symlink/root check is present in this class.
- `URLConfigSource.URLLocation.getReader()` calls `url.openStream()` and decodes UTF-8 (blob `1579ea14f6e2f7b187cb88c65e97dd321c8a2e23`). This class exposes no explicit connect/read timeout, response-size bound, redirect validation or destination allowlist.

The exact `IncludeVisitor` construction/dispatch policy and its relative-location semantics still need a dedicated call-site trace; do not infer that relative includes are constrained to the parent directory merely from the visitor abstraction. The evidence now establishes that both file and URL source implementations are capable of opening their respective underlying resources without policy parameters at this layer.

**Design consequence:** included configuration loading must be routed through the same host-owned, policy-aware resource resolver as ordinary file/HTTP operations, with explicit source-type permissions, canonical path/destination checks, timeouts, byte limits, cycle/depth limits and execution-generation pinning. This remains an audit finding, not an implementation authorization. Tests were inspected only where noted; none were run in this pass.



### 15.13 Include visitor dispatch and recursive-cycle test gap (2026-11-23)

The relative-source dispatch was traced to the concrete visitor implementation. This closes the source-resolution question from section 15.12, but does not establish a containment or recursion policy.

| Evidence | Exact behavior | Finding |
|---|---|---|
| `IncludeVisitor.java`, blob `5794e81d1d699e416183a6a0101d16598d03cd1e` | For URL sources, `visit(URLLocation)` calls `CommonUtil.fullUrl(location.toString(), path)` and constructs a new `URLConfigSource`. For file sources, `visit(FileLocation)` adapts the parent path, takes its last slash-delimited segment, calls `CommonUtil.getAbsoluteFilename(workingPath, path)` for a relative child, then constructs a new `FileConfigSource`. | Relative paths are contextual to the including source. The visitor itself does not canonicalize the resolved file, reject absolute/traversal paths, resolve/check symlinks, validate URL destinations or redirects, or carry a host policy context. The helper methods' normalization semantics are not a security boundary. |
| `AbstractConfigSource.java`, blob `61a59ff096c62e37a7d4c6445e212da868a49bff` | Each include creates a fresh visitor and delegates to the current source. | No include-chain state, cycle detection or depth accounting is passed through this abstraction. |
| `IncludePlugin.java`, blob `ab06127f7f5402978937160a42455b4c0107dfab` | Each include creates/reloads a new Config and recursively executes its children using `DefaultPluginBodyExecutor`; parent Config is restored in `finally`. | A→B→A or self-include can recursively re-enter this path. No explicit cycle/depth guard is visible in the traced call path. |
| `IncludePluginTest.java`, blob `65ce0add3fb45651913f9dea3000b1ec32f61b9a` | Tests cover nested relative file includes, nested HTTP includes, shared variables, missing/malformed sources, parent restoration, interruption and fatal-error propagation. | The inspected tests do not cover direct self-include, indirect cycles, maximum depth, repeated include budgets, absolute-path escape, symlink escape, redirect-to-private destinations, URL timeouts or aggregate byte limits. |
| `IncludePluginRuntimeContractIntegrationTest.java`, blob `ab6bfdc8d4750aa6c4072d2eb3aeaf1c3e859ea2` | Exercises a single file include through the standard XML loader/Harvester path. | Positive runtime contract only; no recursion or policy-negative assertion. |

**Conclusion:** the current include mechanism supports contextual relative resolution for both file and URL sources, but it does not expose a policy-aware resolver contract or an include-chain budget in the inspected path. Recursive include is explicitly described as supported in the plugin Javadoc, so cycle detection and bounded depth are required correctness and availability controls, not merely hardening. A future resolver should track canonical source identity across the active include chain (with URL normalization rules), enforce maximum depth and aggregate source bytes, and apply the same host filesystem/network destination policy as direct resource access. Repeated non-cyclic includes may remain valid subject to explicit aggregate budgets.

**Required conformance cases before release:** self-cycle; two-file cycle; longer cycle; valid repeated include; depth boundary and one-over-boundary; file traversal and symlink escape; URL redirect to disallowed/private destination; slow/unresponsive URL; oversized included source; deterministic failure with parent context restoration. Tests were inspected but not executed. No runtime code or tests changed.


### 15.14 Include path helper semantics (2026-11-23)

A final helper-level check inspected `CommonUtil.java` (blob `70b5297bdd1d11f2571ce6c367764c04b6ba4222`) to avoid overstating the path behavior:

- `adaptFilename` only replaces backslashes with forward slashes.
- `getAbsoluteFilename(workingPath, filePath)` returns an absolute input unchanged; otherwise it concatenates the working path and child path. It does not collapse `..` segments or canonicalize the resulting filesystem path. Therefore relative traversal and absolute-path escape are directly possible at this helper boundary; symlink escape also remains unchecked by the subsequently constructed `FileConfigSource`.
- `fullUrl(pageUrl, link)` resolves relative and absolute-path URL references and normalizes literal `.`/`..` path segments. That normalization is URL syntax handling, not destination authorization: full URLs are returned unchanged, and no scheme/host/IP/port/redirect policy is applied here. Encoded path variants and redirect targets require enforcement at the actual connection boundary.
- `IncludeVisitor` uses these helpers exactly as described in section 15.13.

This narrows the earlier finding: URL dot-segment normalization exists; file path canonicalization/containment does not. Neither helper supplies the host-policy contract required by the dynamic platform. No runtime changes or tests were run.
