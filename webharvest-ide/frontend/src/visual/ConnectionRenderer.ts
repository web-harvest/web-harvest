/**
 * ConnectionRenderer - Nowy system renderowania połączeń
 * 
 * Odpowiedzialny wyłącznie za renderowanie połączeń na podstawie modelu semantycznego.
 * Używa VisualSemanticModel jako JEDYNEGO źródła prawdy.
 * 
 * PRZEPISANE: Używa gettera do SVG zamiast przechowywać referencję - zawsze używa aktualnego SVG z VisualDesigner
 */

import type { VisualSemanticModel, VisualConnectionState, Point } from './VisualSemanticModel';
import type { VisualSemanticModelManager } from './VisualSemanticModelManager';
import type { VisualDesigner } from './VisualDesigner';

export class ConnectionRenderer {
    public svgElement: SVGSVGElement; // Public for diagnostics and VisualDesigner access
    private modelManager: VisualSemanticModelManager;
    public renderedConnections: Map<string, {
        path: SVGPathElement;
        arrow: SVGPolygonElement;
    }> = new Map();
    private selectedConnectionId: string | null = null;

    constructor(
        svgElement: SVGSVGElement,
        modelManager: VisualSemanticModelManager
    ) {
        this.svgElement = svgElement;
        this.modelManager = modelManager;
        console.log('[ConnectionRenderer] Constructor: Initialized with SVG element', {
            svgInDom: !!svgElement.parentElement,
            svgChildren: svgElement.children.length
        });
    }

    /**
     * Update SVG element reference - DEPRECATED: SVG is now accessed via getter
     * Kept for backward compatibility but does nothing
     */
    updateSvgElement(_newSvgElement: SVGSVGElement): void {
        // No-op: SVG is now accessed via getter, always up-to-date
        console.log('[ConnectionRenderer] updateSvgElement: Called but not needed (using getter)');
    }

    /**
     * Renderuj wszystkie połączenia z modelu semantycznego
     */
    renderAll(): void {
        const svg = this.svgElement; // Get current SVG from designer

        console.log('[ConnectionRenderer] renderAll: START', {
            hasSvgElement: !!svg,
            svgParent: !!svg?.parentElement,
            svgChildren: svg?.children.length || 0
        });

        // DIAGNOSTICS: Check SVG element before building model
        if (!svg) {
            console.error('[ConnectionRenderer] renderAll: SVG element is null!');
            return;
        }

        if (!svg.parentElement) {
            console.error('[ConnectionRenderer] renderAll: SVG element is not in DOM!', {
                svgElement: svg,
                svgId: svg.id,
                svgClasses: svg.className.baseVal
            });
            return;
        }

        // CRITICAL: Build and get model from semantic model manager
        // This ensures we use the SINGLE SOURCE OF TRUTH for all connection data
        // Model contains: positions, connection points, routing paths, semantic selection metadata
        this.modelManager.buildModel();
        const model = this.modelManager.getModel();

        if (!model) {
            console.error('[ConnectionRenderer] renderAll: Model is null');
            return;
        }

        const connections = model.workflow.connections;
        console.log(`[ConnectionRenderer] renderAll: Model has ${connections.size} connections`);
        console.log(`[ConnectionRenderer] renderAll: Connection IDs:`, Array.from(connections.keys()));

        // DIAGNOSTICS: Check first connection state
        if (connections.size > 0) {
            const firstConnId = Array.from(connections.keys())[0];
            const firstConn = connections.get(firstConnId);
            console.log(`[ConnectionRenderer] renderAll: First connection sample:`, {
                id: firstConnId,
                hasPath: !!firstConn?.path,
                pathPoints: firstConn?.path?.points?.length || 0,
                hasArrowMetadata: !!firstConn?.path?.arrowMetadata
            });
        }

        if (connections.size === 0) {
            // Don't log warning if there are no nodes (empty workflow)
            if (model.workflow.allNodes.size > 0) {
                console.warn('[ConnectionRenderer] renderAll: No connections in model!', {
                    rootNodes: model.workflow.rootNodes.length,
                    allNodes: model.workflow.allNodes.size,
                    connections: connections.size
                });
            }
        }

        // DIAGNOSTICS: Check SVG before clearing
        const beforeClear = svg.children.length;
        console.log(`[ConnectionRenderer] renderAll: Before clearAll() - SVG has ${beforeClear} children`);

        // Usuń wszystkie istniejące połączenia
        this.clearAll();

        // Get SVG again (might have changed)
        const svgAfter = this.svgElement;
        if (!svgAfter) {
            console.error('[ConnectionRenderer] renderAll: SVG element is null after clearAll()!');
            return;
        }
        const afterClear = svgAfter.children.length;
        console.log(`[ConnectionRenderer] renderAll: After clearAll() - SVG has ${afterClear} children`);

        // Renderuj każde połączenie
        let renderedCount = 0;
        let errorCount = 0;
        connections.forEach((connectionState, connectionId) => {
            try {
                const currentSvg = this.svgElement;
                if (!currentSvg) {
                    console.error(`[ConnectionRenderer] renderAll: SVG is null for connection ${connectionId}`);
                    return;
                }
                const beforeRender = currentSvg.children.length;
                this.renderConnection(connectionId);
                const afterRender = currentSvg.children.length;

                if (afterRender <= beforeRender) {
                    console.warn(`[ConnectionRenderer] renderAll: Connection ${connectionId} did not add elements (${beforeRender} -> ${afterRender})`);
                }

                renderedCount++;
            } catch (error) {
                errorCount++;
                console.error(`[ConnectionRenderer] renderAll: Error rendering connection ${connectionId}:`, error);
            }
        });

        const finalSvg = this.svgElement;
        const finalChildren = finalSvg?.children.length || 0;
        console.log(`[ConnectionRenderer] renderAll: END`, {
            rendered: renderedCount,
            errors: errorCount,
            total: connections.size,
            stored: this.renderedConnections.size,
            svgChildren: finalChildren,
            expectedChildren: renderedCount * 2 // path + arrow per connection
        });

        // DEBUG: Rysuj pomocnicze kropki w miejscach anchor points
        this.renderDebugAnchors(connections);
    }

