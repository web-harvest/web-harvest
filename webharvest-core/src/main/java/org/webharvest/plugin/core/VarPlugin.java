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

 You can contact Vladimir Nikic by sending e-mail to
 nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
 subject line.
*/
package org.webharvest.plugin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Variable reference processor - alternative syntax for {@code <def>} with name attribute.
 * 
 * <p>
 * The {@code <var>} processor is an alternative to {@code <def>} that uses {@code name}
 * attribute instead of {@code var}. Both define variables - this is a legacy syntax
 * maintained for compatibility.
 * </p>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Legacy syntax -->
 * <var name="apiUrl">https://api.example.com</var>
 * 
 * <!-- Modern syntax (preferred) -->
 * <def var="apiUrl">https://api.example.com</def>
 * </config>
 * }</pre>
 * 
 * <p><strong>Recommendation:</strong> Use {@code <def var="...">} for new configurations.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @deprecated Use {@code <def>} instead
 * @see DefPlugin
 */
@CorePlugin(
    elementName = "var",
    description = "Variable reference plugin",
    version = "2.1.0",
    paletteComponent = false
)
public class VarPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.VarPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        String varName = config.getProperty("name", "");
        
        if (varName.isEmpty()) {
            LOGGER.warn("No variable name specified for var plugin");
            return EmptyVariable.INSTANCE;
        }

        try {
            // First, try to get variable from context
            LOGGER.debug("VarPlugin: Looking for variable '{}'", varName);
            Variable variable = context.getVar(varName);
            if (variable != null && !(variable instanceof EmptyVariable)) {
                LOGGER.debug("VarPlugin: Found variable '{}' with value: {}", varName, variable);
                return variable;
            }
            
            // If variable not found, try to execute body content and set it
            LOGGER.debug("VarPlugin: Variable '{}' not found, trying to set it from body content", varName);
            Variable bodyContent = executeBody(context);
            if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                LOGGER.debug("VarPlugin: Setting variable '{}' with body content: {}", varName, bodyContent);
                context.setLocalVar(varName, bodyContent);
                return bodyContent;
            }
            
            LOGGER.warn("VarPlugin: Variable '{}' not found and no body content to set", varName);
            return EmptyVariable.INSTANCE;
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error in VarPlugin: {}", e.getMessage(), e);
            return EmptyVariable.INSTANCE;
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "var";
            }

            @Override
            public String getName() {
                return "Variable Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "Variable reference plugin";
            }

            @Override
            public String getAuthor() {
                return "Web-Harvest Team";
            }

            @Override
            public String getCategory() {
                return "Core";
            }

            @Override
            public String[] getTags() {
                return new String[]{"variable", "core"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return VarPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "var";
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
                        return new String[]{"name"};
                    }

                    @Override
                    public String[] getRequiredAttributes() {
                        return new String[0];
                    }

                    @Override
                    public void validate(PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginValidationException {
                        String name = configuration.getProperty("name", "");
                        if (name.isEmpty()) {
                            throw new org.webharvest.plugin.exceptions.PluginValidationException("Variable name is required");
                        }
                    }
                };
            }
        };
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) {
        // No initialization needed
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) {
        String name = configuration.getProperty("name", "");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Variable name is required");
        }
    }
}