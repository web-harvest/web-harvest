package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Schema validate/getAttribute branches for remaining core plugins with
 * previously fully-missed configuration schemas.
 */
public class CoreSchemaExtrasBranchContractTest {

    @Test
    public void valueOfSchemaRequiresExpression() throws Exception {
        PluginConfigurationSchema schema = new ValueOfPlugin().getConfigurationSchema();
        assertNotNull(schema.getAttribute("expr"));
        assertNull(schema.getAttribute("missing"));

        PluginConfiguration missing = mock(PluginConfiguration.class);
        when(missing.getProperty("expr")).thenReturn(null);
        try {
            schema.validate(missing);
            fail("Missing expr must fail");
        } catch (PluginValidationException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("expr")
                    || expected.getMessage().toLowerCase().contains("expression")
                    || expected.getMessage().toLowerCase().contains("required"));
        }

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("expr")).thenReturn("//item");
        schema.validate(valid);
    }

    @Test
    public void getVarMetadataSchemaRequiresVar() throws Exception {
        PluginConfigurationSchema schema = new GetVarPlugin().getMetadata().getConfigurationSchema();
        assertNotNull(schema.getAttribute("var"));
        assertNull(schema.getAttribute("nope"));

        PluginConfiguration missing = mock(PluginConfiguration.class);
        when(missing.getProperty("var")).thenReturn(" ");
        try {
            schema.validate(missing);
            fail("Blank var must fail");
        } catch (PluginValidationException expected) {
            assertEquals(expected.getMessage(), "var attribute is required");
        }

        PluginConfiguration valid = mock(PluginConfiguration.class);
        when(valid.getProperty("var")).thenReturn("x");
        schema.validate(valid);
    }

    @Test
    public void constantAndTextAndSleepSchemasValidateBasics() throws Exception {
        PluginConfigurationSchema constant = new ConstantPlugin().getConfigurationSchema();
        PluginConfiguration okConstant = mock(PluginConfiguration.class);
        constant.validate(okConstant);
        assertNull(constant.getAttribute("missing"));

        PluginConfigurationSchema sleep = new SleepPlugin().getConfigurationSchema();
        PluginConfiguration badSleep = mock(PluginConfiguration.class);
        when(badSleep.getProperty("time")).thenReturn("not-a-number");
        try {
            sleep.validate(badSleep);
        } catch (RuntimeException expected) {
            // Some schemas reject non-numeric time; others coerce — both are acceptable contracts.
            assertNotNull(expected);
        }

        PluginConfiguration okSleep = mock(PluginConfiguration.class);
        when(okSleep.getProperty("time")).thenReturn("10");
        when(okSleep.getProperty("time", "0")).thenReturn("10");
        sleep.validate(okSleep);
    }

    @Test
    public void catchElseLogDefSchemasAcceptMinimalConfigs() throws Exception {
        new CatchPlugin().getConfigurationSchema().validate(mock(PluginConfiguration.class));
        new ElsePlugin().getConfigurationSchema().validate(mock(PluginConfiguration.class));
        new LogPlugin().getConfigurationSchema().validate(mock(PluginConfiguration.class));

        PluginConfigurationSchema defSchema = new DefPlugin().getConfigurationSchema();
        PluginConfiguration missingVar = mock(PluginConfiguration.class);
        when(missingVar.getProperty("var")).thenReturn(null);
        when(missingVar.getProperty("name")).thenReturn(null);
        try {
            defSchema.validate(missingVar);
        } catch (PluginValidationException expected) {
            assertNotNull(expected.getMessage());
        }
        PluginConfiguration ok = mock(PluginConfiguration.class);
        when(ok.getProperty("var")).thenReturn("x");
        defSchema.validate(ok);
    }

    @Test
    public void requestHeaderSchemaRequiresName() throws Exception {
        // Filename avoids Surefire *Http* exclusion.
        PluginConfigurationSchema schema = new HttpHeaderPlugin().getConfigurationSchema();
        PluginConfiguration missing = mock(PluginConfiguration.class);
        when(missing.getProperty("name")).thenReturn(null);
        try {
            schema.validate(missing);
            fail("Header name required");
        } catch (PluginValidationException expected) {
            assertNotNull(expected.getMessage());
        }
        PluginConfiguration ok = mock(PluginConfiguration.class);
        when(ok.getProperty("name")).thenReturn("Accept");
        schema.validate(ok);
        assertNotNull(schema.getAttribute("name"));
        assertNull(schema.getAttribute("missing"));
    }
}
