/**
 * Spatial Collision Detector Tests
 */

import { describe, it, expect } from '@jest/globals';
import { SpatialCollisionDetector } from '../SpatialCollisionDetector';
import type { Point, Bounds } from '../VisualSemanticModel';

describe('SpatialCollisionDetector', () => {
    describe('boundsIntersect', () => {
        it('should detect overlapping bounds', () => {
            const bounds1: Bounds = { left: 0, right: 100, top: 0, bottom: 100, width: 100, height: 100, centerX: 50, centerY: 50 };
            const bounds2: Bounds = { left: 50, right: 150, top: 50, bottom: 150, width: 100, height: 100, centerX: 100, centerY: 100 };
            
            expect(SpatialCollisionDetector.boundsIntersect(bounds1, bounds2)).toBe(true);
        });

        it('should detect non-overlapping bounds', () => {
            const bounds1: Bounds = { left: 0, right: 100, top: 0, bottom: 100, width: 100, height: 100, centerX: 50, centerY: 50 };
            const bounds2: Bounds = { left: 200, right: 300, top: 200, bottom: 300, width: 100, height: 100, centerX: 250, centerY: 250 };
            
            expect(SpatialCollisionDetector.boundsIntersect(bounds1, bounds2)).toBe(false);
        });
    });

    describe('calculateOverlapArea', () => {
        it('should calculate overlap area correctly', () => {
            const bounds1: Bounds = { left: 0, right: 100, top: 0, bottom: 100, width: 100, height: 100, centerX: 50, centerY: 50 };
            const bounds2: Bounds = { left: 50, right: 150, top: 50, bottom: 150, width: 100, height: 100, centerX: 100, centerY: 100 };
            
            const area = SpatialCollisionDetector.calculateOverlapArea(bounds1, bounds2);
            expect(area).toBe(2500); // 50x50 overlap
        });
    });

    describe('findNodeCollisions', () => {
        it('should find overlapping nodes', () => {
            const nodeBounds: Bounds = { left: 0, right: 100, top: 0, bottom: 100, width: 100, height: 100, centerX: 50, centerY: 50 };
            const allNodes = [
                { nodeId: 'node1', computedBounds: nodeBounds },
                { nodeId: 'node2', computedBounds: { left: 50, right: 150, top: 50, bottom: 150, width: 100, height: 100, centerX: 100, centerY: 100 } },
                { nodeId: 'node3', computedBounds: { left: 200, right: 300, top: 200, bottom: 300, width: 100, height: 100, centerX: 250, centerY: 250 } }
            ];

            const collisions = SpatialCollisionDetector.findNodeCollisions(nodeBounds, 'node1', allNodes);
            expect(collisions.length).toBe(1);
            expect(collisions[0].nodeId).toBe('node2');
        });
    });

    describe('analyzePathGeometry', () => {
        it('should analyze simple path', () => {
            const path: Point[] = [
                { x: 0, y: 0 },
                { x: 100, y: 0 },
                { x: 100, y: 100 }
            ];

            const geometry = SpatialCollisionDetector.analyzePathGeometry(path);
            expect(geometry.segments.length).toBe(2);
            expect(geometry.turningPoints.length).toBe(1);
            expect(geometry.reversesDirection).toBe(false);
        });

        it('should detect direction reversal', () => {
            const path: Point[] = [
                { x: 0, y: 0 },
                { x: 100, y: 0 },
                { x: 50, y: 0 } // Backtracks
            ];

            const geometry = SpatialCollisionDetector.analyzePathGeometry(path);
            expect(geometry.reversesDirection).toBe(true);
        });
    });

    describe('findSelfIntersections', () => {
        it('should find self-intersections', () => {
            // Create a path that forms a figure-8 (self-intersecting)
            const path: Point[] = [
                { x: 0, y: 50 },    // Start
                { x: 100, y: 0 },   // Top right
                { x: 0, y: 0 },     // Top left (crosses first segment)
                { x: 100, y: 50 }   // Bottom right
            ];

            const intersections = SpatialCollisionDetector.findSelfIntersections(path);
            expect(intersections.length).toBeGreaterThan(0);
        });

        it('should return empty array for non-intersecting path', () => {
            const path: Point[] = [
                { x: 0, y: 0 },
                { x: 100, y: 0 },
                { x: 100, y: 100 },
                { x: 0, y: 100 }
            ];

            const intersections = SpatialCollisionDetector.findSelfIntersections(path);
            expect(intersections.length).toBe(0);
        });
    });
});

