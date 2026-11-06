/**
 * VisualSemanticModelManager - Arrow Metadata Tests
 * 
 * Testy sprawdzające, że arrowMetadata jest poprawnie obliczane w buildConnectionStates
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import type { VisualNode, Plugin } from '../types';
import type { PluginManager } from '../../core/PluginManager';

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

describe('VisualSemanticModelManager - Arrow Metadata', () => {
    let designer: VisualDesigner;
    let modelManager: VisualSemanticModelManager;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-arrow-metadata';
        container.style.width = '2000px';
        container.style.height = '2000px';
        document.body.appendChild(container);

        pluginManager = new MockPluginManager() as any;
        pluginManager.addPlugin({
            id: 'var',
            name: 'Var',
            category: 'variable',
            icon: 'fa-code',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);

        designer = new VisualDesigner('test-canvas-arrow-metadata');
        designer.setPluginManager(pluginManager as any);
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('buildConnectionStates - arrowMetadata validation', () => {
        it('should always have pointsTowardsNode=true in arrowMetadata', async () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'var',
                    attributes: { name: 'a' },
                    body: 'value1',
                    children: [],
                    position: { x: 0, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'b' },
                    body: 'value2',
                    children: [],
                    position: { x: 400, y: 100 } // To the right
                },
                {
                    id: 'node3',
                    type: 'var',
                    attributes: { name: 'c' },
                    body: 'value3',
                    children: [],
                    position: { x: 0, y: 100 } // To the left of node2
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            // Wait for render
            await new Promise(resolve => setTimeout(resolve, 200));

            // Connect nodes
            designer.connectNodes('node1', 'node2', 'sequential');
            designer.connectNodes('node2', 'node3', 'sequential'); // Reverse connection

            // Wait for connections
            await new Promise(resolve => setTimeout(resolve, 200));

            const model = modelManager.buildModel();
            const connections = Array.from(model.workflow.connections.values());

            expect(connections.length).toBeGreaterThan(0);

            connections.forEach(conn => {
                const arrowMetadata = conn.path.arrowMetadata;
                expect(arrowMetadata).toBeDefined();
                if (!arrowMetadata?.pointsTowardsNode) {
                    throw new Error(`Connection ${conn.connectionId} should have pointsTowardsNode=true, but got ${arrowMetadata?.pointsTowardsNode}`);
                }
                expect(arrowMetadata?.pointsTowardsNode).toBe(true);
                
                const edgeValidation = conn.edgeValidation;
                if (!edgeValidation.arrowPointsTowardsTarget) {
                    throw new Error(`Connection ${conn.connectionId} should have arrowPointsTowardsTarget=true, but got ${edgeValidation.arrowPointsTowardsTarget}`);
                }
                expect(edgeValidation.arrowPointsTowardsTarget).toBe(true);
            });
        });

        it('should calculate arrowMetadata for all connection types', async () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'var',
                    attributes: { name: 'a' },
                    body: 'value1',
                    children: [],
                    position: { x: 100, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'b' },
                    body: 'value2',
                    children: [],
                    position: { x: 100, y: 300 } // Below
                },
                {
                    id: 'node3',
                    type: 'var',
                    attributes: { name: 'c' },
                    body: 'value3',
                    children: [],
                    position: { x: 300, y: 100 } // To the right
                },
                {
                    id: 'node4',
                    type: 'var',
                    attributes: { name: 'd' },
                    body: 'value4',
                    children: [],
                    position: { x: 0, y: 100 } // To the left
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            await new Promise(resolve => setTimeout(resolve, 200));

            // Connect in all directions
            designer.connectNodes('node1', 'node2', 'sequential'); // Down
            designer.connectNodes('node1', 'node3', 'sequential'); // Right
            designer.connectNodes('node4', 'node1', 'sequential'); // Right (reverse)

            await new Promise(resolve => setTimeout(resolve, 200));

            const model = modelManager.buildModel();
            const connections = Array.from(model.workflow.connections.values());

            expect(connections.length).toBe(3);

            connections.forEach(conn => {
                const arrowMetadata = conn.path.arrowMetadata;
                expect(arrowMetadata).toBeDefined();
                expect(arrowMetadata?.pointsTowardsNode).toBe(true);
                expect(arrowMetadata?.angle).toBeDefined();
                expect(arrowMetadata?.direction).toBeDefined();
                expect(arrowMetadata?.endPoint).toBeDefined();
            });
        });

        it('should handle edge case where path has only 2 points', async () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node1',
                    type: 'var',
                    attributes: { name: 'a' },
                    body: 'value1',
                    children: [],
                    position: { x: 0, y: 100 }
                },
                {
                    id: 'node2',
                    type: 'var',
                    attributes: { name: 'b' },
                    body: 'value2',
                    children: [],
                    position: { x: 200, y: 100 } // Close, might create short path
                }
            ];

            designer.loadNodes(nodes);
            designer.render();

            await new Promise(resolve => setTimeout(resolve, 200));

            designer.connectNodes('node1', 'node2', 'sequential');

            await new Promise(resolve => setTimeout(resolve, 200));

            const model = modelManager.buildModel();
            const connections = Array.from(model.workflow.connections.values());

            expect(connections.length).toBe(1);

            const conn = connections[0];
            const arrowMetadata = conn.path.arrowMetadata;
            expect(arrowMetadata).toBeDefined();
            expect(arrowMetadata?.pointsTowardsNode).toBe(true);
        });
    });
});

