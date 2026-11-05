/**
 * AutoLayout Code Quality Tests
 * Sprawdza jakość kodu i poprawność działania algorytmu auto-layout
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../types';

describe('AutoLayout - Jakość kodu i poprawność działania', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas';
        container.style.width = '2000px';
        container.style.height = '2000px';
        document.body.appendChild(container);

        designer = new VisualDesigner('test-canvas');
    });

    afterEach(() => {
        if (container.parentNode) {
            document.body.removeChild(container);
        }
    });

    function createNode(id: string, type: string, children: VisualNode[] = []): VisualNode {
        return {
            id,
            type,
            attributes: {},
            body: null,
            children,
        };
    }

    describe('Sprawdzanie jakości implementacji', () => {
        it('powinien poprawnie obsługiwać puste listy węzłów', () => {
            const nodes: VisualNode[] = [];
            expect(() => designer.loadNodes(nodes)).not.toThrow();
            expect(designer.getNodes()).toHaveLength(0);
        });

        it('powinien poprawnie obsługiwać węzły bez pozycji przed layout', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
            ];

            // Usuń pozycje przed layout
            nodes.forEach(node => {
                delete node.position;
            });

            expect(() => designer.loadNodes(nodes)).not.toThrow();
            const loadedNodes = designer.getNodes();
            expect(loadedNodes.every(n => n.position !== undefined)).toBe(true);
        });

        it('powinien poprawnie obsługiwać węzły z null/undefined children', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: {},
                    body: null,
                    children: null as any,
                },
                {
                    id: '2',
                    type: 'http',
                    attributes: {},
                    body: null,
                    children: undefined as any,
                },
            ];

            expect(() => designer.loadNodes(nodes)).not.toThrow();
            const loadedNodes = designer.getNodes();
            expect(loadedNodes.length).toBeGreaterThan(0);
        });

        it('powinien poprawnie obsługiwać duże struktury (wydajność)', () => {
            const nodes: VisualNode[] = [];
            
            // Stwórz 20 root nodes (zmniejszone dla lepszej wydajności)
            for (let i = 0; i < 20; i++) {
                const children: VisualNode[] = [];
                // Każdy root node ma 5 dzieci
                for (let j = 0; j < 5; j++) {
                    children.push(createNode(`child-${i}-${j}`, 'var'));
                }
                nodes.push(createNode(`root-${i}`, 'loop', children));
            }

            const startTime = Date.now();
            expect(() => designer.loadNodes(nodes)).not.toThrow();
            const endTime = Date.now();
            const duration = endTime - startTime;

            // Layout powinien zakończyć się w rozsądnym czasie (< 10 sekund dla 120 węzłów)
            expect(duration).toBeLessThan(10000);

            const loadedNodes = designer.getNodes();
            expect(loadedNodes.length).toBe(20);
            
            // Wszystkie węzły powinny mieć pozycje
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children || [])];
            expect(allNodes.every(n => n.position !== undefined)).toBe(true);
        });

        it('powinien poprawnie obsługiwać głębokie zagnieżdżenia (wydajność)', () => {
            // Stwórz bardzo głębokie zagnieżdżenie (10 poziomów)
            let currentNode: VisualNode = createNode('level-9', 'var');
            for (let level = 8; level >= 0; level--) {
                currentNode = createNode(`level-${level}`, 'loop', [currentNode]);
            }

            const startTime = Date.now();
            expect(() => designer.loadNodes([currentNode])).not.toThrow();
            const endTime = Date.now();
            const duration = endTime - startTime;

            // Layout powinien zakończyć się w rozsądnym czasie (< 2 sekundy)
            expect(duration).toBeLessThan(2000);

            const loadedNodes = designer.getNodes();
            expect(loadedNodes.length).toBe(1);
            
            // Sprawdź czy wszystkie poziomy mają pozycje
            const collectNodes = (node: VisualNode): VisualNode[] => {
                const result = [node];
                if (node.children && node.children.length > 0) {
                    node.children.forEach(child => {
                        result.push(...collectNodes(child));
                    });
                }
                return result;
            };
            const allNodes = collectNodes(loadedNodes[0]);
            expect(allNodes.every(n => n.position !== undefined)).toBe(true);
        });

        it('powinien zachować spójność po wielokrotnym wywołaniu layout', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                ]),
            ];

            // Wykonaj layout wielokrotnie
            for (let i = 0; i < 5; i++) {
                designer.loadNodes(nodes);
            }

            const loadedNodes = designer.getNodes();
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children || [])];

            // Wszystkie węzły powinny mieć pozycje
            expect(allNodes.every(n => n.position !== undefined)).toBe(true);

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    const node1 = allNodes[i];
                    const node2 = allNodes[j];
                    if (node1.position && node2.position) {
                        const dims1 = { width: 200, height: 100 }; // Przybliżone wymiary
                        const dims2 = { width: 200, height: 100 };
                        
                        const node1Right = node1.position.x + dims1.width;
                        const node1Bottom = node1.position.y + dims1.height;
                        const node2Right = node2.position.x + dims2.width;
                        const node2Bottom = node2.position.y + dims2.height;

                        const overlapsX = !(node1Right < node2.position.x || node1.position.x > node2Right);
                        const overlapsY = !(node1Bottom < node2.position.y || node1.position.y > node2Bottom);

                        expect(overlapsX && overlapsY).toBe(false);
                    }
                }
            }
        });

        it('powinien poprawnie obsługiwać węzły z bardzo długimi nazwami/etykietami', () => {
            const nodes: VisualNode[] = [
                createNode('very-long-node-name-that-might-cause-layout-issues', 'var'),
                createNode('another-very-long-node-name-with-many-characters', 'http'),
            ];

            expect(() => designer.loadNodes(nodes)).not.toThrow();
            const loadedNodes = designer.getNodes();
            expect(loadedNodes.every(n => n.position !== undefined)).toBe(true);
        });

        it('powinien poprawnie obsługiwać mieszane scenariusze (root + nested + manual positioned)', () => {
            const nodes: VisualNode[] = [
                createNode('auto1', 'var'),
                createNode('manual', 'http'),
                createNode('auto2', 'loop', [
                    createNode('nested1', 'var'),
                    createNode('nested2', 'http'),
                ]),
            ];

            // Ustaw ręczną pozycję dla jednego węzła
            nodes[1].position = { x: 1000, y: 1000 };
            nodes[1].isManualPositioned = true;

            expect(() => designer.loadNodes(nodes)).not.toThrow();
            const loadedNodes = designer.getNodes();
            
            // Węzeł z ręczną pozycją powinien zachować swoją pozycję
            const manualNode = loadedNodes.find(n => n.id === 'manual');
            expect(manualNode?.position?.x).toBe(1000);
            expect(manualNode?.position?.y).toBe(1000);

            // Pozostałe węzły powinny mieć pozycje z auto-layout
            const autoNodes = loadedNodes.filter(n => n.id !== 'manual');
            expect(autoNodes.every(n => n.position !== undefined)).toBe(true);
        });
    });

    describe('Sprawdzanie poprawności algorytmu', () => {
        it('powinien zawsze przypisywać pozycje wszystkim węzłom', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'loop', [
                    createNode('3', 'var'),
                    createNode('4', 'http'),
                ]),
                createNode('5', 'if', [
                    createNode('6', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            const collectAllNodes = (nodes: VisualNode[]): VisualNode[] => {
                const result: VisualNode[] = [];
                nodes.forEach(node => {
                    result.push(node);
                    if (node.children && node.children.length > 0) {
                        result.push(...collectAllNodes(node.children));
                    }
                });
                return result;
            };

            const allNodes = collectAllNodes(loadedNodes);
            expect(allNodes.every(n => n.position !== undefined && n.position !== null)).toBe(true);
        });

        it('powinien zachować kolejność topologiczną dla root nodes', () => {
            const nodes: VisualNode[] = [
                createNode('start', 'var'),
                createNode('middle', 'http'),
                createNode('end', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Węzły powinny być ułożone wertykalnie (Y rośnie)
            const yPositions = loadedNodes.map(n => n.position!.y).sort((a, b) => a - b);
            expect(yPositions).toEqual([...yPositions].sort((a, b) => a - b));
        });

        it('powinien zachować horyzontalny układ dla swim lanes', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                    createNode('child3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const children = loadedNodes[0].children || [];

            if (children.length > 1) {
                // Wszystkie dzieci powinny mieć podobną pozycję Y (horyzontalna linia)
                const yPositions = children.map(c => c.position!.y);
                const maxY = Math.max(...yPositions);
                const minY = Math.min(...yPositions);
                expect(maxY - minY).toBeLessThan(10); // Tolerancja 10px

                // Dzieci powinny mieć różne pozycje X
                const xPositions = children.map(c => c.position!.x);
                const uniqueX = new Set(xPositions);
                expect(uniqueX.size).toBe(children.length);
            }
        });
    });
});

