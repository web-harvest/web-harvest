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
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Unit tests for XMLPlugin.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class XMLPluginTest {

    @Mock
    private PluginConfiguration configuration;
    @Mock
    private DynamicScopeContext context;

    private XMLPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        plugin = new XMLPlugin();
        when(configuration.getProperty("id")).thenReturn(null);
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "xml");
        assertEquals(plugin.getMetadata().getName(), "XML");
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
        assertNotNull(plugin.getMetadata().getDescription());
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getCategory(), expectedCategory(XMLPlugin.class));
        assertEquals(plugin.getMetadata().getElementName(), "xml");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.3.0");
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getOptionalAttributes().length, 1);

        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 1);

        // Check id attribute (optional)
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(idAttr.isRequired());
        assertNull(idAttr.getDefaultValue());
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        plugin.initialize(configuration);
        assertTrue(plugin.isInitialized(), "Valid configuration should initialize the plugin");
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testExecute() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithIdAttribute() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("test-xml");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteMultipleTimes() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        // Execute multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithInterruptedException() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        // Mock context to throw RuntimeException
        when(context.getCharset()).thenThrow(new RuntimeException("Test interruption"));
        
        try {
            plugin.execute(context);
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Failed to execute XML plugin");
            assertNotNull(e.getCause());
            assertEquals(e.getCause().getMessage(), "Test interruption");
        }
    }

    @Test
    public void testValidateConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(configuration);
        // Successful return confirms the valid configuration is accepted.
    }

    @Test
    public void testExecuteWithNullContext() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        try {
            plugin.execute(null);
            fail("Should have thrown NullPointerException");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Failed to execute XML plugin");
            assertTrue(e.getCause() instanceof NullPointerException,
                    "A null execution context must be retained as the cause");
        }
    }

    @Test
    public void testExecuteWithEmptyConfiguration() throws InterruptedException, PluginException {
        PluginConfiguration emptyConfig = mock(PluginConfiguration.class);
        when(emptyConfig.getProperty("id")).thenReturn(null);
        
        plugin.initialize(emptyConfig);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithSpecialCharactersInId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xml-test@#$");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithUnicodeCharactersInId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xml-测试");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithVeryLongId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("very-long-xml-plugin-id-with-many-characters-to-test-system-behavior");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithHyphenatedId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xml-test-id-with-hyphens");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithUnderscoreId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xml_test_id_with_underscores");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNumericId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xml123");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithMixedCaseId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("XmlTestId");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteMultipleTimesInSequence() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xml-test");
        
        plugin.initialize(configuration);
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithDifferentCharsets() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xml-test");
        when(context.getCharset()).thenReturn("UTF-16").thenReturn("ISO-8859-1").thenReturn("UTF-8");
        
        plugin.initialize(configuration);
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertNotNull(metadata.getCategory());
        assertEquals(metadata.getCategory(), expectedCategory(XMLPlugin.class));
        assertNotNull(metadata.getVersion());
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
    }

    @Test
    public void testSchemaAttributeDefinitions() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
        assertNotNull(attrs);
        
        for (PluginConfigurationSchema.AttributeDefinition attr : attrs) {
            assertNotNull(attr.getName());
            assertNotNull(attr.getType());
        }
    }
    
    @Test
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertTrue(schema.allowsBody());
    }
}


