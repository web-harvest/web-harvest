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
import { ConnectionManager } from './ConnectionManager';

type EventListener = (data: any) => void;

export class VisualDesigner {
    private nodes: VisualNode[] = [];
    private connections: Connection[] = [];
    private container: HTMLElement;
    private canvasElement: HTMLElement;
    private svgElement: SVGSVGElement | null = null;
    private selectedNodeId: string | null = null;
    private selectedNodeIds: string[] = [];
    private eventListeners: Map<string, EventListener[]> = new Map();

    // Execution state visualization
    private stateRenderer: NodeStateRenderer = new NodeStateRenderer();
    private metricsOverlay: NodeMetricsOverlay = new NodeMetricsOverlay();
    private gridSystem: GridSystem = new GridSystem(20);
    private minimap: Minimap = new Minimap();
    private iconLibrary: IconLibrary = new IconLibrary();
    private containerRenderer: ContainerRenderer = new ContainerRenderer();
    private portSystem: PortSystem = new PortSystem();
    private connectionManager: ConnectionManager = new ConnectionManager();

    // Container expand/collapse state
    private collapsedContainers: Set<string> = new Set();

    // Layout constants
    private readonly NODE_WIDTH = 250;
    private readonly NODE_HEIGHT = 100;
    private readonly SPACING = 120;
    private readonly START_X = 50;
    private readonly START_Y = 50;
    private readonly PADDING = 20;

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
    private pan: { x: number; y: number } = { x: 0, y: 0 };
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

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.canvasElement = this.createCanvasElement();
        this.container.appendChild(this.canvasElement);

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

    /**
     * Create canvas element
     */
    private createCanvasElement(): HTMLElement {
        const canvas = document.createElement('div');
        canvas.className = 'visual-canvas';
        canvas.style.width = '100%';
        canvas.style.height = '100%';
        canvas.style.position = 'relative';
        canvas.style.overflow = 'auto';
        canvas.style.background = '#f5f5f5';
        return canvas;
    }

    /**
     * Load nodes (replaces existing)
     */
    loadNodes(nodes: VisualNode[]): void {
        this.nodes = [...nodes];
        this.render();
    }

    /**
     * Add node to canvas
     */
    addNode(node: VisualNode): void {
        this.nodes.push(node);
        this.renderNode(node);
        this.markDirty(); // Track changes
    }

    /**
     * Remove node by ID
     */
    removeNode(nodeId: string): boolean {
        const index = this.nodes.findIndex(n => n.id === nodeId);

        if (index === -1) {
            return false;
        }

        this.nodes.splice(index, 1);

        // Remove from DOM
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`);
        if (nodeElement) {
            nodeElement.remove();
        }

        // Clear selection if this was selected
        if (this.selectedNodeId === nodeId) {
            this.selectedNodeId = null;
        }

        this.markDirty(); // Track changes
        this.emit('nodeRemoved', nodeId);

        return true;
    }

    /**
     * Update node
     */
    updateNode(nodeId: string, updatedNode: VisualNode): boolean {
        const index = this.nodes.findIndex(n => n.id === nodeId);

        if (index === -1) {
            return false;
        }

        this.nodes[index] = updatedNode;

        // Re-render node
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`);
        if (nodeElement) {
            nodeElement.remove();
        }
        this.renderNode(updatedNode);

        this.markDirty(); // Track changes
        this.emit('nodeUpdated', updatedNode);

