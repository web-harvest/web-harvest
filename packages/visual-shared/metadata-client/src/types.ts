/**
 * Plugin metadata types owned by @webharvest/metadata-client.
 * Intentionally excludes IDE UI/execution types (tabs, NodeExecutionState, etc.).
 */

export type AttributeFieldKind =
    | 'TEXT'
    | 'TEXTAREA'
    | 'SELECT'
    | 'BOOLEAN'
    | 'EXPRESSION'
    | 'CODE'
    | 'NUMBER'
    | 'SLIDER'
    | 'URL'
    | 'JSON';

export interface AttributeUiMetadata {
    fieldKind?: AttributeFieldKind;
    order?: number;
    group?: string;
    helperText?: string;
    helperTextKey?: string;
    placeholder?: string;
    placeholderKey?: string;
    localizationKey?: string;
    format?: string;
    minLength?: number;
    maxLength?: number;
    expressionCapable?: boolean;
    expressionContext?: string;
    advanced?: boolean;
}

export interface UiToolDefinition {
    id: string;
    className?: string;
    label?: string;
    description?: string;
    icon?: string;
    placement?: string;
}

export interface PluginVisualUiBlock {
    version?: string;
    fields?: Record<string, AttributeUiMetadata>;
    tools?: UiToolDefinition[];
}

export interface MasterDetailConstraint {
    detailType: string;
    maxOccurs: number;
    minOccurs: number;
    sectionTitle?: string;
    addLabel?: string;
    emptyLabel?: string;
    primaryAttribute?: string;
    secondaryAttribute?: string;
    usesBody?: boolean;
}

export interface PluginVisualEnvelope {
    apiVersion: string;
    capabilities: string[];
    id: string;
    elementName: string;
    displayName: string;
    category: string;
    className: string;
    schemaVersion: string;
    helpUrl?: string;
    icon?: string;
    paletteGroup?: string;
    tags?: string[];
    ui?: PluginVisualUiBlock;
    masterDetailConstraints?: MasterDetailConstraint[];
}

export interface PluginDocumentationSummary {
    summary: string;
    url?: string;
    examples?: string[];
}

export interface PluginSemanticsModel {
    roles: Array<'ROOT_CONTAINER' | 'CONTEXT_CONTAINER' | 'BODY_EVALUATOR' | 'DEPENDENT' | 'LEAF'>;
    role?: 'ROOT_CONTAINER' | 'CONTEXT_CONTAINER' | 'BODY_EVALUATOR' | 'DEPENDENT' | 'LEAF';
    canAppearAtRoot: boolean;
    allowsChildren: boolean;
    acceptsAnyChild: boolean;
    allowBody: boolean;
    allowTextContent: boolean;
    allowedChildTypes: string[];
    requiredParentTypes: string[];
}

export interface InputPort {
    name: string;
    description?: string;
    type: string;
    required: boolean;
}

export interface OutputPort {
    name: string;
    description?: string;
    type: string;
}

export interface KeyboardShortcut {
    key: string;
    action: string;
    isGlobal: boolean;
}

export interface AccessibilityMetadata {
    ariaLabel?: string;
    ariaDescription?: string;
    keyboardNavigation?: string[];
}

export interface LinkedServiceMetadata {
    serviceType: string;
    serviceName: string;
    description?: string;
    required: boolean;
}

export interface PluginDetailSectionHint {
    type: string;
    title?: string;
    addLabel?: string;
    emptyLabel?: string;
    primaryAttribute?: string;
    secondaryAttribute?: string;
    summaryEmptyLabel?: string;
    usesBody?: boolean;
}

export interface PluginUIHints {
    paletteGroup?: string;
    tags?: string[];
    detailSections?: PluginDetailSectionHint[];
    fields?: Record<string, AttributeUiMetadata>;
    tools?: UiToolDefinition[];
}

export interface ParentChildRelationship {
    parentElementName: string;
    required: boolean;
    parentDisplayName?: string;
}

export interface PluginSchema {
    attributes: AttributeDefinition[];
    attributeDefinitions?: AttributeDefinition[];
    canHaveBody: boolean;
    canHaveChildren: boolean;
    canAppearAtRoot: boolean;
    requiredParents: string[];
    isContextSpecific: boolean;
    allowedSubElements?: string[];
    requiredSubElements?: string[];
    allowsBody?: boolean;
}

export interface AttributeDefinition {
    name: string;
    type: 'string' | 'number' | 'boolean' | 'url' | 'xpath' | 'enum' | 'integer' | 'long' | 'double' | 'file_path' | 'regex';
    required: boolean;
    defaultValue?: string;
    allowedValues?: string[];
    description?: string;
    minValue?: number;
    maxValue?: number;
    pattern?: string;
    ui?: AttributeUiMetadata;
}

export interface Plugin {
    id: string;
    name: string;
    description: string;
    category: string;
    icon: string;
    paletteGroup?: string;
    tags?: string[];
    schema: PluginSchema;
    semantics?: PluginSemanticsModel;
    helpUrl?: string;
    documentation?: PluginDocumentationSummary;
    dependencies?: string[];
    color?: string;
    group?: string;
    defaultWidth?: number;
    defaultHeight?: number;
    inputPorts?: InputPort[];
    outputPorts?: OutputPort[];
    portType?: string;
    tooltip?: string;
    keyboardShortcuts?: KeyboardShortcut[];
    accessibility?: AccessibilityMetadata;
    linkedServices?: LinkedServiceMetadata[];
    parentRelationship?: ParentChildRelationship;
    allowedChildPlugins?: string[];
    ui?: PluginUIHints;
    masterDetailConstraints?: MasterDetailConstraint[];
    visual?: PluginVisualEnvelope;
    visualApiVersion?: string;
    visualCapabilities?: string[];
    attributeUi?: Record<string, AttributeUiMetadata>;
    uiTools?: UiToolDefinition[];
}

/** Minimal lookup used by XML round-trip without depending on the IDE PluginManager. */
export interface PluginLookup {
    findPlugin(type: string): Plugin | undefined | null;
}
