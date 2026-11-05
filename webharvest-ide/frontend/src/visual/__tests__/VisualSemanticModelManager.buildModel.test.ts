/**
 * Testy TDD dla VisualSemanticModelManager.buildModel()
 * 
 * TDD Cycle: Red → Green → Refactor
 * 
 * Testujemy czy buildModel() poprawnie:
 * - Buduje model z markerami workflow-start i workflow-end
 * - Obsługuje połączenia z markerami
 * - Aktualizuje cache connection points dla markerów
 */

import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import { ConnectionManager } from '../ConnectionManager';

describe('VisualSemanticModelManager.buildModel() - TDD', () => {
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

    describe('RED: Testy które sprawdzają problemy z buildModel()', () => {
        it('should include marker positions in model when markers are set', () => {
            // RED: Sprawdzamy czy model zawiera informacje o markerach
            const model = manager.buildModel();

            // Model powinien być zbudowany
            expect(model).toBeDefined();
            expect(model.workflow).toBeDefined();
            
            // Sprawdzamy czy możemy pobrać connection points dla markerów
            const startPoint = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            const endPoint = manager.getConnectionPointAbsolutePosition('workflow-end', 'marker-input');

            expect(startPoint).not.toBeNull();
            expect(endPoint).not.toBeNull();
        });

        it('should handle connections from workflow-start marker', () => {
            // RED: Test dla połączeń z markerem start
            // Musimy mieć węzeł w modelu, aby połączenie było poprawne
            const node1 = { id: 'node-1', type: 'http', attributes: {}, body: null, children: [], position: { x: 200, y: 200 } };
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            // CRITICAL: Upewnij się, że pozycje markerów są ustawione
            (mockDesigner as any).startMarkerPosition = { x: 170, y: 80 };
            (mockDesigner as any).endMarkerPosition = { x: 170, y: 1300 };
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            
            // Dodajemy połączenie z workflow-start
            const connection = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            
            // DEBUG: Sprawdź czy połączenie jest w ConnectionManager
            const allConnections = connectionManager.getConnections();
            console.log('Connections in ConnectionManager:', allConnections.map(c => ({ id: c.id, from: c.from, to: c.to })));
            
            // Budujemy model
            const model = manager.buildModel();

            // DEBUG: Sprawdź co jest w modelu
            console.log('Model connections:', Array.from(model.workflow.connections.keys()));
            console.log('Expected connection ID:', connection.id);
            console.log('Model nodes:', Array.from(model.workflow.allNodes.keys()));

            // Model powinien zawierać to połączenie
            const connectionState = model.workflow.connections.get(connection.id);
            expect(connectionState).toBeDefined();
            if (connectionState) {
                expect(connectionState.fromNodeId).toBe('workflow-start');
                expect(connectionState.toNodeId).toBe('node-1');
            }
        });

        it('should handle connections to workflow-end marker', () => {
            // RED: Test dla połączeń do markera end
            const node1 = { id: 'node-1', type: 'http', attributes: {}, body: null, children: [], position: { x: 200, y: 200 } };
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            
            // Dodajemy połączenie do workflow-end
            const connection = connectionManager.createConnection('node-1', 'workflow-end', 'sequential');
            
            // Budujemy model
            const model = manager.buildModel();

            // Model powinien zawierać to połączenie
            const connectionState = model.workflow.connections.get(connection.id);
            expect(connectionState).toBeDefined();
            expect(connectionState?.fromNodeId).toBe('node-1');
            expect(connectionState?.toNodeId).toBe('workflow-end');
        });

        it('should calculate correct path for marker connections', () => {
            // RED: Test dla obliczania ścieżki dla połączeń z markerami
            const node1 = { id: 'node-1', type: 'http', attributes: {}, body: null, children: [], position: { x: 200, y: 200 } };
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            
            // Dodajemy połączenie z workflow-start
            const connection = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            
            // Budujemy model
            const model = manager.buildModel();

            const connectionState = model.workflow.connections.get(connection.id);
            expect(connectionState).toBeDefined();
            expect(connectionState?.path).toBeDefined();
            expect(connectionState?.path.points).toBeDefined();
            expect(connectionState?.path.points.length).toBeGreaterThan(0);

            // Pierwszy punkt powinien być na pozycji markera (bottom center)
            const firstPoint = connectionState!.path.points[0];
            const expectedStartPoint = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            
            expect(firstPoint.x).toBe(expectedStartPoint!.x);
            expect(firstPoint.y).toBe(expectedStartPoint!.y);
        });
    });

    describe('GREEN: Testy które sprawdzają poprawne zachowanie', () => {
        it('should build model successfully with markers', () => {
            const model = manager.buildModel();

            expect(model).toBeDefined();
            expect(model.workflow).toBeDefined();
            expect(model.workflow.rootNodes).toBeDefined();
            expect(model.workflow.allNodes).toBeDefined();
            expect(model.workflow.connections).toBeDefined();
        });

        it('should update model when marker positions change', () => {
            const model1 = manager.buildModel();
            
            // Zmieniamy pozycję markera
            (mockDesigner as any).startMarkerPosition = { x: 200, y: 100 };
            
            const model2 = manager.buildModel();

            // Modele powinny być różne (nowe instancje)
            expect(model1).not.toBe(model2);
            
            // Connection points powinny być zaktualizowane
            const newStartPoint = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            expect(newStartPoint!.x).toBe(200);
            expect(newStartPoint!.y).toBe(130); // 100 + 30
        });
    });

    describe('REFACTOR: Testy dla ulepszonej implementacji', () => {
        it('should cache connection points for markers efficiently', () => {
            // REFACTOR: Sprawdzamy czy cache działa efektywnie
            const point1 = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            const point2 = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');

            // Powinno zwrócić ten sam obiekt (lub równoważny)
            expect(point1).toEqual(point2);
        });

        it('should handle multiple marker connections in one model', () => {
            // REFACTOR: Test dla wielu połączeń z markerami
            const node1 = { id: 'node-1', type: 'http', attributes: {}, body: null, children: [], position: { x: 200, y: 200 } };
            const node2 = { id: 'node-2', type: 'http', attributes: {}, body: null, children: [], position: { x: 400, y: 200 } };
            (mockDesigner.getAllNodes as jest.Mock).mockReturnValue([node1, node2]);
            (mockDesigner.getNodes as jest.Mock).mockReturnValue([node1, node2]);
            
            const connectionManager = mockDesigner.connectionManager as ConnectionManager;
            
            const conn1 = connectionManager.createConnection('workflow-start', 'node-1', 'sequential');
            const conn2 = connectionManager.createConnection('node-1', 'workflow-end', 'sequential');
            const conn3 = connectionManager.createConnection('workflow-start', 'node-2', 'sequential');

            const model = manager.buildModel();

            expect(model.workflow.connections.size).toBeGreaterThanOrEqual(3);
            expect(model.workflow.connections.has(conn1.id)).toBe(true);
            expect(model.workflow.connections.has(conn2.id)).toBe(true);
            expect(model.workflow.connections.has(conn3.id)).toBe(true);
        });
    });
});

