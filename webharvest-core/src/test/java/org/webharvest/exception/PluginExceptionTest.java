package org.webharvest.exception;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PluginExceptionTest {

    @Test
    public void testDefaultConstructor() {
        PluginException exception = new PluginException();
        
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testMessageConstructor() {
        String message = "Plugin error message";
        PluginException exception = new PluginException(message);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), message);
        assertNull(exception.getCause());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testCauseConstructor() {
        RuntimeException cause = new RuntimeException("Plugin cause");
        PluginException exception = new PluginException(cause);
        
        assertNotNull(exception);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception.getMessage().contains("RuntimeException"));
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String message = "Plugin error message";
        RuntimeException cause = new RuntimeException("Plugin cause");
        PluginException exception = new PluginException(message, cause);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof BaseException);
    }

    @Test
    public void testInheritanceFromBaseException() {
        PluginException exception = new PluginException("Test message");
        
        assertTrue(exception instanceof BaseException);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testWithNullMessage() {
        PluginException exception = new PluginException((String) null);
        
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    public void testWithNullCause() {
        PluginException exception = new PluginException((Throwable) null);
        
        assertNotNull(exception);
        assertNull(exception.getCause());
    }

    @Test
    public void testWithEmptyMessage() {
        String emptyMessage = "";
        PluginException exception = new PluginException(emptyMessage);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), emptyMessage);
    }

    @Test
    public void testWithSpecialCharactersInMessage() {
        String specialMessage = "Plugin error with special chars: < > & \" ' äöü";
        PluginException exception = new PluginException(specialMessage);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), specialMessage);
    }

    @Test
    public void testWithNestedCause() {
        RuntimeException nestedCause = new RuntimeException("Nested cause");
        IllegalArgumentException cause = new IllegalArgumentException("Cause", nestedCause);
        PluginException exception = new PluginException("Message", cause);
        
        assertNotNull(exception);
        assertEquals(exception.getMessage(), "Message");
        assertEquals(exception.getCause(), cause);
        assertEquals(exception.getCause().getCause(), nestedCause);
    }

    @Test
    public void testToString() {
        String message = "Plugin error message";
        PluginException exception = new PluginException(message);
        
        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("PluginException"));
        assertTrue(toString.contains(message));
    }

    @Test
    public void testMultipleInstances() {
        PluginException exception1 = new PluginException("Error 1");
        PluginException exception2 = new PluginException("Error 2");
        
        assertFalse(exception1.getMessage().equals(exception2.getMessage()));
        assertEquals(exception1.getMessage(), "Error 1");
        assertEquals(exception2.getMessage(), "Error 2");
    }
}
