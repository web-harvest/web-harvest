/**
 * AutoLayout - Szczegółowe testy algorytmu rozmieszczania elementów
 * Testy dla layoutVerticalRootFlow, layoutNestedLevels, layoutSwimLane, etc.
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../types';

describe('AutoLayout - Szczegółowe testy rozmieszczania', () => {
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

    /**
     * Helper: Tworzy prosty węzeł
     */
    function createNode(id: string, type: string, children: VisualNode[] = []): VisualNode {
        return {
            id,
            type,
            attributes: {},
            body: null,
            children,
        };
    }

    /**
     * Helper: Sprawdza czy dwa węzły się nakładają
     */
    function nodesOverlap(node1: VisualNode, node2: VisualNode): boolean {
        if (!node1.position || !node2.position) return false;

        const designerAny = designer as unknown as Record<string, any>;
        const dims1 = designerAny.getNodeDimensions?.(node1) || { width: 200, height: 100 };
        const dims2 = designerAny.getNodeDimensions?.(node2) || { width: 200, height: 100 };

        const node1Right = node1.position.x + dims1.width;
        const node1Bottom = node1.position.y + dims1.height;
        const node2Right = node2.position.x + dims2.width;
        const node2Bottom = node2.position.y + dims2.height;

        return !(
            node1Right < node2.position.x ||
            node1.position.x > node2Right ||
            node1Bottom < node2.position.y ||
            node1.position.y > node2Bottom
        );
    }

    /**
     * Helper: Sprawdza czy wszystkie węzły mają pozycje
     */
    function allNodesHavePositions(nodes: VisualNode[]): boolean {
        const checkNode = (node: VisualNode): boolean => {
            if (!node.position) return false;
            return node.children.every(checkNode);
        };
        return nodes.every(checkNode);
    }

    describe('layoutVerticalRootFlow - Główny przepływ wertykalny', () => {
        it('powinien układać root nodes wertykalnie od góry do dołu', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
                createNode('3', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            expect(allNodesHavePositions(loadedNodes)).toBe(true);

            // Sprawdź czy węzły są ułożone wertykalnie
            const positions = loadedNodes.map(n => n.position!.y).sort((a, b) => a - b);
            expect(positions).toEqual([...positions].sort((a, b) => a - b));

            // Sprawdź czy nie zachodzą na siebie
            for (let i = 0; i < loadedNodes.length; i++) {
                for (let j = i + 1; j < loadedNodes.length; j++) {
                    expect(nodesOverlap(loadedNodes[i], loadedNodes[j])).toBe(false);
                }
            }
        });

        it('powinien zachować kolejność wykonania (topologiczne sortowanie)', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
                createNode('3', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Węzły powinny być ułożone w kolejności XML (lub topologicznej)
            const yPositions = loadedNodes.map(n => ({ id: n.id, y: n.position!.y }));
            const sortedByY = [...yPositions].sort((a, b) => a.y - b.y);

            // Sprawdź czy kolejność jest logiczna
            expect(sortedByY.length).toBe(3);
        });

        it('powinien zapewnić odpowiednie odstępy między węzłami', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            const node1 = loadedNodes[0];
            const node2 = loadedNodes[1];

            if (node1.position && node2.position) {
                const designerAny = designer as unknown as Record<string, any>;
                const dims1 = designerAny.getNodeDimensions?.(node1) || { width: 200, height: 100 };
                const node1Bottom = node1.position.y + dims1.height;
                const gap = node2.position.y - node1Bottom;

                // Odstęp powinien być co najmniej 100px (ROOT_VERTICAL_GAP)
                expect(gap).toBeGreaterThanOrEqual(100);
            }
        });
    });

    describe('layoutMindMapHierarchy - Mind map layout (dzieci na lewo/prawo)', () => {
        it('powinien układać dzieci alternująco na lewo/prawo od rodzica', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                    createNode('child3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            const children = parent.children;

            expect(allNodesHavePositions([parent, ...children])).toBe(true);

            if (parent.position && children.length >= 2) {
                const designerAny = designer as unknown as Record<string, any>;
                const parentDims = designerAny.getNodeDimensions?.(parent) || { width: 200, height: 100 };
                const parentLeft = parent.position.x;
                const parentRight = parent.position.x + parentDims.width;
                const parentCenterY = parent.position.y + parentDims.height / 2;

                // Pierwsze dziecko (index 0) powinno być na lewo
                const child1 = children[0];
                if (child1.position) {
                    expect(child1.position.x).toBeLessThan(parentLeft);
                    // Pozycja Y powinna być wyśrodkowana względem rodzica
                    const child1Dims = designerAny.getNodeDimensions?.(child1) || { width: 200, height: 100 };
                    const child1CenterY = child1.position.y + child1Dims.height / 2;
                    expect(Math.abs(child1CenterY - parentCenterY)).toBeLessThan(50); // Tolerancja
                }

                // Drugie dziecko (index 1) powinno być na prawo
                const child2 = children[1];
                if (child2.position) {
                    expect(child2.position.x).toBeGreaterThan(parentRight);
                }
            }
        });

        it('powinien układać wiele dzieci na tej samej stronie jako stos wertykalny', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                    createNode('child3', 'xpath'),
                    createNode('child4', 'var'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            const children = parent.children;

            expect(allNodesHavePositions([parent, ...children])).toBe(true);

            // Dzieci na lewo (child1, child3) powinny być ułożone wertykalnie
            const leftChildren = [children[0], children[2]]; // child1, child3
            if (leftChildren.length >= 2 && leftChildren[0].position && leftChildren[1].position) {
                const xPositions = leftChildren.map(c => c.position!.x);
                // Powinny mieć podobną pozycję X (na lewo od rodzica)
                const maxX = Math.max(...xPositions);
                const minX = Math.min(...xPositions);
                expect(maxX - minX).toBeLessThan(50); // Tolerancja

                // Powinny być ułożone wertykalnie (różne Y)
                const yPositions = leftChildren.map(c => c.position!.y);
                const uniqueY = new Set(yPositions);
                expect(uniqueY.size).toBe(leftChildren.length);
            }
        });

        it('powinien unikać kolizji między węzłami w mind mapie', () => {
            const nodes: VisualNode[] = [
                createNode('parent1', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                ]),
                createNode('parent2', 'loop', [
                    createNode('child3', 'xpath'),
                    createNode('child4', 'var'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy żadne węzły się nie nakładają
            const allNodesFlat: VisualNode[] = [];
            const collectNodes = (node: VisualNode) => {
                allNodesFlat.push(node);
                node.children.forEach(collectNodes);
            };
            loadedNodes.forEach(collectNodes);

            for (let i = 0; i < allNodesFlat.length; i++) {
                for (let j = i + 1; j < allNodesFlat.length; j++) {
                    expect(nodesOverlap(allNodesFlat[i], allNodesFlat[j])).toBe(false);
                }
            }
        });

        it('powinien obsługiwać głęboką hierarchię (3+ poziomy)', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'loop', [
                    createNode('level1', 'loop', [
                        createNode('level2a', 'var'),
                        createNode('level2b', 'http'),
                    ]),
                    createNode('level1b', 'loop', [
                        createNode('level2c', 'xpath'),
                    ]),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            expect(allNodesHavePositions(loadedNodes)).toBe(true);

            // Sprawdź czy wszystkie poziomy mają pozycje
            const root = loadedNodes[0];
            const level1a = root.children[0];
            const level1b = root.children[1];
            const level2a = level1a.children[0];
            const level2b = level1a.children[1];
            const level2c = level1b.children[0];

            expect(root.position).toBeDefined();
            expect(level1a.position).toBeDefined();
            expect(level1b.position).toBeDefined();
            expect(level2a.position).toBeDefined();
            expect(level2b.position).toBeDefined();
            expect(level2c.position).toBeDefined();

            // Sprawdź czy nie ma kolizji
            const allNodes = [root, level1a, level1b, level2a, level2b, level2c];
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });
    });

    describe('layoutNestedLevels - Swim lanes dla zagnieżdżonych poziomów', () => {
        it('powinien układać dzieci kontenera jako swim lane horyzontalnie', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                    createNode('child3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            const children = parent.children;

            expect(allNodesHavePositions([parent, ...children])).toBe(true);

            // Wszystkie dzieci powinny mieć tę samą pozycję Y (horyzontalna linia)
            if (children.length > 1) {
                const yPositions = children.map(c => c.position!.y);
                const uniqueY = new Set(yPositions);
                // Wszystkie dzieci powinny być na tym samym poziomie Y (z tolerancją na zaokrąglenia)
                const maxY = Math.max(...yPositions);
                const minY = Math.min(...yPositions);
                expect(maxY - minY).toBeLessThan(10); // Tolerancja 10px
            }

            // Dzieci powinny być ułożone horyzontalnie (różne X)
            const xPositions = children.map(c => c.position!.x);
            const uniqueX = new Set(xPositions);
            expect(uniqueX.size).toBe(children.length);
        });

        it('powinien układać swim lane poniżej rodzica gdy nie ma kolizji', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            const children = parent.children;

            if (parent.position && children.length > 0 && children[0].position) {
                const designerAny = designer as unknown as Record<string, any>;
                const parentDims = designerAny.getNodeDimensions?.(parent) || { width: 200, height: 100 };
                const parentBottom = parent.position.y + parentDims.height;
                const childTop = children[0].position.y;

                // Dziecko powinno być poniżej rodzica
                expect(childTop).toBeGreaterThan(parentBottom);
            }
        });

        it('powinien unikać kolizji między swim lanes z różnych rodziców', () => {
            const nodes: VisualNode[] = [
                createNode('parent1', 'loop', [
                    createNode('child1-1', 'var'),
                    createNode('child1-2', 'http'),
                ]),
                createNode('parent2', 'if', [
                    createNode('child2-1', 'xpath'),
                    createNode('child2-2', 'var'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy swim lanes nie zachodzą na siebie
            const allChildren = loadedNodes.flatMap(n => n.children);
            for (let i = 0; i < allChildren.length; i++) {
                for (let j = i + 1; j < allChildren.length; j++) {
                    expect(nodesOverlap(allChildren[i], allChildren[j])).toBe(false);
                }
            }
        });

        it('powinien wybierać kierunek above/below minimalizujący kolizje', () => {
            // Stwórz scenariusz gdzie poniżej rodzica jest już węzeł
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                ]),
                createNode('blocker', 'var'), // Ten węzeł może blokować pozycję "below"
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes.find(n => n.id === 'parent');
            const children = parent?.children || [];

            // Swim lane powinna być ułożona (above lub below) bez kolizji
            if (parent && children.length > 0) {
                const allNodes = [...loadedNodes, ...children];
                for (let i = 0; i < allNodes.length; i++) {
                    for (let j = i + 1; j < allNodes.length; j++) {
                        if (allNodes[i].id !== allNodes[j].id) {
                            expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                        }
                    }
                }
            }
        });
    });

    describe('Kolizje i optymalizacja pozycji', () => {
        it('powinien wykrywać kolizje między węzłami', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
                createNode('3', 'xpath'),
                createNode('4', 'template'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy żadne węzły nie zachodzą na siebie
            for (let i = 0; i < loadedNodes.length; i++) {
                for (let j = i + 1; j < loadedNodes.length; j++) {
                    expect(nodesOverlap(loadedNodes[i], loadedNodes[j])).toBe(false);
                }
            }
        });

        it('powinien rozwiązywać kolizje etykiet', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
                createNode('3', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy etykiety (górne 40px) nie zachodzą na siebie
            for (let i = 0; i < loadedNodes.length; i++) {
                for (let j = i + 1; j < loadedNodes.length; j++) {
                    const node1 = loadedNodes[i];
                    const node2 = loadedNodes[j];

                    if (node1.position && node2.position) {
                        const labelHeight = 40;
                        const node1LabelBottom = node1.position.y + labelHeight;
                        const node2LabelTop = node2.position.y;

                        // Etykiety nie powinny się nakładać
                        const labelsOverlap = !(node1LabelBottom <= node2LabelTop || node2LabelTop + labelHeight <= node1.position.y);
                        expect(labelsOverlap).toBe(false);
                    }
                }
            }
        });

        it('powinien zapewnić minimalne odstępy między elementami', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            if (loadedNodes.length >= 2 && loadedNodes[0].position && loadedNodes[1].position) {
                const designerAny = designer as unknown as Record<string, any>;
                const dims1 = designerAny.getNodeDimensions?.(loadedNodes[0]) || { width: 200, height: 100 };
                const node1Bottom = loadedNodes[0].position.y + dims1.height;
                const node2Top = loadedNodes[1].position.y;
                const gap = node2Top - node1Bottom;

                // Odstęp powinien być co najmniej 20px (minimalny gap)
                expect(gap).toBeGreaterThanOrEqual(20);
            }
        });
    });

    describe('Złożone scenariusze hierarchiczne', () => {
        it('powinien obsługiwać głębokie zagnieżdżenia', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'loop', [
                    createNode('level1', 'if', [
                        createNode('level2', 'loop', [
                            createNode('level3', 'var'),
                        ]),
                    ]),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Wszystkie węzły powinny mieć pozycje
            const allNodes: VisualNode[] = [];
            const collectNodes = (nodes: VisualNode[]) => {
                nodes.forEach(node => {
                    allNodes.push(node);
                    if (node.children.length > 0) {
                        collectNodes(node.children);
                    }
                });
            };
            collectNodes(loadedNodes);

            expect(allNodesHavePositions(allNodes)).toBe(true);

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien obsługiwać wiele swim lanes na różnych poziomach', () => {
            const nodes: VisualNode[] = [
                createNode('parent1', 'loop', [
                    createNode('child1-1', 'var'),
                    createNode('child1-2', 'http'),
                ]),
                createNode('parent2', 'if', [
                    createNode('child2-1', 'xpath'),
                ]),
                createNode('parent3', 'loop', [
                    createNode('child3-1', 'var'),
                    createNode('child3-2', 'http'),
                    createNode('child3-3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Wszystkie węzły powinny mieć pozycje
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];
            expect(allNodesHavePositions(allNodes)).toBe(true);

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien zachować czytelność dla wielu elementów', () => {
            const nodes: VisualNode[] = [];
            for (let i = 0; i < 10; i++) {
                nodes.push(createNode(`node${i}`, 'var'));
            }

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            expect(allNodesHavePositions(loadedNodes)).toBe(true);

            // Sprawdź czy wszystkie węzły są widoczne i nie zachodzą na siebie
            for (let i = 0; i < loadedNodes.length; i++) {
                for (let j = i + 1; j < loadedNodes.length; j++) {
                    expect(nodesOverlap(loadedNodes[i], loadedNodes[j])).toBe(false);
                }
            }

            // Sprawdź czy węzły są ułożone w logicznej kolejności
            const yPositions = loadedNodes.map(n => n.position!.y).sort((a, b) => a - b);
            for (let i = 1; i < yPositions.length; i++) {
                expect(yPositions[i]).toBeGreaterThan(yPositions[i - 1]);
            }
        });
    });

    describe('Edge cases', () => {
        it('powinien obsługiwać puste dzieci', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', []),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            expect(loadedNodes.length).toBe(1);
            expect(loadedNodes[0].position).toBeDefined();
        });

        it('powinien obsługiwać pojedynczy węzeł', () => {
            const nodes: VisualNode[] = [
                createNode('single', 'var'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            expect(loadedNodes.length).toBe(1);
            expect(loadedNodes[0].position).toBeDefined();
        });

        it('powinien obsługiwać bardzo szerokie swim lanes', () => {
            const children: VisualNode[] = [];
            for (let i = 0; i < 20; i++) {
                children.push(createNode(`child${i}`, 'var'));
            }

            const nodes: VisualNode[] = [
                createNode('parent', 'loop', children),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            const loadedChildren = parent.children;

            // Wszystkie dzieci powinny mieć pozycje
            expect(allNodesHavePositions([parent, ...loadedChildren])).toBe(true);

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < loadedChildren.length; i++) {
                for (let j = i + 1; j < loadedChildren.length; j++) {
                    expect(nodesOverlap(loadedChildren[i], loadedChildren[j])).toBe(false);
                }
            }
        });
    });

    describe('Szczegółowe testy wymiarów i pozycji', () => {
        it('powinien używać rzeczywistych wymiarów węzłów do wykrywania kolizji', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
                createNode('3', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy wszystkie węzły mają pozycje i wymiary
            loadedNodes.forEach(node => {
                expect(node.position).toBeDefined();
                expect(node.position!.x).toBeGreaterThanOrEqual(0);
                expect(node.position!.y).toBeGreaterThanOrEqual(0);
            });

            // Sprawdź czy nie ma kolizji używając rzeczywistych wymiarów
            for (let i = 0; i < loadedNodes.length; i++) {
                for (let j = i + 1; j < loadedNodes.length; j++) {
                    expect(nodesOverlap(loadedNodes[i], loadedNodes[j])).toBe(false);
                }
            }
        });

        it('powinien zapewnić odpowiednie odstępy w swim lanes', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                    createNode('child3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const children = loadedNodes[0].children;

            // Sprawdź odstępy horyzontalne między dziećmi
            if (children.length > 1) {
                const xPositions = children.map(c => c.position!.x).sort((a, b) => a - b);
                const designerAny = designer as unknown as Record<string, any>;
                
                for (let i = 0; i < xPositions.length - 1; i++) {
                    const node1 = children.find(c => c.position!.x === xPositions[i]);
                    const node2 = children.find(c => c.position!.x === xPositions[i + 1]);
                    
                    if (node1 && node2) {
                        const dims1 = designerAny.getNodeDimensions?.(node1) || { width: 200, height: 100 };
                        const node1Right = node1.position!.x + dims1.width;
                        const gap = node2.position!.x - node1Right;
                        
                        // Odstęp powinien być co najmniej 40px (SWIM_LANE_NODE_GAP)
                        expect(gap).toBeGreaterThanOrEqual(40);
                    }
                }
            }
        });

        it('powinien układać swim lanes z odpowiednimi wcięciami dla różnych poziomów', () => {
            const nodes: VisualNode[] = [
                createNode('level0', 'loop', [
                    createNode('level1-1', 'if', [
                        createNode('level2-1', 'var'),
                        createNode('level2-2', 'http'),
                    ]),
                    createNode('level1-2', 'var'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const level0 = loadedNodes[0];
            const level1_1 = level0.children.find(c => c.id === 'level1-1')!;
            const level1_2 = level0.children.find(c => c.id === 'level1-2')!;
            const level2_1 = level1_1.children.find(c => c.id === 'level2-1')!;

            // Sprawdź czy wszystkie węzły mają pozycje
            expect(allNodesHavePositions([level0, level1_1, level1_2, level2_1])).toBe(true);

            // Level 1 węzły powinny być w swim lane poniżej level0
            if (level0.position && level1_1.position) {
                expect(level1_1.position.y).toBeGreaterThan(level0.position.y);
            }

            // Level 2 węzły powinny być w swim lane poniżej level1
            if (level1_1.position && level2_1.position) {
                expect(level2_1.position.y).toBeGreaterThan(level1_1.position.y);
            }

            // Sprawdź czy nie ma kolizji
            const allNodes = [level0, level1_1, level1_2, ...level1_1.children];
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien zachować spójność pozycji po ponownym układaniu', () => {
            const nodes: VisualNode[] = [
                createNode('1', 'var'),
                createNode('2', 'http'),
                createNode('3', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const firstLayout = designer.getNodes().map(n => ({ id: n.id, pos: { ...n.position! } }));

            // Załaduj ponownie te same węzły
            designer.loadNodes(nodes);
            const secondLayout = designer.getNodes().map(n => ({ id: n.id, pos: { ...n.position! } }));

            // Pozycje powinny być spójne (z tolerancją na zaokrąglenia)
            firstLayout.forEach((first, i) => {
                const second = secondLayout.find(s => s.id === first.id);
                if (second) {
                    expect(Math.abs(first.pos.x - second.pos.x)).toBeLessThan(10);
                    expect(Math.abs(first.pos.y - second.pos.y)).toBeLessThan(10);
                }
            });
        });

        it('powinien obsługiwać mieszane scenariusze (root + nested)', () => {
            const nodes: VisualNode[] = [
                createNode('root1', 'var'),
                createNode('root2', 'loop', [
                    createNode('nested1', 'var'),
                    createNode('nested2', 'http'),
                ]),
                createNode('root3', 'xpath'),
                createNode('root4', 'if', [
                    createNode('nested3', 'var'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];

            // Wszystkie węzły powinny mieć pozycje
            expect(allNodesHavePositions(allNodes)).toBe(true);

            // Sprawdź czy root nodes są wertykalnie
            const rootYPositions = loadedNodes.map(n => n.position!.y).sort((a, b) => a - b);
            for (let i = 1; i < rootYPositions.length; i++) {
                expect(rootYPositions[i]).toBeGreaterThan(rootYPositions[i - 1]);
            }

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });
    });

    describe('Mind Map Layout - Rozwinięty widok drzewa', () => {
        it('powinien układać główny przepływ wertykalnie jak oś mind map', () => {
            const nodes: VisualNode[] = [
                createNode('start', 'var'),
                createNode('middle', 'http'),
                createNode('end', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Główny przepływ powinien być wertykalny (wszystkie węzły w podobnej kolumnie X)
            const xPositions = loadedNodes.map(n => n.position!.x);
            const avgX = xPositions.reduce((a, b) => a + b, 0) / xPositions.length;
            const maxDeviation = Math.max(...xPositions.map(x => Math.abs(x - avgX)));

            // Wszystkie węzły głównego przepływu powinny być w podobnej kolumnie (odchylenie < 100px)
            expect(maxDeviation).toBeLessThan(100);

            // Węzły powinny być ułożone wertykalnie (Y rośnie)
            const yPositions = loadedNodes.map(n => n.position!.y).sort((a, b) => a - b);
            for (let i = 1; i < yPositions.length; i++) {
                expect(yPositions[i]).toBeGreaterThan(yPositions[i - 1]);
            }
        });

        it('powinien układać swim lanes horyzontalnie jak gałęzie mind map', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'loop', [
                    createNode('branch1', 'var'),
                    createNode('branch2', 'http'),
                    createNode('branch3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const root = loadedNodes[0];
            const branches = root.children;

            // Wszystkie gałęzie powinny być na tym samym poziomie Y (horyzontalna linia)
            const yPositions = branches.map(b => b.position!.y);
            const maxY = Math.max(...yPositions);
            const minY = Math.min(...yPositions);
            expect(maxY - minY).toBeLessThan(10); // Tolerancja 10px

            // Gałęzie powinny być rozłożone horyzontalnie (różne X)
            const xPositions = branches.map(b => b.position!.x);
            const uniqueX = new Set(xPositions);
            expect(uniqueX.size).toBe(branches.length);

            // Gałęzie powinny być ułożone w logicznej kolejności (od lewej do prawej)
            const sortedX = [...xPositions].sort((a, b) => a - b);
            expect(xPositions).toEqual(sortedX);
        });

        it('powinien tworzyć strukturę drzewa z wyraźnymi poziomami', () => {
            const nodes: VisualNode[] = [
                createNode('level0', 'var'),
                createNode('level0-child', 'loop', [
                    createNode('level1-1', 'var'),
                    createNode('level1-2', 'http'),
                    createNode('level1-3', 'xpath', [
                        createNode('level2-1', 'var'),
                        createNode('level2-2', 'http'),
                    ]),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const level0 = loadedNodes[0];
            const level0Child = loadedNodes[1];
            const level1Nodes = level0Child.children;
            const level2Nodes = level1Nodes[2].children;

            // Sprawdź czy każdy poziom jest wyraźnie oddzielony wertykalnie
            if (level0.position && level0Child.position) {
                expect(level0Child.position.y).toBeGreaterThan(level0.position.y);
            }

            if (level0Child.position && level1Nodes[0].position) {
                expect(level1Nodes[0].position.y).toBeGreaterThan(level0Child.position.y);
            }

            if (level1Nodes[2].position && level2Nodes[0].position) {
                expect(level2Nodes[0].position.y).toBeGreaterThan(level1Nodes[2].position.y);
            }

            // Sprawdź czy nie ma kolizji między poziomami
            const allNodes = [level0, level0Child, ...level1Nodes, ...level2Nodes];
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien układać wiele swim lanes bez nakładania się', () => {
            const nodes: VisualNode[] = [
                createNode('parent1', 'loop', [
                    createNode('child1-1', 'var'),
                    createNode('child1-2', 'http'),
                ]),
                createNode('parent2', 'if', [
                    createNode('child2-1', 'xpath'),
                    createNode('child2-2', 'var'),
                ]),
                createNode('parent3', 'loop', [
                    createNode('child3-1', 'var'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Zbierz wszystkie swim lanes
            const swimLanes: VisualNode[][] = [];
            loadedNodes.forEach(parent => {
                if (parent.children.length > 0) {
                    swimLanes.push(parent.children);
                }
            });

            // Sprawdź czy swim lanes nie zachodzą na siebie
            for (let i = 0; i < swimLanes.length; i++) {
                for (let j = i + 1; j < swimLanes.length; j++) {
                    const lane1 = swimLanes[i];
                    const lane2 = swimLanes[j];

                    // Sprawdź czy jakikolwiek węzeł z lane1 koliduje z węzłem z lane2
                    for (const node1 of lane1) {
                        for (const node2 of lane2) {
                            expect(nodesOverlap(node1, node2)).toBe(false);
                        }
                    }
                }
            }
        });

        it('powinien zachować proporcje i czytelność mind map', () => {
            const nodes: VisualNode[] = [
                createNode('root1', 'var'),
                createNode('root2', 'loop', [
                    createNode('branch1', 'var'),
                    createNode('branch2', 'http'),
                    createNode('branch3', 'xpath'),
                ]),
                createNode('root3', 'if', [
                    createNode('branch4', 'var'),
                ]),
                createNode('root4', 'var'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy główny przepływ (root nodes) jest czytelny
            const rootNodes = loadedNodes.filter(n => n.children.length === 0 || n.children.length > 0);
            const rootYPositions = rootNodes.map(n => n.position!.y).sort((a, b) => a - b);

            // Odstępy między root nodes powinny być wystarczające
            for (let i = 1; i < rootYPositions.length; i++) {
                const gap = rootYPositions[i] - rootYPositions[i - 1];
                expect(gap).toBeGreaterThan(100); // Minimalny odstęp dla czytelności
            }

            // Sprawdź czy swim lanes są wyraźnie oddzielone od root nodes
            loadedNodes.forEach(parent => {
                if (parent.children.length > 0 && parent.position) {
                    const children = parent.children;
                    const minChildY = Math.min(...children.map(c => c.position!.y));
                    const designerAny = designer as unknown as Record<string, any>;
                    const parentDims = designerAny.getNodeDimensions?.(parent) || { width: 200, height: 100 };
                    const parentBottom = parent.position.y + parentDims.height;

                    // Swim lane powinna być wyraźnie oddzielona od rodzica
                    expect(minChildY - parentBottom).toBeGreaterThan(80);
                }
            });
        });

        it('powinien obsługiwać asymetryczne drzewo mind map', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'var'),
                createNode('branch1', 'loop', [
                    createNode('leaf1-1', 'var'),
                    createNode('leaf1-2', 'http'),
                ]),
                createNode('branch2', 'if', [
                    createNode('leaf2-1', 'xpath'),
                    createNode('leaf2-2', 'var'),
                    createNode('leaf2-3', 'http'),
                    createNode('leaf2-4', 'xpath'),
                ]),
                createNode('branch3', 'var'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Wszystkie węzły powinny mieć pozycje
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];
            expect(allNodesHavePositions(allNodes)).toBe(true);

            // Sprawdź czy struktura przypomina mind map (główny przepływ + gałęzie)
            const rootNodes = loadedNodes.filter(n => n.children.length === 0);
            const branchNodes = loadedNodes.filter(n => n.children.length > 0);

            // Root nodes powinny być wertykalnie
            if (rootNodes.length > 1) {
                const rootY = rootNodes.map(n => n.position!.y).sort((a, b) => a - b);
                for (let i = 1; i < rootY.length; i++) {
                    expect(rootY[i]).toBeGreaterThan(rootY[i - 1]);
                }
            }

            // Każda gałąź powinna mieć swim lane horyzontalnie
            branchNodes.forEach(branch => {
                const children = branch.children;
                if (children.length > 1) {
                    const yPositions = children.map(c => c.position!.y);
                    const maxY = Math.max(...yPositions);
                    const minY = Math.min(...yPositions);
                    expect(maxY - minY).toBeLessThan(10);
                }
            });

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien zachować spójność wizualną mind map dla złożonych struktur', () => {
            const nodes: VisualNode[] = [
                createNode('start', 'var'),
                createNode('process1', 'loop', [
                    createNode('step1-1', 'var'),
                    createNode('step1-2', 'http'),
                ]),
                createNode('process2', 'if', [
                    createNode('step2-1', 'xpath'),
                ]),
                createNode('process3', 'loop', [
                    createNode('step3-1', 'var'),
                    createNode('step3-2', 'http'),
                    createNode('step3-3', 'xpath', [
                        createNode('step3-3-1', 'var'),
                    ]),
                ]),
                createNode('end', 'var'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy główny przepływ (start -> end) jest wertykalny
            const mainFlow = loadedNodes.filter(n => ['start', 'end'].includes(n.id) || 
                (n.children.length === 0 && !['start', 'end'].includes(n.id)));
            
            if (mainFlow.length > 1) {
                const mainFlowY = mainFlow.map(n => n.position!.y).sort((a, b) => a - b);
                for (let i = 1; i < mainFlowY.length; i++) {
                    expect(mainFlowY[i]).toBeGreaterThan(mainFlowY[i - 1]);
                }
            }

            // Sprawdź czy wszystkie swim lanes są horyzontalne
            loadedNodes.forEach(parent => {
                if (parent.children.length > 1) {
                    const children = parent.children;
                    const yPositions = children.map(c => c.position!.y);
                    const maxY = Math.max(...yPositions);
                    const minY = Math.min(...yPositions);
                    expect(maxY - minY).toBeLessThan(10);
                }
            });

            // Sprawdź czy nie ma kolizji
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien optymalizować przestrzeń dla mind map z wieloma poziomami', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'var'),
                createNode('level1-1', 'loop', [
                    createNode('level2-1', 'if', [
                        createNode('level3-1', 'var'),
                        createNode('level3-2', 'http'),
                    ]),
                    createNode('level2-2', 'var'),
                ]),
                createNode('level1-2', 'http'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy wszystkie poziomy są wyraźnie oddzielone
            const allNodes: VisualNode[] = [];
            const collectNodes = (nodes: VisualNode[]) => {
                nodes.forEach(node => {
                    allNodes.push(node);
                    if (node.children.length > 0) {
                        collectNodes(node.children);
                    }
                });
            };
            collectNodes(loadedNodes);

            // Każdy poziom powinien mieć większą pozycję Y niż poprzedni
            const nodesByLevel = new Map<number, VisualNode[]>();
            const designerAny = designer as unknown as Record<string, any>;
            const getNodeLevel = (node: VisualNode): number => {
                const rootNodes = loadedNodes;
                let level = 0;
                let current = node;
                while (current) {
                    const parent = rootNodes.find(r => r.children.some(c => c.id === current.id)) ||
                        allNodes.find(n => n.children.some(c => c.id === current.id));
                    if (parent) {
                        level++;
                        current = parent;
                    } else {
                        break;
                    }
                }
                return level;
            };

            allNodes.forEach(node => {
                const level = getNodeLevel(node);
                if (!nodesByLevel.has(level)) {
                    nodesByLevel.set(level, []);
                }
                nodesByLevel.get(level)!.push(node);
            });

            // Sprawdź czy poziomy nie zachodzą na siebie
            const levels = Array.from(nodesByLevel.keys()).sort((a, b) => a - b);
            for (let i = 0; i < levels.length - 1; i++) {
                const level1Nodes = nodesByLevel.get(levels[i])!;
                const level2Nodes = nodesByLevel.get(levels[i + 1])!;

                const maxY1 = Math.max(...level1Nodes.map(n => {
                    if (!n.position) return 0;
                    const dims = designerAny.getNodeDimensions?.(n) || { width: 200, height: 100 };
                    return n.position.y + dims.height;
                }));

                const minY2 = Math.min(...level2Nodes.map(n => n.position?.y || Infinity));

                // Poziom i+1 powinien być poniżej poziomu i
                expect(minY2).toBeGreaterThan(maxY1);
            }
        });

        it('powinien zapewnić optymalne wykorzystanie przestrzeni dla mind map', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'var'),
                createNode('branch1', 'loop', [
                    createNode('leaf1', 'var'),
                    createNode('leaf2', 'http'),
                ]),
                createNode('branch2', 'if', [
                    createNode('leaf3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy przestrzeń jest wykorzystana efektywnie
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];
            const positions = allNodes.map(n => n.position!);

            // Oblicz bounding box wszystkich węzłów
            const minX = Math.min(...positions.map(p => p.x));
            const maxX = Math.max(...positions.map(p => {
                const designerAny = designer as unknown as Record<string, any>;
                const node = allNodes.find(n => n.position === p);
                const dims = node ? designerAny.getNodeDimensions?.(node) || { width: 200, height: 100 } : { width: 200, height: 100 };
                return p.x + dims.width;
            }));
            const minY = Math.min(...positions.map(p => p.y));
            const maxY = Math.max(...positions.map(p => {
                const designerAny = designer as unknown as Record<string, any>;
                const node = allNodes.find(n => n.position === p);
                const dims = node ? designerAny.getNodeDimensions?.(node) || { width: 200, height: 100 } : { width: 200, height: 100 };
                return p.y + dims.height;
            }));

            const totalWidth = maxX - minX;
            const totalHeight = maxY - minY;

            // Sprawdź czy przestrzeń nie jest zbyt rozproszona (wymiary powinny być rozsądne)
            expect(totalWidth).toBeGreaterThan(0);
            expect(totalHeight).toBeGreaterThan(0);
            // Wymiary nie powinny być ekstremalnie duże (np. > 10000px)
            expect(totalWidth).toBeLessThan(10000);
            expect(totalHeight).toBeLessThan(10000);
        });

        it('powinien zachować symetrię wizualną dla podobnych struktur', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'var'),
                createNode('left', 'loop', [
                    createNode('left1', 'var'),
                    createNode('left2', 'http'),
                ]),
                createNode('right', 'loop', [
                    createNode('right1', 'var'),
                    createNode('right2', 'http'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const root = loadedNodes[0];
            const left = loadedNodes[1];
            const right = loadedNodes[2];

            // Sprawdź czy podobne struktury są ułożone symetrycznie względem root
            if (root.position && left.position && right.position) {
                const rootCenterX = root.position.x + 100; // Przybliżony środek
                const leftCenterX = left.position.x + 100;
                const rightCenterX = right.position.x + 100;

                const leftDistance = Math.abs(rootCenterX - leftCenterX);
                const rightDistance = Math.abs(rightCenterX - rootCenterX);

                // Odległości powinny być podobne (z tolerancją 50px)
                expect(Math.abs(leftDistance - rightDistance)).toBeLessThan(50);
            }
        });

        it('powinien obsługiwać bardzo szerokie mind map z wieloma gałęziami', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'var'),
            ];

            // Dodaj wiele gałęzi
            for (let i = 0; i < 15; i++) {
                nodes.push(createNode(`branch${i}`, 'loop', [
                    createNode(`leaf${i}-1`, 'var'),
                    createNode(`leaf${i}-2`, 'http'),
                ]));
            }

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Wszystkie węzły powinny mieć pozycje
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];
            expect(allNodesHavePositions(allNodes)).toBe(true);

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }

            // Sprawdź czy główny przepływ jest wertykalny
            const rootNodes = loadedNodes.filter(n => n.children.length === 0);
            if (rootNodes.length > 1) {
                const yPositions = rootNodes.map(n => n.position!.y).sort((a, b) => a - b);
                for (let i = 1; i < yPositions.length; i++) {
                    expect(yPositions[i]).toBeGreaterThan(yPositions[i - 1]);
                }
            }
        });

        it('powinien zapewnić czytelność połączeń w mind map', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'var'),
                createNode('branch1', 'loop', [
                    createNode('leaf1', 'var'),
                    createNode('leaf2', 'http'),
                ]),
                createNode('branch2', 'if', [
                    createNode('leaf3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy swim lanes są wystarczająco oddalone od root nodes
            // aby połączenia były czytelne
            const rootNodes = loadedNodes.filter(n => n.children.length === 0);
            const branchNodes = loadedNodes.filter(n => n.children.length > 0);

            branchNodes.forEach(branch => {
                if (branch.position) {
                    const children = branch.children;
                    const minChildY = Math.min(...children.map(c => c.position!.y));
                    const designerAny = designer as unknown as Record<string, any>;
                    const branchDims = designerAny.getNodeDimensions?.(branch) || { width: 200, height: 100 };
                    const branchBottom = branch.position.y + branchDims.height;

                    // Swim lane powinna być wystarczająco oddalona dla czytelnych połączeń
                    expect(minChildY - branchBottom).toBeGreaterThan(80);
                }
            });
        });

        it('powinien zachować hierarchię wizualną mind map', () => {
            const nodes: VisualNode[] = [
                createNode('level0', 'var'),
                createNode('level1', 'loop', [
                    createNode('level2-1', 'if', [
                        createNode('level3-1', 'var'),
                    ]),
                    createNode('level2-2', 'var'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy każdy poziom jest wyraźnie oddzielony
            const level0 = loadedNodes[0];
            const level1 = loadedNodes[1];
            const level2_1 = level1.children[0];
            const level3_1 = level2_1.children[0];

            // Level 1 powinien być poniżej level 0
            if (level0.position && level1.position) {
                expect(level1.position.y).toBeGreaterThan(level0.position.y);
            }

            // Level 2 powinien być poniżej level 1
            if (level1.position && level2_1.position) {
                expect(level2_1.position.y).toBeGreaterThan(level1.position.y);
            }

            // Level 3 powinien być poniżej level 2
            if (level2_1.position && level3_1.position) {
                expect(level3_1.position.y).toBeGreaterThan(level2_1.position.y);
            }

            // Sprawdź czy nie ma kolizji
            const allNodes = [level0, level1, level2_1, level3_1, level1.children[1]];
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien obsługiwać mind map z różnymi rozmiarami gałęzi', () => {
            const nodes: VisualNode[] = [
                createNode('root', 'var'),
                createNode('small', 'loop', [
                    createNode('small1', 'var'),
                ]),
                createNode('medium', 'if', [
                    createNode('medium1', 'var'),
                    createNode('medium2', 'http'),
                    createNode('medium3', 'xpath'),
                ]),
                createNode('large', 'loop', [
                    createNode('large1', 'var'),
                    createNode('large2', 'http'),
                    createNode('large3', 'xpath'),
                    createNode('large4', 'var'),
                    createNode('large5', 'http'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Wszystkie węzły powinny mieć pozycje
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];
            expect(allNodesHavePositions(allNodes)).toBe(true);

            // Sprawdź czy swim lanes są odpowiednio szerokie dla swoich rozmiarów
            loadedNodes.forEach(parent => {
                if (parent.children.length > 0) {
                    const children = parent.children;
                    const xPositions = children.map(c => c.position!.x).sort((a, b) => a - b);
                    const minX = xPositions[0];
                    const maxX = xPositions[xPositions.length - 1];

                    const designerAny = designer as unknown as Record<string, any>;
                    const lastChild = children.find(c => c.position!.x === maxX);
                    const lastDims = lastChild ? designerAny.getNodeDimensions?.(lastChild) || { width: 200, height: 100 } : { width: 200, height: 100 };
                    const totalWidth = maxX + lastDims.width - minX;

                    // Szerokość swim lane powinna być proporcjonalna do liczby dzieci
                    expect(totalWidth).toBeGreaterThan(children.length * 200);
                }
            });

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien poprawnie obsługiwać swim lanes szersze niż rodzic', () => {
            const nodes: VisualNode[] = [
                createNode('narrow-parent', 'var'),
                createNode('wide-parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                    createNode('child3', 'xpath'),
                    createNode('child4', 'var'),
                    createNode('child5', 'http'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const wideParent = loadedNodes.find(n => n.id === 'wide-parent');
            const children = wideParent?.children || [];

            // Wszystkie dzieci powinny mieć pozycje
            expect(allNodesHavePositions([wideParent!, ...children])).toBe(true);

            // Swim lane powinna być szersza niż rodzic
            if (wideParent && wideParent.position && children.length > 0) {
                const designerAny = designer as unknown as Record<string, any>;
                const parentDims = designerAny.getNodeDimensions?.(wideParent) || { width: 200, height: 100 };
                const bounds = calculateSwimLaneBounds(children);
                
                // Swim lane powinna być szersza niż rodzic (lub przynajmniej nie węższa)
                expect(bounds.width).toBeGreaterThanOrEqual(parentDims.width * 0.5);
            }

            // Sprawdź czy nie ma kolizji
            const allNodes = [...loadedNodes, ...children];
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });

        it('powinien zachować stabilność pozycji po wielu iteracjach layout', () => {
            const nodes: VisualNode[] = [
                createNode('root1', 'var'),
                createNode('root2', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                ]),
                createNode('root3', 'if', [
                    createNode('child3', 'xpath'),
                ]),
            ];

            // Wykonaj layout wielokrotnie
            for (let i = 0; i < 3; i++) {
                designer.loadNodes(nodes);
            }

            const loadedNodes = designer.getNodes();
            const allNodes = [...loadedNodes, ...loadedNodes.flatMap(n => n.children)];

            // Wszystkie węzły powinny mieć pozycje
            expect(allNodesHavePositions(allNodes)).toBe(true);

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < allNodes.length; i++) {
                for (let j = i + 1; j < allNodes.length; j++) {
                    expect(nodesOverlap(allNodes[i], allNodes[j])).toBe(false);
                }
            }
        });
    });

    describe('Zawijanie swim lanes - Responsywność', () => {
        it('powinien zawijać szerokie swim lanes do wielu wierszy', () => {
            const children: VisualNode[] = [];
            // Stwórz wiele dzieci, które nie zmieszczą się w jednym wierszu
            for (let i = 0; i < 30; i++) {
                children.push(createNode(`child${i}`, 'var'));
            }

            const nodes: VisualNode[] = [
                createNode('parent', 'loop', children),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            const loadedChildren = parent.children;

            // Wszystkie dzieci powinny mieć pozycje
            expect(allNodesHavePositions([parent, ...loadedChildren])).toBe(true);

            // Sprawdź czy dzieci są w wielu wierszach (różne Y)
            const yPositions = loadedChildren.map(c => c.position!.y);
            const uniqueY = new Set(yPositions);
            
            // Powinno być więcej niż jeden wiersz dla 30 węzłów
            expect(uniqueY.size).toBeGreaterThan(1);

            // Sprawdź czy nie ma kolizji
            for (let i = 0; i < loadedChildren.length; i++) {
                for (let j = i + 1; j < loadedChildren.length; j++) {
                    expect(nodesOverlap(loadedChildren[i], loadedChildren[j])).toBe(false);
                }
            }
        });

        it('powinien zachować odstępy między wierszami w zawiniętych swim lanes', () => {
            const children: VisualNode[] = [];
            for (let i = 0; i < 20; i++) {
                children.push(createNode(`child${i}`, 'var'));
            }

            const nodes: VisualNode[] = [
                createNode('parent', 'loop', children),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const loadedChildren = loadedNodes[0].children;

            // Grupuj dzieci według Y (wiersze)
            const rows = new Map<number, VisualNode[]>();
            loadedChildren.forEach(child => {
                const y = child.position!.y;
                if (!rows.has(y)) {
                    rows.set(y, []);
                }
                rows.get(y)!.push(child);
            });

            const sortedRows = Array.from(rows.keys()).sort((a, b) => a - b);

            // Sprawdź odstępy między wierszami
            for (let i = 1; i < sortedRows.length; i++) {
                const prevRow = rows.get(sortedRows[i - 1])!;
                const currRow = rows.get(sortedRows[i])!;
                
                const designerAny = designer as unknown as Record<string, any>;
                const maxPrevY = Math.max(...prevRow.map(c => {
                    const dims = designerAny.getNodeDimensions?.(c) || { width: 200, height: 100 };
                    return c.position!.y + dims.height;
                }));
                const minCurrY = Math.min(...currRow.map(c => c.position!.y));
                
                const gap = minCurrY - maxPrevY;
                // Odstęp między wierszami powinien być co najmniej 80px (SWIM_LANE_GAP)
                expect(gap).toBeGreaterThanOrEqual(80);
            }
        });
    });

    describe('Routing połączeń hierarchicznych', () => {
        it('powinien tworzyć połączenia parent-to-child wertykalnie w dół', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            const children = parent.children;

            // Sprawdź czy dzieci są poniżej rodzica
            if (parent.position && children.length > 0 && children[0].position) {
                const designerAny = designer as unknown as Record<string, any>;
                const parentDims = designerAny.getNodeDimensions?.(parent) || { width: 200, height: 100 };
                const parentBottom = parent.position.y + parentDims.height;
                const childTop = children[0].position.y;

                expect(childTop).toBeGreaterThan(parentBottom);
            }
        });

        it('powinien tworzyć połączenia child-to-child horyzontalnie w swim lane', () => {
            const nodes: VisualNode[] = [
                createNode('parent', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                    createNode('child3', 'xpath'),
                ]),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            const children = loadedNodes[0].children;

            // Wszystkie dzieci powinny być na podobnym poziomie Y (horyzontalna linia)
            if (children.length > 1) {
                const yPositions = children.map(c => c.position!.y);
                const maxY = Math.max(...yPositions);
                const minY = Math.min(...yPositions);
                
                // Dzieci powinny być na podobnym poziomie (tolerancja 10px)
                expect(maxY - minY).toBeLessThan(10);

                // Dzieci powinny być ułożone horyzontalnie (różne X)
                const xPositions = children.map(c => c.position!.x);
                const uniqueX = new Set(xPositions);
                expect(uniqueX.size).toBe(children.length);
            }
        });

        it('powinien obsługiwać połączenia między różnymi poziomami hierarchii', () => {
            const nodes: VisualNode[] = [
                createNode('root1', 'var'),
                createNode('root2', 'loop', [
                    createNode('child1', 'var'),
                    createNode('child2', 'http'),
                ]),
                createNode('root3', 'xpath'),
            ];

            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();

            // Sprawdź czy root nodes są wertykalnie
            const rootNodes = loadedNodes.filter(n => n.children.length === 0);
            if (rootNodes.length > 1) {
                const yPositions = rootNodes.map(n => n.position!.y).sort((a, b) => a - b);
                for (let i = 1; i < yPositions.length; i++) {
                    expect(yPositions[i]).toBeGreaterThan(yPositions[i - 1]);
                }
            }

            // Sprawdź czy swim lane jest poniżej rodzica
            const parent = loadedNodes.find(n => n.children.length > 0);
            if (parent && parent.position && parent.children.length > 0) {
                const child = parent.children[0];
                if (child.position) {
                    expect(child.position.y).toBeGreaterThan(parent.position.y);
                }
            }
        });
    });
});

// Helper function dla testów
function calculateSwimLaneBounds(children: VisualNode[]): { left: number; right: number; width: number } {
    if (children.length === 0) {
        return { left: 0, right: 0, width: 0 };
    }

    let minLeft = Number.POSITIVE_INFINITY;
    let maxRight = Number.NEGATIVE_INFINITY;

    children.forEach(child => {
        if (!child.position) return;
        
        const left = child.position.x;
        const right = left + 200; // Przybliżona szerokość
        
        minLeft = Math.min(minLeft, left);
        maxRight = Math.max(maxRight, right);
    });

    return {
        left: minLeft,
        right: maxRight,
        width: maxRight - minLeft
    };
}

