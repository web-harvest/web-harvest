# Historical XML Dialect Compatibility Audit

**Status:** Bounded source/schema/test audit; no compatibility change is approved or implemented.
**Inspected source revision:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`.
**Related design:** [Plugin Registry and Generation Design](PLUGIN_REGISTRY_GENERATION_DESIGN.md), [Plugin Inventory](PLUGIN_INVENTORY.md), [Configuration Parameters and Secret Resolution](CONFIGURATION_PARAMETER_AND_SECRET_MODEL.md).

## 1. Executive findings

1. The repository contains explicit schema material for XML 1.0, 2.0 and 2.1 dialects. The oldest dialect explicitly represented in current source/resources is 1.0; this is not proof that no older external format or private fixture exists.
2. Namespace compatibility is only partially implemented in the current parser. Its private normalizer handles absent namespace, canonical 1.0 namespace and one SourceForge 1.0 alias, but omits another alias declared by `WHConstants.XMLNS_CORE_10_ALIASES`.
3. The 2.0 schema uses a distinct SourceForge namespace. The current parser passes that namespace through unchanged and does not apply a 2.0-to-current dialect adapter.
4. Historical element names are not comprehensively mapped to current plugin names. The current Core registrar includes `set-var` as a compatibility alias, but the inspected 1.0/2.0/2.1 schemas also declare `var-def`, `var` and/or `set`, which are not registered in the current `CorePluginRegistry` mapping list.
5. The parser does not perform XSD validation and currently creates configuration nodes for unknown element names. Failure is deferred until runtime plugin dispatch.
6. `XMLConfig.getVersion()` reads a root configuration property named `namespace`, while the parser stores the normalized namespace in the `DefaultPluginConfiguration.namespace` field. The inspected root XSDs do not declare a `namespace` attribute. For ordinary schema-valid documents, the version calculation therefore appears to classify the document as v2 regardless of its actual namespace.
7. The current parser tests primarily characterize 2.1 parsing and text content. They do not establish end-to-end compatibility for the historical 1.0 and 2.0 schemas, namespace aliases or old element-name aliases.

These are concrete compatibility risks. They do not establish that every historical configuration fails; execution must be characterized with real fixtures before making a final compatibility claim.

## 2. Schema and namespace inventory

| Dialect | Schema/resource | Target namespace | Notes |
|---|---|---|---|
| 1.0 | `webharvest-core/src/main/resources/config.xsd` | `http://web-harvest.sourceforge.net/schema/1.0/config` | Legacy 1.0 grammar; includes `var-def` and `var`. |
| 1.0 canonical runtime ID | `WHConstants.XMLNS_CORE_10` | `http://org.webharvest/schema/1.0/config` | Runtime canonical namespace. |
| 1.0 alias | `WHConstants.XMLNS_CORE_10_ALIASES` | `http://org.webharvest/wh-core.xsd` | Declared alias; not handled by the current parser normalizer. |
| 1.0 alias | `WHConstants.XMLNS_CORE_10_ALIASES` | `http://web-harvest.sourceforge.net/schema/1.0/config` | Matches the target namespace of the legacy 1.0 XSD. |
| 2.0 | `webharvest-core/src/main/resources/wh-core-2.0.xsd` | `http://web-harvest.sourceforge.net/schema/2.0/core` | Distinct dialect; includes `set` and `get`. |
| 2.1 Core | `webharvest-core/src/main/resources/wh-core-2.1-loose.xsd` | `http://org.webharvest/schema/2.1/core` | Current Core namespace; also declares historical element names such as `var-def`, `var`, `set` and `get`. |
| 2.1 JNDI extension | `webharvest-core/src/main/resources/wh-jndi-2.1.xsd` | `http://org.webharvest/schema/2.1/jndi` | Imports the 2.1 Core schema and declares a database extension. |
| 2.1 variable namespace | referenced by 2.1 schema | `http://org.webharvest/schema/2.1/var` | Appears in schema extension/attribute rules; namespace-qualified attribute identity needs explicit characterization. |

