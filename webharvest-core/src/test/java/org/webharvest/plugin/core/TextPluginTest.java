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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Unit tests for TextPlugin.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class TextPluginTest {

    private TextPlugin plugin;
    private DynamicScopeContext mockContext;
    private PluginConfiguration mockConfiguration;

    @BeforeMethod
    public void setUp() {
        plugin = new TextPlugin();
        mockContext = mock(DynamicScopeContext.class);
        mockConfiguration = mock(PluginConfiguration.class);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "text");
        assertEquals(metadata.getName(), "Text Plugin");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getCategory(), "core");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
        assertEquals(metadata.getElementName(), "text");
    }

    @Test
    public void testGetMetadataTags() {
        PluginMetadata metadata = plugin.getMetadata();
        String[] tags = metadata.getTags();
        
        assertNotNull(tags);
        assertEquals(tags.length, 4);
        assertEquals(tags[0], "core");
        assertEquals(tags[1], "text");
        assertEquals(tags[2], "charset");
        assertEquals(tags[3], "delimiter");
    }

    @Test
    public void testGetMetadataDependencies() {
        PluginMetadata metadata = plugin.getMetadata();
        String[] dependencies = metadata.getDependencies();
        
        assertNotNull(dependencies);
        assertEquals(dependencies.length, 0);
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 3);
        
        // Check id attribute
        var idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        
        // Check charset attribute
        var charsetAttr = schema.getAttribute("charset");
        assertNotNull(charsetAttr);
        assertEquals(charsetAttr.getName(), "charset");
        assertFalse(charsetAttr.isRequired());
        assertEquals(charsetAttr.getDefaultValue(), "UTF-8");
        assertNotNull(charsetAttr.getAllowedValues());
        assertEquals(charsetAttr.getAllowedValues().length, 4);
        
        // Check delimiter attribute
        var delimiterAttr = schema.getAttribute("delimiter");
        assertNotNull(delimiterAttr);
        assertEquals(delimiterAttr.getName(), "delimiter");
        assertFalse(delimiterAttr.isRequired());
        assertEquals(delimiterAttr.getDefaultValue(), "\\n");
    }

    @Test
    public void testInitialize() throws PluginException {
        plugin.initialize(mockConfiguration);
        
        assertTrue(plugin.isInitialized());
        // Configuration is protected, so we can't test it directly
    }

    @Test
    public void testExecuteWithDefaultConfiguration() throws PluginException, InterruptedException {
        when(mockContext.getCharset()).thenReturn("UTF-8");
        
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        
        assertNotNull(result);
        // For now, plugin returns empty string as it doesn't implement body execution yet
        assertEquals(result.toString(), "");
    }

    @Test
    public void testExecuteWithCustomCharset() throws PluginException, InterruptedException {
        when(mockConfiguration.getProperty("charset")).thenReturn("ISO-8859-1");
        when(mockConfiguration.getProperty("delimiter")).thenReturn(";");
        when(mockContext.getCharset()).thenReturn("UTF-8");
        
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        
        assertNotNull(result);
        // For now, plugin returns empty string as it doesn't implement body execution yet
        assertEquals(result.toString(), "");
    }

    @Test
    public void testValidateConfigurationWithValidCharset() throws PluginException {
        when(mockConfiguration.getProperty("charset")).thenReturn("UTF-8");
        
        plugin.initialize(mockConfiguration);
        
        // Should not throw exception
        plugin.validateConfiguration(mockConfiguration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithInvalidCharset() throws PluginException {
        when(mockConfiguration.getProperty("charset")).thenReturn("INVALID_CHARSET");
        
        plugin.initialize(mockConfiguration);
        
        // Should throw exception
        plugin.validateConfiguration(mockConfiguration);
    }

    @Test
    public void testValidateConfigurationWithNullCharset() throws PluginException {
        when(mockConfiguration.getProperty("charset")).thenReturn(null);
        
        plugin.initialize(mockConfiguration);
        
        // Should not throw exception
        plugin.validateConfiguration(mockConfiguration);
    }

    @Test
    public void testValidateConfigurationWithEmptyCharset() throws PluginException {
        when(mockConfiguration.getProperty("charset")).thenReturn("");
        
        plugin.initialize(mockConfiguration);
        
        // Should not throw exception
        plugin.validateConfiguration(mockConfiguration);
    }

    @Test
    public void testSchemaValidationWithValidCharset() throws PluginValidationException {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("text", "http://org.webharvest/schema/2.1/core", "text");
        config.setProperty("charset", "UTF-8");
        
        // Should not throw exception
        schema.validate(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testSchemaValidationWithInvalidCharset() throws PluginValidationException {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("text", "http://org.webharvest/schema/2.1/core", "text");
        config.setProperty("charset", "INVALID_CHARSET");
        
        // Should throw exception
        schema.validate(config);
    }

    @Test
    public void testSchemaValidationWithNullCharset() throws PluginValidationException {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("text", "http://org.webharvest/schema/2.1/core", "text");
        // No charset property set
        
        // Should not throw exception
        schema.validate(config);
    }

    @Test
    public void testSchemaValidationWithEmptyCharset() throws PluginValidationException {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("text", "http://org.webharvest/schema/2.1/core", "text");
        config.setProperty("charset", "");
        
        // Should not throw exception
        schema.validate(config);
    }

    @Test
    public void testSchemaAllowsBody() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        assertTrue(schema.allowsBody());
    }

    @Test
    public void testSchemaRequiredAttributes() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        String[] required = schema.getRequiredAttributes();
        assertNotNull(required);
        assertEquals(required.length, 0);
    }

    @Test
    public void testSchemaOptionalAttributes() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        String[] optional = schema.getOptionalAttributes();
        assertNotNull(optional);
        assertEquals(optional.length, 3);
        assertEquals(optional[0], "id");
        assertEquals(optional[1], "charset");
        assertEquals(optional[2], "delimiter");
    }

    @Test
    public void testSchemaAllowedSubElements() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        String[] allowed = schema.getAllowedSubElements();
        assertNotNull(allowed);
        assertEquals(allowed.length, 0);
    }

    @Test
    public void testSchemaRequiredSubElements() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        String[] required = schema.getRequiredSubElements();
        assertNotNull(required);
        assertEquals(required.length, 0);
    }
}
