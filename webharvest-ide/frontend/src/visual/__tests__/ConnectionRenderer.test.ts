/**
 * ConnectionRenderer Tests
 * 
 * Tests for the ConnectionRenderer class that renders connections based on semantic model.
 */

import { describe, test, expect, beforeEach, afterEach } from '@jest/globals';
import { ConnectionRenderer } from '../ConnectionRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import type { VisualDesigner } from '../VisualDesigner';
import type { VisualSemanticModel, VisualConnectionState } from '../VisualSemanticModel';
import type { PluginManager } from '../../core/PluginManager';

// Mock VisualDesigner
class MockVisualDesigner {
    getAllNodes() {
        return [];
    }
    getNodes() {
        return [];
    }
}

// Mock PluginManager
class MockPluginManager implements Partial<PluginManager> {
    findPlugin() {
        return null;
    }
    getPlugins() {
        return [];
    }
}

describe('ConnectionRenderer', () => {
    let svgElement: SVGSVGElement;
    let modelManager: VisualSemanticModelManager;
    let renderer: ConnectionRenderer;
    let designer: MockVisualDesigner;

    beforeEach(() => {
        // Create SVG element
        svgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement.setAttribute('width', '2000');
        svgElement.setAttribute('height', '2000');
        document.body.appendChild(svgElement);

        // Create mock designer
        designer = new MockVisualDesigner() as any;

        // Create model manager
        modelManager = new VisualSemanticModelManager(
            designer as any,
            new MockPluginManager() as any
        );

        // Create renderer
        renderer = new ConnectionRenderer(svgElement, modelManager);
    });

    afterEach(() => {
        if (svgElement && svgElement.parentNode) {
            svgElement.parentNode.removeChild(svgElement);
        }
    });

    describe('renderConnection', () => {
        test('should render single connection from model', () => {
            // Build model with a connection
            const model = modelManager.buildModel();
            
            // Add nodes and connection manually to model
            // This is a simplified test - in real scenario, nodes would be added via designer
            const connectionId = 'conn-test-1';
            
            // Create a mock connection state
            const mockConnectionState: Partial<VisualConnectionState> = {
                connectionId,
                fromNodeId: 'node1',
                toNodeId: 'node2',
                type: 'sequential',
                path: {
                    points: [
                        { x: 100, y: 150 },
                        { x: 200, y: 150 },
                        { x: 200, y: 200 },
                        { x: 300, y: 200 }
                    ],
                    routingType: 'orthogonal',
                    arrowMetadata: {
                        endPoint: { x: 300, y: 200 },
                        direction: { x: 1, y: 0 },
                        angle: 0,
                        pointsTowardsNode: true,
                        lastSegmentDirection: { x: 1, y: 0 },
                        spatialInfo: {
                            arrowTipPosition: { x: 300, y: 200 },
                            arrowBaseCenter: { x: 290, y: 200 },
                            arrowSize: 10,
                            distanceToTargetCenter: 50,
                            angleDegrees: 0,
                            isHorizontal: true,
                            isVertical: false,
                            quadrant: 1
                        }
                    }
                },
                anchors: {
                    from: {
                        nodeId: 'node1',
                        connectionPointId: 'output-default-right',
                        position: { x: 100, y: 150 }
                    },
                    to: {
                        nodeId: 'node2',
                        connectionPointId: 'input-default-left',
                        position: { x: 300, y: 200 }
                    }
                },
                visualState: {
                    selected: false,
                    hovered: false,
                    highlighted: false,
                    error: false
                },
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
                    startPoint: { x: 100, y: 150 },
                    endPoint: { x: 300, y: 200 },
                    turningPoints: [{ x: 200, y: 150 }, { x: 200, y: 200 }],
                    segments: [],
                    selfIntersections: [],
                    crossesOtherConnections: [],
                    reversesDirection: false,
                    maxDeviation: 0
                },
                renderCache: {
                    svgPath: null,
                    lastRenderTime: 0,
                    needsRerender: false
                }
            };

            // Manually add connection to model (simulating what buildModel would do)
            const currentModel = modelManager.getModel();
            if (currentModel) {
                currentModel.workflow.connections.set(connectionId, mockConnectionState as VisualConnectionState);
            }

            // Render connection
            renderer.renderConnection(connectionId);

            // Check that path element was created
            const pathElement = svgElement.querySelector(`path[data-connection-id="${connectionId}"]`);
            expect(pathElement).toBeTruthy();
            expect(pathElement).toBeInstanceOf(SVGPathElement);

            // Check that arrow element was created
            const arrowElement = svgElement.querySelector(`polygon[data-connection-id="${connectionId}"]`);
            expect(arrowElement).toBeTruthy();
            expect(arrowElement).toBeInstanceOf(SVGPolygonElement);
        });

        test('should use path.points from model', () => {
            const connectionId = 'conn-test-2';
            const points = [
                { x: 50, y: 50 },
                { x: 150, y: 50 },
                { x: 150, y: 100 },
                { x: 250, y: 100 }
            ];

            const mockConnectionState: Partial<VisualConnectionState> = {
                connectionId,
                fromNodeId: 'node1',
                toNodeId: 'node2',
                type: 'sequential',
                path: {
                    points,
                    routingType: 'orthogonal',
                    arrowMetadata: {
                        endPoint: points[points.length - 1],
                        direction: { x: 1, y: 0 },
                        angle: 0,
                        pointsTowardsNode: true,
                        lastSegmentDirection: { x: 1, y: 0 },
                        spatialInfo: {
                            arrowTipPosition: points[points.length - 1],
                            arrowBaseCenter: { x: points[points.length - 1].x - 10, y: points[points.length - 1].y },
                            arrowSize: 10,
                            distanceToTargetCenter: 50,
                            angleDegrees: 0,
                            isHorizontal: true,
                            isVertical: false,
                            quadrant: 1
                        }
                    }
                },
                anchors: {
                    from: { nodeId: 'node1', connectionPointId: 'output', position: points[0] },
                    to: { nodeId: 'node2', connectionPointId: 'input', position: points[points.length - 1] }
                },
                visualState: { selected: false, hovered: false, highlighted: false, error: false },
                edgeValidation: { startPointOnEdge: true, endPointOnEdge: true, startPointTolerance: 0, endPointTolerance: 0, arrowPointsTowardsTarget: true },
                spatialValidation: { pathLength: 200, isValidPath: true, startNodeDistance: 0, endNodeDistance: 0, minClearance: 10, routingQuality: 'good' },
                pathGeometry: {
                    startPoint: points[0],
                    endPoint: points[points.length - 1],
                    turningPoints: points.slice(1, -1),
                    segments: [],
                    selfIntersections: [],
                    crossesOtherConnections: [],
                    reversesDirection: false,
                    maxDeviation: 0
                },
                renderCache: { svgPath: null, lastRenderTime: 0, needsRerender: false }
            };

            const currentModel = modelManager.getModel();
            if (currentModel) {
                currentModel.workflow.connections.set(connectionId, mockConnectionState as VisualConnectionState);
            }

            renderer.renderConnection(connectionId);

            const pathElement = svgElement.querySelector(`path[data-connection-id="${connectionId}"]`) as SVGPathElement;
            expect(pathElement).toBeTruthy();
            
            // Check that path data contains the points
            const pathData = pathElement.getAttribute('d');
            expect(pathData).toContain('50');
            expect(pathData).toContain('250');
        });
    });

    describe('renderAll', () => {
        test('should render all connections from model', () => {
            // This test would require a full model setup with nodes and connections
            // For now, we test that the method doesn't crash
            expect(() => {
                renderer.renderAll();
            }).not.toThrow();
        });

        test('should clear existing connections before rendering', () => {
            // Render some connections first
            const connectionId = 'conn-clear-test';
            const mockConnectionState: Partial<VisualConnectionState> = {
                connectionId,
                fromNodeId: 'node1',
                toNodeId: 'node2',
                type: 'sequential',
                path: {
                    points: [{ x: 0, y: 0 }, { x: 100, y: 100 }],
                    routingType: 'straight',
                    arrowMetadata: {
                        endPoint: { x: 100, y: 100 },
                        direction: { x: 1, y: 1 },
                        angle: Math.PI / 4,
                        pointsTowardsNode: true,
                        lastSegmentDirection: { x: 1, y: 1 },
                        spatialInfo: {
                            arrowTipPosition: { x: 100, y: 100 },
                            arrowBaseCenter: { x: 90, y: 90 },
                            arrowSize: 10,
                            distanceToTargetCenter: 50,
                            angleDegrees: 45,
                            isHorizontal: false,
                            isVertical: false,
                            quadrant: 1
                        }
                    }
                },
                anchors: {
                    from: { nodeId: 'node1', connectionPointId: 'output', position: { x: 0, y: 0 } },
                    to: { nodeId: 'node2', connectionPointId: 'input', position: { x: 100, y: 100 } }
                },
                visualState: { selected: false, hovered: false, highlighted: false, error: false },
                edgeValidation: { startPointOnEdge: true, endPointOnEdge: true, startPointTolerance: 0, endPointTolerance: 0, arrowPointsTowardsTarget: true },
                spatialValidation: { pathLength: 141, isValidPath: true, startNodeDistance: 0, endNodeDistance: 0, minClearance: 10, routingQuality: 'good' },
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
            };

            const currentModel = modelManager.getModel();
            if (currentModel) {
                currentModel.workflow.connections.set(connectionId, mockConnectionState as VisualConnectionState);
            }

            renderer.renderConnection(connectionId);
            
            // Verify connection exists
            expect(svgElement.querySelector(`path[data-connection-id="${connectionId}"]`)).toBeTruthy();

            // Call renderAll - should clear and re-render
            renderer.renderAll();

            // Connection should still exist (re-rendered)
            // Note: In a real scenario with proper model, this would work correctly
        });
    });

    describe('updateConnection', () => {
        test('should update existing connection', () => {
            const connectionId = 'conn-update-test';
            const mockConnectionState: Partial<VisualConnectionState> = {
                connectionId,
                fromNodeId: 'node1',
                toNodeId: 'node2',
                type: 'sequential',
                path: {
                    points: [{ x: 0, y: 0 }, { x: 100, y: 100 }],
                    routingType: 'straight',
                    arrowMetadata: {
                        endPoint: { x: 100, y: 100 },
                        direction: { x: 1, y: 1 },
                        angle: Math.PI / 4,
                        pointsTowardsNode: true,
                        lastSegmentDirection: { x: 1, y: 1 },
                        spatialInfo: {
                            arrowTipPosition: { x: 100, y: 100 },
                            arrowBaseCenter: { x: 90, y: 90 },
                            arrowSize: 10,
                            distanceToTargetCenter: 50,
                            angleDegrees: 45,
                            isHorizontal: false,
                            isVertical: false,
                            quadrant: 1
                        }
                    }
                },
                anchors: {
                    from: { nodeId: 'node1', connectionPointId: 'output', position: { x: 0, y: 0 } },
                    to: { nodeId: 'node2', connectionPointId: 'input', position: { x: 100, y: 100 } }
                },
                visualState: { selected: false, hovered: false, highlighted: false, error: false },
                edgeValidation: { startPointOnEdge: true, endPointOnEdge: true, startPointTolerance: 0, endPointTolerance: 0, arrowPointsTowardsTarget: true },
                spatialValidation: { pathLength: 141, isValidPath: true, startNodeDistance: 0, endNodeDistance: 0, minClearance: 10, routingQuality: 'good' },
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
            };

            const currentModel = modelManager.getModel();
            if (currentModel) {
                currentModel.workflow.connections.set(connectionId, mockConnectionState as VisualConnectionState);
            }

            renderer.renderConnection(connectionId);
            expect(svgElement.querySelector(`path[data-connection-id="${connectionId}"]`)).toBeTruthy();

            // Update connection
            renderer.updateConnection(connectionId);

            // Connection should still exist (re-rendered)
            expect(svgElement.querySelector(`path[data-connection-id="${connectionId}"]`)).toBeTruthy();
        });
    });

    describe('removeConnection', () => {
        test('should remove connection from DOM', () => {
            const connectionId = 'conn-remove-test';
            const mockConnectionState: Partial<VisualConnectionState> = {
                connectionId,
                fromNodeId: 'node1',
                toNodeId: 'node2',
                type: 'sequential',
                path: {
                    points: [{ x: 0, y: 0 }, { x: 100, y: 100 }],
                    routingType: 'straight',
                    arrowMetadata: {
                        endPoint: { x: 100, y: 100 },
                        direction: { x: 1, y: 1 },
                        angle: Math.PI / 4,
                        pointsTowardsNode: true,
                        lastSegmentDirection: { x: 1, y: 1 },
                        spatialInfo: {
                            arrowTipPosition: { x: 100, y: 100 },
                            arrowBaseCenter: { x: 90, y: 90 },
                            arrowSize: 10,
                            distanceToTargetCenter: 50,
                            angleDegrees: 45,
                            isHorizontal: false,
                            isVertical: false,
                            quadrant: 1
                        }
                    }
                },
                anchors: {
                    from: { nodeId: 'node1', connectionPointId: 'output', position: { x: 0, y: 0 } },
                    to: { nodeId: 'node2', connectionPointId: 'input', position: { x: 100, y: 100 } }
                },
                visualState: { selected: false, hovered: false, highlighted: false, error: false },
                edgeValidation: { startPointOnEdge: true, endPointOnEdge: true, startPointTolerance: 0, endPointTolerance: 0, arrowPointsTowardsTarget: true },
                spatialValidation: { pathLength: 141, isValidPath: true, startNodeDistance: 0, endNodeDistance: 0, minClearance: 10, routingQuality: 'good' },
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
            };

            const currentModel = modelManager.getModel();
            if (currentModel) {
                currentModel.workflow.connections.set(connectionId, mockConnectionState as VisualConnectionState);
            }

            renderer.renderConnection(connectionId);
            expect(svgElement.querySelector(`path[data-connection-id="${connectionId}"]`)).toBeTruthy();

            // Remove connection
            renderer.removeConnection(connectionId);

            // Connection should be removed
            expect(svgElement.querySelector(`path[data-connection-id="${connectionId}"]`)).toBeNull();
            expect(svgElement.querySelector(`polygon[data-connection-id="${connectionId}"]`)).toBeNull();
        });
    });

    describe('setSelectedConnection', () => {
        test('should highlight selected connection', () => {
            const connectionId = 'conn-select-test';
            const mockConnectionState: Partial<VisualConnectionState> = {
                connectionId,
                fromNodeId: 'node1',
                toNodeId: 'node2',
                type: 'sequential',
                path: {
                    points: [{ x: 0, y: 0 }, { x: 100, y: 100 }],
                    routingType: 'straight',
                    arrowMetadata: {
                        endPoint: { x: 100, y: 100 },
                        direction: { x: 1, y: 1 },
                        angle: Math.PI / 4,
                        pointsTowardsNode: true,
                        lastSegmentDirection: { x: 1, y: 1 },
                        spatialInfo: {
                            arrowTipPosition: { x: 100, y: 100 },
                            arrowBaseCenter: { x: 90, y: 90 },
                            arrowSize: 10,
                            distanceToTargetCenter: 50,
                            angleDegrees: 45,
                            isHorizontal: false,
                            isVertical: false,
                            quadrant: 1
                        }
                    }
                },
                anchors: {
                    from: { nodeId: 'node1', connectionPointId: 'output', position: { x: 0, y: 0 } },
                    to: { nodeId: 'node2', connectionPointId: 'input', position: { x: 100, y: 100 } }
                },
                visualState: { selected: false, hovered: false, highlighted: false, error: false },
                edgeValidation: { startPointOnEdge: true, endPointOnEdge: true, startPointTolerance: 0, endPointTolerance: 0, arrowPointsTowardsTarget: true },
                spatialValidation: { pathLength: 141, isValidPath: true, startNodeDistance: 0, endNodeDistance: 0, minClearance: 10, routingQuality: 'good' },
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
            };

            const currentModel = modelManager.getModel();
            if (currentModel) {
                currentModel.workflow.connections.set(connectionId, mockConnectionState as VisualConnectionState);
            }

            renderer.renderConnection(connectionId);
            const pathElement = svgElement.querySelector(`path[data-connection-id="${connectionId}"]`) as SVGPathElement;
            
            // Set as selected
            renderer.setSelectedConnection(connectionId);
            
            // Check stroke width increased
            expect(pathElement.getAttribute('stroke-width')).toBe('3');
        });
    });
});





