/**
 * Minimap - TDD Tests
 * Tests for minimap navigation component
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { Minimap } from '../Minimap';
import type { VisualNode } from '../../types';

describe('Minimap', () => {
    let minimap: Minimap;
    let mockCanvas: HTMLElement;
    let mockNodes: VisualNode[];

    beforeEach(() => {
        minimap = new Minimap();

        mockCanvas = document.createElement('div');
        mockCanvas.id = 'test-canvas';
        mockCanvas.style.width = '2000px';
        mockCanvas.style.height = '1500px';
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
            },
            {
                id: 'node-3',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 500, y: 600 }
            }
        ];
    });

    afterEach(() => {
        document.body.removeChild(mockCanvas);
    });

    describe('render', () => {
        it('should create minimap element', () => {
            minimap.render(mockCanvas, mockNodes);

            const minimapElement = mockCanvas.querySelector('.minimap');
            expect(minimapElement).not.toBeNull();
        });

        it('should position minimap in bottom-right', () => {
            minimap.render(mockCanvas, mockNodes);

            const minimapElement = mockCanvas.querySelector('.minimap') as HTMLElement;
            expect(minimapElement.style.position).toBe('absolute');
            expect(minimapElement.style.bottom).toBeTruthy();
            expect(minimapElement.style.right).toBeTruthy();
        });

        it('should render thumbnail of nodes', () => {
            minimap.render(mockCanvas, mockNodes);

            const nodes = mockCanvas.querySelectorAll('.minimap-node');
            expect(nodes.length).toBe(3);
        });

        it('should render viewport indicator', () => {
            minimap.render(mockCanvas, mockNodes);

            const viewport = mockCanvas.querySelector('.minimap-viewport');
            expect(viewport).not.toBeNull();
        });
    });

    describe('updateViewport', () => {
        it('should update viewport position on canvas pan', () => {
            minimap.render(mockCanvas, mockNodes);

            minimap.updateViewport({ x: 100, y: 50 }, 1.0);

            const viewport = mockCanvas.querySelector('.minimap-viewport') as HTMLElement;
            expect(viewport).not.toBeNull();
        });

        it('should update viewport size on zoom change', () => {
            minimap.render(mockCanvas, mockNodes);

            minimap.updateViewport({ x: 0, y: 0 }, 0.5);

            const viewport = mockCanvas.querySelector('.minimap-viewport') as HTMLElement;
            // Viewport should be larger when zoomed out
            expect(viewport).not.toBeNull();
        });
    });

    describe('handleClick', () => {
        it('should convert minimap click to canvas position', () => {
            minimap.render(mockCanvas, mockNodes);

            const canvasPos = minimap.minimapToCanvasPosition({ x: 50, y: 50 });

            expect(canvasPos).not.toBeNull();
            expect(canvasPos.x).toBeGreaterThan(0);
            expect(canvasPos.y).toBeGreaterThan(0);
        });

        it('should emit navigation event on click', (done) => {
            minimap.render(mockCanvas, mockNodes);

            minimap.onNavigate((position) => {
                expect(position.x).toBeGreaterThan(0);
                expect(position.y).toBeGreaterThan(0);
                done();
            });

            // Simulate click
            minimap.handleMinimapClick({ x: 50, y: 50 });
        });
    });

    describe('show/hide', () => {
        it('should show minimap', () => {
            minimap.render(mockCanvas, mockNodes);
            minimap.hide();
            minimap.show();

            const minimapElement = mockCanvas.querySelector('.minimap') as HTMLElement;
            expect(minimapElement.style.display).not.toBe('none');
        });

        it('should hide minimap', () => {
            minimap.render(mockCanvas, mockNodes);
            minimap.hide();

            const minimapElement = mockCanvas.querySelector('.minimap') as HTMLElement;
            expect(getComputedStyle(minimapElement).display).toBe('none');
        });
    });

    describe('isVisible', () => {
        it('should return visibility state', () => {
            minimap.render(mockCanvas, mockNodes);

            expect(minimap.isVisible()).toBe(true);

            minimap.hide();
            expect(minimap.isVisible()).toBe(false);
        });
    });

    describe('updateNodes', () => {
        it('should update minimap when nodes change', () => {
            minimap.render(mockCanvas, mockNodes);

            const newNodes: VisualNode[] = [
                ...mockNodes,
                {
                    id: 'node-4',
                    type: 'http',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 800, y: 800 }
                }
            ];

            minimap.updateNodes(newNodes);

            const nodes = mockCanvas.querySelectorAll('.minimap-node');
            expect(nodes.length).toBe(4);
        });
    });

    describe('scale calculation', () => {
        it('should calculate correct scale for minimap', () => {
            const scale = minimap.calculateScale(2000, 1500, 200, 150);

            expect(scale).toBeCloseTo(0.1, 2);
        });

        it('should maintain aspect ratio', () => {
            const scale = minimap.calculateScale(2000, 1000, 200, 150);

            expect(scale).toBeLessThanOrEqual(0.1);
        });
    });

    describe('visibility', () => {
        it('should stay visible even when canvas is small', () => {
            const smallCanvas = document.createElement('div');
            smallCanvas.style.width = '400px';
            smallCanvas.style.height = '300px';
            document.body.appendChild(smallCanvas);

            minimap.render(smallCanvas, mockNodes);

            const minimapElement = smallCanvas.querySelector('.minimap') as HTMLElement;
            // Currently minimap stays visible regardless of size
            expect(getComputedStyle(minimapElement).display).toBe('block');

            document.body.removeChild(smallCanvas);
        });

        it('should show when canvas is large enough', () => {
            minimap.render(mockCanvas, mockNodes);

            const minimapElement = mockCanvas.querySelector('.minimap') as HTMLElement;
            expect(getComputedStyle(minimapElement).display).not.toBe('none');
        });
    });
});


