package org.webharvest.semantic;

import java.util.Objects;

/** XML attribute in the order reported by the SAX provider. */
public final class SemanticAttribute {
    private final QualifiedName name;
    private final String value;

    public SemanticAttribute(QualifiedName name, String value) {
        this.name = Objects.requireNonNull(name, "name");
        this.value = value == null ? "" : value;
    }

    public QualifiedName getName() { return name; }
    public String getValue() { return value; }
}
