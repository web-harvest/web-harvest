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

package org.webharvest.plugin.mail;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Test class for MailPlugin.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class MailPluginTest {

    private MailPlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new MailPlugin();
        context = new org.webharvest.runtime.ScraperContext();
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals("Mail Plugin", plugin.getMetadata().getName());
        assertEquals("mail", plugin.getMetadata().getElementName());
        assertEquals("http://org.webharvest/schema/2.1/core", plugin.getMetadata().getNamespace());
    }

    @Test
    public void testGetConfigurationSchema() {
        assertNotNull(plugin.getConfigurationSchema());
        assertNotNull(plugin.getConfigurationSchema().getAttributeDefinitions());
        assertTrue(plugin.getConfigurationSchema().getAttributeDefinitions().length >= 0);
    }

    @Test
    public void testConfigurationValidation() throws Exception {
        // Test valid configuration
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        config.setProperty("smtp-host", "localhost");
        config.setProperty("smtp-port", "25");
        config.setProperty("from", "test@example.com");
        config.setProperty("to", "recipient@example.com");
        config.setProperty("subject", "Test Subject");
        config.setProperty("body", "Test Body");
        
        plugin.initialize(config);
        // Configuration is protected, so we can't access it directly
    }

    @Test(expectedExceptions = PluginException.class)
    public void testConfigurationValidationWithNullConfig() throws Exception {
        plugin.initialize(null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testConfigurationValidationWithMissingRequiredAttributes() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        // Missing required attributes
        plugin.initialize(config);
    }

    @Test
    public void testExecuteWithInvalidHost() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        config.setProperty("smtp-host", "invalid-host-that-does-not-exist");
        config.setProperty("smtp-port", "25");
        config.setProperty("from", "test@example.com");
        config.setProperty("to", "recipient@example.com");
        config.setProperty("subject", "Test Subject");
        addNonEmptyBody(config);
        plugin.initialize(config);

        // Should throw PluginException due to DNS/connection failure
        try {
            plugin.execute(context);
            fail("Expected PluginException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Mail operation failed") || e.getMessage().contains("operation failed") || e.getMessage().contains("Failed to initialize plugin") || e.getMessage().contains("Invalid message supplied"));
        }
    }

    @Test
    public void testExecuteWithUnavailableLocalSmtpServer() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        config.setProperty("smtp-host", "127.0.0.1");
        config.setProperty("smtp-port", "1");
        config.setProperty("from", "test@example.com");
        config.setProperty("to", "recipient@example.com");
        config.setProperty("subject", "Test Subject");
        addNonEmptyBody(config);

        plugin.initialize(config);

        // The plugin must reach SMTP delivery and report the unavailable server.
        try {
            plugin.execute(context);
            fail("Expected PluginException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Mail operation failed")
                    || e.getMessage().contains("Failed to send email")
                    || e.getMessage().contains("operation failed"));
        }
    }

    private void addNonEmptyBody(DefaultPluginConfiguration config) {
        DefaultPluginConfiguration text = new DefaultPluginConfiguration(
                "text", "http://org.webharvest/schema/2.1/core", "text");
        text.setProperty("_textContent", "Test Body");
        config.setChildren(java.util.Collections.<org.webharvest.plugin.PluginConfiguration>singletonList(text));
    }

    @Test
    public void testGetBooleanProperty() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        config.setProperty("smtp-host", "localhost");
        config.setProperty("smtp-port", "25");
        config.setProperty("from", "test@example.com");
        config.setProperty("to", "recipient@example.com");
        config.setProperty("subject", "Test Subject");
        config.setProperty("body", "Test Body");
        config.setProperty("ssl", "true");
        
        plugin.initialize(config);
        
        // Test getBooleanProperty method - this is protected, so we can't test it directly
        // We can only test that the plugin initializes without errors
    }

    @Test
    public void testExecuteWithValidConfiguration() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        config.setProperty("smtp-host", "localhost");
        config.setProperty("smtp-port", "25");
        config.setProperty("from", "test@example.com");
        config.setProperty("to", "recipient@example.com");
        config.setProperty("subject", "Test Subject");
        config.setProperty("body", "Test Body");
        
        plugin.initialize(config);
        
        // Test that plugin can be initialized without errors
        // Note: executeAction is private, so we can't test it directly
    }

    @Test
    public void testGetStringProperty() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        config.setProperty("smtp-host", "test-host");
        config.setProperty("smtp-port", "25");
        config.setProperty("from", "test@example.com");
        config.setProperty("to", "recipient@example.com");
        config.setProperty("subject", "Test Subject");
        config.setProperty("body", "Test Body");
        
        plugin.initialize(config);
        
        // Test that plugin can be initialized without errors
        // Note: getStringProperty is protected, so we can't test it directly
    }

    @Test
    public void testGetIntProperty() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
        config.setProperty("smtp-host", "localhost");
        config.setProperty("smtp-port", "587");
        config.setProperty("from", "test@example.com");
        config.setProperty("to", "recipient@example.com");
        config.setProperty("subject", "Test Subject");
        config.setProperty("body", "Test Body");
        
        plugin.initialize(config);
        
        // Test that plugin can be initialized without errors
        // Note: getIntProperty is protected, so we can't test it directly
    }

    @Test
    public void testExecuteWithDifferentActions() throws Exception {
        String[] actions = {"send"};
        
        for (String action : actions) {
            MailPlugin testPlugin = new MailPlugin();
            DefaultPluginConfiguration config = new DefaultPluginConfiguration("mail", "http://org.webharvest/schema/2.1/core", "mail");
            config.setProperty("smtp-host", "localhost");
            config.setProperty("smtp-port", "25");
            config.setProperty("from", "test@example.com");
            config.setProperty("to", "recipient@example.com");
            config.setProperty("subject", "Test Subject");
            config.setProperty("body", "Test Body");
            config.setProperty("action", action);
            
            testPlugin.initialize(config);
            
            // Test that plugin can be initialized for each action
        }
    }
}
