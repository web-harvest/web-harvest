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
 * TDD Test for HttpService basic functionality.
 * 
 * PROBLEM: HttpPlugin returns empty content in integration tests!
 * This test verifies that HttpService itself works correctly.
 * 
 * Test order (TDD):
 * 1. Unit test HttpService directly
 * 2. If PASS, problem is elsewhere (HttpPlugin)
 * 3. If FAIL, problem is in HttpService implementation
 * 
 * NOTE: These tests require network access to httpbin.org.
 * They may fail if:
 * - Network is unavailable
 * - httpbin.org is down or rate-limiting
 * - Firewall blocks access
 * 
 * Use @Test(groups = "network") to skip in offline environments.
 */
public class HttpServiceImplBasicTest {

    private HttpService httpService;

    @BeforeMethod
    public void setUp() {
        // Create Guice injector to get HttpService
        Injector injector = Guice.createInjector(new ScraperModule("."));
        httpService = injector.getInstance(HttpService.class);
    }

    @Test
    public void testHttpServiceIsInjected() {
        // GIVEN: Guice injector with ScraperModule
        // WHEN: Get HttpService instance
        // THEN: Should not be null
        assertNotNull(httpService, "HttpService should be injected by Guice");
        assertTrue(httpService instanceof HttpServiceImpl, 
            "HttpService should be instance of HttpServiceImpl");
    }
    
    @Test(groups = "network")
    public void testBasicHttpGet() throws Exception {
        // GIVEN: HttpService instance
        assertNotNull(httpService);
        
        // WHEN: Execute HTTP GET request
        String url = "https://httpbin.org/get";
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = httpService.get(url, headers);
        
        // THEN: Response should not be null
        assertNotNull(response, "HTTP response should not be null");
        
        // AND: Response should have content
        String content = response.getBody();
        assertNotNull(content, "Response body should not be null");
        assertTrue(content.length() > 0, "Response body should not be empty");
        
        // AND: httpbin.org/get returns JSON
        assertTrue(content.contains("\"") || content.contains("{"), 
            "Response should contain JSON content from httpbin.org/get");
    }
    
    @Test(groups = "network")
    public void testHttpGetHtml() throws Exception {
        // GIVEN: HttpService instance
        assertNotNull(httpService);
        
        // WHEN: Execute HTTP GET request for HTML
        String url = "https://httpbin.org/html";
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = httpService.get(url, headers);
        
        // THEN: Response should not be null
        assertNotNull(response, "HTTP response should not be null");
        
        // AND: Response should have HTML content
        String content = response.getBody();
        assertNotNull(content, "Response body should not be null");
        assertTrue(content.length() > 0, "Response body should not be empty");
        assertTrue(content.contains("<html") || content.contains("HTML"), 
            "Response should contain HTML content");
    }
    
    @Test(groups = "network")
    public void testHttpStatus200() throws Exception {
        // GIVEN: HttpService instance
        assertNotNull(httpService);
        
        // WHEN: Execute HTTP GET request
        String url = "https://httpbin.org/status/200";
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = httpService.get(url, headers);
        
        // THEN: Response should not be null
        assertNotNull(response, "HTTP response should not be null");
        
        // AND: Status code should be 200
        assertEquals(response.getStatusCode(), 200, 
            "HTTP status code should be 200");
    }
}

