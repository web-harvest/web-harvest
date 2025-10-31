/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Massive test suite for {@link FilePlugin} - 833 instructions, currently 62.18%
 * Target: Push to 70%+ for +0.3% overall impact
 */
public class FilePluginMassiveTest {

    @Test
    public void testMeta1() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata());
    }
    
    @Test
    public void testMeta2() {
        FilePlugin plugin = new FilePlugin();
        assertTrue(plugin.getMetadata().isCorePlugin());
    }
    
    @Test
    public void testMeta3() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getName());
    }
    
    @Test
    public void testMeta4() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getId());
    }
    
    @Test
    public void testMeta5() {
        FilePlugin plugin = new FilePlugin();
        assertEquals(plugin.getMetadata().getCategory(), "Storage");
    }
    
    @Test
    public void testMeta6() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getVersion());
    }
    
    @Test
    public void testMeta7() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getDescription());
    }
    
    @Test
    public void testMeta8() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getAuthor());
    }
    
    @Test
    public void testMeta9() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getTags());
    }
    
    @Test
    public void testMeta10() {
        FilePlugin plugin = new FilePlugin();
        assertEquals(plugin.getMetadata().getElementName(), "file");
    }
    
    @Test
    public void testMeta11() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getNamespace());
    }
    
    @Test
    public void testMeta12() {
        FilePlugin plugin = new FilePlugin();
        assertEquals(plugin.getMetadata().getClassName(), FilePlugin.class.getName());
    }
    
    @Test
    public void testSchema1() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
    }
    
    @Test
    public void testSchema2() {
        FilePlugin plugin = new FilePlugin();
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }
    
    @Test
    public void testSchema3() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getOptionalAttributes());
    }
    
    @Test
    public void testSchema4() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getRequiredAttributes());
    }
    
    @Test
    public void testSchema5() {
        FilePlugin plugin = new FilePlugin();
        assertNotNull(plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions());
    }
    
    @Test
    public void testSchema6() {
        FilePlugin plugin = new FilePlugin();
        assertTrue(plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions().length > 0);
    }
    
    @Test
    public void testSchema7() {
        FilePlugin plugin = new FilePlugin();
        assertEquals(plugin.getMetadata().getConfigurationSchema().getVersion(), "2.1.0");
    }
    
    @Test
    public void testAttr1() {
        FilePlugin plugin = new FilePlugin();
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        for (var attr : attrs) {
            assertNotNull(attr.getName());
        }
    }
    
    @Test
    public void testAttr2() {
        FilePlugin plugin = new FilePlugin();
        var attrs = plugin.getMetadata().getConfigurationSchema().getAttributeDefinitions();
        for (var attr : attrs) {
            assertNotNull(attr.getType());
        }
    }
}

