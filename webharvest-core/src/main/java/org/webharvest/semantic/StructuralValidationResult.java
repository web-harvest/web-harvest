package org.webharvest.semantic;

import java.util.List;
import java.util.Objects;

/**
 * Immutable result of {@link SemanticModel#validateStructural(SemanticDocument)}.
 *
 * <p>Holds the same {@link SemanticDocument} instance that was validated (never a
 * copy). Success means no diagnostic has {@link SemanticDiagnostic.Severity#ERROR}.
 */
public final class StructuralValidationResult {
    private final SemanticDocument document;
    private final List<SemanticDiagnostic> diagnostics;
    private final int errorCount;
    private final int warningCount;
    private final int infoCount;

    StructuralValidationResult(SemanticDocument document, List<SemanticDiagnostic> diagnostics) {
        this.document = Objects.requireNonNull(document, "document");
        this.diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        int errors = 0;
        int warnings = 0;
        int infos = 0;
        for (SemanticDiagnostic diagnostic : this.diagnostics) {
            switch (diagnostic.getSeverity()) {
                case ERROR:
                    errors++;
                    break;
                case WARNING:
                    warnings++;
                    break;
                case INFO:
                    infos++;
                    break;
                default:
                    break;
            }
        }
        this.errorCount = errors;
        this.warningCount = warnings;
        this.infoCount = infos;
    }

    /** The exact document instance supplied to {@code validateStructural}. */
    public SemanticDocument getDocument() {
        return document;
    }

    public List<SemanticDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    /** {@code true} when no diagnostic has severity {@code ERROR}. */
    public boolean isSuccessful() {
        return errorCount == 0;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }
}
