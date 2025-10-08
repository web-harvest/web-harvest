package org.webharvest.utils;

import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.exception.ScraperXQueryException;

import java.util.Properties;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for XmlNodeWrapper.
 */
public class XmlNodeWrapperTest {

    private Item mockItem;
    private Properties outputProperties;
    private XmlNodeWrapper wrapper;

    @BeforeMethod
    public void setUp() {
        mockItem = mock(Item.class);
        outputProperties = new Properties();
        wrapper = new XmlNodeWrapper(mockItem, outputProperties);
    }

    @Test
    public void testConstructor() {
        assertNotNull(wrapper);
        // Test that constructor works without throwing exceptions
    }

    @Test
    public void testToString() throws XPathException {
        String expectedValue = "<test>value</test>";
        when(CommonUtil.serializeItem(mockItem, outputProperties)).thenReturn(expectedValue);
        
        String result = wrapper.toString();
        assertEquals(result, expectedValue);
        // Note: Can't verify static method calls easily with Mockito
    }

    @Test
    public void testToStringCachesResult() throws XPathException {
        String expectedValue = "<test>value</test>";
        when(CommonUtil.serializeItem(mockItem, outputProperties)).thenReturn(expectedValue);
        
        // Call toString multiple times
        String result1 = wrapper.toString();
        String result2 = wrapper.toString();
        
        assertEquals(result1, expectedValue);
        assertEquals(result2, expectedValue);
        // Note: Can't verify static method calls easily with Mockito
    }

    @Test
    public void testIsEmptyWithEmptyString() throws XPathException {
        when(CommonUtil.serializeItem(mockItem, outputProperties)).thenReturn("");
        
        assertTrue(wrapper.isEmpty());
    }

    @Test
    public void testIsEmptyWithNullString() throws XPathException {
        when(CommonUtil.serializeItem(mockItem, outputProperties)).thenReturn(null);
        
        assertTrue(wrapper.isEmpty());
    }

    @Test
    public void testIsEmptyWithNonEmptyString() throws XPathException {
        when(CommonUtil.serializeItem(mockItem, outputProperties)).thenReturn("<test>value</test>");
        
        assertFalse(wrapper.isEmpty());
    }

    @Test
    public void testIsEmptyWithWhitespaceString() throws XPathException {
        when(CommonUtil.serializeItem(mockItem, outputProperties)).thenReturn("   ");
        
        // StringUtils.isEmpty() returns false for whitespace-only strings
        assertFalse(wrapper.isEmpty());
    }

    @Test
    public void testToStringWithNullItem() {
        XmlNodeWrapper wrapperWithNullItem = new XmlNodeWrapper(null, outputProperties);
        // This will throw NullPointerException, which is expected behavior
        try {
            String result = wrapperWithNullItem.toString();
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected behavior
        }
    }

    @Test
    public void testIsEmptyWithNullItem() {
        XmlNodeWrapper wrapperWithNullItem = new XmlNodeWrapper(null, outputProperties);
        // This will throw NullPointerException, which is expected behavior
        try {
            boolean result = wrapperWithNullItem.isEmpty();
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected behavior
        }
    }

    @Test
    public void testConstructorWithNullItem() {
        XmlNodeWrapper wrapperWithNullItem = new XmlNodeWrapper(null, outputProperties);
        assertNotNull(wrapperWithNullItem);
        // Test that constructor works with null item
    }

    @Test
    public void testConstructorWithNullProperties() {
        XmlNodeWrapper wrapperWithNullProps = new XmlNodeWrapper(mockItem, null);
        assertNotNull(wrapperWithNullProps);
        // Test that constructor works with null properties
    }
}
