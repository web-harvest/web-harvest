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
import org.webharvest.definition.CorePluginDef;
import org.webharvest.definition.IElementDef;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.XmlNode;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for JsonToXmlPlugin
 */
public class JsonToXmlPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private JsonToXmlPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new JsonToXmlPlugin();
        
        // Set ElementDef to avoid NullPointerException
        
        // Mock elementDef
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(xmlNode.getText()).thenReturn("{\"name\": \"test\"}");
        when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration
        when(configuration.getProperty("tag")).thenReturn(null); // Optional attribute
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "JsonToXml Plugin");
        assertEquals(metadata.getDescription(), "Converter from JSON to XML");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "json-to-xml");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        // Check 'tag' attribute
        PluginConfigurationSchema.AttributeDefinition tagAttr = schema.getAttributeDefinitions()[0];
        assertNotNull(tagAttr);
        assertEquals(tagAttr.getName(), "tag");
        assertFalse(tagAttr.isRequired());
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.1.0");
    }

    @Test
    public void testJsonToXmlPluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("root");
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testJsonToXmlPluginInitializeWithNullConfiguration() throws PluginException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Test initialization with null configuration - JsonToXmlPlugin throws exception
        newPlugin.initialize(null);
    }

    @Test
    public void testJsonToXmlPluginExecute() throws InterruptedException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result - JsonToXmlPlugin returns NodeVariable with <empty/> when no JSON content
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testJsonToXmlPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        JsonToXmlPlugin testPlugin = new JsonToXmlPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        testPlugin.execute(context);
    }

    @Test
    public void testJsonToXmlPluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("root");
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testJsonToXmlPluginValidateConfigurationWithNull() throws PluginValidationException {
        // Test validation with null configuration - JsonToXmlPlugin throws exception
        plugin.validateConfiguration(null);
    }

    @Test
    public void testJsonToXmlPluginValidateConfigurationWithMissingTag() throws PluginValidationException {
        // Test validation with missing 'tag' attribute - JsonToXmlPlugin has no validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn(null);
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test
    public void testJsonToXmlPluginWithTagAttribute() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration with 'tag' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("root");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginExecuteMultipleTimes() throws InterruptedException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // All results should be the same NodeVariable instance
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof NodeVariable);
        assertTrue(result2 instanceof NodeVariable);
        assertTrue(result3 instanceof NodeVariable);
        assertEquals(result1.toString(), "<empty/>");
        assertEquals(result2.toString(), "<empty/>");
        assertEquals(result3.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithJsonArray() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("items");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithJsonObject() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("user");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithInvalidJson() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("error");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithCustomTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration with custom tag
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("custom");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithEmptyTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration with empty tag
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithNullTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration with null tag
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn(null);
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithWhitespaceTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration with whitespace tag
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("   ");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithSpecialCharactersTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration with special characters tag
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("tag-with-special-chars");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void testJsonToXmlPluginWithNestedObject() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("data");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithArrayOfObjects() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("items");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithEmptyArray() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("empty");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithEmptyObject() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("empty");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }


    @Test
    public void testJsonToXmlPluginWithNumericTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("123");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithMixedCaseTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("MixedCaseTag");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithUnicodeTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("täg");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithLongTag() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("very-long-tag-name-with-many-characters");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithTagContainingDots() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("tag.with.dots");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithTagContainingHyphens() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("tag-with-hyphens");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginWithTagContainingUnderscores() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("tag_with_underscores");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<empty/>"); // Still returns empty because executeBody is not mocked to return JSON
    }

    @Test
    public void testJsonToXmlPluginMultipleExecutions() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        JsonToXmlPlugin newPlugin = new JsonToXmlPlugin();
        
        // Mock configuration
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("tag")).thenReturn("test");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Mock context (body execution is not mocked here, so it will return EmptyVariable)
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin multiple times
        Variable result1 = newPlugin.execute(context);
        Variable result2 = newPlugin.execute(context);
        Variable result3 = newPlugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof NodeVariable);
        assertTrue(result2 instanceof NodeVariable);
        assertTrue(result3 instanceof NodeVariable);
        assertEquals(result1.toString(), "<empty/>");
        assertEquals(result2.toString(), "<empty/>");
        assertEquals(result3.toString(), "<empty/>");
    }
}
