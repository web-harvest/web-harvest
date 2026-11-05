/**
 * SimpleConnectionRouter - Router połączeń z wykrywaniem kolizji
 * 
 * ZASADY:
 * 1. Tylko center points (środkowe punkty na każdej krawędzi)
 * 2. Tylko orthogonal routing (poziome/pionowe linie)
 * 3. Prosty L-shaped lub U-shaped routing
 * 4. Wykrywanie kolizji z nodes
 */

class SimpleConnectionRouter {
    constructor() {
        this.OFFSET = 20; // Offset od krawędzi węzła
        this.ARROW_BEND_OFFSET = 15; // Odległość przed strzałką gdzie następuje załamanie
        this.EPS = 1e-6;
        this.MIN_SEPARATION_PX = 1;
        this.MIN_FORCED_SEPARATION = 20;
        this.nodeBounds = new Map();
        this.existingPaths = new Map(); // Store existing connection paths for intersection detection
    }

    setNodeBounds(nodeId, bounds) {
        this.nodeBounds.set(nodeId, bounds);
    }

    clearNodeBounds() {
        this.nodeBounds.clear();
    }

    setExistingPaths(connectionId, points) {
        this.existingPaths.set(connectionId, points);
    }

    clearExistingPaths() {
        this.existingPaths.clear();
    }

    removeExistingPath(connectionId) {
        this.existingPaths.delete(connectionId);
    }

    offsetPoint(point, side, offset) {
        switch (side) {
            case 'top': return { x: point.x, y: point.y - offset };
            case 'bottom': return { x: point.x, y: point.y + offset };
            case 'left': return { x: point.x - offset, y: point.y };
            case 'right': return { x: point.x + offset, y: point.y };
        }
    }

    buildHV(start, end, fromOffset, toOffset, toSide) {
        // Calculate bend point before arrow (not at node edge)
        // End point should be toOffset (outside node), not end (on node edge)
        const bendBeforeArrow = this.calculateBendBeforeArrow(toOffset, toSide);
        return [start, fromOffset, { x: toOffset.x, y: fromOffset.y }, bendBeforeArrow, toOffset];
    }

    buildVH(start, end, fromOffset, toOffset, toSide) {
        // Calculate bend point before arrow (not at node edge)
        // End point should be toOffset (outside node), not end (on node edge)
        const bendBeforeArrow = this.calculateBendBeforeArrow(toOffset, toSide);
        return [start, fromOffset, { x: fromOffset.x, y: toOffset.y }, bendBeforeArrow, toOffset];
    }

    calculateBendBeforeArrow(toOffset, toSide) {
        // Create bend point ARROW_BEND_OFFSET pixels before the arrow
        // toOffset is already outside the node, so we move back towards the node
        switch (toSide) {
            case 'top':
                return { x: toOffset.x, y: toOffset.y + this.ARROW_BEND_OFFSET };
            case 'bottom':
                return { x: toOffset.x, y: toOffset.y - this.ARROW_BEND_OFFSET };
            case 'left':
                return { x: toOffset.x + this.ARROW_BEND_OFFSET, y: toOffset.y };
            case 'right':
                return { x: toOffset.x - this.ARROW_BEND_OFFSET, y: toOffset.y };
        }
    }

    buildU(start, end, fromOffset, toOffset, fromSide, toSide) {
        const midX = (fromOffset.x + toOffset.x) / 2;
        const midY = (fromOffset.y + toOffset.y) / 2;
        // End point should be toOffset (outside node), not end (on node edge)
        const bendBeforeArrow = this.calculateBendBeforeArrow(toOffset, toSide);
        
        if (fromSide === 'left' || fromSide === 'right') {
            return [start, fromOffset, { x: midX, y: fromOffset.y }, { x: midX, y: toOffset.y }, bendBeforeArrow, toOffset];
        } else {
            return [start, fromOffset, { x: fromOffset.x, y: midY }, { x: toOffset.x, y: midY }, bendBeforeArrow, toOffset];
        }
    }

    pathManhattanLength(points) {
        let len = 0;
        for (let i = 1; i < points.length; i++) {
            len += Math.abs(points[i].x - points[i - 1].x) + Math.abs(points[i].y - points[i - 1].y);
        }
        return len;
    }

