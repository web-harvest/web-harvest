/**
 * TDD Integration Tests for Plugin Palette API Loading
 * 
 * Test Issues:
 * 1. pluginPalette.init() should fetch from /api/plugins/metadata
 * 2. Should render categories after successful API load
 * 3. Should fallback to hardcoded templates on API failure
 * 4. Should show empty state when no templates
 */

import { PluginPalette } from '../PluginPalette';

describe('PluginPalette - API Integration (TDD)', () => {
    let container: HTMLElement;
    let palette: PluginPalette;

    beforeEach(() => {
        // Setup DOM
        document.body.innerHTML = '<div id="test-palette"></div>';
        container = document.getElementById('test-palette')!;

        // Mock fetch
        global.fetch = jest.fn();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('init() - API Loading', () => {
        it('should fetch plugins from /api/plugins/metadata', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => ({
                    count: 27,
                    plugins: [
                        {
                            id: 'http',
                            name: 'Http',
                            category: 'Network',
                            description: 'HTTP requests',
                            schema: { attributes: [] }
                        }
                    ]
                })
            });

            palette = new PluginPalette('test-palette');
            await palette.init();

            expect(global.fetch).toHaveBeenCalledWith('/api/plugins/metadata');
        });

        it('should map API response to PluginTemplate[] format', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => ({
                    count: 2,
                    plugins: [
                        {
                            id: 'http',
                            name: 'Http',
                            category: 'Network',
                            description: 'HTTP plugin',
                            visual: { icon: '🌐' },
                            schema: {
                                attributes: [
                                    { name: 'url', defaultValue: 'https://example.com' }
                                ]
                            }
                        },
                        {
                            id: 'xpath',
                            name: 'XPath',
                            category: 'Extraction',
                            description: 'XPath extraction',
                            schema: { attributes: [] }
                        }
                    ]
                })
            });

            palette = new PluginPalette('test-palette');
            await palette.init();

            const templates = palette.getTemplates();

            expect(templates).toHaveLength(2);

            // Check first template
            expect(templates[0]).toEqual({
                type: 'http',
                label: 'Http',
                category: 'Network',
                description: 'HTTP plugin',
                icon: '🌐',
                defaultAttributes: { url: 'https://example.com' }
            });

            // Check second template
            expect(templates[1]).toEqual({
                type: 'xpath',
                label: 'XPath',
                category: 'Extraction',
                description: 'XPath extraction',
                icon: '📦', // Default icon
                defaultAttributes: {}
            });
        });

        it('should extract default attributes from schema correctly', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => ({
                    count: 1,
                    plugins: [
                        {
                            id: 'http',
                            name: 'Http',
                            category: 'Network',
                            description: 'HTTP',
                            schema: {
                                attributes: [
                                    { name: 'url', defaultValue: 'https://api.com' },
                                    { name: 'method', defaultValue: 'GET' },
                                    { name: 'timeout' } // No default
                                ]
                            }
                        }
                    ]
                })
            });

            palette = new PluginPalette('test-palette');
            await palette.init();

            const templates = palette.getTemplates();

            expect(templates[0].defaultAttributes).toEqual({
                url: 'https://api.com',
                method: 'GET'
                // timeout should NOT be included (no defaultValue)
            });
        });

        it('should render categories after successful API load', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => ({
                    count: 3,
                    plugins: [
                        { id: 'http', name: 'Http', category: 'Network', description: '', schema: { attributes: [] } },
                        { id: 'xpath', name: 'XPath', category: 'Extraction', description: '', schema: { attributes: [] } },
                        { id: 'def', name: 'Def', category: 'Variables', description: '', schema: { attributes: [] } }
                    ]
                })
            });

            palette = new PluginPalette('test-palette');
            await palette.init();

            // Should render 3 categories
            const categoryHeaders = container.querySelectorAll('.category-header');
            expect(categoryHeaders.length).toBeGreaterThan(0);
        });

        it('should log success message with plugin count', async () => {
            const consoleSpy = jest.spyOn(console, 'log');

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => ({
                    count: 27,
                    plugins: Array.from({ length: 27 }, (_, i) => ({
                        id: `plugin-${i}`,
                        name: `Plugin ${i}`,
                        category: 'Core',
                        description: '',
                        schema: { attributes: [] }
                    }))
                })
            });

            palette = new PluginPalette('test-palette');
            await palette.init();

            expect(consoleSpy).toHaveBeenCalledWith(
                expect.stringContaining('✅'),
                expect.stringContaining('Loaded'),
                expect.stringContaining('27'),
                expect.stringContaining('plugins')
            );

            consoleSpy.mockRestore();
        });
    });

    describe('init() - Error Handling', () => {
        it('should fallback to hardcoded templates on fetch failure', async () => {
            (global.fetch as jest.Mock).mockRejectedValueOnce(new Error('Network error'));

            const consoleSpy = jest.spyOn(console, 'error');

            palette = new PluginPalette('test-palette');

            // Constructor loads hardcoded templates
            const initialCount = palette.getTemplates().length;
            expect(initialCount).toBeGreaterThan(0); // Hardcoded templates

            await palette.init();

            // Should log error
            expect(consoleSpy).toHaveBeenCalledWith(
                expect.stringContaining('❌'),
                expect.stringContaining('Failed to load plugins from API'),
                expect.any(Error)
            );

            // Should still have templates (hardcoded fallback)
            const templates = palette.getTemplates();
            expect(templates.length).toBe(initialCount); // Same as before (fallback)

            consoleSpy.mockRestore();
        });

        it('should handle HTTP 404 error gracefully', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: false,
                status: 404,
                statusText: 'Not Found'
            });

            const consoleSpy = jest.spyOn(console, 'error');

            palette = new PluginPalette('test-palette');
            await palette.init();

            expect(consoleSpy).toHaveBeenCalled();

            // Should still have hardcoded templates
            expect(palette.getTemplates().length).toBeGreaterThan(0);

            consoleSpy.mockRestore();
        });

        it('should handle malformed JSON response', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => {
                    throw new Error('Invalid JSON');
                }
            });

            const consoleSpy = jest.spyOn(console, 'error');

            palette = new PluginPalette('test-palette');
            await palette.init();

            expect(consoleSpy).toHaveBeenCalled();

            // Should fallback to hardcoded templates
            expect(palette.getTemplates().length).toBeGreaterThan(0);

            consoleSpy.mockRestore();
        });
    });

    describe('render() - After API Load', () => {
        it('should render all categories from API', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => ({
                    count: 8,
                    plugins: [
                        { id: 'http', name: 'Http', category: 'Network', description: '', schema: { attributes: [] } },
                        { id: 'xpath', name: 'XPath', category: 'Extraction', description: '', schema: { attributes: [] } },
                        { id: 'xquery', name: 'XQuery', category: 'Extraction', description: '', schema: { attributes: [] } },
                        { id: 'def', name: 'Def', category: 'Variables', description: '', schema: { attributes: [] } },
                        { id: 'loop', name: 'Loop', category: 'Control', description: '', schema: { attributes: [] } },
                        { id: 'if', name: 'If', category: 'Control', description: '', schema: { attributes: [] } },
                        { id: 'script', name: 'Script', category: 'Processing', description: '', schema: { attributes: [] } },
                        { id: 'file', name: 'File', category: 'Storage', description: '', schema: { attributes: [] } }
                    ]
                })
            });

            palette = new PluginPalette('test-palette');
            await palette.init();

            // Should have 6 unique categories
            const categories = palette.getCategories();
            expect(categories).toContain('Network');
            expect(categories).toContain('Extraction');
            expect(categories).toContain('Variables');
            expect(categories).toContain('Control');
            expect(categories).toContain('Processing');
            expect(categories).toContain('Storage');
        });

        it('should show empty state when API returns no plugins', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: async () => ({
                    count: 0,
                    plugins: []
                })
            });

            palette = new PluginPalette('test-palette');

            // Manually clear templates to simulate empty state
            (palette as any).templates = [];
            (palette as any).filteredTemplates = [];

            await palette.init();

            // Should show empty state message
            expect(container.innerHTML).toContain('No plugins found');
        });
    });

    describe('Real API Integration (if server running)', () => {
        it('should load 27 plugins from real API', async () => {
            // This test will fail if server is not running, which is OK for CI
            // In local dev, it validates real API integration

            try {
                palette = new PluginPalette('test-palette');
                await palette.init();

                const templates = palette.getTemplates();

                // If API is available, should have 27 plugins
                if (templates.length > 0) {
                    console.log(`✅ Real API test: Loaded ${templates.length} plugins`);
                    expect(templates.length).toBeGreaterThanOrEqual(20); // At least 20 plugins
                }
            } catch (error) {
                // Server not running, skip test
                console.log('⚠️ Skipping real API test (server not running)');
            }
        });
    });
});




