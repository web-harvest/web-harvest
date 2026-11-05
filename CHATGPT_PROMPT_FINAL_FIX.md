# Prompt dla ChatGPT - Finalna naprawa renderowania linii między etykietami

## Kontekst projektu

**Projekt:** WebHarvest IDE - visual designer dla workflow automation (podobny do NiFi/Conductor).

**Architektura:**
- Frontend: TypeScript (vanilla JS, bez React)
- Backend: Java (Spring Boot + Jetty)
- Canvas: HTML5 Canvas z SVG overlay dla connections
- Nodes: Renderowane jako HTML `<div>` (pozycjonowane absolutnie)
- Connections: Renderowane jako SVG `<path>` w overlay SVG
- Connection Points (Anchors): Renderowane jako HTML `<div>` (pozycjonowane absolutnie)

**Problem:** Linie między etykietami (nodes) **NIE SĄ WIDOCZNE** lub są w złych miejscach.

---

## Co zostało już zrobione (historia zmian)

### 1. **Coordinate Conversion (HTML → SVG)**
- `CoordinateUtils.ts` z funkcjami:
  - `clientToSvgPoint()` - konwersja client coords → SVG coords używając `getScreenCTM().inverse()`
  - `getAnchorSvgPosition()` - konwersja center of anchor div → SVG coords
  - `snapPointToPixel()` - snap do piksela z uwzględnieniem `devicePixelRatio`
- **Dodano retry CTM** - próba ponownego pobrania CTM jeśli jest null

### 2. **Orthogonal Routing**
- `SimpleConnectionRouter` - prosty router tworzący L-shaped paths (HV lub VH)
- `ensureOrthogonalSegments()` - wymusza ortogonalność (tylko poziome/pionowe segmenty)
- `removeDuplicatePoints()` i `simplifyColinear()` - czyszczenie path points
- **Dodano minimalny offset** - jeśli punkty są zbyt blisko (<1px), router zwraca path z offsetem
- **Usunięto `removeCollinear()`** - było zbyt agresywne
- **Wymuszenie min 4 punktów** - jeśli path ma <4 punkty, router zwraca L-shaped path

### 3. **Synchronization & Cache Management**
- `VisualSemanticModelManager`:
  - `anchorElements: Map<string, HTMLElement>` - przechowuje referencje do anchor DOM elements
  - `pendingConnections: Set<string>` - connections czekające na anchors
  - `pendingConnectionsData: Map<string, {...}>` - connection data dla pending connections
  - `registerAnchor()` / `registerSvgElement()` - rejestracja anchor elements i SVG
  - `recalculateAllPendingConnectionsSync()` - przeliczenie pending connections
  - `_recalculateSingleConnection()` - przeliczenie pojedynczego connection
- **Dodano `notifyUpdate()`** po aktualizacji connection state

### 4. **Scroll Support**
- `ConnectionPointsRenderer` ma scroll listener - rekalkulacja po scroll canvas

### 5. **Marker Nodes (workflow-start/end)**
- `workflow-start` i `workflow-end` są traktowane jak regular nodes
- Mają connection points na wszystkich 4 stronach (top, bottom, left, right)
- Każda strona ma 3 punkty (start, center, end) - tylko center points są używane

### 6. **Build Connection States Protection**
- `buildConnectionStates()` **NIE nadpisuje** istniejących connections jeśli już mają >2 punkty

### 7. **Debug Logging**
- Dodano szczegółowe logowanie w `_recalculateSingleConnection()`
- Dodano logowanie CTM w `CoordinateUtils`

---

## Obecny stan (po wszystkich poprawkach)

### Test Results (Selenium):
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

### Problem nadal występuje:
- **Path ma tylko 2 punkty i są identyczne**
- **Connections są pending w modelu, ale mają anchors**
- **Connection state NIE jest aktualizowany** - pending connections nigdy nie wychodzą z pending

---

## Analiza kodu - co może być nie tak

### 1. **Router zwraca 2 punkty**

