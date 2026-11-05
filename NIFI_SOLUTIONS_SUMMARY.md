# Apache NiFi - Rozwiązania dla renderowania połączeń

## 🔍 Kluczowe odkrycia z kodu źródłowego NiFi:

### 1. **Biblioteka: D3.js**
- NiFi używa **D3.js** do renderowania połączeń
- `d3.line()` z `d3.curveLinear` (proste linie, nie bezier curves)
- `d3.select()` do manipulacji SVG elementami

### 2. **Struktura danych połączenia:**
```typescript
{
    id: string;
    start: Position;      // Punkt na obwodzie source node
    end: Position;        // Punkt na obwodzie destination node
    bends: Position[];    // Punkty zgięcia (opcjonalne, użytkownik może dodawać)
    component: {
        bends: Position[];  // Oryginalne bends z backendu
        // ...
    }
}
```

### 3. **Generowanie path:**
```typescript
// Path = [start] + bends + [end]
const datum: Position[] = [d.start].concat(d.bends, [d.end]);
const pathData = d3.line()
    .x(d => d.x)
    .y(d => d.y)
    .curve(d3.curveLinear)(datum);

// Ustawienie w SVG:
connection.select('path.connection-path')
    .attr('d', pathData);
```

### 4. **getPerimeterPoint() - Kluczowa funkcja!**

**Co robi:** Oblicza punkt na obwodzie prostokąta (node) najbliższy do anchor point.

**Implementacja:**
```typescript
public getPerimeterPoint(p: Position, bBox: any): Position {
    const theta = Math.atan2(bBox.height, bBox.width);
    const xRadius = bBox.width / 2;
    const yRadius = bBox.height / 2;
    const cx = bBox.x + xRadius;
    const cy = bBox.y + yRadius;
    
    const dx = p.x - cx;
    const dy = p.y - cy;
    let alpha = Math.atan2(dy, dx);
    
    // Normalizuj alpha do 0 <= alpha < 2π
    alpha = alpha % (2 * Math.PI);
    if (alpha < 0) alpha += 2 * Math.PI;
    
    const beta = Math.PI / 2 - alpha;
    
    // Określ kwadrant i zwróć punkt na odpowiedniej krawędzi
    if ((alpha >= 0 && alpha < theta) || (alpha >= 2*Math.PI - theta && alpha < 2*Math.PI)) {
        // Right quadrant
        return { x: bBox.x + bBox.width, y: cy + Math.tan(alpha) * xRadius };
    } else if (alpha >= theta && alpha < Math.PI - theta) {
        // Bottom quadrant
        return { x: cx + Math.tan(beta) * yRadius, y: bBox.y + bBox.height };
    } else if (alpha >= Math.PI - theta && alpha < Math.PI + theta) {
        // Left quadrant
        return { x: bBox.x, y: cy - Math.tan(alpha) * xRadius };
    } else {
        // Top quadrant
        return { x: cx - Math.tan(beta) * yRadius, y: bBox.y };
    }
}
```

**Użycie:**
```typescript
// Oblicz start point na obwodzie source node
const startAnchor = d.bends.length > 0 ? d.bends[0] : end;
const start = canvasUtils.getPerimeterPoint(startAnchor, {
    x: sourceData.position.x,
    y: sourceData.position.y,
    width: sourceData.dimensions.width,
    height: sourceData.dimensions.height
});

// Oblicz end point na obwodzie destination node
const endAnchor = d.bends.length > 0 ? d.bends[d.bends.length - 1] : start;
const end = canvasUtils.getPerimeterPoint(endAnchor, {
    x: destinationData.position.x,
    y: destinationData.position.y,
    width: destinationData.dimensions.width,
    height: destinationData.dimensions.height
});
```

### 5. **Update Path - Kluczowy proces:**

