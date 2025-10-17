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
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * XSLT stylesheet definition - used within {@code <xslt>} to specify transformation.
 * 
 * <p>
 * The {@code <stylesheet>} processor defines the XSLT stylesheet content for XML transformations.
 * Must be used as a child element of {@code <xslt>}. Supports XSLT 1.0 and 2.0 transformations
 * with Saxon engine.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic XML transformation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xslt>
 *   <stylesheet>
 *     <![CDATA[
 *     <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 *       <xsl:template match="/">
 *         <html>
 *           <head><title>Product Catalog</title></head>
 *           <body>
 *             <h1>Products</h1>
 *             <xsl:for-each select="//product">
 *               <div class="product">
 *                 <h2><xsl:value-of select="name"/></h2>
 *                 <p>Price: $<xsl:value-of select="price"/></p>
 *               </div>
 *             </xsl:for-each>
 *           </body>
 *         </html>
 *       </xsl:template>
 *     </xsl:stylesheet>
 *     ]]>
 *   </stylesheet>
 *   <xml>${sourceXml}</xml>
 * </xslt>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Data transformation with filtering</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xslt>
 *   <stylesheet>
 *     <![CDATA[
 *     <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 *       <xsl:template match="/">
 *         <products>
 *           <xsl:for-each select="//product[price > 100]">
 *             <xsl:sort select="price" order="ascending"/>
 *             <product>
 *               <id><xsl:value-of select="@id"/></id>
 *               <name><xsl:value-of select="name"/></name>
 *               <price><xsl:value-of select="price"/></price>
 *               <category><xsl:value-of select="category"/></category>
 *             </product>
 *           </xsl:for-each>
 *         </products>
 *       </xsl:template>
 *     </xsl:stylesheet>
 *     ]]>
 *   </stylesheet>
 *   <xml>${productCatalog}</xml>
 * </xslt>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Complex transformation with parameters</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="currency">USD</def>
 * <def var="discountRate">0.1</def>
 * 
 * <xslt>
 *   <stylesheet>
 *     <![CDATA[
 *     <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 *       <xsl:param name="currency"/>
 *       <xsl:param name="discountRate"/>
 *       
 *       <xsl:template match="/">
 *         <catalog>
 *           <xsl:for-each select="//product">
 *             <item>
 *               <name><xsl:value-of select="name"/></name>
 *               <originalPrice><xsl:value-of select="price"/></originalPrice>
 *               <discountedPrice>
 *                 <xsl:value-of select="price * (1 - $discountRate)"/>
 *               </discountedPrice>
 *               <currency><xsl:value-of select="$currency"/></currency>
 *             </item>
 *           </xsl:for-each>
 *         </catalog>
 *       </xsl:template>
 *     </xsl:stylesheet>
 *     ]]>
 *   </stylesheet>
 *   <param name="currency">${currency}</param>
 *   <param name="discountRate">${discountRate}</param>
 *   <xml>${sourceData}</xml>
 * </xslt>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: HTML to structured data</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xslt>
 *   <stylesheet>
 *     <![CDATA[
 *     <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 *       <xsl:template match="/">
 *         <structuredData>
 *           <xsl:for-each select="//div[@class='product']">
 *             <product>
 *               <title><xsl:value-of select="h2/text()"/></title>
 *               <price><xsl:value-of select="span[@class='price']/text()"/></price>
 *               <description><xsl:value-of select="p[@class='description']/text()"/></description>
 *               <image><xsl:value-of select="img/@src"/></image>
 *             </product>
 *           </xsl:for-each>
 *         </structuredData>
 *       </xsl:template>
 *     </xsl:stylesheet>
 *     ]]>
 *   </stylesheet>
 *   <xml>
 *     <html-to-xml>
 *       <http url="https://shop.example.com/products"/>
 *     </html-to-xml>
 *   </xml>
 * </xslt>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>Content (body)</strong>: XSLT stylesheet content</li>
 * </ul>
 * 
 * <h3>XSLT Features</h3>
 * <ul>
 *   <li><strong>Templates:</strong> xsl:template for pattern matching</li>
 *   <li><strong>Loops:</strong> xsl:for-each for iteration</li>
 *   <li><strong>Conditionals:</strong> xsl:if and xsl:choose for branching</li>
 *   <li><strong>Sorting:</strong> xsl:sort for ordering results</li>
 *   <li><strong>Parameters:</strong> xsl:param for external values</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <xslt>} element</li>
 *   <li><strong>XSLT Syntax</strong>: Uses XSLT 1.0/2.0 syntax with Saxon engine</li>
 *   <li><strong>CDATA Usage</strong>: Use CDATA for stylesheets with special XML characters</li>
 *   <li><strong>Namespace Support</strong>: Supports XML namespaces in transformations</li>
 *   <li><strong>Parameter Binding</strong>: Use {@code <param>} to pass external parameters</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <xslt>} - Main XSLT processor (parent element)</li>
 *   <li>{@code <param>} - XSLT parameter binding</li>
 *   <li>{@code <xml>} - Source XML for transformation</li>
 *   <li>{@code <html-to-xml>} - Convert HTML to XML for transformation</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see XsltPlugin
 */
@CorePlugin(
    elementName = "stylesheet",
    description = "Stylesheet plugin that provides stylesheet functionality",
    version = "2.1.0"
)
public class StylesheetPlugin extends AbstractPlugin {

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "stylesheet";
        }

        @Override
        public String getName() {
            return "Stylesheet Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "Stylesheet plugin that provides stylesheet functionality";
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
            return new String[]{"core", "stylesheet", "xslt", "xml", "transformation"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return StylesheetPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "stylesheet";
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
                                return "Optional identifier for the stylesheet element";
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
                }
            };
        }
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return METADATA.getConfigurationSchema();
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
        // Validate using schema
        getConfigurationSchema().validate(configuration);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Do nothing - this is a placeholder plugin
        return EmptyVariable.INSTANCE;
    }
}

















