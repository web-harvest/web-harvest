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
