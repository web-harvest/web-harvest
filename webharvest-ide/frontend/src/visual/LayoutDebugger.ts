/**
 * LayoutDebugger - Narzędzie do debugowania i wizualizacji układu przestrzennego
 * 
 * Pozwala na:
 * - Wizualizację pozycji węzłów
 * - Wizualizację połączeń (linii)
 * - Wizualizację etykiet
 * - Wykrywanie kolizji
 * - Sprawdzanie bounding boxów
 */

import type { VisualNode } from '../types';
import type { Connection } from './VisualModelHelpers';

export interface LayoutMetrics {
    nodes: NodeMetrics[];
    connections: ConnectionMetrics[];
    labels: LabelMetrics[];
    collisions: CollisionInfo[];
    bounds: BoundingBox;
}

export interface NodeMetrics {
    id: string;
    type: string;
    position: { x: number; y: number };
    dimensions: { width: number; height: number };
    boundingBox: BoundingBox;
    depth: number;
    parentId: string | null;
}

export interface ConnectionMetrics {
    from: string;
    to: string;
    points: Array<{ x: number; y: number }>;
    length: number;
    boundingBox: BoundingBox;
}

export interface LabelMetrics {
    nodeId: string;
    position: { x: number; y: number };
    dimensions: { width: number; height: number };
    boundingBox: BoundingBox;
    text: string;
}

export interface CollisionInfo {
    type: 'node-node' | 'node-label' | 'label-label' | 'connection-node';
    element1: { id: string; type: string };
    element2: { id: string; type: string };
    overlap: BoundingBox;
    severity: 'minor' | 'major' | 'critical';
}

export interface BoundingBox {
    x: number;
    y: number;
    width: number;
    height: number;
}

export class LayoutDebugger {
    private canvas: HTMLElement | null = null;
    private debugOverlay: SVGElement | null = null;
    private isEnabled: boolean = false;
    private metrics: LayoutMetrics | null = null;

    constructor(canvasElement: HTMLElement) {
        this.canvas = canvasElement;
    }

    /**
     * Włącz/wyłącz debugowanie
     */
    setEnabled(enabled: boolean): void {
        this.isEnabled = enabled;
        if (enabled) {
            this.createDebugOverlay();
        } else {
            this.removeDebugOverlay();
        }
    }

    /**
     * Analizuj układ przestrzenny
     */
    analyzeLayout(
        nodes: VisualNode[],
        connections: Connection[],
        getNodeDimensions: (node: VisualNode) => { width: number; height: number },
        getNodeDepth: (node: VisualNode) => number,
        getLabelMetrics: (node: VisualNode) => LabelMetrics | null
    ): LayoutMetrics {
        const nodeMetrics = this.analyzeNodes(nodes, getNodeDimensions, getNodeDepth);
        const connectionMetrics = this.analyzeConnections(connections, nodeMetrics);
        const labelMetrics = this.analyzeLabels(nodes, getLabelMetrics);
        const collisions = this.detectCollisions(nodeMetrics, labelMetrics, connectionMetrics);
        const bounds = this.calculateBounds(nodeMetrics);

        this.metrics = {
            nodes: nodeMetrics,
            connections: connectionMetrics,
            labels: labelMetrics,
            collisions,
            bounds
        };

        if (this.isEnabled) {
            this.renderDebugOverlay(this.metrics);
        }

        return this.metrics;
    }

    /**
     * Analizuj węzły
     */
    private analyzeNodes(
        nodes: VisualNode[],
        getNodeDimensions: (node: VisualNode) => { width: number; height: number },
        getNodeDepth: (node: VisualNode) => number
    ): NodeMetrics[] {
        const metrics: NodeMetrics[] = [];

        const traverse = (nodeList: VisualNode[], parentId: string | null = null) => {
            nodeList.forEach(node => {
                if (!node.position) return;

                const dimensions = getNodeDimensions(node);
                const depth = getNodeDepth(node);
                const boundingBox: BoundingBox = {
                    x: node.position.x,
                    y: node.position.y,
                    width: dimensions.width,
                    height: dimensions.height
                };

                metrics.push({
                    id: node.id,
                    type: node.type,
                    position: { ...node.position },
                    dimensions: { ...dimensions },
                    boundingBox,
                    depth,
                    parentId
                });

                if (node.children && node.children.length > 0) {
                    traverse(node.children, node.id);
                }
            });
        };

        traverse(nodes);
        return metrics;
    }

