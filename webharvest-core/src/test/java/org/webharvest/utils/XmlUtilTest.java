package org.webharvest.utils;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
// REMOVED in v2.2.x - RuntimeConfig no longer exists
// import org.webharvest.runtime.RuntimeConfig;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import java.io.StringReader;
import java.io.StringWriter;

import static org.testng.Assert.*;

public class XmlUtilTest {

    private static final String VALID_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>value</item></root>";
    private static final String INVALID_XML = "<root><item>value</item>";
    private static final String XML_WITH_NAMESPACE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"http://example.com\"><item>value</item></root>";

    @Test
    public void testParseValidXml() {
        InputSource inputSource = new InputSource(new StringReader(VALID_XML));
        Document doc = XmlUtil.parse(inputSource);
        
        assertNotNull(doc);
        assertEquals(doc.getDocumentElement().getNodeName(), "root");
        assertEquals(doc.getDocumentElement().getFirstChild().getNodeName(), "item");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testParseInvalidXml() {
        InputSource inputSource = new InputSource(new StringReader(INVALID_XML));
        XmlUtil.parse(inputSource);
    }

    @Test
    public void testPrettyPrintXmlString() {
        String result = XmlUtil.prettyPrintXml(VALID_XML);
        
        assertNotNull(result);
        assertTrue(result.contains("<root>"));
        assertTrue(result.contains("<item>value</item>"));
        assertTrue(result.contains("</root>"));
    }

    @Test
    public void testPrettyPrintXmlDocument() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(VALID_XML)));
        StringWriter writer = new StringWriter();
        
        XmlUtil.prettyPrintXml(doc, writer);
        String result = writer.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("<root>"));
        assertTrue(result.contains("<item>value</item>"));
        assertTrue(result.contains("</root>"));
    }

    @Test
    public void testEvaluateXPathOnDocument() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(VALID_XML)));
        
        String result = XmlUtil.evaluateXPath("//item/text()", doc);
        assertEquals(result, "value");
    }

    @Test
    public void testGetSAXParserFactoryNonValidatingNonNamespaceAware() {
        SAXParserFactory factory = XmlUtil.getSAXParserFactory(false, false);
        
        assertNotNull(factory);
        assertFalse(factory.isValidating());
        assertFalse(factory.isNamespaceAware());
    }

    @Test
    public void testGetSAXParserFactoryNonValidatingNamespaceAware() {
        SAXParserFactory factory = XmlUtil.getSAXParserFactory(false, true);
        
        assertNotNull(factory);
        assertFalse(factory.isValidating());
        assertTrue(factory.isNamespaceAware());
    }

    @Test
    public void testGetSAXParserFactoryValidatingNonNamespaceAware() {
        // Note: Based on the implementation, validating parsers are commented out
        // so this should return null
        SAXParserFactory factory = XmlUtil.getSAXParserFactory(true, false);
        assertNull(factory);
    }

    @Test
    public void testGetSAXParserFactoryValidatingNamespaceAware() {
        // Note: Based on the implementation, validating parsers are commented out
        // so this should return null
        SAXParserFactory factory = XmlUtil.getSAXParserFactory(true, true);
        assertNull(factory);
    }

    @Test
    public void testRefreshSchemaResolver() {
        // This method should not throw an exception
        // We can't test much more without complex mocking
        XmlUtil.refreshSchemaResolver();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testPrettyPrintXmlWithInvalidXml() {
        XmlUtil.prettyPrintXml(INVALID_XML);
    }

    @Test
    public void testParseWithNamespaceXml() {
        InputSource inputSource = new InputSource(new StringReader(XML_WITH_NAMESPACE));
        Document doc = XmlUtil.parse(inputSource);
        
        assertNotNull(doc);
        assertEquals(doc.getDocumentElement().getNodeName(), "root");
        // Note: The default DocumentBuilder may not preserve namespace URIs unless namespace awareness is enabled
        // For this test, we'll just check that the document is parsed without error
        assertNotNull(doc.getDocumentElement());
    }

    @Test
    public void testEvaluateXPathWithNonExistentPath() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(VALID_XML)));
        
        String result = XmlUtil.evaluateXPath("//nonexistent/text()", doc);
        assertEquals(result, "");
    }

    @Test
    public void testPrettyPrintXmlWithEmptyDocument() throws Exception {
        String emptyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
        String result = XmlUtil.prettyPrintXml(emptyXml);
        
        assertNotNull(result);
        // The pretty printed XML might format as self-closing tag: <root/>
        assertTrue(result.contains("root") && (result.contains("<root") || result.contains("<root/>")));
    }
}
