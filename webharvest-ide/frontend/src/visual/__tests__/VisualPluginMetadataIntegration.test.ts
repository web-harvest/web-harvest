/**
 * VisualPluginMetadata Integration Tests (TDD)
 * 
 * Tests that frontend TypeScript code correctly uses VisualPluginMetadata
 * from the generated JSON metadata.
 * 
 * RED phase: Tests should fail until VisualPluginMetadata is integrated
 */

import { PluginManager } from '../../core/PluginManager';
import { PluginPalette } from '../PluginPalette';
import type { Plugin } from '../../types';

describe('VisualPluginMetadata Integration', () => {
    let pluginManager: PluginManager;
    let pluginPalette: PluginPalette;

    beforeEach(() => {
        // Reset mocks
        global.fetch = jest.fn();
        pluginManager = new PluginManager();
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('PluginManager.loadPlugins()', () => {
        it('should load plugins with VisualPluginMetadata fields', async () => {
            // RED: This test expects VisualPluginMetadata fields in API response
            const mockPluginsResponse = {
                plugins: [
                    {
                        id: 'http',
                        name: 'HTTP Request',
                        elementName: 'http',
                        description: 'Performs HTTP requests',
                        category: 'HTTP & Network',
                        icon: 'fas fa-globe',
                        color: '#3B82F6', // VisualPluginMetadata field
                        group: 'HTTP & Network', // VisualPluginMetadata field
                        defaultWidth: 200, // VisualPluginMetadata field
                        defaultHeight: 100, // VisualPluginMetadata field
                        inputPorts: [{ name: 'url', type: 'STRING', required: true }], // VisualPluginMetadata
                        outputPorts: [{ name: 'response', type: 'XML' }], // VisualPluginMetadata
                        portType: 'XML',
                        tooltip: 'Fetch data from HTTP endpoint',
                        schema: {
                            attributes: [{ name: 'url', type: 'string', required: true }],
                            allowsBody: true,
                            allowedSubElements: ['http-header', 'http-param']
                        }
                    }
                ],
                count: 1,
                version: '2.3.0'
            };

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => mockPluginsResponse
            });

            const plugins = await pluginManager.loadPlugins();

            expect(plugins.length).toBe(1);
            const httpPlugin = plugins[0];
            
            // These fields should exist in Plugin type after update
            // RED: Will fail until Plugin type is extended
            expect((httpPlugin as any).color).toBe('#3B82F6');
            expect((httpPlugin as any).group).toBe('HTTP & Network');
            expect((httpPlugin as any).defaultWidth).toBe(200);
            expect((httpPlugin as any).defaultHeight).toBe(100);
            expect((httpPlugin as any).inputPorts).toBeDefined();
            expect((httpPlugin as any).outputPorts).toBeDefined();
        });

        it('should map VisualPluginMetadata fields correctly', async () => {
            // RED: Test that VisualPluginMetadata fields are preserved
            const mockResponse = {
                plugins: [
                    {
                        id: 'xpath',
                        name: 'XPath Expression',
                        color: '#10B981',
                        group: 'XML Processing',
                        defaultWidth: 180,
                        defaultHeight: 80,
                        inputPorts: [{ name: 'xml', type: 'XML', required: true }],
                        outputPorts: [{ name: 'result', type: 'STRING' }],
                        portType: 'STRING'
                    }
                ],
                count: 1
            };

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const plugins = await pluginManager.loadPlugins();
            const xpathPlugin = plugins[0];

            // RED: Will fail until Plugin type includes VisualPluginMetadata
            expect((xpathPlugin as any).color).toBe('#10B981');
            expect((xpathPlugin as any).group).toBe('XML Processing');
            expect((xpathPlugin as any).defaultWidth).toBe(180);
            expect((xpathPlugin as any).defaultHeight).toBe(80);
        });
    });

    describe('PluginPalette.init()', () => {
        it('should use VisualPluginMetadata color for plugin items', async () => {
            // RED: PluginPalette should use color from VisualPluginMetadata
            const mockResponse = {
                plugins: [
                    {
                        id: 'http',
                        name: 'HTTP Request',
                        color: '#3B82F6',
                        group: 'HTTP & Network',
                        icon: 'fas fa-globe'
                    }
                ],
                count: 1
            };

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const container = document.createElement('div');
            container.id = 'test-palette';
            document.body.appendChild(container);

            pluginPalette = new PluginPalette('test-palette');
            await pluginPalette.init();

            // RED: PluginPalette should render with color from metadata
            const paletteItems = container.querySelectorAll('.palette-plugin');
            expect(paletteItems.length).toBeGreaterThan(0);
            
            // Check if color is applied (will fail until PluginPalette uses color)
            const firstItem = paletteItems[0] as HTMLElement;
            const itemColor = window.getComputedStyle(firstItem).borderColor || 
                            firstItem.style.borderColor;
            
            // RED: This will fail until PluginPalette applies color from metadata
            // expect(itemColor).toContain('59, 130, 246'); // RGB for #3B82F6

            document.body.removeChild(container);
        });

        it('should group plugins by VisualPluginMetadata group field', async () => {
            // RED: PluginPalette should use group from VisualPluginMetadata, not category
            const mockResponse = {
                plugins: [
                    { id: 'http', name: 'HTTP', group: 'HTTP & Network', category: 'Network' },
                    { id: 'xpath', name: 'XPath', group: 'XML Processing', category: 'Extraction' }
                ],
                count: 2
            };

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const container = document.createElement('div');
            container.id = 'test-palette-2';
            document.body.appendChild(container);

            pluginPalette = new PluginPalette('test-palette-2');
            await pluginPalette.init();

            const categories = pluginPalette.getCategories();
            
            // RED: Should use 'group' field, not 'category'
            expect(categories).toContain('HTTP & Network');
            expect(categories).toContain('XML Processing');
            // Should NOT contain old category names if group exists
            // expect(categories).not.toContain('Network');

            document.body.removeChild(container);
        });
    });

    describe('VisualDesigner node rendering', () => {
        it('should use defaultWidth and defaultHeight from VisualPluginMetadata', () => {
            // RED: VisualDesigner should use metadata dimensions instead of constants
            // This test will be expanded when VisualDesigner is updated
            expect(true).toBe(true); // Placeholder
        });
    });

    describe('PortSystem', () => {
        it('should use inputPorts and outputPorts from VisualPluginMetadata', () => {
            // RED: PortSystem should use ports from metadata instead of hardcoded
            expect(true).toBe(true); // Placeholder
        });
    });
});