    /**
     * Analizuj połączenia
     */
    private analyzeConnections(
        connections: Connection[],
        nodeMetrics: NodeMetrics[]
    ): ConnectionMetrics[] {
        const nodeMap = new Map(nodeMetrics.map(n => [n.id, n]));
        const metrics: ConnectionMetrics[] = [];

        connections.forEach(conn => {
            const fromNode = nodeMap.get(conn.from);
            const toNode = nodeMap.get(conn.to);

            if (!fromNode || !toNode) return;

            // Oblicz punkty połączenia (uproszczone - zakładamy prostą linię)
            const points = [
                {
                    x: fromNode.position.x + fromNode.dimensions.width / 2,
                    y: fromNode.position.y + fromNode.dimensions.height / 2
                },
                {
                    x: toNode.position.x + toNode.dimensions.width / 2,
                    y: toNode.position.y + toNode.dimensions.height / 2
                }
            ];

            const length = Math.sqrt(
                Math.pow(points[1].x - points[0].x, 2) +
                Math.pow(points[1].y - points[0].y, 2)
            );

            const boundingBox: BoundingBox = {
                x: Math.min(points[0].x, points[1].x),
                y: Math.min(points[0].y, points[1].y),
                width: Math.abs(points[1].x - points[0].x),
                height: Math.abs(points[1].y - points[0].y)
            };

            metrics.push({
                from: conn.from,
                to: conn.to,
                points,
                length,
                boundingBox
            });
        });

        return metrics;
    }

    /**
     * Analizuj etykiety
     */
    private analyzeLabels(
        nodes: VisualNode[],
        getLabelMetrics: (node: VisualNode) => LabelMetrics | null
    ): LabelMetrics[] {
        const metrics: LabelMetrics[] = [];

        const traverse = (nodeList: VisualNode[]) => {
            nodeList.forEach(node => {
                const labelMetrics = getLabelMetrics(node);
                if (labelMetrics) {
                    metrics.push(labelMetrics);
                }

                if (node.children && node.children.length > 0) {
                    traverse(node.children);
                }
            });
        };

        traverse(nodes);
        return metrics;
    }

    /**
     * Wykryj kolizje
     */
    private detectCollisions(
        nodeMetrics: NodeMetrics[],
        labelMetrics: LabelMetrics[],
        connectionMetrics: ConnectionMetrics[]
    ): CollisionInfo[] {
        const collisions: CollisionInfo[] = [];

        // Kolizje między węzłami
        for (let i = 0; i < nodeMetrics.length; i++) {
            for (let j = i + 1; j < nodeMetrics.length; j++) {
                const overlap = this.calculateOverlap(nodeMetrics[i].boundingBox, nodeMetrics[j].boundingBox);
                if (overlap) {
                    const severity = this.calculateCollisionSeverity(overlap, nodeMetrics[i].boundingBox);
                    collisions.push({
                        type: 'node-node',
                        element1: { id: nodeMetrics[i].id, type: nodeMetrics[i].type },
                        element2: { id: nodeMetrics[j].id, type: nodeMetrics[j].type },
                        overlap,
                        severity
                    });
                }
            }
        }

        // Kolizje między etykietami
        for (let i = 0; i < labelMetrics.length; i++) {
            for (let j = i + 1; j < labelMetrics.length; j++) {
                const overlap = this.calculateOverlap(labelMetrics[i].boundingBox, labelMetrics[j].boundingBox);
                if (overlap) {
                    collisions.push({
                        type: 'label-label',
                        element1: { id: labelMetrics[i].nodeId, type: 'label' },
                        element2: { id: labelMetrics[j].nodeId, type: 'label' },
                        overlap,
                        severity: 'minor'
                    });
                }
            }
        }

        // Kolizje między węzłami a etykietami
        nodeMetrics.forEach(node => {
            labelMetrics.forEach(label => {
                if (label.nodeId === node.id) return; // Ignoruj własną etykietę
                const overlap = this.calculateOverlap(node.boundingBox, label.boundingBox);
                if (overlap) {
                    collisions.push({
                        type: 'node-label',
                        element1: { id: node.id, type: node.type },
                        element2: { id: label.nodeId, type: 'label' },
                        overlap,
                        severity: 'minor'
                    });
                }
            });
        });

        return collisions;
    }

