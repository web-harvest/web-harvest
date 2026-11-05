# API Reference

Quick reference guide for the Design System API.

## ConnectionDesignSystem

### Constructor

```javascript
const app = new ConnectionDesignSystem();
```

Creates a new instance of the design system. Automatically initializes UI and event listeners.

### Methods

#### Node Management

##### `addNode(x?, y?)`

Adds a new node to the canvas.

```javascript
// Add node at random position
app.addNode();

// Add node at specific position
app.addNode(200, 150);
```

**Parameters:**
- `x` (optional): X coordinate in pixels
- `y` (optional): Y coordinate in pixels

**Returns:** `void`

##### `renderNode(node)`

Renders or updates a node's visual representation.

```javascript
const node = app.nodes.get('node-1');
app.renderNode(node);
```

**Parameters:**
- `node`: Node object

**Returns:** `void`

##### `selectNode(nodeId)`

Selects a node visually.

```javascript
app.selectNode('node-1');
app.selectNode(null); // Deselect all
```

**Parameters:**
- `nodeId`: Node ID string or `null`

**Returns:** `void`

#### Connection Management

##### `createConnection(fromNodeId, fromSide, toNodeId, toSide)`

Creates a connection between two nodes with validation.

```javascript
const success = app.createConnection('node-1', 'right', 'node-2', 'left');
if (!success) {
  console.log('Connection creation failed');
}
```

**Parameters:**
- `fromNodeId`: Source node ID
- `fromSide`: Exit side (`'top' | 'bottom' | 'left' | 'right'`)
- `toNodeId`: Target node ID
- `toSide`: Entry side

**Validation:**
- Prevents self-connections (fromNodeId === toNodeId)
- Prevents cycles using DFS algorithm
- Enforces one-to-one rule (one outgoing, one incoming per node)
- Asks user for confirmation if replacing existing connection

**Returns:** `boolean` - `true` if created, `false` if validation failed

##### `renderConnection(conn)`

Renders a single connection with arrow marker.

```javascript
const conn = app.connections.get('conn-1');
app.renderConnection(conn);
```

**Parameters:**
- `conn`: Connection object

**Process:**
1. Gets anchor positions for source and target
2. Routes path using router
3. Verifies and corrects path direction
4. Ensures path ends exactly at target connection point
5. Creates SVG path element
6. Attaches arrow marker (`marker-end`)
7. Creates hit areas for dragging

**Returns:** `void`

##### `ensureMarkerExists()`

Ensures arrow marker exists in SVG defs.

```javascript
const marker = app.ensureMarkerExists();
```

**Returns:** `SVGMarkerElement | null` - Marker element or null if creation failed

**Details:**
- Checks if marker with id `arrowhead` exists
- If not, creates marker with:
  - Size: 12x12 pixels
  - refX: 12 (tip position)
  - refY: 6 (center)
  - Polygon shape pointing right
  - Auto orientation
- Returns existing or newly created marker

##### `wouldCreateCycle(fromNodeId, toNodeId)`

Checks if connection would create a cycle.

```javascript
if (app.wouldCreateCycle('node-1', 'node-3')) {
  console.log('Would create cycle!');
}
```

**Parameters:**
- `fromNodeId`: Source node ID
- `toNodeId`: Target node ID

**Returns:** `boolean` - `true` if cycle would be created

**Algorithm:** Uses DFS to detect cycles in directed graph

##### `selectConnection(connectionId)`

Selects a connection.

```javascript
app.selectConnection('conn-1');
app.selectConnection(null); // Deselect all
```

**Parameters:**
- `connectionId`: Connection ID string or `null`

**Returns:** `void`

#### Configuration

##### `loadConfiguration(config)`

Loads nodes and connections from configuration.

```javascript
const config = {
  nodes: [
    { id: 'node-1', x: 100, y: 100, title: 'HTTP', type: 'http' },
    { id: 'node-2', x: 300, y: 100, title: 'Parse', type: 'parse' }
  ],
  connections: [
    { fromNodeId: 'node-1', fromSide: 'right', toNodeId: 'node-2', toSide: 'left' }
  ]
};

app.loadConfiguration(config);
```

