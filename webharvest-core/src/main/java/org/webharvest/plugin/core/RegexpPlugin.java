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

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.WHConstants;
import org.webharvest.definition.CorePluginWebHarvestPluginDef;
import org.webharvest.definition.IElementDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regular expression plugin with full regression compatibility.
 * Provides 100% functionality of the old RegexpProcessor.
 */
@CorePlugin(
    elementName = "regexp",
    description = "Regular expression plugin with full regression compatibility",
    version = "2.1.0"
)
public class RegexpPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.RegexpPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        System.out.println("RegexpPlugin.doExecute: START - Method entered");
        LOGGER.debug("RegexpPlugin.doExecute: START - Method entered");
        PluginConfiguration config = getConfiguration();
        IElementDef elementDef = getElementDef();
        
        String pattern = "";
        String source = "";
        String result = "";
        
        // Check if we have a CorePluginWebHarvestPluginDef
        if (elementDef instanceof CorePluginWebHarvestPluginDef) {
            CorePluginWebHarvestPluginDef coreDef = (CorePluginWebHarvestPluginDef) elementDef;
            XmlNode xmlNode = coreDef.getXmlNode();
            
            // Get data from child elements using XmlNode directly
            pattern = getChildText(xmlNode, "regexp-pattern");
            source = getChildText(xmlNode, "regexp-source");
            result = getChildText(xmlNode, "regexp-result");
        }
        
        // Fallback to configuration properties for testing or when no XML structure
        if (pattern.isEmpty()) {
            pattern = config.getProperty("pattern", "");
            LOGGER.debug("RegexpPlugin: Pattern from config = '{}'", pattern);
        }
        if (source.isEmpty()) {
            source = config.getProperty("source", "");
            LOGGER.debug("RegexpPlugin: Source from config = '{}'", source);
        }
        if (result.isEmpty()) {
            result = config.getProperty("result", "");
            LOGGER.debug("RegexpPlugin: Result from config = '{}'", result);
        }
        
        System.out.println("RegexpPlugin.doExecute: Final pattern = '" + pattern + "'");
        System.out.println("RegexpPlugin.doExecute: Final source = '" + source + "'");
        LOGGER.debug("RegexpPlugin: Final pattern = '{}'", pattern);
        LOGGER.debug("RegexpPlugin: Final source = '{}'", source);
        
        // Use executeBody() to get content from children if source is empty
        if (source.isEmpty()) {
            System.out.println("RegexpPlugin.doExecute: Using executeBody() to get source content");
            LOGGER.debug("RegexpPlugin: Using executeBody() to get source content");
            Variable bodyResult = executeBody(context);
            if (bodyResult != null && !(bodyResult instanceof EmptyVariable)) {
                source = bodyResult.toString();
                System.out.println("RegexpPlugin.doExecute: Source from executeBody(): '" + source + "'");
                LOGGER.debug("RegexpPlugin: Source from executeBody(): '{}'", source);
            } else {
                System.out.println("RegexpPlugin.doExecute: executeBody() returned null or EmptyVariable");
                LOGGER.debug("RegexpPlugin: executeBody() returned null or EmptyVariable");
            }
        }

        // Get configuration properties for flags and options
        String replace = config.getProperty("replace", "false");
        String flagCaseInsensitive = config.getProperty("flag-caseinsensitive", "false");
        String flagMultiline = config.getProperty("flag-multiline", "false");
        String flagDotall = config.getProperty("flag-dotall", "false");
        String flagUnicodecase = config.getProperty("flag-unicodecase", "false");
        String flagCanoneq = config.getProperty("flag-canoneq", "false");
        String max = config.getProperty("max", "");

        if (pattern.isEmpty()) {
            LOGGER.warn("No pattern specified for regexp plugin");
            return new ListVariable();
        }

        if (source.isEmpty()) {
            LOGGER.warn("No source specified for regexp plugin");
            System.out.println("RegexpPlugin.doExecute: No source specified, returning empty ListVariable");
            return new ListVariable();
        }

        System.out.println("RegexpPlugin.doExecute: About to execute regexp on source: '" + source + "'");
        LOGGER.debug("RegexpPlugin: About to execute regexp on source: '{}'", source);

        try {
            // Build regex flags
            int flags = 0;
            if (CommonUtil.getBooleanValue(flagCaseInsensitive, false)) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            if (CommonUtil.getBooleanValue(flagMultiline, false)) {
                flags |= Pattern.MULTILINE;
            }
            if (CommonUtil.getBooleanValue(flagDotall, true)) { // Default to true as in original
                flags |= Pattern.DOTALL;
            }
            if (CommonUtil.getBooleanValue(flagUnicodecase, true)) { // Default to true as in original
                flags |= Pattern.UNICODE_CASE;
            }
            if (CommonUtil.getBooleanValue(flagCanoneq, false)) {
                flags |= Pattern.CANON_EQ;
            }

            System.out.println("RegexpPlugin.doExecute: Pattern: '" + pattern + "'");
            System.out.println("RegexpPlugin.doExecute: Source: '" + source + "'");
            System.out.println("RegexpPlugin.doExecute: Flags: " + flags);
            
            final Pattern compiledPattern = Pattern.compile(pattern, flags);
            final List<NodeVariable> resultList = new ArrayList<>();
            final boolean isReplace = CommonUtil.isBooleanTrue(replace);
            final double maxLoops = NumberUtils.toDouble(max, WHConstants.DEFAULT_MAX_LOOPS);
            
            // Log debug information instead of setting properties
            LOGGER.debug("RegexpPlugin: isReplace={}, maxLoops={}", isReplace, maxLoops);
            LOGGER.debug("Flags configured for RegexpPlugin pattern matching");

            List<Variable> sourceList = new ArrayList<>();
            sourceList.add(new NodeVariable(source));
            for (final Variable currVar : sourceList) {
                String text = currVar.toString();
                Matcher matcher = compiledPattern.matcher(text);
                int groupCount = matcher.groupCount();

                StringBuffer buffer = new StringBuffer();
                int index = 0;
                while (matcher.find()) {
                    index++;

                    if (maxLoops < index) {
                        break;
                    }

                    // Set group variables in context
                    for (int i = 0; i <= groupCount; i++) {
                        String groupValue = matcher.group(i);
                        context.setLocalVar("group" + i, new NodeVariable(groupValue != null ? groupValue : ""));
                    }

                    String currResult = (result == null || result.isEmpty()) ? matcher.group(0) : result;
                    if (isReplace) {
                        matcher.appendReplacement(buffer, currResult);
                    } else {
                        // Process the result template if it contains var elements
                        String processedResult = processResultTemplate(currResult, context);
                        resultList.add(new NodeVariable(processedResult));
                    }
                }

                if (isReplace) {
                    matcher.appendTail(buffer);
                    resultList.add(new NodeVariable(buffer.toString()));
                }
            }
            System.out.println("RegexpPlugin.doExecute: Regexp executed successfully, returning ListVariable with " + resultList.size() + " results");
            LOGGER.debug("RegexpPlugin: Regexp executed successfully, returning ListVariable with {} results", resultList.size());
            return new ListVariable(resultList);

        } catch (Exception e) {
            LOGGER.error("Error executing regexp plugin: " + e.getMessage(), e);
            return new ListVariable();
        }
    }

    /**
     * Process the result template, replacing var elements with variable values.
     * This provides full regression compatibility with the old RegexpProcessor.
     */
    private String processResultTemplate(String template, DynamicScopeContext context) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        try {
            // Get the regexp-result element from the XML node
            IElementDef elementDef = getElementDef();
            if (elementDef instanceof CorePluginWebHarvestPluginDef) {
                CorePluginWebHarvestPluginDef coreDef = (CorePluginWebHarvestPluginDef) elementDef;
                XmlNode xmlNode = coreDef.getXmlNode();
                
                // Find the regexp-result child element
                XmlNode resultNode = findChildElement(xmlNode, "regexp-result");
                if (resultNode != null) {
                    // Process the result template by executing child elements
                    return processXmlTemplate(resultNode, context);
                }
            }
            
            // Fallback to simple regex processing
            return processSimpleTemplate(template, context);
            
        } catch (Exception e) {
            LOGGER.warn("Error processing result template: " + e.getMessage(), e);
            return template;
        }
    }

    /**
     * Process XML template by executing child elements and concatenating results.
     */
    private String processXmlTemplate(XmlNode templateNode, DynamicScopeContext context) {
        StringBuilder result = new StringBuilder();
        
        List<java.io.Serializable> elementList = templateNode.getElementList();
        if (elementList != null) {
            for (java.io.Serializable element : elementList) {
                if (element instanceof String) {
                    // Add text content
                    result.append((String) element);
                } else if (element instanceof XmlNode) {
                    XmlNode childNode = (XmlNode) element;
                    if ("var".equals(childNode.getName())) {
                        // Execute var element as a plugin
                        String varName = childNode.getAttribute("name");
                        if (varName != null) {
                            Variable varValue = context.getVar(varName);
                            String replacement = (varValue != null) ? varValue.toString() : "";
                            result.append(replacement);
                        }
                    } else {
                        // Add other elements as text
                        result.append(childNode.getText());
                    }
                }
            }
        }
        
        return result.toString();
    }

    /**
     * Simple template processing for var elements using regex.
     */
    private String processSimpleTemplate(String template, DynamicScopeContext context) {
        // Simple template processing for var elements
        // Pattern: <var name='group0'/> -> value of group0 variable
        Pattern varPattern = Pattern.compile("<var\\s+name=['\"]([^'\"]+)['\"][^>]*/>");
        Matcher matcher = varPattern.matcher(template);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Variable varValue = context.getVar(varName);
            String replacement = (varValue != null) ? varValue.toString() : "";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Helper method to find a child element by name.
     */
    private XmlNode findChildElement(XmlNode parentNode, String elementName) {
        if (parentNode == null) {
            return null;
        }

        List<java.io.Serializable> elementList = parentNode.getElementList();
        if (elementList != null) {
            for (java.io.Serializable element : elementList) {
                if (element instanceof XmlNode) {
                    XmlNode childNode = (XmlNode) element;
                    if (elementName.equals(childNode.getName())) {
                        return childNode;
                    }
                }
            }
        }
        return null;
    }


    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "regexp";
            }

            @Override
            public String getName() {
                return "regexp";
            }

            @Override
            public String getVersion() {
                return "1.0.0";
            }

                       @Override
                       public String getDescription() {
                           return "Regular expression replace plugin";
                       }

            @Override
            public String getAuthor() {
                return "Web-Harvest Team";
            }

            @Override
            public String getCategory() {
                return "Core Web-Harvest plugins";
            }

            @Override
            public String[] getTags() {
                return new String[]{"core", "regexp", "regex", "pattern", "matching", "replacement"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return RegexpPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "regexp";
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
                        return new String[]{"regexp-pattern", "regexp-source"};
                    }

                    @Override
                    public String[] getAllowedSubElements() {
                        return new String[]{"regexp-pattern", "regexp-source", "regexp-result"};
                    }

                    @Override
                    public String[] getOptionalAttributes() {
                        return new String[]{"replace", "max", "flag-caseinsensitive", "flag-multiline", "flag-dotall", "flag-unicodecase", "flag-canoneq"};
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
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return getMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) {
        // Validate configuration first
        doValidateConfiguration(configuration);
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) {
        // No specific validation needed for regexp plugin placeholder
    }

    /**
     * Helper method to get text content from a child element by name.
     */
    protected String getChildText(XmlNode parentNode, String elementName) {
        if (parentNode == null) {
            return "";
        }

        List<java.io.Serializable> elementList = parentNode.getElementList();
        if (elementList != null) {
            for (java.io.Serializable element : elementList) {
                if (element instanceof XmlNode) {
                    XmlNode childNode = (XmlNode) element;
                    if (elementName.equals(childNode.getName())) {
                        return childNode.getText();
                    }
                }
            }
        }
        return "";
    }
}
