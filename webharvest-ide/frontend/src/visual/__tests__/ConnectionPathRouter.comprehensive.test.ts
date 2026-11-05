/**
 * ConnectionPathRouter - Comprehensive Tests
 * 
 * Testy sprawdzające wszystkie scenariusze routingu i obliczania arrowMetadata
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ConnectionPathRouter } from '../ConnectionPathRouter';
import type { Point, VisualNodeState, Side } from '../VisualSemanticModel';

describe('ConnectionPathRouter - Comprehensive Tests', () => {
    let router: ConnectionPathRouter;

    beforeEach(() => {
        router = new ConnectionPathRouter();
    });

    describe('calculateArrowMetadata - all scenarios', () => {
        it('should always return pointsTowardsNode=true after calculation', () => {
            // Test all 4 directions
            const scenarios = [
                {
                    name: 'segment points right, target right',
                    points: [{ x: 0, y: 0 }, { x: 100, y: 0 }],
                    endPoint: { x: 100, y: 0 },
                    targetCenter: { x: 200, y: 0 }
                },
                {
                    name: 'segment points right, target left (should reverse)',
                    points: [{ x: 0, y: 0 }, { x: 100, y: 0 }],
                    endPoint: { x: 100, y: 0 },
                    targetCenter: { x: 0, y: 0 }
                },
                {
                    name: 'segment points left, target left',
                    points: [{ x: 100, y: 0 }, { x: 0, y: 0 }],
                    endPoint: { x: 0, y: 0 },
                    targetCenter: { x: -50, y: 0 }
                },
                {
                    name: 'segment points left, target right (should reverse)',
                    points: [{ x: 100, y: 0 }, { x: 0, y: 0 }],
                    endPoint: { x: 0, y: 0 },
                    targetCenter: { x: 100, y: 0 }
                },
                {
                    name: 'segment points down, target down',
                    points: [{ x: 0, y: 0 }, { x: 0, y: 100 }],
                    endPoint: { x: 0, y: 100 },
                    targetCenter: { x: 0, y: 200 }
                },
                {
                    name: 'segment points down, target up (should reverse)',
                    points: [{ x: 0, y: 0 }, { x: 0, y: 100 }],
                    endPoint: { x: 0, y: 100 },
                    targetCenter: { x: 0, y: 0 }
                },
                {
                    name: 'segment points up, target up',
                    points: [{ x: 0, y: 100 }, { x: 0, y: 0 }],
                    endPoint: { x: 0, y: 0 },
                    targetCenter: { x: 0, y: -50 }
                },
                {
                    name: 'segment points up, target down (should reverse)',
                    points: [{ x: 0, y: 100 }, { x: 0, y: 0 }],
                    endPoint: { x: 0, y: 0 },
                    targetCenter: { x: 0, y: 100 }
                }
            ];

            scenarios.forEach(scenario => {
                const targetNode: VisualNodeState = {
                    nodeId: 'target',
                    pluginType: 'var',
                    position: { x: 0, y: 0 },
                    dimensions: { width: 100, height: 50 },
                    computedBounds: {
                        left: scenario.targetCenter.x - 50,
                        right: scenario.targetCenter.x + 50,
                        top: scenario.targetCenter.y - 25,
                        bottom: scenario.targetCenter.y + 25,
                        width: 100,
                        height: 50,
                        centerX: scenario.targetCenter.x,
                        centerY: scenario.targetCenter.y
                    },
                    connectionPoints: { inputs: [], outputs: [] },
                    variables: { defined: [], used: [] },
                    parentId: null,
                    childrenIds: [],
                    depth: 0,
                    isRoot: true,
                    visualState: {
                        selected: false,
                        hovered: false,
                        expanded: false,
                        collapsed: false,
                        dragging: false,
                        resizing: false,
                        error: false,
                        warning: false
                    },
                    layoutMetadata: {
                        zIndex: 0,
                        isManualPositioned: false
                    },
                    renderCache: {
                        element: null,
                        lastRenderTime: 0,
                        needsRerender: false
                    }
                };

                const metadata = (router as any).calculateArrowMetadata(
                    scenario.points,
                    scenario.endPoint,
                    targetNode
                );

                expect(metadata.pointsTowardsNode).toBe(true);
                if (!metadata.pointsTowardsNode) {
                    throw new Error(`Scenario: ${scenario.name} - pointsTowardsNode should always be true, but got ${metadata.pointsTowardsNode}`);
                }
            });
        });

        it('should correctly reverse angle when segment points away from target', () => {
            // Segment points right, target is left
            const points: Point[] = [
                { x: 100, y: 100 },
                { x: 200, y: 100 } // Points RIGHT
            ];
            const endPoint: Point = { x: 200, y: 100 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'var',
                position: { x: 0, y: 100 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 0,
                    right: 100,
                    top: 100,
                    bottom: 150,
                    width: 100,
                    height: 50,
                    centerX: 50, // LEFT of endPoint
                    centerY: 125
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const metadata = (router as any).calculateArrowMetadata(points, endPoint, targetNode);

            // Original angle would be 0 (pointing right), reversed should be PI (pointing left)
            expect(metadata.angle).toBeCloseTo(Math.PI, 5);
            expect(metadata.direction.x).toBeCloseTo(-1, 5);
            expect(metadata.direction.y).toBeCloseTo(0, 5);
            expect(metadata.pointsTowardsNode).toBe(true);
        });

        it('should not reverse angle when segment points towards target', () => {
            // Segment points right, target is right
            const points: Point[] = [
                { x: 0, y: 100 },
                { x: 100, y: 100 } // Points RIGHT
            ];
            const endPoint: Point = { x: 100, y: 100 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'var',
                position: { x: 200, y: 100 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 200,
                    right: 300,
                    top: 100,
                    bottom: 150,
                    width: 100,
                    height: 50,
                    centerX: 250, // RIGHT of endPoint
                    centerY: 125
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const metadata = (router as any).calculateArrowMetadata(points, endPoint, targetNode);

            // Angle should be 0 (pointing right), not reversed
            expect(metadata.angle).toBeCloseTo(0, 5);
            expect(metadata.direction.x).toBeCloseTo(1, 5);
            expect(metadata.direction.y).toBeCloseTo(0, 5);
            expect(metadata.pointsTowardsNode).toBe(true);
        });
    });

    describe('route + calculateArrowMetadata integration', () => {
        it('should route and calculate arrow metadata for horizontal connection', () => {
            const fromNode: VisualNodeState = {
                nodeId: 'from',
                pluginType: 'var',
                position: { x: 0, y: 100 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 0,
                    right: 100,
                    top: 100,
                    bottom: 150,
                    width: 100,
                    height: 50,
                    centerX: 50,
                    centerY: 125
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const toNode: VisualNodeState = {
                nodeId: 'to',
                pluginType: 'var',
                position: { x: 200, y: 100 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 200,
                    right: 300,
                    top: 100,
                    bottom: 150,
                    width: 100,
                    height: 50,
                    centerX: 250,
                    centerY: 125
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const fromPoint: Point = { x: 100, y: 125 }; // Right edge
            const toPoint: Point = { x: 200, y: 125 }; // Left edge

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'right', 'left', 'orthogonal');
            expect(path.length).toBeGreaterThan(0);

            const endPoint = path[path.length - 1];
            const arrowMetadata = router.calculateArrowMetadata(path, endPoint, toNode);

            expect(arrowMetadata.pointsTowardsNode).toBe(true);
            expect(arrowMetadata.endPoint).toEqual(endPoint);
        });

        it('should route and calculate arrow metadata for vertical connection', () => {
            const fromNode: VisualNodeState = {
                nodeId: 'from',
                pluginType: 'var',
                position: { x: 100, y: 0 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 100,
                    right: 200,
                    top: 0,
                    bottom: 50,
                    width: 100,
                    height: 50,
                    centerX: 150,
                    centerY: 25
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const toNode: VisualNodeState = {
                nodeId: 'to',
                pluginType: 'var',
                position: { x: 100, y: 200 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 100,
                    right: 200,
                    top: 200,
                    bottom: 250,
                    width: 100,
                    height: 50,
                    centerX: 150,
                    centerY: 225
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const fromPoint: Point = { x: 150, y: 50 }; // Bottom edge
            const toPoint: Point = { x: 150, y: 200 }; // Top edge

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'bottom', 'top', 'orthogonal');
            expect(path.length).toBeGreaterThan(0);

            const endPoint = path[path.length - 1];
            const arrowMetadata = router.calculateArrowMetadata(path, endPoint, toNode);

            expect(arrowMetadata.pointsTowardsNode).toBe(true);
        });

        it('should handle reverse connection (target to the left of source)', () => {
            const fromNode: VisualNodeState = {
                nodeId: 'from',
                pluginType: 'var',
                position: { x: 200, y: 100 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 200,
                    right: 300,
                    top: 100,
                    bottom: 150,
                    width: 100,
                    height: 50,
                    centerX: 250,
                    centerY: 125
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const toNode: VisualNodeState = {
                nodeId: 'to',
                pluginType: 'var',
                position: { x: 0, y: 100 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 0,
                    right: 100,
                    top: 100,
                    bottom: 150,
                    width: 100,
                    height: 50,
                    centerX: 50, // LEFT of fromNode
                    centerY: 125
                },
                connectionPoints: { inputs: [], outputs: [] },
                variables: { defined: [], used: [] },
                parentId: null,
                childrenIds: [],
                depth: 0,
                isRoot: true,
                visualState: {
                    selected: false,
                    hovered: false,
                    expanded: false,
                    collapsed: false,
                    dragging: false,
                    resizing: false,
                    error: false,
                    warning: false
                },
                layoutMetadata: {
                    zIndex: 0,
                    isManualPositioned: false
                },
                renderCache: {
                    element: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            const fromPoint: Point = { x: 200, y: 125 }; // Left edge (exit from left)
            const toPoint: Point = { x: 100, y: 125 }; // Right edge (enter from right)

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'left', 'right', 'orthogonal');
            expect(path.length).toBeGreaterThan(0);

            const endPoint = path[path.length - 1];
            const arrowMetadata = router.calculateArrowMetadata(path, endPoint, toNode);

            // CRITICAL: Even if path initially points away, arrowMetadata should have pointsTowardsNode=true
            expect(arrowMetadata.pointsTowardsNode).toBe(true);
        });
    });
});

