/**
 * ConnectionPointsCalculator - Obliczanie connection points (anchors/ports)
 * 
 * Oblicza pozycje connection points dla węzłów na podstawie:
 * - Backend model (InputPort/OutputPort jeśli dostępne)
 * - Rozmiaru węzła
 * - Pozycji węzła
 * 
 * Fallback: domyślne porty (jeden input z lewej, jeden output z prawej)
 */

import type { VisualNode, Plugin, InputPort, OutputPort } from '../types';
import type { ConnectionPoint, Point, Bounds, Side } from './VisualSemanticModel';

export class ConnectionPointsCalculator {
    private readonly DEFAULT_PORT_OFFSET = 10; // Odstęp od krawędzi węzła
    private readonly PORT_SPACING = 30; // Odstęp między portami na tej samej stronie

    /**
     * Oblicz connection points dla węzła
     * CRITICAL: Connection points must be exactly on the edge of the label/node
     */
    calculate(
        node: VisualNode,
        plugin: Plugin | null,
        position: Point,
        dimensions: { width: number; height: number }
    ): ConnectionPoint[] {
        const points: ConnectionPoint[] = [];
        const absoluteBounds = this.calculateBounds(position, dimensions);
        
        // CRITICAL: Connection points are calculated in RELATIVE coordinates (0,0,width,height)
        // They will be converted to absolute later via toAbsolutePosition
        // IMPORTANT: Points must be exactly on the edge (x=0 or x=width for left/right, y=0 or y=height for top/bottom)
        const relativeBounds: Bounds = {
            left: 0,
            right: dimensions.width,
            top: 0,
            bottom: dimensions.height,
            width: dimensions.width,
            height: dimensions.height,
            centerX: dimensions.width / 2,
            centerY: dimensions.height / 2
        };

        // Input ports
        if (plugin?.inputPorts && plugin.inputPorts.length > 0) {
            const inputPoints = this.fromInputPorts(plugin.inputPorts, relativeBounds);
            points.push(...inputPoints);
        }
        // ALWAYS add default input ports on all sides: 3 points per edge (2 extreme + 1 center)
        // This allows automatic selection of the best side and position
        const inputSides: Side[] = ['left', 'right', 'top', 'bottom'];
        inputSides.forEach(side => {
            points.push(...this.createThreePointsOnEdge(relativeBounds, side, 'input'));
        });

        // Output ports
        if (plugin?.outputPorts && plugin.outputPorts.length > 0) {
            const outputPoints = this.fromOutputPorts(plugin.outputPorts, relativeBounds);
            points.push(...outputPoints);
        }
        // ALWAYS add default output ports on all sides: 3 points per edge (2 extreme + 1 center)
        // This allows automatic selection of the best side and position
        const outputSides: Side[] = ['left', 'right', 'top', 'bottom'];
        outputSides.forEach(side => {
            points.push(...this.createThreePointsOnEdge(relativeBounds, side, 'output'));
        });

        // CRITICAL: Validate and snap all points to edges, add edgePosition metadata
        // Use relative bounds for snapping and edge position calculation
        return points.map(cp => {
            // Snap point to edge if not already on edge (using relative bounds)
            const snappedPoint = this.snapPointToEdge(cp.position, relativeBounds, cp.side);
            
            // Calculate edgePosition metadata (using relative bounds)
            const edgePosition = this.calculateEdgePosition(snappedPoint, relativeBounds, cp.side);
            
            // Initialize semanticSelection metadata (model semantyczny będzie to aktualizować)
            const semanticSelection = cp.semanticSelection || {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            };
            
            return {
                ...cp,
                position: snappedPoint, // Relative position (will be converted to absolute later)
                edgePosition,
                semanticSelection // SEMANTIC MODEL: Jedyne źródło prawdy o wyborze strony
            };
        });
    }

    /**
     * Oblicz connection points z InputPorts (z backend model)
     */
    fromInputPorts(ports: InputPort[], bounds: Bounds): ConnectionPoint[] {
        const points: ConnectionPoint[] = [];

        ports.forEach((port, index) => {
            const position = this.calculatePortPosition(bounds, 'left', index, ports.length);
            const edgePosition = this.calculateEdgePosition(position, bounds, 'left');
            
            points.push({
                id: `input-${port.name}`,
                name: port.name,
                side: 'left',
                position,
                type: 'input',
                portType: port.type,
                connected: false,
                description: port.description,
                edgePosition,
                semanticSelection: {
                    isSelected: false,
                    selectedForConnections: [],
                    selectionReason: '',
                    priority: 0,
                    spatialContext: {
                        pathLength: 0,
                        clearance: 0,
                        avoidsCollisions: false
                    }
                }
            });
        });

        return points;
    }

