/**
 * VisualSemanticModelManager - Zarządzanie Visual Semantic Model
 * 
 * Buduje model semantyczny w czasie rzeczywistym na podstawie stanu VisualDesigner.
 * Zawiera:
 * - Pozycje węzłów, connection points, routing paths
 * - UI state (selected, hovered, expanded)
 * - Layout metadata
 * - Variable dependencies
 * 
 * Używa systemu subskrypcji dla real-time updates.
 */

import type { VisualDesigner } from './VisualDesigner';
import type { PluginManager } from '../core/PluginManager';
import type { VisualNode, Plugin } from '../types';
import type {
    VisualSemanticModel,
    VisualSemanticModelForLLM,
    VisualNodeState,
    VisualConnectionState,
    VariableDependency,
    PluginSemanticInfo,
    ViewportState,
    NodePositionState,
    ConnectionPathState,
    UIState,
    ConnectionPoint,
    Point,
    Bounds,
    Side
} from './VisualSemanticModel';
import type { Relationship, EntityType } from './OntologyModel';
import { VariableDependencyAnalyzer } from './VariableDependencyAnalyzer';
import { ConnectionPointsCalculator } from './ConnectionPointsCalculator';
import { SimpleConnectionRouter } from './SimpleConnectionRouter';
import { getAnchorSvgPosition, type Point as CoordPoint } from './CoordinateUtils';
import { SpatialCollisionDetector } from './SpatialCollisionDetector';
import { OntologyRepositoryImpl } from './OntologyRepositoryImpl';
import { OntologyInferenceEngineImpl } from './OntologyInferenceEngineImpl';
import { OntologyBuilder } from './OntologyBuilder';
import type { UnifiedOntologyModel, OntologyRepository, OntologyInferenceEngine } from './OntologyModel';
import { generateConnections, type Connection } from './VisualModelHelpers';
import { planConnections } from './connections/ConnectionPlanner';
import { VisualModelWebSocketManager } from '../core/VisualModelWebSocketManager';

export class VisualSemanticModelManager {
    /**
     * Resolve plugin metadata across the current PluginManager API and legacy
     * integrations that still expose getPlugin(). The IDE has older adapters
     * and test fixtures that predate findPlugin()/getSchemaVersion().
     */
    private findPlugin(pluginId: string) {
        const manager = this.pluginManager as PluginManager & {
            getPlugin?: (id: string) => Plugin | null;
        };

        if (typeof manager.findPlugin === 'function') {
            return manager.findPlugin(pluginId);
        }
        if (typeof manager.getPlugin === 'function') {
            return manager.getPlugin(pluginId);
        }
        return manager.getPlugins().find(plugin => plugin.id === pluginId) ?? null;
    }

    private getPluginSchemaVersion(): string | null {
        const manager = this.pluginManager as PluginManager & {
            getSchemaVersion?: () => string | null;
        };
        return typeof manager.getSchemaVersion === 'function'
            ? manager.getSchemaVersion()
            : null;
    }

    private model: VisualSemanticModel | null = null;
    private designer: VisualDesigner;
    private pluginManager: PluginManager;
    private updateListeners: Set<(model: VisualSemanticModel) => void> = new Set();
    private variableAnalyzer: VariableDependencyAnalyzer;
    private connectionPointsCalculator: ConnectionPointsCalculator;
    private connectionRouter: SimpleConnectionRouter;
    private wsManager: VisualModelWebSocketManager | null = null;

    // Cache connection points (absolute positions) for quick access
    private connectionPointsCache: Map<string, ConnectionPoint[]> = new Map();
    private connectionPointsAbsoluteCache: Map<string, Map<string, Point>> = new Map(); // nodeId -> pointId -> absolute position

    // CRITICAL: Anchor registration - DOM elements for connection points
    // Key: `${nodeId}/${pointId}`, Value: HTMLElement
    private anchorElements: Map<string, HTMLElement> = new Map();

    // CRITICAL: Pending anchors - anchors registered before SVG was ready
    // Set of anchor keys (${nodeId}/${pointId}) waiting for SVG registration
    private pendingAnchors: Set<string> | null = null;

    // CRITICAL: Pending connections - connections that couldn't be built because DOM wasn't ready
    // Set of connectionIds needing svg coords
    private pendingConnections: Set<string> = new Set();

    // CRITICAL: Store connection data for pending connections (so we can recalculate them later)
    // Maps connectionId -> { from, to, type, id? }
    private pendingConnectionsData: Map<string, { from: string; to: string; type?: string; id?: string }> = new Map();

    // CRITICAL: Dirty connections - connections that need recalculation
    // Set of connectionIds marked for recalculation
    private dirtyConnections: Set<string> = new Set();

    // CRITICAL: Node versions for cache invalidation
    // Key: nodeId, Value: version number (incremented on position change)
    private nodeVersions: Map<string, number> = new Map();

    // CRITICAL: Connections by node (for cache invalidation)
    // Key: nodeId, Value: Set of connectionIds
    private connectionsByNode: Map<string, Set<string>> = new Map();

    // CRITICAL: SVG element reference (explicit registration)
    private svgElement: SVGSVGElement | null = null;

    // CRITICAL: Batch flag for requestAnimationFrame
    private _batched: boolean = false;

    // CRITICAL: Scheduled flag for drag batching
    private _dragScheduled: boolean = false;

    // Ontology model (wzorowany na Palantir) - dynamicznie budowany z runtime metadata
    private ontologyModel: UnifiedOntologyModel | null = null;
    private pluginRepository: OntologyRepository;
    private nodeRepository: OntologyRepository;
    private inferenceEngine: OntologyInferenceEngine;
    private ontologyBuilder: OntologyBuilder;

    constructor(designer: VisualDesigner, pluginManager: PluginManager) {
        this.designer = designer;
        this.pluginManager = pluginManager;
        this.variableAnalyzer = new VariableDependencyAnalyzer();
        this.connectionPointsCalculator = new ConnectionPointsCalculator();
        this.connectionRouter = new SimpleConnectionRouter();

        // Inicjalizuj ontologię - dynamicznie budowaną z runtime metadata
        this.pluginRepository = new OntologyRepositoryImpl();
        this.nodeRepository = new OntologyRepositoryImpl();
        this.inferenceEngine = new OntologyInferenceEngineImpl();
        this.ontologyBuilder = new OntologyBuilder(pluginManager);

        // Zbuduj schematy ontologiczne z runtime metadata pluginów
        this.buildOntologySchemas();

        // Inicjalizuj WebSocket manager dla synchronizacji z backendem
        this.initializeWebSocket();
    }

    /**
     * Inicjalizuj WebSocket manager
     */
    private async initializeWebSocket(): Promise<void> {
        try {
            this.wsManager = new VisualModelWebSocketManager();
            await this.wsManager.connect();
            console.log('[VisualSemanticModelManager] ✅ WebSocket manager połączony');
        } catch (error) {
            console.warn('[VisualSemanticModelManager] ⚠️ Nie udało się połączyć z WebSocket:', error);
            // Nie przerywaj inicjalizacji - WebSocket jest opcjonalny
        }
    }

    /**
     * CRITICAL: Register SVG element (explicit registration from Designer)
     * Called from Designer in useLayoutEffect
     */
    registerSvgElement(svgEl: SVGSVGElement): void {
        this.svgElement = svgEl;
        console.log('[VisualSemanticModelManager] ✅ Registered SVG element');

        // CRITICAL: Immediately recalc all pending connections (synchronous if possible)
        this.recalculateAllPendingConnectionsSync();

        // CRITICAL: Also retry any pending anchors that were registered before SVG was ready
        if (this.pendingAnchors && this.pendingAnchors.size > 0) {
            const pendingKeys = Array.from(this.pendingAnchors);
            this.pendingAnchors.clear();

            // Extract nodeIds from pending anchor keys and recalc
            const nodeIds = new Set<string>();
            pendingKeys.forEach(key => {
                const [nodeId] = key.split('/');
                nodeIds.add(nodeId);
            });

            // Recalc connections for all affected nodes
            nodeIds.forEach(nodeId => {
                this.recalculatePendingConnectionsForNodeSync(nodeId);
            });

            console.log(`[VisualSemanticModelManager] ✅ Retried ${pendingKeys.length} pending anchors after SVG registration`);
        }
    }

    /**
     * CRITICAL: Unregister SVG element
     */
    unregisterSvgElement(): void {
        this.svgElement = null;
        console.log('[VisualSemanticModelManager] ✅ Unregistered SVG element');
    }

    /**
     * CRITICAL: Register anchor DOM element for connection point
     * Called from Port component in useLayoutEffect (synchronous)
     * Prefer synchronous recalculation to avoid race conditions
     */
    registerAnchor(nodeId: string, pointId: string, element: HTMLElement): void {
        const key = `${nodeId}/${pointId}`;
        this.anchorElements.set(key, element);
        console.log(`[VisualSemanticModelManager] ✅ Registered anchor: ${key}`);

        // CRITICAL: Immediate synchronous recalculation if SVG is ready
        if (this.svgElement) {
            this.recalculatePendingConnectionsForNodeSync(nodeId);
        } else {
            // SVG not ready - add to pending anchors and retry in microtask
            if (!this.pendingAnchors) {
                this.pendingAnchors = new Set<string>();
            }
            this.pendingAnchors.add(key);

            // Retry in microtask (faster than RAF)
            Promise.resolve().then(() => {
                if (this.svgElement && this.pendingAnchors?.has(key)) {
                    this.pendingAnchors.delete(key);
                    this.recalculatePendingConnectionsForNodeSync(nodeId);
                }
            });
        }

        // CRITICAL: Add ResizeObserver to detect layout changes
        this.setupResizeObserverForAnchor(nodeId, pointId, element);
    }

    /**
     * CRITICAL: Unregister anchor DOM element
     */
    unregisterAnchor(nodeId: string, pointId: string): void {
        const key = `${nodeId}/${pointId}`;
        this.anchorElements.delete(key);
        this.removeResizeObserverForAnchor(key);
        console.log(`[VisualSemanticModelManager] ✅ Unregistered anchor: ${key}`);
    }

    /**
     * CRITICAL: Get anchor element for connection point
     */
    private getAnchorElement(nodeId: string, pointId: string): HTMLElement | null {
        const key = `${nodeId}/${pointId}`;
        return this.anchorElements.get(key) || null;
    }

    /**
     * CRITICAL: ResizeObserver for anchor elements
     * Detects layout changes and invalidates cache
     */
    private resizeObservers: Map<string, ResizeObserver> = new Map();

    private setupResizeObserverForAnchor(nodeId: string, pointId: string, element: HTMLElement): void {
        const key = `${nodeId}/${pointId}`;

        // Remove existing observer if any
        this.removeResizeObserverForAnchor(key);

        // Create new observer
        // CRITICAL: ResizeObserver detects layout changes - trigger nodeMoved to update position
        const observer = new ResizeObserver(() => {
            // Get current node position from model
            const nodeState = this.model?.workflow.allNodes.get(nodeId);
            if (nodeState) {
                // Layout changed - update node position and trigger recalculation
                // This ensures connections are recalculated with new anchor positions
                this.nodeMoved(nodeId, nodeState.position);
            } else {
                // Fallback: invalidate cache if node not found
                this.invalidateCacheForNode(nodeId);
            }
        });

        observer.observe(element);
        this.resizeObservers.set(key, observer);
    }

    private removeResizeObserverForAnchor(key: string): void {
        const observer = this.resizeObservers.get(key);
        if (observer) {
            observer.disconnect();
            this.resizeObservers.delete(key);
        }
    }

    /**
     * CRITICAL: Node moved - update position and invalidate dependent connections
     * Called during node drag (pointermove)
     * Uses RAF batching for high-frequency updates during drag
     */
    nodeMoved(nodeId: string, newPosition: Point): void {
        // node.position should already be in world/SVG coords
        // Increment node version
        const currentVersion = this.nodeVersions.get(nodeId) || 0;
        this.nodeVersions.set(nodeId, currentVersion + 1);

        // Mark dependent connections dirty (don't delete pending, mark dirty)
        const connIds = this.connectionsByNode.get(nodeId) || new Set();
        for (const id of connIds) {
            this.dirtyConnections.add(id);
        }

        console.log(`[VisualSemanticModelManager] 🔄 Node ${nodeId} moved (version ${currentVersion + 1}), ${connIds.size} connections marked dirty`);

        // CRITICAL: Batch recalculation for drag (high-frequency updates)
        // Use RAF to batch multiple moves into single recalculation
        if (!this._dragScheduled) {
            this._dragScheduled = true;
            requestAnimationFrame(() => {
                this._dragScheduled = false;
                const dirty = Array.from(this.dirtyConnections);
                if (dirty.length > 0) {
                    this.recalculateConnectionsForSetSync(dirty);
                    // Notify partial update for batched changes
                    this.notifyUpdatePartial(dirty);
                }
            });
        }
    }

    /**
     * CRITICAL: Invalidate cache for node and rebuild connections
     * Don't delete pending connections - mark them dirty instead
     */
    invalidateCacheForNode(nodeId: string): void {
        // Increment node version
        const currentVersion = this.nodeVersions.get(nodeId) || 0;
        this.nodeVersions.set(nodeId, currentVersion + 1);

        // Invalidate connection points cache
        this.connectionPointsCache.delete(nodeId);
        this.connectionPointsAbsoluteCache.delete(nodeId);

        // Mark dependent connections dirty (don't delete pending, mark dirty)
        const connIds = this.connectionsByNode.get(nodeId) || new Set();
        for (const id of connIds) {
            this.dirtyConnections.add(id); // Mark dirty instead of deleting
        }

        console.log(`[VisualSemanticModelManager] ✅ Invalidated cache for node ${nodeId} (version ${currentVersion + 1}), ${connIds.size} connections marked dirty`);

        // Schedule batched rebuild if many
        this.scheduleBatchedRecalc();
    }

    /**
     * CRITICAL: Recalculate pending connections for a specific node (SYNCHRONOUS)
     * Prefer synchronous recalculation to avoid race conditions
     */
    private recalculatePendingConnectionsForNodeSync(nodeId: string): void {
        const connIds = this.connectionsByNode.get(nodeId) || new Set();
        this.recalculateConnectionsForSetSync(connIds);
    }

    /**
     * CRITICAL: Recalculate connections for a set (SYNCHRONOUS when possible)
     */
    private recalculateConnectionsForSetSync(connIds: Iterable<string>): void {
        if (!this.svgElement) {
            // SVG not ready — keep marked as pending
            for (const id of connIds) {
                this.pendingConnections.add(id);
            }
            return;
        }

        // Recalculate each connection
        const recalculated: string[] = [];
        for (const id of connIds) {
            const wasPending = this.pendingConnections.has(id);
            this._recalculateSingleConnection(id);
            // Only remove from pending if it was successfully recalculated
            // _recalculateSingleConnection will add it back to pending if it fails
            if (!this.pendingConnections.has(id)) {
                recalculated.push(id);
                this.dirtyConnections.delete(id);
            }
        }

        // Notify partial update (only changed connections)
        if (recalculated.length > 0) {
            this.notifyUpdatePartial(recalculated);
        }
    }

    /**
     * CRITICAL: Recalculate all pending connections (SYNCHRONOUS)
     * Public method to be called after anchors are registered
     */
    recalculateAllPendingConnectionsSync(): void {
        if (!this.svgElement) {
            console.log(`[VisualSemanticModelManager] ⚠️ Cannot recalculate pending connections: SVG element not ready`);
            return;
        }

        const pending = Array.from(this.pendingConnections);
        if (pending.length === 0) {
            console.log(`[VisualSemanticModelManager] ✅ No pending connections to recalculate`);
            return;
        }

        console.log(`[VisualSemanticModelManager] 🔄 Recalculating ${pending.length} pending connections`);
        this.recalculateConnectionsForSetSync(pending);
        console.log(`[VisualSemanticModelManager] ✅ Recalculated ${pending.length} pending connections`);
    }

    /**
     * CRITICAL: Schedule batched recalculation (for high-frequency updates)
     */
    private scheduleBatchedRecalc(): void {
        if (this._batched) return;

        this._batched = true;
        requestAnimationFrame(() => {
            this._batched = false;
            const dirty = Array.from(this.dirtyConnections);
            if (dirty.length > 0) {
                this.recalculateConnectionsForSetSync(dirty);
            }
        });
    }

    /**
     * Oblicz punkt na obwodzie prostokąta najbliższy do anchor point
     * 
     * Inspirowane implementacją z Apache NiFi (canvas-utils.service.ts)
     * 
     * @param anchorPoint Punkt anchor (w przestrzeni SVG)
     * @param bounds Bounding box node {x, y, width, height}
     * @returns Punkt na obwodzie prostokąta
     */
    private getPerimeterPoint(anchorPoint: Point, bounds: { x: number; y: number; width: number; height: number }): Point {
        const TWO_PI = 2 * Math.PI;

        // Oblicz kąt theta (kąt między przekątną a poziomą osią)
        const theta = Math.atan2(bounds.height, bounds.width);

        // Oblicz promienie prostokąta
        const xRadius = bounds.width / 2;
        const yRadius = bounds.height / 2;

        // Oblicz środek prostokąta
        const cx = bounds.x + xRadius;
        const cy = bounds.y + yRadius;

        // Oblicz kąt alpha (kąt między anchor point a środkiem)
        const dx = anchorPoint.x - cx;
        const dy = anchorPoint.y - cy;
        let alpha = Math.atan2(dy, dx);

        // Normalizuj alpha do zakresu 0 <= alpha < 2π
        alpha = alpha % TWO_PI;
        if (alpha < 0) {
            alpha += TWO_PI;
        }

        // Oblicz beta
        const beta = Math.PI / 2 - alpha;

        // Określ kwadrant i zwróć punkt na odpowiedniej krawędzi
        if ((alpha >= 0 && alpha < theta) || (alpha >= TWO_PI - theta && alpha < TWO_PI)) {
            // Right quadrant (prawa krawędź)
            return {
                x: bounds.x + bounds.width,
                y: cy + Math.tan(alpha) * xRadius
            };
        } else if (alpha >= theta && alpha < Math.PI - theta) {
            // Bottom quadrant (dolna krawędź)
            return {
                x: cx + Math.tan(beta) * yRadius,
                y: bounds.y + bounds.height
            };
        } else if (alpha >= Math.PI - theta && alpha < Math.PI + theta) {
            // Left quadrant (lewa krawędź)
            return {
                x: bounds.x,
                y: cy - Math.tan(alpha) * xRadius
            };
        } else {
            // Top quadrant (górna krawędź)
            return {
                x: cx - Math.tan(beta) * yRadius,
                y: bounds.y
            };
        }
    }

