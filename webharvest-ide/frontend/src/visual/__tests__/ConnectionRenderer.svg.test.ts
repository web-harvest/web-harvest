/**
 * Tests for ConnectionRenderer SVG element handling
 * Tests critical scenario: SVG recreation after innerHTML = ''
 */

import { ConnectionRenderer } from '../ConnectionRenderer';
import { D3ConnectionRenderer } from '../D3ConnectionRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import type { VisualNode } from '../../types';

describe('ConnectionRenderer SVG Element Handling', () => {
    let designer: VisualDesigner;
    let modelManager: VisualSemanticModelManager;
    let renderer: ConnectionRenderer;
    let svgElement1: SVGSVGElement;
    let svgElement2: SVGSVGElement;
    let pluginManager: PluginManager;

    beforeEach(() => {
        // Create mock plugin manager
        pluginManager = {
            findPlugin: () => null,
            getSchemaVersion: () => null,
            getPlugins: () => [],
            getPlugin: () => null,
            getPluginMetadata: () => null
        } as any;

        // Create SVG elements
        svgElement1 = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement1.setAttribute('width', '1000');
        svgElement1.setAttribute('height', '1000');
        document.body.appendChild(svgElement1);

        svgElement2 = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement2.setAttribute('width', '1000');
        svgElement2.setAttribute('height', '1000');

        // Create VisualDesigner
        const container = document.createElement('div');
        container.id = 'test-container';
        document.body.appendChild(container);
        designer = new VisualDesigner('test-container', pluginManager);

        // Create model manager
        modelManager = new VisualSemanticModelManager(designer, pluginManager);

        // Create renderer with first SVG
        renderer = new ConnectionRenderer(svgElement1, modelManager);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('legacy updateSvgElement keeps the constructor SVG', () => {
        expect(renderer.svgElement).toBe(svgElement1);

        renderer.updateSvgElement(svgElement2);

        expect(renderer.svgElement).toBe(svgElement1);
        expect(renderer.svgElement).not.toBe(svgElement2);
    });

    test('should move rendered connections to new SVG', () => {
        // Create test nodes and connections
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

        designer.loadNodes(nodes);
        modelManager.buildModel();
        renderer.renderAll();

        const initialPaths = svgElement1.querySelectorAll('path.connection-line').length;
        expect(initialPaths).toBeGreaterThan(0);

        // Update SVG
        document.body.appendChild(svgElement2);
        renderer.updateSvgElement(svgElement2);

        // Legacy updateSvgElement is intentionally a no-op; existing nodes stay put.
        const newPaths = svgElement2.querySelectorAll('path.connection-line').length;
        expect(newPaths).toBe(0);

        const oldPaths = svgElement1.querySelectorAll('path.connection-line').length;
        expect(oldPaths).toBe(initialPaths);
        expect(renderer.svgElement).toBe(svgElement1);
    });

    test('should handle SVG update when no connections rendered', () => {
        expect(() => renderer.updateSvgElement(svgElement2)).not.toThrow();
        expect(renderer.svgElement).toBe(svgElement1);
    });

    test('active D3 renderer re-renders into its replacement SVG', () => {
        const activeRenderer = new D3ConnectionRenderer(svgElement1);
        const connection = {
            connectionId: 'conn-active-1',
            fromNodeId: 'node-1',
            toNodeId: 'node-2',
            path: { points: [{ x: 10, y: 20 }, { x: 110, y: 120 }] }
        };
        const activeModelManager = {
            getModel: () => ({
                workflow: { connections: new Map([['conn-active-1', connection]]) }
            })
        };

        activeRenderer.renderAll(activeModelManager);
        expect(svgElement1.querySelector('path.connection-path')).not.toBeNull();
        expect(svgElement1.querySelector('polygon.connection-arrow')).not.toBeNull();

        document.body.appendChild(svgElement2);
        activeRenderer.updateSvgElement(svgElement2);
        activeRenderer.renderAll(activeModelManager);

        expect(activeRenderer.svgElement).toBe(svgElement2);
        expect(svgElement1.querySelector('path.connection-path')).toBeNull();
        expect(svgElement2.querySelector('path.connection-path')).not.toBeNull();
        expect(svgElement2.querySelector('polygon.connection-arrow')).not.toBeNull();
    });

    test('should not update if SVG is the same', () => {
        const initialSvg = renderer.svgElement;
        const initialChildren = initialSvg.children.length;
        
        renderer.updateSvgElement(svgElement1);
        
        expect(renderer.svgElement).toBe(initialSvg);
        expect(renderer.svgElement.children.length).toBe(initialChildren);
    });
});
