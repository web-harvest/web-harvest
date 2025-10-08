/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.webbrowser;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Tests for WebBrowserPluginException - Quick win for 0% → 100%
 */
public class WebBrowserPluginExceptionTest {

    @Test
    public void testExceptionWithMessage() {
        WebBrowserPluginException ex = new WebBrowserPluginException("Test error");
        assertEquals("Test error", ex.getMessage());
    }
    
    @Test
    public void testExceptionWithMessageAndCause() {
        Exception cause = new Exception("Root cause");
        WebBrowserPluginException ex = new WebBrowserPluginException("Test error", cause);
        assertEquals("Test error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
    
    @Test
    public void testExceptionWithCause() {
        Exception cause = new Exception("Root cause");
        WebBrowserPluginException ex = new WebBrowserPluginException(cause);
        assertEquals(cause, ex.getCause());
    }
    
    @Test
    public void testExceptionToString() {
        WebBrowserPluginException ex = new WebBrowserPluginException("Test");
        assertNotNull(ex.toString());
    }
    
    @Test
    public void testExceptionStackTrace() {
        WebBrowserPluginException ex = new WebBrowserPluginException("Test");
        assertNotNull(ex.getStackTrace());
    }
}

