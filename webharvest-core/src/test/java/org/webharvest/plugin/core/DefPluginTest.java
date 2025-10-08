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
import org.webharvest.plugin.PluginConfigurationSchema;
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
 * Unit tests for DefPlugin.
 */
public class DefPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;

    private DefPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new DefPlugin();
        
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
        assertEquals(metadata.getId(), "def");
        assertEquals(metadata.getName(), "def");
        assertEquals(metadata.getDescription(), "Defines variables with their values");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "def");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertTrue(schema.allowsBody());
    }

    @Test
    public void testDefPluginInitialize() throws PluginException {
        DefPlugin newPlugin = new DefPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("testVar");
        when(config.getProperty("value", "")).thenReturn("testValue");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testDefPluginInitializeWithNullConfiguration() throws PluginException {
        DefPlugin newPlugin = new DefPlugin();
        newPlugin.setElementDef(elementDef);
        
        newPlugin.initialize(null);
    }

    @Test
    public void testDefPluginExecuteWithValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(configuration.getProperty("value", "")).thenReturn("myValue");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "myValue");
        verify(context).setLocalVar("myVar", result);
    }

    @Test
    public void testDefPluginExecuteWithoutValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(configuration.getProperty("value", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("myVar"), any(Variable.class));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDefPluginExecuteWithEmptyVar() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        
        plugin.execute(context);
    }

    @Test
    public void testDefPluginValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("validVar");
        
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testDefPluginValidateConfigurationWithEmptyVar() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("");
        
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testDefPluginValidateConfigurationWithNullConfig() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testDefPluginWithDifferentVarNames() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("var123");
        when(configuration.getProperty("value", "")).thenReturn("value123");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar("var123", result);
    }

    @Test
    public void testDefPluginWithSpecialCharactersInValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("myVar");
        when(configuration.getProperty("value", "")).thenReturn("special<>&\"'chars");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "special<>&\"'chars");
    }

    @Test
    public void testDefPluginWithUnicodeValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("unicodeVar");
        when(configuration.getProperty("value", "")).thenReturn("Unicode: 中文 العربية русский");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result.toString().contains("中文"));
    }

    @Test
    public void testDefPluginWithLongValue() throws InterruptedException {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("Line ").append(i).append("\n");
        }
        
        when(configuration.getProperty("var", "")).thenReturn("longVar");
        when(configuration.getProperty("value", "")).thenReturn(longValue.toString());
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result.toString().length() > 1000);
    }

    @Test
    public void testDefPluginMultipleExecutions() throws InterruptedException {
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
    public void testDefPluginWithEmptyValue() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("emptyVar");
        when(configuration.getProperty("value", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn("");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("emptyVar"), any(Variable.class));
    }

    @Test
    public void testDefPluginWithXmlNodeText() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("xmlVar");
        when(configuration.getProperty("value", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        when(xmlNode.getText()).thenReturn("XML node content");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("xmlVar"), any(Variable.class));
    }

    @Test
    public void testDefPluginWithHyphenatedVarName() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("my-var-name");
        when(configuration.getProperty("value", "")).thenReturn("test");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar("my-var-name", result);
    }

    @Test
    public void testDefPluginWithNumericVar() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("var123");
        when(configuration.getProperty("value", "")).thenReturn("numeric_value");
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("var123"), any(Variable.class));
    }

    @Test
    public void testDefPluginExecuteLoop() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("loopVar");
        when(configuration.getProperty("value", "")).thenReturn("iteration");
        when(context.getCharset()).thenReturn("UTF-8");
        
        for (int i = 0; i < 10; i++) {
            Variable result = plugin.execute(context);
            assertNotNull(result);
        }
        
        verify(context, times(10)).setLocalVar(eq("loopVar"), any(Variable.class));
    }
}