    /**
     * Oblicz connection points z OutputPorts (z backend model)
     */
    fromOutputPorts(ports: OutputPort[], bounds: Bounds): ConnectionPoint[] {
        const points: ConnectionPoint[] = [];

        ports.forEach((port, index) => {
            const position = this.calculatePortPosition(bounds, 'right', index, ports.length);
            const snappedPosition = this.snapPointToEdge(position, bounds, 'right');
            const edgePosition = this.calculateEdgePosition(snappedPosition, bounds, 'right');
            
            points.push({
                id: `output-${port.name}`,
                name: port.name,
                side: 'right',
                position: snappedPosition,
                type: 'output',
                portType: port.type,
                connected: false,
                description: port.description,
                edgePosition,
                semanticSelection: {
                    isSelected: false,
                    selectedForConnections: [],
                    selectionReason: '',
                    priority: 0,
                    spatialContext: {
                        pathLength: 0,
                        clearance: 0,
                        avoidsCollisions: false
                    }
                }
            });
        });

        return points;
    }

    /**
     * Utwórz 3 punkty na krawędzi: dwa skrajne (z offsetem od rogów) + jeden środkowy
     */
    private createThreePointsOnEdge(bounds: Bounds, side: Side, type: 'input' | 'output'): ConnectionPoint[] {
        const points: ConnectionPoint[] = [];
        const EDGE_OFFSET = 15; // Offset od rogów dla punktów skrajnych
        
        let startPos: Point;
        let centerPos: Point;
        let endPos: Point;
        
        switch (side) {
            case 'left':
                // Left edge: y varies, x = 0
                startPos = { x: 0, y: EDGE_OFFSET };
                centerPos = { x: 0, y: bounds.height / 2 };
                endPos = { x: 0, y: bounds.height - EDGE_OFFSET };
                break;
            case 'right':
                // Right edge: y varies, x = bounds.width
                startPos = { x: bounds.width, y: EDGE_OFFSET };
                centerPos = { x: bounds.width, y: bounds.height / 2 };
                endPos = { x: bounds.width, y: bounds.height - EDGE_OFFSET };
                break;
            case 'top':
                // Top edge: x varies, y = 0
                startPos = { x: EDGE_OFFSET, y: 0 };
                centerPos = { x: bounds.width / 2, y: 0 };
                endPos = { x: bounds.width - EDGE_OFFSET, y: 0 };
                break;
            case 'bottom':
                // Bottom edge: x varies, y = bounds.height
                startPos = { x: EDGE_OFFSET, y: bounds.height };
                centerPos = { x: bounds.width / 2, y: bounds.height };
                endPos = { x: bounds.width - EDGE_OFFSET, y: bounds.height };
                break;
            default:
                // Fallback to left
                startPos = { x: 0, y: EDGE_OFFSET };
                centerPos = { x: 0, y: bounds.height / 2 };
                endPos = { x: 0, y: bounds.height - EDGE_OFFSET };
        }
        
        // Ensure points are on edge
        const snappedStart = this.snapPointToEdge(startPos, bounds, side);
        const snappedCenter = this.snapPointToEdge(centerPos, bounds, side);
        const snappedEnd = this.snapPointToEdge(endPos, bounds, side);
        
        const edgePosStart = this.calculateEdgePosition(snappedStart, bounds, side);
        const edgePosCenter = this.calculateEdgePosition(snappedCenter, bounds, side);
        const edgePosEnd = this.calculateEdgePosition(snappedEnd, bounds, side);
        
        // Create three points: start (extreme), center, end (extreme)
        points.push({
            id: `${type}-default-${side}-start`,
            name: `${type}-${side}-start`,
            side,
            position: snappedStart,
            type,
            connected: false,
            edgePosition: edgePosStart,
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        });
        
        points.push({
            id: `${type}-default-${side}-center`,
            name: `${type}-${side}-center`,
            side,
            position: snappedCenter,
            type,
            connected: false,
            edgePosition: edgePosCenter,
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        });
        
        points.push({
            id: `${type}-default-${side}-end`,
            name: `${type}-${side}-end`,
            side,
            position: snappedEnd,
            type,
            connected: false,
            edgePosition: edgePosEnd,
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        });
        
        return points;
    }

