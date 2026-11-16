/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.DefaultPluginConfiguration;
import java.util.Arrays;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for XsltPlugin
 */
public class XsltPluginTest {

    private XsltPlugin plugin;
    private PluginConfiguration mockConfiguration;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        plugin = new XsltPlugin();
        mockConfiguration = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    @Test
    public void testExecuteUsesConfiguredXmlAndStylesheet() throws Exception {
        String xml = "<root><name>Distinct Product 731</name></root>";
        String stylesheet = "<xsl:stylesheet version=\"1.0\" "
            + "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
            + "<xsl:template match=\"/\"><result><xsl:value-of select=\"/root/name\"/>"
            + "</result></xsl:template></xsl:stylesheet>";

        DefaultPluginConfiguration xmlChild = new DefaultPluginConfiguration(
            "xml", "", "xml");
        xmlChild.setProperty("_textContent", xml);
        DefaultPluginConfiguration stylesheetChild = new DefaultPluginConfiguration(
            "stylesheet", "", "stylesheet");
        stylesheetChild.setProperty("_textContent", stylesheet);

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "xslt", "", "xslt");
        configuration.setChildren(Arrays.<PluginConfiguration>asList(xmlChild, stylesheetChild));

        plugin.initialize(configuration);
        Variable result = plugin.execute(mockContext);

        assertNotNull(result);
        assertTrue(result.toString().contains("<result>Distinct Product 731</result>"),
            "Transformation must use the supplied XML and stylesheet, not a built-in sample");
    }

    @Test
    public void testExecuteRejectsMissingXmlChild() throws Exception {
        DefaultPluginConfiguration stylesheetChild = new DefaultPluginConfiguration(
            "stylesheet", "", "stylesheet");
        stylesheetChild.setProperty("_textContent",
            "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>");
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "xslt", "", "xslt");
        configuration.setChildren(Arrays.<PluginConfiguration>asList(stylesheetChild));

        plugin.initialize(configuration);
        try {
            plugin.execute(mockContext);
            fail("Missing <xml> input must fail instead of returning a placeholder");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Plugin execution failed"));
        }
    }

    @Test
    public void testInitialization() throws Exception {
        plugin.initialize(mockConfiguration);
        assertNotNull(plugin);
    }
}


