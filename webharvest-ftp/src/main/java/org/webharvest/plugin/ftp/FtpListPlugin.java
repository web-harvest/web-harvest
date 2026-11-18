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

package org.webharvest.plugin.ftp;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FTP directory listing processor - lists files on FTP server.
 * 
 * <p>
 * The {@code <ftp-list>} processor lists files in FTP directory.
 * Must be used as a child element of {@code <ftp>}.
 * </p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <ftp host="ftp.example.com" username="user" password="pass">
 *     <def var="fileList">
 *         <ftp-list path="/data/"/>
 *     </def>
 *     
 *     <loop item="filename">
 *         ${fileList}
 *         <ftp-get path="/data/${filename}"/>
 *     </loop>
 * </ftp>
 * </config>
 * }</pre>
 * 
 * <p>See {@link FtpPlugin} for complete FTP examples.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 * @see FtpPlugin
 */
@CorePlugin(elementName = "ftp-list")
public class FtpListPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpListPlugin.class);

    private static final String HOST_ATT_NAME = "host";
    private static final String PORT_ATT_NAME = "port";
    private static final String USERNAME_ATT_NAME = "user";
    private static final String PASSWORD_ATT_NAME = "password";
    private static final String PATH_ATT_NAME = "path";

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getName() {
            return "FTP List Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for listing files in FTP server directory";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getClassName() {
            return FtpListPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "ftp-list";
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
            return "FTP";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getId() {
            return "FtpListPlugin";
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
                    return new String[] { HOST_ATT_NAME, USERNAME_ATT_NAME, PASSWORD_ATT_NAME };
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[] { PORT_ATT_NAME, PATH_ATT_NAME };
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
                public String[] getRequiredParents() {
                    return new String[]{"ftp"};
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
                    
                    String[] required = getRequiredAttributes();
                    for (String attr : required) {
                        if (configuration.getProperty(attr) == null || configuration.getProperty(attr).trim().isEmpty()) {
                            throw new PluginValidationException("Required attribute '" + attr + "' is missing or empty");
                        }
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
        
        String host = config.getProperty(HOST_ATT_NAME);
        int port = Integer.parseInt(config.getProperty(PORT_ATT_NAME, "21"));
        String username = config.getProperty(USERNAME_ATT_NAME);
        String password = config.getProperty(PASSWORD_ATT_NAME);
        String path = config.getProperty(PATH_ATT_NAME, "/");
        
        // If no path in attribute, get from body
        if (path.equals("/")) {
            String bodyPath;
            try {
                bodyPath = getBodyContent(context);
            } catch (PluginException e) {
                throw new RuntimeException("Error getting body content: " + e.getMessage(), e);
            }
            if (!bodyPath.isEmpty()) {
                path = bodyPath;
            }
        }
        
        LOGGER.debug("FTP List Plugin - Host: " + host + ":" + port + ", User: " + username + ", Path: " + path);
        
        FTPClient ftpClient = null;
        try {
            // Create FTP connection
            ftpClient = new FTPClient();
            ftpClient.connect(host, port);
            
            if (!ftpClient.login(username, password)) {
                throw new PluginException("FTP login failed");
            }
            
            // Change to directory
            if (!ftpClient.changeWorkingDirectory(path)) {
                throw new PluginException("Failed to change to directory: " + path);
            }
            
            // List files
            FTPFile[] files = ftpClient.listFiles();
            
            StringBuilder result = new StringBuilder();
            result.append("<files>");
            
            for (FTPFile file : files) {
                result.append("<file>");
                result.append("<name>").append(file.getName()).append("</name>");
                result.append("<size>").append(file.getSize()).append("</size>");
                result.append("<type>").append(file.isDirectory() ? "directory" : "file").append("</type>");
                result.append("</file>");
            }
            
            result.append("</files>");
            
            LOGGER.debug("FTP List Plugin - Found " + files.length + " items in " + path);
            
            return new NodeVariable(result.toString());
            
        } catch (IOException e) {
            LOGGER.error("FTP List Plugin - IOException", e);
            throw new RuntimeException("FTP List failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("FTP List Plugin - Unexpected error", e);
            throw new RuntimeException("FTP List failed: " + e.getMessage(), e);
        } finally {
            if (ftpClient != null && ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close FTP connection", e);
                }
            }
        }
    }

    private String getBodyContent(DynamicScopeContext context) throws InterruptedException, PluginException {
        Variable bodyContent = executeBody(context);
        if (bodyContent == null || bodyContent instanceof EmptyVariable) {
            return "";
        }
        return bodyContent.toString();
    }
}










