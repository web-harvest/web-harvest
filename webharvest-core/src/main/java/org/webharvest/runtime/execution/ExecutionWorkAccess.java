/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import org.webharvest.runtime.session.ExecutionOwner;

/**
 * Optional capability for submitting bounded partitioned work within one
 * service-managed execution. It does not expose an executor or thread creation.
 */
public interface ExecutionWorkAccess {

    /** Returns the immutable session owner to which all submitted child work belongs. */
    ExecutionOwner getOwner();

    /**
     * Atomically admits a complete finite plan or rejects it before any partition starts.
     *
     * @throws WorkAdmissionException when runtime, session or plan admission limits are reached
     */
    <P, R> WorkExecution<R> submit(
        WorkPlan<P> plan,
        WorkExecutionPolicy policy,
        PartitionTask<P, R> task);

    /**
     * Atomically admits a streaming plan or rejects it before any partition starts.
     *
     * <p>Output callbacks are serialized and execute on the producing worker. A slow
     * callback provides synchronous backpressure. The callback must not wait for this
     * work execution to complete.</p>
     *
     * @throws WorkAdmissionException when runtime, session or plan admission limits are reached
     */
    <P, T, R> WorkExecution<R> submitStreaming(
        WorkPlan<P> plan,
        WorkExecutionPolicy policy,
        StreamingPartitionTask<P, T, R> task,
        PartitionOutputHandler<T> outputHandler);
}
