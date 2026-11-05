# Implementation Details - Design System Connection Routing

This document provides detailed technical information about the implementation of the connection routing system, arrow markers, and related algorithms.

## Table of Contents

1. [Arrow Marker System](#arrow-marker-system)
2. [Path Routing Algorithm](#path-routing-algorithm)
3. [Cycle Detection](#cycle-detection)
4. [One-to-One Connection Rule](#one-to-one-connection-rule)
5. [Path Direction Correction](#path-direction-correction)
6. [Coordinate Systems](#coordinate-systems)
7. [Performance Optimizations](#performance-optimizations)

## Arrow Marker System

### SVG Marker Configuration

The arrow markers use SVG `<marker>` elements with specific attributes for precise positioning:

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

### Marker Positioning

**Key Attribute: `refX="12"`**

The `refX` attribute defines the reference point where the marker attaches to the path:
- With `refX=12`, the reference point is at x=12 in the marker's viewBox
- Since the arrow tip is at x=12, the tip is positioned at the path end
- This ensures the arrow tip touches the connection point exactly

**How it works:**
1. Path ends at connection point (e.g., `{x: 400, y: 182}`)
2. Marker is attached with `marker-end="url(#arrowhead)"`
3. SVG positions marker so that `refX` (x=12) aligns with path end
4. Arrow tip (at x=12 in viewBox) is at the connection point
5. Arrow points in the direction of the path (via `orient="auto"`)

### Marker Orientation

**`orient="auto"`** automatically rotates the marker to match the path direction:
- Calculates tangent vector at path end
- Rotates marker to align with tangent
- Ensures arrow always points in the direction of travel

### Marker Creation and Management

The `ensureMarkerExists()` method:
1. Checks if marker with id `arrowhead` exists in SVG `<defs>`
2. If not found:
   - Creates `<defs>` if missing
   - Creates `<marker>` element with all attributes
   - Creates `<polygon>` for arrow shape
   - Appends to `<defs>`
3. Returns marker element (existing or newly created)

**Why this is needed:**
- Marker might be removed during SVG clearing operations
- Ensures marker is always available before setting `marker-end`
- Prevents rendering errors when marker is missing

## Path Routing Algorithm

### Overview

The routing algorithm generates orthogonal paths (horizontal/vertical only) between connection points, avoiding node collisions and connection intersections.

### Step-by-Step Process

#### 1. Input Processing

```javascript
route(from: Point, to: Point, fromSide: Side, toSide: Side, excludeNodeIds?: Set<string>)
```

- `from`: Connection point on source node edge
- `to`: Connection point on target node edge
- `fromSide/toSide`: Exit/entry sides for offset calculation
- `excludeNodeIds`: Nodes to exclude from collision detection (source and target)

#### 2. Offset Calculation

```javascript
const fromOffset = offsetPoint(start, fromSide, OFFSET); // 20px outside source
const toOffset = offsetPoint(end, toSide, OFFSET);     // 20px outside target
```

**Offset directions:**
- `top`: `y = point.y - 20`
- `bottom`: `y = point.y + 20`
- `left`: `x = point.x - 20`
- `right`: `x = point.x + 20`

**Why offsets?**
- Paths should not touch node edges
- Provides space for clean routing
- Ensures arrow is positioned outside node

#### 3. Path Candidate Generation

Three types of paths are generated:

**A. Horizontal-Vertical (HV):**
```
[start] → [fromOffset] → [toOffset.x, fromOffset.y] → [bendBeforeArrow] → [toOffset]
```
- Moves horizontally first, then vertically
- Good for side-to-side connections

**B. Vertical-Horizontal (VH):**
```
[start] → [fromOffset] → [fromOffset.x, toOffset.y] → [bendBeforeArrow] → [toOffset]
```
- Moves vertically first, then horizontally
- Good for top-to-bottom connections

**C. U-Shaped:**
```
[start] → [fromOffset] → [mid1] → [mid2] → [bendBeforeArrow] → [toOffset]
```
- Creates U-shape with middle segment
- Used for obstacle avoidance
- Calculates midpoint between offsets

#### 4. Bend Before Arrow

The `calculateBendBeforeArrow(toOffset, toSide)` function creates a point 15px before the arrow:

```javascript
switch (toSide) {
  case 'top': return { x: toOffset.x, y: toOffset.y + 15 };
  case 'bottom': return { x: toOffset.x, y: toOffset.y - 15 };
  case 'left': return { x: toOffset.x + 15, y: toOffset.y };
  case 'right': return { x: toOffset.x - 15, y: toOffset.y };
}
```

**Purpose:**
- Creates clean bend before arrow attachment
- Prevents arrow from being too close to last corner
- Improves visual appearance

#### 5. Collision Detection

For each candidate path:

**Node Collision Check:**
- Iterates through path segments
- For each segment, checks intersection with all nodes (except excluded)
- Uses `segmentIntersectsNode()` with 5px margin
- Returns `true` if any segment intersects a node

**Connection Collision Check:**
- Checks if path intersects any existing connection paths
- Uses `pathIntersectsConnections()` method
- Compares path segments with existing connection segments
- Returns `true` if intersection found

#### 6. Path Selection

**Priority order:**
1. Collision-free paths (preferred)
2. Shortest path (Manhattan distance)

**Manhattan Distance:**
```javascript
pathManhattanLength(points) {
  let len = 0;
  for (let i = 1; i < points.length; i++) {
    len += Math.abs(points[i].x - points[i-1].x) + 
           Math.abs(points[i].y - points[i-1].y);
  }
  return len;
}
```

#### 7. Post-Processing

**Orthogonal Enforcement:**
- `ensureOrthogonalSegments()` splits any diagonal segments
- Ensures all segments are strictly horizontal or vertical
- Preserves last point to ensure path reaches target

**Duplicate Removal:**
- `removeDuplicatePoints()` removes points within EPS (1e-6)
- Prevents redundant path commands

**Minimum Points:**
- Ensures at least 4 points (L-shape)
- If less, creates: `[start, fromOffset, corner, toOffset]`

**End Point Validation:**
- Ensures last point is `toOffset` (outside node), not `end` (on edge)
- If last point matches `end` (within 1px), replaces with `toOffset`
- Critical for proper arrow positioning

### Path Data Generation

The path data string is built using SVG commands:

```javascript
let pathData = `M ${points[0].x} ${points[0].y}`; // Move to start

for (let i = 1; i < points.length; i++) {
  const prev = points[i - 1];
  const curr = points[i];
  
  // Skip duplicates
  if (Math.abs(prev.x - curr.x) < 0.1 && Math.abs(prev.y - curr.y) < 0.1) {
    continue;
  }
  
  // Horizontal line
  if (Math.abs(prev.x - curr.x) < 0.1) {
    pathData += ` V ${curr.y}`;
  }
  // Vertical line
  else if (Math.abs(prev.y - curr.y) < 0.1) {
    pathData += ` H ${curr.x}`;
  }
  // Diagonal (shouldn't occur, but handled)
  else {
    pathData += ` L ${curr.x} ${curr.y}`;
  }
}
```

**Commands used:**
- `M x y` - Move to point (start)
- `H x` - Horizontal line to x
- `V y` - Vertical line to y
- `L x y` - Line to point (for diagonals)

## Cycle Detection

### Algorithm: Depth-First Search (DFS)

The `wouldCreateCycle()` method uses DFS to detect cycles:

```javascript
wouldCreateCycle(fromNodeId, toNodeId) {
  // 1. Build adjacency list
  const graph = new Map();
  this.connections.forEach(conn => {
    if (!graph.has(conn.fromNodeId)) {
      graph.set(conn.fromNodeId, []);
    }
    graph.get(conn.fromNodeId).push(conn.toNodeId);
  });
  
  // 2. Add new connection temporarily
  if (!graph.has(fromNodeId)) {
    graph.set(fromNodeId, []);
  }
  graph.get(fromNodeId).push(toNodeId);
  
  // 3. DFS cycle detection
  const visited = new Set();
  const recStack = new Set();
  
  const hasCycleDFS = (node) => {
    if (recStack.has(node)) return true; // Cycle!
    if (visited.has(node)) return false;
    
    visited.add(node);
    recStack.add(node);
    
    const neighbors = graph.get(node) || [];
    for (const neighbor of neighbors) {
      if (hasCycleDFS(neighbor)) return true;
    }
    
    recStack.delete(node);
    return false;
  };
  
  // 4. Check all nodes
  for (const nodeId of graph.keys()) {
    if (!visited.has(nodeId)) {
      if (hasCycleDFS(nodeId)) return true;
    }
  }
  
  return false;
}
```

### How It Works

1. **Build Graph:** Creates adjacency list from existing connections
2. **Add Test Connection:** Temporarily adds new connection to graph
3. **DFS Traversal:** 
   - `visited`: Tracks all processed nodes
   - `recStack`: Tracks current DFS path (backtracking stack)
   - If node is in `recStack` when visited → cycle detected
4. **Check All Nodes:** Ensures disconnected components are checked

### Example

```
Existing: A → B → C
Adding: C → A

DFS from A:
  A → B (recStack: {A, B})
  B → C (recStack: {A, B, C})
  C → A (A in recStack!) → CYCLE DETECTED
```

## One-to-One Connection Rule

### Implementation

The `createConnection()` method enforces one-to-one rule:

```javascript
// Check outgoing connections
const existingOutgoing = [];
this.connections.forEach((conn, connId) => {
  if (conn.fromNodeId === fromNodeId) {
    existingOutgoing.push(connId);
  }
});

// Check incoming connections
const existingIncoming = [];
this.connections.forEach((conn, connId) => {
  if (conn.toNodeId === toNodeId) {
    existingIncoming.push(connId);
  }
});

// Ask user for confirmation
if (existingOutgoing.length > 0) {
  if (!confirm('Replace existing outgoing connection?')) {
    return false;
  }
}

if (existingIncoming.length > 0) {
  if (!confirm('Replace existing incoming connection?')) {
    return false;
  }
}

// Remove old connections
[...existingOutgoing, ...existingIncoming].forEach(connId => {
  this.connections.delete(connId);
  this.router.removeExistingPath(connId);
});
```

### Rationale

- **Linear Flow:** Ensures each node has single input and output
- **Simplified Routing:** Easier to route without branching
- **Workflow Pattern:** Matches typical sequential workflow execution

## Path Direction Correction

### Problem

Router might return points in wrong order (target to source instead of source to target).

### Solution

```javascript
// Verify path direction
const firstPoint = points[0];
const lastPoint = points[points.length - 1];

const distFromFirst = Math.abs(firstPoint.x - fromPos.x) + 
                      Math.abs(firstPoint.y - fromPos.y);
const distFromLast = Math.abs(lastPoint.x - fromPos.x) + 
                     Math.abs(lastPoint.y - fromPos.y);

// If reversed, reverse the path
if (distFromFirst > distFromLast) {
  points.reverse();
  // Rebuild path data with reversed points
  pathData = rebuildPathData(points);
}
```

### How It Works

1. **Distance Check:** Compares first and last point distances to source
2. **Reversal:** If first point is closer to target, path is reversed
3. **Rebuild:** Rebuilds path data string with corrected order
4. **Result:** Path always goes from source to target

## End Point Correction

### Problem

Router returns points ending at `toOffset` (20px outside node), but arrow should touch connection point exactly.

### Solution

```javascript
// Ensure path ends exactly at connection point
if (points.length > 0 && toPos) {
  const lastPoint = points[points.length - 1];
  const distToTarget = Math.abs(lastPoint.x - toPos.x) + 
                       Math.abs(lastPoint.y - toPos.y);
  
  // If not exact, replace last point
  if (distToTarget > 0.1) {
    points[points.length - 1] = { x: toPos.x, y: toPos.y };
    // Rebuild path data
    pathData = rebuildPathData(points);
  }
}
```

### Why This Matters

- Arrow marker tip (refX=12) is positioned at path end
- If path ends at `toOffset` (20px away), arrow doesn't touch connection point
- By correcting to exact `toPos`, arrow tip touches connection point exactly
- Tolerance: 0.1px - very small differences are acceptable

## Coordinate Systems

### Canvas Container Coordinates

All positions are relative to the canvas container:

```javascript
getAnchorPosition(nodeId, side) {
  const rect = this.canvasContainer.getBoundingClientRect();
  const nodeRect = nodeEl.getBoundingClientRect();
  
  // Calculate relative to container
  x = nodeRect.left - rect.left + offset;
  y = nodeRect.top - rect.top + offset;
}
```

### SVG Coordinate Space

SVG element uses same coordinate system:
- `viewBox` matches container dimensions
- `preserveAspectRatio="none"` ensures 1:1 mapping
- Path coordinates directly match container coordinates

### Node Bounds

Router uses node bounds for collision detection:

```javascript
setNodeBounds(nodeId, {
  x: node.x,        // Node position
  y: node.y,
  width: node.width,
  height: node.height
});
```

Bounds are in same coordinate system as paths.

## Performance Optimizations

### Two-Pass Rendering

In `renderAll()`, connections are rendered in two passes:

**Pass 1: Route and Cache**
```javascript
const pathCache = new Map();
this.connections.forEach((conn, connId) => {
  const points = this.router.route(...);
  pathCache.set(connId, points);
  this.router.setExistingPaths(connId, points);
});
```

**Pass 2: Render**
```javascript
this.connections.forEach(conn => {
  this.renderConnection(conn);
});
```

**Why:**
- `pathIntersectsConnections()` needs all other paths
- First pass ensures all paths are available
- Second pass can check intersections correctly

### RequestAnimationFrame Batching

Connection creation uses `requestAnimationFrame`:

```javascript
createConnection(...) {
  this.connections.set(connectionId, connection);
  requestAnimationFrame(() => {
    this.renderAll();
    this.updateInfo();
  });
}
```

**Benefits:**
- Batches DOM updates
- Ensures DOM is ready before rendering
- Prevents race conditions

### Collision Detection Optimization

- Node bounds cached in router
- Only checks nodes, not all DOM elements
- Uses bounding box checks (fast) before precise intersection

## Debugging

### Console Logging

Key debug points:
- `[Router]` - Router input/output
- `[Path]` - Path generation details
- `[ConnectionRenderer]` - Marker assignment
- `[Marker]` - Marker creation
- `[Connection]` - Connection creation/validation

### Debug Overlay

Enable with "Debug overlay" checkbox:
- Red dots at all routing points
- Red dashed lines for path segments
- Tooltips with coordinates

### Visual Debugging

- Connection points highlighted when dragging
- Temporary connection line during drag
- Selected connection highlighted in blue




