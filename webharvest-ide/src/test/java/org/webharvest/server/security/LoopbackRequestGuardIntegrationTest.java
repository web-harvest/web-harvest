package org.webharvest.server.security;

import org.testng.annotations.Test;
import org.webharvest.server.support.EmbeddedIdeTestServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Integration tests for the local IDE HTTP and WebSocket origin boundary. */
public class LoopbackRequestGuardIntegrationTest {

    @Test
    public void allowsLocalUiReadRequestWithoutOrigin() throws Exception {
        int status = request("GET", "/api/execute/test-execution/status", null,
                "localhost", null, null);
        assertEquals(status, 200);
    }

    @Test
    public void allowsSameOriginUiReadRequest() throws Exception {
        EmbeddedIdeTestServer server = EmbeddedIdeTestServer.getInstance();
        int status = request("GET", "/api/execute/test-execution/status",
                "http://localhost:" + server.getPort(), "localhost", null, null);
        assertEquals(status, 200);
    }

    @Test
    public void rejectsForeignOriginTextPlainJsonExecutionRequest() throws Exception {
        String body = "{\"configXml\":\"<config/>\",\"configName\":\"untrusted.xml\"}";
        int status = request("POST", "/api/execute/", "https://attacker.example",
                "localhost", "text/plain", body);
        assertEquals(status, 403);
    }

    @Test
    public void rejectsOpaqueNullOrigin() throws Exception {
        int status = request("GET", "/api/execute/test-execution/status", "null",
                "localhost", null, null);
        assertEquals(status, 403);
    }

    @Test
    public void rejectsStateChangingRequestWithoutOrigin() throws Exception {
        int status = request("POST", "/api/execute/", null,
                "localhost", "text/plain", "{\"configXml\":\"<config/>\"}");
        assertEquals(status, 403);
    }

    @Test
    public void acceptsDocumentedViteDevelopmentOrigin() throws Exception {
        // Invalid JSON stops in ExecutionServlet before an ExecutionRequest is queued.
        int status = request("POST", "/api/execute/", "http://localhost:3000",
                "localhost", "application/json", "{\"missing\":\"configXml\"}");
        assertEquals(status, 500);
    }

    @Test
    public void rejectsTextPlainExecutionBodyEvenFromTrustedOrigin() throws Exception {
        String body = "{\"configXml\":\"<config/>\",\"configName\":\"local.xml\"}";
        int status = request("POST", "/api/execute/", "http://localhost:3000",
                "localhost", "text/plain", body);
        assertEquals(status, 415);
    }

    @Test
    public void rejectsDnsRebindingHostAuthority() throws Exception {
        int status = request("GET", "/api/execute/test-execution/status", "http://localhost:3000",
                "attacker.example", null, null);
        assertEquals(status, 403);
    }

    @Test
    public void rejectsForeignOriginOnExecutionWebSocketHandshake() throws Exception {
        int status = requestWebSocket("/ws/execution/test-execution", "https://attacker.example");
        assertEquals(status, 403);
    }

    @Test
    public void rejectsForeignOriginOnVisualModelWebSocketHandshake() throws Exception {
        int status = requestWebSocket("/ws/visual/model/?clientType=agent",
                "https://attacker.example");
        assertEquals(status, 403);
    }

    private static int request(String method, String path, String origin,
                               String host, String contentType, String body) throws Exception {
        return exchange(method, path, origin, host, contentType, body, false);
    }

    private static int requestWebSocket(String path, String origin) throws Exception {
        return exchange("GET", path, origin, "localhost", null, null, true);
    }

    private static int exchange(String method, String path, String origin,
                                String host, String contentType, String body,
                                boolean websocket) throws Exception {
        EmbeddedIdeTestServer server = EmbeddedIdeTestServer.getInstance();
        try (Socket socket = new Socket("127.0.0.1", server.getPort())) {
            socket.setSoTimeout(5000);
            OutputStream output = socket.getOutputStream();
            StringBuilder request = new StringBuilder()
                    .append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
                    .append("Host: ").append(host).append(':').append(server.getPort()).append("\r\n")
                    .append("Connection: ").append(websocket ? "Upgrade" : "close").append("\r\n");
            if (origin != null) {
                request.append("Origin: ").append(origin).append("\r\n");
            }
            if (websocket) {
                request.append("Upgrade: websocket\r\n")
                        .append("Sec-WebSocket-Version: 13\r\n")
                        .append("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n");
            }
            if (contentType != null) {
                request.append("Content-Type: ").append(contentType).append("\r\n");
            }
            if (body != null) {
                request.append("Content-Length: ")
                        .append(body.getBytes(StandardCharsets.UTF_8).length)
                        .append("\r\n");
            }
            request.append("\r\n");
            if (body != null) {
                request.append(body);
            }
            output.write(request.toString().getBytes(StandardCharsets.UTF_8));
            output.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            String statusLine = reader.readLine();
            assertTrue(statusLine != null && statusLine.startsWith("HTTP/1."),
                    "Expected an HTTP status line but received: " + statusLine);
            String[] parts = statusLine.split(" ");
            return Integer.parseInt(parts[1]);
        } catch (IOException e) {
            throw new AssertionError("IDE test request failed", e);
        }
    }
}
