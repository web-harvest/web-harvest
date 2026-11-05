/**
 * SimpleConnectionRouter - PROSTY router połączeń z ulepszonym orthogonal routing
 * 
 * ZASADY:
 * 1. Tylko center points (środkowe punkty na każdej krawędzi)
 * 2. Tylko orthogonal routing (poziome/pionowe linie)
 * 3. Prosty L-shaped lub U-shaped routing
 * 4. Deterministyczne łamanie diagonalnych segmentów
 * 5. Usuwanie kolinearnych punktów
 */

import type { Point, Side } from './VisualSemanticModel';

type NodeBounds = { x: number; y: number; width: number; height: number };

export class SimpleConnectionRouter {
    private readonly OFFSET = 20; // Offset od krawędzi węzła
    private readonly EPS = 1e-6; // Precyzja numeryczna
    private readonly MIN_SEPARATION_PX = 1; // distance considered "too close"
    private readonly MIN_FORCED_SEPARATION = 20; // forced offset if points identical/too close

    // Node bounds for collision detection (set by caller)
    private nodeBounds: Map<string, NodeBounds> = new Map();

    /**
     * Usuń duplikaty punktów
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
     * 
     * Uproszczona wersja - usuwa punkty które leżą na tej samej linii (poziomej lub pionowej)
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
     * Usuń kolinearne punkty (uproszczona wersja dla orthogonal paths)
     * 
     * Dla orthogonal paths (tylko poziome/pionowe), sprawdzamy czy trzy punkty
     * leżą na tej samej linii (poziomej lub pionowej)
     */
    private removeCollinear(points: Point[]): Point[] {
        if (points.length < 3) return points;
        const res: Point[] = [points[0]];
        for (let i = 1; i < points.length - 1; i++) {
            const a = points[i - 1];
            const b = points[i];
            const c = points[i + 1];
            // Sprawdź czy b jest kolinearny z a i c (wszystkie na tej samej osi)
            if (!((a.x === b.x && b.x === c.x) || (a.y === b.y && b.y === c.y))) {
                res.push(b);
            }
        }
        res.push(points[points.length - 1]);
        return res;
    }

    /**
     * Oblicz długość Manhattan (suma długości poziomej i pionowej)
     */
    private manhattanLength(points: Point[]): number {
        let len = 0;
        for (let i = 1; i < points.length; i++) {
            len += Math.abs(points[i].x - points[i - 1].x) + Math.abs(points[i].y - points[i - 1].y);
        }
        return len;
    }

    /**
     * Oblicz długość Manhattan dla path (alias dla manhattanLength)
     */
    private pathManhattanLength(points: Point[]): number {
        return this.manhattanLength(points);
    }

    /**
     * Zbuduj kandydata HV (poziomo, potem pionowo)
     */
    private buildHV(start: Point, end: Point, fromOffset: Point, toOffset: Point): Point[] {
        return [start, fromOffset, { x: toOffset.x, y: fromOffset.y }, toOffset, end];
    }

    /**
     * Zbuduj kandydata VH (pionowo, potem poziomo)
     */
    private buildVH(start: Point, end: Point, fromOffset: Point, toOffset: Point): Point[] {
        return [start, fromOffset, { x: fromOffset.x, y: toOffset.y }, toOffset, end];
    }

    /**
     * Zbuduj U-shaped path (dla unikania kolizji)
     */
    private buildU(start: Point, end: Point, fromOffset: Point, toOffset: Point, fromSide: Side, toSide: Side): Point[] {
        // U-shaped: start -> offset -> mid (poziomo/pionowo) -> offset -> end
        const midX = (fromOffset.x + toOffset.x) / 2;
        const midY = (fromOffset.y + toOffset.y) / 2;

        // Wybierz kierunek U na podstawie sides
        if (fromSide === 'left' || fromSide === 'right') {
            // Poziomy U
            return [start, fromOffset, { x: midX, y: fromOffset.y }, { x: midX, y: toOffset.y }, toOffset, end];
        } else {
            // Pionowy U
            return [start, fromOffset, { x: fromOffset.x, y: midY }, { x: toOffset.x, y: midY }, toOffset, end];
        }
    }

