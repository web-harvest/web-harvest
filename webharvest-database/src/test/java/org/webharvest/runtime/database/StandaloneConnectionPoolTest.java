/*
 * Copyright (c) 2006-2025 the original author or authors.
 */
package org.webharvest.runtime.database;

import java.sql.Connection;

import org.testng.annotations.Test;
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
}
