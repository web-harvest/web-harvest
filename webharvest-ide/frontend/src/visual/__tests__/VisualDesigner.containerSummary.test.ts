import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode, Plugin } from '../types';
import type { PluginManager } from '../../core/PluginManager';

function createStubPluginManager(): PluginManager {
    const httpPlugin: Plugin = {
        id: 'http',
        name: 'HTTP Request',
        description: '',
        category: 'network',
        icon: 'fa-globe',
        schema: {
            attributes: [
                { name: 'url', type: 'url', required: true, description: 'URL' },
                { name: 'method', type: 'enum', required: true, allowedValues: ['GET', 'POST'], description: 'Method' }
            ],
            canHaveBody: false,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false,
            allowedSubElements: ['http-header'],
            allowsBody: false
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
        documentation: { summary: '' },
        dependencies: [],
        color: '#0a84ff',
        inputPorts: [],
        outputPorts: [],
        keyboardShortcuts: [],
        linkedServices: [],
        allowedChildPlugins: ['http-header'],
        ui: {
            detailSections: [
                {
                    type: 'http-header',
                    title: 'Headers',
                    addLabel: 'Add Header',
                    emptyLabel: 'No headers',
                    primaryAttribute: 'name',
                    secondaryAttribute: 'value'
                }
            ]
        }
    };

    const headerPlugin: Plugin = {
        id: 'http-header',
        name: 'HTTP Header',
        description: '',
        category: 'network',
        icon: 'fa-tag',
        schema: {
            attributes: [
                { name: 'name', type: 'string', required: true, description: 'Header' },
                { name: 'value', type: 'string', required: true, description: 'Value' }
            ],
            canHaveBody: false,
            canHaveChildren: false,
            canAppearAtRoot: false,
            requiredParents: ['http'],
            isContextSpecific: false,
            allowsBody: false
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
        },
        documentation: { summary: '' },
        dependencies: [],
        color: '#ff9f0a',
        inputPorts: [],
        outputPorts: [],
        keyboardShortcuts: [],
        linkedServices: [],
        parentRelationship: {
            parentElementName: 'http',
            required: true
        },
        allowedChildPlugins: []
    };

    const registry: Record<string, Plugin> = {
        http: httpPlugin,
        'http-header': headerPlugin
    };

    return {
        findPlugin: (id: string) => registry[id] ?? null,
        findPluginById: (id: string) => registry[id] ?? null,
        getSemantics: (id: string) => registry[id]?.semantics ?? null,
        getPlugins: () => Object.values(registry),
        getAllPlugins: () => Object.values(registry),
        getSchemaVersion: () => null,
        getCatalogSchema: () => null,
        loadPlugins: async () => Object.values(registry),
        searchPlugins: () => Object.values(registry),
        getPluginsByCategory: () => Object.values(registry),
        getCategories: () => ['Network'],
        getDocumentation: () => null
    } as unknown as PluginManager;
}

describe('VisualDesigner container summary', () => {
    let container: HTMLElement;
    let designer: VisualDesigner;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'summary-canvas';
        document.body.appendChild(container);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('renders unified card with attributes and container metrics', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('summary-canvas', pluginManager);

        const nodes: VisualNode[] = [
            {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://api.example.com/orders', method: 'GET' },
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
            }
        ];

        designer.loadNodes(nodes);

        const summaryNode = container.querySelector('[data-node-id="http-1"]') as HTMLElement | null;
        expect(summaryNode).not.toBeNull();

        const attributesBlock = summaryNode!.querySelector('.visual-node__attributes');
        expect(attributesBlock).toBeNull();

        const metaRow = summaryNode!.querySelector('.visual-node__meta');
        expect(metaRow).not.toBeNull();
        const metaText = metaRow!.textContent || '';
        expect(metaText).toContain('1 children');
        expect(metaText).toContain('0 containers');
        expect(metaText).toContain('1 leaves');
    });

    it('allows removing nodes via role control button', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('summary-canvas', pluginManager);

        const nodes: VisualNode[] = [
            {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://api.example.com/orders', method: 'GET' },
                body: null,
                children: []
            }
        ];

        designer.loadNodes(nodes);

        const removeButton = container.querySelector('.node-actions__btn[data-action="delete"]') as HTMLButtonElement | null;
        expect(removeButton).not.toBeNull();

        removeButton!.dispatchEvent(new MouseEvent('click', { bubbles: true }));

        const remainingNode = container.querySelector('[data-node-id="http-1"]');
        expect(remainingNode).toBeNull();
    });

    it('Details button selects node and opens properties panel', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('summary-canvas', pluginManager);

        const sidebarRequest = jest.fn();
        designer.addEventListener('sidebarPanelRequested', sidebarRequest);

        const nodes: VisualNode[] = [
            {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://api.example.com/orders', method: 'GET' },
                body: null,
                children: []
            }
        ];

        designer.loadNodes(nodes);

        const detailsButton = container.querySelector('.node-actions__btn[data-action="details"]') as HTMLButtonElement | null;
        expect(detailsButton).not.toBeNull();

        detailsButton!.dispatchEvent(new MouseEvent('click', { bubbles: true }));

        expect(designer.getSelectedNodeId()).toBe('http-1');
        expect(sidebarRequest).toHaveBeenCalledWith('properties');
    });

    it('Focus button drills into container node and keeps selection', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('summary-canvas', pluginManager);

        const sidebarRequest = jest.fn();
        designer.addEventListener('sidebarPanelRequested', sidebarRequest);

        const nodes: VisualNode[] = [
            {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://api.example.com/orders', method: 'GET' },
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
            }
        ];

        designer.loadNodes(nodes);

        const focusButton = container.querySelector('.node-actions__btn[data-action="focus"]') as HTMLButtonElement | null;
        expect(focusButton).not.toBeNull();

        focusButton!.dispatchEvent(new MouseEvent('click', { bubbles: true }));

        const drillManager = (designer as unknown as { drillDownManager: { getCurrentId: () => string | null } }).drillDownManager;
        expect(drillManager.getCurrentId()).toBe('http-1');
        expect(designer.getSelectedNodeId()).toBe('http-1');
        expect(sidebarRequest).toHaveBeenCalledWith('properties');
    });

    it('does not start drag when clicking node action controls', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('summary-canvas', pluginManager);

        const nodes: VisualNode[] = [
            {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://api.example.com/orders', method: 'GET' },
                body: null,
                children: []
            }
        ];

        designer.loadNodes(nodes);

        const deleteButton = container.querySelector('.node-actions__btn[data-action="delete"]') as HTMLButtonElement | null;
        expect(deleteButton).not.toBeNull();
        const deleteMouseDown = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
        deleteButton!.dispatchEvent(deleteMouseDown);
        expect(deleteMouseDown.defaultPrevented).toBe(false);
        expect(document.body.style.userSelect).toBe('');

    });

    it('disables focus action for leaf nodes', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('summary-canvas', pluginManager);

        const nodes: VisualNode[] = [
            {
                id: 'log-1',
                type: 'http-header',
                attributes: { name: 'Accept', value: 'application/json' },
                body: null,
                children: []
            }
        ];

        designer.loadNodes(nodes);

        const focusButton = document.querySelector('.node-actions__btn[data-action="focus"]') as HTMLButtonElement | null;
        expect(focusButton).not.toBeNull();
        expect(focusButton!.disabled).toBe(true);
    });
});

