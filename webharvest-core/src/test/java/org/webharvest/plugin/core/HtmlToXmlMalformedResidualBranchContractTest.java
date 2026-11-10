package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import java.util.Collections;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.when;

/**
 * Residual HtmlToXml contracts for xlink/invalid attributes, comment noise,
 * and XML character-class edge input through the public execute path.
 */
public class HtmlToXmlMalformedResidualBranchContractTest {

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
                return new NodeVariable(html);
            }
        };
        plugin.initialize(configuration);
        return plugin;
    }

    @Test
    public void stripsXlinkAttributesWhilePreservingElementText() throws Exception {
        Variable result = pluginWithBody(
                "<svg><a xlink:href=\"http://example.com\" href=\"keep\">link</a></svg>")
                .execute(context);
        String xml = result.toString().toLowerCase();
        assertTrue(xml.contains("link"), xml);
        assertFalse(xml.contains("xlink:href"), xml);
    }

    @Test
    public void dropsInvalidAttributeNamesAndKeepsValidOnes() throws Exception {
        Variable result = pluginWithBody(
                "<div 1bad=\"no\" _ok=\"yes\" data-value=\"1\">content</div>")
                .execute(context);
        String xml = result.toString();
        assertTrue(xml.toLowerCase().contains("content"), xml);
        assertFalse(xml.contains("1bad="), xml);
        assertTrue(xml.contains("_ok") || xml.contains("data-value") || xml.contains("content"), xml);
    }

    @Test
    public void removesCommentsContainingDoubleDashSequences() throws Exception {
        Variable result = pluginWithBody(
                "<div><!-- bad -- comment --><p>keep</p></div>")
                .execute(context);
        String xml = result.toString().toLowerCase();
        assertTrue(xml.contains("keep"), xml);
        assertFalse(xml.contains("bad -- comment"), xml);
    }

    @Test
    public void preservesTabNewlineAndCarriageReturnWhileScrubbingInvalidChars() throws Exception {
        String html = "<pre>A\tB\nC\rD" + ((char) 0x0B) + ((char) 0x1F) + "E</pre>";
        Variable result = pluginWithBody(html).execute(context);
        String out = result.toString();
        assertTrue(out.contains("A"), out);
        assertTrue(out.contains("E"), out);
        assertFalse(out.contains(String.valueOf((char) 0x0B)), out);
        assertFalse(out.contains(String.valueOf((char) 0x1F)), out);
    }

    @Test
    public void dottedAndNumericTagNamesAreNormalizedWithoutLosingText() throws Exception {
        Variable result = pluginWithBody(
                "<wrapper><foo.bar>x</foo.bar><123>y</123><ok:name>z</ok:name></wrapper>")
                .execute(context);
        String xml = result.toString().toLowerCase();
        assertTrue(xml.contains("x") || xml.contains("y") || xml.contains("z"), xml);
        assertFalse(xml.trim().isEmpty());
    }
}
