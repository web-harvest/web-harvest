/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

/** Receives streamed partition output; callbacks are serialized by the runtime. */
@FunctionalInterface
public interface PartitionOutputHandler<T> {

    void onOutput(String partitionId, T item) throws Exception;
}
