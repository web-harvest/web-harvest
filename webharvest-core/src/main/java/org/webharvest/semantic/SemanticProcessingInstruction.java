package org.webharvest.semantic;

import java.util.Objects;

/** XML processing instruction node. */
public final class SemanticProcessingInstruction implements SemanticContent {
    private final String id;
    private final String target;
    private final String data;
    private final SourceLocation location;

    public SemanticProcessingInstruction(String id, String target, String data, SourceLocation location) {
        this.id = Objects.requireNonNull(id, "id");
        this.target = Objects.requireNonNull(target, "target");
        this.data = data == null ? "" : data;
        this.location = location == null ? SourceLocation.unknown() : location;
    }

    @Override public String getId() { return id; }
    public String getTarget() { return target; }
    public String getData() { return data; }
    @Override public SourceLocation getLocation() { return location; }
}
