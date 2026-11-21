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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.webharvest.exception.PluginException;
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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * XML to JSON converter - converts XML data to JSON format for APIs and JavaScript.
 * 
 * <p>
 * The {@code <xml-to-json>} processor converts XML data into JSON format. This is useful
 * when you need to send scraped data to REST APIs, JavaScript applications, or any system
 * that expects JSON input. It's the reverse operation of {@code <json-to-xml>}.
 * </p>
 * 
 * <h2>Common Use Cases</h2>
 * <ul>
 *   <li><strong>API integration:</strong> Send scraped data to REST APIs expecting JSON</li>
 *   <li><strong>JavaScript processing:</strong> Export data for client-side JavaScript</li>
 *   <li><strong>Data export:</strong> Save results in JSON format for other tools</li>
 *   <li><strong>Format transformation:</strong> HTML → XML → JSON pipeline</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Convert extracted data to JSON</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Extract product data as XML -->
 * <def var="productXml">
 *   <xpath expression="//div[@class='product']">
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * 
 * <!-- Convert to JSON -->
 * <def var="productJson">
 *   <xml-to-json>${productXml}</xml-to-json>
 * </def>
 * 
 * <!-- Save as JSON file -->
 * <file path="products.json" action="write">
 *   ${productJson}
 * </file>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: POST JSON to API</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Build XML structure -->
 * <def var="userData">
 *   <template>
 *     <![CDATA[
 *     <user>
 *       <name>John Doe</name>
 *       <email>john@example.com</email>
 *       <age>30</age>
 *     </user>
 *     ]]>
 *   </template>
 * </def>
 * 
 * <!-- Convert to JSON -->
 * <def var="userJson">
 *   <xml-to-json>${userData}</xml-to-json>
 * </def>
 * 
 * <!-- POST to API -->
 * <http url="https://api.example.com/users" method="POST">
 *   <http-header name="Content-Type">application/json</http-header>
 *   <template>${userJson}</template>
 * </http>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Scrape HTML → Export JSON</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Scrape product list -->
 * <def var="products">
 *   <xpath expression="//div[@class='product']">
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * 
 * <!-- Build XML structure -->
 * <def var="productsXml">
 *   <template>
 *     <![CDATA[<products>]]>
 *   </template>
 * </def>
 * 
 * <loop item="product">
 *   ${products}
 *   
 *   <def var="name"><xpath expression=".//h3/text()">${product}</xpath></def>
 *   <def var="price"><xpath expression=".//span[@class='price']/text()">${product}</xpath></def>
 *   
 *   <def var="productsXml" value="${productsXml}">
 *     <template>
 *       <![CDATA[
 *       <product>
 *         <name>${name}</name>
 *         <price>${price}</price>
 *       </product>
 *       ]]>
 *     </template>
 *   </def>
 * </loop>
 * 
 * <def var="productsXml" value="${productsXml}</products>"/>
 * 
 * <!-- Convert entire structure to JSON -->
 * <def var="productsJson">
 *   <xml-to-json>${productsXml}</xml-to-json>
 * </def>
 * 
 * <file path="products.json" action="write">
 *   ${productsJson}
 * </file>
 * </config>
 * }</pre>
 * 
 * <h3>XML to JSON Mapping</h3>
 * <table border="1">
 * <caption>XML to JSON conversion examples</caption>
 *   <tr>
 *     <th>XML Structure</th>
 *     <th>JSON Output</th>
 *   </tr>
 *   <tr>
 *     <td>{@code <name>John</name>}</td>
 *     <td>{@code {"name": "John"}}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code <user><name>John</name></user>}</td>
 *     <td>{@code {"user": {"name": "John"}}}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code <item>1</item><item>2</item>}</td>
 *     <td>{@code {"item": [1, 2]}}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code <count>42</count>}</td>
 *     <td>{@code {"count": 42}}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code <active>true</active>}</td>
 *     <td>{@code {"active": true}}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code <value/>}</td>
 *     <td>{@code {"value": null}}</td>
 *   </tr>
 * </table>
 * 
 * <h3>Type Conversion</h3>
 * <p>
 * The converter attempts to preserve data types:
 * </p>
 * <ul>
 *   <li><strong>Numbers:</strong> {@code <count>42</count>} → {@code 42} (number, not "42")</li>
 *   <li><strong>Booleans:</strong> {@code <active>true</active>} → {@code true} (boolean)</li>
 *   <li><strong>Nulls:</strong> {@code <value/>} → {@code null}</li>
 *   <li><strong>Strings:</strong> All other content → string</li>
 * </ul>
 * 
 * <h3>Repeated Elements → Arrays</h3>
 * <p>
 * Multiple XML elements with the same name become JSON arrays:
 * </p>
 * <pre>
 * XML:  &lt;item&gt;A&lt;/item&gt;&lt;item&gt;B&lt;/item&gt;&lt;item&gt;C&lt;/item&gt;
 * JSON: {"item": ["A", "B", "C"]}
 * </pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Root element:</strong> XML root element name becomes top-level JSON key</li>
 *   <li><strong>Attributes:</strong> XML attributes are converted to JSON properties with @ prefix</li>
 *   <li><strong>Mixed content:</strong> XML with both text and elements may produce unexpected results</li>
 *   <li><strong>Namespaces:</strong> XML namespaces are stripped (only local names used)</li>
 *   <li><strong>CDATA:</strong> CDATA sections are treated as regular text content</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <json-to-xml>} - Convert JSON to XML (reverse operation)</li>
 *   <li>{@code <http>} - Send JSON to REST APIs</li>
 *   <li>{@code <file>} - Save JSON to files</li>
 *   <li>{@code <xpath>} - Build XML before conversion</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 */
