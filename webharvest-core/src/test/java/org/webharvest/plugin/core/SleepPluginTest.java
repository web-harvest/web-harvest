package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginDef;
import org.webharvest.definition.IElementDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SleepPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private SleepPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new SleepPlugin();
        
        plugin.setElementDef(elementDef);
        
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(xmlNode.getText()).thenReturn("");
        when(elementDef.hasOperations()).thenReturn(false);
        
        when(configuration.getProperty("milliseconds")).thenReturn("100");
        
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Sleep Plugin");
        assertEquals(metadata.getDescription(), "Sleep plugin that pauses execution for specified number of milliseconds");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "sleep");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        PluginConfigurationSchema.AttributeDefinition millisAttr = schema.getAttributeDefinitions()[0];
        assertNotNull(millisAttr);
        assertEquals(millisAttr.getName(), "milliseconds");
        assertFalse(millisAttr.isRequired());
        assertEquals(millisAttr.getDefaultValue(), "0");
        assertEquals(millisAttr.getMinValue(), 0);
        
        assertFalse(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.1.0");
    }

    @Test
    public void testSleepPluginInitialize() throws PluginException {
        SleepPlugin newPlugin = new SleepPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("500");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testSleepPluginInitializeWithNullConfiguration() throws PluginException {
        SleepPlugin newPlugin = new SleepPlugin();
        newPlugin.setElementDef(elementDef);
        
        newPlugin.initialize(null);
    }

    @Test
    public void testSleepPluginInitializeWithMissingMilliseconds() throws PluginException {
        SleepPlugin newPlugin = new SleepPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn(null);
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test
    public void testSleepPluginInitializeWithEmptyMilliseconds() throws PluginException {
        SleepPlugin newPlugin = new SleepPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test
    public void testSleepPluginExecute() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testSleepPluginExecuteWithZeroMilliseconds() throws InterruptedException, PluginException {
        SleepPlugin newPlugin = new SleepPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("0");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testSleepPluginExecuteWithInvalidMilliseconds() throws InterruptedException, PluginException {
        SleepPlugin newPlugin = new SleepPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("invalid");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testSleepPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        SleepPlugin testPlugin = new SleepPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };
        testPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("100");
        testPlugin.initialize(config);
        
        testPlugin.execute(context);
    }

    @Test
    public void testSleepPluginValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("100");
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testSleepPluginValidateConfigurationWithNull() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testSleepPluginValidateConfigurationWithMissingMilliseconds() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn(null);
        plugin.validateConfiguration(config);
    }

    @Test
    public void testSleepPluginValidateConfigurationWithEmptyMilliseconds() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("");
        plugin.validateConfiguration(config);
    }

    @Test
    public void testSleepPluginExecuteMultipleTimes() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }

    @Test
    public void testSleepPluginWithNegativeMilliseconds() throws InterruptedException, PluginException {
        SleepPlugin newPlugin = new SleepPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("milliseconds")).thenReturn("-100");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
}
