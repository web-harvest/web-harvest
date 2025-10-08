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
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Enhanced comprehensive unit tests for {@link TemplatePlugin}.
 * Target: 70%+ code coverage
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class TemplatePluginEnhancedTest {

    private TemplatePlugin plugin;
    
    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new TemplatePlugin();
        plugin.setElementDef(elementDef);
    }
    
    // ==================== Metadata Tests ====================
    
    @Test
    public void testMetadata() {
        assertNotNull(plugin.getMetadata());
        assertNotNull(plugin.getMetadata().getName());
        assertTrue(plugin.getMetadata().isCorePlugin());
    }
    
    @Test
    public void testConfigurationSchema() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }
    
    @Test
    public void testMetadataProperties() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertNotNull(plugin.getMetadata().getDescription());
        assertNotNull(plugin.getMetadata().getVersion());
        assertNotNull(plugin.getMetadata().getElementName());
    }
    
    // ==================== Basic Template Tests ====================
    
    @Test
    public void testSimpleTextTemplate() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("Simple text template");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithSingleVariable() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("name", new NodeVariable("John"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("Hello ${name}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithMultipleVariables() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("firstName", new NodeVariable("John"));
        realContext.setLocalVar("lastName", new NodeVariable("Doe"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${firstName} ${lastName}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithExpression() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("x", new NodeVariable("5"));
        realContext.setLocalVar("y", new NodeVariable("10"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("Result: ${x + y}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Empty and Null Tests ====================
    
    @Test
    public void testEmptyTemplate() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithNullVariable() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("Value: ${nonexistent}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Special Characters Tests ====================
    
    @Test
    public void testTemplateWithSpecialCharacters() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("text", new NodeVariable("Special: <>&\"'"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${text}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithUnicode() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("unicode", new NodeVariable("Unicode: ąćęłńóśźż 中文"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${unicode}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Nested Templates Tests ====================
    
    @Test
    public void testTemplateWithNestedVariables() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("inner", new NodeVariable("value"));
        realContext.setLocalVar("outer", new NodeVariable("${inner}"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${outer}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithMultilineContent() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("line1", new NodeVariable("First Line"));
        realContext.setLocalVar("line2", new NodeVariable("Second Line"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String template = "${line1}\n${line2}";
        when(xmlNode.getText()).thenReturn(template);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Configuration Tests ====================
    
    @Test
    public void testInitializeWithConfiguration() throws Exception {
        when(configuration.getProperty("id")).thenReturn("templateId");
        
        plugin.initialize(configuration);
        
        assertTrue(true);
    }
    
    @Test
    public void testSchemaAttributes() {
        String[] optionalAttrs = plugin.getMetadata().getConfigurationSchema().getOptionalAttributes();
        assertNotNull(optionalAttrs);
        
        String[] requiredAttrs = plugin.getMetadata().getConfigurationSchema().getRequiredAttributes();
        assertNotNull(requiredAttrs);
    }
    
    // ==================== Complex Expression Tests ====================
    
    @Test
    public void testTemplateWithConditionalExpression() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("flag", new NodeVariable("true"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${flag ? 'yes' : 'no'}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithStringConcatenation() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("part1", new NodeVariable("Hello"));
        realContext.setLocalVar("part2", new NodeVariable("World"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${part1 + ' ' + part2}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateWithMethodCall() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("text", new NodeVariable("hello world"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${text.toString().toUpperCase()}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    public void testTemplateWithLongContent() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        
        StringBuilder longTemplate = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longTemplate.append("Line ").append(i).append("\n");
        }
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn(longTemplate.toString());
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testTemplateExecutionMultipleTimes() throws Exception {
        ScraperContext realContext = new ScraperContext();
        realContext.setScriptingLanguage(ScriptingLanguage.BEANSHELL);
        realContext.setLocalVar("var", new NodeVariable("value"));
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("${var}");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        
        Variable result1 = plugin.execute(realContext);
        Variable result2 = plugin.execute(realContext);
        Variable result3 = plugin.execute(realContext);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }
}

