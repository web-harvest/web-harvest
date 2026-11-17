# Plugin inventory — evidence baseline

**Status:** Preliminary inventory baseline; not an approved Core membership list.
**Source snapshot:** `main` at `c4357d8fa531d5ba536b5d217e149de8e1bd8262` (2026-11-16).
**Refresh note:** The seven primary registration/runtime/ZIP evidence files cited in this inventory were verified unchanged since the prior snapshot. This remains a targeted evidence baseline, not a complete per-plugin re-audit. Stage 1 descriptor projection and structural validation were subsequently merged in PR #90; they do not change the manual Core mappings or parser/registry lifecycle described here.
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
| `webharvest-zip` | `ZipPlugin`, `ZipEntryPlugin` | Optional-integration candidate; inspect parent-child execution contract |

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
- [ ] Investigate `ZipEntryPlugin.getParentZipPlugin(...)`: the current method returns `null` with a TODO while execution requires a parent `ZipPlugin`. Confirm whether this is a dead path, known defect or test gap before proposing migration.
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
| `webharvest-zip` | `zip`, `zip-entry` | JDK ZIP APIs only | Only one integration-named test currently visible; see limitation below | Optional extension candidate, but classification and tests need review |

The FTP child classes do not have separate test source files, but `FtpPluginTest` contains explicit `testFtpGetPlugin`, `testFtpPutPlugin` and `testFtpListPlugin` methods. Do not infer missing coverage solely from file-name matching.

### ZIP test gap and parent contract

The current `ZipPluginIntegrationTest.testZipPluginExample()` only locates the XML example resource and asserts that the file exists. It does not load or execute the configuration, nor assert the generated archive contents. It is therefore not evidence of end-to-end ZIP runtime behavior.

`ZipEntryPlugin` requires a parent `ZipPlugin` in execution and throws if it cannot resolve one. The parent-resolution helper currently returns `null` with a TODO. This is a direct defect or unfinished path, not merely a test naming gap. The ZIP module must not be considered complete until the parent/child context contract and an actual archive-content integration test are repaired.

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

The current `ZipEntryPlugin.getParentZipPlugin(context)` implementation returns `null` unconditionally and contains an explicit TODO. `doExecute` immediately throws when the result is null, before it writes an entry. The existing `ZipPluginIntegrationTest.testZipPluginExample()` only checks that the XML resource exists; it does not load the configuration, execute it or inspect archive contents.

