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

import com.google.common.eventbus.EventBus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;
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
                "<results><row><id>7</id><name>A&amp;B &lt;C&gt;</name></row></results>");
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
                "<results><row><id>7</id><name>A&amp;B &lt;C&gt;</name></row></results>");
        assertTrue(physicalConnection.isClosed(),
                "Legacy contexts without a session EventBus must still release the physical connection");
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
                "<results><row><empty></empty></row></results>");
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
                new String[]{"connection", "url", "driver", "username", "password", "sql"});
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
        assertEquals(plugin.execute(context).toString(), "<results><row><id>7</id></row></results>");

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
