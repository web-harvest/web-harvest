/**
 * Core type definitions for WebHarvest IDE
 */

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
    sessionInfo?: SessionInfo;
    tokenUsage?: TokenUsage;
    visualNodes?: VisualNode[];
    activeSidebarPanel?: string;
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
}

export interface Plugin {
    id: string;
    name: string;
    description: string;
    category: string;
    icon: string;
    schema: PluginSchema;
    helpUrl?: string;
}

export interface PluginSchema {
    attributes: AttributeDefinition[];
    canHaveBody: boolean;
    canHaveChildren: boolean;
    canAppearAtRoot: boolean;
    requiredParents: string[];
    isContextSpecific: boolean;
}

export interface AttributeDefinition {
    name: string;
    type: 'string' | 'number' | 'boolean' | 'url' | 'xpath' | 'enum';
    required: boolean;
    defaultValue?: string;
    allowedValues?: string[];
    description?: string;
}

// ============================================================================
// Execution & WebSocket
// ============================================================================

export interface ExecutionUpdate {
    status: ExecutionStatus;
    progress: number;
    message: string;
    results?: ExecutionResult[];
    variables?: Record<string, any>;
    sessionInfo?: SessionInfo;
    tokenUsage?: TokenUsage;
    error?: string;
}

export type ExecutionStatus =
    | 'STARTED'
    | 'RUNNING'
    | 'PAUSED'
    | 'RESUMED'
    | 'COMPLETED'
    | 'ERROR'
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

export interface ExecutionEvent {
    type: 'start' | 'stop' | 'pause' | 'resume';
    executionId?: string;
}

export interface ViewModeEvent {
    type: 'switch';
    mode: ViewMode;
}

