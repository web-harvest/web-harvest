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


### 15.15 Include execution also exposes a Core-to-Guice construction seam (2026-11-23)

The same Include path provides a concrete example for the container-neutral embedding work in #346:

- `IncludePlugin.java` (blob `ab06127f7f5402978937160a42455b4c0107dfab`) imports Guice `@Inject` and uses field injection for `ConfigFactory`.
- Its `doExecute` method cannot create/reload the included configuration without that injected factory and deliberately throws an `IncludeExecutionException` explaining that the configured Guice injector is required.
- `DefaultConfigFactory.java` (blob `64e3b3ad284999dcf55d5aa4a71f380293f3ccf9`) itself imports Guice `@Inject`, `Provider` and `@Singleton`; it obtains a `ConfigParser` through a Guice Provider.

**Conclusion:** the current Include execution path is not independently constructible through a plain-Java host composition path. This is a real Core migration seam, not proof that Guice must remain the public contract. The neutral runtime should own a configuration factory/parser service contract; plain Java composition should be able to provide it directly, while Guice/Spring adapters may bind it through their native container. Provider construction should not rely on private-field injection as the only supported path. Add a no-IoC embedded Include execution test to the container-neutral conformance suite, alongside equivalent Guice and future Spring adapter tests. No implementation or tests changed/run.


### 15.16 Core IoC dependency seam inventory (2026-11-23)

This pass distinguishes neutral interfaces from Guice-bound implementation/composition classes. It is a source audit, not an instruction to remove Guice annotations mechanically.

| Type | Source blob | Current dependency shape | Container-neutral migration target |
|---|---|---|---|
| `ConfigFactory` | `37af0cef66db0ee17311ca2d73b63ea59bd2605e` | Interface has no Guice imports or container types. | Keep as a neutral runtime service contract; make construction available through the host composition API. |
| `ConfigParser` | `b434357f0c929a5deaf790b595fe6c509f5456e6` | Interface has no Guice imports or container types. | Keep parser contract neutral; adapter supplies an implementation/factory. |
| `DefaultConfigFactory` | `64e3b3ad284999dcf55d5aa4a71f380293f3ccf9` | Imports Guice `@Inject`, `Provider`, `@Singleton`; constructor accepts `Provider<ConfigParser>`. | Separate neutral implementation from Guice binding; use explicit constructor/factory dependency. |
| `ScraperModule` | `d31a5e30b0949c752743123cbcfb4219ba60713e` | Extends Guice `AbstractModule`; owns bindings, custom scopes, static injection and singleton/scoped registrations. | Retain as optional Guice adapter/composition module, not as Core's only composition path. Extract neutral service graph and lifecycle ownership. |
| `AttributeHolderScope` | `11a39c99e517ae9ae1ba09cf69a885b636465b64` | Implements Guice `Scope` and `Provider<AHT>`; thread-local/nested scope entry and exit are coupled to Guice provider wrapping. | Separate execution-context storage/lifetime primitive from Guice Scope adapter. Preserve nested-scope semantics and explicitly define concurrency/thread handoff behavior. |
| `DefaultHarvesterFactory` | `58bae4e22a82d89dac1c6e63e28ce5f3587e687e` | Guice `@Singleton`, constructor takes `Provider<WebScraper>`, `Provider<EventBus>` and `ScraperScope`; creates `ScrapingHarvester`. | Neutral factory receives explicit runtime-owned providers/services and scope/lifecycle abstraction; Guice adapter maps native providers to it. |
| `ScrapingHarvester` | `2af32ca67c235e075c8cc613118814d54e555338` | Imports Guice `@Inject`/`Provider`; directly depends on `ScraperScope` and `ScopeAttributeHolder`; enters/exits scope around execute. | Execution/session lifecycle must be expressed without Guice types. Keep session scope cleanup deterministic and prevent session close from disposing host/runtime services. |
| `PluginDependencyInjector` | `12d72a925ed81cf34ce2c47de9020042749797b0` | Public interface itself is container-neutral; accepts dependency types and registered `DependencyProvider` abstractions. | Candidate bridge for provider construction, but validate its semantics against generation pinning, host-owned service lifetime, dependency identity and cleanup before adopting it as the SPI. |

**Key distinction:** the current code already contains neutral seams (`ConfigFactory`, `ConfigParser`, `PluginDependencyInjector`) alongside Guice-specific implementations and runtime composition (`DefaultConfigFactory`, `ScraperModule`, `AttributeHolderScope`, `DefaultHarvesterFactory`, `ScrapingHarvester`). The migration is therefore not “replace Guice with Spring”; it is to make the service graph, scopes and ownership explicit and neutral, then retain Guice as one adapter. Spring should map to the same contracts, not introduce a second independent runtime container.

**Required design follow-up before implementation:**
1. Specify neutral runtime construction and close semantics, including host-owned vs runtime-owned services.
2. Specify session scope without thread-local/container assumptions leaking into the public contract; define behavior for async execution and thread handoff before selecting an implementation.
3. Specify provider/dependency resolution for plugin generations, including constructor injection and deterministic close ordering.
4. Prove a Core-only plain-Java host can create a runtime, execute a Core workflow containing `include`, close the session, and close the runtime without Guice on the classpath.
5. Run the same neutral conformance fixture through the Guice adapter; add Spring only when that adapter is declared supported.

**Evidence boundary:** source blobs were fetched from current `main`. No runtime code, tests, POMs or dependency declarations changed. Tests were not run in this audit.


### 15.17 Core runtime, metadata scanner and audit-test reconciliation (2026-11-23)

The current `main` sources establish three distinct inventories that must not be conflated:

| Surface | Source-backed count/behavior | What it proves |
|---|---|---|
| Executable default XML registry | `CorePluginsRegistrar` manually registers 44 element mappings, including legacy `set-var`; `CorePluginsRegistrarContractTest` independently asserts the expected mapping set. | These names are executable through the default Core registry when this registrar runs. |
| Metadata scanner successful path | `PluginClassScanner` scans six hard-coded package roots and returns concrete annotated `Plugin` class names; successful results are cached. | It discovers metadata candidates from the current classpath packages. It does not activate/register XML runtime elements. |
| Metadata scanner fallback | `createFallbackList()` contains 43 Core class names plus 10 optional module class names; the Core list omits `SetVarPlugin`, although the runtime registrar exposes `set-var`. | Fallback metadata discovery is not parity-equivalent to the executable registry. |
| Metadata completeness audit | `AllPluginsMetadataAuditTest` maintains a separate manually enumerated Core class list and explicitly excludes deprecated `VarPlugin`/`SetVarPlugin`. It instantiates classes through no-arg constructors and checks metadata/schema fields. | Metadata shape only for that manually selected set; not runtime registration, provider construction, host dependency injection, or XML execution. |

Exact source references:
- `CorePluginsRegistrar.java`: `7ddc5b26a33c5143ed893a513263718990fa515e`.
- `PluginClassScanner.java`: `dadd28a434000816253ac66bff736e2c5fff0494`.
- `CorePluginsRegistrarContractTest.java`: `bda7938a517bccc78150932711355035c5b4d1eb`.
- `AllPluginsMetadataAuditTest.java`: `333180663b23346d604c070f04123624df904dfc`.

**Classification consequence:** do not mechanically equate every annotated concrete Core class with an independent public capability. `ConfigPlugin` is a configuration/root processing concern; `BodyProcessorPlugin` is an internal body-processing mechanism; `VarPlugin` is legacy syntax superseded by `def`; `SetVarPlugin` is different: it is explicitly retained as a runtime XML compatibility mapping, even though the metadata audit excludes it and the scanner fallback omits it. This is an unresolved product classification and test-contract decision, not authorization to remove or rename any element.

**Required inventory/test work before #93 closure:**
1. Maintain one source-of-truth classification table that separately records executable XML mapping, descriptor/metadata discovery, public semantic capability, compatibility alias/deprecation state and test evidence.
2. Add a parity contract between the default runtime registry and descriptor/catalog projection; explicitly state whether legacy `set-var` remains discoverable and inspectable.
3. Test scanner fallback parity against the intended descriptor inventory (not merely non-empty fallback behavior).
4. Test every retained public provider through the actual runtime construction path, including providers requiring injected services; no-arg metadata instantiation is not sufficient.
5. Classify `ConfigPlugin` and `BodyProcessorPlugin` as internal execution infrastructure unless a separately justified public capability contract is approved.
6. Preserve `VarPlugin` deprecation semantics and `SetVarPlugin` XML compatibility until a maintainer-approved migration plan exists.

**Evidence boundary:** this is a source/test inventory only. No production source, tests, plugin registration, XML behavior or POMs changed; no tests were run.


### 15.18 Resolution of the three non-mapped Core classes (2026-11-23)

This pass resolves the earlier open questions for `BodyProcessorPlugin`, `ConfigPlugin` and `VarPlugin` using the current 2.3.x runtime and retained 2.2.0 documentation.

| Class / element | Current runtime evidence | Documentation/test evidence | Inventory disposition (evidence-based; not a maintainer product approval) |
|---|---|---|---|
| `BodyProcessorPlugin` / `body-processor` | Not in the 44-entry registrar. Runtime execution now calls `PluginBodyExecutor.executeBody(...)` directly; root execution uses `RootConfigurationExecutor`. | Retained `docs/web/documentation/2.2.0/body-processor.html` explicitly says “Removed in 2.3.x” and explains direct `executeBody(...)` invocation. | Historical internal implementation, not a 2.3.x public XML capability. Keep out of runtime descriptor/public semantic inventory; retain only historical documentation as clearly versioned. |
| `ConfigPlugin` / `config` | Not in child-plugin registrar. Root `<config>` is executed by package-private `RootConfigurationExecutor`, which applies charset/script language and delegates children to `PluginBodyExecutor`. | `RootConfigurationExecutorBranchContractTest` tests root settings, fallback behavior, child synchronization and body execution. | Root document/runtime construct, not an ordinary discoverable child plugin. Represent root configuration as a runtime/document contract, not as a provider in the dynamic plugin catalog. |
| `VarPlugin` / `var` | Not in default registrar. | Class Javadoc describes it as deprecated and recommends `def`, but also calls it a legacy syntax maintained for compatibility; test filename exists but does not establish default runtime enablement. | Deprecated, currently non-default implementation. Do not advertise as enabled; any compatibility reactivation requires an explicit decision and dedicated XML execution fixture. |
| `SetVarPlugin` / `set-var` | Explicitly present in default 44-entry registrar and protected by `SetVarCompatibilityRegistrationContractTest`. | Class Javadoc says deprecated but retained for 1.x XML compatibility; metadata audit omission is therefore not authoritative. | Enabled legacy compatibility capability. Keep in runtime and descriptor/inspection parity until an approved migration/deprecation policy says otherwise. |

Source references:
- `RootConfigurationExecutor.java`: `46fed681684797a826eca230cd7f3369f7d2a28b`.
- `RootConfigurationExecutorBranchContractTest.java`: `2d21cc6593cb1d493eecd03f756d0e21461986f3`.
- `docs/web/documentation/2.2.0/body-processor.html`: `65d4d1a4f7452df0288686adb8ba5844baf44c7e`.
- `VarPlugin.java`: `37bbbad754955c2bda0d054f0fd1b3fa6b0e99f5`.
- `SetVarPlugin.java`: `9f941b46a7da54b4103734896f9ad78041e57e44`.
- `SetVarCompatibilityRegistrationContractTest.java`: `db1c3af911ae5e1413aa5fc589d4b8e55129eb6b`.

**Important distinction for the future SPI:** root configuration and body execution are runtime protocol, not provider discovery. A future dynamic catalog must not manufacture descriptors for internal framework machinery or root document nodes. Conversely, the legacy `set-var` mapping is genuinely executable and must not disappear due to scanner fallback or metadata-audit omissions. `var` has legacy source documentation but no default registration; compatibility support must be described honestly as not enabled by default.

**No implementation decision:** no class was deleted, moved, or modified; no XML behavior changed; no tests were run. Maintainer approval remains required for product classification and compatibility policy.


### 15.19 Default Core mapping-to-test inventory (2026-11-23)

The 44 default mappings were reconciled mechanically against the current `AllPluginsMetadataAuditTest` source and the current Git tree of Core test sources. The table records test-file presence only; it does **not** claim that every branch, XML execution path, or dynamic provider lifecycle is covered.

| XML element | Registered class | Included in metadata audit | Class-named test source |
|---|---|---:|---:|
| `call` | `CallPlugin` | Yes | Present |
| `call-param` | `CallParamPlugin` | Yes | Present |
| `case` | `CasePlugin` | Yes | Present |
| `catch` | `CatchPlugin` | Yes | Present |
| `constant` | `ConstantPlugin` | Yes | Present |
| `def` | `DefPlugin` | Yes | Present |
| `else` | `ElsePlugin` | Yes | Present |
| `empty` | `EmptyPlugin` | Yes | Present |
| `exit` | `ExitPlugin` | Yes | Present |
| `file` | `FilePlugin` | Yes | Present |
| `function` | `FunctionPlugin` | Yes | Present |
| `get` | `GetVarPlugin` | Yes | Present |
| `html-to-xml` | `HtmlToXmlPlugin` | Yes | Present |
| `http` | `HttpPlugin` | Yes | Present |
| `http-header` | `HttpHeaderPlugin` | Yes | Present |
| `http-param` | `HttpParamPlugin` | Yes | Present |
| `if` | `IfPlugin` | Yes | Present |
| `include` | `IncludePlugin` | Yes | Present |
| `json-to-xml` | `JsonToXmlPlugin` | Yes | Present |
| `loop` | `LoopPlugin` | Yes | Present |
| `regexp` | `RegexpPlugin` | Yes | Present |
| `regexp-pattern` | `RegexpPatternPlugin` | Yes | Present |
| `regexp-result` | `RegexpResultPlugin` | Yes | Present |
| `regexp-source` | `RegexpSourcePlugin` | Yes | Present |
| `return` | `ReturnPlugin` | Yes | Present |
| `script` | `ScriptPlugin` | Yes | Present |
| `list` | `ListPlugin` | Yes | Present |
| `log` | `LogPlugin` | Yes | Present |
| `value-of` | `ValueOfPlugin` | Yes | Present |
| `xml` | `XMLPlugin` | Yes | Present |
| `set-var` | `SetVarPlugin` | **No** | Present |
| `sleep` | `SleepPlugin` | Yes | Present |
| `stylesheet` | `StylesheetPlugin` | Yes | Present |
| `template` | `TemplatePlugin` | Yes | Present |
| `text` | `TextPlugin` | Yes | Present |
| `tokenize` | `TokenizePlugin` | Yes | Present |
| `try` | `TryPlugin` | Yes | Present |
| `while` | `WhilePlugin` | Yes | Present |
| `xml-to-json` | `XmlToJsonPlugin` | Yes | Present |
| `xpath` | `XPathPlugin` | Yes | Present |
| `xquery` | `XQueryPlugin` | Yes | Present |
| `xq-expression` | `XQExpressionPlugin` | Yes | Present |
| `xq-param` | `XQParamPlugin` | Yes | Present |
| `xslt` | `XsltPlugin` | Yes | Present |

**Findings:**
- The runtime registrar has 44 mappings; the metadata audit enumerates 43 classes and omits only `SetVarPlugin`.
- All 44 mappings have at least one class-named test source in the current Core test tree. This is a useful baseline, not a passing test result and not proof of end-to-end runtime construction for each mapping.
- The focused XML integration suite `CorePluginsIntegrationTest` directly exercises only a subset (constant, text, def, set-var, get, file, sleep, list, value-of); other plugin-specific and grouped integration tests exist separately.
- `CorePluginAnnotationTest` explicitly checks category annotation values for only seven classes (HTTP, XPath, Def, Loop, Script, File, Log). It is not a completeness/parity contract for all 44 mappings.
- Existing schema branch tests exercise some detail-node schemas independently. In particular, `DetailPluginSchemaBranchContractTest` covers HTTP parameter/header, XQuery parameter, template, value-of, config and call schemas; `NestedPluginSchemaBranchContractTest` covers HTTP, while, and text schemas; `CoreSchemaExtrasBranchContractTest` covers selected value/get/constant/text/sleep/catch/else/log/def/header schemas. Schema validation in isolation does not prove the parent-child execution contract.

**Descriptor classification warning:** the 44-entry registry is an XML execution inventory, not a list of 44 independently installable extension capabilities. Several entries are subordinate XML constructs:
- `call-param` belongs to function-call configuration.
- `http-header` and `http-param` configure an HTTP request.
- `regexp-pattern`, `regexp-source`, and `regexp-result` are detail constructs of regexp processing (their class documentation explicitly identifies the regexp parent).
- `xq-expression` and `xq-param` are detail constructs of XQuery processing (their class documentation identifies the xquery parent).
- `stylesheet` is a detail construct of XSLT processing (its class documentation identifies the xslt parent).

These may need descriptors/schema fragments for validation and semantic projection, but must not automatically become independent top-level capabilities or separately loadable providers. Their parent-child contract belongs in the descriptor model. For `http-header`/`http-param` and `call-param`, parent binding should be confirmed from parser/runtime behavior before finalizing descriptor constraints; this pass establishes their role as request/call configuration constructs, not an independent external integration.

**Next evidence gate:** trace the parser/validator/runtime handling for each subordinate construct and verify parent constraints and nested execution with source-backed fixtures. Then build the authoritative table across Core and optional modules, with separate columns for runtime mapping, discovery, descriptor, public semantic capability, compatibility, construction dependencies, and test level. Do not treat file names as test execution or test coverage percentages.

**Evidence boundary:** source/test inventory only. No source, POM, runtime behavior, tests, or XML compatibility changed; no tests were run.


### 15.20 Runtime XML tree, descriptor semantics and discovery lifecycle (2026-11-23)

This pass traced the parser, executor, metadata catalog and optional package scanner together. It identifies a larger boundary than the nested-element inventory alone.

| Path | Current implementation evidence | Consequence |
|---|---|---|
| XML parsing | `StreamingXmlParser.ConfigurationTreeHandler.startElement` deliberately sets `elementInfo = null`; the old resolver lookup is commented out. It creates `PluginConfigurationTree` nodes with `pluginClass = null` and attaches children structurally. | Parsing currently builds a permissive name/configuration tree; it does not resolve names against the runtime registry or validate descriptor parent/child constraints at parse time. |
| Runtime dispatch | `DefaultPluginBodyExecutor.createChildPlugin` resolves each child element name through global singleton `CorePluginRegistry`; missing names fail at execution. It creates via global Guice injector when present, otherwise deprecated reflective no-arg construction, then calls `initialize`. | Parse-time validity and execution-time availability are separate. A document can parse before an unknown element fails during execution. Construction is coupled to global registry and optional global injector. |
| Runtime structural checks | `DefaultPluginBodyExecutor` explicitly validates sibling `if/else` and `try/catch` pairing/order. The generic child loop does not consult each parent's `PluginConfigurationSchema.getAllowedSubElements()`, `getRequiredSubElements()` or `getRequiredParents()`. | Some structural constraints are hard-coded special cases; schema parent/child metadata is not a general runtime XML-tree validator. |
| Semantic projection | `PluginSemanticsResolver` derives allowed children and required parents from schema and visual metadata. `PluginMetadataValidator` checks consistency among descriptor objects (including required-parent/allowed-child relationships). | This validates the metadata graph, not a concrete parsed XML document's actual parent/child edges. It cannot currently guarantee that the XML tree being executed conforms to the descriptor graph. |
| Catalog construction | `PluginMetadataCatalog` builds a static final descriptor map during class initialization. It calls `PluginClassScanner.allPluginClassNames()`, loads each class, and calls its no-arg constructor to obtain metadata. | Catalog is a one-time classpath snapshot, not a live view. Metadata discovery constructs plugin instances; descriptor inspection is not strictly side-effect-free/provider-free. |
| Classpath scanner | `PluginClassScanner` scans six hard-coded package roots and caches successful results. Its fallback is a static list. `clearCache()` clears only class-name cache. | It cannot discover an artifact added to a managed plugin directory after startup and cannot refresh the already initialized static catalog. |
| Opt-in package scanner | `PluginPackageScanner` scans only explicitly supplied packages or the `webharvest.plugin.packages` system property, then immediately registers annotated classes into `CorePluginRegistry`. It is documented as opt-in and is not part of default registration. | This is a package-to-global-registry helper, not an autonomous managed-artifact lifecycle, descriptor-first discovery, staged reconciliation or generation-aware activation mechanism. |

Exact current-main source blobs:
- `StreamingXmlParser.java`: `faad2801021009082cf8f2b72d3543f70aad1353`.
- `PluginConfigurationTree.java`: `bf84a746bb8bd7210b63fc194347917e40f2c017`.
- `DefaultPluginBodyExecutor.java`: `0116211ed31986b757f6f9e4bb5689f199b54f69`.
- `PluginConfigurationSchema.java`: `c649a8a2d58c496bf913a8a16ac989cab81a6e98`.
- `PluginSemanticsResolver.java`: `5590d9642e1dc2b1890a24b6caf1c2d19b07346d`.
- `PluginMetadataValidator.java`: `8230e1a6167a189feff0fe5fb3bcdb0cd0af3732`.
- `PluginMetadataCatalog.java`: `5ac82d7fb6436b458b29647e4c929878557b1c6e`.
- `PluginClassScanner.java`: `dadd28a434000816253ac66bff736e2c5fff0494`.
- `PluginPackageScanner.java`: `c0e6cbff6cb8c472f66c4a897263cb3c3c7f8232`.
- `CorePluginRegistry.java`: `20f478addbf2bacc481e4e11e0328f06942a2a38`.

**Architecture consequence for #346–#350:** the target cannot be achieved by replacing package names in `PluginClassScanner` or adding a JAR watcher around `PluginPackageScanner`. The new platform needs one authoritative, versioned registry generation from which both parsing/validation and execution resolve the same provider identities and structural semantics. Parse/inspect/validate must use descriptors without constructing providers; execution must pin the same immutable generation for its session and obtain provider instances through the neutral host composition/lifecycle contract. Dynamic updates must publish a new generation atomically while existing sessions retain their pinned generation.

**Migration seams (design only):**
1. Replace global `CorePluginRegistry` as the semantic/runtime authority with an instance-owned generation registry; retain an adapter for legacy static APIs only during migration.
2. Make XML parsing resolve element names and namespaces against a descriptor snapshot, preserving a useful tree and source locations while reporting unknown/unauthorized/structurally invalid elements before execution.
3. Apply `allowedChildren`, `requiredParents`, required/allowed child rules and special control-flow pairing as one deterministic tree-validation pass; do not rely on ad-hoc executor checks as the only structural guard.
4. Replace eager `newInstance()` catalog projection with immutable descriptor/provider metadata that can be read without constructing plugin implementations.
5. Make managed artifact source discovery, validation, host authorization and activation separate phases. A newly discovered provider may be described without being activated.
6. Ensure inspect/semantic/schema responses carry registry generation identity and execution rejects or revalidates stale validated trees against the generation it pins.

**No implementation or behavior change:** this is source-backed architecture evidence only. No code, tests, POMs or runtime registration changed; no tests were run.


### 15.21 XML namespace identity and registry collision behavior (2026-11-23)

The parser preserves namespace information in each `DefaultPluginConfiguration`, but current runtime dispatch does not use it:

- `StreamingXmlParser.ConfigurationTreeHandler.startElement` normalizes the XML namespace and stores it in the configuration. It separately stores `localName` as the plugin element name.
- `CorePluginRegistry` is a process-global singleton whose map is keyed only by `String elementName`, not by namespace plus local name.
- `registerMapping` performs an unconditional `ConcurrentHashMap.put(elementName, pluginClass)`. A second registration for the same local name silently replaces the earlier class; there is no duplicate-owner/collision error or provenance record.
- `DefaultPluginBodyExecutor.createChildPlugin` looks up only `childConfig.getElementName()`; it does not pass or compare the stored namespace.
- The current test tree contains no dedicated CorePluginRegistry namespace/collision contract test.

**Consequences:** the runtime's actual identity is currently local-name-only even though XML configuration stores a namespace. Two providers claiming the same local name cannot coexist under distinct namespaces in the current registry; later registration wins. This is incompatible with a robust external plugin platform where independent vendors/modules may legitimately use overlapping local names.

**Required target contract:** use a canonical qualified element identity (at minimum namespace URI + local name, with explicit namespace/version compatibility rules), reject ambiguous duplicate ownership during candidate-generation validation, and preserve provider/artifact provenance in diagnostics. Any legacy namespace normalization/alias behavior must be explicit and tested; do not silently resolve collisions by registration order.

Source blobs:
- `StreamingXmlParser.java`: `faad2801021009082cf8f2b72d3543f70aad1353`.
- `CorePluginRegistry.java`: `20f478addbf2bacc481e4e11e0328f06942a2a38`.
- `DefaultPluginBodyExecutor.java`: `0116211ed31986b757f6f9e4bb5689f199b54f69`.

No implementation or tests changed/run.


### 15.22 Parent/child contract trace: function-call and HTTP detail elements (2026-11-23)

This pass traced the four detail implementations against their parent metadata, schema declarations and parent execution code. It confirms the detail-element classification, but also exposes inconsistent metadata and incomplete child-plugin behavior.

| Detail element | Declared parent contract | Parent-side execution evidence | Finding |
|---|---|---|---|
| `call-param` | `CallParamPluginMetadata.getParentRelationship()` says required parent `call`; its schema `getRequiredParents()` returns both `call` and `def`. `CallPluginMetadata` allows `call-param` children. | `CallPlugin` explicitly iterates direct `call-param` children to collect argument values. In function bodies, examples place `call-param` inside `def`; `CallParamPlugin.doExecute` reads the corresponding `callParam<Name>` variable from the function context and otherwise evaluates its body. | One XML element serves two distinct contexts: argument declaration under `call`, and parameter lookup under `def` inside a function. The schema comment says the latter requires `def` within `function`, but `getRequiredParents()` alone cannot express that ancestor condition. Visual parent metadata currently describes only `call`, so it is incomplete. Do not flatten this to one simple required-parent edge. |
| `http-header` | Metadata relationship and schema both require parent `http`; parent `HttpPlugin` metadata/schema allow `http-header` and `http-param`. | `HttpPlugin.collectHeaders()` reads direct child configuration nodes named `http-header` and extracts their `name` and `_textContent` values before invoking the HTTP service. | This is declarative request configuration consumed by the parent, not an independent request capability. |
| `http-param` | Metadata relationship and schema both require parent `http`; parent `HttpPlugin` metadata/schema allow both HTTP detail elements. | `HttpPlugin.collectParameters()` reads direct child configuration nodes named `http-param`, extracts `name` and `_textContent`, and appends encoded query parameters to the request URL. | This is declarative request configuration consumed by the parent, not an independent request capability. |

**Important implementation discrepancy for HTTP detail classes:** both `HttpHeaderPlugin.doExecute()` and `HttpParamPlugin.doExecute()` contain TODO/simplified placeholder behavior rather than attaching values to a parent request object. The active `HttpPlugin` path independently consumes the raw child configuration. Therefore the parent configuration is the demonstrated source of request headers/parameters; the child implementation classes must not be mistaken for independently functioning integrations. Their standalone execution semantics and whether they should be instantiated at all require an explicit compatibility decision and targeted tests.

**Descriptor/SPI implications:**
- Model detail constructs as descriptor/schema elements with explicit parent-context constraints, not automatically as separately installable top-level capabilities.
- Support context-sensitive constraints (for example `call-param` under `call` versus `def` nested within `function`); a flat list of parent local names is insufficient to encode the documented rule.
- Keep parent consumption semantics and child implementation lifecycle distinct. A descriptor may describe a declarative child that is interpreted by its parent without creating a provider instance.
- Validate the actual XML tree against the same descriptor generation used for execution. Current generic executor behavior and metadata validation do not establish this full-tree contract.

Source blobs on current `main`:
- `CallParamPlugin.java`: `4681032a90e27e77c818cacc6bd6fd35ea76e31f`.
- `CallPlugin.java`: `01238d278e4bbddb262c797f34a481dcd964e3c9`.
- `HttpHeaderPlugin.java`: `eb9ebd92ac924c3e8bce58e9a2b8edf1efd93b2a`.
- `HttpParamPlugin.java`: `b20b05a335de88a1dbac6316a762b8361a4dcfb8`.
- `HttpPlugin.java`: `c6045d6c7638f3aa86c510240814633597b108ad`.

No runtime implementation or tests changed/run. This is source-level evidence; it does not claim a new end-to-end behavior test has passed.


### 15.23 Parent/child contract trace: RegExp, XQuery and XSLT detail elements (2026-11-23)

The remaining declarative detail groups were traced from their parent processors into their metadata and schema implementations.

