/**
 * Core type definitions for WebHarvest IDE
 */

import type { NodeExecutionState } from '../visual/NodeStateRenderer';

// ============================================================================
// Configuration & Tabs
// ============================================================================

export interface Tab {
    id: string;
    name: string;
    content: string;
    isDirty: boolean;
    filePath?: string;
    executionId?: string;
    // v2.2: Per-tab execution data (like old app.js)
    results?: Record<string, any>;
    visualNodes?: VisualNode[];
    activeSidebarPanel?: string;
    runPanelState?: RunPanelState;
}

// ============================================================================
// Visual Designer
// ============================================================================

export interface VisualNode {
    id: string;
    type: string;
    attributes: Record<string, string>;
    body: string | null;
    children: VisualNode[];
    position?: { x: number; y: number };
    dimensions?: { width: number; height: number }; // Custom node dimensions (for resize)
    isManualPositioned?: boolean; // True when user has manually repositioned the node
}

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

export interface MasterDetailConstraint {
    detailType: string;
    maxOccurs: number; // -1 = unlimited
    minOccurs: number;
    sectionTitle?: string;
    addLabel?: string;
    emptyLabel?: string;
    primaryAttribute?: string;
    secondaryAttribute?: string;
    usesBody?: boolean;
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
    
    // VisualPluginMetadata fields (from generated JSON)
    color?: string; // Hex color for category (e.g., "#3B82F6")
    group?: string; // Visual group name (e.g., "HTTP & Network")
    defaultWidth?: number; // Default node width in canvas
    defaultHeight?: number; // Default node height in canvas
    inputPorts?: InputPort[]; // Input ports (NiFi-style)
    outputPorts?: OutputPort[]; // Output ports (NiFi-style)
    portType?: string; // Data type: STRING, XML, BINARY, MIXED, etc.
    tooltip?: string; // Extended tooltip for UI
    keyboardShortcuts?: KeyboardShortcut[]; // Apple HIG shortcuts
    accessibility?: AccessibilityMetadata; // Screen reader support
    linkedServices?: LinkedServiceMetadata[]; // Azure Data Factory style
    parentRelationship?: ParentChildRelationship; // For detail plugins
    allowedChildPlugins?: string[]; // For master plugins
    ui?: PluginUIHints;
    masterDetailConstraints?: MasterDetailConstraint[]; // Master/detail relationship constraints
    visual?: PluginVisualEnvelope;
    visualApiVersion?: string;
    visualCapabilities?: string[];
    attributeUi?: Record<string, AttributeUiMetadata>;
    uiTools?: UiToolDefinition[];
}

export interface InputPort {
    name: string;
    description?: string;
    type: string; // STRING, XML, BINARY, etc.
    required: boolean;
}

export interface OutputPort {
    name: string;
    description?: string;
    type: string; // STRING, XML, BINARY, etc.
}

export interface KeyboardShortcut {
    key: string; // e.g., "Cmd+N"
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
    type: 'string' | 'number' | 'boolean' | 'url' | 'xpath' | 'enum';
    required: boolean;
    defaultValue?: string;
    allowedValues?: string[];
    description?: string;
    minValue?: number;
    maxValue?: number;
    pattern?: string;
    ui?: AttributeUiMetadata;
}

// ============================================================================
// Execution & WebSocket
// ============================================================================

export interface NodeExecutionMetrics {
    dataProcessed?: number;
    httpRequests?: number;
    memoryUsed?: number;
}

export interface NodeExecutionUpdate {
    nodeId: string;
    pluginType: string;
    status: NodeExecutionState;
    progress?: number;
    startTime?: number;
    endTime?: number;
    metrics?: NodeExecutionMetrics;
    variables?: Record<string, any>;
    error?: {
        message: string;
        stackTrace: string;
    };
}

export interface ExecutionUpdate {
    status: ExecutionStatus;
    progress: number;
    message: string;
    results?: ExecutionResult[];
    variables?: Record<string, any>;
    sessionInfo?: SessionInfo;
    tokenUsage?: TokenUsage;
    nodeUpdates?: NodeExecutionUpdate[];
    error?: string;
}

export type ExecutionStatus =
    | 'STARTED'
    | 'RUNNING'
    | 'PAUSED'
    | 'RESUMED'
    | 'COMPLETED'
    | 'ERROR'
    | 'FAILED'
    | 'STOPPED';

export interface ExecutionResult {
    id: string;
    name: string;
    value: any;
    type: string;
    timestamp: number;
}

export interface SessionInfo {
    sessionId: string;
    clientId?: string;
    startTime?: number;
    endTime?: number;
    duration?: number;
    status?: string;
    elementsProcessed?: number;
    processingRate?: number;
    httpMetrics?: {
        requests: number;
        bytesDownloaded: number;
        bytesUploaded: number;
        avgResponseTime: number;
    };
    pluginBreakdown?: Record<string, number>;
}

export interface TokenUsage {
    created?: number;
    consumed?: number;
    active?: number;
    httpRequests?: number;
    httpBytes?: number;
    cpuTimeMs?: number;
    memoryPeakBytes?: number;
}

// ============================================================================
// Logs
// ============================================================================

export interface LogEntry {
    id: string;
    type: LogType;
    message: string;
    timestamp: number;
}

export type LogType = 'info' | 'warning' | 'error' | 'success' | 'debug';

// ============================================================================
// API Responses
// ============================================================================

export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: string;
}

export interface PluginsResponse {
    plugins: Plugin[];
}

export interface ExecutionResponse {
    executionId: string;
    wsUrl: string;
}

export interface ValidationResponse {
    valid: boolean;
    errors?: ValidationError[];
}

export interface ValidationError {
    line: number;
    column: number;
    message: string;
    severity: 'error' | 'warning';
}

// ============================================================================
// UI State
// ============================================================================

export type ViewMode = 'xml' | 'visual';

export interface AppState {
    tabs: Tab[];
    activeTabId: string | null;
    viewMode: ViewMode;
    plugins: Plugin[];
    isExecuting: boolean;
    executionId: string | null;
    webSocket: WebSocket | null;
}

// ============================================================================
// Events
// ============================================================================

export interface TabEvent {
    type: 'add' | 'remove' | 'switch' | 'update';
    tabId: string;
    tab?: Tab;
}

// ============================================================================
// Run/Validate Panel
// ============================================================================

export interface RunPanelPayload {
    inputJson: string;
    correlationId?: string;
    idempotencyKey?: string;
    taskDomainJson?: string;
}

export type RunHistoryScope = 'workflow' | 'task';

export type RunHistoryStatus =
    | 'QUEUED'
    | 'RUNNING'
    | 'COMPLETED'
    | 'FAILED'
    | 'STOPPED'
    | 'CANCELLED';

export interface RunHistoryEntry {
    id: string;
    createdAt: number;
    scope: RunHistoryScope;
    payload: RunPanelPayload;
    executionId?: string;
    nodeId?: string;
    nodeLabel?: string;
    status?: RunHistoryStatus;
    sessionInfo?: SessionInfo;
    tokenUsage?: TokenUsage;
}

export interface RunPanelState extends RunPanelPayload {
    history: RunHistoryEntry[];
}

export interface ExecutionEvent {
    type: 'start' | 'stop' | 'pause' | 'resume';
    executionId?: string;
}

export interface ViewModeEvent {
    type: 'switch';
    mode: ViewMode;
}

