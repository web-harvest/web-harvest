package org.webharvest.exception;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class VariableExceptionTest {

    @Test
    public void testDefaultConstructor() {
        VariableException exception = new VariableException();
        
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testMessageConstructor() {
        String message = "Variable error message";
        VariableException exception = new VariableException(message);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), message);
        assertNull(exception.getCause());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testCauseConstructor() {
        RuntimeException cause = new RuntimeException("Variable cause");
        VariableException exception = new VariableException(cause);
        
        assertNotNull(exception);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception.getMessage().contains("RuntimeException"));
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Variable error message";
        RuntimeException cause = new RuntimeException("Variable cause");
        VariableException exception = new VariableException(message, cause);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testInheritanceFromBaseException() {
        VariableException exception = new VariableException("Test message");
        
        assertTrue(exception instanceof BaseException);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testWithNullMessage() {
        VariableException exception = new VariableException((String) null);
        
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    public void testWithNullCause() {
        VariableException exception = new VariableException((Throwable) null);
        
        assertNotNull(exception);
        assertNull(exception.getCause());
    }

    @Test
    public void testWithEmptyMessage() {
        String emptyMessage = "";
        VariableException exception = new VariableException(emptyMessage);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), emptyMessage);
    }

    @Test
    public void testWithSpecialCharactersInMessage() {
        String specialMessage = "Variable error with special chars: < > & \" ' äöü";
        VariableException exception = new VariableException(specialMessage);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), specialMessage);
    }

    @Test
    public void testWithNestedCause() {
        RuntimeException nestedCause = new RuntimeException("Nested cause");
        IllegalArgumentException cause = new IllegalArgumentException("Cause", nestedCause);
        VariableException exception = new VariableException("Message", cause);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), "Message");
        assertEquals(exception.getCause(), cause);
        assertEquals(exception.getCause().getCause(), nestedCause);
    }

    @Test
    public void testToString() {
        String message = "Variable error message";
        VariableException exception = new VariableException(message);
        
        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("VariableException"));
        assertTrue(toString.contains(message));
    }

    @Test
    public void testMultipleInstances() {
        VariableException exception1 = new VariableException("Error 1");
        VariableException exception2 = new VariableException("Error 2");
        
        assertFalse(exception1.getMessage().equals(exception2.getMessage()));
        assertEquals(exception1.getMessage(), "Error 1");
        assertEquals(exception2.getMessage(), "Error 2");
    }
}
