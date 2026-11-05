/**
 * ConnectionPathRouter - Routing paths dla połączeń
 * 
 * Oblicza ścieżki połączeń między węzłami:
 * - Orthogonal routing (proste linie prostopadłe)
 * - Bezier routing (płynne krzywe)
 * - Straight routing (prosta linia)
 * 
 * Używany przez VisualSemanticModelManager do obliczania connection paths.
 */

import type { Point, Side, RoutingType, BezierControlPoints, VisualNodeState, ArrowMetadata } from './VisualSemanticModel';

export class ConnectionPathRouter {
    private readonly DEFAULT_OFFSET = 20;
    private readonly MAX_OFFSET = 50;
    private readonly MIN_OFFSET = 20;
    private readonly EPS = 1e-6; // Precyzyjna tolerancja dla porównań numerycznych

    /**
     * Oblicz routing path dla połączenia
     */
    route(
        from: VisualNodeState,
        to: VisualNodeState,
        fromPoint: Point,
        toPoint: Point,
        fromSide: Side,
        toSide: Side,
        type: RoutingType = 'orthogonal'
    ): Point[] {
        switch (type) {
            case 'orthogonal':
                return this.routeOrthogonal(fromPoint, toPoint, fromSide, toSide, to);
            case 'bezier':
                return this.generateBezierPath(
                    this.calculateBezierControlPoints(fromPoint, toPoint, fromSide, toSide),
                    20 // segments
                );
            case 'straight':
                return [fromPoint, toPoint];
            default:
                return this.routeOrthogonal(fromPoint, toPoint, fromSide, toSide, to);
        }
    }

    /**
     * Orthogonal routing - INSPIRACJA Z NIFI
     * 
     * Strategia (jak w NiFi):
     * 1. Start: dokładnie na center point (na krawędzi węzła)
     * 2. Offset: mały offset (20px) od krawędzi dla czytelności
     * 3. Routing: minimalna liczba punktów, tylko poziome/pionowe segmenty
     * 4. End: dokładnie na center point (na krawędzi węzła)
     * 
     * Zasady:
     * - L-shaped: gdy strony są prostopadłe (np. right->top)
     * - U-shaped: gdy strony są równoległe (np. right->left)
     * - ZERO diagonalnych segmentów!
     * - Path ZAWSZE zaczyna się i kończy na center points!
     */
    private routeOrthogonal(
        start: Point,
        end: Point,
        exitSide: Side,
        entrySide: Side,
        targetNode: VisualNodeState
    ): Point[] {
        const OFFSET = 20; // Stały offset dla czytelności (jak w NiFi)
        const points: Point[] = [];

        // CRITICAL: Path MUST start exactly at center point (on edge)
        points.push({ x: start.x, y: start.y });

        // Calculate offset points (away from nodes for routing)
        const startOffset = this.offsetAnchorPoint(start, exitSide, OFFSET);
        const endOffset = this.offsetAnchorPoint(end, entrySide, OFFSET);

        // Determine routing strategy based on sides
        const exitVertical = exitSide === 'top' || exitSide === 'bottom';
        const entryVertical = entrySide === 'top' || entrySide === 'bottom';

        // ORTHOGONAL ROUTING: Tylko poziome i pionowe segmenty!
        if (exitVertical && entryVertical) {
            // Both vertical sides (top/bottom): U-shaped connection
            // Path: Start (center) -> Offset -> Horizontal -> Offset -> End (center)
            points.push(startOffset);
            // Horizontal segment - choose middle Y for clean routing
            const midY = (startOffset.y + endOffset.y) / 2;
            points.push({ x: startOffset.x, y: midY });
            points.push({ x: endOffset.x, y: midY });
            points.push(endOffset);
        } else if (!exitVertical && !entryVertical) {
            // Both horizontal sides (left/right): U-shaped connection
            // Path: Start (center) -> Offset -> Vertical -> Offset -> End (center)
            points.push(startOffset);
            // Vertical segment - choose middle X for clean routing
            const midX = (startOffset.x + endOffset.x) / 2;
            points.push({ x: midX, y: startOffset.y });
            points.push({ x: midX, y: endOffset.y });
            points.push(endOffset);
        } else {
            // Mixed sides: L-shaped connection (most common)
            // Path: Start (center) -> Offset -> Corner -> Offset -> End (center)
            points.push(startOffset);

            if (exitVertical) {
                // Exit vertical (top/bottom), entry horizontal (left/right)
                // Go vertical first to align with entry Y, then horizontal
                points.push({ x: startOffset.x, y: endOffset.y });
            } else {
                // Exit horizontal (left/right), entry vertical (top/bottom)
                // Go horizontal first to align with entry X, then vertical
                points.push({ x: endOffset.x, y: startOffset.y });
            }

            points.push(endOffset);
        }

        // CRITICAL: Path MUST end exactly at center point (on edge)
        points.push({ x: end.x, y: end.y });

        // Remove duplicate consecutive points
        const cleanedPoints = this.removeDuplicatePoints(points);

        // CRITICAL: Verify all segments are orthogonal (no diagonals!)
        // Przekaż exitSide i entrySide dla deterministycznego wyboru łamania
        const verifiedPoints = this.ensureOrthogonalSegments(cleanedPoints, exitSide, entrySide);

        return verifiedPoints;
    }

