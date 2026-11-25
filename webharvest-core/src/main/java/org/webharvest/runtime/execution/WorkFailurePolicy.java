/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

/** Controls scheduling of not-yet-started partitions after a partition fails. */
public enum WorkFailurePolicy {
    /**
     * Stop scheduling partitions that have not started after the first failure.
     * Report each such partition as NOT_STARTED; already-started partitions are
     * allowed to reach their own terminal outcomes.
     */
    FAIL_FAST,

    /** Schedule all admitted partitions despite earlier failures and report every outcome. */
    CONTINUE
}
