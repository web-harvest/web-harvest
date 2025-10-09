/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
// ULTRA-LEAN v2.2.x: Removed IElementDef imports
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
// ULTRA-LEAN v2.2.x: No more XmlNode needed

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for EmptyPlugin
 */
public class EmptyPluginTest {

    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private EmptyPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new EmptyPlugin();
        
        
        // Mock configuration
        when(configuration.getProperty("id")).thenReturn(null); // Optional attribute
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Empty Plugin");
        assertEquals(metadata.getDescription(), "Empty plugin that executes its body and returns an empty variable");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "empty");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        // Check 'id' attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttributeDefinitions()[0];
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.1.0");
    }

    @Test
    public void testEmptyPluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        EmptyPlugin newPlugin = new EmptyPlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("emptyId");
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test
    public void testEmptyPluginExecute() throws InterruptedException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result - EmptyPlugin returns EmptyVariable.INSTANCE
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        assertEquals(result.toString(), "");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testEmptyPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        EmptyPlugin testPlugin = new EmptyPlugin() {
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
    public void testEmptyPluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test
    public void testEmptyPluginWithIdAttribute() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        EmptyPlugin newPlugin = new EmptyPlugin();
        
        // Mock configuration with 'id' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("emptyId");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        assertEquals(result.toString(), "");
    }

    @Test
    public void testEmptyPluginExecuteMultipleTimes() throws InterruptedException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // All results should be the same EmptyVariable instance
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
        assertEquals(result1.toString(), "");
        assertEquals(result2.toString(), "");
        assertEquals(result3.toString(), "");
    }

    @Test
    public void testSchemaOptionalAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] optionalAttrs = schema.getOptionalAttributes();
        
        assertNotNull(optionalAttrs);
        assertEquals(optionalAttrs.length, 1);
        assertEquals(optionalAttrs[0], "id");
    }

    @Test
    public void testSchemaRequiredAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] requiredAttrs = schema.getRequiredAttributes();
        
        assertNotNull(requiredAttrs);
        assertEquals(requiredAttrs.length, 0);
    }

    @Test
    public void testSchemaGetAttribute() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition attr = schema.getAttribute("id");
        
        // getAttribute returns null for this implementation
        assertNull(attr);
    }

    @Test
    public void testAttributeDefinitionGetters() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
        PluginConfigurationSchema.AttributeDefinition idAttr = attrs[0];
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getDescription(), "Optional identifier for the empty element");
        assertNull(idAttr.getDefaultValue());
        assertNull(idAttr.getAllowedValues());
        assertNull(idAttr.getMinValue());
        assertNull(idAttr.getMaxValue());
        assertNull(idAttr.getPattern());
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertEquals(metadata.getCategory(), "core");
        assertEquals(metadata.getVersion(), "2.1.0");
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        assertEquals(tags.length, 3);
        assertEquals(tags[0], "core");
        assertEquals(tags[1], "empty");
        assertEquals(tags[2], "grouping");
        
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
        assertEquals(dependencies.length, 0);
    }

    @Test
    public void testEmptyPluginWithNullId() throws PluginException, InterruptedException {
        EmptyPlugin newPlugin = new EmptyPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn(null);
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testEmptyPluginWithEmptyId() throws PluginException, InterruptedException {
        EmptyPlugin newPlugin = new EmptyPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("");
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testEmptyPluginWithSpecialCharacterId() throws PluginException, InterruptedException {
        EmptyPlugin newPlugin = new EmptyPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("empty-id_123");
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertTrue(schema.allowsBody());
    }
}
