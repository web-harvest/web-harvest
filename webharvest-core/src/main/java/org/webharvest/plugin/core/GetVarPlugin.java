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
 * INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import java.text.MessageFormat;

import org.webharvest.exception.VariableException;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that retrieves a variable value from the context.
 * This is equivalent to the old GetVarPlugin but using the new plugin architecture.
 *
 * @author Web-Harvest Team
 * @since 2.1.0
 * @version %I%, %G%
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "get",
    description = "Retrieves a variable value from the context",
    version = "2.1.0"
)
public class GetVarPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.GetVarPlugin");

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() { return "get"; }
            @Override
            public String getName() { return "get"; }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public String getDescription() { return "Retrieves a variable value from the context"; }
            @Override
            public String getAuthor() { return "Web-Harvest Team"; }
            @Override
            public String getCategory() { return "core"; }
            @Override
            public String[] getTags() { return new String[0]; }
            @Override
            public String[] getDependencies() { return new String[0]; }
            @Override
            public PluginConfigurationSchema getConfigurationSchema() { return GetVarPlugin.this.getConfigurationSchema(); }
            @Override
            public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
            @Override
            public String getElementName() { return "get"; }
            @Override
            public boolean isCorePlugin() { return true; }
            @Override
            public String getClassName() { return GetVarPlugin.this.getClass().getName(); }
        };
    }

    /**
     * {@inheritDoc}
     */
    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public AttributeDefinition[] getAttributeDefinitions() {
                return new AttributeDefinition[] {
                    new AttributeDefinition() {
                        @Override
                        public String getName() { return "var"; }
                        @Override
                        public String getDescription() { return "Name of the variable to retrieve"; }
                        @Override
                        public boolean isRequired() { return true; }
                        @Override
                        public AttributeType getType() { return AttributeType.STRING; }
                        @Override
                        public String getPattern() { return null; }
                        @Override
                        public Number getMaxValue() { return null; }
                        @Override
                        public Number getMinValue() { return null; }
                        @Override
                        public String[] getAllowedValues() { return null; }
                        @Override
                        public String getDefaultValue() { return null; }
                    }
                };
            }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public boolean allowsBody() { return false; }
            @Override
            public String[] getRequiredSubElements() { return new String[0]; }
            @Override
            public String[] getAllowedSubElements() { return new String[0]; }
            @Override
            public String[] getOptionalAttributes() { return new String[0]; }
            @Override
            public String[] getRequiredAttributes() { return new String[]{"var"}; }
            @Override
            public AttributeDefinition getAttribute(String name) { 
                if ("var".equals(name)) {
                    return getAttributeDefinitions()[0];
                }
                return null;
            }
            @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                // Validation is handled in doValidateConfiguration
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // No initialization needed for GetVarPlugin
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        // GetVarPlugin should return the variable value directly, not use BodyProcessor
        return doExecute(context);
    }

    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        String varName = config.getProperty("var", "");
        
        LOGGER.debug("GetVarPlugin: doExecute called with varName='" + varName + "'");
        LOGGER.debug("GetVarPlugin: config=" + config);
        
        if (varName.isEmpty()) {
            throw new RuntimeException("Variable name is required for get operation");
        }
        
        // Evaluate the var name as a template if it contains template expressions
        if (varName.contains("${")) {
            Variable evaluatedVarName = BaseTemplater.evaluateToVariable(varName, null, context);
            if (evaluatedVarName != null) {
                varName = evaluatedVarName.toString();
                LOGGER.debug("GetVarPlugin: evaluated varName to: '" + varName + "'");
            }
        }

               try {
                   Variable variable = context.getVar(varName);
                   LOGGER.debug("GetVarPlugin: context.getVar('" + varName + "') returned: " + variable);
                   
                   if (variable == null) {
                       throw new RuntimeException(MessageFormat.format("Error retrieving variable ''{0}'': Variable ''{0}'' is not defined!", varName));
                   }
                   
                   LOGGER.debug("Retrieved variable '{}' with value: {}", varName, variable);
                   LOGGER.debug("GetVarPlugin: returning variable: " + variable);
                   return variable;
                   
               } catch (Exception e) {
                   LOGGER.error("Error retrieving variable '{}': {}", varName, e.getMessage());
                   LOGGER.debug("GetVarPlugin: Error retrieving variable '" + varName + "': " + e.getMessage());
                   if (e instanceof RuntimeException) {
                       // Check if this is already our formatted error message
                       if (e.getMessage() != null && e.getMessage().startsWith("Error retrieving variable")) {
                           throw e; // Re-throw as-is
                       }
                       // Wrap RuntimeException with our error message format
                       throw new RuntimeException(MessageFormat.format("Error retrieving variable ''{0}'': {1}", varName, e.getMessage()), e);
                   }
                   throw new RuntimeException(MessageFormat.format("Error retrieving variable ''{0}'': {1}", varName, e.getMessage()), e);
               }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String varName = configuration.getProperty("var", "");
        if (varName.isEmpty()) {
            throw new PluginValidationException("Variable name 'var' is required");
        }
    }
}