    /**
     * DEBUG: Rysuj pomocnicze kropki w miejscach anchor points
     * Włącz w development mode: window.__DEBUG_ANCHORS__ = true
     */
    private renderDebugAnchors(connections: Map<string, VisualConnectionState>): void {
        const svg = this.svgElement;
        if (!svg) return;

        const shouldDebug = process.env.NODE_ENV === 'development' ||
            (typeof window !== 'undefined' && (window as any).__DEBUG_ANCHORS__);

        if (!shouldDebug) return;

        // Usuń stare debug elementy
        const oldDebug = svg.querySelector('.debug-anchors');
        if (oldDebug) {
            svg.removeChild(oldDebug);
        }

        const debugGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        debugGroup.setAttribute('class', 'debug-anchors');

        connections.forEach((conn, id) => {
            if (conn.anchors?.from?.position) {
                const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                circle.setAttribute('cx', String(conn.anchors.from.position.x));
                circle.setAttribute('cy', String(conn.anchors.from.position.y));
                circle.setAttribute('r', '5');
                circle.setAttribute('fill', 'red');
                circle.setAttribute('opacity', '0.7');
                circle.setAttribute('data-connection-id', id);
                circle.setAttribute('data-anchor', 'from');
                debugGroup.appendChild(circle);
            }
            if (conn.anchors?.to?.position) {
                const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                circle.setAttribute('cx', String(conn.anchors.to.position.x));
                circle.setAttribute('cy', String(conn.anchors.to.position.y));
                circle.setAttribute('r', '5');
                circle.setAttribute('fill', 'blue');
                circle.setAttribute('opacity', '0.7');
                circle.setAttribute('data-connection-id', id);
                circle.setAttribute('data-anchor', 'to');
                debugGroup.appendChild(circle);
            }
        });

        svg.appendChild(debugGroup);
        console.log(`[ConnectionRenderer] 🔍 Debug anchors rendered: ${debugGroup.children.length} circles`);
    }

