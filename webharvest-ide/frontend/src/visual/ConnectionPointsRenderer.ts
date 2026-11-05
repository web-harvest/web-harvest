/**
 * ConnectionPointsRenderer - Renderuje wizualne wskaźniki connection points
 * 
 * Renderuje connection points jako małe kółka na krawędziach węzłów.
 * Punkty są widoczne tylko gdy węzeł jest zaznaczony LUB podczas przeciągania połączenia.
 * Model semantyczny jest jedynym źródłem prawdy o tym, które punkty są aktywne.
 */

import type { VisualSemanticModelManager } from './VisualSemanticModelManager';
import type { ConnectionPoint, Point, Side } from './VisualSemanticModel';

export class ConnectionPointsRenderer {
    private svgElement: SVGSVGElement;
    private modelManager: VisualSemanticModelManager;
    private canvasElement: HTMLElement; // Canvas container for HTML-based connection points
    private visibleNodes: Set<string> = new Set(); // Node IDs for which points should be visible
    private renderedPoints: Map<string, HTMLElement> = new Map(); // pointId -> HTML div element (NOT SVG!)
    private highlightedPoints: Set<string> = new Set(); // pointIds that should be highlighted
    private connectionPointsContainer: HTMLElement | null = null; // Container for all connection points (HTML, rendered on top)

    private scrollContainer: HTMLElement | null = null;
    private scrollListener: (() => void) | null = null;

    constructor(
        svgElement: SVGSVGElement,
        modelManager: VisualSemanticModelManager,
        canvasElement: HTMLElement
    ) {
        this.svgElement = svgElement;
        this.modelManager = modelManager;
        this.canvasElement = canvasElement;

        // Create dedicated HTML container for connection points (rendered on top of everything)
        this.createConnectionPointsContainer();
        
        // CRITICAL: Find scroll container and listen for scroll events
        // When canvas is scrolled, connection points need to be recalculated
        this.setupScrollListener();
    }
    
    /**
     * Setup scroll listener to recalculate connection points when canvas is scrolled
     * Canvas can be scrolled in any direction (up, down, left, right)
     */
    private setupScrollListener(): void {
        // Find scroll container (parent with overflow: auto/scroll)
        let parent = this.canvasElement.parentElement;
        while (parent) {
            const style = window.getComputedStyle(parent);
            if (style.overflow === 'auto' || style.overflow === 'scroll' || 
                style.overflowX === 'auto' || style.overflowX === 'scroll' ||
                style.overflowY === 'auto' || style.overflowY === 'scroll') {
                this.scrollContainer = parent;
                break;
            }
            parent = parent.parentElement;
        }
        
        if (this.scrollContainer) {
            // CRITICAL: When canvas is scrolled, connection points positions change
            // getBoundingClientRect() automatically accounts for scroll, but we need to
            // trigger recalculation of connections to ensure they're still correct
            this.scrollListener = () => {
                // Debounce scroll events
                if (this.scrollTimeout) {
                    clearTimeout(this.scrollTimeout);
                }
                this.scrollTimeout = setTimeout(() => {
                    // CRITICAL: Recalculate pending connections after scroll
                    // This ensures connections are recalculated with new anchor positions
                    if (this.modelManager && (this.modelManager as any).recalculateAllPendingConnectionsSync) {
                        (this.modelManager as any).recalculateAllPendingConnectionsSync();
                    }
                }, 50); // Debounce 50ms
            };
            
            this.scrollContainer.addEventListener('scroll', this.scrollListener, { passive: true });
        }
    }
    
    private scrollTimeout: NodeJS.Timeout | null = null;
    
    /**
     * Cleanup scroll listener
     */
    destroy(): void {
        if (this.scrollContainer && this.scrollListener) {
            this.scrollContainer.removeEventListener('scroll', this.scrollListener);
            this.scrollListener = null;
        }
        if (this.scrollTimeout) {
            clearTimeout(this.scrollTimeout);
            this.scrollTimeout = null;
        }
    }

