/**
 * TDD Tests: PropertyPanel + SchemaEditor Integration
 * Tests schema-driven property editing with type-specific editors
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { PropertyPanel } from '../PropertyPanel';
import type { VisualNode, Plugin } from '../../types';

// Mock fetch
const mockFetch = jest.fn();
global.fetch = mockFetch as jest.Mock;

describe('PropertyPanel - SchemaEditor Integration', () => {
    let container: HTMLElement;
    let propertyPanel: PropertyPanel;

    beforeEach(() => {
        // Create container
        container = document.createElement('div');
        container.id = 'test-property-panel';
        document.body.appendChild(container);

        // Create PropertyPanel instance
        propertyPanel = new PropertyPanel('test-property-panel');

        // Clear mocks
        mockFetch.mockClear();
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('loadPluginMetadata', () => {
        it('should fetch plugin metadata from API', async () => {
            const mockPlugins: Plugin[] = [
                {
                    id: 'http',
                    name: 'HTTP',
                    description: 'HTTP request',
                    category: 'Network',
                    icon: 'fa-globe',
                    schema: {
                        attributes: [
                            {
                                name: 'url',
                                type: 'url',
                                required: true,
                                description: 'Target URL'
                            },
                            {
                                name: 'method',
                                type: 'enum',
                                required: false,
                                allowedValues: ['GET', 'POST', 'PUT'],
                                description: 'HTTP method'
                            }
                        ],
                        canHaveBody: true,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();

            expect(mockFetch).toHaveBeenCalledWith('/api/plugins/metadata');
        });

        it('should cache plugin metadata', async () => {
            const mockPlugins: Plugin[] = [];
            mockFetch.mockResolvedValue({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();
            await (propertyPanel as any).loadPluginMetadata();

            // Should only fetch once (cached)
            expect(mockFetch).toHaveBeenCalledTimes(1);
        });
    });

    describe('getPluginSchema', () => {
        it('should find plugin schema by node type', async () => {
            const mockPlugins: Plugin[] = [
                {
                    id: 'http',
                    name: 'HTTP',
                    description: 'HTTP request',
                    category: 'Network',
                    icon: 'fa-globe',
                    schema: {
                        attributes: [],
                        canHaveBody: true,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();

            const schema = (propertyPanel as any).getPluginSchema('http');
            expect(schema).toBeDefined();
            expect(schema.id).toBe('http');
        });

        it('should return null if plugin not found', async () => {
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: [] })
            });

            await (propertyPanel as any).loadPluginMetadata();

            const schema = (propertyPanel as any).getPluginSchema('unknown');
            expect(schema).toBeNull();
        });
    });

    describe('render with SchemaEditor', () => {
        it('should use SchemaEditor when plugin schema is available', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: { url: 'https://example.com', method: 'GET' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'http',
                    name: 'HTTP',
                    description: 'HTTP request',
                    category: 'Network',
                    icon: 'fa-globe',
                    schema: {
                        attributes: [
                            {
                                name: 'url',
                                type: 'url',
                                required: true,
                                description: 'Target URL'
                            },
                            {
                                name: 'method',
                                type: 'enum',
                                required: false,
                                allowedValues: ['GET', 'POST', 'PUT'],
                                description: 'HTTP method'
                            }
                        ],
                        canHaveBody: true,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            // Should render with SchemaEditor (not simple inputs)
            await new Promise(resolve => setTimeout(resolve, 100)); // Wait for async render

            const form = container.querySelector('.schema-editor-form');
            expect(form).toBeTruthy();

            // Should have type-specific inputs
            const urlInput = container.querySelector('input[type="url"]');
            expect(urlInput).toBeTruthy();

            const methodSelect = container.querySelector('select');
            expect(methodSelect).toBeTruthy();
        });

        it('should fallback to simple inputs when schema not available', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'unknown-plugin',
                attributes: { attr1: 'value1' },
                body: null,
                children: []
            };

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: [] })
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            // Should use simple text inputs (fallback)
            const inputs = container.querySelectorAll('input[type="text"]');
            expect(inputs.length).toBeGreaterThan(0);
        });
    });

    describe('type-specific editors', () => {
        it('should render URL input for url type', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'http',
                    name: 'HTTP',
                    description: 'HTTP request',
                    category: 'Network',
                    icon: 'fa-globe',
                    schema: {
                        attributes: [
                            {
                                name: 'url',
                                type: 'url',
                                required: true,
                                description: 'Target URL'
                            }
                        ],
                        canHaveBody: false,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const urlInput = container.querySelector('input[type="url"]') as HTMLInputElement;
            expect(urlInput).toBeTruthy();
            expect(urlInput.value).toBe('https://example.com');
        });

        it('should render number input for number type', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'test-plugin',
                attributes: { count: '5' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'test-plugin',
                    name: 'Test',
                    description: 'Test plugin',
                    category: 'Test',
                    icon: 'fa-test',
                    schema: {
                        attributes: [
                            {
                                name: 'count',
                                type: 'number',
                                required: false,
                                description: 'Count'
                            }
                        ],
                        canHaveBody: false,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const numberInput = container.querySelector('input[type="number"]') as HTMLInputElement;
            expect(numberInput).toBeTruthy();
            expect(numberInput.value).toBe('5');
        });

        it('should render checkbox for boolean type', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'test-plugin',
                attributes: { enabled: 'true' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'test-plugin',
                    name: 'Test',
                    description: 'Test plugin',
                    category: 'Test',
                    icon: 'fa-test',
                    schema: {
                        attributes: [
                            {
                                name: 'enabled',
                                type: 'boolean',
                                required: false,
                                description: 'Enabled'
                            }
                        ],
                        canHaveBody: false,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const checkbox = container.querySelector('input[type="checkbox"]') as HTMLInputElement;
            expect(checkbox).toBeTruthy();
            expect(checkbox.checked).toBe(true);
        });

        it('should render select for enum type', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'test-plugin',
                attributes: { method: 'POST' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'test-plugin',
                    name: 'Test',
                    description: 'Test plugin',
                    category: 'Test',
                    icon: 'fa-test',
                    schema: {
                        attributes: [
                            {
                                name: 'method',
                                type: 'enum',
                                required: false,
                                allowedValues: ['GET', 'POST', 'PUT'],
                                description: 'HTTP method'
                            }
                        ],
                        canHaveBody: false,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const select = container.querySelector('select') as HTMLSelectElement;
            expect(select).toBeTruthy();
            expect(select.value).toBe('POST');

            const options = Array.from(select.options).map(opt => opt.value);
            expect(options).toEqual(['GET', 'POST', 'PUT']);
        });
    });

    describe('required field indicators', () => {
        it('should show asterisk (*) for required fields', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'test-plugin',
                attributes: { url: 'https://example.com' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'test-plugin',
                    name: 'Test',
                    description: 'Test plugin',
                    category: 'Test',
                    icon: 'fa-test',
                    schema: {
                        attributes: [
                            {
                                name: 'url',
                                type: 'url',
                                required: true,
                                description: 'Target URL'
                            }
                        ],
                        canHaveBody: false,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ plugins: mockPlugins })
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const label = container.querySelector('label');
            expect(label).toBeTruthy();
            expect(label?.textContent).toContain('url *');
        });
    });

    describe('onChange callback', () => {
        it('should trigger onChange when field value changes', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'test-plugin',
                attributes: { url: 'https://example.com' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'test-plugin',
                    name: 'Test',
                    description: 'Test plugin',
                    category: 'Test',
                    icon: 'fa-test',
                    schema: {
                        attributes: [
                            {
                                name: 'url',
                                type: 'url',
                                required: true,
                                description: 'Target URL'
                            }
                        ],
                        canHaveBody: false,
                        canHaveChildren: false,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false
                    }
                }
            ];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                Vish: async () => ({ plugins: mockPlugins })
            });

            let onChangeCalled = false;
            propertyPanel.onChange(() => {
                onChangeCalled = true;
            });

            await (propertyPanel as any).loadPluginMetadata();
            propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const urlInput = container.querySelector('input[type="url"]') as HTMLInputElement;
            expect(urlInput).toBeTruthy();

            urlInput.value = 'https://new-url.com';
            urlInput.dispatchEvent(new Event('input'));

            await new Promise(resolve => setTimeout(resolve, 50));

            expect(onChangeCalled).toBe(true);
        });
    });
});