@CorePlugin(
    elementName = "xml-to-json",
    description = "Converts XML to JSON",
    version = "2.2.0",
    category = "Extraction"
)
public class XmlToJsonPlugin extends AbstractPlugin {

    /**
     * Metadata for XmlToJsonPlugin.
     * 
     * <p>Standalone plugin - converts XML data to JSON format for APIs and JavaScript.</p>
     */
    public static class XmlToJsonPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "xml-to-json";
        }

        @Override
        public String getName() {
            return "XML to JSON";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Converts XML data into JSON format. Useful when sending scraped data to REST APIs, JavaScript applications, or any system expecting JSON input. It's the reverse operation of json-to-xml. Preserves data types (numbers, booleans, nulls) and converts repeated XML elements to JSON arrays.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = XmlToJsonPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Extraction";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "xml", "json", "converter", "transformation", "api"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return XmlToJsonPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "xml-to-json";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new XmlToJsonPluginConfigurationSchema();
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
                new InputPort("xml", "XML content to convert (from body content)", PortType.XML, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "JSON representation of XML data", PortType.STRING)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.STRING;
        }
        
        @Override
        public String getTooltip() {
            return "Converts XML data into JSON format. Useful when sending scraped data to REST APIs, JavaScript applications, or any system expecting JSON input.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+X", "Add XML to JSON", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "XML to JSON Plugin",
                "Converts XML data into JSON format. Useful when sending scraped data to REST APIs, JavaScript applications, or any system expecting JSON input.",
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
     * Configuration schema for XmlToJsonPlugin.
     */
    public static class XmlToJsonPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[0]; // No attributes
        }
        
        @Override
        public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
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
            return new String[0];
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            // No validation needed - no attributes required
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new XmlToJsonPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new XmlToJsonPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No validation needed - no attributes required
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        try {
            Variable body = super.executeBody(context);
            JSONObject jsonObject = XML.toJSONObject(body.toString());
            return new NodeVariable(jsonObject.toString());
        } catch (JSONException e) {
            throw new PluginException("Error converting XML to JSON: " + e.getMessage(), e);
        }
    }
}
