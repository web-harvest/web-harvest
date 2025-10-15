package org.webharvest.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.testng.Assert.*;

/**
 * Tests for Stack utility class.
 * Simple LIFO queue implementation.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class StackTest {

    private Stack<String> stack;

    @BeforeMethod
    public void setUp() {
        stack = new Stack<>();
    }

    // ========== Basic Operations ==========

    @Test
    public void testNewStackIsEmpty() {
        assertTrue(stack.isEmpty());
        assertEquals(stack.size(), 0);
    }

    @Test
    public void testPushOneElement() {
        stack.push("first");
        assertFalse(stack.isEmpty());
        assertEquals(stack.size(), 1);
    }

    @Test
    public void testPushMultipleElements() {
        stack.push("first");
        stack.push("second");
        stack.push("third");
        assertEquals(stack.size(), 3);
        assertFalse(stack.isEmpty());
    }

    @Test
    public void testPopElement() {
        stack.push("item");
        String popped = stack.pop();
        assertEquals(popped, "item");
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testPopMultipleElementsLIFO() {
        stack.push("first");
        stack.push("second");
        stack.push("third");
        
        assertEquals(stack.pop(), "third");
        assertEquals(stack.pop(), "second");
        assertEquals(stack.pop(), "first");
        assertTrue(stack.isEmpty());
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void testPopEmptyStackThrows() {
        stack.pop();
    }

    @Test
    public void testPeekElement() {
        stack.push("item");
        String peeked = stack.peek();
        assertEquals(peeked, "item");
        assertEquals(stack.size(), 1); // Size unchanged
    }

    @Test
    public void testPeekDoesNotRemoveElement() {
        stack.push("first");
        stack.push("second");
        
        assertEquals(stack.peek(), "second");
        assertEquals(stack.size(), 2);
        assertEquals(stack.peek(), "second");
        assertEquals(stack.size(), 2);
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void testPeekEmptyStackThrows() {
        stack.peek();
    }

    // ========== replaceTop() Tests ==========

    @Test
    public void testReplaceTop() {
        stack.push("original");
        String old = stack.replaceTop("replacement");
        
        assertEquals(old, "original");
        assertEquals(stack.peek(), "replacement");
        assertEquals(stack.size(), 1);
    }

    @Test
    public void testReplaceTopWithMultipleElements() {
        stack.push("first");
        stack.push("second");
        stack.push("third");
        
        String old = stack.replaceTop("new-third");
        
        assertEquals(old, "third");
        assertEquals(stack.peek(), "new-third");
        assertEquals(stack.size(), 3);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testReplaceTopOnEmptyStackThrows() {
        stack.replaceTop("value");
    }

    // ========== getList() Tests ==========

    @Test
    public void testGetListEmpty() {
        List<String> list = stack.getList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testGetListWithElements() {
        stack.push("first");
        stack.push("second");
        stack.push("third");
        
        List<String> list = stack.getList();
        assertNotNull(list);
        assertEquals(list.size(), 3);
        assertEquals(list.get(0), "first");
        assertEquals(list.get(1), "second");
        assertEquals(list.get(2), "third");
    }

    @Test
    public void testGetListReturnsInternalList() {
        stack.push("item");
        List<String> list1 = stack.getList();
        List<String> list2 = stack.getList();
        
        assertSame(list1, list2); // Same instance
    }

    // ========== EMPTY Stack Tests ==========

    @Test
    public void testEmptyStackIsEmpty() {
        assertTrue(Stack.EMPTY.isEmpty());
        assertEquals(Stack.EMPTY.size(), 0);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testEmptyStackPushThrows() {
        Stack.EMPTY.push("item");
    }

    @Test
    public void testEmptyStackGetListReturnsEmptyList() {
        List list = Stack.EMPTY.getList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testEmptyStackIsSingleton() {
        Stack empty1 = Stack.EMPTY;
        Stack empty2 = Stack.EMPTY;
        assertSame(empty1, empty2);
    }

    // ========== Edge Cases ==========

    @Test
    public void testPushNullElement() {
        stack.push(null);
        assertFalse(stack.isEmpty());
        assertEquals(stack.size(), 1);
        assertNull(stack.peek());
    }

    @Test
    public void testPushPopSequence() {
        stack.push("A");
        stack.push("B");
        assertEquals(stack.pop(), "B");
        stack.push("C");
        assertEquals(stack.pop(), "C");
        assertEquals(stack.pop(), "A");
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testSizeAfterOperations() {
        assertEquals(stack.size(), 0);
        stack.push("1");
        assertEquals(stack.size(), 1);
        stack.push("2");
        assertEquals(stack.size(), 2);
        stack.pop();
        assertEquals(stack.size(), 1);
        stack.pop();
        assertEquals(stack.size(), 0);
    }

    @Test
    public void testIsEmptyAfterOperations() {
        assertTrue(stack.isEmpty());
        stack.push("item");
        assertFalse(stack.isEmpty());
        stack.pop();
        assertTrue(stack.isEmpty());
    }

    // ========== Type Safety Tests ==========

    @Test
    public void testGenericTypeString() {
        Stack<String> stringStack = new Stack<>();
        stringStack.push("text");
        String value = stringStack.pop();
        assertEquals(value, "text");
    }

    @Test
    public void testGenericTypeInteger() {
        Stack<Integer> intStack = new Stack<>();
        intStack.push(42);
        intStack.push(100);
        assertEquals(intStack.pop().intValue(), 100);
        assertEquals(intStack.pop().intValue(), 42);
    }

    @Test
    public void testMixedOperations() {
        stack.push("A");
        assertEquals(stack.peek(), "A");
        stack.push("B");
        assertEquals(stack.replaceTop("C"), "B");
        assertEquals(stack.peek(), "C");
        assertEquals(stack.size(), 2);
        assertEquals(stack.pop(), "C");
        assertEquals(stack.pop(), "A");
        assertTrue(stack.isEmpty());
    }
}

