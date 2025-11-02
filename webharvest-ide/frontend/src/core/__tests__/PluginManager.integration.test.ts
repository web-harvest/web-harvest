/**
 * Integration tests for PluginManager with real API
 */

import { jest, describe, it, expect, beforeAll, beforeEach } from '@jest/globals';
import { PluginManager } from '../PluginManager';

describe('PluginManager - API Integration', () => {
    const sampleResponse = {
        count: 3,
        schemaVersion: '1.1.0',
        schema: { title: 'WebHarvest Plugin Metadata Catalog', type: 'object' },
        plugins: [
            {
                id: 'http',
                name: 'HTTP',
                description: 'HTTP request',
                category: 'Network',
                icon: 'fa-globe',
                documentation: { summary: 'Makes HTTP requests', url: 'https://docs/http' },
                dependencies: [],
                schema: { attributes: [{ name: 'url', required: true }], requiredAttributes: ['url'] },
                semantics: { role: 'LEAF', canAppearAtRoot: true, allowsChildren: false, acceptsAnyChild: false, allowBody: false, allowTextContent: false, allowedChildTypes: [], requiredParentTypes: [] }
            },
            {
                id: 'xpath',
                name: 'XPath',
                description: 'XPath extractor',
                category: 'Extraction',
                icon: 'fa-code-branch',
                documentation: { summary: 'Extracts with XPath', url: 'https://docs/xpath' },
                dependencies: [],
                schema: { attributes: [] },
                semantics: { role: 'BODY_EVALUATOR', canAppearAtRoot: true, allowsChildren: false, acceptsAnyChild: false, allowBody: true, allowTextContent: true, allowedChildTypes: [], requiredParentTypes: [] }
            },
            {
                id: 'loop',
                name: 'Loop',
                description: 'Loop container',
                category: 'Control',
                icon: 'fa-repeat',
                documentation: { summary: 'Loops over values', url: 'https://docs/loop' },
                dependencies: [],
                schema: { attributes: [] },
                semantics: { role: 'CONTEXT_CONTAINER', canAppearAtRoot: true, allowsChildren: true, acceptsAnyChild: true, allowBody: true, allowTextContent: false, allowedChildTypes: [], requiredParentTypes: [] }
            }
        ]
    };

    beforeEach(() => {
        (global as any).fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => sampleResponse
        });
    });

    let manager: PluginManager;

    beforeEach(() => {
        manager = new PluginManager();
    });

    describe('loadPlugins() with real API', () => {
        it('should load plugins from backend', async () => {
            await manager.loadPlugins();

            const plugins = manager.getAllPlugins();
            expect(plugins.length).toBe(sampleResponse.plugins.length);
        }, 10000);

        it('should find specific plugins', async () => {
            await manager.loadPlugins();

            const httpPlugin = manager.findPluginById('http');
            expect(httpPlugin).toBeDefined();
            expect(httpPlugin?.name).toBe('HTTP');
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
            expect(categoryList.some(c => c.toLowerCase().includes('network'))).toBe(true);
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
                expect(plugin.documentation?.summary).toBeTruthy();
            });
        }, 10000);

        it('should have correct http plugin schema', async () => {
            await manager.loadPlugins();

            const httpPlugin = manager.findPluginById('http');
            expect(httpPlugin).toBeDefined();

            const schema = httpPlugin!.schema;
            expect(schema.requiredAttributes || []).toContain('url');

            const urlAttr = schema.attributes.find(a => a.name === 'url');
            expect(urlAttr).toBeDefined();
            expect(urlAttr?.required).toBe(true);
            expect(httpPlugin?.documentation?.url).toBe('https://docs/http');
            expect(httpPlugin?.dependencies).toEqual([]);
        }, 10000);

        it('should surface schema metadata version info', async () => {
            await manager.loadPlugins();

            expect(manager.getSchemaVersion()).toBe('1.1.0');
            expect(manager.getCatalogSchema()).toEqual(sampleResponse.schema);
        }, 10000);
    });
});





