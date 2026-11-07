package org.webharvest.semantic;

import java.util.List;
import java.util.Objects;

/** Immutable XML element in the Stage 1 inspection graph. */
public final class SemanticElement implements SemanticContent {
    private final String id;
    private final QualifiedName name;
    private final List<SemanticAttribute> attributes;
    private final List<SemanticContent> content;
    private final SourceLocation location;

    public SemanticElement(String id, QualifiedName name, List<SemanticAttribute> attributes,
            List<SemanticContent> content, SourceLocation location) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.attributes = List.copyOf(attributes);
        this.content = List.copyOf(content);
        this.location = location == null ? SourceLocation.unknown() : location;
    }

    @Override public String getId() { return id; }
    public QualifiedName getName() { return name; }
    public List<SemanticAttribute> getAttributes() { return attributes; }
    public List<SemanticContent> getContent() { return content; }
    @Override public SourceLocation getLocation() { return location; }
}
