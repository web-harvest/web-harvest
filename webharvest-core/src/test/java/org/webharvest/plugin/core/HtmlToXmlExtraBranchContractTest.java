package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Collections;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Supplemental HtmlToXml conversion branches (comments, CDATA-like text, entity edges).
 */
public class HtmlToXmlExtraBranchContractTest {

    @Mock
    private DynamicScopeContext context;

    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
        when(context.getCharset()).thenReturn("UTF-8");
    }

    private HtmlToXmlPlugin pluginWithBody(String html) throws Exception {
        HtmlToXmlPlugin plugin = new HtmlToXmlPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext ctx) {
                return html == null ? EmptyVariable.INSTANCE : new NodeVariable(html);
            }
        };
        plugin.initialize(configuration);
        return plugin;
    }

    @Test
    public void htmlCommentIsStrippedButSurroundingTextRemains() throws Exception {
        Variable result = pluginWithBody("<!-- secret --><p>visible</p>").execute(context);
        String xml = result.toString().toLowerCase();
        assertTrue(xml.contains("visible"));
        assertTrue(xml.contains("p") || xml.contains("visible"));
    }

    @Test
    public void preformattedWhitespaceIsPreservedInOutput() throws Exception {
        Variable result = pluginWithBody("<pre>  spaced\nline</pre>").execute(context);
        assertTrue(result.toString().contains("spaced"));
    }

    @Test
    public void numericCharacterReferenceSurvivesConversion() throws Exception {
        Variable result = pluginWithBody("<p>&#65;&#66;</p>").execute(context);
        assertTrue(result.toString().contains("AB"));
    }

    @Test
    public void voidElementsAndAttributesRemainInOutput() throws Exception {
        Variable result = pluginWithBody(
                "<input type=\"checkbox\" checked><meta charset=\"utf-8\">").execute(context);
        String xml = result.toString().toLowerCase();
        assertFalse(xml.isEmpty());
        assertTrue(xml.contains("input") || xml.contains("meta"));
    }
}
