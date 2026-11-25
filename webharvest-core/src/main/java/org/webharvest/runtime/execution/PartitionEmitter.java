/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

/**
 * Synchronous output capability for one partition.
 *
 * <p>Emission is backpressured by the output handler: the call does not return
 * until the handler has processed the item. Implementations must preserve the
 * order emitted by one partition. Ordering between different partitions is
 * unspecified.</p>
 */
@FunctionalInterface
public interface PartitionEmitter<T> {

    void emit(T item) throws Exception;
}
