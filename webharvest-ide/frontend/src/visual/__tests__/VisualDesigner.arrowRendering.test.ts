/**
 * VisualDesigner Arrow Rendering Tests
 * 
 * Testy sprawdzające, że strzałki są renderowane poprawnie na wszystkich połączeniach
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import type { VisualNode, Plugin } from '../types';
import type { PluginManager } from '../../core/PluginManager';

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

    getSchemaVersion(): string | null {
        return null;
    }

    addPlugin(plugin: Plugin): void {
        this.plugins.set(plugin.id, plugin);
    }
}

function createMockDesigner(): { designer: VisualDesigner; pluginManager: MockPluginManager } {
    const container = document.createElement('div');
    container.id = 'test-canvas';
    document.body.appendChild(container);

    const pluginManager = new MockPluginManager();
    
    // Add basic plugins
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

    const designer = new VisualDesigner('test-canvas', pluginManager as any);
    designer.setPluginManager(pluginManager as any);

    return { designer, pluginManager };
}

describe('VisualDesigner Arrow Rendering', () => {
    let designer: VisualDesigner;
    let pluginManager: MockPluginManager;
    let modelManager: VisualSemanticModelManager;

    beforeEach(() => {
        const mock = createMockDesigner();
        designer = mock.designer;
        pluginManager = mock.pluginManager;
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
    });

    describe('Arrow rendering for simple connections', () => {
        it('should render arrow for horizontal connection (right to left)', () => {
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

            // Get connections
            const connections = designer.getManualConnections();
            expect(connections.length).toBeGreaterThan(0);

            // Check if arrows are rendered in DOM
            const svg = document.querySelector('#test-canvas svg');
            expect(svg).toBeTruthy();

            const arrows = svg?.querySelectorAll('.connection-arrow');
            expect(arrows?.length).toBeGreaterThan(0);

            // Verify arrow points
            arrows?.forEach(arrow => {
                const polygon = arrow as SVGPolygonElement;
                const points = polygon.points;
                expect(points.length).toBe(3); // Arrow is a triangle
            });
        });

        it('should render arrow for vertical connection (top to bottom)', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [],
                    position: { x: 100, y: 0 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: null,
                    children: [],
                    position: { x: 100, y: 200 }
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const svg = document.querySelector('#test-canvas svg');
            const arrows = svg?.querySelectorAll('.connection-arrow');
            expect(arrows?.length).toBeGreaterThan(0);
        });

        it('should render arrows for all connections in chain', () => {
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
                },
                {
                    id: 'node3',
                    type: 'var',
                    attributes: { name: 'final' },
                    body: null,
                    children: [],
                    position: { x: 400, y: 100 }
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            const connections = designer.getManualConnections();
            const svg = document.querySelector('#test-canvas svg');
            const arrows = svg?.querySelectorAll('.connection-arrow');

            // Should have at least as many arrows as connections
            expect(arrows?.length || 0).toBeGreaterThanOrEqual(connections.length);
        });
    });

    describe('Arrow direction validation', () => {
        it('should render arrows pointing towards target nodes', () => {
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

            // Get model and check arrow metadata
            const model = modelManager.toLLMFormat();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const arrowMetadata = conn.path?.arrowMetadata;
                if (arrowMetadata) {
                    expect(arrowMetadata.pointsTowardsNode).toBe(true);
                }
            });
        });

        it('should have arrow angle matching last segment direction', () => {
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

            const model = modelManager.toLLMFormat();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const path = conn.path;
                const points = path?.points || [];
                const arrowMetadata = path?.arrowMetadata;

                if (points.length >= 2 && arrowMetadata) {
                    // Calculate last segment angle
                    const lastPoint = points[points.length - 1];
                    const secondLast = points[points.length - 2];
                    const segDx = lastPoint.x - secondLast.x;
                    const segDy = lastPoint.y - secondLast.y;
                    const segAngle = Math.atan2(segDy, segDx);

                    // Compare with arrow angle (allow small tolerance)
                    const angleDiff = Math.abs(arrowMetadata.angle - segAngle);
                    const normalizedDiff = angleDiff > Math.PI ? 2 * Math.PI - angleDiff : angleDiff;

                    expect(normalizedDiff).toBeLessThan(0.1); // ~6 degrees tolerance
                }
            });
        });
    });

    describe('Edge connection points', () => {
        it('should have connection points exactly on node edges', () => {
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

            const model = modelManager.toLLMFormat();
            const connections = Object.values(model.workflow.connections);

            connections.forEach(conn => {
                const pathPoints = conn.path?.points || [];
                if (pathPoints.length >= 2) {
                    const startPoint = pathPoints[0];
                    const endPoint = pathPoints[pathPoints.length - 1];

                    // Find nodes
                    const fromNode = Object.values(model.workflow.allNodes).find(n => n.nodeId === conn.from);
                    const toNode = Object.values(model.workflow.allNodes).find(n => n.nodeId === conn.to);

                    if (fromNode && toNode) {
                        const fromBounds = fromNode.computedBounds;
                        const toBounds = toNode.computedBounds;

                        // Check start point is on from node edge
                        const startOnEdge = (
                            Math.abs(startPoint.x - fromBounds.left) < 1 ||
                            Math.abs(startPoint.x - fromBounds.right) < 1 ||
                            Math.abs(startPoint.y - fromBounds.top) < 1 ||
                            Math.abs(startPoint.y - fromBounds.bottom) < 1
                        );
                        expect(startOnEdge).toBe(true);

                        // Check end point is on to node edge
                        const endOnEdge = (
                            Math.abs(endPoint.x - toBounds.left) < 1 ||
                            Math.abs(endPoint.x - toBounds.right) < 1 ||
                            Math.abs(endPoint.y - toBounds.top) < 1 ||
                            Math.abs(endPoint.y - toBounds.bottom) < 1
                        );
                        expect(endOnEdge).toBe(true);
                    }
                }
            });
        });
    });
});

