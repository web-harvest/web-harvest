/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Test for PluginConfiguration children support (ULTRA-LEAN v2.2.x).
 * 
 * PURPOSE: Verify that PluginConfiguration can store and retrieve children,
 * replacing old IElementDef.getOperationDefs() functionality.
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class PluginConfigurationChildrenTest {

    @Test(groups = {"unit", "plugin", "ultra-lean"})
    public void testConfiguration_NoChildren() {
        PluginConfiguration config = new DefaultPluginConfiguration(
            "test-1", "http://test", "test");
        
        assertFalse(config.hasChildren(), "New config should have no children");
        assertNotNull(config.getChildren(), "getChildren() should never return null");
        assertEquals(config.getChildren().size(), 0, "Should have 0 children");
    }

    @Test(groups = {"unit", "plugin", "ultra-lean"})
    public void testConfiguration_WithChildren() {
        PluginConfiguration parent = new DefaultPluginConfiguration(
            "parent-1", "http://test", "parent");
        
        PluginConfiguration child1 = new DefaultPluginConfiguration(
            "child-1", "http://test", "child1");
        PluginConfiguration child2 = new DefaultPluginConfiguration(
            "child-2", "http://test", "child2");
        
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        
        parent.setChildren(children);
        
        assertTrue(parent.hasChildren(), "Parent should have children");
        assertEquals(parent.getChildren().size(), 2, "Should have 2 children");
        assertEquals(parent.getChildren().get(0).getPluginId(), "child-1");
        assertEquals(parent.getChildren().get(1).getPluginId(), "child-2");
    }

    @Test(groups = {"unit", "plugin", "ultra-lean"})
    public void testConfiguration_ImmutableChildren() {
        PluginConfiguration config = new DefaultPluginConfiguration(
            "test-1", "http://test", "test");
        
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration("child-1", "http://test", "child"));
        
        config.setChildren(children);
        
        // Get children returns immutable list
        List<PluginConfiguration> retrieved = config.getChildren();
        
        try {
            retrieved.add(new DefaultPluginConfiguration("child-2", "http://test", "child2"));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected - list is immutable
        }
        
        // Original list modifications don't affect config
        children.add(new DefaultPluginConfiguration("child-3", "http://test", "child3"));
        assertEquals(config.getChildren().size(), 1, "Config should still have 1 child");
    }

    @Test(groups = {"unit", "plugin", "ultra-lean"})
    public void testConfiguration_SetChildrenNull() {
        PluginConfiguration config = new DefaultPluginConfiguration(
            "test-1", "http://test", "test");
        
        config.setChildren(null);
        
        assertFalse(config.hasChildren(), "Should have no children when set to null");
        assertNotNull(config.getChildren(), "getChildren() should return empty list, not null");
        assertEquals(config.getChildren().size(), 0);
    }

    @Test(groups = {"unit", "plugin", "ultra-lean"})
    public void testConfiguration_NestedChildren() {
        // Test deep nesting: parent -> child -> grandchild
        PluginConfiguration grandchild = new DefaultPluginConfiguration(
            "grandchild-1", "http://test", "grandchild");
        
        PluginConfiguration child = new DefaultPluginConfiguration(
            "child-1", "http://test", "child");
        List<PluginConfiguration> grandchildren = new ArrayList<>();
        grandchildren.add(grandchild);
        child.setChildren(grandchildren);
        
        PluginConfiguration parent = new DefaultPluginConfiguration(
            "parent-1", "http://test", "parent");
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(child);
        parent.setChildren(children);
        
        assertTrue(parent.hasChildren());
        assertTrue(parent.getChildren().get(0).hasChildren());
        assertEquals(parent.getChildren().get(0).getChildren().get(0).getPluginId(), "grandchild-1");
    }

    @Test(groups = {"unit", "plugin", "ultra-lean"})
    public void testConfiguration_ToString_IncludesChildren() {
        PluginConfiguration config = new DefaultPluginConfiguration(
            "test-1", "http://test", "test");
        
        String str1 = config.toString();
        assertTrue(str1.contains("children=0"), "toString should show children count");
        
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration("child-1", "http://test", "child"));
        children.add(new DefaultPluginConfiguration("child-2", "http://test", "child"));
        config.setChildren(children);
        
        String str2 = config.toString();
        assertTrue(str2.contains("children=2"), "toString should show updated children count");
    }
}


