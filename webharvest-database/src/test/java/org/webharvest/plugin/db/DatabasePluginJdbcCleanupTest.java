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

import org.testng.annotations.Test;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

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
        Method method = DatabasePlugin.class.getDeclaredMethod(
            "executeSql", Connection.class, String.class);
        method.setAccessible(true);
        try {
            return method.invoke(new DatabasePlugin(), connection, sql);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }
}
