package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee9.servlet.ServletContextHandler;
import org.eclipse.jetty.ee9.servlet.ServletHolder;
import org.eclipse.jetty.ee9.websocket.api.Session;
import org.eclipse.jetty.ee9.websocket.api.annotations.*;
import org.eclipse.jetty.ee9.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.ee9.websocket.client.WebSocketClient;
import org.eclipse.jetty.ee9.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.testng.annotations.*;
import org.webharvest.server.manager.ExecutionManager;
import org.webharvest.server.websocket.ExecutionWebSocketServlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * CRITICAL TEST: WebSocket Race Condition
 * 
 * This test reproduces the EXACT bug that users experience:
 * 
 * PROBLEM:
 * 1. User clicks "Run" in UI
 * 2. Frontend sends POST /api/execute
 * 3. Backend immediately starts execution (old code waited only 100ms)
 * 4. Frontend tries to connect WebSocket
 * 5. Execution is ALREADY RUNNING - WebSocket misses initial messages!
 * 6. User sees "connecting..." forever
 * 
 * SOLUTION:
 * Backend must WAIT for WebSocket to connect before starting execution
 * 
 * This test verifies the fix works!
 */
@Test(groups = "integration")
public class ExecutionWebSocketRaceConditionTest {
    
    private Server server;
    private WebSocketClient wsClient;
    private Gson gson = new Gson();
    private int testPort = 9999;
    
