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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FTP connection container processor - defines FTP connection for child operations.
 * 
 * <p>
 * The {@code <ftp>} processor establishes an FTP/FTPS connection and provides it to child
 * processors ({@code <ftp-get>}, {@code <ftp-put>}, {@code <ftp-list>}). Supports passive mode,
 * authentication, and binary/text transfers.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Download file from FTP</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <ftp host="ftp.example.com" 
 *      username="user" 
 *      password="pass">
 *     
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
 * <h4>Example 2: Upload scraped data to FTP</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="scrapedData">
 *     <http url="https://example.com/api/data"/>
 * </def>
 * 
 * <ftp host="ftp.backup.com" 
 *      username="backup_user" 
 *      password="backup_pass">
 *     
 *     <ftp-put path="/backups/data-${_date}.json">
 *         ${scrapedData}
 *     </ftp-put>
 * </ftp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: List and download all files</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <ftp host="ftp.example.com" username="user" password="pass">
 *     
 *     <def var="fileList">
 *         <ftp-list path="/data/"/>
 *     </def>
 *     
 *     <loop item="filename">
 *         ${fileList}
 *         
 *         <ftp-get path="/data/${filename}"/>
 *     </loop>
 * </ftp>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>host:</strong> FTP server hostname (required)</li>
 *   <li><strong>port:</strong> FTP port (default: 21)</li>
 *   <li><strong>username:</strong> FTP username (required)</li>
 *   <li><strong>password:</strong> FTP password (required)</li>
 *   <li><strong>passive:</strong> Use passive mode (default: true, firewall-friendly)</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Passive mode:</strong> Enabled by default for firewall compatibility</li>
 *   <li><strong>Connection reuse:</strong> Single connection shared by all child operations</li>
 *   <li><strong>Thread safety:</strong> Not thread-safe - use one FTP block per thread</li>
 *   <li><strong>FTPS support:</strong> Use port 990 for implicit SSL</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <ftp-get>} - Download files</li>
 *   <li>{@code <ftp-put>} - Upload files</li>
 *   <li>{@code <ftp-list>} - List directory</li>
 *   <li>{@code <file>} - Local file operations</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 */