    /**
     * Sprawdź czy segment przecina node bounds
     */
    private segmentIntersectsNode(p1: Point, p2: Point, bounds: NodeBounds, excludeNodeIds?: Set<string>): boolean {
        // Prosty test: sprawdź czy segment przecina prostokąt
        const minX = Math.min(p1.x, p2.x);
        const maxX = Math.max(p1.x, p2.x);
        const minY = Math.min(p1.y, p2.y);
        const maxY = Math.max(p1.y, p2.y);

        // Sprawdź przecięcie z prostokątem (z małym marginesem)
        const margin = 5;
        const nodeLeft = bounds.x - margin;
        const nodeRight = bounds.x + bounds.width + margin;
        const nodeTop = bounds.y - margin;
        const nodeBottom = bounds.y + bounds.height + margin;

        // Jeśli segment jest całkowicie poza prostokątem
        if (maxX < nodeLeft || minX > nodeRight || maxY < nodeTop || minY > nodeBottom) {
            return false;
        }

        // Sprawdź czy segment przechodzi przez środek prostokąta (uproszczone)
        const nodeCenterX = bounds.x + bounds.width / 2;
        const nodeCenterY = bounds.y + bounds.height / 2;

        // Jeśli segment przechodzi przez środek, to przecina
        if (minX <= nodeCenterX && nodeCenterX <= maxX && minY <= nodeCenterY && nodeCenterY <= maxY) {
            return true;
        }

        // Sprawdź przecięcie z krawędziami prostokąta
        return this.lineIntersectsRect(p1, p2, bounds);
    }

