import type {
    VisualNode,
    Plugin,
    PluginDetailSectionHint,
    AttributeDefinition,
    MasterDetailConstraint
} from '../../types';
import type { PluginManager } from '../../core/PluginManager';

export interface DetailSectionDefinition {
    readonly detailType: string;
    readonly childPlugin: Plugin;
    readonly sectionTitle: string;
    readonly addLabel: string;
    readonly emptyLabel: string;
    readonly primaryAttribute?: string;
    readonly secondaryAttribute?: string;
    readonly usesBody: boolean;
    readonly summaryEmptyLabel: string;
    readonly primaryAttributeLabel?: string;
    readonly secondaryAttributeLabel?: string;
    readonly primaryAttributeDescription?: string;
    readonly secondaryAttributeDescription?: string;
    readonly attributeDefinitions: AttributeDefinition[];
    readonly maxOccurs: number; // -1 = unlimited
    readonly minOccurs: number;
}

export interface DetailSummary {
    primaryText: string;
    secondaryText?: string;
    isEmpty: boolean;
}

export interface PartitionedDetails {
    sections: Record<string, VisualNode[]>;
    unmatched: VisualNode[];
}

type DetailSectionOverride = Partial<Omit<DetailSectionDefinition, 'detailType' | 'childPlugin'>> & {
    primaryAttribute?: string;
    secondaryAttribute?: string;
};

const DETAIL_OVERRIDES: Record<string, DetailSectionOverride> = {
    'http-header': {
        sectionTitle: 'Headers',
        addLabel: 'Add Header',
        emptyLabel: 'No headers defined yet.',
        primaryAttribute: 'name',
        secondaryAttribute: 'value',
        summaryEmptyLabel: 'Unnamed header'
    },
    'http-param': {
        sectionTitle: 'Query Parameters',
        addLabel: 'Add Parameter',
        emptyLabel: 'No query parameters defined yet.',
        primaryAttribute: 'name',
        secondaryAttribute: 'value',
        summaryEmptyLabel: 'Unnamed parameter'
    },
    'call-param': {
        sectionTitle: 'Call Parameters',
        addLabel: 'Add Parameter',
        emptyLabel: 'No call parameters defined yet.',
        primaryAttribute: 'name',
        secondaryAttribute: 'value',
        summaryEmptyLabel: 'Unnamed parameter'
    }
};

const SUMMARY_PRIMARY_PREFERENCE = ['name', 'id', 'key', 'header', 'path'];
const SUMMARY_SECONDARY_PREFERENCE = ['value', 'expression', 'select'];

export class MasterDetailRegistry {
    private readonly configCache = new Map<string, DetailSectionDefinition[]>();
    private readonly definitionByType = new Map<string, DetailSectionDefinition>();

    constructor(private readonly pluginManager: PluginManager) {}

    getDetailDefinition(detailType: string): DetailSectionDefinition | null {
        return this.definitionByType.get(detailType) ?? null;
    }

    isMasterDetailPlugin(pluginType: string): boolean {
        return this.getSections(pluginType).length > 0;
    }

    getSections(pluginType: string): DetailSectionDefinition[] {
        if (this.configCache.has(pluginType)) {
            return this.configCache.get(pluginType)!;
        }

        const plugin = this.pluginManager.findPlugin(pluginType);
        if (!plugin) {
            this.configCache.set(pluginType, []);
            return [];
        }

        // Get constraints from metadata annotations
        const constraints = this.getConstraintsForMaster(pluginType);
        const constraintsMap = new Map<string, MasterDetailConstraint>();
        constraints.forEach(constraint => {
            constraintsMap.set(constraint.detailType, constraint);
        });

        const candidateTypes = collectAllowedDetailTypes(plugin);
        const uiOverrides = buildUiOverrides(plugin.ui?.detailSections);
        const sections: DetailSectionDefinition[] = [];

        candidateTypes.forEach(childType => {
            const childPlugin = this.pluginManager.findPlugin(childType);
            if (!childPlugin) {
                return;
            }

            if (!isDetailPlugin(childPlugin, pluginType)) {
                return;
            }

            const constraint = constraintsMap.get(childType);
            const definition = this.buildSectionDefinition(childPlugin, uiOverrides.get(childType), constraint);
            sections.push(definition);
            this.definitionByType.set(definition.detailType, definition);
        });

        sections.sort((a, b) => a.sectionTitle.localeCompare(b.sectionTitle));

        this.configCache.set(pluginType, sections);
        return sections;
    }

