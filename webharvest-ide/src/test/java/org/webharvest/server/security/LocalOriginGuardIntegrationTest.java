package org.webharvest.server.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.annotations.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LocalOriginGuardIntegrationTest {

    @Test
    public void rejectsCrossOriginPostBeforeApiServletAndAllowsSameOriginPost() throws Exception {
        AtomicBoolean servletReached = new AtomicBoolean();
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addFilter(new FilterHolder(new LocalOriginGuardFilter()), "/api/*",
                EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                servletReached.set(true);
                response.setStatus(HttpServletResponse.SC_OK);
            }
        }), "/api/execute");
        server.setHandler(context);

        try {
            server.start();
            int port = connector.getLocalPort();

            assertEquals(post(port, "http://attacker.example:" + port), HttpServletResponse.SC_FORBIDDEN);
            assertFalse(servletReached.get(), "Cross-origin request must not reach the API servlet");

            assertEquals(post(port, "http://127.0.0.1:" + port), HttpServletResponse.SC_OK);
            assertTrue(servletReached.get(), "Same-origin request should reach the API servlet");
        } finally {
            server.stop();
        }
    }

    private int post(int port, String origin) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "http://127.0.0.1:" + port + "/api/execute").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Origin", origin);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream output = connection.getOutputStream()) {
            output.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }
}
