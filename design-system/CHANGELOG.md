# Changelog

All notable changes to the Design System mockup will be documented in this file.

## [Unreleased]

### Added
- Comprehensive technical documentation (DOCUMENTATION.md)
- API reference guide (API_REFERENCE.md)
- Changelog tracking
- Detailed arrow marker implementation documentation
- Cycle detection algorithm documentation
- One-to-one connection rule documentation
- Path direction verification and correction
- End point correction to ensure arrow touches connection point
- **Automatic anchor remapping** - System automatically selects best connection point when connecting from opposite side
- **Intersection avoidance** - System automatically prevents connection line intersections through multiple strategies:
  - Automatic anchor point remapping
  - Multi-point route testing (tries all 4 anchor combinations)
  - Automatic node movement as last resort
  - Runtime rerouting during render
- **`autoSelectBestAnchor()`** - Automatically selects optimal anchor point based on source position
- **`findNonIntersectingRoute()`** - Finds routing path without intersections by trying all anchor combinations
- **`resolveConnectionCollisionsByMovingNodes()`** - Moves nodes to resolve connection collisions

### Fixed
- Arrow marker positioning - now touches connection point exactly
- Path direction - automatic reversal if path is backwards
- End point correction - ensures path ends at exact connection point
- Marker visibility - proper refX positioning for correct arrow direction
- Router end points - always returns toOffset (outside node) not end (on edge)
- Connection line intersections - automatically avoided through multiple strategies

## [1.0.0] - 2024-12-13

### Added
- Initial implementation of connection routing mockup
- Node management (add, drag, select)
- Connection creation and modification
- Orthogonal path routing with collision avoidance
- Automatic collision detection and resolution
- Animated node separation
- Connection point rendering (center points only)
- Debug overlay for routing visualization
- Test scenarios (routing, collision, configuration loading)

### Features

#### Node System
- Add nodes at random or specific positions
- Drag nodes with real-time collision prevention
- Visual selection feedback
- Automatic collision resolution on add/drag
- Animated separation when collisions detected

#### Connection System
- Create connections by dragging between anchors
- Modify connections by dragging connection ends
- Orthogonal routing (horizontal/vertical only)
- Collision detection for paths
- U-shaped routing for obstacle avoidance
- Visual connection selection
- Arrow markers on connections
- **One-to-one rule:** Each node limited to one incoming and one outgoing connection
- **Cycle prevention:** DFS-based cycle detection prevents creating loops
- **Arrow positioning:** Arrow tip touches connection point exactly (refX=12)
- **Path direction:** Automatic verification and correction of path direction
- **End point correction:** Ensures path ends at exact connection point (0.1px tolerance)

#### Routing Algorithm
- HV (Horizontal-Vertical) path generation
- VH (Vertical-Horizontal) path generation
- U-shaped path generation for collision avoidance
- Path optimization (shortest collision-free path)
- Orthogonal segment enforcement
- Duplicate point removal
- **Bend before arrow:** 15px offset creates clean bend before arrow attachment
- **End point handling:** Always ends at toOffset (20px outside node), not on node edge
- **Path direction:** Ensures path goes from source to target
- **Connection collision detection:** Checks intersections with other connection paths

#### Collision Detection
- Rectangle collision detection with margin
- Real-time collision checking during drag
- Automatic free position finding
- 8-direction search algorithm
- Grid-based fallback algorithm
- Recursive collision resolution
- CSS transition animations

### Technical Details

#### Files
- `index.html` - Main HTML structure
- `styles.css` - Visual styling
- `app.js` - Main application logic (676 lines)
- `connection-router.js` - Routing algorithm (201 lines)
- `collision-detector.js` - Collision system (282 lines)

#### Dependencies
- None (vanilla JavaScript)
- Modern browser with ES6+ support
- CSS transitions support
- SVG support

### Known Limitations
- No grid snapping
- Simple collision resolution (may not find optimal positions)
- No undo/redo
- Single canvas (no zoom/pan)
- Fixed node sizes
- No connection waypoints

### Performance
- O(n) collision detection per node
- O(n) path collision testing
- Hardware-accelerated CSS animations
- Incremental connection updates

