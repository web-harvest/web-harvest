package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Public {@link PluginConfigurationSchema#validate(PluginConfiguration)} branches
 * on nested metadata schemas that are cheap to exercise in isolation.
 */
public class NestedPluginSchemaBranchContractTest {

    @Test
    public void httpSchemaAllowsGetAndPostOnly() throws Exception {
        PluginConfigurationSchema schema = new HttpPlugin().getMetadata().getConfigurationSchema();

        PluginConfiguration getConfig = mock(PluginConfiguration.class);
        when(getConfig.getProperty("url")).thenReturn("https://example.com/");
        when(getConfig.getProperty("method", "GET")).thenReturn("GET");
        schema.validate(getConfig);

        PluginConfiguration postConfig = mock(PluginConfiguration.class);
        when(postConfig.getProperty("url")).thenReturn("https://example.com/");
        when(postConfig.getProperty("method", "GET")).thenReturn("POST");
        schema.validate(postConfig);

        PluginConfiguration putConfig = mock(PluginConfiguration.class);
        when(putConfig.getProperty("url")).thenReturn("https://example.com/");
        when(putConfig.getProperty("method", "GET")).thenReturn("PUT");
        try {
            schema.validate(putConfig);
            fail("PUT must be rejected by metadata schema validator");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Method must be GET or POST");
        }
    }

    @Test
    public void httpSchemaRequiresUrl() throws Exception {
        PluginConfigurationSchema schema = new HttpPlugin().getMetadata().getConfigurationSchema();
        PluginConfiguration missingUrl = mock(PluginConfiguration.class);
        when(missingUrl.getProperty("url")).thenReturn("  ");
        try {
            schema.validate(missingUrl);
            fail("Blank URL must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "URL is required");
        }
    }

    @Test
    public void whileSchemaValidatesMaxloopsRangeAndFormat() throws Exception {
        PluginConfigurationSchema schema = new WhilePlugin().getMetadata().getConfigurationSchema();

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("maxloops")).thenReturn("10");
        schema.validate(valid);

        PluginConfiguration tooLow = mock(PluginConfiguration.class);
        when(tooLow.getProperty("maxloops")).thenReturn("0");
        try {
            schema.validate(tooLow);
            fail("Zero maxloops must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Maxloops must be between 1 and 1000000");
        }

        PluginConfiguration nonNumeric = mock(PluginConfiguration.class);
        when(nonNumeric.getProperty("maxloops")).thenReturn("lots");
        try {
            schema.validate(nonNumeric);
            fail("Non-numeric maxloops must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Maxloops must be a valid integer");
        }
    }

    @Test
    public void textSchemaRejectsUnknownCharset() throws Exception {
        PluginConfigurationSchema schema = new TextPlugin().getMetadata().getConfigurationSchema();
        PluginConfiguration badCharset = mock(PluginConfiguration.class);
        when(badCharset.getProperty("charset")).thenReturn("NOT-A-CHARSET");
        try {
            schema.validate(badCharset);
            fail("Invalid charset must throw PluginValidationException");
        } catch (PluginValidationException e) {
            assertTrue(e.getMessage().contains("Invalid charset"));
        }
    }
}
