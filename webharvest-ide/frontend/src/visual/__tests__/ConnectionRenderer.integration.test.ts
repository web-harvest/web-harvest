/**
 * Integration tests for ConnectionRenderer
 * Tests rendering with real VisualSemanticModel
 */

import { ConnectionRenderer } from '../ConnectionRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import type { VisualNode } from '../../types';

describe('ConnectionRenderer Integration', () => {
    let designer: VisualDesigner;
    let modelManager: VisualSemanticModelManager;
    let renderer: ConnectionRenderer;
    let svgElement: SVGSVGElement;
    let pluginManager: PluginManager;

    beforeEach(() => {
        // Create SVG element
        svgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement.setAttribute('width', '1000');
        svgElement.setAttribute('height', '1000');
        document.body.appendChild(svgElement);

        // Create mock plugin manager
        pluginManager = {
            getPlugins: () => [],
            getPlugin: () => null,
            getPluginMetadata: () => null
        } as any;

        // Create VisualDesigner
        const container = document.createElement('div');
        container.id = 'test-container';
        document.body.appendChild(container);
        designer = new VisualDesigner('test-container', pluginManager);

        // Create model manager
        modelManager = new VisualSemanticModelManager(designer, pluginManager);

        // Create renderer
        renderer = new ConnectionRenderer(svgElement, modelManager);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('should render connections when model has connections', () => {
        // Create test nodes
        const nodes: VisualNode[] = [
            {
                id: 'node1',
                type: 'http',
                attributes: { url: 'https://example.com' },
                position: { x: 100, y: 100 }
            },
            {
                id: 'node2',
                type: 'xpath',
                attributes: { expression: '//title' },
                position: { x: 300, y: 100 }
            }
        ];

        // Load nodes into designer
        designer.loadNodes(nodes);

        // Build model and render
        modelManager.buildModel();
        renderer.renderAll();

        // Check if connections were rendered
        const paths = svgElement.querySelectorAll('path.connection-line');
        const arrows = svgElement.querySelectorAll('polygon.connection-arrow');

        expect(paths.length).toBeGreaterThan(0);
        expect(arrows.length).toBeGreaterThan(0);
    });

    test('should handle empty model gracefully', () => {
        designer.loadNodes([]);
        modelManager.buildModel();
        
        expect(() => renderer.renderAll()).not.toThrow();
        
        const paths = svgElement.querySelectorAll('path.connection-line');
        expect(paths.length).toBe(0);
    });

    test('should update connections when model changes', () => {
        const nodes: VisualNode[] = [
            {
                id: 'node1',
                type: 'http',
                attributes: {},
                position: { x: 100, y: 100 }
            }
        ];

        designer.loadNodes(nodes);
        modelManager.buildModel();
        renderer.renderAll();

        const initialPaths = svgElement.querySelectorAll('path.connection-line').length;

        // Add another node
        const newNode: VisualNode = {
            id: 'node2',
            type: 'xpath',
            attributes: {},
            position: { x: 300, y: 100 }
        };
        designer.addNode(newNode);
        modelManager.buildModel();
        renderer.renderAll();

        const updatedPaths = svgElement.querySelectorAll('path.connection-line').length;
        expect(updatedPaths).toBeGreaterThan(initialPaths);
    });
});





