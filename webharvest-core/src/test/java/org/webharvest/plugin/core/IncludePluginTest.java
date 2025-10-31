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
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.CorePluginDef;
// import org.webharvest.definition.IElementDef;
// import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Unit tests for {@link IncludePlugin}.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class IncludePluginTest {
    
    private IncludePlugin plugin;
    
    @Mock
    private DynamicScopeContext context;
    
    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;
    
    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new IncludePlugin();
    }
    
    @Test
    public void testIncludePluginReturnsEmptyVariable() throws Exception {
        // Given
        when(configuration.getProperty("path")).thenReturn("test.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
    
    @Test(expectedExceptions = org.webharvest.plugin.exceptions.PluginException.class)
    public void testIncludePluginThrowsExceptionWhenPathIsNull() throws Exception {
        // Given
        when(configuration.getProperty("path")).thenReturn(null);
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        
        // When/Then
        plugin.initialize(configuration);
    }
    
    @Test(expectedExceptions = org.webharvest.plugin.exceptions.PluginException.class)
    public void testIncludePluginThrowsExceptionWhenPathIsEmpty() throws Exception {
        // Given
        when(configuration.getProperty("path")).thenReturn("");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        
        // When/Then
        plugin.initialize(configuration);
    }
    
    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "include");
        assertEquals(plugin.getMetadata().getName(), "Include Plugin");
        assertEquals(plugin.getMetadata().getVersion(), "2.1.0");
        assertEquals(plugin.getMetadata().getDescription(), "Include plugin that includes external configuration files");
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getCategory(), expectedCategory(IncludePlugin.class));
        assertEquals(plugin.getMetadata().getElementName(), "include");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.1.0");
        assertFalse(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 1);
        assertEquals(schema.getOptionalAttributes().length, 1);

        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 2);

        // Check path attribute (required)
        PluginConfigurationSchema.AttributeDefinition pathAttr = schema.getAttribute("path");
        assertNotNull(pathAttr);
        assertEquals(pathAttr.getName(), "path");
        assertEquals(pathAttr.getType(), PluginConfigurationSchema.AttributeType.FILE_PATH);
        assertTrue(pathAttr.isRequired());
        assertNull(pathAttr.getDefaultValue());

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
        when(configuration.getProperty("path")).thenReturn("test.xml");
        plugin.initialize(configuration);
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithMissingPath() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn(null);
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithEmptyPath() throws PluginValidationException {
        when(configuration.getProperty("path")).thenReturn("");
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void testExecuteWithIdAttribute() throws Exception {
        when(configuration.getProperty("id")).thenReturn("test-include");
        when(configuration.getProperty("path")).thenReturn("test.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteMultipleTimes() throws Exception {
        when(configuration.getProperty("path")).thenReturn("test.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
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
    public void testExecuteWithDifferentPaths() throws Exception {
        when(configuration.getProperty("path")).thenReturn("different/path.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithComplexPath() throws Exception {
        when(configuration.getProperty("path")).thenReturn("examples/complex/path/test.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithAbsolutePath() throws Exception {
        when(configuration.getProperty("path")).thenReturn("/absolute/path/test.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithSpecialCharactersInPath() throws Exception {
        when(configuration.getProperty("path")).thenReturn("test-file_2024.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithUnicodeInPath() throws Exception {
        when(configuration.getProperty("path")).thenReturn("测试文件.xml");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithNullPath() throws Exception {
        when(configuration.getProperty("path")).thenReturn(null);
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithEmptyPath() throws Exception {
        when(configuration.getProperty("path")).thenReturn("");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithWhitespacePath() throws Exception {
        when(configuration.getProperty("path")).thenReturn("   ");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertNotNull(metadata.getCategory());
        assertEquals(metadata.getCategory(), expectedCategory(IncludePlugin.class));
        assertNotNull(metadata.getVersion());
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        assertTrue(tags.length > 0);
        
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
    }

    @Test
    public void testSchemaAttributeDefinitions() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
        assertNotNull(attrs);
        assertTrue(attrs.length >= 1);
        
        for (PluginConfigurationSchema.AttributeDefinition attr : attrs) {
            assertNotNull(attr.getName());
            assertNotNull(attr.getType());
        }
    }

    @Test
    public void testSchemaOptionalAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] optionalAttrs = schema.getOptionalAttributes();
        
        assertNotNull(optionalAttrs);
    }

    @Test
    public void testSchemaRequiredAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] requiredAttrs = schema.getRequiredAttributes();
        
        assertNotNull(requiredAttrs);
        assertTrue(requiredAttrs.length >= 1);
    }

    @Test
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertFalse(schema.allowsBody());
    }

    @Test
    public void testExecuteWithValidPathFormat() throws Exception {
        IncludePlugin newPlugin = new IncludePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("path")).thenReturn("config/test.xml");
        
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testAttributeDefinitionProperties() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
        boolean hasPathAttr = false;
        for (PluginConfigurationSchema.AttributeDefinition attr : attrs) {
            if ("path".equals(attr.getName())) {
                hasPathAttr = true;
                assertTrue(attr.isRequired());
                // Path attribute can be FILE_PATH or STRING type
                assertNotNull(attr.getType());
            }
        }
        assertTrue(hasPathAttr);
    }

    @Test
    public void testExecuteWithRelativePath() throws Exception {
        IncludePlugin newPlugin = new IncludePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("path")).thenReturn("./relative/path/file.xml");
        when(config.getProperty("id")).thenReturn(null);
        
        when(context.getCharset()).thenReturn("UTF-8");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithFullAbsolutePath() throws Exception {
        IncludePlugin newPlugin = new IncludePlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("path")).thenReturn("/absolute/path/file.xml");
        when(config.getProperty("id")).thenReturn(null);
        
        when(context.getCharset()).thenReturn("UTF-8");
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }
}
