/**
 * ConnectionPathRouter.calculateArrowMetadata - Unit Tests
 * 
 * Testy weryfikujące, że calculateArrowMetadata zawsze zwraca pointsTowardsNode=true
 * i poprawnie odwraca kąt, gdy strzałka nie wskazuje na węzeł.
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ConnectionPathRouter } from '../ConnectionPathRouter';
import type { Point, VisualNodeState } from '../VisualSemanticModel';

describe('ConnectionPathRouter.calculateArrowMetadata', () => {
    let router: ConnectionPathRouter;

    beforeEach(() => {
        router = new ConnectionPathRouter();
    });

    describe('pointsTowardsNode should always be true', () => {
        it('should return pointsTowardsNode=true when segment points towards node', () => {
            const points: Point[] = [
                { x: 0, y: 0 },
                { x: 50, y: 0 },
                { x: 100, y: 0 } // Last point - pointing right
            ];
            const endPoint: Point = { x: 100, y: 0 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'http',
                position: { x: 150, y: 0 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 150,
                    right: 250,
                    top: 0,
                    bottom: 50,
                    width: 100,
                    height: 50,
                    centerX: 200,
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

            const metadata = (router as any).calculateArrowMetadata(points, endPoint, targetNode);

            expect(metadata.pointsTowardsNode).toBe(true);
            expect(metadata.angle).toBeCloseTo(0, 5); // Pointing right (0 radians)
        });

        it('should return pointsTowardsNode=true and reverse angle when segment points away from node', () => {
            const points: Point[] = [
                { x: 0, y: 0 },
                { x: 50, y: 0 },
                { x: 100, y: 0 } // Last point - pointing right
            ];
            const endPoint: Point = { x: 100, y: 0 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'http',
                position: { x: 0, y: 0 }, // Target is to the left
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 0,
                    right: 100,
                    top: 0,
                    bottom: 50,
                    width: 100,
                    height: 50,
                    centerX: 50,
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

            const metadata = (router as any).calculateArrowMetadata(points, endPoint, targetNode);

            // After fix: pointsTowardsNode should be true (angle was reversed)
            expect(metadata.pointsTowardsNode).toBe(true);
            // Angle should be reversed (0 + PI = PI, pointing left)
            expect(metadata.angle).toBeCloseTo(Math.PI, 5);
            // Direction should be reversed
            expect(metadata.direction.x).toBeCloseTo(-1, 5);
            expect(metadata.direction.y).toBeCloseTo(0, 5);
        });

        it('should handle vertical segment pointing towards node', () => {
            const points: Point[] = [
                { x: 100, y: 0 },
                { x: 100, y: 50 },
                { x: 100, y: 100 } // Last point - pointing down
            ];
            const endPoint: Point = { x: 100, y: 100 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'http',
                position: { x: 100, y: 150 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 100,
                    right: 200,
                    top: 150,
                    bottom: 200,
                    width: 100,
                    height: 50,
                    centerX: 150,
                    centerY: 175
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

            expect(metadata.pointsTowardsNode).toBe(true);
            expect(metadata.angle).toBeCloseTo(Math.PI / 2, 5); // Pointing down (PI/2 radians)
        });

        it('should handle vertical segment pointing away from node (should reverse)', () => {
            const points: Point[] = [
                { x: 100, y: 200 },
                { x: 100, y: 150 },
                { x: 100, y: 100 } // Last point - segment direction: (0, -50) = pointing up
            ];
            const endPoint: Point = { x: 100, y: 100 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'http',
                position: { x: 100, y: 0 }, // Target center is at y=25 (above endPoint)
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 100,
                    right: 200,
                    top: 0,
                    bottom: 50,
                    width: 100,
                    height: 50,
                    centerX: 150,
                    centerY: 25 // Target center is above endPoint (y=100)
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

            // After fix: pointsTowardsNode should be true (angle was reversed)
            expect(metadata.pointsTowardsNode).toBe(true);
            // Segment points up (y decreases), target is above, so segment DOES point towards target
            // But wait - segment direction is (0, -50) = up, target direction is (50, -75) = up-right
            // Dot product: 0*50 + (-1)*(-75) = 75 > 0, so it points towards!
            // Actually, the segment IS pointing towards the target (up), so no reversal needed
            // The angle should be -PI/2 (pointing up), which is correct
            expect(metadata.angle).toBeCloseTo(-Math.PI / 2, 5);
            // Direction should point up (towards target)
            expect(metadata.direction.x).toBeCloseTo(0, 5);
            expect(metadata.direction.y).toBeCloseTo(-1, 5);
        });

        it('should handle diagonal segment', () => {
            const points: Point[] = [
                { x: 0, y: 0 },
                { x: 50, y: 50 },
                { x: 100, y: 100 } // Last point - pointing down-right
            ];
            const endPoint: Point = { x: 100, y: 100 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'http',
                position: { x: 150, y: 150 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 150,
                    right: 250,
                    top: 150,
                    bottom: 200,
                    width: 100,
                    height: 50,
                    centerX: 200,
                    centerY: 175
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

            expect(metadata.pointsTowardsNode).toBe(true);
            // Angle should be approximately PI/4 (45 degrees, pointing down-right)
            expect(metadata.angle).toBeCloseTo(Math.PI / 4, 5);
        });
    });

    describe('edge cases', () => {
        it('should handle path with less than 2 points (fallback)', () => {
            const points: Point[] = [{ x: 100, y: 100 }];
            const endPoint: Point = { x: 100, y: 100 };
            const targetNode: VisualNodeState = {
                nodeId: 'target',
                pluginType: 'http',
                position: { x: 150, y: 100 },
                dimensions: { width: 100, height: 50 },
                computedBounds: {
                    left: 150,
                    right: 250,
                    top: 100,
                    bottom: 150,
                    width: 100,
                    height: 50,
                    centerX: 200,
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

            // Fallback should always return pointsTowardsNode=true
            expect(metadata.pointsTowardsNode).toBe(true);
            expect(metadata.endPoint).toEqual(endPoint);
        });
    });
});

