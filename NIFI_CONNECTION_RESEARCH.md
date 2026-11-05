# Research: Apache NiFi Connection Rendering Implementation

## Repozytoria do sprawdzenia:

### 1. **apache/nifi** (główne repozytorium)
- **Frontend UI:** `nifi-web-ui` lub `nifi-ui`
- **Canvas rendering:** Prawdopodobnie używa biblioteki do grafów (mxGraph, JointJS, Dagre, lub własna implementacja)
- **Connection routing:** Algorytm routingu połączeń (orthogonal, bezier, straight)

### 2. **apache/nifi-fds** (Flow Design System)
- **Status:** Tylko system designu UI/UX (komponenty Angular, theming)
- **Zawartość:** README.md nie zawiera informacji o implementacji połączeń
- **Wnioski:** To nie jest właściwe miejsce do szukania implementacji routingu

## Prawdopodobne lokalizacje kodu w Apache NiFi:

### Frontend (JavaScript/TypeScript):
```
nifi-web-ui/src/main/webapp/js/
├── canvas/
│   ├── connection-renderer.js
│   ├── connection-router.js
│   └── canvas-controller.js
├── nf-canvas/
│   └── nf-canvas.js
└── nf-connection/
    └── nf-connection.js
```

### Możliwe biblioteki używane przez NiFi:
1. **mxGraph** (draw.io) - popularna biblioteka do grafów z orthogonal routing
2. **JointJS** - inna popularna biblioteka do diagramów
3. **Dagre** - layout engine dla grafów skierowanych
4. **Własna implementacja** - custom routing algorithm

## Co szukać w kodzie NiFi:

### 1. Connection Routing Algorithm
- Orthogonal routing (L-shaped, U-shaped paths)
- Bezier curves dla smooth connections
- Collision avoidance (unikanie przecięć z nodes)
- Path optimization (minimalizacja długości ścieżki)

### 2. Coordinate System
- Konwersja między coordinate spaces (DOM ↔ SVG ↔ Canvas)
- Transform matrices (CTM - Current Transformation Matrix)
- Zoom/pan handling
- Scroll position handling

### 3. Connection Points (Ports/Anchors)
- Definicja punktów połączenia na krawędziach nodes
- Dynamiczne pozycjonowanie portów
- Port selection logic (który port użyć dla connection)

### 4. Rendering Pipeline
- SVG path generation
- Arrow rendering
- Animation/transitions
- Performance optimization (virtualization, batching)

## Inspiracje z innych projektów:

### draw.io (mxGraph)
- **Orthogonal routing:** `mxGraphLayout` z `mxOrthogonalLayout`
- **Connection points:** `mxConnectionConstraint` z `mxConnectionHandler`
- **Path calculation:** `mxEdgeStyle` z różnymi stylami (orthogonal, elbow, etc.)

### JointJS
- **Connection routing:** `connectionStrategy` z `joint.connectors.rounded`
- **Orthogonal routing:** `joint.connectors.orthogonal`
- **Ports:** `joint.shapes.basic.Ports` z dynamic positioning

### Dagre
- **Layout algorithm:** Automatyczne pozycjonowanie nodes
- **Edge routing:** Automatyczne routing edges między nodes

## Rekomendacje dla WebHarvest IDE:

### Obecna implementacja (SimpleConnectionRouter):
- ✅ Orthogonal routing (L-shaped paths)
- ✅ Minimalny offset dla zbyt bliskich punktów
- ✅ Wymuszenie min 4 punktów
- ⚠️ Brak collision avoidance
- ⚠️ Brak U-shaped routing dla złożonych przypadków

### Możliwe ulepszenia (inspirowane NiFi):
1. **U-shaped routing** - gdy L-shaped nie wystarcza
2. **Collision detection** - unikanie przecięć z nodes
3. **Path optimization** - wybór najkrótszej ścieżki
4. **Smooth corners** - zaokrąglone narożniki (już mamy `pathWithRoundedCorners`)
5. **Dynamic port selection** - automatyczny wybór najlepszego portu

## Linki do sprawdzenia:

1. **Apache NiFi GitHub:**
   - https://github.com/apache/nifi
   - https://github.com/apache/nifi/tree/main/nifi-web-ui

2. **mxGraph (draw.io):**
   - https://github.com/jgraph/mxgraph
   - https://github.com/jgraph/drawio

3. **JointJS:**
   - https://github.com/clientIO/joint

4. **Dagre:**
   - https://github.com/dagrejs/dagre

## Następne kroki:

1. Sprawdź kod źródłowy Apache NiFi w repozytorium `apache/nifi`
2. Znajdź pliki związane z canvas rendering i connection routing
3. Przeanalizuj algorytm routingu (orthogonal, bezier, etc.)
4. Zidentyfikuj używaną bibliotekę (jeśli jest) lub własną implementację
5. Porównaj z obecną implementacją w WebHarvest IDE
6. Zaimplementuj ulepszenia inspirowane NiFi




