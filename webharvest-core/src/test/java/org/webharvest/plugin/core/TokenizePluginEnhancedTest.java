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
    
    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new TokenizePlugin();
        plugin.setElementDef(elementDef);
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
        
        when(xmlNode.getText()).thenReturn("a,b,c,d");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testWithSpaceDelimiter() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("delimiters")).thenReturn(" ");
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("word1 word2 word3");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testWithMultipleDelimiters() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("delimiters")).thenReturn(",; ");
        when(configuration.getProperty("id")).thenReturn(null);
        
        when(xmlNode.getText()).thenReturn("a,b;c d");
        when(xmlNode.getElementList()).thenReturn(new ArrayList());
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        plugin.initialize(configuration);
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

