/**
 * OntologyBuilder - Dynamiczny builder ontologii z runtime metadata pluginów
 * 
 * Buduje Entity schemas, Relationship constraints, i Query capabilities
 * dynamicznie na podstawie metadanych pluginów z backendu.
 */

import type { Plugin, PluginSemanticsModel } from '../types';
import type { PluginManager } from '../core/PluginManager';
import type {
    OntologyEntity,
    EntitySchema,
    EntityType,
    RelationshipConstraint,
    RelationshipType,
    PropertySchema,
    Constraint,
    PluginOntologySchema,
    VisualOntologySchema,
    RuntimeOntologySchema
} from './OntologyModel';

export class OntologyBuilder {
    private pluginManager: PluginManager;
    private pluginSchemas: Map<string, EntitySchema> = new Map();
    private pluginOntologySchemas: Map<string, PluginOntologySchema> = new Map();
    private visualOntologySchemas: Map<string, VisualOntologySchema> = new Map();
    private runtimeOntologySchemas: Map<string, RuntimeOntologySchema> = new Map();

    constructor(pluginManager: PluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Zbuduj wszystkie schematy ontologiczne z runtime metadata
     */
    buildAllSchemas(): {
        pluginSchemas: Map<string, EntitySchema>;
        pluginOntologySchemas: Map<string, PluginOntologySchema>;
        visualOntologySchemas: Map<string, VisualOntologySchema>;
        runtimeOntologySchemas: Map<string, RuntimeOntologySchema>;
    } {
        const plugins = this.pluginManager.getPlugins();
        
        // Wyczyść stare schematy
        this.pluginSchemas.clear();
        this.pluginOntologySchemas.clear();
        this.visualOntologySchemas.clear();
        this.runtimeOntologySchemas.clear();

        // Zbuduj schematy dla każdego pluginu
        plugins.forEach(plugin => {
            const pluginSchema = this.buildPluginEntitySchema(plugin);
            const pluginOntologySchema = this.buildPluginOntologySchema(plugin);
            const visualOntologySchema = this.buildVisualOntologySchema(plugin);
            const runtimeOntologySchema = this.buildRuntimeOntologySchema(plugin);

            this.pluginSchemas.set(plugin.id, pluginSchema);
            this.pluginOntologySchemas.set(plugin.id, pluginOntologySchema);
            this.visualOntologySchemas.set(plugin.id, visualOntologySchema);
            this.runtimeOntologySchemas.set(plugin.id, runtimeOntologySchema);
        });

        return {
            pluginSchemas: this.pluginSchemas,
            pluginOntologySchemas: this.pluginOntologySchemas,
            visualOntologySchemas: this.visualOntologySchemas,
            runtimeOntologySchemas: this.runtimeOntologySchemas
        };
    }

    /**
     * Zbuduj EntitySchema dla pluginu z runtime metadata
     */
    private buildPluginEntitySchema(plugin: Plugin): EntitySchema {
        const properties = new Map<string, PropertySchema>();
        const allowedRelationships = new Map<RelationshipType, RelationshipConstraint[]>();
        const constraints: Constraint[] = [];

        // Właściwości z schema.attributes
        if (plugin.schema?.attributes || plugin.schema?.attributeDefinitions) {
            const attributes = (plugin.schema.attributes || plugin.schema.attributeDefinitions || []) as any[];
            attributes.forEach((attr: any) => {
                properties.set(attr.name, {
                    name: attr.name,
                    type: this.mapAttributeType(attr.type),
                    required: attr.required || false,
                    defaultValue: attr.defaultValue,
                    enumValues: attr.allowedValues,
                    description: attr.description
                });

                // Constraints dla wymaganych atrybutów
                if (attr.required) {
                    constraints.push({
                        type: 'required',
                        property: attr.name,
                        message: `Attribute '${attr.name}' is required for plugin '${plugin.id}'`
                    });
                }

                // Constraints dla enum values
                if (attr.allowedValues && attr.allowedValues.length > 0) {
                    constraints.push({
                        type: 'custom',
                        property: attr.name,
                        value: attr.allowedValues,
                        message: `Attribute '${attr.name}' must be one of: ${attr.allowedValues.join(', ')}`,
                        validator: (entity: OntologyEntity) => {
                            const value = entity.properties.get(attr.name);
                            return !value || attr.allowedValues.includes(value);
                        }
                    });
                }
            });
        }

        // Właściwości podstawowe
        properties.set('id', {
            name: 'id',
            type: 'string',
            required: true,
            description: 'Unique node identifier'
        });
        properties.set('type', {
            name: 'type',
            type: 'string',
            required: true,
            defaultValue: plugin.id,
            description: 'Plugin type'
        });
        properties.set('position', {
            name: 'position',
            type: 'object',
            required: false,
            description: 'Node position on canvas'
        });

        // Relationship constraints z semantics
        const semantics = (plugin.semantics || {}) as PluginSemanticsModel;
        
        // PARENT_OF / CHILD_OF relationships
        if (semantics.allowsChildren || semantics.acceptsAnyChild || (semantics.allowedChildTypes && semantics.allowedChildTypes.length > 0)) {
            const childTypes = semantics.acceptsAnyChild 
                ? ['Node'] as EntityType[]
                : (semantics.allowedChildTypes || []).map((t: string) => 'Node' as EntityType);
            
            allowedRelationships.set('PARENT_OF', [{
                relationshipType: 'PARENT_OF',
                targetType: childTypes,
                cardinality: {
                    min: 0,
                    max: -1 // unlimited
                },
                description: `Plugin '${plugin.id}' can be parent of: ${semantics.acceptsAnyChild ? 'any node' : semantics.allowedChildTypes?.join(', ') || 'none'}`
            }]);
        }

        // CHILD_OF relationship
        if (semantics.requiredParentTypes && semantics.requiredParentTypes.length > 0) {
            allowedRelationships.set('CHILD_OF', [{
                relationshipType: 'CHILD_OF',
                targetType: ['Node'] as EntityType[],
                cardinality: {
                    min: 1,
                    max: 1
                },
                description: `Plugin '${plugin.id}' must be child of: ${semantics.requiredParentTypes.join(', ')}`
            }]);

            // REQUIRES_PARENT relationship
            allowedRelationships.set('REQUIRES_PARENT', [{
                relationshipType: 'REQUIRES_PARENT',
                targetType: semantics.requiredParentTypes.map(() => 'Node' as EntityType),
                cardinality: {
                    min: 1,
                    max: 1
                },
                description: `Plugin '${plugin.id}' requires parent of type: ${semantics.requiredParentTypes.join(', ')}`
            }]);
        }

        // SEQUENTIAL_TO relationship (dla workflow flow)
        if (semantics.canAppearAtRoot !== false) {
            allowedRelationships.set('SEQUENTIAL_TO', [{
                relationshipType: 'SEQUENTIAL_TO',
                targetType: ['Node'] as EntityType[],
                cardinality: {
                    min: 0,
                    max: -1
                },
                description: `Plugin '${plugin.id}' can execute sequentially before other nodes`
            }]);
        }

        // DEFINES_VARIABLE / USES_VARIABLE relationships
        // Te będą inferowane z analizy zmiennych w runtime, ale możemy dodać constraints
        allowedRelationships.set('DEFINES_VARIABLE', [{
            relationshipType: 'DEFINES_VARIABLE',
            targetType: ['Variable'] as EntityType[],
            cardinality: {
                min: 0,
                max: -1
            },
            description: `Plugin '${plugin.id}' can define variables`
        }]);

        allowedRelationships.set('USES_VARIABLE', [{
            relationshipType: 'USES_VARIABLE',
            targetType: ['Variable'] as EntityType[],
            cardinality: {
                min: 0,
                max: -1
            },
            description: `Plugin '${plugin.id}' can use variables`
        }]);

        return {
            type: 'Plugin' as EntityType,
            properties,
            allowedRelationships,
            constraints
        };
    }

    /**
     * Zbuduj PluginOntologySchema z runtime metadata
     */
    private buildPluginOntologySchema(plugin: Plugin): PluginOntologySchema {
        const semantics = (plugin.semantics || {}) as PluginSemanticsModel;
        const schema = plugin.schema || {};

        return {
            pluginType: plugin.id,
            category: plugin.category || plugin.paletteGroup || 'Other',
            
            // Hierarchia z semantics
            canAppearAtRoot: (semantics.canAppearAtRoot !== false),
            requiredParentTypes: (semantics.requiredParentTypes || []),
            allowedChildTypes: (semantics.allowedChildTypes || []),
            acceptsAnyChild: (semantics.acceptsAnyChild || false),
            
            // Semantyka wykonania (inferowana z typu pluginu)
            executionSemantics: {
                isContainer: (semantics as any).allowsChildren || false,
                isLeaf: !((semantics as any).allowsChildren),
                executionOrder: this.inferExecutionOrder(plugin),
                canPause: true, // Wszystkie pluginy mogą być wstrzymane
                canResume: true
            },
            
            // Semantyka danych (z visual metadata jeśli dostępne)
            dataSemantics: {
                inputTypes: this.inferInputTypes(plugin),
                outputTypes: this.inferOutputTypes(plugin),
                transformsData: this.inferTransformsData(plugin),
                filtersData: this.inferFiltersData(plugin),
                aggregatesData: this.inferAggregatesData(plugin)
            },
            
            // Semantyka zmiennych (będzie aktualizowana w runtime)
            variableSemantics: {
                definesVariables: [], // Będzie wypełnione w runtime
                usesVariables: [], // Będzie wypełnione w runtime
                variableScope: ((semantics as any).requiredParentTypes && (semantics as any).requiredParentTypes.length > 0) ? 'parent' : 'root'
            },
            
            // Constraints z schema
            constraints: {
                minOccurs: 0,
                maxOccurs: -1, // unlimited
                requiredAttributes: (schema.attributes || schema.attributeDefinitions || [])
                    .filter((attr: any) => attr.required)
                    .map((attr: any) => attr.name),
                attributeConstraints: this.buildAttributeConstraints(schema)
            }
        };
    }

    /**
     * Zbuduj VisualOntologySchema z runtime metadata
     */
    private buildVisualOntologySchema(plugin: Plugin): VisualOntologySchema {
        return {
            position: {
                canMove: true,
                canResize: true,
                defaultSize: {
                    width: plugin.defaultWidth || 320,
                    height: plugin.defaultHeight || 160
                },
                minSize: {
                    width: 200,
                    height: 100
                },
                maxSize: {
                    width: 1000,
                    height: 500
                }
            },
            
            // Connection points będą budowane dynamicznie z visual metadata
            connectionPoints: {
                inputs: this.buildInputConnectionPoints(plugin),
                outputs: this.buildOutputConnectionPoints(plugin)
            },
            
            layout: {
                preferredLayout: this.inferPreferredLayout(plugin),
                canNest: (plugin.semantics?.allowsChildren || false),
                nestingDepth: -1 // unlimited
            },
            
            visualState: {
                canSelect: true,
                canHover: true,
                canExpand: (plugin.semantics?.allowsChildren || false),
                canCollapse: (plugin.semantics?.allowsChildren || false)
            }
        };
    }

    /**
     * Zbuduj RuntimeOntologySchema z runtime metadata
     */
    private buildRuntimeOntologySchema(plugin: Plugin): RuntimeOntologySchema {
        return {
            executionState: {
                canStart: true,
                canStop: true,
                canPause: true,
                canResume: true,
                executionTime: null, // Będzie wypełnione w runtime
                status: 'idle'
            },
            
            logging: {
                canLog: true,
                logLevels: ['DEBUG', 'INFO', 'WARN', 'ERROR'],
                logFormat: 'standard'
            },
            
            results: {
                producesResults: true, // Wszystkie pluginy mogą produkować wyniki
                resultTypes: this.inferResultTypes(plugin),
                resultFormat: this.inferResultFormat(plugin)
            },
            
            variables: {
                definesVariables: [], // Będzie wypełnione w runtime
                usesVariables: [], // Będzie wypełnione w runtime
                variableScope: ((plugin.semantics as any)?.requiredParentTypes && (plugin.semantics as any).requiredParentTypes.length > 0) ? 'parent' : 'root'
            }
        };
    }

    /**
     * Zbuduj Entity dla pluginu z runtime metadata
     */
    buildPluginEntity(plugin: Plugin, nodeId?: string): OntologyEntity {
        const properties = new Map<string, any>();
        properties.set('id', nodeId || plugin.id);
        properties.set('type', plugin.id);
        properties.set('name', plugin.name);
        properties.set('category', plugin.category || plugin.paletteGroup || 'Other');
        properties.set('description', plugin.description || '');
        
        if (plugin.schema?.attributes || plugin.schema?.attributeDefinitions) {
            const attributes = (plugin.schema.attributes || plugin.schema.attributeDefinitions || []) as any[];
            attributes.forEach((attr: any) => {
                if (attr.defaultValue !== undefined) {
                    properties.set(attr.name, attr.defaultValue);
                }
            });
        }

        return {
            id: nodeId || plugin.id,
            type: 'Plugin' as EntityType,
            properties,
            relationships: new Map(),
            metadata: {
                createdAt: Date.now(),
                updatedAt: Date.now(),
                version: '1.0.0',
                source: 'system'
            }
        };
    }

    // Helper methods

    private mapAttributeType(type: string | undefined): PropertySchema['type'] {
        if (!type) return 'string';
        const lower = type.toLowerCase();
        if (lower.includes('int') || lower.includes('number') || lower.includes('float')) return 'number';
        if (lower.includes('bool')) return 'boolean';
        if (lower.includes('array') || lower.includes('list')) return 'array';
        if (lower.includes('object') || lower.includes('map')) return 'object';
        return 'string';
    }

    private buildAttributeConstraints(schema: any): Map<string, any> {
        const constraints = new Map();
        const attributes = schema.attributes || schema.attributeDefinitions || [];
        
        attributes.forEach((attr: any) => {
            if (attr.allowedValues && attr.allowedValues.length > 0) {
                constraints.set(attr.name, {
                    type: 'enum',
                    value: attr.allowedValues,
                    message: `Value must be one of: ${attr.allowedValues.join(', ')}`
                });
            }
            if (attr.pattern) {
                constraints.set(attr.name, {
                    type: 'pattern',
                    pattern: attr.pattern,
                    message: `Value must match pattern: ${attr.pattern}`
                });
            }
        });
        
        return constraints;
    }

    private buildInputConnectionPoints(plugin: Plugin): any[] {
        // Z visual metadata jeśli dostępne (sprawdź różne możliwe struktury)
        const visual = plugin.visual as any;
        if (visual?.inputPorts) {
            return visual.inputPorts.map((port: any) => ({
                id: port.name,
                name: port.name,
                side: 'left' as const,
                type: 'input' as const,
                portType: port.type || 'STRING',
                required: port.required || false,
                multiple: false,
                compatibleTypes: ['STRING', 'XML', 'BINARY']
            }));
        }
        
        // Domyślny input port
        return [{
            id: 'input',
            name: 'Input',
            side: 'left' as const,
            type: 'input' as const,
            portType: 'STRING',
            required: false,
            multiple: false,
            compatibleTypes: ['STRING', 'XML', 'BINARY']
        }];
    }

    private buildOutputConnectionPoints(plugin: Plugin): any[] {
        // Z visual metadata jeśli dostępne (sprawdź różne możliwe struktury)
        const visual = plugin.visual as any;
        if (visual?.outputPorts) {
            return visual.outputPorts.map((port: any) => ({
                id: port.name,
                name: port.name,
                side: 'right' as const,
                type: 'output' as const,
                portType: port.type || 'STRING',
                required: false,
                multiple: true,
                compatibleTypes: ['STRING', 'XML', 'BINARY']
            }));
        }
        
        // Domyślny output port
        return [{
            id: 'output',
            name: 'Output',
            side: 'right' as const,
            type: 'output' as const,
            portType: 'STRING',
            required: false,
            multiple: true,
            compatibleTypes: ['STRING', 'XML', 'BINARY']
        }];
    }

    private inferExecutionOrder(plugin: Plugin): 'sequential' | 'parallel' | 'conditional' {
        const id = plugin.id.toLowerCase();
        if (id.includes('if') || id.includes('else') || id.includes('try') || id.includes('catch')) {
            return 'conditional';
        }
        if (id.includes('loop') || id.includes('while')) {
            return 'parallel'; // Może wykonywać się równolegle dla każdej iteracji
        }
        return 'sequential';
    }

    private inferInputTypes(plugin: Plugin): string[] {
        // Inferuj z typu pluginu
        const id = plugin.id.toLowerCase();
        if (id.includes('http') || id.includes('file')) {
            return ['URL', 'STRING'];
        }
        if (id.includes('xpath') || id.includes('xquery')) {
            return ['XML', 'STRING'];
        }
        return ['STRING', 'XML', 'BINARY'];
    }

    private inferOutputTypes(plugin: Plugin): string[] {
        const id = plugin.id.toLowerCase();
        if (id.includes('http') || id.includes('file')) {
            return ['STRING', 'XML', 'BINARY'];
        }
        if (id.includes('xpath') || id.includes('xquery')) {
            return ['XML', 'STRING'];
        }
        return ['STRING'];
    }

    private inferTransformsData(plugin: Plugin): boolean {
        const id = plugin.id.toLowerCase();
        return id.includes('transform') || id.includes('convert') || id.includes('xslt') || 
               id.includes('json-to-xml') || id.includes('xml-to-json');
    }

    private inferFiltersData(plugin: Plugin): boolean {
        const id = plugin.id.toLowerCase();
        return id.includes('filter') || id.includes('select') || id.includes('xpath');
    }

    private inferAggregatesData(plugin: Plugin): boolean {
        const id = plugin.id.toLowerCase();
        return id.includes('aggregate') || id.includes('group') || id.includes('sum');
    }

    private inferPreferredLayout(plugin: Plugin): 'vertical' | 'horizontal' | 'mindmap' | 'swimlane' {
        const semantics = plugin.semantics || {} as PluginSemanticsModel;
        if (semantics.allowsChildren) {
            return 'swimlane';
        }
        return 'vertical';
    }

    private inferResultTypes(plugin: Plugin): string[] {
        return this.inferOutputTypes(plugin);
    }

    private inferResultFormat(plugin: Plugin): 'string' | 'xml' | 'json' | 'binary' {
        const id = plugin.id.toLowerCase();
        if (id.includes('json')) return 'json';
        if (id.includes('xml') || id.includes('xpath') || id.includes('xquery')) return 'xml';
        if (id.includes('file') && id.includes('binary')) return 'binary';
        return 'string';
    }

    /**
     * Aktualizuj schematy gdy metadane się zmieniają
     */
    refreshSchemas(): void {
        this.buildAllSchemas();
    }
}