The constants in `WHConstants.java` declare:
- Core 2.1: `http://org.webharvest/schema/2.1/core`;
- variable 2.1: `http://org.webharvest/schema/2.1/var`;
- parameter 2.1: `http://org.webharvest/schema/2.1/param`;
- canonical Core 1.0 and the two 1.0 aliases listed above.

The 2.0 schema's SourceForge namespace is not included in the 1.0 alias set and is not represented by a separate parser dialect adapter in the inspected implementation.

## 3. Current parser behavior

### 3.1 Namespace normalization

`StreamingXmlParser.ConfigurationTreeHandler.normalizeNamespace(uri)` currently behaves as follows:

| Input namespace | Current parser result |
|---|---|
| null or empty | Canonical 1.0 namespace |
| `http://org.webharvest/schema/1.0/config` | Canonical 1.0 namespace |
| `http://web-harvest.sourceforge.net/schema/1.0/config` | Canonical 1.0 namespace |
| `http://org.webharvest/wh-core.xsd` | Passed through unchanged |
| `http://web-harvest.sourceforge.net/schema/2.0/core` | Passed through unchanged |
| `http://org.webharvest/schema/2.1/core` | Passed through unchanged |
| 2.1 JNDI or other namespace | Passed through unchanged |

The parser duplicates a partial alias list instead of using `WHConstants.XMLNS_CORE_10_ALIASES`. This creates drift between the declared compatibility constants and the actual parser behavior.

Namespace absence is also assigned a 1.0 identity by default. That is an observable compatibility policy, not a neutral XML parser default, and must be preserved or versioned deliberately.

### 3.2 Element and attribute handling

- `startElement(uri, localName, qName, attributes)` normalizes the element namespace but constructs the configuration using `localName`.
- The current parser's resolver/ElementInfo lookup is commented out; unknown elements are accepted into the tree as fallback nodes.
- `extractAttributes()` keys attributes by `Attributes.getLocalName(i)` and ignores attribute namespace URI and QName. The `namespace` parameter is not used.
- Prefix spelling is not preserved as a binding identity. Namespace-qualified attributes with the same local name can collapse to one map key.
- The runtime body executor dispatches child plugins by local element name through `CorePluginRegistry`, not by the full expanded QName.

Consequently, parsing an XML document successfully does not prove that its element names, namespaces or attributes are semantically compatible with the runtime plugin mappings.

### 3.3 Schema validation

`StreamingXmlParser.parseToTree()` creates a namespace-aware, non-validating SAX parser. The inspected path does not attach an XSD schema or invoke a separate grammar validator.

This means the schema files are not, by themselves, proof that the runtime enforces their element/attribute constraints. Unknown elements and attributes may survive parsing and fail later, be ignored, or be consumed by plugin-specific code.

### 3.4 Configuration version reporting

`XMLConfig.getVersion()` reads:

`configurationTree.getConfiguration().getProperty("namespace")`

However:
- `StreamingXmlParser` passes the normalized namespace to the `DefaultPluginConfiguration` constructor;
- `DefaultPluginConfiguration` stores it in its dedicated `namespace` field and exposes `getNamespace()`;
- the parser does not copy that namespace into a `namespace` property;
- the inspected 1.0, 2.0 and 2.1 root XSDs declare configuration attributes such as `charset`, `scriptlang` and `id`, not a `namespace` attribute.

No test in the inspected `DefaultConfigFactoryTest` verifies `XMLConfig.getVersion()`. Add a regression characterization before relying on this method for dialect selection or migration.

## 4. Historical element-name mapping gap

The current `CorePluginsRegistrar.registerCorePlugins()` maps the following relevant names:
- `def` → `DefPlugin`;
- `get` → `GetVarPlugin`;
- `set-var` → `SetVarPlugin` (explicitly documented as a legacy compatibility alias).

The inspected schema declarations include additional names:
- 1.0 `config.xsd`: `var-def`, `var`;
- 2.0 `wh-core-2.0.xsd`: `set`, `get`;
- 2.1 `wh-core-2.1-loose.xsd`: `var-def`, `var`, `set`, `get`, alongside `def`.

