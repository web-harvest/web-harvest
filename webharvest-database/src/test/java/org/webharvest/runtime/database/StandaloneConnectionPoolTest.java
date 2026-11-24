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

}
