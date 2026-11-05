/**
 * ConnectionDragHandler - Obsługa ręcznego przeciągania połączeń
 * 
 * Obsługuje scenariusz, gdy użytkownik przeciąga linię od jednego portu do drugiego.
 * Waliduje połączenia zgodnie z regułami master-detail.
 */

import type { VisualDesigner } from './VisualDesigner';
import type { VisualSemanticModelManager } from './VisualSemanticModelManager';
import type { IConnectionRenderer } from './IConnectionRenderer';
import type { ConnectionPointsRenderer } from './ConnectionPointsRenderer';
import type { Point, VisualNodeState } from './VisualSemanticModel';
import type { ConnectionType } from './VisualSemanticModel';

export interface DragState {
    fromNodeId: string;
    fromPortId: string;
    fromPortType: string;
    temporaryLine: SVGLineElement | null;
    isReconnect: boolean;
    existingConnectionId: string | null;
    reconnectEnd: 'source' | 'target' | null;
}

export class ConnectionDragHandler {
    private designer: VisualDesigner;
    private modelManager: VisualSemanticModelManager;
    private connectionRenderer: IConnectionRenderer;
    private connectionPointsRenderer: ConnectionPointsRenderer | null = null;
    private dragState: DragState | null = null;
    private hoveredNodeId: string | null = null; // Track which node is being hovered during drag

    constructor(
        designer: VisualDesigner,
        modelManager: VisualSemanticModelManager,
        connectionRenderer: IConnectionRenderer,
        connectionPointsRenderer?: ConnectionPointsRenderer | null
    ) {
        this.designer = designer;
        this.modelManager = modelManager;
        this.connectionRenderer = connectionRenderer;
        this.connectionPointsRenderer = connectionPointsRenderer || null;
    }

    /**
     * Rozpocznij przeciąganie połączenia z portu
     */
    startDrag(fromNodeId: string, fromPortId: string, portType: string): void {
        console.log(`[ConnectionDragHandler] startDrag: ${fromNodeId} -> ${fromPortId} (${portType})`);

        // Sprawdź czy port już ma połączenie (reconnect mode)
        const model = this.modelManager.getModel();
        if (!model) {
            console.error('[ConnectionDragHandler] startDrag: Model is null');
            return;
        }

        const existingConnection = this.findExistingConnection(fromNodeId, fromPortId, 'output');
        const isReconnect = !!existingConnection;

        // Utwórz stan przeciągania
        this.dragState = {
            fromNodeId,
            fromPortId,
            fromPortType: portType,
            temporaryLine: null,
            isReconnect,
            existingConnectionId: existingConnection?.connectionId || null,
            reconnectEnd: isReconnect ? 'source' : null
        };

        // Show connection points on source node
        if (this.connectionPointsRenderer) {
            this.connectionPointsRenderer.showForNode(fromNodeId);
            // Highlight the source point
            this.connectionPointsRenderer.highlightPoint(fromNodeId, fromPortId);
        }

        // Utwórz tymczasową linię
        this.createTemporaryLine();

        // Dodaj event listenery
        this.attachDragListeners();
    }

    /**
     * Aktualizuj pozycję podczas przeciągania
     */
    updateDrag(mousePosition: Point): void {
        if (!this.dragState || !this.dragState.temporaryLine) {
            return;
        }

        // Aktualizuj końcowy punkt tymczasowej linii
        this.dragState.temporaryLine.setAttribute('x2', mousePosition.x.toString());
        this.dragState.temporaryLine.setAttribute('y2', mousePosition.y.toString());

        // Wizualna informacja zwrotna dla reconnect mode
        if (this.dragState.isReconnect) {
            this.dragState.temporaryLine.setAttribute('stroke', '#FF9800');
            this.dragState.temporaryLine.setAttribute('stroke-width', '3');
        } else {
            this.dragState.temporaryLine.setAttribute('stroke', 'var(--magenta, #e91e63)');
            this.dragState.temporaryLine.setAttribute('stroke-width', '2');
        }

        // Detect node under cursor and show connection points
        if (this.connectionPointsRenderer) {
            const nodeUnderCursor = this.findNodeUnderCursor(mousePosition);

            // Hide connection points from previously hovered node
            if (this.hoveredNodeId && this.hoveredNodeId !== nodeUnderCursor) {
                // Don't hide if it's the source node
                if (this.hoveredNodeId !== this.dragState.fromNodeId) {
                    // Only hide if node is not selected
                    const selectedNodes = (this.designer as any).selectedNodeIds || [];
                    if (!selectedNodes.includes(this.hoveredNodeId)) {
                        this.connectionPointsRenderer.hideForNode(this.hoveredNodeId);
                    }
                }
            }

            // Show connection points on node under cursor
            if (nodeUnderCursor && nodeUnderCursor !== this.dragState.fromNodeId) {
                this.connectionPointsRenderer.showForNode(nodeUnderCursor);
                this.hoveredNodeId = nodeUnderCursor;

                // Highlight compatible input points
                this.highlightCompatiblePoints(nodeUnderCursor);
            } else {
                this.hoveredNodeId = null;
            }
        }
    }

