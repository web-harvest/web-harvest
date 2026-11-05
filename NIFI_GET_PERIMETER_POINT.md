# Apache NiFi - getPerimeterPoint() Implementation

## Pełna implementacja funkcji obliczającej punkt na obwodzie prostokąta

```typescript
public getPerimeterPoint(p: Position, bBox: any): Position {
    // calculate theta
    const theta: number = Math.atan2(bBox.height, bBox.width);

    // get the rectangle radius
    const xRadius: number = bBox.width / 2;
    const yRadius: number = bBox.height / 2;

    // get the center point
    const cx: number = bBox.x + xRadius;
    const cy: number = bBox.y + yRadius;

    // calculate alpha
    const dx: number = p.x - cx;
    const dy: number = p.y - cy;
    let alpha: number = Math.atan2(dy, dx);

    // normalize alpha into 0 <= alpha < 2 PI
    alpha = alpha % CanvasUtils.TWO_PI;
    if (alpha < 0) {
        alpha += CanvasUtils.TWO_PI;
    }

    // calculate beta
    const beta: number = Math.PI / 2 - alpha;

    // detect the appropriate quadrant and return the point on the perimeter
    if ((alpha >= 0 && alpha < theta) || (alpha >= CanvasUtils.TWO_PI - theta && alpha < CanvasUtils.TWO_PI)) {
        // right quadrant
        return {
            x: bBox.x + bBox.width,
            y: cy + xRadius * Math.tan(alpha)
        };
    } else if (alpha >= theta && alpha < Math.PI - theta) {
        // bottom quadrant
        return {
            x: cx + yRadius * Math.tan(beta),
            y: bBox.y + bBox.height
        };
    } else if (alpha >= Math.PI - theta && alpha < Math.PI + theta) {
        // left quadrant
        return {
            x: bBox.x,
            y: cy - xRadius * Math.tan(alpha)
        };
    } else {
        // top quadrant
        return {
            x: cx - yRadius * Math.tan(beta),
            y: bBox.y
        };
    }
}
```

## Jak to działa:

1. **Oblicza kąt theta** - kąt między przekątną prostokąta a poziomą osią
2. **Oblicza środek prostokąta** (cx, cy)
3. **Oblicza kąt alpha** - kąt między punktem p a środkiem prostokąta
4. **Normalizuje alpha** do zakresu 0 <= alpha < 2π
5. **Określa kwadrant** i zwraca punkt na odpowiedniej krawędzi:
   - **Right quadrant** (prawa krawędź): `x = bBox.x + bBox.width`
   - **Bottom quadrant** (dolna krawędź): `y = bBox.y + bBox.height`
   - **Left quadrant** (lewa krawędź): `x = bBox.x`
   - **Top quadrant** (górna krawędź): `y = bBox.y`

## Zastosowanie w WebHarvest IDE:

Możemy użyć tej funkcji zamiast connection points z modelu, aby obliczyć punkt na obwodzie node najbliższy do anchor point.




