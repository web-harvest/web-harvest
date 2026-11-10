package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * {@link FilePlugin.FilePluginConfigurationSchema} attribute lookup and validation branches.
 */
public class FilePluginSchemaBranchContractTest {

    @Test
    public void getAttributeReturnsDefinitionsForKnownNamesAndNullForUnknown() {
        PluginConfigurationSchema schema = new FilePlugin().getConfigurationSchema();
        assertNotNull(schema.getAttribute("path"));
        assertNotNull(schema.getAttribute("listrecursive"));
        assertNull(schema.getAttribute("not-defined"));
    }

    @Test
    public void validateAcceptsCaseInsensitiveActionAndType() throws Exception {
        PluginConfigurationSchema schema = new FilePlugin().getConfigurationSchema();
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("path")).thenReturn("data.txt");
        when(config.getProperty("action", "read")).thenReturn("WRITE");
        when(config.getProperty("type", "text")).thenReturn("BINARY");
        schema.validate(config);
    }

    @Test
    public void validateRejectsBlankPath() {
        PluginConfigurationSchema schema = new FilePlugin().getConfigurationSchema();
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("path")).thenReturn("   ");
        try {
            schema.validate(config);
            fail("Whitespace path must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Path attribute is required");
        }
    }

    @Test
    public void schemaExposesBodyAndPathMetadata() {
        PluginConfigurationSchema schema = new FilePlugin().getConfigurationSchema();
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredAttributes()[0], "path");
        assertTrue(schema.getOptionalAttributes().length >= 5);
    }
}