    /**
     * Create a dedicated HTML container for connection points that will be rendered on top
     * Using HTML instead of SVG ensures connection points are always visible above nodes
     */
    private createConnectionPointsContainer(): void {
        // Check if container already exists
        let container = this.canvasElement.querySelector('#connection-points-overlay') as HTMLElement;
        if (container) {
            this.connectionPointsContainer = container;
            return;
        }

        // Create container
        container = document.createElement('div');
        container.id = 'connection-points-overlay';
        container.style.position = 'absolute';
        container.style.top = '0';
        container.style.left = '0';
        container.style.width = '100%';
        container.style.height = '100%';
        container.style.pointerEvents = 'none'; // Allow clicks to pass through to nodes
        container.style.zIndex = '1000'; // VERY HIGH z-index to be on top of everything
        container.style.overflow = 'visible';

        // Append to canvas (after SVG)
        this.canvasElement.appendChild(container);
        this.connectionPointsContainer = container;
    }

    /**
     * Pokaż connection points dla węzła
     * CRITICAL: Center points są zawsze widoczne dla wszystkich węzłów
     */
    showForNode(nodeId: string): void {
        console.log(`[ConnectionPointsRenderer] 🔵 showForNode CALLED: ${nodeId}`);
        this.visibleNodes.add(nodeId);
        console.log(`[ConnectionPointsRenderer] ✅ Added to visibleNodes. Count: ${this.visibleNodes.size}`);
        console.log(`[ConnectionPointsRenderer] 🔍 About to call renderAll()...`);
        this.renderAll();
        console.log(`[ConnectionPointsRenderer] ✅ renderAll() completed`);
    }
    
    /**
     * Pokaż center points dla wszystkich węzłów
     * CRITICAL: Center points są zawsze widoczne (nie tylko dla wybranych węzłów)
     */
    showAllCenterPoints(): void {
        const model = this.modelManager.getModel();
        if (!model) {
            console.warn(`[ConnectionPointsRenderer] ⚠️ No model available for showAllCenterPoints`);
            return;
        }
        
        // Dodaj wszystkie węzły do visibleNodes
        model.workflow.allNodes.forEach((nodeState, nodeId) => {
            this.visibleNodes.add(nodeId);
        });
        
        console.log(`[ConnectionPointsRenderer] ✅ showAllCenterPoints: Showing center points for ${this.visibleNodes.size} nodes`);
        this.renderAll();
    }

    /**
     * Ukryj connection points dla węzła
     */
    hideForNode(nodeId: string): void {
        this.visibleNodes.delete(nodeId);
        this.renderAll();
    }

    /**
     * Pokaż connection points dla wielu węzłów (podczas przeciągania)
     */
    showForNodes(nodeIds: string[]): void {
        nodeIds.forEach(id => this.visibleNodes.add(id));
        this.renderAll();
    }

    /**
     * Ukryj wszystkie connection points
     */
    hideAll(): void {
        this.visibleNodes.clear();
        this.renderAll();
    }

    /**
     * Podświetl konkretny punkt
     */
    highlightPoint(nodeId: string, pointId: string): void {
        const fullPointId = `${nodeId}/${pointId}`;
        this.highlightedPoints.add(fullPointId);
        this.updatePointStyle(fullPointId);
    }

    /**
     * Usuń podświetlenie punktu
     */
    unhighlightPoint(nodeId: string, pointId: string): void {
        const fullPointId = `${nodeId}/${pointId}`;
        this.highlightedPoints.delete(fullPointId);
        this.updatePointStyle(fullPointId);
    }

    /**
     * Usuń wszystkie podświetlenia
     */
    clearHighlights(): void {
        this.highlightedPoints.clear();
        this.renderAll();
    }

    /**
     * Renderuj wszystkie widoczne connection points
     */
    renderAll(): void {
        console.log(`[ConnectionPointsRenderer] 🔄 renderAll() START - visibleNodes: ${this.visibleNodes.size}`);

        // Remove all existing points
        const removedCount = this.renderedPoints.size;
        this.renderedPoints.forEach((circle, pointId) => {
            if (circle.parentElement) {
                circle.parentElement.removeChild(circle);
            }
        });
        this.renderedPoints.clear();

        const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLog) {
            console.log(`[ConnectionPointsRenderer] 🗑️ Removed ${removedCount} existing points`);
        }

        if (this.visibleNodes.size === 0) {
            console.log(`[ConnectionPointsRenderer] ⚠️ No visible nodes - skipping render`);
            return;
        }

        const model = this.modelManager.getModel();
        if (!model) {
            console.error(`[ConnectionPointsRenderer] ❌ No model available`);
            return;
        }
        console.log(`[ConnectionPointsRenderer] ✅ Model retrieved: ${model.workflow.allNodes.size} nodes`);

