/**
 * Visual Semantic Model - Type Definitions
 * 
 * Model semantyczny warstwy wizualnej, budowany w czasie rzeczywistym.
 * Zawiera pełny stan UI: pozycje węzłów, connection points, routing paths, UI state, layout metadata.
 * 
 * Używany przez:
 * - VisualDesigner - do zarządzania stanem wizualnym
 * - Agent AI/LLM - do czytania i modyfikacji workflow przez API
 */

import type { Plugin, VisualNode } from '../types';

// ============================================================================
// Core Types
// ============================================================================

export interface Point {
    x: number;
    y: number;
}

export interface Size {
    width: number;
    height: number;
}

export interface Bounds {
    left: number;
    right: number;
    top: number;
    bottom: number;
    width: number;
    height: number;
    centerX: number;
    centerY: number;
}

export type Side = 'left' | 'right' | 'top' | 'bottom';

export type RoutingType = 'orthogonal' | 'bezier' | 'straight';

export type ConnectionType = 'sequential' | 'conditional' | 'error';

// ============================================================================
// Connection Points (Anchors/Ports)
// ============================================================================

export interface ConnectionPoint {
    id: string;
    name: string;
    side: Side;
    position: Point; // Względem node bounds (0,0 = top-left corner)
    type: 'input' | 'output';
    portType?: string; // STRING, XML, BINARY, etc.
    connected: boolean; // Czy ma połączenie
    description?: string;
    
    // NEW: Edge position metadata
    edgePosition: {
        isOnEdge: boolean; // Czy punkt jest dokładnie na krawędzi
        edgeTolerance: number; // Tolerancja (powinna być < 1px)
        edgeSide: Side; // Która krawędź
        edgeCoordinate: number; // Współrzędna na krawędzi (0-1, gdzie 0.5 = środek)
    };
    
    // NEW: Semantic model decision metadata - jedyne źródło prawdy o wyborze strony
    semanticSelection: {
        isSelected: boolean; // Czy ten punkt został wybrany przez model semantyczny dla jakiegoś połączenia
        selectedForConnections: string[]; // IDs połączeń, które używają tego punktu
        selectionReason: string; // Powód wyboru (np. "minimal path length", "opposite to incoming", "best clearance")
        priority: number; // Priorytet wyboru (wyższy = lepszy)
        spatialContext: {
            // Kontekst przestrzenny użyty do decyzji
            targetNodeId?: string; // Dla którego węzła docelowego został wybrany
            incomingSide?: Side; // Strona z której przychodzi połączenie (dla outputs)
            pathLength: number; // Długość ścieżki gdy używa tego punktu
            clearance: number; // Odległość od innych węzłów/połączeń
            avoidsCollisions: boolean; // Czy unika kolizji z innymi elementami
        };
    };
}

// ============================================================================
// Variable Dependencies
// ============================================================================

export interface VariableDependency {
    variableName: string;
    definedAt: string | null; // nodeId gdzie zmienna jest zdefiniowana
    usedAt: string[]; // nodeIds gdzie zmienna jest używana
    scope: string; // 'root' lub parent nodeId
    isValid: boolean; // Czy użycie jest po definicji
    errors?: string[]; // Błędy walidacji (np. użycie przed definicją)
}

// ============================================================================
// Visual Node State
// ============================================================================

export interface VisualNodeState {
    nodeId: string;
    pluginType: string;
    
    // Position & dimensions
    position: Point;
    dimensions: Size;
    computedBounds: Bounds;
    
    // Connection points (anchors/ports)
    connectionPoints: {
        inputs: ConnectionPoint[];
        outputs: ConnectionPoint[];
    };
    
    // Variables
    variables: {
        defined: string[]; // Zmienne zdefiniowane w tym węźle
        used: string[]; // Zmienne używane w tym węźle
    };
    
    // Hierarchy
    parentId: string | null;
    childrenIds: string[];
    depth: number;
    isRoot: boolean;
    
    // Visual state
    visualState: {
        selected: boolean;
        hovered: boolean;
        expanded: boolean; // Dla containers
        collapsed: boolean;
        dragging: boolean;
        resizing: boolean;
        error: boolean;
        warning: boolean;
    };
    
    // Layout metadata
    layoutMetadata: {
        layoutGroup?: string; // Dla swim lanes/groups
        zIndex: number;
        isManualPositioned: boolean;
    };
    
