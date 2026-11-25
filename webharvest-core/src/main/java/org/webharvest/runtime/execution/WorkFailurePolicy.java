/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

/** Controls whether independent partitions continue after a partition fails. */
public enum WorkFailurePolicy {
    /** Stop admitting pending partitions after the first failure; let running work exit. */
    FAIL_FAST,

    /** Continue admitting remaining partitions and report every partition outcome. */
    CONTINUE
}
