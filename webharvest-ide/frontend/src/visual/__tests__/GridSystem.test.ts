/**
 * GridSystem - TDD Tests
 * Tests for grid overlay and snap-to-grid functionality
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { GridSystem } from '../GridSystem';

describe('GridSystem', () => {
    let grid: GridSystem;
    let mockCanvas: HTMLElement;

    beforeEach(() => {
        grid = new GridSystem(20); // 20px grid

        mockCanvas = document.createElement('div');
        mockCanvas.id = 'test-canvas';
        mockCanvas.style.width = '1000px';
        mockCanvas.style.height = '800px';
        document.body.appendChild(mockCanvas);
    });

    afterEach(() => {
        document.body.removeChild(mockCanvas);
    });

    describe('render', () => {
        it('should render grid overlay', () => {
            grid.render(mockCanvas);

            const overlay = mockCanvas.querySelector('.grid-overlay');
            expect(overlay).not.toBeNull();
        });

        it('should create grid pattern with correct spacing', () => {
            grid.render(mockCanvas);

            const svg = mockCanvas.querySelector('svg.grid-overlay') as SVGSVGElement;
            expect(svg).not.toBeNull();
        });

        it('should expand overlay to cover scrollable content', () => {
            const scrollContainer = document.createElement('div');
            scrollContainer.style.width = '400px';
            scrollContainer.style.height = '300px';
            scrollContainer.style.overflow = 'auto';

            const canvas = document.createElement('div');
            canvas.style.position = 'relative';
            canvas.style.width = '1200px';
            canvas.style.height = '900px';

            scrollContainer.appendChild(canvas);
            document.body.appendChild(scrollContainer);

            grid.render(canvas, scrollContainer);

            const svg = canvas.querySelector('svg.grid-overlay') as SVGSVGElement;
            expect(svg).not.toBeNull();
            const svgWidth = Number(svg.getAttribute('width'));
            const svgHeight = Number(svg.getAttribute('height'));
            expect(svgWidth).toBeGreaterThanOrEqual(1200);
            expect(svgHeight).toBeGreaterThanOrEqual(900);

            document.body.removeChild(scrollContainer);
        });

        it('should be toggleable', () => {
            grid.render(mockCanvas);
            grid.hide();

            const overlay = mockCanvas.querySelector('.grid-overlay') as HTMLElement;
            expect(overlay.style.display).toBe('none');
        });
    });

    describe('snapToGrid', () => {
        it('should snap coordinates to nearest grid point', () => {
            const snapped = grid.snapToGrid({ x: 17, y: 23 });

            expect(snapped.x).toBe(20);
            expect(snapped.y).toBe(20);
        });

        it('should snap to exact grid points', () => {
            const snapped = grid.snapToGrid({ x: 40, y: 60 });

            expect(snapped.x).toBe(40);
            expect(snapped.y).toBe(60);
        });

        it('should round to nearest grid point', () => {
            const snapped = grid.snapToGrid({ x: 35, y: 45 });

            expect(snapped.x).toBe(40);
            expect(snapped.y).toBe(40);
        });

        it('should handle negative coordinates', () => {
            const snapped = grid.snapToGrid({ x: -17, y: -23 });

            expect(snapped.x).toBe(-20);
            expect(snapped.y).toBe(-20);
        });
    });

    describe('isEnabled', () => {
        it('should return enabled state', () => {
            expect(grid.isEnabled()).toBe(true);
        });

        it('should update when toggled', () => {
            grid.disable();
            expect(grid.isEnabled()).toBe(false);

            grid.enable();
            expect(grid.isEnabled()).toBe(true);
        });
    });

    describe('getGridSize', () => {
        it('should return current grid size', () => {
            expect(grid.getGridSize()).toBe(20);
        });
    });

    describe('setGridSize', () => {
        it('should update grid size', () => {
            grid.setGridSize(30);
            expect(grid.getGridSize()).toBe(30);
        });

        it('should re-render grid with new size', () => {
            grid.render(mockCanvas);
            grid.setGridSize(30);

            const snapped = grid.snapToGrid({ x: 35, y: 35 });
            expect(snapped.x).toBe(30);
        });
    });

    describe('showAlignmentGuides', () => {
        it('should show guide for aligned nodes', () => {
            const guide = grid.showAlignmentGuide(mockCanvas, { x: 100, y: 200 });

            expect(guide).not.toBeNull();
            expect(guide?.classList.contains('alignment-guide')).toBe(true);
        });

        it('should position guide correctly', () => {
            const guide = grid.showAlignmentGuide(mockCanvas, { x: 100, y: 200 });

            expect(guide?.style.left).toBe('100px');
        });
    });

    describe('clearAlignmentGuides', () => {
        it('should remove all alignment guides', () => {
            grid.showAlignmentGuide(mockCanvas, { x: 100, y: 200 });
            grid.showAlignmentGuide(mockCanvas, { x: 200, y: 200 });

            grid.clearAlignmentGuides();

            const guides = mockCanvas.querySelectorAll('.alignment-guide');
            expect(guides.length).toBe(0);
        });
    });

    describe('findNearbyAlignments', () => {
        it('should find nodes aligned horizontally', () => {
            const nodes = [
                { id: 'node-1', position: { x: 100, y: 200 } },
                { id: 'node-2', position: { x: 300, y: 200 } },
                { id: 'node-3', position: { x: 500, y: 400 } }
            ];

            const alignments = grid.findNearbyAlignments({ x: 700, y: 205 }, nodes, 10);

            expect(alignments.length).toBeGreaterThan(0);
            expect(alignments.some(a => a.axis === 'horizontal')).toBe(true);
        });

        it('should find nodes aligned vertically', () => {
            const nodes = [
                { id: 'node-1', position: { x: 100, y: 200 } },
                { id: 'node-2', position: { x: 100, y: 400 } }
            ];

            const alignments = grid.findNearbyAlignments({ x: 105, y: 600 }, nodes, 10);

            expect(alignments.some(a => a.axis === 'vertical')).toBe(true);
        });
    });

    describe('distributeSpacing', () => {
        it('should distribute nodes evenly', () => {
            const nodes = [
                { id: 'node-1', position: { x: 100, y: 100 } },
                { id: 'node-2', position: { x: 100, y: 250 } },
                { id: 'node-3', position: { x: 100, y: 500 } }
            ];

            const distributed = grid.distributeSpacing(nodes, 'vertical');

            const spacing1 = distributed[1].position!.y - distributed[0].position!.y;
            const spacing2 = distributed[2].position!.y - distributed[1].position!.y;

            expect(spacing1).toBe(spacing2);
        });

        it('should distribute horizontally', () => {
            const nodes = [
                { id: 'node-1', position: { x: 100, y: 100 } },
                { id: 'node-2', position: { x: 300, y: 100 } },
                { id: 'node-3', position: { x: 700, y: 100 } }
            ];

            const distributed = grid.distributeSpacing(nodes, 'horizontal');

            const spacing1 = distributed[1].position!.x - distributed[0].position!.x;
            const spacing2 = distributed[2].position!.x - distributed[1].position!.x;

            expect(spacing1).toBe(spacing2);
        });
    });

    describe('getSnapThreshold', () => {
        it('should return snap threshold', () => {
            const threshold = grid.getSnapThreshold();
            expect(threshold).toBeGreaterThan(0);
        });
    });
});