    // NEW: Spatial collision detection
    spatialCollisions: {
        overlapsWithNodes: string[]; // IDs of nodes this node overlaps with
        overlapsWithBounds: Bounds[]; // Bounds of overlapping nodes
        isOverlapping: boolean; // Quick check if any overlaps exist
        overlapArea: number; // Total overlap area in pixels²
        clearance: {
            left: number; // Distance to nearest node on left
            right: number; // Distance to nearest node on right
            top: number; // Distance to nearest node on top
            bottom: number; // Distance to nearest node on bottom
            min: number; // Minimum clearance in any direction
        };
    };
    
    // Rendering cache
    renderCache: {
        element: HTMLElement | null;
        lastRenderTime: number;
        needsRerender: boolean;
    };
    
    // Runtime execution state (dla LLM)
    runtimeState?: {
        executionId: string | null;
        status: 'idle' | 'running' | 'paused' | 'completed' | 'failed' | 'stopped';
        lastExecutedAt: number | null;
        executionTime: number | null; // ms
        logs: ExecutionLog[];
        variables: Map<string, string>; // Zmienne dostępne po wykonaniu
        errors: string[];
        warnings: string[];
    };
}

// ============================================================================
// Runtime Execution State
// ============================================================================

export interface ExecutionLog {
    timestamp: number;
    level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
    message: string;
    nodeId?: string; // Który węzeł wygenerował log
    executionId?: string;
}

export interface ExecutionState {
    executionId: string;
    configName: string;
    status: 'PENDING' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'STOPPED';
    progress: number; // 0-100
    startTime: number | null;
    endTime: number | null;
    duration: number | null; // ms
    
    // Session info
    session?: {
        sessionId: string;
        clientId: string;
        status: string;
        startTime: number;
        endTime: number | null;
        duration: number;
        elementsProcessed: number;
        processingRate: number;
    };
    
    // Token usage
    tokens?: {
        httpRequests: number;
        httpBytes: number;
        cpuTimeMs: number;
        memoryPeakBytes: number;
    };
    
    // Logs
    logs: ExecutionLog[];
    logCount: number;
    
    // Results
    results: Map<string, string>;
    resultCount: number;
    
    // Node-level execution state
    nodeStates: Map<string, NodeExecutionState>;
}

export interface NodeExecutionState {
    nodeId: string;
    status: 'idle' | 'running' | 'completed' | 'failed' | 'skipped';
    startTime: number | null;
    endTime: number | null;
    duration: number | null;
    logs: ExecutionLog[];
    variables: Map<string, string>; // Zmienne dostępne po wykonaniu tego węzła
    error: string | null;
}

// ============================================================================
// Visual Connection State
// ============================================================================

export interface VisualConnectionState {
    connectionId: string;
    fromNodeId: string;
    toNodeId: string;
    type: ConnectionType;
    
    // Routing path (płynne linie)
    path: ConnectionPathState;
    
    // Anchor points
    anchors: {
        from: {
            nodeId: string;
            connectionPointId: string;
            position: Point; // Absolutne
        };
        to: {
            nodeId: string;
            connectionPointId: string;
            position: Point; // Absolutne
        };
    };
    
    // Visual state
    visualState: {
        selected: boolean;
        hovered: boolean;
        highlighted: boolean; // Dla variable dependencies
        error: boolean;
    };
    
    // Rendering cache
    renderCache: {
        svgPath: SVGPathElement | null;
        lastRenderTime: number;
        needsRerender: boolean;
    };
    
    // NEW: Connection edge validation
    edgeValidation: {
        startPointOnEdge: boolean;
        endPointOnEdge: boolean;
        startPointTolerance: number; // px
        endPointTolerance: number; // px
        arrowPointsTowardsTarget: boolean;
    };
    
    // NEW: Extended spatial validation
    spatialValidation: {
        pathLength: number; // Total path length
        isValidPath: boolean; // Path is valid (no self-intersections, reasonable length)
        startNodeDistance: number; // Distance from start point to source node center
        endNodeDistance: number; // Distance from end point to target node center
        minClearance: number; // Minimum clearance from other nodes (px)
        routingQuality: 'excellent' | 'good' | 'acceptable' | 'poor'; // Quality of routing
    };
    
