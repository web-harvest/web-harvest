# Layout Debugger - Instrukcja użycia

## Włączanie debugowania layoutu

W konsoli przeglądarki:

```javascript
// Pobierz instancję VisualDesigner
const designer = window.app?.getVisualDesigner?.();

// Włącz debugowanie
designer?.setLayoutDebugEnabled(true);

// Wyłącz debugowanie
designer?.setLayoutDebugEnabled(false);

// Pobierz raport
const report = designer?.getLayoutDebugReport();
console.log(report);
```

## Co pokazuje debugger

1. **Bounding boxy węzłów** - niebieskie (root) / zielone (nested) przerywane linie
2. **Pozycje węzłów** - kropki na pozycji (x, y)
3. **Połączenia** - czerwone przerywane linie
4. **Etykiety** - pomarańczowe przerywane linie
5. **Kolizje** - kolorowe prostokąty:
   - Czerwony: critical (>50% overlap)
   - Pomarańczowy: major (>20% overlap)
   - Żółty: minor (<20% overlap)

## Testy

Uruchom testy:

```bash
npm test -- LayoutDebugger.test.ts
```

## Metryki

Debugger analizuje:
- Pozycje wszystkich węzłów
- Wymiary węzłów
- Pozycje etykiet
- Połączenia między węzłami
- Kolizje między elementami
- Całkowite bounds layoutu











