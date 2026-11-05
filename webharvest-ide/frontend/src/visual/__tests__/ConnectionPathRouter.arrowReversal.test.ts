/**
 * ConnectionPathRouter.arrowReversal - Test that arrows are always reversed when needed
 * 
 * Test case based on runtime failure: arrow with dot product < 0 should be reversed
 * and pointsTowardsNode should be TRUE after reversal
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ConnectionPathRouter } from '../ConnectionPathRouter';
import type { Point, VisualNodeState } from '../VisualSemanticModel';

describe('ConnectionPathRouter.arrowReversal - Runtime Failure Case', () => {
    let router: ConnectionPathRouter;

    beforeEach(() => {
        router = new ConnectionPathRouter();
    });

    it('should reverse arrow when last segment points away from target (dot product < 0)', () => {
        // Scenario from runtime: segment points right (1, 0), but target is to the left
        // Last segment: from (100, 100) to (200, 100) - pointing RIGHT
        // Target node center: (50, 100) - to the LEFT
        // Dot product should be negative, arrow should be reversed
        
        const points: Point[] = [
            { x: 0, y: 100 },
            { x: 50, y: 100 },
            { x: 100, y: 100 },
            { x: 200, y: 100 } // Last point - segment from (100,100) to (200,100) points RIGHT
        ];
        const endPoint: Point = { x: 200, y: 100 };
        const targetNode: VisualNodeState = {
            nodeId: 'target',
            pluginType: 'var',
            position: { x: 0, y: 100 }, // Target is to the LEFT of endPoint
            dimensions: { width: 100, height: 50 },
            computedBounds: {
                left: 0,
                right: 100,
                top: 100,
                bottom: 150,
                width: 100,
                height: 50,
                centerX: 50, // Target center is at x=50 (LEFT of endPoint at x=200)
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

        // CRITICAL: After reversal, pointsTowardsNode MUST be true
        expect(metadata.pointsTowardsNode).toBe(true);
        
        // Angle should be reversed: original was 0 (pointing right), reversed should be PI (pointing left)
        expect(metadata.angle).toBeCloseTo(Math.PI, 5);
        
        // Direction should be reversed: original was (1, 0), reversed should be (-1, 0)
        expect(metadata.direction.x).toBeCloseTo(-1, 5);
        expect(metadata.direction.y).toBeCloseTo(0, 5);
    });

    it('should handle case where segment and target direction are opposite', () => {
        // Segment points right, target is left - dot product should be negative
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

        // Verify dot product would be negative
        const lastPoint = points[points.length - 1];
        const secondLast = points[points.length - 2];
        const segmentDir = {
            x: lastPoint.x - secondLast.x,
            y: lastPoint.y - secondLast.y
        };
        const segmentLength = Math.sqrt(segmentDir.x ** 2 + segmentDir.y ** 2);
        const normalizedSegment = {
            x: segmentDir.x / segmentLength,
            y: segmentDir.y / segmentLength
        };
        
        const targetCenter = {
            x: targetNode.computedBounds.centerX,
            y: targetNode.computedBounds.centerY
        };
        const toTarget = {
            x: targetCenter.x - endPoint.x,
            y: targetCenter.y - endPoint.y
        };
        const targetLength = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2);
        const normalizedTarget = {
            x: toTarget.x / targetLength,
            y: toTarget.y / targetLength
        };
        
        const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
        
        // Dot product should be negative (segment points away)
        expect(dotProduct).toBeLessThan(0);
        
        // After reversal, pointsTowardsNode should be TRUE
        expect(metadata.pointsTowardsNode).toBe(true);
        expect(metadata.angle).toBeCloseTo(Math.PI, 5); // Reversed from 0 to PI
    });
});






