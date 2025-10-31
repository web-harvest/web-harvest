/**
 * LayoutAlgorithms - Implementation
 * Automatic layout algorithms for workflow nodes
 */

import type { VisualNode } from '../types';

export type LayoutType = 'vertical' | 'horizontal' | 'grid' | 'hierarchical' | 'force-directed';

export interface LayoutOptions {
    spacing?: number;
    startX?: number;
    startY?: number;
    iterations?: number; // for force-directed
}

export interface Position {
    x: number;
    y: number;
}

export class LayoutAlgorithms {
    private spacing: number = 150;
    private readonly NODE_WIDTH = 250;
    private readonly NODE_HEIGHT = 100;

    /**
     * Apply layout algorithm to nodes
     */
    applyLayout(nodes: VisualNode[], layoutType: LayoutType, options: LayoutOptions = {}): VisualNode[] {
        const spacing = options.spacing || this.spacing;
        const startX = options.startX || 50;
        const startY = options.startY || 50;

        switch (layoutType) {
            case 'vertical':
                return this.verticalLayout(nodes, startX, startY, spacing);

            case 'horizontal':
                return this.horizontalLayout(nodes, startX, startY, spacing);

            case 'grid':
                return this.gridLayout(nodes, startX, startY, spacing);

            case 'hierarchical':
                return this.hierarchicalLayout(nodes, startX, startY, spacing);

            case 'force-directed':
                return this.forceDirectedLayout(nodes, options.iterations || 50);

            default:
                return nodes;
        }
    }

    /**
     * Vertical layout - stack nodes top to bottom
     */
    private verticalLayout(nodes: VisualNode[], startX: number, startY: number, spacing: number): VisualNode[] {
        return nodes.map((node, index) => ({
            ...node,
            position: {
                x: startX,
                y: startY + (index * spacing)
            }
        }));
    }

    /**
     * Horizontal layout - arrange nodes left to right
     */
    private horizontalLayout(nodes: VisualNode[], startX: number, startY: number, spacing: number): VisualNode[] {
        return nodes.map((node, index) => ({
            ...node,
            position: {
                x: startX + (index * spacing),
                y: startY
            }
        }));
    }

    /**
     * Grid layout - organize in rows and columns
     */
    private gridLayout(nodes: VisualNode[], startX: number, startY: number, spacing: number): VisualNode[] {
        const { rows, cols } = this.calculateGridDimensions(nodes.length);

        return nodes.map((node, index) => {
            const row = Math.floor(index / cols);
            const col = index % cols;

            return {
                ...node,
                position: {
                    x: startX + (col * spacing),
                    y: startY + (row * spacing)
                }
            };
        });
    }

    /**
     * Calculate optimal grid dimensions
     */
    calculateGridDimensions(nodeCount: number): { rows: number; cols: number } {
        const cols = Math.ceil(Math.sqrt(nodeCount));
        const rows = Math.ceil(nodeCount / cols);

        return { rows, cols };
    }

    /**
     * Hierarchical layout - tree structure for containers
     */
    private hierarchicalLayout(nodes: VisualNode[], startX: number, startY: number, spacing: number): VisualNode[] {
        const positioned: VisualNode[] = [];

        // Process root nodes
        nodes.forEach((node, index) => {
            const nodePosition: Position = {
                x: startX,
                y: startY + (index * spacing)
            };

            const positionedNode: VisualNode = {
                ...node,
                position: nodePosition,
                children: this.layoutChildren(node.children, nodePosition, spacing)
            };

            positioned.push(positionedNode);
        });

        return positioned;
    }

    /**
     * Layout children under parent (recursive)
     */
    private layoutChildren(children: VisualNode[], parentPos: Position, spacing: number): VisualNode[] {
        if (children.length === 0) return [];

        const childSpacing = spacing * 0.8;
        const totalWidth = (children.length - 1) * childSpacing;
        const startX = parentPos.x - totalWidth / 2;
        const childY = parentPos.y + spacing;

        return children.map((child, index) => {
            const childPos: Position = {
                x: startX + (index * childSpacing),
                y: childY
            };

            return {
                ...child,
                position: childPos,
                children: this.layoutChildren(child.children, childPos, spacing)
            };
        });
    }

