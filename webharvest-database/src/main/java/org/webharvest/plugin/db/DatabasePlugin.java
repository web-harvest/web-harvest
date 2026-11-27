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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISING OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.database.StandaloneConnectionPool;
import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database plugin for SQL operations.
 * 
 * <p>Supports two modes of operation:</p>
 * 
 * <h2>1. Named Connection (recommended):</h2>
 * <pre>
 * &lt;connection id="mydb" driver="org.postgresql.Driver" 
 *             url="jdbc:postgresql://localhost/mydb"
 *             username="user" password="pass"/&gt;
 * 
 * &lt;database connection="mydb" sql="SELECT * FROM users"/&gt;
 * </pre>
 * 
 * <h3>2. Inline Connection (backward compatibility):</h3>
 * <pre>
 * &lt;database driver="org.h2.Driver"
 *           url="jdbc:h2:mem:test"
 *           username="sa"
 *           password=""
 *           queryTimeout="30"
 *           sql="SELECT * FROM table"/&gt;
 * </pre>
 *
 * <p>The optional {@code queryTimeout} attribute uses JDBC seconds. Zero (the
 * default) preserves the JDBC driver's timeout behavior. Managed executions
 * additionally register the active statement for best-effort cancellation.</p>
 * 
 * @author WebHarvest Team
 * @since 2.1.0
 */