    /**
     * CRITICAL: Recalculate single connection using anchors (if exist) OR model positions
     * 
     * Zgodnie z planem naprawczym + rozwiązanie z NiFi:
     * - Jeśli SVG lub anchor DOM nie istnieją → pending + retry przez RAF
     * - Jeśli getAnchorSvgPosition zwróci null (CTM null) → pending + retry przez RAF
     * - Użyj getPerimeterPoint() aby obliczyć punkty na obwodzie nodes (jak w NiFi)
     * - Zapis modelu i powiadomienie listenerów bezpośrednio (bez notifyUpdate)
     */
    private _recalculateSingleConnection(connId: string): void {
        if (!this.model) return;

        // Get connection from model or pendingConnectionsData
        let conn = this.model.workflow.connections.get(connId);
        if (!conn) {
            const connectionData = this.pendingConnectionsData.get(connId);
            if (!connectionData) {
                console.warn(`[CONN] _recalculateSingleConnection: missing conn data ${connId}`);
                return;
            }
            // Create minimal connection object for recalculation
            conn = {
                connectionId: connId,
                fromNodeId: connectionData.from,
                toNodeId: connectionData.to,
                type: (connectionData.type as 'sequential' | 'conditional' | 'error') || 'sequential',
                path: {
                    points: [],
                    routingType: 'orthogonal' as const,
                    spatialMetadata: {
                        totalLength: 0,
                        segmentCount: 0,
                        minDistanceToNode: 0,
                        crossesOtherConnections: false,
                        boundingBox: { left: 0, right: 0, top: 0, bottom: 0, width: 0, height: 0, centerX: 0, centerY: 0 },
                        startSide: 'right' as Side,
                        endSide: 'left' as Side,
                        startPoint: { x: 0, y: 0 },
                        endPoint: { x: 0, y: 0 },
                        turningPointsCount: 0,
                        passesThroughNodes: [],
                        labelCollisions: []
                    }
                },
                anchors: {
                    from: { nodeId: connectionData.from, connectionPointId: '', position: { x: 0, y: 0 } },
                    to: { nodeId: connectionData.to, connectionPointId: '', position: { x: 0, y: 0 } }
                },
                visualState: { selected: false, hovered: false, highlighted: false, error: false },
                edgeValidation: { startPointOnEdge: false, endPointOnEdge: false, startPointTolerance: 0, endPointTolerance: 0, arrowPointsTowardsTarget: true },
                spatialValidation: { pathLength: 0, isValidPath: false, startNodeDistance: 0, endNodeDistance: 0, minClearance: 0, routingQuality: 'good' as any },
                pathGeometry: { startPoint: { x: 0, y: 0 }, endPoint: { x: 0, y: 0 }, turningPoints: [], segments: [], selfIntersections: [], crossesOtherConnections: [], reversesDirection: false, maxDeviation: 0 },
                renderCache: { svgPath: null, lastRenderTime: 0, needsRerender: false }
            } as VisualConnectionState;
        }

        // Resolve nodes
        const fromNode = this.model.workflow.allNodes.get(conn.fromNodeId);
        const toNode = this.model.workflow.allNodes.get(conn.toNodeId);
        if (!fromNode || !toNode) {
            console.warn(`[CONN] nodes missing for ${connId}`);
            this.pendingConnections.add(connId);
            if (!this.pendingConnectionsData.has(connId)) {
                this.pendingConnectionsData.set(connId, {
                    from: conn.fromNodeId,
                    to: conn.toNodeId,
                    type: conn.type,
                    id: connId
                });
            }
            return;
        }

        // Prefer center anchor
        const fromPoint = fromNode.connectionPoints.outputs.find(p => p.id.includes('-center'));
        const toPoint = toNode.connectionPoints.inputs.find(p => p.id.includes('-center'));
        if (!fromPoint || !toPoint) {
            console.warn(`[CONN] anchor points missing for ${connId}`);
            this.pendingConnections.add(connId);
            if (!this.pendingConnectionsData.has(connId)) {
                this.pendingConnectionsData.set(connId, {
                    from: conn.fromNodeId,
                    to: conn.toNodeId,
                    type: conn.type,
                    id: connId
                });
            }
            return;
        }

        const fromKey = `${fromNode.nodeId}/${fromPoint.id}`;
        const toKey = `${toNode.nodeId}/${toPoint.id}`;
        const fromEl = this.anchorElements.get(fromKey);
        const toEl = this.anchorElements.get(toKey);

        // If SVG not ready or anchor DOM not yet present -> mark pending and schedule retry
        if (!this.svgElement || !fromEl || !toEl) {
            this.pendingConnections.add(connId);
            // Ensure data stored for later retry
            if (!this.pendingConnectionsData.has(connId)) {
                this.pendingConnectionsData.set(connId, {
                    from: conn.fromNodeId,
                    to: conn.toNodeId,
                    type: conn.type,
                    id: connId
                });
            }
            // Schedule a retry on next RAF so we try after layout
            requestAnimationFrame(() => {
                this._recalculateSingleConnection(connId);
            });
            return;
        }

        // Convert anchor DOM -> SVG coords (may return null if CTM not ready)
        const fromAnchorSvg = getAnchorSvgPosition(fromEl, this.svgElement);
        const toAnchorSvg = getAnchorSvgPosition(toEl, this.svgElement);

        if (!fromAnchorSvg || !toAnchorSvg) {
            // CTM likely null - schedule retry and mark pending
            this.pendingConnections.add(connId);
            if (!this.pendingConnectionsData.has(connId)) {
                this.pendingConnectionsData.set(connId, {
                    from: conn.fromNodeId,
                    to: conn.toNodeId,
                    type: conn.type,
                    id: connId
                });
            }
            requestAnimationFrame(() => this._recalculateSingleConnection(connId));
            return;
        }

        // CRITICAL: Użyj DOKŁADNIE center points z connection points
        // Center points są już na obwodzie nodes (na krawędziach), więc używamy ich bezpośrednio
        // Router sam obliczy offset od obwodu jeśli potrzebny (w SimpleConnectionRouter.offsetPoint)
        const start = fromAnchorSvg; // Dokładnie center point z connection point
        const end = toAnchorSvg; // Dokładnie center point z connection point

        // Sprawdź czy start i end są różne
        const distX = Math.abs(start.x - end.x);
        const distY = Math.abs(start.y - end.y);
        if (distX < 1 && distY < 1) {
            console.warn(`[CONN] ${connId}: start and end are too close (${distX}, ${distY}), router will handle it`);
        }

        // Debug logging
        const shouldDebug = (typeof window !== 'undefined' && (window as any).__DEBUG_CONNECTIONS__);
        if (shouldDebug) {
            console.group(`RECALC ${connId}`);
            console.log('fromKey', fromKey, 'toKey', toKey);
            console.log('fromAnchorSvg', fromAnchorSvg, 'toAnchorSvg', toAnchorSvg);
            console.log('start (perimeter)', start, 'end (perimeter)', end);
        }

        // CRITICAL: Set node bounds for collision detection
        this.connectionRouter.clearNodeBounds();
        const allNodes = this.model.workflow.allNodes;
        allNodes.forEach(node => {
            this.connectionRouter.setNodeBounds(node.nodeId, {
                x: node.position.x,
                y: node.position.y,
                width: node.computedBounds.width,
                height: node.computedBounds.height
            });
        });

        // Route używając center points z unikaniem kolizji (wyklucz from/to nodes)
        const excludeNodes = new Set<string>([conn.fromNodeId, conn.toNodeId]);
        const points = this.connectionRouter.route(start, end, fromPoint.side, toPoint.side, excludeNodes);

        if (shouldDebug) {
            console.log('router points', points);
        }

        // Build updated connectionState (simplified - use existing helper methods if needed)
        // For now, create minimal valid state - can be expanded later
        const updatedConnectionState: VisualConnectionState = {
            ...conn,
            anchors: {
                from: { nodeId: fromNode.nodeId, connectionPointId: fromPoint.id, position: fromAnchorSvg },
                to: { nodeId: toNode.nodeId, connectionPointId: toPoint.id, position: toAnchorSvg }
            },
            path: {
                points,
                routingType: 'orthogonal' as const,
                spatialMetadata: {
                    totalLength: 0,
                    segmentCount: points.length - 1,
                    minDistanceToNode: 0,
                    crossesOtherConnections: false,
                    boundingBox: { left: 0, right: 0, top: 0, bottom: 0, width: 0, height: 0, centerX: 0, centerY: 0 },
                    startSide: fromPoint.side,
                    endSide: toPoint.side,
                    startPoint: points[0] || start,
                    endPoint: points[points.length - 1] || end,
                    turningPointsCount: 0,
                    passesThroughNodes: [],
                    labelCollisions: []
                }
            }
        };

        // Write into semantic model - source of truth
        this.model.workflow.connections.set(connId, updatedConnectionState);

        // CRITICAL: Don't call notifyUpdate() here - it will rebuild the model and may overwrite our connection state
        // Instead, notify listeners directly with the updated model
        // This ensures that the connection state we just calculated is preserved
        console.log(`[CONN] ✅ Connection ${connId} recalculated: ${points.length} points, saved to model`);

        // Notify listeners directly without rebuilding model
        this.updateListeners.forEach(listener => {
            try {
                listener(this.model!);
            } catch (error) {
                console.error('[CONN] Error in update listener:', error);
            }
        });

        // Remove from pending if present
        this.pendingConnections.delete(connId);
        this.pendingConnectionsData.delete(connId);

        if (shouldDebug) {
            console.log('updatedConnectionState saved', updatedConnectionState);
            console.log('path points:', updatedConnectionState.path.points);
            console.groupEnd();
        }
    }

    /**
     * CRITICAL: Notify partial update (only changed connections)
     */
    private notifyUpdatePartial(connIds: string[]): void {
        // Emit event only for changed connections (renderer can update minimal set)
        // For now, trigger full notify - can be optimized later
        if (connIds.length > 0) {
            console.log(`[VisualSemanticModelManager] 🔔 Notifying partial update for ${connIds.length} connections`);
            this.notifyUpdate();
        }
    }

    /**
     * CRITICAL: Request rebuild of connections for a specific node
     * Called from Port component after anchor registration
     */
    requestRebuildConnectionsForNode(nodeId: string): void {
        this.invalidateCacheForNode(nodeId);
    }

    /**
     * Zbuduj schematy ontologiczne dynamicznie z runtime metadata
     */
    private buildOntologySchemas(): void {
        const schemas = this.ontologyBuilder.buildAllSchemas();

        // Dodaj plugin entities do repozytorium
        const plugins = this.pluginManager.getPlugins();
        plugins.forEach(plugin => {
            const entity = this.ontologyBuilder.buildPluginEntity(plugin);
            this.pluginRepository.addEntity(entity);
        });

        console.log(`[VisualSemanticModelManager] ✅ Built ontology schemas for ${schemas.pluginSchemas.size} plugins`);
    }

    /**
     * Buduj model real-time z VisualDesigner
     */
    buildModel(): VisualSemanticModel {
        // Odśwież schematy ontologiczne jeśli metadane się zmieniły
        this.ontologyBuilder.refreshSchemas();

        const allNodes = this.designer.getAllNodes();
        const rootNodes = this.designer.getNodes(); // Get root nodes

        // Generate connections dynamically from nodes
        const autoConnections = generateConnections(allNodes);
        const manualConnections = (this.designer as any).connectionManager?.getConnections() || [];

        // CRITICAL: Add planned connections (workflow-start -> nodes -> workflow-end)
        // This ensures start/end markers are included in the model
        const plannedConnections = planConnections(rootNodes, 'workflow-start', 'workflow-end');

        // Combine all connections - preserve original IDs from ConnectionManager
        const connections: Array<Connection & { id?: string }> = [
            ...autoConnections,
            ...manualConnections.map((conn: any) => ({
                id: conn.id, // CRITICAL: Preserve original ID from ConnectionManager
                from: conn.from,
                to: conn.to,
                type: (conn.type || 'sequential') as 'sequential' | 'conditional' | 'error'
            })),
            ...plannedConnections
        ];

        // CRITICAL DEBUG: Check if nodes are actually in designer
        const designerNodesDirect = (this.designer as any).nodes;
        console.log(`[VisualSemanticModelManager] buildModel: DEBUG - designer.nodes directly: ${designerNodesDirect?.length || 0}`);
        console.log(`[VisualSemanticModelManager] buildModel: allNodes.length = ${allNodes.length}, rootNodes.length = ${rootNodes.length}, connections.length = ${connections.length}`);

        if (allNodes.length === 0 && rootNodes.length === 0 && designerNodesDirect?.length > 0) {
            console.error(`[VisualSemanticModelManager] ❌ ERROR: designer.nodes has ${designerNodesDirect.length} nodes but getAllNodesFlat() and getNodes() return empty!`);
        }
        const viewportInfo = { x: 0, y: 0, width: 1920, height: 1080, zoom: 1 }; // TODO: Get from designer
        const metadata = { nodes: [] }; // TODO: Get from designer

        // Build available plugins map
        const availablePlugins = this.buildAvailablePluginsMap();

        // Zbuduj node entities w ontologii
        this.buildNodeEntities(allNodes);

        // Build node states
        const nodeStates = this.buildNodeStates(allNodes, rootNodes, availablePlugins);

        // Build connection states (this updates semanticSelection in cache)
        // CRITICAL: This must happen AFTER buildNodeStates so that connection points are in cache
        const connectionStates = this.buildConnectionStates(connections, nodeStates);

        // CRITICAL: Verify that semanticSelection was updated in cache
        let totalSelectedInCacheAfterBuild = 0;
        let totalPointsInCacheAfterBuild = 0;
        nodeStates.forEach(nodeState => {
            const cachedPoints = this.connectionPointsCache.get(nodeState.nodeId);
            if (cachedPoints) {
                totalPointsInCacheAfterBuild += cachedPoints.length;
                const selected = cachedPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
                totalSelectedInCacheAfterBuild += selected.length;
            }
        });

        const shouldLogCache = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogCache) {
            console.log(`[VisualSemanticModelManager] After buildConnectionStates: ${totalSelectedInCacheAfterBuild}/${totalPointsInCacheAfterBuild} connection points have semanticSelection.isSelected=true in cache`);
        }

