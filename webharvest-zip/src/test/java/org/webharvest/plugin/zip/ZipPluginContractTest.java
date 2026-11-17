/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.zip;

import java.util.Arrays;
import java.util.Set;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeType;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.semantic.PluginNodeRole;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Metadata and schema contracts for the optional ZIP extension.
 *
 * @author WebHarvest Team
 * @since 2.2.0
 */
public class ZipPluginContractTest {

    @Test
    public void testZipPluginMetadataAndSchema() throws Exception {
        ZipPlugin plugin = new ZipPlugin();
        plugin.initialize(mock(PluginConfiguration.class));
        plugin.validateConfiguration(mock(PluginConfiguration.class));

        PluginMetadata metadata = plugin.getMetadata();
        assertEquals(metadata.getId(), "zip");
        assertNotNull(metadata.getName());
        assertNotNull(metadata.getDescription());
        assertEquals(metadata.getVersion(), "2.1.0");
        assertNotNull(metadata.getAuthor());
        assertNotNull(metadata.getCategory());
        assertTrue(metadata.getTags().length > 0);
        assertEquals(metadata.getDependencies().length, 0);
        assertEquals(metadata.getElementName(), "zip");
        assertEquals(metadata.getClassName(), ZipPlugin.class.getName());
        assertNotNull(metadata.getNamespace());
        assertFalse(metadata.isCorePlugin());

        Set<PluginNodeRole> roles = metadata.getDeclaredRoles();
        assertTrue(roles.contains(PluginNodeRole.CONTEXT_CONTAINER));
        assertTrue(roles.contains(PluginNodeRole.BODY_EVALUATOR));
        assertNotNull(metadata.getConfigurationSchema());

        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertEquals(schema.getAttributeDefinitions().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getOptionalAttributes().length, 0);
        assertEquals(schema.getAllowedSubElements(), new String[] {"zip-entry"});
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getRequiredParents().length, 0);
        assertTrue(schema.allowsBody());
        assertTrue(schema.canAppearAtRoot());
        assertFalse(schema.isContextSpecific());
        assertEquals(schema.getVersion(), "2.1.0");
        assertNull(schema.getAttribute("unknown"));
        schema.validate(mock(PluginConfiguration.class));
    }

    @Test
    public void testZipEntryPluginMetadataAndSchema() throws Exception {
        ZipEntryPlugin plugin = new ZipEntryPlugin();

        PluginMetadata metadata = plugin.getMetadata();
        assertEquals(metadata.getId(), "zip-entry");
        assertNotNull(metadata.getName());
        assertNotNull(metadata.getDescription());
        assertEquals(metadata.getVersion(), "2.1.0");
        assertNotNull(metadata.getAuthor());
        assertNotNull(metadata.getCategory());
        assertTrue(metadata.getTags().length > 0);
        assertTrue(Arrays.asList(metadata.getDependencies()).contains("zip"));
        assertEquals(metadata.getElementName(), "zip-entry");
        assertEquals(metadata.getClassName(), ZipEntryPlugin.class.getName());
        assertNotNull(metadata.getNamespace());
        assertFalse(metadata.isCorePlugin());

        Set<PluginNodeRole> roles = metadata.getDeclaredRoles();
        assertTrue(roles.contains(PluginNodeRole.DEPENDENT));
        assertTrue(roles.contains(PluginNodeRole.BODY_EVALUATOR));
        assertNotNull(metadata.getConfigurationSchema());

        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertEquals(schema.getRequiredAttributes(), new String[] {"name"});
        assertEquals(schema.getOptionalAttributes(), new String[] {"charset"});
        assertEquals(schema.getRequiredParents(), new String[] {"zip"});
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertTrue(schema.allowsBody());
        assertFalse(schema.canAppearAtRoot());
        assertTrue(schema.isContextSpecific());
        assertEquals(schema.getVersion(), "2.1.0");

        AttributeDefinition name = schema.getAttribute("name");
        assertNotNull(name);
        assertEquals(name.getName(), "name");
        assertEquals(name.getType(), AttributeType.STRING);
        assertTrue(name.isRequired());
        assertNotNull(name.getDescription());
        assertNull(name.getDefaultValue());
        assertNull(name.getAllowedValues());
        assertNull(name.getMinValue());
        assertNull(name.getMaxValue());
        assertNull(name.getPattern());

        AttributeDefinition charset = schema.getAttribute("charset");
        assertNotNull(charset);
        assertEquals(charset.getName(), "charset");
        assertEquals(charset.getType(), AttributeType.STRING);
        assertFalse(charset.isRequired());
        assertNotNull(charset.getDescription());
        assertEquals(charset.getDefaultValue(), "UTF-8");
        assertTrue(Arrays.asList(charset.getAllowedValues()).contains("UTF-8"));
        assertNull(charset.getMinValue());
        assertNull(charset.getMaxValue());
        assertNull(charset.getPattern());
        assertEquals(schema.getAttributeDefinitions().length, 2);
        assertNull(schema.getAttribute("unknown"));
    }

    @Test
    public void testZipEntrySchemaAndPluginValidationRejectMissingName() throws Exception {
        ZipEntryPlugin plugin = new ZipEntryPlugin();
        PluginConfiguration invalid = mock(PluginConfiguration.class);
        when(invalid.getProperty("name")).thenReturn(" ");

        try {
            plugin.getConfigurationSchema().validate(invalid);
            fail("Schema validation must reject an empty entry name");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("Name"));
        }

        plugin.initialize(invalid);
        try {
            plugin.validateConfiguration(invalid);
            fail("Plugin validation must reject an empty entry name");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("Name"));
        }
    }
}
