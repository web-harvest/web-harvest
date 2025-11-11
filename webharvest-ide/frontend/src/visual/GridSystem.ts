/**
 * GridSystem - Implementation
 * Grid overlay, snap-to-grid, and smart alignment guides
 */

export interface Position {
    x: number;
    y: number;
}

export interface AlignmentGuide {
    axis: 'horizontal' | 'vertical';
    position: number;
    nodeIds: string[];
}

export class GridSystem {
    private gridSize: number;
    private enabled: boolean = true;
    private canvas: HTMLElement | null = null;
    private gridOverlay: SVGSVGElement | null = null;
    private gridBackground: SVGRectElement | null = null;
    private alignmentGuides: HTMLElement[] = [];
    private snapThreshold: number = 10; // pixels
    private resizeObserver: ResizeObserver | null = null;
    private scrollListener: (() => void) | null = null;

    constructor(gridSize: number = 20) {
        this.gridSize = gridSize;
    }

    /**
     * Render grid overlay on canvas
     */
    render(canvas: HTMLElement): void {
        this.canvas = canvas;

        this.detachListeners();

        // Create SVG grid
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('class', 'grid-overlay');
        svg.style.cssText = `
            position: absolute;
            top: 0;
            left: 0;
            pointer-events: none;
            z-index: 0;
        `;

        // Get canvas dimensions
        const { width, height } = this.computeDimensions(canvas);
        this.applyDimensions(svg, width, height);

        // Create grid pattern
        const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
        const pattern = document.createElementNS('http://www.w3.org/2000/svg', 'pattern');
        const patternId = `grid-pattern-${Date.now()}`;
        pattern.id = patternId;
        pattern.setAttribute('width', this.gridSize.toString());
        pattern.setAttribute('height', this.gridSize.toString());
        pattern.setAttribute('patternUnits', 'userSpaceOnUse');

        // Grid lines
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', `M ${this.gridSize} 0 L 0 0 0 ${this.gridSize}`);
        path.setAttribute('fill', 'none');
        path.setAttribute('stroke', 'rgba(0, 0, 0, 0.05)');
        path.setAttribute('stroke-width', '1');

        pattern.appendChild(path);
        defs.appendChild(pattern);
        svg.appendChild(defs);

        // Apply pattern
        const backgroundRect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        backgroundRect.setAttribute('width', width.toString());
        backgroundRect.setAttribute('height', height.toString());
        backgroundRect.setAttribute('fill', `url(#${patternId})`);
        svg.appendChild(backgroundRect);

        canvas.prepend(svg);
        this.gridOverlay = svg;
        this.gridBackground = backgroundRect;

        if (typeof ResizeObserver !== 'undefined') {
            this.resizeObserver = new ResizeObserver(() => {
                this.updateDimensions();
            });
            this.resizeObserver.observe(canvas);
        } else {
            this.resizeObserver = null;
        }

        this.scrollListener = () => this.updateDimensions();
        canvas.addEventListener('scroll', this.scrollListener, { passive: true });
        window.addEventListener('resize', this.updateDimensions);

    }

    private detachListeners(): void {
        if (this.gridOverlay) {
            this.gridOverlay.remove();
        }
        if (this.resizeObserver && this.canvas) {
            this.resizeObserver.disconnect();
            this.resizeObserver = null;
        }
        if (this.canvas && this.scrollListener) {
            this.canvas.removeEventListener('scroll', this.scrollListener);
        }
        window.removeEventListener('resize', this.updateDimensions);
        this.scrollListener = null;
        this.gridOverlay = null;
        this.gridBackground = null;
    }

    private computeDimensions(canvas: HTMLElement): { width: number; height: number } {
        const rect = canvas.getBoundingClientRect();
        const width = Math.max(canvas.scrollWidth, rect.width, 2000);
        const height = Math.max(canvas.scrollHeight, rect.height, 2000);
        return { width, height };
    }

    private applyDimensions(svg: SVGSVGElement, width: number, height: number): void {
        svg.setAttribute('width', width.toString());
        svg.setAttribute('height', height.toString());
        svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
        svg.style.width = `${width}px`;
        svg.style.height = `${height}px`;
    }

    updateDimensions = (): void => {
        if (!this.canvas || !this.gridOverlay) {
            return;
        }

        const { width, height } = this.computeDimensions(this.canvas);
        this.applyDimensions(this.gridOverlay, width, height);
        if (this.gridBackground) {
            this.gridBackground.setAttribute('width', width.toString());
            this.gridBackground.setAttribute('height', height.toString());
        }
    };

    /**
     * Snap coordinates to grid
     */
    snapToGrid(position: Position): Position {
        if (!this.enabled) {
            return position;
        }

        return {
            x: Math.round(position.x / this.gridSize) * this.gridSize,
            y: Math.round(position.y / this.gridSize) * this.gridSize
        };
    }

    /**
     * Check if grid is enabled
     */
    isEnabled(): boolean {
        return this.enabled;
    }

