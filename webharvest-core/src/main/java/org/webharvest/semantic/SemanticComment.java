package org.webharvest.semantic;

import java.util.Objects;

/** XML comment node. */
public final class SemanticComment implements SemanticContent {
    private final String id;
    private final String text;
    private final SourceLocation location;

    public SemanticComment(String id, String text, SourceLocation location) {
        this.id = Objects.requireNonNull(id, "id");
        this.text = Objects.requireNonNull(text, "text");
        this.location = location == null ? SourceLocation.unknown() : location;
    }

    @Override public String getId() { return id; }
    public String getText() { return text; }
    @Override public SourceLocation getLocation() { return location; }
}
