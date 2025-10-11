package org.webharvest.plugin.services;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.ioc.ScraperModule;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * TDD Test for HttpClient 5.x migration.
 * 
 * OBJECTIVE: Verify HttpServiceImpl works correctly after migration from
 * Apache HttpClient 4.5.14 to HttpClient5 5.5.1
 * 
 * Migration changes tested:
 * 1. API package changes: org.apache.http.* → org.apache.hc.*
 * 2. Method name changes: getStatusLine().getStatusCode() → getCode()
 * 3. Method name changes: getAllHeaders() → getHeaders()
 * 4. Entity API changes (if any)
 * 
 * TDD Approach:
 * 1. Write test with expected behavior
 * 2. Update HttpClient5 dependency
 * 3. Run test (will FAIL - compilation errors)
 * 4. Fix HttpServiceImpl implementation
 * 5. Run test (should PASS)
 * 6. Run all HTTP tests to verify no regressions
 * 
 * @author Robert Bala
 * @since 2.2.1
 */
public class HttpClient5MigrationTest {

    private HttpService httpService;

    @BeforeMethod
    public void setUp() {
        Injector injector = Guice.createInjector(new ScraperModule("."));
        httpService = injector.getInstance(HttpService.class);
    }

    @Test
    public void testHttpServiceStillInjectable() {
        // GIVEN: Guice injector with ScraperModule
        // WHEN: Get HttpService instance
        // THEN: Should work with HttpClient 5.x
        assertNotNull(httpService, "HttpService should be injectable with HttpClient 5.x");
        assertTrue(httpService instanceof HttpServiceImpl, 
            "HttpService should still be HttpServiceImpl");
    }
    
    @Test(groups = "network")
    public void testBasicHttpGetWithHttpClient5() throws Exception {
        // GIVEN: HttpService using HttpClient 5.x
        assertNotNull(httpService);
        
        // WHEN: Execute simple HTTP GET
        String url = "https://httpbin.org/get";
        HttpResponse response = httpService.get(url, null);
        
        // THEN: Response should work as before
        assertNotNull(response, "Response should not be null");
        assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        String body = response.getBody();
        assertNotNull(body, "Body should not be null");
        assertTrue(body.length() > 0, "Body should have content");
        assertTrue(body.contains("httpbin"), "Should contain httpbin response");
    }
    
    @Test(groups = "network")
    public void testHttpPostWithHttpClient5() throws Exception {
        // GIVEN: HttpService using HttpClient 5.x
        assertNotNull(httpService);
        
        // WHEN: Execute HTTP POST with body
        String url = "https://httpbin.org/post";
        String postBody = "{\"test\": \"data\", \"migration\": \"httpclient5\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        HttpResponse response = httpService.post(url, postBody, headers);
        
        // THEN: POST should work correctly
        assertNotNull(response, "Response should not be null");
        assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        String body = response.getBody();
        assertNotNull(body, "Body should not be null");
        assertTrue(body.contains("httpclient5"), "Should echo posted data");
    }
    
    @Test(groups = "network")
    public void testHttpHeadersWithHttpClient5() throws Exception {
        // GIVEN: HttpService using HttpClient 5.x
        assertNotNull(httpService);
        
        // WHEN: Execute GET with custom headers
        String url = "https://httpbin.org/headers";
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test-Header", "HttpClient5Migration");
        headers.put("User-Agent", "WebHarvest/2.2.1");
        
        HttpResponse response = httpService.get(url, headers);
        
        // THEN: Headers should be sent correctly
        assertNotNull(response, "Response should not be null");
        assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        String body = response.getBody();
        assertTrue(body.contains("HttpClient5Migration"), 
            "Response should contain our custom header");
        assertTrue(body.contains("WebHarvest"), 
            "Response should contain our User-Agent");
    }
    
    @Test(groups = "network")
    public void testBinaryContentWithHttpClient5() throws Exception {
        // GIVEN: HttpService using HttpClient 5.x
        assertNotNull(httpService);
        
        // WHEN: Download binary content (image)
        String url = "https://httpbin.org/image/png";
        HttpResponse response = httpService.get(url, null);
        
        // THEN: Binary content should work (Bug #39 verification)
        assertNotNull(response, "Response should not be null");
        assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        byte[] binaryData = response.getBodyAsBinary();
        assertNotNull(binaryData, "Binary data should not be null");
        assertTrue(binaryData.length > 0, "Binary data should not be empty");
        
        // PNG images start with specific byte signature
        assertTrue(binaryData.length > 8, "PNG should be larger than 8 bytes");
        
        // Verify Content-Type header
        String contentType = response.getContentType();
        assertNotNull(contentType, "Content-Type should be present");
        assertTrue(contentType.contains("image/png") || contentType.contains("image"), 
            "Content-Type should be image");
    }
    
    @Test(groups = "network")
    public void testHttpErrorCodesWithHttpClient5() throws Exception {
        // GIVEN: HttpService using HttpClient 5.x
        assertNotNull(httpService);
        
        // WHEN: Request returns 404
        String url = "https://httpbin.org/status/404";
        HttpResponse response = httpService.get(url, null);
        
        // THEN: Status code should be correctly returned
        assertNotNull(response, "Response should not be null even for 404");
        assertEquals(response.getStatusCode(), 404, "Status code should be 404");
    }
    
    @Test(groups = "network")
    public void testHttpRedirectHandlingWithHttpClient5() throws Exception {
        // GIVEN: HttpService using HttpClient 5.x
        assertNotNull(httpService);
        
        // WHEN: Request URL that redirects
        String url = "https://httpbin.org/redirect/1";
        HttpResponse response = httpService.get(url, null);
        
        // THEN: Should follow redirect and return final response
        assertNotNull(response, "Response should not be null");
        // HttpClient 5.x follows redirects by default
        assertEquals(response.getStatusCode(), 200, 
            "Should follow redirect and get 200");
    }
}