    /**
     * Domyślny input port (dla określonej strony) - DEPRECATED: Use createThreePointsOnEdge instead
     */
    defaultInputPort(bounds: Bounds, side: Side): ConnectionPoint {
        // CRITICAL: Position must be exactly on edge AND at the center of that edge
        // bounds is already in relative coordinates (0,0,width,height)
        let position: Point;
        switch (side) {
            case 'left':
                position = { x: 0, y: bounds.height / 2 }; // Left edge, center Y (relative)
                break;
            case 'right':
                position = { x: bounds.width, y: bounds.height / 2 }; // Right edge, center Y (relative)
                break;
            case 'top':
                position = { x: bounds.width / 2, y: 0 }; // Top edge, center X (relative)
                break;
            case 'bottom':
                position = { x: bounds.width / 2, y: bounds.height }; // Bottom edge, center X (relative)
                break;
            default:
                position = { x: 0, y: bounds.height / 2 }; // Fallback to left
        }
        
        // Ensure point is on edge (using relative bounds)
        const snappedPosition = this.snapPointToEdge(position, bounds, side);
        const edgePosition = this.calculateEdgePosition(snappedPosition, bounds, side);
        
        return {
            id: `input-default-${side}`,
            name: `input-${side}`,
            side,
            position: snappedPosition, // Relative position (will be converted to absolute later)
            type: 'input',
            connected: false,
            edgePosition,
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        };
    }

    /**
     * Domyślny output port (dla określonej strony)
     */
    defaultOutputPort(bounds: Bounds, side: Side): ConnectionPoint {
        // CRITICAL: Position must be exactly on edge AND at the center of that edge
        // bounds is already in relative coordinates (0,0,width,height)
        let position: Point;
        switch (side) {
            case 'left':
                position = { x: 0, y: bounds.height / 2 }; // Left edge, center Y (relative)
                break;
            case 'right':
                position = { x: bounds.width, y: bounds.height / 2 }; // Right edge, center Y (relative)
                break;
            case 'top':
                position = { x: bounds.width / 2, y: 0 }; // Top edge, center X (relative)
                break;
            case 'bottom':
                position = { x: bounds.width / 2, y: bounds.height }; // Bottom edge, center X (relative)
                break;
            default:
                position = { x: bounds.width, y: bounds.height / 2 }; // Fallback to right
        }
        
        // Ensure point is on edge (using relative bounds)
        const snappedPosition = this.snapPointToEdge(position, bounds, side);
        const edgePosition = this.calculateEdgePosition(snappedPosition, bounds, side);
        
        return {
            id: `output-default-${side}`,
            name: `output-${side}`,
            side,
            position: snappedPosition, // Relative position (will be converted to absolute later)
            type: 'output',
            connected: false,
            edgePosition,
            semanticSelection: {
                isSelected: false,
                selectedForConnections: [],
                selectionReason: '',
                priority: 0,
                spatialContext: {
                    pathLength: 0,
                    clearance: 0,
                    avoidsCollisions: false
                }
            }
        };
    }

    /**
     * Oblicz pozycję portu na określonej stronie węzła
     * CRITICAL: Position must be exactly on edge (not offset)
     */
    private calculatePortPosition(
        bounds: Bounds,
        side: 'left' | 'right' | 'top' | 'bottom',
        index: number,
        totalPorts: number
    ): Point {
        const spacing = this.PORT_SPACING;

        // Oblicz pozycję Y (dla left/right) lub X (dla top/bottom)
        const totalSpacing = (totalPorts - 1) * spacing;
        const startOffset = (bounds.height - totalSpacing) / 2; // Dla left/right
        const startOffsetX = (bounds.width - totalSpacing) / 2; // Dla top/bottom

        let position: Point;
        switch (side) {
            case 'left':
                position = {
                    x: 0, // Exactly on left edge
                    y: startOffset + index * spacing
                };
                break;
            case 'right':
                position = {
                    x: bounds.width, // Exactly on right edge
                    y: startOffset + index * spacing
                };
                break;
            case 'top':
                position = {
                    x: startOffsetX + index * spacing,
                    y: 0 // Exactly on top edge
                };
                break;
            case 'bottom':
                position = {
                    x: startOffsetX + index * spacing,
                    y: bounds.height // Exactly on bottom edge
                };
                break;
        }

        // Ensure point is exactly on edge
        return this.snapPointToEdge(position, bounds, side);
    }

