package org.webharvest.server.api;

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
 * LIVE Server WebSocket Test
 * 
 * Tests WebSocket connection to RUNNING IDE server on port 8080.
 * Run this while IDE is running: java -jar webharvest-ide-2.2.0.jar
 */
public class LiveServerWebSocketTest {
    
    private static final EmbeddedIdeTestServer SERVER = EmbeddedIdeTestServer.getInstance();
    private static final String BASE_URL = SERVER.getBaseHttpUrl();
    private static final String WS_BASE_URL = SERVER.getBaseWsUrl();
    
    @Test
    public void testLiveServerWebSocket() throws Exception {
        System.out.println("\n========================================");
        System.out.println("🔴 LIVE SERVER WebSocket Test");
        System.out.println("========================================\n");
        
        // Step 1: POST /api/execute
        System.out.println("1️⃣  Sending POST to LIVE server on port 8080...");
        
        String configXml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">" +
                "<def var=\"test\">Live Server Test</def>" +
                "</config>";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("configName", "live-server-test.xml");
        requestBody.addProperty("configXml", configXml);
        
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Origin", BASE_URL)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("   Response code: " + response.statusCode());
        System.out.println("   Response body: " + response.body());
        
        assertEquals(response.statusCode(), 200, "POST should succeed");
        
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(result.has("executionId"), "Should have executionId");
        
        String executionId = result.get("executionId").getAsString();
        System.out.println("   ✓ Got executionId: " + executionId.substring(0, 8) + "...");
        
        // Step 2: Connect WebSocket
        System.out.println("\n2️⃣  Connecting WebSocket to LIVE server...");
        
        String wsUrl = WS_BASE_URL + "/ws/execution?id=" + executionId;
        System.out.println("   WS URL: " + wsUrl);
        
        WebSocketClient wsClient = new WebSocketClient();
        wsClient.start();
        
        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(1);
        
        WebSocketAdapter socket = new WebSocketAdapter() {
            @Override
            public void onWebSocketConnect(Session sess) {
                super.onWebSocketConnect(sess);
                System.out.println("   ✓ WebSocket onConnect called!");
                connectLatch.countDown();
            }
            
            @Override
            public void onWebSocketText(String message) {
                System.out.println("   📨 Message: " + message.substring(0, Math.min(80, message.length())) + "...");
                receivedMessages.add(message);
                
                if (message.contains("COMPLETED")) {
                    completeLatch.countDown();
                }
            }
            
            @Override
            public void onWebSocketError(Throwable cause) {
                System.err.println("   ❌ WebSocket ERROR: " + cause.getMessage());
                cause.printStackTrace();
            }
            
            @Override
            public void onWebSocketClose(int statusCode, String reason) {
                System.out.println("   🔌 WebSocket closed: " + statusCode + " - " + reason);
            }
        };
        
        try {
            ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
            upgradeRequest.setHeader("Origin", BASE_URL);
            wsClient.connect(socket, URI.create(wsUrl), upgradeRequest).get(5, TimeUnit.SECONDS);
            System.out.println("   ✓ WebSocket connect() returned successfully");
            
            // Wait for onConnect
            boolean connected = connectLatch.await(5, TimeUnit.SECONDS);
            assertTrue(connected, "WebSocket should connect within 5 seconds");
            
            // Wait for completion
            System.out.println("\n3️⃣  Waiting for execution to complete...");
            boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "Should receive COMPLETED message");
            
            System.out.println("\n4️⃣  Verifying messages:");
            System.out.println("   Total messages: " + receivedMessages.size());
            for (int i = 0; i < receivedMessages.size(); i++) {
                System.out.println("   [" + i + "] " + receivedMessages.get(i).substring(0, Math.min(100, receivedMessages.get(i).length())));
            }
            
            assertTrue(receivedMessages.size() > 0, "Should receive messages");
            
            System.out.println("\n✅ LIVE SERVER TEST PASSED!");
            
        } finally {
            wsClient.stop();
        }
    }
}


