package org.webharvest.semantic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, ordered XML inspection graph. This is deliberately not the final
 * canonical configuration model and carries no runtime execution objects.
 */
public final class SemanticDocument {
    private final SourceOrigin origin;
    private final String sourceXml;
    private final List<SemanticContent> content;
    private final SemanticElement root;

    public SemanticDocument(SourceOrigin origin, String sourceXml, List<SemanticContent> content,
            SemanticElement root) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sourceXml = Objects.requireNonNull(sourceXml, "sourceXml");
        this.content = List.copyOf(content);
        this.root = Objects.requireNonNull(root, "root");
    }

    public SourceOrigin getOrigin() { return origin; }
    public String getSourceXml() { return sourceXml; }
    public List<SemanticContent> getContent() { return content; }
    public SemanticElement getRoot() { return root; }
    public Optional<SemanticElement> rootElement() { return Optional.of(root); }
}
