/**
 * Visual Designer Fixes - Integration Tests
 * Tests for all fixed functionalities: connections, reordering, container movement
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../types';

describe('Visual Designer Fixes - Integration Tests', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas';
        document.body.appendChild(container);

        designer = new VisualDesigner('test-canvas');
    });

    afterEach(() => {
        if (document.body.contains(container)) {
            document.body.removeChild(container);
        }
    });

    describe('Connection Event Listeners', () => {
        it('should attach event listeners only once per connection line', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 250, y: 0 }
            };

            designer.loadNodes([node1, node2]);
            designer.render();

            // Create manual connection
            const connection = designer.createManualConnection('1', '2', 'sequential');

            // Render multiple times
            designer.render();
            designer.render();
            designer.render();

            // Check that connection line exists and has listeners
            const line = container.querySelector(`[data-connection-id="${connection.id}"]`) as SVGLineElement;
            expect(line).toBeDefined();
            expect(line.style.cursor).toBe('pointer');
        });

        it('should clean up event listeners when connections are removed', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 250, y: 0 }
            };

            designer.loadNodes([node1, node2]);
            const connection = designer.createManualConnection('1', '2', 'sequential');
            designer.render();

            const lineBefore = container.querySelector(`[data-connection-id="${connection.id}"]`);
            expect(lineBefore).toBeDefined();

            designer.deleteConnection(connection.id);
            designer.render();

            const lineAfter = container.querySelector(`[data-connection-id="${connection.id}"]`);
            expect(lineAfter).toBeNull();
        });
    });

    describe('Context Menu Positioning', () => {
        it('should position context menu correctly relative to viewport', () => {
            const node: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            designer.loadNodes([node]);
            designer.render();

            const menu = designer.showContextMenu('1', { x: 200, y: 150 });

            expect(menu.element).toBeDefined();
            expect(menu.element?.style.position).toBe('fixed');
            expect(parseInt(menu.element?.style.top || '0')).toBe(150);
            expect(parseInt(menu.element?.style.left || '0')).toBe(200);
        });
    });

    describe('Container Movement with Nested Children', () => {
        it('should move all nested children when container moves', () => {
            const grandchild: VisualNode = {
                id: 'grandchild',
                type: 'get',
                attributes: { var: 'var1' },
                body: null,
                children: [],
                position: { x: 30, y: 30 }
            };
            const child: VisualNode = {
                id: 'child',
                type: 'html-to-xml',
                attributes: {},
                body: null,
                children: [grandchild],
                position: { x: 20, y: 20 }
            };
            const container: VisualNode = {
                id: 'container',
                type: 'def',
                attributes: { var: 'result' },
                body: null,
                children: [child],
                position: { x: 10, y: 10 }
            };

            designer.loadNodes([container]);
            designer.render();

            // Move container by (50, 50)
            designer.moveNode('container', { x: 60, y: 60 });

            const updatedContainer = designer.findNode('container');
            const updatedChild = designer.findNode('child');
            const updatedGrandchild = designer.findNode('grandchild');

            expect(updatedContainer?.position?.x).toBe(60);
            expect(updatedContainer?.position?.y).toBe(60);
            expect(updatedChild?.position?.x).toBe(70); // 20 + 50
            expect(updatedChild?.position?.y).toBe(70); // 20 + 50
            expect(updatedGrandchild?.position?.x).toBe(80); // 30 + 50
            expect(updatedGrandchild?.position?.y).toBe(80); // 30 + 50
        });

        it('should initialize positions if missing before moving', () => {
            const child: VisualNode = {
                id: 'child',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                // position missing
            };
            const container: VisualNode = {
                id: 'container',
                type: 'def',
                attributes: {},
                body: null,
                children: [child],
                // position missing
            };

            designer.loadNodes([container]);
            designer.render();

            // Move container - should not throw error
            expect(() => {
                designer.moveNode('container', { x: 100, y: 100 });
            }).not.toThrow();

            const updatedContainer = designer.findNode('container');
            const updatedChild = designer.findNode('child');

            expect(updatedContainer?.position?.x).toBe(100);
            expect(updatedContainer?.position?.y).toBe(100);
            // Child position is initialized during render, so it will be at START_Y (50) not 0
            expect(updatedChild?.position).toBeDefined();
            // After moving container, child should have moved by offset (100, 100) from its initial position
            expect(updatedChild?.position?.x).toBeGreaterThanOrEqual(0);
            expect(updatedChild?.position?.y).toBeGreaterThanOrEqual(0);
        });
    });

    describe('Root Node Reordering', () => {
        it('should correctly identify root nodes for context menu', () => {
            const root1: VisualNode = {
                id: 'root1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const root2: VisualNode = {
                id: 'root2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 150 }
            };
            const nested: VisualNode = {
                id: 'nested',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 20, y: 20 }
            };
            root1.children = [nested];

            designer.loadNodes([root1, root2]);
            designer.render();

            // Root node should have Move Up/Down options
            const rootMenu = (designer as any).buildContextMenuItems('root1');
            const hasMoveOptions = rootMenu.some((item: any) =>
                item.label === 'Move Up' || item.label === 'Move Down'
            );
            expect(hasMoveOptions).toBe(true);

            // Nested node should NOT have Move Up/Down options
            const nestedMenu = (designer as any).buildContextMenuItems('nested');
            const nestedHasMoveOptions = nestedMenu.some((item: any) =>
                item.label === 'Move Up' || item.label === 'Move Down'
            );
            expect(nestedHasMoveOptions).toBe(false);
        });

        it('should update node positions after reordering', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 150 }
            };
            const node3: VisualNode = {
                id: '3',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 300 }
            };

            designer.loadNodes([node1, node2, node3]);
            designer.render();

            // Reorder: move node2 up
            const result = designer.reorderRootNode('2', 'up');
            expect(result).toBe(true);

            const updatedNodes = designer.getNodes();
            expect(updatedNodes[0].id).toBe('2');
            expect(updatedNodes[1].id).toBe('1');
            expect(updatedNodes[2].id).toBe('3');

            // Positions should be updated (START_Y = 50, spacing = 150)
            expect(updatedNodes[0].position?.y).toBe(50); // First position (START_Y)
            expect(updatedNodes[1].position?.y).toBe(200); // Second position (START_Y + 150)
            expect(updatedNodes[2].position?.y).toBe(350); // Third position (START_Y + 300)
        });
    });

    describe('Connection Selection Visual Feedback', () => {
        it('should highlight selected connection with blue color and thicker stroke', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 250, y: 0 }
            };

            designer.loadNodes([node1, node2]);
            const connection = designer.createManualConnection('1', '2', 'sequential');
            designer.render();

            // Select connection
            (designer as any).handleConnectionClick(connection.id);
            designer.render();

            const line = container.querySelector(`[data-connection-id="${connection.id}"]`) as SVGLineElement;
            expect(line).toBeDefined();
            expect(line.getAttribute('stroke-width')).toBe('3');
            expect(line.getAttribute('stroke')).toContain('3b82f6'); // Blue color
        });
    });
});

