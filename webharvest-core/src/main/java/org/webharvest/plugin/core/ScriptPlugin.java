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
import org.webharvest.runtime.scripting.ScriptEngineFactory;
import org.webharvest.runtime.scripting.ScriptSource;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script execution processor - execute JavaScript, Groovy, or BeanShell code for complex logic.
 * 
 * <p>
 * The {@code <script>} processor enables execution of arbitrary code in multiple scripting
 * languages. This is essential for complex data transformations, arithmetic operations,
 * API calls, conditional logic, and any processing that's difficult to express in pure XML.
 * </p>
 * 
 * <h2>Supported Languages</h2>
 * <ul>
 *   <li><strong>JavaScript (default):</strong> Rhino engine, ECMAScript 5 compatible</li>
 *   <li><strong>Groovy:</strong> Full Groovy language support with Java interop</li>
 *   <li><strong>BeanShell:</strong> Lightweight Java-like scripting</li>
 * </ul>
 * 
 * <h3>Context API</h3>
 * <p>Scripts have access to the WebHarvest context via the {@code context} object:</p>
 * <ul>
 *   <li><strong>context.getVar("name"):</strong> Get variable value (returns Variable object)</li>
 *   <li><strong>context.setVar("name", value):</strong> Set variable (deprecated, use return value)</li>
 *   <li><strong>context.getCharset():</strong> Get current character encoding</li>
 *   <li><strong>context.getScriptingLanguage():</strong> Get active scripting language</li>
 * </ul>
 * 
 * <p><strong>Accessing variable values as String:</strong></p>
 * <pre>{@code
 * var username = context.getVar("username").toString();
 * var count = parseInt(context.getVar("count").toString());
 * }</pre>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Arithmetic operations (real math, not string concat)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="price" value="99.99"/>
 * <def var="quantity" value="5"/>
 * 
 * <def var="total">
 *   <script language="javascript">
 *     <![CDATA[
 *       var price = parseFloat(context.getVar("price").toString());
 *       var qty = parseInt(context.getVar("quantity").toString());
 *       price * qty;  // Returns: 499.95
 *     ]]>
 *   </script>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: String manipulation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="email" value="USER@EXAMPLE.COM"/>
 * 
 * <def var="normalizedEmail">
 *   <script>
 *     <![CDATA[
 *       var email = context.getVar("email").toString();
 *       email.toLowerCase();  // Returns: user@example.com
 *     ]]>
 *   </script>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Conditional logic</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="httpStatus" value="404"/>
 * 
 * <def var="shouldRetry">
 *   <script>
 *     <![CDATA[
 *       var status = parseInt(context.getVar("httpStatus").toString());
 *       
 *       if (status >= 500 && status < 600) {
 *         "true";  // Server error - retry
 *       } else if (status == 429) {
 *         "true";  // Rate limit - retry
 *       } else {
 *         "false";  // Don't retry
 *       }
 *     ]]>
 *   </script>
 * </def>
 * 
 * <if condition="${shouldRetry}">
 *   <sleep time="5000"/>
 *   <http url="${url}"/>
 * </if>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: JSON parsing and manipulation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="jsonData">
 *   <http url="https://api.example.com/data"/>
 * </def>
 * 
 * <def var="extractedValue">
 *   <script>
 *     <![CDATA[
 *       var json = context.getVar("jsonData").toString();
 *       var obj = JSON.parse(json);
 *       obj.users[0].name;  // Extract nested value
 *     ]]>
 *   </script>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: Date formatting</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="timestamp" value="1609459200000"/>
 * 
 * <def var="formattedDate">
 *   <script>
 *     <![CDATA[
 *       var ts = parseInt(context.getVar("timestamp").toString());
 *       var date = new Date(ts);
 *       date.toISOString();  // Returns: 2021-01-01T00:00:00.000Z
 *     ]]>
 *   </script>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 6: Complex URL building</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="baseUrl" value="https://api.example.com"/>
 * <def var="endpoint" value="users"/>
 * <def var="userId" value="123"/>
 * <def var="fields">name,email,age</def>
 * 
 * <def var="fullUrl">
 *   <script>
 *     <![CDATA[
 *       var base = context.getVar("baseUrl").toString();
 *       var ep = context.getVar("endpoint").toString();
 *       var id = context.getVar("userId").toString();
 *       var fields = context.getVar("fields").toString();
 *       
 *       base + "/" + ep + "/" + id + "?fields=" + encodeURIComponent(fields);
 *       // Returns: https://api.example.com/users/123?fields=name%2Cemail%2Cage
 *     ]]>
 *   </script>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 7: Groovy for Java library access</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="uuid">
 *   <script language="groovy">
 *     <![CDATA[
 *       import java.util.UUID
 *       UUID.randomUUID().toString()
 *     ]]>
 *   </script>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 8: Counter with real increment</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="counter" value="0"/>
 * 
 * <loop maxloops="10">
 *   <!-- Real arithmetic increment -->
 *   <def var="counter">
 *     <script>
 *       <![CDATA[
 *         var current = parseInt(context.getVar("counter").toString());
 *         current + 1;
 *       ]]>
 *     </script>
 *   </def>
 *   
 *   <http url="https://example.com/page/${counter}"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Language Selection</h3>
 * 
 * <p>Set language via attribute or config default:</p>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- JavaScript (default) -->
 * <script>
 *   var x = 42;
 * </script>
 * 
 * <!-- Groovy -->
 * <script language="groovy">
 *   def x = 42
 * </script>
 * 
 * <!-- BeanShell -->
 * <script language="beanshell">
 *   int x = 42;
 * </script>
 * </config>
 * }</pre>
 * 
 * <h3>Return Values</h3>
 * <p>
 * The last expression in the script is returned as the processor result.
 * It's automatically converted to a Variable object:
 * </p>
 * <ul>
 *   <li><strong>String:</strong> Wrapped in NodeVariable</li>
 *   <li><strong>Number:</strong> Converted to String, wrapped in NodeVariable</li>
 *   <li><strong>Boolean:</strong> Converted to "true"/"false"</li>
 *   <li><strong>null/undefined:</strong> EmptyVariable</li>
 *   <li><strong>Variable object:</strong> Returned as-is</li>
 * </ul>
 * 
 * <h3>Best Practices</h3>
 * <ul>
 *   <li><strong>Use CDATA:</strong> Wrap script in {@code <![CDATA[...]]>} to avoid XML escaping issues</li>
 *   <li><strong>Error handling:</strong> Use try-catch in scripts for robust execution</li>
 *   <li><strong>Variable conversion:</strong> Always call {@code .toString()} on Variable objects</li>
 *   <li><strong>Type safety:</strong> Use parseInt(), parseFloat() for numeric operations</li>
 *   <li><strong>Keep it simple:</strong> Complex logic should be in Java plugins, not scripts</li>
 * </ul>
 * 
 * <h3>Common Use Cases</h3>
 * <ul>
 *   <li><strong>Math operations:</strong> Real arithmetic (multiplication, division, etc.)</li>
 *   <li><strong>String processing:</strong> Regex, formatting, encoding</li>
 *   <li><strong>Date/time handling:</strong> Parsing, formatting, calculations</li>
 *   <li><strong>Conditional logic:</strong> Complex if/else branching</li>
 *   <li><strong>JSON manipulation:</strong> Parsing, transformation, extraction</li>
 *   <li><strong>API signatures:</strong> HMAC, OAuth, request signing</li>
 * </ul>
 * 
 * <h3>Performance Considerations</h3>
 * <ul>
 *   <li><strong>Script compilation:</strong> Scripts are compiled once per execution (cached)</li>
 *   <li><strong>Engine initialization:</strong> First script execution may be slower</li>
 *   <li><strong>Groovy overhead:</strong> Groovy is slower than JavaScript but more powerful</li>
 *   <li><strong>Avoid in loops:</strong> Extract logic outside loop when possible</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>CDATA required:</strong> Use {@code <![CDATA[...]]>} for any code with {@code <, >, &}</li>
 *   <li><strong>Context access:</strong> {@code context} object is always available</li>
 *   <li><strong>Last expression:</strong> The final evaluated expression is returned</li>
 *   <li><strong>No side effects:</strong> Prefer returning values over context.setVar()</li>
 *   <li><strong>Thread safety:</strong> Each execution has isolated script context</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <template>} - Simple variable interpolation (faster than script)</li>
 *   <li>{@code <def>} - Variable assignment</li>
 *   <li>{@code <function>} - Define reusable script functions</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "script",
    description = "Script plugin that executes script code in various scripting languages",
    version = "2.1.0",
    category = "Processing"
)
public class ScriptPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.ScriptPlugin");

    @Inject
    private ScriptEngineFactory scriptEngineFactory;

    /**
     * Metadata for ScriptPlugin.
     * 
     * <p>Standalone plugin - executes script code in various scripting languages (JavaScript, Groovy, BeanShell).</p>
     */
    public static class ScriptPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "script";
        }

        @Override
        public String getName() {
            return "Script";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Executes script code in various scripting languages (JavaScript, Groovy, BeanShell). Enables complex data transformations, arithmetic operations, string manipulations, and custom logic that cannot be expressed in XML. Essential for advanced data processing scenarios.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = ScriptPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Processing";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "script", "javascript", "groovy", "beanshell", "processing"};
        }

        @Override
        public String[] getDependencies() {
            return new String[]{"ScriptEngineFactory"};
        }

        @Override
        public String getClassName() {
            return ScriptPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "script";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new ScriptPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-code";
        }
        
        @Override
        public String getColor() {
            return "#F59E0B"; // Amber for scripting
        }
        
        @Override
        public String getGroup() {
            return "Functions & Scripting";
        }
        
        @Override
        public int getDefaultWidth() {
            return 240;
        }
        
        @Override
        public int getDefaultHeight() {
            return 180;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("code", "Script code to execute (from body content)", PortType.STRING, false),
                new InputPort("language", "Scripting language (javascript, groovy, beanshell) (from language attribute)", PortType.STRING, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Script execution result", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Executes script code in various scripting languages (JavaScript, Groovy, BeanShell). Enables complex data transformations, arithmetic operations, string manipulations, and custom logic. Essential for advanced data processing scenarios.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+S", "Add Script", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Script Plugin",
                "Executes script code in various scripting languages (JavaScript, Groovy, BeanShell).",
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
     * Configuration schema for ScriptPlugin.
     */
    public static class ScriptPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the script element";
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
                },
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "language";
                    }

                    @Override
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.ENUM;
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
                        return "Scripting language to use (beanshell, javascript, groovy)";
                    }

                    @Override
                    public String[] getAllowedValues() {
                        return new String[]{"beanshell", "javascript", "groovy"};
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
                },
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "return";
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
                        return "Return expression for backward compatibility with ver.2b1";
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
            return new String[]{"id", "language", "return"};
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
            // Additional validation can be added here if needed
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new ScriptPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new ScriptPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        // Validate scripting language if provided
        String languageStr = configuration.getProperty("language");
        if (languageStr != null && !languageStr.trim().isEmpty()) {
            ScriptingLanguage language = ScriptingLanguage.recognize(languageStr);
            if (language == null) {
                throw new PluginValidationException("Unsupported scripting language: " + languageStr);
            }
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Execute body content to get script source code
        Variable bodyContent = executeBody(context);
        String sourceCode = "";
        
        if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
            sourceCode = bodyContent.toString();
            LOGGER.debug("ScriptPlugin: Retrieved script content: " + sourceCode);
        } else {
            LOGGER.debug("ScriptPlugin: No body content available");
            return EmptyVariable.INSTANCE;
        }

        // For backward compatibility with ver.2b1 only!
        final String returnExpression = super.getConfiguration().getProperty("return");
        if (returnExpression != null) {
            sourceCode += ("; " + BaseTemplater.evaluateToString(returnExpression, null, context));
        }

        final ScriptSource scriptSource = new ScriptSource(sourceCode,
                evaluateScriptingLanguage(context));
        
        // Use ScriptEngineFactory if available, otherwise fallback to BaseTemplater
        if (scriptEngineFactory != null) {
            return CommonUtil.createVariable(scriptEngineFactory
                    .getEngine(scriptSource).evaluate(context, scriptSource));
        } else {
            LOGGER.warn("ScriptEngineFactory not injected, using BaseTemplater fallback");
            return BaseTemplater.evaluateToVariable(sourceCode, evaluateScriptingLanguage(context), context);
        }
    }

    private ScriptingLanguage evaluateScriptingLanguage(
            final DynamicScopeContext context) {
        String languageStr = super.getConfiguration().getProperty("language");
        if (languageStr != null) {
            languageStr = BaseTemplater.evaluateToString(languageStr, null, context);
        }
        ScriptingLanguage language = ScriptingLanguage.recognize(languageStr);
        return language != null ? language : context.getScriptingLanguage();
    }
}
