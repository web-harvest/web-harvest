# Prompt dla ChatGPT - Naprawa renderowania linii między etykietami

## Kontekst projektu

Projekt: **WebHarvest IDE** - visual designer dla workflow automation (podobny do NiFi/Conductor).

**Architektura:**
- Frontend: TypeScript + React-like (bez React, vanilla JS)
- Backend: Java (Spring Boot + Jetty)
- Canvas: HTML5 Canvas z SVG overlay dla connections
- Nodes: Renderowane jako HTML `<div>` (pozycjonowane absolutnie)
- Connections: Renderowane jako SVG `<path>` w overlay SVG
- Connection Points (Anchors): Renderowane jako HTML `<div>` (pozycjonowane absolutnie)

**Problem:** Linie między etykietami (nodes) nie są widoczne lub są w złych miejscach.

---

## Co zostało zrobione (historia zmian)

### 1. **Coordinate Conversion (HTML → SVG)**
- Utworzono `CoordinateUtils.ts` z funkcjami:
  - `clientToSvgPoint()` - konwersja client coords → SVG coords używając `getScreenCTM().inverse()`
  - `getAnchorSvgPosition()` - konwersja center of anchor div → SVG coords
  - `snapPointToPixel()` - snap do piksela z uwzględnieniem `devicePixelRatio`

### 2. **Orthogonal Routing**
- `SimpleConnectionRouter` - prosty router tworzący L-shaped paths (HV lub VH)
- `ensureOrthogonalSegments()` - wymusza ortogonalność (tylko poziome/pionowe segmenty)
- `removeDuplicatePoints()` i `simplifyColinear()` - czyszczenie path points

### 3. **Synchronization & Cache Management**
- `VisualSemanticModelManager`:
  - `anchorElements: Map<string, HTMLElement>` - przechowuje referencje do anchor DOM elements
  - `pendingConnections: Set<string>` - connections czekające na anchors
  - `pendingConnectionsData: Map<string, {...}>` - connection data dla pending connections
  - `registerAnchor()` / `registerSvgElement()` - rejestracja anchor elements i SVG
  - `recalculateAllPendingConnectionsSync()` - przeliczenie pending connections
  - `_recalculateSingleConnection()` - przeliczenie pojedynczego connection

### 4. **Scroll Support**
- `ConnectionPointsRenderer` ma scroll listener - rekalkulacja po scroll canvas

### 5. **Marker Nodes (workflow-start/end)**
- `workflow-start` i `workflow-end` są traktowane jak regular nodes
- Mają connection points na wszystkich 4 stronach (top, bottom, left, right)
- Każda strona ma 3 punkty (start, center, end) - tylko center points są używane

---

## Obecne problemy (diagnoza z testów Selenium)

### Test Results:
```
✅ Markery (start/stop): workflow-start i workflow-end istnieją
✅ Start connection points: 24
✅ End connection points: 24
✅ Zarejestrowane anchory: 56
⚠️  Połączenia:
   - Wszystkie: 10
   - Start -> Node: 1
   - Node -> End: 1
   - Node -> Node: 8
   - Pending (w modelu): 8
   - Pending (w managerze): 2
   - Z anchors: 10 (wszystkie mają anchors ustawione!)

❌ Node-to-Node connections:
   - Path istnieje w DOM (SVG path element)
   - rect: width=0.0, height=0.0
   - unique points: 0 (wszystkie punkty są takie same!)
   - path points: 2 (tylko 2 punkty w path)
```

### Przykładowe connection data:
```javascript
{
  id: "conn-node-xxx-node-yyy-0",
  from: "node-xxx",
  to: "node-yyy",
  fromAnchor: "output-default-bottom-center",
  toAnchor: "input-default-top-center",
  pathPoints: 2  // ❌ Tylko 2 punkty!
}
```

### Klucze anchorów (przykładowe):
```
workflow-start/workflow-start-top-center-input
workflow-start/workflow-start-bottom-center-input
node-xxx/output-default-bottom-center
node-yyy/input-default-top-center
```

---

## Główne problemy do rozwiązania

### Problem 1: Path ma tylko 2 punkty i są takie same
**Symptomy:**
- `path.points.length === 2`
- `uniquePointsCount === 0` (wszystkie punkty są identyczne)
- `rect.width === 0 && rect.height === 0`

