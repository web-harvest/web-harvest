package org.webharvest.plugin.db;

import org.testng.annotations.Test;
import org.webharvest.plugin.exceptions.PluginException;

import static org.testng.Assert.*;

/**
 * Test for DatabasePluginException.
 */
public class DatabasePluginExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "Database connection failed";
        DatabasePluginException exception = new DatabasePluginException(message);
        
        assertEquals(exception.getMessage(), message);
        assertNull(exception.getCause());
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        DatabasePluginException exception = new DatabasePluginException(cause);
        
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Database operation failed";
        RuntimeException cause = new RuntimeException("Root cause");
        DatabasePluginException exception = new DatabasePluginException(message, cause);
        
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithNullMessage() {
        DatabasePluginException exception = new DatabasePluginException((String) null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithNullCause() {
        DatabasePluginException exception = new DatabasePluginException((Throwable) null);
        
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithNullMessageAndCause() {
        DatabasePluginException exception = new DatabasePluginException(null, null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testInheritance() {
        DatabasePluginException exception = new DatabasePluginException("Test");
        
        assertTrue(exception instanceof PluginException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }
}