    /**
     * Renderuj pojedyncze połączenie
     */
    renderConnection(connectionId: string): void {
        const svg = this.svgElement; // Get current SVG from designer

        const model = this.modelManager.getModel();
        if (!model) {
            console.error(`[ConnectionRenderer] renderConnection: Model is null for ${connectionId}`);
            return;
        }

        const connectionState = model.workflow.connections.get(connectionId);
        if (!connectionState) {
            console.error(`[ConnectionRenderer] renderConnection: Connection ${connectionId} not found in model`);
            return;
        }

        if (!connectionState.path.points || connectionState.path.points.length < 2) {
            console.error(`[ConnectionRenderer] renderConnection: Connection ${connectionId} has invalid points`, {
                points: connectionState.path.points,
                pointsLength: connectionState.path.points?.length
            });
            return;
        }

        // Usuń istniejące renderowanie tego połączenia (jeśli istnieje)
        this.removeConnection(connectionId);

        // DIAGNOSTICS: Check SVG element before creating elements
        if (!svg) {
            console.error(`[ConnectionRenderer] renderConnection: SVG element is null for ${connectionId}`);
            return;
        }

        if (!svg.parentElement) {
            console.error(`[ConnectionRenderer] renderConnection: SVG element has no parent for ${connectionId}`);
            return;
        }

        // Utwórz nowe elementy SVG
        const path = this.createPathElement(connectionState);
        const arrow = this.createArrowElement(connectionState);

        // DIAGNOSTICS: Verify elements were created
        if (!path || !arrow) {
            console.error(`[ConnectionRenderer] renderConnection: Failed to create SVG elements for ${connectionId}`);
            return;
        }

        // Dodaj do SVG
        try {
            svg.appendChild(path);
            svg.appendChild(arrow);
        } catch (error) {
            console.error(`[ConnectionRenderer] renderConnection: Error appending to SVG for ${connectionId}:`, error);
            return;
        }

        // Zapisz referencje
        this.renderedConnections.set(connectionId, { path, arrow });

        // Dodaj interaktywność dla manualnych połączeń
        if (connectionId.startsWith('conn-')) {
            this.attachInteractivity(path, connectionState);
        }

        // DIAGNOSTICS: Verify elements are in DOM
        const pathInDom = path.parentElement === (this.svgElement as any);
        const arrowInDom = arrow.parentElement === (this.svgElement as any);

        if (!pathInDom || !arrowInDom) {
            console.error(`[ConnectionRenderer] renderConnection: Elements not in DOM for ${connectionId}`, {
                pathInDom,
                arrowInDom,
                svgChildren: this.svgElement.children.length
            });
        } else {
            console.log(`[ConnectionRenderer] renderConnection: ✅ Rendered ${connectionId}`, {
                points: connectionState.path.points.length,
                svgChildren: this.svgElement.children.length
            });
        }
    }

    /**
     * Usuń pojedyncze połączenie
     */
    removeConnection(connectionId: string): void {
        const rendered = this.renderedConnections.get(connectionId);
        if (rendered) {
            const svg = this.svgElement;
            if (svg) {
                if (rendered.path.parentElement === (svg as any)) {
                    svg.removeChild(rendered.path);
                }
                if (rendered.arrow.parentElement === (svg as any)) {
                    svg.removeChild(rendered.arrow);
                }
            } else {
                // SVG not available, just remove from DOM if parent exists
                rendered.path.remove();
                rendered.arrow.remove();
            }
            this.renderedConnections.delete(connectionId);
        }
    }

    /**
     * Usuń wszystkie połączenia
     */
    clearAll(): void {
        const svg = this.svgElement;
        console.log('[ConnectionRenderer] clearAll: START', {
            svgChildren: svg?.children.length || 0,
            storedConnections: this.renderedConnections.size
        });

        if (svg) {
            const toRemove: (SVGPathElement | SVGPolygonElement)[] = [];
            this.renderedConnections.forEach((rendered, connectionId) => {
                if (rendered.path && rendered.path.parentElement === (svg as any)) {
                    toRemove.push(rendered.path);
                }
                if (rendered.arrow && rendered.arrow.parentElement === (svg as any)) {
                    toRemove.push(rendered.arrow);
                }
            });

            toRemove.forEach(el => {
                if (el.parentElement === (svg as any)) {
                    svg.removeChild(el);
                } else {
                    el.remove();
                }
            });
        } else {
            // SVG not available, just remove from DOM
            this.renderedConnections.forEach((rendered) => {
                rendered.path.remove();
                rendered.arrow.remove();
            });
        }

        this.renderedConnections.clear();

        const afterClear = svg?.children.length || 0;
        console.log(`[ConnectionRenderer] clearAll: Cleared ${this.renderedConnections.size} connection elements (SVG now has ${afterClear} children)`);
    }

    /**
     * Aktualizuj pojedyncze połączenie (re-render)
     */
    updateConnection(connectionId: string): void {
        this.removeConnection(connectionId);
        this.renderConnection(connectionId);
    }