@CorePlugin(elementName = "database", namespace = "http://org.webharvest/schema/2.1/core")
public class DatabasePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabasePlugin.class);

    private static final String URL_ATT_NAME = "url";
    private static final String DRIVER_ATT_NAME = "driver";
    private static final String USERNAME_ATT_NAME = "username";
    private static final String PASSWORD_ATT_NAME = "password";
    private static final String SQL_ATT_NAME = "sql";
    private static final String CONNECTION_ATT_NAME = "connection";
    private static final String QUERY_TIMEOUT_ATT_NAME = "queryTimeout";
    
    /**
     * Default constructor - no dependency injection needed anymore.
     * DatabasePlugin now uses named connections from ConnectionPlugin or inline connection parameters.
     */
    public DatabasePlugin() {
        // No dependencies needed
    }
    
    /**
     * Evaluates template expressions in the given string.
     */
    private String evaluateTemplate(String template, DynamicScopeContext context) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        
        try {
            return org.webharvest.runtime.templaters.BaseTemplater.evaluateToString(template, context.getScriptingLanguage(), context);
        } catch (Exception e) {
            LOGGER.warn("Failed to evaluate template '{}': {}", template, e.getMessage());
            return template; // Return original if evaluation fails
        }
    }

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getName() {
            return "Database Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for database operations";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getClassName() {
            return DatabasePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return false;
        }

        @Override
        public String getElementName() {
            return "database";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String[] getTags() {
            return new String[0];
        }

        @Override
        public String getCategory() {
            return "Database";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getId() {
            return "DatabasePlugin";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public AttributeDefinition[] getAttributeDefinitions() {
                    return new AttributeDefinition[0];
                }

                @Override
                public String[] getRequiredAttributes() {
                    return new String[0];
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[] { CONNECTION_ATT_NAME, URL_ATT_NAME, DRIVER_ATT_NAME,
                            USERNAME_ATT_NAME, PASSWORD_ATT_NAME, SQL_ATT_NAME, QUERY_TIMEOUT_ATT_NAME };
                }

                @Override
                public String getVersion() {
                    return "2.1.0";
                }

                @Override
                public boolean allowsBody() {
                    return true;
                }

                @Override
                public String[] getRequiredSubElements() {
                    return new String[0];
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[0];
                }

                @Override
                public AttributeDefinition getAttribute(String name) {
                    return null;
                }

                @Override
                public void validate(PluginConfiguration configuration) throws PluginValidationException {
                    if (configuration == null) {
                        throw new PluginValidationException("Configuration cannot be null");
                    }
                    
                    String connection = configuration.getProperty(CONNECTION_ATT_NAME, "").trim();
                    String driver = configuration.getProperty(DRIVER_ATT_NAME, "").trim();
                    String url = configuration.getProperty(URL_ATT_NAME, "").trim();
                    String username = configuration.getProperty(USERNAME_ATT_NAME, "").trim();
                    String password = configuration.getProperty(PASSWORD_ATT_NAME, "").trim();

                    if (!connection.isEmpty()) {
                        if (!driver.isEmpty() || !url.isEmpty() || !username.isEmpty() || !password.isEmpty()) {
                            throw new PluginValidationException(
                                    "Named connection mode ('connection') cannot be combined with inline JDBC attributes");
                        }
                    } else if (driver.isEmpty() || url.isEmpty()) {
                        throw new PluginValidationException(
                                "Database requires either 'connection' or both 'driver' and 'url'");
                    }
                }
            };
        }
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return METADATA.getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        doValidateConfiguration(configuration);
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        getConfigurationSchema().validate(configuration);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        String connectionId = config.getProperty(CONNECTION_ATT_NAME, "").trim();
        String sql = config.getProperty(SQL_ATT_NAME, "");
        int queryTimeoutSeconds = parseQueryTimeout(config.getProperty(QUERY_TIMEOUT_ATT_NAME, "0"));
        
        // Evaluate SQL template
        sql = evaluateTemplate(sql, context);
        if (sql == null) {
            sql = "";
        }
        
        // If no SQL in attribute, get from body
        if (sql.trim().isEmpty()) {
            try {
                sql = getBodyContent(context);
            } catch (PluginException e) {
                throw new RuntimeException("Error getting body content: " + e.getMessage(), e);
            }
        }
        
        Connection connection = null;
        ConnectionPlugin.NamedConnectionLease namedConnectionLease = null;
        StandaloneConnectionPool inlineFactory = null;
        ExecutionResourceAccess.OperationResource inlineResource = null;
        ExecutionResourceAccess.Lease inlineLease = null;
        Throwable primaryFailure = null;
        try {
            // Option 1: Use a named session resource and hold a lease for the full SQL operation.
            if (!connectionId.isEmpty()) {
                LOGGER.info("Using named connection: {}", connectionId);
                namedConnectionLease = ConnectionPlugin.acquireNamedConnection(context, connectionId);
                connection = namedConnectionLease.getConnection();
            }
            // Option 2: Inline connection (backward compatibility), owned by this operation.
            else {
                String url = config.getProperty(URL_ATT_NAME);
                String driver = config.getProperty(DRIVER_ATT_NAME);
                String username = config.getProperty(USERNAME_ATT_NAME, "");
                String password = config.getProperty(PASSWORD_ATT_NAME, "");

                url = evaluateTemplate(url, context);
                driver = evaluateTemplate(driver, context);
                username = evaluateTemplate(username, context);
                password = evaluateTemplate(password, context);

                LOGGER.info("Creating inline connection: driver={}, url={}", driver, url);
                inlineFactory = new StandaloneConnectionPool();
                ExecutionResourceAccess resourceAccess = context.getResourceAccess();
                if (resourceAccess != null) {
                    inlineResource = resourceAccess.registerOperationResource(
                        "jdbc/inline/" + UUID.randomUUID(), null, inlineFactory::closeAllConnections);
                    inlineLease = inlineResource.acquire();
                } else if (context.getEventBus() != null && context.getSession() != null) {
                    // Compatibility for contexts created outside the managed service.
                    inlineFactory.registerForSession(
                        context.getEventBus(), context.getSession().getSessionId());
                }
                connection = inlineFactory.getConnection(driver, url, username, password);
            }

            if (sql.trim().isEmpty()) {
                throw new RuntimeException("Database SQL is empty; provide the sql attribute or element body");
            }

            return executeSql(connection, sql, queryTimeoutSeconds, context.getResourceAccess());

        } catch (InterruptedException interrupted) {
            primaryFailure = interrupted;
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (SQLException e) {
            RuntimeException failure =
                new RuntimeException("Database operation failed: " + e.getMessage(), e);
            primaryFailure = failure;
            LOGGER.error("Database operation failed", e);
            throw failure;
        } catch (Exception e) {
            RuntimeException failure =
                new RuntimeException("Unexpected error in database plugin: " + e.getMessage(), e);
            primaryFailure = failure;
            LOGGER.error("Unexpected error in database plugin", e);
            throw failure;
        } catch (Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            Throwable cleanupFailure = null;
            try {
                if (inlineLease != null) {
                    inlineLease.close();
                }
            } catch (RuntimeException | Error failure) {
                cleanupFailure = failure;
            }

            try {
                if (inlineResource != null) {
                    inlineResource.close();
                    if (inlineResource.getState() != ExecutionResourceAccess.ResourceState.CLOSED) {
                        LOGGER.warn("Inline JDBC resource {} ended in state {}",
                            inlineResource.getResourceId(), inlineResource.getState());
                        if (inlineResource.getCloseFailure() != null) {
                            LOGGER.warn("Inline JDBC resource close failed",
                                inlineResource.getCloseFailure());
                        }
                    }
                } else if (inlineFactory != null
                    && context.getResourceAccess() == null
                    && (context.getEventBus() == null || context.getSession() == null)) {
                    inlineFactory.closeAllConnections();
                }
            } catch (RuntimeException | Error failure) {
                cleanupFailure = combineFailures(cleanupFailure, failure);
            }

            try {
                if (namedConnectionLease != null) {
                    namedConnectionLease.close();
                }
                // Legacy contexts have no ledger lease to retire the named pool.
                // Keep the historical physical-connection cleanup in that mode.
                if (!connectionId.isEmpty()
                    && context.getResourceAccess() == null
                    && (context.getEventBus() == null || context.getSession() == null)) {
                    ConnectionPlugin.closeNamedConnection(context, connectionId);
                }
            } catch (RuntimeException | Error failure) {
                cleanupFailure = combineFailures(cleanupFailure, failure);
            }

            if (cleanupFailure != null) {
                if (primaryFailure != null) {
                    if (primaryFailure != cleanupFailure) {
                        primaryFailure.addSuppressed(cleanupFailure);
                    }
                } else if (cleanupFailure instanceof RuntimeException) {
                    throw (RuntimeException) cleanupFailure;
                } else {
                    throw (Error) cleanupFailure;
                }
            }
        }
    }

    private int parseQueryTimeout(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            int seconds = Integer.parseInt(value.trim());
            if (seconds < 0) {
                throw new IllegalArgumentException("Database queryTimeout must be zero or a positive integer in seconds");
            }
            return seconds;
        } catch (NumberFormatException invalidTimeout) {
            throw new IllegalArgumentException(
                "Database queryTimeout must be zero or a positive integer in seconds", invalidTimeout);
        }
    }

    private Throwable combineFailures(Throwable primary, Throwable next) {
        if (primary == null) {
            return next;
        }
        if (primary != next) {
            primary.addSuppressed(next);
        }
        return primary;
    }

    private Variable executeSql(Connection connection, String sql, int queryTimeoutSeconds,
                                 ExecutionResourceAccess resourceAccess) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        ExecutionResourceAccess.OperationResource statementResource = null;
        ExecutionResourceAccess.Lease statementLease = null;
        CountDownLatch statementCloseCompleted = null;
        AtomicReference<Throwable> statementCloseFailure = new AtomicReference<>();

        Throwable primaryFailure = null;
        try {
            statement = connection.prepareStatement(sql);
            if (queryTimeoutSeconds > 0) {
                statement.setQueryTimeout(queryTimeoutSeconds);
            }
            if (resourceAccess != null) {
                PreparedStatement activeStatement = statement;
                CountDownLatch closeCompleted = new CountDownLatch(1);
                statementCloseCompleted = closeCompleted;
                statementResource = resourceAccess.registerOperationResource(
                    "jdbc/statement/" + UUID.randomUUID(),
                    activeStatement::cancel,
                    () -> {
                        try {
                            activeStatement.close();
                        } catch (SQLException | RuntimeException failure) {
                            statementCloseFailure.set(failure);
                            throw failure;
                        } finally {
                            closeCompleted.countDown();
                        }
                    });
                statementLease = statementResource.acquire();
            }

            if (sql.trim().toLowerCase().startsWith("select")) {
                // Query operation
                resultSet = statement.executeQuery();
                StringBuilder result = new StringBuilder();
                result.append("<results>");
                
                while (resultSet.next()) {
                    result.append("<row>");
                    int columnCount = resultSet.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = resultSet.getMetaData().getColumnLabel(i);
                        String value = resultSet.getString(i);
                        result.append("<column name=\"")
                              .append(escapeXml(columnName != null ? columnName : ""))
                              .append("\">")
                              .append(escapeXml(value != null ? value : ""))
                              .append("</column>");
                    }
                    result.append("</row>");
                }
                
                result.append("</results>");
                return new NodeVariable(result.toString());
                
            } else {
                // Update/Insert/Delete operation
                int rowsAffected = statement.executeUpdate();
                return new NodeVariable("Rows affected: " + rowsAffected);
            }
            
        } catch (SQLException failure) {
            primaryFailure = failure;
            throw failure;
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            SQLException cleanupFailure = null;
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException failure) {
                    cleanupFailure = failure;
                }
            }
            if (statementLease != null) {
                try {
                    statementLease.close();
                } catch (RuntimeException | Error failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = new SQLException("Failed to release JDBC statement lease", failure);
                    } else {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
            }
            if (statementResource != null) {
                try {
                    statementResource.close();
                } catch (RuntimeException | Error failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = new SQLException("Failed to close managed JDBC statement", failure);
                    } else {
                        cleanupFailure.addSuppressed(failure);
                    }
                } finally {
                    // The connection lease is released by doExecute only after this
                    // callback completes, so a delayed abort cannot race pool close.
                    awaitStatementClose(statementCloseCompleted);
                }
                Throwable closeFailure = statementCloseFailure.get();
                if (closeFailure != null) {
                    SQLException failure = new SQLException("Failed to close managed JDBC statement", closeFailure);
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    } else {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
            } else if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    } else if (cleanupFailure != failure) {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
            }
            if (cleanupFailure != null) {
                if (primaryFailure != null) {
                    if (primaryFailure != cleanupFailure) {
                        primaryFailure.addSuppressed(cleanupFailure);
                    }
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    private void awaitStatementClose(CountDownLatch completed) {
        boolean interrupted = false;
        while (true) {
            try {
                completed.await();
                break;
            } catch (InterruptedException interruption) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String getBodyContent(DynamicScopeContext context) throws InterruptedException, PluginException {
        Variable bodyContent = executeBody(context);
        if (bodyContent == null || bodyContent instanceof EmptyVariable) {
            return "";
        }
        return bodyContent.toString(context.getCharset());
    }
}

