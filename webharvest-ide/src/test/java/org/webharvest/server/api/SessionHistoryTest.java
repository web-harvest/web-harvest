package org.webharvest.server.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.testng.annotations.Test;
import org.webharvest.server.support.EmbeddedIdeTestServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * TDD Test: Session History API
 * 
 * <p>Verifies that the IDE maintains a history of executed sessions
 * and provides an API endpoint to retrieve them.</p>
 * 
 * <h3>Requirements:</h3>
 * <ul>
 *   <li>GET /api/sessions/history returns list of sessions</li>
 *   <li>Each session contains: sessionId, timestamp, status, duration, configName</li>
 *   <li>Sessions are ordered by timestamp (newest first)</li>
 *   <li>Supports limit parameter (default 20, max 100)</li>
 *   <li>Supports status filter (COMPLETED, FAILED, etc.)</li>
 * </ul>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class SessionHistoryTest {
    
    private static final EmbeddedIdeTestServer SERVER = EmbeddedIdeTestServer.getInstance();
    private static final String BASE_URL = SERVER.getBaseHttpUrl();
    
    /**
     * Test: GET /api/sessions/history returns session list
     * 
     * <p>TDD Phase: RED → GREEN → REFACTOR</p>
     */
    @Test
    public void testSessionHistoryEndpoint() throws Exception {
        System.out.println("\n========================================");
        System.out.println("🧪 TDD: Session History API Test");
        System.out.println("========================================\n");
        
        HttpClient httpClient = HttpClient.newHttpClient();
        
        // Step 1: Execute 3 test configurations to create history
        System.out.println("1️⃣  Creating test sessions...");
        
        for (int i = 1; i <= 3; i++) {
            String configXml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">" +
                    "<def var=\"test" + i + "\">Test Session " + i + "</def>" +
                    "</config>";
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("configName", "test-session-" + i + ".xml");
            requestBody.addProperty("configXml", configXml);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/execute"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200, "Execution " + i + " should succeed");
            
            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            String executionId = result.get("executionId").getAsString();
            System.out.println("   ✓ Session " + i + " created: " + executionId.substring(0, 8) + "...");
            
        }
        
        // Step 2: Query session history
        System.out.println("\n2️⃣  Querying session history...");
        
        JsonObject historyJson = null;
        long historyDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        do {
            HttpRequest historyRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/sessions/history?limit=10"))
                    .GET()
                    .build();

            HttpResponse<String> historyResponse = httpClient.send(historyRequest,
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(historyResponse.statusCode(), 200, "History endpoint should return 200");
            historyJson = JsonParser.parseString(historyResponse.body()).getAsJsonObject();

            if (historyJson.get("total").getAsInt() >= 3
                    && historyJson.getAsJsonArray("sessions").size() >= 3) {
                break;
            }
            if (System.nanoTime() >= historyDeadline) {
                break;
            }
            Thread.sleep(100);
        } while (true);

        // Step 3: Verify response structure
        System.out.println("\n3️⃣  Verifying response structure...");
        
        assertTrue(historyJson.has("sessions"), "Response must have 'sessions' field");
        assertTrue(historyJson.has("total"), "Response must have 'total' count");
        
        JsonArray sessions = historyJson.getAsJsonArray("sessions");
        int total = historyJson.get("total").getAsInt();
        
        System.out.println("   ✓ Total sessions: " + total);
        System.out.println("   ✓ Sessions returned: " + sessions.size());
        
        assertTrue(sessions.size() >= 3, "Should have at least 3 sessions");
        assertTrue(total >= 3, "Total count should be at least 3");
        
        // Step 4: Verify session structure
        System.out.println("\n4️⃣  Verifying session data...");
        
        JsonObject firstSession = sessions.get(0).getAsJsonObject();
        
        // Required fields
        assertTrue(firstSession.has("sessionId"), "Session must have sessionId");
        assertTrue(firstSession.has("timestamp"), "Session must have timestamp");
        assertTrue(firstSession.has("status"), "Session must have status");
        assertTrue(firstSession.has("duration"), "Session must have duration");
        assertTrue(firstSession.has("configName"), "Session must have configName");
        assertTrue(firstSession.has("elementsProcessed"), "Session must have elementsProcessed");
        
        String sessionId = firstSession.get("sessionId").getAsString();
        String timestamp = firstSession.get("timestamp").getAsString();
        String status = firstSession.get("status").getAsString();
        long duration = firstSession.get("duration").getAsLong();
        String configName = firstSession.get("configName").getAsString();
        int elements = firstSession.get("elementsProcessed").getAsInt();
        
        System.out.println("   ✓ Session ID: " + sessionId.substring(0, 8) + "...");
        System.out.println("   ✓ Timestamp: " + timestamp);
        System.out.println("   ✓ Status: " + status);
        System.out.println("   ✓ Duration: " + duration + "ms");
        System.out.println("   ✓ Config: " + configName);
        System.out.println("   ✓ Elements: " + elements);
        
        // Validate values
        assertNotNull(sessionId, "sessionId should not be null");
        assertFalse(sessionId.isEmpty(), "sessionId should not be empty");
        Set<String> expectedStatuses = Set.of("COMPLETED", "STOPPED", "CANCELLED");
        assertTrue(expectedStatuses.contains(status),
                "Status should be COMPLETED, STOPPED or CANCELLED");
        assertTrue(duration >= 0, "Duration should be >= 0");
        assertNotNull(configName, "Config name should not be null");
        assertFalse(configName.isEmpty(), "Config name should not be empty");
        assertTrue(elements >= 0, "Elements processed should be >= 0");
        
        // Step 5: Test limit parameter
        System.out.println("\n5️⃣  Testing limit parameter...");
        
        HttpRequest limitRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/sessions/history?limit=2"))
                .GET()
                .build();
        
        HttpResponse<String> limitResponse = httpClient.send(limitRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        JsonObject limitJson = JsonParser.parseString(limitResponse.body()).getAsJsonObject();
        JsonArray limitedSessions = limitJson.getAsJsonArray("sessions");
        
        assertTrue(limitedSessions.size() <= 2, "Should respect limit parameter");
        System.out.println("   ✓ Limit works: returned " + limitedSessions.size() + " sessions");
        
        // Step 6: Test status filter (optional)
        System.out.println("\n6️⃣  Testing status filter...");
        
        HttpRequest filterRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/sessions/history?status=COMPLETED"))
                .GET()
                .build();
        
        HttpResponse<String> filterResponse = httpClient.send(filterRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        JsonObject filterJson = JsonParser.parseString(filterResponse.body()).getAsJsonObject();
        JsonArray filteredSessions = filterJson.getAsJsonArray("sessions");
        
        // All returned sessions should have status COMPLETED
        for (int i = 0; i < filteredSessions.size(); i++) {
            JsonObject session = filteredSessions.get(i).getAsJsonObject();
            assertEquals(session.get("status").getAsString(), "COMPLETED", 
                "Filtered sessions should match status");
        }
        
        System.out.println("   ✓ Status filter works: " + filteredSessions.size() + " COMPLETED sessions");
        
        System.out.println("\n✅ SESSION HISTORY TEST PASSED!");
        System.out.println("   - History endpoint works");
        System.out.println("   - Session data complete");
        System.out.println("   - Limit parameter works");
        System.out.println("   - Status filter works");
    }
    
    /**
     * Test: Session history ordering (newest first)
     */
    @Test
    public void testSessionHistoryOrdering() throws Exception {
        System.out.println("\n========================================");
        System.out.println("🧪 TDD: Session History Ordering Test");
        System.out.println("========================================\n");
        
        HttpClient httpClient = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/sessions/history?limit=5"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray sessions = json.getAsJsonArray("sessions");
        
        if (sessions.size() > 1) {
            // Verify sessions are ordered by timestamp (newest first)
            for (int i = 0; i < sessions.size() - 1; i++) {
                String timestamp1 = sessions.get(i).getAsJsonObject().get("timestamp").getAsString();
                String timestamp2 = sessions.get(i + 1).getAsJsonObject().get("timestamp").getAsString();
                
                assertTrue(timestamp1.compareTo(timestamp2) >= 0, 
                    "Sessions should be ordered newest first");
            }
            
            System.out.println("✅ Sessions are properly ordered (newest first)");
        }
    }
}

