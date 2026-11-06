/**
 * Diagnostic tests for ConnectionRenderer
 * Tests edge cases and error conditions
 */

import { ConnectionRenderer } from '../ConnectionRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import type { VisualNode } from '../../types';

describe('ConnectionRenderer Diagnostics', () => {
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

    test('should handle SVG element change', () => {
        // Create new SVG
        const newSvg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        newSvg.setAttribute('width', '2000');
        newSvg.setAttribute('height', '2000');
        document.body.appendChild(newSvg);

        // Render some connections first
        const nodes: VisualNode[] = [
            { id: 'node1', type: 'http', attributes: {}, position: { x: 100, y: 100 } },
            { id: 'node2', type: 'xpath', attributes: {}, position: { x: 300, y: 100 } }
        ];
        designer.loadNodes(nodes);
        modelManager.buildModel();
        renderer.renderAll();

        const initialConnections = renderer.renderedConnections.size;
        expect(initialConnections).toBeGreaterThan(0);

        // Update SVG
        renderer.updateSvgElement(newSvg);

        // Verify connections moved to new SVG
        expect(renderer.svgElement).toBe(newSvg);
        expect(newSvg.children.length).toBe(initialConnections * 2); // paths + arrows
        expect(renderer.renderedConnections.size).toBe(initialConnections);
    });

    test('should handle empty model gracefully', () => {
        designer.loadNodes([]);
        modelManager.buildModel();
        
        expect(() => renderer.renderAll()).not.toThrow();
        expect(svgElement.children.length).toBe(0);
    });

    test('should handle model with no connections', () => {
        const nodes: VisualNode[] = [
            { id: 'node1', type: 'http', attributes: {}, position: { x: 100, y: 100 } }
        ];
        designer.loadNodes(nodes);
        modelManager.buildModel();
        
        expect(() => renderer.renderAll()).not.toThrow();
        // Single node has no connections
        expect(svgElement.querySelectorAll('path.connection-line').length).toBe(0);
    });

    test('should handle invalid connection state', () => {
        // This test verifies that renderer handles missing path.points gracefully
        const nodes: VisualNode[] = [
            { id: 'node1', type: 'http', attributes: {}, position: { x: 100, y: 100 } },
            { id: 'node2', type: 'xpath', attributes: {}, position: { x: 300, y: 100 } }
        ];
        designer.loadNodes(nodes);
        modelManager.buildModel();
        
        // Manually corrupt a connection state (simulate error)
        const model = modelManager.getModel();
        if (model && model.workflow.connections.size > 0) {
            const firstConnId = Array.from(model.workflow.connections.keys())[0];
            const conn = model.workflow.connections.get(firstConnId);
            if (conn) {
                // Corrupt path.points
                (conn.path as any).points = null;
            }
        }
        
        // Should not throw, should handle gracefully
        expect(() => renderer.renderAll()).not.toThrow();
    });

    test('should verify SVG element is in DOM', () => {
        const nodes: VisualNode[] = [
            { id: 'node1', type: 'http', attributes: {}, position: { x: 100, y: 100 } },
            { id: 'node2', type: 'xpath', attributes: {}, position: { x: 300, y: 100 } }
        ];
        designer.loadNodes(nodes);
        modelManager.buildModel();
        renderer.renderAll();

        // Verify SVG is in DOM
        expect(renderer.svgElement.parentElement).not.toBeNull();
        expect(document.body.contains(renderer.svgElement)).toBe(true);
    });

    test('should handle renderAll called multiple times', () => {
        const nodes: VisualNode[] = [
            { id: 'node1', type: 'http', attributes: {}, position: { x: 100, y: 100 } },
            { id: 'node2', type: 'xpath', attributes: {}, position: { x: 300, y: 100 } }
        ];
        designer.loadNodes(nodes);
        modelManager.buildModel();
        
        // Call renderAll multiple times
        renderer.renderAll();
        const firstRender = svgElement.children.length;
        
        renderer.renderAll();
        const secondRender = svgElement.children.length;
        
        // Should not duplicate connections
        expect(secondRender).toBe(firstRender);
    });
});

