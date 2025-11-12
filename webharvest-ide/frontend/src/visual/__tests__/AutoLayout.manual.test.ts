import { describe, expect, it, beforeEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode, Plugin } from '../../types';
import type { PluginManager } from '../../core/PluginManager';

const createPlugin = (overrides: Partial<Plugin> & { id: string }): Plugin => ({
    id: overrides.id,
    name: overrides.name ?? overrides.id,
    description: overrides.description ?? '',
    category: overrides.category ?? 'General',
    icon: overrides.icon ?? 'fa-puzzle-piece',
    schema: overrides.schema ?? {
        attributes: [],
        canHaveBody: false,
        canHaveChildren: false,
        canAppearAtRoot: true,
        requiredParents: [],
        isContextSpecific: false,
        allowedSubElements: []
    },
    semantics: overrides.semantics ?? {
        roles: ['LEAF'],
        canAppearAtRoot: true,
        allowsChildren: false,
        acceptsAnyChild: false,
        allowBody: false,
        allowTextContent: false,
        allowedChildTypes: [],
        requiredParentTypes: []
    },
    documentation: overrides.documentation ?? { summary: '', examples: [] },
    dependencies: overrides.dependencies ?? [],
    color: overrides.color,
    group: overrides.group,
    defaultWidth: overrides.defaultWidth,
    defaultHeight: overrides.defaultHeight,
    inputPorts: overrides.inputPorts,
    outputPorts: overrides.outputPorts,
    portType: overrides.portType,
    tooltip: overrides.tooltip,
    keyboardShortcuts: overrides.keyboardShortcuts ?? [],
    accessibility: overrides.accessibility,
    linkedServices: overrides.linkedServices ?? [],
    parentRelationship: overrides.parentRelationship,
    allowedChildPlugins: overrides.allowedChildPlugins ?? [],
    paletteGroup: overrides.paletteGroup,
    tags: overrides.tags,
    ui: overrides.ui
});

const pluginRegistry: Record<string, Plugin> = {
    loop: createPlugin({
        id: 'loop',
        semantics: {
            roles: ['ROOT_CONTAINER'],
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        },
        defaultWidth: 320,
        defaultHeight: 200
    }),
    http: createPlugin({
        id: 'http',
        semantics: {
            roles: ['ROOT_CONTAINER'],
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        },
        defaultWidth: 280,
        defaultHeight: 180
    })
};

const pluginManager: PluginManager = {
    findPlugin: (id: string) => pluginRegistry[id] ?? null,
    findPluginById: (id: string) => pluginRegistry[id] ?? null,
    getPlugins: () => Object.values(pluginRegistry),
    getAllPlugins: () => Object.values(pluginRegistry),
    getSemantics: (id: string) => pluginRegistry[id]?.semantics ?? null,
    searchPlugins: () => Object.values(pluginRegistry),
    getPluginsByCategory: () => Object.values(pluginRegistry),
    getCategories: () => ['General'],
    getRootLevelPlugins: () => Object.values(pluginRegistry),
    getDependentPlugins: () => [],
    getMasterPlugins: () => [],
    getCatalogSchema: () => null,
    getSchemaVersion: () => null,
    loadPlugins: async () => Object.values(pluginRegistry),
    getDocumentation: () => null
} as unknown as PluginManager;

describe('VisualDesigner auto-layout with manual nodes', () => {
    let designer: VisualDesigner;

    beforeEach(() => {
        document.body.innerHTML = '<div id="auto-layout-canvas"></div>';
        designer = new VisualDesigner('auto-layout-canvas');
        designer.setPluginManager(pluginManager);
    });

    it('respects manual root positions when laying out automatic nodes', () => {
        const nodes: VisualNode[] = [
            {
                id: 'manual-loop',
                type: 'loop',
                attributes: { name: 'Manual loop' },
                body: null,
                children: [],
                position: { x: 600, y: 120 },
                isManualPositioned: true,
                dimensions: { width: 320, height: 200 }
            },
            {
                id: 'auto-http',
                type: 'http',
                attributes: { url: 'https://example.com' },
                body: null,
                children: []
            }
        ];

        designer.loadNodes(nodes);
        designer.render();

        const renderedNodes = designer.getNodes();
        const manualNode = renderedNodes.find(node => node.id === 'manual-loop');
        const autoNode = renderedNodes.find(node => node.id === 'auto-http');

        expect(manualNode?.position?.x).toBe(600);
        expect(manualNode?.isManualPositioned).toBe(true);
        expect(autoNode?.isManualPositioned).toBe(false);
        expect(autoNode?.position?.x ?? 0).toBeGreaterThan((manualNode?.position?.x ?? 0) + 300);
        expect(autoNode?.position?.y).toBe(50);
    });
});

