import type {
    AttributeDefinition,
    AttributeUiMetadata,
    MasterDetailConstraint,
    Plugin,
    PluginDocumentationSummary,
    PluginSchema,
    PluginSemanticsModel,
    PluginUIHints,
    PluginVisualEnvelope,
    UiToolDefinition
} from '../../types';

export function normalizePlugin(plugin: any): Plugin {
    const semantics = normalizeSemantics(plugin.semantics);
    const visualEnvelope = normalizeVisualEnvelope(plugin.visual);
    const schema = normalizeSchema(plugin.schema);
    const visualApiVersion = visualEnvelope?.apiVersion ?? null;
    const visualCapabilities = Array.isArray(visualEnvelope?.capabilities)
        ? visualEnvelope!.capabilities
        : [];
    const category = plugin.category || visualEnvelope?.category || 'General';
    const icon = plugin.icon
        || visualEnvelope?.icon
        || plugin.ui?.icon
        || 'fa-puzzle-piece';
    const paletteGroup = plugin.paletteGroup
        || plugin.ui?.paletteGroup
        || visualEnvelope?.paletteGroup
        || category
        || 'General';
    const tags = pickTags(plugin, visualEnvelope, category);
    const helpUrl = plugin.helpUrl
        || plugin.documentation?.url
        || visualEnvelope?.helpUrl
        || null;
    const attributeUi = buildAttributeUiMap(
        schema,
        visualEnvelope?.ui?.fields as Record<string, AttributeUiMetadata> | undefined
    );
    const uiTools = normaliseUiTools(visualEnvelope?.ui?.tools);
    const uiHints = buildUiHints(plugin.ui, paletteGroup, tags, attributeUi, uiTools);
    const masterDetailConstraints = normaliseMasterDetailConstraints(
        plugin.masterDetailConstraints || visualEnvelope?.masterDetailConstraints
    );

    return {
        id: plugin.id,
        name: plugin.name || visualEnvelope?.displayName || plugin.elementName || plugin.id,
        description: plugin.description || '',
        category,
        icon,
        paletteGroup,
        tags,
        schema,
        semantics,
        helpUrl: helpUrl || undefined,
        documentation: buildDocumentation(plugin, helpUrl),
        dependencies: Array.isArray(plugin.dependencies) ? plugin.dependencies : [],
        color: plugin.color,
        group: plugin.group || paletteGroup,
        defaultWidth: plugin.defaultWidth,
        defaultHeight: plugin.defaultHeight,
        inputPorts: plugin.inputPorts,
        outputPorts: plugin.outputPorts,
        portType: plugin.portType,
        tooltip: plugin.tooltip,
        keyboardShortcuts: plugin.keyboardShortcuts,
        accessibility: plugin.accessibility,
        linkedServices: plugin.linkedServices,
        parentRelationship: plugin.parentRelationship,
        allowedChildPlugins: plugin.allowedChildPlugins,
        ui: uiHints,
        masterDetailConstraints,
        visual: visualEnvelope || undefined,
        visualApiVersion: visualApiVersion || undefined,
        visualCapabilities: visualCapabilities.length ? visualCapabilities : undefined,
        attributeUi: Object.keys(attributeUi).length ? attributeUi : undefined,
        uiTools: uiTools.length ? uiTools : undefined
    };
}

export function normalizeSemantics(raw: any): PluginSemanticsModel | undefined {
    if (!raw) {
        return undefined;
    }
    const clone: PluginSemanticsModel = { ...raw };
    if (!Array.isArray(clone.roles)) {
        const legacyRole = (clone as any).role;
        clone.roles = legacyRole ? [legacyRole] : [];
    }
    return clone;
}

export function normalizeVisualEnvelope(raw: any): PluginVisualEnvelope | undefined {
    if (!raw || typeof raw !== 'object') {
        return undefined;
    }
    const apiVersion = typeof raw.apiVersion === 'string' ? raw.apiVersion : '1.0';
    const capabilities = Array.isArray(raw.capabilities) ? raw.capabilities : [];
    return {
        apiVersion,
        capabilities,
        id: raw.id ?? '',
        elementName: raw.elementName ?? raw.id ?? '',
        displayName: raw.displayName ?? raw.name ?? raw.elementName ?? raw.id ?? '',
        category: raw.category ?? raw.paletteGroup ?? raw.group ?? 'General',
        className: raw.className ?? '',
        schemaVersion: raw.schemaVersion ?? 'unknown',
        helpUrl: raw.helpUrl,
        icon: raw.icon,
        paletteGroup: raw.paletteGroup,
        tags: Array.isArray(raw.tags) ? raw.tags : undefined,
        ui: raw.ui,
        masterDetailConstraints: normaliseMasterDetailConstraints(raw.masterDetailConstraints)
    };
}

