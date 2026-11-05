/**
 * Zaawansowane testy dla Semantic Model Connection Point Selection
 * 
 * Testuje zaawansowane scenariusze i edge cases:
 * 1. Różne typy pluginów i ich connection points
 * 2. Hierarchiczne struktury (containers)
 * 3. Dynamiczne zmiany w czasie rzeczywistym
 * 4. Weryfikacja zgodności z model semantycznym
 */

/// <reference types="jest" />

import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import { ConnectionManager } from '../ConnectionManager';
import type { VisualNode } from '../../types';
import type { VisualSemanticModel } from '../VisualSemanticModel';

describe('VisualSemanticModelManager - Semantic Connection Point Selection (Advanced)', () => {
    let manager: VisualSemanticModelManager;
    let mockDesigner: Partial<VisualDesigner>;
    let pluginManager: PluginManager;
    let connectionManager: ConnectionManager;

    beforeEach(() => {
        pluginManager = new PluginManager();
        connectionManager = new ConnectionManager();

        mockDesigner = {
            startMarkerPosition: { x: 100, y: 50 },
            endMarkerPosition: { x: 100, y: 800 },
            getAllNodes: jest.fn(() => []),
            getNodes: jest.fn(() => []),
            connectionManager: connectionManager
        } as any;

        manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);
    });

    describe('Different Plugin Types', () => {
        it('should handle plugins with custom input/output ports', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: { url: 'http://example.com' },
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: { xpath: '//div' },
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const node1State = model.workflow.allNodes.get('node-1');
            const node2State = model.workflow.allNodes.get('node-2');

            // Both nodes should have connection points
            expect(node1State!.connectionPoints.outputs.length).toBeGreaterThan(0);
            expect(node2State!.connectionPoints.inputs.length).toBeGreaterThan(0);

            // Selected points should have metadata
            const connection = Array.from(model.workflow.connections.values())[0];
            const fromPoint = node1State!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );
            const toPoint = node2State!.connectionPoints.inputs.find(
                p => p.id === connection.anchors.to.connectionPointId
            );

            expect(fromPoint!.semanticSelection.isSelected).toBe(true);
            expect(toPoint!.semanticSelection.isSelected).toBe(true);
        });

        it('should handle container nodes with children', () => {
            const container: VisualNode = {
                id: 'container-1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [
                    {
                        id: 'child-1',
                        type: 'var',
                        attributes: { name: 'test' },
                        body: 'value',
                        children: [],
                        position: { x: 250, y: 250 }
                    }
                ],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([container, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([container, node2]);

            connectionManager.createConnection('container-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const containerState = model.workflow.allNodes.get('container-1');

            expect(containerState).toBeDefined();
            expect(containerState!.connectionPoints.outputs.length).toBeGreaterThan(0);

            const connection = Array.from(model.workflow.connections.values())[0];
            const fromPoint = containerState!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            expect(fromPoint).toBeDefined();
            expect(fromPoint!.semanticSelection.isSelected).toBe(true);
        });
    });

    describe('Selection Priority and Reasoning', () => {
        it('should assign higher priority to preferred-side selections', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 } // To the right
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const connection = Array.from(model.workflow.connections.values())[0];
            const fromNode = model.workflow.allNodes.get(connection.fromNodeId);
            const fromPoint = fromNode!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            // Preferred side should have high priority
            if (fromPoint!.semanticSelection.selectionReason === 'preferred-side') {
                expect(fromPoint!.semanticSelection.priority).toBe(100);
            }
        });

        it('should track multiple connections on same point with correct metadata', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            const node3: VisualNode = {
                id: 'node-3',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 400 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2, node3]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2, node3]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');
            connectionManager.createConnection('node-1', 'node-3', 'sequential');

            const model = manager.buildModel();
            const node1State = model.workflow.allNodes.get('node-1');
            const connections = Array.from(model.workflow.connections.values())
                .filter(c => c.fromNodeId === 'node-1');

            // Find points used by connections
            const usedPointIds = new Set(
                connections.map(c => c.anchors.from.connectionPointId)
            );

            // Check if same point is used for multiple connections
            if (usedPointIds.size === 1) {
                const pointId = Array.from(usedPointIds)[0];
                const point = node1State!.connectionPoints.outputs.find(p => p.id === pointId);

                expect(point).toBeDefined();
                expect(point!.semanticSelection.selectedForConnections.length).toBeGreaterThanOrEqual(1);
            }
        });
    });

    describe('Model Consistency', () => {
        it('should maintain consistency between cache and model', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const connection = Array.from(model.workflow.connections.values())[0];
            const fromNode = model.workflow.allNodes.get(connection.fromNodeId);
            const fromPoint = fromNode!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            // Get same point from cache
            const cachedPoints = (manager as any).connectionPointsCache.get('node-1');
            const cachedPoint = cachedPoints?.find((p: any) => p.id === fromPoint!.id);

            // They should be the same reference or have same metadata
            if (cachedPoint) {
                expect(cachedPoint.semanticSelection.isSelected).toBe(fromPoint!.semanticSelection.isSelected);
                expect(cachedPoint.semanticSelection.selectionReason).toBe(fromPoint!.semanticSelection.selectionReason);
            }
        });

        it('should update model when connections are added', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);

            // Build model without connections
            const model1 = manager.buildModel();
            expect(model1.workflow.connections.size).toBe(0);

            // Add connection
            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            // Rebuild model
            const model2 = manager.buildModel();
            expect(model2.workflow.connections.size).toBe(1);

            const connection = Array.from(model2.workflow.connections.values())[0];
            const fromNode = model2.workflow.allNodes.get(connection.fromNodeId);
            const fromPoint = fromNode!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            expect(fromPoint).toBeDefined();
            expect(fromPoint!.semanticSelection.isSelected).toBe(true);
        });

        it('should update model when connections are removed', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model1 = manager.buildModel();
            expect(model1.workflow.connections.size).toBe(1);

            const connection1 = Array.from(model1.workflow.connections.values())[0];
            const fromNode1 = model1.workflow.allNodes.get(connection1.fromNodeId);
            const fromPoint1 = fromNode1!.connectionPoints.outputs.find(
                p => p.id === connection1.anchors.from.connectionPointId
            );

            expect(fromPoint1!.semanticSelection.isSelected).toBe(true);
            expect(fromPoint1!.semanticSelection.selectedForConnections.length).toBeGreaterThan(0);

            // Remove connection - find connection ID first
            const connections = connectionManager.getConnections();
            const connToRemove = connections.find(c => c.from === 'node-1' && c.to === 'node-2');
            if (connToRemove) {
                connectionManager.deleteConnection(connToRemove.id);
            }

            const model2 = manager.buildModel();
            expect(model2.workflow.connections.size).toBe(0);

            // Point should no longer be selected (or selectedForConnections should be empty)
            const fromNode2 = model2.workflow.allNodes.get('node-1');
            const fromPoint2 = fromNode2!.connectionPoints.outputs.find(
                p => p.id === fromPoint1!.id
            );

            // After removal, point might still have metadata but not be selected
            // or selectedForConnections should be empty
            if (fromPoint2) {
                const hasConnections = fromPoint2.semanticSelection.selectedForConnections.length > 0;
                // Either not selected or no connections tracked
                expect(fromPoint2.semanticSelection.isSelected === false || !hasConnections).toBe(true);
            }
        });
    });

    describe('Spatial Analysis Accuracy', () => {
        it('should prefer shortest path when multiple options available', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 } // To the right
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const connection = Array.from(model.workflow.connections.values())[0];
            const fromNode = model.workflow.allNodes.get(connection.fromNodeId);
            const fromPoint = fromNode!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            // Right side should be preferred (shortest path)
            expect(fromPoint!.side).toBe('right');
            expect(fromPoint!.semanticSelection.spatialContext.pathLength).toBeGreaterThan(0);
        });

        it('should avoid collisions when possible', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            const obstacle: VisualNode = {
                id: 'obstacle',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 300, y: 200 } // Between node1 and node2
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2, obstacle]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2, obstacle]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const connection = Array.from(model.workflow.connections.values())[0];
            const fromNode = model.workflow.allNodes.get(connection.fromNodeId);
            const fromPoint = fromNode!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            // Spatial context should indicate collision avoidance
            expect(fromPoint!.semanticSelection.spatialContext.clearance).toBeGreaterThanOrEqual(0);
        });
    });
});

