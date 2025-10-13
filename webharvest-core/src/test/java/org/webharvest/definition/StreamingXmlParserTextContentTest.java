/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.definition;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.ConfigSource;
// REMOVED in v2.2.x - DefinitionResolver no longer exists
// import org.webharvest.definition.DefinitionResolver;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.PluginConfiguration;

import static org.testng.Assert.*;

/**
 * ULTRA-LEAN v2.2.x: Test that StreamingXmlParser correctly parses text content and CDATA
 * as _textContent property in PluginConfiguration.
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class StreamingXmlParserTextContentTest {

    private StreamingXmlParser parser;

    @BeforeMethod
    public void setUp() {
        // v2.2.x ULTRA-LEAN: StreamingXmlParser no longer needs resolver
        // Injector injector = Guice.createInjector(new ScraperModule("."));
        // org.webharvest.definition.ConfigurableResolver resolver = 
        //     injector.getInstance(DefinitionResolver.class);
        parser = new StreamingXmlParser();
    }

    @Test
    public void testParsesCDATAAsTextContent() throws Exception {
        String xml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                     "    <constant><![CDATA[Hello CDATA World]]></constant>\n" +
                     "</config>";

        ConfigSource source = new org.webharvest.definition.BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);

        assertNotNull(tree);
        assertEquals(tree.getChildren().size(), 1);
        
        PluginConfigurationTree constantTree = tree.getChildren().get(0);
        assertEquals(constantTree.getPluginElementName(), "constant");
        
        PluginConfiguration config = constantTree.getConfiguration();
        String textContent = config.getProperty("_textContent");
        
        assertNotNull(textContent, "_textContent should be set from CDATA");
        assertEquals(textContent, "Hello CDATA World");
    }

    @Test
    public void testParsesPlainTextAsTextContent() throws Exception {
        String xml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                     "    <template>Plain text template</template>\n" +
                     "</config>";

        ConfigSource source = new org.webharvest.definition.BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);

        PluginConfigurationTree templateTree = tree.getChildren().get(0);
        String textContent = templateTree.getConfiguration().getProperty("_textContent");
        
        assertNotNull(textContent);
        assertEquals(textContent, "Plain text template");
    }

    @Test
    public void testParsesMultilineTextContent() throws Exception {
        String xml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                     "    <constant>\n" +
                     "        Line 1\n" +
                     "        Line 2\n" +
                     "        Line 3\n" +
                     "    </constant>\n" +
                     "</config>";

        ConfigSource source = new org.webharvest.definition.BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);

        PluginConfigurationTree constantTree = tree.getChildren().get(0);
        String textContent = constantTree.getConfiguration().getProperty("_textContent");
        
        assertNotNull(textContent);
        assertTrue(textContent.contains("Line 1"));
        assertTrue(textContent.contains("Line 2"));
        assertTrue(textContent.contains("Line 3"));
    }

    @Test
    public void testParsesEmptyTextContent() throws Exception {
        String xml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                     "    <constant></constant>\n" +
                     "</config>";

        ConfigSource source = new org.webharvest.definition.BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);

        PluginConfigurationTree constantTree = tree.getChildren().get(0);
        String textContent = constantTree.getConfiguration().getProperty("_textContent");
        
        // Empty text content might be null or empty string
        assertTrue(textContent == null || textContent.isEmpty());
    }

    @Test
    public void testParsesChildrenInConfiguration() throws Exception {
        String xml = "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                     "    <def var=\"test\">\n" +
                     "        <constant>Value</constant>\n" +
                     "    </def>\n" +
                     "</config>";

        ConfigSource source = new org.webharvest.definition.BufferConfigSource(xml);
        PluginConfigurationTree tree = parser.parseToTree(source);

        PluginConfigurationTree defTree = tree.getChildren().get(0);
        PluginConfiguration defConfig = defTree.getConfiguration();
        
        assertNotNull(defConfig.getChildren(), "children should be set");
        assertTrue(defConfig.hasChildren(), "hasChildren() should return true");
        assertEquals(defConfig.getChildren().size(), 1, "should have 1 child");
        
        PluginConfiguration childConfig = defConfig.getChildren().get(0);
        assertEquals(childConfig.getElementName(), "constant");
    }
}

