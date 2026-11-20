package org.webharvest.server.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.ee9.websocket.api.Session;
import org.eclipse.jetty.ee9.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.ee9.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.ee9.websocket.client.WebSocketClient;
import org.testng.annotations.Test;
import org.webharvest.server.support.EmbeddedIdeTestServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * TDD Test: Session Tracking Integration
 * 
 * Verifies that:
 * - ScraperSession is created during execution
 * - SessionMetrics are tracked (duration, elements processed, processing rate)
 * - SessionInfo is sent via WebSocket in COMPLETED message
 * - Frontend receives complete session data
 */
public class SessionTrackingIntegrationTest {
    
    private static final EmbeddedIdeTestServer SERVER = EmbeddedIdeTestServer.getInstance();
    private static final String BASE_URL = SERVER.getBaseHttpUrl();
    private static final String WS_BASE_URL = SERVER.getBaseWsUrl();
    
    @Test
    public void testSessionTrackingInExecution() throws Exception {
        System.out.println("\n========================================");
        System.out.println("🧪 TDD: Session Tracking Test");
        System.out.println("========================================\n");
        
        // Step 1: POST /api/execute
        System.out.println("1️⃣  Sending POST to start execution...");
        
        String configXml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">" +
                "<def var=\"test1\">Session Test Value 1</def>" +
                "<def var=\"test2\">Session Test Value 2</def>" +
                "</config>";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("configName", "session-tracking-test.xml");
        requestBody.addProperty("configXml", configXml);
        
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Origin", BASE_URL)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(response.statusCode(), 200, "POST should succeed");
        
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        String executionId = result.get("executionId").getAsString();
        System.out.println("   ✓ Got executionId: " + executionId.substring(0, 8) + "...");
        
        // Step 2: Connect WebSocket and collect messages
        System.out.println("\n2️⃣  Connecting WebSocket...");
        
        String wsUrl = WS_BASE_URL + "/ws/execution?id=" + executionId;
        WebSocketClient wsClient = new WebSocketClient();
        wsClient.start();
        
        List<JsonObject> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(1);
        
        WebSocketAdapter socket = new WebSocketAdapter() {
            @Override
            public void onWebSocketConnect(Session sess) {
                super.onWebSocketConnect(sess);
                System.out.println("   ✓ WebSocket connected!");
                connectLatch.countDown();
            }
            
            @Override
            public void onWebSocketText(String message) {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                receivedMessages.add(json);
                
                if (json.has("status") && "COMPLETED".equals(json.get("status").getAsString())) {
                    System.out.println("   ✓ COMPLETED message received");
                    completeLatch.countDown();
                }
            }
        };
        
        try {
            ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
            upgradeRequest.setHeader("Origin", BASE_URL);
            wsClient.connect(socket, URI.create(wsUrl), upgradeRequest).get(5, TimeUnit.SECONDS);
            assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "WebSocket should connect");
            
            // Step 3: Wait for completion
            System.out.println("\n3️⃣  Waiting for execution to complete...");
            assertTrue(completeLatch.await(10, TimeUnit.SECONDS), "Should receive COMPLETED");
            
            // Step 4: Verify session info in COMPLETED message
            System.out.println("\n4️⃣  Verifying session tracking...");
            
            JsonObject completedMsg = receivedMessages.stream()
                    .filter(msg -> msg.has("status") && "COMPLETED".equals(msg.get("status").getAsString()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No COMPLETED message found"));
            
            // CRITICAL: Session info must be present
            assertTrue(completedMsg.has("sessionInfo"), 
                "❌ COMPLETED message MUST contain sessionInfo!");
            
            JsonObject sessionInfo = completedMsg.getAsJsonObject("sessionInfo");
            assertNotNull(sessionInfo, "sessionInfo must not be null");
            
            // Verify session fields
            assertTrue(sessionInfo.has("sessionId"), "Must have sessionId");
            assertTrue(sessionInfo.has("clientId"), "Must have clientId");
            assertTrue(sessionInfo.has("status"), "Must have status");
            assertTrue(sessionInfo.has("duration"), "Must have duration");
            assertTrue(sessionInfo.has("elementsProcessed"), "Must have elementsProcessed");
            assertTrue(sessionInfo.has("processingRate"), "Must have processingRate");
            
            System.out.println("   ✓ Session ID: " + sessionInfo.get("sessionId").getAsString().substring(0, 8) + "...");
            System.out.println("   ✓ Status: " + sessionInfo.get("status").getAsString());
            System.out.println("   ✓ Duration: " + sessionInfo.get("duration").getAsLong() + "ms");
            System.out.println("   ✓ Elements processed: " + sessionInfo.get("elementsProcessed").getAsInt());
            System.out.println("   ✓ Processing rate: " + sessionInfo.get("processingRate").getAsDouble() + " elem/s");
            
            // Verify metrics make sense
            long duration = sessionInfo.get("duration").getAsLong();
            assertTrue(duration >= 0, "Duration must be >= 0 (can be 0 for very fast executions)");
            assertTrue(duration < 10000, "Duration should be < 10 seconds for this simple config");
            
            int elementsProcessed = sessionInfo.get("elementsProcessed").getAsInt();
            assertTrue(elementsProcessed >= 2, "Should process at least 2 elements (2 def plugins)");
            
            double processingRate = sessionInfo.get("processingRate").getAsDouble();
            assertTrue(processingRate >= 0, "Processing rate must be >= 0");
            
            // Optional: Token usage (if implemented)
            if (completedMsg.has("tokenUsage")) {
                System.out.println("\n   💎 Token usage tracking:");
                JsonObject tokenUsage = completedMsg.getAsJsonObject("tokenUsage");
                System.out.println("   ✓ HTTP requests: " + tokenUsage.get("httpRequests").getAsInt());
                System.out.println("   ✓ HTTP bytes: " + tokenUsage.get("httpBytes").getAsLong());
                System.out.println("   ✓ CPU time: " + tokenUsage.get("cpuTimeMs").getAsLong() + "ms");
                System.out.println("   ✓ Memory peak: " + (tokenUsage.get("memoryPeakBytes").getAsLong() / 1024 / 1024) + "MB");
            }
            
            System.out.println("\n✅ SESSION TRACKING TEST PASSED!");
            System.out.println("   - Session created and tracked");
            System.out.println("   - Metrics collected (duration, elements, rate)");
            System.out.println("   - sessionInfo sent via WebSocket");
            System.out.println("   - Frontend receives complete session data");
            
        } finally {
            wsClient.stop();
        }
    }
}


