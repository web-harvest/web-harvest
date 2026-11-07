package org.webharvest.semantic;

import java.util.Objects;

/** Character content. CDATA remains distinguishable from ordinary parsed text. */
public final class SemanticText implements SemanticContent {
    private final String id;
    private final String text;
    private final boolean cdata;
    private final SourceLocation location;

    public SemanticText(String id, String text, boolean cdata, SourceLocation location) {
        this.id = Objects.requireNonNull(id, "id");
        this.text = Objects.requireNonNull(text, "text");
        this.cdata = cdata;
        this.location = location == null ? SourceLocation.unknown() : location;
    }

    @Override public String getId() { return id; }
    public String getText() { return text; }
    public boolean isCdata() { return cdata; }
    @Override public SourceLocation getLocation() { return location; }
}