    segmentIntersectsNode(p1, p2, bounds) {
        const minX = Math.min(p1.x, p2.x);
        const maxX = Math.max(p1.x, p2.x);
        const minY = Math.min(p1.y, p2.y);
        const maxY = Math.max(p1.y, p2.y);

        const margin = 2;
        const nodeLeft = bounds.x - margin;
        const nodeRight = bounds.x + bounds.width + margin;
        const nodeTop = bounds.y - margin;
        const nodeBottom = bounds.y + bounds.height + margin;

        if (maxX < nodeLeft || minX > nodeRight || maxY < nodeTop || minY > nodeBottom) {
            return false;
        }

        const segmentInNodeX = minX < nodeRight && maxX > nodeLeft;
        const segmentInNodeY = minY < nodeBottom && maxY > nodeTop;
        
        if (segmentInNodeX && segmentInNodeY) {
            const isOnEdge = (minX >= nodeLeft && maxX <= nodeRight && (Math.abs(minY - nodeTop) < margin || Math.abs(maxY - nodeBottom) < margin)) ||
                           (minY >= nodeTop && maxY <= nodeBottom && (Math.abs(minX - nodeLeft) < margin || Math.abs(maxX - nodeRight) < margin));
            if (!isOnEdge) {
                return true;
            }
        }

        return false;
    }

