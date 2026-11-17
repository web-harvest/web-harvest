/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the following
   disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the following
   disclaimer in the documentation and/or other materials provided
   with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Integration tests for XML transformation and processing plugins:
 * XPath, XSLT, XQuery, HtmlToXml, JsonToXml and XmlToJson.
 *
 * Each test asserts data produced by the transformation, not merely that
 * an execution context was returned.
 *
 * @since 2.2.0
 */
public class XmlTransformationPluginsIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(XmlTransformationPluginsIntegrationTest.class);

    @Test(groups = {"integration", "xml-plugins"})
    public void testXPathPluginExtractsBothItemValues() throws Exception {
        LOG.info("Testing XPath extraction against the XML fixture");

        String xmlPath = getNewPluginExamplePath("07_xpath_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "xpath_output.txt");

        assertTrue(output.contains("Test Item"), "XPath output should contain the first selected item");
        assertTrue(output.contains("Another Item"), "XPath output should contain the second selected item");
        assertEquals(countOccurrences(output, "Item"), 2,
            "XPath should return exactly the two matching item text nodes");
    }

    @Test(groups = {"integration", "xml-plugins"})
    public void testHtmlToXmlPluginPreservesFixtureContent() throws Exception {
        LOG.info("Testing HTML-to-XML conversion against the fixture");

        String xmlPath = getNewPluginExamplePath("06_html_to_xml_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "html_to_xml_output.xml");

        assertTrue(output.contains("Test HTML"), "Converted XML should contain the heading text");
        assertTrue(output.contains("This is a test paragraph"),
            "Converted XML should contain the paragraph text");
    }

    @Test(groups = {"integration", "xml-plugins"})
    public void testJsonToXmlPluginPreservesObjectValues() throws Exception {
        LOG.info("Testing JSON-to-XML conversion against the fixture");

        String xmlPath = getNewPluginExamplePath("09_json_to_xml_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "json_to_xml_output.xml");

        assertTrue(output.contains("John"), "Converted XML should contain the name value");
        assertTrue(output.contains("30"), "Converted XML should contain the age value");
        assertTrue(output.contains("New York"), "Converted XML should contain the city value");
    }

    @Test(groups = {"integration", "xml-plugins"})
    public void testXmlToJsonPluginPreservesElementValues() throws Exception {
        LOG.info("Testing XML-to-JSON conversion against the fixture");

        String xmlPath = getNewPluginExamplePath("10_xml_to_json_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "xml_to_json_output.json");

        assertTrue(output.contains("Test Item"), "Converted JSON should contain the first item value");
        assertTrue(output.contains("Another Item"), "Converted JSON should contain the second item value");
    }

    @Test(groups = {"integration", "xml-plugins"})
    public void testXsltPluginAppliesStylesheet() throws Exception {
        LOG.info("Testing XSLT transformation against the fixture");

        String xmlPath = getNewPluginExamplePath("16_xslt_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "xslt_output.xml");

        assertTrue(output.contains("result"), "XSLT output should contain the result element");
        assertTrue(output.contains("Test Item"), "XSLT should select and emit the source item text");
    }

    @Test(groups = {"integration", "xml-plugins"})
    public void testXQueryPluginReturnsBothSelectedItems() throws Exception {
        LOG.info("Testing XQuery transformation against the fixture");

        String xmlPath = getNewPluginExamplePath("17_xquery_plugin_example.xml");
        String output = executeAndReadOutputFile(xmlPath, "xquery_output.xml");

        assertTrue(output.contains("Test Item"), "XQuery output should contain the first selected item");
        assertTrue(output.contains("Another Item"), "XQuery output should contain the second selected item");
        assertEquals(countOccurrences(output, "<result>"), 2,
            "XQuery should emit one result element for each selected item");
    }


    @Test(groups = {"integration", "xml-plugins", "xquery"})
    public void testXQueryEmptySequenceProducesEmptyResultAndContinues() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
            + "  <def var=\"emptyResult\">\n"
            + "    <xquery><xq-expression><![CDATA[()]]></xq-expression></xquery>\n"
            + "  </def>\n"
            + "  <def var=\"after\">continued</def>\n"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable result = context.getVar("emptyResult");

        assertTrue(result instanceof ListVariable,
            "A valid empty XQuery sequence should remain an empty ListVariable");
        assertTrue(((ListVariable) result).getList().isEmpty(),
            "An empty XQuery sequence should contain no result items");
        assertEquals(result.toString(), "",
            "An empty XQuery sequence should have an empty string representation");
        assertEquals(context.getVar("after").toString(), "continued",
            "The parent configuration should continue after an empty XQuery result");
    }


    @Test(groups = {"integration", "xml-plugins"})
    public void testXmlPluginWrapsNestedXmlAndFeedsXPath() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
            + "  <def var=\"xmlData\">\n"
            + "    <xml><![CDATA[<products><product id=\"1\"><name>Widget</name></product></products>]]></xml>\n"
            + "  </def>\n"
            + "  <def var=\"productName\">\n"
            + "    <xpath expression=\"//name/text()\"><get var=\"xmlData\"/></xpath>\n"
            + "  </def>\n"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable xmlResult = context.getVar("xmlData");
        assertNotNull(xmlResult, "The XML wrapper result should be stored in the variable");
        assertTrue(xmlResult instanceof NodeVariable,
            "The XML wrapper should return its body as a NodeVariable");
        assertTrue(xmlResult.toString().contains("<product id=\"1\">"),
            "The XML wrapper should preserve the nested product markup");
        assertEquals(context.getVar("productName").toString(), "Widget",
            "The wrapped XML should be consumable by a downstream XPath plugin");
    }

    private int countOccurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
