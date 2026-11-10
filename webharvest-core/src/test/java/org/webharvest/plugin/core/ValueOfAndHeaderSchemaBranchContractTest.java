package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Metadata-backed schema contracts for ValueOf and request-header plugins.
 */
public class ValueOfAndHeaderSchemaBranchContractTest {

    @Test
    public void valueOfMetadataSchemaRequiresExpressionAndResolvesAttributes() throws Exception {
        PluginConfigurationSchema schema = new ValueOfPlugin().getMetadata().getConfigurationSchema();
        assertNotNull(schema.getAttribute("expr"));
        assertNull(schema.getAttribute("missing"));

        PluginConfiguration missing = mock(PluginConfiguration.class);
        when(missing.getProperty("expr")).thenReturn(null);
        try {
            schema.validate(missing);
            fail("expr required");
        } catch (PluginValidationException expected) {
            assertNotNull(expected.getMessage());
        }

        PluginConfiguration blank = mock(PluginConfiguration.class);
        when(blank.getProperty("expr")).thenReturn("  ");
        try {
            schema.validate(blank);
            fail("blank expr required");
        } catch (PluginValidationException expected) {
            assertNotNull(expected.getMessage());
        }

        PluginConfiguration ok = mock(PluginConfiguration.class);
        when(ok.getProperty("expr")).thenReturn("//a");
        schema.validate(ok);
    }

    @Test
    public void requestHeaderMetadataSchemaRequiresNameAndResolvesAttributes() throws Exception {
        PluginConfigurationSchema schema = new HttpHeaderPlugin().getMetadata().getConfigurationSchema();
        assertNotNull(schema.getAttribute("name"));
        assertNull(schema.getAttribute("missing"));

        PluginConfiguration missing = mock(PluginConfiguration.class);
        when(missing.getProperty("name")).thenReturn(null);
        try {
            schema.validate(missing);
            fail("name required");
        } catch (PluginValidationException expected) {
            assertNotNull(expected.getMessage());
        }

        PluginConfiguration blank = mock(PluginConfiguration.class);
        when(blank.getProperty("name")).thenReturn(" ");
        try {
            schema.validate(blank);
            fail("blank name required");
        } catch (PluginValidationException expected) {
            assertNotNull(expected.getMessage());
        }

        PluginConfiguration invalid = mock(PluginConfiguration.class);
        when(invalid.getProperty("name")).thenReturn("Bad Name!");
        try {
            schema.validate(invalid);
            fail("invalid header name");
        } catch (PluginValidationException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("name"));
        }

        PluginConfiguration ok = mock(PluginConfiguration.class);
        when(ok.getProperty("name")).thenReturn("Accept");
        schema.validate(ok);
    }
}
