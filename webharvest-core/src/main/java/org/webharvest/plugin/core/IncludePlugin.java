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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.PluginConfigurationTree;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.exception.ScraperExecutionExitException;
import org.webharvest.plugin.DefaultPluginBodyExecutor;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.PortType;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.templaters.BaseTemplater;

/**
 * Include external configuration processor - imports other XML config files.
 * 
 * <p>
 * The {@code <include>} processor imports and executes external WebHarvest configuration
 * files, enabling modular scraper design, code reuse, and separation of concerns.
 * Included configurations execute in the same context, sharing variables and state.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Example 1: Include common functions</h3>
 * <pre>{@code
 * <!-- main.xml -->
 * <config>
 *   <!-- Import shared functions -->
 *   <include path="functions/http-helpers.xml"/>
 *   <include path="functions/data-extractors.xml"/>
 *   
 *   <!-- Use imported functions -->
 *   <call name="fetchAndParse">
 *     <call-param name="url">https://example.com</call-param>
 *   </call>
 * </config>
 * }</pre>
 * 
 * <h3>Example 2: Modular configuration</h3>
 * <pre>{@code
 * <!-- main.xml -->
 * <config>
 *   <include path="config/constants.xml"/>     <!-- API keys, URLs -->
 *   <include path="scrapers/products.xml"/>    <!-- Product scraping logic -->
 *   <include path="scrapers/reviews.xml"/>     <!-- Review scraping logic -->
 *   <include path="export/formatters.xml"/>    <!-- Export functions -->
 * </config>
 * }</pre>
 * 
 * <h3>Example 3: Conditional include</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="environment" value="production"/>
 * 
 * <if condition="${environment == 'production'}">
 *   <include path="config/prod-settings.xml"/>
 * </if>
 * <else>
 *   <include path="config/dev-settings.xml"/>
 * </else>
 * </config>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Path resolution:</strong> Relative paths are resolved against the active configuration source, including nested file and URL configurations</li>
 *   <li><strong>Shared context:</strong> Included config runs in same context, shares variables</li>
 *   <li><strong>Execution timing:</strong> Included config executes immediately at include point</li>
 *   <li><strong>Recursive includes:</strong> Can include files that include other files</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <function>} - Alternative: define functions inline</li>
 *   <li>{@code <file>} - Read file content as data</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "include",
    description = "Include plugin that includes external configuration files",
    version = "2.1.0"
)
public class IncludePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.IncludePlugin");

    @Inject
    private ConfigFactory configFactory;

    /**
     * Metadata for IncludePlugin.
     * 
     * <p>Standalone plugin - includes external configuration files for modular scraper design.</p>
     */
    public static class IncludePluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "include";
        }

        @Override
        public String getName() {
            return "Include";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Includes external configuration files into the current configuration. Enables modular scraper design by allowing reuse of common configurations, shared logic, and templates. The included file must be a valid Web-Harvest configuration XML file.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = IncludePlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "include", "modular", "reuse", "configuration"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return IncludePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "include";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new IncludePluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-file-import";
        }
        
        @Override
        public String getColor() {
            return "#6B7280"; // Gray for utilities
        }
        
        @Override
        public String getGroup() {
            return "Utility";
        }
        
        @Override
        public int getDefaultWidth() {
            return 200;
        }
        
        @Override
        public int getDefaultHeight() {
            return 120;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("path", "Path to configuration file to include (from path attribute)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Result from included configuration execution", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Includes external configuration files into the current configuration. Enables modular scraper design by allowing reuse of common configurations, shared logic, and templates.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Rarely used directly
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Include Plugin",
                "Includes external configuration files into the current configuration. Enables modular scraper design.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return null; // Standalone plugin
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // No children
        }
    }
    
    /**
     * Configuration schema for IncludePlugin.
     */
    public static class IncludePluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[]{
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "id";
                    }

                    @Override
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.STRING;
                    }

                    @Override
                    public boolean isRequired() {
                        return false;
                    }

                    @Override
                    public String getDefaultValue() {
                        return null;
                    }

                    @Override
                    public String getDescription() {
                        return "Optional identifier for the include element";
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
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.FILE_PATH;
                    }

                    @Override
                    public boolean isRequired() {
                        return true;
                    }

                    @Override
                    public String getDefaultValue() {
                        return null;
                    }

                    @Override
                    public String getDescription() {
                        return "Path to the configuration file to include";
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
            return "2.3.0";
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
            return new String[0]; // No children
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
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
            
            String path = configuration.getProperty("path");
            if (path == null || path.trim().isEmpty()) {
                throw new PluginValidationException("Path attribute is required");
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new IncludePluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new IncludePluginMetadata().getConfigurationSchema();
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
        
        String path = configuration.getProperty("path");
        if (path == null || path.trim().isEmpty()) {
            throw new PluginValidationException("Path attribute is required");
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        final String configuredPath = getConfiguration().getProperty("path");
        final String path = BaseTemplater.evaluateToString(
                configuredPath, context.getScriptingLanguage(), context);
        if (path == null || path.trim().isEmpty()) {
            throw new org.webharvest.plugin.exceptions.IncludeExecutionException(
                    "IncludePlugin requires a non-empty 'path' attribute");
        }

        final Config parentConfig = context.getConfig();
        if (parentConfig == null || parentConfig.getConfigSource() == null) {
            throw new org.webharvest.plugin.exceptions.IncludeExecutionException(
                    "Cannot resolve include '" + path + "' because the active configuration source is unavailable");
        }
        if (configFactory == null) {
            throw new org.webharvest.plugin.exceptions.IncludeExecutionException(
                    "Cannot execute include '" + path
                    + "' because ConfigFactory was not injected; IncludePlugin requires the configured Guice injector");
        }

        try {
            final ConfigSource.Location location = new ConfigSource.Location() {
                @Override
                public String toString() {
                    return path;
                }
            };
            final ConfigSource includedSource =
                    parentConfig.getConfigSource().include(location);
            final Config includedConfig = configFactory.create(includedSource);
            includedConfig.reload();

            final PluginConfigurationTree tree = includedConfig.getConfigurationTree();
            if (tree == null || tree.getConfiguration() == null) {
                throw new IllegalStateException("Included configuration has no root element: " + path);
            }

            context.setConfig(includedConfig);
            try {
                LOGGER.debug("Executing included configuration: {}", includedSource.getLocation());
                return new DefaultPluginBodyExecutor().executeChildren(
                        tree.getConfiguration().getChildren(), context);
            } finally {
                context.setConfig(parentConfig);
            }
        } catch (ScraperExecutionExitException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (org.webharvest.plugin.exceptions.IncludeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new org.webharvest.plugin.exceptions.IncludeExecutionException(
                    "Failed to include configuration '" + path + "'", e);
        }
    }
}





