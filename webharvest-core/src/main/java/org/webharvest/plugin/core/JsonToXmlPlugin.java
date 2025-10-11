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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webharvest.exception.PluginException;
import org.webharvest.utils.JsonXmlConverter;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Converter from JSON to XML.
 * Migrated from org.webharvest.runtime.processors.plugins.JsonToXmlPlugin
 * 
 * <h3>Bug #61 Fix (v2.2.1)</h3>
 * <p>
 * Fixed issue where JSON key "content" was treated specially by org.json.XML,
 * causing it to be stripped from XML output. Now uses {@link JsonXmlConverter}
 * which treats all JSON keys equally without special cases.
 * </p>
 * 
 * <p>
 * <strong>Before Fix:</strong><br>
 * Input: <code>{"a": {"content": "hello"}}</code><br>
 * Output: <code>&lt;a&gt;hello&lt;/a&gt;</code> (content key lost!)
 * </p>
 * 
 * <p>
 * <strong>After Fix:</strong><br>
 * Input: <code>{"a": {"content": "hello"}}</code><br>
 * Output: <code>&lt;a&gt;&lt;content&gt;hello&lt;/content&gt;&lt;/a&gt;</code> (preserved!)
 * </p>
 * 
 * @author WebHarvest Team
 * @version 2.2.1
 * @see JsonXmlConverter
 */
@CorePlugin(elementName = "json-to-xml")
public class JsonToXmlPlugin extends AbstractPlugin {

    private static final String TAG_ATT_NAME = "tag";

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "json-to-xml";
            }

            @Override
            public String getName() {
                return "JsonToXml Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "Converter from JSON to XML";
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
                return new String[]{"core", "json", "xml", "converter"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return JsonToXmlPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "json-to-xml";
            }

            @Override
            public String getNamespace() {
                return "http://org.webharvest/schema/2.1/core";
            }

            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return JsonToXmlPlugin.this.getConfigurationSchema();
            }
        };
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No validation needed - tag is optional
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                return new PluginConfigurationSchema.AttributeDefinition[]{
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() {
                            return TAG_ATT_NAME;
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
                            return "data";
                        }

                        @Override
                        public String getDescription() {
                            return "Root tag name for XML output";
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
                return new String[]{TAG_ATT_NAME};
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

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        try {
            // Execute body content to get JSON string
            Variable bodyContent = executeBody(context);
            String jsonString = "";
            
            if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                jsonString = bodyContent.toString();
                System.out.println("JsonToXmlPlugin: Retrieved JSON content: " + jsonString);
            }
            
            // Handle empty or invalid JSON
            if (jsonString.isEmpty()) {
                System.out.println("JsonToXmlPlugin: No JSON content provided");
                return new NodeVariable("<empty/>");
            }
            
            String rootTag = getConfiguration().getProperty(TAG_ATT_NAME);
            if (rootTag == null || rootTag.isEmpty()) {
                rootTag = "data";
            }
            
            // Handle JSON array vs object
            if (jsonString.startsWith("[")) {
                // It's a JSON array
                JSONArray jsonArray = new JSONArray(jsonString);
                // Bug #61 Fix: Use JsonXmlConverter instead of org.json.XML
                return new NodeVariable(JsonXmlConverter.toXml(jsonArray, rootTag));
            } else if (!jsonString.startsWith("{")) {
                // If it doesn't start with { or [, wrap it in an object
                jsonString = "{\"data\": " + jsonString + "}";
            }
            
            // Bug #61 Fix: Use JsonXmlConverter which doesn't have "content" special case
            return new NodeVariable(JsonXmlConverter.toXml(
                    new JSONObject(jsonString),
                    rootTag));
        } catch (JSONException e) {
            // If JSON parsing still fails, create a simple XML with the raw content
            // TODO: Implement proper body execution
            String rawContent = "";
            String rootTag = getConfiguration().getProperty(TAG_ATT_NAME);
            if (rootTag == null || rootTag.isEmpty()) {
                rootTag = "data";
            }
            return new NodeVariable("<" + rootTag + "><![CDATA[" + rawContent + "]]></" + rootTag + ">");
        }
    }
}
