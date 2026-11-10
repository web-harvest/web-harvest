package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Config root schema branches for allowed charset and script language values.
 */
public class ConfigPluginSchemaBranchContractTest {

    @Test
    public void acceptsCanonicalCharsetsAndScriptLanguages() throws Exception {
        PluginConfigurationSchema schema = new ConfigPlugin().getConfigurationSchema();

        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("charset")).thenReturn("ISO-8859-1");
        when(config.getProperty("scriptlang")).thenReturn("GROOVY");
        schema.validate(config);
    }

    @Test
    public void nullCharsetAndScriptlangAreAllowed() throws Exception {
        PluginConfigurationSchema schema = new ConfigPlugin().getConfigurationSchema();
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("charset")).thenReturn(null);
        when(config.getProperty("scriptlang")).thenReturn(null);
        schema.validate(config);
    }

    @Test
    public void schemaRejectsNullConfiguration() {
        PluginConfigurationSchema schema = new ConfigPlugin().getConfigurationSchema();
        try {
            schema.validate(null);
            fail("Null configuration must throw");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Configuration cannot be null");
        }
    }

    @Test
    public void schemaAttributeLookupIncludesCharsetAndScriptlang() {
        PluginConfigurationSchema schema = new ConfigPlugin().getConfigurationSchema();
        assertNotNull(schema.getAttribute("charset"));
        assertNotNull(schema.getAttribute("scriptlang"));
        assertNull(schema.getAttribute("missing"));
        assertTrue(schema.allowsBody());
    }
}
