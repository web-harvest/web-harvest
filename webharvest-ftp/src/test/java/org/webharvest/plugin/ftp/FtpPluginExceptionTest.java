package org.webharvest.plugin.ftp;

import org.testng.annotations.Test;
import org.webharvest.plugin.exceptions.PluginException;

import static org.testng.Assert.*;

/**
 * Test for FtpPluginException.
 */
public class FtpPluginExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "FTP connection failed";
        FtpPluginException exception = new FtpPluginException(message);
        
        assertEquals(exception.getMessage(), message);
        assertNull(exception.getCause());
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        FtpPluginException exception = new FtpPluginException(cause);
        
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "FTP operation failed";
        RuntimeException cause = new RuntimeException("Root cause");
        FtpPluginException exception = new FtpPluginException(message, cause);
        
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
        assertTrue(exception instanceof PluginException);
    }

    @Test
    public void testConstructorWithNullMessage() {
        FtpPluginException exception = new FtpPluginException((String) null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithNullCause() {
        FtpPluginException exception = new FtpPluginException((Throwable) null);
        
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithNullMessageAndCause() {
        FtpPluginException exception = new FtpPluginException(null, null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testInheritance() {
        FtpPluginException exception = new FtpPluginException("Test");
        
        assertTrue(exception instanceof PluginException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }
}