| Parent capability | Detail elements | Parent-side behavior | Detail implementation / contract evidence |
|---|---|---|---|
| `regexp` | `regexp-pattern`, `regexp-source`, `regexp-result` | `RegexpPlugin.doExecute()` reads its direct child configurations by element name and derives pattern/source/replacement inputs. The parent schema requires `regexp-pattern` and `regexp-source`, and allows those plus `regexp-result`. | Each detail class declares a visual required-parent relationship to `regexp`; each `doExecute()` is explicitly a no-op placeholder returning `EmptyVariable`. The detail schemas do not override `getRequiredParents()`. |
| `xquery` | `xq-expression`, `xq-param` | `XQueryPlugin.doExecute()` iterates direct children, reads expression content and parameter configuration, then constructs the query inputs. Parent schema requires `xq-expression` and allows both detail elements. | Both detail classes declare visual parent relationship to `xquery`. `XQExpressionPlugin` evaluates its body; `XQParamPlugin` executes its body so nested value-producing elements (for example variable lookup) can supply parameter values. Neither detail schema declares `getRequiredParents()`; XQParam deliberately permits nested child processors for value generation. |
| `xslt` | `stylesheet` (and the `xml` input construct) | `XsltPlugin.doExecute()` obtains required direct child content by element name for `xml` and `stylesheet`; it performs the transformation itself. The parent metadata/schema allow `xml` and `stylesheet`. | `StylesheetPlugin` declares a visual required-parent relationship to `xslt`, but its `doExecute()` is a no-op placeholder and its schema does not override `getRequiredParents()`. The actual transformation consumes child content at the parent. |

**Cross-cutting discrepancy:** for these detail classes, visual `ParentChildRelationship` metadata describes a required parent, but the corresponding schema classes do not declare the same relationship through `getRequiredParents()`. This is materially different from the HTTP detail classes, whose schemas do declare `http` as a required parent. A descriptor validator that reads only schema constraints will therefore lose some currently advertised parent relationships; one that reads only visual metadata will miss required-child and nested-value semantics.

**Design consequences:**
- The future descriptor model must separate *provider capability identity* from *XML syntax/detail-node identity*. A detail node may be parsed and semantically described without being an independently activatable provider.
- Parent descriptors need typed child roles, cardinality/requiredness, value interpretation and whether a child body is evaluated, rather than only a list of child element names.
- Distinguish three patterns: parent directly interprets raw child configuration (`regexp`, `xslt`); parent interprets a child expression/value contract (`xquery`); child body is itself evaluated to produce a nested value (`xq-param`).
- Reconcile visual parent metadata and schema parent constraints before treating either as authoritative. Missing schema declarations are not proof that the child is actually standalone.
- Preserve XML compatibility while making structural validation generation-scoped and consistent with the parent executor.

Source blobs on current `main`:
- `RegexpPlugin.java`: `a860ab08554122680c4d5d5fde1be3d683139c6a`.
- `RegexpPatternPlugin.java`: `f8aae44f39deae0393ebf264156b460dc312442f`.
- `RegexpSourcePlugin.java`: `e97be0a919e78ce5f8c8efb5a9d0e673078f07313`.
- `RegexpResultPlugin.java`: `c473354f978aec5b9b6df6687dbfc8c759ba1377`.
- `XQueryPlugin.java`: `b1f0ee00c16c9a37144e7e27cb46bfd57de6e9f1`.
- `XQExpressionPlugin.java`: `70728c7b6669796a3367e276aacd6735135c0faa`.
- `XQParamPlugin.java`: `3ec7b37615a4bb3cda4614cb4a4eb94ea7236a98`.
- `XsltPlugin.java`: `c94b8e26dba566cb30bc12f6fc811bb1fcdc59b1`.
- `StylesheetPlugin.java`: `4132be73c6e010cb952aa63c7fd7d90efb83c6de`.

No runtime implementation or tests changed/run. This is a source audit, not proof of end-to-end behavior for every nested form.


### 15.24 Filesystem, include and script execution boundary trace (2026-11-23)

This pass followed three high-impact Core capabilities beyond their plugin entry-point imports and inspected the actual operations and default bindings.

| Capability | Source-confirmed execution path | Current boundary gap / implication |
|---|---|---|
| `file` | `FilePlugin.doExecute()` evaluates the configured path and dispatches read/write/append/list. Read constructs `new File(workingDir, path)` and calls `Files.readAllBytes`. Write constructs the same way, creates parent directories with `mkdirs()`, executes the body, then writes via `FileOutputStream`. List recursively collects files when configured. | The inspected path does not canonicalize/normalize against an allowed root, resolve symlinks, or enforce byte/entry limits before I/O. A configured path can therefore be interpreted relative to `workingDir` but this is not a sandbox guarantee. Reads are whole-file memory loads. Text write logs the full content at INFO; `doExecute` logs the configuration object and path. Sensitive file content/path data can leak to logs. Binary reads currently return EmptyVariable ("not implemented"). |
| `include` | `IncludePlugin` evaluates `path`, obtains the active `ConfigSource`, calls `parentConfig.getConfigSource().include(location)`, creates/reloads a new `Config`, swaps `context.config`, executes included root children with a new `DefaultPluginBodyExecutor`, then restores the parent config in finally. `AbstractConfigSource.include()` delegates to `IncludeVisitor`; file includes resolve relative paths via `CommonUtil.getAbsoluteFilename`, URL includes resolve via `CommonUtil.fullUrl` and create a new `URLConfigSource`. `FileConfigSource` opens a `FileInputStream`; `URLConfigSource` uses `url.openStream()`. | Include is both configuration composition and a new file/remote-fetch boundary. The inspected path does not show host root/destination policy, redirect validation, explicit timeouts, size bounds or an include-depth/cycle budget. `IncludeVisitor` itself contains TODOs stating that unit tests and documentation are missing. Include also relies on injected `ConfigFactory`; without it, execution fails with a message that it requires the configured Guice injector. The included children currently execute through the default executor rather than an explicitly passed registry-generation handle. |
| `script` | `ScriptPlugin` evaluates its body into source text, selects a scripting language, and delegates to injected `ScriptEngineFactory`; if the factory is absent it falls back to `BaseTemplater.evaluateToVariable`. `ScraperModule` binds `JSRScriptEngineFactory` as a Guice singleton. That factory uses `ScriptEngineManager` to discover engines by language name and caches engine instances in a thread-local cache. | Engine discovery is not a trust or permission decision. The SPI shown has no policy/capability context, allowed host-object set, execution budget or cancellation contract. The fallback path can execute without the injected factory and must be explicitly accounted for in any restricted profile. Thread-local engine reuse needs lifecycle/cleanup analysis, especially for dynamic classloader retirement; no cleanup contract is visible in the inspected factory. |

**Additional concrete observations:**
- `ScraperModule` binds `WorkingDir` to the supplied string. The annotation is a binding qualifier, not a root-aware filesystem service.
- `FilePlugin` catches broad exceptions and returns `EmptyVariable` for several failures; a future policy denial should have a distinguishable, inspectable failure result rather than being silently indistinguishable from empty output.
- `IncludePlugin` restores the previous config in a finally block (positive context-restoration behavior), but the active runtime generation is not passed explicitly to the nested executor in the inspected code.
- `ConfigSource.include()` supports both file and URL origins. This means a local include can transition to a URL only if the location-resolution contract allows it; host policy must validate the resolved target, not just the original include string.
- These findings do not prove exploitability in every deployment: effective access also depends on the host process filesystem/network permissions and configuration source. They do prove that the inspected Core path does not itself establish the required host policy boundary.

Source blobs on current `main`:
- `FilePlugin.java`: `8d78e797975ab236a47420d18f28600beaae04bf`.
- `IncludePlugin.java`: `ab06127f7f5402978937160a42455b4c0107dfab`.
- `AbstractConfigSource.java`: `61a59ff096c62e37a7d4c6445e212da868a49bff`.
- `IncludeVisitor.java`: `5794e81d1d699e416183a6a0101d16598d03cd1e`.
- `FileConfigSource.java`: `60313fb8f21db7630fa66dacf7a2dcfd4871eb98`.
- `URLConfigSource.java`: `1579ea14f6e2f7b187cb88c65e97dd321c8a2e23`.
- `ScriptPlugin.java`: `0bd1992d0739f43c9d550b302d4fa0a815e63995`.
- `ScriptEngineFactory.java`: `3aa0acd84369b6167db810200d39fc6a6c0c7765`.
- `JSRScriptEngineFactory.java`: `f99047b30d2ca455898771ae8c207fafe4f1efd7`.
- `ScraperModule.java`: `d31a5e30b0949c752743123cbcfb4219ba60713e`.

Tests were not run. The source-level TODO in `IncludeVisitor` is not a test result; no claim is made that every include scenario lacks coverage. No runtime, test or POM files changed.


### 15.25 Path-resolution helper verification (2026-11-23)

A follow-up inspected the shared path helpers used by `IncludeVisitor`, rather than inferring their behavior from the caller alone.

- `CommonUtil.adaptFilename()` only changes backslashes to forward slashes.
- `CommonUtil.getAbsoluteFilename(workingPath, filePath)` returns an absolute input path unchanged; otherwise it concatenates `workingPath + "/" + filePath`. It does not canonicalize the filesystem path, collapse `..`, resolve symlinks or enforce a root.
- `CommonUtil.fullUrl(pageUrl, link)` accepts a full URL unchanged. Relative links are combined with the base URL and dot segments are normalized, but normalization is not an outbound-destination policy and does not validate scheme/host/IP/redirect targets.
- `FilePlugin.handleFileList()` uses `File.listFiles()` and recursive `collectFiles()` when `listrecursive=true`; the inspected recursion has no depth, entry-count or output-size budget.
- The file read path uses `Files.readAllBytes`, and the write path logs full text content at INFO. These are direct memory/logging exposure points, not merely missing descriptor annotations.

This confirms the earlier section 15.24 gap more precisely: current include resolution is path joining/URL construction, not a canonical root or destination allowlist. A host-provided resolver must authorize the **resolved canonical target** (including symlink and redirect behavior), and file enumeration/read/write must apply budgets during traversal/I/O rather than only validating the initial string.

Source blob:
- `CommonUtil.java`: `70b5297bdd1d11f2571ce6c367764c04b6ba4222`.

No runtime code or tests changed/run. This source audit does not claim a particular deployment is exploitable; host process permissions still constrain the effective impact.


### 15.26 HTTP execution contract: declared options, service capability and actual behavior (2026-11-23)

The HTTP path was traced end-to-end from `HttpPlugin` through the injected `HttpService` to `HttpServiceImpl`.

**Contract mismatches confirmed from source:**
- `HttpPlugin` accepts and documents GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS and TRACE. `HttpServiceImpl.request()` implements only GET and POST; every other accepted method throws `HttpServiceException("Unsupported HTTP method: ...")`.
- `HttpPlugin` documentation advertises a `timeout` attribute (30,000 ms default) and `followRedirects`. Neither appears in the plugin configuration schema's attribute list, optional-attribute list or `doExecute()` reads. The plugin calls `httpService.get/post/request(url, body, headers)` without any timeout/redirect policy argument.
- The default `ScraperModule` binding is `HttpServiceImpl.class` in Guice Singleton scope. Its default constructor uses `HttpClients.createDefault()`. The constructor that accepts `followRedirects` is not used by that binding.
- The custom redirect constructor configures only `setRedirectsEnabled`; it does not install host policy or per-redirect destination validation.
- `HttpServiceImpl.executeRequest()` materializes the complete entity with `EntityUtils.toByteArray(entity)`; no response-byte limit is visible in the inspected path. The response is then retained as a byte array and exposed as binary or converted to text.
- `HttpServiceImpl` logs request URLs at DEBUG. `HttpPlugin` logs the first 200 characters of text responses at DEBUG. URLs can contain query credentials/tokens; response excerpts can contain secrets or personal data.
- `HttpPlugin` returns response bodies regardless of HTTP status; status is available through the response abstraction but no status-based failure policy is applied in this path.

**SPI/security implications:**
- HTTP needs a host-supplied policy-aware transport service, not merely an injectable generic client. It must authorize initial and redirect destinations, resolve and validate address classes, enforce connect/response timeouts and response-size limits while streaming, support cancellation, and define redirect/header/credential forwarding policy.
- Method support must be described by the actual transport capability contract. A schema cannot advertise verbs the selected service cannot execute.
- Credentials, query strings and response bodies require explicit redaction policy; logging must not leak them by default.
- HTTP status handling (return body vs typed non-success result vs exception) must be an explicit compatibility contract.

Source blobs on current `main`:
- `HttpPlugin.java`: `c6045d6c7638f3aa86c510240814633597b108ad`.
- `HttpService.java`: `7ef8e5f318f8dcbaf23d05f360474c6a41f27ed2`.
- `HttpServiceImpl.java`: `9e3dab1a40250f4d059ea812c74012d994873f0d`.
- `ScraperModule.java`: `d31a5e30b0949c752743123cbcfb4219ba60713e`.

No HTTP behavior tests were run; these are source-level findings and must not be represented as a passing runtime conformance test.


### 15.27 Loop, sleep, logging and execution-control effects (2026-11-23)

The remaining Core utility/control-flow paths were traced to distinguish semantic constructs from host effects and to check whether nested execution preserves runtime-generation ownership.

**Loop (`LoopPlugin`, `LoopInputSourceResolver`):**
- `LoopInputSourceResolver` recognizes a leading `list` or `xpath` child as a source and evaluates it through a newly constructed `DefaultPluginBodyExecutor`.
- `LoopPlugin` constructs another new `DefaultPluginBodyExecutor` for custom loop-body execution. These constructors do not receive an explicit registry-generation/runtime handle in the inspected call sites; they therefore rely on the existing global/default executor resolution path.
- Source collections are flattened/materialized into `List<Variable>`; inline text is split into a complete token list. The loop result is accumulated in an unbounded `StringBuilder`.
- `maxloops` defaults to 100 and limits body executions when non-negative. A negative value is accepted: with a finite source list it disables the iteration cap; without a source, available item count becomes zero. No runtime-wide execution budget is visible here.
- `filter` is explicitly logged as unsupported and ignored.
- A context-local scope is created for each iteration and the final item/index are deliberately retained in the outer context; this is relevant compatibility behavior to preserve.

**Sleep (`SleepPlugin`):**
- Parses `milliseconds` as an `int`; invalid input silently becomes zero. Positive values call `Thread.sleep(millis)`, then execute child processors.
- Interruption is propagated, which is a useful cancellation seam, but there is no visible host policy ceiling, aggregate time budget or deadline propagation in this plugin.
- A descriptor should declare a time/delay effect; the host runtime should enforce per-operation and total execution budgets without replacing interruptible cancellation.

**Log (`LogPlugin`):**
- Writes directly to process-global `System.out` or `System.err` and returns the message as a value. It does not use a host-provided logging/output service.
- This bypasses host routing, structured logging, redaction and output quotas; a multi-tenant embedding host cannot independently control plugin output through the current service surface.

**Exit (`ExitPlugin`):**
- On a true condition it posts a `ScraperExecutionExitEvent` to an injected EventBus and throws `ScraperExecutionExitException`. This is execution-control flow, not JVM/process termination in the inspected method.
- The neutral runtime contract should represent execution-scoped stop/exit outcomes explicitly and keep them distinct from host shutdown or cancellation.

**Generation/lifecycle implication:** the two Loop-created default executors are another concrete nested-execution seam, in addition to IncludePlugin. A dynamic registry cannot guarantee generation consistency if nested source/body evaluation silently resolves through a process-global registry. Every nested executor must inherit the parent session's pinned generation, host policy context, cancellation/deadline and resource budget.

Source blobs on current `main`:
- `LoopPlugin.java`: `ba91342e4e560f6bea9165bdba9b5ce4b4db82d8`.
- `LoopInputSourceResolver.java`: `97f152bd9c0eec7e41fd7e031db3f4c7c3d613f4`.
- `SleepPlugin.java`: `e6c83473ee573099f4690bafeb8f73ecf313e28a`.
- `LogPlugin.java`: `108db83ce34ffe80e18e0a7fc08d791dce207118`.
- `ExitPlugin.java`: `b197f5580c2850795aadd15bfd0850af6458f074`.

No tests were run and no runtime behavior changed. The absence of a policy hook in these inspected call sites is not a claim that the host OS cannot impose process-level limits; it means the Core/runtime API does not yet expose these as explicit per-execution host contracts.


### 15.28 IoC-neutral embedding audit: current Core is still Guice-coupled

The user requirement is an embeddable Java library that works in arbitrary host applications and does not impose an IoC container. The following source trace distinguishes the intended future contract from the current implementation.

- `DefaultHarvesterFactory` imports Guice `Provider`, `@Inject` and `@Singleton`; its constructor requires Guice providers for `WebScraper` and `EventBus`, plus `ScraperScope`. It is bound by `ScraperModule` as the `HarvesterFactory` implementation.
- `ScrapingHarvester` itself stores Guice `Provider<WebScraper>` and `Provider<EventBus>`, uses `ScraperScope`, and documents its constructor as Guice-aware. It manually enters/exits the scope around execution, but has no explicit runtime owner/close contract in this class.
- `AttributeHolderScope` directly implements Guice `Scope` and `Provider`, and its API exposes Guice `Key`, `Provider` and `OutOfScopeException`. This is not a neutral scope abstraction that a Spring/CDI/plain-Java host can implement without Guice types.
- `ScraperModule` is a Guice `AbstractModule`, registers static injection for `InjectorHelper` and `BaseTemplater`, and binds Core services, scopes, factories, HTTP transport and JSR-223 engine factory. The `HttpService` and `ScriptEngineFactory` bindings are injector singletons.
- `InjectorHelper` stores a process-static Guice `Injector`. `DefaultPluginBodyExecutor.createChildPlugin()` consults the global `CorePluginRegistry`, then asks that global injector for the plugin instance; if absent it falls back to deprecated no-arg reflective construction. It does not call the existing `PluginDependencyInjector` abstraction.
- `PluginDependencyInjector` is a potentially useful neutral seam, but `DefaultPluginDependencyInjector` is a separate field-injection container: it scans fields annotated with `@InjectDependency`, sets them accessible and injects registered singleton/provider values. It does not provide constructor injection, lifecycle scopes, close ownership or a host-container bridge. No use of it appears in the inspected default child-plugin construction path.
- `ScraperModule` also uses static injection for `BaseTemplater`, so scripting factory access is not solely per-runtime composition; the static field can couple multiple embedded runtimes in one JVM.

**Conclusion:** the present Core/runtime path is not yet IoC-neutral and does not provide a first-class plain-Java composition route. The existence of `PluginDependencyInjector` does not close this gap because the active executor bypasses it. The target neutral SPI must remove Guice types from public runtime/plugin contracts, replace global injector/static injection with explicit runtime-owned composition, and let optional adapters map neutral provider/lifecycle contracts to Guice, Spring or CDI. Host ownership and close/drain semantics must be explicit; simply swapping `@Inject` annotations or adding another container is insufficient.

Source blobs on current `main`:
- `DefaultHarvesterFactory.java`: `58bae4e22a82d89dac1c6e63e28ce5f3587e687e`.
- `ScrapingHarvester.java`: `2af32ca67c235e075c8cc613118814d54e555338`.
- `AttributeHolderScope.java`: `11a39c99e517ae9ae1ba09cf69a885b636465b64`.
- `ScraperModule.java`: `d31a5e30b0949c752743123cbcfb4219ba60713e`.
- `InjectorHelper.java`: `24c114b24a3abc079a9d447b59155e3598f5abe7`.
- `DefaultPluginBodyExecutor.java`: `0116211ed31986b757f6f9e4bb5689f199b54f69`.
- `PluginDependencyInjector.java`: `12d72a925ed81cf34ce2c47de9020042749797b0`.
- `DefaultPluginDependencyInjector.java`: `fe7aeacda9041c0aba88d4c15aea9c54d81d3431`.

No runtime or tests changed/run. This is a source audit; no plain-Java, Guice or Spring embedding conformance test has been executed in this pass.


### 15.29 HTTP redirect header forwarding and test-boundary audit (2026-11-23)

A follow-up inspected the actual `HeaderPreservingInterceptor` and the current HTTP contract tests.

- `HeaderPreservingInterceptor.process()` adds each configured default header to **every** request when that request does not already contain it. It does not inspect the request origin, redirect history, target scheme/host/port or whether the header is credential-bearing.
- `HttpServiceImpl(boolean, Map<String,String>)` installs this interceptor as the first request interceptor. Its documented use cases include Authorization and API keys; the class Javadoc explicitly says these headers are applied to all initial and redirect requests.
- This creates a confirmed cross-origin credential-forwarding hazard when a request redirects to another origin and a caller configures sensitive default headers. It is not proof that the default `ScraperModule` binding currently supplies such headers (it uses the no-arg constructor); it is a risk in the exposed constructor/service API and must be fixed before treating redirect behavior as safe.
- `HttpServiceRedirectContractTest` uses a single loopback server and a same-origin relative redirect. It asserts that a configured `X-Api-Key` is present on the redirected request. It does not test cross-origin redirects or credential stripping.
- `HttpPluginRequestContractIntegrationTest` injects a `RecordingHttpService` fake. Its PATCH test proves that the plugin forwards PATCH to an abstract service, not that the shipped `HttpServiceImpl` can execute PATCH. The unit test similarly mocks the service. This explains why the source-level mismatch between the plugin's eight advertised verbs and the concrete GET/POST-only transport is not exposed by those tests.
- No inspected HTTP service test asserts response byte limits, connect/response deadlines, cancellation, redirect destination allowlists, or private/link-local target rejection.

**Required correction/conformance:** redirect policy must be origin-aware. Authorization, cookies and configured secret headers must be stripped on cross-origin redirects by default, with narrowly scoped explicit policy for any exception. Tests need two distinct local origins (different ports are distinct origins), assert sensitive header removal, and exercise redirect chains. Transport-level method tests must instantiate the real service; plugin-level fake tests remain useful but are not transport conformance.

Source blobs on current `main`:
- `HeaderPreservingInterceptor.java`: `182181cc2652b68fbe6c769149d1bcdb622d57d1`.
- `HttpServiceRedirectContractTest.java`: `f21c8c62ccca31e4b01891970244b1d9439b1b8f`.
- `HttpPluginRequestContractIntegrationTest.java`: `6060347f1159bd07f6801d10e8dca746305c6446`.
- `HttpPluginTest.java`: `1b66e0d035c378f30ab784d848aabf7eb29fb0ef`.

Test sources were inspected, not executed. This is a confirmed code-path hazard for callers using sensitive default headers, not a claim that the no-arg default runtime binding currently injects secrets.


### 15.30 Static scripting service is process-wide, not runtime-scoped (2026-11-23)

A direct source check of `BaseTemplater` confirms that `scriptEngineFactory` is a `public static` field annotated with Guice `@Inject`; all static evaluation methods read that field. `ScraperModule` installs it through `requestStaticInjection(BaseTemplater.class)`. Therefore scripting service selection is process-wide rather than attached to a specific WebHarvest runtime/session. Two embedded runtimes in the same JVM cannot safely select independent scripting factories through this field; the most recently established static injection is shared by all callers. This is a concrete multi-runtime isolation issue, not only a stylistic preference for avoiding static state.

Source blob on current `main`:
- `BaseTemplater.java`: `dacefb8faec186176148ab2a0cbc5a538941d4c5`.

No runtime or test changes were made/run. The target is explicit runtime/session-scoped scripting service access through the neutral composition contract.


### 15.31 Remaining Core execution paths: nested dispatch and transformation/query budgets (2026-11-23)

This pass traced function invocation, branching, regular-expression result templates, XML/HTML conversion, XPath/XQuery and XSLT to identify additional runtime-generation and resource-control requirements.

**Nested executor construction (additional call sites):**
- `CallPlugin` directly constructs `CallParamPlugin` for each argument and creates a new `DefaultPluginBodyExecutor` to execute the selected function body.
- `IfPlugin` constructs a new `DefaultPluginBodyExecutor` for selected true/else branches.
- `RegexpPlugin` constructs new default executors for `regexp-result` bodies and for evaluating child text. These are additional generation-propagation seams beyond Include and Loop.
- `TryPlugin` uses `AbstractPlugin.executeBody()` and therefore the normal injected body-executor path; it is not the same direct-construction gap in the inspected implementation.
- `WhilePlugin` executes the body in a nested context, but evaluates conditions via `BaseTemplater` and `BaseTemplater.scriptEngineFactory` (the process-static service already recorded in 15.30). It caps iterations at 10,000 in the execution path; no shared deadline/aggregate resource budget is visible.

The generation-pinning rule therefore applies to **every** nested dispatch path, including function bodies, selected conditional branches, regex result templates, loop source/body, and included configurations. Child plugin construction must use the runtime's provider factory and pinned registry generation, not ad hoc `new` calls or a global registry/injector.

**Transformation/query paths:**
- `XsltPlugin` explicitly enables `XMLConstants.FEATURE_SECURE_PROCESSING`, rejects external URI resolution on both `TransformerFactory` and `Transformer`, and disables DOCTYPE/external general/parameter entities/external DTD loading in its SAX source parser. This is positive source evidence for blocking external XSLT resources and XML external entities in this specific path.
- XSLT output is accumulated in a `StringWriter`; no output-size ceiling is visible. No explicit execution deadline/cancellation hook is visible inside the transform call.
- `XPathPlugin` constructs a fresh Saxon `Configuration` and builds a document from an in-memory `StringReader`; no explicit host-controlled Saxon resource resolver, query time budget or input/output limit is visible in the inspected path. CSS selection uses Jsoup and materializes all matching text results.
- `XQueryPlugin` similarly constructs a fresh default Saxon `Configuration`, compiles and executes expressions, and builds XML node parameters from strings. No explicit host-controlled external-resource resolver, query deadline, cancellation or memory/result budget is visible in the inspected path. Do not infer exploitability solely from absence of an explicit resolver; verify the exact Saxon version defaults and supported resolver hooks before making a vulnerability claim.
- `HtmlToXmlPlugin` parses full HTML strings into Jsoup DOMs and converts to full XML strings; fallback TagSoup writes to a `StringWriter`. No visible input-size, DOM-depth, output-size or processing-time budget in these paths.
- `JsonToXmlPlugin` constructs full `JSONObject`/`JSONArray` instances and serializes full XML strings; no visible input-size, nesting-depth or output-size budget. Its JSON parse fallback currently creates a CDATA wrapper around an empty `rawContent` string rather than preserving the failed input; this is a compatibility/diagnostic behavior to test, not a security conclusion.
- `RegexpPlugin` uses Java `Pattern`/`Matcher` with caller-provided patterns and complete input strings. No regex execution deadline or safe-engine policy is visible. Catastrophic backtracking is therefore a resource-exhaustion concern for untrusted patterns/input; use bounded execution or a linear-time engine/policy rather than assuming a loop budget can interrupt a Java regex call.

**Required neutral host capabilities:** bounded transformation input/output, query execution deadline/cancellation and Saxon resource-resolution policy, HTML/JSON parse depth/size limits, regex execution policy, and a common execution budget shared with the surrounding session. XSLT's existing external-resource restrictions should be preserved as a compatibility/security invariant, not weakened during SPI migration.

Source blobs on current `main`:
- `CallPlugin.java`: `01238d278e4bbddb262c797f34a481dcd964e3c9`.
- `IfPlugin.java`: `c3f6c65078763834114989db4f57d37308e4ee17`.
- `RegexpPlugin.java`: `a860ab08554122680c4d5d5fde1be3d683139c6a`.
- `WhilePlugin.java`: `0c2aeae8e7b0ebcbc72890639dab3ad002ba7ef7`.
- `TryPlugin.java`: `f1363d5e880f424276deeabac1082750fe61a012`.
- `XsltPlugin.java`: `c94b8e26dba566cb30bc12f6fc811bb1fcdc59b1`.
- `XPathPlugin.java`: `e519eb21a2e6faeef43a9d23c219502ac7f7f2a6`.
- `XQueryPlugin.java`: `b1f0ee00c16c9a37144e7e27cb46bfd57de6e9f1`.
- `HtmlToXmlPlugin.java`: `ad3322118e453239fcf8d16646490070b2fc8ba2`.
- `JsonToXmlPlugin.java`: `b10638356f38b7a7259454c6bd72f14f0380a5dd`.
- `XMLPlugin.java`: `8a0d938bd16cbddaedcdfe832d7d5579f13636a6`.

No runtime changes or tests were made/run. XSLT hardening above is a source-level observation, not a claim of broader XML parser safety across all Core paths.


### 15.32 Exact 44-name Core registry classification (2026-11-23)

