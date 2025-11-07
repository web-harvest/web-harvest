/**
 * NodeResizeHandler - Handles node resizing with drag handles
 * Supports 8 resize handles: 4 corners + 4 edges
 * 
 * Features:
 * - Drag handles on corners and edges
 * - Min/max constraints based on metadata
 * - Snap to grid (optional)
 * - Updates node dimensions and port positions
 */

import type { VisualNode } from '../types';

export type ResizeHandlePosition = 
    | 'nw' | 'n' | 'ne'  // Top: northwest, north, northeast
    | 'w' | 'e'           // Middle: west, east
    | 'sw' | 's' | 'se';  // Bottom: southwest, south, southeast

export interface ResizeConstraints {
    minWidth: number;
    minHeight: number;
    maxWidth?: number;
    maxHeight?: number;
}

export class NodeResizeHandler {
    private resizeHandles: Map<string, HTMLElement> = new Map(); // nodeId -> handle container
    private isResizing: boolean = false;
    private resizeState: {
        nodeId: string;
        handle: ResizeHandlePosition;
        startX: number;
        startY: number;
        startWidth: number;
        startHeight: number;
        startNodeX: number;
        startNodeY: number;
    } | null = null;
    private onResizeCallback: ((nodeId: string, dimensions: { width: number; height: number }) => void) | null = null;
    private constraints: Map<string, ResizeConstraints> = new Map(); // nodeId -> constraints
    private snapToGrid: boolean = false;
    private gridSize: number = 10;

    /**
     * Set resize callback
     */
    setOnResize(callback: (nodeId: string, dimensions: { width: number; height: number }) => void): void {
        this.onResizeCallback = callback;
    }

    /**
     * Set constraints for a node
     */
    setConstraints(nodeId: string, constraints: ResizeConstraints): void {
        this.constraints.set(nodeId, constraints);
    }

    /**
     * Set snap to grid
     */
    setSnapToGrid(enabled: boolean, gridSize: number = 10): void {
        this.snapToGrid = enabled;
        this.gridSize = gridSize;
    }

    /**
     * Add resize handles to a node element
     */
    addResizeHandles(nodeElement: HTMLElement, node: VisualNode): void {
        // Remove existing handles
        this.removeResizeHandles(node.id);

        // Only add handles if node is selected
        // We'll check selection state externally

        // Create handle container
        const handleContainer = document.createElement('div');
        handleContainer.className = 'resize-handles-container';
        handleContainer.dataset.nodeId = node.id;
        handleContainer.style.cssText = `
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            z-index: 100;
        `;

        // Create 8 handles
        const positions: ResizeHandlePosition[] = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w'];
        
        positions.forEach(position => {
            const handle = this.createHandle(position, node.id);
            handleContainer.appendChild(handle);
        });

        nodeElement.appendChild(handleContainer);
        this.resizeHandles.set(node.id, handleContainer);
    }

