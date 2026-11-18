/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.db;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;

import static org.testng.Assert.*;

/**
 * Unit tests for ConnectionPlugin.
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class ConnectionPluginTest {

    private ConnectionPlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new ConnectionPlugin();
        context = new org.webharvest.runtime.ScraperContext();
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals("Connection Plugin", plugin.getMetadata().getName());
        assertEquals("connection", plugin.getMetadata().getElementName());
        assertEquals("2.2.0", plugin.getMetadata().getVersion());
        assertEquals("database", plugin.getMetadata().getCategory());
        assertEquals("http://org.webharvest/schema/2.1/core", plugin.getMetadata().getNamespace());
        assertFalse(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        assertNotNull(plugin.getMetadata().getConfigurationSchema());
        assertFalse(plugin.getMetadata().getConfigurationSchema().allowsBody());
    }

    @Test
    public void testConnectionPluginInitialize() throws PluginException {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection", 
            "http://org.webharvest/schema/2.1/core", "connection");
        config.setProperty("id", "testDB");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("username", "sa");
        config.setProperty("password", "");
        
        plugin.initialize(config);
        
        assertNotNull(plugin);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testConnectionPluginWithoutId() throws PluginException {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection", 
            "http://org.webharvest/schema/2.1/core", "connection");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testConnectionPluginWithoutDriver() throws PluginException {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection", 
            "http://org.webharvest/schema/2.1/core", "connection");
        config.setProperty("id", "testDB");
        config.setProperty("url", "jdbc:h2:mem:test");
        
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testConnectionPluginWithoutUrl() throws PluginException {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection", 
            "http://org.webharvest/schema/2.1/core", "connection");
        config.setProperty("id", "testDB");
        config.setProperty("driver", "org.h2.Driver");
        
        plugin.validateConfiguration(config);
    }

    @Test
    public void testConnectionPluginWithOptionalParams() throws PluginException {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection", 
            "http://org.webharvest/schema/2.1/core", "connection");
        config.setProperty("id", "testDB");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:test");
        config.setProperty("poolSize", "20");
        
        plugin.initialize(config);
        
        assertNotNull(plugin);
    }

    @Test
    public void testConnectionPluginMetadataDetails() {
        assertNotNull(plugin.getMetadata().getAuthor());
        assertNotNull(plugin.getMetadata().getCategory());
        assertEquals("database", plugin.getMetadata().getCategory());
        assertNotNull(plugin.getMetadata().getTags());
        assertTrue(plugin.getMetadata().getTags().length > 0);
    }

    @Test
    public void testConnectionPluginWithDifferentDrivers() throws PluginException {
        String[] drivers = {
            "org.h2.Driver",
            "org.postgresql.Driver",
            "com.mysql.jdbc.Driver"
        };
        
        for (String driver : drivers) {
            ConnectionPlugin testPlugin = new ConnectionPlugin();
            DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection", 
                "http://org.webharvest/schema/2.1/core", "connection");
            config.setProperty("id", "test_" + driver.hashCode());
            config.setProperty("driver", driver);
            config.setProperty("url", "jdbc:test://localhost/test");
            
            testPlugin.initialize(config);
            assertNotNull(testPlugin);
        }
    }

    @Test
    public void testConnectionPluginMultipleConnections() throws PluginException {
        String[] connectionIds = {"db1", "db2", "db3"};
        
        for (String id : connectionIds) {
            ConnectionPlugin testPlugin = new ConnectionPlugin();
            DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection", 
                "http://org.webharvest/schema/2.1/core", "connection");
            config.setProperty("id", id);
            config.setProperty("driver", "org.h2.Driver");
            config.setProperty("url", "jdbc:h2:mem:" + id);
            
            testPlugin.initialize(config);
            assertNotNull(testPlugin);
        }
    }

    @Test
    public void testConfigurationSchemaExposesAndValidatesItsContract() throws Exception {
        org.webharvest.plugin.PluginConfigurationSchema schema = plugin.getConfigurationSchema();

        assertEquals(schema.getAttributeDefinitions().length, 0);
        assertEquals(schema.getVersion(), "2.2.0");
        assertFalse(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getOptionalAttributes(),
                new String[]{"username", "password", "poolSize"});
        assertEquals(schema.getRequiredAttributes(),
                new String[]{"id", "driver", "url"});
        assertNull(schema.getAttribute("id"));

        DefaultPluginConfiguration valid = new DefaultPluginConfiguration("connection",
                "http://org.webharvest/schema/2.1/core", "connection");
        valid.setProperty("id", "schema_test");
        valid.setProperty("driver", "org.h2.Driver");
        valid.setProperty("url", "jdbc:h2:mem:schema_test");
        schema.validate(valid);

        DefaultPluginConfiguration invalid = new DefaultPluginConfiguration("connection",
                "http://org.webharvest/schema/2.1/core", "connection");
        try {
            schema.validate(invalid);
            fail("Expected validation to reject a missing id");
        } catch (PluginValidationException expected) {
            assertTrue(expected.getMessage().contains("id"));
        }
    }

}
