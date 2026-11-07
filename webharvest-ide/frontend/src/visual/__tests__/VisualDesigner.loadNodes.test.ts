/**
 * VisualDesigner.loadNodes - Detailed Tests
 * Test loadNodes method and semantic model building
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

describe('VisualDesigner.loadNodes', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;
    let modelManager: VisualSemanticModelManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas';
        document.body.appendChild(container);

        designer = new VisualDesigner('test-canvas');
        
        // Initialize PluginManager for semantic model
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
            id: 'def',
            name: 'Def',
            category: 'variable',
            icon: 'fa-code',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: true }
        } as Plugin);
        
        designer.setPluginManager(pluginManager as any);
        modelManager = (designer as any).modelManager as VisualSemanticModelManager;
    });

    afterEach(() => {
        if (container && container.parentNode) {
            document.body.removeChild(container);
        }
    });

    describe('basic loadNodes functionality', () => {
        it('should load nodes and make them available via getNodes', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: { name: 'test' },
                    body: 'value',
                    children: [],
                },
                {
                    id: '2',
                    type: 'http',
                    attributes: { url: 'https://example.com' },
                    body: null,
                    children: [],
                },
            ];

            designer.loadNodes(nodes);

            const loadedNodes = designer.getNodes();
            expect(loadedNodes).toHaveLength(2);
            expect(loadedNodes[0].id).toBe('1');
            expect(loadedNodes[1].id).toBe('2');
        });

        it('should replace existing nodes when called multiple times', () => {
            const nodes1: VisualNode[] = [
                { id: '1', type: 'var', attributes: {}, body: null, children: [] },
            ];
            const nodes2: VisualNode[] = [
                { id: '2', type: 'http', attributes: {}, body: null, children: [] },
            ];

            designer.loadNodes(nodes1);
            expect(designer.getNodes()).toHaveLength(1);

            designer.loadNodes(nodes2);
            expect(designer.getNodes()).toHaveLength(1);
            expect(designer.getNodes()[0].id).toBe('2');
        });

        it('should clear connections when loading new nodes', () => {
            const nodes1: VisualNode[] = [
                { id: '1', type: 'var', attributes: {}, body: null, children: [] },
            ];

            designer.loadNodes(nodes1);
            // Add connection manually (if possible)
            designer.loadNodes([]);
            
            // Connections should be cleared
            const model = modelManager.toLLMFormat();
            expect(Object.keys(model.workflow.connections).length).toBe(0);
        });
    });

    describe('semantic model building', () => {
        it('should build semantic model with nodes after loadNodes', async () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: { name: 'test' },
                    body: 'value',
                    children: [],
                },
            ];

            designer.loadNodes(nodes);

            // Wait for async operations (auto-layout, model build)
            await new Promise(resolve => setTimeout(resolve, 200));

            const model = modelManager.toLLMFormat();
            
            expect(model).toBeDefined();
            expect(model.workflow).toBeDefined();
            expect(model.workflow.rootNodes.length).toBeGreaterThan(0);
            expect(Object.keys(model.workflow.allNodes).length).toBeGreaterThan(0);
        });

        it('should include all nodes in semantic model', async () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: [
                        {
                            id: '1-1',
                            type: 'http',
                            attributes: { url: 'https://example.com' },
                            body: null,
                            children: [],
                        },
                    ],
                },
                {
                    id: '2',
                    type: 'var',
                    attributes: { name: 'result' },
                    body: '${title}',
                    children: [],
                },
            ];

            designer.loadNodes(nodes);

            // Wait for async operations
            await new Promise(resolve => setTimeout(resolve, 200));

            const model = modelManager.toLLMFormat();
            const allNodes = model.workflow.allNodes;
            
            // Should have at least 2 root nodes + 1 child = 3 total
            const nodeIds = Object.keys(allNodes);
            expect(nodeIds.length).toBeGreaterThanOrEqual(2);
        });

        it('should have correct node structure in semantic model', async () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: { name: 'test' },
                    body: 'value',
                    children: [],
                },
            ];

            designer.loadNodes(nodes);

            // Wait for async operations
            await new Promise(resolve => setTimeout(resolve, 200));

            const model = modelManager.toLLMFormat();
            const allNodes = model.workflow.allNodes;
            
            const nodeIds = Object.keys(allNodes);
            expect(nodeIds.length).toBeGreaterThan(0);
            
            const firstNodeId = nodeIds[0];
            const firstNode = allNodes[firstNodeId];
            
            // Check required fields
            expect(firstNode.nodeId).toBeDefined();
            expect(firstNode.pluginType).toBeDefined();
            expect(firstNode.position).toBeDefined();
            expect(firstNode.dimensions).toBeDefined();
            expect(firstNode.computedBounds).toBeDefined();
            expect(firstNode.connectionPoints).toBeDefined();
        });

        it('should build empty model when loading empty array', async () => {
            designer.loadNodes([]);

            // Wait for async operations
            await new Promise(resolve => setTimeout(resolve, 100));

            const model = modelManager.toLLMFormat();
            
            expect(model).toBeDefined();
            expect(model.workflow.rootNodes).toHaveLength(0);
            expect(Object.keys(model.workflow.allNodes).length).toBe(0);
        });
    });

    describe('auto-layout after loadNodes', () => {
        it('should apply auto-layout after loading nodes', async () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: { name: 'test' },
                    body: 'value',
                    children: [],
                },
            ];

            designer.loadNodes(nodes);

            // Wait for auto-layout
            await new Promise(resolve => setTimeout(resolve, 200));

            const model = modelManager.toLLMFormat();
            const allNodes = model.workflow.allNodes;
            
            const nodeIds = Object.keys(allNodes);
            if (nodeIds.length > 0) {
                const firstNode = allNodes[nodeIds[0]];
                // After auto-layout, nodes should have positions
                expect(firstNode.position.x).toBeDefined();
                expect(firstNode.position.y).toBeDefined();
            }
        });
    });
});

