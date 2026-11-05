/**
 * ConnectionPathRouter Integration Test
 * 
 * Test full flow: route() -> calculateArrowMetadata() with real node positions
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ConnectionPathRouter } from '../ConnectionPathRouter';
import type { Point, VisualNodeState, Side } from '../VisualSemanticModel';

describe('ConnectionPathRouter Integration', () => {
    let router: ConnectionPathRouter;

    beforeEach(() => {
        router = new ConnectionPathRouter();
    });

    it('should route and calculate arrow metadata correctly when target is to the left', () => {
        // Scenario: from node at (300, 100), to node at (0, 100)
        // Segment will point right, but target is left - should reverse
        
        const fromNode: VisualNodeState = {
            nodeId: 'from',
            pluginType: 'var',
            position: { x: 300, y: 100 },
            dimensions: { width: 100, height: 50 },
            computedBounds: {
                left: 300,
                right: 400,
                top: 100,
                bottom: 150,
                width: 100,
                height: 50,
                centerX: 350,
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

        // Connection points: from right side of fromNode, to left side of toNode
        const fromPoint: Point = { x: 400, y: 125 }; // Right edge of fromNode
        const toPoint: Point = { x: 0, y: 125 }; // Left edge of toNode

        // Route the path
        const path = router.route(
            fromNode,
            toNode,
            fromPoint,
            toPoint,
            'right',
            'left',
            'orthogonal'
        );

        expect(path.length).toBeGreaterThan(0);
        
        // Get end point (should be toPoint)
        const endPoint = path[path.length - 1];
        
        // Calculate arrow metadata
        const arrowMetadata = router.calculateArrowMetadata(
            path,
            endPoint,
            toNode
        );

        // CRITICAL: After reversal, pointsTowardsNode MUST be true
        expect(arrowMetadata.pointsTowardsNode).toBe(true);
        
        // Verify the last segment direction
        if (path.length >= 2) {
            const lastPoint = path[path.length - 1];
            const secondLast = path[path.length - 2];
            const segmentDir = {
                x: lastPoint.x - secondLast.x,
                y: lastPoint.y - secondLast.y
            };
            
            // If segment points right but target is left, arrow should be reversed
            if (segmentDir.x > 0 && toNode.computedBounds.centerX < endPoint.x) {
                // Arrow should point left (angle should be around PI)
                expect(arrowMetadata.angle).toBeCloseTo(Math.PI, 1);
                expect(arrowMetadata.direction.x).toBeLessThan(0);
            }
        }
    });
});






