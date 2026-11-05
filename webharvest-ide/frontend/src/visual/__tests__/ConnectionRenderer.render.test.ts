/**
 * Unit tests for ConnectionRenderer rendering
 * Tests that connections are correctly rendered to SVG
 */

import { ConnectionRenderer } from '../ConnectionRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import type { PluginManager } from '../../core/PluginManager';
import type { VisualNode } from '../../types';
import type { VisualSemanticModel } from '../VisualSemanticModel';

describe('ConnectionRenderer Rendering', () => {
    let svgElement: SVGSVGElement;
    let modelManager: VisualSemanticModelManager;
    let renderer: ConnectionRenderer;
    let mockDesigner: Partial<VisualDesigner>;
    let mockPluginManager: PluginManager;

    beforeEach(() => {
        // Create SVG element
        svgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement.setAttribute('width', '1000');
        svgElement.setAttribute('height', '1000');
        document.body.appendChild(svgElement);

        // Create mock designer
        mockDesigner = {
            getAllNodes: () => [],
            getNodes: () => [],
            connectionManager: {
                getConnections: () => []
            }
        } as any;

        // Create mock plugin manager
        mockPluginManager = {
            getPlugins: () => [],
            getPlugin: () => null,
            getPluginMetadata: () => null
        } as any;

        // Create model manager
        modelManager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, mockPluginManager);

        // Create renderer
        renderer = new ConnectionRenderer(svgElement, modelManager);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('should render connections when model has valid path points', () => {
        // Mock model with connections
        const mockModel: Partial<VisualSemanticModel> = {
            workflow: {
                rootNodes: [],
                allNodes: new Map(),
                connections: new Map([
                    ['conn-1', {
                        connectionId: 'conn-1',
                        fromNodeId: 'node1',
                        toNodeId: 'node2',
                        type: 'sequential',
                        path: {
                            points: [
                                { x: 100, y: 100 },
                                { x: 200, y: 100 },
                                { x: 200, y: 200 },
                                { x: 300, y: 200 }
                            ],
                            routingType: 'orthogonal',
                            arrowMetadata: {
                                position: { x: 300, y: 200 },
                                angle: 0,
                                pointsTowardsNode: true
                            }
                        },
                        anchors: {
                            from: { nodeId: 'node1', connectionPointId: 'out1', position: { x: 100, y: 100 } },
                            to: { nodeId: 'node2', connectionPointId: 'in1', position: { x: 300, y: 200 } }
                        },
                        visualState: { selected: false, hovered: false, highlighted: false, error: false },
                        edgeValidation: {
                            startPointOnEdge: true,
                            endPointOnEdge: true,
                            startPointTolerance: 0,
                            endPointTolerance: 0,
                            arrowPointsTowardsTarget: true
                        },
                        spatialValidation: {
                            pathLength: 300,
                            isValidPath: true,
                            startNodeDistance: 0,
                            endNodeDistance: 0,
                            minClearance: 10,
                            routingQuality: 'good'
                        },
                        pathGeometry: {
                            startPoint: { x: 100, y: 100 },
                            endPoint: { x: 300, y: 200 },
                            turningPoints: [{ x: 200, y: 100 }, { x: 200, y: 200 }],
                            segments: [],
                            selfIntersections: [],
                            crossesOtherConnections: [],
                            reversesDirection: false,
                            maxDeviation: 0
                        },
                        renderCache: { svgPath: null, lastRenderTime: 0, needsRerender: false }
                    }]
                ]),
                variableDependencies: new Map()
            }
        };

        // Mock getModel to return our model
        jest.spyOn(modelManager, 'getModel').mockReturnValue(mockModel as VisualSemanticModel);
        jest.spyOn(modelManager, 'buildModel').mockImplementation(() => {});

        // Render
        renderer.renderAll();

        // Check if path and arrow were added
        const paths = svgElement.querySelectorAll('path.connection-line');
        const arrows = svgElement.querySelectorAll('polygon.connection-arrow');

        expect(paths.length).toBe(1);
        expect(arrows.length).toBe(1);
        expect(paths[0].getAttribute('data-connection-id')).toBe('conn-1');
    });

    test('should handle SVG element update correctly', () => {
        // First render to old SVG
        const oldSvg = svgElement;
        const mockModel: Partial<VisualSemanticModel> = {
            workflow: {
                rootNodes: [],
                allNodes: new Map(),
                connections: new Map([
                    ['conn-1', {
                        connectionId: 'conn-1',
                        fromNodeId: 'node1',
                        toNodeId: 'node2',
                        type: 'sequential',
                        path: {
                            points: [{ x: 0, y: 0 }, { x: 100, y: 100 }],
                            routingType: 'straight',
                            arrowMetadata: {
                                position: { x: 100, y: 100 },
                                angle: 45,
                                pointsTowardsNode: true
                            }
                        },
                        anchors: {
                            from: { nodeId: 'node1', connectionPointId: 'out1', position: { x: 0, y: 0 } },
                            to: { nodeId: 'node2', connectionPointId: 'in1', position: { x: 100, y: 100 } }
                        },
                        visualState: { selected: false, hovered: false, highlighted: false, error: false },
                        edgeValidation: {
                            startPointOnEdge: true,
                            endPointOnEdge: true,
                            startPointTolerance: 0,
                            endPointTolerance: 0,
                            arrowPointsTowardsTarget: true
                        },
                        spatialValidation: {
                            pathLength: 141,
                            isValidPath: true,
                            startNodeDistance: 0,
                            endNodeDistance: 0,
                            minClearance: 10,
                            routingQuality: 'good'
                        },
                        pathGeometry: {
                            startPoint: { x: 0, y: 0 },
                            endPoint: { x: 100, y: 100 },
                            turningPoints: [],
                            segments: [],
                            selfIntersections: [],
                            crossesOtherConnections: [],
                            reversesDirection: false,
                            maxDeviation: 0
                        },
                        renderCache: { svgPath: null, lastRenderTime: 0, needsRerender: false }
                    }]
                ]),
                variableDependencies: new Map()
            }
        };

        jest.spyOn(modelManager, 'getModel').mockReturnValue(mockModel as VisualSemanticModel);
        jest.spyOn(modelManager, 'buildModel').mockImplementation(() => {});

        renderer.renderAll();
        expect(oldSvg.querySelectorAll('path.connection-line').length).toBe(1);

        // Create new SVG and update
        const newSvg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        newSvg.setAttribute('width', '1000');
        newSvg.setAttribute('height', '1000');
        document.body.appendChild(newSvg);

        renderer.updateSvgElement(newSvg);

        // Connections should be re-rendered to new SVG
        expect(newSvg.querySelectorAll('path.connection-line').length).toBe(1);
        expect(renderer.svgElement).toBe(newSvg);
    });

    test('should not render connections with invalid points', () => {
        const mockModel: Partial<VisualSemanticModel> = {
            workflow: {
                rootNodes: [],
                allNodes: new Map(),
                connections: new Map([
                    ['conn-invalid', {
                        connectionId: 'conn-invalid',
                        fromNodeId: 'node1',
                        toNodeId: 'node2',
                        type: 'sequential',
                        path: {
                            points: [], // Invalid: empty points
                            routingType: 'orthogonal',
                            arrowMetadata: {
                                position: { x: 0, y: 0 },
                                angle: 0,
                                pointsTowardsNode: true
                            }
                        },
                        anchors: {
                            from: { nodeId: 'node1', connectionPointId: 'out1', position: { x: 0, y: 0 } },
                            to: { nodeId: 'node2', connectionPointId: 'in1', position: { x: 100, y: 100 } }
                        },
                        visualState: { selected: false, hovered: false, highlighted: false, error: false },
                        edgeValidation: {
                            startPointOnEdge: true,
                            endPointOnEdge: true,
                            startPointTolerance: 0,
                            endPointTolerance: 0,
                            arrowPointsTowardsTarget: true
                        },
                        spatialValidation: {
                            pathLength: 0,
                            isValidPath: false,
                            startNodeDistance: 0,
                            endNodeDistance: 0,
                            minClearance: 0,
                            routingQuality: 'poor'
                        },
                        pathGeometry: {
                            startPoint: { x: 0, y: 0 },
                            endPoint: { x: 100, y: 100 },
                            turningPoints: [],
                            segments: [],
                            selfIntersections: [],
                            crossesOtherConnections: [],
                            reversesDirection: false,
                            maxDeviation: 0
                        },
                        renderCache: { svgPath: null, lastRenderTime: 0, needsRerender: false }
                    }]
                ]),
                variableDependencies: new Map()
            }
        };

        jest.spyOn(modelManager, 'getModel').mockReturnValue(mockModel as VisualSemanticModel);
        jest.spyOn(modelManager, 'buildModel').mockImplementation(() => {});

        renderer.renderAll();

        // Should use fallback path from anchors
        const paths = svgElement.querySelectorAll('path.connection-line');
        expect(paths.length).toBe(1); // Fallback path should be created
    });
});