    /**
     * Ustaw wybrane połączenie
     */
    setSelectedConnection(connectionId: string | null): void {
        // Remove previous selection
        if (this.selectedConnectionId) {
            const prev = this.renderedConnections.get(this.selectedConnectionId);
            if (prev) {
                const style = this.getConnectionStyle(
                    this.modelManager.getModel()?.workflow.connections.get(this.selectedConnectionId)?.type || 'sequential',
                    false
                );
                prev.path.setAttribute('stroke', style.stroke);
                prev.path.setAttribute('stroke-width', style.strokeWidth);
            }
        }

        this.selectedConnectionId = connectionId;

        // Apply selection style
        if (connectionId) {
            const current = this.renderedConnections.get(connectionId);
            if (current) {
                const connectionState = this.modelManager.getModel()?.workflow.connections.get(connectionId);
                const style = this.getConnectionStyle(
                    connectionState?.type || 'sequential',
                    true
                );
                current.path.setAttribute('stroke', style.stroke);
                current.path.setAttribute('stroke-width', style.strokeWidth);
            }
        }
    }

    /**
     * PROSTY: Generuj path data z punktów - TYLKO H/V (orthogonal)
     */
    private createPathData(points: Point[]): string {
        if (!points.length) return '';
        
        const eps = 0.1;
        let pathData = `M ${points[0].x} ${points[0].y}`;
        
        for (let i = 1; i < points.length; i++) {
            const prev = points[i - 1];
            const cur = points[i];
            
            if (Math.abs(prev.x - cur.x) < eps) {
                // Vertical segment
                pathData += ` V ${cur.y}`;
            } else if (Math.abs(prev.y - cur.y) < eps) {
                // Horizontal segment
                pathData += ` H ${cur.x}`;
            } else {
                // Fallback: rozbij na H+V
                pathData += ` H ${cur.x} V ${cur.y}`;
            }
        }
        
        return pathData;
    }

    /**
     * Utwórz element SVG path dla połączenia
     * CRITICAL: Używa anchor points w przestrzeni SVG
     */
    private createPathElement(connectionState: VisualConnectionState): SVGPathElement {
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        const points = connectionState.path.points;

        if (!points || points.length < 2) {
            console.warn(`[ConnectionRenderer] createPathElement: Invalid points for ${connectionState.connectionId}`, {
                points,
                pathState: connectionState.path,
                hasAnchors: !!connectionState.anchors
            });
            // Fallback: use anchor points from semantic model
            if (connectionState.anchors) {
                const startPoint = connectionState.anchors.from.position;
                const endPoint = connectionState.anchors.to.position;
                const pathData = `M ${startPoint.x} ${startPoint.y} L ${endPoint.x} ${endPoint.y}`;
                path.setAttribute('d', pathData);
                console.log(`[ConnectionRenderer] createPathElement: Using fallback path for ${connectionState.connectionId}`);
            } else {
                path.setAttribute('d', 'M 0 0 L 1 1');
                console.error(`[ConnectionRenderer] createPathElement: No anchors for ${connectionState.connectionId}`);
            }
        } else {
            // PROSTY: Użyj punktów z modelu semantycznego - są już poprawne (center points, orthogonal)
            const pathData = this.createPathData(points);
            path.setAttribute('d', pathData);
        }

        // Style
        path.setAttribute('class', 'connection-line');
        path.setAttribute('data-connection-id', connectionState.connectionId);
        path.setAttribute('fill', 'none');

        // CRITICAL: Atrybuty dla ostrego renderowania
        path.setAttribute('shape-rendering', 'geometricPrecision'); // lub 'crispEdges' w zależności od efektu
        path.setAttribute('vector-effect', 'non-scaling-stroke'); // Nie skaluj szerokości przy zoomie
        path.setAttribute('stroke-linejoin', 'round');
        path.setAttribute('stroke-linecap', 'round');

        // Style według typu połączenia
        const isSelected = this.selectedConnectionId === connectionState.connectionId;
        const style = this.getConnectionStyle(connectionState.type, isSelected);

        path.setAttribute('stroke', style.stroke);
        path.setAttribute('stroke-width', style.strokeWidth);
        if (style.strokeDasharray) {
            path.setAttribute('stroke-dasharray', style.strokeDasharray);
        }

        return path;
    }

