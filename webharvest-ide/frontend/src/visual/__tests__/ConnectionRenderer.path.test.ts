/**
 * Tests for ConnectionRenderer path creation
 * Verifies that paths are created correctly from model data
 */

import { ConnectionRenderer } from '../ConnectionRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import type { VisualNode } from '../../types';
import type { VisualConnectionState } from '../VisualSemanticModel';

describe('ConnectionRenderer Path Creation', () => {
    let designer: VisualDesigner;
    let modelManager: VisualSemanticModelManager;
    let renderer: ConnectionRenderer;
    let svgElement: SVGSVGElement;
    let pluginManager: PluginManager;

    beforeEach(() => {
        svgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement.setAttribute('width', '1000');
        svgElement.setAttribute('height', '1000');
        document.body.appendChild(svgElement);

        pluginManager = {
            getPlugins: () => [],
            getPlugin: () => null,
            getPluginMetadata: () => null
        } as any;

        const container = document.createElement('div');
        container.id = 'test-container';
        document.body.appendChild(container);
        designer = new VisualDesigner('test-container', pluginManager);
        modelManager = new VisualSemanticModelManager(designer, pluginManager);
        renderer = new ConnectionRenderer(svgElement, modelManager);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('should create path element with correct attributes', () => {
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

        const paths = svgElement.querySelectorAll('path.connection-line');
        expect(paths.length).toBeGreaterThan(0);

        paths.forEach(path => {
            expect(path.getAttribute('class')).toBe('connection-line');
            expect(path.getAttribute('data-connection-id')).toBeTruthy();
            expect(path.getAttribute('d')).toBeTruthy();
            expect(path.getAttribute('stroke')).toBeTruthy();
        });
    });

    test('should handle connections with different routing types', () => {
        const nodes: VisualNode[] = [
            {
                id: 'node1',
                type: 'http',
                attributes: {},
                position: { x: 50, y: 50 }
            },
            {
                id: 'node2',
                type: 'xpath',
                attributes: {},
                position: { x: 250, y: 50 }
            }
        ];

        designer.loadNodes(nodes);
        modelManager.buildModel();
        renderer.renderAll();

        const paths = svgElement.querySelectorAll('path.connection-line');
        expect(paths.length).toBeGreaterThan(0);

        // All paths should have valid 'd' attribute
        paths.forEach(path => {
            const d = path.getAttribute('d');
            expect(d).toBeTruthy();
            expect(d!.length).toBeGreaterThan(0);
            expect(d!.startsWith('M')).toBe(true);
        });
    });

    test('should create arrow elements for each connection', () => {
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

        const paths = svgElement.querySelectorAll('path.connection-line');
        const arrows = svgElement.querySelectorAll('polygon.connection-arrow');

        expect(arrows.length).toBe(paths.length);
        
        arrows.forEach(arrow => {
            expect(arrow.getAttribute('class')).toBe('connection-arrow');
            expect(arrow.getAttribute('data-connection-id')).toBeTruthy();
            expect(arrow.getAttribute('points')).toBeTruthy();
        });
    });
});

