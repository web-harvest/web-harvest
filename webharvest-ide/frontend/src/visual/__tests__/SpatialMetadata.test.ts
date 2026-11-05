/**
 * Spatial Metadata Tests
 * 
 * Testy sprawdzające, że model przestrzenny zawiera wszystkie potrzebne informacje
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode, Plugin } from '../types';
import type { PluginManager } from '../../core/PluginManager';
import type { VisualSemanticModel } from '../VisualSemanticModel';

class MockPluginManager implements Partial<PluginManager> {
    private plugins: Map<string, Plugin> = new Map();

    findPlugin(id: string): Plugin | null {
        return this.plugins.get(id) || null;
    }

    findPluginById(id: string): Plugin | null {
        return this.findPlugin(id);
    }

    getPlugins(): Plugin[] {
        return Array.from(this.plugins.values());
    }

    addPlugin(plugin: Plugin): void {
        this.plugins.set(plugin.id, plugin);
    }
}

function createMockDesigner(): { designer: VisualDesigner; pluginManager: MockPluginManager } {
    const container = document.createElement('div');
    container.id = 'test-canvas-spatial';
    document.body.appendChild(container);

    const pluginManager = new MockPluginManager();
    
    pluginManager.addPlugin({
        id: 'def',
        name: 'Def',
        category: 'Core',
        schema: { attributes: [{ name: 'var', type: 'string', required: true }] },
        semantics: { canAppearAtRoot: true, allowsChildren: false }
    } as Plugin);
    
    pluginManager.addPlugin({
        id: 'var',
        name: 'Var',
        category: 'Core',
        schema: { attributes: [{ name: 'name', type: 'string', required: true }] },
        semantics: { canAppearAtRoot: true, allowsChildren: false }
    } as Plugin);

    const designer = new VisualDesigner('test-canvas-spatial', pluginManager as any);
    designer.setPluginManager(pluginManager as any);

    return { designer, pluginManager };
}

describe('Spatial Metadata', () => {
    let designer: VisualDesigner;
    let pluginManager: MockPluginManager;

    beforeEach(() => {
        const mock = createMockDesigner();
        designer = mock.designer;
        pluginManager = mock.pluginManager;
    });

    describe('ArrowMetadata spatialInfo', () => {
        it('should have spatialInfo in arrowMetadata', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [],
                    position: { x: 0, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: null,
                    children: [],
                    position: { x: 200, y: 100 }
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const model = designer.exportModelForLLM();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const arrowMetadata = conn.path?.arrowMetadata;
                expect(arrowMetadata).toBeDefined();
                
                if (arrowMetadata) {
                    expect(arrowMetadata.spatialInfo).toBeDefined();
                    expect(arrowMetadata.spatialInfo.arrowTipPosition).toBeDefined();
                    expect(arrowMetadata.spatialInfo.arrowBaseCenter).toBeDefined();
                    expect(arrowMetadata.spatialInfo.arrowSize).toBeGreaterThan(0);
                    expect(arrowMetadata.spatialInfo.distanceToTargetCenter).toBeGreaterThanOrEqual(0);
                    expect(arrowMetadata.spatialInfo.angleDegrees).toBeDefined();
                    expect(arrowMetadata.spatialInfo.isHorizontal).toBeDefined();
                    expect(arrowMetadata.spatialInfo.isVertical).toBeDefined();
                    expect([1, 2, 3, 4]).toContain(arrowMetadata.spatialInfo.quadrant);
                }
            });
        });

        it('should have correct quadrant based on angle', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [],
                    position: { x: 100, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: null,
                    children: [],
                    position: { x: 300, y: 100 } // Right of node1
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const model = designer.exportModelForLLM();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const arrowMetadata = conn.path?.arrowMetadata;
                if (arrowMetadata) {
                    const angle = arrowMetadata.angle;
                    const quadrant = arrowMetadata.spatialInfo.quadrant;
                    
                    // Verify quadrant matches angle
                    if (angle >= 0 && angle < Math.PI / 2) {
                        expect(quadrant).toBe(1); // Right-down
                    } else if (angle >= Math.PI / 2 && angle < Math.PI) {
                        expect(quadrant).toBe(2); // Left-down
                    } else if (angle >= Math.PI && angle < 3 * Math.PI / 2) {
                        expect(quadrant).toBe(3); // Left-up
                    } else {
                        expect(quadrant).toBe(4); // Right-up
                    }
                }
            });
        });
    });

    describe('ConnectionPathState spatialMetadata', () => {
        it('should have spatialMetadata in path', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [],
                    position: { x: 0, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: null,
                    children: [],
                    position: { x: 200, y: 100 }
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const model = designer.exportModelForLLM();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const spatialMetadata = conn.path?.spatialMetadata;
                expect(spatialMetadata).toBeDefined();
                
                if (spatialMetadata) {
                    expect(spatialMetadata.totalLength).toBeGreaterThan(0);
                    expect(spatialMetadata.segmentCount).toBeGreaterThan(0);
                    expect(spatialMetadata.minDistanceToNode).toBeGreaterThanOrEqual(0);
                    expect(spatialMetadata.boundingBox).toBeDefined();
                    expect(spatialMetadata.startSide).toBeDefined();
                    expect(spatialMetadata.endSide).toBeDefined();
                    
                    // Verify bounding box
                    expect(spatialMetadata.boundingBox.width).toBeGreaterThanOrEqual(0);
                    expect(spatialMetadata.boundingBox.height).toBeGreaterThanOrEqual(0);
                }
            });
        });

        it('should calculate correct path length', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [],
                    position: { x: 0, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: null,
                    children: [],
                    position: { x: 200, y: 100 }
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const model = designer.exportModelForLLM();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const path = conn.path;
                const spatialMetadata = path?.spatialMetadata;
                
                if (path && spatialMetadata) {
                    // Path length should be at least the direct distance
                    const points = path.points;
                    if (points.length >= 2) {
                        const directDistance = Math.sqrt(
                            (points[points.length - 1].x - points[0].x) ** 2 +
                            (points[points.length - 1].y - points[0].y) ** 2
                        );
                        expect(spatialMetadata.totalLength).toBeGreaterThanOrEqual(directDistance);
                    }
                }
            });
        });
    });

    describe('VisualConnectionState spatialValidation', () => {
        it('should have spatialValidation in connection state', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [],
                    position: { x: 0, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: null,
                    children: [],
                    position: { x: 200, y: 100 }
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const model = designer.exportModelForLLM();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const spatialValidation = (conn as any).spatialValidation;
                expect(spatialValidation).toBeDefined();
                
                if (spatialValidation) {
                    expect(spatialValidation.pathLength).toBeGreaterThan(0);
                    expect(typeof spatialValidation.isValidPath).toBe('boolean');
                    expect(spatialValidation.startNodeDistance).toBeGreaterThanOrEqual(0);
                    expect(spatialValidation.endNodeDistance).toBeGreaterThanOrEqual(0);
                    expect(spatialValidation.minClearance).toBeGreaterThanOrEqual(0);
                    expect(['excellent', 'good', 'acceptable', 'poor']).toContain(spatialValidation.routingQuality);
                }
            });
        });

        it('should assess routing quality correctly', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [],
                    position: { x: 0, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: null,
                    children: [],
                    position: { x: 200, y: 100 }
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const model = designer.exportModelForLLM();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const spatialValidation = (conn as any).spatialValidation;
                if (spatialValidation) {
                    // Quality should be one of the valid values
                    expect(['excellent', 'good', 'acceptable', 'poor']).toContain(spatialValidation.routingQuality);
                }
            });
        });
    });
});