**Kod routera (`SimpleConnectionRouter.ts`):**
```typescript
route(from: Point, to: Point, fromSide: Side, toSide: Side): Point[] {
    // CRITICAL FIX 1: Minimalny offset jeśli punkty są zbyt blisko
    const distX = Math.abs(from.x - to.x);
    const distY = Math.abs(from.y - to.y);
    if (distX < 1 && distY < 1) {
        // Punkty są identyczne lub zbyt blisko - wymuś minimalny offset
        const MIN_SEPARATION = 20;
        return [
            from,
            { x: from.x + MIN_SEPARATION, y: from.y },  // SOFT OFFSET
            { x: to.x + MIN_SEPARATION, y: to.y },
            to
        ];
    }

    // ... routing logic ...
    
    // 5. ULEPSZENIE: Wymuś ortogonalność i usuń zbędne punkty
    const cleaned = this.ensureOrthogonalSegments(points, fromSide, toSide);
    
    // CRITICAL FIX 2: USUŃ removeCollinear() - jest zbyt agresywny
    // Zamiast tego użyj tylko simplifyColinear z większym EPS
    // const simplified = this.removeCollinear(cleaned);  // ❌ USUNIĘTE

    // CRITICAL FIX 3: Wymuś min 4 punkty
    let simplified = this.simplifyColinear(cleaned);

    if (simplified.length < 4) {
        // Force L shape - minimum 4 punkty
        simplified = [
            start,
            { x: start.x, y: end.y },
            { x: end.x, y: end.y },
            end
        ];
    }

    return simplified;
}
```

**Możliwe problemy:**
- `fromSvg` i `toSvg` mogą być identyczne (CTM null → fallback zwraca błędne coords)
- `simplifyColinear()` może usuwać wszystkie środkowe punkty
- Minimalny offset może nie działać jeśli `from` i `to` są identyczne

### 2. **Connection State nie jest aktualizowany**

**Kod `_recalculateSingleConnection()` (`VisualSemanticModelManager.ts`):**
```typescript
private _recalculateSingleConnection(connId: string): void {
    // ... znajdź connection points ...
    
    // Konwertuj anchor DOM → SVG coords
    const fromKey = `${fromNode.nodeId}/${fromPoint.id}`;
    const toKey = `${toNode.nodeId}/${toPoint.id}`;
    const fromEl = this.anchorElements.get(fromKey);
    const toEl = this.anchorElements.get(toKey);
    
    if (this.svgElement && fromEl) {
        fromSvg = getAnchorSvgPosition(fromEl, this.svgElement);
    } else {
        // Anchor element not ready - mark as pending
        this.pendingConnections.add(connId);
        return;
    }
    
    if (this.svgElement && toEl) {
        toSvg = getAnchorSvgPosition(toEl, this.svgElement);
    } else {
        // Anchor element not ready - mark as pending
        this.pendingConnections.add(connId);
        return;
    }
    
    // Route
    const path = this.connectionRouter.route(fromSvg, toSvg, fromPoint.side, toPoint.side);
    
    // ... build updatedConnectionState ...
    
    // CRITICAL FIX 3: Update model semantyczny
    this.model!.workflow.connections.set(connId, updatedConnectionState);
    
    // CRITICAL FIX 3: Wywołaj notifyUpdate()
    this.notifyUpdate();

    // Remove from pending
    this.pendingConnections.delete(connId);
    this.pendingConnectionsData.delete(connId);
}
```

**Możliwe problemy:**
- `fromSvg` i `toSvg` mogą być identyczne → router zwraca 2 punkty
- `notifyUpdate()` może nie wywoływać renderera
- Connection state może być nadpisywany przez `buildConnectionStates()` przy następnym `buildModel()`

### 3. **Build Connection States Protection**

**Kod `buildConnectionStates()` (`VisualSemanticModelManager.ts`):**
```typescript
// CRITICAL FIX 4: NIE nadpisuj istniejących connections jeśli już były przeliczony
if (this.model?.workflow.connections.has(connectionId)) {
    const existing = this.model.workflow.connections.get(connectionId);
    // Jeśli connection ma już poprawny path (>2 punkty), nie nadpisuj
    if (existing && existing.path && existing.path.points && existing.path.points.length > 2) {
        connectionStates.set(connectionId, existing);
        return; // Skip - connection already computed
    }
}
```

**Możliwe problemy:**
- Sprawdzenie `length > 2` może być zbyt restrykcyjne (może być 2 punkty, ale różne)
- Connection może mieć 2 punkty, ale są identyczne → nie jest aktualizowany

### 4. **Coordinate Conversion - CTM może być null**

