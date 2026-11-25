/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import org.mockito.Mockito;
import org.testng.annotations.Test;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.session.ExecutionOwner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/** Deterministic tests for immutable work descriptors and public execution contracts. */
public class WorkExecutionContractTest {

    @Test(groups = {"unit", "execution"})
    public void testWorkPlanTakesImmutableSnapshotAndPreservesPartitionOrder() {
        List<WorkPartition<String>> partitions = new ArrayList<>(Arrays.asList(
            new WorkPartition<>("north", "range-1"),
            new WorkPartition<>("south", "range-2")));

        WorkPlan<String> plan = new WorkPlan<>("read-1", partitions);
        partitions.clear();

        assertEquals(plan.getPlanId(), "read-1");
        assertEquals(plan.getPartitionCount(), 2);
        assertEquals(plan.getPartitions().get(0).getPartitionId(), "north");
        assertEquals(plan.getPartitions().get(1).getPartitionId(), "south");
        assertUnsupportedOperation(() -> plan.getPartitions().clear());
    }

    @Test(groups = {"unit", "execution"}, expectedExceptions = IllegalArgumentException.class)
    public void testWorkPlanRejectsDuplicatePartitionIds() {
        new WorkPlan<>("duplicate", Arrays.asList(
            new WorkPartition<>("same", "first"),
            new WorkPartition<>("same", "second")));
    }

    @Test(groups = {"unit", "execution"}, expectedExceptions = IllegalArgumentException.class)
    public void testWorkPlanRejectsEmptyPartitionList() {
        new WorkPlan<>("empty", Collections.emptyList());
    }

    @Test(groups = {"unit", "execution"}, expectedExceptions = IllegalArgumentException.class)
    public void testWorkExecutionPolicyRejectsUnboundedOrInvalidConcurrency() {
        new WorkExecutionPolicy(0, WorkFailurePolicy.CONTINUE);
    }

    @Test(groups = {"unit", "execution"})
    public void testWorkResultReportsPartialSuccessWithoutImplyingAtomicity() {
        WorkResult<String> result = new WorkResult<>(Arrays.asList(
            new PartitionOutcome<>("p1", PartitionOutcome.Status.SUCCEEDED, "written-1", null),
            new PartitionOutcome<>("p2", PartitionOutcome.Status.FAILED, null,
                new IllegalStateException("partition failed")),
            new PartitionOutcome<>("p3", PartitionOutcome.Status.NOT_STARTED, null, null)));

        assertEquals(result.getStatus(), WorkResult.Status.PARTIALLY_SUCCEEDED);
        assertEquals(result.getOutcomes().size(), 3);
        assertEquals(result.getOutcomes().get(0).getResult(), "written-1");
        assertEquals(result.getOutcomes().get(1).getStatus(), PartitionOutcome.Status.FAILED);
        assertEquals(result.getOutcomes().get(2).getStatus(), PartitionOutcome.Status.NOT_STARTED);
        assertUnsupportedOperation(() -> result.getOutcomes().clear());
    }

    @Test(groups = {"unit", "execution"})
    public void testScraperContextBindsWorkCapabilityOnlyForMatchingOwner() {
        ExecutionOwner owner = new ExecutionOwner("runtime", "session", 1);
        ExecutionWorkAccess workAccess = Mockito.mock(ExecutionWorkAccess.class);
        Mockito.when(workAccess.getOwner()).thenReturn(owner);
        ScraperContext context = new ScraperContext();

        context.setExecutionOwner(owner);
        context.setExecutionWorkAccess(workAccess);

        assertSame(context.getExecutionWorkAccess(), workAccess);
    }

    @Test(groups = {"unit", "execution"}, expectedExceptions = IllegalArgumentException.class)
    public void testScraperContextRejectsWorkCapabilityFromDifferentOwner() {
        ExecutionOwner owner = new ExecutionOwner("runtime", "session", 1);
        ExecutionWorkAccess workAccess = Mockito.mock(ExecutionWorkAccess.class);
        Mockito.when(workAccess.getOwner()).thenReturn(
            new ExecutionOwner("runtime", "another-session", 2));

        ScraperContext context = new ScraperContext();
        context.setExecutionOwner(owner);
        context.setExecutionWorkAccess(workAccess);
    }

    @Test(groups = {"unit", "execution"}, expectedExceptions = IllegalStateException.class)
    public void testScraperContextRejectsWorkCapabilityRebinding() {
        ExecutionOwner owner = new ExecutionOwner("runtime", "session", 1);
        ExecutionWorkAccess workAccess = Mockito.mock(ExecutionWorkAccess.class);
        Mockito.when(workAccess.getOwner()).thenReturn(owner);
        ScraperContext context = new ScraperContext();

        context.setExecutionOwner(owner);
        context.setExecutionWorkAccess(workAccess);
        context.setExecutionWorkAccess(workAccess);
    }

    private void assertUnsupportedOperation(Runnable action) {
        try {
            action.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // Expected: the public collection view is immutable.
        }
    }
}