    // NEW: Path geometry analysis
    pathGeometry: {
        startPoint: Point; // Exact point where path starts (on source node edge)
        endPoint: Point; // Exact point where path ends (on target node edge)
        turningPoints: Point[]; // All turning/break points in the path
        segments: Array<{
            from: Point;
            to: Point;
            direction: Point; // Normalized direction vector
            length: number;
            angle: number; // Angle in radians
            isHorizontal: boolean;
            isVertical: boolean;
        }>;
        selfIntersections: Array<{
            point: Point;
            segment1Index: number;
            segment2Index: number;
        }>; // Points where path intersects itself
        crossesOtherConnections: Array<{
            connectionId: string;
            intersectionPoint: Point;
            thisSegmentIndex: number;
            otherSegmentIndex: number;
        }>; // Points where this path crosses other connections
        reversesDirection: boolean; // True if path contains a 180° turn (backtracks)
        maxDeviation: number; // Maximum deviation from direct line (for efficiency check)
    };
}

export interface BezierControlPoints {
    start: Point;
    control1: Point;
    control2: Point;
    end: Point;
}

// ============================================================================
// Plugin Semantic Info (z backend model)
// ============================================================================

export interface PluginSemanticInfo {
    id: string;
    name: string;
    category: string;
    description: string;
    
    // Semantics - co agent musi wiedzieć
    semantics: {
        canAppearAtRoot: boolean;
        allowsChildren: boolean;
        acceptsAnyChild: boolean;
        allowedChildTypes: string[];
        requiredParentTypes: string[];
        roles: string[]; // Full array of roles from backend (ROOT_CONTAINER, CONTEXT_CONTAINER, BODY_EVALUATOR, DEPENDENT, LEAF)
        allowsBody: boolean;
        allowTextContent: boolean;
        // Execution semantics (how plugin executes)
        executionSemantics?: {
            isContainer: boolean;
            isLeaf: boolean;
            executionOrder: 'sequential' | 'parallel' | 'conditional';
            canPause: boolean;
            canResume: boolean;
        };
        // Variable semantics (what variables plugin defines/uses)
        variableSemantics?: {
            definesVariables: string[]; // Variable names this plugin defines
            usesVariables: string[]; // Variable names this plugin uses
            variableScope: 'root' | 'parent'; // Scope of variables
        };
    };
    
    // Schema - jak skonfigurować
    schema: {
        attributes: Array<{
            name: string;
            type: string;
            required: boolean;
            description?: string;
            allowedValues?: string[];
            defaultValue?: string;
        }>;
        allowsBody: boolean;
    };
    
    // Visual hints
    visual: {
        icon?: string;
        color?: string;
        defaultWidth?: number;
        defaultHeight?: number;
        inputPorts?: Array<{
            name: string;
            description?: string;
            type: string;
            required: boolean;
        }>;
        outputPorts?: Array<{
            name: string;
            description?: string;
            type: string;
        }>;
        // Port compatibility matrix for LLM
        portCompatibility?: {
            [portType: string]: string[]; // Which port types are compatible
        };
    };
    
    // Master/detail constraints (for hierarchical plugins)
    masterDetailConstraints?: Array<{
        detailType: string;
        maxOccurs: number;
        minOccurs: number;
        sectionTitle?: string;
        addLabel?: string;
    }>;
}

// ============================================================================
// Layout State
// ============================================================================

export interface ViewportState {
    pan: Point;
    zoom: number;
    bounds: Size;
}

export interface NodePositionState {
    position: Point;
    dimensions: Size;
    depth: number;
    isRoot: boolean;
    parentId: string | null;
}

export interface ArrowMetadata {
    endPoint: Point; // Dokładny punkt końcowy na krawędzi
    direction: Point; // Kierunek strzałki (normalized vector)
    angle: number; // Kąt strzałki w radianach (0 = right, π/2 = down, π = left, -π/2 = up)
    pointsTowardsNode: boolean; // Czy strzałka wskazuje na węzeł
    lastSegmentDirection: Point; // Kierunek ostatniego segmentu
    
    // NEW: Extended spatial information
    spatialInfo: {
        arrowTipPosition: Point; // Absolute position of arrow tip
        arrowBaseCenter: Point; // Center of arrow base
        arrowSize: number; // Size of arrow in pixels
        distanceToTargetCenter: number; // Distance from arrow tip to target node center
        angleDegrees: number; // Angle in degrees (for easier reading)
        isHorizontal: boolean; // True if arrow is horizontal (angle ≈ 0 or π)
        isVertical: boolean; // True if arrow is vertical (angle ≈ π/2 or -π/2)
        quadrant: 1 | 2 | 3 | 4; // Quadrant: 1=right-down, 2=left-down, 3=left-up, 4=right-up
    };
}

