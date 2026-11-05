# Design System - Connection Routing Mockup - Technical Documentation

## Overview

This is a simplified canvas mockup for testing connection routing systems between labels (nodes). The system provides interactive node management, automatic collision detection and resolution, and orthogonal connection routing with visual feedback.

## Architecture

The system consists of four main JavaScript modules:

1. **`app.js`** - Main application logic and UI orchestration
2. **`connection-router.js`** - Orthogonal path routing algorithm
3. **`collision-detector.js`** - Collision detection and automatic node separation
4. **`index.html`** + **`styles.css`** - UI and styling

## Core Components

### 1. ConnectionDesignSystem (app.js)

Main application class that orchestrates all functionality.

#### Properties

- `nodes: Map<string, Node>` - Stores all nodes by ID
- `connections: Map<string, Connection>` - Stores all connections by ID
- `router: SimpleConnectionRouter` - Instance of connection router
- `collisionDetector: CollisionDetector` - Instance of collision detector
- `autoResolveCollisions: boolean` - Enable/disable automatic collision resolution
- `showAnchors: boolean` - Toggle connection point visibility
- `showDebug: boolean` - Toggle debug overlay

#### Key Methods

##### `addNode(x?: number, y?: number): void`

Adds a new node to the canvas.

- If `x` and `y` are not provided, generates random position near center
- Automatically checks for collisions and resolves them
- Renders the node and updates routing

**Flow:**
1. Creates node object with default dimensions (120x60px)
2. Adds to `nodes` Map
3. Renders node element
4. Checks collisions immediately
5. If collisions found, finds free position and sets it
6. Animates final position adjustment via `requestAnimationFrame`

##### `renderNode(node: Node): void`

Renders or updates a node's visual representation.

- Creates DOM element if doesn't exist
- Sets position (respects ongoing animations)
- Renders connection points if `showAnchors` is enabled
- Attaches event listeners for dragging

##### `onMouseMove(e: MouseEvent): void`

Handles mouse movement for dragging operations.

**Node Dragging:**
- Calculates new position from mouse coordinates
- If `autoResolveCollisions` is enabled:
  - Checks collisions with test position
  - If collision detected, finds nearest free position
  - Updates node position to free position
- Renders node and updates connections

**Connection Dragging:**
- Renders temporary connection line during drag
- Updates visual feedback

##### `onMouseUp(e: MouseEvent): void`

Handles mouse release events.

**Node Dragging End:**
- Checks for final collisions
- If collisions exist, animates node to free position
- Updates routing and connections

**Connection Creation:**
- Checks if dropped on valid anchor point
- Creates connection if valid
- Updates connection rendering

##### `createConnection(fromNodeId: string, fromSide: Side, toNodeId: string, toSide: Side): boolean`

Creates a new connection between two nodes with validation.

**Validation and Optimization Steps:**
1. **Self-connection check:** Prevents node from connecting to itself
   - Returns `false` if `fromNodeId === toNodeId`
   - Logs warning to console
2. **Cycle detection:** Checks if connection would create a cycle in the graph
   - Calls `wouldCreateCycle(fromNodeId, toNodeId)`
   - Uses DFS (Depth-First Search) to detect cycles
   - If cycle detected:
     - Shows alert: "Nie można utworzyć połączenia - utworzyłoby to pętlę w łańcuchu"
     - Returns `false`
3. **Automatic anchor remapping:**
   - Checks if connection is reversed 180 degrees using `isConnectionReversed180()`
   - If reversed 180 degrees:
     - Calls `autoSelectBestAnchor()` to find optimal target anchor point
     - Calls `autoSelectBestSourceAnchor()` to find optimal source anchor point
     - Updates both `finalFromSide` and `finalToSide` with auto-selected sides
   - Otherwise:
     - Calls `autoSelectBestAnchor()` to find optimal target anchor point only
     - Updates `finalToSide` with auto-selected side
4. **Non-intersecting route search:**
   - Calls `findNonIntersectingRoute()` to find best route without intersections
   - Tries all 16 anchor point combinations (4 source × 4 target)
   - Selects route with no connection collisions (preferred) or shortest path
   - Updates both `finalFromSide` and `finalToSide` if better anchor points found
5. **Connection collision resolution:**
   - If route still has connection collisions, attempts to resolve by moving nodes
   - Calls `resolveConnectionCollisionsByMovingNodes()` as last resort
   - Moves target node in direction opposite to connection to create space
6. **One-to-one rule enforcement:**
   - **Outgoing connections:** Each node can have only ONE outgoing connection (from any side)
     - Finds all existing connections from `fromNodeId`
     - If found, asks user for confirmation to replace
     - If user cancels, returns `false`
   - **Incoming connections:** Each node can have only ONE incoming connection (to any side)
     - Finds all existing connections to `toNodeId`
     - If found, asks user for confirmation to replace
     - If user cancels, returns `false`
