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

public class XQParamPluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    private XQParamPlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        plugin = new XQParamPlugin();
        
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("string");
        when(configuration.getProperty("id")).thenReturn(null);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "xq-param");
        assertEquals(metadata.getName(), "XQParam Plugin");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getDescription(), "XQParam plugin that provides XQuery parameter functionality");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "xq-param");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredAttributes().length, 1);
        assertEquals(schema.getAttributeDefinitions().length, 3); // id, name, type
    }

    @Test
    public void testInitialize() throws PluginException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("integer");
        
        // When
        plugin.initialize(configuration);
        
        // Then
        // Should not throw exception
    }

    @Test
    public void testValidateConfiguration() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("string");
        
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
    public void testValidateConfigurationWithInvalidType() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("invalid-type");
        
        // When & Then
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void testValidateConfigurationWithValidTypes() throws PluginValidationException {
        // Test all valid types
        String[] validTypes = {"node()", "node()*", "integer", "integer*", "long", "long*", 
                              "float", "float*", "double", "double*", "boolean", "boolean*", 
                              "string", "string*"};
        
        for (String type : validTypes) {
            // Given
            when(configuration.getProperty("name")).thenReturn("param1");
            when(configuration.getProperty("type")).thenReturn(type);
            
            // When & Then
            plugin.validateConfiguration(configuration);
            // Should not throw exception
        }
    }

    @Test
    public void testValidateConfigurationWithNullType() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn(null);
        
        // When & Then
        plugin.validateConfiguration(configuration);
        // Should not throw exception (type is optional)
    }

    @Test
    public void testValidateConfigurationWithEmptyType() throws PluginValidationException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("");
        
        // When & Then
        plugin.validateConfiguration(configuration);
        // Should not throw exception (empty type is treated as null)
    }

    @Test
    public void testExecute() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("string");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNodeType() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("node()");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithIntegerType() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("integer");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithBooleanType() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("boolean");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithFloatType() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("float");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithDoubleType() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("double");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithLongType() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("long");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithArrayTypes() throws PluginException, InterruptedException {
        // Test array types (with *)
        String[] arrayTypes = {"node()*", "integer*", "long*", "float*", "double*", "boolean*", "string*"};
        
        for (String type : arrayTypes) {
            // Given
            XQParamPlugin testPlugin = new XQParamPlugin();
            when(configuration.getProperty("name")).thenReturn("param1");
            when(configuration.getProperty("type")).thenReturn(type);
            
            testPlugin.initialize(configuration);
            
            // When
            Variable result = testPlugin.execute(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result instanceof EmptyVariable);
        }
    }

    @Test
    public void testExecuteWithSpecialCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name")).thenReturn("param_1");
        when(configuration.getProperty("type")).thenReturn("string");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithLongParameterName() throws PluginException, InterruptedException {
        // Given
        String longName = "very_long_parameter_name_with_many_characters";
        when(configuration.getProperty("name")).thenReturn(longName);
        when(configuration.getProperty("type")).thenReturn("string");
        
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
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("type")).thenReturn("string");
        
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