@CorePlugin(elementName = "ftp")
public class FtpPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpPlugin.class);

    private static final String HOST_ATT_NAME = "host";
    private static final String PORT_ATT_NAME = "port";
    private static final String USERNAME_ATT_NAME = "username";
    private static final String PASSWORD_ATT_NAME = "password";
    private static final String ACTION_ATT_NAME = "action";
    private static final String REMOTE_PATH_ATT_NAME = "remotePath";
    private static final String LOCAL_PATH_ATT_NAME = "localPath";
    private static final String PASSIVE_ATT_NAME = "passive";

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getName() {
            return "FTP Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for FTP file operations";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getClassName() {
            return FtpPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "ftp";
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
            return "Network";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getId() {
            return "FtpPlugin";
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
                    return new String[] { HOST_ATT_NAME, USERNAME_ATT_NAME, PASSWORD_ATT_NAME, ACTION_ATT_NAME };
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[] { PORT_ATT_NAME, REMOTE_PATH_ATT_NAME, LOCAL_PATH_ATT_NAME, PASSIVE_ATT_NAME };
                }

                @Override
                public String getVersion() {
                    return "2.1.0";
                }

                @Override
                public boolean allowsBody() {
                    return false;
                }

                @Override
                public String[] getRequiredSubElements() {
                    return new String[0];
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[]{"ftp-get", "ftp-put", "ftp-list"};
                }

                @Override
                public AttributeDefinition getAttribute(String name) {
                    return null;
                }

                @Override
                public void validate(PluginConfiguration configuration) throws PluginValidationException {
                    // Basic validation
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
        // Validate configuration first
        doValidateConfiguration(configuration);
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        // Validate using schema
        getConfigurationSchema().validate(configuration);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        String host = config.getProperty(HOST_ATT_NAME);
        int port = Integer.parseInt(evaluateTemplate(config.getProperty(PORT_ATT_NAME, "21"), context));
        String username = config.getProperty(USERNAME_ATT_NAME);
        String password = config.getProperty(PASSWORD_ATT_NAME);
        String action = config.getProperty(ACTION_ATT_NAME);
        String remotePath = config.getProperty(REMOTE_PATH_ATT_NAME, "");
        String localPath = config.getProperty(LOCAL_PATH_ATT_NAME, "");
        boolean passive = getConfiguration().getBooleanProperty(PASSIVE_ATT_NAME, true);
        
        // Evaluate template expressions
        host = evaluateTemplate(host, context);
        username = evaluateTemplate(username, context);
        password = evaluateTemplate(password, context);
        action = evaluateTemplate(action, context);
        remotePath = evaluateTemplate(remotePath, context);
        localPath = evaluateTemplate(localPath, context);
        
        LOGGER.debug("FTP Plugin - Host: {}, Port: {}, Username: {}, Action: {}", host + ":" + port + " " + username + " " + action);
        
        FTPClient ftpClient = new FTPClient();
        
        try {
            // Connect to FTP server
            ftpClient.connect(host, port);
            int replyCode = ftpClient.getReplyCode();
            
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                throw new PluginException("FTP server refused connection. Reply code: " + replyCode);
            }
            
            // Login
            boolean loginSuccess = ftpClient.login(username, password);
            if (!loginSuccess) {
                throw new PluginException("FTP login failed");
            }
            
            // Set passive mode
            if (passive) {
                ftpClient.enterLocalPassiveMode();
            }
            
            // Set file type
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            // Execute action
            Variable result = executeAction(ftpClient, action, remotePath, localPath, context);
            
            return result;
            
        } catch (IOException e) {
            LOGGER.error("FTP operation failed", e);
            throw new RuntimeException("FTP operation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in FTP plugin", e);
            throw new RuntimeException("Unexpected error in FTP plugin: " + e.getMessage(), e);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                LOGGER.warn("Error closing FTP connection", e);
            }
        }
    }

    private Variable executeAction(FTPClient ftpClient, String action, String remotePath, String localPath, DynamicScopeContext context) throws IOException, InterruptedException, PluginException {
        switch (action.toLowerCase(Locale.ROOT)) {
            case "get":
                return handleFtpGet(ftpClient, remotePath, localPath, context);
            case "put":
                return handleFtpPut(ftpClient, remotePath, localPath, context);
            case "list":
                return handleFtpList(ftpClient, remotePath, context);
            case "mkdir":
                return handleFtpMkdir(ftpClient, remotePath, context);
            case "rmdir":
                return handleFtpRmdir(ftpClient, remotePath, context);
            case "del":
                return handleFtpDel(ftpClient, remotePath, context);
            default:
                throw new PluginException("Unknown FTP action: " + action);
        }
    }

    private Variable handleFtpGet(FTPClient ftpClient, String remotePath, String localPath, DynamicScopeContext context) throws IOException, InterruptedException, PluginException {
        LOGGER.debug("FTP GET - Remote: {}, Local: {}", remotePath, localPath);
        
        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new PluginException("Remote path is required for FTP GET operation");
        }
        
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(remotePath, byteOutputStream);
            byteOutputStream.close();
            
            if (!success) {
                throw new PluginException("Cannot retrieve file \"" + remotePath + "\" from FTP server");
            }
            
            byte[] bytes = byteOutputStream.toByteArray();
            return new NodeVariable(bytes);
            
        } catch (IOException e) {
            LOGGER.error("FTP GET operation failed", e);
            throw new PluginException("FTP GET operation failed: " + e.getMessage(), e);
        }
    }

    private Variable handleFtpPut(FTPClient ftpClient, String remotePath, String localPath, DynamicScopeContext context) throws IOException, InterruptedException, PluginException {
        LOGGER.debug("FTP PUT - Remote: {}, Local: {}", remotePath, localPath);
        
        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new PluginException("Remote path is required for FTP PUT operation");
        }
        
        try {
            // Get body content for file data
            Variable bodyVariable = executeBody(context);
            String bodyContent = bodyVariable != null ? bodyVariable.toString() : "";
            if (bodyContent == null || bodyContent.trim().isEmpty()) {
                throw new PluginException("Body content is required for FTP PUT operation");
            }
            
            // Convert to bytes using UTF-8 charset (can be made configurable)
            byte[] fileData = bodyContent.getBytes("UTF-8");
            ByteArrayInputStream stream = new ByteArrayInputStream(fileData);
            
            boolean success = ftpClient.storeFile(remotePath, stream);
            stream.close();
            
            if (!success) {
                throw new PluginException("Cannot store file \"" + remotePath + "\" to FTP server");
            }
            
            return EmptyVariable.INSTANCE;
            
        } catch (IOException e) {
            LOGGER.error("FTP PUT operation failed", e);
            throw new PluginException("FTP PUT operation failed: " + e.getMessage(), e);
        }
    }

    private Variable handleFtpList(FTPClient ftpClient, String remotePath, DynamicScopeContext context) throws IOException, InterruptedException, PluginException {
        LOGGER.debug("FTP LIST - Remote: {}", remotePath);
        
        try {
            FTPFile[] files = ftpClient.listFiles(remotePath);
            if (files == null) {
                return EmptyVariable.INSTANCE;
            }
            
            List<String> filenameList = new ArrayList<String>();
            for (FTPFile ftpFile : files) {
                if (ftpFile.isFile() || ftpFile.isDirectory() || ftpFile.isSymbolicLink()) {
                    filenameList.add(ftpFile.getName());
                }
            }
            
            return new ListVariable(filenameList);
            
        } catch (IOException e) {
            LOGGER.error("FTP LIST operation failed", e);
            throw new PluginException("FTP LIST operation failed: " + e.getMessage(), e);
        }
    }

    private Variable handleFtpMkdir(FTPClient ftpClient, String remotePath, DynamicScopeContext context) throws IOException, InterruptedException, PluginException {
        LOGGER.debug("FTP MKDIR - Remote: {}", remotePath);
        
        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new PluginException("Remote path is required for FTP MKDIR operation");
        }
        
        try {
            boolean success = ftpClient.makeDirectory(remotePath);
            if (!success) {
                throw new PluginException("Cannot create directory \"" + remotePath + "\" on FTP server");
            }
            
            return EmptyVariable.INSTANCE;
            
        } catch (IOException e) {
            LOGGER.error("FTP MKDIR operation failed", e);
            throw new PluginException("FTP MKDIR operation failed: " + e.getMessage(), e);
        }
    }

    private Variable handleFtpRmdir(FTPClient ftpClient, String remotePath, DynamicScopeContext context) throws IOException, InterruptedException, PluginException {
        LOGGER.debug("FTP RMDIR - Remote: {}", remotePath);
        
        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new PluginException("Remote path is required for FTP RMDIR operation");
        }
        
        try {
            boolean success = ftpClient.removeDirectory(remotePath);
            if (!success) {
                throw new PluginException("Cannot remove directory \"" + remotePath + "\" on FTP server");
            }
            
            return EmptyVariable.INSTANCE;
            
        } catch (IOException e) {
            LOGGER.error("FTP RMDIR operation failed", e);
            throw new PluginException("FTP RMDIR operation failed: " + e.getMessage(), e);
        }
    }

    private Variable handleFtpDel(FTPClient ftpClient, String remotePath, DynamicScopeContext context) throws IOException, InterruptedException, PluginException {
        LOGGER.debug("FTP DEL - Remote: {}", remotePath);
        
        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new PluginException("Remote path is required for FTP DEL operation");
        }
        
        try {
            boolean success = ftpClient.deleteFile(remotePath);
            if (!success) {
                throw new PluginException("Cannot delete file \"" + remotePath + "\" on FTP server");
            }
            
            return EmptyVariable.INSTANCE;
            
        } catch (IOException e) {
            LOGGER.error("FTP DEL operation failed", e);
            throw new PluginException("FTP DEL operation failed: " + e.getMessage(), e);
        }
    }
    
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
}
