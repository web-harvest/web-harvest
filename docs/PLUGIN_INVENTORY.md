# Plugin inventory — evidence baseline

**Status:** Preliminary inventory baseline; not an approved Core membership list.
**Source snapshot:** `main` at `4e6794de4163c770f9823dd065bc2b975f69d123` (2026-11-15).
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
| `BodyProcessorPlugin` | `@CorePlugin(elementName="body-processor")`; class comment describes an internal body execution processor; metadata tests exist | Not registered | Dedicated unit test | Internal runtime helper; do not count as public Core XML element without contrary call-site evidence |
| `ConfigPlugin` | `@CorePlugin(elementName="config")`; metadata and execution tests exist | Not registered | Unit, XML and integration tests | Unresolved system/configuration element; investigate historical XML use and parser/executor call sites before excluding or promoting |
| `VarPlugin` | `@CorePlugin(elementName="var")`; explicitly deprecated in favor of `<def>` | Not registered | Legacy unit tests still exist | Deprecated compatibility candidate; not currently enabled by default runtime |
| `SetVarPlugin` | `@CorePlugin(elementName="set-var")`; deprecated for new configurations, retained for old XML | Registered as `set-var` | Dedicated tests and an explicit registrar contract test | Public legacy compatibility element; preserve |
| Remaining registrar classes | Manual element mapping is authoritative for current default runtime | Registered | Per-class test files exist in the test tree | Current Core runtime candidates; final product membership still requires dependency/security/semantic review |

### A key inconsistency in the metadata audit

`AllPluginsMetadataAuditTest` builds its own explicit list of Core classes and explicitly excludes `VarPlugin` and `SetVarPlugin`, describing both as removed in favor of `DefPlugin`. That test's list also excludes `BodyProcessorPlugin` and `ConfigPlugin`. However, the runtime registrar still registers `SetVarPlugin`, and `SetVarCompatibilityRegistrationContractTest` explicitly asserts that `set-var` remains mapped. The test audit therefore describes a *new-configuration metadata cohort*, not the actual enabled runtime set. Its comment/list must not be used as the authoritative runtime inventory.

The `@CorePlugin` annotation Javadoc says annotated plugins are always available Core functionality. That statement is too broad for current behavior: `BodyProcessorPlugin`, `ConfigPlugin`, and `VarPlugin` are annotated but absent from the default registrar; FTP implementations also carry the annotation while living in an optional module. The annotation currently mixes at least three concepts: implementation lineage, IDE metadata/palette classification, and runtime/product membership. These must become separate fields or descriptor decisions in the future SPI.

### Core dependency and risk classification still needed

Even for the 44 current runtime mappings, membership in a minimal engine is not yet approved. In particular:
- Network and extraction plugins (`HttpPlugin`, `HtmlToXmlPlugin`, JSON/XML transforms, XPath/XQuery, XSLT) need a dependency/license/security review before declaring them indispensable Core.
- `ScriptPlugin` needs an explicit policy for script engines, language support and execution sandboxing.
- `FilePlugin` needs a documented filesystem access/security boundary.
- `ConfigPlugin` and `BodyProcessorPlugin` need internal-vs-public contracts resolved.
- Legacy `set-var` must remain available for compatibility even if it is not recommended for new configurations.
- `VarPlugin` should remain out of the default runtime unless a deliberate compatibility requirement is established; its deprecated class and tests alone do not imply support.

## 9. Required follow-up before calling the plugin platform complete

1. Define one authoritative runtime registration API and remove the split-brain between metadata-only scanning and execution registry registration.
2. Make extension registration opt-in and module/classloader aware; never require Core to depend on optional integrations.
3. Ensure the same discovered descriptor drives XML element resolution, metadata catalog, schema/validation and semantic resolution.
4. Add duplicate ID/element/namespace conflict checks and deterministic ordering.
5. Add integration tests proving: plugin jar on classpath -> discovered -> registered -> XML parsed -> instantiated with DI -> executed -> lifecycle/resources closed.
6. Add an out-of-tree test plugin Maven project before stabilizing the SPI or extracting the company extensions repository.
7. Repair ZIP parent resolution and replace the current resource-existence test with an actual execution/output assertion.

