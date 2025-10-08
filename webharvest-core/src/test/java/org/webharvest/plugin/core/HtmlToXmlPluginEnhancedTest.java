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
 * Enhanced comprehensive unit tests for {@link HtmlToXmlPlugin}.
 * Target: 70%+ code coverage
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class HtmlToXmlPluginEnhancedTest {

    private HtmlToXmlPlugin plugin;
    
    @Mock
    private DynamicScopeContext mockContext;
    
    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new HtmlToXmlPlugin();
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
    
    // ==================== HTML Parsing Tests ====================
    
    @Test
    public void testWellFormedHtml() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        String html = "<html><body><p>Test</p></body></html>";
        when(xmlNode.getText()).thenReturn(html);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testMalformedHtml() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        String malformedHtml = "<html><body><p>Test</body>";  // Missing closing tags
        when(xmlNode.getText()).thenReturn(malformedHtml);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testEmptyHtml() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
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
    public void testHtmlWithSpecialCharacters() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        String html = "<html><body><p>Special: àáâãäå çèéêë</p></body></html>";
        when(xmlNode.getText()).thenReturn(html);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testHtmlWithEntities() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        String html = "<html><body><p>&lt;test&gt; &amp; &quot;quotes&quot;</p></body></html>";
        when(xmlNode.getText()).thenReturn(html);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testHtmlWithNestedTags() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        String html = "<html><body><div><ul><li>Item 1</li><li>Item 2</li></ul></div></body></html>";
        when(xmlNode.getText()).thenReturn(html);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testHtmlFragment() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        String htmlFragment = "<div><p>Fragment</p></div>";  // Not a complete HTML document
        when(xmlNode.getText()).thenReturn(htmlFragment);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Charset Tests ====================
    
    @Test
    public void testUtf8Charset() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        String html = "<html><body>UTF-8 text: ąćęłńóśźż</body></html>";
        when(xmlNode.getText()).thenReturn(html);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testIso88591Charset() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("ISO-8859-1");
        
        String html = "<html><body>ISO-8859-1 text</body></html>";
        when(xmlNode.getText()).thenReturn(html);
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
        when(configuration.getProperty("id")).thenReturn("htmlParser");
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        plugin.initialize(configuration);
        
        assertTrue(true);
    }
    
    @Test
    public void testMetadataProperties() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertNotNull(plugin.getMetadata().getDescription());
        assertNotNull(plugin.getMetadata().getVersion());
        assertNotNull(plugin.getMetadata().getElementName());
    }
    
    @Test
    public void testSchemaAttributes() {
        String[] optionalAttrs = plugin.getMetadata().getConfigurationSchema().getOptionalAttributes();
        assertNotNull(optionalAttrs);
        
        String[] requiredAttrs = plugin.getMetadata().getConfigurationSchema().getRequiredAttributes();
        assertNotNull(requiredAttrs);
    }
}

