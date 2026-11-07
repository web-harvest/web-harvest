package org.webharvest.semantic;

import java.util.Objects;

/** A stable, machine-readable diagnostic emitted while loading XML. */
public final class SemanticDiagnostic {
    public enum Severity { INFO, WARNING, ERROR }

    private final String code;
    private final Severity severity;
    private final String message;
    private final SourceLocation location;

    public SemanticDiagnostic(String code, Severity severity, String message, SourceLocation location) {
        this.code = Objects.requireNonNull(code, "code");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.message = Objects.requireNonNull(message, "message");
        this.location = location == null ? SourceLocation.unknown() : location;
    }

    public String getCode() { return code; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public SourceLocation getLocation() { return location; }
}
