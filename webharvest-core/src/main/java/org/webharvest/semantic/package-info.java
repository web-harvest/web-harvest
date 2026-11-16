/**
 * Core-owned, read-only XML semantic inspection model (Stage 1).
 *
 * <h2>Fidelity contract</h2>
 * <ul>
 *   <li>Element, text, CDATA, comment and processing-instruction content is retained
 *       in parser event order, including whitespace inside the document element.</li>
 *   <li>Attributes are retained in the order reported by the active SAX provider.
 *       Namespace declarations are not guaranteed to appear as ordinary attributes.</li>
 *   <li>Qualified names retain namespace URI, local name and source prefix. Identity
 *       and deterministic paths use namespace URI plus local name, not the prefix.</li>
 *   <li>Element and content locations are SAX line/column coordinates at event time.
 *       They are not character offsets; attribute-level source locations are unavailable.</li>
 *   <li>CDATA sections remain distinguishable, but provider character chunking and
 *       adjacent same-kind text callbacks are coalesced. Original quote style,
 *       entity spelling, byte encoding and other lexical serialization details are
 *       not reconstructed.</li>
 *   <li>The original input String and source origin are retained by the load result;
 *       a successful document retains them as well.</li>
 * </ul>
 *
 * <h2>Deterministic identity</h2>
 * Element paths are derived from expanded names and one-based sibling occurrence
 * in document order. They do not depend on time, randomness, parser object identity
 * or namespace prefixes.
 *
 * <h2>Public construction</h2>
 * Immutable model types expose public constructors so non-XML adapters may build
 * the same graph. Public construction is <strong>not</strong> proof of XML parsing,
 * validation, or analyzer security policy; only {@link SemanticModel#load} applies
 * the hardened analyzer.
 *
 * <h2>Security and resource limits</h2>
 * The analyzer requires secure processing, namespace awareness, disabled XInclude,
 * rejection of DOCTYPE, disabled external general/parameter entities and external
 * DTD loading, and empty external DTD/schema access properties. Required settings
 * are set and verified; unsupported or unverifiable settings fail the load.
 * The lexical handler property is set and verified by instance identity. A rejecting
 * entity resolver is also installed. A security, limit or fatal parse failure never
 * exposes a partial document.
 *
 * <p>Diagnostic codes are classified from typed causes only (limit / security SAX
 * subclasses, analyzer init failures, or provider parse errors). Message-text sniffing
 * is not used. Honest limitation: with {@code disallow-doctype-decl}, JDK Xerces often
 * rejects DOCTYPE as a provider {@code SAXParseException} before {@code startDTD};
 * that fails closed with {@code XML_PARSE_ERROR}, not a fabricated
 * {@code SECURITY_VIOLATION} from message text. Typed {@code SECURITY_VIOLATION} still
 * applies when the lexical {@code startDTD} path or rejecting entity resolver fires.</p>
 *
 * <p>Default limits are measured in Java UTF-16 code units ({@link String#length()})
 * where applicable: 5,000,000 input characters, nesting depth 256, 100,000 elements,
 * 256 attributes per element, 5,000,000 total text characters, and 1,000,000 characters
 * for a single attribute value. These are defensive defaults, not XML language
 * constraints.</p>
 *
 * <h2>Plugin descriptor index (Stage 1)</h2>
 * {@link PluginDescriptorIndex} provides catalog-backed, namespace-aware plugin
 * resolution used by {@link SemanticModel#validateStructural}. Resolution does
 * not itself perform structural validation, runtime dispatch, or IDE schema
 * validation. Default {@link SemanticModel} construction loads the index lazily
 * from {@code PluginMetadataCatalog}.
 *
 * <h2>Structural validation ({@code validateStructural})</h2>
 * {@link SemanticModel#validateStructural} walks every {@link SemanticElement}
 * in document order and emits {@link SemanticDiagnostic} entries. The result
 * retains the same {@link SemanticDocument} instance passed in (identical to
 * {@link SemanticModel#inspect}).
 *
 * <h3>Diagnostic codes</h3>
 * <ul>
 *   <li>{@code UNKNOWN_ELEMENT} (ERROR) — no catalog descriptor</li>
 *   <li>{@code AMBIGUOUS_ELEMENT} (ERROR) — colliding catalog descriptors</li>
 *   <li>{@code UNAVAILABLE_OPTIONAL} (ERROR) — optional profile class absent</li>
 *   <li>{@code NAMESPACE_MISMATCH} (WARNING) — unknown with same-local-name
 *       candidates under other namespaces (paired with {@code UNKNOWN_ELEMENT})</li>
 *   <li>{@code RUNTIME_UNAVAILABLE} (WARNING) — resolved but not runtime-registered</li>
 *   <li>{@code REQUIRED_ATTRIBUTE_MISSING} / {@code UNKNOWN_ATTRIBUTE} /
 *       {@code INVALID_ATTRIBUTE_VALUE} (ERROR)</li>
 *   <li>{@code DISALLOWED_CHILD} / {@code REQUIRED_CHILD_MISSING} (ERROR)</li>
 *   <li>{@code INVALID_PARENT} / {@code DISALLOWED_ROOT} / {@code DISALLOWED_TEXT} (ERROR)</li>
 *   <li>{@code METADATA_LIMITATION} (WARNING) — rule skipped because schema is
 *       missing/unreliable or a closed constraint set is absent</li>
 * </ul>
 *
 * <h3>Schema-dependent and qualified relationship checks</h3>
 * Resolution diagnostics and runtime-availability diagnostics are evaluated
 * independently of schema presence. Root placement is checked when
 * {@link Stage1PluginDescriptor#getCanAppearAtRoot()} is present. Attribute
 * and text checks require {@link Stage1PluginDescriptor#hasReliableSchema()}.
 * A missing schema does not suppress explicitly available qualified relationship
 * checks; unavailable metadata is not interpreted as permission.
 *
 * <h3>Child policy and metadata provenance</h3>
 * {@link Stage1PluginDescriptor#getChildPolicy()} distinguishes
 * {@code ANY_CHILD}, {@code ALLOW_LIST}, {@code FORBID_CHILDREN}, and
 * {@code UNKNOWN}. Provenance records whether the decision came from explicit
 * visual metadata, catalog semantics, a schema allow-list, a legacy body
 * heuristic, an explicit closed policy, or missing metadata. Only an explicit
 * {@link org.webharvest.plugin.VisualPluginMetadata#acceptsArbitraryChildren()}
 * declaration establishes {@code ANY_CHILD}; a legacy {@code BODY_EVALUATOR}
 * heuristic remains {@code UNKNOWN}. Empty metadata never means forbid-all.
 *
 * <h3>Documented limitations</h3>
 * <ul>
 *   <li>{@code acceptsAnyChild} / arbitrary-child semantics are <strong>not</strong>
 *       exposed on {@link Stage1PluginDescriptor}. When {@code allowedSubElements}
 *       is empty under a reliable schema, child-allow checks emit
 *       {@code METADATA_LIMITATION} rather than inventing “allow all”.</li>
 *   <li>Sub-element and parent names are local-name only (namespace-unaware) as
 *       stored in today's catalog schema snapshot.</li>
 *   <li>Unknown-attribute checks require a closed name set from
 *       {@code attributeDefinitions} or the union of required+optional lists;
 *       otherwise {@code METADATA_LIMITATION}.</li>
 *   <li>{@code xmlns} / {@code xmlns:*} attributes are ignored for unknown-attribute
 *       checks. No database↔db element alias is invented.</li>
 *   <li>Does not execute plugins, mutate the document, or replace IDE/runtime
 *       validators.</li>
 * </ul>
 *
 * <p>This package does not define the final canonical configuration model,
 * execution semantics, editing, or XML export.</p>
 */
package org.webharvest.semantic;
