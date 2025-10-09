package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.JsonXmlConverter;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Test for Bug #61: JSONToXML "content" key issue
 * 
 * Issue: org.json.XML.toString has a special case for the key "content".
 * This causes incorrect conversion:
 * 
 * Input:  {"a": {"content": {"b": "c"}}}
 * Bug:    Renders as <a><b>c</b></a> (content key is stripped!)
 * Expected: <a><content><b>c</b></content></a>
 * 
 * The "content" key is treated specially by org.json library and used
 * to represent text nodes, but this breaks normal JSON→XML conversion.
 */
public class JsonToXmlPluginBug61Test {

    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    /**
     * Test Bug #61: "content" key should be preserved in XML output
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testContentKeyIsPreservedInXmlOutput() throws Exception {
        JsonToXmlPlugin plugin = new JsonToXmlPlugin();
        
        // JSON with "content" key - this should be preserved!
        String json = "{\"a\": {\"content\": {\"b\": \"c\"}}}";
        
        // Set JSON as a variable
        context.setLocalVar("testjson", json);
        
        Map<String, String> config = new HashMap<>();
        config.put("tag", "root");
        
        plugin.initialize(new DefaultPluginConfiguration("test-json-to-xml", 
                "http://org.webharvest/schema/2.1/core", "json-to-xml", config));
        plugin.setElementDef(null);
        
        // Mock body execution by directly testing with JSON string
        // We'll need to test this differently since we can't easily mock executeBody
        
        // Test using JsonXmlConverter (our fix for Bug #61)
        org.json.JSONObject jsonObj = new org.json.JSONObject(json);
        String xmlOutput = JsonXmlConverter.toXml(jsonObj, "root");
        
        System.out.println("Bug #61 test:");
        System.out.println("  Input JSON: " + json);
        System.out.println("  Output XML: " + xmlOutput);
        
        // BUG: "content" key is lost in conversion
        // org.json.XML uses "content" as special key for text nodes
        // So {"a": {"content": {"b": "c"}}} becomes <root><a><b>c</b></a></root>
        // instead of <root><a><content><b>c</b></content></a></root>
        
        if (xmlOutput.contains("<content>")) {
            System.out.println("  ✅ FIXED: 'content' key is preserved");
            assertTrue(xmlOutput.contains("<content>"), "content key should be preserved");
        } else {
            System.out.println("  ❌ BUG CONFIRMED: 'content' key is stripped");
            System.out.println("  Expected: <root><a><content><b>c</b></content></a></root>");
            System.out.println("  Actual:   " + xmlOutput);
            
            // This test SHOULD FAIL before fix
            fail("Bug #61: 'content' key is not preserved in XML output. " +
                 "org.json.XML treats 'content' as special text node key.");
        }
    }

    /**
     * Test case 2: Simple "content" key with string value
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testSimpleContentKey() throws Exception {
        String json = "{\"a\": {\"content\": \"hello world\"}}";
        org.json.JSONObject jsonObj = new org.json.JSONObject(json);
        String xmlOutput = JsonXmlConverter.toXml(jsonObj, "root");
        
        System.out.println("Simple content test:");
        System.out.println("  Input:  " + json);
        System.out.println("  Output: " + xmlOutput);
        
        // With org.json special handling, this becomes: <root><a>hello world</a>
        // Expected without special handling: <root><a><content>hello world</content></a>
        
        if (xmlOutput.contains("<content>")) {
            System.out.println("  ✅ FIXED");
            assertTrue(true);
        } else {
            System.out.println("  ❌ BUG: 'content' treated as text node");
            fail("Bug #61: 'content' key with string value is not preserved");
        }
    }

    /**
     * Test case 3: Nested "content" keys
     */
    @Test(groups = {"unit", "bugs", "hard", "complex"})
    public void testNestedContentKeys() throws Exception {
        String json = "{\"article\": {\"content\": {\"paragraph\": {\"content\": \"text\"}}}}";
        org.json.JSONObject jsonObj = new org.json.JSONObject(json);
        String xmlOutput = JsonXmlConverter.toXml(jsonObj, "root");
        
        System.out.println("Nested content test:");
        System.out.println("  Input:  " + json);
        System.out.println("  Output: " + xmlOutput);
        
        // Both "content" keys should be preserved
        int contentCount = xmlOutput.split("<content>").length - 1;
        
        System.out.println("  Content tags found: " + contentCount);
        
        if (contentCount >= 2) {
            System.out.println("  ✅ FIXED: Both 'content' keys preserved");
            assertTrue(true);
        } else {
            System.out.println("  ❌ BUG: Nested 'content' keys not preserved");
            fail("Bug #61: Nested 'content' keys should both be preserved");
        }
    }

