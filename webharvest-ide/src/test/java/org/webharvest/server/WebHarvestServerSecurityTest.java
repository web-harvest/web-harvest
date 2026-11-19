package org.webharvest.server;

import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.annotations.Test;

import java.net.InetAddress;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** Security regression tests for the embedded local IDE server. */
public class WebHarvestServerSecurityTest {

    @Test
    public void defaultConnectorBindsOnlyToLoopback() throws Exception {
        Server server = WebHarvestServer.createServer(0);
        try {
            assertTrue(server.getConnectors().length == 1, "Expected exactly one HTTP connector");
            NetworkConnector connector = (NetworkConnector) server.getConnectors()[0];
            assertNotNull(connector.getHost(), "The listener must have an explicit bind address");
            assertTrue(
                    InetAddress.getByName(connector.getHost()).isLoopbackAddress(),
                    "The default IDE listener must not bind to a network interface"
            );
        } finally {
            server.stop();
        }
    }

    @Test
    public void staticFileServletDoesNotExposeDirectoryListings() {
        ServletHolder servlet = WebHarvestServer.createDefaultServlet();
        assertFalse(Boolean.parseBoolean(servlet.getInitParameter("dirAllowed")),
                "Directory listings must be disabled");
    }
}