        // Render points for each visible node
        let totalRendered = 0;
        this.visibleNodes.forEach(nodeId => {
            const nodeState = model.workflow.allNodes.get(nodeId);
            if (!nodeState) {
                console.warn(`[ConnectionPointsRenderer] ⚠️ Node ${nodeId} not found in model`);
                return;
            }

            // CRITICAL: Get connection points from semantic model
            // These points contain exact positions, semanticSelection metadata, and all spatial context
            // The model is the SINGLE SOURCE OF TRUTH for what should be rendered on the canvas
            const allPoints = [...nodeState.connectionPoints.inputs, ...nodeState.connectionPoints.outputs];
            
            // CRITICAL: Renderuj TYLKO center points (środkowe punkty) - zawsze widoczne
            // Na każdej krawędzi są 3 punkty: start, center, end
            // Tylko center points są widoczne i używane do łączenia
            const centerPoints = allPoints.filter(point => point.id.includes('-center'));
            
            // DEBUG: Sprawdź, czy są center points na wszystkich 4 stronach
            const sides = new Set(centerPoints.map(p => p.side));
            const expectedSides: Side[] = ['left', 'right', 'top', 'bottom'];
            const missingSides = expectedSides.filter(s => !sides.has(s));
            
            if (missingSides.length > 0) {
                console.warn(`[ConnectionPointsRenderer] ⚠️ Node ${nodeId}: Missing center points on sides: ${missingSides.join(', ')}`);
            }
            console.log(`[ConnectionPointsRenderer] 📍 Node ${nodeId}: ${centerPoints.length} center points on sides: ${Array.from(sides).join(', ')}`);
            
            centerPoints.forEach(point => {
                // Render each center point using EXACT position from semantic model
                // CRITICAL: Center points są widoczne na WSZYSTKICH krawędziach (left, right, top, bottom)
                this.renderPoint(nodeId, point);
                totalRendered++;
            });
        });

        console.log(`[ConnectionPointsRenderer] ✅ renderAll() END - Rendered ${totalRendered} connection points total`);
        console.log(`[ConnectionPointsRenderer] 📊 SVG now has ${this.svgElement?.children.length || 0} children`);
        
