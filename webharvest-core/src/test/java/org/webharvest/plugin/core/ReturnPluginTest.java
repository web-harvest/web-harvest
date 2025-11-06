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

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Unit tests for ReturnPlugin.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class ReturnPluginTest {

    @Mock
    private PluginConfiguration mockConfiguration;
    
    @Mock
    private DynamicScopeContext mockContext;
    
    private ReturnPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new ReturnPlugin();
        
        // Mock hasChildren() for new architecture
        when(mockConfiguration.hasChildren()).thenReturn(false);
        when(mockConfiguration.getChildren()).thenReturn(java.util.Collections.emptyList());
        
        // v2.2.x ULTRA-LEAN: No elementDef needed, only PluginConfiguration
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "return");
        assertEquals(metadata.getName(), "Return");
        assertEquals(metadata.getVersion(), "2.3.0");
        assertEquals(metadata.getCategory(), "Utilities");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
        assertEquals(metadata.getElementName(), "return");
    }

    @Test
    public void testGetMetadataTags() {
        PluginMetadata metadata = plugin.getMetadata();
        String[] tags = metadata.getTags();
        
        assertNotNull(tags);
        assertTrue(tags.length >= 4, "Should have at least 4 tags");
        assertTrue(java.util.Arrays.asList(tags).contains("core"), "Should contain 'core' tag");
        assertTrue(java.util.Arrays.asList(tags).contains("return"), "Should contain 'return' tag");
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
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        // Check id attribute
        var idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        assertNull(idAttr.getDefaultValue());
    }

    @Test
    public void testInitialize() throws PluginException {
        plugin.initialize(mockConfiguration);
        
        assertTrue(plugin.isInitialized());
        // Configuration is protected, so we can't test it directly
    }

    @Test
    public void testExecute() throws PluginException, InterruptedException {
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        
        assertNotNull(result);
        // Return plugin should return EmptyVariable.INSTANCE
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testValidateConfiguration() throws PluginException {
        plugin.initialize(mockConfiguration);
        
        // Should not throw exception (no complex validation)
        plugin.validateConfiguration(mockConfiguration);
    }

    @Test
    public void testSchemaValidation() throws PluginValidationException {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("return", "http://org.webharvest/schema/2.1/core", "return");
        config.setProperty("id", "test-return");
        
        // Should not throw exception
        schema.validate(config);
    }

    @Test
    public void testSchemaValidationWithNullId() throws PluginValidationException {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("return", "http://org.webharvest/schema/2.1/core", "return");
        // No id property set
        
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
        assertEquals(optional.length, 1);
        assertEquals(optional[0], "id");
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

    @Test
    public void testSchemaVersion() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        assertEquals(schema.getVersion(), "2.3.0");
    }

    @Test
    public void testSchemaGetAttributeWithNonExistentName() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        var attr = schema.getAttribute("non-existent");
        assertNull(attr);
    }

    @Test
    public void testSchemaGetAttributeWithNullName() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        var attr = schema.getAttribute(null);
        assertNull(attr);
    }

    @Test
    public void testSchemaGetAttributeWithEmptyName() {
        PluginMetadata metadata = plugin.getMetadata();
        var schema = metadata.getConfigurationSchema();
        
        var attr = schema.getAttribute("");
        assertNull(attr);
    }
}