    /**
     * Ensure all segments are orthogonal (horizontal or vertical only)
     * Removes any diagonal segments by adding intermediate points
     * PUBLIC: Used by VisualSemanticModelManager to fix anchor point updates
     * 
     * Ulepszenia:
     * - Deterministyczny wybór łamania bazujący na exit/entry side
     * - Usuwanie kolinearnych punktów
     * - Zaokrąglanie do 0.01 dla stabilności numerycznej
     */
    public ensureOrthogonalSegments(
        points: Point[],
        exitSide?: Side,
        entrySide?: Side
    ): Point[] {
        if (points.length < 2) return points;

        const pts = this.removeDuplicatePoints(points);
        const out: Point[] = [pts[0]];

        // Określ preferencję łamania na podstawie exit side (jeśli dostępne)
        const prefer = (exitSide && entrySide)
            ? this.chooseBendPreference(exitSide, entrySide)
            : 'H'; // Default: horizontal first

        for (let i = 1; i < pts.length; i++) {
            const prev = out[out.length - 1];
            const cur = pts[i];

            // Sprawdź czy segment jest już ortogonalny
            if (Math.abs(prev.x - cur.x) < this.EPS || Math.abs(prev.y - cur.y) < this.EPS) {
                // Ortogonalny - zaokrąglij i dodaj (dla stabilności numerycznej)
                out.push({
                    x: Math.round(cur.x * 100) / 100,
                    y: Math.round(cur.y * 100) / 100
                });
            } else {
                // Diagonalny - rozbij na dwa ortogonalne segmenty
                const bends = this.splitDiagonalSegment(prev, cur, prefer);
                out.push(...bends);
            }
        }

        return this.simplifyColinear(out);
    }

    /**
     * Remove duplicate consecutive points
     * Używa EPS zamiast 0.1 dla precyzyjniejszej tolerancji
     */
    private removeDuplicatePoints(points: Point[]): Point[] {
        if (points.length <= 1) return points;

        const cleaned: Point[] = [points[0]];
        for (let i = 1; i < points.length; i++) {
            const prev = cleaned[cleaned.length - 1];
            const curr = points[i];
            if (Math.abs(prev.x - curr.x) > this.EPS || Math.abs(prev.y - curr.y) > this.EPS) {
                cleaned.push(curr);
            }
        }
        return cleaned;
    }

    /**
     * Usuń kolinearne punkty (punkty na prostej)
     * Jeśli punkt b jest na prostej a-c, pomiń b
     */
    private simplifyColinear(points: Point[]): Point[] {
        if (points.length <= 2) return points.slice();

        const out: Point[] = [points[0]];
        for (let i = 1; i < points.length - 1; i++) {
            const a = out[out.length - 1];
            const b = points[i];
            const c = points[i + 1];

            // Sprawdź czy b jest na prostej a-c (cross product)
            const cross = (a.x - b.x) * (b.y - c.y) - (a.y - b.y) * (b.x - c.x);
            if (Math.abs(cross) < this.EPS) {
                continue; // b jest kolinearny, pomiń
            }
            out.push(b);
        }
        out.push(points[points.length - 1]);
        return out;
    }

    /**
     * Wybierz preferencję łamania (H-first vs V-first) na podstawie exit/entry side
     */
    private chooseBendPreference(exitSide: Side, entrySide: Side): 'H' | 'V' {
        // Jeśli wychodzimy z lewej/prawej, preferuj H (poziome)
        // Jeśli wychodzimy z góry/dołu, preferuj V (pionowe)
        const exitHorizontal = exitSide === 'left' || exitSide === 'right';
        return exitHorizontal ? 'H' : 'V';
    }

    /**
     * Rozbij diagonalny segment na dwa ortogonalne
     */
    private splitDiagonalSegment(prev: Point, cur: Point, prefer: 'H' | 'V'): Point[] {
        if (prefer === 'H') {
            // Horizontal first: (prev.x, cur.y) then (cur.x, cur.y)
            return [{ x: prev.x, y: cur.y }, { x: cur.x, y: cur.y }];
        } else {
            // Vertical first: (cur.x, prev.y) then (cur.x, cur.y)
            return [{ x: cur.x, y: prev.y }, { x: cur.x, y: cur.y }];
        }
    }

