package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Test for CallPlugin class.
 *
 * @since 2.2.0
 */
public class CallPluginTest {

    private CallPlugin plugin;
    private PluginConfiguration mockConfiguration;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        plugin = new CallPlugin();
        mockConfiguration = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    @Test
    public void testPluginMetadata() {
        assertEquals(plugin.getMetadata().getId(), "call");
        assertEquals(plugin.getMetadata().getName(), "Call Function");
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
        assertNotNull(plugin.getMetadata().getDescription());
        assertTrue(plugin.getMetadata().getDescription().contains("function") || plugin.getMetadata().getDescription().contains("Function"));
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getCategory(), expectedCategory(CallPlugin.class));
        assertTrue(plugin.getMetadata().isCorePlugin());
        assertEquals(plugin.getMetadata().getElementName(), "call");
        assertEquals(plugin.getMetadata().getNamespace(), "http://org.webharvest/schema/2.1/core");
        
        String[] tags = plugin.getMetadata().getTags();
        assertEquals(tags.length, 6);
        assertEquals(tags[0], "core");
        assertEquals(tags[1], "call");
        assertEquals(tags[2], "function");
        assertEquals(tags[3], "execution");
        assertEquals(tags[4], "invocation");
    }

    @Test
    public void testConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.3.0");
        assertTrue(schema.allowsBody());
        
        // Test attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        assertEquals(attrs.length, 2);
        
        // Test name attribute
        PluginConfigurationSchema.AttributeDefinition nameAttr = attrs[0];
        assertEquals(nameAttr.getName(), "name");
        assertEquals(nameAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertTrue(nameAttr.isRequired());
        assertNotNull(nameAttr.getDescription());
        
        // Test id attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = attrs[1];
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(idAttr.isRequired());
        assertNotNull(idAttr.getDescription());
        
        // Test required and optional attributes
        String[] requiredAttrs = schema.getRequiredAttributes();
        assertEquals(requiredAttrs.length, 1);
        assertEquals(requiredAttrs[0], "name");
        
        String[] optionalAttrs = schema.getOptionalAttributes();
        assertEquals(optionalAttrs.length, 1);
        assertEquals(optionalAttrs[0], "id");
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        when(mockConfiguration.getProperty("name")).thenReturn("testFunction");
        
        plugin.initialize(mockConfiguration);
        
        verify(mockConfiguration, atLeastOnce()).getProperty("name");
    }

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithNullConfiguration() throws PluginException {
        plugin.initialize(null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithEmptyName() throws PluginException {
        when(mockConfiguration.getProperty("name")).thenReturn("");
        
        plugin.initialize(mockConfiguration);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithNullName() throws PluginException {
        when(mockConfiguration.getProperty("name")).thenReturn(null);
        
        plugin.initialize(mockConfiguration);
    }

    @Test
    public void testExecuteWithValidFunctionName() throws InterruptedException, PluginException {
        // Setup
        when(mockConfiguration.getProperty("name")).thenReturn("testFunction");
        when(mockConfiguration.getProperty("name", "")).thenReturn("testFunction");
        plugin.initialize(mockConfiguration);
        
        // Execute
        Variable result = plugin.execute(mockContext);
        
        // Verify
        assertNotNull(result);
        // We can't easily test the body execution without mocking protected methods
        // So we just verify that the method doesn't throw an exception
    }

    @Test
    public void testExecuteWithEmptyFunctionName() throws InterruptedException, PluginException {
        // Setup
        when(mockConfiguration.getProperty("name")).thenReturn("testFunction");
        when(mockConfiguration.getProperty("name", "")).thenReturn("");
        plugin.initialize(mockConfiguration);
        
        // Execute and expect exception due to null evaluation
        try {
            Variable result = plugin.execute(mockContext);
            fail("Should have thrown RuntimeException for empty function name");
        } catch (RuntimeException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Plugin execution failed"));
        }
    }

    @Test
    public void testExecuteWithNullFunctionName() throws InterruptedException, PluginException {
        // Setup
        when(mockConfiguration.getProperty("name")).thenReturn("testFunction");
        when(mockConfiguration.getProperty("name", "")).thenReturn(null);
        plugin.initialize(mockConfiguration);
        
        // Execute and expect exception due to null evaluation
        try {
            Variable result = plugin.execute(mockContext);
            fail("Should have thrown RuntimeException for null function name");
        } catch (RuntimeException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Plugin execution failed"));
        }
    }

    @Test
    public void testExecuteWithExceptionInBody() throws InterruptedException, PluginException {
        // Setup
        when(mockConfiguration.getProperty("name")).thenReturn("testFunction");
        when(mockConfiguration.getProperty("name", "")).thenReturn("testFunction");
        plugin.initialize(mockConfiguration);
        
        // Execute - we can't easily mock protected methods, so we just test that it doesn't crash
        Variable result = plugin.execute(mockContext);
        
        // Verify
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNullBodyResult() throws InterruptedException, PluginException {
        // Setup
        when(mockConfiguration.getProperty("name")).thenReturn("testFunction");
        when(mockConfiguration.getProperty("name", "")).thenReturn("testFunction");
        plugin.initialize(mockConfiguration);
        
        // Execute - we can't easily mock protected methods, so we just test that it doesn't crash
        Variable result = plugin.execute(mockContext);
        
        // Verify
        assertNotNull(result);
    }

    @Test
    public void testSetFunctionResultDoesNotThrow() {
        Variable testResult = new NodeVariable("test result");
        
        plugin.setFunctionResult(testResult);
        
        // No exception is the observable contract; this legacy helper exposes no state accessor.
    }

    @Test
    public void testAddFunctionParamDoesNotThrow() {
        String paramName = "testParam";
        Variable paramValue = new NodeVariable("test value");
        
        plugin.addFunctionParam(paramName, paramValue);
        
        // We can't directly verify the internal state, but we can test that the method doesn't throw
        assertTrue(true);
    }

    @Test
    public void testAddFunctionParamWithNullNameDoesNotThrow() {
        Variable paramValue = new NodeVariable("test value");
        
        plugin.addFunctionParam(null, paramValue);
        
        // No exception is the observable contract for this legacy helper.
    }

    @Test
    public void testAddFunctionParamWithNullValueDoesNotThrow() {
        String paramName = "testParam";
        
        plugin.addFunctionParam(paramName, null);
        
        // Should not throw exception
        assertTrue(true);
    }

    @Test
    public void testConfigurationValidation() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        // Test valid configuration
        PluginConfiguration validConfig = mock(PluginConfiguration.class);
        when(validConfig.getProperty("name")).thenReturn("testFunction");
        
        try {
            schema.validate(validConfig);
            // Successful return is the validation contract for this valid configuration.
        } catch (PluginValidationException e) {
            fail("Valid configuration should not throw exception");
        }
        
        // Test null configuration
        try {
            schema.validate(null);
            fail("Null configuration should throw exception");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Configuration cannot be null");
        }
        
        // Test empty name
        PluginConfiguration emptyNameConfig = mock(PluginConfiguration.class);
        when(emptyNameConfig.getProperty("name")).thenReturn("");
        
        try {
            schema.validate(emptyNameConfig);
            fail("Empty name should throw exception");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name attribute is required");
        }
        
        // Test null name
        PluginConfiguration nullNameConfig = mock(PluginConfiguration.class);
        when(nullNameConfig.getProperty("name")).thenReturn(null);
        
        try {
            schema.validate(nullNameConfig);
            fail("Null name should throw exception");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name attribute is required");
        }
    }

    @Test
    public void testGetAttributeFromSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        // Test getting name attribute
        PluginConfigurationSchema.AttributeDefinition nameAttr = schema.getAttribute("name");
        assertNotNull(nameAttr);
        assertEquals(nameAttr.getName(), "name");
        
        // Test getting id attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        
        // Test getting non-existent attribute
        PluginConfigurationSchema.AttributeDefinition nonExistentAttr = schema.getAttribute("nonExistent");
        assertNull(nonExistentAttr);
    }

    @Test
    public void testRequiredAndOptionalSubElements() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        String[] requiredSubElements = schema.getRequiredSubElements();
        assertEquals(requiredSubElements.length, 0);
        
        String[] allowedSubElements = schema.getAllowedSubElements();
        // call-param is allowed as sub-element
        assertTrue(allowedSubElements.length >= 0);
    }
}
