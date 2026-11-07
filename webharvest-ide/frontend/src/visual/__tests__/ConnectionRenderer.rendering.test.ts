/**
 * Unit tests for ConnectionRenderer rendering logic
 * Tests that connections are correctly rendered to SVG
 */

import { ConnectionRenderer } from '../ConnectionRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import type { VisualNode } from '../../types';
import type { VisualSemanticModel } from '../VisualSemanticModel';

describe('ConnectionRenderer Rendering', () => {
    let svgElement: SVGSVGElement;
    let modelManager: VisualSemanticModelManager;
    let renderer: ConnectionRenderer;
    let designer: VisualDesigner;
    let pluginManager: PluginManager;

    beforeEach(() => {
        // Create SVG element
        svgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement.setAttribute('width', '1000');
        svgElement.setAttribute('height', '1000');
        document.body.appendChild(svgElement);

        // Create mock plugin manager
        pluginManager = {
            findPlugin: () => null,
            getSchemaVersion: () => null,
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

    test('should render connections when model has valid path points', () => {
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
        expect(paths.length).toBe(arrows.length); // Each connection has path + arrow
    });

    test('should handle SVG element change correctly', () => {
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

        // Create new SVG element
        const newSvgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        newSvgElement.setAttribute('width', '1000');
        newSvgElement.setAttribute('height', '1000');
        document.body.appendChild(newSvgElement);

        // Update renderer to use new SVG
        renderer.updateSvgElement(newSvgElement);

        // Re-render
        renderer.renderAll();

        const newPaths = newSvgElement.querySelectorAll('path.connection-line').length;
        expect(newPaths).toBe(0);
        expect(svgElement.querySelectorAll('path.connection-line').length).toBe(initialPaths);
        expect(renderer.svgElement).toBe(svgElement);
    });

    test('should clear all connections when clearAll is called', () => {
        const nodes: VisualNode[] = [
            {
                id: 'node1',
                type: 'http',
                attributes: {},
                position: { x: 100, y: 100 }
            },
            {
                id: 'node2',
                type: 'xpath',
                attributes: {},
                position: { x: 300, y: 100 }
            }
        ];

        designer.loadNodes(nodes);
        modelManager.buildModel();
        renderer.renderAll();

        const beforeClear = svgElement.querySelectorAll('path.connection-line').length;
        expect(beforeClear).toBeGreaterThan(0);

        renderer.clearAll();

        const afterClear = svgElement.querySelectorAll('path.connection-line').length;
        expect(afterClear).toBe(0);
        expect(renderer.renderedConnections.size).toBe(0);
    });

    test('should handle empty model gracefully', () => {
        designer.loadNodes([]);
        modelManager.buildModel();
        
        expect(() => renderer.renderAll()).not.toThrow();
        
        const paths = svgElement.querySelectorAll('path.connection-line');
        expect(paths.length).toBe(0);
    });

    test('should render connections with correct attributes', () => {
        const nodes: VisualNode[] = [
            {
                id: 'node1',
                type: 'http',
                attributes: {},
                position: { x: 100, y: 100 }
            },
            {
                id: 'node2',
                type: 'xpath',
                attributes: {},
                position: { x: 300, y: 100 }
            }
        ];

        designer.loadNodes(nodes);
        modelManager.buildModel();
        renderer.renderAll();

        const path = svgElement.querySelector('path.connection-line') as SVGPathElement;
        expect(path).toBeTruthy();
        expect(path.getAttribute('class')).toBe('connection-line');
        expect(path.getAttribute('data-connection-id')).toBeTruthy();
        expect(path.getAttribute('d')).toBeTruthy();
        expect(path.getAttribute('stroke')).toBeTruthy();
    });
});

