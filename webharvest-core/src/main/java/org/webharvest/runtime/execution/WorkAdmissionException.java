/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

/** Indicates atomic rejection of a plan before any of its partitions starts. */
public class WorkAdmissionException extends RuntimeException {

    public WorkAdmissionException(String message) {
        super(message);
    }

    public WorkAdmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
