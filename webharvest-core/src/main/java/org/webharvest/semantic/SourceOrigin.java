package org.webharvest.semantic;

import java.util.Objects;

/** Identifies where the source XML came from without coupling the model to a loader. */
public final class SourceOrigin {
    private final String value;

    public SourceOrigin(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static SourceOrigin unknown() {
        return new SourceOrigin("unknown");
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SourceOrigin && value.equals(((SourceOrigin) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
