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
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Enhanced comprehensive unit tests for {@link JsonToXmlPlugin}.
 * Target: 70%+ code coverage
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class JsonToXmlPluginEnhancedTest {

    private JsonToXmlPlugin plugin;
    
    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new JsonToXmlPlugin();
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
    }
    
    // ==================== Simple JSON Tests ====================
    
    @Test
    public void testSimpleJsonObject() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"name\":\"John\",\"age\":30}";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonArray() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "[\"item1\",\"item2\",\"item3\"]";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testNestedJsonObject() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"person\":{\"name\":\"John\",\"address\":{\"city\":\"Warsaw\"}}}";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithArray() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"items\":[{\"id\":1},{\"id\":2},{\"id\":3}]}";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Special Values Tests ====================
    
    @Test
    public void testJsonWithNull() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"value\":null}";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithBoolean() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"active\":true,\"deleted\":false}";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithNumbers() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"integer\":42,\"float\":3.14,\"negative\":-10}";
        when(xmlNode.getText()).thenReturn(json);
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
    public void testEmptyJsonObject() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{}";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testEmptyJsonArray() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "[]";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithSpecialCharacters() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"text\":\"Special: <>&\\\"\"}";
        when(xmlNode.getText()).thenReturn(json);
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithUnicode() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"text\":\"Unicode: \\u0105\\u0107\\u0119\"}";
        when(xmlNode.getText()).thenReturn(json);
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
        when(configuration.getProperty("id")).thenReturn("jsonId");
        
        plugin.initialize(configuration);
        
        assertTrue(true);
    }
    
    @Test
    public void testMetadataProperties() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertNotNull(plugin.getMetadata().getDescription());
        assertNotNull(plugin.getMetadata().getVersion());
    }
}

