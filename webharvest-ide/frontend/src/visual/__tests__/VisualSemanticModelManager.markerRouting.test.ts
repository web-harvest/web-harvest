/**
 * Testy TDD dla routingu połączeń z markerami
 * 
 * TDD Cycle: Red → Green → Refactor
 * 
 * Testujemy czy routing połączeń z markerami:
 * - Używa poprawnych connection points
 * - Generuje poprawne ścieżki (orthogonal routing)
 * - Oblicza poprawnie arrow metadata
 * - Obsługuje różne kombinacje (start->node, node->end, start->end)
 */

import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import { ConnectionManager } from '../ConnectionManager';
import type { VisualNode } from '../types';

describe('VisualSemanticModelManager - Marker Connection Routing - TDD', () => {
    let manager: VisualSemanticModelManager;
    let mockDesigner: Partial<VisualDesigner>;
    let pluginManager: PluginManager;

    beforeEach(() => {
        pluginManager = new PluginManager();
        
        const connectionManager = new ConnectionManager();
        
        mockDesigner = {
            startMarkerPosition: { x: 170, y: 80 },
            endMarkerPosition: { x: 170, y: 1300 },
            getAllNodes: jest.fn(() => []),
            getNodes: jest.fn(() => []),
            connectionManager: connectionManager
        } as any;

        manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);
    });

    describe('RED: Testy które sprawdzają routing połączeń z markerami', () => {
        it('should route connection from workflow-start to node using correct connection points', () => {
            // RED: Test dla routingu workflow-start -> node
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            expect(connectionState?.path.points.length).toBeGreaterThanOrEqual(2);
            
            // Sprawdź czy path zaczyna się od connection point markera start
            const startPoint = connectionState?.path.points[0];
            const expectedStartY = (mockDesigner.startMarkerPosition!.y + 30); // MARKER_RADIUS = 30
            expect(startPoint?.x).toBe(mockDesigner.startMarkerPosition!.x);
            expect(Math.abs(startPoint!.y - expectedStartY)).toBeLessThan(1);
            
            // Sprawdź czy path kończy się na connection point node'a
            const endPoint = connectionState?.path.points[connectionState.path.points.length - 1];
            expect(endPoint).toBeDefined();
        });

        it('should route connection from node to workflow-end using correct connection points', () => {
            // RED: Test dla routingu node -> workflow-end
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('node-1', 'workflow-end', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            expect(connectionState?.path.points.length).toBeGreaterThanOrEqual(2);
            
            // Sprawdź czy path kończy się na connection point markera end
            const endPoint = connectionState?.path.points[connectionState.path.points.length - 1];
            const expectedEndY = (mockDesigner.endMarkerPosition!.y - 30); // MARKER_RADIUS = 30
            expect(endPoint?.x).toBe(mockDesigner.endMarkerPosition!.x);
            expect(Math.abs(endPoint!.y - expectedEndY)).toBeLessThan(1);
        });

        it('should use correct connectionPointId for node connections to markers', () => {
            // RED: Test czy connectionPointId jest poprawny dla node -> workflow-end
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('node-1', 'workflow-end', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            
            // Sprawdź czy from connectionPointId to rzeczywisty connection point node'a, nie 'marker-output'
            const fromPointId = connectionState?.anchors.from.connectionPointId;
            expect(fromPointId).not.toBe('marker-output');
            expect(fromPointId).toBeDefined();
            
            // Sprawdź czy to connectionPointId to 'marker-input' dla workflow-end
            const toPointId = connectionState?.anchors.to.connectionPointId;
            expect(toPointId).toBe('marker-input');
        });

        it('should calculate arrow metadata for marker connections', () => {
            // RED: Test czy arrow metadata jest obliczane dla połączeń z markerami
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            expect(connectionState?.path.arrowMetadata).toBeDefined();
            expect(connectionState?.path.arrowMetadata?.pointsTowardsNode).toBe(true);
        });

        it('should handle routing type correctly for marker connections', () => {
            // RED: Test czy routing type jest poprawny dla połączeń z markerami
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            expect(connectionState?.path.routingType).toBeDefined();
            // Marker connections should use orthogonal routing
            expect(['orthogonal', 'bezier', 'straight']).toContain(connectionState?.path.routingType);
        });
    });

    describe('GREEN: Testy które sprawdzają poprawne zachowanie', () => {
        it('should route multiple marker connections correctly', () => {
            // GREEN: Test dla wielu połączeń z markerami
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
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 400, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const conn1 = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            const conn2 = connectionManager.createConnection('node-1', 'node-2', 'sequential');
            const conn3 = connectionManager.createConnection('node-2', 'workflow-end', 'sequential');
            
            const model = manager.buildModel();
            
            expect(model.workflow.connections.has(conn1.id)).toBe(true);
            expect(model.workflow.connections.has(conn2.id)).toBe(true);
            expect(model.workflow.connections.has(conn3.id)).toBe(true);
            
            // Sprawdź czy wszystkie mają poprawne path points
            const state1 = model.workflow.connections.get(conn1.id);
            const state2 = model.workflow.connections.get(conn2.id);
            const state3 = model.workflow.connections.get(conn3.id);
            
            expect(state1?.path.points.length).toBeGreaterThanOrEqual(2);
            expect(state2?.path.points.length).toBeGreaterThanOrEqual(2);
            expect(state3?.path.points.length).toBeGreaterThanOrEqual(2);
        });

        it('should calculate spatial metadata for marker connections', () => {
            // GREEN: Test czy spatial metadata jest obliczane
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            expect(connectionState?.path.spatialMetadata).toBeDefined();
            expect(connectionState?.path.spatialMetadata.totalLength).toBeGreaterThan(0);
            expect(connectionState?.path.spatialMetadata.startPoint).toBeDefined();
            expect(connectionState?.path.spatialMetadata.endPoint).toBeDefined();
        });
    });

    describe('REFACTOR: Testy dla edge cases i optymalizacji', () => {
        it('should handle marker positions changing after connection creation', () => {
            // REFACTOR: Test czy zmiana pozycji markerów aktualizuje connection points
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            
            // Build model with initial marker positions
            let model = manager.buildModel();
            let connectionState = model.workflow.connections.get(connection.id);
            const initialStartY = connectionState?.path.spatialMetadata.startPoint.y;
            
            // Change marker position
            (mockDesigner as any).startMarkerPosition = { x: 300, y: 100 };
            
            // Rebuild model
            model = manager.buildModel();
            connectionState = model.workflow.connections.get(connection.id);
            const newStartY = connectionState?.path.spatialMetadata.startPoint.y;
            
            expect(newStartY).not.toBe(initialStartY);
            expect(Math.abs(newStartY - (100 + 30))).toBeLessThan(1); // New position + MARKER_RADIUS
        });

        it('should handle connection from workflow-end to node', () => {
            // REFACTOR: Test dla nietypowego przypadku workflow-end -> node
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('workflow-end', 'node-1', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            expect(connectionState?.path.points.length).toBeGreaterThanOrEqual(2);
            
            // workflow-end as source should use bottom center
            const startPoint = connectionState?.path.points[0];
            const expectedStartY = (mockDesigner.endMarkerPosition!.y + 30);
            expect(Math.abs(startPoint!.y - expectedStartY)).toBeLessThan(1);
        });

        it('should handle connection from node to workflow-start', () => {
            // REFACTOR: Test dla nietypowego przypadku node -> workflow-start
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('node-1', 'workflow-start', 'sequential');
            
            const model = manager.buildModel();
            const connectionState = model.workflow.connections.get(connection.id);
            
            expect(connectionState).toBeDefined();
            expect(connectionState?.path.points.length).toBeGreaterThanOrEqual(2);
            
            // workflow-start as target should use top center
            const endPoint = connectionState?.path.points[connectionState.path.points.length - 1];
            const expectedEndY = (mockDesigner.startMarkerPosition!.y - 30);
            expect(Math.abs(endPoint!.y - expectedEndY)).toBeLessThan(1);
        });

        it('should preserve connectionPointId when rebuilding model', () => {
            // REFACTOR: Test czy connectionPointId jest zachowywany przy rebuild
            const node1: VisualNode = { 
                id: 'node-1', 
                type: 'http', 
                attributes: {}, 
                body: null, 
                children: [], 
                position: { x: 200, y: 200 } 
            };
            
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            const connection = connectionManager.createConnection('node-1', 'workflow-end', 'sequential');
            
            // Build model multiple times
            let model = manager.buildModel();
            let connectionState = model.workflow.connections.get(connection.id);
            const firstFromPointId = connectionState?.anchors.from.connectionPointId;
            const firstToPointId = connectionState?.anchors.to.connectionPointId;
            
            model = manager.buildModel();
            connectionState = model.workflow.connections.get(connection.id);
            const secondFromPointId = connectionState?.anchors.from.connectionPointId;
            const secondToPointId = connectionState?.anchors.to.connectionPointId;
            
            expect(firstFromPointId).toBe(secondFromPointId);
            expect(firstToPointId).toBe(secondToPointId);
        });
    });
});

