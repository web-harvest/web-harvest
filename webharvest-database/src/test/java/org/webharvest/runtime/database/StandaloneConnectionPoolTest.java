/*
 * Copyright (c) 2006-2025 the original author or authors.
 */
package org.webharvest.runtime.database;

import java.sql.Connection;

import com.google.common.eventbus.EventBus;
import org.testng.annotations.Test;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionMetrics;
import org.webharvest.runtime.session.events.SessionCompletedEvent;
import org.webharvest.runtime.session.events.SessionFailedEvent;
import org.webharvest.runtime.session.events.SessionCancelledEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.webharvest.exception.DatabaseException;

import static org.testng.Assert.*;

/** Integration tests for the standalone JDBC connection lifecycle. */
public class StandaloneConnectionPoolTest {

    private static final String DRIVER = "org.h2.Driver";

    @Test
    public void reusesConnectionAndSuppressesClientClose() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        String url = "jdbc:h2:mem:pool_reuse;DB_CLOSE_DELAY=-1";

        Connection first = pool.getConnection(DRIVER, url, "sa", "");
        Connection second = pool.getConnection(DRIVER, url, "sa", "");

        assertSame(second, first, "The same key must reuse the session connection");
        assertTrue(first instanceof ConnectionProxy);

        first.close();
        assertFalse(first.isClosed(), "Client close must be suppressed by the proxy");