        // CRITICAL: After buildConnectionStates updates semanticSelection in cache,
        // update nodeStates connection points with references from cache BEFORE creating the Map
        // This ensures model uses the same objects as cache, so updates are reflected
        // This must happen BEFORE line 199 where we create the Map, so the Map contains references to cache
        let syncCount = 0;
        let selectedCountAfterSync = 0;
        nodeStates.forEach(nodeState => {
            const cachedPoints = this.connectionPointsCache.get(nodeState.nodeId);
            if (cachedPoints && cachedPoints.length > 0) {
                const inputPoints = cachedPoints.filter(cp => cp.type === 'input');
                const outputPoints = cachedPoints.filter(cp => cp.type === 'output');

                // CRITICAL: Count selected points in cache BEFORE sync
                const selectedInCache = [...inputPoints, ...outputPoints].filter(
                    p => p.semanticSelection && p.semanticSelection.isSelected
                );

                // CRITICAL: Replace with cache references - these are the same objects that were updated
                nodeState.connectionPoints.inputs = inputPoints;
                nodeState.connectionPoints.outputs = outputPoints;
                syncCount++;

                // CRITICAL: Verify sync worked - count selected points AFTER sync
                const selectedAfterSync = [...inputPoints, ...outputPoints].filter(
                    p => p.semanticSelection && p.semanticSelection.isSelected
                );
                selectedCountAfterSync += selectedAfterSync.length;

                // CRITICAL: Verify references are the same
                if (selectedInCache.length > 0 && selectedAfterSync.length === 0) {
                    const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
                    if (shouldLog) {
                        console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Lost ${selectedInCache.length} selected points during sync for node ${nodeState.nodeId}`);
                    }
                }
            }
        });

        const shouldLogSync = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogSync && selectedCountAfterSync > 0) {
            console.log(`[VisualSemanticModelManager] ✅ Synced ${syncCount} nodes, found ${selectedCountAfterSync} selected points in cache after sync`);
        }

        // Verify synchronization worked
        let syncedBeforeMap = 0;
        let selectedBeforeMap = 0;
        nodeStates.forEach(nodeState => {
            const allPoints = [...nodeState.connectionPoints.inputs, ...nodeState.connectionPoints.outputs];
            const selected = allPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
            if (selected.length > 0) {
                syncedBeforeMap++;
                selectedBeforeMap += selected.length;
            }
        });
        // Always log in development, but also log in production for debugging runtime tests
        const isDevelopment = process.env.NODE_ENV === 'development';
        const shouldLog = isDevelopment || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (selectedBeforeMap > 0 && shouldLog) {
            console.log(`[VisualSemanticModelManager] ✅ Synced ${syncedBeforeMap} nodes with ${selectedBeforeMap} selected points BEFORE creating Map`);
        }

        // Analyze variable dependencies
        const variableDependencies = this.variableAnalyzer.analyze(allNodes);

        // Update variable semantics in availablePlugins based on actual usage
        this.updateVariableSemanticsFromDependencies(availablePlugins, variableDependencies, allNodes);

        // Build layout state
        const layoutState = this.buildLayoutState(viewportInfo, nodeStates, connectionStates, metadata);

        // Build UI state
        const uiState = this.buildUIState();

        const model: VisualSemanticModel = {
            version: '1.0.0',
            schemaVersion: this.getPluginSchemaVersion(),
            timestamp: Date.now(),
            workflowId: this.getWorkflowId(),

            availablePlugins,
            workflow: {
                rootNodes: nodeStates.filter(ns => ns.isRoot),
                allNodes: new Map(nodeStates.map(ns => [ns.nodeId, ns])),
                connections: connectionStates,
                variableDependencies
            },
            layout: layoutState,
            ui: uiState
        };

        this.model = model;

        // CRITICAL: After building model, ensure connection points in model use same references as cache
        // This is a double-check since we already synced nodeStates before creating the Map (lines 172-180)
        // But we verify and re-sync here to ensure consistency
        let updatedCount = 0;
        let verifiedSelectedCount = 0;
        model.workflow.allNodes.forEach((nodeState, nodeId) => {
            const cachedPoints = this.connectionPointsCache.get(nodeId);
            if (cachedPoints && cachedPoints.length > 0) {
                // Replace connection points in model with cached ones (same references)
                const inputPoints = cachedPoints.filter(cp => cp.type === 'input');
                const outputPoints = cachedPoints.filter(cp => cp.type === 'output');

                // CRITICAL: Replace arrays with cached references for direct access
                // This ensures model uses the same objects as cache, so updates are reflected
                nodeState.connectionPoints.inputs = inputPoints;
                nodeState.connectionPoints.outputs = outputPoints;
                updatedCount++;

                // Verify semanticSelection is present
                const allPoints = [...inputPoints, ...outputPoints];
                const selected = allPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
                verifiedSelectedCount += selected.length;
            }
        });

        // Log only in debug mode
        if (updatedCount > 0 && process.env.NODE_ENV === 'development') {
            console.debug(`[VisualSemanticModelManager] ✅ Verified and synced ${updatedCount} nodes' connection points in model, found ${verifiedSelectedCount} selected points`);
        }

        return model;
    }

    /**
     * Buduj mapę dostępnych pluginów
     */
    private buildAvailablePluginsMap(): Map<string, PluginSemanticInfo> {
        const plugins = this.pluginManager.getPlugins();
        const map = new Map<string, PluginSemanticInfo>();

        plugins.forEach(plugin => {
            // Infer execution semantics from plugin type
            const pluginId = plugin.id.toLowerCase();
            const isContainer = plugin.semantics?.allowsChildren ?? false;
            let executionOrder: 'sequential' | 'parallel' | 'conditional' = 'sequential';
            if (pluginId.includes('if') || pluginId.includes('else') || pluginId.includes('try') || pluginId.includes('catch')) {
                executionOrder = 'conditional';
            } else if (pluginId.includes('loop') || pluginId.includes('while')) {
                executionOrder = 'parallel';
            }

            // Infer variable semantics (will be updated in runtime)
            const variableScope = (plugin.semantics?.requiredParentTypes && plugin.semantics.requiredParentTypes.length > 0) ? 'parent' : 'root';

            // Build port compatibility matrix
            const portCompatibility: { [portType: string]: string[] } = {
                'STRING': ['STRING', 'XML', 'BINARY'],
                'XML': ['XML', 'STRING'],
                'BINARY': ['BINARY', 'STRING'],
                'MIXED': ['STRING', 'XML', 'BINARY', 'MIXED']
            };

            map.set(plugin.id, {
                id: plugin.id,
                name: plugin.name,
                category: plugin.category || 'other',
                description: plugin.description || '',

                semantics: {
                    canAppearAtRoot: plugin.semantics?.canAppearAtRoot ?? true,
                    allowsChildren: plugin.semantics?.allowsChildren ?? false,
                    acceptsAnyChild: plugin.semantics?.acceptsAnyChild ?? false,
                    allowedChildTypes: plugin.semantics?.allowedChildTypes || [],
                    requiredParentTypes: plugin.semantics?.requiredParentTypes || [],
                    roles: plugin.semantics?.roles || [], // Full array from backend
                    allowsBody: plugin.semantics?.allowBody ?? false,
                    allowTextContent: plugin.semantics?.allowTextContent ?? false,
                    // Execution semantics
                    executionSemantics: {
                        isContainer,
                        isLeaf: !isContainer,
                        executionOrder,
                        canPause: true,
                        canResume: true
                    },
                    // Variable semantics (will be updated in runtime)
                    variableSemantics: {
                        definesVariables: [], // Will be populated from VariableDependencyAnalyzer
                        usesVariables: [], // Will be populated from VariableDependencyAnalyzer
                        variableScope
                    }
                },

                schema: {
                    attributes: ((plugin.schema?.attributes || plugin.schema?.attributeDefinitions || []) as any[]).map((attr: any) => ({
                        name: attr.name,
                        type: attr.type || 'string',
                        required: attr.required || false,
                        description: attr.description,
                        allowedValues: attr.allowedValues,
                        defaultValue: attr.defaultValue
                    })),
                    allowsBody: plugin.semantics?.allowBody ?? false
                },

                visual: {
                    icon: plugin.icon,
                    color: plugin.color,
                    defaultWidth: plugin.defaultWidth,
                    defaultHeight: plugin.defaultHeight,
                    inputPorts: plugin.inputPorts?.map(port => ({
                        name: port.name,
                        description: port.description,
                        type: port.type,
                        required: port.required || false
                    })),
                    outputPorts: plugin.outputPorts?.map(port => ({
                        name: port.name,
                        description: port.description,
                        type: port.type
                    })),
                    portCompatibility
                },

                // Master/detail constraints from plugin
                masterDetailConstraints: plugin.masterDetailConstraints?.map(constraint => ({
                    detailType: constraint.detailType,
                    maxOccurs: constraint.maxOccurs,
                    minOccurs: constraint.minOccurs,
                    sectionTitle: constraint.sectionTitle,
                    addLabel: constraint.addLabel
                }))
            });
        });

        return map;
    }

    /**
     * Update variable semantics in availablePlugins based on actual variable dependencies
     */
    private updateVariableSemanticsFromDependencies(
        availablePlugins: Map<string, PluginSemanticInfo>,
        variableDependencies: Map<string, VariableDependency>,
        allNodes: VisualNode[]
    ): void {
        // Build a map of plugin type -> variables defined/used
        const pluginVariableMap = new Map<string, { defines: Set<string>; uses: Set<string> }>();

        allNodes.forEach(node => {
            const pluginInfo = availablePlugins.get(node.type);
            if (!pluginInfo) return;

            if (!pluginVariableMap.has(node.type)) {
                pluginVariableMap.set(node.type, { defines: new Set(), uses: new Set() });
            }
            const varMap = pluginVariableMap.get(node.type)!;

            // Check if this node defines variables
            const varDef = this.variableAnalyzer.extractDefinedVariables(node);
            varDef.forEach(v => varMap.defines.add(v));

            // Check if this node uses variables
            const varUsed = this.variableAnalyzer.extractVariableReferences(node);
            varUsed.forEach(v => varMap.uses.add(v));
        });

        // Update plugin semantics with actual variable usage
        pluginVariableMap.forEach((varMap, pluginId) => {
            const pluginInfo = availablePlugins.get(pluginId);
            if (pluginInfo && pluginInfo.semantics.variableSemantics) {
                pluginInfo.semantics.variableSemantics.definesVariables = Array.from(varMap.defines);
                pluginInfo.semantics.variableSemantics.usesVariables = Array.from(varMap.uses);
            }
        });
    }

    /**
     * Buduj stany węzłów (włącznie z workflow-start i workflow-end jako wirtualne węzły)
     */
    private buildNodeStates(
        allNodes: VisualNode[],
        rootNodes: VisualNode[],
        availablePlugins: Map<string, PluginSemanticInfo>
    ): VisualNodeState[] {
        // First pass: build all node states with bounds
        const nodeStatesWithBounds = allNodes.map(node => {
            const plugin = this.findPlugin(node.type);
            const position = node.position || { x: 0, y: 0 };
            const dimensions = this.getNodeDimensions(node);
            const bounds = this.calculateBounds(position, dimensions);

            // Calculate connection points
            // CRITICAL: Try to reuse connection points from cache if available (to preserve semanticSelection)
            let connectionPoints = this.connectionPointsCache.get(node.id);
            if (!connectionPoints) {
                // Calculate new connection points
                connectionPoints = this.connectionPointsCalculator.calculate(
                    node,
                    plugin,
                    position,
                    dimensions
                );
            } else {
                // Recalculate positions if node moved, but keep semanticSelection metadata
                const newPoints = this.connectionPointsCalculator.calculate(
                    node,
                    plugin,
                    position,
                    dimensions
                );
                // CRITICAL: Get from cache - these are the actual objects that will be updated
                const cachedPoints = this.connectionPointsCache.get(node.id);

                // CRITICAL: Instead of creating new objects, update positions in cached points
                // This preserves references so that semanticSelection updates are reflected
                if (cachedPoints && cachedPoints.length > 0) {
                    // Update positions in cached points, but keep the same objects
                    newPoints.forEach(newPoint => {
                        const cachedPoint = cachedPoints.find(cp => cp.id === newPoint.id);
                        if (cachedPoint) {
                            // Update position but keep the same object reference
                            cachedPoint.position = newPoint.position;
                            // Update other properties that might have changed
                            cachedPoint.edgePosition = newPoint.edgePosition;
                        }
                    });
                    // Use cached points (with updated positions) instead of creating new objects
                    connectionPoints = cachedPoints;
                } else {
                    // If no cache, create new points but preserve semanticSelection if available
                    connectionPoints = newPoints.map(newPoint => {
                        const oldPoint = connectionPoints!.find(cp => cp.id === newPoint.id);
                        if (oldPoint && oldPoint.semanticSelection) {
                            return {
                                ...newPoint,
                                semanticSelection: oldPoint.semanticSelection
                            };
                        }
                        return newPoint;
                    });
                }
            }

            // CRITICAL: Always update cache with current position
            // This ensures cache is fresh even if node was moved
            this.updateConnectionPointsCache(node.id, connectionPoints, position);

            // Extract variables
            const variables = this.extractNodeVariables(node);

            // Find parent and children
            const parent = this.findParentNode(node, rootNodes);
            const children = node.children || [];
            const depth = this.getNodeDepth(node, rootNodes);

            // Get visual state
            const visualState = this.getNodeVisualState(node);

            // Calculate spatial collisions for this node
            const allNodeStates = allNodes.map(n => {
                const nPos = n.position || { x: 0, y: 0 };
                const nDims = this.getNodeDimensions(n);
                return {
                    nodeId: n.id,
                    computedBounds: this.calculateBounds(nPos, nDims)
                };
            });

            const collisions = SpatialCollisionDetector.findNodeCollisions(
                bounds,
                node.id,
                allNodeStates
            );

            const clearance = SpatialCollisionDetector.calculateClearance(
                bounds,
                node.id,
                allNodeStates
            );

            // CRITICAL: Store connection points with references (not copies) so we can update semanticSelection later
            const inputPoints = connectionPoints.filter(cp => cp.type === 'input');
            const outputPoints = connectionPoints.filter(cp => cp.type === 'output');

            return {
                nodeId: node.id,
                pluginType: node.type,
                position,
                dimensions,
                computedBounds: bounds,
                connectionPoints: {
                    inputs: inputPoints,  // Keep references, not copies
                    outputs: outputPoints  // Keep references, not copies
                },
                variables,
                parentId: parent?.id || null,
                childrenIds: children.map(c => c.id),
                depth,
                isRoot: depth === 0,
                visualState,
                layoutMetadata: {
                    layoutGroup: parent ? `parent-${parent.id}` : 'root',
                    zIndex: depth,
                    isManualPositioned: node.isManualPositioned || false
                },
                spatialCollisions: {
                    overlapsWithNodes: collisions.map(c => c.nodeId),
                    overlapsWithBounds: collisions.map(c => c.overlapBounds),
                    isOverlapping: collisions.length > 0,
                    overlapArea: collisions.reduce((sum, c) => sum + c.overlapArea, 0),
                    clearance
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };
        });

        // Second pass: calculate spatial collisions for each node
        const regularNodeStates = nodeStatesWithBounds.map(nodeState => {
            const collisions = SpatialCollisionDetector.findNodeCollisions(
                nodeState.computedBounds,
                nodeState.nodeId,
                nodeStatesWithBounds.map(ns => ({ nodeId: ns.nodeId, computedBounds: ns.computedBounds }))
            );

            const clearance = SpatialCollisionDetector.calculateClearance(
                nodeState.computedBounds,
                nodeState.nodeId,
                nodeStatesWithBounds.map(ns => ({ nodeId: ns.nodeId, computedBounds: ns.computedBounds }))
            );

            const totalOverlapArea = collisions.reduce((sum, c) => sum + c.overlapArea, 0);

            return {
                ...nodeState,
                spatialCollisions: {
                    overlapsWithNodes: collisions.map(c => c.nodeId),
                    overlapsWithBounds: collisions.map(c => c.overlapBounds),
                    isOverlapping: collisions.length > 0,
                    overlapArea: totalOverlapArea,
                    clearance
                }
            };
        });

        // CRITICAL: Add workflow-start and workflow-end as virtual nodes with connection points like regular labels
        // They should have 4 connection points (top, bottom, left, right) like regular labels
        const virtualNodeStates: VisualNodeState[] = [];

        // Get marker positions from designer
        const designerAny = this.designer as any;
        const startPos = designerAny?.startMarkerPosition;
        const endPos = designerAny?.endMarkerPosition;
        const MARKER_RADIUS = 30; // PROCESS_MARKER_RADIUS
        const MARKER_SIZE = MARKER_RADIUS * 2;

        if (startPos) {
            // Create workflow-start node state with connection points on all 4 sides
            const startBounds = this.calculateBounds(
                { x: startPos.x - MARKER_RADIUS, y: startPos.y - MARKER_RADIUS },
                { width: MARKER_SIZE, height: MARKER_SIZE }
            );

            // Create connection points for all 4 sides (like regular labels)
            const startConnectionPoints = this.createMarkerConnectionPoints(
                startBounds,
                'workflow-start',
                'output' // Start marker is primarily an output
            );

            virtualNodeStates.push({
                nodeId: 'workflow-start',
                pluginType: 'workflow-start',
                position: { x: startPos.x - MARKER_RADIUS, y: startPos.y - MARKER_RADIUS },
                dimensions: { width: MARKER_SIZE, height: MARKER_SIZE },
                computedBounds: startBounds,
                connectionPoints: {
                    inputs: startConnectionPoints.filter(cp => cp.type === 'input'),
                    outputs: startConnectionPoints.filter(cp => cp.type === 'output')
                },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    layoutGroup: 'root',
                    zIndex: 0,
                    isManualPositioned: false
                },
                spatialCollisions: {
                    overlapsWithNodes: [],
                    overlapsWithBounds: [],
                    isOverlapping: false,
                    overlapArea: 0,
                    clearance: { left: 0, right: 0, top: 0, bottom: 0, min: 0 }
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            });
        }

        if (endPos) {
            // Create workflow-end node state with connection points on all 4 sides
            const endBounds = this.calculateBounds(
                { x: endPos.x - MARKER_RADIUS, y: endPos.y - MARKER_RADIUS },
                { width: MARKER_SIZE, height: MARKER_SIZE }
            );

            // Create connection points for all 4 sides (like regular labels)
            const endConnectionPoints = this.createMarkerConnectionPoints(
                endBounds,
                'workflow-end',
                'input' // End marker is primarily an input
            );

            virtualNodeStates.push({
                nodeId: 'workflow-end',
                pluginType: 'workflow-end',
                position: { x: endPos.x - MARKER_RADIUS, y: endPos.y - MARKER_RADIUS },
                dimensions: { width: MARKER_SIZE, height: MARKER_SIZE },
                computedBounds: endBounds,
                connectionPoints: {
                    inputs: endConnectionPoints.filter(cp => cp.type === 'input'),
                    outputs: endConnectionPoints.filter(cp => cp.type === 'output')
                },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    layoutGroup: 'root',
                    zIndex: 0,
                    isManualPositioned: false
                },
                spatialCollisions: {
                    overlapsWithNodes: [],
                    overlapsWithBounds: [],
                    isOverlapping: false,
                    overlapArea: 0,
                    clearance: { left: 0, right: 0, top: 0, bottom: 0, min: 0 }
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            });
        }

        return [...regularNodeStates, ...virtualNodeStates];
    }

    /**
     * Create connection points for workflow markers (start/end) on all 4 sides
     * Like regular labels, they should have 3 points per edge (start, center, end)
     */
    private createMarkerConnectionPoints(
        bounds: Bounds,
        markerId: 'workflow-start' | 'workflow-end',
        primaryType: 'input' | 'output'
    ): ConnectionPoint[] {
        const points: ConnectionPoint[] = [];
        const sides: Side[] = ['top', 'bottom', 'left', 'right'];

        sides.forEach(side => {
            // Create 3 points per edge (start, center, end) - same as regular labels
            const edgePoints = this.createThreePointsOnEdge(bounds, side, primaryType, markerId);
            points.push(...edgePoints);

            // Also add opposite type points (start can receive input, end can send output)
            const oppositeType = primaryType === 'input' ? 'output' : 'input';
            const oppositePoints = this.createThreePointsOnEdge(bounds, side, oppositeType, markerId);
            points.push(...oppositePoints);
        });

        return points;
    }

    /**
     * Create 3 points on an edge (start, center, end) - same as ConnectionPointsCalculator
     */
    private createThreePointsOnEdge(
        bounds: Bounds,
        side: Side,
        type: 'input' | 'output',
        markerId: 'workflow-start' | 'workflow-end'
    ): ConnectionPoint[] {
        const points: ConnectionPoint[] = [];
        const offset = 10; // Same as DEFAULT_PORT_OFFSET

        let startPoint: Point;
        let centerPoint: Point;
        let endPoint: Point;

        switch (side) {
            case 'top':
                startPoint = { x: bounds.left + offset, y: bounds.top };
                centerPoint = { x: bounds.centerX, y: bounds.top };
                endPoint = { x: bounds.right - offset, y: bounds.top };
                break;
            case 'bottom':
                startPoint = { x: bounds.left + offset, y: bounds.bottom };
                centerPoint = { x: bounds.centerX, y: bounds.bottom };
                endPoint = { x: bounds.right - offset, y: bounds.bottom };
                break;
            case 'left':
                startPoint = { x: bounds.left, y: bounds.top + offset };
                centerPoint = { x: bounds.left, y: bounds.centerY };
                endPoint = { x: bounds.left, y: bounds.bottom - offset };
                break;
            case 'right':
                startPoint = { x: bounds.right, y: bounds.top + offset };
                centerPoint = { x: bounds.right, y: bounds.centerY };
                endPoint = { x: bounds.right, y: bounds.bottom - offset };
                break;
        }

        // Create 3 points: start, center, end
        points.push({
            id: `${markerId}-${side}-start-${type}`,
            name: `${markerId}-${side}-start-${type}`,
            type,
            side,
            position: startPoint,
            connected: false,
            edgePosition: {
                isOnEdge: true,
                edgeTolerance: 0,
                edgeSide: side,
                edgeCoordinate: side === 'top' || side === 'bottom' ? startPoint.x : startPoint.y
            },
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        });

        points.push({
            id: `${markerId}-${side}-center-${type}`,
            name: `${markerId}-${side}-center-${type}`,
            type,
            side,
            position: centerPoint,
            connected: false,
            edgePosition: {
                isOnEdge: true,
                edgeTolerance: 0,
                edgeSide: side,
                edgeCoordinate: side === 'top' || side === 'bottom' ? centerPoint.x : centerPoint.y
            },
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        });

        points.push({
            id: `${markerId}-${side}-end-${type}`,
            name: `${markerId}-${side}-end-${type}`,
            type,
            side,
            position: endPoint,
            connected: false,
            edgePosition: {
                isOnEdge: true,
                edgeTolerance: 0,
                edgeSide: side,
                edgeCoordinate: side === 'top' || side === 'bottom' ? endPoint.x : endPoint.y
            },
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        });

        return points;
    }

    /**
     * Buduj stany połączeń
     */
    private buildConnectionStates(
        connections: Array<{ from: string; to: string; type?: string }>,
        nodeStates: VisualNodeState[]
    ): Map<string, VisualConnectionState> {
        const shouldLogBuild = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogBuild) {
            console.log(`[VisualSemanticModelManager] 🔄 buildConnectionStates: START with ${connections.length} connections`);
        }
        const connectionStates = new Map<string, VisualConnectionState>();
        const nodeStatesMap = new Map(nodeStates.map(ns => [ns.nodeId, ns]));
        console.log(`[VisualSemanticModelManager] 📊 nodeStatesMap.size = ${nodeStatesMap.size}`);

        // Track which connection points are already used by other connections
        // nodeId -> Set<pointId> for inputs and outputs separately
        const usedInputPoints = new Map<string, Set<string>>();
        const usedOutputPoints = new Map<string, Set<string>>();

        // Track incoming connection sides for each node (to prefer opposite side for outputs)
        // nodeId -> Set<Side> - sides from which connections enter
        const incomingSides = new Map<string, Set<Side>>();

        // FAZA 1: Zbuduj wszystkie podstawowe ścieżki
        // NOTE: W tej fazie nie aktualizujemy semanticSelection, bo nodeStates mogą być niekompletne
        // Aktualizacja nastąpi w FAZIE 2, gdy wszystkie node states są już zbudowane
        const basicPaths = new Map<string, Point[]>();

        connections.forEach((conn, index) => {
            // Check if connection involves markers
            const markerInfo = this.getMarkerConnectionInfo(conn);

            // Skip invalid connections (ale nie workflow-start/end)
            if (!markerInfo.isMarkerConnection) {
                const fromNode = nodeStatesMap.get(conn.from);
                const toNode = nodeStatesMap.get(conn.to);

                if (!fromNode || !toNode) {
                    console.warn(`[VisualSemanticModelManager] Skipping invalid connection: ${conn.from} -> ${conn.to}`);
                    return; // Skip invalid connections
                }
            }

            // Dla workflow-start/end, musimy użyć connection points z markerów
            if (markerInfo.isMarkerConnection) {
                // Use original ID from ConnectionManager if available, otherwise generate one
                const connectionId = (conn as any).id || `conn-${conn.from}-${conn.to}-${index}`;

                // Calculate connection points for markers
                const connectionPoints = this.calculateMarkerConnectionPoints(conn, markerInfo, nodeStatesMap);
                if (!connectionPoints) {
                    return; // Skip if connection points cannot be calculated
                }

                // Build connection state
                const connectionState = this.buildMarkerConnectionState(
                    conn,
                    connectionId,
                    connectionPoints,
                    nodeStates,
                    connectionStates,
                    nodeStatesMap
                );

                if (connectionState) {
                    console.log(`[VisualSemanticModelManager] ✅ Adding marker connection: ${connectionId} (${conn.from} -> ${conn.to})`);
                    connectionStates.set(connectionId, connectionState);
                }

                return; // Skip dalsze przetwarzanie dla markerów
            }

            const fromNode = nodeStatesMap.get(conn.from);
            const toNode = nodeStatesMap.get(conn.to);

            if (!fromNode || !toNode) {
                return;
            }

            // CRITICAL: Find incoming connection side for output selection
            // Check if there's an incoming connection to fromNode
            const incomingConnection = Array.from(connections)
                .find(c => c.to === fromNode.nodeId && c.from !== conn.from);
            let incomingSide: Side | undefined;
            if (incomingConnection) {
                // Try to find the connection state for incoming connection (if already built)
                const incomingConnId = (incomingConnection as any).id || `conn-${incomingConnection.from}-${incomingConnection.to}`;
                const incomingState = connectionStates.get(incomingConnId);
                incomingSide = incomingState?.path?.spatialMetadata?.endSide;
            }

            // Use original ID from ConnectionManager if available, otherwise generate one
            const connectionId = (conn as any).id || `conn-${conn.from}-${conn.to}-${index}`;

            // CRITICAL FIX 4: NIE nadpisuj istniejących connections jeśli już były przeliczony
            // buildConnectionStates() NIGDY nie może nadpisywać istniejącego ConnectionState
            if (this.model?.workflow.connections.has(connectionId)) {
                const existing = this.model.workflow.connections.get(connectionId);
                // Jeśli connection ma już poprawny path (>=2 punkty i są różne), nie nadpisuj
                if (existing && existing.path && existing.path.points && existing.path.points.length >= 2) {
                    // Sprawdź czy punkty są różne (nie identyczne)
                    const first = existing.path.points[0];
                    const last = existing.path.points[existing.path.points.length - 1];
                    const pointsAreDifferent = Math.abs(first.x - last.x) > 0.1 || Math.abs(first.y - last.y) > 0.1;

                    if (pointsAreDifferent) {
                        connectionStates.set(connectionId, existing);
                        console.log(`[VisualSemanticModelManager] ⏭️  Skipping connection ${connectionId} - already computed with ${existing.path.points.length} points`);
                        return; // Skip - connection already computed
                    }
                }
            }

            // CRITICAL: Use selectBestConnectionPoint to ensure different connections use different points
            // For outputs, prefer opposite side to incoming connection
            // SEMANTIC MODEL: Pass all nodes and connections for spatial analysis
            const fromPoint = this.selectBestConnectionPoint(
                fromNode,
                toNode,
                'output',
                usedOutputPoints.get(fromNode.nodeId) || new Set(),
                incomingSide,  // Pass incoming side to prefer opposite for output
                nodeStates,  // All nodes in space
                connections,  // All connections for collision detection
                connectionId  // Pass connectionId for metadata
            );
            const toPoint = this.selectBestConnectionPoint(
                toNode,
                fromNode,
                'input',
                usedInputPoints.get(toNode.nodeId) || new Set(),
                undefined,  // No incoming side for inputs
                nodeStates,  // All nodes in space
                connections,  // All connections for collision detection
                connectionId  // Pass connectionId for metadata
            );

            // CRITICAL: Verify toPoint was selected and has metadata
            const shouldLogSelection = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLogSelection) {
                if (!toPoint) {
                    console.error(`[VisualSemanticModelManager] ❌ CRITICAL: selectBestConnectionPoint returned null for toPoint (input) on node ${toNode.nodeId} for connection ${connectionId}`);
                } else if (!toPoint.semanticSelection || !toPoint.semanticSelection.isSelected) {
                    console.error(`[VisualSemanticModelManager] ❌ CRITICAL: toPoint ${toPoint.id} selected but semanticSelection.isSelected is false for connection ${connectionId}`);
                } else {
                    console.log(`[VisualSemanticModelManager] ✅ toPoint ${toPoint.id} selected with isSelected=true for connection ${connectionId}`);
                }
            }

            if (!fromPoint || !toPoint) {
                return; // Skip if no connection points
            }

            // Mark these points as used
            if (fromPoint) {
                if (!usedOutputPoints.has(fromNode.nodeId)) {
                    usedOutputPoints.set(fromNode.nodeId, new Set());
                }
                usedOutputPoints.get(fromNode.nodeId)!.add(fromPoint.id);
            }
            if (toPoint) {
                if (!usedInputPoints.has(toNode.nodeId)) {
                    usedInputPoints.set(toNode.nodeId, new Set());
                }
                usedInputPoints.get(toNode.nodeId)!.add(toPoint.id);
            }

            // Validate edge positions
            const startPointOnEdge = fromPoint.edgePosition.isOnEdge;
            const endPointOnEdge = toPoint.edgePosition.isOnEdge;
            const startPointTolerance = fromPoint.edgePosition.edgeTolerance;
            const endPointTolerance = toPoint.edgePosition.edgeTolerance;

            // Calculate absolute positions
            const fromAbsolute = this.toAbsolutePosition(fromPoint.position, fromNode.position);
            const toAbsolute = this.toAbsolutePosition(toPoint.position, toNode.position);

            // Zawsze używamy orthogonal routing
            const routingType = this.selectRoutingType();

            // CRITICAL: Set node bounds for collision detection (before routing)
            this.connectionRouter.clearNodeBounds();
            nodeStates.forEach(node => {
                this.connectionRouter.setNodeBounds(node.nodeId, {
                    x: node.position.x,
                    y: node.position.y,
                    width: node.computedBounds.width,
                    height: node.computedBounds.height
                });
            });

            // PROSTY routing - tylko center points, tylko orthogonal, z unikaniem kolizji
            const excludeNodes = new Set<string>([fromNode.nodeId, toNode.nodeId]);
            const path = this.connectionRouter.route(
                fromAbsolute,
                toAbsolute,
                fromPoint.side,
                toPoint.side,
                excludeNodes
            );

            basicPaths.set(connectionId, path);
        });

        // FAZA 2: Użyj podstawowych ścieżek (orthogonal routing bez unikania przecięć)
        // Uproszczone: używamy podstawowych ścieżek bez dodatkowej optymalizacji
        connections.forEach((conn, index) => {
            const fromNode = nodeStatesMap.get(conn.from);
            const toNode = nodeStatesMap.get(conn.to);

            if (!fromNode || !toNode) {
                return;
            }

            // CRITICAL: Find incoming connection side for output selection
            // If there's an incoming connection to fromNode, use its input side to determine output side
            // Check all connections that end at fromNode
            const incomingConnections = Array.from(connectionStates.values())
                .filter(c => c.toNodeId === fromNode.nodeId);

            // Use the most recent incoming connection's side, or check if there are any incoming connections
            const incomingSide = incomingConnections.length > 0
                ? incomingConnections[incomingConnections.length - 1]?.path?.spatialMetadata?.endSide
                : undefined;

            // Also check if there are incoming connections in the current batch (not yet in connectionStates)
            // by looking at connections array
            if (!incomingSide) {
                const incomingConn = connections.find(c => c.to === fromNode.nodeId && c.from !== conn.from);
                if (incomingConn) {
                    // Try to find the connection state for incoming connection (if already built)
                    const incomingConnId = (incomingConn as any).id || `conn-${incomingConn.from}-${incomingConn.to}`;
                    const incomingState = connectionStates.get(incomingConnId);
                    if (incomingState?.path?.spatialMetadata?.endSide) {
                        // Use this side
                    }
                }
            }

            // Use original ID from ConnectionManager if available, otherwise generate one
            const connectionId = (conn as any).id || `conn-${conn.from}-${conn.to}-${index}`;

            // CRITICAL FIX 4: NIE nadpisuj istniejących connections jeśli już były przeliczony
            // buildConnectionStates() NIGDY nie może nadpisywać istniejącego ConnectionState
            if (this.model?.workflow.connections.has(connectionId)) {
                const existing = this.model.workflow.connections.get(connectionId);
                // Jeśli connection ma już poprawny path (>=2 punkty i są różne), nie nadpisuj
                if (existing && existing.path && existing.path.points && existing.path.points.length >= 2) {
                    // Sprawdź czy punkty są różne (nie identyczne)
                    const first = existing.path.points[0];
                    const last = existing.path.points[existing.path.points.length - 1];
                    const pointsAreDifferent = Math.abs(first.x - last.x) > 0.1 || Math.abs(first.y - last.y) > 0.1;

                    if (pointsAreDifferent) {
                        connectionStates.set(connectionId, existing);
                        console.log(`[VisualSemanticModelManager] ⏭️  Skipping connection ${connectionId} - already computed with ${existing.path.points.length} points`);
                        return; // Skip - connection already computed
                    }
                }
            }

            // CRITICAL: Automatically select best connection points based on node positions
            // Choose the side that minimizes path length and avoids obstacles
            // IMPORTANT: Ensure different connections use different points
            // For outputs, prefer opposite side to incoming connection
            // SEMANTIC MODEL: Pass all nodes and connections for spatial analysis
            const fromPoint = this.selectBestConnectionPoint(
                fromNode,
                toNode,
                'output',
                usedOutputPoints.get(fromNode.nodeId) || new Set(),
                incomingSide,  // Pass incoming side to prefer opposite for output
                nodeStates,  // All nodes in space
                connections,  // All connections for collision detection
                connectionId  // CRITICAL: Pass connectionId for metadata
            );
            const toPoint = this.selectBestConnectionPoint(
                toNode,
                fromNode,
                'input',
                usedInputPoints.get(toNode.nodeId) || new Set(),
                undefined,  // No incoming side for inputs
                nodeStates,  // All nodes in space
                connections,  // All connections for collision detection
                connectionId  // CRITICAL: Pass connectionId for metadata
            );

            // Mark these points as used
            if (fromPoint) {
                if (!usedOutputPoints.has(fromNode.nodeId)) {
                    usedOutputPoints.set(fromNode.nodeId, new Set());
                }
                usedOutputPoints.get(fromNode.nodeId)!.add(fromPoint.id);
            }
            if (toPoint) {
                if (!usedInputPoints.has(toNode.nodeId)) {
                    usedInputPoints.set(toNode.nodeId, new Set());
                }
                usedInputPoints.get(toNode.nodeId)!.add(toPoint.id);
            }

            if (!fromPoint || !toPoint) {
                return;
            }

            // CRITICAL: Verify that semanticSelection metadata is set for both points
            // CRITICAL: Verify that both points are CENTER points (strzałki mogą prowadzić tylko do środkowych punktów)
            // PAMIĘTAJ: Na każdej krawędzi są 3 punkty: dwa skrajne (start, end) i jeden środkowy (center)
            // Linia ma wychodzić/wchodzić TYLKO z/do środkowego punktu!
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                if (!fromPoint.id.includes('-center')) {
                    console.error(`[VisualSemanticModelManager] ❌❌❌ CRITICAL: fromPoint ${fromPoint.id} for connection ${connectionId} is NOT a center point! Only center points should be used.`);
                }
                if (!toPoint.id.includes('-center')) {
                    console.error(`[VisualSemanticModelManager] ❌❌❌ CRITICAL: toPoint ${toPoint.id} for connection ${connectionId} is NOT a center point! Only center points should be used.`);
                }
                if (!fromPoint.semanticSelection || !fromPoint.semanticSelection.isSelected) {
                    console.error(`[VisualSemanticModelManager] ❌ CRITICAL: fromPoint ${fromPoint.id} for connection ${connectionId} does not have semanticSelection.isSelected=true`);
                }
                if (!toPoint.semanticSelection || !toPoint.semanticSelection.isSelected) {
                    console.error(`[VisualSemanticModelManager] ❌ CRITICAL: toPoint ${toPoint.id} for connection ${connectionId} does not have semanticSelection.isSelected=true`);
                }
            }

            // CRITICAL: Double-check that we're using center points - if not, this is a BUG!
            if (!fromPoint.id.includes('-center') || !toPoint.id.includes('-center')) {
                console.error(`[VisualSemanticModelManager] ❌❌❌ FATAL ERROR: Connection ${connectionId} is NOT using center points!`, {
                    fromPointId: fromPoint.id,
                    toPointId: toPoint.id,
                    fromIsCenter: fromPoint.id.includes('-center'),
                    toIsCenter: toPoint.id.includes('-center')
                });
                // Don't proceed - this connection should not be created!
                return;
            }

            const startPointOnEdge = fromPoint.edgePosition.isOnEdge;
            const endPointOnEdge = toPoint.edgePosition.isOnEdge;
            const startPointTolerance = fromPoint.edgePosition.edgeTolerance;
            const endPointTolerance = toPoint.edgePosition.edgeTolerance;

            // CRITICAL: Calculate absolute positions in world/SVG coords (model space)
            // toAbsolutePosition returns positions in world/SVG user units (source of truth)
            const fromAbsolute = this.toAbsolutePosition(fromPoint.position, fromNode.position);
            const toAbsolute = this.toAbsolutePosition(toPoint.position, toNode.position);

            // CRITICAL: Konwersja do SVG coords - tylko jeśli SVG i anchor elements istnieją
            // Jeśli nie, connection będzie oznaczony jako pending i przeliczony później
            const svgElement = (this.designer as any).svgElement as SVGSVGElement | undefined;

            let fromAbsoluteSvg: Point | null = null;
            let toAbsoluteSvg: Point | null = null;
            let isPending = false;

            if (svgElement) {
                // CRITICAL: Use registered anchor elements (from registerAnchor)
                const fromAnchorDiv = this.getAnchorElement(fromNode.nodeId, fromPoint.id);
                const toAnchorDiv = this.getAnchorElement(toNode.nodeId, toPoint.id);

                if (fromAnchorDiv && toAnchorDiv) {
                    // Both anchors exist - convert to SVG coords using getBoundingClientRect
                    fromAbsoluteSvg = getAnchorSvgPosition(fromAnchorDiv, svgElement);
                    toAbsoluteSvg = getAnchorSvgPosition(toAnchorDiv, svgElement);
                } else {
                    // CRITICAL: Anchor elements don't exist yet - mark as pending
                    // DO NOT use fallback coords - they may be in wrong coordinate space!
                    // Wait for anchors to be registered and recalculate later
                    isPending = true;

                    console.log(`[VisualSemanticModelManager] ⚠️ Connection ${connectionId} marked as pending (will recalculate when anchors ready):`, {
                        fromAnchorExists: !!fromAnchorDiv,
                        toAnchorExists: !!toAnchorDiv,
                        fromNodeId: fromNode.nodeId,
                        toNodeId: toNode.nodeId,
                        fromPointId: fromPoint.id,
                        toPointId: toPoint.id
                    });

                    // Set temporary coords (will be recalculated)
                    fromAbsoluteSvg = { x: fromAbsolute.x, y: fromAbsolute.y };
                    toAbsoluteSvg = { x: toAbsolute.x, y: toAbsolute.y };
                }
            } else {
                // SVG not ready - mark as pending
                fromAbsoluteSvg = { x: fromAbsolute.x, y: fromAbsolute.y };
                toAbsoluteSvg = { x: toAbsolute.x, y: toAbsolute.y };
                isPending = true;

                console.log(`[VisualSemanticModelManager] ⚠️ Connection ${connectionId} marked as pending: SVG element not ready`);
            }

            // CRITICAL: If pending, mark for later recalculation and SKIP creating connection state
            // Model semantyczny jest źródłem prawdy - nie tworzymy connection state z błędnymi współrzędnymi
            // Connection state zostanie utworzony w _recalculateSingleConnection gdy anchors będą gotowe
            if (isPending) {
                this.pendingConnections.add(connectionId);
                // CRITICAL: Store connection data so we can recalculate it later
                this.pendingConnectionsData.set(connectionId, {
                    from: conn.from,
                    to: conn.to,
                    type: conn.type,
                    id: connectionId
                });
                // CRITICAL: Update connectionsByNode for cache invalidation (even for pending)
                if (!this.connectionsByNode.has(fromNode.nodeId)) {
                    this.connectionsByNode.set(fromNode.nodeId, new Set());
                }
                if (!this.connectionsByNode.has(toNode.nodeId)) {
                    this.connectionsByNode.set(toNode.nodeId, new Set());
                }
                this.connectionsByNode.get(fromNode.nodeId)!.add(connectionId);
                this.connectionsByNode.get(toNode.nodeId)!.add(connectionId);

                console.log(`[VisualSemanticModelManager] ⏳ Skipping connection ${connectionId} - will be created when anchors ready`);
                return; // Skip creating connection state - will be created in _recalculateSingleConnection
            }

            // CRITICAL: Model semantyczny decyduje o routingu - JEDYNE źródło prawdy!
            // Zawsze używamy orthogonal routing dla spójności wizualnej
            const routingType = this.selectRoutingType();

            // CRITICAL: Routing jest wykonywany w modelu semantycznym
            // connectionPathRouter.route() oblicza wszystkie punkty ścieżki na podstawie:
            // - Pozycji węzłów (z modelu semantycznego)
            // - Connection points (z modelu semantycznego) - TERAZ W PRZESTRZENI SVG!
            // - Typu routingu (zdecydowanego przez model semantyczny)

            // CRITICAL: Only route if we have valid SVG positions from anchor elements
            if (!fromAbsoluteSvg || !toAbsoluteSvg) {
                console.warn(`[VisualSemanticModelManager] ⚠️ Cannot route connection ${connectionId}: missing SVG positions`);
                return;
            }

            // CRITICAL: Set node bounds for collision detection (before routing)
            this.connectionRouter.clearNodeBounds();
            nodeStates.forEach(node => {
                this.connectionRouter.setNodeBounds(node.nodeId, {
                    x: node.position.x,
                    y: node.position.y,
                    width: node.computedBounds.width,
                    height: node.computedBounds.height
                });
            });

            // MODEL SEMANTYCZNY oblicza ścieżkę - PROSTY router (używa pozycji w SVG)
            const excludeNodes = new Set<string>([fromNode.nodeId, toNode.nodeId]);
            let path = this.connectionRouter.route(
                fromAbsoluteSvg,
                toAbsoluteSvg,
                fromPoint.side,
                toPoint.side,
                excludeNodes
            );

            // PROSTY: Oblicz arrow metadata z ostatniego segmentu
            const pathEndPoint = path.length > 0 ? path[path.length - 1] : toAbsoluteSvg;
            const secondLast = path.length > 1 ? path[path.length - 2] : fromAbsoluteSvg;

            const dx = pathEndPoint.x - secondLast.x;
            const dy = pathEndPoint.y - secondLast.y;
            const length = Math.sqrt(dx * dx + dy * dy);
            const normalized = length > 0 ? { x: dx / length, y: dy / length } : { x: 1, y: 0 };
            const angle = Math.atan2(normalized.y, normalized.x);

            const arrowMetadata = {
                endPoint: pathEndPoint,
                direction: normalized,
                angle: angle,
                pointsTowardsNode: true,
                lastSegmentDirection: normalized,
                spatialInfo: {
                    arrowTipPosition: pathEndPoint,
                    arrowBaseCenter: {
                        x: pathEndPoint.x - Math.cos(angle) * 10,
                        y: pathEndPoint.y - Math.sin(angle) * 10
                    },
                    arrowSize: 10,
                    distanceToTargetCenter: Math.sqrt(
                        Math.pow(toNode.computedBounds.centerX - pathEndPoint.x, 2) +
                        Math.pow(toNode.computedBounds.centerY - pathEndPoint.y, 2)
                    ),
                    angleDegrees: angle * 180 / Math.PI,
                    isHorizontal: Math.abs(normalized.y) < 0.1,
                    isVertical: Math.abs(normalized.x) < 0.1,
                    quadrant: 1 as 1 | 2 | 3 | 4
                }
            };

            const arrowPointsTowardsTarget = true; // PROSTY: zawsze true

            // CRITICAL: Zapisuj pełną informację o ścieżce w modelu semantycznym
            // Model semantyczny zawiera WSZYSTKIE dane potrzebne do renderowania:
            // - points: wszystkie punkty ścieżki (x,y dla każdego punktu)
            // - routingType: typ routingu użyty do obliczenia ścieżki
            // - arrowMetadata: metadane strzałki (kierunek, pozycja)
            // - spatialMetadata: wszystkie metadane przestrzenne (długość, kolizje, etc.)
            // To jest JEDYNE źródło prawdy - ConnectionRenderer używa tylko tych danych!
            // CRITICAL: Update connectionsByNode for cache invalidation
            if (!this.connectionsByNode.has(fromNode.nodeId)) {
                this.connectionsByNode.set(fromNode.nodeId, new Set());
            }
            if (!this.connectionsByNode.has(toNode.nodeId)) {
                this.connectionsByNode.set(toNode.nodeId, new Set());
            }
            this.connectionsByNode.get(fromNode.nodeId)!.add(connectionId);
            this.connectionsByNode.get(toNode.nodeId)!.add(connectionId);

            // CRITICAL: Remove from pending if successfully built
            if (!isPending) {
                this.pendingConnections.delete(connectionId);
            }

            connectionStates.set(connectionId, {
                connectionId,
                fromNodeId: conn.from,
                toNodeId: conn.to,
                type: (conn.type as 'sequential' | 'conditional' | 'error') || 'sequential',
                path: {
                    points: path, // PEŁNA ścieżka z wszystkich punktów - model decyduje o każdym punkcie!
                    routingType: routingType, // Typ routingu zdecydowany przez model semantyczny
                    arrowMetadata: arrowMetadata, // Metadane strzałki obliczone przez model semantyczny
                    spatialMetadata: {
                        totalLength: this.calculatePathLength(path),
                        segmentCount: path.length - 1,
                        minDistanceToNode: this.calculateMinDistanceToNodes(path, nodeStates),
                        crossesOtherConnections: this.detectPathCrossings(path, connectionStates, connectionId),
                        boundingBox: this.calculatePathBoundingBox(path),
                        startSide: fromPoint.side,
                        endSide: toPoint.side,
                        // Enhanced path information - CRITICAL for semantic model
                        startPoint: path[0],
                        endPoint: path[path.length - 1],
                        turningPointsCount: this.countTurningPoints(path),
                        turningPoints: this.getTurningPoints(path),
                        passesThroughNodes: this.detectPathNodeIntersections(path, nodeStates),
                        labelCollisions: this.detectLabelCollisions(path, nodeStates),
                        // Detailed segment information
                        segments: this.analyzePathSegments(path),
                        // Collision detection results
                        hasCollisions: this.detectPathNodeIntersections(path, nodeStates).length > 0 ||
                            this.detectLabelCollisions(path, nodeStates).length > 0 ||
                            this.detectPathCrossings(path, connectionStates, connectionId),
                        collisionDetails: {
                            nodeIntersections: this.detectPathNodeIntersections(path, nodeStates),
                            labelCollisions: this.detectLabelCollisions(path, nodeStates),
                            connectionCrossings: this.detectPathCrossings(path, connectionStates, connectionId)
                        }
                    }
                },
                anchors: {
                    from: {
                        nodeId: conn.from,
                        connectionPointId: fromPoint.id,
                        position: fromAbsoluteSvg
                    },
                    to: {
                        nodeId: conn.to,
                        connectionPointId: toPoint.id,
                        position: toAbsoluteSvg
                    }
                },
                visualState: {
                    selected: false,
                    hovered: false,
                    highlighted: false,
                    error: false
                },
                edgeValidation: {
                    startPointOnEdge,
                    endPointOnEdge,
                    startPointTolerance,
                    endPointTolerance,
                    arrowPointsTowardsTarget
                },
                spatialValidation: {
                    pathLength: this.calculatePathLength(path),
                    isValidPath: path.length >= 2 && this.validatePath(path),
                    startNodeDistance: this.calculateDistance(fromAbsoluteSvg, fromNode.computedBounds),
                    endNodeDistance: this.calculateDistance(toAbsoluteSvg, toNode.computedBounds),
                    minClearance: this.calculateMinClearance(path, nodeStates),
                    routingQuality: this.assessRoutingQuality(path, fromNode, toNode)
                },
                pathGeometry: this.calculatePathGeometry(path, connectionId, connectionStates),
                renderCache: {
                    svgPath: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            });
        });

        console.log(`[VisualSemanticModelManager] buildConnectionStates: Built ${connectionStates.size} connection states`);
        console.log(`[VisualSemanticModelManager] buildConnectionStates: Connection IDs:`, Array.from(connectionStates.keys()));

        // Sprawdź czy wszystkie wartości są poprawne
        connectionStates.forEach((state, id) => {
            if (!state) {
                console.error(`[VisualSemanticModelManager] ❌ Connection state for ${id} is null/undefined!`);
            }
        });

        // Count how many connection points have semanticSelection metadata
        let totalPointsWithSelection = 0;
        let totalPoints = 0;
        nodeStates.forEach(nodeState => {
            const allPoints = [...nodeState.connectionPoints.inputs, ...nodeState.connectionPoints.outputs];
            totalPoints += allPoints.length;
            const selected = allPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
            totalPointsWithSelection += selected.length;
        });

        const shouldLogEnd = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogEnd) {
            console.log(`[VisualSemanticModelManager] ✅ buildConnectionStates: END with ${connectionStates.size} connection states, ${totalPointsWithSelection}/${totalPoints} connection points have semanticSelection.isSelected=true`);
        }
        console.log(`[VisualSemanticModelManager] 📊 connectionStates keys:`, Array.from(connectionStates.keys()));

        // Sprawdź czy wszystkie wartości są poprawne
        connectionStates.forEach((state, key) => {
            if (!state || !state.fromNodeId || !state.toNodeId) {
                console.error(`[VisualSemanticModelManager] ❌ Invalid connection state for key ${key}:`, state);
            }
        });

        return connectionStates;
    }

    /**
     * Buduj layout state
     */
    private buildLayoutState(
        viewportInfo: any,
        nodeStates: VisualNodeState[],
        connectionStates: Map<string, VisualConnectionState>,
        metadata: any
    ): VisualSemanticModel['layout'] {
        const viewport: ViewportState = {
            pan: viewportInfo.pan,
            zoom: viewportInfo.zoom,
            bounds: viewportInfo.viewportSize
        };

        const nodePositions = new Map<string, NodePositionState>();
        nodeStates.forEach(ns => {
            nodePositions.set(ns.nodeId, {
                position: ns.position,
                dimensions: ns.dimensions,
                depth: ns.depth,
                isRoot: ns.isRoot,
                parentId: ns.parentId
            });
        });

        const connectionPaths = new Map<string, ConnectionPathState>();
        connectionStates.forEach((cs, id) => {
            connectionPaths.set(id, {
                points: cs.path.points,
                routingType: cs.path.routingType,
                bezierControlPoints: cs.path.bezierControlPoints,
                arrowMetadata: cs.path.arrowMetadata || undefined, // Include arrowMetadata
                spatialMetadata: cs.path.spatialMetadata // CRITICAL: Include spatial metadata
            });
        });

        return {
            viewport,
            nodePositions,
            connectionPaths
        };
    }

    /**
     * Buduj UI state
     */
    private buildUIState(): UIState {
        // Access private state from VisualDesigner
        const selectedNodeId = (this.designer as any).selectedNodeId as string | null;
        const selectedNodeIds = (this.designer as any).selectedNodeIds as string[] || [];
        const hoveredNodeId = (this.designer as any).hoveredNodeId as string | null;

        return {
            selectedNodeIds: new Set(selectedNodeIds.length > 0 ? selectedNodeIds : (selectedNodeId ? [selectedNodeId] : [])),
            hoveredNodeId: hoveredNodeId,
            expandedContainers: new Set(), // TODO: Track expanded containers
            collapsedContainers: new Set(), // TODO: Track collapsed containers
            activeTool: null, // TODO: Track active tool
            marqueeSelection: null // TODO: Track marquee selection
        };
    }

    /**
     * Wyciągnij zmienne z węzła
     */
    private extractNodeVariables(node: VisualNode): { defined: string[]; used: string[] } {
        const defined: string[] = [];
        const used: string[] = [];

        // Check if node defines variables (DEF, VAR)
        if (node.type.toLowerCase() === 'def' || node.type.toLowerCase() === 'var') {
            const varName = node.attributes.var || node.attributes.name;
            if (varName) {
                defined.push(varName);
            }
        }

        // Extract used variables from attributes and body
        const referencedVars = this.variableAnalyzer.extractVariableReferences(node);
        used.push(...referencedVars);

        return { defined, used };
    }

    /**
     * Znajdź rodzica węzła
     */
    private findParentNode(node: VisualNode, rootNodes: VisualNode[]): VisualNode | null {
        for (const root of rootNodes) {
            if (root.children && root.children.some(child => child.id === node.id)) {
                return root;
            }
            if (root.children && root.children.length > 0) {
                const found = this.findParentNode(node, root.children);
                if (found) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Oblicz głębokość węzła
     */
    private getNodeDepth(node: VisualNode, rootNodes: VisualNode[]): number {
        const findDepth = (nodeList: VisualNode[], targetId: string, currentDepth: number): number | null => {
            for (const n of nodeList) {
                if (n.id === targetId) {
                    return currentDepth;
                }
                if (n.children && n.children.length > 0) {
                    const depth = findDepth(n.children, targetId, currentDepth + 1);
                    if (depth !== null) {
                        return depth;
                    }
                }
            }
            return null;
        };

        return findDepth(rootNodes, node.id, 0) ?? 0;
    }

    /**
     * Pobierz rozmiary węzła
     */
    private getNodeDimensions(node: VisualNode): { width: number; height: number } {
        // Try to get from metadata
        const metadata = { nodes: [] }; // TODO: Get from designer
        const nodeMeta = (metadata.nodes || []).find((n: any) => n.id === node.id);
        if (nodeMeta && (nodeMeta as any).dimensions) {
            return (nodeMeta as any).dimensions;
        }

        // Fallback to default dimensions
        const plugin = this.findPlugin(node.type);
        return {
            width: plugin?.defaultWidth || 200,
            height: plugin?.defaultHeight || 100
        };
    }

    /**
     * Oblicz bounds węzła
     */
    private calculateBounds(position: Point, dimensions: { width: number; height: number }): Bounds {
        return {
            left: position.x,
            right: position.x + dimensions.width,
            top: position.y,
            bottom: position.y + dimensions.height,
            width: dimensions.width,
            height: dimensions.height,
            centerX: position.x + dimensions.width / 2,
            centerY: position.y + dimensions.height / 2
        };
    }

    /**
     * Konwertuj relative position na absolute
     * CRITICAL: Connection points must be exactly on the edge of the label
     * nodePosition.y is the top edge of the node (which is also the top edge of the label)
     * For 'top' side: y = nodePosition.y (exactly on label top edge)
     * For 'bottom' side: y = nodePosition.y + dimensions.height (exactly on node bottom edge)
     * For 'left'/'right' sides: x = nodePosition.x or nodePosition.x + width (exactly on label edges)
     */
    private toAbsolutePosition(relative: Point, nodePosition: Point): Point {
        // CRITICAL: Ensure points are exactly on edges
        // relative coordinates are already snapped to edges in ConnectionPointsCalculator
        // Just add node position offset
        return {
            x: nodePosition.x + relative.x,
            y: nodePosition.y + relative.y
        };
    }

    /**
     * Pobierz visual state węzła
     */
    private getNodeVisualState(node: VisualNode): VisualNodeState['visualState'] {
        const selectedNodeId = (this.designer as any).selectedNodeId as string | null;
        const selectedNodeIds = (this.designer as any).selectedNodeIds as string[] || [];
        const hoveredNodeId = (this.designer as any).hoveredNodeId as string | null;

        return {
            selected: selectedNodeIds.includes(node.id) || selectedNodeId === node.id,
            hovered: hoveredNodeId === node.id,
            expanded: false, // TODO: Track expanded state
            collapsed: false, // TODO: Track collapsed state
            dragging: false, // TODO: Track dragging state
            resizing: false, // TODO: Track resizing state
            error: false, // TODO: Track error state
            warning: false // TODO: Track warning state
        };
    }

    /**
     * Pobierz workflow ID
     */
    private getWorkflowId(): string {
        // TODO: Get from actual workflow context
        return 'workflow-1';
    }

    /**
     * Aktualizuj cache connection points dla węzła
     * CRITICAL: Stores references to connection points so semanticSelection updates work
     */
    private updateConnectionPointsCache(nodeId: string, connectionPoints: ConnectionPoint[], nodePosition: Point): void {
        // CRITICAL: If cache already exists, merge new points with existing cache to preserve semanticSelection
        // This is important because buildNodeStates is called before buildConnectionStates,
        // so we need to preserve metadata that was set in buildConnectionStates
        const existingCache = this.connectionPointsCache.get(nodeId);
        if (existingCache && existingCache.length > 0) {
            // CRITICAL: Check if any points in existing cache have semanticSelection.isSelected=true
            // If so, we must preserve them
            const hasSelectedPoints = existingCache.some(cp => cp.semanticSelection && cp.semanticSelection.isSelected);

            if (hasSelectedPoints) {
                // CRITICAL: Instead of replacing cache, update existing points with new positions
                // This preserves object references so semanticSelection updates are reflected
                connectionPoints.forEach(newPoint => {
                    const existingPoint = existingCache.find(cp => cp.id === newPoint.id);
                    if (existingPoint) {
                        // Update position but keep the same object reference (preserves semanticSelection)
                        existingPoint.position = newPoint.position;
                        existingPoint.edgePosition = newPoint.edgePosition;
                        // Update other properties that might have changed
                        if (newPoint.side !== existingPoint.side) {
                            existingPoint.side = newPoint.side;
                        }
                        if (newPoint.type !== existingPoint.type) {
                            existingPoint.type = newPoint.type;
                        }
                    } else {
                        // New point not in cache - add it
                        existingCache.push(newPoint);
                    }
                });
                // Remove points that are no longer in new connectionPoints
                const newPointIds = new Set(connectionPoints.map(p => p.id));
                const toRemove: number[] = [];
                existingCache.forEach((point, index) => {
                    if (!newPointIds.has(point.id)) {
                        toRemove.push(index);
                    }
                });
                toRemove.reverse().forEach(index => existingCache.splice(index, 1));

                // Use existing cache (with updated positions) instead of new connectionPoints
                // This preserves object references and semanticSelection metadata
                connectionPoints = existingCache;
            } else {
                // No selected points in cache - but still preserve semanticSelection if it exists
                // CRITICAL: Instead of copying metadata, update existing points to preserve references
                const updatedPoints: ConnectionPoint[] = [];
                connectionPoints.forEach(newPoint => {
                    const existingPoint = existingCache.find(cp => cp.id === newPoint.id);
                    if (existingPoint) {
                        // Update position but keep the same object reference (preserves semanticSelection)
                        existingPoint.position = newPoint.position;
                        existingPoint.edgePosition = newPoint.edgePosition;
                        // Update other properties that might have changed
                        if (newPoint.side !== existingPoint.side) {
                            existingPoint.side = newPoint.side;
                        }
                        if (newPoint.type !== existingPoint.type) {
                            existingPoint.type = newPoint.type;
                        }
                        // Use existing point instead of new point to preserve references
                        updatedPoints.push(existingPoint);
                    } else {
                        // New point not in cache - add it
                        updatedPoints.push(newPoint);
                    }
                });
                // Use updated points with preserved references
                connectionPoints = updatedPoints;
            }
        }

        // Cache relative positions - store references, not copies
        this.connectionPointsCache.set(nodeId, connectionPoints);

        // Cache absolute positions
        const absoluteMap = new Map<string, Point>();
        connectionPoints.forEach(point => {
            const absolute = this.toAbsolutePosition(point.position, nodePosition);
            absoluteMap.set(point.id, absolute);
        });
        this.connectionPointsAbsoluteCache.set(nodeId, absoluteMap);

        // CRITICAL: Also update connection points in model if it exists
        // This ensures semanticSelection updates are reflected in the model
        if (this.model) {
            const nodeState = this.model.workflow.allNodes.get(nodeId);
            if (nodeState) {
                // Update connection points in model to use same references as cache
                const inputPoints = connectionPoints.filter(cp => cp.type === 'input');
                const outputPoints = connectionPoints.filter(cp => cp.type === 'output');
                nodeState.connectionPoints.inputs = inputPoints;
                nodeState.connectionPoints.outputs = outputPoints;
            }
        }
    }

    /**
     * Pobierz absolute position connection point
     */
    getConnectionPointAbsolutePosition(nodeId: string, pointId: string): Point | null {
        // CRITICAL: Special handling for workflow markers
        if (nodeId === 'workflow-start' || nodeId === 'workflow-end') {
            // Handle null designer gracefully
            if (!this.designer) {
                return null;
            }
            const designerAny = this.designer as any;
            const startPos = designerAny?.startMarkerPosition;
            const endPos = designerAny?.endMarkerPosition;
            const MARKER_RADIUS = 30; // PROCESS_MARKER_RADIUS

            if (nodeId === 'workflow-start') {
                if (!startPos) return null;
                // Start marker: output from bottom center
                if (pointId === 'marker-output' || pointId.includes('output')) {
                    return {
                        x: startPos.x,
                        y: startPos.y + MARKER_RADIUS
                    };
                }
                // Start marker as target: input to top center
                if (pointId === 'marker-input' || pointId.includes('input')) {
                    return {
                        x: startPos.x,
                        y: startPos.y - MARKER_RADIUS
                    };
                }
                // Default: bottom center for start marker
                return {
                    x: startPos.x,
                    y: startPos.y + MARKER_RADIUS
                };
            }

            if (nodeId === 'workflow-end') {
                if (!endPos) return null;
                // End marker: input to top center
                if (pointId === 'marker-input' || pointId.includes('input')) {
                    return {
                        x: endPos.x,
                        y: endPos.y - MARKER_RADIUS
                    };
                }
                // End marker as source: output from bottom center
                if (pointId === 'marker-output' || pointId.includes('output')) {
                    return {
                        x: endPos.x,
                        y: endPos.y + MARKER_RADIUS
                    };
                }
                // Default: top center for end marker
                return {
                    x: endPos.x,
                    y: endPos.y - MARKER_RADIUS
                };
            }
        }

        // Regular nodes: use cache
        const absoluteMap = this.connectionPointsAbsoluteCache.get(nodeId);
        if (!absoluteMap) {
            // Try to rebuild cache if not available
            const node = this.findNodeById(nodeId);
            if (node) {
                const plugin = this.findPlugin(node.type);
                const position = node.position || { x: 0, y: 0 };
                const dimensions = this.getNodeDimensions(node);
                const connectionPoints = this.connectionPointsCalculator.calculate(
                    node,
                    plugin,
                    position,
                    dimensions
                );
                this.updateConnectionPointsCache(nodeId, connectionPoints, position);
                const rebuiltMap = this.connectionPointsAbsoluteCache.get(nodeId);
                return rebuiltMap?.get(pointId) || null;
            }
            return null;
        }
        return absoluteMap.get(pointId) || null;
    }

    /**
     * Pobierz connection point dla określonej strony i typu
     */
    getConnectionPointForSide(nodeId: string, side: Side, type: 'input' | 'output'): ConnectionPoint | null {
        const connectionPoints = this.connectionPointsCache.get(nodeId);
        if (!connectionPoints) {
            // Try to rebuild cache if not available
            const node = this.findNodeById(nodeId);
            if (node) {
                const plugin = this.findPlugin(node.type);
                const position = node.position || { x: 0, y: 0 };
                const dimensions = this.getNodeDimensions(node);
                const points = this.connectionPointsCalculator.calculate(
                    node,
                    plugin,
                    position,
                    dimensions
                );
                this.updateConnectionPointsCache(nodeId, points, position);
                const rebuiltPoints = this.connectionPointsCache.get(nodeId);
                if (rebuiltPoints) {
                    return rebuiltPoints.find(cp => cp.side === side && cp.type === type) || null;
                }
            }
            return null;
        }
        return connectionPoints.find(cp => cp.side === side && cp.type === type) || null;
    }

    /**
     * Pobierz wszystkie connection points dla węzła (absolute positions)
     */
    getConnectionPointsAbsolute(nodeId: string): Map<string, Point> | null {
        return this.connectionPointsAbsoluteCache.get(nodeId) || null;
    }

    /**
     * Znajdź węzeł po ID
     */
    private findNodeById(nodeId: string): VisualNode | null {
        const allNodes = this.designer.getAllNodes();
        return allNodes.find((n: VisualNode) => n.id === nodeId) || null;
    }

    /**
     * Wyczyść cache connection points (wywoływane przy zmianie pozycji węzła)
     */
    invalidateConnectionPointsCache(nodeId?: string): void {
        if (nodeId) {
            this.connectionPointsCache.delete(nodeId);
            this.connectionPointsAbsoluteCache.delete(nodeId);
        } else {
            this.connectionPointsCache.clear();
            this.connectionPointsAbsoluteCache.clear();
        }
    }

    /**
     * Subskrypcja zmian modelu
     */
    subscribe(listener: (model: VisualSemanticModel) => void): () => void {
        this.updateListeners.add(listener);
        return () => {
            this.updateListeners.delete(listener);
        };
    }

    /**
     * Calculate total path length
     */
    private calculatePathLength(path: Point[]): number {
        let total = 0;
        for (let i = 1; i < path.length; i++) {
            const dx = path[i].x - path[i - 1].x;
            const dy = path[i].y - path[i - 1].y;
            total += Math.sqrt(dx * dx + dy * dy);
        }
        return total;
    }

    /**
     * Calculate distance from point to node bounds center
     */
    private calculateDistance(point: Point, bounds: Bounds): number {
        const dx = point.x - bounds.centerX;
        const dy = point.y - bounds.centerY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate minimum distance from path to any node
     */
    private calculateMinDistanceToNodes(path: Point[], nodeStates: VisualNodeState[]): number {
        let minDist = Infinity;
        for (const point of path) {
            for (const node of nodeStates) {
                const dist = this.pointToBoundsDistance(point, node.computedBounds);
                minDist = Math.min(minDist, dist);
            }
        }
        return minDist === Infinity ? 0 : minDist;
    }

    /**
     * Calculate distance from point to bounds (edge distance, not center)
     */
    private pointToBoundsDistance(point: Point, bounds: Bounds): number {
        const dx = Math.max(bounds.left - point.x, 0, point.x - bounds.right);
        const dy = Math.max(bounds.top - point.y, 0, point.y - bounds.bottom);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate bounding box of path
     */
    private calculatePathBoundingBox(path: Point[]): Bounds {
        if (path.length === 0) {
            return { left: 0, right: 0, top: 0, bottom: 0, width: 0, height: 0, centerX: 0, centerY: 0 };
        }
        let minX = path[0].x, maxX = path[0].x;
        let minY = path[0].y, maxY = path[0].y;
        for (const point of path) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        const width = maxX - minX;
        const height = maxY - minY;
        return {
            left: minX,
            right: maxX,
            top: minY,
            bottom: maxY,
            width,
            height,
            centerX: minX + width / 2,
            centerY: minY + height / 2
        };
    }

    /**
     * Validate path (check for reasonable length, no self-intersections)
     */
    private validatePath(path: Point[]): boolean {
        if (path.length < 2) return false;
        // Check for reasonable length (not too long)
        const length = this.calculatePathLength(path);
        const directDistance = Math.sqrt(
            (path[path.length - 1].x - path[0].x) ** 2 +
            (path[path.length - 1].y - path[0].y) ** 2
        );
        // Path should not be more than 3x the direct distance
        return length < directDistance * 3;
    }

    /**
     * Calculate minimum clearance from other nodes
     */
    private calculateMinClearance(path: Point[], nodeStates: VisualNodeState[]): number {
        return this.calculateMinDistanceToNodes(path, nodeStates);
    }

    /**
     * Wybierz typ routingu dla połączenia
     * 
     * Zawsze używamy orthogonal routing (jak w NiFi/Conductor) dla:
     * - Spójności wizualnej
     * - Łatwości debugowania
     * - Przewidywalnych ścieżek
     */
    private selectRoutingType(): 'orthogonal' {
        return 'orthogonal';
    }

    /**
     * Assess routing quality
     */
    private assessRoutingQuality(path: Point[], fromNode: VisualNodeState, toNode: VisualNodeState): 'excellent' | 'good' | 'acceptable' | 'poor' {
        if (path.length < 2) return 'poor';

        const pathLength = this.calculatePathLength(path);
        const directDistance = Math.sqrt(
            (toNode.computedBounds.centerX - fromNode.computedBounds.centerX) ** 2 +
            (toNode.computedBounds.centerY - fromNode.computedBounds.centerY) ** 2
        );

        const efficiency = directDistance / pathLength;
        const minClearance = this.calculateMinClearance(path, [fromNode, toNode]);

        if (efficiency > 0.9 && minClearance > 20) return 'excellent';
        if (efficiency > 0.7 && minClearance > 10) return 'good';
        if (efficiency > 0.5 && minClearance > 5) return 'acceptable';
        return 'poor';
    }

    /**
     * Calculate detailed path geometry
     */
    private calculatePathGeometry(
        path: Point[],
        connectionId: string,
        otherConnections: Map<string, VisualConnectionState>
    ): VisualConnectionState['pathGeometry'] {
        if (path.length < 2) {
            return {
                startPoint: path[0] || { x: 0, y: 0 },
                endPoint: path[path.length - 1] || { x: 0, y: 0 },
                turningPoints: [],
                segments: [],
                selfIntersections: [],
                crossesOtherConnections: [],
                reversesDirection: false,
                maxDeviation: 0
            };
        }

        const geometry = SpatialCollisionDetector.analyzePathGeometry(path);
        const selfIntersections = SpatialCollisionDetector.findSelfIntersections(path);

        // Find crossings with other connections
        const otherPaths = Array.from(otherConnections.entries())
            .filter(([id]) => id !== connectionId)
            .map(([id, conn]) => ({
                connectionId: id,
                points: conn.path.points
            }));

        const crossings = SpatialCollisionDetector.findPathCrossings(path, otherPaths);

        return {
            startPoint: path[0],
            endPoint: path[path.length - 1],
            turningPoints: geometry.turningPoints,
            segments: geometry.segments,
            selfIntersections: selfIntersections.map(int => ({
                point: int.point,
                segment1Index: int.segment1Index,
                segment2Index: int.segment2Index
            })),
            crossesOtherConnections: crossings.map(cross => ({
                connectionId: cross.connectionId,
                intersectionPoint: cross.intersectionPoint,
                thisSegmentIndex: cross.thisSegmentIndex,
                otherSegmentIndex: cross.otherSegmentIndex
            })),
            reversesDirection: geometry.reversesDirection,
            maxDeviation: geometry.maxDeviation
        };
    }

    /**
     * Detect path crossings with other connections
     */
    private detectPathCrossings(
        path: Point[],
        allConnections: Map<string, VisualConnectionState>,
        currentConnectionId: string
    ): boolean {
        const otherPaths = Array.from(allConnections.entries())
            .filter(([id]) => id !== currentConnectionId)
            .map(([id, conn]) => ({
                connectionId: id,
                points: conn.path.points
            }));

        const crossings = SpatialCollisionDetector.findPathCrossings(path, otherPaths);
        return crossings.length > 0;
    }

    /**
     * Count turning points in path
     */
    private countTurningPoints(path: Point[]): number {
        if (path.length < 3) return 0;

        let count = 0;
        for (let i = 1; i < path.length - 1; i++) {
            const prev = path[i - 1];
            const curr = path[i];
            const next = path[i + 1];

            const dir1 = { x: curr.x - prev.x, y: curr.y - prev.y };
            const dir2 = { x: next.x - curr.x, y: next.y - curr.y };

            // Check if direction changes (turning point)
            const angle1 = Math.atan2(dir1.y, dir1.x);
            const angle2 = Math.atan2(dir2.y, dir2.x);
            const angleDiff = Math.abs(angle2 - angle1);

            // If angle difference is significant (> 5 degrees), it's a turning point
            if (angleDiff > Math.PI / 36) { // ~5 degrees
                count++;
            }
        }

        return count;
    }

    /**
     * Detect if path passes through any nodes
     */
    private detectPathNodeIntersections(path: Point[], nodeStates: VisualNodeState[]): string[] {
        const intersectedNodes: string[] = [];

        for (const node of nodeStates) {
            // Skip workflow markers
            if (node.nodeId === 'workflow-start' || node.nodeId === 'workflow-end') {
                continue;
            }

            // Check if any segment of path intersects with node bounds
            for (let i = 0; i < path.length - 1; i++) {
                const segmentStart = path[i];
                const segmentEnd = path[i + 1];

                if (this.segmentIntersectsBounds(segmentStart, segmentEnd, node.computedBounds)) {
                    intersectedNodes.push(node.nodeId);
                    break; // Found intersection, no need to check more segments
                }
            }
        }

        return intersectedNodes;
    }

    /**
     * Check if line segment intersects with bounds
     */
    private segmentIntersectsBounds(start: Point, end: Point, bounds: Bounds): boolean {
        // Simple bounding box check first
        const segmentBounds = {
            left: Math.min(start.x, end.x),
            right: Math.max(start.x, end.x),
            top: Math.min(start.y, end.y),
            bottom: Math.max(start.y, end.y)
        };

        if (segmentBounds.right < bounds.left || segmentBounds.left > bounds.right ||
            segmentBounds.bottom < bounds.top || segmentBounds.top > bounds.bottom) {
            return false; // No intersection
        }

        // More precise check: line-rectangle intersection
        // Check if any of the 4 edges of bounds intersect with segment
        const edges = [
            { start: { x: bounds.left, y: bounds.top }, end: { x: bounds.right, y: bounds.top } }, // top
            { start: { x: bounds.right, y: bounds.top }, end: { x: bounds.right, y: bounds.bottom } }, // right
            { start: { x: bounds.right, y: bounds.bottom }, end: { x: bounds.left, y: bounds.bottom } }, // bottom
            { start: { x: bounds.left, y: bounds.bottom }, end: { x: bounds.left, y: bounds.top } } // left
        ];

        for (const edge of edges) {
            if (this.segmentsIntersect(start, end, edge.start, edge.end)) {
                return true;
            }
        }

        // Also check if segment is completely inside bounds
        if (start.x >= bounds.left && start.x <= bounds.right &&
            start.y >= bounds.top && start.y <= bounds.bottom &&
            end.x >= bounds.left && end.x <= bounds.right &&
            end.y >= bounds.top && end.y <= bounds.bottom) {
            return true;
        }

        return false;
    }

    /**
     * Check if two line segments intersect
     */
    private segmentsIntersect(p1: Point, p2: Point, p3: Point, p4: Point): boolean {
        const ccw = (A: Point, B: Point, C: Point): number => {
            return (C.y - A.y) * (B.x - A.x) - (B.y - A.y) * (C.x - A.x);
        };

        const o1 = ccw(p1, p2, p3);
        const o2 = ccw(p1, p2, p4);
        const o3 = ccw(p3, p4, p1);
        const o4 = ccw(p3, p4, p2);

        return ((o1 * o2 < 0) && (o3 * o4 < 0));
    }

    /**
     * Detect label collisions along path
     */
    private detectLabelCollisions(path: Point[], nodeStates: VisualNodeState[]): Array<{ nodeId: string; point: Point }> {
        const collisions: Array<{ nodeId: string; point: Point }> = [];
        const LABEL_HEIGHT = 30; // Approximate label height
        const LABEL_WIDTH = 100; // Approximate label width

        for (const node of nodeStates) {
            // Skip workflow markers
            if (node.nodeId === 'workflow-start' || node.nodeId === 'workflow-end') {
                continue;
            }

            // Label is typically at top of node
            const labelBounds: Bounds = {
                left: node.computedBounds.left,
                right: node.computedBounds.right,
                top: node.computedBounds.top - LABEL_HEIGHT,
                bottom: node.computedBounds.top,
                width: node.computedBounds.width,
                height: LABEL_HEIGHT,
                centerX: node.computedBounds.centerX,
                centerY: node.computedBounds.top - LABEL_HEIGHT / 2
            };

            // Check if path passes through label area
            for (let i = 0; i < path.length - 1; i++) {
                if (this.segmentIntersectsBounds(path[i], path[i + 1], labelBounds)) {
                    collisions.push({
                        nodeId: node.nodeId,
                        point: { x: labelBounds.centerX, y: labelBounds.centerY }
                    });
                    break;
                }
            }
        }

        return collisions;
    }

    /**
     * SEMANTIC MODEL: Automatically select best connection point based on ALL elements and connections in space
     * This is the SINGLE SOURCE OF TRUTH for connection point selection.
     * 
     * Model semantyczny analizuje:
     * - Pozycje wszystkich węzłów w przestrzeni
     * - Wszystkie istniejące połączenia
     * - Kolizje i przecięcia
     * - Optymalną ścieżkę routing
     * 
     * Warstwa wizualna używa tylko danych z modelu - nie decyduje sama o wyborze strony.
     * 
     * CRITICAL: For outputs, prefer opposite side to incoming connections
     */
    private selectBestConnectionPoint(
        fromNode: VisualNodeState,
        toNode: VisualNodeState,
        type: 'input' | 'output',
        usedPointIds: Set<string> = new Set(),
        incomingSide?: Side,  // Side from which connection enters (for output selection)
        allNodeStates?: VisualNodeState[],  // All nodes in space for spatial analysis
        allConnections?: Array<{ from: string; to: string }>,  // All connections for collision detection
        connectionId?: string  // Optional: connection ID for metadata
    ): ConnectionPoint | null {
        // CRITICAL: Get connection points from cache to ensure we update the right objects
        // Cache contains the actual objects that will be used in the model
        const nodeId = type === 'output' ? fromNode.nodeId : toNode.nodeId;
        const cachedPoints = this.connectionPointsCache.get(nodeId);

        // CRITICAL: Always use cached points if available - they are the source of truth
        // Cache contains the actual objects that will be updated by updateConnectionPointSemanticSelection
        let availablePoints: ConnectionPoint[];
        if (cachedPoints && cachedPoints.length > 0) {
            availablePoints = cachedPoints.filter(cp => cp.type === type);
        } else {
            // Fallback to node's points only if cache is empty
            // This should rarely happen, but if it does, we need to ensure cache is populated
            const nodePoints = type === 'output' ? fromNode.connectionPoints.outputs : toNode.connectionPoints.inputs;
            availablePoints = nodePoints;

            // Log warning if cache is empty
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.warn(`[VisualSemanticModelManager] ⚠️ Cache empty for node ${nodeId}, using node's connection points. This may cause semanticSelection metadata to be lost.`);
            }
        }

        if (availablePoints.length === 0) {
            return null;
        }

        // CRITICAL: Only consider CENTER points for connections (strzałki mogą prowadzić tylko do środkowych punktów)
        // Filter to only center points first
        const centerPoints = availablePoints.filter(p => p.id.includes('-center'));

        if (centerPoints.length === 0) {
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.warn(`[VisualSemanticModelManager] ⚠️ No center points available for ${type} on node ${nodeId}`);
            }
            return null;
        }

        // Filter out already used center points
        const unusedPoints = centerPoints.filter(p => !usedPointIds.has(p.id));

        // If all center points are used, fall back to all center points (reuse is better than no connection)
        const candidatePoints = unusedPoints.length > 0 ? unusedPoints : centerPoints;

        // If only one point available, use it
        if (candidatePoints.length === 1) {
            const selectedPoint = candidatePoints[0];
            // CRITICAL: Verify point is from cache
            if (!selectedPoint) {
                const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
                if (shouldLog) {
                    console.error(`[VisualSemanticModelManager] ❌ selectBestConnectionPoint: selectedPoint is null for ${type} on node ${nodeId}`);
                }
                return null;
            }

            // CRITICAL: Ensure selectedPoint is from cache, not a copy
            let pointToUpdate = selectedPoint;
            if (cachedPoints && cachedPoints.length > 0) {
                const pointInCache = cachedPoints.find(cp => cp.id === selectedPoint.id);
                if (pointInCache && pointInCache !== selectedPoint) {
                    // Use point from cache to ensure we update the right object
                    pointToUpdate = pointInCache;
                }
            }

            // Update semantic selection metadata
            this.updateConnectionPointSemanticSelection(
                pointToUpdate,
                fromNode.nodeId,
                toNode.nodeId,
                type,
                'single-available',
                allNodeStates,
                allConnections,
                connectionId
            );

            // CRITICAL: Verify metadata was set
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog && (!pointToUpdate.semanticSelection || !pointToUpdate.semanticSelection.isSelected)) {
                console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Failed to set semanticSelection for point ${pointToUpdate.id} after updateConnectionPointSemanticSelection`);
            }

            // CRITICAL: Return point from cache if available, otherwise return selectedPoint
            return pointToUpdate;
        }

        // SEMANTIC MODEL: Analyze all spatial context to make decision
        // This is the SINGLE SOURCE OF TRUTH for connection point selection
        const spatialAnalysis = this.analyzeSpatialContextForConnectionPoint(
            fromNode,
            toNode,
            type,
            candidatePoints,
            allNodeStates || [],
            allConnections || [],
            incomingSide
        );

        // Use spatial analysis result as primary decision
        let preferredSide: Side = spatialAnalysis.preferredSide;

        // Calculate relative positions (for fallback logic)
        const fromCenter = {
            x: fromNode.computedBounds.centerX,
            y: fromNode.computedBounds.centerY
        };
        const toCenter = {
            x: toNode.computedBounds.centerX,
            y: toNode.computedBounds.centerY
        };

        const dx = toCenter.x - fromCenter.x;
        const dy = toCenter.y - fromCenter.y;

        // If spatial analysis didn't find a clear winner, use fallback logic
        // Determine preferred side based on target node position
        // CRITICAL: For outputs, determine side based on where the NEXT plugin (target) is located
        if (spatialAnalysis.reason === 'fallback') {
            // Fallback to simple position-based logic
            if (type === 'output') {
                // For outputs: determine side based on target node position relative to source node
                // This ensures the line exits from the side closest to the target
                const absDx = Math.abs(dx);
                const absDy = Math.abs(dy);

                // Determine primary direction to target
                if (absDx > absDy) {
                    // Target is more horizontal - prefer left or right
                    preferredSide = dx > 0 ? 'right' : 'left';
                } else {
                    // Target is more vertical - prefer top or bottom
                    preferredSide = dy > 0 ? 'bottom' : 'top';
                }

                // CRITICAL: If there's an incoming connection, prefer opposite side to incoming
                // This ensures: if connection enters from left, it exits from right (or vice versa)
                if (incomingSide) {
                    const oppositeSides: Record<Side, Side> = {
                        'left': 'right',
                        'right': 'left',
                        'top': 'bottom',
                        'bottom': 'top'
                    };
                    const oppositeSide = oppositeSides[incomingSide];

                    // If opposite side aligns with target direction, use it
                    // Otherwise, prefer opposite side if available, but fall back to target direction
                    if (oppositeSide === preferredSide) {
                        // Perfect: opposite side matches target direction
                        preferredSide = oppositeSide;
                    } else {
                        // Check if opposite side is available
                        const oppositePoint = candidatePoints.find(p => p.side === oppositeSide && !usedPointIds.has(p.id));
                        if (oppositePoint) {
                            // Use opposite side (preferred for clean routing: in from one side, out from opposite)
                            preferredSide = oppositeSide;
                        }
                        // Otherwise keep preferredSide based on target position
                    }

                    console.log(`[VisualSemanticModelManager] Output: incoming=${incomingSide}, target=${dx > 0 ? 'right' : 'left'}/${dy > 0 ? 'bottom' : 'top'}, chosen=${preferredSide}`);
                } else {
                    console.log(`[VisualSemanticModelManager] Output: no incoming, target=${dx > 0 ? 'right' : 'left'}/${dy > 0 ? 'bottom' : 'top'}, chosen=${preferredSide}`);
                }
            } else {
                // For inputs: use relative position to determine which side to receive connection
                // Input should be on the side opposite to where the connection comes from
                if (Math.abs(dx) > Math.abs(dy)) {
                    // Horizontal alignment - connection comes from left or right
                    preferredSide = dx > 0 ? 'left' : 'right'; // Receive from opposite side
                } else {
                    // Vertical alignment - connection comes from top or bottom
                    preferredSide = dy > 0 ? 'top' : 'bottom'; // Receive from opposite side
                }
            }
        }

        // Use spatial analysis to find best point
        // Sort candidate points by spatial score (if available) or by preferred side
        const sortedPoints = candidatePoints
            .map(p => ({
                point: p,
                score: spatialAnalysis.spatialScore.get(p.id) || 0,
                isPreferredSide: p.side === preferredSide,
                isUnused: !usedPointIds.has(p.id)
            }))
            .sort((a, b) => {
                // Priority: unused > used, preferred side > other, higher score > lower
                if (a.isUnused !== b.isUnused) return a.isUnused ? -1 : 1;
                if (a.isPreferredSide !== b.isPreferredSide) return a.isPreferredSide ? -1 : 1;
                return b.score - a.score;
            });

        // Find unused point on preferred side
        const preferredPoint = sortedPoints.find(p => p.isPreferredSide && p.isUnused)?.point ||
            sortedPoints.find(p => p.isPreferredSide)?.point;
        if (preferredPoint) {
            // CRITICAL: Ensure preferredPoint is from cache, not a copy
            let pointToUpdate = preferredPoint;
            if (cachedPoints && cachedPoints.length > 0) {
                const pointInCache = cachedPoints.find(cp => cp.id === preferredPoint.id);
                if (pointInCache && pointInCache !== preferredPoint) {
                    // Use point from cache to ensure we update the right object
                    pointToUpdate = pointInCache;
                }
            }

            // Update semantic selection metadata
            this.updateConnectionPointSemanticSelection(
                pointToUpdate,
                fromNode.nodeId,
                toNode.nodeId,
                type,
                spatialAnalysis.reason,
                allNodeStates,
                allConnections,
                connectionId || undefined
            );

            // CRITICAL: Verify metadata was set
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog && (!pointToUpdate.semanticSelection || !pointToUpdate.semanticSelection.isSelected)) {
                console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Failed to set semanticSelection for point ${pointToUpdate.id} after updateConnectionPointSemanticSelection`);
            }

            // CRITICAL: Return point from cache if available, otherwise return preferredPoint
            return pointToUpdate;
        }

        // If preferred side not available, try opposite or adjacent sides
        // Priority: opposite > adjacent > any unused
        const oppositeSides: Record<Side, Side[]> = {
            'left': ['right'],
            'right': ['left'],
            'top': ['bottom'],
            'bottom': ['top']
        };

        const adjacentSides: Record<Side, Side[]> = {
            'left': ['top', 'bottom'],
            'right': ['top', 'bottom'],
            'top': ['left', 'right'],
            'bottom': ['left', 'right']
        };

        // Try opposite side first
        const oppositeSidePoints = candidatePoints.filter(p =>
            oppositeSides[preferredSide].includes(p.side) && !usedPointIds.has(p.id)
        );
        if (oppositeSidePoints.length > 0) {
            // Choose closest to target
            let bestPoint: ConnectionPoint | null = null;
            let minDistance = Infinity;
            for (const point of oppositeSidePoints) {
                const absolutePoint = this.toAbsolutePosition(point.position, fromNode.position);
                const distance = Math.sqrt(
                    Math.pow(toCenter.x - absolutePoint.x, 2) +
                    Math.pow(toCenter.y - absolutePoint.y, 2)
                );
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPoint = point;
                }
            }
            if (bestPoint) {
                // CRITICAL: Ensure bestPoint is from cache, not a copy
                let pointToUpdate = bestPoint;
                if (cachedPoints && cachedPoints.length > 0) {
                    const pointInCache = cachedPoints.find(cp => cp.id === bestPoint.id);
                    if (pointInCache && pointInCache !== bestPoint) {
                        // Use point from cache to ensure we update the right object
                        pointToUpdate = pointInCache;
                    }
                }

                // CRITICAL: Update semantic selection metadata
                this.updateConnectionPointSemanticSelection(
                    pointToUpdate,
                    fromNode.nodeId,
                    toNode.nodeId,
                    type,
                    'opposite-side',
                    allNodeStates,
                    allConnections,
                    connectionId || undefined
                );

                // CRITICAL: Return point from cache if available, otherwise return bestPoint
                return pointToUpdate;
            }
        }

        // Try adjacent sides
        const adjacentSidePoints = candidatePoints.filter(p =>
            adjacentSides[preferredSide].includes(p.side) && !usedPointIds.has(p.id)
        );
        if (adjacentSidePoints.length > 0) {
            // Choose closest to target
            let bestPoint: ConnectionPoint | null = null;
            let minDistance = Infinity;
            for (const point of adjacentSidePoints) {
                const absolutePoint = this.toAbsolutePosition(point.position, fromNode.position);
                const distance = Math.sqrt(
                    Math.pow(toCenter.x - absolutePoint.x, 2) +
                    Math.pow(toCenter.y - absolutePoint.y, 2)
                );
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPoint = point;
                }
            }
            if (bestPoint) {
                // CRITICAL: Ensure bestPoint is from cache, not a copy
                let pointToUpdate = bestPoint;
                if (cachedPoints && cachedPoints.length > 0) {
                    const pointInCache = cachedPoints.find(cp => cp.id === bestPoint.id);
                    if (pointInCache && pointInCache !== bestPoint) {
                        // Use point from cache to ensure we update the right object
                        pointToUpdate = pointInCache;
                    }
                }

                // CRITICAL: Update semantic selection metadata
                this.updateConnectionPointSemanticSelection(
                    pointToUpdate,
                    fromNode.nodeId,
                    toNode.nodeId,
                    type,
                    'adjacent-side',
                    allNodeStates,
                    allConnections,
                    connectionId || undefined
                );

                // CRITICAL: Return point from cache if available, otherwise return bestPoint
                return pointToUpdate;
            }
        }

        // If no unused point on preferred/opposite/adjacent sides, choose any unused point based on distance
        let bestPoint: ConnectionPoint | null = null;
        let minDistance = Infinity;

        for (const point of candidatePoints) {
            if (usedPointIds.has(point.id) && unusedPoints.length > 0) {
                continue; // Skip used points if we have unused ones
            }

            const absolutePoint = this.toAbsolutePosition(point.position, fromNode.position);
            const distance = Math.sqrt(
                Math.pow(toCenter.x - absolutePoint.x, 2) +
                Math.pow(toCenter.y - absolutePoint.y, 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                bestPoint = point;
            }
        }

        const finalPoint = bestPoint || candidatePoints[0];

        // CRITICAL: Verify we have a point to return
        if (!finalPoint) {
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.error(`[VisualSemanticModelManager] ❌ CRITICAL: No point selected for ${type} on node ${nodeId}. candidatePoints.length=${candidatePoints.length}, bestPoint=${bestPoint ? bestPoint.id : 'null'}`);
            }
            return null;
        }

        // Update semantic selection metadata for the selected point
        // CRITICAL: Always update metadata
        // CRITICAL: Ensure finalPoint is from cache, not a copy
        // candidatePoints should already be from cache (via availablePoints), but double-check
        let pointToUpdate = finalPoint;
        if (cachedPoints && cachedPoints.length > 0) {
            const pointInCache = cachedPoints.find(cp => cp.id === finalPoint.id);
            if (pointInCache && pointInCache !== finalPoint) {
                // Use point from cache to ensure we update the right object
                pointToUpdate = pointInCache;
            } else if (!pointInCache) {
                // CRITICAL: finalPoint is not in cache - this should not happen
                const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
                if (shouldLog) {
                    console.error(`[VisualSemanticModelManager] ❌ CRITICAL: finalPoint ${finalPoint.id} (${type}) not found in cache for node ${nodeId}. This will cause metadata to be lost.`);
                }
            }
        } else {
            // CRITICAL: No cache available - this should not happen
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.error(`[VisualSemanticModelManager] ❌ CRITICAL: No cache available for node ${nodeId} (${type}). This will cause metadata to be lost.`);
            }
        }

