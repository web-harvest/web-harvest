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
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Enhanced tests for {@link WhilePlugin}.
 */
public class WhilePluginEnhancedTest {

    private WhilePlugin plugin;
    
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
        plugin = new WhilePlugin();
        
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
    public void testMetadataProperties() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertNotNull(plugin.getMetadata().getVersion());
        assertNotNull(plugin.getMetadata().getElementName());
    }
    
    @Test
    public void testSchemaAttributes() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getOptionalAttributes());
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getRequiredAttributes());
    }
    
    @Test
    public void testConfigurationSchema() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
    }
    
    @Test
    public void testMetadataCategory() {
        assertEquals(plugin.getMetadata().getCategory(), "Control");
    }
    
    @Test
    public void testMetadataNamespace() {
        assertNotNull(plugin.getMetadata().getNamespace());
    }
    
    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
    }
    
    @Test
    public void testMetadataClassName() {
        assertEquals(plugin.getMetadata().getClassName(), WhilePlugin.class.getName());
    }
}