**Kod `clientToSvgPoint()` (`CoordinateUtils.ts`):**
```typescript
export function clientToSvgPoint(svg: SVGSVGElement, clientX: number, clientY: number): Point {
    const pt = svg.createSVGPoint();
    pt.x = clientX;
    pt.y = clientY;

    // CRITICAL FIX 1: Retry CTM - może być null przy pierwszym renderze
    let ctm = safeGetScreenCTM(svg);
    if (!ctm) {
        // Retry 1: immediate retry
        ctm = safeGetScreenCTM(svg);
    }
    if (!ctm) {
        // Fallback: przelicz względem boundingClientRect
        const r = svg.getBoundingClientRect();
        const fallback = { x: clientX - r.left, y: clientY - r.top };
        // ⚠️ PROBLEM: Fallback może zwracać błędne coords jeśli SVG ma transform/zoom
        return fallback;
    }

    const inv = ctm.inverse();
    const svgP = pt.matrixTransform(inv);
    return { x: svgP.x, y: svgP.y };
}
```

**Możliwe problemy:**
- CTM może być null przy pierwszym renderze → fallback zwraca DOM coords zamiast SVG coords
- Fallback nie uwzględnia transform/zoom SVG
- `fromSvg` i `toSvg` mogą być identyczne jeśli oba używają fallback

---

## Główne problemy do rozwiązania

### Problem 1: `fromSvg` i `toSvg` są identyczne
**Symptomy:**
- Router zwraca path z 2 punktami (start i end są identyczne)
- `uniquePointsCount === 0`
- `rect.width === 0 && rect.height === 0`

**Możliwe przyczyny:**
1. CTM jest null → fallback zwraca DOM coords zamiast SVG coords
2. Anchor elements są w tym samym miejscu (błąd w pozycjonowaniu)
3. `getAnchorSvgPosition()` zwraca błędne coords

**Rozwiązanie:**
- Dodaj retry z `requestAnimationFrame` dla CTM
- Sprawdź czy `fromSvg` i `toSvg` są różne przed routowaniem
- Jeśli są identyczne, użyj minimalnego offsetu

### Problem 2: Router zwraca tylko 2 punkty
**Symptomy:**
- `path.length === 2`
- `simplifyColinear()` usuwa wszystkie środkowe punkty

**Możliwe przyczyny:**
1. `fromSvg` i `toSvg` są identyczne → router zwraca 2 punkty
2. `simplifyColinear()` jest zbyt agresywny
3. `ensureOrthogonalSegments()` nie dodaje środkowych punktów

**Rozwiązanie:**
- Sprawdź czy `fromSvg` i `toSvg` są różne przed routowaniem
- Jeśli są identyczne, wymuś minimalny offset
- Zmniejsz EPS w `simplifyColinear()` lub usuń go całkowicie

### Problem 3: Connection State nie jest aktualizowany
**Symptomy:**
- Connections są pending w modelu (8 connections)
- Ale mają anchors ustawione (10 connections)
- `_recalculateSingleConnection()` jest wywoływane, ale path nie jest aktualizowany

**Możliwe przyczyny:**
1. `notifyUpdate()` nie wywołuje renderera
2. Connection state jest nadpisywany przez `buildConnectionStates()`
3. `_recalculateSingleConnection()` nie zapisuje connection state do modelu

**Rozwiązanie:**
- Sprawdź czy `notifyUpdate()` wywołuje renderer
- Sprawdź czy `buildConnectionStates()` nie nadpisuje zaktualizowanego connection state
- Dodaj logowanie aby zobaczyć czy connection state jest zapisywany

---

## Co ChatGPT ma pomóc naprawić

### 1. **Diagnoza: Dlaczego `fromSvg` i `toSvg` są identyczne?**
- Sprawdź czy CTM jest null przy pierwszym renderze
- Sprawdź czy fallback zwraca poprawne coords
- Dodaj logowanie aby zobaczyć wartości `fromSvg` i `toSvg`
- Sprawdź czy anchor elements są w różnych miejscach

### 2. **Naprawa: Zapewnij że `fromSvg` i `toSvg` są różne**
- Dodaj retry z `requestAnimationFrame` dla CTM
- Sprawdź czy `fromSvg` i `toSvg` są różne przed routowaniem
- Jeśli są identyczne, wymuś minimalny offset (np. 20px)

### 3. **Naprawa: Zapewnij że router zwraca >2 punkty**
- Sprawdź czy router zwraca poprawny path (5 punktów: start, fromOffset, middle, toOffset, end)
- Jeśli `fromSvg` i `toSvg` są identyczne, wymuś minimalny offset przed routowaniem
- Zmniejsz EPS w `simplifyColinear()` lub usuń go całkowicie
- Wymuś minimum 4 punkty po `simplifyColinear()`

