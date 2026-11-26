/*
 * Copyright (c) 2006-2025 the original author or authors.
 */
package org.webharvest.runtime.session;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Contract tests for immutable runtime execution ownership. */
public class ExecutionOwnerTest {

    @Test(groups = {"unit", "runtime"})
    public void testIdentityValueEqualityAndAccessors() {
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 7L);
        ExecutionOwner equal = new ExecutionOwner("runtime-a", "session-a", 7L);

        assertEquals(owner.getRuntimeId(), "runtime-a");
        assertEquals(owner.getSessionId(), "session-a");
        assertEquals(owner.getExecutionGeneration(), 7L);
        assertSame(owner, owner);
        assertEquals(owner, equal);
        assertEquals(owner.hashCode(), equal.hashCode());
        assertTrue(owner.toString().contains("runtime-a"));
        assertFalse(owner.equals(null));
        assertFalse(owner.equals("not-an-owner"));
    }

    @Test(groups = {"unit", "runtime"})
    public void testEachIdentityDimensionParticipatesInEquality() {
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1L);

        assertFalse(owner.equals(new ExecutionOwner("runtime-b", "session-a", 1L)));
        assertFalse(owner.equals(new ExecutionOwner("runtime-a", "session-b", 1L)));
        assertFalse(owner.equals(new ExecutionOwner("runtime-a", "session-a", 2L)));
    }

    @Test(groups = {"unit", "runtime"})
    public void testConstructorRejectsInvalidRuntimeIdentity() {
        expectIllegalArgument(() -> new ExecutionOwner(null, "session-a", 1L));
        expectIllegalArgument(() -> new ExecutionOwner("  ", "session-a", 1L));
    }

    @Test(groups = {"unit", "runtime"})
    public void testConstructorRejectsInvalidSessionIdentity() {
        expectIllegalArgument(() -> new ExecutionOwner("runtime-a", null, 1L));
        expectIllegalArgument(() -> new ExecutionOwner("runtime-a", "", 1L));
    }

    @Test(groups = {"unit", "runtime"})
    public void testConstructorRejectsNonPositiveGeneration() {
        expectIllegalArgument(() -> new ExecutionOwner("runtime-a", "session-a", 0L));
    }

    private static void expectIllegalArgument(ThrowingRunnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        } catch (Exception unexpected) {
            throw new AssertionError("Expected IllegalArgumentException", unexpected);
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
