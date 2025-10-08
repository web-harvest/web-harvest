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

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for SetVarPlugin.
 */
public class SetVarPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private SetVarPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new SetVarPlugin();
        
        plugin.setElementDef(elementDef);
        
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(xmlNode.getText()).thenReturn("test value");
        when(elementDef.hasOperations()).thenReturn(false);
        
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(configuration.getProperty("value", "")).thenReturn("");
        
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertNotNull(metadata.getId());
        assertNotNull(metadata.getName());
        assertTrue(metadata.isCorePlugin());
        assertNotNull(metadata.getElementName());
    }

    @Test
    public void testSetVarPluginInitialize() throws PluginException {
        SetVarPlugin newPlugin = new SetVarPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("testVar");
        when(config.getProperty("value", "")).thenReturn("testValue");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testSetVarPluginInitializeWithNullConfiguration() throws PluginException {
        SetVarPlugin newPlugin = new SetVarPlugin();
        newPlugin.setElementDef(elementDef);
        
        newPlugin.initialize(null);
    }

    @Test
    public void testSetVarPluginExecuteWithValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(configuration.getProperty("value", "")).thenReturn("myValue");
        when(context.getCharset()).thenReturn("UTF-8");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("myValue"));
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("myVar"), any(Variable.class));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testSetVarPluginExecuteWithEmptyVar() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        
        plugin.execute(context);
    }

    @Test
    public void testSetVarPluginWithDifferentVarNames() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("var123");
        when(configuration.getProperty("value", "")).thenReturn("value123");
        when(context.getCharset()).thenReturn("UTF-8");
        when(context.getVar("var123")).thenReturn(new NodeVariable("value123"));
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("var123"), any(Variable.class));
    }

    @Test
    public void testSetVarPluginWithSpecialCharactersInValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(configuration.getProperty("value", "")).thenReturn("special<>&\"'chars");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("myVar"), any(Variable.class));
    }

    @Test
    public void testSetVarPluginWithUnicodeValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("unicodeVar");
        when(configuration.getProperty("value", "")).thenReturn("中文 العربية русский");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("unicodeVar"), any(Variable.class));
    }

    @Test
    public void testSetVarPluginMultipleExecutions() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(configuration.getProperty("value", "")).thenReturn("value1");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        verify(context, times(3)).setLocalVar(eq("myVar"), any(Variable.class));
    }

    @Test
    public void testSetVarPluginWithHyphenatedVarName() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("my-var-name");
        when(configuration.getProperty("value", "")).thenReturn("test");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("my-var-name"), any(Variable.class));
    }

    @Test
    public void testSetVarPluginWithNumericValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("numVar");
        when(configuration.getProperty("value", "")).thenReturn("12345");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("numVar"), any(Variable.class));
    }

    @Test
    public void testSetVarPluginWithEmptyValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("emptyVar");
        when(configuration.getProperty("value", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn("");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("emptyVar"), any(Variable.class));
    }
}

