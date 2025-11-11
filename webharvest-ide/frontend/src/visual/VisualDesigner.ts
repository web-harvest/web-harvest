/**
 * VisualDesigner - Implementation (TDD Green Phase)
 * Core visual designer for canvas-based workflow editing
 * 
 * Supports hybrid rendering model:
 * - NESTED: Container nodes (loop, if, try) render children inside
 * - CONNECTED: Action nodes render as flowchart with arrows
 */

import type { VisualNode } from '../types';
import {
    isContainerNode,
    getRenderingStrategy,
    generateConnections,
    type Connection,
    setContainerResolver
} from './VisualModelHelpers';
import { NodeStateRenderer, type NodeExecutionState } from './NodeStateRenderer';
import { NodeMetricsOverlay, type NodeMetrics } from './NodeMetricsOverlay';
import { GridSystem } from './GridSystem';
import { Minimap } from './Minimap';
import { IconLibrary } from './IconLibrary';
import { ContainerRenderer } from './ContainerRenderer';
import { PortSystem, type PortType } from './PortSystem';
import { ConnectionManager, type Connection as ManualConnection, type ConnectionType } from './ConnectionManager';
import { XMLGenerator } from './XMLGenerator';
import { SemanticValidator } from './SemanticValidator';
import { NodeResizeHandler } from './NodeResizeHandler';
import { BreadcrumbNav, type BreadcrumbItem } from './BreadcrumbNav';
import { DrillDownManager } from './DrillDownManager';
import type { PluginManager } from '../core/PluginManager';
import {
    ControlDetailType,
    createControlDetailNode,
    partitionIfChildren,
    partitionTryChildren
} from './control/ControlNodeUtils';
import { planConnections } from './connections/ConnectionPlanner';
import { MasterDetailRegistry, type DetailSectionDefinition } from './detail/MasterDetailRegistry';
import { analyzeHierarchy, analyzeContainer } from './HierarchyInspector';
type DesignerSnapshot = {
    nodes: VisualNode[];
    connections: ManualConnection[];
    pan: { x: number; y: number };
    zoom: number;
};

type ContextMenuActionItem = {
    label: string;
    icon?: string;
    action?: () => void;
    danger?: boolean;
    disabled?: boolean;
};

type ContextMenuItem = ContextMenuActionItem | { type: 'divider' };

type ContextMenuModel = {
    items: ContextMenuItem[];
    element?: HTMLElement | null;
};

type EventListener = (data: any) => void;

type WorkflowStatus = 'idle' | 'running' | 'paused';

export class VisualDesigner {
    private nodes: VisualNode[] = [];
    private connections: Connection[] = [];
    private container: HTMLElement;
    private canvasElement: HTMLElement;
    private svgElement: SVGSVGElement | null = null;
    private processMarkersLayer: HTMLElement | null = null;
    private selectedNodeId: string | null = null;
    private selectedNodeIds: string[] = [];
    private selectedConnectionId: string | null = null;
    private eventListeners: Map<string, EventListener[]> = new Map();
    private connectionLineElements: Map<string, SVGLineElement> = new Map(); // Store connection line references

    // Execution state visualization
    private stateRenderer: NodeStateRenderer = new NodeStateRenderer();
    private metricsOverlay: NodeMetricsOverlay = new NodeMetricsOverlay();
    private gridSystem: GridSystem = new GridSystem(20);
    private gridInitialized = false;
    private gridVisible: boolean = true;
    private minimap: Minimap = new Minimap();
    private iconLibrary: IconLibrary = new IconLibrary();
    private containerRenderer: ContainerRenderer = new ContainerRenderer();
    private portSystem: PortSystem = new PortSystem();
    private connectionManager: ConnectionManager = new ConnectionManager();
    private breadcrumbNav: BreadcrumbNav = new BreadcrumbNav();
    private breadcrumbContainer: HTMLElement | null = null;
    private focusSummaryContainer: HTMLElement | null = null;
    private drillDownManager: DrillDownManager = new DrillDownManager();
    private visibleNodes: VisualNode[] = [];

    // History & clipboard
    private undoStack: DesignerSnapshot[] = [];
    private redoStack: DesignerSnapshot[] = [];
    private clipboard: VisualNode[] = [];
    private activeContextMenu: HTMLElement | null = null;
    private activeContextMenuHandler: ((event: MouseEvent) => void) | null = null;

    // Container expand/collapse state
    private collapsedContainers: Set<string> = new Set();

    // Layout constants (fallback values if metadata not available)
    private readonly DEFAULT_NODE_WIDTH = 250;
    private readonly DEFAULT_NODE_HEIGHT = 100;
    private readonly HORIZONTAL_GAP = 180;
    private readonly START_X = 50;
    private readonly START_Y = 50;
    private readonly PADDING = 20;

    // PluginManager reference for accessing VisualPluginMetadata
    private pluginManager: PluginManager | null = null;

    // Auto-layout measurement caches
    private measuredNodeSizes: Map<string, { width: number; height: number }> = new Map();
    private currentRenderMeasurements: Map<string, { width: number; height: number }> | null = null;
    private pendingRelayout = false;
    private isNodeDragging = false;
    private startMarkerPosition: { x: number; y: number } | null = null;
    private endMarkerPosition: { x: number; y: number } | null = null;
    private startMarkerManual = false;
    private endMarkerManual = false;
    private isMarkerDragging = false;

    // Drag & move state
    private dragState: {
        nodeId: string;
        offsetX: number;
        offsetY: number;
        initialX: number;
        initialY: number;
    } | null = null;

    // Connection drag state
    private connectionDragState: {
        fromNodeId: string;
        fromPortElement: HTMLElement;
        fromPortType: string;
        temporaryLine: SVGLineElement | null;
        isReconnect: boolean; // True if reconnecting existing connection
        existingConnectionId: string | null; // ID of connection being reconnected
        reconnectEnd: 'source' | 'target' | null; // Which end is being reconnected
    } | null = null;

    // Zoom & pan state
    private zoom: number = 1.0;
    private panOffset: { x: number; y: number } = { x: 0, y: 0 };
    private readonly MIN_ZOOM = 0.1;
    private readonly MAX_ZOOM = 3.0;

    // Grid snap
    private gridSnapEnabled: boolean = false;
    private gridSize: number = 20;

    // Marquee selection
    private marqueeState: {
        startX: number;
        startY: number;
        endX: number;
        endY: number;
        isActive: boolean;
    } | null = null;
    private marqueeElement: HTMLElement | null = null;

    // Single Configuration Mode (Conductor-style)
    private isDirtyFlag: boolean = false;
    private currentFilename: string | null = null;
    private autoSaveEnabled: boolean = false;
    private autoSaveInterval: number | null = null;

    // Semantic validation
    private semanticValidator: SemanticValidator | null = null;
    private validationErrors: Map<string, string> = new Map(); // nodeId -> error message
    private invalidConnections: Set<string> = new Set(); // connection IDs with validation errors

    // Node resize handler
    private resizeHandler!: NodeResizeHandler; // Initialized in constructor

    // Workflow execution state for process markers
    private workflowStatus: WorkflowStatus = 'idle';

    private startMarkerElement: HTMLElement | null = null;
    private endMarkerElement: HTMLElement | null = null;
    private lastClickTime: number = 0;
    private detailRegistry: MasterDetailRegistry | null = null;

    constructor(containerId: string, pluginManager?: PluginManager | null) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        if (!this.container.hasAttribute('tabindex')) {
            this.container.tabIndex = 0;
        }
        this.pluginManager = pluginManager || null;
        this.breadcrumbNav.setIconResolver((item) => {
            if (item.level === 0) {
                return this.createFontAwesomeIcon('fa-house', '#636366', 12);
            }
            if (item.nodeType) {
                return this.iconLibrary.getIconElement(item.nodeType, 12);
            }
            return null;
        });
        this.canvasElement = this.createCanvasElement();
        // createCanvasElement() creates wrapper and appends it, so no need to append again

        // Initialize semantic validator if plugin manager provided
        if (pluginManager) {
            this.semanticValidator = new SemanticValidator(pluginManager);
            this.detailRegistry = new MasterDetailRegistry(pluginManager);
        }

        // Initialize resize handler
        this.resizeHandler = new NodeResizeHandler();
        this.resizeHandler.setOnResize((nodeId, dimensions) => {
            this.handleNodeResize(nodeId, dimensions);
        });

        // Setup drag & drop
        this.setupDragAndDrop();

        // Setup container renderer callbacks
        this.containerRenderer.onToggle((containerId: string, expanded: boolean) => {
            this.containerRenderer.setExpanded(containerId, expanded);
            if (expanded) {
                this.collapsedContainers.delete(containerId);
            } else {
                this.collapsedContainers.add(containerId);
            }
            this.render(); // Re-render to update container state
        });

        this.breadcrumbNav.onNavigate((item) => {
            this.handleBreadcrumbNavigate(item);
        });