**Parameters:**
- `config`: Configuration object

**Returns:** `void`

##### `clearAll()`

Clears all nodes and connections.

```javascript
app.clearAll();
```

**Returns:** `void`

#### Utility Methods

##### `getAnchorPosition(nodeId, side)`

Gets the SVG position of a connection point.

```javascript
const pos = app.getAnchorPosition('node-1', 'right');
// Returns: { x: 220, y: 130 }
```

**Parameters:**
- `nodeId`: Node ID
- `side`: Side (`'top' | 'bottom' | 'left' | 'right'`)

**Returns:** `Point | null`

##### `updateRouterBounds()`

Updates node bounds in the router for collision detection.

```javascript
app.updateRouterBounds();
```

**Returns:** `void`

##### `renderAll()`

Re-renders all connections and updates routing.

```javascript
app.renderAll();
```

**Returns:** `void`

## SimpleConnectionRouter

### Constructor

```javascript
const router = new SimpleConnectionRouter();
```

### Methods

##### `route(from, to, fromSide, toSide, excludeNodeIds?)`

Routes a path between two points with collision avoidance.

```javascript
const points = router.route(
  { x: 100, y: 100 },
  { x: 300, y: 200 },
  'right',
  'left',
  new Set(['node-1', 'node-2'])
);
// Returns: [{x: 100, y: 100}, {x: 120, y: 100}, {x: 120, y: 200}, {x: 280, y: 200}, {x: 280, y: 200}]
```

**Parameters:**
- `from`: Start point (on source node edge)
- `to`: End point (on target node edge)
- `fromSide`: Exit side (`'top' | 'bottom' | 'left' | 'right'`)
- `toSide`: Entry side
- `excludeNodeIds` (optional): Set of node IDs to exclude from collision detection

**Returns:** `Point[]` - Array of path points
  - First point: `from` (source connection point)
  - Last point: `toOffset` (20px outside target node, NOT `to`)
  - All intermediate points ensure orthogonal routing
  - Includes `bendBeforeArrow` point (15px before arrow)

**Note:** Last point is `toOffset` (outside node), not `to` (on node edge), to ensure proper arrow positioning.

##### `setNodeBounds(nodeId, bounds)`

Sets node bounds for collision detection.

```javascript
router.setNodeBounds('node-1', {
  x: 100,
  y: 100,
  width: 120,
  height: 60
});
```

**Parameters:**
- `nodeId`: Node ID
- `bounds`: NodeBounds object

**Returns:** `void`

##### `clearNodeBounds()`

Clears all node bounds.

```javascript
router.clearNodeBounds();
```

**Returns:** `void`

## CollisionDetector

### Constructor

```javascript
const detector = new CollisionDetector(canvasContainer);
```

**Parameters:**
- `canvasContainer`: DOM element for canvas container

### Methods

##### `checkCollision(node, otherNodes, excludeNodeId?)`

Checks if a node collides with others.

```javascript
const collisions = detector.checkCollision(node, allNodes, 'node-1');
```

**Parameters:**
- `node`: Node to check
- `otherNodes`: Map of all nodes
- `excludeNodeId` (optional): Node ID to exclude

**Returns:** `Collision[]` - Array of collision objects

##### `findFreePosition(node, otherNodes, excludeNodeId?)`

Finds nearest free position for a node.

```javascript
const freePos = detector.findFreePosition(node, allNodes, 'node-1');
```

**Parameters:**
- `node`: Node to position
- `otherNodes`: Map of all nodes
- `excludeNodeId` (optional): Node ID to exclude

**Returns:** `Point` - Free position

##### `resolveCollisions(node, allNodes, excludeNodeId?, visited?)`

Resolves all collisions for a node recursively.

```javascript
detector.resolveCollisions(node, allNodes, 'node-1');
```

**Parameters:**
- `node`: Node to resolve
- `allNodes`: Map of all nodes
- `excludeNodeId` (optional): Node ID to exclude
- `visited` (optional): Set of visited node IDs

**Returns:** `void`

##### `resolveAllCollisions(allNodes)`