The inspected registrar does not register `var-def`, `var` or `set`. The current body executor looks up the child plugin class by the literal element name in `CorePluginRegistry`; it does not first normalize these historical names through a dialect adapter.

This is a source-confirmed mapping gap between schema vocabulary and the current registry table. It must be resolved by fixtures and an explicit compatibility map, not by guessing that similarly named elements have identical semantics.

## 5. Existing test evidence and missing characterization

The inspected `StreamingXmlParserTest`:
- constructs `StreamingXmlParser` directly;
- uses the 2.1 Core namespace in its sample documents;
- tests basic/nested/multiple elements and text content;
- does not exercise 1.0/2.0 namespace aliases or old element-name mappings.

`StreamingXmlParserTextContentTest` focuses on CDATA, plain text, multiline and empty text. `DefaultConfigFactoryTest` uses a stub parser and does not characterize real XML dialect parsing or `getVersion()`.

The recursive tree contains 130 `.xml` files across the repository (including Maven POMs, site metadata and non-WebHarvest XML). The tracked WebHarvest example corpus was scanned for root namespace declarations:
- The maintained `docs/web/examples` and IDE webapp example copies use the 2.1 Core namespace.
- The 44 numbered Core `new_plugins` fixtures use the 2.1 Core namespace.
- Five legacy-style Core/Mail test resources have no namespace and therefore enter the parser's implicit 1.0 fallback path; the inspected files include repeated `var-def` elements.
- The inspected optional-module integration fixtures use the 2.1 Core namespace.
- No explicit 1.0 or 2.0 namespace was found in the scanned XML configuration example/test corpus. This does not rule out downstream or untracked documents.

The named tests associated with several namespace-less resources do not prove XML compatibility: the inspected CSS-selector test constructs a plugin configuration directly; the logging integration test invokes `LogPlugin` directly; and the variable/function tests exercise variable/script behavior without loading the corresponding XML fixture. A fixture's presence under `src/test/resources` is not proof that a test parses or executes it. A reference/use inventory is still required before declaring these fixtures active or obsolete.

## 6. Required compatibility fixture matrix

Before changing parser, registry or XML adapter behavior, create a fixture set from real historical documents and cover at least:

1. **Namespace variants:** absent namespace; canonical 1.0; both declared 1.0 aliases; 2.0 SourceForge namespace; 2.1 Core; 2.1 variable namespace; 2.1 JNDI extension.
2. **Element aliases:** `var-def`, `var`, `def`, `set`, `set-var`, `get`; establish the expected plugin mapping and exact variable scope/overwrite/return semantics for each dialect.
3. **Root settings:** charset and scriptlang absent, empty, whitespace-padded, valid, legacy alias and invalid; verify effective runtime behavior and diagnostics.
4. **Qualified names:** default namespace, arbitrary prefix, alternate prefix for same URI, unknown namespace with a known local name, same local name from two namespaces.
5. **Attributes:** unqualified attributes, namespaced attributes, same local name from distinct namespaces, namespace-qualified variable/parameter references.
6. **Grammar:** unknown element, invalid child placement, missing required attribute, unknown attribute, empty root and nested include.
7. **Text:** leading/trailing whitespace, whitespace-only text, CDATA, mixed content and text around child elements.
8. **Execution parity:** parse and execute the oldest supported fixtures through the historical behavior baseline and current runtime; compare results, side effects, errors and effective configuration values.

Every fixture must record its dialect/version, source provenance, expected normalized semantic model and expected execution result. The suite must fail if a historical fixture only parses but cannot dispatch or execute.

## 7. Review conclusion

The current parser is a direct XML-to-`PluginConfiguration` POC, not yet a complete historical dialect compatibility adapter. The schema inventory and source mapping expose specific gaps in namespace aliases, old element names, version reporting, validation and tests.

The compatibility requirement remains the historical XML contract, not the current Java parser shape. The next design should normalize every supported XML dialect into a canonical internal configuration model and preserve old names/defaults through explicit, versioned mappings. Do not let the current local-name-only parser become the definition of compatibility.

No production code, tests, POMs or workflow files were changed by this audit.
