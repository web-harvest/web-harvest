package org.webharvest.semantic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, ordered inspection graph. This is deliberately not the final
 * canonical configuration model and carries no runtime execution objects.
 *
 * <p>Public construction is supported so non-XML adapters may build the same
 * immutable graph. A constructed instance is <strong>not</strong> proof that the
 * content was produced by the hardened XML analyzer or that any XML validation
 * or security policy was applied.</p>
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
        Objects.requireNonNull(content, "content");
        this.root = Objects.requireNonNull(root, "root");
        this.content = List.copyOf(content);
        boolean rootInContent = false;
        for (SemanticContent item : this.content) {
            if (item == this.root) {
                rootInContent = true;
                break;
            }
        }
        if (!rootInContent) {
            throw new IllegalArgumentException("content must contain the same root element instance");
        }
    }

    public SourceOrigin getOrigin() { return origin; }
    public String getSourceXml() { return sourceXml; }
    public List<SemanticContent> getContent() { return content; }
    public SemanticElement getRoot() { return root; }
    public Optional<SemanticElement> rootElement() { return Optional.of(root); }
}
