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
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * XQuery expression definition - used within {@code <xquery>} to specify query.
 * 
 * <p>
 * The {@code <xq-expression>} processor defines the XQuery expression for complex XML processing.
 * Must be used as a child element of {@code <xquery>}. Supports XQuery 1.0 syntax with Saxon engine.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic XQuery with parameters</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       for $p in //product
 *       where $p/price > $minPrice
 *       return $p/name
 *     ]]>
 *   </xq-expression>
 *   <xq-param name="minPrice" type="double">100</xq-param>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Complex data transformation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       <products>
 *         {
 *           for $p in //product
 *           let $discount := if ($p/category = "electronics") then 0.1 else 0.05
 *           return
 *             <product>
 *               <name>{$p/name/text()}</name>
 *               <originalPrice>{$p/price/text()}</originalPrice>
 *               <discountedPrice>{$p/price * (1 - $discount)}</discountedPrice>
 *               <category>{$p/category/text()}</category>
 *             </product>
 *         }
 *       </products>
 *     ]]>
 *   </xq-expression>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Aggregation and grouping</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       <summary>
 *         {
 *           for $category in distinct-values(//product/category)
 *           let $products := //product[category = $category]
 *           return
 *             <category name="{$category}">
 *               <count>{count($products)}</count>
 *               <avgPrice>{avg($products/price)}</avgPrice>
 *               <maxPrice>{max($products/price)}</maxPrice>
 *             </category>
 *         }
 *       </summary>
 *     ]]>
 *   </xq-expression>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: External variable binding</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="searchTerm">laptop</def>
 * <def var="maxPrice">2000</def>
 * 
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       declare variable $search as xs:string external;
 *       declare variable $maxPrice as xs:double external;
 *       
 *       <results>
 *         {
 *           for $p in //product
 *           where contains(lower-case($p/name), lower-case($search))
 *             and $p/price <= $maxPrice
 *           order by $p/price ascending
 *           return $p
 *         }
 *       </results>
 *     ]]>
 *   </xq-expression>
 *   <xq-param name="search" type="string">${searchTerm}</xq-param>
 *   <xq-param name="maxPrice" type="double">${maxPrice}</xq-param>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>Content (body)</strong>: The XQuery expression</li>
 * </ul>
 * 
 * <h3>XQuery Features</h3>
 * <ul>
 *   <li><strong>FLWOR Expressions:</strong> for, let, where, order by, return</li>
 *   <li><strong>Functions:</strong> Built-in XQuery functions (count, sum, avg, etc.)</li>
 *   <li><strong>Conditionals:</strong> if-then-else expressions</li>
 *   <li><strong>String Functions:</strong> contains, substring, concat, etc.</li>
 *   <li><strong>Mathematical:</strong> Basic arithmetic and math functions</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <xquery>} element</li>
 *   <li><strong>XQuery Syntax</strong>: Uses XQuery 1.0 syntax with Saxon engine</li>
 *   <li><strong>CDATA Usage</strong>: Use CDATA for queries with special XML characters (&lt;, &gt;, &amp;)</li>
 *   <li><strong>Parameter Binding</strong>: Use {@code <xq-param>} to bind external variables</li>
 *   <li><strong>Namespace Support</strong>: Supports XML namespaces in queries</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <xquery>} - Main XQuery processor (parent element)</li>
 *   <li>{@code <xq-param>} - XQuery parameter binding</li>
 *   <li>{@code <xpath>} - XPath expressions for simpler queries</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 * @see XQueryPlugin
 * @see XQParamPlugin
 */
@CorePlugin(
    elementName = "xq-expression",
    description = "XQExpression plugin that provides XQuery expression evaluation functionality",
    version = "2.1.0"
)
public class XQExpressionPlugin extends AbstractPlugin {

