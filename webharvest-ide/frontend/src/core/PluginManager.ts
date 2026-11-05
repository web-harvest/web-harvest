/**
 * PluginManager - Manages plugin data and palette
 */

import type { Plugin, PluginDocumentationSummary, PluginSemanticsModel } from '../types';
import { normalizePlugin } from '../shared/metadata/PluginNormalizer';

export class PluginManager {
    private plugins: Plugin[] = [];
    private loaded: boolean = false;
    private schemaVersion: string | null = null;
    private catalogSchema: Record<string, any> | null = null;
    private lastVersionCheck: number = 0;
    private versionCheckInterval: number = 60000; // Check every 60 seconds
    private refreshListeners: Set<() => void> = new Set();

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
            this.plugins = (data.plugins || []).map((plugin: any) => normalizePlugin(plugin));
            
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
     * Check if schema version has changed on the server
     */
    async checkSchemaVersion(): Promise<{ changed: boolean; newVersion: string | null }> {
        try {
            const response = await fetch('/api/plugins/metadata/version');
            const data = await response.json();
            const serverVersion = data.schemaVersion || data.version || null;
            
            if (serverVersion && serverVersion !== this.schemaVersion) {
                return { changed: true, newVersion: serverVersion };
            }
            
            return { changed: false, newVersion: this.schemaVersion };
        } catch (error) {
            console.warn('Failed to check schema version:', error);
            return { changed: false, newVersion: this.schemaVersion };
        }
    }

    /**
     * Refresh plugins from API (force reload)
     */
    async refresh(): Promise<Plugin[]> {
        this.loaded = false;
        this.plugins = [];
        this.schemaVersion = null;
        this.catalogSchema = null;
        
        const plugins = await this.loadPlugins();
        
        // Notify listeners
        this.refreshListeners.forEach(listener => {
            try {
                listener();
            } catch (error) {
                console.error('Error in refresh listener:', error);
            }
        });
        
        return plugins;
    }

    /**
     * Add a listener for refresh events
     */
    onRefresh(listener: () => void): () => void {
        this.refreshListeners.add(listener);
        return () => {
            this.refreshListeners.delete(listener);
        };
    }

    /**
     * Start automatic version checking (optional)
     */
    startVersionChecking(intervalMs: number = 60000): void {
        this.versionCheckInterval = intervalMs;
        this.checkVersionPeriodically();
    }

    /**
     * Stop automatic version checking
     */
    stopVersionChecking(): void {
        this.versionCheckInterval = 0;
    }

    private async checkVersionPeriodically(): Promise<void> {
        if (this.versionCheckInterval <= 0) {
            return;
        }

        const now = Date.now();
        if (now - this.lastVersionCheck < this.versionCheckInterval) {
            return;
        }

        this.lastVersionCheck = now;
        const { changed, newVersion } = await this.checkSchemaVersion();
        
        if (changed) {
            console.log(`🔄 Schema version changed from ${this.schemaVersion} to ${newVersion}, refreshing plugins...`);
            await this.refresh();
        }

        // Schedule next check
        if (this.versionCheckInterval > 0) {
            setTimeout(() => this.checkVersionPeriodically(), this.versionCheckInterval);
        }
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
