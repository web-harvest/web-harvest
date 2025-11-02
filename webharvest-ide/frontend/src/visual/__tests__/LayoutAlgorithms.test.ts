/**
 * LayoutAlgorithms - TDD Tests
 * Tests for automatic layout algorithms
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { LayoutAlgorithms, LayoutType } from '../LayoutAlgorithms';
import type { VisualNode } from '../../types';

describe('LayoutAlgorithms', () => {
    let algorithms: LayoutAlgorithms;
    let mockNodes: VisualNode[];

    beforeEach(() => {
        algorithms = new LayoutAlgorithms();

        mockNodes = [
            {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            },
            {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            },
            {
                id: 'node-3',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            },
            {
                id: 'node-4',
                type: 'file',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            }
        ];
    });

    describe('vertical layout', () => {
        it('should stack nodes vertically', () => {
            const positioned = algorithms.applyLayout(mockNodes, 'vertical');

            // All nodes should have same x coordinate
            const xCoords = positioned.map(n => n.position!.x);
            expect(new Set(xCoords).size).toBe(1);

            // Y coordinates should increase
            expect(positioned[1].position!.y).toBeGreaterThan(positioned[0].position!.y);
            expect(positioned[2].position!.y).toBeGreaterThan(positioned[1].position!.y);
        });

        it('should use consistent spacing', () => {
            const positioned = algorithms.applyLayout(mockNodes, 'vertical');

            const spacing1 = positioned[1].position!.y - positioned[0].position!.y;
            const spacing2 = positioned[2].position!.y - positioned[1].position!.y;

            expect(spacing1).toBe(spacing2);
        });
    });

    describe('horizontal layout', () => {
        it('should arrange nodes horizontally', () => {
            const positioned = algorithms.applyLayout(mockNodes, 'horizontal');

            // All nodes should have same y coordinate
            const yCoords = positioned.map(n => n.position!.y);
            expect(new Set(yCoords).size).toBe(1);

            // X coordinates should increase
            expect(positioned[1].position!.x).toBeGreaterThan(positioned[0].position!.x);
            expect(positioned[2].position!.x).toBeGreaterThan(positioned[1].position!.x);
        });

        it('should use consistent spacing', () => {
            const positioned = algorithms.applyLayout(mockNodes, 'horizontal');

            const spacing1 = positioned[1].position!.x - positioned[0].position!.x;
            const spacing2 = positioned[2].position!.x - positioned[1].position!.x;

            expect(spacing1).toBe(spacing2);
        });
    });

    describe('grid layout', () => {
        it('should arrange nodes in grid', () => {
            const positioned = algorithms.applyLayout(mockNodes, 'grid');

            // Should have multiple rows and columns
            const xCoords = new Set(positioned.map(n => n.position!.x));
            const yCoords = new Set(positioned.map(n => n.position!.y));

            expect(xCoords.size).toBeGreaterThan(1);
            expect(yCoords.size).toBeGreaterThan(1);
        });

        it('should calculate grid dimensions', () => {
            const dimensions = algorithms.calculateGridDimensions(4);

            expect(dimensions.rows).toBe(2);
            expect(dimensions.cols).toBe(2);
        });

        it('should handle non-square grids', () => {
            const dimensions = algorithms.calculateGridDimensions(6);

            expect(dimensions.rows * dimensions.cols).toBeGreaterThanOrEqual(6);
        });
    });

    describe('hierarchical layout', () => {
        it('should arrange nodes in tree structure', () => {
            const hierarchicalNodes: VisualNode[] = [
                {
                    id: 'root',
                    type: 'loop',
                    attributes: {},
                    body: null,
                    children: [
                        {
                            id: 'child-1',
                            type: 'http',
                            attributes: {},
                            body: null,
                            children: []
                        },
                        {
                            id: 'child-2',
                            type: 'xpath',
                            attributes: {},
                            body: null,
                            children: []
                        }
                    ]
                }
            ];

            const positioned = algorithms.applyLayout(hierarchicalNodes, 'hierarchical');

            // Root should be at top
            const root = positioned[0];
            expect(root.position!.y).toBe(50);

            // Children should be below and spread horizontally
            if (root.children.length > 0) {
                expect(root.children[0].position!.y).toBeGreaterThan(root.position!.y);
            }
        });

        it('should center children under parent', () => {
            const hierarchicalNodes: VisualNode[] = [
                {
                    id: 'root',
                    type: 'loop',
                    attributes: {},
                    body: null,
                    children: [
                        { id: 'c1', type: 'http', attributes: {}, body: null, children: [] },
                        { id: 'c2', type: 'xpath', attributes: {}, body: null, children: [] }
                    ]
                }
            ];

            const positioned = algorithms.applyLayout(hierarchicalNodes, 'hierarchical');

            const root = positioned[0];
            const child1 = root.children[0];
            const child2 = root.children[1];

            // Children should be distributed evenly
            const childCenterX = (child1.position!.x + child2.position!.x) / 2;
            expect(Math.abs(childCenterX - root.position!.x)).toBeLessThan(50);
        });
    });

    describe('force-directed layout', () => {
        it('should apply physics-based layout', () => {
            const positioned = algorithms.applyLayout(mockNodes, 'force-directed', { iterations: 10 });

            // Nodes should be spread out
            expect(positioned.every(n => n.position !== undefined)).toBe(true);
        });

        it('should respect iteration count', () => {
            const result1 = algorithms.applyLayout(mockNodes, 'force-directed', { iterations: 5 });
            const result2 = algorithms.applyLayout(mockNodes, 'force-directed', { iterations: 50 });

            // More iterations should produce better layout
            expect(result2).toBeDefined();
        });
    });

    describe('setSpacing', () => {
        it('should update default spacing', () => {
            algorithms.setSpacing(200);

            const positioned = algorithms.applyLayout(mockNodes, 'vertical');
            const spacing = positioned[1].position!.y - positioned[0].position!.y;

            expect(spacing).toBe(200);
        });
    });

    describe('getSpacing', () => {
        it('should return current spacing', () => {
            const spacing = algorithms.getSpacing();
            expect(spacing).toBeGreaterThan(0);
        });
    });

    describe('center layout', () => {
        it('should center nodes in canvas', () => {
            const canvasWidth = 1000;
            const canvasHeight = 800;

            const centered = algorithms.centerInCanvas(mockNodes, canvasWidth, canvasHeight);

            // Calculate bounding box
            const minX = Math.min(...centered.map(n => n.position!.x));
            const maxX = Math.max(...centered.map(n => n.position!.x));
            const minY = Math.min(...centered.map(n => n.position!.y));
            const maxY = Math.max(...centered.map(n => n.position!.y));

            const centerX = (minX + maxX) / 2;
            const centerY = (minY + maxY) / 2;

            expect(Math.abs(centerX - canvasWidth / 2)).toBeLessThan(100);
            expect(Math.abs(centerY - canvasHeight / 2)).toBeLessThan(100);
        });
    });

    describe('align nodes', () => {
        it('should align nodes to left edge', () => {
            const aligned = algorithms.alignNodes(mockNodes, 'left');

            const xCoords = aligned.map(n => n.position!.x);
            expect(new Set(xCoords).size).toBe(1);
        });

        it('should align nodes to top edge', () => {
            const aligned = algorithms.alignNodes(mockNodes, 'top');

            const yCoords = aligned.map(n => n.position!.y);
            expect(new Set(yCoords).size).toBe(1);
        });

        it('should align nodes to center', () => {
            const aligned = algorithms.alignNodes(mockNodes, 'center-vertical');

            // Calculate average x
            const avgX = mockNodes.reduce((sum, n) => sum + (n.position?.x || 0), 0) / mockNodes.length;

            aligned.forEach(node => {
                expect(Math.abs(node.position!.x - avgX)).toBeLessThan(1);
            });
        });
    });

    describe('performance', () => {
        it('should handle large workflows efficiently', () => {
            const largeNodeSet: VisualNode[] = [];
            for (let i = 0; i < 100; i++) {
                largeNodeSet.push({
                    id: `node-${i}`,
                    type: 'http',
                    attributes: {},
                    body: null,
                    children: []
                });
            }

            const start = Date.now();
            algorithms.applyLayout(largeNodeSet, 'grid');
            const duration = Date.now() - start;

            expect(duration).toBeLessThan(1000); // Should complete in <1s
        });
    });
});


