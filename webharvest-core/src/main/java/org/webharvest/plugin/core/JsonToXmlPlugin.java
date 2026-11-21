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
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.PortType;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * JSON to XML converter - converts JSON API responses to XML for XPath processing.
 * 
 * <p>
 * The {@code <json-to-xml>} processor converts JSON data (from REST APIs, files, etc.)
 * into well-formed XML. This enables you to use XPath and XQuery to extract data from
 * JSON responses, providing a unified query language for both HTML and JSON sources.
 * </p>
 * 
 * <h2>Why Convert JSON to XML?</h2>
 * <ul>
 *   <li><strong>Unified querying:</strong> Use XPath for both HTML pages and JSON APIs</li>
 *   <li><strong>Complex extraction:</strong> XPath is more powerful than simple JSON path for nested data</li>
 *   <li><strong>Data transformation:</strong> XSLT and XQuery work on XML, not JSON</li>
 *   <li><strong>Consistent pipeline:</strong> Same processors ({@code <xpath>}) for all data sources</li>
 * </ul>
 * 
 * <h3>Bug #61 Fix (v2.2.0): "content" Key Preservation</h3>
 * <p>
 * <strong>Issue:</strong> The org.json.XML library treated the JSON key {@code "content"}
 * specially, stripping it from XML output and using its value as element text content.
 * This broke data extraction for APIs using {@code "content"} as a regular field name.
 * </p>
 * <p>
 * <strong>Before Fix:</strong><br>
 * Input JSON: {@code {"article": {"content": "hello world"}}}<br>
 * Output XML: {@code <article>hello world</article>} (content key lost!)
 * </p>
 * <p>
 * <strong>After Fix (v2.2.0):</strong><br>
 * Input JSON: {@code {"article": {"content": "hello world"}}}<br>
 * Output XML: {@code <article><content>hello world</content></article>} (preserved!)
 * </p>
 * <p>
 * Fix implemented via custom {@link JsonXmlConverter} which treats all JSON keys
 * equally without special cases.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Parse JSON API response</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="userName">
 *   <xpath expression="//user/name/text()">
 *     <json-to-xml>
 *       <http url="https://api.example.com/user/123">
 *         <http-header name="Accept">application/json</http-header>
 *       </http>
 *     </json-to-xml>
 *   </xpath>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Extract from nested JSON</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- JSON: {"data": {"users": [{"id": 1, "name": "John"}, {"id": 2, "name": "Jane"}]}} -->
 * <def var="userNames">
 *   <xpath expression="//users/item/name/text()">
 *     <json-to-xml>
 *       <http url="https://api.example.com/users"/>
 *     </json-to-xml>
 *   </xpath>
 * </def>
 * 
 * <!-- Result: List of names -->
 * <loop item="name">
 *   ${userNames}
 *   
 *   <file path="names.txt" action="append">${name}&#10;</file>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: JSON array processing</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- JSON: [{"id": 1, "title": "Post 1"}, {"id": 2, "title": "Post 2"}] -->
 * <loop item="post">
 *   <xpath expression="//item">
 *     <json-to-xml>
 *       <http url="https://api.example.com/posts"/>
 *     </json-to-xml>
 *   </xpath>
 *   
 *   <def var="postId"><xpath expression="id/text()">${post}</xpath></def>
 *   <def var="postTitle"><xpath expression="title/text()">${post}</xpath></def>
 *   
 *   <file path="posts/post-${postId}.txt" action="write">
 *     ${postTitle}
 *   </file>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Complex nested data extraction</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="addresses">
 *   <xpath expression="//users/item[address/city/text()='London']/address/street/text()">
 *     <json-to-xml>
 *       <http url="https://api.example.com/users"/>
 *     </json-to-xml>
 *   </xpath>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: Custom root tag name</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <json-to-xml tag="root">
 *   <http url="https://api.example.com/data"/>
 * </json-to-xml>
 * <!-- Output: <root>...</root> instead of default <json>...</json> -->
 * </config>
 * }</pre>
 * 
 * <h3>JSON to XML Mapping</h3>
 * <table border="1">
 * <caption>JSON to XML type conversion examples</caption>
 *   <tr>
 *     <th>JSON Type</th>
 *     <th>XML Representation</th>
 *   </tr>
 *   <tr>
 *     <td>Object {@code {"key": "value"}}</td>
 *     <td>{@code <key>value</key>}</td>
 *   </tr>
 *   <tr>
 *     <td>Array {@code [1, 2, 3]}</td>
 *     <td>{@code <item>1</item><item>2</item><item>3</item>}</td>
 *   </tr>
 *   <tr>
 *     <td>Nested {@code {"user": {"name": "John"}}}</td>
 *     <td>{@code <user><name>John</name></user>}</td>
 *   </tr>
 *   <tr>
 *     <td>Number {@code 42}</td>
 *     <td>{@code <number>42</number>}</td>
 *   </tr>
 *   <tr>
 *     <td>Boolean {@code true}</td>
 *     <td>{@code <boolean>true</boolean>}</td>
 *   </tr>
 *   <tr>
 *     <td>Null {@code null}</td>
 *     <td>{@code <null/>}</td>
 *   </tr>
 * </table>
 * 
 * <h3>Array Handling</h3>
 * <p>
 * JSON arrays are converted to repeated {@code <item>} elements:
 * </p>
 * <pre>
 * JSON: [{"id": 1}, {"id": 2}]
 * XML:  &lt;item&gt;&lt;id&gt;1&lt;/id&gt;&lt;/item&gt;
 *       &lt;item&gt;&lt;id&gt;2&lt;/id&gt;&lt;/item&gt;
 * </pre>
 * <p>
 * Use XPath {@code //item} to iterate over array elements.
 * </p>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Root element:</strong> Output is always wrapped in root element (default: {@code <json>})</li>
 *   <li><strong>Array items:</strong> Array elements use {@code <item>} tag name</li>
 *   <li><strong>Key preservation:</strong> All JSON keys are preserved (Bug #61 fixed)</li>
 *   <li><strong>Type information:</strong> JSON types are preserved in element structure</li>
 *   <li><strong>Invalid XML chars:</strong> JSON keys with spaces/special chars are escaped</li>
 * </ul>
 * 
 * <h3>Performance</h3>
 * <ul>
 *   <li><strong>Conversion speed:</strong> Fast for typical API responses (&lt; 1MB)</li>
 *   <li><strong>Memory usage:</strong> Entire JSON is loaded into memory</li>
 *   <li><strong>Large files:</strong> For huge JSON files (&gt; 10MB), consider streaming JSON parsers</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <xml-to-json>} - Convert XML back to JSON</li>
 *   <li>{@code <xpath>} - Query the resulting XML</li>
 *   <li>{@code <http>} - Fetch JSON from APIs</li>
 *   <li>{@code <script>} - Alternative for JSON parsing with JSON.parse()</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 * @see JsonXmlConverter
 */
@CorePlugin(
    elementName = "json-to-xml",
    description = "Converts JSON to XML",
    version = "2.2.0",
    category = "Extraction"
)
public class JsonToXmlPlugin extends AbstractPlugin {

    private static final String TAG_ATT_NAME = "tag";

    /**
     * Metadata for JsonToXmlPlugin.
     * 
     * <p>Standalone plugin - converts JSON data to XML for XPath processing.</p>
     */
    public static class JsonToXmlPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "json-to-xml";
        }

        @Override
        public String getName() {
            return "JSON to XML";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Converts JSON data (from REST APIs, files, etc.) into well-formed XML. Enables unified querying with XPath for both HTML pages and JSON APIs. Essential for processing JSON API responses and extracting nested data structures. All JSON keys are preserved, including 'content' key (Bug #61 fixed in v2.2.0).";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = JsonToXmlPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Extraction";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "json", "xml", "converter", "transformation", "api"};
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
            return new JsonToXmlPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-exchange-alt";
        }
        
        @Override
        public String getColor() {
            return "#60A5FA"; // Light blue for XML/JSON
        }
        
        @Override
        public String getGroup() {
            return "XML & JSON";
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
                new InputPort("json", "JSON content to convert (from body content)", PortType.STRING, false),
                new InputPort("tag", "Root XML tag name (from tag attribute, default: data)", PortType.STRING, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "XML representation of JSON data", PortType.XML)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.XML;
        }
        
        @Override
        public String getTooltip() {
            return "Converts JSON data into well-formed XML. Enables unified querying with XPath for both HTML pages and JSON APIs. Essential for processing JSON API responses and extracting nested data structures.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+J", "Add JSON to XML", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "JSON to XML Plugin",
                "Converts JSON data into well-formed XML. Enables unified querying with XPath for both HTML pages and JSON APIs.",
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
            return new String[0]; // Can contain any child plugins
        }
    }
    
    /**
     * Configuration schema for JsonToXmlPlugin.
     */
    public static class JsonToXmlPluginConfigurationSchema implements PluginConfigurationSchema {
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
            return "2.3.0";
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
            return new String[0]; // Can contain any child plugins
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"tag"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            // No validation needed - tag is optional
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new JsonToXmlPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new JsonToXmlPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No validation needed - tag is optional
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        try {
            // Execute body content to get JSON string
            Variable bodyContent = executeBody(context);
            String jsonString = "";
            
            if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                jsonString = bodyContent.toString();
                LOGGER.debug("JsonToXmlPlugin: Retrieved JSON content: " + jsonString);
            }
            
            // Handle empty or invalid JSON
            if (jsonString.isEmpty()) {
                LOGGER.debug("JsonToXmlPlugin: No JSON content provided");
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
