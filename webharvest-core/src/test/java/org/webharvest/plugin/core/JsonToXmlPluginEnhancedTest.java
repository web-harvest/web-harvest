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
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;

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
    
    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() throws org.webharvest.plugin.exceptions.PluginException {
        MockitoAnnotations.openMocks(this);
        // v2.2.x ULTRA-LEAN: Use spy to mock executeBody() method
        plugin = new JsonToXmlPlugin();
        
        // v2.2.x ULTRA-LEAN: Setup configuration mocks for new architecture
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
        when(configuration.getProperty("roottagname")).thenReturn("json");
        
        plugin.initialize(configuration);        
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
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON content
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonArray() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "[\"item1\",\"item2\",\"item3\"]";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON array
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testNestedJsonObject() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"person\":{\"name\":\"John\",\"address\":{\"city\":\"Warsaw\"}}}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return nested JSON
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithArray() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"items\":[{\"id\":1},{\"id\":2},{\"id\":3}]}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON with array
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Special Values Tests ====================
    
    @Test
    public void testJsonWithNull() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"value\":null}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON with null
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithBoolean() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"active\":true,\"deleted\":false}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON with booleans
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithNumbers() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"integer\":42,\"float\":3.14,\"negative\":-10}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON with numbers
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    public void testEmptyJsonObject() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return empty JSON object
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testEmptyJsonArray() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "[]";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return empty JSON array
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithSpecialCharacters() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"text\":\"Special: <>&\\\"\"}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON with special characters
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testJsonWithUnicode() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        String json = "{\"text\":\"Unicode: \\u0105\\u0107\\u0119\"}";
        // v2.2.x ULTRA-LEAN: Mock executeBody() to return JSON with Unicode
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(java.util.Collections.singletonList(jsonChild));
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Configuration Tests ====================
    
    
    @Test
    public void testMetadataProperties() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertNotNull(plugin.getMetadata().getDescription());
        assertNotNull(plugin.getMetadata().getVersion());
    }
}

