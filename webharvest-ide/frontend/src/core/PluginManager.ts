/**
 * PluginManager - Manages plugin data and palette
 */

import type { Plugin } from '../types';

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
            this.plugins = (data.plugins || []).map((plugin: any) => ({
                id: plugin.id,
                name: plugin.name,
                description: plugin.description || '',
                category: plugin.category || 'General',
                icon: plugin.icon || plugin.ui?.icon || 'fa-puzzle-piece',
                paletteGroup: plugin.paletteGroup || plugin.ui?.paletteGroup || plugin.category || 'General',
                tags: plugin.tags || plugin.ui?.tags || [],
                schema: plugin.schema,
                semantics: plugin.semantics,
                helpUrl: plugin.helpUrl || plugin.documentation?.url,
                documentation: plugin.documentation,
                dependencies: plugin.dependencies || []
            }));
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
     * Search plugins
     */
    searchPlugins(query: string): Plugin[] {
        if (!query) return this.plugins;

        const lowerQuery = query.toLowerCase();
        return this.plugins.filter(p =>
            p.name.toLowerCase().includes(lowerQuery) ||
            p.description.toLowerCase().includes(lowerQuery) ||
            p.id.toLowerCase().includes(lowerQuery)
        );
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
}