export function normalizeSchema(raw: any): PluginSchema {
    if (!raw || typeof raw !== 'object') {
        return {
            attributes: [],
            canHaveBody: true,
            canHaveChildren: true,
            canAppearAtRoot: true,
            requiredParents: [],
            isContextSpecific: false
        };
    }
    const attributeList: any[] = raw.attributes || raw.attributeDefinitions || [];
    const attributes = attributeList.map(attr => ({
        name: attr.name,
        type: attr.type || 'string',
        required: Boolean(attr.required),
        defaultValue: attr.defaultValue,
        allowedValues: attr.allowedValues,
        description: attr.description,
        minValue: attr.minValue,
        maxValue: attr.maxValue,
        pattern: attr.pattern,
        ui: attr.ui
    }));
    return {
        attributes,
        attributeDefinitions: raw.attributeDefinitions,
        canHaveBody: raw.canHaveBody ?? raw.allowsBody ?? true,
        canHaveChildren: raw.canHaveChildren ?? (Array.isArray(raw.allowedSubElements) ? raw.allowedSubElements.length > 0 : true),
        canAppearAtRoot: raw.canAppearAtRoot ?? true,
        requiredParents: raw.requiredParents ?? [],
        isContextSpecific: raw.isContextSpecific ?? false,
        allowedSubElements: raw.allowedSubElements,
        requiredSubElements: raw.requiredSubElements,
        allowsBody: raw.allowsBody
    };
}

export function pickTags(plugin: any, visual: PluginVisualEnvelope | undefined, category: string): string[] {
    if (Array.isArray(plugin.tags) && plugin.tags.length) {
        return plugin.tags;
    }
    if (visual?.tags && visual.tags.length) {
        return visual.tags;
    }
    return [plugin.id, category].filter(Boolean);
}

export function buildDocumentation(plugin: any, helpUrl: string | null): PluginDocumentationSummary {
    if (plugin.documentation) {
        return {
            summary: plugin.documentation.summary || plugin.description || '',
            url: plugin.documentation.url || helpUrl || undefined,
            examples: plugin.documentation.examples || []
        };
    }
    return {
        summary: plugin.description || '',
        url: helpUrl || undefined,
        examples: []
    };
}

export function buildAttributeUiMap(
    schema: PluginSchema,
    visualFields?: Record<string, AttributeUiMetadata>
): Record<string, AttributeUiMetadata> {
    const map: Record<string, AttributeUiMetadata> = {};
    schema.attributes.forEach(attr => {
        if (attr.ui) {
            map[attr.name] = { ...attr.ui };
        }
    });
    if (visualFields) {
        Object.entries(visualFields).forEach(([name, ui]) => {
            map[name] = { ...(map[name] || {}), ...ui };
        });
    }
    return map;
}

export function normaliseUiTools(raw: any): UiToolDefinition[] {
    if (!Array.isArray(raw)) {
        return [];
    }
    return raw.map(tool => ({
        id: tool.id,
        className: tool.className,
        label: tool.label,
        description: tool.description,
        icon: tool.icon,
        placement: tool.placement
    }));
}

export function buildUiHints(
    legacyHints: PluginUIHints | undefined,
    paletteGroup: string,
    tags: string[],
    fields: Record<string, AttributeUiMetadata>,
    tools: UiToolDefinition[]
): PluginUIHints {
    return {
        paletteGroup: legacyHints?.paletteGroup || paletteGroup,
        tags: legacyHints?.tags || tags,
        detailSections: legacyHints?.detailSections,
        fields: Object.keys(fields).length ? fields : legacyHints?.fields,
        tools: tools.length ? tools : legacyHints?.tools
    };
}

export function normaliseMasterDetailConstraints(raw: any): MasterDetailConstraint[] | undefined {
    if (!Array.isArray(raw)) {
        return undefined;
    }
    return raw.map(constraint => ({
        detailType: constraint.detailType,
        maxOccurs: constraint.maxOccurs ?? -1,
        minOccurs: constraint.minOccurs ?? 0,
        sectionTitle: constraint.sectionTitle,
        addLabel: constraint.addLabel,
        emptyLabel: constraint.emptyLabel,
        primaryAttribute: constraint.primaryAttribute,
        secondaryAttribute: constraint.secondaryAttribute,
        usesBody: constraint.usesBody
    }));
}


