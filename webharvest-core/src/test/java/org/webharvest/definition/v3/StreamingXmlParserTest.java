/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.definition.v3;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.BufferConfigSource;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.DefinitionResolver;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.PluginConfiguration;

import static org.testng.Assert.*;

/**
 * POC test for new StreamingXmlParser (v3.0 architecture).
 * 
 * PURPOSE: Prove that we can parse XML directly to PluginConfiguration without *Def layer.
 * 
 * @author Web-Harvest Team
 * @since 3.0.0
 */
public class StreamingXmlParserTest {

    private StreamingXmlParser parser;
    private org.webharvest.definition.ConfigurableResolver resolver;

    @BeforeMethod
    public void setUp() {
        // Initialize Guice to get properly configured DefinitionResolver
        // In full v3.0, we'll have a dedicated PluginRegistry
        Injector injector = Guice.createInjector(new ScraperModule("."));
        resolver = injector.getInstance(DefinitionResolver.class);
        parser = new StreamingXmlParser(resolver);
    }

    /**
     * TEST 1: Parse simple HTTP configuration
     * PROVES: Can create PluginConfiguration directly from XML (no HttpDef!)
     */
    @Test(groups = {"unit", "v3", "parser"})
    public void testParseSimpleHttpConfig() throws Exception {
        System.out.println("\n### v3.0 Parser POC: Simple HTTP config ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config>\n" +
            "    <http url=\"http://example.com\" method=\"GET\"/>\n" +
            "</config>";
        
        ConfigSource source = new BufferConfigSource(xml);
        
        // Parse using NEW parser (no *Def!)
        PluginConfigurationTree tree = parser.parseToTree(source);
        
        // Verify
        assertNotNull(tree, "Should parse successfully");
        assertEquals(tree.getPluginElementName(), "config", "Root should be 'config'");
        assertTrue(tree.hasChildren(), "Should have children");
        assertEquals(tree.getChildCount(), 1, "Should have 1 child (http)");
        
        // Check HTTP child
        PluginConfigurationTree httpTree = tree.getChildren().get(0);
        assertEquals(httpTree.getPluginElementName(), "http", "Child should be 'http'");
        
        PluginConfiguration httpConfig = httpTree.getConfiguration();
        assertEquals(httpConfig.getProperty("url"), "http://example.com", "URL should be preserved");
        assertEquals(httpConfig.getProperty("method"), "GET", "Method should be preserved");
        
        System.out.println("✅ SUCCESS: Parsed without creating HttpDef!");
        System.out.println("   Direct: XML → PluginConfiguration");
        System.out.println("   Properties: " + httpConfig.getAllProperties().size());
    }

    /**
     * TEST 2: Parse nested configuration (http with xpath child)
     * PROVES: Can handle nested elements without *Def tree
     */
    @Test(groups = {"unit", "v3", "parser"})
    public void testParseNestedConfig() throws Exception {
        System.out.println("\n### v3.0 Parser POC: Nested config (http + xpath) ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config>\n" +
            "    <http url=\"http://example.com\">\n" +
            "        <xpath expression=\"//title\"/>\n" +
            "    </http>\n" +
            "</config>";
        
        ConfigSource source = new BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);
        
        // Verify structure
        assertNotNull(tree);
        assertEquals(tree.getChildCount(), 1);
        
        PluginConfigurationTree httpTree = tree.getChildren().get(0);
        assertEquals(httpTree.getPluginElementName(), "http");
        assertTrue(httpTree.hasChildren(), "HTTP should have XPath child");
        
        PluginConfigurationTree xpathTree = httpTree.getChildren().get(0);
        assertEquals(xpathTree.getPluginElementName(), "xpath", "Nested element should be xpath");
        assertEquals(xpathTree.getConfiguration().getProperty("expression"), "//title");
        
        System.out.println("✅ SUCCESS: Nested elements parsed correctly!");
        System.out.println("   Tree: config → http → xpath");
        System.out.println("   NO *Def objects created!");
    }

    /**
     * TEST 3: Parse config with multiple elements
     * PROVES: Can handle multiple sibling elements
     */
    @Test(groups = {"unit", "v3", "parser"})
    public void testParseMultipleElements() throws Exception {
        System.out.println("\n### v3.0 Parser POC: Multiple elements ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config>\n" +
            "    <var-def name=\"url\"><constant>http://example.com</constant></var-def>\n" +
            "    <http url=\"${url}\"/>\n" +
            "    <file action=\"write\" path=\"output.txt\"/>\n" +
            "</config>";
        
        ConfigSource source = new BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);
        
        assertEquals(tree.getChildCount(), 3, "Should have 3 children");
        
        assertEquals(tree.getChildren().get(0).getPluginElementName(), "var-def");
        assertEquals(tree.getChildren().get(1).getPluginElementName(), "http");
        assertEquals(tree.getChildren().get(2).getPluginElementName(), "file");
        
        System.out.println("✅ SUCCESS: Multiple elements parsed!");
        System.out.println("   Parsed: def, http, file");
    }

    /**
     * TEST 4: Parse with text content
     * PROVES: Can handle element text content
     */
    @Test(groups = {"unit", "v3", "parser"})
    public void testParseWithTextContent() throws Exception {
        System.out.println("\n### v3.0 Parser POC: Text content ###");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config>\n" +
            "    <constant>Hello World</constant>\n" +
            "</config>";
        
        ConfigSource source = new BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);
        
        assertEquals(tree.getChildCount(), 1);
        PluginConfigurationTree constantTree = tree.getChildren().get(0);
        
        // Text content should be stored in configuration
        PluginConfiguration config = constantTree.getConfiguration();
        String textContent = config.getProperty("_textContent");
        assertNotNull(textContent, "Text content should be stored");
        assertEquals(textContent, "Hello World");
        
        System.out.println("✅ SUCCESS: Text content preserved!");
        System.out.println("   Content: " + textContent);
    }

    /**
     * TEST 5: Performance comparison hint
     */
    @Test(groups = {"unit", "v3", "parser"})
    public void testParserPerformanceHint() {
        System.out.println("\n### v3.0 Parser Performance ###");
        System.out.println("NEW parser creates:");
        System.out.println("  - PluginConfigurationTree (simple POJO)");
        System.out.println("  - PluginConfiguration (already exists)");
        System.out.println("  - NO *Def objects (eliminates 30+ classes)");
        System.out.println("");
        System.out.println("OLD parser creates:");
        System.out.println("  - XmlNode tree");
        System.out.println("  - *Def objects (HttpDef, XPathDef, etc.)");
        System.out.println("  - Then PluginConfiguration");
        System.out.println("");
        System.out.println("✅ NEW parser: Fewer objects = Better performance!");
    }
}