    /**
     * Oblicz overlap między dwoma bounding boxami
     */
    private calculateOverlap(box1: BoundingBox, box2: BoundingBox): BoundingBox | null {
        const left = Math.max(box1.x, box2.x);
        const right = Math.min(box1.x + box1.width, box2.x + box2.width);
        const top = Math.max(box1.y, box2.y);
        const bottom = Math.min(box1.y + box1.height, box2.y + box2.height);

        if (left < right && top < bottom) {
            return {
                x: left,
                y: top,
                width: right - left,
                height: bottom - top
            };
        }

        return null;
    }

    /**
     * Oblicz severity kolizji
     */
    private calculateCollisionSeverity(overlap: BoundingBox, nodeBox: BoundingBox): 'minor' | 'major' | 'critical' {
        const overlapArea = overlap.width * overlap.height;
        const nodeArea = nodeBox.width * nodeBox.height;
        const ratio = overlapArea / nodeArea;

        if (ratio > 0.5) return 'critical';
        if (ratio > 0.2) return 'major';
        return 'minor';
    }

    /**
     * Oblicz całkowite bounds
     */
    private calculateBounds(nodeMetrics: NodeMetrics[]): BoundingBox {
        if (nodeMetrics.length === 0) {
            return { x: 0, y: 0, width: 0, height: 0 };
        }

        let minX = Infinity;
        let minY = Infinity;
        let maxX = -Infinity;
        let maxY = -Infinity;

        nodeMetrics.forEach(node => {
            minX = Math.min(minX, node.boundingBox.x);
            minY = Math.min(minY, node.boundingBox.y);
            maxX = Math.max(maxX, node.boundingBox.x + node.boundingBox.width);
            maxY = Math.max(maxY, node.boundingBox.y + node.boundingBox.height);
        });

        return {
            x: minX,
            y: minY,
            width: maxX - minX,
            height: maxY - minY
        };
    }

    /**
     * Utwórz debug overlay
     */
    private createDebugOverlay(): void {
        if (!this.canvas) return;

        this.removeDebugOverlay();

        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.id = 'layout-debug-overlay';
        svg.style.position = 'absolute';
        svg.style.top = '0';
        svg.style.left = '0';
        svg.style.width = '100%';
        svg.style.height = '100%';
        svg.style.pointerEvents = 'none';
        svg.style.zIndex = '10000';

        this.canvas.appendChild(svg);
        this.debugOverlay = svg;
    }

    /**
     * Usuń debug overlay
     */
    private removeDebugOverlay(): void {
        if (this.debugOverlay && this.debugOverlay.parentNode) {
            this.debugOverlay.parentNode.removeChild(this.debugOverlay);
        }
        this.debugOverlay = null;
    }

