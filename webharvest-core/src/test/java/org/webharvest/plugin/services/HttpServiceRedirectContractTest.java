package org.webharvest.plugin.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Deterministic loopback tests for the HTTP service redirect contract.
 */
public class HttpServiceRedirectContractTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger finalRequestCount = new AtomicInteger();
    private final AtomicReference<String> finalApiKey = new AtomicReference<>();

    @BeforeClass(alwaysRun = true)
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", "/final");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/final", exchange -> {
            finalRequestCount.incrementAndGet();
            finalApiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
            byte[] body = "redirected".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void resetObservations() {
        finalRequestCount.set(0);
        finalApiKey.set(null);
    }

    @Test
    public void followsRedirectWhenEnabled() throws Exception {
        try (HttpServiceImpl service = new HttpServiceImpl(true)) {
            HttpResponse response = service.get(baseUrl + "/redirect", Map.of());
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getBody(), "redirected");
            assertEquals(finalRequestCount.get(), 1);
        }
    }

    @Test
    public void doesNotFollowRedirectWhenDisabled() throws Exception {
        try (HttpServiceImpl service = new HttpServiceImpl(false)) {
            HttpResponse response = service.get(baseUrl + "/redirect", Map.of());
            assertEquals(response.getStatusCode(), 302);
            assertEquals(finalRequestCount.get(), 0);
        }
    }

    @Test
    public void appliesConfiguredDefaultHeadersToRedirectRequests() throws Exception {
        try (HttpServiceImpl service = new HttpServiceImpl(true, Map.of("X-Api-Key", "contract-test"))) {
            HttpResponse response = service.get(baseUrl + "/redirect", Map.of());
            assertEquals(response.getStatusCode(), 200);
            assertEquals(finalApiKey.get(), "contract-test");
            assertEquals(finalRequestCount.get(), 1);
        }
    }

    @Test
    public void acceptsNullDefaultHeaders() throws Exception {
        try (HttpServiceImpl service = new HttpServiceImpl(true, null)) {
            HttpResponse response = service.get(baseUrl + "/redirect", Map.of());
            assertEquals(response.getStatusCode(), 200);
            assertNull(finalApiKey.get());
            assertEquals(finalRequestCount.get(), 1);
        }
    }
}
