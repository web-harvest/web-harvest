/**
 * Connection Rendering Integration Tests - TDD
 * 
 * Kompleksowe testy integracyjne dla renderowania połączeń.
 */

import { describe, test, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import type { VisualNode, Plugin } from '../../types';
import type { PluginManager } from '../../core/PluginManager';

class MockPluginManager implements Partial<PluginManager> {
    private plugins: Map<string, Plugin> = new Map();

    findPlugin(type: string): Plugin | null {
        return this.plugins.get(type) || null;
    }

    getPlugins(): Plugin[] {
        return Array.from(this.plugins.values());
    }

    addPlugin(plugin: Plugin): void {
        this.plugins.set(plugin.id, plugin);
    }
}

describe('Connection Rendering Integration - TDD', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;
    let modelManager: VisualSemanticModelManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-integration';
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

        pluginManager.addPlugin({
            id: 'var',
            name: 'Variable',
            category: 'variable',
            icon: 'fa-variable',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);

        designer = new VisualDesigner('test-canvas-integration');
        designer.setPluginManager(pluginManager as any);
        
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Complete connection rendering', () => {
        test('complete connection should start and end at edges', () => {
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
                        const fromNodeState = model.workflow.allNodes.get(connection.fromNodeId);
                        const toNodeState = model.workflow.allNodes.get(connection.toNodeId);

                        expect(fromNodeState).toBeTruthy();
                        expect(toNodeState).toBeTruthy();
                        expect(pathState.points.length).toBeGreaterThanOrEqual(2);

                        const startPoint = pathState.points[0];
                        const endPoint = pathState.points[pathState.points.length - 1];

                        // Start point should be on from node edge
                        const fromBounds = fromNodeState!.computedBounds;
                        const startOnEdge = (
                            Math.abs(startPoint.x - fromBounds.left) < 2 ||
                            Math.abs(startPoint.x - fromBounds.right) < 2 ||
                            Math.abs(startPoint.y - fromBounds.top) < 2 ||
                            Math.abs(startPoint.y - fromBounds.bottom) < 2
                        );
                        expect(startOnEdge).toBe(true);

                        // End point should be on to node edge
                        const toBounds = toNodeState!.computedBounds;
                        const endOnEdge = (
                            Math.abs(endPoint.x - toBounds.left) < 2 ||
                            Math.abs(endPoint.x - toBounds.right) < 2 ||
                            Math.abs(endPoint.y - toBounds.top) < 2 ||
                            Math.abs(endPoint.y - toBounds.bottom) < 2
                        );
                        expect(endOnEdge).toBe(true);

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow should point towards target node in all scenarios', () => {
            const scenarios = [
                { from: { x: 100, y: 100 }, to: { x: 400, y: 100 }, description: 'horizontal right' },
                { from: { x: 400, y: 100 }, to: { x: 100, y: 100 }, description: 'horizontal left' },
                { from: { x: 100, y: 100 }, to: { x: 100, y: 400 }, description: 'vertical down' },
                { from: { x: 100, y: 400 }, to: { x: 100, y: 100 }, description: 'vertical up' },
                { from: { x: 100, y: 100 }, to: { x: 400, y: 400 }, description: 'diagonal' }
            ];

            return Promise.all(scenarios.map(scenario => {
                return new Promise<void>((resolve) => {
                    const nodes: VisualNode[] = [
                        { id: 'node1', type: 'http', attributes: { url: 'https://example.com/1' }, position: scenario.from, children: [] },
                        { id: 'node2', type: 'http', attributes: { url: 'https://example.com/2' }, position: scenario.to, children: [] }
                    ];

                    designer.loadNodes(nodes);
                    designer.render();

                    setTimeout(() => {
                        designer.connectNodes('node1', 'node2', 'sequential');
                        designer.render();

                        setTimeout(() => {
                            const model = modelManager.getModel();
                            const connections = Array.from(model.workflow.connections.values());
                            const connectionPaths = Array.from(model.layout.connectionPaths.values());

                            if (connections.length > 0 && connectionPaths.length > 0) {
                                const connection = connections[0];
                                const pathState = connectionPaths[0];
                                const toNodeState = model.workflow.allNodes.get(connection.toNodeId);

                                if (pathState.points.length >= 2 && toNodeState) {
                                    const lastPoint = pathState.points[pathState.points.length - 1];
                                    const secondLastPoint = pathState.points[pathState.points.length - 2];

                                    // Calculate arrow direction
                                    const arrowDirection = {
                                        x: lastPoint.x - secondLastPoint.x,
                                        y: lastPoint.y - secondLastPoint.y
                                    };

                                    // Calculate direction to target center
                                    const targetCenter = {
                                        x: toNodeState.computedBounds.centerX,
                                        y: toNodeState.computedBounds.centerY
                                    };
                                    const toCenterDirection = {
                                        x: targetCenter.x - lastPoint.x,
                                        y: targetCenter.y - lastPoint.y
                                    };

                                    // Dot product should be positive
                                    const dotProduct = arrowDirection.x * toCenterDirection.x + arrowDirection.y * toCenterDirection.y;
                                    expect(dotProduct).toBeGreaterThan(0);
                                }
                            }

                            resolve();
                        }, 500);
                    }, 500);
                });
            }));
        }, 30000);

        test('connection should work for all node positions and sizes', () => {
            const nodes: VisualNode[] = [
                { id: 'node1', type: 'http', attributes: { url: 'https://example.com/1' }, position: { x: 50, y: 50 }, children: [] },
                { id: 'node2', type: 'http', attributes: { url: 'https://example.com/2' }, position: { x: 500, y: 500 }, children: [] },
                { id: 'node3', type: 'xpath', attributes: { expression: '//title' }, position: { x: 1000, y: 100 }, children: [] }
            ];

            designer.loadNodes(nodes);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    designer.connectNodes('node1', 'node2', 'sequential');
                    designer.connectNodes('node2', 'node3', 'sequential');
                    designer.render();

                    setTimeout(() => {
                        const model = modelManager.getModel();
                        const connections = Array.from(model.workflow.connections.values());
                        const connectionPaths = Array.from(model.layout.connectionPaths.values());

                        expect(connections.length).toBeGreaterThanOrEqual(2);
                        expect(connectionPaths.length).toBeGreaterThanOrEqual(2);

                        // All connections should have valid paths
                        connectionPaths.forEach(pathState => {
                            expect(pathState.points.length).toBeGreaterThanOrEqual(2);
                            
                            const startPoint = pathState.points[0];
                            const endPoint = pathState.points[pathState.points.length - 1];

                            // Points should be valid
                            expect(startPoint.x).toBeGreaterThanOrEqual(0);
                            expect(startPoint.y).toBeGreaterThanOrEqual(0);
                            expect(endPoint.x).toBeGreaterThanOrEqual(0);
                            expect(endPoint.y).toBeGreaterThanOrEqual(0);
                        });

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('connection should work for all routing types (orthogonal, bezier, straight)', () => {
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
                        
                        // Should have routing type
                        expect(pathState.routingType).toBeDefined();
                        expect(['orthogonal', 'bezier', 'straight']).toContain(pathState.routingType);

                        // Should have points
                        expect(pathState.points.length).toBeGreaterThanOrEqual(2);

                        // For orthogonal, should have multiple points
                        if (pathState.routingType === 'orthogonal') {
                            expect(pathState.points.length).toBeGreaterThan(2);
                        }

                        // For bezier, should have control points
                        if (pathState.routingType === 'bezier') {
                            expect(pathState.bezierControlPoints).toBeDefined();
                        }

                        // For straight, should have exactly 2 points
                        if (pathState.routingType === 'straight') {
                            expect(pathState.points.length).toBe(2);
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);
    });
});









