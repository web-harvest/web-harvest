/**
 * VisualDesigner drill-down behaviour tests
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
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
        paletteGroup: 'Network',
        tags: [],
        schema: {
            attributes: [],
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
        helpUrl: undefined,
        documentation: { summary: '' },
        dependencies: [],
        color: '#0a84ff',
        group: 'Network',
        defaultWidth: 320,
        defaultHeight: 180,
        inputPorts: [],
        outputPorts: [],
        portType: 'STRING',
        tooltip: 'HTTP request',
        keyboardShortcuts: [],
        accessibility: undefined,
        linkedServices: [],
        parentRelationship: undefined,
        allowedChildPlugins: ['http-header'],
        ui: {
            paletteGroup: 'Network',
            tags: ['http']
        }
    };

    const headerPlugin: Plugin = {
        id: 'http-header',
        name: 'HTTP Header',
        description: '',
        category: 'network',
        icon: 'fa-tag',
        paletteGroup: 'Network',
        tags: [],
        schema: {
            attributes: [
                { name: 'name', type: 'string', required: false },
                { name: 'value', type: 'string', required: false }
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
        helpUrl: undefined,
        documentation: { summary: '' },
        dependencies: [],
        color: '#ff9f0a',
        group: 'Network',
        defaultWidth: 200,
        defaultHeight: 120,
        inputPorts: [],
        outputPorts: [],
        portType: 'STRING',
        tooltip: 'HTTP header',
        keyboardShortcuts: [],
        accessibility: undefined,
        linkedServices: [],
        parentRelationship: {
            parentElementName: 'http',
            required: true
        },
        allowedChildPlugins: [],
        ui: {
            paletteGroup: 'Network',
            tags: ['header']
        }
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

describe('VisualDesigner drill-down focus mode', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'focus-canvas';
        document.body.appendChild(container);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('updates breadcrumbs and canvas when focusing a container', () => {
        designer = new VisualDesigner('focus-canvas');
        const nodes: VisualNode[] = [
            {
                id: 'loop-1',
                type: 'loop',
                attributes: { name: 'Top Loop' },
                body: null,
                children: [
                    {
                        id: 'var-1',
                        type: 'var',
                        attributes: { name: 'counter' },
                        body: null,
                        children: []
                    }
                ]
            }
        ];

        designer.loadNodes(nodes);

        const focusButton = container.querySelector('[data-node-id="loop-1"] .node-actions__btn[data-action="focus"]') as HTMLButtonElement | null;
        expect(focusButton).not.toBeNull();

        focusButton!.dispatchEvent(new MouseEvent('click', { bubbles: true }));

        const breadcrumbCurrent = container.querySelector('.breadcrumb-current .breadcrumb-item__label') as HTMLElement | null;
        expect(breadcrumbCurrent?.textContent).toContain('Loop');

        const processMarkers = container.querySelectorAll('.process-marker');
        expect(processMarkers.length).toBe(2);
        processMarkers.forEach(marker => {
            expect(marker.classList.contains('process-marker--contextual')).toBe(false);
            expect(['workflow-start', 'workflow-end']).toContain(marker.getAttribute('data-node-id'));
        });
    });

    it('renders scoped connections and start/end anchors when focusing a container', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('focus-canvas', pluginManager);

        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
        const summarySpy = jest.fn();
        designer.addEventListener('focusSummaryUpdated', summarySpy);

        const nodes: VisualNode[] = [
            {
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
                    },
                    {
                        id: 'header-2',
                        type: 'http-header',
                        attributes: { name: 'User-Agent', value: 'WebHarvest' },
                        body: null,
                        children: []
                    }
                ]
            }
        ];

        designer.loadNodes(nodes);
        (designer as unknown as { enterContainer: (id: string) => void }).enterContainer('http-1');

        const warningTriggered = warnSpy.mock.calls.some(call => {
            return call.some(arg => typeof arg === 'string' && arg.includes('Cannot render connection'));
        });

        expect(warningTriggered).toBe(false);

        const connectionLines = container.querySelectorAll('.connection-line');
        expect(connectionLines.length).toBeGreaterThan(0);

        const focusStartMarker = container.querySelector('.process-marker[data-node-id="workflow-start"]');
        const focusEndMarker = container.querySelector('.process-marker[data-node-id="workflow-end"]');
        expect(focusStartMarker).not.toBeNull();
        expect(focusEndMarker).not.toBeNull();
        expect(focusStartMarker!.classList.contains('process-marker--contextual')).toBe(false);
        expect(focusEndMarker!.classList.contains('process-marker--contextual')).toBe(false);

        expect(summarySpy).toHaveBeenCalled();
        const lastSummary = summarySpy.mock.calls[summarySpy.mock.calls.length - 1]?.[0] as any;
        expect(lastSummary).toBeDefined();
        expect(lastSummary?.summary?.detailBreakdown.length).toBeGreaterThan(0);

        warnSpy.mockRestore();
    });

    it('shows BPMN start/end overlay when focusing a container', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('focus-canvas', pluginManager);

        const nodes: VisualNode[] = [
            {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                body: null,
                children: [
                    {
                        id: 'step-1',
                        type: 'http-header',
                        attributes: { name: 'Accept', value: 'application/json' },
                        body: null,
                        children: []
                    },
                    {
                        id: 'step-2',
                        type: 'http-header',
                        attributes: { name: 'User-Agent', value: 'WebHarvest' },
                        body: null,
                        children: []
                    }
                ]
            }
        ];

        designer.loadNodes(nodes);
        (designer as unknown as { enterContainer: (id: string) => void }).enterContainer('http-1');

        const overlay = container.querySelector('.focus-flow-overlay');
        expect(overlay).not.toBeNull();

        const startPin = overlay!.querySelector('.focus-flow-pin--start span');
        const endPin = overlay!.querySelector('.focus-flow-pin--end span');
        const countChip = overlay!.querySelector('.focus-flow-overlay__count');

        expect(startPin?.textContent).toContain('Accept');
        expect(endPin?.textContent).toContain('User-Agent');
        expect(countChip?.textContent).toContain('2 step');
    });

    it('keeps focused container cards aligned with root styling', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('focus-canvas', pluginManager);

        const nodes: VisualNode[] = [
            {
                id: 'loop-1',
                type: 'loop',
                attributes: { name: 'outer' },
                body: null,
                children: [
                    {
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
                    }
                ]
            }
        ];

        designer.loadNodes(nodes);
        (designer as unknown as { enterContainer: (id: string) => void }).enterContainer('loop-1');

        let childCard = container.querySelector('[data-node-id="http-1"]') as HTMLElement | null;
        expect(childCard).not.toBeNull();
        expect(childCard!.querySelector('.visual-node__surface')).not.toBeNull();
        expect(childCard!.querySelector('.visual-node__meta')).not.toBeNull();

        const collapseButton = childCard!.querySelector('.visual-node__collapse-btn') as HTMLButtonElement | null;
        expect(collapseButton).not.toBeNull();
        expect(childCard!.querySelector('.visual-node__child-stack')).not.toBeNull();
        collapseButton!.dispatchEvent(new MouseEvent('click', { bubbles: true }));

        childCard = container.querySelector('[data-node-id="http-1"]') as HTMLElement | null;
        expect(childCard).not.toBeNull();
        expect(childCard!.querySelector('.visual-node__child-stack')).toBeNull();
        expect(childCard!.querySelector('.visual-node__collapsed-hint')).not.toBeNull();
    });
});

