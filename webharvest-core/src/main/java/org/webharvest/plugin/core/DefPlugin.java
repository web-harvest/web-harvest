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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.CorePluginDef;
import org.webharvest.definition.XmlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that defines variables using the 'def' element.
 * This plugin allows defining variables with their values.
 * 
 * @author AI Assistant
 * @since 2.1.0
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "def",
    description = "Defines variables with their values",
    version = "2.1.0"
)
public class DefPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.DefPlugin");

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() { return "def"; }
            @Override
            public String getName() { return "def"; }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public String getDescription() { return "Defines variables with their values"; }
            @Override
            public String getAuthor() { return "Web-Harvest Team"; }
            @Override
            public String getCategory() { return "core"; }
            @Override
            public String[] getTags() { return new String[0]; }
            @Override
            public String[] getDependencies() { return new String[0]; }
            @Override
            public PluginConfigurationSchema getConfigurationSchema() { return DefPlugin.this.getConfigurationSchema(); }
            @Override
            public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
            @Override
            public String getElementName() { return "def"; }
            @Override
            public boolean isCorePlugin() { return true; }
            @Override
            public String getClassName() { return DefPlugin.this.getClass().getName(); }
        };
    }

    /**
     * {@inheritDoc}
     */
    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                return new PluginConfigurationSchema.AttributeDefinition[] {
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() { return "var"; }
                        @Override
                        public String getDescription() { return "Variable name to define"; }
                        @Override
                        public PluginConfigurationSchema.AttributeType getType() { return PluginConfigurationSchema.AttributeType.STRING; }
                        @Override
                        public boolean isRequired() { return true; }
                        @Override
                        public Number getMaxValue() { return null; }
                        @Override
                        public Number getMinValue() { return null; }
                        @Override
                        public String[] getAllowedValues() { return null; }
                        @Override
                        public String getDefaultValue() { return null; }
                        @Override
                        public String getPattern() { return null; }
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
                return new String[0];
            }

            @Override
            public String[] getRequiredAttributes() {
                return new String[]{"var"};
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
        // No initialization needed for DefPlugin
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.info("DefPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();
        LOGGER.info("DefPlugin.doExecute: config: {}", config);

        String varName = config.getProperty("var", "");
        LOGGER.info("DefPlugin.doExecute: varName = '{}'", varName);

        if (varName.isEmpty()) {
            LOGGER.error("DefPlugin.doExecute: Variable name is required for def operation");
            throw new RuntimeException("Variable name is required for def operation");
        }

        try {
            // Get value from "value" attribute or execute child elements
            String valueAttr = config.getProperty("value", "");
            Variable value;
            
            if (!valueAttr.isEmpty()) {
                // Use value from attribute
                value = new NodeVariable(valueAttr);
            } else {
                // Execute child elements to get the value
                value = super.executeBody(context);
            }
            LOGGER.info("DefPlugin.doExecute: Body content result: {}", value);
            
            // If no result from child processors, try XML node
            if (value == null || value instanceof EmptyVariable) {
                LOGGER.info("DefPlugin.doExecute: No child processors result, trying XML node");
                value = getBodyContentFromXmlNode();
                LOGGER.info("DefPlugin.doExecute: XML node result: {}", value);
            }

            // Set the variable in the context
            context.setLocalVar(varName, value);
            LOGGER.info("DefPlugin.doExecute: Set variable '{}' in context with value: {}", varName, value);

            LOGGER.info("DefPlugin.doExecute: Returning value: {}", value);
            return value;

        } catch (Exception e) {
            LOGGER.error("Error defining variable '" + varName + "': " + e.getMessage(), e);
            throw new RuntimeException("Error defining variable '" + varName + "': " + e.getMessage(), e);
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

    /**
     * Gets the body content directly from configuration (v2.2.x ULTRA-LEAN).
     * Uses _textContent property populated by StreamingXmlParser.
     */
    private Variable getBodyContentFromXmlNode() {
        // v2.2.x: Use _textContent property from PluginConfiguration
        String textContent = getConfiguration().getProperty("_textContent");
        if (textContent != null && !textContent.trim().isEmpty()) {
            return new NodeVariable(textContent);
        }
        
        return EmptyVariable.INSTANCE;
    }
}
