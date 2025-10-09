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

package org.webharvest.definition.v3;

import org.webharvest.plugin.PluginConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration tree for v3.0 architecture - replaces IElementDef hierarchy.
 * 
 * <h3>Architecture Change - v3.0</h3>
 * <p>
 * In v2.x, XML parsing creates a tree of *Def objects (HttpDef, XPathDef, etc.)
 * which then create Plugins. This requires maintaining 30+ *Def classes.
 * </p>
 * <p>
 * In v3.0, we parse XML directly into PluginConfiguration objects, eliminating
 * the intermediate *Def layer entirely.
 * </p>
 * 
 * <h3>Benefits</h3>
 * <ul>
 *   <li>Simpler architecture - one less layer</li>
 *   <li>No need to maintain 30+ *Def classes</li>
 *   <li>Better performance - fewer object creations</li>
 *   <li>Easier to add new plugins</li>
 * </ul>
 * 
 * <h3>Structure</h3>
 * <pre>
 * PluginConfigurationTree
 *   ├─ rootConfiguration (PluginConfiguration for &lt;config&gt;)
 *   └─ children (List&lt;PluginConfigurationTree&gt;)
 *        ├─ http configuration
 *        │    └─ xpath configuration (child of http)
 *        └─ file configuration
 * </pre>
 * 
 * <h3>Example</h3>
 * <pre>{@code
 * // Old way (v2.x):
 * XmlNode → HttpDef → HttpPlugin
 * 
 * // New way (v3.0):
 * XmlElement → PluginConfiguration → HttpPlugin
 * }</pre>
 * 
 * @author Web-Harvest Team
 * @since 3.0.0
 * @see org.webharvest.definition.v3.StreamingXmlParser
 */
public class PluginConfigurationTree {

    private final PluginConfiguration configuration;
    private final List<PluginConfigurationTree> children;
    private final String pluginElementName;
    private final Class<?> pluginClass;
    private int lineNumber;
    private int columnNumber;

    /**
     * Creates a new configuration tree node.
     * 
     * @param configuration the plugin configuration
     * @param pluginElementName the XML element name (e.g., "http", "xpath")
     * @param pluginClass the plugin class to instantiate
     */
    public PluginConfigurationTree(PluginConfiguration configuration, 
                                   String pluginElementName,
                                   Class<?> pluginClass) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (pluginElementName == null || pluginElementName.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin element name cannot be null or empty");
        }
        
        this.configuration = configuration;
        this.pluginElementName = pluginElementName;
        this.pluginClass = pluginClass;
        this.children = new ArrayList<>();
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    /**
     * Adds a child configuration tree.
     * 
     * @param child the child tree to add
     */
    public void addChild(PluginConfigurationTree child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    /**
     * Gets the plugin configuration.
     * 
     * @return the configuration
     */
    public PluginConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Gets all child configuration trees.
     * 
     * @return unmodifiable list of children
     */
    public List<PluginConfigurationTree> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Gets the plugin element name (e.g., "http", "xpath").
     * 
     * @return the element name
     */
    public String getPluginElementName() {
        return pluginElementName;
    }

    /**
     * Gets the plugin class.
     * 
     * @return the plugin class
     */
    public Class<?> getPluginClass() {
        return pluginClass;
    }

    /**
     * Sets the location in XML source.
     * 
     * @param lineNumber line number in XML
     * @param columnNumber column number in XML
     */
    public void setLocation(int lineNumber, int columnNumber) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Gets the line number in XML source.
     * 
     * @return line number, or -1 if not set
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Gets the column number in XML source.
     * 
     * @return column number, or -1 if not set
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Checks if this node has any children.
     * 
     * @return true if has children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Gets the number of children.
     * 
     * @return children count
     */
    public int getChildCount() {
        return children.size();
    }

    @Override
    public String toString() {
        return String.format("PluginConfigurationTree[element=%s, plugin=%s, children=%d]",
            pluginElementName,
            pluginClass != null ? pluginClass.getSimpleName() : "null",
            children.size());
    }
}