    pathIntersectsNodes(points, excludeNodeIds) {
        if (points.length < 2) return false;

        for (let i = 0; i < points.length - 1; i++) {
            const p1 = points[i];
            const p2 = points[i + 1];

            for (const [nodeId, bounds] of this.nodeBounds.entries()) {
                if (excludeNodeIds && excludeNodeIds.has(nodeId)) continue;
                if (this.segmentIntersectsNode(p1, p2, bounds)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if two line segments intersect (for orthogonal segments only)
     */
    segmentsIntersect(p1, p2, p3, p4) {
        // For orthogonal segments, we can use simpler logic
        const isHorizontal1 = Math.abs(p1.y - p2.y) < this.EPS;
        const isHorizontal2 = Math.abs(p3.y - p4.y) < this.EPS;

        if (isHorizontal1 && isHorizontal2) {
            // Both horizontal - check if on same Y and X ranges overlap
            if (Math.abs(p1.y - p3.y) < this.EPS) {
                const minX1 = Math.min(p1.x, p2.x);
                const maxX1 = Math.max(p1.x, p2.x);
                const minX2 = Math.min(p3.x, p4.x);
                const maxX2 = Math.max(p3.x, p4.x);
                return !(maxX1 < minX2 || maxX2 < minX1);
            }
            return false;
        } else if (!isHorizontal1 && !isHorizontal2) {
            // Both vertical - check if on same X and Y ranges overlap
            if (Math.abs(p1.x - p3.x) < this.EPS) {
                const minY1 = Math.min(p1.y, p2.y);
                const maxY1 = Math.max(p1.y, p2.y);
                const minY2 = Math.min(p3.y, p4.y);
                const maxY2 = Math.max(p3.y, p4.y);
                return !(maxY1 < minY2 || maxY2 < minY1);
            }
            return false;
        } else {
            // One horizontal, one vertical - check intersection point
            const hSeg = isHorizontal1 ? { p1, p2 } : { p1: p3, p2: p4 };
            const vSeg = isHorizontal1 ? { p1: p3, p2: p4 } : { p1, p2 };

            const hY = hSeg.p1.y;
            const vX = vSeg.p1.x;
            const hMinX = Math.min(hSeg.p1.x, hSeg.p2.x);
            const hMaxX = Math.max(hSeg.p1.x, hSeg.p2.x);
            const vMinY = Math.min(vSeg.p1.y, vSeg.p2.y);
            const vMaxY = Math.max(vSeg.p1.y, vSeg.p2.y);

            return (vX >= hMinX && vX <= hMaxX && hY >= vMinY && hY <= vMaxY);
        }
    }

    /**
     * Checks if a path intersects with any existing connection paths
     */
    pathIntersectsConnections(points, excludeConnectionId = null) {
        if (points.length < 2) return false;

        for (let i = 0; i < points.length - 1; i++) {
            const p1 = points[i];
            const p2 = points[i + 1];

            for (const [connId, existingPoints] of this.existingPaths.entries()) {
                if (excludeConnectionId && connId === excludeConnectionId) continue;

                for (let j = 0; j < existingPoints.length - 1; j++) {
                    const p3 = existingPoints[j];
                    const p4 = existingPoints[j + 1];

                    if (this.segmentsIntersect(p1, p2, p3, p4)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    removeDuplicatePoints(points) {
        if (points.length <= 1) return points;
        const cleaned = [points[0]];
        for (let i = 1; i < points.length; i++) {
            const prev = cleaned[cleaned.length - 1];
            const curr = points[i];
            if (Math.abs(prev.x - curr.x) > this.EPS || Math.abs(prev.y - curr.y) > this.EPS) {
                cleaned.push(curr);
            }
        }
        return cleaned;
    }

    ensureOrthogonalSegments(points, fromSide, toSide) {
        if (points.length < 2) return points;
        const pts = this.removeDuplicatePoints(points);
        if (pts.length < 2) return pts;
        
        const out = [pts[0]];
        
        for (let i = 1; i < pts.length; i++) {
            const prev = out[out.length - 1];
            const cur = pts[i];
            
            // If points are on same line (horizontal or vertical), add current point
            if (Math.abs(prev.x - cur.x) < this.EPS || Math.abs(prev.y - cur.y) < this.EPS) {
                out.push({ x: Math.round(cur.x * 100) / 100, y: Math.round(cur.y * 100) / 100 });
            } else {
                // Split diagonal: first move horizontally, then vertically
                // Ensure we don't lose the final point
                if (i === pts.length - 1) {
                    // Last point - ensure we reach it
                    out.push({ x: prev.x, y: cur.y });
                    out.push({ x: cur.x, y: cur.y });
                } else {
                    out.push({ x: prev.x, y: cur.y });
                    out.push({ x: cur.x, y: cur.y });
                }
            }
        }
        
        // Ensure last point is preserved
        if (out.length > 0 && pts.length > 0) {
            const originalLast = pts[pts.length - 1];
            const currentLast = out[out.length - 1];
            if (Math.abs(originalLast.x - currentLast.x) > this.EPS || 
                Math.abs(originalLast.y - currentLast.y) > this.EPS) {
                out.push({ x: originalLast.x, y: originalLast.y });
            }
        }
        
        return out;
    }

    route(from, to, fromSide, toSide, excludeNodeIds) {
        const start = { x: from.x, y: from.y };
        const end = { x: to.x, y: to.y };

        const distX = Math.abs(start.x - end.x);
        const distY = Math.abs(start.y - end.y);

        const fromOffset = this.offsetPoint(start, fromSide, this.OFFSET);
        const toOffset = this.offsetPoint(end, toSide, this.OFFSET);
        
        if (distX < this.MIN_SEPARATION_PX && distY < this.MIN_SEPARATION_PX) {
            // Use offset points for very close nodes
            return [
                start,
                fromOffset,
                { x: toOffset.x, y: fromOffset.y },
                toOffset
            ];
        }

        const hv = this.buildHV(start, end, fromOffset, toOffset, toSide);
        const vh = this.buildVH(start, end, fromOffset, toOffset, toSide);
        const uPath = this.buildU(start, end, fromOffset, toOffset, fromSide, toSide);

        const candidates = [
            { 
                path: hv, 
                length: this.pathManhattanLength(hv), 
                hasNodeCollision: this.pathIntersectsNodes(hv, excludeNodeIds),
                hasConnectionCollision: this.pathIntersectsConnections(hv)
            },
            { 
                path: vh, 
                length: this.pathManhattanLength(vh), 
                hasNodeCollision: this.pathIntersectsNodes(vh, excludeNodeIds),
                hasConnectionCollision: this.pathIntersectsConnections(vh)
            },
            { 
                path: uPath, 
                length: this.pathManhattanLength(uPath), 
                hasNodeCollision: this.pathIntersectsNodes(uPath, excludeNodeIds),
                hasConnectionCollision: this.pathIntersectsConnections(uPath)
            }
        ];

        // Mark candidates with any collision
        candidates.forEach(c => {
            c.hasCollision = c.hasNodeCollision || c.hasConnectionCollision;
        });

        // Prefer candidates without any collisions
        const safeCandidates = candidates.filter(c => !c.hasCollision);
        const bestCandidate = safeCandidates.length > 0
            ? safeCandidates.reduce((best, curr) => curr.length < best.length ? curr : best)
            : candidates.reduce((best, curr) => curr.length < best.length ? curr : best);

        let points = bestCandidate.path;
        points = this.ensureOrthogonalSegments(points, fromSide, toSide);
        points = this.removeDuplicatePoints(points);

        if (points.length < 4) {
            // Use offset points to ensure path ends outside node
            points = [start, fromOffset, { x: toOffset.x, y: fromOffset.y }, toOffset];
        }

        // Final validation - ensure end point is toOffset (outside node)
        const finalStart = points[0];
        const finalEnd = points[points.length - 1];
        
        // If final end is too close to start, use offset points
        if (Math.abs(finalStart.x - finalEnd.x) < this.EPS && Math.abs(finalStart.y - finalEnd.y) < this.EPS) {
            points = [
                start,
                fromOffset,
                { x: toOffset.x, y: fromOffset.y },
                toOffset
            ];
        } else {
            // Ensure last point is toOffset (outside node), not end (on node edge)
            if (Math.abs(finalEnd.x - end.x) < 1 && Math.abs(finalEnd.y - end.y) < 1) {
                points[points.length - 1] = toOffset;
            }
        }

        return points;
    }
}

