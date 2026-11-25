/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable finite plan of uniquely identified partitions. */
public final class WorkPlan<P> {

    private final String planId;
    private final List<WorkPartition<P>> partitions;

    public WorkPlan(String planId, List<WorkPartition<P>> partitions) {
        if (planId == null || planId.trim().isEmpty()) {
            throw new IllegalArgumentException("Plan ID cannot be null or empty");
        }
        Objects.requireNonNull(partitions, "Partitions cannot be null");
        if (partitions.isEmpty()) {
            throw new IllegalArgumentException("A work plan must contain at least one partition");
        }

        List<WorkPartition<P>> snapshot = List.copyOf(partitions);
        Set<String> ids = new HashSet<>();
        for (WorkPartition<P> partition : snapshot) {
            Objects.requireNonNull(partition, "A work partition cannot be null");
            if (!ids.add(partition.getPartitionId())) {
                throw new IllegalArgumentException(
                    "Duplicate partition ID in work plan: " + partition.getPartitionId());
            }
        }
        this.planId = planId;
        this.partitions = Collections.unmodifiableList(snapshot);
    }

    public String getPlanId() {
        return planId;
    }

    public List<WorkPartition<P>> getPartitions() {
        return partitions;
    }

    public int getPartitionCount() {
        return partitions.size();
    }
}
