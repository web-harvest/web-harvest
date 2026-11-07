package org.webharvest.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Core-owned, read-only semantic inspection façade. Stage 1 accepts XML only;
 * it does not execute, edit, normalize or export configurations.
 */
public final class SemanticModel {
    private final SemanticXmlAnalyzer analyzer;

    public SemanticModel() {
        this(new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults()));
    }

    SemanticModel(SemanticXmlAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    }

    public SemanticLoadResult load(String xml, SourceOrigin origin) {
        return analyzer.load(xml, origin == null ? SourceOrigin.unknown() : origin);
    }

    public InspectionView inspect(SemanticDocument document) {
        Objects.requireNonNull(document, "document");
        int[] counts = new int[3];
        Deque<SemanticContent> pending = new ArrayDeque<>(document.getContent());
        while (!pending.isEmpty()) {
            SemanticContent item = pending.removeFirst();
            if (item instanceof SemanticElement) {
                SemanticElement element = (SemanticElement) item;
                counts[0]++;
                counts[1] += element.getAttributes().size();
                for (SemanticContent child : element.getContent()) {
                    if (child instanceof SemanticText) counts[2]++;
                    if (child instanceof SemanticElement) pending.addLast(child);
                }
            }
        }
        return new InspectionView(document, counts[0], counts[1], counts[2]);
    }
}