    /**
     * Zakończ przeciąganie i utwórz połączenie
     */
    endDrag(toNodeId: string | null, toPortId: string | null): boolean {
        if (!this.dragState) {
            return false;
        }

        console.log(`[ConnectionDragHandler] endDrag: to ${toNodeId} -> ${toPortId}`);

        // Hide connection points (unless nodes are selected)
        if (this.connectionPointsRenderer) {
            this.connectionPointsRenderer.clearHighlights();

            // Hide from hovered node if not selected
            if (this.hoveredNodeId) {
                const selectedNodes = (this.designer as any).selectedNodeIds || [];
                if (!selectedNodes.includes(this.hoveredNodeId)) {
                    this.connectionPointsRenderer.hideForNode(this.hoveredNodeId);
                }
            }

            // Hide from source node if not selected
            const selectedNodes = (this.designer as any).selectedNodeIds || [];
            if (!selectedNodes.includes(this.dragState.fromNodeId)) {
                this.connectionPointsRenderer.hideForNode(this.dragState.fromNodeId);
            }

            this.hoveredNodeId = null;
        }

        // Usuń tymczasową linię
        this.cleanupTemporaryLine();

        // Walidacja
        if (!toNodeId || !toPortId) {
            console.log('[ConnectionDragHandler] endDrag: No target node/port - cancelled');
            this.cancelDrag();
            return false;
        }

        // Sprawdź czy można połączyć
        if (!this.canConnect(this.dragState.fromNodeId, toNodeId)) {
            console.warn(`[ConnectionDragHandler] endDrag: Cannot connect ${this.dragState.fromNodeId} -> ${toNodeId}`);
            this.cancelDrag();
            return false;
        }

        // Jeśli to reconnect, usuń stare połączenie
        if (this.dragState.isReconnect && this.dragState.existingConnectionId) {
            this.modelManager.removeConnection(this.dragState.existingConnectionId);
        }

        // Utwórz nowe połączenie
        const connectionId = this.modelManager.addManualConnection(
            this.dragState.fromNodeId,
            toNodeId,
            this.dragState.fromPortId,
            toPortId,
            this.dragState.fromPortType as ConnectionType
        );

        if (connectionId) {
            // Przerenderuj połączenia
            this.connectionRenderer.renderAll(this.modelManager);
            console.log(`[ConnectionDragHandler] endDrag: Created connection ${connectionId}`);
            this.dragState = null;
            return true;
        } else {
            console.error('[ConnectionDragHandler] endDrag: Failed to create connection');
            this.cancelDrag();
            return false;
        }
    }

    /**
     * Anuluj przeciąganie
     */
    cancelDrag(): void {
        if (!this.dragState) {
            return;
        }

        console.log('[ConnectionDragHandler] cancelDrag');

        // Hide connection points (unless nodes are selected)
        if (this.connectionPointsRenderer) {
            this.connectionPointsRenderer.clearHighlights();

            // Hide from hovered node if not selected
            if (this.hoveredNodeId) {
                const selectedNodes = (this.designer as any).selectedNodeIds || [];
                if (!selectedNodes.includes(this.hoveredNodeId)) {
                    this.connectionPointsRenderer.hideForNode(this.hoveredNodeId);
                }
            }

            // Hide from source node if not selected
            const selectedNodes = (this.designer as any).selectedNodeIds || [];
            if (!selectedNodes.includes(this.dragState.fromNodeId)) {
                this.connectionPointsRenderer.hideForNode(this.dragState.fromNodeId);
            }

            this.hoveredNodeId = null;
        }

        this.cleanupTemporaryLine();
        this.removeDragListeners();
        this.dragState = null;
    }

    /**
     * Sprawdź czy można połączyć dwa węzły
     */
    private canConnect(fromNodeId: string, toNodeId: string): boolean {
        // Nie można połączyć węzła z samym sobą
        if (fromNodeId === toNodeId) {
            return false;
        }

        // Walidacja master-detail
        if (!this.validateMasterDetail(fromNodeId, toNodeId)) {
            return false;
        }

        return true;
    }

