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
 * Regexp result template - used within {@code <regexp>} to format extracted matches.
 * 
 * <p>
 * The {@code <regexp-result>} processor defines output format for regex matches using capture group references.
 * Must be used as a child element of {@code <regexp>}. Supports ${group0}, ${group1}, etc. for accessing matched groups.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h4>Example 1: Basic group formatting</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[<a href="([^"]+)">([^<]+)</a>]]></regexp-pattern>
 *   <regexp-source>${html}</regexp-source>
 *   <regexp-result>URL: ${group1}, Text: ${group2}</regexp-result>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Phone number formatting</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[(\\d{3})-(\\d{3})-(\\d{4})]]></regexp-pattern>
 *   <regexp-source>Call 555-123-4567 for support</regexp-source>
 *   <regexp-result>(${group1}) ${group2}-${group3}</regexp-result>
 * </regexp>
 * <!-- Output: (555) 123-4567 -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Email extraction with formatting</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)\\.([a-zA-Z]{2,})]]></regexp-pattern>
 *   <regexp-source>Contact: john.doe@example.com</regexp-source>
 *   <regexp-result>User: ${group1}, Domain: ${group2}.${group3}</regexp-result>
 * </regexp>
 * <!-- Output: User: john.doe, Domain: example.com -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Complex data extraction</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[<div class="product" data-id="(\\d+)" data-name="([^"]+)" data-price="([\\d.]+)">]]></regexp-pattern>
 *   <regexp-source>${productHtml}</regexp-source>
 *   <regexp-result>
 *     <product>
 *       <id>${group1}</id>
 *       <name>${group2}</name>
 *       <price>${group3}</price>
 *     </product>
 *   </regexp-result>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>Content (body)</strong>: Output template with group references</li>
 * </ul>
 * 
 * <h3>Group References</h3>
 * <ul>
 *   <li><strong>${group0}</strong> - Full match (entire matched text)</li>
 *   <li><strong>${group1}</strong> - First capture group (first parentheses)</li>
 *   <li><strong>${group2}</strong> - Second capture group (second parentheses)</li>
 *   <li><strong>${group3}</strong> - Third capture group, etc.</li>
 * </ul>
 * 
 * <h3>Template Features</h3>
 * <ul>
 *   <li><strong>Static Text:</strong> Can include literal text alongside group references</li>
 *   <li><strong>XML Output:</strong> Can generate XML/HTML structure</li>
 *   <li><strong>Multiple Groups:</strong> Can reference multiple capture groups in one template</li>
 *   <li><strong>Conditional:</strong> Groups that don't match will be empty</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <regexp>} element</li>
 *   <li><strong>Group References</strong>: ${group0} = full match, ${group1} = first capture group</li>
 *   <li><strong>Output Formatting</strong>: Defines how matched groups are formatted</li>
 *   <li><strong>Multiple Matches</strong>: Template applied to each match found</li>
 *   <li><strong>Empty Groups</strong>: Unmatched groups will be empty strings</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <regexp>} - Main regexp processor (parent element)</li>
 *   <li>{@code <regexp-pattern>} - Regular expression pattern</li>
 *   <li>{@code <regexp-source>} - Source text for matching</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see RegexpPlugin
 * @see RegexpPatternPlugin
 * @see RegexpSourcePlugin
 */
@CorePlugin(
    elementName = "regexp-result",
    description = "Regexp result plugin that provides regexp result functionality",
    version = "2.1.0"
)
public class RegexpResultPlugin extends AbstractPlugin {

    /**
     * Metadata for RegexpResultPlugin.
     * 
     * <p>Detail plugin - requires regexp as parent. Defines output format for regex matches using capture group references. Supports ${group0}, ${group1}, etc. for accessing matched groups.</p>
     */
    public static class RegexpResultPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() { return "regexp-result"; }
        @Override
        public String getName() { return "Regexp Result"; }
        @Override
        public String getVersion() { return "2.3.0"; }
        @Override
        public String getDescription() {
            return "Defines output format for regex matches using capture group references. Must be used within regexp element. Supports ${group0} (full match), ${group1} (first capture group), ${group2}, etc. Template applied to each match found. Can generate XML/HTML structure.";
        }
        @Override
        public String getAuthor() { return "Web-Harvest Team"; }
        @Override
        public String getCategory() {
            CorePlugin annotation = RegexpResultPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Extraction";
        }
        @Override
        public String[] getTags() {
            return new String[]{"core", "regexp", "result", "regex", "replacement", "formatting", "extraction"};
        }
        @Override
        public String[] getDependencies() { return new String[0]; }
        @Override
        public String getClassName() { return RegexpResultPlugin.class.getName(); }
        @Override
        public boolean isCorePlugin() { return true; }
        @Override
        public String getElementName() { return "regexp-result"; }
        @Override
        public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new RegexpResultPluginConfigurationSchema();
        }
        @Override
        public String getIcon() { return "fas fa-edit"; }
        @Override
        public String getColor() { return "#10B981"; }
        @Override
        public String getGroup() { return "Extraction"; }
        @Override
        public int getDefaultWidth() { return 200; }
        @Override
        public int getDefaultHeight() { return 100; }
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("template", "Output template with group references (from body content)", PortType.STRING, true)
            };
        }
        @Override
        public OutputPort[] getOutputPorts() { return new OutputPort[0]; }
        @Override
        public PortType getPortType() { return PortType.STRING; }
        @Override
        public String getTooltip() {
            return "Defines output format for regex matches using capture group references. Supports ${group0}, ${group1}, etc.";
        }
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() { return new KeyboardShortcut[0]; }
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Regexp Result Plugin",
                "Defines output format for regex matches using capture group references.",
                new String[]{"Tab", "Enter"}
            );
        }
        @Override
        public LinkedServiceMetadata[] getLinkedServices() { return new LinkedServiceMetadata[0]; }
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("regexp", true, "Regular Expression");
        }
        @Override
        public String[] getAllowedChildPlugins() { return new String[0]; }
    }
    
    /**
     * Configuration schema for RegexpResultPlugin.
     */
    public static class RegexpResultPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the regexp-result element";
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
        return new RegexpResultPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new RegexpResultPluginMetadata().getConfigurationSchema();
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

















