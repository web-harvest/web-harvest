package org.webharvest.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.server.api.ConfigServlet;
import org.webharvest.server.api.ExecutionServlet;
import org.webharvest.server.api.VisualSemanticModelServlet;
import org.webharvest.server.websocket.ExecutionWebSocketServlet;
import org.webharvest.server.websocket.VisualSemanticModelWebSocketServlet;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

/**
 * WebHarvest IDE - Embedded Jetty Server
 * 
 * Modern web-based IDE with:
 * - Monaco Editor (VS Code editor)
 * - Real-time execution updates via WebSocket
 * - Multi-tab editing
 * - Concurrent scraper execution
 * 
 * Distribution: Single executable JAR
 * Usage: java -jar webharvest-ide-2.2.0.jar
 */
public class WebHarvestServer {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebHarvestServer.class);
    private static final int DEFAULT_PORT = 8080;
    private static final String LOOPBACK_HOST = "127.0.0.1";
    
    private Server server;
    private final int port;
    
    public WebHarvestServer(int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
        LOG.info("Starting WebHarvest IDE on port {}...", port);
        LOG.info("Java version: {}", System.getProperty("java.version"));
        LOG.info("Working directory: {}", System.getProperty("user.dir"));
        
        server = createServer(port);
        
        // Single ServletContextHandler for everything
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Get webapp resource path
        java.net.URL webappUrl = getClass().getResource("/webapp");
        if (webappUrl == null) {
            LOG.error("FATAL: /webapp resource not found in JAR!");
            LOG.error("Trying to list root resources...");
            java.net.URL root = getClass().getResource("/");
            if (root != null) {
                LOG.error("Root resource: {}", root.toExternalForm());
            }
            throw new IllegalStateException("webapp resource not found - IDE cannot start");
        }
        
        String webappPath = webappUrl.toExternalForm();
        LOG.info("Webapp resource found: {}", webappPath);
        
        // Set base resource for static files
        Resource webappResource = Resource.newResource(webappUrl);
        context.setBaseResource(webappResource);
        LOG.info("Base resource set to: {}", webappResource.toString());
        
        // 1. REST API servlets
        context.addServlet(new ServletHolder(new ConfigServlet()), "/api/config/*");
        context.addServlet(new ServletHolder(new ExecutionServlet()), "/api/execute/*");
        context.addServlet(new ServletHolder(new org.webharvest.server.api.ExecutionControlServlet()), "/api/execution/*");
        context.addServlet(new ServletHolder(new org.webharvest.server.api.SessionHistoryServlet()), "/api/sessions/*");
        context.addServlet(new ServletHolder(new org.webharvest.server.api.PluginMetadataServlet()), "/api/plugins/*");
        context.addServlet(new ServletHolder(new VisualSemanticModelServlet()), "/api/visual/model/*");
        context.addServlet(new ServletHolder(new org.webharvest.server.api.VisualSemanticModelDiagnosticServlet()), "/api/visual/model/diagnostic");
        LOG.info("API servlets configured: /api/config/*, /api/execute/*, /api/execution/*, /api/sessions/*, /api/plugins/*, /api/visual/model/*, /api/visual/model/diagnostic");
        
        // 2. WebSocket servlet
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(1048576); // 1MB - increased for large semantic models
            wsContainer.setIdleTimeout(java.time.Duration.ofMinutes(10));
            LOG.info("WebSocket container configured with maxTextMessageSize=1MB");
        });
        
        context.addServlet(new ServletHolder(new ExecutionWebSocketServlet()), "/ws/execution/*");
        context.addServlet(new ServletHolder(new VisualSemanticModelWebSocketServlet()), "/ws/visual/model/*");
        LOG.info("WebSocket servlets configured: /ws/execution/*, /ws/visual/model/*");
        
        // 3. Default servlet for static files (MUST BE LAST!)
        ServletHolder defaultServlet = createDefaultServlet();
        context.addServlet(defaultServlet, "/");
        LOG.info("Default servlet configured for static files (cache DISABLED for development)");
        
        server.setHandler(context);
        
        LOG.info("All handlers configured in single context successfully");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOG.info("Shutting down WebHarvest IDE...");
                server.stop();
            } catch (Exception e) {
                LOG.error("Error during shutdown", e);
            }
        }));
        
        // Start server
        server.start();
        LOG.info("WebHarvest IDE started successfully!");
        LOG.info("Open in browser: {}", browserUrl(port));
        
        // Auto-open browser
        openBrowser();
        
        // Wait for server
        LOG.info("[DEBUG] About to call server.join()...");
        try {
            server.join();
            LOG.info("[DEBUG] server.join() completed normally");
        } catch (Exception e) {
            LOG.error("[DEBUG] server.join() threw exception:", e);
            throw e;
        }
        LOG.info("[DEBUG] After server.join()");
    }
    
    /**
     * Creates the IDE listener bound only to the local machine.
     * The IDE execution API is intentionally not exposed to the network.
     */
    static Server createServer(int port) {
        return new Server(new InetSocketAddress(LOOPBACK_HOST, port));
    }

    static ServletHolder createDefaultServlet() {
        ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
        defaultServlet.setInitParameter("dirAllowed", "false");
        defaultServlet.setInitParameter("pathInfoOnly", "false");
        defaultServlet.setInitParameter("welcomeServlets", "true");
        defaultServlet.setInitParameter("cacheControl", "no-store, no-cache, must-revalidate, max-age=0");
        defaultServlet.setInitParameter("useFileMappedBuffer", "false");
        defaultServlet.setInitParameter("etags", "false");
        return defaultServlet;
    }

    private static String browserUrl(int port) {
        return "http://" + LOOPBACK_HOST + ":" + port;
    }

    private void openBrowser() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(browserUrl(port)));
                LOG.info("Browser opened automatically");
            } else {
                LOG.info("Please open {} in your browser", browserUrl(port));
            }
        } catch (Exception e) {
            LOG.warn("Could not auto-open browser: {}", e.getMessage());
            LOG.info("Please open {} manually", browserUrl(port));
        }
    }
    
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getByName(LOOPBACK_HOST))) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        for (String arg : args) {
            if (arg.startsWith("--port=") || arg.startsWith("port=")) {
                try {
                    port = Integer.parseInt(arg.split("=")[1]);
                } catch (NumberFormatException e) {
                    LOG.error("Invalid port number: {}", arg);
                    System.exit(1);
                }
            }
        }
        
        // Check if port is available
        if (!isPortAvailable(port)) {
            LOG.error("Port {} is already in use!", port);
            LOG.info("Trying to open existing instance...");
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port));
                System.exit(0);
            } catch (Exception e) {
                LOG.error("Port {} is in use but could not open browser", port);
                System.exit(1);
            }
        }
        
        try {
            WebHarvestServer server = new WebHarvestServer(port);
            server.start();
        } catch (Exception e) {
            LOG.error("Failed to start WebHarvest IDE", e);
            System.exit(1);
        }
    }
}

