package org.webharvest.semantic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.webharvest.WHConstants;

/**
 * Explicit namespace alias table used when building resolution keys.
 *
 * <p>{@link #coreDefaults()} maps {@link WHConstants#XMLNS_CORE_10_ALIASES} and
 * empty/null document namespaces to {@link WHConstants#XMLNS_CORE_10}.
 * {@link WHConstants#XMLNS_CORE} (2.1) is intentionally <strong>not</strong> an
 * alias of 1.0 and is returned unchanged.
 */
public final class NamespaceAliasTable {
    private final Map<String, String> aliasesToCanonical;

    public NamespaceAliasTable(Map<String, String> aliasesToCanonical) {
        Objects.requireNonNull(aliasesToCanonical, "aliasesToCanonical");
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : aliasesToCanonical.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            copy.put(entry.getKey(), Objects.requireNonNull(entry.getValue(), "canonical"));
        }
        this.aliasesToCanonical = Collections.unmodifiableMap(copy);
    }

    /**
     * Core Stage-1 defaults: WH 1.0 aliases → {@link WHConstants#XMLNS_CORE_10};
     * empty/null → {@link WHConstants#XMLNS_CORE_10}.
     */
    public static NamespaceAliasTable coreDefaults() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String alias : WHConstants.XMLNS_CORE_10_ALIASES) {
            map.put(alias, WHConstants.XMLNS_CORE_10);
        }
        return new NamespaceAliasTable(map);
    }

    /** Empty table: only empty/null collapse to {@link WHConstants#XMLNS_CORE_10}. */
    public static NamespaceAliasTable empty() {
        return new NamespaceAliasTable(Map.of());
    }

    /**
     * Canonicalizes a document or catalog namespace URI for index keys.
     *
     * <ul>
     *   <li>null or empty → {@link WHConstants#XMLNS_CORE_10}</li>
     *   <li>known alias → its canonical target</li>
     *   <li>otherwise the URI is returned unchanged (including {@link WHConstants#XMLNS_CORE})</li>
     * </ul>
     */
    public String canonicalize(String namespaceUri) {
        if (namespaceUri == null || namespaceUri.isEmpty()) {
            return WHConstants.XMLNS_CORE_10;
        }
        String mapped = aliasesToCanonical.get(namespaceUri);
        return mapped != null ? mapped : namespaceUri;
    }

    public Map<String, String> asMap() {
        return aliasesToCanonical;
    }
}
