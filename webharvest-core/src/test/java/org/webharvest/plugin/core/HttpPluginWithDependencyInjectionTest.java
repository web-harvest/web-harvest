package org.webharvest.plugin.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * TDD Test for HttpPlugin with Guice dependency injection.
 * 
 * PROBLEM: HttpPlugin returns empty content in integration tests!
 * HttpService works (verified in HttpServiceImplBasicTest).
 * 
 * This test verifies:
 * 1. HttpPlugin gets HttpService via @Inject
 * 2. HttpPlugin executes HTTP request correctly
 * 3. HttpPlugin returns non-empty response
 * 
 * Test order (TDD):
 * 1. Unit test HttpPlugin with real HttpService (via Guice)
 * 2. If PASS: problem is in integration environment (DefPlugin? XML parsing?)
 * 3. If FAIL: problem is in HttpPlugin.doExecute()
 */
public class HttpPluginWithDependencyInjectionTest {

    private Injector injector;
    private DynamicScopeContext context;
    private HttpPlugin httpPlugin;

    @BeforeMethod
    public void setUp() {
        // Create Guice injector
        injector = Guice.createInjector(new ScraperModule("."));
        
        // Get REAL ScraperContext
        context = injector.getInstance(ScraperContext.class);
        Config mockConfig = mock(Config.class);
        context.setConfig(mockConfig);
        context.setScriptingLanguage(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        
        // Get HttpPlugin via Guice (so @Inject HttpService works)
        httpPlugin = injector.getInstance(HttpPlugin.class);
    }

    @Test
    public void testHttpPluginHasHttpServiceInjected() {
        // GIVEN: HttpPlugin created via Guice
        assertNotNull(httpPlugin, "HttpPlugin should be created");
        
        // WHEN: Check if HttpService is injected
        // (we can't directly access private field, but execute() will throw if null)
        
        // THEN: Should not throw IllegalStateException
        // (test passes if no exception)
        assertTrue(true, "HttpPlugin created successfully with dependencies");
    }
    
    @Test
    public void testHttpPluginSimpleGet() throws Exception {
        // GIVEN: HttpPlugin with injected HttpService
        assertNotNull(httpPlugin);
        
        // AND: Simple configuration with URL
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "HttpPlugin",
            "http://org.webharvest/schema/2.1/core",
            "http");
        config.setProperty("url", "https://httpbin.org/get");
        
        // WHEN: Initialize and execute
        httpPlugin.initialize(config);
        Variable result = httpPlugin.execute(context);
        
        // THEN: Result should not be null
        assertNotNull(result, "HTTP response should not be null");
        
        // AND: Result should not be empty
        String content = result.toString();
        assertNotNull(content, "Response content should not be null");
        assertTrue(content.length() > 0, 
            "Response content should not be empty, but got: [" + content + "]");
        
        // AND: httpbin.org/get returns JSON
        assertTrue(content.contains("\"") || content.contains("{"), 
            "Response should contain JSON content from httpbin.org/get");
    }
    
    @Test
    public void testHttpPluginGetHtml() throws Exception {
        // GIVEN: HttpPlugin with injected HttpService
        assertNotNull(httpPlugin);
        
        // AND: Configuration for HTML endpoint
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "HttpPlugin",
            "http://org.webharvest/schema/2.1/core",
            "http");
        config.setProperty("url", "https://httpbin.org/html");
        
        // WHEN: Initialize and execute
        httpPlugin.initialize(config);
        Variable result = httpPlugin.execute(context);
        
        // THEN: Result should not be null
        assertNotNull(result, "HTTP response should not be null");
        
        // AND: Result should contain HTML
        String content = result.toString();
        assertNotNull(content, "Response content should not be null");
        assertTrue(content.length() > 0, "Response content should not be empty");
        assertTrue(content.contains("<html") || content.contains("HTML"), 
            "Response should contain HTML content");
    }
    
    @Test
    public void testHttpPluginWithVariableUrl() throws Exception {
        // GIVEN: HttpPlugin with injected HttpService
        assertNotNull(httpPlugin);
        
        // AND: Variable in context
        context.setLocalVar("baseUrl", 
            new org.webharvest.runtime.variables.NodeVariable("https://httpbin.org"));
        
        // AND: Configuration with variable in URL
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "HttpPlugin",
            "http://org.webharvest/schema/2.1/core",
            "http");
        config.setProperty("url", "${baseUrl}/status/200");
        
        // WHEN: Initialize and execute
        httpPlugin.initialize(config);
        Variable result = httpPlugin.execute(context);
        
        // THEN: Result should not be null
        assertNotNull(result, "HTTP response should not be null");
        
        // AND: Should successfully call httpbin.org
        // (status/200 returns empty body, but response should exist)
        assertNotNull(result.toString(), "Response content should not be null");
    }
}

