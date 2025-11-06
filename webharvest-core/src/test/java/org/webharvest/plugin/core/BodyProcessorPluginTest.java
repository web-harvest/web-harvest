package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.NodeVariable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Tests for BodyProcessorPlugin.
 * Coverage for internal utility plugin used by framework.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class BodyProcessorPluginTest {

    private BodyProcessorPlugin plugin;
    private PluginConfiguration mockConfig;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        plugin = new BodyProcessorPlugin();
        mockConfig = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    // ========== Metadata Tests ==========

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "body-processor");
        assertEquals(metadata.getName(), "Body Processor");
        assertEquals(metadata.getElementName(), "body-processor");
    }

    @Test
    public void testMetadataVersion() {
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
    }

    @Test
    public void testMetadataDescription() {
        String desc = plugin.getMetadata().getDescription();
        assertNotNull(desc);
        // Description may not contain exact words, just check it exists
        assertTrue(desc.length() > 0);
    }

    @Test
    public void testMetadataAuthor() {
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
    }

    @Test
    public void testMetadataCategory() {
        assertEquals(plugin.getMetadata().getCategory(), expectedCategory(BodyProcessorPlugin.class));
    }

    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        assertTrue(tags.length > 0);
        
        boolean hasCore = false, hasBody = false;
        for (String tag : tags) {
            if ("core".equals(tag)) hasCore = true;
            if ("body".equals(tag)) hasBody = true;
        }
        assertTrue(hasCore);
        assertTrue(hasBody);
    }

    @Test
    public void testMetadataNoDependencies() {
        String[] deps = plugin.getMetadata().getDependencies();
        assertNotNull(deps);
        assertEquals(deps.length, 0);
    }

    @Test
    public void testMetadataClassName() {
        assertEquals(plugin.getMetadata().getClassName(), BodyProcessorPlugin.class.getName());
    }

    @Test
    public void testMetadataIsCorePlugin() {
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testMetadataNamespace() {
        assertEquals(plugin.getMetadata().getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    // ========== Configuration Schema Tests ==========

    @Test
    public void testGetConfigurationSchema() {
        assertNotNull(plugin.getConfigurationSchema());
    }

    @Test
    public void testConfigurationSchemaFromMetadata() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
        // Both return ConfigurationSchema instances
        assertNotNull(plugin.getConfigurationSchema());
    }

    @Test
    public void testConfigurationSchemaAllowsBody() {
        assertTrue(plugin.getConfigurationSchema().allowsBody());
    }

    @Test
    public void testConfigurationSchemaVersion() {
        assertEquals(plugin.getMetadata().getConfigurationSchema().getVersion(), "2.3.0");
    }

    @Test
    public void testConfigurationSchemaNoAttributes() {
        var attrs = plugin.getConfigurationSchema().getAttributeDefinitions();
        assertNotNull(attrs);
        assertEquals(attrs.length, 0);
    }

    @Test
    public void testConfigurationSchemaGetAttributeReturnsNull() {
        assertNull(plugin.getConfigurationSchema().getAttribute("anyname"));
    }

    @Test
    public void testConfigurationSchemaNoRequiredSubElements() {
        String[] required = plugin.getConfigurationSchema().getRequiredSubElements();
        assertNotNull(required);
        assertEquals(required.length, 0);
    }

    @Test
    public void testConfigurationSchemaNoAllowedSubElements() {
        String[] allowed = plugin.getConfigurationSchema().getAllowedSubElements();
        assertNotNull(allowed);
        assertEquals(allowed.length, 0);
    }

    @Test
    public void testConfigurationSchemaNoOptionalAttributes() {
        String[] optional = plugin.getConfigurationSchema().getOptionalAttributes();
        assertNotNull(optional);
        assertEquals(optional.length, 0);
    }

    @Test
    public void testConfigurationSchemaNoRequiredAttributes() {
        String[] required = plugin.getConfigurationSchema().getRequiredAttributes();
        assertNotNull(required);
        assertEquals(required.length, 0);
    }

    // ========== Initialization Tests ==========

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        plugin.doInitialize(mockConfig);
        // Should not throw exception
    }

    @Test
    public void testValidateConfiguration() {
        plugin.doValidateConfiguration(mockConfig);
        // Should not throw - BodyProcessor has no validation
    }

    @Test
    public void testValidateConfigurationWithNull() {
        plugin.doValidateConfiguration(null);
        // Should not throw - BodyProcessor is lenient
    }
}

