import { describe, expect, it } from '@jest/globals';
import type { Plugin, VisualNode } from '../../../types';
import { MasterDetailRegistry } from '../MasterDetailRegistry';
import type { PluginManager } from '../../../core/PluginManager';

const createSchema = (): Plugin['schema'] => ({
    attributes: [],
    canHaveBody: false,
    canHaveChildren: false,
    canAppearAtRoot: true,
    requiredParents: [],
    isContextSpecific: false,
    allowedSubElements: []
});

const stubPlugin = (overrides: Partial<Plugin>): Plugin => ({
    id: overrides.id ?? 'stub',
    name: overrides.name ?? 'Stub',
    description: overrides.description ?? '',
    category: overrides.category ?? 'General',
    icon: overrides.icon ?? 'fa-puzzle-piece',
    schema: overrides.schema ?? createSchema(),
    semantics: overrides.semantics,
    helpUrl: overrides.helpUrl,
    documentation: overrides.documentation ?? { summary: '', url: undefined, examples: [] },
    dependencies: overrides.dependencies ?? [],
    color: overrides.color,
    group: overrides.group,
    defaultWidth: overrides.defaultWidth,
    defaultHeight: overrides.defaultHeight,
    inputPorts: overrides.inputPorts,
    outputPorts: overrides.outputPorts,
    portType: overrides.portType,
    tooltip: overrides.tooltip,
    keyboardShortcuts: overrides.keyboardShortcuts,
    accessibility: overrides.accessibility,
    linkedServices: overrides.linkedServices,
    parentRelationship: overrides.parentRelationship,
    allowedChildPlugins: overrides.allowedChildPlugins,
    paletteGroup: overrides.paletteGroup,
    tags: overrides.tags,
    ui: overrides.ui
});

describe('MasterDetailRegistry', () => {
    const headerPlugin = stubPlugin({
        id: 'http-header',
        name: 'HTTP Header',
        schema: {
            ...createSchema(),
            attributes: [
                { name: 'name', type: 'string', required: true },
                { name: 'value', type: 'string', required: true }
            ]
        },
        semantics: {
            roles: ['DEPENDENT'],
            role: 'DEPENDENT',
            canAppearAtRoot: false,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: ['http']
        }
    });

    const httpPlugin = stubPlugin({
        id: 'http',
        name: 'HTTP Request',
        semantics: {
            roles: ['CONTEXT_CONTAINER'],
            role: 'CONTEXT_CONTAINER',
            canAppearAtRoot: true,
            allowsChildren: true,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: ['http-header'],
            requiredParentTypes: []
        },
        schema: {
            ...createSchema(),
            canHaveChildren: true,
            allowedSubElements: ['http-header']
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
                    secondaryAttribute: 'value',
                    summaryEmptyLabel: 'Unnamed header'
                }
            ]
        }
    });

    const pluginMap: Record<string, Plugin> = {
        http: httpPlugin,
        'http-header': headerPlugin
    };

    const manager = {
        findPlugin: (id: string) => pluginMap[id] ?? null
    } as unknown as PluginManager;

    it('applies UI overrides when building sections', () => {
        const registry = new MasterDetailRegistry(manager);
        const sections = registry.getSections('http');
        expect(sections).toHaveLength(1);
        const section = sections[0];

        expect(section.sectionTitle).toBe('Headers');
        expect(section.addLabel).toBe('Add Header');
        expect(section.emptyLabel).toBe('No headers yet.');
        expect(section.primaryAttribute).toBe('name');
        expect(section.secondaryAttribute).toBe('value');
        expect(section.summaryEmptyLabel).toBe('Unnamed header');
    });

    it('creates detail nodes with default values and unique ids', () => {
        const registry = new MasterDetailRegistry(manager);
        registry.getSections('http'); // prime cache
        const detail = registry.createDetailNode('http-header');

        expect(detail).not.toBeNull();
        expect(detail?.id).toMatch(/^http-header-/);
        expect(detail?.attributes?.name).toBe('');
        expect(detail?.attributes?.value).toBe('');
    });

    it('summarises details using configured attributes', () => {
        const registry = new MasterDetailRegistry(manager);
        registry.getSections('http');

        const detail: VisualNode = {
            id: 'detail-1',
            type: 'http-header',
            attributes: { name: 'Accept', value: 'application/json' },
            body: null,
            children: []
        };

        const summary = registry.summarizeDetail(detail);
        expect(summary.primaryText).toBe('Accept');
        expect(summary.secondaryText).toBe('application/json');
        expect(summary.isEmpty).toBe(false);
    });
});