    /**
     * Walidacja zgodności master-detail
     * 
     * Reguły:
     * - Detail nodes (mające parentId) mogą być łączone tylko z innymi detail nodes w tym samym master
     * - Master nodes (parentId === null) mogą być łączone z innymi master nodes
     * - Nie można łączyć detail node z węzłem poza jego master node
     */
    private validateMasterDetail(fromNodeId: string, toNodeId: string): boolean {
        const model = this.modelManager.getModel();
        if (!model) {
            return false;
        }

        const fromNode = model.workflow.allNodes.get(fromNodeId);
        const toNode = model.workflow.allNodes.get(toNodeId);

        if (!fromNode || !toNode) {
            return false;
        }

        const fromParentId = fromNode.parentId;
        const toParentId = toNode.parentId;

        // Oba są master nodes (root level) - OK
        if (!fromParentId && !toParentId) {
            return true;
        }

        // Oba są detail nodes - muszą być w tym samym master
        if (fromParentId && toParentId) {
            return fromParentId === toParentId;
        }

        // Jeden jest master, drugi detail - NIE MOŻNA
        return false;
    }

    /**
     * Znajdź istniejące połączenie dla portu
     */
    private findExistingConnection(
        nodeId: string,
        portId: string,
        direction: 'input' | 'output'
    ): { connectionId: string; connectionState: any } | null {
        const model = this.modelManager.getModel();
        if (!model) {
            return null;
        }

        // Szukaj w połączeniach
        for (const [connectionId, connectionState] of model.workflow.connections) {
            if (direction === 'output') {
                if (connectionState.fromNodeId === nodeId &&
                    connectionState.anchors.from.connectionPointId === portId) {
                    return { connectionId, connectionState };
                }
            } else {
                if (connectionState.toNodeId === nodeId &&
                    connectionState.anchors.to.connectionPointId === portId) {
                    return { connectionId, connectionState };
                }
            }
        }

        return null;
    }

    /**
     * Znajdź docelowy port na podstawie pozycji myszy
     * Używa ConnectionPointsCalculator i VisualSemanticModelManager do obliczania pozycji portów
     */
    findTargetPort(mousePosition: Point): { nodeId: string; portId: string } | null {
        const model = this.modelManager.getModel();
        if (!model) {
            return null;
        }

        // Sprawdź wszystkie węzły
        for (const [nodeId, nodeState] of model.workflow.allNodes) {
            const bounds = nodeState.computedBounds;

            // Sprawdź czy mysz jest w granicach węzła
            if (mousePosition.x >= bounds.left &&
                mousePosition.x <= bounds.right &&
                mousePosition.y >= bounds.top &&
                mousePosition.y <= bounds.bottom) {

                // Znajdź najbliższy port używając absolutnych pozycji z modelu
                const allPorts = [
                    ...nodeState.connectionPoints.inputs,
                    ...nodeState.connectionPoints.outputs
                ];

                let closestPort: { portId: string; distance: number } | null = null;

                for (const port of allPorts) {
                    // Użyj VisualSemanticModelManager do pobrania absolutnej pozycji portu
                    const absolutePortPos = this.modelManager.getConnectionPointAbsolutePosition(nodeId, port.id);

                    if (!absolutePortPos) {
                        // Fallback: oblicz z relative position
                        const fallbackPos = {
                            x: nodeState.position.x + port.position.x,
                            y: nodeState.position.y + port.position.y
                        };
                        const distance = Math.sqrt(
                            Math.pow(mousePosition.x - fallbackPos.x, 2) +
                            Math.pow(mousePosition.y - fallbackPos.y, 2)
                        );
                        if (!closestPort || distance < closestPort.distance) {
                            closestPort = { portId: port.id, distance };
                        }
                    } else {
                        // Użyj absolutnej pozycji z modelu
                        const distance = Math.sqrt(
                            Math.pow(mousePosition.x - absolutePortPos.x, 2) +
                            Math.pow(mousePosition.y - absolutePortPos.y, 2)
                        );

                        if (!closestPort || distance < closestPort.distance) {
                            closestPort = { portId: port.id, distance };
                        }
                    }
                }

                // Jeśli port jest wystarczająco blisko (np. 30px)
                if (closestPort && closestPort.distance < 30) {
                    return { nodeId, portId: closestPort.portId };
                }
            }
        }

        return null;
    }