        this.container.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                this.exitFocusLevel();
            }
        });
    }

    private removeNodeInternal(nodeId: string, nodes: VisualNode[]): boolean {
        const index = nodes.findIndex(n => n.id === nodeId);
        if (index !== -1) {
            nodes.splice(index, 1);
            return true;
        }

        for (const node of nodes) {
            if (node.children && node.children.length > 0) {
                const removed = this.removeNodeInternal(nodeId, node.children);
                if (removed) {
                    return true;
                }
            }
        }

        return false;
    }

    private removeConnectionsForNode(nodeId: string): void {
        const related = this.connectionManager
            .getConnections()
            .filter(conn => conn.from === nodeId || conn.to === nodeId);

        related.forEach(conn => this.deleteConnectionInternal(conn.id));
    }

    private updateNodeInternal(nodeId: string, updatedNode: VisualNode, nodes: VisualNode[]): boolean {
        const index = nodes.findIndex(n => n.id === nodeId);
        if (index !== -1) {
            nodes[index] = updatedNode;
            return true;
        }

        for (const node of nodes) {
            if (node.children && node.children.length > 0) {
                const replaced = this.updateNodeInternal(nodeId, updatedNode, node.children);
                if (replaced) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Create canvas element - use existing #canvas-area container directly
     * Create inner wrapper for transform (prevents container from floating)
     */
    private createCanvasElement(): HTMLElement {
        const shell = document.createElement('div');
        shell.className = 'canvas-shell';
        shell.style.display = 'flex';
        shell.style.flexDirection = 'column';
        shell.style.width = '100%';
        shell.style.height = '100%';

        const breadcrumbBar = document.createElement('div');
        breadcrumbBar.className = 'canvas-breadcrumb-bar';

        const breadcrumbWrapper = document.createElement('div');
        breadcrumbWrapper.className = 'breadcrumb-nav';
        breadcrumbBar.appendChild(breadcrumbWrapper);
        this.breadcrumbContainer = breadcrumbWrapper;

        const focusSummary = document.createElement('div');
        focusSummary.className = 'focus-summary';
        breadcrumbBar.appendChild(focusSummary);
        this.focusSummaryContainer = focusSummary;

        shell.appendChild(breadcrumbBar);

        const contentWrapper = document.createElement('div');
        contentWrapper.className = 'canvas-content-wrapper';
        contentWrapper.style.position = 'relative';
        contentWrapper.style.width = '100%';
        contentWrapper.style.minHeight = '100%';
        contentWrapper.style.flex = '1';

        shell.appendChild(contentWrapper);
        this.container.appendChild(shell);

        this.updateBreadcrumbTrail();
        this.updateFocusSummary();

        return contentWrapper;
    }

    /**
     * Load nodes (replaces existing)
     */
    loadNodes(nodes: VisualNode[]): void {
        this.nodes = this.cloneNodes(nodes);
        this.measuredNodeSizes.clear();
        this.startMarkerManual = false;
        this.endMarkerManual = false;
        this.startMarkerPosition = null;
        this.endMarkerPosition = null;
        this.connectionManager.clear();
        this.clearHistory();
        this.selectedNodeIds = [];
        this.selectedNodeId = null;
        this.clipboard = [];
        this.panOffset = { x: 0, y: 0 };
        this.zoom = 1.0;
        this.render();
        this.applyTransform();
    }

    /**
     * Add node to canvas
     */
    addNode(node: VisualNode): void {
        this.pushHistorySnapshot();
        const newNode = this.cloneNode(node);
        if (!newNode.children) {
            newNode.children = [];
        }
        this.nodes.push(newNode);
        this.markDirty();
        this.render();
    }

    /**
     * Remove node by ID
     */
    removeNode(nodeId: string): boolean {
        const existing = this.findNode(nodeId);
        if (!existing) {
            return false;
        }

        const affectedIds = this.getNodeAndDescendantIdsById(nodeId);
        this.pushHistorySnapshot();

        const removed = this.removeNodeInternal(nodeId, this.nodes);
        if (!removed) {
            // Revert history push if nothing was removed
            this.undoStack.pop();
            return false;
        }

        affectedIds.forEach(id => this.removeConnectionsForNode(id));

        this.selectedNodeIds = this.selectedNodeIds.filter(id => !affectedIds.includes(id));
        if (this.selectedNodeId && affectedIds.includes(this.selectedNodeId)) {
            this.selectedNodeId = null;
        }

        this.markDirty();
        this.render();
        this.highlightSelectedNodes();
        this.emit('nodeRemoved', nodeId);

        return true;
    }

    /**
     * Update node
     */
    updateNode(nodeId: string, updatedNode: VisualNode): boolean {
        const existing = this.findNode(nodeId);

        if (!existing) {
            return false;
        }

        this.pushHistorySnapshot();

        const clone = this.cloneNode(updatedNode);
        const updated = this.updateNodeInternal(nodeId, clone, this.nodes);

        if (!updated) {
            this.undoStack.pop();
            return false;
        }

        this.markDirty();
        this.render();
        this.emit('nodeUpdated', clone);

        return true;
    }

    /**
     * Get all nodes
     */
    getNodes(): VisualNode[] {
        return [...this.nodes]; // Return copy
    }

    /**
     * Get all nodes including nested children (flattened)
     */
    getAllNodes(): VisualNode[] {
        const flattened: VisualNode[] = [];

        const traverse = (node: VisualNode): void => {
            flattened.push(node);
            if (node.children && node.children.length > 0) {
                node.children.forEach(traverse);
            }
        };

        this.nodes.forEach(traverse);
        return flattened;
    }

    /**
     * Find node by ID
     */
    findNode(nodeId: string): VisualNode | null {
        return this.findNodeRecursive(nodeId, this.nodes);
    }

    /**
     * Clear all nodes
     */
    clear(): void {
        this.nodes = [];
        this.canvasElement.innerHTML = '';
        this.selectedNodeId = null;
        this.selectedNodeIds = [];
    }

    /**
     * Select node
     */
    selectNode(nodeId: string, options?: { addToSelection?: boolean }): void {

        if (options?.addToSelection) {
            if (!this.selectedNodeIds.includes(nodeId)) {
                this.selectedNodeIds.push(nodeId);
            }
        } else {
            this.selectedNodeIds = [nodeId];
        }

        this.selectedNodeId = nodeId;
        this.highlightSelectedNodes();

        // Emit event
        this.emit('nodeSelected', nodeId);
    }

    /**
     * Clear selection
     */
    clearSelection(): void {
        this.selectedNodeIds = [];
        this.selectedNodeId = null;
        this.highlightSelectedNodes();
    }

    /**
     * Get selected node ID
     */
    getSelectedNodeId(): string | null {
        return this.selectedNodeId;
    }

    /**
     * Add event listener
     */
    addEventListener(event: string, listener: EventListener): void {
        if (!this.eventListeners.has(event)) {
            this.eventListeners.set(event, []);
        }
        this.eventListeners.get(event)!.push(listener);
    }

    /**
     * Add event listener (alias for addEventListener)
     */
    on(event: string, listener: EventListener): void {
        this.addEventListener(event, listener);
    }

    /**
     * Emit event
     */
    private emit(event: string, data: any): void {
        const listeners = this.eventListeners.get(event);
        if (listeners) {
            listeners.forEach(listener => listener(data));
        }
    }

    /**
     * Apply horizontal auto-layout to root-level nodes, respecting manual overrides.
     */
    private applyAutoLayoutToRootNodes(nodes: VisualNode[]): void {
        if (!nodes || nodes.length === 0) {
            return;
        }

        let cursorX = this.START_X;
        const baselineY = this.START_Y;
        const gap = this.HORIZONTAL_GAP;

        nodes.forEach((node) => {
            const { width } = this.getNodeDimensions(node);

            if (node.isManualPositioned && node.position) {
                // Honour manual placement but ensure subsequent auto nodes start after it
                cursorX = Math.max(cursorX, node.position.x + width + gap);
                return;
            }

            if (!node.position) {
                node.position = { x: cursorX, y: baselineY };
            } else {
                node.position.x = cursorX;
                node.position.y = baselineY;
            }

            node.isManualPositioned = false;
            cursorX += width + gap;
        });
    }

    private recordMeasuredNodeSize(nodeId: string, element: HTMLElement): void {
        if (!this.currentRenderMeasurements) {
            return;
        }

        const rect = element.getBoundingClientRect();
        if (!rect.width || !rect.height) {
            return;
        }

        const width = Math.round(rect.width / this.zoom);
        const height = Math.round(rect.height / this.zoom);
        this.currentRenderMeasurements.set(nodeId, { width, height });
    }

    private finalizeRenderMeasurements(): void {
        if (!this.currentRenderMeasurements) {
            return;
        }

        const newMeasurements = this.currentRenderMeasurements;
        let changed = false;

        if (newMeasurements.size !== this.measuredNodeSizes.size) {
            changed = true;
        } else {
            for (const [nodeId, size] of newMeasurements) {
                const existing = this.measuredNodeSizes.get(nodeId);
                if (!existing || Math.abs(existing.width - size.width) > 1 || Math.abs(existing.height - size.height) > 1) {
                    changed = true;
                    break;
                }
            }
        }

        this.measuredNodeSizes = new Map(newMeasurements);
        this.currentRenderMeasurements = null;

        if (changed && !this.pendingRelayout && !this.isNodeDragging) {
            this.pendingRelayout = true;
            if (typeof requestAnimationFrame === 'function') {
                requestAnimationFrame(() => {
                    this.pendingRelayout = false;
                    this.render();
                });
            } else {
                setTimeout(() => {
                    this.pendingRelayout = false;
                    this.render();
                }, 16);
            }
        }
    }

    private ensureGridOverlay(): void {
        if (!this.gridVisible) {
            return;
        }
        const existingOverlay = this.canvasElement.querySelector('.grid-overlay');
        if (!this.gridInitialized || !existingOverlay) {
            this.gridSystem.render(this.canvasElement);
            this.gridInitialized = true;
        } else {
            this.gridSystem.updateDimensions();
        }
    }

    private rebuildConnections(reapplyLayout: boolean = true, sourceNodes?: VisualNode[]): VisualNode[] {
        const originNodes = sourceNodes ?? this.nodes;
        const linearNodes = [...originNodes];

        if (reapplyLayout) {
            this.applyAutoLayoutToRootNodes(linearNodes);
        }

        linearNodes.sort((a, b) => {
            const ax = a.position?.x ?? 0;
            const bx = b.position?.x ?? 0;
            if (ax !== bx) {
                return ax - bx;
            }
            const ay = a.position?.y ?? 0;
            const by = b.position?.y ?? 0;
            return ay - by;
        });

        const inFocus = this.drillDownManager.getCurrentId() !== null;
        const connectionCandidates = this.filterNodesForConnections(linearNodes);
        const planned = (!inFocus && connectionCandidates.length > 0)
            ? planConnections(connectionCandidates, 'workflow-start', 'workflow-end')
            : [];
        const manualConnections = connectionCandidates.length > 0
            ? generateConnections(connectionCandidates)
            : [];
        this.connections = [...planned, ...manualConnections];

        return linearNodes;
    }

    private filterNodesForConnections(nodes: VisualNode[]): VisualNode[] {
        if (!nodes.length) {
            return nodes;
        }

        if (!this.detailRegistry) {
            return nodes;
        }

        const detailTypes = this.collectDetailTypesForConnections();
        if (detailTypes.size === 0) {
            return nodes;
        }

        return nodes.filter(node => !detailTypes.has(node.type));
    }

    private collectDetailTypesForConnections(): Set<string> {
        const types = new Set<string>();

        if (!this.detailRegistry) {
            return types;
        }

        const queue: VisualNode[] = [...this.nodes];
        const visited = new Set<string>();

        while (queue.length > 0) {
            const current = queue.shift()!;
            if (visited.has(current.id)) {
                continue;
            }
            visited.add(current.id);

            const sections = this.detailRegistry.getSections(current.type);
            sections.forEach(section => types.add(section.detailType));

            if (current.children && current.children.length > 0) {
                queue.push(...current.children);
            }
        }

        return types;
    }

    private updateProcessMarkersPosition(): void {
        if (!this.processMarkersLayer || !this.startMarkerElement || !this.endMarkerElement) {
            return;
        }

        const rootNodeElements = Array.from(this.canvasElement.querySelectorAll('.visual-node'))
            .filter((element): element is HTMLElement => element instanceof HTMLElement)
            .filter(element => element.dataset.nodeId && !element.dataset.parentId);

        if (rootNodeElements.length === 0) {
            return;
        }

        const positions = this.calculateProcessMarkerPositions(rootNodeElements);

        if (!this.startMarkerManual) {
            this.startMarkerPosition = { x: positions.startX, y: positions.startY };
            this.startMarkerElement.style.left = `${positions.startX}px`;
            this.startMarkerElement.style.top = `${positions.startY}px`;
        }

        if (!this.endMarkerManual) {
            this.endMarkerPosition = { x: positions.endX, y: positions.endY };
            this.endMarkerElement.style.left = `${positions.endX}px`;
            this.endMarkerElement.style.top = `${positions.endY}px`;
        }
    }

    /**
     * Create SVG overlay for connections
     */
    private createSVGOverlay(): SVGSVGElement {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.style.position = 'absolute';
        svg.style.top = '0';
        svg.style.left = '0';
        svg.style.width = '100%';
        svg.style.height = '100%';
        svg.style.pointerEvents = 'none';
        svg.style.zIndex = '1';
        // SVG should match container size (canvas-area), not transformed wrapper
        // Size will be updated in applyTransform()
        const containerRect = this.container.getBoundingClientRect();
        if (containerRect.width > 0 && containerRect.height > 0) {
            svg.setAttribute('width', containerRect.width.toString());
            svg.setAttribute('height', containerRect.height.toString());
        }
        return svg;
    }

    /**
     * Render all nodes with hybrid model (nested + connected)
     */
    private render(): void {
        const focusedContainer = this.getFocusedContainer();
        this.updateBreadcrumbTrail();
        const nodesToRender = focusedContainer ? (focusedContainer.children || []) : this.nodes;
        this.visibleNodes = nodesToRender;

        this.canvasElement.innerHTML = '';
        this.currentRenderMeasurements = new Map();

        // Create SVG overlay for connections
        this.svgElement = this.createSVGOverlay();
        this.canvasElement.appendChild(this.svgElement);

        const shouldAutoLayout = !focusedContainer;
        this.rebuildConnections(shouldAutoLayout, nodesToRender);

        // Render nodes
        nodesToRender.forEach(node => this.renderNodeWithStrategy(node, this.canvasElement));

        const rootNodeElements = Array.from(this.canvasElement.querySelectorAll('.visual-node'))
            .filter((element): element is HTMLElement => element instanceof HTMLElement)
            .filter(element => element.dataset.nodeId && !element.dataset.parentId);

        if (!focusedContainer) {
            this.renderProcessMarkers(rootNodeElements);
            this.updateProcessMarkersPosition();
        } else {
            this.clearProcessMarkers();
        }

        // Render connections
        this.renderConnections();

        // Render minimap if enabled
        if (this.minimapVisible && !focusedContainer) {
            this.minimap.render(this.canvasElement, nodesToRender);

            // Setup navigation callback
            this.minimap.onNavigate((position) => {
                this.handleMinimapClick(position);
            });
        } else if (this.minimapVisible && focusedContainer) {
            const existingMinimap = this.canvasElement.querySelector('.minimap');
            if (existingMinimap) {
                existingMinimap.remove();
            }
        }

        this.ensureGridOverlay();
        this.finalizeRenderMeasurements();
        this.highlightSelectedNodes();
        this.updateFocusSummary();
    }

    /**
     * Render single node
     */
    private renderNode(node: VisualNode): void {
        const nodeElement = document.createElement('div');
        nodeElement.className = 'visual-node';
        nodeElement.dataset.nodeId = node.id;
        nodeElement.style.padding = '12px';
        nodeElement.style.margin = '8px';
        nodeElement.style.background = 'white';
        nodeElement.style.border = '2px solid #FFC107';
        nodeElement.style.borderRadius = '6px';
        nodeElement.style.cursor = 'pointer';
        nodeElement.style.display = 'inline-block';
        nodeElement.style.minWidth = '120px';

        // Node header with icon
        const header = document.createElement('div');
        header.style.cssText = 'display: flex; align-items: center; gap: 8px; font-weight: 600; margin-bottom: 8px; font-size: 14px;';

        // Icon from IconLibrary
        const iconConfig = this.iconLibrary.getIcon(node.type);
        const icon = this.iconLibrary.getIconElement(node.type, 16);
        header.appendChild(icon);

        // Type name
        const typeText = document.createElement('span');
        typeText.textContent = node.type;
        typeText.style.color = iconConfig.color;
        header.appendChild(typeText);

        nodeElement.appendChild(header);

        // Update border color based on category
        nodeElement.style.borderColor = iconConfig.color;

        // Attributes
        if (Object.keys(node.attributes).length > 0) {
            const attrs = document.createElement('div');
            attrs.style.fontSize = '12px';
            attrs.style.color = '#666';

            Object.entries(node.attributes).forEach(([key, value]) => {
                const attr = document.createElement('div');
                attr.textContent = `${key}: ${value}`;
                attrs.appendChild(attr);
            });

            nodeElement.appendChild(attrs);
        }

        // Body
        if (node.body) {
            const body = document.createElement('div');
            body.style.marginTop = '8px';
            body.style.fontSize = '11px';
            body.style.color = '#999';
            body.style.fontStyle = 'italic';
            body.textContent = node.body.length > 50 ? node.body.substring(0, 50) + '...' : node.body;
            nodeElement.appendChild(body);
        }

        // Click to select
        nodeElement.onclick = () => {
            this.selectNode(node.id);
        };

        this.canvasElement.appendChild(nodeElement);
    }

    // ========================================================================
    // Hybrid Rendering Methods (Nested + Connected)
    // ========================================================================

    /**
     * Render node with strategy selection
     */
    private renderNodeWithStrategy(node: VisualNode, parentContainer: HTMLElement = this.canvasElement): HTMLElement {
        const strategy = getRenderingStrategy(node);

        if (strategy === 'nested') {
            return this.renderNestedNode(node, parentContainer);
        }

        return this.renderConnectedNode(node, parentContainer);
    }

    /**
     * Render nested container node using ContainerRenderer (FULL SCALE!)
     */
    private renderNestedNode(node: VisualNode, parentContainer: HTMLElement): HTMLElement {
        const isRootLevel = parentContainer === this.canvasElement;
        let containerX = node.position?.x ?? this.START_X;
        let containerY = node.position?.y ?? this.START_Y;

        if (isRootLevel && node.position) {
            containerX = node.position.x;
            containerY = node.position.y;
        }

        // Render container using ContainerRenderer
        const container = document.createElement('div');
        container.className = 'visual-node nested-container';
        container.dataset.nodeId = node.id;
        const containerDimensions = this.getNodeDimensions(node);
        container.style.minWidth = `${containerDimensions.width}px`;
        container.style.minHeight = `${containerDimensions.height}px`;

        if (!isRootLevel && parentContainer.dataset.nodeId) {
            container.dataset.parentId = parentContainer.dataset.nodeId;
        }

        if (!isRootLevel) {
            container.style.position = 'relative';
            container.style.left = '0';
            container.style.top = '0';
            container.style.zIndex = '1';
            container.style.margin = '8px 0';
        } else {
            container.style.position = 'absolute';
            container.style.left = `${containerX}px`;
            container.style.top = `${containerY}px`;
            container.style.zIndex = '2';
            container.classList.add('nested-root');
        }

        // Apply role-based styling for containers
        if (this.semanticValidator) {
            const roles = this.semanticValidator.getRoles(node.type);
            container.dataset.roles = roles.join(',');
            if (roles.length > 0) {
                container.dataset.primaryRole = roles[0];
                this.applyRoleBasedStyling(container, roles, node);
            }
        }

        // Check expansion state using ContainerRenderer
        const isExpanded = this.containerRenderer.isExpanded(node.id);
        if (!isExpanded && !this.collapsedContainers.has(node.id)) {
            // First time - default to expanded
            this.containerRenderer.setExpanded(node.id, true);
        }
        const isCollapsed = !this.containerRenderer.isExpanded(node.id);

        // Get container state from ContainerRenderer
        const containerState = this.containerRenderer.getContainerState(node.id);

        if (isCollapsed) {
            // Render collapsed state using ContainerRenderer
            this.containerRenderer.renderCollapsed(container, node, containerState);
        } else {
            // Render expanded state using ContainerRenderer
            // ContainerRenderer.renderExpanded() already renders children, but we need to use our own rendering
            // So we'll render children ourselves after ContainerRenderer creates the structure
            this.containerRenderer.renderExpanded(container, node);

            // Clear default children rendered by ContainerRenderer and use our own rendering
            const childrenArea = container.querySelector('.container-children') as HTMLElement | null;
            if (childrenArea) {
                // Clear default simple children
                childrenArea.innerHTML = '';

                const detailSections = this.detailRegistry ? this.detailRegistry.getSections(node.type) : [];

                if (this.isIfNode(node)) {
                    const { elseNode, primaryChildren } = partitionIfChildren(node);
                    this.renderIfDetail(childrenArea, node, elseNode);
                    this.renderStandardChildren(primaryChildren, node, childrenArea);
                } else if (this.isTryNode(node)) {
                    const { catchNodes, primaryChildren } = partitionTryChildren(node);
                    this.renderTryDetail(childrenArea, node, catchNodes);
                    this.renderStandardChildren(primaryChildren, node, childrenArea);
                } else if (detailSections.length > 0 && this.detailRegistry) {
                    this.renderMasterDetailContainer(childrenArea, node, detailSections);
                } else {
                    this.renderStandardChildren(node.children || [], node, childrenArea);
                }
            } else {
                console.warn(`[VisualDesigner] Container children area not found for node ${node.id}`);
            }

            const headerElement = container.querySelector('.container-header');
            if (headerElement) {
                headerElement.addEventListener('dblclick', (event) => {
                    event.stopPropagation();
                    this.enterContainer(node.id);
                });
            }
        }

        // Click handler for selection (header handles expand/collapse via ContainerRenderer)
        container.addEventListener('click', (e) => {
            // Don't select if clicking on header (ContainerRenderer handles toggle)
            if (!(e.target as HTMLElement).closest('.container-header')) {
                e.stopPropagation();
                this.selectNode(node.id);
            }
        });

        // Setup drag handlers for container movement
        this.setupNodeDragHandlers(container, node);

        parentContainer.appendChild(container);
        if (isRootLevel) {
            this.recordMeasuredNodeSize(node.id, container);
        }

        return container;
    }

    private renderStandardChildren(children: VisualNode[], parentNode: VisualNode, container: HTMLElement): void {
        children.forEach((child, index) => {
            const childElement = this.renderNodeWithStrategy(child, container);
            childElement.dataset.parentId = parentNode.id;
            childElement.dataset.childIndex = index.toString();

            this.addReorderButtons(childElement, parentNode.id, child.id, index, children.length);
        });
    }

    /**
     * Render connected action node
     */
    private renderConnectedNode(node: VisualNode, parentContainer: HTMLElement): HTMLElement {
        const nodeElement = this.createBasicNodeElement(node);
        const isRootLevel = parentContainer === this.canvasElement;

        if (!isRootLevel && parentContainer.dataset.nodeId) {
            nodeElement.dataset.parentId = parentContainer.dataset.nodeId;
        }

        if (!isRootLevel) {
            nodeElement.style.position = 'relative';
            nodeElement.style.left = '0';
            nodeElement.style.top = '0';
            nodeElement.style.margin = '4px 0';
            nodeElement.style.zIndex = '1';
            parentContainer.appendChild(nodeElement);
        } else {
            const nodeX = node.position?.x ?? this.START_X;
            const nodeY = node.position?.y ?? this.START_Y;

            nodeElement.style.position = 'absolute';
            nodeElement.style.left = `${nodeX}px`;
            nodeElement.style.top = `${nodeY}px`;
            nodeElement.style.zIndex = '2';

            parentContainer.appendChild(nodeElement);
            this.recordMeasuredNodeSize(node.id, nodeElement);
        }

        return nodeElement;
    }

    /**
     * Get node dimensions from VisualPluginMetadata, node.dimensions, or use defaults
     */
    private getNodeDimensions(nodeOrType: VisualNode | string): { width: number; height: number } {
        const isVisualNode = typeof nodeOrType !== 'string';
        if (isVisualNode) {
            const visualNode = nodeOrType as VisualNode;
            if (visualNode.dimensions) {
                return visualNode.dimensions;
            }

            const measured = this.measuredNodeSizes.get(visualNode.id);
            if (measured) {
                return measured;
            }
        }

        const type = isVisualNode ? (nodeOrType as VisualNode).type : nodeOrType;

        if (this.pluginManager) {
            const plugin = this.pluginManager.findPlugin(type);
            if (plugin) {
                return {
                    width: plugin.defaultWidth || this.DEFAULT_NODE_WIDTH,
                    height: plugin.defaultHeight || this.DEFAULT_NODE_HEIGHT
                };
            }
        }

        return {
            width: this.DEFAULT_NODE_WIDTH,
            height: this.DEFAULT_NODE_HEIGHT
        };
    }

    /**
     * Create basic node element (reusable)
     */
    private createBasicNodeElement(node: VisualNode): HTMLElement {
        const dimensions = this.getNodeDimensions(node);
        const nodeElement = document.createElement('div');
        nodeElement.className = 'visual-node connected-node';
        nodeElement.dataset.nodeId = node.id;
        nodeElement.style.width = `${dimensions.width}px`;
        nodeElement.style.minHeight = `${dimensions.height}px`;
        nodeElement.style.padding = '12px';
        nodeElement.style.background = 'white';
        nodeElement.style.border = '2px solid #FFC107';
        nodeElement.style.borderRadius = '6px';
        nodeElement.style.cursor = 'pointer';
        nodeElement.style.boxShadow = '0 2px 4px rgba(0,0,0,0.1)';

        // Apply role-based styling
        if (this.semanticValidator) {
            const roles = this.semanticValidator.getRoles(node.type);
            nodeElement.dataset.roles = roles.join(',');
            if (roles.length > 0) {
                nodeElement.dataset.primaryRole = roles[0];
                this.applyRoleBasedStyling(nodeElement, roles, node);
            }
        }

        // Apply validation error styling if node has errors
        const validationError = this.validationErrors.get(node.id);
        if (validationError) {
            this.applyValidationErrorStyling(nodeElement, validationError);
        }

        // Header with icon
        const header = document.createElement('div');
        header.style.cssText = 'display: flex; align-items: center; gap: 8px; font-weight: 600; margin-bottom: 8px; font-size: 14px;';

        // Icon from IconLibrary
        const iconConfig = this.iconLibrary.getIcon(node.type);
        const icon = this.iconLibrary.getIconElement(node.type, 16);
        header.appendChild(icon);

        // Type name
        const typeText = document.createElement('span');
        typeText.textContent = node.type;
        typeText.style.color = iconConfig.color;
        header.appendChild(typeText);

        nodeElement.appendChild(header);

        // Update border color based on category
        nodeElement.style.borderColor = iconConfig.color;

        // Attributes (max 3)
        const attrEntries = Object.entries(node.attributes).slice(0, 3);
        if (attrEntries.length > 0) {
            const attrs = document.createElement('div');
            attrs.style.fontSize = '12px';
            attrs.style.color = '#666';

            attrEntries.forEach(([key, value]) => {
                const attr = document.createElement('div');
                const displayValue = value.length > 30 ? value.substring(0, 30) + '...' : value;
                attr.textContent = `${key}: ${displayValue}`;
                attrs.appendChild(attr);
            });

            nodeElement.appendChild(attrs);
        }

        // Add ports to node
        this.portSystem.addPorts(nodeElement, node);
        // Setup port event handlers for connections
        this.setupPortHandlers(nodeElement, node);

        // Setup drag handlers for moving node
        this.setupNodeDragHandlers(nodeElement, node);

        // Role-based styling already applied above, skip duplicate

        // Apply accessibility metadata (Apple HIG compliance)
        if (this.pluginManager) {
            const plugin = this.pluginManager.findPlugin(node.type);
            if (plugin && plugin.accessibility) {
                nodeElement.setAttribute('role', 'button');
                nodeElement.setAttribute('aria-label', plugin.accessibility.ariaLabel || node.type);
                if (plugin.accessibility.ariaDescription) {
                    nodeElement.setAttribute('aria-description', plugin.accessibility.ariaDescription);
                    nodeElement.title = plugin.accessibility.ariaDescription; // Fallback
                }
                // Keyboard navigation hints are handled by keyboard event handlers
            } else {
                // Fallback accessibility
                nodeElement.setAttribute('role', 'button');
                nodeElement.setAttribute('aria-label', node.type);
            }
        } else {
            // Fallback accessibility
            nodeElement.setAttribute('role', 'button');
            nodeElement.setAttribute('aria-label', node.type);
        }

        nodeElement.onclick = (e) => {
            // Don't select if clicking on port
            if ((e.target as HTMLElement).closest('.port')) {
                return;
            }
            e.stopPropagation();
            this.selectNode(node.id);
        };

        return nodeElement;
    }

    private renderProcessMarkers(rootNodeElements: HTMLElement[]): void {
        if (this.processMarkersLayer) {
            this.processMarkersLayer.remove();
            this.processMarkersLayer = null;
        }

        const layer = document.createElement('div');
        layer.className = 'process-markers-layer';

        const positions = this.calculateProcessMarkerPositions(rootNodeElements);

        const startPosition = this.startMarkerManual && this.startMarkerPosition
            ? this.startMarkerPosition
            : { x: positions.startX, y: positions.startY };
        if (!this.startMarkerManual) {
            this.startMarkerPosition = startPosition;
        }

        const endPosition = this.endMarkerManual && this.endMarkerPosition
            ? this.endMarkerPosition
            : { x: positions.endX, y: positions.endY };
        if (!this.endMarkerManual) {
            this.endMarkerPosition = endPosition;
        }

        const startMarker = this.createProcessMarker('start', startPosition.x, startPosition.y, 'Start');
        const endMarker = this.createProcessMarker('end', endPosition.x, endPosition.y, 'End');

        layer.appendChild(startMarker);
        layer.appendChild(endMarker);

        this.canvasElement.appendChild(layer);
        this.processMarkersLayer = layer;

        startMarker.dataset.nodeId = 'workflow-start';
        endMarker.dataset.nodeId = 'workflow-end';

        this.startMarkerElement = startMarker;
        this.endMarkerElement = endMarker;

        this.setupMarkerDrag(startMarker, 'start');
        this.setupMarkerDrag(endMarker, 'end');
    }

    private clearProcessMarkers(): void {
        if (this.processMarkersLayer) {
            this.processMarkersLayer.remove();
            this.processMarkersLayer = null;
        }
        this.startMarkerElement = null;
        this.endMarkerElement = null;
    }

    private calculateProcessMarkerPositions(rootNodeElements: HTMLElement[]): { startX: number; startY: number; endX: number; endY: number } {
        if (rootNodeElements.length === 0) {
            const defaultCenterX = this.START_X + this.DEFAULT_NODE_WIDTH / 2;
            return {
                startX: defaultCenterX,
                startY: Math.max(this.START_Y - 100, 30),
                endX: defaultCenterX,
                endY: this.START_Y + this.DEFAULT_NODE_HEIGHT + 120
            };
        }

        let minLeft = Number.POSITIVE_INFINITY;
        let maxRight = Number.NEGATIVE_INFINITY;
        let minTop = Number.POSITIVE_INFINITY;
        let maxBottom = Number.NEGATIVE_INFINITY;

        rootNodeElements.forEach(element => {
            const left = Number.parseFloat(element.style.left || '') || element.offsetLeft || this.START_X;
            const top = Number.parseFloat(element.style.top || '') || element.offsetTop || this.START_Y;
            const width = element.offsetWidth || this.DEFAULT_NODE_WIDTH;
            const height = element.offsetHeight || this.DEFAULT_NODE_HEIGHT;

            minLeft = Math.min(minLeft, left);
            maxRight = Math.max(maxRight, left + width);
            minTop = Math.min(minTop, top);
            maxBottom = Math.max(maxBottom, top + height);
        });

        const centerX = minLeft + (maxRight - minLeft) / 2;

        return {
            startX: centerX,
            startY: Math.max(minTop - 100, 30),
            endX: centerX,
            endY: maxBottom + 120
        };
    }

    private createProcessMarker(type: 'start' | 'end', x: number, y: number, label: string): HTMLElement {
        const marker = document.createElement('div');
        marker.className = `process-marker process-marker-${type}`;
        marker.style.left = `${x}px`;
        marker.style.top = `${y}px`;

        if (type === 'start') {
            const icon = document.createElement('i');
            icon.className = this.workflowStatus === 'running' ? 'fas fa-stop' : 'fas fa-play';
            icon.setAttribute('aria-hidden', 'true');

            const text = document.createElement('span');
            text.textContent = this.workflowStatus === 'running' ? 'Stop' : label;
            text.style.fontSize = '10px';
            text.style.fontWeight = '700';

            marker.appendChild(icon);
            marker.appendChild(text);

            marker.style.pointerEvents = 'auto';
            marker.style.cursor = 'pointer';
            marker.title = this.workflowStatus === 'running'
                ? 'Stop workflow execution'
                : 'Start workflow execution';

            if (this.workflowStatus === 'running') {
                marker.classList.add('process-marker-start--running');
            }

            marker.addEventListener('click', (event) => {
                if (this.isMarkerDragging) {
                    this.isMarkerDragging = false;
                    return;
                }
                event.stopPropagation();
                if (this.workflowStatus === 'running') {
                    this.emit('workflowStopRequested', {});
                } else {
                    this.emit('workflowStartRequested', {});
                }
            });
        } else {
            const icon = document.createElement('i');
            icon.className = 'fas fa-flag-checkered';
            icon.setAttribute('aria-hidden', 'true');

            const text = document.createElement('span');
            text.textContent = label;
            text.style.fontSize = '10px';
            text.style.fontWeight = '700';

            marker.appendChild(icon);
            marker.appendChild(text);
            marker.title = 'Workflow end marker';
            marker.style.pointerEvents = 'auto';
            marker.style.cursor = 'grab';
        }

        return marker;
    }

    private setupMarkerDrag(marker: HTMLElement, type: 'start' | 'end'): void {
        const handleMouseDown = (event: MouseEvent) => {
            event.preventDefault();
            const initialCanvasPos = this.viewportToCanvas(event.clientX, event.clientY);
            const basePosition = type === 'start'
                ? (this.startMarkerPosition ?? { x: initialCanvasPos.x, y: initialCanvasPos.y })
                : (this.endMarkerPosition ?? { x: initialCanvasPos.x, y: initialCanvasPos.y });

            const offsetX = initialCanvasPos.x - basePosition.x;
            const offsetY = initialCanvasPos.y - basePosition.y;
            let dragged = false;

            const handleMouseMove = (moveEvent: MouseEvent) => {
                const canvasPos = this.viewportToCanvas(moveEvent.clientX, moveEvent.clientY);
                const deltaX = canvasPos.x - initialCanvasPos.x;
                const deltaY = canvasPos.y - initialCanvasPos.y;

                if (!dragged && (Math.abs(deltaX) > 2 || Math.abs(deltaY) > 2)) {
                    dragged = true;
                    this.isMarkerDragging = true;
                    marker.style.cursor = 'grabbing';
                    event.stopPropagation();
                }

                if (!dragged) {
                    return;
                }

                const newPosition = {
                    x: canvasPos.x - offsetX,
                    y: canvasPos.y - offsetY
                };

                marker.style.left = `${newPosition.x}px`;
                marker.style.top = `${newPosition.y}px`;

                if (type === 'start') {
                    this.startMarkerPosition = newPosition;
                    this.startMarkerManual = true;
                } else {
                    this.endMarkerPosition = newPosition;
                    this.endMarkerManual = true;
                }

                this.renderConnections();
            };

            const handleMouseUp = () => {
                document.removeEventListener('mousemove', handleMouseMove);
                document.removeEventListener('mouseup', handleMouseUp);
                document.body.style.userSelect = '';
                if (dragged) {
                    marker.style.cursor = type === 'start' ? 'pointer' : 'grab';
                    setTimeout(() => {
                        this.isMarkerDragging = false;
                    }, 0);
                }
            };

            document.addEventListener('mousemove', handleMouseMove);
            document.addEventListener('mouseup', handleMouseUp, { once: true });
            document.body.style.userSelect = 'none';
            event.stopPropagation();
        };

        marker.addEventListener('mousedown', handleMouseDown);
    }

    /**
     * Render connections (arrows)
     */
    private renderConnections(): void {
        if (!this.svgElement) return;

        // Remove all existing event listeners before clearing
        this.connectionLineElements.forEach((line, connectionId) => {
            // Clone to remove all event listeners
            const newLine = line.cloneNode(false) as SVGLineElement;
            line.parentNode?.replaceChild(newLine, line);
        });
        this.connectionLineElements.clear();

        // Clear existing connections from SVG (both lines and arrowheads)
        const existingLines = this.svgElement.querySelectorAll('.connection-line');
        const existingArrows = this.svgElement.querySelectorAll('.connection-arrow');
        existingLines.forEach(line => line.remove());
        existingArrows.forEach(arrow => arrow.remove());

        // Get all connections: automatic + manual
        const manualConnections = this.connectionManager.getConnections();

        // Convert automatic connections to compatible format
        const autoConnections = this.connections.map(conn => ({
            from: conn.from,
            to: conn.to,
            type: conn.type as 'sequential' | 'conditional' | 'error',
            id: undefined // Automatic connections don't have id
        }));

        const allConnections = [
            ...autoConnections,
            ...manualConnections // Manual connections (user-created) have id
        ];

        if (allConnections.length === 0) return;

        allConnections.forEach(connection => {
            try {
                const fromAnchor = this.getConnectionAnchor(connection.from);
                const toAnchor = this.getConnectionAnchor(connection.to);

                if (!fromAnchor || !toAnchor) {
                    console.warn('[VisualDesigner] Cannot render connection: anchor not found', connection);
                    return;
                }

                let x1 = fromAnchor.centerX;
                let y1 = fromAnchor.centerY;
                let x2 = toAnchor.centerX;
                let y2 = toAnchor.centerY;

                const verticalExitOffset = 14;
                const verticalEntryOffset = 14;

                const setDownstreamAnchor = () => {
                    x1 = fromAnchor.centerX;
                    y1 = fromAnchor.centerY + fromAnchor.height / 2 + verticalExitOffset;
                    if (fromAnchor.type === 'marker' && connection.from === 'workflow-end') {
                        y1 = fromAnchor.centerY - fromAnchor.height / 2 - verticalExitOffset;
                    }
                };

                const setUpstreamAnchor = () => {
                    x2 = toAnchor.centerX;
                    y2 = toAnchor.centerY - toAnchor.height / 2 - verticalEntryOffset;
                    if (toAnchor.type === 'marker' && connection.to === 'workflow-start') {
                        y2 = toAnchor.centerY + toAnchor.height / 2 + verticalEntryOffset;
                    }
                };

                if (connection.type === 'sequential') {
                    setDownstreamAnchor();
                    setUpstreamAnchor();
                } else {
                    if (fromAnchor.type === 'node') {
                        x1 = fromAnchor.centerX;
                        y1 = fromAnchor.centerY + fromAnchor.height / 2 + verticalExitOffset;
                    } else {
                        x1 = fromAnchor.centerX;
                        y1 = fromAnchor.centerY + fromAnchor.height / 2 + verticalExitOffset;
                        if (connection.from === 'workflow-end') {
                            y1 = fromAnchor.centerY - fromAnchor.height / 2 - verticalExitOffset;
                        }
                    }

                    if (toAnchor.type === 'node') {
                        x2 = toAnchor.centerX;
                        y2 = toAnchor.centerY - toAnchor.height / 2 - verticalEntryOffset;
                    } else {
                        x2 = toAnchor.centerX;
                        y2 = toAnchor.centerY - toAnchor.height / 2 - verticalEntryOffset;
                        if (connection.to === 'workflow-start') {
                            y2 = toAnchor.centerY + toAnchor.height / 2 + verticalEntryOffset;
                        }
                    }
                }


                const connectionId = (connection.id && typeof connection.id === 'string')
                    ? connection.id
                    : `auto-${connection.from}-${connection.to}`;
                const isSelected = this.selectedConnectionId === connectionId;
                const isManual = connectionId.startsWith('conn-');

                this.createArrow(x1, y1, x2, y2, connection.type, connectionId, isSelected, isManual);
            } catch (error) {
                console.error('[VisualDesigner] Error rendering connection:', error, connection);
            }
        });
    }

    /**
     * Find node recursively (including in nested children)
     */
    private findNodeRecursive(nodeId: string, nodes: VisualNode[]): VisualNode | null {
        for (const node of nodes) {
            if (node.id === nodeId) {
                return node;
            }
            if (node.children && node.children.length > 0) {
                const found = this.findNodeRecursive(nodeId, node.children);
                if (found) return found;
            }
        }
        return null;
    }

    /**
     * Create SVG arrow
     */
    private createArrow(
        x1: number,
        y1: number,
        x2: number,
        y2: number,
        type: 'sequential' | 'conditional' | 'error',
        connectionId?: string,
        isSelected?: boolean,
        isManual: boolean = false
    ): void {
        if (!this.svgElement) return;

        // Determine style
        let stroke = '#333';
        let strokeDasharray = 'none';
        let strokeWidth = '2';

        if (type === 'conditional') {
            stroke = '#4CAF50';
            strokeDasharray = '5,5';
        } else if (type === 'error') {
            stroke = '#F44336';
        }

        // Selected connection styling
        if (isSelected) {
            stroke = 'var(--primary-color, #3b82f6)';
            strokeWidth = '3';
        }

        // Create line
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('class', 'connection-line');
        if (connectionId) {
            line.setAttribute('data-connection-id', connectionId);
        }
        line.setAttribute('x1', x1.toString());
        line.setAttribute('y1', y1.toString());
        line.setAttribute('x2', x2.toString());
        line.setAttribute('y2', y2.toString());
        line.setAttribute('stroke', stroke);
        line.setAttribute('stroke-width', strokeWidth);
        line.setAttribute('stroke-dasharray', strokeDasharray);

        // Make line clickable and hoverable (only for manual connections)
        if (connectionId && isManual) {
            line.style.cursor = 'pointer';
            line.style.opacity = isSelected ? '1' : '0.7';
            line.style.pointerEvents = 'all';

            const handleMouseEnter = () => {
                if (!isSelected) {
                    line.style.opacity = '1';
                    line.setAttribute('stroke-width', (parseInt(strokeWidth) + 1).toString());
                }
            };

            const handleMouseLeave = () => {
                if (!isSelected) {
                    line.style.opacity = '0.7';
                    line.setAttribute('stroke-width', strokeWidth);
                }
            };

            const handleClick = (e: MouseEvent) => {
                e.stopPropagation();
                this.handleConnectionClick(connectionId);
            };

            const handleContextMenu = (e: MouseEvent) => {
                e.preventDefault();
                e.stopPropagation();
                this.showConnectionContextMenu(connectionId, { x: e.clientX, y: e.clientY });
            };

            line.addEventListener('mouseenter', handleMouseEnter);
            line.addEventListener('mouseleave', handleMouseLeave);
            line.addEventListener('click', handleClick);
            line.addEventListener('contextmenu', handleContextMenu);

            // Store reference for cleanup
            this.connectionLineElements.set(connectionId, line);
        } else {
            line.style.pointerEvents = 'none';
        }

        this.svgElement.appendChild(line);

        // Create arrowhead
        const arrowSize = 10;
        const angle = Math.atan2(y2 - y1, x2 - x1);
        const arrowX = x2 - Math.cos(angle) * 10;
        const arrowY = y2 - Math.sin(angle) * 10;

        const arrow = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
        const points = [
            [x2, y2],
            [arrowX - Math.cos(angle + Math.PI / 6) * arrowSize, arrowY - Math.sin(angle + Math.PI / 6) * arrowSize],
            [arrowX - Math.cos(angle - Math.PI / 6) * arrowSize, arrowY - Math.sin(angle - Math.PI / 6) * arrowSize]
        ];
        arrow.setAttribute('points', points.map(p => p.join(',')).join(' '));
        arrow.setAttribute('fill', stroke);

        // Add class for cleanup
        arrow.setAttribute('class', 'connection-arrow');
        if (connectionId) {
            arrow.setAttribute('data-connection-id', connectionId);
        }

        this.svgElement.appendChild(arrow);
    }

    // ========================================================================
    // Drag & Drop Support
    // ========================================================================

    /**
     * Setup drag & drop handlers
     */
    private setupDragAndDrop(): void {
        // Allow drop on canvas and containers
        this.canvasElement.ondragover = (e) => {
            e.preventDefault();

            // Get dragged plugin type from dataTransfer
            let draggedPluginType: string | null = null;
            try {
                const templateJSON = e.dataTransfer!.getData('application/json');
                if (templateJSON) {
                    const template = JSON.parse(templateJSON);
                    draggedPluginType = template.type;
                }
            } catch (err) {
                // Ignore parse errors
            }

            // Remove all existing drop zone classes
            this.canvasElement.querySelectorAll('.drop-zone-valid, .drop-zone-invalid, .drop-target').forEach(el => {
                el.classList.remove('drop-zone-valid', 'drop-zone-invalid', 'drop-target');
            });

            // Check if dragging over container
            const dropTarget = document.elementFromPoint(e.clientX, e.clientY);
            const containerElement = dropTarget?.closest('.container-children, .container-node');
            const nestedContainerElement = dropTarget?.closest('.visual-node[data-node-id]');

            // Check nested container first (higher priority)
            if (nestedContainerElement) {
                const nestedContainerId = nestedContainerElement.getAttribute('data-node-id');
                const nestedContainer = nestedContainerId ? this.findNode(nestedContainerId) : null;

                if (nestedContainer && isContainerNode(nestedContainer.type) && draggedPluginType && this.semanticValidator) {
                    const validation = this.semanticValidator.validateDrop(draggedPluginType, nestedContainer.type);
                    if (validation.valid) {
                        nestedContainerElement.classList.add('drop-zone-valid');
                        e.dataTransfer!.dropEffect = 'copy';
                    } else {
                        nestedContainerElement.classList.add('drop-zone-invalid');
                        e.dataTransfer!.dropEffect = 'none';
                    }
                    return;
                }
            }

            // Check root-level container
            if (containerElement) {
                const containerNodeElement = containerElement.closest('[data-node-id]');
                if (containerNodeElement) {
                    const containerId = containerNodeElement.getAttribute('data-node-id');
                    const container = containerId ? this.findNode(containerId) : null;

                    if (container && draggedPluginType && this.semanticValidator) {
                        const validation = this.semanticValidator.validateDrop(draggedPluginType, container.type);
                        if (validation.valid) {
                            containerNodeElement.classList.add('drop-zone-valid');
                            e.dataTransfer!.dropEffect = 'copy';
                        } else {
                            containerNodeElement.classList.add('drop-zone-invalid');
                            e.dataTransfer!.dropEffect = 'none';
                        }
                    } else {
                        // Fallback to old behavior if no semantic validator
                        e.dataTransfer!.dropEffect = 'copy';
                        containerNodeElement.classList.add('drop-target');
                    }
                }
            } else {
                // Check root-level drop
                if (draggedPluginType && this.semanticValidator) {
                    const validation = this.semanticValidator.validateDrop(draggedPluginType, null);
                    e.dataTransfer!.dropEffect = validation.valid ? 'copy' : 'none';
                } else {
                    e.dataTransfer!.dropEffect = 'copy';
                }
            }
        };

        // Remove drop target highlight on drag leave
        this.canvasElement.addEventListener('dragleave', (e) => {
            const dropTarget = document.elementFromPoint(e.clientX, e.clientY);
            if (!dropTarget || !this.canvasElement.contains(dropTarget)) {
                // Remove all drop zone classes
                this.canvasElement.querySelectorAll('.drop-target, .drop-zone-valid, .drop-zone-invalid').forEach(el => {
                    el.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');
                });
            }
        });

        // Handle drop
        this.canvasElement.ondrop = (e) => {
            e.preventDefault();

            try {
                const templateJSON = e.dataTransfer!.getData('application/json');
                if (!templateJSON) {
                    return;
                }

                const template = JSON.parse(templateJSON);

                // Get drop position in canvas coordinates (accounting for transform and scroll)
                const canvasPos = this.viewportToCanvas(e.clientX, e.clientY);

                // Get node dimensions from metadata
                const dimensions = this.getNodeDimensions(template.type);

                // Create new node from template
                const newNode: VisualNode = {
                    id: `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
                    type: template.type,
                    attributes: { ...template.defaultAttributes },
                    body: null,
                    children: [],
                    position: {
                        x: canvasPos.x - dimensions.width / 2, // Center on drop point
                        y: canvasPos.y - dimensions.height / 2
                    }
                };

                // Check if drop is over a nested container first
                const dropTarget = document.elementFromPoint(e.clientX, e.clientY);
                const nestedContainerElement = dropTarget?.closest('.visual-node[data-node-id]');

                if (nestedContainerElement) {
                    const nestedContainerId = nestedContainerElement.getAttribute('data-node-id');
                    const nestedContainer = nestedContainerId ? this.findNode(nestedContainerId) : null;

                    if (nestedContainer && isContainerNode(nestedContainer.type)) {
                        // Semantic validation before drop
                        if (this.semanticValidator) {
                            const validation = this.semanticValidator.validateDrop(newNode.type, nestedContainer.type);
                            if (!validation.valid) {
                                this.showError(validation.message);
                                console.warn(`❌ Validation failed: ${validation.message}`);
                                // Remove all drop zone classes
                                this.canvasElement.querySelectorAll('.drop-target, .drop-zone-valid, .drop-zone-invalid').forEach(el => {
                                    el.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');
                                });
                                return; // Reject the drop
                            }
                        }

                        // Drop into nested container - add as child

                        // Remove drop zone classes
                        nestedContainerElement.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');

                        // Position handled by container layout logic
                        newNode.position = undefined; // Let container rendering determine placement

                        this.addChildToContainer(nestedContainerId!, newNode);

                        // Remove all remaining drop zone classes
                        this.canvasElement.querySelectorAll('.drop-target, .drop-zone-valid, .drop-zone-invalid').forEach(el => {
                            el.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');
                        });
                        return; // Exit early
                    }
                }

                // Check if drop is over a root-level container node
                let containerElement = dropTarget?.closest('.container-children');

                // If not directly over children area, check if over container node itself
                if (!containerElement) {
                    containerElement = dropTarget?.closest('.container-node');
                }

                if (containerElement) {
                    // Find container node ID
                    const containerNodeElement = containerElement.closest('[data-node-id]');
                    const containerId = containerNodeElement?.getAttribute('data-node-id');

                    if (containerId && containerNodeElement) {
                        const container = this.findNode(containerId);
                        if (container && isContainerNode(container.type)) {
                            // Semantic validation before drop
                            if (this.semanticValidator) {
                                const validation = this.semanticValidator.validateDrop(newNode.type, container.type);
                                if (!validation.valid) {
                                    this.showError(validation.message);
                                    console.warn(`❌ Validation failed: ${validation.message}`);
                                    // Mark as invalid drop zone
                                    containerNodeElement.classList.add('drop-zone-invalid');
                                    containerNodeElement.classList.remove('drop-zone-valid');
                                    // Remove all drop zone classes after delay
                                    setTimeout(() => {
                                        this.canvasElement.querySelectorAll('.drop-target, .drop-zone-valid, .drop-zone-invalid').forEach(el => {
                                            el.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');
                                        });
                                    }, 2000);
                                    return; // Reject the drop
                                } else {
                                    // Mark as valid drop zone
                                    containerNodeElement.classList.add('drop-zone-valid');
                                    containerNodeElement.classList.remove('drop-zone-invalid');
                                }
                            }

                            // Drop into container - add as child

                            // Remove drop-target highlight
                            containerNodeElement.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');

                            // Position handled by container layout logic
                            newNode.position = undefined; // Let container rendering determine placement

                            this.addChildToContainer(containerId, newNode);

                            // Remove all remaining drop zone classes
                            this.canvasElement.querySelectorAll('.drop-target, .drop-zone-valid, .drop-zone-invalid').forEach(el => {
                                el.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');
                            });
                            return; // Exit early
                        }
                    }
                }

                // Remove all drop-target highlights
                this.canvasElement.querySelectorAll('.drop-target, .drop-zone-valid, .drop-zone-invalid').forEach(el => {
                    el.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');
                });

                // Drop on canvas - add to root level

                // Semantic validation before root-level drop
                if (this.semanticValidator) {
                    const validation = this.semanticValidator.validateDrop(newNode.type, null);
                    if (!validation.valid) {
                        this.showError(validation.message);
                        console.warn(`❌ Validation failed: ${validation.message}`);
                        return; // Reject the drop
                    }
                }

                this.addNode(newNode);
                this.emit('nodeAdded', newNode);

                console.log('✅ Added node from palette:', newNode.type);
            } catch (error) {
                console.error('Failed to create node from drop:', error);
            }
        };

        // Context menu (right-click)
        this.canvasElement.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            const target = e.target as HTMLElement;
            const nodeElement = target.closest('[data-node-id]') as HTMLElement;

            if (nodeElement) {
                const nodeId = nodeElement.dataset.nodeId!;
                this.showContextMenu(nodeId, { x: e.clientX, y: e.clientY });
            }
        });

        // Marquee selection (drag on empty canvas)
        this.setupMarqueeSelection();

        // Setup mouse wheel for zoom/pan
        this.canvasElement.addEventListener('wheel', (e) => this.handleMouseWheel(e), { passive: false });

        // Setup canvas-wide mouse move/up for connection drag
        this.canvasElement.addEventListener('mousemove', (e) => {
            if (this.connectionDragState?.temporaryLine) {
                // Convert viewport coordinates to canvas coordinates (SVG uses canvas coords)
                const canvasPos = this.viewportToCanvas(e.clientX, e.clientY);

                this.connectionDragState.temporaryLine.setAttribute('x2', canvasPos.x.toString());
                this.connectionDragState.temporaryLine.setAttribute('y2', canvasPos.y.toString());

                // Visual feedback for reconnect mode (orange line)
                if (this.connectionDragState.isReconnect) {
                    this.connectionDragState.temporaryLine.setAttribute('stroke', '#FF9800');
                    this.connectionDragState.temporaryLine.setAttribute('stroke-width', '3');
                } else {
                    this.connectionDragState.temporaryLine.setAttribute('stroke', 'var(--magenta)');
                    this.connectionDragState.temporaryLine.setAttribute('stroke-width', '2');
                }
            }
        });

        this.canvasElement.addEventListener('mouseup', () => {
            // Cancel connection drag if mouse released outside a port
            if (this.connectionDragState) {
                this.cancelConnectionDrag();
            }
        });

        // Click on canvas to deselect connections
        this.canvasElement.addEventListener('click', (e) => {
            // Only deselect if clicking directly on canvas wrapper (not on nodes or connections)
            const target = e.target as HTMLElement | SVGSVGElement;
            if (target === this.canvasElement ||
                (this.svgElement && target === this.svgElement) ||
                (target.tagName === 'div' && target.classList.contains('canvas-content-wrapper')) ||
                (target.tagName === 'svg' && target === this.svgElement)) {
                this.selectedConnectionId = null;
                this.selectedNodeId = null;
                this.render();
            }
        });
    }

    /**
     * Setup drag handlers for node movement
     */
    private setupNodeDragHandlers(nodeElement: HTMLElement, node: VisualNode): void {
        let isDragging = false;
        let startX = 0;
        let startY = 0;
        let offsetX = 0;
        let offsetY = 0;

        nodeElement.style.cursor = 'grab';

        nodeElement.addEventListener('mousedown', (e) => {
            // Don't start drag if clicking on port
            if ((e.target as HTMLElement).closest('.port')) {
                return;
            }

            // Don't start drag if clicking on container header
            if ((e.target as HTMLElement).closest('.container-header')) {
                return;
            }

            isDragging = true;
            this.isNodeDragging = true;
            // Calculate offset in canvas coordinates (accounting for transform)
            const canvasPos = this.viewportToCanvas(e.clientX, e.clientY);
            startX = canvasPos.x;
            startY = canvasPos.y;
            // Offset is the distance from mouse to node's current position (in canvas coords)
            offsetX = canvasPos.x - (node.position?.x || 0);
            offsetY = canvasPos.y - (node.position?.y || 0);

            nodeElement.style.cursor = 'grabbing';
            document.body.style.userSelect = 'none';
            e.preventDefault();
        });

        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;

            const canvasPos = this.viewportToCanvas(e.clientX, e.clientY);
            const newX = canvasPos.x - offsetX;
            const newY = canvasPos.y - offsetY;

            this.moveNode(node.id, { x: newX, y: newY }, { skipRender: true });
            this.rebuildConnections(false);
            this.renderConnections();
            this.updateProcessMarkersPosition();
            this.ensureGridOverlay();
        });

        document.addEventListener('mouseup', () => {
            if (isDragging) {
                isDragging = false;
                this.isNodeDragging = false;
                nodeElement.style.cursor = 'grab';
                document.body.style.userSelect = '';
                this.render();
            }
        });
    }

    /**
     * Setup port handlers for connection creation
     */
    private setupPortHandlers(nodeElement: HTMLElement, node: VisualNode): void {
        // Remove existing listeners to prevent duplicates
        const outputPorts = nodeElement.querySelectorAll('.port-output');

        // Clone and replace to remove all event listeners
        outputPorts.forEach(portElement => {
            const newPort = portElement.cloneNode(true);
            portElement.parentNode?.replaceChild(newPort, portElement);
        });

        // Re-query after clone
        const freshOutputPorts = nodeElement.querySelectorAll('.port-output');

        freshOutputPorts.forEach(portElement => {
            portElement.addEventListener('mousedown', (e) => {
                e.stopPropagation();

                const port = portElement as HTMLElement;
                const portType = port.dataset.portConnectionType || 'sequential';

                // Check if this port already has a connection (reconnect mode)
                const existingConnections = this.connectionManager.getConnectionsForNode(node.id, 'output');
                const existingConnection = existingConnections.find(conn => {
                    // Match by port type if connection has type info
                    return conn.type === portType || (portType === 'sequential' && !conn.type);
                });

                // Start connection drag (new or reconnect)
                this.connectionDragState = {
                    fromNodeId: node.id,
                    fromPortElement: port,
                    fromPortType: portType,
                    temporaryLine: null,
                    isReconnect: !!existingConnection,
                    existingConnectionId: existingConnection?.id || null,
                    reconnectEnd: existingConnection ? 'source' : null
                };

                // Visual feedback for reconnect mode
                if (existingConnection) {
                    port.style.background = '#FF9800'; // Orange for reconnect
                    port.style.borderColor = '#FF9800';
                }

                // Create temporary SVG line
                if (this.svgElement) {
                    const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                    // Calculate start position in canvas coordinates
                    const nodeRect = nodeElement.getBoundingClientRect();
                    const canvasPos = this.viewportToCanvas(nodeRect.left + nodeRect.width / 2, nodeRect.bottom);

                    line.setAttribute('x1', canvasPos.x.toString());
                    line.setAttribute('y1', canvasPos.y.toString());
                    line.setAttribute('x2', canvasPos.x.toString());
                    line.setAttribute('y2', canvasPos.y.toString());

                    // Color based on reconnect mode
                    if (this.connectionDragState.isReconnect) {
                        line.setAttribute('stroke', '#FF9800'); // Orange for reconnect
                        line.setAttribute('stroke-width', '3');
                    } else {
                        line.setAttribute('stroke', 'var(--magenta)');
                        line.setAttribute('stroke-width', '2');
                    }
                    line.setAttribute('stroke-dasharray', '5,5');
                    line.style.pointerEvents = 'none';

                    this.svgElement.appendChild(line);
                    this.connectionDragState.temporaryLine = line;
                }

            });
        });

        // Remove existing listeners for input ports too
        const inputPorts = nodeElement.querySelectorAll('.port-input');

        inputPorts.forEach(portElement => {
            const newPort = portElement.cloneNode(true);
            portElement.parentNode?.replaceChild(newPort, portElement);
        });

        const freshInputPorts = nodeElement.querySelectorAll('.port-input');

        freshInputPorts.forEach(portElement => {
            // Also handle mousedown for input ports (to reconnect source)
            portElement.addEventListener('mousedown', (e) => {
                e.stopPropagation();

                // Check if this input port already has a connection (reconnect source mode)
                const existingConnections = this.connectionManager.getConnectionsForNode(node.id, 'input');
                const existingConnection = existingConnections.find(conn => conn.to === node.id);

                if (existingConnection) {
                    // Start reconnect mode for source
                    const fromNode = this.findNode(existingConnection.from);
                    if (fromNode) {
                        const fromNodeElement = this.canvasElement.querySelector(`[data-node-id="${existingConnection.from}"]`) as HTMLElement;
                        const fromPortElement = fromNodeElement?.querySelector('.port-output') as HTMLElement;

                        if (fromPortElement) {
                            this.connectionDragState = {
                                fromNodeId: existingConnection.from,
                                fromPortElement: fromPortElement,
                                fromPortType: existingConnection.type || 'sequential',
                                temporaryLine: null,
                                isReconnect: true,
                                existingConnectionId: existingConnection.id,
                                reconnectEnd: 'target' // Reconnecting target (this input port)
                            };

                            // Visual feedback
                            const portEl = portElement as HTMLElement;
                            portEl.style.background = '#FF9800';
                            portEl.style.borderColor = '#FF9800';
                        }
                    }
                }
            });

            portElement.addEventListener('mouseup', (e) => {
                e.stopPropagation();

                if (!this.connectionDragState) {
                    return; // Not in connection drag mode
                }

                if (this.connectionDragState.fromNodeId === node.id) {
                    // Cannot connect to same node
                    this.cancelConnectionDrag();
                    return;
                }

                const connectionType = (this.connectionDragState.fromPortType as ConnectionType) || 'sequential';

                // Handle reconnect vs new connection
                if (this.connectionDragState.isReconnect && this.connectionDragState.existingConnectionId) {
                    // Reconnect existing connection - change target or source
                    const existingConn = this.connectionManager.getConnection(this.connectionDragState.existingConnectionId);
                    if (!existingConn) {
                        this.cancelConnectionDrag();
                        return;
                    }

                    const reconnectEnd = this.connectionDragState.reconnectEnd || 'target';

                    if (reconnectEnd === 'target') {
                        // Reconnect target: change toNodeId

                        // Step 1: Validate port type compatibility
                        const targetNodeElement = nodeElement;
                        const targetInputPort = this.portSystem.findPortElement(targetNodeElement, 'input');
                        const targetPortType = targetInputPort ? this.portSystem.getPortTypeFromElement(targetInputPort) : null;

                        const sourcePortType = (this.connectionDragState.fromPortType as PortType) || 'sequential';

                        const portValidation = this.portSystem.validatePortTypeCompatibility(
                            sourcePortType,
                            targetPortType
                        );

                        if (!portValidation.valid) {
                            this.showError(`Port compatibility error: ${portValidation.error}`);
                            this.cancelConnectionDrag();
                            return;
                        }

                        // Step 1.5: Validate semantic compatibility of ports
                        const semanticValidation = this.connectionManager.validateReconnectSemantics(
                            existingConn.from,
                            node.id,
                            sourcePortType,
                            targetPortType,
                            this.nodes,
                            (nodeId: string) => {
                                const foundNode = this.findNode(nodeId);
                                return foundNode?.type || null;
                            }
                        );

                        if (!semanticValidation.valid) {
                            this.showError(`Semantic validation failed: ${semanticValidation.error}`);
                            this.cancelConnectionDrag();
                            return;
                        }

                        // Step 1.6: Additional semantic checks using SemanticValidator
                        if (this.semanticValidator) {
                            const fromNode = this.findNode(existingConn.from);
                            const toNode = this.findNode(node.id);

                            if (fromNode && toNode) {
                                // Check if nodes are in parent-child relationship (should use nesting, not connections)
                                const areRelated = this.areNodesRelated(fromNode, toNode);
                                if (areRelated) {
                                    this.showError('Cannot connect nodes that are in a parent-child relationship. Use nesting instead.');
                                    this.cancelConnectionDrag();
                                    return;
                                }
                            }
                        }

                        // Step 2: Validate connection rules (no duplicates, no cycles)
                        const validation = this.connectionManager.validateConnection(
                            existingConn.from,
                            node.id,
                            connectionType,
                            this.connectionDragState.existingConnectionId
                        );
                        if (!validation.valid) {
                            this.showError(`Reconnection validation failed: ${validation.error}`);
                            this.cancelConnectionDrag();
                            return;
                        }

                        // Step 3: Update connection
                        this.pushHistorySnapshot();
                        this.connectionManager.updateConnection(this.connectionDragState.existingConnectionId, {
                            to: node.id,
                            type: connectionType
                        });
                        this.markDirty();
                    } else if (reconnectEnd === 'source') {
                        // Reconnect source: change fromNodeId

                        // Step 1: Validate port type compatibility
                        const sourceNodeElement = this.canvasElement.querySelector(`[data-node-id="${this.connectionDragState.fromNodeId}"]`) as HTMLElement;
                        const sourceOutputPort = sourceNodeElement ? this.portSystem.findPortElement(sourceNodeElement, 'output', connectionType as PortType) : null;
                        const sourcePortType = sourceOutputPort ? this.portSystem.getPortTypeFromElement(sourceOutputPort) : (connectionType as PortType);

                        const targetNodeElement = nodeElement;
                        const targetInputPort = this.portSystem.findPortElement(targetNodeElement, 'input');
                        const targetPortType = targetInputPort ? this.portSystem.getPortTypeFromElement(targetInputPort) : null;

                        const portValidation = this.portSystem.validatePortTypeCompatibility(
                            sourcePortType,
                            targetPortType
                        );

                        if (!portValidation.valid) {
                            this.showError(`Port compatibility error: ${portValidation.error}`);
                            this.cancelConnectionDrag();
                            return;
                        }

                        // Step 1.5: Validate semantic compatibility of ports
                        const semanticValidation = this.connectionManager.validateReconnectSemantics(
                            this.connectionDragState.fromNodeId,
                            existingConn.to,
                            sourcePortType,
                            targetPortType,
                            this.nodes,
                            (nodeId: string) => {
                                const foundNode = this.findNode(nodeId);
                                return foundNode?.type || null;
                            }
                        );

                        if (!semanticValidation.valid) {
                            this.showError(`Semantic validation failed: ${semanticValidation.error}`);
                            this.cancelConnectionDrag();
                            return;
                        }

                        // Step 1.6: Additional semantic checks using SemanticValidator
                        if (this.semanticValidator) {
                            const fromNode = this.findNode(this.connectionDragState.fromNodeId);
                            const toNode = this.findNode(existingConn.to);

                            if (fromNode && toNode) {
                                // Check if nodes are in parent-child relationship (should use nesting, not connections)
                                const areRelated = this.areNodesRelated(fromNode, toNode);
                                if (areRelated) {
                                    this.showError('Cannot connect nodes that are in a parent-child relationship. Use nesting instead.');
                                    this.cancelConnectionDrag();
                                    return;
                                }
                            }
                        }

                        // Step 2: Validate connection rules
                        const validation = this.connectionManager.validateConnection(
                            this.connectionDragState.fromNodeId,
                            existingConn.to,
                            connectionType,
                            this.connectionDragState.existingConnectionId
                        );
                        if (!validation.valid) {
                            this.showError(`Reconnection validation failed: ${validation.error}`);
                            this.cancelConnectionDrag();
                            return;
                        }

                        // Step 3: Update connection
                        this.pushHistorySnapshot();
                        this.connectionManager.updateConnection(this.connectionDragState.existingConnectionId, {
                            from: this.connectionDragState.fromNodeId,
                            type: connectionType
                        });
                        this.markDirty();
                    }

                    // Re-render to show updated connection
                    this.render();
                } else {
                    // Create new connection

                    // Step 1: Validate port type compatibility
                    const sourceNodeElement = this.canvasElement.querySelector(`[data-node-id="${this.connectionDragState.fromNodeId}"]`) as HTMLElement;
                    const sourceOutputPort = sourceNodeElement ? this.portSystem.findPortElement(sourceNodeElement, 'output', connectionType as PortType) : null;
                    const sourcePortType = sourceOutputPort ? this.portSystem.getPortTypeFromElement(sourceOutputPort) : (connectionType as PortType);

                    const targetNodeElement = nodeElement;
                    const targetInputPort = this.portSystem.findPortElement(targetNodeElement, 'input');
                    const targetPortType = targetInputPort ? this.portSystem.getPortTypeFromElement(targetInputPort) : null;

                    const portValidation = this.portSystem.validatePortTypeCompatibility(
                        sourcePortType,
                        targetPortType
                    );

                    if (!portValidation.valid) {
                        this.showError(`Port compatibility error: ${portValidation.error}`);
                        this.cancelConnectionDrag();
                        return;
                    }

                    // Step 2: Validate connection rules
                    const validation = this.connectionManager.validateConnection(
                        this.connectionDragState.fromNodeId,
                        node.id
                    );

                    if (!validation.valid) {
                        this.showError(`Connection validation failed: ${validation.error}`);
                        this.cancelConnectionDrag();
                        return;
                    }

                    try {
                        this.createManualConnection(
                            this.connectionDragState.fromNodeId,
                            node.id,
                            connectionType
                        );
                    } catch (error) {
                        this.showError(`Failed to create connection: ${error}`);
                    }
                }

                this.cancelConnectionDrag();
            });
        });
    }

    /**
     * Cancel connection drag
     */
    private cancelConnectionDrag(): void {
        if (this.connectionDragState?.temporaryLine) {
            this.connectionDragState.temporaryLine.remove();
        }

        // Reset port visual feedback
        if (this.connectionDragState?.fromPortElement) {
            this.connectionDragState.fromPortElement.style.background = '';
            this.connectionDragState.fromPortElement.style.borderColor = '';
        }

        this.connectionDragState = null;
    }

    /**
     * Setup marquee selection (drag rectangle)
     */
    private setupMarqueeSelection(): void {
        let isDragging = false;
        let startX = 0;
        let startY = 0;

        this.canvasElement.addEventListener('mousedown', (e) => {
            // Only start marquee if clicking on canvas (not on a node)
            const target = e.target as HTMLElement;
            if (target.closest('[data-node-id]')) {
                return; // Clicked on a node, let node handle it
            }

            // Start marquee selection
            isDragging = true;
            const rect = this.canvasElement.getBoundingClientRect();
            startX = e.clientX - rect.left;
            startY = e.clientY - rect.top;

            this.marqueeState = {
                startX,
                startY,
                endX: startX,
                endY: startY,
                isActive: true
            };

            // Create marquee rectangle
            this.createMarqueeElement();
            this.updateMarqueeElement(startX, startY, startX, startY);

            e.preventDefault();
        });

        this.canvasElement.addEventListener('mousemove', (e) => {
            if (!isDragging || !this.marqueeState) {
                return;
            }

            const rect = this.canvasElement.getBoundingClientRect();
            const currentX = e.clientX - rect.left;
            const currentY = e.clientY - rect.top;

            this.marqueeState.endX = currentX;
            this.marqueeState.endY = currentY;

            // Update visual rectangle
            this.updateMarqueeElement(
                this.marqueeState.startX,
                this.marqueeState.startY,
                currentX,
                currentY
            );

            // Update selection in real-time
            this.updateMarqueeSelection();
        });

        this.canvasElement.addEventListener('mouseup', () => {
            if (isDragging && this.marqueeState) {
                // Final selection
                this.finalizeMarqueeSelection();
                isDragging = false;
            }
        });
    }

    /**
     * Create marquee rectangle element
     */
    private createMarqueeElement(): void {
        // Remove existing
        if (this.marqueeElement) {
            this.marqueeElement.remove();
        }

        this.marqueeElement = document.createElement('div');
        this.marqueeElement.className = 'marquee-selection';
        this.marqueeElement.style.cssText = `
            position: absolute;
            border: 2px dashed var(--magenta, #D81B60);
            background: rgba(216, 27, 96, 0.1);
            pointer-events: none;
            z-index: 1000;
            display: none;
        `;

        this.canvasElement.appendChild(this.marqueeElement);
    }

    /**
     * Update marquee rectangle position
     */
    private updateMarqueeElement(startX: number, startY: number, endX: number, endY: number): void {
        if (!this.marqueeElement) {
            return;
        }

        const left = Math.min(startX, endX);
        const top = Math.min(startY, endY);
        const width = Math.abs(endX - startX);
        const height = Math.abs(endY - startY);

        this.marqueeElement.style.left = `${left}px`;
        this.marqueeElement.style.top = `${top}px`;
        this.marqueeElement.style.width = `${width}px`;
        this.marqueeElement.style.height = `${height}px`;
        this.marqueeElement.style.display = width > 5 && height > 5 ? 'block' : 'none';
    }

    /**
     * Update selection while dragging
     */
    private updateMarqueeSelection(): void {
        if (!this.marqueeState) {
            return;
        }

        const rect = {
            x: Math.min(this.marqueeState.startX, this.marqueeState.endX),
            y: Math.min(this.marqueeState.startY, this.marqueeState.endY),
            width: Math.abs(this.marqueeState.endX - this.marqueeState.startX),
            height: Math.abs(this.marqueeState.endY - this.marqueeState.startY)
        };

        // Select nodes in rectangle
        this.selectNodesInRectangle(rect);
    }

    /**
     * Finalize marquee selection
     */
    private finalizeMarqueeSelection(): void {
        if (!this.marqueeState || !this.marqueeElement) {
            return;
        }

        // Hide marquee
        this.marqueeElement.style.display = 'none';

        // Final selection already done by updateMarqueeSelection()
        this.marqueeState = null;

        // Visual feedback for selected nodes
        this.highlightSelectedNodes();
    }

    showContextMenu(nodeId: string, position?: { x: number; y: number }): ContextMenuModel {
        const items = this.buildContextMenuItems(nodeId);
        const element = position ? this.renderContextMenu(position.x, position.y, items) : undefined;
        return { items, element };
    }

    private isDividerItem(item: ContextMenuItem): item is { type: 'divider' } {
        return typeof item === 'object' && 'type' in item && (item as { type?: string }).type === 'divider';
    }

    private buildContextMenuItems(nodeId: string): ContextMenuItem[] {
        const clipboardAvailable = this.clipboard.length > 0;
        const node = this.findNodeRecursive(nodeId, this.nodes);
        const isRootNode = node !== null && this.nodes.some(n => n.id === nodeId);
        const rootIndex = isRootNode ? this.nodes.findIndex(n => n.id === nodeId) : -1;
        const canMoveUp = isRootNode && rootIndex > 0;
        const canMoveDown = isRootNode && rootIndex >= 0 && rootIndex < this.nodes.length - 1;

        const items: ContextMenuItem[] = [
            {
                label: 'Edit Properties',
                icon: 'fas fa-edit',
                action: () => {
                    this.selectNode(nodeId);
                    this.emit('nodeSelected', nodeId);
                }
            }
        ];

        if (node && isContainerNode(node.type)) {
            items.push({
                label: 'Focus on this container',
                icon: 'fas fa-crop-alt',
                action: () => this.enterContainer(node.id)
            });
            items.push({ type: 'divider' });
        }

        if (node && this.detailRegistry) {
            const detailSections = this.detailRegistry.getSections(node.type);
            if (detailSections.length > 0) {
                detailSections.forEach(section => {
                    items.push({
                        label: section.addLabel,
                        icon: 'fas fa-plus',
                        action: () => this.addDetailNode(node.id, section.detailType)
                    });
                });
                items.push({ type: 'divider' });
            }
        }

        // Add reordering options for root nodes
        if (isRootNode) {
            items.push(
                {
                    label: 'Move Up',
                    icon: 'fas fa-arrow-up',
                    disabled: !canMoveUp,
                    action: () => {
                        if (canMoveUp) {
                            this.reorderRootNode(nodeId, 'up');
                        }
                    }
                },
                {
                    label: 'Move Down',
                    icon: 'fas fa-arrow-down',
                    disabled: !canMoveDown,
                    action: () => {
                        if (canMoveDown) {
                            this.reorderRootNode(nodeId, 'down');
                        }
                    }
                }
            );
            if (items.length > 0 && !this.isDividerItem(items[items.length - 1])) {
                items.push({ type: 'divider' });
            }
        }

        items.push(
            {
                label: 'Delete',
                icon: 'fas fa-trash',
                danger: true,
                action: () => {
                    this.removeNode(nodeId);
                }
            },
            { type: 'divider' },
            {
                label: 'Duplicate',
                icon: 'fas fa-copy',
                action: () => {
                    this.selectNode(nodeId);
                    this.duplicateSelectedNodes();
                }
            },
            {
                label: 'Copy',
                icon: 'fas fa-clone',
                action: () => {
                    this.copyNodes([nodeId]);
                }
            },
            {
                label: 'Paste',
                icon: 'fas fa-paste',
                disabled: !clipboardAvailable,
                action: () => {
                    if (clipboardAvailable) {
                        this.pasteClipboard();
                    }
                }
            }
        );

        return items;
    }

    /**
     * Handle connection click
     */
    private handleConnectionClick(connectionId: string): void {
        this.selectedConnectionId = connectionId;
        this.selectedNodeId = null; // Deselect nodes when selecting connection
        this.render();
        this.emit('connectionSelected', connectionId);
    }

    /**
     * Show context menu for connection
     */
    private showConnectionContextMenu(connectionId: string, position: { x: number; y: number }): void {
        const connection = this.connectionManager.getConnection(connectionId);
        if (!connection) return;

        const items: ContextMenuItem[] = [
            {
                label: 'Change Source',
                icon: 'fas fa-arrow-left',
                action: () => {
                    this.reconnectConnectionSource(connectionId);
                }
            },
            {
                label: 'Change Target',
                icon: 'fas fa-arrow-right',
                action: () => {
                    this.reconnectConnectionTarget(connectionId);
                }
            },
            {
                label: 'Swap Direction',
                icon: 'fas fa-exchange-alt',
                action: () => {
                    this.swapConnectionDirection(connectionId);
                }
            },
            { type: 'divider' },
            {
                label: 'Edit Connection',
                icon: 'fas fa-edit',
                action: () => {
                    this.editConnection(connectionId);
                }
            },
            {
                label: 'Delete Connection',
                icon: 'fas fa-trash',
                danger: true,
                action: () => {
                    this.deleteConnection(connectionId);
                }
            }
        ];

        const menu = this.renderContextMenu(position.x, position.y, items);
        this.activeContextMenu = menu;
    }

    /**
     * Delete connection (public API)
     */
    deleteConnection(connectionId: string): boolean {
        if (!this.connectionManager.getConnection(connectionId)) {
            return false;
        }

        this.pushHistorySnapshot();
        const deleted = this.deleteConnectionInternal(connectionId);
        if (deleted) {
            if (this.selectedConnectionId === connectionId) {
                this.selectedConnectionId = null;
            }
            this.markDirty();
            this.render();
            return true;
        }

        this.undoStack.pop();
        return false;
    }

    /**
     * Reconnect connection source (change fromNodeId)
     */
    private reconnectConnectionSource(connectionId: string): void {
        const connection = this.connectionManager.getConnection(connectionId);
        if (!connection) return;

        // Start connection drag from current source to allow reconnecting
        this.startConnectionDrag(connection.from, 'output');
        this.connectionDragState = {
            ...this.connectionDragState!,
            isReconnect: true,
            existingConnectionId: connectionId,
            reconnectEnd: 'source'
        };

        // Show message to user
        this.showError('Drag from a different output port to change source');
    }

    /**
     * Reconnect connection target (change toNodeId)
     */
    private reconnectConnectionTarget(connectionId: string): void {
        const connection = this.connectionManager.getConnection(connectionId);
        if (!connection) return;

        // Start connection drag from current source
        this.startConnectionDrag(connection.from, 'output');
        this.connectionDragState = {
            ...this.connectionDragState!,
            isReconnect: true,
            existingConnectionId: connectionId,
            reconnectEnd: 'target'
        };

        // Show message to user
        this.showError('Drop on a different input port to change target');
    }

    /**
     * Swap connection direction (swap from and to)
     */
    private swapConnectionDirection(connectionId: string): void {
        const connection = this.connectionManager.getConnection(connectionId);
        if (!connection) return;

        // Get node elements
        const fromNodeElement = this.canvasElement.querySelector(`[data-node-id="${connection.to}"]`) as HTMLElement;
        const toNodeElement = this.canvasElement.querySelector(`[data-node-id="${connection.from}"]`) as HTMLElement;

        if (!fromNodeElement || !toNodeElement) {
            this.showError('Cannot swap: node elements not found');
            return;
        }

        // Step 1: Validate port type compatibility (swapped direction)
        const newSourceOutputPort = this.portSystem.findPortElement(fromNodeElement, 'output', connection.type);
        const newSourcePortType = newSourceOutputPort ? this.portSystem.getPortTypeFromElement(newSourceOutputPort) : connection.type as PortType;

        const newTargetInputPort = this.portSystem.findPortElement(toNodeElement, 'input');
        const newTargetPortType = newTargetInputPort ? this.portSystem.getPortTypeFromElement(newTargetInputPort) : null;

        const portValidation = this.portSystem.validatePortTypeCompatibility(
            newSourcePortType,
            newTargetPortType
        );

        if (!portValidation.valid) {
            this.showError(`Cannot swap direction: ${portValidation.error}`);
            return;
        }

        // Step 2: Validate swapped connection (no duplicates, no cycles)
        const validation = this.connectionManager.validateConnection(
            connection.to,
            connection.from,
            connection.type,
            connectionId // Exclude this connection from duplicate check
        );
        if (!validation.valid) {
            this.showError(`Cannot swap direction: ${validation.error}`);
            return;
        }

        // Step 3: Update connection with swapped from/to
        this.pushHistorySnapshot();
        this.connectionManager.updateConnection(connectionId, {
            from: connection.to,
            to: connection.from
        });
        this.markDirty();
        this.render();
    }

    /**
     * Edit connection (redefine source/target/type)
     */
    private editConnection(connectionId: string): void {
        const connection = this.connectionManager.getConnection(connectionId);
        if (!connection) return;

        // For now, show a simple prompt-based editor
        // TODO: Replace with proper modal dialog
        const newFromId = prompt(`Edit connection ${connectionId}\n\nSource node ID:`, connection.from);
        if (newFromId === null) return;

        const newToId = prompt('Target node ID:', connection.to);
        if (newToId === null) return;

        // Validate changes
        if (newFromId === connection.from && newToId === connection.to) {
            return; // No changes
        }

        const validation = this.connectionManager.validateConnection(newFromId, newToId);
        if (!validation.valid) {
            alert(`Invalid connection: ${validation.error}`);
            return;
        }

        // Update connection
        const updated = this.connectionManager.updateConnection(connectionId, {
            from: newFromId,
            to: newToId
        });

        if (updated) {
            this.pushHistorySnapshot();
            this.markDirty();
            this.render();
        }
    }

    /**
     * Reorder root node (move up or down in root nodes array)
     */
    reorderRootNode(nodeId: string, direction: 'up' | 'down'): boolean {
        const index = this.nodes.findIndex(n => n.id === nodeId);
        if (index === -1) {
            console.warn(`[VisualDesigner] Cannot reorder: node ${nodeId} not found in root nodes`);
            return false;
        }

        const newIndex = direction === 'up' ? index - 1 : index + 1;
        if (newIndex < 0 || newIndex >= this.nodes.length) {
            console.warn(`[VisualDesigner] Cannot reorder: invalid new index ${newIndex}`);
            return false;
        }

        this.pushHistorySnapshot();

        // Swap nodes
        const node = this.nodes[index];
        this.nodes[index] = this.nodes[newIndex];
        this.nodes[newIndex] = node;

        // Update positions to reflect new order (simple vertical layout)
        const spacing = 150;
        this.nodes.forEach((n, i) => {
            if (n.position) {
                n.position.y = this.START_Y + i * spacing;
            }
        });

        this.markDirty();
        this.render();
        return true;
    }

    private renderContextMenu(x: number, y: number, items: ContextMenuItem[]): HTMLElement {
        if (this.activeContextMenu) {
            this.activeContextMenu.remove();
            this.activeContextMenu = null;
        }
        if (this.activeContextMenuHandler) {
            document.removeEventListener('click', this.activeContextMenuHandler);
            this.activeContextMenuHandler = null;
        }

        // Use viewport coordinates directly for fixed positioning
        // Menu should be positioned relative to viewport, not canvas (which may be scrolled/transformed)
        const menuX = x;
        const menuY = y;

        const menu = document.createElement('div');
        menu.className = 'node-context-menu';
        menu.style.cssText = `
            position: fixed;
            top: ${menuY}px;
            left: ${menuX}px;
            background: white;
            border: 1px solid var(--gray-300);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-lg);
            z-index: 10000;
            min-width: 180px;
            padding: 4px 0;
        `;

        items.forEach(item => {
            if ('type' in item && item.type === 'divider') {
                const divider = document.createElement('div');
                divider.style.cssText = 'height: 1px; background: var(--gray-200); margin: 4px 0;';
                menu.appendChild(divider);
                return;
            }

            const actionItem = item as ContextMenuActionItem;
            const button = document.createElement('button');
            button.type = 'button';
            button.style.cssText = `
                outline: none;
                border: none;
                background: none;
                padding: 8px 16px;
                width: 100%;
                text-align: left;
                cursor: ${actionItem.disabled ? 'not-allowed' : 'pointer'};
                font-size: 13px;
                color: ${actionItem.danger ? 'var(--magenta)' : 'var(--text-primary)'};
                display: flex;
                align-items: center;
                gap: 8px;
                transition: background 0.2s;
                opacity: ${actionItem.disabled ? 0.6 : 1};
            `;
            button.innerHTML = `${actionItem.icon ? `<i class="${actionItem.icon}"></i>` : ''} ${actionItem.label}`;
            button.disabled = Boolean(actionItem.disabled);
            if (!actionItem.disabled) {
                button.onmouseenter = () => button.style.background = 'var(--gray-100)';
                button.onmouseleave = () => button.style.background = 'none';
                button.onclick = () => {
                    actionItem.action?.();
                    document.removeEventListener('click', closeMenu);
                    menu.remove();
                    this.activeContextMenu = null;
                    this.activeContextMenuHandler = null;
                };
            }
            menu.appendChild(button);
        });

        // Append to canvas element instead of body for proper positioning
        // Append to body for fixed positioning (relative to viewport)
        document.body.appendChild(menu);
        this.activeContextMenu = menu;

        const closeMenu = (event: MouseEvent) => {
            if (!menu.contains(event.target as Node)) {
                menu.remove();
                this.activeContextMenu = null;
                document.removeEventListener('click', closeMenu);
                this.activeContextMenuHandler = null;
            }
        };
        this.activeContextMenuHandler = closeMenu;
        setTimeout(() => document.addEventListener('click', closeMenu), 0);

        return menu;
    }

    /**
     * Add child node to container
     */
    addChildToContainer(containerId: string, childNode: VisualNode): boolean {
        const container = this.findNode(containerId);
        if (!container) {
            console.warn(`[VisualDesigner] Container not found: ${containerId}`);
            return false;
        }

        if (!isContainerNode(container.type)) {
            console.warn(`[VisualDesigner] Node ${containerId} is not a container`);
            return false;
        }

        // Semantic validation before adding
        if (this.semanticValidator) {
            const validation = this.semanticValidator.validateDrop(childNode.type, container.type);
            if (!validation.valid) {
                this.showError(validation.message);
                console.warn(`❌ Cannot add ${childNode.type} to ${container.type}: ${validation.message}`);
                return false;
            }
        }

        this.pushHistorySnapshot();

        // Add to children array
        if (!container.children) {
            container.children = [];
        }
        const newChild = this.cloneNode(childNode);
        container.children.push(newChild);

        // Mark dirty and re-render
        this.markDirty();
        this.render();

        // Emit event
        this.emit('nodeAdded', newChild);

        console.log(`✅ Added child ${newChild.type} to container ${container.type}`);
        return true;
    }

    /**
     * Reorder child within container
     */
    reorderChild(containerId: string, fromIndex: number, toIndex: number): boolean {
        const container = this.findNode(containerId);
        if (!container) {
            console.warn(`[VisualDesigner] Container not found: ${containerId}`);
            return false;
        }

        if (!container.children || container.children.length === 0) {
            return false;
        }

        // Validate indices
        if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0 ||
            fromIndex >= container.children.length || toIndex >= container.children.length) {
            return false;
        }

        this.pushHistorySnapshot();

        // Reorder array
        const child = container.children[fromIndex];
        container.children.splice(fromIndex, 1);
        container.children.splice(toIndex, 0, child);

        // Mark dirty and re-render
        this.markDirty();
        this.render();

        // Emit event
        this.emit('childReordered', { containerId, fromIndex, toIndex });

        console.log(`✅ Reordered child in container ${container.type}`);
        return true;
    }

    /**
     * Move child up one position
     */
    moveChildUp(containerId: string, childId: string): boolean {
        const container = this.findNode(containerId);
        if (!container || !container.children || container.children.length === 0) {
            return false;
        }

        const childIndex = container.children.findIndex(c => c.id === childId);
        if (childIndex === -1 || childIndex === 0) {
            return false; // Not found or already at top
        }

        return this.reorderChild(containerId, childIndex, childIndex - 1);
    }

    /**
     * Move child down one position
     */
    moveChildDown(containerId: string, childId: string): boolean {
        const container = this.findNode(containerId);
        if (!container || !container.children || container.children.length === 0) {
            return false;
        }

        const childIndex = container.children.findIndex(c => c.id === childId);
        if (childIndex === -1 || childIndex === container.children.length - 1) {
            return false; // Not found or already at bottom
        }

        return this.reorderChild(containerId, childIndex, childIndex + 1);
    }

    /**
     * Add reorder buttons to child node element
     */
    private addReorderButtons(element: HTMLElement, containerId: string, childId: string, index: number, totalChildren: number): void {
        // Create button container
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'child-reorder-buttons';
        buttonContainer.style.cssText = `
            position: absolute;
            bottom: 8px;
            right: 8px;
            display: flex;
            gap: 4px;
            z-index: 10;
        `;

        // Up button
        const upButton = document.createElement('button');
        upButton.className = 'reorder-button reorder-button-up';
        upButton.innerHTML = '⬆️';
        upButton.title = 'Move up';
        upButton.disabled = index === 0;
        upButton.style.cssText = `
            width: 24px;
            height: 24px;
            padding: 0;
            border: none;
            background: rgba(255, 255, 255, 0.9);
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: ${index === 0 ? 0.3 : 1};
        `;

        upButton.onclick = (e) => {
            e.stopPropagation();
            this.moveChildUp(containerId, childId);
        };

        upButton.onmouseenter = () => {
            if (!upButton.disabled) {
                upButton.style.background = 'rgba(255, 255, 255, 1)';
            }
        };

        upButton.onmouseleave = () => {
            upButton.style.background = 'rgba(255, 255, 255, 0.9)';
        };

        // Down button
        const downButton = document.createElement('button');
        downButton.className = 'reorder-button reorder-button-down';
        downButton.innerHTML = '⬇️';
        downButton.title = 'Move down';
        downButton.disabled = index === totalChildren - 1;
        downButton.style.cssText = `
            width: 24px;
            height: 24px;
            padding: 0;
            border: none;
            background: rgba(255, 255, 255, 0.9);
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: ${index === totalChildren - 1 ? 0.3 : 1};
        `;

        downButton.onclick = (e) => {
            e.stopPropagation();
            this.moveChildDown(containerId, childId);
        };

        downButton.onmouseenter = () => {
            if (!downButton.disabled) {
                downButton.style.background = 'rgba(255, 255, 255, 1)';
            }
        };

        downButton.onmouseleave = () => {
            downButton.style.background = 'rgba(255, 255, 255, 0.9)';
        };

        buttonContainer.appendChild(upButton);
        buttonContainer.appendChild(downButton);

        // Make parent element relative for absolute positioning
        if (getComputedStyle(element).position === 'static') {
            element.style.position = 'relative';
        }

        element.appendChild(buttonContainer);
    }

    /**
     * Add node and re-render
     */
    addNodeAndRender(node: VisualNode): void {
        this.addNode(node);
        this.render();
    }

    /**
     * Remove node and re-render
     */
    removeNodeAndRender(nodeId: string): boolean {
        const removed = this.removeNode(nodeId);
        if (removed) {
            this.render();
        }
        return removed;
    }

    /**
     * Update node and re-render
     */
    updateNodeAndRender(nodeId: string, updatedNode: VisualNode): boolean {
        const updated = this.updateNode(nodeId, updatedNode);
        if (updated) {
            this.render();
        }
        return updated;
    }

    // ========================================================================
    // Drag & Move Nodes
    // ========================================================================

    /**
     * Move node to new position
     */
    moveNode(nodeId: string, position: { x: number; y: number }, options?: { skipRender?: boolean }): boolean {
        const node = this.findNodeRecursive(nodeId, this.nodes);
        if (!node) {
            return false;
        }

        // Initialize position if missing
        if (!node.position) {
            node.position = { x: 0, y: 0 };
        }

        // Apply grid snap if enabled
        let x = position.x;
        let y = position.y;

        if (this.gridSnapEnabled) {
            x = Math.round(x / this.gridSize) * this.gridSize;
            y = Math.round(y / this.gridSize) * this.gridSize;
        }

        // Boundary check (keep nodes visible)
        x = Math.max(0, x);
        y = Math.max(0, y);

        // Calculate offset from previous position
        const oldX = node.position.x;
        const oldY = node.position.y;
        const offsetX = x - oldX;
        const offsetY = y - oldY;

        // Update position
        node.position = { x, y };
        node.isManualPositioned = true;

        // If node has children, move them too maintaining relative positions
        if (node.children && node.children.length > 0 && (offsetX !== 0 || offsetY !== 0)) {
            this.moveNodeChildren(node, offsetX, offsetY);
        }

        if (options?.skipRender) {
            const element = this.canvasElement.querySelector(`[data-node-id="${node.id}"]`) as HTMLElement | null;
            if (element && element.style.position === 'absolute') {
                element.style.left = `${x}px`;
                element.style.top = `${y}px`;
            }
            this.highlightSelectedNodes();
        } else {
            this.render();
        }

        return true;
    }

    /**
     * Move node children recursively maintaining relative positions
     */
    private moveNodeChildren(node: VisualNode, offsetX: number, offsetY: number): void {
        if (!node.children) return;

        node.children.forEach(child => {
            // Initialize position if missing
            if (!child.position) {
                child.position = { x: 0, y: 0 };
            }

            child.position.x += offsetX;
            child.position.y += offsetY;

            // Recursively move grandchildren
            if (child.children && child.children.length > 0) {
                this.moveNodeChildren(child, offsetX, offsetY);
            }
        });
    }

    /**
     * Handle node drag start
     */
    handleNodeDragStart(nodeId: string, mousePos: { x: number; y: number }): {
        nodeId: string;
        offsetX: number;
        offsetY: number;
    } | null {
        const node = this.findNode(nodeId);
        if (!node || !node.position) {
            return null;
        }

        const offsetX = mousePos.x - node.position.x;
        const offsetY = mousePos.y - node.position.y;

        this.dragState = {
            nodeId,
            offsetX,
            offsetY,
            initialX: node.position.x,
            initialY: node.position.y
        };

        return { nodeId, offsetX, offsetY };
    }

    /**
     * Handle node drag move
     */
    handleNodeDragMove(mousePos: { x: number; y: number }, dragInfo: any): void {
        if (!dragInfo) {
            return;
        }

        const newX = mousePos.x - dragInfo.offsetX;
        const newY = mousePos.y - dragInfo.offsetY;

        this.moveNode(dragInfo.nodeId, { x: newX, y: newY }, { skipRender: true });
        this.rebuildConnections(false);
        this.renderConnections();
        this.updateProcessMarkersPosition();
        this.ensureGridOverlay();
    }

    /**
     * Set grid snap
     */
    setGridSnap(enabled: boolean, gridSize?: number): void {
        this.gridSnapEnabled = enabled;
        if (gridSize) {
            this.gridSize = gridSize;
            this.gridSystem = new GridSystem(gridSize);
        }

        // Show/hide grid overlay
        if (enabled) {
            this.gridSystem.render(this.canvasElement);
        } else {
            const existing = this.canvasElement.querySelector('.grid-overlay');
            if (existing) {
                existing.remove();
            }
        }
    }

    /**
     * Get selected node IDs
     */
    getSelectedNodeIds(): string[] {
        return [...this.selectedNodeIds];
    }

    /**
     * Select node (with multi-select support)
     */
    selectNodeMulti(nodeId: string, options?: { addToSelection?: boolean }): void {
        this.selectNode(nodeId, options);
    }

    /**
     * Deselect all nodes
     */
    deselectAll(): void {
        this.clearSelection();
    }

    /**
     * Select all nodes
     */
    selectAll(): void {
        this.selectedNodeIds = this.getAllNodes().map(n => n.id);
        this.selectedNodeId = null;
        this.highlightSelectedNodes();
    }

    /**
     * Delete selected nodes
     */
    deleteSelectedNodes(): void {
        if (this.selectedNodeIds.length === 0) {
            return;
        }

        const idsToRemove = [...this.selectedNodeIds];

        this.pushHistorySnapshot();

        const affectedIds = new Set<string>();
        idsToRemove.forEach(id => {
            this.getNodeAndDescendantIdsById(id).forEach(affected => affectedIds.add(affected));
        });

        idsToRemove.forEach(nodeId => {
            this.removeNodeInternal(nodeId, this.nodes);
        });

        affectedIds.forEach(id => this.removeConnectionsForNode(id));

        this.selectedNodeIds = this.selectedNodeIds.filter(id => !affectedIds.has(id));
        if (this.selectedNodeId && affectedIds.has(this.selectedNodeId)) {
            this.selectedNodeId = null;
        }

        this.markDirty();
        this.render();
        this.highlightSelectedNodes();

        affectedIds.forEach(id => this.emit('nodeRemoved', id));
    }

    // ========================================================================
    // Zoom & Pan
    // ========================================================================

    /**
     * Get current zoom level
     */
    getZoom(): number {
        return this.zoom;
    }

    /**
     * Set zoom level
     */
    setZoom(newZoom: number): void {
        this.zoom = Math.max(this.MIN_ZOOM, Math.min(this.MAX_ZOOM, newZoom));
        this.applyTransform();
    }

    /**
     * Zoom in
     */
    zoomIn(): void {
        this.setZoom(this.zoom * 1.2);
    }

    /**
     * Zoom out
     */
    zoomOut(): void {
        this.setZoom(this.zoom / 1.2);
    }

    /**
     * Zoom to specific level at mouse position
     */
    zoomAt(mousePos: { x: number; y: number }, newZoom: number): void {
        const oldZoom = this.zoom;
        this.setZoom(newZoom);

        // Adjust pan to keep mouse position fixed
        const zoomFactor = this.zoom / oldZoom;
        this.panOffset.x = mousePos.x - (mousePos.x - this.panOffset.x) * zoomFactor;
        this.panOffset.y = mousePos.y - (mousePos.y - this.panOffset.y) * zoomFactor;

        this.applyTransform();
    }

    /**
     * Get current pan offset
     */
    getPan(): { x: number; y: number } {
        return { ...this.panOffset };
    }

    /**
     * Pan canvas by delta
     */
    panBy(delta: { x: number; y: number }): void {
        this.panOffset.x += delta.x;
        this.panOffset.y += delta.y;
        this.applyTransform();
    }

    /**
     * Set absolute pan offset
     */
    pan(offset: { x: number; y: number }): void {
        this.panOffset = { x: offset.x, y: offset.y };
        this.applyTransform();
    }

    /**
     * Reset view (zoom 100%, pan to 0,0)
     */
    resetView(): void {
        this.zoom = 1.0;
        this.panOffset = { x: 0, y: 0 };
        this.applyTransform();
    }

    /**
     * Convert viewport coordinates to canvas coordinates (accounting for transform)
     */
    private viewportToCanvas(x: number, y: number): { x: number; y: number } {
        // Get the scrollable container (canvas-area), not the transformed wrapper
        const container = this.container; // This is #canvas-area
        const containerRect = container.getBoundingClientRect();

        // Calculate position relative to container (accounting for scroll)
        const relativeX = x - containerRect.left + container.scrollLeft;
        const relativeY = y - containerRect.top + container.scrollTop;

        // Account for transform (inverse transform)
        const canvasX = (relativeX - this.panOffset.x) / this.zoom;
        const canvasY = (relativeY - this.panOffset.y) / this.zoom;

        return { x: canvasX, y: canvasY };
    }

    /**
     * Apply zoom + pan transform
     */
    private applyTransform(): void {
        // Apply transform to canvas content wrapper (not the scrollable container)
        const transform = `translate(${this.panOffset.x}px, ${this.panOffset.y}px) scale(${this.zoom})`;
        this.canvasElement.style.transform = transform;
        this.canvasElement.style.transformOrigin = '0 0';

        // Update SVG overlay size if it exists to match zoom level
        if (this.svgElement && this.container) {
            const containerRect = this.container.getBoundingClientRect();
            if (containerRect.width > 0 && containerRect.height > 0) {
                // SVG should cover the container size (before zoom)
                this.svgElement.setAttribute('width', containerRect.width.toString());
                this.svgElement.setAttribute('height', containerRect.height.toString());
            }
        }
    }

    /**
     * Handle mouse wheel (zoom + pan)
     */
    handleMouseWheel(event: WheelEvent): void {
        if (event.ctrlKey || event.metaKey) {
            // Zoom with Cmd/Ctrl + wheel
            event.preventDefault();
            const delta = event.deltaY > 0 ? 0.9 : 1.1;
            this.zoomAt({ x: event.clientX, y: event.clientY }, this.zoom * delta);
        } else {
            // Pan with wheel
            this.panBy({ x: -event.deltaX * 0.5, y: -event.deltaY * 0.5 });
        }
    }

    // ========================================================================
    // Keyboard Shortcuts (Apple HIG)
    // ========================================================================

    /**
     * Handle keyboard events
     */
    handleKeyboard(event: { key: string; metaKey?: boolean; shiftKey?: boolean; ctrlKey?: boolean }): void {
        const isMac = typeof navigator !== 'undefined' ? navigator.platform.toUpperCase().indexOf('MAC') >= 0 : true;
        const cmdKey = Boolean(event.metaKey) || (!isMac && Boolean(event.ctrlKey));

        // Cmd+A - Select All
        if (event.key === 'a' && cmdKey) {
            this.selectedNodeIds = this.getAllNodes().map(n => n.id);
            this.selectedNodeId = null;
            this.highlightSelectedNodes();
            return;
        }

        // Delete/Backspace - Delete selected
        if (event.key === 'Delete' || event.key === 'Backspace') {
            this.deleteSelectedNodes();
            return;
        }

        // Cmd+C - Copy
        if (event.key === 'c' && cmdKey) {
            this.copyNodes(this.selectedNodeIds);
            return;
        }

        // Cmd+V - Paste
        if (event.key === 'v' && cmdKey) {
            this.pasteClipboard();
            return;
        }

        // Cmd+D - Duplicate
        if (event.key === 'd' && cmdKey) {
            this.duplicateSelectedNodes();
            return;
        }

        // Cmd+Z - Undo
        if (event.key === 'z' && cmdKey && !event.shiftKey) {
            this.undo();
            return;
        }

        // Cmd+Shift+Z - Redo
        if (event.key === 'z' && cmdKey && event.shiftKey) {
            this.redo();
            return;
        }

        // Cmd+0 - Reset zoom
        if (event.key === '0' && cmdKey) {
            this.resetView();
            return;
        }

        // Cmd++ - Zoom in
        if (event.key === '+' && cmdKey || event.key === '=' && cmdKey) {
            this.zoomIn();
            return;
        }

        // Cmd+- - Zoom out
        if (event.key === '-' && cmdKey) {
            this.zoomOut();
            return;
        }
    }

    /**
     * Duplicate selected nodes
     */
    private duplicateSelectedNodes(): void {
        if (this.selectedNodeIds.length === 0) {
            return;
        }

        const nodesToDuplicate = this.selectedNodeIds
            .map(id => this.findNode(id))
            .filter((node): node is VisualNode => Boolean(node));

        if (nodesToDuplicate.length === 0) {
            return;
        }

        this.pushHistorySnapshot();

        const duplicates = nodesToDuplicate.map(node => this.createNodeWithNewIds(node));

        this.nodes.push(...duplicates);
        this.selectedNodeIds = duplicates.map(node => node.id);

        this.markDirty();
        this.render();
        this.highlightSelectedNodes();
    }

    /**
     * Undo last action
     */
    private undo(): void {
        if (this.undoStack.length === 0) {
            console.warn('[VisualDesigner] Nothing to undo');
            return;
        }

        const snapshot = this.undoStack.pop()!;
        this.redoStack.push(this.createSnapshot());
        this.restoreSnapshot(snapshot);
    }

    /**
     * Redo last undone action
     */
    private redo(): void {
        if (this.redoStack.length === 0) {
            console.warn('[VisualDesigner] Nothing to redo');
            return;
        }

        const snapshot = this.redoStack.pop()!;
        this.undoStack.push(this.createSnapshot());
        this.restoreSnapshot(snapshot);
    }

    // ========================================================================
    // Selection Rectangle (Marquee)
    // ========================================================================

    /**
     * Select nodes in rectangle
     */
    selectNodesInRectangle(rect: { x: number; y: number; width: number; height: number }): string[] {
        const selected: string[] = [];

        this.nodes.forEach(node => {
            if (!node.position) {
                return;
            }

            // Check if node is inside rectangle
            const dimensions = this.getNodeDimensions(node);
            const nodeRight = node.position.x + dimensions.width;
            const nodeBottom = node.position.y + dimensions.height;
            const rectRight = rect.x + rect.width;
            const rectBottom = rect.y + rect.height;

            const isInside =
                node.position.x < rectRight &&
                nodeRight > rect.x &&
                node.position.y < rectBottom &&
                nodeBottom > rect.y;

            if (isInside) {
                selected.push(node.id);
            }
        });

        this.selectedNodeIds = selected;
        this.highlightSelectedNodes();

        return selected;
    }

    /**
     * Highlight selected nodes visually
     */
    private highlightSelectedNodes(): void {
        // Clear all highlights
        const allNodes = this.canvasElement.querySelectorAll('[data-node-id]');
        allNodes.forEach((nodeEl) => {
            (nodeEl as HTMLElement).classList.remove('node-selected');
        });

        // Hide all resize handles
        this.resizeHandler.hideAllHandles();

        // Highlight selected nodes and show resize handles
        this.selectedNodeIds.forEach(nodeId => {
            const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
            if (nodeElement) {
                nodeElement.classList.add('node-selected');

                // Add resize handles for selected node
                const node = this.findNode(nodeId);
                if (node) {
                    this.resizeHandler.addResizeHandles(nodeElement, node);

                    // Set constraints based on metadata
                    if (this.pluginManager) {
                        const plugin = this.pluginManager.findPlugin(node.type);
                        if (plugin) {
                            const minWidth = Math.max(80, (plugin.defaultWidth || this.DEFAULT_NODE_WIDTH) * 0.5);
                            const minHeight = Math.max(60, (plugin.defaultHeight || this.DEFAULT_NODE_HEIGHT) * 0.5);
                            const maxWidth = (plugin.defaultWidth || this.DEFAULT_NODE_WIDTH) * 3;
                            const maxHeight = (plugin.defaultHeight || this.DEFAULT_NODE_HEIGHT) * 3;

                            this.resizeHandler.setConstraints(nodeId, {
                                minWidth,
                                minHeight,
                                maxWidth,
                                maxHeight
                            });
                        }
                    }
                }
            }
        });

        // Show handles for selected nodes
        this.resizeHandler.showHandlesForNodes(this.selectedNodeIds);
    }

    // ========================================================================
    // History & Clipboard Helpers
    // ========================================================================

    private cloneNode(node: VisualNode): VisualNode {
        return {
            ...node,
            attributes: { ...node.attributes },
            body: node.body,
            position: node.position ? { ...node.position } : undefined,
            children: node.children ? this.cloneNodes(node.children) : []
        };
    }

    private cloneNodes(nodes: VisualNode[]): VisualNode[] {
        return nodes.map(node => this.cloneNode(node));
    }

    private createNodeWithNewIds(node: VisualNode): VisualNode {
        const cloned = this.cloneNode(node);
        const newNode: VisualNode = {
            ...cloned,
            id: this.generateNodeId(node.type),
            children: cloned.children ? cloned.children.map(child => this.createNodeWithNewIds(child)) : []
        };

        if (newNode.position) {
            newNode.position = { ...newNode.position, x: newNode.position.x + 30, y: newNode.position.y + 30 };
        }

        return newNode;
    }

    private collectNodeAndDescendantIds(node: VisualNode, acc: string[] = []): string[] {
        acc.push(node.id);
        if (node.children && node.children.length > 0) {
            node.children.forEach(child => this.collectNodeAndDescendantIds(child, acc));
        }
        return acc;
    }

    private getNodeAndDescendantIdsById(nodeId: string): string[] {
        const node = this.findNode(nodeId);
        if (!node) {
            return [];
        }
        return this.collectNodeAndDescendantIds(node, []);
    }

    /**
     * Check if two nodes are in a parent-child relationship
     * (one is an ancestor of the other)
     */
    private areNodesRelated(node1: VisualNode, node2: VisualNode): boolean {
        // Check if node2 is a descendant of node1 (node1 contains node2 in its subtree)
        if (this.isNodeInSubtree(node1, node2.id)) {
            return true;
        }

        // Check if node1 is a descendant of node2 (node2 contains node1 in its subtree)
        if (this.isNodeInSubtree(node2, node1.id)) {
            return true;
        }

        return false;
    }

    /**
     * Check if a node with given ID exists in the subtree of the given node
     * (recursive search through children)
     */
    private isNodeInSubtree(node: VisualNode, targetId: string): boolean {
        // Check children recursively
        if (node.children && node.children.length > 0) {
            for (const child of node.children) {
                if (child.id === targetId) {
                    return true;
                }
                // Recursively check subtree
                if (this.isNodeInSubtree(child, targetId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private generateNodeId(type: string = 'node'): string {
        return `${type}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    private createSnapshot(): DesignerSnapshot {
        const connections = this.connectionManager.getConnections().map(conn => ({ ...conn }));
        return {
            nodes: this.cloneNodes(this.nodes),
            connections,
            pan: { ...this.panOffset },
            zoom: this.zoom
        };
    }

    private restoreSnapshot(snapshot: DesignerSnapshot): void {
        this.nodes = this.cloneNodes(snapshot.nodes);

        this.connectionManager.clear();
        const payload = { connections: snapshot.connections.map(conn => ({ ...conn })) };
        this.connectionManager.importConnections(JSON.stringify(payload));

        this.panOffset = { ...snapshot.pan };
        this.zoom = snapshot.zoom;
        this.selectedNodeIds = [];
        this.selectedNodeId = null;

        this.render();
        this.applyTransform();
    }

    private pushHistorySnapshot(): void {
        const snapshot = this.createSnapshot();
        this.undoStack.push(snapshot);
        if (this.undoStack.length > 50) {
            this.undoStack.shift();
        }
        this.redoStack = [];
    }

    private clearHistory(): void {
        this.undoStack = [];
        this.redoStack = [];
    }

    private copyNodes(nodeIds: string[]): void {
        const copied = nodeIds
            .map(id => this.findNode(id))
            .filter((node): node is VisualNode => Boolean(node))
            .map(node => this.cloneNode(node));
        this.clipboard = this.cloneNodes(copied);
    }

    private pasteClipboard(offset: { x: number; y: number } = { x: 40, y: 40 }): void {
        if (this.clipboard.length === 0) {
            return;
        }

        this.pushHistorySnapshot();

        const clones = this.clipboard.map((node, index) => {
            const duplicated = this.createNodeWithNewIds(node);
            if (duplicated.position) {
                duplicated.position = {
                    x: duplicated.position.x + offset.x + index * 10,
                    y: duplicated.position.y + offset.y + index * 10
                };
            }
            return duplicated;
        });

        this.nodes.push(...clones);
        this.selectedNodeIds = clones.map(node => node.id);
        this.render();
        this.markDirty();
        this.highlightSelectedNodes();
    }

    // ========================================================================
    // Auto-Layout
    // ========================================================================

    /**
     * Auto-arrange nodes in specified layout
     */
    autoLayout(layoutType: 'grid' | 'vertical' | 'horizontal'): void {
        const nodeCount = this.nodes.length;
        if (nodeCount === 0) {
            return;
        }

        switch (layoutType) {
            case 'grid':
                this.layoutGrid();
                break;
            case 'vertical':
                this.layoutVertical();
                break;
            case 'horizontal':
                this.layoutHorizontal();
                break;
        }

        this.render();
    }

    private layoutGrid(): void {
        const cols = Math.ceil(Math.sqrt(this.nodes.length));
        const spacing = 300;

        this.nodes.forEach((node, index) => {
            const col = index % cols;
            const row = Math.floor(index / cols);

            node.position = {
                x: this.START_X + col * spacing,
                y: this.START_Y + row * spacing
            };
        });
    }

    private layoutVertical(): void {
        const spacing = 150;

        this.nodes.forEach((node, index) => {
            node.position = {
                x: this.START_X + 100,
                y: this.START_Y + index * spacing
            };
        });
    }

    private layoutHorizontal(): void {
        const spacing = 300;

        this.nodes.forEach((node, index) => {
            node.position = {
                x: this.START_X + index * spacing,
                y: this.START_Y + 100
            };
        });
    }

    // ========================================================================
    // Context Menu
    // ========================================================================

    // ========================================================================
    // Manual Connections
    // ========================================================================

    /**
     * Create manual connection
     */
    createManualConnection(fromId: string, toId: string, type: ConnectionType = 'sequential'): ManualConnection {
        const validation = this.connectionManager.validateConnection(fromId, toId);

        if (!validation.valid) {
            const message = validation.error || 'Invalid connection';
            if (message.toLowerCase().includes('cycle')) {
                throw new Error('Connection would create a cycle');
            }
            throw new Error(message);
        }

        this.pushHistorySnapshot();
        const connection = this.connectionManager.createConnection(fromId, toId, type);
        this.markDirty();
        this.render();
        return connection;
    }

    /**
     * Get manual connections
     */
    getManualConnections(): ManualConnection[] {
        return this.connectionManager.getConnections().map(conn => ({ ...conn }));
    }

    private deleteConnectionInternal(connectionId: string): boolean {
        return this.connectionManager.deleteConnection(connectionId);
    }

    startConnectionDrag(fromNodeId: string, portDirection: 'output' | 'conditional' | 'error' = 'output'): void {
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${fromNodeId}"]`) as HTMLElement | null;
        if (!nodeElement) {
            throw new Error(`Node not found: ${fromNodeId}`);
        }

        let selector = '.port-output';
        if (portDirection !== 'output') {
            selector = `.port-${portDirection}`;
        }

        const portElement = nodeElement.querySelector(selector) as HTMLElement | null;
        if (!portElement) {
            throw new Error(`Port ${portDirection} not found on node ${fromNodeId}`);
        }

        // Check if this port already has a connection (reconnect mode)
        const existingConnections = this.connectionManager.getConnectionsForNode(fromNodeId, 'output');
        const existingConnection = existingConnections.find(conn => {
            const portType = portElement.dataset.portConnectionType || 'sequential';
            return conn.type === portType || (portType === 'sequential' && !conn.type);
        });

        this.connectionDragState = {
            fromNodeId,
            fromPortElement: portElement,
            fromPortType: portElement.dataset.portConnectionType || 'sequential',
            temporaryLine: null,
            isReconnect: !!existingConnection,
            existingConnectionId: existingConnection?.id || null,
            reconnectEnd: existingConnection ? 'source' : null
        };
    }

    endConnectionDrag(toNodeId: string, _direction: 'input' | 'output' = 'input'): ManualConnection | null {
        if (!this.connectionDragState) {
            return null;
        }

        const { fromNodeId, fromPortType } = this.connectionDragState;

        try {
            const connection = this.createManualConnection(fromNodeId, toNodeId, (fromPortType as ConnectionType) || 'sequential');
            return connection;
        } catch (error) {
            console.warn('Failed to create connection via drag:', error);
            return null;
        } finally {
            this.cancelConnectionDrag();
        }
    }

    /**
     * Check if connection would create cycle
     */
    private wouldCreateCycle(fromId: string, toId: string): boolean {
        return this.connectionManager.wouldCreateCycle(fromId, toId);
    }

    /**
     * Get execution order based on manual connections
     */
    getExecutionOrder(): string[] {
        return this.connectionManager.getExecutionOrder(this.nodes);
    }

    // ========================================================================
    // Minimap
    // ========================================================================

    private minimapVisible: boolean = false;

    /**
     * Show/hide minimap
     */
    showMinimap(visible: boolean): void {
        this.minimapVisible = visible;
        if (visible) {
            this.minimap.render(this.canvasElement, this.nodes);
            this.minimap.onNavigate((position) => {
                this.handleMinimapClick(position);
            });
        } else {
            const existing = this.canvasElement.querySelector('.minimap');
            if (existing) {
                existing.remove();
            }
        }
    }

    /**
     * Check if minimap is visible
     */
    isMinimapVisible(): boolean {
        return this.minimapVisible;
    }

    /**
     * Handle minimap click (navigate canvas)
     */
    handleMinimapClick(position: { x: number; y: number }): void {
        // Position is already converted by Minimap component
        // Center canvas view on clicked position
        const canvasRect = this.canvasElement.getBoundingClientRect();
        this.panOffset = {
            x: -position.x + canvasRect.width / 2,
            y: -position.y + canvasRect.height / 2
        };
        this.applyTransform();
    }

    // ========================================================================
    // Single Configuration Mode (Conductor-inspired)
    // ========================================================================

    /**
     * Check if configuration has unsaved changes
     */
    isDirty(): boolean {
        return this.isDirtyFlag;
    }

    /**
     * Mark configuration as clean (saved)
     */
    markClean(): void {
        this.isDirtyFlag = false;
        console.log('✅ [VisualDesigner] Configuration marked as clean');
    }

    /**
     * Mark configuration as dirty (modified)
     */
    markDirty(): void {
        this.isDirtyFlag = true;
        console.log('⚠️ [VisualDesigner] Configuration marked as dirty');
    }

    /**
     * Get current configuration (nodes + metadata)
     */
    getCurrentConfiguration(): {
        nodes: VisualNode[];
        connections: ManualConnection[];
        filename: string | null;
        isDirty: boolean;
    } | null {
        if (this.nodes.length === 0 && !this.currentFilename) {
            return null;
        }

        const exportNodes = this.prepareNodesForExport();
        const exportConnections = this.connectionManager.getConnections().map(conn => ({ ...conn }));

        return {
            nodes: exportNodes,
            connections: exportConnections,
            filename: this.currentFilename,
            isDirty: this.isDirtyFlag
        };
    }

    /**
     * Set current filename
     */
    setCurrentFilename(filename: string | null): void {
        this.currentFilename = filename;
        console.log('📄 [VisualDesigner] Current filename:', filename);
    }

    /**
     * Get current filename
     */
    getCurrentFilename(): string | null {
        return this.currentFilename;
    }

    /**
     * Check if should prompt before loading new configuration
     */
    shouldPromptBeforeLoad(): boolean {
        return this.isDirtyFlag;
    }

    /**
     * Show unsaved changes dialog
     */
    showUnsavedChangesDialog(): {
        title: string;
        message: string;
        buttons: Array<{ label: string; action: string; style: string }>;
    } {
        return {
            title: 'Unsaved Changes',
            message: 'You have unsaved changes in the current configuration. What would you like to do?',
            buttons: [
                { label: 'Save', action: 'save', style: 'primary' },
                { label: 'Discard', action: 'discard', style: 'warning' },
                { label: 'Cancel', action: 'cancel', style: 'secondary' }
            ]
        };
    }

    /**
     * Load nodes with prompt if dirty
     */
    async loadNodesWithPrompt(newNodes: VisualNode[]): Promise<boolean> {
        // If clean, load directly
        if (!this.isDirtyFlag) {
            this.loadNodes(newNodes);
            this.markClean();
            return true;
        }

        // Show dialog
        const dialog = this.showUnsavedChangesDialog();
        const action = await this.showDialogModal(dialog);

        switch (action) {
            case 'save':
                // Save current configuration first
                await this.saveConfiguration();
                this.loadNodes(newNodes);
                this.markClean();
                return true;

            case 'discard':
                // Discard changes, load new
                this.loadNodes(newNodes);
                this.markClean();
                return true;

            case 'cancel':
                // Cancel load
                console.log('ℹ️ [VisualDesigner] Load cancelled by user');
                return false;

            default:
                return false;
        }
    }

    /**
     * Show dialog modal (non-blocking version)
     */
    private async showDialogModal(dialog: any): Promise<string> {
        // Non-blocking implementation using HTML dialog
        return new Promise((resolve) => {
            const modal = document.createElement('div');
            modal.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                z-index: 10000;
                display: flex;
                align-items: center;
                justify-content: center;
            `;

            const content = document.createElement('div');
            content.style.cssText = `
                background: white;
                border-radius: var(--radius-lg, 8px);
                padding: 24px;
                max-width: 500px;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            `;

            content.innerHTML = `
                <h3 style="margin: 0 0 16px 0; color: var(--magenta, #D81B60);">${dialog.title}</h3>
                <p style="margin: 0 0 24px 0;">${dialog.message}</p>
                <div style="display: flex; gap: 12px; justify-content: flex-end;">
                    ${dialog.buttons.map((btn: any) => `
                        <button data-action="${btn.action}" style="
                            padding: 8px 16px;
                            background: ${btn.style === 'primary' ? 'var(--magenta, #D81B60)' : 'var(--gray-200, #ccc)'};
                            color: ${btn.style === 'primary' ? 'white' : 'black'};
                            border: none;
                            border-radius: var(--radius-md, 4px);
                            cursor: pointer;
                            font-weight: 600;
                        ">${btn.label}</button>
                    `).join('')}
                </div>
            `;

            modal.appendChild(content);
            document.body.appendChild(modal);

            // Handle button clicks
            content.querySelectorAll('button').forEach(btn => {
                btn.addEventListener('click', () => {
                    const action = (btn as HTMLElement).dataset.action || 'cancel';
                    modal.remove();
                    resolve(action);
                });
            });

            // Close on overlay click
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    modal.remove();
                    resolve('cancel');
                }
            });
        });
    }

    /**
     * Save configuration (to be implemented by App.ts)
     */
    private async saveConfiguration(): Promise<boolean> {
        // This will be delegated to App.ts
        console.log('💾 [VisualDesigner] Saving configuration...');
        const config = this.getCurrentConfiguration();
        const xml = this.generateXML();
        if (config) {
            this.emit('saveRequested', { ...config, xml });
        } else {
            this.emit('saveRequested', { xml });
        }
        return true;
    }

    async requestSave(): Promise<boolean> {
        return this.saveConfiguration();
    }

    /**
     * Create new empty configuration
     */
    newConfiguration(): void {
        this.nodes = [];
        this.connections = [];
        this.connectionManager.clear();
        this.currentFilename = null;
        this.isDirtyFlag = false;
        this.render();
        console.log('✨ [VisualDesigner] New configuration created');
    }

    /**
     * Check if has tabs (NO - Visual mode = single config)
     */
    hasTabs(): boolean {
        return false; // Visual mode doesn't use tabs
    }

    /**
     * Check if should prompt before mode switch
     */
    shouldPromptBeforeModeSwitch(): boolean {
        return this.isDirtyFlag;
    }

    /**
     * Handle browser beforeunload event
     */
    handleBeforeUnload(event: BeforeUnloadEvent): boolean {
        if (this.isDirtyFlag) {
            event.preventDefault();
            event.returnValue = 'You have unsaved changes. Are you sure you want to leave?';
            return true;
        }
        return false;
    }

    /**
     * Enable/disable auto-save
     */
    enableAutoSave(enabled: boolean, intervalMs: number = 30000): void {
        this.autoSaveEnabled = enabled;

        if (this.autoSaveInterval) {
            clearInterval(this.autoSaveInterval);
            this.autoSaveInterval = null;
        }

        if (enabled) {
            this.autoSaveInterval = window.setInterval(() => {
                if (this.isDirtyFlag) {
                    this.autoSaveToLocalStorage();
                }
            }, intervalMs);

            console.log(`✅ [VisualDesigner] Auto-save enabled (every ${intervalMs}ms)`);
        } else {
            console.log('ℹ️ [VisualDesigner] Auto-save disabled');
        }
    }

    /**
     * Auto-save to localStorage
     */
    private autoSaveToLocalStorage(): void {
        try {
            const snapshot = {
                nodes: this.nodes,
                connections: this.connectionManager.getConnections(),
                filename: this.currentFilename,
                timestamp: Date.now()
            };

            localStorage.setItem('webharvest-autosave', JSON.stringify(snapshot));
            console.log('💾 [VisualDesigner] Auto-saved to localStorage');
        } catch (error) {
            console.error('❌ [VisualDesigner] Auto-save failed:', error);
        }
    }

    /**
     * Restore from auto-save
     */
    restoreFromAutoSave(): boolean {
        try {
            const saved = localStorage.getItem('webharvest-autosave');
            if (!saved) {
                return false;
            }

            const snapshot = JSON.parse(saved);

            this.nodes = snapshot.nodes || [];
            this.connectionManager.clear();
            if (snapshot.connections) {
                this.connectionManager.importConnections(JSON.stringify({ connections: snapshot.connections }));
            }
            this.currentFilename = snapshot.filename || null;

            this.render();

            console.log('✅ [VisualDesigner] Restored from auto-save');
            return true;
        } catch (error) {
            console.error('❌ [VisualDesigner] Failed to restore from auto-save:', error);
            return false;
        }
    }

    /**
     * Clear auto-save
     */
    clearAutoSave(): void {
        localStorage.removeItem('webharvest-autosave');
        console.log('🗑️ [VisualDesigner] Auto-save cleared');
    }

    /**
     * Generate XML from current nodes
     * 
     * This method respects reconnected connections - it uses the current state
     * from ConnectionManager, which includes all source/target changes made
     * via reconnect operations (drag ports or context menu).
     */
    generateXML(): string {
        const generator = new XMLGenerator();
        // prepareNodesForExport() already applies connection-based ordering
        // It uses current connections from ConnectionManager, which includes
        // all reconnected (source/target changed) connections
        const exportNodes = this.prepareNodesForExport();
        const body = generator.generate(exportNodes, 1);
        const hasContent = body.trim().length > 0;

        const lines: string[] = [];
        lines.push('<?xml version="1.0" encoding="UTF-8"?>');
        lines.push('<config xmlns="http://org.webharvest/schema/2.1/core">');
        if (hasContent) {
            lines.push(body);
        }
        lines.push('</config>');

        return lines.join('\n');
    }

    /**
     * Prepare nodes for export with connection-based ordering
     * 
     * This method:
     * 1. Gets current connections from ConnectionManager (includes reconnected connections)
     * 2. Clones nodes to avoid mutating originals
     * 3. Orders nodes based on connection topology (respects reconnected source/target)
     * 
     * Reconnected connections (changed source/target) are automatically respected
     * because ConnectionManager.updateConnection() updates the from/to fields,
     * and getConnections() returns the updated state.
     */
    private prepareNodesForExport(): VisualNode[] {
        // Get current connections (includes all reconnected connections with updated from/to)
        const connections: ManualConnection[] = this.connectionManager.getConnections().map(conn => ({ ...conn }));
        const cloned = this.cloneNodes(this.nodes);
        // Order nodes based on connections (topological sort respects reconnected connections)
        return this.orderNodesRecursively(cloned, connections);
    }

    private orderNodesRecursively(nodes: VisualNode[], connections: ManualConnection[]): VisualNode[] {
        if (nodes.length === 0) {
            return nodes;
        }

        const ordered = this.applyConnectionOrdering(nodes, connections);

        ordered.forEach(node => {
            if (node.children && node.children.length > 0) {
                node.children = this.orderNodesRecursively(node.children, connections);
            }
        });

        return ordered;
    }

    private applyConnectionOrdering(nodes: VisualNode[], connections: ManualConnection[]): VisualNode[] {
        if (nodes.length <= 1) {
            return nodes;
        }

        const idSet = new Set(nodes.map(node => node.id));
        const relevantConnections = connections.filter(conn => idSet.has(conn.from) && idSet.has(conn.to));

        if (relevantConnections.length === 0) {
            return nodes;
        }

        const nodeMap = new Map<string, VisualNode>();
        const inDegree = new Map<string, number>();
        const adjacency = new Map<string, string[]>();
        const initialOrder: string[] = [];

        nodes.forEach(node => {
            nodeMap.set(node.id, node);
            inDegree.set(node.id, 0);
            adjacency.set(node.id, []);
            initialOrder.push(node.id);
        });

        relevantConnections.forEach(conn => {
            const neighbors = adjacency.get(conn.from);
            if (neighbors) {
                neighbors.push(conn.to);
                inDegree.set(conn.to, (inDegree.get(conn.to) || 0) + 1);
            }
        });

        const queue: string[] = [];
        initialOrder.forEach(id => {
            if ((inDegree.get(id) || 0) === 0) {
                queue.push(id);
            }
        });

        const orderedIds: string[] = [];

        while (queue.length > 0) {
            const currentId = queue.shift()!;
            orderedIds.push(currentId);

            const neighbors = adjacency.get(currentId) || [];
            neighbors.forEach(neighbor => {
                const newDegree = (inDegree.get(neighbor) || 0) - 1;
                inDegree.set(neighbor, newDegree);
                if (newDegree === 0) {
                    queue.push(neighbor);
                }
            });
        }

        const used = new Set(orderedIds);
        initialOrder.forEach(id => {
            if (!used.has(id)) {
                orderedIds.push(id);
                used.add(id);
            }
        });

        return orderedIds
            .map(id => nodeMap.get(id))
            .filter((node): node is VisualNode => Boolean(node));
    }

    /**
     * Save configuration as
     */
    async saveConfigurationAs(filename: string): Promise<boolean> {
        this.currentFilename = filename;
        this.markClean();
        const exportNodes = this.prepareNodesForExport();
        const exportConnections = this.connectionManager.getConnections().map(conn => ({ ...conn }));
        this.emit('saveAsRequested', {
            filename,
            nodes: exportNodes,
            connections: exportConnections,
            xml: this.generateXML()
        });
        return true;
    }

    // ========================================================================
    // Execution State Visualization (Phase 1)
    // ========================================================================

    /**
     * Update execution state for a node
     */
    updateNodeExecutionState(nodeId: string, state: NodeExecutionState): void {
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
        if (!nodeElement) {
            console.warn(`[VisualDesigner] Node element not found: ${nodeId}`);
            return;
        }

        this.stateRenderer.applyState(nodeElement, state);
        console.log(`[VisualDesigner] Applied state ${state} to node ${nodeId}`);
    }

    /**
     * Update metrics for a running node
     */
    updateNodeMetrics(nodeId: string, metrics: NodeMetrics): void {
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
        if (!nodeElement) {
            console.warn(`[VisualDesigner] Node element not found: ${nodeId}`);
            return;
        }

        this.metricsOverlay.showMetrics(nodeElement, metrics);
    }

    /**
     * Clear all execution states (reset to idle)
     */
    clearExecutionStates(): void {
        const nodeElements = this.canvasElement.querySelectorAll('[data-node-id]');
        nodeElements.forEach(element => {
            this.stateRenderer.clearState(element as HTMLElement);
            this.metricsOverlay.hideMetrics(element as HTMLElement);
        });

        console.log('[VisualDesigner] Cleared all execution states');
    }

    /**
     * Get nodes for execution state registration
     */
    getNodesForExecution(): VisualNode[] {
        return [...this.nodes];
    }

    /**
     * Set plugin manager for semantic validation (called after plugins are loaded)
     */
    setPluginManager(pluginManager: PluginManager): void {
        this.pluginManager = pluginManager;
        this.semanticValidator = new SemanticValidator(pluginManager);
        this.detailRegistry = new MasterDetailRegistry(pluginManager);
        console.log('✅ [VisualDesigner] Semantic validator initialized');

        setContainerResolver((type: string) => {
            if (!this.semanticValidator) {
                return false;
            }
            return this.semanticValidator.allowsChildren(type);
        });
    }

    /**
     * Apply role-based styling to node element
     */
    private applyRoleBasedStyling(element: HTMLElement, roles: string[], node?: VisualNode): void {
        // Remove existing role classes
        element.classList.remove('role-root-container', 'role-context-container', 'role-body-evaluator', 'role-dependent', 'role-leaf');

        // Get plugin metadata for additional styling
        let plugin: any = null;
        if (node && this.pluginManager) {
            plugin = this.pluginManager.findPlugin(node.type);
        }

        const primaryRole = roles[0] || 'LEAF';

        switch (primaryRole) {
            case 'ROOT_CONTAINER':
                element.classList.add('role-root-container');
                element.style.borderWidth = '3px';
                element.style.borderStyle = 'dashed';
                element.style.background = 'linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%)';
                element.style.borderColor = '#9C27B0';
                // Add icon indicator
                this.addRoleIndicator(element, 'ROOT_CONTAINER', node);
                break;
            case 'CONTEXT_CONTAINER':
                element.classList.add('role-context-container');
                element.style.borderWidth = '2px';
                element.style.borderStyle = 'solid';
                element.style.background = '#fafafa';
                element.style.borderColor = '#2196F3';
                // Add expand/collapse indicator with child count
                this.addRoleIndicator(element, 'CONTEXT_CONTAINER', node);
                if (node && node.children && node.children.length > 0) {
                    this.addChildCountBadge(element, node.children.length);
                }
                break;
            case 'BODY_EVALUATOR':
                element.classList.add('role-body-evaluator');
                element.style.borderWidth = '2px';
                element.style.borderStyle = 'solid';
                element.style.borderColor = '#4CAF50';
                element.style.background = '#f1f8f4';
                this.addRoleIndicator(element, 'BODY_EVALUATOR', node);
                break;
            case 'DEPENDENT':
                element.classList.add('role-dependent');
                element.style.borderWidth = '2px';
                element.style.borderStyle = 'dotted';
                element.style.opacity = '0.9';
                element.style.background = '#fff9e6';
                element.style.borderColor = '#FF9800';
                // Add parent requirement indicator
                this.addRoleIndicator(element, 'DEPENDENT', node);
                if (plugin?.semantics?.requiredParentTypes && plugin.semantics.requiredParentTypes.length > 0) {
                    this.addParentRequirementIndicator(element, plugin.semantics.requiredParentTypes);
                }
                break;
            case 'LEAF':
            default:
                element.classList.add('role-leaf');
                element.style.borderColor = '#757575';
                // Default styling already applied
                break;
        }

        // Add semantic metadata tooltip
        if (plugin?.semantics) {
            const tooltipParts: string[] = [];
            const tooltipRoles: string[] = Array.isArray(plugin.semantics.roles)
                ? plugin.semantics.roles
                : (plugin.semantics.role ? [plugin.semantics.role] : []);
            if (tooltipRoles.length > 0) {
                tooltipParts.push(`Roles: ${tooltipRoles.join(', ')}`);
            }
            if (plugin.semantics.requiredParentTypes && plugin.semantics.requiredParentTypes.length > 0) {
                tooltipParts.push(`Requires: ${plugin.semantics.requiredParentTypes.join(', ')}`);
            }
            if (plugin.semantics.allowedChildTypes && plugin.semantics.allowedChildTypes.length > 0) {
                tooltipParts.push(`Allows: ${plugin.semantics.allowedChildTypes.join(', ')}`);
            }
            if (tooltipParts.length > 0) {
                element.title = tooltipParts.join(' | ');
            }
        }

        element.dataset.primaryRole = primaryRole;
        element.dataset.roles = roles.join(',');
    }

    /**
     * Add role indicator icon/badge to element
     */
    private addRoleIndicator(element: HTMLElement, role: string, node?: VisualNode): void {
        // Check if indicator already exists
        if (element.querySelector('.role-indicator')) {
            return;
        }

        const indicator = document.createElement('div');
        indicator.className = 'role-indicator';
        indicator.style.cssText = `
            position: absolute;
            top: 4px;
            right: 4px;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            font-weight: bold;
            color: white;
            z-index: 10;
        `;

        const roleColors: Record<string, string> = {
            'ROOT_CONTAINER': '#9C27B0',
            'CONTEXT_CONTAINER': '#2196F3',
            'BODY_EVALUATOR': '#4CAF50',
            'DEPENDENT': '#FF9800',
            'LEAF': '#757575'
        };

        const roleIcons: Record<string, string> = {
            'ROOT_CONTAINER': '🏠',
            'CONTEXT_CONTAINER': '📦',
            'BODY_EVALUATOR': '⚡',
            'DEPENDENT': '🔗',
            'LEAF': '●'
        };

        indicator.style.background = roleColors[role] || '#999';
        indicator.textContent = roleIcons[role] || '●';
        indicator.title = `Semantic role: ${role}`;

        // Ensure element has relative positioning
        if (element.style.position !== 'absolute' && element.style.position !== 'relative') {
            element.style.position = 'relative';
        }

        element.appendChild(indicator);
    }

    /**
     * Add child count badge for containers
     */
    private addChildCountBadge(element: HTMLElement, childCount: number): void {
        // Check if badge already exists
        if (element.querySelector('.child-count-badge')) {
            return;
        }

        const badge = document.createElement('div');
        badge.className = 'child-count-badge';
        badge.textContent = `${childCount}`;
        badge.style.cssText = `
            position: absolute;
            bottom: 4px;
            right: 4px;
            background: rgba(33, 150, 243, 0.9);
            color: white;
            padding: 2px 6px;
            border-radius: 10px;
            font-size: 10px;
            font-weight: 600;
            z-index: 10;
        `;
        badge.title = `${childCount} child plugin${childCount !== 1 ? 's' : ''}`;

        // Ensure element has relative positioning
        if (element.style.position !== 'absolute' && element.style.position !== 'relative') {
            element.style.position = 'relative';
        }

        element.appendChild(badge);
    }

    /**
     * Add parent requirement indicator for dependent plugins
     */
    private addParentRequirementIndicator(element: HTMLElement, requiredParents: string[]): void {
        // Check if indicator already exists
        if (element.querySelector('.parent-requirement-indicator')) {
            return;
        }

        const indicator = document.createElement('div');
        indicator.className = 'parent-requirement-indicator';
        indicator.innerHTML = '<i class="fas fa-link"></i>';
        indicator.style.cssText = `
            position: absolute;
            top: 4px;
            left: 4px;
            width: 20px;
            height: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: rgba(255, 152, 0, 0.2);
            border-radius: 50%;
            color: #FF9800;
            font-size: 10px;
            z-index: 10;
        `;
        indicator.title = `Requires parent: ${requiredParents.join(', ')}`;

        // Ensure element has relative positioning
        if (element.style.position !== 'absolute' && element.style.position !== 'relative') {
            element.style.position = 'relative';
        }

        element.appendChild(indicator);
    }

    /**
     * Apply validation error styling to node
     */
    private applyValidationErrorStyling(element: HTMLElement, errorMessage: string): void {
        element.classList.add('validation-error');
        element.style.borderColor = '#ef4444';
        element.style.borderWidth = '3px';
        element.style.borderStyle = 'dashed';
        element.style.background = '#fee';

        // Add error indicator
        if (!element.querySelector('.validation-error-indicator')) {
            const errorIndicator = document.createElement('div');
            errorIndicator.className = 'validation-error-indicator';
            errorIndicator.innerHTML = '<i class="fas fa-exclamation-triangle"></i>';
            errorIndicator.style.cssText = `
                position: absolute;
                top: 4px;
                left: 4px;
                width: 24px;
                height: 24px;
                background: #ef4444;
                color: white;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 12px;
                z-index: 20;
                cursor: help;
            `;
            errorIndicator.title = errorMessage;
            element.appendChild(errorIndicator);
        }
    }

    /**
     * Clear validation errors for a node
     */
    clearValidationError(nodeId: string): void {
        this.validationErrors.delete(nodeId);
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
        if (nodeElement) {
            nodeElement.classList.remove('validation-error');
            const errorIndicator = nodeElement.querySelector('.validation-error-indicator');
            if (errorIndicator) {
                errorIndicator.remove();
            }
        }
    }

    /**
     * Validate all nodes and highlight errors
     */
    validateAllNodes(): void {
        this.validationErrors.clear();

        if (!this.semanticValidator) {
            return;
        }

        // Validate all nodes
        const validateNode = (node: VisualNode, parentType: string | null) => {
            const validation = this.semanticValidator!.validateDrop(node.type, parentType);
            if (!validation.valid) {
                this.validationErrors.set(node.id, validation.message);
            }

            // Validate children
            if (node.children && node.children.length > 0) {
                node.children.forEach(child => {
                    validateNode(child, node.type);
                });
            }
        };

        // Validate root nodes
        this.nodes.forEach(node => {
            validateNode(node, null);
        });

        // Re-render to show errors
        this.render();
    }

    /**
     * Handle node resize
     */
    private handleNodeResize(nodeId: string, dimensions: { width: number; height: number }): void {
        const node = this.findNode(nodeId);
        if (!node) return;

        // Update node dimensions
        node.dimensions = dimensions;

        // Update node element
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
        if (nodeElement) {
            nodeElement.style.width = `${dimensions.width}px`;
            nodeElement.style.height = `${dimensions.height}px`;
        }

        // Update port positions (ports are positioned relative to node)
        // PortSystem will handle this on next render, but we can trigger update here
        this.updatePortPositions(nodeId);

        // Update connections (connection lines need to be redrawn)
        this.renderConnections();

        // Mark dirty
        this.markDirty();
    }

    /**
     * Update port positions after resize
     */
    private updatePortPositions(nodeId: string): void {
        // Port positions are calculated relative to node center/edges
        // They will be updated automatically on next render
        // This is a placeholder for future optimization
    }

    /**
     * Show error notification
     */
    private showError(message: string): void {
        // Create error notification element
        const errorDiv = document.createElement('div');
        errorDiv.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #ef4444;
            color: white;
            padding: 12px 20px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 10000;
            font-size: 14px;
            font-weight: 500;
            max-width: 400px;
            animation: slideIn 0.3s ease-out;
        `;
        errorDiv.textContent = message;
        document.body.appendChild(errorDiv);

        // Auto-remove after 3 seconds
        setTimeout(() => {
            errorDiv.style.animation = 'slideOut 0.3s ease-in';
            setTimeout(() => {
                if (errorDiv.parentNode) {
                    errorDiv.parentNode.removeChild(errorDiv);
                }
            }, 300);
        }, 3000);
    }

    private isIfNode(node: VisualNode | null | undefined): boolean {
        return Boolean(node?.type && node.type.toLowerCase() === 'if');
    }

    private isTryNode(node: VisualNode | null | undefined): boolean {
        return Boolean(node?.type && node.type.toLowerCase() === 'try');
    }

    private renderIfDetail(childrenArea: HTMLElement, parentNode: VisualNode, elseNode: VisualNode | null): void {
        const summary = document.createElement('div');
        summary.className = 'control-summary control-summary--if';
        summary.dataset.parentId = parentNode.id;

        const header = document.createElement('div');
        header.className = 'control-summary__header';

        const title = document.createElement('span');
        title.textContent = 'Else branch';

        const actions = document.createElement('div');
        actions.className = 'control-summary__actions';

        if (!elseNode) {
            const addButton = this.createControlActionButton('fas fa-plus', 'Add else branch', () => {
                this.addControlDetail(parentNode.id, 'else');
            });
            actions.appendChild(addButton);
        } else {
            const removeButton = this.createControlActionButton('fas fa-trash', 'Remove else branch', () => {
                this.removeControlDetail(elseNode.id, parentNode.id);
            });
            const openButton = this.createControlActionButton('fas fa-pen', 'Edit else properties', () => {
                this.selectNode(elseNode.id);
            });
            actions.appendChild(openButton);
            actions.appendChild(removeButton);
        }

        header.appendChild(title);
        header.appendChild(actions);
        summary.appendChild(header);

        if (elseNode) {
            const card = this.renderControlDetailCard(parentNode, elseNode, 'Else branch', 'else', 0, 1);
            summary.appendChild(card);
        } else {
            const empty = document.createElement('div');
            empty.className = 'control-summary__empty';
            empty.textContent = 'No else branch defined. Use the button above to add fallback logic.';
            summary.appendChild(empty);
        }

        childrenArea.appendChild(summary);
    }

    private renderTryDetail(childrenArea: HTMLElement, parentNode: VisualNode, catchNodes: VisualNode[]): void {
        const summary = document.createElement('div');
        summary.className = 'control-summary control-summary--try';
        summary.dataset.parentId = parentNode.id;

        const header = document.createElement('div');
        header.className = 'control-summary__header';

        const title = document.createElement('span');
        title.textContent = 'Catch blocks';

        const actions = document.createElement('div');
        actions.className = 'control-summary__actions';

        const addButton = this.createControlActionButton('fas fa-plus', 'Add catch block', () => {
            this.addControlDetail(parentNode.id, 'catch');
        });
        actions.appendChild(addButton);

        header.appendChild(title);
        header.appendChild(actions);
        summary.appendChild(header);

        if (catchNodes.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'control-summary__empty';
            empty.textContent = 'No catch blocks defined. Add at least one catch to handle errors in the try block.';
            summary.appendChild(empty);
        } else {
            catchNodes.forEach((catchNode, index) => {
                const card = this.renderControlDetailCard(parentNode, catchNode, `Catch #${index + 1}`, 'catch', index, catchNodes.length);
                summary.appendChild(card);
            });
        }

        childrenArea.appendChild(summary);
    }

    private renderControlDetailCard(
        parentNode: VisualNode,
        detailNode: VisualNode,
        label: string,
        detailType: ControlDetailType,
        index: number,
        total: number
    ): HTMLElement {
        const card = document.createElement('div');
        card.className = 'control-detail-card';
        card.dataset.nodeId = detailNode.id;

        const header = document.createElement('div');
        header.className = 'control-detail-card__header';

        const title = document.createElement('span');
        title.textContent = label;

        const actions = document.createElement('div');
        actions.className = 'control-detail-card__actions';

        const propertiesButton = this.createControlActionButton('fas fa-pen', 'Edit properties', () => {
            this.selectNode(detailNode.id);
        });
        actions.appendChild(propertiesButton);

        if (detailType === 'catch') {
            const upButton = this.createControlActionButton('fas fa-arrow-up', 'Move catch up', () => {
                this.moveControlDetail(parentNode.id, detailNode.id, 'up');
            }, index === 0);
            const downButton = this.createControlActionButton('fas fa-arrow-down', 'Move catch down', () => {
                this.moveControlDetail(parentNode.id, detailNode.id, 'down');
            }, index === total - 1);
            actions.appendChild(upButton);
            actions.appendChild(downButton);
        }

        const removeButton = this.createControlActionButton('fas fa-trash', detailType === 'else' ? 'Remove else branch' : 'Remove catch block', () => {
            this.removeControlDetail(detailNode.id, parentNode.id);
        });
        removeButton.classList.add('danger');
        actions.appendChild(removeButton);

        header.appendChild(title);
        header.appendChild(actions);
        card.appendChild(header);

        const body = document.createElement('div');
        body.className = 'control-detail-card__body container-children';
        body.dataset.nodeId = detailNode.id;
        body.dataset.parentId = parentNode.id;

        if (!detailNode.children || detailNode.children.length === 0) {
            const placeholder = document.createElement('div');
            placeholder.className = 'control-detail-card__empty';
            placeholder.textContent = 'Drop plugins here to define this branch.';
            body.appendChild(placeholder);
        } else {
            detailNode.children.forEach((child, childIndex) => {
                const childElement = this.renderNodeWithStrategy(child, body);
                childElement.dataset.parentId = detailNode.id;
                childElement.dataset.childIndex = childIndex.toString();
                this.addReorderButtons(childElement, detailNode.id, child.id, childIndex, detailNode.children!.length);
            });
        }

        card.appendChild(body);
        return card;
    }

    private createControlActionButton(icon: string, title: string, handler: () => void, disabled = false): HTMLButtonElement {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'control-action-button';
        button.innerHTML = `<i class="${icon}"></i>`;
        button.title = title;
        button.disabled = disabled;
        button.addEventListener('click', (event) => {
            event.stopPropagation();
            if (!button.disabled) {
                handler();
            }
        });
        return button;
    }

    private addControlDetail(parentId: string, detailType: ControlDetailType): void {
        const parentNode = this.findNode(parentId);
        if (!parentNode) {
            return;
        }

        if (detailType === 'else') {
            const existingElse = (parentNode.children || []).some(child => child.type === 'else');
            if (existingElse) {
                this.showError('This if block already has an else branch. Remove it before adding another.');
                return;
            }
        }

        const detailNode = createControlDetailNode(detailType);
        const added = this.addChildToContainer(parentId, detailNode);
        if (added) {
            this.selectNode(detailNode.id);
        }
    }

    private removeControlDetail(detailId: string, parentId: string): void {
        const removed = this.removeNode(detailId);
        if (removed) {
            this.selectNode(parentId);
        }
    }

    private moveControlDetail(parentId: string, detailId: string, direction: 'up' | 'down'): void {
        const moved = direction === 'up'
            ? this.moveChildUp(parentId, detailId)
            : this.moveChildDown(parentId, detailId);

        if (moved) {
            this.selectNode(parentId);
        }
    }

    setWorkflowStatus(status: WorkflowStatus): void {
        if (this.workflowStatus === status) {
            return;
        }
        this.workflowStatus = status;
        this.render();
    }

    getWorkflowStatus(): WorkflowStatus {
        return this.workflowStatus;
    }

    private getConnectionAnchor(targetId: string): ({ centerX: number; centerY: number; width: number; height: number; type: 'node' | 'marker' }) | null {
        const computeMarkerAnchor = (element: HTMLElement): { centerX: number; centerY: number; width: number; height: number; type: 'marker' } => {
            const canvasRect = this.canvasElement.getBoundingClientRect();
            const markerRect = element.getBoundingClientRect();
            const width = markerRect.width || element.offsetWidth || 56;
            const height = markerRect.height || element.offsetHeight || 56;
            const centerX = markerRect.left - canvasRect.left + width / 2;
            const centerY = markerRect.top - canvasRect.top + height / 2;
            return { centerX, centerY, width, height, type: 'marker' };
        };

        if (targetId === 'workflow-start' && this.startMarkerElement) {
            return computeMarkerAnchor(this.startMarkerElement);
        }

        if (targetId === 'workflow-end' && this.endMarkerElement) {
            return computeMarkerAnchor(this.endMarkerElement);
        }

        const node = this.findNodeRecursive(targetId, this.nodes);
        if (!node || !node.position) {
            return null;
        }

        const dimensions = this.getNodeDimensions(node);
        return {
            centerX: node.position.x + dimensions.width / 2,
            centerY: node.position.y + dimensions.height / 2,
            width: dimensions.width,
            height: dimensions.height,
            type: 'node'
        };
    }

    getDetailRegistry(): MasterDetailRegistry | null {
        return this.detailRegistry;
    }

    private renderMasterDetailContainer(childrenArea: HTMLElement, containerNode: VisualNode, sections: DetailSectionDefinition[]): void {
        if (!this.detailRegistry) {
            this.renderStandardChildren(containerNode.children || [], containerNode, childrenArea);
            return;
        }

        childrenArea.classList.add('http-container');

        const partition = this.detailRegistry.partitionChildren(containerNode);

        sections.forEach(section => {
            const detailNodes = partition.sections[section.detailType] || [];
            const sectionElement = this.createDetailSectionElement(containerNode, section, detailNodes);
            childrenArea.appendChild(sectionElement);
        });

        if (partition.unmatched.length > 0) {
            this.renderStandardChildren(partition.unmatched, containerNode, childrenArea);
        }
    }

    private createDetailSectionElement(
        containerNode: VisualNode,
        section: DetailSectionDefinition,
        details: VisualNode[]
    ): HTMLElement {
        const sectionEl = document.createElement('div');
        sectionEl.className = 'http-detail-section';

        const header = document.createElement('div');
        header.className = 'http-detail-section__header';

        const titleEl = document.createElement('span');
        titleEl.className = 'http-detail-section__title';
        titleEl.textContent = `${section.sectionTitle} (${details.length})`;

        const actions = document.createElement('div');
        actions.className = 'http-detail-section__actions';

        const addButton = this.createDetailActionButton('fas fa-plus', section.addLabel, () => {
            this.addDetailNode(containerNode.id, section.detailType);
        });
        actions.appendChild(addButton);

        header.appendChild(titleEl);
        header.appendChild(actions);
        sectionEl.appendChild(header);

        const list = document.createElement('div');
        list.className = 'http-detail-list';

        if (details.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'http-detail-empty';
            empty.textContent = section.emptyLabel;
            list.appendChild(empty);
        } else {
            details.forEach((detail, index) => {
                const row = this.createDetailRow(containerNode, section, detail, index, details.length);
                list.appendChild(row);
            });
        }

        sectionEl.appendChild(list);
        return sectionEl;
    }

    private createDetailRow(
        containerNode: VisualNode,
        section: DetailSectionDefinition,
        detail: VisualNode,
        index: number,
        total: number
    ): HTMLElement {
        const row = document.createElement('div');
        row.className = 'http-detail-row';
        row.dataset.detailId = detail.id;

        const summary = this.detailRegistry!.summarizeDetail(detail);

        const nameEl = document.createElement('span');
        nameEl.className = summary.isEmpty ? 'http-detail-row__label http-detail-row__label--empty' : 'http-detail-row__label';
        nameEl.textContent = summary.primaryText;

        const valueEl = document.createElement('span');
        valueEl.className = summary.secondaryText ? 'http-detail-row__value' : 'http-detail-row__value http-detail-row__value--empty';
        valueEl.textContent = summary.secondaryText ?? '—';

        const actions = document.createElement('div');
        actions.className = 'http-detail-row__actions';

        const editButton = this.createDetailActionButton('fas fa-pen', 'Edit in properties', () => {
            this.selectNode(detail.id);
        });
        actions.appendChild(editButton);

        const moveUpButton = this.createDetailActionButton('fas fa-arrow-up', 'Move up', () => {
            this.moveDetailNode(containerNode.id, detail.id, 'up');
        }, index === 0);
        actions.appendChild(moveUpButton);

        const moveDownButton = this.createDetailActionButton('fas fa-arrow-down', 'Move down', () => {
            this.moveDetailNode(containerNode.id, detail.id, 'down');
        }, index === total - 1);
        actions.appendChild(moveDownButton);

        const duplicateButton = this.createDetailActionButton('fas fa-clone', 'Duplicate', () => {
            this.duplicateDetailNode(containerNode.id, detail.id);
        });
        actions.appendChild(duplicateButton);

        const deleteButton = this.createDetailActionButton('fas fa-trash', 'Remove', () => {
            this.removeDetailNode(containerNode.id, detail.id);
        });
        deleteButton.classList.add('danger');
        actions.appendChild(deleteButton);

        row.appendChild(nameEl);
        row.appendChild(valueEl);
        row.appendChild(actions);

        return row;
    }

    private createDetailActionButton(iconClass: string, title: string, action: () => void, disabled = false): HTMLButtonElement {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'http-detail-row__action';
        button.innerHTML = `<i class="${iconClass}"></i>`;
        button.title = title;
        button.disabled = disabled;
        button.addEventListener('click', (event) => {
            event.stopPropagation();
            if (!button.disabled) {
                action();
            }
        });
        return button;
    }

    private addDetailNode(containerId: string, detailType: string): void {
        if (!this.detailRegistry) {
            return;
        }
        const detailNode = this.detailRegistry.createDetailNode(detailType);
        if (!detailNode) {
            this.showError(`Cannot create detail of type ${detailType}`);
            return;
        }
        const added = this.addChildToContainer(containerId, detailNode);
        if (added) {
            this.selectNode(containerId);
        }
    }

    private removeDetailNode(containerId: string, detailId: string): void {
        const removed = this.removeNode(detailId);
        if (removed) {
            this.selectNode(containerId);
        }
    }

    private moveDetailNode(containerId: string, detailId: string, direction: 'up' | 'down'): void {
        const moved = direction === 'up'
            ? this.moveChildUp(containerId, detailId)
            : this.moveChildDown(containerId, detailId);

        if (moved) {
            this.selectNode(containerId);
        }
    }

    private duplicateDetailNode(containerId: string, detailId: string): void {
        if (!this.detailRegistry) {
            return;
        }

        const container = this.findNode(containerId);
        const original = this.findNode(detailId);

        if (!container || !original) {
            return;
        }

        const duplicate = this.detailRegistry.createDetailNode(original.type);
        if (!duplicate) {
            this.showError(`Cannot duplicate detail of type ${original.type}`);
            return;
        }

        duplicate.attributes = {
            ...original.attributes
        };
        duplicate.body = original.body;
        duplicate.children = original.children ? this.cloneNodes(original.children) : [];

        const updatedContainer = this.cloneNode(container);
        updatedContainer.children = [
            ...(updatedContainer.children || []),
            duplicate
        ];

        const updated = this.updateNode(containerId, updatedContainer);
        if (updated) {
            this.selectNode(duplicate.id);
        }
    }

    private getFocusedContainer(): VisualNode | null {
        const focusId = this.drillDownManager.getCurrentId();
        if (!focusId) {
            return null;
        }
        const node = this.findNode(focusId);
        if (!node || !isContainerNode(node.type)) {
            this.drillDownManager.reset();
            return null;
        }
        return node;
    }

    private getNodeDisplayLabel(node: VisualNode): string {
        const nameAttr = (node.attributes?.name || node.attributes?.id || '').toString().trim();
        const titleCaseType = node.type.charAt(0).toUpperCase() + node.type.slice(1);
        if (nameAttr.length > 0) {
            return `${titleCaseType}: ${nameAttr}`;
        }
        return titleCaseType;
    }

    private updateFocusSummary(): void {
        if (!this.focusSummaryContainer) {
            return;
        }

        const container = this.focusSummaryContainer;
        container.innerHTML = '';
        container.setAttribute('role', 'status');
        container.setAttribute('aria-live', 'polite');

        const focusNode = this.getFocusedContainer();

        if (!focusNode) {
            const overview = document.createElement('div');
            overview.className = 'focus-summary__content';
            const title = this.createSummaryTitle('Workflow overview', this.createFontAwesomeIcon('fa-layer-group', '#636366', 12));
            overview.appendChild(title);

            const stats = analyzeHierarchy(this.nodes);
            const chipsRow = document.createElement('div');
            chipsRow.className = 'focus-summary__chips';
            chipsRow.appendChild(this.createSummaryChip(this.createFontAwesomeIcon('fa-diagram-project'), 'Total nodes', stats.totalNodes.toString()));
            chipsRow.appendChild(this.createSummaryChip(this.createFontAwesomeIcon('fa-boxes'), 'Containers', stats.containerCount.toString()));
            chipsRow.appendChild(this.createSummaryChip(this.createFontAwesomeIcon('fa-atom'), 'Leaves', stats.leafCount.toString()));
            overview.appendChild(chipsRow);

            const topTypes = this.collectTypeFrequency(this.nodes).slice(0, 3);
            if (topTypes.length > 0) {
                const section = this.createSummarySection('Top components', topTypes.map(entry => {
                    const icon = this.iconLibrary.getIconElement(entry.type, 11);
                    return this.createSummaryChip(icon, entry.type, `×${entry.count}`);
                }));
                overview.appendChild(section);
            }

            container.appendChild(overview);
            container.appendChild(this.createSummaryHint('Execution metrics will appear while the workflow runs.'));
            return;
        }

        const summary = analyzeContainer(focusNode, this.detailRegistry);
        const content = document.createElement('div');
        content.className = 'focus-summary__content';

        const title = this.createSummaryTitle(this.getNodeDisplayLabel(focusNode), this.iconLibrary.getIconElement(focusNode.type, 12));
        content.appendChild(title);

        const chipsRow = document.createElement('div');
        chipsRow.className = 'focus-summary__chips';
        chipsRow.appendChild(this.createSummaryChip(this.createFontAwesomeIcon('fa-list-check'), 'Children', summary.childCount.toString()));
        chipsRow.appendChild(this.createSummaryChip(this.createFontAwesomeIcon('fa-layer-group'), 'Nested containers', summary.containerCount.toString()));
        chipsRow.appendChild(this.createSummaryChip(this.createFontAwesomeIcon('fa-code-branch'), 'Leaves', summary.leafCount.toString()));
        chipsRow.appendChild(this.createSummaryChip(this.createFontAwesomeIcon('fa-link'), 'Details', summary.detailCount.toString()));
        content.appendChild(chipsRow);

        const topChildren = summary.typeFrequency.slice(0, 3);
        if (topChildren.length > 0) {
            const section = this.createSummarySection('Top child components', topChildren.map(entry => {
                const icon = this.iconLibrary.getIconElement(entry.type, 11);
                return this.createSummaryChip(icon, entry.type, `×${entry.count}`, true);
            }));
            content.appendChild(section);
        }

        if (summary.attributeHighlights.length > 0) {
            const attributeSection = this.createSummarySection('Key settings', summary.attributeHighlights.map(({ key, value }) => {
                const icon = this.createFontAwesomeIcon('fa-sliders-h', '#636366', 11);
                return this.createSummaryChip(icon, key, value, true);
            }));
            content.appendChild(attributeSection);
        }

        container.appendChild(content);
    }

    private createSummaryTitle(label: string, icon: HTMLElement | null): HTMLElement {
        const title = document.createElement('div');
        title.className = 'focus-summary__title';
        if (icon) {
            icon.classList.add('focus-summary__title-icon');
            title.appendChild(icon);
        }
        const text = document.createElement('span');
        text.textContent = label;
        title.appendChild(text);
        return title;
    }

    private createSummaryChip(icon: HTMLElement | null, label: string, value: string, muted: boolean = false): HTMLElement {
        const chip = document.createElement('div');
        chip.className = 'focus-summary__chip';
        if (muted) {
            chip.classList.add('focus-summary__chip--muted');
        }

        if (icon) {
            icon.classList.add('focus-summary__chip-icon');
            chip.appendChild(icon);
        }

        const labelEl = document.createElement('span');
        labelEl.className = 'focus-summary__chip-label';
        labelEl.textContent = label;
        chip.appendChild(labelEl);

        const valueEl = document.createElement('span');
        valueEl.className = 'focus-summary__chip-value';
        valueEl.textContent = value;
        chip.appendChild(valueEl);

        return chip;
    }

    private createSummarySection(title: string, chips: HTMLElement[]): HTMLElement {
        const section = document.createElement('div');
        section.className = 'focus-summary__section';

        const label = document.createElement('span');
        label.className = 'focus-summary__section-label';
        label.textContent = title;
        section.appendChild(label);

        const chipRow = document.createElement('div');
        chipRow.className = 'focus-summary__section-chips';
        chips.forEach(chip => chipRow.appendChild(chip));
        section.appendChild(chipRow);

        return section;
    }

    private createSummaryHint(text: string): HTMLElement {
        const hint = document.createElement('span');
        hint.className = 'focus-summary__hint';
        hint.textContent = text;
        return hint;
    }

    private createFontAwesomeIcon(iconClass: string, color: string = '#636366', size: number = 12): HTMLElement {
        const icon = document.createElement('i');
        icon.className = `fas ${iconClass}`;
        icon.style.color = color;
        icon.style.fontSize = `${size}px`;
        return icon;
    }

    private collectTypeFrequency(nodes: VisualNode[]): Array<{ type: string; count: number }> {
        const map = new Map<string, number>();

        const visit = (node: VisualNode): void => {
            map.set(node.type, (map.get(node.type) ?? 0) + 1);
            (node.children || []).forEach(visit);
        };

        nodes.forEach(visit);

        return Array.from(map.entries())
            .map(([type, count]) => ({ type, count }))
            .sort((a, b) => b.count - a.count);
    }

    private updateBreadcrumbTrail(): void {
        if (!this.breadcrumbContainer) {
            return;
        }

        const trail: BreadcrumbItem[] = [
            { id: '__root__', label: 'Workflow', level: 0, nodeType: 'root' }
        ];

        const stack = this.drillDownManager.getTrail();
        stack.forEach((id, index) => {
            const node = this.findNode(id);
            if (!node) {
                return;
            }
            trail.push({
                id,
                label: this.getNodeDisplayLabel(node),
                level: index + 1,
                nodeType: node.type
            });
        });

        this.breadcrumbNav.render(this.breadcrumbContainer, trail);
        this.updateFocusSummary();
    }

    private handleBreadcrumbNavigate(item: BreadcrumbItem): void {
        if (item.level === 0) {
            if (this.drillDownManager.getTrail().length === 0) {
                return;
            }
            this.drillDownManager.reset();
        } else {
            this.drillDownManager.navigateToLevel(item.level);
        }
        this.updateBreadcrumbTrail();
        this.render();
    }

    private enterContainer(containerId: string): void {
        const node = this.findNode(containerId);
        if (!node || !isContainerNode(node.type)) {
            return;
        }
        this.drillDownManager.enter(containerId);
        this.container.focus();
        this.updateBreadcrumbTrail();
        this.render();
    }

    private exitFocusLevel(): void {
        const exited = this.drillDownManager.exit();
        if (exited === null) {
            return;
        }
        this.container.focus();
        this.updateBreadcrumbTrail();
        this.render();
    }
}

// Add animation keyframes if not already present
if (!document.getElementById('validation-error-styles')) {
    const style = document.createElement('style');
    style.id = 'validation-error-styles';
    style.textContent = `
        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        @keyframes slideOut {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(100%);
                opacity: 0;
            }
        }
    `;
    document.head.appendChild(style);
}