        // CRITICAL: After all anchors are registered, trigger full recalculation of pending connections
        // Use microtask (Promise.resolve) for faster retry than RAF - DOM is ready after appendChild
        if (this.modelManager && totalRendered > 0) {
            // Microtask is faster than RAF and runs after current sync code but before paint
            Promise.resolve().then(() => {
                if ((this.modelManager as any).recalculateAllPendingConnectionsSync) {
                    (this.modelManager as any).recalculateAllPendingConnectionsSync();
                    console.log(`[ConnectionPointsRenderer] ✅ Triggered recalculation of pending connections (microtask)`);
                }
            });
        }
    }

    /**
     * Renderuj pojedynczy connection point
     */
    private renderPoint(nodeId: string, point: ConnectionPoint): void {
        const fullPointId = `${nodeId}/${point.id}`;

        // CRITICAL: Get node state from semantic model - SINGLE SOURCE OF TRUTH
        // All positions, dimensions, and connection points come from the semantic model
        // This ensures connection points are EXACTLY where the model says they should be
        const model = this.modelManager.getModel();
        if (!model) {
            return;
        }
        const nodeState = model.workflow.allNodes.get(nodeId);
        if (!nodeState) {
            return;
        }

        // CRITICAL: Calculate absolute position from semantic model
        // point.position is RELATIVE to node (from semantic model)
        // nodeState.position is ABSOLUTE on canvas (from semantic model)
        // This ensures connection points are EXACTLY where the semantic model says they should be
        const absolutePosition = this.toAbsolutePosition(point.position, nodeState.position);

        // Create HTML div element (NOT SVG!) - this ensures it's always on top
        const pointElement = document.createElement('div');
        pointElement.className = 'connection-point';
        pointElement.setAttribute('data-node-id', nodeId);
        pointElement.setAttribute('data-point-id', point.id);
        pointElement.setAttribute('data-point-type', point.type);
        pointElement.setAttribute('data-side', point.side);

        // CRITICAL: Check if this is a center point (only center points can be used for connections)
        const isCenterPoint = point.id.includes('-center');
        const isConnectable = isCenterPoint; // Only center points are connectable

        // Position absolutely - use EXACT position from semantic model
        // CRITICAL: Connection points must be at EXACTLY the same position as anchor points in SVG
        // This ensures lines connect to the visible center points
        pointElement.style.position = 'absolute';
        pointElement.style.left = `${absolutePosition.x}px`;
        pointElement.style.top = `${absolutePosition.y}px`;
        // CRITICAL: Center points są ZAWSZE widoczne na wszystkich krawędziach (left, right, top, bottom)
        // Użyj większego rozmiaru dla lepszej widoczności
        pointElement.style.width = isConnectable ? '32px' : '20px'; // Większe dla center points (32px)
        pointElement.style.height = isConnectable ? '32px' : '20px';
        // CRITICAL: marginLeft/marginTop centrują punkt na pozycji absolutePosition
        // To oznacza, że środek diva jest w (absolutePosition.x, absolutePosition.y)
        // SVG path również zaczyna się/kończy w (absolutePosition.x, absolutePosition.y)
        // Więc linia powinna przechodzić przez środek diva!
        pointElement.style.marginLeft = isConnectable ? '-16px' : '-10px'; // Center on point (16px = 32px/2)
        pointElement.style.marginTop = isConnectable ? '-16px' : '-10px';
        pointElement.style.borderRadius = '50%';
        pointElement.style.pointerEvents = isConnectable ? 'all' : 'none'; // Only center points are clickable
        pointElement.style.cursor = isConnectable ? 'pointer' : 'default';
        pointElement.style.boxSizing = 'border-box';
        pointElement.style.transition = 'all 0.2s ease';
        pointElement.setAttribute('data-connectable', isConnectable.toString());

        // Set style based on selection state
        this.updatePointStyle(fullPointId, pointElement, point);

        // Add to connection points container (HTML, rendered on top)
        if (!this.connectionPointsContainer) {
            console.error(`[ConnectionPointsRenderer] renderPoint: connectionPointsContainer is null!`);
            return;
        }

        try {
            this.connectionPointsContainer.appendChild(pointElement);
            this.renderedPoints.set(fullPointId, pointElement);

            // CRITICAL: Register anchor element with model manager (SYNCHRONOUS)
            // This allows model manager to convert HTML coords to SVG coords
            // Use synchronous registration to avoid race conditions
            // DOM is ready because we just appended the element
            if (this.modelManager && isConnectable) {
                // Register anchor for center points only (connectable points)
                // Synchronous registration triggers immediate recalculation if SVG is ready
                (this.modelManager as any).registerAnchor?.(nodeId, point.id, pointElement);
            }

            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.log(`[ConnectionPointsRenderer] ✅ Added connection point for ${fullPointId} at (${absolutePosition.x}, ${absolutePosition.y})`);
            }
        } catch (error) {
            console.error(`[ConnectionPointsRenderer] renderPoint: Error adding point to container:`, error);
        }
    }

    /**
     * Aktualizuj styl punktu na podstawie stanu
     */
    private updatePointStyle(fullPointId: string, element?: HTMLElement, point?: ConnectionPoint): void {
        const pointElement = element || this.renderedPoints.get(fullPointId);
        if (!pointElement) {
            return;
        }

        // Get point from model if not provided
        if (!point) {
            const [nodeId, pointId] = fullPointId.split('/');
            const model = this.modelManager.getModel();
            if (!model) {
                return;
            }
            const nodeState = model.workflow.allNodes.get(nodeId);
            if (!nodeState) {
                return;
            }
            const allPoints = [...nodeState.connectionPoints.inputs, ...nodeState.connectionPoints.outputs];
            point = allPoints.find(p => p.id === pointId);
            if (!point) {
                return;
            }
        }

        const isSelected = point.semanticSelection?.isSelected || false;
        const isHighlighted = this.highlightedPoints.has(fullPointId);

        // Check if this is a center point (only center points can be used for connections)
        const isCenterPoint = point.id.includes('-center');
        const isConnectable = isCenterPoint;

        // CRITICAL: Center points są ZAWSZE widoczne na wszystkich krawędziach
        // Default style - use VERY bright colors and LARGE size for maximum visibility
        // Non-connectable points (start/end) are smaller and less visible
        let backgroundColor = isConnectable ? '#3b82f6' : '#9ca3af'; // blue-500 for connectable (BARDZO widoczne), gray-400 for non-connectable
        let borderColor = '#ffffff'; // white
        let borderWidth = isConnectable ? '3px' : '2px';
        let size = isConnectable ? '32px' : '20px'; // Większe dla center points (32px) - BARDZO widoczne
        let opacity = isConnectable ? '1' : '0.3'; // Center points zawsze widoczne (opacity 1)

        // Selected point style - VERY visible (only for center points)
        if (isSelected && isConnectable) {
            backgroundColor = '#22c55e'; // green-500 (VERY bright green)
            borderColor = '#ffffff'; // white
            borderWidth = '4px';
            size = '36px'; // 36px diameter when selected (MUCH larger)
            opacity = '1';
        }

        // Highlighted point style (during drag) - MOST visible (only for center points)
        if (isHighlighted && isConnectable) {
            backgroundColor = '#ef4444'; // red-500 (VERY bright red)
            borderColor = '#ffffff'; // white
            borderWidth = '4px';
            size = '40px'; // 40px diameter when highlighted (LARGEST)
            opacity = '1';
        }

        // CRITICAL: Set all styles for HTML element (inspired by NiFi's port rendering)
        // Use EXACT position from semantic model - point.position is relative, nodeState.position is absolute
        pointElement.style.backgroundColor = backgroundColor;
        pointElement.style.border = `${borderWidth} solid ${borderColor}`;
        pointElement.style.width = size;
        pointElement.style.height = size;
        pointElement.style.marginLeft = `-${parseInt(size) / 2}px`; // Re-center based on new size
        pointElement.style.marginTop = `-${parseInt(size) / 2}px`;
        pointElement.style.display = 'block';
        pointElement.style.visibility = 'visible';
        pointElement.style.opacity = opacity;

        // Add box shadow for glow effect (similar to NiFi's hover effects)
        // Only center points get glow effect
        if ((isSelected || isHighlighted) && isConnectable) {
            pointElement.style.boxShadow = `0 0 10px ${backgroundColor}, 0 0 20px ${backgroundColor}40`;
        } else {
            pointElement.style.boxShadow = isConnectable ? '0 2px 4px rgba(0,0,0,0.3)' : 'none';
        }
    }

    /**
     * Konwertuj relative position na absolute position
     * 
     * CRITICAL: This converts semantic model's relative positions to absolute canvas positions
     * - relativePosition: Position relative to node (from ConnectionPoint.position in semantic model)
     * - nodePosition: Absolute position of node on canvas (from VisualNodeState.position in semantic model)
     * 
     * The semantic model is the SINGLE SOURCE OF TRUTH for all positions
     */
    private toAbsolutePosition(relativePosition: Point, nodePosition: Point): Point {
        // Both positions come from semantic model - this ensures exact correlation
        return {
            x: nodePosition.x + relativePosition.x,
            y: nodePosition.y + relativePosition.y
        };
    }

    /**
     * Aktualizuj pozycje connection points (np. gdy węzeł się przesunął)
     */
    /**
     * Update positions of all rendered connection points from semantic model
     * 
     * CRITICAL: This ensures rendered points always match the semantic model
     * Called when nodes are moved or resized - positions are recalculated from model
     */
    updatePositions(): void {
        this.renderedPoints.forEach((pointElement, fullPointId) => {
            const [nodeId, pointId] = fullPointId.split('/');

            // Get latest state from semantic model - SINGLE SOURCE OF TRUTH
            const model = this.modelManager.getModel();
            if (!model) {
                return;
            }
            const nodeState = model.workflow.allNodes.get(nodeId);
            if (!nodeState) {
                return;
            }

            // Find point in semantic model
            const allPoints = [...nodeState.connectionPoints.inputs, ...nodeState.connectionPoints.outputs];
            const point = allPoints.find(p => p.id === pointId);
            if (!point) {
                return;
            }

            // Calculate absolute position from semantic model
            const absolutePosition = this.toAbsolutePosition(point.position, nodeState.position);

            // Update HTML element position to match semantic model exactly
            pointElement.style.left = `${absolutePosition.x}px`;
            pointElement.style.top = `${absolutePosition.y}px`;

            // Re-center based on current size
            const currentSize = parseInt(pointElement.style.width || '30px');
            pointElement.style.marginLeft = `-${currentSize / 2}px`;
            pointElement.style.marginTop = `-${currentSize / 2}px`;
        });
    }
}

