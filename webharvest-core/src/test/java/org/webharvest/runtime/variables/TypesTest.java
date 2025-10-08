package org.webharvest.runtime.variables;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test for Types utility class.
 */
public class TypesTest {

    @Test
    public void testTypeConstants() {
        assertEquals(Types.TYPE_EMPTY, "empty");
        assertEquals(Types.TYPE_TEXT, "text");
        assertEquals(Types.TYPE_XML, "xml");
        assertEquals(Types.TYPE_BINARY, "binary");
        assertEquals(Types.TYPE_LIST, "list");
        assertEquals(Types.TYPE_DEFAULT, "text");
    }

    @Test
    public void testTypeConstantsAreNotNull() {
        assertNotNull(Types.TYPE_EMPTY);
        assertNotNull(Types.TYPE_TEXT);
        assertNotNull(Types.TYPE_XML);
        assertNotNull(Types.TYPE_BINARY);
        assertNotNull(Types.TYPE_LIST);
        assertNotNull(Types.TYPE_DEFAULT);
    }

    @Test
    public void testTypeConstantsAreNotEqual() {
        assertFalse(Types.TYPE_EMPTY.equals(Types.TYPE_TEXT));
        assertFalse(Types.TYPE_TEXT.equals(Types.TYPE_XML));
        assertFalse(Types.TYPE_XML.equals(Types.TYPE_BINARY));
        assertFalse(Types.TYPE_BINARY.equals(Types.TYPE_LIST));
        assertFalse(Types.TYPE_LIST.equals(Types.TYPE_EMPTY));
    }

    @Test
    public void testDefaultTypeIsText() {
        assertEquals(Types.TYPE_DEFAULT, Types.TYPE_TEXT);
    }
}
