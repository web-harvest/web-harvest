/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.db;

import com.google.common.eventbus.EventBus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.database.ConnectionProxy;
import org.webharvest.runtime.resource.LedgerExecutionResourceCoordinator;
import org.webharvest.runtime.resource.ResourceLedger;
import org.webharvest.runtime.session.ExecutionOwner;
import org.webharvest.definition.Config;
import org.webharvest.runtime.session.ScraperSessionImpl;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;

import java.time.Duration;

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

    @Test
    public void legacyNamedLookupRejectsMissingFactory() {
        try {
            ConnectionPlugin.getNamedConnection(context, "missing");
            fail("Expected a missing named connection to be rejected");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("not found"));
        } catch (Exception unexpected) {
            fail("Unexpected checked exception", unexpected);
        }
    }

    @Test
    public void legacyNamedLookupRejectsVariableOfAnotherType() {
        context.setLocalVar("webharvest_db_factory_wrongType",
                new org.webharvest.runtime.variables.NodeVariable("not a connection"));
        try {
            ConnectionPlugin.getNamedConnection(context, "wrongType");
            fail("Expected a non-connection variable to be rejected");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("not a database connection"));
        } catch (Exception unexpected) {
            fail("Unexpected checked exception", unexpected);
        }
    }

    @Test
    public void registersNamedPoolWithSessionEventBusAndReleasesPhysicalConnection() throws Exception {
        EventBus eventBus = new EventBus();
        context.setEventBus(eventBus);
        ScraperSessionImpl session = new ScraperSessionImpl(
                "session-db-test", "test-client", mock(Config.class), eventBus);
        context.setSession(session);
        session.markRunning(context);
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection",
                "http://org.webharvest/schema/2.1/core", "connection");
        config.setProperty("id", "sessionDB");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:session_db;DB_CLOSE_DELAY=-1");

        ConnectionPlugin sessionPlugin = new ConnectionPlugin();
        sessionPlugin.initialize(config);
        sessionPlugin.execute(context);

        java.sql.Connection proxy = ConnectionPlugin.getNamedConnection(context, "sessionDB");
        java.sql.Connection physical = ((ConnectionProxy) proxy).getTargetConnection();
        session.markCompleted();

        assertTrue(physical.isClosed(),
                "The session event bus must release the named pool's physical connection");
    }

    @Test
    public void managedNamedConnectionRejectsNewLeaseAfterTerminationClosesAdmission() throws Exception {
        String sessionId = "session-db-managed";
        EventBus eventBus = new EventBus();
        ExecutionOwner owner = new ExecutionOwner("runtime-db-managed", sessionId, 1);
        ScraperSessionImpl session = new ScraperSessionImpl(
                sessionId, "test-client", mock(Config.class), eventBus, owner);
        ScraperContext managedContext = new ScraperContext();
        managedContext.setEventBus(eventBus);
        managedContext.setExecutionOwner(owner);
        managedContext.setSession(session);

        ResourceLedger ledger = new ResourceLedger("runtime-db-managed");
        LedgerExecutionResourceCoordinator coordinator =
                new LedgerExecutionResourceCoordinator(ledger, owner);
        managedContext.setResourceAccess(coordinator.getAccess());

        DefaultPluginConfiguration config = new DefaultPluginConfiguration("connection",
                "http://org.webharvest/schema/2.1/core", "connection");
        config.setProperty("id", "managedDB");
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", "jdbc:h2:mem:managed_db;DB_CLOSE_DELAY=-1");
        ConnectionPlugin managedPlugin = new ConnectionPlugin();
        managedPlugin.initialize(config);
        managedPlugin.execute(managedContext);

        java.sql.Connection physical;
        try (ConnectionPlugin.NamedConnectionLease lease =
                 ConnectionPlugin.acquireNamedConnection(managedContext, "managedDB")) {
            physical = lease.getConnection();
            coordinator.requestTermination();

            ConnectionPlugin.NamedConnectionLease unexpectedLease = null;
            try {
                unexpectedLease = ConnectionPlugin.acquireNamedConnection(managedContext, "managedDB");
            } catch (IllegalStateException expected) {
                // The facade has closed lease admission before per-resource abort fan-out completes.
            }
            if (unexpectedLease != null) {
                unexpectedLease.close();
                fail("A managed named connection must not admit a new lease after termination begins");
            }
        }

        coordinator.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
        assertTrue(physical.isClosed(),
                "The owner drain must close the managed pool after the active lease is released");
        assertEquals(ledger.size(), 0);
    }

}
