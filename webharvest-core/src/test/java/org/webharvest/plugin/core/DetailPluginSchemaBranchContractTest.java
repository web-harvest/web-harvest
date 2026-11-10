package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Configuration-schema validation branches on nested core detail plugins that
 * previously had no JaCoCo branch coverage.
 */
public class DetailPluginSchemaBranchContractTest {

    @Test
    public void httpParamSchemaRejectsInvalidNameAndAcceptsValidMime() throws Exception {
        PluginConfigurationSchema schema = new HttpParamPlugin().getConfigurationSchema();

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("name")).thenReturn("page");
        when(valid.getProperty("contenttype")).thenReturn("text/plain");
        schema.validate(valid);

        PluginConfiguration missingName = mock(PluginConfiguration.class);
        when(missingName.getProperty("name")).thenReturn("  ");
        try {
            schema.validate(missingName);
            fail("Blank param name must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name attribute is required");
        }

        PluginConfiguration badName = mock(PluginConfiguration.class);
        when(badName.getProperty("name")).thenReturn("bad name");
        try {
            schema.validate(badName);
            fail("Whitespace in param name must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name must be a valid HTTP parameter name");
        }

        PluginConfiguration badMime = mock(PluginConfiguration.class);
        when(badMime.getProperty("name")).thenReturn("file");
        when(badMime.getProperty("contenttype")).thenReturn("not-a-mime");
        try {
            schema.validate(badMime);
            fail("Invalid content type must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Content type must be a valid MIME type");
        }
    }

    @Test
    public void httpParamSchemaRejectsNullConfiguration() {
        PluginConfigurationSchema schema = new HttpParamPlugin().getConfigurationSchema();
        try {
            schema.validate(null);
            fail("Null configuration must throw");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Configuration cannot be null");
        }
    }

    @Test
    public void xqParamSchemaRejectsUnknownTypeAndAcceptsInteger() throws Exception {
        PluginConfigurationSchema schema = new XQParamPlugin().getConfigurationSchema();

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("name")).thenReturn("limit");
        when(valid.getProperty("type")).thenReturn("integer");
        schema.validate(valid);

        PluginConfiguration missingName = mock(PluginConfiguration.class);
        when(missingName.getProperty("name")).thenReturn(null);
        try {
            schema.validate(missingName);
            fail("Missing name must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name attribute is required");
        }

        PluginConfiguration badType = mock(PluginConfiguration.class);
        when(badType.getProperty("name")).thenReturn("x");
        when(badType.getProperty("type")).thenReturn("decimal");
        try {
            schema.validate(badType);
            fail("Unknown XQ type must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Invalid type: decimal");
        }
    }

    @Test
    public void templateSchemaRejectsUnsupportedLanguage() throws Exception {
        PluginConfigurationSchema schema = new TemplatePlugin().getConfigurationSchema();

        PluginConfiguration groovy = mock(PluginConfiguration.class);
        when(groovy.getProperty("language")).thenReturn("groovy");
        schema.validate(groovy);

        PluginConfiguration bad = mock(PluginConfiguration.class);
        when(bad.getProperty("language")).thenReturn("python");
        try {
            schema.validate(bad);
            fail("Unsupported template language must throw");
        } catch (PluginValidationException e) {
            assertTrue(e.getMessage().contains("Invalid language value"));
        }
    }

    @Test
    public void valueOfSchemaRequiresExpression() throws Exception {
        PluginConfigurationSchema schema = new ValueOfPlugin().getConfigurationSchema();

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("expr")).thenReturn(".");
        schema.validate(valid);

        PluginConfiguration missing = mock(PluginConfiguration.class);
        when(missing.getProperty("expr")).thenReturn(" ");
        try {
            schema.validate(missing);
            fail("Blank expression must be rejected");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Expression attribute is required for value-of plugin");
        }
    }

    @Test
    public void requestHeaderSchemaRejectsInvalidHeaderName() throws Exception {
        PluginConfigurationSchema schema = new HttpHeaderPlugin().getConfigurationSchema();

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("name")).thenReturn("X-Custom");
        schema.validate(valid);

        PluginConfiguration bad = mock(PluginConfiguration.class);
        when(bad.getProperty("name")).thenReturn("bad header");
        try {
            schema.validate(bad);
            fail("Invalid header name must throw");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name must be a valid HTTP header name");
        }
    }

    @Test
    public void configSchemaRejectsUnknownCharsetAndScriptLanguage() throws Exception {
        PluginConfigurationSchema schema = new ConfigPlugin().getConfigurationSchema();

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("charset")).thenReturn("utf-8");
        when(valid.getProperty("scriptlang")).thenReturn("javascript");
        schema.validate(valid);

        PluginConfiguration badCharset = mock(PluginConfiguration.class);
        when(badCharset.getProperty("charset")).thenReturn("EBCDIC");
        try {
            schema.validate(badCharset);
            fail("Unknown charset must throw");
        } catch (PluginValidationException e) {
            assertTrue(e.getMessage().contains("Invalid charset"));
        }

        PluginConfiguration badScript = mock(PluginConfiguration.class);
        when(badScript.getProperty("scriptlang")).thenReturn("ruby");
        try {
            schema.validate(badScript);
            fail("Unknown script language must throw");
        } catch (PluginValidationException e) {
            assertTrue(e.getMessage().contains("Invalid scriptlang"));
        }
    }

    @Test
    public void callSchemaRejectsBlankFunctionName() {
        PluginConfigurationSchema schema = new CallPlugin().getConfigurationSchema();
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn("   ");
        try {
            schema.validate(config);
            fail("Blank call name must throw");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name attribute is required");
        }
    }
}
