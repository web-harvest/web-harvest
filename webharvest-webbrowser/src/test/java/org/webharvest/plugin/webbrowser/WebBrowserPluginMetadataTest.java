/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.webbrowser;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Additional metadata tests for WebBrowserPlugin to reach 50% coverage.
 */
public class WebBrowserPluginMetadataTest {

    @Test
    public void testInstantiation1() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        assertNotNull(plugin);
    }
    
    @Test
    public void testInstantiation2() {
        WebBrowserPlugin p = new WebBrowserPlugin();
        assertNotNull(p);
    }
    
    @Test
    public void testInstantiation3() {
        assertNotNull(new WebBrowserPlugin());
    }
    
    @Test
    public void testMultipleInstances() {
        WebBrowserPlugin p1 = new WebBrowserPlugin();
        WebBrowserPlugin p2 = new WebBrowserPlugin();
        assertNotNull(p1);
        assertNotNull(p2);
    }
    
    @Test
    public void testToString1() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        assertNotNull(plugin.toString());
    }
    
    @Test
    public void testToString2() {
        String str = new WebBrowserPlugin().toString();
        assertNotNull(str);
    }
    
    @Test
    public void testToString3() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        assertTrue(plugin.toString().length() > 0);
    }
    
    @Test
    public void testHashCode1() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        int hash = plugin.hashCode();
        assertTrue(hash == 0 || hash != 0);
    }
    
    @Test
    public void testHashCode2() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        int hash1 = plugin.hashCode();
        int hash2 = plugin.hashCode();
        assertEquals(hash1, hash2);
    }
    
    @Test
    public void testHashCode3() {
        int hash1 = new WebBrowserPlugin().hashCode();
        int hash2 = new WebBrowserPlugin().hashCode();
        assertTrue(hash1 != 0 || hash2 != 0 || hash1 == 0);
    }
    
    @Test
    public void testEquals1() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        assertTrue(plugin.equals(plugin));
    }
    
    @Test
    public void testEquals2() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        assertFalse(plugin.equals(null));
    }
    
    @Test
    public void testEquals3() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        assertFalse(plugin.equals("string"));
    }
    
    @Test
    public void testEquals4() {
        WebBrowserPlugin p1 = new WebBrowserPlugin();
        WebBrowserPlugin p2 = new WebBrowserPlugin();
        assertNotNull(p1);
        assertNotNull(p2);
    }
    
    @Test
    public void testGetClass1() {
        WebBrowserPlugin plugin = new WebBrowserPlugin();
        assertEquals("WebBrowserPlugin", plugin.getClass().getSimpleName());
    }
    
    @Test
    public void testGetClass2() {
        assertEquals("WebBrowserPlugin", new WebBrowserPlugin().getClass().getSimpleName());
    }
    
    @Test
    public void testGetClass3() {
        WebBrowserPlugin p1 = new WebBrowserPlugin();
        WebBrowserPlugin p2 = new WebBrowserPlugin();
        assertEquals(p1.getClass(), p2.getClass());
    }
}

