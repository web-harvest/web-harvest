/**
 * Connection Points On Edges Tests - TDD
 * 
 * Testy weryfikujące, że connection points są dokładnie na krawędziach węzłów.
 * Tolerancja: < 1px
 */

import { describe, test, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { ConnectionPointsCalculator } from '../ConnectionPointsCalculator';
import type { VisualNode, Plugin } from '../../types';
import type { PluginManager } from '../../core/PluginManager';
import type { ConnectionPoint, Bounds, Side } from '../VisualSemanticModel';

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

describe('Connection Points On Edges - TDD', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;
    let modelManager: VisualSemanticModelManager;
    let connectionPointsCalculator: ConnectionPointsCalculator;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-edges';
        container.style.width = '2000px';
        container.style.height = '2000px';
        document.body.appendChild(container);

        pluginManager = new MockPluginManager() as any;

        // Add test plugins
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

        designer = new VisualDesigner('test-canvas-edges');
        designer.setPluginManager(pluginManager as any);
        
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
        connectionPointsCalculator = new ConnectionPointsCalculator();
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Connection points should be exactly on node edges', () => {
        test('connection points should be exactly on node edges (tolerance < 1px)', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    const model = modelManager.getModel();
                    const nodeState = model.workflow.allNodes.get('node1');

                    expect(nodeState).toBeTruthy();
                    expect(nodeState!.connectionPoints.inputs.length).toBeGreaterThan(0);
                    expect(nodeState!.connectionPoints.outputs.length).toBeGreaterThan(0);

                    const bounds = nodeState!.computedBounds;
                    const tolerance = 1; // 1px tolerance

                    // Check input points
                    nodeState!.connectionPoints.inputs.forEach(cp => {
                        const absPos = modelManager.getConnectionPointAbsolutePosition('node1', cp.id);
                        expect(absPos).toBeTruthy();
                        
                        const pos = absPos!;
                        const side = cp.side;

                        let onEdge = false;
                        switch (side) {
                            case 'left':
                                onEdge = Math.abs(pos.x - bounds.left) < tolerance;
                                break;
                            case 'right':
                                onEdge = Math.abs(pos.x - bounds.right) < tolerance;
                                break;
                            case 'top':
                                onEdge = Math.abs(pos.y - bounds.top) < tolerance;
                                break;
                            case 'bottom':
                                onEdge = Math.abs(pos.y - bounds.bottom) < tolerance;
                                break;
                        }

                        expect(onEdge).toBe(true);
                    });

                    // Check output points
                    nodeState!.connectionPoints.outputs.forEach(cp => {
                        const absPos = modelManager.getConnectionPointAbsolutePosition('node1', cp.id);
                        expect(absPos).toBeTruthy();
                        
                        const pos = absPos!;
                        const side = cp.side;

                        let onEdge = false;
                        switch (side) {
                            case 'left':
                                onEdge = Math.abs(pos.x - bounds.left) < tolerance;
                                break;
                            case 'right':
                                onEdge = Math.abs(pos.x - bounds.right) < tolerance;
                                break;
                            case 'top':
                                onEdge = Math.abs(pos.y - bounds.top) < tolerance;
                                break;
                            case 'bottom':
                                onEdge = Math.abs(pos.y - bounds.bottom) < tolerance;
                                break;
                        }

                        expect(onEdge).toBe(true);
                    });

                    resolve();
                }, 500);
            });
        }, 10000);

        test('connection points should be at center of edges (not offset)', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    const model = modelManager.getModel();
                    const nodeState = model.workflow.allNodes.get('node1');

                    expect(nodeState).toBeTruthy();
                    const bounds = nodeState!.computedBounds;

                    // Check that connection points are at center of edges
                    nodeState!.connectionPoints.inputs.forEach(cp => {
                        const absPos = modelManager.getConnectionPointAbsolutePosition('node1', cp.id);
                        expect(absPos).toBeTruthy();
                        
                        const pos = absPos!;
                        const side = cp.side;

                        switch (side) {
                            case 'left':
                            case 'right':
                                // Should be at center Y of edge
                                expect(Math.abs(pos.y - bounds.centerY)).toBeLessThan(5);
                                break;
                            case 'top':
                            case 'bottom':
                                // Should be at center X of edge
                                expect(Math.abs(pos.x - bounds.centerX)).toBeLessThan(5);
                                break;
                        }
                    });

                    nodeState!.connectionPoints.outputs.forEach(cp => {
                        const absPos = modelManager.getConnectionPointAbsolutePosition('node1', cp.id);
                        expect(absPos).toBeTruthy();
                        
                        const pos = absPos!;
                        const side = cp.side;

                        switch (side) {
                            case 'left':
                            case 'right':
                                // Should be at center Y of edge
                                expect(Math.abs(pos.y - bounds.centerY)).toBeLessThan(5);
                                break;
                            case 'top':
                            case 'bottom':
                                // Should be at center X of edge
                                expect(Math.abs(pos.x - bounds.centerX)).toBeLessThan(5);
                                break;
                        }
                    });

                    resolve();
                }, 500);
            });
        }, 10000);

        test('connection points should update when node position changes', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    const model1 = modelManager.getModel();
                    const nodeState1 = model1.workflow.allNodes.get('node1');
                    const initialPos = nodeState1!.connectionPoints.inputs[0];
                    const initialAbsPos = modelManager.getConnectionPointAbsolutePosition('node1', initialPos.id);

                    // Move node
                    (designer as any).moveNode('node1', { x: 200, y: 200 }, { skipRender: false });
                    designer.render();

                    setTimeout(() => {
                        const model2 = modelManager.getModel();
                        const nodeState2 = model2.workflow.allNodes.get('node1');
                        const updatedPos = nodeState2!.connectionPoints.inputs[0];
                        const updatedAbsPos = modelManager.getConnectionPointAbsolutePosition('node1', updatedPos.id);

                        // Position should be shifted by (100, 100)
                        expect(Math.abs(updatedAbsPos!.x - initialAbsPos!.x - 100)).toBeLessThan(1);
                        expect(Math.abs(updatedAbsPos!.y - initialAbsPos!.y - 100)).toBeLessThan(1);

                        // Should still be on edge
                        const bounds = nodeState2!.computedBounds;
                        const side = updatedPos.side;
                        let onEdge = false;
                        switch (side) {
                            case 'left':
                                onEdge = Math.abs(updatedAbsPos!.x - bounds.left) < 1;
                                break;
                            case 'right':
                                onEdge = Math.abs(updatedAbsPos!.x - bounds.right) < 1;
                                break;
                            case 'top':
                                onEdge = Math.abs(updatedAbsPos!.y - bounds.top) < 1;
                                break;
                            case 'bottom':
                                onEdge = Math.abs(updatedAbsPos!.y - bounds.bottom) < 1;
                                break;
                        }
                        expect(onEdge).toBe(true);

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('connection points should update when node size changes', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    const model1 = modelManager.getModel();
                    const nodeState1 = model1.workflow.allNodes.get('node1');
                    const initialBounds = nodeState1!.computedBounds;

                    // Resize node (simulate by changing dimensions)
                    const nodeElement = container.querySelector(`[data-node-id="node1"]`) as HTMLElement;
                    if (nodeElement) {
                        nodeElement.style.width = '300px';
                        nodeElement.style.height = '150px';
                        
                        // Trigger resize handler
                        (designer as any).handleNodeResize?.('node1', { width: 300, height: 150 });
                        designer.render();
                    }

                    setTimeout(() => {
                        const model2 = modelManager.getModel();
                        const nodeState2 = model2.workflow.allNodes.get('node1');
                        const updatedBounds = nodeState2!.computedBounds;

                        // Bounds should have changed
                        expect(updatedBounds.width).toBeGreaterThan(initialBounds.width);
                        expect(updatedBounds.height).toBeGreaterThan(initialBounds.height);

                        // Connection points should still be on edges
                        nodeState2!.connectionPoints.inputs.forEach(cp => {
                            const absPos = modelManager.getConnectionPointAbsolutePosition('node1', cp.id);
                            expect(absPos).toBeTruthy();
                            
                            const pos = absPos!;
                            const side = cp.side;

                            let onEdge = false;
                            switch (side) {
                                case 'left':
                                    onEdge = Math.abs(pos.x - updatedBounds.left) < 1;
                                    break;
                                case 'right':
                                    onEdge = Math.abs(pos.x - updatedBounds.right) < 1;
                                    break;
                                case 'top':
                                    onEdge = Math.abs(pos.y - updatedBounds.top) < 1;
                                    break;
                                case 'bottom':
                                    onEdge = Math.abs(pos.y - updatedBounds.bottom) < 1;
                                    break;
                            }

                            expect(onEdge).toBe(true);
                        });

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('all connection points should be accessible from model', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    const model = modelManager.getModel();
                    const nodeState = model.workflow.allNodes.get('node1');

                    expect(nodeState).toBeTruthy();
                    expect(nodeState!.connectionPoints.inputs.length).toBeGreaterThan(0);
                    expect(nodeState!.connectionPoints.outputs.length).toBeGreaterThan(0);

                    // All input points should be accessible
                    nodeState!.connectionPoints.inputs.forEach(cp => {
                        const absPos = modelManager.getConnectionPointAbsolutePosition('node1', cp.id);
                        expect(absPos).toBeTruthy();
                        expect(absPos!.x).toBeGreaterThanOrEqual(0);
                        expect(absPos!.y).toBeGreaterThanOrEqual(0);
                    });

                    // All output points should be accessible
                    nodeState!.connectionPoints.outputs.forEach(cp => {
                        const absPos = modelManager.getConnectionPointAbsolutePosition('node1', cp.id);
                        expect(absPos).toBeTruthy();
                        expect(absPos!.x).toBeGreaterThanOrEqual(0);
                        expect(absPos!.y).toBeGreaterThanOrEqual(0);
                    });

                    resolve();
                }, 500);
            });
        }, 10000);
    });
});