    /**
     * Force-directed layout - physics-based simulation
     */
    private forceDirectedLayout(nodes: VisualNode[], iterations: number): VisualNode[] {
        // Initialize random positions
        let positioned = nodes.map(node => ({
            ...node,
            position: node.position || {
                x: Math.random() * 800 + 100,
                y: Math.random() * 600 + 100
            },
            velocity: { x: 0, y: 0 }
        }));

        const k = 150; // Optimal distance
        const repulsion = 5000; // Repulsive force strength
        const attraction = 0.01; // Attractive force strength

        // Simulation iterations
        for (let iter = 0; iter < iterations; iter++) {
            const forces: Array<{ x: number; y: number }> = positioned.map(() => ({ x: 0, y: 0 }));

            // Calculate repulsive forces (all pairs)
            for (let i = 0; i < positioned.length; i++) {
                for (let j = i + 1; j < positioned.length; j++) {
                    const dx = positioned[j].position!.x - positioned[i].position!.x;
                    const dy = positioned[j].position!.y - positioned[i].position!.y;
                    const distance = Math.sqrt(dx * dx + dy * dy) || 1;

                    const force = repulsion / (distance * distance);
                    const fx = (dx / distance) * force;
                    const fy = (dy / distance) * force;

                    forces[i].x -= fx;
                    forces[i].y -= fy;
                    forces[j].x += fx;
                    forces[j].y += fy;
                }
            }

            // Apply forces
            positioned = positioned.map((node, i) => ({
                ...node,
                position: {
                    x: node.position!.x + forces[i].x * 0.1,
                    y: node.position!.y + forces[i].y * 0.1
                }
            }));
        }

        return positioned.map(node => ({
            ...node,
            velocity: undefined as any
        }));
    }

    /**
     * Center nodes in canvas
     */
    centerInCanvas(nodes: VisualNode[], canvasWidth: number, canvasHeight: number): VisualNode[] {
        if (nodes.length === 0 || !nodes[0].position) return nodes;

        // Calculate bounding box
        const minX = Math.min(...nodes.map(n => n.position!.x));
        const maxX = Math.max(...nodes.map(n => n.position!.x));
        const minY = Math.min(...nodes.map(n => n.position!.y));
        const maxY = Math.max(...nodes.map(n => n.position!.y));

        const currentCenterX = (minX + maxX) / 2;
        const currentCenterY = (minY + maxY) / 2;

        const targetCenterX = canvasWidth / 2;
        const targetCenterY = canvasHeight / 2;

        const offsetX = targetCenterX - currentCenterX;
        const offsetY = targetCenterY - currentCenterY;

        return nodes.map(node => ({
            ...node,
            position: {
                x: node.position!.x + offsetX,
                y: node.position!.y + offsetY
            }
        }));
    }

    /**
     * Align nodes
     */
    alignNodes(nodes: VisualNode[], alignment: 'left' | 'right' | 'top' | 'bottom' | 'center-vertical' | 'center-horizontal'): VisualNode[] {
        if (nodes.length === 0 || !nodes[0].position) return nodes;

        switch (alignment) {
            case 'left': {
                const minX = Math.min(...nodes.map(n => n.position!.x));
                return nodes.map(node => ({
                    ...node,
                    position: { ...node.position!, x: minX }
                }));
            }

            case 'right': {
                const maxX = Math.max(...nodes.map(n => n.position!.x));
                return nodes.map(node => ({
                    ...node,
                    position: { ...node.position!, x: maxX }
                }));
            }

            case 'top': {
                const minY = Math.min(...nodes.map(n => n.position!.y));
                return nodes.map(node => ({
                    ...node,
                    position: { ...node.position!, y: minY }
                }));
            }

            case 'bottom': {
                const maxY = Math.max(...nodes.map(n => n.position!.y));
                return nodes.map(node => ({
                    ...node,
                    position: { ...node.position!, y: maxY }
                }));
            }

            case 'center-vertical': {
                const avgX = nodes.reduce((sum, n) => sum + n.position!.x, 0) / nodes.length;
                return nodes.map(node => ({
                    ...node,
                    position: { ...node.position!, x: avgX }
                }));
            }

            case 'center-horizontal': {
                const avgY = nodes.reduce((sum, n) => sum + n.position!.y, 0) / nodes.length;
                return nodes.map(node => ({
                    ...node,
                    position: { ...node.position!, y: avgY }
                }));
            }

            default:
                return nodes;
        }
    }

    /**
     * Set default spacing
     */
    setSpacing(spacing: number): void {
        this.spacing = spacing;
        console.log(`[LayoutAlgorithms] Spacing set to ${spacing}px`);
    }

    /**
     * Get current spacing
     */
    getSpacing(): number {
        return this.spacing;
    }

    /**
     * Compact layout - minimize canvas area
     */
    compactLayout(nodes: VisualNode[]): VisualNode[] {
        // Move all nodes to start from (0, 0)
        if (nodes.length === 0 || !nodes[0].position) return nodes;

        const minX = Math.min(...nodes.map(n => n.position!.x));
        const minY = Math.min(...nodes.map(n => n.position!.y));

        return nodes.map(node => ({
            ...node,
            position: {
                x: node.position!.x - minX + 50,
                y: node.position!.y - minY + 50
            }
        }));
    }
}


