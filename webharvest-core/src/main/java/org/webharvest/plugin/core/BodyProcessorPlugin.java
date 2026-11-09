/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Body execution processor - internal utility for executing child processors.
 * 
 * <p>
 * The {@code <body-processor>} is an internal processor used by the framework
 * to execute child processors and collect their results. It provides a standardized
 * way to execute processor body content and return the results as a variable list.
 * </p>
 * 
 * <h3>Internal Usage</h3>
 * <p>This processor is automatically used by the framework when processors need to execute
 * their body content. It's rarely used directly in user XML configurations.</p>
 * 
 * <h3>Framework Integration</h3>
 * <ul>
 *   <li><strong>Child Execution:</strong> Executes all child processors in sequence</li>
 *   <li><strong>Result Collection:</strong> Collects results from child processors</li>
 *   <li><strong>Variable Management:</strong> Manages variable scope during execution</li>
 *   <li><strong>Error Handling:</strong> Provides consistent error handling for body execution</li>
 * </ul>
 * 
 * <h3>When Framework Uses It</h3>
 * <ul>
 *   <li><strong>Container Processors:</strong> When processors contain child elements</li>
 *   <li><strong>Loop Processors:</strong> For executing loop body content</li>
 *   <li><strong>Conditional Processors:</strong> For executing conditional branches</li>
 *   <li><strong>Function Processors:</strong> For executing function body</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Internal Use:</strong> Primarily used by framework, not user configurations</li>
 *   <li><strong>Body Execution:</strong> Executes child processors and returns results</li>
 *   <li><strong>Variable Scope:</strong> Maintains proper variable scope during execution</li>
 *   <li><strong>Result Format:</strong> Returns results as appropriate variable type</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li><strong>All container processors</strong> - Use this internally for body execution</li>
 *   <li>{@code <loop>} - Uses body-processor for loop iterations</li>
 *   <li>{@code <if>} - Uses body-processor for conditional execution</li>
 *   <li>{@code <function>} - Uses body-processor for function body</li>
 * </ul>
 * 
 * <h3>Note</h3>
 * <p><strong>Framework internal processor.</strong> Users typically don't need to use this
 * directly. It's automatically used by the framework when processors need to execute
 * their body content.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 */
@CorePlugin(
    elementName = "body-processor",
    description = "Plugin which executes only body and returns variables list",
    version = "1.0.0",
    systemComponent = true,
    paletteComponent = false
)
public class BodyProcessorPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.BodyProcessorPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Execute body content using new architecture
        LOGGER.debug("BodyProcessorPlugin executing body content");
        return super.executeBody(context);
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "body-processor";
            }

            @Override
            public String getName() {
                return "body-processor";
            }

            @Override
            public String getVersion() {
                return "1.0.0";
            }

            @Override
            public String getDescription() {
                return "Plugin which executes only body and returns variables list";
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
                return new String[]{"core", "body", "processor", "execution"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return BodyProcessorPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "body-processor";
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
                        return new PluginConfigurationSchema.AttributeDefinition[0];
                    }
                    
                    @Override
                    public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
                        return null;
                    }

                    @Override
                    public String getVersion() {
                        return "1.0.0";
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
                        return new String[0];
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
                    }
                };
            }
        };
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return getMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Validate configuration first
        doValidateConfiguration(configuration);
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) {
        // No specific validation needed for body processor
    }
}