export interface ConnectionPathState {
    points: Point[];
    routingType: RoutingType;
    bezierControlPoints?: BezierControlPoints[];
    
    // NEW: Arrow direction metadata
    arrowMetadata?: ArrowMetadata;
    
    // NEW: Spatial routing metadata - detailed path information
    spatialMetadata: {
        totalLength: number; // Total path length in pixels
        segmentCount: number; // Number of segments (path.length - 1)
        minDistanceToNode: number; // Minimum distance from path to any node
        crossesOtherConnections: boolean; // True if path crosses other connections
        boundingBox: Bounds; // Bounding box of entire path
        startSide: Side; // Side where path starts
        endSide: Side; // Side where path ends
        // Enhanced path information - CRITICAL for semantic model
        startPoint: Point; // Exact start point (on source node edge)
        endPoint: Point; // Exact end point (on target node edge)
        turningPointsCount: number; // Number of turning points (corners/breaks)
        turningPoints?: Point[]; // All turning points coordinates
        passesThroughNodes: string[]; // Node IDs that path passes through (collision)
        labelCollisions: Array<{ nodeId: string; point: Point }>; // Label areas that path intersects
        // Detailed segment information
        segments?: Array<{
            from: Point;
            to: Point;
            length: number;
            direction: Point; // Normalized direction vector
            angle: number; // Angle in radians
            isHorizontal: boolean;
            isVertical: boolean;
        }>;
        // Collision detection results
        hasCollisions?: boolean; // True if any collisions detected
        collisionDetails?: {
            nodeIntersections: string[];
            labelCollisions: Array<{ nodeId: string; point: Point }>;
            connectionCrossings: boolean;
        };
    };
}

// ============================================================================
// UI State
// ============================================================================

export interface UIState {
    selectedNodeIds: Set<string>;
    hoveredNodeId: string | null;
    expandedContainers: Set<string>;
    collapsedContainers: Set<string>;
    activeTool: 'select' | 'connect' | 'pan' | null;
    marqueeSelection: MarqueeState | null;
}

export interface MarqueeState {
    start: Point;
    end: Point;
    bounds: Bounds;
}

// ============================================================================
// Visual Semantic Model (Main Interface)
// ============================================================================

export interface VisualSemanticModel {
    version: string;
    schemaVersion: string | null; // Schema version from backend PluginMetadataCatalog
    timestamp: number;
    workflowId: string;
    
    // Backend semantic model (dla każdego pluginu)
    availablePlugins: Map<string, PluginSemanticInfo>;
    
    // Current workflow state
    workflow: {
        rootNodes: VisualNodeState[];
        allNodes: Map<string, VisualNodeState>;
        connections: Map<string, VisualConnectionState>;
        variableDependencies: Map<string, VariableDependency>;
    };
    
    // Visual layout state
    layout: {
        viewport: ViewportState;
        nodePositions: Map<string, NodePositionState>;
        connectionPaths: Map<string, ConnectionPathState>;
    };
    
    // UI state
    ui: UIState;
    
    // Runtime execution state (dla LLM)
    runtime?: {
        currentExecution: ExecutionState | null;
        executionHistory: ExecutionState[]; // Ostatnie wykonania
        playbackState: {
            isPlaying: boolean;
            currentLogIndex: number;
            playbackSpeed: number; // 1.0 = normal, 2.0 = 2x speed
        } | null;
    };
}

// ============================================================================
// LLM-friendly Format (JSON serializable)
// ============================================================================

/**
 * Format dla agenta AI/LLM - wszystkie Map i Set są konwertowane do Array/Object
 */
export interface VisualSemanticModelForLLM {
    version: string;
    schemaVersion: string | null; // Schema version from backend PluginMetadataCatalog
    timestamp: number;
    workflowId: string;
    
    // Available plugins (Map → Object)
    availablePlugins: {
        [pluginId: string]: PluginSemanticInfo;
    };
    
    // Current workflow state
    workflow: {
        rootNodes: VisualNodeState[];
        allNodes: {
            [nodeId: string]: VisualNodeState;
        };
        connections: {
            [connectionId: string]: VisualConnectionState;
        };
        variableDependencies: {
            [variableName: string]: VariableDependency;
        };
    };
    