The following classification is a source-backed product proposal, not a maintainer-approved plugin removal/migration decision. It is based on the exact 44 mappings in `CorePluginsRegistrar.registerCorePlugins()` on current `main`.

| XML name | Current role | Proposed SPI/catalog treatment |
|---|---|---|
| `call` | Function invocation | Core execution capability; nested dispatch pins session generation |
| `call-param` | Function argument child | Declarative child grammar/value evaluator; not standalone capability |
| `case` | Conditional branch child | Control-flow grammar node; preserve legacy semantics |
| `catch` | Exception branch child | Control-flow grammar node; preserve error binding semantics |
| `constant` | Constant value | Core value capability |
| `def` | Variable definition/assignment | Core value/context capability |
| `else` | Conditional branch child | Control-flow grammar node; ownership/order is parent-sensitive |
| `empty` | Empty value | Core value capability |
| `exit` | Execution-scoped early exit | Core execution-control capability, distinct from runtime shutdown |
| `file` | Local file operations | Core host-effect capability; host filesystem service/policy required |
| `function` | Function definition | Declarative definition node; function body is executed through `call` |
| `get` | Variable lookup | Core value/context capability |
| `html-to-xml` | HTML normalization/conversion | Core transformation capability; bounded parsing/output |
| `http` | HTTP request | Core network capability; policy-aware transport required |
| `http-header` | HTTP request header child | Declarative child configuration; not standalone request capability |
| `http-param` | HTTP request parameter child | Declarative child configuration; not standalone request capability |
| `if` | Conditional execution | Core control-flow capability; branch dispatch generation-pinned |
| `include` | External configuration composition | Core composition capability plus file/URL host effects and nested-generation inheritance |
| `json-to-xml` | JSON conversion | Core transformation capability; bounded parse/output |
| `loop` | Collection iteration | Core control-flow capability; bounded materialization/iteration/output |
| `regexp` | Regex match/replace | Core transformation capability; ReDoS-safe execution policy |
| `regexp-pattern` | Regex pattern child | Declarative child grammar; not standalone capability |
| `regexp-result` | Regex result-template child | Declarative child grammar with nested execution; inherits parent generation/budget |
| `regexp-source` | Regex source child | Declarative child grammar; not standalone capability |
| `return` | Function return | Execution-control grammar node; meaningful within function invocation |
| `script` | JSR-223 script execution | Core script capability; host-authorized engine/object access and budgets |
| `list` | List value construction | Core value capability |
| `log` | Execution output/logging | Core host-output capability; no direct process stdout/stderr bypass |
| `value-of` | Value extraction | Core value capability |
| `xml` | XML literal/body construction | Core value/markup capability; not an XML parser by itself |
| `set-var` | Legacy variable assignment alias | Compatibility alias to preserve; migration/deprecation policy required |
| `sleep` | Execution delay | Core time effect; shared deadline and cancellation required |
| `stylesheet` | XSLT stylesheet child | Declarative child grammar; not standalone capability |
| `template` | Template interpolation | Core templating capability; runtime-scoped engine service |
| `text` | Text value | Core value capability |
| `tokenize` | Text tokenization | Core transformation capability; bounded output/list |
| `try` | Exception handling | Core control-flow capability; uses normal body executor path |
| `while` | Condition loop | Core control-flow capability; shared deadline and iteration budget |
| `xml-to-json` | XML conversion | Core transformation capability; bounded input/output |
| `xpath` | XPath/CSS query | Core query capability; bounded Saxon/Jsoup execution |
| `xquery` | XQuery evaluation | Core query capability; bounded Saxon execution and explicit resource policy |
| `xq-expression` | XQuery expression child | Declarative child grammar; required child of `xquery` |
| `xq-param` | XQuery parameter child | Declarative child grammar; parameter evaluation semantics |
| `xslt` | XSLT transformation | Core transformation capability; preserve existing external-resource restrictions |

**Boundary counts (derived from the 44 registrar statements):** 31 executable/value/query/transformation/host-effect candidates; 13 declarative branch/definition/detail/compatibility nodes. These counts are a useful discussion aid, not a final packaging decision: some control-flow nodes (`call`, `if`, `loop`, `try`, `while`, `exit`) are executable capabilities; some declarative child nodes execute value expressions; and `function` defines callable behavior. The authoritative distinction is provider identity/lifecycle vs XML role/grammar, not a simplistic “all non-root tags are plugins” rule.

**Not in the 44 mappings:** root `<config>` is handled by root configuration execution; `BodyProcessorPlugin` is an internal/removed historical class; `VarPlugin` is legacy/deprecated and not registered by the current registrar. `SetVarPlugin` is registered despite the scanner fallback metadata omission recorded in 15.19.

Source blob: `CorePluginsRegistrar.java` `7ddc5b26a33c5143ed893a513263718990fa515e`.


### 15.33 While maxloops validation/execution mismatch (2026-11-23)

A focused source/test cross-check found a concrete contract mismatch in `WhilePlugin`:
- Configuration validation accepts integer `maxloops` values from 1 through 1,000,000.
- Runtime execution clamps the effective value to `Math.max(0, Math.min(maxLoops, 10_000))`.
- Therefore values 10,001–1,000,000 pass validation but silently execute at most 10,000 iterations. The public schema/validation contract overstates the effective limit by 100x.
- The execution path defaults a missing/invalid value to 1,000; invalid values can be silently defaulted when execution is invoked without prior validation, while the validator rejects malformed integers.
- Existing `WhilePluginTest` has cases for small positive values, zero, negative and invalid input, but the inspected tests do not exercise the 10,000 vs 1,000,000 boundary. Test sources were inspected, not run.

The future semantic validator must use one canonical typed limit and report the effective policy limit before execution. Either align validation/schema to the 10,000 runtime cap or deliberately raise the runtime cap under the shared host budget; do not silently accept a higher declared limit and clamp it later. Preserve legacy behavior only through an explicit compatibility rule.

Source blobs on current `main`:
- `WhilePlugin.java`: `0c2aeae8e7b0ebcbc72890639dab3ad002ba7ef7`.
- `WhilePluginTest.java`: `85e2df9baf1f418dd61ef483b51902e0153164a3`.
- `WhilePluginEnhancedTest.java`: `85ec8fdf51f99095bfef1ff172217cd75fe78d08`.
- `WhilePluginMassiveTest.java`: `110db71e06f186cb2cbac66145ef71d7c8765a45`.
### 15.34 Active plugin construction vs the legacy project-owned injection helper (2026-11-23)

A source-level trace of the active executor, injector helper, injection interfaces and representative Core service-consuming plugins resolves an important ambiguity: the repository's PluginDependencyInjector is not the mechanism used by runtime dispatch.

**Active creation path**

DefaultPluginBodyExecutor.createChildPlugin() performs the following steps:

1. Resolve the element name through the process-global CorePluginRegistry.getInstance().
2. Read the process-static Guice injector through InjectorHelper.getInjector().
3. If non-null, create the plugin with Guice Injector.getInstance(pluginClass).
4. Otherwise call deprecated pluginClass.newInstance(), requiring an accessible no-argument constructor.
5. Call plugin.initialize(childConfig).

The active executor imports Guice Injector directly. InjectorHelper contains a static Guice Injector field, and ScraperModule requests static injection for it. This is not container-neutral host composition and is not isolated per runtime.

**The separate PluginDependencyInjector helper**

- PluginDependencyInjector exposes injectDependencies(Plugin), registerProvider(Class<T>, DependencyProvider<T>), registerSingleton(Class<T>, T) and getDependency(Class<T>).
- DefaultPluginDependencyInjector stores provider and singleton maps keyed by exact Class<?>; singleton wins over provider.
- Injection scans only plugin.getClass().getDeclaredFields(), looking for the project's own @InjectDependency; it does not traverse superclasses.
- It performs reflective field assignment only. There is no constructor injection, qualifier/key model, lifecycle/disposal ownership, or automatic integration with DefaultPluginBodyExecutor.
- Its annotation @InjectDependency is distinct from Guice @Inject.

The helper's test sources exercise basic provider/singleton lookup, null/error cases and one declared-field injection fixture. They do not establish active executor integration, constructor injection, inherited-field handling, container neutrality, runtime isolation or lifecycle ownership. Tests were inspected, not run.

**Representative Core plugin service dependencies**

The current Core source includes Guice-injected fields for:
- HttpPlugin: HttpService.
- ScriptPlugin: ScriptEngineFactory.
- FilePlugin: String workingDir, qualified with @WorkingDir.
- IncludePlugin: ConfigFactory.

These dependencies are not injected by the project's @InjectDependency helper. The active Guice construction path supplies them. The inventory is representative, not yet a complete 44-plugin constructor/service-dependency matrix.

**Nested executor construction paths**

Direct new DefaultPluginBodyExecutor() construction is present in:
- CallPlugin (function body dispatch),
- IncludePlugin (included configuration body),
- LoopPlugin (loop body),
- RegexpPlugin (result/template dispatch).

The existing architecture audit separately traces conditional branch dispatch. Every such path must inherit the same runtime-owned provider factory, immutable registry generation, host policy, cancellation/deadline and shared budget. A default executor detached from the parent execution context is not acceptable.

**Implication for the migration matrix**

Do not classify the existing helper as a ready-made neutral SPI or simply wire it into the executor. The target must define a runtime-owned plugin factory/provider contract with constructor injection, typed/qualified service requirements, host-container adapters, deterministic initialize/cleanup/rollback, explicit lifetimes and per-runtime isolation. The plain-Java composition path must not require Guice on the runtime classpath; Guice and Spring integrations must adapt host-owned containers rather than start new ones.

Source blobs on current main:
- PluginDependencyInjector.java: 12d72a925ed81cf34ce2c47de9020042749797b0.
- DefaultPluginDependencyInjector.java: fe7aeacda9041c0aba88d4c15aea9c54d81d3431.
- DefaultPluginBodyExecutor.java: 0116211ed31986b757f6f9e4bb5689f199b54f69.
- InjectorHelper.java: 24c114b24a3abc079a9d447b59155e3598f5abe7.
- ScraperModule.java: d31a5e30b0949c752743123cbcfb4219ba60713e.
- HttpPlugin.java: c6045d6c7638f3aa86c510240814633597b108ad.
- ScriptPlugin.java: 0bd1992d0739f43c9d550b302d4fa0a815e63995.
- FilePlugin.java: 8d78e797975ab236a47420d18f28600beaae04bf.
- IncludePlugin.java: ab06127f7f5402978937160a42455b4c0107dfab.
- CallPlugin.java: 01238d278e4bbddb262c797f34a481dcd964e3c9.
- LoopPlugin.java: ba91342e4e560f6bea9165bdba9b5ce4b4db82d8.
- RegexpPlugin.java: a860ab08554122680c4d5d5fde1be3d683139c6a.
### 15.35 Public runtime implementation signatures expose Guice types (2026-11-23)

A follow-up API-surface check qualifies the earlier observation that the Plugin interface itself does not expose Guice. Several public runtime implementation/composition types do expose Guice directly:

- DefaultHarvesterFactory has a public constructor accepting Guice Provider<WebScraper>, Provider<EventBus> and ScraperScope.
- ScrapingHarvester has a public constructor accepting Guice Provider<WebScraper>, Provider<EventBus> and ScraperScope.
- AttributeHolderScope is public, implements Guice Scope and Provider<AHT>, and exposes a scope method accepting Guice Key<T> and Provider<T>.
- ScraperModule is a Guice AbstractModule and requests static injection for InjectorHelper and BaseTemplater; it also binds the static ScraperScope.

Therefore, do not infer a container-neutral public embedding surface merely from the Plugin, Harvest or Harvester facade interfaces. The existing public implementation/composition seam still requires Guice. The neutral Java embedding API must be a separate host-facing contract; Guice Provider/Scope/Key/AbstractModule should remain inside an optional Guice adapter. Plain-Java compile/runtime and host-managed Guice adapter fixtures are both required.

Source blobs on current main:
- DefaultHarvesterFactory.java: 58bae4e22a82d89dac1c6e63e28ce5f3587e687e.
- ScrapingHarvester.java: 2af32ca67c235e075c8cc613118814d54e555338.
- AttributeHolderScope.java: 11a39c99e517ae9ae1ba09cf69a885b636465b64.
- ScraperModule.java: d31a5e30b0949c752743123cbcfb4219ba60713e.
### 15.36 44-entry Core provider dependency and host-effect crosswalk (2026-11-23)

The manual registrar was re-read and counted directly: **44 unique XML element names map to 44 unique implementation classes**. This section records the provider-construction and host-effect findings visible in the implementation source. It is an inventory of current behavior, not an approved minimal-Core decision.

**Construction/service dependency findings**

- Four of the 44 classes have explicit Guice field injection in the inspected source: `HttpPlugin` → `HttpService`; `ScriptPlugin` → `ScriptEngineFactory`; `FilePlugin` → qualified `String workingDir` (`@WorkingDir`); `IncludePlugin` → `ConfigFactory`.
- No constructor-injected service dependency was identified among the 44 mapped classes in this pass. This does not mean the classes are already constructible through a neutral factory: active dispatch still uses Guice `Injector.getInstance()` or deprecated no-argument reflection, and nested dispatch creates detached default executors.
- The remaining mapped classes do not declare a separate project `@InjectDependency` requirement in the inspected source. This is not proof that they have no transitive service requirements: they call shared runtime/context/templating facilities, and some depend on globally injected `BaseTemplater` state.
- `HttpPlugin`, `ScriptPlugin`, `FilePlugin` and `IncludePlugin` therefore require explicit neutral service requirements in the migration descriptors. Their existing Guice field annotations must not become the SPI contract.

**Per-element role and effect classification**

| XML element | Implementation | Current role / effect relevant to migration |
|---|---|---|
| `call` | `CallPlugin` | Function dispatch; creates a nested executor; must inherit runtime context and generation |
| `call-param` | `CallParamPlugin` | Declarative argument/parameter detail; two parent contexts documented in 15.22 |
| `case` | `CasePlugin` | Conditional branch selection; child execution |
| `catch` | `CatchPlugin` | Exception branch syntax; consumed by try/catch control flow |
| `constant` | `ConstantPlugin` | Value definition |
| `def` | `DefPlugin` | Variable/definition semantics |
| `else` | `ElsePlugin` | Conditional branch syntax |
| `empty` | `EmptyPlugin` | Empty value |
| `exit` | `ExitPlugin` | Execution control effect |
| `file` | `FilePlugin` | Host filesystem read/write/list/create; working-directory service; canonical path and resource policy required |
| `function` | `FunctionPlugin` | Function declaration/composition |
| `get` | `GetVarPlugin` | Context/value lookup |
| `html-to-xml` | `HtmlToXmlPlugin` | HTML parse/DOM conversion; input/output/time budgets |
| `http` | `HttpPlugin` | Outbound network request; host transport/destination policy; injected `HttpService` |
| `http-header` | `HttpHeaderPlugin` | HTTP request detail syntax; consumed by `HttpPlugin`, not an independent network capability |
| `http-param` | `HttpParamPlugin` | HTTP request detail syntax; consumed by `HttpPlugin`, not an independent network capability |
| `if` | `IfPlugin` | Conditional branch dispatch; creates nested executor; inherit context/generation |
| `include` | `IncludePlugin` | File/URL configuration loading and nested execution; injected `ConfigFactory`; include policy/depth/bytes/deadline |
| `json-to-xml` | `JsonToXmlPlugin` | JSON parse/materialization and XML conversion; input/depth/output budgets |
| `loop` | `LoopPlugin` | Repeated child execution; creates nested executor; shared cancellation/deadline/budget |
| `regexp` | `RegexpPlugin` | Pattern matching/transformation; CPU/input/output budget and ReDoS containment |
| `regexp-pattern` | `RegexpPatternPlugin` | Declarative regex detail; consumed by `regexp` |
| `regexp-result` | `RegexpResultPlugin` | Declarative regex result/template detail; consumed by `regexp` |
| `regexp-source` | `RegexpSourcePlugin` | Declarative regex source detail; consumed by `regexp` |
| `return` | `ReturnPlugin` | Function control-flow effect |
| `script` | `ScriptPlugin` | JSR-223 execution; injected `ScriptEngineFactory`; host-object permissions, engine isolation and execution budget |
| `list` | `ListPlugin` | Value/list composition |
| `log` | `LogPlugin` | Host logging effect; redaction and data-exposure policy |
| `value-of` | `ValueOfPlugin` | Value extraction |
| `xml` | `XMLPlugin` | XML value/parse semantics; parser hardening and input budget |
| `set-var` | `SetVarPlugin` | Legacy variable mutation compatibility; preserve current XML contract unless explicitly migrated |
| `sleep` | `SleepPlugin` | Wall-clock delay; host time budget and cancellation required |
| `stylesheet` | `StylesheetPlugin` | Declarative XSLT stylesheet detail; consumed by `xslt` |
| `template` | `TemplatePlugin` | Template/value composition; scripting/templating dependency must be runtime-scoped, not static |
| `text` | `TextPlugin` | Text value/body composition |
| `tokenize` | `TokenizePlugin` | String transformation; input/output budget |
| `try` | `TryPlugin` | Exception handling and child execution |
| `while` | `WhilePlugin` | Repeated condition evaluation; shared script/query budget; 10,000 runtime vs 1,000,000 validation mismatch in 15.33 |
| `xml-to-json` | `XmlToJsonPlugin` | XML parse and JSON materialization; input/depth/output budget |
| `xpath` | `XPathPlugin` | Saxon XPath/query execution; resolver, deadline, cancellation and result budget |
| `xquery` | `XQueryPlugin` | Saxon XQuery execution; resolver, deadline, cancellation and result budget |
| `xq-expression` | `XQExpressionPlugin` | Declarative XQuery expression detail; consumed by `xquery` |
| `xq-param` | `XQParamPlugin` | Declarative XQuery parameter detail; consumed by `xquery` |
| `xslt` | `XsltPlugin` | XML transformation; preserve external-resource restrictions and add output/deadline budget |

**Cross-cutting implications**

1. Keep XML syntax nodes and provider capabilities distinct. The detail elements `call-param`, `http-header`, `http-param`, `regexp-pattern`, `regexp-result`, `regexp-source`, `stylesheet`, `xq-expression` and `xq-param` are not nine independent extension services.
2. Host effects are not confined to the four Guice-injected classes. `SleepPlugin`, `LogPlugin`, transformation/query plugins and nested control-flow plugins require explicit policy/budget propagation even where they currently obtain dependencies from static helpers or the execution context.
3. The descriptor must state service requirements and effects independently of the implementation class. Host policy grants the effects; plugin code must not bypass those grants by directly opening JDK resources.
4. The crosswalk does not authorize deleting or moving any element. Preserve the 44 current mappings as compatibility baseline until a maintainer-approved migration table and contract fixtures exist.

Audit limits: this pass inspected the 44 implementation sources and registrar mapping for injection annotations, direct nested-executor construction and prominent host-effect APIs; it did not execute tests, perform bytecode/dataflow analysis, or prove absence of indirect effects. Existing test-source presence and coverage caveats remain as documented in earlier sections.
### 15.37 Correction: complete nested executor construction call-site set (2026-11-23)

A follow-up search for fully-qualified as well as imported executor construction found an additional direct path that the earlier abbreviated lists did not enumerate. The inspected Core call-site set is:

- `CallPlugin`: `new DefaultPluginBodyExecutor()` for function-body dispatch.
- `IfPlugin`: `new org.webharvest.plugin.DefaultPluginBodyExecutor()` for selected-branch dispatch.
- `CasePlugin`: `new DefaultPluginBodyExecutor()` for selected case/fallback branch dispatch.
- `IncludePlugin`: `new DefaultPluginBodyExecutor()` for included configuration children.
- `LoopPlugin`: `new DefaultPluginBodyExecutor()` for loop-body dispatch.
- `RegexpPlugin`: two direct executor constructions for result/template dispatch.

`TryPlugin` follows `AbstractPlugin.executeBody()` and does not construct a new executor at the inspected call site. This list is based on source text search in the 44 mapped Core implementation files, not bytecode/dataflow proof that no indirect executor factory exists elsewhere.

Correction to 15.34: its nested-dispatch list was incomplete; the complete direct call-site list is the seven constructions above across six plugin classes. All must receive the same runtime provider factory, immutable registry generation, host policy, cancellation/deadline and shared budget. The detached default-constructor pattern must not survive the dynamic runtime migration.
### 15.38 Plugin execution cleanup is not an unload/rollback lifecycle (2026-11-23)

A focused trace of the Plugin contract and active executor adds a lifecycle caveat to the provider dependency matrix:

- `Plugin.cleanup()` is documented as the resource cleanup method called when a plugin is unloaded.
- `AbstractPlugin.execute()` calls `doCleanup()` in `finally` after each execution, but does not clear its initialized flag or stored configuration.
- `AbstractPlugin.cleanup()` calls `doCleanup()` again whenever initialized is true, then clears initialized/configuration. The base class does not enforce exactly-once invocation across the per-execution and final cleanup paths.
- `AbstractPlugin.initialize()` calls `doInitialize()` after storing configuration. If initialization fails, it does not invoke `doCleanup()` as rollback and does not clear the stored configuration.
- The active `DefaultPluginBodyExecutor` creates/initializes a plugin and then executes it; `executeChildPlugin()` does not invoke public `Plugin.cleanup()` in a finally block. The selected conditional branch path also creates/executes plugins without an explicit final cleanup call.

Therefore, current `doCleanup()` is per-execution cleanup, not proof of safe provider/generation unload. Failed initialization has no guaranteed rollback hook, and the active executor does not demonstrate a deterministic public cleanup path for created instances. Dynamic plugin lifecycle must separate prepare/initialize, per-execution completion, failed-prepare rollback, and final provider/generation close, with explicit ownership and exactly-once disposal.

Conformance must include partial initialization failure after resource allocation, execution success/failure/interruption, reuse across sessions, generation retirement after leases drain, repeated close, and runtime isolation. This is source inspection only; no tests run. Source blobs: `Plugin.java` `29772a9b37c8fead735f4a5e0bd47e5b90b4367e`; `AbstractPlugin.java` `be3298304840e4a585b4fb148c2e62cf471ee8f7`; `DefaultPluginBodyExecutor.java` `0116211ed31986b757f6f9e4bb5689f199b54f69`.
### 15.39 Existing lifecycle tests cover per-execution cleanup, not provider unload/rollback (2026-11-23)

Test-source review qualifies the lifecycle gap in 15.38:

- `AbstractPluginExceptionBug5Test` asserts that `doCleanup()` runs after successful execution, ordinary failure and interruption. This validates per-execution cleanup and interrupt ordering.
- `AbstractPluginLifecycleBranchContractTest` checks initialized state, calls public `cleanup()` and verifies state reset/idempotence when cleanup is called twice without a preceding execute.
- `DefaultPluginBodyExecutorTest` exercises child execution and interruption propagation, but the inspected fixture does not assert public `Plugin.cleanup()` is invoked after the executor creates/executes a child.
- No inspected lifecycle test covers a `doInitialize()` implementation that allocates a resource and then throws, followed by rollback.
- No inspected test covers one plugin/provider surviving multiple sessions and then being disposed exactly once when its generation retires after active execution leases drain.
- No inspected test covers duplicate `doCleanup()` caused by `execute()` followed by public `cleanup()` on the same initialized instance.

These test sources are evidence of current coverage intent, not test execution. No local test suite was run. The future conformance suite must preserve existing per-execution cleanup/interrupt guarantees while adding failed-prepare rollback, separate final disposal and generation/provider lifetime assertions.


### 15.40 Additional nested-executor construction in the shared AbstractPlugin body path (2026-11-23)

A direct read of the complete `AbstractPlugin.executeBody()` implementation exposes an additional executor-construction path that must be included in the migration inventory:

- `AbstractPlugin.executeBody()` constructs `new DefaultPluginBodyExecutor()` whenever the plugin configuration has children, then invokes `executeBody(configuration, context)`.
- This is a shared base-class path used by plugins that delegate body execution through `executeBody()`, including `TryPlugin`. Therefore, saying that TryPlugin has no detached executor construction is only narrowly true at the TryPlugin call site; it is not true for the complete execution path.
- The seven explicit constructions listed in 15.37 remain valid for the six plugin classes, but they are not the complete runtime construction inventory: the shared `AbstractPlugin` path adds another construction site outside that six-class list.
- Every such executor must receive the same runtime composition context, provider factory, pinned registry generation, host policy, cancellation/deadline and shared budget. The no-argument default constructor must not silently create a detached execution path.
- The audit must distinguish syntactic construction sites from reachable execution paths: the shared base-class path can be reached by any plugin using `executeBody()`, so migration tests must cover both explicit nested dispatchers and ordinary body delegation (including Try).

This corrects the scope of 15.37: **seven direct constructions across six mapped plugin classes, plus the shared `AbstractPlugin.executeBody()` construction path**. Source inspection only; no tests run. Source blob: `AbstractPlugin.java` `be3298304840e4a585b4fb148c2e62cf471ee8f7`.


### 15.41 Resource ownership is split across invocation, session and runtime scopes (2026-11-23)

A targeted source pass across resource-bearing built-ins shows that plugin-instance cleanup is not the only, or even the main, owner of resources:

- **Invocation-local resources:** DatabasePlugin.doExecute() closes ResultSet and Statement in its own finally; it closes the connection proxy / named connection according to whether a session EventBus exists. FTP plugin variants create an FTPClient per execution and logout/disconnect it in finally. These are operation-owned resources, not evidence that a plugin provider should be closed after every call.
- **Session-owned resources:** ConnectionPlugin creates a StandaloneConnectionPool and registers it with the session EventBus; its ConnectionFactoryVariable is stored in the execution context. DatabasePlugin relies on session event cleanup for physical pool ownership when an EventBus exists; the standalone/no-EventBus path explicitly closes the named pool.
- **Browser session resources:** WebBrowserPlugin stores browser pages in static process-level maps keyed by session ID (SESSION_PAGES) and, for legacy sessionless contexts, in a synchronized WeakHashMap. Each BrowserSessionState registers itself with the session EventBus and closes pages/unregisters/removes its session entry on completed, failed or cancelled events. closeAllPages() is also an explicit global shutdown path. The map is process-global storage, while the intended ownership of entries is per session; runtime isolation and deterministic owner shutdown are not yet established.
- **Runtime-owned service:** HttpServiceImpl is AutoCloseable and closes its underlying CloseableHttpClient; the existing Guice singleton binding and missing explicit runtime/injector close owner were documented in 7.2. This resource must not be closed when an individual plugin execution or session ends if it is shared by other sessions.
- **Irreversible side effect:** MailPlugin sends during doExecute(); no resource doCleanup() override was found in this targeted pass. Sending mail is an execution effect, not a cleanup responsibility.

No doCleanup() override was found in the targeted set of resource-bearing implementations inspected for this section. This is a bounded source pass, not a repository-wide proof that no override exists. The key design implication is to model **invocation, session, registry-generation, runtime and host** ownership separately. Generation retirement must close only generation-owned providers/resources after leases drain; it must not close host- or runtime-owned shared services, nor defer invocation-local cleanup until unload. Source inspection only; no tests run.

Source evidence: DatabasePlugin.java b3934d753f17f133bd582b50206db701af843728; ConnectionPlugin.java edb6ed93584cb03d967c830d120848c7e891f481; FtpPlugin.java d7098611ea7e0569b7cf9af01346be7b951da799; WebBrowserPlugin.java be7675390275b16ed63e5515a11969585acd2de6; MailPlugin.java 083297481e7e218916a77f0dea2308c63bbac7e7; HttpServiceImpl.java 9e3dab1a40250f4d059ea812c74012d994873f0d.


### 15.42 Legacy PluginFactory/PluginRegistry has a separate prototype-instance lifecycle (2026-11-23)

A source pass over the parallel factory/registry API found another lifecycle model that must not be confused with the active CorePluginRegistry + DefaultPluginBodyExecutor path:

- DefaultPluginRegistry.registerPlugin(Class) reflectively constructs a plugin instance and stores it in maps as the registered Plugin object; metadata and namespace lookups are read from that retained instance.
- DefaultPluginRegistry.unregisterPlugin(id) removes registry/map entries but does not call Plugin.cleanup() on the retained instance.
- DefaultPluginFactory.createPlugin(id/namespace+element) reads the retained instance only to obtain its class, then creates a new plugin instance. createPlugin(class, configuration) initializes the new instance but exposes no corresponding destroy/close handle or ownership callback.
- DefaultPluginFactory instantiation prefers a no-argument constructor, otherwise a PluginMetadata constructor, using reflection and a constructor cache. It is not the active Guice path used by DefaultPluginBodyExecutor, and the earlier source audit found PluginDependencyInjector is not wired into active execution.
- The retained registry object therefore behaves as a metadata/prototype object, while the factory-created instance is a separate execution object. The API does not define ownership/cleanup for either object on unregister, failed initialization or normal caller completion.

