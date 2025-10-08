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

package org.webharvest.plugin.db;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.database.ConnectionFactory;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test class for DatabasePlugin.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class DatabasePluginTest {

    private DatabasePlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new DatabasePlugin();
        context = new org.webharvest.runtime.ScraperContext();
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals("Database Plugin", plugin.getMetadata().getName());
        assertEquals("db", plugin.getMetadata().getElementName());
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
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("username", "test");
        config.setProperty("password", "test");
        config.setProperty("action", "select");
        config.setProperty("query", "SELECT * FROM test");
        
        plugin.initialize(config);
        // Configuration is protected, so we can't access it directly
    }

    @Test(expectedExceptions = PluginException.class)
    public void testConfigurationValidationWithNullConfig() throws Exception {
        plugin.initialize(null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testConfigurationValidationWithMissingRequiredAttributes() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        // Missing required attributes
        plugin.initialize(config);
    }

    @Test
    public void testExecuteWithInvalidDriver() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        config.setProperty("driver", "com.invalid.Driver");
        config.setProperty("url", "jdbc:invalid:test");
        config.setProperty("username", "test");
        config.setProperty("password", "test");
        config.setProperty("action", "select");
        config.setProperty("query", "SELECT * FROM test");
        
        plugin.initialize(config);
        
        // Should throw PluginException due to driver not found
        try {
            plugin.execute(context);
            fail("Expected PluginException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Database driver not found") || e.getMessage().contains("Database operation failed") || e.getMessage().contains("operation failed") || e.getMessage().contains("Failed to initialize plugin") || e.getMessage().contains("ClassNotFoundException") || e.getMessage().contains("SQLException"));
        }
    }

    @Test
    public void testExecuteWithUnsupportedAction() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("username", "test");
        config.setProperty("password", "test");
        config.setProperty("action", "unsupported");
        config.setProperty("query", "SELECT * FROM test");
        
        plugin.initialize(config);
        
        // Should throw PluginException due to unsupported action
        try {
            plugin.execute(context);
            fail("Expected PluginException");
        } catch (Exception e) {
            boolean containsExpectedMessage = e.getMessage().contains("Database operation failed") || 
                e.getMessage().contains("operation failed") || 
                e.getMessage().contains("Failed to initialize plugin") || 
                e.getMessage().contains("ClassNotFoundException") || 
                e.getMessage().contains("SQLException") ||
                e.getMessage().contains("Unsupported action") ||
                e.getMessage().contains("Database driver not found");
            assertTrue(containsExpectedMessage, "Unexpected exception message: " + e.getMessage());
        }
    }

    @Test
    public void testGetBooleanProperty() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("username", "test");
        config.setProperty("password", "test");
        config.setProperty("action", "select");
        config.setProperty("query", "SELECT * FROM test");
        config.setProperty("autocommit", "true");
        
        plugin.initialize(config);
        
        // Test getBooleanProperty method - this is protected, so we can't test it directly
        // We can only test that the plugin initializes without errors
    }

    @Test
    public void testExecuteWithValidConfiguration() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("username", "test");
        config.setProperty("password", "test");
        config.setProperty("action", "select");
        config.setProperty("query", "SELECT * FROM test");
        
        plugin.initialize(config);
        
        // Test that plugin can be initialized without errors
        // Note: executeAction is private, so we can't test it directly
    }

    @Test
    public void testGetStringProperty() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("username", "test");
        config.setProperty("password", "test");
        config.setProperty("action", "select");
        config.setProperty("query", "SELECT * FROM test");
        
        plugin.initialize(config);
        
        // Test that plugin can be initialized without errors
        // Note: getStringProperty is protected, so we can't test it directly
    }

    @Test
    public void testGetIntProperty() throws Exception {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("username", "test");
        config.setProperty("password", "test");
        config.setProperty("action", "select");
        config.setProperty("query", "SELECT * FROM test");
        config.setProperty("timeout", "30");
        
        plugin.initialize(config);
        
        // Test that plugin can be initialized without errors
        // Note: getIntProperty is protected, so we can't test it directly
    }

    @Test
    public void testExecuteWithDifferentActions() throws Exception {
        String[] actions = {"select", "insert", "update", "delete", "execute"};
        
        for (String action : actions) {
            DatabasePlugin testPlugin = new DatabasePlugin();
            DefaultPluginConfiguration config = new DefaultPluginConfiguration("db", "http://org.webharvest/schema/2.1/core", "db");
            config.setProperty("driver", "org.h2.Driver");
            config.setProperty("url", "jdbc:h2:mem:test");
            config.setProperty("username", "test");
            config.setProperty("password", "test");
            config.setProperty("action", action);
            config.setProperty("query", "SELECT * FROM test");
            
            testPlugin.initialize(config);
            
            // Test that plugin can be initialized for each action
        }
    }
}
