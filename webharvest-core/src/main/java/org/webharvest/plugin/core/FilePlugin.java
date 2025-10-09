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

package org.webharvest.plugin.core;

import com.google.inject.Inject;
import org.webharvest.exception.FileException;
import org.webharvest.ioc.WorkingDir;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.BodyContentProvider;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Types;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import org.webharvest.utils.CommonUtil;
import org.webharvest.utils.FileListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * File plugin that provides file operations like read, write, append, and list.
 * 
 * <h3>Bug #41 Fix (v2.2.1): Binary File Downloads</h3>
 * <p>
 * Fixed binary file corruption when downloading files (RAR, ZIP, images, etc.).
 * Now uses {@link NodeVariable#toBinary()} instead of {@code toString().getBytes()}
 * to preserve binary data without String conversion corruption.
 * </p>
 * 
 * @version 2.2.1
 */
@CorePlugin(
    elementName = "file",
    description = "File plugin that provides file operations",
    version = "2.2.1"
)
public class FilePlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.FilePlugin");
    
    @Inject
    @WorkingDir
    private String workingDir;

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "file";
            }

            @Override
            public String getName() {
                return "File Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "File plugin that provides file operations (read, write, append, list)";
            }

            @Override
            public String getAuthor() {
                return "Web-Harvest Team";
            }

            @Override
            public String getCategory() {
                return "core";
            }

            @Override
            public String[] getTags() {
                return new String[]{"core", "file", "filesystem", "read", "write", "list"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return FilePlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "file";
            }

            @Override
            public String getNamespace() {
                return "http://org.webharvest/schema/2.1/core";
            }

            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return new PluginConfigurationSchema() {
                    @Override
                    public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                        return new PluginConfigurationSchema.AttributeDefinition[]{
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "id";
                                }

                                @Override
                                public String getDescription() {
                                    return "Optional identifier for the file element";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.STRING;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return null;
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return null;
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "path";
                                }

                                @Override
                                public String getDescription() {
                                    return "Path to the file or directory";
                                }

                                @Override
                                public boolean isRequired() {
                                    return true;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.FILE_PATH;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return null;
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return null;
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "action";
                                }

                                @Override
                                public String getDescription() {
                                    return "File operation to perform (read, write, append, list)";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.ENUM;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return "read";
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return new String[]{"read", "write", "append", "list"};
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "type";
                                }

                                @Override
                                public String getDescription() {
                                    return "File type (text, binary)";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.ENUM;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return "text";
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return new String[]{"text", "binary"};
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "charset";
                                }

                                @Override
                                public String getDescription() {
                                    return "Character encoding for text files";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.STRING;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return "UTF-8";
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return null;
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "listfilter";
                                }

                                @Override
                                public String getDescription() {
                                    return "Filter pattern for listing files (supports * and ? wildcards)";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.REGEX;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return "";
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return null;
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "listfiles";
                                }

                                @Override
                                public String getDescription() {
                                    return "Whether to include files in listing";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.BOOLEAN;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return "true";
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return new String[]{"true", "false"};
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "listdirs";
                                }

                                @Override
                                public String getDescription() {
                                    return "Whether to include directories in listing";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.BOOLEAN;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return "true";
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return new String[]{"true", "false"};
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            },
                            new PluginConfigurationSchema.AttributeDefinition() {
                                @Override
                                public String getName() {
                                    return "listrecursive";
                                }

                                @Override
                                public String getDescription() {
                                    return "Whether to list files recursively";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.BOOLEAN;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return "false";
                                }

                                @Override
                                public String[] getAllowedValues() {
                                    return new String[]{"true", "false"};
                                }

                                @Override
                                public Number getMinValue() {
                                    return null;
                                }

                                @Override
                                public Number getMaxValue() {
                                    return null;
                                }

                                @Override
                                public String getPattern() {
                                    return null;
                                }
                            }
                        };
                    }

                    @Override
                    public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
                        for (PluginConfigurationSchema.AttributeDefinition attr : getAttributeDefinitions()) {
                            if (attr.getName().equals(name)) {
                                return attr;
                            }
                        }
                        return null;
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
                    public String[] getOptionalAttributes() {
                        return new String[]{"id", "action", "type", "charset", "listfilter", "listfiles", "listdirs", "listrecursive"};
                    }

                    @Override
                    public String[] getRequiredAttributes() {
                        return new String[]{"path"};
                    }

                    @Override
                    public void validate(PluginConfiguration configuration) throws PluginValidationException {
                        if (configuration == null) {
                            throw new PluginValidationException("Configuration cannot be null");
                        }
                        
                        // Validate required attributes
                        String path = configuration.getProperty("path");
                        if (path == null || path.trim().isEmpty()) {
                            throw new PluginValidationException("Path attribute is required");
                        }
                        
                        // Validate action
                        String action = configuration.getProperty("action", "read");
                        String[] allowedActions = {"read", "write", "append", "list"};
                        boolean validAction = false;
                        for (String allowed : allowedActions) {
                            if (allowed.equalsIgnoreCase(action)) {
                                validAction = true;
                                break;
                            }
                        }
                        if (!validAction) {
                            throw new PluginValidationException("Invalid action: " + action + ". Allowed values: " + 
                                String.join(", ", allowedActions));
                        }
                        
                        // Validate type
                        String type = configuration.getProperty("type", "text");
                        String[] allowedTypes = {"text", "binary"};
                        boolean validType = false;
                        for (String allowed : allowedTypes) {
                            if (allowed.equalsIgnoreCase(type)) {
                                validType = true;
                                break;
                            }
                        }
                        if (!validType) {
                            throw new PluginValidationException("Invalid type: " + type + ". Allowed values: " + 
                                String.join(", ", allowedTypes));
                        }
                    }
                };
            }
        };
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return getMetadata().getConfigurationSchema();
    }

    /**
     * Execute child elements directly in the same context (v2.2.x ULTRA-LEAN)
     * Uses new architecture: PluginBodyExecutor
     */
    private Variable executeChildElementsDirectly(DynamicScopeContext context) throws InterruptedException {
        // v2.2.x: Use AbstractPlugin.executeBody() which uses PluginBodyExecutor
        return super.executeBody(context);
    }

    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.info("FilePlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();
        LOGGER.info("FilePlugin.doExecute: config={}", config);
        LOGGER.info("FilePlugin.doExecute: getting action property");
        
        // Get file configuration and expand templates
        String action = config.getProperty("action", "read");
        LOGGER.info("FilePlugin.doExecute: action={}", action);
        String path = BaseTemplater.evaluateToString(config.getProperty("path", ""), null, context);
        LOGGER.info("FilePlugin.doExecute: path={}", path);
        String type = config.getProperty("type", "text");
        System.out.println("FilePlugin.doExecute: type=" + type);
        String charset = BaseTemplater.evaluateToString(config.getProperty("charset", context.getCharset()), null, context);
        System.out.println("FilePlugin.doExecute: charset=" + charset);
        System.out.println("FilePlugin.doExecute: getting listFilter property");
        String listFilter = BaseTemplater.evaluateToString(config.getProperty("listfilter", ""), null, context);
        System.out.println("FilePlugin.doExecute: listFilter=" + listFilter);
        System.out.println("FilePlugin.doExecute: getting listFiles property");
        boolean listFiles = CommonUtil.getBooleanValue(config.getProperty("listfiles", "true"), true);
        System.out.println("FilePlugin.doExecute: listFiles=" + listFiles);
        System.out.println("FilePlugin.doExecute: getting listDirs property");
        boolean listDirs = CommonUtil.getBooleanValue(config.getProperty("listdirs", "true"), true);
        System.out.println("FilePlugin.doExecute: listDirs=" + listDirs);
        System.out.println("FilePlugin.doExecute: getting listRecursive property");
        boolean listRecursive = CommonUtil.getBooleanValue(config.getProperty("listrecursive", "false"), false);
        System.out.println("FilePlugin.doExecute: listRecursive=" + listRecursive);
        System.out.println("FilePlugin.doExecute: about to enter try block");
        
        System.out.println("File plugin - Action: " + action + ", Path: " + path + ", Type: " + type + ", Charset: " + charset);
        
        try {
            System.out.println("FilePlugin.doExecute: inside try block");
            if (path.isEmpty()) {
                System.out.println("FilePlugin.doExecute: path is empty, returning EmptyVariable");
                LOGGER.warn("No file path specified for file plugin");
                return EmptyVariable.INSTANCE;
            }
            System.out.println("FilePlugin.doExecute: path is not empty, continuing");
            
            // Basic file operations based on action
            System.out.println("FilePlugin.doExecute: about to enter switch statement for action=" + action);
            switch (action.toLowerCase()) {
                case "read":
                    System.out.println("FilePlugin.doExecute: calling handleFileRead");
                    return handleFileRead(path, type, charset, context);
                case "write":
                    System.out.println("FilePlugin.doExecute: calling handleFileWrite");
                    return handleFileWrite(path, type, charset, false, context);
                case "append":
                    return handleFileWrite(path, type, charset, true, context);
                case "list":
                    return handleFileList(path, listFilter, listFiles, listDirs, listRecursive, context);
                default:
                    LOGGER.warn("Unknown file action: " + action);
                    return EmptyVariable.INSTANCE;
            }
        } catch (Exception e) {
            LOGGER.error("Error during file operation", e);
            return EmptyVariable.INSTANCE;
        }
    }

    private Variable handleFileRead(String path, String type, String charset, DynamicScopeContext context) {
        try {
            File file = new File(workingDir, path);
            if (!file.exists()) {
                LOGGER.warn("File does not exist: " + file.getAbsolutePath());
                return EmptyVariable.INSTANCE;
            }
            
            if (type.equals("binary")) {
                // For binary files, return empty for now
                System.out.println("Binary file read not implemented yet");
                return EmptyVariable.INSTANCE;
            } else {
                // For text files, read content
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), charset);
                return new NodeVariable(content);
            }
        } catch (Exception e) {
            LOGGER.error("Error reading file: " + path, e);
            return EmptyVariable.INSTANCE;
        }
    }

    private Variable handleFileWrite(String path, String type, String charset, boolean append, DynamicScopeContext context) {
        try {
            if (workingDir == null) {
                workingDir = System.getProperty("user.dir");
            }
            File file = new File(workingDir, path);
        System.out.println("FilePlugin.handleFileWrite: workingDir=" + workingDir);
        System.out.println("FilePlugin.handleFileWrite: file=" + file.getAbsolutePath());
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            // Bug #41 Fix: Get content from body, preserving binary data
            Variable bodyContent = null;
            try {
                // Execute child elements directly in the same context
                bodyContent = super.executeBody(context);
                System.out.println("FilePlugin.handleFileWrite: bodyContent type=" + 
                    (bodyContent != null ? bodyContent.getClass().getName() : "null"));
                
                if (bodyContent == null || bodyContent instanceof EmptyVariable) {
                    LOGGER.warn("FilePlugin: bodyContent is null or EmptyVariable!");
                    System.out.println("FilePlugin.handleFileWrite: bodyContent is null or EmptyVariable");
                }
            } catch (Exception e) {
                LOGGER.error("FilePlugin.handleFileWrite: Error executing body content: " + e.getMessage(), e);
                bodyContent = EmptyVariable.INSTANCE;
            }
            
            // Bug #41 Fix: Write binary or text data
            if (type.equals("binary")) {
                // Bug #41 Fix: For binary files, use toBinary() to avoid String corruption
                byte[] bytes;
                if (bodyContent != null && bodyContent instanceof NodeVariable) {
                    // Use toBinary() - preserves byte[] without String conversion
                    bytes = ((NodeVariable) bodyContent).toBinary();
                    LOGGER.info("FilePlugin: Writing binary data, {} bytes", bytes.length);
                } else {
                    // Fallback for non-NodeVariable
                    String content = bodyContent != null ? bodyContent.toString() : "";
                    bytes = content.getBytes(charset);
                }
                
                try (FileOutputStream fos = new FileOutputStream(file, append)) {
                    fos.write(bytes);
                }
            } else {
                // For text files, convert to String first
                String content = "";
                if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                    content = bodyContent.toString();
                    LOGGER.info("FilePlugin: Content to write: [{}]", content);
                    System.out.println("FilePlugin.handleFileWrite: content=" + content);
                } else {
                    // Try to get raw text from child elements as XML
                    LOGGER.info("FilePlugin.handleFileWrite: Trying XML conversion");
                    content = getChildElementsAsRawText();
                    LOGGER.info("FilePlugin.handleFileWrite: XML conversion result: [{}]", content);
                    System.out.println("FilePlugin.handleFileWrite: XML conversion result=" + content);
                }
                
                // If the content contains template variables (${...}), process them
                if (content.contains("${")) {
                    try {
                        Variable processedContent = BaseTemplater.evaluateToVariable(content, null, context);
                        content = processedContent.toString();
                        System.out.println("FilePlugin.handleFileWrite: processed template content=" + content);
                    } catch (Exception e) {
                        System.out.println("FilePlugin.handleFileWrite: Error processing template: " + e.getMessage());
                        LOGGER.warn("Error processing template content: " + e.getMessage());
                    }
                }
                
                try (FileOutputStream fos = new FileOutputStream(file, append)) {
                    fos.write(content.getBytes(charset));
                }
            }
            
            System.out.println("File written successfully: " + file.getAbsolutePath());
            return EmptyVariable.INSTANCE;
        } catch (Exception e) {
            LOGGER.error("Error writing file: " + path, e);
            return EmptyVariable.INSTANCE;
        }
    }

    private Variable handleFileList(String path, String listFilter, boolean listFiles, boolean listDirs, boolean listRecursive, DynamicScopeContext context) {
        try {
            File file = new File(workingDir, path);
            if (!file.exists()) {
                LOGGER.warn("Directory does not exist: " + file.getAbsolutePath());
                return EmptyVariable.INSTANCE;
            }
            
            if (!file.isDirectory()) {
                LOGGER.warn("Path is not a directory: " + file.getAbsolutePath());
                return EmptyVariable.INSTANCE;
            }
            
            List<String> fileList = new ArrayList<>();
            collectFiles(file, listFilter, listFiles, listDirs, listRecursive, fileList);
            
            if (fileList.isEmpty()) {
                return EmptyVariable.INSTANCE;
            }
            
            StringBuilder result = new StringBuilder();
            for (String fileName : fileList) {
                result.append(fileName).append("\n");
            }
            
            return new NodeVariable(result.toString());
        } catch (Exception e) {
            LOGGER.error("Error listing files: " + path, e);
            return EmptyVariable.INSTANCE;
        }
    }

    private void collectFiles(File dir, String listFilter, boolean listFiles, boolean listDirs, boolean listRecursive, List<String> fileList) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        CommandPromptFilenameFilter filenameFilter = new CommandPromptFilenameFilter(listFilter);
        
        for (File file : files) {
            if (file.isDirectory()) {
                if (listDirs) {
                    if (filenameFilter.accept(dir, file.getName())) {
                        fileList.add(file.getName() + "/");
                    }
                }
                if (listRecursive) {
                    collectFiles(file, listFilter, listFiles, listDirs, listRecursive, fileList);
                }
            } else if (file.isFile()) {
                if (listFiles) {
                    if (filenameFilter.accept(dir, file.getName())) {
                        fileList.add(file.getName());
                    }
                }
            }
        }
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

    /**
     * Appends binary data from multiple variables into a single result.
     * This method is used for binary file operations.
     * 
     * @param body the variable containing binary data to append
     * @return a NodeVariable containing the concatenated binary data
     */
    public NodeVariable appendBinary(Variable body) {
        if (body == null) {
            return new NodeVariable("");
        }

        byte[] result = null;

        for (Object var : body.toList()) {
            final Variable currVariable = (Variable) var;
            byte bytes[] = currVariable.toBinary();
            if (bytes != null) {
                if (result == null) {
                    result = bytes;
                } else {
                    byte[] newResult = new byte[result.length + bytes.length];
                    System.arraycopy(result, 0, newResult, 0, result.length);
                    System.arraycopy(bytes, 0, newResult, result.length,
                            bytes.length);
                    result = newResult;
                }
            }
        }

        return new NodeVariable(result);
    }

    /**
     * Command prompt style filename filter for file listing operations.
     * Supports wildcards: * (any characters), ? (single character), . (literal dot)
     */
    private class CommandPromptFilenameFilter implements FilenameFilter {
        Pattern pattern = Pattern.compile(".*");

        private CommandPromptFilenameFilter(String filter) {
            if (!CommonUtil.isEmpty(filter)) {
                StringBuilder buffer = new StringBuilder();
                for (int i = 0; i < filter.length(); i++) {
                    char ch = filter.charAt(i);
                    switch (ch) {
                    case '.':
                        buffer.append("\\.");
                        break;
                    case '*':
                        buffer.append(".*");
                        break;
                    case '?':
                        buffer.append(".");
                        break;
                    default:
                        buffer.append(ch);
                        break;
                    }
                }
                try {
                    pattern = Pattern.compile(buffer.toString());
                } catch (PatternSyntaxException e) {
                    pattern = Pattern.compile("");
                }
            }
        }

        public boolean accept(File dir, String name) {
            return pattern.matcher(name).matches();
        }
    }
    
    private String getChildElementsAsRawText() {
        // v2.2.x ULTRA-LEAN: Use _textContent property from PluginConfiguration
        String textContent = getConfiguration().getProperty("_textContent");
        if (textContent != null && !textContent.trim().isEmpty()) {
            LOGGER.info("FilePlugin.getChildElementsAsRawText: Found text content: [{}]", textContent);
            return textContent;
        }
        
        LOGGER.debug("FilePlugin.getChildElementsAsRawText: No text content available");
        return "";
    }
}