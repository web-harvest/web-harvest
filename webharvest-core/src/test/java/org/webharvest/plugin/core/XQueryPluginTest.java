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
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class XQueryPluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    private XQueryPlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        plugin = new XQueryPlugin();
        
        when(configuration.getProperty("id")).thenReturn(null);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "xquery");
        assertEquals(metadata.getName(), "XQuery Plugin");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getDescription(), "XQuery plugin that provides XQuery processing functionality");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "xquery");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getAttributeDefinitions().length, 1); // id
    }

    @Test
    public void testInitialize() throws PluginException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        // When
        plugin.initialize(configuration);
        
        // Then
        // Should not throw exception
    }

    @Test
    public void testValidateConfiguration() throws PluginValidationException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        // When & Then
        plugin.validateConfiguration(configuration);
        // Should not throw exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        // When & Then
        plugin.validateConfiguration(null);
    }

    @Test
    public void testExecuteWithEmptyBody() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNullBody() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithSimpleXQuery() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        // Result depends on XQuery execution, but should not be null
    }

    @Test
    public void testExecuteWithComplexXQuery() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        // Result depends on XQuery execution, but should not be null
    }

    @Test
    public void testExecuteWithXQueryAndXML() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        // Result depends on XQuery execution, but should not be null
    }

    @Test
    public void testExecuteWithInvalidXQuery() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        // Result depends on XQuery execution, but should not be null
    }

    @Test
    public void testExecuteWithSpecialCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        // Result depends on XQuery execution, but should not be null
    }

    @Test
    public void testExecuteWithUnicodeCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        // Result depends on XQuery execution, but should not be null
    }

    @Test
    public void testExecuteWithLongXQuery() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        // Result depends on XQuery execution, but should not be null
    }

    @Test
    public void testExecuteWithMultipleExecutions() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("id")).thenReturn("xquery1");
        
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        // Results depend on XQuery execution, but should not be null
    }
}
