/**
 * ZoomController - TDD Tests
 * Tests for enhanced zoom controls with presets and auto-fit
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ZoomController } from '../ZoomController';
import type { VisualNode } from '../../types';

describe('ZoomController', () => {
    let controller: ZoomController;
    let mockCanvas: HTMLElement;
    let mockNodes: VisualNode[];

    beforeEach(() => {
        controller = new ZoomController();

        mockCanvas = document.createElement('div');
        mockCanvas.style.width = '1000px';
        mockCanvas.style.height = '800px';
        document.body.appendChild(mockCanvas);

        mockNodes = [
            {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            },
            {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 300 }
            }
        ];
    });

    afterEach(() => {
        document.body.removeChild(mockCanvas);
    });

    describe('setZoom', () => {
        it('should set zoom level', () => {
            controller.setZoom(1.5);
            expect(controller.getZoom()).toBe(1.5);
        });

        it('should clamp zoom to min limit', () => {
            controller.setZoom(0.05);
            expect(controller.getZoom()).toBe(0.1); // Min is 0.1
        });

        it('should clamp zoom to max limit', () => {
            controller.setZoom(5.0);
            expect(controller.getZoom()).toBe(3.0); // Max is 3.0
        });

        it('should emit zoom change event', (done) => {
            controller.onZoomChange((zoom) => {
                expect(zoom).toBe(1.5);
                done();
            });

            controller.setZoom(1.5);
        });
    });

    describe('zoomToFit', () => {
        it('should calculate zoom to fit all nodes', () => {
            const zoom = controller.zoomToFit(mockNodes, 1000, 800);

            expect(zoom).toBeGreaterThan(0);
            expect(zoom).toBeLessThanOrEqual(3.0);
        });

        it('should handle empty workflow', () => {
            const zoom = controller.zoomToFit([], 1000, 800);

            expect(zoom).toBe(1.0);
        });

        it('should add padding around content', () => {
            const zoom = controller.zoomToFit(mockNodes, 1000, 800, 50);

            // With padding, zoom should be slightly smaller
            const zoomNoPadding = controller.zoomToFit(mockNodes, 1000, 800, 0);
            expect(zoom).toBeLessThan(zoomNoPadding);
        });
    });

    describe('zoomToSelection', () => {
        it('should zoom to fit selected nodes', () => {
            const selectedNodes = [mockNodes[0]];

            const zoom = controller.zoomToSelection(selectedNodes, 1000, 800);

            expect(zoom).toBeGreaterThan(0);
        });

        it('should center on selection', () => {
            const center = controller.calculateSelectionCenter([mockNodes[0]]);

            expect(center.x).toBe(100 + 250 / 2); // node.x + NODE_WIDTH/2
            expect(center.y).toBe(100 + 100 / 2); // node.y + NODE_HEIGHT/2
        });
    });

    describe('getZoomLevel', () => {
        it('should return current zoom level', () => {
            controller.setZoom(1.5);
            expect(controller.getZoom()).toBe(1.5);
        });
    });

    describe('getPresets', () => {
        it('should return zoom presets', () => {
            const presets = controller.getPresets();

            expect(presets).toContain(0.5);
            expect(presets).toContain(1.0);
            expect(presets).toContain(2.0);
        });

        it('should have at least 5 presets', () => {
            const presets = controller.getPresets();

            expect(presets.length).toBeGreaterThanOrEqual(5);
        });
    });

    describe('setPreset', () => {
        it('should set zoom to preset value', () => {
            controller.setPreset(0.5);
            expect(controller.getZoom()).toBe(0.5);
        });

        it('should emit zoom change', (done) => {
            controller.onZoomChange((zoom) => {
                expect(zoom).toBe(2.0);
                done();
            });

            controller.setPreset(2.0);
        });
    });

    describe('reset', () => {
        it('should reset zoom to 100%', () => {
            controller.setZoom(1.5);
            controller.reset();

            expect(controller.getZoom()).toBe(1.0);
        });
    });

    describe('zoomIn', () => {
        it('should increase zoom by factor', () => {
            controller.setZoom(1.0);
            controller.zoomIn();

            expect(controller.getZoom()).toBeGreaterThan(1.0);
        });

        it('should respect max zoom', () => {
            controller.setZoom(2.9);
            controller.zoomIn();
            controller.zoomIn();

            expect(controller.getZoom()).toBeLessThanOrEqual(3.0);
        });
    });

    describe('zoomOut', () => {
        it('should decrease zoom by factor', () => {
            controller.setZoom(1.0);
            controller.zoomOut();

            expect(controller.getZoom()).toBeLessThan(1.0);
        });

        it('should respect min zoom', () => {
            controller.setZoom(0.15);
            controller.zoomOut();
            controller.zoomOut();

            expect(controller.getZoom()).toBeGreaterThanOrEqual(0.1);
        });
    });

    describe('calculateBoundingBox', () => {
        it('should calculate bounding box for nodes', () => {
            const bbox = controller.calculateBoundingBox(mockNodes);

            expect(bbox.minX).toBe(100);
            expect(bbox.minY).toBe(100);
            expect(bbox.maxX).toBeGreaterThan(100);
            expect(bbox.maxY).toBeGreaterThan(100);
        });

        it('should include node dimensions', () => {
            const bbox = controller.calculateBoundingBox(mockNodes);

            expect(bbox.maxX).toBe(100 + 250); // x + NODE_WIDTH
            expect(bbox.maxY).toBe(300 + 100); // y + NODE_HEIGHT
        });
    });
});