    /**
     * Test case 4: "content" key should work like any other key
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testContentKeyLikeOtherKeys() throws Exception {
        // Two similar structures - one with "content", one with "data"
        String json1 = "{\"item\": {\"content\": \"value1\"}}";
        String json2 = "{\"item\": {\"data\": \"value2\"}}";
        
        String xml1 = JsonXmlConverter.toXml(new org.json.JSONObject(json1), "root");
        String xml2 = JsonXmlConverter.toXml(new org.json.JSONObject(json2), "root");
        
        System.out.println("Comparison test:");
        System.out.println("  JSON with 'content': " + json1);
        System.out.println("    XML: " + xml1);
        System.out.println("  JSON with 'data':    " + json2);
        System.out.println("    XML: " + xml2);
        
        // Both should have similar structure (both keys as elements)
        boolean hasContentElement = xml1.contains("<content>");
        boolean hasDataElement = xml2.contains("<data>");
        
        if (hasContentElement == hasDataElement) {
            System.out.println("  ✅ FIXED: Both keys treated equally");
            assertTrue(true);
        } else {
            System.out.println("  ❌ BUG: 'content' treated differently than 'data'");
            fail("Bug #61: 'content' key should behave like any other JSON key");
        }
    }

    /**
     * REGRESSION TEST 1: Arrays should still work correctly
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testArrayConversion_NoRegression() throws Exception {
        String json = "{\"items\": [\"a\", \"b\", \"c\"]}";
        String xmlOutput = JsonXmlConverter.toXml(new org.json.JSONObject(json), "root");
        
        System.out.println("Array regression test:");
        System.out.println("  Input:  " + json);
        System.out.println("  Output: " + xmlOutput);
        
        assertTrue(xmlOutput.contains("<items>"), "Should have items element");
        assertTrue(xmlOutput.contains("<item>a</item>"), "Should have array items");
        assertTrue(xmlOutput.contains("<item>b</item>"), "Should have array items");
        assertTrue(xmlOutput.contains("<item>c</item>"), "Should have array items");
    }

    /**
     * REGRESSION TEST 2: Nested objects without "content" should work
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testNestedObjects_NoRegression() throws Exception {
        String json = "{\"user\": {\"name\": \"John\", \"address\": {\"city\": \"NYC\", \"zip\": \"10001\"}}}";
        String xmlOutput = JsonXmlConverter.toXml(new org.json.JSONObject(json), "root");
        
        System.out.println("Nested objects regression test:");
        System.out.println("  Input:  " + json);
        System.out.println("  Output: " + xmlOutput);
        
        assertTrue(xmlOutput.contains("<user>"), "Should have user element");
        assertTrue(xmlOutput.contains("<name>John</name>"), "Should have name");
        assertTrue(xmlOutput.contains("<address>"), "Should have nested address");
        assertTrue(xmlOutput.contains("<city>NYC</city>"), "Should have city");
        assertTrue(xmlOutput.contains("<zip>10001</zip>"), "Should have zip");
    }

    /**
     * REGRESSION TEST 3: Special characters should be escaped
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testXmlEscaping_NoRegression() throws Exception {
        String json = "{\"text\": \"<tag> & 'quotes' \\\"double\\\"\"}";
        String xmlOutput = JsonXmlConverter.toXml(new org.json.JSONObject(json), "root");
        
        System.out.println("XML escaping regression test:");
        System.out.println("  Input:  " + json);
        System.out.println("  Output: " + xmlOutput);
        
        // Characters should be escaped
        assertTrue(xmlOutput.contains("&lt;tag&gt;"), "< and > should be escaped");
        assertTrue(xmlOutput.contains("&amp;"), "& should be escaped");
        assertTrue(xmlOutput.contains("&apos;") || xmlOutput.contains("'"), "Quotes should be handled");
        assertFalse(xmlOutput.contains("<tag>"), "Raw < should not appear");
    }

    /**
     * REGRESSION TEST 4: Numbers and booleans should work
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testPrimitiveTypes_NoRegression() throws Exception {
        String json = "{\"count\": 42, \"price\": 19.99, \"active\": true, \"archived\": false}";
        String xmlOutput = JsonXmlConverter.toXml(new org.json.JSONObject(json), "root");
        
        System.out.println("Primitive types regression test:");
        System.out.println("  Input:  " + json);
        System.out.println("  Output: " + xmlOutput);
        
        assertTrue(xmlOutput.contains("<count>42</count>"), "Integer should work");
        assertTrue(xmlOutput.contains("<price>19.99</price>"), "Float should work");
        assertTrue(xmlOutput.contains("<active>true</active>"), "Boolean true should work");
        assertTrue(xmlOutput.contains("<archived>false</archived>"), "Boolean false should work");
    }

    /**
     * REGRESSION TEST 5: Empty objects and null values
     */
    @Test(groups = {"unit", "bugs", "regression", "edge-case"})
    public void testEmptyAndNull_NoRegression() throws Exception {
        String json = "{\"empty\": {}, \"nullval\": null}";
        String xmlOutput = JsonXmlConverter.toXml(new org.json.JSONObject(json), "root");
        
        System.out.println("Empty/null regression test:");
        System.out.println("  Input:  " + json);
        System.out.println("  Output: " + xmlOutput);
        
        assertTrue(xmlOutput.contains("<empty>"), "Empty object should have element");
        assertTrue(xmlOutput.contains("<nullval"), "Null should have element");
    }

