/**
 * VisualDesigner - Implementation (TDD Green Phase)
 * Core visual designer for canvas-based workflow editing
 */

import type { VisualNode } from '../types';

type EventListener = (data: any) => void;

export class VisualDesigner {
    private nodes: VisualNode[] = [];
    private container: HTMLElement;
    private canvasElement: HTMLElement;
    private selectedNodeId: string | null = null;
    private eventListeners: Map<string, EventListener[]> = new Map();

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.canvasElement = this.createCanvasElement();
        this.container.appendChild(this.canvasElement);
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
     * Render all nodes
     */
    private render(): void {
        this.canvasElement.innerHTML = '';
        this.nodes.forEach(node => this.renderNode(node));
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
}
