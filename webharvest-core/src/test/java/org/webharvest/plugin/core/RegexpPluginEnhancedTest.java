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
 * Enhanced tests for {@link RegexpPlugin}.
 */
public class RegexpPluginEnhancedTest {

    private RegexpPlugin plugin;
    
    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new RegexpPlugin();
        plugin.setElementDef(elementDef);
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
        assertNotNull(plugin.getMetadata().getDescription());
    }
    
    @Test
    public void testSchemaAttributes() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getOptionalAttributes());
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getRequiredAttributes());
    }
    
    @Test
    public void testSchemaAllowsBody() {
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }
    
    @Test
    public void testConfigurationSchema() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
    }
    
    @Test
    public void testMetadataCategory() {
        assertNotNull(plugin.getMetadata().getCategory());
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
        assertEquals(plugin.getMetadata().getClassName(), RegexpPlugin.class.getName());
    }
    
    @Test
    public void testMetadataId() {
        assertNotNull(plugin.getMetadata().getId());
    }
}