    /**
     * Sprawdź czy linia przecina prostokąt
     */
    private lineIntersectsRect(p1: Point, p2: Point, rect: NodeBounds): boolean {
        // Sprawdź czy którykolwiek punkt jest wewnątrz prostokąta
        if (this.pointInRect(p1, rect) || this.pointInRect(p2, rect)) {
            return true;
        }

        // Sprawdź przecięcie z krawędziami prostokąta
        const edges = [
            { from: { x: rect.x, y: rect.y }, to: { x: rect.x + rect.width, y: rect.y } }, // top
            { from: { x: rect.x + rect.width, y: rect.y }, to: { x: rect.x + rect.width, y: rect.y + rect.height } }, // right
            { from: { x: rect.x + rect.width, y: rect.y + rect.height }, to: { x: rect.x, y: rect.y + rect.height } }, // bottom
            { from: { x: rect.x, y: rect.y + rect.height }, to: { x: rect.x, y: rect.y } } // left
        ];

        for (const edge of edges) {
            if (this.linesIntersect(p1, p2, edge.from, edge.to)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sprawdź czy punkt jest w prostokącie
     */
    private pointInRect(p: Point, rect: NodeBounds): boolean {
        return p.x >= rect.x && p.x <= rect.x + rect.width &&
            p.y >= rect.y && p.y <= rect.y + rect.height;
    }

    /**
     * Sprawdź czy dwie linie się przecinają
     */
    private linesIntersect(p1: Point, p2: Point, p3: Point, p4: Point): boolean {
        const denom = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y);
        if (Math.abs(denom) < this.EPS) return false; // Równoległe

        const ua = ((p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x)) / denom;
        const ub = ((p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x)) / denom;

        return ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1;
    }

    /**
     * Sprawdź czy path przecina jakiekolwiek nodes
     */
    private pathIntersectsNodes(points: Point[], excludeNodeIds?: Set<string>): boolean {
        if (points.length < 2) return false;

        for (let i = 0; i < points.length - 1; i++) {
            const p1 = points[i];
            const p2 = points[i + 1];

            for (const [nodeId, bounds] of this.nodeBounds.entries()) {
                if (excludeNodeIds && excludeNodeIds.has(nodeId)) continue;
                if (this.segmentIntersectsNode(p1, p2, bounds, excludeNodeIds)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Ustaw node bounds dla wykrywania kolizji
     */
    setNodeBounds(nodeId: string, bounds: NodeBounds): void {
        this.nodeBounds.set(nodeId, bounds);
    }

    /**
     * Wyczyść node bounds
     */
    clearNodeBounds(): void {
        this.nodeBounds.clear();
    }

    /**
     * Wybierz preferencję łamania (H-first vs V-first) na podstawie exit/entry side
     */
    private chooseBendPreference(exitSide: Side, entrySide: Side): 'H' | 'V' {
        const exitHorizontal = exitSide === 'left' || exitSide === 'right';
        return exitHorizontal ? 'H' : 'V';
    }

    /**
     * Rozbij diagonalny segment na dwa ortogonalne
     */
    private splitDiagonalSegment(prev: Point, cur: Point, prefer: 'H' | 'V'): Point[] {
        if (prefer === 'H') {
            return [{ x: prev.x, y: cur.y }, { x: cur.x, y: cur.y }];
        } else {
            return [{ x: cur.x, y: prev.y }, { x: cur.x, y: cur.y }];
        }
    }

    /**
     * Wymuś ortogonalność wszystkich segmentów
     */
    private ensureOrthogonalSegments(
        points: Point[],
        exitSide?: Side,
        entrySide?: Side
    ): Point[] {
        if (points.length < 2) return points;
        const pts = this.removeDuplicatePoints(points);
        const out: Point[] = [pts[0]];
        const prefer = (exitSide && entrySide)
            ? this.chooseBendPreference(exitSide, entrySide)
            : 'H';
        for (let i = 1; i < pts.length; i++) {
            const prev = out[out.length - 1];
            const cur = pts[i];
            if (Math.abs(prev.x - cur.x) < this.EPS || Math.abs(prev.y - cur.y) < this.EPS) {
                out.push({
                    x: Math.round(cur.x * 100) / 100,
                    y: Math.round(cur.y * 100) / 100
                });
            } else {
                const bends = this.splitDiagonalSegment(prev, cur, prefer);
                out.push(...bends);
            }
        }
        return this.simplifyColinear(out);
    }

    /**
     * Oblicz prostą ścieżkę orthogonal między dwoma punktami
     * 
     * Strategia:
     * 1. Generuj kandydatów HV, VH i U-shaped
     * 2. Sprawdź kolizje z nodes
     * 3. Wybierz najlepszy (bez kolizji, najkrótszy)
     * 4. Usuń kolinearne punkty
     * 5. Zwróć listę punktów (renderer doda zaokrąglone narożniki)
     */
    route(
        from: Point,
        to: Point,
        fromSide: Side,
        toSide: Side,
        excludeNodeIds?: Set<string>
    ): Point[] {
        const start = { x: from.x, y: from.y };
        const end = { x: to.x, y: to.y };

        const distX = Math.abs(start.x - end.x);
        const distY = Math.abs(start.y - end.y);

        // 1) If practically identical -> force separation (prevents 2-identical point paths)
        if (distX < this.MIN_SEPARATION_PX && distY < this.MIN_SEPARATION_PX) {
            return [
                start,
                { x: start.x + this.MIN_FORCED_SEPARATION, y: start.y },
                { x: end.x + this.MIN_FORCED_SEPARATION, y: end.y },
                end
            ];
        }

        // 2) Compute offsets (outside nodes)
        const fromOffset = this.offsetPoint(start, fromSide, this.OFFSET);
        const toOffset = this.offsetPoint(end, toSide, this.OFFSET);

        // 3) Build candidates: HV, VH, and U-shaped (for collision avoidance)
        const hv = this.buildHV(start, end, fromOffset, toOffset);
        const vh = this.buildVH(start, end, fromOffset, toOffset);
        const uPath = this.buildU(start, end, fromOffset, toOffset, fromSide, toSide);

        // 4) Test candidates and choose best (no collisions, shortest)
        const candidates: { path: Point[]; length: number; hasCollision: boolean }[] = [
            { path: hv, length: this.pathManhattanLength(hv), hasCollision: this.pathIntersectsNodes(hv, excludeNodeIds) },
            { path: vh, length: this.pathManhattanLength(vh), hasCollision: this.pathIntersectsNodes(vh, excludeNodeIds) },
            { path: uPath, length: this.pathManhattanLength(uPath), hasCollision: this.pathIntersectsNodes(uPath, excludeNodeIds) }
        ];

        // Wybierz najlepszy: najpierw bez kolizji, potem najkrótszy
        const safeCandidates = candidates.filter(c => !c.hasCollision);
        const bestCandidate = safeCandidates.length > 0
            ? safeCandidates.reduce((best, curr) => curr.length < best.length ? curr : best)
            : candidates.reduce((best, curr) => curr.length < best.length ? curr : best); // Jeśli wszystkie mają kolizje, wybierz najkrótszy

        let points = bestCandidate.path;

        // 5) Ensure orthogonality & cleanup (do NOT remove middle colinear points aggressively)
        points = this.ensureOrthogonalSegments(points, fromSide, toSide);

        // 6) Simplify - only remove exact duplicates or extremely close points
        points = this.removeDuplicatePoints(points);

        // 7) CRITICAL: Ensure start and end are different and path has minimum points
        // If start and end are too close or identical, force separation
        const finalStart = points[0] || start;
        const finalEnd = points[points.length - 1] || end;
        const finalDistX = Math.abs(finalStart.x - finalEnd.x);
        const finalDistY = Math.abs(finalStart.y - finalEnd.y);

        if (finalDistX < this.MIN_SEPARATION_PX && finalDistY < this.MIN_SEPARATION_PX) {
            // Start and end are identical - force L-shaped path
            points = [start, { x: start.x, y: start.y + this.MIN_FORCED_SEPARATION }, { x: end.x + this.MIN_FORCED_SEPARATION, y: end.y }, end];
        } else if (points.length < 4) {
            // Force an L-shaped path to keep visible
            points = [start, { x: start.x, y: end.y }, { x: end.x, y: end.y }, end];
        }

        // 8) CRITICAL: Final validation - ensure first and last points are different
        const validatedStart = points[0];
        const validatedEnd = points[points.length - 1];
        if (Math.abs(validatedStart.x - validatedEnd.x) < this.EPS && Math.abs(validatedStart.y - validatedEnd.y) < this.EPS) {
            // Last resort: create minimal visible path
            points = [
                { x: validatedStart.x, y: validatedStart.y },
                { x: validatedStart.x, y: validatedStart.y + this.MIN_FORCED_SEPARATION },
                { x: validatedEnd.x + this.MIN_FORCED_SEPARATION, y: validatedEnd.y + this.MIN_FORCED_SEPARATION },
                { x: validatedEnd.x, y: validatedEnd.y }
            ];
        }

        return points;
    }

    /**
     * Odsuń punkt od węzła w kierunku określonej strony
     */
    private offsetPoint(point: Point, side: Side, offset: number): Point {
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
     * Usuń duplikaty punktów
     */
    private removeDuplicates(points: Point[]): Point[] {
        if (points.length <= 1) return points;

        const result: Point[] = [points[0]];
        const EPS = 0.1;

        for (let i = 1; i < points.length; i++) {
            const prev = result[result.length - 1];
            const curr = points[i];

            if (Math.abs(prev.x - curr.x) > EPS || Math.abs(prev.y - curr.y) > EPS) {
                result.push(curr);
            }
        }

        return result;
    }
}

