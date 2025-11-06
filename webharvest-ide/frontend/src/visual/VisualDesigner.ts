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
    calculateLinearLayout,
    calculateNestedLayout,
    generateConnections,
    type Connection
} from './VisualModelHelpers';
import { NodeStateRenderer, type NodeExecutionState } from './NodeStateRenderer';
import { NodeMetricsOverlay, type NodeMetrics } from './NodeMetricsOverlay';
import { GridSystem } from './GridSystem';
import { Minimap } from './Minimap';
import { IconLibrary } from './IconLibrary';
import { ContainerRenderer } from './ContainerRenderer';
import { PortSystem } from './PortSystem';
import { ConnectionManager, type Connection as ManualConnection, type ConnectionType } from './ConnectionManager';
import { XMLGenerator } from './XMLGenerator';
import { SemanticValidator } from './SemanticValidator';
import type { PluginManager } from '../core/PluginManager';

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

export class VisualDesigner {
    private nodes: VisualNode[] = [];
    private connections: Connection[] = [];
    private container: HTMLElement;
    private canvasElement: HTMLElement;
    private svgElement: SVGSVGElement | null = null;
    private selectedNodeId: string | null = null;
    private selectedNodeIds: string[] = [];
    private selectedConnectionId: string | null = null;
    private eventListeners: Map<string, EventListener[]> = new Map();
    private connectionLineElements: Map<string, SVGLineElement> = new Map(); // Store connection line references

    // Execution state visualization
    private stateRenderer: NodeStateRenderer = new NodeStateRenderer();
    private metricsOverlay: NodeMetricsOverlay = new NodeMetricsOverlay();
    private gridSystem: GridSystem = new GridSystem(20);
    private minimap: Minimap = new Minimap();
    private iconLibrary: IconLibrary = new IconLibrary();
    private containerRenderer: ContainerRenderer = new ContainerRenderer();
    private portSystem: PortSystem = new PortSystem();
    private connectionManager: ConnectionManager = new ConnectionManager();

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
    private readonly SPACING = 120;
    private readonly START_X = 50;
    private readonly START_Y = 50;
    private readonly PADDING = 20;

    // PluginManager reference for accessing VisualPluginMetadata
    private pluginManager: PluginManager | null = null;

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

    constructor(containerId: string, pluginManager?: PluginManager | null) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.pluginManager = pluginManager || null;
        this.canvasElement = this.createCanvasElement();
        // createCanvasElement() creates wrapper and appends it, so no need to append again

        // Initialize semantic validator if plugin manager provided
        if (pluginManager) {
            this.semanticValidator = new SemanticValidator(pluginManager);
        }

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
        // Use the existing #canvas-area container as scrollable wrapper
        // Create inner content wrapper for transform to prevent container from floating
        const contentWrapper = document.createElement('div');
        contentWrapper.className = 'canvas-content-wrapper';
        contentWrapper.style.position = 'relative';
        contentWrapper.style.width = '100%';
        contentWrapper.style.minHeight = '100%';
        // This wrapper will receive transform, not the scrollable container

        // Append wrapper to container
        this.container.appendChild(contentWrapper);