    /**
     * Oblicz bounds węzła na podstawie pozycji i rozmiaru
     */
    private calculateBounds(position: Point, dimensions: { width: number; height: number }): Bounds {
        return {
            left: position.x,
            right: position.x + dimensions.width,
            top: position.y,
            bottom: position.y + dimensions.height,
            width: dimensions.width,
            height: dimensions.height,
            centerX: position.x + dimensions.width / 2,
            centerY: position.y + dimensions.height / 2
        };
    }

    /**
     * Konwertuj relative position (względem węzła) na absolute position (na canvas)
     */
    toAbsolutePosition(relativePosition: Point, nodePosition: Point): Point {
        return {
            x: nodePosition.x + relativePosition.x,
            y: nodePosition.y + relativePosition.y
        };
    }

    /**
     * Validate that point is on edge (tolerance < 1px)
     */
    validatePointOnEdge(point: Point, bounds: Bounds, side: Side): boolean {
        const tolerance = 1; // 1px tolerance

        switch (side) {
            case 'left':
                return Math.abs(point.x - bounds.left) < tolerance;
            case 'right':
                return Math.abs(point.x - bounds.right) < tolerance;
            case 'top':
                return Math.abs(point.y - bounds.top) < tolerance;
            case 'bottom':
                return Math.abs(point.y - bounds.bottom) < tolerance;
            default:
                return false;
        }
    }

    /**
     * Snap point to edge (ensure it's exactly on the edge)
     */
    snapPointToEdge(point: Point, bounds: Bounds, side: Side): Point {
        const snapped: Point = { ...point };
        const tolerance = 0.1; // Very small tolerance for snapping

        switch (side) {
            case 'left':
                snapped.x = bounds.left;
                // Clamp Y to bounds
                snapped.y = Math.max(bounds.top, Math.min(bounds.bottom, snapped.y));
                break;
            case 'right':
                snapped.x = bounds.right;
                // Clamp Y to bounds
                snapped.y = Math.max(bounds.top, Math.min(bounds.bottom, snapped.y));
                break;
            case 'top':
                snapped.y = bounds.top;
                // Clamp X to bounds
                snapped.x = Math.max(bounds.left, Math.min(bounds.right, snapped.x));
                break;
            case 'bottom':
                snapped.y = bounds.bottom;
                // Clamp X to bounds
                snapped.x = Math.max(bounds.left, Math.min(bounds.right, snapped.x));
                break;
        }

        return snapped;
    }

    /**
     * Calculate edgePosition metadata for connection point
     */
    private calculateEdgePosition(point: Point, bounds: Bounds, side: Side): ConnectionPoint['edgePosition'] {
        const tolerance = 1; // 1px tolerance
        const isOnEdge = this.validatePointOnEdge(point, bounds, side);

        // Calculate edgeCoordinate (0-1, where 0.5 = center)
        let edgeCoordinate = 0.5; // Default to center
        switch (side) {
            case 'left':
            case 'right':
                // Coordinate along vertical edge (Y position)
                const edgeLength = bounds.height;
                const positionOnEdge = point.y - bounds.top;
                edgeCoordinate = edgeLength > 0 ? positionOnEdge / edgeLength : 0.5;
                break;
            case 'top':
            case 'bottom':
                // Coordinate along horizontal edge (X position)
                const edgeLengthX = bounds.width;
                const positionOnEdgeX = point.x - bounds.left;
                edgeCoordinate = edgeLengthX > 0 ? positionOnEdgeX / edgeLengthX : 0.5;
                break;
        }

        // Clamp to [0, 1]
        edgeCoordinate = Math.max(0, Math.min(1, edgeCoordinate));

        return {
            isOnEdge,
            edgeTolerance: isOnEdge ? 0 : this.calculateTolerance(point, bounds, side),
            edgeSide: side,
            edgeCoordinate
        };
    }

    /**
     * Calculate tolerance (distance from edge)
     */
    private calculateTolerance(point: Point, bounds: Bounds, side: Side): number {
        switch (side) {
            case 'left':
                return Math.abs(point.x - bounds.left);
            case 'right':
                return Math.abs(point.x - bounds.right);
            case 'top':
                return Math.abs(point.y - bounds.top);
            case 'bottom':
                return Math.abs(point.y - bounds.bottom);
            default:
                return Infinity;
        }
    }
}


