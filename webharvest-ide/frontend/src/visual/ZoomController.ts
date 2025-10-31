/**
 * ZoomController - Implementation
 * Enhanced zoom controls with presets, zoom-to-fit, and zoom-to-selection
 */

import type { VisualNode } from '../types';

export interface BoundingBox {
    minX: number;
    minY: number;
    maxX: number;
    maxY: number;
    width: number;
    height: number;
}

export class ZoomController {
    private zoom: number = 1.0;
    private readonly MIN_ZOOM = 0.1;
    private readonly MAX_ZOOM = 3.0;
    private readonly ZOOM_STEP = 1.2;
    private readonly NODE_WIDTH = 250;
    private readonly NODE_HEIGHT = 100;
    private onZoomChangeCallback: ((zoom: number) => void) | null = null;

    private readonly PRESETS: number[] = [0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0];

    /**
     * Set zoom level
     */
    setZoom(zoom: number): void {
        const newZoom = Math.max(this.MIN_ZOOM, Math.min(this.MAX_ZOOM, zoom));

        if (newZoom !== this.zoom) {
            this.zoom = newZoom;

            if (this.onZoomChangeCallback) {
                this.onZoomChangeCallback(this.zoom);
            }

            console.log(`[ZoomController] Zoom set to ${(this.zoom * 100).toFixed(0)}%`);
        }
    }

    /**
     * Get current zoom level
     */
    getZoom(): number {
        return this.zoom;
    }

    /**
     * Zoom in
     */
    zoomIn(): void {
        this.setZoom(this.zoom * this.ZOOM_STEP);
    }

    /**
     * Zoom out
     */
    zoomOut(): void {
        this.setZoom(this.zoom / this.ZOOM_STEP);
    }

    /**
     * Reset zoom to 100%
     */
    reset(): void {
        this.setZoom(1.0);
    }

    /**
     * Get zoom presets
     */
    getPresets(): number[] {
        return [...this.PRESETS];
    }

    /**
     * Set zoom to preset value
     */
    setPreset(preset: number): void {
        this.setZoom(preset);
    }

    /**
     * Zoom to fit all nodes in canvas
     */
    zoomToFit(nodes: VisualNode[], canvasWidth: number, canvasHeight: number, padding: number = 50): number {
        if (nodes.length === 0 || !nodes[0].position) {
            return 1.0;
        }

        const bbox = this.calculateBoundingBox(nodes);

        // Calculate zoom to fit
        const zoomX = (canvasWidth - padding * 2) / bbox.width;
        const zoomY = (canvasHeight - padding * 2) / bbox.height;

        const zoom = Math.min(zoomX, zoomY, this.MAX_ZOOM);
        const clampedZoom = Math.max(zoom, this.MIN_ZOOM);

        this.setZoom(clampedZoom);

        return clampedZoom;
    }

    /**
     * Zoom to fit selected nodes
     */
    zoomToSelection(selectedNodes: VisualNode[], canvasWidth: number, canvasHeight: number, padding: number = 100): number {
        if (selectedNodes.length === 0) {
            return this.zoom;
        }

        return this.zoomToFit(selectedNodes, canvasWidth, canvasHeight, padding);
    }

    /**
     * Calculate bounding box for nodes
     */
    calculateBoundingBox(nodes: VisualNode[]): BoundingBox {
        if (nodes.length === 0 || !nodes[0].position) {
            return {
                minX: 0,
                minY: 0,
                maxX: this.NODE_WIDTH,
                maxY: this.NODE_HEIGHT,
                width: this.NODE_WIDTH,
                height: this.NODE_HEIGHT
            };
        }

        const minX = Math.min(...nodes.map(n => n.position!.x));
        const minY = Math.min(...nodes.map(n => n.position!.y));
        const maxX = Math.max(...nodes.map(n => n.position!.x + this.NODE_WIDTH));
        const maxY = Math.max(...nodes.map(n => n.position!.y + this.NODE_HEIGHT));

        return {
            minX,
            minY,
            maxX,
            maxY,
            width: maxX - minX,
            height: maxY - minY
        };
    }

    /**
     * Calculate center point of selected nodes
     */
    calculateSelectionCenter(selectedNodes: VisualNode[]): { x: number; y: number } {
        if (selectedNodes.length === 0 || !selectedNodes[0].position) {
            return { x: 0, y: 0 };
        }

        const bbox = this.calculateBoundingBox(selectedNodes);

        return {
            x: bbox.minX + bbox.width / 2,
            y: bbox.minY + bbox.height / 2
        };
    }

    /**
     * Register zoom change callback
     */
    onZoomChange(callback: (zoom: number) => void): void {
        this.onZoomChangeCallback = callback;
    }

    /**
     * Get zoom percentage string
     */
    getZoomPercentage(): string {
        return `${(this.zoom * 100).toFixed(0)}%`;
    }

    /**
     * Get min/max zoom limits
     */
    getLimits(): { min: number; max: number } {
        return {
            min: this.MIN_ZOOM,
            max: this.MAX_ZOOM
        };
    }
}


