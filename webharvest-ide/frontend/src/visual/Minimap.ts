/**
 * Minimap - Implementation
 * Thumbnail view of entire workflow for quick navigation
 */

import type { VisualNode } from '../types';

export interface Position {
    x: number;
    y: number;
}

export class Minimap {
    private container: HTMLElement | null = null;
    private canvas: HTMLElement | null = null;
    private minimapElement: HTMLElement | null = null;
    private viewportIndicator: HTMLElement | null = null;
    private nodes: VisualNode[] = [];
    private visible: boolean = true;
    private scale: number = 0.1;
    private onNavigateCallback: ((position: Position) => void) | null = null;

    private readonly MINIMAP_WIDTH = 200;
    private readonly MINIMAP_HEIGHT = 150;
    private readonly MIN_CANVAS_SIZE = 500; // Auto-hide below this size

    /**
     * Render minimap
     */
    render(canvas: HTMLElement, nodes: VisualNode[]): void {
        this.canvas = canvas;
        this.nodes = nodes;

        // Check if canvas is large enough (relaxed check - always show if enabled)
        const canvasWidth = canvas.clientWidth || parseInt(canvas.style.width) || 1000;
        const canvasHeight = canvas.clientHeight || parseInt(canvas.style.height) || 800;

        // Always render if explicitly requested (skip size check)
        // if (canvasWidth < this.MIN_CANVAS_SIZE || canvasHeight < this.MIN_CANVAS_SIZE) {
        //     this.visible = false;
        //     return;
        // }

        // Remove existing minimap
        const existing = canvas.querySelector('.minimap');
        if (existing) {
            existing.remove();
        }

        // Create minimap container
        this.minimapElement = document.createElement('div');
        this.minimapElement.className = 'minimap';
        this.minimapElement.style.cssText = `
            position: absolute;
            bottom: 20px;
            right: 20px;
            width: ${this.MINIMAP_WIDTH}px;
            height: ${this.MINIMAP_HEIGHT}px;
            background: rgba(255, 255, 255, 0.95);
            border: 2px solid var(--gray-300);
            border-radius: 8px;
            box-shadow: var(--shadow-lg);
            overflow: hidden;
            z-index: 50;
            cursor: pointer;
        `;

        // Calculate scale
        this.scale = this.calculateScale(canvasWidth, canvasHeight, this.MINIMAP_WIDTH, this.MINIMAP_HEIGHT);

        // Render nodes
        nodes.forEach(node => {
            if (node.position) {
                const nodeElement = this.createMinimapNode(node);
                this.minimapElement!.appendChild(nodeElement);
            }
        });

        // Render viewport indicator
        this.viewportIndicator = this.createViewportIndicator();
        this.minimapElement.appendChild(this.viewportIndicator);

        // Click handler
        this.minimapElement.addEventListener('click', (e) => {
            const rect = this.minimapElement!.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            this.handleMinimapClick({ x, y });
        });

        canvas.appendChild(this.minimapElement);

        console.log(`[Minimap] Rendered with scale ${this.scale.toFixed(3)}`);
    }

    /**
     * Create minimap node representation
     */
    private createMinimapNode(node: VisualNode): HTMLElement {
        const element = document.createElement('div');
        element.className = 'minimap-node';
        element.dataset.nodeId = node.id;
        element.style.cssText = `
            position: absolute;
            left: ${(node.position!.x * this.scale)}px;
            top: ${(node.position!.y * this.scale)}px;
            width: ${250 * this.scale}px;
            height: ${100 * this.scale}px;
            background: var(--gray-400);
            border: 1px solid var(--gray-600);
            border-radius: 2px;
        `;

        return element;
    }

    /**
     * Create viewport indicator
     */
    private createViewportIndicator(): HTMLElement {
        const indicator = document.createElement('div');
        indicator.className = 'minimap-viewport';
        indicator.style.cssText = `
            position: absolute;
            border: 2px solid var(--magenta);
            background: rgba(216, 27, 96, 0.1);
            pointer-events: none;
        `;

        return indicator;
    }

    /**
     * Update viewport indicator position and size
     */
    updateViewport(pan: Position, zoom: number): void {
        if (!this.viewportIndicator || !this.canvas) return;

        const canvasWidth = parseInt(this.canvas.style.width) || this.canvas.clientWidth;
        const canvasHeight = parseInt(this.canvas.style.height) || this.canvas.clientHeight;

        // Calculate viewport size in canvas coordinates
        const viewportWidth = canvasWidth / zoom;
        const viewportHeight = canvasHeight / zoom;

        // Calculate viewport position (pan is negative for scrolling)
        const viewportX = -pan.x / zoom;
        const viewportY = -pan.y / zoom;

        // Scale to minimap coordinates
        this.viewportIndicator.style.left = `${viewportX * this.scale}px`;
        this.viewportIndicator.style.top = `${viewportY * this.scale}px`;
        this.viewportIndicator.style.width = `${viewportWidth * this.scale}px`;
        this.viewportIndicator.style.height = `${viewportHeight * this.scale}px`;
    }

    /**
     * Handle minimap click
     */
    handleMinimapClick(minimapPosition: Position): void {
        const canvasPosition = this.minimapToCanvasPosition(minimapPosition);

        if (this.onNavigateCallback) {
            this.onNavigateCallback(canvasPosition);
        }

        console.log(`[Minimap] Navigate to:`, canvasPosition);
    }

    /**
     * Convert minimap coordinates to canvas coordinates
     */
    minimapToCanvasPosition(minimapPos: Position): Position {
        return {
            x: minimapPos.x / this.scale,
            y: minimapPos.y / this.scale
        };
    }

    /**
     * Calculate minimap scale
     */
    calculateScale(canvasWidth: number, canvasHeight: number, minimapWidth: number, minimapHeight: number): number {
        const scaleX = minimapWidth / canvasWidth;
        const scaleY = minimapHeight / canvasHeight;

        // Use smaller scale to maintain aspect ratio
        return Math.min(scaleX, scaleY);
    }

    /**
     * Show minimap
     */
    show(): void {
        if (this.minimapElement) {
            this.minimapElement.style.display = 'block';
            this.visible = true;
        }
    }

    /**
     * Hide minimap
     */
    hide(): void {
        if (this.minimapElement) {
            this.minimapElement.style.display = 'none';
            this.visible = false;
        }
    }

    /**
     * Check if minimap is visible
     */
    isVisible(): boolean {
        return this.visible;
    }

    /**
     * Update nodes in minimap
     */
    updateNodes(nodes: VisualNode[]): void {
        this.nodes = nodes;

        if (this.minimapElement && this.canvas) {
            // Remove old node representations
            const oldNodes = this.minimapElement.querySelectorAll('.minimap-node');
            oldNodes.forEach(node => node.remove());

            // Add new nodes
            nodes.forEach(node => {
                if (node.position) {
                    const nodeElement = this.createMinimapNode(node);
                    this.minimapElement!.insertBefore(nodeElement, this.viewportIndicator);
                }
            });
        }
    }

    /**
     * Register navigation callback
     */
    onNavigate(callback: (position: Position) => void): void {
        this.onNavigateCallback = callback;
    }

    /**
     * Toggle minimap visibility
     */
    toggle(): void {
        if (this.visible) {
            this.hide();
        } else {
            this.show();
        }
    }

    /**
     * Get minimap dimensions
     */
    getDimensions(): { width: number; height: number } {
        return {
            width: this.MINIMAP_WIDTH,
            height: this.MINIMAP_HEIGHT
        };
    }
}