    /**
     * Get master/detail constraints for a master plugin type.
     * Returns constraints defined via @MasterDetailConstraint annotations.
     */
    getConstraintsForMaster(masterType: string): MasterDetailConstraint[] {
        const plugin = this.pluginManager.findPlugin(masterType);
        if (!plugin || !plugin.masterDetailConstraints) {
            return [];
        }
        return plugin.masterDetailConstraints;
    }

    createDetailNode(detailType: string): VisualNode | null {
        const definition = this.definitionByType.get(detailType) || this.buildSectionForType(detailType);
        if (!definition) {
            return null;
        }

        const schema = definition.childPlugin.schema;
        const supportsBody = definition.usesBody || Boolean(schema?.canHaveBody);
        const attributes: Record<string, string> = {};

        if (schema?.attributes) {
            schema.attributes.forEach(attr => {
                const key = attr.name;
                if (!key) {
                    return;
                }
                if (attr.type === 'boolean') {
                    attributes[key] = (attr.defaultValue ?? 'false').toString();
                } else if (attr.defaultValue !== undefined && attr.defaultValue !== null) {
                    attributes[key] = attr.defaultValue.toString();
                } else {
                    attributes[key] = '';
                }
            });
        }

        const body = supportsBody ? '' : null;

        return {
            id: generateDetailId(detailType),
            type: detailType,
            attributes,
            body,
            children: []
        };
    }

    summarizeDetail(detail: VisualNode): DetailSummary {
        const definition = this.definitionByType.get(detail.type) || this.buildSectionForType(detail.type);
        if (!definition) {
            return {
                primaryText: detail.type,
                isEmpty: false
            };
        }

        const primaryValue = definition.primaryAttribute
            ? (detail.attributes?.[definition.primaryAttribute] ?? '').toString().trim()
            : '';

        let secondaryValue = '';
        if (definition.usesBody) {
            secondaryValue = (detail.body ?? '').toString().trim();
        } else if (definition.secondaryAttribute) {
            secondaryValue = (detail.attributes?.[definition.secondaryAttribute] ?? '').toString().trim();
        }

        const hasPrimary = primaryValue.length > 0;
        const hasSecondary = secondaryValue.length > 0;

        const primaryText = hasPrimary
            ? primaryValue
            : hasSecondary
                ? secondaryValue
                : definition.summaryEmptyLabel;

        return {
            primaryText,
            secondaryText: hasPrimary && hasSecondary ? secondaryValue : undefined,
            isEmpty: !hasPrimary && !hasSecondary
        };
    }

    partitionChildren(masterNode: VisualNode): PartitionedDetails {
        const sections: Record<string, VisualNode[]> = {};
        const unmatched: VisualNode[] = [];

        (masterNode.children || []).forEach(child => {
            const definition = this.definitionByType.get(child.type) || this.buildSectionForType(child.type);
            if (definition) {
                if (!sections[definition.detailType]) {
                    sections[definition.detailType] = [];
                }
                sections[definition.detailType].push(child);
            } else {
                unmatched.push(child);
            }
        });

        return { sections, unmatched };
    }

    private buildSectionForType(detailType: string): DetailSectionDefinition | null {
        const childPlugin = this.pluginManager.findPlugin(detailType);
        if (!childPlugin) {
            return null;
        }
        const definition = this.buildSectionDefinition(childPlugin);
        this.definitionByType.set(definition.detailType, definition);
        return definition;
    }

    private buildSectionDefinition(
        childPlugin: Plugin,
        uiOverride?: DetailSectionOverride,
        constraint?: MasterDetailConstraint
    ): DetailSectionDefinition {
        const detailType = childPlugin.id;
        const override = {
            ...DETAIL_OVERRIDES[detailType],
            ...uiOverride
        };
        const schema = childPlugin.schema;

        const primaryAttribute = override.primaryAttribute ?? selectPrimaryAttribute(schema?.attributes);
        const secondaryAttribute = override.secondaryAttribute ?? selectSecondaryAttribute(schema?.attributes, primaryAttribute);
        const usesBody = override.usesBody ?? Boolean(schema?.canHaveBody);

        const sectionTitle = override.sectionTitle ?? pluralize(childPlugin.name);
        const addLabel = override.addLabel ?? `Add ${childPlugin.name}`;
        const emptyLabel = override.emptyLabel ?? `No ${sectionTitle.toLowerCase()} defined yet.`;
        const summaryEmptyLabel = override.summaryEmptyLabel ?? 'Not configured';

        const attributeDefinitions = Array.isArray(schema?.attributes)
            ? schema!.attributes.map(attribute => ({ ...attribute }))
            : [];

        const primaryMeta = attributeDefinitions.find(attr => attr.name === primaryAttribute);
        const secondaryMeta = attributeDefinitions.find(attr => attr.name === secondaryAttribute);

        // Use constraint values if available, otherwise defaults
        const maxOccurs = constraint?.maxOccurs ?? -1; // -1 = unlimited
        const minOccurs = constraint?.minOccurs ?? 0;
        
        // Override section title and add label from constraint if provided
        const finalSectionTitle = constraint?.sectionTitle ?? sectionTitle;
        const finalAddLabel = constraint?.addLabel ?? addLabel;

        return {
            detailType,
            childPlugin,
            sectionTitle: finalSectionTitle,
            addLabel: finalAddLabel,
            emptyLabel,
            primaryAttribute,
            secondaryAttribute,
            usesBody,
            summaryEmptyLabel,
            primaryAttributeLabel: primaryMeta?.name ? formatAttributeLabel(primaryMeta.name) : undefined,
            secondaryAttributeLabel: secondaryMeta?.name ? formatAttributeLabel(secondaryMeta.name) : undefined,
            primaryAttributeDescription: primaryMeta?.description,
            secondaryAttributeDescription: secondaryMeta?.description,
            attributeDefinitions,
            maxOccurs,
            minOccurs
        };
    }
}

