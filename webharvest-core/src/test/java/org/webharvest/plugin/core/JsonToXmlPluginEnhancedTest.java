package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Behavior tests for JsonToXmlPlugin using deterministic in-memory JSON inputs.
 */
public class JsonToXmlPluginEnhancedTest {

    private JsonToXmlPlugin plugin;

    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() throws org.webharvest.plugin.exceptions.PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new JsonToXmlPlugin();

        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(Collections.emptyList());
        when(configuration.getProperty("tag")).thenReturn("json");

        plugin.initialize(configuration);
    }

    private void stubJsonBody(String json) {
        PluginConfiguration jsonChild = mock(PluginConfiguration.class);
        when(jsonChild.getProperty("_textContent")).thenReturn(json);
        when(jsonChild.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(jsonChild));
    }

    @Test
    public void testMetadata() {
        assertEquals(plugin.getMetadata().getId(), "json-to-xml");
        assertEquals(plugin.getMetadata().getElementName(), "json-to-xml");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testConfigurationSchema() {
        assertTrue(plugin.getMetadata().getConfigurationSchema().allowsBody());
        assertEquals(plugin.getMetadata().getConfigurationSchema().getAttribute("tag").getDefaultValue(), "data");
    }

    @Test
    public void testSimpleJsonObject() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"name\":\"John\",\"age\":30}");

        Variable result = plugin.execute(context);
        String xml = result.toString();

        assertTrue(xml.startsWith("<json>"));
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<age>30</age>"));
        assertTrue(xml.endsWith("</json>"));
    }

    @Test
    public void testJsonArray() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("[\"item1\",\"item2\",\"item3\"]");

        Variable result = plugin.execute(context);
        String xml = result.toString();

        assertTrue(xml.contains("<json>item1</json>"));
        assertTrue(xml.contains("<json>item2</json>"));
        assertTrue(xml.contains("<json>item3</json>"));
    }

    @Test
    public void testNestedJsonObject() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"person\":{\"name\":\"John\",\"address\":{\"city\":\"Warsaw\"}}}");

        Variable result = plugin.execute(context);
        String xml = result.toString();

        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<city>Warsaw</city>"));
        assertTrue(xml.contains("</person>"));
    }

    @Test
    public void testJsonWithArray() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"items\":[{\"id\":1},{\"id\":2},{\"id\":3}]}");

        Variable result = plugin.execute(context);
        String xml = result.toString();

        assertTrue(xml.contains("<items>"));
        assertTrue(xml.contains("<item><id>1</id></item>"));
        assertTrue(xml.contains("<item><id>2</id></item>"));
        assertTrue(xml.contains("<item><id>3</id></item>"));
    }

    @Test
    public void testJsonWithNull() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"value\":null}");

        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "<json><value></value></json>");
    }

    @Test
    public void testJsonWithBoolean() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"active\":true,\"deleted\":false}");

        Variable result = plugin.execute(context);
        String xml = result.toString();

        assertTrue(xml.contains("<active>true</active>"));
        assertTrue(xml.contains("<deleted>false</deleted>"));
    }

    @Test
    public void testJsonWithNumbers() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"integer\":42,\"float\":3.14,\"negative\":-10}");

        Variable result = plugin.execute(context);
        String xml = result.toString();

        assertTrue(xml.contains("<integer>42</integer>"));
        assertTrue(xml.contains("<float>3.14</float>"));
        assertTrue(xml.contains("<negative>-10</negative>"));
    }

    @Test
    public void testEmptyJsonObject() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{}");

        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "<json></json>");
    }

    @Test
    public void testEmptyJsonArray() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("[]");

        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "");
    }

    @Test
    public void testJsonWithSpecialCharacters() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"text\":\"Special: <>&\"}");

        Variable result = plugin.execute(context);
        String xml = result.toString();

        assertTrue(xml.contains("Special: &lt;&gt;&amp;"));
        assertFalse(xml.contains("Special: <>&"));
    }

    @Test
    public void testJsonWithUnicode() throws Exception {
        ScraperContext context = new ScraperContext();
        stubJsonBody("{\"text\":\"Unicode: \\u0105\\u0107\\u0119\"}");

        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("Unicode: ąćę"));
    }

    @Test
    public void testMetadataProperties() {
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getVersion(), "2.3.0");
        assertTrue(plugin.getMetadata().getDescription().contains("JSON"));
    }
}
