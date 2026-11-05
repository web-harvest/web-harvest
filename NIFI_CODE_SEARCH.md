# Apache NiFi - Wyszukiwanie kodu renderowania połączeń

## Lokalizacja kodu frontendowego:

### Struktura repozytorium:
```
apache/nifi/
└── nifi-nar-bundles/
    └── nifi-framework-bundle/
        └── nifi-framework/
            └── nifi-web/
                └── nifi-web-ui/
                    └── src/
                        └── main/
                            └── webapp/
                                ├── js/
                                │   ├── canvas/
                                │   ├── nf-canvas/
                                │   ├── nf-connection/
                                │   └── nf-graph/
                                └── css/
```

## Pliki do sprawdzenia:

### 1. Canvas i Connection Rendering:
- `js/canvas/canvas-controller.js` - główny kontroler canvas
- `js/canvas/connection-renderer.js` - renderowanie połączeń
- `js/canvas/connection-router.js` - algorytm routingu
- `js/nf-canvas/nf-canvas.js` - komponent canvas
- `js/nf-connection/nf-connection.js` - komponent połączenia

### 2. Graph/Diagram Libraries:
- Sprawdź czy używa mxGraph, JointJS, Dagre, D3.js
- Pliki konfiguracyjne mogą wskazać używaną bibliotekę

### 3. Connection Routing:
- Szukaj funkcji związanych z:
  - `orthogonal`
  - `bezier`
  - `path`
  - `route`
  - `connectionPoint`
  - `anchor`

## Linki do bezpośredniego przeszukania:

1. **GitHub Search:**
   - https://github.com/apache/nifi/search?q=connection+render
   - https://github.com/apache/nifi/search?q=canvas+path
   - https://github.com/apache/nifi/search?q=orthogonal+routing

2. **Konkretne pliki (jeśli istnieją):**
   - https://github.com/apache/nifi/tree/main/nifi-nar-bundles/nifi-framework-bundle/nifi-framework/nifi-web/nifi-web-ui/src/main/webapp/js

3. **Connection-related Java classes:**
   - `ConnectionDTO.java`
   - `ConnectionEntityMerger.java`
   - `DtoFactory.java`

## Co szukać w kodzie:

### Connection Rendering:
```javascript
// Przykładowe funkcje do znalezienia:
function renderConnection(connection) { ... }
function calculateConnectionPath(from, to) { ... }
function routeOrthogonal(start, end) { ... }
function getConnectionPoints(node) { ... }
```

### Coordinate Conversion:
```javascript
// Konwersja współrzędnych:
function clientToSvg(x, y) { ... }
function svgToClient(x, y) { ... }
function getScreenCTM() { ... }
```

### Path Generation:
```javascript
// Generowanie ścieżek:
function createPath(points) { ... }
function createBezierPath(start, end) { ... }
function createOrthogonalPath(start, end) { ... }
```

## Następne kroki:

1. Sklonuj repozytorium Apache NiFi
2. Przeszukaj pliki JavaScript w `nifi-web-ui`
3. Znajdź kod renderowania połączeń
4. Przeanalizuj algorytm routingu
5. Porównaj z obecną implementacją w WebHarvest IDE




