package org.webharvest.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import org.webharvest.semantic.internal.SemanticXmlAnalyzer;

/**
 * Core-owned, read-only semantic inspection façade. Stage 1 accepts XML only;
 * it does not execute, edit, normalize or export configurations.
 *
 * <p>A {@link PluginDescriptorIndex} may be supplied for structural validation.
 * The default constructor builds the index from
 * {@link PluginDescriptorIndex#fromCatalog()} <strong>lazily</strong> on first
 * {@link #getPluginDescriptorIndex()} access so load/inspect stay light when the
 * catalog is unused.
 */
public final class SemanticModel {
    private final SemanticXmlAnalyzer analyzer;
    private final PluginDescriptorIndex providedIndex;
    private volatile PluginDescriptorIndex lazyIndex;

    public SemanticModel() {
        this(new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults()), null);
    }

    /**
     * Uses the given descriptor index; catalog is not loaded unless the caller
     * already built the index from it.
     */
    public SemanticModel(PluginDescriptorIndex index) {
        this(new SemanticXmlAnalyzer(SemanticXmlAnalyzer.Limits.defaults()), index);
    }

    SemanticModel(SemanticXmlAnalyzer analyzer) {
        this(analyzer, null);
    }

    /**
     * Package/test hook combining a custom analyzer with an explicit index
     * (or {@code null} for lazy catalog indexing).
     */
    SemanticModel(SemanticXmlAnalyzer analyzer, PluginDescriptorIndex index) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
        this.providedIndex = index;
    }

    public SemanticLoadResult load(String xml, SourceOrigin origin) {
        return analyzer.load(xml, origin == null ? SourceOrigin.unknown() : origin);
    }

    /**
     * Returns the plugin descriptor index held for later structural validation.
     *
     * <p>When constructed without an explicit index, builds
     * {@link PluginDescriptorIndex#fromCatalog()} once on first access (lazy).
     */
    public PluginDescriptorIndex getPluginDescriptorIndex() {
        if (providedIndex != null) {
            return providedIndex;
        }
        PluginDescriptorIndex local = lazyIndex;
        if (local == null) {
            synchronized (this) {
                local = lazyIndex;
                if (local == null) {
                    lazyIndex = local = PluginDescriptorIndex.fromCatalog();
                }
            }
        }
        return local;
    }

    /**
     * Builds a read-only inspection projection over {@code document}.
     *
     * <p>{@link InspectionView#getTextNodeCount()} counts every {@link SemanticText}
     * in the entire document graph, including document-level text outside the root
     * element when present in {@link SemanticDocument#getContent()}. CDATA counts as
     * text; comments and processing instructions do not.</p>
     */
    public InspectionView inspect(SemanticDocument document) {
        Objects.requireNonNull(document, "document");
        int elementCount = 0;
        int attributeCount = 0;
        int textNodeCount = 0;
        Deque<SemanticContent> pending = new ArrayDeque<>(document.getContent());
        while (!pending.isEmpty()) {
            SemanticContent item = pending.removeFirst();
            if (item instanceof SemanticText) {
                textNodeCount++;
            } else if (item instanceof SemanticElement) {
                SemanticElement element = (SemanticElement) item;
                elementCount++;
                attributeCount += element.getAttributes().size();
                for (SemanticContent child : element.getContent()) {
                    pending.addLast(child);
                }
            }
        }
        return new InspectionView(document, elementCount, attributeCount, textNodeCount);
    }

    /**
     * Validates structural constraints of {@code document} against
     * {@link #getPluginDescriptorIndex()}.
     *
     * <p>Returns a result that holds the <strong>same</strong> document instance
     * (suitable for {@code assertSame} with {@link #inspect}). Does not mutate the
     * document, invoke runtime dispatch, or consult the IDE schema validator.
     *
     * @throws NullPointerException if {@code document} is null
     */
    public StructuralValidationResult validateStructural(SemanticDocument document) {
        Objects.requireNonNull(document, "document");
        return StructuralValidator.validate(document, getPluginDescriptorIndex());
    }
}
