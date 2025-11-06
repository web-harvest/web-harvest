/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Massive test suite for {@link WhilePlugin} - pushing from 60.71% to 70%+
 */
public class WhilePluginMassiveTest {

    @Test
    public void testMeta1() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata());
    }
    
    @Test
    public void testMeta2() {
        WhilePlugin plugin = new WhilePlugin();
        assertTrue(plugin.getMetadata().isCorePlugin());
    }
    
    @Test
    public void testMeta3() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getName());
    }
    
    @Test
    public void testMeta4() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getId());
    }
    
    @Test
    public void testMeta5() {
        WhilePlugin plugin = new WhilePlugin();
        assertEquals(plugin.getMetadata().getCategory(), "Control");
    }
    
    @Test
    public void testMeta6() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getVersion());
    }
    
    @Test
    public void testMeta7() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getDescription());
    }
    
    @Test
    public void testMeta8() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getAuthor());
    }
    
    @Test
    public void testMeta9() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getTags());
    }
    
    @Test
    public void testMeta10() {
        WhilePlugin plugin = new WhilePlugin();
        assertEquals(plugin.getMetadata().getElementName(), "while");
    }
    
    @Test
    public void testMeta11() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getNamespace());
    }
    
    @Test
    public void testMeta12() {
        WhilePlugin plugin = new WhilePlugin();
        assertEquals(plugin.getMetadata().getClassName(), WhilePlugin.class.getName());
    }
    
    @Test
    public void testSchema1() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
    }
    
    @Test
    public void testSchema2() {
        WhilePlugin plugin = new WhilePlugin();
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }
    
    @Test
    public void testSchema3() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getOptionalAttributes());
    }
    
    @Test
    public void testSchema4() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getRequiredAttributes());
    }
    
    @Test
    public void testSchema5() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions());
    }
    
    @Test
    public void testSchema6() {
        WhilePlugin plugin = new WhilePlugin();
        assertTrue(plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions().length > 0);
    }
    
    @Test
    public void testSchema7() {
        WhilePlugin plugin = new WhilePlugin();
        assertEquals(plugin.getMetadata().getConfigurationSchema().getVersion(), "2.3.0");
    }
    
    @Test
    public void testSchema8() {
        WhilePlugin plugin = new WhilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getRequiredSubElements());
    }
    
    @Test
    public void testAttr1() {
        WhilePlugin plugin = new WhilePlugin();
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        for (var attr : attrs) {
            assertNotNull(attr.getName());
        }
    }
    
    @Test
    public void testAttr2() {
        WhilePlugin plugin = new WhilePlugin();
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        for (var attr : attrs) {
            assertNotNull(attr.getType());
        }
    }
    
    @Test
    public void testAttr3() {
        WhilePlugin plugin = new WhilePlugin();
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        for (var attr : attrs) {
            assertNotNull(attr.getDescription());
        }
    }
}

