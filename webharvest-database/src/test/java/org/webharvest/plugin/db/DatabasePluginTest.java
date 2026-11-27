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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

import com.google.common.eventbus.EventBus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.definition.Config;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.resource.LedgerExecutionResourceCoordinator;
import org.webharvest.runtime.resource.ResourceLedger;
import org.webharvest.runtime.session.ExecutionOwner;
import org.webharvest.runtime.database.ConnectionProxy;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;

/** Contract and JDBC integration tests for DatabasePlugin. */
public class DatabasePluginTest {

    private static final String CORE_NAMESPACE = "http://org.webharvest/schema/2.1/core";
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        context = new org.webharvest.runtime.ScraperContext();
    }

    @Test
    public void testMetadataUsesCanonicalCoreElement() {
        DatabasePlugin plugin = new DatabasePlugin();
        assertEquals(plugin.getMetadata().getElementName(), "database");
        assertEquals(plugin.getMetadata().getNamespace(), CORE_NAMESPACE);
        assertFalse(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testInlineConnectionConfigurationIsValid() throws Exception {
        new DatabasePlugin().initialize(inlineConfig("jdbc:h2:mem:db_validation_inline"));
    }

    @Test
    public void testNamedConnectionConfigurationIsValid() throws Exception {
        DefaultPluginConfiguration config = config();
        config.setProperty("connection", "main");
        new DatabasePlugin().initialize(config);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testMissingConnectionModeIsRejected() throws Exception {
        new DatabasePlugin().initialize(config());
    }

    @Test(expectedExceptions = PluginException.class)
    public void testPartialInlineConnectionIsRejected() throws Exception {
        DefaultPluginConfiguration config = config();
        config.setProperty("driver", "org.h2.Driver");
        new DatabasePlugin().initialize(config);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testMixedNamedAndInlineConnectionModesAreRejected() throws Exception {
        DefaultPluginConfiguration config = inlineConfig("jdbc:h2:mem:db_mixed");
        config.setProperty("connection", "main");
        new DatabasePlugin().initialize(config);
    }

    @Test
    public void testInlineSelectReturnsExactEscapedXml() throws Exception {
        String url = "jdbc:h2:mem:db_inline_select;DB_CLOSE_DELAY=-1";
        createFixture(url);
        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, "SELECT id AS \"id\", name AS \"name\" FROM sample"));
        Variable result = plugin.execute(context);
        assertEquals(result.toString(),
                "<results><row><column name=\"id\">7</column><column name=\"name\">A&amp;B &lt;C&gt;</column></row></results>");
    }

    @Test
    public void testNamedConnectionExecutesSelect() throws Exception {
        String url = "jdbc:h2:mem:db_named_select;DB_CLOSE_DELAY=-1";
        createFixture(url);
        ConnectionPlugin connection = new ConnectionPlugin();
        DefaultPluginConfiguration connectionConfig = new DefaultPluginConfiguration(
                "connection", CORE_NAMESPACE, "connection");
        connectionConfig.setProperty("id", "main");
        connectionConfig.setProperty("driver", "org.h2.Driver");
        connectionConfig.setProperty("url", url);
        connectionConfig.setProperty("username", "sa");
        connectionConfig.setProperty("password", "");
        connection.initialize(connectionConfig);
        connection.execute(context);
        Connection trackedConnection = ConnectionPlugin.getNamedConnection(context, "main");
        Connection physicalConnection = ((ConnectionProxy) trackedConnection).getTargetConnection();

        DatabasePlugin plugin = new DatabasePlugin();
        DefaultPluginConfiguration databaseConfig = config();
        databaseConfig.setProperty("connection", "main");
        databaseConfig.setProperty("sql", "SELECT id AS \"id\", name AS \"name\" FROM sample");
        plugin.initialize(databaseConfig);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(),
                "<results><row><column name=\"id\">7</column><column name=\"name\">A&amp;B &lt;C&gt;</column></row></results>");
        assertTrue(physicalConnection.isClosed(),
                "Legacy contexts without a session EventBus must still release the physical connection");
    }

    @Test
    public void testManagedInlineConnectionUsesOperationResourceScope() throws Exception {
        String url = "jdbc:h2:mem:db_managed_inline;DB_CLOSE_DELAY=-1";
        createFixture(url);
        ResourceLedger ledger = new ResourceLedger("db-runtime");
        ExecutionOwner owner = new ExecutionOwner("db-runtime", "db-inline-session", 1);
        LedgerExecutionResourceCoordinator coordinator =
            new LedgerExecutionResourceCoordinator(ledger, owner);
        DynamicScopeContext managedContext = new ScraperContext();
        managedContext.setExecutionOwner(owner);
        managedContext.setResourceAccess(coordinator.getAccess());

        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, "SELECT id AS \"id\" FROM sample"));
        Variable result = plugin.execute(managedContext);

        assertEquals(result.toString(), "<results><row><column name=\"id\">7</column></row></results>");
        assertEquals(ledger.size(), 0,
            "An inline operation resource must be drained and retired when the SQL operation exits");
    }

    @Test
    public void testManagedNamedConnectionHoldsSessionResourceUntilOwnerDrain() throws Exception {
        String url = "jdbc:h2:mem:db_managed_named;DB_CLOSE_DELAY=-1";
        createFixture(url);
        ResourceLedger ledger = new ResourceLedger("db-runtime");
        ExecutionOwner owner = new ExecutionOwner("db-runtime", "db-named-session", 1);
        LedgerExecutionResourceCoordinator coordinator =
            new LedgerExecutionResourceCoordinator(ledger, owner);
        DynamicScopeContext managedContext = new ScraperContext();
        managedContext.setExecutionOwner(owner);
        managedContext.setResourceAccess(coordinator.getAccess());

        ConnectionPlugin connectionPlugin = new ConnectionPlugin();
        DefaultPluginConfiguration connectionConfig = new DefaultPluginConfiguration(
            "connection", CORE_NAMESPACE, "connection");
        connectionConfig.setProperty("id", "main");
        connectionConfig.setProperty("driver", "org.h2.Driver");
        connectionConfig.setProperty("url", url);
        connectionConfig.setProperty("username", "sa");
        connectionConfig.setProperty("password", "");
        connectionPlugin.initialize(connectionConfig);
        connectionPlugin.execute(managedContext);
        try {
            ConnectionPlugin.getNamedConnection(managedContext, "main");
            fail("Managed JDBC connections must require an execution resource lease");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("resource lease"));
        }
        try {
            ConnectionPlugin.closeNamedConnection(managedContext, "main");
            fail("Managed JDBC pools must not be closed outside their resource owner");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("resource owner"));
        }

        DatabasePlugin databasePlugin = new DatabasePlugin();
        DefaultPluginConfiguration databaseConfig = config();
        databaseConfig.setProperty("connection", "main");
        databaseConfig.setProperty("sql", "SELECT id AS \"id\" FROM sample");
        databasePlugin.initialize(databaseConfig);
        assertEquals(databasePlugin.execute(managedContext).toString(),
            "<results><row><column name=\"id\">7</column></row></results>");

        ConnectionPlugin.NamedConnectionLease activeUse =
            ConnectionPlugin.acquireNamedConnection(managedContext, "main");
        Connection tracked = activeUse.getConnection();
        Connection physical = ((ConnectionProxy) tracked).getTargetConnection();
        assertFalse(physical.isClosed());
        assertEquals(ledger.size(), 1,
            "The named pool remains registered at SESSION scope after each SQL operation");

        coordinator.requestTermination();
        assertFalse(physical.isClosed(),
            "A close request must not close a named pool while an operation lease is active");

        activeUse.close();
        activeUse.close(); // Lease close is deliberately idempotent for repeated cleanup paths.
        coordinator.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
        assertTrue(physical.isClosed());
        assertEquals(ledger.size(), 0);
    }

    @Test
    public void failedManagedNamedConnectionAcquisitionReleasesItsOperationLease() throws Exception {
        ResourceLedger ledger = new ResourceLedger("db-runtime");
        ExecutionOwner owner = new ExecutionOwner("db-runtime", "db-failed-acquire-session", 1);
        LedgerExecutionResourceCoordinator coordinator =
            new LedgerExecutionResourceCoordinator(ledger, owner);
        DynamicScopeContext managedContext = new ScraperContext();
        managedContext.setExecutionOwner(owner);
        managedContext.setResourceAccess(coordinator.getAccess());

        ConnectionPlugin connectionPlugin = new ConnectionPlugin();
        DefaultPluginConfiguration connectionConfig = new DefaultPluginConfiguration(
            "connection", CORE_NAMESPACE, "connection");
        connectionConfig.setProperty("id", "unavailable");
        connectionConfig.setProperty("driver", "org.h2.Driver");
        connectionConfig.setProperty("url", "jdbc:missing-driver:test");
        connectionPlugin.initialize(connectionConfig);
        connectionPlugin.execute(managedContext);

        DatabasePlugin databasePlugin = new DatabasePlugin();
        DefaultPluginConfiguration databaseConfig = config();
        databaseConfig.setProperty("connection", "unavailable");
        databaseConfig.setProperty("sql", "SELECT 1");
        databasePlugin.initialize(databaseConfig);

        try {
            databasePlugin.execute(managedContext);
            fail("Expected named connection acquisition to fail");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Unexpected error")
                || expected.getMessage().contains("Database operation failed"));
        }

        coordinator.requestTermination();
        coordinator.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
        assertEquals(ledger.size(), 0,
            "A failed JDBC acquisition must release its operation lease so owner drain can retire the pool");
    }

    @Test
    public void failedManagedInlineConnectionAcquisitionReleasesItsOperationLease() throws Exception {
        ResourceLedger ledger = new ResourceLedger("db-runtime");
        ExecutionOwner owner = new ExecutionOwner("db-runtime", "db-failed-inline-session", 1);
        LedgerExecutionResourceCoordinator coordinator =
            new LedgerExecutionResourceCoordinator(ledger, owner);
        DynamicScopeContext managedContext = new ScraperContext();
        managedContext.setExecutionOwner(owner);
        managedContext.setResourceAccess(coordinator.getAccess());

        DatabasePlugin databasePlugin = new DatabasePlugin();
        databasePlugin.initialize(inlineConfig("jdbc:missing-driver:test", "SELECT 1"));

        try {
            databasePlugin.execute(managedContext);
            fail("Expected inline JDBC connection acquisition to fail");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Unexpected error")
                || expected.getMessage().contains("Database operation failed"));
        }

        coordinator.requestTermination();
        coordinator.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
        assertEquals(ledger.size(), 0,
            "A failed inline JDBC acquisition must release its operation lease and retire its pool");
    }

    @Test
    public void testUpdateReturnsAffectedRowCount() throws Exception {
        String url = "jdbc:h2:mem:db_update;DB_CLOSE_DELAY=-1";
        createFixture(url);
        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, "UPDATE sample SET name = 'Updated' WHERE id = 7"));
        assertEquals(plugin.execute(context).toString(), "Rows affected: 1");
    }

    @Test
    public void testSelectWithNullValueReturnsEmptyElement() throws Exception {
        String url = "jdbc:h2:mem:db_null_value;DB_CLOSE_DELAY=-1";
        createFixture(url);
        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, "SELECT NULL AS \"empty\""));
        assertEquals(plugin.execute(context).toString(),
                "<results><row><column name=\"empty\"></column></row></results>");
    }

    @Test
    public void testSelectWithNonXmlColumnLabelsReturnsWellFormedXml() throws Exception {
        String url = "jdbc:h2:mem:db_invalid_column_labels;DB_CLOSE_DELAY=-1";
        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url,
                "SELECT 1 AS \"bad label\", 2 AS \"a:b\", 3 AS \"x/y\""));
        Variable result = plugin.execute(context);
        assertEquals(result.toString(),
                "<results><row><column name=\"bad label\">1</column>"
                + "<column name=\"a:b\">2</column><column name=\"x/y\">3</column></row></results>");
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(result.toString())));
    }

    @Test
    public void testSelectWithoutRowsReturnsEmptyResults() throws Exception {
        String url = "jdbc:h2:mem:db_no_rows;DB_CLOSE_DELAY=-1";
        createFixture(url);
        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, "SELECT id AS \"id\" FROM sample WHERE id = -1"));
        assertEquals(plugin.execute(context).toString(), "<results></results>");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testEmptySqlIsRejectedAfterConnectionSetup() throws Exception {
        String url = "jdbc:h2:mem:db_empty_sql;DB_CLOSE_DELAY=-1";
        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, ""));
        plugin.execute(context);
    }


    @Test
    public void testConfigurationSchemaContractAndValidationBranches() throws Exception {
        DatabasePlugin plugin = new DatabasePlugin();
        org.webharvest.plugin.PluginConfigurationSchema schema = plugin.getConfigurationSchema();

        assertEquals(schema.getAttributeDefinitions().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getOptionalAttributes(),
                new String[]{"connection", "url", "driver", "username", "password", "sql", "queryTimeout"});
        assertEquals(schema.getVersion(), "2.1.0");
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertNull(schema.getAttribute("connection"));

        try {
            schema.validate(null);
            fail("Expected null configuration to be rejected");
        } catch (org.webharvest.plugin.exceptions.PluginValidationException expected) {
            assertTrue(expected.getMessage().contains("cannot be null"));
        }

        DefaultPluginConfiguration named = config();
        named.setProperty("connection", "main");
        schema.validate(named);

        DefaultPluginConfiguration namedWithUsername = config();
        namedWithUsername.setProperty("connection", "main");
        namedWithUsername.setProperty("username", "sa");
        try {
            schema.validate(namedWithUsername);
            fail("Expected named and inline attributes to be rejected");
        } catch (org.webharvest.plugin.exceptions.PluginValidationException expected) {
            assertTrue(expected.getMessage().contains("cannot be combined"));
        }

        DefaultPluginConfiguration incompleteInline = config();
        incompleteInline.setProperty("driver", "org.h2.Driver");
        try {
            schema.validate(incompleteInline);
            fail("Expected incomplete inline configuration to be rejected");
        } catch (org.webharvest.plugin.exceptions.PluginValidationException expected) {
            assertTrue(expected.getMessage().contains("both 'driver' and 'url'"));
        }

        schema.validate(inlineConfig("jdbc:h2:mem:schema_valid"));
    }

    @Test
    public void testInlineQueryRegistersPoolForActualSessionCompletion() throws Exception {
        String url = "jdbc:h2:mem:db_event_bus_inline;DB_CLOSE_DELAY=-1";
        createFixture(url);
        EventBus eventBus = new EventBus();
        context.setEventBus(eventBus);
        ScraperSessionImpl session = new ScraperSessionImpl(
                "database-inline-session", "test-client", mock(Config.class), eventBus);
        context.setSession(session);
        session.markRunning(context);

        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, "SELECT id AS \"id\" FROM sample"));
        assertEquals(plugin.execute(context).toString(), "<results><row><column name=\"id\">7</column></row></results>");

        session.markCompleted();
        assertEquals(session.getStatus().name(), "COMPLETED");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testMissingNamedConnectionFailsAndRunsLegacyCleanupBranch() throws Exception {
        DatabasePlugin plugin = new DatabasePlugin();
        DefaultPluginConfiguration config = config();
        config.setProperty("connection", "missing");
        config.setProperty("sql", "SELECT 1");
        plugin.initialize(config);
        plugin.execute(context);
    }

    @Test
    public void sqlExecutionFailureIsReportedAndInlineResourceIsCleanedUp() throws Exception {
        String url = "jdbc:h2:mem:db_invalid_sql;DB_CLOSE_DELAY=-1";
        ResourceLedger ledger = new ResourceLedger("db-runtime");
        ExecutionOwner owner = new ExecutionOwner("db-runtime", "db-invalid-sql-session", 1);
        LedgerExecutionResourceCoordinator coordinator =
            new LedgerExecutionResourceCoordinator(ledger, owner);
        DynamicScopeContext managedContext = new ScraperContext();
        managedContext.setExecutionOwner(owner);
        managedContext.setResourceAccess(coordinator.getAccess());

        DatabasePlugin plugin = new DatabasePlugin();
        plugin.initialize(inlineConfig(url, "SELEC deliberately_invalid_sql"));

        try {
            plugin.execute(managedContext);
            fail("Expected invalid SQL to fail");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Unexpected error in database plugin"));
            Throwable cause = expected;
            while (cause != null && !(cause instanceof java.sql.SQLException)) {
                cause = cause instanceof java.lang.reflect.UndeclaredThrowableException
                    ? ((java.lang.reflect.UndeclaredThrowableException) cause).getUndeclaredThrowable()
                    : cause.getCause();
            }
            assertTrue(cause instanceof java.sql.SQLException,
                "The original JDBC failure must remain reachable through the proxy wrapper");
        }

        assertEquals(ledger.size(), 0,
            "The inline operation resource must be retired even when SQL execution fails");
    }


    private static void createFixture(String url) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (id INT PRIMARY KEY, name VARCHAR(100))");
            statement.execute("INSERT INTO sample (id, name) VALUES (7, 'A&B <C>')");
        }
    }

    private static DefaultPluginConfiguration config() {
        return new DefaultPluginConfiguration("database", CORE_NAMESPACE, "database");
    }

    private static DefaultPluginConfiguration inlineConfig(String url) {
        return inlineConfig(url, "SELECT 1 AS result");
    }

    private static DefaultPluginConfiguration inlineConfig(String url, String sql) {
        DefaultPluginConfiguration config = config();
        config.setProperty("driver", "org.h2.Driver");
        config.setProperty("url", url);
        config.setProperty("username", "sa");
        config.setProperty("password", "");
        config.setProperty("sql", sql);
        return config;
    }
}
