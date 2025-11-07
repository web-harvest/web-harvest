package org.webharvest.server.manager;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.annotations.*;
import org.webharvest.server.websocket.ExecutionWebSocketServlet;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * PROFESSIONAL Integration Tests for ExecutionManager
 * 
 * This test suite uses REAL Jetty WebSocket (not mocks) to verify:
 * - Variable extraction from configurations
 * - Log streaming to UI
 * - WebSocket message delivery
 * - Execution lifecycle
 * 
 * These tests will catch bugs that users would report!
 */
@Test(groups = "integration")
public class ExecutionManagerWebSocketTest {
    
    private Server server;
    private WebSocketClient client;
    private ExecutionManager executionManager;
    private int testPort = 8888;
    
    @BeforeClass
    public void setUpServer() throws Exception {
        System.out.println("\n========================================");
        System.out.println("Setting up test WebSocket server...");
        System.out.println("========================================\n");
        
        // Start embedded Jetty for WebSocket tests
        server = new Server(testPort);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Configure WebSocket
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.setIdleTimeout(java.time.Duration.ofMinutes(10));
        });
        
        context.addServlet(new ServletHolder(new ExecutionWebSocketServlet()), "/ws/execution/*");
        
        server.setHandler(context);
        server.start();
        
        // Start WebSocket client
        client = new WebSocketClient();
        client.start();
        
        executionManager = ExecutionManager.getInstance();
        
        System.out.println("✓ Test server started on port " + testPort);
    }
    
    @AfterClass
    public void tearDownServer() throws Exception {
        if (client != null) {
            client.stop();
        }
        if (server != null) {
            server.stop();
        }
        System.out.println("\n✓ Test server stopped");
    }
    
    /**
     * Test 1: Simple DEF variables
     * Verifies basic variable extraction works
     */
    @Test(priority = 1)
    public void testSimpleDefVariables() throws Exception {
        System.out.println("\n### TEST 1: Simple DEF Variables ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"greeting\">Hello World!</def>\n" +
            "    <def var=\"user\">Web-Harvest User</def>\n" +
            "</config>";
        
        ExecutionCapture capture = executeAndCapture("test-def", configXml);
        
        // Verify completion
        assertTrue(capture.completed, "Should complete successfully");
        assertEquals(capture.status, "COMPLETED", "Status should be COMPLETED");
        
        // Verify logs
        assertTrue(capture.hasLog("INFO", "Configuration loaded"), "Should log configuration loaded");
        assertTrue(capture.hasLog("SUCCESS", "Execution finished"), "Should log execution finished");
        
        // Verify variables
        assertTrue(capture.hasVariable("greeting"), "Should have 'greeting' variable");
        assertTrue(capture.hasVariable("user"), "Should have 'user' variable");
        assertTrue(capture.getVariable("greeting").contains("Hello World"), "greeting should contain 'Hello World'");
        assertTrue(capture.getVariable("user").contains("Web-Harvest User"), "user should contain 'Web-Harvest User'");
        
        System.out.println("✓ TEST 1 PASSED: Both variables extracted correctly");
        System.out.println("  - greeting: " + capture.getVariable("greeting"));
        System.out.println("  - user: " + capture.getVariable("user"));
    }
    
    /**
     * Test 2: XPath on static XML
     * Verifies XPath plugin without network dependency
     */
    @Test(priority = 2)
    public void testXPathPluginVariables() throws Exception {
        System.out.println("\n### TEST 2: XPath Plugin Variables ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"xmlContent\">&lt;root&gt;&lt;item&gt;First&lt;/item&gt;&lt;item&gt;Second&lt;/item&gt;&lt;/root&gt;</def>\n" +
            "    <def var=\"items\">\n" +
            "        <xpath expression=\"//item/text()\">\n" +
            "            <get var=\"xmlContent\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    <def var=\"count\">\n" +
            "        <xpath expression=\"count(//item)\">\n" +
            "            <get var=\"xmlContent\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        ExecutionCapture capture = executeAndCapture("test-xpath", configXml);
        
        // Verify completion
        assertTrue(capture.completed, "Should complete successfully");
        
        // Verify variables
        assertTrue(capture.hasVariable("xmlContent"), "Should have 'xmlContent' variable");
        assertTrue(capture.hasVariable("items"), "Should have 'items' variable");
        assertTrue(capture.hasVariable("count"), "Should have 'count' variable");
        assertTrue(capture.getVariable("xmlContent").contains("<root>"), "xmlContent should contain <root>");
        assertTrue(capture.getVariable("items").contains("First"), "items should contain 'First'");
        assertTrue(capture.getVariable("count").contains("2"), "count should be 2");
        
        System.out.println("✓ TEST 2 PASSED: XPath variables extracted correctly");
        System.out.println("  - xmlContent: " + capture.getVariable("xmlContent"));
        System.out.println("  - items: " + capture.getVariable("items"));
        System.out.println("  - count: " + capture.getVariable("count"));
    }
    
    /**
     * Test 3: THE CRITICAL TEST - Multi-variable pipeline with HTML parsing
     * This tests the full variable extraction chain
     */
    @Test(priority = 3)
    public void testMultiVariablePipeline_CRITICAL() throws Exception {
        System.out.println("\n### TEST 3: Multi-Variable Pipeline (CRITICAL!) ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"rawHtml\">&lt;!DOCTYPE html&gt;&lt;html&gt;&lt;head&gt;&lt;title&gt;Test Page&lt;/title&gt;&lt;/head&gt;&lt;body&gt;&lt;h1&gt;Hello&lt;/h1&gt;&lt;/body&gt;&lt;/html&gt;</def>\n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"rawHtml\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    <def var=\"title\">\n" +
            "        <xpath expression=\"//title/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    <def var=\"heading\">\n" +
            "        <xpath expression=\"//h1/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        ExecutionCapture capture = executeAndCapture("test-pipeline", configXml);
        
        // Verify completion
        assertTrue(capture.completed, "Should complete successfully");
        
        // THIS IS THE CRITICAL TEST - ALL variables must be present!
        System.out.println("\n  Checking ALL variables:");
        
        boolean allPresent = true;
        String[] expectedVars = {"rawHtml", "page", "title", "heading"};
        
        for (String varName : expectedVars) {
            if (capture.hasVariable(varName)) {
                String value = capture.getVariable(varName);
                System.out.println("    ✓ " + varName + " = " + 
                    (value.length() > 60 ? value.substring(0, 60) + "..." : value));
            } else {
                System.out.println("    ✗ " + varName + " = MISSING!");
                allPresent = false;
            }
        }
        
        // Assert all variables are present
        assertTrue(capture.hasVariable("rawHtml"), "CRITICAL: 'rawHtml' variable must be present");
        assertTrue(capture.hasVariable("page"), "CRITICAL: 'page' variable must be present");
        assertTrue(capture.hasVariable("title"), "CRITICAL: 'title' variable must be present");
        assertTrue(capture.hasVariable("heading"), "CRITICAL: 'heading' variable must be present");
        
        // Verify content
        assertTrue(capture.getVariable("rawHtml").contains("DOCTYPE"), "rawHtml should contain DOCTYPE");
        assertTrue(capture.getVariable("page").contains("<html>"), "page should contain <html>");
        assertTrue(capture.getVariable("title").contains("Test Page"), "title should be 'Test Page'");
        assertTrue(capture.getVariable("heading").contains("Hello"), "heading should be 'Hello'");
        
        if (allPresent) {
            System.out.println("\n✓✓✓ TEST 3 PASSED: ALL PIPELINE VARIABLES EXTRACTED! ✓✓✓");
        } else {
            fail("CRITICAL FAILURE: Not all variables were extracted from pipeline!");
        }
    }
    
    /**
     * Test 4: Verify rich log output
     */
    @Test(priority = 4)
    public void testLogStreaming() throws Exception {
        System.out.println("\n### TEST 4: Log Streaming ###");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"test\">Test Value</def>\n" +
            "</config>";
        
        ExecutionCapture capture = executeAndCapture("test-logs", configXml);
        
        // Verify we received logs
        assertTrue(capture.logs.size() > 0, "Should receive at least one log message");
        System.out.println("  Received " + capture.logs.size() + " log messages:");
        
        for (String log : capture.logs) {
            System.out.println("    " + log);
        }
        
        System.out.println("✓ TEST 4 PASSED: Logs streaming correctly");
    }
    
    /**
     * Helper method to execute config and capture all WebSocket output
     */
    private ExecutionCapture executeAndCapture(String testName, String configXml) throws Exception {
        String executionId = testName + "-" + System.currentTimeMillis();
        ExecutionRequest request = new ExecutionRequest(executionId, testName + ".xml", configXml);
        
        ExecutionCapture capture = new ExecutionCapture();
        TestWebSocketClient wsClient = new TestWebSocketClient(capture);
        
        // Connect WebSocket
        URI wsUri = new URI("ws://localhost:" + testPort + "/ws/execution/?id=" + executionId);
        Session session = client.connect(wsClient, wsUri, new ClientUpgradeRequest()).get(10, TimeUnit.SECONDS);
        
        // Submit execution
        executionManager.submit(request);
        
        // Wait for completion (max 30 seconds)
        boolean completed = capture.latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Execution should complete within 30 seconds");
        
        // Close WebSocket
        session.close();
        
        return capture;
    }
    
    /**
     * WebSocket client for tests
     */
    @WebSocket
    public static class TestWebSocketClient {
        private final ExecutionCapture capture;
        
        public TestWebSocketClient(ExecutionCapture capture) {
            this.capture = capture;
        }
        
        @OnWebSocketMessage
        public void onMessage(String message) {
            capture.onMessage(message);
        }
        
        @OnWebSocketError
        public void onError(Throwable error) {
            System.err.println("WebSocket error: " + error.getMessage());
            error.printStackTrace();
        }
    }
    
    /**
     * Captures execution output for verification
     */
    private static class ExecutionCapture {
        public final List<String> allMessages = new CopyOnWriteArrayList<>();
        public final List<String> logs = new CopyOnWriteArrayList<>();
        public final CountDownLatch latch = new CountDownLatch(1);
        public boolean completed = false;
        public String status = null;
        public String finalMessage = null;
        
        public void onMessage(String message) {
            allMessages.add(message);
            
            // Parse message type
            if (message.contains("\"type\":\"log\"")) {
                logs.add(message);
            } else if (message.contains("\"status\":\"COMPLETED\"")) {
                completed = true;
                status = "COMPLETED";
                finalMessage = message;
                latch.countDown();
            } else if (message.contains("\"status\":\"FAILED\"")) {
                completed = true;
                status = "FAILED";
                finalMessage = message;
                latch.countDown();
            }
        }
        
        public boolean hasLog(String level, String partialMessage) {
            return logs.stream().anyMatch(log -> 
                log.contains("\"level\":\"" + level + "\"") && 
                log.contains(partialMessage)
            );
        }
        
        public boolean hasVariable(String varName) {
            if (finalMessage == null) return false;
            return finalMessage.contains("\"" + varName + "\"");
        }
        
        public String getVariable(String varName) {
            if (finalMessage == null) return null;
            
            // Simple JSON parsing (good enough for tests)
            int start = finalMessage.indexOf("\"" + varName + "\":");
            if (start == -1) return null;
            
            start = finalMessage.indexOf("\"", start + varName.length() + 3);
            if (start == -1) return null;
            
            int end = finalMessage.indexOf("\"", start + 1);
            if (end == -1) return null;
            
            return finalMessage.substring(start + 1, end);
        }
    }
}

