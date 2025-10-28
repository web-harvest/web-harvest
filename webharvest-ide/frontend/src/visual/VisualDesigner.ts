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

    // Zoom & pan state
    private zoom: number = 1.0;
    private pan: { x: number; y: number } = { x: 0, y: 0 };
    private readonly MIN_ZOOM = 0.1;
    private readonly MAX_ZOOM = 3.0;

    // Grid snap
    private gridSnapEnabled: boolean = false;
    private gridSize: number = 20;

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
        // Clear previous selection
        if (this.selectedNodeId) {
            const prevElement = this.canvasElement.querySelector(`[data-node-id="${this.selectedNodeId}"]`);
            if (prevElement) {
                prevElement.classList.remove('selected');
            }
        }

        // Select new node
        this.selectedNodeId = nodeId;
        const nodeElement = this.canvasElement.querySelector(`[data-node-id="${nodeId}"]`);
        if (nodeElement) {
            nodeElement.classList.add('selected');
        }

        // Emit event
        this.emit('nodeSelected', nodeId);
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

        // Node header
        const header = document.createElement('div');
        header.style.fontWeight = '600';
        header.style.marginBottom = '8px';
        header.style.fontSize = '14px';
        header.textContent = node.type;
        nodeElement.appendChild(header);

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
     * Render nested container node
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

        // Render container
        const container = document.createElement('div');
        container.className = 'visual-node nested-container';
        container.dataset.nodeId = node.id;
        container.style.position = 'absolute';
        container.style.left = `${node.position.x}px`;
        container.style.top = `${node.position.y}px`;
        container.style.minWidth = `${this.NODE_WIDTH + 80}px`;
        container.style.padding = `${this.PADDING}px`;
        container.style.background = '#f0f7ff';
        container.style.border = '2px solid #2196F3';
        container.style.borderRadius = '8px';
        container.style.zIndex = '2';

        // Header
        const header = document.createElement('div');
        header.style.fontWeight = '600';
        header.style.fontSize = '14px';
        header.style.marginBottom = `${this.PADDING}px`;
        header.style.color = '#2196F3';
        header.textContent = `${node.type.toUpperCase()}`;
        container.appendChild(header);

        // Attributes (compact)
        if (Object.keys(node.attributes).length > 0) {
            const attrs = document.createElement('div');
            attrs.style.fontSize = '11px';
            attrs.style.color = '#666';
            attrs.style.marginBottom = `${this.PADDING}px`;
            Object.entries(node.attributes).slice(0, 2).forEach(([key, value]) => {
                const attr = document.createElement('span');
                attr.textContent = `${key}="${value}" `;
                attrs.appendChild(attr);
            });
            container.appendChild(attrs);
        }

        // Children area
        const childrenArea = document.createElement('div');
        childrenArea.style.borderTop = '1px dashed #2196F3';
        childrenArea.style.paddingTop = `${this.PADDING}px`;
        childrenArea.style.position = 'relative';

        // Render children as connected nodes inside
        node.children.forEach(child => {
            const childElement = this.createBasicNodeElement(child);
            childElement.style.position = 'absolute';
            childElement.style.left = `${child.position!.x - node.position!.x - this.PADDING}px`;
            childElement.style.top = `${child.position!.y - node.position!.y - this.PADDING - 40}px`;
            childrenArea.appendChild(childElement);
        });

        // Calculate container height
        const maxChildY = Math.max(...node.children.map(c => c.position!.y - node.position!.y));
        childrenArea.style.height = `${maxChildY + this.NODE_HEIGHT}px`;

        container.appendChild(childrenArea);
        container.onclick = (e) => {
            e.stopPropagation();
            this.selectNode(node.id);
        };

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

        // Header
        const header = document.createElement('div');
        header.style.fontWeight = '600';
        header.style.marginBottom = '8px';
        header.style.fontSize = '14px';
        header.textContent = node.type;
        nodeElement.appendChild(header);

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

        nodeElement.onclick = (e) => {
            e.stopPropagation();
            this.selectNode(node.id);
        };

        return nodeElement;
    }

    /**
     * Render connections (arrows)
     */
    private renderConnections(): void {
        if (!this.svgElement || this.connections.length === 0) return;

        this.connections.forEach(connection => {
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
        // Allow drop on canvas
        this.canvasElement.ondragover = (e) => {
            e.preventDefault();
            e.dataTransfer!.dropEffect = 'copy';
        };

        // Handle drop
        this.canvasElement.ondrop = (e) => {
            e.preventDefault();

            try {
                const templateJSON = e.dataTransfer!.getData('application/json');
                if (!templateJSON) {
                    return;
                }

                const template = JSON.parse(templateJSON);

                // Create new node from template
                const newNode: VisualNode = {
                    id: `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
                    type: template.type,
                    attributes: { ...template.defaultAttributes },
                    body: null,
                    children: [],
                    position: {
                        x: e.offsetX,
                        y: e.offsetY
                    }
                };

                // Add to nodes
                this.addNode(newNode);

                // Emit event
                this.emit('nodeAdded', newNode);

                console.log('✅ Added node from palette:', newNode.type);
            } catch (error) {
                console.error('Failed to create node from drop:', error);
            }
        };
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
        this.render();

        return selected;
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

    /**
     * Show context menu for node
     */
    showContextMenu(nodeId: string, position: { x: number; y: number }): {
        position: { x: number; y: number };
        items: any[];
    } | null {
        const node = this.findNode(nodeId);
        if (!node) {
            return null;
        }

        const menu = {
            position,
            items: [
                {
                    label: 'Delete',
                    shortcut: '⌫',
                    action: () => {
                        this.removeNode(nodeId);
                        this.render();
                    }
                },
                {
                    label: 'Duplicate',
                    shortcut: '⌘D',
                    action: () => {
                        this.selectNodeMulti(nodeId);
                        this.duplicateSelectedNodes();
                    }
                },
                {
                    label: 'Copy',
                    shortcut: '⌘C',
                    action: () => {
                        // TODO: Implement clipboard
                        console.log('Copy not yet implemented');
                    }
                },
                {
                    label: 'Paste',
                    shortcut: '⌘V',
                    action: () => {
                        // TODO: Implement clipboard
                        console.log('Paste not yet implemented');
                    }
                },
                { type: 'separator' },
                {
                    label: 'Edit Properties',
                    action: () => {
                        this.selectNode(nodeId);
                    }
                },
                { type: 'separator' },
                {
                    label: 'Bring to Front',
                    action: () => {
                        // Move node to end of array
                        const index = this.nodes.findIndex(n => n.id === nodeId);
                        if (index !== -1) {
                            const [node] = this.nodes.splice(index, 1);
                            this.nodes.push(node);
                            this.render();
                        }
                    }
                },
                {
                    label: 'Send to Back',
                    action: () => {
                        // Move node to start of array
                        const index = this.nodes.findIndex(n => n.id === nodeId);
                        if (index !== -1) {
                            const [node] = this.nodes.splice(index, 1);
                            this.nodes.unshift(node);
                            this.render();
                        }
                    }
                }
            ]
        };

        return menu;
    }

    // ========================================================================
    // Manual Connections
    // ========================================================================

    private manualConnections: Array<{
        id: string;
        from: string;
        to: string;
        type: 'sequential' | 'conditional' | 'error';
    }> = [];

    private connectionDragState: {
        fromNodeId: string;
        port: 'output' | 'input';
    } | null = null;

    /**
     * Start connection drag
     */
    startConnectionDrag(nodeId: string, port: 'output' | 'input'): void {
        this.connectionDragState = {
            fromNodeId: nodeId,
            port
        };
    }

    /**
     * End connection drag
     */
    endConnectionDrag(toNodeId: string, port: 'output' | 'input'): {
        id: string;
        from: string;
        to: string;
        type: string;
    } | null {
        if (!this.connectionDragState) {
            return null;
        }

        const fromNodeId = this.connectionDragState.fromNodeId;

        // Validate: output → input only
        if (this.connectionDragState.port !== 'output' || port !== 'input') {
            this.connectionDragState = null;
            return null;
        }

        // Check for circular reference
        if (this.wouldCreateCycle(fromNodeId, toNodeId)) {
            console.warn('Circular connection detected');
            this.connectionDragState = null;
            throw new Error('Circular connection detected');
        }

        const connection = {
            id: `conn-${Date.now()}`,
            from: fromNodeId,
            to: toNodeId,
            type: 'sequential' as const
        };

        this.manualConnections.push(connection);
        this.connectionDragState = null;
        this.render();

        return connection;
    }

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
        // TODO: Render minimap
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
        // Convert minimap coordinates to canvas coordinates
        // TODO: Implement minimap navigation
        this.pan = {
            x: -position.x,
            y: -position.y
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
     * Show dialog modal (to be implemented by App.ts)
     */
    private async showDialogModal(dialog: any): Promise<string> {
        // This will be implemented in App.ts
        // For now, use window.confirm as fallback
        const confirmed = window.confirm(
            `${dialog.title}\n\n${dialog.message}\n\nClick OK to discard changes, Cancel to keep editing.`
        );
        return confirmed ? 'discard' : 'cancel';
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
}
