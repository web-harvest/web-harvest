package org.webharvest.plugin.exceptions;

/**
 * Marks an Include failure that must abort the containing configuration.
 *
 * <p>The marker is intentionally distinct from ordinary plugin failures,
 * which retain the executor's historical log-and-continue behavior.</p>
 */
public final class IncludeExecutionException extends RuntimeException {
    public IncludeExecutionException(final String message) {
        super(message);
    }

    public IncludeExecutionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Checks whether a failure is an Include failure, including wrappers added
     * by plugin lifecycle and execution layers.
     *
     * @param failure failure to inspect
     * @return true when the cause chain contains an Include failure
     */
    public static boolean isIncludeFailure(final Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof IncludeExecutionException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Re-throws a wrapped Include failure so container plugins cannot handle it
     * as an ordinary recoverable child failure.
     *
     * @param failure failure to inspect
     */
    public static void rethrowIfIncludeFailure(final Throwable failure) {
        if (isIncludeFailure(failure)) {
            throw new IncludeExecutionException("Fatal Include failure", failure);
        }
    }
}