**Możliwe przyczyny:**
1. Router zwraca path z 2 punktami, które są identyczne
2. `removeCollinear()` usuwa wszystkie środkowe punkty
3. `fromSvg` i `toSvg` są takie same (lub bardzo blisko)
4. Router nie jest wywoływany lub zwraca błędny path

**Kod routera:**
```typescript
// SimpleConnectionRouter.route()
route(from: Point, to: Point, fromSide: Side, toSide: Side): Point[] {
    const start = { x: from.x, y: from.y };
    const fromOffset = this.offsetPoint(from, fromSide, this.OFFSET);
    const toOffset = this.offsetPoint(to, toSide, this.OFFSET);
    const end = { x: to.x, y: to.y };
    
    // Build HV or VH candidate
    const hvCandidate = this.buildHV(start, end, fromOffset, toOffset);
    const vhCandidate = this.buildVH(start, end, fromOffset, toOffset);
    
    // Choose shorter
    points = hvLength <= vhLength ? hvCandidate : vhCandidate;
    
    // Clean up
    const cleaned = this.ensureOrthogonalSegments(points, fromSide, toSide);
    const simplified = this.removeCollinear(cleaned);
    
    return simplified;
}
```

### Problem 2: Connections są pending w modelu, ale mają anchors
**Symptomy:**
- `connections.filter(c => c.anchors?.from && c.anchors?.to).length === 10` (wszystkie mają anchors!)
- Ale `pending.length === 8` (8 connections są pending w modelu)
- `pendingConnections.size === 2` (tylko 2 w managerze)

**Możliwe przyczyny:**
1. `buildConnectionStates()` tworzy connection state z błędnym path (2 punkty, takie same)
2. `_recalculateSingleConnection()` nie aktualizuje connection state w modelu
3. Connection state jest tworzony przed anchors są gotowe, a później nie jest aktualizowany

**Kod `buildConnectionStates()`:**
```typescript
// Jeśli anchor elements nie istnieją, connection jest pending
if (!fromAnchorDiv || !toAnchorDiv || !svgElement) {
    isPending = true;
    // ❌ NIE TWORZY connection state - dodaje do pendingConnections
    this.pendingConnections.add(connectionId);
    return; // Skip
}

// Jeśli pending, connection state nie jest tworzony
// Ale później _recalculateSingleConnection powinien go utworzyć
```

**Kod `_recalculateSingleConnection()`:**
```typescript
private _recalculateSingleConnection(connId: string): void {
    // Pobierz connection z modelu lub pendingConnectionsData
    let conn = this.model?.workflow.connections.get(connId);
    if (!conn) {
        const connectionData = this.pendingConnectionsData.get(connId);
        // Utwórz temporary connection object
    }
    
    // Znajdź connection points (prefer center points)
    const fromPoint = fromNode.connectionPoints.outputs.filter(p => p.id.includes('-center'))[0];
    const toPoint = toNode.connectionPoints.inputs.filter(p => p.id.includes('-center'))[0];
    
    // Konwertuj anchor DOM → SVG coords
    const fromKey = `${fromNode.nodeId}/${fromPoint.id}`;
    const toKey = `${toNode.nodeId}/${toPoint.id}`;
    const fromEl = this.anchorElements.get(fromKey);
    const toEl = this.anchorElements.get(toKey);
    
    if (this.svgElement && fromEl) {
        fromSvg = getAnchorSvgPosition(fromEl, this.svgElement);
    } else {
        // ❌ Mark as pending - nie używa fallback coords
        this.pendingConnections.add(connId);
        return;
    }
    
    // Route
    const path = this.connectionRouter.route(fromSvg, toSvg, fromPoint.side, toPoint.side);
    
    // ❓ Czy path jest poprawny? (2 punkty, takie same?)
    
    // Update connection state in model
    this.model!.workflow.connections.set(connId, updatedConnectionState);
    
    // Remove from pending
    this.pendingConnections.delete(connId);
}
```

### Problem 3: Connection state nie jest aktualizowany w modelu
**Symptomy:**
- Connections mają anchors w modelu
- Ale path ma tylko 2 punkty (prawdopodobnie z `buildConnectionStates()`)
- `_recalculateSingleConnection()` jest wywoływane, ale path nie jest aktualizowany

**Możliwe przyczyny:**
1. `_recalculateSingleConnection()` nie aktualizuje connection state w modelu
2. `notifyUpdate()` nie jest wywoływane po aktualizacji
3. Connection state jest nadpisywany przez `buildConnectionStates()` przy następnym `buildModel()`

