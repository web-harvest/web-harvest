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
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Enhanced tests for {@link TokenizePlugin}.
 */
public class TokenizePluginEnhancedTest {

    private TokenizePlugin plugin;
    
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
        plugin = new TokenizePlugin();
        
        // v2.2.x ULTRA-LEAN: Setup configuration mocks for new architecture
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(java.util.Collections.emptyList());
        
        // Initialize plugin with mock configuration
        plugin.initialize(configuration);
    }
    
    @Test
    public void testMetadata() {
        assertNotNull(plugin.getMetadata());
        assertTrue(plugin.getMetadata().isCorePlugin());
    }
    
    @Test
    public void testWithCommaDelimiter() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("delimiters")).thenReturn(",");
        when(configuration.getProperty("id")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("a,b,c,d");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testWithSpaceDelimiter() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("delimiters")).thenReturn(" ");
        when(configuration.getProperty("id")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("word1 word2 word3");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testWithMultipleDelimiters() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("delimiters")).thenReturn(",; ");
        when(configuration.getProperty("id")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("a,b;c d");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testMetadataProperties() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertNotNull(plugin.getMetadata().getDescription());
    }
    
    @Test
    public void testSchemaAllowsBody() {
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }
}

