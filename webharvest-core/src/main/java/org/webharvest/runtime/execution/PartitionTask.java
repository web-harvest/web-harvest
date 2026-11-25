/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

/** Adapter callback executed for one admitted partition. */
@FunctionalInterface
public interface PartitionTask<P, R> {

    R execute(WorkPartition<P> partition, PartitionContext context) throws Exception;
}