---

## Co ChatGPT ma pomóc naprawić

### 1. **Diagnoza: Dlaczego path ma tylko 2 punkty?**
- Sprawdź czy `fromSvg` i `toSvg` są różne
- Sprawdź czy router zwraca poprawny path (5 punktów: start, fromOffset, middle, toOffset, end)
- Sprawdź czy `removeCollinear()` nie usuwa wszystkich środkowych punktów
- Dodaj logowanie do routera i `_recalculateSingleConnection()`

### 2. **Naprawa: Zapewnij że path ma więcej niż 2 punkty**
- Jeśli `fromSvg` i `toSvg` są zbyt blisko, użyj minimalnego offsetu
- Jeśli router zwraca tylko 2 punkty, dodaj środkowy punkt
- Sprawdź czy `removeCollinear()` nie jest zbyt agresywny

### 3. **Naprawa: Zapewnij że connection state jest aktualizowany**
- Sprawdź czy `_recalculateSingleConnection()` aktualizuje connection state w modelu
- Sprawdź czy `notifyUpdate()` jest wywoływane po aktualizacji
- Sprawdź czy `buildConnectionStates()` nie nadpisuje zaktualizowanego connection state

### 4. **Debug: Dodaj szczegółowe logowanie**
- Loguj `fromSvg`, `toSvg`, `path` przed i po routingu
- Loguj `anchorElements.keys()` i porównaj z `fromKey`/`toKey`
- Loguj czy connection state jest aktualizowany w modelu

---

## Pliki do sprawdzenia/zmodyfikowania

1. **`webharvest-ide/frontend/src/visual/VisualSemanticModelManager.ts`**
   - `buildConnectionStates()` - linia ~1440
   - `_recalculateSingleConnection()` - linia ~397
   - `recalculateConnectionsForSetSync()` - linia ~341

2. **`webharvest-ide/frontend/src/visual/SimpleConnectionRouter.ts`**
   - `route()` - linia ~162
   - `removeCollinear()` - linia ~63
   - `buildHV()` / `buildVH()` - linia ~93, ~100

3. **`webharvest-ide/frontend/src/visual/CoordinateUtils.ts`**
   - `getAnchorSvgPosition()` - linia ~138
   - `clientToSvgPoint()` - linia ~45

4. **`webharvest-ide/frontend/src/visual/ConnectionPointsRenderer.ts`**
   - `renderPoint()` - linia ~234
   - `registerAnchor()` - wywołanie w linii ~310

---

## Oczekiwany rezultat

Po naprawie:
- ✅ Wszystkie connections mają path z >2 punktami
- ✅ `uniquePointsCount > 1` dla wszystkich connections
- ✅ `rect.width > 0 || rect.height > 0` dla wszystkich paths
- ✅ `pendingConnections.size === 0` (wszystkie connections są przeliczone)
- ✅ Linie są widoczne między wszystkimi etykietami

---

## Dodatkowe informacje

- Canvas może być przewijany w dowolnym kierunku (scroll support jest zaimplementowany)
- Connection points są renderowane jako HTML divs (32px dla center points)
- SVG overlay ma `z-index: 10` i `position: absolute`
- Connection points container ma `z-index: 1000`
- Wszystkie connection points używają center points (z `-center` w ID)

---

## Pytania do ChatGPT

1. **Dlaczego router zwraca path z tylko 2 punktami?** Czy `fromSvg` i `toSvg` są zbyt blisko, czy `removeCollinear()` jest zbyt agresywny?

2. **Dlaczego connection state nie jest aktualizowany?** Czy `_recalculateSingleConnection()` aktualizuje model, czy `buildConnectionStates()` nadpisuje zmiany?

3. **Jak zapewnić że path zawsze ma >2 punkty?** Czy dodać minimalny offset, czy zmienić logikę routera?

4. **Jak zsynchronizować connection state z anchors?** Czy użyć `notifyUpdate()` po każdej aktualizacji, czy innego mechanizmu?

5. **Jak debugować problem?** Jakie logi dodać, żeby zobaczyć co się dzieje?

---

**WAŻNE:** Wszystkie zmiany muszą być zgodne z modelem semantycznym - connection state musi być źródłem prawdy, a wszystkie dane (path, anchors, etc.) muszą pochodzić z modelu semantycznego.

