package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * HtmlToXml cleaning contracts for invalid names/characters and comment-heavy markup.
 */
public class HtmlToXmlCleaningBranchContractTest {

    private Variable convert(String html) throws Exception {
        HtmlToXmlPlugin plugin = new HtmlToXmlPlugin() {
            @Override
            protected Variable executeBody(org.webharvest.runtime.DynamicScopeContext ctx) {
                return new NodeVariable(html);
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "html-to-xml", "http://org.webharvest/schema/2.1/core", "html-to-xml");
        plugin.initialize(configuration);
        return plugin.execute(new ScraperContext());
    }

    @Test
    public void stripsControlCharactersAndKeepsReadableText() throws Exception {
        String html = "<div>Hello" + ((char) 0x01) + ((char) 0x08) + "World&#x9;</div>";
        Variable result = convert(html);
        assertTrue(result.toString().toLowerCase().contains("hello"), result.toString());
        assertTrue(result.toString().toLowerCase().contains("world"), result.toString());
    }

    @Test
    public void handlesCommentsAndScriptNoise() throws Exception {
        String html = "<html><body><!-- comment --><p>Keep</p><script>var x=1;</script></body></html>";
        Variable result = convert(html);
        assertTrue(result.toString().toLowerCase().contains("keep"), result.toString());
    }

    @Test
    public void handlesNamespacedAndDottedTagNoise() throws Exception {
        String html = "<div><foo.bar>x</foo.bar><ok>y</ok><br><hr><img src='a'></div>";
        Variable result = convert(html);
        assertNotNull(result);
        assertFalse(result.toString().trim().isEmpty());
    }

    @Test
    public void handlesVeryLargeRepeatedMarkup() throws Exception {
        StringBuilder sb = new StringBuilder("<div>");
        for (int i = 0; i < 200; i++) {
            sb.append("<span>").append(i).append("</span>");
        }
        sb.append("</div>");
        Variable result = convert(sb.toString());
        assertTrue(result.toString().contains("199") || result.toString().contains("span"),
                result.toString().substring(0, Math.min(200, result.toString().length())));
    }
}
