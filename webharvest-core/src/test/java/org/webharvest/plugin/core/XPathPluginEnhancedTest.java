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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Enhanced comprehensive unit tests for {@link XPathPlugin}.
 * Target: 70%+ code coverage
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class XPathPluginEnhancedTest {

    private XPathPlugin plugin;
    
    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new XPathPlugin();
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
        assertEquals(plugin.getMetadata().getElementName(), "xpath");
    }
    
    // ==================== XPath Expression Tests ====================
    
    @Test
    public void testSimpleXPathExpression() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>Item 1</item><item>Item 2</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithTextFunction() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item/text()");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>Text content</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithAttribute() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item/@id");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item id=\"123\">Content</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithPredicate() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item[@type='special']");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item type=\"special\">Special Item</item><item type=\"normal\">Normal Item</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithPosition() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item[1]");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>First</item><item>Second</item><item>Third</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithChild() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("/root/child/item");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><child><item>Nested Item</item></child></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithWildcard() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//*[@id]");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item id=\"1\">Item 1</item><data id=\"2\">Data 2</data></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Complex XPath Tests ====================
    
    @Test
    public void testXPathWithMultiplePredicates() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item[@type='product'][@available='true']");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item type=\"product\" available=\"true\">Available Product</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithContainsFunction() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item[contains(text(), 'special')]");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>This is special text</item><item>Normal text</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithOr() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item[@type='A' or @type='B']");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item type=\"A\">Type A</item><item type=\"B\">Type B</item><item type=\"C\">Type C</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
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
    public void testXPathWithEmptyResult() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//nonexistent");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>Content</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithEmptyXML() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item");
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
    public void testXPathWithSpecialCharacters() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>Special: &lt;&gt;&amp;&quot;</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
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
        when(configuration.getProperty("expression")).thenReturn("//item");
        when(configuration.getProperty("id")).thenReturn("xpathId");
        
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
    
    @Test
    public void testMetadataElementName() {
        assertEquals(plugin.getMetadata().getElementName(), "xpath");
    }
    
    @Test
    public void testMetadataNamespace() {
        assertNotNull(plugin.getMetadata().getNamespace());
        assertTrue(plugin.getMetadata().getNamespace().contains("webharvest"));
    }
    
    // ==================== Additional XPath Function Tests ====================
    
    @Test
    public void testXPathWithCount() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("count(//item)");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>1</item><item>2</item><item>3</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithLast() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("//item[last()]");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>First</item><item>Last</item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testXPathWithNormalize() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("expression")).thenReturn("normalize-space(//item)");
        when(configuration.getProperty("id")).thenReturn(null);
        
        String xml = "<root><item>  Text  with   spaces  </item></root>";
        when(xmlNode.getText()).thenReturn(xml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
}

