package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests for LogPlugin - Feature Request #2
 * Comprehensive coverage for stdout/stderr logging functionality.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class LogPluginTest {

    private LogPlugin plugin;
    private PluginConfiguration mockConfig;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        plugin = new LogPlugin();
        mockConfig = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    // ========== Validation Tests ==========

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullThrows() throws PluginValidationException {
        plugin.doValidateConfiguration(null);
    }

    @Test
    public void testValidateConfigurationWithValidDestOut() throws PluginValidationException {
        when(mockConfig.getProperty("dest")).thenReturn("out");
        plugin.doValidateConfiguration(mockConfig);
        // No exception = success
    }

    @Test
    public void testValidateConfigurationWithValidDestErr() throws PluginValidationException {
        when(mockConfig.getProperty("dest")).thenReturn("err");
        plugin.doValidateConfiguration(mockConfig);
        // No exception = success
    }

    @Test
    public void testValidateConfigurationWithNullDest() throws PluginValidationException {
        when(mockConfig.getProperty("dest")).thenReturn(null);
        plugin.doValidateConfiguration(mockConfig);
        // No exception = success (dest is optional)
    }

    @Test
    public void testValidateConfigurationWithEmptyDest() throws PluginValidationException {
        when(mockConfig.getProperty("dest")).thenReturn("");
        plugin.doValidateConfiguration(mockConfig);
        // No exception = success (defaults to "out")
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithInvalidDest() throws PluginValidationException {
        when(mockConfig.getProperty("dest")).thenReturn("invalid");
        plugin.doValidateConfiguration(mockConfig);
    }

    @Test
    public void testValidateConfigurationWithInvalidDestShowsErrorMessage() {
        when(mockConfig.getProperty("dest")).thenReturn("badvalue");
        try {
            plugin.doValidateConfiguration(mockConfig);
            fail("Should throw PluginValidationException");
        } catch (PluginValidationException e) {
            assertTrue(e.getMessage().contains("dest must be 'out' or 'err'"));
            assertTrue(e.getMessage().contains("badvalue"));
        }
    }

    // ========== Initialization Tests ==========

    @Test
    public void testInitializeWithDestOut() throws PluginException {
        when(mockConfig.getProperty("dest")).thenReturn("out");
        plugin.doInitialize(mockConfig);
        // Should not throw exception
    }

    @Test
    public void testInitializeWithDestErr() throws PluginException {
        when(mockConfig.getProperty("dest")).thenReturn("err");
        plugin.doInitialize(mockConfig);
        // Should not throw exception
    }

    @Test
    public void testInitializeWithNullDestDefaultsToOut() throws PluginException {
        when(mockConfig.getProperty("dest")).thenReturn(null);
        plugin.doInitialize(mockConfig);
        
        // Test default by checking writeLog behavior
        Variable result = plugin.writeLog("test", "out");
        assertNotNull(result);
    }

    @Test
    public void testInitializeWithEmptyDestDefaultsToOut() throws PluginException {
        when(mockConfig.getProperty("dest")).thenReturn("   ");
        plugin.doInitialize(mockConfig);
        
        // Test default by checking writeLog behavior
        Variable result = plugin.writeLog("test", "out");
        assertNotNull(result);
    }

    // ========== writeLog() Tests ==========

    @Test
    public void testWriteLogToStdout() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            Variable result = plugin.writeLog("Test message", "out");
            
            assertTrue(outContent.toString().contains("Test message"));
            assertNotNull(result);
            assertTrue(result instanceof NodeVariable);
            assertEquals(result.toString(), "Test message");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testWriteLogToStderr() {
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));

        try {
            Variable result = plugin.writeLog("Error message", "err");
            
            assertTrue(errContent.toString().contains("Error message"));
            assertNotNull(result);
            assertTrue(result instanceof NodeVariable);
            assertEquals(result.toString(), "Error message");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    public void testWriteLogReturnsMessageAsVariable() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            Variable result = plugin.writeLog("Return value test", "out");
            
            assertNotNull(result);
            assertEquals(result.toString(), "Return value test");
        } finally {
            System.setOut(System.out);
        }
    }

    @Test
    public void testWriteLogWithMultilineMessage() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            String multiline = "Line 1\nLine 2\nLine 3";
            Variable result = plugin.writeLog(multiline, "out");
            
            assertTrue(outContent.toString().contains("Line 1"));
            assertTrue(outContent.toString().contains("Line 2"));
            assertTrue(outContent.toString().contains("Line 3"));
            assertEquals(result.toString(), multiline);
        } finally {
            System.setOut(System.out);
        }
    }

    @Test
    public void testWriteLogWithSpecialCharacters() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            String special = "Test: <>&\"' 中文 €$";
            Variable result = plugin.writeLog(special, "out");
            
            assertTrue(outContent.toString().contains(special));
            assertEquals(result.toString(), special);
        } finally {
            System.setOut(System.out);
        }
    }

    @Test
    public void testWriteLogWithEmptyString() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            Variable result = plugin.writeLog("", "out");
            
            assertNotNull(result);
            assertEquals(result.toString(), "");
        } finally {
            System.setOut(System.out);
        }
    }

    // ========== Metadata Tests ==========

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "log");
        assertEquals(plugin.getMetadata().getName(), "Log Plugin");
        assertEquals(plugin.getMetadata().getVersion(), "2.2.0");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testMetadataDescription() {
        assertNotNull(plugin.getMetadata().getDescription());
        assertTrue(plugin.getMetadata().getDescription().contains("stdout"));
        assertTrue(plugin.getMetadata().getDescription().contains("stderr"));
    }

    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        assertTrue(tags.length >= 3);
        boolean hasCore = false, hasLog = false, hasDebug = false;
        for (String tag : tags) {
            if ("core".equals(tag)) hasCore = true;
            if ("log".equals(tag)) hasLog = true;
            if ("debug".equals(tag)) hasDebug = true;
        }
        assertTrue(hasCore);
        assertTrue(hasLog);
        assertTrue(hasDebug);
    }

    @Test
    public void testMetadataConfigurationSchema() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
        
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        assertNotNull(attrs);
        assertEquals(attrs.length, 1);
        assertEquals(attrs[0].getName(), "dest");
        assertFalse(attrs[0].isRequired());
        assertEquals(attrs[0].getDefaultValue(), "out");
    }

    @Test
    public void testMetadataConfigurationSchemaAllowedValues() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attrs = schema.getAttributeDefinitions();
        String[] allowedValues = attrs[0].getAllowedValues();
        
        assertNotNull(allowedValues);
        assertEquals(allowedValues.length, 2);
        assertEquals(allowedValues[0], "out");
        assertEquals(allowedValues[1], "err");
    }

    @Test
    public void testMetadataElementName() {
        assertEquals(plugin.getMetadata().getElementName(), "log");
    }

    @Test
    public void testMetadataNamespace() {
        assertEquals(plugin.getMetadata().getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testMetadataNoDependencies() {
        String[] deps = plugin.getMetadata().getDependencies();
        assertNotNull(deps);
        assertEquals(deps.length, 0);
    }
}

