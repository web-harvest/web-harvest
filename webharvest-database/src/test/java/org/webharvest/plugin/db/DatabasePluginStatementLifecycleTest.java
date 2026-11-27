/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.plugin.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;
import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** Tests owner cancellation and statement timeout at the JDBC operation boundary. */
public class DatabasePluginStatementLifecycleTest {

    @Test
    public void ownerAbortCancelsBlockedStatementAndClosesOnlyAfterLeaseRelease() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        CountDownLatch executeStarted = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenAnswer(invocation -> {
            executeStarted.countDown();
            if (!cancelled.await(5, TimeUnit.SECONDS)) {
                throw new SQLException("Statement cancellation was not observed");
            }
            throw new SQLException("Statement cancelled");
        });
        doAnswer(invocation -> {
            cancelled.countDown();
            return null;
        }).when(statement).cancel();

        TrackingResourceAccess access = new TrackingResourceAccess(statement);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> execution = executor.submit(() -> {
                try {
                    invokeExecuteSql(connection, "UPDATE sample SET value = 1", 0, access);
                    return null;
                } catch (Throwable failure) {
                    return failure;
                }
            });

            assertTrue(executeStarted.await(3, TimeUnit.SECONDS),
                "The statement must enter executeUpdate before cancellation");
            access.abort();

            Throwable failure = execution.get(5, TimeUnit.SECONDS);
            assertNotNull(failure, "A cancelled SQL operation must fail");
            assertTrue(failure instanceof SQLException,
                "The driver cancellation failure must remain observable");
            verify(statement).cancel();
            assertTrue(access.leaseReleased.get(), "The statement lease must be released");
            assertTrue(access.operationClosed.get(), "The statement resource must be closed");
            assertTrue(access.leaseCloseOrder.get() < access.operationCloseOrder.get(),
                "Final statement close must happen after its active operation lease is released");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    public void configuredQueryTimeoutIsAppliedBeforeStatementExecution() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        invokeExecuteSql(connection, "UPDATE sample SET value = 1", 9, null);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(statement);
        order.verify(statement).setQueryTimeout(9);
        order.verify(statement).executeUpdate();
        verify(statement).close();
    }

    @Test
    public void zeroQueryTimeoutPreservesDriverDefault() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        invokeExecuteSql(connection, "UPDATE sample SET value = 1", 0, null);

        org.mockito.Mockito.verify(statement, org.mockito.Mockito.never()).setQueryTimeout(
            org.mockito.ArgumentMatchers.anyInt());
    }

    private static Variable invokeExecuteSql(Connection connection, String sql, int queryTimeoutSeconds,
                                             ExecutionResourceAccess access) throws Throwable {
        Method method = DatabasePlugin.class.getDeclaredMethod(
            "executeSql", Connection.class, String.class, int.class, ExecutionResourceAccess.class);
        method.setAccessible(true);
        try {
            return (Variable) method.invoke(new DatabasePlugin(), connection, sql, queryTimeoutSeconds, access);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static final class TrackingResourceAccess implements ExecutionResourceAccess {
        private final PreparedStatement statement;
        private final AtomicReference<AbortAction> abortAction = new AtomicReference<>();
        private final AtomicInteger sequence = new AtomicInteger();
        private final AtomicInteger leaseCloseOrder = new AtomicInteger();
        private final AtomicInteger operationCloseOrder = new AtomicInteger();
        private final java.util.concurrent.atomic.AtomicBoolean leaseReleased =
            new java.util.concurrent.atomic.AtomicBoolean();
        private final java.util.concurrent.atomic.AtomicBoolean operationClosed =
            new java.util.concurrent.atomic.AtomicBoolean();

        private TrackingResourceAccess(PreparedStatement statement) {
            this.statement = statement;
        }

        @Override
        public org.webharvest.runtime.session.ExecutionOwner getOwner() {
            return null;
        }

        @Override
        public ResourceHandle registerSessionResource(String resourceId, AbortAction abortAction,
                                                       CloseAction closeAction) {
            throw new UnsupportedOperationException("Session resources are not used by this test");
        }

        @Override
        public OperationResource registerOperationResource(String resourceId, AbortAction abort,
                                                            CloseAction close) {
            abortAction.set(abort);
            return new OperationResource() {
                private ResourceState state = ResourceState.OPEN;
                private Throwable closeFailure;

                @Override public String getResourceId() { return resourceId; }
                @Override public Lease acquire() {
                    if (state != ResourceState.OPEN) {
                        throw new IllegalStateException("Statement resource is not open");
                    }
                    return () -> {
                        leaseReleased.set(true);
                        leaseCloseOrder.set(sequence.incrementAndGet());
                    };
                }
                @Override public ResourceState getState() { return state; }
                @Override public int getActiveLeaseCount() { return leaseReleased.get() ? 0 : 1; }
                @Override public Throwable getAbortFailure() { return null; }
                @Override public Throwable getCloseFailure() { return closeFailure; }
                @Override public void close() {
                    if (!leaseReleased.get()) {
                        throw new IllegalStateException("Cannot close a statement with an active lease");
                    }
                    try {
                        close.close();
                        state = ResourceState.CLOSED;
                    } catch (Exception failure) {
                        closeFailure = failure;
                        state = ResourceState.CLOSE_FAILED;
                        throw new IllegalStateException("Statement close failed", failure);
                    } finally {
                        operationClosed.set(true);
                        operationCloseOrder.set(sequence.incrementAndGet());
                    }
                }
            };
        }

        private void abort() throws Exception {
            AbortAction action = abortAction.get();
            if (action == null) {
                throw new IllegalStateException("Statement abort action has not been registered");
            }
            action.abort();
        }
    }
}
