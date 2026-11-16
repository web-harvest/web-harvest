/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.CorePluginDef;
// import org.webharvest.definition.IElementDef;
// import org.webharvest.definition.XmlNode;
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

/**
 * Test for CatchPlugin
 */
public class CatchPluginTest {

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private CatchPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new CatchPlugin();
        
        // Mock hasChildren() for new architecture
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(java.util.Collections.emptyList());
        
        
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(xmlNode.getText()).thenReturn("Test catch content");
        // when(elementDef.hasOperations()).thenReturn(false);
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Catch");
        assertNotNull(metadata.getDescription());
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "catch");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttributeDefinitions()[0];
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.3.0");
    }

    @Test
    public void testCatchPluginInitialize() throws PluginException {
        CatchPlugin newPlugin = new CatchPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("catchId");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testCatchPluginInitializeWithNullConfiguration() throws PluginException {
        CatchPlugin newPlugin = new CatchPlugin();
        
        newPlugin.initialize(null);
    }

    @Test
    public void testCatchPluginExecuteWithNoError() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        when(context.getVar("error")).thenReturn(null);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testCatchPluginExecuteWithEmptyError() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        when(context.getVar("error")).thenReturn(EmptyVariable.INSTANCE);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testCatchPluginExecuteWithError() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        when(context.getVar("error")).thenReturn(new NodeVariable("Test error message"));
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
        verify(context).setLocalVar("catchResult", result);
    }

    @Test
    public void testCatchPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        Thread.interrupted();
        try {
            when(context.getCharset()).thenReturn("UTF-8");
            
            CatchPlugin testPlugin = new CatchPlugin() {
                @Override
                protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                    throw new InterruptedException("Test interruption");
                }
            };
            
            PluginConfiguration config = mock(PluginConfiguration.class);
            testPlugin.initialize(config);
            
            testPlugin.execute(context);
        
        } catch (InterruptedException expected) {
            org.testng.Assert.assertEquals(expected.getMessage(), "Test interruption");
            org.testng.Assert.assertTrue(Thread.currentThread().isInterrupted(),
                "The interrupt flag must be restored after plugin cleanup");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testCatchPluginValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("catchId");
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testCatchPluginValidateConfigurationWithNull() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testCatchPluginWithIdAttribute() throws PluginException, InterruptedException {
        CatchPlugin newPlugin = new CatchPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("catchId");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        when(context.getVar("error")).thenReturn(null);
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testCatchPluginExecuteMultipleTimes() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        when(context.getVar("error")).thenReturn(null);
        
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
}