        const reason = preferredPoint ? 'preferred-side' :
            (oppositeSidePoints.length > 0 ? 'opposite-side' :
                (adjacentSidePoints.length > 0 ? 'adjacent-side' : 'distance-based'));

        // CRITICAL: Ensure point has semanticSelection initialized before updating
        if (!pointToUpdate.semanticSelection) {
            pointToUpdate.semanticSelection = {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            };
        }

        this.updateConnectionPointSemanticSelection(
            pointToUpdate,
            fromNode.nodeId,
            toNode.nodeId,
            type,
            reason,
            allNodeStates,
            allConnections,
            connectionId || undefined
        );

        // CRITICAL: Verify metadata was set
        const shouldLogVerify = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogVerify && (!pointToUpdate.semanticSelection || !pointToUpdate.semanticSelection.isSelected)) {
            console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Failed to set semanticSelection for point ${pointToUpdate.id} (${type}) after updateConnectionPointSemanticSelection. nodeId=${nodeId}, connectionId=${connectionId}`);
        }

        // CRITICAL: Verify metadata was set before returning
        const shouldLogReturn = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogReturn && (!pointToUpdate.semanticSelection || !pointToUpdate.semanticSelection.isSelected)) {
            console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Returning point ${pointToUpdate.id} (${type}) without isSelected=true. nodeId=${nodeId}, connectionId=${connectionId}`);
        }

        // CRITICAL: Return point from cache if available, otherwise return finalPoint
        return pointToUpdate;
    }

    /**
     * SEMANTIC MODEL: Analyze spatial context for connection point selection
     * This method analyzes ALL elements and connections in space to make the best decision
     */
    private analyzeSpatialContextForConnectionPoint(
        fromNode: VisualNodeState,
        toNode: VisualNodeState,
        type: 'input' | 'output',
        candidatePoints: ConnectionPoint[],
        allNodeStates: VisualNodeState[],
        allConnections: Array<{ from: string; to: string }>,
        incomingSide?: Side
    ): {
        preferredSide: Side;
        reason: string;
        spatialScore: Map<string, number>; // pointId -> score
    } {
        const spatialScore = new Map<string, number>();

        // Calculate scores for each candidate point based on:
        // 1. Path length to target
        // 2. Clearance from other nodes
        // 3. Avoidance of existing connections
        // 4. Alignment with incoming connections (for outputs)

        for (const point of candidatePoints) {
            let score = 0;
            const absolutePoint = this.toAbsolutePosition(point.position, fromNode.position);
            const toCenter = {
                x: toNode.computedBounds.centerX,
                y: toNode.computedBounds.centerY
            };

            // 1. Path length score (shorter is better)
            const pathLength = Math.sqrt(
                Math.pow(toCenter.x - absolutePoint.x, 2) +
                Math.pow(toCenter.y - absolutePoint.y, 2)
            );
            score += 1000 / (1 + pathLength / 100); // Normalize to reasonable range

            // 2. Clearance from other nodes (more clearance is better)
            let minClearance = Infinity;
            for (const otherNode of allNodeStates) {
                if (otherNode.nodeId === fromNode.nodeId || otherNode.nodeId === toNode.nodeId) {
                    continue;
                }
                const clearance = this.pointToBoundsDistance(absolutePoint, otherNode.computedBounds);
                minClearance = Math.min(minClearance, clearance);
            }
            if (minClearance !== Infinity) {
                score += Math.min(minClearance / 10, 100); // Cap at 100 points
            }

            // 3. Alignment with incoming connections (for outputs)
            if (type === 'output' && incomingSide) {
                const oppositeSides: Record<Side, Side> = {
                    'left': 'right',
                    'right': 'left',
                    'top': 'bottom',
                    'bottom': 'top'
                };
                if (point.side === oppositeSides[incomingSide]) {
                    score += 200; // Bonus for opposite side
                }
            }

            // 4. Avoid collisions with existing connections
            // (This is a simplified check - full collision detection would be more complex)
            let collisionPenalty = 0;
            for (const conn of allConnections) {
                // Check if this point would create a crossing
                // Simplified: check if point is too close to existing connection paths
                // Full implementation would check actual path intersections
            }
            score -= collisionPenalty;

            spatialScore.set(point.id, score);
        }

        // Find best point
        let bestPoint: ConnectionPoint | null = null;
        let bestScore = -Infinity;
        for (const point of candidatePoints) {
            const score = spatialScore.get(point.id) || 0;
            if (score > bestScore) {
                bestScore = score;
                bestPoint = point;
            }
        }

        const preferredSide = bestPoint?.side || 'right';
        const reason = bestPoint ? 'spatial-analysis' : 'fallback';

        return {
            preferredSide,
            reason,
            spatialScore
        };
    }

    /**
     * SEMANTIC MODEL: Update connection point semantic selection metadata
     * This is the SINGLE SOURCE OF TRUTH for which side is selected
     * 
     * CRITICAL: Updates both cache and model node states to ensure consistency
     */
    private updateConnectionPointSemanticSelection(
        point: ConnectionPoint,
        fromNodeId: string,
        toNodeId: string,
        type: 'input' | 'output',
        reason: string,
        allNodeStates?: VisualNodeState[],
        allConnections?: Array<{ from: string; to: string }>,
        connectionId?: string  // Optional: use provided connectionId if available
    ): void {
        // CRITICAL: Always log when this function is called (for debugging runtime tests)
        const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLog) {
            console.log(`[VisualSemanticModelManager] 🔵 updateConnectionPointSemanticSelection CALLED: point=${point?.id}, type=${type}, nodeId=${type === 'output' ? fromNodeId : toNodeId}, connectionId=${connectionId || 'none'}, reason=${reason}`);
        }

        // CRITICAL: Verify point is not null/undefined
        if (!point) {
            if (shouldLog) {
                console.error(`[VisualSemanticModelManager] ❌ updateConnectionPointSemanticSelection: point is null/undefined for ${type} on node ${type === 'output' ? fromNodeId : toNodeId}`);
            }
            return;
        }

        // CRITICAL: Verify point is from cache (check if it's in cache)
        const nodeId = type === 'output' ? fromNodeId : toNodeId;
        const cachedPoints = this.connectionPointsCache.get(nodeId);
        const pointInCache = cachedPoints?.find(cp => cp.id === point.id);

        // CRITICAL: Use point from cache if available, otherwise use provided point
        // This ensures we update the actual object that will be used in the model
        const pointToUpdate = pointInCache || point;

        if (!pointInCache) {
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.warn(`[VisualSemanticModelManager] ⚠️ updateConnectionPointSemanticSelection: point ${point.id} not found in cache for node ${nodeId}. Using provided point, but metadata may be lost.`);
            }
        }

        // Initialize semanticSelection if not exists
        if (!pointToUpdate.semanticSelection) {
            pointToUpdate.semanticSelection = {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            };
        }

        // Update metadata
        // CRITICAL: Set isSelected to true BEFORE checking - this ensures the assignment happens
        const wasSelectedBefore = pointToUpdate.semanticSelection.isSelected;
        pointToUpdate.semanticSelection.isSelected = true;

        // CRITICAL: Verify assignment worked immediately
        if (!pointToUpdate.semanticSelection.isSelected) {
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Assignment failed! pointToUpdate.semanticSelection.isSelected is still false after assignment. pointToUpdate=${pointToUpdate.id}, type=${type}, nodeId=${nodeId}`);
            }
        }

        // Use provided connectionId if available, otherwise generate one
        const finalConnectionId = connectionId || `conn-${fromNodeId}-${toNodeId}`;
        if (!pointToUpdate.semanticSelection.selectedForConnections.includes(finalConnectionId)) {
            pointToUpdate.semanticSelection.selectedForConnections.push(finalConnectionId);
        }
        pointToUpdate.semanticSelection.selectionReason = reason;

        // CRITICAL: Verify metadata was set immediately after update
        const shouldLogVerify = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogVerify) {
            if (!pointToUpdate.semanticSelection.isSelected) {
                console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Failed to set isSelected=true for pointToUpdate ${pointToUpdate.id} (${type}) on node ${nodeId}. pointToUpdate === pointInCache: ${pointToUpdate === pointInCache}, pointInCache exists: ${!!pointInCache}, wasSelectedBefore: ${wasSelectedBefore}`);
            } else {
                console.log(`[VisualSemanticModelManager] ✅ Successfully set isSelected=true for pointToUpdate ${pointToUpdate.id} (${type}) on node ${nodeId}. connectionId=${finalConnectionId}`);
            }
        }

        // CRITICAL: Also update provided point if it's different from cache point
        // This ensures that if point is not from cache, we still update it
        if (pointToUpdate !== point) {
            if (!point.semanticSelection) {
                point.semanticSelection = {
                    isSelected: false,
                    selectedForConnections: [],
                    selectionReason: '',
                    priority: 0,
                    spatialContext: {
                        pathLength: 0,
                        clearance: 0,
                        avoidsCollisions: false
                    }
                };
            }
            point.semanticSelection.isSelected = true;
            if (!point.semanticSelection.selectedForConnections.includes(finalConnectionId)) {
                point.semanticSelection.selectedForConnections.push(finalConnectionId);
            }
            point.semanticSelection.selectionReason = reason;
        }

        // Always log in development, but also log in production for debugging runtime tests
        const shouldLogUpdate = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLogUpdate) {
            console.log(`[VisualSemanticModelManager] ✅ Updated semanticSelection for point ${pointToUpdate.id} (${pointToUpdate.side}, ${type}): reason=${reason}, connectionId=${finalConnectionId}, isSelected=${pointToUpdate.semanticSelection.isSelected}, nodeId=${nodeId}, fromCache=${!!pointInCache}`);
        }

        // Calculate spatial context
        if (allNodeStates && allNodeStates.length > 0) {
            const fromNode = allNodeStates.find(n => n.nodeId === fromNodeId);
            const toNode = allNodeStates.find(n => n.nodeId === toNodeId);

            if (fromNode && toNode) {
                const absolutePoint = this.toAbsolutePosition(point.position, fromNode.position);
                const toCenter = {
                    x: toNode.computedBounds.centerX,
                    y: toNode.computedBounds.centerY
                };

                point.semanticSelection.spatialContext.pathLength = Math.sqrt(
                    Math.pow(toCenter.x - absolutePoint.x, 2) +
                    Math.pow(toCenter.y - absolutePoint.y, 2)
                );
                point.semanticSelection.spatialContext.targetNodeId = toNodeId;

                // Calculate clearance
                let minClearance = Infinity;
                for (const otherNode of allNodeStates) {
                    if (otherNode.nodeId === fromNodeId || otherNode.nodeId === toNodeId) {
                        continue;
                    }
                    const clearance = this.pointToBoundsDistance(absolutePoint, otherNode.computedBounds);
                    minClearance = Math.min(minClearance, clearance);
                }
                point.semanticSelection.spatialContext.clearance = minClearance !== Infinity ? minClearance : 0;
            }
        }

        // Set priority based on reason
        const priorityMap: Record<string, number> = {
            'preferred-side': 100,
            'opposite-side': 90,
            'adjacent-side': 70,
            'distance-based': 50,
            'spatial-analysis': 95,
            'single-available': 30,
            'fallback': 10
        };
        point.semanticSelection.priority = priorityMap[reason] || 50;

        // CRITICAL: Also update the point in model node states to ensure consistency
        // Find the node state and update the corresponding connection point
        // This ensures that when model is queried, it has the latest semanticSelection metadata
        if (allNodeStates) {
            const nodeId = type === 'output' ? fromNodeId : toNodeId;
            const nodeState = allNodeStates.find(n => n.nodeId === nodeId);

            if (nodeState) {
                const pointsArray = type === 'output'
                    ? nodeState.connectionPoints.outputs
                    : nodeState.connectionPoints.inputs;

                // Find the same point in node state (by ID) and update it
                const modelPoint = pointsArray.find(p => p.id === point.id);
                if (modelPoint) {
                    // If it's the same reference, no need to update (already done above)
                    if (modelPoint !== point) {
                        // Update the model point with the same metadata
                        if (!modelPoint.semanticSelection) {
                            modelPoint.semanticSelection = {
                                isSelected: false,
                                selectedForConnections: [],
                                selectionReason: '',
                                priority: 0,
                                spatialContext: {
                                    pathLength: 0,
                                    clearance: 0,
                                    avoidsCollisions: false
                                }
                            };
                        }

                        // Copy all metadata to model point
                        modelPoint.semanticSelection.isSelected = point.semanticSelection.isSelected;
                        modelPoint.semanticSelection.selectedForConnections = [...point.semanticSelection.selectedForConnections];
                        modelPoint.semanticSelection.selectionReason = point.semanticSelection.selectionReason;
                        modelPoint.semanticSelection.priority = point.semanticSelection.priority;
                        modelPoint.semanticSelection.spatialContext = { ...point.semanticSelection.spatialContext };
                    }
                }
            }
        }

        // CRITICAL: Also update in nodeStatesMap if available (for direct access during buildConnectionStates)
        // This ensures that connection points in the model are updated immediately
        // We need to pass nodeStatesMap as a parameter or access it from closure
        // For now, we rely on allNodeStates parameter which should contain the same references
    }

    /**
     * Get all turning points in path
     */
    private getTurningPoints(path: Point[]): Point[] {
        if (path.length < 3) return [];

        const turningPoints: Point[] = [];
        for (let i = 1; i < path.length - 1; i++) {
            const prev = path[i - 1];
            const curr = path[i];
            const next = path[i + 1];

            const dir1 = { x: curr.x - prev.x, y: curr.y - prev.y };
            const dir2 = { x: next.x - curr.x, y: next.y - curr.y };

            // Check if direction changes (turning point)
            const angle1 = Math.atan2(dir1.y, dir1.x);
            const angle2 = Math.atan2(dir2.y, dir2.x);
            const angleDiff = Math.abs(angle2 - angle1);

            // If angle difference is significant (> 5 degrees), it's a turning point
            if (angleDiff > Math.PI / 36) { // ~5 degrees
                turningPoints.push(curr);
            }
        }

        return turningPoints;
    }

    /**
     * Analyze path segments with detailed information
     */
    private analyzePathSegments(path: Point[]): Array<{
        from: Point;
        to: Point;
        length: number;
        direction: Point;
        angle: number;
        isHorizontal: boolean;
        isVertical: boolean;
    }> {
        const segments: Array<{
            from: Point;
            to: Point;
            length: number;
            direction: Point;
            angle: number;
            isHorizontal: boolean;
            isVertical: boolean;
        }> = [];

        for (let i = 0; i < path.length - 1; i++) {
            const from = path[i];
            const to = path[i + 1];

            const dx = to.x - from.x;
            const dy = to.y - from.y;
            const length = Math.sqrt(dx * dx + dy * dy);

            // Normalized direction vector
            const direction = length > 0
                ? { x: dx / length, y: dy / length }
                : { x: 0, y: 0 };

            // Angle in radians
            const angle = Math.atan2(dy, dx);

            // Check if horizontal or vertical (with tolerance)
            const isHorizontal = Math.abs(dy) < 0.1;
            const isVertical = Math.abs(dx) < 0.1;

            segments.push({
                from,
                to,
                length,
                direction,
                angle,
                isHorizontal,
                isVertical
            });
        }

        return segments;
    }

    /**
     * Reroute path around nodes that it passes through
     */
    private rerouteAroundNodes(
        originalPath: Point[],
        start: Point,
        end: Point,
        blockingNodes: string[],
        allNodeStates: VisualNodeState[],
        startSide: Side,
        endSide: Side
    ): Point[] {
        // Get bounds of blocking nodes
        const blockingBounds: Bounds[] = [];
        for (const nodeId of blockingNodes) {
            const node = allNodeStates.find(n => n.nodeId === nodeId);
            if (node) {
                // Add padding around node to avoid passing too close
                const padding = 20;
                blockingBounds.push({
                    left: node.computedBounds.left - padding,
                    right: node.computedBounds.right + padding,
                    top: node.computedBounds.top - padding,
                    bottom: node.computedBounds.bottom + padding,
                    width: node.computedBounds.width + padding * 2,
                    height: node.computedBounds.height + padding * 2,
                    centerX: node.computedBounds.centerX,
                    centerY: node.computedBounds.centerY
                });
            }
        }

        // Simple rerouting: go around the first blocking node
        if (blockingBounds.length > 0) {
            const firstBlock = blockingBounds[0];
            const newPath: Point[] = [start];

            // Determine best way to go around
            const startToBlock = {
                x: firstBlock.centerX - start.x,
                y: firstBlock.centerY - start.y
            };
            const blockToEnd = {
                x: end.x - firstBlock.centerX,
                y: end.y - firstBlock.centerY
            };

            // Go around top or bottom based on which is shorter
            const goTop = Math.abs(start.y - firstBlock.top) + Math.abs(firstBlock.top - end.y) <
                Math.abs(start.y - firstBlock.bottom) + Math.abs(firstBlock.bottom - end.y);

            if (goTop) {
                // Go over the node
                newPath.push({ x: start.x, y: firstBlock.top - 20 });
                newPath.push({ x: end.x, y: firstBlock.top - 20 });
            } else {
                // Go under the node
                newPath.push({ x: start.x, y: firstBlock.bottom + 20 });
                newPath.push({ x: end.x, y: firstBlock.bottom + 20 });
            }

            newPath.push(end);
            return newPath;
        }

        // Fallback: return original path
        return originalPath;
    }

    /**
     * Powiadom wszystkich listenerów o zmianie
     */
    notifyUpdate(): void {
        const buildStartTime = performance.now();
        console.log('[VisualSemanticModelManager] 🔄 Building model and notifying listeners...');

        // Rebuild model
        this.model = this.buildModel();
        // CRITICAL: Use getModel() to ensure connection points are synced with cache
        // This ensures semanticSelection metadata is present in the model
        this.model = this.getModel();
        const buildTime = performance.now() - buildStartTime;
        console.log(`[VisualSemanticModelManager] ✅ Model built and synced in ${buildTime.toFixed(2)}ms: ${this.model.workflow.rootNodes.length} root nodes, ${this.model.workflow.allNodes.size} total nodes, ${this.model.workflow.connections.size} connections`);

        // Notify listeners
        this.updateListeners.forEach(listener => {
            try {
                listener(this.model!);
            } catch (error) {
                console.error('[VisualSemanticModelManager] ❌ Error in update listener:', error);
            }
        });
        console.log(`[VisualSemanticModelManager] ✅ Notified ${this.updateListeners.size} listeners`);

        // Send model update via WebSocket if connected
        if (this.wsManager && this.wsManager.isConnected() && this.model) {
            try {
                this.wsManager.sendModelUpdate(this.model);
                console.log('[VisualSemanticModelManager] ✅ Model update sent via WebSocket');
            } catch (error) {
                console.error('[VisualSemanticModelManager] ❌ Error sending model update via WebSocket:', error);
            }
        } else {
            console.debug('[VisualSemanticModelManager] WebSocket not connected, skipping model update');
        }
    }

    /**
     * DEPRECATED: Use notifyUpdate() instead
     * @deprecated
     */
    private oldNotifyUpdate(): void {
        const buildStartTime = performance.now();
        console.log('[VisualSemanticModelManager] 🔄 Building model and notifying listeners...');
        const model = this.buildModel();
        this.model = model; // Cache the model
        const buildTime = performance.now() - buildStartTime;
        console.log(`[VisualSemanticModelManager] ✅ Model built in ${buildTime.toFixed(2)}ms: ${model.workflow.rootNodes.length} root nodes, ${model.workflow.allNodes.size} total nodes`);
        this.updateListeners.forEach(listener => {
            try {
                listener(model);
            } catch (error) {
                console.error('[VisualSemanticModelManager] ❌ Error in update listener:', error);
            }
        });
        console.log(`[VisualSemanticModelManager] ✅ Notified ${this.updateListeners.size} listeners`);

        // Send model update via WebSocket if connected
        if (this.wsManager && this.wsManager.isConnected()) {
            try {
                this.wsManager.sendModelUpdate(model);
                console.log('[VisualSemanticModelManager] ✅ Model update sent via WebSocket');
            } catch (error) {
                console.error('[VisualSemanticModelManager] ❌ Error sending model update via WebSocket:', error);
            }
        } else {
            console.debug('[VisualSemanticModelManager] WebSocket not connected, skipping model update');
        }
    }

    /**
     * Pobierz aktualny model (lub zbuduj jeśli nie istnieje)
     */
    getModel(): VisualSemanticModel {
        if (!this.model) {
            return this.buildModel();
        }

        // CRITICAL: Ensure connection points in model have latest semanticSelection metadata from cache
        // This is needed because semanticSelection is updated in cache during buildConnectionStates,
        // but model might have been built before or connection points might not be references
        let syncedCount = 0;
        let selectedPointsCount = 0;
        let totalPointsCount = 0;
        let selectedInCacheCount = 0;
        this.model.workflow.allNodes.forEach((nodeState, nodeId) => {
            const cachedPoints = this.connectionPointsCache.get(nodeId);
            if (cachedPoints && cachedPoints.length > 0) {
                // Count selected points in cache BEFORE sync
                const selectedInCache = cachedPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
                selectedInCacheCount += selectedInCache.length;

                // Replace connection points with cached references to ensure latest metadata
                const inputPoints = cachedPoints.filter(cp => cp.type === 'input');
                const outputPoints = cachedPoints.filter(cp => cp.type === 'output');
                nodeState.connectionPoints.inputs = inputPoints;
                nodeState.connectionPoints.outputs = outputPoints;
                syncedCount++;

                // Count selected points
                const allPoints = [...inputPoints, ...outputPoints];
                totalPointsCount += allPoints.length;
                const selected = allPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
                selectedPointsCount += selected.length;

                if (selected.length > 0 && process.env.NODE_ENV === 'development') {
                    console.debug(`[VisualSemanticModelManager] Node ${nodeId}: ${selected.length}/${allPoints.length} points selected`);
                }
            }
        });

        // Always log in development, but also log in production for debugging runtime tests
        const isDevelopment = process.env.NODE_ENV === 'development';
        const shouldLog = isDevelopment || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (syncedCount > 0 && shouldLog) {
            console.log(`[VisualSemanticModelManager] ✅ Synced ${syncedCount} nodes' connection points from cache in getModel(), found ${selectedPointsCount}/${totalPointsCount} selected points in model (${selectedInCacheCount} in cache)`);
            if (selectedInCacheCount > 0 && selectedPointsCount === 0) {
                console.error(`[VisualSemanticModelManager] ❌ CRITICAL: Lost ${selectedInCacheCount} selected points during sync in getModel()!`);
            }
        } else if (syncedCount === 0 && shouldLog) {
            console.warn(`[VisualSemanticModelManager] ⚠️ No nodes synced from cache in getModel()`);
        }

        return this.model;
    }

    /**
     * Convert model to LLM-friendly format (JSON serializable)
     */
    toLLMFormat(): VisualSemanticModelForLLM {
        const model = this.getModel();

        // Convert Map to Object for availablePlugins
        const availablePluginsObj: { [pluginId: string]: PluginSemanticInfo } = {};
        model.availablePlugins.forEach((info, id) => {
            availablePluginsObj[id] = info;
        });

        // Convert Map to Object for allNodes
        const allNodesObj: { [nodeId: string]: VisualNodeState } = {};
        model.workflow.allNodes.forEach((node, id) => {
            allNodesObj[id] = node;
        });

        // Convert Map to Object for connections
        const connectionsObj: { [connectionId: string]: VisualConnectionState } = {};
        model.workflow.connections.forEach((conn, id) => {
            connectionsObj[id] = conn;
        });

        // Convert Map to Object for variableDependencies
        const variableDependenciesObj: { [variableName: string]: VariableDependency } = {};
        model.workflow.variableDependencies.forEach((dep, name) => {
            variableDependenciesObj[name] = dep;
        });

        // Convert Map to Object for nodePositions
        const nodePositionsObj: { [nodeId: string]: NodePositionState } = {};
        model.layout.nodePositions.forEach((pos, id) => {
            nodePositionsObj[id] = pos;
        });

        // Convert Map to Object for connectionPaths
        const connectionPathsObj: { [connectionId: string]: ConnectionPathState } = {};
        model.layout.connectionPaths.forEach((path, id) => {
            connectionPathsObj[id] = path;
        });

        // Convert Set to Array for UI state
        const uiState = {
            selectedNodeIds: Array.from(model.ui.selectedNodeIds),
            hoveredNodeId: model.ui.hoveredNodeId,
            expandedContainers: Array.from(model.ui.expandedContainers),
            collapsedContainers: Array.from(model.ui.collapsedContainers),
            activeTool: model.ui.activeTool,
            marqueeSelection: model.ui.marqueeSelection
        };

        return {
            version: model.version,
            schemaVersion: model.schemaVersion,
            timestamp: model.timestamp,
            workflowId: model.workflowId,
            availablePlugins: availablePluginsObj,
            workflow: {
                rootNodes: model.workflow.rootNodes,
                allNodes: allNodesObj,
                connections: connectionsObj,
                variableDependencies: variableDependenciesObj
            },
            layout: {
                viewport: model.layout.viewport,
                nodePositions: nodePositionsObj,
                connectionPaths: connectionPathsObj
            },
            ui: uiState,
            availableActions: {
                addNode: {
                    description: "Add a new node to the workflow",
                    parameters: {
                        pluginType: "Plugin type ID (e.g., 'http', 'xpath')",
                        position: "Optional: {x: number, y: number} position on canvas",
                        parentId: "Optional: Parent node ID for nested nodes",
                        attributes: "Optional: Record<string, string> of attribute values"
                    }
                },
                updateNode: {
                    description: "Update node attributes",
                    parameters: {
                        nodeId: "Node ID to update",
                        attributes: "Record<string, string> of new attribute values"
                    }
                },
                removeNode: {
                    description: "Remove a node from the workflow",
                    parameters: {
                        nodeId: "Node ID to remove"
                    }
                },
                connectNodes: {
                    description: "Create a connection between two nodes",
                    parameters: {
                        fromNodeId: "Source node ID",
                        toNodeId: "Target node ID",
                        type: "Optional: 'sequential' | 'conditional' | 'error'"
                    }
                },
                disconnectNodes: {
                    description: "Remove a connection between nodes",
                    parameters: {
                        fromNodeId: "Source node ID",
                        toNodeId: "Target node ID"
                    }
                },
                moveNode: {
                    description: "Move a node to a new position",
                    parameters: {
                        nodeId: "Node ID to move",
                        position: "{x: number, y: number} new position"
                    }
                },
                loadConfig: {
                    description: "Load a workflow configuration from XML",
                    parameters: {
                        xml: "XML configuration string"
                    }
                },
                applyAutoLayout: {
                    description: "Apply automatic layout to the workflow",
                    parameters: {
                        layoutType: "'grid' | 'vertical' | 'horizontal'"
                    }
                },
                startExecution: {
                    description: "Start workflow execution",
                    parameters: {
                        configName: "Optional: Configuration name",
                        waitForWebSocket: "Optional: Wait for WebSocket connection"
                    }
                },
                stopExecution: {
                    description: "Stop workflow execution",
                    parameters: {
                        executionId: "Execution ID to stop"
                    }
                },
                pauseExecution: {
                    description: "Pause workflow execution",
                    parameters: {
                        executionId: "Execution ID to pause"
                    }
                },
                resumeExecution: {
                    description: "Resume paused workflow execution",
                    parameters: {
                        executionId: "Execution ID to resume"
                    }
                },
                getExecutionLogs: {
                    description: "Get execution logs",
                    parameters: {
                        executionId: "Execution ID",
                        fromIndex: "Optional: Starting log index",
                        limit: "Optional: Maximum number of logs",
                        level: "Optional: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'",
                        nodeId: "Optional: Filter by node ID"
                    }
                },
                getExecutionResults: {
                    description: "Get execution results",
                    parameters: {
                        executionId: "Execution ID",
                        variableNames: "Optional: Array of variable names to retrieve"
                    }
                },
                playbackLogs: {
                    description: "Playback execution logs",
                    parameters: {
                        executionId: "Execution ID",
                        fromIndex: "Optional: Starting log index",
                        toIndex: "Optional: Ending log index",
                        speed: "Optional: Playback speed (1.0 = normal)"
                    }
                },
                stopPlayback: {
                    description: "Stop log playback",
                    parameters: {}
                }
            },
            runtime: model.runtime
        };
    }

    /**
     * Dodaj manualne połączenie
     * Zwraca connectionId lub null jeśli nie udało się utworzyć
     */
    addManualConnection(
        fromNodeId: string,
        toNodeId: string,
        fromPortId: string,
        toPortId: string,
        type: 'sequential' | 'conditional' | 'error'
    ): string | null {
        console.log(`[VisualSemanticModelManager] addManualConnection: ${fromNodeId} -> ${toNodeId} (${type})`);

        // Walidacja
        const allNodes = (this.designer as any).getAllNodes() as VisualNode[];
        const fromNode = allNodes.find(n => n.id === fromNodeId);
        const toNode = allNodes.find(n => n.id === toNodeId);

        if (!fromNode || !toNode) {
            console.error(`[VisualSemanticModelManager] addManualConnection: Node not found`);
            return null;
        }

        // Dodaj do ConnectionManager w VisualDesigner
        const connectionManager = (this.designer as any).connectionManager;
        if (!connectionManager) {
            console.error(`[VisualSemanticModelManager] addManualConnection: ConnectionManager not found`);
            return null;
        }

        const connection = connectionManager.createConnection(fromNodeId, toNodeId, type);

        // Odśwież model
        this.notifyUpdate();

        return connection.id;
    }

    /**
     * Usuń połączenie
     */
    removeConnection(connectionId: string): void {
        console.log(`[VisualSemanticModelManager] removeConnection: ${connectionId}`);

        // Usuń z ConnectionManager w VisualDesigner
        const connectionManager = (this.designer as any).connectionManager;
        if (connectionManager) {
            connectionManager.deleteConnection(connectionId);
        }

        // Odśwież model
        this.notifyUpdate();
    }

    /**
     * Zbuduj node entities w ontologii z aktualnych węzłów
     */
    private buildNodeEntities(nodes: VisualNode[]): void {
        // Wyczyść stare node entities
        const oldNodes = this.nodeRepository.getEntitiesByType('Node');
        oldNodes.forEach(node => this.nodeRepository.removeEntity(node.id));

        // Dodaj nowe node entities
        nodes.forEach(node => {
            const plugin = this.findPlugin(node.type);
            if (plugin) {
                const entity = this.ontologyBuilder.buildPluginEntity(plugin, node.id);
                entity.type = 'Node' as EntityType;
                entity.properties.set('position', node.position || { x: 0, y: 0 });
                entity.properties.set('attributes', node.attributes);

                // Znajdź parentId z hierarchii
                const parentId = this.findParentId(node, nodes);
                if (parentId) {
                    entity.properties.set('parentId', parentId);

                    // Dodaj relację hierarchiczną
                    const relationship: Relationship = {
                        id: `rel-${node.id}-CHILD_OF-${parentId}`,
                        type: 'CHILD_OF',
                        from: node.id,
                        to: parentId,
                        properties: new Map(),
                        metadata: {
                            createdAt: Date.now(),
                            confidence: 1.0,
                            source: 'explicit'
                        }
                    };
                    this.nodeRepository.addRelationship(relationship);
                }

                this.nodeRepository.addEntity(entity);
            }
        });
    }

    /**
     * Znajdź parentId dla węzła (rekurencyjnie przeszukaj hierarchię)
     */
    private findParentId(node: VisualNode, allNodes: VisualNode[]): string | null {
        for (const parent of allNodes) {
            if (parent.children && parent.children.includes(node)) {
                return parent.id;
            }
        }
        return null;
    }

    /**
     * Sprawdź czy połączenie dotyczy markerów workflow
     */
    private getMarkerConnectionInfo(conn: { from: string; to: string }): {
        isMarkerConnection: boolean;
        isFromStart: boolean;
        isToEnd: boolean;
        isFromEnd: boolean;
        isToStart: boolean;
    } {
        const isFromStart = conn.from === 'workflow-start';
        const isToEnd = conn.to === 'workflow-end';
        const isFromEnd = conn.from === 'workflow-end';
        const isToStart = conn.to === 'workflow-start';

        return {
            isMarkerConnection: isFromStart || isToEnd || isFromEnd || isToStart,
            isFromStart,
            isToEnd,
            isFromEnd,
            isToStart
        };
    }

    /**
     * Oblicz connection points dla połączeń z markerami
     */
    private calculateMarkerConnectionPoints(
        conn: { from: string; to: string },
        markerInfo: ReturnType<typeof this.getMarkerConnectionInfo>,
        nodeStatesMap: Map<string, VisualNodeState>
    ): {
        fromAbsolute: Point;
        toAbsolute: Point;
        fromSide: Side;
        toSide: Side;
        fromNodeForRouting: VisualNodeState | null;
        toNodeForRouting: VisualNodeState | null;
        fromConnectionPointId: string;
        toConnectionPointId: string;
    } | null {
        const MARKER_RADIUS = 30; // PROCESS_MARKER_RADIUS
        const designerAny = this.designer as any;
        const startPos: { x: number; y: number } | null = designerAny?.startMarkerPosition;
        const endPos: { x: number; y: number } | null = designerAny?.endMarkerPosition;

        // Validate marker positions
        if ((markerInfo.isFromStart && !startPos) ||
            (markerInfo.isToEnd && !endPos) ||
            (markerInfo.isFromEnd && !endPos) ||
            (markerInfo.isToStart && !startPos)) {
            console.warn(`[VisualSemanticModelManager] Marker position not available for connection: ${conn.from} -> ${conn.to}`);
            return null;
        }

        let fromAbsolute: Point;
        let toAbsolute: Point;
        let fromSide: Side = 'bottom';
        let toSide: Side = 'top';
        let fromConnectionPointId: string;
        let toConnectionPointId: string;

        // Calculate FROM connection point
        if (markerInfo.isFromStart) {
            fromAbsolute = { x: startPos!.x, y: startPos!.y + MARKER_RADIUS };
            fromSide = 'bottom';
            fromConnectionPointId = 'marker-output';
        } else if (markerInfo.isFromEnd) {
            fromAbsolute = { x: endPos!.x, y: endPos!.y + MARKER_RADIUS };
            fromSide = 'bottom';
            fromConnectionPointId = 'marker-output';
        } else {
            // Regular node as source
            const fromNode = nodeStatesMap.get(conn.from);
            if (!fromNode) {
                console.warn(`[VisualSemanticModelManager] Source node ${conn.from} not found`);
                return null;
            }
            const targetNode = nodeStatesMap.get(conn.to);
            // SEMANTIC MODEL: Pass all nodes and connections for spatial analysis
            const allNodesArray = Array.from(nodeStatesMap.values());
            const fromPoint = targetNode
                ? this.selectBestConnectionPoint(
                    fromNode,
                    targetNode,
                    'output',
                    new Set(),
                    undefined,
                    allNodesArray,
                    [] // No connections available yet in marker calculation
                )
                : (fromNode.connectionPoints.outputs[0] || null);
            if (!fromPoint) {
                console.warn(`[VisualSemanticModelManager] No connection point found for source node ${conn.from}`);
                return null;
            }
            fromAbsolute = this.toAbsolutePosition(fromPoint.position, fromNode.position);
            fromSide = fromPoint.side;
            fromConnectionPointId = fromPoint.id;
        }

        // Calculate TO connection point
        if (markerInfo.isToEnd) {
            toAbsolute = { x: endPos!.x, y: endPos!.y - MARKER_RADIUS };
            toSide = 'top';
            toConnectionPointId = 'marker-input';
        } else if (markerInfo.isToStart) {
            toAbsolute = { x: startPos!.x, y: startPos!.y - MARKER_RADIUS };
            toSide = 'top';
            toConnectionPointId = 'marker-input';
        } else {
            // Regular node as target
            const toNode = nodeStatesMap.get(conn.to);
            if (!toNode) {
                console.warn(`[VisualSemanticModelManager] Target node ${conn.to} not found`);
                return null;
            }
            const sourceNode = nodeStatesMap.get(conn.from);
            // SEMANTIC MODEL: Pass all nodes and connections for spatial analysis
            const allNodesArray = Array.from(nodeStatesMap.values());
            const toPoint = sourceNode
                ? this.selectBestConnectionPoint(
                    toNode,
                    sourceNode,
                    'input',
                    new Set(),
                    undefined,
                    allNodesArray,
                    [] // No connections available yet in marker calculation
                )
                : (toNode.connectionPoints.inputs[0] || null);
            if (!toPoint) {
                console.warn(`[VisualSemanticModelManager] No connection point found for target node ${conn.to}`);
                return null;
            }
            toAbsolute = this.toAbsolutePosition(toPoint.position, toNode.position);
            toSide = toPoint.side;
            toConnectionPointId = toPoint.id;
        }

        // Get nodes for routing (null for markers)
        const fromNodeForRouting = markerInfo.isFromStart || markerInfo.isFromEnd
            ? null
            : nodeStatesMap.get(conn.from) || null;
        const toNodeForRouting = markerInfo.isToEnd || markerInfo.isToStart
            ? null
            : nodeStatesMap.get(conn.to) || null;

        return {
            fromAbsolute,
            toAbsolute,
            fromSide,
            toSide,
            fromNodeForRouting,
            toNodeForRouting,
            fromConnectionPointId,
            toConnectionPointId
        };
    }

    /**
     * Zbuduj connection state dla połączenia z markerami
     */
    private buildMarkerConnectionState(
        conn: { from: string; to: string; type?: string },
        connectionId: string,
        connectionPoints: ReturnType<typeof this.calculateMarkerConnectionPoints>,
        nodeStates: VisualNodeState[],
        connectionStates: Map<string, VisualConnectionState>,
        nodeStatesMap: Map<string, VisualNodeState>
    ): VisualConnectionState | null {
        if (!connectionPoints) {
            return null;
        }

        const { fromAbsolute, toAbsolute, fromSide, toSide, fromNodeForRouting, toNodeForRouting } = connectionPoints;

        // Route path
        const routingType = fromNodeForRouting && toNodeForRouting
            ? this.selectRoutingType()
            : 'orthogonal';

        // CRITICAL: Set node bounds for collision detection (before routing)
        this.connectionRouter.clearNodeBounds();
        nodeStatesMap.forEach(node => {
            this.connectionRouter.setNodeBounds(node.nodeId, {
                x: node.position.x,
                y: node.position.y,
                width: node.computedBounds.width,
                height: node.computedBounds.height
            });
        });

        // Wyklucz from/to nodes z wykrywania kolizji
        const excludeNodes = fromNodeForRouting && toNodeForRouting
            ? new Set<string>([fromNodeForRouting.nodeId, toNodeForRouting.nodeId])
            : undefined;

        let path: Point[];
        try {
            // PROSTY router - zawsze działa
            path = this.connectionRouter.route(
                fromAbsolute,
                toAbsolute,
                fromSide,
                toSide,
                excludeNodes
            );
        } catch (error) {
            console.error(`[VisualSemanticModelManager] Error routing path for ${connectionId}:`, error);
            // Fallback: prosty L-shaped
            const dx = Math.abs(toAbsolute.x - fromAbsolute.x);
            const dy = Math.abs(toAbsolute.y - fromAbsolute.y);
            if (dx < 0.1 || dy < 0.1) {
                path = [fromAbsolute, toAbsolute];
            } else {
                if (dx < dy) {
                    path = [fromAbsolute, { x: toAbsolute.x, y: fromAbsolute.y }, toAbsolute];
                } else {
                    path = [fromAbsolute, { x: fromAbsolute.x, y: toAbsolute.y }, toAbsolute];
                }
            }
        }

        // PROSTY: Oblicz arrow metadata
        let arrowMetadata;
        try {
            const targetNodeForArrow = toNodeForRouting || nodeStatesMap.get(conn.to);
            if (targetNodeForArrow) {
                const pathEndPoint = path.length > 0 ? path[path.length - 1] : toAbsolute;
                const secondLast = path.length > 1 ? path[path.length - 2] : fromAbsolute;
                const dx = pathEndPoint.x - secondLast.x;
                const dy = pathEndPoint.y - secondLast.y;
                const length = Math.sqrt(dx * dx + dy * dy);
                const normalized = length > 0 ? { x: dx / length, y: dy / length } : { x: 1, y: 0 };
                const angle = Math.atan2(normalized.y, normalized.x);
                arrowMetadata = {
                    endPoint: pathEndPoint,
                    direction: normalized,
                    angle: angle,
                    pointsTowardsNode: true,
                    lastSegmentDirection: normalized,
                    spatialInfo: {
                        arrowTipPosition: pathEndPoint,
                        arrowBaseCenter: {
                            x: pathEndPoint.x - Math.cos(angle) * 10,
                            y: pathEndPoint.y - Math.sin(angle) * 10
                        },
                        arrowSize: 10,
                        distanceToTargetCenter: Math.sqrt(
                            Math.pow(targetNodeForArrow.computedBounds.centerX - pathEndPoint.x, 2) +
                            Math.pow(targetNodeForArrow.computedBounds.centerY - pathEndPoint.y, 2)
                        ),
                        angleDegrees: angle * 180 / Math.PI,
                        isHorizontal: Math.abs(normalized.y) < 0.1,
                        isVertical: Math.abs(normalized.x) < 0.1,
                        quadrant: 1 as 1 | 2 | 3 | 4
                    }
                };
            }
        } catch (error) {
            console.warn(`[VisualSemanticModelManager] Error calculating arrow metadata for ${connectionId}:`, error);
        }

        // Calculate path geometry
        let pathGeometry;
        let pathLength = 0;
        try {
            pathGeometry = this.calculatePathGeometry(path, connectionId, connectionStates);
            pathLength = pathGeometry.segments.reduce((sum, seg) => sum + seg.length, 0);
        } catch (error) {
            console.error(`[VisualSemanticModelManager] Error calculating path geometry for ${connectionId}:`, error);
            pathGeometry = {
                startPoint: fromAbsolute,
                endPoint: toAbsolute,
                turningPoints: [],
                segments: [],
                selfIntersections: [],
                crossesOtherConnections: [],
                reversesDirection: false,
                maxDeviation: 0
            };
        }

        const spatialMetadata = {
            totalLength: pathLength,
            segmentCount: pathGeometry.segments.length,
            minDistanceToNode: this.calculateMinDistanceToNodes(path, nodeStates),
            crossesOtherConnections: pathGeometry.crossesOtherConnections.length > 0,
            boundingBox: this.calculatePathBoundingBox(path),
            startSide: fromSide,
            endSide: toSide,
            startPoint: fromAbsolute,
            endPoint: toAbsolute,
            turningPointsCount: this.countTurningPoints(path),
            passesThroughNodes: this.detectPathNodeIntersections(path, nodeStates),
            labelCollisions: this.detectLabelCollisions(path, nodeStates)
        };

        return {
            connectionId,
            fromNodeId: conn.from,
            toNodeId: conn.to,
            type: (conn.type as 'sequential' | 'conditional' | 'error') || 'sequential',
            path: {
                points: path,
                routingType: routingType,
                arrowMetadata: arrowMetadata || undefined,
                spatialMetadata: spatialMetadata
            },
            anchors: {
                from: {
                    nodeId: conn.from,
                    connectionPointId: connectionPoints.fromConnectionPointId,
                    position: fromAbsolute
                },
                to: {
                    nodeId: conn.to,
                    connectionPointId: connectionPoints.toConnectionPointId,
                    position: toAbsolute
                }
            },
            visualState: {
                selected: false,
                hovered: false,
                highlighted: false,
                error: false
            },
            edgeValidation: {
                startPointOnEdge: true,
                endPointOnEdge: true,
                startPointTolerance: 0,
                endPointTolerance: 0,
                arrowPointsTowardsTarget: true
            },
            spatialValidation: {
                pathLength: pathLength,
                isValidPath: true,
                startNodeDistance: 0,
                endNodeDistance: 0,
                minClearance: 0,
                routingQuality: 'excellent' as const
            },
            pathGeometry: pathGeometry,
            renderCache: {
                svgPath: null,
                lastRenderTime: 0,
                needsRerender: false
            }
        };
    }
}

