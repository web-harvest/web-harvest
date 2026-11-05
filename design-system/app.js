/**
 * Główna aplikacja - zarządzanie etykietami i połączeniami
 */

class ConnectionDesignSystem {
    constructor() {
        this.nodes = new Map();
        this.connections = new Map();
        this.nodeCounter = 0;
        this.connectionCounter = 0;
        this.selectedNode = null;
        this.selectedConnection = null;
        this.draggingNode = null;
        this.draggingConnection = null;
        this.draggingConnectionEnd = null; // 'from' or 'to'
        this.dragStartPoint = null;
        
        this.router = new SimpleConnectionRouter();
        this.collisionDetector = new CollisionDetector(this.canvasContainer);
        this.showAnchors = true;
        this.showDebug = false;
        this.autoResolveCollisions = true; // Automatyczne rozwiązywanie kolizji
        
        this.canvasContainer = document.getElementById('canvas-container');
        this.nodesLayer = document.getElementById('nodes-layer');
        this.svg = document.getElementById('connections-svg');
        this.debugLayer = document.getElementById('debug-layer');
        
        this.setupEventListeners();
        this.updateSvgSize();
        this.ensureMarkerExists(); // Ensure marker exists on initialization
    }

    setupEventListeners() {
        document.getElementById('btn-add-node').addEventListener('click', () => this.addNode());
        document.getElementById('btn-clear').addEventListener('click', () => this.clearAll());
        document.getElementById('btn-test-routing').addEventListener('click', () => this.testRouting());
        document.getElementById('btn-test-collision').addEventListener('click', () => this.testCollision());
        document.getElementById('btn-load-config').addEventListener('click', () => this.testLoadConfiguration());
        document.getElementById('chk-show-anchors').addEventListener('change', (e) => {
            this.showAnchors = e.target.checked;
            this.renderAll();
        });
        document.getElementById('chk-show-debug').addEventListener('change', (e) => {
            this.showDebug = e.target.checked;
            this.renderAll();
        });

        this.canvasContainer.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.canvasContainer.addEventListener('mouseup', (e) => this.onMouseUp(e));
        this.canvasContainer.addEventListener('mouseleave', (e) => this.onMouseLeave(e));
        this.canvasContainer.addEventListener('click', (e) => this.onCanvasClick(e));
        
        window.addEventListener('resize', () => this.updateSvgSize());
    }

    updateSvgSize() {
        const rect = this.canvasContainer.getBoundingClientRect();
        this.svg.setAttribute('width', rect.width);
        this.svg.setAttribute('height', rect.height);
        // Ensure viewBox is set for proper scaling - update it each time
        this.svg.setAttribute('viewBox', `0 0 ${rect.width} ${rect.height}`);
        // Ensure preserveAspectRatio for proper rendering
        this.svg.setAttribute('preserveAspectRatio', 'none');
    }

    addNode(x = null, y = null) {
        const nodeId = `node-${++this.nodeCounter}`;
        
        if (x === null || y === null) {
            const rect = this.canvasContainer.getBoundingClientRect();
            x = rect.width / 2 + (Math.random() - 0.5) * 200;
            y = rect.height / 2 + (Math.random() - 0.5) * 200;
        }

        const node = {
            id: nodeId,
            x: x,
            y: y,
            width: 120,
            height: 60,
            title: `Etykieta ${this.nodeCounter}`,
            type: 'http'
        };

        this.nodes.set(nodeId, node);
        this.renderNode(node);
        
        // Sprawdź i rozwiąż kolizje natychmiast
        if (this.autoResolveCollisions) {
            // Sprawdź kolizje przed renderowaniem
            const collisions = this.collisionDetector.checkCollision(node, this.nodes, nodeId);
            if (collisions.length > 0) {
                // Jeśli są kolizje, znajdź wolne miejsce i ustaw od razu (bez animacji przy dodawaniu)
                const freePos = this.collisionDetector.findFreePosition(node, this.nodes, nodeId);
                node.x = freePos.x;
                node.y = freePos.y;
                this.renderNode(node);
            }
            
            // Po renderowaniu, sprawdź wszystkie kolizje i rozwiąż je z animacją
            requestAnimationFrame(() => {
                this.collisionDetector.resolveCollisions(node, this.nodes, nodeId);
                this.updateRouterBounds();
                this.renderAll();
            });
        } else {
            this.updateRouterBounds();
            this.renderAll();
        }
    }

    renderNode(node) {
        let nodeEl = document.getElementById(`node-${node.id}`);
        if (!nodeEl) {
            nodeEl = document.createElement('div');
            nodeEl.id = `node-${node.id}`;
            nodeEl.className = 'node';
            nodeEl.innerHTML = `
                <div class="node-title">${node.title}</div>
                <div class="node-type">${node.type}</div>
            `;
            this.nodesLayer.appendChild(nodeEl);
            
            nodeEl.addEventListener('mousedown', (e) => this.onNodeMouseDown(e, node));
            nodeEl.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectNode(node.id);
            });
            
