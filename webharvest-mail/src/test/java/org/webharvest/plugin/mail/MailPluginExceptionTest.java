package org.webharvest.plugin.mail;

import org.testng.annotations.Test;
import org.webharvest.plugin.exceptions.PluginException;

import static org.testng.Assert.*;

/**
 * Test for MailPluginException.
 */
public class MailPluginExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "Mail sending failed";
        MailPluginException exception = new MailPluginException(message);
        
        assertEquals(exception.getMessage(), message);
        assertNull(exception.getCause());
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        MailPluginException exception = new MailPluginException(cause);
        
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Mail operation failed";
        RuntimeException cause = new RuntimeException("Root cause");
        MailPluginException exception = new MailPluginException(message, cause);
        
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithNullMessage() {
        MailPluginException exception = new MailPluginException((String) null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithNullCause() {
        MailPluginException exception = new MailPluginException((Throwable) null);
        
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithNullMessageAndCause() {
        MailPluginException exception = new MailPluginException(null, null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testInheritance() {
        MailPluginException exception = new MailPluginException("Test");
        
        assertTrue(exception instanceof PluginException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }
}










