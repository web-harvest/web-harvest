/**
 * TDD Tests for Canvas Interaction
 * 
 * Features:
 * 1. Drag & move nodes on canvas
 * 2. Zoom in/out canvas
 * 3. Pan canvas (drag background)
 * 4. Select multiple nodes
 * 5. Delete nodes (keyboard + context menu)
 * 6. Undo/Redo
 * 
 * UX Inspiration: Orkes Conductor, Azure Logic Apps, Apple HIG
 */

import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../../types';

describe('Canvas Interaction (TDD - Apple HIG)', () => {
    let designer: VisualDesigner;

    beforeEach(() => {
        document.body.innerHTML = '<div id="test-canvas"></div>';
        designer = new VisualDesigner('test-canvas');
    });

    describe('Drag & Move Nodes', () => {
        it('should move node when dragged', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            designer.loadNodes([node]);

            // Simulate drag
            const moved = designer.moveNode('node-1', { x: 200, y: 150 });

            expect(moved).toBe(true);
            const updatedNode = designer.findNode('node-1');
            expect(updatedNode?.position).toEqual({ x: 200, y: 150 });
        });

        it('should handle drag start event', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            designer.loadNodes([node]);

            const dragStart = designer.handleNodeDragStart('node-1', { x: 105, y: 110 });

            expect(dragStart).toBeTruthy();
            expect(dragStart.nodeId).toBe('node-1');
            expect(dragStart.offsetX).toBe(5); // 105 - 100
            expect(dragStart.offsetY).toBe(10); // 110 - 100
        });

        it('should handle drag move with offset', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            designer.loadNodes([node]);

            // Start drag
            const dragInfo = designer.handleNodeDragStart('node-1', { x: 105, y: 110 });

            // Move to new position
            designer.handleNodeDragMove({ x: 250, y: 300 }, dragInfo!);

            const movedNode = designer.findNode('node-1');
            expect(movedNode?.position).toEqual({
                x: 245, // 250 - 5 (offsetX)
                y: 290  // 300 - 10 (offsetY)
            });
        });

        it('should snap to grid when enabled', () => {
            designer.setGridSnap(true, 20); // Snap to 20px grid

            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            designer.loadNodes([node]);

            // Move to 213, 187
            designer.moveNode('node-1', { x: 213, y: 187 });

            const movedNode = designer.findNode('node-1');
            expect(movedNode?.position).toEqual({
                x: 220, // Snapped to nearest 20
                y: 180  // Snapped to nearest 20
            });
        });

        it('should prevent moving nodes outside canvas bounds', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            designer.loadNodes([node]);

            // Try to move to negative coordinates
            designer.moveNode('node-1', { x: -50, y: -100 });

            const movedNode = designer.findNode('node-1');
            expect(movedNode?.position?.x).toBeGreaterThanOrEqual(0);
            expect(movedNode?.position?.y).toBeGreaterThanOrEqual(0);
        });
    });

    describe('Zoom & Pan', () => {
        it('should zoom in (increase scale)', () => {
            const initialZoom = designer.getZoom();

            designer.zoomIn();

            expect(designer.getZoom()).toBeGreaterThan(initialZoom);
        });

        it('should zoom out (decrease scale)', () => {
            const initialZoom = designer.getZoom();

            designer.zoomOut();

            expect(designer.getZoom()).toBeLessThan(initialZoom);
        });

        it('should zoom to specific level', () => {
            designer.setZoom(1.5);

            expect(designer.getZoom()).toBe(1.5);
        });

        it('should constrain zoom to min/max limits', () => {
            // Too low
            designer.setZoom(0.05);
            expect(designer.getZoom()).toBeGreaterThanOrEqual(0.1); // Min: 10%

            // Too high
            designer.setZoom(10.0);
            expect(designer.getZoom()).toBeLessThanOrEqual(3.0); // Max: 300%
        });

        it('should zoom to mouse position (zoom center)', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            designer.loadNodes([node]);

            // Zoom in at specific point
            designer.zoomAt({ x: 200, y: 200 }, 1.5);

            // Node position should adjust to keep zoom center fixed
            expect(designer.getZoom()).toBe(1.5);
        });

        it('should pan canvas (drag background)', () => {
            const initialPan = designer.getPan();

            designer.pan({ x: 50, y: 30 });

            const newPan = designer.getPan();
            expect(newPan.x).toBe(initial Pan.x + 50);
            expect(newPan.y).toBe(initialPan.y + 30);
        });

        it('should reset zoom and pan', () => {
            designer.setZoom(1.5);
            designer.pan({ x: 100, y: 50 });

            designer.resetView();

            expect(designer.getZoom()).toBe(1.0);
            expect(designer.getPan()).toEqual({ x: 0, y: 0 });
        });
    });

    describe('Selection', () => {
        it('should select single node on click', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.selectNode('node-1');

            expect(designer.getSelectedNodeIds()).toEqual(['node-1']);
        });

        it('should select multiple nodes with Cmd/Ctrl+Click', () => {
            const nodes: VisualNode[] = [
                { id: 'node-1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'node-2', type: 'xpath', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);

            designer.selectNode('node-1');
            designer.selectNode('node-2', { addToSelection: true }); // Cmd+Click

            expect(designer.getSelectedNodeIds()).toEqual(['node-1', 'node-2']);
        });

        it('should deselect when clicking background', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.selectNode('node-1');

            designer.deselectAll();

            expect(designer.getSelectedNodeIds()).toEqual([]);
        });

        it('should select nodes with rectangle drag (marquee)', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [], position: { x: 100, y: 100 } },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [], position: { x: 200, y: 100 } },
                { id: 'n3', type: 'def', attributes: {}, body: null, children: [], position: { x: 100, y: 300 } }
            ];

            designer.loadNodes(nodes);

            // Drag rectangle from (50, 50) to (250, 200)
            const selected = designer.selectNodesInRectangle({
                x: 50, y: 50, width: 200, height: 150
            });

            expect(selected).toContain('n1');
            expect(selected).toContain('n2');
            expect(selected).not.toContain('n3'); // Outside rectangle
        });
    });

    describe('Delete Nodes', () => {
        it('should delete selected node with Delete key', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.selectNode('node-1');

            designer.deleteSelectedNodes();

            expect(designer.getNodes()).toHaveLength(0);
        });

        it('should delete multiple selected nodes', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] },
                { id: 'n3', type: 'def', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.selectNode('n1');
            designer.selectNode('n2', { addToSelection: true });

            designer.deleteSelectedNodes();

            expect(designer.getNodes()).toHaveLength(1);
            expect(designer.getNodes()[0].id).toBe('n3');
        });
    });

    describe('Keyboard Shortcuts (Apple HIG)', () => {
        it('should handle Cmd+A (Select All)', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.handleKeyboard({ key: 'a', metaKey: true });

            expect(designer.getSelectedNodeIds()).toHaveLength(2);
        });

        it('should handle Delete/Backspace', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.selectNode('node-1');
            designer.handleKeyboard({ key: 'Delete' });

            expect(designer.getNodes()).toHaveLength(0);
        });

        it('should handle Cmd+D (Duplicate)', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: { url: 'https://api.com' },
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.selectNode('node-1');
            designer.handleKeyboard({ key: 'd', metaKey: true });

            expect(designer.getNodes()).toHaveLength(2);
            expect(designer.getNodes()[1].type).toBe('http');
            expect(designer.getNodes()[1].attributes.url).toBe('https://api.com');
        });

        it('should handle Cmd+Z (Undo)', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            const nodeId = node.id;

            // Delete node
            designer.removeNode(nodeId);
            expect(designer.getNodes()).toHaveLength(0);

            // Undo
            designer.handleKeyboard({ key: 'z', metaKey: true });
            expect(designer.getNodes()).toHaveLength(1);
        });

        it('should handle Cmd+Shift+Z (Redo)', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.removeNode(node.id);
            designer.handleKeyboard({ key: 'z', metaKey: true }); // Undo
            designer.handleKeyboard({ key: 'z', metaKey: true, shiftKey: true }); // Redo

            expect(designer.getNodes()).toHaveLength(0);
        });
    });

    describe('Context Menu (Right Click)', () => {
        it('should show context menu on right click', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);

            const menu = designer.showContextMenu('node-1', { x: 150, y: 200 });

            expect(menu).toBeTruthy();
            expect(menu?.items).toContainEqual(expect.objectContaining({ label: 'Delete' }));
            expect(menu?.items).toContainEqual(expect.objectContaining({ label: 'Duplicate' }));
            expect(menu?.items).toContainEqual(expect.objectContaining({ label: 'Copy' }));
        });

        it('should execute context menu action', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);

            // Show menu
            const menu = designer.showContextMenu('node-1', { x: 150, y: 200 });

            // Execute "Duplicate"
            const duplicateAction = menu?.items.find(i => i.label === 'Duplicate');
            duplicateAction?.action();

            expect(designer.getNodes()).toHaveLength(2);
        });
    });

    describe('Auto-Layout', () => {
        it('should arrange nodes in grid layout', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] },
                { id: 'n3', type: 'def', attributes: {}, body: null, children: [] },
                { id: 'n4', type: 'file', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.autoLayout('grid');

            // Check nodes are arranged in grid
            const allNodes = designer.getNodes();
            expect(allNodes[0].position).toBeTruthy();
            expect(allNodes[1].position).toBeTruthy();
            // Nodes should have different positions
            expect(allNodes[0].position).not.toEqual(allNodes[1].position);
        });

        it('should arrange nodes in vertical flow', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] },
                { id: 'n3', type: 'def', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.autoLayout('vertical');

            const allNodes = designer.getNodes();
            // Vertical: same X, increasing Y
            expect(allNodes[0].position?.x).toBe(allNodes[1].position?.x);
            expect(allNodes[0].position?.y).toBeLessThan(allNodes[1].position!.y);
        });
    });

    describe('Node Connections (Manual)', () => {
        it('should create connection by dragging from output to input', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);

            // Start connection drag from n1
            designer.startConnectionDrag('n1', 'output');

            // Drop on n2
            const connection = designer.endConnectionDrag('n2', 'input');

            expect(connection).toBeTruthy();
            expect(connection?.from).toBe('n1');
            expect(connection?.to).toBe('n2');
        });

        it('should prevent circular connections', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);

            // Create n1 → n2
            designer.createManualConnection('n1', 'n2');

            // Try to create n2 → n1 (circular!)
            expect(() => {
                designer.createManualConnection('n2', 'n1');
            }).toThrow('Circular connection detected');
        });

        it('should delete connection', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.createManualConnection('n1', 'n2');

            const connections = designer.getManualConnections();
            expect(connections).toHaveLength(1);

            // Delete connection
            designer.deleteConnection(connections[0].id);

            expect(designer.getManualConnections()).toHaveLength(0);
        });

        it('should update XML when manual connections change execution order', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] },
                { id: 'n2', type: 'def', attributes: {}, body: null, children: [] },
                { id: 'n3', type: 'xpath', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(nodes);

            // Create connections: n1 → n3 → n2 (different from array order)
            designer.createManualConnection('n1', 'n3');
            designer.createManualConnection('n3', 'n2');

            // Get execution order from connections
            const executionOrder = designer.getExecutionOrder();

            expect(executionOrder).toEqual(['n1', 'n3', 'n2']);
        });
    });

    describe('Minimap', () => {
        it('should show minimap for large canvas', () => {
            const nodes: VisualNode[] = Array.from({ length: 20 }, (_, i) => ({
                id: `node-${i}`,
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100 + (i % 5) * 300, y: 100 + Math.floor(i / 5) * 200 }
            }));

            designer.loadNodes(nodes);
            designer.showMinimap(true);

            expect(designer.isMinimapVisible()).toBe(true);
        });

        it('should navigate canvas via minimap click', () => {
            designer.showMinimap(true);

            // Click minimap at position (representing far away canvas area)
            designer.handleMinimapClick({ x: 500, y: 400 });

            const pan = designer.getPan();
            expect(pan.x).not.toBe(0);
            expect(pan.y).not.toBe(0);
        });
    });
});

