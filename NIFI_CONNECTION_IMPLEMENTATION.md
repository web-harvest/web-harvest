# Apache NiFi - Implementacja renderowania połączeń

## Kluczowe odkrycia z kodu źródłowego:

### 1. **Biblioteka: D3.js**
NiFi używa **D3.js** do renderowania połączeń na canvas.

### 2. **Connection Manager Service**
Plik: `connection-manager.service.ts` (~2382 linie)

**Kluczowe elementy:**

#### Line Generator:
```typescript
this.lineGenerator = d3
    .line()
    .x(function (d: any) {
        return d.x;
    })
    .y(function (d: any) {
        return d.y;
    })
    .curve(d3.curveLinear);  // Proste linie, nie bezier!
```

#### Struktura danych połączenia:
```typescript
{
    id: string;
    start: Position;      // Punkt startowy na obwodzie source node
    end: Position;        // Punkt końcowy na obwodzie destination node
    bends: Position[];    // Punkty zgięcia (bend points) - użytkownik może je dodawać/usuwać
    component: {
        bends: Position[];  // Oryginalne bends z backendu
        // ... inne właściwości
    }
}
```

#### Generowanie path:
```typescript
// Path składa się z: [start] + bends + [end]
const datum: Position[] = [d.start].concat(d.bends, [d.end]);
connection.select('path.connection-path')
    .attr('d', function () {
        return self.lineGenerator(datum);
    });
```

### 3. **Obliczanie punktów na obwodzie (Perimeter Points)**

**Funkcja:** `getPerimeterPoint(anchor, bounds)`

- Oblicza punkt na obwodzie prostokąta (node) najbliższy do `anchor`
- Używa tego do określenia gdzie connection zaczyna się/kończy na node

**Użycie:**
```typescript
// Start point - na obwodzie source node
const start: Position = self.canvasUtils.getPerimeterPoint(startAnchor, {
    x: sourceData.position.x,
    y: sourceData.position.y,
    width: sourceData.dimensions.width,
    height: sourceData.dimensions.height
});

// End point - na obwodzie destination node
const end: Position = self.canvasUtils.getPerimeterPoint(endAnchor, {
    x: destinationData.position.x,
    y: destinationData.position.y,
    width: destinationData.dimensions.width,
    height: destinationData.dimensions.height
});
```

### 4. **Bend Points (Punkty zgięcia)**

- Użytkownik może dodawać bend points przez **double-click** na connection line
- Użytkownik może usuwać bend points przez **double-click** na bend point
- Bend points można przeciągać (drag)
- Snap do grid (8px) - można wyłączyć przez Shift

### 5. **Renderowanie:**

```typescript
// 1. Render connection group
const connection = entered
    .append('g')
    .attr('class', 'connection');

// 2. Render path (główna linia)
connection.append('path')
    .attr('class', 'connection-path')
    .attr('pointer-events', 'none');

// 3. Render selection path (dla zaznaczenia)
connection.append('path')
    .attr('class', 'connection-selection-path')
    .attr('pointer-events', 'none');

// 4. Render selectable path (dla interakcji)
connection.append('path')
    .attr('class', 'connection-path-selectable')
    .attr('pointer-events', 'stroke')
    .on('mousedown.selection', ...)
    .on('dblclick', ...);  // Dodawanie bend point

// 5. Render bend points (startpoint, endpoint, midpoints)
connection.append('rect')
    .attr('class', 'startpoint linepoint')
    .attr('width', 8)
    .attr('height', 8);
```

### 6. **Update Path:**

```typescript
private updateConnections(updated: any, { updatePath = true, updateLabel = true }: ConnectionRenderOptions = {}) {
    if (updatePath) {
        // Oblicz start i end points na obwodzie nodes
        const start: Position = self.canvasUtils.getPerimeterPoint(startAnchor, sourceBounds);
        const end: Position = self.canvasUtils.getPerimeterPoint(endAnchor, destinationBounds);
        
        // Zaktualizuj d.start i d.end
        d.start = start;
        d.end = end;
        
        // Wygeneruj path: [start] + bends + [end]
        const datum: Position[] = [d.start].concat(d.bends, [d.end]);
        
        // Zaktualizuj SVG path
        connection.select('path.connection-path')
            .attr('d', function () {
                return self.lineGenerator(datum);
            });
    }
}
```

## Różnice między NiFi a WebHarvest IDE:

### NiFi:
- ✅ Używa D3.js `d3.line()` z `d3.curveLinear` (proste linie)
- ✅ Ma bend points (użytkownik może dodawać/usuwać/przeciągać)
- ✅ Oblicza punkty na obwodzie nodes (`getPerimeterPoint`)
- ✅ Path: `[start] + bends + [end]`
- ✅ Snap do grid (8px)

### WebHarvest IDE (obecna implementacja):
- ✅ Używa własnego routera (`SimpleConnectionRouter`)
- ✅ Orthogonal routing (L-shaped, U-shaped)
- ✅ Automatyczne obliczanie path (bez bend points)
- ✅ Path: `[start, offset1, middle, offset2, end]` (automatycznie generowany)
- ❌ Brak bend points (użytkownik nie może ich dodawać)

## Co możemy zastosować z NiFi:

### 1. **getPerimeterPoint() - Obliczanie punktów na obwodzie**
Zamiast używać connection points z modelu, możemy obliczyć punkt na obwodzie node najbliższy do anchor point.

### 2. **D3.js line generator**
Możemy użyć `d3.line()` zamiast ręcznego generowania path data.

### 3. **Bend points (opcjonalnie)**
Możemy dodać możliwość dodawania bend points przez użytkownika (double-click na connection).

### 4. **Snap to grid**
Możemy dodać snap do grid (8px) dla bend points.

## Rekomendacje dla WebHarvest IDE:

### Krótkoterminowe (natychmiastowe):
1. **Użyj `getPerimeterPoint()`** - oblicz punkt na obwodzie node zamiast używać connection points
2. **Upewnij się że path zawsze ma >2 punkty** - już mamy to w routerze
3. **Sprawdź czy `d.start` i `d.end` są różne** - przed generowaniem path

### Długoterminowe (opcjonalne):
1. **Dodaj D3.js** - użyj `d3.line()` do generowania path
2. **Dodaj bend points** - pozwól użytkownikowi dodawać/usuwać/przeciągać bend points
3. **Snap to grid** - dla bend points

## Linki do kodu NiFi:

1. **ConnectionManager:**
   - `/tmp/nifi-search/nifi-frontend/src/main/frontend/apps/nifi/src/app/pages/flow-designer/service/manager/connection-manager.service.ts`

2. **CanvasUtils (getPerimeterPoint):**
   - `/tmp/nifi-search/nifi-frontend/src/main/frontend/apps/nifi/src/app/pages/flow-designer/service/canvas-utils.service.ts`

3. **Canvas Component:**
   - `/tmp/nifi-search/nifi-frontend/src/main/frontend/apps/nifi/src/app/pages/flow-designer/ui/canvas/canvas.component.ts`




