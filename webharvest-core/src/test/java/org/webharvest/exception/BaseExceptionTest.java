package org.webharvest.exception;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BaseExceptionTest {

    @Test
    public void testDefaultConstructor() {
        BaseException exception = new BaseException();
        
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Test error message";
        BaseException exception = new BaseException(message);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), message);
        assertNull(exception.getCause());
    }

    @Test
    public void testCauseConstructor() {
        RuntimeException cause = new RuntimeException("Original cause");
        BaseException exception = new BaseException(cause);
        
        assertNotNull(exception);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception.getMessage().contains("RuntimeException"));
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Test error message";
        RuntimeException cause = new RuntimeException("Original cause");
        BaseException exception = new BaseException(message, cause);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
    }

    @Test
    public void testInheritanceFromRuntimeException() {
        BaseException exception = new BaseException("Test message");
        
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testWithNullMessage() {
        BaseException exception = new BaseException((String) null);
        
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    public void testWithNullCause() {
        BaseException exception = new BaseException((Throwable) null);
        
        assertNotNull(exception);
        assertNull(exception.getCause());
    }

    @Test
    public void testWithEmptyMessage() {
        String emptyMessage = "";
        BaseException exception = new BaseException(emptyMessage);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), emptyMessage);
    }

    @Test
    public void testWithWhitespaceMessage() {
        String whitespaceMessage = "   ";
        BaseException exception = new BaseException(whitespaceMessage);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), whitespaceMessage);
    }

    @Test
    public void testWithSpecialCharactersInMessage() {
        String specialMessage = "Error with special chars: < > & \" ' äöü";
        BaseException exception = new BaseException(specialMessage);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), specialMessage);
    }

    @Test
    public void testWithNestedCause() {
        RuntimeException nestedCause = new RuntimeException("Nested cause");
        IllegalArgumentException cause = new IllegalArgumentException("Cause", nestedCause);
        BaseException exception = new BaseException("Message", cause);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), "Message");
        assertEquals(exception.getCause(), cause);
        assertEquals(exception.getCause().getCause(), nestedCause);
    }

    @Test
    public void testToString() {
        String message = "Test error message";
        BaseException exception = new BaseException(message);
        
        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("BaseException"));
        assertTrue(toString.contains(message));
    }
}
