    /**
     * Create a single resize handle
     */
    private createHandle(position: ResizeHandlePosition, nodeId: string): HTMLElement {
        const handle = document.createElement('div');
        handle.className = `resize-handle resize-handle-${position}`;
        handle.dataset.position = position;
        handle.dataset.nodeId = nodeId;
        handle.style.cssText = `
            position: absolute;
            width: 10px;
            height: 10px;
            background: #4CAF50;
            border: 2px solid white;
            border-radius: 50%;
            cursor: ${this.getCursorForPosition(position)};
            pointer-events: all;
            z-index: 101;
            transition: all 0.1s;
        `;

        // Position the handle
        this.positionHandle(handle, position);

        // Add hover effect
        handle.addEventListener('mouseenter', () => {
            handle.style.background = '#45a049';
            handle.style.transform = 'scale(1.2)';
        });
        handle.addEventListener('mouseleave', () => {
            handle.style.background = '#4CAF50';
            handle.style.transform = 'scale(1)';
        });

        // Add drag handlers
        handle.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.startResize(nodeId, position, e);
        });

        return handle;
    }

    /**
     * Position handle based on corner/edge
     */
    private positionHandle(handle: HTMLElement, position: ResizeHandlePosition): void {
        const offset = -5; // Half of handle size (10px / 2)

        switch (position) {
            case 'nw': // Top-left
                handle.style.top = `${offset}px`;
                handle.style.left = `${offset}px`;
                break;
            case 'n': // Top
                handle.style.top = `${offset}px`;
                handle.style.left = '50%';
                handle.style.transform = 'translateX(-50%)';
                break;
            case 'ne': // Top-right
                handle.style.top = `${offset}px`;
                handle.style.right = `${offset}px`;
                break;
            case 'e': // Right
                handle.style.top = '50%';
                handle.style.right = `${offset}px`;
                handle.style.transform = 'translateY(-50%)';
                break;
            case 'se': // Bottom-right
                handle.style.bottom = `${offset}px`;
                handle.style.right = `${offset}px`;
                break;
            case 's': // Bottom
                handle.style.bottom = `${offset}px`;
                handle.style.left = '50%';
                handle.style.transform = 'translateX(-50%)';
                break;
            case 'sw': // Bottom-left
                handle.style.bottom = `${offset}px`;
                handle.style.left = `${offset}px`;
                break;
            case 'w': // Left
                handle.style.top = '50%';
                handle.style.left = `${offset}px`;
                handle.style.transform = 'translateY(-50%)';
                break;
        }
    }

    /**
     * Get cursor style for handle position
     */
    private getCursorForPosition(position: ResizeHandlePosition): string {
        const cursors: Record<ResizeHandlePosition, string> = {
            'nw': 'nw-resize',
            'n': 'n-resize',
            'ne': 'ne-resize',
            'e': 'e-resize',
            'se': 'se-resize',
            's': 's-resize',
            'sw': 'sw-resize',
            'w': 'w-resize'
        };
        return cursors[position] || 'default';
    }

    /**
     * Start resize operation
     */
    private startResize(nodeId: string, handle: ResizeHandlePosition, e: MouseEvent): void {
        const nodeElement = document.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
        if (!nodeElement) return;

        const currentWidth = nodeElement.offsetWidth;
        const currentHeight = nodeElement.offsetHeight;
        const rect = nodeElement.getBoundingClientRect();

        this.isResizing = true;
        this.resizeState = {
            nodeId,
            handle,
            startX: e.clientX,
            startY: e.clientY,
            startWidth: currentWidth,
            startHeight: currentHeight,
            startNodeX: rect.left,
            startNodeY: rect.top
        };

        // Add global mouse move/up listeners
        document.addEventListener('mousemove', this.handleMouseMove);
        document.addEventListener('mouseup', this.handleMouseUp);

        // Prevent text selection during resize
        document.body.style.userSelect = 'none';
        document.body.style.cursor = this.getCursorForPosition(handle);
    }

    /**
     * Handle mouse move during resize
     */
    private handleMouseMove = (e: MouseEvent): void => {
        if (!this.isResizing || !this.resizeState) return;

        const { nodeId, handle, startX, startY, startWidth, startHeight, startNodeX, startNodeY } = this.resizeState;
        const nodeElement = document.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
        if (!nodeElement) return;

        const deltaX = e.clientX - startX;
        const deltaY = e.clientY - startY;

        let newWidth = startWidth;
        let newHeight = startHeight;
        let newX = startNodeX;
        let newY = startNodeY;

        // Calculate new dimensions and position based on handle
        switch (handle) {
            case 'nw': // Top-left: resize width/height, move position
                newWidth = startWidth - deltaX;
                newHeight = startHeight - deltaY;
                newX = startNodeX + deltaX;
                newY = startNodeY + deltaY;
                break;
            case 'n': // Top: resize height, move Y
                newHeight = startHeight - deltaY;
                newY = startNodeY + deltaY;
                break;
            case 'ne': // Top-right: resize width/height, move Y
                newWidth = startWidth + deltaX;
                newHeight = startHeight - deltaY;
                newY = startNodeY + deltaY;
                break;
            case 'e': // Right: resize width
                newWidth = startWidth + deltaX;
                break;
            case 'se': // Bottom-right: resize width/height
                newWidth = startWidth + deltaX;
                newHeight = startHeight + deltaY;
                break;
            case 's': // Bottom: resize height
                newHeight = startHeight + deltaY;
                break;
            case 'sw': // Bottom-left: resize width/height, move X
                newWidth = startWidth - deltaX;
                newHeight = startHeight + deltaY;
                newX = startNodeX + deltaX;
                break;
            case 'w': // Left: resize width, move X
                newWidth = startWidth - deltaX;
                newX = startNodeX + deltaX;
                break;
        }

        // Apply constraints
        const constraints = this.constraints.get(nodeId);
        if (constraints) {
            newWidth = Math.max(constraints.minWidth, Math.min(newWidth, constraints.maxWidth || Infinity));
            newHeight = Math.max(constraints.minHeight, Math.min(newHeight, constraints.maxHeight || Infinity));
        }

        // Snap to grid
        if (this.snapToGrid) {
            newWidth = Math.round(newWidth / this.gridSize) * this.gridSize;
            newHeight = Math.round(newHeight / this.gridSize) * this.gridSize;
            newX = Math.round(newX / this.gridSize) * this.gridSize;
            newY = Math.round(newY / this.gridSize) * this.gridSize;
        }

        // Apply new dimensions
        nodeElement.style.width = `${newWidth}px`;
        nodeElement.style.height = `${newHeight}px`;

        // Apply new position if handle affects position
        if (handle === 'nw' || handle === 'n' || handle === 'ne' || handle === 'sw' || handle === 'w') {
            const parent = nodeElement.offsetParent as HTMLElement;
            if (parent) {
                const parentRect = parent.getBoundingClientRect();
                const relativeX = newX - parentRect.left;
                const relativeY = newY - parentRect.top;
                nodeElement.style.left = `${relativeX}px`;
                nodeElement.style.top = `${relativeY}px`;
            }
        }
    };

    /**
     * Handle mouse up (end resize)
     */
    private handleMouseUp = (): void => {
        if (!this.isResizing || !this.resizeState) return;

        const { nodeId } = this.resizeState;
        const nodeElement = document.querySelector(`[data-node-id="${nodeId}"]`) as HTMLElement;
        
        if (nodeElement && this.onResizeCallback) {
            const dimensions = {
                width: nodeElement.offsetWidth,
                height: nodeElement.offsetHeight
            };
            this.onResizeCallback(nodeId, dimensions);
        }

        // Cleanup
        this.isResizing = false;
        this.resizeState = null;
        document.removeEventListener('mousemove', this.handleMouseMove);
        document.removeEventListener('mouseup', this.handleMouseUp);
        document.body.style.userSelect = '';
        document.body.style.cursor = '';
    };

    /**
     * Show resize handles for selected nodes
     */
    showHandlesForNodes(nodeIds: string[]): void {
        // Hide all handles first
        this.resizeHandles.forEach(container => {
            container.style.display = 'none';
        });

        // Show handles for selected nodes
        nodeIds.forEach(nodeId => {
            const container = this.resizeHandles.get(nodeId);
            if (container) {
                container.style.display = 'block';
            }
        });
    }

    /**
     * Hide all resize handles
     */
    hideAllHandles(): void {
        this.resizeHandles.forEach(container => {
            container.style.display = 'none';
        });
    }

    /**
     * Remove resize handles for a node
     */
    removeResizeHandles(nodeId: string): void {
        const container = this.resizeHandles.get(nodeId);
        if (container) {
            container.remove();
            this.resizeHandles.delete(nodeId);
        }
    }

    /**
     * Remove all resize handles
     */
    removeAllHandles(): void {
        this.resizeHandles.forEach(container => container.remove());
        this.resizeHandles.clear();
    }
}