```typescript
private updateConnections(updated: any, { updatePath = true }: ConnectionRenderOptions = {}) {
    if (updatePath) {
        // 1. Pobierz source i destination nodes
        const sourceComponentId = this.canvasUtils.getConnectionSourceComponentId(d);
        const sourceData = d3.select('#id-' + sourceComponentId).datum();
        const destinationComponentId = this.canvasUtils.getConnectionDestinationComponentId(d);
        const destinationData = d3.select('#id-' + destinationComponentId).datum();
        
        // 2. Oblicz end point (na obwodzie destination)
        let endAnchor;
        if (d.bends.length > 0) {
            endAnchor = d.bends[d.bends.length - 1];
        } else {
            endAnchor = {
                x: sourceData.position.x + sourceData.dimensions.width / 2,
                y: sourceData.position.y + sourceData.dimensions.height / 2
            };
        }
        
        const end = this.canvasUtils.getPerimeterPoint(endAnchor, {
            x: destinationData.position.x,
            y: destinationData.position.y,
            width: destinationData.dimensions.width,
            height: destinationData.dimensions.height
        });
        
        // 3. Oblicz start point (na obwodzie source)
        let startAnchor;
        if (d.bends.length > 0) {
            startAnchor = d.bends[0];
        } else {
            startAnchor = end;
        }
        
        const start = this.canvasUtils.getPerimeterPoint(startAnchor, {
            x: sourceData.position.x,
            y: sourceData.position.y,
            width: sourceData.dimensions.width,
            height: sourceData.dimensions.height
        });
        
        // 4. Zaktualizuj d.start i d.end
        d.start = start;
        d.end = end;
        
        // 5. Wygeneruj path: [start] + bends + [end]
        const datum: Position[] = [d.start].concat(d.bends, [d.end]);
        
        // 6. Zaktualizuj SVG path
        connection.select('path.connection-path')
            .attr('d', this.lineGenerator(datum));
    }
}
```

## 🎯 Co możemy zastosować w WebHarvest IDE:

### Rozwiązanie 1: getPerimeterPoint() zamiast connection points

**Problem:** Używamy connection points z modelu, które mogą być w złych miejscach.

**Rozwiązanie:** Oblicz punkt na obwodzie node najbliższy do anchor point używając `getPerimeterPoint()`.

**Implementacja:**
```typescript
// W _recalculateSingleConnection():
// Zamiast używać fromPoint.position i toPoint.position,
// oblicz punkty na obwodzie nodes:

const fromNodeBounds = {
    x: fromNode.position.x,
    y: fromNode.position.y,
    width: fromNode.computedBounds.width,
    height: fromNode.computedBounds.height
};

const toNodeBounds = {
    x: toNode.position.x,
    y: toNode.position.y,
    width: toNode.computedBounds.width,
    height: toNode.computedBounds.height
};

// Oblicz anchor point (środek connection point div)
const fromAnchorCenter = {
    x: fromEl.getBoundingClientRect().left + fromEl.getBoundingClientRect().width / 2,
    y: fromEl.getBoundingClientRect().top + fromEl.getBoundingClientRect().height / 2
};

// Konwertuj do SVG coords
const fromAnchorSvg = getAnchorSvgPosition(fromEl, this.svgElement);
if (!fromAnchorSvg) return; // pending

// Oblicz punkt na obwodzie source node najbliższy do anchor
const start = this.getPerimeterPoint(fromAnchorSvg, fromNodeBounds);

// To samo dla destination
const toAnchorSvg = getAnchorSvgPosition(toEl, this.svgElement);
if (!toAnchorSvg) return; // pending
const end = this.getPerimeterPoint(toAnchorSvg, toNodeBounds);

// Route używając start i end (już na obwodzie nodes)
const path = this.connectionRouter.route(start, end, fromPoint.side, toPoint.side);
```

### Rozwiązanie 2: Upewnij się że start i end są różne

**Problem:** `fromSvg` i `toSvg` mogą być identyczne.

**Rozwiązanie:** Sprawdź czy `start` i `end` są różne przed routowaniem.

```typescript
const distX = Math.abs(start.x - end.x);
const distY = Math.abs(start.y - end.y);
if (distX < 1 && distY < 1) {
    // Punkty są zbyt blisko - użyj minimalnego offsetu
    // Router już to obsługuje, ale możemy dodać dodatkową ochronę
    console.warn(`[CONN] start and end are too close: ${distX}, ${distY}`);
}
```

### Rozwiązanie 3: Path zawsze zaczyna się/kończy na obwodzie nodes

**Problem:** Path może zaczynać się w środku node zamiast na obwodzie.

**Rozwiązanie:** Użyj `getPerimeterPoint()` aby upewnić się że path zaczyna się/kończy na obwodzie.

## 📝 Pliki do modyfikacji:

1. **`VisualSemanticModelManager.ts`** - dodać `getPerimeterPoint()`
2. **`_recalculateSingleConnection()`** - użyć `getPerimeterPoint()` zamiast connection points
3. **`SimpleConnectionRouter.ts`** - upewnić się że router obsługuje przypadki gdy start/end są zbyt blisko

## 🔗 Linki do kodu NiFi:

- **ConnectionManager:** `/tmp/nifi-search/nifi-frontend/.../connection-manager.service.ts` (linia 919-1052)
- **CanvasUtils:** `/tmp/nifi-search/nifi-frontend/.../canvas-utils.service.ts` (linia 1053-1105)
- **getPerimeterPoint:** Pełna implementacja w `NIFI_GET_PERIMETER_POINT.md`




