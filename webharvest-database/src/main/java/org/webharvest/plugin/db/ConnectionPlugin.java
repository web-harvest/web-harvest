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

import java.util.HashMap;
import java.util.Map;

/**
 * Connection plugin for managing named database connections.
 * Allows defining multiple named connections with connection pooling.
 * 
 * Example usage:
 * <pre>
 * &lt;connection id="mainDB" driver="org.postgresql.Driver" 
 *             url="jdbc:postgresql://localhost/mydb"
 *             username="user" password="pass" poolSize="10"/&gt;
 * 
 * &lt;database connection="mainDB" sql="SELECT * FROM users"/&gt;
 * </pre>
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
@CorePlugin(elementName = "connection")
public class ConnectionPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPlugin.class);

    private static final String ID_ATT_NAME = "id";
    private static final String DRIVER_ATT_NAME = "driver";
    private static final String URL_ATT_NAME = "url";
    private static final String USERNAME_ATT_NAME = "username";
    private static final String PASSWORD_ATT_NAME = "password";
    private static final String POOL_SIZE_ATT_NAME = "poolSize";
    
    // Key prefix for storing connections in context
    private static final String CONNECTION_KEY_PREFIX = "__db_connection_";
    private static final String CONNECTION_FACTORY_KEY_PREFIX = "__db_factory_";

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        doValidateConfiguration(configuration);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        String id = config.getProperty(ID_ATT_NAME, "");
        String driver = config.getProperty(DRIVER_ATT_NAME, "");
        String url = config.getProperty(URL_ATT_NAME, "");
        String username = config.getProperty(USERNAME_ATT_NAME, "");
        String password = config.getProperty(PASSWORD_ATT_NAME, "");
        String poolSizeStr = config.getProperty(POOL_SIZE_ATT_NAME, "10");
        
        LOGGER.info("Creating connection: id=" + id + ", driver=" + driver + ", url=" + url);
        
        try {
            // Create connection factory for this connection
            ConnectionFactory connectionFactory = new StandaloneConnectionPool();
            
            // Store factory in context for later use
            String factoryKey = CONNECTION_FACTORY_KEY_PREFIX + id;
            context.setLocalVar(factoryKey, new ConnectionFactoryVariable(connectionFactory, driver, url, username, password));
            
            LOGGER.info("Connection '" + id + "' registered successfully");
            
            return EmptyVariable.INSTANCE;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create connection: " + id, e);
            throw new RuntimeException("Failed to create connection '" + id + "': " + e.getMessage(), e);
        }
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String id = configuration.getProperty(ID_ATT_NAME, "");
        if (id.isEmpty()) {
            throw new PluginValidationException("Connection 'id' attribute is required");
        }
        
        String driver = configuration.getProperty(DRIVER_ATT_NAME, "");
        if (driver.isEmpty()) {
            throw new PluginValidationException("Connection 'driver' attribute is required");
        }
        
        String url = configuration.getProperty(URL_ATT_NAME, "");
        if (url.isEmpty()) {
            throw new PluginValidationException("Connection 'url' attribute is required");
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getName() { return "Connection Plugin"; }
            
            @Override
            public String getDescription() { 
                return "Plugin for managing named database connections with pooling support"; 
            }
            
            @Override
            public String getId() { return "connection"; }
            
            @Override
            public String getVersion() { return "2.2.0"; }
            
            @Override
            public String getAuthor() { return "Web-Harvest Team"; }
            
            @Override
            public String getCategory() { return "database"; }
            
            @Override
            public String[] getTags() { return new String[]{"database", "connection", "pool"}; }
            
            @Override
            public String[] getDependencies() { return new String[0]; }
            
            @Override
            public String getClassName() { return ConnectionPlugin.class.getName(); }
            
            @Override
            public boolean isCorePlugin() { return true; }
            
            @Override
            public String getElementName() { return "connection"; }
            
            @Override
            public String getNamespace() { return "http://org.webharvest/schema/2.1/database"; }
            
            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return ConnectionPlugin.this.getConfigurationSchema();
            }
        };
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public AttributeDefinition[] getAttributeDefinitions() {
                return new AttributeDefinition[0];
            }
            
            @Override
            public String getVersion() { return "2.2.0"; }
            
            @Override
            public boolean allowsBody() { return false; }
            
            @Override
            public String[] getRequiredSubElements() { return new String[0]; }
            
            @Override
            public String[] getAllowedSubElements() { return new String[0]; }
            
            @Override
            public String[] getOptionalAttributes() { 
                return new String[]{USERNAME_ATT_NAME, PASSWORD_ATT_NAME, POOL_SIZE_ATT_NAME}; 
            }
            
            @Override
            public String[] getRequiredAttributes() { 
                return new String[]{ID_ATT_NAME, DRIVER_ATT_NAME, URL_ATT_NAME}; 
            }
            
            @Override
            public AttributeDefinition getAttribute(String name) {
                for (AttributeDefinition attr : getAttributeDefinitions()) {
                    if (attr.getName().equals(name)) {
                        return attr;
                    }
                }
                return null;
            }
            
            @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                doValidateConfiguration(configuration);
            }
        };
    }
    
    /**
     * Variable wrapper for ConnectionFactory with connection parameters.
     */
    private static class ConnectionFactoryVariable extends NodeVariable {
        private final ConnectionFactory factory;
        private final String driver;
        private final String url;
        private final String username;
        private final String password;
        
        public ConnectionFactoryVariable(ConnectionFactory factory, String driver, 
                                        String url, String username, String password) {
            super("Connection[" + url + "]");
            this.factory = factory;
            this.driver = driver;
            this.url = url;
            this.username = username;
            this.password = password;
        }
        
        public Connection getConnection() throws Exception {
            return factory.getConnection(driver, url, username, password);
        }
    }
    
    /**
     * Helper method to get named connection from context.
     */
    public static Connection getNamedConnection(DynamicScopeContext context, String connectionId) throws Exception {
        String factoryKey = CONNECTION_FACTORY_KEY_PREFIX + connectionId;
        Variable var = context.getVar(factoryKey);
        
        if (var == null) {
            throw new RuntimeException("Connection '" + connectionId + "' not found. Define it using <connection id=\"" + connectionId + "\" .../>");
        }
        
        if (!(var instanceof ConnectionFactoryVariable)) {
            throw new RuntimeException("Variable '" + connectionId + "' is not a database connection");
        }
        
        ConnectionFactoryVariable factoryVar = (ConnectionFactoryVariable) var;
        return factoryVar.getConnection();
    }
}