    /**
     * Enable grid
     */
    enable(): void {
        this.enabled = true;
        if (this.gridOverlay) {
            this.gridOverlay.style.display = 'block';
        }
        console.log('[GridSystem] Enabled');
    }

    /**
     * Disable grid
     */
    disable(): void {
        this.enabled = false;
        if (this.gridOverlay) {
            this.gridOverlay.style.display = 'none';
        }
        console.log('[GridSystem] Disabled');
    }

    /**
     * Hide grid (without disabling snap)
     */
    hide(): void {
        if (this.gridOverlay) {
            this.gridOverlay.style.display = 'none';
        }
    }

    /**
     * Show grid
     */
    show(): void {
        if (this.gridOverlay) {
            this.gridOverlay.style.display = 'block';
        }
    }

    /**
     * Get grid size
     */
    getGridSize(): number {
        return this.gridSize;
    }

    /**
     * Set grid size
     */
    setGridSize(size: number): void {
        this.gridSize = size;

        if (this.canvas) {
            this.render(this.canvas);
        }

        console.log(`[GridSystem] Grid size set to ${size}px`);
    }

    /**
     * Show alignment guide
     */
    showAlignmentGuide(canvas: HTMLElement, position: Position, axis: 'horizontal' | 'vertical' = 'vertical'): HTMLElement {
        const guide = document.createElement('div');
        guide.className = 'alignment-guide';
        guide.style.cssText = `
            position: absolute;
            background: var(--magenta);
            opacity: 0.5;
            pointer-events: none;
            z-index: 100;
        `;

        if (axis === 'vertical') {
            guide.style.left = `${position.x}px`;
            guide.style.top = '0';
            guide.style.width = '1px';
            guide.style.height = '100%';
        } else {
            guide.style.left = '0';
            guide.style.top = `${position.y}px`;
            guide.style.width = '100%';
            guide.style.height = '1px';
        }

        canvas.appendChild(guide);
        this.alignmentGuides.push(guide);

        return guide;
    }

    /**
     * Clear all alignment guides
     */
    clearAlignmentGuides(): void {
        this.alignmentGuides.forEach(guide => guide.remove());
        this.alignmentGuides = [];
    }

    /**
     * Find nearby alignments with other nodes
     */
    findNearbyAlignments(
        position: Position,
        nodes: Array<{ id: string; position?: Position }>,
        threshold: number
    ): AlignmentGuide[] {
        const alignments: AlignmentGuide[] = [];

        nodes.forEach(node => {
            if (!node.position) return;

            // Check horizontal alignment
            if (Math.abs(node.position.y - position.y) < threshold) {
                alignments.push({
                    axis: 'horizontal',
                    position: node.position.y,
                    nodeIds: [node.id]
                });
            }

            // Check vertical alignment
            if (Math.abs(node.position.x - position.x) < threshold) {
                alignments.push({
                    axis: 'vertical',
                    position: node.position.x,
                    nodeIds: [node.id]
                });
            }
        });

        return alignments;
    }

    /**
     * Distribute nodes with equal spacing
     */
    distributeSpacing(
        nodes: Array<{ id: string; position?: Position }>,
        axis: 'horizontal' | 'vertical'
    ): Array<{ id: string; position?: Position }> {
        if (nodes.length < 3) return nodes;

        // Sort nodes by position
        const sorted = [...nodes].sort((a, b) => {
            if (axis === 'horizontal') {
                return (a.position?.x || 0) - (b.position?.x || 0);
            } else {
                return (a.position?.y || 0) - (b.position?.y || 0);
            }
        });

        // Calculate even spacing
        const first = sorted[0].position;
        const last = sorted[sorted.length - 1].position;

        if (!first || !last) return nodes;

        const totalDistance = axis === 'horizontal'
            ? last.x - first.x
            : last.y - first.y;

        const spacing = totalDistance / (sorted.length - 1);

        // Update positions
        sorted.forEach((node, index) => {
            if (index === 0 || index === sorted.length - 1) {
                return; // Keep first and last positions
            }

            if (node.position) {
                if (axis === 'horizontal') {
                    node.position.x = first.x + (spacing * index);
                } else {
                    node.position.y = first.y + (spacing * index);
                }
            }
        });

        return sorted;
    }

    /**
     * Get snap threshold
     */
    getSnapThreshold(): number {
        return this.snapThreshold;
    }

    /**
     * Set snap threshold
     */
    setSnapThreshold(threshold: number): void {
        this.snapThreshold = threshold;
    }

    /**
     * Align nodes to a specific axis value
     */
    alignNodes(
        nodes: Array<{ id: string; position?: Position }>,
        axis: 'horizontal' | 'vertical',
        alignTo: number
    ): Array<{ id: string; position?: Position }> {
        return nodes.map(node => {
            if (!node.position) return node;

            const newPosition = { ...node.position };
            if (axis === 'horizontal') {
                newPosition.y = alignTo;
            } else {
                newPosition.x = alignTo;
            }

            return { ...node, position: newPosition };
        });
    }
}

