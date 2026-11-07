/**
 * Detailed Routing Collision Avoidance Tests
 * 
 * Testy TDD dla routingu z unikaniem kolizji
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

describe('Routing Collision Avoidance - Detailed TDD', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas-detailed';
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

        designer = new VisualDesigner('test-canvas-detailed');
        designer.setPluginManager(pluginManager as any);
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    test('route should avoid obstacle in direct horizontal path - TDD', () => {
        // Arrange: node1 (100, 200) -> obstacle (300, 200) -> node2 (500, 200)
        // Connection points: node1 bottom ~(200, 300), node2 top ~(400, 200)
        // Obstacle bounds: (300, 200) to (500, 300)
        // Expected: Path should go around obstacle (left or right)
        
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
            position: { x: 300, y: 200 },
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

        // Debug: Log path points
        console.log('Path points:', geometry.points);
        
        // Get obstacle bounds from model
        const model = modelManager.getModel();
        const obstacleState = model.workflow.allNodes.get('obstacle');
        if (obstacleState?.computedBounds) {
            console.log('Obstacle bounds:', obstacleState.computedBounds);
        }

        const intersections = (designer as any).countNodeIntersections(
            geometry.points,
            designer.getNodeById('node1'),
            designer.getNodeById('node2')
        );

        expect(intersections).toBe(0);
    });

    test('route should avoid multiple obstacles - TDD', () => {
        // Arrange: node1 (100, 200) -> obstacle1 (300, 150) + obstacle2 (400, 250) -> node2 (600, 200)
        // Expected: Path should avoid both obstacles
        
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









