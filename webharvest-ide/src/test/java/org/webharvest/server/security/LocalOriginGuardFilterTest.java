package org.webharvest.server.security;

import org.eclipse.jetty.server.ServerConnector;
import org.testng.annotations.Test;
import org.webharvest.server.WebHarvestServer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LocalOriginGuardFilterTest {

    @Test
    public void embeddedServerUsesAnExplicitIpv4LoopbackConnector() {
        ServerConnector connector = (ServerConnector) WebHarvestServer.createLoopbackServer(0)
                .getConnectors()[0];

        assertEquals(connector.getHost(), "127.0.0.1");
        assertEquals(connector.getPort(), 0);
    }

    @Test
    public void acceptsLoopbackHostAndMatchingOrigin() {
        assertTrue(LocalOriginGuardFilter.isLoopbackHost("localhost:8080", 8080));
        assertTrue(LocalOriginGuardFilter.isLoopbackHost("127.0.0.1:8080", 8080));
        assertTrue(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://localhost:8080", "localhost:8080", 8080));
        assertTrue(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://127.0.0.1:8080", "127.0.0.1:8080", 8080));
    }

    @Test
    public void rejectsRemoteHostsAndDnsRebindingAuthorities() {
        assertFalse(LocalOriginGuardFilter.isLoopbackHost("attacker.example:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isLoopbackHost("localhost:8081", 8080));
        assertFalse(LocalOriginGuardFilter.isLoopbackHost("localhost.evil.example:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://attacker.example:8080", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://localhost:8081", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "null", "localhost:8080", 8080));
    }

    @Test
    public void rejectsNonHttpAndMalformedOrigins() {
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "https://localhost:8080", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://localhost:8080/path", "localhost:8080", 8080));
        assertFalse(LocalOriginGuardFilter.isSameLocalOrigin(
                "http://user@localhost:8080", "localhost:8080", 8080));
    }
}
