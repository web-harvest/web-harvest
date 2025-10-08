package org.webharvest.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import static org.testng.Assert.*;

/**
 * Test for XmlValidator.
 */
public class XmlValidatorTest {

    private XmlValidator validator;

    @BeforeMethod
    public void setUp() {
        validator = new XmlValidator();
    }

    @Test
    public void testParseValidXml() {
        String validXml = "<?xml version=\"1.0\"?><root><child>value</child></root>";
        InputSource inputSource = new InputSource(new StringReader(validXml));
        
        boolean result = validator.parse(inputSource);
        
        assertTrue(result);
        assertNull(validator.getException());
        assertEquals(validator.getLineNumber(), 0);
        assertEquals(validator.getColumnNumber(), 0);
    }

    @Test
    public void testParseInvalidXml() {
        String invalidXml = "<?xml version=\"1.0\"?><root><child>value</child>"; // Missing closing tag
        InputSource inputSource = new InputSource(new StringReader(invalidXml));
        
        boolean result = validator.parse(inputSource);
        
        assertFalse(result);
        assertNotNull(validator.getException());
        assertTrue(validator.getException() instanceof SAXParseException);
    }

    @Test
    public void testParseMalformedXml() {
        String malformedXml = "<root><child>value</child></root>"; // Missing XML declaration
        InputSource inputSource = new InputSource(new StringReader(malformedXml));
        
        boolean result = validator.parse(inputSource);
        
        // This might still be valid XML without declaration
        assertTrue(result);
        assertNull(validator.getException());
    }

    @Test
    public void testParseEmptyXml() {
        String emptyXml = "";
        InputSource inputSource = new InputSource(new StringReader(emptyXml));
        
        boolean result = validator.parse(inputSource);
        
        assertFalse(result);
        assertNotNull(validator.getException());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseNullInputSource() {
        validator.parse(null);
    }

    @Test
    public void testParseWithIOException() {
        // Create an input source that will cause IOException
        InputSource inputSource = new InputSource(new ByteArrayInputStream(new byte[0]));
        inputSource.setSystemId("nonexistent://file.xml");
        
        boolean result = validator.parse(inputSource);
        
        // The result might be true or false depending on the XML parser behavior
        assertNotNull(validator.getException());
        // The exception might be IOException or another type
    }

    @Test
    public void testGetExceptionInitiallyNull() {
        assertNull(validator.getException());
    }

    @Test
    public void testGetLineNumberInitiallyZero() {
        assertEquals(validator.getLineNumber(), 0);
    }

    @Test
    public void testGetColumnNumberInitiallyZero() {
        assertEquals(validator.getColumnNumber(), 0);
    }

    @Test
    public void testParseWithSAXParseExceptionSetsLineAndColumn() {
        String invalidXml = "<?xml version=\"1.0\"?>\n<root>\n<child>value</child>"; // Missing closing tag
        InputSource inputSource = new InputSource(new StringReader(invalidXml));
        
        boolean result = validator.parse(inputSource);
        
        assertFalse(result);
        assertNotNull(validator.getException());
        assertTrue(validator.getException() instanceof SAXParseException);
        
        // Line and column numbers should be set for SAXParseException
        assertTrue(validator.getLineNumber() > 0);
        assertTrue(validator.getColumnNumber() >= 0);
    }

    @Test
    public void testParseComplexValidXml() {
        String complexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "  <child1>value1</child1>\n" +
                "  <child2 attr=\"value\">value2</child2>\n" +
                "  <child3>\n" +
                "    <nested>nested value</nested>\n" +
                "  </child3>\n" +
                "</root>";
        InputSource inputSource = new InputSource(new StringReader(complexXml));
        
        boolean result = validator.parse(inputSource);
        
        assertTrue(result);
        assertNull(validator.getException());
        assertEquals(validator.getLineNumber(), 0);
        assertEquals(validator.getColumnNumber(), 0);
    }

    @Test
    public void testParseWithSpecialCharacters() {
        String xmlWithSpecialChars = "<?xml version=\"1.0\"?><root><child>&lt;&gt;&amp;&quot;&apos;</child></root>";
        InputSource inputSource = new InputSource(new StringReader(xmlWithSpecialChars));
        
        boolean result = validator.parse(inputSource);
        
        assertTrue(result);
        assertNull(validator.getException());
    }
}
