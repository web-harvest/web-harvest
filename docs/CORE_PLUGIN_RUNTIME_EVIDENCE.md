# Core XML Runtime Evidence Matrix

**Scope:** the exact 44 XML element mappings registered by `CorePluginsRegistrar`, not the 47 Java `*Plugin` classes, 53-name metadata fallback, or optional-module classes.

**Source of truth:** `webharvest-core/src/main/java/org/webharvest/definition/CorePluginsRegistrar.java`. The exact names and target classes are guarded by `CorePluginsRegistrarContractTest` (PR #173).

This is an evidence map, not a claim that all mapped names are independent execution capabilities. Parent-consumed XML children are marked explicitly. A mapping's presence in the registrar is not proof of a standalone dispatch path.

## 44 mapping rows

| XML mapping | Runtime role | Evidence | Strength / boundary |
|---|---|---|---|
| `call` | Executable function invocation | `FunctionCallIntegrationTest.testSimpleFunctionWithReturn`; `testFunctionCalledMultipleTimes` | Direct XML loader/Harvester; exact returned values. |
| `call-param` | Call child parameter | `FunctionCallIntegrationTest.testFunctionWithParameters`; `testFunctionWithMultipleParameters` | Parent-level XML contract; exact parameter substitution. |
| `case` | Executable control-flow branch | `ControlFlowPluginsIntegrationTest.testCasePlugin` | Direct XML runtime; selected output asserted. |
| `catch` | Try error handler | `ControlFlowPluginsIntegrationTest.testTryCatchHandlesRuntimeFailureAndSkipsRemainingTryBody`; `ExitPluginRuntimeContractIntegrationTest.trueConditionInsideTryBypassesCatchAndStopsRootSiblings` | Parent-level XML runtime; handler and bypass semantics asserted. |
| `constant` | Executable value source | `CorePluginsIntegrationTest.testConstantPlugin` | Direct XML runtime; exact value asserted. |
| `def` | Variable definition | `CorePluginsIntegrationTest.testDefPlugin`; `AdvancedPluginsIntegrationTest.testDefPluginStoresExactValueInContext` | Direct XML runtime; exact context value. |
| `else` | Conditional alternative | `ControlFlowPluginsIntegrationTest.testIfElsePlugin` | Parent-level XML runtime; selected branch output asserted. |
| `empty` | Execute body for side effects, return empty | `EmptyPluginRuntimeContractIntegrationTest.executesNestedPluginsForSideEffectsAndReturnsEmptyWithoutStoppingParentExecution` | Direct XML runtime; side effect, empty result and continuation asserted. |
| `exit` | Terminate execution conditionally | `ExitPluginRuntimeContractIntegrationTest` (six true/false, loop, while, call and try cases) | Direct XML runtime; exit event and sibling/iteration stopping asserted. |
| `file` | File read/write behavior | `CorePluginsIntegrationTest.testFilePlugin` | Direct XML runtime; exact file content asserted. |
| `function` | Function definition | `FunctionCallIntegrationTest.testSimpleFunctionWithReturn`; `testFunctionWithParameters` | Direct XML runtime; definition and invocation observed. |
| `get` | Read variable | `CorePluginsIntegrationTest.testGetVarPlugin` | Direct XML runtime; exact returned value. |
| `html-to-xml` | HTML conversion | `XmlTransformationPluginsIntegrationTest.testHtmlToXmlPluginPreservesFixtureContent` | Direct XML runtime; converted content asserted. |
| `http` | HTTP request | `HttpPluginsIntegrationTest.testHttpPluginReturnsStubbedResponse`; `testHttpPluginStoresStubbedResponseInContext` | Direct XML runtime with deterministic stub; response and context asserted. |
| `http-header` | HTTP header child | `HttpPluginRequestContractIntegrationTest.sendsPostBodyHeadersAndEncodedParameters` | Parent-consumed XML child; recording service asserts header. |
| `http-param` | HTTP parameter child | `HttpPluginRequestContractIntegrationTest.sendsPostBodyHeadersAndEncodedParameters` | Parent-consumed XML child; recording service asserts URL encoding and values. |
| `if` | Conditional execution | `ControlFlowPluginsIntegrationTest.testIfElsePlugin` | Direct XML runtime; selected branch output asserted. |
| `include` | Execute included configuration | `IncludePluginRuntimeContractIntegrationTest.executesIncludedFileAndContinuesParentConfiguration` | Direct XML loader/Harvester; included state and parent continuation asserted. |
| `json-to-xml` | JSON conversion | `XmlTransformationPluginsIntegrationTest.testJsonToXmlPluginPreservesObjectValues` | Direct XML runtime; converted object values asserted. |
| `loop` | Collection iteration | `ControlFlowPluginsIntegrationTest.testLoopPlugin` | Direct XML runtime; ordered values/count asserted. |
| `regexp` | Regular-expression processing | `TextProcessingPluginsIntegrationTest.testRegexpPluginExposesMatchAndCaptureGroups` | Parent-level XML runtime; match and capture output asserted. |
| `regexp-pattern` | Regexp pattern child | `TextProcessingPluginsIntegrationTest.testRegexpPluginExposesMatchAndCaptureGroups` | Parent-consumed XML child; exercised by the regexp result contract. |
| `regexp-result` | Regexp result child | `TextProcessingPluginsIntegrationTest.testRegexpPluginExposesMatchAndCaptureGroups` | Parent-consumed XML child; capture-template result asserted. |
| `regexp-source` | Regexp source child | `TextProcessingPluginsIntegrationTest.testRegexpPluginExposesMatchAndCaptureGroups` | Parent-consumed XML child; source/match behavior asserted. |
| `return` | Function return | `FunctionCallIntegrationTest.testSimpleFunctionWithReturn`; `testExplicitEmptyReturnOverridesEarlierBodyValue` | Direct XML runtime; returned and explicitly empty values asserted. |
| `script` | Script evaluation | `TextProcessingPluginsIntegrationTest.testScriptPluginEvaluatesJavaScriptAndReturnsValue` | Direct XML runtime; exact script result asserted. |
| `list` | List construction | `CorePluginsIntegrationTest.testListPlugin` | Direct XML runtime; ordered list values asserted. |
| `log` | Logging side effect and return | `LogPluginRuntimeContractIntegrationTest.writesXmlBodyToConfiguredStreamsAndReturnsTheMessage` | Direct XML runtime; configured streams and return value asserted. |
| `value-of` | Value extraction | `CorePluginsIntegrationTest.testValueOfPlugin` | Direct XML runtime; exact value asserted. |
| `xml` | XML body wrapper | `XMLPlugin.doExecute`; `XmlTransformationPluginsIntegrationTest.testXmlPluginReadsLiteralCDataAndFeedsXPath` | Dual-use: literal CDATA becomes a runtime value consumed by downstream XPath; under XSLT, parent reads child content directly. PR #183 merged. |
| `set-var` | Legacy variable assignment | `CorePluginsIntegrationTest.testSetVarPlugin`; `SetVarCompatibilityRegistrationContractTest` | Direct XML runtime plus explicit legacy registration contract; preserve for compatibility. |
| `sleep` | Delay | `CorePluginsIntegrationTest.testSleepPlugin` | Direct XML runtime; monotonic elapsed-time lower bound asserted. |
| `stylesheet` | XSLT stylesheet child | `XmlTransformationPluginsIntegrationTest.testXsltPluginAppliesStylesheet`; `StylesheetPlugin.doExecute` | Declarative XSLT configuration; parent reads child text; helper returns EmptyVariable and is not an independent execution capability. |
| `template` | Template rendering | `AdvancedPluginsIntegrationTest.testDefinitionTemplateWritesExpectedOutput` | Direct XML runtime; exact rendered output asserted. |
| `text` | Text body | `CorePluginsIntegrationTest.testTextPlugin` | Direct XML runtime; exact output asserted. |
| `tokenize` | Tokenization | `TextProcessingPluginsIntegrationTest.testTokenizePluginSplitsTextIntoOrderedTokens` | Direct XML runtime; ordered tokens asserted. |
| `try` | Exception boundary | `ControlFlowPluginsIntegrationTest.testTryCatchSkipsCatchWhenTryBodySucceeds`; `testTryCatchHandlesRuntimeFailureAndSkipsRemainingTryBody` | Direct XML runtime; success/failure, handler and continuation asserted. |
| `while` | Conditional iteration | `ControlFlowPluginsIntegrationTest.testWhilePlugin` | Direct XML runtime; iteration order/count asserted. |
| `xml-to-json` | XML conversion | `XmlTransformationPluginsIntegrationTest.testXmlToJsonPluginPreservesElementValues` | Direct XML runtime; converted values asserted. |
| `xpath` | XPath selection | `XmlTransformationPluginsIntegrationTest.testXPathPluginExtractsBothItemValues`; `XPathWithGetVarIntegrationTest.testXPathTextNode` | Direct XML runtime; selected text values asserted. |
| `xquery` | XQuery evaluation | `XmlTransformationPluginsIntegrationTest.testXQueryPluginReturnsBothSelectedItems`; `testXQueryEmptySequenceProducesEmptyResultAndContinues`; `XQueryComplexTypesIntegrationTest` (3 methods); `XQueryParameterTypesTest` (8 methods) | Direct XML runtime plus typed scalar/sequence/node parameter contracts, arithmetic and empty-sequence continuation. |
| `xq-expression` | XQuery expression child | `XQueryPlugin` reads child `_textContent`; `XQueryComplexTypesIntegrationTest` and `XQueryParameterTypesTest` | Declarative XQuery configuration node; parent consumes expression text; parent XML runtime tests prove evaluation with typed parameters. |
| `xq-param` | XQuery parameter child | `XQueryPlugin` reads name/type/value; `XQueryParameterTypesTest` (8 methods); `XQueryComplexTypesIntegrationTest` (3 methods) | Declarative XQuery configuration node; parent consumes the parameter and executes nested value-producing children; typed scalar/sequence/node results are asserted. |
| `xslt` | XSLT transformation | `XmlTransformationPluginsIntegrationTest.testXsltPluginAppliesStylesheet` | Direct XML runtime; transformed output asserted. |

`var` is intentionally **not** one of the 44 default mappings. `CorePluginsRegistrar` explicitly comments `VarPlugin removed - use DefPlugin instead`. The class remains annotated/deprecated and may be visible to class/metadata scanning, but that is not proof of default XML runtime registration. By contrast, `set-var` remains an explicit legacy mapping in the 44-name set.

## Classification rules

- **Direct XML runtime:** configuration is loaded through the XML loader and executed through the Harvester/runtime path; assertions observe output, state, events, file content or request effects.
- **Parent-consumed XML child:** a parent plugin reads or executes the child as part of its own configuration. This proves the parent-level XML contract, not independent dispatch of the child mapping.
- **Dual-use:** the mapping has its own executable implementation and is also consumed declaratively by a parent in another XML shape.
- **Unit-only evidence is not sufficient** for a public runtime mapping when an XML-level contract can reasonably be tested. The `xml` row is dual-use and has direct Failsafe evidence through PR #183.

## Important configuration-node distinction

The four names previously grouped together need separate treatment:

- `xml` is **dual-use**. Standalone `XMLPlugin.doExecute()` executes the body and returns a `NodeVariable`; under `<xslt>`, `XsltPlugin` reads the child content directly.
- `stylesheet` is declarative XSLT configuration. `XsltPlugin` reads `_textContent`; `StylesheetPlugin.doExecute()` returns `EmptyVariable`.
- `xq-expression` is declarative XQuery expression content read by `XQueryPlugin`.
- `xq-param` is declarative XQuery parameter configuration read by `XQueryPlugin`; nested value-producing child plugins are executed by the parent.

Preserve all four XML names for compatibility. The future descriptor/registry design should distinguish executable plugin identities from declarative configuration-node identities.

## Test-suite and CI context

The six principal deterministic XML integration suites are:

- `CorePluginsIntegrationTest` — 9 methods.
- `ControlFlowPluginsIntegrationTest` — 7 methods.
- `AdvancedPluginsIntegrationTest` — 3 methods.
- `HttpPluginsIntegrationTest` — 2 methods.
- `XmlTransformationPluginsIntegrationTest` — 8 methods on current main after PR #183.
- `TextProcessingPluginsIntegrationTest` — 3 methods.

Additional focused runtime suites cover `empty`, `exit`, `include`, and `log`; request-contract tests cover `http-header` and `http-param`; function/call and XQuery parameter suites provide their own XML runtime coverage.

Current main runtime baseline after PR #183: `4a4dcc609f302decdb3fd9f1e3da49790f01fc18`. Main push run #537 (`44158373034`) after PR #181 and post-merge main push run #44161596727 after PR #183 both passed all three jobs: Semantic Core contracts, full Surefire/Failsafe, and unqualified Core verify with Checkstyle/JaCoCo validation. PR #182 and PR #184 changed documentation only.

Additional XQuery XML-loader/Harvester evidence is provided by `XQueryParameterTypesTest` (8 methods) and `XQueryComplexTypesIntegrationTest` (3 methods), covering typed scalar, sequence and node parameters. The `xml` row is complete; no pending direct-runtime row remains.

## Remaining audit boundary

This matrix reconciles the 44 registered names to current source/test evidence. It does not approve the final minimal Core product set, resolve optional module boundaries, or prove security/dependency suitability. Those decisions remain tracked separately under Issues #93 and #94.