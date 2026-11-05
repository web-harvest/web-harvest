/**
 * CoordinateUtils - Konwersja współrzędnych HTML (client) ↔ SVG
 * 
 * GŁÓWNY PROBLEM: Anchor points są renderowane jako HTML <div>,
 * ale używane w SVG bez konwersji → linie "rozjeżdżają się"
 * 
 * ROZWIĄZANIE: Zawsze konwertuj getBoundingClientRect() → SVG coordinates
 * przed użyciem w routerze i rendererze.
 */

export interface Point {
    x: number;
    y: number;
}

/**
 * Safe getter for CTM - may return null when SVG not yet laid out.
 */
function tryGetScreenCTM(svg: SVGSVGElement): DOMMatrix | null {
    try {
        return svg.getScreenCTM ? svg.getScreenCTM() : null;
    } catch (e) {
        return null;
    }
}

/**
 * Snap to half-pixel for crisp 1px strokes (helps avoid subpixel blur).
 */
export function snapToHalfPixel(p: Point): Point {
    // use devicePixelRatio-aware snapping if desired
    const ratio = window.devicePixelRatio || 1;
    // snap to 0.5 device pixels
    const px = 1 / ratio / 2;
    return {
        x: Math.round(p.x / px) * px,
        y: Math.round(p.y / px) * px,
    };
}

/**
 * Convert client (DOM) coordinates -> SVG coordinates.
 * If CTM is not available, returns null so caller may retry later (raf).
 */
export function clientToSvgMaybe(svg: SVGSVGElement, clientX: number, clientY: number): Point | null {
    const ctm = tryGetScreenCTM(svg);
    if (!ctm) return null;

    const pt = svg.createSVGPoint();
    pt.x = clientX;
    pt.y = clientY;
    const inv = ctm.inverse();
    const svgP = pt.matrixTransform(inv);
    return { x: svgP.x, y: svgP.y };
}

/**
 * Wrapper: tries conversion, returns fallback based on boundingClientRect only
 * if CTM unavailable AND caller passes fallback=true. Prefer callers to handle null.
 */
export function clientToSvgPoint(svg: SVGSVGElement, clientX: number, clientY: number, fallback = false): Point | null {
    const p = clientToSvgMaybe(svg, clientX, clientY);
    if (p) return snapToHalfPixel(p);
    if (!fallback) return null;

    // Fallback (best-effort): map relative to svg bounding box (does not handle transform)
    const r = svg.getBoundingClientRect();
    const approx = { x: clientX - r.left, y: clientY - r.top };

    const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_CONNECTIONS__);
    if (shouldLog) {
        console.warn(`[CoordinateUtils] ⚠️ CTM is null, using fallback for (${clientX}, ${clientY}) => (${approx.x}, ${approx.y})`);
        console.log("[CTM]", svg.getScreenCTM(), "client:", clientX, clientY, "=> svg (fallback):", approx.x, approx.y);
    }

    return snapToHalfPixel(approx);
}

/**
 * Pobierz pozycję anchor div w przestrzeni SVG
 * 
 * Konwertuje center of anchor div → SVG coordinates
 * 
 * CRITICAL: getBoundingClientRect() automatycznie uwzględnia:
 * - Scroll position (canvas może być przewinięty w dowolnym kierunku)
 * - CSS transformy
 * - Viewport position
 * 
 * getScreenCTM().inverse() konwertuje viewport coords → SVG user units
 * To działa poprawnie nawet gdy canvas jest przewinięty, bo SVG jest pozycjonowany
 * względem canvas (position: absolute, top: 0, left: 0), a getScreenCTM() uwzględnia
 * wszystkie transformacje, w tym scroll.
 * 
 * @param anchorEl Element HTML (div) reprezentujący anchor point
 * @param svgElement Element SVG
 * @param fallback Jeśli true, używa fallback gdy CTM jest null (nie zalecane)
 * @returns Point w przestrzeni SVG lub null jeśli CTM nie jest dostępny
 */
export function getAnchorSvgPosition(anchorEl: HTMLElement, svgElement: SVGSVGElement, fallback = false): Point | null {
    const rect = anchorEl.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    return clientToSvgPoint(svgElement, centerX, centerY, fallback);
}

/**
 * Konwertuj współrzędne SVG → client (screen)
 */
export function svgToClient(svg: SVGSVGElement, svgX: number, svgY: number): Point {
    const pt = svg.createSVGPoint();
    pt.x = svgX;
    pt.y = svgY;
    const ctm = tryGetScreenCTM(svg);
    if (!ctm) {
        const r = svg.getBoundingClientRect();
        return { x: r.left + svgX, y: r.top + svgY };
    }
    const clientP = pt.matrixTransform(ctm);
    return { x: clientP.x, y: clientP.y };
}

/**
 * Snap do piksela z uwzględnieniem devicePixelRatio i strokeWidth
 * 
 * Wyrównuje do połówki piksela jeśli grubość stroke wymaga (by linia była ostra).
 * Funkcja uwzględnia devicePixelRatio i strokeWidth.
 * 
 * @param value Wartość do wyrównania
 * @param strokeWidth Grubość linii (domyślnie 2.5)
 * @returns Wyrównana wartość
 */
export function snapToPixel(value: number, strokeWidth: number = 2.5): number {
    const dpr = typeof window !== 'undefined' ? (window.devicePixelRatio || 1) : 1;
    const eff = Math.round(strokeWidth * dpr);
    const needsHalf = (eff % 2) === 1;
    const rounded = Math.round(value * dpr) + (needsHalf ? 0.5 : 0);
    return rounded / dpr;
}

/**
 * Snap punkt do piksela
 * 
 * Uwzględnia devicePixelRatio i strokeWidth dla precyzyjnego wyrównania
 */
export function snapPointToPixel(p: Point, strokeWidth: number = 2.5): Point {
    return {
        x: snapToPixel(p.x, strokeWidth),
        y: snapToPixel(p.y, strokeWidth)
    };
}

/**
 * Sprawdź czy dwa punkty są równe (z tolerancją)
 */
export function pointsEqual(p1: Point, p2: Point, eps: number = 1e-6): boolean {
    return Math.abs(p1.x - p2.x) < eps && Math.abs(p1.y - p2.y) < eps;
}

/**
 * Oblicz odległość między dwoma punktami
 */
export function distance(p1: Point, p2: Point): number {
    const dx = p1.x - p2.x;
    const dy = p1.y - p2.y;
    return Math.sqrt(dx * dx + dy * dy);
}

/**
 * Oblicz odległość Manhattan między dwoma punktami
 */
export function manhattanDistance(p1: Point, p2: Point): number {
    return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
}
