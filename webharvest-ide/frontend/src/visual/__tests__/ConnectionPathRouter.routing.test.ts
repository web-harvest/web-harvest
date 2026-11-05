/**
 * ConnectionPathRouter.route - Routing Tests
 * 
 * Testy sprawdzające, że routing tworzy ścieżki, które wskazują na target node
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ConnectionPathRouter } from '../ConnectionPathRouter';
import type { Point, VisualNodeState, Side } from '../VisualSemanticModel';

describe('ConnectionPathRouter.route - Routing Direction', () => {
    let router: ConnectionPathRouter;

    beforeEach(() => {
        router = new ConnectionPathRouter();
    });

    it('should route path where last segment points towards target node', () => {
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
                centerX: 250, // RIGHT of fromNode
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

        const fromPoint: Point = { x: 100, y: 125 };
        const toPoint: Point = { x: 200, y: 125 };

        const path = router.route(fromNode, toNode, fromPoint, toPoint, 'right', 'left', 'orthogonal');

        expect(path.length).toBeGreaterThanOrEqual(2);

        // Check last segment
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

        // Direction from last point to target center
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

        // Dot product should be positive (segment points towards target)
        const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
        expect(dotProduct).toBeGreaterThan(0);
    });

    it('should route path correctly even when target is to the left (reverse connection)', () => {
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

        const fromPoint: Point = { x: 200, y: 125 }; // Left edge of fromNode
        const toPoint: Point = { x: 100, y: 125 }; // Right edge of toNode

        const path = router.route(fromNode, toNode, fromPoint, toPoint, 'left', 'right', 'orthogonal');

        expect(path.length).toBeGreaterThanOrEqual(2);

        // Check last segment
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

        // Direction from last point to target center
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

        // Dot product should be positive (segment points towards target)
        const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
        expect(dotProduct).toBeGreaterThan(0);
    });
});






