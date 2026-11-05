/**
 * Arrow Rendering Tests - TDD
 * 
 * Testy weryfikujące, że strzałki są poprawnie renderowane i wskazują na etykiety.
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

describe('Arrow Rendering - TDD', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;
    let modelManager: VisualSemanticModelManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-arrow-render';
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

        designer = new VisualDesigner('test-canvas-arrow-render');
        designer.setPluginManager(pluginManager as any);
        
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Arrow positioning and direction', () => {
        test('arrow should be positioned exactly at connection point on edge', () => {
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

                        if (pathState.points.length > 0 && toNodeState) {
                            const lastPoint = pathState.points[pathState.points.length - 1];
                            const bounds = toNodeState.computedBounds;

                            // Last point should be on one of the edges (tolerance < 2px)
                            const onLeftEdge = Math.abs(lastPoint.x - bounds.left) < 2;
                            const onRightEdge = Math.abs(lastPoint.x - bounds.right) < 2;
                            const onTopEdge = Math.abs(lastPoint.y - bounds.top) < 2;
                            const onBottomEdge = Math.abs(lastPoint.y - bounds.bottom) < 2;

                            expect(onLeftEdge || onRightEdge || onTopEdge || onBottomEdge).toBe(true);

                            // Check if arrow element exists in DOM
                            const arrowElement = container.querySelector('.connection-arrow');
                            if (arrowElement) {
                                const arrowPoints = arrowElement.getAttribute('points');
                                expect(arrowPoints).toBeTruthy();
                                
                                // Parse arrow points
                                const points = arrowPoints!.split(' ').map(p => {
                                    const [x, y] = p.split(',').map(Number);
                                    return { x, y };
                                });

                                // Arrow tip should be at or very close to last point
                                const arrowTip = points[0]; // First point is usually the tip
                                const distance = Math.sqrt(
                                    Math.pow(arrowTip.x - lastPoint.x, 2) +
                                    Math.pow(arrowTip.y - lastPoint.y, 2)
                                );
                                expect(distance).toBeLessThan(5); // 5px tolerance
                            }
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow should point towards node center (not away)', () => {
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

                            // Calculate arrow direction (from second last to last)
                            const arrowDirection = {
                                x: lastPoint.x - secondLastPoint.x,
                                y: lastPoint.y - secondLastPoint.y
                            };

                            // Calculate direction from arrow tip to node center
                            const targetCenter = {
                                x: toNodeState.computedBounds.centerX,
                                y: toNodeState.computedBounds.centerY
                            };
                            const toCenterDirection = {
                                x: targetCenter.x - lastPoint.x,
                                y: targetCenter.y - lastPoint.y
                            };

                            // Dot product should be positive (arrow points towards center)
                            const dotProduct = arrowDirection.x * toCenterDirection.x + arrowDirection.y * toCenterDirection.y;
                            expect(dotProduct).toBeGreaterThan(0);
                        }

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow should be visible and correctly sized', () => {
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
                        const arrowElement = container.querySelector('.connection-arrow') as SVGPolygonElement;
                        expect(arrowElement).toBeTruthy();

                        // Arrow should have points
                        const points = arrowElement.getAttribute('points');
                        expect(points).toBeTruthy();
                        expect(points!.split(' ').length).toBeGreaterThanOrEqual(3); // At least 3 points for triangle

                        // Arrow should have fill color
                        const fill = arrowElement.getAttribute('fill');
                        expect(fill).toBeTruthy();
                        expect(fill).not.toBe('none');
                        expect(fill).not.toBe('transparent');

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow should match path color and style', () => {
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
                        const pathElement = container.querySelector('.connection-line') as SVGPathElement;
                        const arrowElement = container.querySelector('.connection-arrow') as SVGPolygonElement;

                        expect(pathElement).toBeTruthy();
                        expect(arrowElement).toBeTruthy();

                        const pathStroke = pathElement.getAttribute('stroke');
                        const arrowFill = arrowElement.getAttribute('fill');

                        // Arrow fill should match path stroke color
                        expect(arrowFill).toBe(pathStroke);

                        resolve();
                    }, 500);
                }, 500);
            });
        }, 15000);

        test('arrow should update when connection path changes', () => {
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
                        const arrowElement1 = container.querySelector('.connection-arrow') as SVGPolygonElement;
                        expect(arrowElement1).toBeTruthy();
                        const initialPoints1 = arrowElement1.getAttribute('points');

                        // Move target node
                        (designer as any).moveNode('node2', { x: 500, y: 200 }, { skipRender: false });
                        designer.render();

                        setTimeout(() => {
                            const arrowElement2 = container.querySelector('.connection-arrow') as SVGPolygonElement;
                            expect(arrowElement2).toBeTruthy();
                            const updatedPoints2 = arrowElement2.getAttribute('points');

                            // Arrow points should have changed
                            expect(updatedPoints2).not.toBe(initialPoints1);

                            resolve();
                        }, 500);
                    }, 500);
                }, 500);
            });
        }, 20000);
    });
});









