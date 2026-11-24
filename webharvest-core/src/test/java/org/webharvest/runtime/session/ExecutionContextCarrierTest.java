/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer, are included in all copies.
 */

package org.webharvest.runtime.session;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ExecutionContextCarrierTest {

    @Test
    public void nestedScopesRestoreParentAndRemoveThreadLocalAtEmptyStack() {
        ExecutionIdentity outer = new ExecutionIdentity("runtime-a", "session-a", 1);
        ExecutionIdentity inner = new ExecutionIdentity("runtime-b", "session-b", 2);

        assertFalse(ExecutionContextCarrier.currentIdentity().isPresent());
        try (ExecutionContextCarrier.ScopeToken outerScope = ExecutionContextCarrier.install(outer)) {
            assertEquals(ExecutionContextCarrier.currentIdentity().get(), outer);
            try (ExecutionContextCarrier.ScopeToken innerScope = ExecutionContextCarrier.install(inner)) {
                assertEquals(ExecutionContextCarrier.currentIdentity().get(), inner);
            }
            assertEquals(ExecutionContextCarrier.currentIdentity().get(), outer);
        }
        assertFalse(ExecutionContextCarrier.currentIdentity().isPresent());
    }

    @Test
    public void capturedTokenCanBeInstalledOnWorkerAndThreadReuseIsClean() throws Exception {
        ExecutionIdentity identity = new ExecutionIdentity("runtime-a", "session-a", 7);
        ExecutionContextCarrier.ContextToken token;
        try (ExecutionContextCarrier.ScopeToken ignored = ExecutionContextCarrier.install(identity)) {
            token = ExecutionContextCarrier.capture();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertEquals(executor.submit(() -> {
                try (ExecutionContextCarrier.ScopeToken ignored = token.install()) {
                    return ExecutionContextCarrier.currentIdentity().get();
                }
            }).get(5, TimeUnit.SECONDS), identity);

            assertFalse(executor.submit(() -> ExecutionContextCarrier.currentIdentity().isPresent())
                .get(5, TimeUnit.SECONDS), "A reused worker thread must not retain the previous identity");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void scopeIsRemovedWhenCallableThrows() {
        ExecutionIdentity identity = new ExecutionIdentity("runtime-a", "session-a", 3);
        try {
            try (ExecutionContextCarrier.ScopeToken ignored = ExecutionContextCarrier.install(identity)) {
                throw new IllegalStateException("expected");
            }
        } catch (IllegalStateException expected) {
            assertEquals(expected.getMessage(), "expected");
        }
        assertFalse(ExecutionContextCarrier.currentIdentity().isPresent());
    }

    @Test
    public void outOfOrderCloseIsRejectedWithoutCorruptingStack() {
        ExecutionIdentity outer = new ExecutionIdentity("runtime-a", "session-a", 1);
        ExecutionIdentity inner = new ExecutionIdentity("runtime-a", "session-a", 2);
        ExecutionContextCarrier.ScopeToken outerScope = ExecutionContextCarrier.install(outer);
        ExecutionContextCarrier.ScopeToken innerScope = ExecutionContextCarrier.install(inner);

        try {
            outerScope.close();
            fail("Out-of-order close must be rejected");
        } catch (IllegalStateException expected) {
            assertEquals(ExecutionContextCarrier.currentIdentity().get(), inner);
        }

        innerScope.close();
        outerScope.close();
        assertFalse(ExecutionContextCarrier.currentIdentity().isPresent());
    }

    @Test
    public void scopeCannotBeClosedFromAnotherThread() throws Exception {
        ExecutionIdentity identity = new ExecutionIdentity("runtime-a", "session-a", 4);
        ExecutionContextCarrier.ScopeToken scope = ExecutionContextCarrier.install(identity);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            try {
                executor.submit(scope::close).get(5, TimeUnit.SECONDS);
                fail("A scope must be closed by its owner thread");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IllegalStateException);
            }
        } finally {
            executor.shutdownNow();
            scope.close();
        }
        assertFalse(ExecutionContextCarrier.currentIdentity().isPresent());
    }
}
