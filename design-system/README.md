# Design System - Connection Routing Mockup

Simplified canvas mockup for testing connection routing system between labels (nodes).

## Features

- ✅ **Add Labels** - Click "Add Label" to add a new label
- ✅ **Drag Labels** - Drag a label to move it
- ✅ **Connection Points** - Each label has 4 connection points (top, bottom, left, right) - center points only
- ✅ **Create Connections** - Click on a connection point and drag to another point
- ✅ **Modify Connections** - Click on an arrow (connection), then drag the arrow end to another label to change the target
- ✅ **Orthogonal Routing** - Lines are only horizontal/vertical (orthogonal)
- ✅ **Collision Detection** - Router avoids intersecting labels (uses U-shaped routing when needed)
- ✅ **Automatic Label Separation** - Overlapping labels are automatically separated with animation
- ✅ **Automatic Anchor Remapping** - System automatically selects the best connection point when connecting from opposite side
- ✅ **Intersection Avoidance** - System automatically changes routing to avoid connection line intersections
- ✅ **Automatic Label Movement** - As last resort, system moves labels to avoid intersections
- ✅ **Debug Overlay** - Enable "Debug overlay" to see routing points

## How to Use

1. Open `index.html` in a browser
2. Click "Add Label" to add a new label
3. Drag a label to move it
4. Click on a green connection point (center point) and drag to another point
5. Connection will be created with automatic routing
6. **Modify Connection:** Click on an arrow (connection), then drag the arrow end to another label to change the target

## Test Scenarios

- **Test Routing** - Automatically creates a test scenario with 4 labels and connections
- **Test Collision** - Adds 5 labels close together to see automatic separation
- **Load Config** - Simulates loading a configuration with overlapping labels

## Routing Rules

1. **Center Points Only** - Connections connect only to center points on each edge
2. **Orthogonal Routing** - Lines are only horizontal/vertical (no diagonals)
3. **Collision Avoidance** - Router checks if path intersects labels and uses U-shaped routing when needed
4. **Best Path Selection** - Router selects path without collisions, if all have collisions - shortest one
5. **One-to-One** - Each label can have only one outgoing and one incoming connection
6. **No Cycles** - System prevents creating loops in the connection chain
7. **Arrows** - Arrow touches the connection point exactly and points from source to target
8. **No Line Intersections** - System automatically avoids connection line intersections through:
   - Automatic connection point remapping
   - Trying different routing point combinations
   - As last resort, moving labels

## File Structure

- `index.html` - Main HTML file
- `styles.css` - CSS styles
- `connection-router.js` - Connection router (SimpleConnectionRouter)
- `collision-detector.js` - Collision detection and automatic label separation
- `app.js` - Main application logic

## Technologies

- Vanilla JavaScript (no frameworks)
- SVG for line rendering
- HTML/CSS for labels and UI

## Documentation

Detailed technical documentation in English is available in **[DOCUMENTATION.md](DOCUMENTATION.md)**.

The documentation includes:
- System architecture description
- Detailed description of all components and methods
- Routing and collision detection algorithms
- Data structures
- Event flow
- Configuration and parameters
- Performance considerations

