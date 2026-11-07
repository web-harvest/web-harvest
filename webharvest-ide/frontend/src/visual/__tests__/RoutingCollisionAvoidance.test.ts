/**
 * Routing Collision Avoidance Tests
 * 
 * Testy sprawdzające, czy routing unika kolizji z węzłami
 */

import { describe, test, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
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

describe('Routing Collision Avoidance', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-routing';
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

        designer = new VisualDesigner('test-canvas-routing');
        designer.setPluginManager(pluginManager as any);
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Simple obstacle avoidance', () => {
        test('route should avoid single obstacle node in direct path', () => {
            // Arrange: node1 -> obstacle -> node2 (horizontal)
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 200 },
                children: []
            };

            const obstacle: VisualNode = {
                id: 'obstacle',
                type: 'http',
                attributes: { url: 'https://obstacle.com' },
                position: { x: 300, y: 200 }, // Directly between node1 and node2
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'http',
                attributes: { url: 'https://target.com' },
                position: { x: 500, y: 200 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(obstacle);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            // Act: Connect node1 to node2
            designer.createManualConnection('node1', 'node2', 'sequential');
            designer.render();

            // Assert: Path should not intersect obstacle
            const anchor1 = (designer as any).getConnectionAnchor('node1');
            const anchor2 = (designer as any).getConnectionAnchor('node2');
            const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

            const intersections = (designer as any).countNodeIntersections(
                geometry.points,
                designer.getNodeById('node1'),
                designer.getNodeById('node2')
            );

            expect(intersections).toBe(0);
        });

        test('route should avoid obstacle above direct path', () => {
            // Arrange: node1 -> node2 with obstacle above
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 300 },
                children: []
            };

            const obstacle: VisualNode = {
                id: 'obstacle',
                type: 'http',
                attributes: { url: 'https://obstacle.com' },
                position: { x: 300, y: 150 }, // Above the direct path
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'http',
                attributes: { url: 'https://target.com' },
                position: { x: 500, y: 300 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(obstacle);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.createManualConnection('node1', 'node2', 'sequential');
            designer.render();

            const anchor1 = (designer as any).getConnectionAnchor('node1');
            const anchor2 = (designer as any).getConnectionAnchor('node2');
            const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

            const intersections = (designer as any).countNodeIntersections(
                geometry.points,
                designer.getNodeById('node1'),
                designer.getNodeById('node2')
            );

            expect(intersections).toBe(0);
        });

        test('route should avoid obstacle below direct path', () => {
            // Arrange: node1 -> node2 with obstacle below
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 },
                children: []
            };

            const obstacle: VisualNode = {
                id: 'obstacle',
                type: 'http',
                attributes: { url: 'https://obstacle.com' },
                position: { x: 300, y: 250 }, // Below the direct path
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'http',
                attributes: { url: 'https://target.com' },
                position: { x: 500, y: 100 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(obstacle);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.createManualConnection('node1', 'node2', 'sequential');
            designer.render();

            const anchor1 = (designer as any).getConnectionAnchor('node1');
            const anchor2 = (designer as any).getConnectionAnchor('node2');
            const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

            const intersections = (designer as any).countNodeIntersections(
                geometry.points,
                designer.getNodeById('node1'),
                designer.getNodeById('node2')
            );

            expect(intersections).toBe(0);
        });
    });

    describe('Multiple obstacles avoidance', () => {
        test('route should avoid multiple obstacles', () => {
            // Arrange: node1 -> obstacle1 -> obstacle2 -> node2
            const node1: VisualNode = {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 200 },
                children: []
            };

            const obstacle1: VisualNode = {
                id: 'obstacle1',
                type: 'http',
                attributes: { url: 'https://obstacle1.com' },
                position: { x: 300, y: 150 },
                children: []
            };

            const obstacle2: VisualNode = {
                id: 'obstacle2',
                type: 'http',
                attributes: { url: 'https://obstacle2.com' },
                position: { x: 400, y: 250 },
                children: []
            };

            const node2: VisualNode = {
                id: 'node2',
                type: 'http',
                attributes: { url: 'https://target.com' },
                position: { x: 600, y: 200 },
                children: []
            };

            designer.addNode(node1);
            designer.addNode(obstacle1);
            designer.addNode(obstacle2);
            designer.addNode(node2);
            designer.render();

            const modelManager = (designer as any).modelManager;
            if (modelManager) {
                modelManager.buildModel();
            }

            designer.createManualConnection('node1', 'node2', 'sequential');
            designer.render();

            const anchor1 = (designer as any).getConnectionAnchor('node1');
            const anchor2 = (designer as any).getConnectionAnchor('node2');
            const geometry = (designer as any).computeConnectionGeometry(anchor1, anchor2, 'sequential');

            const intersections = (designer as any).countNodeIntersections(
                geometry.points,
                designer.getNodeById('node1'),
                designer.getNodeById('node2')
            );

            expect(intersections).toBe(0);
        });
    });
});









