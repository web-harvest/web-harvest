/**
 * PluginManager - Manages plugin data and palette
 */

import type { Plugin, PluginDocumentationSummary, PluginSemanticsModel } from '../types';

export class PluginManager {
    private plugins: Plugin[] = [];
    private loaded: boolean = false;
    private schemaVersion: string | null = null;
    private catalogSchema: Record<string, any> | null = null;

    /**
     * Load plugins from API
     */
    async loadPlugins(): Promise<Plugin[]> {
        if (this.loaded) {
            return this.plugins;
        }

        try {
            const response = await fetch('/api/plugins/metadata');
            const data = await response.json();
            this.schemaVersion = data.schemaVersion || data.version || null;
            this.catalogSchema = data.schema || null;
            this.plugins = (data.plugins || []).map((plugin: any) => {
                const semanticsPayload: any = plugin.semantics ? { ...plugin.semantics } : undefined;
                if (semanticsPayload) {
                    if (!Array.isArray(semanticsPayload.roles)) {
                        const legacyRole = semanticsPayload.role;
                        semanticsPayload.roles = legacyRole ? [legacyRole] : [];
                    }
                }

                return {
                    id: plugin.id,
                    name: plugin.name,
                    description: plugin.description || '',
                    category: plugin.category || 'General',
                    icon: plugin.icon || plugin.ui?.icon || 'fa-puzzle-piece',
                    paletteGroup: plugin.paletteGroup || plugin.ui?.paletteGroup || plugin.category || 'General',
                    tags: plugin.tags || plugin.ui?.tags || [],
                    schema: plugin.schema,
                    semantics: semanticsPayload,
                    helpUrl: plugin.helpUrl || plugin.documentation?.url,
                    // Full PluginDocumentation mapping
                    documentation: plugin.documentation ? {
                        summary: plugin.documentation.summary || plugin.description || '',
                        url: plugin.documentation.url || plugin.helpUrl,
                        examples: plugin.documentation.examples || []
                    } : {
                        summary: plugin.description || '',
                        url: plugin.helpUrl,
                        examples: []
                    },
                    dependencies: plugin.dependencies || [],
                    // VisualPluginMetadata fields
                    color: plugin.color,
                    group: plugin.group || plugin.paletteGroup || plugin.category || 'General',
                    defaultWidth: plugin.defaultWidth,
                    defaultHeight: plugin.defaultHeight,
                    inputPorts: plugin.inputPorts,
                    outputPorts: plugin.outputPorts,
                    portType: plugin.portType,
                    tooltip: plugin.tooltip,
                    keyboardShortcuts: plugin.keyboardShortcuts,
                    accessibility: plugin.accessibility,
                    linkedServices: plugin.linkedServices,
                    parentRelationship: plugin.parentRelationship,
                    allowedChildPlugins: plugin.allowedChildPlugins
                };
            });
            
            this.loaded = true;
            this.loaded = true;

            console.log(`✅ Loaded ${this.plugins.length} plugins (schema v${this.schemaVersion || 'unknown'})`);
            return this.plugins;
        } catch (error) {
            console.error('❌ Failed to load plugins:', error);
            throw error;
        }
    }

    /**
     * Get all plugins
     */
    getPlugins(): Plugin[] {
        return [...this.plugins];
    }

    /**
     * Backwards compatible alias for getPlugins()
     */
    getAllPlugins(): Plugin[] {
        return this.getPlugins();
    }

    getSchemaVersion(): string | null {
        return this.schemaVersion;
    }

    getCatalogSchema(): Record<string, any> | null {
        return this.catalogSchema ? { ...this.catalogSchema } : null;
    }

    /**
     * Find plugin by ID
     */
    findPlugin(id: string): Plugin | null {
        return this.plugins.find(p => p.id === id) || null;
    }

    /**
     * Alias for clarity in integration tests
     */
    findPluginById(id: string): Plugin | null {
        return this.findPlugin(id);
    }

    /**
     * Search plugins (by name, description, id, and tags)
     */
    searchPlugins(query: string): Plugin[] {
        if (!query) return this.plugins;

        const lowerQuery = query.toLowerCase();
        return this.plugins.filter(p => {
            // Search in name, description, id
            const matchesBasic = 
                p.name.toLowerCase().includes(lowerQuery) ||
                p.description.toLowerCase().includes(lowerQuery) ||
                p.id.toLowerCase().includes(lowerQuery);
            
            // Search in tags
            const matchesTags = p.tags && p.tags.some(tag => 
                tag.toLowerCase().includes(lowerQuery)
            );
            
            return matchesBasic || matchesTags;
        });
    }

    /**
     * Get plugins by category
     */
    getPluginsByCategory(category: string): Plugin[] {
        return this.plugins.filter(p => (p.paletteGroup || p.category) === category);
    }

    /**
     * Get all categories
     */
    getCategories(): string[] {
        const categories = new Set(this.plugins.map(p => (p.paletteGroup || p.category || 'Other')));
        return Array.from(categories).sort();
    }

    /**
     * Get documentation for a plugin
     */
    getDocumentation(pluginId: string): PluginDocumentationSummary | null {
        const plugin = this.findPlugin(pluginId);
        return plugin?.documentation || null;
    }

    /**
     * Get UI hints for a plugin
     */
    getUIHints(pluginId: string): { paletteGroup?: string; icon?: string; tags?: string[] } | null {
        const plugin = this.findPlugin(pluginId);
        if (!plugin) return null;
        
        return {
            paletteGroup: plugin.paletteGroup,
            icon: plugin.icon,
            tags: plugin.tags
        };
    }

    /**
     * Get semantics for a plugin
     */
    getSemantics(pluginId: string): PluginSemanticsModel | null {
        const plugin = this.findPlugin(pluginId);
        return plugin?.semantics || null;
    }

    /**
     * Get all plugins that can appear at root level
     */
    getRootLevelPlugins(): Plugin[] {
        return this.plugins.filter(p => {
            if (p.semantics?.canAppearAtRoot === false) return false;
            if (p.schema?.canAppearAtRoot === false) return false;
            if (p.semantics?.requiredParentTypes && p.semantics.requiredParentTypes.length > 0) return false;
            if (p.schema?.requiredParents && p.schema.requiredParents.length > 0) return false;
            return true;
        });
    }

    /**
     * Get dependent plugins (require a parent)
     */
    getDependentPlugins(): Plugin[] {
        return this.plugins.filter(p => {
            const requiredParents = p.semantics?.requiredParentTypes || p.schema?.requiredParents || [];
            return requiredParents.length > 0;
        });
    }

    /**
     * Get master plugins (allow specific children)
     */
    getMasterPlugins(): Plugin[] {
        return this.plugins.filter(p => {
            const allowedChildren = p.semantics?.allowedChildTypes || p.schema?.allowedSubElements || [];
            const hasAllowedChildren = allowedChildren.length > 0;
            const hasAllowedChildPlugins = p.allowedChildPlugins && p.allowedChildPlugins.length > 0;
            return hasAllowedChildren || hasAllowedChildPlugins;
        });
    }
}





