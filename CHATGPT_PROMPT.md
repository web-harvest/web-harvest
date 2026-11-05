# Prompt dla ChatGPT - Poprawa implementacji renderowania linii połączeń

## Kontekst projektu

Pracuję nad edytorem wizualnym workflow (inspirowany NiFi/Conductor) w TypeScript/React. System renderuje węzły jako HTML divy i linie połączeń jako SVG paths. Problem polega na tym, że linie połączeń nie są renderowane w prawidłowych pozycjach - pojawiają się w "losowych miejscach" zamiast łączyć środkowe punkty węzłów.

## Architektura systemu

1. **ConnectionPointsRenderer** - Renderuje HTML divy reprezentujące punkty połączeń (anchors) na krawędziach węzłów
2. **VisualSemanticModelManager** - Zarządza modelem semantycznym, oblicza ścieżki routingu, konwertuje współrzędne
3. **SimpleConnectionRenderer** - Renderuje SVG paths reprezentujące linie połączeń
4. **CoordinateUtils** - Narzędzia do konwersji współrzędnych HTML → SVG

## Problem

Linie połączeń są renderowane w nieprawidłowych pozycjach. Analiza wykazała następujące problemy:

1. **Race conditions**: Model semantyczny jest budowany zanim anchor elements (HTML divy) są zarejestrowane w DOM
2. **Niespójność przestrzeni współrzędnych**: Anchors są w przestrzeni HTML (client coordinates), a SVG paths w przestrzeni SVG (user units)
3. **Cache invalidation**: Połączenia są oznaczane jako "pending" gdy anchor elements nie istnieją, ale nie są automatycznie przeliczane gdy anchors się pojawiają
4. **Brak synchronizacji**: `buildConnectionStates()` jest wywoływane przed `renderAll()` w ConnectionPointsRenderer

## Wprowadzone zmiany

### 1. Rejestracja anchor elements (VisualSemanticModelManager.ts)

**Dodano:**
- `anchorElements: Map<string, HTMLElement>` - przechowuje referencje do HTML anchor elements
- `registerAnchor(nodeId, pointId, element)` - rejestruje anchor element SYNCHRONICZNIE po dodaniu do DOM
- `unregisterAnchor(nodeId, pointId)` - usuwa anchor element
- `getAnchorElement(nodeId, pointId)` - pobiera anchor element z mapy

**Implementacja:**
```typescript
registerAnchor(nodeId: string, pointId: string, element: HTMLElement): void {
    const key = `${nodeId}/${pointId}`;
    this.anchorElements.set(key, element);
    // CRITICAL: Prefer synchronous recalculation to avoid race
    this.recalculatePendingConnectionsForNodeSync(nodeId);
    // CRITICAL: Add ResizeObserver to detect layout changes
    this.setupResizeObserverForAnchor(nodeId, pointId, element);
}
```

### 2. Konwersja współrzędnych HTML → SVG (CoordinateUtils.ts)

**Dodano:**
- `getAnchorSvgPosition(anchorDiv, svgElement)` - konwertuje środek HTML diva do współrzędnych SVG używając `getBoundingClientRect()` i `getScreenCTM().inverse()`
- `clientToSvgPoint()` - bardziej robustna konwersja używająca `createSVGPoint()` i `matrixTransform()`
- `snapToPixel()` - zaokrąglanie do pikseli z uwzględnieniem `devicePixelRatio` i `strokeWidth`

**Implementacja:**
```typescript
export function getAnchorSvgPosition(anchorEl: HTMLElement, svgElement: SVGSVGElement, strokeWidth: number = 2.5): Point {
    const rect = anchorEl.getBoundingClientRect();
    const clientX = rect.left + rect.width / 2;
    const clientY = rect.top + rect.height / 2;
    const svgP = clientToSvgPoint(svgElement, clientX, clientY);
    return snapPointToPixel(svgP, strokeWidth);
}
```

### 3. Rekalkulacja pojedynczych połączeń (VisualSemanticModelManager.ts)

**Dodano:**
- `_recalculateSingleConnection(connId)` - pełna implementacja rekalkulacji pojedynczego połączenia
- Metoda pobiera anchor elements z mapy, konwertuje współrzędne do SVG, wywołuje router, aktualizuje stan połączenia w modelu

**Implementacja:**
```typescript
private _recalculateSingleConnection(connId: string): void {
    // Pobierz connection, nodes, points z modelu
    // Spróbuj użyć anchor DOM elements do konwersji HTML → SVG
    // Jeśli anchors nie istnieją, użyj model coords jako fallback
    // Wywołaj router z SVG coords
    // Zaktualizuj connection state w modelu
}
```

### 4. Rekalkulacja wszystkich pending connections (VisualSemanticModelManager.ts)

**Dodano:**
- `recalculateAllPendingConnectionsSync()` - publiczna metoda do rekalkulacji wszystkich pending connections
- `pendingConnections: Set<string>` - przechowuje ID połączeń oczekujących na rekalkulację
- `recalculateConnectionsForSetSync(connIds)` - rekalkuluje zestaw połączeń synchronicznie

**Implementacja:**
```typescript
recalculateAllPendingConnectionsSync(): void {
    if (!this.svgElement) return;
    const pending = Array.from(this.pendingConnections);
    this.recalculateConnectionsForSetSync(pending);
}
```

### 5. Wywołanie rekalkulacji po renderAll() (ConnectionPointsRenderer.ts)