        // Return the wrapper - we'll render into it and apply transform to it
        return contentWrapper;
    }

    /**
     * Load nodes (replaces existing)
     */
    loadNodes(nodes: VisualNode[]): void {
        this.nodes = this.cloneNodes(nodes);
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
        this.canvasElement.innerHTML = '';

        // Create SVG overlay for connections
        this.svgElement = this.createSVGOverlay();
        this.canvasElement.appendChild(this.svgElement);

        // Calculate default positions for root-level nodes
        const linearNodes = this.nodes.filter(n => !isContainerNode(n.type) || n.children.length === 0);

        if (linearNodes.length > 0) {
            const positions = calculateLinearLayout(linearNodes, {
                startX: this.START_X,
                startY: this.START_Y,
                spacing: this.SPACING
            });

            // Only assign a layout position if the node does not already have one
            linearNodes.forEach(node => {
                if (!node.position && positions[node.id]) {
                    node.position = positions[node.id];
                }
            });
        }

        // Generate connections for linear sequences
        if (linearNodes.length > 1) {
            this.connections = generateConnections(linearNodes);
        } else {
            this.connections = [];
        }

        // Render nodes
        this.nodes.forEach(node => this.renderNodeWithStrategy(node));

        // Render connections
        this.renderConnections();

        // Render minimap if enabled
        if (this.minimapVisible) {
            this.minimap.render(this.canvasElement, this.nodes);

            // Setup navigation callback
            this.minimap.onNavigate((position) => {
                this.handleMinimapClick(position);
            });
        }

        this.highlightSelectedNodes();
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
    private renderNodeWithStrategy(node: VisualNode): void {
        const strategy = getRenderingStrategy(node);

        if (strategy === 'nested') {
            this.renderNestedNode(node);
        } else {
            this.renderConnectedNode(node);
        }
    }

    /**
     * Render nested container node using ContainerRenderer (FULL SCALE!)
     */
    private renderNestedNode(node: VisualNode): void {
        // Calculate nested layout for container + children
        const positions = calculateNestedLayout(node, {
            x: node.position?.x || this.START_X,
            y: node.position?.y || this.START_Y,
            padding: this.PADDING
        });

        // Apply positions only when they are not already defined (preserve manual placement)
        if (!node.position && positions[node.id]) {
            node.position = positions[node.id];
        }

        node.children.forEach(child => {
            if (!child.position && positions[child.id]) {
                child.position = positions[child.id];
            }
        });

        // Render container using ContainerRenderer (FULL SCALE!)
        const container = document.createElement('div');
        container.className = 'visual-node nested-container';
        container.dataset.nodeId = node.id;
        const containerX = node.position?.x ?? this.START_X;
        const containerY = node.position?.y ?? this.START_Y;

        container.style.position = 'absolute';
        container.style.left = `${containerX}px`;
        container.style.top = `${containerY}px`;
        container.style.zIndex = '2';

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
            const childrenArea = container.querySelector('.container-children');
            if (childrenArea) {
                // Clear default simple children
                childrenArea.innerHTML = '';

                // Render children using our createBasicNodeElement logic
                // This ensures children also get ports and event handlers
                node.children.forEach((child, index) => {
                    const childElement = this.createBasicNodeElement(child);
                    // Mark as child element and store container ID
                    childElement.dataset.parentId = node.id;
                    childElement.dataset.childIndex = index.toString();

                    // Add reorder buttons for children
                    this.addReorderButtons(childElement, node.id, child.id, index, node.children.length);

                    // Children are rendered inside container's children area
                    childrenArea.appendChild(childElement);
                });
            } else {
                console.warn(`[VisualDesigner] Container children area not found for node ${node.id}`);
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

        this.canvasElement.appendChild(container);
    }

    /**
     * Render connected action node
     */
    private renderConnectedNode(node: VisualNode): void {
        const nodeElement = this.createBasicNodeElement(node);
        const nodeX = node.position?.x ?? this.START_X;
        const nodeY = node.position?.y ?? this.START_Y;

        nodeElement.style.position = 'absolute';
        nodeElement.style.left = `${nodeX}px`;
        nodeElement.style.top = `${nodeY}px`;
        nodeElement.style.zIndex = '2';

        this.canvasElement.appendChild(nodeElement);
    }

    /**
     * Get node dimensions from VisualPluginMetadata or use defaults
     */
    private getNodeDimensions(nodeType: string): { width: number; height: number } {
        if (this.pluginManager) {
            const plugin = this.pluginManager.findPlugin(nodeType);
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
        const dimensions = this.getNodeDimensions(node.type);
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
                const fromNode = this.findNodeRecursive(connection.from, this.nodes);
                const toNode = this.findNodeRecursive(connection.to, this.nodes);

                if (!fromNode || !toNode || !fromNode.position || !toNode.position) {
                    console.warn(`[VisualDesigner] Cannot render connection: nodes not found or positioned`, {
                        from: connection.from,
                        to: connection.to,
                        fromNode: fromNode ? 'found' : 'missing',
                        toNode: toNode ? 'found' : 'missing'
                    });
                    return;
                }

                // Calculate connection points using node dimensions from metadata
                const fromDimensions = this.getNodeDimensions(fromNode.type);
                const toDimensions = this.getNodeDimensions(toNode.type);
                const x1 = fromNode.position.x + fromDimensions.width / 2;
                const y1 = fromNode.position.y + fromDimensions.height;
                const x2 = toNode.position.x + toDimensions.width / 2;
                const y2 = toNode.position.y;

                // Determine connection ID (manual connections have id, automatic ones need generated id)
                const connectionId = (connection.id && typeof connection.id === 'string')
                    ? connection.id
                    : `auto-${connection.from}-${connection.to}`;
                const isSelected = this.selectedConnectionId === connectionId;
                const isManual = connectionId.startsWith('conn-');

                this.createArrow(x1, y1, x2, y2, connection.type, connectionId, isSelected, isManual);
            } catch (error) {
                console.error(`[VisualDesigner] Error rendering connection:`, error, connection);
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

                        // Position will be calculated by calculateNestedLayout
                        newNode.position = undefined; // Let layout algorithm handle it

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
                                    // Remove all drop zone classes
                                    this.canvasElement.querySelectorAll('.drop-target, .drop-zone-valid, .drop-zone-invalid').forEach(el => {
                                        el.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');
                                    });
                                    return; // Reject the drop
                                }
                            }

                            // Drop into container - add as child

                            // Remove drop-target highlight
                            containerNodeElement.classList.remove('drop-target', 'drop-zone-valid', 'drop-zone-invalid');

                            // Position will be calculated by calculateNestedLayout
                            newNode.position = undefined; // Let layout algorithm handle it

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
            // Calculate offset in canvas coordinates (accounting for transform)
            const canvasPos = this.viewportToCanvas(e.clientX, e.clientY);
            startX = canvasPos.x;
            startY = canvasPos.y;
            // Offset is the distance from mouse to node's current position (in canvas coords)
            offsetX = canvasPos.x - (node.position?.x || 0);
            offsetY = canvasPos.y - (node.position?.y || 0);

            nodeElement.style.cursor = 'grabbing';
            e.preventDefault();
        });

        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;

            // Convert viewport coordinates to canvas coordinates (accounting for transform)
            const canvasPos = this.viewportToCanvas(e.clientX, e.clientY);
            const newX = canvasPos.x - offsetX;
            const newY = canvasPos.y - offsetY;

            this.moveNode(node.id, { x: newX, y: newY });
        });

        document.addEventListener('mouseup', () => {
            if (isDragging) {
                isDragging = false;
                nodeElement.style.cursor = 'pointer';
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

                // Start connection drag
                this.connectionDragState = {
                    fromNodeId: node.id,
                    fromPortElement: port,
                    fromPortType: portType,
                    temporaryLine: null
                };

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
                    line.setAttribute('stroke', 'var(--magenta)');
                    line.setAttribute('stroke-width', '2');
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

                // Create connection
                const validation = this.connectionManager.validateConnection(
                    this.connectionDragState.fromNodeId,
                    node.id
                );

                if (!validation.valid) {
                    console.warn(`⚠️ [VisualDesigner] Connection validation failed: ${validation.error}`);
                    this.cancelConnectionDrag();
                    return;
                }

                try {
                    this.createManualConnection(
                        this.connectionDragState.fromNodeId,
                        node.id,
                        (this.connectionDragState.fromPortType as ConnectionType) || 'sequential'
                    );
                } catch (error) {
                    console.warn('Failed to create manual connection:', error);
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
                },
                { type: 'divider' }
            );
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
            top: 8px;
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
    moveNode(nodeId: string, position: { x: number; y: number }): boolean {
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

        // If node has children, move them too maintaining relative positions
        if (node.children && node.children.length > 0 && (offsetX !== 0 || offsetY !== 0)) {
            this.moveNodeChildren(node, offsetX, offsetY);
        }

        // Re-render
        this.render();

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

        this.moveNode(dragInfo.nodeId, { x: newX, y: newY });
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
            const dimensions = this.getNodeDimensions(node.type);
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

        // Highlight selected nodes
        this.selectedNodeIds.forEach(nodeId => {
            const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
            if (nodeElement) {
                nodeElement.classList.add('node-selected');
            }
        });
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

        this.connectionDragState = {
            fromNodeId,
            fromPortElement: portElement,
            fromPortType: portElement.dataset.portConnectionType || 'sequential',
            temporaryLine: null
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
     */
    generateXML(): string {
        const generator = new XMLGenerator();
        // prepareNodesForExport() already applies connection-based ordering
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

    private prepareNodesForExport(): VisualNode[] {
        const connections: ManualConnection[] = this.connectionManager.getConnections().map(conn => ({ ...conn }));
        const cloned = this.cloneNodes(this.nodes);
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
        this.semanticValidator = new SemanticValidator(pluginManager);
        console.log('✅ [VisualDesigner] Semantic validator initialized');
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
