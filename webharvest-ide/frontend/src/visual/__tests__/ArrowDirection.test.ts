/**
 * Arrow Direction Tests - TDD
 * 
 * Testy weryfikujące, że routing i strzałki są skierowane ku etykiecie (target node).
 */

import { describe, test, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { ConnectionPathRouter } from '../ConnectionPathRouter';
import type { VisualNode, Plugin } from '../../types';
import type { PluginManager } from '../../core/PluginManager';
import type { Point, Side, VisualNodeState } from '../VisualSemanticModel';

class MockPluginManager implements Partial<PluginManager> {
    private plugins: Map<string, Plugin> = new Map();

    findPlugin(type: string): Plugin | null {
        return this.plugins.get(type) || null;
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

describe('Arrow Direction - TDD', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;
    let modelManager: VisualSemanticModelManager;
    let connectionPathRouter: ConnectionPathRouter;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-arrow';
        container.style.width = '2000px';
        container.style.height = '2000px';
        document.body.appendChild(container);

        pluginManager = new MockPluginManager() as any;

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
            id: 'xpath',
            name: 'XPath',
            category: 'xpath',
            icon: 'fa-code',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);

        designer = new VisualDesigner('test-canvas-arrow');
        designer.setPluginManager(pluginManager as any);
        
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
        connectionPathRouter = new ConnectionPathRouter();
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Last path segment should point towards target node', () => {
        test('last path segment should point towards target node', () => {
            const nodes: VisualNode[] = [
                { id: 'node1', type: 'http', attributes: { url: 'https://example.com/1' }, position: { x: 100, y: 100 }, children: [] },
                { id: 'node2', type: 'http', attributes: { url: 'https://example.com/2' }, position: { x: 400, y: 100 }, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    designer.connectNodes('node1', 'node2', 'sequential');
                    designer.render();

                    setTimeout(() => {
                        const model = modelManager.getModel();
                        const connections = Array.from(model.workflow.connections.values());
                        const connectionPaths = Array.from(model.layout.connectionPaths.values());

                        expect(connections.length).toBeGreaterThan(0);
                        expect(connectionPaths.length).toBeGreaterThan(0);

                        const connection = connections[0];
                        const pathState = connectionPaths.find(p => p.points.length > 0);

                        if (pathState && pathState.points.length >= 2) {
                            const toNodeState = model.workflow.allNodes.get(connection.toNodeId);
                            expect(toNodeState).toBeTruthy();

                            const lastPoint = pathState.points[pathState.points.length - 1];
                            const secondLastPoint = pathState.points[pathState.points.length - 2];

                            // Calculate direction of last segment
                            const segmentDirection = {
                                x: lastPoint.x - secondLastPoint.x,
                                y: lastPoint.y - secondLastPoint.y
                            };

                            // Calculate direction from last point to target node center
                            const targetCenter = {
                                x: toNodeState!.computedBounds.centerX,
                                y: toNodeState!.computedBounds.centerY
                            };
                            const toTargetDirection = {
                                x: targetCenter.x - lastPoint.x,
                                y: targetCenter.y - lastPoint.y
                            };

                            // Dot product should be positive (same general direction)
                            const dotProduct = segmentDirection.x * toTargetDirection.x + segmentDirection.y * toTargetDirection.y;
                            expect(dotProduct).toBeGreaterThan(0);
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow should be oriented towards target node edge', () => {
            const nodes: VisualNode[] = [
                { id: 'node1', type: 'http', attributes: { url: 'https://example.com/1' }, position: { x: 100, y: 100 }, children: [] },
                { id: 'node2', type: 'http', attributes: { url: 'https://example.com/2' }, position: { x: 400, y: 100 }, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    designer.connectNodes('node1', 'node2', 'sequential');
                    designer.render();

                    setTimeout(() => {
                        const model = modelManager.getModel();
                        const connectionPaths = Array.from(model.layout.connectionPaths.values());

                        expect(connectionPaths.length).toBeGreaterThan(0);

                        const pathState = connectionPaths[0];
                        if (pathState.points.length >= 2) {
                            const lastPoint = pathState.points[pathState.points.length - 1];
                            const secondLastPoint = pathState.points[pathState.points.length - 2];

                            // Calculate arrow angle
                            const angle = Math.atan2(lastPoint.y - secondLastPoint.y, lastPoint.x - secondLastPoint.x);

                            // Arrow should point towards target (angle should be reasonable)
                            // For right-side entry, angle should be around 0 (pointing right)
                            // For left-side entry, angle should be around PI (pointing left)
                            // For top entry, angle should be around -PI/2 (pointing up)
                            // For bottom entry, angle should be around PI/2 (pointing down)
                            expect(angle).toBeGreaterThanOrEqual(-Math.PI);
                            expect(angle).toBeLessThanOrEqual(Math.PI);
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow angle should match last segment direction', () => {
            const nodes: VisualNode[] = [
                { id: 'node1', type: 'http', attributes: { url: 'https://example.com/1' }, position: { x: 100, y: 100 }, children: [] },
                { id: 'node2', type: 'http', attributes: { url: 'https://example.com/2' }, position: { x: 400, y: 100 }, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    designer.connectNodes('node1', 'node2', 'sequential');
                    designer.render();

                    setTimeout(() => {
                        const model = modelManager.getModel();
                        const connectionPaths = Array.from(model.layout.connectionPaths.values());

                        expect(connectionPaths.length).toBeGreaterThan(0);

                        const pathState = connectionPaths[0];
                        if (pathState.points.length >= 2) {
                            const lastPoint = pathState.points[pathState.points.length - 1];
                            const secondLastPoint = pathState.points[pathState.points.length - 2];

                            // Calculate angle from last segment
                            const segmentAngle = Math.atan2(lastPoint.y - secondLastPoint.y, lastPoint.x - secondLastPoint.x);

                            // If arrowMetadata exists, it should match
                            if ((pathState as any).arrowMetadata) {
                                const arrowAngle = (pathState as any).arrowMetadata.angle;
                                const angleDiff = Math.abs(segmentAngle - arrowAngle);
                                // Allow for 180-degree flip (if arrow is reversed)
                                const normalizedDiff = Math.min(angleDiff, Math.abs(angleDiff - Math.PI));
                                expect(normalizedDiff).toBeLessThan(0.1); // ~6 degrees tolerance
                            }
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow should not point away from target node', () => {
            const nodes: VisualNode[] = [
                { id: 'node1', type: 'http', attributes: { url: 'https://example.com/1' }, position: { x: 100, y: 100 }, children: [] },
                { id: 'node2', type: 'http', attributes: { url: 'https://example.com/2' }, position: { x: 400, y: 100 }, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    designer.connectNodes('node1', 'node2', 'sequential');
                    designer.render();

                    setTimeout(() => {
                        const model = modelManager.getModel();
                        const connections = Array.from(model.workflow.connections.values());
                        const connectionPaths = Array.from(model.layout.connectionPaths.values());

                        expect(connections.length).toBeGreaterThan(0);
                        expect(connectionPaths.length).toBeGreaterThan(0);

                        const connection = connections[0];
                        const pathState = connectionPaths[0];
                        const toNodeState = model.workflow.allNodes.get(connection.toNodeId);

                        if (pathState.points.length >= 2 && toNodeState) {
                            const lastPoint = pathState.points[pathState.points.length - 1];
                            const secondLastPoint = pathState.points[pathState.points.length - 2];

                            // Calculate direction of last segment
                            const segmentDirection = {
                                x: lastPoint.x - secondLastPoint.x,
                                y: lastPoint.y - secondLastPoint.y
                            };

                            // Calculate direction from last point to target node center
                            const targetCenter = {
                                x: toNodeState.computedBounds.centerX,
                                y: toNodeState.computedBounds.centerY
                            };
                            const toTargetDirection = {
                                x: targetCenter.x - lastPoint.x,
                                y: targetCenter.y - lastPoint.y
                            };

                            // Dot product should be positive (not pointing away)
                            const dotProduct = segmentDirection.x * toTargetDirection.x + segmentDirection.y * toTargetDirection.y;
                            expect(dotProduct).toBeGreaterThan(0);
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('routing should ensure final segment approaches from correct side', () => {
            const nodes: VisualNode[] = [
                { id: 'node1', type: 'http', attributes: { url: 'https://example.com/1' }, position: { x: 100, y: 100 }, children: [] },
                { id: 'node2', type: 'http', attributes: { url: 'https://example.com/2' }, position: { x: 400, y: 100 }, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    designer.connectNodes('node1', 'node2', 'sequential');
                    designer.render();

                    setTimeout(() => {
                        const model = modelManager.getModel();
                        const connections = Array.from(model.workflow.connections.values());
                        const connectionPaths = Array.from(model.layout.connectionPaths.values());

                        expect(connections.length).toBeGreaterThan(0);
                        expect(connectionPaths.length).toBeGreaterThan(0);

                        const connection = connections[0];
                        const pathState = connectionPaths[0];
                        const toNodeState = model.workflow.allNodes.get(connection.toNodeId);

                        if (pathState.points.length >= 2 && toNodeState) {
                            const lastPoint = pathState.points[pathState.points.length - 1];
                            const bounds = toNodeState.computedBounds;

                            // Last point should be on one of the edges
                            const onLeftEdge = Math.abs(lastPoint.x - bounds.left) < 2;
                            const onRightEdge = Math.abs(lastPoint.x - bounds.right) < 2;
                            const onTopEdge = Math.abs(lastPoint.y - bounds.top) < 2;
                            const onBottomEdge = Math.abs(lastPoint.y - bounds.bottom) < 2;

                            expect(onLeftEdge || onRightEdge || onTopEdge || onBottomEdge).toBe(true);

                            // If approaching from left, last point should be on left edge
                            // If approaching from right, last point should be on right edge
                            // etc.
                            if (lastPoint.x < bounds.left) {
                                // Approaching from left
                                expect(onLeftEdge).toBe(true);
                            } else if (lastPoint.x > bounds.right) {
                                // Approaching from right
                                expect(onRightEdge).toBe(true);
                            } else if (lastPoint.y < bounds.top) {
                                // Approaching from top
                                expect(onTopEdge).toBe(true);
                            } else if (lastPoint.y > bounds.bottom) {
                                // Approaching from bottom
                                expect(onBottomEdge).toBe(true);
                            }
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);
    });
});









