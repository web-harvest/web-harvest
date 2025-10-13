package org.webharvest.plugin.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.ioc.ScraperModule;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.*;

/**
 * TDD Test for HttpClient 5.x migration using WireMock for reliable testing.
 * 
 * CRITICAL: Uses mock HTTP server instead of external httpbin.org to ensure:
 * - Tests are reliable (no network dependencies)
 * - Tests are fast (no external calls)
 * - Tests work offline
 * - No rate limiting issues
 * 
 * @author Robert Bala
 * @since 2.2.1
 */
public class HttpClient5MigrationWithMockTest {

    private WireMockServer wireMockServer;
    private HttpService httpService;
    private String baseUrl;

    @BeforeMethod
    public void setUp() {
        // Start WireMock server on random port
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        baseUrl = "http://localhost:" + wireMockServer.port();
        
        // Create HttpService via Guice
        Injector injector = Guice.createInjector(new ScraperModule("."));
        httpService = injector.getInstance(HttpService.class);
    }

    @AfterMethod
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    public void testHttpClient5_BasicGet() throws Exception {
        // GIVEN: Mock server returns JSON
        stubFor(get(urlEqualTo("/test"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"ok\", \"version\": \"5.x\"}")));
        
        // WHEN: Execute HTTP GET with HttpClient 5.x
        HttpResponse response = httpService.get(baseUrl + "/test", null);
        
        // THEN: Response should work correctly
        assertNotNull(response);
        assertEquals(response.getStatusCode(), 200);
        String body = response.getBody();
        assertTrue(body.contains("5.x"), "Should contain version");
        assertTrue(body.contains("ok"), "Should contain status");
    }
    
    @Test
    public void testHttpClient5_PostWithBody() throws Exception {
        // GIVEN: Mock server echoes POST data
        stubFor(post(urlEqualTo("/echo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"received\": \"data\"}")));
        
        // WHEN: Execute HTTP POST with body
        String postData = "{\"test\": \"httpclient5\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        HttpResponse response = httpService.post(baseUrl + "/echo", postData, headers);
        
        // THEN: POST should work with HttpClient 5.x
        assertNotNull(response);
        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBody().contains("received"));
    }
    
    @Test
    public void testHttpClient5_CustomHeaders() throws Exception {
        // GIVEN: Mock server
        stubFor(get(urlEqualTo("/headers"))
            .withHeader("X-Custom-Header", equalTo("test-value"))
            .withHeader("User-Agent", equalTo("WebHarvest/2.2.1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("headers ok")));
        
        // WHEN: Send request with custom headers
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Header", "test-value");
        headers.put("User-Agent", "WebHarvest/2.2.1");
        
        HttpResponse response = httpService.get(baseUrl + "/headers", headers);
        
        // THEN: Headers should be sent correctly
        assertNotNull(response);
        assertEquals(response.getStatusCode(), 200);
        verify(getRequestedFor(urlEqualTo("/headers"))
            .withHeader("X-Custom-Header", equalTo("test-value")));
    }
    
    @Test
    public void testHttpClient5_BinaryContent() throws Exception {
        // GIVEN: Mock server returns binary data (PNG image signature)
        byte[] pngData = new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D // start of IHDR chunk
        };
        
        stubFor(get(urlEqualTo("/image.png"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "image/png")
                .withBody(pngData)));
        
        // WHEN: Download binary content
        HttpResponse response = httpService.get(baseUrl + "/image.png", null);
        
        // THEN: Binary data should be preserved (Bug #39)
        assertNotNull(response);
        assertEquals(response.getStatusCode(), 200);
        byte[] binary = response.getBodyAsBinary();
        assertNotNull(binary);
        assertTrue(binary.length > 0);
        
        // Verify PNG signature
        assertEquals(binary[0], (byte)0x89);
        assertEquals(binary[1], (byte)0x50);
        assertEquals(binary[2], (byte)0x4E);
        assertEquals(binary[3], (byte)0x47);
    }
    
    @Test
    public void testHttpClient5_StatusCodes() throws Exception {
        // Test 200 OK
        stubFor(get(urlEqualTo("/200"))
            .willReturn(aResponse().withStatus(200).withBody("OK")));
        
        HttpResponse response200 = httpService.get(baseUrl + "/200", null);
        assertEquals(response200.getStatusCode(), 200);
        
        // Test 404 Not Found
        stubFor(get(urlEqualTo("/404"))
            .willReturn(aResponse().withStatus(404).withBody("Not Found")));
        
        HttpResponse response404 = httpService.get(baseUrl + "/404", null);
        assertEquals(response404.getStatusCode(), 404);
        
        // Test 500 Internal Server Error
        stubFor(get(urlEqualTo("/500"))
            .willReturn(aResponse().withStatus(500).withBody("Error")));
        
        HttpResponse response500 = httpService.get(baseUrl + "/500", null);
        assertEquals(response500.getStatusCode(), 500);
    }
    
    @Test
    public void testHttpClient5_ResponseHeaders() throws Exception {
        // GIVEN: Mock server with custom headers
        stubFor(get(urlEqualTo("/with-headers"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("X-Custom-Response", "test123")
                .withHeader("Content-Type", "text/plain; charset=UTF-8")
                .withBody("content")));
        
        // WHEN: Execute request
        HttpResponse response = httpService.get(baseUrl + "/with-headers", null);
        
        // THEN: Response headers should be accessible
        assertNotNull(response);
        assertEquals(response.getHeader("X-Custom-Response"), "test123");
        assertTrue(response.getContentType().contains("text/plain"));
        assertEquals(response.getCharset(), "UTF-8");
    }
    
    @Test
    public void testHttpClient5_EmptyResponse() throws Exception {
        // GIVEN: Mock server returns empty body
        stubFor(get(urlEqualTo("/empty"))
            .willReturn(aResponse()
                .withStatus(204)
                .withBody("")));
        
        // WHEN: Execute request
        HttpResponse response = httpService.get(baseUrl + "/empty", null);
        
        // THEN: Empty response should be handled
        assertNotNull(response);
        assertEquals(response.getStatusCode(), 204);
        String body = response.getBody();
        assertNotNull(body);
        assertEquals(body, "");
    }
    
    @Test
    public void testHttpClient5_LargeResponse() throws Exception {
        // GIVEN: Mock server returns large response (1MB)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append(" of the large response.\n");
        }
        
        stubFor(get(urlEqualTo("/large"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(largeContent.toString())));
        
        // WHEN: Download large content
        HttpResponse response = httpService.get(baseUrl + "/large", null);
        
        // THEN: Large content should be handled
        assertNotNull(response);
        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBody().length() > 100000);
        assertTrue(response.getBodyAsBinary().length > 100000);
    }
}