    /**
     * Renderuj debug overlay
     */
    private renderDebugOverlay(metrics: LayoutMetrics): void {
        if (!this.debugOverlay) return;

        // Wyczyść poprzednią zawartość
        this.debugOverlay.innerHTML = '';

        // Renderuj bounding boxy węzłów
        metrics.nodes.forEach(node => {
            const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            rect.setAttribute('x', node.boundingBox.x.toString());
            rect.setAttribute('y', node.boundingBox.y.toString());
            rect.setAttribute('width', node.boundingBox.width.toString());
            rect.setAttribute('height', node.boundingBox.height.toString());
            rect.setAttribute('fill', 'none');
            rect.setAttribute('stroke', node.depth === 0 ? '#3b82f6' : '#10b981');
            rect.setAttribute('stroke-width', '1');
            rect.setAttribute('stroke-dasharray', '2,2');
            rect.setAttribute('opacity', '0.5');
            this.debugOverlay!.appendChild(rect);

            // Renderuj pozycję (punkt)
            const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttribute('cx', node.position.x.toString());
            circle.setAttribute('cy', node.position.y.toString());
            circle.setAttribute('r', '3');
            circle.setAttribute('fill', node.depth === 0 ? '#3b82f6' : '#10b981');
            this.debugOverlay!.appendChild(circle);
        });

        // Renderuj połączenia
        metrics.connections.forEach(conn => {
            if (conn.points.length >= 2) {
                const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line.setAttribute('x1', conn.points[0].x.toString());
                line.setAttribute('y1', conn.points[0].y.toString());
                line.setAttribute('x2', conn.points[conn.points.length - 1].x.toString());
                line.setAttribute('y2', conn.points[conn.points.length - 1].y.toString());
                line.setAttribute('stroke', '#ef4444');
                line.setAttribute('stroke-width', '1');
                line.setAttribute('stroke-dasharray', '4,4');
                line.setAttribute('opacity', '0.3');
                this.debugOverlay!.appendChild(line);
            }
        });

        // Renderuj etykiety
        metrics.labels.forEach(label => {
            const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            rect.setAttribute('x', label.boundingBox.x.toString());
            rect.setAttribute('y', label.boundingBox.y.toString());
            rect.setAttribute('width', label.boundingBox.width.toString());
            rect.setAttribute('height', label.boundingBox.height.toString());
            rect.setAttribute('fill', 'none');
            rect.setAttribute('stroke', '#f59e0b');
            rect.setAttribute('stroke-width', '1');
            rect.setAttribute('stroke-dasharray', '1,1');
            rect.setAttribute('opacity', '0.5');
            this.debugOverlay!.appendChild(rect);
        });

        // Renderuj kolizje
        metrics.collisions.forEach(collision => {
            const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            rect.setAttribute('x', collision.overlap.x.toString());
            rect.setAttribute('y', collision.overlap.y.toString());
            rect.setAttribute('width', collision.overlap.width.toString());
            rect.setAttribute('height', collision.overlap.height.toString());
            rect.setAttribute('fill', this.getCollisionColor(collision.severity));
            rect.setAttribute('opacity', '0.3');
            this.debugOverlay!.appendChild(rect);
        });
    }

    /**
     * Pobierz kolor dla severity kolizji
     */
    private getCollisionColor(severity: 'minor' | 'major' | 'critical'): string {
        switch (severity) {
            case 'critical': return '#ef4444';
            case 'major': return '#f59e0b';
            case 'minor': return '#eab308';
            default: return '#6b7280';
        }
    }

    /**
     * Pobierz metryki
     */
    getMetrics(): LayoutMetrics | null {
        return this.metrics;
    }

    /**
     * Wygeneruj raport tekstowy
     */
    generateReport(): string {
        if (!this.metrics) {
            return 'No metrics available';
        }

        const lines: string[] = [];
        lines.push('=== Layout Debug Report ===');
        lines.push('');
        lines.push(`Nodes: ${this.metrics.nodes.length}`);
        lines.push(`Connections: ${this.metrics.connections.length}`);
        lines.push(`Labels: ${this.metrics.labels.length}`);
        lines.push(`Collisions: ${this.metrics.collisions.length}`);
        lines.push('');
        lines.push(`Bounds: x=${this.metrics.bounds.x}, y=${this.metrics.bounds.y}, width=${this.metrics.bounds.width}, height=${this.metrics.bounds.height}`);
        lines.push('');

        if (this.metrics.collisions.length > 0) {
            lines.push('=== Collisions ===');
            this.metrics.collisions.forEach(collision => {
                lines.push(`${collision.type}: ${collision.element1.id} <-> ${collision.element2.id} (${collision.severity})`);
            });
        }

        return lines.join('\n');
    }
}











