package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration tests for IDE example configurations.
 * These tests verify that the XML examples shipped with the IDE work correctly.
 */
public class IDEExamplesIntegrationTest extends XmlBasedIntegrationTestBase {

    /**
     * Test Example 1: Simple HTTP + XPath
     * Tests httpbin.org/html with XPath text extraction
     */
    @Test(groups = "network")
    public void testHttpXPathExample() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <!-- Simple HTTP + XPath Example -->\n" +
            "    <!-- Fast test: httpbin.org (~3 seconds) -->\n" +
            "    \n" +
            "    <def var=\"url\">https://httpbin.org/html</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"title\">\n" +
            "        <xpath expression=\"//title/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"heading\">\n" +
            "        <xpath expression=\"//h1/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <file action=\"write\" path=\"target/test-output/http_xpath_result.txt\">\n" +
            "        <template>Title: ${title}\n" +
            "Heading: ${heading}</template>\n" +
            "    </file>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify variables
        Variable url = context.getVar("url");
        assertNotNull(url, "url variable should be set");
        assertEquals(url.toString(), "https://httpbin.org/html");
        
        Variable html = context.getVar("html");
        assertNotNull(html, "html variable should be set");
        assertTrue(html.toString().contains("Herman Melville"), "HTML should contain content");
        
        Variable page = context.getVar("page");
        assertNotNull(page, "page variable should be set");
        
        Variable title = context.getVar("title");
        assertNotNull(title, "title variable should be set");
        // Note: httpbin.org/html doesn't have <title> tag, so it's empty
        
        Variable heading = context.getVar("heading");
        assertNotNull(heading, "heading variable should be set");
        String headingText = heading.toString().trim();
        
        // CRITICAL ASSERTION: heading should contain ONLY TEXT, not XML tags
        assertFalse(headingText.contains("<h1>"), 
                   "heading should NOT contain <h1> tag, got: " + headingText);
        assertFalse(headingText.contains("</h1>"), 
                   "heading should NOT contain </h1> tag, got: " + headingText);
        assertTrue(headingText.contains("Herman Melville") || headingText.contains("Moby-Dick"), 
                   "heading should contain text content, got: " + headingText);
        
        // Verify heading is approximately 27 characters (text only)
        assertTrue(headingText.length() > 20 && headingText.length() < 40,
                   "heading length should be around 27 chars (text only), got: " + headingText.length());
    }
    
    /**
     * Test Example 2: Quotes Scraping
     * Tests quotes.toscrape.com with XPath text extraction
     */
    @Test(groups = "network")
    public void testQuotesScrapingExample() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"url\">https://quotes.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <!-- Extract all quotes (use /text() to get text content) -->\n" +
            "    <def var=\"quotes\">\n" +
            "        <xpath expression=\"//div[@class='quote']//span[@class='text']/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <!-- Extract all authors (use /text() to get text content) -->\n" +
            "    <def var=\"authors\">\n" +
            "        <xpath expression=\"//div[@class='quote']//small[@class='author']/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        Variable quotes = context.getVar("quotes");
        assertNotNull(quotes, "quotes variable should be set");
        String quotesText = quotes.toString();
        
        // Quotes should NOT contain HTML tags
        assertFalse(quotesText.contains("<span"), 
                   "quotes should NOT contain <span> tags");
        assertFalse(quotesText.contains("class="), 
                   "quotes should NOT contain HTML attributes");
        
        Variable authors = context.getVar("authors");
        assertNotNull(authors, "authors variable should be set");
        String authorsText = authors.toString();
        
        // Authors should NOT contain HTML tags
        assertFalse(authorsText.contains("<small"), 
                   "authors should NOT contain <small> tags");
        assertFalse(authorsText.contains("class="), 
                   "authors should NOT contain HTML attributes");
    }
    
    /**
     * Test Example 3: JSON API
     * Tests JSON to XML conversion with XPath
     */
    @Test(groups = "network")
    public void testJsonApiExample() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"jsonData\">\n" +
            "        <http url=\"https://httpbin.org/json\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"xmlData\">\n" +
            "        <json-to-xml>\n" +
            "            <get var=\"jsonData\"/>\n" +
            "        </json-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"userName\">\n" +
            "        <xpath expression=\"//name/text()\">\n" +
            "            <get var=\"xmlData\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"userEmail\">\n" +
            "        <xpath expression=\"//email/text()\">\n" +
            "            <get var=\"xmlData\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        Variable xmlData = context.getVar("xmlData");
        assertNotNull(xmlData, "xmlData variable should be set");
        
        Variable userName = context.getVar("userName");
        assertNotNull(userName, "userName variable should be set");
        
        // userName should be TEXT, not XML element
        String userNameText = userName.toString();
        assertFalse(userNameText.contains("<name>"), 
                   "userName should NOT contain <name> tag");
        
        Variable userEmail = context.getVar("userEmail");
        assertNotNull(userEmail, "userEmail variable should be set");
        
        // userEmail should be TEXT, not XML element
        String userEmailText = userEmail.toString();
        assertFalse(userEmailText.contains("<email>"), 
                   "userEmail should NOT contain <email> tag");
    }
}

