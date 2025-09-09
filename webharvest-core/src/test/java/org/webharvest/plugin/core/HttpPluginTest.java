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
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.di.DefaultPluginDependencyInjector;
import org.webharvest.plugin.di.PluginDependencyInjector;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceException;
import org.webharvest.runtime.DynamicScopeContext;

/**
 * Unit tests for HttpPlugin demonstrating dependency injection and mocking.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public class HttpPluginTest {
    
    private HttpPlugin plugin;
    private PluginDependencyInjector injector;
    private HttpService mockHttpService;
    private DynamicScopeContext mockContext;
    
    @BeforeMethod
    public void setUp() {
        plugin = new HttpPlugin();
        injector = new DefaultPluginDependencyInjector();
        mockHttpService = mock(HttpService.class);
        mockContext = mock(DynamicScopeContext.class);
        
        // Register mock HTTP service
        injector.registerSingleton(HttpService.class, mockHttpService);
    }
    
    @Test
    public void testInitialize() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("http", "test", "http");
        config.setProperty("url", "https://example.com");
        
        plugin.initialize(config);
        
        // Verify initialization completed without errors
        assertTrue(plugin.isInitialized());
    }
    
    @Test
    public void testExecuteGetRequest() throws Exception {
        // Setup
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("http", "test", "http");
        config.setProperty("url", "https://example.com");
        config.setProperty("method", "GET");
        
        injector.injectDependencies(plugin);
        plugin.initialize(config);
        
        // Mock HTTP response
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.getBody()).thenReturn("<html>Test response</html>");
        when(mockHttpService.get(eq("https://example.com"), any(Map.class)))
            .thenReturn(mockResponse);
        
        // Execute
        var result = plugin.execute(mockContext);
        
        // Verify
        assertNotNull(result);
        assertEquals("<html>Test response</html>", result.toString());
        verify(mockHttpService).get(eq("https://example.com"), any(Map.class));
    }
    
    @Test
    public void testExecutePostRequest() throws Exception {
        // Setup
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("http", "test", "http");
        config.setProperty("url", "https://example.com");
        config.setProperty("method", "POST");
        config.setProperty("body", "test data");
        
        injector.injectDependencies(plugin);
        plugin.initialize(config);
        
        // Mock HTTP response
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.getBody()).thenReturn("{\"status\":\"success\"}");
        when(mockHttpService.post(eq("https://example.com"), eq("test data"), any(Map.class)))
            .thenReturn(mockResponse);
        
        // Execute
        var result = plugin.execute(mockContext);
        
        // Verify
        assertNotNull(result);
        assertEquals("{\"status\":\"success\"}", result.toString());
        verify(mockHttpService).post(eq("https://example.com"), eq("test data"), any(Map.class));
    }
    
    @Test
    public void testExecuteWithHttpServiceException() throws Exception {
        // Setup
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("http", "test", "http");
        config.setProperty("url", "https://example.com");
        
        injector.injectDependencies(plugin);
        plugin.initialize(config);
        
        // Mock HTTP service to throw exception
        when(mockHttpService.get(any(String.class), any(Map.class)))
            .thenThrow(new HttpServiceException("Connection failed"));
        
        // Execute and verify exception
        try {
            plugin.execute(mockContext);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            System.out.println("Exception message: " + e.getMessage());
            System.out.println("Exception cause: " + e.getCause());
            assertTrue(e.getMessage().contains("Plugin execution failed"));
            assertTrue(e.getCause() instanceof RuntimeException);
            assertTrue(e.getCause().getMessage().contains("HTTP request failed"));
            assertTrue(e.getCause().getCause() instanceof HttpServiceException);
        }
    }
    
    @Test
    public void testValidateConfigurationValid() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("http", "test", "http");
        config.setProperty("url", "https://example.com");
        config.setProperty("method", "GET");
        
        plugin.initialize(config);
        
        // Should not throw exception
        plugin.validateConfiguration(config);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateConfigurationMissingUrl() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("http", "test", "http");
        // Missing URL
        
        plugin.initialize(config);
        plugin.validateConfiguration(config);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateConfigurationInvalidMethod() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("http", "test", "http");
        config.setProperty("url", "https://example.com");
        config.setProperty("method", "INVALID");
        
        plugin.initialize(config);
        plugin.validateConfiguration(config);
    }
    
    @Test
    public void testGetMetadata() {
        var metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals("http", metadata.getId());
        assertEquals("HTTP Plugin", metadata.getName());
        assertTrue(metadata.getDescription().contains("HTTP request"));
        assertEquals(metadata.getTags(), new String[]{"http", "web", "request", "core"});
        assertEquals(metadata.getDependencies(), new String[]{"HttpService"});
    }
}
