/**
 * Semantic Model Tests
 * 
 * Testy sprawdzające poprawność budowania i działania Visual Semantic Model
 */

import { describe, test, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import type { VisualNode, Plugin } from '../../types';

class MockPluginManager {
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

describe('Visual Semantic Model', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;
    let modelManager: VisualSemanticModelManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-semantic';
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
            id: 'file',
            name: 'File',
            category: 'file',
            icon: 'fa-file',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);

        designer = new VisualDesigner('test-canvas-semantic');
        designer.setPluginManager(pluginManager as any);
        
        // Get model manager
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Model Building', () => {
        test('model should be built after adding nodes', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const model = modelManager.getModel();
            expect(model).toBeTruthy();
            expect(model.workflow.allNodes.has('node1')).toBe(true);
        });

        test('model should have computed bounds for nodes', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const model = modelManager.getModel();
            const nodeState = model.workflow.allNodes.get('node1');
            
            expect(nodeState).toBeTruthy();
            expect(nodeState?.computedBounds).toBeTruthy();
            
            if (nodeState?.computedBounds) {
                expect(nodeState.computedBounds.left).toBeCloseTo(100, 1);
                expect(nodeState.computedBounds.top).toBeCloseTo(100, 1);
                expect(nodeState.computedBounds.right).toBeGreaterThan(100);
                expect(nodeState.computedBounds.bottom).toBeGreaterThan(100);
            }
        });

        test('model should have connection points for nodes', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            const model = modelManager.getModel();
            const nodeState = model.workflow.allNodes.get('node1');
            
            expect(nodeState).toBeTruthy();
            expect(nodeState?.connectionPoints).toBeTruthy();
            expect(nodeState?.connectionPoints.inputs.length + nodeState?.connectionPoints.outputs.length).toBeGreaterThan(0);
        });
    });

    describe('Connection Points Cache', () => {
        test('connection points should be cached after buildModel', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            modelManager.buildModel();

            // Check cache
            const connectionPoint = modelManager.getConnectionPointForSide('node1', 'right', 'output');
            expect(connectionPoint).toBeTruthy();
            
            const absolutePosition = connectionPoint 
                ? modelManager.getConnectionPointAbsolutePosition('node1', connectionPoint.id)
                : null;
            expect(absolutePosition).toBeTruthy();
        });

        test('connection points should be updated when node moves', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            modelManager.buildModel();

            // Get initial position
            const point1 = modelManager.getConnectionPointForSide('node1', 'right', 'output');
            const absolute1 = point1 ? modelManager.getConnectionPointAbsolutePosition('node1', point1.id) : null;
            expect(absolute1).toBeTruthy();

            // Move node
            (designer as any).moveNode('node1', { x: 200, y: 200 }, { skipRender: false });
            modelManager.buildModel();

            // Get updated position
            const point2 = modelManager.getConnectionPointForSide('node1', 'right', 'output');
            const absolute2 = point2 ? modelManager.getConnectionPointAbsolutePosition('node1', point2.id) : null;
            expect(absolute2).toBeTruthy();

            if (absolute1 && absolute2) {
                // Should be shifted by (100, 100)
                expect(Math.abs(absolute2.x - absolute1.x - 100)).toBeLessThan(1);
                expect(Math.abs(absolute2.y - absolute1.y - 100)).toBeLessThan(1);
            }
        });

        test('connection points should be available for all sides', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            modelManager.buildModel();

            // Check all sides
            const topPoint = modelManager.getConnectionPointForSide('node1', 'top', 'input');
            const bottomPoint = modelManager.getConnectionPointForSide('node1', 'bottom', 'output');
            const leftPoint = modelManager.getConnectionPointForSide('node1', 'left', 'input');
            const rightPoint = modelManager.getConnectionPointForSide('node1', 'right', 'output');

            expect(topPoint).toBeTruthy();
            expect(bottomPoint).toBeTruthy();
            expect(leftPoint).toBeTruthy();
            expect(rightPoint).toBeTruthy();
        });

        test('connection points should be at center of edges', () => {
            const node: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            designer.addNode(node);
            designer.render();

            modelManager.buildModel();

            const model = modelManager.getModel();
            const nodeState = model.workflow.allNodes.get('node1');
            expect(nodeState).toBeTruthy();

            if (nodeState?.computedBounds) {
                const cb = nodeState.computedBounds;
                const centerX = cb.left + (cb.right - cb.left) / 2;
                const centerY = cb.top + (cb.bottom - cb.top) / 2;

                // Check top point
                const topPoint = modelManager.getConnectionPointForSide('node1', 'top', 'input');
                if (topPoint) {
                    const topAbsolute = modelManager.getConnectionPointAbsolutePosition('node1', topPoint.id);
                    if (topAbsolute) {
                        expect(topAbsolute.x).toBeCloseTo(centerX, 1);
                        expect(topAbsolute.y).toBeCloseTo(cb.top, 1);
                    }
                }

                // Check bottom point
                const bottomPoint = modelManager.getConnectionPointForSide('node1', 'bottom', 'output');
                if (bottomPoint) {
                    const bottomAbsolute = modelManager.getConnectionPointAbsolutePosition('node1', bottomPoint.id);
                    if (bottomAbsolute) {
                        expect(bottomAbsolute.x).toBeCloseTo(centerX, 1);
                        expect(bottomAbsolute.y).toBeCloseTo(cb.bottom, 1);
                    }
                }

                // Check left point
                const leftPoint = modelManager.getConnectionPointForSide('node1', 'left', 'input');
                if (leftPoint) {
                    const leftAbsolute = modelManager.getConnectionPointAbsolutePosition('node1', leftPoint.id);
                    if (leftAbsolute) {
                        expect(leftAbsolute.x).toBeCloseTo(cb.left, 1);
                        expect(leftAbsolute.y).toBeCloseTo(centerY, 1);
                    }
                }

                // Check right point
                const rightPoint = modelManager.getConnectionPointForSide('node1', 'right', 'output');
                if (rightPoint) {
                    const rightAbsolute = modelManager.getConnectionPointAbsolutePosition('node1', rightPoint.id);
                    if (rightAbsolute) {
                        expect(rightAbsolute.x).toBeCloseTo(cb.right, 1);
                        expect(rightAbsolute.y).toBeCloseTo(centerY, 1);
                    }
                }
            }
        });
    });

    describe('Model After Auto-Layout', () => {
        test('model should be built after auto-layout', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            // Apply auto-layout
            (designer as any).applyAutoLayoutToRootNodes(designer.getNodes());
            designer.render();

            // Build model
            modelManager.buildModel();

            const model = modelManager.getModel();
            expect(model.workflow.allNodes.has('node1')).toBe(true);
            expect(model.workflow.allNodes.has('node2')).toBe(true);

            // Check positions
            const node1State = model.workflow.allNodes.get('node1');
            const node2State = model.workflow.allNodes.get('node2');

            expect(node1State?.computedBounds).toBeTruthy();
            expect(node2State?.computedBounds).toBeTruthy();
        });

        test('connection points should be available after auto-layout', () => {
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'file',
                attributes: { path: '/tmp/test' },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(node2);
            designer.render();

            // Apply auto-layout
            (designer as any).applyAutoLayoutToRootNodes(designer.getNodes());
            designer.render();

            // Build model
            modelManager.buildModel();

            // Check connection points
            const point1 = modelManager.getConnectionPointForSide('node1', 'bottom', 'output');
            const point2 = modelManager.getConnectionPointForSide('node2', 'top', 'input');

            expect(point1).toBeTruthy();
            expect(point2).toBeTruthy();

            // Check absolute positions
            if (point1) {
                const absolute1 = modelManager.getConnectionPointAbsolutePosition('node1', point1.id);
                expect(absolute1).toBeTruthy();
            }

            if (point2) {
                const absolute2 = modelManager.getConnectionPointAbsolutePosition('node2', point2.id);
                expect(absolute2).toBeTruthy();
            }
        });
    });
});

