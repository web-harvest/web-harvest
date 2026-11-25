/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

/** Adapter callback for a partition that emits a bounded stream of output items. */
@FunctionalInterface
public interface StreamingPartitionTask<P, T, R> {

    R execute(WorkPartition<P> partition,
              PartitionContext context,
              PartitionEmitter<T> output) throws Exception;
}
