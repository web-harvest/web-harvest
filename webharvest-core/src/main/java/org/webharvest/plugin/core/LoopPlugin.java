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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Loop plugin for iterating over collections.
 */
@CorePlugin(
    elementName = "loop",
    description = "Loop plugin for iterations",
    version = "2.1.0"
)
public class LoopPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.LoopPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        String item = config.getProperty("item", "");
        String index = config.getProperty("index", "");
        String maxloops = config.getProperty("maxloops", "100");
        String filter = config.getProperty("filter", "");
        String empty = config.getProperty("empty", "false");
        
        LOGGER.debug("LoopPlugin: item=" + item + ", index=" + index + ", maxloops=" + maxloops);

        try {
            // Get items from configuration
            String itemsStr = config.getProperty("items", "");
            String delimiter = config.getProperty("delimiter", ",");
            
            LOGGER.debug("LoopPlugin: items=" + itemsStr + ", delimiter=" + delimiter);
            
            if (itemsStr.isEmpty()) {
                LOGGER.debug("LoopPlugin: No items specified");
                return EmptyVariable.INSTANCE;
            }
            
            // Split items by delimiter
            String[] items = itemsStr.split(delimiter);
            LOGGER.debug("LoopPlugin: Found " + items.length + " items");
            
            StringBuilder result = new StringBuilder();
            
            // Loop through items
            for (int i = 0; i < items.length; i++) {
                String currentItem = items[i].trim();
                LOGGER.debug("LoopPlugin: Processing item " + i + ": " + currentItem);
                
                // Set current item in context
                context.setLocalVar("item", new NodeVariable(currentItem));
                context.setLocalVar("index", new NodeVariable(String.valueOf(i)));
                
                // Execute body content for each item
                Variable bodyResult = executeBody(context);
                if (bodyResult != null && !(bodyResult instanceof EmptyVariable)) {
                    result.append(bodyResult.toString());
                }
            }
            
            LOGGER.debug("LoopPlugin: Final result: " + result.toString());
            return new NodeVariable(result.toString());
            
        } catch (Exception e) {
            LOGGER.debug("LoopPlugin: Error: " + e.getMessage());
            LOGGER.error("Error in LoopPlugin: {}", e.getMessage(), e);
            return EmptyVariable.INSTANCE;
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "loop";
            }

            @Override
            public String getName() {
                return "Loop Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "Loop plugin for iterations";
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
                return new String[]{"loop", "iteration", "core"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return LoopPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "loop";
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
                        return new String[]{"item", "index", "maxloops", "filter", "empty"};
                    }

                    @Override
                    public String[] getRequiredAttributes() {
                        return new String[0];
                    }

                    @Override
                    public void validate(PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginValidationException {
                        // No validation needed for now
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
        // No validation needed for now
    }
}