**Dodano:**
- Po zakończeniu `renderAll()`, wywołanie `recalculateAllPendingConnectionsSync()` w `requestAnimationFrame` aby upewnić się, że DOM jest zaktualizowany

**Implementacja:**
```typescript
// W ConnectionPointsRenderer.renderAll(), po renderowaniu wszystkich punktów:
if (this.modelManager && totalRendered > 0) {
    requestAnimationFrame(() => {
        if ((this.modelManager as any).recalculateAllPendingConnectionsSync) {
            (this.modelManager as any).recalculateAllPendingConnectionsSync();
        }
    });
}
```

### 6. Incremental cache invalidation (VisualSemanticModelManager.ts)

**Dodano:**
- `nodeVersions: Map<string, number>` - wersjonowanie węzłów do śledzenia zmian
- `connectionsByNode: Map<string, Set<string>>` - mapowanie węzłów do połączeń dla efektywnej invalidacji
- `dirtyConnections: Set<string>` - połączenia wymagające rekalkulacji
- `invalidateCacheForNode(nodeId)` - oznacza połączenia jako dirty
- `nodeMoved(nodeId, newPosition)` - wywoływane podczas drag węzła

### 7. Rejestracja SVG element (VisualDesigner.ts)

**Dodano:**
- `registerSvgElement(svgElement)` - rejestruje główny SVG element w model manager
- `unregisterSvgElement()` - usuwa rejestrację

**Implementacja:**
```typescript
// W VisualDesigner.render():
if (!this.svgElement) {
    this.svgElement = this.createSVGOverlay();
    this.canvasElement.appendChild(this.svgElement);
    if ((this as any).modelManager) {
        (this as any).modelManager.registerSvgElement(this.svgElement);
    }
}
```

## Obecny stan

Po wprowadzeniu zmian:
- ✅ Kompilacja TypeScript przechodzi bez błędów
- ✅ Build Maven zakończony sukcesem
- ✅ IDE uruchomione i odpowiada
- ⚠️ Częściowo działa - niektóre połączenia są nadal oznaczone jako pending (`fromAnchorExists: false, toAnchorExists: false`)
- ⚠️ Niektóre połączenia używają "non-center points" zamiast center points

## Pytania i prośba o radę

1. **Czy podejście z synchroniczną rejestracją anchor elements jest poprawne?** Czy powinienem użyć `MutationObserver` lub `ResizeObserver` zamiast bezpośredniej rejestracji?

2. **Czy kolejność wywołań jest optymalna?** Obecnie:
   - `buildModel()` → `buildConnectionStates()` (może oznaczyć jako pending)
   - `renderAll()` → `registerAnchor()` (dla każdego anchor)
   - `renderAll()` kończy → `recalculateAllPendingConnectionsSync()`
   
   Czy powinienem zmienić kolejność lub dodać mechanizm oczekiwania na gotowość anchors?

3. **Czy konwersja HTML → SVG jest wystarczająco robustna?** Używam `getBoundingClientRect()` + `getScreenCTM().inverse()`. Czy powinienem uwzględnić dodatkowe transformacje CSS, zoom, scroll?

4. **Czy cache invalidation jest efektywna?** Obecnie używam `nodeVersions`, `dirtyConnections`, `connectionsByNode`. Czy to wystarczające czy powinienem dodać bardziej zaawansowany system?

5. **Czy "pending connections" to dobre podejście?** Czy powinienem zamiast tego użyć `requestAnimationFrame` z retry logic lub innego mechanizmu?

6. **Czy powinienem przenieść anchor elements do SVG?** Zamiast HTML divów, użyć `<circle>` lub `<foreignObject>` w SVG - wtedy nie byłoby potrzeby konwersji współrzędnych.

7. **Jak najlepiej obsłużyć edge cases?**
   - SVG element nie jest jeszcze w DOM
   - Anchor elements są renderowane asynchronicznie
   - Węzły są przesuwane podczas gdy anchors są rejestrowane
   - Zoom/pan zmienia współrzędne

8. **Czy powinienem użyć biblioteki do routingu?** Obecnie używam prostego `SimpleConnectionRouter`. Czy powinienem rozważyć bardziej zaawansowany router (np. grid-based A*, visibility graph)?

## Kod źródłowy

Kluczowe pliki:
- `webharvest-ide/frontend/src/visual/VisualSemanticModelManager.ts` - zarządzanie modelem, rekalkulacja
- `webharvest-ide/frontend/src/visual/ConnectionPointsRenderer.ts` - renderowanie anchor points
- `webharvest-ide/frontend/src/visual/CoordinateUtils.ts` - konwersja współrzędnych
- `webharvest-ide/frontend/src/visual/SimpleConnectionRenderer.ts` - renderowanie SVG paths
- `webharvest-ide/frontend/src/visual/VisualDesigner.ts` - główny komponent

## Prośba

Proszę o:
1. **Krytyczną analizę** wprowadzonych zmian - czy są poprawne i kompletne?
2. **Sugestie poprawy** - co można zrobić lepiej?
3. **Odpowiedzi na pytania** - szczególnie dotyczące kolejności wywołań i obsługi edge cases
4. **Alternatywne podejścia** - czy są lepsze rozwiązania architektoniczne?
5. **Best practices** - jak profesjonalne edytory wizualne (draw.io, NiFi, Conductor) rozwiązują te problemy?

Dziękuję za pomoc!