This is now tracked as [Issue #97 — ZIP entry plugin cannot resolve its parent ZIP plugin](https://github.com/web-harvest/web-harvest/issues/97). The ZIP module remains an optional extension candidate but is not functionally validated and must not be described as production-ready until the parent/child execution contract, stream cleanup and archive-content tests are implemented.

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
| `xml` | `XMLPlugin.doExecute()` executes its body and returns a `NodeVariable`. Within `<xslt>`, the parent reads child `_textContent` directly rather than dispatching the mapped `XMLPlugin`. | Dual-use: standalone executable wrapper plus declarative XSLT input node; direct Failsafe test pending PR #183. |
| `stylesheet` | `XsltPlugin` reads child `_textContent` directly. `StylesheetPlugin.doExecute()` currently returns `EmptyVariable.INSTANCE`. | Declarative XSLT configuration node; no independent executable capability demonstrated. |
| `xq-expression` | `XQueryPlugin` reads child `_textContent` directly. | Declarative XQuery expression node. |
| `xq-param` | `XQueryPlugin` parses the child name/type/text itself; nested value-producing children are executed by the parent. | Declarative XQuery parameter node. |

Evidence: [XsltPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/XsltPlugin.java),
[XQueryPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/XQueryPlugin.java),
[StylesheetPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/StylesheetPlugin.java),
and [XMLPlugin.java](../webharvest-core/src/main/java/org/webharvest/plugin/core/XMLPlugin.java).
The XSLT/XQuery integration tests prove parent transformation and parameter semantics. The standalone `xml` wrapper has its own executable path; PR #183 adds its missing direct Failsafe contract.

**Compatibility rule:** preserve the existing XML names and forms. The future
descriptor/registry contract should distinguish executable plugin identities from
declarative configuration-node identities. Do not remove or rename these nodes
as part of the reliability work.

## 13. Core XML integration-test evidence quality (refreshed 2026-11-17)

The six principal deterministic XML integration suites were re-read from current
`main` after PR #181 at `d9a1466953a82afc8b6f68b86b4ddb04bde5fe37`. The earlier placeholder
counts in this document are historical and no longer describe the current files.

| Test class | Current test methods | `assertTrue(true)` | Broad catch of Exception / RuntimeException / Throwable | Observable behavior examples |
|---|---:|---:|---:|---|
| `CorePluginsIntegrationTest` | 9 | 0 | 0 | Exact text, definition, legacy set-var, get, file output, list contents and value-of results; Sleep duration lower bound. |
| `ControlFlowPluginsIntegrationTest` | 7 | 0 | 0 | If/Else selected output; Loop/While ordering and counts; Try/Catch branch state and continuation; Case output. |
| `AdvancedPluginsIntegrationTest` | 3 | 0 | 0 | Exact variable/body values and rendered template output. |
| `HttpPluginsIntegrationTest` | 2 | 0 | 0 | Deterministic stub response URL/origin and stored response values. |
| `XmlTransformationPluginsIntegrationTest` | 7 | 0 | 0 | XPath selection, HTML-to-XML, JSON-to-XML, XML-to-JSON, XSLT, XQuery output and empty-sequence behavior. |
| `TextProcessingPluginsIntegrationTest` | 3 | 0 | 0 | Regexp match/capture list, ordered tokenization, JavaScript result. |
| **Total** | **31** | **0** | **0** | Source-level assertion review; not a claim of exhaustive 44-class coverage. |

The six source files were inspected individually. The counts above are static
source observations, not test execution results. In particular, zero placeholder
assertions and zero broad catch-and-pass blocks do not prove every edge case or
every registered implementation is covered.

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

Each of PRs #170–#174 passed its exact-head three-job CI run, including the full
Maven verify job, Checkstyle and JaCoCo report validation. The subsequent main
push run #537 ([Actions run 44158373034](https://github.com/web-harvest/web-harvest/actions/runs/44158373034)) on
`d9a1466953a82afc8b6f68b86b4ddb04bde5fe37` also passed all three jobs: Semantic
Core contracts, full Surefire/Failsafe suites, and unqualified Core verify with
JaCoCo report validation. PR #182 after that changed documentation only. Root
JaCoCo minimums remain 0.80 instruction coverage and 0.80 branch coverage.
PR #183 adds the standalone `xml` wrapper test; its exact-head CI is pending and
must pass before the 44-mapping matrix is considered closed.

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
In particular, XQuery parameter binding, arithmetic aggregation and empty-result
handling still need real runtime tests. The parent XQuery XML integration test
does not cover those scenarios. The per-plugin matrix must continue to separate
metadata/schema smoke tests from observable runtime behavior.

## 14. Optional-module execution and dependency evidence (2026-11-17)

A targeted source/POM/test audit covered Database, FTP, Mail, WebBrowser and ZIP. This is
evidence for the module boundary review in Issue #93, not an approved Core
membership decision.

| Module | Direct runtime dependency | Source-confirmed side effects / lifecycle | Current test evidence |
|---|---|---|---|
| `webharvest-ftp` | `commons-net:commons-net` | `FtpPlugin`, `FtpGetPlugin`, `FtpPutPlugin` and `FtpListPlugin` connect to remote FTP servers, authenticate, and read/write/list remote files. The connection-oriented implementations disconnect in `finally`. Requires external network, credentials and remote filesystem access. | `FtpPluginIntegrationTest` only checks that the example XML resource exists. `FtpPluginTest` mostly covers metadata/schema/initialization; its execute and invalid-action tests accept a broad `Exception` without asserting the exact failure. No deterministic FTP server execution contract was found in the inspected tests. |
| `webharvest-mail` | `org.apache.commons:commons-email` | `MailPlugin` creates SimpleEmail/HtmlEmail and calls `email.send()`; this is an external SMTP/network side effect that can deliver real email. | `MailPluginIntegrationTest` only checks that the example XML resource exists. No deterministic SMTP execution contract was found in the inspected integration test. |
| `webharvest-webbrowser` | `net.sourceforge.htmlunit:htmlunit:2.70.0` | `WebBrowserPlugin` loads remote URLs, executes JavaScript/AJAX and stores WebClients in static `namedPages` / `defaultPage` state. The explicit `close` action closes clients, but the inspected class has no automatic Harvester-run cleanup hook; the static default client also raises cross-run isolation/concurrency questions. | The integration test executes XML but asserts only a non-null context; it does not assert loaded page content or script results. |
| `webharvest-zip` | No additional direct runtime library beyond Core/JDK | `ZipPlugin` owns a `ZipOutputStream` and closes it in `finally`. `ZipEntryPlugin.getParentZipPlugin(context)` unconditionally returns null, so every `<zip-entry>` currently fails before writing. | `ZipPluginIntegrationTest` only checks that the example XML resource exists; it does not execute the Harvester or inspect archive entries. Confirmed defect is tracked in Issue #97. |
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
- The `MockDBDriver.jar` fixture is only used to verify driver loading; its mock
  `connect()` throws `UnsupportedOperationException`. The module POM declares no
  H2 or other JDBC driver, and the XML example is not executed by a Java test.
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
- [Database module POM](../webharvest-database/pom.xml)
- [DatabasePlugin.java](../webharvest-database/src/main/java/org/webharvest/plugin/db/DatabasePlugin.java)
- [ConnectionPlugin.java](../webharvest-database/src/main/java/org/webharvest/plugin/db/ConnectionPlugin.java)
- [DatabaseModule.java](../webharvest-database/src/main/java/org/webharvest/ioc/DatabaseModule.java)
- [DatabasePluginTest.java](../webharvest-database/src/test/java/org/webharvest/plugin/db/DatabasePluginTest.java)
- [ConnectionPluginTest.java](../webharvest-database/src/test/java/org/webharvest/plugin/db/ConnectionPluginTest.java)
- [DefaultDriverManagerTest.java](../webharvest-database/src/test/java/org/webharvest/runtime/database/DefaultDriverManagerTest.java)
- [Database module README](../webharvest-database/README.md)

### Remaining acceptance work

1. Complete the 44-row matrix with one strongest-evidence classification per
   XML name: meaningful runtime behavior, execution smoke, metadata/unit only,
   or absent. Keep registration evidence separate from execution evidence.
2. Decide the descriptor role for the four declarative/dual-use nodes above
   under Issues #93 and #94, preserving XML compatibility.
3. Reconcile each remaining class-level gap before closing Issue #98; do not
   treat the six-suite source scan or aggregate green CI as proof that all 44
   mapped classes are independently executable.
4. Confirm the current `main` push CI result through a workflow-run listing
   capable of returning push-triggered runs, or run an equivalent exact-main
   verification.

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
- [TextProcessingPluginsIntegrationTest.java](../webharvest-core/src/test/java/org/webharvest/plugin/integration/TextProcessingPluginsIntegrationTest.java)
