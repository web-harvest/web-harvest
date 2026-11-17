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

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.database.ConnectionFactory;
import org.webharvest.runtime.database.StandaloneConnectionPool;
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
 * <h3>1. Named Connection (recommended):</h3>
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
 *           sql="SELECT * FROM table"/&gt;
 * </pre>
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
                            USERNAME_ATT_NAME, PASSWORD_ATT_NAME, SQL_ATT_NAME };
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
        try {
            // Option 1: Use named connection from ConnectionPlugin
            if (!connectionId.isEmpty()) {
                LOGGER.info("Using named connection: {}", connectionId);
                connection = ConnectionPlugin.getNamedConnection(context, connectionId);
            } 
            // Option 2: Inline connection (backward compatibility)
            else {
                String url = config.getProperty(URL_ATT_NAME);
                String driver = config.getProperty(DRIVER_ATT_NAME);
                String username = config.getProperty(USERNAME_ATT_NAME, "");
                String password = config.getProperty(PASSWORD_ATT_NAME, "");
                
                // Evaluate template expressions
                url = evaluateTemplate(url, context);
                driver = evaluateTemplate(driver, context);
                username = evaluateTemplate(username, context);
                password = evaluateTemplate(password, context);
                
                LOGGER.info("Creating inline connection: driver={}, url={}", driver, url);
                
                // Use StandaloneConnectionPool for inline connections
                ConnectionFactory factory = new StandaloneConnectionPool();
                connection = factory.getConnection(driver, url, username, password);
            }
            
            if (sql.trim().isEmpty()) {
                throw new RuntimeException("Database SQL is empty; provide the sql attribute or element body");
            }

            // Execute SQL
            return executeSql(connection, sql);
            
        } catch (SQLException e) {
            LOGGER.error("Database operation failed", e);
            throw new RuntimeException("Database operation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in database plugin", e);
            throw new RuntimeException("Unexpected error in database plugin: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.warn("Failed to close database connection", e);
                }
            }
        }
    }

    private Variable executeSql(Connection connection, String sql) throws SQLException, PluginException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            statement = connection.prepareStatement(sql);
            
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
                        result.append("<").append(columnName).append(">")
                              .append(escapeXml(value != null ? value : ""))
                              .append("</").append(columnName).append(">");
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
            
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOGGER.warn("Failed to close result set", e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOGGER.warn("Failed to close statement", e);
                }
            }
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
        return bodyContent.toString();
    }
}

