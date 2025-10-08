package org.webharvest.runtime.variables;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

/**
 * Test for ListVariable class.
 */
public class ListVariableTest {

    private ListVariable listVariable;

    @BeforeMethod
    public void setUp() {
        listVariable = new ListVariable();
    }

    @Test
    public void testDefaultConstructor() {
        ListVariable variable = new ListVariable();
        assertNotNull(variable);
        assertTrue(variable.isEmpty());
        assertEquals(variable.toString(), "");
        assertEquals(variable.toList().size(), 0);
    }

    @Test
    public void testConstructorWithNullIterable() {
        ListVariable variable = new ListVariable((Iterable) null);
        assertNotNull(variable);
        assertTrue(variable.isEmpty());
        assertEquals(variable.toString(), "");
    }

    @Test
    public void testConstructorWithEmptyList() {
        List<Object> emptyList = new ArrayList<>();
        ListVariable variable = new ListVariable(emptyList);
        assertNotNull(variable);
        assertTrue(variable.isEmpty());
        assertEquals(variable.toString(), "");
    }

    @Test
    public void testConstructorWithStringList() {
        List<String> stringList = Arrays.asList("item1", "item2", "item3");
        ListVariable variable = new ListVariable(stringList);
        assertNotNull(variable);
        assertFalse(variable.isEmpty());
        assertEquals(variable.toList().size(), 3);
        assertTrue(variable.toString().contains("item1"));
        assertTrue(variable.toString().contains("item2"));
        assertTrue(variable.toString().contains("item3"));
    }

    @Test
    public void testConstructorWithVariableList() {
        List<Variable> variableList = Arrays.asList(
            new NodeVariable("item1"),
            new NodeVariable("item2")
        );
        ListVariable variable = new ListVariable(variableList);
        assertNotNull(variable);
        assertFalse(variable.isEmpty());
        assertEquals(variable.toList().size(), 2);
    }

    @Test
    public void testConstructorWithEmptyVariables() {
        List<Variable> variableList = Arrays.asList(
            EmptyVariable.INSTANCE,
            new NodeVariable("item1"),
            EmptyVariable.INSTANCE
        );
        ListVariable variable = new ListVariable(variableList);
        assertNotNull(variable);
        assertFalse(variable.isEmpty());
        assertEquals(variable.toList().size(), 1); // Empty variables are filtered out
    }

    @Test
    public void testAddVariable() {
        NodeVariable nodeVar = new NodeVariable("test");
        listVariable.addVariable(nodeVar);
        
        assertFalse(listVariable.isEmpty());
        assertEquals(listVariable.toList().size(), 1);
        assertEquals(listVariable.toString(), "test");
    }

    @Test
    public void testAddNullVariable() {
        listVariable.addVariable(null);
        
        assertTrue(listVariable.isEmpty()); // null becomes EmptyVariable.INSTANCE
        assertEquals(listVariable.toList().size(), 1);
        assertEquals(listVariable.toString(), "");
    }

    @Test
    public void testAddListVariable() {
        ListVariable innerList = new ListVariable();
        innerList.addVariable(new NodeVariable("item1"));
        innerList.addVariable(new NodeVariable("item2"));
        
        listVariable.addVariable(innerList);
        
        assertFalse(listVariable.isEmpty());
        assertEquals(listVariable.toList().size(), 2);
        assertTrue(listVariable.toString().contains("item1"));
        assertTrue(listVariable.toString().contains("item2"));
    }

    @Test
    public void testToStringWithMultipleItems() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(new NodeVariable("item2"));
        listVariable.addVariable(new NodeVariable("item3"));
        
