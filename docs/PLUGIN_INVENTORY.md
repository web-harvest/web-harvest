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
