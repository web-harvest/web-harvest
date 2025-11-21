import { MasterDetailRegistry } from '../MasterDetailRegistry';
import type { Plugin, MasterDetailConstraint } from '../../../types';
import type { PluginManager } from '../../../core/PluginManager';

describe('MasterDetailRegistry - Constraints', () => {
    let registry: MasterDetailRegistry;
    let mockPluginManager: jest.Mocked<PluginManager>;

    const createMockPlugin = (
        id: string,
        name: string,
        constraints?: MasterDetailConstraint[]
    ): Plugin => ({
        id,
        name,
        description: `Test plugin ${name}`,
        category: 'Test',
        icon: 'test-icon',
        schema: {
            attributes: [],
            canHaveBody: false,
            canHaveChildren: false,
            canAppearAtRoot: true,
            requiredParents: []
        },
        masterDetailConstraints: constraints
    });

    beforeEach(() => {
        mockPluginManager = {
            findPlugin: jest.fn(),
            getPlugins: jest.fn().mockReturnValue([])
        } as any;

        registry = new MasterDetailRegistry(mockPluginManager);
    });

    describe('getConstraintsForMaster', () => {
        it('should return constraints from plugin metadata', () => {
            const constraints: MasterDetailConstraint[] = [
                {
                    detailType: 'else',
                    maxOccurs: 1,
                    minOccurs: 0,
                    sectionTitle: 'Else Branch',
                    addLabel: 'Add Else'
                }
            ];

            const masterPlugin = createMockPlugin('if', 'If', constraints);
            mockPluginManager.findPlugin = jest.fn().mockReturnValue(masterPlugin);

            const result = registry.getConstraintsForMaster('if');

            expect(result).toEqual(constraints);
            expect(mockPluginManager.findPlugin).toHaveBeenCalledWith('if');
        });

        it('should return empty array if plugin has no constraints', () => {
            const masterPlugin = createMockPlugin('http', 'HTTP');
            mockPluginManager.findPlugin = jest.fn().mockReturnValue(masterPlugin);

            const result = registry.getConstraintsForMaster('http');

            expect(result).toEqual([]);
        });

        it('should return empty array if plugin not found', () => {
            mockPluginManager.findPlugin = jest.fn().mockReturnValue(null);

            const result = registry.getConstraintsForMaster('unknown');

            expect(result).toEqual([]);
        });
    });

    describe('getSections with constraints', () => {
        it('should apply constraints to detail sections', () => {
            const constraints: MasterDetailConstraint[] = [
                {
                    detailType: 'else',
                    maxOccurs: 1,
                    minOccurs: 0,
                    sectionTitle: 'Else Branch',
                    addLabel: 'Add Else'
                }
            ];

            const masterPlugin = createMockPlugin('if', 'If', constraints);
            masterPlugin.allowedChildPlugins = ['else'];
            masterPlugin.schema.canHaveChildren = true;

            const detailPlugin: Plugin = {
                id: 'else',
                name: 'Else',
                description: 'Else plugin',
                category: 'Control',
                icon: 'else-icon',
                schema: {
                    attributes: [],
                    canHaveBody: true,
                    canHaveChildren: false,
                    canAppearAtRoot: false,
                    requiredParents: ['if']
                },
                semantics: {
                    roles: ['DEPENDENT'],
                    canAppearAtRoot: false,
                    allowsChildren: false,
                    acceptsAnyChild: false,
                    allowBody: true,
                    allowTextContent: false,
                    allowedChildTypes: [],
                    requiredParentTypes: ['if']
                }
            };

            mockPluginManager.findPlugin = jest.fn((id: string) => {
                if (id === 'if') return masterPlugin;
                if (id === 'else') return detailPlugin;
                return null;
            });

            const sections = registry.getSections('if');

            expect(sections).toHaveLength(1);
            expect(sections[0].detailType).toBe('else');
            expect(sections[0].maxOccurs).toBe(1);
            expect(sections[0].minOccurs).toBe(0);
            expect(sections[0].sectionTitle).toBe('Else Branch');
            expect(sections[0].addLabel).toBe('Add Else');
        });

        it('should use default values when constraints not provided', () => {
            const masterPlugin = createMockPlugin('http', 'HTTP');
            masterPlugin.allowedChildPlugins = ['http-header'];
            masterPlugin.schema.canHaveChildren = true;

            const detailPlugin: Plugin = {
                id: 'http-header',
                name: 'HTTP Header',
                description: 'HTTP Header plugin',
                category: 'Network',
                icon: 'header-icon',
                schema: {
                    attributes: [
                        { name: 'name', type: 'string', required: true, description: 'Header name' },
                        { name: 'value', type: 'string', required: true, description: 'Header value' }
                    ],
                    canHaveBody: false,
                    canHaveChildren: false,
                    canAppearAtRoot: false,
                    requiredParents: ['http']
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
            };

            mockPluginManager.findPlugin = jest.fn((id: string) => {
                if (id === 'http') return masterPlugin;
                if (id === 'http-header') return detailPlugin;
                return null;
            });

            const sections = registry.getSections('http');

            expect(sections).toHaveLength(1);
            expect(sections[0].detailType).toBe('http-header');
            expect(sections[0].maxOccurs).toBe(-1); // unlimited by default
            expect(sections[0].minOccurs).toBe(0); // optional by default
        });
    });
});