This is a legacy/parallel API audit, not evidence that it is the current CLI runtime path. The new platform must either formally retire this API or adapt it to the same descriptor-only registry and explicit provider/instance ownership contract; it must not preserve a second, incompatible lifecycle model. Add tests for registration without plugin side effects, unregister disposal policy, failed prepare rollback, factory-created instance ownership, and parity with the authoritative registry. Source inspection only; tests not run.

Source evidence: DefaultPluginRegistry.java da5c6f6e835cc8b551394e0853a70ca4bcd60d2f; DefaultPluginFactory.java 91c8084f671027954375ff3cab784691f8ae26e5; PluginFactory.java f1d2434c5a36d21d9c09bb638f4ebf3e5eaa94f8; DefaultPluginDependencyInjector.java fe7aeacda9041c0aba88d4c15aea9c54d81d3431; PluginDependencyInjector.java 12d72a925ed81cf34ce2c47de9020042749797b0.


### 15.43 Browser session cleanup is session-event-driven but globally stored

A direct read of the current `WebBrowserPlugin` source refines the browser ownership finding in 15.41:

- `SESSION_PAGES` is a static process-wide `ConcurrentHashMap<String, BrowserSessionState>`, keyed only by `ScraperSession.sessionId`; the key does not encode a WebHarvest runtime identity.
- `CONTEXT_PAGES` is a second static process-wide synchronized `WeakHashMap<DynamicScopeContext, BrowserSessionState>` for legacy contexts without a session. The state constructor receives no EventBus in this compatibility path, so there is no session-terminal callback for it.
- For normal sessions, `BrowserSessionState` registers itself with the supplied EventBus. Completed, failed and cancelled events call an idempotent synchronized `close()`, which closes every WebClient, unregisters the listener and removes the matching session entry. `getBrowserSessionState()` also checks terminal session status after map lookup and closes/removes a state if the terminal transition raced with registration.
- The explicit `WebBrowserPlugin.closeAllPages()` method iterates and closes **all** entries in both static maps. It is therefore a process-wide shutdown operation, not a safe per-runtime close hook: one embedded WebHarvest runtime must not call it while another runtime is active.
- The sessionless WeakHashMap compatibility path has no explicit context-disposal callback in the inspected class. Weak-key removal is not equivalent to calling `WebClient.close()`; deterministic browser resource release for that path remains unproven.

This source confirms meaningful per-session terminal cleanup for normal session-backed use, while also confirming that storage and the global shutdown method are process-wide. A target runtime-owned browser service should use runtime-scoped identity/storage, expose per-session close and runtime close separately, and reserve process-wide close for an explicit host-wide shutdown. Tests must cover two runtimes, same/different session IDs, sessionless contexts, terminal-event races, and closing one runtime while another retains active pages. Source inspection only; no tests run.

Source blob: `WebBrowserPlugin.java` `be7675390275b16ed63e5515a11969585acd2de6`.


### 15.44 Session lifecycle currently depends on optional EventBus availability

A direct read of `ScrapingHarvester.execute()` adds an important condition to the resource ownership model:

- The public constructor receives Guice `Provider<EventBus>`. `execute()` attempts `eventBusProvider.get()` only when the provider is non-null.
- If provider lookup throws, the exception is caught and logged as a warning; execution continues with `eventBus == null`.
- A `ScraperSession` is created, attached to `DynamicScopeContext` and marked running only when an EventBus was obtained. Without it, the context runs without a session and without session terminal events.
- Consequently, browser cleanup through `SessionCompletedEvent` / failed / cancelled is not available in this path. The browser plugin falls back to its static weak-key context map. Other resource-bearing plugins must use their own no-EventBus fallback; this is not equivalent to a uniformly enforced session lifecycle.
- The method does exit `ScraperScope` in a `finally` block, and session-backed execution marks the session completed or failed before leaving the scope. That ordering is useful, but it does not repair the no-EventBus path.

For the target platform, lifecycle ownership must not silently disappear when a container provider is unavailable. Either the session/lifecycle service is a required runtime dependency and startup/execution fails clearly when absent, or the runtime supplies a container-neutral internal lifecycle implementation. A host may provide an adapter, but absence/failure must not silently downgrade cleanup guarantees. Test provider absent, provider lookup failure, execution success/failure/interruption, and resource cleanup in each supported mode.

Source blob: `ScrapingHarvester.java` `2af32ca67c235e075c8cc613118814d54e555338`.


### 15.45 Optional-module test evidence and integration-test strength (2026-11-23)

This pass reconciles the ten optional-module plugin entry points against their actual test sources. It distinguishes deterministic behavior tests from the classes named “IntegrationTest”; the latter names alone do not prove an end-to-end runtime test.

| Module / plugin entry points | Deterministic test evidence | XML/Harvester integration evidence | Current assessment |
|---|---|---|---|
| FTP: `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin`, `FtpListPlugin` | `FtpPluginTest` directly exercises get/put/list dispatch and metadata/schema/validation. `FtpPluginBehaviorTest` uses MockFtpServer and covers container/standalone get, put, list, directory/delete actions, credentials, connection failures and invalid inputs. `FtpGetPluginCharsetTest` covers configured charset and UTF-8 fallback. | `FtpPluginIntegrationTest` is tagged `integration, ftp`, but its test only locates `ftp_plugin_example.xml` and asserts that the file exists. It does not load the XML or execute a Harvester. | Good deterministic FTP protocol/behavior coverage; the named XML integration test is a resource-presence check, not runtime integration. Add one deterministic XML/Harvester test backed by the local fake FTP server. |
| Database: `DatabasePlugin`, `ConnectionPlugin` | `DatabasePluginTest` uses H2 and directly initializes/executes DatabasePlugin for inline/named connections, SELECT/UPDATE, XML escaping, empty results, invalid SQL and session completion. `ConnectionPluginTest` covers configuration/schema and named-pool registration. `StandaloneConnectionPoolTest` covers connection reuse and completion/failure/cancellation cleanup. | No dedicated Database XML/Harvester integration test was found in the inspected module test tree; the principal SQL tests construct plugin instances directly. | Strong deterministic plugin/JDBC and pool lifecycle tests; missing explicit XML loader → registry → Harvester contract proof for both `database` and `connection`. |
| Mail: `MailPlugin` | `MailPluginTest` covers metadata/schema, validation, empty bodies, invalid host, unavailable local SMTP and direct execution branches. | `MailPluginIntegrationTest` is tagged `integration, mail`, but only locates `mail_plugin_example.xml` and asserts that the file exists. It does not execute the XML, connect to SMTP or assert a delivered message. | Unit/validation evidence exists; no demonstrated deterministic SMTP wire-level or XML/Harvester delivery test in the inspected sources. Add a local fake SMTP server and assert envelope, headers/body and attachment behavior without external network. |
| WebBrowser: `WebBrowserPlugin` | `WebBrowserPluginTest` covers metadata/schema, validation and action branches. `WebBrowserPluginLifecycleTest` uses a local HTTP server and asserts page loading, JavaScript, redirect/error behavior, replacement cleanup and per-session page isolation/cleanup. | `WebBrowserPluginIntegrationTest` executes the XML example through its integration base, but asserts only that the returned context is non-null; it does not assert extracted values, page state or resource cleanup from that XML. | Stronger lifecycle behavior coverage than the named integration test suggests; add XML/Harvester assertions for observable browser results and runtime-owned teardown/two-runtime isolation. |
| ZIP: `ZipPlugin`, `ZipEntryPlugin` | `ZipPluginContractTest` covers metadata/schema and validation; `ZipEntryPluginTest` covers orphan entry, wrong context type and archive write failure. | `ZipPluginIntegrationTest` executes XML through the Harvester and inspects generated archive bytes, including ordered entries, charset-specific content and nested archive contents. It also tests generic package-scanner discovery of the optional ZIP providers. | Strongest end-to-end optional-module evidence in this set; retain it as a model for other module fixtures. |

**Cross-module conclusion**

1. All ten optional plugin entry points have some source-level test evidence, but coverage strength differs materially. Do not report “all optional plugins have integration tests” based on the presence of three classes named `*IntegrationTest`.
2. FTP and Mail integration classes currently verify example-resource presence only. The WebBrowser class executes XML but its assertion is too weak to prove the advertised result. ZIP has meaningful XML/Harvester and archive-content assertions. Database has direct H2 execution and lifecycle tests but no dedicated XML/Harvester integration class in the inspected module test tree.
3. The target conformance suite should include one deterministic, out-of-tree or module-local XML/Harvester fixture per supported optional provider family, with observable output/effect assertions. External network services must be replaced by local deterministic fixtures (MockFtpServer, H2, local SMTP, local HTTP/browser server).
4. Keep test dimensions separate: metadata/schema contract; direct plugin behavior; XML/Harvester dispatch; external protocol/transport behavior; lifecycle/cleanup; and dynamic discovery/authorization. Passing one dimension does not imply the others pass.

**Source evidence (current `main`):**
- FTP `FtpPluginTest.java`: `dfeec2757e16f71015bc87537223b4e54e21f608`; `FtpPluginBehaviorTest.java`: `32a86a9291cf7dda40bace1a089c25ec2bdaa446`; `FtpPluginIntegrationTest.java`: `d7a67850e080ba4ac7a041f207672e5abc7a9e5e`; `FtpGetPluginCharsetTest.java`: `627dab1fdbe5361d64ac0de71bf188d615f8dc66`.
- Database `DatabasePluginTest.java`: `34002d58b1dc63beaa9df3aeb487073fc0a4bb2e`; `ConnectionPluginTest.java`: `cf96daaf75fd013566df3233f47efd8b9ae5b6c4`; `StandaloneConnectionPoolTest.java`: `1498206a92d2c2006bb399b71720e633590b0efa`.
- Mail `MailPluginTest.java`: `fcd0b6c2a895a0a31fddd8443696cdb7e8de0462`; `MailPluginIntegrationTest.java`: `674bb18d510f6cb7df0e5125f5bbb27191b93549`.
- WebBrowser `WebBrowserPluginTest.java`: source inspected; `WebBrowserPluginLifecycleTest.java`: `572f475dacabb7f706ce150d945a6dd762a83114`; `WebBrowserPluginIntegrationTest.java`: `fa362d3f64db20000f8576e6ec2ce4f4618409d7`.
- ZIP `ZipPluginContractTest.java`: `0c1a161a913106db28f59c2c788fe628a26088f0`; `ZipPluginIntegrationTest.java`: `992dba66310702aff205553d7f85c2eda31b57a9`.

This is test-source inspection only. No tests were executed in this pass, no runtime code changed, and no passing test result is claimed. The source files were read from `main`; this audit section is being added to the #351 documentation branch.


### 15.46 Repository-wide destination proposal — all 57 concrete candidates (2026-11-23)

**Status: proposed classification for maintainer review; no package move or runtime decision is authorized.** This section combines the 44 current Core XML mappings, the three additional Core-package plugin classes, and the ten optional-module entry points. “Destination” means a target architectural ownership boundary, not a proposed immediate Maven module rename.

#### Classification rules

- **Kernel/language** means provider behavior required to interpret the WebHarvest execution language: values, variable/context operations, function invocation, structured control flow and body composition.
- **Built-in capability** means first-party behavior that may ship with WebHarvest but is not intrinsically required to execute the language kernel. It must depend on host-provided services/policies and may be separately enabled in a runtime profile.
- **Optional integration** means an external-system, network, database, mail, browser or archive capability that must not be a mandatory dependency of the minimal kernel.
- **Grammar/descriptor** means XML syntax or parent-owned detail semantics. It needs schema/semantic representation but is not an independently installable provider.
- **Runtime protocol / historical / disabled compatibility** are not ordinary provider destinations.

#### Current Core-package candidates

| Current implementation | Current XML/runtime status | Proposed destination | Rationale / migration constraint |
|---|---|---|---|
| CallPlugin | Registered `call` | Kernel/language | Function invocation and nested execution; must inherit runtime, generation, policy and budget |
| CallParamPlugin | Registered `call-param` | Grammar/descriptor | Call argument detail, not independent capability |
| CasePlugin | Registered `case` | Grammar/descriptor | Parent-owned conditional branch |
| CatchPlugin | Registered `catch` | Grammar/descriptor | Try/catch branch and error binding |
| ConstantPlugin | Registered `constant` | Kernel/language | Literal/value definition |
| DefPlugin | Registered `def` | Kernel/language | Variable definition/context mutation |
| ElsePlugin | Registered `else` | Grammar/descriptor | Parent-owned conditional branch |
| EmptyPlugin | Registered `empty` | Kernel/language | Empty value primitive |
| ExitPlugin | Registered `exit` | Kernel/language | Execution-scoped control signal; never runtime shutdown |
| FilePlugin | Registered `file` | Built-in capability: filesystem | Host filesystem service, path confinement and byte/time limits required |
| FunctionPlugin | Registered `function` | Kernel/language | Function declaration/body composition |
| GetVarPlugin | Registered `get` | Kernel/language | Variable/context lookup |
| HtmlToXmlPlugin | Registered `html-to-xml` | Built-in capability: HTML conversion | Parser dependency and input/output/time budgets; not essential to language dispatch |
| HttpPlugin | Registered `http` | Built-in capability: HTTP | Host transport and destination policy; no direct unrestricted network access |
| HttpHeaderPlugin | Registered `http-header` | Grammar/descriptor | HTTP request detail owned by `http` |
| HttpParamPlugin | Registered `http-param` | Grammar/descriptor | HTTP request detail owned by `http` |
| IfPlugin | Registered `if` | Kernel/language | Structured conditional execution; nested body must pin generation |
| IncludePlugin | Registered `include` | Built-in capability: configuration sources | File/URL loading, include depth/bytes and nested-generation policy; keep composition contract distinct from unrestricted filesystem |
| JsonToXmlPlugin | Registered `json-to-xml` | Built-in capability: data conversion | JSON parser/materialization and bounded conversion |
| LoopPlugin | Registered `loop` | Kernel/language | Iteration and nested body execution under shared budget |
| RegexpPlugin | Registered `regexp` | Built-in capability: text transformation | CPU/input/output bounds and ReDoS containment |
| RegexpPatternPlugin | Registered `regexp-pattern` | Grammar/descriptor | Parent-owned regexp detail |
| RegexpResultPlugin | Registered `regexp-result` | Grammar/descriptor | Parent-owned result/template detail; nested execution inherits context |
| RegexpSourcePlugin | Registered `regexp-source` | Grammar/descriptor | Parent-owned regexp input detail |
| ReturnPlugin | Registered `return` | Kernel/language | Function-scoped control signal |
| ScriptPlugin | Registered `script` | Built-in capability: scripting | Runtime-scoped engine, explicit host-object permissions and execution limits; no static global engine |
| ListPlugin | Registered `list` | Kernel/language | Collection/value construction |
| LogPlugin | Registered `log` | Built-in capability: host logging | Host logging service, redaction and output policy |
| ValueOfPlugin | Registered `value-of` | Kernel/language | Value extraction/composition |
| XMLPlugin | Registered `xml` | Kernel/language | XML/text value construction; parser semantics must remain explicit |
| SetVarPlugin | Registered legacy `set-var` | Kernel/language compatibility | Preserve exact legacy mapping and semantics; metadata scanner parity must be fixed separately |
| SleepPlugin | Registered `sleep` | Built-in capability: time | Deadline-aware cancellable delay; host policy controls maximum duration |
| StylesheetPlugin | Registered `stylesheet` | Grammar/descriptor | XSLT stylesheet detail owned by `xslt` |
| TemplatePlugin | Registered `template` | Kernel/language | Template interpolation is language-level, but engine service must be runtime-scoped |
| TextPlugin | Registered `text` | Kernel/language | Text value/body construction |
| TokenizePlugin | Registered `tokenize` | Kernel/language | Deterministic text/list primitive; output limits remain mandatory |
| TryPlugin | Registered `try` | Kernel/language | Exception/control-flow semantics and nested body execution |
| WhilePlugin | Registered `while` | Kernel/language | Condition loop; canonical validation/runtime iteration limit required (see 15.33) |
| XmlToJsonPlugin | Registered `xml-to-json` | Built-in capability: data conversion | XML parser and bounded JSON materialization |
| XPathPlugin | Registered `xpath` | Built-in capability: query | Saxon/Jsoup query engines, resolver restrictions and execution/result budgets |
| XQueryPlugin | Registered `xquery` | Built-in capability: query | Saxon execution with resolver restrictions and execution/result budgets |
| XQExpressionPlugin | Registered `xq-expression` | Grammar/descriptor | Required XQuery detail, not independent provider |
| XQParamPlugin | Registered `xq-param` | Grammar/descriptor | XQuery parameter detail owned by `xquery` |
| XsltPlugin | Registered `xslt` | Built-in capability: transformation | Preserve external-resource restrictions; add output/deadline budgets |
| BodyProcessorPlugin | Not registered; historical implementation | Historical/internal; exclude from public catalog | 2.3.x docs state removed; runtime uses PluginBodyExecutor directly |
| ConfigPlugin | Not registered as child plugin | Runtime/document protocol | Root `<config>` is handled by RootConfigurationExecutor, not provider discovery |
| VarPlugin | Not registered; deprecated | Disabled legacy compatibility | Do not advertise or auto-enable; reactivation requires explicit compatibility decision and XML execution fixture |

#### Optional-module candidates

| Current implementation | Current module | Proposed destination | Rationale / migration constraint |
|---|---|---|---|
| DatabasePlugin | `webharvest-database` | Optional integration: JDBC/database | JDBC drivers, credentials, URL policy, query timeout/cancellation, result bounds and transaction semantics |
| ConnectionPlugin | `webharvest-database` | Optional integration: JDBC/database | Connection declaration consumed by DatabasePlugin; deterministic session/runtime ownership and secret handling |
| FtpPlugin | `webharvest-ftp` | Optional integration: FTP | External network and remote filesystem effects; current implementation uses plain FTPClient, not FTPS |
| FtpGetPlugin | `webharvest-ftp` | Optional integration: FTP | Child contract currently conflicts with independently opening its own client; bound remote reads |
| FtpPutPlugin | `webharvest-ftp` | Optional integration: FTP | Remote write authorization and bounded payloads; resolve parent/child connection mismatch |
| FtpListPlugin | `webharvest-ftp` | Optional integration: FTP | Remote directory enumeration and output bounds; resolve parent/child connection mismatch |
| MailPlugin | `webharvest-mail` | Optional integration: SMTP/mail | Irreversible external send; current executable schema does not implement documented auth/TLS options |
| WebBrowserPlugin | `webharvest-webbrowser` | Optional integration: browser automation | SSRF/redirect/DNS policy, JavaScript permission, session isolation and deterministic teardown |
| ZipPlugin | `webharvest-zip` | Optional capability: archive | JDK-based and locally scoped, but not required by minimal execution kernel; bounded archive/entry resources |
| ZipEntryPlugin | `webharvest-zip` | Grammar/provider detail within archive capability | Parent-owned ZIP entry writer; path traversal, duplicate and aggregate-size policy required |

#### Resulting architectural posture

1. Preserve all 44 current XML mappings as the compatibility baseline in the default compatibility profile. This is a compatibility statement, not a commitment that every capability is always enabled in every embedding host.
2. Keep the language kernel small and host-neutral. It owns execution semantics, values, context, function/control flow, descriptor grammar and generation-pinned nested dispatch.
3. Keep filesystem, HTTP, configuration-source loading, scripting, logging, time, query and transformation behavior behind explicit first-party capability contracts. Whether selected capabilities are bundled by default is a product-profile decision; none may bypass host policy.
4. Keep Database, FTP, Mail, WebBrowser and ZIP outside the mandatory kernel. ZIP may be shipped as a first-party optional capability; its JDK-only implementation does not justify forcing it into the kernel.
5. Represent parent-owned XML detail elements as descriptor/schema fragments rather than independently installable top-level providers. Root `config` is a runtime protocol; `BodyProcessorPlugin` and disabled `VarPlugin` are not default providers.
6. Keep legacy `set-var` executable and discoverable in parity with the runtime registry until an explicit compatibility/deprecation decision is approved.
7. No Maven module split, package move, class deletion, SPI freeze or runtime rewrite follows from this proposal. First approve the classification, then define the neutral provider/service contract, then prove an out-of-tree extension and live generation lifecycle.

#### Open maintainer decisions

- Approve or revise the proposed language-kernel membership, especially whether `regexp`, `tokenize`, `template` and XML value construction are kernel semantics or separately selectable capabilities.
- Decide which first-party capabilities are enabled in the default compatibility profile versus opt-in host profiles.
- Decide whether ZIP remains a first-party optional capability or is excluded from the supported distribution.
- Confirm the legacy support horizon for `set-var`; decide whether `var` remains disabled permanently or needs a separately gated compatibility pack.
- Approve policy defaults for filesystem, HTTP, include, scripting, logging, time, query and transformation effects before implementation.

**Evidence boundary:** this is a synthesis of source inventories and call-site/test audits already referenced in sections 15.18–15.45. It does not claim tests were executed, does not claim a complete transitive dependency/dataflow proof, and does not authorize runtime changes. Maintainer approval is still required.


### 15.47 Maven dependency direction and compatibility constraints (2026-11-23)

This is a POM-declaration audit, not a resolved/transitive dependency graph. It records direct production declarations and module edges; it does not claim bytecode reachability or prove the absence of reflective/service-loader dependencies.

#### Observed module graph

| Module | Direct production dependency relevant to this boundary | Observed direction | Initial assessment |
|---|---|---|---|
| `webharvest-core` | No WebHarvest sibling module dependency declared | Base module | Good direction at the Maven sibling-module level; its own dependency footprint is not yet a minimal kernel |
| `webharvest-database` | `webharvest-core`, `commons-dbcp` | Optional module → Core | Correct direction; JDBC driver is not bundled as a production module dependency |
| `webharvest-ftp` | `webharvest-core`, `commons-net` | Optional module → Core | Correct direction; FTP transport library remains outside Core |
| `webharvest-mail` | `webharvest-core`, `commons-email` | Optional module → Core | Correct direction; SMTP library remains outside Core |
| `webharvest-webbrowser` | `webharvest-core`, `org.htmlunit:htmlunit` | Optional module → Core | Correct direction; browser automation library remains outside Core |
| `webharvest-zip` | `webharvest-core` only | Optional module → Core | Correct direction; ZIP implementation uses JDK facilities and has no additional production library |
| Root aggregator | Lists Core, CLI, IDE, five optional modules and docs as reactor modules | Aggregation only | Reactor membership is not a runtime dependency edge; root POM dependencyManagement/BOM entries must not be misread as dependencies of every child |

**Positive finding:** all five audited optional Maven modules declare Core as their WebHarvest sibling dependency; none of their POMs declares another optional WebHarvest module as a production dependency. The inspected POM declarations therefore show the desired one-way module direction. This does not prove there are no reverse references through reflection, resources, test fixtures or runtime classpath assumptions; source-level cross-module reference checks remain a separate gate.

#### Core is still a capability bundle, not a neutral minimal kernel

The current Core POM directly declares production libraries for multiple capability families:
- dependency injection / construction: Guice and CGLIB;
- HTTP: Apache HttpClient 5;
- scripting: BeanShell, Groovy runtime/JSR-223, Rhino engine;
- query/transformation/HTML: Saxon (base plus `dom` classifier), Jsoup, TagSoup;
- JSON: `org.json` and Gson;
- discovery/reflection: Reflections;
- shared utilities and logging: SLF4J, Commons Lang/IO/Collections, Guava and annotations.

The precise call-site/dependency rationale is not uniform: sections 15.8–15.10 and 15.36 identify several direct provider seams, but this POM pass does not prove which transitive dependency can be removed or split. In particular, Guice is used by active runtime construction and public composition types (15.34–15.35); removing it from Core requires first delivering the neutral provider/runtime composition contract and a separate Guice adapter. It is not a safe dependency-only cleanup.

#### Compatibility and migration risks

1. **Published artifact compatibility:** all current optional artifacts depend on `org.webharvest:webharvest-core:${project.version}`. A future kernel/capability split must preserve coordinates or provide compatibility artifacts/BOM/dependency mediation; moving classes without a bridge breaks downstream Maven consumers.
2. **XML compatibility:** the default runtime's 44 mappings, including `set-var`, remain the compatibility baseline. A capability being separately selectable must not silently make its XML unknown in existing applications. The runtime needs explicit profile/availability diagnostics and a deliberate default compatibility profile.
3. **SPI compatibility:** current plugin classes, Guice field injection, metadata annotations and registry APIs are implementation details with inconsistent meanings. They are not a stable third-party ABI. Do not promise binary compatibility until the neutral descriptor/provider/service/lifecycle contract and version negotiation are specified.
4. **Optional-module independence:** optional modules must compile and run against the minimal public Core API only. They must not depend on Core internals, Guice internals or another optional module. An out-of-tree sample extension is the required proof before repository extraction.
5. **Runtime classpath and service ownership:** moving a library to an optional artifact is insufficient if Core still statically references its implementation or scanner roots require it. The catalog must tolerate absent capabilities and expose a clear unsupported-capability error without class-loading the optional implementation.
6. **Reactor vs published graph:** root aggregator membership does not imply all dependencies are required by a consumer. Release profiles, dependencyManagement and the effective published POM must be checked for each proposed artifact, including CLI/IDE adapters, before publishing a split.
7. **Java/runtime baseline:** preserve Java 17 unless a separately approved compatibility decision changes it; validate extension bytecode and API signatures against the declared baseline.

#### POM evidence (current `main`, 2026-11-23)

- Root aggregator `pom.xml`: `dc944dc1a15a2c4eedecd270af2016a9ec03c0d4`.
- Core `webharvest-core/pom.xml`: `d6dd45ba5ee481b30fce3f07d4ce08a1b1ea45ed`.
- Database `webharvest-database/pom.xml`: `828886c603c916aaa8dbfeac7b9f914960be8434`.
- FTP `webharvest-ftp/pom.xml`: `46dd3f1777eed4fa7e8cfaaf3e5e9cbb3694791f`.
- Mail `webharvest-mail/pom.xml`: `7fee00811be02d9fdc95af26992310dfe43e1221`.
- WebBrowser `webharvest-webbrowser/pom.xml`: `d3b82f4f934b9bcb6d64d5d7c7059ee529151671`.
- ZIP `webharvest-zip/pom.xml`: `65123421e0095bf77d90f8ccc4a0c46db833e337`.

#### Remaining dependency gate

Before approving a migration, produce an effective dependency graph for each candidate artifact (including transitive compile/runtime dependencies), map each Core library to exact reachable call sites and tests, and run a forbidden-dependency check proving that the minimal kernel has no dependency on optional capability artifacts. For every proposed artifact split, add a consumer fixture that compiles using only the published minimal API plus the selected capability artifact.

**Evidence boundary:** POM source inspection only; no Maven dependency tree was executed, no tests were run, and no POM or runtime file was changed.


### 15.48 Core dependency-to-call-site evidence map (2026-11-23)

This section narrows the POM-level findings in 15.47 to source-level call sites. It is intentionally a **high-confidence call-site sample, not an exhaustive bytecode reachability or transitive dependency proof**. “Candidate to isolate” means a design seam to evaluate after the SPI is approved; it does not authorize moving a dependency or changing the default XML profile.

