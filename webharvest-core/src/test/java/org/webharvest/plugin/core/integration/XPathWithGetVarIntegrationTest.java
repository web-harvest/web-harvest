package org.webharvest.plugin.core.integration;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * TDD Integration Test for XPath with <get var> source.
 * 
 * PROBLEM: User's config shows:
 *   <xpath expression="//h1"><get var="page"/></xpath>
 * Results in: heading = "<h1>Herman Melville - Moby-Dick</h1>"
 * Expected: heading = "Herman Melville - Moby-Dick" (text only)
 * 
 * This test verifies:
 * 1. XPath //h1 with <get var> source - what does it return?
 * 2. Does it return element or text content?
 * 3. Should user use //h1/text() or string(//h1) instead?
 */
public class XPathWithGetVarIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test
    public void testXPathElementVsText() throws Exception {
        System.out.println("=== TEST: XPath //h1 returns element or text? ===");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"page\"><![CDATA[<html><body><h1>Test Title</h1></body></html>]]></def>\n" +
            "    <def var=\"heading\">\n" +
            "        <xpath expression=\"//h1\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable heading = context.getVar("heading");
        
        assertNotNull(heading, "heading variable should exist");
        String headingStr = heading.toString();
        
        System.out.println("XPath //h1 result: [" + headingStr + "]");
        System.out.println("Contains <h1> tag: " + headingStr.contains("<h1>"));
        System.out.println("Contains 'Test Title': " + headingStr.contains("Test Title"));
        
        // DOCUMENT current behavior
        // If it contains tags, that's the problem user reported
        if (headingStr.contains("<h1>")) {
            System.out.println("❌ CONFIRMED: XPath //h1 returns ELEMENT, not text");
            System.out.println("   User should use //h1/text() or string(//h1)");
        } else {
            System.out.println("✅ XPath //h1 returns TEXT content (expected)");
        }
        
        // For now, just verify it's not empty
        assertTrue(headingStr.length() > 0, "Result should not be empty");
    }
    
    @Test
    public void testXPathTextNode() throws Exception {
        System.out.println("=== TEST: XPath //h1/text() ===");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"page\"><![CDATA[<html><body><h1>Test Title</h1></body></html>]]></def>\n" +
            "    <def var=\"heading\">\n" +
            "        <xpath expression=\"//h1/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable heading = context.getVar("heading");
        
        assertNotNull(heading);
        String headingStr = heading.toString();
        
        System.out.println("XPath //h1/text() result: [" + headingStr + "]");
        
        // Should return text content without tags
        assertEquals(headingStr.trim(), "Test Title", 
            "XPath //h1/text() should return text content only");
        assertFalse(headingStr.contains("<"), 
            "Should NOT contain XML tags");
    }
    
    @Test
    public void testXPathStringFunction() throws Exception {
        System.out.println("=== TEST: XPath string(//h1) ===");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"page\"><![CDATA[<html><body><h1>Test Title</h1></body></html>]]></def>\n" +
            "    <def var=\"heading\">\n" +
            "        <xpath expression=\"string(//h1)\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable heading = context.getVar("heading");
        
        assertNotNull(heading);
        String headingStr = heading.toString();
        
        System.out.println("XPath string(//h1) result: [" + headingStr + "]");
        
        // Should return text content without tags
        assertEquals(headingStr.trim(), "Test Title", 
            "XPath string(//h1) should return text content only");
        assertFalse(headingStr.contains("<"), 
            "Should NOT contain XML tags");
    }
}