            // Setup hover listeners for showing/hiding connection points
            this.setupNodeHoverListeners(nodeEl, node);
        }

        // Ustaw pozycję - sprawdź czy nie trwa animacja
        const currentTransition = nodeEl.style.transition;
        if (!currentTransition || currentTransition === '') {
            // Brak animacji - ustaw pozycję bezpośrednio
            nodeEl.style.left = `${node.x}px`;
            nodeEl.style.top = `${node.y}px`;
        }
        // Jeśli jest transition (animacja), collision-detector.js zarządza pozycją

        // Render connection points (center points only) - domyślnie ukryte
        // Punkty będą pokazywane tylko gdy myszka jest nad węzłem
        if (this.showAnchors) {
            this.renderConnectionPoints(node);
            // Domyślnie ukryj punkty
            this.hideConnectionPoints(node.id);
            // Dodaj event listenery dla pokazywania/ukrywania punktów
            this.setupNodeHoverListeners(nodeEl, node);
        } else {
            this.removeConnectionPoints(node.id);
        }
    }

    renderConnectionPoints(node) {
        const sides = ['top', 'bottom', 'left', 'right'];
        const rect = this.nodesLayer.getBoundingClientRect();
        const nodeEl = document.getElementById(`node-${node.id}`);
        const nodeRect = nodeEl.getBoundingClientRect();

        sides.forEach(side => {
            const pointId = `${node.id}-${side}-center`;
            let pointEl = document.getElementById(`point-${pointId}`);
            
            if (!pointEl) {
                pointEl = document.createElement('div');
                pointEl.id = `point-${pointId}`;
                pointEl.className = 'connection-point center';
                pointEl.setAttribute('data-node-id', node.id);
                pointEl.setAttribute('data-side', side);
                pointEl.setAttribute('data-point-type', 'center');
                this.nodesLayer.appendChild(pointEl);
                
                // Gdy najedziemy na punkt, automatycznie przejdź w tryb tworzenia połączenia
                pointEl.addEventListener('mouseenter', (e) => {
                    e.stopPropagation();
                    // Automatycznie rozpocznij przeciąganie połączenia gdy myszka najedzie na punkt
                    // Tylko jeśli nie przeciągamy już czegoś
                    if (!this.draggingConnection && !this.draggingNode) {
                        // Małe opóźnienie aby uniknąć przypadkowego rozpoczęcia
                        pointEl._enterTimeout = setTimeout(() => {
                            if (!this.draggingConnection && !this.draggingNode) {
                                this.startConnectionDragFromPoint(node, side, e);
                            }
                        }, 50); // 50ms opóźnienie
                    }
                });
                
                pointEl.addEventListener('mouseleave', (e) => {
                    // Anuluj timeout jeśli myszka opuściła punkt przed rozpoczęciem przeciągania
                    if (pointEl._enterTimeout) {
                        clearTimeout(pointEl._enterTimeout);
                        pointEl._enterTimeout = null;
                    }
                    // Jeśli nie przeciągamy połączenia, ukryj punkty tego węzła
                    if (!this.draggingConnection) {
                        this.hideConnectionPoints(node.id);
                    }
                });
                
                pointEl.addEventListener('mousedown', (e) => {
                    e.stopPropagation(); // Zapobiegaj propagacji do węzła
                    this.onAnchorMouseDown(e, node, side);
                });
            }

            let x, y;
            switch (side) {
                case 'top':
                    x = nodeRect.left - rect.left + nodeRect.width / 2;
                    y = nodeRect.top - rect.top;
                    break;
                case 'bottom':
                    x = nodeRect.left - rect.left + nodeRect.width / 2;
                    y = nodeRect.top - rect.top + nodeRect.height;
                    break;
                case 'left':
                    x = nodeRect.left - rect.left;
                    y = nodeRect.top - rect.top + nodeRect.height / 2;
                    break;
                case 'right':
                    x = nodeRect.left - rect.left + nodeRect.width;
                    y = nodeRect.top - rect.top + nodeRect.height / 2;
                    break;
            }

            pointEl.style.left = `${x - 6}px`;
            pointEl.style.top = `${y - 6}px`;
        });
    }

    removeConnectionPoints(nodeId) {
        const points = document.querySelectorAll(`[data-node-id="${nodeId}"].connection-point`);
        points.forEach(p => p.remove());
    }

    showConnectionPoints(nodeId) {
        const points = document.querySelectorAll(`[data-node-id="${nodeId}"].connection-point`);
        points.forEach(p => {
            p.style.display = 'block';
            // Użyj requestAnimationFrame dla płynnej animacji
            requestAnimationFrame(() => {
                p.style.opacity = '1';
            });
        });
    }

    hideConnectionPoints(nodeId) {
        const points = document.querySelectorAll(`[data-node-id="${nodeId}"].connection-point`);
        points.forEach(p => {
            p.style.opacity = '0';
            // Ukryj po zakończeniu animacji
            setTimeout(() => {
                if (parseFloat(p.style.opacity) === 0) {
                    p.style.display = 'none';
                }
            }, 200); // Czas trwania transition
        });
    }

    setupNodeHoverListeners(nodeEl, node) {
        // Usuń stare listenery jeśli istnieją
        const oldMouseEnter = nodeEl._mouseEnterHandler;
        const oldMouseLeave = nodeEl._mouseLeaveHandler;
        if (oldMouseEnter) {
            nodeEl.removeEventListener('mouseenter', oldMouseEnter);
        }
        if (oldMouseLeave) {
            nodeEl.removeEventListener('mouseleave', oldMouseLeave);
        }

        // Dodaj nowe listenery
        const mouseEnterHandler = () => {
            // Pokazuj punkty tylko jeśli:
            // 1. showAnchors jest włączone
            // 2. Nie przeciągamy etykiety
            // 3. Nie przeciągamy połączenia (wtedy punkty są już widoczne)
            if (this.showAnchors && !this.draggingNode && !this.draggingConnection) {
                // Małe opóźnienie aby uniknąć przypadkowego pokazania podczas szybkiego ruchu
                nodeEl._hoverTimeout = setTimeout(() => {
                    if (!this.draggingNode && !this.draggingConnection) {
                        this.showConnectionPoints(node.id);
                    }
                }, 100); // 100ms opóźnienie
            }
        };
        const mouseLeaveHandler = () => {
            // Anuluj timeout jeśli myszka opuściła węzeł przed pokazaniem punktów
            if (nodeEl._hoverTimeout) {
                clearTimeout(nodeEl._hoverTimeout);
                nodeEl._hoverTimeout = null;
            }
            
            // Ukryj punkty tylko jeśli nie przeciągamy połączenia ani etykiety
            if (!this.draggingConnection && !this.draggingNode) {
                this.hideConnectionPoints(node.id);
            }
        };

        nodeEl.addEventListener('mouseenter', mouseEnterHandler);
        nodeEl.addEventListener('mouseleave', mouseLeaveHandler);

        // Zapisz referencje do listenerów aby móc je później usunąć
        nodeEl._mouseEnterHandler = mouseEnterHandler;
        nodeEl._mouseLeaveHandler = mouseLeaveHandler;
    }

    onNodeMouseDown(e, node) {
        e.stopPropagation();
        
        // Sprawdź czy kliknięto na punkt połączenia (nie na samą etykietę)
        const clickedElement = e.target;
        if (clickedElement.classList.contains('connection-point')) {
            // To jest kliknięcie na punkt połączenia - nie przeciągaj etykiety
            return;
        }
        
        this.selectNode(node.id);
        this.draggingNode = node;
        this.dragStartX = e.clientX - node.x;
        this.dragStartY = e.clientY - node.y;
        
        // Ukryj punkty podczas przeciągania etykiety
        this.hideConnectionPoints(node.id);
    }

    startConnectionDragFromPoint(node, side, e) {
        // Automatycznie rozpocznij przeciąganie połączenia gdy myszka najedzie na punkt
        const rect = this.canvasContainer.getBoundingClientRect();
        const pointEl = e.target;
        const pointRect = pointEl.getBoundingClientRect();
        
        this.draggingConnection = {
            fromNode: node,
            fromSide: side,
            fromPoint: {
                x: pointRect.left - rect.left + pointRect.width / 2,
                y: pointRect.top - rect.top + pointRect.height / 2
            }
        };
        this.dragStartPoint = { x: e.clientX, y: e.clientY };
        
        // Upewnij się że nie przeciągamy etykiety
        this.draggingNode = null;
        
        // Pokazuj wszystkie punkty podczas przeciągania połączenia
        this.nodes.forEach(n => {
            this.showConnectionPoints(n.id);
        });
        
        // Renderuj tymczasowe połączenie od razu (od punktu do aktualnej pozycji myszki)
        const currentPoint = {
            x: e.clientX - rect.left,
            y: e.clientY - rect.top
        };
        this.renderTemporaryConnection(this.draggingConnection.fromPoint, currentPoint);
    }

    onAnchorMouseDown(e, node, side) {
        e.stopPropagation();
        e.preventDefault();
        
        // Jeśli już przeciągamy (zaczęło się od mouseenter), nie rób nic dodatkowego
        if (this.draggingConnection && 
            this.draggingConnection.fromNode.id === node.id && 
            this.draggingConnection.fromSide === side) {
            return;
        }
        
        const rect = this.canvasContainer.getBoundingClientRect();
        const pointEl = e.target;
        const pointRect = pointEl.getBoundingClientRect();
        
        this.draggingConnection = {
            fromNode: node,
            fromSide: side,
            fromPoint: {
                x: pointRect.left - rect.left + pointRect.width / 2,
                y: pointRect.top - rect.top + pointRect.height / 2
            }
        };
        this.dragStartPoint = { x: e.clientX, y: e.clientY };
        
        // Upewnij się że nie przeciągamy etykiety
        this.draggingNode = null;
        
        // Pokazuj wszystkie punkty podczas przeciągania połączenia
        this.nodes.forEach(n => {
            this.showConnectionPoints(n.id);
        });
    }

    onMouseMove(e) {
        if (this.draggingNode) {
            // Ukryj wszystkie punkty podczas przeciągania etykiety
            this.nodes.forEach(n => {
                this.hideConnectionPoints(n.id);
            });
            
            const rect = this.canvasContainer.getBoundingClientRect();
            const newX = e.clientX - rect.left - this.dragStartX;
            const newY = e.clientY - rect.top - this.dragStartY;
            
            // Sprawdź kolizje podczas przeciągania (bez animacji, tylko wizualne)
            if (this.autoResolveCollisions) {
                const testNode = { ...this.draggingNode, x: newX, y: newY };
                const collisions = this.collisionDetector.checkCollision(testNode, this.nodes, this.draggingNode.id);
                
                if (collisions.length > 0) {
                    // Jeśli jest kolizja, znajdź najbliższe wolne miejsce
                    const freePos = this.collisionDetector.findFreePosition(testNode, this.nodes, this.draggingNode.id);
                    this.draggingNode.x = freePos.x;
                    this.draggingNode.y = freePos.y;
                } else {
                    this.draggingNode.x = newX;
                    this.draggingNode.y = newY;
                }
            } else {
                this.draggingNode.x = newX;
                this.draggingNode.y = newY;
            }
            
            this.renderNode(this.draggingNode);
            this.updateRouterBounds();
            this.renderAll();
        } else if (this.draggingConnection) {
            const rect = this.canvasContainer.getBoundingClientRect();
            const currentPoint = {
                x: e.clientX - rect.left,
                y: e.clientY - rect.top
            };
            
            // Check if near any anchor point for connection
            this.highlightNearbyAnchor(currentPoint);
            
            // If dragging connection end (changing target)
            if (this.draggingConnection.connectionId) {
                const conn = this.connections.get(this.draggingConnection.connectionId);
                if (conn) {
                    if (this.draggingConnection.endType === 'from') {
                        const toPos = this.getAnchorPosition(conn.toNodeId, conn.toSide);
                        this.renderTemporaryConnection(currentPoint, toPos);
                    } else {
                        const fromPos = this.getAnchorPosition(conn.fromNodeId, conn.fromSide);
                        this.renderTemporaryConnection(fromPos, currentPoint);
                    }
                }
            } else {
                // Creating new connection
                this.renderTemporaryConnection(this.draggingConnection.fromPoint, currentPoint);
            }
        }
    }

    highlightNearbyAnchor(mousePoint) {
        // Only highlight if we're actually dragging a connection
        if (!this.draggingConnection) {
            this.clearHighlights();
            return;
        }

        // Pokazuj punkty na węzłach podczas przeciągania połączenia
        this.nodes.forEach(node => {
            this.showConnectionPoints(node.id);
        });

        // Clear previous highlights
        this.clearHighlights();

        // Find nearest anchor within connection range
        const CONNECTION_RANGE = 50; // pixels
        let nearestAnchor = null;
        let minDistance = Infinity;

        this.nodes.forEach(node => {
            const sides = ['top', 'bottom', 'left', 'right'];
            sides.forEach(side => {
                const anchorPos = this.getAnchorPosition(node.id, side);
                if (anchorPos) {
                    const distance = Math.sqrt(
                        Math.pow(mousePoint.x - anchorPos.x, 2) + 
                        Math.pow(mousePoint.y - anchorPos.y, 2)
                    );

                    if (distance < CONNECTION_RANGE && distance < minDistance) {
                        // Don't highlight if it's the source anchor
                        if (!this.draggingConnection.fromNode || 
                            this.draggingConnection.fromNode.id !== node.id ||
                            this.draggingConnection.fromSide !== side) {
                            minDistance = distance;
                            nearestAnchor = { nodeId: node.id, side: side, pos: anchorPos };
                        }
                    }
                }
            });
        });

        // Highlight nearest anchor
        if (nearestAnchor) {
            const anchorEl = document.querySelector(
                `[data-node-id="${nearestAnchor.nodeId}"][data-side="${nearestAnchor.side}"].connection-point`
            );
            if (anchorEl) {
                anchorEl.classList.add('highlight');
                anchorEl.style.animation = 'pulse 0.6s ease-in-out infinite';
            }
        }
    }

    onMouseUp(e) {
        // Always clear highlights first
        this.clearHighlights();
        
        if (this.draggingNode) {
            // Po zakończeniu przeciągania etykiety, ukryj wszystkie punkty
            this.nodes.forEach(n => {
                this.hideConnectionPoints(n.id);
            });
            // Po zakończeniu przeciągania, sprawdź i rozwiąż kolizje
            if (this.autoResolveCollisions) {
                const node = this.draggingNode;
                // Sprawdź kolizje natychmiast
                const collisions = this.collisionDetector.checkCollision(node, this.nodes, node.id);
                if (collisions.length > 0) {
                    // Jeśli są kolizje, znajdź wolne miejsce i animuj przesunięcie
                    requestAnimationFrame(() => {
                        this.collisionDetector.resolveCollisions(node, this.nodes, node.id);
                        this.updateRouterBounds();
                        this.renderAll();
                    });
                } else {
                    // Brak kolizji - tylko zaktualizuj routing
                    this.updateRouterBounds();
                    this.renderAll();
                }
            } else {
                this.updateRouterBounds();
                this.renderAll();
            }
            this.draggingNode = null;
        } else if (this.draggingConnection) {
            // Remove temporary connection immediately
            this.removeTemporaryConnection();
            
            // Check if dropped on an anchor point
            const pointEl = document.elementFromPoint(e.clientX, e.clientY);
            
            if (pointEl && pointEl.classList.contains('connection-point')) {
                const targetNodeId = pointEl.getAttribute('data-node-id');
                const targetSide = pointEl.getAttribute('data-side');
                const targetNode = this.nodes.get(targetNodeId);
                
                if (this.draggingConnection.connectionId) {
                    // Changing existing connection end
                    const conn = this.connections.get(this.draggingConnection.connectionId);
                    if (conn && targetNode) {
                        if (this.draggingConnection.endType === 'from') {
                            // Changing source
                            if (targetNodeId !== conn.toNodeId) {
                                conn.fromNodeId = targetNodeId;
                                conn.fromSide = targetSide;
                                // Use requestAnimationFrame to ensure DOM is ready
                                requestAnimationFrame(() => {
                                    this.renderAll();
                                    this.updateInfo();
                                });
                            }
                        } else {
                            // Changing target
                            if (targetNodeId !== conn.fromNodeId) {
                                // Automatyczne przemapowanie punktów - zawsze próbuj znaleźć najlepsze
                                let finalFromSide = conn.fromSide;
                                let finalToSide = targetSide;

                                // Automatyczne przemapowanie na podstawie pozycji
                                const autoSelectedToSide = this.autoSelectBestAnchor(
                                    conn.fromNodeId,
                                    conn.fromSide,
                                    targetNodeId
                                );
                                const autoSelectedFromSide = this.autoSelectBestSourceAnchor(
                                    conn.fromNodeId,
                                    targetNodeId,
                                    targetSide
                                );
                                
                                if (autoSelectedToSide && autoSelectedToSide !== targetSide) {
                                    console.log(`[Connection] Auto-remapping target anchor from ${targetSide} to ${autoSelectedToSide}`);
                                    finalToSide = autoSelectedToSide;
                                }
                                if (autoSelectedFromSide && autoSelectedFromSide !== conn.fromSide) {
                                    console.log(`[Connection] Auto-remapping source anchor from ${conn.fromSide} to ${autoSelectedFromSide}`);
                                    finalFromSide = autoSelectedFromSide;
                                }

                                conn.toNodeId = targetNodeId;
                                conn.toSide = finalToSide;
                                conn.fromSide = finalFromSide;
                                
                                // Sprawdź routing bez krzyżowania
                                this.updateRouterBounds();
                                const bestRoute = this.findNonIntersectingRoute(
                                    conn.fromNodeId,
                                    conn.fromSide,
                                    conn.toNodeId,
                                    conn.toSide
                                );
                                
                                if (bestRoute) {
                                    if (bestRoute.fromSide !== finalFromSide) {
                                        console.log(`[Connection] Using better source anchor point: ${bestRoute.fromSide}`);
                                        conn.fromSide = bestRoute.fromSide;
                                    }
                                    if (bestRoute.toSide !== finalToSide) {
                                        console.log(`[Connection] Using better target anchor point: ${bestRoute.toSide}`);
                                        conn.toSide = bestRoute.toSide;
                                    }
                                }
                                
                                // Use requestAnimationFrame to ensure DOM is ready
                                requestAnimationFrame(() => {
                                    this.renderAll();
                                    this.updateInfo();
                                });
                            }
                        }
                    }
                } else {
                    // Creating new connection
                    if (targetNode && targetNode.id !== this.draggingConnection.fromNode.id) {
                        // Automatyczne przemapowanie punktów - zawsze próbuj znaleźć najlepsze
                        let finalFromSide = this.draggingConnection.fromSide;
                        let finalToSide = targetSide;

                        // Automatyczne przemapowanie na podstawie pozycji
                        const autoSelectedToSide = this.autoSelectBestAnchor(
                            this.draggingConnection.fromNode.id,
                            this.draggingConnection.fromSide,
                            targetNode.id
                        );
                        const autoSelectedFromSide = this.autoSelectBestSourceAnchor(
                            this.draggingConnection.fromNode.id,
                            targetNode.id,
                            targetSide
                        );
                        
                        if (autoSelectedToSide && autoSelectedToSide !== targetSide) {
                            console.log(`[Connection] Auto-remapping target anchor from ${targetSide} to ${autoSelectedToSide}`);
                            finalToSide = autoSelectedToSide;
                        }
                        if (autoSelectedFromSide && autoSelectedFromSide !== this.draggingConnection.fromSide) {
                            console.log(`[Connection] Auto-remapping source anchor from ${this.draggingConnection.fromSide} to ${autoSelectedFromSide}`);
                            finalFromSide = autoSelectedFromSide;
                        }

                        // Check if connection would create a cycle before creating
                        const created = this.createConnection(
                            this.draggingConnection.fromNode.id,
                            finalFromSide,
                            targetNode.id,
                            finalToSide
                        );
                        
                        if (created) {
                            // Ensure rendering happens
                            requestAnimationFrame(() => {
                                this.renderAll();
                            });
                        }
                    }
                }
            } else {
                // Dropped on empty space - remove connection if changing end
                if (this.draggingConnection.connectionId && this.draggingConnection.endType) {
                    if (confirm('Usunąć połączenie?')) {
                        this.connections.delete(this.draggingConnection.connectionId);
                        requestAnimationFrame(() => {
                            this.renderAll();
                            this.updateInfo();
                        });
                    }
                }
            }
            
            // Clear dragging state
            this.draggingConnection = null;
            
            // Ukryj wszystkie punkty po zakończeniu przeciągania połączenia
            // (zostaną pokazane ponownie gdy myszka najedzie na etykietę)
            this.nodes.forEach(n => {
                this.hideConnectionPoints(n.id);
            });
        }
    }

    clearHighlights() {
        // Clear all highlighted connection points
        document.querySelectorAll('.connection-point.highlight').forEach(p => {
            p.classList.remove('highlight');
            p.style.animation = '';
            p.style.background = '';
            p.style.width = '';
            p.style.height = '';
            p.style.boxShadow = '';
        });
    }

    onMouseLeave(e) {
        // Clear highlights when mouse leaves canvas
        if (this.draggingConnection) {
            this.clearHighlights();
            this.removeTemporaryConnection();
        }
    }

    onCanvasClick(e) {
        // Deselect on canvas click
        if (e.target === this.canvasContainer || e.target === this.svg || e.target === this.nodesLayer) {
            this.selectNode(null);
            this.selectConnection(null);
            this.clearHighlights();
        }
    }

    selectConnection(connectionId) {
        // Reset all connections to default style
        document.querySelectorAll('.connection-line').forEach(l => {
            l.classList.remove('selected');
            l.setAttribute('stroke', '#64748b');
            l.setAttribute('stroke-width', '2.5');
        });
        
        if (connectionId) {
            const path = this.svg.querySelector(`[data-connection-id="${connectionId}"]`);
            if (path) {
                path.classList.add('selected');
                path.setAttribute('stroke', '#3b82f6');
                path.setAttribute('stroke-width', '3');
                this.selectedConnection = connectionId;
                this.updateInfo();
            }
        } else {
            this.selectedConnection = null;
        }
    }

    // Check if creating a connection would create a cycle
    /**
     * Próbuje rozwiązać kolizje połączeń przez przesunięcie węzłów
     */
    resolveConnectionCollisionsByMovingNodes(fromNodeId, toNodeId) {
        const fromNode = this.nodes.get(fromNodeId);
        const toNode = this.nodes.get(toNodeId);
        if (!fromNode || !toNode) return false;

        // Oblicz kierunek połączenia
        const fromPos = this.getAnchorPosition(fromNodeId, 'center');
        const toPos = this.getAnchorPosition(toNodeId, 'center');
        if (!fromPos || !toPos) return false;

        const dx = toPos.x - fromPos.x;
        const dy = toPos.y - fromPos.y;

        // Przesuń węzły w kierunku przeciwnym do połączenia aby stworzyć więcej miejsca
        const offset = 50; // Przesunięcie w pikselach

        let moved = false;

        // Jeśli połączenie idzie w prawo, przesuń cel w prawo
        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) {
                // Połączenie idzie w prawo - przesuń cel w prawo
                toNode.x += offset;
                moved = true;
            } else {
                // Połączenie idzie w lewo - przesuń cel w lewo
                toNode.x -= offset;
                moved = true;
            }
        } else {
            if (dy > 0) {
                // Połączenie idzie w dół - przesuń cel w dół
                toNode.y += offset;
                moved = true;
            } else {
                // Połączenie idzie w górę - przesuń cel w górę
                toNode.y -= offset;
                moved = true;
            }
        }

        if (moved) {
            // Sprawdź kolizje z innymi węzłami
            this.collisionDetector.resolveCollisions(toNode, this.nodes, toNodeId);
            // Renderuj węzeł w nowej pozycji
            this.renderNode(toNode);
            console.log(`[Connection] Moved node ${toNodeId} to resolve connection collisions`);
            return true;
        }

        return false;
    }

    getAnchorPosition(nodeId, side) {
        // Obsługa 'center' dla obliczeń środka
        if (side === 'center') {
            const node = this.nodes.get(nodeId);
            if (!node) return null;
            const nodeEl = document.getElementById(`node-${nodeId}`);
            if (!nodeEl) return null;
            const rect = this.canvasContainer.getBoundingClientRect();
            const nodeRect = nodeEl.getBoundingClientRect();
            return {
                x: nodeRect.left - rect.left + nodeRect.width / 2,
                y: nodeRect.top - rect.top + nodeRect.height / 2
            };
        }
        
        const node = this.nodes.get(nodeId);
        if (!node) return null;

        const nodeEl = document.getElementById(`node-${nodeId}`);
        if (!nodeEl) return null;

        const rect = this.canvasContainer.getBoundingClientRect();
        const nodeRect = nodeEl.getBoundingClientRect();

        let x, y;
        switch (side) {
            case 'top':
                x = nodeRect.left - rect.left + nodeRect.width / 2;
                y = nodeRect.top - rect.top;
                break;
            case 'bottom':
                x = nodeRect.left - rect.left + nodeRect.width / 2;
                y = nodeRect.top - rect.top + nodeRect.height;
                break;
            case 'left':
                x = nodeRect.left - rect.left;
                y = nodeRect.top - rect.top + nodeRect.height / 2;
                break;
            case 'right':
                x = nodeRect.left - rect.left + nodeRect.width;
                y = nodeRect.top - rect.top + nodeRect.height / 2;
                break;
        }

        return { x, y };
    }

    wouldCreateCycle(fromNodeId, toNodeId) {
        // Build adjacency list (directed graph)
        const graph = new Map();
        this.connections.forEach(conn => {
            if (!graph.has(conn.fromNodeId)) {
                graph.set(conn.fromNodeId, []);
            }
            graph.get(conn.fromNodeId).push(conn.toNodeId);
        });
        
        // Add the new connection temporarily
        if (!graph.has(fromNodeId)) {
            graph.set(fromNodeId, []);
        }
        graph.get(fromNodeId).push(toNodeId);
        
        // DFS to detect cycle
        const visited = new Set();
        const recStack = new Set();
        
        const hasCycleDFS = (node) => {
            if (recStack.has(node)) {
                return true; // Cycle detected
            }
            if (visited.has(node)) {
                return false;
            }
            
            visited.add(node);
            recStack.add(node);
            
            const neighbors = graph.get(node) || [];
            for (const neighbor of neighbors) {
                if (hasCycleDFS(neighbor)) {
                    return true;
                }
            }
            
            recStack.delete(node);
            return false;
        };
        
        // Check all nodes for cycles
        for (const nodeId of graph.keys()) {
            if (!visited.has(nodeId)) {
                if (hasCycleDFS(nodeId)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    createConnection(fromNodeId, fromSide, toNodeId, toSide) {
        // Prevent self-connection
        if (fromNodeId === toNodeId) {
            console.warn('[Connection] Cannot connect node to itself');
            return false;
        }
        
        // Check if connection would create a cycle
        if (this.wouldCreateCycle(fromNodeId, toNodeId)) {
            console.warn('[Connection] Cannot create connection - would create a cycle');
            alert('Nie można utworzyć połączenia - utworzyłoby to pętlę w łańcuchu');
            return false;
        }

        // Automatyczne przemapowanie punktów - zawsze próbuj znaleźć najlepsze punkty
        let finalFromSide = fromSide;
        let finalToSide = toSide;

        // Najpierw spróbuj automatycznego przemapowania na podstawie pozycji
        const autoSelectedToSide = this.autoSelectBestAnchor(fromNodeId, fromSide, toNodeId);
        const autoSelectedFromSide = this.autoSelectBestSourceAnchor(fromNodeId, toNodeId, toSide);
        
        if (autoSelectedToSide && autoSelectedToSide !== toSide) {
            finalToSide = autoSelectedToSide;
        }
        
        if (autoSelectedFromSide && autoSelectedFromSide !== fromSide) {
            finalFromSide = autoSelectedFromSide;
        }

        // Znajdź routing bez krzyżowania, próbując różnych kombinacji punktów (oba punkty)
        this.updateRouterBounds();
        const bestRoute = this.findNonIntersectingRoute(fromNodeId, finalFromSide, toNodeId, finalToSide);
        
        if (!bestRoute) {
            console.error('[Connection] Could not find valid route');
            alert('Nie można utworzyć połączenia - brak możliwej ścieżki');
            return false;
        }

        // ZAWSZE używaj punktów znalezionych przez routing
        if (bestRoute.fromSide !== finalFromSide || bestRoute.toSide !== finalToSide) {
            finalFromSide = bestRoute.fromSide;
            finalToSide = bestRoute.toSide;
        }

        // Jeśli nadal są kolizje z połączeniami, spróbuj przesunąć węzły
        if (bestRoute.hasConnectionCollision) {
            console.warn('[Connection] Route has connection collisions, attempting to resolve by moving nodes');
            const resolved = this.resolveConnectionCollisionsByMovingNodes(fromNodeId, toNodeId);
            if (!resolved) {
                console.warn('[Connection] Could not resolve connection collisions by moving nodes');
                // Kontynuuj mimo kolizji - użytkownik może ręcznie poprawić
            }
        }
        
        // One-to-one rule: Each node can have only one outgoing connection (from any side)
        const existingOutgoing = [];
        this.connections.forEach((conn, connId) => {
            if (conn.fromNodeId === fromNodeId) {
                existingOutgoing.push(connId);
            }
        });
        
        // One-to-one rule: Each node can have only one incoming connection (to any side)
        const existingIncoming = [];
        this.connections.forEach((conn, connId) => {
            if (conn.toNodeId === toNodeId) {
                existingIncoming.push(connId);
            }
        });
        
        // Check if source node already has an outgoing connection
        if (existingOutgoing.length > 0) {
            const existingConn = this.connections.get(existingOutgoing[0]);
            console.warn('[Connection] Source node already has an outgoing connection');
            if (!confirm(`Etykieta już ma połączenie wychodzące (do: ${existingConn.toNodeId}). Zastąpić istniejące połączenie?`)) {
                return false;
            }
        }
        
        // Check if target node already has an incoming connection
        if (existingIncoming.length > 0) {
            const existingConn = this.connections.get(existingIncoming[0]);
            console.warn('[Connection] Target node already has an incoming connection');
            if (!confirm(`Etykieta docelowa już ma połączenie przychodzące (z: ${existingConn.fromNodeId}). Zastąpić istniejące połączenie?`)) {
                return false;
            }
        }
        
        // Remove old connections
        const connectionsToRemove = [...existingOutgoing, ...existingIncoming];
        connectionsToRemove.forEach(connId => {
            this.connections.delete(connId);
            this.router.removeExistingPath(connId);
        });
        
        const connectionId = `conn-${fromNodeId}-${toNodeId}-${++this.connectionCounter}`;
        
        const connection = {
            id: connectionId,
            fromNodeId: fromNodeId,
            fromSide: finalFromSide, // Użyj automatycznie wybranego punktu źródłowego
            toNodeId: toNodeId,
            toSide: finalToSide // Użyj automatycznie wybranego punktu docelowego
        };

        this.connections.set(connectionId, connection);
        // Render in next frame to ensure DOM is ready
        requestAnimationFrame(() => {
            this.renderAll();
            this.updateInfo();
        });
        
        return true;
    }

    updateRouterBounds() {
        this.router.clearNodeBounds();
        this.nodes.forEach(node => {
            this.router.setNodeBounds(node.id, {
                x: node.x,
                y: node.y,
                width: node.width,
                height: node.height
            });
        });
    }

    getAnchorPosition(nodeId, side) {
        const node = this.nodes.get(nodeId);
        if (!node) return null;

        const nodeEl = document.getElementById(`node-${nodeId}`);
        if (!nodeEl) return null;

        const rect = this.canvasContainer.getBoundingClientRect();
        const nodeRect = nodeEl.getBoundingClientRect();

        let x, y;
        switch (side) {
            case 'top':
                x = nodeRect.left - rect.left + nodeRect.width / 2;
                y = nodeRect.top - rect.top;
                break;
            case 'bottom':
                x = nodeRect.left - rect.left + nodeRect.width / 2;
                y = nodeRect.top - rect.top + nodeRect.height;
                break;
            case 'left':
                x = nodeRect.left - rect.left;
                y = nodeRect.top - rect.top + nodeRect.height / 2;
                break;
            case 'right':
                x = nodeRect.left - rect.left + nodeRect.width;
                y = nodeRect.top - rect.top + nodeRect.height / 2;
                break;
        }

        return { x, y };
    }

    /**
     * Automatycznie wybiera najlepszy punkt połączenia dla węzła docelowego
     * na podstawie pozycji węzła źródłowego i kierunku połączenia
     */
    autoSelectBestAnchor(fromNodeId, fromSide, toNodeId) {
        const fromNode = this.nodes.get(fromNodeId);
        const toNode = this.nodes.get(toNodeId);
        if (!fromNode || !toNode) {
            console.warn(`[autoSelectBestAnchor] Nodes not found: ${fromNodeId}, ${toNodeId}`);
            return null;
        }

        const fromPos = this.getAnchorPosition(fromNodeId, fromSide);
        if (!fromPos) {
            console.warn(`[autoSelectBestAnchor] Could not get anchor position for ${fromNodeId}(${fromSide})`);
            return null;
        }

        // Oblicz pozycję środka węzła docelowego
        const toNodeEl = document.getElementById(`node-${toNodeId}`);
        if (!toNodeEl) {
            console.warn(`[autoSelectBestAnchor] Target node element not found: ${toNodeId}`);
            return null;
        }
        const rect = this.canvasContainer.getBoundingClientRect();
        const toNodeRect = toNodeEl.getBoundingClientRect();
        const toCenterX = toNodeRect.left - rect.left + toNodeRect.width / 2;
        const toCenterY = toNodeRect.top - rect.top + toNodeRect.height / 2;

        // Oblicz odległości od punktu źródłowego do każdej strony węzła docelowego
        const distances = {
            top: Math.abs(fromPos.y - (toNodeRect.top - rect.top)),
            bottom: Math.abs(fromPos.y - (toNodeRect.top - rect.top + toNodeRect.height)),
            left: Math.abs(fromPos.x - (toNodeRect.left - rect.left)),
            right: Math.abs(fromPos.x - (toNodeRect.left - rect.left + toNodeRect.width))
        };

        // Wybierz stronę z najmniejszą odległością
        let bestSide = 'right';
        let minDist = distances.right;
        for (const [side, dist] of Object.entries(distances)) {
            if (dist < minDist) {
                minDist = dist;
                bestSide = side;
            }
        }

        // Log only if debug enabled
        if (this.showDebug) {
            console.log(`[autoSelectBestAnchor] Distances: ${JSON.stringify(distances)}, best: ${bestSide}`);
        }

        // Jeśli łączymy z przeciwległej strony, preferuj najbliższą stronę
        const oppositeSides = {
            'top': 'bottom',
            'bottom': 'top',
            'left': 'right',
            'right': 'left'
        };

        // Sprawdź czy źródło jest po przeciwnej stronie niż sugerowana
        const fromNodeEl = document.getElementById(`node-${fromNodeId}`);
        if (!fromNodeEl) {
            console.warn(`[autoSelectBestAnchor] Source node element not found: ${fromNodeId}`);
            return bestSide;
        }
        const fromNodeRect = fromNodeEl.getBoundingClientRect();
        const fromCenterX = fromNodeRect.left - rect.left + fromNodeRect.width / 2;
        const fromCenterY = fromNodeRect.top - rect.top + fromNodeRect.height / 2;

        // Jeśli węzły są po przeciwnych stronach, wybierz odpowiednią stronę
        if (fromCenterX < toCenterX && bestSide === 'left') {
            // Źródło po lewej, cel po prawej - użyj prawej strony celu
            bestSide = 'right';
        } else if (fromCenterX > toCenterX && bestSide === 'right') {
            // Źródło po prawej, cel po lewej - użyj lewej strony celu
            bestSide = 'left';
        } else if (fromCenterY < toCenterY && bestSide === 'top') {
            // Źródło powyżej, cel poniżej - użyj dolnej strony celu
            bestSide = 'bottom';
        } else if (fromCenterY > toCenterY && bestSide === 'bottom') {
            // Źródło poniżej, cel powyżej - użyj górnej strony celu
            bestSide = 'top';
        }

        return bestSide;
    }

    /**
     * Automatycznie wybiera najlepszy punkt połączenia dla węzła źródłowego
     * na podstawie pozycji węzła docelowego
     */
    autoSelectBestSourceAnchor(fromNodeId, toNodeId, toSide) {
        const fromNode = this.nodes.get(fromNodeId);
        const toNode = this.nodes.get(toNodeId);
        if (!fromNode || !toNode) return null;

        const toPos = this.getAnchorPosition(toNodeId, toSide);
        if (!toPos) return null;

        // Oblicz pozycję środka węzła źródłowego
        const fromNodeEl = document.getElementById(`node-${fromNodeId}`);
        if (!fromNodeEl) return null;
        const rect = this.canvasContainer.getBoundingClientRect();
        const fromNodeRect = fromNodeEl.getBoundingClientRect();
        const fromCenterX = fromNodeRect.left - rect.left + fromNodeRect.width / 2;
        const fromCenterY = fromNodeRect.top - rect.top + fromNodeRect.height / 2;

        // Oblicz odległości od każdej strony źródła do punktu docelowego
        const distances = {
            top: Math.abs(toPos.y - (fromNodeRect.top - rect.top)),
            bottom: Math.abs(toPos.y - (fromNodeRect.top - rect.top + fromNodeRect.height)),
            left: Math.abs(toPos.x - (fromNodeRect.left - rect.left)),
            right: Math.abs(toPos.x - (fromNodeRect.left - rect.left + fromNodeRect.width))
        };

        // Wybierz stronę z najmniejszą odległością
        let bestSide = 'right';
        let minDist = distances.right;
        for (const [side, dist] of Object.entries(distances)) {
            if (dist < minDist) {
                minDist = dist;
                bestSide = side;
            }
        }

        // Sprawdź czy cel jest po przeciwnej stronie niż sugerowana
        const toNodeEl = document.getElementById(`node-${toNodeId}`);
        if (!toNodeEl) return bestSide;
        const toNodeRect = toNodeEl.getBoundingClientRect();
        const toCenterX = toNodeRect.left - rect.left + toNodeRect.width / 2;
        const toCenterY = toNodeRect.top - rect.top + toNodeRect.height / 2;

        // Jeśli węzły są po przeciwnych stronach, wybierz odpowiednią stronę
        if (fromCenterX < toCenterX && bestSide === 'right') {
            // Źródło po lewej, cel po prawej - użyj prawej strony źródła
            bestSide = 'right';
        } else if (fromCenterX > toCenterX && bestSide === 'left') {
            // Źródło po prawej, cel po lewej - użyj lewej strony źródła
            bestSide = 'left';
        } else if (fromCenterY < toCenterY && bestSide === 'bottom') {
            // Źródło powyżej, cel poniżej - użyj dolnej strony źródła
            bestSide = 'bottom';
        } else if (fromCenterY > toCenterY && bestSide === 'top') {
            // Źródło poniżej, cel powyżej - użyj górnej strony źródła
            bestSide = 'top';
        }

        return bestSide;
    }

    /**
     * Sprawdza czy połączenie jest odwrócone o 180 stopni
     * (punkt źródłowy i docelowy są po przeciwnych stronach)
     */
    isConnectionReversed180(fromNodeId, fromSide, toNodeId, toSide) {
        const fromNode = this.nodes.get(fromNodeId);
        const toNode = this.nodes.get(toNodeId);
        if (!fromNode || !toNode) return false;

        const fromPos = this.getAnchorPosition(fromNodeId, fromSide);
        const toPos = this.getAnchorPosition(toNodeId, toSide);
        if (!fromPos || !toPos) return false;

        // Oblicz środki węzłów
        const fromNodeEl = document.getElementById(`node-${fromNodeId}`);
        const toNodeEl = document.getElementById(`node-${toNodeId}`);
        if (!fromNodeEl || !toNodeEl) return false;

        const rect = this.canvasContainer.getBoundingClientRect();
        const fromNodeRect = fromNodeEl.getBoundingClientRect();
        const toNodeRect = toNodeEl.getBoundingClientRect();
        
        const fromCenterX = fromNodeRect.left - rect.left + fromNodeRect.width / 2;
        const fromCenterY = fromNodeRect.top - rect.top + fromNodeRect.height / 2;
        const toCenterX = toNodeRect.left - rect.left + toNodeRect.width / 2;
        const toCenterY = toNodeRect.top - rect.top + toNodeRect.height / 2;

        // Sprawdź czy punkty są po przeciwnych stronach względem środków węzłów
        const dx = toCenterX - fromCenterX;
        const dy = toCenterY - fromCenterY;

        // Sprawdź czy punkt źródłowy i docelowy są po przeciwnych stronach
        // Prostsza logika: sprawdź czy punkty są po przeciwnych stronach względem środków
        const oppositeSides = {
            'top': 'bottom',
            'bottom': 'top',
            'left': 'right',
            'right': 'left'
        };

        // Jeśli łączymy z przeciwległej strony względem środków węzłów
        if (fromSide === 'left' && toSide === 'right' && dx > 0) return true;
        if (fromSide === 'right' && toSide === 'left' && dx < 0) return true;
        if (fromSide === 'top' && toSide === 'bottom' && dy > 0) return true;
        if (fromSide === 'bottom' && toSide === 'top' && dy < 0) return true;

        // Dodatkowa sprawdzenie: czy punkty są dokładnie po przeciwnych stronach
        // (np. źródło po lewej, cel po prawej i węzły są obok siebie)
        const isOppositeHorizontal = (fromSide === 'left' && toSide === 'right') || 
                                      (fromSide === 'right' && toSide === 'left');
        const isOppositeVertical = (fromSide === 'top' && toSide === 'bottom') || 
                                   (fromSide === 'bottom' && toSide === 'top');
        
        if (isOppositeHorizontal && Math.abs(dx) > Math.abs(dy)) {
            return true; // Węzły są obok siebie poziomo, punkty po przeciwnych stronach
        }
        if (isOppositeVertical && Math.abs(dy) > Math.abs(dx)) {
            return true; // Węzły są obok siebie pionowo, punkty po przeciwnych stronach
        }

        return false;
    }

    /**
     * Zwraca kąt dla danej strony (w stopniach)
     */
    getSideAngle(side) {
        switch (side) {
            case 'top': return -90;
            case 'bottom': return 90;
            case 'left': return 180;
            case 'right': return 0;
            default: return 0;
        }
    }

    /**
     * Próbuje znaleźć routing bez krzyżowania, próbując różnych kombinacji punktów
     * Teraz próbuje również różnych punktów źródłowych
     */
    findNonIntersectingRoute(fromNodeId, fromSide, toNodeId, toSide) {
        const sides = ['top', 'bottom', 'left', 'right'];
        const excludeNodes = new Set([fromNodeId, toNodeId]);

        // Najpierw spróbuj z oryginalnymi punktami
        let bestRoute = null;
        let bestScore = Infinity;

        // Próbuj wszystkie kombinacje punktów źródłowych i docelowych
        for (const testFromSide of sides) {
            const fromPos = this.getAnchorPosition(fromNodeId, testFromSide);
            if (!fromPos) continue;

            for (const testToSide of sides) {
                const toPos = this.getAnchorPosition(toNodeId, testToSide);
                if (!toPos) continue;

                // Route z tymi punktami
                const points = this.router.route(fromPos, toPos, testFromSide, testToSide, excludeNodes);
                if (!points || points.length < 2) continue;

                // Sprawdź kolizje
                const hasNodeCollision = this.router.pathIntersectsNodes(points, excludeNodes);
                const hasConnectionCollision = this.router.pathIntersectsConnections(points);

                // Oblicz score (preferuj bez kolizji, potem najkrótsze)
                let score = this.router.pathManhattanLength(points);
                if (hasNodeCollision) score += 10000; // Duża kara za kolizję z węzłem
                if (hasConnectionCollision) score += 5000; // Kara za kolizję z połączeniem

                // Preferuj oryginalne punkty jeśli bez kolizji
                if (testFromSide === fromSide && testToSide === toSide && !hasNodeCollision && !hasConnectionCollision) {
                    score -= 200; // Bonus za użycie oryginalnych punktów
                } else if (testFromSide === fromSide && !hasNodeCollision && !hasConnectionCollision) {
                    score -= 100; // Bonus za użycie oryginalnego punktu źródłowego
                } else if (testToSide === toSide && !hasNodeCollision && !hasConnectionCollision) {
                    score -= 100; // Bonus za użycie oryginalnego punktu docelowego
                }

                // Kara za odwrócenie o 180 stopni (ale mniejsza niż za kolizje)
                if (this.isConnectionReversed180(fromNodeId, testFromSide, toNodeId, testToSide)) {
                    score += 1000; // Kara za odwrócenie, ale mniejsza niż za kolizje
                }

                if (score < bestScore) {
                    bestScore = score;
                    bestRoute = {
                        fromSide: testFromSide,
                        toSide: testToSide,
                        points: points,
                        hasNodeCollision: hasNodeCollision,
                        hasConnectionCollision: hasConnectionCollision
                    };
                }
            }
        }

        return bestRoute;
    }

    renderAll() {
        this.updateSvgSize();
        this.updateRouterBounds();
        
        // Update router with existing paths for intersection detection
        // First pass: calculate all paths without checking against other connections
        const pathCache = new Map();
        this.router.clearExistingPaths();
        this.connections.forEach((conn, connId) => {
            const fromPos = this.getAnchorPosition(conn.fromNodeId, conn.fromSide);
            const toPos = this.getAnchorPosition(conn.toNodeId, conn.toSide);
            if (fromPos && toPos) {
                const excludeNodes = new Set([conn.fromNodeId, conn.toNodeId]);
                // First pass: route without checking other connections
                const points = this.router.route(fromPos, toPos, conn.fromSide, conn.toSide, excludeNodes);
                if (points && points.length >= 2) {
                    pathCache.set(connId, points);
                }
            }
        });
        
        // Second pass: set existing paths for intersection detection
        pathCache.forEach((points, connId) => {
            this.router.setExistingPaths(connId, points);
        });
        
        // Clear existing connections and hit areas (but preserve defs with marker)
        const existingPaths = this.svg.querySelectorAll('.connection-line, .connection-end-hit');
        existingPaths.forEach(p => p.remove());
        
        // Ensure marker exists in defs
        this.ensureMarkerExists();
        
        // Render all connections
        this.connections.forEach(conn => {
            this.renderConnection(conn);
        });

        // Clear debug
        this.debugLayer.innerHTML = '';
        if (this.showDebug) {
            this.renderDebugOverlay();
        }
        
        // Restore selection
        if (this.selectedConnection) {
            this.selectConnection(this.selectedConnection);
        }
    }

    renderConnection(conn) {
        // Get EXACT connection point positions (on node edge)
        const fromPos = this.getAnchorPosition(conn.fromNodeId, conn.fromSide);
        const toPos = this.getAnchorPosition(conn.toNodeId, conn.toSide);

        if (!fromPos || !toPos) return;
        
        // Path will end EXACTLY at toPos, arrow tip (refX=12) will touch this point

        const excludeNodes = new Set([conn.fromNodeId, conn.toNodeId]);
        // Temporarily remove this connection from existing paths to avoid self-intersection
        this.router.removeExistingPath(conn.id);
        const points = this.router.route(fromPos, toPos, conn.fromSide, conn.toSide, excludeNodes);
        
        // Debug: Log router output (only if debug enabled)
        if (this.showDebug) {
            console.log('[Router] Connection', conn.id, {
                fromPos: fromPos,
                toPos: toPos,
                pointsCount: points.length,
                points: points,
                firstPoint: points[0],
                lastPoint: points[points.length - 1]
            });
        }
        
        // Update with new path
        this.router.setExistingPaths(conn.id, points);

        if (points.length < 2) return;

        // Create SVG path
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        let pathData = `M ${points[0].x} ${points[0].y}`;
        
        for (let i = 1; i < points.length; i++) {
            const prev = points[i - 1];
            const curr = points[i];
            
            // Skip if points are identical (avoid duplicate H/V commands)
            if (Math.abs(prev.x - curr.x) < 0.1 && Math.abs(prev.y - curr.y) < 0.1) {
                continue;
            }
            
            if (Math.abs(prev.x - curr.x) < 0.1) {
                pathData += ` V ${curr.y}`;
            } else if (Math.abs(prev.y - curr.y) < 0.1) {
                pathData += ` H ${curr.x}`;
            } else {
                pathData += ` L ${curr.x} ${curr.y}`;
            }
        }
        
        // CRITICAL: Path MUST end exactly at toPos (connection point)
        // Arrow tip (refX=12) will touch this exact point
        if (points.length > 0 && toPos) {
            const lastPoint = points[points.length - 1];
            const distToTarget = Math.abs(lastPoint.x - toPos.x) + Math.abs(lastPoint.y - toPos.y);
            
            // Always ensure last point is exactly at connection point
            if (distToTarget > 0.1) {
                points[points.length - 1] = { x: toPos.x, y: toPos.y };
                // Rebuild path data ending exactly at connection point
                pathData = `M ${points[0].x} ${points[0].y}`;
                for (let i = 1; i < points.length; i++) {
                    const prev = points[i - 1];
                    const curr = points[i];
                    // Skip duplicate points
                    if (Math.abs(prev.x - curr.x) < 0.1 && Math.abs(prev.y - curr.y) < 0.1) {
                        continue;
                    }
                    if (Math.abs(prev.x - curr.x) < 0.1) {
                        pathData += ` V ${curr.y}`;
                    } else if (Math.abs(prev.y - curr.y) < 0.1) {
                        pathData += ` H ${curr.x}`;
                    } else {
                        pathData += ` L ${curr.x} ${curr.y}`;
                    }
                }
            }
        }
        
        // Debug: Log path info (only if debug enabled)
        if (this.showDebug) {
            console.log('[Path] Connection', conn.id, {
                pointsCount: points.length,
                firstPoint: points[0],
                lastPoint: points[points.length - 1],
                fromPos: fromPos,
                toPos: toPos,
                pathData: pathData
            });
        }

        // FIRST: Verify path direction and fix if reversed
        // Path should go from source (fromPos) to target (toPos)
        let firstPoint = points[0];
        let lastPoint = points[points.length - 1];
        
        if (fromPos && toPos) {
            const distFromFirst = Math.abs(firstPoint.x - fromPos.x) + Math.abs(firstPoint.y - fromPos.y);
            const distFromLast = Math.abs(lastPoint.x - fromPos.x) + Math.abs(lastPoint.y - fromPos.y);
            
            // If path is reversed (first point closer to target than source), reverse it
            if (distFromFirst > distFromLast) {
                console.warn('[Connection] Path reversed for', conn.id, '- fixing direction');
                points.reverse();
                // Rebuild path data with corrected direction
                pathData = `M ${points[0].x} ${points[0].y}`;
                for (let i = 1; i < points.length; i++) {
                    const prev = points[i - 1];
                    const curr = points[i];
                    if (Math.abs(prev.x - curr.x) < 0.1) {
                        pathData += ` V ${curr.y}`;
                    } else if (Math.abs(prev.y - curr.y) < 0.1) {
                        pathData += ` H ${curr.x}`;
                    } else {
                        pathData += ` L ${curr.x} ${curr.y}`;
                    }
                }
                // Update first and last points after reversal
                firstPoint = points[0];
                lastPoint = points[points.length - 1];
            }
        }
        
        // NOW set path attributes with corrected direction
        path.setAttribute('d', pathData);
        path.setAttribute('class', 'connection-line');
        path.setAttribute('data-connection-id', conn.id);
        path.setAttribute('stroke', '#64748b');
        path.setAttribute('stroke-width', '2.5');
        path.setAttribute('fill', 'none');
        path.setAttribute('stroke-linecap', 'round');
        path.setAttribute('stroke-linejoin', 'round');
        
        // Ensure marker exists and set marker-end
        const marker = this.ensureMarkerExists();
        if (marker) {
            const markerId = marker.getAttribute('id');
            path.setAttribute('marker-end', `url(#${markerId})`);
            path.removeAttribute('marker-start');
        } else {
            console.error('[ConnectionRenderer] Failed to create marker for connection', conn.id);
        }
        path.style.cursor = 'pointer';
        
        // Add click handler for connection
        path.addEventListener('click', (e) => {
            e.stopPropagation();
            this.selectConnection(conn.id);
        });
        
        // Create invisible hit areas at connection ends for dragging
        this.createConnectionEndHitArea(conn.id, 'from', firstPoint, conn.fromNodeId, conn.fromSide);
        this.createConnectionEndHitArea(conn.id, 'to', lastPoint, conn.toNodeId, conn.toSide);
        
        this.svg.appendChild(path);
    }

    createConnectionEndHitArea(connectionId, endType, point, nodeId, side) {
        // Create invisible circle at connection end for dragging
        const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        circle.setAttribute('cx', point.x);
        circle.setAttribute('cy', point.y);
        circle.setAttribute('r', '12'); // Hit area radius
        circle.setAttribute('fill', 'transparent');
        circle.setAttribute('stroke', 'none');
        circle.setAttribute('class', 'connection-end-hit');
        circle.setAttribute('data-connection-id', connectionId);
        circle.setAttribute('data-end-type', endType);
        circle.setAttribute('data-node-id', nodeId);
        circle.setAttribute('data-side', side);
        circle.style.cursor = 'grab';
        circle.style.pointerEvents = 'all';
        
        circle.addEventListener('mousedown', (e) => {
            e.stopPropagation();
            this.onConnectionEndMouseDown(e, connectionId, endType);
        });
        
        this.svg.appendChild(circle);
    }

    onConnectionEndMouseDown(e, connectionId, endType) {
        const conn = this.connections.get(connectionId);
        if (!conn) return;
        
        const rect = this.canvasContainer.getBoundingClientRect();
        this.draggingConnection = {
            connectionId: connectionId,
            endType: endType, // 'from' or 'to'
            originalFromNode: conn.fromNodeId,
            originalFromSide: conn.fromSide,
            originalToNode: conn.toNodeId,
            originalToSide: conn.toSide
        };
        
        if (endType === 'from') {
            const pos = this.getAnchorPosition(conn.fromNodeId, conn.fromSide);
            this.dragStartPoint = pos;
        } else {
            const pos = this.getAnchorPosition(conn.toNodeId, conn.toSide);
            this.dragStartPoint = pos;
        }
    }

    renderTemporaryConnection(from, to) {
        this.removeTemporaryConnection();
        
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', `M ${from.x} ${from.y} L ${to.x} ${to.y}`);
        path.setAttribute('class', 'connection-line');
        path.setAttribute('stroke', '#3b82f6');
        path.setAttribute('stroke-width', '2');
        path.setAttribute('stroke-dasharray', '5,5');
        path.setAttribute('opacity', '0.6');
        path.id = 'temp-connection';
        
        this.svg.appendChild(path);
    }

    removeTemporaryConnection() {
        const temp = document.getElementById('temp-connection');
        if (temp) temp.remove();
    }

    renderDebugOverlay() {
        this.connections.forEach(conn => {
            const fromPos = this.getAnchorPosition(conn.fromNodeId, conn.fromSide);
            const toPos = this.getAnchorPosition(conn.toNodeId, conn.toSide);

            if (!fromPos || !toPos) return;

            const excludeNodes = new Set([conn.fromNodeId, conn.toNodeId]);
            const points = this.router.route(fromPos, toPos, conn.fromSide, conn.toSide, excludeNodes);

            // Draw debug points
            points.forEach((point, i) => {
                const debugPoint = document.createElement('div');
                debugPoint.className = 'debug-point';
                debugPoint.style.left = `${point.x - 3}px`;
                debugPoint.style.top = `${point.y - 3}px`;
                debugPoint.title = `Point ${i}: (${point.x.toFixed(1)}, ${point.y.toFixed(1)})`;
                this.debugLayer.appendChild(debugPoint);
            });

            // Draw debug lines
            for (let i = 0; i < points.length - 1; i++) {
                const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line.setAttribute('x1', points[i].x);
                line.setAttribute('y1', points[i].y);
                line.setAttribute('x2', points[i + 1].x);
                line.setAttribute('y2', points[i + 1].y);
                line.setAttribute('class', 'debug-line');
                this.svg.appendChild(line);
            }
        });
    }

    selectNode(nodeId) {
        // Deselect all
        document.querySelectorAll('.node.selected').forEach(n => n.classList.remove('selected'));
        
        if (nodeId) {
            const nodeEl = document.getElementById(`node-${nodeId}`);
            if (nodeEl) {
                nodeEl.classList.add('selected');
                this.selectedNode = nodeId;
            }
        }
    }

    ensureMarkerExists() {
        let marker = this.svg.querySelector('#arrowhead');
        if (!marker) {
            let defs = this.svg.querySelector('defs');
            if (!defs) {
                defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
                this.svg.insertBefore(defs, this.svg.firstChild);
            }
            marker = document.createElementNS('http://www.w3.org/2000/svg', 'marker');
            marker.setAttribute('id', 'arrowhead');
            // Marker size - arrow should touch connection point
            marker.setAttribute('markerWidth', '12');
            marker.setAttribute('markerHeight', '12');
            // refX at tip (12) - tip touches the connection point
            marker.setAttribute('refX', '12');
            marker.setAttribute('refY', '6');
            marker.setAttribute('orient', 'auto');
            marker.setAttribute('markerUnits', 'userSpaceOnUse');
            marker.setAttribute('viewBox', '0 0 12 12');
            const polygon = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
            // Arrow: base at left (0,0 and 0,12), tip at right (12,6)
            polygon.setAttribute('points', '0 0, 12 6, 0 12');
            // Normal color
            polygon.setAttribute('fill', '#64748b');
            polygon.setAttribute('stroke', '#64748b');
            polygon.setAttribute('stroke-width', '1');
            marker.appendChild(polygon);
            defs.appendChild(marker);
            console.log('[Marker] Created arrowhead marker', {
                id: marker.getAttribute('id'),
                width: marker.getAttribute('markerWidth'),
                height: marker.getAttribute('markerHeight'),
                refX: marker.getAttribute('refX'),
                refY: marker.getAttribute('refY')
            });
        }
        return marker;
    }

    clearAll() {
        if (confirm('Czy na pewno chcesz wyczyścić wszystko?')) {
            this.nodes.clear();
            this.connections.clear();
            this.nodesLayer.innerHTML = '';
            
            // Clear connections but preserve defs with marker
            const existingPaths = this.svg.querySelectorAll('.connection-line, .connection-end-hit');
            existingPaths.forEach(p => p.remove());
            this.debugLayer.innerHTML = '';
            
            // Ensure marker exists
            this.ensureMarkerExists();
            
            this.updateInfo();
        }
    }

    testRouting() {
        // Add test nodes
        this.clearAll();
        this.addNode(200, 200);
        this.addNode(400, 200);
        this.addNode(300, 400);
        this.addNode(500, 400);
        
        setTimeout(() => {
            // Create test connections
            const nodeIds = Array.from(this.nodes.keys());
            if (nodeIds.length >= 2) {
                this.createConnection(nodeIds[0], 'right', nodeIds[1], 'left');
            }
            if (nodeIds.length >= 3) {
                this.createConnection(nodeIds[1], 'bottom', nodeIds[2], 'top');
            }
            if (nodeIds.length >= 4) {
                this.createConnection(nodeIds[0], 'bottom', nodeIds[3], 'top');
            }
        }, 100);
    }

    testCollision() {
        // Add 5 nodes close together to test collision detection
        this.clearAll();
        const centerX = 400;
        const centerY = 300;
        
        // Add nodes in a tight cluster
        this.addNode(centerX - 60, centerY - 30);
        this.addNode(centerX + 20, centerY - 30);
        this.addNode(centerX - 20, centerY + 20);
        this.addNode(centerX + 60, centerY + 20);
        this.addNode(centerX, centerY);
        
        // Wait for collision resolution
        setTimeout(() => {
            console.log('Collision test completed - nodes should be automatically separated');
        }, 1000);
    }

    testLoadConfiguration() {
        // Simulate loading a configuration with overlapping nodes
        this.clearAll();
        
        const config = {
            nodes: [
                { id: 'node-1', x: 200, y: 200, title: 'HTTP', type: 'http' },
                { id: 'node-2', x: 220, y: 210, title: 'Parse', type: 'parse' }, // Overlapping
                { id: 'node-3', x: 400, y: 200, title: 'Transform', type: 'transform' },
                { id: 'node-4', x: 420, y: 210, title: 'Output', type: 'output' } // Overlapping
            ],
            connections: [
                { fromNodeId: 'node-1', fromSide: 'right', toNodeId: 'node-3', toSide: 'left' },
                { fromNodeId: 'node-2', fromSide: 'right', toNodeId: 'node-4', toSide: 'left' }
            ]
        };
        
        this.loadConfiguration(config);
    }

    loadConfiguration(config) {
        // Clear existing canvas
        this.clearAll();
        
        // Create nodes from config
        config.nodes.forEach(nodeConfig => {
            const node = {
                id: nodeConfig.id,
                x: nodeConfig.x,
                y: nodeConfig.y,
                width: nodeConfig.width || 120,
                height: nodeConfig.height || 60,
                title: nodeConfig.title,
                type: nodeConfig.type
            };
            this.nodes.set(node.id, node);
            this.renderNode(node);
        });
        
        // Resolve collisions with animation
        setTimeout(() => {
            this.nodes.forEach(node => {
                this.collisionDetector.resolveCollisions(node, this.nodes, node.id);
            });
        }, 100);
        
        // Create connections after delay
        if (config.connections) {
            setTimeout(() => {
                config.connections.forEach(connConfig => {
                    this.createConnection(
                        connConfig.fromNodeId,
                        connConfig.fromSide,
                        connConfig.toNodeId,
                        connConfig.toSide
                    );
                });
            }, 500);
        }
    }

    updateInfo() {
        const infoContent = document.getElementById('info-content');
        let infoHtml = `
            <p><strong>Etykiety:</strong> ${this.nodes.size}</p>
            <p><strong>Połączenia:</strong> ${this.connections.size}</p>
        `;
        
        if (this.selectedConnection) {
            const conn = this.connections.get(this.selectedConnection);
            if (conn) {
                infoHtml += `
                    <div class="connection-info" style="background: #dbeafe; border-left: 3px solid #3b82f6; margin-top: 0.5rem;">
                        <strong>Wybrane połączenie:</strong><br>
                        ${conn.fromNodeId} (${conn.fromSide}) → ${conn.toNodeId} (${conn.toSide})<br>
                        <small style="color: #64748b;">Kliknij i przeciągnij końce strzałki aby zmienić etykiety</small>
                    </div>
                `;
            }
        }
        
        if (this.connections.size > 0) {
            infoHtml += `
                <div class="connection-info" style="margin-top: 0.5rem;">
                    <strong>Wszystkie połączenia:</strong><br>
                    ${Array.from(this.connections.values()).map(c => 
                        `${c.fromNodeId} (${c.fromSide}) → ${c.toNodeId} (${c.toSide})`
                    ).join('<br>')}
                </div>
            `;
        }
        
        infoContent.innerHTML = infoHtml;
    }
}

// Initialize app
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new ConnectionDesignSystem();
    
    // Add initial test nodes
    setTimeout(() => {
        app.addNode(200, 150);
        app.addNode(400, 150);
        app.addNode(300, 300);
    }, 100);
});

