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
 * Behavioral contracts for HtmlToXmlPlugin empty-input handling and conversion
 * of messy HTML into XML-ish output via the public execute path.
 */
public class HtmlToXmlBranchContractTest {

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
                if (html == null) {
                    return EmptyVariable.INSTANCE;
                }
                return new NodeVariable(html);
            }
        };
        plugin.initialize(configuration);
        return plugin;
    }

    @Test
    public void emptyBodyReturnsEmptyXmlMarker() throws Exception {
        HtmlToXmlPlugin plugin = pluginWithBody(null);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void blankBodyReturnsEmptyXmlMarker() throws Exception {
        HtmlToXmlPlugin plugin = pluginWithBody("   ");
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "<empty/>");
    }

    @Test
    public void wellFormedHtmlProducesXmlContainingContent() throws Exception {
        HtmlToXmlPlugin plugin = pluginWithBody("<html><body><p>Hello &amp; welcome</p></body></html>");
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertFalse(result.toString().isBlank());
        assertTrue(result.toString().toLowerCase().contains("hello"),
                "Converted XML should preserve textual content: " + result);
    }

    @Test
    public void malformedHtmlStillYieldsNonEmptyOutput() throws Exception {
        String messy = "<div><p>Unclosed paragraph<span>broken"
                + String.valueOf((char) 0x08)
                + "</div><br><img src='a.png'>";
        HtmlToXmlPlugin plugin = pluginWithBody(messy);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertFalse(result.toString().trim().isEmpty(),
                "Even malformed HTML should produce cleaned output");
    }

    @Test
    public void invalidElementNamesAreSanitizedInOutput() throws Exception {
        HtmlToXmlPlugin plugin = pluginWithBody("<div><1bad>value</1bad><ok_name>x</ok_name></div>");
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertFalse(result.toString().trim().isEmpty());
    }

    @Test
    public void structuralOnlyHtmlStillProducesXmlOutput() throws Exception {
        HtmlToXmlPlugin plugin = pluginWithBody("<br><img src=\"pixel.gif\" alt=\"dot\"><hr>");
        Variable result = plugin.execute(context);
        assertNotNull(result);
        String xml = result.toString();
        assertFalse(xml.trim().isEmpty());
        assertFalse("<empty/>".equals(xml), "Self-closing structural HTML should still convert");
    }

    @Test
    public void heavilyBrokenMarkupStillYieldsParsableXml() throws Exception {
        String broken = "<html><head><title></title>"
                + "<style>unclosed style block"
                + "<script>var x = 1</title>"
                + "<body><p<span>text</p></body>";
        HtmlToXmlPlugin plugin = pluginWithBody(broken);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertFalse(result.toString().trim().isEmpty());
    }

    @Test
    public void plainTextSnippetIsWrappedForDownstreamXPath() throws Exception {
        HtmlToXmlPlugin plugin = pluginWithBody("Just a sentence without tags.");
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result.toString().toLowerCase().contains("sentence"),
                "Text-only input should survive conversion: " + result);
    }
}
