/**
 * ConnectionPathRouter.route - Comprehensive Routing Tests
 * 
 * Testy sprawdzające, że routing ZAWSZE tworzy ścieżki, gdzie ostatni segment wskazuje na target node
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ConnectionPathRouter } from '../ConnectionPathRouter';
import type { Point, VisualNodeState, Side } from '../VisualSemanticModel';

function createNodeState(
    nodeId: string,
    position: Point,
    dimensions: { width: number; height: number }
): VisualNodeState {
    return {
        nodeId,
        pluginType: 'var',
        position,
        dimensions,
        computedBounds: {
            left: position.x,
            right: position.x + dimensions.width,
            top: position.y,
            bottom: position.y + dimensions.height,
            width: dimensions.width,
            height: dimensions.height,
            centerX: position.x + dimensions.width / 2,
            centerY: position.y + dimensions.height / 2
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
}

describe('ConnectionPathRouter.route - Comprehensive Routing Tests', () => {
    let router: ConnectionPathRouter;

    beforeEach(() => {
        router = new ConnectionPathRouter();
    });

    describe('Last segment must point towards target node', () => {
        it('should route horizontal connection (right to left) with last segment pointing towards target', () => {
            const fromNode = createNodeState('from', { x: 0, y: 100 }, { width: 100, height: 50 });
            const toNode = createNodeState('to', { x: 200, y: 100 }, { width: 100, height: 50 });

            const fromPoint: Point = { x: 100, y: 125 }; // Right edge
            const toPoint: Point = { x: 200, y: 125 }; // Left edge

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'right', 'left', 'orthogonal');

            expect(path.length).toBeGreaterThanOrEqual(2);

            // Verify last segment points towards target
            const lastPoint = path[path.length - 1];
            const secondLast = path[path.length - 2];
            const segmentDir = {
                x: lastPoint.x - secondLast.x,
                y: lastPoint.y - secondLast.y
            };
            const segmentLength = Math.sqrt(segmentDir.x ** 2 + segmentDir.y ** 2);
            const normalizedSegment = segmentLength > 0
                ? { x: segmentDir.x / segmentLength, y: segmentDir.y / segmentLength }
                : { x: 1, y: 0 };

            const targetCenter = {
                x: toNode.computedBounds.centerX,
                y: toNode.computedBounds.centerY
            };
            const toTarget = {
                x: targetCenter.x - lastPoint.x,
                y: targetCenter.y - lastPoint.y
            };
            const targetLength = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2);
            const normalizedTarget = targetLength > 0
                ? { x: toTarget.x / targetLength, y: toTarget.y / targetLength }
                : { x: 1, y: 0 };

            const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
            expect(dotProduct).toBeGreaterThan(0);
        });

        it('should route reverse horizontal connection (left to right) with last segment pointing towards target', () => {
            const fromNode = createNodeState('from', { x: 200, y: 100 }, { width: 100, height: 50 });
            const toNode = createNodeState('to', { x: 0, y: 100 }, { width: 100, height: 50 });

            const fromPoint: Point = { x: 200, y: 125 }; // Left edge
            const toPoint: Point = { x: 100, y: 125 }; // Right edge

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'left', 'right', 'orthogonal');

            expect(path.length).toBeGreaterThanOrEqual(2);

            // Verify last segment points towards target
            const lastPoint = path[path.length - 1];
            const secondLast = path[path.length - 2];
            const segmentDir = {
                x: lastPoint.x - secondLast.x,
                y: lastPoint.y - secondLast.y
            };
            const segmentLength = Math.sqrt(segmentDir.x ** 2 + segmentDir.y ** 2);
            const normalizedSegment = segmentLength > 0
                ? { x: segmentDir.x / segmentLength, y: segmentDir.y / segmentLength }
                : { x: 1, y: 0 };

            const targetCenter = {
                x: toNode.computedBounds.centerX,
                y: toNode.computedBounds.centerY
            };
            const toTarget = {
                x: targetCenter.x - lastPoint.x,
                y: targetCenter.y - lastPoint.y
            };
            const targetLength = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2);
            const normalizedTarget = targetLength > 0
                ? { x: toTarget.x / targetLength, y: toTarget.y / targetLength }
                : { x: 1, y: 0 };

            const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
            expect(dotProduct).toBeGreaterThan(0);
        });

        it('should route vertical connection (top to bottom) with last segment pointing towards target', () => {
            const fromNode = createNodeState('from', { x: 100, y: 0 }, { width: 100, height: 50 });
            const toNode = createNodeState('to', { x: 100, y: 200 }, { width: 100, height: 50 });

            const fromPoint: Point = { x: 150, y: 50 }; // Bottom edge
            const toPoint: Point = { x: 150, y: 200 }; // Top edge

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'bottom', 'top', 'orthogonal');

            expect(path.length).toBeGreaterThanOrEqual(2);

            // Verify last segment points towards target
            const lastPoint = path[path.length - 1];
            const secondLast = path[path.length - 2];
            const segmentDir = {
                x: lastPoint.x - secondLast.x,
                y: lastPoint.y - secondLast.y
            };
            const segmentLength = Math.sqrt(segmentDir.x ** 2 + segmentDir.y ** 2);
            const normalizedSegment = segmentLength > 0
                ? { x: segmentDir.x / segmentLength, y: segmentDir.y / segmentLength }
                : { x: 1, y: 0 };

            const targetCenter = {
                x: toNode.computedBounds.centerX,
                y: toNode.computedBounds.centerY
            };
            const toTarget = {
                x: targetCenter.x - lastPoint.x,
                y: targetCenter.y - lastPoint.y
            };
            const targetLength = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2);
            const normalizedTarget = targetLength > 0
                ? { x: toTarget.x / targetLength, y: toTarget.y / targetLength }
                : { x: 1, y: 0 };

            const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
            expect(dotProduct).toBeGreaterThan(0);
        });

        it('should route reverse vertical connection (bottom to top) with last segment pointing towards target', () => {
            const fromNode = createNodeState('from', { x: 100, y: 200 }, { width: 100, height: 50 });
            const toNode = createNodeState('to', { x: 100, y: 0 }, { width: 100, height: 50 });

            const fromPoint: Point = { x: 150, y: 200 }; // Top edge
            const toPoint: Point = { x: 150, y: 50 }; // Bottom edge

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'top', 'bottom', 'orthogonal');

            expect(path.length).toBeGreaterThanOrEqual(2);

            // Verify last segment points towards target
            const lastPoint = path[path.length - 1];
            const secondLast = path[path.length - 2];
            const segmentDir = {
                x: lastPoint.x - secondLast.x,
                y: lastPoint.y - secondLast.y
            };
            const segmentLength = Math.sqrt(segmentDir.x ** 2 + segmentDir.y ** 2);
            const normalizedSegment = segmentLength > 0
                ? { x: segmentDir.x / segmentLength, y: segmentDir.y / segmentLength }
                : { x: 1, y: 0 };

            const targetCenter = {
                x: toNode.computedBounds.centerX,
                y: toNode.computedBounds.centerY
            };
            const toTarget = {
                x: targetCenter.x - lastPoint.x,
                y: targetCenter.y - lastPoint.y
            };
            const targetLength = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2);
            const normalizedTarget = targetLength > 0
                ? { x: toTarget.x / targetLength, y: toTarget.y / targetLength }
                : { x: 1, y: 0 };

            const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
            expect(dotProduct).toBeGreaterThan(0);
        });

        it('should route diagonal connection (top-right to bottom-left) with last segment pointing towards target', () => {
            const fromNode = createNodeState('from', { x: 0, y: 0 }, { width: 100, height: 50 });
            const toNode = createNodeState('to', { x: 200, y: 200 }, { width: 100, height: 50 });

            const fromPoint: Point = { x: 100, y: 25 }; // Right edge
            const toPoint: Point = { x: 200, y: 225 }; // Left edge

            const path = router.route(fromNode, toNode, fromPoint, toPoint, 'right', 'left', 'orthogonal');

            expect(path.length).toBeGreaterThanOrEqual(2);

            // Verify last segment points towards target
            const lastPoint = path[path.length - 1];
            const secondLast = path[path.length - 2];
            const segmentDir = {
                x: lastPoint.x - secondLast.x,
                y: lastPoint.y - secondLast.y
            };
            const segmentLength = Math.sqrt(segmentDir.x ** 2 + segmentDir.y ** 2);
            const normalizedSegment = segmentLength > 0
                ? { x: segmentDir.x / segmentLength, y: segmentDir.y / segmentLength }
                : { x: 1, y: 0 };

            const targetCenter = {
                x: toNode.computedBounds.centerX,
                y: toNode.computedBounds.centerY
            };
            const toTarget = {
                x: targetCenter.x - lastPoint.x,
                y: targetCenter.y - lastPoint.y
            };
            const targetLength = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2);
            const normalizedTarget = targetLength > 0
                ? { x: toTarget.x / targetLength, y: toTarget.y / targetLength }
                : { x: 1, y: 0 };

            const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
            expect(dotProduct).toBeGreaterThan(0);
        });

        it('should route all connection directions correctly', () => {
            const scenarios = [
                {
                    name: 'right to left',
                    from: { x: 0, y: 100 },
                    to: { x: 200, y: 100 },
                    fromPoint: { x: 100, y: 125 },
                    toPoint: { x: 200, y: 125 },
                    fromSide: 'right' as Side,
                    toSide: 'left' as Side
                },
                {
                    name: 'left to right',
                    from: { x: 200, y: 100 },
                    to: { x: 0, y: 100 },
                    fromPoint: { x: 200, y: 125 },
                    toPoint: { x: 100, y: 125 },
                    fromSide: 'left' as Side,
                    toSide: 'right' as Side
                },
                {
                    name: 'bottom to top',
                    from: { x: 100, y: 200 },
                    to: { x: 100, y: 0 },
                    fromPoint: { x: 150, y: 200 },
                    toPoint: { x: 150, y: 50 },
                    fromSide: 'bottom' as Side,
                    toSide: 'top' as Side
                },
                {
                    name: 'top to bottom',
                    from: { x: 100, y: 0 },
                    to: { x: 100, y: 200 },
                    fromPoint: { x: 150, y: 50 },
                    toPoint: { x: 150, y: 200 },
                    fromSide: 'top' as Side,
                    toSide: 'bottom' as Side
                }
            ];

            scenarios.forEach(scenario => {
                const fromNode = createNodeState('from', scenario.from, { width: 100, height: 50 });
                const toNode = createNodeState('to', scenario.to, { width: 100, height: 50 });

                const path = router.route(
                    fromNode,
                    toNode,
                    scenario.fromPoint,
                    scenario.toPoint,
                    scenario.fromSide,
                    scenario.toSide,
                    'orthogonal'
                );

                expect(path.length).toBeGreaterThanOrEqual(2);

                // Verify last segment points towards target
                const lastPoint = path[path.length - 1];
                const secondLast = path[path.length - 2];
                const segmentDir = {
                    x: lastPoint.x - secondLast.x,
                    y: lastPoint.y - secondLast.y
                };
                const segmentLength = Math.sqrt(segmentDir.x ** 2 + segmentDir.y ** 2);
                if (segmentLength === 0) {
                    return; // Skip zero-length segments
                }
                const normalizedSegment = {
                    x: segmentDir.x / segmentLength,
                    y: segmentDir.y / segmentLength
                };

                const targetCenter = {
                    x: toNode.computedBounds.centerX,
                    y: toNode.computedBounds.centerY
                };
                const toTarget = {
                    x: targetCenter.x - lastPoint.x,
                    y: targetCenter.y - lastPoint.y
                };
                const targetLength = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2);
                if (targetLength === 0) {
                    return; // Skip zero-length target direction
                }
                const normalizedTarget = {
                    x: toTarget.x / targetLength,
                    y: toTarget.y / targetLength
                };

                const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
                
                if (dotProduct <= 0) {
                    throw new Error(
                        `Scenario "${scenario.name}": Last segment doesn't point towards target. ` +
                        `Dot product: ${dotProduct.toFixed(3)}, segment direction: (${normalizedSegment.x.toFixed(3)}, ${normalizedSegment.y.toFixed(3)}), ` +
                        `target direction: (${normalizedTarget.x.toFixed(3)}, ${normalizedTarget.y.toFixed(3)})`
                    );
                }
                expect(dotProduct).toBeGreaterThan(0);
            });
        });
    });
});






