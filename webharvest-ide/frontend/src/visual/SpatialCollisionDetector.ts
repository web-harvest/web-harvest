/**
 * Spatial Collision Detector
 * 
 * Wykrywa kolizje między węzłami i przecięcia ścieżek
 */

import type { Point, Bounds } from './VisualSemanticModel';

export interface NodeCollision {
    nodeId: string;
    overlapArea: number;
    overlapBounds: Bounds;
}

export interface PathSegment {
    from: Point;
    to: Point;
    direction: Point;
    length: number;
    angle: number;
    isHorizontal: boolean;
    isVertical: boolean;
}

export interface PathIntersection {
    point: Point;
    segment1Index: number;
    segment2Index: number;
}

export interface PathCrossing {
    connectionId: string;
    intersectionPoint: Point;
    thisSegmentIndex: number;
    otherSegmentIndex: number;
}

export class SpatialCollisionDetector {
    /**
     * Sprawdź czy dwa prostokąty się przecinają
     */
    static boundsIntersect(bounds1: Bounds, bounds2: Bounds): boolean {
        return !(
            bounds1.right < bounds2.left ||
            bounds1.left > bounds2.right ||
            bounds1.bottom < bounds2.top ||
            bounds1.top > bounds2.bottom
        );
    }

    /**
     * Oblicz obszar przecięcia dwóch prostokątów
     */
    static calculateOverlapArea(bounds1: Bounds, bounds2: Bounds): number {
        if (!this.boundsIntersect(bounds1, bounds2)) {
            return 0;
        }

        const overlapLeft = Math.max(bounds1.left, bounds2.left);
        const overlapRight = Math.min(bounds1.right, bounds2.right);
        const overlapTop = Math.max(bounds1.top, bounds2.top);
        const overlapBottom = Math.min(bounds1.bottom, bounds2.bottom);

        const overlapWidth = Math.max(0, overlapRight - overlapLeft);
        const overlapHeight = Math.max(0, overlapBottom - overlapTop);

        return overlapWidth * overlapHeight;
    }

    /**
     * Oblicz prostokąt przecięcia
     */
    static calculateOverlapBounds(bounds1: Bounds, bounds2: Bounds): Bounds | null {
        if (!this.boundsIntersect(bounds1, bounds2)) {
            return null;
        }

        const overlapLeft = Math.max(bounds1.left, bounds2.left);
        const overlapRight = Math.min(bounds1.right, bounds2.right);
        const overlapTop = Math.max(bounds1.top, bounds2.top);
        const overlapBottom = Math.min(bounds1.bottom, bounds2.bottom);

        const width = overlapRight - overlapLeft;
        const height = overlapBottom - overlapTop;

        return {
            left: overlapLeft,
            right: overlapRight,
            top: overlapTop,
            bottom: overlapBottom,
            width,
            height,
            centerX: overlapLeft + width / 2,
            centerY: overlapTop + height / 2
        };
    }

    /**
     * Znajdź wszystkie węzły, z którymi dany węzeł się przecina
     */
    static findNodeCollisions(
        nodeBounds: Bounds,
        nodeId: string,
        allNodes: Array<{ nodeId: string; computedBounds: Bounds }>
    ): NodeCollision[] {
        const collisions: NodeCollision[] = [];

        for (const otherNode of allNodes) {
            if (otherNode.nodeId === nodeId) {
                continue; // Skip self
            }

            const overlapArea = this.calculateOverlapArea(nodeBounds, otherNode.computedBounds);
            if (overlapArea > 0) {
                const overlapBounds = this.calculateOverlapBounds(nodeBounds, otherNode.computedBounds);
                if (overlapBounds) {
                    collisions.push({
                        nodeId: otherNode.nodeId,
                        overlapArea,
                        overlapBounds
                    });
                }
            }
        }

        return collisions;
    }

    /**
     * Oblicz odległości do najbliższych węzłów w każdym kierunku
     */
    static calculateClearance(
        nodeBounds: Bounds,
        nodeId: string,
        allNodes: Array<{ nodeId: string; computedBounds: Bounds }>
    ): { left: number; right: number; top: number; bottom: number; min: number } {
        let left = Infinity;
        let right = Infinity;
        let top = Infinity;
        let bottom = Infinity;

        for (const otherNode of allNodes) {
            if (otherNode.nodeId === nodeId) {
                continue;
            }

            const other = otherNode.computedBounds;

            // Left clearance
            if (other.right <= nodeBounds.left) {
                left = Math.min(left, nodeBounds.left - other.right);
            }

            // Right clearance
            if (other.left >= nodeBounds.right) {
                right = Math.min(right, other.left - nodeBounds.right);
            }

            // Top clearance
            if (other.bottom <= nodeBounds.top) {
                top = Math.min(top, nodeBounds.top - other.bottom);
            }

            // Bottom clearance
            if (other.top >= nodeBounds.bottom) {
                bottom = Math.min(bottom, other.top - nodeBounds.bottom);
            }
        }

        return {
            left: left === Infinity ? 0 : left,
            right: right === Infinity ? 0 : right,
            top: top === Infinity ? 0 : top,
            bottom: bottom === Infinity ? 0 : bottom,
            min: Math.min(left, right, top, bottom) === Infinity ? 0 : Math.min(left, right, top, bottom)
        };
    }

