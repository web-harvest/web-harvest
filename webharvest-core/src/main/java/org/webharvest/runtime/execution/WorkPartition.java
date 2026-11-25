/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.Objects;

/** Immutable identity and adapter-owned input for one logical unit of work. */
public final class WorkPartition<P> {

    private final String partitionId;
    private final P payload;

    public WorkPartition(String partitionId, P payload) {
        if (partitionId == null || partitionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Partition ID cannot be null or empty");
        }
        this.partitionId = partitionId;
        this.payload = Objects.requireNonNull(payload, "Partition payload cannot be null");
    }

    public String getPartitionId() {
        return partitionId;
    }

    /**
     * Returns the adapter-owned payload.
     *
     * <p>The caller must supply an immutable payload or otherwise guarantee that
     * it is not mutated while the plan is executing. Core does not deep-copy it.</p>
     */
    public P getPayload() {
        return payload;
    }
}