Resolves all collisions in the system.

```javascript
detector.resolveAllCollisions(app.nodes);
```

**Parameters:**
- `allNodes`: Map of all nodes

**Returns:** `void`

##### `animateNodeMove(node, targetX, targetY, callback?)`

Animates node movement.

```javascript
detector.animateNodeMove(node, 200, 150, () => {
  console.log('Animation complete');
});
```

**Parameters:**
- `node`: Node to move
- `targetX`: Target X coordinate
- `targetY`: Target Y coordinate
- `callback` (optional): Function to call after animation

**Returns:** `void`

## Properties

### ConnectionDesignSystem

- `nodes: Map<string, Node>` - All nodes
- `connections: Map<string, Connection>` - All connections
- `router: SimpleConnectionRouter` - Router instance
- `collisionDetector: CollisionDetector` - Collision detector instance
- `autoResolveCollisions: boolean` - Enable/disable auto collision resolution
- `showAnchors: boolean` - Show/hide connection points
- `showDebug: boolean` - Show/hide debug overlay

### SimpleConnectionRouter

- `OFFSET: number` - Distance from node edge (default: 20)
- `ARROW_BEND_OFFSET: number` - Distance before arrow where bend occurs (default: 15)
- `EPS: number` - Epsilon for comparisons (default: 1e-6)
- `MIN_SEPARATION_PX: number` - Minimum separation (default: 1)
- `MIN_FORCED_SEPARATION: number` - Forced separation (default: 20)
- `nodeBounds: Map<string, NodeBounds>` - Node bounds cache
- `existingPaths: Map<string, Point[]>` - Existing connection paths for intersection detection

### CollisionDetector

- `MARGIN: number` - Minimum distance between nodes (default: 20)
- `ANIMATION_DURATION: number` - Animation duration in ms (default: 300)
- `STEP_SIZE: number` - Search step size (default: 10)

## Events

The system uses standard DOM events. Key events:

- `mousedown` on node - Start dragging
- `mousemove` on canvas - Update drag position
- `mouseup` on canvas - End dragging
- `click` on connection - Select connection
- `mousedown` on anchor - Start connection creation
- `mousedown` on connection end - Start connection modification

## Examples

### Basic Usage

```javascript
// Create app instance
const app = new ConnectionDesignSystem();

// Add nodes
app.addNode(100, 100);
app.addNode(300, 100);

// Create connection
const nodeIds = Array.from(app.nodes.keys());
app.createConnection(nodeIds[0], 'right', nodeIds[1], 'left');
```

### Load Configuration

```javascript
const config = {
  nodes: [
    { id: 'start', x: 50, y: 50, title: 'Start', type: 'start' },
    { id: 'process', x: 200, y: 50, title: 'Process', type: 'process' },
    { id: 'end', x: 350, y: 50, title: 'End', type: 'end' }
  ],
  connections: [
    { fromNodeId: 'start', fromSide: 'right', toNodeId: 'process', toSide: 'left' },
    { fromNodeId: 'process', fromSide: 'right', toNodeId: 'end', toSide: 'left' }
  ]
};

app.loadConfiguration(config);
```

### Manual Collision Resolution

```javascript
// Disable auto resolution
app.autoResolveCollisions = false;

// Add nodes
app.addNode(100, 100);
app.addNode(120, 110); // Overlapping

// Manually resolve
app.collisionDetector.resolveAllCollisions(app.nodes);
```

### Custom Router Configuration

```javascript
// Adjust router parameters
app.router.OFFSET = 30; // Larger offset
app.router.MIN_FORCED_SEPARATION = 40; // Larger separation

// Update bounds
app.updateRouterBounds();
```

### Custom Collision Detection

```javascript
// Adjust collision parameters
app.collisionDetector.MARGIN = 30; // Larger margin
app.collisionDetector.ANIMATION_DURATION = 500; // Slower animation

// Check specific collision
const node = app.nodes.get('node-1');
const collisions = app.collisionDetector.checkCollision(node, app.nodes, 'node-1');
console.log(`Found ${collisions.length} collisions`);
```