    /**
     * Analizuj geometrię ścieżki
     */
    static analyzePathGeometry(points: Point[]): {
        segments: PathSegment[];
        turningPoints: Point[];
        reversesDirection: boolean;
        maxDeviation: number;
    } {
        if (points.length < 2) {
            return {
                segments: [],
                turningPoints: [],
                reversesDirection: false,
                maxDeviation: 0
            };
        }

        const segments: PathSegment[] = [];
        const turningPoints: Point[] = [];

        // Calculate segments
        for (let i = 1; i < points.length; i++) {
            const from = points[i - 1];
            const to = points[i];

            const dx = to.x - from.x;
            const dy = to.y - from.y;
            const length = Math.sqrt(dx * dx + dy * dy);
            const angle = Math.atan2(dy, dx);

            const direction = length > 0
                ? { x: dx / length, y: dy / length }
                : { x: 1, y: 0 };

            segments.push({
                from,
                to,
                direction,
                length,
                angle,
                isHorizontal: Math.abs(dy) < 0.01,
                isVertical: Math.abs(dx) < 0.01
            });

            // Detect turning points (where direction changes significantly)
            if (i > 1) {
                const prevSegment = segments[segments.length - 2];
                const angleDiff = Math.abs(angle - prevSegment.angle);
                const normalizedAngleDiff = angleDiff > Math.PI ? 2 * Math.PI - angleDiff : angleDiff;
                
                if (normalizedAngleDiff > 0.1) { // More than ~6 degrees
                    turningPoints.push(from);
                }
            }
        }

        // Check for direction reversal (180° turn)
        let reversesDirection = false;
        for (let i = 1; i < segments.length; i++) {
            const prevAngle = segments[i - 1].angle;
            const currAngle = segments[i].angle;
            const angleDiff = Math.abs(currAngle - prevAngle);
            const normalizedDiff = angleDiff > Math.PI ? 2 * Math.PI - angleDiff : angleDiff;
            
            if (normalizedDiff > Math.PI * 0.8) { // More than 144° (almost 180°)
                reversesDirection = true;
                break;
            }
        }

        // Calculate max deviation from direct line
        let maxDeviation = 0;
        if (points.length >= 2) {
            const start = points[0];
            const end = points[points.length - 1];
            const directDistance = Math.sqrt(
                (end.x - start.x) ** 2 + (end.y - start.y) ** 2
            );

            for (const point of points) {
                // Distance from point to direct line
                const dist = this.pointToLineDistance(point, start, end);
                maxDeviation = Math.max(maxDeviation, dist);
            }
        }

        return {
            segments,
            turningPoints,
            reversesDirection,
            maxDeviation
        };
    }

    /**
     * Znajdź self-intersections w ścieżce
     */
    static findSelfIntersections(points: Point[]): PathIntersection[] {
        const intersections: PathIntersection[] = [];

        if (points.length < 4) {
            return intersections; // Need at least 2 segments
        }

        // Check each pair of non-adjacent segments
        for (let i = 0; i < points.length - 1; i++) {
            const seg1Start = points[i];
            const seg1End = points[i + 1];

            for (let j = i + 2; j < points.length - 1; j++) {
                const seg2Start = points[j];
                const seg2End = points[j + 1];

                const intersection = this.segmentIntersection(
                    seg1Start, seg1End,
                    seg2Start, seg2End
                );

                if (intersection) {
                    intersections.push({
                        point: intersection,
                        segment1Index: i,
                        segment2Index: j
                    });
                }
            }
        }

        return intersections;
    }

    /**
     * Znajdź przecięcia z innymi ścieżkami
     */
    static findPathCrossings(
        thisPath: Point[],
        otherPaths: Array<{ connectionId: string; points: Point[] }>
    ): PathCrossing[] {
        const crossings: PathCrossing[] = [];

        for (const otherPath of otherPaths) {
            for (let i = 0; i < thisPath.length - 1; i++) {
                const seg1Start = thisPath[i];
                const seg1End = thisPath[i + 1];

                for (let j = 0; j < otherPath.points.length - 1; j++) {
                    const seg2Start = otherPath.points[j];
                    const seg2End = otherPath.points[j + 1];

                    const intersection = this.segmentIntersection(
                        seg1Start, seg1End,
                        seg2Start, seg2End
                    );

                    if (intersection) {
                        crossings.push({
                            connectionId: otherPath.connectionId,
                            intersectionPoint: intersection,
                            thisSegmentIndex: i,
                            otherSegmentIndex: j
                        });
                    }
                }
            }
        }

        return crossings;
    }

    /**
     * Sprawdź czy dwa segmenty się przecinają
     */
    private static segmentIntersection(
        p1: Point, p2: Point,
        p3: Point, p4: Point
    ): Point | null {
        const denom = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y);
        
        if (Math.abs(denom) < 0.0001) {
            return null; // Parallel lines
        }

        const ua = ((p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x)) / denom;
        const ub = ((p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x)) / denom;

        if (ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1) {
            return {
                x: p1.x + ua * (p2.x - p1.x),
                y: p1.y + ua * (p2.y - p1.y)
            };
        }

        return null;
    }

    /**
     * Oblicz odległość punktu od linii
     */
    private static pointToLineDistance(point: Point, lineStart: Point, lineEnd: Point): number {
        const A = point.x - lineStart.x;
        const B = point.y - lineStart.y;
        const C = lineEnd.x - lineStart.x;
        const D = lineEnd.y - lineStart.y;

        const dot = A * C + B * D;
        const lenSq = C * C + D * D;
        const param = lenSq !== 0 ? dot / lenSq : -1;

        let xx: number, yy: number;

        if (param < 0) {
            xx = lineStart.x;
            yy = lineStart.y;
        } else if (param > 1) {
            xx = lineEnd.x;
            yy = lineEnd.y;
        } else {
            xx = lineStart.x + param * C;
            yy = lineStart.y + param * D;
        }

        const dx = point.x - xx;
        const dy = point.y - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }
}






