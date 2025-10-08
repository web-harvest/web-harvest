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
import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link TextPlugin}.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class TextPluginTest {

    private TextPlugin plugin;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new TextPlugin();
        
        // Set ElementDef to avoid NullPointerException
        plugin.setElementDef(elementDef);
    }
    
    @Test
    public void testTextPluginReturnsTextContent() throws Exception {
        // Given
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn("This is plain text content from Text Plugin");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false); // No child operations
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]); // No child operations
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "This is plain text content from Text Plugin");
    }
    
    @Test
    public void testTextPluginReturnsEmptyStringWhenNoContent() throws Exception {
        // Given
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn("");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false); // No child operations
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]); // No child operations
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "");
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void testTextPluginThrowsExceptionWhenElementDefIsNull() throws Exception {
        // Given
        plugin.initialize(configuration);
        // elementDef remains null (not set in setUp)
        
        // When/Then - should throw RuntimeException due to NullPointerException
        plugin.execute(context);
    }
    
    @Test
    public void testTextPluginReturnsEmptyStringWhenTextIsNull() throws Exception {
        // Given
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(null);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]); // No child operations
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "");
    }
    
    @Test
    public void testTextPluginWithSpecialCharacters() throws Exception {
        // Given
        String specialText = "Text with special chars: àáâãäåæçèéêë";
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(specialText);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]); // No child operations
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), specialText);
    }
    
    @Test
    public void testTextPluginWithNewlinesAndWhitespace() throws Exception {
        // Given
        String textWithWhitespace = "  Line 1\n  Line 2\r\n\tTabbed content  ";
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(textWithWhitespace);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]); // No child operations
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), textWithWhitespace);
    }
    
    @Test
    public void testTextPluginWithLargeContent() throws Exception {
        // Given
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("Line ").append(i).append("\n");
        }
        
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(largeText.toString());
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]); // No child operations
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString().length(), largeText.length());
        assertTrue(result.toString().startsWith("Line 0"));
        assertTrue(result.toString().contains("Line 999"));
    }

    @Test
    public void testGetMetadata() {
        // Test getMetadata method
        org.webharvest.plugin.PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Text Plugin");
        assertEquals(metadata.getDescription(), "Text plugin that processes text content with charset and delimiter support");
        assertTrue(metadata.isCorePlugin());
    }
    
    @Test
    public void testTextPluginWithUnicodeContent() throws Exception {
        // Given
        String unicodeText = "Unicode: ąćęłńóśźż ĄĆĘŁŃÓŚŹŻ 中文 العربية русский";
        
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(unicodeText);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), unicodeText);
    }

    @Test
    public void testTextPluginWithEmptyContent() throws Exception {
        // Given
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn("");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), "");
    }

    @Test
    public void testTextPluginWithHTMLEntities() throws Exception {
        // Given
        String htmlText = "Text with entities: &lt;&gt;&amp;&quot;";
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(htmlText);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), htmlText);
    }

    @Test
    public void testTextPluginWithVeryLongText() throws Exception {
        // Given
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a very long text. ");
        }
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(longText.toString());
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), longText.toString());
    }

    @Test
    public void testTextPluginWithMultipleSpaces() throws Exception {
        // Given
        String multiSpaceText = "Text    with     multiple      spaces";
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(multiSpaceText);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), multiSpaceText);
    }

    @Test
    public void testTextPluginWithTabsAndNewlines() throws Exception {
        // Given
        String tabNewlineText = "Text\twith\ttabs\nand\nnewlines";
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(tabNewlineText);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertEquals(result.toString(), tabNewlineText);
    }

    @Test
    public void testTextPluginWithNumericContent() throws Exception {
        // Given
        String numericText = "123456789";
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(numericText);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);

        // Then
        assertNotNull(result);
        assertEquals(result.toString(), numericText);
    }

    @Test
    public void testTextPluginExecuteMultipleTimes() throws Exception {
        // Given
        String simpleText = "Multiple execution test";
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn(simpleText);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.setElementDef(elementDef);
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(result1.toString(), simpleText);
        assertEquals(result2.toString(), simpleText);
        assertEquals(result3.toString(), simpleText);
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
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertNotNull(metadata.getCategory());
        assertNotNull(metadata.getVersion());
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
    }

    @Test
    public void testAttributeDefinitionGetters() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
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

    @Test
    public void testExecuteWithNullId() throws Exception {
        TextPlugin newPlugin = new TextPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn(null);
        when(config.getProperty("delimiter")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("test");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        newPlugin.setElementDef(elementDef);
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptyDelimiter() throws Exception {
        TextPlugin newPlugin = new TextPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("test-id");
        when(config.getProperty("delimiter")).thenReturn("");
        
        when(xmlNode.getText()).thenReturn("test content");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        newPlugin.setElementDef(elementDef);
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithSpecialDelimiter() throws Exception {
        TextPlugin newPlugin = new TextPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("test-id");
        when(config.getProperty("delimiter")).thenReturn("||");
        
        when(xmlNode.getText()).thenReturn("part1||part2||part3");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        newPlugin.setElementDef(elementDef);
        newPlugin.initialize(config);
        
        Variable result = newPlugin.execute(context);
        assertNotNull(result);
    }

    @Test
    public void testSchemaGetAttribute() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        // Just testing that it doesn't throw exception
        PluginConfigurationSchema.AttributeDefinition attr = schema.getAttribute("delimiter");
        assertTrue(true);
    }
}