        Connection physical = ((ConnectionProxy) first).getTargetConnection();
        assertFalse(physical.isClosed());
        physical.close();
        assertTrue(physical.isClosed());
    }

    @Test
    public void usesDistinctConnectionsForDifferentKeys() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();

        Connection first = pool.getConnection(DRIVER,
                "jdbc:h2:mem:pool_first;DB_CLOSE_DELAY=-1", "sa", "");
        Connection second = pool.getConnection(DRIVER,
                "jdbc:h2:mem:pool_second;DB_CLOSE_DELAY=-1", "sa", "");

        assertNotSame(first, second);
        ((ConnectionProxy) first).getTargetConnection().close();
        ((ConnectionProxy) second).getTargetConnection().close();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void doesNotSupportLookupByJndiName() {
        new StandaloneConnectionPool().getConnection("java:comp/env/database");
    }

    @Test(expectedExceptions = DatabaseException.class)
    public void reportsUnavailableJdbcDriver() {
        new StandaloneConnectionPool().getConnection(
                "org.webharvest.test.MissingDriver",
                "jdbc:missing:test", "sa", "");
    }

    @Test
    public void releasesConnectionsWhenSessionCompletes() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        Connection connection = pool.getConnection(DRIVER,
                "jdbc:h2:mem:pool_completed;DB_CLOSE_DELAY=-1", "sa", "");
        Connection physical = ((ConnectionProxy) connection).getTargetConnection();
        ScraperSession session = mock(ScraperSession.class);
        when(session.getMetrics()).thenReturn(mock(SessionMetrics.class));
        when(session.getSessionId()).thenReturn("pool_completed_session");
        EventBus eventBus = new EventBus();
        pool.registerForSession(eventBus, "pool_completed_session");

        eventBus.post(new SessionCompletedEvent(session));

        assertTrue(physical.isClosed());
    }

    @Test
    public void releasesConnectionsWhenSessionFails() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        Connection connection = pool.getConnection(DRIVER,
                "jdbc:h2:mem:pool_failed;DB_CLOSE_DELAY=-1", "sa", "");
        Connection physical = ((ConnectionProxy) connection).getTargetConnection();
        EventBus eventBus = new EventBus();
        ScraperSession session = mock(ScraperSession.class);
        when(session.getSessionId()).thenReturn("pool_failed_session");
        pool.registerForSession(eventBus, "pool_failed_session");

        eventBus.post(new SessionFailedEvent(session,
                new IllegalStateException("expected test failure")));

        assertTrue(physical.isClosed());
    }

    @Test
    public void releasesConnectionsWhenSessionIsCancelled() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        Connection connection = pool.getConnection(DRIVER,
                "jdbc:h2:mem:pool_cancelled;DB_CLOSE_DELAY=-1", "sa", "");
        Connection physical = ((ConnectionProxy) connection).getTargetConnection();
        EventBus eventBus = new EventBus();
        ScraperSession session = mock(ScraperSession.class);
        when(session.getSessionId()).thenReturn("pool_cancelled_session");
        pool.registerForSession(eventBus, "pool_cancelled_session");

        eventBus.post(new SessionCancelledEvent(session));

        assertTrue(physical.isClosed());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsNullSessionEventBus() {
        new StandaloneConnectionPool().registerForSession(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsOwnerlessEventBusRegistration() {
        new StandaloneConnectionPool().registerForSession(new EventBus());
    }

    @Test
    public void repeatedRegistrationWithSameEventBusIsIdempotent() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        EventBus eventBus = new EventBus();
        pool.registerForSession(eventBus, "same-session");
        pool.registerForSession(eventBus, "same-session");
        pool.closeAllConnections();
        pool.closeAllConnections();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void rejectsRegistrationWithDifferentEventBus() {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        pool.registerForSession(new EventBus(), "session-a");
        try {
            pool.registerForSession(new EventBus(), "session-a");
        } finally {
            pool.closeAllConnections();
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void rejectsChangingOwnerOnTheSameEventBus() {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        EventBus eventBus = new EventBus();
        pool.registerForSession(eventBus, "session-a");
        try {
            pool.registerForSession(eventBus, "session-b");
        } finally {
            pool.closeAllConnections();
        }
    }

    @Test
    public void sharedEventBusCompletionClosesOnlyTheMatchingSessionPool() throws Exception {
        EventBus eventBus = new EventBus();
        StandaloneConnectionPool firstPool = new StandaloneConnectionPool();
        StandaloneConnectionPool secondPool = new StandaloneConnectionPool();
        Connection first = firstPool.getConnection(DRIVER,
            "jdbc:h2:mem:pool_session_a;DB_CLOSE_DELAY=-1", "sa", "");
        Connection second = secondPool.getConnection(DRIVER,
            "jdbc:h2:mem:pool_session_b;DB_CLOSE_DELAY=-1", "sa", "");
        Connection firstPhysical = ((ConnectionProxy) first).getTargetConnection();
        Connection secondPhysical = ((ConnectionProxy) second).getTargetConnection();

        firstPool.registerForSession(eventBus, "session-a");
        secondPool.registerForSession(eventBus, "session-b");

        ScraperSession sessionA = mock(ScraperSession.class);
        when(sessionA.getSessionId()).thenReturn("session-a");
        eventBus.post(new SessionCompletedEvent(sessionA));

        assertTrue(firstPhysical.isClosed(), "Session A must close its own pool");
        assertFalse(secondPhysical.isClosed(), "Session A must not close session B's pool");

        ScraperSession sessionB = mock(ScraperSession.class);
        when(sessionB.getSessionId()).thenReturn("session-b");
        eventBus.post(new SessionCompletedEvent(sessionB));
        assertTrue(secondPhysical.isClosed());
    }

    @Test
    public void closesEmptyPoolWithoutSessionEventBus() {
        new StandaloneConnectionPool().closeAllConnections();
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsNullSessionIdForOwnerRegistration() {
        new StandaloneConnectionPool().registerForSession(new EventBus(), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsBlankSessionIdForOwnerRegistration() {
        new StandaloneConnectionPool().registerForSession(new EventBus(), "  ");
    }

    @Test
    public void ignoresCompletionFailureAndCancellationFromOtherSessions() throws Exception {
        EventBus eventBus = new EventBus();
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        Connection connection = pool.getConnection(DRIVER,
            "jdbc:h2:mem:pool_non_owner_events;DB_CLOSE_DELAY=-1", "sa", "");
        Connection physical = ((ConnectionProxy) connection).getTargetConnection();
        pool.registerForSession(eventBus, "owner-session");

        ScraperSession otherSession = mock(ScraperSession.class);
        when(otherSession.getSessionId()).thenReturn("different-session");
        eventBus.post(new SessionCompletedEvent(otherSession));
        eventBus.post(new SessionFailedEvent(otherSession,
            new IllegalStateException("non-owner failure")));
        eventBus.post(new SessionCancelledEvent(otherSession));

        assertFalse(physical.isClosed(),
            "Events from another session must not close this pool's physical connection");

        ScraperSession ownerSession = mock(ScraperSession.class);
        when(ownerSession.getSessionId()).thenReturn("owner-session");
        eventBus.post(new SessionCompletedEvent(ownerSession));
        assertTrue(physical.isClosed());
    }
    @Test
    public void attemptsEveryPhysicalCloseAndPreservesSubsequentFailures() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        String firstUrl = "jdbc:webharvest-test:first";
        String secondUrl = "jdbc:webharvest-test:second";
        Connection firstPhysical = mock(Connection.class);
        Connection secondPhysical = mock(Connection.class);
        java.sql.SQLException firstFailure = new java.sql.SQLException("first close failed");
        java.sql.SQLException secondFailure = new java.sql.SQLException("second close failed");
        org.mockito.Mockito.doThrow(firstFailure).when(firstPhysical).close();
        org.mockito.Mockito.doThrow(secondFailure).when(secondPhysical).close();
        FailingCloseDriver.connections.put(firstUrl, firstPhysical);
        FailingCloseDriver.connections.put(secondUrl, secondPhysical);
        try {
            pool.getConnection(FailingCloseDriver.class.getName(), firstUrl, "user", "password");
            pool.getConnection(FailingCloseDriver.class.getName(), secondUrl, "user", "password");

            DatabaseException failure = expectDatabaseException(pool);

            assertSame(failure.getCause(), firstFailure,
                "The first physical close failure must remain the primary cause");
            assertEquals(firstFailure.getSuppressed().length, 1);
            assertSame(firstFailure.getSuppressed()[0], secondFailure,
                "Later close failures must be retained as suppressed exceptions");
            org.mockito.Mockito.verify(firstPhysical).close();
            org.mockito.Mockito.verify(secondPhysical).close();
            pool.closeAllConnections();
        } finally {
            FailingCloseDriver.connections.remove(firstUrl);
            FailingCloseDriver.connections.remove(secondUrl);
        }
    }

    @Test
    public void detachesSessionAfterPhysicalCloseFailureAndCanBeReassigned() throws Exception {
        StandaloneConnectionPool pool = new StandaloneConnectionPool();
        EventBus originalBus = new EventBus();
        String failingUrl = "jdbc:webharvest-test:session-close-failure";
        Connection failingPhysical = mock(Connection.class);
        java.sql.SQLException closeFailure = new java.sql.SQLException("session close failed");
        org.mockito.Mockito.doThrow(closeFailure).when(failingPhysical).close();
        FailingCloseDriver.connections.put(failingUrl, failingPhysical);
        try {
            pool.getConnection(FailingCloseDriver.class.getName(), failingUrl, "user", "password");
            pool.registerForSession(originalBus, "original-session");

            ScraperSession originalSession = mock(ScraperSession.class);
            when(originalSession.getSessionId()).thenReturn("original-session");
            originalBus.post(new SessionCompletedEvent(originalSession));

            org.mockito.Mockito.verify(failingPhysical).close();

            EventBus replacementBus = new EventBus();
            String replacementUrl = "jdbc:h2:mem:pool_reassigned;DB_CLOSE_DELAY=-1";
            Connection replacement = pool.getConnection(DRIVER, replacementUrl, "sa", "");
            Connection replacementPhysical = ((ConnectionProxy) replacement).getTargetConnection();
            pool.registerForSession(replacementBus, "replacement-session");

            ScraperSession staleSession = mock(ScraperSession.class);
            when(staleSession.getSessionId()).thenReturn("original-session");
            originalBus.post(new SessionCompletedEvent(staleSession));
            assertFalse(replacementPhysical.isClosed(),
                "A stale event bus must not retain ownership after failed physical cleanup");

            ScraperSession replacementSession = mock(ScraperSession.class);
            when(replacementSession.getSessionId()).thenReturn("replacement-session");
            replacementBus.post(new SessionCompletedEvent(replacementSession));
            assertTrue(replacementPhysical.isClosed(),
                "The reassigned owner must still be able to retire its connection");
        } finally {
            FailingCloseDriver.connections.remove(failingUrl);
            pool.closeAllConnections();
        }
    }

    private DatabaseException expectDatabaseException(StandaloneConnectionPool pool) {
        try {
            pool.closeAllConnections();
            fail("Expected physical connection close failures to be reported");
            return null;
        } catch (DatabaseException expected) {
            return expected;
        }
    }

    /** Minimal JDBC driver seam for deterministic physical-close failure tests. */
    public static final class FailingCloseDriver implements java.sql.Driver {
        private static final java.util.Map<String, Connection> connections =
            new java.util.concurrent.ConcurrentHashMap<String, Connection>();

        @Override
        public Connection connect(String url, java.util.Properties info) {
            return acceptsURL(url) ? connections.get(url) : null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:webharvest-test:");
        }

        @Override
        public java.sql.DriverPropertyInfo[] getPropertyInfo(
                String url, java.util.Properties info) {
            return new java.sql.DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public java.util.logging.Logger getParentLogger()
                throws java.sql.SQLFeatureNotSupportedException {
            throw new java.sql.SQLFeatureNotSupportedException();
        }
    }

}