4. **Remove old connections:** Deletes replaced connections
   - Removes from `connections` Map
   - Removes from router's `existingPaths` for collision detection
5. **Create new connection:**
   - Generates unique ID: `conn-${fromNodeId}-${toNodeId}-${counter}`
   - Creates connection object with all properties
   - Stores in `connections` Map
6. **Render:** Schedules rendering in next animation frame
   - Uses `requestAnimationFrame` to ensure DOM is ready
   - Calls `renderAll()` to update all connections
   - Updates info panel

**Returns:** `true` if connection created, `false` if validation failed

##### `autoSelectBestAnchor(fromNodeId: string, fromSide: Side, toNodeId: string): Side | null`

Automatically selects the best anchor point for target node based on source position.

**Algorithm:**
1. Gets source anchor position
2. Calculates target node center
3. Computes distances from source to all 4 sides of target node
4. Selects side with minimum distance
5. If connecting from opposite side, adjusts selection to avoid opposite connection
6. Returns best side

**Returns:** Best anchor side or `null` if nodes not found

##### `autoSelectBestSourceAnchor(fromNodeId: string, toNodeId: string, toSide: Side): Side | null`

Automatically selects the best anchor point for source node based on target position.

**Algorithm:**
1. Gets target anchor position
2. Calculates source node center
3. Computes distances from all 4 sides of source node to target
4. Selects side with minimum distance
5. Adjusts selection based on node positions to avoid awkward routing
6. Returns best side

**Returns:** Best anchor side or `null` if nodes not found

##### `isConnectionReversed180(fromNodeId: string, fromSide: Side, toNodeId: string, toSide: Side): boolean`

Checks if connection is reversed by 180 degrees (source and target points are on opposite sides).

**Algorithm:**
1. Gets source and target anchor positions
2. Calculates node centers
3. Computes direction vector (dx, dy)
4. Checks if points are on opposite sides (e.g., left-right, top-bottom)
5. Calculates angle difference to verify 180-degree reversal
6. Returns `true` if reversed by approximately 180 degrees (within 45° tolerance)

**Returns:** `true` if connection is reversed 180 degrees, `false` otherwise

##### `findNonIntersectingRoute(fromNodeId: string, fromSide: Side, toNodeId: string, toSide: Side): RouteResult | null`

Finds routing path without intersections by trying all anchor point combinations for both source and target.

**Algorithm:**
1. Tries all 16 combinations (4 source sides × 4 target sides)
2. For each combination:
   - Routes path using router
   - Checks node collisions
   - Checks connection collisions
   - Calculates score (prefers no collisions, then shortest path)
   - Applies penalty for 180-degree reversal (smaller than collision penalty)
3. Selects route with best score
4. Returns route result with selected anchor points and path

**Returns:** Route result object or `null` if no valid route found

**RouteResult:**
```typescript
{
  fromSide: Side;         // Selected source anchor point
  toSide: Side;           // Selected target anchor point
  points: Point[];        // Path points
  hasNodeCollision: boolean;
  hasConnectionCollision: boolean;
}
```

**Scoring:**
- Base score: Manhattan path length
- Node collision: +10000 penalty
- Connection collision: +5000 penalty
- 180-degree reversal: +1000 penalty
- Original points bonus: -200 (if both original and no collisions)
- Original single point bonus: -100 (if one original and no collisions)

##### `resolveConnectionCollisionsByMovingNodes(fromNodeId: string, toNodeId: string): boolean`

Attempts to resolve connection collisions by moving nodes.

**Algorithm:**
1. Calculates connection direction (dx, dy)
2. Moves target node in direction opposite to connection (50px offset)
3. Checks for node collisions with moved node
4. Resolves collisions using collision detector
5. Renders node in new position

**Returns:** `true` if nodes were moved, `false` otherwise

**One-to-One Rule:**
- Each node can have maximum **one outgoing connection** (regardless of which side)
- Each node can have maximum **one incoming connection** (regardless of which side)
- This ensures linear flow without branching or merging

##### `renderConnection(conn: Connection): void`

Renders a single connection as SVG path with arrow marker.

**Detailed Process:**
1. **Get anchor positions:** Retrieves exact connection point positions using `getAnchorPosition()`
   - Positions are on the node edge (center of each side)
   - Coordinates are in SVG coordinate space relative to canvas container
2. **Route calculation:** Calls `router.route()` to generate orthogonal path
   - Router returns array of points from source to target
   - Points include offsets outside nodes (20px from edges)
