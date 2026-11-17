package org.webharvest.plugin.exceptions;

/**
 * Marks an Include failure that must abort the containing configuration.
 *
 * <p>The marker is intentionally distinct from ordinary plugin failures,
 * which retain the executor's historical log-and-continue behavior.</p>
 */
public final class IncludeExecutionException extends RuntimeException {
    public IncludeExecutionException(String message) {
        super(message);
    }

    public IncludeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}