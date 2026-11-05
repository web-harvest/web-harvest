/**
 * Testy dla LayoutDebugger
 */

import { LayoutDebugger } from '../LayoutDebugger';
import type { VisualNode } from '../../types';
import type { Connection } from '../VisualModelHelpers';

describe('LayoutDebugger', () => {
    let layoutDebugger: LayoutDebugger;
    let canvas: HTMLElement;

    beforeEach(() => {
        canvas = document.createElement('div');
        canvas.id = 'test-canvas';
        document.body.appendChild(canvas);
        layoutDebugger = new LayoutDebugger(canvas);
    });

    afterEach(() => {
        if (canvas.parentNode) {
            canvas.parentNode.removeChild(canvas);
        }
    });

    describe('analyzeLayout', () => {
        it('powinien analizować węzły', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: { name: 'test' },
                    body: 'value',
                    children: [],
                    position: { x: 100, y: 100 }
                }
            ];

            const connections: Connection[] = [];
            const getNodeDimensions = () => ({ width: 200, height: 100 });
            const getNodeDepth = () => 0;
            const getLabelMetrics = () => null;

            const metrics = layoutDebugger.analyzeLayout(
                nodes,
                connections,
                getNodeDimensions,
                getNodeDepth,
                getLabelMetrics
            );

            expect(metrics.nodes).toHaveLength(1);
            expect(metrics.nodes[0].id).toBe('1');
            expect(metrics.nodes[0].position).toEqual({ x: 100, y: 100 });
            expect(metrics.nodes[0].dimensions).toEqual({ width: 200, height: 100 });
        });

        it('powinien wykrywać kolizje między węzłami', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 100, y: 100 }
                },
                {
                    id: '2',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 150, y: 120 } // Nakładają się
                }
            ];

            const connections: Connection[] = [];
            const getNodeDimensions = () => ({ width: 200, height: 100 });
            const getNodeDepth = () => 0;
            const getLabelMetrics = () => null;

            const metrics = layoutDebugger.analyzeLayout(
                nodes,
                connections,
                getNodeDimensions,
                getNodeDepth,
                getLabelMetrics
            );

            expect(metrics.collisions.length).toBeGreaterThan(0);
            expect(metrics.collisions[0].type).toBe('node-node');
        });

        it('powinien analizować połączenia', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 100, y: 100 }
                },
                {
                    id: '2',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 400, y: 200 }
                }
            ];

            const connections: Connection[] = [
                { from: '1', to: '2', type: 'sequential' }
            ];

            const getNodeDimensions = () => ({ width: 200, height: 100 });
            const getNodeDepth = () => 0;
            const getLabelMetrics = () => null;

            const metrics = layoutDebugger.analyzeLayout(
                nodes,
                connections,
                getNodeDimensions,
                getNodeDepth,
                getLabelMetrics
            );

            expect(metrics.connections).toHaveLength(1);
            expect(metrics.connections[0].from).toBe('1');
            expect(metrics.connections[0].to).toBe('2');
            expect(metrics.connections[0].length).toBeGreaterThan(0);
        });

        it('powinien analizować zagnieżdżone węzły', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'loop',
                    attributes: {},
                    body: null,
                    children: [
                        {
                            id: '2',
                            type: 'var',
                            attributes: {},
                            body: null,
                            children: [],
                            position: { x: 150, y: 250 }
                        }
                    ],
                    position: { x: 100, y: 100 }
                }
            ];

            const connections: Connection[] = [];
            const getNodeDimensions = () => ({ width: 200, height: 100 });
            const getNodeDepth = (node: VisualNode) => {
                if (node.id === '1') return 0;
                return 1;
            };
            const getLabelMetrics = () => null;

            const metrics = layoutDebugger.analyzeLayout(
                nodes,
                connections,
                getNodeDimensions,
                getNodeDepth,
                getLabelMetrics
            );

            expect(metrics.nodes).toHaveLength(2);
            expect(metrics.nodes.find(n => n.id === '1')?.depth).toBe(0);
            expect(metrics.nodes.find(n => n.id === '2')?.depth).toBe(1);
            expect(metrics.nodes.find(n => n.id === '2')?.parentId).toBe('1');
        });

        it('powinien obliczać całkowite bounds', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 100, y: 100 }
                },
                {
                    id: '2',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 400, y: 300 }
                }
            ];

            const connections: Connection[] = [];
            const getNodeDimensions = () => ({ width: 200, height: 100 });
            const getNodeDepth = () => 0;
            const getLabelMetrics = () => null;

            const metrics = layoutDebugger.analyzeLayout(
                nodes,
                connections,
                getNodeDimensions,
                getNodeDepth,
                getLabelMetrics
            );

            expect(metrics.bounds.x).toBe(100);
            expect(metrics.bounds.y).toBe(100);
            expect(metrics.bounds.width).toBe(500); // 400 + 200 - 100
            expect(metrics.bounds.height).toBe(300); // 300 + 100 - 100
        });
    });

    describe('setEnabled', () => {
        it('powinien tworzyć debug overlay gdy włączony', () => {
            layoutDebugger.setEnabled(true);
            expect(canvas.querySelector('#layout-debug-overlay')).toBeTruthy();
        });

        it('powinien usuwać debug overlay gdy wyłączony', () => {
            layoutDebugger.setEnabled(true);
            layoutDebugger.setEnabled(false);
            expect(canvas.querySelector('#layout-debug-overlay')).toBeFalsy();
        });
    });

    describe('generateReport', () => {
        it('powinien generować raport', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 100, y: 100 }
                }
            ];

            const connections: Connection[] = [];
            const getNodeDimensions = () => ({ width: 200, height: 100 });
            const getNodeDepth = () => 0;
            const getLabelMetrics = () => null;

            layoutDebugger.analyzeLayout(
                nodes,
                connections,
                getNodeDimensions,
                getNodeDepth,
                getLabelMetrics
            );

            const report = layoutDebugger.generateReport();
            expect(report).toContain('Layout Debug Report');
            expect(report).toContain('Nodes: 1');
        });
    });
});