    @BeforeClass
    public void setUpServer() throws Exception {
        System.out.println("\n========================================");
        System.out.println("🐛 WebSocket Race Condition Test");
        System.out.println("========================================\n");
        
        // Start full server stack (like real IDE)
        server = new Server(testPort);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Configure WebSocket
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.setIdleTimeout(java.time.Duration.ofMinutes(10));
        });
        
        // Add servlets (like real server)
        context.addServlet(new ServletHolder(new ExecutionServlet()), "/api/execute/*");
        context.addServlet(new ServletHolder(new ExecutionWebSocketServlet()), "/ws/execution/*");
        
        server.setHandler(context);
        server.start();
        
        // Start WebSocket client
        wsClient = new WebSocketClient();
        wsClient.start();
        
        System.out.println("✓ Test server started on port " + testPort);
    }
    
    @AfterClass
    public void tearDownServer() throws Exception {
        if (wsClient != null) {
            wsClient.stop();
        }
        if (server != null) {
            server.stop();
        }
        System.out.println("\n✓ Test server stopped");
    }
    
    /**
     * CRITICAL TEST: Simulate exact user workflow
     * 
     * This reproduces the bug:
     * 1. POST /api/execute (via HTTP like real frontend)
     * 2. Parse executionId from response
     * 3. Connect WebSocket with that executionId
     * 4. VERIFY we receive ALL messages (including STARTED)
     */
    @Test
    public void testWebSocketConnectsBeforeExecutionStarts_CRITICAL() throws Exception {
        System.out.println("\n### CRITICAL TEST: WebSocket Race Condition ###\n");
        
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"greeting\">Hello World</def>\n" +
            "    <def var=\"counter\">42</def>\n" +
            "</config>";
        
        // STEP 1: POST /api/execute (like frontend)
        System.out.println("1️⃣  Sending POST /api/execute...");
        String executionId = postExecuteRequest(configXml, "race-test.xml");
        System.out.println("   ✓ Got executionId: " + executionId.substring(0, 8) + "...");
        
        // STEP 2: Connect WebSocket (like frontend)
        System.out.println("2️⃣  Connecting WebSocket...");
        MessageCapture capture = new MessageCapture();
        TestWebSocketHandler handler = new TestWebSocketHandler(capture);
        
        URI wsUri = new URI("ws://localhost:" + testPort + "/ws/execution?id=" + executionId);
        Session session = wsClient.connect(handler, wsUri, new ClientUpgradeRequest())
                .get(5, TimeUnit.SECONDS);
        
        System.out.println("   ✓ WebSocket connected!");
        
        // STEP 3: Wait for execution to complete
        System.out.println("3️⃣  Waiting for execution to complete...");
        boolean completed = capture.completionLatch.await(15, TimeUnit.SECONDS);
        
        assertTrue(completed, "Execution should complete within 15 seconds");
        System.out.println("   ✓ Execution completed!");
        
        // STEP 4: Verify we received ALL messages
        System.out.println("\n4️⃣  Verifying messages received:");
        System.out.println("   Total messages: " + capture.allMessages.size());
        
        for (int i = 0; i < capture.allMessages.size(); i++) {
            String msg = capture.allMessages.get(i);
            String preview = msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
            System.out.println("   [" + i + "] " + preview);
        }
        
        // CRITICAL ASSERTIONS
        assertTrue(capture.allMessages.size() > 0, 
            "❌ FAILED: No messages received! WebSocket connected too late!");
        
        assertTrue(capture.hasStatus("STARTED"), 
            "❌ FAILED: Missed STARTED message! Execution started before WebSocket connected!");
        
        assertTrue(capture.hasStatus("COMPLETED"), 
            "❌ FAILED: Missed COMPLETED message!");
        
        assertTrue(capture.hasVariable("greeting"), 
            "❌ FAILED: Missing 'greeting' variable in final message!");
        
        assertTrue(capture.hasVariable("counter"), 
            "❌ FAILED: Missing 'counter' variable in final message!");
        
        session.close();
        
        System.out.println("\n✅✅✅ TEST PASSED! ✅✅✅");
        System.out.println("Backend correctly waited for WebSocket before starting execution!");
        System.out.println("All messages received in correct order!");
    }
    
    /**
     * Send POST request to /api/execute (simulates frontend)
     */
    private String postExecuteRequest(String configXml, String configName) throws Exception {
        URL url = new URL("http://localhost:" + testPort + "/api/execute");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Build request JSON
        JsonObject request = new JsonObject();
        request.addProperty("configXml", configXml);
        request.addProperty("configName", configName);
        request.addProperty("waitForWebSocket", true);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(gson.toJson(request).getBytes());
            os.flush();
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        assertEquals(responseCode, 200, "POST /api/execute should return 200");
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        // Parse executionId
        JsonObject responseJson = gson.fromJson(response.toString(), JsonObject.class);
        assertTrue(responseJson.has("executionId"), "Response must contain executionId");
        
        return responseJson.get("executionId").getAsString();
    }
    
    /**
     * WebSocket handler for tests
     */
    @WebSocket
    public static class TestWebSocketHandler {
        private final MessageCapture capture;
        
        public TestWebSocketHandler(MessageCapture capture) {
            this.capture = capture;
        }
        
        @OnWebSocketConnect
        public void onConnect(Session session) {
            System.out.println("   ✓ WebSocket handler onConnect called");
        }
        
        @OnWebSocketMessage
        public void onMessage(String message) {
            capture.addMessage(message);
        }
        
        @OnWebSocketError
        public void onError(Throwable error) {
            System.err.println("   ❌ WebSocket error: " + error.getMessage());
            error.printStackTrace();
        }
        
        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            System.out.println("   WebSocket closed: " + statusCode + " - " + reason);
        }
    }
    
    /**
     * Captures and analyzes WebSocket messages
     */
    private static class MessageCapture {
        public final List<String> allMessages = new CopyOnWriteArrayList<>();
        public final CountDownLatch completionLatch = new CountDownLatch(1);
        public volatile String completionMessage = null;
        
        public synchronized void addMessage(String message) {
            allMessages.add(message);
            
            if (message.contains("\"status\":\"COMPLETED\"") || 
                message.contains("\"status\":\"FAILED\"")) {
                completionMessage = message;
                completionLatch.countDown();
            }
        }
        
        public boolean hasStatus(String status) {
            return allMessages.stream()
                .anyMatch(msg -> msg.contains("\"status\":\"" + status + "\""));
        }
        
        public boolean hasVariable(String varName) {
            if (completionMessage == null) return false;
            return completionMessage.contains("\"" + varName + "\"");
        }
    }
}





