import { describe, expect, it, jest } from '@jest/globals';
import type { VisualNode, Plugin } from '../../../types';
import { MasterDetailEditor } from '../MasterDetailEditor';
import { MasterDetailRegistry, type DetailSectionDefinition } from '../MasterDetailRegistry';
import type { PluginManager } from '../../../core/PluginManager';

const httpHeaderPlugin: Plugin = {
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
        isContextSpecific: true,
        allowedSubElements: []
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
    documentation: { summary: '', examples: [] },
    dependencies: [],
    keyboardShortcuts: [],
    linkedServices: [],
    allowedChildPlugins: []
};

const httpPlugin: Plugin = {
    id: 'http',
    name: 'HTTP',
    description: '',
    category: 'Network',
    icon: 'fa-globe',
    schema: {
        attributes: [
            { name: 'method', type: 'enum', required: true, allowedValues: ['GET', 'POST'], description: 'HTTP method' },
            { name: 'url', type: 'url', required: true, description: 'Request URL' }
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
    documentation: { summary: '', examples: [] },
    dependencies: [],
    keyboardShortcuts: [],
    linkedServices: [],
    allowedChildPlugins: ['http-header']
};

const pluginMap: Record<string, Plugin> = {
    http: httpPlugin,
    'http-header': httpHeaderPlugin
};

const manager = {
    findPlugin: (id: string) => pluginMap[id] ?? null
} as unknown as PluginManager;

const registry = new MasterDetailRegistry(manager);

describe('MasterDetailEditor', () => {
    it('renders labeled inputs for detail attributes and emits updates', () => {
        const sections = registry.getSections('http');
        expect(sections).toHaveLength(1);
        const [section] = sections;

        const node: VisualNode = {
            id: 'http-1',
            type: 'http',
            attributes: { method: 'POST', url: 'https://example.com' },
            body: null,
            children: [
                {
                    id: 'header-1',
                    type: 'http-header',
                    attributes: { name: 'Accept' },
                    body: 'application/json',
                    children: []
                }
            ]
        };

        const handleChange = jest.fn();
        const editor = new MasterDetailEditor({
            node,
            sections: sections as DetailSectionDefinition[],
            registry,
            onChange: handleChange
        });

        const element = editor.render();
        const nameInput = element.querySelector('input.detail-input[id*="-primary"]') as HTMLInputElement | null;
        const valueInput = element.querySelector('input.detail-input[id*="-secondary"]') as HTMLInputElement | null;

        expect(nameInput).not.toBeNull();
        expect(valueInput).not.toBeNull();

        expect(nameInput!.value).toBe('Accept');
        expect(nameInput!.placeholder.toLowerCase()).toContain('header key');
        expect(valueInput!.value).toBe('application/json');
        expect(valueInput!.placeholder.toLowerCase()).toContain('value');

        nameInput!.value = 'Authorization';
        nameInput!.dispatchEvent(new Event('input'));

        expect(handleChange).toHaveBeenCalled();
        const lastCall = handleChange.mock.calls.at(-1);
        expect(lastCall).toBeDefined();
        const updatedNode = lastCall![0] as VisualNode;
        const updatedHeader = updatedNode.children?.find(child => child.id === 'header-1');
        expect(updatedHeader?.attributes?.name).toBe('Authorization');
    });
});

