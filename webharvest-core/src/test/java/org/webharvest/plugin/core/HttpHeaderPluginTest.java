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

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class HttpHeaderPluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    private HttpHeaderPlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        plugin = new HttpHeaderPlugin();
        
        when(configuration.getProperty("name")).thenReturn("Content-Type");
        when(configuration.getProperty("id")).thenReturn(null);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "http-header");
        assertEquals(metadata.getName(), "HTTP Header Plugin");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getDescription(), "HTTP header plugin that defines HTTP headers for HTTP requests");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "http-header");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredAttributes().length, 1);
        assertEquals(schema.getAttributeDefinitions().length, 2); // name, id
    }

    @Test
    public void testInitialize() throws PluginException {
        // Given
        when(configuration.getProperty("name")).thenReturn("Authorization");
        
        // When
        plugin.initialize(configuration);
        
        // Then
        // Should not throw exception
    }

    @Test
    public void testValidateConfiguration() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn("User-Agent");
        
        // When & Then
        plugin.validateConfiguration(configuration);
        // Should not throw exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        // When & Then
        plugin.validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithEmptyName() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn("");
        
        // When & Then
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullName() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn(null);
        
        // When & Then
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithInvalidName() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn("Invalid Header Name!");
        
        // When & Then
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void testExecuteWithValidHeader() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("Content-Type");
        when(configuration.getProperty("name", "")).thenReturn("Content-Type");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithEmptyName() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("");
        when(configuration.getProperty("name", "")).thenReturn("");
        
        // When & Then
        plugin.initialize(configuration);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithNullName() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn(null);
        when(configuration.getProperty("name", "")).thenReturn("");
        
        // When & Then
        plugin.initialize(configuration);
    }

    @Test
    public void testExecuteWithSpecialCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("X-Custom-Header");
        when(configuration.getProperty("name", "")).thenReturn("X-Custom-Header");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNumericHeader() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("X-API-Version-2");
        when(configuration.getProperty("name", "")).thenReturn("X-API-Version-2");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithHyphenHeader() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("X-Request-ID");
        when(configuration.getProperty("name", "")).thenReturn("X-Request-ID");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithUnderscoreHeader() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("X_Request_ID");
        when(configuration.getProperty("name", "")).thenReturn("X_Request_ID");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithLongHeader() throws PluginException, InterruptedException {
        // Given
        String longHeader = "X-Very-Long-Custom-Header-Name-With-Many-Characters";
        when(configuration.getProperty("name")).thenReturn(longHeader);
        when(configuration.getProperty("name", "")).thenReturn(longHeader);
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithMultipleExecutions() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("Content-Type");
        when(configuration.getProperty("name", "")).thenReturn("Content-Type");
        
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }
}
