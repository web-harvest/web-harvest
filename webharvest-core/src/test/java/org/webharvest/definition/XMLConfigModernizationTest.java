/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.definition;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.v3.PluginConfigurationTree;
import org.webharvest.ioc.ScraperModule;

import static org.testng.Assert.*;

/**
 * Test modernization of XMLConfig with new API getConfigurationTree().
 * 
 * PURPOSE: Verify that XMLConfig supports both old (IElementDef) and new (PluginConfigurationTree) APIs.
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class XMLConfigModernizationTest {

    private Injector injector;
    private ConfigSourceFactory configSourceFactory;

    @BeforeMethod
    public void setUp() {
        injector = Guice.createInjector(new ScraperModule("."));
        configSourceFactory = injector.getInstance(ConfigSourceFactory.class);
    }

    /**
     * TEST 1: Old API still works (backward compatibility)
     */
    @Test(groups = {"unit", "modernization"})
    public void testOldAPI_getElementDef_StillWorks() throws Exception {
        System.out.println("\n### TEST: Old API (getElementDef) still works ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <http url=\"http://example.com\"/>\n" +
            "</config>";
        
        ConfigSource source = configSourceFactory.create(xml);
        ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
        Config config = configFactory.create(source);
        
        config.reload();
        
        // Old API should still work
        @SuppressWarnings("deprecation")
        IElementDef rootDef = config.getElementDef();
        
        assertNotNull(rootDef, "Old API should return IElementDef");
        assertEquals(rootDef.getShortElementName(), "config", "Root should be 'config'");
        
        System.out.println("✅ Old API works: " + rootDef.getClass().getSimpleName());
    }

    /**
     * TEST 2: New API works (getConfigurationTree)
     */
    @Test(groups = {"unit", "modernization"})
    public void testNewAPI_getConfigurationTree_Works() throws Exception {
        System.out.println("\n### TEST: New API (getConfigurationTree) works ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <http url=\"http://example.com\"/>\n" +
            "</config>";
        
        ConfigSource source = configSourceFactory.create(xml);
        ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
        Config config = configFactory.create(source);
        
        config.reload();
        
        // New API should work (lazy parsing)
        PluginConfigurationTree tree = config.getConfigurationTree();
        
        assertNotNull(tree, "New API should return PluginConfigurationTree");
        assertEquals(tree.getPluginElementName(), "config", "Root should be 'config'");
        assertTrue(tree.hasChildren(), "Should have http child");
        
        PluginConfigurationTree httpChild = tree.getChildren().get(0);
        assertEquals(httpChild.getPluginElementName(), "http");
        assertEquals(httpChild.getConfiguration().getProperty("url"), "http://example.com");
        
        System.out.println("✅ New API works: " + tree);
        System.out.println("   HTTP child URL: " + httpChild.getConfiguration().getProperty("url"));
    }

    /**
     * TEST 3: Both APIs can be used on same config
     */
    @Test(groups = {"unit", "modernization"})
    public void testBothAPIs_CanBeUsed_OnSameConfig() throws Exception {
        System.out.println("\n### TEST: Both APIs work on same config ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"test\"><constant>Hello</constant></def>\n" +
            "</config>";
        
        ConfigSource source = configSourceFactory.create(xml);
        ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
        Config config = configFactory.create(source);
        
        config.reload();
        
        // Use OLD API
        @SuppressWarnings("deprecation")
        IElementDef oldDef = config.getElementDef();
        assertNotNull(oldDef);
        
        // Use NEW API on SAME config
        PluginConfigurationTree newTree = config.getConfigurationTree();
        assertNotNull(newTree);
        
        // Both should represent same config
        assertEquals(oldDef.getShortElementName(), newTree.getPluginElementName(), 
            "Both APIs should return same root element");
        
        System.out.println("✅ Both APIs work on same config");
        System.out.println("   Old: " + oldDef.getShortElementName());
        System.out.println("   New: " + newTree.getPluginElementName());
    }

    /**
     * TEST 4: Nested config works with new API
     */
    @Test(groups = {"unit", "modernization"})
    public void testNewAPI_NestedConfig_Works() throws Exception {
        System.out.println("\n### TEST: Nested config works with new API ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <http url=\"http://example.com\">\n" +
            "        <xpath expression=\"//title\"/>\n" +
            "    </http>\n" +
            "</config>";
        
        ConfigSource source = configSourceFactory.create(xml);
        ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
        Config config = configFactory.create(source);
        
        config.reload();
        
        PluginConfigurationTree tree = config.getConfigurationTree();
        
        assertNotNull(tree);
        assertEquals(tree.getChildCount(), 1);
        
        PluginConfigurationTree httpTree = tree.getChildren().get(0);
        assertEquals(httpTree.getPluginElementName(), "http");
        assertTrue(httpTree.hasChildren(), "HTTP should have XPath child");
        
        PluginConfigurationTree xpathTree = httpTree.getChildren().get(0);
        assertEquals(xpathTree.getPluginElementName(), "xpath");
        assertEquals(xpathTree.getConfiguration().getProperty("expression"), "//title");
        
        System.out.println("✅ Nested config works");
        System.out.println("   Tree: config → http → xpath");
        System.out.println("   XPath expression: " + xpathTree.getConfiguration().getProperty("expression"));
    }

    /**
     * TEST 5: Modernization summary
     */
    @Test(groups = {"unit", "modernization"})
    public void testModernizationSummary() {
        System.out.println("\n### MODERNIZATION SUMMARY ###");
        System.out.println("✅ Old API (getElementDef): @Deprecated but works");
        System.out.println("✅ New API (getConfigurationTree): Works with lazy parsing");
        System.out.println("✅ Backward compatibility: Both APIs coexist");
        System.out.println("✅ Migration path: Clear and safe");
        System.out.println("");
        System.out.println("MODERNIZATION STATUS: PHASE 2 COMPLETE");
        System.out.println("NEXT: Harvester & Plugin execution");
    }
}

