/**
 * TDD Tests: PropertyPanel + SchemaEditor Integration
 * Tests schema-driven property editing with type-specific editors
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { PropertyPanel } from '../PropertyPanel';
import type { VisualNode, Plugin } from '../../types';
import type { ContainerSummary } from '../HierarchyInspector';
import type { PluginManager } from '../../core/PluginManager';

// Mock fetch
const mockFetch = jest.fn();
global.fetch = mockFetch as jest.Mock;

describe('PropertyPanel - SchemaEditor Integration', () => {
    let container: HTMLElement;
    let propertyPanel: PropertyPanel;
    const createPluginManager = (plugins: Plugin[]): PluginManager => ({
        getPlugins: () => plugins,
        findPlugin: (id: string) => plugins.find(p => p.id === id) ?? null
    }) as unknown as PluginManager;

    beforeEach(async () => {
        // Create container
        container = document.createElement('div');
        container.id = 'test-property-panel';
        document.body.appendChild(container);

        mockFetch.mockReset();
        mockFetch.mockResolvedValue({
            ok: true,
            json: async () => ({ plugins: [] })
        });

        // Create PropertyPanel instance (constructor triggers metadata load)
        propertyPanel = new PropertyPanel('test-property-panel', { autoLoad: false });

        // Allow async constructor fetch to resolve with default response
        await Promise.resolve();

        // Clear call history for assertions (but keep default implementation)
        mockFetch.mockClear();

        // Reset metadata state so individual tests can control fetch flow
        (propertyPanel as any).metadataLoaded = false;
        (propertyPanel as any).pluginMetadata = [];
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
                type: 'network-task',
                attributes: { url: 'https://example.com', method: 'GET' },
                body: null,
                children: []
            };

            const mockPlugins: Plugin[] = [
                {
                    id: 'network-task',
                    name: 'Network Task',
                    description: 'Network request',
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

            propertyPanel.setPluginManager(createPluginManager(mockPlugins));
            await (propertyPanel as any).loadPluginMetadata();
            await propertyPanel.showNode(node);

            // Should render with SchemaEditor (not simple inputs)
            await new Promise(resolve => setTimeout(resolve, 100)); // Wait for async render

            const form = container.querySelector('.schema-editor-form');
            expect(form).toBeTruthy();

            const urlInput = container.querySelector('input[type="url"]');
            expect(urlInput).toBeTruthy();

            const segmented = container.querySelector('.editor-segmented-control');
            expect(segmented).toBeTruthy();
        });

        it('renders focus summary panel when focus data exists', async () => {
            const node: VisualNode = {
                id: 'node-focus',
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

            propertyPanel.setPluginManager(createPluginManager(mockPlugins));

            const summary: ContainerSummary = {
                childCount: 4,
                containerCount: 1,
                leafCount: 3,
                detailCount: 2,
                typeFrequency: [{ type: 'http-param', count: 2 }],
                attributeHighlights: [{ key: 'method', value: 'GET' }],
                detailBreakdown: [
                    { title: 'Headers', count: 2, preview: ['Accept → application/json', 'User-Agent → WebHarvest'] }
                ]
            };

            propertyPanel.updateFocusSummary({ nodeId: node.id, summary });

            await propertyPanel.showNode(node);

            const focusPanel = container.querySelector('.property-focus-summary');
            expect(focusPanel).not.toBeNull();
            expect(focusPanel?.textContent).toContain('Headers');
        });

        it('renders detail editor for http headers and propagates value updates', async () => {
            const mockPlugins: Plugin[] = [
                {
                    id: 'http',
                    name: 'HTTP Request',
                    description: '',
                    category: 'Network',
                    icon: 'fa-globe',
                    schema: {
                        attributes: [
                            { name: 'url', type: 'url', required: true, description: 'Target URL' }
                        ],
                        canHaveBody: false,
                        canHaveChildren: true,
                        canAppearAtRoot: true,
                        requiredParents: [],
                        isContextSpecific: false,
                        allowedSubElements: ['http-header']
                    },
                    semantics: {
                        roles: ['CONTEXT_CONTAINER'],
                        canAppearAtRoot: true,
                        allowsChildren: true,
                        acceptsAnyChild: false,
                        allowBody: false,
                        allowTextContent: false,
                        allowedChildTypes: ['http-header'],
                        requiredParentTypes: []
                    },
                    allowedChildPlugins: ['http-header'],
                    ui: {
                        detailSections: [
                            {
                                type: 'http-header',
                                title: 'Headers',
                                addLabel: 'Add Header',
                                emptyLabel: 'No headers yet.',
                                primaryAttribute: 'name',
                                secondaryAttribute: 'value'
                            }
                        ]
                    }
                },
                {
                    id: 'http-header',
                    name: 'HTTP Header',
                    description: '',
                    category: 'Network',
                    icon: 'fa-tag',
                    schema: {
                        attributes: [
                            { name: 'name', type: 'string', required: true, description: 'Header key' },
                            { name: 'value', type: 'string', required: true, description: 'Header value' }
                        ],
                        canHaveBody: false,
                        canHaveChildren: false,
                        canAppearAtRoot: false,
                        requiredParents: ['http'],
                        isContextSpecific: true
                    },
                    semantics: {
                        roles: ['DEPENDENT'],
                        canAppearAtRoot: false,
                        allowsChildren: false,
                        acceptsAnyChild: false,
                        allowBody: false,
                        allowTextContent: false,
                        allowedChildTypes: [],
                        requiredParentTypes: ['http']
                    }
                }
            ];

            propertyPanel.setPluginManager(createPluginManager(mockPlugins));

            const node: VisualNode = {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                body: null,
                children: [
                    {
                        id: 'header-1',
                        type: 'http-header',
                        attributes: { name: 'Accept', value: 'application/json' },
                        body: null,
                        children: []
                    }
                ]
            };

            const changeSpy = jest.fn();
            propertyPanel.onChange(changeSpy);

            await propertyPanel.showNode(node);

            const detailEditor = container.querySelector('.detail-editor');
            expect(detailEditor).not.toBeNull();

            const nameInput = detailEditor!.querySelector('input.detail-input[id*="-primary"]') as HTMLInputElement | null;
            const valueInput = detailEditor!.querySelector('input.detail-input[id*="-secondary"]') as HTMLInputElement | null;

            expect(nameInput).not.toBeNull();
            expect(valueInput).not.toBeNull();
            expect(nameInput!.value).toBe('Accept');
            expect(valueInput!.value).toBe('application/json');

            valueInput!.value = 'text/html';
            valueInput!.dispatchEvent(new Event('input', { bubbles: true }));

            expect(changeSpy).toHaveBeenCalled();
            const updatedNode = changeSpy.mock.calls.at(-1)?.[0] as VisualNode;
            const updatedHeader = updatedNode.children?.find(child => child.id === 'header-1');
            expect(updatedHeader?.attributes?.value).toBe('text/html');
        });

        it('should fallback to simple inputs when schema not available', async () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'unknown-plugin',
                attributes: { attr1: 'value1' },
                body: null,
                children: []
            };

            propertyPanel.setPluginManager(createPluginManager([]));
            await (propertyPanel as any).loadPluginMetadata();
            await propertyPanel.showNode(node);

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
            await propertyPanel.showNode(node);

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
            await propertyPanel.showNode(node);

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
            await propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const checkbox = container.querySelector('input[type="checkbox"]') as HTMLInputElement;
            expect(checkbox).toBeTruthy();
            expect(checkbox.checked).toBe(true);

            const stateLabel = container.querySelector('.editor-toggle__state');
            expect(stateLabel?.textContent).toBe('On');
        });

        it('should render segmented control for enum with small option set', async () => {
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
            await propertyPanel.showNode(node);

            await new Promise(resolve => setTimeout(resolve, 100));

            const segmented = container.querySelector('.editor-segmented-control') as HTMLElement;
            expect(segmented).toBeTruthy();

            const buttons = Array.from(segmented.querySelectorAll('button'));
            const labels = buttons.map(btn => btn.textContent);
            expect(labels).toEqual(['GET', 'POST', 'PUT']);

            const active = segmented.querySelector('.is-active') as HTMLButtonElement | null;
            expect(active?.textContent).toBe('POST');

            const hiddenInput = container.querySelector('input[type="hidden"][name="method"]') as HTMLInputElement | null;
            expect(hiddenInput?.value).toBe('POST');
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
            await propertyPanel.showNode(node);

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
                json: async () => ({ plugins: mockPlugins })
            });

            let onChangeCalled = false;
            propertyPanel.onChange(() => {
                onChangeCalled = true;
            });

            await (propertyPanel as any).loadPluginMetadata();
            await propertyPanel.showNode(node);

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