function selectPrimaryAttribute(attributes?: Array<{ name: string }>): string | undefined {
    if (!attributes || attributes.length === 0) {
        return undefined;
    }

    const lookup = new Map<string, string>();
    attributes.forEach(attr => {
        if (!attr?.name) {
            return;
        }
        lookup.set(attr.name.toLowerCase(), attr.name);
    });

    for (const key of SUMMARY_PRIMARY_PREFERENCE) {
        const existing = lookup.get(key);
        if (existing) {
            return existing;
        }
    }

    return attributes[0]?.name;
}

function selectSecondaryAttribute(
    attributes: Array<{ name: string }> | undefined,
    primaryAttribute?: string
): string | undefined {
    if (!attributes || attributes.length === 0) {
        return undefined;
    }

    const lookup = new Map<string, string>();
    attributes.forEach(attr => {
        if (!attr?.name) {
            return;
        }
        if (attr.name === primaryAttribute) {
            return;
        }
        lookup.set(attr.name.toLowerCase(), attr.name);
    });

    for (const key of SUMMARY_SECONDARY_PREFERENCE) {
        const existing = lookup.get(key);
        if (existing) {
            return existing;
        }
    }

    return undefined;
}

function pluralize(label: string): string {
    if (!label) {
        return 'Details';
    }

    if (label.endsWith('y') && !/[aeiou]y$/i.test(label)) {
        return `${label.slice(0, -1)}ies`;
    }

    if (label.endsWith('s')) {
        return label;
    }

    return `${label}s`;
}

function generateDetailId(prefix: string): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `${prefix}-${crypto.randomUUID()}`;
    }
    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function buildUiOverrides(hints?: PluginDetailSectionHint[] | null): Map<string, DetailSectionOverride> {
    const map = new Map<string, DetailSectionOverride>();
    if (!Array.isArray(hints)) {
        return map;
    }
    hints.forEach(hint => {
        if (!hint || !hint.type) {
            return;
        }
        map.set(hint.type, {
            sectionTitle: hint.title,
            addLabel: hint.addLabel,
            emptyLabel: hint.emptyLabel,
            primaryAttribute: hint.primaryAttribute,
            secondaryAttribute: hint.secondaryAttribute,
            summaryEmptyLabel: hint.summaryEmptyLabel,
            usesBody: hint.usesBody
        });
    });
    return map;
}

function formatAttributeLabel(name: string): string {
    if (!name) {
        return '';
    }

    const cleaned = name
        .replace(/[-_]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();

    return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
}

function collectAllowedDetailTypes(plugin: Plugin): Set<string> {
    const set = new Set<string>();

    if (Array.isArray(plugin.allowedChildPlugins)) {
        plugin.allowedChildPlugins.forEach(type => type && set.add(type));
    }

    if (Array.isArray(plugin.semantics?.allowedChildTypes)) {
        plugin.semantics!.allowedChildTypes.forEach(type => type && set.add(type));
    }

    if (Array.isArray(plugin.schema?.allowedSubElements)) {
        plugin.schema!.allowedSubElements!.forEach(type => type && set.add(type));
    }

    return set;
}

function isDetailPlugin(plugin: Plugin, parentType: string): boolean {
    const roles = Array.isArray(plugin.semantics?.roles) ? plugin.semantics!.roles : [];
    const requiredParents = plugin.semantics?.requiredParentTypes || plugin.schema?.requiredParents || [];

    const isDependentRole = roles.includes('DEPENDENT');
    const explicitlyRequiresParent = requiredParents.includes(parentType);

    return isDependentRole || explicitlyRequiresParent;
}
