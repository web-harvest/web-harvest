package org.webharvest.semantic;

import java.util.Objects;

/** Read-only inspection projection over the exact document instance supplied by the caller. */
public final class InspectionView {
    private final SemanticDocument document;
    private final int elementCount;
    private final int attributeCount;
    private final int textNodeCount;

    InspectionView(SemanticDocument document, int elementCount, int attributeCount, int textNodeCount) {
        this.document = Objects.requireNonNull(document, "document");
        this.elementCount = elementCount;
        this.attributeCount = attributeCount;
        this.textNodeCount = textNodeCount;
    }

    public SemanticDocument getDocument() { return document; }
    public int getElementCount() { return elementCount; }
    public int getAttributeCount() { return attributeCount; }

    /**
     * Number of {@link SemanticText} nodes in the entire document graph, including
     * document-level text outside the root element. CDATA counts; comments and
     * processing instructions do not.
     */
    public int getTextNodeCount() { return textNodeCount; }
}