    /**
     * Oblicz control points dla bezier curve
     */
    calculateBezierControlPoints(
        start: Point,
        end: Point,
        fromSide: Side,
        toSide: Side
    ): BezierControlPoints {
        const horizontalGap = Math.abs(end.x - start.x);
        const verticalGap = Math.abs(end.y - start.y);

        const horizontalOffset = Math.max(this.MIN_OFFSET, Math.min(this.MAX_OFFSET, horizontalGap / 3));
        const verticalOffset = Math.max(this.MIN_OFFSET, Math.min(this.MAX_OFFSET, verticalGap / 3));

        // Control point 1: offset od start point w kierunku exit side
        const control1 = this.offsetAnchorPoint(
            start,
            fromSide,
            fromSide === 'top' || fromSide === 'bottom' ? verticalOffset : horizontalOffset
        );

        // Control point 2: offset od end point w kierunku entry side (odwrotny)
        const control2 = this.offsetAnchorPoint(
            end,
            toSide,
            toSide === 'top' || toSide === 'bottom' ? verticalOffset : horizontalOffset
        );

        return {
            start,
            control1,
            control2,
            end
        };
    }

    /**
     * Generuj bezier path z control points
     */
    generateBezierPath(controlPoints: BezierControlPoints, segments: number = 20): Point[] {
        const points: Point[] = [];
        const { start, control1, control2, end } = controlPoints;

        for (let i = 0; i <= segments; i++) {
            const t = i / segments;
            const x = this.cubicBezier(t, start.x, control1.x, control2.x, end.x);
            const y = this.cubicBezier(t, start.y, control1.y, control2.y, end.y);
            points.push({ x, y });
        }

        return points;
    }

    /**
     * Cubic Bezier function: B(t) = (1-t)³P₀ + 3(1-t)²tP₁ + 3(1-t)t²P₂ + t³P₃
     */
    private cubicBezier(t: number, p0: number, p1: number, p2: number, p3: number): number {
        const mt = 1 - t;
        const mt2 = mt * mt;
        const mt3 = mt2 * mt;
        const t2 = t * t;
        const t3 = t2 * t;

        return mt3 * p0 + 3 * mt2 * t * p1 + 3 * mt * t2 * p2 + t3 * p3;
    }

    /**
     * Offset anchor point w kierunku określonej strony
     */
    private offsetAnchorPoint(point: Point, side: Side, offset: number): Point {
        switch (side) {
            case 'top':
                return { x: point.x, y: point.y - offset };
            case 'bottom':
                return { x: point.x, y: point.y + offset };
            case 'left':
                return { x: point.x - offset, y: point.y };
            case 'right':
                return { x: point.x + offset, y: point.y };
        }
    }


