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

package org.webharvest.plugin;

/**
 * Visual plugin metadata interface for visual editor integration.
 * 
 * <p>Extends {@link PluginMetadata} with visual-specific information needed for
 * canvas-based visual editor, following Apple HIG guidelines and Azure Data Factory
 * / Apache NiFi patterns.</p>
 * 
 * <p>This interface provides:</p>
 * <ul>
 *   <li>Visual appearance: icons, colors, groups</li>
 *   <li>Canvas dimensions: default width/height</li>
 *   <li>Data flow: input/output ports (NiFi-style)</li>
 *   <li>Parent-child relationships: master/detail plugin patterns</li>
 *   <li>Accessibility: Apple HIG compliance</li>
 *   <li>Keyboard shortcuts: Apple HIG keyboard navigation</li>
 *   <li>Linked services: Azure Data Factory-style integration</li>
 * </ul>
 * 
 * <p>This integrates with existing {@code PluginSemantics} for parent-child
 * relationships, maintaining consistency with the semantic metadata system.</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public interface VisualPluginMetadata extends PluginMetadata {
    
    /**
     * Gets the icon identifier for this plugin.
     * 
     * <p>Can be FontAwesome class (e.g., "fas fa-globe") or SVG path.</p>
     * 
     * @return icon identifier
     */
    String getIcon();
    
    /**
     * Gets the color for this plugin category.
     * 
     * <p>Color should be in hex format (e.g., "#3B82F6").</p>
     * 
     * @return hex color code
     */
    String getColor();
    
    /**
     * Gets the visual group for palette organization.
     * 
     * <p>Examples: "HTTP & Network", "XML Processing", "Control Flow", etc.</p>
     * 
     * @return group name
     */
    String getGroup();
    
    /**
     * Gets the default width for the plugin node in canvas.
     * 
     * @return default width in pixels
     */
    int getDefaultWidth();
    
    /**
     * Gets the default height for the plugin node in canvas.
     * 
     * @return default height in pixels
     */
    int getDefaultHeight();
    
    /**
     * Gets input ports for this plugin (NiFi-style).
     * 
     * <p>Input ports represent data inputs to the plugin. Can be empty array
     * for source plugins (e.g., {@code <http>}, {@code <file>} with read action).</p>
     * 
     * @return array of input ports
     */
    InputPort[] getInputPorts();
    
    /**
     * Gets output ports for this plugin (NiFi-style).
     * 
     * <p>Output ports represent data outputs from the plugin. Should have at least
     * one output port.</p>
     * 
     * @return array of output ports
     */
    OutputPort[] getOutputPorts();
    
    /**
     * Gets the primary port type for this plugin.
     * 
     * <p>Indicates the primary data type that flows through this plugin.</p>
     * 
     * @return primary port type
     */
    PortType getPortType();
    
    /**
     * Gets the extended tooltip for UI display.
     * 
     * <p>More detailed than basic description, suitable for tooltips and help text.</p>
     * 
     * @return tooltip text
     */
    String getTooltip();
    
    /**
     * Gets keyboard shortcuts for this plugin (Apple HIG).
     * 
     * <p>Defines keyboard shortcuts for plugin actions, following Apple HIG
     * guidelines for keyboard navigation.</p>
     * 
     * @return array of keyboard shortcuts (can be empty)
     */
    KeyboardShortcut[] getKeyboardShortcuts();
    
    /**
     * Gets accessibility metadata (Apple HIG).
     * 
     * <p>Provides accessibility information for screen readers and keyboard
     * navigation, following Apple HIG and WCAG standards.</p>
     * 
     * @return accessibility metadata
     */
    AccessibilityMetadata getAccessibility();
    
    /**
     * Gets linked services metadata (Azure Data Factory style).
     * 
     * <p>Defines external services that can be linked to this plugin, similar
     * to Azure Data Factory linked services concept.</p>
     * 
     * @return array of linked services (can be empty)
     */
    LinkedServiceMetadata[] getLinkedServices();
    
    /**
     * Gets the parent-child relationship for this plugin.
     * 
     * <p>For detail plugins (e.g., {@code <http-header>}), returns the required
     * parent relationship. For master plugins (e.g., {@code <http>}), returns null.</p>
     * 
     * <p>This integrates with existing {@code PluginSemantics} for consistency.
     * If a plugin has {@code PluginSemantics} with {@code requiredParentElements},
     * this should reflect that relationship.</p>
     * 
     * @return parent-child relationship, or null if this is a standalone plugin
     */
    ParentChildRelationship getParentRelationship();
    
    /**
     * Gets allowed child plugins for this plugin.
     * 
     * <p>For master plugins (e.g., {@code <http>}), returns array of allowed
     * child element names (e.g., ["http-header", "http-param"]). For detail plugins
     * or standalone plugins, returns empty array.</p>
     * 
     * <p>This integrates with existing {@code PluginSemantics} for consistency.
     * If a plugin has {@code PluginSemantics} with {@code allowedChildElements},
     * this should reflect those children.</p>
     * 
     * @return array of allowed child element names, or empty array if none
     */
    String[] getAllowedChildPlugins();
    
    /**
     * Checks if this is a master plugin (allows child plugins).
     * 
     * @return true if master plugin, false otherwise
     */
    default boolean isMasterPlugin() {
        String[] children = getAllowedChildPlugins();
        return children != null && children.length > 0;
    }
    
    /**
     * Checks if this is a detail plugin (requires parent).
     * 
     * @return true if detail plugin, false otherwise
     */
    default boolean isDetailPlugin() {
        ParentChildRelationship parent = getParentRelationship();
        return parent != null && parent.isRequired();
    }
}