    /**
     * Metadata for XQExpressionPlugin.
     * 
     * <p>Detail plugin - requires xquery as parent. Defines the XQuery expression for complex XML processing. Supports XQuery 1.0 syntax with Saxon engine, FLWOR expressions, and external variable binding.</p>
     */
    public static class XQExpressionPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() { return "xq-expression"; }
        @Override
        public String getName() { return "XQuery Expression"; }
        @Override
        public String getVersion() { return "2.3.0"; }
        @Override
        public String getDescription() {
            return "Defines the XQuery expression for complex XML processing. Must be used within xquery element. Supports XQuery 1.0 syntax with Saxon engine, FLWOR expressions (For-Let-Where-Order-Return), XML transformation, aggregation, grouping, and external variable binding. Use CDATA for queries with special XML characters.";
        }
        @Override
        public String getAuthor() { return "Web-Harvest Team"; }
        @Override
        public String getCategory() {
            CorePlugin annotation = XQExpressionPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Extraction";
        }
        @Override
        public String[] getTags() {
            return new String[]{"core", "xq-expression", "xquery", "xml", "expression", "evaluation", "query", "flwor", "extraction"};
        }
        @Override
        public String[] getDependencies() { return new String[]{"Saxon"}; }
        @Override
        public String getClassName() { return XQExpressionPlugin.class.getName(); }
        @Override
        public boolean isCorePlugin() { return true; }
        @Override
        public String getElementName() { return "xq-expression"; }
        @Override
        public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new XQExpressionPluginConfigurationSchema();
        }
        @Override
        public String getIcon() { return "fas fa-code"; }
        @Override
        public String getColor() { return "#10B981"; }
        @Override
        public String getGroup() { return "Extraction"; }
        @Override
        public int getDefaultWidth() { return 240; }
        @Override
        public int getDefaultHeight() { return 160; }
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("expression", "XQuery expression (from body content)", PortType.STRING, true)
            };
        }
        @Override
        public OutputPort[] getOutputPorts() { return new OutputPort[0]; }
        @Override
        public PortType getPortType() { return PortType.STRING; }
        @Override
        public String getTooltip() {
            return "Defines the XQuery expression for complex XML processing. Supports FLWOR expressions, XML transformation, and aggregation.";
        }
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() { return new KeyboardShortcut[0]; }
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "XQuery Expression Plugin",
                "Defines the XQuery expression for complex XML processing. Supports FLWOR expressions and XML transformation.",
                new String[]{"Tab", "Enter"}
            );
        }
        @Override
        public LinkedServiceMetadata[] getLinkedServices() { return new LinkedServiceMetadata[0]; }
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("xquery", true, "XQuery");
        }
        @Override
        public String[] getAllowedChildPlugins() { return new String[0]; }
    }
    
    /**
     * Configuration schema for XQExpressionPlugin.
     */
    public static class XQExpressionPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[]{
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() { return "id"; }
                    @Override
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.STRING;
                    }
                    @Override
                    public boolean isRequired() { return false; }
                    @Override
                    public String getDefaultValue() { return null; }
                    @Override
                    public String getDescription() {
                        return "Optional identifier for the xq-expression element";
                    }
                    @Override
                    public String[] getAllowedValues() { return null; }
                    @Override
                    public Number getMinValue() { return null; }
                    @Override
                    public Number getMaxValue() { return null; }
                    @Override
                    public String getPattern() { return null; }
                }
            };
        }
        @Override
        public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
            for (PluginConfigurationSchema.AttributeDefinition attr : getAttributeDefinitions()) {
                if (attr.getName().equals(name)) return attr;
            }
            return null;
        }
        @Override
        public String getVersion() { return "2.3.0"; }
        @Override
        public boolean allowsBody() { return true; }
        @Override
        public String[] getRequiredSubElements() { return new String[0]; }
        @Override
        public String[] getAllowedSubElements() { return new String[0]; }
        @Override
        public String[] getOptionalAttributes() { return new String[]{"id"}; }
        @Override
        public String[] getRequiredAttributes() { return new String[0]; }
        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new XQExpressionPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new XQExpressionPluginMetadata().getConfigurationSchema();
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
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("XQExpressionPlugin.doExecute: START");
        
        try {
            // Execute body content to get the XQuery expression text
            Variable bodyContent = executeBody(context);
            LOGGER.debug("XQExpressionPlugin.doExecute: bodyContent=" + bodyContent);
            
            if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                LOGGER.debug("XQExpressionPlugin.doExecute: Returning body content: " + bodyContent.toString());
                return bodyContent;
            }
            
            LOGGER.debug("XQExpressionPlugin.doExecute: No body content, returning EmptyVariable");
            return EmptyVariable.INSTANCE;
            
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.debug("XQExpressionPlugin.doExecute: ERROR: " + e.getMessage());
            return EmptyVariable.INSTANCE;
        }
    }
}

















