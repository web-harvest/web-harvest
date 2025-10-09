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

import org.webharvest.definition.IElementDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that converts PluginConfigurationTree to IElementDef interface.
 * This is for backward compatibility during migration to ULTRA-LEAN architecture (v2.2.x).
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 * @deprecated Use PluginConfigurationTree directly. This adapter is temporary for migration.
 */
@Deprecated
public class PluginConfigurationTreeElementDefAdapter implements IElementDef {
    
    private final PluginConfigurationTree tree;
    
    public PluginConfigurationTreeElementDefAdapter(PluginConfigurationTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("PluginConfigurationTree cannot be null");
        }
        this.tree = tree;
    }
    
    @Override
    public String getShortElementName() {
        return tree.getPluginElementName();
    }
    
    public String getElementName() {
        return tree.getPluginElementName();
    }
    
    @Override
    public IElementDef[] getOperationDefs() {
        if (!tree.hasChildren()) {
            return new IElementDef[0];
        }
        
        List<IElementDef> childDefs = new ArrayList<>();
        for (PluginConfigurationTree child : tree.getChildren()) {
            childDefs.add(new PluginConfigurationTreeElementDefAdapter(child));
        }
        
        return childDefs.toArray(new IElementDef[0]);
    }
    
    @Override
    public List<IElementDef> getElementDefs() {
        if (!tree.hasChildren()) {
            return new ArrayList<>();
        }
        
        List<IElementDef> childDefs = new ArrayList<>();
        for (PluginConfigurationTree child : tree.getChildren()) {
            childDefs.add(new PluginConfigurationTreeElementDefAdapter(child));
        }
        
        return childDefs;
    }
    
    @Override
    public boolean hasOperations() {
        return tree.hasChildren();
    }
    
    public String getAttribute(String name) {
        PluginConfiguration config = tree.getConfiguration();
        return config != null ? config.getProperty(name) : null;
    }
    
    @Override
    public String getId() {
        PluginConfiguration config = tree.getConfiguration();
        return config != null ? config.getProperty("id", "") : "";
    }
    
    @Override
    public int getLineNumber() {
        return 0; // Not tracked in new architecture
    }
    
    @Override
    public int getColumnNumber() {
        return 0; // Not tracked in new architecture
    }
    
    @Override
    public String getNamespaceURI() {
        PluginConfiguration config = tree.getConfiguration();
        return config != null ? config.getNamespace() : "";
    }
    
    @Override
    public XmlNode getNode() {
        // Not available in new architecture - return null
        // This method is deprecated anyway
        return null;
    }
    
    public void setProperty(String name, String value) {
        // Not supported in new architecture - PluginConfiguration is read-only after parsing
        // This method exists only for IElementDef interface compatibility
    }
    
    @Override
    public org.webharvest.runtime.processors.Processor createPlugin() {
        // v2.2.x ULTRA-LEAN: Create plugin using new architecture (PluginFactory)
        // Then wrap in PluginProcessorAdapter for backward compatibility
        
        try {
            String elementName = tree.getPluginElementName();
            Class<?> pluginClass = tree.getPluginClass();
            
            if (pluginClass == null) {
                throw new IllegalStateException("No plugin class found for element: " + elementName);
            }
            
            // Create plugin instance using Guice
            org.webharvest.plugin.Plugin plugin = (org.webharvest.plugin.Plugin) 
                org.webharvest.ioc.InjectorHelper.getInjector().getInstance(pluginClass);
            
            // Initialize plugin with configuration
            plugin.initialize(tree.getConfiguration());
            
            // Wrap plugin in adapter to make it work as Processor
            org.webharvest.runtime.processors.PluginProcessorAdapter adapter = 
                new org.webharvest.runtime.processors.PluginProcessorAdapter(plugin);
            
            // Set this adapter as elementDef (for backward compatibility)
            adapter.setElementDef(this);
            
            return adapter;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create plugin for element: " + tree.getPluginElementName(), e);
        }
    }
    
    /**
     * Gets the underlying PluginConfigurationTree.
     * This is useful for code that needs to access the new architecture.
     * 
     * @return the plugin configuration tree
     */
    public PluginConfigurationTree getTree() {
        return tree;
    }
}