| Core dependency family | Direct source evidence inspected on `main` | Architectural implication | Confidence / unresolved work |
|---|---|---|---|
| Guice | `ScraperModule` imports and extends Guice module APIs, binds runtime factories/services/scopes and installs listeners/interceptors; `HttpPlugin` and `ScriptPlugin` use Guice `@Inject`. `DefaultPluginFactory` also has reflective constructor paths; `DefaultPluginDependencyInjector` is a separate reflection-based injector. | Guice is an active host composition mechanism, not merely a plugin implementation detail. Define a neutral provider/service/lifecycle contract first; keep Guice in a host adapter or compatibility composition layer until callers are migrated. | High for active runtime use. CGLIB is a direct POM declaration but no direct source call site was established in this pass; determine whether it is required by Guice/runtime or is removable. |
| Apache HttpClient 5 | `HttpServiceImpl` directly imports HttpClient 5 request/client/entity classes and owns a `CloseableHttpClient`; `ScraperModule` binds `HttpService` to `HttpServiceImpl`; `HttpPlugin` depends on the `HttpService` abstraction, not HttpClient classes. | Good capability seam already exists: HTTP provider implementation can move behind a host-owned service/capability module without making the XML plugin itself depend on transport classes. Preserve injectable test/custom transport behavior and explicit network policy. | High for the observed path. Audit all other Core callers and lifecycle ownership of the shared client before extraction. |
| JSR-223 / BeanShell / Groovy / Rhino | `ScriptPlugin` consumes `ScriptEngineFactory`; `JSRScriptEngineFactory` uses `javax.script.ScriptEngineManager` and a thread-local engine cache. `TemplatePlugin` and `HttpPlugin` evaluate templates through `BaseTemplater` with BeanShell language selection. The POM declares BeanShell, Groovy runtime/JSR-223 and Rhino runtime artifacts. | Scripting engines are pluggable implementation providers, but the current language/template surface is part of the XML compatibility contract. Move engine implementations behind a runtime-scoped scripting service; explicitly define engine availability, host-object permissions, cache/session isolation and execution limits. | High for the service seam and ScriptPlugin path. Full language-to-engine provider resolution and engine-specific transitive dependencies still require inspection. |
| Saxon | `XPathPlugin` and `XQueryPlugin` directly import Saxon `Configuration`, query contexts, expression and value types. `XsltPlugin` uses JAXP `TransformerFactory` rather than direct Saxon imports in the inspected source. | XPath/XQuery have a concrete Saxon coupling; XSLT already exposes a JAXP provider seam. Query and transformation are capabilities, not unavoidable language-kernel dependencies. Resolver restrictions, external document access and execution/result budgets belong in the host policy contract. | High for XPath/XQuery. Confirm the actual XSLT provider selected at runtime and whether Saxon is the selected JAXP implementation in supported packaging. |
| Jsoup / TagSoup | `XPathPlugin` imports Jsoup for CSS selectors; `HtmlToXmlPlugin` imports Jsoup parser/cleaner APIs for HTML normalization. | HTML parsing and CSS selection are separable data/query capabilities. The POM also declares TagSoup, but a direct TagSoup call site was not established in the inspected classes. | High for Jsoup. TagSoup reachability and fallback usage remain open; do not remove based on the current sample. |
| JSON libraries | `JsonToXmlPlugin` imports `org.json` types and the Core `JsonXmlConverter`; `XmlToJsonPlugin` imports `org.json` types directly. | JSON conversion is a separable data-conversion capability. Keep XML compatibility and conversion semantics stable while making the implementation/provider selectable. | High for `org.json`. Gson is declared in the POM but no direct Gson call site was established in this pass; trace utility and transitive callers before deciding. |
| Discovery/reflection | `PluginClassScanner` uses the project `ClassPathScanner` / `ClassPathScannerImpl` and maintains a fallback class list. `PluginPackageScanner` performs package/resource/JAR enumeration itself. `DefaultPluginFactory` uses reflective constructors. | The active discovery implementation is not demonstrated to require the Reflections library. The two scanners and reflective factory are separate seams; neither package scanning nor metadata scanning is an acceptable final public extension/security contract. | High for observed scanner implementation. The POM-declared `org.reflections` artifact has no verified direct call site in this pass; establish whether it is unused, used indirectly, or required by another Core class/test before removal. |
| Guava / SLF4J / Commons utilities | `ScraperModule` directly uses Guava `EventBus` and `Monitor`; inspected plugin/runtime classes use SLF4J. Core POM declares Commons Lang, IO and Collections. | Treat these as shared host/runtime infrastructure until a complete call-site map proves otherwise. Logging needs host redaction policy; event bus and synchronization are runtime composition concerns, not plugin SPI types. | Partial. No exhaustive mapping of Commons/Guava/SLF4J usage was attempted. |

#### Source blob evidence (current `main`)

- `ScraperModule.java`: `d31a5e30b0949c752743123cbcfb4219ba60713e`.
- `HttpPlugin.java`: `c6045d6c7638f3aa86c510240814633597b108ad`.
- `HttpServiceImpl.java`: `9e3dab1a40250f4d059ea812c74012d994873f0d`.
- `ScriptPlugin.java`: `0bd1992d0739f43c9d550b302d4fa0a815e63995`.
- `TemplatePlugin.java`: `e20b948fe6a43936ef80587ff326535cf429e416`.
- `JSRScriptEngineFactory.java`: `f99047b30d2ca455898771ae8c207fafe4f1efd7`.
- `XPathPlugin.java`: `e519eb21a2e6faeef43a9d23c219502ac7f7f2a6`.
- `XQueryPlugin.java`: `b1f0ee00c16c9a37144e7e27cb46bfd57de6e9f1`.
- `XsltPlugin.java`: `c94b8e26dba566cb30bc12f6fc811bb1fcdc59b1`.
- `HtmlToXmlPlugin.java`: source inspected; its blob ID is recorded in the GitHub source history for this audit branch.
- `JsonToXmlPlugin.java`: `b10638356f38b7a7259454c6bd72f14f0380a5dd`.
- `XmlToJsonPlugin.java`: `f92faf37c73e0f16a813be174669b7dcec79ff2c`.
- `PluginClassScanner.java`: `dadd28a434000816253ac66bff736e2c5fff0494`.
- `PluginPackageScanner.java`: `c0e6cbff6cb8c472f66c4a897263cb3c3c7f8232`.
- `DefaultPluginFactory.java`: `91c8084f671027954375ff3cab784691f8ae26e5`.
- `DefaultPluginDependencyInjector.java`: `fe7aeacda9041c0aba88d4c15aea9c54d81d3431`.

#### Required next dependency proof

1. Resolve the effective compile/runtime dependency graph for Core and each proposed capability artifact; include optional/runtime-scoped engines and classifier variants.
2. Trace every direct Core POM dependency to production call sites, service-provider resources, reflection, tests and published consumer behavior. In particular, settle CGLIB, Gson, TagSoup and Reflections rather than treating absence from this sample as proof of dead code.
3. Establish a dependency allowlist for the minimal kernel and a forbidden-dependency check in CI. Keep Guice, HTTP, scripting, query, HTML and JSON implementations out of neutral SPI signatures.
4. Build the minimal Core plus one selected capability and an independent consumer fixture against the exact published candidate artifacts before any dependency is moved.

**Evidence boundary:** source imports/call sites and POM declarations were inspected on GitHub; no Maven dependency tree, bytecode analysis, test suite or runtime experiment was executed. No production source, POM or runtime behavior was changed.


### 15.49 Optional provider source-level dependency direction (2026-11-23)

This pass inspected the ten concrete optional-module provider entry-point classes on `main`. It complements the POM-only module graph in 15.47. The result is limited to direct imports in these ten entry points; it is not a complete scan of every helper/test/resource in each module.

| Optional provider(s) | Direct WebHarvest imports observed | External implementation imports observed | Boundary finding |
|---|---|---|---|
| Database: `DatabasePlugin`, `ConnectionPlugin` | Core plugin API/config/schema/metadata, `DynamicScopeContext`, variables, exceptions and `org.webharvest.runtime.database.StandaloneConnectionPool` | JDK JDBC types; Guava EventBus in connection handling | No import from another optional WebHarvest module. However, the providers depend on a Core runtime-internal database pool, not just a small neutral extension API. Pool/session ownership must be redesigned or explicitly exposed through a stable host service before third-party-style isolation. |
| FTP: `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin`, `FtpListPlugin` | Core plugin API/config/schema/metadata, `DynamicScopeContext`, variables and exceptions | Apache Commons Net FTP client/types | No import from another optional WebHarvest module. The provider classes are transport-specific and mostly use Core runtime context/value APIs; they are plausible optional capabilities once those Core types are represented by the approved SPI/session service contract. |
| Mail: `MailPlugin` | Core plugin API/config/schema/metadata, `DynamicScopeContext`, variables and exceptions | Commons Email and Activation APIs | No import from another optional WebHarvest module. SMTP transport is isolated in the module; configuration, secrets and send authorization must remain host-policy responsibilities. |
| WebBrowser: `WebBrowserPlugin` | Core plugin API/config/schema/metadata, `DynamicScopeContext`, variables, plus `ScraperSession` and session cancellation/completion/failure event classes | HtmlUnit browser/page APIs; Guava EventBus | No import from another optional WebHarvest module. This provider has the strongest direct coupling to Core runtime session internals in this sample. The final lifecycle SPI must replace or deliberately standardize these session/event hooks; otherwise a third-party provider compiled against the intended public SPI cannot reproduce the current lifecycle behavior. |
| ZIP: `ZipPlugin`, `ZipEntryPlugin` | Core plugin API/config/schema/metadata, `DynamicScopeContext`, variables, exceptions and `CommonUtil` | No non-JDK implementation imports in the entry-point classes | No import from another optional WebHarvest module and no third-party transport dependency. It is still coupled to Core plugin/context/value APIs; JDK-only implementation is not a reason to place it in the minimal language kernel. |

#### Concrete source blobs (current `main`)

- Database `ConnectionPlugin.java`: `edb6ed93584cb03d967c830d120848c7e891f481`; `DatabasePlugin.java`: `b3934d753f17f133bd582b50206db701af843728`.
- FTP `FtpGetPlugin.java`: `fd4ee5da4556bf91bdae7940072cf7a5e106f6d5`; `FtpListPlugin.java`: `c5fcfa6c5f90f643e6df8e85f6ad6a0b6e1074fa`; `FtpPlugin.java`: `d7098611ea7e0569b7cf9af01346be7b951da799`; `FtpPutPlugin.java`: `34061eb4fbc3a70f95d8d90a3a2faef611659f82`.
- Mail `MailPlugin.java`: `083297481e7e218916a77f0dea2308c63bbac7e7`.
- WebBrowser `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`.
- ZIP `ZipEntryPlugin.java`: `ea90361ca90b31d4c3903688ca78204121af0743`; `ZipPlugin.java`: `e62046292bca8689637e5eefbd7ce25ed460d118`.

#### Architectural consequence

1. The current optional modules follow the desired **Maven/source direction** toward Core; no direct optional-to-optional imports were found in these ten entry-point classes.
2. They are not yet independent against a narrow public SPI. All ten rely on Core plugin/configuration/runtime types; Database additionally consumes Core's database pool implementation, and WebBrowser consumes Core session/event internals.
3. Therefore, “optional module depends on Core” is not sufficient evidence of a stable extension boundary. Issue #346 must define provider context, values, services, cancellation, session lifecycle, diagnostics and resource ownership without leaking implementation-specific runtime classes into the SPI.
4. Before any module extraction, add a source-level forbidden-import check and an independent fixture that compiles against the intended public SPI only. Keep module-specific adapters inside the optional artifact.

**Evidence boundary:** direct imports in the ten optional provider entry-point classes were inspected on GitHub. No tests, builds, Maven dependency tree, full-module source import scan or runtime experiment was executed. No production code or POM was changed.


### 15.50 SPI design constraints derived from current Core APIs (2026-11-23)

This is a **review proposal**, not an approved public API and not implementation authorization. It translates the current source seams into constraints for Issue #346.

#### Current API facts that rule out a simple interface rename

| Existing surface | Observed current behavior | SPI consequence |
|---|---|---|
| `Plugin` | Execution accepts `DynamicScopeContext`, returns Core `Variable`, exposes mutable parent/running-level state, and includes initialization/cleanup on the plugin instance. | Cannot be the neutral third-party SPI unchanged: it leaks Core runtime/context/value types and combines provider definition, execution instance and lifecycle state. |
| `AbstractPlugin` | Execution implementation directly accesses Guava `EventBus`, `ScraperSession`, Core execution events, metrics, and SLF4J; it wraps the subclass `doExecute` path. | Useful as a legacy adapter/base class, but not a dependency of the new SPI. Translate host-neutral execution events/diagnostics through a context/service contract. |
| `PluginMetadata` | Metadata includes ID/name/version, namespace/element, class name and schema; default semantic roles are inferred from schema and `VisualPluginMetadata`. | Existing metadata is not a complete authoritative descriptor: no descriptor/API compatibility range, aliases, artifact provenance, permissions, lifecycle scope or complete semantic IO contract. Class name must not be identity. |
| `PluginConfigurationSchema` | Schema exposes attribute definitions, body allowance, allowed/required children and required parents; it is a Java interface and validates Core `PluginConfiguration`. | Preserve the useful structural concepts, but serialize a data-only descriptor schema so metadata/inspection can work without provider class loading. Keep executable custom validators behind a separately activated provider hook or a declarative rule set. |
| `DefaultPluginFactory` | Creates implementations from Java classes/IDs/QNames and caches constructors; instantiation is reflective. | Factory should consume a validated provider binding from a registry generation, not become the discovery authority. Constructor caching must be scoped to the owning artifact/runtime and retired with it. |
| `DefaultPluginRegistry` | Holds mutable concurrent maps of plugin instances/classes/metadata; supports register/unregister/clear/refresh. | Not suitable as the final authoritative registry: publish immutable generation snapshots atomically; keep mutation in a candidate builder/reconciler. Do not store live instances as descriptor identity. |
| `CorePluginRegistry` | Static singleton mutable element-to-class map with overwrite semantics and global clear. | Retain only inside the legacy XML adapter during migration. It must not be the new registry or a cross-runtime source of truth. |
| `PluginMetadataCatalog` | Static catalog built from `PluginClassScanner` class names; calls `Class.forName` and invokes no-arg constructors to derive descriptors. | This violates descriptor-only, side-effect-free discovery. Replace its data source with immutable descriptor projection from the selected registry snapshot; no plugin class loading or construction during catalog generation. |
| `ScraperSession` / `DynamicScopeContext` | Expose runtime/session/configuration/context implementation types; WebBrowser and AbstractPlugin consume session events/context directly. | New SPI must expose narrow execution services and cancellation/resource ownership abstractions, not these implementation classes. Provide adapters for existing built-ins. |

#### Proposed contract shape for review

Keep the SPI split into three layers, each with a distinct responsibility:

1. **Data-only descriptor** — stable plugin ID and provider ID; descriptor format/API version; supported Core range; XML QNames and explicit aliases; configuration/parent-child constraints; semantic roles and input/output/composition contracts; dependency/conflict declarations; declared capability/permission requirements; provider binding name; artifact provenance. It must be parseable and validated without loading implementation classes.
2. **Provider binding** — a generic Java service/provider entry point located within a managed artifact. It creates an implementation only after the host has accepted the descriptor, compatibility, enablement and policy. ServiceLoader may locate provider bindings inside an already managed artifact; it is not the artifact watcher, trust policy, registry or hot-update mechanism.
3. **Runtime instance contract** — a host-created instance receives a narrow immutable service/context view and explicit configuration, executes with a generation-pinned execution context, and closes deterministically. Declare supported instance scope/concurrency (runtime, session, invocation or explicitly thread-safe shared); do not infer it from constructor shape.

A conceptual API (names illustrative; not source code to implement yet):

- `PluginProvider`: stable provider binding; creates a runtime instance from host-supplied services.
- `PluginDescriptor`: immutable data-only identity, compatibility, XML, schema, semantic, dependency and permission declaration.
- `PluginInstance`: initialize, execute and close; no global registry mutation.
- `PluginExecutionContext`: host-neutral values/configuration, cancellation/deadline, diagnostics, scoped service access and declared capability handles.
- `PluginServices`: host-owned services scoped to the provider/runtime, with explicit close ownership.
- `PluginRegistrySnapshot`: immutable generation ID plus effective descriptor/activation projection; execution and validation pin the same snapshot.

Do **not** expose `DynamicScopeContext`, `Variable`, `ScraperSession`, Guice types, Guava EventBus, Core registry classes, or implementation class names as SPI identity. Existing built-ins can initially implement the new contract through adapters around current `Plugin` classes; the legacy XML parser/runtime remains an adapter until conformance proves parity.

#### Lifecycle and security decisions that must be explicit in #346

- Descriptor parsing/validation is side-effect-free; implementation class loading occurs only after compatibility, host enablement, trust and permissions pass.
- Distinguish discovered → compatible → enabled → authorized → activated → retiring. Classpath presence is not enablement or trust.
- Define provider construction and initialization rollback, runtime/session/invocation scope, concurrency, cancellation, execution failure, close/dispose, active-session drain and artifact classloader retirement.
- Permission declarations are requests, not grants. Host policy supplies capability handles; do not pass unrestricted filesystem/network/session internals.
- Same-JVM classloaders do not sandbox hostile Java. In-process execution is for trusted providers only; untrusted extensions require process/OS isolation and IPC.
- Registry publication is atomic and immutable. A failed candidate leaves the last-known-good generation active; an execution and its validation token identify the same generation.
- Compatibility is fail-closed for unsupported descriptor/API major versions and Core ranges. Alias and QName collisions are deterministic errors, never last-write-wins.

#### Acceptance implications

The first SPI proof should be a tiny external provider with no dependency on optional WebHarvest modules and no access to Core implementation classes. It must compile against only the intended SPI artifact, publish a descriptor, be visible to descriptor-only inspection before activation, and execute only after explicit host enablement/authorization. Then prove a second runtime can load the same provider independently and close it without affecting the first runtime. The full live add/update/remove and generation-pinning matrix remains in #347/#350.

#### Source evidence (current `main`)

- `Plugin.java`: `29772a9b37c8fead735f4a5e0bd47e5b90b4367e`.
- `PluginMetadata.java`: `0d31ed2c958bd03d429af8df6be94f1decfcbfdf`.
- `PluginConfigurationSchema.java`: `c649a8a2d58c496bf913a8a16ac989cab81a6e98`.
- `AbstractPlugin.java`: `be3298304840e4a585b4fb148c2e62cf471ee8f7`.
- `PluginFactory.java`: `f1d2434c5a36d21d9c09bb638f4ebf3e5eaa94f8`.
- `DefaultPluginFactory.java`: `91c8084f671027954375ff3cab784691f8ae26e5`.
- `PluginRegistry.java`: `eceeddca531c98ca4a7f235e04ab28511bfc9281`.
- `DefaultPluginRegistry.java`: `da5c6f6e835cc8b551394e0853a70ca4bcd60d2f`.
- `CorePluginRegistry.java`: `20f478addbf2bacc481e4e11e0328f06942a2a38`.
- `PluginMetadataCatalog.java`: `5ac82d7fb6436b458b29647e4c929878557b1c6e`.
- `ScraperSession.java`: `c8d0086ea160e2d8f02657c4cc9aa9d11d65b237`.

**Evidence boundary:** this section is based on source inspection of current `main`. No API, schema, registry or runtime code was changed; no tests/builds were run. The proposal requires maintainer review and approval in #346 before implementation.


### 15.51 Registry consistency and lifecycle gaps (2026-11-23)

