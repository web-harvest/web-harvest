package org.webharvest.runtime.variables;

import org.testng.annotations.Test;
import org.webharvest.exception.VariableException;
import org.webharvest.utils.XmlNodeWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import static org.testng.Assert.*;

/**
 * Test for NodeVariable.
 */
public class NodeVariableTest {

    @Test
    public void testConstructorWithNull() {
        NodeVariable var = new NodeVariable(null);
        assertNotNull(var);
        assertNull(var.getWrappedObject());
    }

    @Test
    public void testConstructorWithString() {
        NodeVariable var = new NodeVariable("test data");
        assertNotNull(var);
        assertEquals(var.getWrappedObject(), "test data");
    }

    @Test
    public void testConstructorWithByteArray() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        NodeVariable var = new NodeVariable(data);
        assertNotNull(var);
        assertSame(var.getWrappedObject(), data);
    }

    @Test
    public void testToStringWithNull() {
        NodeVariable var = new NodeVariable(null);
        assertEquals(var.toString(), "");
        assertTrue(var.toString().isEmpty());
    }

    @Test
    public void testToStringWithString() {
        NodeVariable var = new NodeVariable("test data");
        assertEquals(var.toString(), "test data");
    }

    @Test
    public void testToStringWithByteArray() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        NodeVariable var = new NodeVariable(data);
        assertEquals(var.toString(), "test data");
    }

    @Test
    public void testDefaultConversionsUseUtf8ForNonAsciiText() {
        String text = "Zażółć gęślą jaźń – €";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);

        assertEquals(new NodeVariable(utf8).toString(), text);
        assertEquals(new NodeVariable(text).toBinary(), utf8);
        assertEquals(new NodeVariable(text).toBinary(null), utf8);
        assertEquals(new NodeVariable(utf8).toString(null), text);
    }

    @Test
    public void testExplicitLegacyCharsetRemainsSupported() {
        String text = "café";
        byte[] latin1 = text.getBytes(StandardCharsets.ISO_8859_1);

        assertEquals(new NodeVariable(latin1).toString("ISO-8859-1"), text);
        assertEquals(new NodeVariable(text).toBinary("ISO-8859-1"), latin1);
    }

    @Test
    public void testToStringWithObject() {
        Object obj = new Object() {
            @Override
            public String toString() {
                return "custom object";
            }
        };
        NodeVariable var = new NodeVariable(obj);
        assertEquals(var.toString(), "custom object");
    }

    @Test
    public void testToStringWithCharset() {
        NodeVariable var = new NodeVariable("test data");
        assertEquals(var.toString("UTF-8"), "test data");
        assertEquals(var.toString("ISO-8859-1"), "test data");
    }

    @Test
    public void testToStringWithCharsetAndByteArray() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        NodeVariable var = new NodeVariable(data);
        assertEquals(var.toString("UTF-8"), "test data");
    }

    @Test(expectedExceptions = VariableException.class)
    public void testToStringWithInvalidCharset() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        NodeVariable var = new NodeVariable(data);
        var.toString("INVALID-CHARSET");
    }

    @Test
    public void testToStringWithCharsetAndNull() {
        NodeVariable var = new NodeVariable(null);
        assertEquals(var.toString("UTF-8"), "");
    }

    @Test
    public void testToBinary() {
        NodeVariable var = new NodeVariable("test data");
        byte[] result = var.toBinary();
        assertNotNull(result);
        assertEquals(new String(result, StandardCharsets.UTF_8), "test data");
    }

    @Test
    public void testToBinaryWithNull() {
        NodeVariable var = new NodeVariable(null);
        byte[] result = var.toBinary();
        assertNotNull(result);
        assertEquals(result.length, 0);
    }

    @Test
    public void testToBinaryWithByteArray() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        NodeVariable var = new NodeVariable(data);
        byte[] result = var.toBinary();
        assertSame(result, data);
    }

    @Test
    public void testToBinaryWithCharset() {
        NodeVariable var = new NodeVariable("test data");
        byte[] result = var.toBinary("UTF-8");
        assertNotNull(result);
        assertEquals(new String(result, StandardCharsets.UTF_8), "test data");
    }

    @Test
    public void testToBinaryWithCharsetAndNull() {
        NodeVariable var = new NodeVariable(null);
        byte[] result = var.toBinary("UTF-8");
        assertNotNull(result);
        assertEquals(result.length, 0);
    }

    @Test
    public void testToBinaryWithCharsetAndByteArray() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        NodeVariable var = new NodeVariable(data);
        byte[] result = var.toBinary("UTF-8");
        assertSame(result, data);
    }

    @Test(expectedExceptions = VariableException.class)
    public void testToBinaryWithInvalidCharset() {
        NodeVariable var = new NodeVariable("test data");
        var.toBinary("INVALID-CHARSET");
    }

    @Test
    public void testIsEmptyWithNull() {
        NodeVariable var = new NodeVariable(null);
        assertTrue(var.isEmpty());
    }

    @Test
    public void testIsEmptyWithString() {
        NodeVariable var1 = new NodeVariable("test data");
        assertFalse(var1.isEmpty());

        NodeVariable var2 = new NodeVariable("");
        assertTrue(var2.isEmpty());
    }

    @Test
    public void testIsEmptyWithEmptyIterator() {
        NodeVariable var = new NodeVariable(Collections.emptyList().iterator());
        assertTrue(var.isEmpty());
    }

    @Test
    public void testIsEmptyWithNonEmptyIterator() {
        NodeVariable var = new NodeVariable(Arrays.asList("item").iterator());
        assertFalse(var.isEmpty());
    }

    @Test
    public void testIsEmptyWithEmptyIterable() {
        NodeVariable var = new NodeVariable(Collections.emptyList());
        assertTrue(var.isEmpty());
    }

    @Test
    public void testIsEmptyWithNonEmptyIterable() {
        NodeVariable var = new NodeVariable(Arrays.asList("item"));
        assertFalse(var.isEmpty());
    }

    @Test
    public void testToList() {
        NodeVariable var = new NodeVariable("test data");
        List<Variable> result = var.toList();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testToListWithNull() {
        NodeVariable var = new NodeVariable(null);
        List<Variable> result = var.toList();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToListWithEmptyData() {
        NodeVariable var = new NodeVariable("");
        List<Variable> result = var.toList();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToIterator() {
        NodeVariable var = new NodeVariable("test data");
        Iterator<Variable> result = var.toIterator();
        assertNotNull(result);
        assertTrue(result.hasNext());
    }

    @Test
    public void testToIteratorWithNull() {
        NodeVariable var = new NodeVariable(null);
        Iterator<Variable> result = var.toIterator();
        assertNotNull(result);
        assertFalse(result.hasNext());
    }

    @Test
    public void testToIteratorWithArray() {
        Object[] array = {"item1", "item2", "item3"};
        NodeVariable var = new NodeVariable(array);
        Iterator<Variable> result = var.toIterator();
        assertNotNull(result);
        assertTrue(result.hasNext());
        
        int count = 0;
        while (result.hasNext()) {
            Variable next = result.next();
            assertNotNull(next);
            count++;
        }
        assertEquals(count, 3);
    }

    @Test
    public void testToIteratorWithIterable() {
        List<String> list = Arrays.asList("item1", "item2");
        NodeVariable var = new NodeVariable(list);
        Iterator<Variable> result = var.toIterator();
        assertNotNull(result);
        assertTrue(result.hasNext());
        
        int count = 0;
        while (result.hasNext()) {
            Variable next = result.next();
            assertNotNull(next);
            count++;
        }
        assertEquals(count, 2);
    }

    @Test
    public void testGetWrappedObject() {
        String data = "test data";
        NodeVariable var = new NodeVariable(data);
        assertSame(var.getWrappedObject(), data);
    }

    @Test
    public void testGetWrappedObjectWithNull() {
        NodeVariable var = new NodeVariable(null);
        assertNull(var.getWrappedObject());
    }

    @Test
    public void testConsistentBehavior() {
        String data = "test data";
        NodeVariable var1 = new NodeVariable(data);
        NodeVariable var2 = new NodeVariable(data);
        
        assertEquals(var1.toString(), var2.toString());
        assertEquals(var1.isEmpty(), var2.isEmpty());
        assertEquals(var1.getWrappedObject(), var2.getWrappedObject());
    }
}

























