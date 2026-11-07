package org.webharvest.semantic;

import java.util.Objects;

/** Namespace-aware XML name. Prefix is retained as source information, not identity. */
public final class QualifiedName {
    private final String namespaceUri;
    private final String localName;
    private final String prefix;

    public QualifiedName(String namespaceUri, String localName, String prefix) {
        this.namespaceUri = namespaceUri == null ? "" : namespaceUri;
        this.localName = Objects.requireNonNull(localName, "localName");
        this.prefix = prefix == null ? "" : prefix;
    }

    public String getNamespaceUri() { return namespaceUri; }
    public String getLocalName() { return localName; }
    public String getPrefix() { return prefix; }

    public String expandedName() {
        return "{" + namespaceUri + "}" + localName;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QualifiedName)) return false;
        QualifiedName that = (QualifiedName) other;
        return namespaceUri.equals(that.namespaceUri) && localName.equals(that.localName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespaceUri, localName);
    }

    @Override
    public String toString() {
        return prefix.isEmpty() ? localName : prefix + ":" + localName;
    }
}