    // Visual layout state
    layout: {
        viewport: ViewportState;
        nodePositions: {
            [nodeId: string]: NodePositionState;
        };
        connectionPaths: {
            [connectionId: string]: ConnectionPathState;
        };
    };
    
    // UI state (Set → Array)
    ui: {
        selectedNodeIds: string[];
        hoveredNodeId: string | null;
        expandedContainers: string[];
        collapsedContainers: string[];
        activeTool: 'select' | 'connect' | 'pan' | null;
        marqueeSelection: MarqueeState | null;
    };
    
    // Available actions for agent (parameters are string descriptions for LLM)
    availableActions: {
        addNode: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        updateNode: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        removeNode: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        connectNodes: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        disconnectNodes: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        moveNode: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        loadConfig: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        applyAutoLayout: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        startExecution: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        stopExecution: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        pauseExecution: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        resumeExecution: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        getExecutionLogs: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        getExecutionResults: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        playbackLogs: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
        stopPlayback: {
            description: string;
            parameters: Record<string, string>; // String descriptions for LLM
        };
    };
    
    // Runtime state (dla LLM)
    runtime?: {
        currentExecution: ExecutionState | null;
        executionHistory: ExecutionState[]; // Ostatnie wykonania
        playbackState: {
            isPlaying: boolean;
            currentLogIndex: number;
            playbackSpeed: number;
        } | null;
    };
}

// ============================================================================
// Agent Actions
// ============================================================================

export type AgentActionType = 
    | 'addNode' 
    | 'updateNode' 
    | 'removeNode' 
    | 'connectNodes' 
    | 'disconnectNodes' 
    | 'moveNode' 
    | 'loadConfig'
    | 'applyAutoLayout'
    | 'startExecution'
    | 'stopExecution'
    | 'pauseExecution'
    | 'resumeExecution'
    | 'getExecutionLogs'
    | 'getExecutionResults'
    | 'playbackLogs'
    | 'stopPlayback';

export interface AgentAction {
    type: AgentActionType;
    parameters: Record<string, any>;
    requestId?: string; // Dla tracking odpowiedzi
}

export interface ActionResult {
    success: boolean;
    nodeId?: string;
    connectionId?: string;
    error?: string;
    updatedModel?: VisualSemanticModelForLLM; // Zaktualizowany model po akcji
}

export interface AddNodeParams {
    pluginType: string;
    position?: Point;
    parentId?: string;
    attributes?: Record<string, string>;
}

export interface UpdateNodeParams {
    nodeId: string;
    attributes: Record<string, string>;
}

export interface RemoveNodeParams {
    nodeId: string;
}

export interface ConnectNodesParams {
    fromNodeId: string;
    toNodeId: string;
    type?: ConnectionType;
}

export interface DisconnectNodesParams {
    fromNodeId: string;
    toNodeId: string;
}

export interface MoveNodeParams {
    nodeId: string;
    position: Point;
}

export interface LoadConfigParams {
    xml: string; // XML configuration string
}

export interface ApplyAutoLayoutParams {
    layoutType: 'grid' | 'vertical' | 'horizontal'; // Typ layoutu do zastosowania
}

// ============================================================================
// Runtime Control Actions
// ============================================================================

export interface StartExecutionParams {
    configName?: string; // Opcjonalna nazwa konfiguracji
    waitForWebSocket?: boolean; // Czy czekać na WebSocket connection
}

export interface StopExecutionParams {
    executionId: string;
}

export interface PauseExecutionParams {
    executionId: string;
}

export interface ResumeExecutionParams {
    executionId: string;
}

export interface GetExecutionLogsParams {
    executionId: string;
    fromIndex?: number; // Od którego indeksu logów
    limit?: number; // Maksymalna liczba logów
    level?: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'; // Filtr poziomu
    nodeId?: string; // Filtr dla konkretnego węzła
}

export interface GetExecutionResultsParams {
    executionId: string;
    variableNames?: string[]; // Konkretne zmienne, null = wszystkie
}

export interface PlaybackLogsParams {
    executionId: string;
    fromIndex?: number;
    toIndex?: number;
    speed?: number; // 1.0 = normal, 2.0 = 2x speed
    onLog?: (log: ExecutionLog) => void; // Callback dla każdego loga
}

export interface StopPlaybackParams {
    // Brak parametrów - zatrzymuje aktualny playback
}

