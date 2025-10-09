package org.webharvest.runtime.variables;

import org.apache.commons.collections.iterators.EmptyIterator;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Test for EmptyVariable.
 */
public class EmptyVariableTest {

    @Test
    public void testSingletonInstance() {
        assertNotNull(EmptyVariable.INSTANCE);
        assertSame(EmptyVariable.INSTANCE, EmptyVariable.INSTANCE);
    }

    @Test
    public void testToString() {
        assertEquals(EmptyVariable.INSTANCE.toString(), "");
        assertTrue(EmptyVariable.INSTANCE.toString().isEmpty());
    }

    @Test
    public void testToStringWithCharset() {
        assertEquals(EmptyVariable.INSTANCE.toString("UTF-8"), "");
        assertEquals(EmptyVariable.INSTANCE.toString("ISO-8859-1"), "");
        assertEquals(EmptyVariable.INSTANCE.toString(null), "");
        assertTrue(EmptyVariable.INSTANCE.toString("UTF-8").isEmpty());
    }

    @Test
    public void testToBinary() {
        byte[] result = EmptyVariable.INSTANCE.toBinary();
        assertNotNull(result);
        assertEquals(result.length, 0);
    }

    @Test
    public void testToBinaryWithCharset() {
        byte[] result1 = EmptyVariable.INSTANCE.toBinary("UTF-8");
        byte[] result2 = EmptyVariable.INSTANCE.toBinary("ISO-8859-1");
        byte[] result3 = EmptyVariable.INSTANCE.toBinary(null);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(result1.length, 0);
        assertEquals(result2.length, 0);
        assertEquals(result3.length, 0);
        
        // Test that the same empty array is returned
        assertSame(result1, result2);
        assertSame(result2, result3);
    }

    @Test
    public void testToList() {
        List<Variable> result = EmptyVariable.INSTANCE.toList();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(result.size(), 0);
    }

    @Test
    public void testIsEmpty() {
        assertTrue(EmptyVariable.INSTANCE.isEmpty());
    }

    @Test
    public void testGetWrappedObject() {
        Object result = EmptyVariable.INSTANCE.getWrappedObject();
        assertNotNull(result);
        assertEquals(result, "");
        assertTrue(result.toString().isEmpty());
    }

    @Test
    public void testToIterator() {
        Iterator<?> result = EmptyVariable.INSTANCE.toIterator();
        assertNotNull(result);
        assertSame(result, EmptyIterator.INSTANCE);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConsistentBehavior() {
        EmptyVariable var1 = EmptyVariable.INSTANCE;
        EmptyVariable var2 = EmptyVariable.INSTANCE;
        
        assertSame(var1, var2);
        assertEquals(var1.toString(), var2.toString());
        assertEquals(var1.toBinary().length, var2.toBinary().length);
        assertEquals(var1.isEmpty(), var2.isEmpty());
        assertEquals(var1.toList().size(), var2.toList().size());
    }

    @Test
    public void testImmutability() {
        EmptyVariable instance1 = EmptyVariable.INSTANCE;
        EmptyVariable instance2 = EmptyVariable.INSTANCE;
        
        // Should always return the same singleton instance
        assertSame(instance1, instance2);
        
        // All operations should return consistent empty results
        assertEquals(instance1.toString(), "");
        assertEquals(instance1.toBinary().length, 0);
        assertTrue(instance1.isEmpty());
        assertEquals(instance1.toList().size(), 0);
        assertFalse(instance1.toIterator().hasNext());
    }
}




