3. **Path direction verification:** Checks if path goes from source to target
   - Compares first point distance to `fromPos` vs `toPos`
   - If reversed (first point closer to target), reverses entire path
   - Rebuilds path data with corrected direction
4. **Path data generation:** Creates SVG path data string
   - Uses `M` (move) for first point
   - Uses `H` (horizontal) for same Y coordinates
   - Uses `V` (vertical) for same X coordinates
   - Uses `L` (line) for diagonal segments (shouldn't occur after orthogonal enforcement)
   - Skips duplicate points to avoid redundant commands
5. **Critical fix - End point correction:** Ensures path ends exactly at connection point
   - Checks if last point matches `toPos` (within 0.1px tolerance)
   - If not, replaces last point with exact `toPos` coordinates
   - Rebuilds path data to ensure arrow touches connection point
6. **SVG path creation:** Creates `<path>` element with attributes
   - Sets `d` attribute with path data
   - Sets stroke color (`#64748b`), width (2.5px), and style
   - Adds `stroke-linecap: round` and `stroke-linejoin: round` for smooth rendering
7. **Arrow marker attachment:**
   - Ensures marker exists via `ensureMarkerExists()`
   - Sets `marker-end` attribute to `url(#arrowhead)`
   - Removes `marker-start` if present (only end marker used)
   - Marker tip (refX=12) touches the connection point exactly
8. **Hit areas:** Creates invisible circles at path ends for dragging
9. **Event handlers:** Attaches click handler for selection
10. **Append to SVG:** Adds path to SVG element for rendering

**Arrow Marker Details:**
- **Size:** 12x12 pixels (markerWidth/markerHeight)
- **Reference point:** `refX=12, refY=6` - tip of arrow is at x=12 in viewBox
- **ViewBox:** `0 0 12 12` - defines coordinate system
- **Shape:** Polygon with points `0 0, 12 6, 0 12` (base-left, tip-right, base-right)
- **Orientation:** `orient="auto"` - automatically rotates to match path direction
- **Positioning:** With `refX=12`, the arrow tip (at x=12) is positioned at the path end
- **Color:** `#64748b` (slate gray) matching connection line color
- **Units:** `markerUnits="userSpaceOnUse"` - marker size in user units, not stroke width

##### `selectConnection(connectionId: string): void`

Selects a connection and highlights it.

- Resets all connections to default style
- Highlights selected connection in blue
- Updates info panel

##### `loadConfiguration(config: Config): void`

Loads nodes and connections from configuration object.

**Config Structure:**
```typescript
interface Config {
  nodes: Array<{
    id: string;
    x: number;
    y: number;
    width?: number;
    height?: number;
    title: string;
    type: string;
  }>;
  connections?: Array<{
    fromNodeId: string;
    fromSide: Side;
    toNodeId: string;
    toSide: Side;
  }>;
}
```

**Flow:**
1. Clears existing canvas
2. Creates all nodes from config
3. Resolves all collisions with animation
4. Creates connections after delay

### 2. SimpleConnectionRouter (connection-router.js)

Handles orthogonal path routing between connection points.

#### Properties

- `OFFSET: number = 20` - Distance from node edge to start routing
- `EPS: number = 1e-6` - Epsilon for floating point comparisons
- `MIN_SEPARATION_PX: number = 1` - Minimum distance considered "too close"
- `MIN_FORCED_SEPARATION: number = 20` - Forced offset for identical points
- `nodeBounds: Map<string, NodeBounds>` - Stores node bounding boxes for collision detection

#### Key Methods

##### `route(from: Point, to: Point, fromSide: Side, toSide: Side, excludeNodeIds?: Set<string>): Point[]`

Main routing method that generates orthogonal path.

**Detailed Algorithm:**
1. **Calculate offsets:** Computes points outside nodes
   - `fromOffset`: Point 20px outside source node (based on `fromSide`)
   - `toOffset`: Point 20px outside target node (based on `toSide`)
   - Uses `offsetPoint()` helper to calculate based on side
2. **Handle identical points:** If start and end are too close (< 1px)
   - Returns simple L-shaped path: `[start, fromOffset, {x: toOffset.x, y: fromOffset.y}, toOffset]`
   - Ensures path ends at `toOffset` (outside node), not `end` (on node edge)
3. **Generate path candidates:**
   - **`buildHV()`:** Horizontal then Vertical
     - Path: `[start, fromOffset, {x: toOffset.x, y: fromOffset.y}, bendBeforeArrow, toOffset]`
     - `bendBeforeArrow`: Point 15px before arrow (towards node) for clean bend
   - **`buildVH()`:** Vertical then Horizontal
     - Path: `[start, fromOffset, {x: fromOffset.x, y: toOffset.y}, bendBeforeArrow, toOffset]`
   - **`buildU()`:** U-shaped for obstacle avoidance
     - Calculates midpoint between offsets
     - Creates U-shape with middle segment
     - Path: `[start, fromOffset, mid1, mid2, bendBeforeArrow, toOffset]`
4. **Test collisions:** For each candidate
   - Checks node collisions via `pathIntersectsNodes()`
   - Checks connection collisions via `pathIntersectsConnections()`
   - Marks candidates with collision flags
5. **Select best path:**
   - **Priority 1:** Collision-free paths (preferred)
   - **Priority 2:** Shortest path (Manhattan distance)
   - Uses `pathManhattanLength()` to compare
6. **Post-processing:**
   - **Ensure orthogonality:** Splits diagonal segments via `ensureOrthogonalSegments()`
   - **Remove duplicates:** Cleans redundant points via `removeDuplicatePoints()`
7. **Minimum points validation:**
   - If less than 4 points, creates L-shape: `[start, fromOffset, {x: toOffset.x, y: fromOffset.y}, toOffset]`
8. **Final validation:**
   - Ensures last point is `toOffset` (outside node), not `end` (on node edge)
   - If last point matches `end` (within 1px), replaces with `toOffset`
   - This ensures arrow marker is positioned correctly outside the node

**Critical Details:**
- **End point:** Always `toOffset` (20px outside node), NOT `end` (on node edge)
- **Bend before arrow:** `ARROW_BEND_OFFSET = 15px` creates clean bend before arrow attachment
- **Path direction:** Always from source to target (first point = start, last point = toOffset)

**Returns:** Array of points forming orthogonal path, ending at `toOffset` (outside target node)

##### `buildHV(start: Point, end: Point, fromOffset: Point, toOffset: Point, toSide: Side): Point[]`

Generates horizontal-vertical path candidate.

**Path structure:**
```
[start] -> [fromOffset] -> [horizontal corner] -> [bendBeforeArrow] -> [toOffset]
```

**Details:**
- `start`: Connection point on source node edge
- `fromOffset`: Point 20px outside source node
- Horizontal corner: `{x: toOffset.x, y: fromOffset.y}` - moves horizontally to target's X
- `bendBeforeArrow`: Point 15px before arrow (calculated via `calculateBendBeforeArrow()`)
  - For `toSide='right'`: `{x: toOffset.x - 15, y: toOffset.y}`
  - For `toSide='left'`: `{x: toOffset.x + 15, y: toOffset.y}`
  - For `toSide='top'`: `{x: toOffset.x, y: toOffset.y + 15}`
  - For `toSide='bottom'`: `{x: toOffset.x, y: toOffset.y - 15}`
- `toOffset`: Final point 20px outside target node (NOT `end` which is on node edge)

**Note:** Path ends at `toOffset`, not `end`, to ensure arrow is positioned outside the node.

##### `buildVH(start: Point, end: Point, fromOffset: Point, toOffset: Point, toSide: Side): Point[]`

Generates vertical-horizontal path candidate.

**Path structure:**
```
[start] -> [fromOffset] -> [vertical corner] -> [bendBeforeArrow] -> [toOffset]
```

**Details:**
- Similar to `buildHV()` but moves vertically first, then horizontally
- Vertical corner: `{x: fromOffset.x, y: toOffset.y}` - moves vertically to target's Y
- Ends at `toOffset` (outside node), not `end` (on node edge)

##### `buildU(start: Point, end: Point, fromOffset: Point, toOffset: Point, fromSide: Side, toSide: Side): Point[]`

Generates U-shaped path candidate for avoiding obstacles.

**Path structure:**
- **Horizontal exit/entry** (`left` or `right`):
  ```
  [start] -> [fromOffset] -> [midX, fromOffset.y] -> [midX, toOffset.y] -> [bendBeforeArrow] -> [toOffset]
  ```
  - Calculates `midX = (fromOffset.x + toOffset.x) / 2`
  - Creates horizontal U with vertical middle segment
  
- **Vertical exit/entry** (`top` or `bottom`):
  ```
  [start] -> [fromOffset] -> [fromOffset.x, midY] -> [toOffset.x, midY] -> [bendBeforeArrow] -> [toOffset]
  ```
  - Calculates `midY = (fromOffset.y + toOffset.y) / 2`
  - Creates vertical U with horizontal middle segment

**Details:**
- Always ends at `toOffset` (outside node), not `end` (on node edge)
- Includes `bendBeforeArrow` point for clean arrow attachment

##### `pathIntersectsNodes(points: Point[], excludeNodeIds?: Set<string>): boolean`

Checks if any segment of the path intersects any node bounds.

- Iterates through path segments
- For each segment, checks intersection with all nodes (excluding source/target)
- Uses `segmentIntersectsNode()` for individual checks

##### `segmentIntersectsNode(p1: Point, p2: Point, bounds: NodeBounds): boolean`

Checks if a line segment intersects a node's bounding box.

**Algorithm:**
1. Calculates segment bounding box
2. Adds small margin (2px) around node bounds
3. Checks bounding box overlap
4. Verifies if segment passes through node (not just touches edge)

##### `ensureOrthogonalSegments(points: Point[], fromSide: Side, toSide: Side): Point[]`

Ensures all path segments are strictly horizontal or vertical.

**Process:**
1. **Remove duplicates:** First calls `removeDuplicatePoints()` to clean input
2. **Process segments:** Iterates through points
   - If segment is already orthogonal (same X or same Y), keeps it
   - If segment is diagonal:
     - Splits into two orthogonal segments
     - First: horizontal move to shared X coordinate
     - Second: vertical move to target Y coordinate
3. **Preserve last point:** Ensures final point is always included
   - Checks if last point matches original last point
   - If not, appends original last point to ensure path reaches target

**Returns:** Array of points with all segments orthogonal

##### `removeDuplicatePoints(points: Point[]): Point[]`

Removes points that are too close together (within EPS).

##### `pathManhattanLength(points: Point[]): number`

Calculates Manhattan distance (sum of horizontal + vertical distances) for path comparison.

##### `wouldCreateCycle(fromNodeId: string, toNodeId: string): boolean`

Checks if creating a connection would create a cycle in the directed graph.

**Algorithm:**
1. **Build adjacency list:** Creates directed graph from existing connections
   - Maps each node to array of its neighbors (targets)
   - Temporarily adds the new connection to test
2. **DFS cycle detection:**
   - Uses two sets: `visited` (all processed nodes) and `recStack` (current DFS path)
   - For each unvisited node:
     - Marks as visited and adds to recursion stack
     - Recursively visits all neighbors
     - If neighbor is in recursion stack → cycle detected
     - Removes from stack when backtracking
3. **Returns:** `true` if cycle would be created, `false` otherwise

**Time Complexity:** O(V + E) where V = nodes, E = edges

**Example:**
```
A → B → C
If adding C → A: cycle detected (C → A → B → C)
If adding C → D: no cycle
```

### 3. CollisionDetector (collision-detector.js)

Handles collision detection and automatic node separation.

#### Properties

- `MARGIN: number = 20` - Minimum distance required between nodes
- `ANIMATION_DURATION: number = 300` - Animation duration in milliseconds
- `STEP_SIZE: number = 10` - Step size when searching for free position

#### Key Methods

##### `rectanglesCollide(rect1: Bounds, rect2: Bounds): boolean`

Checks if two rectangles collide (with margin).

**Algorithm:**
Returns `true` if rectangles overlap or are closer than `MARGIN`.

**Collision condition:**
```
NOT (
  rect1.right + MARGIN < rect2.left OR
  rect2.right + MARGIN < rect1.left OR
  rect1.bottom + MARGIN < rect2.top OR
  rect2.bottom + MARGIN < rect1.top
)
```

##### `checkCollision(node: Node, otherNodes: Map<string, Node>, excludeNodeId?: string): Collision[]`

Checks if a node collides with any other nodes.

- Gets node bounds
- Iterates through all other nodes
- Returns array of collision objects with node info

##### `findFreePosition(node: Node, otherNodes: Map<string, Node>, excludeNodeId?: string): Point`

Finds nearest free position for a node.

**Algorithm:**
1. **Try 8 directions:** N, NE, E, SE, S, SW, W, NW
2. **For each direction:**
   - Move step by step until free position found or canvas boundary reached
   - Check if position is free (no collisions)
   - Track shortest distance
3. **Select best:** Returns position with minimum distance from original
4. **Fallback:** If no position found, uses `findFreePositionSimple()`

##### `findFreePositionSimple(node: Node, otherNodes: Map<string, Node>, excludeNodeId?: string): Point`

Simpler algorithm that searches in grid pattern (down and right).

- Starts from node position
- Moves right by node width + margin
- When reaches canvas edge, moves down and resets horizontal
- Continues until free position found or max attempts reached

##### `animateNodeMove(node: Node, targetX: number, targetY: number, callback?: Function): void`

Animates node movement to target position.

**Process:**
1. Gets node DOM element
2. Checks if already at target (within 1px tolerance)
3. Updates node model position
4. Sets CSS transition property
5. Uses `requestAnimationFrame` to ensure transition applies
6. Sets new position
7. Cleans up transition after animation completes
8. Calls callback if provided

**CSS Transition:**
```css
transition: left 300ms ease-out, top 300ms ease-out;
```

##### `resolveCollisions(node: Node, allNodes: Map<string, Node>, excludeNodeId?: string, visited?: Set<string>): void`

Recursively resolves all collisions for a node.

**Algorithm:**
1. **Prevent infinite loops:** Uses `visited` set to track processed nodes
2. **Check collisions:** Gets all collisions for current node
3. **If no collisions:** Returns early
4. **Find free position:** Calculates nearest free position
5. **Animate move:** Animates node to free position
6. **Recursive check:** After animation, checks if moved node caused new collisions
7. **Resolve recursively:** For each newly colliding node, recursively resolves

**Note:** This ensures cascading collision resolution when nodes are pushed.

##### `resolveAllCollisions(allNodes: Map<string, Node>): void`

Resolves all collisions in the entire system.

- Iterates through all nodes
- For each node with collisions, calls `resolveCollisions()`
- Uses visited set to prevent duplicate processing

## Data Structures

### Node

```typescript
interface Node {
  id: string;              // Unique identifier (e.g., "node-1")
  x: number;               // X position in pixels
  y: number;               // Y position in pixels
  width: number;           // Width in pixels (default: 120)
  height: number;          // Height in pixels (default: 60)
  title: string;           // Display title
  type: string;            // Node type (e.g., "http", "parse")
}
```

### Connection

```typescript
interface Connection {
  id: string;             // Unique identifier
  fromNodeId: string;     // Source node ID
  fromSide: Side;         // Exit side (top, bottom, left, right)
  toNodeId: string;       // Target node ID
  toSide: Side;           // Entry side
}
```

### Point

```typescript
interface Point {
  x: number;
  y: number;
}
```

### Side

```typescript
type Side = 'top' | 'bottom' | 'left' | 'right';
```

### NodeBounds

```typescript
interface NodeBounds {
  x: number;
  y: number;
  width: number;
  height: number;
}
```

## Connection Points

Each node has **4 connection points** (one per side), but only **center points** are used:

- **Top center** - Middle of top edge
- **Bottom center** - Middle of bottom edge
- **Left center** - Middle of left edge
- **Right center** - Middle of right edge

Connection points are rendered as green circles (12px diameter) when `showAnchors` is enabled.

**Position Calculation (`getAnchorPosition`):**
- Gets node's DOM element and bounding rectangle
- Calculates position relative to canvas container
- For each side:
  - `top`: `x = centerX, y = top`
  - `bottom`: `x = centerX, y = bottom`
  - `left`: `x = left, y = centerY`
  - `right`: `x = right, y = centerY`
- Returns coordinates in SVG space (relative to canvas container)

## Connection Routing Flow

1. **User Action:** User drags from source anchor to target anchor
2. **Automatic Anchor Remapping:**
   - System calls `autoSelectBestAnchor()` to find optimal target anchor
   - If connecting from opposite side, automatically selects best side
   - Updates target anchor point if better option found
3. **Get Positions:** System gets SVG coordinates of both anchors via `getAnchorPosition()`
   - Positions are on node edges (center of each side)
   - Coordinates relative to canvas container
4. **Non-Intersecting Route Search:**
   - System calls `findNonIntersectingRoute()` to find best route
   - Tries all 4 anchor point combinations
   - Selects route without connection collisions (preferred) or shortest path
   - Updates target anchor if better point found
5. **Connection Collision Resolution:**
   - If route still has collisions, attempts to resolve by moving nodes
   - Calls `resolveConnectionCollisionsByMovingNodes()` as last resort
   - Moves target node to create space for connection
6. **Route Calculation:**
   - Calculates offsets outside nodes (20px via `OFFSET`)
   - Generates HV, VH, and U-shaped candidates
   - Each candidate includes `bendBeforeArrow` point (15px before arrow)
   - Tests each for node and connection collisions
   - Selects best collision-free path (or shortest if all collide)
4. **Path Post-Processing:**
   - Ensures all segments are orthogonal (splits diagonals)
   - Removes duplicate points
   - Validates minimum 4 points (L-shape)
   - **Critical:** Ensures last point is `toOffset` (outside node), not `end` (on edge)
5. **Path Direction Verification:**
   - Checks if path goes from source to target
   - If reversed, reverses entire path and rebuilds path data
   - Ensures arrow points in correct direction
6. **End Point Correction:**
   - Verifies last point matches target connection point (`toPos`)
   - If not (within 0.1px tolerance), replaces with exact `toPos`
   - Rebuilds path data to ensure arrow touches connection point
7. **SVG Path Generation:**
   - Creates path data string with `M`, `H`, `V`, `L` commands
   - Skips duplicate points to avoid redundant commands
   - Path ends exactly at connection point
8. **Arrow Marker Attachment:**
   - Ensures marker exists (creates if missing)
   - Sets `marker-end="url(#arrowhead)"`
   - Marker tip (refX=12) touches connection point exactly
   - `orient="auto"` rotates marker to match path direction
9. **Hit Areas:** Creates invisible circles at path ends for dragging
10. **Event Handlers:** Attaches click handler for selection

## Collision Resolution Flow

### During Node Addition

1. Node created at specified or random position
2. `checkCollision()` called immediately
3. If collisions found:
   - `findFreePosition()` calculates nearest free spot
   - Node position updated immediately (no animation)
   - `renderNode()` updates visual
4. `requestAnimationFrame` schedules final collision resolution
5. `resolveCollisions()` animates any remaining adjustments

### During Node Dragging

1. Mouse move event calculates new position
2. If `autoResolveCollisions` enabled:
   - Creates test node with new position
   - Checks collisions
   - If collision: finds free position and uses it
   - If no collision: uses calculated position
3. Node rendered at resolved position
4. Connections updated

### After Node Drag End

1. Mouse up event triggers
2. Final collision check performed
3. If collisions exist:
   - `resolveCollisions()` called
   - Node animated to free position
4. Routing and connections updated

## Event Flow

### Node Interaction

```
mousedown on node
  -> onNodeMouseDown()
    -> Sets draggingNode
    -> Stores drag start offset

mousemove on canvas
  -> onMouseMove()
    -> Calculates new position
    -> Checks/resolves collisions
    -> Renders node
    -> Updates connections

mouseup on canvas
  -> onMouseUp()
    -> Final collision check
    -> Animates if needed
    -> Clears draggingNode
```

### Connection Creation

```
mousedown on anchor
  -> onAnchorMouseDown()
    -> Sets draggingConnection
    -> Stores start point

mousemove on canvas
  -> onMouseMove()
    -> Renders temporary connection line

mouseup on anchor
  -> onMouseUp()
    -> Checks if dropped on valid anchor
    -> Creates connection if valid
    -> Renders all connections
```

### Connection Modification

```
click on connection path
  -> selectConnection()
    -> Highlights connection

mousedown on connection end hit area
  -> onConnectionEndMouseDown()
    -> Sets draggingConnection with connectionId and endType

mousemove
  -> Renders temporary connection

mouseup on anchor
  -> Updates connection (fromNodeId/toNodeId)
  -> Re-renders connections
```

## Configuration

### Automatic Collision Resolution

Enabled by default (`autoResolveCollisions: true`). Can be toggled in code:

```javascript
app.autoResolveCollisions = false; // Disable
```

### Collision Detection Parameters

In `CollisionDetector`:

- `MARGIN: 20` - Minimum distance between nodes
- `ANIMATION_DURATION: 300` - Animation speed (ms)
- `STEP_SIZE: 10` - Search granularity

### Routing Parameters

In `SimpleConnectionRouter`:

- `OFFSET: 20` - Distance from node edge
- `MIN_SEPARATION_PX: 1` - Collision threshold
- `MIN_FORCED_SEPARATION: 20` - Minimum path separation

## Debug Features

### Debug Overlay

Enable with checkbox "Debug overlay". Shows:
- Red dots at all routing points
- Red dashed lines for path segments
- Tooltips with point coordinates

### Console Logging

`CollisionDetector` logs:
- Collision detection events
- Node movement operations
- Position calculations

## Browser Compatibility

- Modern browsers with ES6+ support
- CSS transitions support required
- SVG support required
- `requestAnimationFrame` API required

## Performance Considerations

- **Collision detection:** O(n) for each node check, where n = number of nodes
- **Path routing:** O(n) for collision testing, where n = number of nodes
- **Animation:** Uses CSS transitions (hardware accelerated)
- **Rendering:** Updates only affected connections on node move

## Arrow Marker Implementation

### SVG Marker System

The arrow markers are implemented using SVG `<marker>` elements with the following configuration:

**Marker Definition:**
```xml
<marker id="arrowhead" 
        markerWidth="12" 
        markerHeight="12" 
        refX="12" 
        refY="6" 
        orient="auto" 
        markerUnits="userSpaceOnUse" 
        viewBox="0 0 12 12">
  <polygon points="0 0, 12 6, 0 12" 
           fill="#64748b" 
           stroke="#64748b" 
           stroke-width="1" />
</marker>
```

**Key Attributes:**
- **`markerWidth/markerHeight: 12`** - Marker size in user units (pixels)
- **`refX: 12`** - Reference point X coordinate (tip of arrow)
  - With `refX=12`, the arrow tip is positioned at the path end
  - This ensures the arrow tip touches the connection point exactly
- **`refY: 6`** - Reference point Y coordinate (center of arrow)
- **`orient="auto"`** - Automatically rotates marker to match path direction
- **`markerUnits="userSpaceOnUse"`** - Marker size in user units, not stroke width
- **`viewBox="0 0 12 12"`** - Defines coordinate system for marker

**Arrow Shape:**
- Polygon points: `0 0, 12 6, 0 12`
  - Base-left: `(0, 0)`
  - Tip: `(12, 6)` - points to the right
  - Base-right: `(0, 12)`
- Arrow points in positive X direction (right)
- `orient="auto"` rotates it to match path direction

**Path Attachment:**
- Path has `marker-end="url(#arrowhead)"` attribute
- Marker is positioned at the last point of the path
- With `refX=12`, the tip (at x=12 in viewBox) is at the path end
- This means the arrow tip touches the connection point exactly

**Path End Correction:**
- Router returns points ending at `toOffset` (20px outside node)
- Before rendering, last point is corrected to exact `toPos` (connection point)
- This ensures arrow tip touches the connection point precisely
- Tolerance: 0.1px - if last point is further, it's replaced

### Cycle Detection

The system prevents creating cycles in the connection graph using DFS (Depth-First Search):

**Algorithm:**
1. Builds directed graph from existing connections
2. Temporarily adds new connection to test
3. Performs DFS from all nodes
4. Detects cycles by checking if a node is in recursion stack
5. Returns `true` if cycle would be created

**Implementation:**
- Uses `visited` set to track processed nodes
- Uses `recStack` set to track current DFS path
- If neighbor is in `recStack` → cycle detected
- Prevents infinite loops and maintains acyclic graph structure

### One-to-One Connection Rule

**Outgoing Connections:**
- Each node can have maximum **one outgoing connection**
- Applies regardless of which side the connection exits from
- If node already has outgoing connection, user is asked to replace it
- Old connection is removed before creating new one

**Incoming Connections:**
- Each node can have maximum **one incoming connection**
- Applies regardless of which side the connection enters from
- If node already has incoming connection, user is asked to replace it
- Old connection is removed before creating new one

**Rationale:**
- Ensures linear flow without branching or merging
- Simplifies routing and prevents complex graph structures
- Matches typical workflow execution patterns

## Automatic Intersection Avoidance

The system automatically prevents connection line intersections through multiple strategies:

### Strategy 1: Automatic Anchor Remapping

When connecting nodes, the system automatically selects the best anchor points:
- **180-degree reversal detection:** Checks if connection is reversed by 180 degrees
- **If reversed:** Automatically remaps both source and target anchor points
  - Calculates distances from source to all target anchor points
  - Calculates distances from all source anchor points to target
  - Selects closest anchor points for both nodes
- **If not reversed:** Only remaps target anchor point
  - Calculates distances from source to all target anchor points
  - Selects closest anchor point
- Adjusts selection based on node positions to avoid awkward routing

### Strategy 2: Multi-Point Route Testing

During connection creation, the system:
- Tries all 16 anchor point combinations (4 source × 4 target)
- Tests each for node and connection collisions
- Applies scoring system:
  - Prefers collision-free routes
  - Then shortest path
  - Penalizes 180-degree reversals (but less than collisions)
  - Bonuses for using original anchor points
- Selects route with best score
- Automatically updates connection to use best anchor points (both source and target)

### Strategy 3: Automatic Node Movement

As last resort, if routing cannot avoid intersections:
- Calculates connection direction
- Moves target node in opposite direction (50px offset)
- Resolves any resulting node collisions
- Creates space for clean routing

### Strategy 4: Runtime Rerouting

During `renderAll()`, the system:
- Checks all connections for intersections
- Automatically reroutes intersecting connections
- Updates anchor points if better options found
- Ensures clean visual layout

## Known Limitations

1. **No grid snapping:** Nodes can be placed at any pixel position
2. **Simple collision resolution:** May not find optimal positions in complex scenarios
3. **No undo/redo:** Operations are not reversible
4. **Single canvas:** No multi-page or zoom support
5. **Fixed node sizes:** All nodes have same default dimensions
6. **One-to-one connections:** Each node limited to one incoming and one outgoing connection
7. **Intersection resolution:** May not always find perfect solution in complex layouts

## Future Enhancements

Potential improvements:
- Grid snapping
- Undo/redo system
- Zoom and pan
- Node resizing
- Custom node shapes
- Connection waypoints
- Export/import functionality
- Performance optimizations for large node counts

