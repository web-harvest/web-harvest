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
import org.webharvest.plugin.AbstractPlugin;
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
import org.webharvest.plugin.annotations.MasterDetailConstraint;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception handling processor - try-catch blocks for robust error handling.
 * 
 * <p>
 * The {@code <try>} processor provides exception handling similar to Java try-catch.
 * It executes its body and if any exception occurs, the {@code <catch>} block is executed
 * with error details available in the {@code ${error}} variable.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Handle HTTP errors gracefully</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <try>
 *   <def var="data">
 *     <http url="https://api.example.com/data"/>
 *   </def>
 * </try>
 * <catch>
 *   <file path="errors.log" action="append">
 *     HTTP request failed: ${error}
 *     Timestamp: ${_date}
 *   </file>
 *   <def var="data" value=""/>  <!-- Use empty default -->
 * </catch>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Retry on failure</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="success" value="false"/>
 * <def var="retries" value="0"/>
 * 
 * <while condition="${!success && retries < 3}">
 *   <try>
 *     <http url="https://api.example.com/data"/>
 *     <def var="success" value="true"/>
 *   </try>
 *   <catch>
 *     <def var="retries">
 *       <script>parseInt(context.getVar("retries")) + 1</script>
 *     </def>
 *     <sleep milliseconds="2000"/>
 *   </catch>
 * </while>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Continue processing on XPath failure</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="product">
 *   <xpath expression="//product">...</xpath>
 *   
 *   <try>
 *     <def var="price">
 *       <xpath expression=".//price/text()">${product}</xpath>
 *     </def>
 *   </try>
 *   <catch>
 *     <def var="price" value="N/A"/>  <!-- Default if XPath fails -->
 *   </catch>
 *   
 *   <file path="products.txt" action="append">
 *     Product price: ${price}
 *   </file>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Error Variable</h3>
 * <p>Inside {@code <catch>} block, the {@code ${error}} variable contains error message.</p>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Catch is optional:</strong> Can use {@code <try>} without {@code <catch>} (errors swallowed)</li>
 *   <li><strong>Error variable:</strong> ${error} available only inside catch block</li>
 *   <li><strong>No rethrow:</strong> Exceptions are caught and handled, not propagated</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <catch>} - Exception handler block</li>
 *   <li>{@code <if>} - Conditional logic</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "try",
    description = "Try plugin that provides exception handling with try-catch functionality",
    version = "2.1.0",
    category = "Control"
)
@MasterDetailConstraint(
    detailType = "catch",
    maxOccurs = 1,
    minOccurs = 0,
    sectionTitle = "Catch Block",
    addLabel = "Add Catch",
    emptyLabel = "No catch block",
    primaryAttribute = "id",
    secondaryAttribute = "var",
    usesBody = true
)
public class TryPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.TryPlugin");

    /**
     * Metadata for TryPlugin.
     * 
     * <p>Master plugin - allows catch as child. Provides exception handling.</p>
     */
    public static class TryPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "try";
        }

        @Override
        public String getName() {
            return "Try";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Executes body content with exception handling. If an exception occurs, the catch block is executed with error details available in ${error} variable. Essential for robust error handling, graceful degradation, and fault-tolerant scraping.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = TryPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Control";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "try", "catch", "exception", "error-handling", "master"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return TryPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "try";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new TryPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-shield-alt";
        }
        
        @Override
        public String getColor() {
            return "#EF4444"; // Red for error handling
        }
        
        @Override
        public String getGroup() {
            return "Control Flow";
        }
        
        @Override
        public int getDefaultWidth() {
            return 220;
        }
        
        @Override
        public int getDefaultHeight() {
            return 180;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[0]; // No input ports
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Result from body execution (if no exception)", PortType.MIXED),
                new OutputPort("error", "Error details (if exception occurs, passed to catch)", PortType.STRING)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Executes body content with exception handling. If an exception occurs, the catch block is executed with error details available in ${error} variable. Essential for robust error handling, graceful degradation, and fault-tolerant scraping. Allows catch as child.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+T", "Add Try-Catch", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Try Plugin",
                "Executes body content with exception handling. If an exception occurs, the catch block is executed with error details available in ${error} variable.",
                new String[]{"Tab", "Enter", "Escape"}
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
            return new String[]{"catch"}; // Master plugin - allows catch child
        }

        @Override
        public boolean acceptsArbitraryChildren() {
            return true;
        }
    }
    
    /**
     * Configuration schema for TryPlugin.
     */
    public static class TryPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the try element";
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
            return true;
        }

        @Override
        public String[] getRequiredSubElements() {
            return new String[0]; // Body is optional, catch is optional but recommended
        }

        @Override
        public String[] getAllowedSubElements() {
            return new String[]{"catch"}; // Allows catch child
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
            // Additional validation can be added here if needed
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new TryPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new TryPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        // Additional validation can be added here if needed
    }

    @Override
    protected boolean shouldPropagateBodyExecutionFailures() {
        return true;
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("TryPlugin.doExecute: START");

        Variable result = executeChildElementsDirectly(context);
        context.setLocalVar("tryResult", result);
        return result;
    }

    /**
     * Execute child elements directly in the same context (v2.2.x ULTRA-LEAN)
     * Uses new architecture: PluginBodyExecutor
     */
    private Variable executeChildElementsDirectly(DynamicScopeContext context) throws InterruptedException {
        // v2.2.x: Use AbstractPlugin.executeBody() which uses PluginBodyExecutor
        return super.executeBody(context);
    }
}





