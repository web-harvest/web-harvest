/**
 * PluginManager - Manages plugin data and palette
 */

import type { Plugin } from '../types';

export class PluginManager {
    private plugins: Plugin[] = [];
    private loaded: boolean = false;

    /**
     * Load plugins from API
     */
    async loadPlugins(): Promise<Plugin[]> {
        if (this.loaded) {
            return this.plugins;
        }

        try {
            const response = await fetch('/api/plugins');
            const data = await response.json();
            this.plugins = data.plugins || [];
            this.loaded = true;

            console.log(`✅ Loaded ${this.plugins.length} plugins`);
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
     * Find plugin by ID
     */
    findPlugin(id: string): Plugin | null {
        return this.plugins.find(p => p.id === id) || null;
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
        return this.plugins.filter(p => p.category === category);
    }

    /**
     * Get all categories
     */
    getCategories(): string[] {
        const categories = new Set(this.plugins.map(p => p.category || 'Other'));
        return Array.from(categories).sort();
    }
}


