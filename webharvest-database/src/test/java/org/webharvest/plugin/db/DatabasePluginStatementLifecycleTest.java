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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/** Tests owner cancellation and statement timeout at the JDBC operation boundary. */
public class DatabasePluginStatementLifecycleTest {

    @Test
    public void ownerAbortCancelsBlockedStatementAndClosesOnlyAfterLeaseRelease() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        CountDownLatch executeStarted = new CountDownLatch(1);
        CountDownLatch cancelStarted = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        CountDownLatch releaseCancel = new CountDownLatch(1);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenAnswer(invocation -> {
            executeStarted.countDown();
            if (!cancelled.await(5, TimeUnit.SECONDS)) {
                throw new SQLException("Statement cancellation was not observed");
            }
            throw new SQLException("Statement cancelled");
        });
        doAnswer(invocation -> {
            cancelStarted.countDown();
            cancelled.countDown();
            if (!releaseCancel.await(5, TimeUnit.SECONDS)) {
                throw new SQLException("Test did not release the simulated slow cancel callback");
            }
            return null;
        }).when(statement).cancel();

        TrackingResourceAccess access = new TrackingResourceAccess();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean interruptedAfterCleanup =
            new java.util.concurrent.atomic.AtomicBoolean();
        try {
            Future<Throwable> execution = executor.submit(() -> {
                workerThread.set(Thread.currentThread());
                try {
                    invokeExecuteSql(connection, "UPDATE sample SET value = 1", 0, access);
                    return null;
                } catch (Throwable failure) {
                    return failure;
                } finally {
                    interruptedAfterCleanup.set(Thread.currentThread().isInterrupted());
                }
            });

            assertTrue(executeStarted.await(3, TimeUnit.SECONDS),
                "The statement must enter executeUpdate before cancellation");
            Future<?> abort = executor.submit(() -> {
                try {
                    access.abort();
                } catch (Exception failure) {
                    throw new IllegalStateException(failure);
                }
            });
            assertTrue(cancelStarted.await(3, TimeUnit.SECONDS),
                "The asynchronous owner abort callback must enter Statement.cancel()");
            assertTrue(access.operationCloseRequested.await(3, TimeUnit.SECONDS),
                "The worker must request operation close after releasing its statement lease");
            assertTrue(!execution.isDone(),
                "The connection-owning worker must wait while the abort callback is still active");
            org.mockito.Mockito.verify(statement, org.mockito.Mockito.never()).close();

            workerThread.get().interrupt();
            releaseCancel.countDown();
            abort.get(3, TimeUnit.SECONDS);
            Throwable failure = execution.get(5, TimeUnit.SECONDS);
            assertNotNull(failure, "A cancelled SQL operation must fail");
            assertTrue(failure instanceof SQLException,
                "The driver cancellation failure must remain observable");
            verify(statement).cancel();
            assertTrue(access.leaseReleased.get(), "The statement lease must be released");
            assertTrue(access.operationClosed.get(), "The statement resource must be closed");
            assertTrue(access.leaseCloseOrder.get() < access.operationCloseOrder.get(),
                "Final statement close must happen after its active operation lease is released");
            assertTrue(interruptedAfterCleanup.get(),
                "The statement-close wait must restore interruption after asynchronous abort quiescence");
        } finally {
            releaseCancel.countDown();
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

    @Test
    public void queryTimeoutParserPreservesDriverDefaultAndRejectsInvalidValues() throws Throwable {
        Method method = DatabasePlugin.class.getDeclaredMethod("parseQueryTimeout", String.class);
        method.setAccessible(true);
        DatabasePlugin plugin = new DatabasePlugin();

        assertEquals(method.invoke(plugin, new Object[] { null }), 0);
        assertEquals(method.invoke(plugin, "   "), 0);
        assertEquals(method.invoke(plugin, "0"), 0);
        assertEquals(method.invoke(plugin, "12"), 12);

        assertQueryTimeoutRejected(method, plugin, "-1");
        assertQueryTimeoutRejected(method, plugin, "invalid");
    }

    @Test
    public void managedStatementCloseFailureRemainsObservable() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        SQLException closeFailure = new SQLException("managed statement close failed");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        doThrow(closeFailure).when(statement).close();

        try {
            invokeExecuteSql(connection, "UPDATE sample SET value = 1", 0,
                new TrackingResourceAccess());
            fail("A managed statement close failure must remain observable");
        } catch (SQLException expected) {
            assertTrue(expected.getSuppressed().length > 0,
                "The resource-close failure must remain attached to the surfaced cleanup failure");
            assertSame(expected.getSuppressed()[0].getCause(), closeFailure);
        }
    }

    private static void assertQueryTimeoutRejected(Method method, DatabasePlugin plugin, String value)
            throws Exception {
        try {
            method.invoke(plugin, value);
            fail("Invalid queryTimeout must be rejected: " + value);
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
        }
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
        private final AtomicReference<AbortAction> abortAction = new AtomicReference<>();
        private final AtomicInteger sequence = new AtomicInteger();
        private final AtomicInteger leaseCloseOrder = new AtomicInteger();
        private final AtomicInteger operationCloseOrder = new AtomicInteger();
        private final java.util.concurrent.atomic.AtomicBoolean leaseReleased =
            new java.util.concurrent.atomic.AtomicBoolean();
        private final java.util.concurrent.atomic.AtomicBoolean operationClosed =
            new java.util.concurrent.atomic.AtomicBoolean();

        private TrackingResourceAccess(PreparedStatement statement) {
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
                    operationCloseRequested.countDown();
                    if (!leaseReleased.get()) {
                        throw new IllegalStateException("Cannot close a statement with an active lease");
                    }
                    awaitAbortCompletion();
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
            try {
                action.abort();
            } finally {
                abortCompleted.countDown();
            }
        }

        private void awaitAbortCompletion() {
            boolean interrupted = false;
            while (true) {
                try {
                    abortCompleted.await();
                    break;
                } catch (InterruptedException interruption) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
