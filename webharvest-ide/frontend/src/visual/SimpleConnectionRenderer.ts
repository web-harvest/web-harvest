/**
 * SimpleConnectionRenderer - PROSTY renderer połączeń
 * 
 * ZASADY:
 * 1. Renderuj TYLKO na podstawie punktów z modelu semantycznego
 * 2. Używaj H (horizontal) i V (vertical) dla orthogonal paths
 * 3. ZERO dodatkowej logiki - tylko renderowanie
 */

import type { VisualConnectionState, Point } from './VisualSemanticModel';
import { snapPointToPixel, getAnchorSvgPosition } from './CoordinateUtils';
import type { IConnectionRenderer } from './IConnectionRenderer';

export class SimpleConnectionRenderer implements IConnectionRenderer {
    public svgElement: SVGSVGElement; // Public for VisualDesigner access
    private renderedPaths: Map<string, SVGPathElement> = new Map();
    private renderedArrows: Map<string, SVGPolygonElement> = new Map();
    private debugOverlay: SVGGElement | null = null; // Debug overlay group
    private readonly CORNER_RADIUS = 8; // Promień zaokrąglenia narożników
    private readonly STROKE_WIDTH = 2.5; // Grubość linii
    private readonly DEBUG_ENABLED = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_CONNECTIONS__);

    constructor(svgElement: SVGSVGElement) {
        this.svgElement = svgElement;
    }

    /**
     * Update SVG element reference
     */
    updateSvgElement(newSvgElement: SVGSVGElement): void {
        this.svgElement = newSvgElement;
    }

    /**
     * Renderuj wszystkie połączenia z modelu semantycznego
     * 
     * CRITICAL: Używa center points (środkowych punktów) do łączenia
     * Na każdej krawędzi są 3 punkty: start, center, end
     * Linie wychodzą/wchodzą TYLKO z/do center points!
     */
    renderAll(modelManager: any): void {
        // Pobierz model z modelManager
        const model = modelManager.getModel();
        if (!model) {
            console.error('[SimpleConnectionRenderer] renderAll: Model is null');
            return;
        }

        const connections = model.workflow.connections;

        console.log(`[SimpleConnectionRenderer] renderAll: START - ${connections.size} connections to render`);
        console.log(`[SimpleConnectionRenderer] SVG element:`, {
            exists: !!this.svgElement,
            inDom: !!this.svgElement?.parentElement,
            width: this.svgElement?.getAttribute('width'),
            height: this.svgElement?.getAttribute('height'),
            viewBox: this.svgElement?.getAttribute('viewBox'),
            style: this.svgElement ? window.getComputedStyle(this.svgElement).zIndex : 'N/A'
        });

        // Usuń stare
        this.clearAll();

        // CRITICAL: Setup debug overlay if enabled
        if (this.DEBUG_ENABLED) {
            this.setupDebugOverlay(modelManager);
        }

        // Renderuj każde połączenie
        let renderedCount = 0;
        let skippedCount = 0;
        connections.forEach((conn: VisualConnectionState) => {
            // CRITICAL: Sprawdź, czy używamy center points
            const fromPoint = conn.anchors?.from;
            const toPoint = conn.anchors?.to;

            if (fromPoint && toPoint) {
                // Sprawdź, czy to są center points (powinny mieć '-center' w ID)
                const fromIsCenter = fromPoint.connectionPointId?.includes('-center') ?? true;
                const toIsCenter = toPoint.connectionPointId?.includes('-center') ?? true;

                if (!fromIsCenter || !toIsCenter) {
                    console.warn(`[SimpleConnectionRenderer] Connection ${conn.connectionId} uses non-center points!`, {
                        fromPointId: fromPoint.connectionPointId,
                        toPointId: toPoint.connectionPointId
                    });
                }
            }

            if (!conn.path || !conn.path.points || conn.path.points.length < 2) {
                console.warn(`[SimpleConnectionRenderer] Skipping connection ${conn.connectionId} - invalid path:`, {
                    hasPath: !!conn.path,
                    pointsCount: conn.path?.points?.length || 0
                });
                skippedCount++;
                return;
            }

            // DEBUG: Sprawdź pierwsze i ostatnie punkty
            const firstPoint = conn.path.points[0];
            const lastPoint = conn.path.points[conn.path.points.length - 1];
            console.log(`[SimpleConnectionRenderer] Rendering connection ${conn.connectionId}:`, {
                fromNode: conn.fromNodeId,
                toNode: conn.toNodeId,
                pointsCount: conn.path.points.length,
                firstPoint: `${firstPoint.x}, ${firstPoint.y}`,
                lastPoint: `${lastPoint.x}, ${lastPoint.y}`
            });

            this.renderConnection(conn);
            renderedCount++;
        });

        console.log(`[SimpleConnectionRenderer] renderAll: END - Rendered ${renderedCount}, Skipped ${skippedCount}`);

        // DIAGNOSTICS: Sprawdź, czy elementy są w SVG
        if (this.svgElement) {
            const paths = this.svgElement.querySelectorAll('path.connection-line');
            const arrows = this.svgElement.querySelectorAll('polygon.connection-arrow');
            console.log(`[SimpleConnectionRenderer] renderAll: SVG diagnostics`, {
                svgChildren: this.svgElement.children.length,
                paths: paths.length,
                arrows: arrows.length,
                svgInDom: !!this.svgElement.parentElement
            });
        }
    }

    /**
     * Renderuj pojedyncze połączenie
     * 
     * CRITICAL: Linie są renderowane w SVG, więc są na wierzchu węzłów (które są HTML)
     * SVG ma wyższy z-index niż HTML elementy w tym samym kontenerze
     */
    renderConnection(conn: VisualConnectionState): void {
        const path = this.createPath(conn);
        const arrow = this.createArrow(conn);

        // CRITICAL: Dodaj path i arrow do SVG
        // SVG elementy są renderowane w kolejności dodania
        // Linie powinny być widoczne nad wszystkimi innymi elementami SVG
        this.svgElement.appendChild(path);
        this.svgElement.appendChild(arrow);

        this.renderedPaths.set(conn.connectionId, path);
        this.renderedArrows.set(conn.connectionId, arrow);

        // DEBUG: Sprawdź, czy elementy są w DOM
        if (!path.parentElement) {
            console.error(`[SimpleConnectionRenderer] Path for ${conn.connectionId} was not added to SVG!`);
        }
        if (!arrow.parentElement) {
            console.error(`[SimpleConnectionRenderer] Arrow for ${conn.connectionId} was not added to SVG!`);
        }
    }

    /**
     * Utwórz SVG path element
     * 
     * CRITICAL: Path zaczyna się i kończy na center points (środkowych punktach)
     * Punkty są już w przestrzeni SVG (skonwertowane z HTML przez CoordinateUtils)
     */
    private createPath(conn: VisualConnectionState): SVGPathElement {
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        const points = conn.path.points;

        if (!points || points.length < 2) {
            console.error(`[SimpleConnectionRenderer] ❌ Invalid points for ${conn.connectionId}:`, {
                pointsCount: points?.length || 0,
                connectionId: conn.connectionId,
                fromNode: conn.fromNodeId,
                toNode: conn.toNodeId,
                hasPath: !!conn.path,
                hasPoints: !!conn.path?.points
            });
            path.setAttribute('d', 'M 0 0');
            return path;
        }

        // DEBUG: Sprawdź, czy punkty mają sensowne wartości
        const allValid = points.every(p => typeof p.x === 'number' && typeof p.y === 'number' && !isNaN(p.x) && !isNaN(p.y));
        if (!allValid) {
            console.error(`[SimpleConnectionRenderer] ❌ Invalid point values for ${conn.connectionId}:`, points);
        }

        // CRITICAL: Upewnij się, że pierwszy i ostatni punkt to center points
        // (zgodnie z założeniem: 3 punkty na krawędzi, tylko center do łączenia)
        const startPoint = snapPointToPixel(points[0], this.STROKE_WIDTH);
        const endPoint = snapPointToPixel(points[points.length - 1], this.STROKE_WIDTH);

        // Snap wszystkie punkty do pikseli
        const snappedPoints = points.map(p => snapPointToPixel(p, this.STROKE_WIDTH));

        // Generuj path data z zaokrąglonymi narożnikami
        const pathData = this.pathWithRoundedCorners(snappedPoints, this.CORNER_RADIUS);

        path.setAttribute('d', pathData);
        path.setAttribute('class', 'connection-line');
        path.setAttribute('data-connection-id', conn.connectionId);
        path.setAttribute('data-from-node', conn.fromNodeId);
        path.setAttribute('data-to-node', conn.toNodeId);
        path.setAttribute('fill', 'none');
        // CRITICAL: Profesjonalne style linii - subtelne kolory, cienkie linie
        // Linie łączą TYLKO center points (środkowe punkty) na krawędziach etykiet
        path.setAttribute('stroke', '#64748b'); // slate-500 - subtelny szary
        path.setAttribute('stroke-width', String(this.STROKE_WIDTH)); // 2.5px - cienka, precyzyjna linia
        path.setAttribute('stroke-linejoin', 'round');
        path.setAttribute('stroke-linecap', 'round');
        // CRITICAL: geometricPrecision dla płynnych linii, vector-effect zapobiega skalowaniu stroke
        path.setAttribute('shape-rendering', 'geometricPrecision');
        path.setAttribute('vector-effect', 'non-scaling-stroke');
        path.setAttribute('opacity', '1');
        // CRITICAL: Upewnij się, że linie są widoczne (z-index przez SVG order)
        // SVG elementy są renderowane w kolejności dodania - linie powinny być na wierzchu
        // CRITICAL: Włącz pointer events, żeby można było kliknąć i przeciągnąć linię
        path.style.pointerEvents = 'stroke'; // Klikalne tylko na linii (nie na tle)
        path.style.cursor = 'pointer'; // Kursor wskazujący, że można kliknąć

        return path;
    }

    /**
     * Utwórz path data z zaokrąglonymi narożnikami używając łuków SVG (A commands)
     * 
     * Zamienia ostre rogi na gładkie kwadrantowe łuki bez użycia krzywych Béziera
     */
    private pathWithRoundedCorners(points: Point[], radius: number): string {
        if (points.length === 0) return '';

        // Jeśli tylko 2 punkty - prosta linia
        if (points.length === 2) {
            return `M ${points[0].x} ${points[0].y} L ${points[1].x} ${points[1].y}`;
        }

        let d = `M ${points[0].x} ${points[0].y}`;
        const EPS = 1e-6;

        for (let i = 1; i < points.length; i++) {
            const prev = points[i - 1];
            const curr = points[i];

            if (i < points.length - 1) {
                // Corner exists at curr between prev->curr and curr->next
                const next = points[i + 1];

                // Directions
                const dir1 = {
                    x: Math.abs(curr.x - prev.x) < EPS ? 0 : Math.sign(curr.x - prev.x),
                    y: Math.abs(curr.y - prev.y) < EPS ? 0 : Math.sign(curr.y - prev.y)
                };
                const dir2 = {
                    x: Math.abs(next.x - curr.x) < EPS ? 0 : Math.sign(next.x - curr.x),
                    y: Math.abs(next.y - curr.y) < EPS ? 0 : Math.sign(next.y - curr.y)
                };

                // If not a turn (collinear) just line
                if ((dir1.x === dir2.x && dir1.x !== 0) || (dir1.y === dir2.y && dir1.y !== 0)) {
                    d += ` L ${curr.x} ${curr.y}`;
                    continue;
                }

                // Compute points shortened by radius along both segments
                const seg1Length = Math.abs(curr.x - prev.x) + Math.abs(curr.y - prev.y);
                const seg2Length = Math.abs(next.x - curr.x) + Math.abs(next.y - curr.y);
                const r1 = Math.min(radius, seg1Length);
                const r2 = Math.min(radius, seg2Length);

                const before = {
                    x: curr.x - dir1.x * r1,
                    y: curr.y - dir1.y * r1
                };
                const after = {
                    x: curr.x + dir2.x * r2,
                    y: curr.y + dir2.y * r2
                };

                // Line to 'before' point
                d += ` L ${before.x} ${before.y}`;

                // Arc to 'after' point
                // Compute sweep flag: 0 = counter-clockwise, 1 = clockwise
                // For orthogonal paths, we can determine sweep by checking if we're turning right or left
                const cross = dir1.x * dir2.y - dir1.y * dir2.x;
                const sweepFlag = cross > 0 ? 1 : 0;

                // Use rx=ry=radius for quadrant arc
                d += ` A ${radius} ${radius} 0 0 ${sweepFlag} ${after.x} ${after.y}`;
            } else {
                // Last segment, straight to end
                d += ` L ${curr.x} ${curr.y}`;
            }
        }

        return d;
    }

    /**
     * Utwórz strzałkę
     */
    private createArrow(conn: VisualConnectionState): SVGPolygonElement {
        const arrow = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
        const points = conn.path.points;

        if (!points || points.length < 2) {
            arrow.setAttribute('points', '0,0 0,0 0,0');
            return arrow;
        }

        // Ostatni punkt to koniec strzałki
        const tip = points[points.length - 1];
        const beforeTip = points[points.length - 2];

        // Oblicz kąt
        const dx = tip.x - beforeTip.x;
        const dy = tip.y - beforeTip.y;
        const angle = Math.atan2(dy, dx);

        // Utwórz trójkąt strzałki
        const size = 8;
        const arrowPoints = [
            { x: tip.x, y: tip.y },
            {
                x: tip.x - size * Math.cos(angle) - size * 0.5 * Math.sin(angle),
                y: tip.y - size * Math.sin(angle) + size * 0.5 * Math.cos(angle)
            },
            {
                x: tip.x - size * Math.cos(angle) + size * 0.5 * Math.sin(angle),
                y: tip.y - size * Math.sin(angle) - size * 0.5 * Math.cos(angle)
            }
        ];

        arrow.setAttribute('points', arrowPoints.map(p => `${p.x},${p.y}`).join(' '));
        arrow.setAttribute('class', 'connection-arrow');
        arrow.setAttribute('data-connection-id', conn.connectionId);
        arrow.setAttribute('fill', '#64748b'); // slate-500 - ten sam kolor co linia
        arrow.setAttribute('stroke', '#64748b');
        arrow.setAttribute('stroke-width', '1');
        arrow.setAttribute('opacity', '1');
        // CRITICAL: Strzałka powinna być widoczna na końcu linii (center point)

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

        // Remove debug overlay
        if (this.debugOverlay) {
            this.debugOverlay.remove();
            this.debugOverlay = null;
        }
    }

    /**
     * Usuń pojedyncze połączenie
     */
    removeConnection(connectionId: string): void {
        const path = this.renderedPaths.get(connectionId);
        const arrow = this.renderedArrows.get(connectionId);

        if (path) path.remove();
        if (arrow) arrow.remove();

        this.renderedPaths.delete(connectionId);
        this.renderedArrows.delete(connectionId);
    }

    /**
     * CRITICAL: Setup debug overlay - shows model vs DOM coordinate differences
     * Renders two dots per anchor: model-based (blue) and DOM-based (red)
     * If they differ significantly, there's a coordinate conversion issue
     */
    private setupDebugOverlay(modelManager: any): void {
        // Remove existing overlay
        if (this.debugOverlay) {
            this.debugOverlay.remove();
        }

        // Create debug overlay group
        this.debugOverlay = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        this.debugOverlay.setAttribute('class', 'debug-overlay');
        this.debugOverlay.style.pointerEvents = 'none';

        const model = modelManager.getModel();
        if (!model || !this.svgElement) return;

        const EPSILON = 2; // 2px tolerance for differences

        // Iterate through all connections and check anchor positions
        model.workflow.connections.forEach((conn: VisualConnectionState) => {
            const fromAnchor = conn.anchors?.from;
            const toAnchor = conn.anchors?.to;

            if (!fromAnchor || !toAnchor) return;

            // Get model-based position (from connection state)
            const modelFrom = fromAnchor.position;
            const modelTo = toAnchor.position;

            // Try to get DOM-based position (if anchor element exists)
            const anchorElements = (modelManager as any).anchorElements as Map<string, HTMLElement> | undefined;
            if (anchorElements) {
                const fromKey = `${fromAnchor.nodeId}/${fromAnchor.connectionPointId}`;
                const toKey = `${toAnchor.nodeId}/${toAnchor.connectionPointId}`;
                const fromEl = anchorElements.get(fromKey);
                const toEl = anchorElements.get(toKey);

                if (fromEl && this.svgElement) {
                    const domFrom = getAnchorSvgPosition(fromEl, this.svgElement);
                    if (domFrom) {
                        const diff = Math.sqrt(
                            Math.pow(domFrom.x - modelFrom.x, 2) +
                            Math.pow(domFrom.y - modelFrom.y, 2)
                        );

                        // Draw model point (blue)
                        const modelCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                        modelCircle.setAttribute('cx', String(modelFrom.x));
                        modelCircle.setAttribute('cy', String(modelFrom.y));
                        modelCircle.setAttribute('r', '4');
                        modelCircle.setAttribute('fill', '#3b82f6'); // blue
                        modelCircle.setAttribute('opacity', '0.8');
                        this.debugOverlay!.appendChild(modelCircle);

                        // Draw DOM point (red)
                        const domCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                        domCircle.setAttribute('cx', String(domFrom.x));
                        domCircle.setAttribute('cy', String(domFrom.y));
                        domCircle.setAttribute('r', '4');
                        domCircle.setAttribute('fill', '#ef4444'); // red
                        domCircle.setAttribute('opacity', '0.8');
                        this.debugOverlay!.appendChild(domCircle);

                        // Draw line if difference is significant
                        if (diff > EPSILON) {
                            const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                            line.setAttribute('x1', String(modelFrom.x));
                            line.setAttribute('y1', String(modelFrom.y));
                            line.setAttribute('x2', String(domFrom.x));
                            line.setAttribute('y2', String(domFrom.y));
                            line.setAttribute('stroke', '#f59e0b'); // orange
                            line.setAttribute('stroke-width', '1');
                            line.setAttribute('stroke-dasharray', '2,2');
                            this.debugOverlay!.appendChild(line);

                            console.warn(`[SimpleConnectionRenderer] ⚠️ Coordinate mismatch for ${fromKey}: diff=${diff.toFixed(2)}px`, {
                                model: modelFrom,
                                dom: domFrom
                            });
                        }
                    }
                }

                // Same for toAnchor
                if (toEl && this.svgElement) {
                    const domTo = getAnchorSvgPosition(toEl, this.svgElement);
                    if (domTo) {
                        const diff = Math.sqrt(
                            Math.pow(domTo.x - modelTo.x, 2) +
                            Math.pow(domTo.y - modelTo.y, 2)
                        );

                        // Draw model point (blue)
                        const modelCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                        modelCircle.setAttribute('cx', String(modelTo.x));
                        modelCircle.setAttribute('cy', String(modelTo.y));
                        modelCircle.setAttribute('r', '4');
                        modelCircle.setAttribute('fill', '#3b82f6');
                        modelCircle.setAttribute('opacity', '0.8');
                        this.debugOverlay!.appendChild(modelCircle);

                        // Draw DOM point (red)
                        const domCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                        domCircle.setAttribute('cx', String(domTo.x));
                        domCircle.setAttribute('cy', String(domTo.y));
                        domCircle.setAttribute('r', '4');
                        domCircle.setAttribute('fill', '#ef4444');
                        domCircle.setAttribute('opacity', '0.8');
                        this.debugOverlay!.appendChild(domCircle);

                        if (diff > EPSILON) {
                            const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                            line.setAttribute('x1', String(modelTo.x));
                            line.setAttribute('y1', String(modelTo.y));
                            line.setAttribute('x2', String(domTo.x));
                            line.setAttribute('y2', String(domTo.y));
                            line.setAttribute('stroke', '#f59e0b');
                            line.setAttribute('stroke-width', '1');
                            line.setAttribute('stroke-dasharray', '2,2');
                            this.debugOverlay!.appendChild(line);

                            console.warn(`[SimpleConnectionRenderer] ⚠️ Coordinate mismatch for ${toKey}: diff=${diff.toFixed(2)}px`, {
                                model: modelTo,
                                dom: domTo
                            });
                        }
                    }
                }
            }
        });

        // Append overlay to SVG
        if (this.debugOverlay && this.svgElement) {
            this.svgElement.appendChild(this.debugOverlay);
        }
    }
}

