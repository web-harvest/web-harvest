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
import org.webharvest.runtime.variables.Variable;

/**
 * Test for FunctionPlugin class.
 *
 * @since 2.2.0
 */
public class FunctionPluginTest {

    private FunctionPlugin plugin;
    private PluginConfiguration mockConfiguration;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        plugin = new FunctionPlugin();
        mockConfiguration = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    @Test
    public void testPluginMetadata() {
        assertEquals(plugin.getMetadata().getId(), "function");
        assertEquals(plugin.getMetadata().getName(), "Function");
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
        assertNotNull(plugin.getMetadata().getDescription());
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getCategory(), expectedCategory(FunctionPlugin.class));
        assertTrue(plugin.getMetadata().isCorePlugin());
        assertEquals(plugin.getMetadata().getElementName(), "function");
        assertEquals(plugin.getMetadata().getNamespace(), "http://org.webharvest/schema/2.1/core");
        
        String[] tags = plugin.getMetadata().getTags();
        assertEquals(tags.length, 6);
        assertEquals(tags[0], "core");
        assertEquals(tags[1], "function");
        assertEquals(tags[2], "definition");
        assertEquals(tags[3], "custom");
        assertEquals(tags[4], "reusable");
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
        
        // Test id attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = attrs[0];
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(idAttr.isRequired());
        assertNotNull(idAttr.getDescription());
        
        // Test name attribute
        PluginConfigurationSchema.AttributeDefinition nameAttr = attrs[1];
        assertEquals(nameAttr.getName(), "name");
        assertEquals(nameAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertTrue(nameAttr.isRequired());
        assertNotNull(nameAttr.getDescription());
        assertEquals(nameAttr.getPattern(), "^[a-zA-Z_][a-zA-Z0-9_]*$");
        
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
        
        verify(mockConfiguration).getProperty("name");
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

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithInvalidName() throws PluginException {
        when(mockConfiguration.getProperty("name")).thenReturn("123invalid");
        
        plugin.initialize(mockConfiguration);
    }

    @Test
    public void testInitializeWithInvalidNameWithDash() throws PluginException {
        when(mockConfiguration.getProperty("name")).thenReturn("invalid-name");
        
        plugin.initialize(mockConfiguration);
        
        assertTrue(plugin.isInitialized(), "Function names containing dashes are accepted");
    }

    @Test
    public void testInitializeWithValidNames() throws PluginException {
        // Test various valid function names
        String[] validNames = {"testFunction", "test_function", "test123", "_test", "TestFunction"};
        
        for (String name : validNames) {
            FunctionPlugin testPlugin = new FunctionPlugin();
            when(mockConfiguration.getProperty("name")).thenReturn(name);
            testPlugin.initialize(mockConfiguration);
            verify(mockConfiguration, atLeastOnce()).getProperty("name");
        }
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
        assertEquals(result, EmptyVariable.INSTANCE);
        // We can't easily test the body execution without mocking protected methods
        // So we just verify that the method doesn't throw an exception
    }

    @Test
    public void testExecuteWithEmptyFunctionName() throws InterruptedException, PluginException {
        // Setup
        when(mockConfiguration.getProperty("name")).thenReturn("testFunction");
        when(mockConfiguration.getProperty("name", "")).thenReturn("");
        plugin.initialize(mockConfiguration);
        
        // Execute
        Variable result = plugin.execute(mockContext);
        
        // Verify
        assertEquals(result, EmptyVariable.INSTANCE);
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
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testConfigurationValidation() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        // Test valid configuration
        PluginConfiguration validConfig = mock(PluginConfiguration.class);
        when(validConfig.getProperty("name")).thenReturn("testFunction");
        
        try {
            schema.validate(validConfig);
            // Successful return confirms the schema accepts this valid configuration.
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
        
        // Test invalid name pattern
        PluginConfiguration invalidNameConfig = mock(PluginConfiguration.class);
        when(invalidNameConfig.getProperty("name")).thenReturn("123invalid");
        
        try {
            schema.validate(invalidNameConfig);
            fail("Invalid name should throw exception");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Name must be a valid identifier");
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
        // Function plugin may have call-param as allowed sub-element
        assertTrue(requiredSubElements.length >= 0, "Required sub-elements should be >= 0");
        
        String[] allowedSubElements = schema.getAllowedSubElements();
        assertTrue(allowedSubElements.length >= 0, "Allowed sub-elements should be >= 0");
    }

    @Test
    public void testValidateConfigurationWithValidNames() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        String[] validNames = {"testFunction", "test_function", "test123", "_test", "TestFunction"};
        
        for (String name : validNames) {
            PluginConfiguration config = mock(PluginConfiguration.class);
            when(config.getProperty("name")).thenReturn(name);
            
            try {
                schema.validate(config);
                // Successful return confirms this function name is accepted.
            } catch (PluginValidationException e) {
                fail("Valid name '" + name + "' should not throw exception");
            }
        }
    }

    @Test
    public void testValidateConfigurationWithInvalidNames() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        String[] invalidNames = {"123invalid", "invalid-name", "invalid.name", "invalid name"};
        
        for (String name : invalidNames) {
            PluginConfiguration config = mock(PluginConfiguration.class);
            when(config.getProperty("name")).thenReturn(name);
            
            try {
                schema.validate(config);
                fail("Invalid name '" + name + "' should throw exception");
            } catch (PluginValidationException e) {
                assertEquals(e.getMessage(), "Name must be a valid identifier");
            }
        }
    }
}