    /**
     * Utwórz tymczasową linię podczas przeciągania
     */
    private createTemporaryLine(): void {
        if (!this.dragState) {
            return;
        }

        const model = this.modelManager.getModel();
        if (!model) {
            return;
        }

        const fromNode = model.workflow.allNodes.get(this.dragState.fromNodeId);
        if (!fromNode) {
            return;
        }

        // Znajdź port
        const allPorts = [
            ...(fromNode.connectionPoints?.inputs || []),
            ...(fromNode.connectionPoints?.outputs || [])
        ];
        const port = allPorts.find(p => p.id === this.dragState?.fromPortId);

        if (!port || !fromNode.computedBounds) {
            return;
        }

        // Oblicz absolutną pozycję portu
        const bounds = fromNode.computedBounds;
        const startPos = {
            x: bounds.left + port.position.x,
            y: bounds.top + port.position.y
        };

        // Utwórz linię SVG
        const svgElement = (this.designer as any).svgElement;
        if (!svgElement) {
            return;
        }

        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', startPos.x.toString());
        line.setAttribute('y1', startPos.y.toString());
        line.setAttribute('x2', startPos.x.toString());
        line.setAttribute('y2', startPos.y.toString());
        line.setAttribute('stroke', 'var(--magenta, #e91e63)');
        line.setAttribute('stroke-width', '2');
        line.setAttribute('stroke-dasharray', '5,5');
        line.style.pointerEvents = 'none';
        line.setAttribute('class', 'connection-drag-temporary');

        svgElement.appendChild(line);
        this.dragState.temporaryLine = line;
    }

    /**
     * Usuń tymczasową linię
     */
    private cleanupTemporaryLine(): void {
        if (this.dragState?.temporaryLine) {
            this.dragState.temporaryLine.remove();
            this.dragState.temporaryLine = null;
        }
    }

    /**
     * Dodaj event listenery dla przeciągania
     */
    private attachDragListeners(): void {
        const canvasElement = (this.designer as any).canvasElement;
        if (!canvasElement) {
            return;
        }

        const handleMouseMove = (e: MouseEvent) => {
            if (!this.dragState) {
                return;
            }

            // Konwertuj pozycję myszy na współrzędne canvas
            const canvasPos = (this.designer as any).viewportToCanvas(e.clientX, e.clientY);
            this.updateDrag(canvasPos);
        };

        const handleMouseUp = (e: MouseEvent) => {
            if (!this.dragState) {
                return;
            }

            // Konwertuj pozycję myszy na współrzędne canvas
            const canvasPos = (this.designer as any).viewportToCanvas(e.clientX, e.clientY);

            // Znajdź docelowy port
            const target = this.findTargetPort(canvasPos);

            if (target) {
                this.endDrag(target.nodeId, target.portId);
            } else {
                this.cancelDrag();
            }

            this.removeDragListeners();
        };

        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp, { once: true });
    }

    /**
     * Usuń event listenery
     */
    private removeDragListeners(): void {
        // Event listenery są automatycznie usuwane przez { once: true } i removeEventListener
        // Ale możemy dodać dodatkową logikę tutaj jeśli potrzeba
    }

    /**
     * Znajdź węzeł pod kursorem podczas przeciągania
     */
    private findNodeUnderCursor(mousePosition: Point): string | null {
        // Get all node elements from canvas
        const canvasElement = (this.designer as any).canvasElement;
        if (!canvasElement) {
            return null;
        }

        const nodeElements = canvasElement.querySelectorAll('.visual-node[data-node-id]');
        for (const element of nodeElements) {
            const nodeId = (element as HTMLElement).dataset.nodeId;
            if (!nodeId) {
                continue;
            }

            const rect = element.getBoundingClientRect();
            const canvasRect = canvasElement.getBoundingClientRect();

            // Convert mouse position to canvas coordinates
            const canvasX = mousePosition.x;
            const canvasY = mousePosition.y;

            // Check if mouse is within node bounds
            if (canvasX >= rect.left - canvasRect.left &&
                canvasX <= rect.right - canvasRect.left &&
                canvasY >= rect.top - canvasRect.top &&
                canvasY <= rect.bottom - canvasRect.top) {
                return nodeId;
            }
        }

        return null;
    }

    /**
     * Podświetl kompatybilne punkty na węźle docelowym
     */
    /**
     * Highlight compatible points - ONLY center points can be used for connections
     */
    private highlightCompatiblePoints(nodeId: string): void {
        if (!this.connectionPointsRenderer || !this.dragState) {
            return;
        }

        const model = this.modelManager.getModel();
        if (!model) {
            return;
        }

        const nodeState = model.workflow.allNodes.get(nodeId);
        if (!nodeState) {
            return;
        }

        // Get all input points for the target node
        const inputPoints = nodeState.connectionPoints.inputs || [];

        // CRITICAL: Only highlight CENTER points - connections can only go to center points
        // This matches the semantic model's decision-making
        const centerPoints = inputPoints.filter(point => point.id.includes('-center'));

        // Highlight only center points (they are the only connectable ones)
        centerPoints.forEach(point => {
            this.connectionPointsRenderer!.highlightPoint(nodeId, point.id);
        });
    }
}

