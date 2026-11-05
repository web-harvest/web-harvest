/**
 * Unit tests for collision detection and auto-layout
 */

import { SpatialCollisionDetector } from '../SpatialCollisionDetector';
import type { Point, Bounds } from '../VisualSemanticModel';

describe('SpatialCollisionDetector', () => {
    describe('boundsIntersect', () => {
        it('should detect overlapping bounds', () => {
            const bounds1: Bounds = {
                left: 0,
                right: 100,
                top: 0,
                bottom: 100,
                width: 100,
                height: 100,
                centerX: 50,
                centerY: 50
            };
            const bounds2: Bounds = {
                left: 50,
                right: 150,
                top: 50,
                bottom: 150,
                width: 100,
                height: 100,
                centerX: 100,
                centerY: 100
            };
            
            expect(SpatialCollisionDetector.boundsIntersect(bounds1, bounds2)).toBe(true);
        });

        it('should detect non-overlapping bounds', () => {
            const bounds1: Bounds = {
                left: 0,
                right: 100,
                top: 0,
                bottom: 100,
                width: 100,
                height: 100,
                centerX: 50,
                centerY: 50
            };
            const bounds2: Bounds = {
                left: 200,
                right: 300,
                top: 200,
                bottom: 300,
                width: 100,
                height: 100,
                centerX: 250,
                centerY: 250
            };
            
            expect(SpatialCollisionDetector.boundsIntersect(bounds1, bounds2)).toBe(false);
        });
    });

    describe('calculateOverlapArea', () => {
        it('should calculate overlap area correctly', () => {
            const bounds1: Bounds = {
                left: 0,
                right: 100,
                top: 0,
                bottom: 100,
                width: 100,
                height: 100,
                centerX: 50,
                centerY: 50
            };
            const bounds2: Bounds = {
                left: 50,
                right: 150,
                top: 50,
                bottom: 150,
                width: 100,
                height: 100,
                centerX: 100,
                centerY: 100
            };
            
            const overlap = SpatialCollisionDetector.calculateOverlapArea(bounds1, bounds2);
            expect(overlap).toBe(2500); // 50x50 overlap
        });
    });

    describe('analyzePathGeometry', () => {
        it('should detect turning points in orthogonal path', () => {
            const path: Point[] = [
                { x: 0, y: 0 },
                { x: 0, y: 50 },
                { x: 100, y: 50 },
                { x: 100, y: 100 }
            ];
            
            const geometry = SpatialCollisionDetector.analyzePathGeometry(path);
            expect(geometry.turningPoints.length).toBeGreaterThan(0);
            expect(geometry.segments.length).toBe(3);
        });

        it('should calculate segment directions correctly', () => {
            const path: Point[] = [
                { x: 0, y: 0 },
                { x: 100, y: 0 }
            ];
            
            const geometry = SpatialCollisionDetector.analyzePathGeometry(path);
            expect(geometry.segments.length).toBe(1);
            expect(geometry.segments[0].isHorizontal).toBe(true);
            expect(geometry.segments[0].isVertical).toBe(false);
        });
    });

    describe('findPathCrossings', () => {
        it('should detect crossing paths', () => {
            const path1: Point[] = [
                { x: 0, y: 50 },
                { x: 100, y: 50 }
            ];
            const path2: Point[] = [
                { x: 50, y: 0 },
                { x: 50, y: 100 }
            ];
            
            const crossings = SpatialCollisionDetector.findPathCrossings(path1, [
                { connectionId: 'conn-2', points: path2 }
            ]);
            
            expect(crossings.length).toBe(1);
            expect(crossings[0].intersectionPoint.x).toBeCloseTo(50);
            expect(crossings[0].intersectionPoint.y).toBeCloseTo(50);
        });
    });
});

