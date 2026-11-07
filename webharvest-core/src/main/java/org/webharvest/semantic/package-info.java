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
 * <h2>Security and resource limits</h2>
 * The analyzer requires secure processing, namespace awareness, disabled XInclude,
 * rejection of DOCTYPE, disabled external general/parameter entities and external
 * DTD loading, and empty external DTD/schema access properties. Required settings
 * are set and verified; unsupported or unverifiable settings fail the load.
 * A rejecting entity resolver is also installed. A security, limit or fatal parse
 * failure never exposes a partial document.
 *
 * Default limits are measured in Java UTF-16 code units where applicable:
 * 5,000,000 input characters, nesting depth 256, 100,000 elements, 256 attributes
 * per element, and 5,000,000 total text characters. These are defensive defaults,
 * not XML language constraints.
 *
 * <p>This package does not define the final canonical configuration model, plugin
 * resolution, structural validation, execution semantics, editing, or XML export.</p>
 */
package org.webharvest.semantic;
