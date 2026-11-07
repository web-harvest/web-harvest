/**
 * ConnectionPointsRenderer - Unit Tests
 */

/// <reference types="jest" />

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { ConnectionPointsRenderer } from '../ConnectionPointsRenderer';
import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../../types';

describe('ConnectionPointsRenderer', () => {
    let renderer: ConnectionPointsRenderer;
    let svgElement: SVGSVGElement;
    let modelManager: VisualSemanticModelManager;
    let designer: VisualDesigner;

    beforeEach(() => {
        // Create SVG element
        svgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgElement.setAttribute('width', '1000');
        svgElement.setAttribute('height', '1000');
        document.body.appendChild(svgElement);

        // Create VisualDesigner
        const container = document.createElement('div');
        container.id = 'test-canvas';
        document.body.appendChild(container);
        designer = new VisualDesigner('test-canvas');

        // Get model manager from designer
        const pluginManager = {
            findPlugin: () => null,
            getPlugins: () => [],
            getSchemaVersion: () => null
        };
        modelManager = new VisualSemanticModelManager(designer, pluginManager as any);

        // The renderer operates on the actual inner canvas created by VisualDesigner.
        renderer = new ConnectionPointsRenderer(
            svgElement,
            modelManager,
            (designer as any).canvasElement
        );
    });

    afterEach(() => {
        // Cleanup
        if (svgElement.parentElement) {
            svgElement.parentElement.removeChild(svgElement);
        }
        const container = document.getElementById('test-canvas');
        if (container && container.parentElement) {
            container.parentElement.removeChild(container);
        }
    });

    describe('showForNode', () => {
        it('should show connection points for a node', () => {
            const nodeId = 'test-node-1';
            
            // Add a node to the designer
            const node: VisualNode = {
                id: nodeId,
                type: 'http-request',
                position: { x: 100, y: 100 }
            };
            designer.addNode(node);
            
            // Build model
            modelManager.notifyUpdate();
            
            // Show connection points
            renderer.showForNode(nodeId);
            
            // Check if points are rendered
            const circles = (designer as any).canvasElement.querySelectorAll('.connection-point[data-node-id="' + nodeId + '"]');
            expect(circles.length).toBeGreaterThan(0);
        });

        it('should render 3 points per edge (12 total for 4 edges)', () => {
            const nodeId = 'test-node-2';
            
            // Add a node to the designer
            const node: VisualNode = {
                id: nodeId,
                type: 'http-request',
                position: { x: 100, y: 100 }
            };
            designer.addNode(node);
            
            // Build model
            modelManager.notifyUpdate();
            
            // Show connection points
            renderer.showForNode(nodeId);
            
            // Check if points are rendered
            const circles = svgElement.querySelectorAll('circle.connection-point');
            // Should have 12 points: 3 per edge × 4 edges (but may have more if plugin has custom ports)
            expect(circles.length).toBeGreaterThanOrEqual(12);
        });
    });

    describe('hideForNode', () => {
        it('should hide connection points for a node', () => {
            const nodeId = 'test-node-3';
            
            // Add a node to the designer
            const node: VisualNode = {
                id: nodeId,
                type: 'http-request',
                position: { x: 100, y: 100 }
            };
            designer.addNode(node);
            
            // Build model
            modelManager.notifyUpdate();
            
            // Show then hide connection points
            renderer.showForNode(nodeId);
            const circlesBefore = (designer as any).canvasElement.querySelectorAll('.connection-point[data-node-id="' + nodeId + '"]');
            expect(circlesBefore.length).toBeGreaterThan(0);
            
            renderer.hideForNode(nodeId);
            const circlesAfter = (designer as any).canvasElement.querySelectorAll('.connection-point[data-node-id="' + nodeId + '"]');
            expect(circlesAfter.length).toBe(0);
        });
    });

    describe('hideAll', () => {
        it('should hide all connection points', () => {
            const nodeId1 = 'test-node-4';
            const nodeId2 = 'test-node-5';
            
            // Add nodes to the designer
            const node1: VisualNode = {
                id: nodeId1,
                type: 'http-request',
                position: { x: 100, y: 100 }
            };
            const node2: VisualNode = {
                id: nodeId2,
                type: 'http-request',
                position: { x: 300, y: 300 }
            };
            designer.addNode(node1);
            designer.addNode(node2);
            
            // Build model
            modelManager.notifyUpdate();
            
            // Show connection points for both nodes
            renderer.showForNode(nodeId1);
            renderer.showForNode(nodeId2);
            const circlesBefore = svgElement.querySelectorAll('circle.connection-point');
            expect(circlesBefore.length).toBeGreaterThan(0);
            
            // Hide all
            renderer.hideAll();
            const circlesAfter = svgElement.querySelectorAll('circle.connection-point');
            expect(circlesAfter.length).toBe(0);
        });
    });

    describe('highlightPoint', () => {
        it('should highlight a specific point', () => {
            const nodeId = 'test-node-6';
            
            // Add a node to the designer
            const node: VisualNode = {
                id: nodeId,
                type: 'http-request',
                position: { x: 100, y: 100 }
            };
            designer.addNode(node);
            
            // Build model
            modelManager.notifyUpdate();
            
            // Show connection points
            renderer.showForNode(nodeId);
            
            // Get first point
            const model = modelManager.getModel();
            const nodeState = model.workflow.allNodes.get(nodeId);
            if (!nodeState) {
                throw new Error('Node state not found');
            }
            const firstPoint = nodeState.connectionPoints.inputs[0];
            if (!firstPoint) {
                throw new Error('No input points found');
            }
            
            // Highlight point
            renderer.highlightPoint(nodeId, firstPoint.id);
            
            // Check if point is highlighted (should have different fill color)
            const circle = svgElement.querySelector(`circle[data-point-id="${firstPoint.id}"]`) as SVGCircleElement;
            expect(circle).toBeDefined();
            const fillColor = circle.getAttribute('fill');
            expect(fillColor).toBe('var(--magenta, #e91e63)'); // Highlighted color
        });
    });

    describe('updatePositions', () => {
        it('should update positions when node moves', () => {
            const nodeId = 'test-node-7';
            
            // Add a node to the designer
            const node: VisualNode = {
                id: nodeId,
                type: 'http-request',
                position: { x: 100, y: 100 }
            };
            designer.addNode(node);
            
            // Build model
            modelManager.notifyUpdate();
            
            // Show connection points
            renderer.showForNode(nodeId);
            
            // Get first point position
            const model = modelManager.getModel();
            const nodeState = model.workflow.allNodes.get(nodeId);
            if (!nodeState) {
                throw new Error('Node state not found');
            }
            const firstPoint = nodeState.connectionPoints.inputs[0];
            if (!firstPoint) {
                throw new Error('No input points found');
            }
            
            const circle = svgElement.querySelector(`circle[data-point-id="${firstPoint.id}"]`) as SVGCircleElement;
            const initialCx = parseFloat(circle.getAttribute('cx') || '0');
            const initialCy = parseFloat(circle.getAttribute('cy') || '0');
            
            // Move node
            node.position = { x: 200, y: 200 };
            designer.updateNode(nodeId, node);
            modelManager.notifyUpdate();
            
            // Update positions
            renderer.updatePositions();
            
            // Check if position changed
            const newCx = parseFloat(circle.getAttribute('cx') || '0');
            const newCy = parseFloat(circle.getAttribute('cy') || '0');
            expect(newCx).not.toBe(initialCx);
            expect(newCy).not.toBe(initialCy);
        });
    });
});