        String result = listVariable.toString();
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
        assertTrue(result.contains("item3"));
        assertTrue(result.contains("\n")); // Items are separated by newlines
    }

    @Test
    public void testToStringWithCharset() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(new NodeVariable("item2"));
        
        String result = listVariable.toString("UTF-8");
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
    }

    @Test
    public void testToStringWithCharsetAndDelimiter() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(new NodeVariable("item2"));
        
        String result = listVariable.toString("UTF-8", ",");
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
        assertTrue(result.contains(","));
    }

    @Test
    public void testToBinary() {
        listVariable.addVariable(new NodeVariable("test"));
        
        byte[] result = listVariable.toBinary();
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void testToBinaryWithCharset() {
        listVariable.addVariable(new NodeVariable("test"));
        
        byte[] result = listVariable.toBinary("UTF-8");
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void testToBinaryWithNullCharset() {
        listVariable.addVariable(new NodeVariable("test"));
        
        byte[] result = listVariable.toBinary(null);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void testGetWrappedObject() {
        Object wrapped = listVariable.getWrappedObject();
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof List);
    }

    @Test
    public void testToIterator() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(new NodeVariable("item2"));
        
        Iterator iterator = listVariable.toIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
    }

    @Test
    public void testIterator() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(new NodeVariable("item2"));
        
        Iterator<Variable> iterator = listVariable.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
    }

    @Test
    public void testGet() {
        NodeVariable nodeVar1 = new NodeVariable("item1");
        NodeVariable nodeVar2 = new NodeVariable("item2");
        listVariable.addVariable(nodeVar1);
        listVariable.addVariable(nodeVar2);
        
        assertEquals(listVariable.get(0), nodeVar1);
        assertEquals(listVariable.get(1), nodeVar2);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testGetWithInvalidIndex() {
        listVariable.get(0);
    }

    @Test
    public void testContains() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(new NodeVariable("item2"));
        
        assertTrue(listVariable.contains("item1"));
        assertTrue(listVariable.contains("item2"));
        assertFalse(listVariable.contains("item3"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testContainsWithNull() {
        listVariable.addVariable(new NodeVariable("item1"));
        
        listVariable.contains(null); // This should throw NullPointerException
    }

    @Test
    public void testIsEmptyWithNonEmptyVariables() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(EmptyVariable.INSTANCE);
        
        assertFalse(listVariable.isEmpty()); // At least one non-empty variable
    }

    @Test
    public void testIsEmptyWithOnlyEmptyVariables() {
        listVariable.addVariable(EmptyVariable.INSTANCE);
        listVariable.addVariable(EmptyVariable.INSTANCE);
        
        assertTrue(listVariable.isEmpty()); // All variables are empty
    }

    @Test
    public void testCachedStringRepresentation() {
        listVariable.addVariable(new NodeVariable("item1"));
        String firstCall = listVariable.toString();
        
        listVariable.addVariable(new NodeVariable("item2"));
        String secondCall = listVariable.toString();
        
        assertFalse(firstCall.equals(secondCall)); // Cache should be invalidated
        assertTrue(secondCall.contains("item1"));
        assertTrue(secondCall.contains("item2"));
    }

    @Test
    public void testToArray() {
        listVariable.addVariable(new NodeVariable("item1"));
        listVariable.addVariable(new NodeVariable("item2"));
        
        Object[] array = listVariable.toArray();
        assertNotNull(array);
        assertEquals(array.length, 2);
    }

    @Test
    public void testToBoolean() {
        listVariable.addVariable(new NodeVariable("true"));
        assertTrue(listVariable.toBoolean());
        
        listVariable = new ListVariable();
        listVariable.addVariable(new NodeVariable("false"));
        assertFalse(listVariable.toBoolean());
    }

    @Test
    public void testToInt() {
        listVariable.addVariable(new NodeVariable("42"));
        assertEquals(listVariable.toInt(), 42);
    }

    @Test
    public void testToLong() {
        listVariable.addVariable(new NodeVariable("123456789"));
        assertEquals(listVariable.toLong(), 123456789L);
    }

    @Test
    public void testToDouble() {
        listVariable.addVariable(new NodeVariable("3.14"));
        assertEquals(listVariable.toDouble(), 3.14, 0.001);
    }
}
