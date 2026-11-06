/**
 * Core type definitions for WebHarvest IDE
 *
 * Plugin metadata and VisualNode ownership lives in @webharvest/* packages.
 * This module re-exports those contracts and adds IDE-only UI/execution types.
 */

import type { NodeExecutionState } from '../visual/NodeStateRenderer';

export type {
    AttributeFieldKind,
    AttributeUiMetadata,
    UiToolDefinition,
    PluginVisualUiBlock,
    PluginVisualEnvelope,
    PluginDocumentationSummary,
    PluginSemanticsModel,
    MasterDetailConstraint,
    Plugin,
    InputPort,
    OutputPort,
    KeyboardShortcut,
    AccessibilityMetadata,
    LinkedServiceMetadata,
    PluginDetailSectionHint,
    PluginUIHints,
    ParentChildRelationship,
    PluginSchema,
    AttributeDefinition,
    PluginLookup
} from '@webharvest/metadata-client';

export type { VisualNode } from '@webharvest/xml-roundtrip';

import type { Plugin } from '@webharvest/metadata-client';
import type { VisualNode } from '@webharvest/xml-roundtrip';

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
    sessionInfo?: SessionInfo;
    tokenUsage?: TokenUsage;
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

