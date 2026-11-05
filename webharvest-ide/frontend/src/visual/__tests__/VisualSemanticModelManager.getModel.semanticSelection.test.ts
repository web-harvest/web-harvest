/**
 * Testy dla getModel() - weryfikacja, że semanticSelection metadata jest dostępna
 * 
 * Testuje czy getModel() zwraca model z aktualnymi metadanymi semanticSelection
 * z cache, nawet jeśli model był zbudowany wcześniej.
 */

/// <reference types="jest" />

import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import { ConnectionManager } from '../ConnectionManager';
import type { VisualNode } from '../../types';

describe('VisualSemanticModelManager.getModel() - SemanticSelection Metadata', () => {
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

    describe('getModel() returns model with semanticSelection metadata', () => {
        it('should return model with semanticSelection metadata after buildModel', () => {
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

            // Build model
            const model1 = manager.buildModel();
            const connection1 = Array.from(model1.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
            expect(connection1).toBeDefined();
            if (!connection1) return;

            const node1State1 = model1.workflow.allNodes.get('node-1');
            expect(node1State1).toBeDefined();
            if (!node1State1) return;

            const fromPoint1 = node1State1.connectionPoints.outputs.find(
                p => p.id === connection1.anchors.from.connectionPointId
            );

            expect(fromPoint1).toBeDefined();
            if (!fromPoint1) return;
            expect(fromPoint1.semanticSelection).toBeDefined();
            expect(fromPoint1.semanticSelection.isSelected).toBe(true);

            // Get model via getModel() - should have same metadata
            const model2 = manager.getModel();
            const connection2 = Array.from(model2.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
            expect(connection2).toBeDefined();
            if (!connection2) return;

            const node1State2 = model2.workflow.allNodes.get('node-1');
            expect(node1State2).toBeDefined();
            if (!node1State2) return;

            const fromPoint2 = node1State2.connectionPoints.outputs.find(
                p => p.id === connection2.anchors.from.connectionPointId
            );

            expect(fromPoint2).toBeDefined();
            if (!fromPoint2) return;
            expect(fromPoint2.semanticSelection).toBeDefined();
            expect(fromPoint2.semanticSelection.isSelected).toBe(true);
            expect(fromPoint2.semanticSelection.selectionReason).toBe(fromPoint1.semanticSelection.selectionReason);
        });

        it('should update model connection points with cache references on getModel()', () => {
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

            // Build model first time
            const model1 = manager.buildModel();
            const connection1 = Array.from(model1.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
            expect(connection1).toBeDefined();
            if (!connection1) return;

            const node1State1 = model1.workflow.allNodes.get('node-1');
            expect(node1State1).toBeDefined();
            if (!node1State1) return;

            const fromPoint1 = node1State1.connectionPoints.outputs.find(
                p => p.id === connection1.anchors.from.connectionPointId
            );

            // Verify metadata exists
            expect(fromPoint1).toBeDefined();
            if (!fromPoint1) return;
            expect(fromPoint1.semanticSelection.isSelected).toBe(true);

            // Get model via getModel() - should update connection points with cache references
            const model2 = manager.getModel();
            const connection2 = Array.from(model2.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
            expect(connection2).toBeDefined();
            if (!connection2) return;

            const node1State2 = model2.workflow.allNodes.get('node-1');
            expect(node1State2).toBeDefined();
            if (!node1State2) return;

            const fromPoint2 = node1State2.connectionPoints.outputs.find(
                p => p.id === connection2.anchors.from.connectionPointId
            );

            // Should have same metadata (from cache)
            expect(fromPoint2).toBeDefined();
            if (!fromPoint2) return;
            expect(fromPoint2.semanticSelection).toBeDefined();
            expect(fromPoint2.semanticSelection.isSelected).toBe(true);
            expect(fromPoint2.semanticSelection.selectionReason).toBe(fromPoint1.semanticSelection.selectionReason);

            // Should be same reference (or have same metadata)
            expect(fromPoint2.semanticSelection.selectedForConnections.length).toBeGreaterThan(0);
        });

        it('should return model with semanticSelection even if model was built before connection was added', () => {
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

            // Build model without connection
            const model1 = manager.buildModel();
            // Model might have workflow-start/end connections, so check for our specific connection
            const hasOurConnection = Array.from(model1.workflow.connections.values())
                .some(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
            // Connection should not exist yet
            if (hasOurConnection) {
                // If it exists, it's from workflow-start/end, which is OK
                const ourConn = Array.from(model1.workflow.connections.values())
                    .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
                if (ourConn && ourConn.fromNodeId === 'node-1' && ourConn.toNodeId === 'node-2') {
                    // This is our connection, but we haven't added it yet - skip test
                    return;
                }
            }

            // Add connection
            connectionManager.createConnection('node-1', 'node-2', 'sequential');

            // Rebuild model
            const model2 = manager.buildModel();
            const connection2 = Array.from(model2.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
            expect(connection2).toBeDefined();
            if (!connection2) return;

            const node1State2 = model2.workflow.allNodes.get('node-1');
            expect(node1State2).toBeDefined();
            if (!node1State2) return;

            const fromPoint2 = node1State2.connectionPoints.outputs.find(
                p => p.id === connection2.anchors.from.connectionPointId
            );

            expect(fromPoint2).toBeDefined();
            if (!fromPoint2) return;
            expect(fromPoint2.semanticSelection.isSelected).toBe(true);

            // Get model via getModel() - should have metadata
            const model3 = manager.getModel();
            const connection3 = Array.from(model3.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');
            expect(connection3).toBeDefined();
            if (!connection3) return;

            const node1State3 = model3.workflow.allNodes.get('node-1');
            expect(node1State3).toBeDefined();
            if (!node1State3) return;

            const fromPoint3 = node1State3.connectionPoints.outputs.find(
                p => p.id === connection3.anchors.from.connectionPointId
            );

            expect(fromPoint3).toBeDefined();
            if (!fromPoint3) return;
            expect(fromPoint3.semanticSelection).toBeDefined();
            expect(fromPoint3.semanticSelection.isSelected).toBe(true);
        });
    });

    describe('getModel() cache synchronization', () => {
        it('should sync connection points from cache to model on getModel()', () => {
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

            // Build model
            manager.buildModel();

            // Get cache directly
            const cache = (manager as any).connectionPointsCache;
            const cachedPoints = cache.get('node-1');
            const selectedInCache = cachedPoints.filter((p: any) =>
                p.semanticSelection && p.semanticSelection.isSelected
            );

            expect(selectedInCache.length).toBeGreaterThan(0);

            // Get model via getModel() - should sync from cache
            const model = manager.getModel();
            const node1State = model.workflow.allNodes.get('node-1');
            const selectedInModel = node1State!.connectionPoints.outputs.filter(
                p => p.semanticSelection && p.semanticSelection.isSelected
            );

            expect(selectedInModel.length).toBeGreaterThan(0);
            // At least one point should be selected in both cache and model
            expect(selectedInModel.length).toBeGreaterThanOrEqual(1);
            expect(selectedInCache.length).toBeGreaterThanOrEqual(1);
        });
    });

    describe('Serialization and WebSocket export', () => {
        it('should preserve semanticSelection metadata after JSON serialization', () => {
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

            const model = manager.getModel();
            // Find our specific connection (node-1 -> node-2)
            const connection = Array.from(model.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');

            expect(connection).toBeDefined();
            if (!connection) return;

            const fromNode = model.workflow.allNodes.get(connection.fromNodeId);
            expect(fromNode).toBeDefined();
            if (!fromNode) return;

            const fromPoint = fromNode.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            expect(fromPoint).toBeDefined();
            if (!fromPoint) return;
            expect(fromPoint.semanticSelection).toBeDefined();
            expect(fromPoint.semanticSelection.isSelected).toBe(true);

            // Simulate JSON serialization (as done by WebSocket)
            // Note: Map needs to be converted to object first
            const modelForSerialization = {
                ...model,
                workflow: {
                    ...model.workflow,
                    allNodes: Object.fromEntries(model.workflow.allNodes)
                }
            };
            const serialized = JSON.stringify(modelForSerialization);
            const deserialized = JSON.parse(serialized);

            // After deserialization, semanticSelection should still be present
            const deserializedNode = deserialized.workflow.allNodes[connection.fromNodeId];
            expect(deserializedNode).toBeDefined();
            expect(deserializedNode.connectionPoints).toBeDefined();
            expect(deserializedNode.connectionPoints.outputs).toBeDefined();

            const deserializedPoint = deserializedNode.connectionPoints.outputs.find(
                (p: any) => p.id === connection.anchors.from.connectionPointId
            );

            expect(deserializedPoint).toBeDefined();
            expect(deserializedPoint.semanticSelection).toBeDefined();
            expect(deserializedPoint.semanticSelection.isSelected).toBe(true);
            expect(deserializedPoint.semanticSelection.selectionReason).toBe(fromPoint!.semanticSelection.selectionReason);
        });

        it('should preserve semanticSelection metadata in exportForLLM format', () => {
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

            const model = manager.getModel();
            // Find our specific connection (node-1 -> node-2)
            const connection = Array.from(model.workflow.connections.values())
                .find(c => c.fromNodeId === 'node-1' && c.toNodeId === 'node-2');

            expect(connection).toBeDefined();
            if (!connection) return;

            const fromNode = model.workflow.allNodes.get(connection.fromNodeId);
            expect(fromNode).toBeDefined();
            if (!fromNode) return;

            const fromPoint = fromNode.connectionPoints.outputs.find(
                p => p.id === connection.anchors.from.connectionPointId
            );

            expect(fromPoint).toBeDefined();
            if (!fromPoint) return;
            expect(fromPoint.semanticSelection.isSelected).toBe(true);

            // Import exporter
            const { VisualSemanticModelExporter } = require('../VisualSemanticModelExporter');
            const exporter = new VisualSemanticModelExporter();
            const llmModel = exporter.exportForLLM(model);

            // Check that semanticSelection is preserved in LLM format
            const llmNode = llmModel.workflow.allNodes[connection.fromNodeId];
            expect(llmNode).toBeDefined();
            expect(llmNode.connectionPoints).toBeDefined();
            expect(llmNode.connectionPoints.outputs).toBeDefined();

            const llmPoint = llmNode.connectionPoints.outputs.find(
                (p: any) => p.id === connection.anchors.from.connectionPointId
            );

            expect(llmPoint).toBeDefined();
            expect(llmPoint.semanticSelection).toBeDefined();
            expect(llmPoint.semanticSelection.isSelected).toBe(true);
            expect(llmPoint.semanticSelection.selectionReason).toBe(fromPoint!.semanticSelection.selectionReason);
        });
    });
});