        return true;
    }

    /**
     * Get all nodes
     */
    getNodes(): VisualNode[] {
        return [...this.nodes]; // Return copy
    }

    /**
     * Find node by ID
     */
    findNode(nodeId: string): VisualNode | null {
        return this.nodes.find(n => n.id === nodeId) || null;
    }

    /**
     * Clear all nodes
     */
    clear(): void {
        this.nodes = [];
        this.canvasElement.innerHTML = '';
        this.selectedNodeId = null;
    }

    /**
     * Select node
     */
    selectNode(nodeId: string): void {
        console.log(`🎯 [VisualDesigner] selectNode() called for: ${nodeId}`);

        // Clear previous selection
        if (this.selectedNodeId) {
            const prevElement = this.canvasElement.querySelector(`[data-node-id="${this.selectedNodeId}"]`);
            if (prevElement) {
                prevElement.classList.remove('selected');
            }
        }

        // Select new node
        this.selectedNodeId = nodeId;
        this.selectedNodeIds = [nodeId]; // Also update multi-select
        this.highlightSelectedNodes();

        // Emit event
        console.log(`📡 [VisualDesigner] Emitting 'nodeSelected' event for: ${nodeId}`);
        this.emit('nodeSelected', nodeId);
        console.log(`✅ [VisualDesigner] selectNode() complete`);
    }

    /**
     * Clear selection
     */
    clearSelection(): void {
        if (this.selectedNodeId) {
            const element = this.canvasElement.querySelector(`[data-node-id="${this.selectedNodeId}"]`);
            if (element) {
                element.classList.remove('selected');
            }
        }
        this.selectedNodeId = null;
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

        // Calculate positions for root-level nodes
        const linearNodes = this.nodes.filter(n => !isContainerNode(n.type) || n.children.length === 0);
        const positions = calculateLinearLayout(linearNodes, {
            startX: this.START_X,
            startY: this.START_Y,
            spacing: this.SPACING
        });

        // Assign positions to nodes
        linearNodes.forEach(node => {
            if (positions[node.id]) {
                node.position = positions[node.id];
            }
        });

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

        // Apply positions
        node.position = positions[node.id];
        node.children.forEach(child => {
            child.position = positions[child.id];
        });

        // Render container using ContainerRenderer (FULL SCALE!)
        const container = document.createElement('div');
        container.className = 'visual-node nested-container';
        container.dataset.nodeId = node.id;
        container.style.position = 'absolute';
        container.style.left = `${node.position.x}px`;
        container.style.top = `${node.position.y}px`;
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
                node.children.forEach(child => {
                    const childElement = this.createBasicNodeElement(child);
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
        nodeElement.style.position = 'absolute';
        nodeElement.style.left = `${node.position?.x || this.START_X}px`;
        nodeElement.style.top = `${node.position?.y || this.START_Y}px`;
        nodeElement.style.zIndex = '2';

        this.canvasElement.appendChild(nodeElement);
    }

    /**
     * Create basic node element (reusable)
     */
    private createBasicNodeElement(node: VisualNode): HTMLElement {
        const nodeElement = document.createElement('div');
        nodeElement.className = 'visual-node connected-node';
        nodeElement.dataset.nodeId = node.id;
        nodeElement.style.width = `${this.NODE_WIDTH}px`;
        nodeElement.style.minHeight = `${this.NODE_HEIGHT}px`;
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
        console.log(`🔌 [VisualDesigner] Added ports to node ${node.id}, type: ${node.type}`);

        // Verify ports were added
        const ports = nodeElement.querySelectorAll('.port');
        console.log(`🔌 [VisualDesigner] Node ${node.id} has ${ports.length} ports rendered`);

        // Setup port event handlers for connections
        this.setupPortHandlers(nodeElement, node);
        console.log(`🔌 [VisualDesigner] Setup port handlers for node ${node.id}`);

        // Setup drag handlers for moving node
        this.setupNodeDragHandlers(nodeElement, node);

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

        // Get all connections: automatic + manual
        const allConnections = [
            ...this.connections, // Automatic connections (from XML parsing)
            ...this.connectionManager.getConnections() // Manual connections (user-created)
        ];

        if (allConnections.length === 0) return;

        allConnections.forEach(connection => {
            const fromNode = this.nodes.find(n => n.id === connection.from);
            const toNode = this.nodes.find(n => n.id === connection.to);

            if (!fromNode || !toNode || !fromNode.position || !toNode.position) {
                return;
            }

            // Calculate connection points
            const x1 = fromNode.position.x + this.NODE_WIDTH / 2;
            const y1 = fromNode.position.y + this.NODE_HEIGHT;
            const x2 = toNode.position.x + this.NODE_WIDTH / 2;
            const y2 = toNode.position.y;

            this.createArrow(x1, y1, x2, y2, connection.type);
        });
    }

    /**
     * Create SVG arrow
     */
    private createArrow(
        x1: number,
        y1: number,
        x2: number,
        y2: number,
        type: 'sequential' | 'conditional' | 'error'
    ): void {
        if (!this.svgElement) return;

        // Determine style
        let stroke = '#333';
        let strokeDasharray = 'none';

        if (type === 'conditional') {
            stroke = '#4CAF50';
            strokeDasharray = '5,5';
        } else if (type === 'error') {
            stroke = '#F44336';
        }

        // Create line
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', x1.toString());
        line.setAttribute('y1', y1.toString());
        line.setAttribute('x2', x2.toString());
        line.setAttribute('y2', y2.toString());
        line.setAttribute('stroke', stroke);
        line.setAttribute('stroke-width', '2');
        line.setAttribute('stroke-dasharray', strokeDasharray);

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

            // Check if dragging over container
            const dropTarget = document.elementFromPoint(e.clientX, e.clientY);
            const containerElement = dropTarget?.closest('.container-children, .container-node');

            if (containerElement) {
                e.dataTransfer!.dropEffect = 'copy';
                // Visual feedback - highlight container
                const containerNodeElement = containerElement.closest('[data-node-id]');
                if (containerNodeElement) {
                    containerNodeElement.classList.add('drop-target');
                }
            } else {
                e.dataTransfer!.dropEffect = 'copy';
                // Remove all drop-target classes
                this.canvasElement.querySelectorAll('.drop-target').forEach(el => {
                    el.classList.remove('drop-target');
                });
            }
        };

        // Remove drop target highlight on drag leave
        this.canvasElement.addEventListener('dragleave', (e) => {
            const dropTarget = document.elementFromPoint(e.clientX, e.clientY);
            if (!dropTarget || !this.canvasElement.contains(dropTarget)) {
                // Remove all drop-target classes
                this.canvasElement.querySelectorAll('.drop-target').forEach(el => {
                    el.classList.remove('drop-target');
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

                // Get drop position relative to canvas
                const canvasRect = this.canvasElement.getBoundingClientRect();
                const dropX = e.clientX - canvasRect.left;
                const dropY = e.clientY - canvasRect.top;

                // Create new node from template
                const newNode: VisualNode = {
                    id: `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
                    type: template.type,
                    attributes: { ...template.defaultAttributes },
                    body: null,
                    children: [],
                    position: {
                        x: dropX - this.NODE_WIDTH / 2, // Center on drop point
                        y: dropY - this.NODE_HEIGHT / 2
                    }
                };

                // Check if drop is over a container node
                const dropTarget = document.elementFromPoint(e.clientX, e.clientY);
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
                            // Drop into container - add as child
                            console.log(`📦 [VisualDesigner] Dropping ${newNode.type} into container ${container.type} (${containerId})`);

                            // Remove drop-target highlight
                            containerNodeElement.classList.remove('drop-target');

                            // Position will be calculated by calculateNestedLayout
                            newNode.position = undefined; // Let layout algorithm handle it

                            this.addChildToContainer(containerId, newNode);
                            return; // Exit early
                        }
                    }
                }

                // Remove all drop-target highlights
                this.canvasElement.querySelectorAll('.drop-target').forEach(el => {
                    el.classList.remove('drop-target');
                });

                // Drop on canvas - add to root level
                console.log(`📄 [VisualDesigner] Dropping ${newNode.type} on canvas (root level)`);
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
                this.showContextMenu(e.clientX, e.clientY, nodeId);
            }
        });

        // Marquee selection (drag on empty canvas)
        this.setupMarqueeSelection();

        // Setup mouse wheel for zoom/pan
        this.canvasElement.addEventListener('wheel', (e) => this.handleMouseWheel(e), { passive: false });

        // Setup canvas-wide mouse move/up for connection drag
        this.canvasElement.addEventListener('mousemove', (e) => {
            if (this.connectionDragState?.temporaryLine) {
                const canvasRect = this.canvasElement.getBoundingClientRect();
                const currentX = e.clientX - canvasRect.left;
                const currentY = e.clientY - canvasRect.top;

                this.connectionDragState.temporaryLine.setAttribute('x2', currentX.toString());
                this.connectionDragState.temporaryLine.setAttribute('y2', currentY.toString());
            }
        });

        this.canvasElement.addEventListener('mouseup', () => {
            // Cancel connection drag if mouse released outside a port
            if (this.connectionDragState) {
                this.cancelConnectionDrag();
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
            const rect = nodeElement.getBoundingClientRect();
            startX = e.clientX;
            startY = e.clientY;
            offsetX = e.clientX - rect.left;
            offsetY = e.clientY - rect.top;

            nodeElement.style.cursor = 'grabbing';
            e.preventDefault();
        });

        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;

            const canvasRect = this.canvasElement.getBoundingClientRect();
            const newX = e.clientX - canvasRect.left - offsetX;
            const newY = e.clientY - canvasRect.top - offsetY;

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
        const outputPorts = nodeElement.querySelectorAll('.port-output');
        console.log(`🔌 [VisualDesigner] setupPortHandlers for ${node.id}: found ${outputPorts.length} output ports`);

        outputPorts.forEach(portElement => {
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
                    const portRect = port.getBoundingClientRect();
                    const canvasRect = this.canvasElement.getBoundingClientRect();
                    const nodeRect = nodeElement.getBoundingClientRect();

                    const startX = nodeRect.left - canvasRect.left + nodeRect.width / 2;
                    const startY = nodeRect.bottom - canvasRect.top;

                    line.setAttribute('x1', startX.toString());
                    line.setAttribute('y1', startY.toString());
                    line.setAttribute('x2', startX.toString());
                    line.setAttribute('y2', startY.toString());
                    line.setAttribute('stroke', 'var(--magenta)');
                    line.setAttribute('stroke-width', '2');
                    line.setAttribute('stroke-dasharray', '5,5');
                    line.style.pointerEvents = 'none';

                    this.svgElement.appendChild(line);
                    this.connectionDragState.temporaryLine = line;
                }

                console.log(`🔗 [VisualDesigner] Started connection drag from ${node.id}`);
            });
        });

        const inputPorts = nodeElement.querySelectorAll('.port-input');

        inputPorts.forEach(portElement => {
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

                // Create connection
                const connection = this.connectionManager.createConnection(
                    this.connectionDragState.fromNodeId,
                    node.id,
                    this.connectionDragState.fromPortType as any,
                    undefined
                );

                console.log(`✅ [VisualDesigner] Created connection: ${this.connectionDragState.fromNodeId} → ${node.id}`);

                // Clear drag state
                this.cancelConnectionDrag();

                // Re-render to show new connection
                this.markDirty();
                this.render();
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

    /**
     * Show context menu for node
     */
    private showContextMenu(x: number, y: number, nodeId: string): void {
        // Remove existing context menu
        const existing = document.querySelector('.node-context-menu');
        if (existing) {
            existing.remove();
        }

        const menu = document.createElement('div');
        menu.className = 'node-context-menu';
        menu.style.cssText = `
            position: fixed;
            top: ${y}px;
            left: ${x}px;
            background: white;
            border: 1px solid var(--gray-300);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-lg);
            z-index: 10000;
            min-width: 180px;
            padding: 4px 0;
        `;

        const items = [
            {
                label: 'Edit Properties', icon: 'fas fa-edit', action: () => {
                    this.selectNode(nodeId);
                    this.emit('nodeSelected', nodeId);
                }
            },
            {
                label: 'Delete', icon: 'fas fa-trash', action: () => {
                    this.removeNode(nodeId);
                    this.render();
                }, danger: true
            },
            { type: 'divider' },
            {
                label: 'Duplicate', icon: 'fas fa-copy', action: () => {
                    // TODO: Implement duplicate
                    console.log('Duplicate not yet implemented');
                }
            },
        ];

        items.forEach(item => {
            if ('type' in item && item.type === 'divider') {
                const divider = document.createElement('div');
                divider.style.cssText = 'height: 1px; background: var(--gray-200); margin: 4px 0;';
                menu.appendChild(divider);
            } else if ('label' in item) {
                const button = document.createElement('button');
                button.style.cssText = `
                    outline: none;
                    border: none;
                    background: none;
                    padding: 8px 16px;
                    width: 100%;
                    text-align: left;
                    cursor: pointer;
                    font-size: 13px;
                    color: ${item.danger ? 'var(--magenta)' : 'var(--text-primary)'};
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    transition: background 0.2s;
                `;
                button.innerHTML = `<i class="${item.icon}"></i> ${item.label}`;
                button.onmouseenter = () => button.style.background = 'var(--gray-100)';
                button.onmouseleave = () => button.style.background = 'none';
                button.onclick = () => {
                    if (item.action) {
                        item.action();
                    }
                    menu.remove();
                };
                menu.appendChild(button);
            }
        });

        document.body.appendChild(menu);

        // Close on click outside
        const closeMenu = (e: MouseEvent) => {
            if (!menu.contains(e.target as Node)) {
                menu.remove();
                document.removeEventListener('click', closeMenu);
            }
        };
        setTimeout(() => document.addEventListener('click', closeMenu), 0);
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

        // Add to children array
        if (!container.children) {
            container.children = [];
        }
        container.children.push(childNode);

        // Mark dirty and re-render
        this.markDirty();
        this.render();

        // Emit event
        this.emit('nodeAdded', childNode);

        console.log(`✅ Added child ${childNode.type} to container ${container.type}`);
        return true;
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
        const node = this.findNode(nodeId);
        if (!node) {
            return false;
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

        // Update position
        node.position = { x, y };

        // Re-render
        this.render();

        return true;
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
        return this.selectedNodeIds;
    }

    /**
     * Select node (with multi-select support)
     */
    selectNodeMulti(nodeId: string, options?: { addToSelection?: boolean }): void {
        if (options?.addToSelection) {
            if (!this.selectedNodeIds.includes(nodeId)) {
                this.selectedNodeIds.push(nodeId);
            }
        } else {
            this.selectedNodeIds = [nodeId];
        }

        this.selectedNodeId = nodeId;
        this.render();
    }

    /**
     * Deselect all nodes
     */
    deselectAll(): void {
        this.selectedNodeIds = [];
        this.selectedNodeId = null;
        this.clearSelection();
    }

    /**
     * Select all nodes
     */
    selectAll(): void {
        this.selectedNodeIds = this.nodes.map(n => n.id);
        this.selectedNodeId = null;
        this.render();
    }

    /**
     * Delete selected nodes
     */
    deleteSelectedNodes(): void {
        this.selectedNodeIds.forEach(nodeId => {
            this.removeNode(nodeId);
        });
        this.selectedNodeIds = [];
        this.render();
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
        this.pan.x = mousePos.x - (mousePos.x - this.pan.x) * zoomFactor;
        this.pan.y = mousePos.y - (mousePos.y - this.pan.y) * zoomFactor;

        this.applyTransform();
    }

    /**
     * Get current pan offset
     */
    getPan(): { x: number; y: number } {
        return { ...this.pan };
    }

    /**
     * Pan canvas by delta
     */
    panBy(delta: { x: number; y: number }): void {
        this.pan.x += delta.x;
        this.pan.y += delta.y;
        this.applyTransform();
    }

    /**
     * Reset view (zoom 100%, pan to 0,0)
     */
    resetView(): void {
        this.zoom = 1.0;
        this.pan = { x: 0, y: 0 };
        this.applyTransform();
    }

    /**
     * Apply zoom + pan transform
     */
    private applyTransform(): void {
        const transform = `translate(${this.pan.x}px, ${this.pan.y}px) scale(${this.zoom})`;
        this.canvasElement.style.transform = transform;
        this.canvasElement.style.transformOrigin = '0 0';
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
        const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
        const cmdKey = isMac ? event.metaKey : event.ctrlKey;

        // Cmd+A - Select All
        if (event.key === 'a' && cmdKey) {
            this.selectedNodeIds = this.nodes.map(n => n.id);
            this.render();
            return;
        }

        // Delete/Backspace - Delete selected
        if (event.key === 'Delete' || event.key === 'Backspace') {
            this.deleteSelectedNodes();
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
        const duplicated: VisualNode[] = [];

        this.selectedNodeIds.forEach(nodeId => {
            const node = this.findNode(nodeId);
            if (node) {
                const newNode: VisualNode = {
                    ...node,
                    id: `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
                    position: node.position ? {
                        x: node.position.x + 30,
                        y: node.position.y + 30
                    } : undefined
                };
                duplicated.push(newNode);
            }
        });

        duplicated.forEach(node => this.addNode(node));
        this.render();
    }

    /**
     * Undo last action
     */
    private undo(): void {
        // TODO: Implement undo stack
        console.log('Undo not yet implemented');
    }

    /**
     * Redo last undone action
     */
    private redo(): void {
        // TODO: Implement redo stack
        console.log('Redo not yet implemented');
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
            const nodeRight = node.position.x + this.NODE_WIDTH;
            const nodeBottom = node.position.y + this.NODE_HEIGHT;
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

    private manualConnections: Array<{
        id: string;
        from: string;
        to: string;
        type: 'sequential' | 'conditional' | 'error';
    }> = [];

    /**
     * Create manual connection
     */
    createManualConnection(fromId: string, toId: string): void {
        if (this.wouldCreateCycle(fromId, toId)) {
            throw new Error('Circular connection detected');
        }

        const connection = {
            id: `conn-${Date.now()}`,
            from: fromId,
            to: toId,
            type: 'sequential' as const
        };

        this.manualConnections.push(connection);
        this.render();
    }

    /**
     * Delete connection
     */
    deleteConnection(connectionId: string): void {
        const index = this.manualConnections.findIndex(c => c.id === connectionId);
        if (index !== -1) {
            this.manualConnections.splice(index, 1);
            this.render();
        }
    }

    /**
     * Get manual connections
     */
    getManualConnections(): Array<any> {
        return [...this.manualConnections];
    }

    /**
     * Check if connection would create cycle
     */
    private wouldCreateCycle(fromId: string, toId: string): boolean {
        // Build adjacency list
        const graph = new Map<string, string[]>();

        this.manualConnections.forEach(conn => {
            if (!graph.has(conn.from)) {
                graph.set(conn.from, []);
            }
            graph.get(conn.from)!.push(conn.to);
        });

        // Add new connection temporarily
        if (!graph.has(fromId)) {
            graph.set(fromId, []);
        }
        graph.get(fromId)!.push(toId);

        // DFS to detect cycle
        const visited = new Set<string>();
        const recStack = new Set<string>();

        const hasCycle = (nodeId: string): boolean => {
            if (recStack.has(nodeId)) {
                return true;
            }
            if (visited.has(nodeId)) {
                return false;
            }

            visited.add(nodeId);
            recStack.add(nodeId);

            const neighbors = graph.get(nodeId) || [];
            for (const neighbor of neighbors) {
                if (hasCycle(neighbor)) {
                    return true;
                }
            }

            recStack.delete(nodeId);
            return false;
        };

        return hasCycle(fromId);
    }

    /**
     * Get execution order based on manual connections
     */
    getExecutionOrder(): string[] {
        // Topological sort
        const graph = new Map<string, string[]>();
        const inDegree = new Map<string, number>();

        // Initialize
        this.nodes.forEach(node => {
            graph.set(node.id, []);
            inDegree.set(node.id, 0);
        });

        // Build graph
        this.manualConnections.forEach(conn => {
            graph.get(conn.from)!.push(conn.to);
            inDegree.set(conn.to, (inDegree.get(conn.to) || 0) + 1);
        });

        // Kahn's algorithm
        const queue: string[] = [];
        const result: string[] = [];

        // Add nodes with no incoming edges
        inDegree.forEach((degree, nodeId) => {
            if (degree === 0) {
                queue.push(nodeId);
            }
        });

        while (queue.length > 0) {
            const nodeId = queue.shift()!;
            result.push(nodeId);

            const neighbors = graph.get(nodeId) || [];
            neighbors.forEach(neighbor => {
                const newDegree = (inDegree.get(neighbor) || 0) - 1;
                inDegree.set(neighbor, newDegree);

                if (newDegree === 0) {
                    queue.push(neighbor);
                }
            });
        }

        return result;
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
        this.pan = {
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
        filename: string | null;
        isDirty: boolean;
    } | null {
        if (this.nodes.length === 0 && !this.currentFilename) {
            return null;
        }

        return {
            nodes: [...this.nodes],
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
        // Emit event for App.ts to handle
        this.emit('saveRequested', this.getCurrentConfiguration());
        return true;
    }

    /**
     * Create new empty configuration
     */
    newConfiguration(): void {
        this.nodes = [];
        this.connections = [];
        this.manualConnections = [];
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
                connections: this.manualConnections,
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
            this.manualConnections = snapshot.connections || [];
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
        // This will use XMLGenerator
        // Placeholder for now
        return '<?xml version="1.0" encoding="UTF-8"?>\n<config></config>';
    }

    /**
     * Save configuration as
     */
    async saveConfigurationAs(filename: string): Promise<boolean> {
        this.currentFilename = filename;
        this.markClean();
        this.emit('saveAsRequested', { filename, nodes: this.nodes });
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
}
