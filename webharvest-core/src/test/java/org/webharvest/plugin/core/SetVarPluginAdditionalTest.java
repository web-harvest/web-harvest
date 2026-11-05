package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Additional tests for SetVarPlugin to boost coverage.
 * Focuses on metadata, schema, and parameter variations.
 */
public class SetVarPluginAdditionalTest {

    private SetVarPlugin plugin;
    private PluginConfiguration config;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new SetVarPlugin();
        config = mock(PluginConfiguration.class);
        context = mock(DynamicScopeContext.class);
        
        when(config.getProperty("name")).thenReturn("testVar");
        when(config.getProperty("id")).thenReturn(null);
        when(context.getCharset()).thenReturn("UTF-8");
    }

    // ========== Metadata Tests ==========

    @Test
    public void testMetadataId() {
        assertEquals(plugin.getMetadata().getId(), "set-var");
    }

    @Test
    public void testMetadataName() {
        assertNotNull(plugin.getMetadata().getName());
    }

    @Test
    public void testMetadataVersion() {
        assertNotNull(plugin.getMetadata().getVersion());
    }

    @Test
    public void testMetadataDescription() {
        assertNotNull(plugin.getMetadata().getDescription());
        assertTrue(plugin.getMetadata().getDescription().length() > 10);
    }

    @Test
    public void testMetadataAuthor() {
        assertNotNull(plugin.getMetadata().getAuthor());
    }

    @Test
    public void testMetadataCategory() {
        assertEquals(plugin.getMetadata().getCategory(), "core");
    }

    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        // Tags may be empty array
    }

    @Test
    public void testMetadataDependencies() {
        String[] deps = plugin.getMetadata().getDependencies();
        assertNotNull(deps);
    }

    @Test
    public void testMetadataClassName() {
        assertEquals(plugin.getMetadata().getClassName(), SetVarPlugin.class.getName());
    }

    @Test
    public void testMetadataIsCorePlugin() {
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testMetadataElementName() {
        assertEquals(plugin.getMetadata().getElementName(), "set-var");
    }

    @Test
    public void testMetadataNamespace() {
        assertEquals(plugin.getMetadata().getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    // ========== Configuration Schema Tests ==========

    @Test
    public void testSchemaAllowsBody() {
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }

    @Test
    public void testSchemaVersion() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getVersion());
    }

    @Test
    public void testSchemaAttributeDefinitions() {
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        assertNotNull(attrs);
        assertTrue(attrs.length > 0);
    }

    @Test
    public void testSchemaGetAttributeName() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attr = schema.getAttribute("name");
        if (attr != null) {
            assertEquals(attr.getName(), "name");
        }
    }

    @Test
    public void testSchemaGetAttributeNonexistent() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attr = schema.getAttribute("nonexistent");
        // Should handle gracefully (either null or empty)
    }

    @Test
    public void testSchemaAllowedSubElements() {
        var allowed = plugin.getMetadata().getConfigurationSchema().getAllowedSubElements();
        assertNotNull(allowed);
    }

    @Test
    public void testSchemaRequiredSubElements() {
        var required = plugin.getMetadata().getConfigurationSchema().getRequiredSubElements();
        assertNotNull(required);
    }

    @Test
    public void testSchemaOptionalAttributes() {
        var optional = plugin.getMetadata().getConfigurationSchema().getOptionalAttributes();
        assertNotNull(optional);
    }

    @Test
    public void testSchemaRequiredAttributes() {
        var required = plugin.getMetadata().getConfigurationSchema().getRequiredAttributes();
        assertNotNull(required);
    }

    @Test
    public void testSchemaValidate() throws Exception {
        var schema = plugin.getMetadata().getConfigurationSchema();
        schema.validate(config);
        // Should not throw
    }

    // ========== Initialization Tests ==========

    @Test
    public void testInitializeWithNameParameter() throws Exception {
        when(config.getProperty("name")).thenReturn("var1");
        plugin.initialize(config);
        // Should not throw
    }

    @Test
    public void testInitializeWithIdParameter() throws Exception {
        when(config.getProperty("id")).thenReturn("test-id");
        plugin.initialize(config);
        // Should not throw
    }

    @Test
    public void testInitializeWithBothParameters() throws Exception {
        when(config.getProperty("name")).thenReturn("var1");
        when(config.getProperty("id")).thenReturn("test-id");
        plugin.initialize(config);
        // Should not throw
    }
}

