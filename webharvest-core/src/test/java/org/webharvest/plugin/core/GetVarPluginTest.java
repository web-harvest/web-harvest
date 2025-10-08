/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginDef;
import org.webharvest.definition.IElementDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;
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
 * Unit tests for GetVarPlugin.
 */
public class GetVarPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;

    private GetVarPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new GetVarPlugin();
        
        plugin.setElementDef(elementDef);
        
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "get");
        assertEquals(metadata.getName(), "get");
        assertEquals(metadata.getDescription(), "Retrieves a variable value from the context");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "get");
    }

    @Test
    public void testGetVarPluginInitialize() throws PluginException {
        GetVarPlugin newPlugin = new GetVarPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("testVar");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test
    public void testGetVarPluginExecute() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("myValue"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "myValue");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetVarPluginExecuteWithEmptyVar() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetVarPluginExecuteWithNullVariable() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("nonExistentVar");
        when(context.getVar("nonExistentVar")).thenReturn(null);
        when(context.getCharset()).thenReturn("UTF-8");
        
        plugin.execute(context);
    }

    @Test
    public void testGetVarPluginValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("validVar");
        
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testGetVarPluginValidateConfigurationWithEmptyVar() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("");
        
        plugin.validateConfiguration(config);
    }

    @Test
    public void testGetVarPluginWithDifferentVarNames() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("var123");
        when(context.getVar("var123")).thenReturn(new NodeVariable("value123"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "value123");
    }

    @Test
    public void testGetVarPluginWithUnicodeValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("unicodeVar");
        when(context.getVar("unicodeVar")).thenReturn(new NodeVariable("中文 العربية русский"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result.toString().contains("中文"));
    }

    @Test
    public void testGetVarPluginWithEmptyVariable() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("emptyVar");
        when(context.getVar("emptyVar")).thenReturn(EmptyVariable.INSTANCE);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testGetVarPluginMultipleExecutions() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("value"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(result1.toString(), "value");
        assertEquals(result2.toString(), "value");
        assertEquals(result3.toString(), "value");
    }

    @Test
    public void testGetVarPluginWithHyphenatedVarName() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("my-var-name");
        when(context.getVar("my-var-name")).thenReturn(new NodeVariable("test-value"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "test-value");
    }

    @Test
    public void testGetVarPluginWithNumericVarName() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("var123");
        when(context.getVar("var123")).thenReturn(new NodeVariable("123"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "123");
    }

    @Test
    public void testGetVarPluginWithSpecialCharsInValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("specialVar");
        when(context.getVar("specialVar")).thenReturn(new NodeVariable("<>&\"'"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "<>&\"'");
    }

    @Test
    public void testGetVarPluginRepeatedCalls() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("repeatedVar");
        when(context.getVar("repeatedVar")).thenReturn(new NodeVariable("repeatedValue"));
        when(context.getCharset()).thenReturn("UTF-8");
        
        for (int i = 0; i < 10; i++) {
            Variable result = plugin.execute(context);
            assertNotNull(result);
            assertEquals(result.toString(), "repeatedValue");
        }
    }

    @Test
    public void testGetVarPluginWithLongValue() throws InterruptedException {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("Line ").append(i).append(" ");
        }
        
        when(configuration.getProperty("var", "")).thenReturn("longVar");
        when(context.getVar("longVar")).thenReturn(new NodeVariable(longValue.toString()));
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result.toString().length() > 1000);
    }
}

