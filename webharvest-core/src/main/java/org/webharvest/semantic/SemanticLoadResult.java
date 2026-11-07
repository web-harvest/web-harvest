package org.webharvest.semantic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Result of a fail-closed semantic XML load. */
public final class SemanticLoadResult {
    private final SourceOrigin origin;
    private final String sourceXml;
    private final SemanticDocument document;
    private final List<SemanticDiagnostic> diagnostics;

    public SemanticLoadResult(SourceOrigin origin, String sourceXml, SemanticDocument document,
            List<SemanticDiagnostic> diagnostics) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sourceXml = sourceXml;
        this.document = document;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public SourceOrigin getOrigin() { return origin; }
    public Optional<String> getSourceXml() { return Optional.ofNullable(sourceXml); }
    public Optional<SemanticDocument> getDocument() { return Optional.ofNullable(document); }
    public List<SemanticDiagnostic> getDiagnostics() { return diagnostics; }
    public boolean isSuccessful() { return document != null; }
}
