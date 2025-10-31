package org.webharvest.plugin.core;

import com.google.common.eventbus.EventBus;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.events.ScraperExecutionExitEvent;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for ExitPlugin.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class ExitPluginTest {

    @Mock
    private PluginConfiguration configuration;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private EventBus eventBus;
    
    private ExitPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new ExitPlugin();
        
        // Inject EventBus using reflection
        try {
            java.lang.reflect.Field eventBusField = ExitPlugin.class.getDeclaredField("eventBus");
            eventBusField.setAccessible(true);
            eventBusField.set(plugin, eventBus);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject EventBus", e);
        }
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "exit");
        assertEquals(metadata.getName(), "Exit Plugin");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getDescription(), "Exit plugin that provides conditional exit functionality");
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertEquals(metadata.getCategory(), "Utilities");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "exit");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.1.0");
        assertFalse(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getOptionalAttributes().length, 3);
        
        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 3);
        
        // Check id attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(idAttr.isRequired());
        assertNull(idAttr.getDefaultValue());
        
        // Check condition attribute
        PluginConfigurationSchema.AttributeDefinition conditionAttr = schema.getAttribute("condition");
        assertNotNull(conditionAttr);
        assertEquals(conditionAttr.getName(), "condition");
        assertEquals(conditionAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(conditionAttr.isRequired());
        assertEquals(conditionAttr.getDefaultValue(), "true");
        
        // Check message attribute
        PluginConfigurationSchema.AttributeDefinition messageAttr = schema.getAttribute("message");
        assertNotNull(messageAttr);
        assertEquals(messageAttr.getName(), "message");
        assertEquals(messageAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(messageAttr.isRequired());
        assertEquals(messageAttr.getDefaultValue(), "");
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        plugin.initialize(configuration);
        
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithNullConfiguration() throws PluginException {
        plugin.initialize(null);
    }

    @Test
    public void testValidateConfigurationWithValidConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(configuration);
        
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testExecuteWithTrueCondition() throws InterruptedException, PluginException {
        when(configuration.getProperty("condition")).thenReturn("true");
        when(configuration.getProperty("message")).thenReturn("Test exit message");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        
        // Verify that exit event was posted
        verify(eventBus).post(any(ScraperExecutionExitEvent.class));
    }

    @Test
    public void testExecuteWithFalseCondition() throws InterruptedException, PluginException {
        when(configuration.getProperty("condition")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        
        // Verify that no exit event was posted
        verify(eventBus, never()).post(any(ScraperExecutionExitEvent.class));
    }

    @Test
    public void testExecuteWithEmptyCondition() throws InterruptedException, PluginException {
        when(configuration.getProperty("condition")).thenReturn("");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        
        // Verify that exit event was posted (empty condition defaults to "true")
        verify(eventBus).post(any(ScraperExecutionExitEvent.class));
    }

    @Test
    public void testExecuteWithNullCondition() throws InterruptedException, PluginException {
        when(configuration.getProperty("condition")).thenReturn(null);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        
        // Verify that exit event was posted (null condition defaults to "true")
        verify(eventBus).post(any(ScraperExecutionExitEvent.class));
    }

    @Test
    public void testExecuteWithNullMessage() throws InterruptedException, PluginException {
        when(configuration.getProperty("condition")).thenReturn("true");
        when(configuration.getProperty("message")).thenReturn(null);
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        
        // Verify that exit event was posted with empty message
        verify(eventBus).post(any(ScraperExecutionExitEvent.class));
    }

    @Test
    public void testExecuteWithEmptyMessage() throws InterruptedException, PluginException {
        when(configuration.getProperty("condition")).thenReturn("true");
        when(configuration.getProperty("message")).thenReturn("");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        
        // Verify that exit event was posted with empty message
        verify(eventBus).post(any(ScraperExecutionExitEvent.class));
    }

    @Test
    public void testExecuteWithIdAttribute() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("test-exit");
        when(configuration.getProperty("condition")).thenReturn("true");
        when(configuration.getProperty("message")).thenReturn("Exit with ID");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        
        // Verify that exit event was posted
        verify(eventBus).post(any(ScraperExecutionExitEvent.class));
    }

    @Test
    public void testExecuteMultipleTimes() throws InterruptedException, PluginException {
        when(configuration.getProperty("condition")).thenReturn("true");
        when(configuration.getProperty("message")).thenReturn("Multiple executions");
        
        plugin.initialize(configuration);
        
        // Execute multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        
        // Verify that exit event was posted for each execution
        verify(eventBus, times(2)).post(any(ScraperExecutionExitEvent.class));
    }
}