    /**
     * HARD TEST: Complex real-world JSON with "content" key
     */
    @Test(groups = {"unit", "bugs", "hard", "real-world"})
    public void testRealWorldJsonWithContentKey() throws Exception {
        String json = "{\n" +
                "  \"article\": {\n" +
                "    \"id\": 123,\n" +
                "    \"title\": \"Test Article\",\n" +
                "    \"content\": {\n" +
                "      \"type\": \"html\",\n" +
                "      \"body\": \"<p>Article body</p>\",\n" +
                "      \"summary\": \"Brief summary\"\n" +
                "    },\n" +
                "    \"metadata\": {\n" +
                "      \"author\": \"John Doe\",\n" +
                "      \"date\": \"2025-10-09\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        String xmlOutput = JsonXmlConverter.toXml(new org.json.JSONObject(json), "root");
        
        System.out.println("Real-world complex JSON test:");
        System.out.println("  Output: " + xmlOutput);
        
        // ALL keys should be preserved, including "content"
        assertTrue(xmlOutput.contains("<article>"), "Should have article");
        assertTrue(xmlOutput.contains("<id>123</id>"), "Should have id");
        assertTrue(xmlOutput.contains("<title>Test Article</title>"), "Should have title");
        assertTrue(xmlOutput.contains("<content>"), "CRITICAL: content key must be preserved!");
        assertTrue(xmlOutput.contains("<type>html</type>"), "Should have nested type");
        assertTrue(xmlOutput.contains("<body>"), "Should have nested body");
        assertTrue(xmlOutput.contains("<summary>Brief summary</summary>"), "Should have summary");
        assertTrue(xmlOutput.contains("<metadata>"), "Should have metadata");
        assertTrue(xmlOutput.contains("<author>John Doe</author>"), "Should have author");
        
        // Verify XML structure is valid
        int openContent = xmlOutput.split("<content>").length - 1;
        int closeContent = xmlOutput.split("</content>").length - 1;
        assertEquals(openContent, closeContent, "Opening and closing content tags should match");
        assertEquals(openContent, 1, "Should have exactly 1 content element");
    }
}


