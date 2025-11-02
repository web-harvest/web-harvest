/**
 * PluginManager - TDD Tests
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { PluginManager } from '../PluginManager';

// Mock fetch
global.fetch = jest.fn(() =>
    Promise.resolve({
        json: () => Promise.resolve({
            plugins: [
                {
                    id: 'var',
                    name: 'Variable',
                    category: 'Variables',
                    icon: 'fa-box',
                    description: 'Define variable',
                    schema: {},
                    ui: { paletteGroup: 'Variables', icon: 'fa-box', tags: ['var', 'variables'] },
                    semantics: { role: 'BODY_EVALUATOR', canAppearAtRoot: true }
                },
                {
                    id: 'http',
                    name: 'HTTP',
                    category: 'Network',
                    icon: 'fa-globe',
                    description: 'HTTP request',
                    schema: {},
                    ui: { paletteGroup: 'Network', icon: 'fa-globe' },
                    semantics: { role: 'LEAF', canAppearAtRoot: true }
                },
                {
                    id: 'loop',
                    name: 'Loop',
                    category: 'Control',
                    icon: 'fa-repeat',
                    description: 'Loop',
                    schema: {},
                    ui: { paletteGroup: 'Control', icon: 'fa-repeat' },
                    semantics: { role: 'CONTEXT_CONTAINER', canAppearAtRoot: true }
                }
            ],
        }),
    })
) as jest.Mock;

describe('PluginManager', () => {
    let manager: PluginManager;

    beforeEach(() => {
        manager = new PluginManager();
        (fetch as jest.Mock).mockClear();
    });

    describe('loadPlugins', () => {
        it('should load plugins from API', async () => {
            const plugins = await manager.loadPlugins();

            expect(plugins).toHaveLength(3);
            expect(fetch).toHaveBeenCalledWith('/api/plugins/metadata');
        });

        it('should cache plugins after first load', async () => {
            await manager.loadPlugins();
            await manager.loadPlugins();

            expect(fetch).toHaveBeenCalledTimes(1);
        });
    });

    describe('findPlugin', () => {
        it('should find plugin by ID', async () => {
            await manager.loadPlugins();

            const plugin = manager.findPlugin('var');
            expect(plugin?.name).toBe('Variable');
        });
    });

    describe('getCategories', () => {
        it('should return unique categories', async () => {
            await manager.loadPlugins();

            const categories = manager.getCategories();
            expect(categories).toContain('Variables');
            expect(categories).toContain('Network');
            expect(categories).toContain('Control');
        });
    });
});





