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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.net.ftp.FTPClient;
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
 * FTP file download processor - downloads files from FTP server.
 * 
 * <p>
 * The {@code <ftp-get>} processor downloads files from FTP server.
 * Must be used as a child element of {@code <ftp>}.
 * </p>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <ftp host="ftp.example.com" username="user" password="pass">
 *     <def var="fileContent">
 *         <ftp-get path="/data/products.xml"/>
 *     </def>
 *     
 *     <file path="products.xml" action="write">
 *         ${fileContent}
 *     </file>
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
@CorePlugin(elementName = "ftp-get")
public class FtpGetPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpGetPlugin.class);

    private static final String HOST_ATT_NAME = "host";
    private static final String PORT_ATT_NAME = "port";
    private static final String USERNAME_ATT_NAME = "user";
    private static final String PASSWORD_ATT_NAME = "password";
    private static final String PATH_ATT_NAME = "path";

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getName() {
            return "FTP Get Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for downloading files from FTP server";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getClassName() {
            return FtpGetPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "ftp-get";
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
            return "FtpGetPlugin";
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
        String path = config.getProperty(PATH_ATT_NAME, "");
        
        // If no path in attribute, get from body
        if (path.isEmpty()) {
            try {
                path = getBodyContent(context);
            } catch (PluginException e) {
                throw new RuntimeException("Error getting body content: " + e.getMessage(), e);
            }
        }
        
        LOGGER.debug("FTP Get Plugin - Host: " + host + ":" + port + ", User: " + username + ", Path: " + path);
        
        try (FtpClientResource ftpResource = FtpClientResource.open(context)) {
            // Create FTP connection
            FTPClient ftpClient = ftpResource.getClient();
            ftpClient.connect(host, port);
            
            if (!ftpClient.login(username, password)) {
                throw new PluginException("FTP login failed");
            }
            
            // Set file type to binary
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            
            // Download file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(path, outputStream);
            
            if (!success) {
                throw new PluginException("Failed to download file: " + path);
            }
            
            byte[] fileData = outputStream.toByteArray();
            LOGGER.debug("FTP Get Plugin - Downloaded " + fileData.length + " bytes");
            
            String charset = context.getCharset();
            if (charset == null || charset.trim().isEmpty()) {
                charset = StandardCharsets.UTF_8.name();
            }
            return new NodeVariable(new String(fileData, charset));
            
        } catch (IOException e) {
            LOGGER.error("FTP Get Plugin - IOException", e);
            throw new RuntimeException("FTP Get failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("FTP Get Plugin - Unexpected error", e);
            throw new RuntimeException("FTP Get failed: " + e.getMessage(), e);
        }
    }

    private String getBodyContent(DynamicScopeContext context) throws InterruptedException, PluginException {
        Variable bodyContent = super.executeBody(context);
        if (bodyContent == null || bodyContent instanceof EmptyVariable) {
            return "";
        }
        return bodyContent.toString();
    }
}










