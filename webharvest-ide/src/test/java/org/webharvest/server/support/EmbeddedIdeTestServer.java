package org.webharvest.server.support;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.WebHarvestServer;
import org.webharvest.server.api.ConfigServlet;
import org.webharvest.server.api.ExecutionControlServlet;
import org.webharvest.server.api.ExecutionServlet;
import org.webharvest.server.api.PluginMetadataServlet;
import org.webharvest.server.api.SessionHistoryServlet;
import org.webharvest.server.websocket.ExecutionWebSocketServlet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Embedded Jetty server used for IDE integration tests.
 * Mirrors the production server configuration while running in-memory.
 */
public final class EmbeddedIdeTestServer {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedIdeTestServer.class);
    private static final String LOOPBACK_HOST = "127.0.0.1";
    private static EmbeddedIdeTestServer INSTANCE;

    static {
        TestEnvironment.ensure();
    }

    private final int port;
    private final Server server;
    private final String baseHttpUrl;
    private final String baseWsUrl;

    private EmbeddedIdeTestServer() throws Exception {
        this.port = findFreePort();
        this.server = new Server(new InetSocketAddress(LOOPBACK_HOST, port));
        configureServer();
        this.baseHttpUrl = "http://" + LOOPBACK_HOST + ":" + port;
        this.baseWsUrl = "ws://" + LOOPBACK_HOST + ":" + port;

        Runtime.getRuntime().addShutdownHook(new Thread(this::safeStop, "embedded-ide-server-shutdown"));
    }

    public static synchronized EmbeddedIdeTestServer getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new EmbeddedIdeTestServer();
                LOG.info("Embedded IDE server started on port {}", INSTANCE.port);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to start embedded IDE server", e);
            }
        }
        return INSTANCE;
    }

    public String getBaseHttpUrl() {
        return baseHttpUrl;
    }

    public String getBaseWsUrl() {
        return baseWsUrl;
    }

    public int getPort() {
        return port;
    }

    private void configureServer() throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        java.net.URL webappUrl = WebHarvestServer.class.getResource("/webapp");
        if (webappUrl == null) {
            throw new IllegalStateException("Unable to locate /webapp resources");
        }
        Resource webappResource = Resource.newResource(webappUrl);
        context.setBaseResource(webappResource);

        context.addServlet(new ServletHolder(new ConfigServlet()), "/api/config/*");
        context.addServlet(new ServletHolder(new ExecutionServlet()), "/api/execute/*");
        context.addServlet(new ServletHolder(new ExecutionControlServlet()), "/api/execution/*");
        context.addServlet(new ServletHolder(new SessionHistoryServlet()), "/api/sessions/*");
        context.addServlet(new ServletHolder(new PluginMetadataServlet()), "/api/plugins/*");

        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.setIdleTimeout(java.time.Duration.ofMinutes(10));
        });
        context.addServlet(new ServletHolder(new ExecutionWebSocketServlet()), "/ws/execution/*");

        ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
        defaultServlet.setInitParameter("dirAllowed", "false");
        defaultServlet.setInitParameter("pathInfoOnly", "false");
        defaultServlet.setInitParameter("welcomeServlets", "true");
        defaultServlet.setInitParameter("cacheControl", "no-store, no-cache, must-revalidate, max-age=0");
        defaultServlet.setInitParameter("useFileMappedBuffer", "false");
        defaultServlet.setInitParameter("etags", "false");
        context.addServlet(defaultServlet, "/");

        server.setHandler(context);
        server.start();
    }

    private void safeStop() {
        try {
            if (server.isRunning()) {
                server.stop();
            }
        } catch (Exception e) {
            LOG.warn("Failed to stop embedded IDE server cleanly", e);
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK_HOST))) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}


