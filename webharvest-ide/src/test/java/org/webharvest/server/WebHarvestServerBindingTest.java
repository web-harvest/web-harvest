package org.webharvest.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WebHarvestServerBindingTest {

    @Test
    public void embeddedServerStartsOnAnEphemeralIpv4LoopbackPort() throws Exception {
        Server server = WebHarvestServer.createLoopbackServer(0);
        try {
            server.start();

            ServerConnector connector = (ServerConnector) server.getConnectors()[0];
            assertEquals(connector.getHost(), "127.0.0.1");
            assertTrue(connector.getLocalPort() > 0);
        } finally {
            server.stop();
        }
    }
}
