package org.webharvest.runtime.variables;

import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

/**
 * Test for Variable abstract class.
 * Tests abstract methods through concrete implementations.
 */
public class VariableTest {

    @Test
    public void testToBooleanWithNullObject() {
        Variable variable = new NodeVariable(null);
        assertFalse(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithBooleanTrue() {
        Variable variable = new NodeVariable(Boolean.TRUE);
        assertTrue(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithBooleanFalse() {
        Variable variable = new NodeVariable(Boolean.FALSE);
        assertFalse(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithStringTrue() {
        Variable variable = new NodeVariable("true");
        assertTrue(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithStringTrueCaseInsensitive() {
        Variable variable = new NodeVariable("TRUE");
        assertTrue(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithStringYes() {
        Variable variable = new NodeVariable("yes");
        assertTrue(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithStringYesCaseInsensitive() {
        Variable variable = new NodeVariable("YES");
        assertTrue(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithStringOne() {
        Variable variable = new NodeVariable("1");
        assertTrue(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithStringFalse() {
        Variable variable = new NodeVariable("false");
        assertFalse(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithStringRandom() {
        Variable variable = new NodeVariable("random");
        assertFalse(variable.toBoolean());
    }

    @Test
    public void testToBooleanWithWhitespace() {
        Variable variable = new NodeVariable("  true  ");
        assertTrue(variable.toBoolean());
    }

    @Test
    public void testToIntWithNullObject() {
        Variable variable = new NodeVariable(null);
        assertEquals(variable.toInt(), 0);
    }

    @Test
    public void testToIntWithInteger() {
        Variable variable = new NodeVariable(Integer.valueOf(42));
        assertEquals(variable.toInt(), 42);
    }

    @Test
    public void testToIntWithLong() {
        Variable variable = new NodeVariable(Long.valueOf(123L));
        assertEquals(variable.toInt(), 123);
    }

    @Test
    public void testToIntWithDouble() {
        Variable variable = new NodeVariable(Double.valueOf(3.14));
        assertEquals(variable.toInt(), 3);
    }

    @Test
    public void testToIntWithBooleanTrue() {
        Variable variable = new NodeVariable(Boolean.TRUE);
        assertEquals(variable.toInt(), 1);
    }

    @Test
    public void testToIntWithBooleanFalse() {
        Variable variable = new NodeVariable(Boolean.FALSE);
        assertEquals(variable.toInt(), 0);
    }

    @Test
    public void testToIntWithString() {
        Variable variable = new NodeVariable("42");
        assertEquals(variable.toInt(), 42);
    }

    @Test
    public void testToIntWithStringWhitespace() {
        Variable variable = new NodeVariable("  42  ");
        assertEquals(variable.toInt(), 42);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testToIntWithInvalidString() {
        Variable variable = new NodeVariable("not a number");
        variable.toInt();
    }

    @Test
    public void testToLongWithNullObject() {
        Variable variable = new NodeVariable(null);
        assertEquals(variable.toLong(), 0L);
    }

    @Test
    public void testToLongWithLong() {
        Variable variable = new NodeVariable(Long.valueOf(123456789L));
        assertEquals(variable.toLong(), 123456789L);
    }

    @Test
    public void testToLongWithInteger() {
        Variable variable = new NodeVariable(Integer.valueOf(42));
        assertEquals(variable.toLong(), 42L);
    }

    @Test
    public void testToLongWithBooleanTrue() {
        Variable variable = new NodeVariable(Boolean.TRUE);
        assertEquals(variable.toLong(), 1L);
    }

    @Test
    public void testToLongWithBooleanFalse() {
        Variable variable = new NodeVariable(Boolean.FALSE);
        assertEquals(variable.toLong(), 0L);
    }

    @Test
    public void testToLongWithString() {
        Variable variable = new NodeVariable("123456789");
        assertEquals(variable.toLong(), 123456789L);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testToLongWithInvalidString() {
        Variable variable = new NodeVariable("not a number");
        variable.toLong();
    }

    @Test
    public void testToDoubleWithNullObject() {
        Variable variable = new NodeVariable(null);
        assertEquals(variable.toDouble(), 0.0, 0.001);
    }

    @Test
    public void testToDoubleWithDouble() {
        Variable variable = new NodeVariable(Double.valueOf(3.14));
        assertEquals(variable.toDouble(), 3.14, 0.001);
    }

    @Test
    public void testToDoubleWithInteger() {
        Variable variable = new NodeVariable(Integer.valueOf(42));
        assertEquals(variable.toDouble(), 42.0, 0.001);
    }

    @Test
    public void testToDoubleWithBooleanTrue() {
        Variable variable = new NodeVariable(Boolean.TRUE);
        assertEquals(variable.toDouble(), 1.0, 0.001);
    }

    @Test
    public void testToDoubleWithBooleanFalse() {
        Variable variable = new NodeVariable(Boolean.FALSE);
        assertEquals(variable.toDouble(), 0.0, 0.001);
    }

    @Test
    public void testToDoubleWithString() {
        Variable variable = new NodeVariable("3.14");
        assertEquals(variable.toDouble(), 3.14, 0.001);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testToDoubleWithInvalidString() {
        Variable variable = new NodeVariable("not a number");
        variable.toDouble();
    }

    @Test
    public void testToArrayWithNullObject() {
        Variable variable = new NodeVariable(null);
        Object[] array = variable.toArray();
        assertNotNull(array);
        assertEquals(array.length, 0);
    }

    @Test
    public void testToArrayWithObjectArray() {
        String[] stringArray = {"item1", "item2"};
        Variable variable = new NodeVariable(stringArray);
        Object[] array = variable.toArray();
        assertNotNull(array);
        assertEquals(array.length, 2);
        assertEquals(array[0], "item1");
        assertEquals(array[1], "item2");
    }

    @Test
    public void testToArrayWithCollection() {
        List<String> list = Arrays.asList("item1", "item2");
        Variable variable = new NodeVariable(list);
        Object[] array = variable.toArray();
        assertNotNull(array);
        assertEquals(array.length, 2);
        assertEquals(array[0], "item1");
        assertEquals(array[1], "item2");
    }

    @Test
    public void testToArrayWithSingleObject() {
        Variable variable = new NodeVariable("single item");
        Object[] array = variable.toArray();
        assertNotNull(array);
        assertEquals(array.length, 1);
        assertEquals(array[0], "single item");
    }

    @Test
    public void testAbstractMethodsExist() {
        // Test that all abstract methods are implemented by concrete classes
        NodeVariable nodeVar = new NodeVariable("test");
        
        // These should not throw exceptions
        assertNotNull(nodeVar.toBinary());
        assertNotNull(nodeVar.toBinary("UTF-8"));
        assertNotNull(nodeVar.toString());
        assertNotNull(nodeVar.toString("UTF-8"));
        assertNotNull(nodeVar.toList());
        assertNotNull(nodeVar.isEmpty());
        assertNotNull(nodeVar.getWrappedObject());
        assertNotNull(nodeVar.toIterator());
    }

    @Test
    public void testEmptyVariableImplementsAbstractMethods() {
        // Test EmptyVariable implementation
        assertNotNull(EmptyVariable.INSTANCE.toBinary());
        assertNotNull(EmptyVariable.INSTANCE.toBinary("UTF-8"));
        assertNotNull(EmptyVariable.INSTANCE.toString());
        assertNotNull(EmptyVariable.INSTANCE.toString("UTF-8"));
        assertNotNull(EmptyVariable.INSTANCE.toList());
        assertTrue(EmptyVariable.INSTANCE.isEmpty());
        assertNotNull(EmptyVariable.INSTANCE.getWrappedObject());
        assertNotNull(EmptyVariable.INSTANCE.toIterator());
    }

    @Test
    public void testListVariableImplementsAbstractMethods() {
        // Test ListVariable implementation
        ListVariable listVar = new ListVariable();
        
        assertNotNull(listVar.toBinary());
        assertNotNull(listVar.toBinary("UTF-8"));
        assertNotNull(listVar.toString());
        assertNotNull(listVar.toString("UTF-8"));
        assertNotNull(listVar.toList());
        assertNotNull(listVar.isEmpty());
        assertNotNull(listVar.getWrappedObject());
        assertNotNull(listVar.toIterator());
    }
}