    /**
     * Calculate arrow metadata for connection path
     */
    calculateArrowMetadata(
        points: Point[],
        endPoint: Point,
        targetNode: VisualNodeState
    ): ArrowMetadata {
        if (points.length < 2) {
            // Fallback: point towards node center
            const targetCenter = {
                x: targetNode.computedBounds.centerX,
                y: targetNode.computedBounds.centerY
            };
            const direction = {
                x: targetCenter.x - endPoint.x,
                y: targetCenter.y - endPoint.y
            };
            const length = Math.sqrt(direction.x * direction.x + direction.y * direction.y);
            const normalized = length > 0 ? { x: direction.x / length, y: direction.y / length } : { x: 1, y: 0 };
            const angle = Math.atan2(normalized.y, normalized.x);

            // Calculate spatial info for edge case (path with only 2 points)
            const arrowSize = 10;
            const arrowTipPosition = endPoint;
            const arrowBaseCenter = {
                x: endPoint.x - Math.cos(angle) * arrowSize,
                y: endPoint.y - Math.sin(angle) * arrowSize
            };

            const distanceToTargetCenter = Math.sqrt(
                (targetCenter.x - arrowTipPosition.x) ** 2 +
                (targetCenter.y - arrowTipPosition.y) ** 2
            );

            const angleDegrees = angle * 180 / Math.PI;
            const angleNormalized = ((angle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
            const isHorizontal = Math.abs(angleNormalized) < 0.1 || Math.abs(angleNormalized - Math.PI) < 0.1;
            const isVertical = Math.abs(angleNormalized - Math.PI / 2) < 0.1 || Math.abs(angleNormalized - 3 * Math.PI / 2) < 0.1;

            let quadrant: 1 | 2 | 3 | 4;
            if (angleNormalized >= 0 && angleNormalized < Math.PI / 2) {
                quadrant = 1;
            } else if (angleNormalized >= Math.PI / 2 && angleNormalized < Math.PI) {
                quadrant = 2;
            } else if (angleNormalized >= Math.PI && angleNormalized < 3 * Math.PI / 2) {
                quadrant = 3;
            } else {
                quadrant = 4;
            }

            return {
                endPoint,
                direction: normalized,
                angle,
                pointsTowardsNode: true,
                lastSegmentDirection: normalized,
                spatialInfo: {
                    arrowTipPosition,
                    arrowBaseCenter,
                    arrowSize,
                    distanceToTargetCenter,
                    angleDegrees,
                    isHorizontal,
                    isVertical,
                    quadrant
                }
            };
        }

        const lastPoint = points[points.length - 1];
        const secondLastPoint = points[points.length - 2];

        // Calculate last segment direction
        const lastSegmentDirection = {
            x: lastPoint.x - secondLastPoint.x,
            y: lastPoint.y - secondLastPoint.y
        };
        const segmentLength = Math.sqrt(
            lastSegmentDirection.x * lastSegmentDirection.x +
            lastSegmentDirection.y * lastSegmentDirection.y
        );
        const normalizedSegment = segmentLength > 0
            ? { x: lastSegmentDirection.x / segmentLength, y: lastSegmentDirection.y / segmentLength }
            : { x: 1, y: 0 };

        // Calculate direction from arrow tip to target node center
        const targetCenter = {
            x: targetNode.computedBounds.centerX,
            y: targetNode.computedBounds.centerY
        };
        const toTargetDirection = {
            x: targetCenter.x - endPoint.x,
            y: targetCenter.y - endPoint.y
        };
        const targetLength = Math.sqrt(
            toTargetDirection.x * toTargetDirection.x +
            toTargetDirection.y * toTargetDirection.y
        );
        const normalizedTarget = targetLength > 0
            ? { x: toTargetDirection.x / targetLength, y: toTargetDirection.y / targetLength }
            : { x: 1, y: 0 };

        // Check if arrow points towards node (dot product > 0)
        const dotProduct = normalizedSegment.x * normalizedTarget.x + normalizedSegment.y * normalizedTarget.y;
        let pointsTowardsNode = dotProduct > 0;

        // Calculate angle from last segment
        let angle = Math.atan2(normalizedSegment.y, normalizedSegment.x);
        let direction = normalizedSegment;

        // If arrow doesn't point towards node, reverse the angle and direction
        if (!pointsTowardsNode) {
            console.log(`[ConnectionPathRouter] Arrow doesn't point towards node (dotProduct=${dotProduct.toFixed(3)}), reversing angle from ${angle.toFixed(3)} to ${(angle + Math.PI).toFixed(3)}`);
            angle = angle + Math.PI; // Reverse by 180 degrees
            direction = { x: -normalizedSegment.x, y: -normalizedSegment.y };
            pointsTowardsNode = true; // After reversal, it now points towards node
        } else {
            console.log(`[ConnectionPathRouter] Arrow points towards node (dotProduct=${dotProduct.toFixed(3)}), angle=${angle.toFixed(3)}`);
        }

        // Calculate extended spatial information
        const arrowSize = 10;
        const arrowTipPosition = endPoint;
        const arrowBaseCenter = {
            x: endPoint.x - Math.cos(angle) * arrowSize,
            y: endPoint.y - Math.sin(angle) * arrowSize
        };

        // Calculate distance to target center
        const distanceToTargetCenter = Math.sqrt(
            (targetCenter.x - arrowTipPosition.x) ** 2 +
            (targetCenter.y - arrowTipPosition.y) ** 2
        );

        // Calculate angle in degrees
        const angleDegrees = angle * 180 / Math.PI;

        // Determine if horizontal/vertical
        const angleNormalized = ((angle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        const isHorizontal = Math.abs(angleNormalized) < 0.1 || Math.abs(angleNormalized - Math.PI) < 0.1;
        const isVertical = Math.abs(angleNormalized - Math.PI / 2) < 0.1 || Math.abs(angleNormalized - 3 * Math.PI / 2) < 0.1;

        // Determine quadrant
        let quadrant: 1 | 2 | 3 | 4;
        if (angleNormalized >= 0 && angleNormalized < Math.PI / 2) {
            quadrant = 1; // Right-down
        } else if (angleNormalized >= Math.PI / 2 && angleNormalized < Math.PI) {
            quadrant = 2; // Left-down
        } else if (angleNormalized >= Math.PI && angleNormalized < 3 * Math.PI / 2) {
            quadrant = 3; // Left-up
        } else {
            quadrant = 4; // Right-up
        }

        return {
            endPoint,
            direction,
            angle,
            pointsTowardsNode,
            lastSegmentDirection: normalizedSegment,
            spatialInfo: {
                arrowTipPosition,
                arrowBaseCenter,
                arrowSize,
                distanceToTargetCenter,
                angleDegrees,
                isHorizontal,
                isVertical,
                quadrant
            }
        };
    }

}


