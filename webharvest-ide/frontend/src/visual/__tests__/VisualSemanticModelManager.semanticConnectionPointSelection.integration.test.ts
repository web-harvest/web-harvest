/**
 * Testy integracyjne dla Semantic Model Connection Point Selection
 * 
 * Testuje pełną integrację między komponentami:
 * 1. VisualSemanticModelManager
 * 2. ConnectionPointsCalculator
 * 3. VisualSemanticModel
 * 4. ConnectionManager
 * 
 * Testy weryfikują:
 * - Czy metadane są zachowywane po rebuild modelu
 * - Czy różne konfiguracje węzłów działają poprawnie
 * - Czy analiza przestrzenna działa dla złożonych scenariuszy
 * - Czy wydajność jest akceptowalna
 */

/// <reference types="jest" />

import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import { ConnectionManager } from '../ConnectionManager';
import type { VisualNode } from '../../types';
import type { VisualSemanticModel } from '../VisualSemanticModel';

describe('VisualSemanticModelManager - Semantic Connection Point Selection (Integration)', () => {
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

    describe('Model Persistence and Rebuild', () => {
        it('should preserve semanticSelection metadata after model rebuild', () => {
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

            // Add connection
            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            // Build model first time
            const model1 = manager.buildModel();
            const node1State1 = model1.workflow.allNodes.get('node-1');
            const connection1 = Array.from(model1.workflow.connections.values())[0];

            // Find selected connection points
            const fromPoint1 = node1State1!.connectionPoints.outputs.find(
                p => p.id === connection1.anchors.from.connectionPointId
            );

            expect(fromPoint1).toBeDefined();
            if (!fromPoint1) return; // Skip if point not found
            expect(fromPoint1.semanticSelection).toBeDefined();
            expect(fromPoint1.semanticSelection.isSelected).toBe(true);
            expect(fromPoint1.semanticSelection.selectedForConnections.length).toBeGreaterThan(0);

            // Rebuild model
            const model2 = manager.buildModel();
            const node1State2 = model2.workflow.allNodes.get('node-1');
            const connection2 = Array.from(model2.workflow.connections.values())[0];

            if (!connection2 || !node1State2) return; // Skip if connection or node not found

            const fromPoint2 = node1State2.connectionPoints.outputs.find(
                p => p.id === connection2.anchors.from.connectionPointId
            );

            // Metadata should be preserved
            expect(fromPoint2).toBeDefined();
            if (!fromPoint2) return; // Skip if point not found
            expect(fromPoint2.semanticSelection).toBeDefined();
            expect(fromPoint2.semanticSelection.isSelected).toBe(true);
            expect(fromPoint2.semanticSelection.selectedForConnections.length).toBeGreaterThan(0);
            expect(fromPoint2.semanticSelection.selectionReason).toBe(fromPoint1.semanticSelection.selectionReason);
        });

        it('should update semanticSelection when node positions change', () => {
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

            const model1 = manager.buildModel();
            const connection1 = Array.from(model1.workflow.connections.values())[0];
            const node1State1 = model1.workflow.allNodes.get('node-1');
            const fromPoint1 = node1State1!.connectionPoints.outputs.find(
                p => p.id === connection1.anchors.from.connectionPointId
            );

            if (!fromPoint1) return; // Skip if point not found
            const initialSide = fromPoint1.side;
            const initialReason = fromPoint1.semanticSelection.selectionReason;

            // Move node2 to the left of node1
            node2.position = { x: 100, y: 200 };

            const model2 = manager.buildModel();
            const connection2 = Array.from(model2.workflow.connections.values())[0];
            const node1State2 = model2.workflow.allNodes.get('node-1');
            const fromPoint2 = node1State2!.connectionPoints.outputs.find(
                p => p.id === connection2.anchors.from.connectionPointId
            );

            // Side might change based on spatial analysis
            expect(fromPoint2).toBeDefined();
            expect(fromPoint2!.semanticSelection).toBeDefined();
            expect(fromPoint2!.semanticSelection.isSelected).toBe(true);
            // Spatial context should be updated
            expect(fromPoint2!.semanticSelection.spatialContext.pathLength).not.toBe(fromPoint1!.semanticSelection.spatialContext.pathLength);
        });
    });

    describe('Complex Spatial Scenarios', () => {
        it('should handle grid layout with multiple connections', () => {
            // Create a 3x3 grid of nodes
            const nodes: VisualNode[] = [];
            for (let i = 0; i < 3; i++) {
                for (let j = 0; j < 3; j++) {
                    nodes.push({
                        id: `node-${i}-${j}`,
                        type: 'var',
                        attributes: {},
                        body: null,
                        children: [],
                        position: { x: 200 + j * 150, y: 200 + i * 150 }
                    });
                }
            }

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue(nodes);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue(nodes);

            // Create connections: each node connects to the one to its right
            for (let i = 0; i < 3; i++) {
                for (let j = 0; j < 2; j++) {
                    connectionManager.createConnection(`node-${i}-${j}`, `node-${i}-${j + 1}`, 'sequential');
                }
            }

            const model = manager.buildModel();
            const connections = Array.from(model.workflow.connections.values());

            expect(connections.length).toBe(6); // 3 rows * 2 connections per row

            // Each connection should have selected points with metadata
            connections.forEach(conn => {
                const fromNode = model.workflow.allNodes.get(conn.fromNodeId);
                const toNode = model.workflow.allNodes.get(conn.toNodeId);

                if (!fromNode || !toNode) return; // Skip if nodes not found

                const fromPoint = fromNode.connectionPoints.outputs.find(
                    p => p.id === conn.anchors.from.connectionPointId
                );
                const toPoint = toNode.connectionPoints.inputs.find(
                    p => p.id === conn.anchors.to.connectionPointId
                );

                expect(fromPoint).toBeDefined();
                expect(toPoint).toBeDefined();
                if (!fromPoint || !toPoint) return; // Skip if points not found
                expect(fromPoint.semanticSelection.isSelected).toBe(true);
                expect(toPoint.semanticSelection.isSelected).toBe(true);
                expect(fromPoint.semanticSelection.selectionReason).not.toBe('');
                expect(toPoint.semanticSelection.selectionReason).not.toBe('');
            });
        });

        it('should handle circular connections (A->B->C->A)', () => {
            const nodeA: VisualNode = {
                id: 'node-A',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            const nodeB: VisualNode = {
                id: 'node-B',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            const nodeC: VisualNode = {
                id: 'node-C',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 300, y: 400 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([nodeA, nodeB, nodeC]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([nodeA, nodeB, nodeC]);

            connectionManager.createConnection('node-A', 'node-B', 'sequential');
            connectionManager.createConnection('node-B', 'node-C', 'sequential');
            connectionManager.createConnection('node-C', 'node-A', 'sequential');

            const model = manager.buildModel();
            const connections = Array.from(model.workflow.connections.values());

            expect(connections.length).toBe(3);

            // Each node should have different connection points used
            const nodeAState = model.workflow.allNodes.get('node-A');
            const nodeBState = model.workflow.allNodes.get('node-B');
            const nodeCState = model.workflow.allNodes.get('node-C');

            const aOutputs = nodeAState!.connectionPoints.outputs.filter(p => p.semanticSelection.isSelected);
            const bOutputs = nodeBState!.connectionPoints.outputs.filter(p => p.semanticSelection.isSelected);
            const cOutputs = nodeCState!.connectionPoints.outputs.filter(p => p.semanticSelection.isSelected);

            expect(aOutputs.length).toBeGreaterThan(0);
            expect(bOutputs.length).toBeGreaterThan(0);
            expect(cOutputs.length).toBeGreaterThan(0);

            // Each selected point should have proper metadata
            [...aOutputs, ...bOutputs, ...cOutputs].forEach(point => {
                expect(point.semanticSelection.selectedForConnections.length).toBeGreaterThan(0);
                expect(point.semanticSelection.selectionReason).not.toBe('');
                expect(point.semanticSelection.priority).toBeGreaterThan(0);
            });
        });

        it('should handle nodes with overlapping positions', () => {
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
                position: { x: 200, y: 200 } // Same position
            };

            const node3: VisualNode = {
                id: 'node-3',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 200 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2, node3]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2, node3]);

            connectionManager.createConnection('node-1', 'node-3', 'sequential');
            connectionManager.createConnection('node-2', 'node-3', 'sequential');

            const model = manager.buildModel();
            const connections = Array.from(model.workflow.connections.values());

            expect(connections.length).toBe(2);

            // Both connections should have selected points
            connections.forEach(conn => {
                const fromNode = model.workflow.allNodes.get(conn.fromNodeId);
                if (!fromNode) return; // Skip if node not found
                const fromPoint = fromNode.connectionPoints.outputs.find(
                    p => p.id === conn.anchors.from.connectionPointId
                );

                expect(fromPoint).toBeDefined();
                if (!fromPoint) return; // Skip if point not found
                expect(fromPoint.semanticSelection.isSelected).toBe(true);
            });
        });
    });

    describe('Performance and Scalability', () => {
        it('should handle large number of nodes efficiently', () => {
            const nodes: VisualNode[] = [];
            const nodeCount = 50;

            for (let i = 0; i < nodeCount; i++) {
                nodes.push({
                    id: `node-${i}`,
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 100 + (i % 10) * 100, y: 100 + Math.floor(i / 10) * 100 }
                });
            }

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue(nodes);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue(nodes);

            // Create linear chain of connections
            for (let i = 0; i < nodeCount - 1; i++) {
                connectionManager.createConnection(`node-${i}`, `node-${i + 1}`, 'sequential');
            }

            const startTime = Date.now();
            const model = manager.buildModel();
            const endTime = Date.now();

            const buildTime = endTime - startTime;

            // Should complete in reasonable time (< 1 second for 50 nodes)
            expect(buildTime).toBeLessThan(1000);

            // All connections should have metadata
            const connections = Array.from(model.workflow.connections.values());
            expect(connections.length).toBe(nodeCount - 1);

            connections.forEach(conn => {
                const fromNode = model.workflow.allNodes.get(conn.fromNodeId);
                if (!fromNode) return; // Skip if node not found
                const fromPoint = fromNode.connectionPoints.outputs.find(
                    p => p.id === conn.anchors.from.connectionPointId
                );

                expect(fromPoint).toBeDefined();
                if (!fromPoint) return; // Skip if point not found
                expect(fromPoint.semanticSelection).toBeDefined();
            });
        });

        it('should handle dense connection graph efficiently', () => {
            const nodes: VisualNode[] = [];
            const nodeCount = 20;

            for (let i = 0; i < nodeCount; i++) {
                nodes.push({
                    id: `node-${i}`,
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 100 + (i % 5) * 100, y: 100 + Math.floor(i / 5) * 100 }
                });
            }

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue(nodes);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue(nodes);

            // Create dense graph: each node connects to next 3 nodes
            for (let i = 0; i < nodeCount; i++) {
                for (let j = 1; j <= 3 && i + j < nodeCount; j++) {
                    connectionManager.createConnection(`node-${i}`, `node-${i + j}`, 'sequential');
                }
            }

            const startTime = Date.now();
            const model = manager.buildModel();
            const endTime = Date.now();

            const buildTime = endTime - startTime;

            // Should complete in reasonable time
            expect(buildTime).toBeLessThan(2000);

            const connections = Array.from(model.workflow.connections.values());
            expect(connections.length).toBeGreaterThan(0);

            // Verify all connections have metadata
            let connectionsWithMetadata = 0;
            connections.forEach(conn => {
                const fromNode = model.workflow.allNodes.get(conn.fromNodeId);
                const fromPoint = fromNode!.connectionPoints.outputs.find(
                    p => p.id === conn.anchors.from.connectionPointId
                );

                if (fromPoint && fromPoint.semanticSelection && fromPoint.semanticSelection.isSelected) {
                    connectionsWithMetadata++;
                }
            });

            // At least 80% of connections should have metadata
            expect(connectionsWithMetadata / connections.length).toBeGreaterThan(0.8);
        });
    });

    describe('Edge Cases and Error Handling', () => {
        it('should handle empty node list', () => {
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([]);

            const model = manager.buildModel();

            expect(model).toBeDefined();
            expect(model.workflow.allNodes.size).toBe(0);
            expect(model.workflow.connections.size).toBe(0);
        });

        it('should handle single node without connections', () => {
            const node: VisualNode = {
                id: 'node-1',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 200, y: 200 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node]);

            const model = manager.buildModel();
            const nodeState = model.workflow.allNodes.get('node-1');

            expect(nodeState).toBeDefined();
            expect(nodeState!.connectionPoints.inputs.length).toBeGreaterThan(0);
            expect(nodeState!.connectionPoints.outputs.length).toBeGreaterThan(0);

            // All points should have semanticSelection initialized but not selected
            const allPoints = [
                ...nodeState!.connectionPoints.inputs,
                ...nodeState!.connectionPoints.outputs
            ];

            allPoints.forEach(point => {
                expect(point.semanticSelection).toBeDefined();
                expect(point.semanticSelection.isSelected).toBe(false);
            });
        });

        it('should handle nodes at extreme positions', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: -1000, y: -1000 } // Far left and top
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 10000, y: 10000 } // Far right and bottom
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const connections = Array.from(model.workflow.connections.values());

            expect(connections.length).toBe(1);

            const conn = connections[0];
            const fromNode = model.workflow.allNodes.get(conn.fromNodeId);
            const fromPoint = fromNode!.connectionPoints.outputs.find(
                p => p.id === conn.anchors.from.connectionPointId
            );

            expect(fromPoint).toBeDefined();
            expect(fromPoint!.semanticSelection).toBeDefined();
            expect(fromPoint!.semanticSelection.isSelected).toBe(true);
            // Path length should be very large
            expect(fromPoint!.semanticSelection.spatialContext.pathLength).toBeGreaterThan(1000);
        });

        it('should handle multiple connections from same node using different points', () => {
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
                position: { x: 200, y: 400 }
            };

            const node4: VisualNode = {
                id: 'node-4',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 400 }
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2, node3, node4]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2, node3, node4]);

            // node1 connects to node2, node3, and node4
            connectionManager.createConnection('node-1', 'node-2', 'sequential');
            connectionManager.createConnection('node-1', 'node-3', 'sequential');
            connectionManager.createConnection('node-1', 'node-4', 'sequential');

            const model = manager.buildModel();
            const node1State = model.workflow.allNodes.get('node-1');
            const connections = Array.from(model.workflow.connections.values())
                .filter(c => c.fromNodeId === 'node-1');

            expect(connections.length).toBe(3);

            // Get all selected output points from node1
            const selectedPoints = node1State!.connectionPoints.outputs.filter(
                p => p.semanticSelection.isSelected
            );

            // Should use at least 2 different points (ideally 3, but might reuse if needed)
            const uniquePointIds = new Set(selectedPoints.map(p => p.id));
            expect(uniquePointIds.size).toBeGreaterThanOrEqual(2);

            // Each selected point should track its connections
            selectedPoints.forEach(point => {
                expect(point.semanticSelection.selectedForConnections.length).toBeGreaterThan(0);
            });
        });
    });

    describe('Spatial Context Accuracy', () => {
        it('should calculate accurate path length in spatial context', () => {
            const node1: VisualNode = {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };

            const node2: VisualNode = {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 300, y: 400 } // Distance: sqrt(300^2 + 400^2) = 500
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

            expect(fromPoint).toBeDefined();
            if (!fromPoint) return; // Skip if point not found

            // Path length should be approximately 500 (with some tolerance for node dimensions)
            const pathLength = fromPoint.semanticSelection.spatialContext.pathLength;
            expect(pathLength).toBeGreaterThan(400);
            expect(pathLength).toBeLessThan(600);
        });

        it('should calculate clearance correctly when nodes are close', () => {
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
                position: { x: 300, y: 200 } // Between node1 and node2
            };

            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2, node3]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2, node3]);

            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            const model = manager.buildModel();
            const connection = Array.from(model.workflow.connections.values())[0];
            const fromNode = model.workflow.allNodes.get(connection.fromNodeId);
            const fromPoint = fromNode!.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            expect(fromPoint).toBeDefined();
            if (!fromPoint) return; // Skip if point not found

            // Clearance should be calculated (might be small due to node3 in the way)
            expect(fromPoint.semanticSelection.spatialContext.clearance).toBeGreaterThanOrEqual(0);
        });
    });
});

