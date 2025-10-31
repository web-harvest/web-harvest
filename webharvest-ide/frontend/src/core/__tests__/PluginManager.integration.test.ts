/**
 * Integration tests for PluginManager with real API
 */

import { PluginManager } from '../PluginManager';
import fetch from 'node-fetch';

(global as any).fetch = fetch;

describe('PluginManager - API Integration', () => {
    const API_BASE = 'http://localhost:8080';
    let manager: PluginManager;

    beforeEach(() => {
        manager = new PluginManager();
    });

    describe('loadPlugins() with real API', () => {
        it('should load plugins from backend', async () => {
            await manager.loadPlugins();

            const plugins = manager.getAllPlugins();
            expect(plugins.length).toBeGreaterThan(0);
            expect(plugins.length).toBeGreaterThanOrEqual(29); // We know we have 29 core plugins
        }, 10000);

        it('should find specific plugins', async () => {
            await manager.loadPlugins();

            const httpPlugin = manager.findPluginById('http');
            expect(httpPlugin).toBeDefined();
            expect(httpPlugin?.name).toBe('Http');
            expect(httpPlugin?.category).toBeTruthy();

            const xpathPlugin = manager.findPluginById('xpath');
            expect(xpathPlugin).toBeDefined();
            expect(xpathPlugin?.name).toBe('XPath');
        }, 10000);

        it('should categorize plugins correctly', async () => {
            await manager.loadPlugins();

            const plugins = manager.getAllPlugins();
            const categories = new Set(plugins.map(p => p.category));

            // Should have multiple categories
            expect(categories.size).toBeGreaterThan(1);

            // Check common categories exist
            const categoryList = Array.from(categories);
            expect(categoryList.some(c => c.includes('HTTP') || c.includes('Web'))).toBe(true);
        }, 10000);

        it('should have Font Awesome icons', async () => {
            await manager.loadPlugins();

            const httpPlugin = manager.findPluginById('http');
            expect(httpPlugin?.icon).toMatch(/^fa-/); // Should start with "fa-"
        }, 10000);
    });

    describe('Plugin schema validation', () => {
        it('should have schema for each plugin', async () => {
            await manager.loadPlugins();

            const plugins = manager.getAllPlugins();

            plugins.forEach(plugin => {
                expect(plugin).toHaveProperty('schema');
                expect(plugin.schema).toHaveProperty('attributes');
                expect(Array.isArray(plugin.schema.attributes)).toBe(true);
            });
        }, 10000);

        it('should have correct http plugin schema', async () => {
            await manager.loadPlugins();

            const httpPlugin = manager.findPluginById('http');
            expect(httpPlugin).toBeDefined();

            const schema = httpPlugin!.schema;
            expect(schema.requiredAttributes).toContain('url');

            const urlAttr = schema.attributes.find(a => a.name === 'url');
            expect(urlAttr).toBeDefined();
            expect(urlAttr?.required).toBe(true);
        }, 10000);
    });
});





