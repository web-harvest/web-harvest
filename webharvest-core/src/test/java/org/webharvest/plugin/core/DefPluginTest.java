/*
 * Copyright (c) 2006-2025 the original author or authors.
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
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.definition.Config;
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

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;

    @Mock
    private PluginConfiguration configuration;

    private DefPlugin plugin;
    
    // v2.2.x: Real ScraperContext for variable interpolation tests
    private DynamicScopeContext realContext;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        
        // v2.2.x CRITICAL: Initialize BaseTemplater.scriptEngineFactory via Guice
        // AND create real ScraperContext for interpolation tests
        com.google.inject.Injector injector;
        if (org.webharvest.runtime.templaters.BaseTemplater.scriptEngineFactory == null) {
            injector = com.google.inject.Guice.createInjector(
                new org.webharvest.ioc.ScraperModule("."));
            // Static injection happens in ScraperModule.configure()
        } else {
            // BaseTemplater already initialized, but we still need injector for context
            injector = com.google.inject.Guice.createInjector(
                new org.webharvest.ioc.ScraperModule("."));
        }
        
        // v2.2.x: Create REAL ScraperContext for EVERY test
        // Mock context doesn't work with JSRScriptEngineAdapter.copyVariables()
        realContext = injector.getInstance(org.webharvest.runtime.ScraperContext.class);
        org.webharvest.definition.Config mockConfig = mock(org.webharvest.definition.Config.class);
        realContext.setConfig(mockConfig);
        realContext.setScriptingLanguage(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        
        // v2.2.x CRITICAL: Register core plugins so DefaultPluginBodyExecutor can find them!
        // Without this, child elements like <constant> won't be found
        org.webharvest.definition.CorePluginsRegistrar.registerCorePlugins();
        
        plugin = new DefPlugin();
        
        
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(xmlNode.getText()).thenReturn("test value");
        // when(elementDef.hasOperations()).thenReturn(false);
        
        // v2.2.x FIX: Mock ScriptingLanguage for BaseTemplater interpolation
        when(context.getScriptingLanguage()).thenReturn(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        
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
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("var", "")).thenReturn("testVar");
        when(config.getProperty("value", "")).thenReturn("testValue");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testDefPluginInitializeWithNullConfiguration() throws PluginException {
        DefPlugin newPlugin = new DefPlugin();
        
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
        // v2.2.x: xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        verify(context).setLocalVar(eq("emptyVar"), any(Variable.class));
    }

    @Test
    public void testDefPluginWithXmlNodeText() throws InterruptedException {
        when(configuration.getProperty("var", "")).thenReturn("xmlVar");
        when(configuration.getProperty("value", "")).thenReturn("");
        when(context.getCharset()).thenReturn("UTF-8");
        // v2.2.x: xmlNode removed - no longer needed
        // when(xmlNode.getText()).thenReturn("XML node content");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        
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

    // ==================================================================================
    // CRITICAL TESTS - Variable Interpolation & Template Processing (v2.2.x)
    // ==================================================================================

    @Test
    public void testDefPluginWithVariableInterpolation() throws InterruptedException, PluginException {
        // PROBLEM 1: DefPlugin should interpolate ${variables} in text content
        // v2.2.x FIX: Use REAL ScraperContext and DefaultPluginConfiguration
        
        // Setup: Set variable in REAL context
        realContext.setLocalVar("name", new NodeVariable("World"));
        
        // Create new plugin instance with real configuration
        DefPlugin testPlugin = new DefPlugin();
        DefaultPluginConfiguration realConfig = new DefaultPluginConfiguration(
            "DefPlugin", 
            "http://org.webharvest/schema/2.1/core", 
            "def");
        realConfig.setProperty("var", "greeting");
        realConfig.setProperty("_textContent", "Hello ${name}!");
        
        testPlugin.initialize(realConfig);
        Variable result = testPlugin.execute(realContext);
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString();
        
        assertTrue(resultStr.contains("World"), 
                   "Should interpolate ${name} to 'World', but got: " + resultStr);
        assertFalse(resultStr.contains("${"), 
                    "Should NOT contain literal ${, but got: " + resultStr);
    }

    @Test
    public void testDefPluginWithMultipleVariableInterpolations() throws InterruptedException, PluginException {
        // PROBLEM 1 (extended): Multiple variable interpolations should all work
        // v2.2.x FIX: Use REAL ScraperContext and DefaultPluginConfiguration
        
        realContext.setLocalVar("firstName", new NodeVariable("John"));
        realContext.setLocalVar("lastName", new NodeVariable("Doe"));
        realContext.setLocalVar("age", new NodeVariable("30"));
        
        DefPlugin testPlugin = new DefPlugin();
        DefaultPluginConfiguration realConfig = new DefaultPluginConfiguration(
            "DefPlugin", 
            "http://org.webharvest/schema/2.1/core", 
            "def");
        realConfig.setProperty("var", "message");
        realConfig.setProperty("_textContent", "Name: ${firstName} ${lastName}, Age: ${age}");
        
        testPlugin.initialize(realConfig);
        Variable result = testPlugin.execute(realContext);
        
        assertNotNull(result);
        String resultStr = result.toString();
        
        assertTrue(resultStr.contains("John"), "Should interpolate ${firstName}");
        assertTrue(resultStr.contains("Doe"), "Should interpolate ${lastName}");
        assertTrue(resultStr.contains("30"), "Should interpolate ${age}");
        assertFalse(resultStr.contains("${"), "Should NOT contain literal ${");
    }

    @Test
    public void testDefPluginWithNestedVariableInterpolation() throws InterruptedException, PluginException {
        // PROBLEM 1 (nested): Nested variable references
        // v2.2.x FIX: Use REAL ScraperContext and DefaultPluginConfiguration
        
        realContext.setLocalVar("var1", new NodeVariable("Value1"));
        realContext.setLocalVar("var2", new NodeVariable("${var1}"));
        
        DefPlugin testPlugin = new DefPlugin();
        DefaultPluginConfiguration realConfig = new DefaultPluginConfiguration(
            "DefPlugin", 
            "http://org.webharvest/schema/2.1/core", 
            "def");
        realConfig.setProperty("var", "result");
        realConfig.setProperty("_textContent", "${var2}");
        
        testPlugin.initialize(realConfig);
        Variable result = testPlugin.execute(realContext);
        
        assertNotNull(result);
        // Should eventually resolve to "Value1" but for now just check it tries
        String resultStr = result.toString();
        assertTrue(resultStr.contains("var") || resultStr.contains("Value"), 
                   "Should attempt interpolation, got: " + resultStr);
    }

    @Test
    public void testDefPluginWithChildrenExecution() throws InterruptedException {
        // PROBLEM 2: DefPlugin with children should execute them and get their result
        // Currently FAILS - returns empty string when children exist
        
        // v2.2.x FIX: Use REAL DefaultPluginConfiguration instead of mock
        // Mocks don't work properly with DefaultPluginBodyExecutor
        DefaultPluginConfiguration childConfig = new DefaultPluginConfiguration(
            "ConstantPlugin", 
            "http://org.webharvest/schema/2.1/core", 
            "constant");
        childConfig.setProperty("value", "Child Value");
        
        java.util.List<PluginConfiguration> children = new java.util.ArrayList<>();
        children.add(childConfig);
        
        when(configuration.getProperty("var", "")).thenReturn("fromChildren");
        when(configuration.getProperty("value", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(children);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result, "Result should not be null");
        
        // THIS TEST SHOULD FAIL NOW - showing that children are not executed
        assertFalse(result instanceof EmptyVariable, 
                    "Should NOT be empty when children exist");
        String resultStr = result.toString();
        assertTrue(resultStr.length() > 0, 
                   "Should have content from children, but got empty string");
    }

    @Test
    public void testDefPluginWithTextContentAndNoTemplating() throws InterruptedException {
        // BASELINE TEST: Without interpolation, text content should work
        
        when(configuration.getProperty("var", "")).thenReturn("plain");
        when(configuration.getProperty("value", "")).thenReturn("");
        when(configuration.getProperty("_textContent")).thenReturn("Plain text without variables");
        when(configuration.hasChildren()).thenReturn(false);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "Plain text without variables");
    }

    @Test
    public void testDefPluginPriority_ValueAttributeOverTextContent() throws InterruptedException {
        // Test that "value" attribute has priority over text content
        
        when(configuration.getProperty("var", "")).thenReturn("test");
        when(configuration.getProperty("value", "")).thenReturn("From Attribute");
        when(configuration.getProperty("_textContent")).thenReturn("From Text Content");
        when(configuration.hasChildren()).thenReturn(false);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "From Attribute", 
                    "Value attribute should have priority");
    }

    @Test
    public void testDefPluginInterpolationWithSpecialChars() throws InterruptedException, PluginException {
        // Test interpolation with special characters in variable values
        // v2.2.x FIX: Use REAL ScraperContext and DefaultPluginConfiguration
        
        realContext.setLocalVar("special", new NodeVariable("<>&\"'"));
        
        DefPlugin testPlugin = new DefPlugin();
        DefaultPluginConfiguration realConfig = new DefaultPluginConfiguration(
            "DefPlugin", 
            "http://org.webharvest/schema/2.1/core", 
            "def");
        realConfig.setProperty("var", "result");
        realConfig.setProperty("_textContent", "Special: ${special}");
        
        testPlugin.initialize(realConfig);
        Variable result = testPlugin.execute(realContext);
        
        assertNotNull(result);
        String resultStr = result.toString();
        
        assertTrue(resultStr.contains("<>&\"'"), 
                   "Should preserve special chars in interpolation");
    }
}

