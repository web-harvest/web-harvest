/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.ftp;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;

/**
 * Unit tests for FTP plugin classes.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public class FtpPluginTest {
    
    private FtpPlugin ftpPlugin;
    private PluginConfiguration config;
    
    @BeforeMethod
    public void setUp() {
        ftpPlugin = new FtpPlugin();
        config = mock(PluginConfiguration.class);
    }
    
    @Test
    public void testFtpPluginConstructor() {
        assertNotNull(ftpPlugin, "FtpPlugin should not be null");
    }
    
    @Test
    public void testFtpPluginGetMetadata() {
        PluginMetadata metadata = ftpPlugin.getMetadata();
        
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(metadata.getElementName(), "ftp", "Element name should be 'ftp'");
        assertTrue(metadata.isCorePlugin(), "Should be a core plugin");
    }
    
    @Test
    public void testFtpPluginGetConfigurationSchema() {
        PluginConfigurationSchema schema = ftpPlugin.getConfigurationSchema();
        
        assertNotNull(schema, "Configuration schema should not be null");
        assertTrue(schema.getRequiredAttributes().length > 0, "Should have required attributes");
        assertFalse(schema.allowsBody(), "Should not allow body content");
    }
    
    @Test
    public void testFtpPluginInitialize() throws Exception {
        // Set required attributes
        when(config.getProperty("host")).thenReturn("localhost");
        when(config.getProperty("username")).thenReturn("user");
        when(config.getProperty("password")).thenReturn("pass");
        when(config.getProperty("action")).thenReturn("list");
        
        ftpPlugin.initialize(config);
        
        assertTrue(ftpPlugin.isInitialized(), "Plugin should be initialized");
    }
    
    @Test
    public void testFtpPluginValidateConfiguration() throws Exception {
        // Test with valid configuration
        when(config.getProperty("host")).thenReturn("localhost");
        when(config.getProperty("username")).thenReturn("user");
        when(config.getProperty("password")).thenReturn("pass");
        when(config.getProperty("port")).thenReturn("21");
        when(config.getProperty("action")).thenReturn("list");
        
        ftpPlugin.initialize(config);
        
        // Should not throw exception
        ftpPlugin.validateConfiguration(config);
    }
    
    @Test
    public void testFtpGetPlugin() {
        FtpGetPlugin getPlugin = new FtpGetPlugin();
        
        assertNotNull(getPlugin, "FtpGetPlugin should not be null");
        
        PluginMetadata metadata = getPlugin.getMetadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(metadata.getElementName(), "ftp-get", "Element name should be 'ftp-get'");
    }
    
    @Test
    public void testFtpPutPlugin() {
        FtpPutPlugin putPlugin = new FtpPutPlugin();
        
        assertNotNull(putPlugin, "FtpPutPlugin should not be null");
        
        PluginMetadata metadata = putPlugin.getMetadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(metadata.getElementName(), "ftp-put", "Element name should be 'ftp-put'");
    }
    
    @Test
    public void testFtpListPlugin() {
        FtpListPlugin listPlugin = new FtpListPlugin();
        
        assertNotNull(listPlugin, "FtpListPlugin should not be null");
        
        PluginMetadata metadata = listPlugin.getMetadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(metadata.getElementName(), "ftp-list", "Element name should be 'ftp-list'");
    }
    
    @Test
    public void testFtpPluginException() {
        String message = "FTP error";
        FtpPluginException exception = new FtpPluginException(message);
        
        assertEquals(exception.getMessage(), message);
        assertTrue(exception instanceof Exception);
    }
    
    @Test
    public void testFtpPluginExceptionWithCause() {
        String message = "FTP error";
        Throwable cause = new RuntimeException("Root cause");
        FtpPluginException exception = new FtpPluginException(message, cause);
        
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getCause(), cause);
    }
    
    @Test
    public void testFtpPluginWithDifferentActions() throws Exception {
        String[] actions = {"list", "get", "put", "mkdir", "rmdir", "del"};
        
        for (String action : actions) {
            FtpPlugin testPlugin = new FtpPlugin();
            when(config.getProperty("host")).thenReturn("localhost");
            when(config.getProperty("username")).thenReturn("user");
            when(config.getProperty("password")).thenReturn("pass");
            when(config.getProperty("port")).thenReturn("21");
            when(config.getProperty("action")).thenReturn(action);
            
            testPlugin.initialize(config);
            
            // Should not throw exception during initialization
            assertTrue(testPlugin.isInitialized(), "Plugin should be initialized for action: " + action);
        }
    }
    

}
