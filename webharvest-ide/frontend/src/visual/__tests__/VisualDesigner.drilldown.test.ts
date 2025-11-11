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

        const header = container.querySelector('[data-node-id="loop-1"] .container-header') as HTMLElement | null;
        expect(header).not.toBeNull();

        header!.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));

        const breadcrumbCurrent = container.querySelector('.breadcrumb-current') as HTMLElement | null;
        expect(breadcrumbCurrent?.textContent).toContain('Loop');

        const processMarkers = container.querySelector('.process-markers-layer');
        expect(processMarkers).toBeNull();
    });

    it('does not attempt to render connections for detail rows inside focus mode', () => {
        const pluginManager = createStubPluginManager();
        designer = new VisualDesigner('focus-canvas', pluginManager);

        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

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
        expect(connectionLines.length).toBe(0);

        warnSpy.mockRestore();
    });
});

