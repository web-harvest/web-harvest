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
 * Regexp pattern definition - used within {@code <regexp>} to specify regex pattern.
 * 
 * <p>
 * The {@code <regexp-pattern>} processor defines the regular expression pattern for matching and extraction.
 * Must be used as a child element of {@code <regexp>}. Supports Java regex syntax with capture groups.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Extract links from HTML</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[<a href="([^"]+)">]]></regexp-pattern>
 *   <regexp-source>${htmlPage}</regexp-source>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Extract emails</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})]]></regexp-pattern>
 *   <regexp-source>${textContent}</regexp-source>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Price extraction with capture groups</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[Price:\s*\$(\d+)\.(\d{2})]]></regexp-pattern>
 *   <regexp-source>Price: $19.99</regexp-source>
 *   <regexp-result index="1"/><!-- Returns "19" -->
 *   <regexp-result index="2"/><!-- Returns "99" -->
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Complex pattern with multiple groups</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[<div class="product" data-id="(\d+)" data-name="([^"]+)" data-price="([\d.]+)">]]></regexp-pattern>
 *   <regexp-source>${productHtml}</regexp-source>
 * </regexp>
 * <!-- Access groups with <regexp-result index="1"/>, <regexp-result index="2"/>, etc. -->
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>Content (body)</strong>: The regular expression pattern</li>
 * </ul>
 * 
 * <h3>Pattern Syntax</h3>
 * <p>Uses Java regex syntax. Common patterns:</p>
 * <ul>
 *   <li><strong>.*</strong> - Match any character (greedy)</li>
 *   <li><strong>.*?</strong> - Match any character (non-greedy)</li>
 *   <li><strong>\d+</strong> - Match one or more digits</li>
 *   <li><strong>\w+</strong> - Match one or more word characters</li>
 *   <li><strong>[^"]</strong> - Match any character except quotes</li>
 *   <li><strong>(...)</strong> - Capture group (accessible via index)</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <regexp>} element</li>
 *   <li><strong>Pattern Syntax</strong>: Uses Java regular expression syntax (java.util.regex)</li>
 *   <li><strong>CDATA Usage</strong>: Use CDATA for patterns with special XML characters (&lt;, &gt;, &amp;)</li>
 *   <li><strong>Capture Groups</strong>: Use parentheses () for capturing matched text</li>
 *   <li><strong>Escaping</strong>: Backslash \ must be escaped in XML (or use CDATA)</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <regexp>} - Main regexp processor (parent element)</li>
 *   <li>{@code <regexp-source>} - Source text for pattern matching</li>
 *   <li>{@code <regexp-result>} - Access to matched groups</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see RegexpPlugin
 * @see RegexpSourcePlugin
 * @see RegexpResultPlugin
 */
@CorePlugin(
    elementName = "regexp-pattern",
    description = "Regexp pattern plugin that provides regexp pattern functionality",
    version = "2.1.0"
)
public class RegexpPatternPlugin extends AbstractPlugin {

    /**
     * Metadata for RegexpPatternPlugin.
     * 
     * <p>Detail plugin - requires regexp as parent. Defines the regular expression pattern for matching and extraction. Supports Java regex syntax with capture groups.</p>
     */
    public static class RegexpPatternPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() { return "regexp-pattern"; }
        @Override
        public String getName() { return "Regexp Pattern"; }
        @Override
        public String getVersion() { return "2.3.0"; }
        @Override
        public String getDescription() {
            return "Defines the regular expression pattern for matching and extraction. Must be used within regexp element. Supports Java regex syntax with capture groups (parentheses). Use CDATA for patterns with special XML characters.";
        }
        @Override
        public String getAuthor() { return "Web-Harvest Team"; }
        @Override
        public String getCategory() {
            CorePlugin annotation = RegexpPatternPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Extraction";
        }
        @Override
        public String[] getTags() {
            return new String[]{"core", "regexp", "pattern", "regex", "matching", "extraction"};
        }
        @Override
        public String[] getDependencies() { return new String[0]; }
        @Override
        public String getClassName() { return RegexpPatternPlugin.class.getName(); }
        @Override
        public boolean isCorePlugin() { return true; }
        @Override
        public String getElementName() { return "regexp-pattern"; }
        @Override
        public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new RegexpPatternPluginConfigurationSchema();
        }
        @Override
        public String getIcon() { return "fas fa-code-branch"; }
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
                new InputPort("pattern", "Regular expression pattern (from body content)", PortType.STRING, true)
            };
        }
        @Override
        public OutputPort[] getOutputPorts() { return new OutputPort[0]; }
        @Override
        public PortType getPortType() { return PortType.STRING; }
        @Override
        public String getTooltip() {
            return "Defines the regular expression pattern for matching and extraction. Supports Java regex syntax with capture groups.";
        }
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() { return new KeyboardShortcut[0]; }
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Regexp Pattern Plugin",
                "Defines the regular expression pattern for matching and extraction.",
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
     * Configuration schema for RegexpPatternPlugin.
     */
    public static class RegexpPatternPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the regexp-pattern element";
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
        return new RegexpPatternPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new RegexpPatternPluginMetadata().getConfigurationSchema();
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

















