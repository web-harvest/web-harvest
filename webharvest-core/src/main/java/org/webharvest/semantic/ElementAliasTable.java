package org.webharvest.semantic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit element local-name alias table.
 *
 * <p>The Stage-1 default is <strong>empty</strong>: there is no {@code database}↔{@code db}
 * (or any other) element alias. Aliases, when configured, map a document local name
 * to the catalog local name used as the index key.
 */
public final class ElementAliasTable {
    private final Map<String, String> aliasesToCanonical;

    public ElementAliasTable(Map<String, String> aliasesToCanonical) {
        Objects.requireNonNull(aliasesToCanonical, "aliasesToCanonical");
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : aliasesToCanonical.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "alias");
            Objects.requireNonNull(entry.getValue(), "canonical");
            if (entry.getKey().isEmpty() || entry.getValue().isEmpty()) {
                throw new IllegalArgumentException("element alias keys and values must be non-empty");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        this.aliasesToCanonical = Collections.unmodifiableMap(copy);
    }

    /** Default Stage-1 table: no element aliases. */
    public static ElementAliasTable empty() {
        return new ElementAliasTable(Map.of());
    }

    /**
     * Returns the canonical local name for indexing/lookup. Unmapped names are
     * returned unchanged.
     */
    public String canonicalize(String localName) {
        Objects.requireNonNull(localName, "localName");
        String mapped = aliasesToCanonical.get(localName);
        return mapped != null ? mapped : localName;
    }

    public Map<String, String> asMap() {
        return aliasesToCanonical;
    }
}
