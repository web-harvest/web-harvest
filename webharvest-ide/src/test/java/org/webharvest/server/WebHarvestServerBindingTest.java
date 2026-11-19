package org.webharvest.server;

import org.eclipse.jetty.server.ServerConnector;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class WebHarvestServerBindingTest {

    @Test
    public void embeddedServerUsesAnExplicitIpv4LoopbackConnector() {
        ServerConnector connector = (ServerConnector) WebHarvestServer.createLoopbackServer(0)
                .getConnectors()[0];

        assertEquals(connector.getHost(), "127.0.0.1");
        assertEquals(connector.getPort(), 0);
    }
}