    /**
     * Utwórz element SVG arrowhead dla połączenia
     */
    private createArrowElement(connectionState: VisualConnectionState): SVGPolygonElement {
        const arrow = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');

        // Użyj arrowMetadata z modelu semantycznego
        const arrowMetadata = connectionState.path.arrowMetadata;
        if (!arrowMetadata) {
            console.warn(`[ConnectionRenderer] createArrowElement: No arrowMetadata for ${connectionState.connectionId}`);
            // Fallback: oblicz z ostatnich punktów ścieżki
            const points = connectionState.path.points;
            if (!points || points.length < 2) {
                arrow.setAttribute('points', '0,0 0,0 0,0');
                arrow.setAttribute('class', 'connection-arrow');
                arrow.setAttribute('data-connection-id', connectionState.connectionId);
                const style = this.getConnectionStyle(connectionState.type, false);
                arrow.setAttribute('fill', style.stroke);
                return arrow;
            }

            const lastPoint = points[points.length - 1];
            const secondLast = points[points.length - 2];
            const angle = Math.atan2(lastPoint.y - secondLast.y, lastPoint.x - secondLast.x);
            const arrowSize = 8;
            const arrowPoints = this.calculateArrowPoints(lastPoint, angle, arrowSize);
            arrow.setAttribute('points', arrowPoints.map(p => `${p.x},${p.y}`).join(' '));
        } else {
            // Użyj arrowMetadata z modelu
            const arrowSize = 8;
            // Get position from last point of path (arrowMetadata doesn't have position field)
            const points = connectionState.path.points;
            const lastPoint = points && points.length > 0 ? points[points.length - 1] : { x: 0, y: 0 };
            const arrowPoints = this.calculateArrowPoints(lastPoint, arrowMetadata.angle, arrowSize);
            arrow.setAttribute('points', arrowPoints.map(p => `${p.x},${p.y}`).join(' '));
        }

        arrow.setAttribute('class', 'connection-arrow');
        arrow.setAttribute('data-connection-id', connectionState.connectionId);

        const style = this.getConnectionStyle(connectionState.type, false);
        arrow.setAttribute('fill', style.stroke);

        return arrow;
    }

    /**
     * Oblicz punkty strzałki
     */
    private calculateArrowPoints(position: Point, angle: number, size: number): Point[] {
        const tip = position;
        const back1 = {
            x: tip.x - size * Math.cos(angle) - size * 0.5 * Math.sin(angle),
            y: tip.y - size * Math.sin(angle) + size * 0.5 * Math.cos(angle)
        };
        const back2 = {
            x: tip.x - size * Math.cos(angle) + size * 0.5 * Math.sin(angle),
            y: tip.y - size * Math.sin(angle) - size * 0.5 * Math.cos(angle)
        };
        return [tip, back1, back2];
    }

    /**
     * Pobierz styl połączenia na podstawie typu i stanu wyboru
     */
    private getConnectionStyle(
        type: 'sequential' | 'conditional' | 'error',
        isSelected: boolean
    ): { stroke: string; strokeWidth: string; strokeDasharray?: string } {
        if (isSelected) {
            return {
                stroke: 'var(--primary-color, #3b82f6)',
                strokeWidth: '3'
            };
        }

        switch (type) {
            case 'conditional':
                return {
                    stroke: '#4CAF50', // Green
                    strokeWidth: '2',
                    strokeDasharray: '5,5'
                };
            case 'error':
                return {
                    stroke: '#F44336', // Red
                    strokeWidth: '2'
                };
            case 'sequential':
            default:
                return {
                    stroke: '#3b82f6', // Blue
                    strokeWidth: '2'
                };
        }
    }

    /**
     * Dodaj interaktywność do połączenia (hover, click)
     */
    private attachInteractivity(path: SVGPathElement, connectionState: VisualConnectionState): void {
        path.style.cursor = 'pointer';

        path.addEventListener('mouseenter', () => {
            path.setAttribute('stroke-width', '3');
        });

        path.addEventListener('mouseleave', () => {
            if (this.selectedConnectionId !== connectionState.connectionId) {
                const style = this.getConnectionStyle(connectionState.type, false);
                path.setAttribute('stroke-width', style.strokeWidth);
            }
        });

        path.addEventListener('click', (e) => {
            e.stopPropagation();
            this.setSelectedConnection(connectionState.connectionId);
        });
    }
}
