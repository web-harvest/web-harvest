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

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.WHConstants;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.DefaultPluginBodyExecutor;
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
 * Regular expression processor - pattern matching, extraction, and text replacement.
 * 
 * <p>
 * The {@code <regexp>} processor provides powerful regular expression capabilities for:
 * pattern matching, text extraction using capture groups, search and replace operations,
 * and complex text parsing. It automatically stores captured groups as variables for
 * easy access.
 * </p>
 * 
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>Pattern matching:</strong> Find patterns in text using Java regex syntax</li>
 *   <li><strong>Capture groups:</strong> Extract parts of matches into variables (group0-group99)</li>
 *   <li><strong>Search &amp; replace:</strong> Replace matched text with templates</li>
 *   <li><strong>Multiple matches:</strong> Process all matches or limit with max attribute</li>
 *   <li><strong>Regex flags:</strong> Case-insensitive, multiline, dotall, etc.</li>
 * </ul>
 * 
 * <h3>Captured Group Variables (Bug #62 Fixed in v2.2.0)</h3>
 * <p>
 * Capture groups from pattern matching are automatically stored as variables:
 * </p>
 * <ul>
 *   <li><strong>${group0}:</strong> Full matched text (entire pattern match)</li>
 *   <li><strong>${group1}:</strong> First capturing group (...)</li>
 *   <li><strong>${group2}:</strong> Second capturing group</li>
 *   <li><strong>${groupN}:</strong> Nth capturing group (up to 100 groups)</li>
 * </ul>
 * 
 * <p>
 * <strong>Bug #62 Fix:</strong> Prior to v2.2.0, sequential (non-nested) regexp calls
 * would incorrectly share group variables. Groups are now properly cleared before each
 * regexp execution, ensuring complete isolation.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Extract email addresses</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="emails">
 *   <regexp>
 *     <regexp-pattern>([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})</regexp-pattern>
 *     <regexp-source>
 *       <http url="https://example.com/contacts"/>
 *     </regexp-source>
 *   </regexp>
 * </def>
 * 
 * <loop item="email">
 *   ${emails}
 *   
 *   <file path="emails.txt" action="append">${email}&#10;</file>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Parse key-value pairs with capture groups</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="data">name:John;email:john@example.com;age:30</def>
 * 
 * <loop item="pair">
 *   <regexp>
 *     <regexp-pattern>([^;]+)</regexp-pattern>
 *     <regexp-source>${data}</regexp-source>
 *   </regexp>
 *   
 *   <!-- For each pair, extract key and value -->
 *   <regexp>
 *     <regexp-pattern>([^:]+):([^:]+)</regexp-pattern>
 *     <regexp-source>${pair}</regexp-source>
 *   </regexp>
 *   
 *   <!-- group1 = key, group2 = value (Bug #62: properly isolated!) -->
 *   <def var="${group1}" value="${group2}"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Find and replace</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="html">
 *   <http url="https://example.com/page"/>
 * </def>
 * 
 * <!-- Replace all http:// with https:// -->
 * <def var="secureHtml">
 *   <regexp replace="true">
 *     <regexp-pattern>http://</regexp-pattern>
 *     <regexp-source>${html}</regexp-source>
 *     <regexp-result>https://</regexp-result>
 *   </regexp>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Extract URLs with named groups</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="url">
 *   <regexp>
 *     <regexp-pattern>https?://([^/]+)(/.*)}</regexp-pattern>
 *     <regexp-source>
 *       <http url="https://example.com/links"/>
 *     </regexp-source>
 *   </regexp>
 *   
 *   <!-- group0 = full URL, group1 = domain, group2 = path -->
 *   <file path="urls.txt" action="append">
 *     URL: ${group0}
 *     Domain: ${group1}
 *     Path: ${group2}
 *   </file>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: Case-insensitive search</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp flag-caseinsensitive="true">
 *   <regexp-pattern>error|warning|fail</regexp-pattern>
 *   <regexp-source>
 *     <file path="log.txt" action="read"/>
 *   </regexp-source>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h3>Child Elements</h3>
 * <ul>
 *   <li><strong>{@code <regexp-pattern>}:</strong> Regular expression pattern (required)</li>
 *   <li><strong>{@code <regexp-source>}:</strong> Text to search/process (required)</li>
 *   <li><strong>{@code <regexp-result>}:</strong> Template for formatting results (optional)</li>
 * </ul>
 * 
 * <h3>Attributes</h3>
 * <ul>
 *   <li><strong>replace:</strong> If true, performs find & replace (default: false)</li>
 *   <li><strong>max:</strong> Maximum matches to process (default: 1000000)</li>
 *   <li><strong>flag-caseinsensitive:</strong> Ignore case (default: false)</li>
 *   <li><strong>flag-multiline:</strong> ^ and $ match line boundaries (default: false)</li>
 *   <li><strong>flag-dotall:</strong> . matches newlines (default: true)</li>
 *   <li><strong>flag-unicodecase:</strong> Unicode-aware case folding (default: true)</li>
 *   <li><strong>flag-canoneq:</strong> Canonical equivalence (default: false)</li>
 * </ul>
 * 
 * <h3>Common Patterns</h3>
 * <pre>{@code
 * <!-- Extract emails -->
 * <regexp-pattern>([\\w.-]+@[\\w.-]+\\.\\w+)</regexp-pattern>
 * 
 * <!-- Extract phone numbers -->
 * <regexp-pattern>\\d{3}-\\d{3}-\\d{4}</regexp-pattern>
 * 
 * <!-- Extract between tags -->
 * <regexp-pattern><title>(.*?)</title></regexp-pattern>
 * 
 * <!-- Split CSV -->
 * <regexp-pattern>([^,]+)</regexp-pattern>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Group isolation:</strong> Groups cleared before each regexp (Bug #62 fixed)</li>
 *   <li><strong>Java regex syntax:</strong> Uses {@code java.util.regex.Pattern}</li>
 *   <li><strong>Returns ListVariable:</strong> All matches as list items</li>
 *   <li><strong>Max limit:</strong> Default 1M matches (adjust if needed)</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <tokenize>} - Simple text splitting</li>
 *   <li>{@code <xpath>} - XML querying</li>
 *   <li>{@code <script>} - Complex regex with JavaScript</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see java.util.regex.Pattern
 * @see RegexpPatternPlugin
 * @see RegexpSourcePlugin
 * @see RegexpResultPlugin
 */
@CorePlugin(
    elementName = "regexp",
    description = "Regular expression plugin with group isolation (Bug #62 fixed in v2.2.0)",
    version = "2.2.0",
    category = "Extraction"
)
public class RegexpPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.RegexpPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("RegexpPlugin.doExecute: START - Method entered");
        LOGGER.debug("RegexpPlugin.doExecute: START - Method entered");
        PluginConfiguration config = getConfiguration();
        
        String pattern = "";
        String source = "";
        String result = "";
        
        // v2.2.x ULTRA-LEAN: Get data from child configurations
        // StreamingXmlParser populates children with PluginConfiguration objects
        if (config.hasChildren()) {
            for (PluginConfiguration child : config.getChildren()) {
                String elementName = child.getElementName();

                if ("regexp-pattern".equals(elementName)) {
                    pattern = extractChildText(child, context);
                } else if ("regexp-source".equals(elementName)) {
                    source = extractChildText(child, context);
                } else if ("regexp-result".equals(elementName)) {
                    result = child.getProperty("_textContent", "");
                }
            }
        }
        
        // Fallback to configuration properties for testing or when no children
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
        
        LOGGER.debug("RegexpPlugin.doExecute: Final pattern = '" + pattern + "'");
        LOGGER.debug("RegexpPlugin.doExecute: Final source = '" + source + "'");
        LOGGER.debug("RegexpPlugin: Final pattern = '{}'", pattern);
        LOGGER.debug("RegexpPlugin: Final source = '{}'", source);
        
        // Use executeBody() to get content from children if source is empty
        if (source.isEmpty()) {
            LOGGER.debug("RegexpPlugin.doExecute: Using executeBody() to get source content");
            LOGGER.debug("RegexpPlugin: Using executeBody() to get source content");
            Variable bodyResult = executeBody(context);
            if (bodyResult != null && !(bodyResult instanceof EmptyVariable)) {
                source = bodyResult.toString();
                LOGGER.debug("RegexpPlugin.doExecute: Source from executeBody(): '" + source + "'");
                LOGGER.debug("RegexpPlugin: Source from executeBody(): '{}'", source);
            } else {
                LOGGER.debug("RegexpPlugin.doExecute: executeBody() returned null or EmptyVariable");
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
            LOGGER.debug("RegexpPlugin.doExecute: No source specified, returning empty ListVariable");
            return new ListVariable();
        }

        LOGGER.debug("RegexpPlugin.doExecute: About to execute regexp on source: '" + source + "'");
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

            LOGGER.debug("RegexpPlugin.doExecute: Pattern: '" + pattern + "'");
            LOGGER.debug("RegexpPlugin.doExecute: Source: '" + source + "'");
            LOGGER.debug("RegexpPlugin.doExecute: Flags: " + flags);
            
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

                    // BUGFIX #62: Clear ALL previous group variables before setting new ones
                    // This prevents group values from previous regexp executions from persisting
                    clearAllGroupVariables(context);

                    // Set group variables in context for THIS regexp execution
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
            LOGGER.debug("RegexpPlugin.doExecute: Regexp executed successfully, returning ListVariable with " + resultList.size() + " results");
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
            // v2.2.x ULTRA-LEAN: Find regexp-result child in configuration
            PluginConfiguration resultConfig = null;
            if (getConfiguration().hasChildren()) {
                for (PluginConfiguration child : getConfiguration().getChildren()) {
                    if ("regexp-result".equals(child.getElementName())) {
                        resultConfig = child;
                        break;
                    }
                }
            }
            
            if (resultConfig != null && resultConfig.hasChildren()) {
                // Process the result template by executing child elements
                // Use PluginBodyExecutor to execute regexp-result children
                org.webharvest.plugin.PluginBodyExecutor executor = new org.webharvest.plugin.DefaultPluginBodyExecutor();
                Variable execResult = executor.executeBody(resultConfig, context);
                return execResult != null ? execResult.toString() : template;
            }
            
            // Fallback to simple regex processing
            return processSimpleTemplate(template, context);
            
        } catch (Exception e) {
            LOGGER.warn("Error processing result template: " + e.getMessage(), e);
            return template;
        }
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
     * Metadata for RegexpPlugin.
     * 
     * <p>Master plugin - requires regexp-pattern and regexp-source as children. Provides powerful regular expression capabilities for pattern matching, text extraction using capture groups, search and replace operations, and complex text parsing.</p>
     */
    public static class RegexpPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "regexp";
        }

        @Override
        public String getName() {
            return "Regular Expression";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Regular expression processor providing powerful pattern matching, text extraction using capture groups, search and replace operations, and complex text parsing. Automatically stores captured groups as variables (group0-group99) for easy access. Supports multiple matches, regex flags (case-insensitive, multiline, dotall, etc.), and search/replace operations with templates.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = RegexpPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Extraction";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "regexp", "regex", "pattern", "matching", "replacement", "extraction", "text-processing"};
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
            return new RegexpPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-search";
        }
        
        @Override
        public String getColor() {
            return "#10B981"; // Green for extraction/text processing
        }
        
        @Override
        public String getGroup() {
            return "Extraction";
        }
        
        @Override
        public int getDefaultWidth() {
            return 220;
        }
        
        @Override
        public int getDefaultHeight() {
            return 140;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("source", "Text source to match against", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("matches", "List of matched results", PortType.MIXED),
                new OutputPort("replaced", "Text with replacements applied (if replace attribute used)", PortType.STRING)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Regular expression processor for pattern matching, text extraction, and search/replace. Automatically stores captured groups as variables (group0-group99).";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+R", "Add Regexp", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Regular Expression Plugin",
                "Pattern matching and text extraction using regular expressions. Captures groups automatically as variables.",
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
            return new String[]{"regexp-pattern", "regexp-source", "regexp-result"};
        }
    }
    
    /**
     * Configuration schema for RegexpPlugin.
     */
    public static class RegexpPluginConfigurationSchema implements PluginConfigurationSchema {
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
            return "2.3.0";
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
    }

    @Override
    public PluginMetadata getMetadata() {
        return new RegexpPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new RegexpPluginMetadata().getConfigurationSchema();
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

    private String extractChildText(PluginConfiguration child, DynamicScopeContext context) throws InterruptedException {
        String direct = child.getProperty("_textContent", "");
        if (direct != null && !direct.trim().isEmpty()) {
            return direct;
        }

        DefaultPluginBodyExecutor executor = new DefaultPluginBodyExecutor();
        Variable evaluated = executor.getBodyTextContent(child, context);
        if (evaluated == null || evaluated instanceof EmptyVariable) {
            return "";
        }
        return evaluated.toString();
    }

    /**
     * Clear all group variables from context (group0, group1, group2, etc.)
     * This fixes Bug #62: prevents group values from previous regexp executions
     * from persisting and interfering with subsequent regexp calls.
     * 
     * @param context the context to clear group variables from
     */
    private void clearAllGroupVariables(DynamicScopeContext context) {
        // Clear up to 100 group variables (more than enough for any practical regexp)
        // We don't know how many groups existed before, so clear a reasonable maximum
        for (int i = 0; i < 100; i++) {
            String groupVarName = "group" + i;
            try {
                // Try to remove the variable if it exists
                // Note: ScraperContext doesn't have a removeVar method, so we set to empty
                context.setLocalVar(groupVarName, EmptyVariable.INSTANCE);
            } catch (Exception e) {
                // Ignore any exceptions (e.g., if variable doesn't exist)
                // This is just defensive cleanup
            }
        }
    }
}