This pass inspects the current \`main\` implementations and focused registry tests. It is a source/test audit only; the tests were read but not executed.

#### \`DefaultPluginRegistry\` is thread-safe only at individual map-operation level

The implementation uses \`ConcurrentHashMap\` for its maps, but registration and removal update several independent indexes in sequence. There is no transaction, lock, immutable candidate or generation boundary covering the complete operation.

Concrete consequences visible in the source:

1. **Duplicate-ID check is a check-then-act race.** \`registerPlugin\` calls \`pluginsById.containsKey(pluginId)\` and later \`put\`. Two concurrent registrations with the same ID can both pass the check; the later \`put\` replaces the earlier instance while metadata and secondary indexes may reflect a mixed sequence.
2. **Secondary category/namespace collections are mutable \`ArrayList\`s.** They are stored inside concurrent maps, but concurrent list mutation and snapshot copying are not protected by the map's thread safety. Concurrent register/unregister/read operations can race.
3. **QName collision is silently last-write-wins.** \`pluginsByNamespaceAndElement.put(namespace + ":" + elementName, plugin)\` does not reject an existing different plugin. Two distinct plugin IDs can therefore claim the same namespace/element.
4. **Unregister can remove another plugin's QName mapping.** Removal uses \`pluginsByNamespaceAndElement.remove(key)\` without checking that the current value is the plugin being unregistered. If plugin B replaced plugin A at the same QName, unregistering A removes B's mapping.
5. **The multi-index state is externally observable mid-update.** ID, metadata, QName, category and namespace indexes are changed separately; readers can observe partial registration/removal.
6. **\`registerPlugin(Class)\` instantiates immediately.** This contradicts the \`PluginRegistry\` Javadoc that says a class is instantiated when needed. It delegates to instance registration and does not retain a provider binding for deferred activation.
7. **\`pluginClassesById\` is not populated by the inspected registration paths.** The map is removed from/cleared but the inspected \`registerPlugin(Class)\` path creates an instance and delegates without storing the class.
8. **\`refresh()\` is explicitly a no-op.** The current implementation does not discover, reconcile or reload providers.

These are not merely missing target-platform features: items 1–5 are consistency hazards in the current mutable registry under concurrent use, and item 4 is a deterministic mapping-loss case when QName collisions occur.

#### The Core XML registry is a separate global overwrite surface

\`CorePluginRegistry\` is a process-wide static singleton. \`registerMapping\` unconditionally puts a class by element name, so a duplicate element silently replaces the previous mapping. \`clear()\` clears the singleton map globally. It has no runtime identity, descriptor provenance, activation state, collision diagnostics or immutable generation.

\`PluginPackageScanner\` is explicitly opt-in, enumerates classes from configured packages/classloaders, inspects \`@CorePlugin\`, and writes mappings directly into that global singleton. It is not a managed artifact watcher/reconciler and does not provide descriptor-first inspection or host-controlled activation. The default Core registrar remains a distinct registration path.

#### Existing tests do not exercise the identified consistency contracts

The inspected \`DefaultPluginRegistryTest\` and \`DefaultPluginRegistryUnregisterResidualBranchContractTest\` cover sequential registration, sequential unregister, basic lookup, category/namespace cleanup and input validation. They do not test:

- concurrent duplicate-ID registration or concurrent register/unregister/read;
- duplicate QName rejection and preservation of the surviving mapping after unregister;
- atomic visibility across all indexes;
- failed registration rollback;
- provider class deferral/activation;
- meaningful \`refresh()\` reconciliation;
- runtime isolation or immutable registry generations.

No test was run in this pass. These are test-source observations, not claims that an unexecuted test fails.

#### Required target contract / acceptance additions

For #346/#347/#350, the authoritative registry must:

- build a complete candidate state off to the side and publish it atomically as one immutable generation;
- reject duplicate stable IDs, QNames and aliases deterministically before publication (unless an explicit, versioned alias-resolution rule is approved);
- never let removal of one provider erase another provider's mapping;
- keep descriptor discovery separate from implementation construction and activation;
- serialize or compare-and-swap candidate publication so stale concurrent scans cannot overwrite newer state;
- pin execution and semantic validation to the same generation;
- preserve last-known-good state when candidate validation, provider preparation or activation fails;
- test concurrent reconciliation, duplicate/collision rollback, add/update/remove/re-add, stale-scan races, runtime isolation and active-execution retirement.

The current \`DefaultPluginRegistry\` should therefore be treated as a legacy compatibility implementation during migration, not extended incrementally into the target authoritative registry without a new atomic state model.

#### Exact source/test blobs (current \`main\`)

- \`DefaultPluginRegistry.java\`: \`da5c6f6e835cc8b551394e0853a70ca4bcd60d2f\`.
- \`PluginRegistry.java\`: \`eceeddca531c98ca4a7f235e04ab28511bfc9281\`.
- \`CorePluginRegistry.java\`: \`20f478addbf2bacc481e4e11e0328f06942a2a38\`.
- \`PluginPackageScanner.java\`: \`c0e6cbff6cb8c472f66c4a897263cb3c3c7f8232\`.
- \`DefaultPluginRegistryTest.java\`: \`ee2dd3c932637c39bd25282505d06c940a286f64\`.
- \`DefaultPluginRegistryUnregisterResidualBranchContractTest.java\`: \`07de046e581e135ee41224bf3f15dec51f797ae3\`.

**Evidence boundary:** direct source inspection and test-source review on GitHub \`main\`. No tests, builds, concurrency experiments or runtime changes were made. This section is an evidence-backed audit addition, not approval of the SPI or authorization to change production registry behavior.


### 15.52 Discovery cache, classloader identity and catalog refresh gaps (2026-11-23)

This pass traces the current classpath discovery path into metadata catalog construction and compares it with the separate package scanner. It is source/test review only; no tests or runtime experiments were executed.

#### Discovery and metadata are process/classloader scoped, not runtime scoped

1. \`PluginClassScanner.cachedClassNames\` is one static volatile cache guarded by \`PluginClassScanner.class\`. It has no runtime, artifact, classloader or scan-generation key. \`clearCache()\` clears this process/classloader-wide cache for every caller using that defining classloader.
2. The scanner asks \`ClassPathScannerImpl\` to scan with \`ClasspathHelper.classLoaders()\`. The returned \`Class<?>\` objects are reduced to class-name strings in a \`LinkedHashSet\`; the classloader that exposed each class is discarded.
3. \`PluginMetadataCatalog\` is itself static and eagerly initializes \`DESCRIPTORS\` once when the class is initialized. It iterates the class-name list, calls one-argument \`Class.forName(className)\` (which uses the caller's defining loader and initializes the class), then invokes the implementation's no-argument constructor to obtain metadata.
4. Consequently, a class visible only through a child/plugin classloader can be found by the discovery scan but its loader identity is not carried into metadata construction. The subsequent \`Class.forName(String)\` does not use the loader that discovered it. This is a concrete discovery-to-load identity mismatch in the current path.
5. Clearing \`PluginClassScanner\`'s cache does not rebuild \`PluginMetadataCatalog.DESCRIPTORS\`: the latter is an immutable static map with no refresh/reconcile API. A later scan therefore cannot, through the inspected public catalog path, publish a changed descriptor set.
6. \`buildRuntimeCatalog()\` uses \`descriptors.put(descriptor.getId(), descriptor)\`; duplicate descriptor IDs silently replace earlier descriptors in iteration order. There is no collision diagnostic or artifact provenance in the resulting key.
7. By contrast, \`PluginPackageScanner\` explicitly retains a \`Map<String, ClassLoader>\` while enumerating package classes and calls \`Class.forName(name, false, candidateLoader)\`. This demonstrates that loader identity is available in a separate legacy path, but that path writes classes directly into the global \`CorePluginRegistry\` rather than producing a runtime-scoped candidate catalog.

#### Existing tests do not establish dynamic catalog behavior

The inspected \`PluginClassScannerTest\` covers discovery, fallback, sorting, cache reuse and explicit cache clearing. \`PluginMetadataCatalogDynamicTest\` covers initial metadata projection and schema/UI fields. \`PluginPackageScannerClasspathTest\` verifies a class loaded from a temporary JAR using a child \`URLClassLoader\`, but it exercises the separate package scanner and global Core registry.

The inspected tests do not establish:

- two independent runtime catalogs with distinct classloader visibility;
- descriptor discovery and metadata construction using the exact loader that exposed the provider;
- add/update/remove after \`PluginMetadataCatalog\` has initialized;
- cache invalidation scoped to one runtime without affecting another;
- duplicate descriptor ID rejection with artifact provenance;
- safe retirement of a loader after its provider is removed.

The temporary-JAR scanner test is useful evidence for the separate package scanner's loader-aware path; it must not be read as proof that \`PluginClassScanner\` + \`PluginMetadataCatalog\` support the same behavior.

#### Required target contract / acceptance additions

For #346/#347/#350:

- discovery results must retain an explicit artifact/provider identity and owning classloader or isolated process handle; do not reduce candidates to bare class-name strings;
- each runtime must own its discovery state, descriptor catalog and immutable registry generation; no static process-wide cache or catalog may be authoritative;
- descriptor parsing must not initialize or instantiate provider implementation classes;
- metadata inspection must be possible from data-only descriptors before activation;
- refresh must build and validate a candidate generation, detect duplicate IDs/QNames deterministically, and publish atomically; failed refresh must preserve last-known-good state;
- classloader/process retirement must wait for executions pinned to the old generation, then release provider instances and resources deterministically;
- add tests for two runtimes with separate loaders, child-loader-only provider discovery, add/update/remove, duplicate identity, failed refresh rollback, stale refresh races and old-generation execution drain.

The existing \`PluginClassScanner\` and \`PluginMetadataCatalog\` should remain legacy compatibility surfaces during migration, not be extended as the target hot-discovery/catalog mechanism.

#### Exact source/test blobs (current \`main\`)

- \`PluginClassScanner.java\`: \`dadd28a434000816253ac66bff736e2c5fff0494\`.
- \`PluginMetadataCatalog.java\`: \`5ac82d7fb6436b458b29647e4c929878557b1c6e\`.
- \`ClassPathScannerImpl.java\`: \`ee310a0f79fbe196cf0ddd7f3f9896aa129b2596\`.
- \`PluginPackageScanner.java\`: \`c0e6cbff6cb8c472f66e4c897263cb3c3c7f8232\`.
- \`PluginClassScannerTest.java\`: \`acb1d90457c8912f23f0078a9e0f73092178c221\`.
- \`PluginMetadataCatalogDynamicTest.java\`: \`c3582c89aad930e15c714b434b0a5c4aba4d6425\`.
- \`PluginPackageScannerClasspathTest.java\`: \`ff9c42f4bac2efdc6b97185c1358772daf628192\`.

**Evidence boundary:** direct source and test-source inspection on GitHub \`main\`. No test/build/runtime experiment was run. No production source or POM was changed. This section adds evidence for the existing #347 hot-discovery and #349 runtime-isolation requirements; it does not authorize an implementation design beyond the approved SPI decision.


### 15.53 Execution/session lifecycle does not pin plugin generations (2026-11-23)

This pass follows the current Core plugin factory, registry, execution context and session lifecycle. It is source inspection only; no tests, builds or runtime experiments were executed.

#### A session retains execution state, but no plugin-catalog generation

- `ScraperSession` exposes the configuration, execution context, status and metrics. Its API has no runtime identity, plugin-catalog snapshot, provider/artifact identity, generation token or lease/retirement handle.
- `ScraperSessionImpl.markRunning(context)` stores the `DynamicScopeContext`; the session does not capture a plugin registry/catalog snapshot alongside it. `DynamicScopeContext` exposes session and EventBus references, but the inspected interface has no generation or runtime-scoped provider-catalog accessor.
- `SessionRegistryImpl` is only a concurrent map of sessions. Register/unregister/clear do not acquire or release plugin/provider resources, and unregistering a session does not imply execution completion or provider retirement.
- The session cancellation contract is cooperative. A terminal status transition is not itself evidence that every executing plugin body has unwound or released provider resources.

#### Factory and legacy registry are instance/class based, not generation based

- `DefaultPluginFactory` resolves a plugin ID or QName through `PluginRegistry`, obtains the registered plugin instance's implementation class, then creates a new instance through reflection. Its constructor cache is keyed by `Class<? extends Plugin>`; it has no provider artifact identity or generation key.
- `DefaultPluginRegistry` stores live plugin instances, metadata and class/QName/category indexes in mutable maps. It has no immutable snapshot or lease API. Unregister removes index entries but does not call `Plugin.cleanup()`, track active executions, or defer classloader disposal.
- The factory's class-based creation path can initialize a newly created instance when configuration is supplied. The current model therefore mixes a registry of representative instances with per-configuration instances; neither is bound to an immutable catalog generation.
- `AbstractPlugin.execute()` runs the plugin body and invokes `doCleanup()` in its `finally` path after each execution. Separately, `Plugin.cleanup()` invokes `doCleanup()` when the instance remains initialized. This legacy lifecycle is not a provider-generation retirement protocol: it does not count in-flight calls, distinguish execution cleanup from provider disposal, or coordinate classloader lifetime.

#### Consequence and evidence boundary

The inspected public session/factory/registry contracts do not establish that an execution started before a refresh continues using the same provider generation after that refresh. Nor do they establish that a removed provider remains loaded until all active executions drain. This is an absence-of-contract finding, not proof of a specific observed cross-generation race: no concurrent runtime experiment was run.

The required architecture is a runtime-owned immutable catalog generation acquired at execution admission. The execution/session (including nested plugin-body execution) must retain a lease to that exact generation; refresh publishes a new generation atomically for new executions; retirement stops new admissions to the old generation and releases provider instances/classloaders only after its active lease count reaches zero. Provider disposal must be distinct from per-invocation cleanup. Runtime close must stop admissions, drain or explicitly cancel executions, then release only runtime-owned resources.

#### Required conformance tests for #347/#349/#350

1. Start a long-running execution under generation G1, publish G2, and prove the active execution and all nested plugin calls still resolve G1 while newly admitted executions resolve G2.
2. Remove/update a provider while its execution is active; prove its instance and classloader remain valid until the final G1 lease is released, then are closed exactly once.
3. Fail candidate refresh and prove G1 remains the last-known-good generation for new and active executions.
4. Run two independent runtimes with distinct provider versions/classloaders and prove no cross-runtime resolution, retirement or cleanup.
5. Exercise success, failure, interruption, cancellation, nested execution and runtime close; prove every lease is released exactly once and no provider is disposed while in use.
6. Verify session-registry unregister/clear cannot silently retire providers still referenced by active execution.

#### Exact source blobs (current `main`)

- `Plugin.java`: `29772a9b37c8fead735f4a5e0bd47e5b90b4367e`.
- `AbstractPlugin.java`: `be3298304840e4a585b4fb148c2e62cf471ee8f7`.
- `DefaultPluginFactory.java`: `91c8084f671027954375ff3cab784691f8ae26e5`.
- `DefaultPluginRegistry.java`: `da5c6f6e835cc8b551394e0853a70ca4bcd60d2f`.
- `DynamicScopeContext.java`: `f5ca038f25442f597986bf64ebc65e958b92a010`.
- `ScraperSession.java`: `c8d0086ea160e2d8f02657c4cc9aa9d11d65b237`.
- `ScraperSessionImpl.java`: `78d07d1c485ea215d68d6d415f36b2be0c180a4f`.
- `SessionRegistryImpl.java`: `8813c995b6f2c7e4b4adb3a4a12d57cbb3c6c124`.
- `ScraperModule.java`: `d31a5e30b0949c752743123cbcfb4219ba60713e`.

**Evidence boundary:** direct inspection of GitHub `main` source. No tests, builds, concurrency experiments, runtime changes or POM changes were made. This is an audit finding and acceptance-test proposal only; it does not authorize implementation or freeze the SPI.


### 15.54 Polymorphic termination contract: GRACE and KILL (2026-11-23)

This section records the required lifecycle semantics raised for the plugin platform. It is a target-contract proposal derived from the existing generation/session/lifecycle gaps above; it is not an implementation claim or authorization to change runtime code.

#### One host-controlled contract across provider types

Termination must not depend on concrete plugin classes or provider-specific stop methods. The runtime should manage a common execution handle and immutable termination request for every execution kind, including nested plugin calls and provider adapters. The request should carry at least:

- unique execution and runtime identity;
- reason and initiator (user stop, runtime shutdown, timeout, parent cancellation, provider failure);
- mode: `GRACE` or `KILL`;
- monotonic deadline / remaining budget (never wall-clock-only timeout arithmetic);
- cancellation generation/token so stale or duplicate signals cannot revive or affect a later execution;
- parent/child relationship and the pinned provider-catalog generation.

Providers receive a narrow, runtime-scoped cancellation/execution context. The host owns the state machine, deadline, admission control, resource registry, lease accounting and final outcome. Provider callbacks are cooperative extension points, not authorities over runtime state.

#### GRACE semantics

1. Atomically transition the execution from `RUNNING` to `STOP_REQUESTED_GRACE`; repeated requests are idempotent and cannot extend the original deadline unless an explicit policy permits it.
2. Stop admitting new top-level work and new nested work for that execution. Propagate the same cancellation token/deadline to all child executions, async tasks and provider calls.
3. Notify provider code through the common cancellation context and interruptible APIs. Allow bounded completion/rollback of safe sections; do not silently treat a stop request as success.
4. Wait until the configured deadline for execution scopes, child tasks and resource operations to settle. Record whether the task completed, cancelled, failed or exceeded its deadline.
5. Run deterministic cleanup exactly once, release the execution's generation lease only after no code can still use that generation, and then allow old-generation retirement.

GRACE is a request for orderly termination, not a promise that arbitrary third-party code will cooperate.

#### KILL semantics and the in-process Java boundary

KILL is an escalation, not a stronger Java callback:

- Immediately close admission, cancel child tasks, signal cancellation and issue thread interruption where the execution contract supports it.
- Close host-owned cancellable I/O/resources through registered handles, with idempotent close and bounded cleanup.
- If execution is still running after escalation, mark it `UNRESPONSIVE/QUARANTINED`; do not report it as terminated, do not release its generation lease, and do not unload its classloader while its code may still be executing.
- Do not use `Thread.stop()`, unsafe asynchronous exception injection, or classloader disposal as a kill mechanism. Java cannot safely and reliably force-stop arbitrary code running in-process.
- If the product requires a hard kill guarantee for untrusted or non-cooperative providers, execute those providers behind a process boundary (worker process/container/OS isolation). KILL then means terminate the worker process, verify process exit, reclaim its OS resources, and only then release the corresponding generation/process lease. In-process providers remain trusted/cooperative code and cannot be advertised as hard-killable.

A timeout or interrupt request alone is not proof of termination. The externally visible result must distinguish `TERMINATED`, `CANCELLED`, `FAILED`, `KILL_ESCALATED` and `UNRESPONSIVE` (or an equivalent versioned outcome model).

#### Lifecycle state machine and invariants

The target model should define and test legal transitions, at minimum:

`CREATED -> RUNNING -> STOP_REQUESTED_GRACE -> TERMINATING -> TERMINATED`

Escalation path:

`STOP_REQUESTED_GRACE -> KILL_REQUESTED -> TERMINATING -> TERMINATED`

Non-cooperative in-process path:

`KILL_REQUESTED -> UNRESPONSIVE/QUARANTINED`

Failure/cancellation may be terminal outcomes only after child work and resource ownership have been reconciled. No transition may move a terminal execution back to running. A stale signal for an earlier execution/cancellation generation must be ignored.

Core invariants:

- exactly-once terminal publication and cleanup;
- no new work after stop admission closes;
- cancellation/deadline propagation through every nested call and asynchronous boundary;
- parent cancellation reaches descendants; child failure does not accidentally cancel unrelated siblings unless policy says so;
- generation lease remains held until all execution/child/provider activity is proven quiescent;
- runtime shutdown drains or quarantines executions before disposing providers;
- one runtime's GRACE/KILL cannot cancel, interrupt, close or retire another runtime's work/resources;
- cleanup and resource close are idempotent, bounded and observable;
- hard-kill guarantees are advertised only for process-isolated execution.

#### Required conformance matrix for #346/#347/#349/#350

1. GRACE on a simple provider: signal observed, no new nested work admitted, bounded cleanup, one terminal outcome.
2. GRACE on nested and asynchronous work: all descendants inherit the same deadline/token; no orphan task or leaked generation lease.
3. GRACE deadline expiry escalates once to KILL; duplicate/stale signals are harmless.
4. Cooperative in-process KILL: interruption and registered I/O closure are observed; termination is reported only after actual quiescence.
5. Non-cooperative in-process provider ignores interrupt: execution becomes UNRESPONSIVE/QUARANTINED, its lease/classloader remain retained, and runtime remains honest about incomplete termination.
6. Process-isolated provider ignores GRACE: worker is killed, process exit verified, OS resources reclaimed, and lease released only after confirmed exit.
7. Runtime A shutdown while B is active: A's sessions/providers/resources terminate without affecting B.
8. Concurrent stop, natural completion, failure and timeout races: exactly one terminal outcome and exactly-once cleanup.
9. Refresh G1->G2 during GRACE/KILL: old executions remain pinned to G1 until quiescence; new executions use G2.
10. Verify monotonic deadline handling, bounded cleanup, observability/audit trail and no resurrection after terminal state.

**Decision boundary:** GRACE can be guaranteed as a protocol for cooperative providers. Hard KILL cannot be guaranteed for arbitrary in-process Java plugins. If hard termination is a product requirement, process isolation is an architectural prerequisite, not an optional future optimization. No tests/builds/runtime/POM changes were made in this audit pass.


### 15.55 Session cancellation is a terminal status signal, not execution termination (2026-11-23)

A focused trace from the public session API through the asynchronous service and plugin executor identifies a concrete lifecycle mismatch that the GRACE/KILL contract must correct:

- `ScraperSessionImpl.cancel()` synchronously changes any non-terminal session to `CANCELLED`, marks its metrics ended, posts `SessionCancelledEvent`, and signals the completion condition. It does not hold or cancel a worker `Future`, interrupt the executing thread, or wait for plugin/child execution to unwind.
- Consequently, `awaitCompletion()` and its timed overload return as soon as the session status becomes terminal. They do not currently mean that the worker stopped, descendants settled, or session-owned resources were released. The session API documentation describes cancellation as cooperative, but the implementation's immediate terminal transition makes this distinction especially important for callers.
- `WebHarvestServiceImpl.executeAsync()` submits the session runnable and discards the returned `Future`; no execution handle is retained for later cancellation. `cancelSession()` delegates only to `ScraperSession.cancel()`.
- `shutdownNow()` separately calls `cancel()` on registered sessions and then invokes `executorService.shutdownNow()`. This interrupts executor workers as a separate service-level action, but it still does not establish that plugin code has unwound. Graceful `shutdown()` calls `shutdown()` and waits up to 60 seconds; it ignores the boolean result of `awaitTermination` and logs shutdown complete even if termination was not confirmed.
- The optional per-session timeout path creates a new single-thread executor, submits the scraper, and cancels its `Future` on timeout, but does not retain or shut down that executor. This is a separate executor-ownership defect to resolve in the same lifecycle redesign; a timeout-triggered interrupt is not proof of worker termination.
- `DefaultPluginBodyExecutor.executeChildrenInternal()` correctly propagates `InterruptedException` and prevents later siblings from running. However, this only covers code paths that actually observe thread interruption; it does not provide a session-wide cancellation token, descendant registry, or a way for the host to reach every active worker.
- Nested execution remains synchronous in the inspected `LoopPlugin` and `CallPlugin` paths, but each constructs a fresh `DefaultPluginBodyExecutor`. Those children inherit the same mutable context object, not an explicit immutable execution handle with a shared cancellation generation/deadline. The broader nested-dispatch inventory in 15.37/15.40 remains applicable.
- `WebBrowserPlugin` listens for completed, failed and cancelled session events and closes its browser state on those events. Because the cancellation event is emitted before worker quiescence is established, this event currently signals a requested/declared session state, not proof that all in-flight browser calls have stopped. Resource close must be idempotent and coordinated with active operations in the future contract.

**Test coverage qualification:** `ScraperSessionTest` verifies status transitions and that waiters wake after completion/cancellation, but it has no blocked worker/plugin proving whether cancellation interrupts or drains it. `WebHarvestServiceTest.testCancelSession_ExistingSession` permits cancellation or an already-terminal status after a sleep; it does not assert worker exit, descendant completion, resource release, or absence of post-cancel side effects. The inspected tests therefore do not establish the semantics required by GRACE/KILL.

**Required correction in the target contract:**

1. Separate `STOP_REQUESTED` from terminal execution outcomes. A stop request must not mark metrics ended or satisfy `awaitTermination/quiescence` until the host has reconciled the worker, descendants and registered resources.
2. Retain a host-owned execution handle for every admitted session and nested/async task. It must support atomic admission closure, cancellation propagation, supported interruption, bounded drain, and observable worker completion.
3. Define distinct waits: status/request acknowledgement versus execution quiescence. A compatibility `awaitCompletion()` must have one documented meaning and must never report quiescence solely because a stop flag changed.
4. Make timeout and shutdown use the same cancellation protocol and monotonic deadline. Retain and deterministically close every executor created by the runtime; never leave timeout helper executors detached.
5. Test with latch-controlled cooperative and non-cooperative plugins: stop request while blocked, no new sibling/child admission, GRACE drain, KILL escalation, late completion after cancellation, browser/resource close racing an active call, service shutdown deadline, and proof that `awaitQuiescence` remains false while worker code is still running.

This is a source/test inspection finding, not a reproduced live race; no tests were executed. It is stronger than a missing-interface observation: the immediate status/event/completion behavior and discarded execution `Future` are directly visible in the inspected implementation.

**Exact source/test blobs at `main` snapshot `1729bdf4620a66686d02605083e27e27d1ab3944`:**

- `ScraperSessionImpl.java`: `78d07d1c485ea215d68d6d415f36b2be0c180a4f`.
- `ScraperSession.java`: `c8d0086ea160e2d8f02657c4cc9aa9d11d65b237`.
- `WebHarvestServiceImpl.java`: `10e60a93a70b417cdb55dd674ad2e5bc8f9bbdec`.
- `DynamicScopeContext.java`: `f5ca038f25442f597986bf64ebc65e958b92a010`.
- `DefaultPluginBodyExecutor.java`: `0116211ed31986b757f6f9e4bb5689f199b54f69`.
- `LoopPlugin.java`: `ba91342e4e560f6bea9165bdba9b5ce4b4db82d8`.
- `CallPlugin.java`: `01238d278e4bbddb262c797f34a481dcd964e3c9`.
- `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`.
- `ScraperSessionTest.java`: `31988741de660cb7455851e98ff75a706ea7d3dc`.
- `WebHarvestServiceTest.java`: `024d1aeb30dc1ec6a97cfd0d1afa847c0eccf07c`.


### 15.56 Interruption propagation audit: selected plugins swallow stop signals (2026-11-23)

The previous section established that the service/session layer does not retain a uniform execution handle. This follow-up checks whether interruption, when it does reach plugin code, is consistently propagated. It is not. The findings below are direct control-flow observations from the current `main` snapshot, not claims that every blocking operation is interruptible.

#### Confirmed propagation paths

- `DefaultPluginBodyExecutor.executeChildrenInternal()` explicitly rethrows `InterruptedException` and does not execute later siblings after interruption.
- `IncludePlugin.doExecute()` catches `InterruptedException` separately, restores the thread interrupt flag, and rethrows; its `finally` restores the parent Config before the signal escapes.
- `XPathPlugin.getXmlContent()` catches `InterruptedException` separately and rethrows it.
- Existing `IncludePluginTest.shouldRestoreParentConfigWhenIncludedExecutionIsInterrupted`, `CasePluginTest.testCasePluginExecuteWithInterruptedException` and `TryPluginTest.testTryPluginExecuteWithInterruptedException` explicitly exercise interruption propagation/interrupt-flag restoration.

These are useful compatibility behaviors to preserve, not proof that all plugin paths comply.

#### Confirmed interruption-swallowing paths

1. **File write body execution.** `FilePlugin.handleFileWrite()` calls `super.executeBody(context)` inside `catch (Exception)`. Since `InterruptedException` is an `Exception`, it is converted to `EmptyVariable.INSTANCE`; execution then continues into the binary/text write path. The caller may therefore write empty/fallback content after a child execution was interrupted. The outer file operation also catches broad `Exception` and returns an empty result. The top-level `FilePluginTest` exercises file operations but contains no interruption/cancellation assertion.
2. **XQuery parameter child execution.** `XQueryPlugin.executeChildElements()` declares `throws InterruptedException`, calls `paramPlugin.execute(context)`, but catches broad `Exception` and returns `EmptyVariable.INSTANCE`. An interruption from a nested parameter plugin is therefore swallowed and XQuery processing may continue with an empty parameter value. The inspected `XQueryPluginTest` is only 25 lines and contains no interruption test; the other XQuery branch-contract tests were not shown to exercise interruption in this pass.
3. **Regexp result-template execution.** `RegexpPlugin.processResultTemplate()` invokes `DefaultPluginBodyExecutor.executeBody()` inside `catch (Exception)`, then returns the original template on failure. An interruption in a nested `regexp-result` child is converted into fallback text; the outer matching loop may continue. By contrast, `extractChildText()` declares `InterruptedException` and is called before the main regex try/catch, so interruption in pattern/source child extraction can propagate. This is a split behavior within one plugin, not a blanket statement that all Regexp interruption is swallowed. The inspected Regexp test suite covers matching and templates but contains no interruption/cancellation assertions.
4. **HTTP request/resource cancellation seam.** `HttpPlugin.doExecute()` catches broad `Exception` and wraps it as `IllegalStateException`. `HttpServiceImpl.executeRequest()` owns the synchronous `httpClient.execute(request)` call and response buffering, exposes no per-request cancellation handle to the plugin/runtime, and only closes the shared client through service-level `close()`. The inspected request config sets redirect behavior but does not configure a request/response timeout. This audit does not claim that Java thread interruption always aborts the underlying Apache request; rather, the host currently has no explicit request-scoped abort handle or verified cancellation/quiescence contract for it. Existing HTTP plugin/service tests inspected here contain no interruption or in-flight cancellation test.

#### Consequence for GRACE/KILL and polymorphism

A provider-neutral cancellation contract cannot rely only on `Thread.interrupt()` or on every provider author remembering to rethrow it. The host must own cancellation state and execution/resource handles; plugin adapters must preserve cancellation as a distinct control signal, never downgrade it to an ordinary empty value, fallback template, or partial side effect.

The future SPI/adapter conformance contract should require:

- cancellation is a distinct control outcome, not a normal provider failure/result;
- broad exception handlers must detect and rethrow cancellation/interruption before fallback/error conversion;
- resource-bearing operations register host-visible cancellable handles (HTTP request, file write transaction/temp-file strategy, browser operation, DB statement/connection, FTP transfer, etc.);
- no irreversible commit/side effect after cancellation is observed; file writes should use a defined atomic/partial-write policy;
- parent/child and nested parameter/template execution share the same cancellation token and monotonic deadline;
- conformance tests inject interruption at every nested boundary and assert no fallback work, later sibling, file commit, request continuation, or generation-lease release before actual quiescence.

**Evidence / coverage boundary:** source review only; no tests were executed. The audit confirms concrete catch blocks that swallow `InterruptedException` in File, XQuery and Regexp paths. It does not claim that all listed plugins or all Java blocking calls are non-interruptible.

**Exact source/test blobs at `main` snapshot `1729bdf4620a66686d02605083e27e27d1ab3944`:**

- `FilePlugin.java`: `8d78e797975ab236a47420d18f28600beaae04bf`.
- `XQueryPlugin.java`: `b1f0ee00c16c9a37144e7e27cb46bfd57de6e9f1`.
- `RegexpPlugin.java`: `a860ab08554122680c4d5d5fde1be3d683139c6a`.
- `HttpPlugin.java`: `c6045d6c7638f3aa86c510240814633597b108ad`.
- `HttpServiceImpl.java`: `9e3dab1a40250f4d059ea812c74012d994873f0d`.
- `DefaultPluginBodyExecutor.java`: `0116211ed31986b757f6f9e4bb5689f199b54f69`.
- `IncludePlugin.java`: `ab06127f7f5402978937160a42455b4c0107dfab`.
- `XPathPlugin.java`: `e519eb21a2e6faeef43a9d23c219502ac7f7f2a6`.
- `FilePluginTest.java`: `2a1df9027b111dd412d41696542720a12b1a8e51`.
- `RegexpPluginTest.java`: `a5615e591599468b0dc0a846e26341f2d834233b`.
- `XQueryPluginTest.java`: `6be2233dd74f49716ccbda46156bb9441d6c469c`.
- `HttpPluginTest.java`: `1b66e0d035c378f30ab784d848aabf7eb29fb0ef`.
- `IncludePluginTest.java`: `65ce0add3fb45651913f9dea3000b1ec32f61b9a`.
- `CasePluginTest.java`: `25021e40e0025aa4e19232ea364954f8278d6ac9`.
- `TryPluginTest.java`: `3593103011139561cb2079eb1cc0a4e4a0ccdbdf`.


### 15.57 Optional provider cancellation and resource ownership audit (2026-11-23)

The same cancellation/exception review was extended across the five optional integration modules. This section distinguishes interruption propagation, synchronous external operations and resource ownership. No live cancellation experiment or test execution was performed.

#### Database

- `DatabasePlugin.getBodyContent()` is called before the main JDBC try/catch and declares `InterruptedException`; that body interruption can escape. The SQL phase then uses synchronous `PreparedStatement.executeQuery/executeUpdate`, iterates the `ResultSet`, and closes ResultSet/Statement in `finally`.
- The inspected SQL execution path does not set `Statement.setQueryTimeout` or call `Statement.cancel()`; the host has no per-statement cancellation handle in this plugin. Interrupting a worker therefore cannot be treated as proof that a JDBC driver stopped the SQL operation.
- `ConnectionPlugin` stores a session-owned connection factory in the mutable execution context. The factory registers with the session EventBus and is closed on session lifecycle events; because the cancellation event currently precedes quiescence (15.55), pool close may race active statements. The pool proxy also intentionally suppresses ordinary connection close when session event ownership is active. The target contract must distinguish logical close request from driver-confirmed statement/connection quiescence.

#### FTP

- `FtpPlugin.doExecute()` invokes `executeAction()` inside a broad `catch (Exception)`; its action methods declare `InterruptedException`, so interruption from nested FTP PUT body execution is converted into a generic runtime failure. The `finally` block logs out/disconnects only after the synchronous action returns or throws.
- `FtpPutPlugin.doExecute()` calls `getBodyContent()` inside a broad `catch (Exception)`; nested-body interruption is wrapped as a generic FTP failure. `FtpGetPlugin` and `FtpListPlugin` resolve optional body paths before their FTP try blocks and therefore allow interruption from that preliminary body execution to escape; their subsequent `retrieveFile/listFiles` operations are synchronous and expose no host-visible per-transfer cancellation handle.
- `FtpGetPlugin`, `FtpPutPlugin` and `FtpListPlugin` disconnect in `finally`, but this is post-unwind cleanup, not a way for the host to reach and abort an active transfer. `FtpPlugin` similarly disconnects only after its action call returns.
- The inspected FTP plugin test files contain no interruption/cancellation assertions; no test was executed.

#### Mail

- `MailPlugin.getBodyContent()` declares `InterruptedException`, but is called inside the broad `catch (Exception)` in `doExecute()`; interruption is converted to a generic mail failure. The plugin then performs synchronous `email.send()` without exposing a host-owned SMTP operation handle or an explicit cancellation/abort hook.
- The inspected MailPlugin unit/integration test sources contain no interruption/cancellation assertions; no test was executed.

#### WebBrowser

- `WebBrowserPlugin.doExecute()` catches broad `Exception`. `handleLoad()` does catch `InterruptedException` separately and restores the thread interrupt flag before wrapping it in `PluginException`; `handleExecuteScript()`, however, catches broad `Exception` around `Thread.sleep()` and wraps interruption without restoring the flag. Thus the load and execute-script paths have inconsistent interruption semantics.
- `handleLoad()` creates a new `WebClient` in a local variable and only registers it in the session's `BrowserSessionState` after page load and optional JavaScript waits. If interruption/failure occurs before registration, the session cancellation listener cannot find that client to close it; the inspected method has no failure-path `finally` closing the local client. This is a concrete ownership gap to address.
- The session state closes registered clients on completed/failed/cancelled events. As established in 15.55, cancellation event publication is not a quiescence barrier; there is no active-operation reference count/lease around calls using a stored `WebClient`. Closing a client may therefore race an in-flight browser operation.
- The inspected WebBrowser lifecycle/unit test sources contain no interruption/cancellation assertions; no test was executed.

#### ZIP

- The ZIP path is a positive example: `ZipPlugin.doExecute()` runs child execution inside try-with-resources for the `ZipOutputStream`; interruption from `executeBody()` can escape while the stream is still deterministically closed. `ZipEntryPlugin` executes its body before writing the entry, so an interruption in child-body execution occurs before that entry's write/commit.
- This does not make ZIP execution independently cancellable during byte-array compression/writes, but its current resource scope is clearer than the other inspected adapters.

#### Cross-module target requirements

- The common host cancellation handle must be able to reach active JDBC statements/connections, FTP control/data sockets and transfers, SMTP transport, HtmlUnit clients/background jobs, HTTP requests, file writes and ZIP streams.
- Each adapter must declare whether an operation is cooperatively interruptible, externally abortable, only bounded by a timeout, or non-interruptible; the host must not infer termination from thread interruption.
- Cleanup must be coordinated with in-flight operation leases. An event notification may request close, but resource disposal must wait for safe quiescence or escalate to process isolation where appropriate.
- Side-effecting operations need explicit cancellation commit semantics: a cancelled FTP upload, SMTP send, JDBC update or file write may have crossed an irreversible boundary. The runtime must report outcome as indeterminate/committed/aborted where the underlying protocol cannot prove rollback; it must not falsely report a clean cancellation.
- Add deterministic fake-server/fake-driver tests with latches for active JDBC query/update, FTP retrieve/store/list, SMTP send, HtmlUnit page load/background JS, and ZIP child execution; verify interruption type/flag, abort visibility, cleanup timing, no use-after-close, and exactly-once terminal outcome.

**Evidence / coverage boundary:** current-main source and selected test-source inspection only. No test suite, live server, JDBC driver experiment or network cancellation experiment was run. Findings about resource abortability are limited to the absence of an explicit host-visible operation handle in the inspected plugin/service paths; they do not claim that a particular third-party library can never respond to interruption.

**Exact source/test blobs at `main` snapshot `1729bdf4620a66686d02605083e27e27d1ab3944`:**

- `DatabasePlugin.java`: `b3934d753f17f133bd582b50206db701af843728`.
- `ConnectionPlugin.java`: `edb6ed93584cb03d967c830d120848c7e891f481`.
- `FtpPlugin.java`: `d7098611ea7e0569b7cf9af01346be7b951da799`.
- `FtpGetPlugin.java`: `fd4ee5da4556bf91bdae7940072cf7a5e106f6d5`.
- `FtpPutPlugin.java`: `34061eb4fbc3a70f95d8d90a3a2faef611659f82`.
- `FtpListPlugin.java`: `c5fcfa6c5f90f643e6df8e85f6ad6a0b6e1074fa`.
- `MailPlugin.java`: `083297481e7e218916a77f0dea2308c63bbac7e7`.
- `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`.
- `ZipPlugin.java`: `e62046292bca8689637e5eefbd7ce25ed460d118`.
- `ZipEntryPlugin.java`: `ea90361ca90b31d4c3903688ca78204121af0743`.
- `DatabasePluginTest.java`: `34002d58b1dc63beaa9df3aeb487073fc0a4bb2e`.
- `FtpPluginTest.java`: `dfeec2757e16f71015bc87537223b4e54e21f608`.
- `MailPluginTest.java`: `fcd0b6c2a895a0a31fddd8443696cdb7e8de0462`.
- `WebBrowserPluginLifecycleTest.java`: `572f475dacabb7f706ce150d945a6dd762a83114`.
- `ZipPluginContractTest.java`: `0c1a161a913106db28f59c2c788fe628a26088f0`.


### 15.58 External side-effect commit boundaries and honest cancellation outcomes (2026-11-23)

This pass traces where selected Core and optional providers cross an externally visible side-effect boundary. The key design correction is that a stop request and a plugin exception do not establish whether an external effect was committed. The host must report execution termination separately from effect disposition.

#### Source observations

1. **Filesystem writes are direct, not staged.** `FilePlugin.handleFileWrite()` creates parent directories and opens the destination with `new FileOutputStream(file, append)`. With `append=false`, opening the stream truncates an existing destination before the write completes; with `append=true`, bytes are appended directly. There is no temporary sibling file, fsync/atomic rename, backup, or rollback protocol in this method. A write failure can therefore leave a truncated or partially modified destination. Its broad catches return `EmptyVariable`, so the caller does not receive a structured partial/indeterminate outcome.
2. **FTP upload crosses a remote commit boundary inside `storeFile()`.** `FtpPutPlugin` sends the complete in-memory body through synchronous `FTPClient.storeFile(path, inputStream)`; it treats a false return as failure and disconnects in `finally`. It does not upload to a unique temporary remote name and rename after verification. If the control/data connection fails around the final server response, the client may not know whether the server retained all, some, or none of the destination. The code has no durable operation ID or reconciliation step. This is a protocol-dependent uncertainty, not a claim that every failed upload leaves a partial file.
3. **Database update effects are delegated to JDBC/driver transaction defaults.** `DatabasePlugin.executeSql()` calls `executeUpdate()` and returns the row count; the plugin does not explicitly set auto-commit, begin/commit/rollback a transaction, set a savepoint, or call `Statement.cancel()`. `StandaloneConnectionPool` creates a DriverManager connection and does not configure transaction policy. Therefore the plugin itself cannot promise rollback on cancellation; the actual commit boundary depends on the supplied connection/driver state and SQL/database semantics. A timeout or lost response after an update must not be reported as a proven rollback.
4. **SMTP send has an ambiguous acknowledgement boundary.** `MailPlugin` invokes synchronous `email.send()` and returns the message ID only on success. It has no idempotency key, durable send-operation record, or reconciliation mechanism. If the remote SMTP server accepts a message but the acknowledgement is lost or the caller is interrupted before observing success, retrying may duplicate delivery. The source does not prove whether a particular interrupted send was accepted.
5. **HTTP POST has no idempotency/reconciliation contract.** `HttpServiceImpl.post()` builds a POST and calls the synchronous shared-client execution path; response content is buffered before the method returns. The request has no operation-scoped cancellation/abort handle or idempotency-key policy in the inspected service. If a connection fails after the server processes the request but before the response reaches the client, the outcome is indeterminate; blindly retrying may duplicate a non-idempotent operation. GET/POST method support is explicit in `request()`; no claim is made here about remote server semantics.
6. **Browser JavaScript can initiate effects outside the host's direct resource model.** `WebBrowserPlugin.handleExecuteScript()` executes arbitrary page JavaScript on the current HtmlUnit page and waits for background JavaScript. The host currently has no operation record for individual network requests or script-triggered mutations. Closing the WebClient is not evidence that already-issued remote effects were rolled back.

#### Required effect model for the polymorphic SPI

Execution state and effect state must be separate dimensions. A provider-neutral result should carry at least:

- execution: `RUNNING`, `STOP_REQUESTED`, `QUIESCENT`, `UNRESPONSIVE`;
- effect disposition: `NOT_STARTED`, `COMMITTED`, `ABORTED`, `INDETERMINATE`, or `PARTIALLY_COMMITTED` where the provider can prove partial progress;
- operation identity and correlation/idempotency key where the external system supports one;
- last confirmed side-effect checkpoint, external acknowledgement and reconciliation capability;
- retry policy: safe, unsafe, requires idempotency key, or requires manual reconciliation;
- compensation action, if the provider explicitly supports one (compensation is not equivalent to rollback).

The runtime must not collapse `INDETERMINATE` into `CANCELLED` or `FAILED`, and must not automatically retry a non-idempotent operation with an unknown commit result. KILL stops host execution/resources where possible; it does not reverse a database commit, retract an accepted email, undo an HTTP mutation, or delete a remote FTP object unless an explicit compensating operation is defined and itself confirmed.

#### Conformance requirements

- File: inject cancellation before open, after truncation, during write and after close; verify atomic staging/commit policy or report partial/indeterminate state. Test append and replace separately.
- FTP: fake server drops control/data channel before transfer, mid-transfer, after final data close and before/after final reply; distinguish server-side temporary upload from final rename and reconcile remote state.
- JDBC: fake driver blocks before execution, during execution and after update but before acknowledgement; test auto-commit true/false, explicit transaction, driver cancel support, commit/rollback result and unknown commit outcome.
- SMTP: fake server drops connection before DATA, during DATA, after message terminator and before final 250 response; retries must not silently duplicate.
- HTTP: fake server records POST then drops response; verify idempotency key/reconciliation and ensure runtime does not blindly retry.
- Browser: script starts an external mutation then execution is cancelled; report the remote effect separately from local WebClient closure.
- For every provider, assert the terminal execution state, effect disposition, resource quiescence, lease release and retry recommendation independently.

**Evidence / coverage boundary:** source inspection only, using the exact blobs below. No fake server/driver, live network call, cancellation experiment, test suite or build was run. Statements about uncertain remote outcomes describe the absence of a client-side proof/reconciliation path and common request/acknowledgement boundaries; they do not assert a particular remote server's behavior.

**Exact source blobs at `main` snapshot `1729bdf4620a66686d02605083e27e27d1ab3944`:**

- `FilePlugin.java`: `8d78e797975ab236a47420d18f28600beaae04bf`.
- `FtpPutPlugin.java`: `34061eb4fbc3a70f95d8d90a3a2faef611659f82`.
- `DatabasePlugin.java`: `b3934d753f17f133bd582b50206db701af843728`.
- `StandaloneConnectionPool.java`: `ab4a92fb63599e88a55821fe141bf5aa50327008`.
- `MailPlugin.java`: `083297481e7e218916a77f0dea2308c63bbac7e7`.
- `HttpPlugin.java`: `c6045d6c7638f3aa86c510240814633597b108ad`.
- `HttpServiceImpl.java`: `9e3dab1a40250f4d059ea812c74012d994873f0d`.
- `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`.


### 15.59 Deadline propagation and blocking-operation budget audit (2026-11-23)

The side-effect audit is complemented by a check of whether the host can enforce one monotonic GRACE/KILL deadline across blocking provider operations. The inspected adapters do not currently accept or consume a shared execution deadline. Some have local timeouts; those are not equivalent to the host's remaining budget.

| Provider path | Explicit timeout/budget visible in inspected code | Consequence for a shared stop deadline |
|---|---|---|
| HTTP (HttpServiceImpl) | RequestConfig sets redirects only; no connect, connection-request or response timeout is set here. | The host cannot derive or enforce a request deadline from this service API; library defaults must not be mistaken for the runtime's monotonic deadline. |
| FTP (FtpClientFactory, FTP operations) | Factory only sets PASV response address policy. Plugin calls connect, storeFile, retrieveFile/listFiles without setting connect/data/control timeouts in the inspected adapter. | A GRACE budget cannot be propagated to the synchronous transfer through a provider-neutral handle; disconnect is currently in finally, after the operation returns. |
| JDBC (DatabasePlugin) | No Statement.setQueryTimeout, Statement.cancel, or deadline argument in the inspected execution path. | Driver execution and result iteration can outlive the host's requested budget; interruption alone is not a portable JDBC cancellation proof. |
| SMTP (MailPlugin) | Plugin sets host/port and message fields, then calls email.send(); no connection/read/write timeout is configured in this plugin path. | SMTP connect/send may consume time outside the host's remaining GRACE budget; no host-visible SMTP operation handle is retained. |
| WebBrowser (WebBrowserPlugin) | HtmlUnit WebClient sets a fixed 30,000 ms request timeout. Separately, page/script paths call waitForBackgroundJavaScript(waitTime) and then Thread.sleep(waitTime). | Fixed per-request timeout is not derived from remaining session budget; sequential waits can exceed it, and interruption behavior differs between load and execute-script. |
| File (FilePlugin) | No operation deadline or cancellation-aware stream wrapper in the inspected write path. | Large/slow filesystem writes have no host-visible bounded cancellation point; partial-write semantics remain as in 15.58. |

The table records explicit configuration in the inspected adapter code, not undocumented defaults of Apache HttpClient, Commons Net, JDBC drivers, Commons Email or HtmlUnit. Those defaults vary by library/version/driver and must be measured or explicitly configured before a product guarantee is made.

#### Required deadline model

- Runtime creates one monotonic deadline at admission; children, nested providers, async descendants and resource handles receive the same deadline (or a stricter child deadline), never a fresh full timeout.
- Every blocking operation declares a timeout/abort capability and applies the minimum of its configured cap and remaining host budget.
- Deadline expiry first closes admission and requests cooperative cancellation; after grace budget, KILL escalation uses registered abort handles. It does not claim a hard stop for an in-process operation that ignores cancellation.
- Time spent in cleanup is part of a separately defined bounded cleanup budget; cleanup cannot reset the operation deadline or block runtime shutdown indefinitely.
- Provider result reports whether the operation observed deadline expiry, whether the worker became quiescent, and whether external effect disposition is known or indeterminate (15.58).
- Test with an injected monotonic clock and deterministic fake endpoints/drivers; do not use wall-clock sleeps as the primary assertion. Verify nested operations consume one shared budget, cancellation begins at deadline, escalation occurs once, and shutdown reports unresponsive workers instead of waiting forever.

**Evidence / coverage boundary:** source-only configuration review; no library-default assumptions, live network calls, fake servers, tests or builds were run.

**Exact source blobs at main snapshot 1729bdf4620a66686d02605083e27e27d1ab3944:**

- HttpServiceImpl.java: 9e3dab1a40250f4d059ea812c74012d994873f0d.
- FtpClientFactory.java: acfb17feb4361527bf86db21642f77a96c2ddece.
- DatabasePlugin.java: b3934d753f17f133bd582b50206db701af843728.
- MailPlugin.java: 083297481e7e218916a77f0dea2308c63bbac7e7.
- WebBrowserPlugin.java: be7675390275b16ed63e5515a11969585acd2de6.
- FilePlugin.java: 8d78e797975ab236a47420d18f28600beaae04bf.


### 15.60 Existing plugin metadata cannot declare execution/effect capabilities (2026-11-23)

The preceding sections define requirements for polymorphic cancellation, deadlines, resource ownership and side-effect disposition. This pass verifies whether the current public plugin contracts can declare those properties. They cannot in the inspected API.

- `PluginMetadata` exposes identity/presentation fields, dependencies, configuration schema, XML namespace/element mapping, Core classification and inferred/declared semantic node roles. It has no fields for blocking behavior, cancellation mode, deadline support, resource ownership, side-effect class, idempotency/reconciliation, process-isolation requirement, concurrency/reentrancy or cleanup budget.
- `PluginConfigurationSchema` describes configuration attributes, parent/child grammar, body allowance, schema version and validation. Its FILE_PATH/URL attribute types are configuration types, not capability declarations or enforceable sandbox permissions.
- `@CorePlugin` carries namespace, element name, description, version, palette category/visibility and system-component flags. It is registration/editor metadata, not an execution-safety contract.
- `Plugin.execute(DynamicScopeContext)` declares that InterruptedException may be thrown, but accepts no immutable execution handle, cancellation token, monotonic deadline, operation/resource registrar or effect journal. The same interface exposes mutable parent-plugin/running-level state and instance cleanup, which do not describe per-invocation capability.
- The source therefore cannot support preflight policy decisions such as “this provider is safe for in-process GRACE”, “this operation can be aborted by handle”, “this effect is retry-safe”, or “this provider requires process isolation” from its descriptor alone. Runtime discovery currently has to infer behavior from implementation code or rely on undocumented convention.

#### Required descriptor and runtime capability split

Do not overload PluginConfigurationSchema or presentation annotations with operational policy. The target descriptor should separate:

1. **Static provider capabilities** — declared at discovery time: execution model (sync/async), concurrency/reentrancy, cooperative cancellation support, bounded/unbounded blocking operations, resource types, side-effect categories, idempotency/reconciliation support, cleanup behavior, and whether hard isolation is mandatory.
2. **Per-operation capabilities** — declared when an invocation is admitted: actual deadline/remaining budget, registered abort/close handles, effect identity, idempotency key, commit/acknowledgement checkpoints, and compensation/reconciliation action.
3. **Host-verified facts** — conformance evidence and runtime observations. A provider declaration is a claim, not proof. The host must validate claims against conformance tests and downgrade/deny capabilities that are not verified.
4. **Trust boundary** — metadata/permissions do not sandbox Java code. Any provider that is untrusted or requires a hard KILL guarantee must execute in a separately killable process with OS-level resource/security controls.

A provider may declare that an operation is non-cancellable in-process; the host can then reject it under a strict deadline policy, isolate it, or expose an explicit weaker guarantee. It must not silently promise GRACE/KILL compliance.

#### Required conformance

- Descriptor schema rejects unknown capability values and unsupported combinations; capability versioning is independent from XML configuration schema version.
- Provider claims are compared against runtime-observed behavior: interrupt propagation, abort-handle completion, deadline compliance, effect disposition, cleanup idempotency and concurrent invocation safety.
- Discovery/preflight can reject a provider before activation when required host capabilities or isolation are unavailable.
- Runtime reports declared, verified and observed capability separately; refresh to a new generation cannot retroactively change capabilities of already-pinned sessions.
- Tests include lying/misdeclared provider capability, missing abort handle, false idempotency claim, blocking uninterruptible operation, and two concurrent calls against a provider declared non-reentrant.

**Evidence / coverage boundary:** interface/annotation source inspection only. No implementation changes, tests or builds were performed.

**Exact source blobs at main snapshot 1729bdf4620a66686d02605083e27e27d1ab3944:**

- PluginMetadata.java: 0d31ed2c958bd03d429af8df6be94f1decfcbfdf.
- PluginConfigurationSchema.java: c649a8a2d58c496bf913a8a16ac989cab81a6e98.
- CorePlugin.java: 073f25677e8a647a4a9bbd6de7ec456df521ae40.
- Plugin.java: 29772a9b37c8fead735f4a5e0bd47e5b90b4367e.


### 15.61 Conservative compatibility profile for legacy Plugin implementations (2026-11-23)

Sections 15.54–15.60 establish the target execution handle, capability declaration and effect/deadline model. The existing Plugin API cannot satisfy that contract without an adapter because legacy implementations receive only DynamicScopeContext and return Variable (or throw InterruptedException). They do not receive a host-owned invocation handle, deadline, resource registrar or effect journal.

To preserve XML/runtime compatibility without inventing guarantees, every legacy Plugin implementation must enter the new runtime through an explicit **LegacyPluginAdapter** with a conservative, host-assigned capability profile:

- execution model: in-process, synchronous from the legacy interface's perspective; nested behavior may still block or initiate library-managed asynchronous work;
- cancellation: interruption may be attempted, but cooperative cancellation is unverified unless the exact implementation and its dependency paths pass conformance;
- hard KILL: unsupported in-process; a non-quiescent legacy call is UNRESPONSIVE/QUARANTINED, never reported as terminated;
- deadline: host can observe its own monotonic deadline and request interruption, but cannot promise that a legacy call will return by it;
- resource ownership: unknown unless the adapter can register concrete resources at the actual call site; session EventBus cleanup is not a substitute for active-operation leases;
- side effects: UNKNOWN/INDETERMINATE by default for external or mutating operations unless a provider-specific adapter proves otherwise;
- retry: unsafe by default when effect disposition is unknown; no automatic replay after timeout/interruption;
- concurrency/reentrancy: unspecified by default; serialize per plugin instance unless an explicit verified capability permits concurrent use;
- cleanup: legacy cleanup is best-effort provider cleanup, not proof of invocation quiescence or generation retirement.

The adapter must not infer capabilities from plugin name, category, XML schema, annotations, dependency list or a plugin's declaration that it throws InterruptedException. Capabilities may be promoted only by versioned provider metadata plus host conformance evidence tied to the provider artifact/version and runtime compatibility range.

#### Migration levels

- **L0 — Legacy-compatible:** behavior remains available under compatibility policy; no strict cancellation/deadline/retry guarantee is claimed.
- **L1 — Cooperative:** provider receives the host execution context, propagates cancellation and deadline through descendants, and passes deterministic GRACE/quiescence tests.
- **L2 — Resource-aware:** provider additionally registers operation-scoped abort/close handles and reports effect disposition/reconciliation; passes resource-race and side-effect conformance.
- **L3 — Isolated:** provider executes in a separately managed process with verified worker exit and OS-level resource controls; only this level can support a hard KILL guarantee for arbitrary non-cooperative code.

These are conformance/migration levels, not security trust labels. A provider may be L3 for hard termination while still having INDETERMINATE external effects; process death does not undo remote commits. Conversely, a provider can be L2 and fully cooperative without requiring process isolation if hard KILL is not promised.

#### Host policy

- Existing configurations may continue through L0 only when the selected runtime policy permits the weaker guarantee and surfaces it to the caller.
- A strict GRACE policy requires L1 or higher; resource-sensitive/side-effect-sensitive operations require L2 or explicit provider-specific policy.
- A hard KILL requirement requires L3. If process isolation is unavailable, admission must fail or downgrade only with explicit caller acceptance; never silently pretend thread interruption is a kill.
- Refresh creates a new provider capability record for the new generation. Sessions pinned to the old generation retain the old profile and adapter until their execution leases drain.
- Capability promotion is versioned and auditable; no automatic promotion based solely on a passing generic unit test or metadata declaration.

**Evidence / coverage boundary:** this is a conservative compatibility design derived from the current Plugin API and prior source findings. No implementation, migration or test execution was performed. The profile is a proposal for review, not a claim that an adapter already exists.

**Exact source blobs at main snapshot 1729bdf4620a66686d02605083e27e27d1ab3944:**

- Plugin.java: 29772a9b37c8fead735f4a5e0bd47e5b90b4367e.
- PluginMetadata.java: 0d31ed2c958bd03d429af8df6be94f1decfcbfdf.
- PluginConfigurationSchema.java: c649a8a2d58c496bf913a8a16ac989cab81a6e98.
- AbstractPlugin.java: be3298304840e4a585b4fb148c2e62cf471ee8f7.
- DefaultPluginFactory.java: 91c8084f671027954375ff3cab784691f8ae26e5.


### 15.62 Consolidated normative execution and provider SPI contract (2026-11-23)

This section consolidates the source-backed findings and design constraints in 15.45–15.61 into one candidate contract for review across #346–#350. It is a design baseline, not an approved public API and not an implementation. The existing XML/runtime contract remains the compatibility surface until maintainers approve the SPI and migration plan.

#### A. Identity and immutable descriptor

Every provider is identified by a stable, globally unique `providerId` and every plugin/capability by a stable `pluginId`. Class names, package names, XML local names and display labels are never identity.

A versioned, data-only descriptor must carry:

- descriptor schema version and SPI/API version range, plus supported Core compatibility range;
- provider ID, artifact coordinates/content digest, provider version and provenance;
- plugin IDs, XML QName mappings, explicit aliases and deterministic collision behavior;
- semantic capability IDs/roles, input/output contracts, composition and parent/child constraints;
- configuration schema and validation version (separate from capability schema version);
- dependency/version/conflict declarations and optional capability relationships;
- requested permissions/resource classes, execution model, concurrency/reentrancy and lifecycle scope;
- declared cancellation/deadline behavior, abortable resource kinds, effect categories, idempotency/reconciliation claims, cleanup budget and process-isolation requirement.

Descriptor parsing and semantic projection must not load or instantiate provider implementation classes. Unknown mandatory fields/versions, incompatible API ranges, duplicate IDs/QNames/aliases, dependency cycles and unsupported capability combinations fail closed with actionable diagnostics. Optional unknown extension fields may be ignored only where the descriptor version explicitly permits forward-compatible extension. XML aliases are explicit data, never inferred from class names.

#### B. Provider construction and invocation boundary

The new SPI must not expose `DynamicScopeContext`, Core `Variable`, `ScraperSession`, Guice injectors, Guava EventBus or mutable legacy plugin parent/running-level state as its stable public contract.

Conceptual host-owned interfaces (names illustrative; exact Java types/package/API version remain open):

- `PluginProvider`: creates provider/plugin instances using an immutable activation context and returns a provider lifecycle handle.
- `PluginInstance`: invokes one capability with immutable input/configuration and an `ExecutionContext`; has explicit instance/session close semantics.
- `ExecutionContext`: immutable execution/runtime identity, cancellation token, monotonic deadline/remaining budget, generation identity, parent/child relationship, host service capabilities, resource registrar and effect-operation recorder.
- `ResourceRegistration`: registers an operation-scoped abort/close action and ownership scope; registration returns an idempotent handle that is released only after the operation is quiescent.
- `EffectOperation`: records operation ID, external system, idempotency/correlation key, commit/acknowledgement checkpoints, disposition and reconciliation/compensation capability.
- `ProviderGeneration`: immutable descriptor/capability snapshot plus provider/classloader ownership and reference-counted execution leases.

These are logical boundaries, not a mandate to expose six public Java interfaces. API minimization and compatibility review remain part of #346. The essential requirement is that every provider invocation receives host-controlled context and cannot mutate global registry state.

#### C. Admission, execution and terminal-state model

The host admits an invocation only after descriptor compatibility, provider enablement, authorization, permission/resource policy, dependency resolution and generation selection succeed. Admission atomically pins one immutable generation and creates one execution handle before provider code starts.

Minimum execution lifecycle:

`CREATED -> ADMITTED -> RUNNING -> STOP_REQUESTED_GRACE -> TERMINATING -> QUIESCENT`

KILL escalation:

`STOP_REQUESTED_GRACE -> KILL_REQUESTED -> TERMINATING -> QUIESCENT`

If in-process work does not unwind:

`KILL_REQUESTED -> UNRESPONSIVE/QUARANTINED`

Terminal execution outcomes are distinct from business/effect outcomes. At minimum: `SUCCEEDED`, `FAILED`, `CANCELLED`, `TERMINATED`, `UNRESPONSIVE`. A requested stop is not terminal quiescence. Session status, event publication or completion-future signalling must not claim that worker/descendant execution has stopped.

GRACE closes new top-level and nested admission, propagates the same cancellation token/deadline to descendants and async work, permits bounded cooperative cleanup, and waits for quiescence. KILL escalates after the monotonic deadline or explicit request, invokes registered abort handles and interrupts where supported. In-process non-cooperation yields `UNRESPONSIVE/QUARANTINED`; no unsafe thread termination. A hard KILL guarantee requires a separately managed worker process and verified process exit.

#### D. Generation and resource ownership

Every invocation and descendant pins its originating provider generation. Refresh atomically publishes G(n+1); new admissions use it, while existing executions remain on G(n). Removal closes admission to the retiring generation and applies explicit drain/cancel policy. Provider disposal and classloader closure occur exactly once only after all invocation/descendant/resource leases are released and execution is quiescent.

Per-invocation cleanup, plugin-instance close, provider-generation disposal and runtime shutdown are four separate lifecycle scopes. Closing a session-owned resource on a cancellation event before active operations unwind is forbidden. A cleanup callback is not proof of quiescence. Runtime shutdown must report remaining unresponsive executions and retain any resources/classloaders they still require rather than falsely report a clean drain.

#### E. Deadline, resources and external effects

One monotonic deadline is created at admission and propagated to all nested/async operations. Each blocking adapter applies the minimum of its configured cap and remaining host budget, or declares that it cannot enforce the budget. Cleanup has a separate bounded budget and cannot reset the execution deadline.

Provider capability claims are separated into `DECLARED`, `HOST_VERIFIED` and `OBSERVED`; metadata alone is not proof. A provider operation declares whether it is cooperative, externally abortable, timeout-bounded or non-interruptible. Abort/close handles are operation-scoped and must not close resources still used by another live operation.

Execution state and external-effect disposition are independent. Effect values: `NOT_STARTED`, `COMMITTED`, `ABORTED`, `INDETERMINATE`, and `PARTIALLY_COMMITTED` only where partial progress is established. Unknown acknowledgement is not proof of rollback. Retry defaults to unsafe for indeterminate non-idempotent effects. Compensation is a new operation, not rollback. Process death does not reverse a remote database, SMTP, HTTP or FTP effect.

#### F. Legacy adapter and migration

All current `Plugin` implementations enter the new execution path through a compatibility adapter. Until proven otherwise they are L0: in-process, cancellation-unverified, no hard KILL, unknown resource/effect disposition, unsafe retry and unspecified concurrency (serialize per instance). Existing XML behavior remains available only under a runtime policy that explicitly accepts this weaker profile.

- L0 legacy-compatible: no strict cancellation/deadline/effect guarantee.
- L1 cooperative: receives host context and passes nested cancellation, deadline and quiescence conformance.
- L2 resource-aware: additionally registers abort/close handles and reports effect disposition/reconciliation.
- L3 isolated: separate worker process with verified exit and OS-level resource controls; required for hard KILL of non-cooperative code.

Promotion is per provider artifact/version and runtime compatibility range, backed by conformance evidence. No promotion by plugin name, annotation, schema, dependency list or generic unit test. Capability changes apply only to new generations; already-pinned sessions retain the old profile.

#### G. Minimum release-gate evidence mapped to child issues

| Gate | Required evidence | Owning issue |
|---|---|---|
| Descriptor/SPI | Versioned schema + minimal Java contract; compatibility, identity, collision, dependency and descriptor-only projection tests; maintainer approval before implementation | #346 |
| Discovery/registry | Managed artifact source; add/update/remove/re-add without restart; deterministic atomic generations; last-known-good rollback; old-generation pinning and classloader retirement | #347 |
| Semantic/runtime parity | Exact ID/QName/alias/capability equality for one generation across metadata, schema, inspection, validation and dispatch; stale snapshot rejection | #348 |
| Policy/lifecycle | Denied-by-default authorization; declared vs verified capabilities; resource/effect handling; bounded GRACE/KILL; two-runtime isolation; no false quiescence | #349 |
| Conformance/release | Out-of-tree fixture against exact Core artifact; positive/negative matrix; cancellation/effect/deadline/lease tests; artifact hashes, CI run IDs and maintainer sign-off | #350 |

No implementation begins until #93 inventory/Core boundary and #94 architecture dependencies are reviewed, the #346 contract is accepted by maintainers, and the required design decisions are recorded. A green CI run on documentation does not constitute SPI approval or release GO.

#### H. Explicit open decisions before API freeze

1. Exact descriptor serialization/schema format and canonicalization/signature envelope.
2. Stable provider/plugin ID namespace and artifact provenance/trust roots.
3. Minimal Java SPI type count, binary compatibility policy and Java 17 API baseline.
4. Provider, instance, session and invocation scopes; concurrency and reentrancy defaults.
5. Process worker protocol, startup/health/IPC, OS resource limits and failure recovery for L3.
6. Host permission vocabulary and policy UX for filesystem paths, network endpoints, DB, browser, scripts, secrets, time and memory.
7. Whether any legacy provider is promoted above L0 in the first migration wave, based on evidence rather than assumption.
8. Which XML compatibility aliases and the `set-var` behavior are explicitly frozen by #93.

**Status:** candidate consolidated contract for review only. No maintainer approval, public API freeze, production implementation, test run, package split or release authorization is implied.


## 15.63 Consolidated candidate classification and destination proposal for #93 (2026-11-23)

This section converts the source census into a bounded product-boundary proposal. It is deliberately conservative: **it classifies current responsibilities and proposes destinations, but does not approve a reduced Core distribution, delete XML names, move packages or authorize implementation.** The current 44-name XML compatibility surface remains intact until a maintainer-approved migration decision exists.

### A. The 44 current Core XML mappings

The destination distinction is between (1) Core execution semantics, (2) Core-owned capability providers that require explicit host services/policy, and (3) declarative XML grammar nodes. A declarative node retains XML identity/schema/validation but does not automatically become an independently activatable provider.

| Candidate group | XML names | Proposed destination and rationale |
|---|---|---|
| Execution semantics and value/context primitives | `call`, `constant`, `def`, `empty`, `exit`, `function`, `get`, `if`, `loop`, `list`, `value-of`, `set-var`, `text`, `try`, `while` | Retain as Core-owned execution/value semantics for the compatibility baseline. They define control flow, function behavior, values and execution-context behavior rather than external integrations. Keep `set-var` as an explicit legacy compatibility identity; do not silently alias/remove it. |
| Core-owned capability providers with host-service boundaries | `file`, `html-to-xml`, `http`, `include`, `json-to-xml`, `regexp`, `script`, `log`, `sleep`, `template`, `xml`, `tokenize`, `xml-to-json`, `xpath`, `xquery`, `xslt` | Keep their XML contracts available in the current Core compatibility profile, but model them as explicit capabilities behind host-owned services and budgets. Filesystem/network/script execution require policy enforcement at the I/O/engine boundary; parsing/query/transformation require CPU, memory, input/output and external-resource limits. Their presence in current Core is not proof that every embedding host must enable them by default. |
| Declarative or parent-consumed XML grammar | `call-param`, `case`, `catch`, `else`, `http-header`, `http-param`, `regexp-pattern`, `regexp-result`, `regexp-source`, `return`, `stylesheet`, `xq-expression`, `xq-param` | Retain as XML grammar/schema/semantic identities, not independent activation units by default. Their execution/permissions inherit the owning parent operation; nested executable values still undergo their own admission and policy checks. |
| Dual-use identity | `xml` | Preserve the dual role already demonstrated by the runtime matrix: standalone XML value/body behavior and parent-consumed XSLT content. Descriptor projection must express the two roles without creating duplicate plugin identity. |

The 13 declarative/parent-consumed identities include the branch and return grammar nodes even though their parent-level runtime behavior is covered by integration tests. The groups intentionally overlap only where a mapping has more than one documented role (`xml`). The execution-semantics group retains `function` because its definition establishes callable behavior used by `call`; its body is not an independently activated external provider. The source-backed per-name classification remains the 15.32 table; the direct XML test evidence remains the 44-row `CORE_PLUGIN_RUNTIME_EVIDENCE.md` matrix. Do not infer that every mapping has an independent top-level dispatch test: parent-consumed nodes are tested through their parent contract.

### B. Ten optional-module entry points

| Current module | Implementations | Proposed destination |
|---|---|---|
| Database | `DatabasePlugin`, `ConnectionPlugin` | Keep in the optional Database integration artifact. Core must not acquire JDBC driver, connection-pool or database dependencies. Require explicit driver/URL/secret/query/transaction/cancellation policy. |
| FTP | `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin`, `FtpListPlugin` | Keep in the optional FTP integration artifact. Preserve the four XML identities only through an explicitly enabled integration; fix/define transport security, destination, credential, parent/child connection and transfer-budget contracts before strict-profile activation. |
| Mail | `MailPlugin` | Keep in the optional Mail integration artifact. SMTP send is an irreversible external effect; require explicit authorization, transport-security/secret policy, redaction and indeterminate-commit handling. |
| WebBrowser | `WebBrowserPlugin` | Keep in the optional browser integration artifact. Browser navigation and JavaScript are high-risk network/code capabilities; require destination/redirect/SSRF policy, explicit script authorization and bounded lifecycle/resource controls. |
| ZIP | `ZipPlugin`, `ZipEntryPlugin` | Keep as an optional archive capability artifact. Preserve scoped stream ownership; add archive-size/entry-count and entry-name policy before strict activation. |

No optional integration should be pulled into the mandatory Core dependency graph. The existing module/POM direction is compatible with this proposal at the inspected direct-dependency level; transitive and runtime coupling still require the approved out-of-tree proof. ZIP has the strongest current XML/runtime integration evidence; the other optional modules have material gaps documented in 15.45–15.58. This is not a claim that their tests were executed in this audit.

### C. Three additional Core-package classes outside the 44 mappings

| Class | Current evidence | Proposed destination |
|---|---|---|
| `ConfigPlugin` | Root configuration/bootstrap protocol; not one of the 44 child mappings | Keep as internal Core configuration protocol unless a separate public XML capability is explicitly specified. Do not expose as an ordinary plugin merely because it is plugin-named. |
| `BodyProcessorPlugin` | Historical/internal metadata artifact; current body execution is through `AbstractPlugin.executeBody()` and `DefaultPluginBodyExecutor`; no current registrar mapping | Exclude from the default provider set. Retain only as legacy/internal code until a separate cleanup decision is made; do not count its metadata/fixture as proof of an active runtime capability. |
| `VarPlugin` | Deprecated and not registered by the current registrar; `DefPlugin` is the current variable-definition path | Exclude from the default provider set. Do not restore it based on scanner visibility. This does not affect the separate registered `set-var` compatibility mapping. |

### D. Test and migration evidence map

| Candidate area | Existing evidence | Remaining acceptance work |
|---|---|---|
| 44 Core XML names | `CORE_PLUGIN_RUNTIME_EVIDENCE.md` maps every registered name to source/runtime evidence and labels direct vs parent-consumed behavior. The registrar contract test protects the exact name/class mapping. | Re-run the compatibility corpus on the eventual exact candidate; add descriptor/schema/semantic parity and enabled/disabled policy tests. Do not treat the matrix as a security or lifecycle suite. |
| Core execution and value semantics | Core control-flow, function/call, value and text-processing XML integration suites are identified in the runtime matrix. | Add shared generation/deadline/cancellation, recursion/iteration/output-budget and stale-validation tests under #347–#350. |
| Host-effect Core capabilities | File, HTTP, include, script, logging, sleep and transformation call sites/policy gaps are recorded in 15.6–15.60. | Add policy-boundary tests with recording/fake host services, bounded input/output and cancellation; verify actual enforcement rather than descriptor claims. |
| Database/FTP/Mail/WebBrowser/ZIP | Optional-module source, dependencies, lifecycle and existing test strength are mapped in 15.45–15.58 and 15.6.1. | Build per-provider XML conformance and deterministic fake-service tests; add cancellation, deadline, external-effect and resource-close assertions. Keep live network credentials/services out of ordinary CI. |
| All provider types | Current source audit identifies legacy construction, mutable/global registries, classloader cache and session lifecycle gaps. | Independent out-of-tree Maven fixture plus add/update/remove/re-add, last-known-good rollback, two-runtime isolation, generation pinning, resource drain and policy-denial tests. |

### E. Proposed decision and guardrails for maintainer review

1. Accept the classification above as a **working destination proposal**, not as permission to move code.
2. Keep all 44 current XML names and their existing compatibility behavior in the current compatibility profile, including `set-var`.
3. Keep Database, FTP, Mail, WebBrowser and ZIP outside the mandatory Core dependency graph.
4. Treat the 13 declarative/parent-consumed identities as descriptor grammar/schema entries rather than automatically activatable provider instances; preserve their XML names and parent semantics.
5. Treat ConfigPlugin as internal root protocol, BodyProcessorPlugin as historical/internal, and VarPlugin as deprecated/unregistered.
6. Defer any decision to remove/split current Core capabilities until maintainers explicitly decide which host-service capabilities are mandatory in the minimal Core distribution and which are separately selectable first-party providers.
7. Do not begin module moves, package deletion, POM splitting, SPI implementation or release work from this proposal alone. #93 remains open until the maintainer records the product-boundary decision.

**Decision status: PROPOSAL ONLY — maintainer acceptance outstanding.** Source inventory is complete; this classification is now explicit for review. No runtime/POM/test/workflow changes were made and no tests/builds were run for this documentation update.


## 15.64 Proposed safe defaults for the remaining #346 SPI decisions (2026-11-23)

The following defaults are recommended to make maintainer review concrete. They are intentionally conservative and remain **proposals**. They do not freeze the API, create a compatibility promise, or authorize implementation.

| Open decision | Recommended default | Reason / rejection condition |
|---|---|---|
| Descriptor wire format | UTF-8 JSON descriptor with an explicit schema version, canonical field/array ordering for identity hashing, and a separately versioned JSON Schema. Keep runtime XML configuration and plugin descriptor formats distinct. | Easy to inspect and parse without class loading. Reject descriptors with unknown major schema versions or unknown mandatory fields; permit optional extensions only under an explicit forward-compatibility rule. |
| Provider/plugin identity | Stable reverse-DNS IDs owned by the provider namespace; separate `providerId` from `pluginId`. XML QName and aliases are bindings, never IDs. | Avoid collisions and renames caused by Java package/class changes. Reject duplicate IDs and QName/alias collisions before activation. |
| Artifact identity | Record artifact coordinates where available plus immutable content digest; treat coordinates, digest, publisher identity and trust decision as separate fields. | A digest identifies bytes but does not establish who published them or whether the host trusts them. |
| Trust/provenance | Host-configured provider allowlist/policy is authoritative. Signature/attestation may provide provenance evidence but must not imply automatic authorization. Unknown publisher or changed digest requires a new decision. | Discovery must not become trust or execution. Do not introduce an implicit trust-on-first-use path. |
| SPI compatibility | Explicit descriptor version, SPI major version and Core compatibility range. Fail closed on unsupported major versions; minor/patch compatibility only where the contract guarantees it. | Avoid reflective best-effort adaptation of incompatible providers. Do not equate Maven version ranges with binary compatibility proof. |
| Java API shape | Start with the smallest public boundary: provider factory/entry point, invocation instance, immutable execution context and host capability services. Keep resource/effect handles as focused value contracts; do not publish six interfaces merely because the conceptual model has six boxes. | Reduce long-term binary surface while retaining host-owned deadlines, cancellation, resource ownership and effect reporting. Exact names/packages wait for a compiling out-of-tree fixture. |
| Discovery mechanism | Descriptor-first artifact scan; optional ServiceLoader/provider binding only inside an already managed artifact. Discovery and descriptor projection must not initialize provider classes or invoke constructors. | ServiceLoader alone cannot own update/removal, trust, artifact staging, rollback or generation retirement. |
| Permission vocabulary | Stable, namespaced capability identifiers with explicit operation scope (e.g. filesystem read/write, outbound network, database, browser navigation, script engine, secret access). Unknown permissions deny by default. | Broad booleans such as `network=true` are insufficient for host policy. Enforce at the actual host service/engine boundary. |
| Provider concurrency | Default to serialized calls per provider instance; parallelism/reentrancy is opt-in and requires host-verified conformance. | Existing legacy providers have unspecified shared state and concurrency behavior. |
| Legacy L0 admission | Compatibility profile may run only explicitly trusted legacy providers under documented weaker guarantees. Strict profile denies L0 providers for untrusted/side-effecting use unless the embedding host explicitly accepts the risk. Never advertise hard KILL, bounded cancellation or safe retry for L0. | Preserves migration without turning old APIs into false security guarantees. L1/L2/L3 promotion is per artifact version and evidence-backed. |
| Isolation model | Trusted providers may run in-process under host policy; untrusted providers require a separate worker process and OS isolation. | A Java ClassLoader is not a sandbox. Do not promise safe termination of non-cooperative in-process code. |
| Registry publication | Immutable candidate generation, full validation before atomic publication, last-known-good retained on failure; each execution pins exactly one generation. | Prevent partial multi-index state, cross-runtime leakage and semantic/runtime time-of-check/time-of-use drift. |
| XML compatibility | Preserve all current registered names and aliases, including `set-var`; declarative nodes remain schema/grammar identities without independent activation unless their source behavior proves otherwise. | Avoid accidental breakage from mapping-count or class-scanner discrepancies. |

### Suggested decision sequence

1. Maintainer accepts/rejects the product-boundary proposal in 15.63 (#93).
2. Maintainer accepts/revises these SPI defaults and the consolidated contract in 15.62 (#346).
3. Only then create a minimal compiling descriptor/SPI prototype and an out-of-tree fixture; do not begin registry/runtime replacement in parallel.
4. Validate the fixture against descriptor-only discovery, compatibility failures, host-policy denial and XML compatibility before authorizing #347–#350 implementation slices.
5. Freeze the public API only after the fixture proves independent compilation and the compatibility policy is documented.

**Decision status: REVIEW INPUT ONLY.** These defaults are recommendations, not maintainer decisions. No public API, descriptor schema, Java interface, module, POM, runtime code or test was created or changed.


## 15.65 P0 acceptance traceability and implementation entry gate (2026-11-23)

This matrix maps the current documentation evidence to the actual acceptance criteria of #93, #346–#350 and parent #94. A documented requirement is not a passed test. “Covered in design” must not be reported as “implemented” or “verified”.

| Gate / issue | Evidence now available | Still missing before acceptance | Current disposition |
|---|---|---|---|
| #93 — inventory and Core boundary | Sections 15.32, 15.45–15.50 and 15.63: exact 44-name registrar inventory, ten optional entry points, three additional Core-package classes, source/POM boundary and proposed destination for every candidate; CORE_PLUGIN_RUNTIME_EVIDENCE.md test map. | Maintainer decision on minimal Core membership; explicit compatibility profile/default enablement; approval of optional integration boundary and set-var; confirmation whether any current Core host-effect capability is optional by default. | **STOP — proposal ready for review, not approved.** |
| #346 — descriptor and SPI | Sections 15.50, 15.60–15.62 and 15.64: required descriptor fields, provider/runtime boundary, lifecycle, compatibility, conservative defaults, trust and permission model. | Maintainer acceptance; actual versioned schema and Java API; serialization/canonicalization and compatibility tests; compiling built-in and external examples; explicit public binary-compatibility policy. | **STOP — design candidate only.** |
| #347 — discovery and registry | Sections 15.51–15.53 and 15.62: current registry/scanner defects; descriptor-first discovery; immutable generations, atomic publication, last-known-good, generation pinning and retirement contract. | Implementation against accepted SPI; managed artifact source/reconciliation; real add/update/remove/re-add; atomic rollback; two-runtime and classloader/resource-drain conformance. | **STOP — no implementation authorized.** |
| #348 — semantic/runtime parity | Sections 15.32, 15.62 and 15.63: executable vs declarative XML roles; one-generation projection principle; exact 44-name compatibility evidence. | Registry-backed Semantic Model/catalog/inspection/validation; snapshot token in inspection and validation; exact set-equality tests across descriptor, semantic, schema, validation and dispatch; external enabled/disabled fixture. | **STOP — contract only.** |
| #349 — policy and lifecycle | Sections 15.6–15.7 and 15.54–15.61: side-effect inventory; host-effect boundaries; GRACE/KILL; cancellation/quiescence; interruption, resource, deadline, effect and legacy-profile constraints. | Actual host service enforcement and default profile; denied-by-default and unknown-permission tests; deterministic cleanup/fault injection; capability conformance evidence; process worker/OS policy if untrusted providers are supported. | **STOP — requirements only.** |
| #350 — conformance and release evidence | Sections 15.45–15.61 plus 15.63: current test-strength map and proposed positive/negative, lifecycle, compatibility, policy, generation and packaging matrix. | Independent out-of-tree Maven fixture against exact candidate artifact; executable live lifecycle matrix; test reports, artifact hashes and CI run IDs; explicit maintainer release sign-off. | **STOP — no fixture or release proof.** |
| #94 — unified parent architecture | PR #352 describes managed sources, immutable generations, semantic/runtime parity, host-owned composition and isolation; MULTI_RUNTIME_ISOLATION_AUDIT.md records concrete static-IoC cross-runtime hazards. | Maintainer architecture review; reconcile approved #93/#346 decisions; implementation and full parent acceptance matrix. | **STOP — Draft design, unmerged.** |
| #88 — release gate | Distribution candidate evidence is recorded elsewhere; plugin platform remains P0 and Jetty Servlet API legal disposition remains open. | All P0 child gates above, release/legal/NOTICE closure, exact release-candidate evidence and explicit sign-off. | **NO-GO.** |

### Mandatory implementation entry sequence

1. Maintainer records a decision on #93: Core compatibility baseline, default built-in set, optional integrations, and the 44 XML names including set-var.
2. Maintainer reviews and accepts/revises #346 descriptor/SPI contract. The JSON/reverse-DNS/host-allowlist/minimal-SPI recommendations in 15.64 are defaults to decide, not hidden approvals.
3. Produce a minimal compiling API/schema prototype plus independent out-of-tree fixture. The fixture must prove descriptor-only discovery and semantic projection before provider implementation loading, then authorized activation and XML execution.
4. Only after that proof, authorize bounded #347–#349 implementation slices; keep #350 as the independent acceptance authority.
5. Run the full compatibility and conformance matrix against one exact candidate commit/artifact. Attach immutable artifact hashes, reports and workflow IDs to #94/#88.
6. Release remains blocked until maintainers explicitly sign off every P0 gate and the separate legal/distribution requirements.

### Scope and evidence guardrail

The audit PR is documentation and design evidence. It does not satisfy runtime acceptance by describing the desired behavior. Existing source/test references are not test results; tests inspected but not executed remain unverified. No code, POM, test, workflow, module, API or release metadata changes are authorized by this traceability matrix.

**Current decision: STOP before implementation; continue source/design review and maintainer decision preparation only.**

## 15.66 Minimal Core membership — explicit decision alternatives for #93 (2026-11-23)

Section 15.63 classifies every current XML mapping, but it intentionally leaves one product decision open: whether “Core” means the smallest execution kernel, or the compatibility distribution that ships the established XML surface. These are different boundaries and should not be collapsed into one package/module decision.

### Decision A — compatibility-first Core (recommended immediate baseline)

- **Logical execution kernel:** retain the 15 execution/value/context identities from 15.63.
- **XML grammar:** retain the 13 declarative/parent-consumed identities as Core grammar/schema semantics; they are not independently activated providers.
- **Compatibility capability set:** retain all 16 host-service capability identities in the current Core compatibility distribution for now, with explicit host-service boundaries, budgets and policy enforcement as required by #349.
- **Optional integrations:** keep Database, FTP, Mail, WebBrowser and ZIP outside the mandatory Core dependency graph.
- **Compatibility:** preserve all 44 current XML names, aliases and established behavior, including `set-var`.

This is the lowest-risk decision for the next engineering step: it defines a coherent logical kernel without forcing a premature Maven/package split or breaking existing XML documents. It does **not** claim that every capability is safe or enabled by default. A restrictive host profile may deny effects; compatibility behavior and authorization remain separate.

### Decision B — kernel-only Core distribution

- Ship the 15 execution/value/context identities and 13 grammar/schema identities as the mandatory Core distribution.
- Move the 16 capability providers into separately selectable, first-party capability artifacts, preserving their XML identities through an explicit compatibility bundle/profile.
- Keep the five optional integration modules independent as in 15.63.

This produces a smaller default artifact, but it is a materially larger compatibility and packaging change. Before choosing it, maintainers must decide which capability providers are bundled in the standard compatibility profile, how old XML documents resolve them, and how the current reactor/publication model exposes those artifacts. It must not be implemented as silent removal from the registrar.

### Decision C — selective capability split

Retain only individually approved capability providers in the Core distribution and move the rest into first-party capability artifacts. This may produce a better product boundary than treating all 16 uniformly, but requires a per-capability decision based on host-service requirements, dependency footprint, minimal workflow usefulness, and compatibility evidence. No provider should be classified as “pure” or “safe” merely because it performs parsing or transformation; CPU, memory, external-resource resolution and script-engine behavior still need explicit limits.

### Recommendation and exact maintainer decisions requested

Recommend **Decision A as the immediate compatibility baseline**, while recording the logical execution kernel separately from the compatibility capability set. Defer physical artifact/module extraction until the descriptor/SPI and out-of-tree fixture gates pass.

For #93 closure, maintainers need to record all of the following explicitly:

1. Choose A, B or C (or record a materially different alternative).
2. Approve the 15 execution/value/context identities as the logical execution kernel, or identify exceptions.
3. Approve the 13 declarative identities as Core grammar/schema, not standalone providers, or identify exceptions.
4. Decide whether all 16 capability providers remain in the compatibility distribution initially, and whether any are disabled by default under the host policy profile.
5. Confirm the five optional integration boundaries (Database, FTP, Mail, WebBrowser, ZIP).
6. Confirm preservation of all 44 XML identities and aliases, including `set-var`, during any later packaging transition.

### Consequences for dependent work

- #346 may define descriptors and host-policy semantics against the approved logical roles; it must not assume a packaging split that has not been chosen.
- #347 must support both built-in and managed first-party/external artifacts without hard-coding the current packaging decision into the registry.
- #348 must project grammar and capability availability from one effective registry generation; grammar identity does not imply provider activation.
- #349 must enforce policy at host-service boundaries regardless of whether a capability ships in Core or a separate artifact.
- #350 must test the selected compatibility distribution and Core-only profile separately where both are supported.

**Decision status: PROPOSAL ONLY — no maintainer choice is inferred.** This section clarifies the decision required to close #93; it does not authorize module moves, POM changes, provider removal, SPI implementation or release work. Source inventory and design only; no tests/builds run for this documentation update.


## 15.67 Source-level SPI boundary audit — existing contracts versus target boundary (2026-11-23)

This section records a focused reinspection of the current implementation on `main` at tree/commit baseline `1729bdf4620a66686d02605083e27e27d1ab3944`. It is source inspection only; no tests were run.

### A. Existing public plugin interface is not a viable new SPI

Source: `webharvest-core/src/main/java/org/webharvest/plugin/Plugin.java`, blob `29772a9b37c8fead735f4a5e0bd47e5b90b4367e`.

The existing `Plugin` contract directly requires:
- `execute(DynamicScopeContext)` and returns Core-owned `Variable`;
- Core `PluginConfiguration`, `PluginMetadata`, and legacy exception types;
- mutable parent linkage through `setParentPlugin(Plugin)`;
- a mutable running-level concept;
- legacy initialize/isInitialized/cleanup lifecycle methods.

This is a compatibility contract, not a suitable neutral SPI. A new SPI artifact must not depend on `webharvest-core`, `DynamicScopeContext`, `Variable`, or the old plugin hierarchy. The adapter belongs on the Core/host side and translates between contracts; the new SPI must not import legacy types to make that adapter convenient.

### B. Existing metadata is neither a static manifest nor safe discovery input

Source: `PluginMetadata.java`, blob `0d31ed2c958bd03d429af8df6be94f1decfcbfdf`; `PluginDescriptor.java`, blob `c3d3f8a70f4072895fed6c0133c8caf0bc556e79`; `PluginMetadataCatalog.java`, blob `5ac82d7fb6436b458b29647e4c929878557b1c6e`.

- `PluginMetadata` exposes a Java callback interface, arrays and a `PluginConfigurationSchema` callback, and includes `getClassName()`. Semantic role defaults are inferred by invoking metadata/schema methods and inspecting visual metadata types.
- The current `PluginDescriptor` is a runtime projection object containing `className`, `origin`, display/UI fields, semantic projection, dependency list and generic extensions. Its existence does not establish a versioned wire contract or an implementation-independent descriptor.
- `PluginMetadataCatalog` owns static descriptor/schema state. Its catalog initialization enumerates implementation names, calls `Class.forName(className)`, then `getDeclaredConstructor().newInstance()`, followed by plugin metadata/schema callbacks. Thus catalog generation can load, initialize and instantiate implementation code; it is not descriptor-only discovery.
- The current class lookup uses one-argument `Class.forName`, not an explicit artifact-scoped loader contract. The catalog is static rather than runtime/generation-owned.

Target consequence: descriptor parsing and semantic projection must operate exclusively on validated data values. No provider class, ServiceLoader, constructor, static initializer, plugin metadata callback or implementation-defined schema callback may execute in the discovery/projection phase.

### C. Existing factory and registry APIs expose implementation classes and mutation

Sources: `PluginFactory.java`, blob `f1d2434c5a36d21d9c09bb638f4ebf3e5eaa94f8`; `DefaultPluginFactory.java`, blob `91c8084f671027954375ff3cab784691f8ae26e5`; `PluginRegistry.java`, blob `eceeddca531c98ca4a7f235e04ab28511bfc9281`; `CorePluginRegistry.java`, blob `20f478addbf2bacc481e4e11e0328f06942a2a38`.

- `PluginFactory` creates by `Class<? extends Plugin>`, plugin ID, or XML namespace/element name and returns the legacy `Plugin` object.
- `DefaultPluginFactory` caches `Constructor` values keyed by `Class`. The cache is instance-owned, but retains implementation classes/classloaders for as long as its factory remains reachable.
- `PluginRegistry` exposes register/unregister/clear/refresh and returns plugin implementation instances and classes indirectly through its API.
- `CorePluginRegistry` is a process-wide singleton with mutable mappings and public `clear()`.

Target consequence: new SPI must describe provider entry points as opaque data until activation. The host owns construction, classloader selection, registry publication and retirement. Provider code must not receive mutation authority over registry generations. A generation manager, not a legacy registry facade, is the unit of publication.

### D. Configuration schema and runtime XML configuration are distinct but both currently Java-facing

Sources: `PluginConfiguration.java`, blob `085f0681ac816d5587c98c93e1b134ac1904656b`; `PluginConfigurationSchema.java`, blob `c649a8a2d58c496bf913a8a16ac989cab81a6e98`.

The current schema is an interface implemented by Java code, and its contract returns arrays/attribute-definition callbacks. Runtime configuration is a separate mutable object with child-configuration mutation. The new descriptor therefore needs a declarative, serializable configuration schema, while each invocation receives an immutable validated configuration value. Do not serialize a live `PluginConfiguration` implementation or call provider-defined schema methods during discovery.

### E. Refined minimal SPI and descriptor rules

The source audit supports the following constraints for #346:

1. **Separate artifacts:** the SPI API must compile without a dependency on `webharvest-core`. Core may depend on/adapt the SPI; the SPI must not depend back on Core.
2. **Data-only metadata:** use schema-validated immutable descriptor values. Keep UI/editor hints as optional projections, never as authority for runtime semantics, permissions or activation.
3. **Opaque provider binding:** descriptor entry-point data is inert until explicit host admission/activation. Do not store a live `Class`, `Constructor`, `PluginMetadata`, `PluginConfigurationSchema` or provider instance in the descriptor.
4. **Host-owned lifecycle:** host creates and closes provider/instance/invocation scopes. Provider close cannot close runtime-shared services. All failures are reported/aggregated rather than swallowed.
5. **Explicit compatibility adapter:** old implementations remain behind a Core-side adapter. The adapter is the only boundary allowed to know both legacy Core types and the new SPI.
6. **No semantic authority from UI metadata:** UI hints and display categories may be projected from descriptors, but semantic capability, execution role, host permissions and policy remain separately typed and validated.

### F. Remaining questions to settle before schema/API freeze

- Is the descriptor one artifact-level manifest containing one or more plugin contributions, or one descriptor per plugin contribution? Recommendation: one provider manifest per immutable artifact version, containing a deterministically ordered list of plugin contributions. This aligns provider trust/digest with artifact ownership while allowing one artifact to contribute multiple plugin IDs.
- How is provider identity ownership established and delegated? Reverse-DNS syntax alone does not prove domain control. The host must bind the namespace to an explicit publisher/trust policy; a provider cannot self-assert trusted ownership.
- Which canonicalization standard is used for digest/signature input? Recommendation: RFC 8785 JSON Canonicalization Scheme (JCS), with an explicit Unicode/number/duplicate-key policy and conformance vectors. Do not invent a project-specific canonicalizer.
- Which JSON Schema dialect is normative? Pin a specific dialect/version and validator behavior; do not use an unversioned `$schema` or silently accept unknown keywords.
- Are dependency declarations artifact-level, plugin-level or both? Define hard versus optional dependencies, version constraints, cycles, capability-provider selection and deterministic resolution before activation.
- Is the compatibility range a separate Core runtime API contract, not inferred from Maven version ranges? It must be an explicit machine-readable compatibility claim and must fail closed where unsupported.

**Disposition:** source evidence strengthens the #346 design but does not authorize implementation. The minimal compiling SPI/schema and independent fixture remain the next gate after explicit maintainer acceptance. No source, POM, test or runtime changes were made; no tests/builds were run.
