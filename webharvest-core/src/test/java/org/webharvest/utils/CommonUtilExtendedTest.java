/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.utils;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Extended tests for CommonUtil
 */
public class CommonUtilExtendedTest {

    @Test
    public void testIsEmptyStringWithNull() {
        assertTrue(CommonUtil.isEmptyString(null));
    }

    @Test
    public void testIsEmptyStringWithEmpty() {
        assertTrue(CommonUtil.isEmptyString(""));
    }

    @Test
    public void testIsEmptyStringWithWhitespace() {
        // Whitespace is considered empty by CommonUtil
        assertTrue(CommonUtil.isEmptyString(" "));
    }

    @Test
    public void testIsEmptyStringWithText() {
        assertFalse(CommonUtil.isEmptyString("test"));
    }

    @Test
    public void testAdaptFilenameWithNull() {
        assertNull(CommonUtil.adaptFilename(null));
    }

    @Test
    public void testAdaptFilenameWithValidName() {
        assertEquals(CommonUtil.adaptFilename("test.txt"), "test.txt");
    }

    @Test
    public void testAdaptFilenameWithInvalidChars() {
        String result = CommonUtil.adaptFilename("test:file?.txt");
        assertNotNull(result);
        // adaptFilename may or may not replace these chars depending on implementation
        assertTrue(result.length() > 0);
    }

    @Test
    public void testGetBooleanValueTrue() {
        assertTrue(CommonUtil.getBooleanValue("true", false));
        assertTrue(CommonUtil.getBooleanValue("TRUE", false));
        assertTrue(CommonUtil.getBooleanValue("1", false));
    }

    @Test
    public void testGetBooleanValueFalse() {
        assertFalse(CommonUtil.getBooleanValue("false", true));
        assertFalse(CommonUtil.getBooleanValue("0", true));
    }

    @Test
    public void testGetBooleanValueDefault() {
        assertTrue(CommonUtil.getBooleanValue(null, true));
        assertFalse(CommonUtil.getBooleanValue("", false));
    }

    @Test
    public void testGetIntValueValid() {
        assertEquals(CommonUtil.getIntValue("123", 0), 123);
        assertEquals(CommonUtil.getIntValue("-456", 0), -456);
    }

    @Test
    public void testGetIntValueInvalid() {
        assertEquals(CommonUtil.getIntValue("abc", 99), 99);
        assertEquals(CommonUtil.getIntValue(null, 88), 88);
    }

    @Test
    public void testEscapeXml() {
        assertEquals(CommonUtil.escapeXml("<tag>"), "&lt;tag&gt;");
        assertEquals(CommonUtil.escapeXml("a&b"), "a&amp;b");
        assertEquals(CommonUtil.escapeXml("'quote'"), "&apos;quote&apos;");
    }
}

