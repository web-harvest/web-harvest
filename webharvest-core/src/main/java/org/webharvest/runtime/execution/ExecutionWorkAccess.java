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
     * Atomically admits the complete finite plan or rejects it before any partition starts.
     *
     * <p>Plan admission is all-or-nothing; after admission, the scheduler may start
     * partitions incrementally within the effective concurrency limit. The
     * {@link WorkFailurePolicy} controls scheduling after admission: FAIL_FAST stops
     * starting not-yet-started partitions after the first failure, and those partitions
     * are reported as NOT_STARTED.</p>
     *
     * @throws WorkAdmissionException when runtime, session or plan admission limits are reached
     */
    <P, R> WorkExecution<R> submit(
        WorkPlan<P> plan,
        WorkExecutionPolicy policy,
        PartitionTask<P, R> task);

    /**
     * Atomically admits the complete streaming plan or rejects it before any partition starts.
     *
     * <p>Output callbacks are serialized and execute on the producing worker. A slow
     * callback provides synchronous backpressure. If the handler throws, that exception
     * fails the emitting partition and is reflected in its terminal outcome; the selected
     * failure policy then controls scheduling of not-yet-started partitions. The callback
     * must not wait for this work execution to complete. Plan admission is all-or-nothing;
     * FAIL_FAST controls scheduling after admission, not plan admission.</p>
     *
     * @throws WorkAdmissionException when runtime, session or plan admission limits are reached
     */
    <P, T, R> WorkExecution<R> submitStreaming(
        WorkPlan<P> plan,
        WorkExecutionPolicy policy,
        StreamingPartitionTask<P, T, R> task,
        PartitionOutputHandler<T> outputHandler);
}
