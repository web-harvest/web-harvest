/**
 * VisualDesigner - Layout Validation Tests
 * Testy weryfikujące poprawność rozmieszczenia węzłów i połączeń
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../types';

describe('VisualDesigner - Layout Validation', () => {
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
        document.body.removeChild(container);
    });

    /**
     * Helper: Sprawdź czy węzły są ułożone wertykalnie (główny przepływ)
     */
    const verifyVerticalRootFlow = (nodes: VisualNode[]): { valid: boolean; errors: string[] } => {
        const errors: string[] = [];
        
        if (nodes.length === 0) {
            return { valid: true, errors: [] };
        }

        // Sprawdź czy wszystkie root nodes mają pozycje
        const nodesWithPositions = nodes.filter(n => n.position);
        if (nodesWithPositions.length !== nodes.length) {
            errors.push(`Not all root nodes have positions: ${nodesWithPositions.length}/${nodes.length}`);
        }

        // Sprawdź czy węzły są ułożone wertykalnie (Y rośnie)
        const sortedByY = [...nodesWithPositions].sort((a, b) => {
            const aY = a.position!.y;
            const bY = b.position!.y;
            return aY - bY;
        });

        for (let i = 1; i < sortedByY.length; i++) {
            const prev = sortedByY[i - 1];
            const curr = sortedByY[i];
            const prevY = prev.position!.y;
            const currY = curr.position!.y;
            
            if (currY <= prevY) {
                errors.push(`Node ${curr.id} (Y=${currY}) should be below ${prev.id} (Y=${prevY})`);
            }
        }

        // Sprawdź czy węzły są w podobnej kolumnie X (z tolerancją)
        const xPositions = nodesWithPositions.map(n => n.position!.x);
        const avgX = xPositions.reduce((sum, x) => sum + x, 0) / xPositions.length;
        const maxDeviation = 100; // Tolerancja 100px

        for (const node of nodesWithPositions) {
            const deviation = Math.abs(node.position!.x - avgX);
            if (deviation > maxDeviation) {
                errors.push(`Node ${node.id} X position (${node.position!.x}) deviates too much from average (${avgX.toFixed(0)})`);
            }
        }

        return { valid: errors.length === 0, errors };
    };

    /**
     * Helper: Sprawdź czy dzieci są ułożone horyzontalnie (swim lanes)
     */
    const verifyHorizontalChildren = (parent: VisualNode): { valid: boolean; errors: string[] } => {
        const errors: string[] = [];
        
        if (!parent.children || parent.children.length === 0) {
            return { valid: true, errors: [] };
        }

        if (!parent.position) {
            errors.push(`Parent ${parent.id} has no position`);
            return { valid: false, errors };
        }

        const childrenWithPositions = parent.children.filter(c => c.position);
        if (childrenWithPositions.length !== parent.children.length) {
            errors.push(`Not all children of ${parent.id} have positions: ${childrenWithPositions.length}/${parent.children.length}`);
        }

        // Sprawdź czy dzieci są na podobnej wysokości Y (swim lane)
        const yPositions = childrenWithPositions.map(c => c.position!.y);
        const avgY = yPositions.reduce((sum, y) => sum + y, 0) / yPositions.length;
        const maxYDeviation = 50; // Tolerancja 50px dla swim lane

        for (const child of childrenWithPositions) {
            const deviation = Math.abs(child.position!.y - avgY);
            if (deviation > maxYDeviation) {
                errors.push(`Child ${child.id} Y position (${child.position!.y}) deviates too much from swim lane average (${avgY.toFixed(0)})`);
            }
        }

        // Sprawdź czy dzieci są ułożone horyzontalnie (X rośnie lub maleje)
        const sortedByX = [...childrenWithPositions].sort((a, b) => a.position!.x - b.position!.x);
        for (let i = 1; i < sortedByX.length; i++) {
            const prev = sortedByX[i - 1];
            const curr = sortedByX[i];
            const prevX = prev.position!.x;
            const currX = curr.position!.x;
            
            if (currX <= prevX) {
                errors.push(`Child ${curr.id} (X=${currX}) should be to the right of ${prev.id} (X=${prevX})`);
            }
        }

        return { valid: errors.length === 0, errors };
    };

    /**
     * Helper: Sprawdź czy nie ma kolizji między węzłami
     */
    const verifyNoCollisions = (nodes: VisualNode[]): { valid: boolean; errors: string[] } => {
        const errors: string[] = [];
        const nodesWithPositions = nodes.filter(n => n.position);

        for (let i = 0; i < nodesWithPositions.length; i++) {
            const nodeA = nodesWithPositions[i];
            if (!nodeA.position) continue;

            // Pobierz wymiary węzła (użyj domyślnych jeśli nie ma)
            const widthA = 320; // MIN_CARD_WIDTH
            const heightA = 160; // MIN_CARD_HEIGHT
            const leftA = nodeA.position.x;
            const rightA = leftA + widthA;
            const topA = nodeA.position.y;
            const bottomA = topA + heightA;

            for (let j = i + 1; j < nodesWithPositions.length; j++) {
                const nodeB = nodesWithPositions[j];
                if (!nodeB.position) continue;

                const widthB = 320;
                const heightB = 160;
                const leftB = nodeB.position.x;
                const rightB = leftB + widthB;
                const topB = nodeB.position.y;
                const bottomB = topB + heightB;

                // Sprawdź kolizję
                const horizontalOverlap = !(rightA < leftB || leftA > rightB);
                const verticalOverlap = !(bottomA < topB || topA > bottomB);

                if (horizontalOverlap && verticalOverlap) {
                    errors.push(`Collision detected: ${nodeA.id} and ${nodeB.id} overlap`);
                }
            }
        }

        return { valid: errors.length === 0, errors };
    };

    /**
     * Helper: Sprawdź czy połączenia są poprawne
     */
    const verifyConnections = (nodes: VisualNode[], connections: Array<{ from: string; to: string }>): { valid: boolean; errors: string[] } => {
        const errors: string[] = [];
        const nodeMap = new Map<string, VisualNode>();
        
        const collectNodes = (nodeList: VisualNode[]) => {
            nodeList.forEach(node => {
                nodeMap.set(node.id, node);
                if (node.children) {
                    collectNodes(node.children);
                }
            });
        };
        collectNodes(nodes);

        // Sprawdź czy wszystkie połączenia odnoszą się do istniejących węzłów
        for (const conn of connections) {
            if (!nodeMap.has(conn.from)) {
                errors.push(`Connection from "${conn.from}" references non-existent node`);
            }
            if (!nodeMap.has(conn.to)) {
                errors.push(`Connection to "${conn.to}" references non-existent node`);
            }
        }

        // Sprawdź czy główny przepływ ma połączenia: START -> first -> ... -> last -> STOP
        const rootNodes = nodes.filter(n => !n.children || n.children.length === 0 || true); // Wszystkie root nodes
        if (rootNodes.length > 0) {
            const startConn = connections.find(c => c.from === 'workflow-start');
            if (!startConn) {
                errors.push('Missing connection from workflow-start to first root node');
            } else if (startConn.to !== rootNodes[0].id) {
                errors.push(`START should connect to first root node ${rootNodes[0].id}, but connects to ${startConn.to}`);
            }

            const lastRoot = rootNodes[rootNodes.length - 1];
            const endConn = connections.find(c => c.to === 'workflow-end' && c.from === lastRoot.id);
            if (!endConn) {
                errors.push(`Missing connection from last root node ${lastRoot.id} to workflow-end`);
            }
        }

        return { valid: errors.length === 0, errors };
    };

    describe('vertical root flow layout', () => {
        it('should layout single root node vertically', async () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'http', attributes: {}, body: null, children: [], position: undefined }
            ];

            designer.loadNodes(nodes);
            
            // Czekaj na auto-layout
            await new Promise(resolve => setTimeout(resolve, 500));

            const loadedNodes = designer.getNodes();
            const validation = verifyVerticalRootFlow(loadedNodes);
            
            expect(validation.valid).toBe(true);
            if (!validation.valid) {
                console.error('Layout validation errors:', validation.errors);
            }
        });

        it('should layout multiple root nodes vertically', async () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'http', attributes: {}, body: null, children: [], position: undefined },
                { id: '2', type: 'xpath', attributes: {}, body: null, children: [], position: undefined },
                { id: '3', type: 'var', attributes: {}, body: null, children: [], position: undefined }
            ];

            designer.loadNodes(nodes);
            
            await new Promise(resolve => setTimeout(resolve, 500));

            const loadedNodes = designer.getNodes();
            const validation = verifyVerticalRootFlow(loadedNodes);
            
            expect(validation.valid).toBe(true);
            if (!validation.valid) {
                console.error('Layout validation errors:', validation.errors);
            }
        });
    });

    describe('horizontal children layout (swim lanes)', () => {
        it('should layout children horizontally', async () => {
            const nodes: VisualNode[] = [
                {
                    id: 'parent-1',
                    type: 'loop',
                    attributes: {},
                    body: null,
                    children: [
                        { id: 'child-1', type: 'http', attributes: {}, body: null, children: [], position: undefined },
                        { id: 'child-2', type: 'xpath', attributes: {}, body: null, children: [], position: undefined },
                        { id: 'child-3', type: 'var', attributes: {}, body: null, children: [], position: undefined }
                    ],
                    position: undefined
                }
            ];

            designer.loadNodes(nodes);
            
            await new Promise(resolve => setTimeout(resolve, 500));

            const loadedNodes = designer.getNodes();
            const parent = loadedNodes[0];
            
            expect(parent.children).toBeDefined();
            expect(parent.children?.length).toBeGreaterThan(0);
            
            if (parent.children && parent.children.length > 0) {
                // Sprawdź czy dzieci mają pozycje
                const childrenWithPositions = parent.children.filter(c => c.position);
                if (childrenWithPositions.length === 0) {
                    console.warn('Children do not have positions yet - layout may not be complete');
                    // Czekaj dłużej
                    await new Promise(resolve => setTimeout(resolve, 500));
                }
                
                const validation = verifyHorizontalChildren(parent);
                if (!validation.valid) {
                    console.error('Children layout validation errors:', validation.errors);
                    console.log('Parent position:', parent.position);
                    console.log('Children positions:', parent.children.map(c => ({ id: c.id, pos: c.position })));
                }
                expect(validation.valid).toBe(true);
            }
        });
    });

    describe('collision detection', () => {
        it('should not have overlapping nodes', async () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'http', attributes: {}, body: null, children: [], position: undefined },
                { id: '2', type: 'xpath', attributes: {}, body: null, children: [], position: undefined },
                { id: '3', type: 'var', attributes: {}, body: null, children: [], position: undefined }
            ];

            designer.loadNodes(nodes);
            
            await new Promise(resolve => setTimeout(resolve, 500));

            const allNodes: VisualNode[] = [];
            const collectAll = (nodeList: VisualNode[]) => {
                nodeList.forEach(node => {
                    allNodes.push(node);
                    if (node.children) {
                        collectAll(node.children);
                    }
                });
            };
            collectAll(designer.getNodes());

            const validation = verifyNoCollisions(allNodes);
            expect(validation.valid).toBe(true);
            if (!validation.valid) {
                console.error('Collision detection errors:', validation.errors);
            }
        });
    });

    describe('connection validation', () => {
        it('should have correct connections for root flow', async () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'http', attributes: {}, body: null, children: [], position: undefined },
                { id: '2', type: 'xpath', attributes: {}, body: null, children: [], position: undefined }
            ];

            designer.loadNodes(nodes);
            
            await new Promise(resolve => setTimeout(resolve, 500));

            // Pobierz połączenia z designer
            const connections = designer.getConnections().map(c => ({ from: c.from, to: c.to }));

            if (connections.length === 0) {
                console.warn('No connections found - may need to wait longer');
                await new Promise(resolve => setTimeout(resolve, 500));
            }

            const validation = verifyConnections(designer.getNodes(), connections);
            if (!validation.valid) {
                console.error('Connection validation errors:', validation.errors);
                console.log('Available connections:', connections);
                console.log('Available nodes:', designer.getNodes().map(n => n.id));
            }
            expect(validation.valid).toBe(true);
        });
    });

    describe('cycle-safe optimal layers (Issue #30)', () => {
        const makeNode = (id: string): VisualNode => ({
            id,
            type: 'var',
            attributes: {},
            body: null,
            children: []
        });

        const computeLayers = (
            nodes: VisualNode[],
            connections: Array<{ from: string; to: string }>
        ): Map<string, number> =>
            (designer as any).computeOptimalLayers(nodes, connections, nodes);

        it('preserves longest-path layers for DAGs and handles cycles and deep graphs iteratively', () => {
            // Acyclic graph: A branches to B/C, which converge at D.
            const dagNodes = ['A', 'B', 'C', 'D', 'isolated'].map(makeNode);
            const dagLayers = computeLayers(dagNodes, [
                { from: 'A', to: 'B' },
                { from: 'A', to: 'C' },
                { from: 'B', to: 'D' },
                { from: 'C', to: 'D' }
            ]);
            expect(Object.fromEntries(dagLayers)).toEqual({
                A: 0,
                B: 1,
                C: 1,
                D: 2,
                isolated: 0
            });

            // A <-> B is one strongly connected component. Its incoming
            // predecessor and outgoing successor retain deterministic layers.
            const cycleNodes = ['P', 'A', 'B', 'T', 'disconnected'].map(makeNode);
            const cycleLayers = computeLayers(cycleNodes, [
                { from: 'P', to: 'A' },
                { from: 'A', to: 'B' },
                { from: 'B', to: 'A' },
                { from: 'B', to: 'T' }
            ]);
            expect(cycleLayers.get('P')).toBe(0);
            expect(cycleLayers.get('A')).toBe(1);
            expect(cycleLayers.get('B')).toBe(1);
            expect(cycleLayers.get('T')).toBe(2);
            expect(cycleLayers.get('disconnected')).toBe(0);

            // Self-loops are ignored as before; an isolated node remains layer 0.
            const selfLoopLayers = computeLayers(
                ['self', 'other'].map(makeNode),
                [{ from: 'self', to: 'self' }]
            );
            expect(selfLoopLayers.get('self')).toBe(0);
            expect(selfLoopLayers.get('other')).toBe(0);

            // A long chain proves traversal does not consume recursive call stack.
            const chainNodes = Array.from({ length: 2500 }, (_value, index) =>
                makeNode(`node-${index}`)
            );
            const chainConnections = chainNodes.slice(1).map((node, index) => ({
                from: chainNodes[index].id,
                to: node.id
            }));
            const chainLayers = computeLayers(chainNodes, chainConnections);
            expect(chainLayers.size).toBe(2500);
            expect(chainLayers.get('node-0')).toBe(0);
            expect(chainLayers.get('node-2499')).toBe(2499);
        });

        it('remaps mixed-content child references when cloning nodes', () => {
            const child = makeNode('child-original');
            const parent: VisualNode = {
                id: 'file-original',
                type: 'file',
                attributes: {},
                body: ' <catalog>  </catalog> ',
                children: [child],
                contentSequence: [
                    { kind: 'text', value: ' <catalog> ' },
                    { kind: 'child', nodeId: child.id },
                    { kind: 'text', value: ' </catalog> ' }
                ]
            };

            const cloned = (designer as any).createNodeWithNewIds(parent) as VisualNode;
            expect(cloned.children[0].id).not.toBe(child.id);
            expect(cloned.contentSequence?.[1]).toEqual({
                kind: 'child',
                nodeId: cloned.children[0].id
            });
        });

    });

});