### 4. **Naprawa: Zapewnij że connection state jest aktualizowany**
- Sprawdź czy `_recalculateSingleConnection()` zapisuje connection state do modelu
- Sprawdź czy `notifyUpdate()` wywołuje renderer
- Sprawdź czy `buildConnectionStates()` nie nadpisuje zaktualizowanego connection state
- Dodaj logowanie aby zobaczyć czy connection state jest zapisywany

### 5. **Debug: Dodaj szczegółowe logowanie**
- Loguj `fromSvg`, `toSvg`, `path` przed i po routowaniu
- Loguj `anchorElements.keys()` i porównaj z `fromKey`/`toKey`
- Loguj czy connection state jest aktualizowany w modelu
- Loguj czy `notifyUpdate()` jest wywoływane

---

## Pliki do sprawdzenia/zmodyfikowania

1. **`webharvest-ide/frontend/src/visual/CoordinateUtils.ts`**
   - `clientToSvgPoint()` - linia ~47
   - `getAnchorSvgPosition()` - linia ~138
   - **Problem:** CTM może być null → fallback zwraca błędne coords

2. **`webharvest-ide/frontend/src/visual/SimpleConnectionRouter.ts`**
   - `route()` - linia ~162
   - `simplifyColinear()` - linia ~39
   - `ensureOrthogonalSegments()` - linia ~126
   - **Problem:** Router zwraca tylko 2 punkty

3. **`webharvest-ide/frontend/src/visual/VisualSemanticModelManager.ts`**
   - `_recalculateSingleConnection()` - linia ~401
   - `buildConnectionStates()` - linia ~1571
   - `notifyUpdate()` - linia ~694
   - **Problem:** Connection state nie jest aktualizowany

4. **`webharvest-ide/frontend/src/visual/ConnectionPointsRenderer.ts`**
   - `renderPoint()` - linia ~234
   - `registerAnchor()` - wywołanie w linii ~310
   - **Problem:** Anchors mogą być rejestrowane zanim SVG jest gotowy

---

## Oczekiwany rezultat

Po naprawie:
- ✅ Wszystkie connections mają path z >2 punktami
- ✅ `uniquePointsCount > 1` dla wszystkich connections
- ✅ `rect.width > 0 || rect.height > 0` dla wszystkich paths
- ✅ `pendingConnections.size === 0` (wszystkie connections są przeliczony)
- ✅ Linie są widoczne między wszystkimi etykietami

---

## Pytania do ChatGPT

1. **Dlaczego `fromSvg` i `toSvg` są identyczne?** Czy CTM jest null, czy fallback zwraca błędne coords?

2. **Dlaczego router zwraca tylko 2 punkty?** Czy `fromSvg` i `toSvg` są identyczne, czy `simplifyColinear()` jest zbyt agresywny?

3. **Dlaczego connection state nie jest aktualizowany?** Czy `_recalculateSingleConnection()` zapisuje do modelu, czy `buildConnectionStates()` nadpisuje zmiany?

4. **Jak zapewnić że path zawsze ma >2 punkty?** Czy dodać minimalny offset przed routowaniem, czy zmienić logikę routera?

5. **Jak zsynchronizować connection state z anchors?** Czy użyć `notifyUpdate()` po każdej aktualizacji, czy innego mechanizmu?

6. **Jak debugować problem?** Jakie logi dodać, żeby zobaczyć co się dzieje?

---

## Dodatkowe informacje

- Canvas może być przewijany w dowolnym kierunku (scroll support jest zaimplementowany)
- Connection points są renderowane jako HTML divs (32px dla center points)
- SVG overlay ma `z-index: 10` i `position: absolute`
- Connection points container ma `z-index: 1000`
- Wszystkie connection points używają center points (z `-center` w ID)
- Debug logging można włączyć: `window.__DEBUG_CONNECTIONS__ = true;`

---

## WAŻNE

**Wszystkie zmiany muszą być zgodne z modelem semantycznym** - connection state musi być źródłem prawdy, a wszystkie dane (path, anchors, etc.) muszą pochodzić z modelu semantycznego.

**Problem jest krytyczny** - linie między etykietami **NIE SĄ WIDOCZNE**, co uniemożliwia używanie visual designera.

**Potrzebuję konkretnych, działających rozwiązań** - nie teoretycznych, ale praktycznych poprawek kodu, które naprawią problem.

