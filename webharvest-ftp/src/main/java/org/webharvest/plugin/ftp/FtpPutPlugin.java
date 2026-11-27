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

import java.io.ByteArrayInputStream;
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
 * FTP file upload processor - uploads files to FTP server.
 * 
 * <p>
 * The {@code <ftp-put>} processor uploads files to FTP server.
 * Must be used as a child element of {@code <ftp>}.
 * </p>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <ftp host="ftp.example.com" username="user" password="pass">
 *     <ftp-put path="/upload/data.xml">
 *         <http url="https://example.com/data"/>
 *     </ftp-put>
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
@CorePlugin(elementName = "ftp-put")
public class FtpPutPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpPutPlugin.class);

    private static final String HOST_ATT_NAME = "host";
    private static final String PORT_ATT_NAME = "port";
    private static final String USERNAME_ATT_NAME = "user";
    private static final String PASSWORD_ATT_NAME = "password";
    private static final String PATH_ATT_NAME = "path";

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getName() {
            return "FTP Put Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for uploading files to FTP server";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getClassName() {
            return FtpPutPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "ftp-put";
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
            return "FtpPutPlugin";
        }

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

                @Override
                public boolean canAppearAtRoot() {
                    return false;
                }

                @Override
                public String[] getRequiredParents() {
                    return new String[]{"ftp"};
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
        
        LOGGER.debug("FTP Put Plugin - Host: " + host + ":" + port + ", User: " + username + ", Path: " + path);
        
        try (FtpClientResource ftpResource = FtpClientResource.open(context)) {
            // Create FTP connection
            FTPClient ftpClient = ftpResource.getClient();
            ftpClient.connect(host, port);
            
            if (!ftpClient.login(username, password)) {
                throw new PluginException("FTP login failed");
            }
            
            // Set file type to binary
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            
            // Get file content from body
            String content = getBodyContent(context);
            byte[] fileData = content.getBytes(getRuntimeCharset(context));
            
            // Upload file
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
            boolean success = ftpClient.storeFile(path, inputStream);
            
            if (!success) {
                throw new PluginException("Failed to upload file: " + path);
            }
            
            LOGGER.debug("FTP Put Plugin - Uploaded " + fileData.length + " bytes to " + path);
            
            return new NodeVariable("File uploaded successfully to " + path);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (IOException e) {
            LOGGER.error("FTP Put Plugin - IOException", e);
            throw new RuntimeException("FTP Put failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("FTP Put Plugin - Unexpected error", e);
            throw new RuntimeException("FTP Put failed: " + e.getMessage(), e);
        }
    }

    private String getRuntimeCharset(DynamicScopeContext context) {
        String charset = context.getCharset();
        return charset == null || charset.trim().isEmpty()
                ? StandardCharsets.UTF_8.name()
                : charset;
    }

    private String getBodyContent(DynamicScopeContext context) throws InterruptedException, PluginException {
        Variable bodyContent = executeBody(context);
        if (bodyContent == null || bodyContent instanceof EmptyVariable) {
            return "";
        }
        return bodyContent.toString(getRuntimeCharset(context));
    }
}










