/**
 * Detailed Connection Rendering Tests
 * 
 * Comprehensive tests for every aspect of connection rendering:
 * - Connection point precision for each side (top, bottom, left, right)
 * - Arrow positioning for different connection types
 * - Line routing for various node arrangements
 * - Collision detection and avoidance
 * - Model synchronization
 */

import { describe, test, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import type { VisualNode, Plugin } from '../../types';

class MockPluginManager {
    private plugins: Map<string, Plugin> = new Map();

    findPlugin(type: string): Plugin | null {
        return this.plugins.get(type) || null;
    }

    getPlugins(): Plugin[] {
        return Array.from(this.plugins.values());
    }

    addPlugin(plugin: Plugin): void {
        this.plugins.set(plugin.id, plugin);
    }
}

describe('Connection Rendering - Detailed Tests', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-detailed';
        container.style.width = '2000px';
        container.style.height = '2000px';
        document.body.appendChild(container);

        pluginManager = new MockPluginManager() as any;

        // Add test plugins
        pluginManager.addPlugin({
            id: 'http',
            name: 'HTTP',
            category: 'http',
            icon: 'fa-globe',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);

        pluginManager.addPlugin({
            id: 'file',
            name: 'File',
            category: 'file',
            icon: 'fa-file',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);

        designer = new VisualDesigner('test-canvas-detailed');
        designer.setPluginManager(pluginManager as any);
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Connection Points - All Sides', () => {
        test('top side connection point should be at center of top edge', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            const point = (designer as any).getConnectionPointFromModel('node1', 'top', 'input');
            expect(point).toBeTruthy();
            
            if (point) {
                // Should be at center X, top Y
                expect(point.x).toBeCloseTo(node.position!.x + 200 / 2, 1);
                expect(point.y).toBeCloseTo(node.position!.y, 1);
            }
        });

        test('bottom side connection point should be at center of bottom edge', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            const point = (designer as any).getConnectionPointFromModel('node1', 'bottom', 'output');
            expect(point).toBeTruthy();
            
            if (point) {
                // Should be at center X, bottom Y
                expect(point.x).toBeCloseTo(node.position!.x + 200 / 2, 1);
                expect(point.y).toBeCloseTo(node.position!.y + 100, 1);
            }
        });

        test('left side connection point should be at center of left edge', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            const point = (designer as any).getConnectionPointFromModel('node1', 'left', 'input');
            expect(point).toBeTruthy();
            
            if (point) {
                // Should be at left X, center Y
                expect(point.x).toBeCloseTo(node.position!.x, 1);
                expect(point.y).toBeCloseTo(node.position!.y + 100 / 2, 1);
            }
        });

        test('right side connection point should be at center of right edge', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            const point = (designer as any).getConnectionPointFromModel('node1', 'right', 'output');
            expect(point).toBeTruthy();
            
            if (point) {
                // Should be at right X, center Y
                expect(point.x).toBeCloseTo(node.position!.x + 200, 1);
                expect(point.y).toBeCloseTo(node.position!.y + 100 / 2, 1);
            }
        });
    });

    describe('Arrow Positioning - Different Scenarios', () => {
        test('arrow should be on connection point for horizontal connection', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 200 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                position: { x: 400, y: 200 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.connectNodes('node1', 'node2', 'sequential');
            designer.render();

            // Get connection point for node2 (left side, input)
            const connectionPoint = (designer as any).getConnectionPointFromModel('node2', 'left', 'input');
            expect(connectionPoint).toBeTruthy();

            if (connectionPoint) {
                // Get rendered connection
                const connections = designer.getConnections();
                const conn = connections.find(c => c.from === 'node1' && c.to === 'node2');
                expect(conn).toBeTruthy();

                // Check if arrow is rendered at connection point
                // This is verified by checking the path ends at connection point
                const anchor1 = (designer as any).getConnectionAnchor('node1');
                const anchor2 = (designer as any).getConnectionAnchor('node2');
                const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

                // Last point should be at connection point
                const lastPoint = geometry.points[geometry.points.length - 1];
                const distance = Math.sqrt(
                    Math.pow(lastPoint.x - connectionPoint.x, 2) +
                    Math.pow(lastPoint.y - connectionPoint.y, 2)
                );

                // Should be very close (within 2px)
                expect(distance).toBeLessThan(2);
            }
        });

        test('arrow should be on connection point for vertical connection', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 200, y: 100 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                position: { x: 200, y: 300 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.connectNodes('node1', 'node2', 'sequential');
            designer.render();

            const connectionPoint = (designer as any).getConnectionPointFromModel('node2', 'top', 'input');
            expect(connectionPoint).toBeTruthy();

            if (connectionPoint) {
                const anchor1 = (designer as any).getConnectionAnchor('node1');
                const anchor2 = (designer as any).getConnectionAnchor('node2');
                const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

                const lastPoint = geometry.points[geometry.points.length - 1];
                const distance = Math.sqrt(
                    Math.pow(lastPoint.x - connectionPoint.x, 2) +
                    Math.pow(lastPoint.y - connectionPoint.y, 2)
                );

                expect(distance).toBeLessThan(2);
            }
        });

        test('arrow should be on connection point for diagonal connection', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                position: { x: 400, y: 300 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.connectNodes('node1', 'node2', 'sequential');
            designer.render();

            // Determine entry side based on position
            const connectionPoint = (designer as any).getConnectionPointFromModel('node2', 'left', 'input');
            expect(connectionPoint).toBeTruthy();

            if (connectionPoint) {
                const anchor1 = (designer as any).getConnectionAnchor('node1');
                const anchor2 = (designer as any).getConnectionAnchor('node2');
                const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

                const lastPoint = geometry.points[geometry.points.length - 1];
                const distance = Math.sqrt(
                    Math.pow(lastPoint.x - connectionPoint.x, 2) +
                    Math.pow(lastPoint.y - connectionPoint.y, 2)
                );

                // For diagonal connections, routing may add intermediate points
                // But final point should still be close to connection point
                expect(distance).toBeLessThan(5);
            }
        });
    });

    describe('Line Routing - Collision Avoidance', () => {
        test('line should route around single obstacle node', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 200 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                position: { x: 500, y: 200 },
                children: []
            };

            const obstacle: VisualNode = {
                id: 'obstacle',
                type: 'http',
                attributes: { url: 'https://obstacle.com' },
                position: { x: 300, y: 150 }, // Between node1 and node2, slightly above
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.addNode(obstacle);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.connectNodes('node1', 'node2', 'sequential');
            designer.render();

            const anchor1 = (designer as any).getConnectionAnchor('node1');
            const anchor2 = (designer as any).getConnectionAnchor('node2');
            const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

            // Check intersections
            const intersections = (designer as any).countNodeIntersections(
                geometry.points,
                designer.getNodeById('node1'),
                designer.getNodeById('node2')
            );

            expect(intersections).toBe(0);
        });

        test('line should route around multiple obstacle nodes', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 200 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                position: { x: 600, y: 200 },
                children: []
            };

            const obstacle1: VisualNode = {
                id: 'obstacle1',
                type: 'http',
                attributes: { url: 'https://obstacle1.com' },
                position: { x: 300, y: 150 },
                children: []
            };

            const obstacle2: VisualNode = {
                id: 'obstacle2',
                type: 'http',
                attributes: { url: 'https://obstacle2.com' },
                position: { x: 400, y: 250 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.addNode(obstacle1);
            designer.addNode(obstacle2);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.connectNodes('node1', 'node2', 'sequential');
            designer.render();

            const anchor1 = (designer as any).getConnectionAnchor('node1');
            const anchor2 = (designer as any).getConnectionAnchor('node2');
            const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

            const intersections = (designer as any).countNodeIntersections(
                geometry.points,
                designer.getNodeById('node1'),
                designer.getNodeById('node2')
            );

            expect(intersections).toBe(0);
        });

        test('line should minimize crossings with other lines', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                position: { x: 400, y: 200 },
                children: []
            };

            const node3: VisualNode = {
                id: 'node3',
                type: 'http',
                attributes: { url: 'https://example3.com' },
                position: { x: 100, y: 300 },
                children: []
            };

            const node4: VisualNode = {
                id: 'node4',
                type: 'file',
                attributes: { path: '/tmp/test4' },
                position: { x: 400, y: 400 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.addNode(node3);
            designer.addNode(node4);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            // Create first connection
            designer.connectNodes('node1', 'node2', 'sequential');
            designer.render();

            // Create second connection (might cross first)
            designer.connectNodes('node3', 'node4', 'sequential');
            designer.render();

            const connections = designer.getConnections();
            expect(connections.length).toBeGreaterThanOrEqual(2);

            // Get geometry for second connection
            const anchor3 = (designer as any).getConnectionAnchor('node3');
            const anchor4 = (designer as any).getConnectionAnchor('node4');
            const geometry = (designer as any).computeConnectionGeometry(anchor3, anchor4, 'sequential');

            // Count crossings with existing connections
            const existingConnections = connections.filter(c => 
                c.from !== 'node3' && c.to !== 'node4'
            );
            const crossings = (designer as any).countPathCrossings(
                geometry.points,
                existingConnections
            );

            // Should minimize crossings (ideally 0)
            expect(crossings).toBeLessThanOrEqual(1); // Allow 1 crossing if unavoidable
        });
    });

    describe('Model Synchronization', () => {
        test('connection points should update when node moves', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (!modelManager) {
                return; // Skip if model manager not available
            }

            modelManager.buildModel();

            // Get initial connection point
            const point1 = modelManager.getConnectionPointForSide('node1', 'right', 'output');
            expect(point1).toBeTruthy();

            const absolute1 = point1 ? modelManager.getConnectionPointAbsolutePosition('node1', point1.id) : null;
            expect(absolute1).toBeTruthy();

            // Move node
            (designer as any).moveNode('node1', { x: 200, y: 200 }, { skipRender: false });

            // Rebuild model
            modelManager.buildModel();

            // Get updated connection point
            const point2 = modelManager.getConnectionPointForSide('node1', 'right', 'output');
            expect(point2).toBeTruthy();

            const absolute2 = point2 ? modelManager.getConnectionPointAbsolutePosition('node1', point2.id) : null;
            expect(absolute2).toBeTruthy();

            if (absolute1 && absolute2) {
                // Should be shifted by (100, 100)
                expect(Math.abs(absolute2.x - absolute1.x - 100)).toBeLessThan(1);
                expect(Math.abs(absolute2.y - absolute1.y - 100)).toBeLessThan(1);
            }
        });

        test('model should be built before rendering connections', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                position: { x: 400, y: 100 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (!modelManager) {
                return;
            }

            const buildModelSpy = jest.spyOn(modelManager, 'buildModel');

            designer.connectNodes('node1', 'node2', 'sequential');
            (designer as any).renderConnections();

            expect(buildModelSpy).toHaveBeenCalled();
        });
    });

    describe('Layout Integration', () => {
        test('connection points should be correct after auto-layout', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            // Apply auto-layout
            (designer as any).applyAutoLayoutToRootNodes(designer.getNodes());
            designer.render();

            const modelManager = (designer as any).modelManager as VisualSemanticModelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            // Both nodes should have positions after auto-layout
            const node1Pos = designer.getNodeById('node1')?.position;
            const node2Pos = designer.getNodeById('node2')?.position;

            expect(node1Pos).toBeTruthy();
            expect(node2Pos).toBeTruthy();

            // Connection points should be calculated correctly
            const point1 = (designer as any).getConnectionPointFromModel('node1', 'bottom', 'output');
            const point2 = (designer as any).getConnectionPointFromModel('node2', 'top', 'input');

            expect(point1).toBeTruthy();
            expect(point2).toBeTruthy();

            if (node1Pos && point1) {
                // Point1 should be at bottom center of node1
                expect(point1.x).toBeCloseTo(node1Pos.x + 200 / 2, 1);
                expect(point1.y).toBeCloseTo(node1Pos.y + 100, 1);
            }

            if (node2Pos && point2) {
                // Point2 should be at top center of node2
                expect(point2.x).toBeCloseTo(node2Pos.x + 200 / 2, 1);
                expect(point2.y).toBeCloseTo(node2Pos.y, 1);
            }
        });

        test('connections should render correctly after layout change', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            // Connect nodes
            designer.connectNodes('node1', 'node2', 'sequential');
            designer.render();

            // Apply auto-layout (should move nodes)
            (designer as any).applyAutoLayoutToRootNodes(designer.getNodes());
            designer.render();

            // Connection should still be valid
            const connections = designer.getConnections();
            const conn = connections.find(c => c.from === 'node1' && c.to === 'node2');
            expect(conn).toBeTruthy();

            // Connection points should be updated
            const anchor1 = (designer as any).getConnectionAnchor('node1');
            const anchor2 = (designer as any).getConnectionAnchor('node2');
            expect(anchor1).toBeTruthy();
            expect(anchor2).toBeTruthy();

            // Geometry should be valid
            const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');
            expect(geometry.points.length).toBeGreaterThan(1);
        });
    });
});










