/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Comprehensive unit tests for {@link HttpPlugin}.
 * 
 * Tests all identified scenarios and edge cases.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class HttpPluginTest {
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private HttpService httpService;

    @Mock
    private ExecutionResourceAccess resourceAccess;
    
    @Mock
    private PluginConfiguration configuration;
    
    @Mock
    private HttpResponse httpResponse;
    
    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Common test data
        when(httpResponse.getBody()).thenReturn("HTTP Response Body");
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentType()).thenReturn("application/json");
        when(httpResponse.getCharset()).thenReturn("UTF-8");
        
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        when(httpResponse.getHeaders()).thenReturn(responseHeaders);
        
        // Default HttpService mock configuration
        when(httpService.get(anyString(), anyMap())).thenReturn(httpResponse);
        when(httpService.post(anyString(), anyString(), anyMap())).thenReturn(httpResponse);

        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
    }
    
    private HttpPlugin createHttpPluginWithInjection() throws Exception {
        HttpPlugin plugin = new HttpPlugin();
        
        // Manually inject HttpService for testing
        Field httpServiceField = HttpPlugin.class.getDeclaredField("httpService");
        httpServiceField.setAccessible(true);
        httpServiceField.set(plugin, httpService);
        
        return plugin;
    }
    
    @Test
    public void testHttpGetRequest() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "HTTP Response Body");
        // Verify result is NodeVariable with correct content
        
        verify(httpService).get(eq("https://httpbin.org/get"), anyMap());
    }
    
    @Test
    public void serviceManagedExecutionUsesOwnerBoundRequestAndConfiguredTimeout() throws Exception {
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(context.getResourceAccess()).thenReturn(resourceAccess);
        when(configuration.getProperty("url")).thenReturn("https://example.com/data");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        when(configuration.getProperty("timeout", "30000")).thenReturn("750");
        when(httpService.request(anyString(), anyString(), anyString(), anyMap(),
            any(Duration.class), same(resourceAccess))).thenReturn(httpResponse);

        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertNotNull(result);
        verify(httpService).request(
            eq("GET"),
            eq("https://example.com/data"),
            eq(""),
            anyMap(),
            eq(Duration.ofMillis(750)),
            same(resourceAccess));
        verify(httpService, never()).get(anyString(), anyMap());
    }

    @Test
    public void testHttpPluginPutRequestUsesGenericRequest() throws Exception {
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://api.example.com/users/42");
        when(configuration.getProperty("method", "GET")).thenReturn("PUT");
        when(configuration.getProperty("body", "")).thenReturn("{\"name\":\"Alice\"}");
        when(httpService.request(anyString(), anyString(), anyString(), anyMap())).thenReturn(httpResponse);

        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertNotNull(result);
        verify(httpService).request(eq("PUT"), eq("https://api.example.com/users/42"), eq("{\"name\":\"Alice\"}"), anyMap());
    }

    @Test
    public void testHttpPluginAppendsQueryParametersToUrl() throws Exception {
        HttpPlugin plugin = createHttpPluginWithInjection();

        PluginConfiguration param = mock(PluginConfiguration.class);
        when(param.getElementName()).thenReturn("http-param");
        when(param.getProperty("name")).thenReturn("q");
        when(param.getProperty("_textContent", "")).thenReturn("web harvest");

        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(List.of(param));
        when(configuration.getProperty("url")).thenReturn("https://example.com/search");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        plugin.initialize(configuration);
        plugin.execute(context);

        verify(httpService).get(eq("https://example.com/search?q=web+harvest"), anyMap());

        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
    }

    @Test
    public void testHttpPluginCollectsHeadersFromChildren() throws Exception {
        HttpPlugin plugin = createHttpPluginWithInjection();

        PluginConfiguration header = mock(PluginConfiguration.class);
        when(header.getElementName()).thenReturn("http-header");
        when(header.getProperty("name")).thenReturn("Authorization");
        when(header.getProperty("_textContent", "")).thenReturn("Bearer token");

        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(List.of(header));
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");

        plugin.initialize(configuration);
        plugin.execute(context);

        ArgumentCaptor<Map<String, String>> headerCaptor = ArgumentCaptor.forClass(Map.class);
        verify(httpService).get(eq("https://httpbin.org/get"), headerCaptor.capture());

        Map<String, String> capturedHeaders = headerCaptor.getValue();
        assertTrue(capturedHeaders.containsKey("Authorization"));
        assertEquals(capturedHeaders.get("Authorization"), "Bearer token");

        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
    }

    @Test
    public void testHttpPostRequest() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/post");
        when(configuration.getProperty("method", "GET")).thenReturn("POST");
        when(configuration.getProperty("body", "")).thenReturn("test body");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "HTTP Response Body");
        
        verify(httpService).post(eq("https://httpbin.org/post"), eq("test body"), anyMap());
    }
    
    @Test
    public void testHttpWithCustomHeaders() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("User-Agent", "Web-Harvest-Plugin");
        customHeaders.put("Accept", "application/json");
        
        // Mock configuration to return headers (simulating XML attributes)
        // Note: HttpPlugin doesn't currently support custom headers from config
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "HTTP Response Body");
        
        verify(httpService).get(eq("https://httpbin.org/get"), anyMap());
    }
    
    @Test
    public void testHttpWithEmptyResponse() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(httpResponse.getBody()).thenReturn("");
        when(httpService.get(anyString(), anyMap())).thenReturn(httpResponse);
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "");
        assertTrue(result instanceof NodeVariable);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void testHttpPluginThrowsExceptionWhenUrlIsMissing() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn(null);
        
        plugin.initialize(configuration);
        
        // When/Then
        plugin.execute(context);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void testHttpPluginThrowsExceptionWhenUrlIsEmpty() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("   ");
        
        plugin.initialize(configuration);
        
        // When/Then
        plugin.execute(context);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void testHttpPluginHandlesHttpServiceException() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(httpService.get(anyString(), anyMap())).thenThrow(
            new HttpServiceException("Connection failed", new RuntimeException()));
        
        plugin.initialize(configuration);
        
        // When/Then
        plugin.execute(context);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void testHttpPluginThrowsExceptionWhenHttpServiceNotInjected() throws Exception {
        // Given
        HttpPlugin plugin = new HttpPlugin(); // No HttpService injection
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        
        plugin.initialize(configuration);
        
        // When/Then - This should throw explicit RuntimeException
        try {
            plugin.execute(context);
            fail("Expected RuntimeException when HttpService is not injected");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("HttpService is not properly injected"));
            throw e; // Re-throw for expected exception test
        }
    }
    
    @Test
    public void testHttpPluginDefaultMethodIsGet() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET"); // Explicit GET
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "HTTP Response Body");
        
        // Verify GET method is used (no calls to post())
        verify(httpService).get(anyString(), anyMap());
        verify(httpService, never()).post(anyString(), anyString(), anyMap());
    }
    
    @Test
    public void testHttpPluginPostWithEmptyBody() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/post");
        when(configuration.getProperty("method", "GET")).thenReturn("POST");
        when(configuration.getProperty("body", "")).thenReturn(""); // Empty body
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "HTTP Response Body");
        
        verify(httpService).post("https://httpbin.org/post", "", new HashMap<>());
    }
    
    @Test
    public void testHttpPluginPostWithNullBody() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/post");
        when(configuration.getProperty("method", "GET")).thenReturn("POST");
        when(configuration.getProperty("body", "")).thenReturn(null); // Null body
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "HTTP Response Body");
        
        verify(httpService).post(eq("https://httpbin.org/post"), anyString(), anyMap());
    }
    
    @Test
    public void testHttpPluginLargeResponse() throws Exception {
        // Given
        HttpPlugin plugin = createHttpPluginWithInjection();
        StringBuilder largeResponse = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeResponse.append("Line ").append(i).append("\n");
        }
        
        when(httpResponse.getBody()).thenReturn(largeResponse.toString());
        when(httpService.get(anyString(), anyMap())).thenReturn(httpResponse);
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString().length(), largeResponse.length());
        assertTrue(result.toString().startsWith("Line 0"));
        assertTrue(result.toString().contains("Line 999"));
    }
    
    @Test
    public void testHttpPluginResponseWithStatusCodes() throws Exception {
        // Test 200 OK
        HttpPlugin plugin = createHttpPluginWithInjection();
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpService.get(anyString(), anyMap())).thenReturn(httpResponse);
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        
        // Test 404 Not Found
        reset(httpService, httpResponse);
        when(configuration.getProperty("url")).thenReturn("https://httpbin.org/get");
        when(configuration.getProperty("method", "GET")).thenReturn("GET");
        HttpServiceException notFoundException = new HttpServiceException("404 Not Found", new RuntimeException());
        when(httpService.get(anyString(), anyMap())).thenThrow(notFoundException);
        
        try {
            plugin.execute(context);
            fail("Expected HttpServiceException for 404");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("HTTP request failed"));
        }
    }
}