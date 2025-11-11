import { describe, expect, it } from '@jest/globals';
import type { PluginTemplate } from '../VisualModelHelpers';
import {
    filterPaletteTemplates,
    groupPaletteTemplates,
    buildPaletteViewModel,
    type PaletteFilterOptions
} from '../PluginPaletteModel';

const createTemplate = (overrides: Partial<PluginTemplate> & {
    type: string;
} & {
    semantics?: {
        roles?: string[];
        requiredParentTypes?: string[];
        allowsAnyChild?: boolean;
    };
    requiredParentTypes?: string[];
}): PluginTemplate & {
    semantics?: {
        roles?: string[];
        requiredParentTypes?: string[];
        allowsAnyChild?: boolean;
    };
    requiredParentTypes?: string[];
} => ({
    type: overrides.type,
    label: overrides.label ?? overrides.type,
    category: overrides.category ?? 'general',
    icon: overrides.icon ?? 'icon',
    defaultAttributes: {},
    description: overrides.description ?? '',
    canAppearAtRoot: overrides.canAppearAtRoot ?? true,
    ...overrides
});

describe('PluginPaletteModel filtering', () => {
    const templates = [
        createTemplate({
            type: 'http',
            semantics: { roles: ['CONTEXT_CONTAINER'], requiredParentTypes: [] }
        }),
        createTemplate({
            type: 'http-header',
            semantics: { roles: ['DEPENDENT'], requiredParentTypes: ['http'] },
            requiredParentTypes: ['http'],
            canAppearAtRoot: false
        }),
        createTemplate({
            type: 'xpath',
            semantics: { roles: ['BODY_EVALUATOR'] }
        })
    ];

    const baseOptions: PaletteFilterOptions = {
        searchTerm: '',
        containersOnly: false
    };

    it('always excludes dependent templates from palette results', () => {
        const result = filterPaletteTemplates(templates, baseOptions);
        const types = result.map(t => t.type);
        expect(types).toContain('http');
        expect(types).toContain('xpath');
        expect(types).not.toContain('http-header');
    });

    it('applies search term across label and type', () => {
        const result = filterPaletteTemplates(templates, {
            ...baseOptions,
            searchTerm: 'xpath'
        });
        expect(result).toHaveLength(1);
        expect(result[0].type).toBe('xpath');
    });

    it('buildPaletteViewModel omits dependent templates from primary groups', () => {
        const viewModel = buildPaletteViewModel(templates, baseOptions);
        const primaryTypes = viewModel.primaryGroups.flatMap(group => group.templates.map(t => t.type));
        const advancedTypes = viewModel.advancedGroups.flatMap(group => group.templates.map(t => t.type));

        expect(primaryTypes).toEqual(expect.arrayContaining(['http', 'xpath']));
        expect(primaryTypes).not.toContain('http-header');
        expect(advancedTypes).toHaveLength(0);
    });

    it('respects search term when building view model', () => {
        const viewModel = buildPaletteViewModel(templates, {
            ...baseOptions,
            searchTerm: 'http'
        });
        const primaryTypes = viewModel.primaryGroups.flatMap(group => group.templates.map(t => t.type));
        const advancedTypes = viewModel.advancedGroups.flatMap(group => group.templates.map(t => t.type));

        expect(primaryTypes).toEqual(['http']);
        expect(advancedTypes).toHaveLength(0);
    });
});

describe('PluginPaletteModel grouping', () => {
    const templates = [
        createTemplate({
            type: 'http',
            semantics: { roles: ['CONTEXT_CONTAINER'] }
        }),
        createTemplate({
            type: 'loop',
            semantics: { roles: ['CONTEXT_CONTAINER'] }
        }),
        createTemplate({
            type: 'xpath',
            semantics: { roles: ['BODY_EVALUATOR'] }
        })
    ];

    it('groups context containers under Workflow Blocks', () => {
        const groups = groupPaletteTemplates(templates);
        const workflowGroup = groups.find(group => group.name === 'Workflow Blocks');
        expect(workflowGroup).toBeDefined();
        expect(workflowGroup?.templates.map(t => t.type)).toEqual(
            expect.arrayContaining(['http', 'loop'])
        );
    });

    it('places evaluators under Utilities', () => {
        const groups = groupPaletteTemplates(templates);
        const utilities = groups.find(group => group.name === 'Utilities');
        expect(utilities).toBeDefined();
        expect(utilities?.templates.map(t => t.type)).toContain('xpath');
    });
});

