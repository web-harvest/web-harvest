/**
 * D3ConnectionRenderer - Renderer połączeń używający D3.js (jak Apache NiFi)
 * 
 * Inspirowane implementacją z Apache NiFi (connection-manager.service.ts)
 * 
 * ZASADY:
 * 1. Używa D3.js line generator (jak NiFi)
 * 2. Renderuj TYLKO na podstawie punktów z modelu semantycznego
 * 3. Proste linie (d3.curveLinear) - nie bezier curves
 */

import * as d3 from 'd3';
import type { VisualConnectionState, Point } from './VisualSemanticModel';
import type { IConnectionRenderer } from './IConnectionRenderer';

export class D3ConnectionRenderer implements IConnectionRenderer {
    public svgElement: SVGSVGElement;
    private renderedPaths: Map<string, SVGPathElement> = new Map();
    private renderedArrows: Map<string, SVGPolygonElement> = new Map();
    private lineGenerator: d3.Line<Point>;
    private readonly STROKE_WIDTH = 2.5;
    private readonly ARROW_SIZE = 8;

    constructor(svgElement: SVGSVGElement) {
        this.svgElement = svgElement;
        
        // D3 line generator (jak w NiFi)
        this.lineGenerator = d3
            .line<Point>()
            .x(function (d: Point) {
                return d.x;
            })
            .y(function (d: Point) {
                return d.y;
            })
            .curve(d3.curveLinear); // Proste linie, nie bezier (jak NiFi)
    }

    /**
     * Update SVG element reference
     */
    updateSvgElement(newSvgElement: SVGSVGElement): void {
        this.svgElement = newSvgElement;
    }

    /**
     * Renderuj wszystkie połączenia z modelu semantycznego
     */
    renderAll(modelManager: any): void {
        const model = modelManager.getModel();
        if (!model) {
            console.error('[D3ConnectionRenderer] renderAll: Model is null');
            return;
        }

        const connections = model.workflow.connections;
        console.log(`[D3ConnectionRenderer] renderAll: ${connections.size} connections to render`);

        // Usuń stare
        this.clearAll();

        // Renderuj każde połączenie
        let renderedCount = 0;
        let skippedCount = 0;

        connections.forEach((conn: VisualConnectionState) => {
            if (!conn.path || !conn.path.points || conn.path.points.length < 2) {
                console.warn(`[D3ConnectionRenderer] Skipping connection ${conn.connectionId} - invalid path`);
                skippedCount++;
                return;
            }

            // Renderuj path
            const path = this.createPath(conn);
            if (path) {
                this.svgElement.appendChild(path);
                this.renderedPaths.set(conn.connectionId, path);
                renderedCount++;
            }

            // Renderuj arrow
            const arrow = this.createArrow(conn);
            if (arrow) {
                this.svgElement.appendChild(arrow);
                this.renderedArrows.set(conn.connectionId, arrow);
            }
        });

        console.log(`[D3ConnectionRenderer] renderAll: DONE - rendered ${renderedCount}, skipped ${skippedCount}`);
    }

    /**
     * Utwórz SVG path element używając D3.js line generator (jak NiFi)
     */
    private createPath(conn: VisualConnectionState): SVGPathElement | null {
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        const points = conn.path.points;

        if (!points || points.length < 2) {
            return null;
        }

        // Użyj D3 line generator (jak NiFi)
        const pathData = this.lineGenerator(points);

        if (!pathData) {
            console.error(`[D3ConnectionRenderer] Failed to generate path data for ${conn.connectionId}`);
            return null;
        }

        // Ustaw atrybuty (jak NiFi)
        path.setAttribute('d', pathData);
        path.setAttribute('class', 'connection-path');
        path.setAttribute('data-connection-id', conn.connectionId);
        path.setAttribute('data-from-node', conn.fromNodeId);
        path.setAttribute('data-to-node', conn.toNodeId);
        path.setAttribute('fill', 'none');
        path.setAttribute('stroke', '#64748b'); // slate-500
        path.setAttribute('stroke-width', String(this.STROKE_WIDTH));
        path.setAttribute('stroke-linejoin', 'round');
        path.setAttribute('stroke-linecap', 'round');
        path.setAttribute('shape-rendering', 'geometricPrecision');
        path.setAttribute('vector-effect', 'non-scaling-stroke');
        path.setAttribute('opacity', '1');
        path.style.pointerEvents = 'stroke';
        path.style.cursor = 'pointer';

        return path;
    }

    /**
     * Utwórz strzałkę na końcu połączenia
     */
    private createArrow(conn: VisualConnectionState): SVGPolygonElement | null {
        const arrow = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
        const points = conn.path.points;

        if (!points || points.length < 2) {
            return null;
        }

        // Ostatni punkt to koniec strzałki
        const tip = points[points.length - 1];
        const beforeTip = points[points.length - 2];

        // Oblicz kąt
        const dx = tip.x - beforeTip.x;
        const dy = tip.y - beforeTip.y;
        const angle = Math.atan2(dy, dx);

        // Utwórz trójkąt strzałki
        const arrowPoints = [
            { x: tip.x, y: tip.y },
            {
                x: tip.x - this.ARROW_SIZE * Math.cos(angle) - this.ARROW_SIZE * 0.5 * Math.sin(angle),
                y: tip.y - this.ARROW_SIZE * Math.sin(angle) + this.ARROW_SIZE * 0.5 * Math.cos(angle)
            },
            {
                x: tip.x - this.ARROW_SIZE * Math.cos(angle) + this.ARROW_SIZE * 0.5 * Math.sin(angle),
                y: tip.y - this.ARROW_SIZE * Math.sin(angle) - this.ARROW_SIZE * 0.5 * Math.cos(angle)
            }
        ];

        arrow.setAttribute('points', arrowPoints.map(p => `${p.x},${p.y}`).join(' '));
        arrow.setAttribute('class', 'connection-arrow');
        arrow.setAttribute('data-connection-id', conn.connectionId);
        arrow.setAttribute('fill', '#64748b');
        arrow.setAttribute('stroke', '#64748b');
        arrow.setAttribute('stroke-width', '1');
        arrow.setAttribute('opacity', '1');

        return arrow;
    }

    /**
     * Usuń wszystkie połączenia
     */
    clearAll(): void {
        this.renderedPaths.forEach(path => path.remove());
        this.renderedArrows.forEach(arrow => arrow.remove());
        this.renderedPaths.clear();
        this.renderedArrows.clear();
    }

    /**
     * Renderuj pojedyncze połączenie
     */
    renderConnection(conn: VisualConnectionState): void {
        // Usuń stare jeśli istnieje
        const oldPath = this.renderedPaths.get(conn.connectionId);
        const oldArrow = this.renderedArrows.get(conn.connectionId);
        if (oldPath) oldPath.remove();
        if (oldArrow) oldArrow.remove();

        if (!conn.path || !conn.path.points || conn.path.points.length < 2) {
            return;
        }

        // Renderuj nowe
        const path = this.createPath(conn);
        if (path) {
            this.svgElement.appendChild(path);
            this.renderedPaths.set(conn.connectionId, path);
        }

        const arrow = this.createArrow(conn);
        if (arrow) {
            this.svgElement.appendChild(arrow);
            this.renderedArrows.set(conn.connectionId, arrow);
        }
    }
}

