/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;
import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.resource.LedgerExecutionResourceCoordinator;
import org.webharvest.runtime.resource.ResourceLedger;
import org.webharvest.runtime.session.ExecutionOwner;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Focused tests for JDBC statement/result-set cleanup and failure precedence.
 */
public class DatabasePluginJdbcCleanupTest {

    @Test
    public void prepareFailureDoesNotAttemptStatementCleanup() throws Throwable {
        Connection connection = mock(Connection.class);
        SQLException failure = new SQLException("prepare failed");
        when(connection.prepareStatement("SELECT 1")).thenThrow(failure);

        assertSame(invokeExecuteSqlFailure(connection, "SELECT 1"), failure);
    }

    @Test
    public void queryFailureRemainsPrimaryWhenStatementCloseAlsoFails() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        SQLException queryFailure = new SQLException("query failed");
        SQLException closeFailure = new SQLException("statement close failed");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(queryFailure);
        doThrow(closeFailure).when(statement).close();

        Throwable actual = invokeExecuteSqlFailure(connection, "SELECT 1");

        assertSame(actual, queryFailure);
        assertEquals(actual.getSuppressed().length, 1);
        assertSame(actual.getSuppressed()[0], closeFailure);
    }

    @Test
    public void resultSetAndStatementCloseFailuresAreBothRetained() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        SQLException resultSetCloseFailure = new SQLException("result set close failed");
        SQLException statementCloseFailure = new SQLException("statement close failed");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        doThrow(resultSetCloseFailure).when(resultSet).close();
        doThrow(statementCloseFailure).when(statement).close();

        Throwable actual = invokeExecuteSqlFailure(connection, "SELECT 1");

        assertSame(actual, resultSetCloseFailure);
        assertEquals(actual.getSuppressed().length, 1);
        assertSame(actual.getSuppressed()[0], statementCloseFailure);
    }

    @Test
    public void statementCloseFailureIsSurfacedAfterSuccessfulUpdate() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        SQLException closeFailure = new SQLException("statement close failed");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);
        doThrow(closeFailure).when(statement).close();

        assertSame(invokeExecuteSqlFailure(connection, "UPDATE sample SET value = 1"), closeFailure);
    }

    @Test
    public void runtimeQueryFailureRemainsPrimaryWhenStatementCloseFails() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        IllegalStateException queryFailure = new IllegalStateException("iteration failed");
        SQLException closeFailure = new SQLException("statement close failed");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenThrow(queryFailure);
        doThrow(closeFailure).when(statement).close();

        Throwable actual = invokeExecuteSqlFailure(connection, "SELECT 1");

        assertSame(actual, queryFailure);
        assertEquals(actual.getSuppressed().length, 1);
        assertSame(actual.getSuppressed()[0], closeFailure);
    }

    @Test
    public void nullColumnLabelAndValueAreSerializedAsEmptyStrings() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(1);
        when(metadata.getColumnLabel(1)).thenReturn(null);
        when(resultSet.getString(1)).thenReturn(null);

        Variable result = (Variable) invokeExecuteSql(connection, "SELECT 1");

        assertEquals(result.toString(), "<results><row><column name=\"\"></column></row></results>");
    }

    @Test
    public void configuredQueryTimeoutIsAppliedBeforeExecution() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        invokeExecuteSql(connection, "UPDATE sample SET value = 1", null, 17);

        verify(statement).setQueryTimeout(17);
    }

    @Test
    public void zeroQueryTimeoutPreservesDriverDefault() throws Throwable {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        invokeExecuteSql(connection, "UPDATE sample SET value = 1", null, 0);

        org.mockito.Mockito.verify(statement,
            org.mockito.Mockito.never()).setQueryTimeout(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void realResourceLedgerAbortCancelsBlockedJdbcStatementAndDrainsAfterLeaseRelease() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch cancellationObserved = new CountDownLatch(1);
        CountDownLatch releaseQuery = new CountDownLatch(1);

        ResourceLedger ledger = new ResourceLedger("jdbc-test-runtime");
        ExecutionOwner owner = new ExecutionOwner("jdbc-test-runtime", "jdbc-cancel-session", 1);
        LedgerExecutionResourceCoordinator coordinator =
            new LedgerExecutionResourceCoordinator(ledger, owner);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenAnswer(invocation -> {
            queryStarted.countDown();
            if (!releaseQuery.await(3, TimeUnit.SECONDS)) {
                throw new SQLException("Timed out waiting for Statement.cancel()");
            }
            return resultSet;
        });
        when(resultSet.next()).thenReturn(false);
        doAnswer(invocation -> {
            cancellationObserved.countDown();
            releaseQuery.countDown();
            return null;
        }).when(statement).cancel();

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Object> execution = worker.submit(
                () -> invokeExecuteSql(connection, "SELECT 1", coordinator.getAccess(), 0));
            assertTrue(queryStarted.await(2, TimeUnit.SECONDS),
                "JDBC execution must be blocked before owner termination");

            coordinator.requestTermination();
            assertTrue(cancellationObserved.await(2, TimeUnit.SECONDS),
                "ResourceLedger abort must invoke Statement.cancel()");
            execution.get(2, TimeUnit.SECONDS);

            coordinator.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
            verify(statement).cancel();
            verify(statement).close();
            assertEquals(ledger.size(), 0,
                "The statement operation resource must drain after its execution lease is released");
        } catch (ExecutionException failure) {
            throw new AssertionError("JDBC execution should exit after ResourceLedger abort", failure.getCause());
        } finally {
            releaseQuery.countDown();
            worker.shutdownNow();
            worker.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void ownerAbortCancelsBlockedStatementAndFinalCloseWaitsForLeaseRelease() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        ExecutionResourceAccess resourceAccess = mock(ExecutionResourceAccess.class);
        ExecutionResourceAccess.OperationResource operation =
            mock(ExecutionResourceAccess.OperationResource.class);
        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch releaseQuery = new CountDownLatch(1);
        AtomicBoolean leaseReleased = new AtomicBoolean();
        AtomicReference<ExecutionResourceAccess.AbortAction> abortAction = new AtomicReference<>();
        AtomicReference<ExecutionResourceAccess.CloseAction> closeAction = new AtomicReference<>();
        AtomicReference<ExecutionResourceAccess.ResourceState> resourceState =
            new AtomicReference<>(ExecutionResourceAccess.ResourceState.OPEN);

        ExecutionResourceAccess.Lease lease = () -> leaseReleased.set(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenAnswer(invocation -> {
            queryStarted.countDown();
            if (!releaseQuery.await(3, TimeUnit.SECONDS)) {
                throw new SQLException("Timed out waiting for query release");
            }
            return resultSet;
        });
        when(resultSet.next()).thenReturn(false);
        when(operation.acquire()).thenReturn(lease);
        when(operation.getState()).thenAnswer(invocation -> resourceState.get());
        when(operation.getCloseFailure()).thenReturn(null);
        when(operation.getResourceId()).thenReturn("jdbc/statement/test");
        when(resourceAccess.registerOperationResource(
            anyString(),
            any(ExecutionResourceAccess.AbortAction.class),
            any(ExecutionResourceAccess.CloseAction.class))).thenAnswer(invocation -> {
                abortAction.set(invocation.getArgument(1));
                closeAction.set(invocation.getArgument(2));
                return operation;
            });
        doAnswer(invocation -> {
            assertTrue(leaseReleased.get(), "Final statement close must follow lease release");
            closeAction.get().close();
            resourceState.set(ExecutionResourceAccess.ResourceState.CLOSED);
            return null;
        }).when(operation).close();

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Object> execution = worker.submit(
                () -> invokeExecuteSql(connection, "SELECT 1", resourceAccess, 0));
            assertTrue(queryStarted.await(2, TimeUnit.SECONDS),
                "The statement must enter its blocking query before owner abort");
            abortAction.get().abort();
            verify(statement).cancel();

            releaseQuery.countDown();
            execution.get(2, TimeUnit.SECONDS);

            assertTrue(leaseReleased.get());
            verify(operation).close();
            verify(statement).close();
        } catch (ExecutionException failure) {
            throw new AssertionError("Managed JDBC query should exit after statement cancellation", failure.getCause());
        } finally {
            releaseQuery.countDown();
            worker.shutdownNow();
            worker.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void combineFailuresPreservesIdentityAndAvoidsSelfSuppression() throws Throwable {
        DatabasePlugin plugin = new DatabasePlugin();
        Method method = DatabasePlugin.class.getDeclaredMethod(
            "combineFailures", Throwable.class, Throwable.class);
        method.setAccessible(true);
        Throwable primary = new IllegalStateException("primary");
        Throwable secondary = new IllegalArgumentException("secondary");

        assertSame(method.invoke(plugin, null, secondary), secondary);
        assertSame(method.invoke(plugin, primary, secondary), primary);
        assertEquals(primary.getSuppressed().length, 1);
        assertSame(primary.getSuppressed()[0], secondary);
        assertSame(method.invoke(plugin, primary, primary), primary);
        assertEquals(primary.getSuppressed().length, 1);
    }


    private static Throwable invokeExecuteSqlFailure(Connection connection, String sql) {
        try {
            invokeExecuteSql(connection, sql);
        } catch (Throwable failure) {
            return failure;
        }
        throw new AssertionError("Expected executeSql to fail");
    }

    private static Object invokeExecuteSql(Connection connection, String sql) throws Throwable {
        return invokeExecuteSql(connection, sql, null, 0);
    }

    private static Object invokeExecuteSql(
            Connection connection, String sql, ExecutionResourceAccess resourceAccess,
            int queryTimeoutSeconds) throws Throwable {
        Method method = DatabasePlugin.class.getDeclaredMethod(
            "executeSql", Connection.class, String.class, ExecutionResourceAccess.class, int.class);
        method.setAccessible(true);
        try {
            return method.invoke(new DatabasePlugin(), connection, sql, resourceAccess, queryTimeoutSeconds);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }
}
