package org.webharvest.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for JsonXmlConverter - Bug #61 fix.
 * Verifies that "content" key is preserved (no special handling).
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class JsonXmlConverterTest {

    // ========== Basic JSONObject to XML Tests ==========

    @Test
    public void testSimpleObjectWithRootTag() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", "John");
        json.put("age", 30);

        String xml = JsonXmlConverter.toXml(json, "person");
        
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("</person>"));
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<age>30</age>"));
    }

    @Test
    public void testSimpleObjectWithoutRootTag() throws Exception {
        JSONObject json = new JSONObject();
        json.put("key", "value");

        String xml = JsonXmlConverter.toXml(json, null);
        
        assertFalse(xml.contains("<null>"));
        assertTrue(xml.contains("<key>value</key>"));
    }

    @Test
    public void testSimpleObjectWithEmptyRootTag() throws Exception {
        JSONObject json = new JSONObject();
        json.put("key", "value");

        String xml = JsonXmlConverter.toXml(json, "");
        
        assertTrue(xml.contains("<key>value</key>"));
        assertFalse(xml.startsWith("<>"));
    }

    @Test
    public void testEmptyObject() throws Exception {
        JSONObject json = new JSONObject();

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertEquals(xml, "<root></root>");
    }

    // ========== Bug #61: "content" Key Preservation ==========

    @Test
    public void testContentKeyIsPreservedNotStripped() throws Exception {
        JSONObject json = new JSONObject();
        JSONObject nested = new JSONObject();
        nested.put("b", "c");
        
        JSONObject content = new JSONObject();
        content.put("content", nested);
        
        json.put("a", content);

        String xml = JsonXmlConverter.toXml(json, "root");
        
        // Bug #61 FIX: "content" key must be present!
        assertTrue(xml.contains("<content>"), "Bug #61: 'content' key must be preserved!");
        assertTrue(xml.contains("</content>"), "Bug #61: 'content' closing tag must be present!");
        assertTrue(xml.contains("<b>c</b>"));
    }

    @Test
    public void testContentKeyAsDirectValue() throws Exception {
        JSONObject json = new JSONObject();
        json.put("content", "This is content");

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<content>This is content</content>"));
    }

    @Test
    public void testMultipleContentKeys() throws Exception {
        JSONObject json = new JSONObject();
        json.put("content", "First content");
        json.put("data", new JSONObject().put("content", "Second content"));

        String xml = JsonXmlConverter.toXml(json, "root");
        
        int contentCount = xml.split("<content>").length - 1;
        assertTrue(contentCount >= 2, "Should have at least 2 'content' elements");
    }

    // ========== Nested Objects Tests ==========

    @Test
    public void testNestedObject() throws Exception {
        JSONObject inner = new JSONObject();
        inner.put("street", "123 Main St");
        inner.put("city", "Boston");
        
        JSONObject outer = new JSONObject();
        outer.put("name", "Alice");
        outer.put("address", inner);

        String xml = JsonXmlConverter.toXml(outer, "person");
        
        assertTrue(xml.contains("<name>Alice</name>"));
        assertTrue(xml.contains("<address>"));
        assertTrue(xml.contains("<street>123 Main St</street>"));
        assertTrue(xml.contains("<city>Boston</city>"));
        assertTrue(xml.contains("</address>"));
    }

    @Test
    public void testDeeplyNestedObject() throws Exception {
        JSONObject level3 = new JSONObject();
        level3.put("value", "deep");
        
        JSONObject level2 = new JSONObject();
        level2.put("level3", level3);
        
        JSONObject level1 = new JSONObject();
        level1.put("level2", level2);

        String xml = JsonXmlConverter.toXml(level1, "root");
        
        assertTrue(xml.contains("<level2>"));
        assertTrue(xml.contains("<level3>"));
        assertTrue(xml.contains("<value>deep</value>"));
    }

    // ========== JSONArray Tests ==========

    @Test
    public void testSimpleArray() throws Exception {
        JSONArray array = new JSONArray();
        array.put("one");
        array.put("two");
        array.put("three");

        String xml = JsonXmlConverter.toXml(array, "item");
        
        assertTrue(xml.contains("<item>one</item>"));
        assertTrue(xml.contains("<item>two</item>"));
        assertTrue(xml.contains("<item>three</item>"));
    }

    @Test
    public void testArrayWithDefaultItemTag() throws Exception {
        JSONArray array = new JSONArray();
        array.put("value");

        String xml = JsonXmlConverter.toXml(array, null);
        
        assertTrue(xml.contains("<item>value</item>"));
    }

    @Test
    public void testArrayWithEmptyItemTag() throws Exception {
        JSONArray array = new JSONArray();
        array.put("value");

        String xml = JsonXmlConverter.toXml(array, "");
        
        assertTrue(xml.contains("<item>value</item>"));
    }

    @Test
    public void testArrayOfObjects() throws Exception {
        JSONObject obj1 = new JSONObject();
        obj1.put("id", 1);
        obj1.put("name", "First");
        
        JSONObject obj2 = new JSONObject();
        obj2.put("id", 2);
        obj2.put("name", "Second");
        
        JSONArray array = new JSONArray();
        array.put(obj1);
        array.put(obj2);

        String xml = JsonXmlConverter.toXml(array, "record");
        
        assertTrue(xml.contains("<record>"));
        assertTrue(xml.contains("<id>1</id>"));
        assertTrue(xml.contains("<name>First</name>"));
        assertTrue(xml.contains("<id>2</id>"));
        assertTrue(xml.contains("<name>Second</name>"));
    }

    @Test
    public void testEmptyArray() throws Exception {
        JSONArray array = new JSONArray();

        String xml = JsonXmlConverter.toXml(array, "item");
        
        assertEquals(xml, "");
    }

    // ========== Type Handling Tests ==========

    @Test
    public void testStringValue() throws Exception {
        JSONObject json = new JSONObject();
        json.put("text", "Hello World");

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<text>Hello World</text>"));
    }

    @Test
    public void testNumberValue() throws Exception {
        JSONObject json = new JSONObject();
        json.put("count", 42);
        json.put("price", 19.99);

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<count>42</count>"));
        assertTrue(xml.contains("<price>19.99</price>"));
    }

    @Test
    public void testBooleanValue() throws Exception {
        JSONObject json = new JSONObject();
        json.put("active", true);
        json.put("deleted", false);

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<active>true</active>"));
        assertTrue(xml.contains("<deleted>false</deleted>"));
    }

    @Test
    public void testNullValue() throws Exception {
        JSONObject json = new JSONObject();
        json.put("nullable", JSONObject.NULL);

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<nullable>"));
        assertTrue(xml.contains("</nullable>"));
        // Null should produce empty element
    }

    // ========== XML Escaping Tests ==========

    @Test
    public void testXmlSpecialCharactersEscaped() throws Exception {
        JSONObject json = new JSONObject();
        json.put("text", "<tag> & \"quotes\" & 'apostrophe'");

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("&lt;tag&gt;"));
        assertTrue(xml.contains("&amp;"));
        assertTrue(xml.contains("&quot;"));
        assertTrue(xml.contains("&apos;"));
    }

    @Test
    public void testTagNameWithSpaces() throws Exception {
        JSONObject json = new JSONObject();
        json.put("invalid tag name", "value");

        String xml = JsonXmlConverter.toXml(json, "root");
        
        // Spaces should be replaced with underscores
        assertTrue(xml.contains("<invalid_tag_name>value</invalid_tag_name>"));
    }

    @Test
    public void testTagNameWithSpecialChars() throws Exception {
        JSONObject json = new JSONObject();
        json.put("tag@name#123", "value");

        String xml = JsonXmlConverter.toXml(json, "root");
        
        // Special chars replaced with underscores
        assertTrue(xml.contains("<tag_name_123>"));
        assertTrue(xml.contains("value"));
        assertTrue(xml.contains("</tag_name_123>"));
    }

    // ========== Complex Scenarios ==========

    @Test
    public void testObjectWithArray() throws Exception {
        JSONArray items = new JSONArray();
        items.put("item1");
        items.put("item2");
        
        JSONObject json = new JSONObject();
        json.put("name", "List");
        json.put("items", items);

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<name>List</name>"));
        assertTrue(xml.contains("<items>"));
        assertTrue(xml.contains("<item>item1</item>"));
        assertTrue(xml.contains("<item>item2</item>"));
        assertTrue(xml.contains("</items>"));
    }

    @Test
    public void testMixedTypes() throws Exception {
        JSONObject json = new JSONObject();
        json.put("string", "text");
        json.put("number", 123);
        json.put("boolean", true);
        json.put("object", new JSONObject().put("key", "value"));
        json.put("array", new JSONArray().put("a").put("b"));

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<string>text</string>"));
        assertTrue(xml.contains("<number>123</number>"));
        assertTrue(xml.contains("<boolean>true</boolean>"));
        assertTrue(xml.contains("<object>"));
        assertTrue(xml.contains("<key>value</key>"));
        assertTrue(xml.contains("<array>"));
    }

    @Test
    public void testEmptyString() throws Exception {
        JSONObject json = new JSONObject();
        json.put("empty", "");

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<empty></empty>"));
    }

    @Test
    public void testWhitespacePreserved() throws Exception {
        JSONObject json = new JSONObject();
        json.put("text", "  spaces  ");

        String xml = JsonXmlConverter.toXml(json, "root");
        
        assertTrue(xml.contains("<text>  spaces  </text>"));
    }

    // ========== Real-World Examples ==========

    @Test
    public void testBug61Example() throws Exception {
        // Exact example from Bug #61
        JSONObject inner = new JSONObject();
        inner.put("b", "c");
        
        JSONObject content = new JSONObject();
        content.put("content", inner);
        
        JSONObject json = new JSONObject();
        json.put("a", content);

        String xml = JsonXmlConverter.toXml(json, "root");
        
        // Bug #61 verification: "content" key must be present!
        assertTrue(xml.contains("<a>"), "Should have <a> tag");
        assertTrue(xml.contains("<content>"), "Bug #61: Should have <content> tag");
        assertTrue(xml.contains("<b>c</b>"), "Should have <b>c</b>");
        assertTrue(xml.contains("</content>"), "Bug #61: Should have </content> tag");
        assertTrue(xml.contains("</a>"), "Should have </a> tag");
        
        // Should NOT lose the "content" element
        assertFalse(xml.equals("<root><a><b>c</b></a></root>"), 
                   "Bug #61: 'content' key must not be stripped!");
    }

    @Test
    public void testAPIResponse() throws Exception {
        JSONObject user1 = new JSONObject();
        user1.put("id", 1);
        user1.put("name", "Alice");
        
        JSONObject user2 = new JSONObject();
        user2.put("id", 2);
        user2.put("name", "Bob");
        
        JSONArray users = new JSONArray();
        users.put(user1);
        users.put(user2);
        
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("users", users);

        String xml = JsonXmlConverter.toXml(response, "api-response");
        
        assertTrue(xml.contains("<status>success</status>"));
        assertTrue(xml.contains("<users>"));
        assertTrue(xml.contains("<item>"));
        assertTrue(xml.contains("<id>1</id>"));
        assertTrue(xml.contains("<name>Alice</name>"));
    }
}

